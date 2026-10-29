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
    ? `<th class="table-head"><a class="button table-sort" data-variant="ghost" data-size="sm" id="sort-${index}"
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

// sticky-first-column pins the first cell of every row to the inline start with CSS only.
const pinned = (dir: 'ltr' | 'rtl') => `<div dir="${dir}"><div class="table-container"><table class="table" data-sticky="first-column">
  <thead><tr><th id="h">Person</th>${head(false)}</tr></thead>
  <tbody><tr><td id="c">Ada</td>${cells(8)}</tr></tbody>
  <tfoot><tr><td id="f">Total</td>${cells(8)}</tr></tfoot></table></div></div>`;

test('the first column stays in view after scrolling sideways', async ({page}) => {
  await openFixture(page, pinned('ltr'), {alpine: false});
  await page.locator('.table-container').evaluate(container => container.scrollTo({left: 400}));
  await expect.poll(() => scrollLeft(page)).toBeGreaterThan(300);

  for (const id of ['h', 'c', 'f']) {
    const box = await page.locator(`#${id}`).boundingBox();
    expect(box!.x).toBeGreaterThanOrEqual(0);
    expect(box!.x).toBeLessThan(5);
  }
  // The second column has scrolled out from under it.
  expect((await page.locator('#c + td').boundingBox())!.x).toBeLessThan(150);
});

test('the pinned column sticks to the right edge in a right-to-left page', async ({page}) => {
  await openFixture(page, pinned('rtl'), {alpine: false});
  await page.locator('.table-container').evaluate(container => container.scrollTo({left: -400}));
  await expect.poll(() => scrollLeft(page)).toBeLessThan(-300);

  const box = await page.locator('#c').boundingBox();
  expect(box!.x + box!.width).toBeGreaterThan(420 - 5);
  expect(box!.x + box!.width).toBeLessThanOrEqual(420 + 1);
});

test('the edge shows only while the table is scrolled, and the pinned cell is opaque', async ({page}) => {
  await openFixture(page, pinned('ltr'), {alpine: false});
  const edge = () => page.locator('#c').evaluate(cell => getComputedStyle(cell, '::after').opacity);
  const supported = await page.evaluate(() => CSS.supports('animation-timeline', 'scroll()'));
  if (supported) {
    expect(await edge()).toBe('0');
    await page.locator('.table-container').evaluate(container => container.scrollTo({left: 400}));
    await expect.poll(edge).toBe('1');
  }
  expect(await page.locator('#c').evaluate(cell => getComputedStyle(cell).backgroundColor))
      .not.toBe('rgba(0, 0, 0, 0)');
});

// A colgroup per group of columns: a line on the start edge of every group after the first, a collapsed border.
const grouped = (dir: 'ltr' | 'rtl', sticky: boolean) => `<div dir="${dir}"><div class="table-container">
  <table class="table"${sticky ? ' data-sticky="first-column"' : ''}>
  <colgroup id="g0"></colgroup><colgroup id="g1" span="4"></colgroup><colgroup id="g2" span="4"></colgroup>
  <thead><tr><th id="h">Person</th>${head(false)}</tr></thead>
  <tbody><tr><td id="c">Ada</td>${cells(8)}</tr></tbody></table></div></div>`;

const borders = (page: Page, id: string) => page.locator(`#${id}`).evaluate(group => {
  const style = getComputedStyle(group);
  return {left: style.borderLeftWidth, right: style.borderRightWidth, style: style.borderLeftStyle};
});

test('a line starts every column group after the first', async ({page}) => {
  await openFixture(page, grouped('ltr', false), {alpine: false});
  expect(await page.locator('.table').evaluate(table => getComputedStyle(table).borderCollapse)).toBe('collapse');

  expect((await borders(page, 'g0')).left).toBe('0px');
  for (const id of ['g1', 'g2']) {
    expect(await borders(page, id)).toEqual({left: '1px', right: '0px', style: 'solid'});
  }
});

test('the group line is on the right in a right-to-left page', async ({page}) => {
  await openFixture(page, grouped('rtl', false), {alpine: false});

  expect(await borders(page, 'g1')).toMatchObject({left: '0px', right: '1px'});
});

test('the pinned edge lies on the line of a group that starts at the second column', async ({page}) => {
  await openFixture(page, grouped('ltr', true), {alpine: false});
  const edge = () => page.locator('#c').evaluate(cell => getComputedStyle(cell, '::after').right);
  expect(await edge()).toBe('-1px');

  await openFixture(page, pinned('ltr'), {alpine: false});
  expect(await edge()).toBe('0px');
});
