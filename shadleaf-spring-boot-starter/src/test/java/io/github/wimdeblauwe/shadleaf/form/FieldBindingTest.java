package io.github.wimdeblauwe.shadleaf.form;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import io.github.wimdeblauwe.shadleaf.test.ComponentRenderTester;
import io.github.wimdeblauwe.shadleaf.test.Rendered;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.Test;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;

/**
 * {@code <sl:field>} and {@code <sl:field-set>} with a {@code th:field} inside a {@code th:object} form: the control
 * takes the field, the label points at the control, the control is described by the description and the errors, and
 * the errors come from the binding.
 */
class FieldBindingTest {

  private final ComponentRenderTester renderer = ComponentRenderTester.create();

  @Test
  void fieldWiresTheLabelTheControlAndTheDescription() {
    Rendered rendered = render(new Signup(), Map.of(), """
        <sl:field th:field="*{email}">
          <sl:field-label>Email</sl:field-label>
          <sl:input type="email"/>
          <sl:field-description>We never share it.</sl:field-description>
          <sl:field-error/>
        </sl:field>""");

    Element field = rendered.select(".field").first();
    Element input = field.selectFirst("input.input");
    assertThat(field.hasAttr("th:field")).isFalse();
    assertThat(field.hasAttr("data-invalid")).isFalse();
    assertThat(input.attr("id")).isEqualTo("email");
    assertThat(input.attr("name")).isEqualTo("email");
    assertThat(input.attr("value")).isEqualTo("wim@example.com");
    assertThat(input.hasAttr("aria-invalid")).isFalse();
    assertThat(input.attr("aria-describedby")).isEqualTo("email-description");
    assertThat(field.selectFirst("label.label.field-label").attr("for")).isEqualTo("email");
    assertThat(field.selectFirst(".field-description").id()).isEqualTo("email-description");
    assertThat(field.select(".field-error")).isEmpty();
  }

  @Test
  void fieldWithAnErrorShowsItAndDescribesTheControlWithIt() {
    Rendered rendered = render(new Signup(), Map.of("email", List.of("must be a valid email address")), """
        <sl:field th:field="*{email}">
          <sl:field-label>Email</sl:field-label>
          <sl:input type="email"/>
          <sl:field-description>We never share it.</sl:field-description>
          <sl:field-error/>
        </sl:field>""");

    Element field = rendered.select(".field").first();
    Element input = field.selectFirst("input");
    assertThat(field.attr("data-invalid")).isEqualTo("true");
    assertThat(input.attr("aria-invalid")).isEqualTo("true");
    assertThat(input.attr("aria-describedby")).isEqualTo("email-description email-error");
    Element error = field.selectFirst(".field-error");
    assertThat(error.id()).isEqualTo("email-error");
    assertThat(error.text()).isEqualTo("must be a valid email address");
    assertThat(error.hasAttr("role")).isFalse();
    assertThat(error.select("ul")).isEmpty();
  }

  @Test
  void severalErrorsBecomeAListWithEachMessageOnce() {
    Rendered rendered = render(new Signup(), Map.of("email", List.of("must not be blank", "is too short",
        "must not be blank")), """
        <sl:field th:field="*{email}">
          <sl:input/>
          <sl:field-error/>
        </sl:field>""");

    assertThat(rendered.select(".field-error > ul > li")).extracting(Element::text)
        .containsExactly("must not be blank", "is too short");
    assertThat(rendered.select("input").first().attr("aria-describedby")).isEqualTo("email-error");
  }

  @Test
  void errorContentOfItsOwnReplacesTheMessages() {
    Rendered rendered = render(new Signup(), Map.of("email", List.of("must be a valid email address")), """
        <sl:field th:field="*{email}">
          <sl:input/>
          <sl:field-error>Enter an address like <code>you@example.com</code>.</sl:field-error>
        </sl:field>""");

    assertThat(rendered.select(".field-error").first().html())
        .isEqualTo("Enter an address like <code>you@example.com</code>.");
  }

  @Test
  void theControlKeepsAnAriaDescribedbyOfItsOwnFirst() {
    Rendered rendered = render(new Signup(), Map.of(), """
        <p id="email-hint">Hint</p>
        <sl:field th:field="*{email}">
          <sl:input aria-describedby="email-hint"/>
          <sl:field-description>We never share it.</sl:field-description>
        </sl:field>""");

    assertThat(rendered.select("input").first().attr("aria-describedby")).isEqualTo("email-hint email-description");
  }

  @Test
  void forChoosesTheControlIdAndTheIdsDerivedFromIt() {
    Rendered rendered = render(new Signup(), Map.of("email", List.of("is taken")), """
        <sl:field th:field="*{email}" for="work-email">
          <sl:field-label>Work email</sl:field-label>
          <sl:input/>
          <sl:field-description>Your company address.</sl:field-description>
          <sl:field-error/>
        </sl:field>""");

    Element input = rendered.select("input").first();
    assertThat(input.id()).isEqualTo("work-email");
    assertThat(input.attr("name")).isEqualTo("email");
    assertThat(rendered.select("label").first().attr("for")).isEqualTo("work-email");
    assertThat(input.attr("aria-describedby")).isEqualTo("work-email-description work-email-error");
    assertThat(rendered.select(".field-error").first().id()).isEqualTo("work-email-error");
  }

  @Test
  void aThFieldOnTheControlWinsOverTheFields() {
    Rendered rendered = render(new Signup(), Map.of(), """
        <sl:field th:field="*{email}">
          <sl:textarea th:field="*{bio}"/>
        </sl:field>""");

    Element textarea = rendered.select("textarea").first();
    assertThat(textarea.attr("name")).isEqualTo("bio");
    assertThat(textarea.id()).isEqualTo("email");
  }

  @Test
  void onlyTheFirstControlTakesTheFieldsId() {
    Rendered rendered = render(new Signup(), Map.of(), """
        <sl:field th:field="*{email}">
          <sl:input/>
          <sl:input th:field="*{bio}"/>
        </sl:field>""");

    assertThat(rendered.select("input")).extracting(Element::id).containsExactly("email", "bio");
  }

  @Test
  void textareaAndNativeSelectTakeTheField() {
    Rendered rendered = render(new Signup(), Map.of("plan", List.of("is not a plan")), """
        <sl:field th:field="*{bio}">
          <sl:field-label>Bio</sl:field-label>
          <sl:textarea/>
          <sl:field-description>A few words.</sl:field-description>
        </sl:field>
        <sl:field th:field="*{plan}">
          <sl:field-label>Plan</sl:field-label>
          <sl:native-select class="w-full">
            <option value="free">Free</option>
            <option value="pro">Pro</option>
          </sl:native-select>
          <sl:field-error/>
        </sl:field>""");

    Element textarea = rendered.select("textarea").first();
    assertThat(textarea.id()).isEqualTo("bio");
    assertThat(textarea.text()).isEqualTo("Hello there");
    assertThat(textarea.attr("aria-describedby")).isEqualTo("bio-description");

    Element select = rendered.select("select").first();
    assertThat(select.id()).isEqualTo("plan");
    assertThat(select.attr("name")).isEqualTo("plan");
    assertThat(select.attr("aria-invalid")).isEqualTo("true");
    assertThat(select.attr("aria-describedby")).isEqualTo("plan-error");
    assertThat(select.select("option[selected]")).extracting(Element::val).containsExactly("pro");
    assertThat(select.parent().classNames()).containsExactly("native-select-wrapper", "w-full");
  }

  @Test
  void aCheckboxOrSwitchInAFieldGetsTheUnnumberedIdTheLabelPointsAt() {
    Rendered rendered = render(new Signup(), Map.of("terms", List.of("must be accepted")), """
        <sl:field th:field="*{terms}" orientation="horizontal">
          <sl:checkbox/>
          <sl:field-content>
            <sl:field-label>I accept the terms</sl:field-label>
            <sl:field-error/>
          </sl:field-content>
        </sl:field>
        <sl:field th:field="*{newsletter}" orientation="horizontal">
          <sl:switch/>
          <sl:field-label>Newsletter</sl:field-label>
        </sl:field>""");

    Element terms = rendered.select("input.checkbox").first();
    assertThat(rendered.select(".field").first().attr("data-orientation")).isEqualTo("horizontal");
    assertThat(terms.id()).isEqualTo("terms");
    assertThat(terms.attr("name")).isEqualTo("terms");
    assertThat(terms.attr("aria-invalid")).isEqualTo("true");
    assertThat(terms.attr("aria-describedby")).isEqualTo("terms-error");
    assertThat(rendered.select("input[type=hidden][name=_terms]")).hasSize(1);
    assertThat(rendered.select(".field-content > label").first().attr("for")).isEqualTo("terms");
    assertThat(rendered.select(".field-content > .field-error").first().text()).isEqualTo("must be accepted");

    Element newsletter = rendered.select("input.switch").first();
    assertThat(newsletter.id()).isEqualTo("newsletter");
    assertThat(newsletter.hasAttr("checked")).isTrue();
    assertThat(newsletter.hasAttr("aria-describedby")).isFalse();
    assertThat(rendered.select(".field").get(1).selectFirst("label").attr("for")).isEqualTo("newsletter");
  }

  @Test
  void fieldSetSharesItsFieldWithTheRadioButtonsAndIsDescribedByTheError() {
    Rendered rendered = render(new Signup(), Map.of("plan", List.of("choose a plan")), """
        <sl:field-set th:field="*{plan}">
          <sl:field-legend variant="label">Plan</sl:field-legend>
          <sl:field-description>You can change it later.</sl:field-description>
          <sl:radio-group>
            <sl:label><sl:radio-group-item value="free"/>Free</sl:label>
            <sl:label><sl:radio-group-item value="pro"/>Pro</sl:label>
          </sl:radio-group>
          <sl:field-error/>
        </sl:field-set>""");

    Element fieldSet = rendered.select("fieldset.field-set").first();
    assertThat(fieldSet.hasAttr("th:field")).isFalse();
    assertThat(fieldSet.attr("data-invalid")).isEqualTo("true");
    assertThat(fieldSet.attr("aria-describedby")).isEqualTo("plan-description plan-error");
    assertThat(fieldSet.selectFirst("legend.field-legend").attr("data-variant")).isEqualTo("label");
    assertThat(fieldSet.selectFirst(".field-description").id()).isEqualTo("plan-description");
    assertThat(fieldSet.selectFirst(".field-error").id()).isEqualTo("plan-error");
    assertThat(fieldSet.select("input.radio-group-item"))
        .extracting(Element::id, item -> item.attr("name"), item -> item.hasAttr("checked"),
            item -> item.attr("aria-invalid"), item -> item.hasAttr("aria-describedby"))
        .containsExactly(tuple("plan1", "plan", false, "true", false), tuple("plan2", "plan", true, "true", false));
  }

  @Test
  void fieldSetWithoutErrorsIsDescribedByItsDescriptionOnly() {
    Rendered rendered = render(new Signup(), Map.of(), """
        <sl:field-set th:field="*{plan}">
          <sl:field-legend>Plan</sl:field-legend>
          <sl:field-description>You can change it later.</sl:field-description>
          <sl:radio-group>
            <sl:label><sl:radio-group-item value="free"/>Free</sl:label>
          </sl:radio-group>
          <sl:field-error/>
        </sl:field-set>""");

    Element fieldSet = rendered.select("fieldset").first();
    assertThat(fieldSet.hasAttr("data-invalid")).isFalse();
    assertThat(fieldSet.attr("aria-describedby")).isEqualTo("plan-description");
    assertThat(fieldSet.select(".field-error")).isEmpty();
    assertThat(fieldSet.selectFirst("input").hasAttr("aria-invalid")).isFalse();
  }

  @Test
  void checkboxOptionsInAFieldSetAreFieldsNumberedLikeSpringNumbersThem() {
    Rendered rendered = render(new Signup(), Map.of("toppings", List.of("choose at least one")), """
        <sl:field-set th:field="*{toppings}">
          <sl:field-legend variant="label">Toppings</sl:field-legend>
          <sl:field orientation="horizontal">
            <sl:checkbox value="cheese"/>
            <sl:field-content>
              <sl:field-label>Cheese</sl:field-label>
              <sl:field-description>Aged gouda.</sl:field-description>
            </sl:field-content>
          </sl:field>
          <sl:field orientation="horizontal">
            <sl:checkbox value="olives"/>
            <sl:field-label>Olives</sl:field-label>
          </sl:field>
          <sl:field-error/>
        </sl:field-set>""");

    Element fieldSet = rendered.select("fieldset").first();
    assertThat(fieldSet.attr("aria-describedby")).isEqualTo("toppings-error");
    assertThat(fieldSet.select("input.checkbox"))
        .extracting(Element::id, box -> box.attr("name"), Element::val, box -> box.hasAttr("checked"),
            box -> box.attr("aria-invalid"), box -> box.attr("aria-describedby"))
        .containsExactly(
            tuple("toppings1", "toppings", "cheese", true, "true", "toppings1-description"),
            tuple("toppings2", "toppings", "olives", false, "true", ""));
    assertThat(fieldSet.select("label")).extracting(label -> label.attr("for"))
        .containsExactly("toppings1", "toppings2");
    assertThat(fieldSet.select(".field")).allSatisfy(field -> assertThat(field.hasAttr("data-invalid")).isFalse());
    assertThat(fieldSet.select(".field-error")).hasSize(1);
    assertThat(fieldSet.select("input[type=hidden][name=_toppings]")).hasSize(2);
  }

  @Test
  void aFieldWithoutABindingUsesForAndShowsItsOwnError() {
    Rendered rendered = renderer.render("""
        <sl:field for="query" data-invalid="true">
          <sl:field-label>Search</sl:field-label>
          <sl:input type="search" name="q" aria-invalid="true"/>
          <sl:field-description>Name or order number.</sl:field-description>
          <sl:field-error>Type at least two characters.</sl:field-error>
        </sl:field>""");

    Element input = rendered.select("input").first();
    assertThat(input.id()).isEqualTo("query");
    assertThat(input.attr("aria-describedby")).isEqualTo("query-description query-error");
    assertThat(rendered.select("label").first().attr("for")).isEqualTo("query");
    assertThat(rendered.select(".field-error").first().id()).isEqualTo("query-error");
    assertThat(rendered.select(".field").first().attr("data-invalid")).isEqualTo("true");
  }

  @Test
  void aFieldWithoutABindingOrForLeavesTheIdsToTheAuthor() {
    Rendered rendered = renderer.render("""
        <sl:field>
          <sl:field-label for="name">Name</sl:field-label>
          <sl:input id="name" name="name"/>
          <sl:field-description>As on your passport.</sl:field-description>
          <sl:field-error/>
        </sl:field>""");

    Element input = rendered.select("input").first();
    assertThat(input.id()).isEqualTo("name");
    assertThat(input.hasAttr("aria-describedby")).isFalse();
    assertThat(rendered.select("label").first().attr("for")).isEqualTo("name");
    assertThat(rendered.select(".field-description").first().hasAttr("id")).isFalse();
    assertThat(rendered.select(".field-error")).isEmpty();
  }

  @Test
  void aNestedPathGivesTheIdThFieldWouldGive() {
    Signup signup = new Signup();
    Rendered rendered = render(signup, Map.of("address.street", List.of("must not be blank")), """
        <sl:field th:field="*{address.street}">
          <sl:field-label>Street</sl:field-label>
          <sl:input/>
          <sl:field-error/>
        </sl:field>""");

    Element input = rendered.select("input").first();
    assertThat(input.id()).isEqualTo("address.street");
    assertThat(input.attr("name")).isEqualTo("address.street");
    assertThat(input.attr("aria-describedby")).isEqualTo("address.street-error");
    assertThat(rendered.select(".field-error").first().text()).isEqualTo("must not be blank");
  }

  @Test
  void theDescriptionOfANestedFieldIsNotTheOuterFieldsDescription() {
    Rendered rendered = render(new Signup(), Map.of(), """
        <sl:field th:field="*{email}">
          <sl:input/>
          <sl:field for="nested"><sl:field-description>Inner</sl:field-description></sl:field>
        </sl:field>""");

    assertThat(rendered.select("input").first().hasAttr("aria-describedby")).isFalse();
    assertThat(rendered.select(".field-description").first().id()).isEqualTo("nested-description");
  }

  /** Every entry in {@code errors} is rejected on the form object's binding, one message at a time. */
  private Rendered render(Signup signup, Map<String, List<String>> errors, String fields) {
    BindingResult bindingResult = new BeanPropertyBindingResult(signup, "signup");
    errors.forEach((field, messages) -> messages.forEach(message ->
        bindingResult.rejectValue(field, "Invalid", message)));
    Map<String, Object> variables = new HashMap<>();
    variables.put("signup", signup);
    variables.put(BindingResult.MODEL_KEY_PREFIX + "signup", bindingResult);
    return renderer.render("<form th:object=\"${signup}\">" + fields + "</form>", variables);
  }

  /** A form backing object with JavaBeans accessors, as Spring's data binding expects. */
  public static class Signup {

    private String email = "wim@example.com";
    private String bio = "Hello there";
    private String plan = "pro";
    private boolean terms;
    private boolean newsletter = true;
    private List<String> toppings = List.of("cheese");
    private Address address = new Address();

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

    public boolean isTerms() {
      return terms;
    }

    public void setTerms(boolean terms) {
      this.terms = terms;
    }

    public boolean isNewsletter() {
      return newsletter;
    }

    public void setNewsletter(boolean newsletter) {
      this.newsletter = newsletter;
    }

    public List<String> getToppings() {
      return toppings;
    }

    public void setToppings(List<String> toppings) {
      this.toppings = toppings;
    }

    public Address getAddress() {
      return address;
    }

    public void setAddress(Address address) {
      this.address = address;
    }
  }

  public static class Address {

    private String street = "";

    public String getStreet() {
      return street;
    }

    public void setStreet(String street) {
      this.street = street;
    }
  }
}
