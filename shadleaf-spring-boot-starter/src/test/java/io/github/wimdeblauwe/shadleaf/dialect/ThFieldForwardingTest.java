package io.github.wimdeblauwe.shadleaf.dialect;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.wimdeblauwe.shadleaf.test.ComponentRenderTester;
import io.github.wimdeblauwe.shadleaf.test.Rendered;
import java.util.Map;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.Test;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;

/**
 * {@code th:field} is a reserved prop name, so a component forwards it through {@code sl:attrs} and Spring's field
 * processors run on the rendered element, inside the caller's {@code th:object} form.
 */
class ThFieldForwardingTest {

  private final ComponentRenderTester renderer = ComponentRenderTester.create();

  @Test
  void inputGetsIdNameAndValueFromThField() {
    Element input = renderForm(new Signup("wim@example.com", false, "pro"), null, """
        <sl:test-input th:field="*{email}" size="sm"/>""").select("form > input").first();

    assertThat(input.attr("id")).isEqualTo("email");
    assertThat(input.attr("name")).isEqualTo("email");
    assertThat(input.attr("value")).isEqualTo("wim@example.com");
    assertThat(input.attr("class")).isEqualTo("input");
    assertThat(input.attr("data-size")).isEqualTo("sm");
    assertThat(input.hasAttr("th:field")).isFalse();
  }

  @Test
  void anExplicitIdWins() {
    Element input = renderForm(new Signup("wim@example.com", false, "pro"), null, """
        <sl:test-input id="signup-email" th:field="*{email}"/>""").select("form > input").first();

    assertThat(input.attr("id")).isEqualTo("signup-email");
    assertThat(input.attr("name")).isEqualTo("email");
  }

  @Test
  void checkboxGetsASequencedIdAndTheHiddenMarker() {
    Rendered rendered = renderForm(new Signup("", true, "pro"), null, """
        <sl:test-input type="checkbox" th:field="*{terms}"/>""");

    Element checkbox = rendered.select("input[type=checkbox]").first();
    assertThat(checkbox.attr("id")).isEqualTo("terms1");
    assertThat(checkbox.attr("name")).isEqualTo("terms");
    assertThat(checkbox.hasAttr("checked")).isTrue();
    assertThat(rendered.select("input[type=hidden][name=_terms]")).hasSize(1);
  }

  @Test
  void selectMarksTheBoundOptionInItsSlot() {
    Element select = renderForm(new Signup("", false, "pro"), null, """
        <sl:test-select th:field="*{plan}">
          <option value="free">Free</option>
          <option value="pro">Pro</option>
        </sl:test-select>""").select("select").first();

    assertThat(select.attr("name")).isEqualTo("plan");
    assertThat(select.select("option[selected]")).extracting(Element::val).containsExactly("pro");
  }

  @Test
  void errorClassIsAppliedWhenTheFieldHasAnError() {
    Signup signup = new Signup("nope", false, "pro");
    Element input = renderForm(signup, rejectEmail(signup), """
        <sl:test-input th:field="*{email}" th:errorclass="invalid"/>""").select("form > input").first();

    assertThat(input.attr("class")).isEqualTo("input invalid");
  }

  @Test
  void componentReadsTheErrorsOfItsForwardedField() {
    Signup signup = new Signup("nope", false, "pro");
    Element field = renderForm(signup, rejectEmail(signup), """
        <sl:test-text-field th:field="*{email}"/>""").select(".field").first();

    Element input = field.selectFirst("input");
    assertThat(input.attr("name")).isEqualTo("email");
    assertThat(input.attr("value")).isEqualTo("nope");
    assertThat(input.attr("aria-invalid")).isEqualTo("true");
    assertThat(field.select("p.error")).extracting(Element::text).containsExactly("must be a valid email address");
  }

  @Test
  void componentWithoutErrorsRendersNone() {
    Signup signup = new Signup("wim@example.com", false, "pro");
    Element field = renderForm(signup, new BeanPropertyBindingResult(signup, "signup"), """
        <sl:test-text-field th:field="*{email}"/>""").select(".field").first();

    assertThat(field.selectFirst("input").hasAttr("aria-invalid")).isFalse();
    assertThat(field.select("p.error")).isEmpty();
  }

  private static BindingResult rejectEmail(Signup signup) {
    BindingResult bindingResult = new BeanPropertyBindingResult(signup, "signup");
    bindingResult.rejectValue("email", "Email", "must be a valid email address");
    return bindingResult;
  }

  private Rendered renderForm(Signup signup, BindingResult bindingResult, String fields) {
    Map<String, Object> variables = bindingResult == null
        ? Map.of("signup", signup)
        : Map.of("signup", signup, BindingResult.MODEL_KEY_PREFIX + "signup", bindingResult);
    return renderer.render("<form th:object=\"${signup}\">" + fields + "</form>", variables);
  }

  /** A form backing object with JavaBeans accessors, as Spring's data binding expects. */
  public static class Signup {

    private String email;
    private boolean terms;
    private String plan;

    Signup(String email, boolean terms, String plan) {
      this.email = email;
      this.terms = terms;
      this.plan = plan;
    }

    public String getEmail() {
      return email;
    }

    public void setEmail(String email) {
      this.email = email;
    }

    public boolean isTerms() {
      return terms;
    }

    public void setTerms(boolean terms) {
      this.terms = terms;
    }

    public String getPlan() {
      return plan;
    }

    public void setPlan(String plan) {
      this.plan = plan;
    }
  }
}
