package io.github.wimdeblauwe.shadleaf.form;

import static io.github.wimdeblauwe.shadleaf.test.ShadleafAssertions.assertThat;
import static io.github.wimdeblauwe.shadleaf.test.ShadleafAssertions.assertThatRenderFailure;

import io.github.wimdeblauwe.shadleaf.form.FieldBindingTest.Signup;
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
 * {@code <sl:form validate>} and {@code sl:validate}: the htmx attributes a control gets to be validated on the server
 * while the user edits it, the {@code data-validate-field} its field is selected by, and the {@code <hx-partial>} a
 * field with {@code depends-on} wraps itself in when a validation request renders.
 */
class FormValidateTest {

  private static final String VALIDATE = "th:validate=\"@{/signup/validate}\"";

  private final ComponentRenderTester renderer = ComponentRenderTester.create();

  @Test
  void theFormSelectsItsObjectAndPassesItsAttributesOn() {
    Rendered rendered = render(renderer, Map.of(), """
        <sl:form th:object="${signup}" th:action="@{/signup}" method="post" novalidate>
          <sl:field th:field="*{email}"><sl:input type="email"/></sl:field>
        </sl:form>""");

    Element form = rendered.select("form").first();
    assertThat(form).hasClass("form").hasAttribute("action", "/signup").hasAttribute("method", "post")
        .hasAttribute("novalidate").hasNoAttribute("th:object", "validate");
    assertThat(form.selectFirst("input")).hasAttribute("name", "email").hasAttribute("value", "wim@example.com")
        .hasNoAttribute("hx-post", "sl:validate");
    assertThat(form.selectFirst(".field")).hasNoAttribute("data-validate-field");
  }

  @Test
  void aTypedControlPostsTheFormAndSwapsItsField() {
    ComponentRenderTester tester = ComponentRenderTester.builder().contextPath("/app").build();
    Rendered rendered = render(tester, Map.of(), form("""
        <sl:field th:field="*{email}"><sl:input type="email"/></sl:field>"""));

    assertThat(rendered.select(".field").first()).hasAttribute("data-validate-field", "email");
    assertThat(rendered.select("input").first())
        .hasAttribute("hx-post", "/app/signup/validate")
        .hasAttribute("hx-trigger", "input changed delay:200ms")
        .hasAttribute("hx-include", "closest form")
        .hasAttribute("hx-swap", "outerMorph")
        .hasAttribute("hx-target", "closest .field")
        .hasAttribute("hx-select", "[data-validate-field='email']")
        .hasAttribute("hx-headers", "SL-Validate-Field:'email'")
        .hasNoAttribute("sl:validate");
    assertThat(rendered).hasNoElement("hx-partial");
  }

  @Test
  void typedControlsValidateWhileTypingAndPickedOnesOnChange() {
    Rendered rendered = render(renderer, Map.of(), form("""
        <sl:field th:field="*{bio}"><sl:textarea/></sl:field>
        <sl:field th:field="*{email}"><sl:input type="number"/></sl:field>
        <sl:field th:field="*{email}"><sl:input type="date"/></sl:field>
        <sl:field th:field="*{email}"><sl:input type="time"/></sl:field>
        <sl:field th:field="*{plan}"><sl:native-select><option value="pro">Pro</option></sl:native-select></sl:field>
        <sl:field th:field="*{plan}"><sl:select><sl:select-item value="pro">Pro</sl:select-item></sl:select></sl:field>
        <sl:field th:field="*{terms}"><sl:checkbox/></sl:field>
        <sl:field th:field="*{newsletter}"><sl:switch/></sl:field>
        <sl:field th:field="*{plan}"><input type="hidden"/></sl:field>"""));

    assertThat(rendered.select("textarea").first()).hasAttribute("hx-trigger", "input changed delay:200ms");
    assertThat(rendered.select("input[type=number]").first()).hasAttribute("hx-trigger", "input changed delay:200ms");
    assertThat(rendered.select("input[type=date]").first()).hasAttribute("hx-trigger", "change");
    assertThat(rendered.select("input[type=time]").first()).hasAttribute("hx-trigger", "change");
    assertThat(rendered.select("select.native-select").first()).hasAttribute("hx-trigger", "change")
        .hasAttribute("hx-select", "[data-validate-field='plan']");
    assertThat(rendered.select("select.select").first()).hasAttribute("hx-trigger", "change");
    assertThat(rendered.select("input.checkbox").first()).hasAttribute("hx-trigger", "change")
        .hasAttribute("hx-select", "[data-validate-field='terms']");
    assertThat(rendered.select("input.switch").first()).hasAttribute("hx-trigger", "change");
    // A plain control in a field takes nothing from it: no #slFields.control, so no sl:validate.
    assertThat(rendered.select("input[type=hidden]").first()).hasNoAttribute("hx-post");
    // Spring's marker inputs for unchecked checkboxes are no controls of their own.
    assertThat(rendered.select("input[name=_terms]").first()).hasNoAttribute("hx-post");
  }

  @Test
  void aGroupIsSwappedAsAWholeForEachOfItsOptions() {
    Rendered rendered = render(renderer, Map.of(), form("""
        <sl:field-set th:field="*{plan}">
          <sl:radio-group>
            <sl:field><sl:radio-group-item value="free"/></sl:field>
            <sl:field><sl:radio-group-item value="pro"/></sl:field>
          </sl:radio-group>
        </sl:field-set>
        <sl:field-set th:field="*{toppings}">
          <sl:field><sl:checkbox value="cheese"/></sl:field>
          <sl:field><sl:checkbox value="ham"/></sl:field>
        </sl:field-set>"""));

    assertThat(rendered.select("fieldset").eachAttr("data-validate-field")).containsExactly("plan", "toppings");
    assertThat(rendered.select(".field[data-validate-field]")).isEmpty();
    for (Element radio : rendered.select("input[type=radio]")) {
      assertThat(radio).hasAttribute("hx-trigger", "change").hasAttribute("hx-target", "closest .field-set")
          .hasAttribute("hx-select", "[data-validate-field='plan']").hasAttribute("hx-headers", "SL-Validate-Field:'plan'");
    }
    for (Element checkbox : rendered.select("input.checkbox")) {
      assertThat(checkbox).hasAttribute("hx-target", "closest .field-set")
          .hasAttribute("hx-select", "[data-validate-field='toppings']");
    }
  }

  @Test
  void aNestedPathIsValidatedUnderItsParameterName() {
    Rendered rendered = render(renderer, Map.of(), form("""
        <sl:field th:field="*{address.street}"><sl:input/></sl:field>"""));

    assertThat(rendered.select(".field").first()).hasAttribute("data-validate-field", "address.street");
    assertThat(rendered.select("input").first()).hasAttribute("name", "address.street")
        .hasAttribute("hx-headers", "SL-Validate-Field:'address.street'");
  }

  @Test
  void aControlOptsOutWithFalse() {
    Rendered rendered = render(renderer, Map.of(), form("""
        <sl:field th:field="*{email}"><sl:input sl:validate="false"/></sl:field>"""));

    assertThat(rendered.select("input").first()).hasNoAttribute("sl:validate", "hx-post", "hx-trigger", "hx-target");
  }

  @Test
  void theAuthorsOwnAttributesAreKept() {
    Rendered rendered = render(renderer, Map.of(), form("""
        <sl:field th:field="*{email}"><sl:input hx-trigger="blur" data-hx-target="#somewhere"/></sl:field>
        <sl:field th:field="*{bio}"><sl:textarea th:hx-post="@{/elsewhere}" hx:swap="innerHTML"/></sl:field>
        <sl:field th:field="*{plan}"><sl:input hx-get="/plans" hx-headers="X-Plan:'1'"/></sl:field>"""));

    assertThat(rendered.select("input[name=email]").first()).hasAttribute("hx-trigger", "blur")
        .hasAttribute("data-hx-target", "#somewhere").hasNoAttribute("hx-target")
        .hasAttribute("hx-post", "/signup/validate").hasAttribute("hx-swap", "outerMorph");
    assertThat(rendered.select("textarea").first()).hasAttribute("hx-post", "/elsewhere")
        .hasNoAttribute("hx-swap").hasAttribute("hx-target", "closest .field");
    assertThat(rendered.select("input[name=plan]").first()).hasAttribute("hx-get", "/plans")
        .hasNoAttribute("hx-post").hasAttribute("hx-headers", "X-Plan:'1'");
  }

  @Test
  void nothingIsValidatedOutsideAValidatingForm() {
    Rendered rendered = render(renderer, Map.of(), """
        <sl:form th:object="${signup}">
          <sl:field th:field="*{email}"><sl:input/></sl:field>
        </sl:form>
        <form th:object="${signup}">
          <sl:field th:field="*{bio}"><sl:textarea/></sl:field>
          <sl:field-set th:field="*{plan}"><sl:radio-group><sl:radio-group-item value="pro"/></sl:radio-group></sl:field-set>
        </form>""");

    assertThat(rendered.select("[data-validate-field]")).isEmpty();
    assertThat(rendered.select("[hx-post], [sl\\:validate]")).isEmpty();
  }

  @Test
  void aControlWithoutAFieldIsNotValidated() {
    Rendered rendered = render(renderer, Map.of(), form("""
        <sl:input th:field="*{email}"/>
        <sl:field><sl:input name="nickname"/></sl:field>"""));

    assertThat(rendered.select("[hx-post]")).isEmpty();
    assertThat(rendered.select("[data-validate-field]")).isEmpty();
  }

  @Test
  void aControlInAFieldValidatesWithoutAForm() {
    Rendered rendered = render(renderer, Map.of(), """
        <form th:object="${signup}">
          <sl:field th:field="*{email}"><sl:input sl:validate="/signup/validate"/></sl:field>
          <sl:field th:field="*{bio}"><sl:textarea/></sl:field>
        </form>""");

    assertThat(rendered.select(".field").eachAttr("data-validate-field")).containsExactly("email");
    assertThat(rendered.select("input").first()).hasAttribute("hx-post", "/signup/validate")
        .hasAttribute("hx-target", "closest .field").hasAttribute("hx-select", "[data-validate-field='email']")
        .hasNoAttribute("sl:validate");
    assertThat(rendered.select("textarea").first()).hasNoAttribute("hx-post");
  }

  @Test
  void aPlainControlWithSlValidateGetsTheRequestAttributesOnly() {
    Rendered rendered = renderer.render("""
        <form><input name="q" sl:validate="@{/search/validate}"></form>""");

    assertThat(rendered.select("input").first()).hasAttribute("hx-post", "/search/validate")
        .hasAttribute("hx-trigger", "input changed delay:200ms").hasAttribute("hx-swap", "outerMorph")
        .hasNoAttribute("hx-target", "hx-select", "hx-headers", "sl:validate");
  }

  @Test
  void slValidateNeedsAUrl() {
    assertThatRenderFailure(() -> renderer.render("<input name=\"q\" sl:validate=\"\">"))
        .hasMessageContaining("sl:validate needs the URL of the validation endpoint");
  }

  @Test
  void aFileInputIsNotValidated() {
    Rendered rendered = render(renderer, Map.of(), """
        <sl:form th:object="${signup}" th:validate="@{/signup/validate}" method="post" enctype="multipart/form-data">
          <sl:field th:field="*{email}"><sl:input/></sl:field>
          <sl:field th:field="*{bio}"><sl:input type="file"/></sl:field>
          <sl:field><input type="file" name="photo" sl:validate="/signup/validate"></sl:field>
        </sl:form>""");

    assertThat(rendered.select("input[name=email]").first()).hasAttribute("hx-post").hasNoAttribute("hx-encoding");
    for (Element file : rendered.select("input[type=file]")) {
      assertThat(file).hasNoAttribute("hx-post", "hx-trigger", "sl:validate");
    }
  }

  @Test
  void aDependentFieldIsLeftAloneOutsideAValidationRequest() {
    assertThat(renderDependent(renderer)).hasNoElement("hx-partial");
  }

  @Test
  void aDependentFieldIsSwappedWhenAFieldItDependsOnIsValidatedAndItHasAValue() {
    Rendered rendered = renderDependent(validating("email").parameter("bio", "Hi").build());

    Element partial = rendered.select("hx-partial").first();
    assertThat(partial).hasAttribute("hx-target", "[data-validate-field='bio']").hasAttribute("hx-swap", "outerMorph")
        .hasNoAttribute("th:remove");
    assertThat(partial.child(0)).hasClass("field").hasAttribute("data-validate-field", "bio");
    assertThat(rendered.select("hx-partial")).hasSize(1);
    assertThat(rendered.select(".field[data-validate-field=email]").first().parent().tagName()).isEqualTo("form");
  }

  @Test
  void dependsOnTakesSpacesAndCommas() {
    Rendered rendered = render(validating("plan").parameter("bio", "Hi").build(), Map.of(), form("""
        <sl:field th:field="*{bio}" depends-on="email, plan terms"><sl:textarea/></sl:field>"""));

    assertThat(rendered.select("hx-partial")).hasSize(1);
  }

  @Test
  void aDependentFieldIsNotSwappedForItselfAnUnrelatedFieldOrWithoutAValue() {
    assertThat(renderDependent(validating("bio").parameter("bio", "Hi").build())).hasNoElement("hx-partial");
    assertThat(renderDependent(validating("plan").parameter("bio", "Hi").build())).hasNoElement("hx-partial");
    assertThat(renderDependent(validating("email").parameter("bio", "  ").build())).hasNoElement("hx-partial");
    assertThat(renderDependent(validating("email").build())).hasNoElement("hx-partial");
  }

  @Test
  void aDependentGroupIsSwappedWhenOneOfItsOptionsIsChosen() {
    Rendered rendered = render(validating("email").parameter("toppings", "ham", "cheese").build(), Map.of(),
        form("""
            <sl:field-set th:field="*{toppings}" depends-on="email">
              <sl:field><sl:checkbox value="cheese"/></sl:field>
            </sl:field-set>"""));

    Element partial = rendered.select("hx-partial").first();
    assertThat(partial).hasAttribute("hx-target", "[data-validate-field='toppings']");
    assertThat(partial.child(0)).hasTag("fieldset");
  }

  @Test
  void aValidationResponseMovesNoFocus() {
    String snippet = form("""
        <sl:form-errors autofocus/>
        <sl:field th:field="*{email}"><sl:input/><sl:field-error/></sl:field>""");
    Map<String, List<String>> errors = Map.of("email", List.of("is taken"));

    assertThat(render(renderer, errors, snippet).select("input").first()).hasAttribute("autofocus");
    assertThat(render(validating("email").build(), errors, snippet).select("input").first())
        .hasNoAttribute("autofocus");
  }

  @Test
  void aValidationResponseDoesNotFocusTheSummary() {
    String snippet = form("<sl:form-errors autofocus><p>Something went wrong.</p></sl:form-errors>");

    assertThat(render(renderer, Map.of(), snippet).select(".form-errors").first()).hasAttribute("autofocus");
    assertThat(render(validating("email").build(), Map.of(), snippet).select(".form-errors").first())
        .hasNoAttribute("autofocus");
  }

  private static ComponentRenderTester.Builder validating(String field) {
    return ComponentRenderTester.builder().header("HX-Request", "true").header("SL-Validate-Field", field);
  }

  private static Rendered renderDependent(ComponentRenderTester tester) {
    return render(tester, Map.of(), form("""
        <sl:field th:field="*{email}"><sl:input/></sl:field>
        <sl:field th:field="*{bio}" depends-on="email"><sl:textarea/></sl:field>"""));
  }

  private static String form(String fields) {
    return "<sl:form th:object=\"${signup}\" " + VALIDATE + " method=\"post\">" + fields + "</sl:form>";
  }

  private static Rendered render(ComponentRenderTester tester, Map<String, List<String>> errors, String snippet) {
    Signup signup = new Signup();
    BindingResult bindingResult = new BeanPropertyBindingResult(signup, "signup");
    errors.forEach((field, messages) -> messages.forEach(message ->
        bindingResult.rejectValue(field, "Invalid", message)));
    Map<String, Object> variables = new HashMap<>();
    variables.put("signup", signup);
    variables.put(BindingResult.MODEL_KEY_PREFIX + "signup", bindingResult);
    return tester.render(snippet, variables);
  }
}
