# Application shell

> Choosing between a sidebar and a header for the frame around every page, and what every layout needs whichever you pick.

Source: https://shadleaf.dev/current/components/application-shell/

The application shell is what every page shares: the navigation, a header, and the places where dialogs and toasts
appear. Shadleaf has two kinds of layout for it, both built from the [Sidebar](https://shadleaf.dev/current/components/sidebar.md) family:

- **A sidebar**: the navigation in a column beside the page, as in [Collapses to icons](https://shadleaf.dev/current/layouts/collapses-to-icons.md),
  which collapses to a strip of icons on a desktop.
- **A header**: the navigation as a row of links in a [site header](https://shadleaf.dev/current/components/site-header.md) across the top, as in
  [Navigation in the header](https://shadleaf.dev/current/layouts/header.md).

In both, the navigation is one `<sl:sidebar>`. Below 768 px the menu button opens that same element as a panel over
the page, so the layouts work the same way on a phone. The [Layouts](https://shadleaf.dev/current/layouts.md) section has each as a whole layout file with a page
that uses it and its CSS. Copy one into your application (the samples keep theirs in `templates/layout/main.html`) and
change what you need: the templates are yours, and so is the CSS around the components. This page helps you pick one
and says what every layout needs, whichever you pick.

## Which one

|                         | [A sidebar](https://shadleaf.dev/current/layouts/collapses-to-icons.md)                                 | [A header](https://shadleaf.dev/current/layouts/header.md)                                          |
|-------------------------|--------------------------------------------------------------------------------|----------------------------------------------------------------------------|
| Pages in the navigation | Many, in labelled groups, with sub-menus                                       | A handful of top-level pages                                               |
| On a desktop            | A column the user collapses to icons (or out of view) and the cookie remembers | A row of links that scrolls sideways when it does not fit; never collapses |
| Page header             | Inside the page: trigger, breadcrumb, theme toggle                             | The site header itself: name, links, theme toggle                          |
| Room for the page       | The width minus the sidebar (3 rem collapsed to icons)                         | The full width                                                             |
| On a phone              | The panel                                                                      | The same panel                                                             |
| Sample                  | sample-01                                                                      | sample-02 (strict Content-Security-Policy)                                 |

Pick a sidebar when the application has more pages than fit in a header row or needs groups and sub-menus. Pick
the header for a smaller application. Moving from one to the other later means changing the layout template, not the
pages: both take the same `pageTitle` and `content` (a sidebar layout takes the breadcrumb's levels too). The other
[layouts](https://shadleaf.dev/current/layouts.md#the-layouts) are sidebars as well: grouped by section, with sub-menus, floating, on the
end, and more, most of them after one of shadcn/ui's sidebar blocks.

## What every layout needs

Whichever you choose, the layout template has the same pieces. Every layout in the Layouts section has all of them:

- **In the `head`**, `~{sl/layout :: theme-script}` and then `~{sl/layout :: assets}`, then your own stylesheet. The
  theme script sets the dark class before the first paint. Under a strict policy it needs its nonce or hash (see
  [Content-Security-Policy](https://shadleaf.dev/current/guides/csp.md)).
- **`<sl:skip-link/>` first in the body.** It lets a keyboard user jump past the navigation to the page.
- **One `main`:** `<sl:sidebar-inset>` renders `<main id="main" tabindex="-1">`, the skip link's target. Put nothing
  of the page outside it.
- **The page's title** in the `<title>` and as the page's `h1`, from the same `pageTitle` parameter.
- **The current page** marked in the navigation with `th:active="${#slNav.current('/people')}"`, which reads the
  request, so no controller has to say which page it is.
- **`<sl:toaster>` at the end of the body**, with the toasts a controller put in the `toasts` flash attribute before a
  redirect (see [Toast](https://shadleaf.dev/current/components/toast.md)).
- **A `<div id="modal-root">`**, if htmx fetches dialogs into the page (see [Dialog](https://shadleaf.dev/current/components/dialog.md)).

The CSS for the page itself (its padding, the `h1`, the logo in the sidebar) is the application's. Shadleaf's CSS styles
its components, not the page around them.

## A page without the navigation

A sign-in page has nothing to navigate to yet. Sample-02 gives it a layout of its own: the site header with only the
name and the theme toggle, and the page in a plain `main` with the skip link's target:

```html
<body>
<sl:skip-link/>
<sl:site-header>
  <sl:button as="a" th:href="@{/}" variant="ghost">
    <sl:slot name="icon-start"><sl:icon name="leaf"/></sl:slot>
    Acme
  </sl:button>
  <sl:slot name="end"><sl:theme-toggle/></sl:slot>
</sl:site-header>
<main id="main" class="page" tabindex="-1">
  <h1 th:text="${pageTitle}"></h1>
  <div th:replace="${content}"></div>
</main>
</body>
```

Without `<sl:sidebar-provider>` the site header lies across the top of the page, on the page's colour with a border
below.

## With htmx

`hx-boost` on the body (as [Collapses to icons](https://shadleaf.dev/current/layouts/collapses-to-icons.md) has it) loads every page with htmx. The server renders the whole page
each time, so the current page, the breadcrumb and the sidebar's collapsed state are always right, and a link in the
phone panel closes it. The [Sidebar](https://shadleaf.dev/current/components/sidebar.md#htmx) page says what happens with history and when only
the main area is boosted.

A boosted link sends `HX-Request` too. A controller that answers other htmx requests with a fragment (a table's rows,
a dialog) has to tell such a request apart, or a link in the navigation gets the fragment without the page around it.
A request from the body's boost has `HX-Boosted` and no `HX-Target`, because the body has no id. With
[htmx-spring-boot](https://github.com/wimdeblauwe/htmx-spring-boot):

```java
static boolean wantsFragment(HtmxRequest request) {
  return request.isHtmxRequest()
      && !request.isHistoryRestoreRequest()
      && !(request.isBoosted() && request.getTarget() == null);
}
```

A link inside an element with an `hx-target` of its own (a table's sort and page links, boosted to swap only the
table) does send `HX-Target`, and still gets its fragment. See [Tables with htmx](https://shadleaf.dev/current/guides/tables-htmx.md).

A boosted link or form made after the session expired would get the sign-in page swapped into the body (or, for a
form, nothing at all, as its CSRF token went with the session). The Spring Security guide shows how to send the browser
to the page instead, which then asks the user to sign in and comes back to it:
[htmx after the session expired](https://shadleaf.dev/current/guides/spring-security.md#htmx-after-the-session-expired).

## A sidebar below a full-width header

shadcn/ui's sidebar-16 puts a header across the whole top of the page and the sidebar below it. Shadleaf does not
support that layout. From 768 px the sidebar is sticky at the top of the screen and as tall as the screen, so below a
header it would slide under the header and run past the bottom of the screen. `<sl:sidebar-provider>` has to start
at the top of the page. Put the page's header inside `<sl:sidebar-inset>` (a sidebar layout), or put the navigation
in the header ([Navigation in the header](https://shadleaf.dev/current/layouts/header.md)).

## The signed-in user

Both layouts have a place for the user: the sidebar's footer, and the site header's `end` slot. The
[user menu](https://shadleaf.dev/current/components/user-menu.md) goes in either (`placement="header"` in the site header): the signed-in user from
Spring Security with a menu that signs out, or a Sign in link for an anonymous visitor, so one layout serves public
pages too. Sign out posts a form with the CSRF token; `hx-boost="false"` on it keeps a boosted body from sending it
with htmx. Where the user comes from, and how to change it, is in the [Spring Security](https://shadleaf.dev/current/guides/spring-security.md#the-signed-in-user)
guide.

## Navigation by role

Some pages are only for some users. Hide their links with the Thymeleaf extras for Spring Security, which Spring Boot
configures when the dependency is there (also with Spring Security 7, under its `springsecurity6` name):

```xml
<dependency>
  <groupId>org.thymeleaf.extras</groupId>
  <artifactId>thymeleaf-extras-springsecurity6</artifactId>
</dependency>
```

Declare the `sec` namespace on the layout's `<html>` (`xmlns:sec="http://www.thymeleaf.org/extras/spring-security"`)
and put `sec:authorize` on the menu item, so no empty list item is left behind:

```html
<sl:sidebar-menu-item sec:authorize="hasRole('ADMIN')">
  <sl:sidebar-menu-button th:href="@{/admin}" th:active="${#slNav.current('/admin')}">
    <sl:slot name="icon-start"><sl:icon name="shield"/></sl:slot>
    Admin
  </sl:sidebar-menu-button>
</sl:sidebar-menu-item>
```

There is one navigation, so the phone panel follows the same rule as the sidebar or the header row. A group whose
items are all for one role takes the attribute on the `<sl:sidebar-group>` instead, label included.

**Hiding a link is no security.** Anyone can type the address. What keeps other users out is the rule on the URL:

```java
.authorizeHttpRequests(requests -> requests
    .requestMatchers("/shadleaf/**").permitAll()
    .requestMatchers("/admin/**").hasRole("ADMIN")
    .anyRequest().authenticated())
```

A user without the role who opens `/admin` gets a 403. The same attribute works on content inside a page: a button
only admins may use, a column only they may see. Sample-02 has both: an Admin page in its header row, and a Manage
access button on its Data page, shown to Ada (an admin) and not to Grace. With `sec:authorize-url="/admin"` instead,
the template asks whether the URL rule lets the user in, so link and rule cannot disagree.

Test both sides with spring-security-test: the link is in the page for one user and not for another, and the URL
answers 403 without the role.

```java
mockMvc.perform(get("/admin").with(user("grace").roles("USER")))
    .andExpect(status().isForbidden());
```

The [Spring Security](https://shadleaf.dev/current/guides/spring-security.md#navigation-by-role) guide has the same in short, next to where the
signed-in user comes from.
