package io.github.wimdeblauwe.shadleaf.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class InitialsTest {

  @ParameterizedTest(name = "{0} / {1} -> {2}")
  @CsvSource(nullValues = "-", value = {
      "Ada Lovelace, -, AL",
      "ada lovelace, -, AL",
      "Ludwig van Beethoven, -, LB",
      "Ada, -, A",
      "octocat, -, O",
      "Jean-Pierre Dupont, -, JD",
      "'  Ada   Lovelace  ', -, AL",
      "(Ada) Lovelace, -, AL",
      "🦊 Fox, -, F",
      "王小明, -, 王",
      "Émile Zola, -, ÉZ",
      "Иван Петров, -, ИП",
      "R2 D2, -, RD",
      "-, ada.lovelace@example.com, AL",
      "-, ada@example.com, A",
      "-, ada_king-lovelace+news@example.com, AL",
      "-, ada+news@example.com, A",
      "ada.lovelace@example.com, ada.lovelace@example.com, AL",
      "ada@example.com, -, A",
      "Ada Lovelace, someone.else@example.com, AL",
      "'', -, -",
      "-, -, -",
      "!!!, -, -",
  })
  void initials(String name, String email, String expected) {
    assertThat(Initials.of(name, email)).isEqualTo(expected);
  }

  @ParameterizedTest
  @CsvSource({
      // e + combining acute accent stays one initial
      "'Amélie', A",
      "'émile zola', 'ÉZ'",
  })
  void keepsAGraphemeTogether(String name, String expected) {
    assertThat(Initials.of(name, null)).isEqualTo(expected);
  }
}
