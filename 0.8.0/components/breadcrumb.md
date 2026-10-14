# Breadcrumb

> The trail of pages above the one shown now, with a menu for the levels a long trail leaves out.

Source: https://wimdeblauwe.github.io/shadleaf/0.8.0/components/breadcrumb/

**Example: Breadcrumb.**

```html
<sl:breadcrumb>
  <sl:breadcrumb-list>
    <sl:breadcrumb-item><sl:breadcrumb-link href="/">Home</sl:breadcrumb-link></sl:breadcrumb-item>
    <sl:breadcrumb-separator/>
    <sl:breadcrumb-item><sl:breadcrumb-link href="/components">Components</sl:breadcrumb-link></sl:breadcrumb-item>
    <sl:breadcrumb-separator/>
    <sl:breadcrumb-item><sl:breadcrumb-page>Breadcrumb</sl:breadcrumb-page></sl:breadcrumb-item>
  </sl:breadcrumb-list>
</sl:breadcrumb>
```

## Built from parts

A breadcrumb is shadcn/ui's parts: a `nav` holding an ordered list of levels, with a separator between them. The last
level is the page shown now.

```html
<sl:breadcrumb>
  <sl:breadcrumb-list>
    <sl:breadcrumb-item><sl:breadcrumb-link th:href="@{/}">Home</sl:breadcrumb-link></sl:breadcrumb-item>
    <sl:breadcrumb-separator/>
    <sl:breadcrumb-item><sl:breadcrumb-link th:href="@{/components}">Components</sl:breadcrumb-link></sl:breadcrumb-item>
    <sl:breadcrumb-separator/>
    <sl:breadcrumb-item><sl:breadcrumb-page>Breadcrumb</sl:breadcrumb-page></sl:breadcrumb-item>
  </sl:breadcrumb-list>
</sl:breadcrumb>
```

`<sl:breadcrumb-link>` is a link: give it an `href` or `th:href`. `<sl:breadcrumb-page>` is plain text with
`aria-current="page"`. The list wraps onto a second line when there is no room, as on a phone.

In an [application shell](https://wimdeblauwe.github.io/shadleaf/0.8.0/components/sidebar.md) the breadcrumb goes in the page header, after the sidebar's trigger and a
vertical [separator](https://wimdeblauwe.github.io/shadleaf/0.8.0/components/separator.md).

## A level without a page

A level that has no page of its own, such as a group in the sidebar, is plain text in its item:

**Example: A level without a page.** A level that has no page of its own is plain text in its item.

```html
<sl:breadcrumb aria-label="Breadcrumb with a plain level">
  <sl:breadcrumb-list>
    <sl:breadcrumb-item><sl:breadcrumb-link href="/">Home</sl:breadcrumb-link></sl:breadcrumb-item>
    <sl:breadcrumb-separator/>
    <sl:breadcrumb-item>Settings</sl:breadcrumb-item>
    <sl:breadcrumb-separator/>
    <sl:breadcrumb-item><sl:breadcrumb-page>Notifications</sl:breadcrumb-page></sl:breadcrumb-item>
  </sl:breadcrumb-list>
</sl:breadcrumb>
```

## From a list

There is no mode that renders the levels from a list: write them with `th:each`, and the page shown now after them.
`th:block` keeps item and separator together:

```html
<sl:breadcrumb>
  <sl:breadcrumb-list>
    <th:block th:each="level : ${levels}">
      <sl:breadcrumb-item><sl:breadcrumb-link th:href="${level.url}">[[${level.name}]]</sl:breadcrumb-link></sl:breadcrumb-item>
      <sl:breadcrumb-separator/>
    </th:block>
    <sl:breadcrumb-item><sl:breadcrumb-page th:text="${title}">Title</sl:breadcrumb-page></sl:breadcrumb-item>
  </sl:breadcrumb-list>
</sl:breadcrumb>
```

## Collapsed levels

A long trail can leave levels out. An empty `<sl:breadcrumb-ellipsis>` stands for them; a screen reader says "More
levels".

**Example: Collapsed levels.** An ellipsis without content stands for the levels left out; a screen reader says "More levels".

```html
<sl:breadcrumb aria-label="Breadcrumb with collapsed levels">
  <sl:breadcrumb-list>
    <sl:breadcrumb-item><sl:breadcrumb-link href="/">Home</sl:breadcrumb-link></sl:breadcrumb-item>
    <sl:breadcrumb-separator/>
    <sl:breadcrumb-item><sl:breadcrumb-ellipsis/></sl:breadcrumb-item>
    <sl:breadcrumb-separator/>
    <sl:breadcrumb-item><sl:breadcrumb-link href="/components">Components</sl:breadcrumb-link></sl:breadcrumb-item>
    <sl:breadcrumb-separator/>
    <sl:breadcrumb-item><sl:breadcrumb-page>Breadcrumb</sl:breadcrumb-page></sl:breadcrumb-item>
  </sl:breadcrumb-list>
</sl:breadcrumb>
```

### With a menu

Give the ellipsis the levels it stands for, as [dropdown menu](https://wimdeblauwe.github.io/shadleaf/0.8.0/components/dropdown-menu.md) items, and it becomes a ghost icon
button that opens a menu of them, as in shadcn's example:

**Example: With a menu.** An ellipsis with the hidden levels as its content is a button that opens a menu of them. Arrow keys move through the menu, Escape closes it.

```html
<sl:breadcrumb aria-label="Breadcrumb with a menu">
  <sl:breadcrumb-list>
    <sl:breadcrumb-item><sl:breadcrumb-link href="/">Home</sl:breadcrumb-link></sl:breadcrumb-item>
    <sl:breadcrumb-separator/>
    <sl:breadcrumb-item>
      <sl:breadcrumb-ellipsis>
        <sl:dropdown-menu-item as="a" href="/docs">Documentation</sl:dropdown-menu-item>
        <sl:dropdown-menu-item as="a" href="/themes">Themes</sl:dropdown-menu-item>
        <sl:dropdown-menu-item as="a" href="/github">GitHub</sl:dropdown-menu-item>
      </sl:breadcrumb-ellipsis>
    </sl:breadcrumb-item>
    <sl:breadcrumb-separator/>
    <sl:breadcrumb-item><sl:breadcrumb-link href="/components">Components</sl:breadcrumb-link></sl:breadcrumb-item>
    <sl:breadcrumb-separator/>
    <sl:breadcrumb-item><sl:breadcrumb-page>Breadcrumb</sl:breadcrumb-page></sl:breadcrumb-item>
  </sl:breadcrumb-list>
</sl:breadcrumb>
```

```html
<sl:breadcrumb-item>
  <sl:breadcrumb-ellipsis>
    <sl:dropdown-menu-item as="a" href="/docs">Documentation</sl:dropdown-menu-item>
    <sl:dropdown-menu-item as="a" href="/themes">Themes</sl:dropdown-menu-item>
  </sl:breadcrumb-ellipsis>
</sl:breadcrumb-item>
```

The button is named "Show more levels"; an `aria-label` replaces it, and any other attribute goes to the button too.
The menu's id is `breadcrumb-more` (the button's `breadcrumb-more-trigger`): give a second breadcrumb with a menu on the
same page another `id`. The menu behaves as every [dropdown menu](https://wimdeblauwe.github.io/shadleaf/0.8.0/components/dropdown-menu.md): arrow keys, Home, End and
typeahead move through the levels, Enter follows one, Escape closes it and gives the focus back to the button.

## Separator

The separator is a chevron that points to the next level: to the left on a right-to-left page (`dir="rtl"`). Content
of its own replaces it, such as another icon; that content is not turned around.

**Example: Another separator.** Content of its own replaces the chevron.

```html
<sl:breadcrumb aria-label="Breadcrumb with slashes">
  <sl:breadcrumb-list>
    <sl:breadcrumb-item><sl:breadcrumb-link href="/">Home</sl:breadcrumb-link></sl:breadcrumb-item>
    <sl:breadcrumb-separator><sl:icon name="slash"/></sl:breadcrumb-separator>
    <sl:breadcrumb-item><sl:breadcrumb-link href="/components">Components</sl:breadcrumb-link></sl:breadcrumb-item>
    <sl:breadcrumb-separator><sl:icon name="slash"/></sl:breadcrumb-separator>
    <sl:breadcrumb-item><sl:breadcrumb-page>Breadcrumb</sl:breadcrumb-page></sl:breadcrumb-item>
  </sl:breadcrumb-list>
</sl:breadcrumb>
```

## Accessibility

- The `nav` is named "Breadcrumb" (`sl.breadcrumb.label`); an `aria-label` of your own replaces it, which two
  breadcrumbs on one page need.
- The separators are list items hidden from assistive technology, so a screen reader counts only the levels.
- The page shown now has `aria-current="page"` and is not a link. shadcn's adds `role="link"` and
  `aria-disabled="true"`, which a screen reader announces as a link that goes nowhere.
- The empty ellipsis reads "More levels", where shadcn/ui hides it from screen readers.
- In Windows' high-contrast mode a focused link gets an outline, as the focus ring (a shadow) is gone there.

## Messages

| Key | English |
| --- | --- |
| `sl.breadcrumb.label` | Breadcrumb |
| `sl.breadcrumb.more` | More levels |
| `sl.breadcrumb.more.trigger` | Show more levels |

## Left out

A breadcrumb built from the request's path or from the sidebar's menu, and structured data (`BreadcrumbList`) for
search engines.

## Attributes

### sl:breadcrumb

Attributes of `<sl:breadcrumb>`: The trail of pages above the one shown now, in a `nav` named "Breadcrumb" (the message `sl.breadcrumb.label`; an `aria-label` replaces it). Holds a `sl:breadcrumb-list` with `sl:breadcrumb-item`s, each a `sl:breadcrumb-link` or, last, the `sl:breadcrumb-page`, with a `sl:breadcrumb-separator` between them.

`sl:breadcrumb` declares no props; every attribute passes through.

### sl:breadcrumb-list

Attributes of `<sl:breadcrumb-list>`: The ordered list of a `sl:breadcrumb`: its items and the separators between them, on one line that wraps when there is no room.

`sl:breadcrumb-list` declares no props; every attribute passes through.

### sl:breadcrumb-item

Attributes of `<sl:breadcrumb-item>`: One level of a `sl:breadcrumb-list`: a `sl:breadcrumb-link`, the `sl:breadcrumb-page`, a `sl:breadcrumb-ellipsis`, or plain text for a level that has no page of its own.

`sl:breadcrumb-item` declares no props; every attribute passes through.

### sl:breadcrumb-link

Attributes of `<sl:breadcrumb-link>`: A link to a level above the page shown now. Give it an `href` or `th:href`. A level without a page of its own is plain text in its `sl:breadcrumb-item` instead.

`sl:breadcrumb-link` declares no props; every attribute passes through.

### sl:breadcrumb-page

Attributes of `<sl:breadcrumb-page>`: The page shown now, last in the trail: text in the foreground colour, marked `aria-current="page"`. Not a link.

`sl:breadcrumb-page` declares no props; every attribute passes through.

### sl:breadcrumb-separator

Attributes of `<sl:breadcrumb-separator>`: The mark between two items of a `sl:breadcrumb-list`: a chevron that points the other way on a right-to-left page. Content of its own (another icon, a slash) replaces it; that content is not turned around. Decorative: screen readers skip it, so the list holds only the levels.

`sl:breadcrumb-separator` declares no props; every attribute passes through.

### sl:breadcrumb-ellipsis

Attributes of `<sl:breadcrumb-ellipsis>`: Stands for the levels a long trail leaves out. Without content it is an ellipsis, read as "More levels" (the message `sl.breadcrumb.more`). With content, the `sl:dropdown-menu-item`s of those levels (`as="a"`), it is a ghost icon button named "Show more levels" (`sl.breadcrumb.more.trigger`; an `aria-label` replaces it) that opens a `sl:dropdown-menu` of them; any other attribute then goes to the button.

| Attribute | Values | Default | Description |
| --- | --- | --- | --- |
| `id` | *string* | — | With content: the menu's id, `breadcrumb-more` when not set (the button's is `&lt;id&gt;-trigger`); give a second breadcrumb on the page another one. Without content: the ellipsis's own id. |

Every prop can also be set from an expression: `th:id="${...}"`. Any other attribute passes through to the rendered element, and `class` is appended.

## JavaScript

Only the ellipsis with a menu needs any: it is a [dropdown menu](https://wimdeblauwe.github.io/shadleaf/0.8.0/components/dropdown-menu.md#javascript), which needs Alpine and
no plugin. Without it the button still opens the menu (the browser's popover), without the arrow keys.
