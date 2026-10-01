/*
  sl:checkbox indeterminate. HTML has no attribute for the indeterminate state, only the DOM property, so the template
  renders data-indeterminate="true" and this sets the property: on the checkboxes there when it starts, and on those
  added later (an htmx swap, a history restore). Only once per element, so a click (which clears the state, as on
  every checkbox) or a script setting it is never undone.

  Not an Alpine component, for the same reason as avatar.js: one observer covers every checkbox, and the entries call
  this whatever shadleaf.assets.alpine is.
*/
const SELECTOR = 'input[type="checkbox"][data-indeterminate="true"]';
const done = new WeakSet();

function apply(checkbox) {
  if (!done.has(checkbox)) {
    done.add(checkbox);
    checkbox.indeterminate = true;
  }
}

function applyWithin(node) {
  if (node.nodeType !== Node.ELEMENT_NODE) {
    return;
  }
  if (node.matches(SELECTOR)) {
    apply(node);
  }
  node.querySelectorAll(SELECTOR).forEach(apply);
}

export function watchIndeterminateCheckboxes() {
  if (document.slIndeterminateCheckboxes) {
    return;
  }
  document.slIndeterminateCheckboxes = true;
  new MutationObserver(records => records.forEach(record => record.addedNodes.forEach(applyWithin)))
      .observe(document.documentElement, {childList: true, subtree: true});
  document.querySelectorAll(SELECTOR).forEach(apply);
}
