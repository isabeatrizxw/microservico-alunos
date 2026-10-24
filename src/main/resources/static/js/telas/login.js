import { api } from '../api.js';
import { campo, cartao, caixaMensagem, comBotao, el, mostrarMensagem } from '../ui.js';

/** Tela de login. Professores e alunos entram depois de aceitar o convite da escola. */
export function telaLogin({ aoEntrar, aviso }) {
  const email = el('input', { type: 'email', autocomplete: 'username', required: true });
  const senha = el('input', { type: 'password', autocomplete: 'current-password', required: true });
  const botao = el('button', { class: 'btn', type: 'submit' }, 'Entrar');
  const mensagem = caixaMensagem();
  if (aviso) {
    mostrarMensagem(mensagem, aviso, 'err');
  }

  const formulario = el('form', { novalidate: true }, campo('E-mail', email), campo('Senha', senha), botao, mensagem);
  formulario.addEventListener('submit', (evento) => {
    evento.preventDefault();
    if (!email.value.trim() || !senha.value) {
      mostrarMensagem(mensagem, 'Preencha e-mail e senha.', 'err');
      return;
    }
    comBotao(botao, 'Entrando...', async () => {
      try {
        const usuario = await api('/api/auth/login', {
          metodo: 'POST',
          corpo: { email: email.value.trim(), senha: senha.value }
        });
        aoEntrar(usuario);
      } catch (erro) {
        senha.value = '';
        mostrarMensagem(mensagem, erro.message, 'err');
      }
    });
  });

  queueMicrotask(() => email.focus());
  return el('div', { class: 'estreito' },
    cartao('Entrar', formulario,
      el('p', { class: 'dica' }, 'Recebeu um convite da escola? Abra o link do convite para criar sua senha.'),
      el('p', { class: 'dica' }, 'Dá aulas por conta própria? ', el('a', { href: '/cadastro.html' }, 'Crie sua conta de professor'), '.')));
}
