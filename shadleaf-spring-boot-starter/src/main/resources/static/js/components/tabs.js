/*
  sl:tabs. The server renders the active tab's panel and hides the others, so the page is right before this runs, and
  a tab with an href is a link that loads the page with that tab active, so the tabs work without it too. This adds
  the ARIA tabs pattern on top:
  - a click on a tab shows its panel in place (a link's address becomes the page's, with history.replaceState, so a
    reload or a shared link keeps the tab; a click with a modifier key still opens the link elsewhere);
  - one tab stop for the list (roving tabindex): the arrow keys move between the tabs, wrapping around and skipping
    disabled ones, Home and End go to the first and last; left and right swap on a right-to-left page, and a vertical
    list uses up and down;
  - automatic activation (a tab shows its panel as soon as it has the focus) or, with data-activation="manual", Enter
    or Space (which a link does not react to by itself);
  - the inactive panels become hidden="until-found" (and leave the tab order), so find-in-page reaches them, and the
    tab of a match becomes active (beforematch);
  - an sl-tabs-show event on a panel each time it is shown, e.g. for hx-trigger="sl-tabs-show once" on a panel that
    loads its content on first view.
  htmx's history cache saves the markup as it is here, active tab included, and the restored markup starts from it.
*/
import {listener} from '../popup.js';

const TAB = '[role="tab"]';

export default function tabs() {
  return {
    init() {
      const root = this.$el;
      const cleanups = [];
      const listen = listener(cleanups);
      const manual = root.dataset.activation === 'manual';
      const vertical = root.dataset.orientation === 'vertical';

      // This component's own tabs and panels, not those of tabs nested in a panel.
      const ownTabs = () => Array.from(root.querySelectorAll(TAB)).filter(tab => tab.closest('.tabs') === root);
      const enabledTabs = () => ownTabs().filter(isEnabled);
      const tabOf = panel => ownTabs().find(tab => panelOf(tab) === panel);

      const activate = (tab, {event = true} = {}) => {
        for (const other of ownTabs()) {
          const active = other === tab;
          other.setAttribute('aria-selected', String(active));
          other.setAttribute('tabindex', active ? '0' : '-1');
          const panel = panelOf(other);
          if (!panel) {
            continue;
          }
          // An until-found panel keeps a box of its own (only its content is skipped), so it leaves the tab order.
          if (active) {
            const shown = panel.hasAttribute('hidden');
            panel.removeAttribute('hidden');
            panel.setAttribute('tabindex', '0');
            if (shown && event) {
              panel.dispatchEvent(new CustomEvent('sl-tabs-show'));
            }
          } else {
            panel.setAttribute('hidden', 'until-found');
            panel.setAttribute('tabindex', '-1');
          }
        }
      };

      const choose = tab => {
        if (tab.getAttribute('aria-selected') === 'true') {
          return;
        }
        activate(tab);
        if (tab instanceof HTMLAnchorElement && tab.href) {
          try {
            history.replaceState(history.state, '', tab.href);
          } catch (error) {
            // A document whose address cannot change (about:srcdoc, another origin): the tab still switches.
            if (!(error instanceof DOMException)) {
              throw error;
            }
          }
        }
      };

      // The server's active tab, or the first enabled one when it named none of them.
      const current = ownTabs().find(tab => tab.getAttribute('aria-selected') === 'true') ?? enabledTabs()[0];
      if (current) {
        activate(current, {event: current.getAttribute('aria-selected') !== 'true'});
      }

      listen(root, 'click', event => {
        const tab = ownTab(event.target, root);
        if (!tab) {
          return;
        }
        if (!isEnabled(tab)) {
          event.preventDefault();
          return;
        }
        if (tab instanceof HTMLAnchorElement
            && (event.button !== 0 || event.ctrlKey || event.metaKey || event.shiftKey || event.altKey)) {
          return;
        }
        event.preventDefault();
        choose(tab);
      });

      listen(root, 'keydown', event => {
        const tab = ownTab(event.target, root);
        if (!tab || event.ctrlKey || event.metaKey || event.altKey) {
          return;
        }
        const tabs = enabledTabs();
        const index = tabs.indexOf(tab);
        const step = offset => tabs[(index + offset + tabs.length) % tabs.length];
        const forward = getComputedStyle(root).direction === 'rtl' ? -1 : 1;
        let next;
        switch (event.key) {
          case 'ArrowRight':
            next = vertical ? undefined : step(forward);
            break;
          case 'ArrowLeft':
            next = vertical ? undefined : step(-forward);
            break;
          case 'ArrowDown':
            next = vertical ? step(1) : undefined;
            break;
          case 'ArrowUp':
            next = vertical ? step(-1) : undefined;
            break;
          case 'Home':
            next = tabs[0];
            break;
          case 'End':
            next = tabs.at(-1);
            break;
          case ' ':
            // A button chooses itself on Space (a click); a link does not.
            if (tab instanceof HTMLAnchorElement) {
              event.preventDefault();
              choose(tab);
            }
            return;
          default:
            return;
        }
        if (next) {
          event.preventDefault();
          if (manual) {
            // The focus moves, the panel stays: roving tabindex follows the focus.
            ownTabs().forEach(other => other.setAttribute('tabindex', other === next ? '0' : '-1'));
          }
          next.focus();
          if (!manual) {
            choose(next);
          }
        }
      });

      // With manual activation, a tab that loses the focus without being chosen hands the tab stop back to the active one.
      listen(root, 'focusout', event => {
        if (!manual || !ownTab(event.target, root) || ownTab(event.relatedTarget, root)) {
          return;
        }
        const active = ownTabs().find(tab => tab.getAttribute('aria-selected') === 'true');
        ownTabs().forEach(other => other.setAttribute('tabindex', other === active ? '0' : '-1'));
      });

      // Find-in-page (or a link to an element) found a match in an inactive panel. Capture: whether it bubbles
      // differs between browsers' early implementations.
      listen(root, 'beforematch', event => {
        // The match can be in a panel of tabs nested in one of these panels: look outwards for one of ours.
        let panel = event.target instanceof Element ? event.target.closest('[role="tabpanel"]') : null;
        while (panel && !tabOf(panel)) {
          panel = panel.parentElement?.closest('[role="tabpanel"]') ?? null;
        }
        if (panel) {
          choose(tabOf(panel));
        }
      }, true);

      this.cleanups = cleanups;
    },

    destroy() {
      this.cleanups.forEach(cleanup => cleanup());
    },
  };
}

function ownTab(target, root) {
  const tab = target instanceof Element ? target.closest(TAB) : null;
  return tab && tab.closest('.tabs') === root ? tab : null;
}

function panelOf(tab) {
  const id = tab.getAttribute('aria-controls');
  return id ? document.getElementById(id) : null;
}

function isEnabled(tab) {
  return !tab.matches(':disabled, [aria-disabled="true"]');
}
