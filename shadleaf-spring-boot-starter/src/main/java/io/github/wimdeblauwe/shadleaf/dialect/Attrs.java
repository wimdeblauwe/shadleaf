package io.github.wimdeblauwe.shadleaf.dialect;

import java.util.AbstractMap;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.jspecify.annotations.Nullable;

/**
 * The attributes of one use of a component that no prop consumed, available in its template as {@code attrs} and
 * copied onto the rendered element by {@code sl:attrs}.
 * <p>
 * It is a map from the attribute's complete name ({@code hx-post}, {@code th:href}) to its literal value, which is
 * {@code null} for a bare attribute such as {@code hx-boost}.
 */
public final class Attrs extends AbstractMap<String, @Nullable String> {

  private static final String EXPRESSION_PREFIX = "th:";

  private final Map<String, @Nullable String> values;

  Attrs(Map<String, @Nullable String> values) {
    this.values = Collections.unmodifiableMap(new LinkedHashMap<>(values));
  }

  @Override
  public Set<Entry<String, @Nullable String>> entrySet() {
    return values.entrySet();
  }

  /**
   * Returns these attributes without the given ones, each in both spellings: {@code without('href')} drops
   * {@code href} and {@code th:href}.
   * <p>
   * For a template that must leave out an attribute in some state, e.g. a disabled anchor that must not be followed:
   * {@code sl:attrs="${props.disabled ? attrs.without('href') : attrs}"}.
   */
  public Attrs without(String... names) {
    Map<String, @Nullable String> remaining = new LinkedHashMap<>(values);
    for (String name : names) {
      remaining.remove(name);
      remaining.remove(EXPRESSION_PREFIX + name);
    }
    return new Attrs(remaining);
  }

  /**
   * Returns only the given attributes, each in both spellings: {@code only('class')} keeps {@code class} and
   * {@code th:class}.
   * <p>
   * For a template that splits the attributes over two elements, e.g. a select inside a wrapper that takes the
   * classes: {@code sl:attrs="${attrs.only('class', 'classappend')}"} on the wrapper and
   * {@code sl:attrs="${attrs.without('class', 'classappend')}"} on the select.
   */
  public Attrs only(String... names) {
    Map<String, @Nullable String> kept = new LinkedHashMap<>();
    for (String name : names) {
      for (String key : new String[] {name, EXPRESSION_PREFIX + name}) {
        if (values.containsKey(key)) {
          kept.put(key, values.get(key));
        }
      }
    }
    return new Attrs(kept);
  }
}
