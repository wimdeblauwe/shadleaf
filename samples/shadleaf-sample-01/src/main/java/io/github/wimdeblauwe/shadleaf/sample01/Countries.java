package io.github.wimdeblauwe.shadleaf.sample01;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;

/**
 * The countries of the trip page's destination: every ISO 3166 country, by its English name. Too many to put in the
 * page, so the combobox asks {@link TripController} for them as the user types.
 */
@Component("countries")
public class Countries {

  private static final int MAX_RESULTS = 20;

  private final List<Country> all = Arrays.stream(Locale.getISOCountries())
      .map(code -> new Country(code, new Locale.Builder().setRegion(code).build().getDisplayCountry(Locale.ENGLISH)))
      .sorted(Comparator.comparing(Country::name))
      .toList();

  /** The countries whose name contains the query, ignoring case and accents: at most twenty. */
  public List<Country> search(String query) {
    String search = normalize(query);
    return all.stream().filter(country -> normalize(country.name()).contains(search)).limit(MAX_RESULTS).toList();
  }

  /** The country with this code, or {@code null} for none or an unknown one. */
  public Country byCode(String code) {
    return all.stream().filter(country -> country.code().equals(code)).findFirst().orElse(null);
  }

  private static String normalize(String text) {
    return Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT).strip();
  }

  public record Country(String code, String name) {
  }
}
