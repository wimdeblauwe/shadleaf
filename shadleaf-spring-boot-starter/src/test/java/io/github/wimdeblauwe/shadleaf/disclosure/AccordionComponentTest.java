package io.github.wimdeblauwe.shadleaf.disclosure;

import static io.github.wimdeblauwe.shadleaf.test.ShadleafAssertions.assertThat;

import io.github.wimdeblauwe.shadleaf.test.ComponentRenderTester;
import io.github.wimdeblauwe.shadleaf.test.Rendered;
import java.util.List;
import java.util.Map;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.Test;

class AccordionComponentTest {

  private final ComponentRenderTester tester = ComponentRenderTester.create();

  private static final String TWO_ITEMS = """
      <sl:accordion-item><sl:accordion-trigger>One</sl:accordion-trigger>
        <sl:accordion-content>First</sl:accordion-content></sl:accordion-item>
      <sl:accordion-item><sl:accordion-trigger>Two</sl:accordion-trigger>
        <sl:accordion-content>Second</sl:accordion-content></sl:accordion-item>""";

  @Test
  void itemsAreDetailsWithTheSummaryFirstAndNoScript() {
    Rendered rendered = tester.render("<sl:accordion name=\"faq\">" + TWO_ITEMS + "</sl:accordion>");

    assertThat(rendered).hasNoLeakedMarkup();
    assertThat(rendered).root().hasTag("div").hasClass("accordion").hasNoAttribute("x-data", "name");
    List<Element> items = rendered.select("details.accordion-item");
    assertThat(items).hasSize(2);
    for (Element item : items) {
      assertThat(item).hasAttribute("name", "faq").hasNoAttribute("open", "data-disabled");
      assertThat(item.child(0).tagName()).isEqualTo("summary");
    }
  }

  @Test
  void theTriggersTextIsAHeadingInsideTheSummary() {
    Rendered rendered = tester.render("<sl:accordion-trigger>Question</sl:accordion-trigger>");

    assertThat(rendered).root().hasTag("summary").hasClass("accordion-trigger").hasNoAttribute("role", "tabindex");
    assertThat(rendered).element("summary > span.accordion-trigger-text")
        .hasAttribute("role", "heading")
        .hasAttribute("aria-level", "3")
        .hasText("Question");
    assertThat(rendered).element("summary > svg.accordion-trigger-icon").hasAttribute("aria-hidden", "true");
    assertThat(tester.render("<sl:accordion-trigger level=\"2\">Q</sl:accordion-trigger>"))
        .element(".accordion-trigger-text").hasAttribute("aria-level", "2");
  }

  @Test
  void withoutANameASingleAccordionGeneratesOneEachRender() {
    Rendered first = tester.render("<sl:accordion>" + TWO_ITEMS + "</sl:accordion>");
    Rendered second = tester.render("<sl:accordion>" + TWO_ITEMS + "</sl:accordion>");

    String name = first.select("details").first().attr("name");
    assertThat(name).matches("sl-accordion-[a-z0-9]{8}");
    assertThat(first.select("details").last()).hasAttribute("name", name);
    assertThat(second.select("details").first().attr("name")).isNotEqualTo(name);
  }

  @Test
  void aMultipleAccordionHasNoName() {
    Rendered rendered = tester.render("<sl:accordion type=\"multiple\" name=\"ignored\">" + TWO_ITEMS
        + "</sl:accordion>");

    assertThat(rendered).elements("details[name]").isEmpty();
  }

  @Test
  void aNestedMultipleAccordionLeavesTheOuterGroup() {
    Rendered rendered = tester.render("""
        <sl:accordion name="outer">
          <sl:accordion-item id="a"><sl:accordion-trigger>A</sl:accordion-trigger><sl:accordion-content>
            <sl:accordion type="multiple"><sl:accordion-item id="inner"><sl:accordion-trigger>I</sl:accordion-trigger>
            </sl:accordion-item></sl:accordion>
          </sl:accordion-content></sl:accordion-item>
          <sl:accordion-item id="b"><sl:accordion-trigger>B</sl:accordion-trigger></sl:accordion-item>
        </sl:accordion>""");

    assertThat(rendered).element("#a").hasAttribute("name", "outer");
    assertThat(rendered).element("#inner").hasNoAttribute("name");
    assertThat(rendered).element("#b").hasAttribute("name", "outer");
  }

  @Test
  void openFromTheServer() {
    Rendered rendered = tester.render("""
        <sl:accordion name="faq">
          <sl:accordion-item th:each="question : ${questions}" th:open="${question == open}">
            <sl:accordion-trigger>[[${question}]]</sl:accordion-trigger>
          </sl:accordion-item>
        </sl:accordion>""", Map.of("questions", List.of("a", "b", "c"), "open", "b"));

    assertThat(rendered.select("details[open]")).hasSize(1);
    assertThat(rendered.select("details[open] .accordion-trigger-text").text()).isEqualTo("b");
  }

  @Test
  void aDisabledItemCannotBeReachedOrOpenedByFindInPage() {
    Rendered rendered = tester.render("""
        <sl:accordion-item disabled><sl:accordion-trigger>Premium</sl:accordion-trigger>
          <sl:accordion-content>Hidden</sl:accordion-content></sl:accordion-item>""");

    assertThat(rendered).root().hasAttribute("data-disabled", "true").hasNoAttribute("open");
    assertThat(rendered).element("summary")
        .hasAttribute("aria-disabled", "true")
        .hasAttribute("tabindex", "-1");
    assertThat(rendered).hasNoElement(".accordion-content");
  }

  @Test
  void aDisabledOpenItemKeepsItsContent() {
    Rendered rendered = tester.render("""
        <sl:accordion-item disabled open><sl:accordion-trigger>Plan</sl:accordion-trigger>
          <sl:accordion-content>Starter</sl:accordion-content></sl:accordion-item>""");

    assertThat(rendered).root().hasAttribute("open");
    assertThat(rendered).element(".accordion-content").hasText("Starter");
  }

  @Test
  void aDisabledItemsTriggerOnlyAffectsItsOwnItem() {
    Rendered rendered = tester.render("""
        <sl:accordion-item disabled open><sl:accordion-trigger>Outer</sl:accordion-trigger>
          <sl:accordion-content><sl:accordion-item id="inner"><sl:accordion-trigger>Inner</sl:accordion-trigger>
            <sl:accordion-content>Text</sl:accordion-content></sl:accordion-item></sl:accordion-content>
        </sl:accordion-item>""");

    assertThat(rendered).element("#inner > summary").hasNoAttribute("aria-disabled", "tabindex");
    assertThat(rendered).element("#inner > .accordion-content").hasText("Text");
  }
}
