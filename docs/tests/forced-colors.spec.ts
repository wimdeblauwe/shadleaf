import {expect, test} from '@playwright/test';
import {openShowcase, skins} from './showcase';

// In forced-colors mode (Windows' high contrast) a disabled control should use the system's GrayText, not half
// opacity: fading turns the colours the user chose into ones they did not. The text-like controls, the selects'
// chevron and the labels and legends that fade with a control turn GrayText; the checkbox, radio button and switch are
// drawn by the browser (appearance: auto), which draws its own disabled state, so they only must not be faded.

for (const skin of skins) {
  test(`disabled controls use GrayText in forced-colors mode: ${skin}`, async ({page}) => {
    await page.emulateMedia({forcedColors: 'active'});
    await openShowcase(page, skin, 'light');

    const problems = await page.evaluate(() => {
      const probe = document.createElement('span');
      probe.style.color = 'GrayText';
      document.body.append(probe);
      const grayText = getComputedStyle(probe).color;
      probe.remove();

      // The opacity an element is drawn with: its own times its ancestors'.
      const opacity = (element: Element) => {
        let result = 1;
        for (let current: Element | null = element; current; current = current.parentElement) {
          result *= parseFloat(getComputedStyle(current).opacity);
        }
        return result;
      };
      const describe = (element: Element) => element.outerHTML.slice(0, 120);
      const problems: string[] = [];
      const check = (element: Element, gray: boolean) => {
        if (opacity(element) !== 1) {
          problems.push(`faded (${opacity(element)}): ${describe(element)}`);
        }
        if (gray && getComputedStyle(element).color !== grayText) {
          problems.push(`not GrayText (${getComputedStyle(element).color}): ${describe(element)}`);
        }
      };

      const controls = [...document.querySelectorAll('main :is(input:not([type=hidden]), select, textarea)')]
          .filter(control => control.matches(':disabled'));
      for (const control of controls) {
        check(control, !control.matches('[type=checkbox], [type=radio]'));
      }
      for (const label of document.querySelectorAll<HTMLLabelElement>('main label')) {
        if (label.control?.matches(':disabled')) {
          check(label, true);
        }
      }
      for (const legend of document.querySelectorAll('main fieldset:disabled > legend')) {
        check(legend, true);
      }
      for (const icon of document.querySelectorAll('main :is(.native-select-wrapper, .select-wrapper):has(> select:disabled) > svg')) {
        check(icon, true);
      }
      return {checked: controls.length, problems};
    });

    expect(problems.checked, 'the showcase has disabled controls').toBeGreaterThan(0);
    expect(problems.problems, problems.problems.join('\n')).toEqual([]);
  });
}

// Forced colors drop backgrounds and box-shadows, so the accent background and the ring of a focused menu item are
// gone: the item needs an outline there. (outline-none sets outline-style none, which a width alone does not undo.)
test('a focused menu item draws an outline in forced-colors mode', async ({page}) => {
  await page.emulateMedia({forcedColors: 'active'});
  await openShowcase(page, skins[0], 'light');
  await page.waitForFunction(() => 'Alpine' in window);
  const id = await page.locator('main .dropdown-menu-content').first().evaluate(menu => menu.id);
  await page.locator(`#${id}-trigger`).focus();
  await page.keyboard.press('Enter');
  await expect(page.locator(`#${id} [role^="menuitem"]`).first()).toBeFocused();

  const outline = await page.evaluate(() => {
    const style = getComputedStyle(document.activeElement!);
    return {style: style.outlineStyle, width: parseFloat(style.outlineWidth)};
  });
  expect(outline.style).not.toBe('none');
  expect(outline.width).toBeGreaterThan(0);
});
