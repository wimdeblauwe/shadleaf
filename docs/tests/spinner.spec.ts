import {expect, test, type Locator} from '@playwright/test';
import {openShowcase, skins} from './showcase';

// sl:spinner: a status named "Loading" (or its aria-label) that turns, and turns slower under reduced motion. 16 pixels
// on its own, the media's icon size in sl:empty-media. On the showcase, in every skin.

// The computed width: a turning icon's bounding box is wider than the icon.
const width = (spinner: Locator) => spinner.evaluate(element => parseFloat(getComputedStyle(element).width));
const animation = (spinner: Locator) => spinner.evaluate(element => {
  const style = getComputedStyle(element);
  return {name: style.animationName, duration: parseFloat(style.animationDuration)};
});

for (const skin of skins) {
  test.describe(skin, () => {
    test('a spinner is a named status, 16 pixels unless sized', async ({page}) => {
      await openShowcase(page, skin, 'light');
      const spinners = page.locator('[data-scenario="spinner--default"]');
      await expect(spinners.getByRole('status', {name: 'Loading'})).toBeVisible();
      expect(await width(spinners.locator('.spinner'))).toBe(16);
      expect(await width(page.locator('[data-scenario="spinner--sizes"] .spinner').nth(2))).toBe(32);
      await expect(page.locator('[data-scenario="spinner--label"]').getByRole('status', {name: 'Loading invoices'}))
          .toBeVisible();

      // In the empty state's media it takes the size of the media's icons, not its own.
      expect(await width(page.locator('[data-scenario="empty--spinner"] .empty-media .spinner')))
          .toBe(await width(page.locator('[data-scenario="empty--default"] .empty-media svg')));
    });
  });
}

test.describe(() => {
  test.use({contextOptions: {reducedMotion: 'no-preference'}});

  test('the spinner turns, slower under reduced motion', async ({page}) => {
    await openShowcase(page, 'vega', 'light');
    const spinner = page.locator('[data-scenario="spinner--default"] .spinner');
    const normal = await animation(spinner);
    expect(normal.name).not.toBe('none');

    await page.emulateMedia({reducedMotion: 'reduce'});
    const reduced = await animation(spinner);
    expect(reduced.name).toBe(normal.name);
    expect(reduced.duration).toBeGreaterThan(normal.duration);
  });
});
