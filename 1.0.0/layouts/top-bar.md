# Sidebar and top bar

> A sidebar that is always there on a desktop, with the pages, the user's teams and Settings, and a bar across the top with a search field, the notifications and the signed-in user's menu.

Source: https://shadleaf.dev/1.0.0/layouts/top-bar/

A sidebar for an application that keeps its navigation in view: the pages with their icons, a "Your teams" group with
each team's initial where a page has its icon, and Settings at the bottom (`<sl:sidebar-footer>`). It never collapses
on a desktop (`collapsible="none"`), so there is no trigger there. Across the top of the page, a
[site header](https://shadleaf.dev/1.0.0/components/site-header.md) that stays in view while the page scrolls: a search field that takes the
free room, a notifications button and the [user menu](https://shadleaf.dev/1.0.0/components/user-menu.md). On a phone the menu button at the
start of the bar opens the sidebar as a panel. Switch the preview to Phone to see it.

**Example: Sidebar and top bar.** A sidebar that is always there on a desktop, with the pages, the user's teams and Settings at the bottom, and a bar across the top of the page with a search field, the notifications and the signed-in user's menu. On a phone the menu button in the bar opens the sidebar as a panel.

## The layout

The whole layout file. It has what [every layout needs](https://shadleaf.dev/1.0.0/components/application-shell.md#what-every-layout-needs).
`#slNav.current` marks the current page from the request.

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
  <sl:sidebar collapsible="none">
    <sl:sidebar-header>
      <sl:sidebar-menu>
        <sl:sidebar-menu-item>
          <sl:sidebar-menu-button th:href="@{/}" size="lg">
            <sl:slot name="icon-start"><span class="app-logo"><sl:icon name="leaf"/></span></sl:slot>
            <strong>Acme</strong>
          </sl:sidebar-menu-button>
        </sl:sidebar-menu-item>
      </sl:sidebar-menu>
    </sl:sidebar-header>
    <sl:sidebar-content>
      <sl:sidebar-group id="nav-main">
        <sl:sidebar-group-content>
          <sl:sidebar-menu>
            <sl:sidebar-menu-item>
              <sl:sidebar-menu-button th:href="@{/}" th:active="${#slNav.current('/')}">
                <sl:slot name="icon-start"><sl:icon name="house"/></sl:slot>
                Dashboard
              </sl:sidebar-menu-button>
            </sl:sidebar-menu-item>
            <sl:sidebar-menu-item>
              <sl:sidebar-menu-button th:href="@{/users}" th:active="${#slNav.current('/users')}">
                <sl:slot name="icon-start"><sl:icon name="user"/></sl:slot>
                Users
              </sl:sidebar-menu-button>
            </sl:sidebar-menu-item>
            <sl:sidebar-menu-item>
              <sl:sidebar-menu-button th:href="@{/teams}" th:active="${#slNav.current('/teams')}">
                <sl:slot name="icon-start"><sl:icon name="users"/></sl:slot>
                Teams
              </sl:sidebar-menu-button>
            </sl:sidebar-menu-item>
            <sl:sidebar-menu-item>
              <sl:sidebar-menu-button th:href="@{/calendar}" th:active="${#slNav.current('/calendar')}">
                <sl:slot name="icon-start"><sl:icon name="calendar"/></sl:slot>
                Calendar
              </sl:sidebar-menu-button>
            </sl:sidebar-menu-item>
            <sl:sidebar-menu-item>
              <sl:sidebar-menu-button th:href="@{/documents}" th:active="${#slNav.current('/documents')}">
                <sl:slot name="icon-start"><sl:icon name="files"/></sl:slot>
                Documents
              </sl:sidebar-menu-button>
            </sl:sidebar-menu-item>
            <sl:sidebar-menu-item>
              <sl:sidebar-menu-button th:href="@{/reports}" th:active="${#slNav.current('/reports')}">
                <sl:slot name="icon-start"><sl:icon name="chart-pie"/></sl:slot>
                Reports
              </sl:sidebar-menu-button>
            </sl:sidebar-menu-item>
          </sl:sidebar-menu>
        </sl:sidebar-group-content>
      </sl:sidebar-group>
      <sl:sidebar-group id="nav-teams">
        <sl:sidebar-group-label>Your teams</sl:sidebar-group-label>
        <sl:sidebar-group-content>
          <sl:sidebar-menu>
            <sl:sidebar-menu-item>
              <sl:sidebar-menu-button th:href="@{/teams/engineering}" th:active="${#slNav.current('/teams/engineering')}">
                <sl:slot name="icon-start"><span class="team-initial" aria-hidden="true">E</span></sl:slot>
                Engineering
              </sl:sidebar-menu-button>
            </sl:sidebar-menu-item>
            <sl:sidebar-menu-item>
              <sl:sidebar-menu-button th:href="@{/teams/marketing}" th:active="${#slNav.current('/teams/marketing')}">
                <sl:slot name="icon-start"><span class="team-initial" aria-hidden="true">M</span></sl:slot>
                Marketing
              </sl:sidebar-menu-button>
            </sl:sidebar-menu-item>
            <sl:sidebar-menu-item>
              <sl:sidebar-menu-button th:href="@{/teams/sales}" th:active="${#slNav.current('/teams/sales')}">
                <sl:slot name="icon-start"><span class="team-initial" aria-hidden="true">S</span></sl:slot>
                Sales
              </sl:sidebar-menu-button>
            </sl:sidebar-menu-item>
          </sl:sidebar-menu>
        </sl:sidebar-group-content>
      </sl:sidebar-group>
    </sl:sidebar-content>
    <sl:sidebar-footer>
      <sl:sidebar-menu>
        <sl:sidebar-menu-item>
          <sl:sidebar-menu-button th:href="@{/settings}" th:active="${#slNav.current('/settings')}">
            <sl:slot name="icon-start"><sl:icon name="settings"/></sl:slot>
            Settings
          </sl:sidebar-menu-button>
        </sl:sidebar-menu-item>
      </sl:sidebar-menu>
    </sl:sidebar-footer>
  </sl:sidebar>
  <sl:sidebar-inset>
    <sl:site-header>
      <sl:sidebar-trigger aria-label="Menu"><sl:icon name="menu"/></sl:sidebar-trigger>
      <sl:separator orientation="vertical"/>
      <form class="app-search" role="search" th:action="@{/search}">
        <sl:input type="search" name="q" placeholder="Search" aria-label="Search"/>
        <sl:icon name="search"/>
      </form>
      <sl:slot name="end">
        <sl:button as="a" th:href="@{/notifications}" variant="ghost" size="icon" aria-label="Notifications">
          <sl:icon name="bell"/>
        </sl:button>
        <sl:separator orientation="vertical"/>
        <sl:user-menu placement="header">
          <sl:dropdown-menu-item as="a" th:href="@{/profile}"><sl:icon name="user"/>Your profile</sl:dropdown-menu-item>
        </sl:user-menu>
      </sl:slot>
    </sl:site-header>
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

The search field is a plain `GET` form; Shadleaf has no search of its own. The notifications button is a link to a
page here; give it a [badge](https://shadleaf.dev/1.0.0/components/badge.md) or open a [popover](https://shadleaf.dev/1.0.0/components/popover.md) from it as the
application needs. The teams are links; an application lists its own with `th:each` and takes the initial with
`${#strings.substring(team.name, 0, 1)}`.

## A page

A page passes its title and its content:

```html
<html xmlns:th="http://www.thymeleaf.org" xmlns:sl="https://shadleaf.dev/sl"
      th:replace="~{layout/main :: layout('Users', ~{::#page-content})}">
<body>
<div id="page-content">
  ...
</div>
</body>
</html>
```

## The CSS

Shadleaf's CSS styles the components, not the page around them, and has no utility classes. The rest is the
application's CSS, here without Tailwind: the square behind the logo, the teams' initials, the bar (its height, the
border below it and staying in view), the search field without a border until it has focus, and the page's padding.

```css
/* The square behind the logo in the sidebar's header. */
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

/* A team's initial in a small bordered square, where a page has its icon. */
.team-initial {
  display: grid;
  place-items: center;
  flex-shrink: 0;
  inline-size: 1.25rem;
  block-size: 1.25rem;
  border: 1px solid var(--sidebar-border);
  border-radius: calc(var(--radius) - 4px);
  background: var(--background);
  font-size: 0.625rem;
  font-weight: 500;
}

/*
  The bar: as high as the sidebar's header, so the two line up, and it stays at the top while the page scrolls,
  with a border below. The separator after the menu button goes with it, from 768 px; the one before the user's menu
  is a little taller.
*/
.sidebar-inset > .site-header {
  --site-header-height: 4rem;
  position: sticky;
  inset-block-start: 0;
  z-index: 10;
  gap: 1rem;
  border-block-end: 1px solid var(--border);
  background: var(--background);
}
.sidebar-inset > .site-header > .site-header-end > .separator { block-size: 1.5rem; }
@media (width >= 48rem) {
  .sidebar-inset > .site-header > .separator { display: none; }
}

/* The search field takes the bar's free room, a magnifier inside it, without a border until it has focus. */
.app-search { position: relative; flex: 1; }
.app-search > .input { padding-inline-start: 2rem; }
.app-search > .input:not(:focus-visible) { border-color: transparent; box-shadow: none; }
.app-search > svg {
  position: absolute;
  inset-inline-start: 0.5rem;
  inset-block-start: 50%;
  translate: 0 -50%;
  inline-size: 1rem;
  block-size: 1rem;
  opacity: 0.5;
  pointer-events: none;
}

/* The page's padding, more on a wider screen. */
.page { padding: 2rem 1rem; }
.page > h1 { margin-block-end: 1.5rem; font-size: 1.5rem; font-weight: 600; }
@media (width >= 64rem) {
  .page { padding: 2.5rem 2rem; }
}
```
