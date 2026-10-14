# Form errors

> The errors that belong to the whole form rather than one field, or all of the form's errors, read from the binding and shown above the fields.

Source: https://wimdeblauwe.github.io/shadleaf/current/components/form-errors/

**Example: Form errors.** The binding has a global error, from a class-level constraint or reject(...) in the controller. It shows above the fields (here first in the field group, which spaces it like a field); the field errors stay with their fields.

```html
<sl:field-group>
  <sl:form-errors/>
  <sl:field th:field="*{signupPassword}">
    <sl:field-label>Password</sl:field-label>
    <sl:input type="password" autocomplete="new-password"/>
  </sl:field>
  <sl:field th:field="*{signupRepeat}">
    <sl:field-label>Repeat the password</sl:field-label>
    <sl:input type="password" autocomplete="new-password"/>
  </sl:field>
</sl:field-group>
```

## Errors that belong to the form

Some errors are about no single field: two passwords that do not match, a date range that ends before it starts, or a
service that refused the request. Spring keeps them in the `BindingResult` as global errors. `<sl:form-errors/>`
shows them as a destructive [alert](https://wimdeblauwe.github.io/shadleaf/current/components/alert.md), and renders nothing while there are none:

```html
<form th:object="${signup}" th:action="@{/signup}" method="post" novalidate>
  <sl:field-group>
    <sl:form-errors/>
    <sl:field th:field="*{password}">...</sl:field>
    <sl:field th:field="*{repeatPassword}">...</sl:field>
  </sl:field-group>
</form>
```

With one global error, that renders:

```html
<div class="form-errors alert" data-variant="destructive" tabindex="-1" id="signup-errors">
  <svg class="sl-icon" aria-hidden="true" ...>...</svg>
  <div class="alert-title">There is a problem</div>
  <div class="alert-description">The passwords do not match.</div>
</div>
```

As the first thing in a `<sl:field-group>`, it gets the same space as a field. One message is shown as text, several
as a list, each message once. By default field errors are not repeated: they stay with
their [fields](https://wimdeblauwe.github.io/shadleaf/current/components/field.md). To list them too, see [All errors](#all-errors).

**Example: Several errors.**

```html
<sl:form-errors id="order-errors"/>
```

It must be inside the element with `th:object`: that is the form object whose errors it reads. Outside one, rendering
fails with a message that says so.

## Where global errors come from

Anything that calls `reject(...)` on the `BindingResult` instead of `rejectValue(field, ...)`:

- **The controller**, for a rule that needs a service or a second look at the form:

  ```java
  @PostMapping("/signup")
  String signup(@Valid @ModelAttribute("signup") SignupForm signup, BindingResult bindingResult) {
    if (accounts.isBlocked(signup.getEmail())) {
      bindingResult.reject("signup.blocked", "We cannot create an account for this address.");
    }
    if (bindingResult.hasErrors()) {
      return "signup";
    }
    ...
  }
  ```

  The code (`signup.blocked`) is looked up in your `messages.properties` first; the second argument is the default.

- **A class-level constraint**: a Bean Validation annotation on the form class, such as a `@PasswordsMatch` of your
  own. Its violation has no property path, so Spring adds it as a global error.

A failure the controller catches and does not want in the binding can go in as content of its own (below).

## Title

The title defaults to the message `sl.form-errors.title`, "There is a problem". Define that key in your own
`messages.properties` to reword or translate it for every form, or set `title` (or `th:title="#{...}"`) on one.
`level` makes the title a heading that screen reader users can jump to.

**Example: A title of its own.** title replaces the default title (the message sl.form-errors.title); level makes it a heading.

```html
<sl:form-errors id="transfer-errors" title="The transfer was not made" level="2"/>
```

## Content of your own

Content of your own replaces the messages and always shows, so wrap it in a `th:if` when it should only show
sometimes.

**Example: Content of its own.** Content of its own replaces the messages and always shows, for an error the controller does not put in the binding.

```html
<sl:form-errors id="payment-errors">
  <p>The payment service did not answer. Your card was not charged.</p>
  <p>Try again in a minute.</p>
</sl:form-errors>
```

## Announcement and focus

`<sl:form-errors>` is not announced, and takes focus only when you ask for it with `autofocus`.

- **No `role="alert"`.** After a submit the whole page loads again. A live region that is there when the page loads
  is read out by some screen readers and ignored by others, and when it is read, it talks over the page itself. The
  [field errors](https://wimdeblauwe.github.io/shadleaf/current/components/field.md#errors) leave out `role="alert"` for the same reason. When focus moves to the problem, as
  below, the screen reader reads it there, and a live region would only say it a second time.
- **No `autofocus` by default.** Moving focus on load skips the reader past the page's heading, so it fits a form
  that is the page's only task, or one that htmx swaps in, where the submit button that had the focus is gone. It is
  your call per form.

With `autofocus`, focus goes to the problem when the form comes back with errors:

```html
<form th:object="${signup}" th:action="@{/signup}" method="post" novalidate>
  <sl:form-errors autofocus/>
  ...
</form>
```

| The form comes back with | Focus goes to |
| --- | --- |
| a global error (or, with `show="all"`, any error) | the summary, which is read out; <kbd>Tab</kbd> continues into the form |
| field errors only | the first control with an error, in the order of the form: its label, its invalid state and its error are read out through `aria-describedby` |
| no errors | nowhere: the first, empty form keeps the focus where it was |

Content of your own always shows, so with it the summary takes the focus. The summary decides before the controls
below it render, so keep it above the fields, where it belongs anyway. It works on any control with a `th:field` in
the form, inside a field or not. A control with a `disabled` attribute cannot take focus, so it is passed over (see
[disabled and read-only fields](https://wimdeblauwe.github.io/shadleaf/current/components/field.md#disabled-and-read-only)); a read-only one takes it.

For a form that reloads the page, also put the outcome in the page title, such as `Error: Sign up`: it is the first
thing a screen reader reads after a page load.

htmx honours `autofocus` in the content it swaps in, after its own attempt to keep focus on the element that had it.
The [htmx guide](https://wimdeblauwe.github.io/shadleaf/current/guides/htmx.md) shows a form that posts with htmx and falls back to a plain POST.

## Id

The summary's id is the form object's name followed by `-errors`: `signup-errors` for `th:object="${signup}"`. Set
`id` to use another one, for instance when two forms on a page bind objects of the same name. The id is there for
a link (`<a href="#signup-errors">`) or a script. The form's `aria-describedby` does not point at it:
screen readers read a form's description, if at all, when the user enters the form, which is not where anyone is
after a submit.

## All errors

`show="all"` lists the field errors too, after the global ones, in the order they were rejected. Each field still
shows its own error next to its control. A message is listed once per field, so two fields that both say "must not be
blank" both appear.

**Example: All errors.** show="all" lists the field errors too, after the global ones. Each field still shows its own error.

```html
<sl:field-group>
  <sl:form-errors id="account-errors" show="all"/>
  <sl:field th:field="*{accountName}">
    <sl:field-label>Name</sl:field-label>
    <sl:input autocomplete="name"/>
    <sl:field-error/>
  </sl:field>
  <sl:field th:field="*{accountEmail}">
    <sl:field-label>Email</sl:field-label>
    <sl:input type="email" autocomplete="email"/>
    <sl:field-error/>
  </sl:field>
</sl:field-group>
```

The messages are listed as the binding has them, without the field's label, so write them to make sense on their own:
"Enter your name" rather than "must not be blank". Spring looks up a message code per field before the constraint's
default, so your `messages.properties` can do that without touching the form class:

```properties
NotBlank.contactForm.name=Enter your name.
Email.contactForm.email=Enter an email address like you@example.com.
```

A title with the number of errors takes `#fields` from your own `messages.properties`; the component's attributes are
evaluated inside your `th:object`:

```html
<sl:form-errors show="all" th:title="#{errors.summary.title(${#fields.errors().size()})}"/>
```

```properties
errors.summary.title=There were {0} errors with your submission
```

With every error in it, the summary is where a keyboard or screen reader user can take stock after a submit, so with
`autofocus` it takes the focus whenever the form has errors.

The messages are not links to their controls (GOV.UK's summary has those), at least for now:

- The links need the controls' ids, which the fields decide as they render, below the summary. Radio buttons and
  checkbox groups have numbered ids (`replyBy1`), so the field's name is not always an id to link to.
- Each field error is already next to its control, and read out with it through `aria-describedby`.
- Without `show="all"`, `autofocus` takes the user straight to the first field with an error, which is what a link
  from the summary would do.

## Attributes

Attributes of `<sl:form-errors>`: The errors of the form object the enclosing `th:object` selects, shown as a destructive alert: by default the errors that belong to the whole form rather than one field (global errors, from `bindingResult.reject(...)` or a class-level constraint); with `show="all"` the field errors too. One message as text, several as a list. It renders nothing while there are none. Content of its own replaces the messages, and always shows. Not announced, and not focused unless it has `autofocus`; it has an id (`signup-errors` for `th:object="${signup}"`) and `tabindex="-1"`, so a link or a script can move focus to it too.

| Attribute | Values | Default | Description |
| --- | --- | --- | --- |
| `show` | `global`, `all` | `global` | Which errors to list: the global errors only, or all of them, the global ones first and then every field's (each field keeps its own error too). Field messages are listed as they are, without the field's label, so write them to make sense on their own. |
| `title` | *string* | — | The title above the messages. Defaults to the message `sl.form-errors.title` ("There is a problem"); use `th:title` for a message of your own. |
| `level` | `1`, `2`, `3`, `4`, `5`, `6` | — | Heading level of the title. With it the title is a heading (`role="heading"` and `aria-level`) that screen reader users can jump to; without it, plain text. |
| `autofocus` | *boolean* | — | Move focus to the problem when the form comes back with errors: to the summary when it shows, otherwise to the first control with errors below it (in a field or with a `th:field` of its own). Nothing takes focus while the form has no errors. htmx honours it in swapped content too. |

Every prop can also be set from an expression: `th:show="${...}"`. Any other attribute passes through to the rendered element, and `class` is appended.
