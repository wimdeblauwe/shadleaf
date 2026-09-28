package io.github.wimdeblauwe.shadleaf.dev;

import io.github.wimdeblauwe.shadleaf.ShadleafAutoConfiguration;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.Enumeration;

/**
 * Tells whether the application overrides one of the library's templates, by shipping its own copy on the classpath
 * (ladder rung 5).
 * <p>
 * Needed in dev mode only: {@code shadleaf.dev.templates-path} reads the library's templates from disk ahead of the
 * classpath, which would otherwise hide the application's copy exactly while it is being worked on. A copy counts as
 * the application's when it comes from any classpath root other than the library's own (its jar, or
 * {@code target/classes} when the library is resolved from the workspace).
 */
public class ApplicationTemplateOverrides {

  private static final String TEMPLATES_PREFIX = "templates/";
  private static final String LIBRARY_MARKER = ShadleafAutoConfiguration.class.getName().replace('.', '/') + ".class";

  private final ClassLoader classLoader;
  private final String libraryRoot;

  public ApplicationTemplateOverrides(ClassLoader classLoader) {
    this.classLoader = classLoader;
    URL marker = classLoader.getResource(LIBRARY_MARKER);
    this.libraryRoot = marker == null ? "" : root(marker, LIBRARY_MARKER);
  }

  /**
   * @param templateName the template name as Thymeleaf resolves it, e.g. {@code sl/components/button}
   */
  public boolean isOverridden(String templateName) {
    String resource = TEMPLATES_PREFIX + templateName + (templateName.endsWith(".html") ? "" : ".html");
    try {
      Enumeration<URL> copies = classLoader.getResources(resource);
      while (copies.hasMoreElements()) {
        if (!root(copies.nextElement(), resource).equals(libraryRoot)) {
          return true;
        }
      }
      return false;
    } catch (IOException e) {
      throw new UncheckedIOException("Could not look up " + resource + " on the classpath", e);
    }
  }

  private static String root(URL url, String resource) {
    String location = url.toExternalForm();
    return location.endsWith(resource) ? location.substring(0, location.length() - resource.length()) : location;
  }
}
