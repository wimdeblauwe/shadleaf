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

### Spring Security and Content-Security-Policy

Permit the library's assets with one matcher, `requestMatchers("/shadleaf/**").permitAll()`. The dark-mode script
(`~{sl/layout :: theme-script}`) is inline, and a strict policy can allow it in either of two ways:

- **Nonce**: put a per-request nonce in the `cspNonce` request attribute (`shadleaf.csp.nonce-attribute`) and the
  script renders it as `nonce="…"`.
- **Hash**: add `ShadleafThemeScript#getCspHash()` (a `'sha256-…'` source) to `script-src`.

`samples/shadleaf-sample-02` does both, with no `'unsafe-inline'`.

`<sl:button>` renders `type="button"` unless you say `type="submit"`, so a button in a form never submits by accident.
`<sl:icon>` inlines the SVG of any [lucide](https://lucide.dev) icon; declare an `IconSource` bean to add your own.

## Building

```shell
mvn install
```

The build downloads Node.js and pnpm through the frontend-maven-plugin.

See `samples/shadleaf-sample-01` for a consuming application, `samples/shadleaf-sample-02` for one behind Spring
Security with a strict Content-Security-Policy (and no Node in its build), and `CLAUDE.md` for the live-reload
development loop.

Component markup is pinned by approval files in `shadleaf-spring-boot-starter/src/test/resources/approved/`. After a
deliberate change, accept the new markup with `mvn test -Dshadleaf.approve`, review the diff and commit it.

## License

Apache License 2.0. See [LICENSE](LICENSE).

The bundled icons are [lucide](https://lucide.dev) (ISC licence); the jar ships its licence as
`shadleaf/icons/LICENSE-lucide.txt`.
