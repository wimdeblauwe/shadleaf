# Accordion

> A stack of sections, each with a heading that shows or hides its content, on the browser's details element.

Source: https://shadleaf.dev/0.8.0/components/accordion/

**Example: Accordion.** One section open at a time. Opening another closes it; the open one closes on a click.

```html
<sl:accordion name="accordion-demo">
  <sl:accordion-item open>
    <sl:accordion-trigger>Is it accessible?</sl:accordion-trigger>
    <sl:accordion-content>Yes. Each section is a details element, which the browser opens and closes, from the
      keyboard too.</sl:accordion-content>
  </sl:accordion-item>
  <sl:accordion-item>
    <sl:accordion-trigger>Is it styled?</sl:accordion-trigger>
    <sl:accordion-content>Yes. It comes with default styles that match the other components.</sl:accordion-content>
  </sl:accordion-item>
  <sl:accordion-item>
    <sl:accordion-trigger>Is it animated?</sl:accordion-trigger>
    <sl:accordion-content>Yes, where the browser can animate a height to auto, and only when the user has not
      asked for reduced motion.</sl:accordion-content>
  </sl:accordion-item>
</sl:accordion>
```

## Built from parts

An accordion is `<sl:accordion>` holding `<sl:accordion-item>`s, each with an `<sl:accordion-trigger>` and an
`<sl:accordion-content>`, as in shadcn/ui:

```html
<sl:accordion>
  <sl:accordion-item open>
    <sl:accordion-trigger>Is it accessible?</sl:accordion-trigger>
    <sl:accordion-content>Yes. It adheres to the WAI-ARIA design pattern.</sl:accordion-content>
  </sl:accordion-item>
  <sl:accordion-item>
    <sl:accordion-trigger>Is it styled?</sl:accordion-trigger>
    <sl:accordion-content>Yes.</sl:accordion-content>
  </sl:accordion-item>
</sl:accordion>
```

Each item is a [`<details>`](https://developer.mozilla.org/en-US/docs/Web/HTML/Reference/Elements/details) element and
its trigger is the `<summary>`, so **the accordion needs no JavaScript**: the browser opens and closes the sections,
from the keyboard too (Tab, then Enter or Space), and a screen reader announces each trigger as expanded or collapsed.
The trigger must be the item's first child.

## One or several open

`type="single"` (the default) keeps one section open: opening another closes it. The items share a `details` `name`,
which the browser uses to close the others. The open section closes on a click too, so a single accordion is always
what shadcn/ui calls `collapsible`. The name must be unique on the page; without `name` each render generates one
(`sl-accordion-` and eight random characters), so an accordion that htmx swaps in never joins one that is already there.
Set `name` yourself when you want stable markup, but not on an accordion that htmx replaces: see below.

`type="multiple"` opens and closes every section on its own:

**Example: Multiple.** type="multiple" opens and closes each section on its own.

```html
<sl:accordion type="multiple">
  <sl:accordion-item open>
    <sl:accordion-trigger>How do I reset my password?</sl:accordion-trigger>
    <sl:accordion-content>Choose "Forgot password" on the sign-in page and follow the link we email
      you.</sl:accordion-content>
  </sl:accordion-item>
  <sl:accordion-item open>
    <sl:accordion-trigger>Can I change my username?</sl:accordion-trigger>
    <sl:accordion-content>Yes, once every 30 days, from your profile settings.</sl:accordion-content>
  </sl:accordion-item>
</sl:accordion>
```

## Open from the server

`open` on an item renders it open, where shadcn/ui takes `defaultValue`. The server decides, for example from a
request parameter or a fragment of the URL it rendered a link to:

```html
<sl:accordion>
  <sl:accordion-item th:each="question : ${faq}" th:open="${question.id == openQuestion}" th:id="${question.id}">
    <sl:accordion-trigger>[[${question.title}]]</sl:accordion-trigger>
    <sl:accordion-content th:utext="${question.answerHtml}">Answer</sl:accordion-content>
  </sl:accordion-item>
</sl:accordion>
```

In a single accordion only the first open item stays open. Write the trigger's text with `[[${...}]]`, not `th:text`:
`th:text` replaces the trigger's content, heading and chevron included.

## Headings

shadcn/ui puts each trigger in an `h3`. A `<summary>` must be the first child of its `<details>`, so it cannot sit in
a heading; the heading goes inside it instead. The trigger renders
`<summary><span role="heading" aria-level="3">…</span><svg …/></summary>`: the summary keeps its own role, a
disclosure with its expanded state, and screen reader users can still jump from question to question by heading.
`level` sets the level (3 by default); pick the one that fits the page's outline. For content that is not a list of
headed sections, use a [collapsible](https://shadleaf.dev/0.8.0/components/collapsible.md).

## Find in page and links

The content of a closed section is still in the page. When the browser's find-in-page, or a link to an element inside
it (`/faq#refunds`), finds a match there, the browser opens the section. This works in Chrome and Edge 97, Firefox 148
and Safari 26.2 (where Safari and Firefox 139 to 147 do not scroll to the match yet); older browsers do not search
closed sections.

## Animation

The section's height animates open and closed where the browser can animate a height to `auto`
([`interpolate-size`](https://developer.mozilla.org/en-US/docs/Web/CSS/interpolate-size), Chrome and Edge 129). The
height is that of `::details-content`, the box around everything but the summary (Chrome 131, Safari 18.4, Firefox
143). Other browsers open and close the section at once, and so does every browser when the user asks for reduced
motion. The chevron turns in both cases (not under reduced motion).

## In a card

**Example: In a card.**

```html
<sl:card>
  <sl:card-header>
    <sl:card-title level="2">Subscription &amp; billing</sl:card-title>
    <sl:card-description>Common questions about your account, plans and payments</sl:card-description>
  </sl:card-header>
  <sl:card-content>
    <sl:accordion name="accordion-billing">
      <sl:accordion-item open>
        <sl:accordion-trigger>What subscription plans do you offer?</sl:accordion-trigger>
        <sl:accordion-content>
          <p>Three: Starter, Professional and Enterprise, each with more storage and support.</p>
          <p><a href="/plans">Annual billing is available</a> with a 20% discount.</p>
        </sl:accordion-content>
      </sl:accordion-item>
      <sl:accordion-item>
        <sl:accordion-trigger>How does billing work?</sl:accordion-trigger>
        <sl:accordion-content>At the start of each billing cycle, by card or bank transfer.</sl:accordion-content>
      </sl:accordion-item>
      <sl:accordion-item>
        <sl:accordion-trigger>How do I cancel?</sl:accordion-trigger>
        <sl:accordion-content>From your account settings, at any time.</sl:accordion-content>
      </sl:accordion-item>
    </sl:accordion>
  </sl:card-content>
</sl:card>
```

## Disabled

`disabled` on an item keeps it closed (or open, with `open`): its trigger fades, leaves the tab order and ignores the
pointer. A closed disabled item renders no content, so find-in-page cannot open it either. shadcn/ui does the same by
not rendering closed content at all.

**Example: Disabled.** A disabled section cannot be opened, and renders no content while it is closed.

```html
<sl:accordion name="accordion-disabled">
  <sl:accordion-item>
    <sl:accordion-trigger>Can I access my account history?</sl:accordion-trigger>
    <sl:accordion-content>Yes, in the Account history section of your dashboard.</sl:accordion-content>
  </sl:accordion-item>
  <sl:accordion-item disabled>
    <sl:accordion-trigger>Premium feature information</sl:accordion-trigger>
    <sl:accordion-content>Upgrade your plan to see this.</sl:accordion-content>
  </sl:accordion-item>
  <sl:accordion-item>
    <sl:accordion-trigger>How do I update my email address?</sl:accordion-trigger>
    <sl:accordion-content>In your account settings; we send a link to the new address.</sl:accordion-content>
  </sl:accordion-item>
</sl:accordion>
```

## With htmx

An accordion is plain markup, so a swap that replaces it shows it as the server rendered it, and htmx's history cache
keeps the sections the user opened (the `open` attribute is in the markup it saves).

Leave `name` out of a single accordion that a swap replaces. htmx inserts the new markup before it removes the old,
and the browser closes an open `details` that joins a group which already has one open: with the same name on both,
the section the server rendered open arrives closed. The generated name is new on every render, so it never meets
the old one.

## Left out

- **Arrow keys between the triggers.** Radix moves the focus with the up and down arrows, Home and End; the ARIA
  pattern makes these optional. A `details` accordion has no script, so Tab moves between the triggers.
- **A single accordion that cannot be closed** (shadcn/ui's single without `collapsible`): the browser always lets the
  open `details` close.
- **`value`, `onValueChange` and controlled state**: the server renders the open items with `open`; the browser's
  `toggle` event reports changes.
- **`dir` and `orientation`**: the accordion follows the page's direction, and is vertical.
- **`role="region"` on the content.** Radix gives every panel one; the ARIA pattern advises against it where more than
  about six panels can be open at once, as landmarks pile up. The content is inside the `details`, which already
  groups it with its trigger.

## Attributes

### sl:accordion

Attributes of `<sl:accordion>`: A stack of sections, each with a heading that shows or hides its content. Built on the browser's `details` element, so it needs no JavaScript: the keyboard, the open state, and find-in-page opening a section that holds a match all come from the browser. Holds `sl:accordion-item`s, each with a `sl:accordion-trigger` and a `sl:accordion-content`.

| Attribute | Values | Default | Description |
| --- | --- | --- | --- |
| `type` | `single`, `multiple` | `single` | `single`: opening a section closes the one that was open (the items share a `details` `name`). `multiple`: sections open and close on their own. |
| `name` | *string* | — | The group name the items of a `single` accordion share. Must be unique on the page. Without it, each render generates one (`sl-accordion-` and eight random characters). |

Every prop can also be set from an expression: `th:type="${...}"`. Any other attribute passes through to the rendered element, and `class` is appended.

### sl:accordion-item

Attributes of `<sl:accordion-item>`: One section of a `sl:accordion`: a `details` element holding a `sl:accordion-trigger` (first) and a `sl:accordion-content`.

| Attribute | Values | Default | Description |
| --- | --- | --- | --- |
| `open` | *boolean* | — | Render the section open. In a `single` accordion only the first open section stays open. |
| `disabled` | *boolean* | — | The section cannot be opened or closed: its trigger is out of the tab order and ignores the pointer, and a closed disabled section renders no content, so find-in-page cannot open it. |

Every prop can also be set from an expression: `th:open="${...}"`. Any other attribute passes through to the rendered element, and `class` is appended.

### sl:accordion-trigger

Attributes of `<sl:accordion-trigger>`: The heading of a `sl:accordion-item`, which opens and closes it: the `details` element's `summary`, so it must be the item's first child. Its text is a heading inside the `summary`, and a chevron follows it.

| Attribute | Values | Default | Description |
| --- | --- | --- | --- |
| `level` | `1`, `2`, `3`, `4`, `5`, `6` | `3` | Heading level of the text (`role="heading"` and `aria-level`), as shadcn/ui's trigger sits in an `h3`. Pick the level that fits the page's outline. |

Every prop can also be set from an expression: `th:level="${...}"`. Any other attribute passes through to the rendered element, and `class` is appended.

### sl:accordion-content

Attributes of `<sl:accordion-content>`: The content of a `sl:accordion-item`, shown while it is open. Find-in-page searches it while it is closed, and opens the section on a match.

`sl:accordion-content` declares no props; every attribute passes through.
