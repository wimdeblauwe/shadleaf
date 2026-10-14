# Query params

> Hidden inputs that keep a table's order, page size and filters when a search form is sent.

Source: https://wimdeblauwe.github.io/shadleaf/0.9.0-SNAPSHOT/components/query-params/

**Example: Query params.** In a request for /people?q=ada&sort=email,desc&size=50&page=2. Sending the form asks for /people?q=…&sort=email,desc&size=50: the order and the size stay, the page starts again at the first.

```html
<form action="/people" method="get">
  <sl:label for="query-params-q">Search people</sl:label>
  <sl:input type="search" id="query-params-q" name="q" th:value="${param.q}" placeholder="Name or email"/>
  <sl:query-params except="q"/>
</form>
```

## Searching a sorted table

A search above a [table](https://wimdeblauwe.github.io/shadleaf/0.9.0-SNAPSHOT/components/table.md) is a `GET` form: sending it asks for the same page with `?q=...`, which the
controller filters on. A browser sends only the form's own fields, so on its own the form would forget the table's
order and page size. `<sl:query-params>` copies the current request's other parameters into the form as hidden inputs:

```html
<form th:action="@{/people}" method="get" role="search">
  <sl:input type="search" name="q" th:value="${param.q}" aria-label="Search people"/>
  <sl:query-params except="q"/>
</form>
```

In a request for `/people?q=ada&sort=email,desc&size=50&page=2`, that renders:

```html
<form action="/people" method="get" role="search">
  <input class="input" type="search" name="q" value="ada" aria-label="Search people">
  <input type="hidden" name="sort" value="email,desc">
  <input type="hidden" name="size" value="50">
</form>
```

and searching for "grace" asks for `/people?q=grace&sort=email,desc&size=50`.

- `except` names the form's own fields, separated by commas (`except="q,status"`). Without it, `q` would be sent twice:
  the field's new value and the hidden copy of the old one.
- The page number is always left out, so a new search starts on the first page. It is the page parameter your
  `Pageable` reads (`spring.data.web.pageable.*`, see [parameter names](https://wimdeblauwe.github.io/shadleaf/0.9.0-SNAPSHOT/components/table.md#parameter-names)); with
  `qualifier="members"` it is `members_page`, for the [second table](https://wimdeblauwe.github.io/shadleaf/0.9.0-SNAPSHOT/components/table.md#two-tables-on-one-page) on a page.
- A repeated parameter stays repeated, in its order: `?sort=name,desc&sort=email` gives two `sort` inputs, as Spring
  Data's `Sort` reads them.
- Values are decoded once (`?q=ada%20l%C3%A9` gives `value="ada lé"`) and escaped like any other attribute value, so a
  parameter cannot add markup.

The controller needs nothing for it: the `Pageable` reads `sort` and `size` as it does for the table's links.

```java
@GetMapping("/people")
String people(@RequestParam(required = false) String q, @PageableDefault(sort = "name") Pageable pageable,
    Model model) {
  Page<Person> people = q == null || q.isBlank()
      ? personRepository.findAll(pageable)
      : personRepository.findByNameContainingIgnoreCaseOrEmailContainingIgnoreCase(q, q, pageable);
  model.addAttribute("people", people);
  return "people";
}
```

The links of the table's headers, its [pagination](https://wimdeblauwe.github.io/shadleaf/0.9.0-SNAPSHOT/components/pagination.md) and the rows-per-page menu keep `q` in the same
way, as they are built from the request too.

## Outside the form

Other attributes go to every hidden input. With `form`, the inputs belong to a form elsewhere on the page. The
[live search with htmx](https://wimdeblauwe.github.io/shadleaf/0.9.0-SNAPSHOT/guides/tables-htmx.md) uses this to put them next to the table, so they are swapped with it
and follow the order of the table they came with:

```html
<form id="people-search" ...>...</form>
<div id="people-results">
  <sl:query-params except="q" form="people-search"/>
  <sl:table th:page="${people}">...</sl:table>
</div>
```

## Attributes

Attributes of `<sl:query-params>`: Hidden inputs that copy the current request's query parameters into a `GET` form, so sending a search keeps the table's order, page size and other filters. Name the form's own fields in `except`, or they are sent twice. The page number is always left out, so a new search starts on the first page. Values are decoded once and escaped like any other attribute; a repeated parameter (several `sort` values) stays repeated. Renders no element of its own. Other attributes go to every input: `form="search"` ties them to a form elsewhere on the page.

| Attribute | Values | Default | Description |
| --- | --- | --- | --- |
| `except` | *string* | — | The parameters not to copy, separated by commas: the fields the form has itself (`q`, `q,status`). |
| `qualifier` | *string* | — | The `@Qualifier` of the controller's `Pageable`, for a second table on the page: its page parameter (`members_page`) is the one left out. |

Every prop can also be set from an expression: `th:except="${...}"`. Any other attribute passes through to the rendered element, and `class` is appended.
