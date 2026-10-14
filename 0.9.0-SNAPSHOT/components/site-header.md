# Site Header

> A header bar across the top of the page, or at the top of the sidebar layout's page.

Source: https://wimdeblauwe.github.io/shadleaf/0.9.0-SNAPSHOT/components/site-header/

**Example: Site header.** A header bar: its content in a row from the start, the end slot at the end. Across the top of a page it has the page's ground and a border below.

```html
<sl:site-header>
  <sl:button as="a" href="/" variant="ghost">
    <sl:slot name="icon-start"><sl:icon name="gallery-vertical-end"/></sl:slot>
    Acme Inc.
  </sl:button>
  <sl:slot name="end">
    <sl:button variant="ghost" size="icon-sm" aria-label="Search"><sl:icon name="search"/></sl:button>
    <sl:button variant="outline" size="sm">Sign in</sl:button>
  </sl:slot>
</sl:site-header>
```

## Usage

`<sl:site-header>` is a `header` that lays out its content in a row. Whatever you put in it starts at the start of the
line; the `end` slot goes to the end:

```html
<sl:site-header>
  <sl:button as="a" th:href="@{/}" variant="ghost">Acme Inc.</sl:button>
  <sl:slot name="end">
    <sl:theme-toggle/>
  </sl:slot>
</sl:site-header>
```

Across the top of the page it is the page's banner landmark, on the page's colour with a border below. It has no
`sticky` option yet; the header scrolls with the page.

## In the header layout

With a [`<sl:sidebar placement="header">`](https://wimdeblauwe.github.io/shadleaf/0.9.0-SNAPSHOT/components/sidebar.md#in-the-header) in it, it is the header layout's header: from
768 px the sidebar is a row of links in it, and on a phone a menu button (`<sl:sidebar-trigger>`) opens the same
navigation as a panel. It then goes first in `<sl:sidebar-provider>`, above `<sl:sidebar-inset>`. With
`variant="inset"` on the sidebar the header lies on the sidebar's colour without a border, and the page is a card below
it.

**Example: Header layout.** placement="header" puts the same navigation in a sl:site-header: from 768 px a row of links (the current page marked as in the sidebar), with the application's name before it and search, the theme toggle and the signed-in user's menu at the end. Favorites, Settings and Help are panel-only, so the row stays short; the panel's own header with the name is panel-only too. "More" beside Documents opens a dropdown menu below it. variant="inset" makes the page a card below the header. Narrower, the menu button opens the same nav as the panel: see the next example.

```html
<sl:skip-link/>
<sl:sidebar-provider>
  <sl:site-header>
    <sl:sidebar-trigger aria-label="Menu"><sl:icon name="menu"/></sl:sidebar-trigger>
    <sl:button as="a" href="/" variant="ghost">
      <sl:slot name="icon-start"><sl:icon name="gallery-vertical-end"/></sl:slot>
      Acme Inc.
    </sl:button>
    <sl:sidebar placement="header" variant="inset">
      <sl:sidebar-header panel-only>
        <sl:sidebar-menu>
          <sl:sidebar-menu-item>
            <sl:sidebar-menu-button href="/" size="lg">
              <sl:slot name="icon-start"><sl:icon name="gallery-vertical-end"/></sl:slot>
              <strong>Acme Inc.</strong>
              <small>Enterprise</small>
            </sl:sidebar-menu-button>
          </sl:sidebar-menu-item>
        </sl:sidebar-menu>
      </sl:sidebar-header>
      <sl:sidebar-content>
        <sl:sidebar-group>
          <sl:sidebar-group-content>
            <sl:sidebar-menu>
              <sl:sidebar-menu-item>
                <sl:sidebar-menu-button href="/" th:active="${#slNav.current('/')}">
                  <sl:slot name="icon-start"><sl:icon name="house"/></sl:slot>
                  Home
                </sl:sidebar-menu-button>
              </sl:sidebar-menu-item>
              <sl:sidebar-menu-item id="header-inbox">
                <sl:sidebar-menu-button href="/inbox" th:active="${#slNav.current('/inbox')}">
                  <sl:slot name="icon-start"><sl:icon name="inbox"/></sl:slot>
                  Inbox
                </sl:sidebar-menu-button>
                <sl:sidebar-menu-badge label="12 unread">12</sl:sidebar-menu-badge>
              </sl:sidebar-menu-item>
              <sl:sidebar-menu-item>
                <sl:sidebar-menu-button href="/calendar" th:active="${#slNav.current('/calendar')}">
                  <sl:slot name="icon-start"><sl:icon name="calendar"/></sl:slot>
                  Calendar
                </sl:sidebar-menu-button>
              </sl:sidebar-menu-item>
              <sl:sidebar-menu-item>
                <sl:sidebar-menu-button href="/documents" th:active="${#slNav.current('/documents')}">
                  <sl:slot name="icon-start"><sl:icon name="file-text"/></sl:slot>
                  Documents
                </sl:sidebar-menu-button>
                <sl:dropdown-menu id="header-documents-more">
                  <sl:dropdown-menu-trigger as="sidebar-menu-action" aria-label="More for Documents">
                    <sl:icon name="ellipsis"/>
                  </sl:dropdown-menu-trigger>
                  <sl:dropdown-menu-content side="right">
                    <sl:dropdown-menu-item as="a" href="/documents/new">New document</sl:dropdown-menu-item>
                    <sl:dropdown-menu-item as="a" href="/documents/shared">Shared with me</sl:dropdown-menu-item>
                  </sl:dropdown-menu-content>
                </sl:dropdown-menu>
              </sl:sidebar-menu-item>
            </sl:sidebar-menu>
          </sl:sidebar-group-content>
        </sl:sidebar-group>
        <sl:sidebar-group id="favorites" panel-only>
          <sl:sidebar-group-label>Favorites</sl:sidebar-group-label>
          <sl:sidebar-group-content>
            <sl:sidebar-menu>
              <sl:sidebar-menu-item>
                <sl:sidebar-menu-button href="/projects/marketing">
                  <sl:slot name="icon-start"><sl:icon name="globe"/></sl:slot>
                  Marketing site
                </sl:sidebar-menu-button>
              </sl:sidebar-menu-item>
              <sl:sidebar-menu-item>
                <sl:sidebar-menu-button href="/projects/android">
                  <sl:slot name="icon-start"><sl:icon name="smartphone"/></sl:slot>
                  Android app
                </sl:sidebar-menu-button>
              </sl:sidebar-menu-item>
            </sl:sidebar-menu>
          </sl:sidebar-group-content>
        </sl:sidebar-group>
      </sl:sidebar-content>
      <sl:sidebar-footer panel-only>
        <sl:sidebar-menu>
          <sl:sidebar-menu-item>
            <sl:sidebar-menu-button href="/settings" size="sm" th:active="${#slNav.current('/settings')}">
              <sl:slot name="icon-start"><sl:icon name="settings"/></sl:slot>
              Settings
            </sl:sidebar-menu-button>
          </sl:sidebar-menu-item>
          <sl:sidebar-menu-item>
            <sl:sidebar-menu-button href="/help" size="sm">
              <sl:slot name="icon-start"><sl:icon name="life-buoy"/></sl:slot>
              Help
            </sl:sidebar-menu-button>
          </sl:sidebar-menu-item>
        </sl:sidebar-menu>
      </sl:sidebar-footer>
    </sl:sidebar>
    <sl:slot name="end">
      <sl:button variant="ghost" size="icon-sm" aria-label="Search"><sl:icon name="search"/></sl:button>
      <sl:theme-toggle/>
      <sl:user-menu as="header" id="header-user-menu">
        <sl:dropdown-menu-item as="a" href="/account"><sl:icon name="badge-check"/>Account</sl:dropdown-menu-item>
      </sl:user-menu>
    </sl:slot>
  </sl:site-header>
  <sl:sidebar-inset>
    <h1>Home</h1>
    <p>The page's content.</p>
  </sl:sidebar-inset>
</sl:sidebar-provider>
```

The [Navigation in the header](https://wimdeblauwe.github.io/shadleaf/0.9.0-SNAPSHOT/layouts/header.md) layout has the whole layout to copy.

## As the page's header

At the top of `<sl:sidebar-inset>`, in the sidebar layout, it is the page's header (shadcn's sidebar-07): the trigger,
a vertical [separator](https://wimdeblauwe.github.io/shadleaf/0.9.0-SNAPSHOT/components/separator.md), the [breadcrumb](https://wimdeblauwe.github.io/shadleaf/0.9.0-SNAPSHOT/components/breadcrumb.md), and the theme toggle at the end. Inside the
`main` it is no landmark, and it has no border or colour of its own. The separator is a short line in the middle of the
row there. The [Collapses to icons](https://wimdeblauwe.github.io/shadleaf/0.9.0-SNAPSHOT/layouts/collapses-to-icons.md) layout uses it this way.

```html
<sl:sidebar-inset>
  <sl:site-header>
    <sl:sidebar-trigger/>
    <sl:separator orientation="vertical"/>
    <sl:breadcrumb>...</sl:breadcrumb>
    <sl:slot name="end"><sl:theme-toggle/></sl:slot>
  </sl:site-header>
  ...
</sl:sidebar-inset>
```

## Height

The skins make it 3.5rem high (`--site-header-height`, set on `.site-header`): a minimum, so a breadcrumb that wraps on
a narrow screen makes it taller.

## Attributes

Attributes of `<sl:site-header>`: A header bar: a `header` laying out its content in a row. Across the top of the page (inside `sl:sidebar-provider`, before `sl:sidebar-inset`) it is the header layout's: a `sl:sidebar-trigger` that opens the menu on a phone, the application's name, the `sl:sidebar` with `placement="header"` as a row of links, and the `end` slot. At the top of `sl:sidebar-inset` it is the page's header in the sidebar layout: the trigger, a vertical `sl:separator` and a `sl:breadcrumb`. The `end` slot (search, the theme toggle, the signed-in user) goes to the end of the row.

`sl:site-header` declares no props; every attribute passes through.
