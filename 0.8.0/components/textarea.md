# Textarea

> A multi-line text input, bound to a form field with th:field.

Source: https://wimdeblauwe.github.io/shadleaf/0.8.0/components/textarea/

**Example: Textarea.**

```html
<sl:label for="message">Message</sl:label>
<sl:textarea th:field="*{message}" placeholder="Type your message here."/>
```

The textarea grows with its content in browsers that support `field-sizing`; elsewhere it keeps its height and
scrolls. `rows` sets a starting height.

## Initial text

With `th:field`, Spring fills the textarea from the field. Without it, the component's content is the initial text:

**Example: Initial text.** Without th:field, the content is the initial text.

```html
<sl:label for="textarea-notes">Notes</sl:label>
<sl:textarea id="textarea-notes" name="notes">Leave the parcel at the back door.</sl:textarea>
```

## Invalid

As with the [input](https://wimdeblauwe.github.io/shadleaf/0.8.0/components/input.md), a field with errors marks the textarea `aria-invalid="true"`.

**Example: Invalid.**

```html
<sl:label for="message">Message</sl:label>
<sl:textarea th:field="*{message}"/>
```

## Disabled

As the [input](https://wimdeblauwe.github.io/shadleaf/0.8.0/components/input.md#disabled): faded, not focusable and not submitted; lyra adds a grey background.

**Example: Disabled.**

```html
<sl:label for="textarea-disabled">Message</sl:label>
<sl:textarea id="textarea-disabled" name="message" disabled/>
```

## Read-only

As the [input](https://wimdeblauwe.github.io/shadleaf/0.8.0/components/input.md#read-only): a muted background and full text colour; it takes focus and is submitted.

**Example: Read-only.**

```html
<sl:label for="textarea-instructions">Delivery instructions</sl:label>
<sl:textarea id="textarea-instructions" name="instructions" readonly>Leave the parcel with the neighbours at number 12.</sl:textarea>
```

## Attributes

Attributes of `<sl:textarea>`: A multi-line text input that grows with its content where the browser supports it. The content is the initial text, unless `th:field` sets it. Every attribute that is not a prop passes through; `class` is appended to `textarea`. With `th:field`, the textarea is marked `aria-invalid` when its field has errors. In a `sl:field`, it takes the field's `th:field`, id and `aria-describedby`.

`sl:textarea` declares no props; every attribute passes through.
