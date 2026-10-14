# Skeleton

> A placeholder that pulses while content loads.

Source: https://wimdeblauwe.github.io/shadleaf/current/components/skeleton/

**Example: Skeleton.**

```html
<sl:skeleton/>
<sl:skeleton/>
```

## Shapes and sizes

A skeleton is a line of text by default: the full width of its container and one line high. `shape="circle"` stands in
for an avatar. Both are starting points: give a skeleton the size of what it replaces with a class of your own, for
example `class="h-32 w-64"` when your application compiles Tailwind. Your class wins over the skin's size.

**Example: In a card.** The card that is loading says so with aria-busy; the skeletons are hidden from assistive technology.

```html
<sl:card aria-busy="true">
  <sl:card-header>
    <sl:skeleton shape="circle"/>
  </sl:card-header>
  <sl:card-content>
    <sl:skeleton/>
    <sl:skeleton/>
  </sl:card-content>
</sl:card>
```

## While it loads

A skeleton is only a picture of content to come, so it is hidden from assistive technology (`aria-hidden="true"`).
Put `aria-busy="true"` on the region that is loading, and remove it when the content arrives, for example when htmx
swaps it in. The pulse stops for users who ask for reduced motion.

## Attributes

Attributes of `<sl:skeleton>`: A placeholder that pulses while content loads. It is hidden from assistive technology; mark the region that is loading with `aria-busy="true"` instead. Give it the size of what it stands in for with a class of your own.

| Attribute | Values | Default | Description |
| --- | --- | --- | --- |
| `shape` | `line`, `circle` | `line` | A line of text (full width, one line high) or a circle (an avatar). |

Every prop can also be set from an expression: `th:shape="${...}"`. Any other attribute passes through to the rendered element, and `class` is appended.
