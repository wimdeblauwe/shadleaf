# Switch

> An on/off control, bound to a boolean form field with th:field.

Source: https://shadleaf.dev/0.9.0-SNAPSHOT/components/switch/

**Example: Switch.**

```html
<sl:label><sl:switch th:field="*{airplaneMode}"/>Airplane mode</sl:label>
```

A switch is a checkbox with `role="switch"`, so a screen reader announces it as on or off and a form submits it as a
checkbox. `th:field` on a boolean field works as it does on the [checkbox](https://shadleaf.dev/0.9.0-SNAPSHOT/components/checkbox.md), including the numbered `id`:
put the switch inside its label.

Use a switch for a setting that takes effect as it is, and a checkbox for a choice the user confirms by submitting a
form.

## Sizes

**Example: Sizes.**

```html
<sl:label><sl:switch size="sm" name="compact" checked/>Small</sl:label>
<sl:label><sl:switch name="regular" checked/>Default</sl:label>
```

## Invalid

**Example: Invalid.**

```html
<sl:label><sl:switch th:field="*{marketing}"/>Marketing emails</sl:label>
```

## Disabled

Track and thumb fade together, and so does the label. A disabled switch is not submitted, so Spring leaves out the
hidden marker input it adds for a `th:field` checkbox: the bound value stays as it was.

**Example: Disabled.**

```html
<sl:label><sl:switch name="sso" checked disabled/>Single sign-on (managed by your organisation)</sl:label>
<sl:label><sl:switch name="betaFeatures" disabled/>Beta features (not in your plan)</sl:label>
```

## Attributes

Attributes of `<sl:switch>`: A switch: a checkbox with `role="switch"`, announced as on or off. It submits like a checkbox, so `th:field` on a boolean works as it does there (numbered id, hidden marker input; in a `sl:field`, the field's name as id). Every attribute that is not a prop passes through; `class` is appended to `switch`. With `th:field`, the switch is marked `aria-invalid` when its field has errors.

| Attribute | Values | Default | Description |
| --- | --- | --- | --- |
| `size` | `sm`, `default` | `default` | Size of the switch. |

Every prop can also be set from an expression: `th:size="${...}"`. Any other attribute passes through to the rendered element, and `class` is appended.
