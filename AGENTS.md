# Shadleaf

shadcn/ui for Thymeleaf, packaged as a Spring Boot starter.

This file is for coding agents and contributors working **on** Shadleaf. People using the library find its
documentation at https://shadleaf.dev/current/ (as Markdown: `llms.txt`). For how one component
works, read its template, CSS, script, tests and docs page; this file holds what applies to all of them.

## Repository layout

- `shadleaf-spring-boot-starter/`: the library, the only module of the Maven reactor.
  - `src/main/resources/templates/sl/components/<name>.html`: one template per component or part.
  - `src/main/resources/static/css/`: `tokens.css`, `page.css`, `components/<name>.css` (structure),
    `skins/<skin>.css` (look). `static/js/`: `components/<name>.js` (Alpine components), shared modules, `entries/`.
    These are Vite sources; only Vite's hashed output goes into the jar.
  - `src/main/java/io/github/wimdeblauwe/shadleaf/`: dialect and engine (`dialect`, `component`), assets, icons,
    expression objects (`nav`, `paging`, `security`, `sidebar`, ...), auto-configuration.
  - `src/test/`: tests per component package, the test fixtures in `...shadleaf.test`, approved files in
    `src/test/resources/approved/`, preview YAML in `src/test/resources/previews/`.
- `samples/shadleaf-sample-01|02|03/`: standalone Maven projects, not in the reactor (see Samples).
- `docs/`: the documentation site, a standalone pnpm project (Astro + Starlight), with the Playwright browser tests in
  `docs/tests/`. `docs/README.md` explains the generation pipeline.
- `branding/`: the logo (SVG and PNG), its colours and typeface; written by `docs/scripts/logo.mjs` (`pnpm run logo`).
- `.github/`: CI (`build.yml`), docs publishing, the release workflow.
- `references/`: git-ignored clones of the reference projects (see Reference material).

## Building and running

- `mvn install` in the repository root builds and tests the starter; the samples depend on the SNAPSHOT, so run it
  first. `-DskipTests` also skips `PreviewGeneratorTest`, which feeds the docs, so they get stale CSS.
- Each sample: `mvn verify` in its own directory (sample-03 starts Keycloak with Testcontainers: it needs Docker).
- Docs: `pnpm run generate:watch` and `pnpm run dev` in `docs/`; `pnpm run build && pnpm test` for the browser tests
  (`pnpm exec playwright install chromium firefox` once). Run the starter's tests first.
- Accept changed approval files with `-Dshadleaf.approve`; `-Pperformance` runs the performance tests.

Live reload while working on components:

1. `mvn install` in the repository root.
2. `pnpm run dev` in `shadleaf-spring-boot-starter` (library Vite server, port 5174).
3. `pnpm run dev` in `samples/shadleaf-sample-01` (sample Vite server, port 5173).
4. `mvn spring-boot:run -Dspring-boot.run.profiles=local` in the sample directory.
5. Open http://localhost:5173. Changes to the library CSS or `templates/sl/**` reload the browser.

## Working agreements

- Do not commit until asked.
- Commit messages follow [Conventional Commits](https://www.conventionalcommits.org/) (`feat:`, `fix:`, `docs:`,
  `build:`, `test:`, `refactor:`, `chore:`). There is no CHANGELOG.md: JReleaser builds release notes from the
  commits. No breaking-change markers (`!` / `BREAKING CHANGE:`) before 1.0.0; until then markup may change freely.
- Follow shadcn/ui: its component names, part names, prop names and variant names, unless there is a reason not to.
  Every deviation a shadcn user should know goes on the docs page `coming-from-shadcn.mdx`.
- Settled decisions (ask before revisiting): no Spring Boot 3.x; Java 17 baseline, Spring Boot 4, Thymeleaf 3.1,
  Tailwind CSS v4; the `sl:` prefix is fixed (namespace URI `https://shadleaf.dev/sl`, only an identifier);
  configuration under `shadleaf.*`; no htmx in the starter; two skins (`vega`, the default, and `lyra`) until 1.0;
  no `shadleaf-test` module yet; ttcli integration just before 1.0; a sidebar below a full-width header is
  unsupported.

## Reference material (read-only)

`scripts/clone-references.sh` clones the repositories into `references/` (git-ignored); when a clone exists, read it
there instead of fetching from the web.

| What | Where | Local clone |
|---|---|---|
| shadcn/ui docs (the APIs Shadleaf follows) | https://ui.shadcn.com/docs/components | |
| shadcn/ui sources | https://github.com/shadcn-ui/ui | `references/shadcn-ui` |
| basecoat (shadcn styles as plain CSS; the skins follow its `styles/<name>.css`) | https://github.com/hunvreus/basecoat | `references/basecoat` |
| starting-point-ui (CSS reference) | https://github.com/gufodotdev/starting-point-ui | `references/starting-point-ui` |
| "Writing a Thymeleaf component library", parts [1](https://www.wimdeblauwe.com/blog/2026/07/27/writing-a-thymeleaf-component-library/), [2](https://www.wimdeblauwe.com/blog/2026/08/03/writing-a-thymeleaf-component-library---part-2/), [3](https://www.wimdeblauwe.com/blog/2026/08/10/writing-a-thymeleaf-component-library---part-3/), [4](https://www.wimdeblauwe.com/blog/2026/08/17/writing-a-thymeleaf-component-library---part-4/): the design of the engine | wimdeblauwe.com | |
| The series' code, which Shadleaf started from | https://github.com/wimdeblauwe/blog-example-code (`thymeleaf-component-library`) | `references/blog-example-code` |

## How the component engine works

- **Registry.** Components are read from the templates at runtime: the classpath once at startup, and the
  `shadleaf.dev.templates-path` directory again when a file changes. There is no build-time snapshot, because an
  application may override a template and change its props. An application's copy of a library template (any
  classpath root other than the library's) overrides it. Web-types and the docs' JSON are generated from the registry.
- **Props.** A template declares its API in `<sl:props>` in its `<head>`: `<sl:description>`, `<sl:prop name type default
  values required>` (`type` string (the default), `boolean`, `number`, `object` (expression-only, passed through
  untouched); an enum when `values` is set), and `<sl:accessible-name required-when="size=icon icon-sm"/>` when an accessible name is
  needed (always, without `required-when`). `<code>` in descriptions becomes backticks in the docs. A required prop
  fails the render when missing or null. Prop names that clash with Spring's standard dialect are reserved (`class`,
  `src`, `value`, `errors`, ...), except the plain attribute setters (`th:size`, `th:type`, `th:disabled`); pick
  another name, as `active` for tabs. Literal prop values are HTML-unescaped; a boolean prop defaulting to true is an
  enum `true|false` in web-types.
- **Template variables.** A component template sees `props` (always set; `props.unlessDefault('variant')` is null
  for the default value), `attrs` (the attributes that are no prop, an `Attrs` map with `without(...)`, `only(...)`,
  `has(...)`) and `slots` (`has('x')`, `hasDefault()`, `contains(name, notInside...)`, which scans the unevaluated
  content). `sl:attrs` writes `attrs` (or the given expression, `sl:attrs="${attrs.without('href')}"`) onto the
  element and appends `class`.
- **Slots.** `<sl:slot/>` (default) and `<sl:slot name="x"/>`; content inside it is the fallback. Content the caller
  provided sees the **caller's** `props`/`attrs`/`slots`; fallback content sees the template's. Other local
  variables of the receiving template still hide the caller's, so every `th:with` in a library template uses an
  `sl` prefix (`slIconOnly`, `slDialogId`). To pass a named slot on to a nested component, write
  `<sl:slot name="x"><sl:slot name="x"/></sl:slot>`.
- **Precedence.** `sl:attrs` runs at 750, `th:attr` at 700, the component processor at 1000 (before `th:href`,
  `th:src`, `th:action` and the attribute setters, by class name). So put class and role in `th:attr`, never
  `th:class` (1000, it would overwrite the class `sl:attrs` appended), and attributes forwarded through `sl:attrs`
  to a nested `sl:` component reach its props. Attributes on a nested component are evaluated in its template:
  pass values in through `sl`-prefixed `th:with` locals.
- **Forms.** `th:field` passes through `sl:attrs`; Spring's processors run on the rendered element. Controls get
  their field binding, id, `aria-describedby` and `aria-invalid` from `#slFields` (see `field.html` and
  `input.html`); new form controls follow the same pattern.
- **Expression objects** (dialect, so they work without Spring beans): `#slIcons`, `#slFields`, `#slPaging`, `#slNav`,
  `#slUser`, `#slSidebar`. Templates cannot use `@bean` or `#ctx` inside `th:attr`/`th:src`; read them with `th:with`.
- **Optional dependencies** (Spring Data, Spring Security, OAuth2 client): a single class touches each, guarded by
  a `PRESENT` check, and the starter must work without them (`WithoutSpringSecurityTest` runs in its own surefire
  execution with those jars removed).
- **Messages** live in `shadleaf/messages.properties` (`sl.*`), attached as the last parent of the application's
  `messageSource`. Every visible or accessible text has a message key; a caller's `aria-label` replaces the default
  (take it from `attrs`, see `theme-toggle.html`).
- **Icons** are Lucide (`lucide-static`, pinned), turned into `lucide.json` at build time; `IconSource` beans are
  asked first. `<sl:icon name>`; icon-only buttons need an accessible name.

## Writing a component

### Templates

- One template per component or part (`card.html`, `card-header.html`, ...). Parts never know their parent in
  markup; a parent passes data to its parts with an `sl`-prefixed `th:with` around `<sl:slot/>` (see `dialog.html`).
- Several element types (`as="button|a"`): one branch per element in one fragment, sharing their content through
  `~{sl/components/<name> :: <name>-content}`, spelled out (`~{::x}` looks in the calling template). A component that
  renders no element of its own has a `th:block` root.
- Markup: a class named after the component (not shadcn's `data-slot`), `data-variant` / `data-size` only for
  non-default values. `th:attr` drops an attribute whose value is `''`, so a presence-style data attribute needs a
  real value (`data-border="bottom"`).
- Comments inside a fragment must be parser-level (`<!--/* */-->`), or they reach every page. Never name a fragment
  after an HTML element: it also matches that element.
- `th:text` on a component replaces its whole content, icons included; docs tell users to write `[[${...}]]`.
- Native elements first: `<dialog>` (invoker commands, `closedby`), the Popover API with CSS anchor positioning,
  `<details>`, `hidden="until-found"`, customizable `<select>`. Check browser support (Chrome, Firefox, Safari) when
  relying on a new platform feature, and add a stand-in when one of them lacks it.

### CSS

- `components/<name>.css` holds structure, `skins/<skin>.css` the identity (colours, radius, sizes, focus ring),
  for **both** skins. A new file in `css/components/` is picked up by `scripts/css-entries.js`.
- Skins use tokens only (shadcn's names, `tokens.css`), never literal colours. Text on `--muted` or destructive text
  uses `text-muted-legible` / `text-destructive-legible` (shadcn's pairs fail 4.5:1).
- Utility classes are no API: the standalone bundle has none. What shadcn does with a class (`border-b`) is a prop.
- Parent-to-part rules always use `>` (`.card[data-size='sm'] > .card-header`), so nested components don't leak.
- Use logical properties (RTL), `motion-safe:` or `prefers-reduced-motion` for movement, and check forced colours
  (`outline-none`, not `outline-hidden`; `forced-colors:` variants).
- CSP: no inline styles and no `data:` images. Set dynamic values from JavaScript through CSSOM.
- The embedded bundle must not emit Tailwind theme variables: write `--spacing(n)`, never `var(--spacing)`.
- Tailwind silently drops an arbitrary variant (`[&_svg:not(...)]:size-4`) from an `@apply` in the skins; write a
  plain rule.

### JavaScript

- Alpine.js, under five rules: only expressions Alpine's CSP build accepts (logic in `Alpine.data`), never a second
  Alpine on the page, `sl`-prefixed names, native elements first, test htmx swaps.
- `static/js/components/<name>.js` default-exports one `Alpine.data` function, registered as `sl<Name>`. Behaviour
  that must run without Alpine (avatar images, indeterminate checkboxes) is a module next to `register.js`, called by
  all three entries (`bundled`, `csp`, `external`). Shared helpers: `popup.js`, `tooltip.js`, `table-rows.js`.
- Declare every property a component sets on `this` (`cleanups: []`): Alpine puts an undeclared one on the outermost
  `x-data`, shared by every component on the page.
- Without JavaScript a component must still work or degrade sensibly; the server renders the state.
- htmx: a component must survive being swapped in, swapped out and restored from history (`htmx:beforeHistorySave`).
  Events are `sl-`-prefixed (`sl-dialog-close`, `sl-toast`) so an `HX-Trigger` header can send them.

### Accessibility

- axe runs on the showcase (every preview scenario), at rest, on hover and with popups open; every interactive element needs a visible
  `:focus-visible` style in both skins.
- `aria-label` on a `<span>`, `<div>` or `<kbd>` (generic role) is prohibited; titles are `role="heading"` with
  `aria-level` (a `level` prop), not `h1`–`h6`, so an app's heading styles don't apply.
- No `role="alert"` by default on server-rendered content: a full page load would announce it.

## Testing a component

- **Java** (starter `src/test`, a package per component): `ComponentRenderTester` (`create()` or `builder()`: icon
  sources, messages, locale, request URI, cookies, Spring Security users) renders a snippet; assert with
  `ShadleafAssertions` (`assertThat(Rendered)`, `assertThatRenderFailure`). `FormModel` gives a `th:object` with
  errors. An approval test (`HtmlApproval.verifyRenders(name, tester, snippets)`, cases from the `<sl:props>` values)
  writes `approved/<name>.approved.html`. Test-only components are `src/test/resources/templates/sl/components/test-*.html`.
- **Checks every component passes:** `SkinCompletenessTest` renders every enum value and requires, in every bundle,
  a selector for each rendered `data-*` value, an explicit default rule (`.x:not([data-size])`), `:focus-visible`
  for focusable elements, `:disabled`/`[readonly]`/`:indeterminate` where they apply; `CssBundleHygieneTest` and
  `JsBundleHygieneTest` check the bundles.
- **Preview YAML** (`src/test/resources/previews/<component>.yaml`, required for every component; a family shares one
  with `parts:`): the scenarios of the docs page and the showcase. Ids must be unique across the whole showcase, every
  control labelled, and landmarks named uniquely; `showcase: false` keeps a scenario off the showcase.
- **Docs page:** `docs/src/content/docs/components/<name>.mdx`, listed in `docs/src/component-groups.mjs` (the build
  fails otherwise). A new MDX component needs a case in `docs/src/lib/markdown.ts` (the `.md`/`llms.txt` output).
- **Browser tests** (`docs/tests/<name>.spec.ts`) for anything with behaviour: the showcase in both skins and themes,
  and fixture pages (`fixture.ts`: strict CSP, the `csp` build, Alpine on/off, htmx, served routes). Add the spec to
  the Firefox project's `testMatch` in `docs/playwright.config.ts` when browsers differ.
- Pitfalls: jsoup drops markup inside `<select>` (`Rendered` works around it) and finds elements inside
  `<template>`s; Playwright's role queries see neither `summary`, hidden popover content nor `inert`; focus after
  opening a popup lands a task later (`expect.poll`); wait for `.htmx-settling` to go before pressing Tab after an
  htmx swap; Playwright does not route the request a fulfilled 302 leads to; `astro preview` runs with
  `--ignore-lock` on port 4399 (Astro backgrounds it when it detects an AI agent).

## Samples

Standalone Maven projects depending on the starter's SNAPSHOT; CI builds each as its own step.

- **sample-01**: every component, with Vite live reload in the `local` profile, htmx (htmx-spring-boot) with a
  boosted body, Spring Data JPA on H2 for the tables, a demo `UserSource` instead of Spring Security. A boosted body
  sends `HX-Request` too: controllers decide fragment or page with `HtmxRequests.wantsFragment`.
- **sample-02**: Spring Security with a strict CSP (nonce or hash), the `csp` Alpine build, the header layout,
  navigation by role, htmx after an expired session (htmx-spring-boot's entry point and access-denied handler).
- **sample-03**: OAuth2 login against Keycloak (`compose.yaml`; Testcontainers in the tests), optionally GitHub.

## Documentation site

`PreviewGeneratorTest` renders the preview YAML through `ComponentRenderTester` into `target/generated-docs/`
(`previews.json`, the CSS bundles and scripts, `components.json`, `web-types.json`); `docs/scripts/sync.mjs` copies it
into the site. Layout pages come from `previews/layouts/*.yaml`. Every URL goes through `withBase()`; the site is
published per version on GitHub Pages by `publish-docs.yml`. `/showcase/` renders every scenario on one bare page for
the axe, focus and hover checks. Every page is also served as Markdown, plus `llms.txt` and friends for coding agents
(`docs/src/lib/markdown.ts`); keep `coding-agents.mdx`, the `llms.txt` intro and `context7.json`'s `rules` in step.

## Releases

`release.yml` (manual, on `main`): `set-version.sh` sets the library's and the samples' versions, commit
`chore(release): <v>`, tag `<v>` (no `v` prefix), build, push, deploy to Maven Central, GitHub release with
JReleaser (notes from the commits since the last release), docs, next SNAPSHOT. Every `main` build of a SNAPSHOT
refreshes an early-access pre-release.
