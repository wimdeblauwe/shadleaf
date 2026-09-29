package io.github.wimdeblauwe.shadleaf;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.wimdeblauwe.shadleaf.assets.ShadleafAssets;
import io.github.wimdeblauwe.shadleaf.component.ComponentDefinition;
import io.github.wimdeblauwe.shadleaf.component.ComponentDefinitionSource;
import io.github.wimdeblauwe.shadleaf.component.ComponentRegistry;
import io.github.wimdeblauwe.shadleaf.dev.WebTypesFileWriter;
import io.github.wimdeblauwe.shadleaf.dialect.ShadleafDialect;
import io.github.wimdeblauwe.shadleaf.icon.Icon;
import io.github.wimdeblauwe.shadleaf.icon.IconRegistry;
import io.github.wimdeblauwe.shadleaf.icon.IconSource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.thymeleaf.IEngineConfiguration;
import org.thymeleaf.spring6.SpringTemplateEngine;
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
  void skinAndAssetVariantSelectTheStylesheet() {
    contextRunner.withPropertyValues("shadleaf.skin=flat", "shadleaf.assets.variant=embedded")
        .run(context -> assertThat(context.getBean(ShadleafAssets.class).getCssUrl())
            .startsWith("/shadleaf/assets/shadleaf-flat.embedded-"));
  }

  @Test
  void alpineVariantSelectsTheScript() {
    contextRunner.run(context -> assertThat(context.getBean(ShadleafAssets.class).getJsUrl())
        .startsWith("/shadleaf/assets/shadleaf.alpine-"));
    contextRunner.withPropertyValues("shadleaf.assets.alpine=external")
        .run(context -> assertThat(context.getBean(ShadleafAssets.class).getJsUrl())
            .matches("/shadleaf/assets/shadleaf-[\\w-]+\\.js"));
  }

  @Test
  void unknownSkinFailsTheStartup() {
    contextRunner.withPropertyValues("shadleaf.skin=glossy")
        .run(context -> assertThat(context).getFailure().rootCause()
            .hasMessageContaining("Unknown Shadleaf skin 'glossy'"));
  }

  @Test
  void templatesPathRegistersTheLiveReloadResolver() {
    contextRunner.withPropertyValues("shadleaf.dev.templates-path=src/main/resources/templates/")
        .run(context -> assertThat(context).hasSingleBean(FileTemplateResolver.class));
  }

  @Test
  void templatesPathIsTheFirstComponentSource(@TempDir Path templates) throws Exception {
    Path components = Files.createDirectories(templates.resolve("sl/components"));
    Files.writeString(components.resolve("button.html"),
        "<html><head><sl:props><sl:prop name=\"tone\"/></sl:props></head></html>");

    contextRunner.withPropertyValues("shadleaf.dev.templates-path=" + templates)
        .run(context -> assertThat(context.getBean(ComponentRegistry.class).get("button").props())
            .containsOnlyKeys("tone"));
  }

  /**
   * The test-* components live in target/test-classes, a different classpath root from the library's
   * target/classes: to the dev mode they are the application's own templates, like an override of button.html.
   */
  @Test
  void applicationsOwnTemplateBeatsTheTemplatesPath(@TempDir Path templates) throws Exception {
    Path components = Files.createDirectories(templates.resolve("sl/components"));
    Files.writeString(components.resolve("test-chip.html"),
        "<html><head><sl:props><sl:prop name=\"tone\"/></sl:props></head></html>");
    Files.writeString(components.resolve("button.html"), "<html></html>");

    contextRunner.withPropertyValues("shadleaf.dev.templates-path=" + templates + "/").run(context -> {
      assertThat(context.getBean(ComponentRegistry.class).get("test-chip").props()).containsKey("variant");

      FileTemplateResolver resolver = context.getBean(FileTemplateResolver.class);
      IEngineConfiguration configuration = new SpringTemplateEngine().getConfiguration();
      assertThat(resolver.resolveTemplate(configuration, null, "sl/components/test-chip", null)).isNull();
      assertThat(resolver.resolveTemplate(configuration, null, "sl/components/button", null)).isNotNull();
    });
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

  @Test
  void iconSourceBeansComeBeforeTheBundledIcons() {
    IconSource appIcons = name -> name.equals("trash") ? Optional.of(Icon.lucide("<rect/>")) : Optional.empty();

    contextRunner.withBean(IconSource.class, () -> appIcons).run(context -> {
      IconRegistry icons = context.getBean(IconRegistry.class);
      assertThat(icons.get("trash").body()).isEqualTo("<rect/>");
      assertThat(icons.get("x").body()).startsWith("<path");
    });
  }

  @Test
  void writesNoWebTypesByDefault() {
    contextRunner.run(context -> assertThat(context).doesNotHaveBean(WebTypesFileWriter.class));
  }

  @Test
  void writesWebTypesOfTheApplicationsComponentsAtStartup(@TempDir Path directory) {
    Path file = directory.resolve("ide/shadleaf.web-types.json");

    contextRunner.withPropertyValues("shadleaf.dev.web-types-file=" + file).run(context -> {
      assertThat(context).hasSingleBean(WebTypesFileWriter.class);
      assertThat(file).content()
          .contains("\"name\" : \"sl:button\"")
          .contains("\"name\" : \"sl:test-chip\"");
    });
  }

  @Test
  void rewritesWebTypesWhenATemplateOnTheTemplatesPathChanges(@TempDir Path templates, @TempDir Path directory)
      throws Exception {
    Path components = Files.createDirectories(templates.resolve("sl/components"));
    Path button = components.resolve("button.html");
    Files.writeString(button, "<html><head><sl:props><sl:prop name=\"tone\"/></sl:props></head></html>");
    Path file = directory.resolve("shadleaf.web-types.json");

    contextRunner.withPropertyValues("shadleaf.dev.templates-path=" + templates,
        "shadleaf.dev.web-types-file=" + file).run(context -> {
      assertThat(file).content().contains("\"name\" : \"tone\"");
      WebTypesFileWriter writer = context.getBean(WebTypesFileWriter.class);
      assertThat(writer.write()).as("unchanged").isFalse();

      Files.writeString(button,
          "<html><head><sl:props><sl:prop name=\"tone\"/><sl:prop name=\"pill\" type=\"boolean\"/></sl:props></head></html>");

      assertThat(writer.write()).isTrue();
      assertThat(file).content().contains("\"name\" : \"pill\"");
    });
  }
}
