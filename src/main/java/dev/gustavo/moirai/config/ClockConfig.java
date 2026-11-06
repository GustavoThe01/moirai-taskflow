package dev.gustavo.moirai.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class ClockConfig {

    /** O relógio é uma dependência explícita, para os testes poderem controlar o tempo. */
    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
