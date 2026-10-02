# Shadleaf Sample App 3

Shadleaf with OAuth2 login, as in the
[Signing in with OAuth2](https://wimdeblauwe.github.io/shadleaf/current/guides/oauth2/) guide:

- **Keycloak** (OpenID Connect): the user menu shows the name, email and picture from the ID token, and Sign out ends
  the session at Keycloak too (`OidcClientInitiatedLogoutSuccessHandler`), so the next Sign in asks for the password
  again.
- **GitHub** (OAuth2), with the `github` profile: the user menu shows the GitHub login as its second line, as GitHub
  sends no email by default. With two providers, Sign in goes to the sample's own page to choose one.
- **htmx** on every page (`hx-boost` on the body); a request made after the session expired is sent to the page and
  from there to the provider.

Pages: Home (what `#slUser` made of the sign-in) and Claims (everything the provider sent).

## Running it

You need Docker: the application starts Keycloak from `compose.yaml` (Spring Boot's Docker Compose support), on port
8180, with the realm in `keycloak/shadleaf-realm.json`.

The sample depends on the starter as a SNAPSHOT, so install it first, from the repository root:

```shell
mvn install
```

Then, in this directory:

```shell
mvn spring-boot:run
```

and open http://localhost:8080. Sign in on Keycloak's page as `ada` (with a picture) or `grace` (initials), both with
password `password`. Keycloak's admin console is at http://localhost:8180 (`admin` / `admin`).

Spring Boot stops the Keycloak container when the application stops (unless it was already running when the
application started); `docker compose down` in this directory removes it.

### With GitHub

Register an OAuth app at https://github.com/settings/developers with the authorization callback URL
`http://localhost:8080/login/oauth2/code/github`, then:

```shell
GITHUB_CLIENT_ID=... GITHUB_CLIENT_SECRET=... mvn spring-boot:run -Dspring-boot.run.profiles=github
```

### Tests

`mvn verify` starts its own Keycloak with Testcontainers (Docker again) and signs in and out over HTTP.

### From an IDE

Set the run configuration's working directory to this directory (`samples/shadleaf-sample-03`). IntelliJ IDEA
defaults to the directory of the project you opened, which is the repository root when you open the whole repository.
Spring Boot looks for `compose.yaml` in the working directory, and the tests read the realm from
`keycloak/shadleaf-realm.json` relative to it. Other IDEs may need the same.
