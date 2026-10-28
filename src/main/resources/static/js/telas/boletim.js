import { celula, comBotao, el, formatarDataHora, formatarNota, selo, tabela } from '../ui.js';

/**
 * Boletim: notas por disciplina, com média e situação.
 * Com `aoEditar`/`aoApagar` (visão do professor), as notas lançadas por `usuarioId` ganham botões de ação;
 * o servidor também recusa a alteração de nota de outro professor (403).
 */
export function renderBoletim(boletim, { usuarioId, aoEditar, aoApagar } = {}) {
  const comAcoes = Boolean(aoEditar);
  const disciplinas = boletim.disciplinas.map((disciplina) => {
    const cabecalhos = [{ texto: 'Avaliação' }, { texto: 'Nota', numero: true }, { texto: 'Professor' }, { texto: 'Lançada em' }];
    if (comAcoes) {
      cabecalhos.push({ texto: '' });
    }
    const linhas = disciplina.notas.map((nota) =>
      linhaNota(nota, { comAcoes, podeAlterar: comAcoes && nota.lancadaPorId === usuarioId, aoEditar, aoApagar }));

    return el('div', {},
      el('h3', {}, disciplina.disciplinaNome, ' ', selo(disciplina.situacao)),
      el('p', { class: 'dica' }, `Média: ${formatarNota(disciplina.media)}`),
      tabela(cabecalhos, linhas, 'Nenhuma nota lançada nesta disciplina.'));
  });

  return el('div', {},
    el('p', { class: 'dica' },
      `${boletim.alunoNome}${boletim.turmaNome ? ' · Turma ' + boletim.turmaNome : ''} · calculado em ${formatarDataHora(boletim.calculadoEm)}`),
    disciplinas.length > 0 ? disciplinas : el('p', { class: 'vazio' }, 'A escola ainda não cadastrou disciplinas.'));
}

function linhaNota(nota, { comAcoes, podeAlterar, aoEditar, aoApagar }) {
  const linha = el('tr');

  function modoLeitura() {
    const editar = el('button', { class: 'btn btn-secundario btn-pequeno', type: 'button', onClick: modoEdicao }, 'Editar');
    const apagar = el('button', { class: 'btn btn-perigo btn-pequeno', type: 'button' }, 'Apagar');
    apagar.addEventListener('click', () => comBotao(apagar, 'Apagando...', () => aoApagar(nota)));

    const alteradaEm = nota.atualizadaEm ? ` (corrigida ${formatarDataHora(nota.atualizadaEm)})` : '';
    const celulas = [
      celula(nota.descricao),
      celula(formatarNota(nota.valor), true),
      celula(nota.lancadaPorNome),
      celula(formatarDataHora(nota.lancadaEm) + alteradaEm)
    ];
    // Sem coluna de ações (visão do aluno), não acrescenta nada: replaceChildren escreveria "null"
    if (comAcoes) {
      celulas.push(celula(podeAlterar ? el('div', { class: 'acoes' }, editar, apagar) : ''));
    }
    linha.replaceChildren(...celulas);
  }

  function modoEdicao() {
    const descricao = el('input', { type: 'text', maxLength: 100, value: nota.descricao, 'aria-label': 'Avaliação' });
    const valor = el('input', { type: 'number', min: 0, max: 10, step: 0.01, value: nota.valor, 'aria-label': 'Nota' });
    const salvar = el('button', { class: 'btn btn-pequeno', type: 'button' }, 'Salvar');
    const cancelar = el('button', { class: 'btn btn-secundario btn-pequeno', type: 'button', onClick: modoLeitura }, 'Cancelar');
    salvar.addEventListener('click', () => comBotao(salvar, 'Salvando...', () => aoEditar(nota, {
      descricao: descricao.value.trim(),
      valor: valor.value === '' ? null : Number(valor.value)
    })));

    linha.replaceChildren(
      celula(descricao), celula(valor), celula(nota.lancadaPorNome), celula(''),
      celula(el('div', { class: 'acoes' }, salvar, cancelar)));
    descricao.focus();
  }

  modoLeitura();
  return linha;
}
