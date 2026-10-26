# Alert Dialog

> A modal dialog that interrupts the user with something important and waits for an answer.

Source: https://shadleaf.dev/1.1.0-SNAPSHOT/components/alert-dialog/

**Example: Alert dialog.** A question the user has to answer. Cancel has the focus when it opens.

```html
<sl:button variant="outline" commandfor="delete-account" command="show-modal">Delete account</sl:button>
<sl:alert-dialog id="delete-account">
  <sl:alert-dialog-header>
    <sl:alert-dialog-title>Are you absolutely sure?</sl:alert-dialog-title>
    <sl:alert-dialog-description>
      This action cannot be undone. This will permanently delete your account and remove your data from our
      servers.
    </sl:alert-dialog-description>
  </sl:alert-dialog-header>
  <sl:alert-dialog-footer>
    <sl:alert-dialog-cancel/>
    <sl:button>Continue</sl:button>
  </sl:alert-dialog-footer>
</sl:alert-dialog>
```

## Built from parts

An alert dialog asks a question the user has to answer before going on, such as "Delete this account?". It is
`<sl:alert-dialog>` plus its parts, as in shadcn/ui: `<sl:alert-dialog-header>` (holding an optional
`<sl:alert-dialog-media>`, `<sl:alert-dialog-title>` and `<sl:alert-dialog-description>`) and
`<sl:alert-dialog-footer>` with `<sl:alert-dialog-cancel>` and the action, a plain `<sl:button>`.

```html
<sl:button variant="outline" commandfor="delete-account" command="show-modal">Delete account</sl:button>

<sl:alert-dialog id="delete-account">
  <sl:alert-dialog-header>
    <sl:alert-dialog-title>Are you absolutely sure?</sl:alert-dialog-title>
    <sl:alert-dialog-description>This action cannot be undone.</sl:alert-dialog-description>
  </sl:alert-dialog-header>
  <sl:alert-dialog-footer>
    <sl:alert-dialog-cancel/>
    <sl:button>Continue</sl:button>
  </sl:alert-dialog-footer>
</sl:alert-dialog>
```

It works like a [dialog](https://shadleaf.dev/1.1.0-SNAPSHOT/components/dialog.md), with the differences an alert dialog needs:

- It has `role="alertdialog"`, so a screen reader announces it as an alert and reads the description with the title.
- A click outside does not close it: the user has to choose. Escape still does, as in Radix, and counts as cancel.
- It has no close button in the corner.
- `<sl:alert-dialog-cancel>` takes the focus when it opens, so pressing Enter straight away never confirms by accident.
  It closes the dialog (`commandfor` and `command="close"` are set for you) and says "Cancel" unless you give it text
  of its own (the default comes from the message `sl.alert-dialog.cancel`, see [Translating](https://shadleaf.dev/1.1.0-SNAPSHOT/guides/translating.md)). It takes the button's `variant` (default
  `outline`) and `size`, and passes any other attribute on to the button.

The action has no part of its own: what it does depends on the page. A `type="submit"` button in a form, a button
with `hx-delete`, or one with `commandfor` and `command="close"` when closing is all it has to do.

## Small, and with media

`size="sm"` makes it narrower, centres the header and puts the two buttons side by side in equal columns.
`<sl:alert-dialog-media>` shows an icon above the title.

**Example: Small, with media.** size="sm" centres the header and puts the two buttons side by side; the media shows an icon above the title.

```html
<sl:button variant="outline" commandfor="allow-accessory" command="show-modal">Connect accessory</sl:button>
<sl:alert-dialog id="allow-accessory" size="sm">
  <sl:alert-dialog-header>
    <sl:alert-dialog-media><sl:icon name="bluetooth"/></sl:alert-dialog-media>
    <sl:alert-dialog-title>Allow accessory to connect?</sl:alert-dialog-title>
    <sl:alert-dialog-description>
      Do you want to allow the USB accessory to connect to this device?
    </sl:alert-dialog-description>
  </sl:alert-dialog-header>
  <sl:alert-dialog-footer>
    <sl:alert-dialog-cancel>Don't allow</sl:alert-dialog-cancel>
    <sl:button>Allow</sl:button>
  </sl:alert-dialog-footer>
</sl:alert-dialog>
```

In a default-size dialog on a wide screen, the media sits beside the title and description.

**Example: Media.** In a default-size dialog on a wide screen, the media sits beside the title and description.

```html
<sl:button variant="outline" commandfor="share-project" command="show-modal">Share project</sl:button>
<sl:alert-dialog id="share-project">
  <sl:alert-dialog-header>
    <sl:alert-dialog-media><sl:icon name="circle-fading-plus"/></sl:alert-dialog-media>
    <sl:alert-dialog-title>Share this project?</sl:alert-dialog-title>
    <sl:alert-dialog-description>
      Anyone with the link will be able to view and edit this project.
    </sl:alert-dialog-description>
  </sl:alert-dialog-header>
  <sl:alert-dialog-footer>
    <sl:alert-dialog-cancel/>
    <sl:button>Share</sl:button>
  </sl:alert-dialog-footer>
</sl:alert-dialog>
```

## Destructive

For an action that deletes something, give the action `variant="destructive"`, and the media too.

**Example: Destructive.** A destructive action, with a destructive media square.

```html
<sl:button variant="destructive" commandfor="delete-chat" command="show-modal">Delete chat</sl:button>
<sl:alert-dialog id="delete-chat" size="sm">
  <sl:alert-dialog-header>
    <sl:alert-dialog-media variant="destructive"><sl:icon name="trash-2"/></sl:alert-dialog-media>
    <sl:alert-dialog-title>Delete chat?</sl:alert-dialog-title>
    <sl:alert-dialog-description>
      This will permanently delete this chat conversation. View <a href="#">Settings</a> to delete any
      memories saved during this chat.
    </sl:alert-dialog-description>
  </sl:alert-dialog-header>
  <sl:alert-dialog-footer>
    <sl:alert-dialog-cancel/>
    <sl:button variant="destructive">Delete</sl:button>
  </sl:alert-dialog-footer>
</sl:alert-dialog>
```

## Confirming with htmx

The alert dialog can come from the server, as the [dialog](https://shadleaf.dev/1.1.0-SNAPSHOT/components/dialog.md#rendered-by-the-server-htmx) does: a Delete
button in a row fetches it into an empty element on the page, and `open` shows it as soon as it is there.

```html
<!-- In the layout -->
<div id="modal-root"></div>

<!-- In the row -->
<sl:button variant="outline" hx:get="@{/members/{id}/delete(id=${member.id})}" hx-target="#modal-root">
  Delete
</sl:button>
```

The fragment has the question, and the destructive button sends the request:

```html
<sl:alert-dialog th:fragment="delete-dialog" id="delete-member" size="sm" open>
  <sl:alert-dialog-header>
    <sl:alert-dialog-media variant="destructive"><sl:icon name="trash-2"/></sl:alert-dialog-media>
    <sl:alert-dialog-title th:text="|Delete ${member.name}?|">Delete Ada Lovelace?</sl:alert-dialog-title>
    <sl:alert-dialog-description>They lose access to the team at once.</sl:alert-dialog-description>
  </sl:alert-dialog-header>
  <sl:alert-dialog-footer>
    <sl:alert-dialog-cancel/>
    <sl:button variant="destructive" hx:delete="@{/members/{id}(id=${member.id})}"
               th:hx-target="|#member-${member.id}|" hx-swap="outerHTML">Delete</sl:button>
  </sl:alert-dialog-footer>
</sl:alert-dialog>
```

The controller deletes, closes the dialog with the `sl-dialog-close` event (the same one that closes a dialog), says
what happened in a [toast](https://shadleaf.dev/1.1.0-SNAPSHOT/components/toast.md) and answers with nothing for the row, which removes it:

```java
@HxRequest
@DeleteMapping("/members/{id}")
public String delete(@PathVariable long id, HtmxRequest htmxRequest, HtmxResponse htmxResponse, Model model) {
  List<Member> rows = memberService.list(sortOf(htmxRequest.getCurrentUrl()));  // as the page shows them
  Member member = memberService.delete(id);
  htmxResponse.addTrigger("sl-dialog-close");
  htmxResponse.addTrigger("sl-toast", Toast.success("%s was deleted".formatted(member.name())));
  Member next = neighbour(rows, member);  // the next row, the previous one after the last, or null
  if (next == null) {
    return "members :: invite-button-focus";
  }
  model.addAttribute("member", next);
  return "members :: member-row-focus";  // the next row, out of band, its button focused
}
```

Closing returns focus to the Delete button that opened the dialog, but the swap then removes that button with its
row, and focus would fall back to the top of the page. The toast cannot take it instead: it goes away after a few
seconds, and the focus would fall with it. So the response moves the focus to what comes next: the next row's button,
re-rendered out of band with `autofocus`, which htmx focuses after the swap. After the last row that is the row before
it, and once the table is empty, the button that adds a member. The toast is announced, so a screen reader user hears
what happened and is where they can go on.

```html
<th:block th:fragment="member-row-focus" th:with="oob=true, focus=true">
  <tr th:replace="~{members :: member-row}"></tr>
</th:block>

<tr th:fragment="member-row" th:id="|member-${member.id}|"
    th:attr="hx-swap-oob=${oob != null and oob ? 'true' : null}">
  <td th:text="${member.name}">Ada Lovelace</td>
  <td>
    <sl:button variant="outline" hx:get="@{/members/{id}/delete(id=${member.id})}" hx-target="#modal-root"
               th:autofocus="${focus != null and focus}">Delete</sl:button>
  </td>
</tr>
```

"Next" is the next row as the page shows it: htmx sends the page's address in the `HX-Current-URL` header, so the
controller can read the sort order from it, as sample-01's `/dialog` page does.

Without JavaScript the same buttons need a page of their own for the question, or a form per row with
`<sl:alert-dialog>` in the page and a `type="submit"` action.

## Attributes

### sl:alert-dialog

Attributes of `<sl:alert-dialog>`: A modal dialog that interrupts the user with something important and waits for an answer, such as a confirmation before deleting. Built on the native `dialog` element with `role="alertdialog"`: a click outside does not close it, Escape does. Build it from `sl:alert-dialog-header` (with an optional `sl:alert-dialog-media`, `sl:alert-dialog-title` and `sl:alert-dialog-description`) and `sl:alert-dialog-footer` holding `sl:alert-dialog-cancel`, which takes the focus when the dialog opens, and the action, a `sl:button`. A button opens it with `commandfor` set to its `id` and `command="show-modal"`; an htmx response closes it with the `sl-dialog-close` event.

| Attribute | Values | Default | Description |
| --- | --- | --- | --- |
| `id` | *string* | **required** | The dialog's id, which the button that opens it points at with `commandfor`. The title and description ids derive from it. |
| `size` | `default`, `sm` | `default` | Size of the dialog. `sm` is narrower, with the header centred and the footer's two buttons side by side. |
| `open` | *boolean* | — | Open the dialog as soon as it is on the page, for a dialog the server renders in answer to a request (an htmx swap). Needs Alpine. |

Every prop can also be set from an expression: `th:id="${...}"`. Any other attribute passes through to the rendered element, and `class` is appended.

### sl:alert-dialog-header

Attributes of `<sl:alert-dialog-header>`: The top of a `sl:alert-dialog`: an optional `sl:alert-dialog-media`, a `sl:alert-dialog-title` and an optional `sl:alert-dialog-description`. Centred on a narrow screen and in a small dialog.

`sl:alert-dialog-header` declares no props; every attribute passes through.

### sl:alert-dialog-media

Attributes of `<sl:alert-dialog-media>`: An icon (or an image) at the start of a `sl:alert-dialog-header`, on a muted square: above the title, or beside title and description in a wide dialog.

| Attribute | Values | Default | Description |
| --- | --- | --- | --- |
| `variant` | `default`, `destructive` | `default` | Colour of the square and its icon: `destructive` for a dialog that confirms deleting something. |

Every prop can also be set from an expression: `th:variant="${...}"`. Any other attribute passes through to the rendered element, and `class` is appended.

### sl:alert-dialog-title

Attributes of `<sl:alert-dialog-title>`: The title of a `sl:alert-dialog`, inside `sl:alert-dialog-header`: a heading that names the dialog (`aria-labelledby`). Its id derives from the dialog's (`delete-account-title`).

| Attribute | Values | Default | Description |
| --- | --- | --- | --- |
| `level` | `1`, `2`, `3`, `4`, `5`, `6` | `2` | Heading level (`role="heading"` and `aria-level`). A dialog starts an outline of its own, so the default, 2, rarely needs changing. |

Every prop can also be set from an expression: `th:level="${...}"`. Any other attribute passes through to the rendered element, and `class` is appended.

### sl:alert-dialog-description

Attributes of `<sl:alert-dialog-description>`: Supporting text below a `sl:alert-dialog-title`, which describes the dialog (`aria-describedby`): what happens, and whether it can be undone. Its id derives from the dialog's (`delete-account-description`).

`sl:alert-dialog-description` declares no props; every attribute passes through.

### sl:alert-dialog-footer

Attributes of `<sl:alert-dialog-footer>`: The bottom of a `sl:alert-dialog`, for `sl:alert-dialog-cancel` and the action: side by side at the end on a wide screen, stacked with the first one at the bottom on a narrow one, and two equal columns in a small dialog.

`sl:alert-dialog-footer` declares no props; every attribute passes through.

### sl:alert-dialog-cancel

Attributes of `<sl:alert-dialog-cancel>`: The button that closes a `sl:alert-dialog` without doing anything, in its `sl:alert-dialog-footer`. It takes the focus when the dialog opens, so Enter never confirms by accident. Its text defaults to "Cancel" (`sl.alert-dialog.cancel`); content of its own replaces it. Any other attribute passes through to the `sl:button`.

| Attribute | Values | Default | Description |
| --- | --- | --- | --- |
| `variant` | `default`, `outline`, `ghost`, `destructive`, `secondary`, `link` | `outline` | Visual style of the button, as on `sl:button`. |
| `size` | `xs`, `sm`, `default`, `lg` | `default` | Size of the button, as on `sl:button`. |

Every prop can also be set from an expression: `th:variant="${...}"`. Any other attribute passes through to the rendered element, and `class` is appended.

## JavaScript

An alert dialog uses the dialog's Alpine component, `slDialog`; see [Dialog](https://shadleaf.dev/1.1.0-SNAPSHOT/components/dialog.md#javascript).

- **Most of it works without Alpine.** Opening from a button, closing with Escape or Cancel, and focus on Cancel are
  the browser's. Alpine is needed for `open` and the `sl-dialog-close` event.
- **It needs no Alpine plugin.**
- **Keep your own `x-data` off `<sl:alert-dialog>`**: it would replace `slDialog`. Put it on an element inside.
