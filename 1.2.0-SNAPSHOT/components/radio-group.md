# Radio Group

> A set of the browser's own radio buttons, styled, bound to a form field with th:field; for one choice out of a few options that are all visible.

Source: https://shadleaf.dev/1.2.0-SNAPSHOT/components/radio-group/

A set of radio buttons of which the user picks exactly one, bound to a form field. All the options stay in view.

**Example: Radio group.**

```html
<p id="plan-question">Plan</p>
<sl:radio-group aria-labelledby="plan-question">
  <sl:label th:each="plan : ${plans}">
    <sl:radio-group-item th:field="*{plan}" th:value="${plan}"/>
    <th:block th:text="#{|plan.${plan}|}">Free</th:block>
  </sl:label>
</sl:radio-group>
```

## When to use

- One choice out of two to five options, such as a plan or a delivery method.
- Options the user should see side by side before choosing, such as how often to be notified.

Use something else for:

- A long list: a [select](https://shadleaf.dev/1.2.0-SNAPSHOT/components/select.md) or a [combobox](https://shadleaf.dev/1.2.0-SNAPSHOT/components/combobox.md).
- A yes or no: a [checkbox](https://shadleaf.dev/1.2.0-SNAPSHOT/components/checkbox.md) or a [switch](https://shadleaf.dev/1.2.0-SNAPSHOT/components/switch.md).
- Options that fit on buttons, such as text alignment: a [toggle group](https://shadleaf.dev/1.2.0-SNAPSHOT/components/toggle-group.md).

## Binding to a form field

Write `th:field` on every item, with the item's own `value` or `th:value`. Spring gives them the field's `name`,
numbers their ids (`plan1`, `plan2`, ...) and checks the one whose value matches the field. Because of the numbered
ids, put each item inside its [label](https://shadleaf.dev/1.2.0-SNAPSHOT/components/label.md).

The group is announced as a radio group. Name it with `aria-labelledby`, pointing at the question, or with
`aria-label`.

In a [field set](https://shadleaf.dev/1.2.0-SNAPSHOT/components/field.md#radio-buttons-and-checkbox-groups), `th:field` goes on the field set once, and the items
take it. Its legend asks the question, and it shows the error message and describes the group with it.

## Horizontal

**Example: Horizontal.**

```html
<sl:radio-group orientation="horizontal" aria-label="Density">
  <sl:label><sl:radio-group-item name="density" value="compact"/>Compact</sl:label>
  <sl:label><sl:radio-group-item name="density" value="comfortable" checked/>Comfortable</sl:label>
  <sl:label><sl:radio-group-item name="density" value="spacious"/>Spacious</sl:label>
</sl:radio-group>
```

## Invalid

A field with errors marks every item `aria-invalid="true"`.

**Example: Invalid.** With th:field, a field that has errors marks every item aria-invalid by itself.

```html
<sl:radio-group aria-label="Delivery">
  <sl:label><sl:radio-group-item th:field="*{delivery}" value="standard"/>Standard</sl:label>
  <sl:label><sl:radio-group-item th:field="*{delivery}" value="express"/>Express</sl:label>
</sl:radio-group>
```

## Disabled

A disabled radio button and its label fade, and the keyboard skips it. To disable the whole group, put it in a
[field set](https://shadleaf.dev/1.2.0-SNAPSHOT/components/field.md#disabled) with `disabled`.

**Example: Disabled item.**

```html
<sl:radio-group aria-label="Shipping">
  <sl:label><sl:radio-group-item name="shipping" value="pickup" checked/>Pick up in store</sl:label>
  <sl:label><sl:radio-group-item name="shipping" value="drone" disabled/>Drone (not available)</sl:label>
</sl:radio-group>
```

## Attributes

### sl:radio-group

Attributes of `<sl:radio-group>`: A group of `sl:radio-group-item`s, announced as a radio group. Name it with `aria-labelledby` (pointing at the question) or `aria-label`. The items share their `name`, or their `th:field`, which is what makes them one choice.

| Attribute | Values | Default | Description |
| --- | --- | --- | --- |
| `orientation` | `vertical`, `horizontal` | `vertical` | Stack the items, or put them on a row that wraps. |

Every prop can also be set from an expression: `th:orientation="${...}"`. Any other attribute passes through to the rendered element, and `class` is appended.

### sl:radio-group-item

Attributes of `<sl:radio-group-item>`: One radio button in `sl:radio-group`: the browser's own, styled. Give it a `value` and the group's `name`, or `th:field` with a `value` or `th:value`; Spring then numbers the ids (`plan1`, `plan2`), so put the item inside its label. In a `sl:field-set` with `th:field`, the items take its field. Every attribute that is not a prop passes through; `class` is appended to `radio-group-item`. With `th:field`, the item is marked `aria-invalid` when its field has errors.

`sl:radio-group-item` declares no props; every attribute passes through.
