# Components

> Every Shadleaf component, in the groups of the sidebar, with one line on what each is for.

Source: https://shadleaf.dev/current/components/

Every component is a Thymeleaf tag with the `sl:` prefix, rendered on the server. Each page shows examples, the
markup to write, and a table of the component's attributes. Whole pages built from them are in
[Layouts](../layouts/.

## Actions

- [Button](https://shadleaf.dev/current/components/button.md): A button, or a link that looks like one.
- [Theme Toggle](https://shadleaf.dev/current/components/theme-toggle.md): Switches the page between the light and the dark theme, and remembers the choice in the browser.

## Forms

- [Field](https://shadleaf.dev/current/components/field.md): A label, a control, a description and the binding's error messages, wired together from one th:field.
- [Form errors](https://shadleaf.dev/current/components/form-errors.md): The errors that belong to the whole form rather than one field, or all of the form's errors, read from the binding and shown above the fields.
- [Label](https://shadleaf.dev/current/components/label.md): A label for a form control.
- [Input](https://shadleaf.dev/current/components/input.md): A text input, bound to a form field with th:field.
- [Input Group](https://shadleaf.dev/current/components/input-group.md): Text, icons and buttons inside the border of an input or textarea.
- [Textarea](https://shadleaf.dev/current/components/textarea.md): A multi-line text input, bound to a form field with th:field.
- [Select](https://shadleaf.dev/current/components/select.md): A select drawn in the page's style on a real select element, bound with th:field; the browser's customizable select where it has one.
- [Select (Native)](https://shadleaf.dev/current/components/select-native.md): The browser's own select, styled, bound to a form field with th:field.
- [Combobox](https://shadleaf.dev/current/components/combobox.md): A searchable select on a real select element, bound with th:field; type to filter, choose one value or several as chips, and load the options from the server as you type.
- [Checkbox](https://shadleaf.dev/current/components/checkbox.md): The browser's own checkbox, styled, bound to a form field with th:field.
- [Radio Group](https://shadleaf.dev/current/components/radio-group.md): A set of the browser's own radio buttons, styled, bound to a form field with th:field.
- [Switch](https://shadleaf.dev/current/components/switch.md): An on/off control, bound to a boolean form field with th:field.
- [Toggle](https://shadleaf.dev/current/components/toggle.md): A button that is on or off, as a checkbox drawn as a button, bound to a boolean form field with th:field.
- [Toggle Group](https://shadleaf.dev/current/components/toggle-group.md): A set of toggles of which one or several can be on, as radio buttons or checkboxes bound with th:field.
- [Calendar](https://shadleaf.dev/current/components/calendar.md): A month to pick from, drawn over native date inputs, bound with th:field; a date or a range of dates.
- [Date Picker](https://shadleaf.dev/current/components/date-picker.md): A button showing the chosen date that opens a calendar in a popover; a date or a range, bound with th:field.
- [File Upload](https://shadleaf.dev/current/components/file-upload.md): A drop zone with a Browse button and the list of the files picked, around a file input bound with th:field.

## Overlays

- [Dialog](https://shadleaf.dev/current/components/dialog.md): A modal window over the page, on the native dialog element.
- [Alert Dialog](https://shadleaf.dev/current/components/alert-dialog.md): A modal dialog that interrupts the user with something important and waits for an answer.
- [Sheet](https://shadleaf.dev/current/components/sheet.md): A dialog that slides in from an edge of the screen.
- [Popover](https://shadleaf.dev/current/components/popover.md): Rich content in a panel next to the button that opens it, on the browser's Popover API.
- [Dropdown Menu](https://shadleaf.dev/current/components/dropdown-menu.md): A menu of actions or links that opens from a button, on the browser's Popover API.
- [Tooltip](https://shadleaf.dev/current/components/tooltip.md): A short text that shows while the pointer rests on an element or the keyboard focuses it.

## Feedback

- [Alert](https://shadleaf.dev/current/components/alert.md): A callout for a message the user should notice.
- [Toast](https://shadleaf.dev/current/components/toast.md): A short message in a corner of the page that closes itself, sent by the server.
- [Skeleton](https://shadleaf.dev/current/components/skeleton.md): A placeholder that pulses while content loads.
- [Spinner](https://shadleaf.dev/current/components/spinner.md): A spinning icon that says something is loading.
- [Empty](https://shadleaf.dev/current/components/empty.md): What a list, a table, a search or a page shows when there is nothing to show yet.

## Data

- [Table](https://shadleaf.dev/current/components/table.md): A styled table, whose column headers sort it on the server.
- [Pagination](https://shadleaf.dev/current/components/pagination.md): Links to the other pages of a list, built from the Spring Data Page the controller returns.
- [Query params](https://shadleaf.dev/current/components/query-params.md): Hidden inputs that keep a table's order, page size and filters when a search form is sent.
- [Month view](https://shadleaf.dev/current/components/month-view.md): A month rendered on the server, with content of your own per day and a link per day; no picking.
- [Chart](https://shadleaf.dev/current/components/chart.md): Recharts drawing bars, lines and areas from rows the controller puts in the model; labels and colours on the series, named Intl formats.

## Application shell

Start with the [overview](https://shadleaf.dev/current/components/application-shell.md): Choosing between a sidebar and a header for the frame around every page, and what every layout needs whichever you pick.

- [Sidebar](https://shadleaf.dev/current/components/sidebar.md): The application's navigation beside the page, and a panel over it on a phone.
- [Site Header](https://shadleaf.dev/current/components/site-header.md): A header bar across the top of the page, or at the top of the sidebar layout's page.
- [User Menu](https://shadleaf.dev/current/components/user-menu.md): The signed-in user with a menu that signs out, or a Sign in link, for the sidebar's footer or the site header.
- [Breadcrumb](https://shadleaf.dev/current/components/breadcrumb.md): The trail of pages above the one shown now, with a menu for the levels a long trail leaves out.
- [Skip Link](https://shadleaf.dev/current/components/skip-link.md): A link past the navigation to the page's content, for keyboard users.

## Disclosure

- [Tabs](https://shadleaf.dev/current/components/tabs.md): Panels of content shown one at a time, chosen from a row of tabs.
- [Accordion](https://shadleaf.dev/current/components/accordion.md): A stack of sections, each with a heading that shows or hides its content, on the browser's details element.
- [Collapsible](https://shadleaf.dev/current/components/collapsible.md): Content that a button shows and hides, on the browser's details element.

## Display

- [Card](https://shadleaf.dev/current/components/card.md): A container for related content and actions, built from its parts.
- [Attachment](https://shadleaf.dev/current/components/attachment.md): A file or an image attached to a message, a form or a record, with its name, its size or state, and actions.
- [Badge](https://shadleaf.dev/current/components/badge.md): A small label for a status, a count or a category.
- [Avatar](https://shadleaf.dev/current/components/avatar.md): A picture of a person, with initials when there is no picture.
- [Icon](https://shadleaf.dev/current/components/icon.md): Inline SVG icons from lucide, or your own.
- [Kbd](https://shadleaf.dev/current/components/kbd.md): A key on the keyboard, or a combination of keys.
- [Separator](https://shadleaf.dev/current/components/separator.md): A thin line between content.
