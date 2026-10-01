package io.github.wimdeblauwe.shadleaf.paging;

import org.jspecify.annotations.Nullable;

/**
 * Where a page is in the results: what the pagination, its summary and its size menu need from a Spring Data
 * {@code Page} or {@code Slice}, or from the numbers an application passes without Spring Data.
 *
 * @param number           the page, zero-based as Spring Data counts
 * @param size             the rows per page; {@code null} when only the page numbers are known
 * @param numberOfElements the rows on this page; {@code null} when only the page numbers are known
 * @param totalPages       the number of pages; {@code null} for a {@code Slice}, which does not know it
 * @param totalElements    the rows on every page together; {@code null} when unknown
 * @param hasNext          whether there is a page after this one
 */
record PageState(int number, @Nullable Integer size, @Nullable Integer numberOfElements,
                 @Nullable Integer totalPages, @Nullable Long totalElements, boolean hasNext) {
}
