package com.estudante.microservico.service;

import com.estudante.microservico.dto.AlunoRequestDTO;
import com.estudante.microservico.dto.AlunoResponseDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AlunoService {

    private final double notaDeCorte;

    public AlunoService(@Value("${aluno.nota-de-corte:5.0}") double notaDeCorte) {
        this.notaDeCorte = notaDeCorte;
    }

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
        if (media > notaDeCorte) {
            return String.format("Parabéns %s, você foi aprovado.", nome);
        }
        if (media < notaDeCorte) {
            return String.format("%s, você foi reprovado.", nome);
        }
        // Se não é maior nem menor, só pode ser exatamente igual à nota de corte
        return String.format("%s, você não atingiu a nota de corte e foi reprovado.", nome);
    }

}
