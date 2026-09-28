package io.github.wimdeblauwe.shadleaf.component;

import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * A single prop declared in a component's {@code <sl:props>} block.
 *
 * @param name         the attribute name, e.g. {@code variant}
 * @param type         how the attribute value is coerced
 * @param defaultValue the value used when the attribute is absent, already coerced; {@code null} when there is none
 * @param values       the legal values of an {@link PropType#ENUM} prop, in declaration order; empty otherwise
 * @param description  the element's text, used for documentation and IDE metadata
 */
public record PropDefinition(String name, PropType type, @Nullable Object defaultValue, List<String> values,
                             String description) {

  public PropDefinition {
    values = List.copyOf(values);
  }
}
