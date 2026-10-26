package io.github.wimdeblauwe.shadleaf.i18n;

import static io.github.wimdeblauwe.shadleaf.test.ShadleafAssertions.assertThat;

import io.github.wimdeblauwe.shadleaf.test.ComponentRenderTester;
import io.github.wimdeblauwe.shadleaf.test.Rendered;
import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;

class ShadleafMessageBundlesTest {

  private static final Pattern ARGUMENT = Pattern.compile("\\{(\\d+)}");
  // An apostrophe not doubled: MessageFormat would take it as the start of quoted text.
  private static final Pattern SINGLE_APOSTROPHE = Pattern.compile("(?<!')'(?!')");

  @Test
  void shipsEnglishDutchFrenchAndGerman() throws IOException {
    assertThat(MessageBundles.all()).containsOnlyKeys(MessageBundles.ENGLISH, "nl", "fr", "de");
  }

  @Test
  void everyTranslationHasExactlyTheEnglishKeys() throws IOException {
    Map<String, Properties> bundles = MessageBundles.all();
    Set<String> english = bundles.get(MessageBundles.ENGLISH).stringPropertyNames();
    SoftAssertions softly = new SoftAssertions();
    bundles.forEach((language, messages) -> softly.assertThat(messages.stringPropertyNames())
        .as("the keys of messages_%s.properties", language)
        .containsExactlyInAnyOrderElementsOf(english));
    softly.assertAll();
  }

  @Test
  void everyTranslationHasTheArgumentsOfTheEnglishText() throws IOException {
    Map<String, Properties> bundles = MessageBundles.all();
    Properties english = bundles.get(MessageBundles.ENGLISH);
    SoftAssertions softly = new SoftAssertions();
    bundles.forEach((language, messages) -> messages.stringPropertyNames().forEach(key -> softly
        .assertThat(arguments(messages.getProperty(key)))
        .as("the arguments of %s in messages_%s.properties", key, language)
        .isEqualTo(arguments(english.getProperty(key, "")))));
    softly.assertAll();
  }

  @Test
  void apostrophesAreDoubledInMessagesWithArgumentsOnly() throws IOException {
    SoftAssertions softly = new SoftAssertions();
    MessageBundles.all().forEach((language, messages) -> messages.stringPropertyNames().forEach(key -> {
      String text = messages.getProperty(key);
      if (ARGUMENT.matcher(text).find()) {
        softly.assertThat(SINGLE_APOSTROPHE.matcher(text).find())
            .as("%s in messages_%s.properties has arguments, so it is a MessageFormat pattern: write '' for an "
                + "apostrophe", key, language)
            .isFalse();
      } else {
        softly.assertThat(text)
            .as("%s in messages_%s.properties has no arguments, so it is used as written: write ' for an apostrophe",
                key, language)
            .doesNotContain("''");
      }
    }));
    softly.assertAll();
  }

  @Test
  void theRequestsLanguagePicksTheTexts() {
    String snippet = "<sl:alert-dialog-cancel/>";

    assertThat(render(snippet, Locale.forLanguageTag("nl"))).root().hasText("Annuleren");
    assertThat(render(snippet, Locale.forLanguageTag("nl-BE"))).root().hasText("Annuleren");
    assertThat(render(snippet, Locale.forLanguageTag("fr-BE"))).root().hasText("Annuler");
    assertThat(render(snippet, Locale.forLanguageTag("de-AT"))).root().hasText("Abbrechen");
    assertThat(render(snippet, Locale.forLanguageTag("it"))).root().hasText("Cancel");
  }

  @Test
  void anApostropheShowsOnceWithAndWithoutArguments() {
    assertThat(render("<sl:pagination-ellipsis/>", Locale.forLanguageTag("nl")).root().text())
        .contains("Meer pagina's");
    assertThat(new ShadleafMessageSource()
        .getMessage("sl.file-upload.announce.rejected", new Object[] {"a.pdf", "x"}, Locale.FRENCH))
        .startsWith("Impossible d'ajouter a.pdf");
  }

  private static Rendered render(String snippet, Locale locale) {
    return ComponentRenderTester.builder().locale(locale).build().render(snippet);
  }

  private static Set<String> arguments(String text) {
    Set<String> arguments = new TreeSet<>();
    Matcher matcher = ARGUMENT.matcher(text);
    while (matcher.find()) {
      arguments.add(matcher.group());
    }
    return arguments;
  }
}
