package io.github.wimdeblauwe.shadleaf.sample01;

import io.github.wimdeblauwe.htmx.spring.boot.mvc.HtmxRequest;
import io.github.wimdeblauwe.htmx.spring.boot.mvc.HtmxResponse;
import io.github.wimdeblauwe.shadleaf.toast.Toast;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * The people pages: a table of a few hundred people in an H2 database, sorted and paged by Spring Data JPA. The
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
 * <p>
 * {@code /people-multiselect} is the same table with a checkbox per row ({@code ids}), in a form that posts the
 * selected ids to this same URL,
 * query string included, so the delete knows the search, order and page it came from. Without htmx it redirects back
 * there with a toast (post/redirect/get); with htmx the answer is the results fragment of that same page, with the
 * toast and the close of the confirm dialog in {@code HX-Trigger}. The selection is per page: a sort, page or search
 * starts with nothing selected.
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
      @PageableDefault(size = PAGE_SIZE, sort = "name") Pageable pageable, HtmxRequest htmxRequest,
      HttpServletRequest request, Model model) {
    return list("people", q, pageable, htmxRequest, request, model);
  }

  @GetMapping("/people-multiselect")
  public String peopleMultiselect(@RequestParam(name = "q", required = false) String q,
      @PageableDefault(size = PAGE_SIZE, sort = "name") Pageable pageable, HtmxRequest htmxRequest,
      HttpServletRequest request, Model model) {
    return list("people-multiselect", q, pageable, htmxRequest, request, model);
  }

  /** Deletes the selected people, then shows the page the form was on: the query string carries q, sort and page. */
  @PostMapping("/people-multiselect")
  public String delete(@RequestParam(name = "ids", required = false) List<Long> ids,
      @RequestParam(name = "q", required = false) String q,
      @PageableDefault(size = PAGE_SIZE, sort = "name") Pageable pageable, HtmxRequest htmxRequest,
      HtmxResponse htmxResponse, HttpServletRequest request, Model model, RedirectAttributes redirectAttributes) {
    Toast toast = deleteAll(ids == null ? List.of() : ids);
    if (!htmxRequest.isHtmxRequest()) {
      redirectAttributes.addFlashAttribute("toasts", List.of(toast));
      return "redirect:" + selfUrl(request).substring(request.getContextPath().length());
    }
    // Closes the confirm dialog first, so the focus goes back to its trigger, which htmx finds again by id after the
    // swap.
    htmxResponse.addTrigger("sl-dialog-close");
    htmxResponse.addTrigger("sl-toast", toast);
    return list("people-multiselect", q, pageable, htmxRequest, request, model);
  }

  private Toast deleteAll(List<Long> ids) {
    if (ids.isEmpty()) {
      return Toast.warning("Nothing was deleted").withDescription("Select the people to delete first.");
    }
    int found = repository.findAllById(ids).size();
    repository.deleteAllByIdInBatch(ids);
    return Toast.success(found == 1 ? "1 person deleted" : found + " people deleted");
  }

  private String list(String view, String q, Pageable pageable, HtmxRequest htmxRequest, HttpServletRequest request, Model model) {
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
    // Where the selection form posts: this page, with its search, order and page.
    model.addAttribute("selfUrl", selfUrl(request));
    return fragment ? view + " :: results" : view;
  }

  /** The request's path (context path included) and query string. */
  private static String selfUrl(HttpServletRequest request) {
    String query = request.getQueryString();
    return request.getRequestURI() + (query == null ? "" : "?" + query);
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
