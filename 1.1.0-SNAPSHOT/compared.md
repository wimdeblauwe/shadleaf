# Compared with other libraries

> How Shadleaf differs from daisyUI, Flowbite, Basecoat, Flux, Vaadin and other UI libraries, and when another one is the better choice.

Source: https://shadleaf.dev/1.1.0-SNAPSHOT/compared/

Shadleaf is not the only way to get good-looking components into a server-rendered application. This section sets it
next to the libraries people most often ask about, one page each or one for two that are close, and says when one of
them is the better choice. The pages describe them as of November 2026; prices and versions change, so check their own
sites before you decide.

## At a glance

| Library | What you write | Runs on | Your build needs | Behaviour | Licence |
| --- | --- | --- | --- | --- | --- |
| **Shadleaf** | Thymeleaf tags: `<sl:button>` | Spring Boot 4 and Thymeleaf | nothing: the jar ships compiled CSS | Alpine.js and native elements | Apache 2.0 |
| [Vaadin](https://shadleaf.dev/1.1.0-SNAPSHOT/compared/vaadin.md) | Java code: `new Button(...)` | Spring Boot (Java 21) | nothing until you add frontend code or a theme; then Node.js, which Vaadin downloads | web components, kept in step with the server | Apache 2.0; commercial components by subscription |
| [Flux](https://shadleaf.dev/1.1.0-SNAPSHOT/compared/flux.md) | Blade tags: `<flux:button>` | Laravel and Livewire | Tailwind CSS 4.2+ | Livewire and Alpine.js | free core; Pro components paid once |
| [Basecoat](https://shadleaf.dev/1.1.0-SNAPSHOT/compared/basecoat-and-starting-point-ui.md) | classes on HTML; Jinja and Nunjucks macros | any | nothing with the CDN stylesheet; Tailwind otherwise | vanilla JS | MIT |
| [starting-point-ui](https://shadleaf.dev/1.1.0-SNAPSHOT/compared/basecoat-and-starting-point-ui.md) | classes on HTML | any | Tailwind CSS v4 | vanilla JS | MIT |
| [daisyUI](https://shadleaf.dev/1.1.0-SNAPSHOT/compared/daisyui.md) | classes on HTML | any | nothing with the CDN stylesheet; Tailwind otherwise | CSS and native elements; the rest is your JS | MIT; templates and charts sold separately |
| [Flowbite](https://shadleaf.dev/1.1.0-SNAPSHOT/compared/flowbite.md) | Tailwind utility classes and data attributes | any | Tailwind CSS (or the CDN build) | vanilla JS | MIT; Pro blocks and Figma kit paid |
| [Penguin UI](https://shadleaf.dev/1.1.0-SNAPSHOT/compared/penguin-ui-and-pines.md) | copied Tailwind and Alpine.js markup | any | Tailwind CSS v4 | Alpine.js, inline in the markup | MIT |
| [Pines](https://shadleaf.dev/1.1.0-SNAPSHOT/compared/penguin-ui-and-pines.md) | copied Tailwind and Alpine.js markup | any | Tailwind CSS | Alpine.js, inline in the markup | MIT |
| [Canon](https://shadleaf.dev/1.1.0-SNAPSHOT/compared/canon.md) | classes on HTML | any | nothing: one stylesheet | optional vanilla JS | MIT |

The libraries fall into four groups, and the comparison is a different one for each:

1. **Components for another stack** ([Flux](https://shadleaf.dev/1.1.0-SNAPSHOT/compared/flux.md)): the same idea as Shadleaf, for Laravel.
2. **Another architecture on the same stack** ([Vaadin](https://shadleaf.dev/1.1.0-SNAPSHOT/compared/vaadin.md)): Java and Spring, but the UI lives on the server
   as Java objects instead of HTML templates.
3. **shadcn/ui as plain HTML** ([Basecoat](https://shadleaf.dev/1.1.0-SNAPSHOT/compared/basecoat-and-starting-point-ui.md),
   [starting-point-ui](https://shadleaf.dev/1.1.0-SNAPSHOT/compared/basecoat-and-starting-point-ui.md)): the same design as Shadleaf, as CSS classes for any stack.
4. **Class and copy-paste libraries** ([daisyUI](https://shadleaf.dev/1.1.0-SNAPSHOT/compared/daisyui.md), [Flowbite](https://shadleaf.dev/1.1.0-SNAPSHOT/compared/flowbite.md),
   [Penguin UI](https://shadleaf.dev/1.1.0-SNAPSHOT/compared/penguin-ui-and-pines.md), [Pines](https://shadleaf.dev/1.1.0-SNAPSHOT/compared/penguin-ui-and-pines.md), [Canon](https://shadleaf.dev/1.1.0-SNAPSHOT/compared/canon.md)): markup you write or
   copy into your templates, with a look of their own.

## What a component engine adds

Every library in groups 3 and 4 works from Thymeleaf, because they are CSS and a script, and Thymeleaf writes HTML. The
difference is in what you write, and what you keep up to date. With a class library, a dialog is its markup, written
out in every template that has one:

```html
<!-- daisyUI -->
<button class="btn" onclick="edit_profile.showModal()">Edit profile</button>
<dialog id="edit_profile" class="modal">
  <div class="modal-box">
    <h3 class="text-lg font-bold">Edit profile</h3>
    <div class="modal-action">
      <form method="dialog"><button class="btn">Close</button></form>
    </div>
  </div>
</dialog>
```

With Shadleaf the markup is in the library's template, and you write the component:

```html
<sl:button commandfor="edit-profile" command="show-modal">Edit profile</sl:button>
<sl:dialog id="edit-profile">
  <sl:dialog-header>
    <sl:dialog-title>Edit profile</sl:dialog-title>
  </sl:dialog-header>
  ...
</sl:dialog>
```

That brings a few things no class library can:

- **One place to change.** A fix or a new version of a component's markup reaches every page that uses it. Copied
  markup stays as it was copied.
- **Declared props.** `variant="destructve"` fails the render with the legal values, and so does an icon-only button
  without an accessible name. A misspelt class does nothing, silently.
- **Spring wiring.** `th:field` on a field gives its label, control, error message, `aria-describedby` and
  `aria-invalid` in one go; the [table](https://shadleaf.dev/1.1.0-SNAPSHOT/components/table.md) sorts and pages a Spring Data `Page`; the
  [user menu](https://shadleaf.dev/1.1.0-SNAPSHOT/components/user-menu.md) reads Spring Security; every text is a message you can
  [translate](https://shadleaf.dev/1.1.0-SNAPSHOT/guides/translating.md). With a class library, you write all of that yourself.
- **No Tailwind build.** Most class libraries are Tailwind plugins, so your application compiles Tailwind. Shadleaf's
  jar ships the compiled CSS ([Tailwind CSS](https://shadleaf.dev/1.1.0-SNAPSHOT/guides/tailwind.md)).
- **IDE completion** of tags and props, through [web-types](https://shadleaf.dev/1.1.0-SNAPSHOT/ide-completion.md).

What you give up is the freedom of owning every line of markup, and a library that works the same in any language.
Shadleaf lets you take a template over when a prop is not enough (the
[adjustability ladder](https://shadleaf.dev/1.1.0-SNAPSHOT/guides/adjustability.md)), but it only runs in Spring Boot with Thymeleaf.
