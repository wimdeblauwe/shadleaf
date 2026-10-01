package io.github.wimdeblauwe.shadleaf.sample01;

import io.github.wimdeblauwe.htmx.spring.boot.mvc.HtmxRequest;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * The people page: a table of a few hundred people in an H2 database, sorted and paged by Spring Data JPA. The
 * column headers link to {@code ?sort=<property>,asc|desc}, the pagination to {@code ?page=<n>} and the size menu to
 * {@code ?size=<n>}, which Spring Data turns into the {@link Pageable}; the page it returns tells the table how it is
 * sorted and the pagination where it is.
 * <p>
 * The search form above the table sends {@code ?q=}, with the other parameters copied in by {@code sl:query-params},
 * and filters on name and email. With htmx it searches while the user types, and the column headers, page links and
 * size menu are boosted: htmx sends {@code HX-Request}, and the answer is then the {@code results} fragment alone (the
 * table and its footer), which htmx swaps in. Both ask for this same URL, so the address htmx pushes and the links the
 * fragment's components build from the request agree. A history restore (back to a page htmx did not keep) gets the
 * whole page, as does every request without htmx.
 */
@Controller
public class PeopleController {

  static final int PAGE_SIZE = 20;

  /** The properties of the table's sortable columns. Anything else in ?sort= falls back to the default order. */
  private static final Set<String> SORTABLE = Set.of("name", "email", "role", "orders", "joined");

  private final PersonRepository repository;

  public PeopleController(PersonRepository repository) {
    this.repository = repository;
  }

  @GetMapping("/people")
  public String people(@RequestParam(name = "q", required = false) String q,
      @PageableDefault(size = PAGE_SIZE, sort = "name") Pageable pageable, HtmxRequest htmxRequest, Model model) {
    String search = q == null ? "" : q.strip();
    Pageable query = stable(sortable(pageable));
    Page<Person> people = search.isEmpty()
        ? repository.findAll(query)
        : repository.findByNameContainingIgnoreCaseOrEmailContainingIgnoreCase(search, search, query);
    model.addAttribute("people", people);
    model.addAttribute("search", search);
    // What a screen reader announces after a search (the form's role="status").
    model.addAttribute("found", search.isEmpty() ? ""
        : people.getTotalElements() == 1 ? "1 person found" : people.getTotalElements() + " people found");
    boolean fragment = htmxRequest.isHtmxRequest() && !htmxRequest.isHistoryRestoreRequest();
    model.addAttribute("fragment", fragment);
    return fragment ? "people :: results" : "people";
  }

  /** The pageable as requested, or sorted by name when it names a property the table does not offer. */
  private static Pageable sortable(Pageable pageable) {
    boolean allowed = pageable.getSort().stream().allMatch(order -> SORTABLE.contains(order.getProperty()));
    return allowed
        ? pageable
        : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by("name"));
  }

  /**
   * The pageable with the id as its last sort key. Rows with the same value in the sorted column (two people with 59
   * orders) come back in no particular order, so without a unique last key such a row could show up on two pages or
   * on none. The headers show only the first key, so the id never appears in a link.
   */
  private static Pageable stable(Pageable pageable) {
    return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort().and(Sort.by("id")));
  }
}
