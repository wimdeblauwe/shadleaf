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
 * {@code <sl:combobox>} inside a {@code th:object} form: {@code th:field} on the combobox (or on its field) marks the
 * bound item, or the bound items of a collection with {@code multiple}, among the options its parts render; a field
 * with errors marks the select {@code aria-invalid}; a field wires the id, label and description as for
 * {@code sl:select}. The template slCombobox fills in holds the input, the list box and the texts it needs.
 */
class ComboboxBindingTest {

  private static final String ITEMS = """
      <sl:combobox-group label="React">
        <sl:combobox-item value="next">Next.js</sl:combobox-item>
        <sl:combobox-item value="remix">Remix</sl:combobox-item>
      </sl:combobox-group>
      <sl:combobox-separator/>
      <sl:combobox-item value="astro" disabled>Astro</sl:combobox-item>""";

  private final ComponentRenderTester renderer = ComponentRenderTester.create();

  @Test
  void thFieldMarksTheBoundItemAndKeepsClassOnTheWrapper() {
    Rendered rendered = render(new Project("remix", List.of()), Map.of(), """
        <sl:combobox th:field="*{framework}" class="w-full" aria-label="Framework">%s</sl:combobox>"""
        .formatted(ITEMS));

    Element wrapper = rendered.select(".combobox").first();
    Element select = wrapper.selectFirst("select");
    assertThat(wrapper.classNames()).containsExactly("combobox", "w-full");
    assertThat(wrapper.attr("x-data")).isEqualTo("slCombobox");
    assertThat(wrapper.hasAttr("data-multiple")).isFalse();
    assertThat(select.classNames()).containsExactly("combobox-select");
    assertThat(select.id()).isEqualTo("framework");
    assertThat(select.attr("name")).isEqualTo("framework");
    assertThat(select.hasAttr("multiple")).isFalse();
    assertThat(select.select("option[selected]")).extracting(Element::val).containsExactly("remix");
    assertThat(select.select("hr.combobox-separator")).hasSize(1);
    assertThat(select.hasAttr("aria-invalid")).isFalse();
    assertThat(wrapper.select("> .combobox-icon")).hasSize(1);
  }

  @Test
  void aSingleComboboxHasAHiddenEmptyFirstOptionWithThePlaceholder() {
    Element select = render(new Project(null, List.of()), Map.of(), """
        <sl:combobox th:field="*{framework}" placeholder="Select a framework" aria-label="Framework">%s</sl:combobox>"""
        .formatted(ITEMS)).select("select").first();

    Element first = select.select("option").first();
    assertThat(first.val()).isEmpty();
    assertThat(first.hasAttr("hidden")).isTrue();
    assertThat(first.attr("data-placeholder")).isEqualTo("true");
    assertThat(first.text()).isEqualTo("Select a framework");
    assertThat(select.select("option[selected]").stream().map(Element::val))
        .as("none of the real options, so the browser chooses the empty one")
        .allMatch(String::isEmpty);
  }

  @Test
  void multipleBindsACollection() {
    Rendered rendered = render(new Project(null, List.of("next", "astro")), Map.of(), """
        <sl:combobox th:field="*{tags}" multiple placeholder="Add a framework" aria-label="Frameworks">%s</sl:combobox>"""
        .formatted(ITEMS));

    Element wrapper = rendered.select(".combobox").first();
    Element select = wrapper.selectFirst("select");
    assertThat(wrapper.attr("data-multiple")).isEqualTo("true");
    assertThat(select.hasAttr("multiple")).isTrue();
    assertThat(select.attr("name")).isEqualTo("tags");
    assertThat(select.select("option[data-placeholder]")).as("a multiple select can choose none").isEmpty();
    assertThat(select.select("option[selected]")).extracting(Element::val).containsExactly("next", "astro");
    assertThat(wrapper.select("input[type=hidden][name=_tags]"))
        .as("Spring's marker, so that choosing none binds an empty collection")
        .hasSize(1);
    assertThat(wrapper.select("> .combobox-icon")).as("a list box has no chevron").isEmpty();
  }

  @Test
  void theTemplateHoldsTheControlTheListBoxAndTheTexts() {
    Rendered rendered = render(new Project(null, List.of()), Map.of(), """
        <sl:combobox th:field="*{tags}" multiple show-clear placeholder="Add a framework" aria-label="Frameworks">%s</sl:combobox>"""
        .formatted(ITEMS));

    Element wrapper = rendered.select(".combobox").first();
    Element template = wrapper.selectFirst("> template.combobox-template");
    Element input = template.selectFirst(".combobox-control > input.combobox-input");
    assertThat(input.attr("role")).isEqualTo("combobox");
    assertThat(input.attr("aria-autocomplete")).isEqualTo("list");
    assertThat(input.attr("aria-expanded")).isEqualTo("false");
    assertThat(input.attr("autocomplete")).isEqualTo("off");
    assertThat(input.attr("placeholder")).isEqualTo("Add a framework");
    assertThat(input.hasAttr("name")).as("only the select submits").isFalse();
    assertThat(template.select(".combobox-clear")).extracting(button -> button.attr("aria-label"), button -> button.attr("tabindex"))
        .containsExactly(tuple("Clear", "-1"));
    assertThat(template.select(".combobox-trigger")).as("a multiple combobox has no chevron").isEmpty();
    assertThat(template.select(".combobox-content[popover=manual] > .combobox-list[role=listbox]")).hasSize(1);
    assertThat(template.select(".combobox-empty").text()).isEqualTo("No items found.");
    assertThat(template.select(".combobox-status[role=status]")).hasSize(1);
    assertThat(template.select("> .combobox-chip > .combobox-chip-remove[tabindex=-1]")).hasSize(1);
    assertThat(template.select("> .combobox-item-indicator")).hasSize(1);
    assertThat(wrapper.attr("data-remove-label")).isEqualTo("Remove {0}");
    assertThat(wrapper.attr("data-result")).isEqualTo("1 result");
    assertThat(wrapper.attr("data-results")).isEqualTo("{0} results");
    assertThat(wrapper.hasAttr("data-search-url")).isFalse();
  }

  @Test
  void aSingleComboboxHasTheChevronAndNoChip() {
    Element template = render(new Project(null, List.of()), Map.of(), """
        <sl:combobox th:field="*{framework}" aria-label="Framework">%s</sl:combobox>""".formatted(ITEMS))
        .select("template.combobox-template").first();

    assertThat(template.select(".combobox-trigger")).extracting(button -> button.attr("aria-label"),
        button -> button.attr("aria-expanded")).containsExactly(tuple("Show options", "false"));
    assertThat(template.select(".combobox-clear")).isEmpty();
    assertThat(template.select(".combobox-chip")).isEmpty();
  }

  @Test
  void searchUrlAndTheEmptyTextAreDataForTheScript() {
    Element wrapper = render(new Project(null, List.of()), Map.of(), """
        <sl:combobox th:field="*{framework}" aria-label="Framework" th:search-url="@{/frameworks}"
                     empty="No frameworks found." auto-highlight>%s</sl:combobox>""".formatted(ITEMS))
        .select(".combobox").first();

    assertThat(wrapper.attr("data-search-url")).isEqualTo("/frameworks");
    assertThat(wrapper.attr("data-searching")).isEqualTo("Searching…");
    assertThat(wrapper.attr("data-search-error")).isEqualTo("The options could not be loaded.");
    assertThat(wrapper.attr("data-auto-highlight")).isEqualTo("true");
    assertThat(wrapper.select(".combobox-empty").text()).isEqualTo("No frameworks found.");
    assertThat(wrapper.selectFirst("select").hasAttr("search-url")).isFalse();
  }

  @Test
  void aFieldWithErrorsMarksTheSelectInvalid() {
    Element select = render(new Project("vue", List.of()), Map.of("framework", List.of("is not supported")), """
        <sl:combobox th:field="*{framework}" aria-label="Framework">%s</sl:combobox>""".formatted(ITEMS))
        .select("select").first();

    assertThat(select.attr("aria-invalid")).isEqualTo("true");
  }

  @Test
  void inAFieldItTakesTheFieldsIdLabelAndDescription() {
    Rendered rendered = render(new Project(null, List.of("remix")), Map.of("tags", List.of("needs one more")), """
        <sl:field th:field="*{tags}">
          <sl:field-label>Frameworks</sl:field-label>
          <sl:combobox multiple>%s</sl:combobox>
          <sl:field-description>Pick two.</sl:field-description>
          <sl:field-error/>
        </sl:field>""".formatted(ITEMS));

    Element select = rendered.select("select").first();
    assertThat(select.id()).isEqualTo("tags");
    assertThat(select.attr("name")).isEqualTo("tags");
    assertThat(select.attr("aria-invalid")).isEqualTo("true");
    assertThat(select.attr("aria-describedby")).isEqualTo("tags-description tags-error");
    assertThat(select.select("option[selected]")).extracting(Element::val).containsExactly("remix");
    assertThat(rendered.select("label.field-label").first().attr("for")).isEqualTo("tags");
  }

  @Test
  void theFormSummaryGivesAutofocusToAnInvalidCombobox() {
    Rendered rendered = render(new Project("vue", List.of()), Map.of("framework", List.of("is not supported")), """
        <sl:form-errors autofocus/>
        <sl:field th:field="*{framework}">
          <sl:field-label>Framework</sl:field-label>
          <sl:combobox>%s</sl:combobox>
          <sl:field-error/>
        </sl:field>""".formatted(ITEMS));

    assertThat(rendered.select("[autofocus]")).extracting(Element::tagName, Element::id)
        .containsExactly(tuple("select", "framework"));
  }

  @Test
  void disabledPassesThroughToTheSelect() {
    Element select = render(new Project("next", List.of()), Map.of(), """
        <sl:combobox th:field="*{framework}" aria-label="Framework" disabled>%s</sl:combobox>""".formatted(ITEMS))
        .select("select").first();

    assertThat(select.hasAttr("disabled")).isTrue();
    assertThat(select.select("option[value=astro]").first().hasAttr("disabled")).isTrue();
  }

  /** Every entry in {@code errors} is rejected on the form object's binding, one message at a time. */
  private Rendered render(Project project, Map<String, List<String>> errors, String fields) {
    BindingResult bindingResult = new BeanPropertyBindingResult(project, "project");
    errors.forEach((field, messages) -> messages.forEach(message ->
        bindingResult.rejectValue(field, "Invalid", message)));
    Map<String, Object> variables = new HashMap<>();
    variables.put("project", project);
    variables.put(BindingResult.MODEL_KEY_PREFIX + "project", bindingResult);
    return renderer.render("<form th:object=\"${project}\">" + fields + "</form>", variables);
  }

  /** A form backing object with JavaBeans accessors, as Spring's data binding expects. */
  public static class Project {

    private String framework;
    private List<String> tags;

    public Project(String framework, List<String> tags) {
      this.framework = framework;
      this.tags = tags;
    }

    public String getFramework() {
      return framework;
    }

    public void setFramework(String framework) {
      this.framework = framework;
    }

    public List<String> getTags() {
      return tags;
    }

    public void setTags(List<String> tags) {
      this.tags = tags;
    }
  }
}
