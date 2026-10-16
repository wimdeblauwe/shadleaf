# Collapsible

> Content that a button shows and hides, on the browser's details element.

Source: https://shadleaf.dev/0.8.0-SNAPSHOT/components/collapsible/

**Example: Collapsible.**

```html
<sl:collapsible>
  <sl:collapsible-trigger>@peduarte starred 3 repositories <sl:icon name="chevrons-up-down"/></sl:collapsible-trigger>
  <sl:collapsible-content>
    <p>@radix-ui/primitives</p>
    <p>@radix-ui/colors</p>
    <p>@stitches/react</p>
  </sl:collapsible-content>
</sl:collapsible>
```

## Built from parts

A collapsible is `<sl:collapsible>` with an `<sl:collapsible-trigger>` and an `<sl:collapsible-content>`:

```html
<sl:collapsible>
  <sl:collapsible-trigger>@peduarte starred 3 repositories <sl:icon name="chevrons-up-down"/></sl:collapsible-trigger>
  <sl:collapsible-content>
    <p>@radix-ui/primitives</p>
    <p>@radix-ui/colors</p>
  </sl:collapsible-content>
</sl:collapsible>
```

It is a [`<details>`](https://developer.mozilla.org/en-US/docs/Web/HTML/Reference/Elements/details) element with the
trigger as its `<summary>`, so it needs no JavaScript: the browser opens and closes it, from the keyboard too, and
tells a screen reader whether it is expanded. Two consequences for porting a shadcn/ui example:

- **The trigger comes first**, and the content after it, both inside `<sl:collapsible>`. shadcn/ui's examples can put
  the trigger anywhere in the collapsible, even after the content; a `summary` must be the first child.
- **Everything else goes in the trigger or the content.** Text next to the trigger (shadcn's "@peduarte starred 3
  repositories" heading) can be part of the trigger.

`open` renders it open. As with the [accordion](https://shadleaf.dev/0.8.0-SNAPSHOT/components/accordion.md), find-in-page and a link to an element inside open a
closed collapsible on a match (Chrome and Edge 97, Firefox 148, Safari 26.2). It does not animate, as shadcn/ui's does
not.

## Drawn as a button

The trigger is plain text by default. `variant` or `size` draws it as a [button](https://shadleaf.dev/0.8.0-SNAPSHOT/components/button.md), with the button's
variants and sizes, where shadcn/ui writes `<CollapsibleTrigger asChild><Button …>`. An icon-only trigger
(`size="icon"`) needs an `aria-label`.

**Example: Drawn as a button.** variant and size draw the trigger as a button, as shadcn/ui's asChild on a Button does.

```html
<sl:collapsible open>
  <sl:collapsible-trigger variant="outline" size="sm"><sl:icon name="sliders-horizontal"/> Filters</sl:collapsible-trigger>
  <sl:collapsible-content>
    <sl:field orientation="horizontal" for="collapsible-in-stock">
      <sl:checkbox checked/>
      <sl:field-label>In stock only</sl:field-label>
    </sl:field>
  </sl:collapsible-content>
</sl:collapsible>
```

## Disabled

`disabled` keeps it closed (or open, with `open`): the trigger fades, leaves the tab order and ignores the pointer, and
a closed disabled collapsible renders no content, so find-in-page cannot open it.

**Example: Disabled.**

```html
<sl:collapsible disabled>
  <sl:collapsible-trigger variant="ghost" size="sm">Order history</sl:collapsible-trigger>
  <sl:collapsible-content>Your past orders.</sl:collapsible-content>
</sl:collapsible>
```

## Left out

- **`asChild` on the trigger**: `variant` and `size` draw it as a button; the element stays a `summary`.
- **`open`/`onOpenChange` state**: `open` renders it open; the browser's `toggle` event reports changes.
- **A trigger outside the collapsible**, or after the content: a `details` element has its `summary` first.
- **Styling the trigger's icon by state** (shadcn's `group-data-[state=open]:rotate-90`): the bundle has no utility
  classes. In your own CSS, `.collapsible[open] > .collapsible-trigger svg { rotate: 90deg; }` does it.

## Attributes

### sl:collapsible

Attributes of `<sl:collapsible>`: Content that a button shows and hides. Built on the browser's `details` element, so it needs no JavaScript. Holds a `sl:collapsible-trigger` (first) and a `sl:collapsible-content`.

| Attribute | Values | Default | Description |
| --- | --- | --- | --- |
| `open` | *boolean* | — | Render it open. |
| `disabled` | *boolean* | — | It cannot be opened or closed: its trigger is out of the tab order and ignores the pointer, and closed it renders no content, so find-in-page cannot open it. |

Every prop can also be set from an expression: `th:open="${...}"`. Any other attribute passes through to the rendered element, and `class` is appended.

### sl:collapsible-trigger

Attributes of `<sl:collapsible-trigger>`: What the user clicks to open and close a `sl:collapsible`: its `details` element's `summary`, so it must come first. Plain text by default; `variant` or `size` draws it as a `sl:button`.

| Attribute | Values | Default | Description |
| --- | --- | --- | --- |
| `variant` | `default`, `outline`, `secondary`, `ghost`, `destructive`, `link` | — | Draw the trigger as a button of this variant, as on `sl:button`. |
| `size` | `xs`, `sm`, `default`, `lg`, `icon`, `icon-xs`, `icon-sm`, `icon-lg` | — | Draw the trigger as a button of this size, as on `sl:button`. The `icon` sizes need an `aria-label`. |

Every prop can also be set from an expression: `th:variant="${...}"`. Any other attribute passes through to the rendered element, and `class` is appended. An `aria-label` or `aria-labelledby` is required when `size` is `icon` or `icon-xs` or `icon-sm` or `icon-lg`; rendering fails without one.

### sl:collapsible-content

Attributes of `<sl:collapsible-content>`: The content of a `sl:collapsible`, shown while it is open. Find-in-page searches it while it is closed, and opens it on a match.

`sl:collapsible-content` declares no props; every attribute passes through.
