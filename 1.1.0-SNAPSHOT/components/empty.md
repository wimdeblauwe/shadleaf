# Empty

> What a list, a table, a search or a page shows when there is nothing to show yet.

Source: https://shadleaf.dev/1.1.0-SNAPSHOT/components/empty/

**Example: Empty.**

```html
<sl:empty>
  <sl:empty-header>
    <sl:empty-media variant="icon"><sl:icon name="folder-code"/></sl:empty-media>
    <sl:empty-title level="3">No projects yet</sl:empty-title>
    <sl:empty-description>You haven't created any projects yet. Get started by creating your first
      project.</sl:empty-description>
  </sl:empty-header>
  <sl:empty-content>
    <sl:button as="a" href="/projects/new">Create project</sl:button>
  </sl:empty-content>
  <sl:button as="a" href="/docs/projects" variant="link" size="sm">
    Learn more
    <sl:slot name="icon-end"><sl:icon name="arrow-up-right"/></sl:slot>
  </sl:button>
</sl:empty>
```

## Built from parts

An empty state is `<sl:empty>` plus the parts you need: `<sl:empty-header>` (holding
`<sl:empty-media>`, `<sl:empty-title>` and `<sl:empty-description>`) and `<sl:empty-content>` for what to do next, such
as a button, a search field or a link. Everything is centred, and the empty state grows to fill a flex container, so
it sits in the middle of the space it is given.

`<sl:empty-media variant="icon">` puts an icon on a muted square. The default media shows its content as it is, for an
avatar or an image.

**Example: With an avatar.** The default media shows its content as it is.

```html
<sl:empty>
  <sl:empty-header>
    <sl:empty-media>
      <sl:avatar size="lg" label="Ada Lovelace">
        <sl:avatar-image th:src="@{/users/{id}/photo(id=${user.id})}"/>
        <sl:avatar-fallback>AL</sl:avatar-fallback>
      </sl:avatar>
    </sl:empty-media>
    <sl:empty-title level="3">User offline</sl:empty-title>
    <sl:empty-description>This user is currently offline. You can leave a message to notify them or try
      again later.</sl:empty-description>
  </sl:empty-header>
  <sl:empty-content>
    <sl:button size="sm">Leave message</sl:button>
  </sl:empty-content>
</sl:empty>
```

## A title that is a heading

`<sl:empty-title>` is plain text unless you give it a `level`. With one, it is announced as a heading of that level.
Pick the level that fits the page: an empty state under an `<h2>` gets `level="3"`.

## Outline

`border` draws a dashed line around the empty state, to mark out the area. It works without utility classes, so also
with the standalone stylesheet.

**Example: Outline.** border draws a dashed line around the empty state.

```html
<sl:empty border>
  <sl:empty-header>
    <sl:empty-media variant="icon"><sl:icon name="cloud"/></sl:empty-media>
    <sl:empty-title level="3">Cloud storage is empty</sl:empty-title>
    <sl:empty-description>Upload files to your cloud storage to access them anywhere.</sl:empty-description>
  </sl:empty-header>
  <sl:empty-content>
    <sl:button variant="outline" size="sm">Upload files</sl:button>
  </sl:empty-content>
</sl:empty>
```

## In a card

**Example: In a card.**

```html
<sl:card>
  <sl:card-header>
    <sl:card-title level="3">Projects</sl:card-title>
    <sl:card-description>Everything your team is working on</sl:card-description>
  </sl:card-header>
  <sl:card-content>
    <sl:empty>
      <sl:empty-header>
        <sl:empty-media variant="icon"><sl:icon name="folder"/></sl:empty-media>
        <sl:empty-title level="4">No projects yet</sl:empty-title>
        <sl:empty-description>Create a project to start working with your team.</sl:empty-description>
      </sl:empty-header>
      <sl:empty-content>
        <sl:button as="a" href="/projects/new">Create project</sl:button>
      </sl:empty-content>
    </sl:empty>
  </sl:card-content>
</sl:card>
```

## In an empty table

Put the empty state in [`<sl:table-empty>`](https://shadleaf.dev/1.1.0-SNAPSHOT/components/table.md), which spans every column: it replaces the row's
"No results." text.

**Example: In an empty table.** An sl:empty as the content of sl:table-empty replaces its "No results." text.

```html
<sl:table>
  <thead>
    <tr>
      <th>Name</th>
      <th>Email</th>
      <th>Role</th>
    </tr>
  </thead>
  <tbody>
    <tr th:each="person : ${people}">
      <td th:text="${person.name}">Ada Lovelace</td>
      <td th:text="${person.email}">ada@example.com</td>
      <td th:text="${person.role}">Owner</td>
    </tr>
    <sl:table-empty colspan="3" th:if="${people.empty}">
      <sl:empty>
        <sl:empty-header>
          <sl:empty-media variant="icon"><sl:icon name="users"/></sl:empty-media>
          <sl:empty-title level="3">No people yet</sl:empty-title>
          <sl:empty-description>Invite your team to collaborate on this project.</sl:empty-description>
        </sl:empty-header>
        <sl:empty-content>
          <sl:button as="a" href="/people/invite" size="sm">
            <sl:slot name="icon-start"><sl:icon name="plus"/></sl:slot>
            Invite people
          </sl:button>
        </sl:empty-content>
      </sl:empty>
    </sl:table-empty>
  </tbody>
</sl:table>
```

## With a search field

A link in a description is underlined. An `<sl:empty-description>` can also go in the content, below the field.

**Example: With a search field.** Links in the description are underlined.

```html
<sl:empty>
  <sl:empty-header>
    <sl:empty-title level="3">404 - Not found</sl:empty-title>
    <sl:empty-description>The page you're looking for doesn't exist. Try searching for what you need
      below.</sl:empty-description>
  </sl:empty-header>
  <sl:empty-content>
    <form action="/search" role="search" aria-label="Pages">
      <sl:input-group>
        <sl:input-group-input type="search" name="pageQuery" placeholder="Try searching for pages..."
                              aria-label="Search pages"/>
        <sl:input-group-addon><sl:icon name="search"/></sl:input-group-addon>
      </sl:input-group>
    </form>
    <sl:empty-description>Need help? <a href="/support">Contact support</a></sl:empty-description>
  </sl:empty-content>
</sl:empty>
```

## While it loads

An [`<sl:spinner>`](https://shadleaf.dev/1.1.0-SNAPSHOT/components/spinner.md) takes the icon's place in the media while something loads.

**Example: With a spinner.** While something loads, an sl:spinner takes the icon's place.

```html
<sl:empty>
  <sl:empty-header>
    <sl:empty-media variant="icon"><sl:spinner/></sl:empty-media>
    <sl:empty-title level="3">Processing your request</sl:empty-title>
    <sl:empty-description>Please wait while we process your request. Do not refresh the
      page.</sl:empty-description>
  </sl:empty-header>
  <sl:empty-content>
    <sl:button variant="outline" size="sm">Cancel</sl:button>
  </sl:empty-content>
</sl:empty>
```

## Attributes

### sl:empty

Attributes of `<sl:empty>`: An empty state: what a list, a table, a search or a page shows when there is nothing to show yet. Holds an `sl:empty-header` (media, title and description) and an optional `sl:empty-content` with what to do next, centred in the space it is given.

| Attribute | Values | Default | Description |
| --- | --- | --- | --- |
| `border` | *boolean* | — | A dashed line around the empty state, to mark out the area. |

Every prop can also be set from an expression: `th:border="${...}"`. Any other attribute passes through to the rendered element, and `class` is appended.

### sl:empty-header

Attributes of `<sl:empty-header>`: The top of an `sl:empty`: an optional `sl:empty-media`, an `sl:empty-title` and an `sl:empty-description`, stacked and centred.

`sl:empty-header` declares no props; every attribute passes through.

### sl:empty-media

Attributes of `<sl:empty-media>`: An icon, an avatar or an image above the title of an `sl:empty`, inside `sl:empty-header`.

| Attribute | Values | Default | Description |
| --- | --- | --- | --- |
| `variant` | `default`, `icon` | `default` | `icon` puts an icon on a muted square; `default` shows the content as it is, e.g. an avatar. |

Every prop can also be set from an expression: `th:variant="${...}"`. Any other attribute passes through to the rendered element, and `class` is appended.

### sl:empty-title

Attributes of `<sl:empty-title>`: The title of an `sl:empty`, inside `sl:empty-header`.

| Attribute | Values | Default | Description |
| --- | --- | --- | --- |
| `level` | `1`, `2`, `3`, `4`, `5`, `6` | — | Heading level. With it the title is a heading (`role="heading"` and `aria-level`) that screen reader users can jump to; without it, plain text. Pick the level that fits the page's outline. |

Every prop can also be set from an expression: `th:level="${...}"`. Any other attribute passes through to the rendered element, and `class` is appended.

### sl:empty-description

Attributes of `<sl:empty-description>`: The text below the title of an `sl:empty`. Links in it are underlined.

`sl:empty-description` declares no props; every attribute passes through.

### sl:empty-content

Attributes of `<sl:empty-content>`: What to do next in an `sl:empty`, below its header: a button, a search field, a link.

`sl:empty-content` declares no props; every attribute passes through.
