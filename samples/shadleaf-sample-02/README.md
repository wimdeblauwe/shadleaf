# Shadleaf Sample App 2

Shadleaf behind Spring Security with a strict Content-Security-Policy, and no Node in the build: the application's own
CSS is a plain static file.

- **CSP:** no inline script or style, no `unsafe-eval`. The page loads Alpine's CSP build (`shadleaf.assets.alpine=csp`)
  and allows the theme script by a nonce per request or by its hash (`sample.csp.mode=nonce|hash` in
  `application.properties`).
- **Header layout:** the navigation as a row of links in the site header, the same nav as a panel on a phone, and the
  user menu at the end (see the [Application shell](https://wimdeblauwe.github.io/shadleaf/current/guides/application-shell/)
  guide).
- **Form login** with two users: `ada` (an admin) and `grace`, both with password `password`. Admins see an Admin page
  (navigation by role with `sec:authorize`, the URL protected in `SecurityConfiguration`). Sign out posts with the
  CSRF token.
- **htmx** on every page (`hx-boost` on the body); a request made after the session expired is sent to the sign-in page
  instead of having it swapped into the page (htmx-spring-boot's `HxRedirectToPage*` classes).

Pages: Home, Forms, Overlays (dialog, menu, popover, tooltip, toast), Data (tabs and a table), Admin, Settings, Help.

## Running it

The sample depends on the starter as a SNAPSHOT, so install it first, from the repository root:

```shell
mvn install
```

Then, in this directory:

```shell
mvn spring-boot:run
```

and open http://localhost:8080. Sign in as `ada` or `grace` (password `password`). Open the browser's console to see
that the policy blocks nothing.

### From an IDE

Set the run configuration's working directory to this directory (`samples/shadleaf-sample-02`). IntelliJ IDEA
defaults to the directory of the project you opened, which is the repository root when you open the whole repository.
Other IDEs may need the same.
