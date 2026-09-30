import {expect, test, type Locator, type Page} from '@playwright/test';
import AxeBuilder from '@axe-core/playwright';
import {join} from 'node:path';
import previews from '../src/generated/previews.json' with {type: 'json'};
import {combinations, openShowcase} from './showcase';

// sl:select, in both of the ways it is drawn:
// - 'base-select': the browser's customizable select (appearance: base-select), in Chromium;
// - 'listbox': slSelect's button and list box, where the customizable select is missing: in Firefox for real, and in
//   Chromium with CSS.supports('appearance', 'base-select') made to answer false.
// On the showcase, in every skin and theme: every select opens from the keyboard, passes axe open, shows focus on
// every item, closes on Escape with the value and focus unchanged, and Enter chooses. In fixture pages under a strict
// Content-Security-Policy with the csp Alpine build: the keyboard of the ARIA select-only combobox pattern, the pointer,
// submitting, the events, form reset, the label, the mirrored attributes, validation, autofocus, forced colours, and
// htmx (a form re-rendered with errors keeps its value and focus, a swap replacing an open select, history).

type Mode = 'base-select' | 'listbox';

const TAGS = ['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa', 'wcag22aa', 'best-practice'];

/** The modes a browser can run: Chromium both, Firefox (no customizable select) only the list box. */
function modes(browserName: string): Mode[] {
  return browserName === 'chromium' ? ['base-select', 'listbox'] : ['listbox'];
}

/** Makes the page see no customizable select, before any of its scripts run. */
async function withoutBaseSelect(page: Page) {
  await page.addInitScript(() => {
    const supports = CSS.supports.bind(CSS);
    CSS.supports = ((...args: [string, string?]) => !(args[0] === 'appearance' && args[1] === 'base-select')
        && supports(...(args as [string, string]))) as typeof CSS.supports;
  });
}

/** What a select looks like to the user: its parts in the current mode. */
class Select {
  constructor(readonly page: Page, readonly wrapper: Locator) {
  }

  get native() {
    return this.wrapper.locator('> select');
  }

  get trigger() {
    return this.wrapper.locator('> .select-trigger');
  }

  get listbox() {
    return this.wrapper.locator('> .select-content');
  }

  async mode(): Promise<Mode> {
    return await this.trigger.count() > 0 ? 'listbox' : 'base-select';
  }

  /** The element that takes the focus. */
  async control(): Promise<Locator> {
    return await this.mode() === 'listbox' ? this.trigger : this.native;
  }

  async isOpen(): Promise<boolean> {
    if (await this.mode() === 'listbox') {
      return this.listbox.evaluate(element => element.matches(':popover-open'));
    }
    return this.native.evaluate(element => element.matches(':open'));
  }

  async expectOpen() {
    await expect.poll(() => this.isOpen()).toBe(true);
    // The opening fade is over: axe reads a half-transparent list's colours as they are mid-way.
    if (await this.mode() === 'listbox') {
      await expect.poll(() => this.listbox.evaluate(element => getComputedStyle(element).opacity)).toBe('1');
    } else {
      await expect.poll(() => this.native.evaluate(element => getComputedStyle(element, '::picker(select)').opacity))
          .toBe('1');
    }
  }

  async expectClosed() {
    await expect.poll(() => this.isOpen()).toBe(false);
  }

  /** The text of the highlighted item: the focused option, or the list box's active one. */
  async highlighted(): Promise<string | null> {
    if (await this.mode() === 'listbox') {
      return this.trigger.evaluate(trigger => {
        const id = trigger.getAttribute('aria-activedescendant');
        return id ? document.getElementById(id)!.textContent!.trim() : null;
      });
    }
    return this.page.evaluate(() => document.activeElement instanceof HTMLOptionElement
        ? document.activeElement.label : null);
  }

  /** Whether the highlighted item shows a focus ring (a box-shadow) or an outline. */
  async highlightVisible(): Promise<boolean> {
    const selector = await this.mode() === 'listbox' ? '[data-active]' : 'option:focus-visible';
    return this.wrapper.evaluate((wrapper, selector) => {
      const item = wrapper.querySelector(selector) ?? (wrapper.ownerDocument.activeElement?.matches(selector)
          ? wrapper.ownerDocument.activeElement : null);
      if (!item) {
        return false;
      }
      const style = getComputedStyle(item);
      return style.boxShadow !== 'none' || (style.outlineStyle !== 'none' && parseFloat(style.outlineWidth) > 0);
    }, selector);
  }

  /** The value shown on the closed select. */
  async shown(): Promise<string> {
    if (await this.mode() === 'listbox') {
      return (await this.trigger.innerText()).trim();
    }
    return this.native.evaluate(select => select.querySelector('selectedcontent')!.textContent!.trim());
  }

  async value(): Promise<string> {
    return this.native.evaluate(select => (select as HTMLSelectElement).value);
  }

  /** Clicks an item in the open list by its text. */
  async clickItem(text: string) {
    if (await this.mode() === 'listbox') {
      await this.listbox.getByRole('option', {name: text}).click();
    } else {
      await this.native.locator('option', {hasText: text}).click();
    }
  }
}

function axeFailures(label: string, violations: Awaited<ReturnType<AxeBuilder['analyze']>>['violations']) {
  return violations.map(violation => `${label}: ${violation.id}: ${violation.help}\n    ${
      violation.nodes.map(node => node.target.join(' ')).join('\n    ')}`);
}

// --- the showcase ----------------------------------------------------------------------------------------------------

for (const {skin, theme} of combinations) {
  // The customizable select in Chromium, the list box in Firefox, where it is real.
  test(`every select works with the keyboard and passes axe open: ${skin}, ${theme}`, async ({page, browserName}) => {
    const mode: Mode = browserName === 'chromium' ? 'base-select' : 'listbox';
    {
      await openShowcase(page, skin, theme);
      await page.waitForFunction(() => 'Alpine' in window);
      const wrappers = page.locator('main .select-wrapper:not(:has(> select:disabled))');
      const count = await wrappers.count();
      expect(count, 'the showcase has selects').toBeGreaterThan(0);

      const failures: string[] = [];
      for (let i = 0; i < count; i++) {
        const select = new Select(page, wrappers.nth(i));
        expect(await select.mode()).toBe(mode);
        const label = `${mode} select ${i} (${await select.native.evaluate(element => element.id || element.getAttribute('name'))})`;
        const control = await select.control();
        const valueBefore = await select.value();
        const shownBefore = await select.shown();

        await control.focus();
        await page.keyboard.press('ArrowDown');
        await select.expectOpen();

        await wrappers.nth(i).evaluate(element => element.setAttribute('data-axe', ''));
        const results = await new AxeBuilder({page}).include('[data-axe]').withTags(TAGS).analyze();
        await wrappers.nth(i).evaluate(element => element.removeAttribute('data-axe'));
        failures.push(...axeFailures(label, results.violations));

        // Through every enabled item: each shows the focus.
        const items = await select.native.evaluate(element =>
            [...(element as HTMLSelectElement).options].filter(option => !option.hidden && !option.disabled).length);
        for (let j = 0; j < items; j++) {
          if (!await select.highlightVisible()) {
            failures.push(`${label}: no visible focus on ${await select.highlighted()}`);
          }
          await page.keyboard.press('ArrowDown');
        }

        await page.keyboard.press('Escape');
        await select.expectClosed();
        await expect(control).toBeFocused();
        expect(await select.value(), `${label}: Escape keeps the value`).toBe(valueBefore);
        expect(await select.shown()).toBe(shownBefore);

        // Enter chooses: the last item, so the value changes unless it was chosen already.
        await page.keyboard.press('ArrowDown');
        await select.expectOpen();
        await page.keyboard.press('End');
        const last = await select.highlighted();
        await page.keyboard.press('Enter');
        await select.expectClosed();
        await expect(control).toBeFocused();
        expect(await select.shown(), `${label}: Enter chooses`).toBe(last);
      }
      expect(failures, failures.join('\n')).toEqual([]);
    }
  });
}

// --- fixture pages ---------------------------------------------------------------------------------------------------

const ORIGIN = 'http://app.test';
const PUBLIC_DIR = join(import.meta.dirname, '..', 'public');
const HTMX = join(import.meta.dirname, '..', 'node_modules', 'htmx.org', 'dist', 'htmx.min.js');
const STRICT_CSP = "default-src 'self'; script-src 'self'; style-src 'self'; object-src 'none'; base-uri 'self'";
const HTMX_CONFIG = `<meta name="htmx-config" content='{"includeIndicatorStyles": false, "allowEval": false}'>`;

type Scripts = { bundled: string, csp: string, external: string };
const scripts = (previews as unknown as { scripts: Scripts }).scripts;
const css = previews.skins[0].css;

type Item = string | { value: string, text?: string, disabled?: boolean, selected?: boolean };

const FRUITS: Item[] = ['Apple', 'Banana', {value: 'blueberry', text: 'Blueberry', disabled: true}, 'Cherry', 'Grapes',
  'Pineapple'];

function option(item: Item): string {
  const {value, text, disabled, selected} = typeof item === 'string' ? {value: item.toLowerCase(), text: item} : item;
  return `<option class="select-item" value="${value}"${disabled ? ' disabled' : ''}${selected ? ' selected' : ''}>${
      text ?? value}<svg class="sl-icon select-item-indicator" viewBox="0 0 24 24" aria-hidden="true"><path d="M20 6 9 17l-5-5"/></svg></option>`;
}

/** A select marked up as <sl:select> renders it (see select.approved.html). */
function select({id = 'fruit', name = 'fruit', items = FRUITS, placeholder = '', attributes = ''} = {}): string {
  return `<div class="select-wrapper" x-data="slSelect"><select class="select" id="${id}" name="${name}" ${attributes}>`
      + '<button type="button"><selectedcontent></selectedcontent></button>'
      + (placeholder ? `<option class="select-item" value="" hidden data-placeholder="true">${placeholder}</option>` : '')
      + items.map(option).join('')
      + '</select><svg class="sl-icon select-icon" viewBox="0 0 24 24" aria-hidden="true"><path d="m6 9 6 6 6-6"/></svg>'
      + '<template class="select-indicator-template"><svg class="sl-icon select-item-indicator" viewBox="0 0 24 24"'
      + ' aria-hidden="true"><path d="M20 6 9 17l-5-5"/></svg></template></div>';
}

type Response = { body: string, headers?: Record<string, string>, status?: number };
type Routes = Record<string, (body: string, headers: Record<string, string>, url: URL) => Response>;

/**
 * Serves `body` at / with the csp build (and htmx), and `routes` for other requests; returns what the console said.
 * In 'listbox' mode on Chromium the page is told there is no customizable select.
 */
async function openFixture(page: Page, mode: Mode, body: string, {htmx = false, routes = {} as Routes} = {}):
    Promise<string[]> {
  const messages: string[] = [];
  page.on('console', message => {
    // Not Firefox's deprecation warnings, which Alpine's csp build causes by reading every property of window.
    if (message.type() === 'error' || (message.type() === 'warning' && !message.text().includes('deprecated'))) {
      messages.push(message.text());
    }
  });
  page.on('pageerror', error => messages.push(error.message));
  await page.addInitScript(() => document.addEventListener('securitypolicyviolation',
      event => console.error(`CSP violation: ${event.violatedDirective}`)));
  if (mode === 'listbox') {
    await withoutBaseSelect(page);
  }
  const document = (main: string) => `<!doctype html><html lang="en"><head><title>Fixture</title>`
      + `<link rel="stylesheet" href="/${css}">`
      + (htmx ? `${HTMX_CONFIG}<script defer src="/webjars/htmx.min.js"></script>` : '')
      + `<script type="module" src="/${scripts.csp}"></script></head><body><main>${main}</main></body></html>`;
  await page.route(`${ORIGIN}/**`, route => {
    const request = route.request();
    const url = new URL(request.url());
    if (url.pathname === '/webjars/htmx.min.js') {
      return route.fulfill({contentType: 'text/javascript', path: HTMX});
    }
    if (url.pathname.startsWith('/shadleaf/')) {
      return route.fulfill({path: join(PUBLIC_DIR, url.pathname)});
    }
    const headers = {'Content-Security-Policy': STRICT_CSP};
    if (url.pathname === '/') {
      return route.fulfill({contentType: 'text/html', headers, body: document(body)});
    }
    const handler = routes[url.pathname];
    if (handler) {
      const response = handler(request.postData() ?? '', request.headers(), url);
      const html = request.headers()['hx-request'] === 'true' ? response.body : document(response.body);
      return route.fulfill({contentType: 'text/html', status: response.status, headers: {...headers, ...response.headers},
        body: html});
    }
    return route.fulfill({status: 404});
  });
  await page.goto(`${ORIGIN}/`);
  await page.waitForFunction(() => 'Alpine' in window);
  if (htmx) {
    await page.waitForFunction(() => 'htmx' in window);
  }
  return messages;
}

/** Counts the input and change events the select fires, in window.slEvents. */
async function countEvents(page: Page, selector = '#fruit') {
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

for (const mode of ['base-select', 'listbox'] as Mode[]) {
  test.describe(mode, () => {
    test.beforeEach(({browserName}) => {
      test.skip(!modes(browserName).includes(mode), `${browserName} has no ${mode}`);
    });

    test('the csp build runs it under a strict policy: arrows, Home and End, disabled items skipped, Enter, Escape',
        async ({page}) => {
          const messages = await openFixture(page, mode, `<label for="fruit">Fruit</label>${
              select({items: FRUITS.map(item => item === 'Cherry' ? {value: 'cherry', text: 'Cherry', selected: true}
                  : item)})}<button type="button" id="after">After</button>`);
          const s = new Select(page, page.locator('.select-wrapper'));
          expect(await s.mode()).toBe(mode);
          const control = await s.control();
          await countEvents(page);

          await control.focus();
          await page.keyboard.press('ArrowDown');
          await s.expectOpen();
          expect(await s.highlighted(), 'opens on the chosen item').toBe('Cherry');
          await page.keyboard.press('ArrowUp');
          expect(await s.highlighted(), 'skips the disabled item').toBe('Banana');
          await page.keyboard.press('End');
          expect(await s.highlighted()).toBe('Pineapple');
          await page.keyboard.press('Home');
          expect(await s.highlighted()).toBe('Apple');
          await page.keyboard.press('Escape');
          await s.expectClosed();
          await expect(control).toBeFocused();
          expect(await s.value(), 'Escape chooses nothing').toBe('cherry');
          expect(await events(page)).toEqual({input: 0, change: 0});

          await page.keyboard.press('ArrowDown');
          await s.expectOpen();
          await page.keyboard.press('ArrowDown');
          await page.keyboard.press('Enter');
          await s.expectClosed();
          await expect(control).toBeFocused();
          expect(await s.value()).toBe('grapes');
          expect(await s.shown()).toBe('Grapes');
          expect(await events(page)).toEqual({input: 1, change: 1});
          expect(messages).toEqual([]);
        });

    test('Enter and Space open it, Home and End open it on the first and last item', async ({page}) => {
      await openFixture(page, mode, `<label for="fruit">Fruit</label>${select()}`);
      const s = new Select(page, page.locator('.select-wrapper'));
      const control = await s.control();

      for (const [key, expected] of [['Enter', 'Apple'], [' ', 'Apple'], ['Home', 'Apple'], ['End', 'Pineapple']]) {
        await control.focus();
        await page.keyboard.press(key === ' ' ? 'Space' : key);
        await s.expectOpen();
        expect(await s.highlighted(), `${key} opens the list`).toBe(expected);
        await page.keyboard.press('Escape');
        await s.expectClosed();
      }
      expect(await s.value(), 'opening chooses nothing').toBe('apple');
    });

    test('typeahead: a letter goes to the next item starting with it, a quick word to a prefix', async ({page}) => {
      await openFixture(page, mode, `<label for="fruit">Fruit</label>${select()}`);
      const s = new Select(page, page.locator('.select-wrapper'));
      const control = await s.control();

      await control.focus();
      await page.keyboard.press('ArrowDown');
      await s.expectOpen();
      await page.keyboard.type('g');
      expect(await s.highlighted()).toBe('Grapes');
      await page.waitForTimeout(1100);
      await page.keyboard.type('pi');
      expect(await s.highlighted()).toBe('Pineapple');
      await page.keyboard.press('Enter');
      await s.expectClosed();
      expect(await s.value()).toBe('pineapple');
    });

    test('pointer: a click opens it, a click on an item chooses it, a disabled item cannot be chosen',
        async ({page}) => {
          await openFixture(page, mode, `<label for="fruit">Fruit</label>${select()}`);
          const s = new Select(page, page.locator('.select-wrapper'));
          const control = await s.control();
          await countEvents(page);

          await control.click();
          await s.expectOpen();
          await s.clickItem('Cherry');
          await s.expectClosed();
          await expect(control).toBeFocused();
          expect(await s.value()).toBe('cherry');
          expect(await events(page)).toEqual({input: 1, change: 1});

          await control.click();
          await s.expectOpen();
          if (mode === 'listbox') {
            await s.listbox.getByRole('option', {name: 'Blueberry'}).click({force: true});
          } else {
            await s.native.locator('option[value=blueberry]').click({force: true});
          }
          expect(await s.value()).toBe('cherry');
          await page.keyboard.press('Escape');
          await s.expectClosed();

          // A click outside closes it without choosing.
          await control.click();
          await s.expectOpen();
          await page.mouse.move(5, 5);
          await page.mouse.click(5, 400);
          await s.expectClosed();
          expect(await s.value()).toBe('cherry');
        });

    test('the item under the pointer is highlighted, and the keyboard goes on from it', async ({page}) => {
      await openFixture(page, mode, `<label for="fruit">Fruit</label>${select()}`);
      const s = new Select(page, page.locator('.select-wrapper'));
      await (await s.control()).focus();
      await page.keyboard.press('ArrowDown');
      await s.expectOpen();

      const cherry = mode === 'listbox' ? s.listbox.getByRole('option', {name: 'Cherry'})
          : s.native.locator('option[value=cherry]');
      await cherry.hover();
      await expect.poll(() => s.highlighted()).toBe('Cherry');
      await page.keyboard.press('ArrowDown');
      expect(await s.highlighted()).toBe('Grapes');
    });

    test('the chosen value is submitted with the form, and form reset puts it back', async ({page}) => {
      let submitted = '';
      await openFixture(page, mode, `<form action="/submit" method="get"><label for="fruit">Fruit</label>${
          select({items: FRUITS.map(item => item === 'Banana' ? {value: 'banana', text: 'Banana', selected: true}
              : item)})}<button type="reset" id="reset">Reset</button><button id="send">Send</button></form>`, {
        routes: {
          '/submit': (_body, _headers, url) => {
            submitted = url.search;
            return {body: '<p id="done">Sent</p>'};
          },
        },
      });
      const s = new Select(page, page.locator('.select-wrapper'));
      const control = await s.control();

      await control.focus();
      await page.keyboard.press('ArrowDown');
      await s.expectOpen();
      await page.keyboard.press('End');
      await page.keyboard.press('Enter');
      await s.expectClosed();
      expect(await s.shown()).toBe('Pineapple');

      await page.locator('#reset').click();
      await expect.poll(() => s.shown()).toBe('Banana');
      expect(await s.value()).toBe('banana');

      await control.focus();
      await page.keyboard.press('ArrowDown');
      await s.expectOpen();
      await page.keyboard.press('Home');
      await page.keyboard.press('Enter');
      await s.expectClosed();
      await page.locator('#send').click();
      await expect(page.locator('#done')).toBeVisible();
      expect(submitted).toBe('?fruit=apple');
    });

    test('a click on its label focuses it; it is named by the label and described like the select', async ({page}) => {
      await openFixture(page, mode, `<label for="fruit">Fruit</label>${select({
        attributes: 'aria-describedby="fruit-description" aria-invalid="true" required',
      })}<p id="fruit-description">Pick one you like.</p>`);
      const s = new Select(page, page.locator('.select-wrapper'));
      const control = await s.control();

      await page.locator('label').click();
      await expect(control).toBeFocused();
      await expect(page.getByRole('combobox', {name: 'Fruit'})).toBeFocused();
      await expect(page.getByRole('combobox', {name: 'Fruit'})).toHaveAccessibleDescription('Pick one you like.');
      await expect(control).toHaveAttribute('aria-invalid', 'true');
      if (mode === 'listbox') {
        await expect(control).toHaveAttribute('aria-required', 'true');
        await expect(s.native).toHaveAttribute('aria-hidden', 'true');
        await expect(s.native).toHaveAttribute('tabindex', '-1');
        expect(await page.getByRole('combobox').count(), 'the hidden select is not a second combobox').toBe(1);
      }
    });

    test('it follows the select: disabled, aria-invalid and new options', async ({page}) => {
      await openFixture(page, mode, `<label for="fruit">Fruit</label>${select()}`);
      const s = new Select(page, page.locator('.select-wrapper'));
      const control = await s.control();

      await s.native.evaluate(element => element.setAttribute('disabled', ''));
      await expect(control).toBeDisabled();
      await s.native.evaluate(element => element.removeAttribute('disabled'));
      await expect(control).toBeEnabled();

      await s.native.evaluate(element => element.setAttribute('aria-invalid', 'true'));
      await expect(control).toHaveAttribute('aria-invalid', 'true');

      // New options, as a request for dependent choices swaps them in; the first becomes the chosen one.
      await s.native.evaluate(element => {
        element.querySelectorAll('option').forEach(option => option.remove());
        element.insertAdjacentHTML('beforeend',
            '<option class="select-item" value="kiwi">Kiwi</option><option class="select-item" value="lime">Lime</option>');
      });
      await expect.poll(() => s.shown()).toBe('Kiwi');
      await control.focus();
      await page.keyboard.press('ArrowDown');
      await s.expectOpen();
      await page.keyboard.press('ArrowDown');
      expect(await s.highlighted()).toBe('Lime');
      await page.keyboard.press('Enter');
      expect(await s.value()).toBe('lime');
    });

    test('a disabled fieldset disables it', async ({page}) => {
      await openFixture(page, mode, `<fieldset disabled><legend>Order</legend><label for="fruit">Fruit</label>${
          select()}</fieldset>`);
      const s = new Select(page, page.locator('.select-wrapper'));
      await expect(await s.control()).toBeDisabled();
    });

    test('the placeholder shows until an item is chosen, and a required select with it is invalid', async ({page}) => {
      await openFixture(page, mode, `<form action="/submit"><label for="fruit">Fruit</label>${
          select({placeholder: 'Select a fruit', attributes: 'required'})}<button id="send">Send</button></form>
          <p id="elsewhere" tabindex="-1">Elsewhere</p>`);
      const s = new Select(page, page.locator('.select-wrapper'));
      const control = await s.control();

      expect(await s.shown()).toBe('Select a fruit');
      expect(await s.value()).toBe('');
      await control.focus();
      await page.keyboard.press('ArrowDown');
      await s.expectOpen();
      expect(await s.highlighted(), 'the placeholder is not in the list').toBe('Apple');
      await page.keyboard.press('Escape');

      await page.locator('#elsewhere').focus();
      await page.locator('#send').click();
      // The browser refuses to submit and focuses the select; the list box's button takes it over.
      await expect(control).toBeFocused();
      expect(page.url()).toBe(`${ORIGIN}/`);
    });

    test('autofocus on the select puts the focus on it', async ({page}) => {
      await openFixture(page, mode, `<label for="fruit">Fruit</label>${select({attributes: 'autofocus'})}`);
      const s = new Select(page, page.locator('.select-wrapper'));
      await expect(await s.control()).toBeFocused();
    });

    test('forced colours: the list keeps a border and the highlighted item an outline', async ({page}) => {
      await page.emulateMedia({forcedColors: 'active'});
      await openFixture(page, mode, `<label for="fruit">Fruit</label>${select()}`);
      const s = new Select(page, page.locator('.select-wrapper'));
      await (await s.control()).focus();
      await page.keyboard.press('ArrowDown');
      await s.expectOpen();

      const border = mode === 'listbox'
          ? await s.listbox.evaluate(element => getComputedStyle(element).borderTopStyle)
          : await s.native.evaluate(element => getComputedStyle(element, '::picker(select)').borderTopStyle);
      expect(border).not.toBe('none');
      expect(await s.highlightVisible()).toBe(true);
    });

    test('the list opens below the select, at least as wide, and above it only when there is no room below',
        async ({page}) => {
          await openFixture(page, mode, `<div id="stage"><label for="fruit">Fruit</label>${select()}</div>`);
          const s = new Select(page, page.locator('.select-wrapper'));
          const control = await s.control();
          // Where the list is: the box around its items (the picker is a pseudo-element, which has no box of its own).
          const list = () => page.evaluate(mode => {
            const items = [...document.querySelectorAll(mode === 'listbox' ? '.select-content [role=option]'
                : '#fruit option:not([hidden])')].map(item => item.getBoundingClientRect());
            return {top: Math.min(...items.map(item => item.top)), bottom: Math.max(...items.map(item => item.bottom)),
              left: Math.min(...items.map(item => item.left)), width: Math.max(...items.map(item => item.width))};
          }, mode);

          // Lower on the page than the middle, with room below: more room above must not move it there.
          const height = await page.evaluate(() => window.innerHeight);
          await page.locator('#stage').evaluate((stage, top) => stage.style.setProperty('margin-top', `${top}px`),
              Math.round(height * 0.6));
          await control.click();
          await s.expectOpen();
          let box = (await control.boundingBox())!;
          let items = await list();
          expect(items.top, 'below the select').toBeGreaterThan(box.y + box.height);
          expect(items.left).toBeGreaterThanOrEqual(box.x - 1);
          expect(items.left).toBeLessThan(box.x + 12);
          expect(items.width, 'at least as wide').toBeGreaterThanOrEqual(Math.min(box.width, 144) - 12);
          await page.keyboard.press('Escape');
          await s.expectClosed();

          await page.locator('#stage').evaluate((stage, top) => stage.style.setProperty('margin-top', `${top}px`),
              height - 60);
          await control.click();
          await s.expectOpen();
          box = (await control.boundingBox())!;
          items = await list();
          expect(items.bottom, 'above the select').toBeLessThan(box.y);
        });

    test('an icon in an item is 16 pixels, in the list and on the select', async ({page}) => {
      const icon = (id: string) => `<svg class="sl-icon" id="${id}" viewBox="0 0 24 24" width="24" height="24"`
          + ' aria-hidden="true"><circle cx="12" cy="12" r="10"/></svg>';
      await openFixture(page, mode, `<label for="fruit">Fruit</label>${select({
        items: [{value: 'sun', text: `${icon('sun-icon')}Light`, selected: true}, {value: 'moon', text: `${icon('moon-icon')}Dark`}],
      })}`);
      const s = new Select(page, page.locator('.select-wrapper'));
      await (await s.control()).click();
      await s.expectOpen();
      const sizes = await page.evaluate(mode => {
        const inList = mode === 'listbox' ? document.querySelector('.select-content [role=option] svg:not(.select-item-indicator)')!
            : document.querySelector('#moon-icon')!;
        const onSelect = mode === 'listbox' ? document.querySelector('.select-value svg')!
            : document.querySelector('#fruit selectedcontent svg:not(.select-item-indicator)')!;
        return [inList, onSelect].map(svg => svg.getBoundingClientRect().width);
      }, mode);
      expect(sizes).toEqual([16, 16]);
    });

    test('htmx: a form rendered again with errors keeps the chosen value, and focus goes to the select',
        async ({page}) => {
          const form = (fruit: string, errors: boolean) => `<form id="order" action="/order" method="post"
              hx-post="/order" hx-target="this" hx-swap="outerHTML">
            <label for="fruit">Fruit</label>
            ${select({
            placeholder: 'Select a fruit',
            items: FRUITS.map(item => {
              const value = typeof item === 'string' ? item.toLowerCase() : item.value;
              const base = typeof item === 'string' ? {value, text: item} : item;
              return {...base, selected: value === fruit};
            }),
            attributes: errors ? 'aria-invalid="true" aria-describedby="fruit-error" autofocus' : '',
          })}
            ${errors ? '<p id="fruit-error">We are out of apples.</p>' : ''}
            <label for="note">Note</label><input id="note" name="note">
            <button id="send">Send</button></form>`;
          const messages = await openFixture(page, mode, form('', false), {
            htmx: true,
            routes: {
              '/order': body => {
                const fruit = new URLSearchParams(body).get('fruit') ?? '';
                return {body: form(fruit, fruit === 'apple' || fruit === '')};
              },
            },
          });
          const s = new Select(page, page.locator('.select-wrapper'));
          await (await s.control()).focus();
          await page.keyboard.press('ArrowDown');
          await s.expectOpen();
          await page.keyboard.press('Enter');
          await s.expectClosed();
          expect(await s.value()).toBe('apple');

          await page.locator('#note').fill('Ripe ones');
          await page.locator('#note').press('Enter');
          await expect(page.locator('#fruit-error')).toBeVisible();
          const again = new Select(page, page.locator('.select-wrapper'));
          await expect.poll(() => again.mode()).toBe(mode);
          expect(await again.value()).toBe('apple');
          expect(await again.shown()).toBe('Apple');
          await expect(await again.control()).toBeFocused();
          await expect(await again.control()).toHaveAttribute('aria-invalid', 'true');
          expect(await page.locator('.select-trigger').count(), 'one button, not one per render')
              .toBe(mode === 'listbox' ? 1 : 0);
          expect(messages).toEqual([]);
        });

    test('htmx: hx-trigger="change" sends the chosen value', async ({page}) => {
      let sent = '';
      await openFixture(page, mode, `<label for="fruit">Fruit</label>${select({
        attributes: 'hx-get="/price" hx-trigger="change" hx-target="#price"',
      })}<p id="price">-</p>`, {
        htmx: true,
        routes: {
          '/price': (_body, _headers, url) => {
            sent = url.search;
            return {body: 'Price: 2'};
          },
        },
      });
      const s = new Select(page, page.locator('.select-wrapper'));
      await (await s.control()).click();
      await s.expectOpen();
      await s.clickItem('Grapes');
      await expect(page.locator('#price')).toHaveText('Price: 2');
      expect(sent).toBe('?fruit=grapes');
    });

    test('htmx: a swap that replaces an open select leaves a closed, working one', async ({page}) => {
      await openFixture(page, mode, `<div id="row"><label for="fruit">Fruit</label>${select()}</div>`, {
        htmx: true,
        routes: {'/row': () => ({body: `<label for="fruit">Fruit</label>${select({items: ['Kiwi', 'Lime']})}`})},
      });
      const s = new Select(page, page.locator('.select-wrapper'));
      await (await s.control()).click();
      await s.expectOpen();
      await page.evaluate(() => (window as unknown as { htmx: { ajax: Function } }).htmx.ajax('GET', '/row',
          {target: '#row', swap: 'innerHTML'}));
      await expect.poll(() => s.shown()).toBe('Kiwi');
      expect(await s.isOpen(), 'nothing is left open').toBe(false);
      expect(await page.locator(':popover-open').count()).toBe(0);

      await (await s.control()).click();
      await s.expectOpen();
      await s.clickItem('Lime');
      expect(await s.value()).toBe('lime');
    });

    test('htmx: back restores the select with one working list', async ({page}) => {
      await openFixture(page, mode, `<label for="fruit">Fruit</label>${select()}
          <a id="next" href="/next" hx-get="/next" hx-target="main" hx-push-url="true">Next</a>`, {
        htmx: true,
        routes: {'/next': () => ({body: '<p id="next-page">Next page</p>'})},
      });
      const s = new Select(page, page.locator('.select-wrapper'));
      await (await s.control()).click();
      await s.expectOpen();
      await s.clickItem('Cherry');
      await page.locator('#next').click();
      await expect(page.locator('#next-page')).toBeVisible();
      await page.goBack();
      await expect(page.locator('.select-wrapper')).toBeVisible();
      await expect.poll(() => page.locator('.select-trigger').count()).toBe(mode === 'listbox' ? 1 : 0);
      expect(await page.locator('.select-content').count()).toBe(mode === 'listbox' ? 1 : 0);
      expect(await s.isOpen()).toBe(false);
      await (await s.control()).click();
      await s.expectOpen();
      await s.clickItem('Grapes');
      expect(await s.shown()).toBe('Grapes');
    });
  });
}

test.describe('listbox only', () => {
  test.beforeEach(({browserName}) => {
    test.skip(!modes(browserName).includes('listbox'));
  });

  test('Tab chooses the highlighted item and moves on; Alt+ArrowUp chooses and closes', async ({page}) => {
    await openFixture(page, 'listbox', `<label for="fruit">Fruit</label>${select()}
        <button type="button" id="after">After</button>`);
    const s = new Select(page, page.locator('.select-wrapper'));
    await s.trigger.focus();
    await page.keyboard.press('ArrowDown');
    await page.keyboard.press('ArrowDown');
    await page.keyboard.press('Tab');
    await s.expectClosed();
    await expect(page.locator('#after')).toBeFocused();
    expect(await s.value()).toBe('banana');

    await s.trigger.focus();
    await page.keyboard.press('ArrowDown');
    await page.keyboard.press('ArrowDown');
    await page.keyboard.press('Alt+ArrowUp');
    await s.expectClosed();
    expect(await s.value()).toBe('cherry');
  });

  test('typing on the closed button opens the list on the matching item', async ({page}) => {
    await openFixture(page, 'listbox', `<label for="fruit">Fruit</label>${select()}`);
    const s = new Select(page, page.locator('.select-wrapper'));
    await s.trigger.focus();
    await page.keyboard.type('c');
    await s.expectOpen();
    expect(await s.highlighted()).toBe('Cherry');
    expect(await s.value(), 'nothing is chosen yet').toBe('apple');
  });

});

test.describe('base-select only', () => {
  test.beforeEach(({browserName}) => {
    test.skip(!modes(browserName).includes('base-select'));
  });

  test('the list is the browser\'s picker: no list box, the chosen item\'s icon shows in the select', async ({page}) => {
    await openFixture(page, 'base-select', `<label for="fruit">Fruit</label>${select({
      items: [{value: 'sun', text: '<svg class="sl-icon" id="sun-icon" viewBox="0 0 24 24" aria-hidden="true"></svg>Light'}],
    })}`);
    const s = new Select(page, page.locator('.select-wrapper'));
    expect(await s.mode()).toBe('base-select');
    expect(await s.native.evaluate(element => getComputedStyle(element).appearance)).toBe('base-select');
    await expect(s.native.locator('selectedcontent svg#sun-icon')).toBeVisible();
    await expect(s.native.locator('selectedcontent .select-item-indicator'), 'no check mark').toBeHidden();
    expect(await s.shown()).toBe('Light');
  });
});
