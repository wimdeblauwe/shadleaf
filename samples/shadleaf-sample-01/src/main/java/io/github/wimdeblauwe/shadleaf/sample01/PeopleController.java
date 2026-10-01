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
 * The people page: a table of a few hundred people in an H2 database, sorted by Spring Data JPA. The column headers
 * are links to {@code ?sort=<property>,asc|desc}, which Spring Data turns into the {@link Pageable}; the page it
 * returns tells the table how it is sorted. Paging arrives with the pagination component; until then the page shows
 * the first {@value #PAGE_SIZE} rows.
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
    model.addAttribute("people", repository.findAll(sortable(pageable)));
    return "people";
  }

  /** The pageable as requested, or sorted by name when it names a property the table does not offer. */
  private static Pageable sortable(Pageable pageable) {
    boolean allowed = pageable.getSort().stream().allMatch(order -> SORTABLE.contains(order.getProperty()));
    return allowed
        ? pageable
        : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by("name"));
  }
}
