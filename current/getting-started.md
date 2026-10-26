# Getting started

> Add Shadleaf to a Spring Boot application.

Source: https://shadleaf.dev/current/getting-started/

Shadleaf needs Java 17, Spring Boot 4 and Thymeleaf. The jar ships compiled CSS and JavaScript, so a plain Maven
build is enough to get started.

## Add the dependency

Add the dependency to your `pom.xml` of your Spring Boot with Thymeleaf application:

```xml
<dependency>
  <groupId>io.github.wimdeblauwe</groupId>
  <artifactId>shadleaf-spring-boot-starter</artifactId>
  <version>1.0.0</version>
</dependency>
```

> **Note: Don't have an application yet?**
>
> You can generate an application from [Spring Initializr](https://start.spring.io/) with the Spring Web & Thymeleaf dependencies.
>
> Alternatively, use [ttcli](https://github.com/wimdeblauwe/ttcli) to generate an application.
> It allows to have a very nice live-reload setup for quick development.

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

The `theme-script` reads the user's preference from `localStorage` and sets the `class="dark"` on `<html>` before the first paint.
Place the [`<sl:theme-toggle>`](https://shadleaf.dev/current/components/theme-toggle.md) on your page to let the user choose between light and dark mode.

The `assets` block renders the CSS and JS links.
By default, Alpine.js is loaded and you can use it in your own pages.
See [Alpine.js](https://shadleaf.dev/current/guides/alpine.md) if your application loads Alpine itself.

If you want to use Tailwind CSS in your own pages, see [Tailwind CSS](https://shadleaf.dev/current/guides/tailwind.md) for how to set it up.

If you have a Content-Security-Policy, see [CSP](https://shadleaf.dev/current/guides/csp.md) for more information.

## Use the components

Add a component to your Thymeleaf template:

```html
<html xmlns:th="http://www.thymeleaf.org" xmlns:sl="https://shadleaf.dev/sl">
...
<sl:button variant="outline">Cancel</sl:button>
<sl:button type="submit">Save</sl:button>
```

See the [Components](https://shadleaf.dev/current/components.md) section for all available components and their attributes.
There are also [Layouts](https://shadleaf.dev/current/layouts.md) with whole page layouts to copy.

## Configuration

| Property                       | Default      |                                                                                                                                                                                  |
|--------------------------------|--------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `shadleaf.skin`                | `vega`       | The visual identity, named after the shadcn/ui style it follows; see [Skin](https://shadleaf.dev/current/guides/adjustability.md#4-skin) for the list.                                                       |
| `shadleaf.assets.variant`      | `standalone` | `embedded` leaves out Tailwind's reset, for applications that compile Tailwind themselves.                                                                                       |
| `shadleaf.assets.alpine`       | `bundled`    | Where Alpine.js comes from: `bundled`, `csp` or `external`; see [Alpine.js](https://shadleaf.dev/current/guides/alpine.md).                                                                                  |
| `shadleaf.csp.nonce-attribute` | `cspNonce`   | The request attribute holding a CSP nonce for the theme script.                                                                                                                  |
| `shadleaf.security.login-url`  | `/login`     | Where the user menu's Sign in link goes; `/oauth2/authorization/<id>` with one OAuth2 login client. See [Spring Security](https://shadleaf.dev/current/guides/spring-security.md#sign-in-and-sign-out-urls). |
| `shadleaf.security.logout-url` | `/logout`    | Where the user menu's sign-out form posts to; see [Spring Security](https://shadleaf.dev/current/guides/spring-security.md#sign-in-and-sign-out-urls).                                                       |
| `shadleaf.dev.web-types-file`  |              | Writes IDE metadata for your components; see [IDE completion](https://shadleaf.dev/current/ide-completion.md).                                                                                               |
