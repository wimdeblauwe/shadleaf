# Shadleaf and Vaadin

> Two ways to build a Spring Boot UI: Thymeleaf templates with Shadleaf's components, or Java components with Vaadin Flow.

Source: https://shadleaf.dev/1.0.3/compared/vaadin/

[Vaadin](https://vaadin.com) is the best-known UI framework for Java, and it runs on Spring Boot like Shadleaf. The two
answer the same question, how to give a Spring application a good-looking interface, in opposite ways. This page is
about Vaadin Flow, as of version 25 (November 2026); Vaadin's other part, Hilla, is a React front end with Java
endpoints.

## Two architectures

**Shadleaf** is a set of Thymeleaf components. A controller handles a request and returns a view; Thymeleaf renders
it to HTML, and the browser gets a page. Between requests the server remembers nothing about the page. Forms post,
links navigate, and [htmx](https://shadleaf.dev/1.0.3/guides/htmx.md) can swap parts of the page if you want that.

**Vaadin Flow** keeps the UI on the server. You build it from Java objects, a tree of components per user session,
and a client engine in the browser shows it with Vaadin's web components. A click goes to the server as an event, your
Java listener runs, and the changes go back to the browser. There are no templates and no requests you write.

```java
// Vaadin Flow
var name = new TextField("Name");
var binder = new BeanValidationBinder<>(Profile.class);
binder.forField(name).bind("name");
binder.setBean(service.load());

var dialog = new Dialog("Edit profile", name);
dialog.getFooter().add(
    new Button("Cancel", e -> dialog.close()),
    new Button("Save", e -> {
      if (binder.validate().isOk()) {
        service.save(binder.getBean());
        dialog.close();
      }
    }));
add(new Button("Edit profile", e -> dialog.open()));
```

```html
<!-- Shadleaf: profile.html -->
<sl:button commandfor="edit-profile" command="show-modal">Edit profile</sl:button>
<form th:action="@{/profile}" th:object="${profile}" method="post">
  <sl:dialog id="edit-profile" th:open="${#fields.hasErrors('*')}">
    <sl:dialog-header>
      <sl:dialog-title>Edit profile</sl:dialog-title>
    </sl:dialog-header>
    <sl:field th:field="*{name}">
      <sl:field-label>Name</sl:field-label>
      <sl:input/>
      <sl:field-error/>
    </sl:field>
    <sl:dialog-footer>
      <sl:button variant="outline" commandfor="edit-profile" command="close">Cancel</sl:button>
      <sl:button type="submit">Save</sl:button>
    </sl:dialog-footer>
  </sl:dialog>
</form>
```

```java
// Shadleaf: the controller
@GetMapping("/profile")
String show(Model model) {
  model.addAttribute("profile", service.load());
  return "profile";
}

@PostMapping("/profile")
String save(@Valid @ModelAttribute("profile") Profile profile, BindingResult result) {
  if (result.hasErrors()) {
    return "profile";
  }
  service.save(profile);
  return "redirect:/profile";
}
```

Both validate the name with Bean Validation and show its error under the field. In Vaadin the dialog stays open while
the user fixes it. In Shadleaf the form posts, and with errors the controller renders the page again, with the dialog
open (`th:open`, which needs Alpine). With htmx only the form is swapped, and the dialog never closes (see the
[dialog](https://shadleaf.dev/1.0.3/components/dialog.md)).

Most of the differences below follow from that one.

## Where Vaadin is stronger

- **Only Java.** The whole UI is Java: type-checked, refactorable, and testable without a browser, with browserless
  UI tests that are free since Vaadin 25.1. A team that knows no HTML or CSS can build a complete application.
- **No API layer.** You don't design, document or secure requests and endpoints. A click calls your Java method.
- **State stays on the server.** The client cannot change a hidden field or call an endpoint you forgot to protect,
  because there are none. It sends events, and the server decides what they mean.
- **Binder.** Form binding, conversion and validation work the same way in every screen.
- **Rich, stateful screens.** Its Grid scrolls lazily through large data sets, sorts, filters and edits in place, and
  the server can push changes to the browser. In Shadleaf each of those is a request you design.
- **Breadth.** Around 60 components, among them a tree grid, master-detail and split layouts, a login form and a
  message list. The commercial ones add charts, maps, a dashboard, a CRUD editor, a spreadsheet and a rich text
  editor.
- **A company behind it.** Paid support, long-term maintenance of a major version, a published table of WCAG criteria
  per component, a visual editor (Copilot) and Figma libraries.

## Where Shadleaf is stronger

- **Plain HTTP and HTML.** Every page is a request and a response, rendered on the server. It works with a load
  balancer without sticky sessions, holds no UI state per user in server memory, can be cached and indexed by search
  engines, and fits into an existing Spring MVC application one page at a time. Without its script the components
  still show their content, and forms and links still work; a Vaadin page is empty until its client engine has
  started. For partial updates, [htmx](https://shadleaf.dev/1.0.3/guides/htmx.md) swaps parts of the page, and Shadleaf's components are
  tested with its swaps and history restores.
- **Familiar to front-end developers.** A Shadleaf page is an HTML template with CSS and a little JavaScript, the
  same material as an Angular or React application. Developers coming from those frameworks read and debug it in the
  browser's developer tools, as they do today. In Vaadin the UI is Java objects, and the HTML they render is an
  implementation detail.
- **The web's ecosystem.** Any HTML, CSS or JavaScript library goes straight into a template: a map, a rich text
  editor, a charting library, an Alpine.js plugin, a snippet from a blog post. In Vaadin a JavaScript library first
  needs a Java class that wraps it (`@NpmPackage`, `@JsModule`), or an add-on from the Vaadin Directory.
- **A strict Content-Security-Policy.** Shadleaf runs under a policy without `'unsafe-inline'` or `'unsafe-eval'`, in
  development too ([CSP](https://shadleaf.dev/1.0.3/guides/csp.md)). Vaadin's default policy needs both. Its documentation describes a
  nonce-based strict policy for scripts, but only in production mode, and only after you replace `eval` and the
  `Function` constructor with a map of predefined code.
- **Java 17.** Shadleaf runs on Java 17; Vaadin 25 needs Java 21.
- **No licence fees.** All of Shadleaf is Apache 2.0, the chart included. Vaadin's core is Apache 2.0 too, but its
  Charts, Grid Pro, CRUD, Map, Dashboard and Rich Text Editor need a subscription, per developer, at the time of
  writing $159 per month.
- **shadcn/ui's design.** Shadleaf's skins are shadcn/ui's styles, themed with its tokens. Vaadin has its own themes,
  Lumo and Aura, styled through custom properties and the parts of its web components; Tailwind support is
  experimental.

## Which one to choose

**Choose Vaadin** for an internal application with complex, stateful screens (large editable grids, live updates), a
team that would rather write only Java, or when paid support and long-term maintenance matter.

**Choose Shadleaf** for an application that is, or should stay, Spring MVC with Thymeleaf: public pages, pages that
must work under a strict CSP or without JavaScript, horizontal scaling without session state, or a team that is happy
writing HTML (developers coming from Angular or React among them) and wants shadcn/ui's look.
