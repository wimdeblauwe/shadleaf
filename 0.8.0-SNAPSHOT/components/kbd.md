# Kbd

> A key on the keyboard, or a combination of keys.

Source: https://wimdeblauwe.github.io/shadleaf/0.8.0-SNAPSHOT/components/kbd/

**Example: Kbd.**

```html
<sl:kbd>Esc</sl:kbd>
<sl:kbd>Enter</sl:kbd>
<sl:kbd>/</sl:kbd>
```

`<sl:kbd>` renders HTML's own `<kbd>` element, so it fits in running text.

**Example: In a sentence.**

```html
<p>Press <sl:kbd>/</sl:kbd> to search.</p>
```

## Key combinations

Put the keys pressed together in `<sl:kbd-group>`. It is an outer `<kbd>`, which is how HTML marks up a combination.
Text between the keys, such as `+`, is up to you.

**Example: A key combination.**

```html
<sl:kbd-group>
  <sl:kbd>Ctrl</sl:kbd>
  <sl:kbd>Shift</sl:kbd>
  <sl:kbd>P</sl:kbd>
</sl:kbd-group>
<sl:kbd-group><sl:kbd>Ctrl</sl:kbd>+<sl:kbd>K</sl:kbd></sl:kbd-group>
```

## Icons

A key can hold an [`<sl:icon>`](https://wimdeblauwe.github.io/shadleaf/0.8.0-SNAPSHOT/components/icon.md). Give the icon a `label`, as a screen reader has no text to read otherwise.
An `aria-label` on the key itself would not do: `<kbd>` has no role that takes a name.

Lucide has icons for the usual keys, so you never need to type a symbol: `command`, `option`, `arrow-big-up` (Shift),
`corner-down-left` (Enter), `delete` (Backspace), `arrow-up` / `arrow-down` / `arrow-left` / `arrow-right`, and
`space`.

**Example: With an icon.** An icon on its own has no text, so give the key a name.

```html
<sl:kbd-group>
  <sl:kbd><sl:icon name="command" label="Command"/></sl:kbd>
  <sl:kbd>K</sl:kbd>
</sl:kbd-group>
```

## In a button

A key can sit next to a button's label, to show its shortcut. Hide the key from screen readers with
`aria-hidden="true"`, or it becomes part of the button's name ("Accept Enter"), and name the shortcut in
`aria-keyshortcuts` on the button instead. An icon in a hidden key needs no `label`. `aria-keyshortcuts` only
announces the shortcut: listening for the key is up to your own script.

**Example: In a button.** The shortcut goes in aria-keyshortcuts; the key itself is hidden from screen readers.

```html
<sl:button aria-keyshortcuts="Enter">
  Accept <sl:kbd aria-hidden="true"><sl:icon name="corner-down-left"/></sl:kbd>
</sl:button>
<sl:button variant="outline" aria-keyshortcuts="Escape">
  Cancel <sl:kbd aria-hidden="true">Esc</sl:kbd>
</sl:button>
```

## Attributes

### sl:kbd

Attributes of `<sl:kbd>`: A key on the keyboard, as text (`Ctrl`) or an `sl:icon`. Several keys pressed together go in an `sl:kbd-group`.

`sl:kbd` declares no props; every attribute passes through.

### sl:kbd-group

Attributes of `<sl:kbd-group>`: A key combination: `sl:kbd` elements, with any text between them (such as `+`). HTML nests the keys of a combination in an outer `kbd`, as this does.

`sl:kbd-group` declares no props; every attribute passes through.
