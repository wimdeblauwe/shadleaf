package io.github.wimdeblauwe.shadleaf.paging;

import org.jspecify.annotations.Nullable;

/**
 * The sort link of one column header, for {@code sl:table-head sort="..."}: where it leads and how the column is
 * sorted now.
 */
public final class SortLink {

  private final String href;
  private final String id;
  private final @Nullable String direction;

  SortLink(String href, String id, @Nullable String direction) {
    this.href = href;
    this.id = id;
    this.direction = direction;
  }

  /**
   * The same page sorted by this column: ascending, or descending when it is sorted ascending now. Every other
   * parameter is kept, except the page number, so the order starts on the first page.
   */
  public String getHref() {
    return href;
  }

  /**
   * The link's id ({@code sort-name}, or {@code members-sort-name} with a qualifier), so htmx can give the focus back
   * to the new link after it swapped the table.
   */
  public String getId() {
    return id;
  }

  /** {@code asc} or {@code desc} when the page is sorted by this column first, otherwise {@code null}. */
  public @Nullable String getDirection() {
    return direction;
  }

  /** The header cell's {@code aria-sort}: {@code ascending} or {@code descending}, only on the sorted column. */
  public @Nullable String getAriaSort() {
    if (direction == null) {
      return null;
    }
    return direction.equals("desc") ? "descending" : "ascending";
  }

  /** The icon after the label: an arrow on the sorted column, both chevrons on the others (as shadcn/ui's). */
  public String getIcon() {
    if (direction == null) {
      return "chevrons-up-down";
    }
    return direction.equals("desc") ? "arrow-down" : "arrow-up";
  }
}
