package io.github.wimdeblauwe.shadleaf.paging;

/** One parameter of the request's query string, decoded, as {@code sl:query-params} writes it into a hidden input. */
public final class QueryParam {

  private final String name;
  private final String value;

  QueryParam(String name, String value) {
    this.name = name;
    this.value = value;
  }

  /** The parameter's name. */
  public String getName() {
    return name;
  }

  /** Its value; empty for a parameter without {@code =} ({@code ?flag}). */
  public String getValue() {
    return value;
  }

  @Override
  public String toString() {
    return name + "=" + value;
  }
}
