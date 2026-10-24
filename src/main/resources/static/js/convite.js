// Página do convite: a pessoa vê para qual escola, papel e turma foi convidada e cria nome e senha.
import { api } from './api.js';
import { campo, cartao, caixaMensagem, comBotao, el, formatarDataHora, mostrarMensagem } from './ui.js';

const TAMANHO_MINIMO_SENHA = 8;
const conteudo = document.getElementById('conteudo');

// O código vem depois do "#" (ex.: /convite.html#abc123), que o navegador não envia ao servidor
const codigo = decodeURIComponent(window.location.hash.substring(1));

function erroDeConvite(mensagem) {
  conteudo.replaceChildren(cartao('Convite indisponível',
    el('p', { class: 'mensagem err' }, mensagem),
    el('p', { class: 'dica' }, 'Peça um novo convite para a administração da sua escola.')));
}

function formularioDeAceite(convite) {
  const nome = el('input', { type: 'text', maxLength: 150, autocomplete: 'name' });
  const senha = el('input', { type: 'password', minLength: TAMANHO_MINIMO_SENHA, maxLength: 72, autocomplete: 'new-password' });
  const confirmacao = el('input', { type: 'password', maxLength: 72, autocomplete: 'new-password' });
  const botao = el('button', { class: 'btn', type: 'submit' }, 'Criar minha conta');
  const mensagem = caixaMensagem();

  const papel = convite.papel === 'ALUNO' ? 'aluno' : 'professor';
  const detalhes = el('p', {},
    `${convite.instituicaoNome} convidou `, el('strong', {}, convite.email), ` como ${papel}`,
    convite.turmaNome ? ` da turma ${convite.turmaNome}` : '', '.');

  const formulario = el('form', { novalidate: true },
    campo('Seu nome', nome),
    campo(`Senha (mínimo ${TAMANHO_MINIMO_SENHA} caracteres)`, senha),
    campo('Confirme a senha', confirmacao),
    botao, mensagem);

  formulario.addEventListener('submit', (evento) => {
    evento.preventDefault();
    if (senha.value !== confirmacao.value) {
      mostrarMensagem(mensagem, 'As senhas não conferem.', 'err');
      return;
    }
    comBotao(botao, 'Criando...', async () => {
      try {
        await api(`/api/convites/${encodeURIComponent(codigo)}/aceitar`, {
          metodo: 'POST',
          corpo: { nome: nome.value.trim(), senha: senha.value }
        });
        contaCriada(convite.email);
      } catch (erro) {
        mostrarMensagem(mensagem, erro.message, 'err');
      }
    });
  });

  conteudo.replaceChildren(cartao('Aceitar convite',
    detalhes,
    el('p', { class: 'dica' }, `Convite válido até ${formatarDataHora(convite.expiraEm)}.`),
    formulario));
  nome.focus();
}

function contaCriada(email) {
  // Tira o código da barra de endereço: ele já foi usado
  history.replaceState(null, '', '/convite.html');
  conteudo.replaceChildren(cartao('Conta criada',
    el('p', { class: 'mensagem ok' }, `Pronto! Entre com o e-mail ${email} e a senha que você escolheu.`),
    el('p', {}, el('a', { class: 'btn', href: '/' }, 'Ir para o login'))));
}

async function iniciar() {
  if (!codigo) {
    erroDeConvite('O link do convite está incompleto.');
    return;
  }
  try {
    formularioDeAceite(await api(`/api/convites/${encodeURIComponent(codigo)}`));
  } catch (erro) {
    erroDeConvite(erro.message);
  }
}

iniciar();
