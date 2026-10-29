# Skip Link

> A link past the navigation to the page's content, for keyboard users.

Source: https://shadleaf.dev/1.0.2/components/skip-link/

**Example: Skip link.** Hidden until it has the focus: click into the frame and press Tab. It skips to the element with id="main" (here the paragraph below), which tabindex="-1" lets take the focus.

```html
<sl:skip-link/>
<p id="main" tabindex="-1">The page's content.</p>
```

## Usage

A sidebar or a header puts many links before the content. A skip link lets a keyboard user jump past them
([WCAG 2.4.1](https://www.w3.org/WAI/WCAG22/Understanding/bypass-blocks)). Put it first in the body:

```html
<body>
  <sl:skip-link/>
  <sl:sidebar-provider>
    <sl:sidebar>...</sl:sidebar>
    <sl:sidebar-inset>...</sl:sidebar-inset>
  </sl:sidebar-provider>
</body>
```

It is hidden until it has the focus, which the first Tab on the page gives it, and then shows at the top of the page.
Following it moves the focus to the page's `main`, and the next Tab goes on from there.

## The target

The link goes to the element with `id="main"`; `for` points it at another id. [`<sl:sidebar-inset>`](https://shadleaf.dev/1.0.2/components/sidebar.md) is
`<main id="main" tabindex="-1">`, so with a sidebar there is nothing to set up.

The target needs `tabindex="-1"` when it is no control of its own, as a `main` is. Without it, Chromium and Firefox
only move the place the next Tab starts from: the focus itself goes to the page, and a screen reader stays where it
was. `tabindex="-1"` lets the target take the focus without making it a tab stop. A target of your own:

```html
<sl:skip-link for="content"/>
...
<main id="content" tabindex="-1">...</main>
```

The focused `main` draws no focus ring: it is no control, and the page scrolling to it shows where the focus went.

## Text

The text is "Skip to main content" (the message `sl.skip-link`, see [Translating](https://shadleaf.dev/1.0.2/guides/translating.md)). Content of its own replaces it:

```html
<sl:skip-link for="results">Skip to the results</sl:skip-link>
```

## htmx

With `hx-boost` on the body the skip link still works: htmx leaves links to a place on the same page alone, so
following it makes no request.

## Messages

| Key | English |
| --- | --- |
| `sl.skip-link` | Skip to main content |

## Attributes

Attributes of `<sl:skip-link>`: A link past the navigation to the page's content, for keyboard users: put it first in the body. Hidden until it has the focus (the first Tab on the page), then drawn at the top of the page. Its text is "Skip to main content" (the message `sl.skip-link`), which content of its own replaces. The target should be able to take the focus: `sl:sidebar-inset` is `id="main"` with `tabindex="-1"`, so the default target works with it.

| Attribute | Values | Default | Description |
| --- | --- | --- | --- |
| `for` | *string* | `main` | The `id` of the element it skips to, usually the `main`. |

Every prop can also be set from an expression: `th:for="${...}"`. Any other attribute passes through to the rendered element, and `class` is appended.
