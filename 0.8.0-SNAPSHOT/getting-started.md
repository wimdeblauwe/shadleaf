# Getting started

> Add Shadleaf to a Spring Boot application.

Source: https://shadleaf.dev/0.8.0-SNAPSHOT/getting-started/

Shadleaf needs Java 17, Spring Boot 4 and Thymeleaf. Your application needs no Node.js and no Tailwind.

## Add the dependency

```xml
<dependency>
  <groupId>io.github.wimdeblauwe</groupId>
  <artifactId>shadleaf-spring-boot-starter</artifactId>
  <version>0.8.0-SNAPSHOT</version>
</dependency>
```

## Include the stylesheet and the theme script

In the `<head>` of your layout:

```html
<head>
  <th:block th:replace="~{sl/layout :: theme-script}"></th:block>
  <th:block th:replace="~{sl/layout :: assets}"></th:block>
  <!-- your own stylesheet after it, so your overrides win -->
  <link rel="stylesheet" th:href="@{/css/application.css}">
</head>
```

`assets` links the stylesheet of the configured skin and loads the script with Alpine.js, which the interactive
components use; see [Alpine.js](https://shadleaf.dev/0.8.0-SNAPSHOT/guides/alpine.md) if your application loads Alpine itself. `theme-script` is a tiny inline script that sets `class="dark"`
on `<html>` before the first paint: `localStorage['shadleaf-theme']` set to `light` or `dark` wins, anything else follows
the operating system. [`<sl:theme-toggle>`](https://shadleaf.dev/0.8.0-SNAPSHOT/components/theme-toggle.md) lets the user choose. Behind a
Content-Security-Policy, see [CSP](https://shadleaf.dev/0.8.0-SNAPSHOT/guides/csp.md).

## Use the components

Declare the namespace, then use the tags:

```html
<html xmlns:th="http://www.thymeleaf.org" xmlns:sl="https://shadleaf.dev/sl">
...
<sl:button variant="outline">Cancel</sl:button>
<sl:button type="submit">Save</sl:button>
```

> **Note: type="button" is the default**
>
> `<sl:button>` renders `type="button"`, unlike HTML's `<button>`, so a button inside a form never submits it by
> accident. Write `type="submit"` on the button that should.

## Configuration

| Property | Default | |
| --- | --- | --- |
| `shadleaf.skin` | `vega` | The visual identity, named after the shadcn/ui style it follows: `vega` or `lyra`. |
| `shadleaf.assets.variant` | `standalone` | `embedded` leaves out Tailwind's reset, for applications that compile Tailwind themselves. |
| `shadleaf.assets.alpine` | `bundled` | Where Alpine.js comes from: `bundled`, `csp` or `external`; see [Alpine.js](https://shadleaf.dev/0.8.0-SNAPSHOT/guides/alpine.md). |
| `shadleaf.csp.nonce-attribute` | `cspNonce` | The request attribute holding a CSP nonce for the theme script. |
| `shadleaf.dev.web-types-file` | | Writes IDE metadata for your components; see [IDE completion](https://shadleaf.dev/0.8.0-SNAPSHOT/ide-completion.md). |

## Already compiling Tailwind?

Load the `embedded` bundle (`shadleaf.assets.variant=embedded`). It carries no preflight and no theme variables, so
your own Tailwind build keeps its reset, its headings and its `@theme`. Utility classes such as `w-full mt-4` on a
component come from your build: the library's bundles define none.

## A layout for every page

The Layouts section has whole layouts to copy, with the navigation in a sidebar
([Collapses to icons](https://shadleaf.dev/0.8.0-SNAPSHOT/layouts/collapses-to-icons.md)) or in a header ([Navigation in the header](https://shadleaf.dev/0.8.0-SNAPSHOT/layouts/header.md)),
a panel on a phone, the skip link, the breadcrumb, the theme toggle and the toasts already in place. The
[Application shell](https://shadleaf.dev/0.8.0-SNAPSHOT/components/application-shell.md) page helps you pick one and says what every layout needs.
