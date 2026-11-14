package dev.gustavo.moirai.config;

import java.util.Arrays;
import java.util.List;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * No profile {@code prod}, confere as variáveis obrigatórias antes de qualquer bean ser criado, e lista todas as que
 * faltam de uma vez. Sem isso, a falta de {@code MOIRAI_DB_URL} só aparecia como "'url' must start with jdbc", vinda
 * do pool de conexões, sem dizer qual variável faltou.
 *
 * <p>Roda depois que os arquivos de configuração e os profiles foram lidos (ordem mais baixa possível). Registrado em
 * {@code META-INF/spring.factories}.
 */
public class RequiredProdSettings implements EnvironmentPostProcessor, Ordered {

    static final List<String> REQUIRED = List.of("MOIRAI_DB_URL", "MOIRAI_JWT_SECRET");

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        if (!Arrays.asList(environment.getActiveProfiles()).contains("prod")) {
            return;
        }
        List<String> missing = REQUIRED.stream()
                .filter(name -> environment.getProperty(name, "").isBlank())
                .toList();
        if (!missing.isEmpty()) {
            throw new IllegalStateException(
                    "O profile prod precisa das variáveis de ambiente: %s. Veja o .env.example.".formatted(String.join(", ", missing)));
        }
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
