import { abas, cartao, el } from '../ui.js';
import { cadastroSimples, convites, DISCIPLINAS, TURMAS } from './escola.js';
import { listaAlunos } from './pessoas.js';
import { telaProfessor } from './professor.js';

/**
 * Professor autônomo: é a administração e o professor do próprio espaço.
 * Reaproveita a tela do professor (notas e médias) e os cadastros da escola, mas só convida alunos.
 */
export function telaAutonomo(contexto) {
  return el('div', {}, abas([
    { titulo: 'Notas', render: () => telaProfessor(contexto) },
    { titulo: 'Turmas', render: () => cartao(null, cadastroSimples(contexto, TURMAS)) },
    { titulo: 'Disciplinas', render: () => cartao(null, cadastroSimples(contexto, DISCIPLINAS)) },
    {
      titulo: 'Alunos',
      render: () => el('div', {},
        cartao('Seus alunos', listaAlunos(contexto)),
        cartao('Convidar alunos', convites(contexto, { apenasAlunos: true })))
    }
  ]));
}
