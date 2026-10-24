import { api } from '../api.js';
import { cartao, caixaMensagem, comBotao, el, tratarErro } from '../ui.js';
import { renderBoletim } from './boletim.js';

/** Aluno: vê só o próprio boletim, com a média e a situação de cada disciplina. */
export function telaAluno(contexto) {
  const mensagem = caixaMensagem();
  const area = el('div', {}, el('p', { class: 'vazio' }, 'Carregando...'));
  const atualizar = el('button', { class: 'btn btn-secundario btn-pequeno', type: 'button' }, 'Atualizar');

  async function carregar() {
    try {
      const boletim = await api('/api/aluno/boletim');
      area.replaceChildren(renderBoletim(boletim));
    } catch (erro) {
      tratarErro(contexto, erro, mensagem);
    }
  }

  atualizar.addEventListener('click', () => comBotao(atualizar, 'Atualizando...', carregar));
  carregar();
  return cartao('Meu boletim', el('div', { class: 'acoes' }, atualizar), mensagem, area);
}
