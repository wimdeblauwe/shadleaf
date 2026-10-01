package io.github.wimdeblauwe.shadleaf.sample01;

import java.util.Set;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * The people page: a table of a few hundred people in an H2 database, sorted and paged by Spring Data JPA. The
 * column headers link to {@code ?sort=<property>,asc|desc}, the pagination to {@code ?page=<n>} and the size menu to
 * {@code ?size=<n>}, which Spring Data turns into the {@link Pageable}; the page it returns tells the table how it is
 * sorted and the pagination where it is.
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
  public String people(@PageableDefault(size = PAGE_SIZE, sort = "name") Pageable pageable, Model model) {
    model.addAttribute("people", repository.findAll(stable(sortable(pageable))));
    return "people";
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
