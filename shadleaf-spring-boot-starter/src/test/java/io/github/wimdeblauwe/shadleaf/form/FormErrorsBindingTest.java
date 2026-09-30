package io.github.wimdeblauwe.shadleaf.form;

import static io.github.wimdeblauwe.shadleaf.test.ShadleafAssertions.assertThatRenderFailure;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.wimdeblauwe.shadleaf.form.FieldBindingTest.Signup;
import io.github.wimdeblauwe.shadleaf.i18n.ShadleafMessageSource;
import io.github.wimdeblauwe.shadleaf.test.ComponentRenderTester;
import io.github.wimdeblauwe.shadleaf.test.Rendered;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;

/**
 * {@code <sl:form-errors>} inside a {@code th:object} form: it shows the binding's global errors, as
 * {@code reject(...)} or a class-level constraint adds them, and nothing else.
 */
class FormErrorsBindingTest {

  private final ComponentRenderTester renderer = ComponentRenderTester.create();

  @Test
  void rendersNothingWithoutGlobalErrors() {
    Rendered rendered = render(renderer, bindingResult -> bindingResult.rejectValue("email", "Invalid", "is taken"),
        "<sl:form-errors/>");

    assertThat(rendered.select(".form-errors")).isEmpty();
  }

  @Test
  void oneGlobalErrorIsADestructiveAlertWithTheMessageAsText() {
    Rendered rendered = render(renderer, bindingResult -> {
      bindingResult.reject("Invalid", "The passwords do not match");
      bindingResult.rejectValue("email", "Invalid", "is taken");
    }, "<sl:form-errors/>");

    Element errors = rendered.select(".form-errors").first();
    assertThat(errors.classNames()).containsExactly("form-errors", "alert");
    assertThat(errors.attr("data-variant")).isEqualTo("destructive");
    assertThat(errors.id()).isEqualTo("signup-errors");
    assertThat(errors.attr("tabindex")).isEqualTo("-1");
    assertThat(errors.hasAttr("role")).isFalse();
    assertThat(errors.hasAttr("autofocus")).isFalse();
    assertThat(errors.selectFirst("> svg").attr("aria-hidden")).isEqualTo("true");
    Element title = errors.selectFirst("> .alert-title");
    assertThat(title.text()).isEqualTo("There is a problem");
    assertThat(title.hasAttr("role")).isFalse();
    Element description = errors.selectFirst("> .alert-description");
    assertThat(description.text()).as("field errors stay with their fields").isEqualTo("The passwords do not match");
    assertThat(description.select("ul")).isEmpty();
  }

  @Test
  void severalGlobalErrorsBecomeAListWithEachMessageOnce() {
    Rendered rendered = render(renderer, bindingResult -> {
      bindingResult.reject("Invalid", "The passwords do not match");
      bindingResult.reject("Invalid", "The service is not available");
      bindingResult.reject("Invalid", "The passwords do not match");
    }, "<sl:form-errors/>");

    assertThat(rendered.select(".form-errors .alert-description > ul > li")).extracting(Element::text)
        .containsExactly("The passwords do not match", "The service is not available");
  }

  @Test
  void allListsTheFieldErrorsAfterTheGlobalOnesEachMessageOncePerField() {
    Rendered rendered = render(renderer, bindingResult -> {
      bindingResult.rejectValue("email", "Invalid", "must not be blank");
      bindingResult.rejectValue("email", "Invalid", "must not be blank");
      bindingResult.reject("Invalid", "The passwords do not match");
      bindingResult.rejectValue("bio", "Invalid", "must not be blank");
      bindingResult.rejectValue("email", "Invalid", "is taken");
    }, "<sl:form-errors show=\"all\"/>");

    assertThat(rendered.select(".form-errors .alert-description > ul > li")).extracting(Element::text)
        .as("in the order they were rejected; the same message on two fields shows twice")
        .containsExactly("The passwords do not match", "must not be blank", "must not be blank", "is taken");
  }

  @Test
  void allShowsFieldErrorsWithoutAGlobalOne() {
    Rendered rendered = render(renderer, bindingResult -> bindingResult.rejectValue("email", "Invalid", "is taken"),
        "<sl:form-errors show=\"all\"/>");

    assertThat(rendered.select(".form-errors .alert-description").text()).isEqualTo("is taken");
  }

  @Test
  void allRendersNothingWithoutErrors() {
    Rendered rendered = render(renderer, bindingResult -> { }, "<sl:form-errors show=\"all\"/>");

    assertThat(rendered.select(".form-errors")).isEmpty();
  }

  @Test
  void errorCodesResolveThroughTheApplicationsMessages() {
    StaticMessageSource messages = new StaticMessageSource();
    messages.addMessage("signup.passwords", Locale.ENGLISH, "Type the same password twice");
    ShadleafMessageSource.attachTo(messages);
    ComponentRenderTester english = ComponentRenderTester.builder().messageSource(messages).locale(Locale.ENGLISH)
        .build();

    Rendered rendered = render(english, bindingResult -> bindingResult.reject("signup.passwords"),
        "<sl:form-errors/>");

    assertThat(rendered.select(".form-errors .alert-description").text()).isEqualTo("Type the same password twice");
    assertThat(rendered.select(".form-errors .alert-title").text()).isEqualTo("There is a problem");
  }

  @Test
  void theDefaultTitleComesFromTheApplicationsMessages() {
    StaticMessageSource messages = new StaticMessageSource();
    messages.addMessage("sl.form-errors.title", Locale.forLanguageTag("nl"), "Er is een probleem");
    ShadleafMessageSource.attachTo(messages);
    ComponentRenderTester dutch = ComponentRenderTester.builder().messageSource(messages)
        .locale(Locale.forLanguageTag("nl")).build();

    Rendered rendered = render(dutch, bindingResult -> bindingResult.reject("Invalid", "Mislukt"),
        "<sl:form-errors/>");

    assertThat(rendered.select(".form-errors .alert-title").text()).isEqualTo("Er is een probleem");
  }

  @Test
  void titleAndLevelMakeTheTitleAHeadingOfYourOwn() {
    Rendered rendered = render(renderer, bindingResult -> bindingResult.reject("Invalid", "Failed"),
        "<sl:form-errors title=\"Could not sign you up\" level=\"2\"/>");

    Element title = rendered.select(".form-errors .alert-title").first();
    assertThat(title.text()).isEqualTo("Could not sign you up");
    assertThat(title.attr("role")).isEqualTo("heading");
    assertThat(title.attr("aria-level")).isEqualTo("2");
    assertThat(rendered.select(".form-errors").first().hasAttr("title")).isFalse();
  }

  @Test
  void aTitleExpressionSeesTheCallersFormObject() {
    Rendered rendered = render(renderer, bindingResult -> {
      bindingResult.reject("Invalid", "Failed");
      bindingResult.rejectValue("email", "Invalid", "is taken");
    }, "<sl:form-errors show=\"all\" th:title=\"|${#fields.errors().size()} errors|\"/>");

    assertThat(rendered.select(".form-errors .alert-title").text()).isEqualTo("2 errors");
  }

  @Test
  void contentOfItsOwnReplacesTheMessagesAndAlwaysShows() {
    Rendered rendered = render(renderer, bindingResult -> { },
        "<sl:form-errors>We could not reach the payment service. Try again in a minute.</sl:form-errors>");

    assertThat(rendered.select(".form-errors .alert-description").text())
        .isEqualTo("We could not reach the payment service. Try again in a minute.");
  }

  @Test
  void anIdAClassAndAutofocusOfItsOwnPassThrough() {
    Rendered rendered = render(renderer, bindingResult -> bindingResult.reject("Invalid", "Failed"),
        "<sl:form-errors id=\"top-errors\" class=\"mb-4\" autofocus/>");

    Element errors = rendered.select(".form-errors").first();
    assertThat(errors.id()).isEqualTo("top-errors");
    assertThat(errors.classNames()).containsExactly("form-errors", "alert", "mb-4");
    assertThat(errors.hasAttr("autofocus")).isTrue();
  }

  @Test
  void theIdFollowsTheFormObjectsName() {
    BindingResult bindingResult = new BeanPropertyBindingResult(new Signup(), "newAccount");
    bindingResult.reject("Invalid", "Failed");
    Map<String, Object> variables = new HashMap<>();
    variables.put("newAccount", bindingResult.getTarget());
    variables.put(BindingResult.MODEL_KEY_PREFIX + "newAccount", bindingResult);

    Rendered rendered = renderer.render("<form th:object=\"${newAccount}\"><sl:form-errors/></form>", variables);

    assertThat(rendered.select(".form-errors").first().id()).isEqualTo("newAccount-errors");
  }

  @Test
  void outsideAFormObjectItFails() {
    assertThatRenderFailure(() -> renderer.render("<sl:form-errors/>"))
        .hasMessageContaining("th:object");
  }

  private Rendered render(ComponentRenderTester tester, Consumer<BindingResult> errors, String snippet) {
    Signup signup = new Signup();
    BindingResult bindingResult = new BeanPropertyBindingResult(signup, "signup");
    errors.accept(bindingResult);
    Map<String, Object> variables = new HashMap<>();
    variables.put("signup", signup);
    variables.put(BindingResult.MODEL_KEY_PREFIX + "signup", bindingResult);
    return tester.render("<form th:object=\"${signup}\">" + snippet + "</form>", variables);
  }
}
