package com.estudante.microservico.email;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/** Use com @Import(EmailsDeTeste.class): os serviços passam a "enviar" para EmailsCapturados. */
@TestConfiguration
public class EmailsDeTeste {

    @Bean
    @Primary
    public EmailsCapturados emailsCapturados() {
        return new EmailsCapturados();
    }

}
