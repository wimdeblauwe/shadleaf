/*
  sl:popover-content. The popover does the rest: the trigger opens and closes it with popovertarget, Escape and a click
  outside close it, and focus returns to the trigger. This adds what a non-modal dialog next to its trigger needs:
  - aria-expanded on the trigger;
  - focus on the first control inside when it opens (an element with autofocus wins, as the browser focuses that
    itself), or on the panel when it has none;
  - closing when the focus moves out, e.g. Tab past its last control;
  - the sl-popover-close event, e.g. from an htmx HX-Trigger response header on a request from inside it, closes it.
  Nothing is needed for htmx's history cache: whether a popover is open is not in the markup it saves, and Alpine
  initialises the restored markup again, which resets aria-expanded.
*/
import {firstTabbable, hide, listener, positionWithoutAnchoring, trackExpanded, triggersOf} from '../popup.js';

export default function popover() {
  return {
    init() {
      const popup = this.$el;
      const cleanups = [];
      const listen = listener(cleanups);

      trackExpanded(popup, listen);
      positionWithoutAnchoring(popup, () => triggersOf(popup)[0], {side: 'bottom', align: 'center'}, listen);

      // Once it is shown (beforetoggle, then a timeout: the browser drops a toggle event when the popover closed and
      // opened again before it fired).
      listen(popup, 'beforetoggle', event => {
        if (event.newState === 'open') {
          setTimeout(() => {
            if (popup.matches(':popover-open') && !popup.contains(document.activeElement)) {
              (firstTabbable(popup) ?? popup).focus();
            }
          });
        }
      });
      listen(popup, 'focusout', event => {
        const to = event.relatedTarget;
        if (to instanceof Node && !popup.contains(to) && !triggersOf(popup).includes(to)) {
          hide(popup);
        }
      });
      // From inside a nested popover, the event closes that one only.
      listen(popup, 'sl-popover-close', event => {
        if (event.target.closest('.popover-content') === popup) {
          hide(popup);
        }
      });

      this.cleanups = cleanups;
    },

    destroy() {
      this.cleanups.forEach(cleanup => cleanup());
    },
  };
}
