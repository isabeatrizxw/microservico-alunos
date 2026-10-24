// Acesso à API: envia o token CSRF e transforma respostas de erro em mensagens legíveis.

const COOKIE_CSRF = 'XSRF-TOKEN';
const HEADER_CSRF = 'X-XSRF-TOKEN';

export class ErroApi extends Error {
  constructor(status, mensagem) {
    super(mensagem);
    this.status = status;
  }
}

function lerCookie(nome) {
  const cookie = document.cookie.split('; ').find((item) => item.startsWith(nome + '='));
  return cookie ? decodeURIComponent(cookie.substring(nome.length + 1)) : null;
}

/**
 * Chama a API. Métodos que alteram dados levam o token CSRF que o servidor deixou no cookie.
 * @returns o JSON da resposta, ou null quando não há corpo (201/204)
 * @throws ErroApi com status HTTP (0 = sem conexão) e mensagem pronta para mostrar
 */
export async function api(caminho, { metodo = 'GET', corpo } = {}) {
  const headers = { Accept: 'application/json' };
  if (corpo !== undefined) {
    headers['Content-Type'] = 'application/json';
  }
  if (metodo !== 'GET') {
    const token = lerCookie(COOKIE_CSRF);
    if (token) {
      headers[HEADER_CSRF] = token;
    }
  }

  let resposta;
  try {
    resposta = await fetch(caminho, {
      method: metodo,
      headers,
      credentials: 'same-origin',
      body: corpo === undefined ? undefined : JSON.stringify(corpo)
    });
  } catch {
    throw new ErroApi(0, 'Não foi possível conectar ao servidor.');
  }

  const dados = await lerJson(resposta);
  if (!resposta.ok) {
    throw new ErroApi(resposta.status, mensagemDeErro(dados, resposta.status));
  }
  return dados;
}

async function lerJson(resposta) {
  const texto = await resposta.text();
  if (!texto) {
    return null;
  }
  try {
    return JSON.parse(texto);
  } catch {
    return null;
  }
}

function mensagemDeErro(dados, status) {
  if (dados && typeof dados.erro === 'string') {
    return dados.erro;
  }
  // Erros de validação chegam como { campo: mensagem }; o erro padrão do Spring tem "status"
  if (dados && typeof dados === 'object' && !('status' in dados)) {
    return Object.values(dados).join(' ');
  }
  if (status === 404) {
    return 'Não encontrado.';
  }
  return 'Algo deu errado. Tente de novo.';
}
