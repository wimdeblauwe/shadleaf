/*
  sl:toaster. The server renders the region, its announcers and a template per variant; this makes the toasts work:
  - toasts come from the markup (rendered with the page, or swapped in: an out-of-band swap into the toaster itself is
    moved into its list) and from the sl-toast event, whose detail is {title, description, variant, duration}, an
    array of those, or a string (the title). htmx dispatches it for an HX-Trigger response header, and wraps a detail
    that is not an object in {value}. The text is set as text, never as HTML. One toaster takes each event.
  - the viewport is a popover="manual", shown so that it is in the top layer. While a modal dialog is open it moves into
    the top-most one: outside it, the dialog would make it inert (not clickable, not focusable, not in the
    accessibility tree), though it would still be drawn above it.
  - each toast is announced through the announcers, a moment after it arrives: the polite one, or the assertive one for
    an error. A moment later, because an htmx response may close a dialog right after it showed a toast (the order of
    HX-Trigger events is not fixed), and the announcer has to be where it stays.
  - a toast closes after its duration (data-duration, or the toaster's; 0 keeps it). The time stops while the pointer is
    on the toasts, while one has the focus and while the page is hidden, and at least a second is left afterwards.
  - at most data-visible-toasts show, the newest; older ones wait. They stack, and spread out on hover and focus.
  - the viewport is a region (landmark) named "Notifications" only while it holds toasts.
  - the close button and Escape (on a focused toast) close one, and the focus moves to another toast or back to where
    it came from. Alt+T moves the focus to the newest toast, as in Sonner.
  - toasts are removed before htmx saves the page in its history cache, so going back never shows one again.
*/
import {listener} from '../popup.js';

const DEFAULT_DURATION_MS = 5000;
const DEFAULT_VISIBLE_TOASTS = 3;
const MIN_REMAINING_MS = 1000;
const EXIT_MS = 400;
const ANNOUNCE_DELAY_MS = 150;
const ANNOUNCEMENT_LIFETIME_MS = 10000;
const VARIANTS = ['default', 'success', 'info', 'warning', 'error'];

// Events one toaster has taken: a page with two toasters shows each toast once.
const handled = new WeakSet();
const utf8 = new TextDecoder('utf-8', {fatal: true});

/**
 * Text from an HTTP header. A server writes the HX-Trigger JSON as UTF-8, but the browser reads a header's bytes as
 * Latin-1, so "Zoë" arrives as "ZoÃ«". Text made only of characters up to U+00FF that are valid UTF-8 bytes is decoded
 * again; anything else (real Latin-1 text, such as a lone "ë") is not valid UTF-8 and stays as it is.
 */
function text(value) {
  const string = String(value);
  if (!/[\u0080-\u00ff]/.test(string) || /[^\u0000-\u00ff]/.test(string)) {
    return string;
  }
  try {
    return utf8.decode(Uint8Array.from(string, character => character.charCodeAt(0)));
  } catch {
    return string;
  }
}

export default function toaster() {
  return {
    init() {
      const root = this.$el;
      const viewport = root.querySelector(':scope > .toaster-viewport');
      const list = viewport?.querySelector(':scope > .toaster-list');
      if (!list) {
        return;
      }
      const [polite, assertive] = viewport.querySelectorAll(':scope > .toaster-announcer');
      const label = viewport.getAttribute('aria-label');
      const cleanups = [];
      const listen = listener(cleanups);
      const defaultDuration = Number(root.dataset.duration ?? DEFAULT_DURATION_MS);
      const visibleToasts = Math.max(1, Number(root.dataset.visibleToasts ?? DEFAULT_VISIBLE_TOASTS));
      // Per toast: the time left, its timer, when that started, and its height when last measured.
      const states = new Map();
      let hovering = false;
      let focusWithin = false;
      let returnFocus = null;

      const paused = () => hovering || focusWithin || document.hidden;
      const toasts = () => Array.from(list.children)
          .filter(element => element.matches('li.toast') && !element.hasAttribute('data-removed'));
      const shown = () => toasts().filter(toast => !toast.hasAttribute('data-hidden'));

      /** The top-most open modal dialog: the one with the focus, else the last one. */
      const topModal = () => document.activeElement?.closest?.('dialog:modal')
          ?? Array.from(document.querySelectorAll('dialog:modal')).at(-1) ?? null;

      /** Puts the viewport where it is usable (in the top-most modal dialog, or back home) and shows it. */
      const place = () => {
        // Removed from the page (an htmx swap, a history restore): Alpine destroys this toaster in a microtask, and
        // until then it must not show a viewport that is no longer in the document.
        if (!root.isConnected) {
          return;
        }
        const target = topModal() ?? root;
        if (viewport.parentElement !== target) {
          if (target === root) {
            root.prepend(viewport);
          } else {
            target.append(viewport);
          }
        }
        if (!viewport.matches(':popover-open')) {
          viewport.showPopover();
        }
      };

      const startTimer = toast => {
        const state = states.get(toast);
        if (!state || state.timer || !Number.isFinite(state.remaining) || paused()) {
          return;
        }
        state.started = Date.now();
        state.timer = setTimeout(() => close(toast), state.remaining);
      };
      const stopTimer = toast => {
        const state = states.get(toast);
        if (!state?.timer) {
          return;
        }
        clearTimeout(state.timer);
        state.timer = 0;
        state.remaining = Math.max(state.remaining - (Date.now() - state.started), MIN_REMAINING_MS);
      };

      /** Sets the stack's numbers, decides which toasts show, and runs the timers of those that do. */
      const layout = () => {
        const newestFirst = toasts().reverse();
        const expanded = list.hasAttribute('data-expanded');
        let offset = 0;
        let count = 0;
        newestFirst.forEach((toast, index) => {
          const state = states.get(toast);
          const visible = index < visibleToasts;
          toast.toggleAttribute('data-hidden', !visible);
          toast.toggleAttribute('data-behind', visible && index > 0);
          toast.style.setProperty('--toast-index', String(visible ? index : visibleToasts - 1));
          toast.style.setProperty('--toast-offset', `${offset}px`);
          // A collapsed toast behind the front one has the front one's height: keep the height it had.
          if (state && (index === 0 || expanded)) {
            state.height = toast.getBoundingClientRect().height;
          }
          if (visible) {
            offset += state?.height ?? 0;
            count++;
          }
          if (visible && !paused()) {
            startTimer(toast);
          } else {
            stopTimer(toast);
          }
        });
        // A landmark only while there is something in it: an empty one is noise in a screen reader's list of them.
        if (newestFirst.length > 0) {
          viewport.setAttribute('role', 'region');
          viewport.setAttribute('aria-label', label);
        } else {
          viewport.removeAttribute('role');
          viewport.removeAttribute('aria-label');
        }
        const front = newestFirst[0];
        list.style.setProperty('--toast-front-height', `${front ? states.get(front)?.height ?? 0 : 0}px`);
        list.style.setProperty('--toast-stack-sum', `${offset}px`);
        list.style.setProperty('--toast-count', String(Math.max(count, 1)));
      };

      const announce = toast => {
        const text = ['.toast-title', '.toast-description']
            .map(selector => toast.querySelector(selector)?.textContent.trim())
            .filter(Boolean)
            .join('. ');
        const region = toast.dataset.variant === 'error' ? assertive : polite;
        if (!text || !region) {
          return;
        }
        setTimeout(() => {
          place();
          const message = document.createElement('p');
          message.textContent = text;
          region.append(message);
          setTimeout(() => message.remove(), ANNOUNCEMENT_LIFETIME_MS);
        }, ANNOUNCE_DELAY_MS);
      };

      const register = toast => {
        const own = toast.dataset.duration;
        const duration = own !== undefined && own !== '' ? Number(own) : defaultDuration;
        states.set(toast, {
          remaining: Number.isFinite(duration) && duration > 0 ? duration : Infinity,
          timer: 0,
          started: 0,
          height: 0,
        });
        toast.setAttribute('data-enhanced', '');
        announce(toast);
      };

      /** Takes up toasts that arrived in the markup, and forgets the ones that are gone. */
      const adopt = () => {
        root.querySelectorAll(':scope > li.toast').forEach(toast => list.append(toast));
        states.forEach((state, toast) => {
          if (!toast.isConnected) {
            clearTimeout(state.timer);
            states.delete(toast);
          }
        });
        const fresh = toasts().filter(toast => !states.has(toast));
        if (fresh.length > 0) {
          place();
          fresh.forEach(register);
        }
        layout();
      };

      const close = toast => {
        const state = states.get(toast);
        if (!state || toast.hasAttribute('data-removed')) {
          return;
        }
        clearTimeout(state.timer);
        states.delete(toast);
        if (toast.contains(document.activeElement)) {
          const others = shown().filter(other => other !== toast);
          const index = toasts().indexOf(toast);
          // The next older toast, else the next newer one, else where the focus came from.
          const next = others.filter(other => toasts().indexOf(other) < index).at(-1) ?? others[0];
          const target = next ?? (returnFocus?.isConnected ? returnFocus : null);
          if (target) {
            target.focus();
          } else {
            document.activeElement.blur();
          }
        }
        toast.setAttribute('data-removed', '');
        layout();
        setTimeout(() => toast.remove(), EXIT_MS);
      };

      const add = detail => {
        const items = detail !== null && typeof detail === 'object' && !Array.isArray(detail)
            && 'value' in detail && !('title' in detail) ? [].concat(detail.value) : [].concat(detail);
        for (const item of items) {
          const data = typeof item === 'string' ? {title: item} : (item ?? {});
          const title = data.title == null ? '' : text(data.title).trim();
          if (!title) {
            console.error('Shadleaf: an sl-toast event needs a title, e.g. {"title": "Member deleted"}.');
            continue;
          }
          const variant = VARIANTS.includes(data.variant) ? data.variant : 'default';
          const template = root.querySelector(`:scope > template.toaster-template[data-variant="${variant}"]`);
          const toast = template?.content.querySelector('.toast')?.cloneNode(true);
          if (!toast) {
            continue;
          }
          toast.querySelector('.toast-title').textContent = title;
          const description = toast.querySelector('.toast-description');
          if (data.description == null || String(data.description).trim() === '') {
            description?.remove();
          } else if (description) {
            description.textContent = text(data.description);
          }
          if (data.duration != null && data.duration !== '' && Number.isFinite(Number(data.duration))) {
            toast.dataset.duration = String(Number(data.duration));
          }
          list.append(toast);
        }
        adopt();
      };

      const update = () => {
        list.toggleAttribute('data-expanded', hovering || focusWithin);
        layout();
      };

      list.setAttribute('data-stacked', '');
      place();
      adopt();

      const observer = new MutationObserver(adopt);
      observer.observe(root, {childList: true});
      observer.observe(list, {childList: true});
      cleanups.push(() => observer.disconnect());

      listen(document, 'sl-toast', event => {
        // A toaster that was just removed leaves the event to one that is still on the page.
        if (root.isConnected && !handled.has(event)) {
          handled.add(event);
          add(event.detail);
        }
      });
      listen(list, 'pointerenter', () => {
        hovering = true;
        update();
      });
      listen(list, 'pointerleave', () => {
        hovering = false;
        update();
      });
      listen(viewport, 'focusin', event => {
        if (!viewport.contains(event.relatedTarget) && event.relatedTarget && !returnFocus) {
          returnFocus = event.relatedTarget;
        }
        focusWithin = true;
        update();
      });
      listen(viewport, 'focusout', event => {
        if (!viewport.contains(event.relatedTarget)) {
          focusWithin = false;
          returnFocus = null;
          update();
        }
      });
      listen(document, 'visibilitychange', update);
      listen(window, 'resize', layout);
      listen(viewport, 'click', event => {
        const button = event.target instanceof Element ? event.target.closest('.toast-close') : null;
        if (button) {
          close(button.closest('.toast'));
        }
      });
      // Escape closes the focused toast, and nothing else: not the dialog the toaster may be in.
      listen(viewport, 'keydown', event => {
        const toast = event.key === 'Escape' && event.target instanceof Element ? event.target.closest('.toast') : null;
        if (toast && states.has(toast)) {
          event.preventDefault();
          event.stopPropagation();
          close(toast);
        }
      });
      listen(document, 'keydown', event => {
        if (!event.altKey || event.ctrlKey || event.metaKey || event.shiftKey || event.code !== 'KeyT'
            || handled.has(event)) {
          return;
        }
        const newest = shown().at(-1);
        if (newest) {
          handled.add(event);
          event.preventDefault();
          if (!viewport.contains(document.activeElement)) {
            returnFocus = document.activeElement;
          }
          newest.focus();
        }
      });
      // A dialog that opens or closes changes where the viewport has to be.
      const replace = event => {
        if (event.target instanceof HTMLDialogElement) {
          place();
        }
      };
      listen(document, 'toggle', replace, true);
      listen(document, 'close', replace, true);
      listen(document, 'htmx:beforeHistorySave', () => {
        states.forEach(state => clearTimeout(state.timer));
        states.clear();
        list.replaceChildren();
        viewport.querySelectorAll(':scope > .toaster-announcer').forEach(region => region.replaceChildren());
        // The snapshot is restored as markup, where a strict CSP blocks style attributes.
        list.removeAttribute('style');
        list.removeAttribute('data-expanded');
        root.prepend(viewport);
      });

      cleanups.push(() => states.forEach(state => clearTimeout(state.timer)));
      this.cleanups = cleanups;
    },

    destroy() {
      this.cleanups?.forEach(cleanup => cleanup());
    },
  };
}
