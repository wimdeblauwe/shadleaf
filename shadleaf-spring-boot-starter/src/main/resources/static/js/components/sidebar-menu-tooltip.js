/*
  sl:sidebar-menu-button's tooltip: a sl:tooltip-content inside the button (or the summary of a collapsible item), which
  is its trigger. The behaviour is sl:tooltip's (../tooltip.js) in label mode: it repeats the button's name, so it is
  hidden from assistive technology and describes nothing. It shows only while the sidebar is collapsed to icons on a
  desktop, where the label is cut off. A click on it would reach the button it lies in, so it does nothing.
  A menu button that opens a dropdown menu (sl:dropdown-menu-trigger as="sidebar-menu-button") never shows its tooltip
  and its menu together: the tooltip hides when the menu opens and does not show while it is open.
*/
import {listener} from '../popup.js';
import {attachTooltip} from '../tooltip.js';

const DELAY_MS = 300;
const desktop = window.matchMedia('(width >= 48rem)');

export default function sidebarMenuTooltip() {
  return {
    init() {
      const content = this.$el;
      const trigger = content.parentElement;
      if (!trigger) {
        return;
      }
      const cleanups = [];
      const listen = listener(cleanups);
      const canShow = () => desktop.matches
          && trigger.closest('.sidebar')?.dataset.collapsible === 'icon'
          && trigger.closest('.sidebar')?.dataset.placement !== 'header'
          && trigger.closest('.sidebar-provider')?.dataset.state === 'collapsed'
          && trigger.getAttribute('aria-expanded') !== 'true';
      const self = attachTooltip(trigger, content, {delay: DELAY_MS, describe: false, when: canShow}, listen);
      listen(content, 'click', event => {
        event.preventDefault();
        event.stopPropagation();
      });
      // Expanding (the trigger, Ctrl/Cmd+B, a group's icon) hides a tooltip that is showing.
      listen(document, 'sl-sidebar-toggle', () => {
        if (!canShow()) {
          self.hide();
        }
      });
      // beforetoggle does not bubble: caught on its way down to the menu the trigger opens.
      listen(document, 'beforetoggle', event => {
        if (event.newState === 'open' && event.target instanceof Element && event.target.id
            && event.target.id === trigger.getAttribute('popovertarget')) {
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
