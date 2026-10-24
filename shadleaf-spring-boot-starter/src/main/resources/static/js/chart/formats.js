/*
  The named formats of sl:chart's parts (format on the axes, label-format and value-format on the tooltip), applied
  with Intl in the chart's language (the closest lang attribute, <html lang> on most pages).

  Dates come from the server as Jackson writes java.time, and are never handed to the browser's date parser, which
  reads a date-only string as UTC midnight and one without an offset as local time:
  - "2024-04-01" (LocalDate) and "2024-04" (YearMonth) are calendar dates, formatted in UTC so they never shift;
  - "2024-04-01T10:00:00" (LocalDateTime) is a wall-clock time, shown as written, also through UTC;
  - "2024-04-01T10:00:00Z" or "...+02:00" (Instant, OffsetDateTime, ZonedDateTime) is an instant, formatted in the
    chart's time zone (its time-zone attribute: the request's zone, unless the template set one).
  A value that is not of the format's kind (a date format on a number, a number format on text) is shown as it is.
*/

const NUMBER_FORMATS = {
  number: {},
  integer: {maximumFractionDigits: 0},
  compact: {notation: 'compact'},
  percent: {style: 'percent'},
  currency: {style: 'currency'}
};

const DATE_FORMATS = {
  day: {month: 'short', day: 'numeric'},
  'month-short': {month: 'short'},
  month: {month: 'long'},
  'month-year': {month: 'short', year: 'numeric'},
  date: {dateStyle: 'medium'},
  time: {timeStyle: 'short'}
};

// Year, month, then optionally day, time, fraction (any number of digits: Jackson writes nanoseconds) and offset.
const ISO_DATE = /^(\d{4})-(\d{2})(?:-(\d{2})(?:T(\d{2}):(\d{2})(?::(\d{2})(?:[.,](\d+))?)?(Z|[+-]\d{2}(?::?\d{2})?)?)?)?$/;

/** The point in time an ISO string stands for, and whether it is an instant (has an offset); null for anything else. */
export function parseDate(value) {
  const match = typeof value === 'string' ? ISO_DATE.exec(value) : null;
  if (!match) {
    return null;
  }
  const [, year, month, day = '1', hour = '0', minute = '0', second = '0', fraction = '', offset] = match;
  let time = Date.UTC(Number(year), Number(month) - 1, Number(day), Number(hour), Number(minute), Number(second),
      Number(fraction.slice(0, 3).padEnd(3, '0')));
  if (offset && offset !== 'Z') {
    const sign = offset.startsWith('-') ? -1 : 1;
    const digits = offset.slice(1).replace(':', '');
    time -= sign * (Number(digits.slice(0, 2)) * 60 + Number(digits.slice(2) || 0)) * 60_000;
  }
  return {date: new Date(time), instant: offset !== undefined};
}

const formatters = new Map();

function cached(key, create) {
  let formatter = formatters.get(key);
  if (!formatter) {
    formatter = create();
    formatters.set(key, formatter);
  }
  return formatter;
}

function dateTimeFormat(locale, options, timeZone) {
  return cached(JSON.stringify([locale, options, timeZone]), () => {
    try {
      return new Intl.DateTimeFormat(locale, {...options, timeZone});
    } catch {
      // A zone this browser does not know: the browser's own.
      return new Intl.DateTimeFormat(locale, options);
    }
  });
}

/**
 * A function that formats a value with the named format, or shows it as it is when there is no format. The context
 * holds the chart's locale and timeZone, and the part's currency (an ISO 4217 code, for the currency format).
 */
export function formatter(format, {locale, timeZone, currency}) {
  if (!format) {
    return value => value;
  }
  if (format in NUMBER_FORMATS) {
    const options = format === 'currency' ? {...NUMBER_FORMATS.currency, currency} : NUMBER_FORMATS[format];
    const numberFormat = cached(JSON.stringify([locale, options]), () => new Intl.NumberFormat(locale, options));
    return value => typeof value === 'number' ? numberFormat.format(value) : value;
  }
  const options = DATE_FORMATS[format];
  if (!options) {
    return value => value;
  }
  return value => {
    const parsed = parseDate(value);
    if (!parsed) {
      return value;
    }
    return dateTimeFormat(locale, options, parsed.instant ? timeZone : 'UTC').format(parsed.date);
  };
}
