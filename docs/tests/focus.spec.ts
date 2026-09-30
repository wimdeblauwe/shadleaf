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
      const candidates = document.querySelectorAll<HTMLElement>(
          'main :is(a[href], button, input:not([type=hidden]), select, textarea, [tabindex])');
      // :disabled, not the disabled property: a control in a disabled fieldset is disabled without the attribute.
      const disabled = (element: Element) => element.matches(':disabled');
      // Tab stops once per radio group: on its checked radio, or on its first one when none is checked.
      const radioTabStop = (radio: HTMLInputElement) => {
        const group = [...document.querySelectorAll<HTMLInputElement>(`input[type=radio][name="${radio.name}"]`)]
            .filter(other => !disabled(other));
        return radio === (group.find(other => other.checked) ?? group[0]);
      };
      return [...candidates]
          .filter(element => element.tabIndex >= 0 && !disabled(element))
          .filter(element => !(element instanceof HTMLInputElement && element.type === 'radio') || radioTabStop(element))
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
    const readFocused = () => page.evaluate(() => {
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

    for (let i = 0; i < tabbable.length; i++) {
      await page.keyboard.press('Tab');
      let focused = await readFocused();
      // A date or time input has a tab stop per segment (day, month, year): tab on until focus leaves it.
      for (let extra = 0; extra < 5 && i > 0 && focused.index === String(i - 1); extra++) {
        await page.keyboard.press('Tab');
        focused = await readFocused();
      }
      expect(focused.index, `Tab reaches control ${i}`).toBe(String(i));
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
