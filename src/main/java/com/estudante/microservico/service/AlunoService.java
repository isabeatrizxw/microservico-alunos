package com.estudante.microservico.service;

import com.estudante.microservico.dto.AlunoRequestDTO;
import com.estudante.microservico.dto.AlunoResponseDTO;
import org.springframework.stereotype.Service;

@Service
public class AlunoService {

    private static final double NOTA_DE_CORTE = 5.0;

    /**
     * Calcula a média das duas notas e monta a mensagem de aprovação ou reprovação.
     *
     * @param request dados do aluno (nome e notas)
     * @return média calculada e mensagem correspondente
     */
    public AlunoResponseDTO calcularMedia(AlunoRequestDTO request) {
        double media = (request.getNota1() + request.getNota2()) / 2;
        String mensagem = gerarMensagem(request.getNome(), media);
        return new AlunoResponseDTO(request.getNome(), media, mensagem);
    }

    private String gerarMensagem(String nome, double media) {
        if (media > NOTA_DE_CORTE) {
            return String.format("Parabéns %s, você foi aprovado.", nome);
        }
        if (media < NOTA_DE_CORTE) {
            return String.format("%s, você foi reprovado.", nome);
        }
        // Se não é maior nem menor, só pode ser exatamente igual à nota de corte
        return String.format("%s, você não atingiu a nota de corte e foi reprovado.", nome);
    }

}
