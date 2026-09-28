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

## Building

```shell
mvn install
```

The build downloads Node.js and pnpm through the frontend-maven-plugin.

See `samples/shadleaf-sample-01` for a consuming application, and `CLAUDE.md` for the live-reload development loop.

## License

Apache License 2.0. See [LICENSE](LICENSE).
