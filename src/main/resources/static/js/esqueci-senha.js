// Pedido de link para criar uma nova senha. A resposta é a mesma exista ou não a conta.
import { api } from './api.js';
import './tema.js';
import { campo, cartao, caixaMensagem, comBotao, el, marcarCampos, mostrarMensagem } from './ui.js';

const conteudo = document.getElementById('conteudo');

function formulario() {
  const email = el('input', { type: 'email', name: 'email', maxLength: 254, autocomplete: 'email' });
  const botao = el('button', { class: 'btn', type: 'submit' }, 'Enviar link');
  const mensagem = caixaMensagem();

  const form = el('form', { novalidate: true }, campo('E-mail da sua conta', email), botao, mensagem);
  form.addEventListener('submit', (evento) => {
    evento.preventDefault();
    comBotao(botao, 'Enviando...', async () => {
      try {
        const resposta = await api('/api/senha/esqueci', { metodo: 'POST', corpo: { email: email.value.trim() } });
        linkEnviado(resposta.mensagem);
      } catch (erro) {
        mostrarMensagem(mensagem, erro.message, 'err');
        marcarCampos(form, mensagem, erro.campos);
      }
    });
  });

  conteudo.replaceChildren(cartao('Esqueci minha senha',
    el('p', { class: 'dica' }, 'Informe seu e-mail. Enviaremos um link para você criar uma nova senha.'),
    form,
    el('p', { class: 'dica' }, el('a', { href: '/' }, 'Voltar para o login'))));
  email.focus();
}

function linkEnviado(texto) {
  conteudo.replaceChildren(cartao('Verifique seu e-mail',
    el('p', { class: 'mensagem ok' }, texto),
    el('p', { class: 'dica' }, 'Não chegou? Confira a caixa de spam ou peça outro link.'),
    el('p', {}, el('a', { class: 'btn', href: '/' }, 'Ir para o login'))));
}

formulario();
