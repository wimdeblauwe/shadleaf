package io.github.wimdeblauwe.shadleaf.component;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;

/**
 * Reads the component templates under {@code templates/sl/components/} on the classpath, once, when it is created.
 * <p>
 * Every template is parsed up front, so a broken {@code <sl:props>} block fails the application at startup rather
 * than on the first page that uses the component. When the same template exists more than once on the classpath, the
 * first one wins, exactly as it does for Thymeleaf's own template resolution: an application's copy of
 * {@code button.html} overrides the library's, props included.
 */
public class ClasspathComponentDefinitionSource implements ComponentDefinitionSource {

  public static final String DEFAULT_LOCATION_PATTERN = "classpath*:templates/sl/components/*.html";

  private final Map<String, ComponentDefinition> definitions;

  public ClasspathComponentDefinitionSource(ClassLoader classLoader) {
    this(new PathMatchingResourcePatternResolver(classLoader), DEFAULT_LOCATION_PATTERN);
  }

  public ClasspathComponentDefinitionSource(ResourcePatternResolver resolver, String locationPattern) {
    this.definitions = Collections.unmodifiableMap(scan(resolver, locationPattern));
  }

  @Override
  public Optional<ComponentDefinition> find(String name) {
    return Optional.ofNullable(definitions.get(name));
  }

  @Override
  public Set<String> names() {
    return definitions.keySet();
  }

  private static Map<String, ComponentDefinition> scan(ResourcePatternResolver resolver, String locationPattern) {
    Map<String, ComponentDefinition> definitions = new LinkedHashMap<>();
    try {
      for (Resource resource : resolver.getResources(locationPattern)) {
        String filename = resource.getFilename();
        if (filename == null || !filename.endsWith(".html")) {
          continue;
        }
        String name = filename.substring(0, filename.length() - ".html".length());
        if (definitions.containsKey(name)) {
          continue;
        }
        String source = resource.getDescription();
        definitions.put(name, PropsParser.parseAndClose(name,
            new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8), source));
      }
    } catch (IOException e) {
      throw new UncheckedIOException("Could not read the component templates at " + locationPattern, e);
    }
    return definitions;
  }
}
