import { api } from '../api.js';
import {
  abas, campo, cartao, caixaMensagem, celula, comBotao, el, formatarDataHora, mostrarMensagem, opcoes, tabela,
  tratarErro
} from '../ui.js';
import { listaAlunos, listaProfessores } from './pessoas.js';

export const TURMAS = {
  rota: '/api/escola/turmas', rotulo: 'Nome da turma', exemplo: 'Ex.: 1º A', maximo: 100,
  textoBotao: 'Cadastrar turma', textoVazio: 'Nenhuma turma cadastrada ainda.'
};

export const DISCIPLINAS = {
  rota: '/api/escola/disciplinas', rotulo: 'Nome da disciplina', exemplo: 'Ex.: Matemática', maximo: 100,
  textoBotao: 'Cadastrar disciplina', textoVazio: 'Nenhuma disciplina cadastrada ainda.'
};

/** Administração da escola: turmas, disciplinas e convites para professores e alunos. */
export function telaEscola(contexto) {
  return cartao(null, abas([
    { titulo: 'Turmas', render: () => cadastroSimples(contexto, TURMAS) },
    { titulo: 'Disciplinas', render: () => cadastroSimples(contexto, DISCIPLINAS) },
    { titulo: 'Alunos', render: () => listaAlunos(contexto) },
    { titulo: 'Professores', render: () => listaProfessores(contexto) },
    { titulo: 'Convites', render: () => convites(contexto) }
  ]));
}

/** Cadastro só com nome + lista (usado para turmas e disciplinas). */
export function cadastroSimples(contexto, { rota, rotulo, exemplo, maximo, textoBotao, textoVazio }) {
  const nome = el('input', { type: 'text', name: 'nome', maxLength: maximo, placeholder: exemplo });
  const botao = el('button', { class: 'btn', type: 'submit' }, textoBotao);
  const mensagem = caixaMensagem();
  const lista = el('div', {}, el('p', { class: 'vazio' }, 'Carregando...'));

  async function carregar() {
    try {
      const itens = await api(rota);
      lista.replaceChildren(tabela([{ texto: 'Nome' }, { texto: '' }], itens.map(linhaEditavel), textoVazio));
    } catch (erro) {
      tratarErro(contexto, erro, mensagem);
    }
  }

  /** Linha com Renomear (edição na própria linha) e Excluir (o servidor recusa se estiver em uso). */
  function linhaEditavel(item) {
    const linha = el('tr');

    function leitura() {
      const renomear = el('button', { class: 'btn btn-secundario btn-pequeno', type: 'button', onClick: edicao }, 'Renomear');
      const excluir = el('button', { class: 'btn btn-perigo btn-pequeno', type: 'button' }, 'Excluir');
      excluir.addEventListener('click', () => {
        if (!window.confirm(`Excluir "${item.nome}"?`)) {
          return;
        }
        comBotao(excluir, 'Excluindo...', async () => {
          try {
            await api(`${rota}/${item.id}`, { metodo: 'DELETE' });
            mostrarMensagem(mensagem, `"${item.nome}" excluída.`, 'ok');
            await carregar();
          } catch (erro) {
            tratarErro(contexto, erro, mensagem);
          }
        });
      });
      linha.replaceChildren(celula(item.nome), celula(el('div', { class: 'acoes' }, renomear, excluir)));
    }

    function edicao() {
      const campoNome = el('input', { type: 'text', maxLength: maximo, value: item.nome, 'aria-label': rotulo });
      const salvar = el('button', { class: 'btn btn-pequeno', type: 'button' }, 'Salvar');
      const cancelar = el('button', { class: 'btn btn-secundario btn-pequeno', type: 'button', onClick: leitura }, 'Cancelar');
      salvar.addEventListener('click', () => comBotao(salvar, 'Salvando...', async () => {
        try {
          await api(`${rota}/${item.id}`, { metodo: 'PUT', corpo: { nome: campoNome.value.trim() } });
          mostrarMensagem(mensagem, 'Nome alterado.', 'ok');
          await carregar();
        } catch (erro) {
          // O campo editado fica na linha, fora do formulário de cadastro: não marcar o campo de lá
          tratarErro(contexto, erro, mensagem, { marcar: false });
        }
      }));
      linha.replaceChildren(celula(campoNome), celula(el('div', { class: 'acoes' }, salvar, cancelar)));
      campoNome.focus();
    }

    leitura();
    return linha;
  }

  const formulario = el('form', { novalidate: true }, campo(rotulo, nome), botao, mensagem);
  formulario.addEventListener('submit', (evento) => {
    evento.preventDefault();
    comBotao(botao, 'Salvando...', async () => {
      try {
        const criado = await api(rota, { metodo: 'POST', corpo: { nome: nome.value.trim() } });
        formulario.reset();
        mostrarMensagem(mensagem, `"${criado.nome}" cadastrada.`, 'ok');
        await carregar();
      } catch (erro) {
        tratarErro(contexto, erro, mensagem);
      }
    });
  });

  carregar();
  return el('div', {}, formulario, el('h3', {}, 'Cadastradas'), lista);
}

/** Convites. Com `apenasAlunos` (professor autônomo), só dá para convidar alunos. */
export function convites(contexto, { apenasAlunos = false } = {}) {
  const email = el('input', { type: 'email', name: 'email', maxLength: 254, autocomplete: 'off' });
  const papel = el('select', { name: 'papel' },
    apenasAlunos ? null : el('option', { value: 'PROFESSOR' }, 'Professor'),
    el('option', { value: 'ALUNO' }, 'Aluno'));
  const turma = el('select', { name: 'turmaId', disabled: true });
  const botao = el('button', { class: 'btn', type: 'submit' }, 'Gerar convite');
  const mensagem = caixaMensagem();
  const linkGerado = el('div');
  const pendentes = el('div', {}, el('p', { class: 'vazio' }, 'Carregando...'));
  let turmas = [];

  function atualizarCampoTurma() {
    // Só aluno entra numa turma
    const ehAluno = papel.value === 'ALUNO';
    opcoes(turma, ehAluno ? turmas : [], ehAluno ? 'Cadastre uma turma primeiro' : 'Não se aplica a professor');
  }
  papel.addEventListener('change', atualizarCampoTurma);

  async function carregar() {
    try {
      turmas = await api('/api/escola/turmas');
      atualizarCampoTurma();
      const lista = await api('/api/escola/convites');
      pendentes.replaceChildren(tabela(
        [{ texto: 'E-mail' }, { texto: 'Papel' }, { texto: 'Turma' }, { texto: 'Vale até' }, { texto: '' }],
        lista.map((convite) => el('tr', {},
          celula(convite.email),
          celula(convite.papel === 'ALUNO' ? 'Aluno' : 'Professor'),
          celula(convite.turmaNome ?? '—'),
          celula(formatarDataHora(convite.expiraEm)),
          celula(botaoRevogar(convite)))),
        'Nenhum convite pendente.'));
    } catch (erro) {
      tratarErro(contexto, erro, mensagem);
    }
  }

  function botaoRevogar(convite) {
    const revogar = el('button', { class: 'btn btn-perigo btn-pequeno', type: 'button' }, 'Revogar');
    revogar.addEventListener('click', () => {
      if (!window.confirm(`Revogar o convite de ${convite.email}? O link deixará de funcionar.`)) {
        return;
      }
      comBotao(revogar, 'Revogando...', async () => {
        try {
          await api(`/api/escola/convites/${convite.id}`, { metodo: 'DELETE' });
          mostrarMensagem(mensagem, `Convite de ${convite.email} revogado.`, 'ok');
          await carregar();
        } catch (erro) {
          tratarErro(contexto, erro, mensagem);
        }
      });
    });
    return revogar;
  }

  const formulario = el('form', { novalidate: true },
    campo('E-mail da pessoa', email),
    el('div', { class: 'linha' }, campo('Papel', papel), campo('Turma', turma)),
    botao, mensagem, linkGerado);

  formulario.addEventListener('submit', (evento) => {
    evento.preventDefault();
    linkGerado.replaceChildren();
    comBotao(botao, 'Gerando...', async () => {
      try {
        const corpo = { email: email.value.trim(), papel: papel.value };
        if (papel.value === 'ALUNO' && turma.value) {
          corpo.turmaId = Number(turma.value);
        }
        const convite = await api('/api/escola/convites', { metodo: 'POST', corpo });
        email.value = '';
        mostrarMensagem(mensagem,
          `Convite enviado por e-mail para ${convite.email}, válido até ${formatarDataHora(convite.expiraEm)}.`, 'ok');
        linkGerado.replaceChildren(mostrarLink(convite.codigo));
        await carregar();
      } catch (erro) {
        tratarErro(contexto, erro, mensagem);
      }
    });
  });

  carregar();
  return el('div', {},
    formulario,
    el('p', { class: 'dica' }, 'A pessoa abre o link, escolhe nome e senha e entra com o papel e a turma definidos aqui.'),
    el('h3', {}, 'Convites pendentes'),
    pendentes);
}

/** O código só existe nesta resposta (o servidor guarda apenas o hash), então é mostrado uma única vez. */
function mostrarLink(codigo) {
  // O código vai depois do "#": o navegador não o envia ao servidor nem o grava em logs de acesso
  const link = `${window.location.origin}/convite.html#${codigo}`;
  const campoLink = el('input', { type: 'text', readOnly: true, value: link, 'aria-label': 'Link do convite' });
  const copiar = el('button', { class: 'btn btn-secundario', type: 'button' }, 'Copiar');
  copiar.addEventListener('click', async () => {
    try {
      await navigator.clipboard.writeText(link);
      copiar.textContent = 'Copiado!';
    } catch {
      campoLink.select();
      copiar.textContent = 'Use Ctrl+C';
    }
  });
  return el('div', {},
    el('p', { class: 'dica' }, 'Se a pessoa não receber o e-mail, copie e envie este link. Ele não será mostrado de novo.'),
    el('div', { class: 'convite-gerado' }, campoLink, copiar));
}
