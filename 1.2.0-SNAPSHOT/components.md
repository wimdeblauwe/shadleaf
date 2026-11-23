# Components

> Every Shadleaf component, in the groups of the sidebar, with one line on what each is for.

Source: https://shadleaf.dev/1.2.0-SNAPSHOT/components/

Every component is a Thymeleaf tag with the `sl:` prefix, rendered on the server. Each page shows examples, the
markup to write, and a table of the component's attributes. Whole pages built from them are in
[Layouts](../layouts/.

## Actions

- [Button](https://shadleaf.dev/1.2.0-SNAPSHOT/components/button.md): A button that submits a form, opens a dialog or runs an action, or a link that looks like one; with an icon, or icon-only for toolbars and table rows.
- [Theme Toggle](https://shadleaf.dev/1.2.0-SNAPSHOT/components/theme-toggle.md): Switches the page between the light and the dark theme, and remembers the choice in the browser; for the site header or the sidebar.

## Forms

- [Field](https://shadleaf.dev/1.2.0-SNAPSHOT/components/field.md): A label, a control, a description and the binding's error messages, wired together from one th:field; for every control of a form bound to an object.
- [Form](https://shadleaf.dev/1.2.0-SNAPSHOT/components/form.md): A form element that passes every attribute on, and validates its fields on the server as the user types, with htmx 4.
- [Form errors](https://shadleaf.dev/1.2.0-SNAPSHOT/components/form-errors.md): The errors that belong to the whole form rather than one field, or all of the form's errors, shown above the fields; for passwords that do not match or a request the server refused.
- [Label](https://shadleaf.dev/1.2.0-SNAPSHOT/components/label.md): A label for a form control; for a control outside a field, such as a checkbox or a switch with its text next to it.
- [Input](https://shadleaf.dev/1.2.0-SNAPSHOT/components/input.md): A text input, bound to a form field with th:field; for names, email addresses, numbers, passwords and other one-line values.
- [Input Group](https://shadleaf.dev/1.2.0-SNAPSHOT/components/input-group.md): Text, icons and buttons inside the border of an input or textarea; for a search field with an icon, an amount with its currency or a message box with a Send button.
- [Textarea](https://shadleaf.dev/1.2.0-SNAPSHOT/components/textarea.md): A multi-line text input, bound to a form field with th:field; for comments, messages, descriptions and notes.
- [Select](https://shadleaf.dev/1.2.0-SNAPSHOT/components/select.md): A select drawn in the page's style on a real select element, bound with th:field; for one value out of a short list.
- [Select (Native)](https://shadleaf.dev/1.2.0-SNAPSHOT/components/select-native.md): The browser's own select, styled, bound to a form field with th:field; for long lists, dense forms and several values, with the phone's own picker.
- [Combobox](https://shadleaf.dev/1.2.0-SNAPSHOT/components/combobox.md): A select the user types into to filter, bound with th:field; one value or several as chips, options from the server as the user types; for long lists such as customers, countries or tags.
- [Checkbox](https://shadleaf.dev/1.2.0-SNAPSHOT/components/checkbox.md): The browser's own checkbox, styled, bound to a form field with th:field; for accepting terms, a yes or no in a form, or picking several options.
- [Radio Group](https://shadleaf.dev/1.2.0-SNAPSHOT/components/radio-group.md): A set of the browser's own radio buttons, styled, bound to a form field with th:field; for one choice out of a few options that are all visible.
- [Switch](https://shadleaf.dev/1.2.0-SNAPSHOT/components/switch.md): An on/off control, bound to a boolean form field with th:field; for settings that are on or off, such as email notifications.
- [Toggle](https://shadleaf.dev/1.2.0-SNAPSHOT/components/toggle.md): A button that is on or off, as a checkbox drawn as a button, bound to a boolean form field with th:field; for Bold in a toolbar, or marking a favourite.
- [Toggle Group](https://shadleaf.dev/1.2.0-SNAPSHOT/components/toggle-group.md): A set of toggles of which one or several can be on, as radio buttons or checkboxes bound with th:field; for text alignment, formatting or switching a view.
- [Calendar](https://shadleaf.dev/1.2.0-SNAPSHOT/components/calendar.md): A month to pick from, drawn over native date inputs, bound with th:field; a date or a range of dates, always visible in the page.
- [Date Picker](https://shadleaf.dev/1.2.0-SNAPSHOT/components/date-picker.md): A button showing the chosen date that opens a calendar in a popover; a date or a range, bound with th:field; for the date fields of forms and filters.
- [File Upload](https://shadleaf.dev/1.2.0-SNAPSHOT/components/file-upload.md): A drop zone with a Browse button and the list of the files picked, around a file input bound with th:field; for uploading documents, photos or a file to import.

## Overlays

- [Dialog](https://shadleaf.dev/1.2.0-SNAPSHOT/components/dialog.md): A modal window over the page, on the native dialog element; for a short form, details or a step the user finishes before going back to the page.
- [Alert Dialog](https://shadleaf.dev/1.2.0-SNAPSHOT/components/alert-dialog.md): A modal dialog that interrupts the user with something important and waits for an answer; for confirming a delete or another action that cannot be undone.
- [Sheet](https://shadleaf.dev/1.2.0-SNAPSHOT/components/sheet.md): A dialog that slides in from an edge of the screen; for filters, settings or the details of a row beside the page.
- [Popover](https://shadleaf.dev/1.2.0-SNAPSHOT/components/popover.md): Rich content in a panel next to the button that opens it, on the browser's Popover API; for a small inline form, details with links, or a share panel.
- [Dropdown Menu](https://shadleaf.dev/1.2.0-SNAPSHOT/components/dropdown-menu.md): A menu of actions or links that opens from a button, on the browser's Popover API; for the actions of a table row, a More button or an account menu.
- [Tooltip](https://shadleaf.dev/1.2.0-SNAPSHOT/components/tooltip.md): A short text that shows while the pointer rests on an element or the keyboard focuses it; for the name of an icon button or a keyboard shortcut.

## Feedback

- [Alert](https://shadleaf.dev/1.2.0-SNAPSHOT/components/alert.md): A callout for a message the user should notice; for an error on the page, a warning or a notice about the account.
- [Toast](https://shadleaf.dev/1.2.0-SNAPSHOT/components/toast.md): A short message in a corner of the page that closes itself, sent by the server; for confirming an action, such as a saved form or a deleted row.
- [Skeleton](https://shadleaf.dev/1.2.0-SNAPSHOT/components/skeleton.md): A placeholder that pulses while content loads; for the cards, rows and text that htmx loads after the page.
- [Spinner](https://shadleaf.dev/1.2.0-SNAPSHOT/components/spinner.md): A spinning icon that says something is loading; for a busy button or a part of the page that waits for the server.
- [Empty](https://shadleaf.dev/1.2.0-SNAPSHOT/components/empty.md): What a list, a table, a search or a page shows when there is nothing to show yet; with an icon, a title, a text and a next step.

## Data

- [Table](https://shadleaf.dev/1.2.0-SNAPSHOT/components/table.md): A styled table, whose column headers sort it on the server; for lists of records with columns, search, actions per row and selection.
- [Pagination](https://shadleaf.dev/1.2.0-SNAPSHOT/components/pagination.md): Links to the other pages of a list, built from the Spring Data Page the controller returns; for tables and other long lists.
- [Query params](https://shadleaf.dev/1.2.0-SNAPSHOT/components/query-params.md): Hidden inputs that keep a table's order, page size and filters when a search form is sent; for a search above a sorted or paged table.
- [Month view](https://shadleaf.dev/1.2.0-SNAPSHOT/components/month-view.md): A month rendered on the server, with content of your own per day and a link per day; for an agenda, a booking overview or orders per day.
- [Chart](https://shadleaf.dev/1.2.0-SNAPSHOT/components/chart.md): Bar, line and area charts drawn by Recharts from rows the controller puts in the model; for dashboards, reports and figures over time.

## Application shell

Start with the [overview](https://shadleaf.dev/1.2.0-SNAPSHOT/components/application-shell.md): Choosing between a sidebar and a header for the frame around every page, and what every layout needs whichever you pick.

- [Sidebar](https://shadleaf.dev/1.2.0-SNAPSHOT/components/sidebar.md): The application's navigation beside the page, and a panel over it on a phone; for admin panels, dashboards and other applications with many pages.
- [Site Header](https://shadleaf.dev/1.2.0-SNAPSHOT/components/site-header.md): A header bar across the top of the page, or at the top of the sidebar layout's page; for the logo, the main links, a search and the user menu.
- [User Menu](https://shadleaf.dev/1.2.0-SNAPSHOT/components/user-menu.md): The signed-in user with a menu that signs out, or a Sign in link; for the sidebar's footer or the site header.
- [Breadcrumb](https://shadleaf.dev/1.2.0-SNAPSHOT/components/breadcrumb.md): The trail of pages above the one shown now, with a menu for the levels a long trail leaves out; for the top of a page deep in a hierarchy.
- [Skip Link](https://shadleaf.dev/1.2.0-SNAPSHOT/components/skip-link.md): A link past the navigation to the page's content, for keyboard users; first in the body of every layout.

## Disclosure

- [Tabs](https://shadleaf.dev/1.2.0-SNAPSHOT/components/tabs.md): Panels of content shown one at a time, chosen from a row of tabs; for the views of one record, such as Overview, Activity and Settings.
- [Accordion](https://shadleaf.dev/1.2.0-SNAPSHOT/components/accordion.md): A stack of sections, each with a heading that shows or hides its content, on the browser's details element; for an FAQ, grouped settings or details most readers skip.
- [Collapsible](https://shadleaf.dev/1.2.0-SNAPSHOT/components/collapsible.md): Content that a button shows and hides, on the browser's details element; for advanced options, extra details or a Show more.

## Display

- [Card](https://shadleaf.dev/1.2.0-SNAPSHOT/components/card.md): A bordered box for related content and actions, built from its parts; for a sign-in form, a dashboard tile or a section of a settings page.
- [Item](https://shadleaf.dev/1.2.0-SNAPSHOT/components/item.md): A row of content with media, a title, a description and actions, alone or in a list; for people, files, notifications or settings.
- [Attachment](https://shadleaf.dev/1.2.0-SNAPSHOT/components/attachment.md): A file or an image attached to a message, a form or a record, with its name, its size or state, and actions; for the documents on a record or the files of an upload.
- [Badge](https://shadleaf.dev/1.2.0-SNAPSHOT/components/badge.md): A small label for a status, a count or a category; for the state of an order, unread messages or a tag.
- [Avatar](https://shadleaf.dev/1.2.0-SNAPSHOT/components/avatar.md): A picture of a person, with initials when there is no picture; for the signed-in user, the author of a comment or the members of a team.
- [Icon](https://shadleaf.dev/1.2.0-SNAPSHOT/components/icon.md): Inline SVG icons from lucide, or your own; for buttons, menu items, badges, alerts and anywhere a symbol helps the text.
- [Kbd](https://shadleaf.dev/1.2.0-SNAPSHOT/components/kbd.md): A key on the keyboard, or a combination of keys; for the shortcuts in menus, tooltips and buttons, and in help text.
- [Separator](https://shadleaf.dev/1.2.0-SNAPSHOT/components/separator.md): A thin line between content; for dividing the sections of a page, a card or a toolbar.
