package io.github.wimdeblauwe.shadleaf.component;

import java.util.List;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * All components the engine can render, looked up by name across an ordered list of
 * {@link ComponentDefinitionSource}s. The first source that knows a component wins.
 * <p>
 * The element processor resolves every {@code <sl:*>} tag through here, and later the IDE metadata and the docs
 * attribute tables are generated from it.
 */
public class ComponentRegistry {

  private final List<ComponentDefinitionSource> sources;

  public ComponentRegistry(List<ComponentDefinitionSource> sources) {
    this.sources = List.copyOf(sources);
  }

  public Optional<ComponentDefinition> find(String name) {
    for (ComponentDefinitionSource source : sources) {
      Optional<ComponentDefinition> definition = source.find(name);
      if (definition.isPresent()) {
        return definition;
      }
    }
    return Optional.empty();
  }

  /**
   * @throws ShadleafComponentException when no source knows the component; the message lists the registered ones
   */
  public ComponentDefinition get(String name) {
    return find(name).orElseThrow(() -> new ShadleafComponentException(
        "Unknown component <sl:%s>. Registered components: %s. A component is a template at templates/sl/components/<name>.html."
            .formatted(name, names().isEmpty() ? "(none)" : String.join(", ", names()))));
  }

  public SortedSet<String> names() {
    SortedSet<String> names = new TreeSet<>();
    for (ComponentDefinitionSource source : sources) {
      names.addAll(source.names());
    }
    return names;
  }
}
