// Ponto de entrada: descobre quem está logado e mostra a tela do papel dessa pessoa.
import { api } from './api.js';
import { aplicarTema, salvarTema, temaEscuroAtivo } from './tema.js';
import { el, NOMES_DOS_PAPEIS } from './ui.js';
import { telaLogin } from './telas/login.js';
import { telaSuperAdmin } from './telas/superAdmin.js';
import { telaEscola } from './telas/escola.js';
import { telaProfessor } from './telas/professor.js';
import { telaAluno } from './telas/aluno.js';
import { telaAutonomo } from './telas/autonomo.js';

const TELAS_POR_PAPEL = {
  SUPER_ADMIN: telaSuperAdmin,
  ADMIN_ESCOLA: telaEscola,
  PROFESSOR: telaProfessor,
  PROFESSOR_AUTONOMO: telaAutonomo,
  ALUNO: telaAluno
};

const conteudo = document.getElementById('conteudo');
const areaUsuario = document.getElementById('usuario');
const botaoSair = document.getElementById('sair');

function mostrarLogin(aviso) {
  areaUsuario.hidden = true;
  botaoSair.hidden = true;
  conteudo.replaceChildren(telaLogin({ aoEntrar: mostrarAreaDoUsuario, aviso }));
}

function mostrarAreaDoUsuario(usuario) {
  areaUsuario.replaceChildren(el('strong', {}, usuario.nome), NOMES_DOS_PAPEIS[usuario.papel] ?? usuario.papel);
  areaUsuario.hidden = false;
  botaoSair.hidden = false;

  const tela = TELAS_POR_PAPEL[usuario.papel];
  conteudo.replaceChildren(tela({ usuario, sessaoExpirou }));
}

/** Chamado pelas telas quando a API responde 401: a sessão acabou (30 min sem uso). */
function sessaoExpirou() {
  mostrarLogin('Sua sessão expirou. Entre de novo.');
}

async function iniciar() {
  try {
    mostrarAreaDoUsuario(await api('/api/auth/eu'));
  } catch (erro) {
    mostrarLogin(erro.status === 401 ? null : erro.message);
  }
}

botaoSair.addEventListener('click', async () => {
  try {
    await api('/api/auth/logout', { metodo: 'POST' });
  } finally {
    mostrarLogin();
  }
});

// Tema claro/escuro (ver tema.js): o botão alterna e lembra a escolha neste navegador
const botaoTema = document.getElementById('alternarTema');
function mostrarTema(escuro) {
  aplicarTema(escuro);
  botaoTema.textContent = escuro ? 'Modo claro' : 'Modo escuro';
  botaoTema.setAttribute('aria-pressed', String(escuro));
}
botaoTema.addEventListener('click', () => {
  const escuro = !document.body.classList.contains('dark');
  mostrarTema(escuro);
  salvarTema(escuro);
});
mostrarTema(temaEscuroAtivo());

// Relógio do navegador
const relogio = document.getElementById('relogio');
function atualizarRelogio() {
  relogio.textContent = new Date().toLocaleString('pt-BR', { dateStyle: 'short', timeStyle: 'medium' });
}
setInterval(atualizarRelogio, 1000);
atualizarRelogio();

iniciar();
