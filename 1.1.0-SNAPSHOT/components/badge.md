# Badge

> A small label for a status, a count or a category.

Source: https://shadleaf.dev/1.1.0-SNAPSHOT/components/badge/

**Example: Badge.**

```html
<sl:badge>New</sl:badge>
```

## Variants

The same variants as the [button](https://shadleaf.dev/1.1.0-SNAPSHOT/components/button.md).

**Example: Variants.**

```html
<sl:badge>Default</sl:badge>
<sl:badge variant="secondary">Secondary</sl:badge>
<sl:badge variant="destructive">Destructive</sl:badge>
<sl:badge variant="outline">Outline</sl:badge>
<sl:badge variant="ghost">Ghost</sl:badge>
<sl:badge variant="link">Link</sl:badge>
```

## With an icon

Put an [`<sl:icon>`](https://shadleaf.dev/1.1.0-SNAPSHOT/components/icon.md) in the `icon-start` or `icon-end` slot. It takes the badge's colour and a 12 pixel size.

**Example: With an icon.**

```html
<sl:badge variant="secondary">
  <sl:slot name="icon-start"><sl:icon name="badge-check"/></sl:slot>
  Verified
</sl:badge>
<sl:badge variant="outline">
  Details
  <sl:slot name="icon-end"><sl:icon name="arrow-right"/></sl:slot>
</sl:badge>
```

## As a link

`as="a"` renders an anchor, for a badge that filters a list or opens a detail page. Only a linked badge reacts to hover
and shows a focus ring; a plain badge is not interactive, so it does not pretend to be.

**Example: As a link.** Only a linked badge changes on hover and takes keyboard focus.

```html
<sl:badge as="a" href="/orders?status=open">Open</sl:badge>
<sl:badge as="a" href="/orders?status=late" variant="destructive">Late</sl:badge>
<sl:badge as="a" href="/orders" variant="outline">
  All orders
  <sl:slot name="icon-end"><sl:icon name="arrow-up-right"/></sl:slot>
</sl:badge>
```

A badge is text, so a screen reader reads it where it stands. When a bare number or a single word would not make sense
out of context, write the context into the text, or next to the badge.

## Props from expressions

**Example: Props from an expression.**

```html
<sl:badge th:variant="${order.late ? 'destructive' : 'secondary'}"
          th:text="${order.status}">Open</sl:badge>
```

## Everything else passes through

**Example: Pass-through attributes.** Anything that is not a prop lands on the rendered element; class is appended.

```html
<sl:badge variant="outline" class="app-wide" id="order-status" data-testid="status">Shipped</sl:badge>
```

## Attributes

Attributes of `<sl:badge>`: A small label for a status, a count or a category; with `as="a"`, a link that looks the same. Every attribute that is not a prop passes through; `class` is appended to `badge`.

| Attribute | Values | Default | Description |
| --- | --- | --- | --- |
| `variant` | `default`, `secondary`, `destructive`, `outline`, `ghost`, `link` | `default` | Visual style of the badge. |
| `as` | `span`, `a` | `span` | Element to render: a `span`, or an anchor (`a`) for a badge that links somewhere. Only a linked badge reacts to hover. |

Every prop can also be set from an expression: `th:variant="${...}"`. Any other attribute passes through to the rendered element, and `class` is appended.
