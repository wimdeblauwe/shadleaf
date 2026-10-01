package io.github.wimdeblauwe.shadleaf.paging;

import org.jspecify.annotations.Nullable;

/**
 * One entry of a pagination: Previous, Next, a page number, or the ellipsis that stands for the pages left out.
 */
public final class PageLink {

  private final int number;
  private final @Nullable String href;
  private final @Nullable String id;
  private final boolean current;
  private final boolean ellipsis;

  private PageLink(int number, @Nullable String href, @Nullable String id, boolean current, boolean ellipsis) {
    this.number = number;
    this.href = href;
    this.id = id;
    this.current = current;
    this.ellipsis = ellipsis;
  }

  static PageLink page(int number, @Nullable String href, String id, boolean current) {
    return new PageLink(number, href, id, current, false);
  }

  static PageLink ellipsis() {
    return new PageLink(0, null, null, false, true);
  }

  /** The page it leads to, one-based as the user counts; {@code 0} for the ellipsis. */
  public int getNumber() {
    return number;
  }

  /**
   * The same request asking for that page, with every other parameter (sort, size, filters) kept; {@code null} for
   * Previous on the first page, Next on the last and the ellipsis.
   */
  public @Nullable String getHref() {
    return href;
  }

  /**
   * The link's id ({@code page-3}, {@code page-previous}, {@code page-next}, prefixed with the qualifier), so htmx
   * can give the focus back to the new link after it swapped the page; {@code null} for the ellipsis.
   */
  public @Nullable String getId() {
    return id;
  }

  /** Whether it is the page shown now ({@code aria-current="page"}). */
  public boolean isCurrent() {
    return current;
  }

  /** Whether there is nowhere to go: Previous on the first page, Next on the last. */
  public boolean isDisabled() {
    return href == null && !ellipsis;
  }

  /** Whether it stands for the pages left out between two numbers. */
  public boolean isEllipsis() {
    return ellipsis;
  }
}
