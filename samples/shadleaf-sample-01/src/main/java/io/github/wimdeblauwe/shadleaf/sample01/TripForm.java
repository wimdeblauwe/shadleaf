package io.github.wimdeblauwe.shadleaf.sample01;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;

/** The form backing object of the trip page. JavaBeans accessors, as Spring's data binding and th:field expect. */
public class TripForm {

  /** A country code, from the destination combobox. */
  @NotBlank(message = "Choose where you are going.")
  private String destination;

  /** Language codes, from the multiple combobox: a collection, which th:field binds to a select multiple. */
  @Size(min = 1, message = "Choose at least one language.")
  private List<String> languages = new ArrayList<>();

  public String getDestination() {
    return destination;
  }

  public void setDestination(String destination) {
    this.destination = destination;
  }

  public List<String> getLanguages() {
    return languages;
  }

  public void setLanguages(List<String> languages) {
    this.languages = languages;
  }
}
