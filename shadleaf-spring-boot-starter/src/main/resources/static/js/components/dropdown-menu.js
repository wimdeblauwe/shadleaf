/*
  sl:dropdown-menu-content. The popover does the rest: the trigger opens and closes it with popovertarget, Escape and a
  click outside close it, and focus returns to the trigger. This adds what a menu needs on top (the ARIA menu pattern):
  - aria-expanded on the trigger;
  - focus on the first item when the keyboard opens it (on the menu itself for a pointer), arrow down or up on the
    trigger opening it on the first or last item;
  - arrow keys, Home and End between the items, skipping disabled ones, and typeahead: typing the start of an item's
    text focuses it;
  - the item under the pointer takes the focus, so pointer and keyboard highlight the same way;
  - closing after an item is chosen (a click, Enter, or Space, which a link does not react to by itself), with the focus
    back on the trigger, and when Tab moves the focus out;
  - the focus back on the trigger after Escape inside another popover (a menu in the sidebar's phone panel), where the
    browser does not return it.
  Nothing is needed for htmx's history cache: whether a popover is open is not in the markup it saves, and Alpine
  initialises the restored markup again, which resets aria-expanded.
*/
import {
  hide, listener, positionWithoutAnchoring, restoreFocusAfterEscape, show, trackExpanded, triggersOf, typeaheadMatch,
} from '../popup.js';

const ITEM = '[role="menuitem"], [role="menuitemcheckbox"], [role="menuitemradio"]';
const TYPEAHEAD_RESET_MS = 500;

export default function dropdownMenu() {
  return {
    init() {
      const menu = this.$el;
      const cleanups = [];
      const listen = listener(cleanups);
      let openedWithKeyboard = false;
      let lastTrigger = null;
      let focusLastOnOpen = false;
      let typed = '';
      let typedAt = 0;

      // Out of the tab order: the arrow keys move between the items. The server renders them without, so Tab reaches
      // them when Alpine is not there.
      menu.querySelectorAll(ITEM).forEach(item => item.setAttribute('tabindex', '-1'));
      trackExpanded(menu, listen);
      positionWithoutAnchoring(menu, () => triggersOf(menu)[0], {side: 'bottom', align: 'start'}, listen);
      restoreFocusAfterEscape(menu, () => lastTrigger ?? triggersOf(menu)[0], listen);

      // Focus into the menu once it is open: on the first (or last) item when the keyboard opened it, on the menu itself
      // for a pointer. beforetoggle, not toggle: the browser merges a toggle event into one still pending, and drops it
      // when the menu closed and opened again before it fired. The timeout runs once the menu is shown.
      listen(menu, 'beforetoggle', event => {
        if (event.newState !== 'open') {
          return;
        }
        setTimeout(() => {
          const items = enabledItems(menu);
          const target = openedWithKeyboard ? (focusLastOnOpen ? items.at(-1) : items[0]) : undefined;
          (target ?? menu).focus();
          openedWithKeyboard = false;
          focusLastOnOpen = false;
        });
      });

      for (const trigger of triggersOf(menu)) {
        listen(trigger, 'pointerdown', () => {
          openedWithKeyboard = false;
          lastTrigger = trigger;
        });
        listen(trigger, 'keydown', event => {
          lastTrigger = trigger;
          if (event.key === 'Enter' || event.key === ' ') {
            openedWithKeyboard = true;
          } else if (event.key === 'ArrowDown' || event.key === 'ArrowUp') {
            event.preventDefault();
            openedWithKeyboard = true;
            focusLastOnOpen = event.key === 'ArrowUp';
            show(menu, trigger);
          }
        });
      }

      listen(menu, 'keydown', event => {
        const items = enabledItems(menu);
        const index = items.indexOf(document.activeElement);
        let next;
        switch (event.key) {
          case 'ArrowDown':
            next = index < 0 ? items[0] : items[Math.min(index + 1, items.length - 1)];
            break;
          case 'ArrowUp':
            next = index < 0 ? items.at(-1) : items[Math.max(index - 1, 0)];
            break;
          case 'Home':
          case 'PageUp':
            next = items[0];
            break;
          case 'End':
          case 'PageDown':
            next = items.at(-1);
            break;
          case 'Tab':
            // Focus goes back to the trigger as the menu closes, and Tab moves on from there.
            hide(menu);
            return;
          case ' ':
            // A button clicks on Space by itself; a link does not.
            if (event.target instanceof HTMLAnchorElement && event.target.matches(ITEM)) {
              event.preventDefault();
              event.target.click();
            }
            return;
          default:
            if (event.key.length === 1 && !event.ctrlKey && !event.metaKey && !event.altKey) {
              const now = Date.now();
              typed = now - typedAt > TYPEAHEAD_RESET_MS ? event.key : typed + event.key;
              typedAt = now;
              next = typeaheadMatch(items, index, typed.toLowerCase(), textOf);
              if (next) {
                event.preventDefault();
              }
            }
            break;
        }
        if (next) {
          event.preventDefault();
          next.focus();
        }
      });

      listen(menu, 'pointermove', event => {
        if (event.pointerType !== 'mouse') {
          return;
        }
        const item = event.target instanceof Element ? event.target.closest(ITEM) : null;
        const target = item && isEnabled(item) ? item : menu;
        if (document.activeElement !== target) {
          target.focus({preventScroll: true});
        }
      });
      listen(menu, 'pointerleave', event => {
        if (event.pointerType === 'mouse' && menu.contains(document.activeElement)) {
          menu.focus({preventScroll: true});
        }
      });

      // After the item's own listeners (htmx's included) have run: they sit on the item, this on the menu. Focus goes
      // back to the trigger before the item's default action, so a dialog the item opens returns the focus there when
      // it closes; the menu closes after that action, as a submit button that is no longer rendered submits nothing.
      listen(menu, 'click', event => {
        const item = event.target instanceof Element ? event.target.closest(ITEM) : null;
        if (!item || !menu.contains(item)) {
          return;
        }
        if (!isEnabled(item)) {
          event.preventDefault();
          return;
        }
        (lastTrigger ?? triggersOf(menu)[0])?.focus({preventScroll: true});
        setTimeout(() => hide(menu));
      });

      this.cleanups = cleanups;
    },

    destroy() {
      this.cleanups.forEach(cleanup => cleanup());
    },
  };
}

function enabledItems(menu) {
  return Array.from(menu.querySelectorAll(ITEM)).filter(isEnabled);
}

function isEnabled(item) {
  return !item.matches(':disabled, [aria-disabled="true"]');
}

/** An item's text without its shortcut, for typeahead. */
function textOf(item) {
  const walker = document.createTreeWalker(item, NodeFilter.SHOW_TEXT);
  let text = '';
  for (let node = walker.nextNode(); node; node = walker.nextNode()) {
    if (!node.parentElement.closest('.dropdown-menu-shortcut')) {
      text += node.textContent;
    }
  }
  return text.trim().toLowerCase();
}
