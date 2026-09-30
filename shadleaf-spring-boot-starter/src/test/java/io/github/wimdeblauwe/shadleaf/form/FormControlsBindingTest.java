package io.github.wimdeblauwe.shadleaf.form;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.wimdeblauwe.shadleaf.test.ComponentRenderTester;
import io.github.wimdeblauwe.shadleaf.test.Rendered;
import java.util.HashMap;
import java.util.Map;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.Test;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;

/**
 * The form controls inside a {@code th:object} form: {@code th:field} sets the control's {@code id}, {@code name} and
 * value, and a field with errors marks the control {@code aria-invalid}.
 */
class FormControlsBindingTest {

  private final ComponentRenderTester renderer = ComponentRenderTester.create();

  @Test
  void inputTakesItsIdNameAndValueFromTheField() {
    Element input = render(new Profile("wim@example.com", "Hi", "pro"), false, """
        <sl:input type="email" th:field="*{email}"/>""").select("input").first();

    assertThat(input.attr("type")).isEqualTo("email");
    assertThat(input.attr("id")).isEqualTo("email");
    assertThat(input.attr("name")).isEqualTo("email");
    assertThat(input.attr("value")).isEqualTo("wim@example.com");
    assertThat(input.hasAttr("aria-invalid")).isFalse();
  }

  @Test
  void inputWithAFieldErrorIsInvalid() {
    Element input = render(new Profile("nope", "", "pro"), true, """
        <sl:input th:field="*{email}"/>""").select("input").first();

    assertThat(input.attr("aria-invalid")).isEqualTo("true");
    assertThat(input.attr("value")).isEqualTo("nope");
  }

  @Test
  void textareaTakesItsTextFromTheField() {
    Element textarea = render(new Profile("", "Hello there", "pro"), false, """
        <sl:textarea th:field="*{bio}">ignored</sl:textarea>""").select("textarea").first();

    assertThat(textarea.attr("name")).isEqualTo("bio");
    assertThat(textarea.text()).isEqualTo("Hello there");
    assertThat(textarea.hasAttr("aria-invalid")).isFalse();
  }

  @Test
  void textareaWithAFieldErrorIsInvalid() {
    Element textarea = render(new Profile("", "", "pro"), true, """
        <sl:textarea th:field="*{bio}"/>""").select("textarea").first();

    assertThat(textarea.attr("aria-invalid")).isEqualTo("true");
  }

  @Test
  void nativeSelectMarksTheBoundOptionAndKeepsClassOnTheWrapper() {
    Rendered rendered = render(new Profile("", "", "pro"), false, """
        <sl:native-select th:field="*{plan}" class="w-full">
          <option value="free">Free</option>
          <option value="pro">Pro</option>
        </sl:native-select>""");

    Element wrapper = rendered.select(".native-select-wrapper").first();
    Element select = wrapper.selectFirst("select");
    assertThat(wrapper.classNames()).containsExactly("native-select-wrapper", "w-full");
    assertThat(select.classNames()).containsExactly("native-select");
    assertThat(select.attr("id")).isEqualTo("plan");
    assertThat(select.attr("name")).isEqualTo("plan");
    assertThat(select.select("option[selected]")).extracting(Element::val).containsExactly("pro");
    assertThat(select.hasAttr("aria-invalid")).isFalse();
  }

  @Test
  void nativeSelectWithAFieldErrorIsInvalid() {
    Element select = render(new Profile("", "", "gold"), true, """
        <sl:native-select th:field="*{plan}"><option value="free">Free</option></sl:native-select>""")
        .select("select").first();

    assertThat(select.attr("aria-invalid")).isEqualTo("true");
  }

  @Test
  void labelPointsAtTheIdThFieldGives() {
    Rendered rendered = render(new Profile("", "", "pro"), false, """
        <sl:label for="email">Email</sl:label>
        <sl:input th:field="*{email}"/>""");

    assertThat(rendered.select("label").first().attr("for")).isEqualTo(rendered.select("input").first().id());
  }

  /** With {@code invalid}, every field of the profile has an error. */
  private Rendered render(Profile profile, boolean invalid, String fields) {
    BindingResult bindingResult = new BeanPropertyBindingResult(profile, "profile");
    if (invalid) {
      bindingResult.rejectValue("email", "Email", "must be a valid email address");
      bindingResult.rejectValue("bio", "NotBlank", "must not be blank");
      bindingResult.rejectValue("plan", "Plan", "is not a plan");
    }
    Map<String, Object> variables = new HashMap<>();
    variables.put("profile", profile);
    variables.put(BindingResult.MODEL_KEY_PREFIX + "profile", bindingResult);
    return renderer.render("<form th:object=\"${profile}\">" + fields + "</form>", variables);
  }

  /** A form backing object with JavaBeans accessors, as Spring's data binding expects. */
  public static class Profile {

    private String email;
    private String bio;
    private String plan;

    Profile(String email, String bio, String plan) {
      this.email = email;
      this.bio = bio;
      this.plan = plan;
    }

    public String getEmail() {
      return email;
    }

    public void setEmail(String email) {
      this.email = email;
    }

    public String getBio() {
      return bio;
    }

    public void setBio(String bio) {
      this.bio = bio;
    }

    public String getPlan() {
      return plan;
    }

    public void setPlan(String plan) {
      this.plan = plan;
    }
  }
}
