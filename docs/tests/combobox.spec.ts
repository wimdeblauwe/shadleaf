import {expect, test, type Locator, type Page} from '@playwright/test';
import AxeBuilder from '@axe-core/playwright';
import previews from '../src/generated/previews.json' with {type: 'json'};
import {combinations, keepScenariosWith, openShowcase} from './showcase';
import {openFixture, type Routes} from './fixture';

// sl:combobox: a real select that slCombobox replaces by an input and a list box. On the showcase, in every skin and
// theme: every combobox opens from the keyboard, passes axe open, shows the highlight on every item, and Escape closes
// it with the value unchanged. In fixture pages under a strict Content-Security-Policy with the csp Alpine build, with the markup of the docs previews: filtering, the keyboard of the ARIA combobox pattern,
// the pointer, the events, clearing, auto-highlight, multiple with its chips, submitting, form reset, the label and
// the mirrored attributes, validation, search-url (debounce, results, the chosen option kept, the event, errors), no
// Alpine, a dialog around it, and htmx's history. Runs in Chromium and Firefox.

const TAGS = ['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa', 'wcag22aa', 'best-practice'];

type Scenario = { id: string, html: string };

/** A scenario's markup, as the server renders it for the docs. */
const preview = (id: string) => (previews.scenarios as Scenario[]).find(scenario => scenario.id === `combobox--${id}`)!
    .html;

async function open(page: Page, main: string, options: Parameters<typeof openFixture>[2] = {}) {
  const messages = await openFixture(page, main, options);
  if (options.alpine !== false) {
    await expect(page.locator('.combobox[data-enhanced]').first()).toBeAttached();
  }
  return messages;
}

/** What a combobox looks like to the user, by its select's id. */
class Combobox {
  readonly root: Locator;

  constructor(readonly page: Page, readonly id: string) {
    this.root = page.locator(`.combobox:has(> select#${id})`);
  }

  get select() {
    return this.root.locator('> select');
  }

  get input() {
    return this.root.locator('.combobox-input');
  }

  get content() {
    return this.root.locator('> .combobox-content');
  }

  get chips() {
    return this.root.locator('.combobox-chip');
  }

  option(name: string) {
    return this.content.getByRole('option', {name, exact: true});
  }

  async isOpen() {
    return this.content.evaluate(element => element.matches(':popover-open'));
  }

  /** Waits until the opening fade is over: axe reads a half-transparent popup's colours as they are mid-way. */
  async expectOpen() {
    await expect.poll(() => this.isOpen()).toBe(true);
    await expect.poll(() => this.content.evaluate(element => getComputedStyle(element).opacity)).toBe('1');
    await expect(this.input).toHaveAttribute('aria-expanded', 'true');
  }

  async expectClosed() {
    await expect.poll(() => this.isOpen()).toBe(false);
    await expect(this.input).toHaveAttribute('aria-expanded', 'false');
  }

  /** The text of the highlighted option, through aria-activedescendant. */
  highlighted() {
    return this.input.evaluate(input => {
      const id = input.getAttribute('aria-activedescendant');
      return id ? document.getElementById(id)!.textContent!.trim() : null;
    });
  }

  /** The texts of the options the list shows. */
  visible() {
    return this.content.locator('[role="option"]:not([hidden])').evaluateAll(rows =>
        rows.map(row => row.textContent!.trim()));
  }

  value() {
    return this.select.evaluate(select => (select as HTMLSelectElement).value);
  }

  values() {
    return this.select.evaluate(select => [...(select as HTMLSelectElement).selectedOptions]
        .filter(option => option.value !== '').map(option => option.value));
  }

  chipTexts() {
    return this.chips.locator('.combobox-chip-label').allTextContents();
  }
}

/** Counts the input and change events the select fires, in window.slEvents. */
async function countEvents(page: Page, selector: string) {
  await page.evaluate(selector => {
    const counts = {input: 0, change: 0};
    (window as unknown as { slEvents: typeof counts }).slEvents = counts;
    const select = document.querySelector(selector)!;
    select.addEventListener('input', () => counts.input++);
    select.addEventListener('change', () => counts.change++);
  }, selector);
}

function events(page: Page) {
  return page.evaluate(() => (window as unknown as { slEvents: { input: number, change: number } }).slEvents);
}

/** Whether the element shows its focus or highlight: an outline, a box-shadow ring or another background. */
async function stands(locator: Locator, against: Locator) {
  return locator.evaluate((element, other) => {
    const style = getComputedStyle(element);
    return (style.outlineStyle !== 'none' && parseFloat(style.outlineWidth) > 0) || style.boxShadow !== 'none'
        || style.backgroundColor !== getComputedStyle(other!).backgroundColor;
  }, await against.elementHandle());
}

// --- the showcase ----------------------------------------------------------------------------------------------------

for (const {skin, theme} of combinations) {
  test(`every combobox opens from the keyboard, passes axe open, shows focus and closes on Escape: ${skin}, ${theme}`,
      async ({page}) => {
        await openShowcase(page, skin, theme);
        await keepScenariosWith(page, '.combobox');
        await page.waitForFunction(() => 'Alpine' in window);
        const ids = await page.locator('.showcase .combobox[data-enhanced] > select:not(:disabled)')
            .evaluateAll(selects => selects.map(select => select.id));
        expect(ids.length, 'the showcase has comboboxes').toBeGreaterThan(0);

        const failures: string[] = [];
        for (const id of ids) {
          const combobox = new Combobox(page, id);
          const before = await combobox.values();
          // The focus ring on the control is checked with every other control's by focus.spec.ts.
          await combobox.input.focus();
          await page.keyboard.press('ArrowDown');
          await combobox.expectOpen();

          await combobox.root.evaluate(element => element.setAttribute('data-axe', ''));
          const results = await new AxeBuilder({page}).include('[data-axe]').withTags(TAGS).analyze();
          await combobox.root.evaluate(element => element.removeAttribute('data-axe'));
          failures.push(...results.violations.map(violation => `${id}: ${violation.id}: ${violation.help}\n    ${
              violation.nodes.map(node => node.target.join(' ')).join('\n    ')}`));

          // Through every enabled option: each stands out from the list.
          const count = await combobox.content.locator('[role="option"]:not([aria-disabled="true"])').count();
          for (let i = 0; i < Math.min(count, 8); i++) {
            const active = combobox.content.locator('[role="option"][data-active]');
            if (await active.count() !== 1 || !await stands(active, combobox.content)) {
              failures.push(`${id}: no visible highlight on ${await combobox.highlighted()}`);
            }
            await page.keyboard.press('ArrowDown');
          }

          await page.keyboard.press('Escape');
          await combobox.expectClosed();
          await expect(combobox.input).toBeFocused();
          expect(await combobox.values(), `${id}: Escape keeps the value`).toEqual(before);
        }
        expect(failures, failures.join('\n')).toEqual([]);
      });
}

// --- fixture pages ---------------------------------------------------------------------------------------------------

test('the csp build runs it under a strict policy: typing filters, the arrows and Enter choose, events fire',
    async ({page}) => {
      const messages = await open(page, `<form>${preview('groups')}</form>`);
      const combobox = new Combobox(page, 'meetingZone');
      await countEvents(page, '#meetingZone');
      await expect(combobox.select).toHaveAttribute('aria-hidden', 'true');
      await expect(combobox.select).toHaveCSS('opacity', '0');
      await expect(combobox.input).toHaveAttribute('placeholder', 'Select a timezone');
      await expect(combobox.input).toHaveAccessibleName('Timezone');

      await combobox.input.focus();
      await page.keyboard.type('SAO');
      await combobox.expectOpen();
      expect(await combobox.visible(), 'case and accents are ignored').toEqual(['(GMT-3) São Paulo']);
      await expect(combobox.content.getByText('Europe', {exact: true}), 'a group without matches hides').toBeHidden();
      await expect(combobox.content.locator('.combobox-separator').first()).toBeHidden();
      expect(await combobox.highlighted(), 'nothing is highlighted without auto-highlight').toBeNull();

      await page.keyboard.press('ArrowDown');
      expect(await combobox.highlighted()).toBe('(GMT-3) São Paulo');
      await page.keyboard.press('Enter');
      await combobox.expectClosed();
      await expect(combobox.input).toBeFocused();
      await expect(combobox.input).toHaveValue('(GMT-3) São Paulo');
      expect(await combobox.value()).toBe('America/Sao_Paulo');
      expect(await events(page)).toEqual({input: 1, change: 1});

      await page.keyboard.press('ArrowDown');
      await combobox.expectOpen();
      expect(await combobox.visible(), 'reopened, the list shows every option').toHaveLength(84);
      expect(await combobox.highlighted(), 'on the chosen option').toBe('(GMT-3) São Paulo');
      await page.keyboard.press('End');
      expect(await combobox.highlighted(), 'End moves the caret, not the highlight').toBe('(GMT-3) São Paulo');
      await page.keyboard.press('PageDown');
      expect(await combobox.highlighted(), 'ten further, into the next group').toBe('(GMT+1) Madrid');
      await page.keyboard.press('Alt+ArrowUp');
      await combobox.expectClosed();
      expect(await combobox.value()).toBe('Europe/Madrid');

      await page.keyboard.type('zzz');
      await expect(combobox.content.locator('.combobox-empty')).toHaveText('No items found.');
      await expect(combobox.content.locator('.combobox-empty')).toBeVisible();
      await expect(combobox.root.locator('.combobox-status')).toHaveText('No items found.');
      expect(messages).toEqual([]);
    });

test('Escape and Tab put the text of the choice back; an emptied input clears it', async ({page}) => {
  await open(page, `<form>${preview('icons')}</form><button id="after">After</button>`);
  const combobox = new Combobox(page, 'issueStatus');
  await countEvents(page, '#issueStatus');
  await expect(combobox.input).toHaveValue('Todo');

  await combobox.input.focus();
  await page.keyboard.press('End');
  await page.keyboard.type('xyz');
  await combobox.expectOpen();
  await page.keyboard.press('Escape');
  await combobox.expectClosed();
  await expect(combobox.input).toHaveValue('Todo');

  await page.keyboard.type('Do');
  await page.keyboard.press('Tab');
  await combobox.expectClosed();
  await expect(page.locator('#after')).toBeFocused();
  await expect(combobox.input).toHaveValue('Todo');
  expect(await events(page)).toEqual({input: 0, change: 0});

  await combobox.input.fill('');
  await page.locator('#after').focus();
  await expect(combobox.input).toHaveValue('');
  expect(await combobox.value()).toBe('');
  expect(await events(page)).toEqual({input: 1, change: 1});
});

test('the list shows an item\'s icon; the check mark follows the choice', async ({page}) => {
  await open(page, preview('icons'));
  const combobox = new Combobox(page, 'issueStatus');
  await combobox.input.click();
  await combobox.expectOpen();
  await expect(combobox.option('Todo').locator('svg').first()).toBeVisible();
  await expect(combobox.option('Todo')).toHaveAttribute('aria-selected', 'true');
  await expect(combobox.option('Todo').locator('.combobox-item-indicator')).toBeVisible();
  await expect(combobox.option('Done').locator('.combobox-item-indicator')).toBeHidden();
});

test('auto-highlight highlights the first match, so Enter chooses it', async ({page}) => {
  await open(page, preview('long-list'));
  const combobox = new Combobox(page, 'destination');
  await combobox.input.focus();
  await page.keyboard.type('land');
  expect(await combobox.highlighted()).toBe('Finland');
  await expect(combobox.root.locator('.combobox-status')).toHaveText('7 results');
  await page.keyboard.press('Enter');
  await combobox.expectClosed();
  expect(await combobox.value()).toBe('finland');

  await combobox.input.fill('');
  await page.keyboard.type('turk');
  expect(await combobox.visible(), 'accents are ignored, as the docs say').toEqual(['Türkiye']);
});

test('pointer: a click opens it, an item chooses, a disabled one cannot, outside closes, the chevron toggles',
    async ({page}) => {
      await open(page, preview('disabled'));
      const combobox = new Combobox(page, 'build');
      await countEvents(page, '#build');

      await combobox.input.click();
      await combobox.expectOpen();
      await combobox.option('Ant').click({force: true});
      expect(await combobox.value()).toBe('');
      await combobox.option('Gradle').click();
      await combobox.expectClosed();
      await expect(combobox.input).toBeFocused();
      await expect(combobox.input).toHaveValue('Gradle');
      expect(await events(page)).toEqual({input: 1, change: 1});

      const chevron = combobox.root.locator('.combobox-trigger');
      await chevron.click();
      await combobox.expectOpen();
      await expect(chevron).toHaveAttribute('aria-expanded', 'true');
      await expect(combobox.input, 'the chevron leaves the focus in the input').toBeFocused();
      await chevron.click();
      await combobox.expectClosed();

      await combobox.input.click();
      await combobox.expectOpen();
      await page.mouse.click(700, 10);
      await combobox.expectClosed();

      const runtime = new Combobox(page, 'runtime');
      await expect(runtime.input).toBeDisabled();
      await expect(runtime.input).toHaveValue('JVM');
    });

test('the clear button takes the chevron\'s place while there is a choice, and clears it', async ({page}) => {
  await open(page, preview('clear'));
  const combobox = new Combobox(page, 'stack');
  const clear = combobox.root.locator('.combobox-clear');
  const chevron = combobox.root.locator('.combobox-trigger');
  await expect(clear).toBeVisible();
  await expect(clear).toHaveAccessibleName('Clear');
  await expect(chevron).toBeHidden();

  await clear.click();
  expect(await combobox.value()).toBe('');
  await expect(combobox.input).toHaveValue('');
  await expect(combobox.input).toBeFocused();
  await expect(clear).toBeHidden();
  await expect(chevron).toBeVisible();
});

test('multiple: Enter toggles and keeps the list open, chips show the choice, Backspace and the chips remove',
    async ({page}) => {
      let submitted = '';
      const messages = await open(page, `<form action="/submit" method="get">${preview('multiple')}`
          + '<button id="send">Send</button></form>', {
        routes: {
          '/submit': ({url}) => {
            submitted = url.search;
            return {body: '<p id="done">Sent</p>'};
          },
        },
      });
      const combobox = new Combobox(page, 'frameworks');
      await countEvents(page, '#frameworks');
      expect(await combobox.chipTexts()).toEqual(['Next.js']);
      await expect(combobox.input, 'no placeholder next to chips').toHaveAttribute('placeholder', '');
      await expect(combobox.content.locator('[role="listbox"]')).toHaveAttribute('aria-multiselectable', 'true');

      await combobox.input.focus();
      await page.keyboard.type('nu');
      expect(await combobox.highlighted()).toBe('Nuxt.js');
      await page.keyboard.press('Enter');
      await combobox.expectOpen();
      await expect(combobox.input, 'the text goes').toHaveValue('');
      expect(await combobox.visible(), 'and the list shows every option again').toHaveLength(5);
      expect(await combobox.chipTexts()).toEqual(['Next.js', 'Nuxt.js']);
      expect(await combobox.values()).toEqual(['next', 'nuxt']);
      await expect(combobox.option('Nuxt.js')).toHaveAttribute('aria-selected', 'true');

      expect(await combobox.highlighted(), 'the highlight stays on the option').toBe('Nuxt.js');
      await page.keyboard.press('ArrowUp');
      await page.keyboard.press('ArrowUp');
      await page.keyboard.press('Enter');
      expect(await combobox.values(), 'Enter on a chosen option removes it').toEqual(['nuxt']);
      expect(await events(page)).toEqual({input: 2, change: 2});

      await combobox.option('Astro').click();
      expect(await combobox.chipTexts()).toEqual(['Nuxt.js', 'Astro']);
      await page.keyboard.press('Backspace');
      expect(await combobox.chipTexts()).toEqual(['Nuxt.js']);

      await combobox.option('Remix').click();
      await page.keyboard.press('Escape');
      await combobox.expectClosed();
      await page.keyboard.press('ArrowLeft');
      const remove = combobox.chips.locator('.combobox-chip-remove');
      await expect(remove.last()).toBeFocused();
      await expect(remove.last()).toHaveAccessibleName('Remove Remix');
      await page.keyboard.press('ArrowLeft');
      await expect(remove.first()).toBeFocused();
      await page.keyboard.press('Delete');
      expect(await combobox.chipTexts()).toEqual(['Remix']);
      await expect(remove.first(), 'the focus goes to the chip in its place').toBeFocused();
      await page.keyboard.press('ArrowRight');
      await expect(combobox.input).toBeFocused();

      await remove.first().click();
      expect(await combobox.chipTexts()).toEqual([]);
      await expect(combobox.input).toHaveAttribute('placeholder', 'Add a framework');

      await combobox.input.click();
      await combobox.option('SvelteKit').click();
      await combobox.option('Astro').click();
      await page.keyboard.press('Escape');
      await page.locator('#send').click();
      await expect(page.locator('#done')).toBeVisible();
      expect(submitted).toBe('?_frameworks=1&frameworks=sveltekit&frameworks=astro');
      expect(messages).toEqual([]);
    });

test('the input takes the select\'s label, description, invalid state and required; a label click focuses it',
    async ({page}) => {
      await open(page, preview('invalid'));
      const combobox = new Combobox(page, 'language');
      await expect(combobox.input).toHaveAccessibleName('Language');
      await expect(combobox.input).toHaveAttribute('aria-invalid', 'true');
      await expect(combobox.input).toHaveAttribute('aria-describedby', 'language-error');
      await expect(combobox.input).toHaveAccessibleDescription('must not be empty');
      await expect(combobox.content.locator('[role="listbox"]')).toHaveAttribute('aria-labelledby', 'language-label');
      await expect(combobox.input).toHaveAttribute('aria-labelledby', 'language-label');

      await page.getByText('Language', {exact: true}).click();
      await expect(combobox.input).toBeFocused();

      await combobox.select.evaluate(select => {
        select.removeAttribute('aria-invalid');
        select.setAttribute('required', '');
      });
      await expect(combobox.input).not.toHaveAttribute('aria-invalid');
      await expect(combobox.input).toHaveAttribute('aria-required', 'true');
    });

test('an empty required combobox stops the submit and gets the focus; once chosen, the form submits it',
    async ({page}) => {
      let submitted = '';
      await open(page, `<form action="/submit" method="get">${preview('default').replace('<select ', '<select required ')}`
          + '<button id="send">Send</button></form>', {
        routes: {
          '/submit': ({url}) => {
            submitted = url.search;
            return {body: '<p id="done">Sent</p>'};
          },
        },
      });
      const combobox = new Combobox(page, 'framework');
      await page.locator('#send').click();
      await expect(combobox.input).toBeFocused();
      expect(submitted).toBe('');

      await page.keyboard.type('astro');
      await page.keyboard.press('ArrowDown');
      await page.keyboard.press('Enter');
      await page.locator('#send').click();
      await expect(page.locator('#done')).toBeVisible();
      expect(submitted).toBe('?framework=astro');
    });

test('form reset and a value set by a script (with a change event) show in the input and the chips', async ({page}) => {
  await open(page, `<form>${preview('clear')}${preview('multiple')}<button type="reset" id="reset">Reset</button></form>`);
  const single = new Combobox(page, 'stack');
  const multiple = new Combobox(page, 'frameworks');
  await single.input.click();
  await single.option('Nuxt.js').click();
  await multiple.input.click();
  await multiple.option('Astro').click();
  await page.keyboard.press('Escape');
  expect(await multiple.chipTexts()).toEqual(['Next.js', 'Astro']);

  await page.locator('#reset').click();
  await expect(single.input).toHaveValue('SvelteKit');
  await expect.poll(() => multiple.chipTexts()).toEqual(['Next.js']);

  await page.evaluate(() => {
    const select = document.querySelector<HTMLSelectElement>('#stack')!;
    select.value = 'next';
    select.dispatchEvent(new Event('change', {bubbles: true}));
  });
  await expect(single.input).toHaveValue('Next.js');
  expect(await single.isOpen(), 'a script does not open it').toBe(false);
});

test('options swapped into the select show in the list', async ({page}) => {
  await open(page, preview('default'));
  const combobox = new Combobox(page, 'framework');
  await combobox.select.evaluate(select => {
    select.insertAdjacentHTML('beforeend', '<option class="combobox-item" value="qwik">Qwik</option>');
  });
  await combobox.input.click();
  await expect(combobox.option('Qwik')).toBeVisible();
});

test('in a modal dialog, the list shows above it and Escape closes the list, not the dialog', async ({page}) => {
  await open(page, `<dialog id="dialog" aria-label="Project">${preview('default')}</dialog>`);
  await page.evaluate(() => document.querySelector<HTMLDialogElement>('#dialog')!.showModal());
  const combobox = new Combobox(page, 'framework');
  await combobox.input.click();
  await combobox.expectOpen();
  await combobox.option('Remix').click();
  expect(await combobox.value()).toBe('remix');
  await page.keyboard.press('ArrowDown');
  await combobox.expectOpen();
  await page.keyboard.press('Escape');
  await combobox.expectClosed();
  await expect(page.locator('#dialog')).toBeVisible();
});

test('without Alpine it is the select, which submits', async ({page}) => {
  await open(page, `<form>${preview('multiple')}${preview('default')}</form>`, {alpine: false});
  await expect(page.locator('#framework')).toBeVisible();
  await expect(page.locator('#frameworks')).toBeVisible();
  await expect(page.locator('.combobox-input')).toHaveCount(0);
  await page.locator('#framework').selectOption('astro');
  expect(await page.locator('#framework').inputValue()).toBe('astro');
});

test('htmx: back restores the combobox with one working control', async ({page}) => {
  await open(page, `<form>${preview('multiple')}</form>`
      + '<a id="next" href="/next" hx-get="/next" hx-target="main" hx-push-url="true">Next</a>', {
    htmx: true,
    routes: {'/next': () => ({body: '<p id="next-page">Next page</p>'})},
  });
  const combobox = new Combobox(page, 'frameworks');
  await combobox.input.click();
  await combobox.option('Remix').click();
  await page.keyboard.press('Escape');
  await page.locator('#next').click();
  await expect(page.locator('#next-page')).toBeVisible();
  await page.goBack();
  await expect(combobox.root.locator('.combobox-control')).toHaveCount(1);
  await expect(combobox.root.locator('.combobox-content')).toHaveCount(1);
  await combobox.input.click();
  await combobox.expectOpen();
  await combobox.option('Astro').click();
  await expect.poll(() => combobox.values()).toContain('astro');
  await expect.poll(() => combobox.chipTexts()).toContain('Astro');
});

// --- search-url ------------------------------------------------------------------------------------------------------

const COUNTRIES = ['Belgium', 'Bolivia', 'Botswana', 'Brazil', 'Bulgaria', 'Germany', 'Ghana', 'Greece'];

const searchable = (attributes = '') => '<label for="country">Country</label>'
    + `<div class="combobox" x-data="slCombobox" data-search-url="/countries"${attributes}`
    + ' data-searching="Searching…" data-search-error="The options could not be loaded."'
    + ' data-result="1 result" data-results="{0} results">'
    + '<select class="combobox-select" id="country" name="country">'
    + '<option value="" hidden data-placeholder="true"></option>'
    + '<option class="combobox-item" value="germany" selected>Germany</option></select>'
    + preview('default').match(/<template class="combobox-template">[\s\S]*?<\/template>/)![0]
    + '</div>';

function countries(log: string[], {status = 200, delay = 0, redirect = false} = {}): Routes {
  return {
    '/countries': ({url}) => {
      const q = url.searchParams.get('q')!;
      log.push(url.search);
      if (redirect) {
        return {status: 302, headers: {Location: '/login'}, body: ''};
      }
      // Not text/html, which the fixture wraps in a page.
      return {
        status, delay, contentType: 'text/html; charset=utf-8', body: COUNTRIES.filter(country => country.toLowerCase().startsWith(q.toLowerCase()))
            .map(country => `<option class="combobox-item" value="${country.toLowerCase()}">${country}</option>`)
            .join(''),
      };
    },
    '/login': () => ({body: '<form id="login"></form>'}),
  };
}

test('search-url: typing asks the server once it pauses, and the answer replaces the options', async ({page}) => {
  const log: string[] = [];
  const messages = await open(page, `<form>${searchable()}</form>`, {routes: countries(log)});
  const combobox = new Combobox(page, 'country');
  await countEvents(page, '#country');

  await combobox.input.focus();
  await page.keyboard.press('ArrowDown');
  await combobox.expectOpen();
  expect(await combobox.visible(), 'the options of the page').toEqual(['Germany']);

  await combobox.input.fill('');
  await page.keyboard.type('bo', {delay: 50});
  await expect(combobox.content.locator('.combobox-empty')).toHaveText('Searching…');
  await expect.poll(() => combobox.visible()).toEqual(['Bolivia', 'Botswana']);
  expect(log, 'one request, after the typing paused').toEqual(['?q=bo']);
  await expect(combobox.root.locator('.combobox-status')).toHaveText('2 results');
  expect(await combobox.value(), 'the chosen option stays').toBe('germany');
  expect(await combobox.select.locator('option[value="germany"]').evaluate(option => (option as HTMLOptionElement).hidden))
      .toBe(true);

  await page.keyboard.press('ArrowDown');
  await page.keyboard.press('ArrowDown');
  await page.keyboard.press('Enter');
  await combobox.expectClosed();
  expect(await combobox.value()).toBe('botswana');
  await expect(combobox.input).toHaveValue('Botswana');
  expect(await events(page)).toEqual({input: 1, change: 1});

  await page.keyboard.press('ArrowDown');
  await combobox.expectOpen();
  expect(await combobox.visible(), 'closing brought back the page\'s options, and the choice').toEqual(
      ['Germany', 'Botswana']);

  await combobox.input.fill('zz');
  await expect.poll(() => log.at(-1)).toBe('?q=zz');
  await expect(combobox.content.locator('.combobox-empty')).toHaveText('No items found.');
  expect(await combobox.value()).toBe('botswana');
  expect(messages).toEqual([]);
});

test('search-url: a newer query wins over a slow answer, and sl-combobox-search adds parameters', async ({page}) => {
  const log: string[] = [];
  let first = true;
  const routes = countries(log);
  const slow: Routes = {
    '/countries': request => {
      const response = routes['/countries'](request);
      if (first) {
        first = false;
        return {...response, delay: 1500};
      }
      return response;
    },
  };
  await open(page, searchable(), {routes: slow});
  await page.evaluate(() => document.addEventListener('sl-combobox-search', event => {
    (event as CustomEvent<{ url: URL }>).detail.url.searchParams.set('region', 'eu');
  }));
  const combobox = new Combobox(page, 'country');

  await combobox.input.fill('b');
  await expect.poll(() => log.length).toBe(1);
  await combobox.input.fill('gr');
  await expect.poll(() => combobox.visible()).toEqual(['Greece']);
  await page.waitForTimeout(1700);
  expect(await combobox.visible(), 'the slow answer to "b" came too late').toEqual(['Greece']);
  expect(log).toEqual(['?q=b&region=eu', '?q=gr&region=eu']);
});

test('search-url: an error or a redirect (an expired session) shows that the options could not be loaded',
    async ({page}) => {
      for (const options of [{status: 500}, {redirect: true}]) {
        const log: string[] = [];
        await page.unrouteAll();
        await open(page, searchable(), {routes: countries(log, options)});
        const combobox = new Combobox(page, 'country');
        await combobox.input.fill('b');
        await expect(combobox.content.locator('.combobox-empty'), JSON.stringify(options))
            .toHaveText('The options could not be loaded.');
        await expect(combobox.root.locator('.combobox-status')).toHaveText('The options could not be loaded.');
        expect(await combobox.value()).toBe('germany');
      }
    });
