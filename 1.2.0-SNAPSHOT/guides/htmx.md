# htmx

> Add htmx to an application that uses Shadleaf, within a strict CSP, and what differs if you are still on htmx 2.

Source: https://shadleaf.dev/1.2.0-SNAPSHOT/guides/htmx/

[htmx](https://four.htmx.org/) is an essential part of a Thymeleaf developer's toolkit nowadays.
It makes it easy to create rich, interactive UIs, without the need to write JavaScript yourself.

Shadleaf has no hard dependency on htmx, so it is not included by default.
But both do work nicely together once your application loads htmx.

The components pass `hx-*` attributes through like any other attribute.
They are built for it: dialogs, menus, popovers and toasts keep working
when htmx swaps them in or out, and response headers such as `HX-Trigger` can close a dialog or show a toast.

Using htmx 4 is recommended, but Shadleaf also works with htmx 2: see [Still using htmx 2?](#still-using-htmx-2).

## Loading htmx

Loading htmx is easiest done as a [webjar](https://www.webjars.org/):

```xml title="pom.xml"
<dependency>
  <groupId>org.webjars</groupId>
  <artifactId>webjars-locator-lite</artifactId>
</dependency>
<dependency>
  <groupId>org.webjars.npm</groupId>
  <artifactId>htmx.org</artifactId>
  <version>4.0.2</version>
</dependency>
```

> **Tip**
>
> If you want to learn more about webjars, check out [Using Webjars with Thymeleaf](https://www.youtube.com/watch?v=84zmvoSg7dc).

Optionally, you can use [htmx-spring-boot](https://github.com/wimdeblauwe/htmx-spring-boot) which brings some nice
quality-of-life improvements when working with htmx and Thymeleaf:

```xml title="pom.xml"
<dependency>
  <groupId>io.github.wimdeblauwe</groupId>
  <artifactId>htmx-spring-boot-thymeleaf</artifactId>
  <version>6.0.1</version>
</dependency>
```

With the dependencies added in the `pom.xml`, you can now load htmx like this:

```html
<head>
  ...
  <meta name="htmx-config" content='{"includeIndicatorCSS": false}'>
  <script th:src="@{/webjars/htmx.org/dist/htmx.min.js}" defer></script>
  <script th:src="@{/webjars/htmx.org/dist/ext/hx-alpine-compat.min.js}" defer></script>
  <th:block th:replace="~{sl/layout :: assets}"></th:block>
</head>
```

Some important points:

- Shadleaf's components use Alpine, so be sure to also load the `hx-alpine-compat` extension, which is included in the htmx webjar.
Without it, an htmx swap resets attributes the components set, such as `aria-expanded` on a menu's trigger.
- The htmx scripts must load before the `sl/layout :: assets` because that contains the AlpineJS script.
- `webjars-locator-lite` lets the script URLs leave out the version: Spring Boot adds it (`/webjars/htmx.org/4.0.2/...`).
Without it, write the version in the URL yourself.
- If you need a strict [CSP](https://shadleaf.dev/1.2.0-SNAPSHOT/guides/csp.md), disable the indicator CSS via `includeIndicatorCSS`.
Setting this to `false` avoids the inline `<style>` that htmx adds by default.
- When the application is secured (with [Spring Security](https://shadleaf.dev/1.2.0-SNAPSHOT/guides/spring-security.md) for example), permit `/webjars/**` so htmx can be loaded on the login page as well.

## Next

- [Forms with htmx](https://shadleaf.dev/1.2.0-SNAPSHOT/guides/forms-htmx.md): post a form, get it back with its errors, and where the focus goes; also
the request headers, history, dialogs, menus, toasts and swapping.
- [Tables with htmx](https://shadleaf.dev/1.2.0-SNAPSHOT/guides/tables-htmx.md): search as you type, sort and page a server-side table, row actions, load more.
- [Application shell](https://shadleaf.dev/1.2.0-SNAPSHOT/components/application-shell.md#with-htmx): `hx-boost` on the body, and the check a
controller then needs to tell a page from a fragment.

## Still using htmx 2?

Shadleaf works with htmx 2 too.
Load it from its webjar and combine it with htmx-spring-boot 5.x:

```xml
<dependency>
  <groupId>org.webjars</groupId>
  <artifactId>webjars-locator-lite</artifactId>
</dependency>
<dependency>
  <groupId>org.webjars.npm</groupId>
  <artifactId>htmx.org</artifactId>
  <version>2.0.11</version>
</dependency>
<dependency>
  <groupId>io.github.wimdeblauwe</groupId>
  <artifactId>htmx-spring-boot-thymeleaf</artifactId>
  <version>5.2.0</version> <!-- 5.x is for htmx 2.x -->
</dependency>
```

The `<head>` snippet is also slightly different compared to using htmx 4:

```html
<head>
  ...
  <th:block th:replace="~{sl/layout :: assets}"></th:block>
  <meta name="htmx-config" content='{"includeIndicatorStyles": false, "allowEval": false}'>
  <script th:src="@{/webjars/htmx.org/dist/htmx.min.js}" defer></script>
</head>
```
Take note:

- **The config has other names.** htmx 2's `includeIndicatorStyles` is htmx 4's `includeIndicatorCSS` (each ignores
the other's name), and `allowEval: false` turns off `hx-on` handlers and `js:` values, which htmx 4 has no setting
for.
- **No Alpine extension.** `hx-alpine-compat` is htmx 4's; leave it out, and htmx can come after the `assets`
fragment.
- Use htmx-spring-boot 5.x. Version 6.x is for htmx 4.x.

The snippets in the docs are htmx 4.
Some of those snippets need to be tweaked if you use htmx 2.

Below are the differences that matter for Shadleaf.
Important differences of htmx itself can be seen at [What's new in htmx 4](https://four.htmx.org/docs/whats-new-in-htmx-4).

### Inheritance

htmx 2 inherits `hx-boost`, `hx-target`, `hx-select` and `hx-swap` from any ancestor, without `:inherited`.
So you write `hx-boost="true"` instead of `hx-boost:inherited="true"`.
To drop an inherited `hx-select` on one element, htmx 2 has `hx-select="unset"`.

### Request headers

|                                   | htmx 2                                   | htmx 4                                                          |
|-----------------------------------|------------------------------------------|-----------------------------------------------------------------|
| The element that sent the request | `HX-Trigger` (its id), `HX-Trigger-Name` | `HX-Source`, as `tag#id` (`a#sort-name`)                        |
| The target                        | `HX-Target` (its id; none without an id) | `HX-Target`, as `tag#id`, always (`div#people-results`, `body`) |
| Full page or part                 |                                          | `HX-Request-Type`: `full` or `partial`                          |

With htmx-spring-boot 5, `getTriggerId()` and `getTriggerName()` take the place of `getSource()`
(`@HxRequest(triggerId = ..., triggerName = ...)`), and `getTarget()` returns `people-results`, not
`div#people-results`. A link boosted from a `<body>` without an id sends no `HX-Target`, so `getTarget() == null`
tells it apart; the [fragment check](https://shadleaf.dev/1.2.0-SNAPSHOT/guides/forms-htmx.md#fragment-or-whole-page) works with both.

### History

htmx 2 keeps a snapshot of the page in `localStorage` for each address it pushes (`hx-push-url`, `hx-boost`), and puts
it back on the back button. A failed form keeps the user's input in its `value` attributes there: for a form with
personal data, set `"historyCacheSize": 0` in the htmx config, or `hx-history="false"` on the page, which makes htmx
ask the server again (`HX-History-Restore-Request`). A search field shows the value of the first load in a snapshot,
so [Tables with htmx](https://shadleaf.dev/1.2.0-SNAPSHOT/guides/tables-htmx.md) puts `hx-history="false"` on its search form there. Before htmx 2 saves a
snapshot, Shadleaf closes open dialogs and removes the toasts, so back never shows one.

### Response headers and events

htmx 2 also has `HX-Trigger-After-Swap` and `HX-Trigger-After-Settle` (`addTriggerAfterSwap()` and
`addTriggerAfterSettle()` in htmx-spring-boot 5), whose events are lost when the swap removed the element that sent
the request: use `HX-Trigger`. htmx 2 dispatches the events of `HX-Trigger` before the swap, where htmx 4 does it
after. `HX-Redirect`, `HX-Location`, `HX-Refresh`, `HX-Push-Url`, `HX-Replace-Url`, `HX-Retarget`, `HX-Reswap` and
`HX-Reselect` are the same in both.

### Swapping

- **htmx 2 does not swap `4xx` and `5xx` answers** unless a `responseHandling` entry says so (htmx 4 has no
`responseHandling`).
- **A response with only out-of-band content empties the target** in htmx 2, so `swapEmpty:true` is not needed there.
- **`hx-delete` includes the enclosing form** in htmx 2.
- **htmx 2's `hx-disabled-elt` is htmx 4's `hx-disable`**, and htmx 2's `hx-disable` (leave this element alone) is
htmx 4's `hx-ignore`.
- **Validating as you type is htmx 4 only.** [`<sl:form validate>`](https://shadleaf.dev/1.2.0-SNAPSHOT/components/form.md#validating-as-you-type)
swaps with `outerMorph` and `<hx-partial>`, which htmx 2 does not have.
