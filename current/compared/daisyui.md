# Shadleaf and daisyUI

> daisyUI is a Tailwind plugin of component classes, with many themes, for any stack.

Source: https://shadleaf.dev/current/compared/daisyui/

[daisyUI](https://daisyui.com) is a Tailwind CSS plugin that adds component classes (`btn btn-primary`) to plain HTML.
It is by far the most used library on these pages, at version 5 (November 2026), and has a look and colour names of
its own rather than shadcn/ui's.

## The same dialog

```html
<!-- daisyUI -->
<button class="btn" onclick="edit_profile.showModal()">Edit profile</button>
<dialog id="edit_profile" class="modal">
  <div class="modal-box">
    <h3 class="text-lg font-bold">Edit profile</h3>
    ...
    <div class="modal-action">
      <form method="dialog"><button class="btn">Close</button></form>
    </div>
  </div>
</dialog>
```

```html
<!-- Shadleaf -->
<sl:button commandfor="edit-profile" command="show-modal">Edit profile</sl:button>
<sl:dialog id="edit-profile">
  <sl:dialog-header>
    <sl:dialog-title>Edit profile</sl:dialog-title>
  </sl:dialog-header>
  ...
  <sl:dialog-footer show-close-button/>
</sl:dialog>
```

daisyUI's markup is short, and its modal is the native `<dialog>`, opened with your own script.

## Where daisyUI is stronger

- **Any stack**, and a huge community: tutorials, templates and answers for every question.
- **Themes.** 35 built-in themes and an online theme generator.
- **No build needed.** It loads from a CDN without Tailwind in your build; as a plugin it mixes with every Tailwind
  utility.
- **Many small pieces**: a timeline, a dock, a rating, a countdown, chat bubbles, device mockups.
- **Little to learn.** It is CSS: there is no script and no component engine to understand.

## Where Shadleaf is stronger

- **Behaviour.** daisyUI is CSS and native elements only. A modal opens from your script, a toast is a positioned
  container you fill yourself, and its legacy checkbox and anchor modals do not close on Escape. Shadleaf's components
  bring their keyboard handling, focus management and htmx support.
- **More components.** daisyUI has no combobox, no date picker (its calendar styles third-party calendar libraries)
  and no data table; its charts are a paid add-on.
- **Components instead of markup**, with declared props and one place to change. See
  [what a component engine adds](https://shadleaf.dev/current/compared.md#what-a-component-engine-adds).
- **Spring wiring.** In daisyUI you apply `input-error` and write the error message yourself; Shadleaf does it from
  `th:field`.
- **Tested accessibility.** axe runs on every Shadleaf example in every skin; daisyUI publishes no accessibility
  tests.

## Which one to choose

**Choose daisyUI** for a quick, themeable look in any stack, for pages that are mostly content, or when your
application already compiles Tailwind and you want its utilities everywhere. **Choose Shadleaf** for a Spring Boot
application with forms, tables and dialogs that should behave the same everywhere, without writing that behaviour
yourself.
