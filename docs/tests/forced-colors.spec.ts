import {expect, test} from '@playwright/test';
import {openShowcase, skins} from './showcase';

// In forced-colors mode (Windows' high contrast) a disabled control should use the system's GrayText, not half
// opacity: fading turns the colours the user chose into ones they did not. The text-like controls, the selects'
// chevron, the labels and legends that fade with a control, and disabled accordion, collapsible and tab triggers turn
// GrayText; the checkbox, radio button and switch are
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
      // A disabled accordion item's or collapsible's trigger, and a disabled tab.
      for (const trigger of document.querySelectorAll('main :is(details[data-disabled] > summary, '
          + '[role=tab]:is(:disabled, [aria-disabled=true]))')) {
        check(trigger, true);
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

// The accordion and collapsible triggers, the tabs and the tab panels show focus with a ring (a box-shadow), which
// forced colors drop: each needs an outline there. The active tab loses its raised background, so it is underlined.
test('accordion, collapsible and tabs show focus and the active tab in forced-colors mode', async ({page}) => {
  await page.emulateMedia({forcedColors: 'active'});
  await openShowcase(page, skins[0], 'light');
  await page.waitForFunction(() => 'Alpine' in window);

  const selectors = ['main .accordion-trigger:not([tabindex="-1"])', 'main .collapsible-trigger:not([tabindex="-1"])',
    'main .tabs-trigger[aria-selected=true]', 'main .tabs-content:not([hidden])'];
  for (const selector of selectors) {
    const element = page.locator(selector).first();
    await element.focus();
    await page.keyboard.press('Shift+Tab');
    await page.keyboard.press('Tab');
    await expect(element).toBeFocused();
    const outline = await element.evaluate(focused => {
      const style = getComputedStyle(focused);
      return {visible: focused.matches(':focus-visible'), style: style.outlineStyle, width: parseFloat(style.outlineWidth)};
    });
    expect(outline.visible, selector).toBe(true);
    expect(outline.style, selector).not.toBe('none');
    expect(outline.width, selector).toBeGreaterThan(0);
  }

  const decoration = await page.locator('main .tabs-trigger[aria-selected=true]').first()
      .evaluate(tab => getComputedStyle(tab).textDecorationLine);
  expect(decoration).toBe('underline');
});

// The indeterminate checkbox (sl:checkbox indeterminate, sl:table-select-all): the browser draws its own, with its own
// dash, so neither icon is drawn over it.
for (const skin of skins) {
  test(`an indeterminate checkbox is the browser's own in forced-colors mode: ${skin}`, async ({page}) => {
    await page.emulateMedia({forcedColors: 'active'});
    await openShowcase(page, skin, 'light');
    await page.waitForFunction(() => 'Alpine' in window);

    for (const scope of ['[data-scenario="checkbox--indeterminate"]', '[data-scenario="table--selection"] thead']) {
      const wrapper = page.locator(`${scope} .checkbox-wrapper`).first();
      await expect.poll(() => wrapper.locator('input').evaluate(input => input.matches(':indeterminate'))).toBe(true);
      expect(await wrapper.evaluate(root => ({
        appearance: getComputedStyle(root.querySelector('input')!).appearance,
        icons: [...root.querySelectorAll('svg')].map(svg => getComputedStyle(svg).display),
      }))).toEqual({appearance: 'auto', icons: ['none', 'none']});
    }
  });
}
