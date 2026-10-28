import {expect, test, type Page} from '@playwright/test';
import {openShowcase, skins} from './showcase';

// The icon, icon-xs and icon-sm buttons look as they are but are at least 44 by 44 pixels to hit on a coarse pointer
// (an invisible ::after); a fine pointer keeps the visible box, and icon-lg needs nothing.

const hitArea = (page: Page, selector: string) => page.evaluate(selector => {
  const button = document.querySelector(selector)!;
  const visible = button.getBoundingClientRect();
  const after = getComputedStyle(button, '::after');
  return {
    visible: {width: visible.width, height: visible.height},
    content: after.content,
    width: parseFloat(after.width),
    height: parseFloat(after.height),
    position: after.position,
  };
}, selector);

const sizes = '[data-scenario="button--icon-sizes"]';
const iconSm = `${sizes} .button[data-size="icon-sm"]`;

for (const skin of skins) {
  test.describe(`${skin} on a touch screen`, () => {
    test.use({hasTouch: true});

    test('an icon-sm button is at least 44 by 44 pixels to hit, and looks the same', async ({page}) => {
      await openShowcase(page, skin, 'light');
      expect(await page.evaluate(() => matchMedia('(pointer: coarse)').matches)).toBe(true);
      const area = await hitArea(page, iconSm);

      expect(area.position).toBe('absolute');
      expect(area.width).toBeGreaterThanOrEqual(44);
      expect(area.height).toBeGreaterThanOrEqual(44);
      expect(area.visible.width).toBeLessThan(44);
    });

    test('a tap just outside the visible button presses it', async ({page}) => {
      await openShowcase(page, skin, 'light');
      const button = page.locator(iconSm);
      await button.evaluate(element => {
        element.addEventListener('click', () => element.setAttribute('data-pressed', 'true'));
      });
      await button.scrollIntoViewIfNeeded();
      const box = (await button.boundingBox())!;
      // Above the button, where its neighbours are not: 4 pixels beyond the visible edge, inside the 44.
      await page.touchscreen.tap(box.x + box.width / 2, box.y - 4);
      await expect(button).toHaveAttribute('data-pressed', 'true');
    });

    test('icon-xs and icon reach 44 pixels too, icon-lg is left alone', async ({page}) => {
      await openShowcase(page, skin, 'light');
      for (const size of ['icon-xs', 'icon']) {
        const area = await hitArea(page, `${sizes} .button[data-size="${size}"]`);
        expect(area.height, size).toBeGreaterThanOrEqual(44);
      }
      const large = await hitArea(page, `${sizes} .button[data-size="icon-lg"]`);
      expect(large.content).toBe('none');
    });
  });
}

test.describe('with a mouse', () => {
  test('the hit area is the visible button', async ({page}) => {
    await openShowcase(page, 'vega', 'light');
    expect(await page.evaluate(() => matchMedia('(pointer: coarse)').matches)).toBe(false);
    expect((await hitArea(page, iconSm)).content).toBe('none');

    const button = page.locator(iconSm);
    await button.evaluate(element => {
      element.addEventListener('click', () => element.setAttribute('data-pressed', 'true'));
    });
    await button.scrollIntoViewIfNeeded();
    const box = (await button.boundingBox())!;
    await page.mouse.click(box.x + box.width / 2, box.y - 4);
    await expect(button).not.toHaveAttribute('data-pressed', 'true');
  });
});
