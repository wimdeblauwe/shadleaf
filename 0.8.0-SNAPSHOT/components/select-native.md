# Select (Native)

> The browser's own select, styled, bound to a form field with th:field.

Source: https://shadleaf.dev/0.8.0-SNAPSHOT/components/select-native/

**Example: Native select.**

```html
<sl:label for="status">Status</sl:label>
<sl:native-select th:field="*{status}">
  <option value="">Select status</option>
  <option th:each="status : ${statuses}" th:value="${status}" th:text="#{|status.${status}|}">Todo</option>
</sl:native-select>
```

## Native select or select?

Both are a real `<select>` underneath, bind with `th:field` and submit with the form. This one styles only the closed
control and leaves the list to the operating system: on a phone that is the system's own picker, which many people
find easier to use. Use it for long lists (countries, years), in dense forms, and for `multiple`.
[`<sl:select>`](https://shadleaf.dev/0.8.0-SNAPSHOT/components/select.md) draws its list in the page's style, like shadcn/ui's Select, with a check mark on the
chosen item and room for an icon; use it where the look matters and the list is short enough to scan.

## Options

Write plain `<option>` and `<optgroup>` elements as the content, with `th:each` for options from the model. With
`th:field` on the select, Spring marks the option that matches the field's value as selected.

**Example: Groups.**

```html
<sl:label for="select-department">Department</sl:label>
<sl:native-select id="select-department" name="department">
  <optgroup label="Engineering">
    <option value="frontend">Frontend</option>
    <option value="backend">Backend</option>
  </optgroup>
  <optgroup label="Sales">
    <option value="sales-rep">Sales representative</option>
    <option value="account-manager">Account manager</option>
  </optgroup>
</sl:native-select>
```

## Sizes

**Example: Sizes.**

```html
<sl:native-select size="sm" name="priority" aria-label="Priority (small)">
  <option>Low</option>
  <option>High</option>
</sl:native-select>
<sl:native-select name="priority" aria-label="Priority">
  <option>Low</option>
  <option>High</option>
</sl:native-select>
```

`size` picks the height, as in shadcn/ui. It replaces HTML's own `size` attribute, which turns a select into a list box;
`multiple` still works, and hides the chevron.

## Width

The select sits in a wrapper that also holds the chevron. `class` goes to that wrapper, so `class="w-full"` widens
both; every other attribute goes to the `<select>`.

## Invalid

As with the [input](https://shadleaf.dev/0.8.0-SNAPSHOT/components/input.md), a field with errors marks the select `aria-invalid="true"`.

**Example: Invalid.** With th:field, a field that has errors marks the select aria-invalid by itself.

```html
<sl:native-select th:field="*{status}" aria-label="Status">…</sl:native-select>
```

## Disabled

The select fades with its chevron; the lyra skin also gives it a grey background, as it does a disabled input. HTML has
no read-only select: `readonly` does nothing on one. To show a choice that cannot be changed, use a read-only
[input](https://shadleaf.dev/0.8.0-SNAPSHOT/components/input.md#read-only) with the option's text, and a hidden input if the value has to be submitted.

**Example: Disabled.**

```html
<sl:native-select name="status" aria-label="Status" disabled>
  <option>Todo</option>
</sl:native-select>
```

## Attributes

Attributes of `<sl:native-select>`: The browser's own `select`, styled, with its `option` and `optgroup` elements as content. `th:field` marks the bound option. Every attribute that is not a prop passes through to the `select`, except `class`, which goes to the wrapper around it (for its width). With `th:field`, the select is marked `aria-invalid` when its field has errors. In a `sl:field`, it takes the field's `th:field`, id and `aria-describedby`.

| Attribute | Values | Default | Description |
| --- | --- | --- | --- |
| `size` | `sm`, `default` | `default` | Height of the select. It replaces HTML's `size` attribute, which turns a select into a list box. |

Every prop can also be set from an expression: `th:size="${...}"`. Any other attribute passes through to the rendered element, and `class` is appended.
