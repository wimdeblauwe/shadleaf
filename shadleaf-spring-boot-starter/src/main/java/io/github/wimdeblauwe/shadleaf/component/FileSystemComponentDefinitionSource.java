package io.github.wimdeblauwe.shadleaf.component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * Reads component templates from a directory on disk, for live reload while working on the library
 * ({@code shadleaf.dev.templates-path}).
 * <p>
 * Nothing is cached beyond the current version of each file: a template is parsed again as soon as its modification
 * time or size changes, so editing a prop list shows up on the next request, just like editing the markup does.
 */
public class FileSystemComponentDefinitionSource implements ComponentDefinitionSource {

  private final Path componentsDirectory;
  private final Map<String, CachedDefinition> cache = new ConcurrentHashMap<>();

  /**
   * @param componentsDirectory the directory holding the component templates, e.g.
   *                            {@code src/main/resources/templates/sl/components}
   */
  public FileSystemComponentDefinitionSource(Path componentsDirectory) {
    this.componentsDirectory = componentsDirectory;
  }

  /**
   * @param templatesPath the value of {@code shadleaf.dev.templates-path}: the library's templates root
   */
  public static FileSystemComponentDefinitionSource forTemplatesPath(String templatesPath) {
    return new FileSystemComponentDefinitionSource(Path.of(templatesPath, "sl", "components"));
  }

  @Override
  public Optional<ComponentDefinition> find(String name) {
    Path file = componentsDirectory.resolve(name + ".html");
    if (!file.normalize().startsWith(componentsDirectory.normalize()) || !Files.isRegularFile(file)) {
      cache.remove(name);
      return Optional.empty();
    }
    try {
      FileTime modified = Files.getLastModifiedTime(file);
      long size = Files.size(file);
      CachedDefinition cached = cache.get(name);
      if (cached != null && cached.modified().equals(modified) && cached.size() == size) {
        return Optional.of(cached.definition());
      }
      ComponentDefinition definition = PropsParser.parseAndClose(name,
          Files.newBufferedReader(file, StandardCharsets.UTF_8), file.toString());
      cache.put(name, new CachedDefinition(modified, size, definition));
      return Optional.of(definition);
    } catch (IOException e) {
      throw new UncheckedIOException("Could not read component template " + file, e);
    }
  }

  @Override
  public Set<String> names() {
    if (!Files.isDirectory(componentsDirectory)) {
      return Set.of();
    }
    try (Stream<Path> files = Files.list(componentsDirectory)) {
      Set<String> names = new TreeSet<>();
      files.map(file -> file.getFileName().toString())
          .filter(filename -> filename.endsWith(".html"))
          .forEach(filename -> names.add(filename.substring(0, filename.length() - ".html".length())));
      return names;
    } catch (IOException e) {
      throw new UncheckedIOException("Could not list component templates in " + componentsDirectory, e);
    }
  }

  private record CachedDefinition(FileTime modified, long size, ComponentDefinition definition) {

  }
}
