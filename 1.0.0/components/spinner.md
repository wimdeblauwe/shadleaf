# Spinner

> A spinning icon that says something is loading.

Source: https://shadleaf.dev/1.0.0/components/spinner/

**Example: Spinner.**

```html
<sl:spinner/>
```

## What it says

A spinner is a status (`role="status"`) named "Loading". Give it an `aria-label` of its own to say what is loading;
the default is the message `sl.spinner.label`, which you can reword or [translate](https://shadleaf.dev/1.0.0/guides/translating.md) in your `messages.properties`.

**Example: Saying what loads.** An aria-label replaces the default name, "Loading".

```html
<sl:spinner aria-label="Loading invoices"/>
```

For a button that is busy, use [`<sl:button loading>`](https://shadleaf.dev/1.0.0/components/button.md) instead: it shows the spinner, disables the button
and announces the busy state. To show a skeleton of the content to come, use [`<sl:skeleton>`](https://shadleaf.dev/1.0.0/components/skeleton.md).

## Sizes

A spinner is 16 pixels, or the size of an icon in the component around it, such as
[`<sl:empty-media variant="icon">`](https://shadleaf.dev/1.0.0/components/empty.md#while-it-loads). `size` sets the width and height in pixels.

**Example: Sizes.**

```html
<sl:spinner/>
<sl:spinner size="24"/>
<sl:spinner size="32"/>
```

## Reduced motion

For users who ask for reduced motion the spinner turns slower instead of standing still: a frozen arc reads as a
broken icon.

## Attributes

Attributes of `<sl:spinner>`: A spinning icon that says something is loading: a status (`role="status"`) named "Loading" (the message `sl.spinner.label`; an `aria-label` of its own replaces it). It turns slower, not still, when the user asks for reduced motion. For a button that is busy, use `sl:button loading` instead.

| Attribute | Values | Default | Description |
| --- | --- | --- | --- |
| `size` | *number* | — | Width and height in pixels. Without it the spinner is 16 pixels, or sized by the surrounding component (as an icon in `sl:empty-media`). |

Every prop can also be set from an expression: `th:size="${...}"`. Any other attribute passes through to the rendered element, and `class` is appended.
