package io.github.wimdeblauwe.shadleaf.dialect;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.wimdeblauwe.shadleaf.test.ComponentRenderTester;
import java.util.List;
import java.util.Map;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.Test;

class SlotsTest {

  private final ComponentRenderTester renderer = ComponentRenderTester.create();

  @Test
  void defaultSlotReplacesTheFallback() {
    Element card = render("<sl:test-card><p>Body</p></sl:test-card>");

    assertThat(card.selectFirst(".card-body").html()).isEqualTo("<p>Body</p>");
  }

  @Test
  void emptyComponentShowsTheFallback() {
    Element card = render("<sl:test-card></sl:test-card>");

    assertThat(card.selectFirst(".card-body").text()).isEqualTo("Empty card");
  }

  @Test
  void whitespaceOnlyContentCountsAsEmpty() {
    Element card = render("""
        <sl:test-card>
          <!-- nothing yet -->
        </sl:test-card>""");

    assertThat(card.selectFirst(".card-body").text()).isEqualTo("Empty card");
  }

  @Test
  void namedSlotsAreRenderedOnlyWhenGiven() {
    Element card = render("""
        <sl:test-card>
          <sl:slot name="header"><h2>Title</h2></sl:slot>
          <p>Body</p>
        </sl:test-card>""");

    assertThat(card.selectFirst(".card-header").html()).isEqualTo("<h2>Title</h2>");
    assertThat(card.selectFirst(".card-body").html()).isEqualTo("<p>Body</p>");
    assertThat(card.selectFirst(".card-footer")).as("slots.has('footer') is false").isNull();
  }

  @Test
  void namedSlotWithOnlyWhitespaceIsNotPresent() {
    Element card = render("""
        <sl:test-card>
          <sl:slot name="header">   </sl:slot>
          <sl:slot name="footer"/>
          Body
        </sl:test-card>""");

    assertThat(card.selectFirst(".card-header")).isNull();
    assertThat(card.selectFirst(".card-footer")).isNull();
  }

  @Test
  void onlyNamedSlotsLeaveTheDefaultFallbackInPlace() {
    Element card = render("""
        <sl:test-card>
          <sl:slot name="footer">Footer</sl:slot>
        </sl:test-card>""");

    assertThat(card.selectFirst(".card-body").text()).isEqualTo("Empty card");
    assertThat(card.selectFirst(".card-footer").text()).isEqualTo("Footer");
  }

  @Test
  void slotContentIsEvaluatedWithTheCallersVariables() {
    Element card = render("""
            <sl:test-card>
              <sl:slot name="header"><span th:text="${title}">x</span></sl:slot>
              <span th:each="item : ${items}" th:text="${item}"></span>
            </sl:test-card>""",
        Map.of("title", "Orders", "items", List.of("a", "b")));

    assertThat(card.selectFirst(".card-header").text()).isEqualTo("Orders");
    assertThat(card.select(".card-body > span")).extracting(Element::text).containsExactly("a", "b");
  }

  @Test
  void nestedComponentInANamedSlot() {
    Element card = render("""
        <sl:test-card>
          <sl:slot name="header"><sl:test-chip variant="success">New</sl:test-chip></sl:slot>
          Body
        </sl:test-card>""");

    Element chip = card.selectFirst(".card-header > span.chip");
    assertThat(chip.attr("data-variant")).isEqualTo("success");
    assertThat(chip.text()).isEqualTo("New");
    assertThat(card.selectFirst(".card-body").text()).isEqualTo("Body");
  }

  @Test
  void namedSlotInsideANestedComponentBelongsToThatComponent() {
    Element outer = render("""
        <sl:test-card class="outer">
          <sl:slot name="header">Outer header</sl:slot>
          <sl:test-card class="inner">
            <sl:slot name="header">Inner header</sl:slot>
            <sl:slot name="footer">Inner footer</sl:slot>
            Inner body
          </sl:test-card>
        </sl:test-card>""");

    assertThat(outer.selectFirst("> .card-header").text()).isEqualTo("Outer header");
    assertThat(outer.selectFirst("> .card-footer")).as("the footer was given to the inner card").isNull();
    Element inner = outer.selectFirst("> .card-body > .card.inner");
    assertThat(inner.selectFirst("> .card-header").text()).isEqualTo("Inner header");
    assertThat(inner.selectFirst("> .card-footer").text()).isEqualTo("Inner footer");
    assertThat(inner.selectFirst("> .card-body").text()).isEqualTo("Inner body");
  }

  @Test
  void selfClosingComponentWithoutSlot() {
    Element dot = render("<sl:test-dot tone=\"warn\"/>");

    assertThat(dot.outerHtml()).isEqualTo("<i class=\"dot\" data-tone=\"warn\"></i>");
  }

  @Test
  void selfClosingComponentInsideAnotherComponentsSlot() {
    Element card = render("""
        <sl:test-card>
          <sl:slot name="header"><sl:test-dot/> Status</sl:slot>
        </sl:test-card>""");

    assertThat(card.selectFirst(".card-header").html()).isEqualTo("<i class=\"dot\" data-tone=\"info\"></i> Status");
  }

  @Test
  void contentGivenToAComponentWithoutSlotIsDropped() {
    Element dot = render("<sl:test-dot>ignored</sl:test-dot>");

    assertThat(dot.outerHtml()).isEqualTo("<i class=\"dot\" data-tone=\"info\"></i>");
  }

  // --- slot scope -------------------------------------------------------------------------------

  @Test
  void slotContentInALibraryTemplateSeesThatTemplatesProps() {
    Element panel = render("<sl:test-panel title=\"Orders\">Body</sl:test-panel>");

    assertThat(panel.selectFirst("span.chip .title").text()).isEqualTo("Orders");
  }

  @Test
  void slotContentInALibraryTemplateSeesThatTemplatesAttrs() {
    Element panel = render("<sl:test-panel data-id=\"42\">Body</sl:test-panel>");

    assertThat(panel.selectFirst(".card-header .id").text()).isEqualTo("42");
  }

  @Test
  void slotContentInALibraryTemplateSeesThatTemplatesSlots() {
    Element panel = render("""
        <sl:test-panel>
          <sl:slot name="actions"><button>Save</button></sl:slot>
          Body
        </sl:test-panel>""");

    assertThat(panel.selectFirst(".card-footer .has-actions").text()).isEqualTo("true");
  }

  @Test
  void aSlotPassedIntoAnotherComponentsSlotIsTheLibraryTemplatesOwn() {
    Element panel = render("<sl:test-panel><p>Panel body</p></sl:test-panel>");

    assertThat(panel.selectFirst(".card-body").html()).isEqualTo("<p>Panel body</p>");
  }

  @Test
  void aNamedSlotPassedIntoAnotherComponentsSlotIsTheLibraryTemplatesOwn() {
    Element panel = render("""
        <sl:test-panel>
          <sl:slot name="actions"><button>Save</button></sl:slot>
        </sl:test-panel>""");

    assertThat(panel.selectFirst(".card-header button").text()).isEqualTo("Save");
  }

  @Test
  void theFallbackOfAPassedOnSlotIsEvaluatedInTheLibraryTemplatesScope() {
    Element panel = render("<sl:test-panel title=\"Orders\"></sl:test-panel>");

    assertThat(panel.selectFirst(".card-body").html()).isEqualTo("<em>Orders</em>");
  }

  @Test
  void slotContentInAnApplicationTemplateSeesTheApplicationsOwnPropsVariable() {
    Element chip = render("""
            <sl:test-chip variant="danger"><span th:text="${props}">x</span></sl:test-chip>""",
        Map.of("props", "the application's"));

    assertThat(chip.selectFirst("span").text()).isEqualTo("the application's");
  }

  @Test
  void containsFindsAComponentAtAnyDepthInAnySlot() {
    Element found = render("""
        <sl:test-slot-probe>
          <p><span><sl:test-dot/></span></p>
          <sl:slot name="aside"><sl:test-chip>Chip</sl:test-chip></sl:slot>
        </sl:test-slot-probe>""");

    assertThat(found.attr("data-dot")).isEqualTo("true");
    assertThat(found.attr("data-chip")).as("names are case-insensitive, like elements").isEqualTo("true");
  }

  @Test
  void containsSkipsTheContentOfTheComponentsItIsToldToSkip() {
    Element outer = render("""
        <sl:test-slot-probe>
          <sl:test-slot-probe><p><sl:test-dot/></p></sl:test-slot-probe>
          <p>After</p>
        </sl:test-slot-probe>""");

    assertThat(outer.attr("data-dot")).isEqualTo("false");
    assertThat(outer.select(".slot-probe").get(1).attr("data-dot")).as("the nested one finds its own")
        .isEqualTo("true");
    assertThat(outer.attr("data-chip")).isEqualTo("false");
  }

  private Element render(String snippet) {
    return render(snippet, Map.of());
  }

  private Element render(String snippet, Map<String, ?> variables) {
    return renderer.render(snippet, variables).root();
  }
}
