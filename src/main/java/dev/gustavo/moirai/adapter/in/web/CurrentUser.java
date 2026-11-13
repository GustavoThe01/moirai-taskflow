package dev.gustavo.moirai.adapter.in.web;

import dev.gustavo.moirai.domain.UserId;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;
import java.util.UUID;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Parâmetro de controller que recebe o usuário autenticado: o {@code sub} do JWT, já como {@link UserId}.
 * Substitui o cabeçalho provisório {@code X-User-Id} dos Dias 04 a 11.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrentUser {
}

@Configuration
class CurrentUserConfig implements WebMvcConfigurer {

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(new CurrentUserResolver());
    }
}

class CurrentUserResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUser.class) && UserId.class.equals(parameter.getParameterType());
    }

    @Override
    public UserId resolveArgument(MethodParameter parameter, ModelAndViewContainer container,
                                  NativeWebRequest request, WebDataBinderFactory binders) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof JwtAuthenticationToken token)) {
            throw new InvalidBearerTokenException("Requisição sem access token.");
        }
        try {
            return new UserId(UUID.fromString(token.getToken().getSubject()));
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new InvalidBearerTokenException("O token não identifica um usuário.");
        }
    }
}
