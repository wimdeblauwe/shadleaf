package io.github.wimdeblauwe.shadleaf.sample01;

import io.github.wimdeblauwe.shadleaf.toast.Toast;
import jakarta.validation.Valid;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * The trip page: two comboboxes. The destination's options come from the server as the user types (search-url): the
 * combobox sends a GET to /trip/countries with the text as q, and this answers with the HTML of the matching options,
 * the trip page's countries fragment. The languages are a multiple combobox with its options in the page. The
 * controller checks that the destination is a known country and the languages known languages, as a request can hold
 * any value. A valid trip redirects back to an empty form with a toast (post/redirect/get).
 */
@Controller
public class TripController {

  static final Map<String, String> LANGUAGES = languages("ar", "Arabic", "zh", "Chinese", "nl", "Dutch", "en",
      "English", "fr", "French", "de", "German", "hi", "Hindi", "it", "Italian", "ja", "Japanese", "pt", "Portuguese",
      "ru", "Russian", "es", "Spanish", "sw", "Swahili", "tr", "Turkish");

  private final Countries countries;

  public TripController(Countries countries) {
    this.countries = countries;
  }

  @ModelAttribute("languages")
  Map<String, String> languages() {
    return LANGUAGES;
  }

  @GetMapping("/trip")
  public String form(@ModelAttribute("tripForm") TripForm tripForm) {
    return "trip";
  }

  /** The options for the destination combobox: the trip page's countries fragment, with the countries that match. */
  @GetMapping("/trip/countries")
  public String searchCountries(@RequestParam("q") String query, Model model) {
    model.addAttribute("countries", countries.search(query));
    return "trip :: countries";
  }

  @PostMapping("/trip")
  public String submit(@Valid @ModelAttribute("tripForm") TripForm tripForm, BindingResult bindingResult,
      RedirectAttributes redirectAttributes) {
    if (tripForm.getDestination() != null && !tripForm.getDestination().isBlank()
        && countries.byCode(tripForm.getDestination()) == null) {
      bindingResult.rejectValue("destination", "tripForm.destination", "is not a country we know.");
    }
    if (!LANGUAGES.keySet().containsAll(tripForm.getLanguages())) {
      bindingResult.rejectValue("languages", "tripForm.languages", "holds a language we do not know.");
    }
    if (bindingResult.hasErrors()) {
      return "trip";
    }
    String spoken = tripForm.getLanguages().stream().map(LANGUAGES::get).collect(Collectors.joining(", "));
    redirectAttributes.addFlashAttribute("toasts", List.of(Toast.success("Trip planned")
        .withDescription("To %s, speaking %s.".formatted(countries.byCode(tripForm.getDestination()).name(), spoken))));
    return "redirect:/trip";
  }

  /** Codes and names, in pairs, in the order given. */
  private static Map<String, String> languages(String... codesAndNames) {
    Map<String, String> languages = new LinkedHashMap<>();
    for (int i = 0; i < codesAndNames.length; i += 2) {
      languages.put(codesAndNames[i], codesAndNames[i + 1]);
    }
    return languages;
  }
}
