package io.github.wimdeblauwe.shadleaf.metadata;

import static io.github.wimdeblauwe.shadleaf.test.ShadleafAssertions.assertThat;

import io.github.wimdeblauwe.shadleaf.component.ComponentDefinition;
import io.github.wimdeblauwe.shadleaf.component.ComponentDefinitionSource;
import io.github.wimdeblauwe.shadleaf.component.ComponentRegistry;
import io.github.wimdeblauwe.shadleaf.component.PropsParser;
import io.github.wimdeblauwe.shadleaf.test.HtmlApproval;
import io.github.wimdeblauwe.shadleaf.test.LibraryComponents;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class WebTypesTest {

  private final JsonMapper jsonMapper = JsonMapper.builder().build();

  @Test
  void libraryComponents() {
    ComponentMetadata metadata = ComponentMetadata.of(LibraryComponents.registry(), "test");

    HtmlApproval.verify("components", "json", metadata.toJson(jsonMapper));
    HtmlApproval.verify("web-types", "json", WebTypes.toJson(metadata, jsonMapper));
  }

  @Test
  void mapsPropTypesToAttributeValues() {
    JsonNode element = element("""
        <sl:props>
          <sl:description>A <code>chip</code>.</sl:description>
          <sl:prop name="variant" default="plain" values="plain bold">Style.</sl:prop>
          <sl:prop name="removable" type="boolean">Show a remove button.</sl:prop>
          <sl:prop name="count" type="number" default="1.50"/>
          <sl:prop name="label"/>
        </sl:props>
        """);

    assertThat(element.get("name").asString()).isEqualTo("sl:chip");
    assertThat(element.get("description").asString()).isEqualTo("A `chip`.");
    JsonNode variant = attribute(element, "variant");
    assertThat(variant.get("value").get("type").asString()).isEqualTo("enum");
    assertThat(variant.get("values").findValuesAsString("name")).containsExactly("plain", "bold");
    assertThat(variant.get("default").asString()).isEqualTo("plain");
    JsonNode removable = attribute(element, "removable");
    assertThat(removable.get("value").get("kind").asString()).isEqualTo("no-value");
    assertThat(removable.has("default")).isFalse();
    assertThat(attribute(element, "count").get("default").asString()).isEqualTo("1.50");
    JsonNode label = attribute(element, "label");
    assertThat(label.get("value").get("kind").asString()).isEqualTo("plain");
    assertThat(label.has("description")).isFalse();
    assertThat(attribute(element, "th:variant").get("value").get("kind").asString()).isEqualTo("expression");
  }

  @Test
  void accessibleNameRuleIsPartOfTheDescription() {
    JsonNode element = element("""
        <sl:props>
          <sl:prop name="size" values="default icon icon-sm"/>
          <sl:accessible-name required-when="size=icon-sm icon"/>
        </sl:props>
        """);

    assertThat(element.get("description").asString())
        .isEqualTo("Requires `aria-label` or `aria-labelledby` when `size` is `icon` or `icon-sm`.");
  }

  @Test
  void undeclaredComponentHasNoAttributes() {
    ComponentMetadata metadata = ComponentMetadata.of(registry(ComponentDefinition.undeclared("raw", "raw.html")),
        "test");

    Map<String, Object> webTypes = WebTypes.of(metadata);

    JsonNode element = jsonMapper.valueToTree(webTypes).get("contributions").get("html").get("elements").get(1);
    assertThat(element.get("name").asString()).isEqualTo("sl:raw");
    assertThat(element.get("attributes")).isEmpty();
    assertThat(metadata.components().get(0).declared()).isFalse();
  }

  private JsonNode element(String propsBlock) {
    ComponentDefinition definition = PropsParser.parse("chip", "<html><head>" + propsBlock + "</head></html>",
        "chip.html");
    JsonNode webTypes = jsonMapper.valueToTree(WebTypes.of(ComponentMetadata.of(registry(definition), "test")));
    JsonNode elements = webTypes.get("contributions").get("html").get("elements");
    assertThat(elements.get(0).get("name").asString()).isEqualTo("sl:slot");
    return elements.get(1);
  }

  private static JsonNode attribute(JsonNode element, String name) {
    for (JsonNode attribute : element.get("attributes")) {
      if (attribute.get("name").asString().equals(name)) {
        return attribute;
      }
    }
    throw new AssertionError("No attribute " + name + " in " + element);
  }

  private static ComponentRegistry registry(ComponentDefinition definition) {
    return new ComponentRegistry(List.of(new ComponentDefinitionSource() {
      @Override
      public Optional<ComponentDefinition> find(String name) {
        return name.equals(definition.name()) ? Optional.of(definition) : Optional.empty();
      }

      @Override
      public Set<String> names() {
        return Set.of(definition.name());
      }
    }));
  }
}
