package com.estudante.microservico.dto;

public class AlunoResponseDTO {

    private String nome;
    private double media;
    private String mensagem;

    public AlunoResponseDTO(String nome, double media, String mensagem) {
        this.nome = nome;
        this.media = media;
        this.mensagem = mensagem;
    }

    public String getNome() {
        return nome;
    }

    public double getMedia() {
        return media;
    }

    public String getMensagem() {
        return mensagem;
    }

}
