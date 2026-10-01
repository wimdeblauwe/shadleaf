package io.github.wimdeblauwe.shadleaf.sidebar;

import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.thymeleaf.context.IExpressionContext;
import org.thymeleaf.context.IWebContext;

/**
 * The expression object {@code #slSidebar}: whether the sidebar of {@code sl:sidebar-provider} is expanded or
 * collapsed on a desktop, read from the {@value #COOKIE_NAME} cookie that {@code slSidebar} writes when the user
 * collapses or expands it. The server renders that state, so the first paint is right without script and the page does
 * not jump, also when htmx swaps the whole body.
 * <p>
 * An expression object rather than a model attribute, so no controller has to read the cookie. Outside a web request
 * there is no cookie, and the state is the default.
 */
public final class SidebarState {

  /** The cookie {@code slSidebar} writes: {@value #EXPANDED} or {@value #COLLAPSED}. */
  public static final String COOKIE_NAME = "sl-sidebar-state";
  public static final String EXPANDED = "expanded";
  public static final String COLLAPSED = "collapsed";
  private static final Set<String> STATES = Set.of(EXPANDED, COLLAPSED);

  private final @Nullable String cookie;

  public SidebarState(IExpressionContext context) {
    this.cookie = context instanceof IWebContext webContext
        ? webContext.getExchange().getRequest().getCookieValue(COOKIE_NAME)
        : null;
  }

  /**
   * The state from the cookie, or {@code defaultState} when there is none (or it holds something else); without a
   * default, {@value #EXPANDED}.
   */
  public String state(@Nullable String defaultState) {
    if (cookie != null && STATES.contains(cookie)) {
      return cookie;
    }
    return defaultState != null && STATES.contains(defaultState) ? defaultState : EXPANDED;
  }
}
