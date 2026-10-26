# Field

> A label, a control, a description and the binding's error messages, wired together from one th:field.

Source: https://shadleaf.dev/1.1.0-SNAPSHOT/components/field/

**Example: Field.**

```html
<sl:field th:field="*{username}">
  <sl:field-label>Username</sl:field-label>
  <sl:input autocomplete="username"/>
  <sl:field-description>Letters, digits and dashes.</sl:field-description>
  <sl:field-error/>
</sl:field>
```

## One th:field for the whole field

Put `th:field` on `<sl:field>`, not on the control. The field reads the binding and hands it to its parts, so you
write the field once and get:

- the control bound to the field: `name`, value and `id`, as `th:field` on the control would give;
- `<sl:field-label>` with a `for` that points at the control;
- `<sl:field-description>` with an `id`, and the control's `aria-describedby` pointing at it;
- `<sl:field-error>` showing the binding's messages, and the control described by them while there are any;
- `aria-invalid` on the control and `data-invalid` on the field when the binding has errors.

```html
<form th:object="${signup}" th:action="@{/signup}" method="post" novalidate>
  <sl:field th:field="*{email}">
    <sl:field-label>Email</sl:field-label>
    <sl:input type="email"/>
    <sl:field-description>We never share it.</sl:field-description>
    <sl:field-error/>
  </sl:field>
</form>
```

With an error on `email`, that renders:

```html
<div class="field" data-invalid="true">
  <label class="label field-label" for="email">Email</label>
  <input class="input" type="email" aria-invalid="true" id="email"
         aria-describedby="email-description email-error" name="email" value="wim@">
  <p class="field-description" id="email-description">We never share it.</p>
  <div class="field-error" id="email-error">must be a well-formed email address</div>
</div>
```

The parts go in any order. A field holds one control: a second one keeps its own `th:field` and id.

## Errors

`<sl:field-error>` renders nothing while the field has no errors. One message is shown as text, several as a list,
each message once. Content of your own replaces the messages and always shows, so wrap it in a `th:if` when it should
only show with errors.

**Example: With errors.** A second submit renders the form again. The field shows the binding's messages and is marked data-invalid; the input is aria-invalid and described by the message.

```html
<sl:field th:field="*{workEmail}">
  <sl:field-label>Work email</sl:field-label>
  <sl:input type="email"/>
  <sl:field-description>Your company address.</sl:field-description>
  <sl:field-error/>
</sl:field>
```

**Example: Several errors.**

```html
<sl:field th:field="*{newPassword}">
  <sl:field-label>Password</sl:field-label>
  <sl:input type="password" autocomplete="new-password"/>
  <sl:field-error/>
</sl:field>
```

The error has no `role="alert"`. After a submit the whole page loads again, and a screen reader would read out every
error at once. The control is `aria-invalid` and described by its error instead, so the message is read when the user
reaches the control. When htmx swaps the whole form in, `<sl:form-errors autofocus/>` moves focus to the first
control with an error, which reads it out; see [Forms with htmx](https://shadleaf.dev/1.1.0-SNAPSHOT/guides/htmx.md). If you swap a single field in
while focus stays elsewhere, for example to validate it as the user leaves it, add `role="alert"` to its error
yourself.

Errors that belong to the whole form rather than one field, such as `reject(...)` in the controller, go in a
[`<sl:form-errors/>`](https://shadleaf.dev/1.1.0-SNAPSHOT/components/form-errors.md) above the fields.

## Ids

The control's id is the field's name, the id `th:field` gives a text input: `email`, or `address.street` for a nested
property. The description and error ids add `-description` and `-error`. Set `for` on the field to use another id:

```html
<sl:field th:field="*{email}" for="work-email">
```

An `aria-describedby` you put on the control is kept, and the field's ids are added after it.

For a checkbox or switch, Spring numbers the id (`terms1`), so that several checkboxes bound to one list field get
different ids. In a field, the checkbox gets the field's name instead, and the label points at it.

## Other controls

A textarea and a native select work as the input does:

**Example: Textarea.**

```html
<sl:field th:field="*{bio}">
  <sl:field-label>Bio</sl:field-label>
  <sl:textarea rows="3"/>
  <sl:field-description>A few words for your profile page.</sl:field-description>
</sl:field>
```

**Example: Native select.**

```html
<sl:field th:field="*{country}">
  <sl:field-label>Country</sl:field-label>
  <sl:native-select>
    <option value="">Select a country</option>
    <option value="be">Belgium</option>
    <option value="nl">Netherlands</option>
  </sl:native-select>
  <sl:field-error/>
</sl:field>
```

## Checkbox and switch

`orientation="horizontal"` puts the control first and the label beside it. `<sl:field-content>` stacks the label with
a description or an error.

**Example: Switch.** A horizontal field puts the control first. field-content stacks the label with its description.

```html
<sl:field th:field="*{productUpdates}" orientation="horizontal">
  <sl:switch/>
  <sl:field-content>
    <sl:field-label>Product updates</sl:field-label>
    <sl:field-description>One email a month, no more.</sl:field-description>
  </sl:field-content>
</sl:field>
```

**Example: Checkbox.** In a field, the checkbox gets the field's name as its id, not Spring's numbered one, so the label points at it.

```html
<sl:field th:field="*{acceptTerms}" orientation="horizontal">
  <sl:checkbox/>
  <sl:field-content>
    <sl:field-label>I accept the terms and conditions</sl:field-label>
    <sl:field-error/>
  </sl:field-content>
</sl:field>
```

## Radio buttons and checkbox groups

A set of options that answers one question is a `<sl:field-set>`, with a `<sl:field-legend>` as its question.
`th:field` goes on the field set, and the options inside share it. A description and an error directly inside the field
set describe the whole group: the `fieldset` gets the `aria-describedby`, while each radio button is still marked
`aria-invalid` from the field.

**Example: Radio group.** The field-set holds the th:field, which its radio buttons share. The description and the error describe the fieldset.

```html
<sl:field-set th:field="*{billingPlan}">
  <sl:field-legend variant="label">Plan</sl:field-legend>
  <sl:field-description>You can change it at any time.</sl:field-description>
  <sl:radio-group>
    <sl:label><sl:radio-group-item value="monthly"/>Monthly</sl:label>
    <sl:label><sl:radio-group-item value="yearly"/>Yearly</sl:label>
  </sl:radio-group>
  <sl:field-error/>
</sl:field-set>
```

For checkboxes, make each option a horizontal field without a `th:field` of its own. It takes the field set's, and the
next of Spring's numbered ids (`toppings1`, `toppings2`), which its label points at.

**Example: Checkbox group.** Each option is a field without th:field of its own. It takes the field-set's, with the next of Spring's numbered ids.

```html
<sl:field-set th:field="*{toppings}">
  <sl:field-legend variant="label">Toppings</sl:field-legend>
  <sl:field orientation="horizontal">
    <sl:checkbox value="cheese"/>
    <sl:field-label>Cheese</sl:field-label>
  </sl:field>
  <sl:field orientation="horizontal">
    <sl:checkbox value="olives"/>
    <sl:field-label>Olives</sl:field-label>
  </sl:field>
  <sl:field orientation="horizontal">
    <sl:checkbox value="basil"/>
    <sl:field-label>Basil</sl:field-label>
  </sl:field>
</sl:field-set>
```

`variant="label"` on the legend gives it the size of a field label; the default is a section heading.

## Layout

`<sl:field-group>` stacks fields with even spacing. A field set without `th:field` groups related fields under a
heading.

**Example: Field set and field group.**

```html
<sl:field-set>
  <sl:field-legend>Shipping address</sl:field-legend>
  <sl:field-description>Where we deliver your order.</sl:field-description>
  <sl:field-group>
    <sl:field th:field="*{street}">
      <sl:field-label>Street</sl:field-label>
      <sl:input autocomplete="street-address"/>
    </sl:field>
    <sl:field th:field="*{city}">
      <sl:field-label>City</sl:field-label>
      <sl:input autocomplete="address-level2"/>
    </sl:field>
  </sl:field-group>
</sl:field-set>
```

`orientation="responsive"` puts the label and the control side by side when the enclosing field group is at least
28rem wide, and stacks them when it is narrower.

**Example: Responsive.** Side by side in a wide field-group, stacked in a narrow one.

```html
<sl:field-group>
  <sl:field th:field="*{displayName}" orientation="responsive">
    <sl:field-content>
      <sl:field-label>Display name</sl:field-label>
      <sl:field-description>Shown next to your comments.</sl:field-description>
    </sl:field-content>
    <sl:input/>
  </sl:field>
</sl:field-group>
```

## Disabled and read-only

A field notices a disabled control inside it and fades its label: there is no `data-disabled` to set, as there is in
shadcn. The description keeps its colour, since it often says why the control is disabled.

**Example: Disabled.** The label fades with a disabled control, found by the field itself; the description stays legible, as it often says why.

```html
<sl:field-group>
  <sl:field th:field="*{companyName}">
    <sl:field-label>Company name</sl:field-label>
    <sl:input disabled/>
    <sl:field-description>Only an administrator can change it.</sl:field-description>
  </sl:field>
  <sl:field th:field="*{twoFactor}" orientation="horizontal">
    <sl:switch disabled/>
    <sl:field-content>
      <sl:field-label>Two-factor authentication</sl:field-label>
      <sl:field-description>Required by your organisation.</sl:field-description>
    </sl:field-content>
  </sl:field>
</sl:field-group>
```

`disabled` on a `<sl:field-set>` disables every control in it, as it does on any `fieldset`. The legend and the
options' labels fade with them.

**Example: Disabled field set.** disabled on the field set disables every control in it, as on a fieldset; the legend and labels fade with them.

```html
<sl:field-set th:field="*{billingCycle}" disabled>
  <sl:field-legend variant="label">Billing cycle</sl:field-legend>
  <sl:field-description>Fixed until your contract ends.</sl:field-description>
  <sl:radio-group>
    <sl:label><sl:radio-group-item value="monthly"/>Monthly</sl:label>
    <sl:label><sl:radio-group-item value="yearly"/>Yearly</sl:label>
  </sl:radio-group>
</sl:field-set>
```

A read-only control changes nothing in the field: its label stays as it is. See the [input](https://shadleaf.dev/1.1.0-SNAPSHOT/components/input.md#read-only) for
how it looks.

**Example: Read-only.** A read-only input keeps its label as it is. It still takes focus, and its value is submitted.

```html
<sl:field th:field="*{accountNumber}">
  <sl:field-label>Account number</sl:field-label>
  <sl:input readonly/>
  <sl:field-description>Quote it when you contact us.</sl:field-description>
</sl:field>
```

A disabled control is not submitted, so an error on it usually comes from something the user cannot change in this
form. It still shows: the control keeps its destructive border, faded, next to the field's error. With
`<sl:form-errors autofocus/>`, a control with a `disabled` attribute is passed over and the next control with errors
takes the focus; one disabled by `th:disabled` or by a disabled field set is not known to be disabled when the page is
rendered, and then nothing takes the focus.

## Without th:field

A field without `th:field` is only layout. Set `for` to give the control its id, the label its `for`, and the
description and error their ids. Without a binding, `data-invalid` and `aria-invalid` are yours to set.

**Example: Without th:field.** for gives the control its id, and the description and error their ids.

```html
<sl:field for="field-search">
  <sl:field-label>Search orders</sl:field-label>
  <sl:input type="search" name="q"/>
  <sl:field-description>An order number or a customer name.</sl:field-description>
</sl:field>
```

## How the parts find the field

`<sl:field>` renders its content with a local variable, `slField`, that holds the `th:field`, the ids and the
messages. Every part and control inside it reads that variable, which is why nothing has to be repeated. To know
whether it has a description or an error, the field looks at its content as written: a part added with `th:replace` is
not seen, and a part with a `th:if` that is false still counts, so the control would point at a description that is
not there.

## Attributes

### sl:field

Attributes of `<sl:field>`: A form field: a `sl:field-label`, one control, and optionally a `sl:field-description` and a `sl:field-error`, in any order. Put `th:field` on the field, not on the control: the control takes it, gets the `id` the label points at (the field's name, `email`, also for a checkbox or switch), and an `aria-describedby` listing the description and, when the binding has errors, the error. The error shows the binding's messages. A field with errors gets `data-invalid`. Without `th:field`, set `for` to the control's id.

| Attribute | Values | Default | Description |
| --- | --- | --- | --- |
| `orientation` | `vertical`, `horizontal`, `responsive` | `vertical` | Stack the label above the control, or put them side by side (a checkbox or switch, then its label). `responsive` stacks them in a narrow `sl:field-group` and puts them side by side in a wide one. |
| `for` | *string* | — | The id of the control: for a field without `th:field`, or to use another id than the field's name. The control gets it when it has no id of its own, and the description and error ids derive from it. |

Every prop can also be set from an expression: `th:orientation="${...}"`. Any other attribute passes through to the rendered element, and `class` is appended.

### sl:field-label

Attributes of `<sl:field-label>`: The label of a `sl:field`. Its `for` points at the field's control; a `for` of your own wins.

`sl:field-label` declares no props; every attribute passes through.

### sl:field-description

Attributes of `<sl:field-description>`: Help text for a `sl:field` or `sl:field-set`. It gets the id the control's (or fieldset's) `aria-describedby` points at.

`sl:field-description` declares no props; every attribute passes through.

### sl:field-error

Attributes of `<sl:field-error>`: The error messages of a `sl:field` or `sl:field-set`, read from its `th:field`'s binding: one message as text, several as a list. It renders nothing while there are none. Content of its own replaces the messages, and always shows. It gets the id the control's (or fieldset's) `aria-describedby` points at while it shows messages.

`sl:field-error` declares no props; every attribute passes through.

### sl:field-content

Attributes of `<sl:field-content>`: Stacks a `sl:field-label` with its `sl:field-description` or `sl:field-error`, beside the control of a horizontal `sl:field`.

`sl:field-content` declares no props; every attribute passes through.

### sl:field-set

Attributes of `<sl:field-set>`: A group of related fields or options under a `sl:field-legend`, as a `fieldset`. With `th:field`, the options inside share it: radio buttons in a `sl:radio-group`, or checkboxes each in a `sl:field` of their own (which then need no `th:field` and get Spring's numbered ids, `toppings1`). A `sl:field-description` and `sl:field-error` directly inside describe the fieldset, and it gets `data-invalid` when the binding has errors.

`sl:field-set` declares no props; every attribute passes through.

### sl:field-legend

Attributes of `<sl:field-legend>`: The caption of a `sl:field-set`, as its `legend`. Put it first in the fieldset.

| Attribute | Values | Default | Description |
| --- | --- | --- | --- |
| `variant` | `legend`, `label` | `legend` | A section heading, or the size of a field label (for a set of options that answers one question). |

Every prop can also be set from an expression: `th:variant="${...}"`. Any other attribute passes through to the rendered element, and `class` is appended.

### sl:field-group

Attributes of `<sl:field-group>`: Stacks `sl:field`s and `sl:field-set`s with even spacing. Fields with `orientation="responsive"` inside it go side by side when it is wide enough.

`sl:field-group` declares no props; every attribute passes through.
