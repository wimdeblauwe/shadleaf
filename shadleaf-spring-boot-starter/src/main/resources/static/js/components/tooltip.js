/*
  sl:tooltip: the wrapper around a trigger (its first element) and a sl:tooltip-content, a popover="manual". Nothing in
  HTML shows a popover on hover in every browser yet, so this does:
  - the content describes the trigger (aria-describedby, added to any the trigger has; the content gets an id if it
    has none);
  - it shows when the pointer rests on the trigger for the delay (data-delay, 300 ms by default), at once on keyboard
    focus, and at once when another tooltip hid a moment ago, so moving along a toolbar does not wait at every button;
  - it stays while the pointer moves from the trigger onto it, so it can be read at any zoom (WCAG 1.4.13), and hides
    when the pointer leaves both, when the trigger loses focus or is pressed, and on Escape, which then does nothing
    else (an open dialog or menu around it stays open);
  - one tooltip at a time; touch never shows one.
  The trigger is passed to showPopover() as its source, which makes it the implicit anchor CSS positions against.
*/
import {listener, placedSide, positionWithoutAnchoring, show} from '../popup.js';

const DEFAULT_DELAY_MS = 300;
const HIDE_GRACE_MS = 100;
const SKIP_DELAY_MS = 300;

let current = null;
let lastHiddenAt = 0;
let counter = 0;

export default function tooltip() {
  return {
    init() {
      const wrapper = this.$el;
      const content = wrapper.querySelector(':scope > .tooltip-content');
      const trigger = Array.from(wrapper.children).find(child => child !== content);
      if (!content || !trigger) {
        return;
      }
      const cleanups = [];
      const listen = listener(cleanups);
      const delay = Number(wrapper.dataset.delay ?? DEFAULT_DELAY_MS);
      let showTimer = 0;
      let hideTimer = 0;
      let suppressed = false;

      if (!content.id) {
        content.id = `sl-tooltip-${++counter}`;
      }
      const describedBy = (trigger.getAttribute('aria-describedby') ?? '').split(/\s+/).filter(Boolean);
      if (!describedBy.includes(content.id)) {
        trigger.setAttribute('aria-describedby', [...describedBy, content.id].join(' '));
      }

      const self = {
        hide() {
          clearTimeout(showTimer);
          clearTimeout(hideTimer);
          if (content.matches(':popover-open')) {
            content.hidePopover();
            lastHiddenAt = Date.now();
          }
          if (current === self) {
            current = null;
          }
        },
      };
      const open = () => {
        clearTimeout(showTimer);
        clearTimeout(hideTimer);
        if (current && current !== self) {
          current.hide();
        }
        current = self;
        show(content, trigger);
        content.dataset.placed = placedSide(content, trigger);
      };
      const openAfterDelay = () => {
        clearTimeout(hideTimer);
        if (content.matches(':popover-open')) {
          return;
        }
        clearTimeout(showTimer);
        const warm = Date.now() - lastHiddenAt < SKIP_DELAY_MS || (current && current !== self);
        showTimer = setTimeout(open, warm ? 0 : delay);
      };
      const hideSoon = () => {
        clearTimeout(showTimer);
        clearTimeout(hideTimer);
        hideTimer = setTimeout(() => self.hide(), HIDE_GRACE_MS);
      };

      positionWithoutAnchoring(content, () => trigger, {side: 'top', align: 'center'}, listen);

      listen(trigger, 'pointerenter', event => {
        if (event.pointerType !== 'touch' && !suppressed) {
          openAfterDelay();
        }
      });
      listen(trigger, 'pointerleave', () => {
        suppressed = false;
        hideSoon();
      });
      listen(trigger, 'pointerdown', () => {
        suppressed = true;
        self.hide();
      });
      listen(trigger, 'focus', () => {
        if (trigger.matches(':focus-visible')) {
          open();
        }
      });
      listen(trigger, 'blur', () => self.hide());
      listen(content, 'pointerenter', () => clearTimeout(hideTimer));
      listen(content, 'pointerleave', hideSoon);
      listen(document, 'keydown', event => {
        if (event.key === 'Escape' && content.matches(':popover-open')) {
          event.preventDefault();
          event.stopPropagation();
          self.hide();
        }
      }, true);

      cleanups.push(() => self.hide());
      this.cleanups = cleanups;
    },

    destroy() {
      this.cleanups?.forEach(cleanup => cleanup());
    },
  };
}
