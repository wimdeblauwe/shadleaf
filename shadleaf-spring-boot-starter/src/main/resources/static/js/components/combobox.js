/*
  sl:combobox. The server renders a real select, which submits, binds with th:field and is drawn like sl:native-select
  before Alpine runs and without it. This hides the select and puts in its place what the combobox's template holds:
  a text input with role="combobox" (and, for a multiple select, a chip per chosen option before it) and a list box in
  a manual popover, placed by popup.css, built from the select's options, groups and separators.

  - Typing opens the list and filters it: the options whose text contains what was typed, ignoring case and accents.
    A group is hidden while none of its options match, a separator while anything is typed. With search-url, the
    server filters instead (below).
  - The keyboard follows the ARIA editable combobox pattern with list autocomplete: focus stays in the input
    (aria-activedescendant). Down and Up open the list and move through it, Alt+Down opens it without moving, Page
    Down/Up move by ten, Enter chooses (Alt+Up too, closing the list), Escape closes it, and Tab moves on without
    choosing. Home and End move the caret, as in any text field. In a multiple combobox Enter toggles and the list
    stays open; Backspace in the empty input removes the last chip, and Left from its start goes to the chips' remove
    buttons (Left and Right between them, Backspace or Delete removes one).
  - A click on the input opens the list, the chevron toggles it, a click outside closes it. The clear button, the
    chevron and the remove buttons stay out of the tab order: each has a key.
  - A single combobox shows the chosen option's text. Closing the list puts it back when it was changed without
    choosing, and clears the choice when it was emptied.
  - The select stays the source of truth: choosing sets its options' selected and fires input and change on it, so
    th:field, submitting, htmx triggers and the application's listeners see what they would see from the select. The
    input follows the select: its value (on change and input, and after a form reset), its label (the select's labels,
    aria-labelledby or aria-label), aria-describedby, aria-invalid, required and disabled, and the options when they
    change. Focus that reaches the hidden select (a click on its label, autofocus, the browser's validation) moves on
    to the input.
  - The number of matches is announced in a live region once typing pauses.

  With search-url (data-search-url), typing asks the server for the options, 300 ms after the last key: a GET with the
  text as q, answered with the HTML of options (sl:combobox-item), groups and separators. They replace the options
  that are not chosen; a chosen option the answer leaves out stays, hidden from the list, so the value stays. A newer
  query aborts the request before it. An empty input brings back the options the page came with. Before each request
  sl-combobox-search fires on the select, cancelable, with detail.query and detail.url (a URL, to add parameters to).
  An answer that is no 2xx, or that was redirected (a session that expired and sent the request to the login page),
  shows an error in the list.

  htmx's history cache saves the markup this adds; a restored combobox removes it and starts again.
*/
import {hide, listener, positionWithoutAnchoring, show} from '../popup.js';

const SEARCH_DELAY_MS = 300;
const ANNOUNCE_DELAY_MS = 500;
const PAGE_STEP = 10;
const MIRRORED = ['aria-describedby', 'aria-invalid', 'aria-label', 'aria-labelledby', 'disabled', 'required'];
let instances = 0;

export default function combobox() {
  return {
    // Declared, so init() sets it here: Alpine puts a property no scope has on the outermost x-data, where every
    // component nested in that one (all of them, inside sl:sidebar-provider) would share it.
    cleanups: [],

    init() {
      const root = this.$el;
      const select = root.querySelector(':scope > select');
      const template = root.querySelector(':scope > template.combobox-template');
      this.cleanups = [];
      if (select && template) {
        setUp(root, select, template, listener(this.cleanups), this.cleanups);
      }
    },

    destroy() {
      this.cleanups.forEach(cleanup => cleanup());
    },
  };
}

function setUp(root, select, template, listen, cleanups) {
  const instance = ++instances;
  const id = select.id || `sl-combobox-${instance}`;
  // Markup from an earlier run, such as a page htmx restored from its history cache.
  root.querySelectorAll(':scope > .combobox-control, :scope > .combobox-content, :scope > .combobox-status')
      .forEach(element => element.remove());

  const parts = template.content.cloneNode(true);
  const control = parts.querySelector('.combobox-control');
  const input = control.querySelector('.combobox-input');
  const clear = control.querySelector('.combobox-clear');
  const trigger = control.querySelector('.combobox-trigger');
  const content = parts.querySelector('.combobox-content');
  const listbox = content.querySelector('.combobox-list');
  const empty = content.querySelector('.combobox-empty');
  const status = parts.querySelector('.combobox-status');
  // Top-level elements of the template, which :scope cannot reach in a fragment.
  const chipPrototype = Array.from(parts.children).find(element => element.matches('.combobox-chip'));
  const indicator = Array.from(parts.children).find(element => element.matches('.combobox-item-indicator'));

  const multiple = select.multiple;
  const autoHighlight = root.dataset.autoHighlight === 'true';
  const searchUrl = root.dataset.searchUrl;
  const placeholder = input.placeholder;
  const emptyText = empty.textContent;

  input.id = `${id}-input`;
  listbox.id = `${id}-listbox`;
  content.id = `${id}-content`;
  input.setAttribute('aria-controls', listbox.id);
  trigger?.setAttribute('aria-controls', listbox.id);
  if (multiple) {
    listbox.setAttribute('aria-multiselectable', 'true');
  }
  // An explicit anchor: the popover is opened from script, which not every browser lets give a source.
  const anchor = `--sl-combobox-${instance}`;
  control.style.anchorName = anchor;
  content.style.positionAnchor = anchor;

  root.append(control, content, status);
  select.tabIndex = -1;
  select.setAttribute('aria-hidden', 'true');
  root.dataset.enhanced = 'true';

  const optionOf = new Map();
  let rows = [];
  let active = null;
  // What was typed since the list opened; null while the list shows every option.
  let query = null;
  let state = 'idle';
  let searched = false;
  const initialOptions = searchUrl ? Array.from(select.children).map(child => child.cloneNode(true)) : [];
  let searchTimer = 0;
  let announceTimer = 0;
  let request = null;

  const isOpen = () => content.matches(':popover-open');
  const realOptions = () => Array.from(select.options).filter(option => option.dataset.placeholder === undefined);
  const chosenOptions = () => realOptions().filter(option => option.selected);
  const enabledRows = () => rows.filter(row => !row.hidden && row.getAttribute('aria-disabled') !== 'true');
  const visibleRows = () => rows.filter(row => !row.hidden);

  const render = () => {
    optionOf.clear();
    rows = [];
    listbox.replaceChildren();
    let index = 0;
    const addOption = (parent, option) => {
      if (option.hidden) {
        return;
      }
      const row = document.createElement('div');
      row.className = option.className || 'combobox-item';
      row.id = `${listbox.id}-${index++}`;
      row.setAttribute('role', 'option');
      if (option.matches(':disabled')) {
        row.setAttribute('aria-disabled', 'true');
      }
      row.append(...contentOf(option));
      if (indicator) {
        row.append(indicator.cloneNode(true));
      }
      optionOf.set(row, option);
      rows.push(row);
      parent.append(row);
    };
    for (const child of select.children) {
      if (child instanceof HTMLOptionElement) {
        addOption(listbox, child);
      } else if (child instanceof HTMLOptGroupElement) {
        const group = document.createElement('div');
        group.className = child.className || 'combobox-group';
        group.setAttribute('role', 'group');
        const label = document.createElement('div');
        label.className = 'combobox-label';
        label.id = `${listbox.id}-group-${index}`;
        label.textContent = child.label;
        group.setAttribute('aria-labelledby', label.id);
        group.append(label);
        Array.from(child.children).filter(option => option instanceof HTMLOptionElement)
            .forEach(option => addOption(group, option));
        listbox.append(group);
      } else if (child instanceof HTMLHRElement) {
        const separator = document.createElement('div');
        separator.className = child.className || 'combobox-separator';
        separator.setAttribute('aria-hidden', 'true');
        listbox.append(separator);
      }
    }
    active = null;
    filter();
  };

  /** Shows the rows that match the query (all of them when the server filtered), and the empty or busy message. */
  const filter = () => {
    const search = searchUrl || !query ? '' : normalize(query);
    for (const row of rows) {
      row.hidden = search !== '' && !normalize(optionOf.get(row).label).includes(search);
    }
    listbox.querySelectorAll(':scope > [role="group"]').forEach(group => {
      group.hidden = !group.querySelector('[role="option"]:not([hidden])');
    });
    listbox.querySelectorAll(':scope > .combobox-separator').forEach(separator => {
      separator.hidden = Boolean(query);
    });
    const count = visibleRows().length;
    const message = state === 'searching' ? root.dataset.searching
        : state === 'error' ? root.dataset.searchError
            : count === 0 ? emptyText : null;
    empty.textContent = message ?? emptyText;
    empty.hidden = message === null;
    listbox.hidden = count === 0;
    content.toggleAttribute('data-empty', count === 0);
    if (active && active.hidden) {
      active = null;
    }
    if (query && autoHighlight) {
      setActive(enabledRows()[0] ?? null);
    } else {
      setActive(active, false);
    }
    if (isOpen()) {
      content.dispatchEvent(new Event('sl-popup-place'));
    }
  };

  const announce = () => {
    clearTimeout(announceTimer);
    announceTimer = setTimeout(() => {
      const count = visibleRows().length;
      status.textContent = state === 'error' ? root.dataset.searchError
          : count === 0 ? emptyText
              : count === 1 ? root.dataset.result : root.dataset.results.replace('{0}', String(count));
    }, ANNOUNCE_DELAY_MS);
  };

  const setActive = (row, scroll = true) => {
    rows.forEach(each => each.toggleAttribute('data-active', each === row));
    if (row) {
      input.setAttribute('aria-activedescendant', row.id);
      if (scroll) {
        row.scrollIntoView({block: 'nearest'});
      }
    } else {
      input.removeAttribute('aria-activedescendant');
    }
    active = row;
  };

  const chosenRow = () => enabledRows().find(row => optionOf.get(row).selected);

  const sync = () => {
    const disabled = select.disabled;
    input.disabled = disabled;
    // The chips fade with the control: text of a disabled control, which needs no contrast.
    if (disabled) {
      control.setAttribute('aria-disabled', 'true');
    } else {
      control.removeAttribute('aria-disabled');
    }
    for (const button of [clear, trigger]) {
      if (button) {
        button.disabled = disabled;
      }
    }
    for (const name of ['aria-describedby', 'aria-invalid']) {
      mirror(select, input, name);
    }
    if (select.required) {
      input.setAttribute('aria-required', 'true');
    } else {
      input.removeAttribute('aria-required');
    }
    const labelledBy = select.getAttribute('aria-labelledby') || labelIds(select, id);
    for (const element of [input, listbox]) {
      if (labelledBy) {
        element.setAttribute('aria-labelledby', labelledBy);
        element.removeAttribute('aria-label');
      } else {
        element.removeAttribute('aria-labelledby');
        mirror(select, element, 'aria-label');
      }
    }
    const chosen = chosenOptions();
    if (multiple) {
      renderChips(chosen, disabled);
      input.placeholder = chosen.length ? '' : placeholder;
    } else if (query === null) {
      input.value = chosen[0] ? chosen[0].label : '';
    }
    if (clear) {
      clear.hidden = chosen.length === 0 && input.value === '';
    }
    rows.forEach(row => row.setAttribute('aria-selected', String(optionOf.get(row).selected)));
  };

  const renderChips = (chosen, disabled) => {
    // Removing a focused chip would blur it while it is removed, and focusout closes the list, rendering the chips.
    if (document.activeElement?.closest('.combobox-chip') && control.contains(document.activeElement)) {
      input.focus({preventScroll: true});
    }
    control.querySelectorAll(':scope > .combobox-chip').forEach(chip => chip.remove());
    if (!chipPrototype) {
      return;
    }
    for (const option of chosen) {
      const chip = chipPrototype.cloneNode(true);
      chip.querySelector('.combobox-chip-label').textContent = option.label;
      const remove = chip.querySelector('.combobox-chip-remove');
      remove.setAttribute('aria-label', root.dataset.removeLabel.replace('{0}', option.label));
      remove.disabled = disabled;
      chip.dataset.value = option.value;
      input.before(chip);
    }
  };

  const changed = () => {
    select.dispatchEvent(new Event('input', {bubbles: true}));
    select.dispatchEvent(new Event('change', {bubbles: true}));
  };

  const choose = row => {
    const option = row ? optionOf.get(row) : null;
    if (!option || option.matches(':disabled')) {
      return;
    }
    if (multiple) {
      option.selected = !option.selected;
      changed();
      if (query !== null) {
        input.value = '';
        query = null;
        restoreOptions();
        filter();
      }
      setActive(rows.includes(row) ? row : null, false);
      return;
    }
    if (!option.selected) {
      option.selected = true;
      changed();
    }
    query = null;
    restoreOptions();
    close();
  };

  const clearChoice = () => {
    const chosen = chosenOptions();
    chosen.forEach(option => {
      option.selected = false;
    });
    const none = select.querySelector(':scope > option[data-placeholder]');
    if (none) {
      none.selected = true;
    }
    input.value = '';
    query = null;
    restoreOptions();
    if (chosen.length) {
      changed();
    }
    sync();
    filter();
  };

  const open = row => {
    if (input.disabled) {
      return;
    }
    if (!isOpen()) {
      if (!supportsAnchoring()) {
        // anchor-size() goes with anchor positioning: the list is at least as wide as the input.
        content.style.minInlineSize = `${control.offsetWidth}px`;
      }
      show(content, control);
    }
    if (row !== undefined) {
      setActive(row);
    }
  };

  /** Closes the list. A single combobox shows its choice again, or loses it when the text was emptied. */
  const close = () => {
    hide(content);
    if (query !== null) {
      if (!multiple && input.value.trim() === '' && chosenOptions().length) {
        clearChoice();
        return;
      }
      if (multiple) {
        input.value = '';
      }
      query = null;
      restoreOptions();
    }
    sync();
    filter();
  };

  /** After a search, the options the page came with. */
  const restoreOptions = () => {
    if (searchUrl && searched) {
      searched = false;
      search('');
    }
  };

  const search = text => {
    clearTimeout(searchTimer);
    request?.abort();
    request = null;
    listbox.removeAttribute('aria-busy');
    if (text.trim() === '') {
      state = 'idle';
      replaceOptions(select, initialOptions.map(child => child.cloneNode(true)), false);
      return;
    }
    state = 'searching';
    searched = true;
    filter();
    searchTimer = setTimeout(() => {
      const url = new URL(searchUrl, document.baseURI);
      url.searchParams.set('q', text);
      const event = new CustomEvent('sl-combobox-search', {bubbles: true, cancelable: true, detail: {query: text, url}});
      if (!select.dispatchEvent(event)) {
        state = 'idle';
        filter();
        return;
      }
      const controller = new AbortController();
      request = controller;
      listbox.setAttribute('aria-busy', 'true');
      fetch(event.detail.url, {headers: {Accept: 'text/html'}, signal: controller.signal})
          .then(response => {
            if (!response.ok || response.redirected) {
              throw new Error(`${response.status} from ${response.url}`);
            }
            return response.text();
          })
          .then(html => {
            if (request !== controller) {
              return;
            }
            const results = document.createElement('template');
            results.innerHTML = html;
            state = 'idle';
            replaceOptions(select, Array.from(results.content.children), true);
          })
          .catch(error => {
            if (error.name === 'AbortError' || request !== controller) {
              return;
            }
            state = 'error';
            filter();
            announce();
          })
          .finally(() => {
            if (request === controller) {
              request = null;
              listbox.removeAttribute('aria-busy');
            }
          });
    }, SEARCH_DELAY_MS);
  };

  render();
  sync();

  const observer = new MutationObserver(records => {
    const optionsChanged = records.some(record => record.type !== 'attributes' || record.target !== select);
    if (optionsChanged) {
      render();
      if (query) {
        announce();
      }
    }
    sync();
  });
  observer.observe(select, {attributes: true, attributeFilter: MIRRORED, childList: true, subtree: true,
    characterData: true});
  cleanups.push(() => {
    observer.disconnect();
    clearTimeout(searchTimer);
    clearTimeout(announceTimer);
    request?.abort();
  });

  positionWithoutAnchoring(content, () => control, {side: 'bottom', align: 'start'}, listen);

  listen(content, 'toggle', event => {
    const expanded = String(event.newState === 'open');
    input.setAttribute('aria-expanded', expanded);
    trigger?.setAttribute('aria-expanded', expanded);
    if (event.newState !== 'open') {
      setActive(null);
    }
  });

  listen(select, 'change', sync);
  listen(select, 'input', sync);
  listen(select, 'focus', () => input.focus());
  if (select.form) {
    // The reset event comes before the form resets its controls.
    listen(select.form, 'reset', () => setTimeout(() => {
      query = null;
      input.value = '';
      sync();
      filter();
    }));
  }
  listen(document, 'htmx:beforeHistorySave', () => hide(content));
  if (document.activeElement === select) {
    input.focus();
  }

  listen(input, 'input', () => {
    query = input.value;
    if (clear) {
      clear.hidden = chosenOptions().length === 0 && input.value === '';
    }
    open();
    if (searchUrl) {
      search(query);
    } else {
      filter();
      announce();
    }
  });

  listen(input, 'click', () => open());

  listen(input, 'keydown', event => {
    if (event.ctrlKey || event.metaKey || event.isComposing) {
      return;
    }
    const enabled = enabledRows();
    const index = enabled.indexOf(active);
    if (!isOpen()) {
      switch (event.key) {
        case 'ArrowDown':
          event.preventDefault();
          open(event.altKey ? null : chosenRow() ?? enabled[0] ?? null);
          return;
        case 'ArrowUp':
          event.preventDefault();
          open(chosenRow() ?? enabled.at(-1) ?? null);
          return;
        case 'Escape':
          if (multiple && input.value !== '') {
            event.preventDefault();
            input.value = '';
            query = '';
            close();
          }
          return;
        case 'Backspace':
          removeLastChip(event);
          return;
        case 'ArrowLeft':
          focusChipFromInput(event);
          return;
        default:
          return;
      }
    }

    let next;
    switch (event.key) {
      case 'ArrowDown':
        next = enabled[Math.min(index + 1, enabled.length - 1)];
        break;
      case 'ArrowUp':
        if (event.altKey) {
          event.preventDefault();
          if (active) {
            choose(active);
          }
          if (multiple || !active) {
            close();
          }
          return;
        }
        next = index < 0 ? enabled.at(-1) : enabled[Math.max(index - 1, 0)];
        break;
      case 'PageDown':
        next = enabled[Math.min(Math.max(index, 0) + PAGE_STEP, enabled.length - 1)];
        break;
      case 'PageUp':
        next = enabled[Math.max(index - PAGE_STEP, 0)];
        break;
      case 'Enter':
        if (active) {
          event.preventDefault();
          choose(active);
        }
        return;
      case 'Escape':
        // Not the dialog or sheet around the combobox too.
        event.preventDefault();
        event.stopPropagation();
        close();
        return;
      case 'Tab':
        close();
        return;
      case 'Backspace':
        removeLastChip(event);
        return;
      case 'ArrowLeft':
        focusChipFromInput(event);
        return;
      default:
        return;
    }
    if (next) {
      event.preventDefault();
      setActive(next);
    }
  });

  listen(input, 'focusout', event => {
    if (!root.contains(event.relatedTarget)) {
      close();
    }
  });

  const chipButtons = () => Array.from(control.querySelectorAll(':scope > .combobox-chip > .combobox-chip-remove'));

  const removeChip = button => {
    const option = chosenOptions().find(each => each.value === button.parentElement.dataset.value);
    if (option) {
      option.selected = false;
      changed();
    }
  };

  function removeLastChip(event) {
    const last = chipButtons().at(-1);
    if (multiple && last && input.value === '' && !last.disabled) {
      event.preventDefault();
      removeChip(last);
    }
  }

  function focusChipFromInput(event) {
    const last = chipButtons().at(-1);
    if (multiple && last && input.selectionStart === 0 && input.selectionEnd === 0 && !last.disabled) {
      event.preventDefault();
      hide(content);
      last.focus();
    }
  }

  listen(control, 'keydown', event => {
    const button = event.target instanceof Element ? event.target.closest('.combobox-chip-remove') : null;
    if (!button) {
      return;
    }
    const buttons = chipButtons();
    const index = buttons.indexOf(button);
    switch (event.key) {
      case 'ArrowLeft':
        event.preventDefault();
        buttons[Math.max(index - 1, 0)].focus();
        return;
      case 'ArrowRight':
        event.preventDefault();
        (buttons[index + 1] ?? input).focus();
        return;
      case 'Backspace':
      case 'Delete': {
        event.preventDefault();
        removeChip(button);
        const remaining = chipButtons();
        (remaining[Math.max(index - 1, 0)] ?? input).focus();
        return;
      }
      default:
    }
  });

  listen(control, 'focusout', event => {
    if (event.target !== input && !root.contains(event.relatedTarget)) {
      close();
    }
  });

  // The input keeps the focus while the pointer works the buttons and the list.
  listen(control, 'mousedown', event => {
    if (event.target !== input && !input.disabled) {
      event.preventDefault();
      if (document.activeElement !== input) {
        input.focus();
      }
    }
  });

  listen(control, 'click', event => {
    const target = event.target instanceof Element ? event.target : null;
    const remove = target?.closest('.combobox-chip-remove');
    if (remove) {
      removeChip(remove);
      input.focus();
    } else if (clear && target?.closest('.combobox-clear')) {
      clearChoice();
      input.focus();
    } else if (trigger && target?.closest('.combobox-trigger')) {
      if (isOpen()) {
        close();
      } else {
        open(chosenRow() ?? null);
      }
    } else if (target !== input) {
      open();
    }
  });

  listen(content, 'mousedown', event => event.preventDefault());
  listen(content, 'pointermove', event => {
    const row = event.target instanceof Element ? event.target.closest('[role="option"]') : null;
    if (row && row !== active && row.getAttribute('aria-disabled') !== 'true') {
      setActive(row, false);
    }
  });
  listen(content, 'click', event => {
    const row = event.target instanceof Element ? event.target.closest('[role="option"]') : null;
    if (row && row.getAttribute('aria-disabled') !== 'true') {
      choose(row);
    }
  });
}

/**
 * Puts `children` (options, groups and separators) in the select in place of its options that are not chosen. A
 * chosen option keeps its place among them when they hold its value, and stays after them otherwise, so the select's
 * value does not change: hidden from the list for the results of a search (`hideKept`), shown with the page's options.
 */
function replaceOptions(select, children, hideKept) {
  const chosen = Array.from(select.options).filter(option => option.selected && option.dataset.placeholder === undefined);
  const chosenValues = new Set(chosen.map(option => option.value));
  const placeholder = select.querySelector(':scope > option[data-placeholder]');
  const fragment = document.createDocumentFragment();
  const found = new Set();
  for (const child of children) {
    if (!(child instanceof HTMLOptionElement || child instanceof HTMLOptGroupElement || child instanceof HTMLHRElement)) {
      continue;
    }
    const options = child instanceof HTMLOptionElement ? [child] : Array.from(child.querySelectorAll('option'));
    options.forEach(option => {
      if (chosenValues.has(option.value)) {
        found.add(option.value);
      }
      option.removeAttribute('selected');
    });
    fragment.append(child);
  }
  const kept = chosen.filter(option => !found.has(option.value));
  kept.forEach(option => {
    option.hidden = hideKept;
  });
  select.replaceChildren(...(placeholder ? [placeholder] : []), fragment, ...kept);
  for (const option of select.options) {
    if (chosenValues.has(option.value) && option.dataset.placeholder === undefined) {
      option.selected = true;
    }
  }
  if (!select.multiple && chosen.length === 0 && placeholder) {
    placeholder.selected = true;
  }
}

/** Read each time, so a test can switch it off. */
function supportsAnchoring() {
  return CSS.supports('position-area', 'block-end');
}

/** Lower case, without accents: what the filter compares. */
function normalize(text) {
  return text.normalize('NFD').replace(/\p{Diacritic}/gu, '').toLowerCase().trim();
}

/** An option's content, or its text when the browser kept none of its markup. */
function contentOf(option) {
  const nodes = Array.from(option.childNodes).map(node => node.cloneNode(true));
  if (!nodes.some(node => node.textContent.trim() !== '' || node instanceof Element)) {
    return [document.createTextNode(option.label)];
  }
  return nodes;
}

function mirror(from, to, name) {
  const value = from.getAttribute(name);
  if (value === null) {
    to.removeAttribute(name);
  } else {
    to.setAttribute(name, value);
  }
}

/** The ids of the select's labels, given ids where they have none. */
function labelIds(select, id) {
  return Array.from(select.labels ?? []).map((label, index) => {
    if (!label.id) {
      label.id = `${id}-label${index === 0 ? '' : index + 1}`;
    }
    return label.id;
  }).join(' ');
}
