# Sheet

> A dialog that slides in from an edge of the screen.

Source: https://wimdeblauwe.github.io/shadleaf/0.10.0-SNAPSHOT/components/sheet/

**Example: Sheet.** A form in a sheet. The form wraps the sheet, so the submit button in its footer submits it.

```html
<sl:button variant="outline" commandfor="sheet-profile" command="show-modal">Open</sl:button>
<form method="post" action="/profile">
  <sl:sheet id="sheet-profile">
    <sl:sheet-header>
      <sl:sheet-title>Edit profile</sl:sheet-title>
      <sl:sheet-description>
        Make changes to your profile here. Click save when you're done.
      </sl:sheet-description>
    </sl:sheet-header>
    <sl:field-group>
      <sl:field for="sheet-profile-name">
        <sl:field-label>Name</sl:field-label>
        <sl:input name="name" value="Pedro Duarte"/>
      </sl:field>
      <sl:field for="sheet-profile-username">
        <sl:field-label>Username</sl:field-label>
        <sl:input name="username" value="@peduarte"/>
      </sl:field>
    </sl:field-group>
    <sl:sheet-footer show-close-button>
      <sl:button type="submit">Save changes</sl:button>
    </sl:sheet-footer>
  </sl:sheet>
</form>
```

## Built from parts

A sheet is a [dialog](https://wimdeblauwe.github.io/shadleaf/0.10.0-SNAPSHOT/components/dialog.md) on an edge of the screen, for content that complements the page: filters,
settings, the details of a row. It is `<sl:sheet>` plus the parts you need, as in shadcn/ui: `<sl:sheet-header>`
(holding `<sl:sheet-title>` and `<sl:sheet-description>`), your own content and `<sl:sheet-footer>`.

```html
<sl:button variant="outline" commandfor="edit-profile" command="show-modal">Open</sl:button>

<form method="post" th:action="@{/profile}">
  <sl:sheet id="edit-profile">
    <sl:sheet-header>
      <sl:sheet-title>Edit profile</sl:sheet-title>
      <sl:sheet-description>Make changes to your profile here.</sl:sheet-description>
    </sl:sheet-header>
    <sl:field-group>
      ...
    </sl:field-group>
    <sl:sheet-footer show-close-button>
      <sl:button type="submit">Save changes</sl:button>
    </sl:sheet-footer>
  </sl:sheet>
</form>
```

Everything the dialog page says holds for a sheet: it is the browser's `<dialog>` element, buttons open and close it
with `commandfor` and `command`, the page behind it is inert and does not scroll, Escape and a click outside close it,
focus returns to the button that opened it, and `open` shows one the server renders (an htmx swap). The title and
description name and describe it, with ids derived from its `id`.

The content between the header and the footer gets the sheet's inline padding, so it lines up with them. The footer
stays at the bottom of a sheet on the side of the screen, however little is above it. Content taller than the screen
scrolls inside the sheet; put it in `<sl:sheet-body>` to scroll only the content, with the header and the footer in
view.

**Example: Body.** sl:sheet-body scrolls by itself, so the header and the footer stay in view.

```html
<sl:button variant="outline" commandfor="sheet-member" command="show-modal">Open</sl:button>
<sl:sheet id="sheet-member">
  <sl:sheet-header>
    <sl:sheet-title>Ada Lovelace</sl:sheet-title>
    <sl:sheet-description>Member since 1843.</sl:sheet-description>
  </sl:sheet-header>
  <sl:sheet-body>
    <sl:field-group>
      <sl:field for="sheet-member-name">
        <sl:field-label>Name</sl:field-label>
        <sl:input name="name" value="Ada Lovelace"/>
      </sl:field>
      <sl:field for="sheet-member-email">
        <sl:field-label>Email</sl:field-label>
        <sl:input name="email" type="email" value="ada@example.com"/>
      </sl:field>
      <sl:field for="sheet-member-notes">
        <sl:field-label>Notes</sl:field-label>
        <sl:textarea name="notes" rows="12">Wrote the first program for the Analytical Engine.</sl:textarea>
      </sl:field>
    </sl:field-group>
  </sl:sheet-body>
  <sl:sheet-footer show-close-button>
    <sl:button type="submit">Save changes</sl:button>
  </sl:sheet-footer>
</sl:sheet>
```

## Side

`side` is the edge it slides in from: `top`, `right` (the default), `bottom` or `left`. A sheet on the top or bottom
is as wide as the screen and as tall as its content; one on the right or left is as tall as the screen and three
quarters of its width, up to a maximum.

**Example: Side.** side="top", "right" (the default), "bottom" or "left".

```html
<sl:button variant="outline" commandfor="sheet-top" command="show-modal">Top</sl:button>
<sl:button variant="outline" commandfor="sheet-right" command="show-modal">Right</sl:button>
<sl:button variant="outline" commandfor="sheet-bottom" command="show-modal">Bottom</sl:button>
<sl:button variant="outline" commandfor="sheet-left" command="show-modal">Left</sl:button>
<sl:sheet id="sheet-top" side="top">
  <sl:sheet-header>
    <sl:sheet-title>From the top</sl:sheet-title>
    <sl:sheet-description>A sheet on the top edge, as wide as the screen.</sl:sheet-description>
  </sl:sheet-header>
  <sl:sheet-footer show-close-button/>
</sl:sheet>
<sl:sheet id="sheet-right" side="right">
  <sl:sheet-header>
    <sl:sheet-title>From the right</sl:sheet-title>
    <sl:sheet-description>The default: the end of the line, which is the left on a right-to-left page.</sl:sheet-description>
  </sl:sheet-header>
  <sl:sheet-footer show-close-button/>
</sl:sheet>
<sl:sheet id="sheet-bottom" side="bottom">
  <sl:sheet-header>
    <sl:sheet-title>From the bottom</sl:sheet-title>
    <sl:sheet-description>A sheet on the bottom edge, as wide as the screen.</sl:sheet-description>
  </sl:sheet-header>
  <sl:sheet-footer show-close-button/>
</sl:sheet>
<sl:sheet id="sheet-left" side="left">
  <sl:sheet-header>
    <sl:sheet-title>From the left</sl:sheet-title>
    <sl:sheet-description>The start of the line, which is the right on a right-to-left page.</sl:sheet-description>
  </sl:sheet-header>
  <sl:sheet-footer show-close-button/>
</sl:sheet>
```

`right` and `left` are the end and the start of a line, as everywhere in Shadleaf: on a right-to-left page
(`dir="rtl"`) a `right` sheet opens on the left and slides in from there.

The sheet fades in and slides a short distance from its edge. With reduced motion (`prefers-reduced-motion: reduce`)
it only fades.

## Close buttons

The close button in the corner is on by default; `show-close-button="false"` leaves it out. `<sl:sheet-footer
show-close-button>` adds an outline "Close" button after the footer's own actions.

**Example: No close button in the corner.** show-close-button="false" leaves the x out; the footer's close button remains.

```html
<sl:button variant="outline" commandfor="sheet-notifications" command="show-modal">Notifications</sl:button>
<sl:sheet id="sheet-notifications" show-close-button="false">
  <sl:sheet-header>
    <sl:sheet-title>Notifications</sl:sheet-title>
    <sl:sheet-description>You have no unread messages.</sl:sheet-description>
  </sl:sheet-header>
  <sl:sheet-footer show-close-button/>
</sl:sheet>
```

## Loaded into the sheet: a body

`<sl:sheet-body>` is what htmx loads, as the [dialog's body](https://wimdeblauwe.github.io/shadleaf/0.10.0-SNAPSHOT/components/dialog.md#loaded-into-the-dialog-a-body). You put the sheet
in the page once, closed. A request that targets its body (`#<sheet id>-body`) opens it at once and shows a spinner
until the answer is swapped in:

1. **It opens** as soon as the request is sent, with the focus in it.
2. **A spinner covers the body** while the request runs. It shows only after a moment (200 ms), so a quick answer does
   not flicker, and the body takes no clicks meanwhile.
3. **The answer is swapped in**, and the focus moves to its first field, if it has one.

Closing the sheet while the request still runs aborts it. A body that was empty in the page is emptied again when the
sheet closes, so it never shows the previous row while the next one loads.

### Show the details of a row

1. Add the sheet to the page, with an empty `<sl:sheet-body>`. The body's content gets the id `<id>-body`, here
   `member-details-body`.

   ```html
   <sl:sheet id="member-details">
     <sl:sheet-header>
       <sl:sheet-title>Member details</sl:sheet-title>
     </sl:sheet-header>
     <sl:sheet-body/>
     <sl:sheet-footer show-close-button/>
   </sl:sheet>
   ```

2. Write a fragment with what goes in the body.

   ```html
   <div th:fragment="member-details">
     <dl>
       <dt>Name</dt>
       <dd th:text="${member.name}">Ada Lovelace</dd>
       <dt>Email</dt>
       <dd th:text="${member.email}">ada@example.com</dd>
     </dl>
   </div>
   ```

3. Write a controller method that returns the fragment.

   ```java
   @HxRequest
   @GetMapping("/members/{id}")
   public String details(@PathVariable long id, Model model) {
     model.addAttribute("member", memberService.get(id));
     return "members :: member-details";
   }
   ```

4. Add a button that asks for it with `hx:get`, with `hx-target` set to the sheet's body.

   ```html
   <sl:button variant="outline"
              hx:get="@{/members/{id}(id=${member.id})}"
              hx-target="#member-details-body">Details</sl:button>
   ```

A click on the button opens the sheet and loads the member. While the answer takes its time, the sheet shows the
spinner:

**Example: Loading.** While an htmx request into the body runs, a spinner covers it. Here the body keeps data-loading, which the script sets and removes.

```html
<sl:button variant="outline" commandfor="sheet-loading" command="show-modal">Open</sl:button>
<sl:sheet id="sheet-loading">
  <sl:sheet-header>
    <sl:sheet-title>Member details</sl:sheet-title>
  </sl:sheet-header>
  <sl:sheet-body data-loading="true"/>
  <sl:sheet-footer show-close-button/>
</sl:sheet>
```

### When loading fails

When the request fails (an error status, no connection, a timeout), the sheet closes again and a
[toast](https://wimdeblauwe.github.io/shadleaf/0.10.0-SNAPSHOT/components/toast.md) shows "Something went wrong. Please try again." The `error` slot replaces that message:

```html
<sl:sheet-body>
  <sl:slot name="error">The member could not be loaded.</sl:slot>
</sl:sheet-body>
```

The toast needs an `<sl:toaster>` on the page (in the layout). Without one, or with `load-error="inline"`, the sheet
stays open and shows the message in it instead:

```html
<sl:sheet-body load-error="inline">
  <sl:slot name="error">The member could not be loaded.</sl:slot>
</sl:sheet-body>
```

### A form in the sheet

Put the actions in `<sl:sheet-footer>`: it stays at the bottom while the body scrolls. The footer is part of the sheet
in the page and the form is what htmx loads, so the submit button points at the form with `form`:

```html
<sl:sheet id="member-edit">
  <sl:sheet-header>
    <sl:sheet-title>Edit member</sl:sheet-title>
  </sl:sheet-header>
  <sl:sheet-body/>
  <sl:sheet-footer show-close-button>
    <sl:button type="submit" form="member-edit-form">Save</sl:button>
  </sl:sheet-footer>
</sl:sheet>

<sl:button variant="outline"
           hx:get="@{/members/{id}/edit(id=${member.id})}"
           hx-target="#member-edit-body">Edit</sl:button>
```

The form swaps only itself, so the sheet stays open while it has errors:

```html
<form th:fragment="edit-form" id="member-edit-form" th:object="${memberForm}"
      hx:post="@{/members/{id}(id=${member.id})}" hx-target="this" hx-swap="outerHTML">
  <sl:form-errors autofocus/>
  <sl:field th:field="*{name}">
    <sl:field-label>Name</sl:field-label>
    <sl:input/>
    <sl:field-error/>
  </sl:field>
</form>
```

The spinner covers the form while it is being saved too, and a second click on Save sends nothing. With errors, the
controller returns the form again; when the save succeeds, it closes the sheet with the `HX-Trigger` response header
and updates the member's row out of band:

```java
@HxRequest
@PostMapping("/members/{id}")
public String update(@PathVariable long id, @Valid @ModelAttribute("memberForm") MemberForm memberForm,
    BindingResult bindingResult, HtmxResponse htmxResponse, Model model) {
  if (bindingResult.hasErrors()) {
    return "members :: edit-form";
  }
  model.addAttribute("member", memberService.update(id, memberForm));
  htmxResponse.addTrigger("sl-dialog-close");
  htmxResponse.setReswap(HtmxReswap.none());
  return "members :: member-row-oob";  // the row, with hx-swap-oob="true"
}
```

htmx fires `sl-dialog-close` on the form, inside the sheet, which closes; the focus returns to the Edit button. Give
that button an id if the row is replaced, so htmx puts the focus back on the new one (see
[Dialog](https://wimdeblauwe.github.io/shadleaf/0.10.0-SNAPSHOT/components/dialog.md#rendered-by-the-server-htmx)). A failed save (an error status) shows the error in the sheet and
keeps what was typed.

## Width

A sheet on the right or left is three quarters of the screen wide, up to a maximum the skin sets (`24rem` in vega).
For a wider one, give it a class and set the maximum in your own stylesheet, loaded after the library's: the library's
rules sit in a cascade layer, so yours win.

```html
<sl:sheet id="member-edit" class="sheet-wide">...</sl:sheet>
```

```css
.sheet-wide {
  max-width: 40rem;
}
```

If you compile Tailwind yourself (the `embedded` bundle), a utility class does the same: `class="sm:max-w-2xl"`.

## Attributes

### sl:sheet

Attributes of `<sl:sheet>`: A dialog that slides in from an edge of the screen, for content that complements the page: filters, settings, details. Built on the native `dialog` element, like `sl:dialog`: the page behind it is inert, Escape and a click outside close it, and focus returns to where it came from. Build it from `sl:sheet-header` (holding `sl:sheet-title` and `sl:sheet-description`), your content and `sl:sheet-footer`. A button opens it with `commandfor` set to the sheet's `id` and `command="show-modal"`.

| Attribute | Values | Default | Description |
| --- | --- | --- | --- |
| `id` | *string* | **required** | The sheet's id, which the buttons that open and close it point at with `commandfor`. The title and description ids derive from it. |
| `side` | `top`, `right`, `bottom`, `left` | `right` | The edge it slides in from. `right` and `left` are the end and the start of a line: on a right-to-left page a `right` sheet opens on the left. |
| `open` | *boolean* | — | Open the sheet as soon as it is on the page, for a sheet the server renders in answer to a request (an htmx swap). Needs Alpine. |
| `show-close-button` | *boolean* | `true` | A close button (an x) in the corner. |

Every prop can also be set from an expression: `th:id="${...}"`. Any other attribute passes through to the rendered element, and `class` is appended.

### sl:sheet-header

Attributes of `<sl:sheet-header>`: The top of a `sl:sheet`: a `sl:sheet-title` and an optional `sl:sheet-description` below it.

`sl:sheet-header` declares no props; every attribute passes through.

### sl:sheet-title

Attributes of `<sl:sheet-title>`: The title of a `sl:sheet`, inside `sl:sheet-header`: a heading that names the sheet (`aria-labelledby`). Its id derives from the sheet's (`filters-title`).

| Attribute | Values | Default | Description |
| --- | --- | --- | --- |
| `level` | `1`, `2`, `3`, `4`, `5`, `6` | `2` | Heading level (`role="heading"` and `aria-level`). A sheet starts an outline of its own, so the default, 2, rarely needs changing. |

Every prop can also be set from an expression: `th:level="${...}"`. Any other attribute passes through to the rendered element, and `class` is appended.

### sl:sheet-description

Attributes of `<sl:sheet-description>`: Supporting text below a `sl:sheet-title`, which describes the sheet (`aria-describedby`). Its id derives from the sheet's (`filters-description`).

`sl:sheet-description` declares no props; every attribute passes through.

### sl:sheet-body

Attributes of `<sl:sheet-body>`: The content of a `sl:sheet` between its header and footer, which scrolls by itself so the header and footer stay in view. Its content has the id `&lt;id&gt;-body` (`edit-body`), the target for htmx: a request from outside that targets it opens the sheet at once and shows a spinner until the answer is swapped in; closing aborts a request still running. Left empty, it is emptied again when the sheet closes, so it never shows the previous content. When a request fails, the error shows in the sheet, or, for the request that opened it, as a toast (see `load-error`). Needs Alpine.

| Attribute | Values | Default | Description |
| --- | --- | --- | --- |
| `load-error` | `toast`, `inline` | `toast` | What happens when the request that opened the sheet fails: `toast` closes it and shows the error as a toast (shown in the sheet when the page has no `sl:toaster`); `inline` keeps it open with the error in it. A failed request from inside (a form) always shows the error in the sheet, so nothing typed is lost. |

Every prop can also be set from an expression: `th:load-error="${...}"`. Any other attribute passes through to the rendered element, and `class` is appended.

### sl:sheet-footer

Attributes of `<sl:sheet-footer>`: The bottom of a `sl:sheet`, for its actions, stacked. It stays at the bottom of a sheet on the side of the screen however little content there is above it.

| Attribute | Values | Default | Description |
| --- | --- | --- | --- |
| `show-close-button` | *boolean* | — | An outline "Close" button after the other actions. |

Every prop can also be set from an expression: `th:show-close-button="${...}"`. Any other attribute passes through to the rendered element, and `class` is appended.

## JavaScript

A sheet uses the dialog's Alpine component, `slDialog`; see [Dialog](https://wimdeblauwe.github.io/shadleaf/0.10.0-SNAPSHOT/components/dialog.md#javascript).

- **Most of it works without Alpine.** Opening from a button, Escape, a click outside and the close buttons are the
  browser's. Alpine is needed for `open`, the `sl-dialog-close` event, which closes a sheet as it closes a dialog, and
  a body that htmx loads.
- **It needs no Alpine plugin.**
- **Keep your own `x-data` off `<sl:sheet>`**: it would replace `slDialog`. Put it on an element inside.
