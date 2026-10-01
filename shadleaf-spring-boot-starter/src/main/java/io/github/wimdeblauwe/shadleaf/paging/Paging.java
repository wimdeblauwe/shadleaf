package io.github.wimdeblauwe.shadleaf.paging;

import io.github.wimdeblauwe.shadleaf.component.ShadleafComponentException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.UnaryOperator;
import org.jspecify.annotations.Nullable;
import org.thymeleaf.context.IExpressionContext;
import org.thymeleaf.context.IWebContext;
import org.thymeleaf.web.IWebExchange;
import org.thymeleaf.web.IWebRequest;

/**
 * The expression object {@code #slPaging}: the links of {@code sl:table-head}, {@code sl:pagination} and
 * {@code sl:pagination-size}, built from the request that renders the page, the numbers of
 * {@code sl:pagination-summary}, and the parameters {@code sl:query-params} keeps in a search form.
 * <p>
 * A link replaces only its own parameters and keeps every other one, so a filter in the query string survives
 * sorting and paging. Parameter names are Spring Data's, as the application configured them
 * ({@link PagingParameters}), with a {@code @Qualifier} for a second table on the page. The state (how the page is
 * sorted now) comes from a Spring Data {@code Page} or {@code Slice} when the template has one; without it, from the
 * request's own parameters, so a table sorts without Spring Data too.
 * <p>
 * Links start with the request's path, context path included, and pass through the exchange's
 * {@code transformURL}, as {@code @{...}} links do. Outside a web request they are relative to the page
 * ({@code ?sort=name,asc}).
 */
public final class Paging {

  private static final String ASCENDING = "asc";
  private static final String DESCENDING = "desc";

  private final PagingParameters parameters;
  private final RequestQuery query;
  private final UnaryOperator<String> urlTransformer;

  public Paging(IExpressionContext context, PagingParameters parameters) {
    this.parameters = parameters;
    if (context instanceof IWebContext webContext) {
      IWebExchange exchange = webContext.getExchange();
      IWebRequest request = exchange.getRequest();
      this.query = RequestQuery.of(request.getApplicationPath() + request.getPathWithinApplication(),
          request.getQueryString());
      this.urlTransformer = exchange::transformURL;
    } else {
      this.query = RequestQuery.of("", null);
      this.urlTransformer = UnaryOperator.identity();
    }
  }

  /**
   * The sort link of a column header.
   *
   * @param page      the {@code Page} or {@code Slice} the table shows, whose {@code Sort} says how it is sorted
   *                  now; {@code null} to read that from the request's sort parameter
   * @param property  the property the column sorts by, as the {@code Sort} names it ({@code name},
   *                  {@code address.city})
   * @param qualifier the {@code @Qualifier} of the controller's {@code Pageable}, for a second table on the page
   */
  public SortLink sort(@Nullable Object page, String property, @Nullable String qualifier) {
    if (property == null || property.isBlank()) {
      throw new ShadleafComponentException("A sort link needs the property to sort by, e.g. sort=\"name\".");
    }
    String sortParameter = parameters.sort(qualifier);
    SortOrder current = currentOrder(page, sortParameter);
    String direction = current != null && current.property().equals(property)
        ? (current.descending() ? DESCENDING : ASCENDING)
        : null;
    String next = ASCENDING.equals(direction) ? DESCENDING : ASCENDING;
    String href = query.without(sortParameter, parameters.page(qualifier))
        .with(sortParameter, property + "," + next)
        .href();
    return new SortLink(urlTransformer.apply(href), id(qualifier, "sort-" + property), direction);
  }

  private @Nullable SortOrder currentOrder(@Nullable Object page, String sortParameter) {
    if (page == null) {
      return requestOrder(sortParameter);
    }
    requireSlice(page);
    return SpringDataPages.firstOrder(page);
  }

  /**
   * The links of a pagination: Previous, the page numbers, Next.
   *
   * @param page      the {@code Page} or {@code Slice} shown now; a {@code Slice} gets Previous and Next only
   * @param current   without a {@code page}: the page shown now, one-based
   * @param total     without a {@code page}: the number of pages
   * @param siblings  how many pages to show on each side of the current one ({@code 1} when {@code null})
   * @param qualifier the {@code @Qualifier} of the controller's {@code Pageable}, for a second table on the page
   */
  public PageLinks pages(@Nullable Object page, @Nullable Number current, @Nullable Number total,
      @Nullable Number siblings, @Nullable String qualifier) {
    PageState state = state(page, current, total);
    int shown = state.number() + 1;
    // An empty result still has a page: the one saying so.
    Integer totalPages = state.totalPages() == null ? null : Math.max(state.totalPages(), 1);
    // Past the end (a hand-edited ?page=99), Previous leads back to the last page.
    PageLink previous = shown > 1
        ? pageLink(totalPages == null ? shown - 1 : Math.min(shown - 1, totalPages), "previous", false, qualifier)
        : PageLink.page(shown - 1, null, id(qualifier, "page-previous"), false);
    PageLink next = state.hasNext()
        ? pageLink(shown + 1, "next", false, qualifier)
        : PageLink.page(shown + 1, null, id(qualifier, "page-next"), false);
    List<PageLink> items = new ArrayList<>();
    if (totalPages != null) {
      int around = siblings == null ? 1 : Math.max(siblings.intValue(), 0);
      TreeSet<Integer> numbers = new TreeSet<>(List.of(1, totalPages));
      for (int number = Math.max(shown - around, 1); number <= Math.min(shown + around, totalPages); number++) {
        numbers.add(number);
      }
      Integer before = null;
      for (int number : numbers) {
        if (before != null && number - before == 2) {
          items.add(pageLink(before + 1, null, false, qualifier));
        } else if (before != null && number - before > 2) {
          items.add(PageLink.ellipsis());
        }
        items.add(pageLink(number, null, number == shown, qualifier));
        before = number;
      }
    }
    return new PageLinks(previous, items, next);
  }

  /**
   * The choices of a page size menu.
   *
   * @param page      the {@code Page} or {@code Slice} shown now, whose size is the checked one
   * @param sizes     the sizes to offer, separated by commas or spaces ({@code 10,20,50})
   * @param qualifier the {@code @Qualifier} of the controller's {@code Pageable}, for a second table on the page
   */
  public SizeLinks sizes(@Nullable Object page, @Nullable String sizes, @Nullable String qualifier) {
    if (page == null) {
      throw new ShadleafComponentException(
          "sl:pagination-size needs the page shown now, e.g. th:page=\"${people}\".");
    }
    requireSlice(page);
    int current = SpringDataPages.state(page).size();
    TreeSet<Integer> offered = new TreeSet<>();
    offered.add(current);
    for (String size : (sizes == null ? "" : sizes).trim().split("[,\\s]+")) {
      if (size.isEmpty()) {
        continue;
      }
      try {
        offered.add(Integer.parseInt(size));
      } catch (NumberFormatException e) {
        throw new ShadleafComponentException("sl:pagination-size: '" + size + "' in sizes=\"" + sizes
            + "\" is no number.");
      }
    }
    String sizeParameter = parameters.size(qualifier);
    List<SizeLink> items = offered.stream()
        .map(size -> new SizeLink(size, urlTransformer.apply(query.without(sizeParameter, parameters.page(qualifier))
            .with(sizeParameter, String.valueOf(size))
            .href()), size == current))
        .toList();
    return new SizeLinks(current, items);
  }

  /**
   * The numbers of a summary such as "11–20 of 270".
   *
   * @param page the {@code Page} or {@code Slice} shown now
   */
  public PageSummary summary(@Nullable Object page) {
    if (page == null) {
      throw new ShadleafComponentException(
          "sl:pagination-summary needs the page shown now, e.g. th:page=\"${people}\".");
    }
    requireSlice(page);
    PageState state = SpringDataPages.state(page);
    long offset = (long) state.number() * state.size();
    int rows = state.numberOfElements();
    return new PageSummary(rows == 0 ? 0 : offset + 1, rows == 0 ? 0 : offset + rows, state.totalElements(),
        state.number() + 1, state.totalPages() == null ? null : Math.max(state.totalPages(), 1));
  }

  /**
   * The parameters {@code sl:query-params} copies into a {@code GET} form, so sending it keeps the order, the page
   * size and the other filters: every parameter of the request, decoded and in its order (a repeated one repeated),
   * except the ones named and the page number, so a new search starts on the first page. Outside a web request there
   * is no query to copy, so the list is empty.
   *
   * @param except    the names the form has fields of its own for, separated by commas or spaces ({@code q})
   * @param qualifier the {@code @Qualifier} of the controller's {@code Pageable}, whose page parameter is dropped
   */
  public List<QueryParam> queryParams(@Nullable String except, @Nullable String qualifier) {
    Set<String> dropped = new HashSet<>();
    dropped.add(parameters.page(qualifier));
    for (String name : (except == null ? "" : except).trim().split("[,\\s]+")) {
      if (!name.isEmpty()) {
        dropped.add(name);
      }
    }
    return query.without(dropped).decoded();
  }

  private PageState state(@Nullable Object page, @Nullable Number current, @Nullable Number total) {
    if (page != null) {
      requireSlice(page);
      return SpringDataPages.state(page);
    }
    if (current == null || total == null) {
      throw new ShadleafComponentException("sl:pagination needs the page shown now: th:page=\"${people}\" with a "
          + "Spring Data Page or Slice, or the numbers current (one-based) and total (the number of pages).");
    }
    int shown = Math.max(current.intValue(), 1);
    int pages = Math.max(total.intValue(), 0);
    return new PageState(shown - 1, null, null, pages, null, shown < pages);
  }

  /** A link to a page, one-based; {@code name} is the id's last part, the number when {@code null}. */
  private PageLink pageLink(int number, @Nullable String name, boolean current, @Nullable String qualifier) {
    String pageParameter = parameters.page(qualifier);
    int value = parameters.oneIndexedParameters() ? number : number - 1;
    String href = query.without(pageParameter).with(pageParameter, String.valueOf(value)).href();
    return PageLink.page(number, urlTransformer.apply(href),
        id(qualifier, "page-" + (name == null ? String.valueOf(number) : name)), current);
  }

  private static void requireSlice(Object page) {
    if (!SpringDataPages.PRESENT || !SpringDataPages.isSlice(page)) {
      throw new ShadleafComponentException("th:page takes a Spring Data Page or Slice, not a "
          + page.getClass().getName() + ".");
    }
  }

  /**
   * The first order in the request's sort parameter, read as Spring Data reads it: {@code name}, {@code name,desc},
   * or several properties before one direction ({@code lastName,firstName,asc}).
   */
  private @Nullable SortOrder requestOrder(String sortParameter) {
    for (String value : query.values(sortParameter)) {
      String[] parts = value.split(",");
      if (parts.length == 0 || parts[0].isBlank()) {
        continue;
      }
      String last = parts[parts.length - 1].trim().toLowerCase(Locale.ROOT);
      boolean descending = parts.length > 1 && last.equals(DESCENDING);
      return new SortOrder(parts[0].trim(), descending);
    }
    return null;
  }

  /** An id for a link, prefixed with the qualifier and limited to characters that need no escaping in CSS. */
  private static String id(@Nullable String qualifier, String name) {
    String id = qualifier == null || qualifier.isEmpty() ? name : qualifier + "-" + name;
    return id.replaceAll("[^A-Za-z0-9_-]", "-");
  }
}
