package io.github.wimdeblauwe.shadleaf.security;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.wimdeblauwe.shadleaf.ShadleafAutoConfiguration;
import io.github.wimdeblauwe.shadleaf.dialect.ShadleafDialect;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import tools.jackson.databind.json.JsonMapper;

class SecurityAutoConfigurationTest {

  private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
      .withConfiguration(AutoConfigurations.of(ShadleafAutoConfiguration.class))
      .withBean(JsonMapper.class, () -> JsonMapper.builder().build());

  @AfterEach
  void clearContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void defaultsWithSpringSecurity() {
    contextRunner.run(context -> {
      assertThat(context).getBean(CurrentUserResolver.class).isInstanceOf(DefaultCurrentUserResolver.class);
      UserSource userSource = context.getBean(UserSource.class);
      assertThat(userSource).isInstanceOf(SpringSecurityUserSource.class);
      assertThat(userSource.loginUrl()).isEqualTo("/login");
      assertThat(userSource.logoutUrl()).isEqualTo("/logout");
      assertThat(userSource.currentUser()).isNull();

      SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("ada", null, "ROLE_USER"));
      assertThat(userSource.currentUser()).isEqualTo(new ShadleafUser("ada", "ada", null, null, "A"));
    });
  }

  @Test
  void anApplicationResolverReplacesTheDefault() {
    contextRunner
        .withBean(CurrentUserResolver.class, () -> authentication -> ShadleafUser.of("Ada Lovelace", null, null, null))
        .run(context -> {
          assertThat(context).hasSingleBean(CurrentUserResolver.class);
          SecurityContextHolder.getContext()
              .setAuthentication(new TestingAuthenticationToken("ada", null, "ROLE_USER"));
          assertThat(context.getBean(UserSource.class).currentUser().name()).isEqualTo("Ada Lovelace");
        });
  }

  @Test
  void usesTheApplicationsSecurityContextHolderStrategy() {
    SecurityContext grace = new SecurityContextImpl(new TestingAuthenticationToken("grace", null, "ROLE_USER"));
    SecurityContextHolderStrategy strategy = new SecurityContextHolderStrategy() {
      @Override
      public void clearContext() {
      }

      @Override
      public SecurityContext getContext() {
        return grace;
      }

      @Override
      public void setContext(SecurityContext context) {
      }

      @Override
      public SecurityContext createEmptyContext() {
        return new SecurityContextImpl();
      }
    };

    contextRunner.withBean(SecurityContextHolderStrategy.class, () -> strategy)
        .run(context -> assertThat(context.getBean(UserSource.class).currentUser().name()).isEqualTo("grace"));
  }

  @Test
  void urlsFromProperties() {
    contextRunner.withPropertyValues("shadleaf.security.login-url=/sign-in", "shadleaf.security.logout-url=/sign-out")
        .run(context -> {
          UserSource userSource = context.getBean(UserSource.class);
          assertThat(userSource.loginUrl()).isEqualTo("/sign-in");
          assertThat(userSource.logoutUrl()).isEqualTo("/sign-out");
        });
  }

  @Test
  void oneOAuth2LoginClientIsTheLoginUrl() {
    contextRunner.withBean(ClientRegistrationRepository.class, () -> clients(github()))
        .run(context -> assertThat(context.getBean(UserSource.class).loginUrl())
            .isEqualTo("/oauth2/authorization/github"));
  }

  @Test
  void clientsThatDoNotLogInDoNotCount() {
    contextRunner.withBean(ClientRegistrationRepository.class, () -> clients(github(), clientCredentials()))
        .run(context -> assertThat(context.getBean(UserSource.class).loginUrl())
            .isEqualTo("/oauth2/authorization/github"));
  }

  @Test
  void severalOAuth2LoginClientsLeaveTheChoiceToTheLoginPage() {
    contextRunner.withBean(ClientRegistrationRepository.class, () -> clients(github(), google()))
        .run(context -> assertThat(context.getBean(UserSource.class).loginUrl()).isEqualTo("/login"));
  }

  @Test
  void aRepositoryThatCannotBeListedLeavesTheLoginPage() {
    ClientRegistrationRepository repository = registrationId -> github();

    contextRunner.withBean(ClientRegistrationRepository.class, () -> repository)
        .run(context -> assertThat(context.getBean(UserSource.class).loginUrl()).isEqualTo("/login"));
  }

  @Test
  void theLoginUrlPropertyWinsOverTheClient() {
    contextRunner.withBean(ClientRegistrationRepository.class, () -> clients(github()))
        .withPropertyValues("shadleaf.security.login-url=/login")
        .run(context -> assertThat(context.getBean(UserSource.class).loginUrl()).isEqualTo("/login"));
  }

  @Test
  void withoutSpringSecurityTheDialectStillStarts() {
    contextRunner.withClassLoader(new FilteredClassLoader("org.springframework.security"))
        .withPropertyValues("shadleaf.security.login-url=/sign-in")
        .run(context -> {
          assertThat(context).hasNotFailed();
          assertThat(context).hasSingleBean(ShadleafDialect.class);
          assertThat(context).doesNotHaveBean(CurrentUserResolver.class);
          assertThat(context).doesNotHaveBean(UserSource.class);
        });
  }

  private static InMemoryClientRegistrationRepository clients(ClientRegistration... registrations) {
    return new InMemoryClientRegistrationRepository(List.of(registrations));
  }

  private static ClientRegistration github() {
    return login("github");
  }

  private static ClientRegistration google() {
    return login("google");
  }

  private static ClientRegistration login(String registrationId) {
    return ClientRegistration.withRegistrationId(registrationId)
        .clientId("id")
        .clientSecret("secret")
        .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
        .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
        .authorizationUri("https://auth.example.com/authorize")
        .tokenUri("https://auth.example.com/token")
        .userInfoUri("https://auth.example.com/user")
        .userNameAttributeName("id")
        .build();
  }

  private static ClientRegistration clientCredentials() {
    return ClientRegistration.withRegistrationId("api")
        .clientId("id")
        .clientSecret("secret")
        .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
        .tokenUri("https://auth.example.com/token")
        .build();
  }
}
