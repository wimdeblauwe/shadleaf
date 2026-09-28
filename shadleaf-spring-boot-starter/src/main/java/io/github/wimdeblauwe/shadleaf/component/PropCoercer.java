package io.github.wimdeblauwe.shadleaf.component;

import java.math.BigDecimal;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Turns an attribute value (a literal string, or the result of a {@code th:<prop>} expression) into the typed value
 * the component template sees as {@code ${props.<name>}}.
 */
public final class PropCoercer {

  private PropCoercer() {
  }

  /**
   * Binds one prop for one use of a component.
   *
   * @param componentName the component, for error messages
   * @param prop          the declared prop
   * @param raw           the attribute value, or {@code null} when the attribute is absent or its expression
   *                      evaluated to {@code null}
   * @return the coerced value, or the prop's default when {@code raw} is {@code null}
   */
  public static @Nullable Object bind(String componentName, PropDefinition prop, @Nullable Object raw) {
    if (raw == null) {
      return prop.defaultValue();
    }
    return coerce(componentName, prop.name(), prop.type(), prop.values(), raw);
  }

  static Object coerce(String componentName, String propName, PropType type, List<String> values, Object raw) {
    return switch (type) {
      case STRING -> String.valueOf(raw);
      case BOOLEAN -> coerceBoolean(componentName, propName, raw);
      case NUMBER -> coerceNumber(componentName, propName, raw);
      case ENUM -> coerceEnum(componentName, propName, values, raw);
    };
  }

  private static Boolean coerceBoolean(String componentName, String propName, Object raw) {
    if (raw instanceof Boolean b) {
      return b;
    }
    String value = String.valueOf(raw).trim();
    // HTML style: <sl:x disabled>, <sl:x disabled="disabled">
    if (value.isEmpty() || value.equals("true") || value.equals(propName)) {
      return Boolean.TRUE;
    }
    if (value.equals("false")) {
      return Boolean.FALSE;
    }
    throw new ShadleafComponentException(
        "Invalid value '%s' for boolean prop '%s' of <sl:%s>. Use %s, %s=\"true\" or %s=\"false\"."
            .formatted(raw, propName, componentName, propName, propName, propName));
  }

  private static BigDecimal coerceNumber(String componentName, String propName, Object raw) {
    if (raw instanceof BigDecimal bigDecimal) {
      return bigDecimal;
    }
    try {
      return new BigDecimal(String.valueOf(raw).trim());
    } catch (NumberFormatException e) {
      throw new ShadleafComponentException(
          "Invalid value '%s' for number prop '%s' of <sl:%s>.".formatted(raw, propName, componentName), e);
    }
  }

  private static String coerceEnum(String componentName, String propName, List<String> values, Object raw) {
    String value = String.valueOf(raw);
    if (!values.contains(value)) {
      throw new ShadleafComponentException(
          "Invalid value '%s' for prop '%s' of <sl:%s>. Must be one of: %s."
              .formatted(value, propName, componentName, String.join(", ", values)));
    }
    return value;
  }
}
