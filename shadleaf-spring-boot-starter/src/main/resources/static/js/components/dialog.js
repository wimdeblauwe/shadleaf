/*
  sl:dialog. The <dialog> element does the work: showModal() makes it modal and the page behind it inert, Escape closes
  it, and closing returns focus to the element that had it. Buttons open and close it with invoker commands
  (commandfor/command), and closedby="any" closes it on a click outside, all without script. This adds what the
  element cannot do, and stands in for the two attributes where a browser lacks them:
  - data-show-modal (the open prop): open as a modal as soon as it is on the page, e.g. after an htmx swap;
  - the sl-dialog-close event, e.g. from an htmx HX-Trigger response header on a request from inside the dialog, closes
    it. htmx 2 handles that header before the swap, htmx 4 after it: there it closes when the swap starts instead, so
    that, as with htmx 2, focus is back on the element that opened the dialog when htmx swaps. Otherwise the page is
    still inert behind the modal dialog during the swap, htmx can neither keep the focus on an element it replaces
    (by its id) nor move it to [autofocus], and closing afterwards returns the focus to an opener the swap removed;
  - it closes before htmx saves the page in its history cache, which would otherwise restore it open but not modal;
  - a body (sl:dialog-body, sl:sheet-body) is an htmx target that opens the dialog and shows the request's progress
    and errors (see dialog-body.js).
*/
import {watchBody} from '../dialog-body.js';
import {onHistorySave} from '../history.js';

const supportsCommands = 'commandForElement' in HTMLButtonElement.prototype;
const supportsClosedBy = 'closedBy' in HTMLDialogElement.prototype;

export default function dialog() {
  return {
    // Declared, so init() sets it here: Alpine puts a property no scope has on the outermost x-data, where every
    // component nested in that one (all of them, inside sl:sidebar-provider) would share it.
    cleanups: [],

    init() {
      const dialog = this.$el;
      const cleanups = [];
      const listen = (target, type, listener) => {
        target.addEventListener(type, listener);
        cleanups.push(() => target.removeEventListener(type, listener));
      };

      // From inside a nested dialog, the event closes that one only.
      listen(dialog, 'sl-dialog-close', event => {
        if (event.target.closest('dialog') === dialog) {
          dialog.close();
        }
      });
      listen(document, 'htmx:before:swap', event => {
        const ctx = event.detail?.ctx;
        const target = closeTarget(ctx?.hx?.trigger, ctx?.sourceElement);
        if (dialog.open && target instanceof Element && target.closest('dialog') === dialog) {
          dialog.close();
        }
      });
      onHistorySave(listen, () => dialog.close());
      if (!supportsCommands) {
        listen(document, 'click', event => runCommand(dialog, event));
      }
      if (!supportsClosedBy && dialog.getAttribute('closedby') === 'any') {
        closeOnClickOutside(dialog, listen);
      }
      watchBody(dialog, listen);
      this.cleanups = cleanups;

      if (dialog.hasAttribute('data-show-modal') && !dialog.open) {
        dialog.showModal();
      }
    },

    destroy() {
      this.cleanups.forEach(cleanup => cleanup());
    },
  };
}

/**
 * The element htmx 4 will dispatch sl-dialog-close on for an HX-Trigger header `header` (a list of event names, or an
 * object of names and details, where a detail's `target` names another element), or null when the header has no
 * sl-dialog-close.
 */
function closeTarget(header, source) {
  if (typeof header !== 'string' || !header.includes('sl-dialog-close')) {
    return null;
  }
  if (!header.trimStart().startsWith('{')) {
    return header.split(',').some(name => name.trim() === 'sl-dialog-close') ? source : null;
  }
  let triggers;
  try {
    triggers = JSON.parse(header);
  } catch {
    // htmx reads the header as HCON, which JSON is part of; for anything else, the source is where it would go.
    return source;
  }
  if (!Object.hasOwn(triggers, 'sl-dialog-close')) {
    return null;
  }
  const target = triggers['sl-dialog-close']?.target;
  if (typeof target !== 'string') {
    return source;
  }
  try {
    // htmx's own lookup, which knows its extended selectors (closest, next, ...), as htmx dispatches it there.
    return window.htmx?.find ? window.htmx.find(target) : document.querySelector(target);
  } catch {
    return null;
  }
}

/** What a button with commandfor pointing at this dialog does in a browser with invoker commands. */
function runCommand(dialog, event) {
  const button = event.target instanceof Element ? event.target.closest('button[commandfor]') : null;
  if (!button || button.disabled || button.getAttribute('commandfor') !== dialog.id) {
    return;
  }
  const command = button.getAttribute('command');
  if (command === 'show-modal' && !dialog.open) {
    dialog.showModal();
  } else if ((command === 'close' || command === 'request-close') && dialog.open) {
    dialog.close(button.value);
  }
}

/**
 * closedby="any": a click that both starts and ends outside the panel closes it. A click on the backdrop reaches the
 * dialog itself, as does one on its padding, so the position decides. Checking where it started keeps a text
 * selection dragged out of the dialog from closing it.
 */
function closeOnClickOutside(dialog, listen) {
  const outside = event => {
    const rect = dialog.getBoundingClientRect();
    return event.clientX < rect.left || event.clientX > rect.right
        || event.clientY < rect.top || event.clientY > rect.bottom;
  };
  let startedOutside = false;
  listen(dialog, 'pointerdown', event => {
    startedOutside = event.target === dialog && outside(event);
  });
  listen(dialog, 'click', event => {
    if (startedOutside && event.target === dialog && outside(event)) {
      dialog.close();
    }
    startedOutside = false;
  });
}
