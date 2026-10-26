# Theme Toggle

> Switches the page between the light and the dark theme, and remembers the choice in the browser.

Source: https://shadleaf.dev/1.1.0-SNAPSHOT/components/theme-toggle/

**Example: Theme toggle.** An icon button opening a menu with Light, Dark and System; the icon shows the page's theme.

```html
<sl:theme-toggle/>
```

## Usage

Put the theme script in the `<head>` of your layout, as [Getting started](https://shadleaf.dev/1.1.0-SNAPSHOT/getting-started.md) shows, and the
toggle wherever it belongs, usually the page header:

```html
<head>
  <th:block th:replace="~{sl/layout :: theme-script}"></th:block>
  <th:block th:replace="~{sl/layout :: assets}"></th:block>
</head>
<body>
<header>
  ...
  <sl:theme-toggle/>
</header>
```

The toggle is an icon button that opens a menu with **Light**, **Dark** and **System**. Light and Dark are stored in
the browser (`localStorage`, key `shadleaf-theme`); System removes the key, and the page follows the operating system
again. The theme script reads that key before the first paint, so every page after the choice starts in the right
theme, with no flash of the other one. A dark page prints in the light theme: the script takes the `dark` class off
while the browser prints and puts it back afterwards.

The button shows a sun in the light theme and a moon in the dark one. A screen reader hears something like "Theme, menu
button"; in the menu, "Light, checked, menu item radio, 1 of 3" (the wording differs between screen readers).

## One button

`as="button"` makes it one button that switches between light and dark. It is a toggle button for dark mode: a screen
reader hears "Dark mode, toggle button, pressed" while the page is dark.

**Example: One button.** as="button" switches between light and dark with one press (aria-pressed is on while dark).

```html
<sl:theme-toggle as="button" variant="ghost"/>
```

Pressing it stores light or dark, so once pressed the page no longer follows the operating system. Use the menu where
that matters.

## What the server renders

The server cannot know what the browser stored, so neither the menu's check mark nor the button's pressed state is in
the markup: the toggle's Alpine component sets them when it starts. Nothing shows wrong in the meantime. The icon comes
from the `dark` class the theme script put on `<html>` before the first paint, and the menu is closed until someone
opens it. A toggle that htmx swaps in, or brings back from its history cache, works the same way.

## Several toggles, several tabs

Every toggle on the page shows the same state, and a choice made in another tab of the same site changes this page
too. While the choice is System, the page follows the operating system when it switches between light and dark (at
sunset, say). Each toggle on a page needs its own `id` when it is a menu:

```html
<sl:theme-toggle id="header-theme" variant="ghost"/>
...
<sl:theme-toggle id="footer-theme" side="top"/>
```

## Labels

The labels are messages, which your `messages.properties` can reword or translate (see
[Translating](https://shadleaf.dev/1.1.0-SNAPSHOT/guides/translating.md)):

```properties
sl.theme-toggle.label=Theme
sl.theme-toggle.dark-mode=Dark mode
sl.theme-toggle.light=Light
sl.theme-toggle.dark=Dark
sl.theme-toggle.system=System
```

`sl.theme-toggle.label` names the menu's button and `sl.theme-toggle.dark-mode` the single button; an `aria-label` on
`<sl:theme-toggle>` replaces either for one toggle.

There is no tooltip. The sun and the moon are what people look for, and a [tooltip](https://shadleaf.dev/1.1.0-SNAPSHOT/components/tooltip.md) would only repeat the
button's name to a screen reader, as its description.

## Without JavaScript

Nothing can store a choice without JavaScript, and the theme script cannot run either, so the page stays light. The
toggle is then not shown at all (`@media (scripting: none)`), rather than shown and doing nothing.

## Left out

- **More themes than light and dark.** The theme script and the skins know the `dark` class only.
- **A choice stored on the server** (a user setting). Render `class="dark"` on `<html>` yourself and leave out the
  theme script and the toggle; the toggle stores in the browser only.
- **The keyboard shortcut** shadcn's own site has (D). Your application can add one that clicks the toggle.

## Attributes

Attributes of `<sl:theme-toggle>`: Switches the page between the light and the dark theme, and remembers the choice in the browser under the key the theme script (`sl/layout :: theme-script`) reads before the first paint, so the next page starts in it. By default an icon button opening a menu with Light, Dark and System; with `as="button"` one button that switches between light and dark. Its icon shows the theme the page is in. Needs Alpine; without JavaScript it is not shown. Every attribute that is not a prop passes through to the button.

| Attribute | Values | Default | Description |
| --- | --- | --- | --- |
| `as` | `menu`, `button` | `menu` | `menu`: a menu with Light, Dark and System (`menuitemradio`s). `button`: one toggle button for dark mode (`aria-pressed`), which cannot go back to following the system. |
| `id` | *string* | `theme-toggle` | The menu's id (its button gets the id with `-trigger` added). Two menus on a page need different ids. With `as="button"` it is the button's id, rendered only when set. |
| `variant` | `default`, `outline`, `ghost`, `destructive`, `secondary`, `link` | `outline` | Visual style of the button, as on `sl:button`. |
| `size` | `icon`, `icon-xs`, `icon-sm`, `icon-lg` | `icon` | Size of the button: one of `sl:button`'s icon sizes. |
| `side` | `top`, `right`, `bottom`, `left` | `bottom` | The side of the button the menu opens on, as on `sl:dropdown-menu-content`. |
| `align` | `start`, `center`, `end` | `end` | How the menu lines up with the button along that side, as on `sl:dropdown-menu-content`: `end` lines up the end edges. |

Every prop can also be set from an expression: `th:as="${...}"`. Any other attribute passes through to the rendered element, and `class` is appended.

## JavaScript

A small Alpine component, `slThemeToggle`, does the work: `<sl:theme-toggle>` renders as
`<div class="theme-toggle" x-data="slThemeToggle">`, around a [dropdown menu](https://shadleaf.dev/1.1.0-SNAPSHOT/components/dropdown-menu.md) (whose own
`slDropdownMenu` handles the keyboard) or a button.

- **It needs Alpine**, and no Alpine plugin: it writes the key itself, not with `$persist`, which would store
  `"dark"` as JSON, quotes included, where the theme script expects `dark`. Alpine is included by default; see
  [Alpine](https://shadleaf.dev/1.1.0-SNAPSHOT/guides/alpine.md).
- **Attributes go to the button**, `class` and your own `x-data` included; the wrapper around it has no box.
- **It changes nothing when it starts.** The page is already in the stored theme; a page that sets the `dark` class
  some other way keeps it until someone chooses.

| Event | On | Effect |
| --- | --- | --- |
| `sl-theme-change` | the document | Fired after the theme changed: by a toggle, in another tab, or by the operating system while the choice is System. The detail is `{theme: 'light' \| 'dark' \| 'system', dark: true \| false}`. For a chart that has to redraw, say. |
