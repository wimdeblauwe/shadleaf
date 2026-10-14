# Tooltip

> A short text that shows while the pointer rests on an element or the keyboard focuses it.

Source: https://wimdeblauwe.github.io/shadleaf/current/components/tooltip/

**Example: Tooltip.** Shows while the pointer rests on the button, and at once when the keyboard focuses it.

```html
<sl:tooltip>
  <sl:button variant="outline">Hover</sl:button>
  <sl:tooltip-content>Add to library</sl:tooltip-content>
</sl:tooltip>
```

## Usage

`<sl:tooltip>` wraps the element it describes, the trigger, and a `<sl:tooltip-content>` after it. The trigger is
the first element inside: a `<sl:button>`, a link, anything that takes focus.

```html
<sl:tooltip>
  <sl:button variant="outline">Hover</sl:button>
  <sl:tooltip-content>Add to library</sl:tooltip-content>
</sl:tooltip>
```

The tooltip shows when the pointer rests on the trigger for a moment (`delay`, 300 ms by default), and at once when
the keyboard focuses it or when another tooltip hid a moment ago. It stays while the pointer moves onto it, so it can
be read when zoomed in, and hides when the pointer leaves both, when the trigger loses focus or is pressed, and on
Escape. Escape then does nothing else, so a dialog or menu around the trigger stays open. Showing a tooltip never
closes an open menu or popover either, so an item in a dropdown menu can have one. One tooltip shows at a time, and
touch screens never show one.

`<sl:tooltip>` itself has no box, so the trigger keeps its place in the layout, in a toolbar or in running text.

## Describes, does not name

The tooltip describes its trigger (`aria-describedby`, added to any description the trigger already has), so a screen
reader reads it after the trigger's name. It does not replace the name: an icon-only button keeps its `aria-label`.

**Example: With a keyboard shortcut.** An icon button keeps its aria-label as its name; the tooltip describes it, shortcut included.

```html
<sl:tooltip>
  <sl:button variant="outline" size="icon" aria-label="Save"><sl:icon name="save"/></sl:button>
  <sl:tooltip-content>
    Save changes <sl:kbd-group><sl:kbd>Ctrl</sl:kbd><sl:kbd>S</sl:kbd></sl:kbd-group>
  </sl:tooltip-content>
</sl:tooltip>
```

### When it repeats the name

A tooltip that only shows an icon button's name (the same words as its `aria-label`) would make a screen reader say
them twice: "Edit, button, Edit". `mode="label"` says the tooltip repeats the name: it is hidden from assistive
technology (`aria-hidden` on the content) and describes nothing. It still shows on hover and keyboard focus.

```html
<sl:tooltip mode="label">
  <sl:button variant="outline" size="icon" aria-label="Edit"><sl:icon name="pencil"/></sl:button>
  <sl:tooltip-content>Edit</sl:tooltip-content>
</sl:tooltip>
```

**Example: Repeating the name.** mode="label" is for a tooltip that says what the trigger's name already says, such as an icon button's aria-label: it is hidden from assistive technology and describes nothing, so a screen reader does not hear the name twice.

```html
<sl:tooltip mode="label">
  <sl:button variant="outline" size="icon" aria-label="Edit"><sl:icon name="pencil"/></sl:button>
  <sl:tooltip-content>Edit</sl:tooltip-content>
</sl:tooltip>
<sl:tooltip mode="label">
  <sl:button variant="outline" size="icon" aria-label="Delete"><sl:icon name="trash"/></sl:button>
  <sl:tooltip-content>Delete</sl:tooltip-content>
</sl:tooltip>
```

The [sidebar's](https://wimdeblauwe.github.io/shadleaf/current/components/sidebar.md#collapsed-to-icons) menu buttons use this for their labels while it is collapsed to icons.

Keep the text short and plain. A tooltip cannot hold links or buttons (it hides when the pointer or focus leaves), and
nobody on a touch screen sees it, so never put information in it that is not available another way. For richer
content that the user can interact with, use a [popover](https://wimdeblauwe.github.io/shadleaf/current/components/popover.md).

A disabled button fires no pointer events and cannot take focus, so its tooltip never shows. Wrap it in an element
that takes focus (`<span tabindex="0">`) when the tooltip explains why it is disabled, or better, say why next to it.

## Position

The tooltip shows above its trigger, centred on it. `side="top|right|bottom|left"` and `align="start|center|end"` on
`<sl:tooltip-content>` change that, and it flips to the other side when there is no room, its arrow with it. `right`
and `left` are the end and the start of a line.

**Example: Sides.** side puts it on a side of its trigger; it flips when there is no room there.

```html
<sl:tooltip>
  <sl:button variant="outline">Top</sl:button>
  <sl:tooltip-content>Shown above</sl:tooltip-content>
</sl:tooltip>
<sl:tooltip>
  <sl:button variant="outline">Right</sl:button>
  <sl:tooltip-content side="right">Shown after</sl:tooltip-content>
</sl:tooltip>
<sl:tooltip>
  <sl:button variant="outline">Bottom</sl:button>
  <sl:tooltip-content side="bottom">Shown below</sl:tooltip-content>
</sl:tooltip>
<sl:tooltip>
  <sl:button variant="outline">Left</sl:button>
  <sl:tooltip-content side="left">Shown before</sl:tooltip-content>
</sl:tooltip>
```

It is placed with [CSS anchor positioning](https://developer.mozilla.org/en-US/docs/Web/CSS/CSS_anchor_positioning),
which every current browser supports (Chrome and Edge 133, Safari 26, Firefox 147). In older ones the tooltip's small
Alpine component places it from the trigger's position instead.

## Attributes

### sl:tooltip

Attributes of `<sl:tooltip>`: A short text that shows while the pointer rests on an element or the keyboard focuses it, on the browser's Popover API. The first element inside is the trigger, any focusable element (a `sl:button`, a link); the `sl:tooltip-content` after it describes it (`aria-describedby`). Escape hides it. Needs Alpine.

| Attribute | Values | Default | Description |
| --- | --- | --- | --- |
| `mode` | `description`, `label` | `description` | What the text is to a screen reader: `description` adds it to the trigger as a description; `label` is for a text that repeats the trigger's name, such as an icon button's `aria-label`: it is hidden from assistive technology and describes nothing, so the name is not said twice. |
| `delay` | *number* | `300` | How long the pointer rests on the trigger before the tooltip shows, in milliseconds. Keyboard focus shows it at once, and so does moving to another trigger right after a tooltip hid. |

Every prop can also be set from an expression: `th:mode="${...}"`. Any other attribute passes through to the rendered element, and `class` is appended.

### sl:tooltip-content

Attributes of `<sl:tooltip-content>`: The text of a `sl:tooltip` (`role="tooltip"`), after its trigger. Keep it short and plain: it cannot hold links or buttons, and touch screens never show it. It flips to the other side when there is no room. In a tooltip with `mode="label"` it is hidden from assistive technology (`aria-hidden`).

| Attribute | Values | Default | Description |
| --- | --- | --- | --- |
| `side` | `top`, `right`, `bottom`, `left` | `top` | The side of the trigger it shows on. `right` and `left` are the end and the start of a line. |
| `align` | `start`, `center`, `end` | `center` | How it lines up with the trigger along that side. |

Every prop can also be set from an expression: `th:side="${...}"`. Any other attribute passes through to the rendered element, and `class` is appended.

## JavaScript

The tooltip is shown and hidden by a small Alpine component, `slTooltip`: `<sl:tooltip>` renders as
`<span class="tooltip" x-data="slTooltip">`.

- **It needs Alpine.** Browsers cannot show a popup on hover by themselves yet, so without Alpine the trigger still
  works but no tooltip shows. Alpine is included by default; see [Alpine](https://wimdeblauwe.github.io/shadleaf/current/guides/alpine.md).
- **It needs no Alpine plugin.** If you load Alpine yourself (`shadleaf.assets.alpine=external`), plain Alpine is
  enough.
- **Keep your own `x-data` off `<sl:tooltip>`.** An element has one `x-data`, and attributes on `<sl:tooltip>` end up
  on that span, so yours would replace `slTooltip` and the tooltip would stop working. Put it on the trigger, or on an
  element around `<sl:tooltip>`.
