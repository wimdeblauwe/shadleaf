# Input

> A text input, bound to a form field with th:field.

Source: https://shadleaf.dev/1.1.0-SNAPSHOT/components/input/

**Example: Input.**

```html
<sl:label for="email">Email</sl:label>
<sl:input type="email" th:field="*{email}" placeholder="you@example.com"/>
```

## Binding to a form field

Inside a form with `th:object`, write `th:field` as on a plain `<input>`. Spring sets the input's `id`, `name` and
`value`, so a [label](https://shadleaf.dev/1.1.0-SNAPSHOT/components/label.md) with `for` set to the field name points at it:

```html
<form th:object="${profile}" th:action="@{/profile}" method="post">
  <sl:label for="email">Email</sl:label>
  <sl:input type="email" th:field="*{email}"/>
</form>
```

An explicit `id` wins over the one `th:field` gives. In a [field](https://shadleaf.dev/1.1.0-SNAPSHOT/components/field.md), put `th:field` on the field instead: it
adds the label, description and error messages and wires them to the input.

## Types

`type` defaults to `text` and takes the text-like input types. Checkboxes, radio buttons and switches get components of
their own.

**Example: Types.**

```html
<sl:label for="input-password">Password</sl:label>
<sl:input type="password" id="input-password" name="password"/>
<sl:label for="input-date">Delivery date</sl:label>
<sl:input type="date" id="input-date" name="deliveryDate"/>
<sl:label for="input-file">Invoice</sl:label>
<sl:input type="file" id="input-file" name="invoice"/>
```

## Invalid

When the field has errors after a `@Valid` submit, the input marks itself `aria-invalid="true"`, which the skins draw
in the destructive colour. A [field](https://shadleaf.dev/1.1.0-SNAPSHOT/components/field.md) shows the error message and links it with `aria-describedby`; outside
one, an `aria-describedby` you write passes through.

**Example: Invalid.** With th:field, a field that has errors marks the input aria-invalid by itself.

```html
<sl:label for="email">Email</sl:label>
<sl:input type="email" th:field="*{email}"/>
```

Without `th:field`, write `aria-invalid="true"` yourself.

## Disabled

A disabled input fades and cannot be focused; the lyra and nova skins also give it a grey background. Its
value is not submitted, and its label fades with it. In Windows' high-contrast mode it turns the system's grey text
colour instead of fading.

**Example: Disabled.**

```html
<sl:label for="input-disabled">Customer number</sl:label>
<sl:input id="input-disabled" name="customerNumber" value="C-1042" disabled/>
```

## Read-only

`readonly` keeps the value but stops the user from changing it. Unlike a disabled input, a read-only one still takes
focus, its value can be selected and copied, and it is submitted with the form: use it for a value the user needs to
see or quote, such as a reference number. It gets a muted background and loses its shadow, but keeps its full text
colour, so it reads differently from a disabled input and its text keeps its contrast (a read-only input is not
exempt from WCAG's contrast minimum, a disabled one is). Errors still show: the destructive border and ring win over
the read-only look.

**Example: Read-only.** The value cannot be changed, but it can be focused, selected and copied, and it is submitted.

```html
<sl:label for="input-reference">Order reference</sl:label>
<sl:input id="input-reference" name="reference" value="ORD-2026-0042" readonly/>
```

## With htmx

Every attribute that is not a prop lands on the `<input>`:

**Example: With htmx.** Every attribute that is not a prop lands on the input.

```html
<sl:input type="search" name="q" aria-label="Search orders"
          th:hx-get="@{/orders}" hx-trigger="input changed delay:300ms" hx-target="#orders"/>
```

## Attributes

Attributes of `<sl:input>`: A text input. Every attribute that is not a prop (`th:field`, `name`, `placeholder`, `required`, `hx-*`) passes through; `class` is appended to `input`. With `th:field`, the input is marked `aria-invalid` when its field has errors. In a `sl:field`, it takes the field's `th:field`, id and `aria-describedby`.

| Attribute | Values | Default | Description |
| --- | --- | --- | --- |
| `type` | `text`, `email`, `password`, `number`, `search`, `tel`, `url`, `date`, `datetime-local`, `month`, `week`, `time`, `file` | `text` | Type of the input. Checkboxes and radio buttons have components of their own. |

Every prop can also be set from an expression: `th:type="${...}"`. Any other attribute passes through to the rendered element, and `class` is appended.
