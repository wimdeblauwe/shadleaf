# Popover

> Rich content in a panel next to the button that opens it, on the browser's Popover API.

Source: https://shadleaf.dev/current/components/popover/

**Example: Popover.** A small form next to its trigger. Focus moves to the first field when it opens.

```html
<sl:popover id="dimensions">
  <sl:popover-trigger variant="outline">Open popover</sl:popover-trigger>
  <sl:popover-content>
    <sl:popover-header>
      <sl:popover-title>Dimensions</sl:popover-title>
      <sl:popover-description>Set the dimensions for the layer.</sl:popover-description>
    </sl:popover-header>
    <sl:field-group>
      <sl:field orientation="horizontal" for="dimensions-width">
        <sl:field-label>Width</sl:field-label>
        <sl:input value="100%"/>
      </sl:field>
      <sl:field orientation="horizontal" for="dimensions-height">
        <sl:field-label>Height</sl:field-label>
        <sl:input value="25px"/>
      </sl:field>
    </sl:field-group>
  </sl:popover-content>
</sl:popover>
```

## Built from parts

A popover is `<sl:popover>` with a `<sl:popover-trigger>` and a `<sl:popover-content>`. The
content can hold a `<sl:popover-header>` with `<sl:popover-title>` and `<sl:popover-description>`, and anything
else: text, a small form, links. `<sl:popover>` renders no element of its own: it gives the parts their ids.

```html
<sl:popover id="dimensions">
  <sl:popover-trigger variant="outline">Open popover</sl:popover-trigger>
  <sl:popover-content>
    <sl:popover-header>
      <sl:popover-title>Dimensions</sl:popover-title>
      <sl:popover-description>Set the dimensions for the layer.</sl:popover-description>
    </sl:popover-header>
    ...
  </sl:popover-content>
</sl:popover>
```

The panel is a [popover](https://developer.mozilla.org/en-US/docs/Web/API/Popover_API): it is drawn above everything
else, Escape or a click outside closes it, and closing returns focus to the trigger. When it opens, focus moves to
its first control (or to an element with `autofocus`), or to the panel when it has none; moving the focus out of it,
with Tab past its last control for example, closes it. The trigger is a `<sl:button>` with `popovertarget` set to the
popover's `id`; any other button can open it the same way.

The panel is a non-modal dialog (`role="dialog"`): the rest of the page stays usable. The title names it and the
description describes it (`aria-labelledby` and `aria-describedby`, with ids derived from the popover's `id`:
`dimensions-title`). A popover without a title needs an `aria-label` on `<sl:popover-content>`. `level` on the title
makes it a heading.

## Position

The panel opens below its trigger, centred on it. `side="top|right|bottom|left"` and `align="start|center|end"` on
`<sl:popover-content>` change that, and it flips to the other side when there is no room. `right` and `left` are the
end and the start of a line, so they swap on a right-to-left page.

**Example: Sides.** side puts it on a side of its trigger; it flips when there is no room there.

```html
<sl:popover id="popover-top">
  <sl:popover-trigger variant="outline">Top</sl:popover-trigger>
  <sl:popover-content side="top" aria-label="Top">Opens above its trigger.</sl:popover-content>
</sl:popover>
<sl:popover id="popover-right">
  <sl:popover-trigger variant="outline">Right</sl:popover-trigger>
  <sl:popover-content side="right" aria-label="Right">Opens after its trigger.</sl:popover-content>
</sl:popover>
<sl:popover id="popover-bottom">
  <sl:popover-trigger variant="outline">Bottom</sl:popover-trigger>
  <sl:popover-content aria-label="Bottom">Opens below its trigger.</sl:popover-content>
</sl:popover>
<sl:popover id="popover-left">
  <sl:popover-trigger variant="outline">Left</sl:popover-trigger>
  <sl:popover-content side="left" aria-label="Left">Opens before its trigger.</sl:popover-content>
</sl:popover>
```

It is placed with [CSS anchor positioning](https://developer.mozilla.org/en-US/docs/Web/CSS/CSS_anchor_positioning),
which every current browser supports (Chrome and Edge 133, Safari 26, Firefox 147). In older ones the popover's small
Alpine component places it from the trigger's position instead.

## A form in a popover, with htmx

A form in the panel can post with htmx and swap only what it changes. An `HX-Trigger: sl-popover-close` response
header closes the popover after a valid save, and focus returns to the trigger:

```html
<h2 id="team-name" th:text="${team.name}">Design</h2>
<sl:popover id="rename-team">
  <sl:popover-trigger variant="outline" size="sm">Rename</sl:popover-trigger>
  <sl:popover-content>
    <sl:popover-header><sl:popover-title>Rename team</sl:popover-title></sl:popover-header>
    <form th:replace="~{team :: rename-form}"></form>
  </sl:popover-content>
</sl:popover>
```

```java
@HxRequest
@PostMapping("/team/name")
public String rename(@Valid @ModelAttribute("renameForm") RenameForm form, BindingResult bindingResult,
    HtmxResponse htmxResponse, Model model) {
  if (bindingResult.hasErrors()) {
    return "team :: rename-form";  // the form swaps itself: the popover stays open
  }
  model.addAttribute("team", teamService.rename(form.getName()));
  htmxResponse.addTrigger("sl-popover-close");
  return "team :: team-name-oob";  // the heading, with hx-swap-oob="true"
}
```

htmx fires `sl-popover-close` on the form; it bubbles up to the popover, which closes. A popover nested in another
closes only itself. Back never brings a popover back open: whether it is open is not part of the markup htmx saves in
its history cache.

## Left out

- **`PopoverAnchor`**: the popover is placed against its trigger.
- **`PopoverPortal`**: the top layer does what the portal is for.
- **State in the browser** (`open`/`onOpenChange`): the trigger opens it; the `sl-popover-close` event closes it.
- **`modal`**: a popover never makes the rest of the page inert. Use a [dialog](https://shadleaf.dev/current/components/dialog.md) for that.

## Attributes

### sl:popover

Attributes of `<sl:popover>`: Rich content in a panel that opens from a button, on the browser's Popover API: it sits above the page next to its trigger, and Escape, a click outside or moving the focus out of it closes it. Holds a `sl:popover-trigger` and a `sl:popover-content`. Renders no element of its own.

| Attribute | Values | Default | Description |
| --- | --- | --- | --- |
| `id` | *string* | **required** | The popover's id, which the trigger points at with `popovertarget`. The title and description ids derive from it. |

Every prop can also be set from an expression: `th:id="${...}"`. Any other attribute passes through to the rendered element, and `class` is appended.

### sl:popover-trigger

Attributes of `<sl:popover-trigger>`: The button that opens and closes a `sl:popover`: a `sl:button` with `popovertarget` set to the popover. Any other attribute passes through to the `sl:button`.

| Attribute | Values | Default | Description |
| --- | --- | --- | --- |
| `variant` | `default`, `outline`, `ghost`, `destructive`, `secondary`, `link` | `default` | Visual style of the button, as on `sl:button`. |
| `size` | `xs`, `sm`, `default`, `lg`, `icon`, `icon-xs`, `icon-sm`, `icon-lg` | `default` | Size of the button, as on `sl:button`. The `icon` sizes need an `aria-label`. |

Every prop can also be set from an expression: `th:variant="${...}"`. Any other attribute passes through to the rendered element, and `class` is appended.

### sl:popover-content

Attributes of `<sl:popover-content>`: The panel of a `sl:popover`, next to its trigger in the top layer. It is a non-modal dialog: name it with a `sl:popover-title` (in a `sl:popover-header`) or an `aria-label`. When it opens, focus moves to its first control. It flips to the other side when there is no room.

| Attribute | Values | Default | Description |
| --- | --- | --- | --- |
| `side` | `top`, `right`, `bottom`, `left` | `bottom` | The side of the trigger it opens on. `right` and `left` are the end and the start of a line: on a right-to-left page a `right` popover opens on the left. |
| `align` | `start`, `center`, `end` | `center` | How it lines up with the trigger along that side. |

Every prop can also be set from an expression: `th:side="${...}"`. Any other attribute passes through to the rendered element, and `class` is appended.

### sl:popover-header

Attributes of `<sl:popover-header>`: The top of a `sl:popover-content`, holding `sl:popover-title` and `sl:popover-description`.

`sl:popover-header` declares no props; every attribute passes through.

### sl:popover-title

Attributes of `<sl:popover-title>`: The title of a `sl:popover`, inside `sl:popover-header`, which names the popover (`aria-labelledby`). Its id derives from the popover's (`dimensions-title`).

| Attribute | Values | Default | Description |
| --- | --- | --- | --- |
| `level` | `1`, `2`, `3`, `4`, `5`, `6` | — | Heading level. With it the title is a heading (`role="heading"` and `aria-level`); without it, plain text. |

Every prop can also be set from an expression: `th:level="${...}"`. Any other attribute passes through to the rendered element, and `class` is appended.

### sl:popover-description

Attributes of `<sl:popover-description>`: Supporting text below a `sl:popover-title`, which describes the popover (`aria-describedby`). Its id derives from the popover's (`dimensions-description`).

`sl:popover-description` declares no props; every attribute passes through.

## JavaScript

A small Alpine component, `slPopover`, adds what the browser's popover lacks: `<sl:popover-content>` renders as
`<div class="popover-content" role="dialog" popover x-data="slPopover">`.

- **The basics work without Alpine.** The trigger opens and closes the popover, and Escape and a click outside close
  it. Alpine is needed for moving the focus into it when it opens, closing it when the focus leaves, the
  `sl-popover-close` event, `aria-expanded` on the trigger, and placing it in browsers without CSS anchor positioning.
  Alpine is included by default; see [Alpine](https://shadleaf.dev/current/guides/alpine.md).
- **It needs no Alpine plugin.** If you load Alpine yourself (`shadleaf.assets.alpine=external`), plain Alpine is
  enough.
- **Keep your own `x-data` off `<sl:popover-content>`.** An element has one `x-data`, and attributes on it end up on
  the panel, so yours would replace `slPopover`. Put it on an element inside the panel.

| Event | On | Effect |
| --- | --- | --- |
| `sl-popover-close` | the popover, or anything inside it (it bubbles) | Closes the popover. |
| `toggle` | the popover (the browser's own) | Fired when it opens or closes, however that happened. |
