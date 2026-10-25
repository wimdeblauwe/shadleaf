# Attachment

> A file or an image attached to a message, a form or a record, with its name, its size or state, and actions.

Source: https://shadleaf.dev/0.11.0-SNAPSHOT/components/attachment/

**Example: Attachment.**

```html
<sl:attachment>
  <sl:attachment-media><sl:icon name="file-text"/></sl:attachment-media>
  <sl:attachment-content>
    <sl:attachment-title>sales-dashboard.pdf</sl:attachment-title>
    <sl:attachment-description>PDF · 2.4 MB</sl:attachment-description>
  </sl:attachment-content>
  <sl:attachment-actions>
    <sl:attachment-action aria-label="Remove sales-dashboard.pdf"><sl:icon name="x"/></sl:attachment-action>
  </sl:attachment-actions>
</sl:attachment>
```

## Built from parts

An attachment is `<sl:attachment>` plus the parts you need, as in shadcn/ui: `<sl:attachment-media>` (an icon or an
image), `<sl:attachment-content>` (holding `<sl:attachment-title>` and `<sl:attachment-description>`),
`<sl:attachment-actions>` with one or more `<sl:attachment-action>`s, and an optional `<sl:attachment-trigger>` that
makes the whole card clickable. `<sl:attachment-group>` lays several out in a row or a grid.

The attachment only shows a file; it uploads nothing. Use it for files that are already on the server, such as the
documents on a customer's page, and for the list of a file upload, where your page or your script sets each one's
`state`.

An `<sl:attachment-action>` is an [`<sl:button>`](https://shadleaf.dev/0.11.0-SNAPSHOT/components/button.md), ghost and an extra small icon by default. It holds
only an icon, so give it an `aria-label` that names the file: "Remove report.pdf", not just "Remove", because a page
can show many.

A title or description that does not fit is cut off with an ellipsis. Without `full-width` an attachment is as wide
as its content; with it, it fills its container, for a list of attachments.

## Image

`<sl:attachment-media variant="image">` crops an `<img>` to fill the media's square. `orientation="vertical"` puts the
media above the content. The title already names the file, so the image can have an empty `alt`.

**Example: Image.** variant="image" crops the image to the media's square; orientation="vertical" puts it above the content. The trigger opens the file.

```html
<sl:attachment-group>
  <sl:attachment orientation="vertical" th:each="photo : ${photos}">
    <sl:attachment-media variant="image">
      <img th:src="@{/files/{id}/thumbnail(id=${photo.id})}" alt="" loading="lazy">
    </sl:attachment-media>
    <sl:attachment-content>
      <sl:attachment-title>[[${photo.name}]]</sl:attachment-title>
      <sl:attachment-description>[[${photo.meta}]]</sl:attachment-description>
    </sl:attachment-content>
    <sl:attachment-actions>
      <sl:attachment-action th:aria-label="|Remove ${photo.name}|"><sl:icon name="x"/></sl:attachment-action>
    </sl:attachment-actions>
    <sl:attachment-trigger as="a" th:href="@{/files/{id}(id=${photo.id})}" target="_blank"
                           th:aria-label="|Open ${photo.name}|"/>
  </sl:attachment>
</sl:attachment-group>
```

The image is your own `<img>`, so it works under a strict Content Security Policy as long as it comes from your own
server: point `th:src` at a thumbnail URL, such as `@{/files/{id}/thumbnail(id=${file.id})}`. A `data:` URL would need
`data:` in `img-src`, which a strict policy leaves out.

## States

`state` says where a file is in its upload:

- `idle`: picked, not sent yet. A dashed border.
- `uploading` and `processing`: the title shimmers (not for users who ask for reduced motion) and an image is dimmed.
  The attachment is marked busy (`aria-busy="true"`).
- `error`: a destructive border, media and description.
- `done`, the default: the file is there.

**Example: States.** idle draws a dashed border, uploading and processing shimmer the title, error turns destructive. The description says the state too.

```html
<sl:attachment state="idle" full-width>
  <sl:attachment-media><sl:icon name="clock"/></sl:attachment-media>
  <sl:attachment-content>
    <sl:attachment-title>selected-file.pdf</sl:attachment-title>
    <sl:attachment-description>Ready to upload</sl:attachment-description>
  </sl:attachment-content>
  <sl:attachment-actions>
    <sl:attachment-action aria-label="Remove selected-file.pdf"><sl:icon name="x"/></sl:attachment-action>
  </sl:attachment-actions>
</sl:attachment>
<sl:attachment state="uploading" full-width>
  <sl:attachment-media><sl:spinner aria-hidden="true"/></sl:attachment-media>
  <sl:attachment-content>
    <sl:attachment-title>design-system.zip</sl:attachment-title>
    <sl:attachment-description>Uploading · 64%</sl:attachment-description>
  </sl:attachment-content>
  <sl:attachment-actions>
    <sl:attachment-action aria-label="Cancel upload of design-system.zip"><sl:icon name="x"/></sl:attachment-action>
  </sl:attachment-actions>
</sl:attachment>
<sl:attachment state="processing" full-width>
  <sl:attachment-media><sl:icon name="file-text"/></sl:attachment-media>
  <sl:attachment-content>
    <sl:attachment-title>market-research.pdf</sl:attachment-title>
    <sl:attachment-description>Processing document</sl:attachment-description>
  </sl:attachment-content>
  <sl:attachment-actions>
    <sl:attachment-action aria-label="Remove market-research.pdf"><sl:icon name="x"/></sl:attachment-action>
  </sl:attachment-actions>
</sl:attachment>
<sl:attachment state="error" full-width>
  <sl:attachment-media><sl:icon name="file-warning"/></sl:attachment-media>
  <sl:attachment-content>
    <sl:attachment-title>financial-model.xlsx</sl:attachment-title>
    <sl:attachment-description>Upload failed. Try again.</sl:attachment-description>
  </sl:attachment-content>
  <sl:attachment-actions>
    <sl:attachment-action aria-label="Retry upload of financial-model.xlsx"><sl:icon name="refresh-cw"/></sl:attachment-action>
    <sl:attachment-action aria-label="Remove financial-model.xlsx"><sl:icon name="x"/></sl:attachment-action>
  </sl:attachment-actions>
</sl:attachment>
<sl:attachment state="done" full-width>
  <sl:attachment-media><sl:icon name="check"/></sl:attachment-media>
  <sl:attachment-content>
    <sl:attachment-title>uploaded-report.pdf</sl:attachment-title>
    <sl:attachment-description>Uploaded · 1.8 MB</sl:attachment-description>
  </sl:attachment-content>
  <sl:attachment-actions>
    <sl:attachment-action aria-label="Remove uploaded-report.pdf"><sl:icon name="x"/></sl:attachment-action>
  </sl:attachment-actions>
</sl:attachment>
```

The state is not announced by itself. Say it in the description as well ("Uploading · 64%", "Upload failed. Try
again."), so it is not told by colour alone and a screen reader reads it with the file. The attachment is no live
region and has no `role="alert"`: a page that renders a failed upload would announce it on every load. When a script
changes the state, it should announce the change itself, once, in one polite live region for the whole list.

An [`<sl:spinner>`](https://shadleaf.dev/0.11.0-SNAPSHOT/components/spinner.md) in the media shows that a file is uploading. The description already says so: give
the spinner `aria-hidden="true"`, so "Loading" is not read too. There is no progress bar; show the percentage in the
description.

## Sizes

`size` is `default`, `sm` or `xs`.

**Example: Sizes.**

```html
<sl:attachment full-width>
  <sl:attachment-media><sl:icon name="file-text"/></sl:attachment-media>
  <sl:attachment-content>
    <sl:attachment-title>Default attachment</sl:attachment-title>
    <sl:attachment-description>PDF · 2.4 MB</sl:attachment-description>
  </sl:attachment-content>
</sl:attachment>
<sl:attachment size="sm" full-width>
  <sl:attachment-media><sl:icon name="file-text"/></sl:attachment-media>
  <sl:attachment-content>
    <sl:attachment-title>Small attachment</sl:attachment-title>
    <sl:attachment-description>PDF · 2.4 MB</sl:attachment-description>
  </sl:attachment-content>
</sl:attachment>
<sl:attachment size="xs" full-width>
  <sl:attachment-media><sl:icon name="file-text"/></sl:attachment-media>
  <sl:attachment-content>
    <sl:attachment-title>Extra small attachment</sl:attachment-title>
  </sl:attachment-content>
</sl:attachment>
```

## Group

`<sl:attachment-group>` lays attachments out in a row that scrolls sideways when they do not fit, snapping to each
attachment, its edges fading out. The scrollbar is hidden, as in shadcn: the fade shows there is more.

**Example: Group.** A row that scrolls sideways when the attachments do not fit, snapping to each one. Tab reaches the ones off screen through their actions.

```html
<sl:attachment-group>
  <sl:attachment>
    <sl:attachment-media><sl:icon name="file-text"/></sl:attachment-media>
    <sl:attachment-content>
      <sl:attachment-title>briefing-notes.pdf</sl:attachment-title>
      <sl:attachment-description>PDF · 1.4 MB</sl:attachment-description>
    </sl:attachment-content>
    <sl:attachment-actions>
      <sl:attachment-action aria-label="Remove briefing-notes.pdf"><sl:icon name="x"/></sl:attachment-action>
    </sl:attachment-actions>
  </sl:attachment>
  <sl:attachment>
    <sl:attachment-media variant="image">
      <img th:src="@{/files/{id}/thumbnail(id=${photo.id})}" alt="" loading="lazy">
    </sl:attachment-media>
    <sl:attachment-content>
      <sl:attachment-title>dunes.png</sl:attachment-title>
      <sl:attachment-description>PNG · 820 KB</sl:attachment-description>
    </sl:attachment-content>
    <sl:attachment-actions>
      <sl:attachment-action aria-label="Remove dunes.png"><sl:icon name="x"/></sl:attachment-action>
    </sl:attachment-actions>
  </sl:attachment>
  <sl:attachment>
    <sl:attachment-media><sl:icon name="table"/></sl:attachment-media>
    <sl:attachment-content>
      <sl:attachment-title>customers.csv</sl:attachment-title>
      <sl:attachment-description>CSV · 18 KB</sl:attachment-description>
    </sl:attachment-content>
    <sl:attachment-actions>
      <sl:attachment-action aria-label="Remove customers.csv"><sl:icon name="x"/></sl:attachment-action>
    </sl:attachment-actions>
  </sl:attachment>
  <sl:attachment>
    <sl:attachment-media><sl:icon name="file-code"/></sl:attachment-media>
    <sl:attachment-content>
      <sl:attachment-title>renderer.tsx</sl:attachment-title>
      <sl:attachment-description>TSX · 12 KB</sl:attachment-description>
    </sl:attachment-content>
    <sl:attachment-actions>
      <sl:attachment-action aria-label="Remove renderer.tsx"><sl:icon name="x"/></sl:attachment-action>
    </sl:attachment-actions>
  </sl:attachment>
</sl:attachment-group>
```

Keyboard users reach attachments off screen by tabbing to their actions or triggers. A row of attachments without any
has nothing to tab to: give the group a `label`. It is then a named group (`role="group"`) that takes the focus, and
the arrow keys scroll it.

**Example: Group without actions.** Attachments without actions or a trigger give the keyboard nothing to tab to. label makes the group a named group that takes the focus, so the arrow keys scroll it.

```html
<sl:attachment-group label="Reference photos">
  <sl:attachment orientation="vertical" th:each="photo : ${photos}">
    <sl:attachment-media variant="image">
      <img th:src="@{/files/{id}/thumbnail(id=${photo.id})}" th:alt="${photo.description}" loading="lazy">
    </sl:attachment-media>
  </sl:attachment>
</sl:attachment-group>
```

## Grid

`layout="grid"` wraps the attachments instead, in as many columns as fit the width, for a gallery of photos. It does
not scroll, so nothing is hidden off screen and the edges do not fade. Give each attachment `orientation="vertical"`
and `full-width`, so it fills its cell: the image is cropped to a square and the actions sit in its corner. The width
of a cell and the gap between them come from the skin.

**Example: Grid.** layout="grid" wraps the attachments in columns that fill the width, for a gallery of photos. Each vertical attachment with full-width fills its cell.

```html
<sl:attachment-group layout="grid" label="Inspection photos">
  <sl:attachment orientation="vertical" full-width th:each="photo : ${photos}">
    <sl:attachment-media variant="image">
      <img th:src="@{/files/{id}/thumbnail(id=${photo.id})}" alt="" loading="lazy">
    </sl:attachment-media>
    <sl:attachment-content>
      <sl:attachment-title>[[${photo.name}]]</sl:attachment-title>
      <sl:attachment-description>[[${photo.meta}]]</sl:attachment-description>
    </sl:attachment-content>
    <sl:attachment-actions>
      <sl:attachment-action th:aria-label="|Remove ${photo.name}|"><sl:icon name="x"/></sl:attachment-action>
    </sl:attachment-actions>
    <sl:attachment-trigger as="a" th:href="@{/files/{id}(id=${photo.id})}" target="_blank"
                           th:aria-label="|Open ${photo.name}|"/>
  </sl:attachment>
</sl:attachment-group>
```

A grid's `label` still makes it a named group, but the group takes no focus: there is nothing to scroll. Tab goes
from one attachment's actions and trigger to the next.

## Trigger

`<sl:attachment-trigger>` covers the whole card, behind its actions, so a click anywhere but on an action activates
it. It is a button, to open an [`<sl:dialog>`](https://shadleaf.dev/0.11.0-SNAPSHOT/components/dialog.md) with `commandfor` and `command="show-modal"`, or with
`as="a"` a link that opens the file. It has no text of its own: give it an `aria-label` that says what it does. Put
it last in the attachment, so Tab reaches the actions first; its focus ring is drawn around the whole card.

**Example: Trigger.** The trigger covers the card and opens a dialog with commandfor; the actions above it stay clickable.

```html
<sl:attachment full-width>
  <sl:attachment-media><sl:icon name="file-search"/></sl:attachment-media>
  <sl:attachment-content>
    <sl:attachment-title>research-summary.pdf</sl:attachment-title>
    <sl:attachment-description>Open preview dialog</sl:attachment-description>
  </sl:attachment-content>
  <sl:attachment-actions>
    <sl:attachment-action aria-label="Copy link to research-summary.pdf"><sl:icon name="copy"/></sl:attachment-action>
    <sl:attachment-action aria-label="Remove research-summary.pdf"><sl:icon name="x"/></sl:attachment-action>
  </sl:attachment-actions>
  <sl:attachment-trigger commandfor="research-summary-preview" command="show-modal"
                         aria-label="Preview research-summary.pdf"/>
</sl:attachment>
<sl:dialog id="research-summary-preview">
  <sl:dialog-header>
    <sl:dialog-title>research-summary.pdf</sl:dialog-title>
    <sl:dialog-description>The attachment trigger fills the card and opens the dialog, while the actions stay
      independently clickable above it.</sl:dialog-description>
  </sl:dialog-header>
</sl:dialog>
```

## Download

An action can be a link (`as="a"`) and take any of the button's variants and sizes, such as a download button in a
message thread.

On a page with `hx-boost`, add `hx-boost="false"` to a download link: htmx boosts every link to your own server
without a `target`, `download` or not, and would put the file's bytes into the page instead of saving it. A trigger
that opens the file in a new tab (`target="_blank"`) is left alone.

**Example: Download.** An action can be a link (as="a") in another variant and size, as in a message thread.

```html
<sl:attachment>
  <sl:attachment-media><sl:icon name="file-text"/></sl:attachment-media>
  <sl:attachment-content>
    <sl:attachment-title>sales-dashboard.pdf</sl:attachment-title>
    <sl:attachment-description>PDF · 2.4 MB</sl:attachment-description>
  </sl:attachment-content>
  <sl:attachment-actions>
    <sl:attachment-action as="a" href="/files/sales-dashboard.pdf" download variant="secondary" size="icon-sm"
                          aria-label="Download sales-dashboard.pdf"><sl:icon name="download"/></sl:attachment-action>
  </sl:attachment-actions>
</sl:attachment>
```

## Without content

An attachment can be only its media, as an image in a chat message. Then the image's `alt` names it.

**Example: Without content.** An attachment can be only its media, as an image in a chat message. The image's alt is its name then.

```html
<sl:attachment orientation="vertical">
  <sl:attachment-media variant="image">
    <img th:src="@{/files/{id}/thumbnail(id=${photo.id})}" alt="A mountain lake">
  </sl:attachment-media>
</sl:attachment>
```

## Attributes

### sl:attachment

Attributes of `<sl:attachment>`: A file or an image attached to something: a message, a form, a record. Holds an `sl:attachment-media`, an `sl:attachment-content` (title and description), optional `sl:attachment-actions` and an optional `sl:attachment-trigger` that makes the whole card open a link or a dialog. Purely presentational: the server renders its state. Say the state in the description too ("Uploading · 64%", "Upload failed"), so it is not told by colour alone.

| Attribute | Values | Default | Description |
| --- | --- | --- | --- |
| `state` | `idle`, `uploading`, `processing`, `error`, `done` | `done` | Where the file is in its upload: `idle` (picked, not sent yet) draws a dashed border, `uploading` and `processing` shimmer the title and mark the attachment busy (`aria-busy`), `error` turns it destructive. `done` is a file that is there. |
| `size` | `default`, `sm`, `xs` | `default` | Size of the attachment. |
| `orientation` | `horizontal`, `vertical` | `horizontal` | `vertical` puts the media above the content, for an image. |
| `full-width` | *boolean* | — | Fill the width of the container, for a list of attachments (shadcn's `w-full` class). Without it the attachment is as wide as its content. |

Every prop can also be set from an expression: `th:state="${...}"`. Any other attribute passes through to the rendered element, and `class` is appended.

### sl:attachment-media

Attributes of `<sl:attachment-media>`: The icon or image of an `sl:attachment`: an icon (or an `sl:spinner` while it uploads) on a muted square, or an `&lt;img&gt;` that fills it.

| Attribute | Values | Default | Description |
| --- | --- | --- | --- |
| `variant` | `icon`, `image` | `icon` | `icon` centres an icon on a muted square; `image` crops an `&lt;img&gt;` to fill the square, dimmed while the attachment uploads, is processed or failed. |

Every prop can also be set from an expression: `th:variant="${...}"`. Any other attribute passes through to the rendered element, and `class` is appended.

### sl:attachment-content

Attributes of `<sl:attachment-content>`: The text of an `sl:attachment`: its `sl:attachment-title` and `sl:attachment-description`. Takes the space the media and the actions leave.

`sl:attachment-content` declares no props; every attribute passes through.

### sl:attachment-title

Attributes of `<sl:attachment-title>`: The name of an attachment, usually its file name, in `sl:attachment-content`. Cut off with an ellipsis when it does not fit; shimmers while the attachment uploads or is processed, unless the user asks for reduced motion.

`sl:attachment-title` declares no props; every attribute passes through.

### sl:attachment-description

Attributes of `<sl:attachment-description>`: What else to know about an attachment, below its title: the file type and size, or its state ("Uploading · 64%", "Upload failed. Try again."). Cut off with an ellipsis when it does not fit.

`sl:attachment-description` declares no props; every attribute passes through.

### sl:attachment-actions

Attributes of `<sl:attachment-actions>`: The `sl:attachment-action`s of an attachment, at its end; in a vertical attachment, over the top corner of the media. They sit above an `sl:attachment-trigger`, so they stay clickable.

`sl:attachment-actions` declares no props; every attribute passes through.

### sl:attachment-action

Attributes of `<sl:attachment-action>`: A button in `sl:attachment-actions`, to remove, retry or download the file: a `sl:button`, ghost and an extra small icon by default. Give it an `aria-label` that names the file ("Remove report.pdf"). Any other attribute passes through to the `sl:button` (`href`, `type`, `disabled`, `hx-*`).

| Attribute | Values | Default | Description |
| --- | --- | --- | --- |
| `variant` | `default`, `outline`, `ghost`, `destructive`, `secondary`, `link` | `ghost` | Visual style of the button, as on `sl:button`. |
| `size` | `xs`, `sm`, `default`, `lg`, `icon`, `icon-xs`, `icon-sm`, `icon-lg` | `icon-xs` | Size of the button, as on `sl:button`. The `icon` sizes are square, for a button that holds only an icon, and need an `aria-label`. |
| `as` | `button`, `a` | `button` | Element to render: a `button`, or an anchor (`a`) to download or open the file. |

Every prop can also be set from an expression: `th:variant="${...}"`. Any other attribute passes through to the rendered element, and `class` is appended. An `aria-label` or `aria-labelledby` is required when `size` is `icon` or `icon-xs` or `icon-sm` or `icon-lg`; rendering fails without one.

### sl:attachment-trigger

Attributes of `<sl:attachment-trigger>`: Makes a whole `sl:attachment` clickable: a button (to open an `sl:dialog` with `commandfor` and `command="show-modal"`) or, with `as="a"`, a link that covers the card behind its actions. It has no text of its own, so it needs an `aria-label` that says what it does ("Open report.pdf"). Put it last in the attachment.

| Attribute | Values | Default | Description |
| --- | --- | --- | --- |
| `as` | `button`, `a` | `button` | Element to render: a `button`, or an anchor (`a`) to open the file. |

Every prop can also be set from an expression: `th:as="${...}"`. Any other attribute passes through to the rendered element, and `class` is appended. An `aria-label` or `aria-labelledby` is required; rendering fails without one.

### sl:attachment-group

Attributes of `<sl:attachment-group>`: A row of `sl:attachment`s that scrolls sideways when they do not fit, snapping to each attachment, its edges fading out, or a grid that wraps them. Keyboard users reach attachments off screen by tabbing to their actions or triggers; for a row of attachments without any, give the group a `label`.

| Attribute | Values | Default | Description |
| --- | --- | --- | --- |
| `layout` | `row`, `grid` | `row` | `row` scrolls sideways; `grid` wraps the attachments in columns that fill the width, for a gallery of photos: vertical attachments with `full-width`, each filling its cell. |
| `label` | *string* | — | Accessible name of the group. With it the group is a named group (`role="group"`). A row then takes keyboard focus, so the arrow keys scroll it: for a row of attachments that have no actions or trigger. A grid does not scroll, so it takes no focus. |

Every prop can also be set from an expression: `th:layout="${...}"`. Any other attribute passes through to the rendered element, and `class` is appended.
