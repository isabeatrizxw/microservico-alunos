// Funções de tela compartilhadas. Todo texto vindo da API entra via textContent (nunca innerHTML),
// para que um nome como "<script>" apareça como texto e não seja executado.

/** Cria um elemento: el('button', { class: 'btn', onClick: fn }, 'Salvar'). */
export function el(tag, props = {}, ...filhos) {
  const elemento = document.createElement(tag);
  for (const [chave, valor] of Object.entries(props)) {
    if (valor === null || valor === undefined || valor === false) {
      continue;
    }
    if (chave.startsWith('on')) {
      elemento.addEventListener(chave.substring(2).toLowerCase(), valor);
    } else if (chave === 'class') {
      elemento.className = valor;
    } else if (chave === 'for') {
      elemento.htmlFor = valor;
    } else if (chave in elemento) {
      elemento[chave] = valor;
    } else {
      elemento.setAttribute(chave, valor === true ? '' : valor);
    }
  }
  for (const filho of filhos.flat()) {
    if (filho === null || filho === undefined || filho === false) {
      continue;
    }
    elemento.append(filho instanceof Node ? filho : String(filho));
  }
  return elemento;
}

let proximoId = 0;

/** Campo com rótulo ligado ao input (acessível para leitores de tela). */
export function campo(rotulo, controle) {
  if (!controle.id) {
    controle.id = 'campo-' + (++proximoId);
  }
  return el('div', {}, el('label', { for: controle.id }, rotulo), controle);
}

export function cartao(titulo, ...filhos) {
  return el('section', { class: 'card' }, titulo ? el('h2', {}, titulo) : null, ...filhos);
}

/** Caixa de mensagem anunciada por leitores de tela. tipo: 'ok' | 'err' | ''. */
export function caixaMensagem() {
  return el('div', { class: 'mensagem', role: 'status', 'aria-live': 'polite' });
}

export function mostrarMensagem(caixa, texto, tipo = '') {
  caixa.textContent = texto;
  caixa.className = `mensagem ${tipo}`.trim();
  // Erro é anunciado na hora pelo leitor de tela; confirmação, quando ele puder
  caixa.setAttribute('role', tipo === 'err' ? 'alert' : 'status');
}

/**
 * Destaca os campos que o servidor apontou como inválidos ({ email: '...' }) e liga cada um à mensagem,
 * para o leitor de tela dizer qual campo está errado. Os campos precisam ter `name` igual ao da API.
 */
export function marcarCampos(formulario, caixa, campos) {
  if (!formulario) {
    return;
  }
  formulario.querySelectorAll('[aria-invalid]').forEach((controle) => {
    controle.removeAttribute('aria-invalid');
    controle.removeAttribute('aria-describedby');
  });
  if (!campos) {
    return;
  }
  if (!caixa.id) {
    caixa.id = 'mensagem-' + (++proximoId);
  }
  let primeiro = null;
  for (const nome of Object.keys(campos)) {
    const controle = formulario.querySelector(`[name="${CSS.escape(nome)}"]`);
    if (!controle) {
      continue;
    }
    controle.setAttribute('aria-invalid', 'true');
    controle.setAttribute('aria-describedby', caixa.id);
    controle.addEventListener('input', () => controle.removeAttribute('aria-invalid'), { once: true });
    primeiro = primeiro ?? controle;
  }
  primeiro?.focus();
}

/**
 * 401 = sessão expirada: volta ao login. Outros erros aparecem na caixa de mensagem e, quando o servidor
 * aponta campos, eles são destacados no formulário da caixa (desligue com `{ marcar: false }` em edições na linha).
 */
export function tratarErro(contexto, erro, caixa, { marcar = true } = {}) {
  if (erro.status === 401) {
    contexto.sessaoExpirou();
    return;
  }
  mostrarMensagem(caixa, erro.message, 'err');
  if (marcar) {
    marcarCampos(caixa.closest('form'), caixa, erro.campos);
  }
}

/** Desabilita o botão enquanto a ação roda, para evitar envio duplo. */
export async function comBotao(botao, textoCarregando, acao) {
  const textoOriginal = botao.textContent;
  botao.disabled = true;
  botao.textContent = textoCarregando;
  try {
    await acao();
  } finally {
    botao.disabled = false;
    botao.textContent = textoOriginal;
  }
}

/** Tabela com rolagem horizontal em telas estreitas. cabecalhos: [{ texto, numero }]. */
export function tabela(cabecalhos, linhas, textoVazio = 'Nada cadastrado ainda.') {
  if (linhas.length === 0) {
    return el('p', { class: 'vazio' }, textoVazio);
  }
  const cabeca = el('tr', {}, cabecalhos.map((c) => el('th', { class: c.numero ? 'numero' : null, scope: 'col' }, c.texto)));
  return el('div', { class: 'tabela-rolagem' },
    el('table', {}, el('thead', {}, cabeca), el('tbody', {}, linhas)));
}

export function celula(conteudo, numero = false) {
  return el('td', { class: numero ? 'numero' : null }, conteudo);
}

/** Abas acessíveis: abas([{ titulo, render: () => Node }]). */
export function abas(itens) {
  const painel = el('div', { role: 'tabpanel' });
  const botoes = itens.map((item, indice) => el('button', {
    class: 'aba', type: 'button', role: 'tab', 'aria-selected': 'false',
    onClick: () => selecionar(indice)
  }, item.titulo));

  function selecionar(indice) {
    botoes.forEach((botao, i) => botao.setAttribute('aria-selected', String(i === indice)));
    painel.replaceChildren(itens[indice].render());
  }

  selecionar(0);
  return el('div', {}, el('div', { class: 'abas', role: 'tablist' }, botoes), painel);
}

const SITUACOES = {
  APROVADO: { texto: 'Aprovado', classe: 'ok' },
  REPROVADO: { texto: 'Reprovado', classe: 'err' },
  SEM_NOTAS: { texto: 'Sem notas', classe: 'neutro' }
};

export function selo(situacao) {
  const dados = SITUACOES[situacao] ?? { texto: situacao, classe: 'neutro' };
  return el('span', { class: `selo ${dados.classe}` }, dados.texto);
}

export const NOMES_DOS_PAPEIS = {
  SUPER_ADMIN: 'Dono do sistema',
  ADMIN_ESCOLA: 'Administração da escola',
  PROFESSOR: 'Professor',
  PROFESSOR_AUTONOMO: 'Professor autônomo',
  ALUNO: 'Aluno'
};

export function formatarNota(valor) {
  if (valor === null || valor === undefined) {
    return '—';
  }
  return Number(valor).toLocaleString('pt-BR', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}

/** Datas da API vêm como LocalDateTime ISO (sem fuso), no horário do servidor. */
export function formatarDataHora(iso) {
  if (!iso) {
    return '—';
  }
  return new Date(iso).toLocaleString('pt-BR', { dateStyle: 'short', timeStyle: 'short' });
}

export function opcoes(select, itens, textoVazio) {
  select.replaceChildren(
    ...(itens.length === 0 ? [el('option', { value: '' }, textoVazio)] : []),
    ...itens.map((item) => el('option', { value: String(item.id) }, item.nome))
  );
  select.disabled = itens.length === 0;
}
