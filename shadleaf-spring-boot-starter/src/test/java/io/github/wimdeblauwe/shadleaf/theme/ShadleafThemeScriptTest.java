package io.github.wimdeblauwe.shadleaf.theme;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.wimdeblauwe.shadleaf.ShadleafAutoConfiguration;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Map;
import java.util.Set;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.thymeleaf.autoconfigure.ThymeleafAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.core.convert.support.DefaultConversionService;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.expression.ThymeleafEvaluationContext;
import tools.jackson.databind.json.JsonMapper;

class ShadleafThemeScriptTest {

  private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
      .withConfiguration(AutoConfigurations.of(ThymeleafAutoConfiguration.class, ShadleafAutoConfiguration.class))
      .withBean(JsonMapper.class, () -> JsonMapper.builder().build());

  @Test
  void publishedHashMatchesTheRenderedScript() {
    contextRunner.run(context -> {
      Element script = renderThemeScript(context, Map.of());

      String renderedHash = "'sha256-" + sha256Base64(script.data()) + "'";
      assertThat(context.getBean(ShadleafThemeScript.class).getCspHash()).isEqualTo(renderedHash);
    });
  }

  @Test
  void scriptSetsTheDarkClassFromStorageOrTheOperatingSystem() {
    contextRunner.run(context -> assertThat(renderThemeScript(context, Map.of()).data())
        .contains("localStorage.getItem('shadleaf-theme')")
        .contains("prefers-color-scheme: dark")
        .contains("classList.toggle('dark'"));
  }

  @Test
  void rendersTheNonceWhenTheRequestCarriesOne() {
    contextRunner.run(context -> assertThat(renderThemeScript(context, Map.of("cspNonce", "r4nd0m")).attr("nonce"))
        .isEqualTo("r4nd0m"));
  }

  @Test
  void omitsTheNonceWithoutOne() {
    contextRunner.run(context -> assertThat(renderThemeScript(context, Map.of()).hasAttr("nonce")).isFalse());
  }

  @Test
  void nonceAttributeNameIsConfigurable() {
    contextRunner.withPropertyValues("shadleaf.csp.nonce-attribute=_csp_nonce")
        .run(context -> {
          Element script = renderThemeScript(context, Map.of("_csp_nonce", "abc", "cspNonce", "wrong"));
          assertThat(script.attr("nonce")).isEqualTo("abc");
        });
  }

  /**
   * Renders the fragment the way a request would: in a web request, request attributes are Thymeleaf context
   * variables, which is where the fragment looks for the nonce.
   */
  private static Element renderThemeScript(ApplicationContext applicationContext, Map<String, Object> variables) {
    Context context = new Context();
    context.setVariable(ThymeleafEvaluationContext.THYMELEAF_EVALUATION_CONTEXT_CONTEXT_VARIABLE_NAME,
        new ThymeleafEvaluationContext(applicationContext, new DefaultConversionService()));
    variables.forEach(context::setVariable);
    String html = applicationContext.getBean(SpringTemplateEngine.class)
        .process("sl/layout", Set.of("theme-script"), context);
    Element script = Jsoup.parse(html).selectFirst("script");
    assertThat(script).as("script in%n%s", html).isNotNull();
    return script;
  }

  private static String sha256Base64(String value) throws Exception {
    byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
    return Base64.getEncoder().encodeToString(digest);
  }
}
