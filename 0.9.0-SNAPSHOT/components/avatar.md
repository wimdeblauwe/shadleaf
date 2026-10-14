# Avatar

> A picture of a person, with initials when there is no picture.

Source: https://wimdeblauwe.github.io/shadleaf/0.9.0-SNAPSHOT/components/avatar/

**Example: Avatar.**

```html
<sl:avatar label="shadcn">
  <sl:avatar-image th:src="@{/users/{id}/photo(id=${user.id})}"/>
  <sl:avatar-fallback>CN</sl:avatar-fallback>
</sl:avatar>
```

## Image and fallback

Write both parts, as in shadcn/ui: `<sl:avatar-image>` with the picture's `src` or `th:src`, and
`<sl:avatar-fallback>` with what to show without one, usually initials or an [`<sl:icon>`](https://wimdeblauwe.github.io/shadleaf/0.9.0-SNAPSHOT/components/icon.md). The avatar shows
the fallback when the image has no `src` or an empty one. A `th:src` whose value is null renders an empty `src`, so a
user without a photo needs no `th:if`:

**Example: Fallback.** A user without a photo gets their initials; th:src with a null value renders an empty src, which shows the fallback.

```html
<sl:avatar label="Jane Doe">
  <sl:avatar-image th:src="${user.photoUrl}"/>
  <sl:avatar-fallback>JD</sl:avatar-fallback>
</sl:avatar>
<sl:avatar label="Unknown user">
  <sl:avatar-fallback><sl:icon name="user"/></sl:avatar-fallback>
</sl:avatar>
```

Both parts are always rendered, and the stylesheet decides which one shows. The hidden one is also hidden from
assistive technology. The image lies over the fallback at the same size, so nothing moves when one replaces the other.

## A photo that fails to load

A photo URL that fails to load shows the fallback too: a 404, a URL your Content-Security-Policy's `img-src` blocks, a
file that is no image, or no network. So does a photo that is still loading, as in shadcn/ui; the picture replaces the
fallback once it is there.

**Example: Broken image.** A photo URL that fails to load (a 404, a URL the Content-Security-Policy blocks, offline) shows the fallback, as does one that is still loading.

```html
<sl:avatar label="Grace Hopper">
  <sl:avatar-image th:src="@{/users/{id}/photo(id=${user.id})}"/>
  <sl:avatar-fallback>GH</sl:avatar-fallback>
</sl:avatar>
```

Shadleaf's script marks each avatar image with `data-status="loaded"` when its picture has loaded, or
`data-status="error"`, and the stylesheet only shows an image that is marked loaded. So a broken image never shows, not
even for a moment before the script runs. The script also marks images htmx swaps in, and an image whose `src` changes
later (htmx swapping the attribute, or a script of yours): the fallback shows while the new picture loads.

Your own CSS can use the mark, for instance to fade the picture in:

```css
@media (prefers-reduced-motion: no-preference) {
  .avatar-image[data-status='loaded'] {
    animation: avatar-fade-in 150ms;
  }
}

@keyframes avatar-fade-in {
  from { opacity: 0; }
}
```

### Without the script

- **Without JavaScript**, nothing marks the images, so they lie over the fallback as plain images: a picture that loads
  covers it. One that fails shows the fallback through it in Firefox, which draws nothing for a broken image with an
  empty `alt`; Chromium draws its broken-image icon over it.
- **With JavaScript but without Shadleaf's script** (your policy blocks it, say), every avatar shows its fallback.

The script is part of the one in the `assets` fragment, whatever `shadleaf.assets.alpine` is: it is not an Alpine
component, and works without Alpine on the page. One listener on the document handles every avatar, so a long list of
them costs nothing extra.

## Sizes

**Example: Sizes.**

```html
<sl:avatar size="sm" label="Jane Doe"><sl:avatar-fallback>JD</sl:avatar-fallback></sl:avatar>
<sl:avatar label="Jane Doe"><sl:avatar-fallback>JD</sl:avatar-fallback></sl:avatar>
<sl:avatar size="lg" label="Jane Doe"><sl:avatar-fallback>JD</sl:avatar-fallback></sl:avatar>
```

## Naming the avatar

An avatar usually sits next to the person's name, so it is decorative by default and hidden from assistive technology.
When there is no name next to it, give it a `label`: it is then announced as an image with that name, whichever part
shows. `aria-labelledby`, pointing at a name elsewhere on the page, works too.

**Example: Next to a name.** Without a label the avatar is decorative; the name beside it is what a screen reader reads.

```html
<sl:avatar><sl:avatar-fallback>JD</sl:avatar-fallback></sl:avatar>
<span>Jane Doe</span>
```

## Left out

- **A delay before the fallback shows** (Radix's `delayMs`): the fallback shows at once while a photo loads. A photo
  from the browser's cache is there before the first paint, so it does not flash.
- **A loading status event** (Radix's `onLoadingStatusChange`): read `data-status` on the image instead.

## Attributes

### sl:avatar

Attributes of `<sl:avatar>`: A picture of a person: an `sl:avatar-image`, and an `sl:avatar-fallback` (initials) shown instead while the image has no `src`, is loading or fails to load. Decorative unless it has a `label`, as an avatar usually sits next to the person's name.

| Attribute | Values | Default | Description |
| --- | --- | --- | --- |
| `size` | `sm`, `default`, `lg` | `default` | Size of the avatar. |
| `label` | *string* | — | The person's name, for an avatar with no name next to it. It is then announced as an image with that name; without it the avatar is hidden from assistive technology. |

Every prop can also be set from an expression: `th:size="${...}"`. Any other attribute passes through to the rendered element, and `class` is appended.

### sl:avatar-image

Attributes of `<sl:avatar-image>`: The picture inside `sl:avatar`. Pass `src` or `th:src`; when it is empty or null, while the picture loads, and when it fails to load (marked by Shadleaf's script), the image is hidden and the `sl:avatar-fallback` shows instead. The avatar's `label` names it, so the image itself has an empty `alt`.

`sl:avatar-image` declares no props; every attribute passes through.

### sl:avatar-fallback

Attributes of `<sl:avatar-fallback>`: What `sl:avatar` shows when its image has no `src`, while it loads and when it fails to load: usually the person's initials, or an `sl:icon`.

`sl:avatar-fallback` declares no props; every attribute passes through.
