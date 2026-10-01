/*
  sl:table-select-all. Keeps the checkbox checked when every row of its table is selected, indeterminate when some
  are, unchecked when none are (disabled when the table has no rows), and checks or unchecks every row when it is
  clicked. The rows stay the source of truth: they are plain checkboxes in a form, which submit without this.

  It follows the rows through the change events they fire (Space or a click), a form reset, and rows being added or
  replaced (an htmx swap of the body; a swap of the whole table starts a new one). After each change it fires
  sl-table-selection-change ({selected, total}, bubbling) on the table, for sl:table-selection-count and for the
  application (a bulk action button). Changing the rows from here fires input and change on each one that changes,
  as if the user had clicked it.

  The count is announced (the status region in the template) only after a click on this checkbox, which changes rows
  the user did not touch; a row's own checkbox already announces its state, so counting on every click would be noise.
*/
import {CHANGE_EVENT, countText, rowCheckboxes, selection} from '../table-rows.js';

export default function tableSelection() {
  return {
    init() {
      const root = this.$el;
      const table = root.closest('table');
      if (!table) {
        console.error('Shadleaf: sl:table-select-all must be inside the table whose rows it selects.');
        return;
      }
      const checkbox = root.querySelector('input[type="checkbox"]');
      const status = root.querySelector('[role="status"]');
      const name = root.dataset.name;
      let selecting = false;
      let announceTimer;

      const update = () => {
        const current = selection(table, name);
        checkbox.checked = current.total > 0 && current.selected === current.total;
        checkbox.indeterminate = current.selected > 0 && current.selected < current.total;
        checkbox.disabled = current.total === 0;
        table.dispatchEvent(new CustomEvent(CHANGE_EVENT, {bubbles: true, detail: current}));
        return current;
      };
      const announce = current => {
        // Emptied first, then filled after a moment, so the same text twice is announced twice.
        status.textContent = '';
        clearTimeout(announceTimer);
        announceTimer = setTimeout(() => status.textContent = countText(root.dataset.format, current), 150);
      };
      const selectAll = () => {
        selecting = true;
        for (const row of rowCheckboxes(table, name)) {
          if (row.checked !== checkbox.checked) {
            row.checked = checkbox.checked;
            row.dispatchEvent(new Event('input', {bubbles: true}));
            row.dispatchEvent(new Event('change', {bubbles: true}));
          }
        }
        selecting = false;
        announce(update());
      };
      const change = event => {
        if (event.target === checkbox) {
          selectAll();
        } else if (!selecting && rowCheckboxes(table, name).includes(event.target)) {
          update();
        }
      };
      // reset fires before the form puts the values back.
      const reset = () => setTimeout(update);
      const observer = new MutationObserver(records => {
        if (records.some(record => record.target !== root && !root.contains(record.target))) {
          update();
        }
      });

      table.addEventListener('change', change);
      document.addEventListener('reset', reset, true);
      observer.observe(table, {childList: true, subtree: true, attributes: true, attributeFilter: ['disabled']});
      this.cleanups = [
        () => table.removeEventListener('change', change),
        () => document.removeEventListener('reset', reset, true),
        () => observer.disconnect(),
        () => clearTimeout(announceTimer),
      ];
      update();
    },

    destroy() {
      this.cleanups?.forEach(cleanup => cleanup());
    },
  };
}
