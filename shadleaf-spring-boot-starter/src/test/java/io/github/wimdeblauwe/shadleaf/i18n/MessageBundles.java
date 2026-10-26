package io.github.wimdeblauwe.shadleaf.i18n;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;

/**
 * The bundles of {@link ShadleafMessageSource} as files in the sources: {@code messages.properties} (English) and every
 * {@code messages_<language>.properties} next to it.
 */
public final class MessageBundles {

  public static final Path DIRECTORY = Path.of("src", "main", "resources", "shadleaf");
  public static final String ENGLISH = "en";

  private static final Pattern TRANSLATION = Pattern.compile("messages_([a-zA-Z_]+)\\.properties");

  private MessageBundles() {
  }

  /** Each bundle's messages by language, English first, then the translations in alphabetical order. */
  public static Map<String, Properties> all() throws IOException {
    Map<String, Properties> bundles = new LinkedHashMap<>();
    bundles.put(ENGLISH, load(DIRECTORY.resolve("messages.properties")));
    Map<String, Properties> translations = new TreeMap<>();
    try (Stream<Path> files = Files.list(DIRECTORY)) {
      for (Path file : files.toList()) {
        Matcher matcher = TRANSLATION.matcher(file.getFileName().toString());
        if (matcher.matches()) {
          translations.put(matcher.group(1), load(file));
        }
      }
    }
    bundles.putAll(translations);
    return bundles;
  }

  /**
   * The keys of {@code messages.properties} in the order of the file, each with the comment right above it, if any. A
   * comment followed by a blank line belongs to no key (the file's header).
   */
  public static List<Key> keys() throws IOException {
    List<Key> keys = new ArrayList<>();
    List<String> comment = new ArrayList<>();
    for (String line : Files.readAllLines(DIRECTORY.resolve("messages.properties"), StandardCharsets.UTF_8)) {
      String trimmed = line.strip();
      if (trimmed.isEmpty()) {
        comment.clear();
      } else if (trimmed.startsWith("#")) {
        comment.add(trimmed.replaceFirst("^#\\s?", ""));
      } else {
        Properties property = load(new StringReader(line));
        String key = property.stringPropertyNames().iterator().next();
        keys.add(new Key(key, comment.isEmpty() ? null : String.join(" ", comment)));
        comment.clear();
      }
    }
    return keys;
  }

  private static Properties load(Path file) throws IOException {
    try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
      return load(reader);
    }
  }

  private static Properties load(Reader reader) throws IOException {
    Properties properties = new Properties();
    properties.load(reader);
    return properties;
  }

  /**
   * @param comment the comment lines right above the key, joined with spaces
   */
  public record Key(String name, @Nullable String comment) {

  }
}
