/*
  sl:dialog. The <dialog> element does the work: showModal() makes it modal and the page behind it inert, Escape closes
  it, and closing returns focus to the element that had it. Buttons open and close it with invoker commands
  (commandfor/command), and closedby="any" closes it on a click outside, all without script. This adds what the
  element cannot do, and stands in for the two attributes where a browser lacks them:
  - data-show-modal (the open prop): open as a modal as soon as it is on the page, e.g. after an htmx swap;
  - the sl-dialog-close event, e.g. from an htmx HX-Trigger response header on a request from inside the dialog, closes
    it;
  - it closes before htmx saves the page in its history cache, which would otherwise restore it open but not modal.
*/
const supportsCommands = 'commandForElement' in HTMLButtonElement.prototype;
const supportsClosedBy = 'closedBy' in HTMLDialogElement.prototype;

export default function dialog() {
  return {
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
      listen(document, 'htmx:beforeHistorySave', () => dialog.close());
      if (!supportsCommands) {
        listen(document, 'click', event => runCommand(dialog, event));
      }
      if (!supportsClosedBy && dialog.getAttribute('closedby') === 'any') {
        closeOnClickOutside(dialog, listen);
      }
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
