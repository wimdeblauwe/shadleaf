package io.github.wimdeblauwe.shadleaf.security;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.wimdeblauwe.shadleaf.ShadleafAutoConfiguration;
import io.github.wimdeblauwe.shadleaf.dialect.ShadleafDialect;
import io.github.wimdeblauwe.shadleaf.test.ComponentRenderTester;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.thymeleaf.autoconfigure.ThymeleafAutoConfiguration;
import org.springframework.util.ClassUtils;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

/**
 * The starter in an application without Spring Security. Runs in its own surefire execution, whose classpath has no
 * Spring Security at all (a {@code FilteredClassLoader} would not show that no class of the starter needs it).
 */
@Tag("without-spring-security")
class WithoutSpringSecurityTest {

  @Test
  void springSecurityIsNotOnTheClasspath() {
    assertThat(ClassUtils.isPresent("org.springframework.security.core.Authentication", null)).isFalse();
  }

  @Test
  void theStarterStartsAndRendersNobodySignedIn() {
    new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(JacksonAutoConfiguration.class, ThymeleafAutoConfiguration.class,
            ShadleafAutoConfiguration.class))
        .withPropertyValues("shadleaf.security.login-url=/sign-in")
        .run(context -> {
          assertThat(context).hasNotFailed();
          assertThat(context).hasSingleBean(ShadleafDialect.class);
          assertThat(context).doesNotHaveBean(UserSource.class);

          Document page = Jsoup.parse(context.getBean(SpringTemplateEngine.class).process("security/user",
              new Context()));

          assertThat(page.getElementById("signed-in").text()).isEqualTo("false");
          assertThat(page.getElementById("user").text()).isEmpty();
          assertThat(page.getElementById("login").attr("href")).isEqualTo("/sign-in");
          assertThat(page.getElementById("logout").text()).isEqualTo("/logout");
          assertThat(page.select("button.btn")).hasSize(1);
        });
  }

  @Test
  void theRenderTesterRendersNobodySignedIn() {
    String html = ComponentRenderTester.create().render("""
        <p th:text="${#slUser.signedIn}"></p>""").html();

    assertThat(html).contains("false");
  }
}
