/*
  sl:date-picker. A sl:button opens a sl:popover-content (slPopover: aria-expanded, Escape, closing when the focus
  leaves, the stand-in for anchor positioning) holding a sl:calendar (slCalendar), whose date inputs are the value.
  This ties them together, as a React app does in its own code around shadcn's Popover and Calendar:

  - the button shows the chosen date, formatted with Intl in the page's locale (data-date-style, the calendar's
    data-locale): once when it starts, so it is always Intl's text, and after every change of the inputs (a pick, a
    script that fires change, a form reset); data-empty and the placeholder while there is none;
  - a pick closes the popover, once a date, or both ends of a range, are chosen; the focus goes back to the button;
  - opening it shows the chosen date's month again and focuses its day (else today), through autofocus;
  - the button is named by what names the calendar's input (its labels, aria-labelledby or aria-label) or a range's
    calendar, followed by the date it shows; a click on the input's label focuses the button, as the input inside the
    closed popover cannot take the focus;
  - the inputs' aria-invalid and aria-describedby (the field's errors) go to the button too. An input the browser
    finds invalid on submit opens the popover, as the browser can only point at a control it can focus;
  - the button is disabled while every input is.
  htmx's history cache saves the button's text; a restored date picker starts again.
*/
import {hide, listener, show} from '../popup.js';
import {parseIso} from './calendar.js';

let instances = 0;

export default function datePicker() {
  return {
    // Declared, so init() sets it here: Alpine puts a property no scope has on the outermost x-data, where every
    // component nested in that one (all of them, inside sl:sidebar-provider) would share it.
    cleanups: [],

    init() {
      this.cleanups = [];
      setUp(this.$el, listener(this.cleanups), this.cleanups);
    },

    destroy() {
      this.cleanups.forEach(cleanup => cleanup());
    },
  };
}

function setUp(root, listen, cleanups) {
  const trigger = root.querySelector(':scope > .date-picker-trigger');
  const popup = root.querySelector(':scope > .date-picker-content');
  const calendar = popup?.querySelector(':scope > .calendar');
  const inputs = calendar ? Array.from(calendar.querySelectorAll(':scope > input[type="date"]')) : [];
  const label = trigger?.querySelector('.date-picker-value');
  if (!trigger || !label || inputs.length < 1 || inputs.length > 2) {
    console.error('Shadleaf: sl:date-picker needs its button and a calendar with one date input (a date) or two (a '
        + 'range), found %d.', inputs.length);
    return;
  }
  const range = inputs.length === 2;
  const id = `sl-date-picker-${++instances}`;
  const format = formatFor(calendar.dataset.locale || document.documentElement.lang || undefined,
      root.dataset.dateStyle || 'long');
  const placeholder = root.dataset.placeholder ?? '';
  const rangeFormat = root.dataset.rangeFormat || '{0} – {1}';

  const values = () => inputs.map(input => parseIso(input.value) ? input.value : '');
  const complete = () => values().every(value => value);

  const text = () => {
    const [from, to] = values();
    if (!from) {
      return null;
    }
    const start = format.format(parseIso(from));
    return range && to ? rangeFormat.replace('{0}', start).replace('{1}', format.format(parseIso(to))) : start;
  };

  const update = () => {
    const shown = text();
    label.textContent = shown ?? placeholder;
    if (shown === null) {
      trigger.dataset.empty = 'true';
    } else {
      delete trigger.dataset.empty;
    }
    mirrorField();
  };

  /** The field's state, which the server put on the inputs: aria-invalid and aria-describedby. */
  const mirrorField = () => {
    if (inputs.some(input => input.getAttribute('aria-invalid') === 'true')) {
      trigger.setAttribute('aria-invalid', 'true');
    } else {
      trigger.removeAttribute('aria-invalid');
    }
    const describedBy = [...new Set(inputs.flatMap(input => (input.getAttribute('aria-describedby') ?? '')
        .split(/\s+/).filter(Boolean)))];
    if (describedBy.length > 0) {
      trigger.setAttribute('aria-describedby', describedBy.join(' '));
    }
  };

  const ensureId = (element, suffix) => {
    element.id ||= `${id}-${suffix}`;
    return element.id;
  };

  // The button's name: what names the calendar, then the date it shows. aria-labelledby takes an element's
  // aria-label as its text, so a hidden input or calendar can lend its own.
  const nameButton = () => {
    const source = range ? calendar : inputs[0];
    const labels = range ? [] : Array.from(inputs[0].labels ?? []);
    let names;
    if (labels.length > 0) {
      names = labels.map((element, index) => ensureId(element, `label-${index}`));
    } else if (source.hasAttribute('aria-labelledby')) {
      names = source.getAttribute('aria-labelledby').split(/\s+/).filter(Boolean);
    } else if (source.hasAttribute('aria-label')) {
      names = [ensureId(source, 'name')];
    } else {
      return;
    }
    trigger.setAttribute('aria-labelledby', [...names, ensureId(label, 'value')].join(' '));
  };

  const updateDisabled = () => {
    trigger.disabled = inputs.every(input => input.matches(':disabled'));
  };

  // Opening: the day that takes the focus gets autofocus, which the popover gives the focus to as it shows.
  listen(popup, 'beforetoggle', event => {
    if (event.newState !== 'open') {
      return;
    }
    calendar.dispatchEvent(new CustomEvent('sl-calendar-show-start'));
    calendar.querySelectorAll('.calendar-day-button[autofocus]').forEach(button => button.removeAttribute('autofocus'));
    calendar.querySelector('.calendar-day-button[tabindex="0"]')?.setAttribute('autofocus', '');
  });

  // A pick writes the inputs one after the other (a new range clears the end after setting the start): decide once
  // all of them are written.
  let closing = 0;
  listen(calendar, 'change', event => {
    if (!inputs.includes(event.target)) {
      return;
    }
    update();
    clearTimeout(closing);
    closing = setTimeout(() => {
      if (!popup.matches(':popover-open') || !complete()) {
        return;
      }
      const hadFocus = popup.contains(document.activeElement);
      hide(popup);
      if (hadFocus && trigger.isConnected && !trigger.contains(document.activeElement)) {
        trigger.focus();
      }
    });
  });
  cleanups.push(() => clearTimeout(closing));

  // invalid does not bubble.
  listen(calendar, 'invalid', event => {
    if (!inputs.includes(event.target)) {
      return;
    }
    trigger.setAttribute('aria-invalid', 'true');
    show(popup, trigger);
  }, true);

  if (!range) {
    for (const element of inputs[0].labels ?? []) {
      listen(element, 'click', event => {
        event.preventDefault();
        if (!trigger.disabled) {
          trigger.focus();
        }
      });
    }
  }

  const form = inputs[0].form;
  if (form) {
    // The reset event comes before the form resets its controls.
    listen(form, 'reset', () => setTimeout(update));
  }

  const observer = new MutationObserver(updateDisabled);
  for (const input of inputs) {
    observer.observe(input, {attributes: true, attributeFilter: ['disabled']});
  }
  const fieldset = root.closest('fieldset');
  if (fieldset) {
    observer.observe(fieldset, {attributes: true, attributeFilter: ['disabled']});
  }
  cleanups.push(() => observer.disconnect());

  update();
  nameButton();
  updateDisabled();
}

function formatFor(locale, dateStyle) {
  try {
    return new Intl.DateTimeFormat(locale, {dateStyle});
  } catch {
    return new Intl.DateTimeFormat(undefined, {dateStyle: 'long'});
  }
}
