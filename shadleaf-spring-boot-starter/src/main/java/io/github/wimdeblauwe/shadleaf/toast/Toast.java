package io.github.wimdeblauwe.shadleaf.toast;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.annotation.JsonSerialize;

/**
 * A toast for {@code <sl:toaster>}, as a controller writes it. Serialized to JSON (by htmx-spring-boot, or any JSON
 * mapper), it is the detail of the {@code sl-toast} event the toaster listens for:
 * <pre>{@code
 * htmxResponse.addTrigger("sl-toast", Toast.success("Ada Lovelace was deleted"));
 * }</pre>
 * sends {@code HX-Trigger: {"sl-toast": {"title": "Ada Lovelace was deleted", "description": null,
 * "variant": "success", "duration": null}}}. The JSON is ASCII: every other character is escaped as a JSON unicode escape,
 * because an HTTP header holds Latin-1 at most, and Tomcat drops a header with anything else (the whole
 * {@code HX-Trigger}, events of other components included). htmx-spring-boot escapes since 5.1.1; a {@code Toast} does
 * it whatever mapper writes it, and {@link #toJson()} for a header written by hand. For a toast after a redirect, put a
 * list of them in a flash attribute and render them in the toaster:
 * <pre>{@code
 * redirectAttributes.addFlashAttribute("toasts", List.of(Toast.success("Message sent")));
 *
 * <sl:toaster>
 *   <sl:toast th:each="toast : ${toasts}" th:title="${toast.title}" th:description="${toast.description}"
 *             th:variant="${toast.variant}" th:duration="${toast.duration}"/>
 * </sl:toaster>
 * }</pre>
 *
 * @param title       what happened, in a few words
 * @param description more about it, or {@code null}
 * @param variant     {@code default}, {@code success}, {@code info}, {@code warning} or {@code error}
 * @param duration    how long it shows, in milliseconds ({@code 0}: until it is closed), or {@code null} for the
 *                    toaster's
 */
@JsonSerialize(using = Toast.AsciiJsonSerializer.class)
public record Toast(String title, @Nullable String description, String variant, @Nullable Long duration) {

  /** The values of {@code variant}, as {@code <sl:toast>} declares them. */
  public static final List<String> VARIANTS = List.of("default", "success", "info", "warning", "error");

  public Toast {
    Objects.requireNonNull(title, "title");
    Objects.requireNonNull(variant, "variant");
    if (title.isBlank()) {
      throw new IllegalArgumentException("A toast needs a title");
    }
    if (!VARIANTS.contains(variant)) {
      throw new IllegalArgumentException("Unknown toast variant '%s'; use one of %s".formatted(variant, VARIANTS));
    }
    if (duration != null && duration < 0) {
      throw new IllegalArgumentException("A toast's duration cannot be negative: " + duration);
    }
  }

  /** A toast without an icon. */
  public static Toast of(String title) {
    return new Toast(title, null, "default", null);
  }

  public static Toast success(String title) {
    return new Toast(title, null, "success", null);
  }

  public static Toast info(String title) {
    return new Toast(title, null, "info", null);
  }

  public static Toast warning(String title) {
    return new Toast(title, null, "warning", null);
  }

  public static Toast error(String title) {
    return new Toast(title, null, "error", null);
  }

  /** This toast with a description under the title. */
  public Toast withDescription(@Nullable String description) {
    return new Toast(title, description, variant, duration);
  }

  /** This toast, showing for {@code duration} instead of the toaster's duration; {@link Duration#ZERO} keeps it. */
  public Toast withDuration(Duration duration) {
    return new Toast(title, description, variant, duration.toMillis());
  }

  /** This toast, shown until it is closed. */
  public Toast untilClosed() {
    return withDuration(Duration.ZERO);
  }

  /**
   * This toast as the JSON the {@code sl-toast} event takes, with every character outside printable ASCII escaped, so
   * it can go in an HTTP header as it is. Jackson writes the same for a {@code Toast}.
   */
  public String toJson() {
    return "{\"title\":" + string(title)
        + ",\"description\":" + (description == null ? "null" : string(description))
        + ",\"variant\":" + string(variant)
        + ",\"duration\":" + (duration == null ? "null" : duration.toString())
        + "}";
  }

  private static String string(String value) {
    StringBuilder json = new StringBuilder(value.length() + 2).append('"');
    for (char character : value.toCharArray()) {
      if (character == '"' || character == '\\') {
        json.append('\\').append(character);
      } else if (character < 0x20 || character > 0x7e) {
        json.append("\\u%04x".formatted((int) character));
      } else {
        json.append(character);
      }
    }
    return json.append('"').toString();
  }

  /** Writes {@link #toJson()}, whatever the mapper's settings. */
  public static final class AsciiJsonSerializer extends ValueSerializer<Toast> {

    @Override
    public void serialize(Toast toast, JsonGenerator generator, SerializationContext context) {
      generator.writeRawValue(toast.toJson());
    }
  }

  // Bean-style accessors, so templates can write ${toast.title} with any expression language.

  public String getTitle() {
    return title;
  }

  public @Nullable String getDescription() {
    return description;
  }

  public String getVariant() {
    return variant;
  }

  public @Nullable Long getDuration() {
    return duration;
  }
}
