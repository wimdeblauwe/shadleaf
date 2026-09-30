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
 * {@code <sl:select>} inside a {@code th:object} form: {@code th:field} on the select (or on its field) marks the bound
 * item among the options its parts render, a field with errors marks the select {@code aria-invalid}, and a field or
 * field set wires the id, label and description as for {@code sl:native-select}.
 */
class SelectBindingTest {

  private static final String ITEMS = """
      <sl:select-group label="Plans">
        <sl:select-item value="free">Free</sl:select-item>
        <sl:select-item value="pro">Pro</sl:select-item>
      </sl:select-group>
      <sl:select-separator/>
      <sl:select-item value="team" disabled>Team</sl:select-item>""";

  private final ComponentRenderTester renderer = ComponentRenderTester.create();

  @Test
  void thFieldMarksTheBoundItemAndKeepsClassOnTheWrapper() {
    Rendered rendered = render(new Order("pro"), Map.of(), """
        <sl:select th:field="*{plan}" class="w-full" aria-label="Plan">%s</sl:select>""".formatted(ITEMS));

    Element wrapper = rendered.select(".select-wrapper").first();
    Element select = wrapper.selectFirst("select");
    assertThat(wrapper.classNames()).containsExactly("select-wrapper", "w-full");
    assertThat(wrapper.attr("x-data")).isEqualTo("slSelect");
    assertThat(select.classNames()).containsExactly("select");
    assertThat(select.id()).isEqualTo("plan");
    assertThat(select.attr("name")).isEqualTo("plan");
    assertThat(select.select("option[selected]")).extracting(Element::val).containsExactly("pro");
    assertThat(select.hasAttr("aria-invalid")).isFalse();
    assertThat(select.child(0).tagName()).as("the button of the customizable select comes first").isEqualTo("button");
    assertThat(select.selectFirst("button > selectedcontent")).isNotNull();
  }

  @Test
  void itemsFromTheModelTakeThValueAndTheBoundOneIsSelected() {
    Rendered rendered = renderWith(new Order("team"), Map.of("plans", List.of("free", "pro", "team")), Map.of(), """
        <sl:select th:field="*{plan}" aria-label="Plan">
          <sl:select-item th:each="plan : ${plans}" th:value="${plan}">[[${plan}]]</sl:select-item>
        </sl:select>""");

    Element select = rendered.select("select").first();
    assertThat(select.select("option.select-item")).extracting(Element::val).containsExactly("free", "pro", "team");
    assertThat(select.select("option[selected]")).extracting(Element::val).containsExactly("team");
    assertThat(select.select("option.select-item > .select-item-indicator")).hasSize(3);
  }

  @Test
  void withoutAValueThePlaceholderIsTheChosenOption() {
    Rendered rendered = render(new Order(null), Map.of(), """
        <sl:select th:field="*{plan}" placeholder="Choose a plan" aria-label="Plan">%s</sl:select>"""
        .formatted(ITEMS));

    Element select = rendered.select("select").first();
    Element placeholder = select.select("option").first();
    assertThat(placeholder.val()).isEmpty();
    assertThat(placeholder.hasAttr("hidden")).isTrue();
    assertThat(placeholder.hasAttr("disabled")).as("the browser only chooses an enabled first option").isFalse();
    assertThat(placeholder.attr("data-placeholder")).isEqualTo("true");
    assertThat(placeholder.text()).isEqualTo("Choose a plan");
    assertThat(select.select("option[selected]").stream().map(Element::val))
        .as("none of the real options, so the browser chooses the placeholder")
        .allMatch(String::isEmpty);
  }

  @Test
  void aFieldWithErrorsMarksTheSelectInvalid() {
    Element select = render(new Order("gold"), Map.of("plan", List.of("is not a plan")), """
        <sl:select th:field="*{plan}" aria-label="Plan">%s</sl:select>""".formatted(ITEMS))
        .select("select").first();

    assertThat(select.attr("aria-invalid")).isEqualTo("true");
  }

  @Test
  void inAFieldItTakesTheFieldsIdLabelAndDescription() {
    Rendered rendered = render(new Order("free"), Map.of("plan", List.of("is not available")), """
        <sl:field th:field="*{plan}">
          <sl:field-label>Plan</sl:field-label>
          <sl:select aria-describedby="plan-help">%s</sl:select>
          <sl:field-description>Billed monthly.</sl:field-description>
          <sl:field-error/>
        </sl:field>""".formatted(ITEMS));

    Element select = rendered.select("select").first();
    assertThat(select.id()).isEqualTo("plan");
    assertThat(select.attr("name")).isEqualTo("plan");
    assertThat(select.attr("aria-invalid")).isEqualTo("true");
    assertThat(select.attr("aria-describedby")).isEqualTo("plan-help plan-description plan-error");
    assertThat(select.select("option[selected]")).extracting(Element::val).containsExactly("free");
    assertThat(rendered.select("label.field-label").first().attr("for")).isEqualTo("plan");
    assertThat(rendered.select(".field").first().attr("data-invalid")).isEqualTo("true");
  }

  @Test
  void inAFieldSetItTakesTheGroupsFieldButNoId() {
    Rendered rendered = render(new Order("pro"), Map.of("plan", List.of("is not available")), """
        <sl:field-set th:field="*{plan}">
          <sl:field-legend>Plan</sl:field-legend>
          <sl:select aria-label="Plan">%s</sl:select>
          <sl:field-error/>
        </sl:field-set>""".formatted(ITEMS));

    Element fieldSet = rendered.select("fieldset").first();
    Element select = fieldSet.selectFirst("select");
    assertThat(fieldSet.attr("aria-describedby")).isEqualTo("plan-error");
    assertThat(select.attr("name")).isEqualTo("plan");
    assertThat(select.attr("aria-invalid")).isEqualTo("true");
    assertThat(select.hasAttr("aria-describedby")).isFalse();
    assertThat(select.select("option[selected]")).extracting(Element::val).containsExactly("pro");
  }

  @Test
  void theFormSummaryGivesAutofocusToAnInvalidSelect() {
    Rendered rendered = render(new Order("gold"), Map.of("plan", List.of("is not a plan")), """
        <sl:form-errors autofocus/>
        <sl:field th:field="*{plan}">
          <sl:field-label>Plan</sl:field-label>
          <sl:select>%s</sl:select>
          <sl:field-error/>
        </sl:field>""".formatted(ITEMS));

    assertThat(rendered.select("[autofocus]")).extracting(Element::tagName, Element::id)
        .containsExactly(tuple("select", "plan"));
  }

  @Test
  void disabledPassesThroughToTheSelect() {
    Element select = render(new Order("pro"), Map.of(), """
        <sl:select th:field="*{plan}" aria-label="Plan" disabled>%s</sl:select>""".formatted(ITEMS))
        .select("select").first();

    assertThat(select.hasAttr("disabled")).isTrue();
    assertThat(select.select("option[value=team]").first().hasAttr("disabled")).isTrue();
  }

  /** Every entry in {@code errors} is rejected on the form object's binding, one message at a time. */
  private Rendered render(Order order, Map<String, List<String>> errors, String fields) {
    return renderWith(order, Map.of(), errors, fields);
  }

  private Rendered renderWith(Order order, Map<String, ?> extraVariables, Map<String, List<String>> errors,
      String fields) {
    BindingResult bindingResult = new BeanPropertyBindingResult(order, "order");
    errors.forEach((field, messages) -> messages.forEach(message ->
        bindingResult.rejectValue(field, "Invalid", message)));
    Map<String, Object> variables = new HashMap<>(extraVariables);
    variables.put("order", order);
    variables.put(BindingResult.MODEL_KEY_PREFIX + "order", bindingResult);
    return renderer.render("<form th:object=\"${order}\">" + fields + "</form>", variables);
  }

  /** A form backing object with JavaBeans accessors, as Spring's data binding expects. */
  public static class Order {

    private String plan;

    public Order(String plan) {
      this.plan = plan;
    }

    public String getPlan() {
      return plan;
    }

    public void setPlan(String plan) {
      this.plan = plan;
    }
  }
}
