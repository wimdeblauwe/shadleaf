import {expect, test, type Locator} from '@playwright/test';
import previews from '../src/generated/previews.json' with {type: 'json'};
import {combinations, openShowcase} from './showcase';
import {openFixture} from './fixture';

// sl:input-group: the group draws its control's focus ring, error, disabled and read-only look (:has() in the skins),
// addons sit where align says whatever their place in the markup, and a click on an addon focuses the control
// (js/input-group.js), but not a click on a button in it. On the showcase, in every skin and theme; in fixture pages,
// with the csp build, shadleaf.js without Alpine, and no Shadleaf script.

type Scenario = { id: string, html: string };
const scenarios = previews.scenarios as Scenario[];
const html = (id: string) => scenarios.find(scenario => scenario.id === id)!.html;

const scenario = (page: import('@playwright/test').Page, id: string) =>
    page.locator(`[data-scenario="input-group--${id}"]`);

/** The group's box-shadow, where the skins draw the focus ring and the error ring (none in sera). */
const ring = (group: Locator) => group.evaluate(element => getComputedStyle(element).boxShadow);
const borderColor = (group: Locator) => group.evaluate(element => getComputedStyle(element).borderBottomColor);

// --- the showcase ------------------------------------------------------------------------------------------------------

for (const {skin, theme} of combinations) {
  test.describe(`${skin}, ${theme}`, () => {
    test.beforeEach(async ({page}) => openShowcase(page, skin, theme));

    test('addons sit where align says, after the control in the markup', async ({page}) => {
      const group = scenario(page, 'text').locator('.input-group').first();
      const input = group.locator('.input-group-input');
      const start = await group.locator('.input-group-addon').first().boundingBox();
      const end = await group.locator('.input-group-addon[data-align="inline-end"]').boundingBox();
      const control = await input.boundingBox();
      expect(start!.x + start!.width).toBeLessThanOrEqual(control!.x + 1);
      expect(end!.x).toBeGreaterThanOrEqual(control!.x + control!.width - 1);

      const block = scenario(page, 'block-end').locator('.input-group').first();
      const below = await block.locator('.input-group-addon').boundingBox();
      const above = await block.locator('.input-group-input').boundingBox();
      expect(below!.y).toBeGreaterThanOrEqual(above!.y + above!.height - 1);
    });

    test('the group draws the focus of its control, and a click on an addon focuses it', async ({page}) => {
      const group = scenario(page, 'text').locator('.input-group').first();
      const input = group.locator('.input-group-input');
      const atRest = [await ring(group), await borderColor(group)];

      await group.locator('.input-group-text', {hasText: 'USD'}).click();
      await expect(input).toBeFocused();
      expect([await ring(group), await borderColor(group)]).not.toEqual(atRest);
    });

    test('a click on a button in an addon leaves the focus on the button', async ({page}) => {
      const button = scenario(page, 'button').getByRole('button', {name: 'Copy'});
      await button.click();
      await expect(button).toBeFocused();
    });

    test('an invalid control colours the group; a disabled one fades it', async ({page}) => {
      const invalid = scenario(page, 'invalid').locator('.input-group');
      const valid = scenario(page, 'field').locator('.input-group');
      await expect(invalid.locator('.input-group-input')).toHaveAttribute('aria-invalid', 'true');
      expect(await borderColor(invalid)).not.toEqual(await borderColor(valid));

      const disabled = scenario(page, 'disabled').locator('.input-group');
      await expect(disabled).toHaveCSS('opacity', '0.5');
      await disabled.locator('.input-group-addon').click({force: true});
      await expect(disabled.locator('.input-group-input')).not.toBeFocused();
    });

    test('the field labels and describes the input in the group', async ({page}) => {
      const field = scenario(page, 'invalid');
      const input = field.getByRole('spinbutton', {name: 'Weight'});
      await expect(input).toHaveAccessibleDescription('must be greater than 0');
    });
  });
}

// --- fixture pages -----------------------------------------------------------------------------------------------------

for (const alpine of ['csp', 'external'] as const) {
  test(`a click on an addon focuses the control (${alpine === 'csp' ? 'csp build' : 'shadleaf.js, no Alpine'})`,
      async ({page}) => {
        const messages = await openFixture(page, html('input-group--text') + html('input-group--block-start'),
            {alpine: alpine === 'csp' ? true : 'external'});
        await page.locator('.input-group-text', {hasText: 'kg'}).click();
        await expect(page.getByRole('spinbutton', {name: 'Weight in kilograms'})).toBeFocused();
        // A textarea too, from an addon above it; a label in an addon keeps its own behaviour.
        await page.locator('.input-group-text', {hasText: 'script.js'}).click();
        await expect(page.getByRole('textbox', {name: 'Script'})).toBeFocused();
        await page.getByText('Full name').click();
        await expect(page.getByRole('textbox', {name: 'Full name'})).toBeFocused();
        expect(messages).toEqual([]);
      });
}

test('without a Shadleaf script, a click on an addon does nothing, and the group still looks the same',
    async ({page}) => {
      await openFixture(page, html('input-group--text'), {alpine: false});
      const group = page.locator('.input-group').first();
      await group.locator('.input-group-text', {hasText: 'USD'}).click();
      await expect(group.locator('.input-group-input')).not.toBeFocused();
      await group.locator('.input-group-input').focus();
      await expect(group.locator('.input-group-input')).toBeFocused();
    });
