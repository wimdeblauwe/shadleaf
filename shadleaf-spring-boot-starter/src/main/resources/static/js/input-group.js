/*
  sl:input-group-addon. A click on an addon, outside a button, link or other control in it, focuses the group's input
  or textarea, as in shadcn: the addon reads as part of the input.

  Not an Alpine component: one listener on the document covers every input group, also those htmx swaps in, with no
  x-data on each one. The entries call this whatever shadleaf.assets.alpine is, so it works with an external Alpine, or
  none at all. Without JavaScript a click on the addon simply does nothing.
*/
const INTERACTIVE = 'a[href], button, input, select, textarea, label, summary, [tabindex], [contenteditable]';

function onClick(event) {
  if (!(event.target instanceof Element)) {
    return;
  }
  const addon = event.target.closest('.input-group-addon');
  const interactive = event.target.closest(INTERACTIVE);
  if (!addon || (interactive && addon.contains(interactive))) {
    return;
  }
  const control = addon.parentElement?.querySelector(':scope > :is(.input-group-input, .input-group-textarea)');
  if (control && !control.disabled) {
    control.focus();
  }
}

export function watchInputGroupAddons() {
  if (document.slInputGroupAddons) {
    // A second Shadleaf script on the page (it logs why elsewhere); one listener is enough.
    return;
  }
  document.slInputGroupAddons = true;
  document.addEventListener('click', onClick);
}
