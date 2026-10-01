import {expect, test, type Page} from '@playwright/test';
import AxeBuilder from '@axe-core/playwright';
import previews from '../src/generated/previews.json' with {type: 'json'};
import {combinations, openShowcase} from './showcase';
import {openFixture, type Routes} from './fixture';

// sl:theme-toggle. On the showcase, in every skin and theme: the menu and the button pass axe, show focus, and choose
// with the keyboard. In fixture pages under a strict Content-Security-Policy with the csp Alpine build and the theme
// script: the stored value (a plain string, no key for System), the choice surviving a reload with no flash of the other
// theme, following the system's theme while on System, several toggles and two tabs in step, forced colours, htmx
// swaps and history, and what shows without Alpine and without JavaScript.

const TAGS = ['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa', 'wcag22aa', 'best-practice'];
const KEY = 'shadleaf-theme';

type Scenario = { id: string, html: string };
const scenarios = previews.scenarios as Scenario[];
const html = (id: string) => scenarios.find(scenario => scenario.id === id)!.html;
/** The menu <sl:theme-toggle/> renders (id theme-toggle), and the button of as="button". */
const MENU = html('theme-toggle--default');
const BUTTON = html('theme-toggle--button');

const isDark = (page: Page) => page.evaluate(() => document.documentElement.classList.contains('dark'));
const stored = (page: Page) => page.evaluate(key => localStorage.getItem(key), KEY);
const checked = (page: Page, scope = 'body') => page.locator(`${scope} [data-theme-choice][aria-checked="true"]`)
    .evaluateAll(items => items.map(item => (item as HTMLElement).dataset.themeChoice));
/** The icon the trigger shows: the one of its two that is displayed. */
const icon = (page: Page, trigger = '.theme-toggle-trigger') => page.locator(trigger).first()
    .evaluate(button => [...button.querySelectorAll('svg')]
        .filter(svg => getComputedStyle(svg).display !== 'none')
        .map(svg => svg.classList.contains('theme-toggle-dark') ? 'moon' : 'sun'));

async function choose(page: Page, theme: 'light' | 'dark' | 'system', menu = '#theme-toggle') {
  await page.locator(`${menu}-trigger`).click();
  await expect(page.locator(menu)).toBeVisible();
  await page.locator(`${menu} [data-theme-choice="${theme}"]`).click();
  await expect(page.locator(menu)).toBeHidden();
}

// --- the showcase ------------------------------------------------------------------------------------------------------

for (const {skin, theme} of combinations) {
  test(`the menu and the button pass axe and choose with the keyboard: ${skin}, ${theme}`, async ({page}) => {
    await openShowcase(page, skin, theme);
    await page.waitForFunction(() => 'Alpine' in window);
    const stage = page.locator('[data-scenario="theme-toggle--default"]');
    const trigger = stage.locator('.theme-toggle-trigger');
    const button = page.locator('[data-scenario="theme-toggle--button"] .theme-toggle-button');
    const dark = theme === 'dark';

    // The showcase sets the class itself; the toggles show it and change nothing when they start.
    expect(await isDark(page)).toBe(dark);
    await expect(button).toHaveAttribute('aria-pressed', String(dark));
    expect(await icon(page)).toEqual([dark ? 'moon' : 'sun']);
    await expect(trigger).toHaveAccessibleName('Theme');
    await expect(button).toHaveAccessibleName('Dark mode');

    await trigger.focus();
    await page.keyboard.press('Enter');
    const menu = page.locator('#theme-toggle');
    await expect(menu).toBeVisible();
    // Axe reads colours half-way through the fade in.
    await expect.poll(() => menu.evaluate(element => getComputedStyle(element).opacity)).toBe('1');
    expect(await checked(page)).toEqual(['system']);
    const results = await new AxeBuilder({page}).include('#theme-toggle').include('.theme-toggle-button')
        .withTags(TAGS).analyze();
    expect(results.violations.map(violation => `${violation.id}: ${violation.help}\n    ${
        violation.nodes.map(node => `${node.target.join(' ')} ${node.failureSummary}`).join('\n    ')}`)).toEqual([]);

    // The keyboard opened it on Light; down to Dark, Enter chooses, and the focus goes back to the trigger.
    await expect(menu.getByRole('menuitemradio', {name: 'Light'})).toBeFocused();
    await page.keyboard.press('ArrowDown');
    await page.keyboard.press('Enter');
    await expect(menu).toBeHidden();
    await expect(trigger).toBeFocused();
    expect(await isDark(page)).toBe(true);
    expect(await stored(page)).toBe('dark');
    await expect(button).toHaveAttribute('aria-pressed', 'true');
    expect(await icon(page)).toEqual(['moon']);

    // The button: Space switches to light, with a visible focus ring.
    await button.focus();
    expect(await button.evaluate(element => {
      const style = getComputedStyle(element);
      return element.matches(':focus-visible')
          && ((style.outlineStyle !== 'none' && parseFloat(style.outlineWidth) > 0) || style.boxShadow !== 'none');
    })).toBe(true);
    await page.keyboard.press('Space');
    await expect(button).toHaveAttribute('aria-pressed', 'false');
    expect(await isDark(page)).toBe(false);
    expect(await stored(page)).toBe('light');
    await trigger.click();
    expect(await checked(page)).toEqual(['light']);
  });
}

// --- fixture pages -----------------------------------------------------------------------------------------------------

test('the csp build stores the plain string, and System removes the key', async ({page}) => {
  await page.emulateMedia({colorScheme: 'light'});
  const messages = await openFixture(page, MENU, {theme: true});
  await expect(page.locator('#theme-toggle-trigger')).toHaveAttribute('aria-expanded', 'false');
  expect(await checked(page)).toEqual(['system']);

  await choose(page, 'dark');
  expect(await stored(page)).toBe('dark');
  expect(await isDark(page)).toBe(true);
  expect(await checked(page)).toEqual(['dark']);

  await choose(page, 'light');
  expect(await stored(page)).toBe('light');
  expect(await isDark(page)).toBe(false);

  await page.emulateMedia({colorScheme: 'dark'});
  await choose(page, 'system');
  expect(await stored(page)).toBeNull();
  expect(await isDark(page)).toBe(true);
  expect(await checked(page)).toEqual(['system']);
  expect(messages).toEqual([]);
});

test('the choice survives a reload, with no flash of the other theme', async ({page}) => {
  await page.emulateMedia({colorScheme: 'light'});
  // Before the page's own scripts: the class when <body> is parsed and at the first frame, and the icon then.
  await page.addInitScript(() => {
    const record = (name: string) => {
      const trigger = document.querySelector('.theme-toggle-trigger');
      const shown = trigger ? [...trigger.querySelectorAll('svg')]
          .filter(svg => getComputedStyle(svg).display !== 'none')
          .map(svg => svg.classList.contains('theme-toggle-dark') ? 'moon' : 'sun') : null;
      (window as any)[name] = {dark: document.documentElement.classList.contains('dark'), shown};
    };
    new MutationObserver((records, observer) => {
      if (document.body) {
        record('atBody');
        observer.disconnect();
      }
    }).observe(document, {childList: true, subtree: true});
    requestAnimationFrame(() => record('atFirstFrame'));
  });
  await openFixture(page, MENU, {theme: true});
  expect(await page.evaluate(() => (window as any).atBody.dark)).toBe(false);

  await choose(page, 'dark');
  await page.reload();
  await page.waitForFunction(() => 'Alpine' in window);
  expect(await page.evaluate(() => (window as any).atBody.dark), 'dark before the body is parsed').toBe(true);
  expect(await page.evaluate(() => (window as any).atFirstFrame), 'dark, with the moon, at the first frame')
      .toEqual({dark: true, shown: ['moon']});
  expect(await checked(page)).toEqual(['dark']);
});

test('the icon is right without Alpine, and nothing is checked or pressed', async ({page}) => {
  await page.emulateMedia({colorScheme: 'dark'});
  await openFixture(page, MENU + BUTTON, {theme: true, alpine: false});
  expect(await isDark(page)).toBe(true);
  expect(await icon(page)).toEqual(['moon']);
  expect(await icon(page, '.theme-toggle-button')).toEqual(['moon']);
  await expect(page.locator('.theme-toggle-button')).not.toHaveAttribute('aria-pressed', /.*/);
  expect(await checked(page)).toEqual([]);
});

test('without JavaScript the toggle is not shown', async ({browser}) => {
  const context = await browser.newContext({javaScriptEnabled: false});
  const page = await context.newPage();
  await openFixture(page, MENU + BUTTON, {theme: true, alpine: false});
  await expect(page.locator('.theme-toggle-trigger')).toHaveCount(2);
  await expect(page.locator('.theme-toggle-trigger').first()).toBeHidden();
  await expect(page.locator('.theme-toggle-button')).toBeHidden();
  await context.close();
});

test('on System the page follows the operating system; a stored choice does not', async ({page}) => {
  await page.emulateMedia({colorScheme: 'light'});
  await openFixture(page, MENU + BUTTON, {theme: true});
  const changes = await page.evaluateHandle(() => {
    const seen: unknown[] = [];
    document.addEventListener('sl-theme-change', event => seen.push((event as CustomEvent).detail));
    return seen;
  });
  expect(await isDark(page)).toBe(false);

  await page.emulateMedia({colorScheme: 'dark'});
  await expect.poll(() => isDark(page)).toBe(true);
  await expect(page.locator('.theme-toggle-button')).toHaveAttribute('aria-pressed', 'true');
  expect(await icon(page)).toEqual(['moon']);
  expect(await changes.evaluate(seen => seen)).toEqual([{theme: 'system', dark: true}]);

  await choose(page, 'light');
  await page.emulateMedia({colorScheme: 'light'});
  await page.emulateMedia({colorScheme: 'dark'});
  expect(await isDark(page)).toBe(false);
  expect(await changes.evaluate(seen => seen)).toEqual([{theme: 'system', dark: true}, {theme: 'light', dark: false}]);
});

test('several toggles on a page stay in step', async ({page}) => {
  await page.emulateMedia({colorScheme: 'light'});
  const second = MENU
      .replace('id="theme-toggle-trigger"', 'id="footer-theme-trigger"')
      .replace('popovertarget="theme-toggle"', 'popovertarget="footer-theme"')
      .replace('id="theme-toggle"', 'id="footer-theme"')
      .replace('aria-labelledby="theme-toggle-trigger"', 'aria-labelledby="footer-theme-trigger"');
  await openFixture(page, `<header>${MENU}${BUTTON}</header><footer>${second}</footer>`, {theme: true});
  const button = page.locator('.theme-toggle-button');

  await choose(page, 'dark');
  await expect(button).toHaveAttribute('aria-pressed', 'true');
  expect(await checked(page, '#footer-theme')).toEqual(['dark']);

  await button.click();
  expect(await isDark(page)).toBe(false);
  expect(await checked(page, '#theme-toggle')).toEqual(['light']);
  expect(await checked(page, '#footer-theme')).toEqual(['light']);

  await choose(page, 'system', '#footer-theme');
  expect(await checked(page, '#theme-toggle')).toEqual(['system']);
  await expect(button).toHaveAttribute('aria-pressed', 'false');
});

test('a choice in another tab changes this one', async ({context}) => {
  const first = await context.newPage();
  const second = await context.newPage();
  for (const page of [first, second]) {
    await page.emulateMedia({colorScheme: 'light'});
    await openFixture(page, MENU + BUTTON, {theme: true});
  }

  await choose(first, 'dark');
  await expect.poll(() => isDark(second)).toBe(true);
  expect(await checked(second)).toEqual(['dark']);
  await expect(second.locator('.theme-toggle-button')).toHaveAttribute('aria-pressed', 'true');

  await choose(second, 'system');
  await expect.poll(() => isDark(first)).toBe(false);
  expect(await checked(first)).toEqual(['system']);
});

test('forced colours: the icon and the check mark show', async ({page}) => {
  await page.emulateMedia({colorScheme: 'light', forcedColors: 'active'});
  await openFixture(page, MENU, {theme: true});
  await choose(page, 'dark');
  expect(await icon(page)).toEqual(['moon']);
  await page.locator('#theme-toggle-trigger').click();
  const indicator = page.locator('[data-theme-choice="dark"] .dropdown-menu-item-indicator');
  await expect(indicator).toBeVisible();
  await expect(page.locator('[data-theme-choice="light"] .dropdown-menu-item-indicator')).toBeHidden();
  const stroke = await indicator.locator('svg').evaluate(svg => getComputedStyle(svg).stroke);
  expect(stroke, 'the check mark is drawn in a system colour').not.toBe('none');
});

test('htmx: a swapped-in toggle and one restored from history show the current choice', async ({page}) => {
  await page.emulateMedia({colorScheme: 'light'});
  const routes: Routes = {
    '/toggle': () => ({body: MENU}),
    '/other': () => ({body: `<p>Other page</p>${BUTTON}`}),
  };
  const main = `<div hx-boost="true"><a href="/other">Other</a></div>`
      + `<div id="slot"><button type="button" hx-get="/toggle" hx-target="#slot">Load</button></div>`;
  await openFixture(page, main, {theme: true, htmx: true, routes});

  // A swap brings a toggle the server rendered without a state; it shows the stored one.
  await page.getByRole('button', {name: 'Load'}).click();
  await expect(page.locator('#theme-toggle-trigger')).toBeVisible();
  expect(await checked(page)).toEqual(['system']);
  await choose(page, 'dark');
  expect(await checked(page)).toEqual(['dark']);

  // History: the page htmx saved said Dark; choosing Light elsewhere and coming back shows Light.
  await page.getByRole('link', {name: 'Other'}).click();
  await expect(page.getByText('Other page')).toBeVisible();
  await page.locator('.theme-toggle-button').click();
  expect(await stored(page)).toBe('light');
  await page.goBack();
  await expect(page.locator('#theme-toggle-trigger')).toBeVisible();
  await expect.poll(() => checked(page)).toEqual(['light']);
  expect(await isDark(page)).toBe(false);
  expect(await icon(page)).toEqual(['sun']);
});
