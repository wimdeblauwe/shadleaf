import {expect, test, type Locator, type Page} from '@playwright/test';
import {combinations, openShowcase} from './showcase';

// sl:empty: no behaviour of its own, so this checks what the skins draw. On the showcase, in every skin and theme: the
// border prop draws a dashed line (and only then a line), the icon media is a filled square around its icon, the
// content is centred, and an empty state in sl:table-empty fills the row's one cell.

const scenario = (page: Page, id: string) => page.locator(`[data-scenario="${id}"]`);
const style = (element: Locator) => element.evaluate(element => {
  const style = getComputedStyle(element);
  return {borderStyle: style.borderTopStyle, borderWidth: parseFloat(style.borderTopWidth),
    background: style.backgroundColor};
});

for (const {skin, theme} of combinations) {
  test.describe(`${skin}, ${theme}`, () => {
    test.beforeEach(async ({page}) => openShowcase(page, skin, theme));

    test('border draws a dashed line, and only with it', async ({page}) => {
      const bordered = await style(scenario(page, 'empty--border').locator('.empty'));
      expect(bordered.borderStyle).toBe('dashed');
      expect(bordered.borderWidth).toBeGreaterThan(0);

      expect((await style(scenario(page, 'empty--default').locator('.empty'))).borderWidth).toBe(0);
    });

    test('the icon media is a filled square around its icon', async ({page}) => {
      const media = scenario(page, 'empty--default').locator('.empty-media');
      const box = (await media.boundingBox())!;
      expect(box.width).toBeGreaterThan(0);
      expect(box.width).toBeCloseTo(box.height, 0);
      expect((await style(media)).background).not.toBe('rgba(0, 0, 0, 0)');

      const icon = (await media.locator('svg').boundingBox())!;
      expect(icon.width).toBeLessThan(box.width);
      expect(icon.x - box.x).toBeCloseTo(box.x + box.width - icon.x - icon.width, 0);

      // The default media draws nothing around an avatar.
      expect((await style(scenario(page, 'empty--avatar').locator('.empty-media'))).background)
          .toBe('rgba(0, 0, 0, 0)');
    });

    test('the header is centred in the empty state', async ({page}) => {
      const empty = (await scenario(page, 'empty--border').locator('.empty').boundingBox())!;
      const title = (await scenario(page, 'empty--border').locator('.empty-title').boundingBox())!;
      expect(title.x - empty.x).toBeCloseTo(empty.x + empty.width - title.x - title.width, 0);
    });

    test('in sl:table-empty it fills the row', async ({page}) => {
      const table = scenario(page, 'empty--table');
      const cell = (await table.locator('.table-empty > td').boundingBox())!;
      const header = (await table.locator('thead tr').boundingBox())!;
      expect(cell.width).toBeCloseTo(header.width, 0);
      await expect(table.getByRole('heading', {name: 'No people yet', level: 3})).toBeVisible();
    });
  });
}
