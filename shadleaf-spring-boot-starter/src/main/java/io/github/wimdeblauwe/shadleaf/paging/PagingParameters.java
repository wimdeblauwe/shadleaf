package io.github.wimdeblauwe.shadleaf.paging;

import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

/**
 * The request parameters Spring Data's {@code Pageable} and {@code Sort} resolvers read, so the links Shadleaf builds
 * ask for what the controller's {@code Pageable} resolves.
 * <p>
 * The auto-configuration reads them from Spring Boot's {@code spring.data.web.*} properties. An application that
 * configures the resolvers in code instead (a {@code PageableHandlerMethodArgumentResolverCustomizer}) declares a
 * bean of this type with the same values.
 *
 * @param pageParameter        the page number ({@code spring.data.web.pageable.page-parameter})
 * @param sizeParameter        the page size ({@code spring.data.web.pageable.size-parameter})
 * @param oneIndexedParameters whether the first page is {@code 1} rather than {@code 0}
 *                             ({@code spring.data.web.pageable.one-indexed-parameters})
 * @param prefix               put before the page and size parameters, not the sort parameter
 *                             ({@code spring.data.web.pageable.prefix})
 * @param qualifierDelimiter   between a {@code @Qualifier} and the page and size parameters
 *                             ({@code spring.data.web.pageable.qualifier-delimiter})
 * @param sortParameter        the order ({@code spring.data.web.sort.sort-parameter})
 */
public record PagingParameters(String pageParameter, String sizeParameter, boolean oneIndexedParameters,
                               String prefix, String qualifierDelimiter, String sortParameter) {

  /**
   * Between a {@code @Qualifier} and the sort parameter. Spring Boot does not configure it, so it is the sort
   * resolver's own default.
   */
  private static final String SORT_QUALIFIER_DELIMITER = "_";

  /** Spring Data's defaults: {@code page}, {@code size}, zero-based, no prefix, {@code _}, {@code sort}. */
  public static PagingParameters defaults() {
    return new PagingParameters("page", "size", false, "", "_", "sort");
  }

  /** The values of Spring Boot's {@code spring.data.web.*} properties, or Spring Data's defaults. */
  public static PagingParameters from(Environment environment) {
    Binder binder = Binder.get(environment);
    PagingParameters defaults = defaults();
    String pageable = "spring.data.web.pageable.";
    return new PagingParameters(
        binder.bind(pageable + "page-parameter", String.class).orElse(defaults.pageParameter()),
        binder.bind(pageable + "size-parameter", String.class).orElse(defaults.sizeParameter()),
        binder.bind(pageable + "one-indexed-parameters", Boolean.class).orElse(defaults.oneIndexedParameters()),
        binder.bind(pageable + "prefix", String.class).orElse(defaults.prefix()),
        binder.bind(pageable + "qualifier-delimiter", String.class).orElse(defaults.qualifierDelimiter()),
        binder.bind("spring.data.web.sort.sort-parameter", String.class).orElse(defaults.sortParameter()));
  }

  /** The page number's parameter, for a {@code Pageable} with this {@code @Qualifier} (or none). */
  public String page(@Nullable String qualifier) {
    return prefix + qualified(qualifier, qualifierDelimiter) + pageParameter;
  }

  /** The page size's parameter, for a {@code Pageable} with this {@code @Qualifier} (or none). */
  public String size(@Nullable String qualifier) {
    return prefix + qualified(qualifier, qualifierDelimiter) + sizeParameter;
  }

  /** The order's parameter, for a {@code Pageable} or {@code Sort} with this {@code @Qualifier} (or none). */
  public String sort(@Nullable String qualifier) {
    return qualified(qualifier, SORT_QUALIFIER_DELIMITER) + sortParameter;
  }

  private static String qualified(@Nullable String qualifier, String delimiter) {
    return StringUtils.hasLength(qualifier) ? qualifier + delimiter : "";
  }
}
