package io.github.wimdeblauwe.shadleaf.security;

import java.util.List;
import java.util.stream.StreamSupport;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter;
import org.springframework.util.ClassUtils;

/**
 * The sign-in URL of an application with exactly one OAuth2 login client: Spring Security's authorization endpoint for
 * it, {@code /oauth2/authorization/<registration id>}, which sends the user straight to the provider, as Spring
 * Security's own login page does in that case. Touches {@code spring-security-oauth2-client} only when
 * {@link #PRESENT} is true.
 */
public final class OAuth2LoginUrl {

  static final boolean PRESENT = ClassUtils.isPresent(
      "org.springframework.security.oauth2.client.registration.ClientRegistrationRepository",
      OAuth2LoginUrl.class.getClassLoader());

  private OAuth2LoginUrl() {
  }

  /**
   * The authorization URL when the application has one {@code ClientRegistrationRepository} that can be listed and
   * holds exactly one client for login (authorization code); {@code null} otherwise.
   */
  public static @Nullable String find(BeanFactory beanFactory) {
    if (!PRESENT) {
      return null;
    }
    ClientRegistrationRepository repository = beanFactory.getBeanProvider(ClientRegistrationRepository.class)
        .getIfUnique();
    if (!(repository instanceof Iterable<?> registrations)) {
      return null;
    }
    List<ClientRegistration> logins = StreamSupport.stream(registrations.spliterator(), false)
        .filter(ClientRegistration.class::isInstance)
        .map(ClientRegistration.class::cast)
        .filter(registration -> AuthorizationGrantType.AUTHORIZATION_CODE.equals(
            registration.getAuthorizationGrantType()))
        .toList();
    return logins.size() == 1
        ? OAuth2AuthorizationRequestRedirectFilter.DEFAULT_AUTHORIZATION_REQUEST_BASE_URI + "/"
            + logins.get(0).getRegistrationId()
        : null;
  }
}
