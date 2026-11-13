package dev.gustavo.moirai.application.port.in;

import dev.gustavo.moirai.domain.User;
import java.time.Instant;
import java.util.List;

/** Portas de entrada da sessão: login, renovação e logout. */
public final class SessionUseCases {

    private SessionUseCases() {
    }

    /**
     * O par de tokens de uma sessão. O access token é um JWT curto, que a API confere sem consultar o banco.
     * O refresh token é um texto aleatório, longo e de uso único, que só serve para pedir um novo par.
     */
    public record Session(User user, String accessToken, Instant accessTokenExpiresAt, String refreshToken, Instant refreshTokenExpiresAt) {

        @Override
        public String toString() {
            return "Session[user=%s, accessTokenExpiresAt=%s]".formatted(user, accessTokenExpiresAt);
        }
    }

    public interface StartSession {
        /** Confere e-mail e senha e abre uma sessão. Lança {@code InvalidCredentialsException} se não conferirem. */
        Session login(String email, String password);
    }

    public interface RefreshSession {
        /**
         * Troca um refresh token por um par novo. O token usado deixa de valer (rotação). Se um token já trocado
         * aparecer de novo, alguém o copiou: todos os tokens daquela sessão são revogados.
         */
        Session refresh(String refreshToken);
    }

    public interface EndSession {
        /** Revoga o refresh token. O access token continua válido até vencer, por isso ele dura poucos minutos. */
        void logout(String refreshToken);
    }

    public interface ListUsers {
        /** Todos os usuários. Só para administradores. */
        List<User> listAll();
    }
}
