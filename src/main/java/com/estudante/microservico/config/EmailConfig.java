package com.estudante.microservico.config;

import com.estudante.microservico.email.EnvioEmail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

/**
 * Escolhe como os e-mails saem:
 * <ul>
 *   <li>com SMTP configurado (variáveis SPRING_MAIL_HOST, SPRING_MAIL_PORT, SPRING_MAIL_USERNAME,
 *       SPRING_MAIL_PASSWORD), envia de verdade;</li>
 *   <li>sem SMTP (desenvolvimento), só escreve o e-mail no log, com o link, para testar localmente.</li>
 * </ul>
 * Em produção ({@code app.email.exigir-smtp=true}) a aplicação não sobe sem SMTP:
 * links de confirmação e convites nunca podem parar no log de um servidor real.
 */
@Configuration
public class EmailConfig {

    private static final Logger log = LoggerFactory.getLogger(EmailConfig.class);

    @Bean
    public EnvioEmail envioEmail(ObjectProvider<JavaMailSender> smtp,
                                 @Value("${app.email.remetente}") String remetente,
                                 @Value("${app.email.exigir-smtp:false}") boolean exigirSmtp) {
        JavaMailSender enviador = smtp.getIfAvailable();
        if (enviador != null) {
            return new EnvioPorSmtp(enviador, remetente);
        }
        if (exigirSmtp) {
            throw new IllegalStateException(
                    "SMTP não configurado. Defina SPRING_MAIL_HOST, SPRING_MAIL_PORT, SPRING_MAIL_USERNAME e SPRING_MAIL_PASSWORD.");
        }
        log.warn("SMTP não configurado: e-mails serão apenas escritos no log (modo desenvolvimento).");
        return new EnvioSomenteNoLog();
    }

    private static final class EnvioPorSmtp implements EnvioEmail {

        private final JavaMailSender enviador;
        private final String remetente;

        private EnvioPorSmtp(JavaMailSender enviador, String remetente) {
            this.enviador = enviador;
            this.remetente = remetente;
        }

        @Override
        public void enviar(String para, String assunto, String texto) {
            SimpleMailMessage mensagem = new SimpleMailMessage();
            mensagem.setFrom(remetente);
            mensagem.setTo(para);
            mensagem.setSubject(assunto);
            mensagem.setText(texto);
            try {
                enviador.send(mensagem);
            } catch (MailException e) {
                // Não derruba a operação (o cadastro/convite já foi salvo); fica registrado para investigar
                log.error("Falha ao enviar e-mail \"{}\" para {}", assunto, para, e);
            }
        }

    }

    private static final class EnvioSomenteNoLog implements EnvioEmail {

        @Override
        public void enviar(String para, String assunto, String texto) {
            log.warn("""

                    ===== E-MAIL (não enviado: SMTP não configurado) =====
                    Para: {}
                    Assunto: {}

                    {}
                    =======================================================""", para, assunto, texto);
        }

    }

}
