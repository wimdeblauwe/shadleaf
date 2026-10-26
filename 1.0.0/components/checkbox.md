# Checkbox

> The browser's own checkbox, styled, bound to a form field with th:field.

Source: https://shadleaf.dev/1.0.0/components/checkbox/

**Example: Checkbox.**

```html
<sl:label><sl:checkbox th:field="*{terms}"/>Accept the terms and conditions</sl:label>
```

## Binding to a form field

`th:field` on a boolean field sets `checked` and adds the hidden `_terms` input Spring uses to see an unchecked box.
For a collection field, give each checkbox a `value` (or `th:value`), as with a plain checkbox.

```html
<form th:object="${signup}" th:action="@{/signup}" method="post">
  <sl:label><sl:checkbox th:field="*{terms}"/>Accept the terms and conditions</sl:label>
</form>
```

## Labelling it

Put the checkbox inside its [label](https://shadleaf.dev/1.0.0/components/label.md): clicking the text toggles it, and it needs no `id`. Spring numbers the
`id` of a checkbox (`terms1`), so a label with `for="terms"` would miss it. For a label beside the checkbox, give the
checkbox an explicit `id`, which wins over the numbered one:

**Example: With a separate label.** Give the checkbox an explicit id for the label's for; th:field would number it.

```html
<sl:checkbox id="checkbox-notify" name="notify"/>
<sl:label for="checkbox-notify">Notify me by email</sl:label>
```

In a [field](https://shadleaf.dev/1.0.0/components/field.md) with `th:field`, the checkbox gets the field's name as its `id` (`terms`), and the field's
label points at it. The field also shows the error message.

## Checked

**Example: Checked.**

```html
<sl:label><sl:checkbox name="newsletter" checked/>Send me the newsletter</sl:label>
```

## Indeterminate

Neither checked nor unchecked: some of a group is selected. HTML has no attribute for it, only the DOM property
`indeterminate`. `indeterminate` on `<sl:checkbox>` renders `data-indeterminate="true"`, from which Shadleaf's script
sets the property when the page loads or htmx swaps the checkbox in; the box then shows a minus, and a screen reader
says "mixed". A click clears it and checks the box, as with any checkbox. A script of yours can set the property on
any checkbox at any time; the minus follows it (the CSS reads `:indeterminate`). The script is not an Alpine
component, so it also runs with `shadleaf.assets.alpine=external`; without JavaScript the checkbox is unchecked.

**Example: Indeterminate.** Some of a group selected. A click clears the state, as on any checkbox; a script can set it again.

```html
<sl:label><sl:checkbox name="toppings-all" indeterminate/>All toppings</sl:label>
```

[Select-all in a table](https://shadleaf.dev/1.0.0/components/table.md#selecting-rows) is the common case, and sets the state itself.

## Invalid

A field with errors marks the checkbox `aria-invalid="true"`, as with the [input](https://shadleaf.dev/1.0.0/components/input.md).

**Example: Invalid.** With th:field, a field that has errors marks the checkbox aria-invalid by itself.

```html
<sl:label><sl:checkbox th:field="*{terms}"/>Accept the terms and conditions</sl:label>
```

## Disabled

A disabled checkbox fades with its check mark, and a label fades with the disabled checkbox it holds, or sits next
to. In Windows' high-contrast mode the browser draws its own disabled checkbox. HTML has no read-only checkbox:
`readonly` does nothing on one.

**Example: Disabled.**

```html
<sl:label><sl:checkbox name="archived" checked disabled/>Archived</sl:label>
<sl:label><sl:checkbox name="deleted" disabled/>Deleted</sl:label>
```

## How it is drawn

The check mark and the minus are icons inside a wrapper around the `<input>`, not CSS background images, so the checkbox renders
under a strict `img-src` policy. `class` goes to that wrapper; every other attribute to the `<input>`. In Windows'
high-contrast mode the browser draws its own checkbox, with its own dash when it is indeterminate.

## Attributes

Attributes of `<sl:checkbox>`: A checkbox: the browser's own, styled. Every attribute that is not a prop (`th:field`, `name`, `value`, `checked`, `required`) passes through to the `input`, except `class`, which goes to the wrapper that holds the check mark. With `th:field`, Spring numbers the id (`terms1`), so put the checkbox inside its label, in a `sl:field` (which gives it the field's name as id), or give it an explicit `id`; the checkbox is marked `aria-invalid` when its field has errors. A checkbox in the indeterminate state (neither checked nor unchecked: some of a group selected) shows a minus.

| Attribute | Values | Default | Description |
| --- | --- | --- | --- |
| `indeterminate` | *boolean* | — | Start in the indeterminate state. HTML has no attribute for it, only the DOM property `indeterminate`: this renders `data-indeterminate`, from which Shadleaf's script sets the property when the page loads (or htmx swaps the checkbox in). A click clears it, as it does on every checkbox. A script can set the property on any checkbox; the minus follows it. |

Every prop can also be set from an expression: `th:indeterminate="${...}"`. Any other attribute passes through to the rendered element, and `class` is appended.
