# Components

> Every Shadleaf component, in the groups of the sidebar, with one line on what each is for.

Source: https://shadleaf.dev/0.10.0/components/

Every component is a Thymeleaf tag with the `sl:` prefix, rendered on the server. Each page shows examples, the
markup to write, and a table of the component's attributes. Whole pages built from them are in
[Layouts](../layouts/.

## Actions

- [Button](https://shadleaf.dev/0.10.0/components/button.md): A button, or a link that looks like one.
- [Theme Toggle](https://shadleaf.dev/0.10.0/components/theme-toggle.md): Switches the page between the light and the dark theme, and remembers the choice in the browser.

## Forms

- [Field](https://shadleaf.dev/0.10.0/components/field.md): A label, a control, a description and the binding's error messages, wired together from one th:field.
- [Form errors](https://shadleaf.dev/0.10.0/components/form-errors.md): The errors that belong to the whole form rather than one field, or all of the form's errors, read from the binding and shown above the fields.
- [Label](https://shadleaf.dev/0.10.0/components/label.md): A label for a form control.
- [Input](https://shadleaf.dev/0.10.0/components/input.md): A text input, bound to a form field with th:field.
- [Textarea](https://shadleaf.dev/0.10.0/components/textarea.md): A multi-line text input, bound to a form field with th:field.
- [Select](https://shadleaf.dev/0.10.0/components/select.md): shadcn/ui's Select on a real select element, bound with th:field; the browser's customizable select where it has one.
- [Select (Native)](https://shadleaf.dev/0.10.0/components/select-native.md): The browser's own select, styled, bound to a form field with th:field.
- [Checkbox](https://shadleaf.dev/0.10.0/components/checkbox.md): The browser's own checkbox, styled, bound to a form field with th:field.
- [Radio Group](https://shadleaf.dev/0.10.0/components/radio-group.md): A set of the browser's own radio buttons, styled, bound to a form field with th:field.
- [Switch](https://shadleaf.dev/0.10.0/components/switch.md): An on/off control, bound to a boolean form field with th:field.

## Overlays

- [Dialog](https://shadleaf.dev/0.10.0/components/dialog.md): A modal window over the page, on the native dialog element.
- [Alert Dialog](https://shadleaf.dev/0.10.0/components/alert-dialog.md): A modal dialog that interrupts the user with something important and waits for an answer.
- [Sheet](https://shadleaf.dev/0.10.0/components/sheet.md): A dialog that slides in from an edge of the screen.
- [Popover](https://shadleaf.dev/0.10.0/components/popover.md): Rich content in a panel next to the button that opens it, on the browser's Popover API.
- [Dropdown Menu](https://shadleaf.dev/0.10.0/components/dropdown-menu.md): A menu of actions or links that opens from a button, on the browser's Popover API.
- [Tooltip](https://shadleaf.dev/0.10.0/components/tooltip.md): A short text that shows while the pointer rests on an element or the keyboard focuses it.

## Feedback

- [Alert](https://shadleaf.dev/0.10.0/components/alert.md): A callout for a message the user should notice.
- [Toast](https://shadleaf.dev/0.10.0/components/toast.md): A short message in a corner of the page that closes itself, sent by the server; shadcn/ui's Sonner.
- [Skeleton](https://shadleaf.dev/0.10.0/components/skeleton.md): A placeholder that pulses while content loads.

## Data

- [Table](https://shadleaf.dev/0.10.0/components/table.md): A table in shadcn/ui's look, whose column headers sort it on the server.
- [Pagination](https://shadleaf.dev/0.10.0/components/pagination.md): Links to the other pages of a list, built from the Spring Data Page the controller returns.
- [Query params](https://shadleaf.dev/0.10.0/components/query-params.md): Hidden inputs that keep a table's order, page size and filters when a search form is sent.

## Application shell

Start with the [overview](https://shadleaf.dev/0.10.0/components/application-shell.md): Choosing between a sidebar and a header for the frame around every page, and what every layout needs whichever you pick.

- [Sidebar](https://shadleaf.dev/0.10.0/components/sidebar.md): The application's navigation beside the page, and a panel over it on a phone.
- [Site Header](https://shadleaf.dev/0.10.0/components/site-header.md): A header bar across the top of the page, or at the top of the sidebar layout's page.
- [User Menu](https://shadleaf.dev/0.10.0/components/user-menu.md): The signed-in user with a menu that signs out, or a Sign in link, for the sidebar's footer or the site header.
- [Breadcrumb](https://shadleaf.dev/0.10.0/components/breadcrumb.md): The trail of pages above the one shown now, with a menu for the levels a long trail leaves out.
- [Skip Link](https://shadleaf.dev/0.10.0/components/skip-link.md): A link past the navigation to the page's content, for keyboard users.

## Disclosure

- [Tabs](https://shadleaf.dev/0.10.0/components/tabs.md): Panels of content shown one at a time, chosen from a row of tabs.
- [Accordion](https://shadleaf.dev/0.10.0/components/accordion.md): A stack of sections, each with a heading that shows or hides its content, on the browser's details element.
- [Collapsible](https://shadleaf.dev/0.10.0/components/collapsible.md): Content that a button shows and hides, on the browser's details element.

## Display

- [Card](https://shadleaf.dev/0.10.0/components/card.md): A container for related content and actions, built from its parts.
- [Badge](https://shadleaf.dev/0.10.0/components/badge.md): A small label for a status, a count or a category.
- [Avatar](https://shadleaf.dev/0.10.0/components/avatar.md): A picture of a person, with initials when there is no picture.
- [Icon](https://shadleaf.dev/0.10.0/components/icon.md): Inline SVG icons from lucide, or your own.
- [Kbd](https://shadleaf.dev/0.10.0/components/kbd.md): A key on the keyboard, or a combination of keys.
- [Separator](https://shadleaf.dev/0.10.0/components/separator.md): A thin line between content.
