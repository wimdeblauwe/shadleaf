/*
  sl:select. The server renders a real select, which submits, binds with th:field and takes the keyboard before Alpine
  runs and without it. What this adds depends on the browser:

  - With the customizable select (appearance: base-select; Chrome and Edge 135, Safari 27), the browser draws the
    list and handles nearly everything. This adds the keys it leaves out of the ARIA select-only combobox pattern:
    Enter, Home and End open the list (the last two on the first or last option), and the option under the pointer
    takes the focus, so pointer and keyboard highlight the same way. After a form reset it makes Chrome show the
    reset value: its selectedcontent kept the option chosen before (Chrome 153).

  - Without it (Firefox at the time of writing), it hides the select and puts a button with role="combobox" and a list
    box (a popover, placed by popup.css) in its place, built from the select's options, groups and separators. The
    keyboard follows the pattern: arrows, Home/End, Page Up/Down, typeahead, Enter/Space (and Tab) choose, Escape and
    a click outside close without choosing, and focus stays on the button (aria-activedescendant). The select stays
    the source of truth: choosing sets its value and fires input and change on it, so th:field, submitting, htmx
    triggers and the application's listeners see what they would see from the select. The button follows the select:
    its value (on change and input, and after a form reset), its label (the select's labels, aria-labelledby or
    aria-label), aria-describedby, aria-invalid, required and disabled, and the options when they change. Focus that
    reaches the hidden select (a click on its label, autofocus, the browser's validation) moves on to the button.

  A multiple select is left alone: it is a list box already.
*/
import {hide, listener, positionWithoutAnchoring, show, trackExpanded, typeaheadMatch} from '../popup.js';

const TYPEAHEAD_RESET_MS = 500;
const PAGE_STEP = 10;
const MIRRORED = ['aria-describedby', 'aria-invalid', 'aria-label', 'aria-labelledby', 'disabled', 'required'];
let instances = 0;

export default function select() {
  return {
    init() {
      const wrapper = this.$el;
      const native = wrapper.querySelector(':scope > select');
      this.cleanups = [];
      if (!native || native.multiple) {
        return;
      }
      const listen = listener(this.cleanups);
      if (supportsBaseSelect()) {
        completeBaseSelect(native, listen);
      } else {
        replaceWithListBox(wrapper, native, listen, this.cleanups);
      }
    },

    destroy() {
      this.cleanups.forEach(cleanup => cleanup());
    },
  };
}

/** Read each time, so a test can switch it off. */
function supportsBaseSelect() {
  return CSS.supports('appearance', 'base-select');
}

function completeBaseSelect(select, listen) {
  const isOpen = () => {
    try {
      return select.matches(':open');
    } catch {
      return false;
    }
  };
  const options = () => Array.from(select.options).filter(option => !option.hidden && !option.matches(':disabled'));

  listen(select, 'keydown', event => {
    if (event.target !== select || isOpen() || event.altKey || event.ctrlKey || event.metaKey) {
      return;
    }
    let target;
    if (event.key === 'Home') {
      target = options()[0];
    } else if (event.key === 'End') {
      target = options().at(-1);
    } else if (event.key !== 'Enter') {
      return;
    }
    event.preventDefault();
    try {
      select.showPicker();
    } catch {
      return;
    }
    target?.focus();
  });

  listen(select, 'pointermove', event => {
    if (event.pointerType !== 'mouse' || !isOpen()) {
      return;
    }
    const option = event.target instanceof Element ? event.target.closest('option') : null;
    if (option && !option.matches(':disabled') && document.activeElement !== option) {
      option.focus({preventScroll: true});
    }
  });

  if (select.form) {
    // The reset event comes before the form resets its controls. Setting the index again updates selectedcontent.
    listen(select.form, 'reset', () => setTimeout(() => {
      select.selectedIndex = select.selectedIndex;
    }));
  }
}

function replaceWithListBox(wrapper, select, listen, cleanups) {
  const id = select.id || `sl-select-${++instances}`;
  // Markup from an earlier run, such as a page htmx restored from its history cache.
  wrapper.querySelectorAll(':scope > .select-trigger, :scope > .select-content').forEach(element => element.remove());

  const listbox = document.createElement('div');
  listbox.className = 'select-content';
  listbox.id = `${id}-listbox`;
  listbox.setAttribute('role', 'listbox');
  listbox.popover = 'auto';

  const trigger = document.createElement('button');
  trigger.type = 'button';
  trigger.className = 'select select-trigger';
  trigger.id = `${id}-trigger`;
  trigger.setAttribute('role', 'combobox');
  trigger.setAttribute('aria-haspopup', 'listbox');
  trigger.setAttribute('aria-controls', listbox.id);
  trigger.setAttribute('popovertarget', listbox.id);
  const value = document.createElement('span');
  value.className = 'select-value';
  trigger.append(value);

  // An explicit anchor: keyboard opening goes through showPopover(), which not every browser gives a source.
  const anchor = `--sl-select-${++instances}`;
  trigger.style.anchorName = anchor;
  listbox.style.positionAnchor = anchor;

  select.after(trigger, listbox);
  select.tabIndex = -1;
  select.setAttribute('aria-hidden', 'true');
  // The customizable select's button, which a browser that parses it keeps in the (now hidden) select.
  select.querySelectorAll(':scope > button').forEach(button => {
    button.tabIndex = -1;
  });
  wrapper.dataset.enhanced = 'true';

  const indicatorTemplate = wrapper.querySelector(':scope > template.select-indicator-template');
  const optionOf = new Map();
  let rows = [];
  let active = null;

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
      row.className = option.className || 'select-item';
      row.id = `${listbox.id}-${index++}`;
      row.setAttribute('role', 'option');
      if (option.matches(':disabled')) {
        row.setAttribute('aria-disabled', 'true');
      }
      row.append(...contentOf(option));
      const indicator = indicatorTemplate?.content.firstElementChild;
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
        group.className = child.className || 'select-group';
        group.setAttribute('role', 'group');
        const label = document.createElement('div');
        label.className = 'select-label';
        label.id = `${listbox.id}-group-${index}`;
        label.textContent = child.label;
        group.setAttribute('aria-labelledby', label.id);
        group.append(label);
        Array.from(child.children).filter(option => option instanceof HTMLOptionElement)
            .forEach(option => addOption(group, option));
        listbox.append(group);
      } else if (child instanceof HTMLHRElement) {
        const separator = document.createElement('div');
        separator.className = child.className || 'select-separator';
        separator.setAttribute('aria-hidden', 'true');
        listbox.append(separator);
      }
    }
    active = null;
  };

  const sync = () => {
    trigger.disabled = select.disabled;
    for (const name of ['aria-describedby', 'aria-invalid']) {
      mirror(select, trigger, name);
    }
    if (select.required) {
      trigger.setAttribute('aria-required', 'true');
    } else {
      trigger.removeAttribute('aria-required');
    }
    const labelledBy = select.getAttribute('aria-labelledby') || labelIds(select, id);
    for (const element of [trigger, listbox]) {
      if (labelledBy) {
        element.setAttribute('aria-labelledby', labelledBy);
        element.removeAttribute('aria-label');
      } else {
        element.removeAttribute('aria-labelledby');
        mirror(select, element, 'aria-label');
      }
    }
    const chosen = select.selectedOptions[0];
    value.replaceChildren(...(chosen ? contentOf(chosen) : []));
    trigger.toggleAttribute('data-placeholder', chosen?.dataset.placeholder !== undefined);
    rows.forEach(row => row.setAttribute('aria-selected', String(optionOf.get(row).selected)));
  };

  const enabledRows = () => rows.filter(row => row.getAttribute('aria-disabled') !== 'true');
  const isOpen = () => listbox.matches(':popover-open');

  const setActive = (row, scroll = true) => {
    rows.forEach(each => each.toggleAttribute('data-active', each === row));
    if (row) {
      trigger.setAttribute('aria-activedescendant', row.id);
      if (scroll) {
        row.scrollIntoView({block: 'nearest'});
      }
    } else {
      trigger.removeAttribute('aria-activedescendant');
    }
    active = row;
  };

  const selectedRow = () => enabledRows().find(row => optionOf.get(row).selected);

  const open = row => {
    show(listbox, trigger);
    setActive(row ?? selectedRow() ?? enabledRows()[0]);
  };

  const choose = row => {
    const option = row ? optionOf.get(row) : null;
    if (option && !option.selected && !option.matches(':disabled')) {
      option.selected = true;
      select.dispatchEvent(new Event('input', {bubbles: true}));
      select.dispatchEvent(new Event('change', {bubbles: true}));
    }
    sync();
  };

  render();
  sync();

  const observer = new MutationObserver(records => {
    if (records.some(record => record.type !== 'attributes' || record.target !== select)) {
      render();
    }
    sync();
  });
  observer.observe(select, {attributes: true, attributeFilter: MIRRORED, childList: true, subtree: true,
    characterData: true});
  cleanups.push(() => observer.disconnect());

  trackExpanded(listbox, listen);
  positionWithoutAnchoring(listbox, () => trigger, {side: 'bottom', align: 'start'}, listen);

  listen(listbox, 'toggle', event => {
    if (event.newState === 'open') {
      // A click does not focus a button in every browser; the keys need it there.
      if (document.activeElement !== trigger) {
        trigger.focus({preventScroll: true});
      }
      if (!active) {
        setActive(selectedRow() ?? enabledRows()[0]);
      }
    } else {
      setActive(null);
    }
  });

  listen(select, 'change', sync);
  listen(select, 'input', sync);
  listen(select, 'focus', () => trigger.focus());
  if (select.form) {
    // The reset event comes before the form resets its controls.
    listen(select.form, 'reset', () => setTimeout(sync));
  }
  if (document.activeElement === select) {
    trigger.focus();
  }

  let typed = '';
  let typedAt = 0;
  let swallowClick = false;

  listen(trigger, 'keydown', event => {
    if (event.ctrlKey || event.metaKey) {
      return;
    }
    const enabled = enabledRows();
    const index = enabled.indexOf(active);
    const typing = event.key.length === 1 && !event.altKey && (event.key !== ' ' || Date.now() - typedAt < TYPEAHEAD_RESET_MS);
    if (typing) {
      event.preventDefault();
      const now = Date.now();
      typed = now - typedAt > TYPEAHEAD_RESET_MS ? event.key : typed + event.key;
      typedAt = now;
      const match = typeaheadMatch(enabled, index, typed.toLowerCase(), row => optionOf.get(row).label.trim().toLowerCase());
      if (!isOpen()) {
        open(match);
      } else if (match) {
        setActive(match);
      }
      if (event.key === ' ') {
        swallowClick = true;
      }
      return;
    }

    if (!isOpen()) {
      switch (event.key) {
        case 'ArrowDown':
        case 'ArrowUp':
        case 'Enter':
          event.preventDefault();
          open();
          return;
        case ' ':
          event.preventDefault();
          swallowClick = true;
          open();
          return;
        case 'Home':
          event.preventDefault();
          open(enabled[0]);
          return;
        case 'End':
          event.preventDefault();
          open(enabled.at(-1));
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
          choose(active);
          hide(listbox);
          return;
        }
        next = enabled[Math.max(index - 1, 0)];
        break;
      case 'Home':
        next = enabled[0];
        break;
      case 'End':
        next = enabled.at(-1);
        break;
      case 'PageDown':
        next = enabled[Math.min(index + PAGE_STEP, enabled.length - 1)];
        break;
      case 'PageUp':
        next = enabled[Math.max(index - PAGE_STEP, 0)];
        break;
      case 'Enter':
      case ' ':
        event.preventDefault();
        swallowClick = event.key === ' ';
        choose(active);
        hide(listbox);
        return;
      case 'Tab':
        // Chooses the highlighted option, and Tab moves on as usual.
        choose(active);
        hide(listbox);
        return;
      default:
        return;
    }
    if (next) {
      event.preventDefault();
      setActive(next);
    }
  });

  // Space activates a button when it is released: that click would toggle the list the keydown just handled.
  listen(trigger, 'keyup', event => {
    if (event.key === ' ') {
      setTimeout(() => {
        swallowClick = false;
      });
    }
  });
  listen(trigger, 'click', event => {
    if (swallowClick) {
      swallowClick = false;
      event.preventDefault();
    }
  });

  // The button keeps the focus while the pointer chooses.
  listen(listbox, 'mousedown', event => event.preventDefault());
  listen(listbox, 'pointermove', event => {
    const row = event.target instanceof Element ? event.target.closest('[role="option"]') : null;
    if (row && row !== active && row.getAttribute('aria-disabled') !== 'true') {
      setActive(row, false);
    }
  });
  listen(listbox, 'click', event => {
    const row = event.target instanceof Element ? event.target.closest('[role="option"]') : null;
    if (!row || row.getAttribute('aria-disabled') === 'true') {
      return;
    }
    choose(row);
    hide(listbox);
    trigger.focus({preventScroll: true});
  });
}

/** An option's content without its check mark, or its text when the browser kept none of its markup. */
function contentOf(option) {
  const nodes = Array.from(option.childNodes)
      .filter(node => !(node instanceof Element && node.matches('.select-item-indicator')))
      .map(node => node.cloneNode(true));
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
