package io.github.wimdeblauwe.shadleaf.sample01;

import io.github.wimdeblauwe.htmx.spring.boot.mvc.HtmxRequest;
import io.github.wimdeblauwe.htmx.spring.boot.mvc.HtmxResponse;
import io.github.wimdeblauwe.shadleaf.toast.Toast;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
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
 * whole page, as do a link the layout's boost sends (the sidebar's) and every request without htmx
 * ({@link HtmxRequests}).
 * <p>
 * Each row has a menu of actions. View is a link to the person's page. Change role is a radio group of submit buttons
 * in a form around the menu, which posts the person and the role to this page's own URL, query string included, so
 * the answer knows the search, order and page it came from: without htmx a redirect back there with a toast, with htmx
 * the results fragment of that page and the toast in {@code HX-Trigger}. Delete links to a confirm page
 * ({@code /people/<id>/delete}); with htmx the same request fetches an alert dialog instead, whose Delete button posts
 * to the page's URL as well and closes the dialog with {@code HX-Trigger}. After a change the focus goes back to the
 * row's menu button, or, when the row left the page, to the one in its place.
 * <p>
 * {@code /people-load-more} reads the people a {@link Slice} at a time and appends the next slice's rows with htmx;
 * without htmx its Load more link opens the page with one slice more.
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
    if (!HtmxRequests.wantsFragment(htmxRequest)) {
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

  /** Changes a person's role from the row menu, then shows the page the menu was on. */
  @PostMapping(path = "/people", params = "role")
  public String changeRole(@RequestParam("person") long id, @RequestParam Person.Role role,
      @RequestParam(name = "q", required = false) String q,
      @PageableDefault(size = PAGE_SIZE, sort = "name") Pageable pageable, HtmxRequest htmxRequest,
      HtmxResponse htmxResponse, HttpServletRequest request, Model model, RedirectAttributes redirectAttributes) {
    Person person = person(id);
    List<Person> before = find(q, pageable).getContent();
    person.changeRole(role);
    repository.save(person);
    Toast toast = Toast.success("%s is now %s".formatted(person.getName(), article(role)));
    if (!HtmxRequests.wantsFragment(htmxRequest)) {
      redirectAttributes.addFlashAttribute("toasts", List.of(toast));
      return "redirect:" + selfUrl(request).substring(request.getContextPath().length());
    }
    htmxResponse.addTrigger("sl-toast", toast);
    String view = list("people", q, pageable, htmxRequest, request, model);
    model.addAttribute("focusPerson", focusAfter(before, id, people(model)));
    return view;
  }

  /** Deletes a person, after the confirm page or the alert dialog, then shows the page the menu was on. */
  @PostMapping(path = "/people", params = "delete")
  public String deleteOne(@RequestParam("delete") long id, @RequestParam(name = "q", required = false) String q,
      @PageableDefault(size = PAGE_SIZE, sort = "name") Pageable pageable, HtmxRequest htmxRequest,
      HtmxResponse htmxResponse, HttpServletRequest request, Model model, RedirectAttributes redirectAttributes) {
    Person person = person(id);
    List<Person> before = find(q, pageable).getContent();
    repository.delete(person);
    Toast toast = Toast.success("%s was deleted".formatted(person.getName()));
    if (!HtmxRequests.wantsFragment(htmxRequest)) {
      redirectAttributes.addFlashAttribute("toasts", List.of(toast));
      return "redirect:" + selfUrl(request).substring(request.getContextPath().length());
    }
    // Closes the alert dialog first: it gives the focus back to the row's menu button, which the swap then removes,
    // so the next row's menu button takes it (autofocus).
    htmxResponse.addTrigger("sl-dialog-close");
    htmxResponse.addTrigger("sl-toast", toast);
    String view = list("people", q, pageable, htmxRequest, request, model);
    model.addAttribute("focusPerson", focusAfter(before, id, people(model)));
    return view;
  }

  /** The person's page: View in the row menu. */
  @GetMapping("/people/{id}")
  public String person(@PathVariable long id, Model model) {
    model.addAttribute("person", person(id));
    model.addAttribute("confirm", false);
    return "person";
  }

  /**
   * Delete in the row menu: with htmx an alert dialog for {@code #modal-root}, without it a page asking the same. The
   * query string is the people page's (search, order, page), which the delete posts to and Cancel goes back to.
   */
  @GetMapping("/people/{id}/delete")
  public String confirmDelete(@PathVariable long id, HtmxRequest htmxRequest, HttpServletRequest request,
      Model model) {
    model.addAttribute("person", person(id));
    model.addAttribute("confirm", true);
    String query = request.getQueryString();
    model.addAttribute("pageUrl", request.getContextPath() + "/people" + (query == null ? "" : "?" + query));
    return HtmxRequests.wantsFragment(htmxRequest) ? "person :: delete-dialog" : "person";
  }

  /** The people page with one slice more each time, or with htmx the next slice's rows alone. */
  @GetMapping("/people-load-more")
  public String loadMore(@PageableDefault(size = PAGE_SIZE, sort = "name") Pageable pageable,
      HtmxRequest htmxRequest, Model model) {
    Pageable query = stable(sortable(pageable));
    boolean fragment = HtmxRequests.wantsFragment(htmxRequest);
    Slice<Person> people;
    int offset;
    if (fragment) {
      people = repository.findAllBy(query);
      offset = (int) query.getOffset();
    } else {
      // Every slice up to the requested one, as htmx would have appended them: a reload, or the Load more link
      // without JavaScript, shows the same rows. Its number is still the requested one, so the next link is right.
      Slice<Person> all = repository.findAllBy(PageRequest.of(0, (int) query.getOffset() + query.getPageSize(),
          query.getSort()));
      people = new SliceImpl<>(all.getContent(), query, all.hasNext());
      offset = 0;
    }
    model.addAttribute("people", people);
    // The position of the first row rendered, for the rows' ids and the first row of each slice.
    model.addAttribute("offset", offset);
    model.addAttribute("fragment", fragment);
    return fragment ? "people-load-more :: rows" : "people-load-more";
  }

  private Person person(long id) {
    return repository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
  }

  private static String article(Person.Role role) {
    return (role == Person.Role.ADMIN || role == Person.Role.OWNER ? "an " : "a ") + role.name().toLowerCase(Locale.ROOT);
  }

  /**
   * Whose menu button takes the focus after a change: the person's, while they are still on the page (htmx would find
   * it by its id anyway), else the person now in their place (or the last row, when theirs was the last).
   */
  private static @Nullable Long focusAfter(List<Person> before, long id, Page<Person> after) {
    if (after.stream().anyMatch(person -> person.getId() == id)) {
      return id;
    }
    if (after.isEmpty()) {
      return null;
    }
    int index = before.stream().map(Person::getId).toList().indexOf(id);
    return after.getContent().get(Math.min(Math.max(index, 0), after.getNumberOfElements() - 1)).getId();
  }

  @SuppressWarnings("unchecked")
  private static Page<Person> people(Model model) {
    return (Page<Person>) model.getAttribute("people");
  }

  private Page<Person> find(@Nullable String q, Pageable pageable) {
    String search = q == null ? "" : q.strip();
    Pageable query = stable(sortable(pageable));
    return search.isEmpty()
        ? repository.findAll(query)
        : repository.findByNameContainingIgnoreCaseOrEmailContainingIgnoreCase(search, search, query);
  }

  private String list(String view, String q, Pageable pageable, HtmxRequest htmxRequest, HttpServletRequest request, Model model) {
    String search = q == null ? "" : q.strip();
    Page<Person> people = find(search, pageable);
    model.addAttribute("people", people);
    model.addAttribute("search", search);
    // What a screen reader announces after a search (the form's role="status").
    model.addAttribute("found", search.isEmpty() ? ""
        : people.getTotalElements() == 1 ? "1 person found" : people.getTotalElements() + " people found");
    boolean fragment = HtmxRequests.wantsFragment(htmxRequest);
    model.addAttribute("fragment", fragment);
    // Where the selection form and the row menus post: this page, with its search, order and page.
    model.addAttribute("selfUrl", selfUrl(request));
    // The query string alone, for the row menu's Delete link, so the delete comes back to this page.
    model.addAttribute("query", request.getQueryString() == null ? "" : "?" + request.getQueryString());
    model.addAttribute("roles", Person.Role.values());
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
