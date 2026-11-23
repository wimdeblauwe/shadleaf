# Item

> A row of content with media, a title, a description and actions, alone or in a list; for people, files, notifications or settings.

Source: https://shadleaf.dev/1.2.0-SNAPSHOT/components/item/

A row that shows one thing at a glance: an icon, avatar or image, a title and a description, and actions at its end.
Several make a list in an item group, or a grid of tiles.

**Example: Item.**

```html
<sl:item variant="outline">
  <sl:item-content>
    <sl:item-title>Basic item</sl:item-title>
    <sl:item-description>A simple item with a title and a description.</sl:item-description>
  </sl:item-content>
  <sl:item-actions>
    <sl:button variant="outline" size="sm">Action</sl:button>
  </sl:item-actions>
</sl:item>
<sl:item as="a" href="/profile" variant="outline" size="sm">
  <sl:item-media><sl:icon name="badge-check" size="20"/></sl:item-media>
  <sl:item-content>
    <sl:item-title>Your profile has been verified.</sl:item-title>
  </sl:item-content>
  <sl:item-actions><sl:icon name="chevron-right" size="16"/></sl:item-actions>
</sl:item>
```

## When to use

- A list of people, files, notifications, integrations or devices, each with a button or a menu.
- A setting shown as a title, an explanation and a button, such as "Two-factor authentication: Enable".
- A row that links to its detail page (`as="a"`).
- A grid of products or albums with a picture above each (`<sl:item-header>`, `layout="grid"`).

Use something else for:

- A box holding a form, a table or a chart: a [card](https://shadleaf.dev/1.2.0-SNAPSHOT/components/card.md).
- A checkbox or switch with its label and description: a [field](https://shadleaf.dev/1.2.0-SNAPSHOT/components/field.md).
- Rows with columns the user compares or sorts: a [table](https://shadleaf.dev/1.2.0-SNAPSHOT/components/table.md).
- A file attached to a form or a message: an [attachment](https://shadleaf.dev/1.2.0-SNAPSHOT/components/attachment.md).

## Built from parts

An item is `<sl:item>` plus the parts you need: `<sl:item-media>` (an icon, an avatar or an image),
`<sl:item-content>` (holding `<sl:item-title>` and `<sl:item-description>`) and `<sl:item-actions>`.
`<sl:item-header>` and `<sl:item-footer>` take a line of their own, above and below the rest. `<sl:item-group>` makes
several items a list.

An item only lays out what you put in it. For a form control with its label and description, such as a checkbox or a
switch, use [`<sl:field>`](https://shadleaf.dev/1.2.0-SNAPSHOT/components/field.md) instead.

## Variant

`variant` is `default` (no border or background), `outline` or `muted`.

**Example: Variant.** default has no border or background, outline a border, muted a muted background.

```html
<sl:item>
  <sl:item-media variant="icon"><sl:icon name="inbox"/></sl:item-media>
  <sl:item-content>
    <sl:item-title>Default variant</sl:item-title>
    <sl:item-description>Transparent background with no border.</sl:item-description>
  </sl:item-content>
</sl:item>
<sl:item variant="outline">
  <sl:item-media variant="icon"><sl:icon name="inbox"/></sl:item-media>
  <sl:item-content>
    <sl:item-title>Outline variant</sl:item-title>
    <sl:item-description>Outlined style with a visible border.</sl:item-description>
  </sl:item-content>
</sl:item>
<sl:item variant="muted">
  <sl:item-media variant="icon"><sl:icon name="inbox"/></sl:item-media>
  <sl:item-content>
    <sl:item-title>Muted variant</sl:item-title>
    <sl:item-description>Muted background for secondary content.</sl:item-description>
  </sl:item-content>
</sl:item>
```

## Size

`size` is `default`, `sm` or `xs`. It sets the padding and the gaps, the size of an image in the media and, in a
group, the gap between the items.

**Example: Size.** size is default, sm or xs.

```html
<sl:item variant="outline">
  <sl:item-media variant="icon"><sl:icon name="inbox"/></sl:item-media>
  <sl:item-content>
    <sl:item-title>Default size</sl:item-title>
    <sl:item-description>The standard size for most use cases.</sl:item-description>
  </sl:item-content>
</sl:item>
<sl:item variant="outline" size="sm">
  <sl:item-media variant="icon"><sl:icon name="inbox"/></sl:item-media>
  <sl:item-content>
    <sl:item-title>Small size</sl:item-title>
    <sl:item-description>A compact size for dense layouts.</sl:item-description>
  </sl:item-content>
</sl:item>
<sl:item variant="outline" size="xs">
  <sl:item-media variant="icon"><sl:icon name="inbox"/></sl:item-media>
  <sl:item-content>
    <sl:item-title>Extra small size</sl:item-title>
    <sl:item-description>The most compact size available.</sl:item-description>
  </sl:item-content>
</sl:item>
```

## Icon

`<sl:item-media variant="icon">` sizes an icon. When the item has a description, the media lines up with the title
instead of the middle of the item.

**Example: Icon.** variant="icon" on the media sizes an icon.

```html
<sl:item variant="outline">
  <sl:item-media variant="icon"><sl:icon name="shield-alert"/></sl:item-media>
  <sl:item-content>
    <sl:item-title>Security alert</sl:item-title>
    <sl:item-description>New login detected from an unknown device.</sl:item-description>
  </sl:item-content>
  <sl:item-actions>
    <sl:button size="sm" variant="outline">Review</sl:button>
  </sl:item-actions>
</sl:item>
```

## Avatar

The default media shows its content as it is: an [`<sl:avatar>`](https://shadleaf.dev/1.2.0-SNAPSHOT/components/avatar.md), or a few of them next to each other.

**Example: Avatar.** The default media shows an sl:avatar as it is.

```html
<sl:item variant="outline">
  <sl:item-media>
    <sl:avatar size="lg">
      <sl:avatar-image th:src="@{/users/{id}/photo(id=${user.id})}"/>
      <sl:avatar-fallback>AM</sl:avatar-fallback>
    </sl:avatar>
  </sl:item-media>
  <sl:item-content>
    <sl:item-title>Alex Morgan</sl:item-title>
    <sl:item-description>Last seen 5 months ago</sl:item-description>
  </sl:item-content>
  <sl:item-actions>
    <sl:button size="icon-sm" variant="outline" aria-label="Invite Alex Morgan"><sl:icon name="plus"/></sl:button>
  </sl:item-actions>
</sl:item>
<sl:item variant="outline">
  <sl:item-media>
    <sl:avatar><sl:avatar-fallback>JD</sl:avatar-fallback></sl:avatar>
    <sl:avatar><sl:avatar-fallback>KL</sl:avatar-fallback></sl:avatar>
  </sl:item-media>
  <sl:item-content>
    <sl:item-title>No team members</sl:item-title>
    <sl:item-description>Invite your team to collaborate on this project.</sl:item-description>
  </sl:item-content>
  <sl:item-actions>
    <sl:button size="sm" variant="outline">Invite</sl:button>
  </sl:item-actions>
</sl:item>
```

## Image

`<sl:item-media variant="image">` crops an `<img>` to a square that follows the item's size. The title already names
the item, so the image can have an empty `alt`. A second `<sl:item-content>` after the first is as wide as its text,
for a duration, a size or a price at the end.

**Example: Image.** variant="image" crops an image to a square. A second item-content is as wide as its text.

```html
<sl:item-group>
  <sl:item as="a" variant="outline" th:each="photo : ${photos}" th:href="@{/photos/{id}(id=${photo.id})}">
    <sl:item-media variant="image">
      <img th:src="@{/photos/{id}/thumbnail(id=${photo.id})}" alt="" loading="lazy">
    </sl:item-media>
    <sl:item-content>
      <sl:item-title>[[${photo.title}]]</sl:item-title>
      <sl:item-description>[[${photo.place}]]</sl:item-description>
    </sl:item-content>
    <sl:item-content>
      <sl:item-description>[[${photo.size}]]</sl:item-description>
    </sl:item-content>
  </sl:item>
</sl:item-group>
```

## Group

`<sl:item-group>` stacks items with a gap that follows their size. It is a list (`role="list"`) and every item in it a
list item, so a screen reader announces how many there are.

**Example: Group.** sl:item-group makes its items a list, with a gap that follows their size.

```html
<sl:item-group>
  <th:block th:each="person : ${people}">
    <sl:item variant="outline">
      <sl:item-media>
        <sl:avatar><sl:avatar-fallback>[[${person.initials}]]</sl:avatar-fallback></sl:avatar>
      </sl:item-media>
      <sl:item-content>
        <sl:item-title>[[${person.name}]]</sl:item-title>
        <sl:item-description>[[${person.email}]]</sl:item-description>
      </sl:item-content>
      <sl:item-actions>
        <sl:button variant="ghost" size="icon" th:aria-label="|Add ${person.name}|"><sl:icon name="plus"/></sl:button>
      </sl:item-actions>
    </sl:item>
  </th:block>
</sl:item-group>
```

`<sl:item-separator>` draws a line between two items, for items without a border. A group with separators has no gap
of its own: the separator's margins are the space between the items.

**Example: Separator.** sl:item-separator draws a line between items without a border; the group then has no gap of its own.

```html
<sl:item-group>
  <th:block th:each="person, status : ${people}">
    <sl:item>
      <sl:item-media>
        <sl:avatar><sl:avatar-fallback>[[${person.initials}]]</sl:avatar-fallback></sl:avatar>
      </sl:item-media>
      <sl:item-content>
        <sl:item-title>[[${person.name}]]</sl:item-title>
        <sl:item-description>[[${person.email}]]</sl:item-description>
      </sl:item-content>
      <sl:item-actions>
        <sl:button variant="ghost" size="icon" th:aria-label="|Add ${person.name}|"><sl:icon name="plus"/></sl:button>
      </sl:item-actions>
    </sl:item>
    <sl:item-separator th:unless="${status.last}"/>
  </th:block>
</sl:item-group>
```

## Header

`<sl:item-header>` takes the full width above the rest of the item; an image in it is cropped to a square. With
`layout="grid"` the group lays its items out in as many columns as fit the width; the width of a column comes from the
skin.

**Example: Header.** sl:item-header takes a line of its own above the content; layout="grid" lays the items out in columns.

```html
<sl:item-group layout="grid">
  <sl:item variant="outline" th:each="model : ${models}">
    <sl:item-header>
      <img th:src="@{/models/{id}/cover(id=${model.id})}" alt="">
    </sl:item-header>
    <sl:item-content>
      <sl:item-title>[[${model.name}]]</sl:item-title>
      <sl:item-description>[[${model.description}]]</sl:item-description>
    </sl:item-content>
  </sl:item>
</sl:item-group>
```

## Link

`as="a"` makes the whole item a link: give it an `href`. It is tinted on hover and draws the skin's focus ring. Its
title and description are the link's text, so keep them short, and put no other links or buttons in it: a link cannot
hold them.

**Example: Link.** as="a" makes the whole item a link, tinted on hover.

```html
<sl:item as="a" href="/docs">
  <sl:item-content>
    <sl:item-title>Visit our documentation</sl:item-title>
    <sl:item-description>Learn how to get started with our components.</sl:item-description>
  </sl:item-content>
  <sl:item-actions><sl:icon name="chevron-right" size="16"/></sl:item-actions>
</sl:item>
<sl:item as="a" href="https://example.com/resources" target="_blank" rel="noopener noreferrer" variant="outline">
  <sl:item-content>
    <sl:item-title>External resource</sl:item-title>
    <sl:item-description>Opens in a new tab.</sl:item-description>
  </sl:item-content>
  <sl:item-actions><sl:icon name="external-link" size="16"/></sl:item-actions>
</sl:item>
```

In a group a link item is wrapped in a `div` that is the list item, so the link keeps its role.

## Attributes

### sl:item

Attributes of `<sl:item>`: A row of content: an `sl:item-media` (an icon, an avatar or an image), an `sl:item-content` with an `sl:item-title` and an `sl:item-description`, and `sl:item-actions`; `sl:item-header` and `sl:item-footer` take a line of their own. Group items in an `sl:item-group`, which makes them a list. For a form control with its label, use `sl:field` instead.

| Attribute | Values | Default | Description |
| --- | --- | --- | --- |
| `variant` | `default`, `outline`, `muted` | `default` | Visual style: `default` has no border or background, `outline` a border, `muted` a muted background. |
| `size` | `default`, `sm`, `xs` | `default` | Size of the item: its padding, gaps and the size of an image in its media. |
| `as` | `div`, `a` | `div` | Element to render: a `div`, or an anchor (`a`, give it an `href`) that makes the whole item a link, tinted on hover. A link item holds no other links or buttons. |

Every prop can also be set from an expression: `th:variant="${...}"`. Any other attribute passes through to the rendered element, and `class` is appended.

### sl:item-media

Attributes of `<sl:item-media>`: The icon, avatar or image at the start of an `sl:item`. With an `sl:item-description` in the item it aligns with the title instead of the middle.

| Attribute | Values | Default | Description |
| --- | --- | --- | --- |
| `variant` | `default`, `icon`, `image` | `default` | `default` shows its content as it is (an `sl:avatar`, or a few of them), `icon` sizes an icon, `image` crops an `&lt;img&gt;` to a square that follows the item's size. |

Every prop can also be set from an expression: `th:variant="${...}"`. Any other attribute passes through to the rendered element, and `class` is appended.

### sl:item-content

Attributes of `<sl:item-content>`: The text of an `sl:item`: its `sl:item-title` and `sl:item-description`. Takes the space the media and the actions leave; a second content after it (a duration, a price) is as wide as its text.

`sl:item-content` declares no props; every attribute passes through.

### sl:item-title

Attributes of `<sl:item-title>`: The title of an `sl:item`, in its `sl:item-content`. It can hold an icon or an `sl:badge` next to the text.

| Attribute | Values | Default | Description |
| --- | --- | --- | --- |
| `level` | `1`, `2`, `3`, `4`, `5`, `6` | — | Heading level. With it the title is a heading (`role="heading"` and `aria-level`) that screen reader users can jump to; without it, plain text. Pick the level that fits the page's outline. |

Every prop can also be set from an expression: `th:level="${...}"`. Any other attribute passes through to the rendered element, and `class` is appended.

### sl:item-description

Attributes of `<sl:item-description>`: What else to know about an `sl:item`, below its title, in muted text. Cut off after two lines. A link in it is underlined.

`sl:item-description` declares no props; every attribute passes through.

### sl:item-actions

Attributes of `<sl:item-actions>`: The buttons (or an icon, such as a chevron) at the end of an `sl:item`.

`sl:item-actions` declares no props; every attribute passes through.

### sl:item-header

Attributes of `<sl:item-header>`: A line of its own above the rest of an `sl:item`, the full width of it: an image, or a title and an action at either end.

`sl:item-header` declares no props; every attribute passes through.

### sl:item-footer

Attributes of `<sl:item-footer>`: A line of its own below the rest of an `sl:item`, the full width of it, its content at either end.

`sl:item-footer` declares no props; every attribute passes through.

### sl:item-group

Attributes of `<sl:item-group>`: A list of `sl:item`s (`role="list"`, each item a list item), stacked with a gap that follows their size, or in a grid. Put an `sl:item-separator` between items for a line.

| Attribute | Values | Default | Description |
| --- | --- | --- | --- |
| `layout` | `list`, `grid` | `list` | `list` stacks the items; `grid` lays them out in as many columns as fit the width, for cards with an image in their `sl:item-header`. |

Every prop can also be set from an expression: `th:layout="${...}"`. Any other attribute passes through to the rendered element, and `class` is appended.

### sl:item-separator

Attributes of `<sl:item-separator>`: A horizontal line between the items of an `sl:item-group`: an `sl:separator`, decorative by default. Any attribute passes through to it.

`sl:item-separator` declares no props; every attribute passes through.
