# Shadleaf

> UI components for Thymeleaf, as a Spring Boot starter.

Source: https://shadleaf.dev/1.0.2/

Shadleaf is a library of UI components for Thymeleaf, as a Spring Boot starter. Add one dependency, include two
fragments in your layout's `<head>`, and write `<sl:button>`:

**Example: Variants.**

```html
<sl:button>Default</sl:button>
<sl:button variant="outline">Outline</sl:button>
<sl:button variant="ghost">Ghost</sl:button>
<sl:button variant="destructive">Destructive</sl:button>
<sl:button variant="secondary">Secondary</sl:button>
<sl:button variant="link">Link</sl:button>
```

- **Fits your build.** The jar ships compiled CSS, served under `/shadleaf/**`, so a plain Maven build is enough.
  Already compiling Tailwind? The [embedded bundle](guides/tailwind/) fits into your own
  build.
- **Themed with CSS variables.** `--primary`, `--radius`, `--ring`: redefine them and every component follows, see
  [token overrides](guides/adjustability/#3-token-overrides).
- **Declared props.** A typo such as `variant="destructve"` fails with the legal values, and an icon-only button
  without an `aria-label` fails too, instead of shipping.
- **Adjustable in five steps**, from a prop to owning the template yourself: see
  [the adjustability ladder](guides/adjustability/).
- **Works under a strict Content-Security-Policy**, with no `'unsafe-inline'`: see [CSP](guides/csp/).

Every preview on this site is rendered by the library itself, from the build that produced these pages, so it shows
exactly the markup your application gets.

> **Caution: Pre-release**
>
> Before 1.0, the rendered markup, class names and `data-*` attributes can change between versions.
