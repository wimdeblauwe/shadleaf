# Alert

> A callout for a message the user should notice.

Source: https://shadleaf.dev/1.1.0-SNAPSHOT/components/alert/

**Example: Alert.**

```html
<sl:alert>
  <sl:icon name="circle-check"/>
  <sl:alert-title>Order placed</sl:alert-title>
  <sl:alert-description>We will email you when it ships.</sl:alert-description>
</sl:alert>
```

## Built from parts

`<sl:alert>` holds an optional [`<sl:icon>`](https://shadleaf.dev/1.1.0-SNAPSHOT/components/icon.md), then `<sl:alert-title>` and `<sl:alert-description>`. The icon
takes the alert's colour and sits beside both. Use either part on its own.

**Example: Title only, no icon.**

```html
<sl:alert>
  <sl:alert-title>Your trial ends in 3 days.</sl:alert-title>
</sl:alert>
```

## Destructive

**Example: Destructive.**

```html
<sl:alert variant="destructive">
  <sl:icon name="circle-alert"/>
  <sl:alert-title>Payment failed</sl:alert-title>
  <sl:alert-description>
    <p>Your card was declined. Check the details and try again:</p>
    <ul>
      <li>the expiry date</li>
      <li>the billing address</li>
    </ul>
  </sl:alert-description>
</sl:alert>
```

## Announcing an alert

An alert is not announced by itself. Most alerts are part of the page when it loads, and a screen reader reads them in
order with everything else; announcing them too would interrupt the user for no reason.

When an alert appears later, for example in the response to an htmx request, add `role="alert"`. A screen reader then
reads it as soon as it is inserted. Like any other attribute, `role` passes through to the rendered element. An
alert that takes the focus when it appears (`tabindex="-1"` and `autofocus`, which htmx honours in swapped content) is
read out because of the focus, and needs no role; see [Forms with htmx](https://shadleaf.dev/1.1.0-SNAPSHOT/guides/htmx.md).

**Example: Announced.** role="alert" makes a screen reader read the alert as soon as it appears.

```html
<sl:alert variant="destructive" role="alert">
  <sl:icon name="circle-alert"/>
  <sl:alert-title>The order could not be saved</sl:alert-title>
  <sl:alert-description>Someone else changed it. Reload the page to see their changes.</sl:alert-description>
</sl:alert>
```

For a message that is not urgent, `role="status"` is announced when the user is idle instead of at once.

## Attributes

### sl:alert

Attributes of `<sl:alert>`: A callout for a message the user should notice: an optional `sl:icon`, then `sl:alert-title` and `sl:alert-description`. It is not announced by itself; add `role="alert"` when it appears after the page loaded, such as in an htmx response.

| Attribute | Values | Default | Description |
| --- | --- | --- | --- |
| `variant` | `default`, `destructive` | `default` | Visual style of the alert. |

Every prop can also be set from an expression: `th:variant="${...}"`. Any other attribute passes through to the rendered element, and `class` is appended.

### sl:alert-title

Attributes of `<sl:alert-title>`: The title of an alert, inside `sl:alert`.

| Attribute | Values | Default | Description |
| --- | --- | --- | --- |
| `level` | `1`, `2`, `3`, `4`, `5`, `6` | — | Heading level. With it the title is a heading (`role="heading"` and `aria-level`) that screen reader users can jump to; without it, plain text. |

Every prop can also be set from an expression: `th:level="${...}"`. Any other attribute passes through to the rendered element, and `class` is appended.

### sl:alert-description

Attributes of `<sl:alert-description>`: The body of an alert, inside `sl:alert`: text, paragraphs or a list.

`sl:alert-description` declares no props; every attribute passes through.
