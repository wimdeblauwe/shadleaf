package io.github.wimdeblauwe.shadleaf.table;

import static io.github.wimdeblauwe.shadleaf.test.ShadleafAssertions.assertThat;
import static io.github.wimdeblauwe.shadleaf.test.ShadleafAssertions.assertThatRenderFailure;

import io.github.wimdeblauwe.shadleaf.test.ComponentRenderTester;
import io.github.wimdeblauwe.shadleaf.test.Rendered;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.StaticMessageSource;

/**
 * {@code <sl:table-select-all>}, {@code <sl:table-selection-count>} and the row checkboxes they work on. What they do
 * in the browser is in {@code docs/tests/table-selection.spec.ts}.
 */
class TableSelectionComponentTest {

  private static final ComponentRenderTester TESTER = ComponentRenderTester.create();

  @Test
  void selectAllIsACheckboxThatSubmitsNothingAndWaitsForAlpine() {
    Rendered rendered = TESTER.render("<sl:table-select-all/>");

    assertThat(rendered).hasNoLeakedMarkup();
    assertThat(rendered).root()
        .hasTag("span")
        .hasClass("checkbox-wrapper", "table-select-all")
        .hasAttribute("x-data", "slTableSelection")
        .hasAttribute("x-cloak")
        .hasAttribute("data-format", "{0} of {1} selected")
        .hasNoAttribute("data-name");
    assertThat(rendered).element("input")
        .hasAttribute("type", "checkbox")
        .hasClass("checkbox")
        .hasAttribute("aria-label", "Select all rows on this page")
        .hasNoAttribute("name", "value", "id");
    assertThat(rendered).element(".checkbox-indicator").hasTag("svg");
    assertThat(rendered).element(".checkbox-indeterminate-indicator").hasTag("svg");
    assertThat(rendered).element("[role=status]").hasClass("sl-sr-only").hasText("");
  }

  @Test
  void theNamePropNarrowsTheRowsAndOtherAttributesGoToTheInput() {
    Rendered rendered = TESTER.render("""
        <sl:table-select-all name="ids" id="select-all" form="bulk" class="extra"
                             aria-label="Select all people on this page"/>""");

    assertThat(rendered).root().hasClass("checkbox-wrapper", "table-select-all", "extra")
        .hasAttribute("data-name", "ids")
        .hasNoAttribute("id", "form", "name");
    assertThat(rendered).element("input")
        .hasAttribute("id", "select-all")
        .hasAttribute("form", "bulk")
        .hasAttribute("aria-label", "Select all people on this page")
        .hasNoAttribute("name");
  }

  @Test
  void anExpressionLabelReplacesTheMessage() {
    Rendered rendered = TESTER.render("<sl:table-select-all th:aria-label=\"${label}\"/>",
        Map.of("label", "Select every member"));

    assertThat(rendered).element("input").hasAttribute("aria-label", "Select every member");
  }

  @Test
  void theCountNamesItsTableAndIsEmptyUntilAlpineFillsItIn() {
    Rendered rendered = TESTER.render("<sl:table-selection-count table=\"people\" id=\"people-count\"/>");

    assertThat(rendered).hasNoLeakedMarkup();
    assertThat(rendered).root()
        .hasTag("span")
        .hasClass("table-selection-count")
        .hasAttribute("x-data", "slTableSelectionCount")
        .hasAttribute("x-cloak")
        .hasAttribute("data-table", "people")
        .hasAttribute("data-format", "{0} of {1} selected")
        .hasAttribute("id", "people-count")
        .hasNoAttribute("role", "aria-live")
        .hasText("");
  }

  @Test
  void theCountNeedsItsTable() {
    assertThatRenderFailure(() -> TESTER.render("<sl:table-selection-count/>"))
        .hasMessageContaining("table");
  }

  @Test
  void aTranslatedCountKeepsItsPlaceholders() {
    StaticMessageSource messages = new StaticMessageSource();
    messages.addMessage("sl.table.selection.count", Locale.ENGLISH, "{0} van {1} geselecteerd");
    Rendered rendered = ComponentRenderTester.builder().messageSource(messages).locale(Locale.ENGLISH).build()
        .render("<sl:table-selection-count table=\"people\"/>");

    assertThat(rendered).root().hasAttribute("data-format", "{0} van {1} geselecteerd");
  }

  @Test
  void rowCheckboxesAreTheApplicationsOwnNamedByTheirRow() {
    Rendered rendered = TESTER.render("""
        <sl:table id="people">
          <thead><tr><th><sl:table-select-all/></th><th>Name</th></tr></thead>
          <tbody>
            <tr th:each="person : ${people}">
              <td><sl:checkbox name="ids" th:value="${person.id}"
                               th:aria-label="#{sl.table.select-row(${person.name})}"/></td>
              <td th:text="${person.name}">Name</td>
            </tr>
          </tbody>
        </sl:table>""", Map.of("people", List.of(Map.of("id", 1, "name", "Ada Lovelace"),
        Map.of("id", 2, "name", "Grace Hopper"))));

    assertThat(rendered).hasNoLeakedMarkup();
    assertThat(rendered).elements("tbody input.checkbox").extracting(input -> input.attr("aria-label"))
        .containsExactly("Select Ada Lovelace", "Select Grace Hopper");
    assertThat(rendered).elements("tbody input.checkbox").extracting(input -> input.attr("value"))
        .containsExactly("1", "2");
    assertThat(rendered).element("thead th > .table-select-all > input").hasNoAttribute("name");
  }
}
