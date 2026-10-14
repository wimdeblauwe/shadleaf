# On the end

> A table of contents in a sidebar on the end of the line, the right in a left-to-right page, with the trigger at the end of the page's header.

Source: https://wimdeblauwe.github.io/shadleaf/0.8.0/layouts/sidebar-end/

A sidebar on the end of the line (`side="end"`): the right, or the left on a right-to-left page (`dir="rtl"`). Here
it holds a table of contents under a group label, with the sections and their pages in sub-menus. The trigger is at
the end of the page's header, in the [site header](https://wimdeblauwe.github.io/shadleaf/0.8.0/components/site-header.md)'s `end` slot, on the same side as
the sidebar; its icon shows a panel on that side. On a phone the panel slides in from the end.

**Example: On the end.** A table of contents on the end of the line: the right, or the left on a right-to-left page. The sidebar comes after the page in the markup, so the keyboard reaches the page first, and the trigger sits at the end of the page's header.

For reference: the [shadcn/ui block](https://ui.shadcn.com/blocks/sidebar#sidebar-14) this layout is based on.

## The layout

The whole layout file, with what [every layout needs](https://wimdeblauwe.github.io/shadleaf/0.8.0/components/application-shell.md#what-every-layout-needs). The
sidebar comes after `<sl:sidebar-inset>` in the markup, as in the block, so the keyboard and a screen reader reach the
page first and the table of contents after it. `side="end"` puts it on the end either way: written before the inset,
it is still drawn there, and reached first.

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
  <sl:sidebar-inset>
    <sl:site-header>
      <sl:breadcrumb>
        <sl:breadcrumb-list>
          <th:block th:replace="${breadcrumb}"></th:block>
        </sl:breadcrumb-list>
      </sl:breadcrumb>
      <sl:slot name="end"><sl:sidebar-trigger/></sl:slot>
    </sl:site-header>
    <div class="page">
      <h1 th:text="${pageTitle}"></h1>
      <div th:replace="${content}"></div>
    </div>
  </sl:sidebar-inset>
  <sl:sidebar side="end">
    <sl:sidebar-content>
      <sl:sidebar-group id="toc">
        <sl:sidebar-group-label>Table of Contents</sl:sidebar-group-label>
        <sl:sidebar-group-content>
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
        </sl:sidebar-group-content>
      </sl:sidebar-group>
    </sl:sidebar-content>
  </sl:sidebar>
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

A page passes its title, its breadcrumb's levels and its content, as for the [Sub-menus](https://wimdeblauwe.github.io/shadleaf/0.8.0/layouts/sub-menus.md#a-page)
layout:

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

The weight of the sections' links, a border below the header and the page's padding:

```css
/* The sections' links in a medium weight, their pages' in the normal one. */
.docs-nav > .sidebar-menu-item > .sidebar-menu-button { font-weight: 500; }

/* A border below the page's header, and the page's padding. */
.sidebar-inset > .site-header { border-block-end: 1px solid var(--border); }
.page { padding: 1rem; }
.page > h1 { margin-block-end: 1rem; font-size: 1.5rem; font-weight: 600; }
```
