# Select

> shadcn/ui's Select on a real select element, bound with th:field; the browser's customizable select where it has one.

Source: https://shadleaf.dev/current/components/select/

**Example: Select.** A placeholder until an option is chosen; th:field marks the bound one.

```html
<sl:field th:field="*{fruit}">
  <sl:field-label>Fruit</sl:field-label>
  <sl:select placeholder="Select a fruit">
    <sl:select-group label="Fruits">
      <sl:select-item value="apple">Apple</sl:select-item>
      <sl:select-item value="banana">Banana</sl:select-item>
      <sl:select-item value="blueberry">Blueberry</sl:select-item>
      <sl:select-item value="grapes">Grapes</sl:select-item>
      <sl:select-item value="pineapple">Pineapple</sl:select-item>
    </sl:select-group>
  </sl:select>
</sl:field>
```

## Built from parts

A select is `<sl:select>` holding `<sl:select-item>`s, optionally in `<sl:select-group>`s with
`<sl:select-separator>`s between them. It renders a real `<select>` with `<option>`, `<optgroup>` and `<hr>`
elements, so `th:field` binds it as it binds any select: Spring marks the bound item as selected, and the form submits
the chosen value.

```html
<form th:object="${order}" th:action="@{/orders}" method="post">
  <sl:field th:field="*{fruit}">
    <sl:field-label>Fruit</sl:field-label>
    <sl:select placeholder="Select a fruit">
      <sl:select-group label="Fruits">
        <sl:select-item th:each="fruit : ${fruits}" th:value="${fruit.id}">[[${fruit.name}]]</sl:select-item>
      </sl:select-group>
    </sl:select>
    <sl:field-error/>
  </sl:field>
</form>
```

Give an item its text as content (`[[${fruit.name}]]`), not with `th:text`: that attribute passes through to the
`<option>` and replaces its check mark too. (The select still works; the browser's own check mark shows instead.)

## Select or native select?

Both are a real `<select>` underneath, bind with `th:field` and submit with the form.

- **`<sl:select>`** looks like shadcn/ui's Select: its list is drawn in the page's style, with a check mark on the
  chosen item, and items can start with an icon. Use it where the look matters and the list is short enough to scan:
  a few to a few dozen items.
- **[`<sl:native-select>`](https://shadleaf.dev/current/components/select-native.md)** styles only the closed control and leaves the list to the operating
  system: on a phone that is the system's own picker wheel or sheet, which many people find easier to use. Use it for
  long lists (countries, years), inside dense forms, and for `multiple`, which `<sl:select>` does not support.

## Placeholder

`placeholder` adds a first option with an empty value, hidden from the list. It is chosen while the field has no
value, and shown in the muted colour. A `required` select with the placeholder chosen is invalid, as for any select
whose first option has an empty value; with server-side validation (`@NotBlank`, `@NotNull`) the field shows its
error instead.

## Groups, separators and icons

`<sl:select-group label="...">` is an `<optgroup>`: its label shows above its items and is read with them.
`<sl:select-separator/>` is an `<hr>`, which browsers draw as a line in a select's list.

**Example: Groups and separators.**

```html
<sl:field th:field="*{food}">
  <sl:field-label>Food</sl:field-label>
  <sl:select>
    <sl:select-group label="Fruits">
      <sl:select-item value="apple">Apple</sl:select-item>
      <sl:select-item value="banana">Banana</sl:select-item>
    </sl:select-group>
    <sl:select-separator/>
    <sl:select-group label="Vegetables">
      <sl:select-item value="carrot">Carrot</sl:select-item>
      <sl:select-item value="broccoli">Broccoli</sl:select-item>
    </sl:select-group>
  </sl:select>
</sl:field>
```

An item may start with an [icon](https://shadleaf.dev/current/components/icon.md). The closed select shows the chosen item's icon and text.

**Example: Icons.** An item may start with an icon; the select shows it with the chosen item's text. Older browsers, which drop markup inside an option, show the text alone.

```html
<sl:field th:field="*{chartType}">
  <sl:field-label>Chart</sl:field-label>
  <sl:select>
    <sl:select-item value="bar"><sl:icon name="chart-bar"/>Bar</sl:select-item>
    <sl:select-item value="line"><sl:icon name="chart-line"/>Line</sl:select-item>
    <sl:select-item value="pie"><sl:icon name="chart-pie"/>Pie</sl:select-item>
  </sl:select>
</sl:field>
```

Long lists scroll; the list never grows taller than the window.

**Example: Long lists.** The list scrolls when it is taller than the window.

```html
<sl:field th:field="*{timezone}">
  <sl:field-label>Timezone</sl:field-label>
  <sl:select placeholder="Select a timezone">
    <sl:select-group label="Europe &amp; Africa">
      <sl:select-item value="gmt">Greenwich Mean Time (GMT)</sl:select-item>
      <sl:select-item value="cet">Central European Time (CET)</sl:select-item>
      <sl:select-item value="eet">Eastern European Time (EET)</sl:select-item>
      <sl:select-item value="west">Western European Summer Time (WEST)</sl:select-item>
      <sl:select-item value="cat">Central Africa Time (CAT)</sl:select-item>
      <sl:select-item value="eat">East Africa Time (EAT)</sl:select-item>
    </sl:select-group>
    <sl:select-group label="Asia">
      <sl:select-item value="msk">Moscow Time (MSK)</sl:select-item>
      <sl:select-item value="ist">India Standard Time (IST)</sl:select-item>
      <sl:select-item value="cst_china">China Standard Time (CST)</sl:select-item>
      <sl:select-item value="jst">Japan Standard Time (JST)</sl:select-item>
      <sl:select-item value="kst">Korea Standard Time (KST)</sl:select-item>
    </sl:select-group>
    <sl:select-group label="Americas">
      <sl:select-item value="est">Eastern Standard Time (EST)</sl:select-item>
      <sl:select-item value="cst">Central Standard Time (CST)</sl:select-item>
      <sl:select-item value="mst">Mountain Standard Time (MST)</sl:select-item>
      <sl:select-item value="pst">Pacific Standard Time (PST)</sl:select-item>
    </sl:select-group>
  </sl:select>
</sl:field>
```

## Sizes

**Example: Sizes.**

```html
<sl:select size="sm" name="density" aria-label="Density (small)">
  <sl:select-item value="compact">Compact</sl:select-item>
  <sl:select-item value="comfortable">Comfortable</sl:select-item>
</sl:select>
<sl:select name="density" aria-label="Density">
  <sl:select-item value="compact">Compact</sl:select-item>
  <sl:select-item value="comfortable">Comfortable</sl:select-item>
</sl:select>
```

`size` picks the height, as in shadcn/ui. The list opens below the select, lined up with its start and at least as
wide as it, and above it when there is no room below.

## Width

The select sits in a wrapper that also holds the chevron. `class` goes to that wrapper, so `class="w-full"` widens
both; every other attribute goes to the `<select>`.

## In a form

- **`th:field`**, on the select or on its [field](https://shadleaf.dev/current/components/field.md), binds it. In a field, the select also takes the field's
  `id` (the label points at it) and `aria-describedby` (the description and, while there are errors, the error).
- **Errors.** A field with errors marks the select `aria-invalid="true"`, and
  [`<sl:form-errors autofocus/>`](https://shadleaf.dev/current/components/form-errors.md) gives it `autofocus` when it is the first control with errors.
- **In a field set** with a `th:field` of its own (`<sl:field-set th:field="*{plan}">`), the select takes the field
  but no id; give it an `aria-label`, as the legend names the group, not the select.
- **Events.** Choosing an option fires `input` and `change` on the `<select>`, in every browser and with Shadleaf's
  list box too, so `hx-trigger="change"` and your own listeners work unchanged. A script that sets the select's value
  should fire `change` itself, as it should for any select; the list box then shows the new value.
- **Form reset** puts the select back to its rendered value, and the list box follows.

**Example: With errors.** In a field with errors, the select is aria-invalid and described by the message.

```html
<sl:field th:field="*{shipping}">
  <sl:field-label>Shipping</sl:field-label>
  <sl:select placeholder="Select a shipping method">
    <sl:select-item value="standard">Standard</sl:select-item>
    <sl:select-item value="express">Express</sl:select-item>
  </sl:select>
  <sl:field-error/>
</sl:field>
```

## Disabled

`disabled` on the select, or on a `<sl:field-set>` around it, disables it: it fades with its chevron, and the lyra and
nova skins also give it a grey background. `disabled` on an item leaves it in the list, faded; the keyboard and the
pointer skip it.

HTML has no read-only select: `readonly` does nothing on one, and Shadleaf's list box ignores it too. To show a choice
that cannot be changed, use a read-only [input](https://shadleaf.dev/current/components/input.md#read-only) with the item's text, and a hidden input if the
value has to be submitted.

**Example: Disabled.** A disabled select, and a disabled item that the keyboard and the pointer skip.

```html
<sl:field th:field="*{region}">
  <sl:field-label>Region</sl:field-label>
  <sl:select disabled>
    <sl:select-item value="eu">Europe</sl:select-item>
  </sl:select>
</sl:field>
<sl:field th:field="*{seat}">
  <sl:field-label>Seat</sl:field-label>
  <sl:select>
    <sl:select-item value="aisle">Aisle</sl:select-item>
    <sl:select-item value="middle" disabled>Middle</sl:select-item>
    <sl:select-item value="window">Window</sl:select-item>
  </sl:select>
</sl:field>
```

## Keyboard

With the focus on the select:

| Key | Closed | Open |
| --- | --- | --- |
| Arrow down, arrow up | Opens the list on the chosen item | Next, previous item |
| Enter, Space | Opens the list | Chooses the highlighted item and closes |
| Home, End | Opens the list on the first, last item | First, last item |
| A letter | Chooses (customizable select) or highlights (list box) the next item that starts with it | Highlights the next item that starts with it; typing quickly matches a prefix |
| Escape | | Closes without choosing; the value stays as it was |
| Tab | Moves on | The customizable select closes without choosing, the focus staying on it; Shadleaf's list box chooses the highlighted item and moves on, as the ARIA pattern describes |

Disabled items, group labels and separators are skipped. The item under the pointer is highlighted, and only keyboard
focus draws the focus ring as well. Focus stays on the select (or on the list box's button) while the list is open,
and is back there when it closes.

With the customizable select the browser handles the keys itself; `slSelect` only adds what it leaves out: Enter
opens the list, Home and End open it on the first and last item, and the item under the pointer takes the focus. It
also works around a Chrome bug: after a form reset the select showed the option chosen before, not the reset one.

## With htmx

- A response that renders the form again with errors keeps the chosen value (`th:field` marks it), and focus goes
  where `autofocus` says, the select included; see [Forms with htmx](https://shadleaf.dev/current/guides/htmx.md).
- A swap that replaces an open select leaves nothing open behind.
- Back restores the select as htmx saved it; Shadleaf's list box builds itself again from the restored select.
- A select can trigger a request itself: `hx-get="/cities" hx-trigger="change" hx-target="#city-field"`. Swapping in
  new items (`hx-target` on the select) is picked up by the list box.

## Forced colours

The select and its list keep a visible border, and the highlighted item an outline, when the system's colours
replace the page's (Windows High Contrast). A disabled select turns `GrayText` instead of fading.

## Left out

These shadcn/Radix parts and props have no counterpart, on purpose:

- **`SelectTrigger`, `SelectValue` and `SelectContent`.** In HTML the select is the trigger, shows its own value and
  holds its options, so `<sl:select>` covers all three: `size` from the trigger, `placeholder` from the value.
- **`SelectLabel` on its own.** A group's heading is the `label` of `<sl:select-group>` (an `<optgroup>` needs one);
  there is no heading outside a group, which no browser would show.
- **`SelectScrollUpButton`, `SelectScrollDownButton`.** The list scrolls like any other, with the wheel, the keys or a
  finger.
- **`position="item-aligned"`, `side`, `align`.** shadcn's default lays the list over the select with the chosen item
  on top of it; the customizable select always opens below the select (above when there is no room), and the list box
  does the same, so both look alike.
- **State in the browser** (`value`/`defaultValue`/`onValueChange`, `open`/`onOpenChange`): the server renders the
  value with `th:field`, and the select's `change` event reports a new one.
- **`multiple`**, which shadcn does not have either: use [`<sl:native-select multiple>`](https://shadleaf.dev/current/components/select-native.md) or a
  [field set](https://shadleaf.dev/current/components/field.md) of checkboxes.
- **`SelectItem`'s `textValue`**: typeahead reads the item's text.

## How it is drawn

Browsers are halfway through gaining the
[customizable select](https://developer.mozilla.org/en-US/docs/Learn_web_development/Extensions/Forms/Customizable_select)
(`appearance: base-select`), which lets CSS draw a select's list. Checked on 30 September 2026:

| Browser | Customizable select | What Shadleaf does |
| --- | --- | --- |
| Chrome, Edge 135 and later | Yes | The browser draws the list in the skin's style, above everything else, placed with anchor positioning. |
| Safari 27 and later | Yes | The same. |
| Firefox (155 at the time of writing) | Behind a setting | Once Alpine runs, the `slSelect` component hides the select and shows a button and a list box of its own in its place, built from the select's options. |
| Older browsers, or no Alpine | No | The select is drawn like the native select: the closed control has the skin's look, and the list is the browser's own. |

The server always renders the same markup, a real select, so the choice happens in the browser and needs nothing
from you. That also means the form works before Alpine starts and without it: the select submits, binds, validates
and takes the keyboard like any select (rule 4 of [Alpine](https://shadleaf.dev/current/guides/alpine.md)).

The customizable select is used wherever the browser has it, rather than Shadleaf's list box everywhere, because the
browser's own is simply the better control: the keyboard, the screen reader, the top layer, the placing next to the
select and the form all come from the browser, and keep working if the page's JavaScript fails. The list box exists
only for the browsers still missing it, and follows the same
[ARIA select-only combobox pattern](https://www.w3.org/WAI/ARIA/apg/patterns/combobox/examples/combobox-select-only/)
the browsers follow.

Only options with text work everywhere: an older browser drops any markup inside an `<option>` and keeps its text, so
an item's icon is extra, never its only label.

## Attributes

### sl:select

Attributes of `<sl:select>`: A select drawn like shadcn/ui's: a real `select` with `sl:select-item`, `sl:select-group` and `sl:select-separator` as content, so `th:field`, submitting and the keyboard work as for any select. Where the browser supports the customizable select (`appearance: base-select`) it draws the list itself; elsewhere `slSelect` puts a list box in its place once Alpine runs. Every attribute that is not a prop passes through to the `select`, except `class`, which goes to the wrapper around it. In a `sl:field`, it takes the field's `th:field`, id and `aria-describedby`.

| Attribute | Values | Default | Description |
| --- | --- | --- | --- |
| `size` | `sm`, `default` | `default` | Height of the select. |
| `placeholder` | *string* | — | Text shown while no option is chosen: a first option with an empty value, hidden from the list. A required select with it selected is invalid. |

Every prop can also be set from an expression: `th:size="${...}"`. Any other attribute passes through to the rendered element, and `class` is appended.

### sl:select-item

Attributes of `<sl:select-item>`: An option in a `sl:select`: an `option` with a check mark that shows while it is chosen. `value` (or `th:value`) and `disabled` pass through; `th:field` on the select marks the bound one. Its content may start with an icon; always give it text, which is all an older browser keeps of it.

`sl:select-item` declares no props; every attribute passes through.

### sl:select-group

Attributes of `<sl:select-group>`: A group of `sl:select-item`s under a heading: an `optgroup`.

| Attribute | Values | Default | Description |
| --- | --- | --- | --- |
| `label` | *string* | **required** | The group's heading, shadcn/ui's `SelectLabel`. |

Every prop can also be set from an expression: `th:label="${...}"`. Any other attribute passes through to the rendered element, and `class` is appended.

### sl:select-separator

Attributes of `<sl:select-separator>`: A line between items or groups of a `sl:select`: an `hr`, which every current browser draws in a select's list.

`sl:select-separator` declares no props; every attribute passes through.

## JavaScript

A small Alpine component, `slSelect`, sits on the select's wrapper:
`<div class="select-wrapper" x-data="slSelect">`.

- **Without Alpine the select works** as the browser's own, customizable or not.
- **Where the customizable select is missing**, `slSelect` hides the `<select>` (it stays in the form, and still
  submits, resets and validates) and adds a `<button role="combobox" class="select select-trigger">` and a
  `<div role="listbox" class="select-content" popover>` after it. The button takes the select's label (its `<label>`s,
  `aria-labelledby` or `aria-label`), `aria-describedby`, `aria-invalid`, `required` and `disabled`, and follows them
  when they change. Focus that reaches the hidden select, from a click on its label, `autofocus` or the browser's
  validation, moves on to the button.
- **It needs no Alpine plugin.** If you load Alpine yourself (`shadleaf.assets.alpine=external`), plain Alpine is
  enough.
- **Keep your own `x-data` off `<sl:select>`**: `class` is the only attribute that reaches the wrapper, and an element
  has one `x-data`. Listen for `change` on the select instead.
