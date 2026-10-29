import {expect, test, type Page} from '@playwright/test';
import {openShowcase, skins} from './showcase';

// size="sm" makes a table denser in every skin: lower header and body rows, text no larger than the default's, and
// extra-small sort links.

const measure = (page: Page, scenario: string) => page.locator(`[data-scenario="table--${scenario}"] .table`)
    .evaluate(table => ({
      head: table.querySelector('thead > tr')!.getBoundingClientRect().height,
      row: table.querySelector('tbody > tr')!.getBoundingClientRect().height,
      fontSize: parseFloat(getComputedStyle(table.querySelector('tbody > tr > td')!).fontSize),
    }));

for (const skin of skins) {
  test(`a compact table has lower rows and no larger text: ${skin}`, async ({page}) => {
    await openShowcase(page, skin, 'light');
    const normal = await measure(page, 'sort');
    const compact = await measure(page, 'compact');

    expect(compact.head).toBeLessThan(normal.head);
    expect(compact.row).toBeLessThan(normal.row);
    expect(compact.fontSize).toBeLessThanOrEqual(normal.fontSize);
    await expect(page.locator('[data-scenario="table--compact"] .table-sort').first())
        .toHaveAttribute('data-size', 'xs');
  });
}
