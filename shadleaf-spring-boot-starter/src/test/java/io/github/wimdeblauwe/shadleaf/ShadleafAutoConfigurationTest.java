package io.github.wimdeblauwe.shadleaf;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.wimdeblauwe.shadleaf.assets.ShadleafAssets;
import io.github.wimdeblauwe.shadleaf.dialect.ShadleafDialect;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.thymeleaf.templateresolver.FileTemplateResolver;
import tools.jackson.databind.json.JsonMapper;

class ShadleafAutoConfigurationTest {

  private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
      .withConfiguration(AutoConfigurations.of(ShadleafAutoConfiguration.class))
      .withBean(JsonMapper.class, () -> JsonMapper.builder().build());

  @Test
  void registersDialectAndAssets() {
    contextRunner.run(context -> {
      assertThat(context).hasSingleBean(ShadleafDialect.class);
      assertThat(context).hasSingleBean(ShadleafAssets.class);
      assertThat(context.getBean(ShadleafDialect.class).getPrefix()).isEqualTo("sl");
      assertThat(context).doesNotHaveBean(FileTemplateResolver.class);
    });
  }

  @Test
  void viteServerUrlSwitchesAssetsToDevMode() {
    contextRunner.withPropertyValues("shadleaf.dev.vite-server-url=http://localhost:5174")
        .run(context -> assertThat(context.getBean(ShadleafAssets.class).isDevMode()).isTrue());
  }

  @Test
  void templatesPathRegistersTheLiveReloadResolver() {
    contextRunner.withPropertyValues("shadleaf.dev.templates-path=src/main/resources/templates/")
        .run(context -> assertThat(context).hasSingleBean(FileTemplateResolver.class));
  }
}
