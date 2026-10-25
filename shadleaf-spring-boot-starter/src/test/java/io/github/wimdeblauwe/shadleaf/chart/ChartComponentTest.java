package io.github.wimdeblauwe.shadleaf.chart;

import static io.github.wimdeblauwe.shadleaf.test.ShadleafAssertions.assertThat;
import static io.github.wimdeblauwe.shadleaf.test.ShadleafAssertions.assertThatRenderFailure;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.wimdeblauwe.shadleaf.test.ComponentRenderTester;
import io.github.wimdeblauwe.shadleaf.test.Rendered;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * {@code <sl:chart>} and its parts as the server renders them: an {@code <sl-chart>} with the rows as a JSON data
 * block, a marker per part with plain attributes for {@code sl-chart.jsx}, and a skeleton. What the browser draws is in
 * {@code docs/tests/chart.spec.ts}.
 */
class ChartComponentTest {

  private static final List<Map<String, Object>> ROWS = List.of(
      Map.of("month", "January", "desktop", 186, "mobile", 80),
      Map.of("month", "February", "desktop", 305, "mobile", 200));

  private final ComponentRenderTester tester = ComponentRenderTester.create();
  private final JsonMapper jsonMapper = JsonMapper.builder().build();

  record Visit(LocalDate day, YearMonth month, LocalDateTime at, Instant instant, OffsetDateTime offset,
               ZonedDateTime zoned, double visitors, Integer missing) {

  }

  @Test
  void rendersTheRowsAMarkerPerPartAndASkeleton() {
    Rendered rendered = tester.render("""
        <sl:chart th:data="${rows}" label="Visitors" class="mt-4" id="visitors">
          <sl:chart-grid/>
          <sl:chart-x-axis key="month"/>
          <sl:chart-tooltip/>
          <sl:chart-bar key="desktop" label="Desktop"/>
          <sl:chart-bar key="mobile" label="Mobile" color="var(--chart-4)"/>
        </sl:chart>""", Map.of("rows", ROWS));

    assertThat(rendered).hasNoLeakedMarkup();
    assertThat(rendered).root()
        .hasTag("sl-chart")
        .hasClass("chart", "mt-4")
        .hasAttribute("id", "visitors")
        .hasAttribute("label", "Visitors")
        .hasAttribute("time-zone", "UTC")
        .hasNoAttribute("aria-label", "role", "margin");
    Element chart = rendered.root();
    assertThat(chart.children()).extracting(Element::tagName).containsExactly("script", "sl-chart-grid",
        "sl-chart-x-axis", "sl-chart-tooltip", "sl-chart-bar", "sl-chart-bar", "div");
    assertThat(chart.child(0)).hasAttribute("type", "application/json");
    assertThat(chart.child(4)).hasClassName("chart-bar").hasAttribute("key", "desktop")
        .hasAttribute("label", "Desktop").hasNoAttribute("color", "color-dark", "radius", "stack");
    assertThat(chart.child(5)).hasAttribute("color", "var(--chart-4)");
    assertThat(chart.child(6)).hasClassName("chart-skeleton");
    JsonNode data = data(rendered);
    assertThat(data.size()).isEqualTo(2);
    assertThat(data.get(1).get("month").asString()).isEqualTo("February");
    assertThat(data.get(1).get("desktop").asInt()).isEqualTo(305);
  }

  @Test
  void writesJavaTimeAsIsoStrings() {
    Visit visit = new Visit(LocalDate.of(2024, 4, 1), YearMonth.of(2024, 4), LocalDateTime.of(2024, 4, 1, 10, 0),
        Instant.parse("2024-04-01T08:00:00Z"), OffsetDateTime.parse("2024-04-01T10:00:00+02:00"),
        ZonedDateTime.parse("2024-04-01T10:00:00+02:00[Europe/Brussels]"), 1.5, null);

    JsonNode row = data(tester.render("""
        <sl:chart th:data="${rows}" label="Visits"><sl:chart-line key="visitors"/></sl:chart>""",
        Map.of("rows", List.of(visit)))).get(0);

    assertThat(row.get("day").asString()).isEqualTo("2024-04-01");
    assertThat(row.get("month").asString()).isEqualTo("2024-04");
    assertThat(row.get("at").asString()).isEqualTo("2024-04-01T10:00:00");
    assertThat(row.get("instant").asString()).isEqualTo("2024-04-01T08:00:00Z");
    assertThat(row.get("offset").asString()).isEqualTo("2024-04-01T10:00:00+02:00");
    assertThat(row.get("zoned").asString()).isEqualTo("2024-04-01T10:00:00+02:00");
    assertThat(row.get("visitors").asDouble()).isEqualTo(1.5);
    assertThat(row.get("missing").isNull()).isTrue();
  }

  @Test
  void escapesTheDataBlockSoNoValueEndsTheScript() {
    Rendered rendered = tester.render("""
        <sl:chart th:data="${rows}" label="Visits"><sl:chart-bar key="visitors"/></sl:chart>""",
        Map.of("rows", List.of(Map.of("name", "</script><script>alert(1)</script>'\"&<!--", "visitors", 1))));

    assertThat(rendered.html()).doesNotContain("</script><script>");
    assertThat(rendered.select("script")).hasSize(1);
    assertThat(data(rendered).get(0).get("name").asString()).isEqualTo("</script><script>alert(1)</script>'\"&<!--");
  }

  @Test
  void takesTheRowsOfAPage() {
    JsonNode data = data(tester.render("""
        <sl:chart th:data="${page}" label="Visits"><sl:chart-bar key="desktop"/></sl:chart>""",
        Map.of("page", new PageImpl<>(ROWS, PageRequest.of(0, 2), 10))));

    assertThat(data.isArray()).isTrue();
    assertThat(data.size()).isEqualTo(2);
  }

  @Test
  void needsDataAndALabel() {
    assertThatRenderFailure(() -> tester.render("<sl:chart label=\"Visits\"/>"))
        .hasMessageContaining("<sl:chart> needs data");
    assertThatRenderFailure(() -> tester.render("<sl:chart th:data=\"${rows}\"/>", Map.of("rows", ROWS)))
        .hasMessageContaining("<sl:chart> needs label");
    assertThatRenderFailure(() -> tester.render("<sl:chart th:data=\"${rows}\" label=\"Visits\"/>",
        Map.of("rows", "not rows")))
        .hasMessageContaining("takes its data as a list of rows");
  }

  @ParameterizedTest
  @ValueSource(strings = {"1st", "-x", "desk top", "a;b", "a:b", ""})
  void keysMustBeCssIdentifiers(String key) {
    assertThatRenderFailure(() -> tester.render("<sl:chart-bar key=\"%s\"/>".formatted(key)))
        .hasMessageContaining("<sl:chart-bar key=\"%s\">: a key is a property name".formatted(key));
  }

  @Test
  void keysMayHaveHyphensUnderscoresAndCapitals() {
    assertThat(tester.render("<sl:chart-area key=\"total_Visits-2\"/>")).root()
        .hasAttribute("key", "total_Visits-2");
  }

  @Test
  void xAxisAndTooltipCheckTheirKeys() {
    assertThatRenderFailure(() -> tester.render("<sl:chart-x-axis key=\"2024\"/>"))
        .hasMessageContaining("<sl:chart-x-axis key=\"2024\">");
    assertThatRenderFailure(() -> tester.render("<sl:chart-line key=\"a\" dashed join=\"a b\"/>"))
        .hasMessageContaining("join");
    assertThatRenderFailure(() -> tester.render("<sl:chart-tooltip label-key=\"a b\"/>"))
        .hasMessageContaining("<sl:chart-tooltip label-key=\"a b\">");
  }

  @Test
  void colorDarkGoesWithTheLightColor() {
    assertThat(tester.render("""
        <sl:chart-line key="desktop" color="oklch(0.5 0.2 260)" color-dark="oklch(0.7 0.2 260)"/>""")).root()
        .hasAttribute("color", "oklch(0.5 0.2 260)")
        .hasAttribute("color-dark", "oklch(0.7 0.2 260)");
  }

  @Test
  void dynamicSeriesFromThEach() {
    Element chart = tester.render("""
        <sl:chart th:data="${rows}" label="Visitors">
          <sl:chart-bar th:each="series : ${series}" th:key="${series.key}" th:label="${series.label}"/>
        </sl:chart>""", Map.of("rows", ROWS, "series", List.of(Map.of("key", "desktop", "label", "Desktop"),
        Map.of("key", "mobile", "label", "Mobile")))).root();

    assertThat(chart.select("> sl-chart-bar")).extracting(bar -> bar.attr("key")).containsExactly("desktop", "mobile");
    assertThat(chart.select("> sl-chart-bar")).extracting(bar -> bar.attr("label")).containsExactly("Desktop",
        "Mobile");
  }

  @Test
  void timeZoneComesFromTheRequest() {
    ComponentRenderTester brussels = ComponentRenderTester.builder().timeZone(ZoneId.of("Europe/Brussels")).build();

    assertThat(brussels.render("<sl:chart th:data=\"${rows}\" label=\"Visits\"/>", Map.of("rows", ROWS))).root()
        .hasAttribute("time-zone", "Europe/Brussels");
  }

  @Test
  void timeZoneOfTheChartWins() {
    ComponentRenderTester brussels = ComponentRenderTester.builder().timeZone(ZoneId.of("Europe/Brussels")).build();

    assertThat(brussels.render("<sl:chart th:data=\"${rows}\" label=\"Visits\" time-zone=\"America/New_York\"/>",
        Map.of("rows", ROWS))).root().hasAttribute("time-zone", "America/New_York");
  }

  @Test
  void fixedOffsetsAreWrittenAsIntlReadsThem() {
    assertThat(ComponentRenderTester.builder().timeZone(ZoneId.of("GMT")).build()
        .render("<sl:chart th:data=\"${rows}\" label=\"Visits\"/>", Map.of("rows", ROWS))).root()
        .hasAttribute("time-zone", "UTC");
    assertThat(tester.render("<sl:chart th:data=\"${rows}\" label=\"Visits\" time-zone=\"+02:00\"/>",
        Map.of("rows", ROWS))).root().hasAttribute("time-zone", "+02:00");
  }

  @Test
  void anUnknownTimeZoneFails() {
    assertThatRenderFailure(() -> tester.render("<sl:chart th:data=\"${rows}\" label=\"Visits\" time-zone=\"Mars/Base\"/>",
        Map.of("rows", ROWS)))
        .hasMessageContaining("<sl:chart time-zone=\"Mars/Base\">: not a time zone");
  }

  @Test
  void formatsAreEnumsSoATypoFails() {
    assertThatRenderFailure(() -> tester.render("<sl:chart-x-axis key=\"month\" format=\"monthly\"/>"))
        .hasMessageContaining("monthly");
  }

  @Test
  void currencyFormatNeedsACurrency() {
    assertThatRenderFailure(() -> tester.render("<sl:chart-y-axis format=\"currency\"/>"))
        .hasMessageContaining("<sl:chart-y-axis> formats as currency: add currency=\"EUR\"");
    assertThatRenderFailure(() -> tester.render("<sl:chart-tooltip value-format=\"currency\"/>"))
        .hasMessageContaining("<sl:chart-tooltip> formats as currency");
    assertThatRenderFailure(() -> tester.render("<sl:chart-tooltip value-format=\"currency\" currency=\"EURO\"/>"))
        .hasMessageContaining("<sl:chart-tooltip currency=\"EURO\">: not an ISO 4217 currency code");
    assertThat(tester.render("<sl:chart-tooltip value-format=\"currency\" currency=\"EUR\"/>")).root()
        .hasAttribute("value-format", "currency").hasAttribute("currency", "EUR");
  }

  @Test
  void radiusIsOneOrFourNumbers() {
    assertThat(tester.render("<sl:chart-bar key=\"a\" radius=\"4, 4, 0, 0\"/>")).root()
        .hasAttribute("radius", "4,4,0,0");
    assertThatRenderFailure(() -> tester.render("<sl:chart-bar key=\"a\" radius=\"4,4\"/>"))
        .hasMessageContaining("<sl:chart-bar radius=\"4,4\">");
  }

  @Test
  void defaultsAreLeftToTheScript() {
    assertThat(tester.render("<sl:chart-tooltip/>")).root().hasAttributeNames("class", "value-format");
    assertThat(tester.render("<sl:chart-x-axis key=\"month\"/>")).root().hasAttributeNames("class", "key");
    assertThat(tester.render("<sl:chart-line key=\"a\"/>")).root().hasAttributeNames("class", "key");
    assertThat(tester.render("<sl:chart-area key=\"a\"/>")).root().hasAttributeNames("class", "key");
    assertThat(tester.render("<sl:chart-grid/>")).root().hasAttributeNames("class");
    assertThat(tester.render("<sl:chart-legend/>")).root().hasAttributeNames("class");
  }

  @Test
  void nonDefaultsAreWrittenAsPlainAttributes() {
    assertThat(tester.render("""
        <sl:chart-tooltip indicator="dashed" cursor="false" hide-label hide-indicator label-key="date"
                          label-format="day" value-format="percent"/>""")).root()
        .hasAttribute("indicator", "dashed").hasAttribute("cursor", "false").hasAttribute("hide-label", "true")
        .hasAttribute("hide-indicator", "true").hasAttribute("label-key", "date").hasAttribute("label-format", "day")
        .hasAttribute("value-format", "percent").hasNoAttribute("data-indicator");
    assertThat(tester.render("<sl:chart-grid vertical horizontal=\"false\"/>")).root()
        .hasAttribute("vertical", "true").hasAttribute("horizontal", "false");
    assertThat(tester.render("<sl:chart-line key=\"a\" curve=\"step-after\" dots dashed join=\"desktop\" stroke-width=\"3\"/>")).root()
        .hasAttribute("curve", "step-after").hasAttribute("dots", "true").hasAttribute("dashed", "true")
        .hasAttribute("join", "desktop").hasAttribute("stroke-width", "3");
    assertThat(tester.render("<sl:chart-area key=\"a\" stack=\"a\" dashed join=\"desktop\" fill-opacity=\"0.8\"/>")).root()
        .hasAttribute("stack", "a").hasAttribute("dashed", "true").hasAttribute("join", "desktop")
        .hasAttribute("fill-opacity", "0.8");
    assertThat(tester.render("<sl:chart-legend vertical-align=\"top\"/>")).root()
        .hasAttribute("vertical-align", "top");
    assertThat(tester.render("<sl:chart th:data=\"${rows}\" label=\"Visits\" margin=\"12\"/>",
        Map.of("rows", ROWS))).root().hasAttribute("margin", "12");
  }

  private JsonNode data(Rendered rendered) {
    return jsonMapper.readTree(rendered.root().selectFirst("script[type=application/json]").data());
  }
}
