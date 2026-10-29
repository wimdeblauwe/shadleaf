# Toggle

> A button that is on or off, as a checkbox drawn as a button, bound to a boolean form field with th:field.

Source: https://shadleaf.dev/1.0.2/components/toggle/

**Example: Toggle.**

```html
<sl:toggle name="bookmarked" size="sm" variant="outline">
  <sl:icon name="bookmark"/> Bookmark
</sl:toggle>
```

A toggle is a checkbox drawn as a button: a `<label>` with the checkbox inside it, covering it. A click anywhere on the
toggle checks or unchecks it, the form submits it as a checkbox, and it works without JavaScript. A screen reader
announces it as a checkbox, checked or not.

The content is the toggle's label. A toggle that holds only an icon needs an `aria-label`. Every attribute that is not
a prop goes to the checkbox (`name`, `value`, `checked`, `disabled`, `aria-label`, `x-on:change`), except `class`,
which goes to the label.

For several toggles that belong together, one of which (or any of which) can be on, use a
[toggle group](https://shadleaf.dev/1.0.2/components/toggle-group.md).

## Outline

**Example: Outline.**

```html
<sl:toggle variant="outline" name="italicText" aria-label="Italic"><sl:icon name="italic"/></sl:toggle>
<sl:toggle variant="outline" name="underlineText" aria-label="Underline"><sl:icon name="underline"/></sl:toggle>
```

## With text

**Example: With text.**

```html
<sl:toggle name="italicLabel"><sl:icon name="italic"/> Italic</sl:toggle>
```

## Sizes

**Example: Sizes.**

```html
<sl:toggle size="sm" variant="outline" name="sizeSm">Small</sl:toggle>
<sl:toggle variant="outline" name="sizeDefault">Default</sl:toggle>
<sl:toggle size="lg" variant="outline" name="sizeLg">Large</sl:toggle>
```

## In a form

`th:field` on a boolean field checks the toggle when the field is `true`, as on a [checkbox](https://shadleaf.dev/1.0.2/components/checkbox.md): Spring
numbers the `id` (`starred1`) and adds a hidden marker input, so unchecking it submits `false`. In a
[field](https://shadleaf.dev/1.0.2/components/field.md), the toggle takes the field's name as its `id` and is described by the field's description and
error.

**Example: In a form.** th:field on a boolean checks the toggle from the form object, as on a checkbox.

```html
<sl:toggle th:field="*{starred}" variant="outline"><sl:icon name="star"/> Starred</sl:toggle>
```

## Invalid

**Example: Invalid.** With th:field, a field that has errors marks the toggle aria-invalid by itself.

```html
<sl:toggle th:field="*{pinned}" variant="outline"><sl:icon name="bookmark"/> Pin to the top</sl:toggle>
```

## Disabled

**Example: Disabled.**

```html
<sl:toggle name="boldLocked" disabled aria-label="Bold"><sl:icon name="bold"/></sl:toggle>
<sl:toggle name="underlineLocked" variant="outline" checked disabled aria-label="Underline"><sl:icon
    name="underline"/></sl:toggle>
```

## Attributes

Attributes of `<sl:toggle>`: A button that is on or off: a checkbox drawn as a button, so it submits, binds and works as one without JavaScript, and a screen reader announces it as checked or not. The content (text, icons) is its label; give a toggle that holds only an icon an `aria-label`. Every attribute that is not a prop (`th:field`, `name`, `value`, `checked`, `disabled`, `aria-label`, `x-*`) passes through to the `input`, except `class`, which goes to the `label` around it, and `th:text` and `th:utext`, which set the label. `th:field` on a boolean works as on a checkbox; the toggle is marked `aria-invalid` when its field has errors.

| Attribute | Values | Default | Description |
| --- | --- | --- | --- |
| `variant` | `default`, `outline` | `default` | Visual style of the toggle. |
| `size` | `sm`, `default`, `lg` | `default` | Size of the toggle. |

Every prop can also be set from an expression: `th:variant="${...}"`. Any other attribute passes through to the rendered element, and `class` is appended.
