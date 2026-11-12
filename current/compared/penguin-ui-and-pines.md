# Shadleaf, Penguin UI and Pines

> Penguin UI and Pines are collections of Tailwind CSS and Alpine.js snippets to copy into your templates.

Source: https://shadleaf.dev/current/compared/penguin-ui-and-pines/

[Penguin UI](https://www.penguinui.com) and DevDojo's [Pines](https://devdojo.com/pines) are both free (MIT)
collections of markup built with Tailwind CSS and Alpine.js. There is nothing to install beyond those two. You copy an
element into your template and change it as you like. The behaviour sits in the markup as Alpine.js expressions,
where you can read and change it.

|  | Penguin UI | Pines |
| --- | --- | --- |
| Theming | a theme designer with presets (Arctic, Minimal, Neo Brutalism, Pastel and more), dark mode included | none: a colour change is a change of classes in every snippet |
| Beyond components | components for AI chat interfaces | marketing sections: heroes, pricing tables, footers |

## The same dialog

```html
<!-- Penguin UI, utility classes shortened -->
<div x-data="{modalIsOpen: false}">
  <button x-on:click="modalIsOpen = true" type="button" class="...">Edit profile</button>
  <div x-cloak x-show="modalIsOpen" x-trap.inert.noscroll="modalIsOpen"
       x-on:keydown.esc.window="modalIsOpen = false" x-on:click.self="modalIsOpen = false"
       class="fixed inset-0 z-30 ..." role="dialog" aria-modal="true" aria-labelledby="edit-profile-title">
    <div x-show="modalIsOpen" class="...">
      <h3 id="edit-profile-title" class="...">Edit profile</h3>
      ...
      <button x-on:click="modalIsOpen = false" type="button" class="...">Save</button>
    </div>
  </div>
</div>
```

```html
<!-- Pines, utility classes shortened -->
<div x-data="{ modalOpen: false }" @keydown.escape.window="modalOpen = false">
  <button @click="modalOpen=true" class="...">Edit profile</button>
  <template x-teleport="body">
    <div x-show="modalOpen" class="fixed top-0 left-0 z-[99] ...">
      <div x-show="modalOpen" @click="modalOpen=false" class="absolute inset-0 bg-black/40"></div>
      <div x-show="modalOpen" x-trap.inert.noscroll="modalOpen" class="relative ...">
        <h3 class="text-lg font-semibold">Edit profile</h3>
        ...
      </div>
    </div>
  </template>
</div>
```

```html
<!-- Shadleaf -->
<sl:button commandfor="edit-profile" command="show-modal">Edit profile</sl:button>
<sl:dialog id="edit-profile">
  <sl:dialog-header>
    <sl:dialog-title>Edit profile</sl:dialog-title>
  </sl:dialog-header>
  ...
  <sl:dialog-footer>
    <sl:button type="submit">Save</sl:button>
  </sl:dialog-footer>
</sl:dialog>
```

## Where Penguin UI and Pines are stronger

- **You own the markup.** No dependency to upgrade and no engine to learn.
- **Any stack** that can load Tailwind and Alpine.js, Spring included.
- **Elements Shadleaf has not.** Penguin UI has a carousel, chat bubbles, a rating, a counter and steps; Pines has a
  Monaco editor, an image gallery, a marquee, text and typing effects and a retro grid.
- **Pines' marketing sections**, ready to paste, and a look close to shadcn/ui's: neutral greys, small radii, a focus
  ring.

## Where Shadleaf is stronger

- **No Tailwind build.** Both need Tailwind CSS in your build; Shadleaf's jar ships its CSS.
- **Without JavaScript.** Their modals, dropdowns and tabs keep their state in Alpine.js and stay hidden without it.
  Shadleaf's are native elements first (`<dialog>`, popovers, `<details>`) that work with Alpine.js missing.
- **More components for applications.** Neither has a date picker or a chart, and Penguin UI's table is styling
  only; Pines also has no combobox, sidebar, application layout, or form field with validation.
- **Activity.** Pines has had no release, and its last change was in August 2025.
- **Components instead of markup**, with declared props and one place to change. See
  [what a component engine adds](https://shadleaf.dev/current/compared.md#what-a-component-engine-adds).
- **Spring wiring, a documented strict CSP, and tested accessibility**: `th:field` binding and errors, Spring Data
  paging, a [CSP guide](https://shadleaf.dev/current/guides/csp.md), axe on every example.

## Which one to choose

**Choose Penguin UI** if you already compile Tailwind and use Alpine.js, want a look of your own, and are happy to
own and maintain the markup. **Choose Pines** for a marketing page, or for single elements in such a project.
**Choose Shadleaf** for a Spring Boot application with forms, tables and navigation, and the Spring wiring done.
