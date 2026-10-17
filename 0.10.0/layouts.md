# Layouts

> Whole layout files to copy into an application, each with a page that uses it and the CSS around it, after shadcn/ui's sidebar blocks.

Source: https://shadleaf.dev/0.10.0/layouts/

A layout is the frame every page of an application shares: the navigation, the page's header, the skip link, and the
places where dialogs and toasts appear. In Thymeleaf it is one template, `templates/layout/main.html`, whose fragment
every page fills in with its title and content. The layouts here are built from Shadleaf's
[application shell](https://shadleaf.dev/0.10.0/components/application-shell.md) components, most of them after one of
[shadcn/ui's sidebar blocks](https://ui.shadcn.com/blocks/sidebar), and each one has what
[every layout needs](https://shadleaf.dev/0.10.0/components/application-shell.md#what-every-layout-needs).

## What a layout page holds

- **A preview**, at a desktop's width and, with the Phone switch, at a phone's. The navigation is one element for
  both: beside the page (or in the header) from 768 px, a panel the menu button opens below that. The preview works:
  open the menus, collapse the sidebar, open the panel.
- **The layout**: the whole layout file, head included, ready to copy.
- **A page**: a page that uses it. It passes its title, its content and, where the layout has a breadcrumb, the
  breadcrumb's levels, as fragments of itself (`th:ref`), so nothing is rendered where it stands.
- **The CSS**: what the layout needs of the application's own CSS. Shadleaf's CSS styles the components, not the page
  around them, and has no utility classes, so the page's padding, a logo's square or a border below the header are
  yours. It is plain CSS, without Tailwind, on Shadleaf's colour tokens, so it follows the skin and the dark theme.

## Using one

Copy the layout file to `src/main/resources/templates/layout/main.html` and its CSS into your stylesheet, then
replace the example navigation with your own pages. Mark the current page with `#slNav.current`, which reads the
request, so no controller has to tell the layout which page it is. The template is yours from then on: Shadleaf
does not update it, and nothing in it is configuration. Which kind of layout suits an application, a sidebar or a
header, is on the [Application shell](https://shadleaf.dev/0.10.0/components/application-shell.md#which-one) page, with the htmx check a
controller needs when the layout boosts its links.

The first sample application uses a shorter [Collapses to icons](https://shadleaf.dev/0.10.0/layouts/collapses-to-icons.md), the second
[Navigation in the header](https://shadleaf.dev/0.10.0/layouts/header.md).

## The layouts

- [Grouped by section](https://shadleaf.dev/0.10.0/layouts/grouped-by-section.md): A sidebar of sections, each a group of pages under its label, with a version switcher and a search field, as documentation has.
- [Collapsible sections](https://shadleaf.dev/0.10.0/layouts/collapsible-sections.md): A sidebar of sections whose labels open and close them, with a version switcher and a search field, as documentation has.
- [Sub-menus](https://shadleaf.dev/0.10.0/layouts/sub-menus.md): A sidebar of sections with their pages in sub-menus that are always open, as documentation has.
- [Floating, with sub-menus](https://shadleaf.dev/0.10.0/layouts/floating-sub-menus.md): The sections and sub-menus of the Sub-menus layout in a floating sidebar, wider and with more room between the sections.
- [Collapsible sub-menus](https://shadleaf.dev/0.10.0/layouts/collapsible-sub-menus.md): A sidebar of sections that open and close, each with its pages in a sub-menu, only the current page's section open.
- [Collapses to icons](https://shadleaf.dev/0.10.0/layouts/collapses-to-icons.md): A team switcher, collapsible sections and projects beside the page, collapsing to a strip of icons on a desktop, with the page as a card and the signed-in user's menu at the bottom.
- [Inset with secondary navigation](https://shadleaf.dev/0.10.0/layouts/inset-secondary-navigation.md): Sections that are pages of their own with a chevron for their sub-menus, projects, a secondary navigation and the user menu, beside the page as a card.
- [On the end](https://shadleaf.dev/0.10.0/layouts/sidebar-end.md): A table of contents in a sidebar on the end of the line, the right in a left-to-right page, with the trigger at the end of the page's header.
- [Navigation in the header](https://shadleaf.dev/0.10.0/layouts/header.md): For an application with a handful of pages, the pages as a row of links in the site header and the page as a card below it; on a phone the same navigation is a panel.
