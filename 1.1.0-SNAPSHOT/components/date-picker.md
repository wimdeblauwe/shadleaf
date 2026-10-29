# Date Picker

> A button showing the chosen date that opens a calendar in a popover; a date or a range, bound with th:field.

Source: https://shadleaf.dev/1.1.0-SNAPSHOT/components/date-picker/

**Example: Date picker.** A date in a field. The button shows "Pick a date" until a date is chosen; a pick closes the popover.

```html
<sl:field th:field="*{pickupDate}">
  <sl:field-label>Pickup date</sl:field-label>
  <sl:date-picker/>
</sl:field>
```

## A popover with a calendar

A date picker is a button in a [popover](https://shadleaf.dev/1.1.0-SNAPSHOT/components/popover.md) with a [calendar](https://shadleaf.dev/1.1.0-SNAPSHOT/components/calendar.md): the button shows the chosen date,
and a pick closes the popover. `<sl:date-picker>` is that composition as one component, with the glue in the
`slDatePicker` Alpine component.

The value lives in the calendar's date inputs, so `th:field` binds a date picker as it binds a calendar, and the form
submits ISO dates (`2026-10-23`). The [LocalDate fields](https://shadleaf.dev/1.1.0-SNAPSHOT/components/calendar.md#localdate-fields) note applies here too.

```html
<form th:object="${booking}" th:action="@{/bookings}" method="post">
  <sl:field th:field="*{pickupDate}">
    <sl:field-label>Pickup date</sl:field-label>
    <sl:date-picker/>
    <sl:field-error/>
  </sl:field>
</form>
```

Every attribute that is not a prop goes to the calendar inside. `class` is the exception: it stays on the date picker.
So the calendar's props work on the date picker (`caption-layout`, `number-of-months`, `disabled-dates`, ...), and so
do its input's attributes (`th:field`, `name`, `required`, `min`, `max`, `disabled`, `hx-*`).

## The button's text

The button shows the chosen date, or `placeholder` ("Pick a date") while there is none.

- **On the server**, the date is formatted in the page's locale (Spring's `LocaleResolver`). That is what shows before
  JavaScript runs.
- **In the browser**, `slDatePicker` formats it again with `Intl`, for the same locale, when it starts and after every
  change. Java and `Intl` both use the CLDR's patterns, so the two texts agree.

`format` picks the style, as `Intl.DateTimeFormat`'s `dateStyle` and Java's `FormatStyle`:

| `format` | en-US | nl-BE |
| --- | --- | --- |
| `short` | 10/23/26 | 23/10/2026 |
| `medium` | Oct 23, 2026 | 23 okt 2026 |
| `long` (default) | October 23, 2026 | 23 oktober 2026 |
| `full` | Friday, October 23, 2026 | vrijdag 23 oktober 2026 |

**Example: Format.** format is Intl's dateStyle (Java's FormatStyle on the server); long is the default.

```html
<sl:field-group>
  <sl:field th:field="*{reminderShort}">
    <sl:field-label>Short</sl:field-label>
    <sl:date-picker format="short"/>
  </sl:field>
  <sl:field th:field="*{reminderFull}">
    <sl:field-label>Full</sl:field-label>
    <sl:date-picker format="full"/>
  </sl:field>
</sl:field-group>
```

**Example: With a date.** The server formats the bound date in the page's locale; the browser formats it the same way after a pick.

```html
<sl:field th:field="*{appointmentDate}">
  <sl:field-label>Appointment</sl:field-label>
  <sl:date-picker/>
</sl:field>
```

## Range

For a range, give the date picker two date inputs as content, the start and then the end, and an `aria-label` that
names the range. The popover closes after the second pick. The button shows both ends: `Oct 12, 2026 – Oct 21, 2026`
with `format="medium"` (the separator is the message `sl.date-picker.range`, see [Translating](https://shadleaf.dev/1.1.0-SNAPSHOT/guides/translating.md)).

**Example: Range.** Two date inputs, the start and the end, make a range; the popover closes after the second pick.

```html
<sl:date-picker aria-label="Trip" format="medium" number-of-months="2">
  <sl:input type="date" th:field="*{tripStart}" aria-label="Departure"/>
  <sl:input type="date" th:field="*{tripEnd}" aria-label="Return"/>
</sl:date-picker>
```

## Date of birth

The calendar's month and year selects (`caption-layout="dropdown"`) help with dates far from today.

**Example: Date of birth.** The calendar's props pass through, such as caption-layout="dropdown" for a date far from today.

```html
<sl:field th:field="*{dateOfBirth}">
  <sl:field-label>Date of birth</sl:field-label>
  <sl:date-picker format="medium" placeholder="Select date" caption-layout="dropdown" start-month="1926-01"
                  end-month="2026-12" max="2026-10-23"/>
</sl:field>
```

## In a form

- **The label.** In a [field](https://shadleaf.dev/1.1.0-SNAPSHOT/components/field.md), the label points at the date input inside the popover, as for a calendar.
  The button takes the label's text and the date it shows as its name ("Pickup date, October 23, 2026"), and a click
  on the label focuses the button.
- **Errors.** A field with errors marks the input and the button `aria-invalid="true"`, and the field's error
  describes both.
- **Validation.** The browser can only point at a control it can show. When a `required` date is empty on submit,
  the popover opens, so the browser's message points at the calendar.
- **Events.** A pick fires `input` and `change` on the input, as on a [calendar](https://shadleaf.dev/1.1.0-SNAPSHOT/components/calendar.md#in-a-form).
- **A value set by a script** shows on the button once the script fires `change` on the input.
- **Form reset** puts the button's text back too.

**Example: With errors.** The field's error marks the button aria-invalid and describes it. A required empty date opens the popover on submit.

```html
<sl:field th:field="*{returnDate}">
  <sl:field-label>Return date</sl:field-label>
  <sl:date-picker required default-month="2026-10"/>
  <sl:field-error/>
</sl:field>
```

## Disabled

`disabled` on the input, or on a `<sl:field-set>` around it, disables the button.

**Example: Disabled.**

```html
<sl:date-picker name="archivedOn" aria-label="Archived on" value="2026-10-23" disabled/>
```

## Keyboard

| Key | On | Does |
| --- | --- | --- |
| Enter, Space | The button | Opens the popover, with the focus on the chosen day, or else today |
| Arrows, Home, End, Page Up, Page Down | A day | Move the focus, as in the [calendar](https://shadleaf.dev/1.1.0-SNAPSHOT/components/calendar.md#keyboard) |
| Enter, Space | A day | Picks it, and closes the popover once the date (or both ends of the range) is chosen |
| Escape | The popover | Closes it, and the focus goes back to the button |
| Tab | The popover's last stop | Closes it, as the focus leaves it |

The popover is a non-modal dialog, as [sl:popover](https://shadleaf.dev/1.1.0-SNAPSHOT/components/popover.md) is. The focus is not trapped in it.

## With htmx

- Back restores the date picker as htmx saved it, and `slDatePicker` formats the date again.
- `hx-trigger="change"` on the date picker's input (`<sl:date-picker hx-post="..." hx-trigger="change">`) or on the
  form sends the picked date.

## Left out

- **An input to type the date into**, **the time** and **natural language** variants are not here yet. For a time, put
  an `<sl:input type="time">` next to the date picker.
- **Presets** (a select of "Today", "Tomorrow", ...) are not here either.

## Attributes

Attributes of `<sl:date-picker>`: A button showing the chosen date that opens a `sl:calendar` in a popover. Its value lives in the calendar's date inputs, so `th:field` binds it and the form submits ISO dates. Every attribute that is not a prop goes to the `sl:calendar` inside, except `class`, which stays on the date picker: the calendar's props (`caption-layout`, `number-of-months`, `disabled-dates`, ...) and, for a single date, the attributes of its input (`th:field`, `name`, `required`, `min`, `max`). In a `sl:field`, the input takes the field's `th:field`, id and `aria-describedby`, and the button is named by the field's label. For a range, give it two `sl:input type="date"`s as content, the start and the end, and an `aria-label`. A pick closes the popover; for a range the second pick does. Needs JavaScript (`slDatePicker`).

| Attribute | Values | Default | Description |
| --- | --- | --- | --- |
| `placeholder` | *string* | — | The button's text while no date is chosen. By default "Pick a date". |
| `format` | `short`, `medium`, `long`, `full` | `long` | How the button shows the date, in the page's locale: as `Intl.DateTimeFormat`'s `dateStyle` and Java's `FormatStyle` (`10/23/26`, `Oct 23, 2026`, `October 23, 2026`, `Friday, October 23, 2026`). |
| `popover-id` | *string* | — | The popover's id, which the button points at with `popovertarget`. By default a generated one (`sl-date-picker-` and eight random characters). |

Every prop can also be set from an expression: `th:placeholder="${...}"`. Any other attribute passes through to the rendered element, and `class` is appended.

## JavaScript

`slDatePicker` sits on the date picker: `<div class="date-picker" x-data="slDatePicker">`. Its popover has
`slPopover` and its calendar `slCalendar`.

- **It needs JavaScript.** Without Alpine, the button still opens the popover (`popovertarget`), but it holds the bare
  date inputs, and the button's text does not follow them.
- **It needs no Alpine plugin.** If you load Alpine yourself (`shadleaf.assets.alpine=external`), plain Alpine is
  enough.
- **Keep your own `x-data` off `<sl:date-picker>`**: an element has one `x-data`. Listen for `change` on the input
  instead.
