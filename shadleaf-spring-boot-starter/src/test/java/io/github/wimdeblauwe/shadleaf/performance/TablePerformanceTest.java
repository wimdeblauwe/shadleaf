package io.github.wimdeblauwe.shadleaf.performance;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.wimdeblauwe.shadleaf.test.ComponentRenderTester;
import io.github.wimdeblauwe.shadleaf.test.Rendered;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import java.util.zip.GZIPOutputStream;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Measures what a table page of 50 rows costs, each row with a badge and a menu of row actions, as sample-01's
 * {@code /people} page renders it: render time and HTML size (plain and gzipped), against the same table without the
 * menu, without the badge, and with only the menu's trigger (the alternative where htmx fetches the menu's items when
 * it opens). The M7 baseline, 200 buttons with an icon, runs in the same JVM, so the numbers compare on one machine.
 * <p>
 * Excluded from the normal build, like {@link RenderPerformanceTest}. Run it with {@code mvn test -Pperformance} in the
 * starter. It prints a table and writes it to {@code target/performance/table-performance.txt}.
 */
@Tag("performance")
class TablePerformanceTest {

  private static final int ROWS = 50;
  private static final long WARMUP_NANOS = 3_000_000_000L;
  private static final long MEASURE_NANOS = 5_000_000_000L;
  private static final List<String> ROLES = List.of("OWNER", "ADMIN", "MEMBER", "GUEST");

  private static final String HEAD = """
      <sl:table><thead><tr>\
      <sl:table-head sort="name">Name</sl:table-head><sl:table-head sort="email">Email</sl:table-head>\
      <sl:table-head sort="role">Role</sl:table-head><sl:table-head sort="orders" align="end">Orders</sl:table-head>\
      %s</tr></thead><tbody>""";
  private static final String CELLS = """
      <td th:text="${person.name}">Ada</td><td th:text="${person.email}">ada@example.com</td>""";
  private static final String ORDERS = """
      <td data-align="end" th:text="${person.orders}">3</td>""";
  private static final String BADGE = """
      <td><sl:badge th:variant="${person.role == 'MEMBER' ? 'outline' : 'secondary'}" \
      th:text="${person.role}">Member</sl:badge></td>""";
  private static final String TRIGGER = """
      <sl:dropdown-menu-trigger variant="ghost" size="icon-sm" th:aria-label="|Actions for ${person.name}|">\
      <sl:icon name="ellipsis"/></sl:dropdown-menu-trigger>""";
  /** The row menu of sample-01's /people: View, the four roles as radio items that submit, Delete. */
  private static final String MENU = """
      <td><sl:dropdown-menu th:id="|people-${person.id}-actions|">%s\
      <sl:dropdown-menu-content align="end">\
      <sl:dropdown-menu-item as="a" th:href="@{/people/{id}(id=${person.id})}" hx-boost="false">\
      <sl:icon name="eye"/>View</sl:dropdown-menu-item>\
      <sl:dropdown-menu-separator/>\
      <sl:dropdown-menu-label>Change role</sl:dropdown-menu-label>\
      <sl:dropdown-menu-radio-group aria-label="Role">\
      <sl:dropdown-menu-radio-item th:each="role : ${roles}" type="submit" th:form="|people-${person.id}-form|" \
      name="role" th:value="${role}" th:checked="${person.role == role}" hx-post="/people">[[${role}]]\
      </sl:dropdown-menu-radio-item></sl:dropdown-menu-radio-group>\
      <sl:dropdown-menu-separator/>\
      <sl:dropdown-menu-item as="a" variant="destructive" th:href="@{/people/{id}/delete(id=${person.id})}" \
      th:hx-get="@{/people/{id}/delete(id=${person.id})}" hx-target="#modal-root">\
      <sl:icon name="trash-2"/>Delete</sl:dropdown-menu-item>\
      </sl:dropdown-menu-content></sl:dropdown-menu>\
      <form th:id="|people-${person.id}-form|" method="post" action="/people">\
      <input type="hidden" name="person" th:value="${person.id}"></form></td>""".formatted(TRIGGER);
  /** The alternative: the trigger and an empty menu, whose items htmx fetches when it opens. */
  private static final String LAZY_MENU = """
      <td><sl:dropdown-menu th:id="|people-${person.id}-actions|">%s\
      <sl:dropdown-menu-content align="end" th:hx-get="@{/people/{id}/actions(id=${person.id})}" \
      hx-trigger="toggle once"></sl:dropdown-menu-content></sl:dropdown-menu></td>""".formatted(TRIGGER);

  private static final List<Scenario> SCENARIOS = List.of(
      table("table, text cells only", "", CELLS + "<td th:text=\"${person.role}\">Member</td>" + ORDERS),
      table("+ <sl:badge>", "", CELLS + BADGE + ORDERS),
      table("+ trigger, items by htmx", "<th>Actions</th>", CELLS + BADGE + ORDERS + LAZY_MENU),
      table("+ row menu", "<th>Actions</th>", CELLS + BADGE + ORDERS + MENU),
      new Scenario("M7: 200 buttons + icon", """
          <th:block th:each="item : ${items}"><sl:button variant="outline">\
          <sl:slot name="icon-start"><sl:icon name="trash"/></sl:slot>[[${item}]]</sl:button></th:block>"""));

  private final ComponentRenderTester renderer = ComponentRenderTester.builder().cacheSnippets().build();
  private final Map<String, Object> variables = Map.of(
      "people", IntStream.rangeClosed(1, ROWS).mapToObj(i -> new Person(i, "Person " + i,
          "person" + i + "@example.com", ROLES.get(i % ROLES.size()), i * 3)).toList(),
      "roles", ROLES,
      "items", IntStream.rangeClosed(1, 200).mapToObj(i -> "Delete order " + i).toList());

  @Test
  void fiftyRowsWithABadgeAndAMenu() throws IOException {
    Rendered menus = render(SCENARIOS.get(3));
    assertThat(menus.select("tbody > tr")).hasSize(ROWS);
    assertThat(menus.select("tbody .dropdown-menu-content[role=menu] > .dropdown-menu-radio-group > [role=menuitemradio]"))
        .hasSize(ROWS * ROLES.size());
    assertThat(menus.select("tbody .badge")).hasSize(ROWS);

    for (Scenario scenario : SCENARIOS) {
      runFor(scenario, WARMUP_NANOS);
    }
    List<Result> results = new ArrayList<>();
    for (Scenario scenario : SCENARIOS) {
      String html = render(scenario).html();
      results.add(new Result(scenario, runFor(scenario, MEASURE_NANOS), html.getBytes(StandardCharsets.UTF_8).length,
          gzipped(html)));
    }

    String report = report(results);
    System.out.println(report);
    Path file = Path.of("target/performance/table-performance.txt");
    Files.createDirectories(file.getParent());
    Files.writeString(file, report);
  }

  private static Scenario table(String name, String extraHead, String cells) {
    return new Scenario(name, HEAD.formatted(extraHead)
        + "<tr th:each=\"person : ${people}\">" + cells + "</tr></tbody></sl:table>");
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

  private static int gzipped(String html) {
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    try (GZIPOutputStream gzip = new GZIPOutputStream(bytes)) {
      gzip.write(html.getBytes(StandardCharsets.UTF_8));
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    return bytes.size();
  }

  private static String report(List<Result> results) {
    StringBuilder report = new StringBuilder();
    report.append("A table of %d rows, Java %s, %s%n%n".formatted(ROWS, Runtime.version(), System.getProperty("os.arch")));
    report.append("%-28s %8s %12s %10s %12s %12s%n"
        .formatted("scenario", "renders", "median (ms)", "p90 (ms)", "HTML (KB)", "gzipped (KB)"));
    for (Result result : results) {
      report.append("%-28s %8d %12.2f %10.2f %12.1f %12.1f%n".formatted(
          result.scenario().name(), result.timings().length, result.median() / 1e6, result.p90() / 1e6,
          result.bytes() / 1024.0, result.gzipped() / 1024.0));
    }
    return report.toString();
  }

  public record Person(long id, String name, String email, String role, int orders) {
  }

  private record Scenario(String name, String template) {
  }

  private record Result(Scenario scenario, long[] timings, int bytes, int gzipped) {

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
