import { api } from '../api.js';
import {
  campo, cartao, caixaMensagem, celula, comBotao, el, formatarDataHora, formatarNota, mostrarMensagem, opcoes,
  selo, tabela, tratarErro
} from '../ui.js';
import { renderBoletim } from './boletim.js';

/** Professor: escolhe turma e disciplina, lança notas, vê a média da turma e o boletim de cada aluno. */
export function telaProfessor(contexto) {
  // O professor autônomo é a própria administração: as dicas apontam para as abas dele
  const autonomo = contexto.usuario.papel === 'PROFESSOR_AUTONOMO';
  const textoSemCadastros = autonomo
    ? 'Cadastre suas turmas e disciplinas nas abas Turmas e Disciplinas.'
    : 'Você ainda não tem aulas atribuídas. Peça para a administração da escola atribuir suas turmas e disciplinas.';
  const textoSemAlunos = autonomo
    ? 'Nenhum aluno nesta turma ainda. Convide seus alunos na aba Alunos.'
    : 'Nenhum aluno nesta turma ainda. A administração da escola convida os alunos.';

  const turma = el('select');
  const disciplina = el('select');
  const mensagemGeral = caixaMensagem();

  const aluno = el('select');
  const descricao = el('input', { type: 'text', maxLength: 100, placeholder: 'Ex.: Prova 1' });
  const valor = el('input', { type: 'number', min: 0, max: 10, step: 0.01, placeholder: '0 – 10' });
  const botaoLancar = el('button', { class: 'btn', type: 'submit' }, 'Lançar nota');
  const mensagemLancar = caixaMensagem();

  const areaMedias = el('div', {}, el('p', { class: 'vazio' }, 'Escolha a turma e a disciplina.'));
  const mensagemBoletim = caixaMensagem();
  const areaBoletim = el('div');
  const cartaoBoletim = cartao('Boletim do aluno', mensagemBoletim, areaBoletim);
  cartaoBoletim.hidden = true;
  let alunoAberto = null;

  // Disciplinas que podem ser escolhidas em cada turma: todas (autônomo) ou só as atribuídas (professor da escola)
  let disciplinasDaTurma = () => [];

  async function iniciar() {
    try {
      let turmas;
      if (autonomo) {
        const [todasTurmas, disciplinas] = await Promise.all([api('/api/escola/turmas'), api('/api/escola/disciplinas')]);
        turmas = todasTurmas;
        disciplinasDaTurma = () => disciplinas;
      } else {
        const aulas = await api('/api/professor/atribuicoes');
        turmas = [...new Map(aulas.map((aula) => [aula.turmaId, { id: aula.turmaId, nome: aula.turmaNome }])).values()];
        disciplinasDaTurma = (turmaId) => aulas
          .filter((aula) => String(aula.turmaId) === String(turmaId))
          .map((aula) => ({ id: aula.disciplinaId, nome: aula.disciplinaNome }));
      }
      opcoes(turma, turmas, autonomo ? 'Nenhuma turma cadastrada' : 'Nenhuma aula atribuída');
      atualizarDisciplinas();
      if (turmas.length === 0 || disciplina.disabled) {
        mostrarMensagem(mensagemGeral, textoSemCadastros, 'err');
        return;
      }
      await carregarTurma();
    } catch (erro) {
      tratarErro(contexto, erro, mensagemGeral);
    }
  }

  function atualizarDisciplinas() {
    opcoes(disciplina, turma.value ? disciplinasDaTurma(turma.value) : [],
      autonomo ? 'Nenhuma disciplina cadastrada' : 'Nenhuma disciplina nesta turma');
  }

  async function carregarTurma() {
    fecharBoletim();
    try {
      const alunos = await api(`/api/professor/turmas/${turma.value}/alunos`);
      opcoes(aluno, alunos, 'Nenhum aluno nesta turma');
      await carregarMedias();
    } catch (erro) {
      tratarErro(contexto, erro, mensagemGeral);
    }
  }

  async function carregarMedias() {
    if (!turma.value || !disciplina.value) {
      return;
    }
    try {
      const dados = await api(`/api/professor/turmas/${turma.value}/medias?disciplinaId=${disciplina.value}`);
      areaMedias.replaceChildren(resumoDaTurma(dados), tabelaDeMedias(dados));
    } catch (erro) {
      tratarErro(contexto, erro, mensagemGeral);
    }
  }

  function tabelaDeMedias(dados) {
    return tabela(
      [{ texto: 'Aluno' }, { texto: 'Notas', numero: true }, { texto: 'Média', numero: true }, { texto: 'Situação' }],
      dados.alunos.map((item) => el('tr', {},
        celula(el('button', { class: 'link', type: 'button', onClick: () => abrirBoletim(item.alunoId) }, item.alunoNome)),
        celula(String(item.quantidadeNotas), true),
        celula(formatarNota(item.media), true),
        celula(selo(item.situacao)))),
      textoSemAlunos);
  }

  async function abrirBoletim(alunoId) {
    alunoAberto = alunoId;
    mostrarMensagem(mensagemBoletim, '');
    try {
      const boletim = await api(`/api/professor/alunos/${alunoId}/boletim`);
      areaBoletim.replaceChildren(renderBoletim(boletim, { usuarioId: contexto.usuario.id, aoEditar, aoApagar }));
      cartaoBoletim.hidden = false;
      cartaoBoletim.scrollIntoView({ behavior: 'smooth', block: 'start' });
    } catch (erro) {
      tratarErro(contexto, erro, mensagemBoletim);
    }
  }

  function fecharBoletim() {
    alunoAberto = null;
    cartaoBoletim.hidden = true;
    areaBoletim.replaceChildren();
  }

  async function atualizarDepoisDeMudanca() {
    await carregarMedias();
    if (alunoAberto !== null) {
      await abrirBoletim(alunoAberto);
    }
  }

  async function aoEditar(nota, alteracao) {
    try {
      await api(`/api/professor/notas/${nota.id}`, { metodo: 'PUT', corpo: alteracao });
      await atualizarDepoisDeMudanca();
      mostrarMensagem(mensagemBoletim, 'Nota corrigida.', 'ok');
    } catch (erro) {
      tratarErro(contexto, erro, mensagemBoletim);
    }
  }

  async function aoApagar(nota) {
    if (!window.confirm(`Apagar a nota "${nota.descricao}" (${formatarNota(nota.valor)})?`)) {
      return;
    }
    try {
      await api(`/api/professor/notas/${nota.id}`, { metodo: 'DELETE' });
      await atualizarDepoisDeMudanca();
      mostrarMensagem(mensagemBoletim, 'Nota apagada.', 'ok');
    } catch (erro) {
      tratarErro(contexto, erro, mensagemBoletim);
    }
  }

  const formularioLancar = el('form', { novalidate: true },
    el('div', { class: 'linha' }, campo('Aluno', aluno), campo('Avaliação', descricao), campo('Nota', valor)),
    botaoLancar, mensagemLancar);

  formularioLancar.addEventListener('submit', (evento) => {
    evento.preventDefault();
    if (!aluno.value) {
      mostrarMensagem(mensagemLancar, 'Escolha um aluno.', 'err');
      return;
    }
    comBotao(botaoLancar, 'Lançando...', async () => {
      try {
        const nota = await api('/api/professor/notas', {
          metodo: 'POST',
          corpo: {
            alunoId: Number(aluno.value),
            disciplinaId: Number(disciplina.value),
            descricao: descricao.value.trim(),
            valor: valor.value === '' ? null : Number(valor.value)
          }
        });
        descricao.value = '';
        valor.value = '';
        mostrarMensagem(mensagemLancar,
          `Nota ${formatarNota(nota.valor)} lançada para ${nota.alunoNome} em ${nota.disciplinaNome}.`, 'ok');
        await atualizarDepoisDeMudanca();
      } catch (erro) {
        tratarErro(contexto, erro, mensagemLancar);
      }
    });
  });

  turma.addEventListener('change', () => {
    atualizarDisciplinas();
    carregarTurma();
  });
  disciplina.addEventListener('change', () => {
    carregarMedias();
    if (alunoAberto !== null) {
      abrirBoletim(alunoAberto);
    }
  });

  iniciar();
  return el('div', {},
    cartao('Turma e disciplina',
      el('div', { class: 'linha' }, campo('Turma', turma), campo('Disciplina', disciplina)),
      mensagemGeral),
    cartao('Lançar nota', formularioLancar),
    cartao('Média da turma', areaMedias),
    cartaoBoletim);
}

function resumoDaTurma(dados) {
  const item = (rotulo, valor) => el('div', {}, el('span', {}, rotulo), el('strong', {}, valor));
  return el('div', {},
    el('div', { class: 'resumo' },
      item('Média da turma', formatarNota(dados.mediaTurma)),
      item('Aprovados', String(dados.aprovados)),
      item('Reprovados', String(dados.reprovados)),
      item('Sem notas', String(dados.semNotas))),
    el('p', { class: 'dica' },
      `${dados.turmaNome} · ${dados.disciplinaNome} · calculado em ${formatarDataHora(dados.calculadoEm)}. `
      + 'A média da turma é a média das médias dos alunos que já têm nota. Clique no nome para ver o boletim.'));
}
