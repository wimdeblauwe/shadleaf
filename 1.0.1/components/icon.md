# Icon

> Inline SVG icons from lucide, or your own.

Source: https://shadleaf.dev/1.0.1/components/icon/

`<sl:icon>` inlines the SVG of a [lucide](https://lucide.dev) icon into the page: no sprite, no extra request, and it
works inside htmx swaps. It draws in `currentColor`, so it takes the colour of the text around it. An unknown name
fails with the closest known ones.

**Example: Icon.**

```html
<sl:icon name="leaf"/>
<sl:icon name="trash"/>
<sl:icon name="settings"/>
```

## Size and stroke width

**Example: Sizes.**

```html
<sl:icon name="leaf" size="16"/>
<sl:icon name="leaf"/>
<sl:icon name="leaf" size="32"/>
```
**Example: Stroke width.**

```html
<sl:icon name="leaf" stroke-width="1"/>
<sl:icon name="leaf"/>
<sl:icon name="leaf" stroke-width="3"/>
```

## Decorative or meaningful

Without a `label` an icon is decorative: it renders `aria-hidden="true"`, which is right next to text or inside a
labelled button. With a `label` it is announced as an image with that name.

**Example: A meaningful icon.** With a label the icon is announced as an image; without one it is hidden from assistive technology.

```html
<sl:icon name="circle-alert" label="Warning"/>
```

## Your own icons

Register an `IconSource` bean. Sources are asked in `@Order`, before the bundled lucide icons, so your icons can also
replace lucide's:

```java
@Bean
IconSource applicationIcons() {
  // reads src/main/resources/icons/<name>.svg
  return IconSource.classpathDirectory("icons/");
}
```

## Attributes

Attributes of `<sl:icon>`: Inlines the SVG of an icon: the bundled lucide icons, or the application's own through an `IconSource` bean. An unknown name fails with the closest known ones.

| Attribute | Values | Default | Description |
| --- | --- | --- | --- |
| `name` | *string* | — | Name of the icon, e.g. `trash`. Lucide's names and aliases, plus any icons the application registers. |
| `size` | *number* | — | Width and height in pixels. Without it the icon is 24 pixels, or sized by the surrounding component (16 pixels inside a button). |
| `stroke-width` | *number* | — | Stroke width, overriding the icon's own (2 for lucide). |
| `label` | *string* | — | Accessible name for an icon that carries meaning on its own. Without it the icon is decorative and hidden from assistive technology. |

Every prop can also be set from an expression: `th:name="${...}"`. Any other attribute passes through to the rendered element, and `class` is appended.
