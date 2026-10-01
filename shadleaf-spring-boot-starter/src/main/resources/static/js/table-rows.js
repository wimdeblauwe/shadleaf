/*
  The row checkboxes of a table, shared by slTableSelection (sl:table-select-all) and slTableSelectionCount
  (sl:table-selection-count). Imported, not registered.

  A row checkbox is any checkbox in a cell of the table's body (not of a nested table) that can be submitted: a
  disabled one sends nothing, so it is neither selected nor counted. sl:table-select-all's name prop narrows them to
  that name.
*/
export const CHANGE_EVENT = 'sl-table-selection-change';

export function rowCheckboxes(table, name = table.querySelector(':scope > thead .table-select-all')?.dataset.name) {
  return [...table.querySelectorAll(':scope > tbody > tr > :is(td, th) input[type="checkbox"]')]
      .filter(checkbox => checkbox.closest('table') === table
          && !checkbox.matches(':disabled')
          && (!name || checkbox.name === name));
}

export function selection(table, name) {
  const rows = rowCheckboxes(table, name);
  return {selected: rows.filter(row => row.checked).length, total: rows.length};
}

/** The message sl.table.selection.count, rendered with {0} and {1} left in, filled in with the page's numbers. */
export function countText(format, {selected, total}) {
  const numbers = new Intl.NumberFormat(document.documentElement.lang || undefined);
  return format.replace('{0}', numbers.format(selected)).replace('{1}', numbers.format(total));
}
