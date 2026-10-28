# Month view

> A month rendered on the server, with content of your own per day and a link per day; no picking.

Source: https://shadleaf.dev/1.1.0-SNAPSHOT/components/month-view/

**Example: Month view.** Each day shows its events from the model and links to the day's agenda.

```html
<sl:month-view day-href="/agenda/{date}">
  <sl:slot name="day">
    <sl:badge th:each="event : ${events[slDay.date]}" variant="secondary">[[${event}]]</sl:badge>
  </sl:slot>
</sl:month-view>
```

## Content per day

`<sl:month-view>` shows a month to look at, not to pick from: an agenda, a booking overview, orders per day. The server
renders it whole, so it needs no JavaScript. For a date in a form, use the [calendar](https://shadleaf.dev/1.1.0-SNAPSHOT/components/calendar.md) or the
[date picker](https://shadleaf.dev/1.1.0-SNAPSHOT/components/date-picker.md).

The `day` slot is rendered once for every day, and sees that day as `${slDay}`:

| Property | |
| --- | --- |
| `date` | The day as a `LocalDate`, to look it up in your model: `${events[slDay.date]}` |
| `iso` | The same as an ISO string: `2026-10-23` |
| `number` | The day of the month, which the cell shows |
| `label` | The whole date in the page's locale (`Friday, October 23, 2026`), which screen readers get |
| `outside` | Whether the day belongs to the previous or next month |
| `today` | Whether it is today |

Put the data in the model as a map from `LocalDate` to what a day shows, and look the day up in the slot:

```java
@GetMapping("/agenda")
String agenda(Model model) {
  model.addAttribute("events", agenda.eventsByDay()); // Map<LocalDate, List<String>>
  return "agenda";
}
```

```html
<sl:month-view day-href="/agenda/{date}">
  <sl:slot name="day">
    <sl:badge th:each="event : ${events[slDay.date]}" variant="secondary">[[${event}]]</sl:badge>
  </sl:slot>
</sl:month-view>
```

The seven days share the width. Content wider than its day is cut off, so keep it short: a badge, a count, a word.

## Links to a day

`day-href` makes every day a link. `{date}` in it is replaced by the day's ISO date: `/agenda/{date}` links to
`/agenda/2026-10-23`, `?date={date}` to the same page with a parameter. The result is a Thymeleaf link, so a path that
starts with `/` gets the context path.

The link holds the day's number, its whole date for screen readers, and the slot's content. So a screen reader reads
"Friday, October 23, 2026, Demo". Put nothing interactive in the slot then: a link cannot hold a button or another
link. Today's day is marked `aria-current="date"`.

Without `day-href`, the days are no links.

## The month shown

The previous and next buttons are links to the same page with `?month=2026-09`; every other parameter of the request
is kept, so a filter (`?team=ops`) stays. Without a `month` attribute, `<sl:month-view>` reads that parameter itself,
so the controller needs nothing for it. A value that is no month falls back to the current month.

To read it in the controller, take a `YearMonth` (Spring converts `2026-10`) and pass it on:

```java
@GetMapping("/agenda")
String agenda(@RequestParam(required = false) YearMonth month, Model model) {
  YearMonth shown = month != null ? month : YearMonth.now();
  model.addAttribute("month", shown);
  model.addAttribute("events", agenda.eventsByDay(shown));
  return "agenda";
}
```

```html
<sl:month-view th:month="${month}">...</sl:month-view>
```

`month` takes a `YearMonth`, a `LocalDate` in the month, or the text `2026-10`. `month-param` renames the parameter,
for a second month view on the page.

Today is marked; by default that is the server's date. If your users are in another time zone, put their date in the
model (`LocalDate.now(zone)`) and pass it: `th:today="${today}"`.

## Size of the navigation

`nav-size` sets the size of the previous and next links, with the sizes of an icon [`<sl:button>`](https://shadleaf.dev/1.1.0-SNAPSHOT/components/button.md#sizes)
(`icon-xs`, `icon-sm`, `icon`, `icon-lg`), so they can match the buttons elsewhere on the page. Without it they have the
size of the skin's calendar. On a touch screen the links have a hit area of at least 44 by 44 pixels whatever their
size, as the icon buttons.

**Example: Size of the navigation.** The previous and next links take the size of an icon button, here icon-lg, to match the buttons around the month. On a touch screen their hit area is at least 44 by 44 pixels whatever they look like.

```html
<sl:month-view nav-size="icon-lg"/>
```

## Week numbers and the first day of the week

As for the [calendar](https://shadleaf.dev/1.1.0-SNAPSHOT/components/calendar.md#week-numbers-and-the-first-day-of-the-week), the page's locale decides the first day
of the week and how weeks are numbered, and the names of the month and the days. `week-starts-on` overrides the first
day: 0 is Sunday, 1 Monday, up to 6. `show-week-number` adds a column with the week numbers.
`show-outside-days="false"` leaves the days of the previous and next month empty.

**Example: Counts and week numbers.** A count per day, no links. Monday first, with the week numbers; the days of the other months left out.

```html
<sl:month-view show-outside-days="false" show-week-number week-starts-on="1">
  <sl:slot name="day">
    <span th:if="${orders[slDay.date]}">[[${orders[slDay.date]}]] orders</span>
  </sl:slot>
</sl:month-view>
```

## Without content

**Example: Without content.** Without a day slot or day-href, the month only shows its dates.

```html
<sl:month-view/>
```

## With htmx

The month links are plain links, so `hx-boost` on the page, or `hx-get` with `hx-target` and `hx-select` on a
container around the month view, swaps the next month in. The controller renders the same page; nothing in the month
view needs JavaScript.

## Accessibility

- The month is a `<table>`, named by its caption (`October 2026`). The column headers carry the whole name of the day
  (`Monday`), the week numbers are row headers.
- Each day says its whole date, then its content.
- It is no ARIA grid: the arrow keys do not move between days. Tab goes through the previous and next links, then
  through the days that are links.

## Attributes

Attributes of `<sl:month-view>`: A month to look at, rendered on the server: each day shows content of its own (a badge, a short text, a count) from the `day` slot, which is rendered once per day and sees the day as `${slDay}` (`date`, a `LocalDate`, `iso`, `number`, `label`, `outside`, `today`), and can link to a page for that day. Nothing is picked: for a date in a form, use `sl:calendar`. The previous and next month are links to the same page with `?month=2026-09`, every other parameter kept, so it needs no JavaScript and works with htmx.

| Attribute | Values | Default | Description |
| --- | --- | --- | --- |
| `month` | *string* | — | The month shown, as `2026-10` (a `YearMonth`) or a date in it. By default the month the request asks for in `month-param`, or else the current month. |
| `month-param` | *string* | `month` | The request parameter the links to the previous and next month set, and that gives the month when `month` is not set. |
| `day-href` | *string* | — | Makes every day a link: `{date}` is replaced by the day's ISO date (`/agenda/{date}`, `?date={date}`), and the result is a Thymeleaf link, so a path starting with `/` gets the context path. The `day` slot is then inside the link: put nothing interactive in it. |
| `today` | *string* | — | Today, as `2026-10-23` (a `LocalDate`), marked as the current date. By default the server's date; pass it in the user's time zone when that differs. |
| `show-outside-days` | *boolean* | `true` | Show the days of the previous and next month that fill the first and last week. |
| `show-week-number` | *boolean* | — | Show the week numbers, counted as the page's locale counts them (ISO weeks in most of Europe). |
| `nav-size` | `default`, `icon-xs`, `icon-sm`, `icon`, `icon-lg` | `default` | Size of the previous and next month links, the sizes of an icon `sl:button`, to match the buttons elsewhere on the page. By default the skin's calendar size. On a touch screen the link is always at least 44 by 44 pixels to hit, whatever it looks like. |
| `week-starts-on` | *number* | — | The first day of the week: 0 is Sunday, 1 Monday, up to 6. By default the page's locale decides. |

Every prop can also be set from an expression: `th:month="${...}"`. Any other attribute passes through to the rendered element, and `class` is appended.
