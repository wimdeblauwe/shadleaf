package io.github.wimdeblauwe.shadleaf.component;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ComponentRegistryTest {

  private final ClasspathComponentDefinitionSource classpath =
      new ClasspathComponentDefinitionSource(getClass().getClassLoader());

  @Test
  void scansComponentTemplatesOnTheClasspath() {
    ComponentRegistry registry = new ComponentRegistry(List.of(classpath));

    assertThat(registry.names()).contains("test-chip", "test-card", "test-raw", "test-dot");
    ComponentDefinition chip = registry.get("test-chip");
    assertThat(chip.declared()).isTrue();
    assertThat(chip.props().keySet()).containsExactly("variant", "size", "removable", "count", "label");
    assertThat(chip.source()).contains("test-chip.html");
    assertThat(registry.get("test-raw").declared()).isFalse();
  }

  @Test
  void unknownComponentListsTheRegisteredOnes() {
    ComponentRegistry registry = new ComponentRegistry(List.of(new FixedSource("button"), new FixedSource("icon")));

    assertThatThrownBy(() -> registry.get("buton"))
        .isInstanceOf(ShadleafComponentException.class)
        .hasMessage("Unknown component <sl:buton>. Registered components: button, icon. "
            + "A component is a template at templates/sl/components/<name>.html.");
  }

  @Test
  void firstSourceWins() {
    ComponentDefinition fromFirst = ComponentDefinition.undeclared("button", "first");
    ComponentRegistry registry = new ComponentRegistry(List.of(
        new FixedSource(fromFirst), new FixedSource(ComponentDefinition.undeclared("button", "second"))));

    assertThat(registry.get("button")).isSameAs(fromFirst);
  }

  @Test
  void fileSystemSourcePicksUpEditsWithoutRestart(@TempDir Path templates) throws IOException {
    Path components = Files.createDirectories(templates.resolve("sl/components"));
    Path button = components.resolve("button.html");
    Files.writeString(button, template("<sl:prop name=\"variant\" values=\"primary outline\"/>"));
    FileSystemComponentDefinitionSource source =
        new FileSystemComponentDefinitionSource(templates.resolve("sl/components"));

    ComponentDefinition first = source.find("button").orElseThrow();
    assertThat(first.prop("variant").values()).containsExactly("primary", "outline");
    assertThat(source.find("button").orElseThrow()).as("unchanged file is not parsed again").isSameAs(first);

    Files.writeString(button, template("<sl:prop name=\"variant\" values=\"primary outline ghost\"/>"));
    Files.setLastModifiedTime(button, FileTime.from(Instant.now().plusSeconds(5)));

    assertThat(source.find("button").orElseThrow().prop("variant").values())
        .containsExactly("primary", "outline", "ghost");
    assertThat(source.names()).containsExactly("button");

    Files.delete(button);
    assertThat(source.find("button")).isEmpty();
    assertThat(source.names()).isEmpty();
  }

  @Test
  void fileSystemSourceComesBeforeTheClasspath(@TempDir Path templates) throws IOException {
    Path components = Files.createDirectories(templates.resolve("sl/components"));
    Files.writeString(components.resolve("test-chip.html"), template("<sl:prop name=\"tone\"/>"));
    ComponentRegistry registry = new ComponentRegistry(List.of(
        new FileSystemComponentDefinitionSource(templates.resolve("sl/components")), classpath));

    assertThat(registry.get("test-chip").props()).containsOnlyKeys("tone");
    assertThat(registry.get("test-card").declared()).isTrue();
  }

  @Test
  void fileSystemSourceIgnoresMissingDirectory(@TempDir Path templates) {
    FileSystemComponentDefinitionSource source =
        new FileSystemComponentDefinitionSource(templates.resolve("nope").resolve("sl/components"));

    assertThat(source.names()).isEmpty();
    assertThat(source.find("button")).isEmpty();
    assertThat(source.find("../../etc/passwd")).isEmpty();
  }

  private static String template(String props) {
    return "<html><head><sl:props>" + props + "</sl:props></head><body><button th:fragment=\"button\"></button></body></html>";
  }

  private record FixedSource(ComponentDefinition definition) implements ComponentDefinitionSource {

    FixedSource(String name) {
      this(ComponentDefinition.undeclared(name, name + ".html"));
    }

    @Override
    public Optional<ComponentDefinition> find(String name) {
      return definition.name().equals(name) ? Optional.of(definition) : Optional.empty();
    }

    @Override
    public Set<String> names() {
      return Set.of(definition.name());
    }
  }
}
