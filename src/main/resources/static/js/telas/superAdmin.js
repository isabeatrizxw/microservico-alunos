import { api } from '../api.js';
import {
  campo, cartao, caixaMensagem, celula, comBotao, el, formatarDataHora, mostrarMensagem, tabela, tratarErro
} from '../ui.js';

/** Dono do sistema: cadastra escolas junto com o administrador de cada uma. */
export function telaSuperAdmin(contexto) {
  const listaEscolas = el('div', {}, el('p', { class: 'vazio' }, 'Carregando...'));
  const mensagemLista = caixaMensagem();

  async function carregarEscolas() {
    try {
      const escolas = await api('/api/admin/instituicoes');
      listaEscolas.replaceChildren(tabela(
        [{ texto: 'Nome' }, { texto: 'Tipo' }, { texto: 'Cadastrada em' }],
        escolas.map((escola) => el('tr', {},
          celula(escola.nome),
          celula(escola.tipo === 'AUTONOMO' ? 'Professor autônomo' : 'Escola'),
          celula(formatarDataHora(escola.criadoEm)))),
        'Nenhuma escola cadastrada ainda.'));
    } catch (erro) {
      tratarErro(contexto, erro, mensagemLista);
    }
  }

  carregarEscolas();
  return el('div', {},
    formularioNovaEscola(contexto, carregarEscolas),
    cartao('Escolas', mensagemLista, listaEscolas));
}

function formularioNovaEscola(contexto, aoCriar) {
  const nome = el('input', { type: 'text', name: 'nome', maxLength: 150, placeholder: 'Ex.: Colégio Exemplo' });
  const adminNome = el('input', { type: 'text', name: 'adminNome', maxLength: 150 });
  const adminEmail = el('input', { type: 'email', name: 'adminEmail', maxLength: 254, autocomplete: 'off' });
  const adminSenha = el('input', {
    type: 'password', name: 'adminSenha', minLength: 8, maxLength: 72, autocomplete: 'new-password'
  });
  const botao = el('button', { class: 'btn', type: 'submit' }, 'Cadastrar escola');
  const mensagem = caixaMensagem();

  const formulario = el('form', { novalidate: true },
    campo('Nome da escola', nome),
    el('div', { class: 'linha' },
      campo('Nome do administrador', adminNome),
      campo('E-mail do administrador', adminEmail)),
    campo('Senha inicial do administrador (mínimo 8 caracteres)', adminSenha),
    botao, mensagem);

  formulario.addEventListener('submit', (evento) => {
    evento.preventDefault();
    comBotao(botao, 'Cadastrando...', async () => {
      try {
        const escola = await api('/api/admin/instituicoes', {
          metodo: 'POST',
          corpo: {
            nome: nome.value.trim(),
            adminNome: adminNome.value.trim(),
            adminEmail: adminEmail.value.trim(),
            adminSenha: adminSenha.value
          }
        });
        formulario.reset();
        mostrarMensagem(mensagem,
          `Escola "${escola.nome}" cadastrada. Envie ao administrador o e-mail e a senha inicial.`, 'ok');
        aoCriar();
      } catch (erro) {
        tratarErro(contexto, erro, mensagem);
      }
    });
  });

  return cartao('Nova escola', formulario);
}
