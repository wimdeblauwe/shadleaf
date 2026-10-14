import {expect, test, type Page} from '@playwright/test';
import AxeBuilder from '@axe-core/playwright';
import previews from '../src/generated/previews.json' with {type: 'json'};
import {openFixture, servePhotos} from './fixture';
import {expectVisibleFocus} from './focus-pass';
import {combinations, widths} from './showcase';

// The layouts of the docs' Layouts section (previews/layouts/*.yaml), each as a page of its own: its rendered markup
// and its application CSS, under a strict Content-Security-Policy with the csp Alpine build. At a desktop and a phone
// width, in every skin and theme: axe and a visible focus on every control. Then what each layout is about: the side
// the sidebar is on, the current page, the trigger on a desktop and the panel on a phone.

const TAGS = ['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa', 'wcag22aa', 'best-practice'];

type Layout = { id: string, title: string, html: string, css: string, layout?: boolean };
const layouts = (previews.scenarios as Layout[]).filter(scenario => scenario.layout);

/**
 * What each layout is expected to be, by scenario id: the side of its sidebar, whether it floats (a margin from the
 * screen's edge), what collapsing does on a desktop (out of view, to icons, or nothing for the navigation in the
 * header, where the trigger is hidden), the current page its YAML renders, and whether it has a breadcrumb.
 */
type Expected = {
  side: 'start' | 'end', floating: boolean, collapse: 'offcanvas' | 'icon' | 'header', current: string,
  breadcrumb: boolean,
};
const DOCS = {floating: false, collapse: 'offcanvas', current: 'Data Fetching', breadcrumb: true} as const;
const expected: Record<string, Expected> = {
  'layout--sub-menus': {...DOCS, side: 'start'},
  'layout--floating-sub-menus': {...DOCS, side: 'start', floating: true},
  'layout--sidebar-end': {...DOCS, side: 'end'},
  'layout--collapses-to-icons': {side: 'start', floating: false, collapse: 'icon', current: 'All people', breadcrumb: true},
  'layout--header': {side: 'start', floating: false, collapse: 'header', current: 'Forms', breadcrumb: false},
};

const [DESKTOP, PHONE] = widths.map(width => width.viewport);

function openLayout(page: Page, layout: Layout, options: { skin?: string, theme?: string, dir?: 'rtl' } = {}) {
  const classes = options.theme === 'dark' ? ' class="dark"' : '';
  return openFixture(page, servePhotos(layout.html), {
    wrap: false,
    skin: options.skin,
    stylesheet: layout.css,
    htmlAttributes: `${classes}${options.dir ? ` dir="${options.dir}"` : ''}`,
  });
}

const sidebar = (page: Page) => page.locator('#sidebar');
const trigger = (page: Page) => page.locator('.sidebar-trigger');
const box = (page: Page) => sidebar(page).evaluate(element => {
  const rect = element.getBoundingClientRect();
  return {left: Math.round(rect.left), right: Math.round(rect.right)};
});
const focusedText = (page: Page) => page.evaluate(() => document.activeElement?.textContent?.trim());

test('every layout has its expectations here', () => {
  expect(layouts.map(layout => layout.id).sort()).toEqual(Object.keys(expected).sort());
});

for (const layout of layouts) {
  const {side, floating, collapse, current, breadcrumb} = expected[layout.id] ?? {...DOCS, side: 'start'};

  test.describe(layout.title, () => {
    for (const width of widths) {
      for (const {skin, theme} of combinations) {
        test(`${width.name}, ${skin}, ${theme}: axe, and a visible focus on every control`, async ({page}) => {
          await page.setViewportSize(width.viewport);
          const messages = await openLayout(page, layout, {skin, theme});

          const results = await new AxeBuilder({page}).withTags(TAGS).analyze();
          expect(results.violations.map(violation => violation.id)).toEqual([]);
          await expectVisibleFocus(page, 'body');
          expect(messages).toEqual([]);
        });
      }
    }

    test('desktop: the navigation marks the current page, and the trigger collapses it', async ({page}) => {
      await page.setViewportSize(DESKTOP);
      await openLayout(page, layout);

      await expect(sidebar(page)).toBeVisible();
      await expect(sidebar(page).locator('[aria-current="page"]')).toHaveText(current);
      // The current page in the navigation, and the breadcrumb's page.
      await expect(page.locator('[aria-current="page"]')).toHaveCount(breadcrumb ? 2 : 1);
      if (collapse === 'header') {
        // A row in the site header, which never collapses: no trigger on a desktop.
        await expect(sidebar(page)).toHaveJSProperty('parentElement.tagName', 'HEADER');
        await expect(trigger(page)).toBeHidden();
        return;
      }
      const {left, right} = await box(page);
      // A floating sidebar keeps a margin from the edge of the screen.
      const margin = floating ? 8 : 0;
      if (side === 'start') {
        expect(left).toBe(margin);
      } else {
        expect(right).toBe(DESKTOP.width - margin);
      }

      await trigger(page).click();
      await expect(page.locator('.sidebar-provider')).toHaveAttribute('data-state', 'collapsed');
      await expect(trigger(page)).toHaveAttribute('aria-expanded', 'false');
      if (collapse === 'icon') {
        // A strip of icons; the labels are only for assistive technology (visually hidden, still the names).
        await expect.poll(async () => (await box(page)).right - (await box(page)).left).toBeLessThan(80);
        expect(await sidebar(page).locator('.sidebar-menu-button-label').first()
            .evaluate(label => label.getBoundingClientRect().width)).toBeLessThanOrEqual(1);
      } else {
        await expect(sidebar(page)).toBeHidden();
      }
    });

    test('phone: the trigger opens the panel from its side on the current page; Escape closes it', async ({page}) => {
      await page.setViewportSize(PHONE);
      await openLayout(page, layout);
      await expect(sidebar(page)).toBeHidden();

      await trigger(page).click();
      await expect(sidebar(page)).toBeVisible();
      await expect.poll(() => focusedText(page)).toBe(current);
      const {left, right} = await box(page);
      if (side === 'start') {
        expect(left).toBe(0);
      } else {
        expect(right).toBe(PHONE.width);
      }

      await page.keyboard.press('Escape');
      await expect(sidebar(page)).toBeHidden();
      await expect(trigger(page)).toBeFocused();
    });

    test('the trigger\'s icon shows the panel on the sidebar\'s side, left to right and right to left', async ({page}) => {
      test.skip(collapse === 'header', 'the header\'s menu button has an icon of its own');
      await page.setViewportSize(DESKTOP);
      const icon = () => page.locator('.sidebar-trigger-icon')
          .evaluate(element => getComputedStyle(element).scale.startsWith('-') ? 'turned' : 'as drawn');
      // The icon draws the panel on the left: turned round when the sidebar is on the right.
      const turned = (rightToLeft: boolean) => (side === 'end') !== rightToLeft;

      await openLayout(page, layout);
      expect(await icon()).toBe(turned(false) ? 'turned' : 'as drawn');
      await openLayout(page, layout, {dir: 'rtl'});
      expect(await icon()).toBe(turned(true) ? 'turned' : 'as drawn');
      const {left, right} = await box(page);
      if (side === 'end') {
        expect(left).toBe(0);
      } else {
        expect(right).toBe(DESKTOP.width - (floating ? 8 : 0));
      }
    });
  });
}

test('on the end: the keyboard reaches the page before the sidebar', async ({page}) => {
  const layout = layouts.find(l => l.id === 'layout--sidebar-end')!;
  await page.setViewportSize(DESKTOP);
  await openLayout(page, layout);

  const order: string[] = [];
  for (let i = 0; i < 4; i++) {
    await page.keyboard.press('Tab');
    order.push(await page.evaluate(() => {
      const element = document.activeElement!;
      return `${element.closest('#sidebar') ? 'sidebar: ' : ''}${element.textContent?.trim() || element.getAttribute('aria-label')}`;
    }));
  }
  expect(order).toEqual(['Skip to main content', 'Build Your Application', 'Toggle sidebar', 'sidebar: Getting Started']);
});
