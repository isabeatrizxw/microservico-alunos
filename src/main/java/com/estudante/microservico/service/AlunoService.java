package com.estudante.microservico.service;

import com.estudante.microservico.dto.AlunoRequestDTO;
import com.estudante.microservico.dto.AlunoResponseDTO;
import org.springframework.stereotype.Service;

@Service
public class AlunoService {

    public AlunoResponseDTO calcularMedia(AlunoRequestDTO request) {
        double media = (request.getNota1() + request.getNota2()) / 2;
        String mensagem = gerarMensagem(request.getNome(), media);
        return new AlunoResponseDTO(request.getNome(), media, mensagem);
    }

    private String gerarMensagem(String nome, double media) {
        if (media > 5) {
            return "Parabéns " + nome + ", você foi aprovado.";
        } else if (media < 5) {
            return nome + ", você foi reprovado.";
        }
        return nome + ", você não atingiu a nota de corte e foi reprovado.";
    }

}
