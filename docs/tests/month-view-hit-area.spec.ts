import {expect, test, type Page} from '@playwright/test';
import {openShowcase, skins} from './showcase';

// sl:month-view's nav-size: the previous and next links take an icon button's size, and on a coarse pointer all of them
// are at least 44 by 44 pixels to hit, whatever they look like.

const scenario = '[data-scenario="month-view--nav-size"]';
const defaultScenario = '[data-scenario="month-view--default"]';
const size = (page: Page, selector: string) => page.locator(selector).evaluate(element => {
  const box = element.getBoundingClientRect();
  return {width: box.width, height: box.height};
});

for (const skin of skins) {
  test.describe(skin, () => {
    test('nav-size sets the size of the links to an icon button\'s', async ({page}) => {
      await openShowcase(page, skin, 'light');
      const link = await size(page, `${scenario} .month-view-previous`);
      const button = await size(page, '[data-scenario="button--icon-sizes"] .button[data-size="icon-lg"]');
      expect(link).toEqual(button);

      // Without it, the skin's calendar size, smaller than icon-lg.
      const plain = await size(page, `${defaultScenario} .month-view-previous`);
      expect(plain.width).toBeGreaterThan(0);
      expect(plain.width).toBeLessThan(link.width);
    });

    test.describe('on a touch screen', () => {
      test.use({hasTouch: true});

      test('the links are at least 44 by 44 pixels to hit', async ({page}) => {
        await openShowcase(page, skin, 'light');
        for (const root of [scenario, defaultScenario]) {
          for (const link of ['.month-view-previous', '.month-view-next']) {
            const area = await page.locator(`${root} ${link}`).evaluate(element => {
              const after = getComputedStyle(element, '::after');
              return {width: parseFloat(after.width), height: parseFloat(after.height)};
            });
            expect(area.width, `${root} ${link}`).toBeGreaterThanOrEqual(44);
            expect(area.height, `${root} ${link}`).toBeGreaterThanOrEqual(44);
          }
        }
      });

      test('a tap just outside the visible link follows it', async ({page}) => {
        await openShowcase(page, skin, 'light');
        const link = page.locator(`${defaultScenario} .month-view-next`);
        await link.scrollIntoViewIfNeeded();
        const box = (await link.boundingBox())!;
        // Beside the link, 4 pixels from its edge.
        await Promise.all([
          page.waitForURL(/month=/),
          page.touchscreen.tap(box.x + box.width + 4, box.y + box.height / 2),
        ]);
      });
    });
  });
}
