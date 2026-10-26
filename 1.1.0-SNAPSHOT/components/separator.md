# Separator

> A thin line between content.

Source: https://shadleaf.dev/1.1.0-SNAPSHOT/components/separator/

**Example: Separator.**

```html
<sl:separator/>
```

## Vertical

`orientation="vertical"` draws a vertical line that stretches to the height of the row it is in. Put it in a flex
container, such as a toolbar or a row of links.

**Example: Vertical.** A vertical separator stretches to the height of the flex row it is in.

```html
<sl:button as="a" href="/blog" variant="link">Blog</sl:button>
<sl:separator orientation="vertical"/>
<sl:button as="a" href="/docs" variant="link">Docs</sl:button>
<sl:separator orientation="vertical"/>
<sl:button as="a" href="/source" variant="link">Source</sl:button>
```

## Decorative or announced

A separator is decorative by default, as in shadcn/ui: it only draws a line and is hidden from assistive technology
(`role="none"`). When the line divides the content into groups a screen reader user should know about, write
`decorative="false"`. It is then announced as a separator (`role="separator"`, with `aria-orientation` when
vertical).

**Example: Announced.** decorative="false" makes it a separator for assistive technology too.

```html
<sl:separator decorative="false"/>
```

## Attributes

Attributes of `<sl:separator>`: A thin line between content. A vertical one stretches to the height of the row it is in, so it needs a flex container.

| Attribute | Values | Default | Description |
| --- | --- | --- | --- |
| `orientation` | `horizontal`, `vertical` | `horizontal` | Direction of the line. |
| `decorative` | *boolean* | `true` | Hide the line from assistive technology, as most separators only draw a line. With `decorative="false"` it is announced as a separator, for one that divides content into groups. |

Every prop can also be set from an expression: `th:orientation="${...}"`. Any other attribute passes through to the rendered element, and `class` is appended.
