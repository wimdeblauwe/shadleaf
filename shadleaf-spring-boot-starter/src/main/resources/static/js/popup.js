/*
  What the dropdown menu, the popover and the tooltip share. Each is a popover (the Popover API), which gives the top
  layer, light dismiss and Escape (popover="auto") and focus back on the trigger when it closes. CSS anchor
  positioning puts it next to its trigger, the popover's implicit anchor. This adds:
  - aria-expanded on the buttons that point at a popover with popovertarget;
  - a stand-in for anchor positioning in browsers without it (Safari before 26, Firefox before 147), which sets the
    position from the trigger's box while the popup is open;
  - small helpers for the listeners a component removes again in destroy().
*/

/** Adds a listener and remembers how to remove it. */
export function listener(cleanups) {
  return (target, type, handler, options) => {
    target.addEventListener(type, handler, options);
    cleanups.push(() => target.removeEventListener(type, handler, options));
  };
}

/** The buttons that open `popup` through popovertarget. */
export function triggersOf(popup) {
  return popup.id ? Array.from(document.querySelectorAll(`[popovertarget="${CSS.escape(popup.id)}"]`)) : [];
}

/**
 * Keeps aria-expanded on the triggers in step with the popup; the attribute starts false. On beforetoggle, which fires
 * for every change: the browser merges toggle events that follow each other quickly.
 */
export function trackExpanded(popup, listen) {
  const update = open => triggersOf(popup).forEach(trigger => trigger.setAttribute('aria-expanded', String(open)));
  update(popup.matches(':popover-open'));
  listen(popup, 'beforetoggle', event => update(event.newState === 'open'));
}

export function hide(popup) {
  if (popup.matches(':popover-open')) {
    popup.hidePopover();
  }
}

/** Shows the popup with `source` as its anchor, where the browser takes a source. */
export function show(popup, source) {
  if (popup.matches(':popover-open')) {
    return;
  }
  try {
    popup.showPopover({source});
  } catch (error) {
    if (!(error instanceof TypeError)) {
      throw error;
    }
    popup.showPopover();
  }
}

const TABBABLE = 'a[href], button:not(:disabled), input:not(:disabled):not([type="hidden"]), select:not(:disabled), '
    + 'textarea:not(:disabled), [tabindex]:not([tabindex="-1"]), [contenteditable="true"]';

/** The first element in `container` that Tab would reach. */
export function firstTabbable(container) {
  return Array.from(container.querySelectorAll(TABBABLE)).find(element => element.checkVisibility());
}

/** Read each time, so a test can switch it off. */
function supportsAnchoring() {
  return CSS.supports('position-area', 'block-end');
}

/**
 * Where anchor positioning is missing, places the popup next to `anchorOf()` while it is open, as the CSS would: on
 * its side (data-side, or `defaults.side`), lined up by data-align (or `defaults.align`), flipped when it would
 * overflow the viewport. The gap is the margin the CSS gives the popup.
 */
export function positionWithoutAnchoring(popup, anchorOf, defaults, listen) {
  if (supportsAnchoring()) {
    return;
  }
  let frame = 0;
  const place = () => {
    const anchor = anchorOf();
    if (anchor && popup.matches(':popover-open')) {
      placeNextTo(popup, anchor, defaults);
    }
  };
  const schedule = () => {
    cancelAnimationFrame(frame);
    frame = requestAnimationFrame(place);
  };
  listen(popup, 'toggle', event => {
    if (event.newState === 'open') {
      place();
      window.addEventListener('scroll', schedule, true);
      window.addEventListener('resize', schedule);
    } else {
      window.removeEventListener('scroll', schedule, true);
      window.removeEventListener('resize', schedule);
    }
  });
  listen(popup, 'sl-popup-place', place);
}

function placeNextTo(popup, anchor, defaults) {
  const style = popup.style;
  if (!style.position) {
    // The gap to the trigger, from the CSS margin on the side facing it, before the stand-in takes the margins over.
    const computed = getComputedStyle(popup);
    popup.dataset.slGap = String(Math.max(...['Top', 'Right', 'Bottom', 'Left']
        .map(side => parseFloat(computed[`margin${side}`]) || 0)));
    style.position = 'fixed';
    style.inset = 'auto';
    style.margin = '0';
    style.positionArea = 'none';
    style.positionTryFallbacks = 'none';
  }
  const gap = parseFloat(popup.dataset.slGap) || 0;
  const rtl = getComputedStyle(anchor).direction === 'rtl';
  const a = anchor.getBoundingClientRect();
  const p = popup.getBoundingClientRect();
  const viewport = {width: document.documentElement.clientWidth, height: window.innerHeight};
  let side = popup.dataset.side ?? defaults.side;
  if (side === 'right' || side === 'left') {
    side = (side === 'right') !== rtl ? 'right' : 'left';
  }
  const align = popup.dataset.align ?? defaults.align;
  const room = {
    top: a.top - gap, bottom: viewport.height - a.bottom - gap,
    left: a.left - gap, right: viewport.width - a.right - gap,
  };
  const opposite = {top: 'bottom', bottom: 'top', left: 'right', right: 'left'};
  const needed = side === 'top' || side === 'bottom' ? p.height : p.width;
  if (room[side] < needed && room[opposite[side]] > room[side]) {
    side = opposite[side];
  }

  let top;
  let left;
  if (side === 'top' || side === 'bottom') {
    top = side === 'bottom' ? a.bottom + gap : a.top - gap - p.height;
    const start = rtl ? a.right - p.width : a.left;
    const end = rtl ? a.left : a.right - p.width;
    left = align === 'center' ? a.left + (a.width - p.width) / 2 : align === 'end' ? end : start;
    left = Math.min(Math.max(left, 0), Math.max(viewport.width - p.width, 0));
  } else {
    left = side === 'right' ? a.right + gap : a.left - gap - p.width;
    top = align === 'center' ? a.top + (a.height - p.height) / 2 : align === 'end' ? a.bottom - p.height : a.top;
    top = Math.min(Math.max(top, 0), Math.max(viewport.height - p.height, 0));
  }
  style.top = `${Math.round(top)}px`;
  style.left = `${Math.round(left)}px`;
}

/**
 * The side of `anchor` the open popup ended up on, on screen: after a flip it differs from the one asked for. By its
 * centre, which the opening zoom and slide do not move out of place.
 */
export function placedSide(popup, anchor) {
  const a = anchor.getBoundingClientRect();
  const p = popup.getBoundingClientRect();
  const x = p.left + p.width / 2;
  const y = p.top + p.height / 2;
  if (y < a.top) {
    return 'top';
  }
  if (y > a.bottom) {
    return 'bottom';
  }
  return x < a.left ? 'left' : 'right';
}
