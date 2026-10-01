package io.github.wimdeblauwe.shadleaf.nav;

import io.github.wimdeblauwe.shadleaf.component.ShadleafComponentException;
import org.jspecify.annotations.Nullable;
import org.thymeleaf.context.IExpressionContext;
import org.thymeleaf.context.IWebContext;

/**
 * The expression object {@code #slNav}: whether a path is the page being rendered, for the {@code active} prop of
 * {@code sl:sidebar-menu-button}: {@code th:active="${#slNav.current('/people')}"}. An expression object rather than a
 * model attribute, so a layout marks its current page without every controller naming it.
 * <p>
 * The path is the application's own, as in {@code @{/people}}: it is compared with the request's path within the
 * application, so a context path makes no difference. A query string or fragment on either side is ignored, and so is
 * a trailing slash. By default a path is current for its own page and every page below it ({@code /people} for
 * {@code /people/12/edit}, not for {@code /peoples}); the root, {@code /}, only for itself, or it would be current on
 * every page. Outside a web request no path is current.
 */
public final class Navigation {

  private final @Nullable String requestPath;

  public Navigation(IExpressionContext context) {
    this.requestPath = context instanceof IWebContext webContext
        ? normalize(webContext.getExchange().getRequest().getPathWithinApplication())
        : null;
  }

  /** Whether {@code path} is the current page or a page below it; {@code /} only for itself. */
  public boolean current(String path) {
    return current(path, false);
  }

  /**
   * Whether {@code path} is the current page: with {@code exact} only that page, otherwise also any page below it
   * ({@code /} is always exact).
   */
  public boolean current(String path, boolean exact) {
    if (path == null || !path.startsWith("/")) {
      throw new ShadleafComponentException(
          "#slNav.current needs a path within the application that starts with /, e.g. '/people', not '%s'."
              .formatted(path));
    }
    if (requestPath == null) {
      return false;
    }
    String target = normalize(path);
    if (requestPath.equals(target)) {
      return true;
    }
    return !exact && !target.equals("/") && requestPath.startsWith(target + "/");
  }

  /** Without query string, fragment, path parameters ({@code ;jsessionid=...}) and trailing slash. */
  private static String normalize(String path) {
    String result = path;
    for (char end : new char[]{'?', '#', ';'}) {
      int index = result.indexOf(end);
      if (index >= 0) {
        result = result.substring(0, index);
      }
    }
    while (result.length() > 1 && result.endsWith("/")) {
      result = result.substring(0, result.length() - 1);
    }
    return result.isEmpty() ? "/" : result;
  }
}
