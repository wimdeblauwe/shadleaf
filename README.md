# Shadleaf

shadcn/ui for Thymeleaf, packaged as a Spring Boot starter.

> **Status:** pre-release, under active development. Before 1.0, the rendered markup can change between versions.

## Usage

Add the dependency:

```xml
<dependency>
  <groupId>io.github.wimdeblauwe</groupId>
  <artifactId>shadleaf-spring-boot-starter</artifactId>
  <version>0.1.0-SNAPSHOT</version>
</dependency>
```

Include the assets in the `<head>` of your layout:

```html
<th:block th:replace="~{sl/layout :: assets}"></th:block>
```

No Node.js or Tailwind is needed in your application: the jar ships compiled CSS, served under `/shadleaf/**`.

Declare the namespace and use the components:

```html
<html xmlns:th="http://www.thymeleaf.org" xmlns:sl="https://shadleaf.dev/sl">
...
<sl:button variant="destructive" hx-delete="/orders/42">
  <sl:slot name="icon-start"><sl:icon name="trash"/></sl:slot>
  Delete order
</sl:button>
```

`<sl:button>` renders `type="button"` unless you say `type="submit"`, so a button in a form never submits by accident.
`<sl:icon>` inlines the SVG of any [lucide](https://lucide.dev) icon; declare an `IconSource` bean to add your own.

## Building

```shell
mvn install
```

The build downloads Node.js and pnpm through the frontend-maven-plugin.

See `samples/shadleaf-sample-01` for a consuming application, and `CLAUDE.md` for the live-reload development loop.

## License

Apache License 2.0. See [LICENSE](LICENSE).

The bundled icons are [lucide](https://lucide.dev) (ISC licence); the jar ships its licence as
`shadleaf/icons/LICENSE-lucide.txt`.
