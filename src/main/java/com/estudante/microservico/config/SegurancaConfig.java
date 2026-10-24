package com.estudante.microservico.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.authentication.AccountStatusUserDetailsChecker;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.CsrfTokenRequestHandler;
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.function.Supplier;

/**
 * Segurança da API: login por sessão (cookie HttpOnly), proteção CSRF e regras de acesso por papel.
 *
 * <p>CSRF: o servidor manda o token no cookie {@code XSRF-TOKEN} (legível pelo JavaScript da página)
 * e toda requisição que altera dados (POST, PUT, DELETE) precisa devolvê-lo no header {@code X-XSRF-TOKEN}.
 */
@Configuration
public class SegurancaConfig {

    @Bean
    public SecurityFilterChain filtroDeSeguranca(HttpSecurity http) throws Exception {
        CookieCsrfTokenRepository repositorioCsrf = CookieCsrfTokenRepository.withHttpOnlyFalse();
        http
                .authorizeHttpRequests(acesso -> acesso
                        // Páginas e arquivos da interface: os dados só vêm da API, que exige login
                        .requestMatchers("/", "/index.html", "/convite.html", "/cadastro.html", "/confirmar.html",
                                "/css/**", "/js/**", "/favicon.ico", "/error").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        // Cadastro público de professor autônomo e confirmação de e-mail
                        .requestMatchers(HttpMethod.POST, "/api/cadastro/autonomo", "/api/cadastro/confirmar").permitAll()
                        // Quem recebeu um convite ainda não tem conta: o código do convite é a autorização
                        .requestMatchers(HttpMethod.GET, "/api/convites/*").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/convites/*/aceitar").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/datetime").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("SUPER_ADMIN")
                        // Professor precisa ver turmas e disciplinas da escola para lançar notas
                        .requestMatchers(HttpMethod.GET, "/api/escola/turmas", "/api/escola/disciplinas")
                        .hasAnyRole("ADMIN_ESCOLA", "PROFESSOR", "PROFESSOR_AUTONOMO")
                        // Atribuir aulas a professores: só escolas (o autônomo não tem outros professores)
                        .requestMatchers("/api/escola/atribuicoes/**", "/api/escola/atribuicoes").hasRole("ADMIN_ESCOLA")
                        // O professor autônomo administra o próprio espaço e também dá as aulas
                        .requestMatchers("/api/escola/**").hasAnyRole("ADMIN_ESCOLA", "PROFESSOR_AUTONOMO")
                        .requestMatchers("/api/professor/**").hasAnyRole("PROFESSOR", "PROFESSOR_AUTONOMO")
                        .requestMatchers("/api/aluno/**").hasRole("ALUNO")
                        .anyRequest().authenticated())
                .csrf(csrf -> csrf
                        .csrfTokenRepository(repositorioCsrf)
                        .csrfTokenRequestHandler(new CsrfParaPaginaWeb()))
                .addFilterAfter(new GerarCookieCsrf(), BasicAuthenticationFilter.class)
                .exceptionHandling(erros -> erros
                        .authenticationEntryPoint((req, res, ex) ->
                                responderErro(res, HttpStatus.UNAUTHORIZED, "Faça login para continuar."))
                        .accessDeniedHandler((req, res, ex) -> responderErro(res, HttpStatus.FORBIDDEN,
                                ex instanceof CsrfException
                                        ? "Token de segurança ausente ou expirado. Recarregue a página e tente de novo."
                                        : "Você não tem permissão para esta ação.")))
                .logout(logout -> logout
                        .logoutUrl("/api/auth/logout")
                        // O logout apaga o token CSRF; já manda um novo para o próximo login não falhar com 403
                        .logoutSuccessHandler((req, res, autenticacao) -> {
                            repositorioCsrf.saveToken(repositorioCsrf.generateToken(req), req, res);
                            res.setStatus(HttpStatus.NO_CONTENT.value());
                        }));
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(UserDetailsService usuarios, PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provedor = new DaoAuthenticationProvider();
        provedor.setUserDetailsService(usuarios);
        provedor.setPasswordEncoder(passwordEncoder);
        // Por padrão o Spring checa "conta ativa" antes da senha, o que revelaria a quem não sabe a senha
        // que o e-mail tem conta pendente. Aqui a situação da conta só é verificada depois da senha certa.
        provedor.setPreAuthenticationChecks(usuario -> { });
        provedor.setPostAuthenticationChecks(new AccountStatusUserDetailsChecker());
        return new ProviderManager(provedor);
    }

    /** Onde o login guarda o usuário autenticado: na sessão HTTP. */
    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    private static void responderErro(HttpServletResponse resposta, HttpStatus status, String mensagem) throws IOException {
        resposta.setStatus(status.value());
        resposta.setContentType(MediaType.APPLICATION_JSON_VALUE);
        resposta.setCharacterEncoding("UTF-8");
        resposta.getWriter().write("{\"erro\":\"" + mensagem + "\"}");
    }

    /**
     * Aceita o token CSRF puro vindo no header (lido do cookie pelo JavaScript) e mantém a proteção
     * contra BREACH quando o token é renderizado no servidor. Padrão recomendado pelo Spring Security para SPAs.
     */
    private static final class CsrfParaPaginaWeb extends CsrfTokenRequestAttributeHandler {

        private final CsrfTokenRequestHandler xor = new XorCsrfTokenRequestAttributeHandler();

        @Override
        public void handle(HttpServletRequest request, HttpServletResponse response, Supplier<CsrfToken> token) {
            xor.handle(request, response, token);
        }

        @Override
        public String resolveCsrfTokenValue(HttpServletRequest request, CsrfToken token) {
            if (StringUtils.hasText(request.getHeader(token.getHeaderName()))) {
                return super.resolveCsrfTokenValue(request, token);
            }
            return xor.resolveCsrfTokenValue(request, token);
        }

    }

    /** O token CSRF é carregado sob demanda; este filtro força a criação do cookie em toda resposta. */
    private static final class GerarCookieCsrf extends OncePerRequestFilter {

        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
                throws ServletException, IOException {
            CsrfToken token = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
            if (token != null) {
                token.getToken();
            }
            chain.doFilter(request, response);
        }

    }

}
