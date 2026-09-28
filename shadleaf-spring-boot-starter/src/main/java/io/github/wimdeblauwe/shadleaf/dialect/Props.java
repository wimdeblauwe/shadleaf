package io.github.wimdeblauwe.shadleaf.dialect;

import io.github.wimdeblauwe.shadleaf.component.ComponentDefinition;
import io.github.wimdeblauwe.shadleaf.component.PropDefinition;
import io.github.wimdeblauwe.shadleaf.component.ShadleafComponentException;
import java.math.BigDecimal;
import java.util.AbstractMap;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.jspecify.annotations.Nullable;

/**
 * The bound props of one use of a component, available in its template as {@code props}.
 * <p>
 * It is a map, so a template reads {@code ${props.variant}}; every declared prop is present, already coerced and
 * defaulted. Reading an undeclared prop fails, so a typo in the template does not silently yield {@code null}.
 */
public final class Props extends AbstractMap<String, @Nullable Object> {

  private final ComponentDefinition definition;
  private final Map<String, @Nullable Object> values;

  Props(ComponentDefinition definition, Map<String, @Nullable Object> values) {
    this.definition = definition;
    this.values = Collections.unmodifiableMap(values);
  }

  @Override
  public Set<Entry<String, @Nullable Object>> entrySet() {
    return values.entrySet();
  }

  /**
   * Returns the value of a prop, or {@code null} while it still equals the declared default.
   * <p>
   * Thymeleaf omits an attribute whose value is {@code null}, so
   * {@code th:attr="data-variant=${props.unlessDefault('variant')}"} renders nothing for the default variant and
   * keeps the markup free of noise.
   */
  public @Nullable Object unlessDefault(String name) {
    PropDefinition prop = definition.prop(name);
    if (prop == null) {
      throw new ShadleafComponentException("<sl:%s> has no prop '%s'. Declared props: %s."
          .formatted(definition.name(), name, String.join(", ", definition.props().keySet())));
    }
    Object value = values.get(name);
    return isSame(value, prop.defaultValue()) ? null : value;
  }

  private static boolean isSame(@Nullable Object value, @Nullable Object defaultValue) {
    if (value instanceof BigDecimal a && defaultValue instanceof BigDecimal b) {
      return a.compareTo(b) == 0;
    }
    return Objects.equals(value, defaultValue);
  }
}
