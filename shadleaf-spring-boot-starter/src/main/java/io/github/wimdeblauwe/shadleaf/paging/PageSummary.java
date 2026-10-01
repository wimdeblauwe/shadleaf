package io.github.wimdeblauwe.shadleaf.paging;

import org.jspecify.annotations.Nullable;

/** The numbers {@code sl:pagination-summary} puts in its message. */
public final class PageSummary {

  private final long first;
  private final long last;
  private final @Nullable Long total;
  private final int number;
  private final @Nullable Integer totalPages;

  PageSummary(long first, long last, @Nullable Long total, int number, @Nullable Integer totalPages) {
    this.first = first;
    this.last = last;
    this.total = total;
    this.number = number;
    this.totalPages = totalPages;
  }

  /** The position of the page's first row in all results, one-based; {@code 0} on an empty page. */
  public long getFirst() {
    return first;
  }

  /** The position of the page's last row, one-based; {@code 0} on an empty page. */
  public long getLast() {
    return last;
  }

  /** The rows on every page together; {@code null} for a {@code Slice}. */
  public @Nullable Long getTotal() {
    return total;
  }

  /** The page, one-based. */
  public int getNumber() {
    return number;
  }

  /** The number of pages, at least 1; {@code null} for a {@code Slice}. */
  public @Nullable Integer getTotalPages() {
    return totalPages;
  }

  /** Whether the page has no rows. */
  public boolean isEmpty() {
    return last == 0;
  }
}
