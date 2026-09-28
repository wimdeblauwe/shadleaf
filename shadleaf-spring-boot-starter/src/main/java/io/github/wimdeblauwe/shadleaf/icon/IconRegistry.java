package io.github.wimdeblauwe.shadleaf.icon;

import io.github.wimdeblauwe.shadleaf.component.ShadleafComponentException;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;

/**
 * Looks icons up by name in a list of {@link IconSource}s, first match wins. Available in templates as
 * {@code #slIcons}; {@code <sl:icon>} uses it to inline the SVG.
 */
public class IconRegistry {

  private static final int SUGGESTIONS = 3;

  private final List<IconSource> sources;

  public IconRegistry(List<IconSource> sources) {
    this.sources = List.copyOf(sources);
  }

  /**
   * @throws ShadleafComponentException when no source knows the name, suggesting the closest known names
   */
  public Icon get(@Nullable String name) {
    if (name == null || name.isBlank()) {
      throw new ShadleafComponentException("<sl:icon> needs a name, e.g. <sl:icon name=\"trash\"/>.");
    }
    for (IconSource source : sources) {
      Optional<Icon> icon = source.find(name);
      if (icon.isPresent()) {
        return icon.get();
      }
    }
    List<String> suggestions = suggestionsFor(name);
    throw new ShadleafComponentException("Unknown icon '%s'.%s".formatted(name, suggestions.isEmpty()
        ? ""
        : " Did you mean " + suggestions.stream().map(s -> "'" + s + "'").collect(Collectors.joining(", ")) + "?"));
  }

  private List<String> suggestionsFor(String name) {
    TreeSet<String> names = new TreeSet<>();
    sources.forEach(source -> names.addAll(source.names()));
    int maxDistance = Math.max(2, name.length() / 3);
    return names.stream()
        .map(candidate -> new Candidate(candidate, distance(name, candidate)))
        .filter(candidate -> candidate.distance() <= maxDistance || candidate.name().startsWith(name + "-"))
        .sorted(Comparator.comparingInt(Candidate::distance).thenComparing(Candidate::name))
        .limit(SUGGESTIONS)
        .map(Candidate::name)
        .toList();
  }

  private record Candidate(String name, int distance) {

  }

  /** Levenshtein distance. */
  private static int distance(String a, String b) {
    int[] previous = new int[b.length() + 1];
    int[] current = new int[b.length() + 1];
    for (int j = 0; j <= b.length(); j++) {
      previous[j] = j;
    }
    for (int i = 1; i <= a.length(); i++) {
      current[0] = i;
      for (int j = 1; j <= b.length(); j++) {
        int substitution = previous[j - 1] + (a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1);
        current[j] = Math.min(substitution, Math.min(previous[j], current[j - 1]) + 1);
      }
      int[] swap = previous;
      previous = current;
      current = swap;
    }
    return previous[b.length()];
  }
}
