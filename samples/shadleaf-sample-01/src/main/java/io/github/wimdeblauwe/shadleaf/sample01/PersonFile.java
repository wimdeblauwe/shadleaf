package io.github.wimdeblauwe.shadleaf.sample01;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;

/**
 * A file kept for a person, as an application would keep uploads: here three per person, made up from their details
 * (notes, their orders as CSV, a photo from the classpath), so the person page has files that are already on the server
 * to show as {@code sl:attachment}s.
 */
public record PersonFile(String name, String type, MediaType mediaType, String icon, byte[] content) {

  private static final DateTimeFormatter JOINED = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.ENGLISH);

  /** The files of a person, in the order the person page lists them. */
  public static List<PersonFile> of(Person person) {
    return List.of(
        new PersonFile("onboarding-notes.txt", "Text", MediaType.TEXT_PLAIN, "file-text", """
            Onboarding notes for %s

            Email: %s
            Role: %s
            Joined: %s
            """.formatted(person.getName(), person.getEmail(), person.getRole().getLabel(),
            JOINED.format(person.getJoined())).getBytes(StandardCharsets.UTF_8)),
        new PersonFile("orders.csv", "CSV", MediaType.parseMediaType("text/csv"), "table", orders(person)),
        new PersonFile("team-offsite.jpg", "JPG", MediaType.IMAGE_JPEG, "image", photo()));
  }

  /** Whether the person page shows a thumbnail of the file, and opens it in a dialog. */
  public boolean image() {
    return mediaType.getType().equals("image");
  }

  /** The size as people read it: {@code 820 B}, {@code 1.4 KB}. */
  public String size() {
    if (content.length < 1024) {
      return content.length + " B";
    }
    return String.format(Locale.ENGLISH, "%.1f KB", content.length / 1024.0);
  }

  private static byte[] orders(Person person) {
    StringBuilder csv = new StringBuilder("order,person,items\n");
    for (int order = 1; order <= person.getOrders(); order++) {
      csv.append("%d,%s,%d\n".formatted(order, person.getName(), order % 4 + 1));
    }
    return csv.toString().getBytes(StandardCharsets.UTF_8);
  }

  private static byte[] photo() {
    try (InputStream inputStream = new ClassPathResource("person-files/team-offsite.jpg").getInputStream()) {
      return inputStream.readAllBytes();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
