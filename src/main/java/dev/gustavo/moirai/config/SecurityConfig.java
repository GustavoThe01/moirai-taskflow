package dev.gustavo.moirai.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security, primeiro passo: a cadeia de filtros de uma API sem sessão.
 *
 * <ul>
 *   <li><b>Sem sessão e sem cookie:</b> cada requisição se identifica sozinha. Por isso o CSRF fica desligado:
 *       ele protege sessões baseadas em cookie, que esta API não usa.</li>
 *   <li><b>Sem formulário de login e sem HTTP Basic:</b> o login é o endpoint {@code POST /api/v1/auth/login}.</li>
 *   <li><b>Rotas públicas declaradas:</b> cadastro, login, documentação e health check.</li>
 * </ul>
 *
 * <p>As rotas de projetos e tarefas ainda ficam abertas e usam o cabeçalho provisório {@code X-User-Id}.
 * No Dia 12, o filtro do JWT entra nesta cadeia e elas passam a exigir um token.
 */
@Configuration
class SecurityConfig {

    static final String[] PUBLIC_ROUTES = {
        "/api/v1/auth/**", "/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**", "/actuator/health", "/actuator/info"
    };

    @Bean
    SecurityFilterChain apiSecurity(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(routes -> routes
                        .requestMatchers(PUBLIC_ROUTES).permitAll()
                        // Provisório até o Dia 12: troca para .anyRequest().authenticated() com o filtro do JWT.
                        .anyRequest().permitAll())
                .build();
    }

    /**
     * Sem este bean, o Spring Boot cria um usuário em memória com uma senha aleatória e a imprime no log.
     * Os usuários de verdade estão no banco e são conferidos pelo caso de uso de login.
     */
    @Bean
    UserDetailsService noInMemoryUsers() {
        return new InMemoryUserDetailsManager();
    }
}
