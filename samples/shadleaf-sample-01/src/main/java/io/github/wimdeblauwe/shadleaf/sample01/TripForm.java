package io.github.wimdeblauwe.shadleaf.sample01;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
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

  /** In euros, optional: an sl:input-group with the currency inside the input. */
  @Positive(message = "Enter an amount above zero, or leave it empty.")
  private BigDecimal dailyBudget;

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

  public BigDecimal getDailyBudget() {
    return dailyBudget;
  }

  public void setDailyBudget(BigDecimal dailyBudget) {
    this.dailyBudget = dailyBudget;
  }
}
