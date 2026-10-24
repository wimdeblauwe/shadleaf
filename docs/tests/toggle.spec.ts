import {expect, test, type Locator, type Page} from '@playwright/test';
import previews from '../src/generated/previews.json' with {type: 'json'};
import {combinations, openShowcase, skins} from './showcase';
import {openFixture} from './fixture';

// sl:toggle and sl:toggle-group: a checkbox (or a group item's radio button) covering a label, which draws its state
// through :has(): a click or Space anywhere on it checks it, and the label turns pressed and draws the focus ring. A
// single group's arrow keys select the next item; a joined group (spacing="0") rounds only its ends. On the showcase,
// in every skin and theme; in fixture pages, a form submits the toggles without any script, a tooltip on a toggle
// shows on keyboard focus and describes its input, and in forced-colors mode a pressed toggle takes the highlight
// colours.

type Scenario = { id: string, html: string };
const scenarios = previews.scenarios as Scenario[];
const html = (id: string) => scenarios.find(scenario => scenario.id === id)!.html;

const scenario = (page: Page, id: string) => page.locator(`[data-scenario="${id}"]`);
const background = (label: Locator) => label.evaluate(element => getComputedStyle(element).backgroundColor);
const ring = (label: Locator) => label.evaluate(element => getComputedStyle(element).boxShadow);

// --- the showcase ------------------------------------------------------------------------------------------------------

for (const {skin, theme} of combinations) {
  test.describe(`${skin}, ${theme}`, () => {
    test.beforeEach(async ({page}) => openShowcase(page, skin, theme));

    test('a click anywhere on a toggle checks it, and it looks pressed', async ({page}) => {
      const toggle = scenario(page, 'toggle--text').locator('.toggle');
      const checkbox = toggle.getByRole('checkbox', {name: 'Italic'});
      await page.mouse.move(0, 0);
      const atRest = await background(toggle);

      // Near the start edge, not on the icon or the text; vertically centred, as a corner is outside a pill's shape.
      const box = (await toggle.boundingBox())!;
      await toggle.click({position: {x: 6, y: box.height / 2}});
      await expect(checkbox).toBeChecked();
      await page.mouse.move(0, 0);
      expect(await background(toggle)).not.toEqual(atRest);

      await toggle.click();
      await expect(checkbox).not.toBeChecked();
    });

    test('Space toggles the focused toggle, which draws the focus ring', async ({page}) => {
      const toggle = scenario(page, 'toggle--outline').locator('.toggle').first();
      const checkbox = toggle.getByRole('checkbox', {name: 'Italic'});
      const atRest = await ring(toggle);
      await checkbox.focus();
      await page.keyboard.press('Shift+Tab');
      await page.keyboard.press('Tab');
      await expect(checkbox).toBeFocused();
      expect(await ring(toggle)).not.toEqual(atRest);

      await page.keyboard.press('Space');
      await expect(checkbox).toBeChecked();
    });

    test('the arrow keys select the next item of a single group', async ({page}) => {
      const group = scenario(page, 'toggle-group--vertical').getByRole('radiogroup', {name: 'Density'});
      await group.getByRole('radio', {name: 'Comfortable'}).focus();
      await page.keyboard.press('ArrowDown');
      await expect(group.getByRole('radio', {name: 'Spacious'})).toBeChecked();
      await expect(group.getByRole('radio', {name: 'Spacious'})).toBeFocused();
      await expect(group.getByRole('radio', {name: 'Comfortable'})).not.toBeChecked();
    });

    test('a multiple group checks its items independently', async ({page}) => {
      const group = scenario(page, 'toggle-group--default').getByRole('group', {name: 'Text style'});
      await group.locator('.toggle-group-item').nth(0).click();
      await group.locator('.toggle-group-item').nth(2).click();
      await expect(group.getByRole('checkbox', {checked: true})).toHaveCount(2);
    });

    test('a joined group puts its items edge to edge and rounds only its ends', async ({page}) => {
      const items = scenario(page, 'toggle-group--spacing').locator('.toggle-group').first().locator('.toggle-group-item');
      const boxes = await items.evaluateAll(elements => elements.map(element => {
        const box = element.getBoundingClientRect();
        const style = getComputedStyle(element);
        return {start: box.left, end: box.right, startRadius: style.borderStartStartRadius,
          endRadius: style.borderStartEndRadius};
      }));
      expect(boxes[1].start).toBeCloseTo(boxes[0].end, 0);
      expect(boxes[2].start).toBeCloseTo(boxes[1].end, 0);
      expect(boxes[1].startRadius).toBe('0px');
      expect(boxes[1].endRadius).toBe('0px');
      expect(boxes[0].endRadius).toBe('0px');
      expect(boxes[2].startRadius).toBe('0px');
    });

    test('an invalid toggle is coloured; a disabled one fades and ignores clicks', async ({page}) => {
      const invalid = scenario(page, 'toggle--invalid').locator('.toggle');
      const valid = scenario(page, 'toggle--form').locator('.toggle');
      await expect(invalid.locator('.toggle-input')).toHaveAttribute('aria-invalid', 'true');
      expect(await invalid.evaluate(element => getComputedStyle(element).borderColor))
          .not.toEqual(await valid.evaluate(element => getComputedStyle(element).borderColor));

      const disabled = scenario(page, 'toggle--disabled').locator('.toggle').first();
      await expect(disabled).toHaveCSS('opacity', '0.5');
      await disabled.click({force: true});
      await expect(disabled.locator('.toggle-input')).not.toBeChecked();
    });
  });
}

// --- fixture pages -----------------------------------------------------------------------------------------------------

test('without any script, a form submits the toggle and the group items', async ({page}) => {
  let submitted = '';
  await openFixture(page, `<form method="post" action="/save">${html('toggle--text')}${html('toggle-group--outline')}
      ${html('toggle-group--default')}<button>Save</button></form>`, {
    alpine: false,
    routes: {
      '/save': request => {
        submitted = request.body;
        return {body: '<p>Saved</p>'};
      },
    },
  });
  await page.locator('.toggle', {hasText: 'Italic'}).click();
  await page.locator('.toggle-group-item', {hasText: 'Missed'}).click();
  await page.getByRole('checkbox', {name: 'Bold'}).click();
  await page.getByRole('checkbox', {name: 'Underline'}).click();
  await page.getByRole('button', {name: 'Save'}).click();
  await expect(page.getByText('Saved')).toBeVisible();

  const body = new URLSearchParams(submitted);
  expect(body.get('italicLabel')).toBe('on');
  expect(body.get('callFilter')).toBe('missed');
  expect(body.getAll('textStyle')).toEqual(['bold', 'underline']);
});

test('a tooltip on a group item shows on keyboard focus and on hover, and the group still joins its items', async ({page}) => {
  const messages = await openFixture(page, `<p><button type="button" id="before">Before</button></p>
      ${html('toggle-group--tooltip')}`);
  const items = page.locator('.toggle-group-item');
  const contents = page.locator('.tooltip-content');
  const isOpen = (content: Locator) => content.evaluate(element => element.matches(':popover-open'));

  await page.locator('#before').focus();
  await page.keyboard.press('Tab');
  await expect(page.getByRole('radio', {name: 'Align left'})).toBeFocused();
  await expect.poll(() => isOpen(contents.nth(0))).toBe(true);
  // mode="label": the tooltip repeats the item's aria-label, so it describes nothing.
  await expect(items.nth(0).locator('input')).not.toHaveAttribute('aria-describedby');
  await page.keyboard.press('ArrowRight');
  await expect(page.getByRole('radio', {name: 'Align center'})).toBeFocused();
  await expect.poll(() => isOpen(contents.nth(1))).toBe(true);
  expect(await isOpen(contents.nth(0))).toBe(false);

  await page.locator('#before').focus();
  await expect.poll(() => isOpen(contents.nth(1))).toBe(false);
  await items.nth(2).hover();
  await expect.poll(() => isOpen(contents.nth(2))).toBe(true);

  const radii = await items.evaluateAll(elements => elements.map(element => {
    const style = getComputedStyle(element);
    return [style.borderStartStartRadius, style.borderStartEndRadius];
  }));
  expect(radii[0][0]).not.toBe('0px');
  expect(radii[1]).toEqual(['0px', '0px']);
  expect(radii[2][1]).not.toBe('0px');
  expect(messages).toEqual([]);
});

test('a tooltip on a toggle describes its checkbox, not the label around it', async ({page}) => {
  const messages = await openFixture(page, `<p><button type="button" id="before">Before</button></p>
      <span class="tooltip" x-data="slTooltip"><label class="toggle"><input type="checkbox" class="toggle-input"
      aria-label="Bold"></label><span class="tooltip-content" role="tooltip" popover="manual">Make the text
      bold<span class="tooltip-arrow" aria-hidden="true"></span></span></span>`);
  const checkbox = page.getByRole('checkbox', {name: 'Bold'});

  await expect(checkbox).toHaveAccessibleDescription('Make the text bold');
  await expect(page.locator('label.toggle')).not.toHaveAttribute('aria-describedby');
  await page.locator('#before').focus();
  await page.keyboard.press('Tab');
  await expect(checkbox).toBeFocused();
  await expect.poll(() => page.locator('.tooltip-content').evaluate(element => element.matches(':popover-open')))
      .toBe(true);
  expect(messages).toEqual([]);
});

for (const skin of skins) {
  test(`a pressed toggle takes the highlight colours in forced-colors mode: ${skin}`, async ({page}) => {
    await page.emulateMedia({forcedColors: 'active'});
    await openShowcase(page, skin, 'light');
    const colours = await page.evaluate(() => {
      const probe = document.createElement('span');
      probe.style.backgroundColor = 'Highlight';
      probe.style.forcedColorAdjust = 'none';
      document.body.append(probe);
      const highlight = getComputedStyle(probe).backgroundColor;
      probe.remove();
      const pressed = document.querySelector('[data-scenario="toggle-group--outline"] .toggle:has(> :checked)')!;
      const released = document.querySelector('[data-scenario="toggle-group--outline"] .toggle:has(> :not(:checked))')!;
      return {highlight, pressed: getComputedStyle(pressed).backgroundColor,
        released: getComputedStyle(released).backgroundColor};
    });
    expect(colours.pressed).toBe(colours.highlight);
    expect(colours.released).not.toBe(colours.highlight);
  });
}
