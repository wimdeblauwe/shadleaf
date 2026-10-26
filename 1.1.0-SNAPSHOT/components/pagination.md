# Pagination

> Links to the other pages of a list, built from the Spring Data Page the controller returns.

Source: https://shadleaf.dev/1.1.0-SNAPSHOT/components/pagination/

**Example: From a page.** Page 5 of 27, in a request for /people?sort=name,asc&page=4. Each link keeps the sort.

```html
<sl:pagination th:page="${people}"/>
```

## From a page

Give `<sl:pagination>` the Spring Data `Page` the controller put in the model, and nothing else:

```html
<sl:pagination th:page="${people}"/>
```

It renders Previous, the first and the last page, the current page with one page on each side, an ellipsis for the
pages it leaves out, and Next. A gap of a single page shows that page instead of an ellipsis, which would take the same
room. `siblings` sets how many pages show on each side of the current one:

**Example: More pages around the current one.**

```html
<sl:pagination th:page="${orders}" siblings="2" qualifier="orders" aria-label="Order pages"/>
```

Each link is the request that rendered the page with another page number: every other parameter in the address (the
sort, the page size, a search) is kept as it was. The controller needs nothing new, as the `Pageable` that sorts the
[table](https://shadleaf.dev/1.1.0-SNAPSHOT/components/table.md) also reads the page:

```java
@GetMapping("/people")
String people(@PageableDefault(size = 20, sort = "name") Pageable pageable, Model model) {
  model.addAttribute("people", personRepository.findAll(pageable));
  return "people";
}
```

Sort the rows by a unique key last, or a row can show up on two pages or on none: see
[a stable order across pages](https://shadleaf.dev/1.1.0-SNAPSHOT/components/table.md#a-stable-order-across-pages).

The page shown now is an outline button with `aria-current="page"`. On the first page Previous is disabled, and on the
last page Next, the way a disabled link button is: no `href`, `aria-disabled="true"`, out of the tab order. An empty
result still has one page, the one saying so.

**Example: On the first page.** Previous is disabled. A gap of one page shows that page rather than an ellipsis.

```html
<sl:pagination th:page="${team}" qualifier="team" aria-label="Team pages"/>
```

### A Slice

A `Slice` knows whether there is a next page, not how many pages there are, so it gets Previous and Next only. It
saves the count query on a large table:

**Example: A Slice.** A Slice does not know how many pages there are, so it gets Previous and Next only.

```html
<sl:pagination th:page="${activity}" qualifier="activity" aria-label="Activity pages"/>
```

A list read a `Slice` at a time can also grow instead: [Load more](https://shadleaf.dev/1.1.0-SNAPSHOT/guides/tables-htmx.md#load-more) appends the
next slice's rows with htmx, and is a link to the next page without it.

### Without Spring Data

Without a `Page`, pass the page shown now (counting from 1) and the number of pages:

**Example: Without Spring Data.**

```html
<sl:pagination th:current="${results.page}" th:total="${results.pages}" qualifier="results"
               aria-label="Result pages"/>
```

The links ask for the page with the same parameter Spring Data would read, zero-based unless
`spring.data.web.pageable.one-indexed-parameters` is set (see [parameter names](https://shadleaf.dev/1.1.0-SNAPSHOT/components/table.md#parameter-names)); the
controller reads it itself. `sl:pagination-summary` and `sl:pagination-size` need a `Page` or `Slice`: without Spring
Data, write the summary with `#messages` and the sizes as links of your own.

### Two lists on one page

The `qualifier` of the controller's second `Pageable` (`@Qualifier("members")`) gives a pagination its own parameter
(`members_page`) and its links their own ids (`members-page-2`), as it does for the
[table's headers](https://shadleaf.dev/1.1.0-SNAPSHOT/components/table.md#two-tables-on-one-page).

## Written by hand

With content of its own, `<sl:pagination>` is the `nav` around a pagination of parts, as in shadcn/ui:

**Example: Written by hand.** The parts, as in shadcn/ui's example.

```html
<sl:pagination aria-label="Invoice pages">
  <sl:pagination-content>
    <sl:pagination-item>
      <sl:pagination-previous href="#"/>
    </sl:pagination-item>
    <sl:pagination-item>
      <sl:pagination-link href="#">1</sl:pagination-link>
    </sl:pagination-item>
    <sl:pagination-item>
      <sl:pagination-link href="#" active>2</sl:pagination-link>
    </sl:pagination-item>
    <sl:pagination-item>
      <sl:pagination-link href="#">3</sl:pagination-link>
    </sl:pagination-item>
    <sl:pagination-item>
      <sl:pagination-ellipsis/>
    </sl:pagination-item>
    <sl:pagination-item>
      <sl:pagination-next href="#"/>
    </sl:pagination-item>
  </sl:pagination-content>
</sl:pagination>
```

`sl:pagination-link` takes `active` for the page shown now and `disabled` for nowhere to go; `sl:pagination-previous`
and `sl:pagination-next` take `disabled`, and content of their own replaces their text.

## Summary

`<sl:pagination-summary>` says which rows the page shows, or with `format="page"` which page it is:

**Example: Summary.**

```html
<sl:pagination-summary th:page="${people}"/>
```

**Example: Summary as the page number.**

```html
<sl:pagination-summary th:page="${people}" format="page"/>
```

A `Slice` has no total, so it reads "11–20" or "Page 2". The words are messages, so your `messages.properties` rewords
or [translates](https://shadleaf.dev/1.1.0-SNAPSHOT/guides/translating.md) them; the numbers are formatted for the locale ("1,001–1,010 of 12,345").

## Rows per page

`<sl:pagination-size>` is a label and a [dropdown menu](https://shadleaf.dev/1.1.0-SNAPSHOT/components/dropdown-menu.md) of sizes, as in shadcn/ui's data table:

**Example: Rows per page.** In a request for /people?sort=name,asc&page=3&size=20. Each size keeps the sort and starts on the first page.

```html
<sl:pagination-size th:page="${people}"/>
```

Each size is a link to the same request with that size, every other parameter kept except the page number, so the list
starts on its first page. It works without JavaScript: the menu is a popover. `sizes="10,20,50"` is the default; a size
the page has that is not in the list is added to it, so the menu always shows a checked one. Limit what the controller
accepts with `spring.data.web.pageable.max-page-size`.

## A table footer

Summary, size and pagination are separate, so you lay out the footer. shadcn/ui's data table puts the summary at the
start and the controls at the end:

```html
<div class="table-footer">
  <sl:pagination-summary th:page="${people}"/>
  <div class="table-footer-controls">
    <sl:pagination-size th:page="${people}"/>
    <sl:pagination th:page="${people}"/>
  </div>
</div>
```

```css
.table-footer { display: flex; flex-wrap: wrap; align-items: center; justify-content: space-between; gap: 1rem; }
.table-footer-controls { display: flex; flex-wrap: wrap; align-items: center; gap: 1.5rem; }
/* The pagination centres itself across the full width by default. */
.table-footer .pagination { inline-size: auto; margin: 0; }
```

Sample-01's `/people` page does this.

## Focus after an htmx swap

Every link has an id: `page-previous`, `page-next` and `page-<number>` (prefixed with the qualifier). When htmx replaces
the list and its pagination, it gives the focus back to the element with the id that had it, so after Next the focus is
on the new Next. The size menu's button is `page-size-trigger`: the menu gives the focus back to it before the chosen
link is followed. [Tables with htmx](https://shadleaf.dev/1.1.0-SNAPSHOT/guides/tables-htmx.md) shows the whole page.

## Accessibility

- The `nav` is named "Pagination" (`sl.pagination.label`); an `aria-label` of your own replaces it, which two
  paginations on one page need.
- Previous and Next are named "Go to previous page" and "Go to next page". On a narrow screen only their chevron shows.
- The ellipsis reads "More pages", where shadcn/ui hides it from screen readers: a list item that says nothing is
  harder to follow.
- The size menu's button reads "Rows per page" followed by the size.

## Messages

[Translating](https://shadleaf.dev/1.1.0-SNAPSHOT/guides/translating.md) lists the languages Shadleaf ships them in, and explains how to add one.

| Key | English |
| --- | --- |
| `sl.pagination.label` | Pagination |
| `sl.pagination.previous`, `sl.pagination.previous.label` | Previous, Go to previous page |
| `sl.pagination.next`, `sl.pagination.next.label` | Next, Go to next page |
| `sl.pagination.more` | More pages |
| `sl.pagination.summary` | `{0}–{1} of {2}` |
| `sl.pagination.summary.slice` | `{0}–{1}` |
| `sl.pagination.summary.empty` | 0 of 0 |
| `sl.pagination.summary.page`, `sl.pagination.summary.page.slice` | `Page {0} of {1}`, `Page {0}` |
| `sl.pagination-size.label` | Rows per page |

## Left out

First and last page buttons (shadcn's dashboard has them; the first and the last page are always among the numbers),
a field to type a page number, and paging in the browser.

## Attributes

### sl:pagination

Attributes of `<sl:pagination>`: Links to the other pages of a list, in a `nav`. Given the Spring Data `Page` it shows (`th:page="${people}"`) and no content of its own, it renders them all: Previous, the first and the last page, the current page with `siblings` on each side, an ellipsis for the pages left out, and Next. Every link keeps the request's other parameters (sort, size, filters) and has a stable id, so htmx can give the focus back after a swap. A `Slice`, which does not know how many pages there are, gets Previous and Next only. Without Spring Data, pass `current` and `total` instead. With content of its own it is the frame for a hand-written pagination of `sl:pagination-content`, `-item`, `-link`, `-previous`, `-next` and `-ellipsis`, as in shadcn/ui.

| Attribute | Values | Default | Description |
| --- | --- | --- | --- |
| `th:page` | *object* | — | The Spring Data `Page` or `Slice` shown now. |
| `current` | *number* | — | Without a `page`: the page shown now, counting from 1. |
| `total` | *number* | — | Without a `page`: the number of pages. |
| `siblings` | *number* | `1` | How many pages to show on each side of the current one. |
| `qualifier` | *string* | — | The `@Qualifier` of the controller's `Pageable`, for a second list on the page: its links use `members_page` instead of `page`, and their ids start with `members-`. |

Every prop can also be set from an expression: `th:page="${...}"`. Any other attribute passes through to the rendered element, and `class` is appended.

### sl:pagination-content

Attributes of `<sl:pagination-content>`: The list (`ul`) of a hand-written `sl:pagination`, holding `sl:pagination-item`s.

`sl:pagination-content` declares no props; every attribute passes through.

### sl:pagination-item

Attributes of `<sl:pagination-item>`: One entry (`li`) of a `sl:pagination-content`: holds a link, Previous, Next or an ellipsis.

`sl:pagination-item` declares no props; every attribute passes through.

### sl:pagination-link

Attributes of `<sl:pagination-link>`: A link to one page, drawn as a ghost button, or an outline one for the page shown now (`active`). Give it an `href` or `th:href`, and the page number as content.

| Attribute | Values | Default | Description |
| --- | --- | --- | --- |
| `active` | *boolean* | — | Whether it is the page shown now: `aria-current="page"` and the outline look. |
| `size` | `xs`, `sm`, `default`, `lg`, `icon`, `icon-xs`, `icon-sm`, `icon-lg` | `icon` | Size of the link, as on `sl:button`: square by default, for a number. |
| `disabled` | *boolean* | — | There is nowhere to go: the link loses its `href`, gets `aria-disabled` and leaves the tab order, as a disabled `sl:button as="a"`. |

Every prop can also be set from an expression: `th:active="${...}"`. Any other attribute passes through to the rendered element, and `class` is appended.

### sl:pagination-previous

Attributes of `<sl:pagination-previous>`: The link to the previous page: a `sl:pagination-link` with a chevron and the text "Previous" (the message `sl.pagination.previous`), which content of its own replaces. Its name is "Go to previous page" (`sl.pagination.previous.label`); an `aria-label` replaces it. On a narrow screen only the chevron shows.

| Attribute | Values | Default | Description |
| --- | --- | --- | --- |
| `disabled` | *boolean* | — | There is no previous page: the link loses its `href`, gets `aria-disabled` and leaves the tab order. |

Every prop can also be set from an expression: `th:disabled="${...}"`. Any other attribute passes through to the rendered element, and `class` is appended.

### sl:pagination-next

Attributes of `<sl:pagination-next>`: The link to the next page: a `sl:pagination-link` with a chevron and the text "Next" (the message `sl.pagination.next`), which content of its own replaces. Its name is "Go to next page" (`sl.pagination.next.label`); an `aria-label` replaces it. On a narrow screen only the chevron shows.

| Attribute | Values | Default | Description |
| --- | --- | --- | --- |
| `disabled` | *boolean* | — | There is no next page: the link loses its `href`, gets `aria-disabled` and leaves the tab order. |

Every prop can also be set from an expression: `th:disabled="${...}"`. Any other attribute passes through to the rendered element, and `class` is appended.

### sl:pagination-ellipsis

Attributes of `<sl:pagination-ellipsis>`: Stands for the pages a `sl:pagination` leaves out: an ellipsis, read as "More pages" (the message `sl.pagination.more`).

`sl:pagination-ellipsis` declares no props; every attribute passes through.

### sl:pagination-summary

Attributes of `<sl:pagination-summary>`: Where the page is in the results, as text: "11–20 of 270", or with `format="page"` "Page 2 of 27". Takes the Spring Data `Page` shown now (`th:page`); for a `Slice`, which does not know the total, "11–20" or "Page 2". The words are messages (`sl.pagination.summary*`), so an application rewords or translates them; numbers are formatted for the locale.

| Attribute | Values | Default | Description |
| --- | --- | --- | --- |
| `th:page` | *object* | — | The Spring Data `Page` or `Slice` shown now. |
| `format` | `range`, `page` | `range` | `range`: the rows shown and the total. `page`: the page number and the number of pages. |

Every prop can also be set from an expression: `th:page="${...}"`. Any other attribute passes through to the rendered element, and `class` is appended.

### sl:pagination-size

Attributes of `<sl:pagination-size>`: "Rows per page" and a menu of page sizes, as in shadcn/ui's data table. The sizes are `sl:dropdown-menu-radio-item` links: each asks for the same request with that size, every other parameter kept except the page number, so the list starts on the first page. It works without JavaScript (the menu is a popover). Takes the Spring Data `Page` shown now, whose size is the checked one; a size the page has but `sizes` does not list is added. The label is the message `sl.pagination-size.label`. Attributes go to the wrapper.

| Attribute | Values | Default | Description |
| --- | --- | --- | --- |
| `th:page` | *object* | — | The Spring Data `Page` or `Slice` shown now. |
| `sizes` | *string* | `10,20,50` | The sizes to choose from, separated by commas. |
| `qualifier` | *string* | — | The `@Qualifier` of the controller's `Pageable`, for a second list on the page: its links use `members_size` instead of `size`. |
| `id` | *string* | — | The menu's id; its button gets `-trigger` added, its label `-label`. `page-size` when not set, prefixed with the qualifier (`members-page-size`). |

Every prop can also be set from an expression: `th:page="${...}"`. Any other attribute passes through to the rendered element, and `class` is appended.
