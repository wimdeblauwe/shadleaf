package io.github.wimdeblauwe.shadleaf.component;

import java.util.Set;
import org.jspecify.annotations.Nullable;

/**
 * Declares that a component must be given an accessible name ({@code aria-label} or {@code aria-labelledby}),
 * either always or only while one prop has one of a set of values.
 * <p>
 * Declared as {@code <sl:accessible-name required-when="size=icon icon-sm"/>} inside {@code <sl:props>}, or as
 * {@code <sl:accessible-name/>} when the name is always required.
 *
 * @param prop   the prop the requirement depends on; {@code null} when the name is always required
 * @param values the values of {@code prop} for which the name is required
 */
public record AccessibleNameRule(@Nullable String prop, Set<String> values) {

  public AccessibleNameRule {
    values = Set.copyOf(values);
  }

  public static AccessibleNameRule always() {
    return new AccessibleNameRule(null, Set.of());
  }

  /**
   * @param propValue the coerced value of {@link #prop()} for this use of the component
   */
  public boolean appliesTo(@Nullable Object propValue) {
    return prop == null || (propValue != null && values.contains(String.valueOf(propValue)));
  }
}
