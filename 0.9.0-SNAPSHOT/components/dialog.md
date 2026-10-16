# Dialog

> A modal window over the page, on the native dialog element.

Source: https://shadleaf.dev/0.9.0-SNAPSHOT/components/dialog/

**Example: Dialog.** A form in a dialog. The form wraps the dialog, so the submit button in its footer submits it.

```html
<sl:button variant="outline" commandfor="edit-profile" command="show-modal">Edit profile</sl:button>
<form method="post" action="/profile">
  <sl:dialog id="edit-profile">
    <sl:dialog-header>
      <sl:dialog-title>Edit profile</sl:dialog-title>
      <sl:dialog-description>
        Make changes to your profile here. Click save when you're done.
      </sl:dialog-description>
    </sl:dialog-header>
    <sl:field-group>
      <sl:field for="profile-name">
        <sl:field-label>Name</sl:field-label>
        <sl:input name="name" value="Pedro Duarte"/>
      </sl:field>
      <sl:field for="profile-username">
        <sl:field-label>Username</sl:field-label>
        <sl:input name="username" value="@peduarte"/>
      </sl:field>
    </sl:field-group>
    <sl:dialog-footer>
      <sl:button variant="outline" commandfor="edit-profile" command="close">Cancel</sl:button>
      <sl:button type="submit">Save changes</sl:button>
    </sl:dialog-footer>
  </sl:dialog>
</form>
```

## Built from parts

A dialog is `<sl:dialog>` plus the parts you need, as in shadcn/ui: `<sl:dialog-header>` (holding
`<sl:dialog-title>` and `<sl:dialog-description>`), your own content and `<sl:dialog-footer>`. shadcn's `Dialog`,
`DialogContent`, `DialogOverlay` and `DialogPortal` are all `<sl:dialog>`: it renders the browser's own `<dialog>`
element, which is the panel, and its `::backdrop` is the overlay.

The browser does the hard parts. A dialog opened as a modal is drawn above everything else, the page behind it is inert
(no clicks, no focus, hidden from screen readers), focus moves to the first control in it, Escape closes it, and closing
returns focus to the button that opened it. A click outside closes it too. The page behind it does not scroll.

The title names the dialog and the description describes it (`aria-labelledby` and `aria-describedby`, with ids
derived from the dialog's `id`: `edit-profile-title`). A dialog without a title needs an `aria-label`.

## Opening and closing

Every dialog has an `id`, and the buttons that open and close it point at it with HTML's
[invoker commands](https://developer.mozilla.org/en-US/docs/Web/API/Invoker_Commands_API): `commandfor` names the
dialog and `command` says what to do. No JavaScript of yours is involved.

```html
<sl:button commandfor="edit-profile" command="show-modal">Edit profile</sl:button>

<sl:dialog id="edit-profile">
  ...
  <sl:dialog-footer>
    <sl:button variant="outline" commandfor="edit-profile" command="close">Cancel</sl:button>
    <sl:button type="submit">Save changes</sl:button>
  </sl:dialog-footer>
</sl:dialog>
```

The close button in the corner does the same; `show-close-button="false"` leaves it out. `<sl:dialog-footer show-close-button>`
adds an outline "Close" button after the footer's own actions.

**Example: Close button in the footer.** No close button in the corner; the footer has one instead.

```html
<sl:button variant="outline" commandfor="share-link" command="show-modal">Share</sl:button>
<sl:dialog id="share-link" show-close-button="false">
  <sl:dialog-header>
    <sl:dialog-title>Share link</sl:dialog-title>
    <sl:dialog-description>Anyone who has this link will be able to view this.</sl:dialog-description>
  </sl:dialog-header>
  <sl:input aria-label="Link" value="https://ui.shadcn.com/docs/installation" readonly/>
  <sl:dialog-footer show-close-button/>
</sl:dialog>
```

A dialog per row of a table takes its id from an expression, and so do the buttons:

```html
<tr th:each="order : ${orders}">
  <td>
    <sl:button variant="outline" th:commandfor="|delete-${order.id}|" command="show-modal">Delete</sl:button>
    <sl:dialog th:id="|delete-${order.id}|">...</sl:dialog>
  </td>
</tr>
```

Invoker commands work in every current browser. For older ones (Safari before 26.2), and for a click outside where
the browser lacks `closedby`, the dialog's small Alpine component stands in, so both still work with Alpine on the page.
To react when a dialog closes, listen for the `close` event of the `<dialog>` element.

## A form in a dialog

Put the form around the dialog, as shadcn's example does, so the submit button in the footer submits it. After a full
page post, the page renders again and the dialog is closed. To show the form's errors in the dialog again, render it
with `open`, below.

## Rendered by the server: htmx

With htmx the dialog can come from the server when it is needed: a
button fetches it into an empty element on the page, and `open` shows it as a modal as soon as it is there.

```html
<!-- In the layout -->
<div id="modal-root"></div>

<!-- The button -->
<sl:button hx:get="@{/members/{id}/edit(id=${member.id})}" hx-target="#modal-root">Edit</sl:button>
```

The fragment the controller returns has the dialog, and inside it a form that swaps only itself. The submit button
sits in the footer, outside the form, and points at it with `form`:

```html
<sl:dialog th:fragment="edit-dialog" id="edit-member" open>
  <sl:dialog-header>
    <sl:dialog-title>Edit member</sl:dialog-title>
  </sl:dialog-header>
  <form th:fragment="edit-form" id="edit-member-form" th:object="${member}"
        hx:post="@{/members/{id}(id=${member.id})}" hx-target="this" hx-swap="outerHTML">
    <sl:form-errors autofocus/>
    <sl:field th:field="*{name}">
      <sl:field-label>Name</sl:field-label>
      <sl:input/>
      <sl:field-error/>
    </sl:field>
  </form>
  <sl:dialog-footer>
    <sl:button variant="outline" commandfor="edit-member" command="close">Cancel</sl:button>
    <sl:button type="submit" form="edit-member-form">Save</sl:button>
  </sl:dialog-footer>
</sl:dialog>
```

With errors, the controller returns the form alone: the dialog stays open and focus moves to the first field with an
error (see [form errors](https://shadleaf.dev/0.9.0-SNAPSHOT/components/form-errors.md)). When the save succeeds, it closes the dialog with an `HX-Trigger`
response header, swaps nothing where the form is, and updates the member's row out of band:

```java
@HxRequest
@PostMapping("/members/{id}")
public String update(@PathVariable long id, @Valid @ModelAttribute("member") EditMemberForm form,
    BindingResult bindingResult, HtmxResponse htmxResponse, Model model) {
  if (bindingResult.hasErrors()) {
    return "members :: edit-form";
  }
  model.addAttribute("member", memberService.update(id, form));
  htmxResponse.addTrigger("sl-dialog-close");
  htmxResponse.setReswap(HtmxReswap.none());
  return "members :: member-row-oob";  // the row, with hx-swap-oob="true"
}
```

htmx fires `sl-dialog-close` on the form that made the request; the event bubbles up to the dialog, which closes, and
focus returns to the button that fetched it. When the response then replaces that button, as the new row does here,
give the button an id (`th:id="|edit-member-${member.id}|"`): htmx moves focus to the element with the same id after a
swap. Without one, focus falls back to the top of the page. Use `HX-Trigger`, not `HX-Trigger-After-Swap` or `-After-Settle`: those
fire after the swap, when the element that made the request may be gone.

`open` needs Alpine (on by default: see [Alpine](https://shadleaf.dev/0.9.0-SNAPSHOT/guides/alpine.md)); the browser has no attribute that opens a
dialog as a modal. A dialog removed from the page while it is open, by any swap, takes its backdrop and the page's
inertness with it. Before htmx saves a page in its history cache (`hx-push-url`, `hx-boost`), open dialogs close, so
the back button never brings one back.

## Loaded into the dialog: a body

The dialog above appears only once the server has answered, and a slow answer leaves the user waiting with nothing to
show for the click. `<sl:dialog-body>` turns this around: the dialog is in the page, closed, and its body is what htmx
loads. A request that targets the body opens the dialog at once, and a spinner covers the body until the answer is
swapped in.

```html
<!-- The button, in each row -->
<sl:button hx:get="@{/members/{id}/edit(id=${member.id})}" hx-target="#edit-member-body">Edit</sl:button>

<!-- The dialog, once on the page -->
<sl:dialog id="edit-member">
  <sl:dialog-header>
    <sl:dialog-title>Edit member</sl:dialog-title>
  </sl:dialog-header>
  <sl:dialog-body/>
  <sl:dialog-footer>
    <sl:button variant="outline" commandfor="edit-member" command="close">Cancel</sl:button>
    <sl:button type="submit" form="edit-member-form">Save</sl:button>
  </sl:dialog-footer>
</sl:dialog>
```

The body's content has the id `<dialog id>-body`; the controller returns only what goes in it, here the form of the
previous section (`members :: edit-form`). Once it is in, the focus moves to its first field (or to one with
`autofocus`), as it would have had the form been there when the dialog opened. Saving works as before: with errors the
form swaps itself, and `HX-Trigger: sl-dialog-close` closes the dialog.

**Example: Loading.** While an htmx request into sl:dialog-body runs, a spinner covers it. Here the body keeps data-loading, which the script sets and removes.

```html
<sl:button variant="outline" commandfor="dialog-loading" command="show-modal">Edit member</sl:button>
<sl:dialog id="dialog-loading">
  <sl:dialog-header>
    <sl:dialog-title>Edit member</sl:dialog-title>
    <sl:dialog-description>Changes are saved when you click Save.</sl:dialog-description>
  </sl:dialog-header>
  <sl:dialog-body data-loading="true"/>
  <sl:dialog-footer show-close-button/>
</sl:dialog>
```

What the body does with a request that targets it (or an element inside it):

- **A request from outside opens the dialog**, as a modal, at once; focus returns to the button when it closes. A
  request from inside a closed dialog, such as content loaded with `hx-trigger="load"`, opens nothing.
- **The spinner shows after a moment** (200 ms), so a quick answer does not flicker. The body takes no clicks while
  the request runs, and a second request into it is not sent: clicking Save twice saves once. The content has
  `aria-busy="true"` meanwhile.
- **Closing the dialog aborts a request still running**, so a slow answer never fills a dialog the user has left.
- **An empty body is emptied again** once the dialog has closed, so the next member's dialog never shows the previous
  member while it loads. Content you put in the body in the page stays.
- **The header and the footer stay in view.** The body scrolls by itself when its content is taller than the screen.

### When a request fails

When the request that opened the dialog fails (an error status, no connection, a timeout), the dialog closes and the
error is shown as a [toast](https://shadleaf.dev/0.9.0-SNAPSHOT/components/toast.md). Without an `<sl:toaster>` on the page, or with `load-error="inline"`, the dialog
stays open and the error is shown in it, above the content. A failed request from inside the dialog, such as a save,
always shows the error in the dialog, so nothing the user typed is lost; the next request hides it.

The message is "Something went wrong. Please try again." (`sl.dialog.error` in your `messages.properties`). The
`error` slot replaces it for one dialog:

```html
<sl:dialog-body>
  <sl:slot name="error">The member could not be loaded.</sl:slot>
</sl:dialog-body>
```

Validation errors are no failure: return the form with its errors and a success status, as above.

## Width

The skin decides the dialog's width (at most `28rem` in vega). Give the dialog a class and set its width in your own
stylesheet, loaded after the library's: the library's rules sit in a cascade layer, so yours win.

```html
<sl:dialog id="edit-member" class="dialog-wide">...</sl:dialog>
```

```css
.dialog-wide {
  max-width: min(42rem, calc(100% - 2rem));
}
```

If you compile Tailwind yourself (the `embedded` bundle), a utility class does the same: `class="sm:max-w-2xl"`.

## Attributes

### sl:dialog

Attributes of `<sl:dialog>`: A modal dialog on the native `dialog` element: the page behind it is inert, Escape and a click outside close it, and focus returns to where it came from. Build it from `sl:dialog-header` (holding `sl:dialog-title` and `sl:dialog-description`), your content and `sl:dialog-footer`. A button opens it with `commandfor` set to the dialog's `id` and `command="show-modal"`, and closes it with `command="close"`; an htmx response closes it with the `sl-dialog-close` event.

| Attribute | Values | Default | Description |
| --- | --- | --- | --- |
| `id` | *string* | **required** | The dialog's id, which the buttons that open and close it point at with `commandfor`. The title and description ids derive from it. |
| `open` | *boolean* | — | Open the dialog as soon as it is on the page, for a dialog the server renders in answer to a request (an htmx swap, or a page shown after a redirect). Needs Alpine. |
| `show-close-button` | *boolean* | `true` | A close button (an x) in the corner. |

Every prop can also be set from an expression: `th:id="${...}"`. Any other attribute passes through to the rendered element, and `class` is appended.

### sl:dialog-header

Attributes of `<sl:dialog-header>`: The top of a `sl:dialog`: a `sl:dialog-title` and an optional `sl:dialog-description` below it.

`sl:dialog-header` declares no props; every attribute passes through.

### sl:dialog-title

Attributes of `<sl:dialog-title>`: The title of a `sl:dialog`, inside `sl:dialog-header`: a heading that names the dialog (`aria-labelledby`). Its id derives from the dialog's (`edit-profile-title`).

| Attribute | Values | Default | Description |
| --- | --- | --- | --- |
| `level` | `1`, `2`, `3`, `4`, `5`, `6` | `2` | Heading level (`role="heading"` and `aria-level`). A dialog starts an outline of its own, so the default, 2, rarely needs changing. |

Every prop can also be set from an expression: `th:level="${...}"`. Any other attribute passes through to the rendered element, and `class` is appended.

### sl:dialog-description

Attributes of `<sl:dialog-description>`: Supporting text below a `sl:dialog-title`, which describes the dialog (`aria-describedby`). Its id derives from the dialog's (`edit-profile-description`).

`sl:dialog-description` declares no props; every attribute passes through.

### sl:dialog-body

Attributes of `<sl:dialog-body>`: The content of a `sl:dialog` between its header and footer, which scrolls by itself so the header and footer stay in view. Its content has the id `&lt;id&gt;-body` (`edit-body`), the target for htmx: a request from outside that targets it opens the dialog at once and shows a spinner until the answer is swapped in; closing aborts a request still running. Left empty, it is emptied again when the dialog closes, so it never shows the previous content. When a request fails, the error shows in the dialog, or, for the request that opened it, as a toast (see `load-error`). Needs Alpine.

| Attribute | Values | Default | Description |
| --- | --- | --- | --- |
| `load-error` | `toast`, `inline` | `toast` | What happens when the request that opened the dialog fails: `toast` closes it and shows the error as a toast (shown in the dialog when the page has no `sl:toaster`); `inline` keeps it open with the error in it. A failed request from inside (a form) always shows the error in the dialog, so nothing typed is lost. |

Every prop can also be set from an expression: `th:load-error="${...}"`. Any other attribute passes through to the rendered element, and `class` is appended.

### sl:dialog-footer

Attributes of `<sl:dialog-footer>`: The bottom of a `sl:dialog`, for its actions: side by side at the end on a wide screen, stacked with the first one at the bottom on a narrow one.

| Attribute | Values | Default | Description |
| --- | --- | --- | --- |
| `show-close-button` | *boolean* | — | An outline "Close" button after the other actions. |

Every prop can also be set from an expression: `th:show-close-button="${...}"`. Any other attribute passes through to the rendered element, and `class` is appended.

## JavaScript

A small Alpine component, `slDialog`, adds what the browser's `<dialog>` cannot do: `<sl:dialog>` renders as
`<dialog class="dialog" x-data="slDialog">`.

- **Most of it works without Alpine.** Opening and closing with `commandfor`, Escape, a click outside and focus
  returning to the button are the browser's. Alpine is needed for `open`, for the `sl-dialog-close` event, for a
  body that htmx loads, and for invoker commands and a click outside in browsers that lack them. Alpine is included by default; see
  [Alpine](https://shadleaf.dev/0.9.0-SNAPSHOT/guides/alpine.md).
- **It needs no Alpine plugin.** If you load Alpine yourself (`shadleaf.assets.alpine=external`), plain Alpine is
  enough.
- **Keep your own `x-data` off `<sl:dialog>`.** An element has one `x-data`, and attributes on `<sl:dialog>` end up on
  the `<dialog>`, so yours would replace `slDialog`. Put it on an element inside the dialog.

| Event | On | Effect |
| --- | --- | --- |
| `sl-dialog-close` | the dialog, or anything inside it (it bubbles) | Closes the dialog. |
| `close` | the `<dialog>` (the browser's own) | Fired when it closes, however that happened. |
