# Forms with htmx

> Post a Shadleaf form with htmx, get it back with its errors without a page load, and keep it working as a plain POST.

Source: https://shadleaf.dev/current/guides/htmx/

Shadleaf's form components render plain HTML with every `id`, `aria-invalid` and `aria-describedby` in place, so a
form that htmx swaps in is as accessible as one that loaded with the page. Shadleaf has no dependency on htmx and
ships none of it; your application loads htmx, and the components pass `hx-*` attributes through like any other.

The snippets use htmx 2. On htmx 4 (`htmx.org` 4.x with htmx-spring-boot 6.x) several of them need a change, and none
of them fails with an error: see [htmx 2 and htmx 4](#htmx-2-and-htmx-4).

This guide follows the "Form with htmx" page of sample-01 (`samples/shadleaf-sample-01`, `/htmx-form`): it posts with
htmx, comes back with its errors or a confirmation, moves focus to the right place, and still works as a normal POST
without JavaScript. The sample's `/form` page has the same fields without htmx, for comparison.

## Loading htmx

The sample uses [htmx-spring-boot](https://github.com/wimdeblauwe/htmx-spring-boot) for the request and response
headers, and htmx itself from its webjar, pinned in the pom:

```xml
<dependency>
  <groupId>io.github.wimdeblauwe</groupId>
  <artifactId>htmx-spring-boot-thymeleaf</artifactId>
  <version>5.1.1</version>
</dependency>
<dependency>
  <groupId>org.webjars.npm</groupId>
  <artifactId>htmx.org</artifactId>
  <version>2.0.11</version>
</dependency>
<dependency>
  <groupId>org.webjars</groupId>
  <artifactId>webjars-locator-lite</artifactId>
</dependency>
```

```html
<head>
  ...
  <th:block th:replace="~{sl/layout :: assets}"></th:block>
  <meta name="htmx-config" content='{"includeIndicatorStyles": false, "allowEval": false}'>
  <script th:src="@{/webjars/htmx.org/dist/htmx.min.js}" defer></script>
</head>
```

- **A webjar, served by your application.** It needs no Node build, and it comes from your own origin, so a policy
  with `script-src 'self'` allows it (add the [nonce](https://shadleaf.dev/current/guides/csp.md) if your policy uses one). With `webjars-locator-lite`
  the template leaves the version out, and Spring Boot writes it into the URL (`/webjars/htmx.org/2.0.11/...`), which
  also busts caches on an upgrade. An application that bundles its JavaScript with Vite can `import 'htmx.org'`
  instead.
- **The config keeps htmx within a strict [CSP](https://shadleaf.dev/current/guides/csp.md).** By default htmx adds an inline `<style>` for its request
  indicator, which `style-src 'self'` blocks, and evaluates `hx-on` handlers and `js:` values with `eval`. Turn both
  off unless you use them.

Behind Spring Security, an htmx request made after the session expired gets the sign-in page swapped into its
target, or, for a POST, nothing at all. See [htmx after the session expired](https://shadleaf.dev/current/guides/spring-security.md#htmx-after-the-session-expired).

## One form, two ways to post

The form keeps its `th:action` and `method="post"`, and gets `hx-post` next to them. htmx posts it when JavaScript
runs; without it, the browser does. The part htmx swaps is a fragment that holds the form, or the confirmation:

```html
<div id="contact" th:fragment="contact">
  <th:block th:if="${sentTo}">
    <sl:alert tabindex="-1" autofocus>
      <sl:icon name="circle-check"/>
      <sl:alert-title>Message sent</sl:alert-title>
      <sl:alert-description th:text="|We will reply to ${sentTo}.|">We will reply to you.</sl:alert-description>
    </sl:alert>
    <sl:button as="a" variant="outline" th:href="@{/form}">Send another message</sl:button>
  </th:block>

  <form th:unless="${sentTo}" th:object="${contactForm}" th:action="@{/form}" method="post" novalidate
        hx:post="@{/form}" hx-target="#contact" hx-swap="outerHTML">
    <sl:form-errors autofocus/>
    <sl:field-group>
      <sl:field th:field="*{name}">
        <sl:field-label>Name</sl:field-label>
        <sl:input autocomplete="name"/>
        <sl:field-error/>
      </sl:field>
      ...
    </sl:field-group>
    <sl:button type="submit">Send</sl:button>
  </form>
</div>
```

`hx:post` (with a colon) is htmx-spring-boot's Thymeleaf dialect: it resolves `@{/form}` like `th:action` does, and
adds the CSRF header when Spring Security is on. `hx-target` is a plain attribute: `#` starts a message expression in
Thymeleaf. The target is the fragment's own wrapper, so the fragment replaces itself, and a second submit works like
the first.

The controller answers an htmx request (it sends `HX-Request`) with the fragment alone, and everything else as
before, with post/redirect/get on success:

```java
@PostMapping("/form")
public String submit(@Valid @ModelAttribute("contactForm") ContactForm contactForm, BindingResult bindingResult,
    HtmxRequest htmxRequest, Model model, RedirectAttributes redirectAttributes) {
  if (htmxRequest.isHtmxRequest()) {
    if (!bindingResult.hasErrors()) {
      model.addAttribute("sentTo", contactForm.getEmail());
    }
    return "form :: contact";
  }
  if (bindingResult.hasErrors()) {
    return "form";
  }
  redirectAttributes.addFlashAttribute("sentTo", contactForm.getEmail());
  return "redirect:/form";
}
```

- **Errors come back with status 200**, as for the full page. htmx 2 does not swap 4xx responses unless you
  configure it to, so a `422` would need a `responseHandling` entry. (htmx 4 swaps them, and has no
  `responseHandling`: see [htmx 2 and htmx 4](#htmx-2-and-htmx-4).)
- **A valid htmx submit gets the confirmation, not a redirect.** An htmx POST is not a navigation: a reload cannot
  send it again, which is what post/redirect/get protects against. An `HX-Redirect` (`redirect:htmx:/form` with
  htmx-spring-boot, flash attributes included) works too, but loads the whole page again, and focus with it.
- **Two handler methods** with `@HxRequest` on one of them work as well, if you prefer that to the `if`.

## Where focus goes

When htmx replaces the form, the submit button that had the focus is gone, and focus falls back to the page.
`<sl:form-errors autofocus/>` gives the server's answer to where it should go, the same for both ways to post:

- a global error, or any error with `show="all"`: the summary, which the screen reader reads out;
- field errors only: the first control with an error, which is read with its label, its invalid state and, through
  `aria-describedby`, its error;
- the confirmation has its own `tabindex="-1"` and `autofocus`.

htmx focuses the first `[autofocus]` in content it swaps in. It first tries to put focus back on the element that had
it, by id, so a user who pressed <kbd>Enter</kbd> in the name field would stay there; `autofocus` runs later and
wins. See [Form errors](https://shadleaf.dev/current/components/form-errors.md#announcement-and-focus) for the details.

## Announcement

The errors are announced because focus moves to them, so there is no `role="alert"` on the summary or the field
errors, in the htmx response or anywhere else. A live region would read the same message a second time, and
`role="alert"` on every field error would read all of them at once. The confirmation has no `role="status"` for the
same reason: a live region inserted together with its text is not announced reliably anyway.

Add `role="alert"` when content changes without moving focus, for example a single field that htmx validates as the
user leaves it: see [Field](https://shadleaf.dev/current/components/field.md#errors).

## Validating as you type

On htmx 4, `<sl:form th:validate="@{/contact/validate}">` validates each field on the server while the user edits it,
and swaps only that field: no rules in JavaScript, no focus moved, nothing announced. See
[Validating as you type](https://shadleaf.dev/current/components/form.md#validating-as-you-type).

## Back button and history

`hx-post` pushes no history entry. The URL stays `/form`, and htmx's history cache, which snapshots the page into
`localStorage` when htmx pushes a URL, is not involved: back from another page shows `/form` as the browser kept it
or as the server renders it again. If you boost navigation (`hx-boost`) or push URLs, htmx does snapshot pages, and a
failed form keeps the user's input in its `value` attributes. For a form with personal data, set
`"historyCacheSize": 0` in the htmx config, or `hx-history="false"` on the page. This is htmx 2: htmx 4 keeps no
snapshots and asks the server again ([htmx 2 and htmx 4](#history)).

## Dialogs

A dialog can come from the server too: a button fetches `<sl:dialog open>` into an empty element, the form in it
swaps only itself while it has errors, and an `HX-Trigger: sl-dialog-close` response header closes it after a valid
save. Open dialogs close before htmx snapshots a page for its history, so back never restores one. See
[Dialog](https://shadleaf.dev/current/components/dialog.md#rendered-by-the-server-htmx); sample-01's `/dialog` page does all of it.

The same works for an [alert dialog](https://shadleaf.dev/current/components/alert-dialog.md#confirming-with-htmx) that confirms a delete: its
destructive button sends `hx-delete`, and the response closes it with the same event, shows a toast saying what
happened, removes the row and moves the focus to the next row. A [sheet](https://shadleaf.dev/current/components/sheet.md) is a dialog too, with the same `open`
and `sl-dialog-close`.

## Menus and popovers

A [dropdown menu](https://shadleaf.dev/current/components/dropdown-menu.md) item takes `hx-*` like a button: it sends its request and the menu
closes, with the focus back on the trigger. A menu of row actions whose response replaces the row keeps the focus in
place when the trigger has an id, which it gets from the menu's (`th:id="|order-${order.id}-actions|"` gives
`order-42-actions-trigger`). A form in a [popover](https://shadleaf.dev/current/components/popover.md#a-form-in-a-popover-with-htmx) closes
it with `HX-Trigger: sl-popover-close`. A swap that replaces an open menu or popover leaves nothing open behind it,
and back never restores one open: whether a popover is open is not part of the markup htmx saves.

## Toasts

An `HX-Trigger: sl-toast` response header shows a [toast](https://shadleaf.dev/current/components/toast.md), whatever the response swaps:
`htmxResponse.addTrigger("sl-toast", Toast.success("Member deleted"))`. A response can also swap `<sl:toast>`s into
the toaster out of band. Use `HX-Trigger` rather than `HX-Trigger-After-Swap`/`-After-Settle`, whose events are lost
when the swap removed the element that sent the request, and a flash attribute rather than a toast when the response
redirects. Toasts are removed before htmx saves a page in its history cache, so back never shows one again. A toast
never takes the focus: it goes away.

## htmx 2 and htmx 4

The examples in this guide, in [Tables with htmx](https://shadleaf.dev/current/guides/tables-htmx.md), and on the component pages use **htmx 2**
(`htmx.org` 2.0.11 with htmx-spring-boot 5.1.1): that is what samples 02 and 03 load, and what the docs' browser
tests run against. Sample-01 runs on htmx 4 (`htmx.org` 4.0.1 with htmx-spring-boot 6.0.1), so its pages show the
changes below in place. A few of the browser tests also run on htmx 4.0.1 (popovers, toasts, file upload, the
dialog body and validating as you type), and the components work with it. The guides' own snippets are not tested on htmx 4. This section lists what a Shadleaf user
changes to use them there (`htmx.org` 4.0.1 with htmx-spring-boot 6.0.1), and each htmx page has a note pointing here.
htmx's own list is [What's new in htmx 4](https://four.htmx.org/docs/whats-new-in-htmx-4).

### Versions

```xml
<dependency>
  <groupId>io.github.wimdeblauwe</groupId>
  <artifactId>htmx-spring-boot-thymeleaf</artifactId>
  <version>6.0.1</version> <!-- was 5.1.1 -->
</dependency>
<dependency>
  <groupId>org.webjars.npm</groupId>
  <artifactId>htmx.org</artifactId>
  <version>4.0.1</version> <!-- was 2.0.11 -->
</dependency>
```

htmx-spring-boot 6 is for htmx 4 only: an `HtmxRequest` argument works with both, but `isPartialRequest()` throws
under htmx 2, and `hx:post` and the other `hx:` attributes work as before, with the CSRF header on the element's own
`hx-headers`.

### The config

```html
<!-- htmx 2 -->
<meta name="htmx-config" content='{"includeIndicatorStyles": false, "allowEval": false}'>
<!-- htmx 4 -->
<meta name="htmx-config" content='{"includeIndicatorCSS": false}'>
```

`includeIndicatorStyles` is now `includeIndicatorCSS`; htmx 4 ignores the old name and keeps adding its indicator
rules. There is no `allowEval` in htmx 4. `hx-on:` handlers and `js:` values are still evaluated with
`Function`, so a policy without `'unsafe-eval'` blocks them: none of Shadleaf's snippets use them. htmx's `hx-csp`
extension (`safeEval`) covers them, with a nonce on every htmx element.

### Inheritance is explicit

In htmx 2 an element inherits `hx-boost`, `hx-target`, `hx-select` and `hx-swap` from its ancestors. In htmx 4 it
inherits an attribute only when the ancestor has it with the `:inherited` modifier. Without it, `hx-boost` on the
body boosts nothing: it is not on the links.

```html
<!-- htmx 2 -->
<body hx-boost="true">
<div id="people-results"
     hx-boost="true" hx-target="#people-results" hx-select="#people-results" hx-swap="outerHTML show:none">
<!-- htmx 4 -->
<body hx-boost:inherited="true">
<div id="people-results"
     hx-boost:inherited="true" hx-target:inherited="#people-results" hx-select:inherited="#people-results"
     hx-swap:inherited="outerHTML show:none">
```

An attribute on the element that makes the request needs no modifier (`hx-post` with `hx-target` on a form), and
`hx-boost="false"` on a link still opts it out. To keep htmx 2's behavior while you migrate, set
`"implicitInheritance": true` in the config.

`hx-select="unset"`, which a menu item inside the table's results uses to drop the `hx-select` it would inherit, is
gone: an empty `hx-select=""` on the element does it, as the element's own attribute wins and an empty selector
selects nothing.

### Request headers and the fragment check

| | htmx 2 | htmx 4 |
| --- | --- | --- |
| The element that sent the request | `HX-Trigger` (its id), `HX-Trigger-Name` | `HX-Source`, as `tag#id` (`a#sort-name`) |
| The target | `HX-Target` (its id; none without an id) | `HX-Target`, as `tag#id`, always (`div#people-results`, `body`) |
| Full page or part | | `HX-Request-Type`: `full` or `partial` |

htmx-spring-boot 6 reads them: `getSource()` replaces `getTriggerId()` and `getTriggerName()` (and `@HxRequest(source =
...)` replaces `triggerId` and `triggerName`), and `getTarget()` returns `div#people-results`, not `people-results`.

This changes the check that tells a boosted link in the navigation from a request for a fragment (see the
[application shell](https://shadleaf.dev/current/components/application-shell.md#with-htmx)). A link boosted from a `<body>` without an id sends
`HX-Target: body` in htmx 4, where htmx 2 sends no `HX-Target`, so `getTarget() == null` is never true and the sidebar's
links get a fragment. This works with both:

```java
static boolean wantsFragment(HtmxRequest request) {
  String target = request.getTarget();
  return request.isHtmxRequest()
      && !request.isHistoryRestoreRequest()
      && !(request.isBoosted() && (target == null || target.equals("body") || target.startsWith("body#")));
}
```

Do not use `HX-Request-Type` for this: htmx 4 sends `full` for a request with an `hx-select` too, so the table's own
requests, which select `#people-results` from the page, are `full` as well. A history restore sends `HX-Request`,
`HX-History-Restore-Request`, `HX-Target: body` and `full`, and not `HX-Boosted`.

### History

htmx 4 has no history cache. On back and forward it asks the server for the page again, with
`HX-History-Restore-Request`, and swaps it into the body (or the `[hx-history-elt]`). What the Back button sections of
these guides say about snapshots, `hx-history="false"` and `historyCacheSize` is htmx 2 only: both are gone, and the
page is always rendered again, so a search field shows the value of the address. A controller still has to answer the
restore request with the whole page, as the check above does.

The optional `hx-history-cache` extension brings the snapshots back, with `hx-history="false"`. Do not combine it with
`hx-alpine-compat` for now, which Shadleaf needs on htmx 4 (see below). Before the extension saves a page,
`hx-alpine-compat` stops every Alpine component on it (htmx 4.0.1). So Shadleaf cannot close an open dialog or remove
the toasts before the snapshot, and after a request that pushes a URL, the components outside its target no longer
work ([htmx#4221](https://github.com/bigskysoftware/htmx/issues/4221)).

### Response headers and events

`HX-Trigger` works as in htmx 2: a JSON object of event names and details, which Shadleaf's `sl-toast`,
`sl-dialog-close` and `sl-popover-close` are. `HX-Trigger-After-Swap` and `HX-Trigger-After-Settle` are gone, and so
are `HtmxResponse.addTriggerAfterSwap()` and `addTriggerAfterSettle()` in htmx-spring-boot 6; the guides use
`HX-Trigger`. `HX-Redirect`, `HX-Location`, `HX-Refresh`, `HX-Push-Url`, `HX-Replace-Url`, `HX-Retarget`, `HX-Reswap`
and `HX-Reselect` remain.

### Swapping

- **A response that holds only out-of-band content swaps nothing into the target.** htmx 2 emptied the target then;
  htmx 4 skips the main swap when only `hx-swap-oob` elements are left, so the delete in the [alert dialog](https://shadleaf.dev/current/components/alert-dialog.md#confirming-with-htmx),
  whose answer is the next row out of band, would keep its row. Ask for the empty swap with `swapEmpty:true`
  (`"allowEmptySwapAfterOOB": true` in the config does it everywhere):

  ```html
  <sl:button variant="destructive" hx:delete="@{/members/{id}(id=${member.id})}"
             th:hx-target="|#member-${member.id}|" hx-swap="outerHTML swapEmpty:true">Delete</sl:button>
  ```

  A toast sent out of band into `#toaster` needs nothing.
- **`hx-delete` leaves the enclosing form out**, like `hx-get`: add `hx-include="closest form"` where it needs the
  form's fields.
- **`hx-disabled-elt` is `hx-disable`**, and htmx 2's `hx-disable` (leave this element alone) is `hx-ignore`. The
  indicator class is still `htmx-request`, and `hx-indicator`, `hx-sync="this:replace"`, `hx-swap-oob` and `show:none`
  work as before. A boosted link still scrolls to the top of the page unless `show:none` is there.

### What else changed for Shadleaf

- **Validating as you type is htmx 4 only.** [`<sl:form validate>`](https://shadleaf.dev/current/components/form.md#validating-as-you-type)
  swaps with `outerMorph` and `<hx-partial>`, which htmx 2 does not have.
- **Do not morph a combobox, select, calendar or date picker yourself.** Their scripts build part of their markup in
  the browser (the combobox's input and list, the calendar's months, the date picker's button text and name). A
  morph (`outerMorph`, `innerMorph`) makes the page match the server's markup and removes those parts, and the
  component does not start again, so it stops working. Swap a region that holds one with `outerHTML` or `innerHTML`,
  or put `hx-morph-skip` on the component to leave it out of the morph (it then keeps its value and ignores what the
  server sent for it). Validating as you type morphs fields too, but Shadleaf keeps these components through its own
  swaps.

- **Load htmx's Alpine extension.** htmx 4's settle step puts every attribute of the server's markup back on a swapped
  element whose `id` stayed the same, removing the ones the components set when they start (`aria-expanded` on a
  menu's trigger, for one). Load `ext/hx-alpine-compat.js` after htmx: it holds Alpine back until the settle is done.
  The htmx 4 webjar has it. Alpine must start after the extension, so both scripts go before the `assets` fragment, which starts Alpine (its module script runs in document order with
  the `defer` scripts):

  ```html
  <script th:src="@{/webjars/htmx.org/dist/htmx.min.js}" defer></script>
  <script th:src="@{/webjars/htmx.org/dist/ext/hx-alpine-compat.min.js}" defer></script>
  <th:block th:replace="~{sl/layout :: assets}"></th:block>
  ```

  With `shadleaf.assets.alpine=external`, they go before the script that loads the application's Alpine.

  Without it, turn the settle off, `<meta name="htmx-config" content='{"defaultSettleDelay":0}'>` (or `settle:0` in
  one `hx-swap`), which also turns off htmx's settle transitions.
- **Error answers are swapped in.** htmx 4 swaps a `4xx` or `5xx` response like any other, where htmx 2 leaves the
  page alone. A [dialog or sheet body](https://shadleaf.dev/current/components/dialog.md#loaded-into-the-dialog-a-body) stops that for the
  requests into it, so a failed save still keeps what was typed; elsewhere, use `hx-status:5xx="swap:none"` or
  `"noSwap": [204, 304, "4xx", "5xx"]` in the config where you want htmx 2's behavior.
- **`HX-Trigger` runs after the swap**, on the element that sent the request, or on the document when the swap
  removed it. A dialog, alert dialog or sheet whose request answers with `sl-dialog-close` closes when the swap
  starts instead, as on htmx 2: the page behind a modal dialog is inert, so otherwise htmx could not put the focus
  back on a replaced opener (by its id) or on an `autofocus` element in the answer. Send `sl-popover-close` from a
  request whose element stays (`HtmxReswap.none()`, or a target elsewhere); `sl-toast` works either way.
- **Use htmx 4.0.1 or later. A `null` detail in `HX-Trigger` throws in htmx 4.0.0** (`detail.cancelled` in its `trigger()`; 4.0.1 reads
  `detail?.cancelled`, see [htmx#4135](https://github.com/bigskysoftware/htmx/issues/4135)). htmx-spring-boot sends one
  for an event without a detail when another event of the same response has one, such as `sl-dialog-close` next to a
  toast: on 4.0.0, give it an empty detail there, `htmxResponse.addTrigger("sl-dialog-close", Map.of())`.
