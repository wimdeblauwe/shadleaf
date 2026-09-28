package io.github.wimdeblauwe.shadleaf.dialect;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.wimdeblauwe.shadleaf.test.ComponentRenderer;
import java.util.List;
import java.util.Map;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.Test;

class SlotsTest {

  private final ComponentRenderer renderer = new ComponentRenderer();

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

  private Element render(String snippet) {
    return render(snippet, Map.of());
  }

  private Element render(String snippet, Map<String, ?> variables) {
    return Jsoup.parseBodyFragment(renderer.render(snippet, variables)).body().child(0);
  }
}
