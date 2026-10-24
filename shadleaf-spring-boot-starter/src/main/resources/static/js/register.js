/*
  Registers the library's Alpine components. Every file in components/ default-exports the function for one
  Alpine.data component and is registered under its file name with an `sl` prefix: components/dropdown-menu.js
  becomes x-data="slDropdownMenu". Component logic lives here, never in template expressions, so the markup only
  needs what Alpine's CSP build accepts.

  A page has exactly one Alpine. The entries in entries/ decide whose it is:
  - shadleaf.alpine.js / shadleaf.alpine-csp.js bring their own (shadleaf.assets.alpine=bundled or csp);
  - shadleaf.js registers with the application's (shadleaf.assets.alpine=external).
*/

const modules = import.meta.glob('./components/*.js', {eager: true});

export const components = Object.entries(modules)
    .map(([file, module]) => [componentName(file), module.default]);

function componentName(file) {
  const baseName = file.slice('./components/'.length, -'.js'.length);
  return 'sl' + baseName.split('-').map(part => part.charAt(0).toUpperCase() + part.slice(1)).join('');
}

function register(Alpine) {
  for (const [name, component] of components) {
    Alpine.data(name, component);
  }
  watchRemovedAncestors(Alpine);
}

/*
  Alpine only destroys a removed node it has marked (_x_marker), and it marks just the elements it walked from an
  x-data root. When a swap removes a plain ancestor of a component (htmx swapping <main>), destroy() would never run:
  listeners on document/window, timers and blob: URLs would leak. So destroy the components inside such a node
  ourselves, once it is still disconnected a task later (a node that was moved, not removed, is left alone). Alpine's
  destroyTree deletes the marker, so a component is never destroyed twice.
*/
let watchingRemovedAncestors = false;

function watchRemovedAncestors(Alpine) {
  if (watchingRemovedAncestors || typeof Alpine.destroyTree !== 'function') {
    return;
  }
  watchingRemovedAncestors = true;
  new MutationObserver(mutations => {
    for (const mutation of mutations) {
      for (const node of mutation.removedNodes) {
        if (node.nodeType === Node.ELEMENT_NODE && !node._x_marker) {
          setTimeout(() => destroyComponentsIn(Alpine, node), 0);
        }
      }
    }
  }).observe(document, {childList: true, subtree: true});
}

function destroyComponentsIn(Alpine, removed) {
  if (removed.isConnected) {
    return;
  }
  for (const el of removed.querySelectorAll('[x-data]')) {
    if (el._x_marker && !el.isConnected) {
      Alpine.destroyTree(el);
    }
  }
}

/** For the bundled entries: registers with the given Alpine and starts it, unless the page already has one. */
export function startBundled(Alpine) {
  if (window.Alpine) {
    console.error('Shadleaf: this page already loads Alpine, so Shadleaf did not start a second copy and its '
        + 'components are not registered. Set shadleaf.assets.alpine=external to use the application\'s Alpine.');
    return;
  }
  register(Alpine);
  window.Alpine = Alpine;
  Alpine.start();
}

/**
 * For shadleaf.js: registers with the application's Alpine when it initialises. That only works when this script
 * runs before Alpine starts. Registering later still helps elements added afterwards, but the ones Alpine already
 * initialised stay inert, so that case is reported.
 */
export function registerWithApplicationAlpine() {
  if (!window.Alpine) {
    document.addEventListener('alpine:init', () => register(window.Alpine));
    return;
  }
  if (alpineHasStarted()) {
    console.error('Shadleaf: shadleaf.js ran after Alpine started, so Shadleaf components already on the page do '
        + 'not work. Include the Shadleaf assets fragment before the script that loads Alpine.');
  }
  register(window.Alpine);
}

function alpineHasStarted() {
  // Alpine keeps the data of every element it initialised in _x_dataStack.
  return Array.from(document.querySelectorAll('[x-data]')).some(el => el._x_dataStack !== undefined);
}
