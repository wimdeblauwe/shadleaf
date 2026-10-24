package io.github.wimdeblauwe.shadleaf.toggle;

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
 * {@code <sl:toggle>} and {@code <sl:toggle-group>} inside a {@code th:object} form: a toggle binds as a checkbox, a
 * group's items as radio buttons ({@code single}) or checkboxes ({@code multiple}) that share the group's
 * {@code th:field}, or that of a {@code sl:field-set} around it, and a field with errors marks them
 * {@code aria-invalid}.
 */
class ToggleBindingTest {

  private final ComponentRenderTester renderer = ComponentRenderTester.create();

  @Test
  void toggleIsACheckboxCheckedFromTheFieldWithTheMarkerInsideTheLabel() {
    Rendered rendered = render(new Settings(true, "grid", List.of()), false, """
        <sl:toggle th:field="*{starred}" class="ms-2">Starred</sl:toggle>""");

    Element label = rendered.select("label.toggle").first();
    Element input = label.selectFirst("input.toggle-input");
    assertThat(label.classNames()).containsExactly("toggle", "ms-2");
    assertThat(input.attr("type")).isEqualTo("checkbox");
    assertThat(input.id()).isEqualTo("starred1");
    assertThat(input.attr("name")).isEqualTo("starred");
    assertThat(input.hasAttr("checked")).isTrue();
    assertThat(input.hasAttr("aria-invalid")).isFalse();
    assertThat(label.select("input[type=hidden][name=_starred]")).hasSize(1);
    assertThat(label.text()).isEqualTo("Starred");
  }

  @Test
  void toggleWithAFieldErrorIsInvalid() {
    Element input = render(new Settings(false, "grid", List.of()), true, """
        <sl:toggle th:field="*{starred}">Starred</sl:toggle>""").select("input.toggle-input").first();

    assertThat(input.hasAttr("checked")).isFalse();
    assertThat(input.attr("aria-invalid")).isEqualTo("true");
  }

  @Test
  void toggleInAFieldTakesItsFieldIdAndDescription() {
    Rendered rendered = render(new Settings(false, "grid", List.of()), false, """
        <sl:field th:field="*{starred}">
          <sl:toggle>Starred</sl:toggle>
          <sl:field-description>Starred posts show first.</sl:field-description>
        </sl:field>""");

    Element input = rendered.select("input.toggle-input").first();
    assertThat(input.id()).isEqualTo("starred");
    assertThat(input.attr("name")).isEqualTo("starred");
    assertThat(input.attr("aria-describedby")).isEqualTo("starred-description");
  }

  @Test
  void singleGroupItemsAreRadioButtonsSharingTheGroupsField() {
    Rendered rendered = render(new Settings(false, "grid", List.of()), false, """
        <sl:toggle-group th:field="*{layout}" variant="outline" size="sm" aria-label="Layout">
          <sl:toggle-group-item value="list">List</sl:toggle-group-item>
          <sl:toggle-group-item value="grid">Grid</sl:toggle-group-item>
        </sl:toggle-group>""");

    Element group = rendered.select(".toggle-group").first();
    assertThat(group.attr("role")).isEqualTo("radiogroup");
    assertThat(group.hasAttr("th:field")).isFalse();
    assertThat(group.hasAttr("name")).isFalse();
    assertThat(group.select("> label.toggle-group-item"))
        .extracting(item -> item.attr("data-variant"), item -> item.attr("data-size"))
        .containsExactly(tuple("outline", "sm"), tuple("outline", "sm"));
    assertThat(group.select("input.toggle-input"))
        .extracting(input -> input.attr("type"), Element::id, input -> input.attr("name"),
            input -> input.hasAttr("checked"))
        .containsExactly(tuple("radio", "layout1", "layout", false), tuple("radio", "layout2", "layout", true));
  }

  @Test
  void multipleGroupItemsAreCheckboxesBoundToACollection() {
    Rendered rendered = render(new Settings(false, "grid", List.of("MONDAY", "WEDNESDAY")), false, """
        <sl:toggle-group type="multiple" th:field="*{workdays}" aria-label="Workdays">
          <sl:toggle-group-item value="MONDAY">Mon</sl:toggle-group-item>
          <sl:toggle-group-item value="TUESDAY">Tue</sl:toggle-group-item>
          <sl:toggle-group-item value="WEDNESDAY">Wed</sl:toggle-group-item>
        </sl:toggle-group>""");

    assertThat(rendered.select(".toggle-group").first().attr("role")).isEqualTo("group");
    assertThat(rendered.select("input.toggle-input"))
        .extracting(input -> input.attr("type"), input -> input.attr("name"), input -> input.hasAttr("checked"))
        .containsExactly(tuple("checkbox", "workdays", true), tuple("checkbox", "workdays", false),
            tuple("checkbox", "workdays", true));
    assertThat(rendered.select("input[type=hidden][name=_workdays]")).isNotEmpty();
  }

  @Test
  void groupInAFieldSetTakesItsField() {
    Rendered rendered = render(new Settings(false, "list", List.of()), false, """
        <sl:field-set th:field="*{layout}">
          <sl:field-legend>Layout</sl:field-legend>
          <sl:toggle-group aria-label="Layout">
            <sl:toggle-group-item value="list">List</sl:toggle-group-item>
            <sl:toggle-group-item value="grid">Grid</sl:toggle-group-item>
          </sl:toggle-group>
        </sl:field-set>""");

    assertThat(rendered.select("input.toggle-input"))
        .extracting(input -> input.attr("name"), input -> input.hasAttr("checked"))
        .containsExactly(tuple("layout", true), tuple("layout", false));
  }

  @Test
  void groupItemsWithAFieldErrorAreInvalid() {
    Rendered rendered = render(new Settings(false, "gold", List.of()), true, """
        <sl:toggle-group th:field="*{layout}" aria-label="Layout">
          <sl:toggle-group-item value="list">List</sl:toggle-group-item>
          <sl:toggle-group-item value="grid">Grid</sl:toggle-group-item>
        </sl:toggle-group>""");

    assertThat(rendered.select("input.toggle-input"))
        .hasSize(2)
        .allSatisfy(item -> assertThat(item.attr("aria-invalid")).isEqualTo("true"));
  }

  @Test
  void groupGivesItsNameAndDisabledToItsItems() {
    Rendered rendered = render(new Settings(false, "grid", List.of()), false, """
        <sl:toggle-group name="view" disabled aria-label="View">
          <sl:toggle-group-item value="list">List</sl:toggle-group-item>
          <sl:toggle-group-item value="grid" name="other">Grid</sl:toggle-group-item>
        </sl:toggle-group>""");

    assertThat(rendered.select("input.toggle-input"))
        .extracting(input -> input.attr("name"), input -> input.hasAttr("disabled"))
        .containsExactly(tuple("view", true), tuple("other", true));
  }

  @Test
  void aDisabledGroupsItemsNeverTakeTheFormSummarysAutofocus() {
    Rendered rendered = render(new Settings(false, "gold", List.of()), true, """
        <sl:form-errors autofocus/>
        <sl:toggle-group th:field="*{layout}" disabled aria-label="Layout">
          <sl:toggle-group-item value="list">List</sl:toggle-group-item>
          <sl:toggle-group-item value="grid">Grid</sl:toggle-group-item>
        </sl:toggle-group>
        <sl:toggle th:field="*{starred}">Starred</sl:toggle>""");

    assertThat(rendered.select("[autofocus]")).extracting(Element::id).containsExactly("starred1");
  }

  @Test
  void singleGroupWithoutANameOrFieldGetsAGeneratedNameForItsItems() {
    Rendered rendered = render(new Settings(false, "grid", List.of()), false, """
        <sl:toggle-group aria-label="Align">
          <sl:toggle-group-item value="left">Left</sl:toggle-group-item>
          <sl:toggle-group-item value="right">Right</sl:toggle-group-item>
        </sl:toggle-group>
        <sl:toggle-group aria-label="Align again">
          <sl:toggle-group-item value="left">Left</sl:toggle-group-item>
        </sl:toggle-group>
        <sl:toggle-group type="multiple" aria-label="Style">
          <sl:toggle-group-item value="bold">Bold</sl:toggle-group-item>
        </sl:toggle-group>""");

    assertThat(rendered.select("input.toggle-input")).extracting(input -> input.attr("name"))
        .containsExactly("sl-toggle-group-1", "sl-toggle-group-1", "sl-toggle-group-2", "");
  }

  /** With {@code invalid}, every field of the settings has an error. */
  private Rendered render(Settings settings, boolean invalid, String fields) {
    BindingResult bindingResult = new BeanPropertyBindingResult(settings, "settings");
    if (invalid) {
      bindingResult.rejectValue("starred", "AssertTrue", "must be starred");
      bindingResult.rejectValue("layout", "Layout", "is not a layout");
      bindingResult.rejectValue("workdays", "NotEmpty", "must not be empty");
    }
    Map<String, Object> variables = new HashMap<>();
    variables.put("settings", settings);
    variables.put(BindingResult.MODEL_KEY_PREFIX + "settings", bindingResult);
    return renderer.render("<form th:object=\"${settings}\">" + fields + "</form>", variables);
  }

  /** A form backing object with JavaBeans accessors, as Spring's data binding expects. */
  public static class Settings {

    private boolean starred;
    private String layout;
    private List<String> workdays;

    Settings(boolean starred, String layout, List<String> workdays) {
      this.starred = starred;
      this.layout = layout;
      this.workdays = workdays;
    }

    public boolean isStarred() {
      return starred;
    }

    public void setStarred(boolean starred) {
      this.starred = starred;
    }

    public String getLayout() {
      return layout;
    }

    public void setLayout(String layout) {
      this.layout = layout;
    }

    public List<String> getWorkdays() {
      return workdays;
    }

    public void setWorkdays(List<String> workdays) {
      this.workdays = workdays;
    }
  }
}
