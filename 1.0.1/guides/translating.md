# Translating

> The components' built-in texts are messages, which Shadleaf ships in several languages; your own message files add a language or reword them.

Source: https://shadleaf.dev/1.0.1/guides/translating/

Some Shadleaf components have built-in texts, such as the "Cancel" button of an alert dialog.
Shadleaf ships these texts in the languages [listed below](#supported-languages), and your application can add a language or reword a text if needed.

## How it works

Shadleaf puts its messages behind your application's message source: it becomes the last parent of the `messageSource`
bean, whether Spring Boot configured it (`spring.messages.basename`) or you declared it. Spring asks your messages first,
so a key you define wins; the keys you leave out come from Shadleaf. An application without messages of its own gets
Shadleaf's as its message source.

The request's locale picks the language, as for your own messages. Spring Boot takes it from the `Accept-Language`
header unless you set `spring.web.locale` and `spring.web.locale-resolver=fixed`, or declare a `LocaleResolver` of your
own. A language Shadleaf has no messages for gets English, whatever the server's own locale is.

Set the page's language too, on the `<html>` element of your layout. Shadleaf formats numbers, dates and lists in the
browser (charts, the selection count of a table, the file sizes of a file upload) in that language:

```html
<html th:lang="${#locale.toLanguageTag()}">
```

## Your application's messages

Keep your own keys in your `messages.properties` and `messages_<language>.properties`, as you would without Shadleaf.
Leave Shadleaf's keys out of them: they come from Shadleaf, in the page's language.

Set this in `application.properties`:

```properties
spring.messages.fallback-to-system-locale=false
```

Without it, Spring Boot answers a language your application has no file for with the server's language: on a server
in Dutch, a French page gets the texts of your `messages_nl.properties`, while Shadleaf, which never falls back to the
server's language, writes English. With it off, such a page gets your `messages.properties` and Shadleaf's English.

## Adding a language

For a language Shadleaf does not ship, add its keys to your file of that language, next to your own keys:

```properties title="src/main/resources/messages_fr.properties"
app.greeting=Bonjour
sl.alert-dialog.cancel=Annuler
sl.pagination.previous=Précédent
sl.pagination.next=Suivant
```

The [list below](#the-keys) has every key. A key you leave out shows Shadleaf's English text.

## Rewording a text

To word one of Shadleaf's texts differently, define its key in your file of that language, such as
`messages_en.properties` for English. Not in `messages.properties`: Spring reads that file for every language, so the
new wording would replace Shadleaf's other translations too. This needs `fallback-to-system-locale` off, as
[above](#your-applications-messages); on a server in English, a page in another language Shadleaf ships would read
your `messages_en.properties` otherwise.

An `aria-label` or the content you give a component replaces its message for that one element, as each component page
says.

## Arguments and apostrophes

Some messages have arguments: `sl.table.select-row=Select {0}` gets the row's name. A message with arguments is a
`MessageFormat` pattern, in which an apostrophe starts quoted text, so write an apostrophe twice:

```properties
sl.file-upload.announce.uploading={0} fichiers en cours d''envoi.
sl.dialog.error=Une erreur s'est produite. Veuillez réessayer.
```

A message without arguments is used as it is written, with a single apostrophe, as long as
`spring.messages.always-use-message-format` stays off (the default). With it on, every message in your own files is a
pattern and needs the doubled apostrophe.

Some messages are filled in by the browser: the selection count of a table (`sl.table.selection.count`), the number
of results and the chips' remove buttons of a combobox (`sl.combobox.results`, `sl.combobox.remove`), the range of a
date picker (`sl.date-picker.range`) and the file upload's messages with arguments (`sl.file-upload.*`). The server
renders their pattern into the page with `{0}` left in, and the script replaces it. They are the same keys, translated
the same way, but keep their arguments plain: `{0}`, never `{0,number}` or `{0,choice,...}`.

## Supported languages

These languages are supported by Shadleaf:

- 🇬🇧 English (`en`)
- 🇩🇪 German (`de`)
- 🇫🇷 French (`fr`)
- 🇳🇱 Dutch (`nl`)

## The keys

Every key with its English text, generated from the library's `messages.properties`, with the comment it has there.
The texts are shown as written in the file.

### [Button](https://shadleaf.dev/1.0.1/components/button.md)

| Key | English |
| --- | --- |
| `sl.button.loading` | `Loading` |

### [Spinner](https://shadleaf.dev/1.0.1/components/spinner.md)

| Key | English |
| --- | --- |
| `sl.spinner.label` | `Loading` |

### [Form errors](https://shadleaf.dev/1.0.1/components/form-errors.md)

| Key | English |
| --- | --- |
| `sl.form-errors.title` | `There is a problem` |

### [Dialog](https://shadleaf.dev/1.0.1/components/dialog.md)

| Key | English |
| --- | --- |
| `sl.dialog.close` | `Close` |
| `sl.dialog.error` | `Something went wrong. Please try again.` |

### [Alert Dialog](https://shadleaf.dev/1.0.1/components/alert-dialog.md)

| Key | English |
| --- | --- |
| `sl.alert-dialog.cancel` | `Cancel` |

### [Toast](https://shadleaf.dev/1.0.1/components/toast.md)

| Key | English |
| --- | --- |
| `sl.toaster.label` | `Notifications (Alt+T)` |
| `sl.toast.close` | `Close` |

### [Theme Toggle](https://shadleaf.dev/1.0.1/components/theme-toggle.md)

| Key | English |
| --- | --- |
| `sl.theme-toggle.label` | `Theme` |
| `sl.theme-toggle.dark-mode` | `Dark mode` |
| `sl.theme-toggle.light` | `Light` |
| `sl.theme-toggle.dark` | `Dark` |
| `sl.theme-toggle.system` | `System` |

### [Table](https://shadleaf.dev/1.0.1/components/table.md)

| Key | English |
| --- | --- |
| `sl.table.empty` | `No results.` |
| `sl.table.select-all` | `Select all rows on this page` |
| `sl.table.select-row`: {0}: what the row is, e.g. a name. For the row checkboxes: th:aria-label="#{sl.table.select-row(${person.name})}". | `Select {0}` |
| `sl.table.selection.count`: {0}: the selected rows, {1}: the rows on the page. The browser fills them in as the selection changes, so keep them as plain {0} and {1}: no number or choice format. | `{0} of {1} selected` |

### [Pagination](https://shadleaf.dev/1.0.1/components/pagination.md)

| Key | English |
| --- | --- |
| `sl.pagination.label` | `Pagination` |
| `sl.pagination.previous` | `Previous` |
| `sl.pagination.previous.label` | `Go to previous page` |
| `sl.pagination.next` | `Next` |
| `sl.pagination.next.label` | `Go to next page` |
| `sl.pagination.more` | `More pages` |
| `sl.pagination.summary`: {0} and {1}: the first and the last row on the page, {2}: all rows. A Slice does not know the total. | `{0}–{1} of {2}` |
| `sl.pagination.summary.slice` | `{0}–{1}` |
| `sl.pagination.summary.empty` | `0 of 0` |
| `sl.pagination.summary.page`: {0}: the page, {1}: the number of pages. | `Page {0} of {1}` |
| `sl.pagination.summary.page.slice` | `Page {0}` |
| `sl.pagination-size.label` | `Rows per page` |

### [Sidebar](https://shadleaf.dev/1.0.1/components/sidebar.md)

| Key | English |
| --- | --- |
| `sl.sidebar.label` | `Main` |
| `sl.sidebar.trigger` | `Toggle sidebar` |

### [Breadcrumb](https://shadleaf.dev/1.0.1/components/breadcrumb.md)

| Key | English |
| --- | --- |
| `sl.breadcrumb.label` | `Breadcrumb` |
| `sl.breadcrumb.more` | `More levels` |
| `sl.breadcrumb.more.trigger` | `Show more levels` |

### [Skip Link](https://shadleaf.dev/1.0.1/components/skip-link.md)

| Key | English |
| --- | --- |
| `sl.skip-link` | `Skip to main content` |

### [Calendar](https://shadleaf.dev/1.0.1/components/calendar.md)

| Key | English |
| --- | --- |
| `sl.calendar.previous` | `Go to the previous month` |
| `sl.calendar.next` | `Go to the next month` |
| `sl.calendar.month` | `Month` |
| `sl.calendar.year` | `Year` |
| `sl.calendar.week-number` | `Week` |

### [Date Picker](https://shadleaf.dev/1.0.1/components/date-picker.md)

| Key | English |
| --- | --- |
| `sl.date-picker.placeholder` | `Pick a date` |
| `sl.date-picker.dialog` | `Choose a date` |
| `sl.date-picker.range`: {0} and {1}: the start and the end of a range, each formatted for the locale. | `{0} – {1}` |

### [Combobox](https://shadleaf.dev/1.0.1/components/combobox.md)

| Key | English |
| --- | --- |
| `sl.combobox.empty` | `No items found.` |
| `sl.combobox.clear` | `Clear` |
| `sl.combobox.toggle` | `Show options` |
| `sl.combobox.remove`: {0}: a chosen option's text, on the button of its chip. | `Remove {0}` |
| `sl.combobox.results.one`: The number of options that match, announced while typing. The browser fills in {0}, so keep it as a plain {0}: no number or choice format. | `1 result` |
| `sl.combobox.results` | `{0} results` |
| `sl.combobox.searching` | `Searching…` |
| `sl.combobox.search-error` | `The options could not be loaded.` |

### [User Menu](https://shadleaf.dev/1.0.1/components/user-menu.md)

| Key | English |
| --- | --- |
| `sl.user-menu.trigger`: {0}: the signed-in user's name. The name of sl:user-menu's button in the header, which shows only the picture. | `Account: {0}` |
| `sl.user-menu.sign-in` | `Sign in` |
| `sl.user-menu.sign-out` | `Sign out` |

### [File Upload](https://shadleaf.dev/1.0.1/components/file-upload.md)

| Key | English |
| --- | --- |
| `sl.file-upload.title`: sl:file-upload. The title and the Browse button, for a multiple and a single (.one) upload. | `Upload files` |
| `sl.file-upload.title.one` | `Upload a file` |
| `sl.file-upload.browse` | `Browse files` |
| `sl.file-upload.browse.one` | `Browse` |
| `sl.file-upload.remove`: The browser fills in {0} and {1}, so keep them plain: no number or choice format. {0}: a file's name. | `Remove {0}` |
| `sl.file-upload.dismiss` | `Dismiss {0}` |
| `sl.file-upload.rejected.type`: Why a dropped or picked file was not added. {0}: the max-size, formatted in the page's language ("10 MB"). | `File type not accepted` |
| `sl.file-upload.rejected.size` | `Larger than {0}` |
| `sl.file-upload.rejected.count` | `Only one file can be added` |
| `sl.file-upload.announce.added`: Announced after a change. {0}: the files' names, as a list in the page's language; {1}: why they were not added. | `Added {0}.` |
| `sl.file-upload.announce.removed` | `Removed {0}.` |
| `sl.file-upload.announce.rejected` | `Could not add {0}: {1}.` |
| `sl.file-upload.cancel`: Sending the files with htmx: the button that stops it, and each file's description while it goes. {0}: the percentage sent, formatted in the page's language ("64%"). | `Cancel upload` |
| `sl.file-upload.uploading` | `Uploading · {0}` |
| `sl.file-upload.uploading.indeterminate` | `Uploading` |
| `sl.file-upload.processing` | `Processing` |
| `sl.file-upload.failed` | `Upload failed` |
| `sl.file-upload.done` | `Uploaded` |
| `sl.file-upload.announce.uploading`: Announced when the upload starts and ends. {0}: the number of files, or for one file (.one) its name. | `Uploading {0} files.` |
| `sl.file-upload.announce.uploading.one` | `Uploading {0}.` |
| `sl.file-upload.announce.uploaded` | `Uploaded {0} files.` |
| `sl.file-upload.announce.uploaded.one` | `Uploaded {0}.` |
| `sl.file-upload.announce.failed` | `The files could not be uploaded. Try again.` |
| `sl.file-upload.announce.cancelled` | `Upload cancelled.` |

## Contributing a language

A new language is a `shadleaf/messages_<language>.properties` in the starter (`src/main/resources/shadleaf/`), with
every key of `messages.properties`. `ShadleafMessageBundlesTest` checks that each translation has exactly the English
keys, the same arguments, and its apostrophes written for whether the message has arguments. Pull requests are welcome.
