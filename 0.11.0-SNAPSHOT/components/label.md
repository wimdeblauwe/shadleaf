# Label

> A label for a form control.

Source: https://shadleaf.dev/0.11.0-SNAPSHOT/components/label/

**Example: Label.**

```html
<sl:label for="label-email">Email</sl:label>
<sl:input type="email" id="label-email" placeholder="you@example.com"/>
```

## Pointing at a control

Give the label a `for` with the control's `id`. A control with `th:field="*{email}"` gets `id="email"` from Spring, so
`for="email"` matches it without writing the `id` yourself. Clicking the label focuses the control, and a screen
reader announces the label's text as the control's name.

A label can also hold its control, and then needs no `for`:

**Example: Around its control.** A label that holds its control needs no for.

```html
<sl:label><input type="checkbox" name="newsletter"> Send me the newsletter</sl:label>
```

In a [field](https://shadleaf.dev/0.11.0-SNAPSHOT/components/field.md), use `<sl:field-label>`: it points at the field's control by itself.

## Disabled

A label fades with a disabled control it holds, or one right before or after it, including a control disabled by a
`<fieldset disabled>` around it. There is nothing to add to the label.

## Attributes

Attributes of `<sl:label>`: A label for a form control. Point it at the control with `for` (the control's `id`; `th:field` gives the control an `id` equal to the field name), or put the control inside it.

`sl:label` declares no props; every attribute passes through.
