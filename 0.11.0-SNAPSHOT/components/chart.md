# Chart

> shadcn/ui's Chart, Recharts drawing bars, lines and areas from rows the controller puts in the model; labels and colours on the series, named Intl formats.

Source: https://shadleaf.dev/0.11.0-SNAPSHOT/components/chart/

**Example: Bar chart.** Two series side by side, in a card as shadcn shows it. The months are YearMonths: month-short writes the ticks (Jan), month the tooltip's label (January).

```html
<sl:card>
  <sl:card-header>
    <sl:card-title level="3">Bar Chart - Multiple</sl:card-title>
    <sl:card-description>January - June 2024</sl:card-description>
  </sl:card-header>
  <sl:card-content>
    <sl:chart th:data="${visitors}" label="Visitors, January to June 2024">
      <sl:chart-grid/>
      <sl:chart-x-axis key="month" format="month-short" tick-margin="10"/>
      <sl:chart-tooltip indicator="dashed" cursor="false" label-format="month"/>
      <sl:chart-bar key="desktop" label="Desktop" radius="4"/>
      <sl:chart-bar key="mobile" label="Mobile" radius="4"/>
    </sl:chart>
  </sl:card-content>
  <sl:card-footer>
    <p>Showing total visitors for the last 6 months</p>
  </sl:card-footer>
</sl:card>
```

## Built from parts

A chart is shadcn's: [Recharts](https://recharts.github.io/) draws it in the browser, with shadcn's look. The template
says what to draw, the controller gives the rows:

```html
<sl:chart th:data="${visitors}" th:label="#{visitors.title}">
  <sl:chart-grid/>
  <sl:chart-x-axis key="month" format="month-short"/>
  <sl:chart-tooltip/>
  <sl:chart-bar key="desktop" th:label="#{visitors.desktop}"/>
  <sl:chart-bar key="mobile" th:label="#{visitors.mobile}"/>
</sl:chart>
```

```java
record MonthlyVisitors(YearMonth month, long desktop, long mobile) {}

@GetMapping("/visitors")
String visitors(Model model) {
  model.addAttribute("visitors", repository.visitorsPerMonth()); // List<MonthlyVisitors>
  return "visitors";
}
```

- **`th:data`** takes a list of rows: records, objects or maps, or a Spring Data `Page`. They go into the page as
  JSON, written by Jackson.
- **The series parts** (`sl:chart-bar`, `sl:chart-line`, `sl:chart-area`) name the property they draw in `key`, and
  carry the series' `label` and `color`: what shadcn's `ChartConfig` holds. The chart draws bars for bars only, lines
  for lines only, areas for areas only, and a composed chart for a mix.
- **The other parts** add a grid, the axes, a tooltip and a legend. Every part is optional; their order is the order
  Recharts draws them in.
- **`label`** names the chart for a screen reader. The chart takes the focus, and the arrow keys move the tooltip from
  one category to the next.

## Colours

A series without a `color` takes the chart tokens in turn: the first `var(--chart-1)`, the second `var(--chart-2)`,
up to `--chart-5`, then again from the first. A theme that redefines them recolours every chart. `color` takes any
CSS colour, and `color-dark` a different one for the dark theme:

```html
<sl:chart-line key="revenue" color="oklch(0.55 0.2 260)" color-dark="oklch(0.75 0.15 260)"/>
```

The colour becomes the CSS property `--color-<key>` on the chart, which is why a key is checked to be a CSS
identifier: letters, digits, `_` and `-`, not starting with a digit or `-`. A key that is not fails the render.

## Stacked bars and a legend

**Example: Stacked bars and a legend.** The same stack id stacks the series; the radius rounds the bottom of the first and the top of the last.

```html
<sl:chart th:data="${visitors}" label="Visitors, January to June 2024">
  <sl:chart-grid/>
  <sl:chart-x-axis key="month" format="month-short" tick-margin="10"/>
  <sl:chart-tooltip hide-label/>
  <sl:chart-legend/>
  <sl:chart-bar key="desktop" label="Desktop" stack="a" radius="0,0,4,4"/>
  <sl:chart-bar key="mobile" label="Mobile" stack="a" radius="4,4,0,0"/>
</sl:chart>
```

The legend lists the series by label, in alphabetical order, as Recharts sorts it (shadcn's does the same).

## Line chart

**Example: Line chart.**

```html
<sl:chart th:data="${visitors}" label="Visitors, January to June 2024" margin="12">
  <sl:chart-grid/>
  <sl:chart-x-axis key="month" format="month-short"/>
  <sl:chart-tooltip cursor="false" label-format="month"/>
  <sl:chart-line key="desktop" label="Desktop"/>
  <sl:chart-line key="mobile" label="Mobile"/>
</sl:chart>
```

`margin` leaves room left and right of the plot, so the first and last tick labels fit. `curve` sets how a line runs
between the points (`monotone`, `natural`, `linear`, `step`), `dots` puts a dot on each.

## Area chart

**Example: Area chart.** The tooltip's line indicator, nested beside the label when there is one series.

```html
<sl:chart th:data="${visitors}" label="Desktop visitors, January to June 2024" margin="12">
  <sl:chart-grid/>
  <sl:chart-x-axis key="month" format="month-short"/>
  <sl:chart-tooltip indicator="line" cursor="false" label-format="month"/>
  <sl:chart-area key="desktop" label="Desktop"/>
</sl:chart>
```

**Example: Stacked areas.**

```html
<sl:chart th:data="${visitors}" label="Visitors, January to June 2024" margin="12">
  <sl:chart-grid/>
  <sl:chart-x-axis key="month" format="month-short"/>
  <sl:chart-tooltip cursor="false" label-format="month"/>
  <sl:chart-area key="mobile" label="Mobile" stack="a" color="var(--chart-2)"/>
  <sl:chart-area key="desktop" label="Desktop" stack="a" color="var(--chart-1)"/>
</sl:chart>
```

## Tooltip

The tooltip shows the hovered category and every series' label and value. `indicator` is the mark before each label:
`dot` (the default), `line` or `dashed`. `hide-label` leaves out the category, `hide-indicator` the marks, and
`cursor="false"` the highlight of the hovered category.

## Formats

Instead of shadcn's `tickFormatter`, `formatter` and `labelFormatter` functions, the parts take a named format,
written with `Intl` in the page's language (`<html lang>`, from Spring's locale):

| Format | Writes | For |
| --- | --- | --- |
| `number` | 1,234.5 | numbers; the tooltip's values by default |
| `integer` | 1,235 | |
| `compact` | 1.2K | a value axis |
| `percent` | 25% for 0.25 | |
| `currency` | €1,234.50, with `currency="EUR"` | |
| `day` | Apr 1 | dates |
| `month-short` | Apr | |
| `month` | April | |
| `month-year` | Apr 2024 | |
| `date` | Apr 1, 2024 | |
| `time` | 10:00 AM | |

`format` on an axis writes its ticks, `label-format` and `value-format` on the tooltip its label and values. A format
is an enum: a typo fails the render. A value of another kind (a date format on a name) is shown as it is.

**Example: Formats.** Days as LocalDates (day on the axis, date in the tooltip), a value axis in compact numbers and the values in euros, all in the page's language.

```html
<sl:chart th:data="${revenue}" label="Revenue per day, 1 to 10 April 2024">
  <sl:chart-grid/>
  <sl:chart-x-axis key="day" format="day"/>
  <sl:chart-y-axis format="compact"/>
  <sl:chart-tooltip label-format="date" value-format="currency" currency="EUR"/>
  <sl:chart-bar key="revenue" label="Revenue" radius="4"/>
</sl:chart>
```

For a label no format writes, format it in Java and give the tooltip that property in `label-key`: it then shows that
property of the hovered row as its label.

### Dates and time zones

The dates are read by their Java type, as Jackson writes them, never by the browser's date parser (which reads
`2024-04-01` as midnight UTC, the evening before in America):

- **`LocalDate`** (`2024-04-01`) and **`YearMonth`** (`2024-04`) are calendar dates: shown as they are, in every time
  zone.
- **`LocalDateTime`** (`2024-04-01T10:00:00`) is a time on the clock: shown as written.
- **`Instant`**, **`OffsetDateTime`** and **`ZonedDateTime`** (`2024-04-01T08:00:00Z`) are moments: shown in the
  chart's time zone. That is the request's (Spring's `LocaleContextHolder`, which a `LocaleResolver` that knows the
  user's zone sets; the server's otherwise), or the one `time-zone` names.

**Example: Instants in a time zone.** Instants (written with Z) are shown in the chart's time zone: the request's, or time-zone. The first point is 06:00 UTC, so 08:00 in Brussels.

```html
<sl:chart th:data="${requests}" label="Requests per hour, 1 April 2024" time-zone="Europe/Brussels" margin="12">
  <sl:chart-grid/>
  <sl:chart-x-axis key="hour" format="time"/>
  <sl:chart-tooltip label-format="time" value-format="integer"/>
  <sl:chart-line key="requests" label="Requests" dots/>
</sl:chart>
```

## Series from the model

For series known only at run time, put `th:each` on a series part:

**Example: Series from the model.** th:each on a series part, for series known at run time. Each takes the next chart colour.

```html
<sl:chart th:data="${browsers}" label="Visitors per browser, January to June 2024">
  <sl:chart-grid/>
  <sl:chart-x-axis key="month" format="month-short"/>
  <sl:chart-tooltip label-format="month"/>
  <sl:chart-legend/>
  <sl:chart-line th:each="browser : ${series}" th:key="${browser.key}" th:label="${browser.label}"/>
</sl:chart>
```

## Size

A chart is as wide as its container and 16:9, as shadcn's. For a fixed height, give it one in your CSS, with
`aspect-ratio: auto` (shadcn's `aspect-auto h-[250px]`):

```css
.visitors-chart { aspect-ratio: auto; height: 250px; }
```

## Right to left

On a `dir="rtl"` page the category axis runs from right to left and the value axis stands on the right; the arrow
keys follow what you see.

## With htmx

- A chart that htmx swaps in draws itself; one it swaps out is cleaned up.
- Before htmx saves a page for Back, the charts are put back to their skeletons, so the saved page holds none of the
  inline styles Recharts sets (a strict Content-Security-Policy would report them when the page comes back). Back
  draws them again.

## Left out

These shadcn and Recharts features have no counterpart yet: pie, radar and radial charts (pie charts are planned),
horizontal bars, gradients, custom tooltip and legend content, icons in the config, reference lines and brushes.
For anything else, `sl-chart` is a custom element: an application can write its own.

## Attributes

### sl:chart

Attributes of `<sl:chart>`: A chart drawn by Recharts in the browser, as shadcn's: bars, lines and areas over a category or time axis, with a grid, axes, a tooltip and a legend. Pass the rows in `th:data` and put the parts inside, in the order Recharts draws them: `sl:chart-grid`, `sl:chart-x-axis`, `sl:chart-y-axis`, `sl:chart-tooltip`, `sl:chart-legend`, then one `sl:chart-bar`, `sl:chart-line` or `sl:chart-area` per series (only bars give a bar chart, a mix a composed chart). Needs JavaScript: without it the chart's skeleton stays. The chart is 16:9; for a fixed height, give it one in your CSS with `aspect-ratio: auto`. Every attribute that is not a prop passes through to the element.

| Attribute | Values | Default | Description |
| --- | --- | --- | --- |
| `th:data` | *object* | **required** | The rows: a list (or a Spring Data `Page`) of objects, records or maps, one per category or point in time. Each row holds the category (`month`) and a number per series (`desktop`, `mobile`). Serialised with Jackson: dates as ISO strings, which the named date formats read by their Java type. |
| `label` | *string* | **required** | The chart's accessible name, such as its title: what a screen reader announces when the chart takes the focus. |
| `margin` | *number* | — | Space in pixels left and right of the plot, so the first and last tick labels fit (shadcn's line and area charts use 12). |
| `time-zone` | *string* | — | The time zone (an IANA id such as `Europe/Brussels`) instants in the data are shown in: `Instant`, `OffsetDateTime` and `ZonedDateTime`. Defaults to the request's (Spring's `LocaleContextHolder`). A `LocalDate` or `LocalDateTime` is shown as written, in any zone. |

Every prop can also be set from an expression: `th:data="${...}"`. Any other attribute passes through to the rendered element, and `class` is appended.

### sl:chart-grid

Attributes of `<sl:chart-grid>`: The grid lines behind a `sl:chart`'s plot. Draws no element of its own on the page: the chart reads it.

| Attribute | Values | Default | Description |
| --- | --- | --- | --- |
| `vertical` | *boolean* | — | Vertical lines too, one per category. |
| `horizontal` | *boolean* | `true` | Horizontal lines, one per value tick. |

Every prop can also be set from an expression: `th:vertical="${...}"`. Any other attribute passes through to the rendered element, and `class` is appended.

### sl:chart-x-axis

Attributes of `<sl:chart-x-axis>`: The category or time axis of a `sl:chart`, along the bottom: its ticks are the values of `key` in the rows, without tick or axis lines (as shadcn's). Under `dir="rtl"` it runs from right to left. Draws no element of its own on the page: the chart reads it.

| Attribute | Values | Default | Description |
| --- | --- | --- | --- |
| `key` | *string* | **required** | The property of the rows that holds the category (`month`) or the date. |
| `format` | `number`, `integer`, `compact`, `percent`, `currency`, `day`, `month-short`, `month`, `month-year`, `date`, `time` | — | How the ticks are written, with `Intl` in the page's language. Numbers: `number`, `integer`, `compact` (1.2K), `percent` (0.25 is 25%), `currency` (with `currency`). Dates: `day` (Apr 1), `month-short` (Apr), `month` (April), `month-year` (Apr 2024), `date`, `time`. Without one, the values as they are. |
| `currency` | *string* | — | The ISO 4217 code (`EUR`) for `format="currency"`. |
| `tick-margin` | *number* | `8` | Space in pixels between the ticks and the plot. |
| `min-tick-gap` | *number* | `5` | The least space in pixels between two ticks; Recharts leaves out ticks that would come closer, as on a long time axis. |

Every prop can also be set from an expression: `th:key="${...}"`. Any other attribute passes through to the rendered element, and `class` is appended.

### sl:chart-y-axis

Attributes of `<sl:chart-y-axis>`: The value axis of a `sl:chart`, at the start side (the left, or the right under `dir="rtl"`), without tick or axis lines. shadcn's charts often leave it out and show the values in the tooltip. Draws no element of its own on the page: the chart reads it.

| Attribute | Values | Default | Description |
| --- | --- | --- | --- |
| `format` | `number`, `integer`, `compact`, `percent`, `currency`, `day`, `month-short`, `month`, `month-year`, `date`, `time` | — | How the ticks are written, as on `sl:chart-x-axis`: `compact` (1.2K) and `currency` suit a value axis. |
| `currency` | *string* | — | The ISO 4217 code (`EUR`) for `format="currency"`. |
| `tick-margin` | *number* | `8` | Space in pixels between the ticks and the plot. |
| `tick-count` | *number* | `5` | How many ticks Recharts aims for. |

Every prop can also be set from an expression: `th:format="${...}"`. Any other attribute passes through to the rendered element, and `class` is appended.

### sl:chart-tooltip

Attributes of `<sl:chart-tooltip>`: The tooltip of a `sl:chart`: on hover, or with the arrow keys once the chart has the focus, it shows the category and every series' label and value. Draws no element of its own on the page: the chart reads it.

| Attribute | Values | Default | Description |
| --- | --- | --- | --- |
| `indicator` | `dot`, `line`, `dashed` | `dot` | The mark before each series' label, in its colour. |
| `cursor` | *boolean* | `true` | Highlight the hovered category (a band behind bars, a line over lines and areas). |
| `hide-label` | *boolean* | — | Leave out the label at the top (the category). |
| `hide-indicator` | *boolean* | — | Leave out the marks before the series' labels. |
| `label-key` | *string* | — | The property of the rows to take the label from instead of the axis category: a series key gives that series' label, any other property its value in the hovered row, such as a date the server formatted. |
| `label-format` | `number`, `integer`, `compact`, `percent`, `currency`, `day`, `month-short`, `month`, `month-year`, `date`, `time` | — | How the label is written, as the ticks of `sl:chart-x-axis` (`month`, `date`, ...). Without one, as it is. |
| `value-format` | `number`, `integer`, `compact`, `percent`, `currency`, `day`, `month-short`, `month`, `month-year`, `date`, `time` | `number` | How the values are written: `number` in the page's language, or `currency`, `percent`, ... |
| `currency` | *string* | — | The ISO 4217 code (`EUR`) for a `currency` format. |

Every prop can also be set from an expression: `th:indicator="${...}"`. Any other attribute passes through to the rendered element, and `class` is appended.

### sl:chart-legend

Attributes of `<sl:chart-legend>`: The legend of a `sl:chart`: every series' label with its colour. Draws no element of its own on the page: the chart reads it.

| Attribute | Values | Default | Description |
| --- | --- | --- | --- |
| `vertical-align` | `bottom`, `top` | `bottom` | Below the plot, or above it. |

Every prop can also be set from an expression: `th:vertical-align="${...}"`. Any other attribute passes through to the rendered element, and `class` is appended.

### sl:chart-bar

Attributes of `<sl:chart-bar>`: One series of a `sl:chart`, drawn as bars: one per row. Draws no element of its own on the page: the chart reads it.

| Attribute | Values | Default | Description |
| --- | --- | --- | --- |
| `key` | *string* | **required** | The property of the rows that holds this series' numbers (`desktop`): letters, digits, `_` and `-`, as it also names the colour property `--color-&lt;key&gt;`. |
| `label` | *string* | — | The series' name in the tooltip and the legend, typically a message (`th:label="#{visitors.desktop}"`). Defaults to the key. |
| `color` | *string* | — | Its colour, any CSS colour (`var(--chart-3)`, `#2563eb`). Defaults to the chart tokens in turn: the first series `var(--chart-1)`, the second `var(--chart-2)`, and so on (after the fifth, again from the first). |
| `color-dark` | *string* | — | A different colour in the dark theme; `color` is then the light one (the two go into `light-dark()`). |
| `radius` | *string* | — | The bars' corner radius in pixels: `4` for every corner, or four numbers (top-left, top-right, bottom-right, bottom-left), as `4,4,0,0` for the top bar of a stack. |
| `stack` | *string* | — | Stack this series on the others with the same `stack` id (any name, such as `a`) instead of drawing them side by side. |

Every prop can also be set from an expression: `th:key="${...}"`. Any other attribute passes through to the rendered element, and `class` is appended.

### sl:chart-line

Attributes of `<sl:chart-line>`: One series of a `sl:chart`, drawn as a line through the rows. Draws no element of its own on the page: the chart reads it.

| Attribute | Values | Default | Description |
| --- | --- | --- | --- |
| `key` | *string* | **required** | The property of the rows that holds this series' numbers (`desktop`): letters, digits, `_` and `-`, as it also names the colour property `--color-&lt;key&gt;`. |
| `label` | *string* | — | The series' name in the tooltip and the legend, typically a message (`th:label="#{visitors.desktop}"`). Defaults to the key. |
| `color` | *string* | — | Its colour, any CSS colour (`var(--chart-3)`, `#2563eb`). Defaults to the chart tokens in turn: the first series `var(--chart-1)`, the second `var(--chart-2)`, and so on (after the fifth, again from the first). |
| `color-dark` | *string* | — | A different colour in the dark theme; `color` is then the light one (the two go into `light-dark()`). |
| `curve` | `monotone`, `natural`, `linear`, `step`, `step-before`, `step-after` | `monotone` | How the line runs between the points: smooth (`monotone`, `natural`), straight (`linear`) or in steps. |
| `dots` | *boolean* | — | A dot on every point. |
| `stroke-width` | *number* | `2` | The line's width in pixels. |

Every prop can also be set from an expression: `th:key="${...}"`. Any other attribute passes through to the rendered element, and `class` is appended.

### sl:chart-area

Attributes of `<sl:chart-area>`: One series of a `sl:chart`, drawn as a filled area under a line through the rows. Draws no element of its own on the page: the chart reads it.

| Attribute | Values | Default | Description |
| --- | --- | --- | --- |
| `key` | *string* | **required** | The property of the rows that holds this series' numbers (`desktop`): letters, digits, `_` and `-`, as it also names the colour property `--color-&lt;key&gt;`. |
| `label` | *string* | — | The series' name in the tooltip and the legend, typically a message (`th:label="#{visitors.desktop}"`). Defaults to the key. |
| `color` | *string* | — | Its colour, any CSS colour (`var(--chart-3)`, `#2563eb`). Defaults to the chart tokens in turn: the first series `var(--chart-1)`, the second `var(--chart-2)`, and so on (after the fifth, again from the first). |
| `color-dark` | *string* | — | A different colour in the dark theme; `color` is then the light one (the two go into `light-dark()`). |
| `curve` | `natural`, `monotone`, `linear`, `step`, `step-before`, `step-after` | `natural` | How the line runs between the points: smooth (`natural`, `monotone`), straight (`linear`) or in steps. |
| `stack` | *string* | — | Stack this series on the others with the same `stack` id (any name, such as `a`). |
| `fill-opacity` | *number* | `0.4` | The opacity of the fill, from 0 to 1. |

Every prop can also be set from an expression: `th:key="${...}"`. Any other attribute passes through to the rendered element, and `class` is appended.

## JavaScript

The chart is the one component that is no Alpine component: Recharts is a React library, and Shadleaf runs it on
[Preact](https://preactjs.com/) (`preact/compat`), which makes the chart's script some 50 KB (gzip) smaller.

- **JavaScript is required.** Without it, the chart's skeleton stays where the chart would be.
- **It loads when it is needed.** Every Shadleaf script (`shadleaf.assets.alpine` `bundled`, `csp` or `external`)
  holds a loader of about 1 KB. It loads the chart's script (about 125 KB gzip) when the first chart comes near the
  viewport, so a page without charts never loads it. It needs no Alpine.
- **It works under a strict Content-Security-Policy**, without `'unsafe-inline'` or `'unsafe-eval'`: the rows are a
  JSON data block (`<script type="application/json">`), which never runs, and the colours are set through CSSOM rather
  than a `<style>` element.
- **The server renders** `<sl-chart class="chart">` holding the data block, one element per part
  (`<sl-chart-bar key="desktop" label="Desktop">`) and the skeleton. The script mounts the chart beside them and marks
  the chart `data-ready` once it has drawn.
