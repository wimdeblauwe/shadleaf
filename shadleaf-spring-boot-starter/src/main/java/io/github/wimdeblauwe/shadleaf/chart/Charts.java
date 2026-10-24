package io.github.wimdeblauwe.shadleaf.chart;

import io.github.wimdeblauwe.shadleaf.component.ShadleafComponentException;
import java.time.DateTimeException;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Currency;
import java.util.List;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;
import org.springframework.context.i18n.LocaleContextHolder;

/**
 * The expression object {@code #slCharts}: what {@code sl:chart} and its parts check and compute on the server.
 * <p>
 * A series key becomes a CSS custom property ({@code --color-desktop}), so it must be a CSS identifier. Instants in the
 * data are formatted in the request's time zone ({@link LocaleContextHolder#getTimeZone()}, which Spring's
 * {@code LocaleResolver} sets when it knows one, the JVM's otherwise), unless the chart names one.
 */
public final class Charts {

  /** An identifier that needs no escaping in CSS: no leading digit or hyphen. */
  private static final Pattern KEY = Pattern.compile("[A-Za-z_][A-Za-z0-9_-]*");
  private static final String NUMBER = "\\d+(?:\\.\\d+)?";
  private static final Pattern RADIUS = Pattern.compile(NUMBER + "|" + NUMBER + "(?:," + NUMBER + "){3}");

  /** The rows of a chart's data as a list, which the data block serialises as a JSON array. */
  public List<?> rows(Object data) {
    if (data instanceof Collection<?> collection) {
      return collection instanceof List<?> list ? list : new ArrayList<>(collection);
    }
    if (data instanceof Object[] array) {
      return Arrays.asList(array);
    }
    if (data instanceof Iterable<?> iterable) {
      // A Spring Data Page or Slice: its rows, not the page object.
      List<Object> rows = new ArrayList<>();
      iterable.forEach(rows::add);
      return rows;
    }
    throw new ShadleafComponentException("<sl:chart> takes its data as a list of rows (objects or maps), not a %s."
        .formatted(data.getClass().getName()));
  }

  /** The key of a series or an axis, checked to be a CSS identifier. */
  public String key(String part, String key) {
    return key(part, "key", key);
  }

  /** A key in another attribute of a part ({@code label-key}), checked as {@link #key(String, String)}. */
  public String key(String part, String attribute, String key) {
    if (!KEY.matcher(key).matches()) {
      throw new ShadleafComponentException(("<sl:%s %s=\"%s\">: a key is a property name of the rows (letters, "
          + "digits, _ and -, not starting with a digit or -), as it becomes the CSS property --color-<key>.")
          .formatted(part, attribute, key));
    }
    return key;
  }

  /**
   * The IANA id of the zone a chart formats instants in: the given one, checked, or the request's. A fixed offset is
   * written as {@code Intl} reads it ({@code +02:00}), and UTC as {@code UTC}.
   */
  public String timeZone(@Nullable String zone) {
    ZoneId zoneId;
    if (zone == null) {
      zoneId = LocaleContextHolder.getTimeZone().toZoneId();
    } else {
      try {
        zoneId = ZoneId.of(zone);
      } catch (DateTimeException e) {
        throw new ShadleafComponentException("<sl:chart time-zone=\"%s\">: not a time zone, use an IANA id such as "
            .formatted(zone) + "Europe/Brussels.", e);
      }
    }
    ZoneId normalized = zoneId.normalized();
    if (normalized instanceof ZoneOffset offset) {
      return offset.getTotalSeconds() == 0 ? "UTC" : offset.getId();
    }
    return zoneId.getId();
  }

  /** A bar's corner radius: one number of pixels, or four (top-left, top-right, bottom-right, bottom-left). */
  public @Nullable String radius(@Nullable String radius) {
    if (radius == null) {
      return null;
    }
    String compact = radius.replaceAll("\\s+", "");
    if (!RADIUS.matcher(compact).matches()) {
      throw new ShadleafComponentException(("<sl:chart-bar radius=\"%s\">: one number of pixels (4), or four for the "
          + "corners from the top-left (4,4,0,0).").formatted(radius));
    }
    return compact;
  }

  /** The currency of a part's {@code currency} format: required by that format, and an ISO 4217 code. */
  public @Nullable String currency(String part, @Nullable String format, @Nullable String currency) {
    if (currency == null) {
      if ("currency".equals(format)) {
        throw new ShadleafComponentException("<sl:%s> formats as currency: add currency=\"EUR\" (an ISO 4217 code)."
            .formatted(part));
      }
      return null;
    }
    try {
      return Currency.getInstance(currency).getCurrencyCode();
    } catch (IllegalArgumentException e) {
      throw new ShadleafComponentException("<sl:%s currency=\"%s\">: not an ISO 4217 currency code, such as EUR."
          .formatted(part, currency), e);
    }
  }
}
