/*
  What sl:tooltip (slTooltip) and the sidebar menu button's tooltip (slSidebarMenuTooltip) share: a popover="manual"
  that shows next to its trigger. Nothing in HTML shows a popover on hover in every browser yet, so this does:
  - it shows when the pointer rests on the trigger for the delay, at once on keyboard focus, and at once when another
    tooltip hid a moment ago, so moving along a toolbar does not wait at every button;
  - it stays while the pointer moves from the trigger onto it, so it can be read at any zoom (WCAG 1.4.13), and hides
    when the pointer leaves both, when the trigger loses focus or is pressed, and on Escape, which then does nothing
    else (an open dialog or menu around it stays open);
  - one tooltip at a time; touch never shows one.
  With `describe` the content describes the trigger (aria-describedby, added to any the trigger has; the content gets
  an id if it has none). Without it the content repeats the trigger's name and the markup hides it from assistive
  technology. `when` (optional) is asked each time before it shows. The trigger is passed to showPopover() as its
  source, which makes it the implicit anchor CSS positions against.
*/
import {placedSide, positionWithoutAnchoring, show} from './popup.js';

const HIDE_GRACE_MS = 100;
const SKIP_DELAY_MS = 300;

let current = null;
let lastHiddenAt = 0;
let counter = 0;

/** Sets the tooltip up; `listen` (popup.js's listener) collects what destroy() removes. Returns its hide(). */
export function attachTooltip(trigger, content, {delay, describe = true, when = () => true}, listen) {
  let showTimer = 0;
  let hideTimer = 0;
  let suppressed = false;

  if (describe) {
    if (!content.id) {
      content.id = `sl-tooltip-${++counter}`;
    }
    const describedBy = (trigger.getAttribute('aria-describedby') ?? '').split(/\s+/).filter(Boolean);
    if (!describedBy.includes(content.id)) {
      trigger.setAttribute('aria-describedby', [...describedBy, content.id].join(' '));
    }
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
    if (!when()) {
      return;
    }
    if (current && current !== self) {
      current.hide();
    }
    current = self;
    show(content, trigger);
    content.dataset.placed = placedSide(content, trigger);
  };
  const openAfterDelay = () => {
    clearTimeout(hideTimer);
    if (content.matches(':popover-open') || !when()) {
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

  return self;
}
