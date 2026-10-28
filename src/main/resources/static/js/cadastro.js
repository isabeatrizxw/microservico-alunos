// Cadastro público de professor autônomo. A conta só ativa depois de abrir o link enviado por e-mail.
import { api } from './api.js';
import './tema.js';
import { campo, cartao, caixaMensagem, comBotao, el, marcarCampos, mostrarMensagem } from './ui.js';

const TAMANHO_MINIMO_SENHA = 8;
const conteudo = document.getElementById('conteudo');

function formulario() {
  const nome = el('input', { type: 'text', name: 'nome', maxLength: 150, autocomplete: 'name' });
  const email = el('input', { type: 'email', name: 'email', maxLength: 254, autocomplete: 'email' });
  const nomeEspaco = el('input', {
    type: 'text', name: 'nomeEspaco', maxLength: 150, placeholder: 'Ex.: Reforço de Matemática da Ana'
  });
  const senha = el('input', {
    type: 'password', name: 'senha', minLength: TAMANHO_MINIMO_SENHA, maxLength: 72, autocomplete: 'new-password'
  });
  const confirmacao = el('input', { type: 'password', maxLength: 72, autocomplete: 'new-password' });
  const botao = el('button', { class: 'btn', type: 'submit' }, 'Criar conta');
  const mensagem = caixaMensagem();

  const form = el('form', { novalidate: true },
    campo('Seu nome', nome),
    campo('E-mail', email),
    campo('Nome das suas aulas (opcional)', nomeEspaco),
    campo(`Senha (mínimo ${TAMANHO_MINIMO_SENHA} caracteres)`, senha),
    campo('Confirme a senha', confirmacao),
    botao, mensagem);

  form.addEventListener('submit', (evento) => {
    evento.preventDefault();
    if (senha.value !== confirmacao.value) {
      mostrarMensagem(mensagem, 'As senhas não conferem.', 'err');
      return;
    }
    comBotao(botao, 'Criando...', async () => {
      try {
        const resposta = await api('/api/cadastro/autonomo', {
          metodo: 'POST',
          corpo: {
            nome: nome.value.trim(),
            email: email.value.trim(),
            senha: senha.value,
            nomeEspaco: nomeEspaco.value.trim() || null
          }
        });
        verifiqueSeuEmail(resposta.mensagem);
      } catch (erro) {
        mostrarMensagem(mensagem, erro.message, 'err');
        marcarCampos(form, mensagem, erro.campos);
      }
    });
  });

  conteudo.replaceChildren(cartao('Criar conta de professor',
    el('p', { class: 'dica' },
      'Para quem dá aulas por conta própria: cadastre suas turmas e disciplinas, convide seus alunos e lance notas.'),
    form,
    el('p', { class: 'dica' }, 'Já tem conta? ', el('a', { href: '/' }, 'Entrar'), '.'),
    el('p', { class: 'dica' }, 'Dá aula numa escola cliente? Peça o convite para a administração da escola.')));
  nome.focus();
}

function verifiqueSeuEmail(texto) {
  conteudo.replaceChildren(cartao('Verifique seu e-mail',
    el('p', { class: 'mensagem ok' }, texto),
    el('p', { class: 'dica' }, 'Não chegou? Confira a caixa de spam ou cadastre-se de novo para receber outro link.')));
}

formulario();
