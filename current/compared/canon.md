# Shadleaf and Canon

> Canon is a new library of plain HTML and CSS components for internal tools, with swappable themes and no build.

Source: https://shadleaf.dev/current/compared/canon/

[Canon](https://recepkutuk.github.io/canon/reference/introduction.html) is a component library for internal tools
(tables, forms, dialogs and records) made of plain HTML with `canon-*` classes, one stylesheet and an optional script.
It is new, first released in October 2026 and at version 0.2.

The idea comes from CSS Zen Garden. The markup stays the same, and a new stylesheet gives it a new look. The markup
carries meaning (`data-*` and ARIA attributes) and never a look: no `primary`, `outline` or `rounded` in the classes.

## The same dialog

```html
<!-- Canon -->
<button type="button" class="canon-button" commandfor="edit-profile" command="show-modal">
  <span class="canon-button-label">Edit profile</span>
</button>
<dialog id="edit-profile" class="canon-dialog">
  <div class="canon-dialog-header">Edit profile</div>
  <div class="canon-dialog-body">...</div>
  <div class="canon-dialog-footer">...</div>
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
</sl:dialog>
```

Both open the browser's `<dialog>` with invoker commands, and both have a script that stands in for them (and for
`closedby`) where a browser lacks them.

## Where Canon is stronger

- **Any stack**, with no build: one stylesheet and one deferred script, from a CDN or npm.
- **Themes.** Its own, and themes inspired by Carbon, Material, Fluent, Ant Design and Bootstrap, swapped by changing
  the stylesheet only.
- **Without JavaScript.** It ranks its components by how much script they need, and all but one of its 68 work
  without.
- **Components for internal tools** Shadleaf has not: a form repeater, a save bar, a menubar, a split button and a time
  input.

## Where Shadleaf is stronger

- **Maturity.** Canon was first released in October 2026, by one author, and is before 1.0. Shadleaf's components
  are tested in every skin, in Chromium and Firefox.
- **Components instead of markup**, with declared props and one place to change. See
  [what a component engine adds](https://shadleaf.dev/current/compared.md#what-a-component-engine-adds).
- **Spring wiring.** Canon's data table, fields and forms have no binding to a back end; Shadleaf's sort and page a
  Spring Data `Page` and bind with `th:field`.
- **shadcn/ui's design and a chart.** Canon has a look of its own and no chart.

## Which one to choose

**Choose Canon** for an internal tool in any stack where you want plain HTML, no build, and a Material, Fluent or
Carbon look, and are fine with a young library. **Choose Shadleaf** for a Spring Boot application that wants
shadcn/ui's design and Thymeleaf components with Spring wired in.
