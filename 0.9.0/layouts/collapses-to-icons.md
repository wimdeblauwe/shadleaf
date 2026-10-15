# Collapses to icons

> A team switcher, collapsible sections and projects beside the page, collapsing to a strip of icons on a desktop, with the page as a card and the signed-in user's menu at the bottom.

Source: https://wimdeblauwe.github.io/shadleaf/0.9.0/layouts/collapses-to-icons/

A sidebar for an application with more pages than fit in a header row. At the top, a team switcher: a
[dropdown menu](https://wimdeblauwe.github.io/shadleaf/0.9.0/components/dropdown-menu.md#opened-from-the-sidebar) opened from a large menu button. Then the
platform's sections as [collapsible items](https://wimdeblauwe.github.io/shadleaf/0.9.0/components/sidebar.md#collapsible-groups), each with its pages in a
sub-menu, the projects with a "More" menu each, and at the bottom the [user menu](https://wimdeblauwe.github.io/shadleaf/0.9.0/components/user-menu.md). The
page is a card beside the sidebar (`variant="inset"`). The page's header is a
[site header](https://wimdeblauwe.github.io/shadleaf/0.9.0/components/site-header.md) with the trigger, a separator, the page's
[breadcrumb](https://wimdeblauwe.github.io/shadleaf/0.9.0/components/breadcrumb.md) and the theme toggle.

On a desktop the trigger (or Ctrl/⌘+B) collapses the sidebar to a strip of icons (`collapsible="icon"`), with a
tooltip on every menu button, and a cookie remembers the choice. The projects have no icons worth a strip of their
own, so their group is left out of it (`icon-hidden`). On a phone the trigger opens the same sidebar as a panel.
Switch the preview to Phone to see it.

**Example: Collapses to icons.** A team switcher at the top, the platform's sections as collapsible items, projects with a menu each, the signed-in user's menu at the bottom, and the page as a card beside it. On a desktop the trigger collapses the sidebar to a strip of icons with tooltips; on a phone it opens it as a panel.

For reference: the [shadcn/ui block](https://ui.shadcn.com/blocks/sidebar#sidebar-07) this layout is based on.

## The layout

The whole layout file. It has what [every layout needs](https://wimdeblauwe.github.io/shadleaf/0.9.0/components/application-shell.md#what-every-layout-needs).
The first sample application (sample-01) has a shorter version of it, with its own pages and no team switcher.
`hx-boost` on the body loads every page with htmx; see [With htmx](https://wimdeblauwe.github.io/shadleaf/0.9.0/components/application-shell.md#with-htmx) for
the one check a controller then needs. Leave out the script and the attribute for a layout without htmx.

```html
<!DOCTYPE html>
<html lang="en" xmlns:th="http://www.thymeleaf.org" xmlns:sl="https://shadleaf.dev/sl"
      th:fragment="layout(pageTitle, breadcrumb, content)">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title th:text="${pageTitle}">Page</title>
  <th:block th:replace="~{sl/layout :: theme-script}"></th:block>
  <th:block th:replace="~{sl/layout :: assets}"></th:block>
  <script th:src="@{/webjars/htmx.org/dist/htmx.min.js}" defer></script>
  <link rel="stylesheet" th:href="@{/css/application.css}">
</head>
<body hx-boost="true">
<sl:skip-link/>
<sl:sidebar-provider>
  <sl:sidebar variant="inset" collapsible="icon">
    <sl:sidebar-header>
      <sl:sidebar-menu>
        <sl:sidebar-menu-item>
          <sl:dropdown-menu id="team-switcher">
            <sl:dropdown-menu-trigger as="sidebar-menu-button" size="lg" tooltip="Acme Inc">
              <sl:slot name="icon-start"><span class="app-logo"><sl:icon name="gallery-vertical-end"/></span></sl:slot>
              <strong>Acme Inc</strong>
              <small>Enterprise</small>
              <sl:slot name="icon-end"><sl:icon name="chevrons-up-down"/></sl:slot>
            </sl:dropdown-menu-trigger>
            <sl:dropdown-menu-content side="right">
              <sl:dropdown-menu-label id="team-switcher-label">Teams</sl:dropdown-menu-label>
              <sl:dropdown-menu-radio-group aria-labelledby="team-switcher-label">
                <sl:dropdown-menu-radio-item as="a" th:href="@{/teams/acme-inc}" checked>Acme Inc</sl:dropdown-menu-radio-item>
                <sl:dropdown-menu-radio-item as="a" th:href="@{/teams/acme-corp}">Acme Corp.</sl:dropdown-menu-radio-item>
                <sl:dropdown-menu-radio-item as="a" th:href="@{/teams/evil-corp}">Evil Corp.</sl:dropdown-menu-radio-item>
              </sl:dropdown-menu-radio-group>
              <sl:dropdown-menu-separator/>
              <sl:dropdown-menu-item as="a" th:href="@{/teams/new}">
                <sl:icon name="plus"/>
                Add team
              </sl:dropdown-menu-item>
            </sl:dropdown-menu-content>
          </sl:dropdown-menu>
        </sl:sidebar-menu-item>
      </sl:sidebar-menu>
    </sl:sidebar-header>
    <sl:sidebar-content>
      <sl:sidebar-group id="nav-platform">
        <sl:sidebar-group-label>Platform</sl:sidebar-group-label>
        <sl:sidebar-menu>
              <sl:sidebar-menu-item collapsible th:open="${#slNav.current('/playground')}">
                <sl:sidebar-menu-button tooltip="Playground">
                  <sl:slot name="icon-start"><sl:icon name="square-terminal"/></sl:slot>
                  Playground
                </sl:sidebar-menu-button>
                <sl:sidebar-menu-sub>
                  <sl:sidebar-menu-sub-item>
                    <sl:sidebar-menu-sub-button th:href="@{/playground/history}" th:active="${#slNav.current('/playground/history')}">
                      History
                    </sl:sidebar-menu-sub-button>
                  </sl:sidebar-menu-sub-item>
                  <sl:sidebar-menu-sub-item>
                    <sl:sidebar-menu-sub-button th:href="@{/playground/starred}" th:active="${#slNav.current('/playground/starred')}">
                      Starred
                    </sl:sidebar-menu-sub-button>
                  </sl:sidebar-menu-sub-item>
                  <sl:sidebar-menu-sub-item>
                    <sl:sidebar-menu-sub-button th:href="@{/playground/settings}" th:active="${#slNav.current('/playground/settings')}">
                      Settings
                    </sl:sidebar-menu-sub-button>
                  </sl:sidebar-menu-sub-item>
                </sl:sidebar-menu-sub>
              </sl:sidebar-menu-item>
              <sl:sidebar-menu-item collapsible th:open="${#slNav.current('/models')}">
                <sl:sidebar-menu-button tooltip="Models">
                  <sl:slot name="icon-start"><sl:icon name="bot"/></sl:slot>
                  Models
                </sl:sidebar-menu-button>
                <sl:sidebar-menu-sub>
                  <sl:sidebar-menu-sub-item>
                    <sl:sidebar-menu-sub-button th:href="@{/models/genesis}" th:active="${#slNav.current('/models/genesis')}">
                      Genesis
                    </sl:sidebar-menu-sub-button>
                  </sl:sidebar-menu-sub-item>
                  <sl:sidebar-menu-sub-item>
                    <sl:sidebar-menu-sub-button th:href="@{/models/explorer}" th:active="${#slNav.current('/models/explorer')}">
                      Explorer
                    </sl:sidebar-menu-sub-button>
                  </sl:sidebar-menu-sub-item>
                  <sl:sidebar-menu-sub-item>
                    <sl:sidebar-menu-sub-button th:href="@{/models/quantum}" th:active="${#slNav.current('/models/quantum')}">
                      Quantum
                    </sl:sidebar-menu-sub-button>
                  </sl:sidebar-menu-sub-item>
                </sl:sidebar-menu-sub>
              </sl:sidebar-menu-item>
              <sl:sidebar-menu-item collapsible th:open="${#slNav.current('/docs')}">
                <sl:sidebar-menu-button tooltip="Documentation">
                  <sl:slot name="icon-start"><sl:icon name="book-open"/></sl:slot>
                  Documentation
                </sl:sidebar-menu-button>
                <sl:sidebar-menu-sub>
                  <sl:sidebar-menu-sub-item>
                    <sl:sidebar-menu-sub-button th:href="@{/docs/introduction}" th:active="${#slNav.current('/docs/introduction')}">
                      Introduction
                    </sl:sidebar-menu-sub-button>
                  </sl:sidebar-menu-sub-item>
                  <sl:sidebar-menu-sub-item>
                    <sl:sidebar-menu-sub-button th:href="@{/docs/get-started}" th:active="${#slNav.current('/docs/get-started')}">
                      Get Started
                    </sl:sidebar-menu-sub-button>
                  </sl:sidebar-menu-sub-item>
                  <sl:sidebar-menu-sub-item>
                    <sl:sidebar-menu-sub-button th:href="@{/docs/tutorials}" th:active="${#slNav.current('/docs/tutorials')}">
                      Tutorials
                    </sl:sidebar-menu-sub-button>
                  </sl:sidebar-menu-sub-item>
                  <sl:sidebar-menu-sub-item>
                    <sl:sidebar-menu-sub-button th:href="@{/docs/changelog}" th:active="${#slNav.current('/docs/changelog')}">
                      Changelog
                    </sl:sidebar-menu-sub-button>
                  </sl:sidebar-menu-sub-item>
                </sl:sidebar-menu-sub>
              </sl:sidebar-menu-item>
              <sl:sidebar-menu-item collapsible th:open="${#slNav.current('/settings')}">
                <sl:sidebar-menu-button tooltip="Settings">
                  <sl:slot name="icon-start"><sl:icon name="settings-2"/></sl:slot>
                  Settings
                </sl:sidebar-menu-button>
                <sl:sidebar-menu-sub>
                  <sl:sidebar-menu-sub-item>
                    <sl:sidebar-menu-sub-button th:href="@{/settings/general}" th:active="${#slNav.current('/settings/general')}">
                      General
                    </sl:sidebar-menu-sub-button>
                  </sl:sidebar-menu-sub-item>
                  <sl:sidebar-menu-sub-item>
                    <sl:sidebar-menu-sub-button th:href="@{/settings/team}" th:active="${#slNav.current('/settings/team')}">
                      Team
                    </sl:sidebar-menu-sub-button>
                  </sl:sidebar-menu-sub-item>
                  <sl:sidebar-menu-sub-item>
                    <sl:sidebar-menu-sub-button th:href="@{/settings/billing}" th:active="${#slNav.current('/settings/billing')}">
                      Billing
                    </sl:sidebar-menu-sub-button>
                  </sl:sidebar-menu-sub-item>
                  <sl:sidebar-menu-sub-item>
                    <sl:sidebar-menu-sub-button th:href="@{/settings/limits}" th:active="${#slNav.current('/settings/limits')}">
                      Limits
                    </sl:sidebar-menu-sub-button>
                  </sl:sidebar-menu-sub-item>
                </sl:sidebar-menu-sub>
              </sl:sidebar-menu-item>
        </sl:sidebar-menu>
      </sl:sidebar-group>
      <sl:sidebar-group id="nav-projects" icon-hidden>
        <sl:sidebar-group-label>Projects</sl:sidebar-group-label>
        <sl:sidebar-menu>
              <sl:sidebar-menu-item>
                <sl:sidebar-menu-button th:href="@{/projects/design}" th:active="${#slNav.current('/projects/design')}">
                  <sl:slot name="icon-start"><sl:icon name="frame"/></sl:slot>
                  Design Engineering
                </sl:sidebar-menu-button>
                <sl:dropdown-menu id="project-design-more">
                  <sl:dropdown-menu-trigger as="sidebar-menu-action" show-on-hover aria-label="More for Design Engineering">
                    <sl:icon name="ellipsis"/>
                  </sl:dropdown-menu-trigger>
                  <sl:dropdown-menu-content side="right">
                    <sl:dropdown-menu-item as="a" th:href="@{/projects/design}">
                      <sl:icon name="folder"/>
                      View project
                    </sl:dropdown-menu-item>
                    <sl:dropdown-menu-item as="a" th:href="@{/projects/design/share}">
                      <sl:icon name="forward"/>
                      Share project
                    </sl:dropdown-menu-item>
                    <sl:dropdown-menu-separator/>
                    <sl:dropdown-menu-item as="a" th:href="@{/projects/design/delete}" variant="destructive">
                      <sl:icon name="trash-2"/>
                      Delete project
                    </sl:dropdown-menu-item>
                  </sl:dropdown-menu-content>
                </sl:dropdown-menu>
              </sl:sidebar-menu-item>
              <sl:sidebar-menu-item>
                <sl:sidebar-menu-button th:href="@{/projects/sales}" th:active="${#slNav.current('/projects/sales')}">
                  <sl:slot name="icon-start"><sl:icon name="chart-pie"/></sl:slot>
                  Sales &amp; Marketing
                </sl:sidebar-menu-button>
                <sl:dropdown-menu id="project-sales-more">
                  <sl:dropdown-menu-trigger as="sidebar-menu-action" show-on-hover aria-label="More for Sales &amp; Marketing">
                    <sl:icon name="ellipsis"/>
                  </sl:dropdown-menu-trigger>
                  <sl:dropdown-menu-content side="right">
                    <sl:dropdown-menu-item as="a" th:href="@{/projects/sales}">
                      <sl:icon name="folder"/>
                      View project
                    </sl:dropdown-menu-item>
                    <sl:dropdown-menu-item as="a" th:href="@{/projects/sales/share}">
                      <sl:icon name="forward"/>
                      Share project
                    </sl:dropdown-menu-item>
                    <sl:dropdown-menu-separator/>
                    <sl:dropdown-menu-item as="a" th:href="@{/projects/sales/delete}" variant="destructive">
                      <sl:icon name="trash-2"/>
                      Delete project
                    </sl:dropdown-menu-item>
                  </sl:dropdown-menu-content>
                </sl:dropdown-menu>
              </sl:sidebar-menu-item>
              <sl:sidebar-menu-item>
                <sl:sidebar-menu-button th:href="@{/projects/travel}" th:active="${#slNav.current('/projects/travel')}">
                  <sl:slot name="icon-start"><sl:icon name="map"/></sl:slot>
                  Travel
                </sl:sidebar-menu-button>
                <sl:dropdown-menu id="project-travel-more">
                  <sl:dropdown-menu-trigger as="sidebar-menu-action" show-on-hover aria-label="More for Travel">
                    <sl:icon name="ellipsis"/>
                  </sl:dropdown-menu-trigger>
                  <sl:dropdown-menu-content side="right">
                    <sl:dropdown-menu-item as="a" th:href="@{/projects/travel}">
                      <sl:icon name="folder"/>
                      View project
                    </sl:dropdown-menu-item>
                    <sl:dropdown-menu-item as="a" th:href="@{/projects/travel/share}">
                      <sl:icon name="forward"/>
                      Share project
                    </sl:dropdown-menu-item>
                    <sl:dropdown-menu-separator/>
                    <sl:dropdown-menu-item as="a" th:href="@{/projects/travel/delete}" variant="destructive">
                      <sl:icon name="trash-2"/>
                      Delete project
                    </sl:dropdown-menu-item>
                  </sl:dropdown-menu-content>
                </sl:dropdown-menu>
              </sl:sidebar-menu-item>
          <sl:sidebar-menu-item>
            <sl:sidebar-menu-button th:href="@{/projects}" th:active="${#slNav.current('/projects', true)}">
              <sl:slot name="icon-start"><sl:icon name="ellipsis"/></sl:slot>
              More
            </sl:sidebar-menu-button>
          </sl:sidebar-menu-item>
        </sl:sidebar-menu>
      </sl:sidebar-group>
    </sl:sidebar-content>
    <sl:sidebar-footer>
      <sl:user-menu>
        <sl:dropdown-menu-item as="a" th:href="@{/upgrade}"><sl:icon name="sparkles"/>Upgrade to Pro</sl:dropdown-menu-item>
        <sl:dropdown-menu-separator/>
        <sl:dropdown-menu-item as="a" th:href="@{/account}"><sl:icon name="badge-check"/>Account</sl:dropdown-menu-item>
        <sl:dropdown-menu-item as="a" th:href="@{/billing}"><sl:icon name="credit-card"/>Billing</sl:dropdown-menu-item>
        <sl:dropdown-menu-item as="a" th:href="@{/notifications}"><sl:icon name="bell"/>Notifications</sl:dropdown-menu-item>
      </sl:user-menu>
    </sl:sidebar-footer>
  </sl:sidebar>
  <sl:sidebar-inset>
    <sl:site-header>
      <sl:sidebar-trigger/>
      <sl:separator orientation="vertical"/>
      <sl:breadcrumb>
        <sl:breadcrumb-list>
          <th:block th:replace="${breadcrumb}"></th:block>
        </sl:breadcrumb-list>
      </sl:breadcrumb>
      <sl:slot name="end"><sl:theme-toggle/></sl:slot>
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

The teams are links, and the server checks the current one (`checked`), so the switcher works without JavaScript; an
application sets it with `th:checked` and lists its teams with `th:each`. The projects' menus are links too: a delete
link leads to a page (or, with htmx, an [alert dialog](https://wimdeblauwe.github.io/shadleaf/0.9.0/components/alert-dialog.md)) that asks first.

Without `variant="inset"` the sidebar is a column along the edge of the screen; with `collapsible="offcanvas"` it
moves out of view instead of collapsing to icons. The [Sidebar](https://wimdeblauwe.github.io/shadleaf/0.9.0/components/sidebar.md) page has every part and
option.

## A page

A page passes its title, its breadcrumb's levels and its content. The levels are a `th:block` with `th:ref`, so
nothing of it is rendered where it stands; the layout puts them in its `<sl:breadcrumb-list>`.

```html
<html xmlns:th="http://www.thymeleaf.org" xmlns:sl="https://shadleaf.dev/sl"
      th:replace="~{layout/main :: layout('History', ~{::breadcrumb}, ~{::#page-content})}">
<body>
<th:block th:ref="breadcrumb">
  <sl:breadcrumb-item>Playground</sl:breadcrumb-item>
  <sl:breadcrumb-separator/>
  <sl:breadcrumb-item>
    <sl:breadcrumb-page>History</sl:breadcrumb-page>
  </sl:breadcrumb-item>
</th:block>
<div id="page-content">
  ...
</div>
</body>
</html>
```

## The CSS

Shadleaf's CSS styles the components, not the page around them, and has no utility classes. The rest is the
application's CSS, here without Tailwind: the square behind the logo and the page's padding.

```css
/* The square behind the team's logo in the sidebar's header. */
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

/* The page's padding: the header above it has its own. */
.page { padding: 0 1rem 1rem; }
.page > h1 { margin-block-end: 1rem; font-size: 1.5rem; font-weight: 600; }
```
