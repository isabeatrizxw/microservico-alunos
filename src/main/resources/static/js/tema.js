// Tema claro/escuro de todas as páginas: a escolha salva neste navegador ou, sem escolha, o tema do sistema.
const CHAVE = 'tema';

function escolhaSalva() {
  try {
    return localStorage.getItem(CHAVE);
  } catch {
    // Navegação privada pode bloquear o localStorage
    return null;
  }
}

export function temaEscuroAtivo() {
  const salva = escolhaSalva();
  if (salva) {
    return salva === 'escuro';
  }
  return window.matchMedia?.('(prefers-color-scheme: dark)').matches ?? false;
}

export function aplicarTema(escuro) {
  document.body.classList.toggle('dark', escuro);
}

export function salvarTema(escuro) {
  try {
    localStorage.setItem(CHAVE, escuro ? 'escuro' : 'claro');
  } catch {
    // Sem localStorage o tema só não fica salvo
  }
}

// Ao importar este módulo, a página já abre no tema certo
aplicarTema(temaEscuroAtivo());
