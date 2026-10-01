/*
  sl:avatar-image. Marks every avatar image with data-status="loaded" once its picture is there, or "error" when it
  failed (a 404, a URL the Content-Security-Policy's img-src blocks, offline, not an image). The stylesheet shows the
  image only when it is marked loaded, and the fallback until then, so a broken image never shows: Chromium draws its
  broken-image icon even with alt="", and before this script runs the image is simply not shown yet.

  Not an Alpine component: one listener on the document covers every avatar, also those htmx swaps in, with no x-data
  on each one in a long list. The entries call this whatever shadleaf.assets.alpine is, so it works with an external
  Alpine, or none at all.

  - load and error do not bubble, so they are caught on their way down (capture);
  - an image that loaded or failed before this script ran is found when it starts;
  - a src or srcset that changes (htmx swapping the attribute, an application's script) clears the mark, so the
    fallback shows while the new picture loads; the new load or error event marks it again.
  A history restore by htmx brings back the marks of the saved page, on new images whose events then correct them.
*/
const SELECTOR = '.avatar-image';

function mark(image, status) {
  if (image.dataset.status !== status) {
    image.dataset.status = status;
  }
}

function onLoadOrError(event) {
  const image = event.target;
  if (image instanceof HTMLImageElement && image.matches(SELECTOR)) {
    mark(image, event.type === 'load' ? 'loaded' : 'error');
  }
}

/** An image that finished before the listeners were there. complete is also true for a broken one. */
function markFinished(image) {
  if (!image.complete || !image.getAttribute('src')) {
    return;
  }
  if (image.naturalWidth > 0) {
    mark(image, 'loaded');
    return;
  }
  // Broken, or an SVG without a size of its own, which has no natural width in every browser: decode() tells them
  // apart. The answer only counts if the src is still the one it was asked about.
  const src = image.currentSrc;
  image.decode().then(
      () => image.currentSrc === src && mark(image, 'loaded'),
      () => image.currentSrc === src && mark(image, 'error'));
}

function onSourceChange(records) {
  for (const record of records) {
    if (record.target.matches(SELECTOR)) {
      delete record.target.dataset.status;
      markFinished(record.target);
    }
  }
}

export function watchAvatarImages() {
  if (document.slAvatarImages) {
    // A second Shadleaf script on the page (it logs why elsewhere); one set of listeners is enough.
    return;
  }
  document.slAvatarImages = true;
  document.addEventListener('load', onLoadOrError, true);
  document.addEventListener('error', onLoadOrError, true);
  new MutationObserver(onSourceChange)
      .observe(document.documentElement, {subtree: true, attributes: true, attributeFilter: ['src', 'srcset']});
  document.querySelectorAll(SELECTOR).forEach(markFinished);
}
