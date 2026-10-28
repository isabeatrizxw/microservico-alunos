package com.estudante.microservico.email;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** EnvioEmail de teste: guarda os e-mails em memória para os testes lerem os links enviados. */
public class EmailsCapturados implements EnvioEmail {

    public record Email(String para, String assunto, String texto) {
    }

    private static final Pattern CODIGO_NO_LINK = Pattern.compile("\\.html#([A-Za-z0-9_-]+)");

    private final List<Email> enviados = Collections.synchronizedList(new ArrayList<>());

    @Override
    public void enviar(String para, String assunto, String texto) {
        enviados.add(new Email(para, assunto, texto));
    }

    public List<Email> enviadosPara(String para) {
        synchronized (enviados) {
            return enviados.stream().filter(email -> email.para().equals(para)).toList();
        }
    }

    public Email ultimoPara(String para) {
        List<Email> lista = enviadosPara(para);
        if (lista.isEmpty()) {
            throw new AssertionError("Nenhum e-mail enviado para " + para);
        }
        return lista.get(lista.size() - 1);
    }

    /** Código do link (o que vem depois de ".html#") do último e-mail enviado para o endereço. */
    public String codigoDoUltimoLinkPara(String para) {
        Matcher matcher = CODIGO_NO_LINK.matcher(ultimoPara(para).texto());
        if (!matcher.find()) {
            throw new AssertionError("O último e-mail para " + para + " não tem link com código.");
        }
        return matcher.group(1);
    }

    public void limpar() {
        enviados.clear();
    }

}
