# Inset with secondary navigation

> Sections that are pages of their own with a chevron for their sub-menus, projects, a secondary navigation and the user menu, beside the page as a card.

Source: https://shadleaf.dev/0.11.0-SNAPSHOT/layouts/inset-secondary-navigation/

A sidebar whose sections are pages of their own: the section's name is a link, and a chevron beside it opens and
closes its pages in a [collapsible sub-menu](https://shadleaf.dev/0.11.0-SNAPSHOT/components/sidebar.md#a-section-that-is-a-page-too). The server
opens the current page's section. Below them the projects, each with a "More" menu, and at the bottom of the content
a secondary navigation (Support, Feedback) in small buttons. The footer holds the
[user menu](https://shadleaf.dev/0.11.0-SNAPSHOT/components/user-menu.md). The page is a card beside the sidebar (`variant="inset"`), and its header
has the trigger, the [breadcrumb](https://shadleaf.dev/0.11.0-SNAPSHOT/components/breadcrumb.md) and the theme toggle. On a desktop the trigger moves
the sidebar out of view; on a phone it opens the same sidebar as a panel. Switch the preview to Phone to see it.

**Example: Inset with secondary navigation.** Sections that are pages of their own, with a chevron beside each that opens its sub-menu, projects with a menu each, a secondary navigation at the bottom and the signed-in user's menu, and the page as a card beside it.

For reference: the [shadcn/ui block](https://ui.shadcn.com/blocks/sidebar#sidebar-08) this layout is based on.

## The layout

The whole layout file. It has what [every layout needs](https://shadleaf.dev/0.11.0-SNAPSHOT/components/application-shell.md#what-every-layout-needs): the
theme script and Shadleaf's assets in the head, the skip link first, one `main` (`<sl:sidebar-inset>`), the page's
title, and the toaster. The chevrons are `summary` elements and need a name each (`aria-label`), as an icon button
does: "Playground's pages".

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
  <link rel="stylesheet" th:href="@{/css/application.css}">
</head>
<body>
<sl:skip-link/>
<sl:sidebar-provider>
  <sl:sidebar variant="inset">
    <sl:sidebar-header>
      <sl:sidebar-menu>
        <sl:sidebar-menu-item>
          <sl:sidebar-menu-button th:href="@{/}" size="lg">
            <sl:slot name="icon-start"><span class="app-logo"><sl:icon name="command"/></span></sl:slot>
            <strong>Acme Inc</strong>
            <small>Enterprise</small>
          </sl:sidebar-menu-button>
        </sl:sidebar-menu-item>
      </sl:sidebar-menu>
    </sl:sidebar-header>
    <sl:sidebar-content>
      <sl:sidebar-group id="nav-platform">
        <sl:sidebar-group-label>Platform</sl:sidebar-group-label>
        <sl:sidebar-menu>
          <sl:sidebar-menu-item>
            <sl:sidebar-menu-button th:href="@{/playground}" th:active="${#slNav.current('/playground', true)}">
              <sl:slot name="icon-start"><sl:icon name="square-terminal"/></sl:slot>
              Playground
            </sl:sidebar-menu-button>
            <sl:sidebar-menu-sub collapsible th:open="${#slNav.current('/playground')}" aria-label="Playground's pages">
              <sl:sidebar-menu-sub-item>
                <sl:sidebar-menu-sub-button th:href="@{/playground/history}" th:active="${#slNav.current('/playground/history')}">History</sl:sidebar-menu-sub-button>
              </sl:sidebar-menu-sub-item>
              <sl:sidebar-menu-sub-item>
                <sl:sidebar-menu-sub-button th:href="@{/playground/starred}" th:active="${#slNav.current('/playground/starred')}">Starred</sl:sidebar-menu-sub-button>
              </sl:sidebar-menu-sub-item>
              <sl:sidebar-menu-sub-item>
                <sl:sidebar-menu-sub-button th:href="@{/playground/settings}" th:active="${#slNav.current('/playground/settings')}">Settings</sl:sidebar-menu-sub-button>
              </sl:sidebar-menu-sub-item>
            </sl:sidebar-menu-sub>
          </sl:sidebar-menu-item>
          <sl:sidebar-menu-item>
            <sl:sidebar-menu-button th:href="@{/models}" th:active="${#slNav.current('/models', true)}">
              <sl:slot name="icon-start"><sl:icon name="bot"/></sl:slot>
              Models
            </sl:sidebar-menu-button>
            <sl:sidebar-menu-sub collapsible th:open="${#slNav.current('/models')}" aria-label="Models's pages">
              <sl:sidebar-menu-sub-item>
                <sl:sidebar-menu-sub-button th:href="@{/models/genesis}" th:active="${#slNav.current('/models/genesis')}">Genesis</sl:sidebar-menu-sub-button>
              </sl:sidebar-menu-sub-item>
              <sl:sidebar-menu-sub-item>
                <sl:sidebar-menu-sub-button th:href="@{/models/explorer}" th:active="${#slNav.current('/models/explorer')}">Explorer</sl:sidebar-menu-sub-button>
              </sl:sidebar-menu-sub-item>
              <sl:sidebar-menu-sub-item>
                <sl:sidebar-menu-sub-button th:href="@{/models/quantum}" th:active="${#slNav.current('/models/quantum')}">Quantum</sl:sidebar-menu-sub-button>
              </sl:sidebar-menu-sub-item>
            </sl:sidebar-menu-sub>
          </sl:sidebar-menu-item>
          <sl:sidebar-menu-item>
            <sl:sidebar-menu-button th:href="@{/docs}" th:active="${#slNav.current('/docs', true)}">
              <sl:slot name="icon-start"><sl:icon name="book-open"/></sl:slot>
              Documentation
            </sl:sidebar-menu-button>
            <sl:sidebar-menu-sub collapsible th:open="${#slNav.current('/docs')}" aria-label="Documentation's pages">
              <sl:sidebar-menu-sub-item>
                <sl:sidebar-menu-sub-button th:href="@{/docs/introduction}" th:active="${#slNav.current('/docs/introduction')}">Introduction</sl:sidebar-menu-sub-button>
              </sl:sidebar-menu-sub-item>
              <sl:sidebar-menu-sub-item>
                <sl:sidebar-menu-sub-button th:href="@{/docs/get-started}" th:active="${#slNav.current('/docs/get-started')}">Get Started</sl:sidebar-menu-sub-button>
              </sl:sidebar-menu-sub-item>
              <sl:sidebar-menu-sub-item>
                <sl:sidebar-menu-sub-button th:href="@{/docs/tutorials}" th:active="${#slNav.current('/docs/tutorials')}">Tutorials</sl:sidebar-menu-sub-button>
              </sl:sidebar-menu-sub-item>
              <sl:sidebar-menu-sub-item>
                <sl:sidebar-menu-sub-button th:href="@{/docs/changelog}" th:active="${#slNav.current('/docs/changelog')}">Changelog</sl:sidebar-menu-sub-button>
              </sl:sidebar-menu-sub-item>
            </sl:sidebar-menu-sub>
          </sl:sidebar-menu-item>
          <sl:sidebar-menu-item>
            <sl:sidebar-menu-button th:href="@{/settings}" th:active="${#slNav.current('/settings', true)}">
              <sl:slot name="icon-start"><sl:icon name="settings-2"/></sl:slot>
              Settings
            </sl:sidebar-menu-button>
            <sl:sidebar-menu-sub collapsible th:open="${#slNav.current('/settings')}" aria-label="Settings's pages">
              <sl:sidebar-menu-sub-item>
                <sl:sidebar-menu-sub-button th:href="@{/settings/general}" th:active="${#slNav.current('/settings/general')}">General</sl:sidebar-menu-sub-button>
              </sl:sidebar-menu-sub-item>
              <sl:sidebar-menu-sub-item>
                <sl:sidebar-menu-sub-button th:href="@{/settings/team}" th:active="${#slNav.current('/settings/team')}">Team</sl:sidebar-menu-sub-button>
              </sl:sidebar-menu-sub-item>
              <sl:sidebar-menu-sub-item>
                <sl:sidebar-menu-sub-button th:href="@{/settings/billing}" th:active="${#slNav.current('/settings/billing')}">Billing</sl:sidebar-menu-sub-button>
              </sl:sidebar-menu-sub-item>
              <sl:sidebar-menu-sub-item>
                <sl:sidebar-menu-sub-button th:href="@{/settings/limits}" th:active="${#slNav.current('/settings/limits')}">Limits</sl:sidebar-menu-sub-button>
              </sl:sidebar-menu-sub-item>
            </sl:sidebar-menu-sub>
          </sl:sidebar-menu-item>
        </sl:sidebar-menu>
      </sl:sidebar-group>
      <sl:sidebar-group id="nav-projects">
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
                  <sl:icon name="share"/>
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
                  <sl:icon name="share"/>
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
                  <sl:icon name="share"/>
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
      <sl:sidebar-group class="nav-secondary">
        <sl:sidebar-group-content>
          <sl:sidebar-menu>
            <sl:sidebar-menu-item>
              <sl:sidebar-menu-button th:href="@{/support}" th:active="${#slNav.current('/support')}" size="sm">
                <sl:slot name="icon-start"><sl:icon name="life-buoy"/></sl:slot>
                Support
              </sl:sidebar-menu-button>
            </sl:sidebar-menu-item>
            <sl:sidebar-menu-item>
              <sl:sidebar-menu-button th:href="@{/feedback}" th:active="${#slNav.current('/feedback')}" size="sm">
                <sl:slot name="icon-start"><sl:icon name="send"/></sl:slot>
                Feedback
              </sl:sidebar-menu-button>
            </sl:sidebar-menu-item>
          </sl:sidebar-menu>
        </sl:sidebar-group-content>
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

## A page

A page passes its title, its breadcrumb's levels and its content. The levels are a `th:block` with `th:ref`, so
nothing of it is rendered where it stands; the layout puts them in its `<sl:breadcrumb-list>`.

```html
<html xmlns:th="http://www.thymeleaf.org" xmlns:sl="https://shadleaf.dev/sl"
      th:replace="~{layout/main :: layout('History', ~{::breadcrumb}, ~{::#page-content})}">
<body>
<th:block th:ref="breadcrumb">
  <sl:breadcrumb-item>
    <sl:breadcrumb-link th:href="@{/playground}">Playground</sl:breadcrumb-link>
  </sl:breadcrumb-item>
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
application's CSS, here without Tailwind: the square behind the logo, the secondary navigation pushed to the bottom
of the content (the `nav-secondary` class on its group), and the page's padding.

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

/* The secondary navigation at the bottom of the sidebar's content. */
.nav-secondary { margin-block-start: auto; }

/* The page's padding: the header above it has its own. */
.page { padding: 0 1rem 1rem; }
.page > h1 { margin-block-end: 1rem; font-size: 1.5rem; font-weight: 600; }
```
