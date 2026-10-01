/*
  sl:table-selection-count. Shows "2 of 20 selected" for the table whose id is in data-table: counted when it starts,
  then on every sl-table-selection-change from that table (sl:table-select-all fires it after each change, also when
  rows are swapped in) and on every change event of one of its rows (for a table without select-all). It finds the
  table by id each time, so a table htmx replaced is counted, and a count outside the swapped part follows it.
*/
import {CHANGE_EVENT, countText, rowCheckboxes, selection} from '../table-rows.js';

export default function tableSelectionCount() {
  return {
    init() {
      const root = this.$el;
      const table = () => document.getElementById(root.dataset.table);
      const show = current => root.textContent = countText(root.dataset.format, current);
      const selectionChange = event => {
        if (event.target === table()) {
          show(event.detail);
        }
      };
      const change = event => {
        const current = table();
        if (current && rowCheckboxes(current).includes(event.target)) {
          show(selection(current));
        }
      };
      document.addEventListener(CHANGE_EVENT, selectionChange);
      document.addEventListener('change', change);
      this.cleanups = [
        () => document.removeEventListener(CHANGE_EVENT, selectionChange),
        () => document.removeEventListener('change', change),
      ];
      const current = table();
      if (current) {
        show(selection(current));
      }
    },

    destroy() {
      this.cleanups?.forEach(cleanup => cleanup());
    },
  };
}
