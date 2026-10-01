package io.github.wimdeblauwe.shadleaf.metadata;

import io.github.wimdeblauwe.shadleaf.dialect.ShadleafDialect;
import io.github.wimdeblauwe.shadleaf.metadata.ComponentMetadata.Component;
import io.github.wimdeblauwe.shadleaf.metadata.ComponentMetadata.Prop;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import tools.jackson.databind.json.JsonMapper;

/**
 * Generates a JetBrains <a href="https://github.com/JetBrains/web-types">web-types</a> file from
 * {@link ComponentMetadata}, so IntelliJ IDEA and WebStorm complete {@code <sl:button>}, its attributes and their
 * values. The IDE finds the file through the {@code web-types} property of a {@code package.json} next to it.
 * <p>
 * Everything comes from the declared props, nothing is inferred: {@code values} is the enum, {@code default} the
 * default, a {@code boolean} prop is an attribute without a value (or {@code true}/{@code false} when it defaults to
 * true), and the prop's text is the description. Every prop
 * is offered a second time as {@code th:<prop>}, for a value computed by an expression.
 */
public final class WebTypes {

  public static final String SCHEMA = "https://raw.githubusercontent.com/JetBrains/web-types/master/schema/web-types.json";

  private WebTypes() {
  }

  public static Map<String, Object> of(ComponentMetadata metadata) {
    List<Object> elements = new ArrayList<>();
    elements.add(slotElement());
    for (Component component : metadata.components()) {
      elements.add(element(component));
    }
    Map<String, Object> webTypes = new LinkedHashMap<>();
    webTypes.put("$schema", SCHEMA);
    webTypes.put("name", "shadleaf");
    webTypes.put("version", metadata.version());
    webTypes.put("description-markup", "markdown");
    webTypes.put("contributions", Map.of("html", Map.of("elements", elements)));
    return webTypes;
  }

  public static String toJson(ComponentMetadata metadata, JsonMapper jsonMapper) {
    return jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(of(metadata)) + "\n";
  }

  /**
   * Writes the file, creating its directory; an unchanged file is left alone.
   *
   * @return whether the file was written
   */
  public static boolean write(ComponentMetadata metadata, JsonMapper jsonMapper, Path file) {
    return MetadataFiles.write(file, toJson(metadata, jsonMapper));
  }

  private static Map<String, Object> element(Component component) {
    Map<String, Object> element = new LinkedHashMap<>();
    element.put("name", component.tag());
    String description = component.description();
    if (component.accessibleName() != null) {
      description = (description.isEmpty() ? "" : description + " ") + component.accessibleName().sentence();
    }
    if (!description.isEmpty()) {
      element.put("description", description);
    }
    List<Object> attributes = new ArrayList<>();
    for (Prop prop : component.props()) {
      // An object prop can only be set with th:<prop>; its plain attribute would fail.
      if (!prop.type().equals("object")) {
        attributes.add(attribute(prop));
      }
    }
    for (Prop prop : component.props()) {
      attributes.add(expressionAttribute(prop));
    }
    element.put("attributes", attributes);
    return element;
  }

  private static Map<String, Object> attribute(Prop prop) {
    Map<String, Object> attribute = new LinkedHashMap<>();
    attribute.put("name", prop.name());
    if (!prop.description().isEmpty()) {
      attribute.put("description", prop.description());
    }
    // A boolean that is on by default can only be switched off with ="false", so it takes the two values instead.
    boolean onByDefault = prop.type().equals("boolean") && "true".equals(prop.defaultValue());
    Map<String, Object> value = new LinkedHashMap<>();
    switch (prop.type()) {
      case "boolean" -> {
        if (onByDefault) {
          value.put("kind", "plain");
          value.put("type", "enum");
        } else {
          value.put("kind", "no-value");
        }
      }
      case "enum" -> {
        value.put("kind", "plain");
        value.put("type", "enum");
      }
      case "number" -> {
        value.put("kind", "plain");
        value.put("type", "number");
      }
      default -> value.put("kind", "plain");
    }
    value.put("required", false);
    attribute.put("value", value);
    // A required prop is not marked "required" here: it can be given as th:<prop> instead, which the IDE would not
    // count, so it would flag every use that sets it with an expression.
    if (!prop.values().isEmpty()) {
      attribute.put("values", prop.values().stream().map(name -> Map.of("name", name)).toList());
    } else if (onByDefault) {
      attribute.put("values", List.of(Map.of("name", "true"), Map.of("name", "false")));
    }
    if (prop.defaultValue() != null && (!prop.type().equals("boolean") || onByDefault)) {
      attribute.put("default", prop.defaultValue());
    }
    return attribute;
  }

  private static Map<String, Object> expressionAttribute(Prop prop) {
    Map<String, Object> attribute = new LinkedHashMap<>();
    attribute.put("name", "th:" + prop.name());
    // An object prop has no plain attribute to carry its description, so this one does.
    attribute.put("description", prop.type().equals("object") && !prop.description().isEmpty()
        ? prop.description()
        : "`" + prop.name() + "` from a Thymeleaf expression, e.g. `th:" + prop.name() + "=\"${...}\"`.");
    attribute.put("value", value("expression"));
    return attribute;
  }

  private static Map<String, Object> value(String kind) {
    Map<String, Object> value = new LinkedHashMap<>();
    value.put("kind", kind);
    value.put("required", false);
    return value;
  }

  private static Map<String, Object> slotElement() {
    Map<String, Object> name = new LinkedHashMap<>();
    name.put("name", "name");
    name.put("description", "The named slot to fill, e.g. `icon-start`. Omit it for the default slot.");
    name.put("value", value("plain"));

    Map<String, Object> element = new LinkedHashMap<>();
    element.put("name", ShadleafDialect.PREFIX + ":slot");
    element.put("description", "Content for a named slot of the enclosing component. Inside a component template: "
        + "where that content goes, with this element's body as the fallback.");
    element.put("attributes", List.of(name));
    return element;
  }
}
