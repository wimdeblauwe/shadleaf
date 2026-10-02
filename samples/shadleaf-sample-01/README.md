# Shadleaf Sample App 1

A Spring Boot application that uses most Shadleaf components, in the sidebar layout (collapsing to icons) of the
[Application shell](https://wimdeblauwe.github.io/shadleaf/current/guides/application-shell/) guide, with htmx on
every page (`hx-boost` on the body).

| Page                  | What it shows                                                                                            |
|-----------------------|----------------------------------------------------------------------------------------------------------|
| `/`                   | Buttons: variants, sizes, icons, loading                                                                 |
| `/form`, `/htmx-form` | A bound form with fields, server-side validation and a form error summary, without and with htmx         |
| `/dialog`             | Dialog, alert dialog, sheet, dropdown menus, popover, tooltip and toasts, with htmx                      |
| `/settings`           | Tabs as links (one panel loaded with htmx), collapsible, accordion                                       |
| `/people`             | A table from Spring Data JPA (in-memory H2): sorting, pagination, page size, live search, a menu per row |
| `/people-multiselect` | Row selection with a bulk delete                                                                         |
| `/people-load-more`   | Loading more rows with htmx                                                                              |

There is no Spring Security: a `UserSource` bean of the sample's own signs in a demo user, so the sidebar's user menu
has someone to show.

## Running it

The sample depends on the starter as a SNAPSHOT, so install it first, from the repository root:

```shell
mvn install
```

Then, in this directory:

```shell
mvn spring-boot:run
```

and open http://localhost:8080. The Maven build installs Node and pnpm itself (frontend-maven-plugin) and builds the
application's stylesheet with Vite.

### With live reload

To work on the library's templates and CSS with the browser reloading on every change, run the `local` profile with
both Vite dev servers:

1. `pnpm run dev` in `shadleaf-spring-boot-starter` (the library, port 5174)
2. `pnpm run dev` in this directory (the sample, port 5173)
3. `mvn spring-boot:run -Dspring-boot.run.profiles=local` in this directory
4. Open http://localhost:5173

The `local` profile reads the library's templates from `../../shadleaf-spring-boot-starter/...` and writes
`shadleaf.web-types.json` (IDE completion for the `<sl:*>` tags) here.

### From an IDE

Set the run configuration's working directory to this directory (`samples/shadleaf-sample-01`). IntelliJ IDEA
defaults to the directory of the project you opened, which is the repository root when you open the whole repository,
and the `local` profile's paths are relative to the working directory. Other IDEs may need the same.
