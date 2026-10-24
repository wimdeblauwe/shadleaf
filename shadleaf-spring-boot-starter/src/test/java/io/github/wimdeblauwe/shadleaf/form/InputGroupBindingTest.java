package io.github.wimdeblauwe.shadleaf.form;

import static io.github.wimdeblauwe.shadleaf.test.ShadleafAssertions.assertThatRenderFailure;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.wimdeblauwe.shadleaf.test.ComponentRenderTester;
import io.github.wimdeblauwe.shadleaf.test.FormModel;
import io.github.wimdeblauwe.shadleaf.test.Rendered;
import java.util.List;
import java.util.Map;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.Test;

/**
 * {@code <sl:input-group>} in a {@code th:object} form: its input or textarea binds with {@code th:field} as
 * {@code sl:input} does, and inside a {@code sl:field} it is the field's control, which the label, description and
 * error point at, past the group around it.
 */
class InputGroupBindingTest {

  private final ComponentRenderTester renderer = ComponentRenderTester.create();

  @Test
  void inputTakesItsIdNameAndValueFromTheField() {
    Element input = render(Map.of("amount", "12.50"), Map.of(), """
        <sl:input-group>
          <sl:input-group-input type="number" th:field="*{amount}"/>
          <sl:input-group-addon><sl:input-group-text>$</sl:input-group-text></sl:input-group-addon>
        </sl:input-group>""").select("input").first();

    assertThat(input.classNames()).containsExactly("input-group-input");
    assertThat(input.attr("type")).isEqualTo("number");
    assertThat(input.id()).isEqualTo("amount");
    assertThat(input.attr("name")).isEqualTo("amount");
    assertThat(input.val()).isEqualTo("12.50");
    assertThat(input.hasAttr("aria-invalid")).isFalse();
  }

  @Test
  void inputWithAFieldErrorIsInvalid() {
    Element input = render(Map.of("amount", "-1"), Map.of("amount", List.of("must be positive")), """
        <sl:input-group><sl:input-group-input th:field="*{amount}"/></sl:input-group>""").select("input").first();

    assertThat(input.attr("aria-invalid")).isEqualTo("true");
  }

  @Test
  void fieldWiresTheLabelDescriptionAndErrorToTheInputInTheGroup() {
    Rendered rendered = render(Map.of("weight", "-2"), Map.of("weight", List.of("must be greater than 0")), """
        <sl:field th:field="*{weight}">
          <sl:field-label>Weight</sl:field-label>
          <sl:input-group>
            <sl:input-group-input type="number"/>
            <sl:input-group-addon align="inline-end">
              <sl:input-group-text>kg</sl:input-group-text>
              <sl:input-group-button size="icon-xs" aria-label="Clear"><sl:icon name="x"/></sl:input-group-button>
            </sl:input-group-addon>
          </sl:input-group>
          <sl:field-description>Of the parcel.</sl:field-description>
          <sl:field-error/>
        </sl:field>""");

    Element field = rendered.select(".field").first();
    Element input = field.selectFirst(".input-group > input.input-group-input");
    assertThat(field.attr("data-invalid")).isEqualTo("true");
    assertThat(field.selectFirst("label.field-label").attr("for")).isEqualTo("weight");
    assertThat(input.id()).isEqualTo("weight");
    assertThat(input.attr("name")).isEqualTo("weight");
    assertThat(input.val()).isEqualTo("-2");
    assertThat(input.attr("aria-invalid")).isEqualTo("true");
    assertThat(input.attr("aria-describedby")).isEqualTo("weight-description weight-error");
    assertThat(field.selectFirst(".field-error").text()).isEqualTo("must be greater than 0");
    Element button = field.selectFirst("button.input-group-button");
    assertThat(button.hasAttr("name")).isFalse();
    assertThat(button.hasAttr("id")).isFalse();
    assertThat(button.attr("type")).isEqualTo("button");
  }

  @Test
  void textareaInAFieldTakesTheFieldAndItsText() {
    Rendered rendered = render(Map.of("comment", "Looks good"), Map.of(), """
        <sl:field th:field="*{comment}">
          <sl:field-label>Comment</sl:field-label>
          <sl:input-group>
            <sl:input-group-textarea rows="3">ignored</sl:input-group-textarea>
            <sl:input-group-addon align="block-end" border>
              <sl:input-group-text>0/280</sl:input-group-text>
            </sl:input-group-addon>
          </sl:input-group>
        </sl:field>""");

    Element textarea = rendered.select("textarea").first();
    assertThat(textarea.classNames()).containsExactly("input-group-textarea");
    assertThat(textarea.id()).isEqualTo("comment");
    assertThat(textarea.attr("name")).isEqualTo("comment");
    assertThat(textarea.text()).isEqualTo("Looks good");
    assertThat(rendered.select(".input-group-addon").first().attr("data-border")).isEqualTo("top");
  }

  @Test
  void iconButtonNeedsAnAccessibleName() {
    assertThatRenderFailure(() -> renderer.render(
        "<sl:input-group-button size=\"icon-xs\"><sl:icon name=\"copy\"/></sl:input-group-button>"))
        .hasMessageContaining("aria-label");
  }

  private Rendered render(Map<String, ?> values, Map<String, List<String>> errors, String snippet) {
    FormModel form = FormModel.of(values, errors);
    return renderer.render(FormModel.wrap(snippet), form.variables());
  }
}
