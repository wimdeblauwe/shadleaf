package io.github.wimdeblauwe.shadleaf.paging;

/** The first key a page is sorted by: the only one a table header shows. */
record SortOrder(String property, boolean descending) {
}
