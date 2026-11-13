package dev.gustavo.moirai.config;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.oauth2.server.resource.web.access.BearerTokenAccessDeniedHandler;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import tools.jackson.databind.json.JsonMapper;

/**
 * Spring Security como resource server: toda rota, menos as públicas, exige um JWT válido no cabeçalho
 * {@code Authorization: Bearer}. Sem sessão, sem cookie e, por isso, sem CSRF.
 *
 * <ul>
 *   <li>Sem token ou com token inválido (assinatura, emissor, validade): {@code 401} com {@code WWW-Authenticate}.</li>
 *   <li>Com token válido, mas sem o papel exigido: {@code 403}.</li>
 *   <li>As duas respostas usam ProblemDetail, como o resto da API.</li>
 * </ul>
 */
@Configuration
class SecurityConfig {

    static final String[] PUBLIC_ROUTES = {
        "/api/v1/auth/**", "/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**", "/actuator/health", "/actuator/info"
    };

    @Bean
    SecurityFilterChain apiSecurity(HttpSecurity http, JsonMapper json) throws Exception {
        var bearerEntryPoint = new BearerTokenAuthenticationEntryPoint();
        var bearerDenied = new BearerTokenAccessDeniedHandler();
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(routes -> routes
                        .requestMatchers(PUBLIC_ROUTES).permitAll()
                        .requestMatchers(HttpMethod.GET, "/").permitAll()
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .oauth2ResourceServer(server -> server
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(rolesFromToken()))
                        .authenticationEntryPoint((request, response, exception) -> {
                            bearerEntryPoint.commence(request, response, exception); // status 401 e WWW-Authenticate
                            write(response, json, HttpStatus.UNAUTHORIZED, "unauthorized", "Não autenticado",
                                    "Envie um access token válido no cabeçalho Authorization: Bearer <token>.", request.getRequestURI());
                        })
                        .accessDeniedHandler((request, response, exception) -> {
                            bearerDenied.handle(request, response, exception);
                            write(response, json, HttpStatus.FORBIDDEN, "forbidden", "Operação não permitida",
                                    "O seu papel não permite esta operação.", request.getRequestURI());
                        }))
                .build();
    }

    /** O claim {@code roles} do token vira as autoridades {@code ROLE_USER} e {@code ROLE_ADMIN}. */
    private static JwtAuthenticationConverter rolesFromToken() {
        var authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName("roles");
        authorities.setAuthorityPrefix("ROLE_");
        var converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        return converter;
    }

    private static void write(HttpServletResponse response, JsonMapper json, HttpStatus status, String type,
                              String title, String detail, String instance) throws IOException {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(URI.create("/problems/" + type));
        problem.setTitle(title);
        problem.setInstance(URI.create(instance));
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        json.writeValue(response.getOutputStream(), problem);
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
