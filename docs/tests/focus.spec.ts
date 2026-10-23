import {test} from '@playwright/test';
import {expectVisibleFocus} from './focus-pass';
import {combinations, component, keepComponentOnly, openShowcase, showcasePages, widths} from './showcase';

// Keyboard focus must be visible on every control of the showcase, at both widths and on both of its pages (see
// focus-pass.ts). SHADLEAF_TEST_COMPONENT limits it to one component's controls (showcase.ts).

for (const {skin, theme} of combinations) {
  for (const width of widths) {
    for (const showcase of showcasePages) {
      test(`keyboard focus is visible on every control: ${skin}, ${theme}, ${width.name}, ${showcase.name}`, async ({page}) => {
        await page.setViewportSize(width.viewport);
        await openShowcase(page, skin, theme, showcase.path);
        // slTabs takes the inactive tabs out of the tab order: collect the stops once it has.
        await page.waitForFunction(() => 'Alpine' in window);
        await keepComponentOnly(page);

        await expectVisibleFocus(page, '.showcase', {allowNone: !!component});
      });
    }
  }
}
