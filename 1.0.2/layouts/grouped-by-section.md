# Grouped by section

> A sidebar of sections, each a group of pages under its label, with a version switcher and a search field, as documentation has.

Source: https://shadleaf.dev/1.0.2/layouts/grouped-by-section/

A sidebar for an application whose pages fall into a handful of sections, such as documentation: each section is a
[group](https://shadleaf.dev/1.0.2/components/sidebar.md) whose label names it, with its pages below, so the whole structure is in view. At
the top, a version switcher (a [dropdown menu](https://shadleaf.dev/1.0.2/components/dropdown-menu.md#opened-from-the-sidebar) opened from a
large menu button) and a search field. The page's header has the trigger and the
[breadcrumb](https://shadleaf.dev/1.0.2/components/breadcrumb.md). On a desktop the trigger moves the sidebar out of view; on a phone it opens
the same sidebar as a panel. Switch the preview to Phone to see it.

**Example: Grouped by section.** A sidebar of sections, each a group of pages under its label, with a version switcher and a search field at the top, as in documentation. The trigger moves the sidebar out of view on a desktop and opens it as a panel on a phone.

## The layout

The whole layout file. It has what [every layout needs](https://shadleaf.dev/1.0.2/components/application-shell.md#what-every-layout-needs): the
theme script and Shadleaf's assets in the head, the skip link first, one `main` (`<sl:sidebar-inset>`), the page's
title, and the toaster. `#slNav.current` marks the current page from the request.

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
          <sl:dropdown-menu id="version-switcher">
            <sl:dropdown-menu-trigger as="sidebar-menu-button" size="lg">
              <sl:slot name="icon-start"><span class="app-logo"><sl:icon name="gallery-vertical-end"/></span></sl:slot>
              <strong>Documentation</strong>
              <small>v1.0.1</small>
              <sl:slot name="icon-end"><sl:icon name="chevrons-up-down"/></sl:slot>
            </sl:dropdown-menu-trigger>
            <sl:dropdown-menu-content>
              <sl:dropdown-menu-radio-group aria-label="Versions">
                <sl:dropdown-menu-radio-item as="a" th:href="@{/}" checked>v1.0.1</sl:dropdown-menu-radio-item>
                <sl:dropdown-menu-radio-item as="a" th:href="@{/1.1.0-alpha/}">v1.1.0-alpha</sl:dropdown-menu-radio-item>
                <sl:dropdown-menu-radio-item as="a" th:href="@{/2.0.0-beta1/}">v2.0.0-beta1</sl:dropdown-menu-radio-item>
              </sl:dropdown-menu-radio-group>
            </sl:dropdown-menu-content>
          </sl:dropdown-menu>
        </sl:sidebar-menu-item>
      </sl:sidebar-menu>
      <form class="docs-search" role="search" th:action="@{/search}">
        <sl:input type="search" name="q" placeholder="Search the docs..." aria-label="Search the docs"/>
        <sl:icon name="search"/>
      </form>
    </sl:sidebar-header>
    <sl:sidebar-content>
      <sl:sidebar-group id="nav-getting">
        <sl:sidebar-group-label>Getting Started</sl:sidebar-group-label>
        <sl:sidebar-group-content>
          <sl:sidebar-menu>
            <sl:sidebar-menu-item>
              <sl:sidebar-menu-button th:href="@{/getting-started/installation}" th:active="${#slNav.current('/getting-started/installation')}">Installation</sl:sidebar-menu-button>
            </sl:sidebar-menu-item>
            <sl:sidebar-menu-item>
              <sl:sidebar-menu-button th:href="@{/getting-started/project-structure}" th:active="${#slNav.current('/getting-started/project-structure')}">Project Structure</sl:sidebar-menu-button>
            </sl:sidebar-menu-item>
          </sl:sidebar-menu>
        </sl:sidebar-group-content>
      </sl:sidebar-group>
      <sl:sidebar-group id="nav-build">
        <sl:sidebar-group-label>Build Your Application</sl:sidebar-group-label>
        <sl:sidebar-group-content>
          <sl:sidebar-menu>
            <sl:sidebar-menu-item>
              <sl:sidebar-menu-button th:href="@{/build/routing}" th:active="${#slNav.current('/build/routing')}">Routing</sl:sidebar-menu-button>
            </sl:sidebar-menu-item>
            <sl:sidebar-menu-item>
              <sl:sidebar-menu-button th:href="@{/build/data-fetching}" th:active="${#slNav.current('/build/data-fetching')}">Data Fetching</sl:sidebar-menu-button>
            </sl:sidebar-menu-item>
            <sl:sidebar-menu-item>
              <sl:sidebar-menu-button th:href="@{/build/rendering}" th:active="${#slNav.current('/build/rendering')}">Rendering</sl:sidebar-menu-button>
            </sl:sidebar-menu-item>
            <sl:sidebar-menu-item>
              <sl:sidebar-menu-button th:href="@{/build/caching}" th:active="${#slNav.current('/build/caching')}">Caching</sl:sidebar-menu-button>
            </sl:sidebar-menu-item>
            <sl:sidebar-menu-item>
              <sl:sidebar-menu-button th:href="@{/build/styling}" th:active="${#slNav.current('/build/styling')}">Styling</sl:sidebar-menu-button>
            </sl:sidebar-menu-item>
            <sl:sidebar-menu-item>
              <sl:sidebar-menu-button th:href="@{/build/optimizing}" th:active="${#slNav.current('/build/optimizing')}">Optimizing</sl:sidebar-menu-button>
            </sl:sidebar-menu-item>
            <sl:sidebar-menu-item>
              <sl:sidebar-menu-button th:href="@{/build/configuring}" th:active="${#slNav.current('/build/configuring')}">Configuring</sl:sidebar-menu-button>
            </sl:sidebar-menu-item>
            <sl:sidebar-menu-item>
              <sl:sidebar-menu-button th:href="@{/build/testing}" th:active="${#slNav.current('/build/testing')}">Testing</sl:sidebar-menu-button>
            </sl:sidebar-menu-item>
            <sl:sidebar-menu-item>
              <sl:sidebar-menu-button th:href="@{/build/authentication}" th:active="${#slNav.current('/build/authentication')}">Authentication</sl:sidebar-menu-button>
            </sl:sidebar-menu-item>
            <sl:sidebar-menu-item>
              <sl:sidebar-menu-button th:href="@{/build/deploying}" th:active="${#slNav.current('/build/deploying')}">Deploying</sl:sidebar-menu-button>
            </sl:sidebar-menu-item>
            <sl:sidebar-menu-item>
              <sl:sidebar-menu-button th:href="@{/build/upgrading}" th:active="${#slNav.current('/build/upgrading')}">Upgrading</sl:sidebar-menu-button>
            </sl:sidebar-menu-item>
            <sl:sidebar-menu-item>
              <sl:sidebar-menu-button th:href="@{/build/examples}" th:active="${#slNav.current('/build/examples')}">Examples</sl:sidebar-menu-button>
            </sl:sidebar-menu-item>
          </sl:sidebar-menu>
        </sl:sidebar-group-content>
      </sl:sidebar-group>
      <sl:sidebar-group id="nav-api">
        <sl:sidebar-group-label>API Reference</sl:sidebar-group-label>
        <sl:sidebar-group-content>
          <sl:sidebar-menu>
            <sl:sidebar-menu-item>
              <sl:sidebar-menu-button th:href="@{/api/components}" th:active="${#slNav.current('/api/components')}">Components</sl:sidebar-menu-button>
            </sl:sidebar-menu-item>
            <sl:sidebar-menu-item>
              <sl:sidebar-menu-button th:href="@{/api/file-conventions}" th:active="${#slNav.current('/api/file-conventions')}">File Conventions</sl:sidebar-menu-button>
            </sl:sidebar-menu-item>
            <sl:sidebar-menu-item>
              <sl:sidebar-menu-button th:href="@{/api/functions}" th:active="${#slNav.current('/api/functions')}">Functions</sl:sidebar-menu-button>
            </sl:sidebar-menu-item>
            <sl:sidebar-menu-item>
              <sl:sidebar-menu-button th:href="@{/api/configuration-options}" th:active="${#slNav.current('/api/configuration-options')}">Configuration Options</sl:sidebar-menu-button>
            </sl:sidebar-menu-item>
            <sl:sidebar-menu-item>
              <sl:sidebar-menu-button th:href="@{/api/cli}" th:active="${#slNav.current('/api/cli')}">CLI</sl:sidebar-menu-button>
            </sl:sidebar-menu-item>
            <sl:sidebar-menu-item>
              <sl:sidebar-menu-button th:href="@{/api/edge-runtime}" th:active="${#slNav.current('/api/edge-runtime')}">Edge Runtime</sl:sidebar-menu-button>
            </sl:sidebar-menu-item>
          </sl:sidebar-menu>
        </sl:sidebar-group-content>
      </sl:sidebar-group>
      <sl:sidebar-group id="nav-architecture">
        <sl:sidebar-group-label>Architecture</sl:sidebar-group-label>
        <sl:sidebar-group-content>
          <sl:sidebar-menu>
            <sl:sidebar-menu-item>
              <sl:sidebar-menu-button th:href="@{/architecture/accessibility}" th:active="${#slNav.current('/architecture/accessibility')}">Accessibility</sl:sidebar-menu-button>
            </sl:sidebar-menu-item>
            <sl:sidebar-menu-item>
              <sl:sidebar-menu-button th:href="@{/architecture/fast-refresh}" th:active="${#slNav.current('/architecture/fast-refresh')}">Fast Refresh</sl:sidebar-menu-button>
            </sl:sidebar-menu-item>
            <sl:sidebar-menu-item>
              <sl:sidebar-menu-button th:href="@{/architecture/compiler}" th:active="${#slNav.current('/architecture/compiler')}">Compiler</sl:sidebar-menu-button>
            </sl:sidebar-menu-item>
            <sl:sidebar-menu-item>
              <sl:sidebar-menu-button th:href="@{/architecture/supported-browsers}" th:active="${#slNav.current('/architecture/supported-browsers')}">Supported Browsers</sl:sidebar-menu-button>
            </sl:sidebar-menu-item>
            <sl:sidebar-menu-item>
              <sl:sidebar-menu-button th:href="@{/architecture/bundler}" th:active="${#slNav.current('/architecture/bundler')}">Bundler</sl:sidebar-menu-button>
            </sl:sidebar-menu-item>
          </sl:sidebar-menu>
        </sl:sidebar-group-content>
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

The versions are links, and the server checks the one being read (`checked`), so the switcher works without
JavaScript. Here the check is fixed in the markup; an application that serves several versions sets it with
`th:checked`. The search field is a plain `GET` form; Shadleaf has no search of its own.

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
application's CSS, here without Tailwind: the square behind the logo, the magnifier in the search field (the
`docs-search` class on the form) and its height, a border below the header, and the page's padding.

```css
/* The square behind the logo in the version switcher. */
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

/* The search field: a magnifier inside it, at the start. */
.docs-search { position: relative; }
.docs-search > .input {
  block-size: 2rem;
  padding-inline-start: 2rem;
  background: var(--background);
}
.docs-search > svg {
  position: absolute;
  inset-inline-start: 0.5rem;
  inset-block-start: 50%;
  translate: 0 -50%;
  inline-size: 1rem;
  block-size: 1rem;
  opacity: 0.5;
  pointer-events: none;
}

/* A border below the page's header, and the page's padding. */
.sidebar-inset > .site-header { border-block-end: 1px solid var(--border); }
.page { padding: 1rem; }
.page > h1 { margin-block-end: 1rem; font-size: 1.5rem; font-weight: 600; }
```
