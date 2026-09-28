package io.github.wimdeblauwe.shadleaf.metadata;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

final class MetadataFiles {

  private MetadataFiles() {
  }

  /**
   * Writes {@code content} to {@code file}, creating its directory. An unchanged file is not touched, so a watcher on
   * it (the IDE, a docs dev server) does not fire for nothing.
   *
   * @return whether the file was written
   */
  static boolean write(Path file, String content) {
    try {
      if (Files.exists(file) && Files.readString(file, StandardCharsets.UTF_8).equals(content)) {
        return false;
      }
      Path parent = file.toAbsolutePath().getParent();
      if (parent != null) {
        Files.createDirectories(parent);
      }
      Files.writeString(file, content, StandardCharsets.UTF_8);
      return true;
    } catch (IOException e) {
      throw new UncheckedIOException("Could not write " + file, e);
    }
  }
}
