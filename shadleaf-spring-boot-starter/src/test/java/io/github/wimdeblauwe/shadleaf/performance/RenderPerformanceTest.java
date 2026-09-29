package io.github.wimdeblauwe.shadleaf.performance;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.wimdeblauwe.shadleaf.test.ComponentRenderTester;
import io.github.wimdeblauwe.shadleaf.test.Rendered;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Measures what a page of 200 components costs to render, against the same markup written by hand and rendered by a
 * plain Thymeleaf fragment, on the same engine.
 * <p>
 * Excluded from the normal build, because timings on a shared CI runner say little. Run it with
 * {@code mvn test -Pperformance} in the starter. It prints a table and writes it to
 * {@code target/performance/render-performance.txt}.
 * <p>
 * Every scenario renders a cached page template, as an application does in production, so the numbers are the cost
 * of rendering rather than of parsing the page. The component templates are cached as well.
 */
@Tag("performance")
class RenderPerformanceTest {

  private static final int ITEMS = 200;
  private static final long WARMUP_NANOS = 3_000_000_000L;
  private static final long MEASURE_NANOS = 5_000_000_000L;

  private static final String TRASH_SVG = """
      <svg xmlns="http://www.w3.org/2000/svg" class="sl-icon" viewBox="0 0 24 24" width="24" height="24" \
      fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" \
      aria-hidden="true" focusable="false"><path d="M10 11v6"/><path d="M14 11v6"/>\
      <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6"/><path d="M3 6h18"/>\
      <path d="M8 6V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/></svg>""";

  private static final List<Scenario> SCENARIOS = List.of(
      new Scenario("hand-written markup", """
          <th:block th:each="item : ${items}"><button class="btn" type="button" data-variant="outline">\
          <span class="btn-icon" data-icon="inline-start">%s</span>[[${item}]]</button></th:block>"""
          .formatted(TRASH_SVG)),
      new Scenario("Thymeleaf fragment", """
          <th:block th:each="item : ${items}">\
          <th:block th:replace="~{sl/perf/plain-button :: button('outline', ${item})}"></th:block></th:block>"""),
      new Scenario("<sl:button>", """
          <th:block th:each="item : ${items}"><sl:button variant="outline">[[${item}]]</sl:button></th:block>"""),
      new Scenario("<sl:button> + <sl:icon>", """
          <th:block th:each="item : ${items}"><sl:button variant="outline">\
          <sl:slot name="icon-start"><sl:icon name="trash"/></sl:slot>[[${item}]]</sl:button></th:block>"""),
      new Scenario("<sl:icon>", """
          <th:block th:each="item : ${items}"><sl:icon name="trash"/></th:block>"""));

  private final ComponentRenderTester renderer = ComponentRenderTester.builder().cacheSnippets().build();
  private final Map<String, Object> variables = Map.of(
      "items", IntStream.rangeClosed(1, ITEMS).mapToObj(i -> "Delete order " + i).toList());

  @Test
  void twoHundredButtonsWithIcons() throws IOException {
    assertEquivalentMarkup();

    for (Scenario scenario : SCENARIOS) {
      runFor(scenario, WARMUP_NANOS);
    }
    List<Result> results = new ArrayList<>();
    for (Scenario scenario : SCENARIOS) {
      results.add(new Result(scenario, runFor(scenario, MEASURE_NANOS)));
    }

    String report = report(results);
    System.out.println(report);
    Path file = Path.of("target/performance/render-performance.txt");
    Files.createDirectories(file.getParent());
    Files.writeString(file, report);
  }

  /** The baselines only mean something if they render what the components render. */
  private void assertEquivalentMarkup() {
    Rendered fragment = render(SCENARIOS.get(1));
    Rendered component = render(SCENARIOS.get(3));
    assertThat(render(SCENARIOS.get(0)).normalizedHtml()).isEqualTo(fragment.normalizedHtml());
    assertThat(component.normalizedHtml()).isEqualTo(fragment.normalizedHtml());
    assertThat(component.select("button.btn[data-variant=outline] > .btn-icon > svg")).hasSize(ITEMS);
  }

  private long[] runFor(Scenario scenario, long nanos) {
    List<Long> timings = new ArrayList<>();
    long end = System.nanoTime() + nanos;
    while (System.nanoTime() < end) {
      long start = System.nanoTime();
      render(scenario);
      timings.add(System.nanoTime() - start);
    }
    return timings.stream().mapToLong(Long::longValue).sorted().toArray();
  }

  private Rendered render(Scenario scenario) {
    return renderer.render(scenario.template(), variables);
  }

  private static String report(List<Result> results) {
    double fragmentMedian = results.get(1).median();
    StringBuilder report = new StringBuilder();
    report.append("Rendering %d items per page, Java %s, %s%n%n"
        .formatted(ITEMS, Runtime.version(), System.getProperty("os.arch")));
    report.append("%-26s %8s %12s %12s %12s %14s%n"
        .formatted("scenario", "renders", "median (ms)", "p90 (ms)", "per item (µs)", "vs fragment"));
    for (Result result : results) {
      report.append("%-26s %8d %12.2f %12.2f %12.1f %13.1fx%n".formatted(
          result.scenario().name(), result.timings().length, result.median() / 1e6, result.p90() / 1e6,
          result.median() / ITEMS / 1e3, result.median() / fragmentMedian));
    }
    return report.toString();
  }

  private record Scenario(String name, String template) {
  }

  private record Result(Scenario scenario, long[] timings) {

    double median() {
      return percentile(0.5);
    }

    double p90() {
      return percentile(0.9);
    }

    private double percentile(double p) {
      return timings[(int) Math.min(timings.length - 1, Math.floor(p * timings.length))];
    }
  }
}
