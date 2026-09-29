package io.github.wimdeblauwe.shadleaf.icon;

import io.github.wimdeblauwe.shadleaf.component.ShadleafComponentException;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
  private static final String DEFAULT_SIZE = "24";
  /** The presentation attributes in the order {@code <sl:icon>} renders them. */
  private static final List<String> PRESENTATION_ORDER = List.of(
      "fill", "stroke", "stroke-width", "stroke-linecap", "stroke-linejoin", "fill-rule", "clip-rule");

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

  /**
   * The attributes of the rendered {@code <svg>} element, for {@code sl:attrs} in {@code icon.html}: the icon's
   * {@code viewBox}, the size, its presentation attributes and the accessibility attributes, followed by the
   * attributes passed to the component, which override them. Computed here in one call rather than as a dozen
   * expressions in the template, which made the icon the most expensive component to render.
   *
   * @param size        the {@code size} prop; without it the icon is 24 pixels and has no {@code data-icon-size}
   * @param strokeWidth the {@code stroke-width} prop, overriding the icon's own
   * @param label       the {@code label} prop; without it (or when blank) the icon is decorative and hidden from
   *                    assistive technology
   * @param attrs       the attributes of the component tag no prop consumed
   */
  public Map<String, @Nullable String> svgAttributes(Icon icon, @Nullable Object size, @Nullable Object strokeWidth,
      @Nullable String label, Map<String, @Nullable String> attrs) {
    Map<String, @Nullable String> attributes = new LinkedHashMap<>();
    attributes.put("viewBox", icon.viewBox());
    String width = size == null ? DEFAULT_SIZE : size.toString();
    attributes.put("width", width);
    attributes.put("height", width);
    if (size != null) {
      // Marks an explicitly sized icon, so a component's default icon size (the button's svg rule) leaves it alone.
      attributes.put("data-icon-size", width);
    }
    for (String name : PRESENTATION_ORDER) {
      String value = name.equals("stroke-width") && strokeWidth != null
          ? strokeWidth.toString()
          : icon.attribute(name);
      if (value != null) {
        attributes.put(name, value);
      }
    }
    if (label != null && !label.isBlank()) {
      attributes.put("role", "img");
      attributes.put("aria-label", label);
    } else {
      attributes.put("aria-hidden", "true");
      attributes.put("focusable", "false");
    }
    attributes.putAll(attrs);
    return attributes;
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
