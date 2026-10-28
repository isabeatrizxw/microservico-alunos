// Página do link de redefinição de senha enviado por e-mail.
import { api } from './api.js';
import './tema.js';
import { campo, cartao, caixaMensagem, comBotao, el, marcarCampos, mostrarMensagem } from './ui.js';

const TAMANHO_MINIMO_SENHA = 8;
const conteudo = document.getElementById('conteudo');

// O código vem depois do "#" (ex.: /redefinir-senha.html#abc123), que o navegador não envia ao servidor
const codigo = decodeURIComponent(window.location.hash.substring(1));

function linkInvalido(texto) {
  conteudo.replaceChildren(cartao('Link indisponível',
    el('p', { class: 'mensagem err' }, texto),
    el('p', {}, el('a', { class: 'btn', href: '/esqueci-senha.html' }, 'Pedir outro link'))));
}

function formulario() {
  const senha = el('input', {
    type: 'password', name: 'senha', minLength: TAMANHO_MINIMO_SENHA, maxLength: 72, autocomplete: 'new-password'
  });
  const confirmacao = el('input', { type: 'password', maxLength: 72, autocomplete: 'new-password' });
  const botao = el('button', { class: 'btn', type: 'submit' }, 'Salvar nova senha');
  const mensagem = caixaMensagem();

  const form = el('form', { novalidate: true },
    campo(`Nova senha (mínimo ${TAMANHO_MINIMO_SENHA} caracteres)`, senha),
    campo('Confirme a nova senha', confirmacao),
    botao, mensagem);

  form.addEventListener('submit', (evento) => {
    evento.preventDefault();
    if (senha.value !== confirmacao.value) {
      mostrarMensagem(mensagem, 'As senhas não conferem.', 'err');
      return;
    }
    comBotao(botao, 'Salvando...', async () => {
      try {
        const resposta = await api('/api/senha/redefinir', { metodo: 'POST', corpo: { codigo, senha: senha.value } });
        senhaAlterada(resposta.mensagem);
      } catch (erro) {
        if (erro.status === 404) {
          linkInvalido(erro.message);
          return;
        }
        mostrarMensagem(mensagem, erro.message, 'err');
        marcarCampos(form, mensagem, erro.campos);
      }
    });
  });

  conteudo.replaceChildren(cartao('Criar nova senha', form));
  senha.focus();
}

function senhaAlterada(texto) {
  // Tira o código da barra de endereço: ele já foi usado
  history.replaceState(null, '', '/redefinir-senha.html');
  conteudo.replaceChildren(cartao('Senha alterada',
    el('p', { class: 'mensagem ok' }, texto),
    el('p', {}, el('a', { class: 'btn', href: '/' }, 'Ir para o login'))));
}

if (codigo) {
  formulario();
} else {
  linkInvalido('O link está incompleto. Abra o link exatamente como chegou no e-mail.');
}
