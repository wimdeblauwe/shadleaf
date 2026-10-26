# File Upload

> A drop zone with a Browse button and the list of the files picked, around a file input bound with th:field.

Source: https://shadleaf.dev/1.0.0/components/file-upload/

**Example: File upload.** Drop files on it or browse. Each file shows as an attachment with a remove button; a file of another type or larger than max-size shows as an error.

```html
<sl:field th:field="*{uploadAttachments}">
  <sl:field-label>Attachments</sl:field-label>
  <sl:file-upload multiple accept="image/png,image/jpeg,application/pdf" max-size="10MB">
    <sl:slot name="description">PNG, JPG, PDF up to 10 MB</sl:slot>
  </sl:file-upload>
  <sl:field-error/>
</sl:field>
```

## A file input with a drop zone

shadcn/ui has no file upload component, so this one is Shadleaf's own. It looks like shadcn's file upload card: an
outlined [empty state](https://shadleaf.dev/1.0.0/components/empty.md) with an icon, a title, a description and a Browse button.

Underneath is a plain `<input type="file">`, visually hidden. It holds the files and the form submits it, so it binds
with `th:field` as any other input does, to a `MultipartFile`, or with `multiple` to a `List<MultipartFile>`. The form
needs `enctype="multipart/form-data"`:

```html
<form th:object="${upload}" th:action="@{/people/{id}/files(id=${person.id})}" method="post"
      enctype="multipart/form-data">
  <sl:field th:field="*{files}">
    <sl:field-label>Attachments</sl:field-label>
    <sl:file-upload multiple accept="image/png,image/jpeg,application/pdf" max-size="10MB">
      <sl:slot name="description">PNG, JPG, PDF up to 10 MB</sl:slot>
    </sl:file-upload>
    <sl:field-error/>
  </sl:field>
  <sl:button type="submit">Upload</sl:button>
</form>
```

```java
public class UploadForm {
  @NotEmpty
  private List<MultipartFile> files = new ArrayList<>();
  // getter and setter
}
```

Every attribute that is not a prop goes to the input: `th:field`, `name`, `id`, `required`, `disabled`, `aria-*`,
`hx-*`. `class` stays on the component. In an [`<sl:field>`](https://shadleaf.dev/1.0.0/components/field.md) the input takes the field's binding, its id
(which the field's label points at), `aria-describedby` and `aria-invalid`, and a field error colours the drop zone's
border.

Spring Boot limits uploads to 1 MB a file and 10 MB a request by default. Raise
`spring.servlet.multipart.max-file-size` and `max-request-size` to match `max-size`.

## What the script adds

With JavaScript (the `slFileUpload` Alpine component), the component does more:

- **Drop.** Files dropped anywhere on the component are added. The drop zone takes the ring colour while files are
  dragged over it.
- **A list.** Each file shows below the drop zone as an [`<sl:attachment>`](https://shadleaf.dev/1.0.0/components/attachment.md) in its `idle` state, with
  its name, type and size and a button that removes it. The list is in the order the files were added.
- **Adding more.** With `multiple`, picking or dropping files again adds them to the ones there; the browser would
  replace them. A file that is there already is not added twice. Without `multiple`, a new file replaces the old one.
- **Checks.** A file that does not match `accept`, or is larger than `max-size`, is not added. It shows as an
  attachment in its `error` state, with the reason, until it is dismissed or files are added again. The browser's
  picker applies `accept` by itself, but a drop does not, and the picker lets one choose any file.
- **Announcements.** Added, removed and rejected files are announced once per change, in one polite live region: "Added
  report.pdf and photo.png."

Everything still goes through the input: the script puts the files back with a `DataTransfer` (`input.files =
dt.files`), so the form submits exactly what the list shows, and a drop dispatches `change` as a pick does, so
`hx-trigger="change"` on the component works for both.

**Example: In a card.** As shadcn's file upload card.

```html
<sl:card>
  <sl:card-header>
    <sl:card-title>File Upload</sl:card-title>
    <sl:card-description>Drag and drop or browse</sl:card-description>
  </sl:card-header>
  <sl:card-content>
    <sl:file-upload th:field="*{uploadCardFiles}" multiple max-size="10MB">
      <sl:slot name="description">PNG, JPG, PDF up to 10MB</sl:slot>
    </sl:file-upload>
  </sl:card-content>
</sl:card>
```

The server checks the files anyway: `accept` and `max-size` only spare the user a round trip. A browser without
JavaScript, or a request not made through the page, can send anything.

## One file

Without `multiple`, the title and button say so ("Upload a file", "Browse"). The slots `media`, `title` and
`description` replace the icon and the texts, and `browse-label` the button's text.

**Example: One file.** Without multiple, a new file replaces the one picked before. The slots and browse-label change the icon, title and button.

```html
<sl:file-upload th:field="*{uploadAvatar}" accept="image/*" max-size="2MB" browse-label="Choose a photo">
  <sl:slot name="media"><sl:icon name="image"/></sl:slot>
  <sl:slot name="title">Profile photo</sl:slot>
  <sl:slot name="description">An image of up to 2 MB</sl:slot>
</sl:file-upload>
```

## Image previews

With `previews`, a picked or dropped image shows a thumbnail in place of the file icon. It stays through the upload,
dimmed while the file is sent, processed, or failed, as an [attachment's image](https://shadleaf.dev/1.0.0/components/attachment.md#states) is.

**Example: Image previews.** With previews, a picked image shows a thumbnail instead of the file icon, from a blob URL; other files keep theirs. A Content-Security-Policy must allow blob in img-src.

```html
<sl:file-upload th:field="*{uploadPhotos}" multiple accept="image/*,.pdf" max-size="10MB" previews>
  <sl:slot name="media"><sl:icon name="images"/></sl:slot>
  <sl:slot name="description">Images and PDFs up to 10 MB</sl:slot>
</sl:file-upload>
```

- **Which files.** Images the browser can show (`image/*`), up to 20 MB: decoding a larger photo takes much memory and
  time. A file the browser cannot decode (HEIC, outside Safari) keeps its icon, and no broken image shows: the
  thumbnail takes the icon's place only once it has loaded. SVG keeps its icon too. Shown in an `<img>` an SVG runs
  no script, but its `blob:` URL has the page's origin, and opened from it ("Open image in new tab") it would.
- **The image.** Its `alt` is empty: the file's name is right beside it.
- **The URLs.** Each thumbnail is a `blob:` URL (`URL.createObjectURL`), made in the browser: the file is not sent
  for it. It is revoked when the file leaves the list (removed, replaced, the next change after an upload), and when
  the component is swapped out. A page htmx saves in its history gets the icons back, so it holds no URL that no
  longer works.

### Content-Security-Policy

A policy without `img-src`, or with `img-src 'self'`, blocks `blob:` images, so add it:

```text
img-src 'self' blob:
```

Without it the files keep their icons and the upload works as before. The browser reports the blocked image in the
console; after that, the page makes no more previews. See [Content-Security-Policy](https://shadleaf.dev/1.0.0/guides/csp.md).

## Grid

With `layout="grid"`, the picked files show as cards in columns that fill the width, instead of small attachments one
below the other. With `previews`, a photo then shows as a large square thumbnail: big enough to judge, say, the
photos of an inspection before they are sent with the rest of the form.

**Example: Grid.** With layout grid, the picked files show as cards in columns that fill the width, images with a large thumbnail, other files with their icon, rejected ones as errors.

```html
<sl:field th:field="*{uploadInspectionPhotos}">
  <sl:field-label>Photos of defects</sl:field-label>
  <sl:file-upload multiple accept="image/*,.pdf" max-size="10MB" layout="grid" previews>
    <sl:slot name="media"><sl:icon name="images"/></sl:slot>
    <sl:slot name="description">Images and PDFs up to 10 MB</sl:slot>
  </sl:file-upload>
</sl:field>
```

```html
<sl:file-upload th:field="*{photos}" multiple accept="image/*,.pdf" max-size="10MB" layout="grid" previews>
  <sl:slot name="description">Images and PDFs up to 10 MB</sl:slot>
</sl:file-upload>
```

- **The cards.** Each file is a vertical [`<sl:attachment>`](https://shadleaf.dev/1.0.0/components/attachment.md#image) at the default size, filling
  its cell; the grid is the one of [`<sl:attachment-group layout="grid">`](https://shadleaf.dev/1.0.0/components/attachment.md#grid), so both look
  alike. Other files show their icon in the same square, and a rejected file shows as an error card with the reason.
- **The buttons.** The remove or dismiss button sits in the card's corner, and is hidden while the files are sent.
- **The order.** The cards are in the order of the files in the input, as in the list, so the focus order, the focus
  after a remove and the announcements are the same.

Without JavaScript nothing changes: there is no list.

## Errors

A second submit renders the form again with the binding's errors, as for any field. The input is marked
`aria-invalid` and described by the error. A browser never keeps the files of a file input across a page load, so the
user picks them again.

**Example: With errors.** The server checks the files anyway. A field error marks the input aria-invalid and the drop zone's border destructive.

```html
<sl:field th:field="*{uploadContract}">
  <sl:field-label>Signed contract</sl:field-label>
  <sl:file-upload accept=".pdf">
    <sl:slot name="description">A PDF</sl:slot>
  </sl:file-upload>
  <sl:field-error/>
</sl:field>
```

## Disabled

A disabled input fades the Browse button, and the component takes no files.

**Example: Disabled.** A disabled input fades the Browse button, and the component takes no files.

```html
<sl:file-upload th:field="*{uploadArchive}" disabled>
  <sl:slot name="description">Uploads are closed for this project.</sl:slot>
</sl:file-upload>
```

## Accessibility

- The input is the only tab stop. Space or Enter opens the file picker, as on any file input; the Browse button is
  its `<label>` (`<sl:button as="label">`), so a click opens it too, and the input's focus ring is drawn on the button.
- The input's name is the field's label and the Browse button's text; it is described by the drop zone's description
  and the field's description and error.
- Each remove button is named after its file: "Remove report.pdf". After a remove, the focus moves to the next
  file's button, or back to the input.
- Dropping is a pointer gesture; the keyboard path is the input.
- An upload is announced once when it starts ("Uploading 3 files."), and again when it fails, is cancelled, or ends
  while the component is still there. The percentages are not announced; each uploading or processing attachment is
  `aria-busy`. After a Cancel, the focus goes back to the input.
- Its texts and announcements are messages (`sl.file-upload.*`); your own message files can reword them or add a
  language, as [Translating](https://shadleaf.dev/1.0.0/guides/translating.md) explains.

## Without JavaScript

The Browse button still opens the picker and the form submits the files. There is no drop and no list: the browser
shows nothing of the picked files but what the hidden input would.

## htmx

The component can be swapped in and out. The browser never restores a file input's files, and htmx's history restore
renders the page's markup again: the list is drawn from the input's files whenever the component starts, so going back
shows an empty upload, never files that would not be sent. A page saved in the history while uploading comes back
as an upload that is not sending anything.

## Upload progress

When the form is sent with htmx (`hx-post` on the form, or a boosted form), the component shows how far the upload
is, with no attribute of its own. It takes part in every htmx request whose body carries its files, so the form needs
`enctype="multipart/form-data"` or `hx-encoding="multipart/form-data"`. The files go through the
[attachment's states](https://shadleaf.dev/1.0.0/components/attachment.md#states):

1. **Uploading.** Each file shows the share of its bytes sent: "Uploading · 64%".
2. **Processing**, once the whole request is sent and the server is still working on it.
3. **The outcome.** On success the server's answer is swapped in, as for any htmx request. It usually replaces the
   component, with an empty form and what was uploaded, so the answer is what tells the user it worked. A
   [toast](https://shadleaf.dev/1.0.0/components/toast.md) or the focus on the new files does that well. If the component stays, its files show as
   "Uploaded" and leave the input, so a second submit does not send them again. On a failed request (an error status,
   no connection, a timeout), the files show "Upload failed" and stay in the input, so the user can submit again.

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

The browser reports the progress of the whole request, not of each file. The files go into the request one after the
other, in the list's order, so each one's share is worked out from where its bytes are in the request. The figure is
off by the few bytes of the form's other fields, which hardly shows.

While the files are sent, they cannot be added or removed: the input is disabled and the remove buttons are hidden. A
**Cancel upload** button takes the Browse button's place. It aborts the request (`htmx:abort`), and the files are back
as they were, ready to send again. All files go in one request, so cancelling stops them all.

- **htmx 2** sends with an `XMLHttpRequest`, and the component follows its upload.
- **htmx 4** sends with `fetch()`, which reports no upload progress. For a request that carries the files, the
  component gives htmx a `fetch` of its own (`ctx.fetch`), which sends with an `XMLHttpRequest`. Everything else
  (swapping, `HX-*` headers, history, `htmx:abort`) is still htmx's. htmx 4 aborts a request after 60 seconds by
  default; for large uploads, raise its `defaultTimeout` or set `hx-config` on the form. htmx 4 also swaps an error
  answer in, where htmx 2 leaves the page alone: if that answer replaces the component, it shows what went wrong
  instead of the failed files.

Without htmx, a plain POST, nothing of this happens: the browser shows its own progress, if any.

Spring Boot answers a request larger than `spring.servlet.multipart.max-request-size` with an error, which shows as a
failed upload: set the limits to what the form may send.

## Attributes

Attributes of `<sl:file-upload>`: A drop zone to pick files with, and the list of the files picked. Shadleaf's own: shadcn/ui has none. A visually hidden `&lt;input type="file"&gt;` holds the files and is submitted with the form, so put `th:field` on the component (a `MultipartFile`, or with `multiple` a `List&lt;MultipartFile&gt;`) and give the form `enctype="multipart/form-data"`. Every attribute that is not a prop goes to the input (`th:field`, `name`, `id`, `required`, `disabled`, `aria-*`, `hx-*`); `class` is appended to the component. In a `sl:field`, the input takes the field's `th:field`, id and `aria-describedby`. With JavaScript, files can be dropped too, each picked file shows as an `sl:attachment` with a remove button, and a file that does not match `accept` or is larger than `max-size` shows as an error. When the form is sent with htmx (2 or 4), each file shows its upload progress, then that the server is processing it, and an error if the request fails; a Cancel button stops the upload. With `previews`, a picked image shows a thumbnail. Without JavaScript, the Browse button still picks files. The server checks the files anyway.

| Attribute | Values | Default | Description |
| --- | --- | --- | --- |
| `multiple` | *boolean* | — | Take more than one file. Picking or dropping files adds them to the ones already there. |
| `accept` | *string* | — | The file types to take, as the input's `accept`: extensions (`.pdf`), MIME types (`application/pdf`) or `image/*`, separated by commas. A dropped file of another type is not added. |
| `max-size` | *string* | — | The largest file to take, as Spring Boot writes sizes: `10MB`, `500KB`. A larger file is not added. |
| `level` | *number* | — | Makes the title a heading of this level (`role="heading"`). |
| `previews` | *boolean* | — | Show a thumbnail of each picked image (up to 20 MB, no SVG) instead of the file icon. The thumbnail is a `blob:` URL: a Content-Security-Policy must allow it in `img-src`, or the icon stays. |
| `layout` | `list`, `grid` | `list` | `list` shows the picked files one below the other, as small attachments; `grid` as cards in columns that fill the width, the grid of `sl:attachment-group layout="grid"`: with `previews`, for photos one wants to see before sending them. |
| `browse-label` | *string* | — | The text of the Browse button. Defaults to "Browse files", or "Browse" without `multiple`. |

Every prop can also be set from an expression: `th:multiple="${...}"`. Any other attribute passes through to the rendered element, and `class` is appended.
