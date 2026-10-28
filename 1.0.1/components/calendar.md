# Calendar

> shadcn/ui's Calendar over native date inputs, bound with th:field; a date or a range of dates.

Source: https://shadleaf.dev/1.0.1/components/calendar/

**Example: Calendar.** A date in a field; th:field binds the date input the calendar draws over.

```html
<sl:field th:field="*{dueDate}">
  <sl:field-label>Due date</sl:field-label>
  <sl:calendar/>
</sl:field>
```

## Drawn over date inputs

A calendar's value lives in real `<input type="date">` elements. The server renders them, and the `slCalendar` Alpine
component hides them and draws the month in their place. So `th:field` binds a calendar as it binds any input, and
the form submits ISO dates (`2026-10-23`). Without JavaScript, the date inputs show as they are and still work.

```html
<form th:object="${task}" th:action="@{/tasks}" method="post">
  <sl:field th:field="*{dueDate}">
    <sl:field-label>Due date</sl:field-label>
    <sl:calendar/>
    <sl:field-error/>
  </sl:field>
</form>
```

Without content, `<sl:calendar>` renders one date input itself. That input takes every attribute that is not a prop:
`th:field`, `name`, `required`, `min`, `max`, `disabled`, `hx-*`. `class` is the exception: it stays on the calendar.

### LocalDate fields

A date input only takes ISO dates, so the field has to be formatted as one. Do one of these:

- Annotate the field with `@DateTimeFormat(iso = DateTimeFormat.ISO.DATE)`.
- Set `spring.mvc.format.date=iso` for the whole application.

Without either, Spring formats a `LocalDate` for the locale (`10/23/26`), which a date input shows as empty.

## Range

For a range, give the calendar two date inputs as content: the start, then the end. Each input binds its own field.
The calendar's own attributes then stay on the calendar, so give it an `aria-label` that names the range, and give
each input a name of its own.

**Example: Range.** Two date inputs, the start and the end, make a range. Two months show side by side.

```html
<sl:calendar aria-label="Stay" number-of-months="2">
  <sl:input type="date" th:field="*{checkIn}" aria-label="Check-in"/>
  <sl:input type="date" th:field="*{checkOut}" aria-label="Check-out"/>
</sl:calendar>
```

The first pick sets the start. The second sets the end; if it comes before the start, the two swap. While only the
start is picked, the band follows the pointer. A pick on a complete range starts a new one.

`number-of-months` shows several months side by side; on a narrow screen they stack.

## Month and year selects

`caption-layout="dropdown"` replaces the month's name with a select for the month and one for the year. This helps
for dates far from today, such as a date of birth. `dropdown-months` and `dropdown-years` show only one of the two.

The years run from `start-month` to `end-month`. By default those are the months of the inputs' `min` and `max`, or
else 100 years back and 10 years ahead. Both also stop the previous and next buttons.

**Example: Month and year selects.** caption-layout="dropdown" for dates far from today, such as a date of birth.

```html
<sl:field th:field="*{birthDate}">
  <sl:field-label>Date of birth</sl:field-label>
  <sl:calendar caption-layout="dropdown" start-month="1926-01" end-month="2026-12"/>
</sl:field>
```

## Disabled dates

The inputs' `min` and `max` disable the days outside them. `disabled-dates` disables more, comma-separated:

- a date: `2026-12-25`;
- a range: `2026-12-24..2026-12-31`;
- a day of the week: `sat`, `sun`.

From a list in the model, join it: `th:disabled-dates="${#strings.listJoin(booked, ',')}"`. A `LocalDate` prints as
an ISO date.

**Example: Disabled dates.** Weekends and the booked days cannot be picked; neither can the days outside min and max.

```html
<sl:field th:field="*{meetingDate}">
  <sl:field-label>Meeting</sl:field-label>
  <sl:calendar min="2026-10-05" max="2026-11-27" disabled-dates="sat,sun,2026-10-14..2026-10-16,2026-10-28"/>
</sl:field>
```

A disabled day still takes the focus, so the arrow keys move across it, but it cannot be picked. Only the calendar
knows about `disabled-dates`; a date input does not. So check the date on the server too, as you would any input.

## Week numbers and the first day of the week

The page's locale (Spring's `LocaleResolver`) decides the first day of the week and how weeks are numbered. In most
of Europe weeks start on Monday and are ISO weeks; in the United States they start on Sunday, and the week of
1 January is week 1. `week-starts-on` overrides the first day: 0 is Sunday, 1 Monday, up to 6. `show-week-number`
adds a column with the week numbers.

The names of the months and days come from the browser (`Intl`), in the page's language.

**Example: Week numbers.** The week numbers, and Monday as the first day of the week, as the locale counts them unless week-starts-on says otherwise.

```html
<sl:field th:field="*{sprintStart}">
  <sl:field-label>Sprint start</sl:field-label>
  <sl:calendar show-week-number week-starts-on="1"/>
</sl:field>
```

## In a form

- **`th:field`**, on the calendar or on its [field](https://shadleaf.dev/1.0.1/components/field.md), binds the input. In a field, the input also takes the
  field's `id` (the label points at it) and `aria-describedby`. The calendar takes the label of its input as its own
  name.
- **Errors.** A field with errors marks the input `aria-invalid="true"`, and the field's error describes the calendar.
- **Events.** A pick fires `input` and `change` on the input, so `hx-trigger="change"` and your own listeners work as
  on a date input. The month and year selects fire nothing outside the calendar.
- **A value set by a script** shows once the script fires `change` on the input, as it should for any input.
- **Form reset** puts the inputs back to their rendered values, and the calendar shows them.
- **A click on the label**, `autofocus` or the browser's validation focus the input; the calendar passes the focus on
  to the day that takes it.

**Example: With errors.** In a field with errors, the input is aria-invalid and the calendar is described by the message.

```html
<sl:field th:field="*{deliveryDate}">
  <sl:field-label>Delivery</sl:field-label>
  <sl:calendar default-month="2026-10"/>
  <sl:field-error/>
</sl:field>
```

## Disabled and read-only

`disabled` on the input, or on a `<sl:field-set>` around it, disables the calendar: no day takes the focus.
`readonly` keeps the navigation but stops the picking.

**Example: Disabled.**

```html
<sl:calendar name="closedDate" aria-label="Closed on" value="2026-10-23" disabled/>
```

## Keyboard

The previous and next buttons are tab stops, and the month is one more. Within the month:

| Key | Moves the focus to |
| --- | --- |
| Arrow left, arrow right | The previous, next day (swapped on a right-to-left page) |
| Arrow up, arrow down | The same day a week earlier, later |
| Home, End | The first, last day of the week |
| Page Up, Page Down | The same day a month earlier, later (or the month's last day when it is shorter) |
| Shift + Page Up, Shift + Page Down | The same day a year earlier, later |
| Enter, Space | Picks the day |

When the focus moves past the months that show, the calendar shows the next ones. A change of month is announced.

## With htmx

- Back restores the calendar as htmx saved it, and `slCalendar` draws it again.
- `hx-trigger="change"` on the calendar's input (or on the form) sends the picked date.

## Left out

These react-day-picker props, which shadcn's Calendar passes on, have no counterpart, on purpose:

- **`mode`.** The inputs decide: one is a date, two are a range. `mode="multiple"` has no counterpart yet.
- **`selected` and `onSelect`.** The inputs are the value: `th:field` renders it, and the input's `change` event
  reports a new one.
- **`disabled` with matchers.** `disabled` disables the whole calendar, as on any input; the days go in
  `disabled-dates`.
- **`timeZone`.** A date input holds a date without a time, so there is no time zone to get wrong.
- **`buttonVariant`, `classNames`, `components` and `formatters`.** The skins draw the calendar; the locale formats it.

For a button that opens the calendar in a popover, see the [date picker](https://shadleaf.dev/1.0.1/components/date-picker.md).

## Attributes

Attributes of `<sl:calendar>`: A month calendar to pick a date or a range of dates, drawn over native date inputs: the inputs are its value, so `th:field` binds it and the form submits ISO dates (`2026-10-23`). Without content, it renders one date input itself, which takes every attribute that is not a prop except `class` (`th:field`, `name`, `required`, `min`, `max`), and in a `sl:field` the field's `th:field`, id and `aria-describedby`. For a range, give it two `sl:input type="date"`s as content, the start and the end; its attributes then go to the calendar. `slCalendar` hides the inputs and draws the month; without JavaScript the inputs show as they are.

| Attribute | Values | Default | Description |
| --- | --- | --- | --- |
| `caption-layout` | `label`, `dropdown`, `dropdown-months`, `dropdown-years` | `label` | The month's caption: its name, or selects for the month and year, the month, or the year. |
| `number-of-months` | *number* | `1` | How many months show side by side (stacked on a narrow screen). |
| `show-outside-days` | *boolean* | `true` | Show the days of the previous and next month that fill the first and last week. |
| `show-week-number` | *boolean* | — | Show the week numbers, counted as the page's locale counts them (ISO weeks in most of Europe). |
| `week-starts-on` | *number* | — | The first day of the week: 0 is Sunday, 1 Monday, up to 6. By default the page's locale decides. |
| `default-month` | *string* | — | The month shown first, as `2026-10`. By default the month of the chosen date (or of the start of the range), or else the current month. |
| `start-month` | *string* | — | The first month the calendar navigates to, as `2026-01`; also the first year of the year select. By default the month of the inputs' `min`, or else 100 years ago. |
| `end-month` | *string* | — | The last month the calendar navigates to, as `2026-12`; also the last year of the year select. By default the month of the inputs' `max`, or else 10 years from now. |
| `disabled-dates` | *string* | — | Days that cannot be picked, comma-separated: a date (`2026-12-25`), a range (`2026-12-24..2026-12-31`) or a day of the week (`sat`, `sun`). The inputs' `min` and `max` disable the days outside them too. Only the calendar knows: check the date on the server as well. |

Every prop can also be set from an expression: `th:caption-layout="${...}"`. Any other attribute passes through to the rendered element, and `class` is appended.

## JavaScript

`slCalendar` sits on the calendar: `<div class="calendar" x-data="slCalendar">`.

- **Without Alpine the date inputs show**, and the form works with them.
- **With it**, the inputs stay in the form, hidden, and still submit, reset and validate. `slCalendar` adds a
  `.calendar-months` element with the navigation and a `<table role="grid">` per month after them.
- **It needs no Alpine plugin.** If you load Alpine yourself (`shadleaf.assets.alpine=external`), plain Alpine is
  enough.
- **Keep your own `x-data` off `<sl:calendar>`**: an element has one `x-data`. Listen for `change` on the input
  instead.
