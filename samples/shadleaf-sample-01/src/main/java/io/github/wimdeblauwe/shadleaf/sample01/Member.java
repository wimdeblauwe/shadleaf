package io.github.wimdeblauwe.shadleaf.sample01;

import java.util.Arrays;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;

/** A team member on the dialog page, with the address of their photo if they have one. */
public record Member(long id, String name, String email, @Nullable String photoUrl) {

  public Member(long id, String name, String email) {
    this(id, name, email, null);
  }

  public Member withNameAndEmail(String name, String email) {
    return new Member(id, name, email, photoUrl);
  }

  /** What the avatar shows without a photo: the first letters of the first and the last name. */
  public String initials() {
    String[] words = name.strip().split("\\s+");
    return Arrays.stream(words.length > 1 ? new String[]{words[0], words[words.length - 1]} : words)
        .filter(word -> !word.isEmpty())
        .map(word -> word.substring(0, 1).toUpperCase())
        .collect(Collectors.joining());
  }
}
