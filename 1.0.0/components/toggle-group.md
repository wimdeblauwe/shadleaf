# Toggle Group

> A set of toggles of which one or several can be on, as radio buttons or checkboxes bound with th:field.

Source: https://shadleaf.dev/1.0.0/components/toggle-group/

**Example: Toggle group.**

```html
<sl:toggle-group type="multiple" name="textStyle" aria-label="Text style">
  <sl:toggle-group-item value="bold" aria-label="Bold"><sl:icon name="bold"/></sl:toggle-group-item>
  <sl:toggle-group-item value="italic" aria-label="Italic"><sl:icon name="italic"/></sl:toggle-group-item>
  <sl:toggle-group-item value="underline" aria-label="Underline"><sl:icon name="underline"/></sl:toggle-group-item>
</sl:toggle-group>
```

The items of a toggle group are [toggles](https://shadleaf.dev/1.0.0/components/toggle.md) around radio buttons (`type="single"`, the default: one item is
on at a time) or checkboxes (`type="multiple"`: any number). The form submits them as such inputs, and they work
without JavaScript. In a `single` group the arrow keys move between the items and Tab leaves the group, as in any
group of radio buttons; in a `multiple` group every item is a tab stop.

Name the group with `aria-label`, or with `aria-labelledby` pointing at its label. Give each item a `value`; an item
that holds only an icon needs an `aria-label`.

## Outline

**Example: Outline.**

```html
<sl:toggle-group variant="outline" name="callFilter" aria-label="Calls">
  <sl:toggle-group-item value="all" checked>All</sl:toggle-group-item>
  <sl:toggle-group-item value="missed">Missed</sl:toggle-group-item>
</sl:toggle-group>
```

## In a form

Write `th:field` on the group, once. The items share it: Spring gives them the field's `name`, numbers their ids
(`layout1`, `layout2`, ...) and checks the one whose value matches the field. Without a form object, `name` on the
group gives the items their name. A `single` group with neither gets a generated name, so its items still exclude
each other; give it a `name` when the form should submit it.

**Example: In a form.** th:field on the group: the items are radio buttons that share it, and the one with the field's value is on.

```html
<p id="layout-label">Layout</p>
<sl:toggle-group th:field="*{layout}" variant="outline" aria-labelledby="layout-label">
  <sl:toggle-group-item value="list"><sl:icon name="list"/> List</sl:toggle-group-item>
  <sl:toggle-group-item value="grid"><sl:icon name="layout-grid"/> Grid</sl:toggle-group-item>
  <sl:toggle-group-item value="table"><sl:icon name="table-2"/> Table</sl:toggle-group-item>
</sl:toggle-group>
```

In a [field set](https://shadleaf.dev/1.0.0/components/field.md#radio-buttons-and-checkbox-groups) with `th:field`, the group takes the field set's, and
the legend labels it.

## Multiple

With `type="multiple"`, bind a collection (a `List`, a `Set`, an array). Spring checks every item whose value is in
it, and adds a hidden marker input, so a form with no item on clears the collection.

**Example: Multiple.** type="multiple" makes the items checkboxes: th:field binds a collection, and Spring's hidden marker input lets the form clear it.

```html
<sl:toggle-group type="multiple" th:field="*{workdays}" variant="outline" aria-label="Workdays">
  <sl:toggle-group-item value="MONDAY">Mon</sl:toggle-group-item>
  <sl:toggle-group-item value="TUESDAY">Tue</sl:toggle-group-item>
  <sl:toggle-group-item value="WEDNESDAY">Wed</sl:toggle-group-item>
  <sl:toggle-group-item value="THURSDAY">Thu</sl:toggle-group-item>
  <sl:toggle-group-item value="FRIDAY">Fri</sl:toggle-group-item>
</sl:toggle-group>
```

## Sizes

**Example: Sizes.**

```html
<sl:toggle-group size="sm" variant="outline" spacing="0" name="positionSm" aria-label="Position, small">
  <sl:toggle-group-item value="top" checked>Top</sl:toggle-group-item>
  <sl:toggle-group-item value="bottom">Bottom</sl:toggle-group-item>
</sl:toggle-group>
<sl:toggle-group variant="outline" spacing="0" name="positionDefault" aria-label="Position">
  <sl:toggle-group-item value="top" checked>Top</sl:toggle-group-item>
  <sl:toggle-group-item value="bottom">Bottom</sl:toggle-group-item>
</sl:toggle-group>
<sl:toggle-group size="lg" variant="outline" spacing="0" name="positionLg" aria-label="Position, large">
  <sl:toggle-group-item value="top" checked>Top</sl:toggle-group-item>
  <sl:toggle-group-item value="bottom">Bottom</sl:toggle-group-item>
</sl:toggle-group>
```

## Spacing

`spacing` sets the gap between the items, in Tailwind's spacing steps: `2` (`0.5rem`) by default, `0` to join them
into one control.

**Example: Spacing.** spacing="0" joins the items into one control; the default is 2.

```html
<sl:toggle-group variant="outline" spacing="0" name="alignment" aria-label="Alignment">
  <sl:toggle-group-item value="left" checked aria-label="Align left"><sl:icon name="align-left"/></sl:toggle-group-item>
  <sl:toggle-group-item value="center" aria-label="Align center"><sl:icon name="align-center"/></sl:toggle-group-item>
  <sl:toggle-group-item value="right" aria-label="Align right"><sl:icon name="align-right"/></sl:toggle-group-item>
</sl:toggle-group>
<sl:toggle-group variant="outline" spacing="4" name="alignmentSpaced" aria-label="Alignment, spaced">
  <sl:toggle-group-item value="left" checked aria-label="Align left"><sl:icon name="align-left"/></sl:toggle-group-item>
  <sl:toggle-group-item value="center" aria-label="Align center"><sl:icon name="align-center"/></sl:toggle-group-item>
  <sl:toggle-group-item value="right" aria-label="Align right"><sl:icon name="align-right"/></sl:toggle-group-item>
</sl:toggle-group>
```

## Vertical

**Example: Vertical.**

```html
<sl:toggle-group orientation="vertical" variant="outline" spacing="0" name="rowDensity" aria-label="Density">
  <sl:toggle-group-item value="compact">Compact</sl:toggle-group-item>
  <sl:toggle-group-item value="comfortable" checked>Comfortable</sl:toggle-group-item>
  <sl:toggle-group-item value="spacious">Spacious</sl:toggle-group-item>
</sl:toggle-group>
```

## Invalid

A field with errors marks every item `aria-invalid="true"`.

**Example: Invalid.** With th:field, a field that has errors marks every item aria-invalid by itself.

```html
<sl:toggle-group th:field="*{shippingSpeed}" variant="outline" aria-label="Shipping">
  <sl:toggle-group-item value="standard">Standard</sl:toggle-group-item>
  <sl:toggle-group-item value="express">Express</sl:toggle-group-item>
</sl:toggle-group>
```

## Disabled

**Example: Disabled.** disabled on the group disables every item; on an item, only that one.

```html
<sl:toggle-group type="multiple" disabled name="textStyleLocked" aria-label="Text style (locked)">
  <sl:toggle-group-item value="bold" checked aria-label="Bold"><sl:icon name="bold"/></sl:toggle-group-item>
  <sl:toggle-group-item value="italic" aria-label="Italic"><sl:icon name="italic"/></sl:toggle-group-item>
</sl:toggle-group>
<sl:toggle-group variant="outline" name="billingPeriod" aria-label="Plan">
  <sl:toggle-group-item value="monthly" checked>Monthly</sl:toggle-group-item>
  <sl:toggle-group-item value="yearly" disabled>Yearly</sl:toggle-group-item>
</sl:toggle-group>
```

## With tooltips

Put an item in a [tooltip](https://shadleaf.dev/1.0.0/components/tooltip.md). The group still joins the items, the tooltip shows on hover and when the
keyboard focuses the item, and its text describes the item's input. An icon-only item already has its
`aria-label`: give the `sl:tooltip` `mode="label"`, so a screen reader does not say it twice.

**Example: With tooltips.** A sl:tooltip around an item. The tooltip shows next to the item, on hover and on keyboard focus; mode="label" because it repeats the item's aria-label.

```html
<sl:toggle-group variant="outline" spacing="0" name="tooltipAlign" aria-label="Alignment with tooltips">
  <sl:tooltip mode="label">
    <sl:toggle-group-item value="left" checked aria-label="Align left"><sl:icon name="align-left"/></sl:toggle-group-item>
    <sl:tooltip-content>Align left</sl:tooltip-content>
  </sl:tooltip>
  <sl:tooltip mode="label">
    <sl:toggle-group-item value="center" aria-label="Align center"><sl:icon name="align-center"/></sl:toggle-group-item>
    <sl:tooltip-content>Align center</sl:tooltip-content>
  </sl:tooltip>
  <sl:tooltip mode="label">
    <sl:toggle-group-item value="right" aria-label="Align right"><sl:icon name="align-right"/></sl:toggle-group-item>
    <sl:tooltip-content>Align right</sl:tooltip-content>
  </sl:tooltip>
</sl:toggle-group>
```

## Attributes

### sl:toggle-group

Attributes of `<sl:toggle-group>`: A row of `sl:toggle-group-item`s, of which one (`type="single"`, radio buttons) or several (`type="multiple"`, checkboxes) can be on. The items submit, bind and work as those inputs, without JavaScript. Name the group with `aria-label` or `aria-labelledby`. Give it the `th:field` (or the `name`) its items share, or put it in a `sl:field-set` with `th:field`; each item then only needs a `value`. Every other attribute that is not a prop passes through to the group.

| Attribute | Values | Default | Description |
| --- | --- | --- | --- |
| `type` | `single`, `multiple` | `single` | `single`: one item is on at a time, as radio buttons (the arrow keys move between them). `multiple`: any number, as checkboxes. |
| `variant` | `default`, `outline` | `default` | Visual style of the items. |
| `size` | `sm`, `default`, `lg` | `default` | Size of the items. |
| `spacing` | `0`, `1`, `2`, `3`, `4` | `2` | The gap between the items, in Tailwind's spacing steps (`2` is `0.5rem`). `0` joins them into one control. |
| `orientation` | `horizontal`, `vertical` | `horizontal` | Put the items on a row, or in a column. |
| `name` | *string* | — | The `name` every item submits under, for a group without `th:field`. A `single` group without either gets a generated one, so its items still exclude each other. |
| `disabled` | *boolean* | — | Disable every item. |

Every prop can also be set from an expression: `th:type="${...}"`. Any other attribute passes through to the rendered element, and `class` is appended.

### sl:toggle-group-item

Attributes of `<sl:toggle-group-item>`: One item of a `sl:toggle-group`, drawn as a `sl:toggle`: a radio button in a `single` group, a checkbox in a `multiple` one. Give it a `value`; it takes the group's `th:field` or `name`, variant, size and `disabled`. The content is its label; give an item that holds only an icon an `aria-label`. Every attribute that is not a prop (`value`, `checked`, `disabled`, `aria-label`) passes through to the `input`, except `class`, which goes to the `label` around it, and `th:text` and `th:utext`, which set the label. With `th:field`, the item is marked `aria-invalid` when its field has errors.

`sl:toggle-group-item` declares no props; every attribute passes through.
