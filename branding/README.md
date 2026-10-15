# Shadleaf branding

The logo, its colours and its typeface. Use the files here as they are; regenerate them with `pnpm run logo` in
`docs/` (`docs/scripts/logo.mjs`) rather than editing them by hand.

<picture>
  <source media="(prefers-color-scheme: dark)" srcset="shadleaf-logo-dark.svg">
  <img alt="Shadleaf" src="shadleaf-logo-light.svg" width="204" height="48">
</picture>

## The design

**The mark** is a leaf split along its midrib: one half in light, one in shade. It is the name taken literally,
shad(e) + leaf, and the leaf keeps the line to Thymeleaf visible. It needs no background tile and stays readable at
16px.

**The wordmark** is "shadleaf" in lowercase, split the same way as the leaf: "shad" is light and quiet, "leaf" is
heavy. In running text the project is written "Shadleaf".

The colours, pine teal on slate greys, are deliberately cooler than Thymeleaf's and Spring's greens.

## Files

| File                               | Use                                                                                |
|------------------------------------|------------------------------------------------------------------------------------|
| `shadleaf-logo-light.svg` / `.png` | Mark and wordmark, for light backgrounds. PNG: 816×192, transparent.               |
| `shadleaf-logo-dark.svg` / `.png`  | Mark and wordmark, for dark backgrounds. PNG: 816×192, transparent.                |
| `shadleaf-mark-light.svg` / `.png` | The mark alone (avatars, icons), for light backgrounds. PNG: 512×512, transparent. |
| `shadleaf-mark-dark.svg` / `.png`  | The mark alone, for dark backgrounds. PNG: 512×512, transparent.                   |

The SVGs have the wordmark as outlines, so they need no font installed. Elsewhere in the repository:

- `docs/public/favicon.svg` and `samples/*/src/main/resources/static/favicon.svg`: the mark, switching colours with
  the system's colour scheme (written by the same script).
- `swatches/`: the colour squares in the tables below, one per colour (written by the same script, which also adds
  them to the tables: GitHub shows no colour for a hex code in a Markdown file).
- `docs/src/components/SiteTitle.astro`: the docs header, which draws the mark inline and sets the wordmark as text,
  taking its colours from `docs/src/styles/docs.css`.

For a README or other Markdown on GitHub, pick the file by colour scheme:

```html
<picture>
  <source media="(prefers-color-scheme: dark)" srcset="branding/shadleaf-logo-dark.svg">
  <img alt="Shadleaf" src="branding/shadleaf-logo-light.svg" width="204" height="48">
</picture>
```

## Colours

### Logo

| Part                       | Light background                                                                          | Dark background                                                                           |
|----------------------------|-------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------|
| Lit half of the leaf, stem | <img src="swatches/0f6e66.svg" width="16" height="16" alt="" align="absmiddle"> `#0f6e66` | <img src="swatches/3bbfb1.svg" width="16" height="16" alt="" align="absmiddle"> `#3bbfb1` |
| Shaded half of the leaf    | <img src="swatches/6fcfc3.svg" width="16" height="16" alt="" align="absmiddle"> `#6fcfc3` | <img src="swatches/13665e.svg" width="16" height="16" alt="" align="absmiddle"> `#13665e` |
| "shad"                     | <img src="swatches/475569.svg" width="16" height="16" alt="" align="absmiddle"> `#475569` | <img src="swatches/94a3b8.svg" width="16" height="16" alt="" align="absmiddle"> `#94a3b8` |
| "leaf"                     | <img src="swatches/0f172a.svg" width="16" height="16" alt="" align="absmiddle"> `#0f172a` | <img src="swatches/f1f5f9.svg" width="16" height="16" alt="" align="absmiddle"> `#f1f5f9` |

On a dark background the shaded half is the darker teal and on a light one the paler teal, so the lit half is always
the one that stands out.

### Docs site (Starlight)

The accent is pine teal; the greys are Tailwind's slate. Set in `docs/src/styles/docs.css`.

| Token                           | Light                                                                                     | Dark                                                                                      |
|---------------------------------|-------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------|
| `--sl-color-accent-low`         | <img src="swatches/d3ecea.svg" width="16" height="16" alt="" align="absmiddle"> `#d3ecea` | <img src="swatches/0c2725.svg" width="16" height="16" alt="" align="absmiddle"> `#0c2725` |
| `--sl-color-accent`             | <img src="swatches/0f6e66.svg" width="16" height="16" alt="" align="absmiddle"> `#0f6e66` | <img src="swatches/3bbfb1.svg" width="16" height="16" alt="" align="absmiddle"> `#3bbfb1` |
| `--sl-color-accent-high`        | <img src="swatches/0a3a36.svg" width="16" height="16" alt="" align="absmiddle"> `#0a3a36` | <img src="swatches/c2ece6.svg" width="16" height="16" alt="" align="absmiddle"> `#c2ece6` |
| Background (`--sl-color-black`) | <img src="swatches/ffffff.svg" width="16" height="16" alt="" align="absmiddle"> `#ffffff` | <img src="swatches/0f172a.svg" width="16" height="16" alt="" align="absmiddle"> `#0f172a` |
| Text (`--sl-color-white`)       | <img src="swatches/0f172a.svg" width="16" height="16" alt="" align="absmiddle"> `#0f172a` | <img src="swatches/ffffff.svg" width="16" height="16" alt="" align="absmiddle"> `#ffffff` |

`#0f6e66` on white has a contrast of 6:1, so it works for links and small text; on dark backgrounds use `#3bbfb1` or
lighter.

## Typeface

[Schibsted Grotesk](https://fonts.google.com/specimen/Schibsted+Grotesk) (SIL Open Font License), from the
`@fontsource/schibsted-grotesk` package.

- "shad": weight 400 (Regular); "leaf": weight 800 (ExtraBold).
- Letter-spacing -0.02em, no space between the two halves.
- In the lockup the mark is 1.22× the font size, with a gap of a third of the font size, and the cap height centred on
  the mark.

The typeface is for the wordmark only. The docs' body text keeps Starlight's system font stack.

## Using the logo

- Keep clear space around the logo of at least the mark's width.
- Use the mark alone below 120px wide; the wordmark gets hard to read below that.
- Don't recolour, stretch, rotate or outline the logo, and don't swap the weights of "shad" and "leaf".
