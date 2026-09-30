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
application (see `field.yaml` and `form-errors.yaml`).

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

`.github/workflows/publish-docs.yml` builds the site once per destination, with `DOCS_BASE` as Astro's `base`:
`/shadleaf/<version>/` for every release, and `/shadleaf/current/` for the newest one. Every link and asset therefore
goes through `import.meta.env.BASE_URL` (`withBase()` in `src/lib/generated.ts`), never a hard-coded `/`.

It runs on every published release, by hand, and from the Build workflow after the `docs` job passed on a push to
`main`. That last one reuses the previews the `build` job rendered and publishes `/shadleaf/<version>-SNAPSHOT/`;
`current/` only moves for releases. Until the first release, the site root redirects to the published version.
