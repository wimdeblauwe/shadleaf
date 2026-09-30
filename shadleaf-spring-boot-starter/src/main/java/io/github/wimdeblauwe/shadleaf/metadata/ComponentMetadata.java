package io.github.wimdeblauwe.shadleaf.metadata;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.wimdeblauwe.shadleaf.component.AccessibleNameRule;
import io.github.wimdeblauwe.shadleaf.component.ComponentDefinition;
import io.github.wimdeblauwe.shadleaf.component.ComponentRegistry;
import io.github.wimdeblauwe.shadleaf.component.PropDefinition;
import io.github.wimdeblauwe.shadleaf.dialect.ShadleafDialect;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import org.jspecify.annotations.Nullable;
import tools.jackson.databind.json.JsonMapper;

/**
 * An export of the {@link ComponentRegistry}: every component with its declared props, as plain data. Written as
 * {@code components.json} for the docs' attribute tables, and the source {@link WebTypes} are generated from.
 * <p>
 * It is taken from a live registry, so it describes what the application actually renders: an application that
 * overrides {@code button.html} and changes its props, or adds its own {@code <sl:*>} component, sees that here.
 *
 * @param version    the Shadleaf version the export was made with
 * @param components the components, sorted by name
 */
public record ComponentMetadata(String version, List<Component> components) {

  public ComponentMetadata {
    components = List.copyOf(components);
  }

  public static ComponentMetadata of(ComponentRegistry registry, String version) {
    return new ComponentMetadata(version, registry.names().stream()
        .map(registry::get)
        .map(ComponentMetadata::component)
        .toList());
  }

  /**
   * The version from the jar's manifest, or {@code dev} when the library runs from {@code target/classes}.
   */
  public static String libraryVersion() {
    String version = ComponentMetadata.class.getPackage().getImplementationVersion();
    return version == null ? "dev" : version;
  }

  public String toJson(JsonMapper jsonMapper) {
    return jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(this) + "\n";
  }

  /** Writes {@code components.json}, creating its directory; an unchanged file is left alone. */
  public void write(JsonMapper jsonMapper, Path file) {
    MetadataFiles.write(file, toJson(jsonMapper));
  }

  private static Component component(ComponentDefinition definition) {
    AccessibleNameRule rule = definition.accessibleNameRule();
    return new Component(
        definition.name(),
        ShadleafDialect.PREFIX + ":" + definition.name(),
        definition.description(),
        definition.declared(),
        definition.props().values().stream().map(ComponentMetadata::prop).toList(),
        rule == null ? null : accessibleName(definition, rule));
  }

  /** The values in the order the prop declares them, or sorted when it declares none. */
  private static AccessibleName accessibleName(ComponentDefinition definition, AccessibleNameRule rule) {
    PropDefinition prop = rule.prop() == null ? null : definition.prop(rule.prop());
    List<String> values = prop == null || prop.values().isEmpty()
        ? rule.values().stream().sorted().toList()
        : prop.values().stream().filter(rule.values()::contains).toList();
    return new AccessibleName(rule.prop(), values);
  }

  private static Prop prop(PropDefinition prop) {
    return new Prop(prop.name(), prop.type().name().toLowerCase(Locale.ROOT), text(prop.defaultValue()),
        prop.values(), prop.description(), prop.required());
  }

  private static @Nullable String text(@Nullable Object value) {
    if (value instanceof BigDecimal number) {
      return number.toPlainString();
    }
    return value == null ? null : value.toString();
  }

  /**
   * @param name           e.g. {@code button}
   * @param tag            e.g. {@code sl:button}
   * @param description    markdown; empty when the template declares none
   * @param declared       whether the template has a {@code <sl:props>} block; an undeclared component takes any
   *                       attribute
   * @param props          in declaration order
   * @param accessibleName when {@code aria-label} or {@code aria-labelledby} is required; absent when it never is
   */
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record Component(String name, String tag, String description, boolean declared, List<Prop> props,
                          @Nullable AccessibleName accessibleName) {

    public Component {
      props = List.copyOf(props);
    }
  }

  /**
   * @param name         the attribute name
   * @param type         {@code string}, {@code boolean}, {@code number} or {@code enum}
   * @param defaultValue the default as written in the template; absent when there is none
   * @param values       the legal values of an enum; empty otherwise
   * @param description  markdown
   * @param required     whether every use must set it; written only when it is
   */
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record Prop(String name, String type, @Nullable String defaultValue, List<String> values,
                     String description, @JsonInclude(JsonInclude.Include.NON_DEFAULT) boolean required) {

    public Prop {
      values = List.copyOf(values);
    }
  }

  /**
   * @param prop   the prop the requirement depends on; absent when the name is always required
   * @param values the values of {@code prop} that require it, in the order the prop declares them
   */
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record AccessibleName(@Nullable String prop, List<String> values) {

    public AccessibleName {
      values = List.copyOf(values);
    }

    /** One sentence for a description, e.g. "Requires `aria-label` ... when `size` is `icon` or `icon-sm`." */
    public String sentence() {
      String requirement = "Requires `aria-label` or `aria-labelledby`";
      if (prop == null) {
        return requirement + ".";
      }
      List<String> quoted = values.stream().map(value -> "`" + value + "`").toList();
      String alternatives = quoted.size() == 1 ? quoted.get(0)
          : String.join(", ", quoted.subList(0, quoted.size() - 1)) + " or " + quoted.get(quoted.size() - 1);
      return requirement + " when `" + prop + "` is " + alternatives + ".";
    }
  }
}
