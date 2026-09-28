package io.github.wimdeblauwe.shadleaf.i18n;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.wimdeblauwe.shadleaf.ShadleafAutoConfiguration;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.MessageSourceAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.MessageSource;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.context.support.StaticMessageSource;
import tools.jackson.databind.json.JsonMapper;

class ShadleafMessageSourceTest {

  private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
      .withConfiguration(AutoConfigurations.of(MessageSourceAutoConfiguration.class, ShadleafAutoConfiguration.class))
      .withBean(JsonMapper.class, () -> JsonMapper.builder().build());

  @Test
  void applicationWithoutMessagesGetsTheShadleafStrings() {
    contextRunner.run(context -> {
      assertThat(context.getBean("messageSource")).isInstanceOf(ShadleafMessageSource.class);
      assertThat(context.getMessage("sl.button.loading", null, Locale.ENGLISH)).isEqualTo("Loading");
    });
  }

  @Test
  void bootsMessageSourceStaysAndFallsBackToTheShadleafStrings() {
    contextRunner.withPropertyValues("spring.messages.basename=i18n/app").run(context -> {
      assertThat(context.getBean("messageSource")).isExactlyInstanceOf(ResourceBundleMessageSource.class);
      assertThat(context.getMessage("app.greeting", null, Locale.ENGLISH)).isEqualTo("Hello");
      assertThat(context.getMessage("sl.button.loading", null, Locale.ENGLISH)).isEqualTo("Loading");
    });
  }

  @Test
  void applicationRewordsAndTranslatesAString() {
    contextRunner.withPropertyValues("spring.messages.basename=i18n/reworded").run(context -> {
      assertThat(context.getMessage("sl.button.loading", null, Locale.ENGLISH)).isEqualTo("Busy");
      assertThat(context.getMessage("sl.button.loading", null, Locale.forLanguageTag("nl"))).isEqualTo("Bezig");
    });
  }

  @Test
  void applicationsOwnMessageSourceBeanGetsTheShadleafStringsAsLastParent() {
    StaticMessageSource grandParent = new StaticMessageSource();
    StaticMessageSource own = new StaticMessageSource();
    own.setParentMessageSource(grandParent);
    own.addMessage("app.greeting", Locale.ENGLISH, "Hi");

    contextRunner.withBean("messageSource", MessageSource.class, () -> own).run(context -> {
      assertThat(context.getBean("messageSource")).isSameAs(own);
      assertThat(grandParent.getParentMessageSource()).isInstanceOf(ShadleafMessageSource.class);
      assertThat(context.getMessage("app.greeting", null, Locale.ENGLISH)).isEqualTo("Hi");
      assertThat(context.getMessage("sl.button.loading", null, Locale.ENGLISH)).isEqualTo("Loading");
    });
  }

  @Test
  void attachingTwiceAddsOneParent() {
    StaticMessageSource own = new StaticMessageSource();

    assertThat(ShadleafMessageSource.attachTo(own)).isTrue();
    MessageSource parent = own.getParentMessageSource();
    assertThat(ShadleafMessageSource.attachTo(own)).isTrue();

    assertThat(own.getParentMessageSource()).isSameAs(parent);
    assertThat(((ShadleafMessageSource) parent).getParentMessageSource()).isNull();
  }
}
