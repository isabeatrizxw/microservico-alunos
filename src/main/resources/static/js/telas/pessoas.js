import { api } from '../api.js';
import {
  campo, caixaMensagem, celula, comBotao, el, mostrarMensagem, opcoes, tabela, tratarErro
} from '../ui.js';

/** Selo "Ativo"/"Desativado" e botão que alterna a situação da pessoa. */
function situacao(pessoa, contexto, mensagem, aoMudar) {
  const botao = el('button', { class: `btn btn-pequeno ${pessoa.ativo ? 'btn-perigo' : 'btn-secundario'}`, type: 'button' },
    pessoa.ativo ? 'Desativar' : 'Reativar');
  botao.addEventListener('click', () => {
    const pergunta = pessoa.ativo
      ? `Desativar ${pessoa.nome}? A pessoa perde o acesso, mas as notas continuam no histórico.`
      : `Reativar ${pessoa.nome}?`;
    if (!window.confirm(pergunta)) {
      return;
    }
    comBotao(botao, 'Salvando...', async () => {
      try {
        await api(`/api/escola/pessoas/${pessoa.id}/ativo`, { metodo: 'PUT', corpo: { ativo: !pessoa.ativo } });
        mostrarMensagem(mensagem, `${pessoa.nome} ${pessoa.ativo ? 'desativado(a)' : 'reativado(a)'}.`, 'ok');
        await aoMudar();
      } catch (erro) {
        tratarErro(contexto, erro, mensagem);
      }
    });
  });
  return [
    celula(el('span', { class: `selo ${pessoa.ativo ? 'ok' : 'neutro'}` }, pessoa.ativo ? 'Ativo' : 'Desativado')),
    celula(botao)
  ];
}

/** Alunos da escola, com filtro por turma, troca de turma e ativar/desativar. */
export function listaAlunos(contexto) {
  const filtro = el('select');
  const mensagem = caixaMensagem();
  const area = el('div', {}, el('p', { class: 'vazio' }, 'Carregando...'));
  let turmas = [];
  let alunos = [];

  async function carregar() {
    try {
      const [listaTurmas, pessoas] = await Promise.all([api('/api/escola/turmas'), api('/api/escola/pessoas')]);
      turmas = listaTurmas;
      alunos = pessoas.alunos;
      const filtroAtual = filtro.value;
      filtro.replaceChildren(el('option', { value: '' }, 'Todas as turmas'),
        ...turmas.map((turma) => el('option', { value: String(turma.id) }, turma.nome)));
      filtro.value = filtroAtual;
      desenhar();
    } catch (erro) {
      tratarErro(contexto, erro, mensagem);
    }
  }

  function seletorDeTurma(aluno) {
    const select = el('select', { 'aria-label': `Turma de ${aluno.nome}` });
    opcoes(select, turmas, 'Sem turmas');
    select.value = aluno.turmaId ? String(aluno.turmaId) : '';
    select.addEventListener('change', async () => {
      const turmaNova = turmas.find((turma) => String(turma.id) === select.value);
      try {
        await api(`/api/escola/alunos/${aluno.id}/turma`, { metodo: 'PUT', corpo: { turmaId: Number(select.value) } });
        mostrarMensagem(mensagem, `${aluno.nome} agora está na turma ${turmaNova.nome}.`, 'ok');
        await carregar();
      } catch (erro) {
        select.value = aluno.turmaId ? String(aluno.turmaId) : '';
        tratarErro(contexto, erro, mensagem);
      }
    });
    return select;
  }

  function desenhar() {
    const visiveis = filtro.value ? alunos.filter((aluno) => String(aluno.turmaId) === filtro.value) : alunos;
    area.replaceChildren(tabela(
      [{ texto: 'Aluno' }, { texto: 'E-mail' }, { texto: 'Turma' }, { texto: 'Situação' }, { texto: '' }],
      visiveis.map((aluno) => el('tr', {},
        celula(aluno.nome),
        celula(aluno.email),
        celula(seletorDeTurma(aluno)),
        ...situacao(aluno, contexto, mensagem, carregar))),
      filtro.value ? 'Nenhum aluno nesta turma.' : 'Nenhum aluno ainda. Convide alunos na aba de convites.'));
  }

  filtro.addEventListener('change', desenhar);
  carregar();
  return el('div', {},
    campo('Mostrar', filtro),
    el('p', { class: 'dica' }, 'Para trocar um aluno de turma, escolha a nova turma na linha dele.'),
    mensagem, area);
}

/** Professores da escola, as aulas atribuídas a cada um e ativar/desativar. */
export function listaProfessores(contexto) {
  const mensagem = caixaMensagem();
  const area = el('div', {}, el('p', { class: 'vazio' }, 'Carregando...'));
  const professor = el('select', { name: 'professorId' });
  const turma = el('select', { name: 'turmaId' });
  const disciplina = el('select', { name: 'disciplinaId' });
  const botao = el('button', { class: 'btn', type: 'submit' }, 'Atribuir aula');
  const mensagemAtribuir = caixaMensagem();

  async function carregar() {
    try {
      const [pessoas, turmas, disciplinas] = await Promise.all([
        api('/api/escola/pessoas'), api('/api/escola/turmas'), api('/api/escola/disciplinas')]);
      const ativos = pessoas.professores.filter((p) => p.ativo);
      opcoes(professor, ativos, 'Nenhum professor ativo');
      opcoes(turma, turmas, 'Cadastre uma turma');
      opcoes(disciplina, disciplinas, 'Cadastre uma disciplina');
      area.replaceChildren(tabela(
        [{ texto: 'Professor' }, { texto: 'Aulas (turma · disciplina)' }, { texto: 'Situação' }, { texto: '' }],
        pessoas.professores.map((p) => el('tr', {},
          celula(el('div', {}, el('strong', {}, p.nome), el('div', { class: 'dica' }, p.email))),
          celula(aulas(p)),
          ...situacao(p, contexto, mensagem, carregar))),
        'Nenhum professor ainda. Convide professores na aba Convites.'));
    } catch (erro) {
      tratarErro(contexto, erro, mensagem);
    }
  }

  function aulas(p) {
    if (p.atribuicoes.length === 0) {
      return el('span', { class: 'dica' }, 'Nenhuma aula atribuída');
    }
    return el('div', { class: 'acoes' }, p.atribuicoes.map((aula) => {
      const remover = el('button', {
        class: 'btn btn-secundario btn-pequeno', type: 'button',
        title: 'Remover esta aula', 'aria-label': `Remover ${aula.turmaNome} · ${aula.disciplinaNome}`
      }, `${aula.turmaNome} · ${aula.disciplinaNome} ✕`);
      remover.addEventListener('click', () => {
        if (!window.confirm(`Tirar ${aula.disciplinaNome} da turma ${aula.turmaNome} de ${p.nome}?`)) {
          return;
        }
        comBotao(remover, 'Removendo...', async () => {
          try {
            await api(`/api/escola/atribuicoes/${aula.id}`, { metodo: 'DELETE' });
            mostrarMensagem(mensagem, 'Aula removida.', 'ok');
            await carregar();
          } catch (erro) {
            tratarErro(contexto, erro, mensagem);
          }
        });
      });
      return remover;
    }));
  }

  const formulario = el('form', { novalidate: true },
    el('div', { class: 'linha' }, campo('Professor', professor), campo('Turma', turma), campo('Disciplina', disciplina)),
    botao, mensagemAtribuir);
  formulario.addEventListener('submit', (evento) => {
    evento.preventDefault();
    if (!professor.value || !turma.value || !disciplina.value) {
      mostrarMensagem(mensagemAtribuir, 'Escolha professor, turma e disciplina.', 'err');
      return;
    }
    comBotao(botao, 'Atribuindo...', async () => {
      try {
        const aula = await api('/api/escola/atribuicoes', {
          metodo: 'POST',
          corpo: { professorId: Number(professor.value), turmaId: Number(turma.value), disciplinaId: Number(disciplina.value) }
        });
        mostrarMensagem(mensagemAtribuir,
          `${aula.professorNome} agora dá ${aula.disciplinaNome} na turma ${aula.turmaNome}.`, 'ok');
        await carregar();
      } catch (erro) {
        tratarErro(contexto, erro, mensagemAtribuir);
      }
    });
  });

  carregar();
  return el('div', {},
    el('h3', {}, 'Atribuir aula'),
    el('p', { class: 'dica' }, 'O professor só vê e lança notas nas turmas e disciplinas atribuídas a ele.'),
    formulario,
    el('h3', {}, 'Professores'),
    mensagem, area);
}
