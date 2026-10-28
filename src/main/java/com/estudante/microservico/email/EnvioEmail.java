package com.estudante.microservico.email;

/** Envia e-mails de texto (confirmação de cadastro, convites). Implementações em EmailConfig. */
public interface EnvioEmail {

    void enviar(String para, String assunto, String texto);

}
