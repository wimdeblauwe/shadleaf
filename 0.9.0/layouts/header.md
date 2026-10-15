# Navigation in the header

> For an application with a handful of pages, the pages as a row of links in the site header and the page as a card below it; on a phone the same navigation is a panel.

Source: https://wimdeblauwe.github.io/shadleaf/0.9.0/layouts/header/

For an application with a handful of pages. The [site header](https://wimdeblauwe.github.io/shadleaf/0.9.0/components/site-header.md) holds the menu button
(on a phone), the application's name, the pages as a row of links, and the theme toggle and the
[user menu](https://wimdeblauwe.github.io/shadleaf/0.9.0/components/user-menu.md) at the end. The page is a card below the header (`variant="inset"`).

The row is a `<sl:sidebar placement="header">`. On a phone the menu button opens that same navigation as a panel,
with the icons and the parts marked `panel-only`: here the panel's own header and a More group (Settings, Help).
Switch the preview to Phone to see it.

**Example: Navigation in the header.** For an application with a handful of pages: the pages as a row of links in the site header, the page as a card below it, the theme toggle and the signed-in user's menu at the end. On a phone the menu button opens the same navigation as a panel, with its icons and the panel-only parts.

For reference: Flux's [header layout](https://fluxui.dev/layouts/header) this layout is based on.

## The layout

The whole layout file, with what [every layout needs](https://wimdeblauwe.github.io/shadleaf/0.9.0/components/application-shell.md#what-every-layout-needs). It is
the layout of the second sample application (sample-02's `layout/main.html`, which runs under a strict
Content-Security-Policy). Sample-02 also has an Admin link for admins only, left out here: see
[Navigation by role](https://wimdeblauwe.github.io/shadleaf/0.9.0/components/application-shell.md#navigation-by-role).

```html
<!DOCTYPE html>
<html lang="en" xmlns:th="http://www.thymeleaf.org" xmlns:sl="https://shadleaf.dev/sl"
      th:fragment="layout(pageTitle, content)">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title th:text="${pageTitle}">Page</title>
  <th:block th:replace="~{sl/layout :: theme-script}"></th:block>
  <th:block th:replace="~{sl/layout :: assets}"></th:block>
  <link rel="stylesheet" th:href="@{/css/application.css}">
</head>
<body>
<sl:skip-link/>
<sl:sidebar-provider>
  <sl:site-header>
    <sl:sidebar-trigger aria-label="Menu"><sl:icon name="menu"/></sl:sidebar-trigger>
    <sl:button as="a" th:href="@{/}" variant="ghost">
      <sl:slot name="icon-start"><sl:icon name="leaf"/></sl:slot>
      Acme
    </sl:button>
    <sl:sidebar placement="header" variant="inset">
      <sl:sidebar-header panel-only>
        <sl:sidebar-menu>
          <sl:sidebar-menu-item>
            <sl:sidebar-menu-button th:href="@{/}" size="lg">
              <sl:slot name="icon-start"><span class="app-logo"><sl:icon name="leaf"/></span></sl:slot>
              <strong>Acme</strong>
              <small>Enterprise</small>
            </sl:sidebar-menu-button>
          </sl:sidebar-menu-item>
        </sl:sidebar-menu>
      </sl:sidebar-header>
      <sl:sidebar-content>
        <sl:sidebar-group>
          <sl:sidebar-group-content>
            <sl:sidebar-menu>
              <sl:sidebar-menu-item>
                <sl:sidebar-menu-button th:href="@{/}" th:active="${#slNav.current('/')}">
                  <sl:slot name="icon-start"><sl:icon name="house"/></sl:slot>
                  Home
                </sl:sidebar-menu-button>
              </sl:sidebar-menu-item>
              <sl:sidebar-menu-item>
                <sl:sidebar-menu-button th:href="@{/forms}" th:active="${#slNav.current('/forms')}">
                  <sl:slot name="icon-start"><sl:icon name="text-cursor-input"/></sl:slot>
                  Forms
                </sl:sidebar-menu-button>
              </sl:sidebar-menu-item>
              <sl:sidebar-menu-item>
                <sl:sidebar-menu-button th:href="@{/overlays}" th:active="${#slNav.current('/overlays')}">
                  <sl:slot name="icon-start"><sl:icon name="layers"/></sl:slot>
                  Overlays
                </sl:sidebar-menu-button>
              </sl:sidebar-menu-item>
              <sl:sidebar-menu-item>
                <sl:sidebar-menu-button th:href="@{/data}" th:active="${#slNav.current('/data')}">
                  <sl:slot name="icon-start"><sl:icon name="table"/></sl:slot>
                  Data
                </sl:sidebar-menu-button>
              </sl:sidebar-menu-item>
            </sl:sidebar-menu>
          </sl:sidebar-group-content>
        </sl:sidebar-group>
        <sl:sidebar-group id="nav-more" panel-only>
          <sl:sidebar-group-label>More</sl:sidebar-group-label>
          <sl:sidebar-group-content>
            <sl:sidebar-menu>
              <sl:sidebar-menu-item>
                <sl:sidebar-menu-button th:href="@{/settings}" th:active="${#slNav.current('/settings')}">
                  <sl:slot name="icon-start"><sl:icon name="settings"/></sl:slot>
                  Settings
                </sl:sidebar-menu-button>
              </sl:sidebar-menu-item>
              <sl:sidebar-menu-item>
                <sl:sidebar-menu-button th:href="@{/help}" th:active="${#slNav.current('/help')}">
                  <sl:slot name="icon-start"><sl:icon name="life-buoy"/></sl:slot>
                  Help
                </sl:sidebar-menu-button>
              </sl:sidebar-menu-item>
            </sl:sidebar-menu>
          </sl:sidebar-group-content>
        </sl:sidebar-group>
      </sl:sidebar-content>
    </sl:sidebar>
    <sl:slot name="end">
      <sl:theme-toggle/>
      <sl:user-menu placement="header">
        <sl:dropdown-menu-item as="a" th:href="@{/settings}"><sl:icon name="settings"/>Settings</sl:dropdown-menu-item>
      </sl:user-menu>
    </sl:slot>
  </sl:site-header>
  <sl:sidebar-inset>
    <div class="page">
      <h1 th:text="${pageTitle}"></h1>
      <div th:replace="${content}"></div>
    </div>
  </sl:sidebar-inset>
</sl:sidebar-provider>
<div id="modal-root"></div>
<sl:toaster>
  <sl:toast th:each="toast : ${toasts}" th:title="${toast.title}" th:description="${toast.description}"
            th:variant="${toast.variant}" th:duration="${toast.duration}"/>
</sl:toaster>
</body>
</html>
```

In the row, the links show their labels without icons and the current page is marked as in a sidebar; group actions,
separators and collapsible items are left out of the row, and the menu button is hidden on a desktop. When the links do
not fit, the row scrolls sideways. See [In the header](https://wimdeblauwe.github.io/shadleaf/0.9.0/components/sidebar.md#in-the-header) and
[Panel-only parts](https://wimdeblauwe.github.io/shadleaf/0.9.0/components/sidebar.md#panel-only-parts).

## A page

A page passes its title and its content:

```html
<html xmlns:th="http://www.thymeleaf.org" xmlns:sl="https://shadleaf.dev/sl"
      th:replace="~{layout/main :: layout('Forms', ~{::#page-content})}">
<body>
<div id="page-content">
  ...
</div>
</body>
</html>
```

## The CSS

The square behind the logo in the panel's header, and the page in the card, kept to a readable width:

```css
/* The square behind the logo in the panel's header. */
.app-logo {
  display: grid;
  place-items: center;
  flex-shrink: 0;
  inline-size: 2rem;
  block-size: 2rem;
  border-radius: calc(var(--radius) - 2px);
  background: var(--sidebar-primary);
  color: var(--sidebar-primary-foreground);
}
.app-logo > svg { inline-size: 1rem; block-size: 1rem; }

/* The page in the card, kept to a readable width. */
.page { max-width: 48rem; padding: 1.5rem 1rem 2rem; }
.page > h1 { margin-block-end: 1rem; font-size: 1.75rem; font-weight: 600; }
```
