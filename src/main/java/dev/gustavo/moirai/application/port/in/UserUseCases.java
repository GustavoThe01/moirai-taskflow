package dev.gustavo.moirai.application.port.in;

import dev.gustavo.moirai.domain.User;

/** Portas de entrada de cadastro e login. */
public final class UserUseCases {

    private UserUseCases() {
    }

    public interface RegisterUser {
        /** Cria a conta. A senha chega em texto puro e só sai daqui como hash. */
        User register(String email, String name, String password);
    }

    public interface AuthenticateUser {
        /**
         * Confere e-mail e senha. Devolve o usuário ou lança {@code InvalidCredentialsException}, com a mesma
         * mensagem para e-mail desconhecido e senha errada.
         */
        User authenticate(String email, String password);
    }
}
