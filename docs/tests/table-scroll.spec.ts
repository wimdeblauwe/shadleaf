import {expect, test, type Page} from '@playwright/test';
import {openFixture} from './fixture';

// sl:table's container scrolls sideways when the table is wider than the page. A scrollable region must be reachable
// with the keyboard (WCAG 2.1.1; axe's scrollable-region-focusable). Checked on 1 October 2026: Chromium and Firefox
// make an overflowing container a tab stop of their own when nothing in it can take the focus (Firefox even when
// something can), and the arrow keys then scroll it; when a link in it takes the focus, the container scrolls it into
// view. So the container has no tabindex: a fixed one would add a tab stop to every table that does not overflow.
// Playwright's WebKit (26.6) reaches neither the container nor, without full keyboard access, the links, and scrolls
// no element with the arrow keys even with a tabindex; with Option+Tab, a focused link scrolls into view there too.
// Runs in Chromium and Firefox.

const cells = (count: number) => Array.from({length: count}, (_, index) => `<td>A long cell, number ${index}</td>`)
    .join('');
const head = (links: boolean) => Array.from({length: 8}, (_, index) => links
    ? `<th class="table-head"><a class="btn table-sort" data-variant="ghost" data-size="sm" id="sort-${index}"
        href="#sort-${index}">Column ${index}</a></th>`
    : `<th class="table-head">Column ${index}</th>`).join('');
const table = (links: boolean, columns = 8) => `<button id="before">Before</button>
  <div class="table-container"><table class="table"><thead><tr>${head(links)}</tr></thead>
    <tbody><tr>${cells(columns)}</tr><tr>${cells(columns)}</tr></tbody></table></div>
  <button id="after">After</button>`;

const focused = (page: Page) => page.evaluate(() => document.activeElement?.id || document.activeElement?.className);
const scrollLeft = (page: Page) => page.locator('.table-container').evaluate(container => container.scrollLeft);

test.use({viewport: {width: 420, height: 600}});

test('an overflowing table without links is a tab stop, and the arrow keys scroll it', async ({page}) => {
  await openFixture(page, table(false), {alpine: false});
  expect(await page.locator('.table-container').evaluate(container => container.scrollWidth > container.clientWidth))
      .toBe(true);

  await page.locator('#before').focus();
  await page.keyboard.press('Tab');
  expect(await focused(page)).toBe('table-container');
  await page.keyboard.press('ArrowRight');
  await page.keyboard.press('ArrowRight');
  await expect.poll(() => scrollLeft(page)).toBeGreaterThan(0);
  await page.keyboard.press('Tab');
  expect(await focused(page)).toBe('after');
});

test('a link in an overflowing table scrolls into view when it takes the focus', async ({page}) => {
  await openFixture(page, table(true), {alpine: false});

  await page.locator('#sort-7').focus();
  await expect.poll(() => scrollLeft(page)).toBeGreaterThan(0);
  const box = await page.locator('#sort-7').boundingBox();
  expect(box!.x + box!.width).toBeLessThanOrEqual(420);
});

test('a table that fits adds no tab stop', async ({page}) => {
  await openFixture(page, table(false, 1).replace(head(false), '<th class="table-head">Name</th>'), {alpine: false});

  await page.locator('#before').focus();
  await page.keyboard.press('Tab');
  expect(await focused(page)).toBe('after');
});
