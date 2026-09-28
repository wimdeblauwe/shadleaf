package io.github.wimdeblauwe.shadleaf.icon;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;

/**
 * One icon, as {@code <sl:icon>} inlines it: the {@code viewBox}, the markup inside the {@code <svg>} element and
 * the presentation attributes of the {@code <svg>} element itself.
 *
 * @param viewBox    the {@code viewBox} of the root element, e.g. {@code 0 0 24 24}
 * @param body       the markup inside the root element, rendered unescaped: it must come from a trusted source
 * @param attributes presentation attributes of the root element, such as {@code fill} and {@code stroke}; only the
 *                   names in {@link #PRESENTATION_ATTRIBUTES} are kept
 */
public record Icon(String viewBox, String body, Map<String, String> attributes) {

  /** The root attributes an icon may carry. Anything else on a source {@code <svg>} (size, class, id) is dropped. */
  public static final Set<String> PRESENTATION_ATTRIBUTES = Set.of(
      "fill", "stroke", "stroke-width", "stroke-linecap", "stroke-linejoin", "fill-rule", "clip-rule");

  private static final Map<String, String> LUCIDE_ATTRIBUTES = Map.of(
      "fill", "none",
      "stroke", "currentColor",
      "stroke-width", "2",
      "stroke-linecap", "round",
      "stroke-linejoin", "round");

  private static final Pattern SVG = Pattern.compile("<svg\\b([^>]*)>(.*)</svg\\s*>",
      Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
  private static final Pattern ATTRIBUTE = Pattern.compile("([\\w:-]+)\\s*=\\s*(?:\"([^\"]*)\"|'([^']*)')");
  private static final Pattern COMMENT = Pattern.compile("<!--.*?-->", Pattern.DOTALL);

  public Icon {
    Map<String, String> kept = new LinkedHashMap<>();
    attributes.forEach((name, value) -> {
      if (PRESENTATION_ATTRIBUTES.contains(name)) {
        kept.put(name, value);
      }
    });
    attributes = Map.copyOf(kept);
  }

  /** A lucide-style icon: 24&times;24, stroked in {@code currentColor} with width 2 and round caps and joins. */
  public static Icon lucide(String body) {
    return new Icon("0 0 24 24", body, LUCIDE_ATTRIBUTES);
  }

  /**
   * An icon from a complete SVG document, such as a file exported from a design tool. The root's {@code viewBox}
   * and presentation attributes are kept; its size, class and other attributes are dropped, since
   * {@code <sl:icon>} sets those.
   *
   * @throws IllegalArgumentException when {@code svg} has no {@code <svg>} element or no {@code viewBox}
   */
  public static Icon fromSvg(String svg) {
    Matcher matcher = SVG.matcher(COMMENT.matcher(svg).replaceAll(""));
    if (!matcher.find()) {
      throw new IllegalArgumentException("Not an SVG document: no <svg> element found.");
    }
    Map<String, String> attributes = new LinkedHashMap<>();
    Matcher attribute = ATTRIBUTE.matcher(matcher.group(1));
    while (attribute.find()) {
      String value = attribute.group(2) != null ? attribute.group(2) : attribute.group(3);
      attributes.put(attribute.group(1), value);
    }
    String viewBox = attributes.remove("viewBox");
    if (viewBox == null) {
      viewBox = attributes.remove("viewbox");
    }
    if (viewBox == null) {
      throw new IllegalArgumentException("The <svg> element has no viewBox, so the icon cannot be scaled.");
    }
    attributes.keySet().removeIf(name -> !PRESENTATION_ATTRIBUTES.contains(name.toLowerCase(Locale.ROOT)));
    return new Icon(viewBox, matcher.group(2).trim(), attributes);
  }

  /** The value of a presentation attribute of the root element, or {@code null} when the icon does not set it. */
  public @Nullable String attribute(String name) {
    return attributes.get(name);
  }
}
