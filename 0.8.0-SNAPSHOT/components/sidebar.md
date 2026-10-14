# Sidebar

> The application's navigation beside the page, and a panel over it on a phone.

Source: https://wimdeblauwe.github.io/shadleaf/0.8.0-SNAPSHOT/components/sidebar/

**Example: Sidebar.** Where there are 768 px or more (here: the frame's width), the sidebar sits beside the page and the trigger (or Ctrl/⌘+B) collapses it, remembered in a cookie; narrower, the trigger opens it as a panel over the page. Rendered for a request for /, so #slNav.current('/') marks Home as the current page. The skip link (the first Tab) goes to the inset, whose header holds the trigger and the breadcrumb. variant="inset" makes the page a card on the sidebar's ground (from 768 px), and collapsible="icon" collapses it to a strip of icons whose tooltips show the labels, as sample-01's layout. Documents opens a sub-menu. The footer ends in the signed-in user's menu (sl:user-menu). The large button in the header is a team switcher: it opens a dropdown menu beside the sidebar (below it in the panel) with the current team checked, and "More" beside Design Engineering opens one too.

```html
<sl:skip-link/>
<sl:sidebar-provider>
  <sl:sidebar variant="inset" collapsible="icon">
    <sl:sidebar-header>
      <sl:sidebar-menu>
        <sl:sidebar-menu-item>
          <sl:dropdown-menu id="team-switcher">
            <sl:dropdown-menu-trigger as="sidebar-menu-button" size="lg" tooltip="Acme Inc.">
              <sl:slot name="icon-start"><sl:icon name="gallery-vertical-end"/></sl:slot>
              <strong>Acme Inc.</strong>
              <small>Enterprise</small>
              <sl:slot name="icon-end"><sl:icon name="chevrons-up-down"/></sl:slot>
            </sl:dropdown-menu-trigger>
            <sl:dropdown-menu-content side="right">
              <sl:dropdown-menu-label id="team-switcher-label">Teams</sl:dropdown-menu-label>
              <sl:dropdown-menu-radio-group aria-labelledby="team-switcher-label">
                <sl:dropdown-menu-radio-item as="a" href="/teams/acme-inc" checked>
                  Acme Inc.
                </sl:dropdown-menu-radio-item>
                <sl:dropdown-menu-radio-item as="a" href="/teams/acme-corp">
                  Acme Corp.
                </sl:dropdown-menu-radio-item>
                <sl:dropdown-menu-radio-item as="a" href="/teams/evil-corp">
                  Evil Corp.
                </sl:dropdown-menu-radio-item>
              </sl:dropdown-menu-radio-group>
              <sl:dropdown-menu-separator/>
              <sl:dropdown-menu-item as="a" href="/teams/new">
                <sl:icon name="plus"/>
                Add team
              </sl:dropdown-menu-item>
            </sl:dropdown-menu-content>
          </sl:dropdown-menu>
        </sl:sidebar-menu-item>
      </sl:sidebar-menu>
    </sl:sidebar-header>
    <sl:sidebar-content>
      <sl:sidebar-group id="platform">
        <sl:sidebar-group-label>Platform</sl:sidebar-group-label>
        <sl:sidebar-group-content>
          <sl:sidebar-menu>
            <sl:sidebar-menu-item>
              <sl:sidebar-menu-button href="/" th:active="${#slNav.current('/')}" tooltip="Home">
                <sl:slot name="icon-start"><sl:icon name="house"/></sl:slot>
                Home
              </sl:sidebar-menu-button>
            </sl:sidebar-menu-item>
            <sl:sidebar-menu-item id="inbox">
              <sl:sidebar-menu-button href="/inbox" th:active="${#slNav.current('/inbox')}" tooltip="Inbox">
                <sl:slot name="icon-start"><sl:icon name="inbox"/></sl:slot>
                Inbox
              </sl:sidebar-menu-button>
              <sl:sidebar-menu-badge label="12 unread">12</sl:sidebar-menu-badge>
            </sl:sidebar-menu-item>
            <sl:sidebar-menu-item>
              <sl:sidebar-menu-button href="/calendar" th:active="${#slNav.current('/calendar')}" tooltip="Calendar">
                <sl:slot name="icon-start"><sl:icon name="calendar"/></sl:slot>
                Calendar
              </sl:sidebar-menu-button>
            </sl:sidebar-menu-item>
            <sl:sidebar-menu-item collapsible th:open="${#slNav.current('/documents')}">
              <sl:sidebar-menu-button tooltip="Documents">
                <sl:slot name="icon-start"><sl:icon name="file-text"/></sl:slot>
                Documents
              </sl:sidebar-menu-button>
              <sl:sidebar-menu-sub>
                <sl:sidebar-menu-sub-item>
                  <sl:sidebar-menu-sub-button href="/documents"
                                              th:active="${#slNav.current('/documents', true)}">
                    Recent
                  </sl:sidebar-menu-sub-button>
                </sl:sidebar-menu-sub-item>
                <sl:sidebar-menu-sub-item>
                  <sl:sidebar-menu-sub-button href="/documents/shared"
                                              th:active="${#slNav.current('/documents/shared')}">
                    Shared with me
                  </sl:sidebar-menu-sub-button>
                </sl:sidebar-menu-sub-item>
              </sl:sidebar-menu-sub>
            </sl:sidebar-menu-item>
          </sl:sidebar-menu>
        </sl:sidebar-group-content>
      </sl:sidebar-group>
      <sl:sidebar-group id="projects">
        <sl:sidebar-group-label>Projects</sl:sidebar-group-label>
        <sl:sidebar-group-action aria-label="Add project"><sl:icon name="plus"/></sl:sidebar-group-action>
        <sl:sidebar-group-content>
          <sl:sidebar-menu>
            <sl:sidebar-menu-item>
              <sl:sidebar-menu-button href="/projects/design" tooltip="Design Engineering">
                <sl:slot name="icon-start"><sl:icon name="frame"/></sl:slot>
                Design Engineering
              </sl:sidebar-menu-button>
              <sl:dropdown-menu id="project-design-more">
                <sl:dropdown-menu-trigger as="sidebar-menu-action" show-on-hover
                                          aria-label="More for Design Engineering">
                  <sl:icon name="ellipsis"/>
                </sl:dropdown-menu-trigger>
                <sl:dropdown-menu-content side="right">
                  <sl:dropdown-menu-item as="a" href="/projects/design">
                    <sl:icon name="folder"/>
                    View project
                  </sl:dropdown-menu-item>
                  <sl:dropdown-menu-item as="a" href="/projects/design/share">
                    <sl:icon name="forward"/>
                    Share project
                  </sl:dropdown-menu-item>
                  <sl:dropdown-menu-separator/>
                  <sl:dropdown-menu-item as="a" href="/projects/design/delete" variant="destructive">
                    <sl:icon name="trash-2"/>
                    Delete project
                  </sl:dropdown-menu-item>
                </sl:dropdown-menu-content>
              </sl:dropdown-menu>
            </sl:sidebar-menu-item>
            <sl:sidebar-menu-item>
              <sl:sidebar-menu-button href="/projects/sales" tooltip="Sales &amp; Marketing">
                <sl:slot name="icon-start"><sl:icon name="chart-pie"/></sl:slot>
                Sales &amp; Marketing
              </sl:sidebar-menu-button>
              <sl:sidebar-menu-action aria-label="More for Sales &amp; Marketing" show-on-hover>
                <sl:icon name="ellipsis"/>
              </sl:sidebar-menu-action>
            </sl:sidebar-menu-item>
          </sl:sidebar-menu>
        </sl:sidebar-group-content>
      </sl:sidebar-group>
    </sl:sidebar-content>
    <sl:sidebar-footer>
      <sl:sidebar-menu>
        <sl:sidebar-menu-item>
          <sl:sidebar-menu-button href="/settings" size="sm" th:active="${#slNav.current('/settings')}"
                                  tooltip="Settings">
            <sl:slot name="icon-start"><sl:icon name="settings"/></sl:slot>
            Settings
          </sl:sidebar-menu-button>
        </sl:sidebar-menu-item>
        <sl:sidebar-menu-item>
          <sl:sidebar-menu-button href="/help" size="sm" tooltip="Help">
            <sl:slot name="icon-start"><sl:icon name="life-buoy"/></sl:slot>
            Help
          </sl:sidebar-menu-button>
        </sl:sidebar-menu-item>
      </sl:sidebar-menu>
      <sl:user-menu>
        <sl:dropdown-menu-item as="a" href="/account"><sl:icon name="badge-check"/>Account</sl:dropdown-menu-item>
        <sl:dropdown-menu-item as="a" href="/notifications"><sl:icon name="bell"/>Notifications</sl:dropdown-menu-item>
      </sl:user-menu>
    </sl:sidebar-footer>
  </sl:sidebar>
  <sl:sidebar-inset>
    <sl:site-header>
      <sl:sidebar-trigger/>
      <sl:separator orientation="vertical"/>
      <sl:breadcrumb>
        <sl:breadcrumb-list>
          <sl:breadcrumb-item>Platform</sl:breadcrumb-item>
          <sl:breadcrumb-separator/>
          <sl:breadcrumb-item><sl:breadcrumb-page>Home</sl:breadcrumb-page></sl:breadcrumb-item>
        </sl:breadcrumb-list>
      </sl:breadcrumb>
    </sl:site-header>
    <h1>Home</h1>
    <p>The page's content.</p>
  </sl:sidebar-inset>
</sl:sidebar-provider>
```

> **Tip: Layouts to copy**
>
> The [Application shell](https://wimdeblauwe.github.io/shadleaf/0.8.0-SNAPSHOT/guides/application-shell.md) guide has two whole layouts built from these parts, the
> sidebar layout and the header layout, and what every layout needs around them.

## Built from parts

An application shell with a sidebar is four components, as in shadcn/ui:

- `<sl:sidebar-provider>` wraps the whole page and holds the state: is the sidebar expanded or collapsed on a desktop.
- `<sl:sidebar>` is the navigation itself: a `<nav>` landmark named "Main".
- `<sl:sidebar-inset>` is the page's `<main>`, beside the sidebar: `<main id="main" tabindex="-1">`, the target of
  the [skip link](https://wimdeblauwe.github.io/shadleaf/0.8.0-SNAPSHOT/components/skip-link.md).
- `<sl:sidebar-trigger>` is the icon button that collapses the sidebar, or opens it on a phone. It usually sits in a
  header at the top of the inset.

```html
<body>
<sl:skip-link/>
<sl:sidebar-provider>
  <sl:sidebar>
    <!-- header, content with groups and menus, footer: see "What goes inside" below -->
  </sl:sidebar>
  <sl:sidebar-inset>
    <header>
      <sl:sidebar-trigger/>
      <sl:separator orientation="vertical"/>
      <sl:breadcrumb>...</sl:breadcrumb>
    </header>
    <div th:replace="${content}"></div>
  </sl:sidebar-inset>
</sl:sidebar-provider>
</body>
```

Put the sidebar straight inside the provider (not in an element of your own): the CSS that collapses it looks for it
there.

The [skip link](https://wimdeblauwe.github.io/shadleaf/0.8.0-SNAPSHOT/components/skip-link.md) comes first in the body: the sidebar puts many links before the content, and it lets a
keyboard user jump past them to the inset. The header at the top of the inset holds the trigger, a vertical
[separator](https://wimdeblauwe.github.io/shadleaf/0.8.0-SNAPSHOT/components/separator.md) and the [breadcrumb](https://wimdeblauwe.github.io/shadleaf/0.8.0-SNAPSHOT/components/breadcrumb.md), as shadcn's blocks have it. Lay it out as a row
yourself, or use a [site header](https://wimdeblauwe.github.io/shadleaf/0.8.0-SNAPSHOT/components/site-header.md), which does it for you, as the
[sidebar layout](https://wimdeblauwe.github.io/shadleaf/0.8.0-SNAPSHOT/guides/application-shell.md#the-sidebar-layout) does.

## What goes inside

The sidebar holds a header, the content and a footer, with shadcn/ui's names:

- `<sl:sidebar-header>` at the top (the application or team) and `<sl:sidebar-footer>` at the bottom (settings, help,
  the signed-in user's [user menu](https://wimdeblauwe.github.io/shadleaf/0.8.0-SNAPSHOT/components/user-menu.md)). They stay in view.
- `<sl:sidebar-content>` between them, which scrolls when there is more than fits. It is a plain `div`: the sidebar
  itself is the navigation landmark.
- `<sl:sidebar-separator>` draws a line between parts; screen readers skip it.

The content is made of groups, and each group of a menu:

```html
<sl:sidebar>
  <sl:sidebar-header>
    <sl:sidebar-menu>
      <sl:sidebar-menu-item>
        <sl:sidebar-menu-button th:href="@{/}" size="lg">
          <sl:slot name="icon-start"><sl:icon name="gallery-vertical-end"/></sl:slot>
          <strong>Acme Inc.</strong>
          <small>Enterprise</small>
        </sl:sidebar-menu-button>
      </sl:sidebar-menu-item>
    </sl:sidebar-menu>
  </sl:sidebar-header>
  <sl:sidebar-content>
    <sl:sidebar-group id="platform">
      <sl:sidebar-group-label>Platform</sl:sidebar-group-label>
      <sl:sidebar-group-content>
        <sl:sidebar-menu>
          <sl:sidebar-menu-item>
            <sl:sidebar-menu-button th:href="@{/people}" th:active="${#slNav.current('/people')}">
              <sl:slot name="icon-start"><sl:icon name="users"/></sl:slot>
              People
            </sl:sidebar-menu-button>
          </sl:sidebar-menu-item>
        </sl:sidebar-menu>
      </sl:sidebar-group-content>
    </sl:sidebar-group>
  </sl:sidebar-content>
  <sl:sidebar-footer>...</sl:sidebar-footer>
</sl:sidebar>
```

- `<sl:sidebar-group>` with a `<sl:sidebar-group-label>` is a group named by its label: a screen reader says
  "Platform, group" when the focus moves into it. The label's id is the group's `id` with `-label`; without an `id` the
  group generates one on every render. Without a label, a group is a plain `div`.
- `<sl:sidebar-group-content>` holds the group's menu, below the label.
- `<sl:sidebar-menu>` is a list (`ul`), so a screen reader says how many entries it has, and
  `<sl:sidebar-menu-item>` one entry (`li`).
- `<sl:sidebar-menu-button>` is the link: an icon in the `icon-start` slot (and `icon-end` for one at the end), then
  the label, which ends in an ellipsis when it does not fit. For two lines, put each in an element of its own, such as
  `<strong>` and `<small>`.

## Menu buttons

A menu button is a link (`as="a"`, the default), or with `as="button"` a button, for an action that is no page of its
own; every attribute that is no prop passes through, so `hx-get` or `commandfor` work as on any button. `size` is
`default`, `sm` (a dense list, such as the footer's) or `lg` (two lines, such as the application's name in the
header), and `variant="outline"` adds a background and a border. `disabled` works as on `<sl:button>`: a disabled link
loses its `href` and leaves the tab order.

**Example: Menu buttons.** The sizes, the outline variant, a button marked as the current one (aria-current="true"), and disabled entries.

```html
<sl:sidebar-provider>
  <sl:sidebar id="sidebar-buttons">
    <sl:sidebar-content>
      <sl:sidebar-group>
        <sl:sidebar-group-content>
          <sl:sidebar-menu>
            <sl:sidebar-menu-item>
              <sl:sidebar-menu-button href="/" size="lg">
                <sl:slot name="icon-start"><sl:icon name="gallery-vertical-end"/></sl:slot>
                <strong>Large</strong>
                <small>Two lines</small>
              </sl:sidebar-menu-button>
            </sl:sidebar-menu-item>
            <sl:sidebar-menu-item>
              <sl:sidebar-menu-button href="/">
                <sl:slot name="icon-start"><sl:icon name="house"/></sl:slot>
                Default
              </sl:sidebar-menu-button>
            </sl:sidebar-menu-item>
            <sl:sidebar-menu-item>
              <sl:sidebar-menu-button href="/" size="sm">
                <sl:slot name="icon-start"><sl:icon name="house"/></sl:slot>
                Small
              </sl:sidebar-menu-button>
            </sl:sidebar-menu-item>
            <sl:sidebar-menu-item>
              <sl:sidebar-menu-button href="/" variant="outline">
                <sl:slot name="icon-start"><sl:icon name="square-dashed"/></sl:slot>
                Outline
              </sl:sidebar-menu-button>
            </sl:sidebar-menu-item>
            <sl:sidebar-menu-item>
              <sl:sidebar-menu-button as="button" active>
                <sl:slot name="icon-start"><sl:icon name="file-pen"/></sl:slot>
                Drafts (a button)
              </sl:sidebar-menu-button>
            </sl:sidebar-menu-item>
            <sl:sidebar-menu-item>
              <sl:sidebar-menu-button href="/billing" disabled>
                <sl:slot name="icon-start"><sl:icon name="credit-card"/></sl:slot>
                Disabled link
              </sl:sidebar-menu-button>
            </sl:sidebar-menu-item>
            <sl:sidebar-menu-item>
              <sl:sidebar-menu-button href="/a-page-with-a-very-long-name">
                <sl:slot name="icon-start"><sl:icon name="file-text"/></sl:slot>
                A label too long for the sidebar ends in an ellipsis
              </sl:sidebar-menu-button>
            </sl:sidebar-menu-item>
          </sl:sidebar-menu>
        </sl:sidebar-group-content>
      </sl:sidebar-group>
    </sl:sidebar-content>
  </sl:sidebar>
  <sl:sidebar-inset>
    <sl:site-header>
      <sl:sidebar-trigger for="sidebar-buttons"/>
    </sl:site-header>
    <h1>Menu buttons</h1>
  </sl:sidebar-inset>
</sl:sidebar-provider>
```

## The current page

`active` marks the current page: the link gets `aria-current="page"`, which a screen reader says ("current page"), and
the accent background. A menu button that is a button gets `aria-current="true"` ("current").

A layout usually has no model attribute saying which page it shows. `#slNav.current(path)` asks the request:

```html
<sl:sidebar-menu-button th:href="@{/people}" th:active="${#slNav.current('/people')}">People</sl:sidebar-menu-button>
```

- The path is the application's own, as in `@{/people}`: a context path makes no difference.
- It is the current page for `/people` and for every page below it, such as `/people/12/edit`, but not for
  `/peoples`. `#slNav.current('/people', true)` is only `/people` itself.
- `/` is only the current page for `/` itself, or Home would be current on every page.
- A query string and a trailing slash are ignored: `/people?page=2` is `/people`.

On a phone the panel opens with the focus on the current page's link. With `hx-boost` on the body every page comes
from the server with the right link marked; an application that swaps only the main area leaves the old one marked,
unless it swaps the menu too (out of band).

## Badges

`<sl:sidebar-menu-badge>` puts a count at the end of a menu button, inside the same menu item:

```html
<sl:sidebar-menu-item id="inbox">
  <sl:sidebar-menu-button th:href="@{/inbox}">
    <sl:slot name="icon-start"><sl:icon name="inbox"/></sl:slot>
    Inbox
  </sl:sidebar-menu-button>
  <sl:sidebar-menu-badge th:label="${unread + ' unread'}">[[${unread}]]</sl:sidebar-menu-badge>
</sl:sidebar-menu-item>
```

The button is described by the badge (`aria-describedby`), so when Tab reaches it a screen reader says "Inbox, link,
12 unread". `label` is what it says instead of the bare number. The badge's id is the item's `id` with `-badge`;
without an `id` the item generates one on every render.

## Actions

`<sl:sidebar-group-action>` is an icon button at the end of a group's label ("Add project"), and
`<sl:sidebar-menu-action>` one at the end of a menu item, beside its button ("More"). Both hold only an icon, so they
need an `aria-label`; name the entry too, as every item has one ("More for Design Engineering"). With `as="a"` they are
links. `show-on-hover` on a menu action hides it on a desktop until the pointer is over its item or the focus is in
it; on a phone it is always there. A menu action is 24 px, the minimum target size of WCAG 2.2 (shadcn's is 20 px);
a group action is 20 px on a desktop, where nothing is close to it, and 24 px on a phone. A "More" that opens a menu is
a [menu trigger](#menus-in-the-sidebar).

```html
<sl:sidebar-menu-item>
  <sl:sidebar-menu-button th:href="@{/projects/{id}(id=${project.id})}">[[${project.name}]]</sl:sidebar-menu-button>
  <sl:sidebar-menu-action th:aria-label="|More for ${project.name}|" show-on-hover>
    <sl:icon name="ellipsis"/>
  </sl:sidebar-menu-action>
</sl:sidebar-menu-item>
```

## Menus in the sidebar

A dropdown menu opens from a sidebar part when its trigger says so: `<sl:dropdown-menu-trigger as="sidebar-menu-button">`
renders a menu button, `as="sidebar-menu-action"` a menu action (shadcn's `<DropdownMenuTrigger asChild>`). The trigger
takes the part's props (`size`, `variant`, `tooltip`; `show-on-hover`) and its `icon-start` and `icon-end` slots, and
any other attribute passes through. Put the `<sl:dropdown-menu>` in the `<sl:sidebar-menu-item>`, where the part would
be. The default example above has three: a team switcher in the header, "More" beside Design Engineering, and the
[user menu](https://wimdeblauwe.github.io/shadleaf/0.8.0-SNAPSHOT/components/user-menu.md) in the footer, which is one component.

```html
<sl:sidebar-header>
  <sl:sidebar-menu>
    <sl:sidebar-menu-item>
      <sl:dropdown-menu id="team-switcher">
        <sl:dropdown-menu-trigger as="sidebar-menu-button" size="lg" th:tooltip="${team.name}">
          <sl:slot name="icon-start"><sl:icon name="gallery-vertical-end"/></sl:slot>
          <strong>[[${team.name}]]</strong>
          <small>[[${team.plan}]]</small>
          <sl:slot name="icon-end"><sl:icon name="chevrons-up-down"/></sl:slot>
        </sl:dropdown-menu-trigger>
        <sl:dropdown-menu-content side="right">
          <sl:dropdown-menu-label id="team-switcher-label">Teams</sl:dropdown-menu-label>
          <sl:dropdown-menu-radio-group aria-labelledby="team-switcher-label">
            <sl:dropdown-menu-radio-item as="a" th:each="t : ${teams}" th:href="@{/teams/{id}(id=${t.id})}"
                                         th:checked="${t.id == team.id}">[[${t.name}]]</sl:dropdown-menu-radio-item>
          </sl:dropdown-menu-radio-group>
          <sl:dropdown-menu-separator/>
          <sl:dropdown-menu-item as="a" th:href="@{/teams/new}"><sl:icon name="plus"/>Add team</sl:dropdown-menu-item>
        </sl:dropdown-menu-content>
      </sl:dropdown-menu>
    </sl:sidebar-menu-item>
  </sl:sidebar-menu>
</sl:sidebar-header>
```

```html
<sl:sidebar-menu-item th:each="project : ${projects}">
  <sl:sidebar-menu-button th:href="@{/projects/{id}(id=${project.id})}">[[${project.name}]]</sl:sidebar-menu-button>
  <sl:dropdown-menu th:id="|project-${project.id}-more|">
    <sl:dropdown-menu-trigger as="sidebar-menu-action" show-on-hover th:aria-label="|More for ${project.name}|">
      <sl:icon name="ellipsis"/>
    </sl:dropdown-menu-trigger>
    <sl:dropdown-menu-content side="right">
      <sl:dropdown-menu-item as="a" th:href="@{/projects/{id}(id=${project.id})}">View project</sl:dropdown-menu-item>
      <sl:dropdown-menu-separator/>
      <sl:dropdown-menu-item variant="destructive" hx:get="@{/projects/{id}/delete(id=${project.id})}"
                             hx-target="#modal-root">Delete project</sl:dropdown-menu-item>
    </sl:dropdown-menu-content>
  </sl:dropdown-menu>
</sl:sidebar-menu-item>
```

**The current team or version** is a checked radio item: a menu of links, one per team (or version), the current one
checked by the server. A screen reader says "Acme Inc., radio menu item, checked", the check mark shows it, and it works without
JavaScript, as the [radio items](https://wimdeblauwe.github.io/shadleaf/0.8.0-SNAPSHOT/components/dropdown-menu.md#checkbox-and-radio-items) of a sort menu do. The menu is labelled by its
trigger; the group of teams by the label above it.

A version switcher (shadcn's sidebar-01) is the same menu with the versions, opened from the header's large menu button:

**Example: Version switcher.** A dropdown menu opened from a large menu button in the header (sl:dropdown-menu-trigger as="sidebar-menu-button"), as shadcn's version switcher. The versions are links, and the server checks the current one, so it works without JavaScript. In the phone panel it opens below the button.

```html
<sl:sidebar-provider>
  <sl:sidebar id="sidebar-versions">
    <sl:sidebar-header>
      <sl:sidebar-menu>
        <sl:sidebar-menu-item>
          <sl:dropdown-menu id="version-switcher">
            <sl:dropdown-menu-trigger as="sidebar-menu-button" size="lg">
              <sl:slot name="icon-start"><sl:icon name="gallery-vertical-end"/></sl:slot>
              <strong>Documentation</strong>
              <small>v1.0.1</small>
              <sl:slot name="icon-end"><sl:icon name="chevrons-up-down"/></sl:slot>
            </sl:dropdown-menu-trigger>
            <sl:dropdown-menu-content>
              <sl:dropdown-menu-radio-group aria-label="Versions">
                <sl:dropdown-menu-radio-item as="a" href="/docs/1.0.1" checked>v1.0.1</sl:dropdown-menu-radio-item>
                <sl:dropdown-menu-radio-item as="a" href="/docs/1.1.0-alpha">v1.1.0-alpha</sl:dropdown-menu-radio-item>
                <sl:dropdown-menu-radio-item as="a" href="/docs/2.0.0-beta1">v2.0.0-beta1</sl:dropdown-menu-radio-item>
              </sl:dropdown-menu-radio-group>
            </sl:dropdown-menu-content>
          </sl:dropdown-menu>
        </sl:sidebar-menu-item>
      </sl:sidebar-menu>
    </sl:sidebar-header>
    <sl:sidebar-content>
      <sl:sidebar-group>
        <sl:sidebar-group-label>Getting started</sl:sidebar-group-label>
        <sl:sidebar-group-content>
          <sl:sidebar-menu>
            <sl:sidebar-menu-item>
              <sl:sidebar-menu-button href="/docs/installation">Installation</sl:sidebar-menu-button>
            </sl:sidebar-menu-item>
            <sl:sidebar-menu-item>
              <sl:sidebar-menu-button href="/docs/project-structure">Project structure</sl:sidebar-menu-button>
            </sl:sidebar-menu-item>
          </sl:sidebar-menu>
        </sl:sidebar-group-content>
      </sl:sidebar-group>
    </sl:sidebar-content>
  </sl:sidebar>
  <sl:sidebar-inset>
    <sl:site-header>
      <sl:sidebar-trigger for="sidebar-versions"/>
    </sl:site-header>
    <h1>Installation</h1>
  </sl:sidebar-inset>
</sl:sidebar-provider>
```

**Where it opens**, in each state of the sidebar:

| State | The menu |
| --- | --- |
| Expanded, on a desktop | On the side its content asks for: `side="right"` puts it beside the sidebar, its top lined up with the trigger, as shadcn's team switcher and user menu. A menu from a menu button is at least as wide as the button, and 14 rem. |
| Collapsed to icons | Beside the strip, the same way. The button's tooltip and its menu are never shown together: the tooltip hides when the menu opens and does not show while it is open. A "More" action is not in the strip, like every action. |
| The phone panel | Below its trigger, lined up with its end, whatever `side` says (there is no room beside a panel as wide as the screen); above it when there is no room below. The menu is drawn above the panel and the panel's `inert` leaves it alone. Escape closes the menu first, with the focus back on its trigger, and the panel on the next Escape. A click elsewhere in the panel closes only the menu. |
| The header layout's row | Below its trigger, lined up with its end, as in the panel. The row's sideways scrolling does not clip it. A menu button that opens a menu keeps its `icon-end` (a chevron) in the row, where other icons are left out. |

The trigger looks active while its menu is open, and a `show-on-hover` action stays shown.

## Collapsible groups

A menu item with `collapsible` is a group of links that opens and closes, as shadcn's sidebar-07 has: its menu button
is what the user clicks, and `<sl:sidebar-menu-sub>` holds the links, indented along a line below it. It is the
browser's `details` element (the button becomes its `summary`, with a chevron that turns when it opens), so it needs
no JavaScript, and the keyboard opens and closes it with Enter and Space.

```html
<sl:sidebar-menu-item collapsible th:open="${#slNav.current('/settings')}">
  <sl:sidebar-menu-button tooltip="Settings">
    <sl:slot name="icon-start"><sl:icon name="settings"/></sl:slot>
    Settings
  </sl:sidebar-menu-button>
  <sl:sidebar-menu-sub>
    <sl:sidebar-menu-sub-item>
      <sl:sidebar-menu-sub-button th:href="@{/settings}" th:active="${#slNav.current('/settings', true)}">
        General
      </sl:sidebar-menu-sub-button>
    </sl:sidebar-menu-sub-item>
    <sl:sidebar-menu-sub-item>
      <sl:sidebar-menu-sub-button th:href="@{/settings/billing}" th:active="${#slNav.current('/settings/billing')}">
        Billing
      </sl:sidebar-menu-sub-button>
    </sl:sidebar-menu-sub-item>
  </sl:sidebar-menu-sub>
</sl:sidebar-menu-item>
```

- `open` renders the group open. Open the one that holds the current page, so its link is in view:
  `#slNav.current('/settings')` is true on every page below `/settings`. When the pages share no path, combine them:
  `th:open="${#slNav.current('/people') or #slNav.current('/teams')}"`.
- `<sl:sidebar-menu-sub-button>` is a link (or `as="button"`) like a menu button: `active` marks the current page,
  `size` is `md` (the default) or `sm`, `disabled` works as on a menu button, and an icon goes in `icon-start`.
- The menu button of a collapsible item takes no `href`: it opens the group. Link the group's first page as a
  sub-button.
- A collapsible item takes no badge or action: the browser hides everything in a closed `details` but its `summary`.
  Put a count on a sub-button instead.

**Example: Collapsible groups.** A collapsible sl:sidebar-menu-item is a group that opens and closes on the browser's details element, no JavaScript needed; its sl:sidebar-menu-button is what the user clicks, its sl:sidebar-menu-sub the links. The server opens the group holding the current page (th:open with #slNav.current), here /settings/billing. Sub-buttons come in two sizes.

```html
<sl:sidebar-provider>
  <sl:sidebar id="sidebar-sub">
    <sl:sidebar-content>
      <sl:sidebar-group id="sub-platform">
        <sl:sidebar-group-label>Platform</sl:sidebar-group-label>
        <sl:sidebar-group-content>
          <sl:sidebar-menu>
            <sl:sidebar-menu-item collapsible th:open="${#slNav.current('/projects')}">
              <sl:sidebar-menu-button>
                <sl:slot name="icon-start"><sl:icon name="folder"/></sl:slot>
                Projects
              </sl:sidebar-menu-button>
              <sl:sidebar-menu-sub>
                <sl:sidebar-menu-sub-item>
                  <sl:sidebar-menu-sub-button href="/projects/design">Design Engineering</sl:sidebar-menu-sub-button>
                </sl:sidebar-menu-sub-item>
                <sl:sidebar-menu-sub-item>
                  <sl:sidebar-menu-sub-button href="/projects/sales">Sales &amp; Marketing</sl:sidebar-menu-sub-button>
                </sl:sidebar-menu-sub-item>
              </sl:sidebar-menu-sub>
            </sl:sidebar-menu-item>
            <sl:sidebar-menu-item collapsible th:open="${#slNav.current('/settings')}">
              <sl:sidebar-menu-button>
                <sl:slot name="icon-start"><sl:icon name="settings"/></sl:slot>
                Settings
              </sl:sidebar-menu-button>
              <sl:sidebar-menu-sub>
                <sl:sidebar-menu-sub-item>
                  <sl:sidebar-menu-sub-button href="/settings"
                                              th:active="${#slNav.current('/settings', true)}">
                    General
                  </sl:sidebar-menu-sub-button>
                </sl:sidebar-menu-sub-item>
                <sl:sidebar-menu-sub-item>
                  <sl:sidebar-menu-sub-button href="/settings/billing"
                                              th:active="${#slNav.current('/settings/billing')}">
                    Billing
                  </sl:sidebar-menu-sub-button>
                </sl:sidebar-menu-sub-item>
                <sl:sidebar-menu-sub-item>
                  <sl:sidebar-menu-sub-button href="/settings/limits" size="sm">
                    Limits (small)
                  </sl:sidebar-menu-sub-button>
                </sl:sidebar-menu-sub-item>
              </sl:sidebar-menu-sub>
            </sl:sidebar-menu-item>
          </sl:sidebar-menu>
        </sl:sidebar-group-content>
      </sl:sidebar-group>
    </sl:sidebar-content>
  </sl:sidebar>
  <sl:sidebar-inset>
    <sl:site-header>
      <sl:sidebar-trigger for="sidebar-sub"/>
    </sl:site-header>
    <h1>Billing</h1>
  </sl:sidebar-inset>
</sl:sidebar-provider>
```

## One element for a desktop and a phone

The server does not know how wide the screen is, so the sidebar is one element for both:

- **From 768 px** (shadcn's `md`) it is a column beside the page, as tall as the screen and sticky while the page
  scrolls. The trigger, or **Ctrl+B** (**⌘B** on a Mac), collapses it out of view and expands it again. A collapsed
  sidebar's links leave the tab order.
- **Below 768 px** it is hidden, and the trigger opens it as a panel over the page, with the page dimmed behind it.
  The focus moves to the current page's link (`aria-current="page"`) or the first link. Tab stays in the panel, and the
  rest of the page is inert, so a screen reader finds only the navigation. Escape, a click outside, Ctrl/⌘B, following
  one of its links and widening the window past 768 px close it, and the focus goes back to the trigger.

The sidebar is a [popover](https://developer.mozilla.org/en-US/docs/Web/API/Popover_API) (`<nav popover>`): the
browser shows the panel in the top layer and closes it on Escape or a click outside, without script. On a desktop the
CSS shows it in the page instead, where it is a plain navigation landmark.

Ctrl/⌘B does nothing while the focus is in a rich text editor (`contenteditable`), where it means bold.

`<sl:sidebar-provider>` has to start at the top of the page. On a desktop the sidebar is sticky at the top of the
screen and as tall as the screen, so below a header of the page's own (shadcn's sidebar-16) it would slide under that
header and run past the bottom of the screen. Put the page's header inside `<sl:sidebar-inset>`, or the navigation in
the header with [`placement="header"`](#in-the-header).

## The state is remembered

When the user collapses or expands the sidebar, the browser stores the choice in a cookie, `sl-sidebar-state`
(`expanded` or `collapsed`, for a year, the whole site). `<sl:sidebar-provider>` reads it on the server, so the next
page renders in the same state from the first paint: nothing jumps, with or without htmx.

- `default-state="collapsed"` starts collapsed until the user chooses.
- `state` renders a state whatever the cookie says, for an application that keeps the choice itself, such as in the
  user's settings: `<sl:sidebar-provider th:state="${settings.sidebar}">`. Listen to `sl-sidebar-toggle` (below) to
  save it.

**Example: Collapsed.** state="collapsed" renders the sidebar collapsed whatever the cookie says, for an application that keeps the choice itself; default-state="collapsed" only until the user chose.

```html
<sl:sidebar-provider state="collapsed">
  <sl:sidebar id="sidebar-collapsed">
    <sl:sidebar-content>
      <sl:sidebar-group>
        <sl:sidebar-group-content>
          <sl:sidebar-menu>
            <sl:sidebar-menu-item>
              <sl:sidebar-menu-button href="/" active>Home</sl:sidebar-menu-button>
            </sl:sidebar-menu-item>
            <sl:sidebar-menu-item>
              <sl:sidebar-menu-button href="/settings">Settings</sl:sidebar-menu-button>
            </sl:sidebar-menu-item>
          </sl:sidebar-menu>
        </sl:sidebar-group-content>
      </sl:sidebar-group>
    </sl:sidebar-content>
  </sl:sidebar>
  <sl:sidebar-inset>
    <sl:site-header>
      <sl:sidebar-trigger for="sidebar-collapsed"/>
    </sl:site-header>
    <h1>Home</h1>
  </sl:sidebar-inset>
</sl:sidebar-provider>
```

The phone panel ignores the state: it always opens with everything in it.

## Variants

`variant` changes how the sidebar looks on a desktop, as shadcn/ui's:

- `sidebar` (the default) is a column along the edge of the screen, with a border on the page's side.
- `floating` is a panel with a margin all round, rounded (in the vega skin) and with a shadow. It takes as much room
  as the plain sidebar, so a page lays out the same with either.
- `inset` gives the sidebar no edge: its colour lies around the page, and `<sl:sidebar-inset>` becomes a card on it,
  with a margin, rounded corners (vega) and a shadow. The example at the top of this page, and the
  [sidebar layout](https://wimdeblauwe.github.io/shadleaf/0.8.0-SNAPSHOT/guides/application-shell.md#the-sidebar-layout), use it.

**Example: Variants.** variant="sidebar" (the default) is a column along the edge with a border, variant="floating" a panel with a margin all round; the inset variant is the first example. On a phone the panel is the same for all three.

```html
<sl:sidebar-provider>
  <sl:sidebar id="sidebar-floating" variant="floating">
    <sl:sidebar-content>
      <sl:sidebar-group>
        <sl:sidebar-group-content>
          <sl:sidebar-menu>
            <sl:sidebar-menu-item>
              <sl:sidebar-menu-button href="/" active>Home</sl:sidebar-menu-button>
            </sl:sidebar-menu-item>
            <sl:sidebar-menu-item>
              <sl:sidebar-menu-button href="/inbox">Inbox</sl:sidebar-menu-button>
            </sl:sidebar-menu-item>
            <sl:sidebar-menu-item>
              <sl:sidebar-menu-button href="/settings">Settings</sl:sidebar-menu-button>
            </sl:sidebar-menu-item>
          </sl:sidebar-menu>
        </sl:sidebar-group-content>
      </sl:sidebar-group>
    </sl:sidebar-content>
  </sl:sidebar>
  <sl:sidebar-inset>
    <sl:site-header>
      <sl:sidebar-trigger for="sidebar-floating"/>
    </sl:site-header>
    <h1>Floating</h1>
  </sl:sidebar-inset>
</sl:sidebar-provider>
```

Below 768 px the variant makes no difference: the panel and the page are the same for all three.

## Collapsed to icons

`collapsible="icon"` collapses the sidebar to a strip of its menu buttons' icons instead of moving it out of view, as
shadcn's sidebar-07 does. It is as wide as `--sidebar-width-icon` (3 rem in both skins); floating and inset add their
margin around it.

```html
<sl:sidebar variant="inset" collapsible="icon">
  ...
  <sl:sidebar-menu-button th:href="@{/inbox}" th:active="${#slNav.current('/inbox')}" tooltip="Inbox">
    <sl:slot name="icon-start"><sl:icon name="inbox"/></sl:slot>
    Inbox
  </sl:sidebar-menu-button>
```

- Every menu button becomes a square with its icon in the middle. The label is visually hidden, not removed: it stays
  the link's name, so a
  screen reader still says "Inbox, link". A large button (the application's name in the header) loses its padding,
  so a logo as large as the square fills it.
- Badges, actions, sub-menus and a collapsible item's chevron are hidden. A badge still describes its button. Group
  labels move out of view but still name their groups.
- `tooltip` on a menu button shows its label beside it, on hover and on keyboard focus, while the sidebar is collapsed
  to icons; never while it is expanded or on a phone. It repeats the button's name, so it is hidden from screen
  readers and describes nothing (as a [tooltip](https://wimdeblauwe.github.io/shadleaf/0.8.0-SNAPSHOT/components/tooltip.md#when-it-repeats-the-name) with `mode="label"`). Give every
  menu button one: without it, the icon alone has to say where the link goes.
- A menu button that [opens a menu](#menus-in-the-sidebar) still does, beside the strip; its tooltip never shows
  while the menu is open.
- A click (or Enter) on a [collapsible group's](#collapsible-groups) icon expands the sidebar and opens the group, as
  the sub-menu has no room in the strip. A group that holds the current page shows it on its icon.
- The server renders the strip from the cookie, so the first paint is right with and without JavaScript. On a phone
  the panel ignores it and shows everything.

**Example: Collapsed to icons.** collapsible="icon" collapses the sidebar to a strip of its menu buttons' icons (from 768 px). The labels stay the buttons' names; a tooltip shows them on hover and keyboard focus. Badges, actions and sub-menus are hidden; the icon of a group holding the current page (here: Billing, below Settings) is marked, and a click on it expands the sidebar with the group open. Rendered collapsed (state="collapsed"), for a request for /settings/billing.

```html
<sl:sidebar-provider state="collapsed">
  <sl:sidebar id="sidebar-icon" collapsible="icon">
    <sl:sidebar-header>
      <sl:sidebar-menu>
        <sl:sidebar-menu-item>
          <sl:sidebar-menu-button href="/" size="lg" tooltip="Acme Inc.">
            <sl:slot name="icon-start"><sl:icon name="gallery-vertical-end"/></sl:slot>
            <strong>Acme Inc.</strong>
            <small>Enterprise</small>
          </sl:sidebar-menu-button>
        </sl:sidebar-menu-item>
      </sl:sidebar-menu>
    </sl:sidebar-header>
    <sl:sidebar-content>
      <sl:sidebar-group id="icon-platform">
        <sl:sidebar-group-label>Platform</sl:sidebar-group-label>
        <sl:sidebar-group-content>
          <sl:sidebar-menu>
            <sl:sidebar-menu-item>
              <sl:sidebar-menu-button href="/" th:active="${#slNav.current('/')}" tooltip="Home">
                <sl:slot name="icon-start"><sl:icon name="house"/></sl:slot>
                Home
              </sl:sidebar-menu-button>
            </sl:sidebar-menu-item>
            <sl:sidebar-menu-item id="icon-inbox">
              <sl:sidebar-menu-button href="/inbox" th:active="${#slNav.current('/inbox')}" tooltip="Inbox">
                <sl:slot name="icon-start"><sl:icon name="inbox"/></sl:slot>
                Inbox
              </sl:sidebar-menu-button>
              <sl:sidebar-menu-badge label="12 unread">12</sl:sidebar-menu-badge>
            </sl:sidebar-menu-item>
            <sl:sidebar-menu-item collapsible th:open="${#slNav.current('/settings')}">
              <sl:sidebar-menu-button tooltip="Settings">
                <sl:slot name="icon-start"><sl:icon name="settings"/></sl:slot>
                Settings
              </sl:sidebar-menu-button>
              <sl:sidebar-menu-sub>
                <sl:sidebar-menu-sub-item>
                  <sl:sidebar-menu-sub-button href="/settings"
                                              th:active="${#slNav.current('/settings', true)}">
                    General
                  </sl:sidebar-menu-sub-button>
                </sl:sidebar-menu-sub-item>
                <sl:sidebar-menu-sub-item>
                  <sl:sidebar-menu-sub-button href="/settings/billing"
                                              th:active="${#slNav.current('/settings/billing')}">
                    Billing
                  </sl:sidebar-menu-sub-button>
                </sl:sidebar-menu-sub-item>
              </sl:sidebar-menu-sub>
            </sl:sidebar-menu-item>
          </sl:sidebar-menu>
        </sl:sidebar-group-content>
      </sl:sidebar-group>
    </sl:sidebar-content>
  </sl:sidebar>
  <sl:sidebar-inset>
    <sl:site-header>
      <sl:sidebar-trigger for="sidebar-icon"/>
    </sl:site-header>
    <h1>Billing</h1>
  </sl:sidebar-inset>
</sl:sidebar-provider>
```

`collapsible="offcanvas"` (the default) moves the collapsed sidebar out of view. `collapsible="none"` never collapses
it: the trigger is hidden on a desktop and Ctrl/⌘B does nothing there, whatever the cookie says. On a phone it is the
panel, as with the other two.

## Side

`side="end"` puts the sidebar at the end of the line: the right, or the left on a right-to-left page (`dir="rtl"`).
The default, `start`, is the left, or the right on a right-to-left page. The panel slides in from the same edge.

**Example: On the end.** side="end" puts the sidebar on the end of the line (the right, or the left on a right-to-left page).

```html
<sl:sidebar-provider>
  <sl:sidebar id="sidebar-end" side="end">
    <sl:sidebar-content>
      <sl:sidebar-group>
        <sl:sidebar-group-content>
          <sl:sidebar-menu>
            <sl:sidebar-menu-item>
              <sl:sidebar-menu-button href="/" active>Home</sl:sidebar-menu-button>
            </sl:sidebar-menu-item>
            <sl:sidebar-menu-item>
              <sl:sidebar-menu-button href="/inbox">Inbox</sl:sidebar-menu-button>
            </sl:sidebar-menu-item>
            <sl:sidebar-menu-item>
              <sl:sidebar-menu-button href="/settings">Settings</sl:sidebar-menu-button>
            </sl:sidebar-menu-item>
          </sl:sidebar-menu>
        </sl:sidebar-group-content>
      </sl:sidebar-group>
    </sl:sidebar-content>
  </sl:sidebar>
  <sl:sidebar-inset>
    <sl:site-header>
      <sl:sidebar-trigger for="sidebar-end"/>
    </sl:site-header>
    <h1>Home</h1>
  </sl:sidebar-inset>
</sl:sidebar-provider>
```

## In the header

`placement="header"` puts the same `<sl:sidebar>` inside a [`<sl:site-header>`](https://wimdeblauwe.github.io/shadleaf/0.8.0-SNAPSHOT/components/site-header.md) instead of beside the
page: from 768 px it is a row of links in the header, and narrower it is the panel, as in the sidebar layout. Nothing
is rendered twice: it is one `nav` with one set of ids at every width. Inside `<sl:sidebar-provider>`, the header goes
first and `<sl:sidebar-inset>` below it:

```html
<sl:sidebar-provider>
  <sl:site-header>
    <sl:sidebar-trigger aria-label="Menu"><sl:icon name="menu"/></sl:sidebar-trigger>
    <sl:button as="a" th:href="@{/}" variant="ghost">Acme Inc.</sl:button>
    <sl:sidebar placement="header" variant="inset">
      <sl:sidebar-content>
        <sl:sidebar-group>
          <sl:sidebar-group-content>
            <sl:sidebar-menu>...</sl:sidebar-menu>
          </sl:sidebar-group-content>
        </sl:sidebar-group>
      </sl:sidebar-content>
    </sl:sidebar>
    <sl:slot name="end"><sl:theme-toggle/></sl:slot>
  </sl:site-header>
  <sl:sidebar-inset>...</sl:sidebar-inset>
</sl:sidebar-provider>
```

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

In the row the menu buttons show their labels only (no icons), the current page marked as in the sidebar. Groups run
one after the other; a group's label is visually hidden but still names it for a screen reader. Badges and actions
stay beside their links. Collapsible items, group actions and separators are left out of the row: a sub-menu that
drops down from a header is a different component (a navigation menu), which Shadleaf does not have yet. Mark such
an item [panel-only](#panel-only-parts) so it is still in the panel.

When the links do not fit, the row scrolls sideways (with a thin scrollbar where the system shows them), and a link the
keyboard reaches scrolls into view; the page itself never gets wider. Keep the row short with panel-only parts.

The row never collapses, so `collapsible`, the cookie's state and Ctrl/⌘B do nothing on a desktop, and the trigger
is hidden there. Give the trigger a menu icon and a name of its own, as above: it opens the panel on a phone only.
`side` still picks the panel's edge. Of the variants only `inset` counts: the page becomes a card below the header, on
the sidebar's colour, as in the sidebar layout.

On a phone the menu button opens the same `nav` as the panel, with everything in it: icons, group labels, collapsible
groups, and the panel-only parts.

**Example: Header layout on a phone.** The same markup in a frame 390 px wide: the row is gone and the menu button opens the nav as a panel, with the panel-only parts (the name, Favorites, Settings and Help) in it. The panel takes the focus and makes the page behind it inert; Escape, a click outside or a followed link closes it.

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

## Panel-only parts

`panel-only` on `<sl:sidebar-header>`, `<sl:sidebar-footer>`, `<sl:sidebar-group>` or `<sl:sidebar-menu-item>` shows
it only in the phone panel: from 768 px it is left out (`display: none`). In the header placement it keeps the row
short (favourites, Settings and Help in the examples above), and gives the panel a header of its own with the
application's name, which the site header already shows on a desktop. It works in the sidebar layout too, for an
entry that is only needed on a phone.

```html
<sl:sidebar-group id="more" panel-only>
  <sl:sidebar-group-label>More</sl:sidebar-group-label>
  ...
</sl:sidebar-group>
```

## Trigger

The trigger is a ghost icon button with a panel icon (turned around on a right-to-left page), named "Toggle sidebar"
(the message `sl.sidebar.trigger`). An `aria-label` replaces the name, and content of its own replaces the icon:

```html
<sl:sidebar-trigger aria-label="Menu"><sl:icon name="menu"/></sl:sidebar-trigger>
```

`for` points it at a sidebar with another `id` than `sidebar`. `variant` and `size` are the button's (icon sizes only).

## Motion

On a desktop the sidebar slides out and the page widens with it (collapsed to icons, it narrows to the strip and the
group labels slide up); on a phone the panel slides in from its edge and the backdrop fades. With reduced motion (`prefers-reduced-motion: reduce`) the changes are immediate.

## Without JavaScript

The parts inside the sidebar need none: the current page, the badges and the actions are all in the markup (an action
does what its attributes say), and collapsible groups are the browser's `details`. Tooltips need it: without
JavaScript a strip of icons shows none. The phone panel still opens and closes: that is the browser's popover. It is not modal then (Tab can leave it), and it
stays open when one of its links goes to a place on the same page. On a desktop nothing can collapse the sidebar, so
the trigger is hidden there (`@media (scripting: none)`); the sidebar shows in the state the cookie asked for, also
as a strip of icons, where a click on a group's icon then opens a sub-menu the strip hides.

## htmx

With `hx-boost` on the body, every page comes from the server with the sidebar in the cookie's state, the current
page marked and the panel closed. Boosting only the main area works too: following a link in the panel closes it, and a trigger htmx swaps in is
set up again. Going back through htmx's history shows the page with the panel closed (whether a popover is open is not
part of the markup htmx saves, and the panel closes before htmx takes its snapshot), and in the state the user chose
last: the snapshot holds the state of the moment it was taken, so the restored page reads the cookie, as the server
would.

A boosted link sends `HX-Request` too. A controller that answers htmx with a fragment (a table's rows, a dialog) must
tell such a link apart, or the sidebar's link to that page gets the fragment alone: a link the body's boost sends has
`HX-Boosted` and no `HX-Target` (the body has no id), and wants the whole page. The
[Application shell](https://wimdeblauwe.github.io/shadleaf/0.8.0-SNAPSHOT/guides/application-shell.md#with-htmx) guide shows the check.

## Left out

For now:

- a collapsible group whose chevron is a menu action beside a link (shadcn's sidebar-08), and sub-menus that open as
  dropdown menus while collapsed to icons (sidebar-06);
- `SidebarInput` (use `<sl:input>`) and `SidebarMenuSkeleton` (use `<sl:skeleton>`);
- `SidebarRail`, the strip along the sidebar's edge that toggles it;
- a second sidebar on the same page; Ctrl/⌘B toggles every sidebar on the page;
- in the header placement: sub-menus that drop down from the row (a navigation menu), and a "More" entry gathering
  the links that do not fit (the row scrolls instead);
- a sidebar below a header across the whole page (shadcn's sidebar-16): see
  [One element for a desktop and a phone](#one-element-for-a-desktop-and-a-phone).

## Attributes

### sl:sidebar-provider

Attributes of `<sl:sidebar-provider>`: The wrapper of an application shell with a sidebar: it holds `sl:sidebar`, and `sl:sidebar-inset` with the page's content (and the `sl:sidebar-trigger`), and it holds whether the sidebar is expanded or collapsed on a desktop. The server renders that state from the `sl-sidebar-state` cookie, which the browser writes when the user collapses or expands the sidebar (the trigger, or Ctrl/⌘+B), so every page starts in it without a jump. Below 768&nbsp;px the sidebar is a panel the trigger opens instead, and the state does not apply. Needs Alpine for the trigger and the shortcut; without JavaScript the sidebar stays as the server rendered it.

| Attribute | Values | Default | Description |
| --- | --- | --- | --- |
| `state` | `expanded`, `collapsed` | — | The state to render, whatever the cookie says: for an application that keeps the choice itself, such as in the user's settings. Without it, the cookie decides. |
| `default-state` | `expanded`, `collapsed` | `expanded` | The state before the user chose one (no cookie yet). |

Every prop can also be set from an expression: `th:state="${...}"`. Any other attribute passes through to the rendered element, and `class` is appended.

### sl:sidebar

Attributes of `<sl:sidebar>`: The application's navigation, inside `sl:sidebar-provider`. One element serves both widths: from 768&nbsp;px up it is a column beside the page, which the trigger collapses and expands; below that it is a panel over the page, which the trigger opens. It is a `nav` named "Main" (`sl.sidebar.label`; an `aria-label` replaces it) and a popover: the browser opens the panel in the top layer and closes it on Escape or a click outside. On a phone, Alpine moves the focus into the open panel and makes the rest of the page inert, and closes it when a link in it is followed. It holds `sl:sidebar-header`, `sl:sidebar-content` (groups of menus) and `sl:sidebar-footer`. With `placement="header"` it goes inside `sl:site-header` instead, where from 768&nbsp;px it is a row of links; on a phone it is the same panel.

| Attribute | Values | Default | Description |
| --- | --- | --- | --- |
| `id` | *string* | `sidebar` | The id the trigger points at (`sl:sidebar-trigger`'s `for`). |
| `side` | `start`, `end` | `start` | The edge it sits on: the start of a line (the left, or the right on a right-to-left page) or the end. |
| `placement` | `sidebar`, `header` | `sidebar` | Where it sits on a desktop: `sidebar` is a column beside the page; `header` a row of links inside `sl:site-header` (the header layout), which never collapses: `collapsible`, the collapsed state and the trigger do not apply there (the trigger is hidden), and `side` only picks the phone panel's edge. Collapsible items and the parts marked `panel-only` are left out of the row; the phone panel is the same for both. |
| `variant` | `sidebar`, `floating`, `inset` | `sidebar` | How it looks on a desktop: `sidebar` is a column along the edge with a border; `floating` a panel with a margin all round; `inset` lays the sidebar's ground around the page, which (`sl:sidebar-inset`) becomes a card on it. The phone panel looks the same for all three. In the header placement only `inset` counts: the page becomes a card below the header. |
| `collapsible` | `offcanvas`, `icon`, `none` | `offcanvas` | How it collapses on a desktop: `offcanvas` moves it out of view entirely; `icon` narrows it to a strip of its menu buttons' icons (labels, badges, actions and sub-menus hidden; a menu button's `tooltip` shows its label); `none` never collapses it, and the trigger is hidden on a desktop. The phone panel is the same for all three. |

Every prop can also be set from an expression: `th:id="${...}"`. Any other attribute passes through to the rendered element, and `class` is appended.

### sl:sidebar-trigger

Attributes of `<sl:sidebar-trigger>`: The icon button that collapses and expands the `sl:sidebar` on a desktop and opens its panel on a phone. Named "Toggle sidebar" (`sl.sidebar.trigger`; an `aria-label` replaces it); content of its own replaces the icon. Without JavaScript it is hidden on a desktop, where it could not do anything, and still opens the panel on a phone. Any other attribute passes through to the `sl:button`.

| Attribute | Values | Default | Description |
| --- | --- | --- | --- |
| `for` | *string* | `sidebar` | The `id` of the sidebar it toggles. |
| `variant` | `default`, `outline`, `ghost`, `destructive`, `secondary`, `link` | `ghost` | Visual style of the button, as on `sl:button`. |
| `size` | `icon`, `icon-xs`, `icon-sm`, `icon-lg` | `icon-sm` | Size of the button: one of `sl:button`'s icon sizes. |

Every prop can also be set from an expression: `th:for="${...}"`. Any other attribute passes through to the rendered element, and `class` is appended.

### sl:sidebar-inset

Attributes of `<sl:sidebar-inset>`: The page's main content beside the `sl:sidebar`: the `main` element, which takes the rest of the width. Put the page header with the `sl:sidebar-trigger` at its top. It is the target of a `sl:skip-link`: `tabindex="-1"` lets the link move the focus to it.

| Attribute | Values | Default | Description |
| --- | --- | --- | --- |
| `id` | *string* | `main` | The element's id, which a `sl:skip-link` points at (`main` is its default too). |

Every prop can also be set from an expression: `th:id="${...}"`. Any other attribute passes through to the rendered element, and `class` is appended.

### sl:sidebar-header

Attributes of `<sl:sidebar-header>`: The top of the `sl:sidebar`: the application or team, above the `sl:sidebar-content`. It does not scroll with the content.

| Attribute | Values | Default | Description |
| --- | --- | --- | --- |
| `panel-only` | *boolean* | — | Show it only in the phone panel: from 768&nbsp;px it is left out (`display: none`), so a header row or a sidebar stays short. |

Every prop can also be set from an expression: `th:panel-only="${...}"`. Any other attribute passes through to the rendered element, and `class` is appended.

### sl:sidebar-content

Attributes of `<sl:sidebar-content>`: The part of the `sl:sidebar` that scrolls, between `sl:sidebar-header` and `sl:sidebar-footer`; it holds the `sl:sidebar-group`s. A plain `div`: the sidebar itself is the navigation landmark.

`sl:sidebar-content` declares no props; every attribute passes through.

### sl:sidebar-footer

Attributes of `<sl:sidebar-footer>`: The bottom of the `sl:sidebar`: settings, help or the signed-in user, below the `sl:sidebar-content`. It does not scroll with the content.

| Attribute | Values | Default | Description |
| --- | --- | --- | --- |
| `panel-only` | *boolean* | — | Show it only in the phone panel: from 768&nbsp;px it is left out (`display: none`), so a header row or a sidebar stays short. |

Every prop can also be set from an expression: `th:panel-only="${...}"`. Any other attribute passes through to the rendered element, and `class` is appended.

### sl:sidebar-separator

Attributes of `<sl:sidebar-separator>`: A line between parts of the `sl:sidebar`, in the sidebar's border colour. Decorative: screen readers skip it.

`sl:sidebar-separator` declares no props; every attribute passes through.

### sl:sidebar-group

Attributes of `<sl:sidebar-group>`: A section of the `sl:sidebar`'s links: an optional `sl:sidebar-group-label` and `sl:sidebar-group-action`, then `sl:sidebar-group-content` with a `sl:sidebar-menu`. With a label it is a group named by that label (`role="group"`, `aria-labelledby`), so a screen reader says "Platform, group" when the focus moves into it; without one it is a plain `div`.

| Attribute | Values | Default | Description |
| --- | --- | --- | --- |
| `id` | *string* | — | The group's id. Its label gets `&lt;id&gt;-label`; without an id, each render generates one (`sl-sidebar-group-` and eight random characters). |
| `panel-only` | *boolean* | — | Show it only in the phone panel: from 768&nbsp;px it is left out (`display: none`), so a header row or a sidebar stays short. |

Every prop can also be set from an expression: `th:id="${...}"`. Any other attribute passes through to the rendered element, and `class` is appended.

### sl:sidebar-group-label

Attributes of `<sl:sidebar-group-label>`: The name of a `sl:sidebar-group`, in small muted text above its menu. The group is labelled by it, so it gets an id from the group (`&lt;group id&gt;-label`); give the group an `id` rather than the label.

`sl:sidebar-group-label` declares no props; every attribute passes through.

### sl:sidebar-group-action

Attributes of `<sl:sidebar-group-action>`: An icon button at the end of a `sl:sidebar-group`'s label, such as "Add project". It holds only an icon, so it needs an `aria-label`. Every other attribute passes through (`hx-*`, `commandfor`, `href`).

| Attribute | Values | Default | Description |
| --- | --- | --- | --- |
| `as` | `button`, `a` | `button` | Element to render: a `button`, or an anchor (`a`) to a page. |
| `type` | `button`, `submit`, `reset` | `button` | Submit behaviour of the button; defaults to `button`. |

Every prop can also be set from an expression: `th:as="${...}"`. Any other attribute passes through to the rendered element, and `class` is appended. An `aria-label` or `aria-labelledby` is required; rendering fails without one.

### sl:sidebar-group-content

Attributes of `<sl:sidebar-group-content>`: The content of a `sl:sidebar-group`, below its label: usually one `sl:sidebar-menu`.

`sl:sidebar-group-content` declares no props; every attribute passes through.

### sl:sidebar-menu

Attributes of `<sl:sidebar-menu>`: A list of links (or buttons) in the `sl:sidebar`: a `ul` of `sl:sidebar-menu-item`s, so a screen reader says how many there are.

`sl:sidebar-menu` declares no props; every attribute passes through.

### sl:sidebar-menu-item

Attributes of `<sl:sidebar-menu-item>`: One entry of a `sl:sidebar-menu`: an `li` holding a `sl:sidebar-menu-button`, and optionally a `sl:sidebar-menu-badge` or a `sl:sidebar-menu-action` beside it. With a badge, the button is described by it (`aria-describedby`), so a screen reader says "Inbox, link, 12". With `collapsible` it is a group of links that opens and closes: the menu button is what the user clicks, followed by a `sl:sidebar-menu-sub`.

| Attribute | Values | Default | Description |
| --- | --- | --- | --- |
| `id` | *string* | — | The item's id. Its badge gets `&lt;id&gt;-badge`; without an id, each render generates one (`sl-sidebar-menu-item-` and eight random characters). |
| `collapsible` | *boolean* | — | Make it a group that opens and closes, on the browser's `details` element (no JavaScript needed): its `sl:sidebar-menu-button` becomes the `summary`, with a chevron, and its `sl:sidebar-menu-sub` the content. Collapsed to icons, a click on the button expands the sidebar and opens the group. It takes no badge or action: the browser would hide them with the sub-menu. |
| `open` | *boolean* | — | Render a collapsible item open, for instance when it holds the current page: `th:open="${#slNav.current('/settings')}"`. |
| `panel-only` | *boolean* | — | Show it only in the phone panel: from 768&nbsp;px it is left out (`display: none`), so a header row or a sidebar stays short. |

Every prop can also be set from an expression: `th:id="${...}"`. Any other attribute passes through to the rendered element, and `class` is appended.

### sl:sidebar-menu-button

Attributes of `<sl:sidebar-menu-button>`: A link (or button) in a `sl:sidebar-menu-item`: an icon in the `icon-start` slot, then the label. Mark the current page with `active`, for instance `th:active="${#slNav.current('/people')}"`. The label is cut off with an ellipsis when it does not fit; for two lines, put each in an element of its own (`&lt;strong&gt;`, `&lt;small&gt;`). Every attribute that is not a prop passes through (`href`, `hx-*`); `class` is appended to `sidebar-menu-button`. In a `collapsible` `sl:sidebar-menu-item` it is the item's `summary`, which opens and closes the group, with a chevron at the end; `as`, `type` and `disabled` do not apply there, and it takes no `href`.

| Attribute | Values | Default | Description |
| --- | --- | --- | --- |
| `as` | `a`, `button` | `a` | Element to render: an anchor (`a`) to a page, or a `button` for an action. |
| `active` | *boolean* | — | The current one: the link gets `aria-current="page"`, a button `aria-current="true"`, and both the active look. |
| `variant` | `default`, `outline` | `default` | Visual style: `outline` adds a background and a border. |
| `size` | `default`, `sm`, `lg` | `default` | Height: `sm` for a dense list, `lg` for an entry with two lines, such as the application's name in the `sl:sidebar-header`. |
| `type` | `button`, `submit`, `reset` | `button` | Submit behaviour of a button; defaults to `button`. |
| `disabled` | *boolean* | — | Disable it. A disabled link loses its `href` and leaves the tab order. |
| `tooltip` | *string* | — | The label to show beside the button while the sidebar is collapsed to icons (`collapsible="icon"`), on hover and on keyboard focus; usually the same words as the label. Not shown while the sidebar is expanded or on a phone. It repeats the button's name, so it is hidden from assistive technology and describes nothing (`sl:tooltip`'s `mode="label"`). |

Every prop can also be set from an expression: `th:as="${...}"`. Any other attribute passes through to the rendered element, and `class` is appended.

### sl:sidebar-menu-action

Attributes of `<sl:sidebar-menu-action>`: An icon button at the end of a `sl:sidebar-menu-item`, beside its `sl:sidebar-menu-button`: a second action for that entry, such as "More" or "Remove from favourites". It holds only an icon, so it needs an `aria-label` that names the entry too ("More for Design Engineering"). Every other attribute passes through.

| Attribute | Values | Default | Description |
| --- | --- | --- | --- |
| `as` | `button`, `a` | `button` | Element to render: a `button`, or an anchor (`a`) to a page. |
| `type` | `button`, `submit`, `reset` | `button` | Submit behaviour of the button; defaults to `button`. |
| `show-on-hover` | *boolean* | — | From 768&nbsp;px, show it only while the pointer is over the item, the focus is in it, or its menu is open (`aria-expanded="true"`). Always shown on a phone. |

Every prop can also be set from an expression: `th:as="${...}"`. Any other attribute passes through to the rendered element, and `class` is appended. An `aria-label` or `aria-labelledby` is required; rendering fails without one.

### sl:sidebar-menu-badge

Attributes of `<sl:sidebar-menu-badge>`: A count at the end of a `sl:sidebar-menu-button`, inside the same `sl:sidebar-menu-item`. The button is described by it, so a screen reader says the count after the link's name ("Inbox, link, 12"); the badge gets its id from the item (`&lt;item id&gt;-badge`). Clicks go through it to the button.

| Attribute | Values | Default | Description |
| --- | --- | --- | --- |
| `label` | *string* | — | What a screen reader says instead of the visible text, such as "12 unread messages". The visible count is then hidden from it. |

Every prop can also be set from an expression: `th:label="${...}"`. Any other attribute passes through to the rendered element, and `class` is appended.

### sl:sidebar-menu-sub

Attributes of `<sl:sidebar-menu-sub>`: A list of links below a `sl:sidebar-menu-button`, indented along a line: a `ul` of `sl:sidebar-menu-sub-item`s. In a `collapsible` `sl:sidebar-menu-item` it is what opens and closes. Hidden while the sidebar is collapsed to icons.

`sl:sidebar-menu-sub` declares no props; every attribute passes through.

### sl:sidebar-menu-sub-item

Attributes of `<sl:sidebar-menu-sub-item>`: One entry of a `sl:sidebar-menu-sub`: an `li` holding a `sl:sidebar-menu-sub-button`.

`sl:sidebar-menu-sub-item` declares no props; every attribute passes through.

### sl:sidebar-menu-sub-button

Attributes of `<sl:sidebar-menu-sub-button>`: A link (or button) in a `sl:sidebar-menu-sub-item`. Mark the current page with `active`, for instance `th:active="${#slNav.current('/settings/billing')}"`. An optional icon goes in the `icon-start` slot; the label is cut off with an ellipsis when it does not fit. Every attribute that is not a prop passes through (`href`, `hx-*`); `class` is appended to `sidebar-menu-sub-button`.

| Attribute | Values | Default | Description |
| --- | --- | --- | --- |
| `as` | `a`, `button` | `a` | Element to render: an anchor (`a`) to a page, or a `button` for an action. |
| `active` | *boolean* | — | The current one: the link gets `aria-current="page"`, a button `aria-current="true"`, and both the active look. |
| `size` | `sm`, `md` | `md` | Text size: `sm` for a denser list. |
| `type` | `button`, `submit`, `reset` | `button` | Submit behaviour of a button; defaults to `button`. |
| `disabled` | *boolean* | — | Disable it. A disabled link loses its `href` and leaves the tab order. |

Every prop can also be set from an expression: `th:as="${...}"`. Any other attribute passes through to the rendered element, and `class` is appended.

## JavaScript

A small Alpine component, `slSidebar`, does the work: `<sl:sidebar-provider>` renders as
`<div class="sidebar-provider" x-data="slSidebar" data-state="expanded">`.

- **It needs Alpine**, and no Alpine plugin. It writes the cookie itself (a plain cookie, not `$persist`, which would
  store JSON in the browser where the server cannot read it). Alpine is included by default; see
  [Alpine](https://wimdeblauwe.github.io/shadleaf/0.8.0-SNAPSHOT/guides/alpine.md).
- **On a desktop it takes `popovertarget` off the trigger** and sets `aria-expanded` from the state instead: browsers
  report a `popovertarget` button as collapsed while its popover is closed, which the sidebar on a desktop always is.
  Below 768 px it puts it back. It finds the trigger by its `aria-controls`.
- **Collapsed to icons, it expands the sidebar** when a collapsible group's icon is clicked, and opens the group.
  Each menu button's tooltip is a second small Alpine component, `slSidebarMenuTooltip`, on the tooltip itself; it
  shares `slTooltip`'s code.
- **It fires `sl-sidebar-toggle`** on the provider (it bubbles) after the user collapses or expands the sidebar and when
  the panel opens or closes: `detail` is `{state: 'expanded' | 'collapsed', open, mobile}`.
- **Keep your own `x-data` off `<sl:sidebar-provider>`**: it would replace `slSidebar`. Put it on an element inside.
