# Shadleaf docs

The documentation site: [Astro](https://astro.build) with [Starlight](https://starlight.astro.build). A standalone
pnpm project, not part of the Maven build.

## What is generated

The previews, the attribute tables, the downloadable web-types file and the CSP hash all come from the library build,
so the docs cannot drift from what the library renders. `PreviewGeneratorTest` in the starter writes them to
`shadleaf-spring-boot-starter/target/generated-docs/`:

- `previews.json`: every scenario in `src/test/resources/previews/*.yaml`, rendered through `ComponentRenderTester`
- `shadleaf/assets/*.css`: the standalone bundle of each skin
- `components.json`: the component registry export, behind `<ComponentAttributes>`
- `web-types.json`: the IDE metadata, published as `shadleaf.web-types.json`
- `theme-script.json`: the theme script and its CSP hash, behind `<CspHash>`

`scripts/sync.mjs` copies them into `src/generated/` and `public/` before every `dev` and `build`. Without them (a fresh
clone) the site still builds, and each page says which command to run.

To add examples for a component, add or edit its YAML file, then embed a scenario with
`<ComponentPreview id="button--variants" />`. A scenario with a `form` (`values` and `errors`, by field name, and
`globalErrors` for the whole form) renders inside `th:object="${form}"`, so `th:field` binds and shows errors as in an
application (see `field.yaml` and `form-errors.yaml`). `showcase: false` keeps a scenario off the showcase page, for
one that would get in the way of the other components' tests there (a toast shown when the page loads, in
`toaster.yaml`). `request` (`/people?sort=name,asc`) renders the scenario in a request for that path and query, for
links built from the request, such as a table's sort links (see `table.yaml`). `pages` puts Spring Data pages in the
model by name (`people: {number: 4, size: 10, total: 270}`, `number` zero-based; `slice: true` for a `Slice`), for a
pagination (see `pagination.yaml`). `width` (`390`) makes the frame that many pixels wide, for a scenario shown as on
a phone (the header layout's panel in `sidebar.yaml`).

## Commands

```shell
pnpm install
pnpm run generate          # render the previews (runs Maven) and sync them in
pnpm run generate:watch    # ... and again whenever a template, the CSS or a preview changes
pnpm run dev               # http://localhost:4321
pnpm run build             # into dist/
pnpm test                  # axe-core and the keyboard focus pass over /showcase/ (build first)
```

`pnpm exec playwright install chromium` once, before the first `pnpm test`.

## The showcase and its checks

`/showcase/` renders every scenario on one bare page with only the library's CSS; `?skin=lyra&theme=dark` selects the
combination. `tests/a11y.spec.ts` runs axe-core over it, resting and hovered, and `tests/focus.spec.ts` tabs through
every control and requires a visible change in outline or box-shadow. Both run for every skin, light and dark, in CI.

## Publishing

The site is served by GitHub Pages from the `gh-pages` branch, on the custom domain https://shadleaf.dev:

| Path                | What                                                                    | Written by                       |
|---------------------|-------------------------------------------------------------------------|----------------------------------|
| `/<version>/`       | The docs of every release, and the SNAPSHOT of every push to `main`     | `publish-docs.yml`               |
| `/current/`         | The newest release                                                      | `publish-docs.yml`, at a release |
| `/`                 | The home page, unversioned                                              | see [The home page](#the-home-page) |
| `/CNAME`            | The custom domain, written on every publish                             | `publish-docs.yml`               |

`.github/workflows/publish-docs.yml` builds the site once per destination, with `DOCS_BASE` as Astro's `base`
(`/0.9.0`, `/current`). Every link and asset therefore goes through `import.meta.env.BASE_URL` (`withBase()` in
`src/lib/generated.ts`), never a hard-coded `/`.

It runs from the Build workflow after the `docs` job passed on a push to `main` (reusing the previews the `build` job
rendered; it publishes `/<version>-SNAPSHOT/`), from the Release workflow (which also moves `current/`), and by hand,
with an optional `ref` to rebuild the docs of a tag.

## The home page

`src/pages/home.astro` (with `src/components/HomeDemo.astro`) is a Starlight page of its own, so it shares the docs'
look and live previews. `pnpm run dev` shows it at http://localhost:4321/home/.

Every build of the site renders it as `<base>/home/`, but it is published once, at the root, with its links and assets
pointing into `/current/`; no version directory keeps a copy. Its canonical URL is the root, and it is left out of
search. Two workflows write it:

- **A release** (`publish-docs.yml`, when `current/` moves): copies `current/home/index.html` to the root, and keeps
  the release's generated docs (`previews.json`, the bundles, ...) in `current/generated-docs/`.
- **Between releases** (`.github/workflows/publish-home.yml`): runs on every push to `main` that touches `docs/`, and
  by hand (Actions, "Publish home page", Run workflow). It builds `main`'s `docs/` against `current/generated-docs/`
  instead of the SNAPSHOT's previews, then copies `home/index.html` to the root and adds the new hashed files of
  `_astro/` to `current/_astro/` (the next release replaces `current/` and clears them out).

So a change to the home page's text or layout goes live with its push, without a release, while the page keeps
showing the released version, its components and links into the released docs. A change to a component shows on the
home page with the next release.

If `current/` has no `generated-docs/` (a release published before this existed), `publish-home.yml` skips with a
notice: run "Publish docs" by hand with `ref` set to that release's tag first.

## The domain

shadleaf.dev is registered at behostings, with its DNS at Netlify DNS (the registrar only sets the nameservers; the
domain is not attached to a Netlify site). The records point it at GitHub Pages:

| Type  | Name                                  | Value                                                                    |
|-------|---------------------------------------|--------------------------------------------------------------------------|
| A     | (apex)                                | `185.199.108.153`, `185.199.109.153`, `185.199.110.153`, `185.199.111.153` |
| AAAA  | (apex)                                | `2606:50c0:8000::153`, `2606:50c0:8001::153`, `2606:50c0:8002::153`, `2606:50c0:8003::153` |
| CNAME | `www`                                 | `wimdeblauwe.github.io`                                                  |
| TXT   | `_github-pages-challenge-wimdeblauwe` | the verification of the domain in the GitHub account's Pages settings    |

The repository's Pages settings have the custom domain and "Enforce HTTPS" on. GitHub redirects the former project
page, `https://wimdeblauwe.github.io/shadleaf/<path>`, to `https://shadleaf.dev/<path>`, so old links keep working.
