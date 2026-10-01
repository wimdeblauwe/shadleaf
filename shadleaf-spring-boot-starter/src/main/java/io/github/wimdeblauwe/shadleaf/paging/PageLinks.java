package io.github.wimdeblauwe.shadleaf.paging;

import java.util.List;

/**
 * What {@code sl:pagination} renders for a page: Previous, the page numbers with ellipses between, and Next.
 */
public final class PageLinks {

  private final PageLink previous;
  private final List<PageLink> items;
  private final PageLink next;

  PageLinks(PageLink previous, List<PageLink> items, PageLink next) {
    this.previous = previous;
    this.items = List.copyOf(items);
    this.next = next;
  }

  public PageLink getPrevious() {
    return previous;
  }

  /**
   * The first page, the last, the current one with its siblings on each side, and an ellipsis for every gap of more
   * than one page (a gap of one shows that page instead). Empty for a {@code Slice}, which does not know how many
   * pages there are.
   */
  public List<PageLink> getItems() {
    return items;
  }

  public PageLink getNext() {
    return next;
  }
}
