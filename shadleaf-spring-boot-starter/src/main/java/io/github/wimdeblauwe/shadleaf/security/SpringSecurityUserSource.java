package io.github.wimdeblauwe.shadleaf.security;

import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;
import org.springframework.security.authentication.AuthenticationTrustResolver;
import org.springframework.security.authentication.AuthenticationTrustResolverImpl;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;

/**
 * The {@link UserSource} of an application with Spring Security: the authentication of the request being rendered (an
 * error page's too), unless Spring Security calls it anonymous, through the {@link CurrentUserResolver}.
 */
public final class SpringSecurityUserSource implements UserSource {

  private final Supplier<SecurityContextHolderStrategy> strategy;
  private final AuthenticationTrustResolver trustResolver;
  private final CurrentUserResolver resolver;
  private final String loginUrl;
  private final String logoutUrl;

  /** With Spring Security's static {@link SecurityContextHolder} and its default trust resolver. */
  public SpringSecurityUserSource(CurrentUserResolver resolver, String loginUrl, String logoutUrl) {
    this(SecurityContextHolder::getContextHolderStrategy, new AuthenticationTrustResolverImpl(), resolver, loginUrl,
        logoutUrl);
  }

  public SpringSecurityUserSource(Supplier<SecurityContextHolderStrategy> strategy,
      AuthenticationTrustResolver trustResolver, CurrentUserResolver resolver, String loginUrl, String logoutUrl) {
    this.strategy = strategy;
    this.trustResolver = trustResolver;
    this.resolver = resolver;
    this.loginUrl = loginUrl;
    this.logoutUrl = logoutUrl;
  }

  @Override
  public @Nullable ShadleafUser currentUser() {
    Authentication authentication = strategy.get().getContext().getAuthentication();
    if (!trustResolver.isAuthenticated(authentication)) {
      return null;
    }
    return resolver.resolve(authentication);
  }

  @Override
  public String loginUrl() {
    return loginUrl;
  }

  @Override
  public String logoutUrl() {
    return logoutUrl;
  }
}
