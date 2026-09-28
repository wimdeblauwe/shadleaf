package io.github.wimdeblauwe.shadleaf.icon;

import java.util.Optional;
import java.util.Set;

/**
 * Where {@code <sl:icon name="...">} finds its icons.
 * <p>
 * An application adds its own icons, or replaces a bundled one, by declaring an {@code IconSource} bean. Beans are
 * asked in {@link org.springframework.core.annotation.Order @Order} and before the bundled lucide catalogue; the
 * first source that knows a name wins.
 * <pre>{@code
 * @Bean
 * IconSource appIcons() {
 *   return IconSource.classpathDirectory("icons/"); // src/main/resources/icons/logo.svg -> <sl:icon name="logo"/>
 * }
 * }</pre>
 */
public interface IconSource {

  Optional<Icon> find(String name);

  /**
   * The names this source knows, used to suggest alternatives for an unknown name. A source that cannot list them
   * returns an empty set.
   */
  default Set<String> names() {
    return Set.of();
  }

  /**
   * Icons read from {@code <name>.svg} files in a classpath directory, e.g. {@code icons/}. Each file is read on
   * first use and kept. Names are limited to lower-case letters, digits and dashes.
   */
  static IconSource classpathDirectory(String location) {
    return new ClasspathDirectoryIconSource(location, Thread.currentThread().getContextClassLoader());
  }
}
