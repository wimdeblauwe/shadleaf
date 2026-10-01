package io.github.wimdeblauwe.shadleaf.paging;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.jspecify.annotations.Nullable;

/**
 * The path and query string of the request that renders a page, from which a link to the same page with other
 * parameters is built. Parameters it does not change keep their place and their encoding, so a filter such as
 * {@code ?q=ada%20l} survives every sort and page link as the browser sent it.
 */
final class RequestQuery {

  private final String path;
  /** The parameters, each as it appeared in the query string ({@code q=ada%20l}). */
  private final List<String> parameters;

  private RequestQuery(String path, List<String> parameters) {
    this.path = path;
    this.parameters = parameters;
  }

  /**
   * @param path        the path the links start with, context path included; empty for links relative to the page
   * @param queryString the raw query string, without {@code ?}
   */
  static RequestQuery of(String path, @Nullable String queryString) {
    List<String> parameters = queryString == null || queryString.isEmpty()
        ? List.of()
        : Arrays.stream(queryString.split("&")).filter(parameter -> !parameter.isEmpty()).toList();
    return new RequestQuery(path, parameters);
  }

  /** The decoded values of a parameter, in the order they appear. */
  List<String> values(String name) {
    List<String> values = new ArrayList<>();
    for (String parameter : parameters) {
      int equals = parameter.indexOf('=');
      String parameterName = decode(equals < 0 ? parameter : parameter.substring(0, equals));
      if (parameterName.equals(name)) {
        values.add(equals < 0 ? "" : decode(parameter.substring(equals + 1)));
      }
    }
    return values;
  }

  /** The same query without any of these parameters. */
  RequestQuery without(String... names) {
    Set<String> removed = Set.of(names);
    List<String> kept = parameters.stream()
        .filter(parameter -> {
          int equals = parameter.indexOf('=');
          return !removed.contains(decode(equals < 0 ? parameter : parameter.substring(0, equals)));
        })
        .toList();
    return new RequestQuery(path, kept);
  }

  /** The same query with this parameter added at the end. */
  RequestQuery with(String name, String value) {
    List<String> added = new ArrayList<>(parameters);
    added.add(encode(name) + "=" + encode(value));
    return new RequestQuery(path, added);
  }

  /** The link: the path, then {@code ?} and the parameters if there are any. */
  String href() {
    return parameters.isEmpty() ? path : path + "?" + String.join("&", parameters);
  }

  private static String decode(String text) {
    try {
      return URLDecoder.decode(text, StandardCharsets.UTF_8);
    } catch (IllegalArgumentException e) {
      // A malformed escape in a hand-edited URL: compare it as written.
      return text;
    }
  }

  /** Form encoding, with spaces as {@code %20} and the comma of {@code name,desc} left readable. */
  private static String encode(String text) {
    return URLEncoder.encode(text, StandardCharsets.UTF_8).replace("+", "%20").replace("%2C", ",");
  }
}
