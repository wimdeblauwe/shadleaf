package io.github.wimdeblauwe.shadleaf.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Compares rendered HTML with an approved copy kept in version control, so a change in the markup a component emits
 * shows up as a change to a reviewable file.
 * <p>
 * The approved file is {@code src/test/resources/approved/<name>.approved.html}. When the received HTML differs, it
 * is written to {@code target/approvals/<name>.received.html} and the test fails with a line diff. To accept a
 * deliberate change, run the test with {@value #APPROVE_PROPERTY} set:
 * <pre>mvn test -Dtest=ButtonApprovalTest -Dshadleaf.approve</pre>
 * which overwrites the approved file; review its diff and commit it. A missing approved file is written and the test
 * fails, so a new approval is never silently accepted.
 * <p>
 * Other generated text, such as the web-types JSON, is approved the same way with {@link #verify(String, String,
 * String)}.
 */
public final class HtmlApproval {

  public static final String APPROVE_PROPERTY = "shadleaf.approve";

  private static final Path APPROVED_DIRECTORY = Path.of("src", "test", "resources", "approved");
  private static final Path RECEIVED_DIRECTORY = Path.of("target", "approvals");

  private HtmlApproval() {
  }

  public static void verify(String name, String received) {
    verify(name, "html", received);
  }

  /**
   * @param extension the approved file's extension, e.g. {@code json} for {@code <name>.approved.json}
   */
  public static void verify(String name, String extension, String received) {
    String content = received.replace("\r\n", "\n").stripTrailing() + "\n";
    Path approved = APPROVED_DIRECTORY.resolve(name + ".approved." + extension);
    Path receivedFile = RECEIVED_DIRECTORY.resolve(name + ".received." + extension);
    try {
      Files.deleteIfExists(receivedFile);
      if (approving()) {
        write(approved, content);
        return;
      }
      if (!Files.exists(approved)) {
        write(approved, content);
        throw new AssertionError(("No approved file for '%s'. Wrote %s: review it, commit it and run the test "
            + "again.").formatted(name, approved.toAbsolutePath()));
      }
      if (!Files.readString(approved, StandardCharsets.UTF_8).equals(content)) {
        write(receivedFile, content);
        assertThat(receivedFile)
            .as("Received text differs from %s (received: %s). If the change is deliberate, run the test with "
                + "-D%s to accept it", approved, receivedFile, APPROVE_PROPERTY)
            .hasSameTextualContentAs(approved, StandardCharsets.UTF_8);
      }
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private static boolean approving() {
    String value = System.getProperty(APPROVE_PROPERTY);
    return value != null && !value.equalsIgnoreCase("false");
  }

  private static void write(Path file, String content) throws IOException {
    Files.createDirectories(file.getParent());
    Files.writeString(file, content, StandardCharsets.UTF_8);
  }
}
