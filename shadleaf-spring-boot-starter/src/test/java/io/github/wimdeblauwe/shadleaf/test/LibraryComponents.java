package io.github.wimdeblauwe.shadleaf.test;

import io.github.wimdeblauwe.shadleaf.component.ClasspathComponentDefinitionSource;
import io.github.wimdeblauwe.shadleaf.component.ComponentRegistry;
import java.util.List;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

/**
 * The library's own components, read from {@code target/classes}. The test classpath also holds the test-only
 * {@code test-*} components, which must not reach the docs or the IDE metadata.
 */
public final class LibraryComponents {

  public static final String LOCATION_PATTERN = "file:target/classes/templates/sl/components/*.html";

  private LibraryComponents() {
  }

  public static ComponentRegistry registry() {
    ComponentRegistry registry = new ComponentRegistry(List.of(new ClasspathComponentDefinitionSource(
        new PathMatchingResourcePatternResolver(), LOCATION_PATTERN)));
    if (registry.names().isEmpty()) {
      throw new IllegalStateException("No components at " + LOCATION_PATTERN + "; run the build first.");
    }
    return registry;
  }
}
