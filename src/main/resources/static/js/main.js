// Ponto de entrada: descobre quem está logado e mostra a tela do papel dessa pessoa.
import { api } from './api.js';
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

// Tema claro/escuro, lembrado neste navegador
const botaoTema = document.getElementById('alternarTema');
function aplicarTema(escuro) {
  document.body.classList.toggle('dark', escuro);
  botaoTema.textContent = escuro ? 'Modo claro' : 'Modo escuro';
}
botaoTema.addEventListener('click', () => {
  const escuro = !document.body.classList.contains('dark');
  aplicarTema(escuro);
  try {
    localStorage.setItem('tema', escuro ? 'escuro' : 'claro');
  } catch {
    // Navegação privada pode bloquear o localStorage: o tema só não fica salvo
  }
});
try {
  aplicarTema(localStorage.getItem('tema') === 'escuro');
} catch {
  aplicarTema(false);
}

// Relógio do navegador
const relogio = document.getElementById('relogio');
function atualizarRelogio() {
  relogio.textContent = new Date().toLocaleString('pt-BR', { dateStyle: 'short', timeStyle: 'medium' });
}
setInterval(atualizarRelogio, 1000);
atualizarRelogio();

iniciar();
