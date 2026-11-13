package dev.gustavo.moirai.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;

/**
 * A segurança de verdade para os testes de fatia ({@code @WebMvcTest}): a mesma cadeia de filtros e o mesmo
 * decodificador de JWT da aplicação. Assim um controller testado sozinho também exige token.
 */
@TestConfiguration
@Import({SecurityConfig.class, JwtConfig.class})
public class SecurityTestImports {
}
