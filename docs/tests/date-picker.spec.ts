import {expect, test, type Locator, type Page} from '@playwright/test';
import AxeBuilder from '@axe-core/playwright';
import previews from '../src/generated/previews.json' with {type: 'json'};
import {combinations, openShowcase} from './showcase';
import {openFixture} from './fixture';

// sl:date-picker: a sl:button opening a sl:popover-content with a sl:calendar, tied together by slDatePicker. On the
// showcase, in every skin and theme: every date picker opens from its button with the keyboard, with the focus on the
// day that takes it, passes axe open, shows focus, and Escape closes it with the focus back on the button. In fixture
// pages under a strict Content-Security-Policy with the csp Alpine build, with "today" fixed at 23 October 2026 and the
// markup of the docs previews: a pick (the input, its events, the button's text and name, closing), a range, the
// keyboard, Intl's formatting in the page's locale, the label, the field's errors, the browser's validation, form
// reset and a value set by a script, disabled, no Alpine, and htmx's history. Runs in Chromium and Firefox.

const TAGS = ['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa', 'wcag22aa', 'best-practice'];
const TODAY = new Date('2026-10-23T10:00:00');

type Scenario = { id: string, html: string };

/** A scenario's markup, as the server renders it for the docs. */
const preview = (id: string) => (previews.scenarios as Scenario[]).find(scenario => scenario.id === `date-picker--${id}`)!
    .html;

async function open(page: Page, main: string, options: Parameters<typeof openFixture>[2] = {}) {
  await page.clock.setFixedTime(TODAY);
  const messages = await openFixture(page, main, options);
  await expect(page.locator('.calendar[data-enhanced]').first()).toBeAttached();
  return messages;
}

/** Waits until the opening fade is over: axe reads a half-transparent popup's colours as they are mid-way. */
async function waitUntilOpen(popup: Locator) {
  await expect.poll(() => popup.evaluate(element => element.matches(':popover-open'))).toBe(true);
  await expect.poll(() => popup.evaluate(element => getComputedStyle(element).opacity)).toBe('1');
}

async function expectClosed(popup: Locator) {
  await expect.poll(() => popup.evaluate(element => element.matches(':popover-open'))).toBe(false);
}

function day(scope: Page | Locator, iso: string) {
  return scope.locator(`.calendar-day-button[data-date="${iso}"]:not([data-outside])`);
}

function focusedDate(page: Page) {
  return page.evaluate(() => (document.activeElement as HTMLElement | null)?.dataset.date ?? null);
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

const trigger = (page: Page) => page.locator('.date-picker-trigger').first();
const popup = (page: Page) => page.locator('.date-picker-content').first();

for (const {skin, theme} of combinations) {
  test(`every date picker opens on its day, passes axe open, shows focus and closes on Escape: ${skin}, ${theme}`,
      async ({page}) => {
        await page.clock.setFixedTime(TODAY);
        await openShowcase(page, skin, theme);
        await page.waitForFunction(() => 'Alpine' in window);
        const ids = await page.locator('.showcase .date-picker-content').evaluateAll(popups => popups.map(p => p.id));
        expect(ids.length, 'the showcase has date pickers').toBeGreaterThan(0);

        const failures: string[] = [];
        for (const id of ids) {
          const content = page.locator(`#${id}`);
          const button = page.locator(`.showcase button[popovertarget="${id}"]`);
          if (await button.isDisabled()) {
            continue;
          }
          await button.focus();
          expect(await focusVisible(page), `${id}: the button shows focus`).toBe(true);
          await page.keyboard.press('Enter');
          await waitUntilOpen(content);
          await expect(button).toHaveAttribute('aria-expanded', 'true');
          await expect.poll(() => page.evaluate(() => document.activeElement?.matches('.calendar-day-button[tabindex="0"]')),
              `${id}: the day that takes the focus has it`).toBe(true);
          if (!await focusVisible(page)) {
            failures.push(`${id}: no visible focus on the day`);
          }

          const results = await new AxeBuilder({page}).include(`#${id}`).withTags(TAGS).analyze();
          failures.push(...results.violations.map(violation => `${id}: ${violation.id}: ${violation.help}\n    ${
              violation.nodes.map(node => node.target.join(' ')).join('\n    ')}`));

          await page.keyboard.press('Shift+Tab');
          expect(await content.evaluate(element => element.contains(document.activeElement)), `${id} keeps the focus`)
              .toBe(true);
          if (!await focusVisible(page)) {
            failures.push(`${id}: no visible focus before the day`);
          }

          await page.keyboard.press('Escape');
          await expectClosed(content);
          await expect(button).toBeFocused();
          await expect(button).toHaveAttribute('aria-expanded', 'false');
        }
        expect(failures, failures.join('\n')).toEqual([]);
      });
}

test('a pick writes the input, fires input and change, closes the popover and shows the date on the button',
    async ({page}) => {
      const messages = await open(page, `<form>${preview('default')}</form>`);
      const input = page.locator('#pickupDate');
      await countEvents(page, '#pickupDate');

      await expect(trigger(page)).toHaveText('Pick a date');
      await expect(trigger(page)).toHaveAttribute('data-empty', 'true');
      await expect(page.getByRole('button', {name: 'Pickup date Pick a date'})).toBeVisible();

      await trigger(page).click();
      await waitUntilOpen(popup(page));
      expect(await focusedDate(page), 'today takes the focus').toBe('2026-10-23');
      await expect(popup(page)).toHaveAccessibleName('Choose a date');

      await day(page, '2026-10-14').click();
      await expect(input).toHaveValue('2026-10-14');
      expect(await events(page)).toEqual({input: 1, change: 1});
      await expectClosed(popup(page));
      await expect(trigger(page)).toBeFocused();
      await expect(trigger(page)).toHaveText('October 14, 2026');
      await expect(trigger(page)).not.toHaveAttribute('data-empty');
      await expect(page.getByRole('button', {name: 'Pickup date October 14, 2026'})).toBeVisible();

      await trigger(page).click();
      await waitUntilOpen(popup(page));
      expect(await focusedDate(page), 'the chosen day takes the focus').toBe('2026-10-14');
      await day(page, '2026-10-14').click();
      await expect(input, 'a second click clears an optional date').toHaveValue('');
      await expect(popup(page), 'and keeps the popover open').toBeVisible();
      await expect(trigger(page)).toHaveText('Pick a date');
      await expect(trigger(page)).toHaveAttribute('data-empty', 'true');
      expect(messages).toEqual([]);
    });

test('keyboard: Enter opens on the chosen day, the arrows and Enter pick, Escape closes without a change',
    async ({page}) => {
      await open(page, `<form>${preview('chosen')}</form>`);
      const input = page.locator('#appointmentDate');
      await trigger(page).focus();
      await page.keyboard.press('Enter');
      await waitUntilOpen(popup(page));
      expect(await focusedDate(page)).toBe('2026-10-23');

      await page.keyboard.press('ArrowRight');
      await page.keyboard.press('ArrowDown');
      await page.keyboard.press('Escape');
      await expectClosed(popup(page));
      await expect(trigger(page)).toBeFocused();
      await expect(input).toHaveValue('2026-10-23');
      await expect(trigger(page)).toHaveText('October 23, 2026');

      await page.keyboard.press('Space');
      await waitUntilOpen(popup(page));
      await page.keyboard.press('ArrowRight');
      await page.keyboard.press('Enter');
      await expect(input).toHaveValue('2026-10-24');
      await expectClosed(popup(page));
      await expect(trigger(page)).toBeFocused();
      await expect(trigger(page)).toHaveText('October 24, 2026');
    });

test('Tab past the popover\'s last stop closes it', async ({page}) => {
  await open(page, `<form>${preview('chosen')}<button type="button" id="after">After</button></form>`);
  await trigger(page).click();
  await waitUntilOpen(popup(page));
  await page.keyboard.press('Tab');
  await expectClosed(popup(page));
});

test('a range: the first pick keeps the popover open, the second closes it; a new range starts again', async ({page}) => {
  await open(page, `<form>${preview('range')}</form>`);
  const start = page.locator('#tripStart');
  const end = page.locator('#tripEnd');
  await expect(trigger(page)).toHaveText('Oct 12, 2026 – Oct 21, 2026');
  await expect(page.getByRole('button', {name: 'Trip Oct 12, 2026 – Oct 21, 2026'})).toBeVisible();

  await trigger(page).click();
  await waitUntilOpen(popup(page));
  await day(page, '2026-11-02').click();
  await expect(start).toHaveValue('2026-11-02');
  await expect(end).toHaveValue('');
  await expect(trigger(page)).toHaveText('Nov 2, 2026');
  await page.waitForTimeout(50);
  await expect(popup(page), 'a new range does not close it').toBeVisible();

  await day(page, '2026-11-06').click();
  await expect(end).toHaveValue('2026-11-06');
  await expectClosed(popup(page));
  await expect(trigger(page)).toHaveText('Nov 2, 2026 – Nov 6, 2026');
  await expect(trigger(page)).toBeFocused();
});

test('Intl formats the date again in the page\'s locale and the date picker\'s format', async ({page}) => {
  const server = preview('chosen').replace(/(class="date-picker-value">)[^<]*/, '$1Rendered by the server');
  const belgian = preview('format').replaceAll('data-locale="en"', 'data-locale="nl-BE"');
  await open(page, `<form>${server}${belgian}</form>`);

  const values = page.locator('.date-picker-value');
  await expect(values).toHaveText(['October 23, 2026', '23/10/2026', 'vrijdag 23 oktober 2026']);
});

test('a click on the label focuses the button; the field\'s error marks and describes it', async ({page}) => {
  await open(page, `<form>${preview('invalid')}</form>`);
  await expect(trigger(page)).toHaveAttribute('aria-invalid', 'true');
  await expect(trigger(page)).toHaveAttribute('aria-describedby', 'returnDate-error');
  await expect(trigger(page)).toHaveAccessibleDescription('must not be empty');

  await page.locator('label.field-label').click();
  await expect(trigger(page)).toBeFocused();
  await expect(popup(page)).toBeHidden();
});

test('an empty required date opens the popover on submit; once picked, the form submits it', async ({page}) => {
  let submitted = '';
  const field = preview('invalid').replace(/<div class="field-error"[\s\S]*?<\/div>/, '')
      .replace(' aria-invalid="true"', '').replace(' aria-describedby="returnDate-error"', '');
  await open(page, `<form action="/submit" method="get">${field}<button id="send">Send</button></form>`, {
    routes: {
      '/submit': ({url}) => {
        submitted = url.search;
        return {body: '<p id="done">Sent</p>'};
      },
    },
  });
  await expect(trigger(page)).not.toHaveAttribute('aria-invalid');

  await page.locator('#send').click();
  await waitUntilOpen(popup(page));
  await expect(trigger(page)).toHaveAttribute('aria-invalid', 'true');
  expect(submitted).toBe('');

  await day(page, '2026-10-29').click();
  await expectClosed(popup(page));
  await expect(trigger(page)).not.toHaveAttribute('aria-invalid');
  await page.locator('#send').click();
  await expect(page.locator('#done')).toBeVisible();
  expect(submitted).toBe('?returnDate=2026-10-29');
});

test('form reset and a value set by a script (with a change event) show on the button', async ({page}) => {
  await open(page, `<form>${preview('chosen')}<button type="reset" id="reset">Reset</button></form>`);
  await trigger(page).click();
  await day(page, '2026-10-05').click();
  await expect(trigger(page)).toHaveText('October 5, 2026');

  await page.locator('#reset').click();
  await expect(trigger(page)).toHaveText('October 23, 2026');

  await page.evaluate(() => {
    const input = document.querySelector<HTMLInputElement>('#appointmentDate')!;
    input.value = '2027-03-08';
    input.dispatchEvent(new Event('change', {bubbles: true}));
  });
  await expect(trigger(page)).toHaveText('March 8, 2027');
  await expect(popup(page), 'a script does not open it').toBeHidden();
});

test('a disabled input disables the button, and enabling it again enables the button', async ({page}) => {
  await open(page, preview('disabled'));
  await expect(trigger(page)).toBeDisabled();
  await expect(trigger(page)).toHaveText('October 23, 2026');

  await page.evaluate(() => document.querySelector('.calendar > input')!.removeAttribute('disabled'));
  await expect(trigger(page)).toBeEnabled();
  await trigger(page).click();
  await waitUntilOpen(popup(page));
});

test('without Alpine the button shows the server\'s text and opens the bare date input', async ({page}) => {
  await page.clock.setFixedTime(TODAY);
  await openFixture(page, `<form>${preview('chosen')}</form>`, {alpine: false});
  await expect(trigger(page)).toHaveText('October 23, 2026');
  await trigger(page).click();
  await expect(page.locator('#appointmentDate')).toBeVisible();
  await expect(page.locator('.calendar-months')).toHaveCount(0);
});

// htmx saves the markup, which holds the input's rendered value, not the picked one: the restored button follows the
// input, so the two never disagree.
test('htmx: back restores the date picker with the text of its input and one working calendar', async ({page}) => {
  await open(page, `<form>${preview('chosen')}</form>`
      + '<a id="next" href="/next" hx-get="/next" hx-target="main" hx-push-url="true">Next</a>', {
    htmx: true,
    routes: {'/next': () => ({body: '<p id="next-page">Next page</p>'})},
  });
  const input = page.locator('#appointmentDate');
  await trigger(page).click();
  await day(page, '2026-10-21').click();
  await expect(trigger(page)).toHaveText('October 21, 2026');
  await page.locator('#next').click();
  await expect(page.locator('#next-page')).toBeVisible();
  await page.goBack();
  await expect(input).toHaveValue('2026-10-23');
  await expect(trigger(page)).toHaveText('October 23, 2026');
  await expect(page.locator('.calendar-months')).toHaveCount(1);

  await trigger(page).click();
  await waitUntilOpen(popup(page));
  expect(await focusedDate(page)).toBe('2026-10-23');
  await day(page, '2026-10-22').click();
  await expectClosed(popup(page));
  await expect(input).toHaveValue('2026-10-22');
  await expect(trigger(page)).toHaveText('October 22, 2026');
});

test('reopened after browsing other months, it shows the chosen date\'s month again', async ({page}) => {
  await open(page, `<form>${preview('chosen')}</form>`);
  await trigger(page).click();
  await waitUntilOpen(popup(page));
  await page.keyboard.press('PageDown');
  await page.keyboard.press('PageDown');
  expect(await page.locator('.calendar-caption').innerText()).toBe('December 2026');
  await page.keyboard.press('Escape');
  await expectClosed(popup(page));

  await page.keyboard.press('Enter');
  await waitUntilOpen(popup(page));
  expect(await page.locator('.calendar-caption').innerText()).toBe('October 2026');
  expect(await focusedDate(page)).toBe('2026-10-23');
});
