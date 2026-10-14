# Sub-menus

> A sidebar of sections with their pages in sub-menus that are always open, as documentation has.

Source: https://wimdeblauwe.github.io/shadleaf/current/layouts/sub-menus/

A sidebar for an application whose pages fall into a handful of sections, such as documentation: each section is a
link of its own with its pages in a [sub-menu](https://wimdeblauwe.github.io/shadleaf/current/components/sidebar.md#collapsible-groups) below it, always open, so
the whole structure is in view. The application's name and version are at the top. The page's header has the
trigger and the [breadcrumb](https://wimdeblauwe.github.io/shadleaf/current/components/breadcrumb.md). On a desktop the trigger moves the sidebar out of view; on
a phone it opens the same sidebar as a panel. Switch the preview to Phone to see it.

**Example: Sub-menus.** A sidebar of sections, each with its pages in a sub-menu that is always open, as in documentation. The current page is a sub-button; the trigger moves the sidebar out of view on a desktop and opens it as a panel on a phone.

For reference: the [shadcn/ui block](https://ui.shadcn.com/blocks/sidebar#sidebar-03) this layout is based on.

The [floating variant](https://wimdeblauwe.github.io/shadleaf/current/layouts/floating-sub-menus.md) has the same menus in a panel with a margin round it.

## The layout

The whole layout file. It has what [every layout needs](https://wimdeblauwe.github.io/shadleaf/current/components/application-shell.md#what-every-layout-needs): the
theme script and Shadleaf's assets in the head, the skip link first, one `main` (`<sl:sidebar-inset>`), the page's
title, and the toaster. `#slNav.current` marks the current page from the request: the sections compare the exact path
(`true`), so a section is not marked as well when one of its pages is shown.

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
  <sl:sidebar>
    <sl:sidebar-header>
      <sl:sidebar-menu>
        <sl:sidebar-menu-item>
          <sl:sidebar-menu-button th:href="@{/}" size="lg">
            <sl:slot name="icon-start"><span class="app-logo"><sl:icon name="gallery-vertical-end"/></span></sl:slot>
            <strong>Documentation</strong>
            <small>v1.0.0</small>
          </sl:sidebar-menu-button>
        </sl:sidebar-menu-item>
      </sl:sidebar-menu>
    </sl:sidebar-header>
    <sl:sidebar-content>
      <sl:sidebar-group>
        <sl:sidebar-menu class="docs-nav">
          <sl:sidebar-menu-item>
            <sl:sidebar-menu-button th:href="@{/getting-started}"
                                    th:active="${#slNav.current('/getting-started', true)}">
              Getting Started
            </sl:sidebar-menu-button>
            <sl:sidebar-menu-sub>
              <sl:sidebar-menu-sub-item>
                <sl:sidebar-menu-sub-button th:href="@{/getting-started/installation}"
                                            th:active="${#slNav.current('/getting-started/installation')}">
                  Installation
                </sl:sidebar-menu-sub-button>
              </sl:sidebar-menu-sub-item>
              <sl:sidebar-menu-sub-item>
                <sl:sidebar-menu-sub-button th:href="@{/getting-started/project-structure}"
                                            th:active="${#slNav.current('/getting-started/project-structure')}">
                  Project Structure
                </sl:sidebar-menu-sub-button>
              </sl:sidebar-menu-sub-item>
            </sl:sidebar-menu-sub>
          </sl:sidebar-menu-item>
          <sl:sidebar-menu-item>
            <sl:sidebar-menu-button th:href="@{/build}"
                                    th:active="${#slNav.current('/build', true)}">
              Build Your Application
            </sl:sidebar-menu-button>
            <sl:sidebar-menu-sub>
              <sl:sidebar-menu-sub-item>
                <sl:sidebar-menu-sub-button th:href="@{/build/routing}"
                                            th:active="${#slNav.current('/build/routing')}">
                  Routing
                </sl:sidebar-menu-sub-button>
              </sl:sidebar-menu-sub-item>
              <sl:sidebar-menu-sub-item>
                <sl:sidebar-menu-sub-button th:href="@{/build/data-fetching}"
                                            th:active="${#slNav.current('/build/data-fetching')}">
                  Data Fetching
                </sl:sidebar-menu-sub-button>
              </sl:sidebar-menu-sub-item>
              <sl:sidebar-menu-sub-item>
                <sl:sidebar-menu-sub-button th:href="@{/build/rendering}"
                                            th:active="${#slNav.current('/build/rendering')}">
                  Rendering
                </sl:sidebar-menu-sub-button>
              </sl:sidebar-menu-sub-item>
              <sl:sidebar-menu-sub-item>
                <sl:sidebar-menu-sub-button th:href="@{/build/caching}"
                                            th:active="${#slNav.current('/build/caching')}">
                  Caching
                </sl:sidebar-menu-sub-button>
              </sl:sidebar-menu-sub-item>
              <sl:sidebar-menu-sub-item>
                <sl:sidebar-menu-sub-button th:href="@{/build/styling}"
                                            th:active="${#slNav.current('/build/styling')}">
                  Styling
                </sl:sidebar-menu-sub-button>
              </sl:sidebar-menu-sub-item>
            </sl:sidebar-menu-sub>
          </sl:sidebar-menu-item>
          <sl:sidebar-menu-item>
            <sl:sidebar-menu-button th:href="@{/api}"
                                    th:active="${#slNav.current('/api', true)}">
              API Reference
            </sl:sidebar-menu-button>
            <sl:sidebar-menu-sub>
              <sl:sidebar-menu-sub-item>
                <sl:sidebar-menu-sub-button th:href="@{/api/components}"
                                            th:active="${#slNav.current('/api/components')}">
                  Components
                </sl:sidebar-menu-sub-button>
              </sl:sidebar-menu-sub-item>
              <sl:sidebar-menu-sub-item>
                <sl:sidebar-menu-sub-button th:href="@{/api/file-conventions}"
                                            th:active="${#slNav.current('/api/file-conventions')}">
                  File Conventions
                </sl:sidebar-menu-sub-button>
              </sl:sidebar-menu-sub-item>
              <sl:sidebar-menu-sub-item>
                <sl:sidebar-menu-sub-button th:href="@{/api/functions}"
                                            th:active="${#slNav.current('/api/functions')}">
                  Functions
                </sl:sidebar-menu-sub-button>
              </sl:sidebar-menu-sub-item>
            </sl:sidebar-menu-sub>
          </sl:sidebar-menu-item>
          <sl:sidebar-menu-item>
            <sl:sidebar-menu-button th:href="@{/architecture}"
                                    th:active="${#slNav.current('/architecture', true)}">
              Architecture
            </sl:sidebar-menu-button>
            <sl:sidebar-menu-sub>
              <sl:sidebar-menu-sub-item>
                <sl:sidebar-menu-sub-button th:href="@{/architecture/accessibility}"
                                            th:active="${#slNav.current('/architecture/accessibility')}">
                  Accessibility
                </sl:sidebar-menu-sub-button>
              </sl:sidebar-menu-sub-item>
              <sl:sidebar-menu-sub-item>
                <sl:sidebar-menu-sub-button th:href="@{/architecture/fast-refresh}"
                                            th:active="${#slNav.current('/architecture/fast-refresh')}">
                  Fast Refresh
                </sl:sidebar-menu-sub-button>
              </sl:sidebar-menu-sub-item>
            </sl:sidebar-menu-sub>
          </sl:sidebar-menu-item>
          <sl:sidebar-menu-item>
            <sl:sidebar-menu-button th:href="@{/community}"
                                    th:active="${#slNav.current('/community', true)}">
              Community
            </sl:sidebar-menu-button>
            <sl:sidebar-menu-sub>
              <sl:sidebar-menu-sub-item>
                <sl:sidebar-menu-sub-button th:href="@{/community/contribution-guide}"
                                            th:active="${#slNav.current('/community/contribution-guide')}">
                  Contribution Guide
                </sl:sidebar-menu-sub-button>
              </sl:sidebar-menu-sub-item>
            </sl:sidebar-menu-sub>
          </sl:sidebar-menu-item>
        </sl:sidebar-menu>
      </sl:sidebar-group>
    </sl:sidebar-content>
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
      th:replace="~{layout/main :: layout('Data Fetching', ~{::breadcrumb}, ~{::#page-content})}">
<body>
<th:block th:ref="breadcrumb">
  <sl:breadcrumb-item>
    <sl:breadcrumb-link th:href="@{/build}">Build Your Application</sl:breadcrumb-link>
  </sl:breadcrumb-item>
  <sl:breadcrumb-separator/>
  <sl:breadcrumb-item>
    <sl:breadcrumb-page>Data Fetching</sl:breadcrumb-page>
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
application's CSS, here without Tailwind: the square behind the logo, the weight of the sections' links (the `docs-nav` class on the menu), a border below the header, and the page's padding.

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

/* The sections' links in a medium weight, their pages' in the normal one. */
.docs-nav > .sidebar-menu-item > .sidebar-menu-button { font-weight: 500; }

/* A border below the page's header, and the page's padding. */
.sidebar-inset > .site-header { border-block-end: 1px solid var(--border); }
.page { padding: 1rem; }
.page > h1 { margin-block-end: 1rem; font-size: 1.5rem; font-weight: 600; }
```
