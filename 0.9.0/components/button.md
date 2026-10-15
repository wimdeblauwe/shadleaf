# Button

> A button, or a link that looks like one.

Source: https://wimdeblauwe.github.io/shadleaf/0.9.0/components/button/

**Example: Button.**

```html
<sl:button>Save</sl:button>
```

## A button never submits by accident

`<sl:button>` renders `type="button"`, not HTML's default `submit`. A button inside a form therefore does nothing
unless you say so: write `type="submit"` on the one that should submit.

**Example: In a form.** Only the button that says type="submit" submits the form.

```html
<form method="post" th:action="@{/orders}">
  <sl:button type="submit">Save</sl:button>
  <sl:button variant="outline">Preview</sl:button>
</form>
```

## Variants

**Example: Variants.**

```html
<sl:button>Default</sl:button>
<sl:button variant="outline">Outline</sl:button>
<sl:button variant="ghost">Ghost</sl:button>
<sl:button variant="destructive">Destructive</sl:button>
<sl:button variant="secondary">Secondary</sl:button>
<sl:button variant="link">Link</sl:button>
```

## Sizes

**Example: Sizes.**

```html
<sl:button size="xs">Extra small</sl:button>
<sl:button size="sm">Small</sl:button>
<sl:button>Default</sl:button>
<sl:button size="lg">Large</sl:button>
```

The `icon` sizes are square, for a button holding only an icon. Such a button has no text for a screen reader, so it
must have an `aria-label` (or `aria-labelledby`); rendering fails without one.

**Example: Icon-only sizes.** An icon-only button needs an aria-label; rendering fails without one.

```html
<sl:button size="icon-xs" variant="outline" aria-label="Add"><sl:icon name="plus"/></sl:button>
<sl:button size="icon-sm" variant="outline" aria-label="Add"><sl:icon name="plus"/></sl:button>
<sl:button size="icon" variant="outline" aria-label="Add"><sl:icon name="plus"/></sl:button>
<sl:button size="icon-lg" variant="outline" aria-label="Add"><sl:icon name="plus"/></sl:button>
```

## With an icon

Put an [`<sl:icon>`](https://wimdeblauwe.github.io/shadleaf/0.9.0/components/icon.md) in the `icon-start` or `icon-end` slot. It takes the button's colour and a 16 pixel size.

**Example: With an icon.**

```html
<sl:button variant="destructive">
  <sl:slot name="icon-start"><sl:icon name="trash"/></sl:slot>
  Delete order
</sl:button>
<sl:button variant="outline">
  Next
  <sl:slot name="icon-end"><sl:icon name="arrow-right"/></sl:slot>
</sl:button>
```

## As a link

`as="a"` renders an anchor that looks the same, for navigation.

**Example: As a link.**

```html
<sl:button as="a" href="/orders" variant="outline">View orders</sl:button>
<sl:button as="a" href="/orders" variant="link">View orders</sl:button>
```

## Disabled and loading

A disabled button gets the native `disabled` attribute. A disabled link loses its `href`, leaves the tab order and is
marked `aria-disabled`, so it cannot be followed. `loading` shows a spinner in place of the start icon, announces the
busy state with `aria-busy` and a visually hidden "Loading" label, and disables the button.

**Example: Disabled.**

```html
<sl:button disabled>Save</sl:button>
<sl:button as="a" href="/orders" variant="outline" disabled>View orders</sl:button>
```
**Example: Loading.**

```html
<sl:button loading>Saving</sl:button>
<sl:button variant="outline" loading>
  <sl:slot name="icon-start"><sl:icon name="save"/></sl:slot>
  Save
</sl:button>
<sl:button size="icon" variant="outline" aria-label="Refresh" loading><sl:icon name="refresh-cw"/></sl:button>
```

The "Loading" label comes from the message `sl.button.loading`; define it in your own `messages.properties` to
translate or reword it.

## Props from expressions

Every prop also has a `th:` form that takes a Thymeleaf expression.

**Example: Props from an expression.**

```html
<sl:button th:variant="${order.cancellable ? 'destructive' : 'secondary'}"
           th:text="#{orders.cancel}">Cancel order</sl:button>
```

## Everything else passes through

Attributes that are not props, such as `hx-*`, `x-*`, `aria-*` and `data-*`, land on the rendered element unchanged,
so the button works with htmx and Alpine. `class` is appended to the component's own class.

**Example: Pass-through attributes.** Anything that is not a prop lands on the rendered element; class is appended.

```html
<sl:button variant="outline" class="app-wide"
           hx-post="/orders/42/archive" hx-target="#orders"
           data-testid="archive">Archive</sl:button>
```

## Attributes

Attributes of `<sl:button>`: A button, or with `as="a"` an anchor that looks the same. Every attribute that is not a prop (`hx-*`, `x-*`, `aria-*`, `data-*`, `href`) passes through; `class` is appended to `button`.

| Attribute | Values | Default | Description |
| --- | --- | --- | --- |
| `variant` | `default`, `outline`, `ghost`, `destructive`, `secondary`, `link` | `default` | Visual style of the button. |
| `size` | `xs`, `sm`, `default`, `lg`, `icon`, `icon-xs`, `icon-sm`, `icon-lg` | `default` | Size of the button. The `icon` sizes are square, for a button that holds only an icon, and need an `aria-label`. |
| `as` | `button`, `a` | `button` | Element to render: a `button`, or an anchor (`a`) for navigation. |
| `type` | `button`, `submit`, `reset` | `button` | Submit behaviour. Defaults to `button`, unlike HTML, so a button in a form never submits by accident. |
| `disabled` | *boolean* | — | Disable the button. A disabled anchor loses its `href` and leaves the tab order. |
| `loading` | *boolean* | — | Show a spinner, announce the busy state and disable the button. |

Every prop can also be set from an expression: `th:variant="${...}"`. Any other attribute passes through to the rendered element, and `class` is appended. An `aria-label` or `aria-labelledby` is required when `size` is `icon` or `icon-xs` or `icon-sm` or `icon-lg`; rendering fails without one.
