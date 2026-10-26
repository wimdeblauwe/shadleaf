# Combobox

> shadcn/ui's Combobox on a real select element, bound with th:field; type to filter, choose one value or several as chips, and load the options from the server as you type.

Source: https://shadleaf.dev/current/components/combobox/

**Example: Combobox.** Type to filter the list; th:field marks the bound item.

```html
<sl:field th:field="*{framework}">
  <sl:field-label>Framework</sl:field-label>
  <sl:combobox placeholder="Select a framework">
    <sl:combobox-item value="next">Next.js</sl:combobox-item>
    <sl:combobox-item value="sveltekit">SvelteKit</sl:combobox-item>
    <sl:combobox-item value="nuxt">Nuxt.js</sl:combobox-item>
    <sl:combobox-item value="remix">Remix</sl:combobox-item>
    <sl:combobox-item value="astro">Astro</sl:combobox-item>
  </sl:combobox>
</sl:field>
```

## Built from parts

A combobox is `<sl:combobox>` holding `<sl:combobox-item>`s, optionally in `<sl:combobox-group>`s with
`<sl:combobox-separator>`s between them. Like [`<sl:select>`](https://shadleaf.dev/current/components/select.md), it renders a real `<select>` with
`<option>`, `<optgroup>` and `<hr>` elements, so `th:field` binds it as it binds any select and the form submits the
chosen value. Once Alpine runs, a text input and a list box take the select's place; typing filters the list.

```html
<form th:object="${project}" th:action="@{/projects}" method="post">
  <sl:field th:field="*{framework}">
    <sl:field-label>Framework</sl:field-label>
    <sl:combobox placeholder="Select a framework">
      <sl:combobox-item th:each="framework : ${frameworks}"
                        th:value="${framework.id}">[[${framework.name}]]</sl:combobox-item>
    </sl:combobox>
    <sl:field-error/>
  </sl:field>
</form>
```

Give an item its text as content (`[[${framework.name}]]`): the input, the chips and the filter use it.

## Combobox or select?

- **[`<sl:select>`](https://shadleaf.dev/current/components/select.md)** for a short list people scan: a few to a few dozen items.
- **`<sl:combobox>`** for a list people would rather search: many items, items whose names people know, or several
  values at once (`multiple`). With `search-url`, the options can come from the server, for lists too long to put in
  the page.
- **[`<sl:native-select>`](https://shadleaf.dev/current/components/select-native.md)** to leave the list to the operating system, as phones show it.

## Multiple

`multiple` lets people choose any number of items. The chosen ones are chips before the input, each with a button
that removes it; the list stays open while choosing, with a check mark on every chosen item. The select is a
`<select multiple>`, so `th:field` binds a collection (`List<String>`, `Set<Long>`, ...) and the form submits one
value per chosen item. Spring's hidden marker input comes with it, so choosing none binds an empty collection.

**Example: Multiple.** With multiple, the chosen items are chips before the input and th:field binds a collection.

```html
<sl:field th:field="*{frameworks}">
  <sl:field-label>Frameworks</sl:field-label>
  <sl:combobox multiple auto-highlight placeholder="Add a framework">
    <sl:combobox-item value="next">Next.js</sl:combobox-item>
    <sl:combobox-item value="sveltekit">SvelteKit</sl:combobox-item>
    <sl:combobox-item value="nuxt">Nuxt.js</sl:combobox-item>
    <sl:combobox-item value="remix">Remix</sl:combobox-item>
    <sl:combobox-item value="astro">Astro</sl:combobox-item>
  </sl:combobox>
</sl:field>
```

## Placeholder and clearing

`placeholder` is the input's text while nothing is chosen. For a single value it is also a first option with an empty
value, hidden from the list, which is chosen while the field has no value: a `required` combobox with nothing chosen
is invalid, as for a select.

Emptying the input and leaving it clears the choice. `show-clear` adds a button that does the same, in place of the
chevron while there is something to clear; in a multiple combobox it removes every chip.

**Example: Clear button.** show-clear puts a button that clears the choice in place of the chevron while there is one.

```html
<sl:field th:field="*{stack}">
  <sl:field-label>Stack</sl:field-label>
  <sl:combobox placeholder="Select a framework" show-clear>
    <sl:combobox-item value="next">Next.js</sl:combobox-item>
    <sl:combobox-item value="sveltekit">SvelteKit</sl:combobox-item>
    <sl:combobox-item value="nuxt">Nuxt.js</sl:combobox-item>
  </sl:combobox>
</sl:field>
```

## Groups, separators and icons

`<sl:combobox-group label="...">` is an `<optgroup>`: its label shows above its items, and hides while none of them
match what was typed. `<sl:combobox-separator/>` is an `<hr>`, hidden while typing.

**Example: Groups and separators.** Some 80 time zones, too many to scan, so you type a city instead (bru finds Brussels). A group's heading hides while none of its items match; separators hide while you type.

```html
<sl:field th:field="*{meetingZone}">
  <sl:field-label>Timezone</sl:field-label>
  <sl:combobox placeholder="Select a timezone">
    <sl:combobox-group label="Americas">
      <sl:combobox-item value="Pacific/Honolulu">(GMT-10) Honolulu</sl:combobox-item>
      <sl:combobox-item value="America/Anchorage">(GMT-9) Anchorage</sl:combobox-item>
      <sl:combobox-item value="America/Los_Angeles">(GMT-8) Los Angeles</sl:combobox-item>
      <sl:combobox-item value="America/Tijuana">(GMT-8) Tijuana</sl:combobox-item>
      <sl:combobox-item value="America/Vancouver">(GMT-8) Vancouver</sl:combobox-item>
      <sl:combobox-item value="America/Denver">(GMT-7) Denver</sl:combobox-item>
      <sl:combobox-item value="America/Edmonton">(GMT-7) Edmonton</sl:combobox-item>
      <sl:combobox-item value="America/Phoenix">(GMT-7) Phoenix</sl:combobox-item>
      <sl:combobox-item value="America/Chicago">(GMT-6) Chicago</sl:combobox-item>
      <sl:combobox-item value="America/Guatemala">(GMT-6) Guatemala</sl:combobox-item>
      <sl:combobox-item value="America/Mexico_City">(GMT-6) Mexico City</sl:combobox-item>
      <sl:combobox-item value="America/Winnipeg">(GMT-6) Winnipeg</sl:combobox-item>
      <sl:combobox-item value="America/Bogota">(GMT-5) Bogota</sl:combobox-item>
      <sl:combobox-item value="America/Havana">(GMT-5) Havana</sl:combobox-item>
      <sl:combobox-item value="America/Lima">(GMT-5) Lima</sl:combobox-item>
      <sl:combobox-item value="America/New_York">(GMT-5) New York</sl:combobox-item>
      <sl:combobox-item value="America/Toronto">(GMT-5) Toronto</sl:combobox-item>
      <sl:combobox-item value="America/Caracas">(GMT-4) Caracas</sl:combobox-item>
      <sl:combobox-item value="America/Halifax">(GMT-4) Halifax</sl:combobox-item>
      <sl:combobox-item value="America/La_Paz">(GMT-4) La Paz</sl:combobox-item>
      <sl:combobox-item value="America/Santiago">(GMT-4) Santiago</sl:combobox-item>
      <sl:combobox-item value="America/St_Johns">(GMT-3:30) St. John's</sl:combobox-item>
      <sl:combobox-item value="America/Argentina/Buenos_Aires">(GMT-3) Buenos Aires</sl:combobox-item>
      <sl:combobox-item value="America/Montevideo">(GMT-3) Montevideo</sl:combobox-item>
      <sl:combobox-item value="America/Sao_Paulo">(GMT-3) São Paulo</sl:combobox-item>
    </sl:combobox-group>
    <sl:combobox-separator/>
    <sl:combobox-group label="Europe">
      <sl:combobox-item value="Europe/Dublin">(GMT+0) Dublin</sl:combobox-item>
      <sl:combobox-item value="Europe/Lisbon">(GMT+0) Lisbon</sl:combobox-item>
      <sl:combobox-item value="Europe/London">(GMT+0) London</sl:combobox-item>
      <sl:combobox-item value="Atlantic/Reykjavik">(GMT+0) Reykjavik</sl:combobox-item>
      <sl:combobox-item value="Europe/Amsterdam">(GMT+1) Amsterdam</sl:combobox-item>
      <sl:combobox-item value="Europe/Berlin">(GMT+1) Berlin</sl:combobox-item>
      <sl:combobox-item value="Europe/Brussels">(GMT+1) Brussels</sl:combobox-item>
      <sl:combobox-item value="Europe/Budapest">(GMT+1) Budapest</sl:combobox-item>
      <sl:combobox-item value="Europe/Copenhagen">(GMT+1) Copenhagen</sl:combobox-item>
      <sl:combobox-item value="Europe/Madrid">(GMT+1) Madrid</sl:combobox-item>
      <sl:combobox-item value="Europe/Oslo">(GMT+1) Oslo</sl:combobox-item>
      <sl:combobox-item value="Europe/Paris">(GMT+1) Paris</sl:combobox-item>
      <sl:combobox-item value="Europe/Prague">(GMT+1) Prague</sl:combobox-item>
      <sl:combobox-item value="Europe/Rome">(GMT+1) Rome</sl:combobox-item>
      <sl:combobox-item value="Europe/Stockholm">(GMT+1) Stockholm</sl:combobox-item>
      <sl:combobox-item value="Europe/Vienna">(GMT+1) Vienna</sl:combobox-item>
      <sl:combobox-item value="Europe/Warsaw">(GMT+1) Warsaw</sl:combobox-item>
      <sl:combobox-item value="Europe/Zurich">(GMT+1) Zurich</sl:combobox-item>
      <sl:combobox-item value="Europe/Athens">(GMT+2) Athens</sl:combobox-item>
      <sl:combobox-item value="Europe/Bucharest">(GMT+2) Bucharest</sl:combobox-item>
      <sl:combobox-item value="Europe/Helsinki">(GMT+2) Helsinki</sl:combobox-item>
      <sl:combobox-item value="Europe/Kyiv">(GMT+2) Kyiv</sl:combobox-item>
      <sl:combobox-item value="Europe/Istanbul">(GMT+3) Istanbul</sl:combobox-item>
      <sl:combobox-item value="Europe/Moscow">(GMT+3) Moscow</sl:combobox-item>
    </sl:combobox-group>
    <sl:combobox-separator/>
    <sl:combobox-group label="Africa">
      <sl:combobox-item value="Africa/Accra">(GMT+0) Accra</sl:combobox-item>
      <sl:combobox-item value="Africa/Casablanca">(GMT+0) Casablanca</sl:combobox-item>
      <sl:combobox-item value="Africa/Algiers">(GMT+1) Algiers</sl:combobox-item>
      <sl:combobox-item value="Africa/Kinshasa">(GMT+1) Kinshasa</sl:combobox-item>
      <sl:combobox-item value="Africa/Lagos">(GMT+1) Lagos</sl:combobox-item>
      <sl:combobox-item value="Africa/Cairo">(GMT+2) Cairo</sl:combobox-item>
      <sl:combobox-item value="Africa/Johannesburg">(GMT+2) Johannesburg</sl:combobox-item>
      <sl:combobox-item value="Africa/Addis_Ababa">(GMT+3) Addis Ababa</sl:combobox-item>
      <sl:combobox-item value="Africa/Nairobi">(GMT+3) Nairobi</sl:combobox-item>
    </sl:combobox-group>
    <sl:combobox-separator/>
    <sl:combobox-group label="Asia">
      <sl:combobox-item value="Asia/Jerusalem">(GMT+2) Jerusalem</sl:combobox-item>
      <sl:combobox-item value="Asia/Riyadh">(GMT+3) Riyadh</sl:combobox-item>
      <sl:combobox-item value="Asia/Tehran">(GMT+3:30) Tehran</sl:combobox-item>
      <sl:combobox-item value="Asia/Dubai">(GMT+4) Dubai</sl:combobox-item>
      <sl:combobox-item value="Asia/Karachi">(GMT+5) Karachi</sl:combobox-item>
      <sl:combobox-item value="Asia/Kolkata">(GMT+5:30) Kolkata</sl:combobox-item>
      <sl:combobox-item value="Asia/Kathmandu">(GMT+5:45) Kathmandu</sl:combobox-item>
      <sl:combobox-item value="Asia/Dhaka">(GMT+6) Dhaka</sl:combobox-item>
      <sl:combobox-item value="Asia/Bangkok">(GMT+7) Bangkok</sl:combobox-item>
      <sl:combobox-item value="Asia/Ho_Chi_Minh">(GMT+7) Ho Chi Minh City</sl:combobox-item>
      <sl:combobox-item value="Asia/Jakarta">(GMT+7) Jakarta</sl:combobox-item>
      <sl:combobox-item value="Asia/Hong_Kong">(GMT+8) Hong Kong</sl:combobox-item>
      <sl:combobox-item value="Asia/Manila">(GMT+8) Manila</sl:combobox-item>
      <sl:combobox-item value="Asia/Shanghai">(GMT+8) Shanghai</sl:combobox-item>
      <sl:combobox-item value="Asia/Singapore">(GMT+8) Singapore</sl:combobox-item>
      <sl:combobox-item value="Asia/Taipei">(GMT+8) Taipei</sl:combobox-item>
      <sl:combobox-item value="Asia/Seoul">(GMT+9) Seoul</sl:combobox-item>
      <sl:combobox-item value="Asia/Tokyo">(GMT+9) Tokyo</sl:combobox-item>
    </sl:combobox-group>
    <sl:combobox-separator/>
    <sl:combobox-group label="Pacific">
      <sl:combobox-item value="Australia/Perth">(GMT+8) Perth</sl:combobox-item>
      <sl:combobox-item value="Australia/Adelaide">(GMT+9:30) Adelaide</sl:combobox-item>
      <sl:combobox-item value="Australia/Brisbane">(GMT+10) Brisbane</sl:combobox-item>
      <sl:combobox-item value="Australia/Melbourne">(GMT+10) Melbourne</sl:combobox-item>
      <sl:combobox-item value="Pacific/Port_Moresby">(GMT+10) Port Moresby</sl:combobox-item>
      <sl:combobox-item value="Australia/Sydney">(GMT+10) Sydney</sl:combobox-item>
      <sl:combobox-item value="Pacific/Auckland">(GMT+12) Auckland</sl:combobox-item>
      <sl:combobox-item value="Pacific/Fiji">(GMT+12) Fiji</sl:combobox-item>
    </sl:combobox-group>
  </sl:combobox>
</sl:field>
```

An item may start with an [icon](https://shadleaf.dev/current/components/icon.md). The list shows it; the input and the chips show the item's text.

**Example: Custom items.** An item may start with an icon. The input and the chips show its text.

```html
<sl:field th:field="*{issueStatus}">
  <sl:field-label>Status</sl:field-label>
  <sl:combobox placeholder="Set a status">
    <sl:combobox-item value="backlog"><sl:icon name="circle-dashed"/>Backlog</sl:combobox-item>
    <sl:combobox-item value="todo"><sl:icon name="circle"/>Todo</sl:combobox-item>
    <sl:combobox-item value="in-progress"><sl:icon name="circle-dot"/>In progress</sl:combobox-item>
    <sl:combobox-item value="done"><sl:icon name="circle-check"/>Done</sl:combobox-item>
  </sl:combobox>
</sl:field>
```

## Filtering and auto-highlight

The list shows the items whose text contains what was typed, ignoring case and accents (`turk` finds Türkiye). By
default nothing is highlighted while typing, so Enter does not choose by accident; `auto-highlight` highlights the
first match, so typing a few letters and pressing Enter chooses it. Long lists scroll; the list never grows taller
than the window.

**Example: Long lists.** The list scrolls when it is taller than the window; auto-highlight makes Enter choose the first match.

```html
<sl:field th:field="*{destination}">
  <sl:field-label>Country</sl:field-label>
  <sl:combobox placeholder="Select a country" auto-highlight>
    <sl:combobox-item value="argentina">Argentina</sl:combobox-item>
    <sl:combobox-item value="australia">Australia</sl:combobox-item>
    <sl:combobox-item value="austria">Austria</sl:combobox-item>
    <sl:combobox-item value="belgium">Belgium</sl:combobox-item>
    <sl:combobox-item value="brazil">Brazil</sl:combobox-item>
    <sl:combobox-item value="canada">Canada</sl:combobox-item>
    <sl:combobox-item value="chile">Chile</sl:combobox-item>
    <sl:combobox-item value="china">China</sl:combobox-item>
    <sl:combobox-item value="colombia">Colombia</sl:combobox-item>
    <sl:combobox-item value="czechia">Czechia</sl:combobox-item>
    <sl:combobox-item value="denmark">Denmark</sl:combobox-item>
    <sl:combobox-item value="egypt">Egypt</sl:combobox-item>
    <sl:combobox-item value="finland">Finland</sl:combobox-item>
    <sl:combobox-item value="france">France</sl:combobox-item>
    <sl:combobox-item value="germany">Germany</sl:combobox-item>
    <sl:combobox-item value="greece">Greece</sl:combobox-item>
    <sl:combobox-item value="india">India</sl:combobox-item>
    <sl:combobox-item value="indonesia">Indonesia</sl:combobox-item>
    <sl:combobox-item value="ireland">Ireland</sl:combobox-item>
    <sl:combobox-item value="italy">Italy</sl:combobox-item>
    <sl:combobox-item value="japan">Japan</sl:combobox-item>
    <sl:combobox-item value="kenya">Kenya</sl:combobox-item>
    <sl:combobox-item value="mexico">Mexico</sl:combobox-item>
    <sl:combobox-item value="morocco">Morocco</sl:combobox-item>
    <sl:combobox-item value="netherlands">Netherlands</sl:combobox-item>
    <sl:combobox-item value="new-zealand">New Zealand</sl:combobox-item>
    <sl:combobox-item value="nigeria">Nigeria</sl:combobox-item>
    <sl:combobox-item value="norway">Norway</sl:combobox-item>
    <sl:combobox-item value="peru">Peru</sl:combobox-item>
    <sl:combobox-item value="poland">Poland</sl:combobox-item>
    <sl:combobox-item value="portugal">Portugal</sl:combobox-item>
    <sl:combobox-item value="south-africa">South Africa</sl:combobox-item>
    <sl:combobox-item value="south-korea">South Korea</sl:combobox-item>
    <sl:combobox-item value="spain">Spain</sl:combobox-item>
    <sl:combobox-item value="sweden">Sweden</sl:combobox-item>
    <sl:combobox-item value="switzerland">Switzerland</sl:combobox-item>
    <sl:combobox-item value="thailand">Thailand</sl:combobox-item>
    <sl:combobox-item value="turkiye">Türkiye</sl:combobox-item>
    <sl:combobox-item value="united-kingdom">United Kingdom</sl:combobox-item>
    <sl:combobox-item value="united-states">United States</sl:combobox-item>
    <sl:combobox-item value="vietnam">Vietnam</sl:combobox-item>
  </sl:combobox>
</sl:field>
```

## Options from the server

`search-url` makes the combobox ask the server for the options as people type, for lists too long to put in the page.
300 ms after the last key, it sends a `GET` to that URL with the text as `q`, and expects the HTML of
`<sl:combobox-item>`s (and groups and separators, if you like) back. A Thymeleaf fragment renders them:

```html
<sl:combobox th:field="*{country}" placeholder="Search a country" th:search-url="@{/countries/search}">
  <!-- The chosen country, which the controller looked up: the combobox shows it before anyone types. -->
  <sl:combobox-item th:if="${chosenCountry != null}"
                    th:value="${chosenCountry.code}">[[${chosenCountry.name}]]</sl:combobox-item>
</sl:combobox>
```

```java
@GetMapping("/countries/search")
String searchCountries(@RequestParam String q, Model model) {
  model.addAttribute("countries", countryService.search(q, 20));
  return "countries :: options";
}
```

```html
<!-- countries.html -->
<th:block th:fragment="options">
  <sl:combobox-item th:each="country : ${countries}"
                    th:value="${country.code}">[[${country.name}]]</sl:combobox-item>
</th:block>
```

- **The answer replaces the options** that are not chosen. A chosen option stays, so the value does not change; when
  the answer leaves it out, it stays hidden from the list. Render the chosen option(s) in the page, as above, so the
  combobox can show and submit them before anyone types.
- **The server filters.** The browser shows what the answer holds, without filtering it again; an empty answer shows
  "No items found." (or `empty`). Limit the number of results on the server.
- **An empty input** brings back the options the page came with: render a first few there, or none.
- **A newer query aborts the request before it**, so a slow answer never replaces a newer one. While waiting, the list
  says "Searching…"; an answer that is not a 2xx, or that was redirected (a session that expired and sent the request
  to the login page), shows "The options could not be loaded."
- **To add parameters**, such as another field's value for options that depend on it, listen for
  `sl-combobox-search` on the select. It fires before each request, with `detail.query` and `detail.url`, a `URL`
  whose `searchParams` you can change; `preventDefault()` skips the request.

```js
document.addEventListener('sl-combobox-search', event => {
  if (event.target.id === 'city') {
    event.detail.url.searchParams.set('country', document.getElementById('country').value);
  }
});
```

Searching needs JavaScript. Without it, the combobox is a select with the options in the page, so only those can be
chosen.

## Width

The combobox sits in a wrapper that also holds the input and the list. `class` goes to that wrapper, so
`class="w-full"` widens it; every other attribute goes to the `<select>`. The list is at least as wide as the input.

## In a form

- **`th:field`**, on the combobox or on its [field](https://shadleaf.dev/current/components/field.md), binds it. In a field, the select takes the field's `id`
  (the label points at it) and `aria-describedby`; the input takes them over from the select, so the label names the
  input and the description and error describe it.
- **Errors.** A field with errors marks the combobox `aria-invalid="true"`, and
  [`<sl:form-errors autofocus/>`](https://shadleaf.dev/current/components/form-errors.md) gives it `autofocus` when it is the first control with errors; the
  focus goes on to the input.
- **No free text.** Only the options can be chosen: what was typed without choosing is not submitted, and the input
  shows the choice again when it loses the focus. For a text field with suggestions, use an
  [input](https://shadleaf.dev/current/components/input.md) with a `<datalist>`.
- **Events.** Choosing fires `input` and `change` on the `<select>`, so `hx-trigger="change"` and your own listeners
  work as for any select. A script that changes the select's options or value should fire `change` itself; the input
  and the chips follow.
- **Form reset** puts the combobox back to its rendered value.

**Example: With errors.** In a field with errors, the combobox is aria-invalid and described by the message.

```html
<sl:field th:field="*{language}">
  <sl:field-label>Language</sl:field-label>
  <sl:combobox placeholder="Select a language">
    <sl:combobox-item value="java">Java</sl:combobox-item>
    <sl:combobox-item value="kotlin">Kotlin</sl:combobox-item>
    <sl:combobox-item value="groovy">Groovy</sl:combobox-item>
  </sl:combobox>
  <sl:field-error/>
</sl:field>
<sl:field th:field="*{languages}">
  <sl:field-label>Other languages</sl:field-label>
  <sl:combobox multiple placeholder="Add a language">
    <sl:combobox-item value="java">Java</sl:combobox-item>
    <sl:combobox-item value="kotlin">Kotlin</sl:combobox-item>
    <sl:combobox-item value="groovy">Groovy</sl:combobox-item>
  </sl:combobox>
  <sl:field-error/>
</sl:field>
```

## Disabled

`disabled` on the combobox, or on a `<sl:field-set>` around it, disables it: it fades with its chips. `disabled` on an
item leaves it in the list, faded; the keyboard and the pointer skip it.

**Example: Disabled.** A disabled combobox, and disabled items that the keyboard and the pointer skip.

```html
<sl:field th:field="*{runtime}">
  <sl:field-label>Runtime</sl:field-label>
  <sl:combobox disabled>
    <sl:combobox-item value="jvm">JVM</sl:combobox-item>
  </sl:combobox>
</sl:field>
<sl:field th:field="*{plugins}">
  <sl:field-label>Plugins</sl:field-label>
  <sl:combobox multiple disabled>
    <sl:combobox-item value="lombok">Lombok</sl:combobox-item>
    <sl:combobox-item value="mapstruct">MapStruct</sl:combobox-item>
  </sl:combobox>
</sl:field>
<sl:field th:field="*{build}">
  <sl:field-label>Build tool</sl:field-label>
  <sl:combobox placeholder="Select a build tool">
    <sl:combobox-item value="maven">Maven</sl:combobox-item>
    <sl:combobox-item value="ant" disabled>Ant</sl:combobox-item>
    <sl:combobox-item value="gradle">Gradle</sl:combobox-item>
  </sl:combobox>
</sl:field>
```

## Keyboard

The focus stays in the input; the highlighted item is announced through `aria-activedescendant`, as the
[ARIA combobox pattern](https://www.w3.org/WAI/ARIA/apg/patterns/combobox/examples/combobox-autocomplete-list/)
describes.

| Key | Does |
| --- | --- |
| Typing | Opens the list and filters it |
| Arrow down, arrow up | Opens the list on the chosen item; next, previous item |
| Alt + arrow down | Opens the list without highlighting |
| Page down, page up | Ten items further, back |
| Enter | Chooses the highlighted item; in a multiple combobox, adds or removes it and keeps the list open |
| Alt + arrow up | Chooses the highlighted item and closes the list |
| Escape | Closes the list, putting back the text of the choice |
| Tab | Closes the list without choosing and moves on |
| Home, End | Move the caret, as in any text field |
| Backspace (multiple, empty input) | Removes the last chip |
| Arrow left (multiple, at the start of the input) | Moves to the chips' remove buttons: arrow left and right between them, Backspace or Delete removes one |

The number of matches is announced once typing pauses ("3 results"; the texts are messages, `sl.combobox.*`, see
[Translating](https://shadleaf.dev/current/guides/translating.md)). The clear and chevron buttons stay out of the tab order: each
has a key. The item under the pointer is highlighted.

## With htmx

- A response that renders the form again with errors keeps the chosen values (`th:field` marks them).
- A swap that replaces an open combobox leaves nothing open behind.
- Back restores the combobox as htmx saved it; it builds its input and list again from the restored select.
- A combobox can trigger a request itself: `hx-get="/cities" hx-trigger="change" hx-target="#city-field"`. Options
  swapped into the select are picked up by the list.

## Forced colours

The input and the list keep a visible border, the highlighted item an outline and the chips a border, when the
system's colours replace the page's (Windows High Contrast). A disabled combobox turns `GrayText` instead of fading.

## Left out

These shadcn/Base UI parts and props have no counterpart, on purpose:

- **`ComboboxInput`, `ComboboxContent`, `ComboboxList`, `ComboboxChips`, `ComboboxChip`, `ComboboxChipsInput`,
  `ComboboxValue` and `ComboboxTrigger`.** `<sl:combobox>` draws them all: `showClear`, `showTrigger` and
  `autoHighlight` are its `show-clear`, `show-trigger` and `auto-highlight`.
- **`items` and the render function.** Items are written as `<sl:combobox-item>`s (with `th:each`): `value` and their
  text take the place of `itemToStringValue`.
- **`ComboboxEmpty`** is the `empty` attribute; **`ComboboxLabel`** is a group's `label`, and there is no
  `ComboboxCollection`.
- **The popup layout**, with a button as the trigger and the input inside the list, and **addons** in the input.
- **Free text**, which Base UI's Combobox does not allow either (its Autocomplete does).
- **Removing chips is always possible** (`showRemove`), and a chip shows the item's text.
- **`side`, `align`**: the list opens below the input, or above it when there is no room.
- **State in the browser** (`value`/`defaultValue`/`onValueChange`, `open`/`onOpenChange`): the server renders the
  value with `th:field`, and the select's `change` event reports a new one.

## Without JavaScript

The server renders a real select, drawn like the [native select](https://shadleaf.dev/current/components/select-native.md): a single combobox is a select
with a chevron, a multiple one a list box (Ctrl- or Cmd-click chooses several). Both bind, submit and validate as any
select. With Alpine, `slCombobox` puts its input and list in the select's place.

## Attributes

### sl:combobox

Attributes of `<sl:combobox>`: A text input that filters a list of options as you type: shadcn/ui's combobox. Like `sl:select`, it is a real `select` with `sl:combobox-item`, `sl:combobox-group` and `sl:combobox-separator` as content, so `th:field` binds it (a collection with `multiple`) and the form submits it. Once Alpine runs, `slCombobox` hides the select and puts an input and a list box in its place; without it, the select is drawn like `sl:native-select`. Every attribute that is not a prop passes through to the `select`, except `class`, which goes to the wrapper around it. In a `sl:field`, it takes the field's `th:field`, id and `aria-describedby`.

| Attribute | Values | Default | Description |
| --- | --- | --- | --- |
| `placeholder` | *string* | — | The input's text while nothing is chosen. For a single value also the text of a first option with an empty value, hidden from the list: a required combobox with it chosen is invalid. |
| `multiple` | *boolean* | — | Choose any number of options, shown as chips before the input. The select is a `select multiple`, so `th:field` binds a collection. |
| `show-clear` | *boolean* | — | A button that clears the choice, in place of the chevron while there is one. |
| `show-trigger` | *boolean* | `true` | The chevron button that opens and closes the list. A multiple combobox has none. |
| `auto-highlight` | *boolean* | — | Highlight the first match while typing, so Enter chooses it. |
| `empty` | *string* | — | The text shown when no option matches. By default "No items found." |
| `search-url` | *string* | — | Ask the server for the options as you type: a `GET` to this URL with the text as `q`, answered with the HTML of `sl:combobox-item`s (and groups and separators), which replace the options that are not chosen. Needs JavaScript; without it, only the options in the page can be chosen. |

Every prop can also be set from an expression: `th:placeholder="${...}"`. Any other attribute passes through to the rendered element, and `class` is appended.

### sl:combobox-item

Attributes of `<sl:combobox-item>`: An option in a `sl:combobox`: an `option`. `value` (or `th:value`) and `disabled` pass through; `th:field` on the combobox marks the bound one. The list shows its content, which may start with an icon, with a check mark while it is chosen; the input, the chips and the filter use its text, which is all an older browser keeps of it.

`sl:combobox-item` declares no props; every attribute passes through.

### sl:combobox-group

Attributes of `<sl:combobox-group>`: A group of `sl:combobox-item`s under a heading: an `optgroup`. The list hides the heading while none of its items match.

| Attribute | Values | Default | Description |
| --- | --- | --- | --- |
| `label` | *string* | **required** | The group's heading, shadcn/ui's `ComboboxLabel`. |

Every prop can also be set from an expression: `th:label="${...}"`. Any other attribute passes through to the rendered element, and `class` is appended.

### sl:combobox-separator

Attributes of `<sl:combobox-separator>`: A line between items or groups of a `sl:combobox`: an `hr`. The list hides it while you type.

`sl:combobox-separator` declares no props; every attribute passes through.

## JavaScript

A small Alpine component, `slCombobox`, sits on the wrapper: `<div class="combobox" x-data="slCombobox">`.

- **It hides the `<select>`** (it stays in the form, and still submits, resets and validates) and adds what the
  wrapper's `<template>` holds after it: a `<div class="combobox-control">` with an
  `<input role="combobox" class="combobox-input">` (and the chips, the clear and the chevron buttons), and a
  `<div class="combobox-content" popover="manual">` with the `<div role="listbox" class="combobox-list">`. The input
  takes the select's label (its `<label>`s, `aria-labelledby` or `aria-label`), `aria-describedby`, `aria-invalid`,
  `required` and `disabled`, and follows them when they change. Focus that reaches the hidden select moves on to the
  input.
- **The list is a popover** in the top layer, so it shows above dialogs and sheets, placed with anchor positioning
  (with a stand-in for browsers without it). Escape in the input closes the list, not the dialog around it.
- **It works with Alpine's CSP build** and a strict Content Security Policy: no inline styles, no `eval`. A search
  request is a `fetch` to the same origin (`connect-src 'self'`).
- **Keep your own `x-data` off `<sl:combobox>`**: `class` is the only attribute that reaches the wrapper. Listen for
  `change` on the select instead.
