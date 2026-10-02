/*
  sl:tooltip: the wrapper around a trigger (its first element) and a sl:tooltip-content, a popover="manual". The
  behaviour is in ../tooltip.js. The delay is data-delay (300 ms by default). A content hidden from assistive technology
  (aria-hidden, which sl:tooltip-content renders for mode="label") repeats the trigger's name, such as an icon button's
  aria-label: it adds no aria-describedby, which would make a screen reader say the name twice.
*/
import {listener} from '../popup.js';
import {attachTooltip} from '../tooltip.js';

const DEFAULT_DELAY_MS = 300;

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
      const self = attachTooltip(trigger, content, {
        delay: Number(wrapper.dataset.delay ?? DEFAULT_DELAY_MS),
        describe: content.getAttribute('aria-hidden') !== 'true',
      }, listener(cleanups));
      cleanups.push(() => self.hide());
      this.cleanups = cleanups;
    },

    destroy() {
      this.cleanups?.forEach(cleanup => cleanup());
    },
  };
}
