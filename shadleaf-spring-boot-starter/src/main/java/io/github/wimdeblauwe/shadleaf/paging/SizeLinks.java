package io.github.wimdeblauwe.shadleaf.paging;

import java.util.List;

/** What {@code sl:pagination-size} renders: the current page size and the sizes to choose from. */
public final class SizeLinks {

  private final int current;
  private final List<SizeLink> items;

  SizeLinks(int current, List<SizeLink> items) {
    this.current = current;
    this.items = List.copyOf(items);
  }

  /** The rows per page now. */
  public int getCurrent() {
    return current;
  }

  /** The sizes offered, smallest first, with the current one among them even when it was not offered. */
  public List<SizeLink> getItems() {
    return items;
  }
}
