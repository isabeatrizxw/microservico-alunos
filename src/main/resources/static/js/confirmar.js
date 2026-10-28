// Página do link de confirmação de e-mail do cadastro de professor autônomo.
import { api } from './api.js';
import './tema.js';
import { cartao, el } from './ui.js';

const conteudo = document.getElementById('conteudo');

// O código vem depois do "#" (ex.: /confirmar.html#abc123), que o navegador não envia ao servidor
const codigo = decodeURIComponent(window.location.hash.substring(1));

function mostrar(titulo, texto, tipo) {
  conteudo.replaceChildren(cartao(titulo,
    el('p', { class: `mensagem ${tipo}` }, texto),
    el('p', {}, el('a', { class: 'btn', href: tipo === 'ok' ? '/' : '/cadastro.html' },
      tipo === 'ok' ? 'Ir para o login' : 'Cadastrar de novo'))));
}

async function confirmar() {
  if (!codigo) {
    mostrar('Link incompleto', 'O link de confirmação está incompleto. Abra o link exatamente como chegou no e-mail.', 'err');
    return;
  }
  try {
    const resposta = await api('/api/cadastro/confirmar', { metodo: 'POST', corpo: { codigo } });
    // Tira o código da barra de endereço: ele já foi usado
    history.replaceState(null, '', '/confirmar.html');
    mostrar('E-mail confirmado', resposta.mensagem, 'ok');
  } catch (erro) {
    mostrar('Não foi possível confirmar', `${erro.message} Cadastre-se de novo para receber outro link.`, 'err');
  }
}

confirmar();
