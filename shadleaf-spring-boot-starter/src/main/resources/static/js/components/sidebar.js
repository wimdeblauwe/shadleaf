/*
  sl:sidebar-provider. The sidebar is one <nav popover>: from 768 px CSS shows it beside the page, below that the
  trigger opens it as a panel through popovertarget, and the browser puts it in the top layer, closes it on Escape or a
  click outside and returns focus to the trigger. This adds:

  Desktop (>= 768 px):
  - the trigger and Ctrl/Cmd+B collapse and expand the sidebar: data-state on the provider, and the sl-sidebar-state
    cookie, which the server reads to render the next page in the same state (a plain cookie, written at once, so a
    link followed right after sees it);
  - the trigger loses popovertarget (browsers would report it as collapsed from the closed popover, and ignore an
    explicit aria-expanded) and gets aria-expanded from data-state; it gets popovertarget back below 768 px. The
    trigger keeps aria-controls, which is how this finds it at both widths, also after htmx swapped it.

  Phone (< 768 px), the panel:
  - when it opens, focus moves into it (the current page's link, else the first one), and everything but the panel and
    its ancestors is inert, which keeps Tab inside and takes the page out of the accessibility tree. inert is set and
    removed on beforetoggle, so it is gone before the browser returns focus to the trigger;
  - Ctrl/Cmd+B opens and closes it;
  - it closes when a link in it is followed (a same-page link, or htmx swapping only the main area), when the viewport
    crosses 768 px, before htmx saves the page in its history cache, and when the page comes back from the back/forward
    cache.

  Both: sl-sidebar-toggle on the provider (bubbles), {state: 'expanded' | 'collapsed', open, mobile}, after a desktop
  change and when the panel opens or closes.

  Events are listened to on the provider (toggle events in the capture phase: they do not bubble), so a sidebar or
  trigger that htmx swaps in keeps working.
*/
import {firstTabbable, hide, listener, show} from '../popup.js';

const COOKIE = 'sl-sidebar-state';
const COOKIE_MAX_AGE = 60 * 60 * 24 * 365;
const TOGGLE_EVENT = 'sl-sidebar-toggle';
const desktop = window.matchMedia('(width >= 48rem)');

export default function sidebar() {
  return {
    init() {
      const provider = this.$el;
      const cleanups = [];
      const listen = listener(cleanups);
      let inerted = [];

      /** The provider's own sidebar, looked up each time: htmx may have swapped it. */
      const sidebarOf = () => Array.from(provider.querySelectorAll('.sidebar'))
          .find(element => element.closest('.sidebar-provider') === provider) ?? null;
      const triggersOf = sidebar => sidebar && sidebar.id
          ? Array.from(document.querySelectorAll(
              `[aria-controls="${CSS.escape(sidebar.id)}"], [popovertarget="${CSS.escape(sidebar.id)}"]`))
          : [];
      const isOpen = sidebar => sidebar.matches(':popover-open');
      const state = () => provider.dataset.state === 'collapsed' ? 'collapsed' : 'expanded';

      const syncTriggers = () => {
        const sidebar = sidebarOf();
        for (const trigger of triggersOf(sidebar)) {
          trigger.setAttribute('aria-controls', sidebar.id);
          if (desktop.matches) {
            trigger.removeAttribute('popovertarget');
            trigger.setAttribute('aria-expanded', String(state() === 'expanded'));
          } else {
            trigger.setAttribute('popovertarget', sidebar.id);
            trigger.setAttribute('aria-expanded', String(isOpen(sidebar)));
          }
        }
      };
      let syncScheduled = false;
      const scheduleSync = () => {
        if (!syncScheduled) {
          syncScheduled = true;
          queueMicrotask(() => {
            syncScheduled = false;
            syncTriggers();
          });
        }
      };

      const announce = open => provider.dispatchEvent(new CustomEvent(TOGGLE_EVENT, {
        bubbles: true, detail: {state: state(), open, mobile: !desktop.matches},
      }));

      const setState = next => {
        provider.dataset.state = next;
        document.cookie = `${COOKIE}=${next}; path=/; max-age=${COOKIE_MAX_AGE}; samesite=lax`;
        syncTriggers();
        announce(false);
      };

      const toggle = source => {
        const sidebar = sidebarOf();
        if (!sidebar) {
          return;
        }
        if (desktop.matches) {
          setState(state() === 'collapsed' ? 'expanded' : 'collapsed');
        } else if (isOpen(sidebar)) {
          sidebar.hidePopover();
        } else {
          show(sidebar, source ?? triggersOf(sidebar).find(trigger => trigger.checkVisibility()));
        }
      };

      const makeInert = sidebar => {
        for (let element = sidebar; element.parentElement && element !== document.body;
             element = element.parentElement) {
          for (const sibling of element.parentElement.children) {
            if (sibling !== element && !sibling.inert && !['SCRIPT', 'STYLE', 'TEMPLATE'].includes(sibling.tagName)) {
              sibling.inert = true;
              inerted.push(sibling);
            }
          }
        }
      };
      const restoreInert = () => {
        inerted.forEach(element => element.inert = false);
        inerted = [];
      };

      const focusIn = sidebar => {
        if (!isOpen(sidebar) || sidebar.contains(document.activeElement)) {
          return;
        }
        const current = sidebar.querySelector('[aria-current="page"]');
        const target = current && current.checkVisibility() ? current : firstTabbable(sidebar);
        if (target) {
          target.focus();
        } else {
          sidebar.tabIndex = -1;
          sidebar.focus();
        }
      };

      listen(provider, 'beforetoggle', event => {
        const sidebar = sidebarOf();
        if (event.target !== sidebar) {
          return;
        }
        if (event.newState === 'open') {
          // On a desktop the sidebar is in the page already: a trigger that still has popovertarget collapses instead.
          if (desktop.matches) {
            event.preventDefault();
            return;
          }
          makeInert(sidebar);
          // A task later: the browser has shown it by then (a native activation shows it after this event).
          setTimeout(() => focusIn(sidebar));
        } else {
          restoreInert();
        }
        for (const trigger of triggersOf(sidebar)) {
          trigger.setAttribute('aria-expanded', String(event.newState === 'open'));
        }
        announce(event.newState === 'open');
      }, true);

      listen(document, 'click', event => {
        const sidebar = sidebarOf();
        if (!sidebar || !(event.target instanceof Element)) {
          return;
        }
        if (desktop.matches) {
          const trigger = event.target.closest(`[aria-controls="${CSS.escape(sidebar.id)}"]`);
          if (trigger && !trigger.matches(':disabled')) {
            toggle(trigger);
          }
        } else if (isOpen(sidebar) && sidebar.contains(event.target) && event.target.closest('a[href]')) {
          hide(sidebar);
        }
      });

      listen(document, 'keydown', event => {
        if ((event.ctrlKey || event.metaKey) && !event.altKey && !event.shiftKey && event.key.toLowerCase() === 'b'
            && !(event.target instanceof Element && event.target.closest('[contenteditable]:not([contenteditable="false"])'))) {
          event.preventDefault();
          toggle();
        }
      });

      listen(desktop, 'change', () => {
        const sidebar = sidebarOf();
        if (sidebar) {
          hide(sidebar);
        }
        syncTriggers();
      });

      const close = () => {
        const sidebar = sidebarOf();
        if (sidebar) {
          hide(sidebar);
        }
      };
      listen(document, 'htmx:beforeHistorySave', close);
      listen(window, 'pageshow', event => {
        if (event.persisted) {
          close();
        }
      });

      // A trigger htmx swaps in (into the inset, or anywhere in the provider) arrives with the server's popovertarget.
      const observer = new MutationObserver(scheduleSync);
      observer.observe(provider, {childList: true, subtree: true});
      cleanups.push(() => observer.disconnect(), restoreInert);

      this.cleanups = cleanups;
      syncTriggers();
    },

    destroy() {
      this.cleanups.forEach(cleanup => cleanup());
    },
  };
}
