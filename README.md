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

### IDE completion

IntelliJ IDEA and WebStorm complete `<sl:*>` tags, their attributes and values from a web-types file. Set
`shadleaf.dev.web-types-file=shadleaf.web-types.json` in a development profile, and reference the file from a
`package.json` in the project root: `{"name": "my-app", "private": true, "web-types": "./shadleaf.web-types.json"}`.
The running application writes it from its own components, overrides included.

## Documentation

`docs/` is the documentation site (Astro + Starlight), with previews rendered by the library's own build; see
`docs/README.md`. It is published to https://wimdeblauwe.github.io/shadleaf/.

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

## Releasing

Commit messages follow [Conventional Commits](https://www.conventionalcommits.org/); the release notes are generated
from them by [JReleaser](https://jreleaser.org). There is no CHANGELOG file.

### Early access

Every build of `main` (the Build workflow) recreates the **early-access** pre-release on the GitHub releases page: the
`early-access` tag moves to the new commit and the notes list every commit since the last release, i.e. what the next
release would contain. No versions change and nothing is published to Maven Central.

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
6. sets the next development version, commits it and runs the Build workflow, which renews the early-access release.

The `chore(release):` commits are left out of the notes.

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
- **After the push, before Maven Central** (the deploy failed): the commit and the tag are on `main`. Delete the tag
  (`git push origin :refs/tags/1.0.0`), revert the version commit, and run the workflow again.
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
