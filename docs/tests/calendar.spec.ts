import {expect, test, type Page} from '@playwright/test';
import AxeBuilder from '@axe-core/playwright';
import {combinations, openShowcase} from './showcase';
import {openFixture} from './fixture';

// sl:calendar, with slCalendar drawing the month over the date inputs the server renders. On the showcase, in every
// skin and theme: every calendar is one tab stop, the arrow keys move the focus, which shows on the days and the
// navigation, and axe passes with a day focused. In fixture pages under a strict Content-Security-Policy with the csp
// Alpine build, with "today" fixed at 23 October 2026: picking and clearing a date (the input's value and events), the
// keyboard of the ARIA grid pattern (right-to-left too), a range with its band, min, max and disabled dates, the month
// and year selects, week numbers, the label, a disabled input, form reset, a value set by a script, no Alpine, and
// htmx's history. Runs in Chromium and Firefox.

const TAGS = ['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa', 'wcag22aa', 'best-practice'];
const TODAY = new Date('2026-10-23T10:00:00');

const icon = (path: string) => `<svg class="icon" viewBox="0 0 24 24" aria-hidden="true"><path d="${path}"/></svg>`;

/** The template sl:calendar renders (see calendar.approved.html). */
const TEMPLATE = '<template class="calendar-template"><div class="calendar-nav">'
    + `<button type="button" class="calendar-nav-button calendar-previous" aria-label="Go to the previous month">${
        icon('m15 18-6-6 6-6')}</button>`
    + `<button type="button" class="calendar-nav-button calendar-next" aria-label="Go to the next month">${
        icon('m9 18 6-6-6-6')}</button></div>`
    + `<label class="calendar-dropdown" data-unit="month"><span class="calendar-dropdown-label"><span></span>${
        icon('m6 9 6 6 6-6')}</span><select class="calendar-select" aria-label="Month"></select></label>`
    + `<label class="calendar-dropdown" data-unit="year"><span class="calendar-dropdown-label"><span></span>${
        icon('m6 9 6 6 6-6')}</span><select class="calendar-select" aria-label="Year"></select></label>`
    + '<span class="calendar-week-number-label">Week</span></template>';

/** A calendar marked up as <sl:calendar> renders it, with its own input unless `inputs` are given. */
function calendar({inputs = '<input type="date" class="input calendar-input" id="due" name="due">', attributes = '',
  weekStartsOn = 0, minDays = 1} = {}): string {
  return `<div class="calendar" x-data="slCalendar" role="group" data-locale="en-US" data-week-starts-on="${
      weekStartsOn}" data-first-week-min-days="${minDays}" ${attributes}>${inputs}${TEMPLATE}</div>`;
}

const RANGE = '<input type="date" class="input" id="from" name="from" aria-label="Check-in">'
    + '<input type="date" class="input" id="to" name="to" aria-label="Check-out">';

async function open(page: Page, main: string, options: Parameters<typeof openFixture>[2] = {}) {
  await page.clock.setFixedTime(TODAY);
  const messages = await openFixture(page, main, options);
  await expect(page.locator('.calendar[data-enhanced]').first()).toBeVisible();
  return messages;
}

/** A day's button in its own month (not shown as another month's outside day). */
function day(page: Page, iso: string) {
  return page.locator(`.calendar-day-button[data-date="${iso}"]:not([data-outside])`);
}

function focusedDate(page: Page) {
  return page.evaluate(() => (document.activeElement as HTMLElement | null)?.dataset.date ?? null);
}

function captions(page: Page) {
  return page.locator('.calendar-caption').allInnerTexts();
}

/** Counts the input and change events on an input, in window.slEvents. */
async function countEvents(page: Page, selector: string) {
  await page.evaluate(selector => {
    const counts = {input: 0, change: 0};
    (window as unknown as { slEvents: typeof counts }).slEvents = counts;
    const input = document.querySelector(selector)!;
    input.addEventListener('input', () => counts.input++);
    input.addEventListener('change', () => counts.change++);
  }, selector);
}

function events(page: Page) {
  return page.evaluate(() => (window as unknown as { slEvents: { input: number, change: number } }).slEvents);
}

/** Whether the focused element shows its focus (an outline or a box-shadow ring). */
function focusVisible(page: Page) {
  return page.evaluate(() => {
    const element = document.activeElement as HTMLElement;
    const style = getComputedStyle(element);
    return element.matches(':focus-visible')
        && ((style.outlineStyle !== 'none' && parseFloat(style.outlineWidth) > 0) || style.boxShadow !== 'none');
  });
}

for (const {skin, theme} of combinations) {
  test(`every calendar is one tab stop, moves with the arrows, shows focus and passes axe: ${skin}, ${theme}`,
      async ({page}) => {
        await openShowcase(page, skin, theme);
        const calendars = page.locator('.calendar[data-enhanced]');
        await expect(calendars.first()).toBeVisible();
        const count = await calendars.count();
        expect(count).toBeGreaterThan(0);
        for (let index = 0; index < count; index++) {
          const root = calendars.nth(index);
          const stops = await root.locator('.calendar-day-button[tabindex="0"]').count();
          const disabled = await root.locator('.calendar-day-button:disabled').count() > 0;
          expect(stops, `calendar ${index}`).toBe(disabled ? 0 : 1);
          if (disabled) {
            continue;
          }
          await root.locator('.calendar-day-button[tabindex="0"]').focus();
          await page.keyboard.press('ArrowRight');
          await page.keyboard.press('ArrowLeft');
          expect(await focusVisible(page), `calendar ${index}: a day shows focus`).toBe(true);
          await root.evaluate(element => element.setAttribute('data-axe', 'true'));
          const results = await new AxeBuilder({page}).include('[data-axe]').withTags(TAGS).analyze();
          await root.evaluate(element => element.removeAttribute('data-axe'));
          expect(results.violations, `calendar ${index}`).toEqual([]);
        }
        await calendars.first().locator('.calendar-next').focus();
        await page.keyboard.press('Shift+Tab');
        await page.keyboard.press('Tab');
        expect(await focusVisible(page), 'the navigation shows focus').toBe(true);
      });
}

test('the csp build draws the month over the hidden input; a pick writes the input and fires input and change',
    async ({page}) => {
      const messages = await open(page, `<label for="due">Due</label>${calendar()}`);
      const input = page.locator('#due');
      await countEvents(page, '#due');

      expect(await captions(page)).toEqual(['October 2026']);
      await expect(input).toHaveAttribute('aria-hidden', 'true');
      await expect(input).toHaveAttribute('tabindex', '-1');
      expect(await page.locator('.calendar-weekday').allInnerTexts()).toEqual(['Su', 'Mo', 'Tu', 'We', 'Th', 'Fr', 'Sa']);
      await expect(day(page, '2026-10-23')).toHaveAttribute('aria-current', 'date');
      await expect(day(page, '2026-10-23')).toHaveAttribute('aria-label', 'Friday, October 23, 2026');
      await expect(page.getByRole('group', {name: 'Due'})).toBeVisible();

      await day(page, '2026-10-14').click();
      await expect(input).toHaveValue('2026-10-14');
      expect(await events(page)).toEqual({input: 1, change: 1});
      await expect(page.locator('.calendar-day:has(> [data-date="2026-10-14"])')).toHaveAttribute('aria-selected', 'true');
      expect(await focusedDate(page)).toBe('2026-10-14');

      await day(page, '2026-10-14').click();
      await expect(input, 'a second click clears an optional date').toHaveValue('');
      expect(await events(page)).toEqual({input: 2, change: 2});
      expect(messages).toEqual([]);
    });

test('a required date is not cleared by a second click', async ({page}) => {
  await open(page, calendar({
    inputs: '<input type="date" class="input calendar-input" id="due" name="due" value="2026-10-14" required '
        + 'aria-label="Due">',
  }));
  await day(page, '2026-10-14').click();
  await expect(page.locator('#due')).toHaveValue('2026-10-14');
});

test('keyboard: one tab stop on the picked day, arrows, Home and End, Page Up and Down (with Shift a year), Enter',
    async ({page}) => {
      await open(page, `<button type="button" id="before">Before</button>${calendar({
        inputs: '<input type="date" class="input calendar-input" id="due" name="due" value="2026-10-14" aria-label="Due">',
      })}<button type="button" id="after">After</button>`);
      await page.locator('#before').focus();
      await page.keyboard.press('Tab');
      await expect(page.locator('.calendar-previous'), 'the navigation comes first').toBeFocused();
      await page.keyboard.press('Tab');
      await expect(page.locator('.calendar-next')).toBeFocused();
      await page.keyboard.press('Tab');
      expect(await focusedDate(page)).toBe('2026-10-14');

      await page.keyboard.press('Home');
      expect(await focusedDate(page)).toBe('2026-10-11');
      await page.keyboard.press('End');
      expect(await focusedDate(page)).toBe('2026-10-17');
      await page.keyboard.press('ArrowDown');
      await page.keyboard.press('ArrowDown');
      await page.keyboard.press('ArrowDown');
      expect(await focusedDate(page)).toBe('2026-11-07');
      expect(await captions(page), 'the next month shows').toEqual(['November 2026']);
      await expect(page.locator('.calendar-status')).toHaveText('November 2026');
      await page.keyboard.press('ArrowLeft');
      await page.keyboard.press('ArrowUp');
      expect(await focusedDate(page)).toBe('2026-10-30');
      await page.keyboard.press('ArrowRight');
      await page.keyboard.press('PageDown');
      expect(await focusedDate(page), '31 October + 1 month is the end of November').toBe('2026-11-30');
      await page.keyboard.press('Shift+PageUp');
      expect(await focusedDate(page)).toBe('2025-11-30');
      expect(await captions(page)).toEqual(['November 2025']);

      await page.keyboard.press('Enter');
      await expect(page.locator('#due')).toHaveValue('2025-11-30');
      await page.keyboard.press('Tab');
      await expect(page.locator('#after'), 'one tab stop').toBeFocused();
      await page.keyboard.press('Shift+Tab');
      expect(await focusedDate(page)).toBe('2025-11-30');
    });

test('right to left: the arrows follow the reading direction', async ({page}) => {
  await open(page, calendar({
    inputs: '<input type="date" class="input calendar-input" id="due" name="due" value="2026-10-14" aria-label="Due">',
  }), {htmlAttributes: 'dir="rtl"'});
  await day(page, '2026-10-14').focus();
  await page.keyboard.press('ArrowLeft');
  expect(await focusedDate(page)).toBe('2026-10-15');
  await page.keyboard.press('ArrowRight');
  expect(await focusedDate(page)).toBe('2026-10-14');
});

test('a range: the first pick starts it, the second ends it (swapped when earlier), a third starts a new one',
    async ({page}) => {
      await open(page, calendar({inputs: RANGE, attributes: 'aria-label="Stay" data-number-of-months="2"'}));
      const from = page.locator('#from');
      const to = page.locator('#to');
      expect(await captions(page)).toEqual(['October 2026', 'November 2026']);
      await expect(page.getByRole('group', {name: 'Stay'})).toBeVisible();

      await day(page, '2026-11-03').click();
      await expect(from).toHaveValue('2026-11-03');
      await expect(to).toHaveValue('');
      await day(page, '2026-10-28').hover();
      await expect(page.locator('.calendar-day[data-range-middle]'), 'the band follows the pointer').toHaveCount(5);

      await day(page, '2026-10-28').click();
      await expect(from).toHaveValue('2026-10-28');
      await expect(to).toHaveValue('2026-11-03');
      await expect(page.locator('.calendar-day-button[data-range-start][data-selected]')).toHaveAttribute('data-date', '2026-10-28');
      await expect(page.locator('.calendar-day-button[data-range-end][data-selected]')).toHaveAttribute('data-date', '2026-11-03');
      await expect(page.locator('.calendar-day[aria-selected="true"]:not(:has([data-outside]))')).toHaveCount(7);

      await day(page, '2026-11-20').click();
      await expect(from).toHaveValue('2026-11-20');
      await expect(to).toHaveValue('');
    });

test('min, max and disabled dates: those days take the focus but no pick, and the navigation stops at the bounds',
    async ({page}) => {
      await open(page, calendar({
        inputs: '<input type="date" class="input calendar-input" id="due" name="due" min="2026-10-05" max="2026-11-27" '
            + 'aria-label="Due">',
        attributes: 'data-disabled-dates="sat,sun,2026-10-14..2026-10-16"',
      }));
      await expect(page.locator('.calendar-previous')).toBeDisabled();
      for (const iso of ['2026-10-02', '2026-10-10', '2026-10-15']) {
        await expect(day(page, iso)).toHaveAttribute('aria-disabled', 'true');
      }
      await day(page, '2026-10-15').click({force: true});
      await expect(page.locator('#due')).toHaveValue('');
      await day(page, '2026-10-13').focus();
      await page.keyboard.press('ArrowRight');
      expect(await focusedDate(page), 'a disabled day takes the focus').toBe('2026-10-14');
      await page.keyboard.press('Enter');
      await expect(page.locator('#due')).toHaveValue('');

      await page.locator('.calendar-next').click();
      expect(await captions(page)).toEqual(['November 2026']);
      await expect(page.locator('.calendar-next')).toBeDisabled();
      await expect(day(page, '2026-11-30')).toHaveAttribute('aria-disabled', 'true');
    });

test('the month and year selects show another month; their events stay inside the calendar', async ({page}) => {
  await open(page, `<form id="form">${calendar({
    inputs: '<input type="date" class="input calendar-input" id="birth" name="birth" value="1990-06-15" '
        + 'aria-label="Date of birth">',
    attributes: 'data-caption="dropdown" data-start-month="1926-01" data-end-month="2026-12"',
  })}</form>`);
  await page.evaluate(() => {
    (window as unknown as { formEvents: number }).formEvents = 0;
    document.getElementById('form')!.addEventListener('change', () => (window as unknown as { formEvents: number }).formEvents++);
  });
  const month = page.getByRole('combobox', {name: 'Month'});
  const year = page.getByRole('combobox', {name: 'Year'});
  await expect(month).toHaveValue('5');
  await expect(year).toHaveValue('1990');
  expect(await year.locator('option').count()).toBe(101);

  await month.focus();
  await month.selectOption('1');
  await expect(page.locator('.calendar-dropdown[data-unit="month"] .calendar-dropdown-label')).toHaveText('Feb');
  await expect(page.getByRole('combobox', {name: 'Month'}), 'the new select keeps the focus').toBeFocused();
  await page.getByRole('combobox', {name: 'Year'}).selectOption('2000');
  await expect(day(page, '2000-02-29')).toBeVisible();
  expect(await page.evaluate(() => (window as unknown as { formEvents: number }).formEvents)).toBe(0);
  await expect(page.locator('#birth')).toHaveValue('1990-06-15');
});

test('week numbers: ISO weeks from Monday, or the week of 1 January first from Sunday', async ({page}) => {
  await open(page, calendar({
    weekStartsOn: 1, minDays: 4,
    attributes: 'aria-label="Sprint" data-show-week-number="true" data-default-month="2026-12"',
  }) + calendar({
    inputs: '<input type="date" class="input calendar-input" id="us" name="us" aria-label="US">',
    attributes: 'data-show-week-number="true" data-default-month="2026-12"',
  }));
  const [iso, us] = [page.locator('.calendar').nth(0), page.locator('.calendar').nth(1)];
  expect(await iso.locator('.calendar-weekday').first().innerText()).toBe('Mo');
  expect(await iso.locator('.calendar-week-number').allInnerTexts()).toEqual(['49', '50', '51', '52', '53']);
  expect(await us.locator('.calendar-week-number').allInnerTexts()).toEqual(['49', '50', '51', '52', '1']);
  await expect(iso.locator('.calendar-week-number').first()).toHaveAttribute('aria-label', 'Week 49');
});

test('a click on the label focuses the day that takes the focus; the calendar is described like its input',
    async ({page}) => {
      await open(page, `<label for="due">Due</label>${calendar({
        inputs: '<input type="date" class="input calendar-input" id="due" name="due" value="2026-10-14" '
            + 'aria-describedby="due-help">',
      })}<p id="due-help">Pick a weekday.</p>`);
      await page.getByText('Due', {exact: true}).click();
      expect(await focusedDate(page)).toBe('2026-10-14');
      await expect(page.getByRole('group', {name: 'Due'})).toHaveAttribute('aria-describedby', 'due-help');
    });

test('a disabled input disables the calendar, and enabling it again brings it back', async ({page}) => {
  await open(page, calendar({
    inputs: '<input type="date" class="input calendar-input" id="due" name="due" value="2026-10-14" aria-label="Due" disabled>',
  }));
  await expect(page.locator('.calendar-day-button:not(:disabled)')).toHaveCount(0);
  await expect(page.locator('.calendar-previous')).toBeDisabled();
  await expect(page.locator('.calendar-day-button[tabindex="0"]')).toHaveCount(0);

  await page.locator('#due').evaluate(input => input.removeAttribute('disabled'));
  await expect(page.locator('.calendar-day-button[tabindex="0"]')).toHaveAttribute('data-date', '2026-10-14');
});

test('form reset and a value set by a script (with a change event) show their month', async ({page}) => {
  await open(page, `<form>${calendar({
    inputs: '<input type="date" class="input calendar-input" id="due" name="due" value="2026-10-14" aria-label="Due">',
  })}<button type="reset" id="reset">Reset</button></form>`);
  await page.locator('.calendar-next').click();
  await day(page, '2026-11-20').click();
  await page.locator('#reset').click();
  await expect(page.locator('#due')).toHaveValue('2026-10-14');
  await expect.poll(() => captions(page)).toEqual(['October 2026']);
  await expect(day(page, '2026-10-14')).toHaveAttribute('data-selected', 'true');

  await page.locator('#due').evaluate(input => {
    (input as HTMLInputElement).value = '2027-03-08';
    input.dispatchEvent(new Event('change', {bubbles: true}));
  });
  expect(await captions(page)).toEqual(['March 2027']);
  await expect(day(page, '2027-03-08')).toHaveAttribute('data-selected', 'true');
});

test('the picked date is submitted with the form', async ({page}) => {
  let submitted = '';
  await open(page, `<form action="/submit" method="get">${calendar({
    inputs: '<input type="date" class="input calendar-input" id="due" name="due" aria-label="Due" required>',
  })}<button id="send">Send</button></form>`, {
    routes: {
      '/submit': ({url}) => {
        submitted = url.search;
        return {body: '<p id="done">Sent</p>'};
      },
    },
  });
  await day(page, '2026-10-29').click();
  await page.locator('#send').click();
  await expect(page.locator('#done')).toBeVisible();
  expect(submitted).toBe('?due=2026-10-29');
});

test('without Alpine the date inputs show as they are', async ({page}) => {
  await page.clock.setFixedTime(TODAY);
  await openFixture(page, calendar({inputs: RANGE, attributes: 'aria-label="Stay"'}), {alpine: false});
  await expect(page.locator('#from')).toBeVisible();
  await expect(page.locator('#to')).toBeVisible();
  await expect(page.locator('.calendar-months')).toHaveCount(0);
});

test('htmx: back restores the calendar with one working month grid', async ({page}) => {
  await open(page, `${calendar({
    inputs: '<input type="date" class="input calendar-input" id="due" name="due" aria-label="Due">',
  })}<a id="next" href="/next" hx-get="/next" hx-target="main" hx-push-url="true">Next</a>`, {
    htmx: true,
    routes: {'/next': () => ({body: '<p id="next-page">Next page</p>'})},
  });
  await day(page, '2026-10-21').click();
  await page.locator('#next').click();
  await expect(page.locator('#next-page')).toBeVisible();
  await page.goBack();
  await expect(page.locator('.calendar-months')).toHaveCount(1);
  await expect(page.locator('.calendar-month')).toHaveCount(1);
  await expect(page.locator('.calendar-status')).toHaveCount(1);
  await day(page, '2026-10-22').click();
  await expect(page.locator('#due')).toHaveValue('2026-10-22');
});
