# Tabs

> Panels of content shown one at a time, chosen from a row of tabs.

Source: https://shadleaf.dev/1.0.0/components/tabs/

**Example: Tabs.** The arrow keys move between the tabs; Tab moves on to the panel.

```html
<sl:tabs id="tabs-demo" active="account">
  <sl:tabs-list>
    <sl:tabs-trigger tab="account">Account</sl:tabs-trigger>
    <sl:tabs-trigger tab="password">Password</sl:tabs-trigger>
  </sl:tabs-list>
  <sl:tabs-content tab="account">
    <sl:card>
      <sl:card-header>
        <sl:card-title level="2">Account</sl:card-title>
        <sl:card-description>Make changes to your account here. Click save when you're done.</sl:card-description>
      </sl:card-header>
      <sl:card-content>
        <sl:field-group>
          <sl:field for="tabs-demo-name">
            <sl:field-label>Name</sl:field-label>
            <sl:input value="Pedro Duarte"/>
          </sl:field>
          <sl:field for="tabs-demo-username">
            <sl:field-label>Username</sl:field-label>
            <sl:input value="@peduarte"/>
          </sl:field>
        </sl:field-group>
      </sl:card-content>
      <sl:card-footer><sl:button>Save changes</sl:button></sl:card-footer>
    </sl:card>
  </sl:tabs-content>
  <sl:tabs-content tab="password">
    <sl:card>
      <sl:card-header>
        <sl:card-title level="2">Password</sl:card-title>
        <sl:card-description>Change your password here. After saving, you'll be logged out.</sl:card-description>
      </sl:card-header>
      <sl:card-content>
        <sl:field-group>
          <sl:field for="tabs-demo-current">
            <sl:field-label>Current password</sl:field-label>
            <sl:input type="password"/>
          </sl:field>
          <sl:field for="tabs-demo-new">
            <sl:field-label>New password</sl:field-label>
            <sl:input type="password"/>
          </sl:field>
        </sl:field-group>
      </sl:card-content>
      <sl:card-footer><sl:button>Save password</sl:button></sl:card-footer>
    </sl:card>
  </sl:tabs-content>
</sl:tabs>
```

## Built from parts

Tabs are `<sl:tabs>` with an `<sl:tabs-list>` of `<sl:tabs-trigger>`s and an `<sl:tabs-content>` per tab, as in
shadcn/ui. A trigger and its panel share a `tab`, and `active` on `<sl:tabs>` names the one that shows:

```html
<sl:tabs id="settings" active="account">
  <sl:tabs-list aria-label="Settings">
    <sl:tabs-trigger tab="account">Account</sl:tabs-trigger>
    <sl:tabs-trigger tab="password">Password</sl:tabs-trigger>
  </sl:tabs-list>
  <sl:tabs-content tab="account">…</sl:tabs-content>
  <sl:tabs-content tab="password">…</sl:tabs-content>
</sl:tabs>
```

shadcn/ui calls these `value` and `defaultValue`; `value` is taken in Thymeleaf (`th:value` is Spring's form value). The
tabs need an `id`: the triggers and panels get ids derived from it and their `tab` (`settings-account-trigger`,
`settings-account-content`), which link each tab to its panel (`aria-controls`, `aria-labelledby`). So a `tab` is
letters, digits and dashes.

**The server renders the active tab's panel and hides the others** (`hidden`), so the page is right before any script
runs. The Alpine component `slTabs` then switches panels in the browser and adds the keyboard of the
[ARIA tabs pattern](https://www.w3.org/WAI/ARIA/apg/patterns/tabs/). An `aria-label` on the list names it, which helps
when a page has more than one.

## Tabs in the address

A trigger with an `href` is a link. Without JavaScript (or before Alpine starts) it loads the page with that tab
active, so every panel is reachable; with Alpine the tab switches in place, and the page's address becomes the link's
(`history.replaceState`), so a reload or a shared link opens the same tab. A click with Ctrl, Cmd or Shift still opens
the link in a new tab or window. The controller reads the tab from the request:

```html
<sl:tabs id="settings" th:active="${tab}">
  <sl:tabs-list aria-label="Settings">
    <sl:tabs-trigger tab="account" th:href="@{/settings(tab=account)}">Account</sl:tabs-trigger>
    <sl:tabs-trigger tab="password" th:href="@{/settings(tab=password)}">Password</sl:tabs-trigger>
  </sl:tabs-list>
  …
</sl:tabs>
```

```java
@GetMapping("/settings")
public String settings(@RequestParam(defaultValue = "account") String tab, Model model) {
  model.addAttribute("tab", TABS.contains(tab) ? tab : "account");
  return "settings";
}
```

Triggers without an `href` are buttons, which need Alpine: without it only the active panel can be seen. Use links for
tabs that must work without JavaScript.

**Example: Links.** Triggers with an href load the page with that tab active when there is no JavaScript; with it, the tab switches in place and the address follows.

```html
<sl:tabs id="tabs-links" active="week" activation-mode="manual">
  <sl:tabs-list aria-label="Period">
    <sl:tabs-trigger tab="day" href="?period=day">Day</sl:tabs-trigger>
    <sl:tabs-trigger tab="week" href="?period=week">Week</sl:tabs-trigger>
    <sl:tabs-trigger tab="month" href="?period=month">Month</sl:tabs-trigger>
  </sl:tabs-list>
  <sl:tabs-content tab="day">14 orders today.</sl:tabs-content>
  <sl:tabs-content tab="week">86 orders this week.</sl:tabs-content>
  <sl:tabs-content tab="month">312 orders this month.</sl:tabs-content>
</sl:tabs>
```

## Keyboard

Tab moves into the list on the active tab, and out of it to the active panel (which takes the focus, as in Radix, so a
panel without controls can be scrolled and read). In the list:

| Key | Does |
| --- | --- |
| Left and Right arrow | The previous and next tab, wrapping around; they swap on a right-to-left page. Up and Down with `orientation="vertical"`. |
| Home, End | The first and last tab. |
| Enter, Space | Shows the focused tab's panel (for `activation-mode="manual"`). |

Disabled tabs are skipped. With `activation-mode="automatic"` (the default) a tab shows its panel as soon as the
arrow keys reach it. `manual` only moves the focus, and Enter or Space shows the panel: for panels that take a request
to show, such as ones loaded with htmx, so that arrowing past a tab does not load it.

## Line

`variant="line"` on the list draws no track, and a line under the active tab:

**Example: Line.**

```html
<sl:tabs id="tabs-line" active="overview">
  <sl:tabs-list variant="line" aria-label="Report">
    <sl:tabs-trigger tab="overview">Overview</sl:tabs-trigger>
    <sl:tabs-trigger tab="analytics">Analytics</sl:tabs-trigger>
    <sl:tabs-trigger tab="reports">Reports</sl:tabs-trigger>
  </sl:tabs-list>
  <sl:tabs-content tab="overview">Sales are up 12% on last month.</sl:tabs-content>
  <sl:tabs-content tab="analytics">1,204 visitors this week.</sl:tabs-content>
  <sl:tabs-content tab="reports">Three reports are ready to download.</sl:tabs-content>
</sl:tabs>
```

## Vertical

`orientation="vertical"` puts the tabs in a column next to the panels:

**Example: Vertical.** orientation="vertical" puts the tabs in a column; the up and down arrow keys move between them.

```html
<sl:tabs id="tabs-vertical" active="general" orientation="vertical">
  <sl:tabs-list aria-label="Preferences">
    <sl:tabs-trigger tab="general">General</sl:tabs-trigger>
    <sl:tabs-trigger tab="appearance">Appearance</sl:tabs-trigger>
    <sl:tabs-trigger tab="advanced">Advanced</sl:tabs-trigger>
  </sl:tabs-list>
  <sl:tabs-content tab="general">Language, time zone and start page.</sl:tabs-content>
  <sl:tabs-content tab="appearance">Theme and density.</sl:tabs-content>
  <sl:tabs-content tab="advanced">Developer options.</sl:tabs-content>
</sl:tabs>
```

## Icons and disabled tabs

**Example: Icons and a disabled tab.** The arrow keys skip a disabled tab.

```html
<sl:tabs id="tabs-icons" active="preview">
  <sl:tabs-list aria-label="View">
    <sl:tabs-trigger tab="preview"><sl:icon name="app-window"/> Preview</sl:tabs-trigger>
    <sl:tabs-trigger tab="code"><sl:icon name="code"/> Code</sl:tabs-trigger>
    <sl:tabs-trigger tab="history" disabled><sl:icon name="history"/> History</sl:tabs-trigger>
  </sl:tabs-list>
  <sl:tabs-content tab="preview">The rendered page.</sl:tabs-content>
  <sl:tabs-content tab="code">The template.</sl:tabs-content>
  <sl:tabs-content tab="history">Earlier versions.</sl:tabs-content>
</sl:tabs>
```

A disabled button tab gets `disabled`; a disabled link tab loses its `href` and gets `aria-disabled`.

## Find in page

Once Alpine runs, the inactive panels are `hidden="until-found"` instead of `hidden`: the browser's find-in-page, and
a link to an element inside a panel (`#billing-address`), find their text, and the tab of the panel with the match
becomes active (the `beforematch` event). This works in Chrome and Edge 102, Firefox 148 and Safari 26.2 (Safari and
Firefox 139 to 147 do not scroll to the match yet). The server renders plain `hidden`: without Alpine a match would
show the panel under a tab that stays inactive.

## With htmx

**A panel that loads on first view.** Each time a panel is shown, `slTabs` fires `sl-tabs-show` on it, so htmx can
fetch its content the first time:

```html
<sl:tabs id="settings" th:active="${tab}" activation-mode="manual">
  <sl:tabs-list aria-label="Settings">
    <sl:tabs-trigger tab="account" th:href="@{/settings(tab=account)}">Account</sl:tabs-trigger>
    <sl:tabs-trigger tab="sessions" th:href="@{/settings(tab=sessions)}">Sessions</sl:tabs-trigger>
  </sl:tabs-list>
  <sl:tabs-content tab="account" th:insert="~{settings :: account}"/>
  <sl:tabs-content tab="sessions" th:hx-get="${tab != 'sessions'} ? @{/settings/sessions}" hx-trigger="sl-tabs-show once">
    <th:block th:if="${tab == 'sessions'}" th:insert="~{settings :: sessions}"/>
  </sl:tabs-content>
</sl:tabs>
```

The server renders the active panel's content itself: `sl-tabs-show` is only fired when a panel is shown in the
browser, not for the one that is active when the page loads. The event does not bubble, so a panel's `hx-trigger`
never reacts to the tabs nested in it. `activation-mode="manual"` keeps the arrow keys from loading every panel on
the way.

**A swap that replaces the tabs** shows the tab the new markup has active: send the `active` the user had (for
example with `hx-vals` or `hx-include`) if it should stay.

**History.** htmx saves the page's markup as it is when it navigates away, with the tab the user chose and any panel
content loaded since; back restores exactly that. With link triggers the address has the tab too.

## Left out

- **`value`/`onValueChange` and controlled state**: the server renders the active tab; the `sl-tabs-show` event
  reports a switch.
- **`dir`**: the arrow keys follow the page's direction (`dir` on an ancestor or the page).
- **`loop`**: the arrow keys always wrap around, as Radix's do by default.
- **`forceMount`**: every panel is always in the page; the inactive ones are hidden.

## Attributes

### sl:tabs

Attributes of `<sl:tabs>`: Panels of content shown one at a time, chosen from a row of tabs. Holds a `sl:tabs-list` of `sl:tabs-trigger`s and a `sl:tabs-content` per tab. The server renders the active tab's panel and hides the others, so the page is right before any script runs; Alpine adds switching in the browser and the keyboard (the ARIA tabs pattern). Give the triggers an `href` and the tabs work without JavaScript too.

| Attribute | Values | Default | Description |
| --- | --- | --- | --- |
| `id` | *string* | **required** | The id of the tabs. The ids of the triggers and panels derive from it (`settings-account-trigger`, `settings-account-content`). |
| `active` | *string* | **required** | The `tab` of the trigger and panel that are active when the page renders, e.g. from a request parameter: `th:active="${tab}"`. |
| `orientation` | `horizontal`, `vertical` | `horizontal` | `vertical` puts the tabs in a column next to the panels, and the up and down arrow keys move between them. |
| `activation-mode` | `automatic`, `manual` | `automatic` | `automatic`: a tab shows its panel as soon as the arrow keys reach it. `manual`: the arrow keys only move the focus, and Enter or Space shows the panel; for panels that are slow to show, such as ones loaded with htmx. |

Every prop can also be set from an expression: `th:id="${...}"`. Any other attribute passes through to the rendered element, and `class` is appended.

### sl:tabs-list

Attributes of `<sl:tabs-list>`: The row (or column) of `sl:tabs-trigger`s of a `sl:tabs`: a `tablist`. An `aria-label` names it when the page has more than one.

| Attribute | Values | Default | Description |
| --- | --- | --- | --- |
| `variant` | `default`, `line` | `default` | `default`: the tabs sit on a muted track and the active one is raised. `line`: no track, a line under the active tab. |

Every prop can also be set from an expression: `th:variant="${...}"`. Any other attribute passes through to the rendered element, and `class` is appended.

### sl:tabs-trigger

Attributes of `<sl:tabs-trigger>`: A tab in a `sl:tabs-list`, which shows the `sl:tabs-content` with the same `tab`. A button, or with an `href` a link to the page with this tab active: without JavaScript the link loads it, with Alpine the tab switches in place and the address follows.

| Attribute | Values | Default | Description |
| --- | --- | --- | --- |
| `tab` | *string* | **required** | Which tab this is: the `tab` of its `sl:tabs-content`, and the value `sl:tabs`' `active` names. Part of the ids, so letters, digits and dashes. |
| `disabled` | *boolean* | — | The tab cannot be chosen; the arrow keys skip it. A disabled link loses its `href`. |

Every prop can also be set from an expression: `th:tab="${...}"`. Any other attribute passes through to the rendered element, and `class` is appended.

### sl:tabs-content

Attributes of `<sl:tabs-content>`: The panel of one tab of a `sl:tabs`, labelled by its trigger. Only the active tab's panel shows; the others are `hidden`. It takes the focus with Tab after the tabs (as in Radix), so a panel without controls can be read and scrolled.

| Attribute | Values | Default | Description |
| --- | --- | --- | --- |
| `tab` | *string* | **required** | Which tab's panel this is: the `tab` of its `sl:tabs-trigger`. |

Every prop can also be set from an expression: `th:tab="${...}"`. Any other attribute passes through to the rendered element, and `class` is appended.

## JavaScript

A small Alpine component, `slTabs`, is on `<sl:tabs>`: it renders as
`<div class="tabs" x-data="slTabs">`.

- **Without Alpine** the active panel shows, and link triggers load the page with another tab active. Button triggers
  need Alpine. Alpine is included by default; see [Alpine](https://shadleaf.dev/1.0.0/guides/alpine.md).
- **It needs no Alpine plugin.** If you load Alpine yourself (`shadleaf.assets.alpine=external`), plain Alpine is
  enough.
- **Keep your own `x-data` off `<sl:tabs>`.** An element has one `x-data`; put yours on an element inside.

| Event | On | Effect |
| --- | --- | --- |
| `sl-tabs-show` | a panel (it does not bubble) | Fired when the panel is shown in the browser: a click, the keyboard, or find-in-page. |
