# Shadleaf, Basecoat and starting-point-ui

> Basecoat and starting-point-ui bring shadcn/ui's design to any stack as plain HTML and CSS classes. The look is the same as Shadleaf's; the components are not there.

Source: https://shadleaf.dev/1.0.3/compared/basecoat-and-starting-point-ui/

[Basecoat](https://basecoatui.com) and [starting-point-ui](https://startingpointui.com) do the same thing. They bring
shadcn/ui to any stack that writes HTML, as CSS classes on native elements with a small vanilla script, and no React.
Shadleaf owes both a lot. Its skins follow Basecoat's style files, and starting-point-ui's CSS was a reference for its
components.

|  | Basecoat | starting-point-ui |
| --- | --- | --- |
| Version (November 2026) | 1.1 | 0.41, before 1.0 |
| Your build needs | nothing with the CDN stylesheet; Tailwind otherwise | Tailwind CSS v4 |
| shadcn/ui styles | all eight | the default look; any shadcn/ui theme pasted in |
| Templates | Jinja and Nunjucks macros for nine components | none |
| Strict CSP | blocked by the inline `onclick` in its dialog markup, dialog macro and toast actions | fine; only its experimental event attributes need `'unsafe-eval'` |

## The same dialog

```html
<!-- Basecoat -->
<button type="button" class="btn" data-variant="outline"
        onclick="document.getElementById('edit-profile').showModal()">Edit profile</button>
<dialog id="edit-profile" class="dialog" aria-labelledby="edit-profile-title"
        onclick="if (event.target === this) this.close()">
  <div class="sm:max-w-sm">
    <header>
      <h2 id="edit-profile-title">Edit profile</h2>
    </header>
    <section>...</section>
    <footer>
      <button type="button" class="btn" onclick="this.closest('dialog').close()">Save</button>
    </footer>
  </div>
</dialog>
```

```html
<!-- starting-point-ui -->
<button id="edit-profile-trigger" class="btn btn-outline">Edit profile</button>
<dialog class="dialog" data-sp-toggle="#edit-profile-trigger">
  <form class="dialog-panel">
    <div class="dialog-header">
      <h2 class="dialog-title">Edit profile</h2>
    </div>
    ...
    <div class="dialog-footer">
      <button type="button" class="btn btn-outline" data-sp-dismiss>Cancel</button>
      <button type="submit" class="btn">Save</button>
    </div>
  </form>
</dialog>
```

```html
<!-- Shadleaf -->
<sl:button variant="outline" commandfor="edit-profile" command="show-modal">Edit profile</sl:button>
<sl:dialog id="edit-profile">
  <sl:dialog-header>
    <sl:dialog-title>Edit profile</sl:dialog-title>
  </sl:dialog-header>
  ...
  <sl:dialog-footer>
    <sl:button variant="outline" commandfor="edit-profile" command="close">Cancel</sl:button>
    <sl:button type="submit">Save</sl:button>
  </sl:dialog-footer>
</sl:dialog>
```

All three follow shadcn/ui's part names on the native `<dialog>`. In Basecoat and starting-point-ui you write the ids,
the ARIA wiring and the close behaviour yourself, in every template.

## Where Basecoat is stronger

- **Any stack.** Django, Flask, Express, Go, Rails, PHP or static HTML: the same classes everywhere.
- **No build at all.** A stylesheet from a CDN holds everything it needs; as a Tailwind plugin it mixes with your own
  utility classes.
- **Plain HTML** that you read, copy and change as you like, with no component engine between you and the markup.
- **The most widely used** library of its kind, with thousands of GitHub stars.

## Where starting-point-ui is stronger

- **Any stack.** The same classes in Laravel, Rails, Django or a React application.
- **Wide range.** Around 50 components, among them some Shadleaf has not: a carousel, a command palette, a context
  menu, an OTP input, and chat message components.
- **Any shadcn/ui theme**, pasted in as it is, and right-to-left support.
- **Small script.** Vanilla TypeScript on native elements, with Playwright tests for each interactive component.

## Where Shadleaf is stronger

- **Components instead of markup.** One template per component, declared props that fail on a typo, and a fix that
  reaches every page. See [what a component engine adds](https://shadleaf.dev/1.0.3/compared.md#what-a-component-engine-adds).
- **Spring wiring.** `th:field` sets the label, the error message and `aria-invalid` (which both libraries only
  style); tables sort and page a Spring Data `Page`; the user menu reads Spring Security; texts are translated. With
  either library, all of that is yours to write.
- **No Tailwind build.** starting-point-ui is imported into your own Tailwind v4 build; Shadleaf's jar ships its CSS.
- **More components than Basecoat.** Basecoat has no calendar, date picker or file upload, and its table is styling
  only; its chart and drawer are marked beta.
- **A strict Content-Security-Policy.** Basecoat's documented dialog markup, its dialog macro and the action buttons of
  its toasts use inline `onclick` handlers, which a policy without `'unsafe-inline'` blocks. Shadleaf renders none
  ([CSP](https://shadleaf.dev/1.0.3/guides/csp.md)).
- **Tested accessibility.** axe runs on every Shadleaf example in every skin, with keyboard and forced-colours tests;
  Basecoat publishes no such tests.
- **Stability.** starting-point-ui is before 1.0 and its classes can change between versions.

Shadleaf and Basecoat have the same eight shadcn/ui styles (vega, nova, maia, lyra, mira, luma, sera, rhea) and
shadcn/ui's colour tokens, so a theme works in all three. Shadleaf adds one skin of its own, `sketch`, for mockups.

## Which one to choose

**Choose Basecoat** for shadcn/ui's look in a stack that is not Spring Boot with Thymeleaf, with no build at all.
**Choose starting-point-ui** for the same in a project that already compiles Tailwind v4 and wants its wider range of
components. Either also fits a Thymeleaf application where you want to own every line of markup. **Choose Shadleaf**
to write components instead of markup, with forms, tables and security wired to Spring.
