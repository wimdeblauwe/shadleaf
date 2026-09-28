package io.github.wimdeblauwe.shadleaf;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.wimdeblauwe.shadleaf.assets.ShadleafAssets;
import io.github.wimdeblauwe.shadleaf.component.ComponentDefinition;
import io.github.wimdeblauwe.shadleaf.component.ComponentDefinitionSource;
import io.github.wimdeblauwe.shadleaf.component.ComponentRegistry;
import io.github.wimdeblauwe.shadleaf.dialect.ShadleafDialect;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
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
      assertThat(context.getBean(ComponentRegistry.class).names()).contains("test-chip", "test-card");
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

  @Test
  void templatesPathIsTheFirstComponentSource(@TempDir Path templates) throws Exception {
    Path components = Files.createDirectories(templates.resolve("sl/components"));
    Files.writeString(components.resolve("test-chip.html"),
        "<html><head><sl:props><sl:prop name=\"tone\"/></sl:props></head></html>");

    contextRunner.withPropertyValues("shadleaf.dev.templates-path=" + templates)
        .run(context -> assertThat(context.getBean(ComponentRegistry.class).get("test-chip").props())
            .containsOnlyKeys("tone"));
  }

  @Test
  void componentDefinitionSourceBeansComeBeforeTheClasspath() {
    ComponentDefinition custom = ComponentDefinition.undeclared("test-chip", "java");
    contextRunner.withBean(ComponentDefinitionSource.class, () -> new ComponentDefinitionSource() {
          @Override
          public Optional<ComponentDefinition> find(String name) {
            return name.equals("test-chip") ? Optional.of(custom) : Optional.empty();
          }

          @Override
          public Set<String> names() {
            return Set.of("test-chip");
          }
        })
        .run(context -> assertThat(context.getBean(ComponentRegistry.class).get("test-chip")).isSameAs(custom));
  }
}
