import {expect, test, type Page} from '@playwright/test';
import AxeBuilder from '@axe-core/playwright';
import previews from '../src/generated/previews.json' with {type: 'json'};
import {openFixture} from './fixture';
import {openShowcase, skins} from './showcase';

// The breadcrumb: the ellipsis's menu with the keyboard, the separators on a right-to-left page, wrapping on a phone,
// forced colours and shadcn's sizes per skin. Fixture pages under a strict Content-Security-Policy with the csp Alpine
// build; the markup is the docs previews'. axe, hover contrast and the focus pass run on the showcase (a11y.spec.ts,
// focus.spec.ts).

const TAGS = ['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa', 'wcag22aa', 'best-practice'];

type Scenario = { id: string, html: string };
const preview = (id: string) => (previews.scenarios as Scenario[]).find(scenario => scenario.id === `breadcrumb--${id}`)!.html;

const trigger = (page: Page) => page.locator('.breadcrumb-ellipsis-trigger');
const menu = (page: Page) => page.locator('#breadcrumb-more');
const isOpen = (page: Page) => menu(page).evaluate(element => element.matches(':popover-open'));
const focused = (page: Page) => page.evaluate(() =>
    document.activeElement?.textContent?.trim() || document.activeElement?.getAttribute('aria-label'));

test.describe('ellipsis menu', () => {
  test('the hidden levels open from the ellipsis, and the keyboard moves through them', async ({page}) => {
    const messages = await openFixture(page, `<h1>Breadcrumb</h1>${preview('menu')}`);

    // Home, then the ellipsis's button, named for what it does.
    await page.keyboard.press('Tab');
    await page.keyboard.press('Tab');
    expect(await focused(page)).toBe('Show more levels');
    await expect(trigger(page)).toHaveAttribute('aria-expanded', 'false');
    await expect(page.getByRole('button', {name: 'Show more levels'})).toHaveAttribute('aria-haspopup', 'menu');

    await page.keyboard.press('Enter');
    await expect.poll(() => isOpen(page)).toBe(true);
    await expect(trigger(page)).toHaveAttribute('aria-expanded', 'true');
    await expect.poll(() => focused(page)).toBe('Documentation');
    await page.keyboard.press('ArrowDown');
    expect(await focused(page)).toBe('Themes');
    await page.keyboard.press('End');
    expect(await focused(page)).toBe('GitHub');
    // The menu is named by its button, and opens below it, lined up with its start.
    await expect(page.getByRole('menu', {name: 'Show more levels'})).toBeVisible();
    const [button, list] = [(await trigger(page).boundingBox())!, (await menu(page).boundingBox())!];
    expect(list.y).toBeGreaterThan(button.y + button.height - 1);
    expect(Math.abs(list.x - button.x)).toBeLessThan(1);
    const results = await new AxeBuilder({page}).withTags(TAGS).analyze();
    expect(results.violations.map(violation => violation.id)).toEqual([]);

    await page.keyboard.press('Escape');
    await expect.poll(() => isOpen(page)).toBe(false);
    expect(await focused(page)).toBe('Show more levels');
    await expect(trigger(page)).toHaveAttribute('aria-expanded', 'false');

    // Arrow up opens it on the last level.
    await page.keyboard.press('ArrowUp');
    await expect.poll(() => focused(page)).toBe('GitHub');
    await page.keyboard.press('Escape');
    expect(messages).toEqual([]);
  });

  test('choosing a level follows its link', async ({page}) => {
    await openFixture(page, preview('menu'), {routes: {'/themes': () => ({body: '<h1>Themes</h1>'})}});

    // A pointer opens it with the focus on the menu: the first arrow down reaches Documentation, the second Themes.
    await trigger(page).click();
    await expect.poll(() => isOpen(page)).toBe(true);
    await expect.poll(() => menu(page).evaluate(element => element === document.activeElement)).toBe(true);
    await page.keyboard.press('ArrowDown');
    await page.keyboard.press('ArrowDown');
    expect(await focused(page)).toBe('Themes');
    await page.keyboard.press('Enter');
    await expect(page.locator('h1')).toHaveText('Themes');
  });

  test('a plain ellipsis is no control, and is read as "More levels"', async ({page, browserName}) => {
    await openFixture(page, preview('ellipsis'));

    await expect(page.locator('.breadcrumb-ellipsis')).toHaveText('More levels');
    await page.keyboard.press('Tab');
    await page.keyboard.press('Tab');
    expect(await focused(page)).toBe('Components');
    // The list holds the four levels: the separators are hidden from assistive technology.
    await expect(page.getByRole('navigation', {name: 'Breadcrumb with collapsed levels'}).getByRole('listitem'))
        .toHaveCount(4);
    if (browserName === 'chromium') {
      const snapshot = await page.locator('.breadcrumb').ariaSnapshot();
      expect(snapshot).toContain('More levels');
    }
  });
});

test.describe('direction', () => {
  // The x of each level's centre and the separators' scale (Tailwind's -scale-x-100 sets scale, not transform).
  const layout = (page: Page) => page.evaluate(() => ({
    levels: [...document.querySelectorAll('.breadcrumb-item')].map(item => {
      const rect = item.getBoundingClientRect();
      return Math.round(rect.left + rect.width / 2);
    }),
    chevrons: [...document.querySelectorAll('.breadcrumb-separator > svg')].map(svg => getComputedStyle(svg).scale),
  }));

  test('left to right: the chevrons point right, to the next level', async ({page}) => {
    await openFixture(page, preview('default'));
    const {levels, chevrons} = await layout(page);

    expect(levels).toEqual([...levels].sort((a, b) => a - b));
    expect(chevrons).toEqual(['none', 'none']);
  });

  test('right to left: the levels run leftwards, and the chevrons point left', async ({page}) => {
    await openFixture(page, preview('default'), {htmlAttributes: 'dir="rtl"'});
    const {levels, chevrons} = await layout(page);

    expect(levels).toEqual([...levels].sort((a, b) => b - a));
    expect(chevrons).toEqual(['-1 1', '-1 1']);
  });

  test('right to left: a separator of the application\'s own is not turned around', async ({page}) => {
    await openFixture(page, preview('separator'), {htmlAttributes: 'dir="rtl"'});

    expect((await layout(page)).chevrons).toEqual(['none', 'none']);
  });
});

test('on a phone a long trail wraps instead of overflowing', async ({page}) => {
  await page.setViewportSize({width: 320, height: 640});
  await openFixture(page, preview('menu').replace(/>Components</, '>Components and their many variants<'));

  const list = page.locator('.breadcrumb-list');
  const {height, overflow} = await list.evaluate(element =>
      ({height: element.getBoundingClientRect().height, overflow: element.scrollWidth - element.clientWidth}));
  const line = await trigger(page).evaluate(element => element.getBoundingClientRect().height);
  expect(height).toBeGreaterThan(line * 1.5);
  expect(overflow).toBe(0);
  expect(await page.evaluate(() => document.documentElement.scrollWidth)).toBe(320);
});

test('forced colours: a focused link and the skip link get an outline', async ({page, browserName}) => {
  test.skip(browserName !== 'chromium', 'forced colours emulation is Chromium only');
  await page.emulateMedia({forcedColors: 'active'});
  await openFixture(page, `<a class="skip-link" href="#main">Skip to main content</a>${preview('default')}`);

  const outline = () => page.evaluate(() => {
    const style = getComputedStyle(document.activeElement!);
    return `${style.outlineStyle} ${style.outlineWidth}`;
  });
  await page.keyboard.press('Tab');
  expect(await focused(page)).toBe('Skip to main content');
  expect(await outline()).toBe('solid 2px');
  await page.keyboard.press('Tab');
  expect(await focused(page)).toBe('Home');
  expect(await outline()).toBe('solid 2px');
});

// shadcn's sizes: vega text-sm with gap-1.5 (gap-2.5 from sm), lyra text-xs with gap-1.5; both a 14 px chevron and the
// ellipsis a 20 px box around a 16 px icon. On the showcase, where both skins are.
for (const skin of skins) {
  test(`sizes follow shadcn: ${skin}`, async ({page}) => {
    await openShowcase(page, skin, 'light');
    const sizes = await page.evaluate(() => {
      const list = document.querySelector('[data-scenario="breadcrumb--ellipsis"] .breadcrumb-list')!;
      const px = (element: Element, property: string) => getComputedStyle(element).getPropertyValue(property);
      return {
        font: px(list, 'font-size'),
        gap: px(list, 'column-gap'),
        chevron: px(list.querySelector('.breadcrumb-separator > svg')!, 'width'),
        ellipsis: px(list.querySelector('.breadcrumb-ellipsis')!, 'width'),
        dots: px(list.querySelector('.breadcrumb-ellipsis > svg')!, 'width'),
      };
    });
    expect(sizes).toEqual(skin === 'lyra'
        ? {font: '12px', gap: '6px', chevron: '14px', ellipsis: '20px', dots: '16px'}
        : {font: '14px', gap: '10px', chevron: '14px', ellipsis: '20px', dots: '16px'});
  });
}
