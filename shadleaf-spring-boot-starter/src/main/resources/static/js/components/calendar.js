/*
  sl:calendar. The server renders date inputs, one for a single date and two (start and end) for a range; they are
  the state, so the form submits, binds, resets and validates without this, and without it the inputs show as they
  are. This hides them and draws the month grid after them, after react-day-picker (which shadcn/ui's Calendar is):

  - picking a day writes it into the input and fires input and change on it, so th:field, htmx triggers and the
    application's listeners see what a date input would make them see. A second click on the picked day clears an
    optional single date. In a range, the first pick is the start, the second the end (the two swap when the end comes
    first); a pick on a complete range starts a new one; while only the start is picked, the band follows the pointer;
  - the ARIA grid pattern for the keyboard, with one tab stop (roving tabindex): arrows move a day or a week (left and
    right swap on a right-to-left page), Home and End go to the start and end of the week, Page Up and Page Down a
    month, with Shift a year; Enter and Space pick. Moving past the shown months shows the next ones;
  - the inputs' min and max, the data-disabled-dates and the start and end months disable days and the navigation;
    a disabled input disables the calendar (no day takes the focus), a readonly one only the picking;
  - the caption is the month's name, or selects for the month and the year (data-caption); a change of month
    is announced in a status region;
  - the calendar takes the single input's label (its labels, aria-labelledby or aria-label) as its group's name, and
    focus that reaches a hidden input (a click on its label, autofocus, the browser's validation) moves on to the day
    that takes the focus;
  - a value set by a script (followed by a change event) or a form reset shows its month.
  The locale (data-locale), the first day of the week and the week numbering come from the server; the names of the
  months and days from Intl. htmx's history cache saves the drawn calendar; a restored one draws itself again.
*/
import {listener} from '../popup.js';

const WEEKDAYS = ['sun', 'mon', 'tue', 'wed', 'thu', 'fri', 'sat'];
const YEARS_BACK = 100;
const YEARS_AHEAD = 10;
let instances = 0;

export default function calendar() {
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
  const inputs = Array.from(root.querySelectorAll(':scope > input[type="date"]'));
  const template = root.querySelector(':scope > template.calendar-template');
  if (inputs.length < 1 || inputs.length > 2 || !template) {
    console.error('Shadleaf: sl:calendar needs one date input (a date) or two (a range), found %d.', inputs.length);
    return;
  }
  const range = inputs.length === 2;
  const id = `sl-calendar-${++instances}`;
  const config = readConfig(root);
  const formats = formatsFor(config.locale);

  // Markup from an earlier run, such as a page htmx restored from its history cache.
  root.querySelectorAll(':scope > .calendar-months, :scope > .calendar-status').forEach(element => element.remove());

  const body = element('div', 'calendar-months');
  const nav = template.content.querySelector('.calendar-nav').cloneNode(true);
  const previous = nav.querySelector('.calendar-previous');
  const next = nav.querySelector('.calendar-next');
  body.append(nav);
  const status = element('div', 'calendar-status sl-sr-only');
  status.setAttribute('role', 'status');
  root.append(body, status);
  root.dataset.enhanced = 'true';
  for (const input of inputs) {
    input.tabIndex = -1;
    input.setAttribute('aria-hidden', 'true');
  }
  nameGroup(root, inputs, range, id);

  const state = {
    month: null,
    focus: null,
    hover: null,
    writing: false,
  };

  // --- values -------------------------------------------------------------------------------------------------------

  const valueOf = input => parseIso(input.value) ? input.value : '';
  const values = () => inputs.map(valueOf);
  const disabled = () => inputs.every(input => input.matches(':disabled'));
  const readonly = () => inputs.some(input => input.readOnly);
  const bounds = () => {
    const mins = inputs.map(input => input.min).filter(parseIso);
    const maxes = inputs.map(input => input.max).filter(parseIso);
    return {
      min: mins.length ? mins.sort().at(-1) : null,
      max: maxes.length ? maxes.sort()[0] : null,
    };
  };
  const monthBounds = () => {
    const {min, max} = bounds();
    const now = today();
    return {
      start: parseMonth(config.startMonth) ?? (min ? firstOfMonth(parseIso(min)) : new Date(now.getFullYear() - YEARS_BACK, 0, 1)),
      end: parseMonth(config.endMonth) ?? (max ? firstOfMonth(parseIso(max)) : new Date(now.getFullYear() + YEARS_AHEAD, 11, 1)),
    };
  };
  const dayDisabled = date => {
    const {min, max} = bounds();
    const iso = toIso(date);
    return (min !== null && iso < min) || (max !== null && iso > max) || config.disabledDates(date);
  };

  /** The first month shown: the given one, moved so that every shown month lies within the bounds. */
  const clampMonth = month => {
    const {start, end} = monthBounds();
    const lastFirst = addMonths(end, 1 - config.numberOfMonths);
    if (month > lastFirst) {
      month = lastFirst;
    }
    return month < start ? start : month;
  };

  const anchor = () => values().find(value => value) ?? null;

  const write = next => {
    state.writing = true;
    try {
      next.forEach((value, index) => {
        const input = inputs[index];
        if (input.value === value) {
          return;
        }
        input.value = value;
        input.dispatchEvent(new Event('input', {bubbles: true}));
        input.dispatchEvent(new Event('change', {bubbles: true}));
      });
    } finally {
      state.writing = false;
    }
  };

  const pick = iso => {
    if (disabled() || readonly()) {
      return;
    }
    if (!range) {
      const [value] = values();
      write([value === iso && !inputs[0].required ? '' : iso]);
    } else {
      const [from, to] = values();
      if (!from || to) {
        write([iso, '']);
      } else {
        write(iso < from ? [iso, from] : [from, iso]);
      }
    }
    state.hover = null;
    render();
  };

  // --- drawing ------------------------------------------------------------------------------------------------------

  const render = () => {
    const focused = root.contains(document.activeElement) ? document.activeElement : null;
    const refocusSelect = focused?.matches('.calendar-select')
        ? {month: monthIndexOf(focused), unit: focused.closest('.calendar-dropdown').dataset.unit}
        : null;
    body.querySelectorAll(':scope > .calendar-month').forEach(month => month.remove());

    const isDisabled = disabled();
    const {start, end} = monthBounds();
    previous.disabled = isDisabled || state.month <= start;
    next.disabled = isDisabled || addMonths(state.month, config.numberOfMonths - 1) >= end;

    for (let index = 0; index < config.numberOfMonths; index++) {
      body.append(renderMonth(addMonths(state.month, index), index, isDisabled));
    }
    placeTabStop(isDisabled);
    if (refocusSelect) {
      body.querySelectorAll(':scope > .calendar-month')[refocusSelect.month]
          ?.querySelector(`.calendar-dropdown[data-unit="${refocusSelect.unit}"] > .calendar-select`)?.focus();
    } else if (focused?.matches('.calendar-day-button')) {
      dayButton(state.focus)?.focus();
    }
  };

  const monthIndexOf = node => Array.from(body.querySelectorAll(':scope > .calendar-month'))
      .indexOf(node.closest('.calendar-month'));

  const renderMonth = (month, index, isDisabled) => {
    const container = element('div', 'calendar-month');
    const captionId = `${id}-caption-${index}`;
    const caption = element('div', 'calendar-caption');
    caption.id = captionId;
    caption.append(renderCaption(month, isDisabled));
    container.append(caption);

    const grid = element('table', 'calendar-grid');
    grid.setAttribute('role', 'grid');
    grid.setAttribute('aria-labelledby', captionId);
    grid.setAttribute('aria-multiselectable', String(range));
    if (readonly()) {
      grid.setAttribute('aria-readonly', 'true');
    }
    const head = element('thead');
    const headRow = element('tr', 'calendar-weekdays');
    if (config.showWeekNumber) {
      // Text for screen readers: axe wants a column header with content, not only a name.
      const header = element('th', 'calendar-week-number-header');
      header.scope = 'col';
      const label = element('span', 'sl-sr-only');
      label.textContent = weekNumberLabel();
      header.append(label);
      headRow.append(header);
    }
    for (const day of weekdayNames(formats, config.weekStartsOn)) {
      const header = element('th', 'calendar-weekday');
      header.scope = 'col';
      header.setAttribute('aria-label', day.long);
      header.textContent = day.short;
      headRow.append(header);
    }
    head.append(headRow);

    const rows = element('tbody');
    const offset = (month.getDay() - config.weekStartsOn + 7) % 7;
    const first = addDays(month, -offset);
    const weeks = Math.ceil((offset + daysInMonth(month)) / 7);
    const [from, to] = range ? values() : [null, null];
    const [single] = range ? [null] : values();
    const todayIso = toIso(today());
    for (let week = 0; week < weeks; week++) {
      const row = element('tr', 'calendar-week');
      if (config.showWeekNumber) {
        const number = weekNumber(addDays(first, week * 7), config.weekStartsOn, config.firstWeekMinDays);
        const cell = element('th', 'calendar-week-number');
        cell.scope = 'row';
        cell.setAttribute('aria-label', `${weekNumberLabel()} ${number}`);
        cell.textContent = String(number);
        row.append(cell);
      }
      for (let column = 0; column < 7; column++) {
        const date = addDays(first, week * 7 + column);
        row.append(renderDay(date, month, {from, to, single, todayIso, isDisabled}));
      }
      rows.append(row);
    }
    grid.append(head, rows);
    container.append(grid);
    return container;
  };

  const renderDay = (date, month, {from, to, single, todayIso, isDisabled}) => {
    const iso = toIso(date);
    const cell = element('td', 'calendar-day');
    cell.setAttribute('role', 'gridcell');
    const outside = date.getMonth() !== month.getMonth();
    if (outside && !config.showOutsideDays) {
      cell.dataset.hidden = 'true';
      return cell;
    }
    const flags = {};
    if (outside) {
      flags.outside = 'true';
    }
    if (iso === todayIso) {
      flags.today = 'true';
    }
    const unavailable = dayDisabled(date);
    if (unavailable) {
      flags.disabled = 'true';
    }
    let selected = false;
    if (range) {
      // While only the start is picked, the band runs to the day under the pointer.
      const end = to || (from && state.hover && !isDisabled ? state.hover : '');
      const [low, high] = end && end < from ? [end, from] : [from, end];
      if (from && iso === low) {
        flags.rangeStart = 'true';
      }
      if (from && (iso === high || (!high && iso === low))) {
        flags.rangeEnd = 'true';
      }
      if (low && high && iso > low && iso < high) {
        flags.rangeMiddle = 'true';
      }
      selected = !!from && (to ? iso >= from && iso <= to : iso === from);
    } else {
      selected = iso === single;
    }
    if (selected) {
      flags.selected = 'true';
    }
    Object.assign(cell.dataset, flags);
    cell.setAttribute('aria-selected', String(selected));

    const button = element('button', 'calendar-day-button');
    button.type = 'button';
    Object.assign(button.dataset, flags);
    button.dataset.date = iso;
    button.tabIndex = -1;
    button.textContent = String(date.getDate());
    button.setAttribute('aria-label', formats.day.format(date));
    if (flags.today) {
      button.setAttribute('aria-current', 'date');
    }
    if (isDisabled) {
      button.disabled = true;
    } else if (unavailable) {
      // Still takes the focus, so the arrow keys move across it.
      button.setAttribute('aria-disabled', 'true');
    }
    cell.append(button);
    return cell;
  };

  const renderCaption = (month, isDisabled) => {
    const layout = config.captionLayout;
    if (layout === 'label') {
      const label = element('span', 'calendar-caption-label');
      label.textContent = formats.caption.format(month);
      return label;
    }
    const dropdowns = element('div', 'calendar-dropdowns');
    const {start, end} = monthBounds();
    if (layout === 'dropdown' || layout === 'dropdown-months') {
      const options = [];
      for (let m = 0; m < 12; m++) {
        const first = new Date(month.getFullYear(), m, 1);
        options.push({value: String(m), text: formats.monthShort.format(first), disabled: first < start || first > end});
      }
      dropdowns.append(dropdown('month', options, String(month.getMonth()), formats.monthShort.format(month), isDisabled));
    } else {
      dropdowns.append(captionText(formats.monthLong.format(month)));
    }
    if (layout === 'dropdown' || layout === 'dropdown-years') {
      const options = [];
      for (let year = start.getFullYear(); year <= end.getFullYear(); year++) {
        options.push({value: String(year), text: formats.year.format(new Date(year, 0, 1)), disabled: false});
      }
      dropdowns.append(dropdown('year', options, String(month.getFullYear()), formats.year.format(month), isDisabled));
    } else {
      dropdowns.append(captionText(formats.year.format(month)));
    }
    return dropdowns;
  };

  const captionText = text => {
    const label = element('span', 'calendar-caption-label');
    label.textContent = text;
    return label;
  };

  const dropdown = (unit, options, value, text, isDisabled) => {
    const label = template.content.querySelector(`.calendar-dropdown[data-unit="${unit}"]`).cloneNode(true);
    label.querySelector('.calendar-dropdown-label > span').textContent = text;
    const select = label.querySelector('.calendar-select');
    for (const option of options) {
      const choice = new Option(option.text, option.value, false, option.value === value);
      choice.disabled = option.disabled;
      select.append(choice);
    }
    select.disabled = isDisabled;
    return label;
  };

  const weekNumberLabel = () => template.content.querySelector('.calendar-week-number-label')?.textContent ?? '';

  /** Exactly one day takes the focus: the one that last had it, else the selection, today or the first day. */
  const placeTabStop = isDisabled => {
    if (isDisabled) {
      return;
    }
    const own = ':not([data-outside])';
    const target = dayButton(state.focus)
        ?? body.querySelector(`.calendar-day-button[data-selected]${own}`)
        ?? body.querySelector(`.calendar-day-button[data-today]${own}`)
        ?? body.querySelector(`.calendar-day-button${own}:not([aria-disabled])`)
        ?? body.querySelector(`.calendar-day-button${own}`);
    if (target) {
      target.tabIndex = 0;
      state.focus = target.dataset.date;
    }
  };

  /** The button of a day, in its own month rather than as another month's outside day. */
  const dayButton = iso => iso
      ? body.querySelector(`.calendar-day-button[data-date="${iso}"]:not([data-outside])`)
        ?? body.querySelector(`.calendar-day-button[data-date="${iso}"]`)
      : null;

  // --- navigation ---------------------------------------------------------------------------------------------------

  const showMonth = (month, {announce = true} = {}) => {
    const next = clampMonth(month);
    if (state.month && next.getTime() === state.month.getTime()) {
      return;
    }
    state.month = next;
    render();
    if (announce) {
      status.textContent = Array.from(body.querySelectorAll(':scope > .calendar-month'))
          .map(month => formats.caption.format(monthOf(month)))
          .join(', ');
    }
  };

  const monthOf = container => addMonths(state.month, Array.from(body.children).indexOf(container) - 1);

  /** Shows the months holding `iso`, unless they are already shown. */
  const reveal = iso => {
    const month = firstOfMonth(parseIso(iso));
    const last = addMonths(state.month, config.numberOfMonths - 1);
    if (month < state.month) {
      showMonth(month);
    } else if (month > last) {
      showMonth(addMonths(month, 1 - config.numberOfMonths));
    }
  };

  const moveFocus = date => {
    const {start, end} = monthBounds();
    const lastDay = addDays(addMonths(end, 1), -1);
    if (date < start) {
      date = start;
    } else if (date > lastDay) {
      date = lastDay;
    }
    state.focus = toIso(date);
    reveal(state.focus);
    body.querySelectorAll('.calendar-day-button[tabindex="0"]').forEach(button => {
      button.tabIndex = -1;
    });
    const button = dayButton(state.focus);
    if (button) {
      button.tabIndex = 0;
      button.focus();
    }
  };

  listen(body, 'click', event => {
    const target = event.target instanceof Element ? event.target : null;
    if (target?.closest('.calendar-previous')) {
      showMonth(addMonths(state.month, -1));
    } else if (target?.closest('.calendar-next')) {
      showMonth(addMonths(state.month, 1));
    } else {
      const button = target?.closest('.calendar-day-button');
      if (!button || button.disabled || button.hasAttribute('aria-disabled')) {
        return;
      }
      state.focus = button.dataset.date;
      pick(button.dataset.date);
    }
  });

  // The month and year selects are no part of the form's value: their events stay inside the calendar, so a form's
  // hx-trigger="change" or listeners do not see them.
  listen(body, 'input', event => {
    if (event.target instanceof Element && event.target.matches('.calendar-select')) {
      event.stopPropagation();
    }
  });

  listen(body, 'change', event => {
    const select = event.target;
    if (!(select instanceof HTMLSelectElement) || !select.matches('.calendar-select')) {
      return;
    }
    event.stopPropagation();
    const index = monthIndexOf(select);
    const shown = addMonths(state.month, index);
    const chosen = select.closest('.calendar-dropdown').dataset.unit === 'month'
        ? new Date(shown.getFullYear(), Number(select.value), 1)
        : new Date(Number(select.value), shown.getMonth(), 1);
    showMonth(addMonths(chosen, -index));
  });

  listen(body, 'keydown', event => {
    const button = event.target instanceof Element ? event.target.closest('.calendar-day-button') : null;
    if (!button || event.altKey || event.ctrlKey || event.metaKey) {
      return;
    }
    const current = parseIso(button.dataset.date);
    const rtl = getComputedStyle(root).direction === 'rtl';
    const weekday = (current.getDay() - config.weekStartsOn + 7) % 7;
    let target;
    switch (event.key) {
      case 'ArrowRight':
        target = addDays(current, rtl ? -1 : 1);
        break;
      case 'ArrowLeft':
        target = addDays(current, rtl ? 1 : -1);
        break;
      case 'ArrowDown':
        target = addDays(current, 7);
        break;
      case 'ArrowUp':
        target = addDays(current, -7);
        break;
      case 'Home':
        target = addDays(current, -weekday);
        break;
      case 'End':
        target = addDays(current, 6 - weekday);
        break;
      case 'PageDown':
        target = addMonthsKeepingDay(current, event.shiftKey ? 12 : 1);
        break;
      case 'PageUp':
        target = addMonthsKeepingDay(current, event.shiftKey ? -12 : -1);
        break;
      case 'Enter':
      case ' ':
        event.preventDefault();
        if (!button.hasAttribute('aria-disabled')) {
          state.focus = button.dataset.date;
          pick(button.dataset.date);
        }
        return;
      default:
        return;
    }
    event.preventDefault();
    moveFocus(target);
  });

  listen(body, 'pointerover', event => {
    const button = event.target instanceof Element ? event.target.closest('.calendar-day-button') : null;
    const [from, to] = values();
    const hover = button && !button.hasAttribute('aria-disabled') ? button.dataset.date : null;
    if (!range || !from || to || hover === state.hover || readonly()) {
      return;
    }
    state.hover = hover;
    render();
  });

  listen(body, 'pointerleave', () => {
    if (state.hover) {
      state.hover = null;
      render();
    }
  });

  // A value set by a script (that fires change, as for any input) shows its month.
  listen(root, 'change', event => {
    if (state.writing || !inputs.includes(event.target)) {
      return;
    }
    const value = anchor();
    if (value) {
      reveal(value);
    }
    render();
  });

  for (const input of inputs) {
    listen(input, 'focus', () => {
      if (!disabled()) {
        body.querySelector('.calendar-day-button[tabindex="0"]')?.focus();
      }
    });
  }

  const form = inputs[0].form;
  if (form) {
    // The reset event comes before the form resets its controls.
    listen(form, 'reset', () => setTimeout(() => {
      state.hover = null;
      const value = anchor();
      state.focus = value;
      if (value) {
        reveal(value);
      }
      render();
    }));
  }

  // disabled, readonly, min, max and required can change after the page loaded.
  const observer = new MutationObserver(() => render());
  for (const input of inputs) {
    observer.observe(input, {attributes: true, attributeFilter: ['disabled', 'readonly', 'min', 'max', 'required']});
  }
  const fieldset = root.closest('fieldset');
  if (fieldset) {
    observer.observe(fieldset, {attributes: true, attributeFilter: ['disabled']});
  }
  cleanups.push(() => observer.disconnect());

  const initial = anchor();
  state.focus = initial;
  showMonth(parseMonth(config.defaultMonth) ?? firstOfMonth(initial ? parseIso(initial) : today()), {announce: false});
  if (inputs.includes(document.activeElement)) {
    body.querySelector('.calendar-day-button[tabindex="0"]')?.focus();
  }
}

/**
 * The calendar is a group named like its single input: by the input's labels, aria-labelledby or aria-label. A range's
 * two inputs have names of their own (start, end); there the calendar keeps the attributes it was given.
 */
function nameGroup(root, inputs, range, id) {
  if (range || root.hasAttribute('aria-label') || root.hasAttribute('aria-labelledby')) {
    return;
  }
  const [input] = inputs;
  const labels = Array.from(input.labels ?? []);
  if (labels.length > 0) {
    root.setAttribute('aria-labelledby', labels.map((label, index) => {
      label.id ||= `${id}-label-${index}`;
      return label.id;
    }).join(' '));
  } else if (input.hasAttribute('aria-labelledby')) {
    root.setAttribute('aria-labelledby', input.getAttribute('aria-labelledby'));
  } else if (input.hasAttribute('aria-label')) {
    root.setAttribute('aria-label', input.getAttribute('aria-label'));
  }
  if (input.hasAttribute('aria-describedby')) {
    root.setAttribute('aria-describedby', input.getAttribute('aria-describedby'));
  }
}

function readConfig(root) {
  const data = root.dataset;
  const number = (value, fallback) => Number.isFinite(Number.parseInt(value, 10)) ? Number.parseInt(value, 10) : fallback;
  return {
    locale: data.locale || document.documentElement.lang || undefined,
    captionLayout: data.caption || 'label',
    numberOfMonths: Math.max(1, number(data.numberOfMonths, 1)),
    showOutsideDays: data.showOutsideDays !== 'false',
    showWeekNumber: data.showWeekNumber === 'true',
    weekStartsOn: ((number(data.weekStartsOn, 0) % 7) + 7) % 7,
    firstWeekMinDays: Math.min(7, Math.max(1, number(data.firstWeekMinDays, 1))),
    defaultMonth: data.defaultMonth,
    startMonth: data.startMonth,
    endMonth: data.endMonth,
    disabledDates: parseDisabledDates(data.disabledDates ?? ''),
  };
}

function formatsFor(locale) {
  const format = options => {
    try {
      return new Intl.DateTimeFormat(locale, options);
    } catch {
      return new Intl.DateTimeFormat(undefined, options);
    }
  };
  return {
    caption: format({month: 'long', year: 'numeric'}),
    monthLong: format({month: 'long'}),
    monthShort: format({month: 'short'}),
    year: format({year: 'numeric'}),
    day: format({dateStyle: 'full'}),
    weekdayShort: format({weekday: 'short'}),
    weekdayNarrow: format({weekday: 'narrow'}),
    weekdayLong: format({weekday: 'long'}),
  };
}

/**
 * The column headers, starting on `weekStartsOn`: two letters where those stay distinct ("Su Mo"), as
 * react-day-picker shows them, otherwise the locale's narrow names, as some languages' short names share a prefix.
 */
function weekdayNames(formats, weekStartsOn) {
  // 7 January 2024 is a Sunday.
  const days = Array.from({length: 7}, (_, index) => new Date(2024, 0, 7 + weekStartsOn + index));
  const short = days.map(day => formats.weekdayShort.format(day).slice(0, 2));
  const names = new Set(short).size === 7 ? short : days.map(day => formats.weekdayNarrow.format(day));
  return days.map((day, index) => ({short: names[index], long: formats.weekdayLong.format(day)}));
}

/** A date (2026-12-25), a range (2026-12-24..2026-12-31) or a day of the week (sat), comma-separated. */
export function parseDisabledDates(spec) {
  const rules = [];
  for (const part of spec.split(',').map(value => value.trim().toLowerCase()).filter(Boolean)) {
    const weekday = WEEKDAYS.indexOf(part.slice(0, 3));
    const [from, to] = part.split('..');
    if (weekday >= 0 && !/\d/.test(part)) {
      rules.push(date => date.getDay() === weekday);
    } else if (to !== undefined && parseIso(from) && parseIso(to)) {
      rules.push(date => toIso(date) >= from && toIso(date) <= to);
    } else if (parseIso(part)) {
      rules.push(date => toIso(date) === part);
    } else {
      console.error('Shadleaf: sl:calendar ignores "%s" in disabled-dates: not a date, a range or a day of the week.',
          part);
    }
  }
  return date => rules.some(rule => rule(date));
}

/**
 * The week number of the week starting on `weekStart`, counted as Java's WeekFields: week 1 is the first week with at
 * least `minDays` days of the new year (ISO: weeks start on Monday, 4 days).
 */
export function weekNumber(weekStart, weekStartsOn, minDays) {
  const firstWeek = year => {
    const january = new Date(year, 0, 1);
    const offset = (january.getDay() - weekStartsOn + 7) % 7;
    const start = addDays(january, -offset);
    return 7 - offset >= minDays ? start : addDays(start, 7);
  };
  let year = addDays(weekStart, 6).getFullYear();
  let start = firstWeek(year);
  if (weekStart < start) {
    start = firstWeek(--year);
  }
  return Math.round(daysBetween(start, weekStart) / 7) + 1;
}

function element(tag, className) {
  const node = document.createElement(tag);
  if (className) {
    node.className = className;
  }
  return node;
}

function pad(number) {
  return String(number).padStart(2, '0');
}

export function toIso(date) {
  return `${String(date.getFullYear()).padStart(4, '0')}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
}

/** A local date from an ISO date (2026-10-23), or null for anything else, an impossible date included. */
export function parseIso(value) {
  const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(value ?? '');
  if (!match) {
    return null;
  }
  const date = new Date(Number(match[1]), Number(match[2]) - 1, Number(match[3]));
  date.setFullYear(Number(match[1]));
  return toIso(date) === value ? date : null;
}

function parseMonth(value) {
  return parseIso(value ? `${value.slice(0, 7)}-01` : null);
}

function today() {
  const now = new Date();
  return new Date(now.getFullYear(), now.getMonth(), now.getDate());
}

function firstOfMonth(date) {
  return new Date(date.getFullYear(), date.getMonth(), 1);
}

function addDays(date, days) {
  return new Date(date.getFullYear(), date.getMonth(), date.getDate() + days);
}

function addMonths(date, months) {
  return new Date(date.getFullYear(), date.getMonth() + months, 1);
}

/** The same day `months` later, or the last day of that month when it is shorter (31 January + 1 month). */
function addMonthsKeepingDay(date, months) {
  const month = addMonths(date, months);
  return new Date(month.getFullYear(), month.getMonth(), Math.min(date.getDate(), daysInMonth(month)));
}

function daysInMonth(month) {
  return new Date(month.getFullYear(), month.getMonth() + 1, 0).getDate();
}

/** Whole days from `a` to `b`, counted on the calendar, so a change to or from summer time makes no difference. */
function daysBetween(a, b) {
  return Math.round((Date.UTC(b.getFullYear(), b.getMonth(), b.getDate())
      - Date.UTC(a.getFullYear(), a.getMonth(), a.getDate())) / 86_400_000);
}
