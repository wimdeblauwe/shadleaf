# Table

> A styled table, whose column headers sort it on the server.

Source: https://shadleaf.dev/current/components/table/

**Example: Table.**

```html
<sl:table>
  <caption>A list of your recent invoices.</caption>
  <thead>
    <tr>
      <th>Invoice</th>
      <th>Status</th>
      <th>Method</th>
      <sl:table-head align="end">Amount</sl:table-head>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td>INV001</td>
      <td><sl:badge variant="secondary">Paid</sl:badge></td>
      <td>Credit card</td>
      <td data-align="end">€250.00</td>
    </tr>
    <tr>
      <td>INV002</td>
      <td><sl:badge variant="outline">Pending</sl:badge></td>
      <td>Bank transfer</td>
      <td data-align="end">€150.00</td>
    </tr>
    <tr>
      <td>INV003</td>
      <td><sl:badge variant="destructive">Overdue</sl:badge></td>
      <td>Credit card</td>
      <td data-align="end">€350.00</td>
    </tr>
  </tbody>
  <tfoot>
    <tr>
      <td colspan="3">Total</td>
      <td data-align="end">€750.00</td>
    </tr>
  </tfoot>
</sl:table>
```

## Plain rows and cells

`<sl:table>` styles the plain table elements you write inside it: `caption`, `thead`, `tbody`, `tfoot`, `tr`, `th` and
`td`. There is no component per row or cell, so a row is the `th:each` you already write:

```html
<sl:table>
  <thead>
    <tr><th>Name</th><th>Email</th></tr>
  </thead>
  <tbody>
    <tr th:each="person : ${people}">
      <td th:text="${person.name}">Ada Lovelace</td>
      <td th:text="${person.email}">ada@example.com</td>
    </tr>
  </tbody>
</sl:table>
```

Attributes on `<sl:table>` go to the `table`; it sits in a container that scrolls sideways when the table is wider
than the page. A table nested in a cell keeps its own look, so put it in an `<sl:table>` of its own.

## Alignment

`<sl:table-head align="end">` puts a header's label at the end of its cell. The cells below take the same with
`data-align="end"` (or `"center"`), which works without utility classes, so also with the standalone stylesheet. Use
it for amounts and other numbers, so their digits line up.

## Wide tables

A table wider than the page scrolls sideways inside its container. A keyboard user can scroll it too: Chromium and
Firefox make the container a tab stop of its own when nothing in it can take the focus, and the arrow keys then scroll
it; when a sort link, a checkbox or a row's menu button in it takes the focus, the browser scrolls it into view. The
container has no `tabindex` of its own, which would add a tab stop to every table that fits.

WebKit, checked with Playwright's build of it, does neither for a container of plain text: there a wide table with
nothing to focus in it cannot be reached with the keyboard, and with Option+Tab (Safari's *Press Tab to highlight each
item*) its links can, as above. axe reports such a table as
`scrollable-region-focusable`. Keep a table that is only text from growing wider than a phone, or give its rows
something to focus.

### Pin the first column

When the first column names the row, it should not scroll away. `sticky-first-column` pins the first cell of every row
(`th` and `td`, in `thead`, `tbody` and `tfoot`) to the start edge with `position: sticky`, so it also works in
right-to-left pages. No JavaScript is involved:

**Example: Pinned first column.** A wide table whose first column stays in view while the rest scrolls sideways.

```html
<sl:table sticky-first-column>
  <caption>Sales per month.</caption>
  <thead>
    <tr>
      <th>Person</th>
      <th data-align="end">Jan</th>
      <th data-align="end">Feb</th>
      <th data-align="end">Mar</th>
      <th data-align="end">Apr</th>
      <th data-align="end">May</th>
      <th data-align="end">Jun</th>
      <th data-align="end">Jul</th>
      <th data-align="end">Aug</th>
      <th data-align="end">Sep</th>
      <th data-align="end">Oct</th>
      <th data-align="end">Nov</th>
      <th data-align="end">Dec</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td>Ada Lovelace</td>
      <td data-align="end">€1200</td>
      <td data-align="end">€1237</td>
      <td data-align="end">€1274</td>
      <td data-align="end">€1311</td>
      <td data-align="end">€1348</td>
      <td data-align="end">€1385</td>
      <td data-align="end">€1422</td>
      <td data-align="end">€1459</td>
      <td data-align="end">€1496</td>
      <td data-align="end">€1533</td>
      <td data-align="end">€1570</td>
      <td data-align="end">€1607</td>
    </tr>
    <tr>
      <td>Grace Hopper</td>
      <td data-align="end">€900</td>
      <td data-align="end">€953</td>
      <td data-align="end">€1006</td>
      <td data-align="end">€1059</td>
      <td data-align="end">€1112</td>
      <td data-align="end">€1165</td>
      <td data-align="end">€1218</td>
      <td data-align="end">€1271</td>
      <td data-align="end">€1324</td>
      <td data-align="end">€1377</td>
      <td data-align="end">€1430</td>
      <td data-align="end">€1483</td>
    </tr>
    <tr>
      <td>Alan Turing</td>
      <td data-align="end">€1500</td>
      <td data-align="end">€1479</td>
      <td data-align="end">€1458</td>
      <td data-align="end">€1437</td>
      <td data-align="end">€1416</td>
      <td data-align="end">€1395</td>
      <td data-align="end">€1374</td>
      <td data-align="end">€1353</td>
      <td data-align="end">€1332</td>
      <td data-align="end">€1311</td>
      <td data-align="end">€1290</td>
      <td data-align="end">€1269</td>
    </tr>
  </tbody>
  <tfoot>
    <tr>
      <td>Total</td>
      <td data-align="end">€3600</td>
      <td data-align="end">€3669</td>
      <td data-align="end">€3738</td>
      <td data-align="end">€3807</td>
      <td data-align="end">€3876</td>
      <td data-align="end">€3945</td>
      <td data-align="end">€4014</td>
      <td data-align="end">€4083</td>
      <td data-align="end">€4152</td>
      <td data-align="end">€4221</td>
      <td data-align="end">€4290</td>
      <td data-align="end">€4359</td>
    </tr>
  </tfoot>
</sl:table>
```

The pinned cells have the page's `--background` so the scrolled cells do not show through; when the table sits on
something else, such as a card, set `--sl-table-bg` on the table (`style` is not allowed under a strict CSP, so do it
in your stylesheet: `.my-table { --sl-table-bg: var(--card); }`). Row hover, selected and footer tints carry over to the
pinned cells. A line on the pinned column's end edge appears once the table has scrolled; it is driven by a CSS
scroll-driven animation (Chromium and Safari 26; Firefox where `animation-timeline` is enabled), and where that is
missing the line is always drawn. In forced colours the line is a system-coloured border, and on paper nothing is
pinned. The example is plain text, so what [Wide tables](#wide-tables) says about WebKit and the keyboard applies to it.

### Column groups

A wide table often groups its columns under a header row, such as sales and payments per quarter. Write a `colgroup`
per group, as plain HTML: the table draws a line on the start edge of every group after the first, through the header,
body and footer rows, in the colour of its row lines. The start edge is the right one in a right-to-left page.

```html
<sl:table sticky-first-column>
  <colgroup></colgroup>
  <colgroup span="4"></colgroup>
  <colgroup span="4"></colgroup>
  <thead>
    <tr>
      <td></td>
      <th scope="colgroup" colspan="4">Sales</th>
      <th scope="colgroup" colspan="4">Payments</th>
    </tr>
    <tr><th>Person</th><th>Q1</th>...</tr>
  </thead>
  ...
</sl:table>
```

**Example: Column groups.** A colgroup per group of columns draws a line at the start of every group after the first.

```html
<sl:table sticky-first-column>
  <caption>Sales and payments per quarter.</caption>
  <colgroup></colgroup>
  <colgroup span="4"></colgroup>
  <colgroup span="4"></colgroup>
  <thead>
    <tr>
      <td></td>
      <th scope="colgroup" colspan="4" data-align="center">Sales</th>
      <th scope="colgroup" colspan="4" data-align="center">Payments</th>
    </tr>
    <tr>
      <th>Person</th>
      <th data-align="end">Q1</th>
      <th data-align="end">Q2</th>
      <th data-align="end">Q3</th>
      <th data-align="end">Q4</th>
      <th data-align="end">Q1</th>
      <th data-align="end">Q2</th>
      <th data-align="end">Q3</th>
      <th data-align="end">Q4</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td>Ada Lovelace</td>
      <td data-align="end">€3711</td>
      <td data-align="end">€4033</td>
      <td data-align="end">€4144</td>
      <td data-align="end">€4603</td>
      <td data-align="end">€3500</td>
      <td data-align="end">€3900</td>
      <td data-align="end">€4100</td>
      <td data-align="end">€4400</td>
    </tr>
    <tr>
      <td>Grace Hopper</td>
      <td data-align="end">€2859</td>
      <td data-align="end">€3177</td>
      <td data-align="end">€3495</td>
      <td data-align="end">€3813</td>
      <td data-align="end">€2800</td>
      <td data-align="end">€3100</td>
      <td data-align="end">€3400</td>
      <td data-align="end">€3800</td>
    </tr>
    <tr>
      <td>Alan Turing</td>
      <td data-align="end">€4437</td>
      <td data-align="end">€4374</td>
      <td data-align="end">€4311</td>
      <td data-align="end">€4248</td>
      <td data-align="end">€4400</td>
      <td data-align="end">€4300</td>
      <td data-align="end">€4300</td>
      <td data-align="end">€4200</td>
    </tr>
  </tbody>
  <tfoot>
    <tr>
      <td>Total</td>
      <td data-align="end">€11007</td>
      <td data-align="end">€11584</td>
      <td data-align="end">€11950</td>
      <td data-align="end">€12664</td>
      <td data-align="end">€10700</td>
      <td data-align="end">€11300</td>
      <td data-align="end">€11800</td>
      <td data-align="end">€12400</td>
    </tr>
  </tfoot>
</sl:table>
```

A `colgroup` without `span` covers one column, or the `col` elements inside it. `scope="colgroup"` on a group's
header tells a screen reader which columns it heads. When the second group starts right after a pinned first column,
the group line and the pinned column's edge are one line. The lines are borders, so they print and keep a system
colour in forced colours. The example is plain text too, so what [Wide tables](#wide-tables) says about WebKit
applies.

## Compact tables

A page of many rows of figures, such as a ledger or a report meant for one sheet of paper, takes less room with
`size="sm"`: smaller cell padding, row height and text, in every skin. The header, body, footer, caption and
`sl:table-empty` row follow it, and the sort links become extra-small buttons:

**Example: Compact.** size="sm", for many rows of figures or a table meant for paper.

```html
<sl:table size="sm">
  <caption>Cash book, October.</caption>
  <thead>
    <tr>
      <sl:table-head sort="date">Date</sl:table-head>
      <th>Description</th>
      <sl:table-head sort="amount" align="end">In</sl:table-head>
      <th data-align="end">Out</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td>02/10</td>
      <td>Opening balance</td>
      <td data-align="end"></td>
      <td data-align="end"></td>
    </tr>
    <tr>
      <td>03/10</td>
      <td>Invoice INV041</td>
      <td data-align="end">€1250.00</td>
      <td data-align="end"></td>
    </tr>
    <tr>
      <td>04/10</td>
      <td>Office rent</td>
      <td data-align="end"></td>
      <td data-align="end">€950.00</td>
    </tr>
    <tr>
      <td>07/10</td>
      <td>Invoice INV042</td>
      <td data-align="end">€480.00</td>
      <td data-align="end"></td>
    </tr>
    <tr>
      <td>09/10</td>
      <td>Software licences</td>
      <td data-align="end"></td>
      <td data-align="end">€120.00</td>
    </tr>
    <tr>
      <td>11/10</td>
      <td>Invoice INV043</td>
      <td data-align="end">€2100.00</td>
      <td data-align="end"></td>
    </tr>
    <tr>
      <td>14/10</td>
      <td>Travel</td>
      <td data-align="end"></td>
      <td data-align="end">€86.40</td>
    </tr>
    <tr>
      <td>16/10</td>
      <td>Invoice INV044</td>
      <td data-align="end">€640.00</td>
      <td data-align="end"></td>
    </tr>
  </tbody>
  <tfoot>
    <tr>
      <td colspan="2">Total</td>
      <td data-align="end">€4470.00</td>
      <td data-align="end">€1156.40</td>
    </tr>
  </tfoot>
</sl:table>
```

A table prints at the size you gave it: for a report meant for paper, render it with `size="sm"`.

## Sorting on the server

The rows of a Spring application live on the server, which sends one page of them. So a sortable header is a link that
asks the server for the page in another order, not a script that reorders the rows in the browser:

**Example: Sorted on the server.** Sorted by name, in a request for /people?q=a&sort=name,asc. Each link keeps q.

```html
<sl:table th:page="${people}">
  <thead>
    <tr>
      <sl:table-head sort="name">Name</sl:table-head>
      <sl:table-head sort="email">Email</sl:table-head>
      <th data-align="end">Orders</th>
    </tr>
  </thead>
  <tbody>
    <tr th:each="person : ${people}">
      <td th:text="${person.name}">Ada Lovelace</td>
      <td th:text="${person.email}">ada@example.com</td>
      <td data-align="end" th:text="${person.orders}">3</td>
    </tr>
  </tbody>
</sl:table>
```

`<sl:table-head sort="name">` links to the same page with `sort=name,asc`, or `sort=name,desc` when the table is
sorted by name ascending already. The link keeps every other parameter in the address (a search, the page size) and
drops the page number, so a new order starts on the first page. The sorted column gets `aria-sort`, which screen
readers announce, and an arrow; the others show both chevrons. A click on the sorted column reverses it; there is no
unsorted state, because the default order is the controller's.

The controller takes a `Pageable`, which Spring Data fills from those parameters, and puts the page in the model:

```java
@GetMapping("/people")
String people(@PageableDefault(sort = "name") Pageable pageable, Model model) {
  model.addAttribute("people", personRepository.findAll(pageable));
  return "people";
}
```

```html
<sl:table th:page="${people}">
  <thead>
    <tr>
      <sl:table-head sort="name">Name</sl:table-head>
      <sl:table-head sort="email">Email</sl:table-head>
    </tr>
  </thead>
  ...
</sl:table>
```

`th:page` takes the `Page` (or `Slice`), whose `Sort` tells the headers how the rows are sorted, the default from
`@PageableDefault` included. Only the first sort key is shown. `spring-data-commons` is all it needs, not a Spring Data
module: rows that do not come from a repository can be wrapped in a
`new PageImpl<>(rows, pageable, total)`.

Without `th:page` the headers read the order from the request's `sort` parameter, so a table sorts without Spring Data
too; the controller then reads that parameter itself.

### Parameter names

The links use the parameter names your `Pageable` reads. Shadleaf takes them from Spring Boot's properties:
`spring.data.web.sort.sort-parameter`, and for the page and size links
`spring.data.web.pageable.page-parameter`, `size-parameter`, `prefix`, `qualifier-delimiter` and
`one-indexed-parameters`. If you configure Spring Data's resolvers in code instead, declare a `PagingParameters` bean
with the same values.

### Two tables on one page

Two sortable tables on one page need their own parameters. Give the controller's second `Pageable` a `@Qualifier`, and
the table the same `qualifier`:

```java
@GetMapping("/team")
String team(Pageable pageable, @Qualifier("members") Pageable members, Model model) { ... }
```

```html
<sl:table th:page="${members}" qualifier="members"> ... </sl:table>
```

Its headers then link to `members_sort=email,asc` and leave the other table's `sort` and `page` alone.

### Limit what can be sorted

The sort parameter is part of the address, so anyone can edit it. Spring Data passes an unknown property on to the
repository, which fails with an error page (`?sort=password` is an error at best). So the controller checks the order
against the columns the table offers before it queries, and falls back to the default order when it asks for anything
else:

```java
@Controller
class PeopleController {

  // The properties of the table's <sl:table-head sort="..."> columns.
  private static final Set<String> SORTABLE = Set.of("name", "email", "orders");

  private final PersonRepository personRepository;

  PeopleController(PersonRepository personRepository) {
    this.personRepository = personRepository;
  }

  @GetMapping("/people")
  String people(@PageableDefault(sort = "name") Pageable pageable, Model model) {
    model.addAttribute("people", personRepository.findAll(sortable(pageable)));
    return "people";
  }

  /** The pageable as requested, or sorted by name when it names a property the table does not offer. */
  private static Pageable sortable(Pageable pageable) {
    boolean allowed = pageable.getSort().stream().allMatch(order -> SORTABLE.contains(order.getProperty()));
    return allowed
        ? pageable
        : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by("name"));
  }
}
```

The page the repository returns carries the order it was sorted by, so the table shows the fallback correctly too.

### A stable order across pages

Rows with the same value in the sorted column (two people with 59 orders) come back from the database in no particular
order, and not necessarily the same one for every page. Once the table has [pages](https://shadleaf.dev/current/components/pagination.md), such a row can
show up on two pages, or on none. Add a unique key the reader does not see, such as the id, as the last sort key before
you query:

```java
@GetMapping("/people")
String people(@PageableDefault(sort = "name") Pageable pageable, Model model) {
  Pageable stable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
      sortable(pageable).getSort().and(Sort.by("id")));
  model.addAttribute("people", personRepository.findAll(stable));
  return "people";
}
```

The headers show only the first sort key, and the links are built from the request rather than from the page's
`Sort`, so the id never shows up in the address. Sample-01's `/people` page does this, and its test follows Next from
the first page to the last and finds every person exactly once.

### Sorting an enum column

An enum stored as a string (`@Enumerated(EnumType.STRING)`) sorts alphabetically: Admin, Guest, Member, Owner. To sort
roles by rank, sort the database by the rank:

- **Store the rank.** An `AttributeConverter` writes the enum as a number, so `sort=role` sorts by rank and the header
  needs nothing else. Sample-01 does this:

  ```java
  public enum Role {
    OWNER(1), ADMIN(2), MEMBER(3), GUEST(4);
    ...
  }

  @Converter
  class RoleConverter implements AttributeConverter<Role, Integer> {
    public Integer convertToDatabaseColumn(Role role) { return role == null ? null : role.getRank(); }
    public Role convertToEntityAttribute(Integer rank) { return rank == null ? null : Role.ofRank(rank); }
  }
  ```

  ```java
  @Convert(converter = RoleConverter.class)
  private Role role;
  ```

  An explicit rank, unlike `EnumType.ORDINAL`, survives reordering the enum's constants.
- **Sort by an expression.** Keep the string column and order by a `CASE` expression in a query of your own
  (`order by case p.role when 'OWNER' then 1 when 'ADMIN' then 2 ... end`), or with `JpaSort.unsafe(...)`. The
  controller then maps the `role` property of the requested `Sort` to that expression, and the `Page` it gets back
  carries the expression, not `role`: build the page the template gets with the requested `Pageable`
  (`new PageImpl<>(page.getContent(), pageable, page.getTotalElements())`) so the header still shows the order.

### Focus after an htmx swap

Each sort link has an id (`sort-name`, or `members-sort-name` with a qualifier), so when htmx replaces the table it
gives the focus back to the link that was clicked. [Tables with htmx](https://shadleaf.dev/current/guides/tables-htmx.md) puts it together.

## Search and filters

A search or filter above the table is a `GET` form the controller reads (`?q=ada`). Put
[`<sl:query-params except="q"/>`](https://shadleaf.dev/current/components/query-params.md) in it, so sending it keeps the table's order and page size and
starts on the first page; the sort and page links keep `q` by themselves. To search as the user types, see
[Tables with htmx](https://shadleaf.dev/current/guides/tables-htmx.md).

## Nothing found

`<sl:table-empty>` is the row to show when there are no rows: one cell across the table's `colspan` columns, saying
"No results." (the message `sl.table.empty`, which your `messages.properties` can [reword or translate](https://shadleaf.dev/current/guides/translating.md)). Content of its own replaces
the text.

**Example: Nothing found.**

```html
<sl:table>
  <thead>
    <tr>
      <th>Name</th>
      <th>Email</th>
      <th>Role</th>
    </tr>
  </thead>
  <tbody>
    <tr th:each="person : ${people}">
      <td th:text="${person.name}">Ada Lovelace</td>
      <td th:text="${person.email}">ada@example.com</td>
      <td th:text="${person.role}">Owner</td>
    </tr>
    <sl:table-empty colspan="3" th:if="${people.empty}"/>
  </tbody>
</sl:table>
```

```html
<sl:table-empty colspan="3" th:if="${people.empty}">Nobody matches your search.</sl:table-empty>
```

## Actions per row

Put a [dropdown menu](https://shadleaf.dev/current/components/dropdown-menu.md) in the last cell. Give each menu an id built
from its row, so every trigger has a stable id of its own (`people-12-actions-trigger`), and name the trigger after the
row: a screen reader user tabbing through the table hears "Actions for Ada Lovelace, menu button". The header cell
above the menus needs a name too, read out with each cell: `<span class="sl-sr-only">Actions</span>`.

**Example: Actions per row.** A menu in the last cell of each row, named after its row. In an application its items post or link (see Tables with htmx); here they do nothing.

```html
<sl:table>
  <thead>
    <tr>
      <th>Name</th>
      <th>Role</th>
      <th data-align="end"><span class="sl-sr-only">Actions</span></th>
    </tr>
  </thead>
  <tbody>
    <tr th:each="person : ${people}">
      <td th:text="${person.name}">Ada Lovelace</td>
      <td><sl:badge variant="secondary" th:text="${person.role.label}">Owner</sl:badge></td>
      <td data-align="end">
        <!--/* A role="menu" holds menu items only, so the form goes around the menu. */-->
        <form method="post" th:action="${selfUrl}">
          <input type="hidden" name="person" th:value="${person.id}">
          <sl:dropdown-menu th:id="|people-${person.id}-actions|">
            <sl:dropdown-menu-trigger variant="ghost" size="icon-sm"
                                      th:aria-label="|Actions for ${person.name}|">
              <sl:icon name="ellipsis"/>
            </sl:dropdown-menu-trigger>
            <sl:dropdown-menu-content align="end">
              <sl:dropdown-menu-item as="a" th:href="@{/people/{id}(id=${person.id})}">
                <sl:icon name="eye"/>View
              </sl:dropdown-menu-item>
              <sl:dropdown-menu-separator/>
              <sl:dropdown-menu-label>Change role</sl:dropdown-menu-label>
              <sl:dropdown-menu-radio-group aria-label="Change role">
                <sl:dropdown-menu-radio-item th:each="role : ${roles}" type="submit" name="role"
                                             th:value="${role}" th:checked="${person.role == role}">
                  [[${role.label}]]
                </sl:dropdown-menu-radio-item>
              </sl:dropdown-menu-radio-group>
              <sl:dropdown-menu-separator/>
              <sl:dropdown-menu-item as="a" variant="destructive"
                                     th:href="@{/people/{id}/delete(id=${person.id})}">
                <sl:icon name="trash-2"/>Delete
              </sl:dropdown-menu-item>
            </sl:dropdown-menu-content>
          </sl:dropdown-menu>
        </form>
      </td>
    </tr>
  </tbody>
</sl:table>
```

An item that changes something is a submit button of a `post` form. A `role="menu"` may only hold menu items, so the
form goes around the menu: a hidden input names the row, and each item sends its own `name` and `value` (`role=ADMIN`).
View is a link; Delete is a link to a page that asks first. [Tables with htmx](https://shadleaf.dev/current/guides/tables-htmx.md#actions-per-row)
turns the same menu into htmx requests, with an alert dialog for Delete, and keeps the focus on the row's menu button.

A menu per row is markup per row: 50 rows with a badge and the menu above render in about 9 ms and 222 KB of HTML,
5 KB once compressed (see [the cost](https://shadleaf.dev/current/guides/tables-htmx.md#the-cost-of-a-menu-per-row)).

## Selecting rows

For a bulk action (delete, change a role), give each row a checkbox of your own and put
`<sl:table-select-all/>` in the header cell above them. Select-all checks or unchecks every row on the page, and is
checked when they all are, indeterminate (a minus) when some are. `<sl:table-selection-count>` says how many are
selected; place it where it fits, e.g. at the start of the footer.

**Example: Selecting rows.** The rows are plain checkboxes named ids; one is selected, so select-all starts indeterminate.

```html
<sl:table id="people-selection">
  <thead>
    <tr>
      <th><sl:table-select-all/></th>
      <th>Name</th>
      <th>Email</th>
    </tr>
  </thead>
  <tbody>
    <tr th:each="person : ${people}">
      <td><sl:checkbox name="ids" th:value="${person.id}"
                       th:aria-label="#{sl.table.select-row(${person.name})}"/></td>
      <td th:text="${person.name}">Ada Lovelace</td>
      <td th:text="${person.email}">ada@example.com</td>
    </tr>
  </tbody>
</sl:table>
<sl:table-selection-count for="people-selection"/>
```

```html
<form id="people-bulk" method="post" th:action="${selfUrl}">
  <sl:table id="people-table" th:page="${people}">
    <thead>
      <tr>
        <th><sl:table-select-all/></th>
        <sl:table-head sort="name">Name</sl:table-head>
        <sl:table-head sort="email">Email</sl:table-head>
      </tr>
    </thead>
    <tbody>
      <tr th:each="person : ${people}">
        <td><sl:checkbox name="ids" th:value="${person.id}"
                         th:aria-label="#{sl.table.select-row(${person.name})}"/></td>
        <td th:text="${person.name}">Ada Lovelace</td>
        <td th:text="${person.email}">ada@example.com</td>
      </tr>
    </tbody>
  </sl:table>
  <sl:button type="submit" variant="destructive">Delete selected</sl:button>
</form>
<div class="table-footer">
  <sl:table-selection-count for="people-table"/>
  <sl:pagination th:page="${people}"/>
</div>
```

- **The rows are yours.** Plain `sl:checkbox`es (`name="ids"`, the row's id as value) submit with the form, also
  without JavaScript; the controller gets `@RequestParam List<Long> ids`. The table can sit inside the form, or the
  checkboxes can name a form elsewhere with `form="..."`.
- **Which checkboxes are rows.** Select-all works on the checkboxes in the body cells of the table it is in (not of a
  table nested in a cell), leaving out disabled ones, which would not be submitted. When the body has other
  checkboxes too, `name="ids"` on select-all narrows it to those.
- **The selected look** comes from CSS: a row whose first cell holds a checked checkbox gets the muted background,
  with or without JavaScript. Put the selection in the first column.
- **Names.** Every row checkbox needs an accessible name, telling the rows apart. `aria-label` with the message
  `sl.table.select-row` (`Select {0}`) as above, or point at the cell that names the row:

  ```html
  <td><sl:checkbox name="ids" th:value="${person.id}"
                   th:aria-labelledby="|person-${person.id}-name|"/></td>
  <td th:id="|person-${person.id}-name|" th:text="${person.name}">Ada Lovelace</td>
  ```

  Select-all is named "Select all rows on this page" (`sl.table.select-all`); an `aria-label` of yours replaces it.
- **The count** reads "2 of 20 selected" (`sl.table.selection.count`; keep `{0}` and `{1}` as they are, the browser
  fills them in; see [Translating](https://shadleaf.dev/current/guides/translating.md)). Its `table` prop is the table's `id`, which `sl:table` puts on the `table`. It is not a live region:
  a row's checkbox announces its own state, so the count is only announced after a click on select-all, which
  changes rows the user did not touch.

### Without JavaScript

Select-all and the count need Alpine, so they keep their place but stay hidden until it starts (`x-cloak`). Without
JavaScript the user checks the rows one by one, and the form submits them as before.

### The bulk action

Post the form to the page's own URL, query string included, so the action knows the search, order and page to come
back to. The controller below puts that URL in the model; after the action it redirects back there with a toast
(post/redirect/get):

```java
@PostMapping("/people")
public String delete(@RequestParam(name = "ids", required = false) List<Long> ids,
    HttpServletRequest request, RedirectAttributes redirectAttributes) {
  repository.deleteAllByIdInBatch(ids == null ? List.of() : ids);
  redirectAttributes.addFlashAttribute("toasts", List.of(Toast.success("People deleted")));
  String query = request.getQueryString();
  return "redirect:/people" + (query == null ? "" : "?" + query);
}
```

Ask before a destructive action with an [alert dialog](https://shadleaf.dev/current/components/alert-dialog.md) inside the form: its destructive button is a
submit button. With htmx, answer with the table alone; [Tables with htmx](https://shadleaf.dev/current/guides/tables-htmx.md#selection-and-a-bulk-action)
shows how, and sample-01's `/people-multiselect` page does both.

### Selection across pages

Select-all selects the rows on the page, and a sort, another page or a new search (with htmx too) starts with nothing
selected: the selection lives in the checkboxes, and those are replaced. Keeping it across pages is the server's
business. Two ways:

- **Keep the ids on the server**, e.g. in the session: each page posts its selection (or a checkbox posts on change
  with htmx), and the page renders `checked` on the rows that were selected before. Select-all and the count then
  start from those.
- **Select everything that matches** instead of ids: after select-all, offer "Select all 270 people", which posts the
  search itself (`q`) for the action to apply to every match.

Sample-01 keeps the selection per page.

### JavaScript

`<sl:table-select-all>` renders `x-data="slTableSelection"` and `<sl:table-selection-count>`
`x-data="slTableSelectionCount"`. They need no Alpine plugin. They follow the rows' `change` events (Space or a click),
a form reset, and rows being added or replaced (an htmx swap). Changing the rows from select-all fires `input` and
`change` on every row it changes, as a click would.

| Event | On | Detail |
| --- | --- | --- |
| `sl-table-selection-change` | the `table`, bubbling | `{selected, total}`, after every change, and when select-all starts |

A bulk action can listen to it, for instance to show how many people it would delete:

```html
<div x-data="{selected: 0}" @sl-table-selection-change.document="selected = $event.detail.selected">...</div>
```

A checkbox of your own can be indeterminate too: set its `indeterminate` property, or render
[`<sl:checkbox indeterminate>`](https://shadleaf.dev/current/components/checkbox.md#indeterminate).

## Left out

The table is sorted, filtered and paged on the server. These are not in Shadleaf: sorting and filtering in the browser,
column visibility, resizing, reordering and pinning other than the first column (see above), drag handles, expandable
rows, editable cells, virtual scrolling, selecting a range of rows with Shift and a click, and a selection that lasts
across pages (see above).

## Attributes

### sl:table

Attributes of `<sl:table>`: A styled table. Write the plain table elements inside it (`caption`, `thead`, `tbody`, `tfoot`, `tr`, `th`, `td`), with `th:each` on a row; there is no component per row or cell. Use `sl:table-head` for a header cell that sorts or aligns, and `sl:table-empty` for the row that says nothing was found. A `colgroup` per group of columns draws a line between the groups. The table scrolls sideways inside its own container when it is wider than the page. Attributes go to the `table`.

| Attribute | Values | Default | Description |
| --- | --- | --- | --- |
| `th:page` | *object* | — | The Spring Data `Page` or `Slice` the table shows. Its `Sort` tells every `sl:table-head` inside how the rows are sorted now. Without it, the headers read the order from the request's sort parameter. |
| `qualifier` | *string* | — | The `@Qualifier` of the controller's `Pageable`, for a second table on the page: its sort links use `members_sort` instead of `sort`. |
| `sticky-first-column` | *boolean* | — | Pin the first cell of every row to the start edge, so it stays in view when a wide table scrolls sideways. It gets the page's `--background` (set `--sl-table-bg` on the table when it sits on something else, such as `var(--card)`), and an edge line appears once the table has scrolled. |
| `size` | `default`, `sm` | `default` | The density of the table. `sm` has smaller cells and text, for a table of many rows of figures or one meant for paper; the header, body, footer, caption, sort links and `sl:table-empty` row follow it. |

Every prop can also be set from an expression: `th:page="${...}"`. Any other attribute passes through to the rendered element, and `class` is appended.

### sl:table-head

Attributes of `<sl:table-head>`: A header cell (`th`) of a `sl:table`. With `sort` its label is a link that sorts the table by that property on the server: ascending, or descending when it is sorted ascending already. The link keeps the request's other parameters (filters, page size) and goes back to the first page. The sorted column gets `aria-sort` and an arrow. A plain `th` works too, for a header that does neither.

| Attribute | Values | Default | Description |
| --- | --- | --- | --- |
| `sort` | *string* | — | The property to sort by, as the controller's `Sort` names it (`name`, `address.city`). Only the first key of the current order is shown. |
| `align` | `start`, `center`, `end` | `start` | Where the label sits in the cell. Put the same `data-align` on the column's `td`s: `end` for amounts. |

Every prop can also be set from an expression: `th:sort="${...}"`. Any other attribute passes through to the rendered element, and `class` is appended.

### sl:table-empty

Attributes of `<sl:table-empty>`: The row a `sl:table` shows when there are no rows to show: one cell across every column saying "No results." (the message `sl.table.empty`). Content of its own replaces the text. Render it with `th:if`, e.g. `th:if="${people.empty}"`.

| Attribute | Values | Default | Description |
| --- | --- | --- | --- |
| `colspan` | *number* | **required** | The number of columns the cell spans: all of the table's. |

Every prop can also be set from an expression: `th:colspan="${...}"`. Any other attribute passes through to the rendered element, and `class` is appended.

### sl:table-select-all

Attributes of `<sl:table-select-all>`: A checkbox for a header cell of a `sl:table` that selects every row on the page. The rows are the application's own checkboxes (a `sl:checkbox` with `name="ids"` and `th:value="${person.id}"`) in the cells of the table's body; they submit without JavaScript. This one is checked when every row is, indeterminate when some are, and checks or unchecks them all when clicked; it is named "Select all rows on this page" (the message `sl.table.select-all`) unless it gets an `aria-label`. It needs JavaScript, so it stays hidden until Alpine starts. It fires `sl-table-selection-change` on the table (`{selected, total}`), which `sl:table-selection-count` shows. It submits nothing itself (no `name`); other attributes go to its `input`, `class` to its wrapper.

| Attribute | Values | Default | Description |
| --- | --- | --- | --- |
| `name` | *string* | — | The `name` of the row checkboxes, when the body has checkboxes of another kind too. Without it, every checkbox in a body cell is a row. |

Every prop can also be set from an expression: `th:name="${...}"`. Any other attribute passes through to the rendered element, and `class` is appended.

### sl:table-selection-count

Attributes of `<sl:table-selection-count>`: How many rows of a `sl:table` are selected: "2 of 20 selected" (the message `sl.table.selection.count`), kept up to date in the browser as rows are checked, as rows are replaced (an htmx swap) and after a form reset. Place it anywhere, e.g. at the start of the table's footer. It is not a live region: a click on `sl:table-select-all` announces the count, a row's checkbox announces its own state. Empty and hidden until Alpine starts.

| Attribute | Values | Default | Description |
| --- | --- | --- | --- |
| `for` | *string* | **required** | The `id` of the table whose rows it counts (`sl:table` puts its `id` on the `table`). |

Every prop can also be set from an expression: `th:for="${...}"`. Any other attribute passes through to the rendered element, and `class` is appended.
