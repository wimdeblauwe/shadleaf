package io.github.wimdeblauwe.shadleaf.icon;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/** Icons from {@code <name>.svg} files in one classpath directory. See {@link IconSource#classpathDirectory}. */
final class ClasspathDirectoryIconSource implements IconSource {

  // Keeps a name from reaching outside the directory ("../application").
  private static final Pattern NAME = Pattern.compile("[a-z0-9]+(-[a-z0-9]+)*");

  private final String location;
  private final ClassLoader classLoader;
  private final Map<String, Optional<Icon>> cache = new ConcurrentHashMap<>();

  ClasspathDirectoryIconSource(String location, ClassLoader classLoader) {
    String trimmed = location.startsWith("/") ? location.substring(1) : location;
    this.location = trimmed.isEmpty() || trimmed.endsWith("/") ? trimmed : trimmed + "/";
    this.classLoader = classLoader;
  }

  @Override
  public Optional<Icon> find(String name) {
    if (!NAME.matcher(name).matches()) {
      return Optional.empty();
    }
    return cache.computeIfAbsent(name, this::read);
  }

  private Optional<Icon> read(String name) {
    String resource = location + name + ".svg";
    try (InputStream inputStream = classLoader.getResourceAsStream(resource)) {
      if (inputStream == null) {
        return Optional.empty();
      }
      return Optional.of(Icon.fromSvg(new String(inputStream.readAllBytes(), StandardCharsets.UTF_8)));
    } catch (IOException e) {
      throw new UncheckedIOException("Could not read the icon " + resource, e);
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("Could not read the icon " + resource + ": " + e.getMessage(), e);
    }
  }
}
