# Input Group

> Text, icons and buttons inside the border of an input or textarea.

Source: https://shadleaf.dev/0.11.0-SNAPSHOT/components/input-group/

**Example: Input group.**

```html
<sl:input-group>
  <sl:input-group-input type="search" name="q" placeholder="Search..." aria-label="Search"/>
  <sl:input-group-addon><sl:icon name="search"/></sl:input-group-addon>
  <sl:input-group-addon align="inline-end">12 results</sl:input-group-addon>
</sl:input-group>
```

## Composition

```text
sl:input-group
├── sl:input-group-input or sl:input-group-textarea
└── sl:input-group-addon
    ├── sl:input-group-text
    ├── sl:input-group-button
    ├── sl:icon
    └── sl:kbd
```

The group draws the border, and its control has none of its own: the group shows the control's focus ring, its error
and its read-only look. A disabled control fades the whole group. A click on an addon, outside a button or link in it,
focuses the control.

## Align

Write the addons **after** the control, so the control comes first in the tab order and is read before them; `align`
on the addon places it. `inline-start`, the default, puts it before the control on its line:

**Example: Inline start.** The default align. The addon is written after the input, so the input comes first in the tab order.

```html
<sl:input-group>
  <sl:input-group-input name="query" placeholder="Search..." aria-label="Search"/>
  <sl:input-group-addon><sl:icon name="search"/></sl:input-group-addon>
</sl:input-group>
```

### inline-end

**Example: Inline end.**

```html
<sl:input-group>
  <sl:input-group-input type="password" name="apiKey" placeholder="Enter password" aria-label="Password"/>
  <sl:input-group-addon align="inline-end"><sl:icon name="eye-off"/></sl:input-group-addon>
</sl:input-group>
```

### block-start

`block-start` puts the addon above the control, across the whole group. `border` draws a line between the addon and
the control. In such a row an `sl:input-group-text` takes the free space, so what follows it sits at the end.

**Example: Block start.** Above the input, across the whole group. border draws a line between the addon and the control.

```html
<sl:input-group>
  <sl:input-group-input id="input-group-full-name" name="fullName" placeholder="Jane Doe"/>
  <sl:input-group-addon align="block-start">
    <sl:label for="input-group-full-name">Full name</sl:label>
  </sl:input-group-addon>
</sl:input-group>
<sl:input-group>
  <sl:input-group-textarea name="script" rows="3" aria-label="Script">console.log('Hello, world!');</sl:input-group-textarea>
  <sl:input-group-addon align="block-start" border>
    <sl:input-group-text><sl:icon name="file-code"/> script.js</sl:input-group-text>
    <sl:input-group-button size="icon-xs" aria-label="Copy"><sl:icon name="copy"/></sl:input-group-button>
  </sl:input-group-addon>
</sl:input-group>
```

### block-end

**Example: Block end.** Below the input, across the whole group.

```html
<sl:input-group>
  <sl:input-group-input name="price" placeholder="Enter amount" aria-label="Amount"/>
  <sl:input-group-addon align="block-end">
    <sl:input-group-text>USD</sl:input-group-text>
  </sl:input-group-addon>
</sl:input-group>
<sl:input-group>
  <sl:input-group-textarea name="comment" rows="3" placeholder="Write a comment..." aria-label="Comment"/>
  <sl:input-group-addon align="block-end" border>
    <sl:input-group-text>0/280 characters</sl:input-group-text>
    <sl:input-group-button variant="default" size="sm" type="submit">Post</sl:input-group-button>
  </sl:input-group-addon>
</sl:input-group>
```

## Text

`sl:input-group-text` holds muted text: a currency, a unit, a domain. The input still needs a name of its own, as the
text is not its label: `aria-label="Amount in US dollars"`.

**Example: Text.** A currency, a unit or a domain before or after the input.

```html
<sl:input-group>
  <sl:input-group-input type="number" name="amount" placeholder="0.00" aria-label="Amount in US dollars"/>
  <sl:input-group-addon><sl:input-group-text>$</sl:input-group-text></sl:input-group-addon>
  <sl:input-group-addon align="inline-end"><sl:input-group-text>USD</sl:input-group-text></sl:input-group-addon>
</sl:input-group>
<sl:input-group>
  <sl:input-group-input name="website" placeholder="example" aria-label="Website"/>
  <sl:input-group-addon><sl:input-group-text>https://</sl:input-group-text></sl:input-group-addon>
  <sl:input-group-addon align="inline-end"><sl:input-group-text>.com</sl:input-group-text></sl:input-group-addon>
</sl:input-group>
<sl:input-group>
  <sl:input-group-input type="number" name="weight" aria-label="Weight in kilograms"/>
  <sl:input-group-addon align="inline-end"><sl:input-group-text>kg</sl:input-group-text></sl:input-group-addon>
</sl:input-group>
```

## Button

`sl:input-group-button` is an `sl:button`, ghost and `xs` by default, with sizes that fit inside the group: `xs`,
`sm`, `icon-xs` and `icon-sm`. An icon button needs an `aria-label`. As every `sl:button`, it does not submit unless
you write `type="submit"`.

**Example: Button.** An icon button needs an aria-label.

```html
<sl:input-group>
  <sl:input-group-input name="shareUrl" value="https://shadleaf.dev" readonly aria-label="Link"/>
  <sl:input-group-addon align="inline-end">
    <sl:input-group-button size="icon-xs" aria-label="Copy"><sl:icon name="copy"/></sl:input-group-button>
  </sl:input-group-addon>
</sl:input-group>
<sl:input-group>
  <sl:input-group-input type="search" name="docsSearch" placeholder="Type to search..." aria-label="Search the docs"/>
  <sl:input-group-addon align="inline-end">
    <sl:input-group-button variant="secondary" type="submit">Search</sl:input-group-button>
  </sl:input-group-addon>
</sl:input-group>
```

## Kbd

The key is decoration: say the shortcut with `aria-keyshortcuts` on the input, and hide the addon from screen readers.

**Example: Kbd.**

```html
<sl:input-group>
  <sl:input-group-input type="search" name="commandSearch" placeholder="Search..." aria-label="Search"
                        aria-keyshortcuts="Control+K"/>
  <sl:input-group-addon><sl:icon name="search"/></sl:input-group-addon>
  <sl:input-group-addon align="inline-end" aria-hidden="true"><sl:kbd>⌘K</sl:kbd></sl:input-group-addon>
</sl:input-group>
```

## Loading

`loading` on the button shows a spinner, disables it and announces the busy state.

**Example: Loading.** A loading button shows a spinner, announces the busy state and is disabled.

```html
<sl:input-group>
  <sl:input-group-input name="username" value="wim" aria-label="Username"/>
  <sl:input-group-addon align="inline-end">
    <sl:input-group-button loading>Check</sl:input-group-button>
  </sl:input-group-addon>
</sl:input-group>
```

## In a field

Put `th:field` on the [field](https://shadleaf.dev/0.11.0-SNAPSHOT/components/field.md), as for an [input](https://shadleaf.dev/0.11.0-SNAPSHOT/components/input.md). The `sl:input-group-input` (or
`sl:input-group-textarea`) inside the group takes it: Spring sets its `id`, `name` and value, the label points at it,
and it is described by the field's description and error.

```html
<form th:object="${order}" th:action="@{/orders}" method="post">
  <sl:field th:field="*{budget}">
    <sl:field-label>Budget</sl:field-label>
    <sl:input-group>
      <sl:input-group-input type="number" placeholder="0.00"/>
      <sl:input-group-addon><sl:input-group-text>€</sl:input-group-text></sl:input-group-addon>
      <sl:input-group-addon align="inline-end"><sl:input-group-text>EUR</sl:input-group-text></sl:input-group-addon>
    </sl:input-group>
    <sl:field-description>Excluding VAT.</sl:field-description>
    <sl:field-error/>
  </sl:field>
</form>
```

**Example: In a field.** Put th:field on the field. The input in the group takes it, and the label, description and error point at the input.

```html
<sl:field th:field="*{budget}">
  <sl:field-label>Budget</sl:field-label>
  <sl:input-group>
    <sl:input-group-input type="number" placeholder="0.00"/>
    <sl:input-group-addon><sl:input-group-text>€</sl:input-group-text></sl:input-group-addon>
    <sl:input-group-addon align="inline-end"><sl:input-group-text>EUR</sl:input-group-text></sl:input-group-addon>
  </sl:input-group>
  <sl:field-description>Excluding VAT.</sl:field-description>
  <sl:field-error/>
</sl:field>
```

Outside a field, write `th:field` on the `sl:input-group-input` itself.

## Invalid

When the field has errors, the input marks itself `aria-invalid="true"`, and the group draws the destructive border
and ring.

**Example: Invalid.** With th:field, a field that has errors marks the input aria-invalid, and the group draws the error.

```html
<sl:field th:field="*{packageWeight}">
  <sl:field-label>Weight</sl:field-label>
  <sl:input-group>
    <sl:input-group-input type="number"/>
    <sl:input-group-addon align="inline-end"><sl:input-group-text>kg</sl:input-group-text></sl:input-group-addon>
  </sl:input-group>
  <sl:field-error/>
</sl:field>
```

## Disabled

**Example: Disabled.** A disabled input fades the whole group.

```html
<sl:input-group>
  <sl:input-group-input name="discount" value="10" disabled aria-label="Discount"/>
  <sl:input-group-addon align="inline-end"><sl:input-group-text>%</sl:input-group-text></sl:input-group-addon>
</sl:input-group>
```

## Read-only

As a read-only [input](https://shadleaf.dev/0.11.0-SNAPSHOT/components/input.md#read-only): the group gets a muted background, and the value can still be focused,
selected and copied.

**Example: Read-only.**

```html
<sl:input-group>
  <sl:input-group-input name="volume" value="1.5" readonly aria-label="Volume"/>
  <sl:input-group-addon align="inline-end"><sl:input-group-text>l</sl:input-group-text></sl:input-group-addon>
</sl:input-group>
```

## Attributes

### sl:input-group

Attributes of `<sl:input-group>`: An input with text, icons or buttons inside its border: one `sl:input-group-input` or `sl:input-group-textarea`, and `sl:input-group-addon`s around it. Put the addons after the control in the markup, so the control comes first in the tab order and is read first; `align` on the addon places it. The group draws the control's focus ring, error and disabled look. In a `sl:field`, the control takes the field's `th:field`, id and `aria-describedby`.

`sl:input-group` declares no props; every attribute passes through.

### sl:input-group-addon

Attributes of `<sl:input-group-addon>`: Text, an icon, a `sl:kbd` or `sl:input-group-button`s beside or around the control of a `sl:input-group`. Clicking it, outside a button or link, focuses the control. Write it after the control: `align` places it.

| Attribute | Values | Default | Description |
| --- | --- | --- | --- |
| `align` | `inline-start`, `inline-end`, `block-start`, `block-end` | `inline-start` | Where the addon goes: before or after the control on its line, or above or below it across the whole group. |
| `border` | *boolean* | — | With `align` `block-start` or `block-end`, a dividing line between the addon and the control, with matching spacing. |

Every prop can also be set from an expression: `th:align="${...}"`. Any other attribute passes through to the rendered element, and `class` is appended.

### sl:input-group-text

Attributes of `<sl:input-group-text>`: Muted text in a `sl:input-group-addon`: a currency symbol, a unit, a domain. It may hold an `sl:icon`. In an addon above or below the control, it takes the free space, so what follows it sits at the end.

`sl:input-group-text` declares no props; every attribute passes through.

### sl:input-group-button

Attributes of `<sl:input-group-button>`: A button in a `sl:input-group-addon`: a `sl:button`, ghost and extra small by default, sized to fit inside the group. Any other attribute passes through to the `sl:button` (`type`, `loading`, `disabled`, `hx-*`).

| Attribute | Values | Default | Description |
| --- | --- | --- | --- |
| `variant` | `default`, `outline`, `ghost`, `destructive`, `secondary`, `link` | `ghost` | Visual style of the button, as on `sl:button`. |
| `size` | `xs`, `sm`, `icon-xs`, `icon-sm` | `xs` | Size of the button. The `icon` sizes are square, for a button that holds only an icon, and need an `aria-label`. |

Every prop can also be set from an expression: `th:variant="${...}"`. Any other attribute passes through to the rendered element, and `class` is appended. An `aria-label` or `aria-labelledby` is required when `size` is `icon-xs` or `icon-sm`; rendering fails without one.

### sl:input-group-input

Attributes of `<sl:input-group-input>`: The input of a `sl:input-group`: a `sl:input` without a border of its own, as the group draws it. Every attribute that is not a prop (`th:field`, `name`, `placeholder`, `required`, `hx-*`) passes through; `class` is appended to `input`. With `th:field`, the input is marked `aria-invalid` when its field has errors. In a `sl:field`, it takes the field's `th:field`, id and `aria-describedby`.

| Attribute | Values | Default | Description |
| --- | --- | --- | --- |
| `type` | `text`, `email`, `password`, `number`, `search`, `tel`, `url`, `date`, `datetime-local`, `month`, `week`, `time` | `text` | Type of the input. |

Every prop can also be set from an expression: `th:type="${...}"`. Any other attribute passes through to the rendered element, and `class` is appended.

### sl:input-group-textarea

Attributes of `<sl:input-group-textarea>`: The textarea of a `sl:input-group`: a `sl:textarea` without a border of its own, as the group draws it, that grows with its content where the browser supports it. Addons go above or below it (`align` `block-start` or `block-end`). The content is the initial text, unless `th:field` sets it. Every attribute that is not a prop passes through; `class` is appended to `textarea`. With `th:field`, the textarea is marked `aria-invalid` when its field has errors. In a `sl:field`, it takes the field's `th:field`, id and `aria-describedby`.

`sl:input-group-textarea` declares no props; every attribute passes through.
