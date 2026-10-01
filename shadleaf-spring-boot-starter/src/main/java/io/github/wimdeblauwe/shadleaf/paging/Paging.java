package io.github.wimdeblauwe.shadleaf.paging;

import io.github.wimdeblauwe.shadleaf.component.ShadleafComponentException;
import java.util.Locale;
import java.util.function.UnaryOperator;
import org.jspecify.annotations.Nullable;
import org.thymeleaf.context.IExpressionContext;
import org.thymeleaf.context.IWebContext;
import org.thymeleaf.web.IWebExchange;
import org.thymeleaf.web.IWebRequest;

/**
 * The expression object {@code #slPaging}: the links of {@code sl:table-head} (and, later, the pagination), built
 * from the request that renders the page.
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
    if (!SpringDataPages.PRESENT || !SpringDataPages.isSlice(page)) {
      throw new ShadleafComponentException("th:page takes a Spring Data Page or Slice, not a "
          + page.getClass().getName() + ".");
    }
    return SpringDataPages.firstOrder(page);
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
