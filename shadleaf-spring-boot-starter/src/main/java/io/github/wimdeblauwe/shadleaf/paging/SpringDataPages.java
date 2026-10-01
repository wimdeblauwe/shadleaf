package io.github.wimdeblauwe.shadleaf.paging;

import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.util.ClassUtils;

/**
 * Everything that touches Spring Data, which is an optional dependency: {@link Paging} only calls in here when
 * {@link #PRESENT} is true, so the class is never loaded without it.
 */
final class SpringDataPages {

  static final boolean PRESENT = ClassUtils.isPresent("org.springframework.data.domain.Slice",
      SpringDataPages.class.getClassLoader());

  private SpringDataPages() {
  }

  /** Whether it is a {@code Page} or a {@code Slice}. */
  static boolean isSlice(Object page) {
    return page instanceof Slice<?>;
  }

  /** The first order of the slice's {@code Sort}, or {@code null} when it is unsorted. */
  static @Nullable SortOrder firstOrder(Object page) {
    Sort sort = ((Slice<?>) page).getSort();
    return sort.stream()
        .findFirst()
        .map(order -> new SortOrder(order.getProperty(), order.isDescending()))
        .orElse(null);
  }

  /** Where the slice is in the results; a {@code Page} also knows the totals. */
  static PageState state(Object page) {
    Slice<?> slice = (Slice<?>) page;
    // An unpaged slice (all rows on one page) gives the number of rows as its size.
    int size = slice.getSize();
    if (slice instanceof Page<?> full) {
      return new PageState(full.getNumber(), size, full.getNumberOfElements(), full.getTotalPages(),
          full.getTotalElements(), full.hasNext());
    }
    return new PageState(slice.getNumber(), size, slice.getNumberOfElements(), null, null, slice.hasNext());
  }
}
