package io.github.wimdeblauwe.shadleaf.sample01;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.wimdeblauwe.shadleaf.toast.Toast;
import java.util.List;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

/**
 * The trip page: a combobox whose options come from the server as the user types (search-url), a multiple combobox
 * bound to a list, an input group in a field, and a toggle group and a toggle.
 */
@SpringBootTest
@AutoConfigureMockMvc
class TripPageTest {

  @Autowired
  private MockMvc mockMvc;

  @Test
  void theDestinationSearchesTheServerAndTheLanguagesAreAMultipleCombobox() throws Exception {
    Document page = Jsoup.parse(mockMvc.perform(get("/trip")).andExpect(status().isOk()).andReturn().getResponse()
        .getContentAsString());

    Element destination = page.getElementById("destination");
    assertThat(destination.tagName()).isEqualTo("select");
    assertThat(destination.parent().attr("x-data")).isEqualTo("slCombobox");
    assertThat(destination.parent().attr("data-search-url")).isEqualTo("/trip/countries");
    assertThat(destination.select("option")).as("only the empty first option: the server sends the countries")
        .extracting(Element::val).containsExactly("");
    assertThat(page.select("label[for=destination]")).hasSize(1);

    Element languages = page.getElementById("languages");
    assertThat(languages.hasAttr("multiple")).isTrue();
    assertThat(languages.parent().attr("data-multiple")).isEqualTo("true");
    assertThat(languages.select("option")).extracting(Element::val).hasSize(TripController.LANGUAGES.size())
        .contains("nl", "fr", "en");
    assertThat(page.select("input[type=hidden][name=_languages]")).hasSize(1);
    assertThat(page.select(".form-errors, [aria-invalid]")).isEmpty();
  }

  @Test
  void theDailyBudgetIsAnInputGroupWiredToItsField() throws Exception {
    Document page = Jsoup.parse(mockMvc.perform(get("/trip")).andExpect(status().isOk()).andReturn().getResponse()
        .getContentAsString());

    Element budget = page.getElementById("dailyBudget");
    assertThat(budget.classNames()).containsExactly("input-group-input");
    assertThat(budget.parent().hasClass("input-group")).isTrue();
    assertThat(budget.attr("name")).isEqualTo("dailyBudget");
    assertThat(budget.attr("aria-describedby")).isEqualTo("dailyBudget-description");
    assertThat(page.select("label[for=dailyBudget]")).hasSize(1);
    assertThat(budget.parent().select(".input-group-text")).extracting(Element::text).containsExactly("€", "EUR");
  }

  @Test
  void travelByIsAToggleGroupOfRadioButtonsAndFlexibleDatesAToggle() throws Exception {
    Document page = Jsoup.parse(mockMvc.perform(get("/trip")).andExpect(status().isOk()).andReturn().getResponse()
        .getContentAsString());

    assertThat(page.select("fieldset .toggle-group[role=radiogroup] input.toggle-input"))
        .extracting(input -> input.attr("type"), input -> input.attr("name"), Element::val,
            input -> input.hasAttr("checked"))
        .containsExactly(tuple("radio", "travelBy", "train", true), tuple("radio", "travelBy", "plane", false),
            tuple("radio", "travelBy", "car", false));

    Element flexible = page.getElementById("flexibleDates");
    assertThat(flexible.attr("type")).isEqualTo("checkbox");
    assertThat(flexible.parent().hasClass("toggle")).isTrue();
    assertThat(flexible.attr("aria-describedby")).isEqualTo("flexibleDates-description");
    assertThat(flexible.hasAttr("checked")).isFalse();
  }

  @Test
  void anInvalidSubmitKeepsTheChosenTravelAndFlexibleDates() throws Exception {
    Document page = Jsoup.parse(mockMvc.perform(post("/trip")
            .param("travelBy", "car")
            .param("flexibleDates", "true")
            .param("_flexibleDates", "on"))
        .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());

    assertThat(page.select(".toggle-group input[checked]")).extracting(Element::val).containsExactly("car");
    assertThat(page.getElementById("flexibleDates").hasAttr("checked")).isTrue();
  }

  @Test
  void aBudgetOfZeroIsRejectedAndKept() throws Exception {
    Document page = Jsoup.parse(mockMvc.perform(post("/trip")
            .param("destination", "BE")
            .param("languages", "nl")
            .param("dailyBudget", "0"))
        .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());

    Element budget = page.getElementById("dailyBudget");
    assertThat(budget.val()).isEqualTo("0");
    assertThat(budget.attr("aria-invalid")).isEqualTo("true");
    assertThat(budget.attr("aria-describedby")).isEqualTo("dailyBudget-description dailyBudget-error");
    assertThat(page.getElementById("dailyBudget-error").text()).contains("Enter an amount above zero");
  }

  @Test
  void theSearchAnswersWithTheOptionsThatMatch() throws Exception {
    Document options = Jsoup.parseBodyFragment(mockMvc.perform(get("/trip/countries").param("q", "bel"))
        .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());

    assertThat(options.select("option.combobox-item")).extracting(Element::val, Element::text)
        .contains(tuple("BE", "Belgium"), tuple("BY", "Belarus"));
    assertThat(options.select("html > body > *")).allMatch(element -> element.tagName().equals("option"));
    assertThat(options.select("main, nav, form")).as("the options alone, no page").isEmpty();
  }

  @Test
  void theSearchIgnoresAccentsAndAnswersAtMostTwentyOptions() throws Exception {
    Document accented = Jsoup.parseBodyFragment(mockMvc.perform(get("/trip/countries").param("q", "reunion"))
        .andReturn().getResponse().getContentAsString());
    Document many = Jsoup.parseBodyFragment(mockMvc.perform(get("/trip/countries").param("q", "a"))
        .andReturn().getResponse().getContentAsString());

    assertThat(accented.select("option")).extracting(Element::val).containsExactly("RE");
    assertThat(many.select("option")).hasSize(20);
  }

  @Test
  void anEmptyTripShowsBothErrors() throws Exception {
    Document page = Jsoup.parse(mockMvc.perform(post("/trip").param("_languages", "1"))
        .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());

    assertThat(page.getElementById("destination").attr("aria-invalid")).isEqualTo("true");
    assertThat(page.getElementById("languages").attr("aria-invalid")).isEqualTo("true");
    assertThat(page.getElementById("destination-error").text()).contains("Choose where you are going.");
    assertThat(page.getElementById("languages-error").text()).contains("Choose at least one language.");
  }

  @Test
  void anInvalidSubmitKeepsTheChosenCountryAndLanguages() throws Exception {
    Document page = Jsoup.parse(mockMvc.perform(post("/trip")
            .param("destination", "BE")
            .param("languages", "nl", "xx")
            .param("_languages", "1"))
        .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());

    assertThat(page.getElementById("destination").select("option[selected]")).extracting(Element::val, Element::text)
        .containsExactly(tuple("BE", "Belgium"));
    assertThat(page.getElementById("languages").select("option[selected]")).extracting(Element::val)
        .containsExactly("nl");
    assertThat(page.getElementById("languages-error").text()).contains("holds a language we do not know.");
  }

  @Test
  void anUnknownCountryIsRejected() throws Exception {
    Document page = Jsoup.parse(mockMvc.perform(post("/trip")
            .param("destination", "XX")
            .param("languages", "en"))
        .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());

    assertThat(page.getElementById("destination-error").text()).contains("is not a country we know.");
  }

  @Test
  void aValidTripRedirectsWithAToast() throws Exception {
    mockMvc.perform(post("/trip")
            .param("destination", "BE")
            .param("languages", "nl", "fr")
            .param("_languages", "1"))
        .andExpect(redirectedUrl("/trip"))
        .andExpect(flash().attribute("toasts", List.of(Toast.success("Trip planned")
            .withDescription("To Belgium, speaking Dutch, French."))));
  }
}
