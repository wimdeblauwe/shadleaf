# User Menu

> The signed-in user with a menu that signs out, or a Sign in link, for the sidebar's footer or the site header.

Source: https://shadleaf.dev/0.10.0/components/user-menu/

**Example: User menu.** In the sidebar's footer, the signed-in user as a large menu button: picture, name and email. It opens a menu beside the sidebar (below the button in the phone panel) with the user, the application's items and Sign out, which posts to #slUser.logoutUrl with the CSRF token. Collapsed to icons, the picture stays, with the name as a tooltip.

```html
<sl:sidebar-provider>
  <sl:sidebar collapsible="none">
    <sl:sidebar-content>
      <p>The application's navigation.</p>
    </sl:sidebar-content>
    <sl:sidebar-footer>
      <sl:user-menu>
        <sl:dropdown-menu-item as="a" href="/account"><sl:icon name="badge-check"/>Account</sl:dropdown-menu-item>
        <sl:dropdown-menu-item as="a" href="/billing"><sl:icon name="credit-card"/>Billing</sl:dropdown-menu-item>
        <sl:dropdown-menu-item as="a" href="/notifications"><sl:icon name="bell"/>Notifications</sl:dropdown-menu-item>
      </sl:user-menu>
    </sl:sidebar-footer>
  </sl:sidebar>
  <sl:sidebar-inset>
    <p>The page's content.</p>
  </sl:sidebar-inset>
</sl:sidebar-provider>
```

## Usage

Put it in the [sidebar](https://shadleaf.dev/0.10.0/components/sidebar.md)'s footer:

```html
<sl:sidebar-footer>
  <sl:user-menu>
    <sl:dropdown-menu-item as="a" th:href="@{/account}"><sl:icon name="badge-check"/>Account</sl:dropdown-menu-item>
    <sl:dropdown-menu-item as="a" th:href="@{/settings}"><sl:icon name="settings"/>Settings</sl:dropdown-menu-item>
  </sl:user-menu>
</sl:sidebar-footer>
```

It shows who is signed in, as Spring Security knows them: the user's picture, name and email, in a large
[menu button](https://shadleaf.dev/0.10.0/components/sidebar.md#menu-buttons) (shadcn's `NavUser`). The button opens a [dropdown menu](https://shadleaf.dev/0.10.0/components/dropdown-menu.md)
with the user at the top, then the items you put inside `<sl:user-menu>`, then **Sign out**. Without items there is
nothing between the user and Sign out but a separator.

The user comes from `#slUser` (see [Spring Security](https://shadleaf.dev/0.10.0/guides/spring-security.md#the-signed-in-user)), so no
controller adds anything to the model, and the menu works on every page, error pages included. The second line is the
email, or the username when there is no email (GitHub often sends none) and it is not the name already (a form
login's user is named by the username), or nothing.

## In the site header

`placement="header"` makes it a round button with the picture alone, for the `end` slot of a
[site header](https://shadleaf.dev/0.10.0/components/site-header.md), as in the header layout:

**Example: In the site header.** placement="header": a round button with the picture, named "Account: " and the user's name, at the end of a sl:site-header. The menu opens below it, lined up with its end.

```html
<sl:site-header>
  <sl:button as="a" href="/" variant="ghost">
    <sl:slot name="icon-start"><sl:icon name="gallery-vertical-end"/></sl:slot>
    Acme Inc.
  </sl:button>
  <sl:slot name="end">
    <sl:user-menu placement="header" id="user-menu-header">
      <sl:dropdown-menu-item as="a" href="/account"><sl:icon name="badge-check"/>Account</sl:dropdown-menu-item>
      <sl:dropdown-menu-item as="a" href="/settings"><sl:icon name="settings"/>Settings</sl:dropdown-menu-item>
    </sl:user-menu>
  </sl:slot>
</sl:site-header>
```

```html
<sl:site-header>
  ...
  <sl:slot name="end">
    <sl:theme-toggle/>
    <sl:user-menu placement="header">
      <sl:dropdown-menu-item as="a" th:href="@{/account}"><sl:icon name="badge-check"/>Account</sl:dropdown-menu-item>
    </sl:user-menu>
  </sl:slot>
</sl:site-header>
```

The button has no text, so it is named "Account: " and the user's name: a screen reader says "Account: shadcn, menu
button". The menu opens below it, lined up with its end.

## The picture

The picture goes through an [avatar](https://shadleaf.dev/0.10.0/components/avatar.md): while it loads, when the user has none and when it fails to load,
the initials show instead (from the name, or from the email's local part), and the user icon when there are no
initials either. A picture that is gone, as an identity provider's often are after a while, never shows a broken
image.

**Example: Without a picture.** A user without a picture, or whose picture fails to load, gets the initials (here from a GitHub sign-in: no email, so the username is the second line).

```html
<sl:site-header>
  <sl:slot name="end">
    <sl:user-menu placement="header" id="user-menu-initials"/>
  </sl:slot>
</sl:site-header>
```

## Nobody signed in

For an anonymous visitor the menu is a **Sign in** link instead: a menu button with the log-in icon in the sidebar, an
outline button in the header. It goes to `#slUser.loginUrl`: `/login`, the authorization endpoint of your one OAuth2
client (`/oauth2/authorization/github`), or what `shadleaf.security.login-url` says.

**Example: Nobody signed in.** An anonymous visitor gets a Sign in link to #slUser.loginUrl instead: /login, the one OAuth2 client's authorization endpoint, or shadleaf.security.login-url. In the sidebar a menu button, in the header an outline button.

```html
<sl:site-header>
  <sl:button as="a" href="/" variant="ghost">
    <sl:slot name="icon-start"><sl:icon name="gallery-vertical-end"/></sl:slot>
    Acme Inc.
  </sl:button>
  <sl:slot name="end">
    <sl:user-menu placement="header"/>
  </sl:slot>
</sl:site-header>
```

So one layout serves the sign-in page, a public page and the application. Without Spring Security on the classpath
nobody is ever signed in, and the menu is always the link.

## Signing out

**Sign out** is a POST, with Spring Security's CSRF token, to `#slUser.logoutUrl`: `/logout`, or what
`shadleaf.security.logout-url` says. The menu holds only menu items, so the form is not inside it: `<sl:user-menu>`
renders it after the menu, hidden, with the id `user-menu-sign-out`, and Sign out is a submit button in the menu that
names it (`form="user-menu-sign-out"`).

```html
<button type="submit" class="dropdown-menu-item user-menu-sign-out" role="menuitem"
        form="user-menu-sign-out">Sign out</button>
...
<form id="user-menu-sign-out" method="post" action="/logout" hx-boost="false" hidden>
  <input type="hidden" name="_csrf" value="...">
</form>
```

The form's `th:action` is what adds the token, as on every form of yours; nothing to configure. Spring Security's
default `/logout` then signs the user out and redirects to `/login?logout`. With OpenID Connect, sign out at the
provider too with Spring Security's `OidcClientInitiatedLogoutSuccessHandler`: see
[Signing out at Keycloak](https://shadleaf.dev/0.10.0/guides/oauth2.md#signing-out-at-keycloak).

`hx-boost="false"` keeps a [boosted](https://shadleaf.dev/0.10.0/guides/htmx.md) page from sending it with htmx: signing out should load a
whole page, with a new CSRF token. The Sign in link has it too, as signing in usually redirects to an identity
provider, which an htmx request cannot follow.

## Where it opens

In the sidebar, the menu opens beside it (`side="right"`), its end lined up with the button's: from the footer, it
grows upwards. Collapsed to icons the button is the picture, with the name as a [tooltip](https://shadleaf.dev/0.10.0/components/sidebar.md#collapsed-to-icons);
the tooltip and the menu are never shown together. In the phone panel there is no room beside the button, so the menu
opens above or below it, as every [menu in the sidebar](https://shadleaf.dev/0.10.0/components/sidebar.md#menus-in-the-sidebar) does. `side` and `align`
change it:

```html
<sl:user-menu side="top" align="start"/>
```

## Labels

```properties
sl.user-menu.trigger=Account: {0}
sl.user-menu.sign-in=Sign in
sl.user-menu.sign-out=Sign out
```

`{0}` is the user's name. An `aria-label` on `<sl:user-menu placement="header">` replaces the header button's name for one
menu. In the sidebar the button is named by the name and email it shows, and the Sign in link by its text, so an
`aria-label` is left out there.

## A different user menu

`<sl:user-menu>` covers shadcn's `NavUser`. A menu laid out differently (another trigger, items above the user, a
second group) is a [dropdown menu](https://shadleaf.dev/0.10.0/components/dropdown-menu.md) of your own that reads `#slUser`:

```html
<sl:dropdown-menu id="account-menu" th:if="${#slUser.signedIn}">
  <sl:dropdown-menu-trigger variant="ghost">[[${#slUser.current.name}]]</sl:dropdown-menu-trigger>
  <sl:dropdown-menu-content align="end">
    <sl:dropdown-menu-item as="a" th:href="@{/account}">Account</sl:dropdown-menu-item>
    <sl:dropdown-menu-separator/>
    <sl:dropdown-menu-item type="submit" form="sign-out">Sign out</sl:dropdown-menu-item>
  </sl:dropdown-menu-content>
</sl:dropdown-menu>
<form id="sign-out" method="post" th:action="@{${#slUser.logoutUrl}}" hx-boost="false" hidden></form>
<sl:button th:unless="${#slUser.signedIn}" as="a" th:href="@{${#slUser.loginUrl}}" hx-boost="false">Sign in</sl:button>
```

Keep the form outside `role="menu"`, and keep `th:action`: it adds the CSRF token.

A team or version switcher in the sidebar's header is built the same way, from a dropdown menu: see
[Menus in the sidebar](https://shadleaf.dev/0.10.0/components/sidebar.md#menus-in-the-sidebar).

## Without JavaScript

The menu is a popover, opened by the browser, so it opens and Sign out signs out without JavaScript. The arrow keys,
typeahead and closing after an item need Alpine, as in every [dropdown menu](https://shadleaf.dev/0.10.0/components/dropdown-menu.md). Without JavaScript the
picture lies over the initials and covers them once loaded.

## Attributes

Attributes of `<sl:user-menu>`: The signed-in user (`#slUser.current`) with a menu that signs out, or a Sign in link for an anonymous visitor. In the sidebar's footer a large menu button with the user's picture, name and email (or username, when it is not the name); in the site header a button with the picture alone. Both open a `sl:dropdown-menu`: the user's name and email, the items in its content (Account, Settings), and Sign out, which posts to `#slUser.logoutUrl` (`shadleaf.security.logout-url`) from a form of its own, so `th:action` adds Spring Security's CSRF token. The picture goes through `sl:avatar`: while it loads, without one and when it fails to load, the initials show. Every attribute that is not a prop passes through to the button (or the Sign in link), except an `aria-label`, which only names the header's button: in the sidebar the button's name is the text it shows, and Sign in is named by its text.

| Attribute | Values | Default | Description |
| --- | --- | --- | --- |
| `placement` | `sidebar`, `header` | `sidebar` | `sidebar`: a `sl:sidebar-menu` with one large menu button, for the `sl:sidebar-footer` (shadcn's NavUser). `header`: a round button with the picture, for the `end` slot of `sl:site-header`, named "Account: " and the user's name. |
| `id` | *string* | `user-menu` | The menu's id. Its button gets the id with `-trigger` added, the sign-out form with `-sign-out`. Two user menus on a page need different ids. |
| `side` | `top`, `right`, `bottom`, `left` | — | The side of the button the menu opens on, as on `sl:dropdown-menu-content`. By default `right` in the sidebar (below the button in the phone panel) and `bottom` in the header. |
| `align` | `start`, `center`, `end` | `end` | How the menu lines up with the button along that side, as on `sl:dropdown-menu-content`. |

Every prop can also be set from an expression: `th:placement="${...}"`. Any other attribute passes through to the rendered element, and `class` is appended.
