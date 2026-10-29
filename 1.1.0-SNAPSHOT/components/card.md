# Card

> A container for related content and actions, built from its parts.

Source: https://shadleaf.dev/1.1.0-SNAPSHOT/components/card/

**Example: Card.**

```html
<sl:card>
  <sl:card-header>
    <sl:card-title level="3">Order #1042</sl:card-title>
    <sl:card-description>Placed on 12 September</sl:card-description>
    <sl:card-action><sl:badge variant="secondary">Shipped</sl:badge></sl:card-action>
  </sl:card-header>
  <sl:card-content>
    <p>Three items, delivered to the Antwerp warehouse.</p>
  </sl:card-content>
  <sl:card-footer>
    <sl:button as="a" href="/orders/1042" variant="outline">View order</sl:button>
  </sl:card-footer>
</sl:card>
```

## Built from parts

A card is `<sl:card>` plus the parts you need: `<sl:card-header>` (holding `<sl:card-title>`,
`<sl:card-description>` and `<sl:card-action>`), `<sl:card-content>` and `<sl:card-footer>`. Use any of them, in any
order. Each part is its own component, so it has its own attributes and IDE completion, and passes every other
attribute through.

A card takes the width of its container; set it from your layout.

**Example: Content only.**

```html
<sl:card>
  <sl:card-content><p>A card needs none of its parts but one.</p></sl:card-content>
</sl:card>
```

## A title that is a heading

`<sl:card-title>` is plain text unless you give it a `level`. With one, it is announced as a heading of that level, so
screen reader users can jump between cards. Pick the level that fits the page: a card under an `<h2>` gets
`level="3"`.

## Small

`size="sm"` tightens the spacing inside the card and between its parts.

**Example: Small.**

```html
<sl:card size="sm">
  <sl:card-header>
    <sl:card-title level="3">Late orders</sl:card-title>
    <sl:card-description>Past their delivery date</sl:card-description>
  </sl:card-header>
  <sl:card-content><p>3</p></sl:card-content>
</sl:card>
```

## Dividing lines

`border` on the header or the footer draws a line between it and the content, with matching spacing. It works without
utility classes, so also with the standalone stylesheet.

**Example: Dividing lines.**

```html
<sl:card>
  <sl:card-header border>
    <sl:card-title level="3">Notifications</sl:card-title>
    <sl:card-description>What we email you about</sl:card-description>
  </sl:card-header>
  <sl:card-content><p>Order updates and weekly summaries.</p></sl:card-content>
  <sl:card-footer border>
    <sl:button>Save</sl:button>
    <sl:button variant="ghost">Cancel</sl:button>
  </sl:card-footer>
</sl:card>
```

## Attributes

### sl:card

Attributes of `<sl:card>`: A container for related content and actions. Build it from its parts, in any order and number: `sl:card-header` (holding `sl:card-title`, `sl:card-description` and `sl:card-action`), `sl:card-content` and `sl:card-footer`. An `img` as the first or last child runs to the card's edges.

| Attribute | Values | Default | Description |
| --- | --- | --- | --- |
| `size` | `default`, `sm` | `default` | Spacing inside the card and between its parts. |

Every prop can also be set from an expression: `th:size="${...}"`. Any other attribute passes through to the rendered element, and `class` is appended.

### sl:card-header

Attributes of `<sl:card-header>`: The top of a card: a `sl:card-title`, an optional `sl:card-description` below it, and an optional `sl:card-action` beside both.

| Attribute | Values | Default | Description |
| --- | --- | --- | --- |
| `border` | *boolean* | — | A dividing line below the header, with matching spacing. |

Every prop can also be set from an expression: `th:border="${...}"`. Any other attribute passes through to the rendered element, and `class` is appended.

### sl:card-title

Attributes of `<sl:card-title>`: The title of a card, inside `sl:card-header`.

| Attribute | Values | Default | Description |
| --- | --- | --- | --- |
| `level` | `1`, `2`, `3`, `4`, `5`, `6` | — | Heading level. With it the title is a heading (`role="heading"` and `aria-level`) that screen reader users can jump to; without it, plain text. Pick the level that fits the page's outline. |

Every prop can also be set from an expression: `th:level="${...}"`. Any other attribute passes through to the rendered element, and `class` is appended.

### sl:card-description

Attributes of `<sl:card-description>`: Supporting text below a `sl:card-title`, inside `sl:card-header`.

`sl:card-description` declares no props; every attribute passes through.

### sl:card-action

Attributes of `<sl:card-action>`: An action in the top end corner of a card, such as a button or a badge, inside `sl:card-header`.

`sl:card-action` declares no props; every attribute passes through.

### sl:card-content

Attributes of `<sl:card-content>`: The body of a card.

`sl:card-content` declares no props; every attribute passes through.

### sl:card-footer

Attributes of `<sl:card-footer>`: The bottom of a card, for actions or a summary.

| Attribute | Values | Default | Description |
| --- | --- | --- | --- |
| `border` | *boolean* | — | A dividing line above the footer, with matching spacing. |

Every prop can also be set from an expression: `th:border="${...}"`. Any other attribute passes through to the rendered element, and `class` is appended.
