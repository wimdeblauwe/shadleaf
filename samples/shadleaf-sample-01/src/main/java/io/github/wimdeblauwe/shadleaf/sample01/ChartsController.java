package io.github.wimdeblauwe.shadleaf.sample01;

import io.github.wimdeblauwe.shadleaf.sample01.Person.Role;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * The charts page: sl:chart over aggregates of the people table (in H2), grouped by the database. The rows are maps
 * with a YearMonth per month and a count per role, as the series are the roles: the template has one series part per
 * role, with th:each.
 */
@Controller
public class ChartsController {

  /** The last months the joined-per-month bars show. */
  private static final int RECENT_MONTHS = 12;

  private final PersonRepository repository;

  ChartsController(PersonRepository repository) {
    this.repository = repository;
  }

  /** People of a role who joined in a month, as the database counts them. */
  public record JoinCount(int year, int month, Role role, long count) {

  }

  /** The orders of a role's people, in total and per person. */
  public record RoleOrders(Role role, long orders, double average) {

    public String getLabel() {
      return role.getLabel();
    }
  }

  /** A series of the charts: a role, by the key of its column in the rows and its label. */
  public record Series(String key, String label) {

  }

  @GetMapping("/charts")
  public String charts(Model model) {
    List<Series> series = Arrays.stream(Role.values())
        .map(role -> new Series(role.name().toLowerCase(Locale.ROOT), role.getLabel()))
        .toList();
    List<Map<String, Object>> joined = joinedPerMonth(repository.countJoinedPerMonthAndRole());
    model.addAttribute("series", series);
    model.addAttribute("joinedRecently", joined.subList(Math.max(0, joined.size() - RECENT_MONTHS), joined.size()));
    model.addAttribute("membersOverTime", runningTotals(joined));
    model.addAttribute("ordersPerRole", repository.ordersPerRole().stream()
        .sorted(Comparator.comparing(RoleOrders::role))
        .toList());
    return "charts";
  }

  /** A row per month from the first to the last one anyone joined in, with the count of every role (0 if none). */
  private static List<Map<String, Object>> joinedPerMonth(List<JoinCount> counts) {
    TreeMap<YearMonth, Map<String, Object>> months = new TreeMap<>();
    for (JoinCount count : counts) {
      row(months, YearMonth.of(count.year(), count.month())).put(key(count.role()), count.count());
    }
    if (!months.isEmpty()) {
      for (YearMonth month = months.firstKey(); month.isBefore(months.lastKey()); month = month.plusMonths(1)) {
        row(months, month);
      }
    }
    return new ArrayList<>(months.values());
  }

  /** The same rows with how many people of each role had joined by the end of each month. */
  private static List<Map<String, Object>> runningTotals(List<Map<String, Object>> joined) {
    Map<String, Long> totals = new LinkedHashMap<>();
    List<Map<String, Object>> rows = new ArrayList<>();
    for (Map<String, Object> month : joined) {
      Map<String, Object> row = new LinkedHashMap<>();
      row.put("month", month.get("month"));
      for (Role role : Role.values()) {
        row.put(key(role), totals.merge(key(role), (Long) month.get(key(role)), Long::sum));
      }
      rows.add(row);
    }
    return rows;
  }

  private static Map<String, Object> row(TreeMap<YearMonth, Map<String, Object>> months, YearMonth month) {
    return months.computeIfAbsent(month, key -> {
      Map<String, Object> row = new LinkedHashMap<>();
      row.put("month", key);
      for (Role role : Role.values()) {
        row.put(key(role), 0L);
      }
      return row;
    });
  }

  private static String key(Role role) {
    return role.name().toLowerCase(Locale.ROOT);
  }
}
