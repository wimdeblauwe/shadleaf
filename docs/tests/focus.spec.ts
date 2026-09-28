import {expect, test} from '@playwright/test';
import {combinations, openShowcase} from './showcase';

// Keyboard focus must be visible on every control. axe does not check this, and "the skin has a :focus-visible rule"
// is not enough: a rule can exist and draw nothing (an outline width with outline-style none). So this tabs through the
// page and compares each control's computed outline and box-shadow with its unfocused state.

type Indicator = { outline: string; boxShadow: string };

for (const {skin, theme} of combinations) {
  test(`keyboard focus is visible on every control: ${skin}, ${theme}`, async ({page}) => {
    await openShowcase(page, skin, theme);

    const tabbable = await page.evaluate(() => {
      const candidates = document.querySelectorAll<HTMLElement>('main a[href], main button, main [tabindex]');
      return [...candidates]
          .filter(element => element.tabIndex >= 0 && !(element as HTMLButtonElement).disabled)
          .map((element, index) => {
            element.dataset.focusIndex = String(index);
            const style = getComputedStyle(element);
            return {
              label: element.outerHTML.slice(0, 120),
              unfocused: {outline: `${style.outlineStyle} ${style.outlineWidth} ${style.outlineColor}`,
                boxShadow: style.boxShadow},
            };
          });
    });
    expect(tabbable.length).toBeGreaterThan(0);

    const invisible: string[] = [];
    for (let i = 0; i < tabbable.length; i++) {
      await page.keyboard.press('Tab');
      const focused = await page.evaluate(() => {
        const element = document.activeElement as HTMLElement;
        const style = getComputedStyle(element);
        return {
          index: element.dataset.focusIndex,
          focusVisible: element.matches(':focus-visible'),
          outlineVisible: style.outlineStyle !== 'none' && parseFloat(style.outlineWidth) > 0,
          indicator: {outline: `${style.outlineStyle} ${style.outlineWidth} ${style.outlineColor}`,
            boxShadow: style.boxShadow} as Indicator,
        };
      });
      expect(focused.index, `Tab ${i + 1} reaches control ${i}`).toBe(String(i));
      expect(focused.focusVisible).toBe(true);

      const before = tabbable[i].unfocused;
      const changed = focused.indicator.outline !== before.outline || focused.indicator.boxShadow !== before.boxShadow;
      const drawn = focused.outlineVisible || focused.indicator.boxShadow !== 'none';
      if (!changed || !drawn) {
        invisible.push(`${tabbable[i].label}\n    unfocused ${JSON.stringify(before)}\n    focused   ${
            JSON.stringify(focused.indicator)}`);
      }
    }
    expect(invisible, `no visible focus indicator on:\n${invisible.join('\n')}`).toEqual([]);
  });
}
