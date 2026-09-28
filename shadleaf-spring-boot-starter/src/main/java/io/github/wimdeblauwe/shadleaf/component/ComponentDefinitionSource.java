package io.github.wimdeblauwe.shadleaf.component;

import java.util.Optional;
import java.util.Set;

/**
 * A place component definitions come from. The {@link ComponentRegistry} asks its sources in order and the first one
 * that knows a component wins.
 * <p>
 * By default definitions are read from the {@code <sl:props>} block of the component templates. A component with real
 * behaviour behind it can contribute its definition from Java by registering a bean of this type.
 */
public interface ComponentDefinitionSource {

  Optional<ComponentDefinition> find(String name);

  /**
   * The names of all components this source knows, used to list the alternatives when a component is unknown.
   */
  Set<String> names();
}
