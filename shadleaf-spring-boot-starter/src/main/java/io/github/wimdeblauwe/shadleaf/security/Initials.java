package io.github.wimdeblauwe.shadleaf.security;

import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;

/**
 * One or two letters for an avatar: the first letter of the first and the last word of the name ("Ada Lovelace" and
 * "Ludwig van Beethoven" give AL and LB), or of its only word ("Ada" gives A, "王小明" gives 王). A hyphen does not
 * split a word. When the name is an email address, or missing, the email's local part is split at {@code .},
 * {@code _} and {@code -} instead ({@code ada.lovelace@...} gives AL), without a {@code +tag} ({@code ada+news@...}
 * gives A).
 * <p>
 * A word's initial is its first letter or digit, so "(Ada)" gives A and an emoji before a name is skipped; it is a
 * whole grapheme, so a letter with a combining accent stays together.
 */
final class Initials {

  private static final Pattern NAME_SEPARATORS = Pattern.compile("[\\s\\p{Z}]+");
  private static final Pattern EMAIL_SEPARATORS = Pattern.compile("[._\\-]+");

  private Initials() {
  }

  static @Nullable String of(@Nullable String name, @Nullable String email) {
    if (hasText(name) && !isEmail(name)) {
      return initials(NAME_SEPARATORS.split(name.strip()));
    }
    String address = hasText(email) ? email : name;
    if (!hasText(address)) {
      return null;
    }
    int at = address.indexOf('@');
    String localPart = at < 0 ? address : address.substring(0, at);
    int tag = localPart.indexOf('+');
    return initials(EMAIL_SEPARATORS.split(tag < 0 ? localPart : localPart.substring(0, tag)));
  }

  private static @Nullable String initials(String[] words) {
    List<String> letters = new ArrayList<>();
    for (String word : words) {
      String initial = initial(word);
      if (initial != null) {
        letters.add(initial);
      }
    }
    if (letters.isEmpty()) {
      return null;
    }
    String initials = letters.size() == 1 ? letters.get(0) : letters.get(0) + letters.get(letters.size() - 1);
    return initials.toUpperCase(Locale.ROOT);
  }

  private static @Nullable String initial(String word) {
    int start = 0;
    while (start < word.length()) {
      int codePoint = word.codePointAt(start);
      if (Character.isLetterOrDigit(codePoint)) {
        BreakIterator graphemes = BreakIterator.getCharacterInstance(Locale.ROOT);
        graphemes.setText(word);
        int end = graphemes.following(start);
        return word.substring(start, end == BreakIterator.DONE ? word.length() : end);
      }
      start += Character.charCount(codePoint);
    }
    return null;
  }

  private static boolean isEmail(String text) {
    return text.indexOf('@') > 0 && !NAME_SEPARATORS.matcher(text.strip()).find();
  }

  private static boolean hasText(@Nullable String text) {
    return text != null && !text.isBlank();
  }
}
