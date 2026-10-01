package io.github.wimdeblauwe.shadleaf.paging;

/** One choice of {@code sl:pagination-size}: a page size and the link that asks for it. */
public final class SizeLink {

  private final int size;
  private final String href;
  private final boolean checked;

  SizeLink(int size, String href, boolean checked) {
    this.size = size;
    this.href = href;
    this.checked = checked;
  }

  /** The rows per page. */
  public int getSize() {
    return size;
  }

  /**
   * The same request with this size, every other parameter kept except the page number: the rows the user was
   * looking at would land on another page anyway, so it starts on the first.
   */
  public String getHref() {
    return href;
  }

  /** Whether it is the size of the page shown now. */
  public boolean isChecked() {
    return checked;
  }
}
