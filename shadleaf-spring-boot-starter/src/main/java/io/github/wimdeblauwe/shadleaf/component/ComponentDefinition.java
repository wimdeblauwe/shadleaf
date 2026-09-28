package io.github.wimdeblauwe.shadleaf.component;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * What the engine knows about a component: its name and, when the template declares a {@code <sl:props>} block, its
 * props.
 * <p>
 * A component without a {@code <sl:props>} block is <em>undeclared</em>: it receives every attribute in the raw
 * {@code attrs} map and nothing is validated.
 *
 * @param name               the component name, e.g. {@code button} for {@code <sl:button>}
 * @param declared           whether the template has a {@code <sl:props>} block
 * @param props              the declared props by name, in declaration order
 * @param accessibleNameRule when an accessible name is required, or {@code null} when it never is
 * @param source             where the definition was read from, for error messages
 */
public record ComponentDefinition(String name, boolean declared, Map<String, PropDefinition> props,
                                  @Nullable AccessibleNameRule accessibleNameRule, String source) {

  public ComponentDefinition {
    props = Collections.unmodifiableMap(new LinkedHashMap<>(props));
  }

  public static ComponentDefinition undeclared(String name, String source) {
    return new ComponentDefinition(name, false, Map.of(), null, source);
  }

  public static ComponentDefinition declared(String name, Collection<PropDefinition> props,
      @Nullable AccessibleNameRule accessibleNameRule, String source) {
    Map<String, PropDefinition> byName = new LinkedHashMap<>();
    for (PropDefinition prop : props) {
      byName.put(prop.name(), prop);
    }
    return new ComponentDefinition(name, true, byName, accessibleNameRule, source);
  }

  public @Nullable PropDefinition prop(String name) {
    return props.get(name);
  }
}
