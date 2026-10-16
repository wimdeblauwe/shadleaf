# Dropdown Menu

> A menu of actions or links that opens from a button, on the browser's Popover API.

Source: https://shadleaf.dev/0.10.0-SNAPSHOT/components/dropdown-menu/

**Example: Dropdown menu.** Labels, groups, separators and shortcuts; links and buttons; a disabled item.

```html
<sl:dropdown-menu id="account-menu">
  <sl:dropdown-menu-trigger variant="outline">Open</sl:dropdown-menu-trigger>
  <sl:dropdown-menu-content>
    <sl:dropdown-menu-label>My Account</sl:dropdown-menu-label>
    <sl:dropdown-menu-group>
      <sl:dropdown-menu-item as="a" href="/profile">
        Profile <sl:dropdown-menu-shortcut>⇧⌘P</sl:dropdown-menu-shortcut>
      </sl:dropdown-menu-item>
      <sl:dropdown-menu-item as="a" href="/billing">
        Billing <sl:dropdown-menu-shortcut>⌘B</sl:dropdown-menu-shortcut>
      </sl:dropdown-menu-item>
      <sl:dropdown-menu-item as="a" href="/settings">
        Settings <sl:dropdown-menu-shortcut>⌘S</sl:dropdown-menu-shortcut>
      </sl:dropdown-menu-item>
    </sl:dropdown-menu-group>
    <sl:dropdown-menu-separator/>
    <sl:dropdown-menu-group>
      <sl:dropdown-menu-item as="a" href="/team">Team</sl:dropdown-menu-item>
      <sl:dropdown-menu-item>
        New Team <sl:dropdown-menu-shortcut>⌘T</sl:dropdown-menu-shortcut>
      </sl:dropdown-menu-item>
    </sl:dropdown-menu-group>
    <sl:dropdown-menu-separator/>
    <sl:dropdown-menu-item as="a" href="https://github.com/wimdeblauwe/shadleaf">GitHub</sl:dropdown-menu-item>
    <sl:dropdown-menu-item as="a" href="/support">Support</sl:dropdown-menu-item>
    <sl:dropdown-menu-item disabled>API</sl:dropdown-menu-item>
    <sl:dropdown-menu-separator/>
    <sl:dropdown-menu-item type="submit" form="logout-form">
      Log out <sl:dropdown-menu-shortcut>⇧⌘Q</sl:dropdown-menu-shortcut>
    </sl:dropdown-menu-item>
  </sl:dropdown-menu-content>
</sl:dropdown-menu>
<form id="logout-form" method="post" action="/logout"></form>
```

## Built from parts

A dropdown menu is `<sl:dropdown-menu>` with a `<sl:dropdown-menu-trigger>` and a `<sl:dropdown-menu-content>`
holding the items, as in shadcn/ui. `<sl:dropdown-menu>` renders no element of its own: it gives the parts their ids.

```html
<sl:dropdown-menu id="account-menu">
  <sl:dropdown-menu-trigger variant="outline">Open</sl:dropdown-menu-trigger>
  <sl:dropdown-menu-content>
    <sl:dropdown-menu-label>My Account</sl:dropdown-menu-label>
    <sl:dropdown-menu-group>
      <sl:dropdown-menu-item as="a" th:href="@{/profile}">
        Profile <sl:dropdown-menu-shortcut>⇧⌘P</sl:dropdown-menu-shortcut>
      </sl:dropdown-menu-item>
      <sl:dropdown-menu-item as="a" th:href="@{/settings}">Settings</sl:dropdown-menu-item>
    </sl:dropdown-menu-group>
    <sl:dropdown-menu-separator/>
    <sl:dropdown-menu-item type="submit" form="logout-form">Log out</sl:dropdown-menu-item>
  </sl:dropdown-menu-content>
</sl:dropdown-menu>
```

The menu is a [popover](https://developer.mozilla.org/en-US/docs/Web/API/Popover_API), so the browser does the hard
parts: the menu is drawn above everything else (no `z-index` to manage, no portal), Escape or a click outside closes
it, and closing returns focus to the trigger. The trigger is a `<sl:button>` with `popovertarget` set to the menu's
`id`; it takes the button's `variant` and `size`, its `icon-start` and `icon-end` slots, and any other attribute passes
through. Any other button can open the menu the same way: `<sl:button popovertarget="account-menu">`.

The menu is labelled by its trigger. A menu opened by a button of your own needs an `aria-label` on
`<sl:dropdown-menu-content>`.

## Opened from the sidebar

`as` makes the trigger a sidebar part instead of a button: shadcn's `<DropdownMenuTrigger asChild>` around a
`SidebarMenuButton` or a `SidebarMenuAction`.

| `as` | Renders | For |
| --- | --- | --- |
| `button` (default) | `<sl:button>` | Any menu |
| `sidebar-menu-button` | `<sl:sidebar-menu-button as="button">`, with its `variant`, `size`, `tooltip` and `icon-start`/`icon-end` slots | A team or version switcher in the sidebar header, a user menu in its footer |
| `sidebar-menu-action` | `<sl:sidebar-menu-action>`, with `show-on-hover` | "More" beside a project's link |

```html
<sl:sidebar-menu-item>
  <sl:dropdown-menu id="team-switcher">
    <sl:dropdown-menu-trigger as="sidebar-menu-button" size="lg" tooltip="Acme Inc.">
      <sl:slot name="icon-start"><sl:icon name="gallery-vertical-end"/></sl:slot>
      <strong>Acme Inc.</strong>
      <small>Enterprise</small>
      <sl:slot name="icon-end"><sl:icon name="chevrons-up-down"/></sl:slot>
    </sl:dropdown-menu-trigger>
    <sl:dropdown-menu-content side="right">...</sl:dropdown-menu-content>
  </sl:dropdown-menu>
</sl:sidebar-menu-item>
```

The trigger is the same element the part renders on its own, with the menu's `popovertarget`, `aria-haspopup` and
id, so it looks and lines up like the entries around it. The [Sidebar](https://shadleaf.dev/0.10.0-SNAPSHOT/components/sidebar.md#menus-in-the-sidebar) page shows
the team switcher, the version switcher and the "More" menu, and where each opens: beside the sidebar on a desktop
(`side="right"`), below its trigger in the phone panel and in the header layout's row.

## Items: links and buttons

An item is a button, or with `as="a"` a link. Every attribute that is not a prop passes through, so an item does
anything a button or link does, and the menu closes once it is chosen, with focus back on the trigger:

| Item | What it does |
| --- | --- |
| `<sl:dropdown-menu-item as="a" th:href="@{/orders/{id}(id=${order.id})}">` | Navigates. |
| `<sl:dropdown-menu-item hx:post="@{/orders/{id}/duplicate(id=${order.id})}">` | Sends an htmx request. |
| `<sl:dropdown-menu-item type="submit" form="logout-form">` | Submits a form (a POST logout). |
| `<sl:dropdown-menu-item commandfor="rename-dialog" command="show-modal">` | Opens a [dialog](https://shadleaf.dev/0.10.0-SNAPSHOT/components/dialog.md); closing it returns focus to the menu's trigger. |

`variant="destructive"` is for an item that deletes something. `disabled` disables an item: the arrow keys skip it, a
disabled button cannot be clicked (htmx included), and a disabled link loses its `href`. `inset` indents an item (or a
label) to line up with items that start with an icon.

**Example: Icons and a destructive item.** An icon at the start of an item; variant="destructive" for one that deletes.

```html
<sl:dropdown-menu id="file-menu">
  <sl:dropdown-menu-trigger variant="outline">File</sl:dropdown-menu-trigger>
  <sl:dropdown-menu-content>
    <sl:dropdown-menu-item><sl:icon name="file"/>New file</sl:dropdown-menu-item>
    <sl:dropdown-menu-item><sl:icon name="folder"/>New folder</sl:dropdown-menu-item>
    <sl:dropdown-menu-item><sl:icon name="share"/>Share</sl:dropdown-menu-item>
    <sl:dropdown-menu-separator/>
    <sl:dropdown-menu-item variant="destructive"><sl:icon name="trash-2"/>Delete</sl:dropdown-menu-item>
  </sl:dropdown-menu-content>
</sl:dropdown-menu>
```

`<sl:dropdown-menu-shortcut>` shows a keyboard shortcut at the end of an item. It only shows it: your application
handles the keys.

## Checkbox and radio items

A checkbox item shows an on/off state with a check mark (`role="menuitemcheckbox"`), a radio item one choice out of a
few (`role="menuitemradio"`, inside `<sl:dropdown-menu-radio-group>`). The state comes from the server: `checked`
marks it, and choosing an item does what its attributes say, usually a link to the same page with another parameter.
The page that comes back shows the new state.

```html
<sl:dropdown-menu-radio-group aria-label="Sort by">
  <sl:dropdown-menu-radio-item as="a" th:href="@{/orders(sort='name')}" th:checked="${sort == 'name'}">
    Name
  </sl:dropdown-menu-radio-item>
  <sl:dropdown-menu-radio-item as="a" th:href="@{/orders(sort='date')}" th:checked="${sort == 'date'}">
    Date
  </sl:dropdown-menu-radio-item>
</sl:dropdown-menu-radio-group>
```

Give such an item its text as content (`[[${option.label}]]`), not with `th:text`: that attribute passes through to
the item and would replace its check mark too.

With an `hx-post` instead of a link, return the menu again (or the part of the page that holds it), so the check mark
follows the server. Nothing changes the check mark in the browser: it always shows what the server knows.

**Example: Checkbox items.** Settings that are on or off. The server renders the state; choosing an item sends a request (here a link) and the page that comes back shows the new state.

```html
<sl:dropdown-menu id="view-menu">
  <sl:dropdown-menu-trigger variant="outline">View</sl:dropdown-menu-trigger>
  <sl:dropdown-menu-content>
    <sl:dropdown-menu-label>Appearance</sl:dropdown-menu-label>
    <sl:dropdown-menu-checkbox-item as="a" href="?status-bar=false" checked>Status Bar</sl:dropdown-menu-checkbox-item>
    <sl:dropdown-menu-checkbox-item as="a" href="?activity-bar=true" disabled>Activity Bar</sl:dropdown-menu-checkbox-item>
    <sl:dropdown-menu-checkbox-item as="a" href="?panel=true">Panel</sl:dropdown-menu-checkbox-item>
  </sl:dropdown-menu-content>
</sl:dropdown-menu>
```

**Example: Radio items.** One choice out of a few, such as where a panel goes or how a list is sorted.

```html
<sl:dropdown-menu id="position-menu">
  <sl:dropdown-menu-trigger variant="outline">Panel position</sl:dropdown-menu-trigger>
  <sl:dropdown-menu-content>
    <sl:dropdown-menu-label>Panel Position</sl:dropdown-menu-label>
    <sl:dropdown-menu-separator/>
    <sl:dropdown-menu-radio-group aria-label="Panel position">
      <sl:dropdown-menu-radio-item as="a" href="?position=top">Top</sl:dropdown-menu-radio-item>
      <sl:dropdown-menu-radio-item as="a" href="?position=bottom" checked>Bottom</sl:dropdown-menu-radio-item>
      <sl:dropdown-menu-radio-item as="a" href="?position=right">Right</sl:dropdown-menu-radio-item>
    </sl:dropdown-menu-radio-group>
  </sl:dropdown-menu-content>
</sl:dropdown-menu>
```

## Keyboard

| Key | On the trigger | In the menu |
| --- | --- | --- |
| Enter, Space | Opens the menu on its first item | Chooses the item (Space works on links too) |
| Arrow down | Opens the menu on its first item | Next item |
| Arrow up | Opens the menu on its last item | Previous item |
| Home, End | | First, last item |
| A letter | | The next item whose text starts with it; typing quickly matches a prefix |
| Escape | | Closes the menu; focus returns to the trigger (also inside another popover, such as the sidebar's phone panel, where the browser itself does not return it) |
| Tab | | Closes the menu and moves on |

Disabled items, labels and separators are skipped. The item under the pointer takes the focus, so pointer and
keyboard highlight items the same way; only keyboard focus draws the focus ring.

## Position

The menu opens below its trigger, lined up with its start. `side="top|right|bottom|left"` and
`align="start|center|end"` on `<sl:dropdown-menu-content>` change that, and the menu flips to the other side when there
is no room. `right` and `left` are the end and the start of a line, so they swap on a right-to-left page. Inside the
sidebar's phone panel and its header row a `right` or `left` menu opens below its trigger instead (see
[Sidebar](https://shadleaf.dev/0.10.0-SNAPSHOT/components/sidebar.md#menus-in-the-sidebar)).

It is placed with [CSS anchor positioning](https://developer.mozilla.org/en-US/docs/Web/CSS/CSS_anchor_positioning),
which every current browser supports (Chrome and Edge 133, Safari 26, Firefox 147). In older ones the menu's small
Alpine component places it from the trigger's position instead.

## Row actions

A menu per row of a table takes its id from an expression; the parts derive theirs from it.

**Example: Row actions.** An icon button opens the actions of a table row; align="end" lines the menu up with its end.

```html
<sl:dropdown-menu id="order-1042-actions">
  <sl:dropdown-menu-trigger variant="ghost" size="icon-sm" aria-label="Actions for order 1042">
    <sl:icon name="ellipsis"/>
  </sl:dropdown-menu-trigger>
  <sl:dropdown-menu-content align="end">
    <sl:dropdown-menu-item as="a" href="/orders/1042">View order</sl:dropdown-menu-item>
    <sl:dropdown-menu-item hx-post="/orders/1042/duplicate">Duplicate</sl:dropdown-menu-item>
    <sl:dropdown-menu-separator/>
    <sl:dropdown-menu-item variant="destructive" hx-delete="/orders/1042">Delete</sl:dropdown-menu-item>
  </sl:dropdown-menu-content>
</sl:dropdown-menu>
```

```html
<tr th:each="order : ${orders}" th:id="|order-${order.id}|">
  <td th:text="${order.number}">1042</td>
  <td>
    <sl:dropdown-menu th:id="|order-${order.id}-actions|">
      <sl:dropdown-menu-trigger variant="ghost" size="icon-sm" th:aria-label="|Actions for order ${order.number}|">
        <sl:icon name="ellipsis"/>
      </sl:dropdown-menu-trigger>
      <sl:dropdown-menu-content align="end">
        <sl:dropdown-menu-item hx:get="@{/orders/{id}/edit(id=${order.id})}" hx-target="#modal-root">
          Edit
        </sl:dropdown-menu-item>
        <sl:dropdown-menu-separator/>
        <sl:dropdown-menu-item variant="destructive" hx:get="@{/orders/{id}/delete(id=${order.id})}"
                               hx-target="#modal-root">Delete</sl:dropdown-menu-item>
      </sl:dropdown-menu-content>
    </sl:dropdown-menu>
  </td>
</tr>
```

The trigger's id is the menu's id plus `-trigger` (`order-1042-actions-trigger`). When a response replaces the row,
htmx puts the focus back on the element with the same id in the new row, so the user does not lose their place. An item
that fetches a [dialog](https://shadleaf.dev/0.10.0-SNAPSHOT/components/dialog.md#rendered-by-the-server-htmx) closes the menu first, so the dialog returns the
focus to the trigger when it closes.

## With htmx

- A swap that replaces an open menu leaves nothing behind: the new menu starts closed and works like the old one.
- Back never brings a menu back open: whether a popover is open is not part of the markup htmx saves in its history
  cache, and the restored menu starts again.
- An `hx-*` item sends its request and the menu closes; an `hx-target` can be anything, the menu's own row included.

## Left out

These shadcn/Radix parts and props have no counterpart, on purpose:

- **Sub-menus** (`DropdownMenuSub`, `-SubTrigger`, `-SubContent`). They need a lot more JavaScript (hover intent,
  a pointer grace area towards the sub-menu, arrow right and left) and are hard to use on a touch screen and with a
  screen reader. A server-rendered menu is short: flatten it with labels and groups, or open a dialog.
- **`DropdownMenuPortal`**: the top layer does what the portal is for.
- **State in the browser** (`open`/`onOpenChange`, `checked`/`onCheckedChange`, `value`/`onValueChange`, `onSelect`):
  the menu is opened by its trigger, an item is chosen by what its attributes say, and checked states come from the
  server.
- **`modal`**: the menu never makes the rest of the page inert; a click outside closes it.
- **`loop`**: arrow keys stop at the first and last item, Radix's default.

## Attributes

### sl:dropdown-menu

Attributes of `<sl:dropdown-menu>`: A menu of actions or links that opens from a button, on the browser's Popover API: it sits above the page, and Escape or a click outside closes it. Holds a `sl:dropdown-menu-trigger` and a `sl:dropdown-menu-content` with the items. Renders no element of its own. Needs Alpine for the arrow keys, typeahead and closing after an item is chosen.

| Attribute | Values | Default | Description |
| --- | --- | --- | --- |
| `id` | *string* | **required** | The menu's id, which the trigger points at with `popovertarget`. Any other button can open it that way too. |

Every prop can also be set from an expression: `th:id="${...}"`. Any other attribute passes through to the rendered element, and `class` is appended.

### sl:dropdown-menu-trigger

Attributes of `<sl:dropdown-menu-trigger>`: What opens and closes a `sl:dropdown-menu`: by default a `sl:button`, or with `as` a sidebar part (shadcn's `&lt;DropdownMenuTrigger asChild&gt;`). It renders that part with `popovertarget` set to the menu, which it names. Arrow down opens the menu on its first item, arrow up on its last. Its `icon-start` and `icon-end` slots go to the part's; any other attribute passes through to it.

| Attribute | Values | Default | Description |
| --- | --- | --- | --- |
| `as` | `button`, `sidebar-menu-button`, `sidebar-menu-action` | `button` | The part to render: a `sl:button`, a `sl:sidebar-menu-button` (a team switcher or user menu in a `sl:sidebar-menu-item`), or a `sl:sidebar-menu-action` (a "More" beside a menu button, which needs an `aria-label`). |
| `variant` | `default`, `outline`, `ghost`, `destructive`, `secondary`, `link` | `default` | Visual style, as on `sl:button`, or `default` and `outline` as on `sl:sidebar-menu-button`. Not for a sidebar menu action. |
| `size` | `xs`, `sm`, `default`, `lg`, `icon`, `icon-xs`, `icon-sm`, `icon-lg` | `default` | Size, as on `sl:button` (the `icon` sizes need an `aria-label`), or `sm`, `default` and `lg` as on `sl:sidebar-menu-button`. Not for a sidebar menu action. |
| `tooltip` | *string* | — | With `as="sidebar-menu-button"`: the label shown beside it while the sidebar is collapsed to icons, as on `sl:sidebar-menu-button`. Never shown while the menu is open. |
| `show-on-hover` | *boolean* | — | With `as="sidebar-menu-action"`: from 768&nbsp;px, show it only while the pointer is over its item, the focus is in it, or the menu is open. |

Every prop can also be set from an expression: `th:as="${...}"`. Any other attribute passes through to the rendered element, and `class` is appended.

### sl:dropdown-menu-content

Attributes of `<sl:dropdown-menu-content>`: The menu itself, inside `sl:dropdown-menu`: a popover next to its trigger, in the top layer, holding the items, labels, groups and separators. It flips to the other side when there is no room.

| Attribute | Values | Default | Description |
| --- | --- | --- | --- |
| `side` | `top`, `right`, `bottom`, `left` | `bottom` | The side of the trigger it opens on. `right` and `left` are the end and the start of a line: on a right-to-left page a `right` menu opens on the left. |
| `align` | `start`, `center`, `end` | `start` | How it lines up with the trigger along that side: `start` lines up the start edges. |

Every prop can also be set from an expression: `th:side="${...}"`. Any other attribute passes through to the rendered element, and `class` is appended.

### sl:dropdown-menu-item

Attributes of `<sl:dropdown-menu-item>`: An action in a `sl:dropdown-menu-content`: a button, or with `as="a"` a link. Every attribute that is not a prop (`hx-*`, `href`, `commandfor`, `form`) passes through, and the menu closes once the item is chosen.

| Attribute | Values | Default | Description |
| --- | --- | --- | --- |
| `as` | `button`, `a` | `button` | Element to render: a `button`, or an anchor (`a`) for navigation. |
| `variant` | `default`, `destructive` | `default` | `destructive` for an item that deletes something. |
| `type` | `button`, `submit`, `reset` | `button` | Submit behaviour of a button item. Defaults to `button`; `submit` submits the form it is in (or names with `form`). |
| `inset` | *boolean* | — | Indent the item to line up with items that start with an icon. |
| `disabled` | *boolean* | — | Disable the item. The arrow keys skip it; a disabled link loses its `href`. |

Every prop can also be set from an expression: `th:as="${...}"`. Any other attribute passes through to the rendered element, and `class` is appended.

### sl:dropdown-menu-checkbox-item

Attributes of `<sl:dropdown-menu-checkbox-item>`: An item in a `sl:dropdown-menu-content` that shows an on/off state with a check mark (`role="menuitemcheckbox"`), such as a filter or a view setting. The state comes from the server: choosing the item does what its attributes say (a link to `?archived=true`, an `hx-post`), and the page or fragment that comes back shows the new state. Give it its text as content (`[[${...}]]`), not with `th:text`, which would replace the check mark too. Otherwise as `sl:dropdown-menu-item`.

| Attribute | Values | Default | Description |
| --- | --- | --- | --- |
| `checked` | *boolean* | — | Whether the setting is on (`aria-checked`). |
| `as` | `button`, `a` | `button` | Element to render: a `button`, or an anchor (`a`). |
| `type` | `button`, `submit`, `reset` | `button` | Submit behaviour of a button item. |
| `inset` | *boolean* | — | Indent the item to line up with items that start with an icon. |
| `disabled` | *boolean* | — | Disable the item. The arrow keys skip it. |

Every prop can also be set from an expression: `th:checked="${...}"`. Any other attribute passes through to the rendered element, and `class` is appended.

### sl:dropdown-menu-radio-group

Attributes of `<sl:dropdown-menu-radio-group>`: A set of `sl:dropdown-menu-radio-item`s in a menu, one of which is chosen (`role="group"`). Name it with `aria-label` when a label above it does not make the choice clear.

`sl:dropdown-menu-radio-group` declares no props; every attribute passes through.

### sl:dropdown-menu-radio-item

Attributes of `<sl:dropdown-menu-radio-item>`: One of a set of choices in a `sl:dropdown-menu-radio-group`, with a check mark on the chosen one (`role="menuitemradio"`), such as the sort order of a list. The server marks the chosen one `checked`: each item is a link (`?sort=name`) or a request, and the page or fragment that comes back shows the new choice. Give it its text as content (`[[${...}]]`), not with `th:text`, which would replace the check mark too. Otherwise as `sl:dropdown-menu-item`.

| Attribute | Values | Default | Description |
| --- | --- | --- | --- |
| `checked` | *boolean* | — | Whether this is the chosen item (`aria-checked`). |
| `as` | `button`, `a` | `button` | Element to render: a `button`, or an anchor (`a`). |
| `type` | `button`, `submit`, `reset` | `button` | Submit behaviour of a button item. |
| `inset` | *boolean* | — | Indent the item to line up with items that start with an icon. |
| `disabled` | *boolean* | — | Disable the item. The arrow keys skip it. |

Every prop can also be set from an expression: `th:checked="${...}"`. Any other attribute passes through to the rendered element, and `class` is appended.

### sl:dropdown-menu-label

Attributes of `<sl:dropdown-menu-label>`: A heading for the items below it in a `sl:dropdown-menu-content`. It cannot be chosen, and the arrow keys pass it by.

| Attribute | Values | Default | Description |
| --- | --- | --- | --- |
| `inset` | *boolean* | — | Indent the label to line up with items that start with an icon. |

Every prop can also be set from an expression: `th:inset="${...}"`. Any other attribute passes through to the rendered element, and `class` is appended.

### sl:dropdown-menu-group

Attributes of `<sl:dropdown-menu-group>`: Items that belong together in a `sl:dropdown-menu-content` (`role="group"`), usually between separators or under a label.

`sl:dropdown-menu-group` declares no props; every attribute passes through.

### sl:dropdown-menu-separator

Attributes of `<sl:dropdown-menu-separator>`: A line between groups of items in a `sl:dropdown-menu-content` (`role="separator"`).

`sl:dropdown-menu-separator` declares no props; every attribute passes through.

### sl:dropdown-menu-shortcut

Attributes of `<sl:dropdown-menu-shortcut>`: A keyboard shortcut at the end of a `sl:dropdown-menu-item`, such as `⌘S`. It only shows the shortcut; the application handles the keys. Typeahead ignores it.

`sl:dropdown-menu-shortcut` declares no props; every attribute passes through.

## JavaScript

A small Alpine component, `slDropdownMenu`, adds the menu's keyboard and behaviour: `<sl:dropdown-menu-content>`
renders as `<div class="dropdown-menu-content" role="menu" popover x-data="slDropdownMenu">`.

- **The basics work without Alpine.** The trigger opens and closes the menu, Escape and a click outside close it, and
  items work with the mouse and with Tab. Alpine is needed for the arrow keys and typeahead, for closing the menu after
  an item is chosen, for `aria-expanded` on the trigger, and for placing the menu in browsers without CSS anchor
  positioning. Alpine is included by default; see [Alpine](https://shadleaf.dev/0.10.0-SNAPSHOT/guides/alpine.md).
- **It needs no Alpine plugin.** If you load Alpine yourself (`shadleaf.assets.alpine=external`), plain Alpine is
  enough.
- **Keep your own `x-data` off `<sl:dropdown-menu-content>`.** An element has one `x-data`, and attributes on it end up
  on the menu, so yours would replace `slDropdownMenu`. Put it on an element inside the menu.
