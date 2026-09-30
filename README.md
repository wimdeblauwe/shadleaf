# Shadleaf

shadcn/ui for Thymeleaf, packaged as a Spring Boot starter.

> **Status:** pre-release, under active development. Before 1.0, the rendered markup can change between versions.

Shadleaf brings the [shadcn/ui](https://ui.shadcn.com) components to server-rendered Spring Boot applications. It is
a Thymeleaf dialect with a library of components, each one an ordinary Thymeleaf template, that you use as custom tags:

```html
<sl:button variant="destructive" hx-delete="/orders/42">
  <sl:slot name="icon-start"><sl:icon name="trash"/></sl:slot>
  Delete order
</sl:button>
```

- **Nothing to install on the frontend.** The jar ships compiled Tailwind CSS and serves it under `/shadleaf/**`, so
  your application needs no Node.js and no Tailwind. If you do compile Tailwind yourself, an embedded bundle fits into
  your build.
- **shadcn's design tokens, verbatim.** `--primary`, `--radius`, `--ring`: a shadcn theme drops in unchanged, with
  light and dark mode.
- **Checked props.** Each component declares its props, so `variant="destructve"` or an icon-only button without an
  accessible name fails with a clear message instead of reaching the browser.
- **Adjustable step by step**, from a prop to overriding the component's template in your own application.
- **Works under a strict Content-Security-Policy** and with Spring Security, and completes in IntelliJ IDEA through
  web-types.

The [documentation](https://wimdeblauwe.github.io/shadleaf/) shows every component with live previews, rendered by the
library itself. Start with [Getting started](https://wimdeblauwe.github.io/shadleaf/current/getting-started/) to add
the starter to your application.

## Compatibility

| Shadleaf                                                            | Spring Boot | Minimum Java version | Docs                                                                 |
|---------------------------------------------------------------------|-------------|----------------------|----------------------------------------------------------------------|
| [0.3.0](https://github.com/wimdeblauwe/shadleaf/releases/tag/0.3.0) | 4.1.x       | 17                   | [Documentation 0.3.0](https://wimdeblauwe.github.io/shadleaf/0.3.0/) |
| [0.2.0](https://github.com/wimdeblauwe/shadleaf/releases/tag/0.2.0) | 4.1.x       | 17                   | [Documentation 0.2.0](https://wimdeblauwe.github.io/shadleaf/0.2.0/) |
| [0.1.0](https://github.com/wimdeblauwe/shadleaf/releases/tag/0.1.0) | 4.1.x       | 17                   | [Documentation 0.1.0](https://wimdeblauwe.github.io/shadleaf/0.1.0/) |

## Building

```shell
mvn install
```

The build downloads Node.js and pnpm through the frontend-maven-plugin.

`docs/` is the documentation site (Astro + Starlight), with previews rendered by the library's own build; see
`docs/README.md`.

See `samples/shadleaf-sample-01` for a consuming application, `samples/shadleaf-sample-02` for one behind Spring
Security with a strict Content-Security-Policy (and no Node in its build), and `CLAUDE.md` for the live-reload
development loop.

Component markup is pinned by approval files in `shadleaf-spring-boot-starter/src/test/resources/approved/`. After a
deliberate change, accept the new markup with `mvn test -Dshadleaf.approve`, review the diff and commit it.

## Releasing

Commit messages follow [Conventional Commits](https://www.conventionalcommits.org/); the release notes are generated
from them by [JReleaser](https://jreleaser.org). There is no CHANGELOG file.

### Snapshot pre-release

Every build of `main` (the Build workflow) recreates the snapshot pre-release on the GitHub releases page, tagged with
the development version (e.g. `0.3.0-SNAPSHOT`): the tag moves to the new commit and the notes list every commit since
the last release, i.e. what the next release would contain. No versions change and nothing is published to Maven
Central. The tag is the version rather than `early-access` so GitHub lists it above the last release. Delete the
snapshot's release and tag by hand once its version has been released.

### A release

Run the **Release** workflow from the Actions tab, on `main`, with the release version (e.g. `1.0.0`) and the next
development version (e.g. `1.1.0-SNAPSHOT`). It:

1. sets the release version in the library and the samples (`.github/scripts/set-version.sh`), commits
   `chore(release): 1.0.0` and tags the commit `1.0.0`;
2. builds and tests the library and the samples, then pushes the commit and the tag (atomically);
3. deploys the parent and the starter to Maven Central with the `release` profile (sources, javadoc, signatures,
   central-publishing-maven-plugin), waiting until they are published;
4. creates the GitHub release on the tag, with JReleaser's notes since the previous release;
5. publishes the docs to `/shadleaf/1.0.0/` and `/shadleaf/current/`;
6. sets the next development version, commits it and runs the Build workflow, which creates the pre-release of the next snapshot.

The `chore(release):` commits are left out of the notes. After a release, add a row to the
[Compatibility](#compatibility) table.

The workflow needs these repository secrets:

| Secret                   | Value                                                                  |
|--------------------------|------------------------------------------------------------------------|
| `MAVEN_CENTRAL_USERNAME` | Username of a [Central Portal](https://central.sonatype.com) user token |
| `MAVEN_CENTRAL_PASSWORD` | Password of that user token                                            |
| `MAVEN_GPG_KEY`          | ASCII-armored secret key: `gpg --armor --export-secret-keys <key id>`  |
| `MAVEN_GPG_PASSPHRASE`   | Passphrase of that key                                                 |

The workflow pushes to `main` with the `GITHUB_TOKEN`, so a branch protection rule on `main` must let GitHub Actions
bypass it.

### When a release fails halfway

- **Before the push** (build, tests or samples failed): nothing left the runner. Fix the problem and run the workflow
  again.
- **After the push, before Maven Central** (the deploy failed): the commit and the tag are on `main`. Nothing was
  published. Delete the tag and, if nothing landed on `main` since, reset `main` to before the version commit (or
  revert it), then run the workflow again:

  ```shell
  git push origin :refs/tags/1.0.0
  git push --force-with-lease=main:<version commit> origin <version commit>~1:main
  ```
- **After Maven Central** (the GitHub release, the docs or the next version failed): the version is published and
  cannot be published again. Finish the remaining steps by hand, from the tag:

  ```shell
  git fetch --tags && git checkout 1.0.0
  JRELEASER_GITHUB_TOKEN=$(gh auth token) mvn -N jreleaser:release -Dshadleaf.release.skip-tag=true
  ```

  The docs: run the **Publish docs** workflow on the `1.0.0` tag. The next version:
  `.github/scripts/set-version.sh 1.1.0-SNAPSHOT` on `main`, then commit and push.

## License

Apache License 2.0. See [LICENSE](LICENSE).

The bundled icons are [lucide](https://lucide.dev) (ISC licence); the jar ships its licence as
`shadleaf/icons/LICENSE-lucide.txt`.
