import {expect, test, type Page} from '@playwright/test';
import AxeBuilder from '@axe-core/playwright';
import previews from '../src/generated/previews.json' with {type: 'json'};
import {combinations, openShowcase} from './showcase';
import {openFixture, type Request} from './fixture';

// Row selection: sl:table-select-all (slTableSelection), sl:table-selection-count (slTableSelectionCount), the
// application's row checkboxes and the indeterminate checkbox. On the showcase, in every skin and theme: the keyboard
// (Space on each checkbox), the indeterminate start, the count, axe with rows selected. In fixture pages under a strict
// Content-Security-Policy with the csp Alpine build: select-all, indeterminate, the count and its announcement, the
// events the rows fire, a form reset, a name prop, disabled and no rows; without Alpine (and without JavaScript) the
// select-all is hidden and the rows still submit; htmx swaps of the rows and of the whole table, and a history restore.

const TAGS = ['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa', 'wcag22aa', 'best-practice'];

type Scenario = { id: string, html: string };
const scenarios = previews.scenarios as Scenario[];
const SELECTION = scenarios.find(scenario => scenario.id === 'table--selection')!.html;
/** The markup sl:table-select-all renders, and the count, taken from the preview. */
const SELECT_ALL = SELECTION.match(/<span class="checkbox-wrapper table-select-all"[\s\S]*?role="status"><\/span>\s*<\/span>/)![0];
const COUNT = SELECTION.match(/<span class="table-selection-count"[\s\S]*?<\/span>/)![0]
    .replace('data-table="people-selection"', 'data-table="people"');
/** A row's sl:checkbox, with {attributes} where name, value, aria-label (and checked) go. */
const ROW_CHECKBOX = SELECTION.match(/<span class="checkbox-wrapper">[\s\S]*?<\/span>/)![0]
    .replace(/name="ids" value="1" aria-label="Select Ada Lovelace" checked/, '{attributes}');

const PEOPLE = ['Ada Lovelace', 'Alan Turing', 'Grace Hopper', 'Edsger Dijkstra', 'Barbara Liskov', 'Ken Thompson'];

type Row = { name: string, id?: number, checked?: boolean, disabled?: boolean, extra?: string };

function rowCheckbox({name, id, checked, disabled}: Row, field = 'ids') {
  return ROW_CHECKBOX.replace('{attributes}', `name="${field}" value="${id}" aria-label="Select ${name}"`
      + `${checked ? ' checked' : ''}${disabled ? ' disabled' : ''}`);
}

function table(rows: Row[], {selectAll = SELECT_ALL, id = 'people'} = {}) {
  return `<div class="table-container"><table class="table" id="${id}">
    <thead><tr><th>${selectAll}</th><th>Name</th><th>Archived</th></tr></thead>
    <tbody>${rows.map(row => `<tr><td>${rowCheckbox(row)}</td><td>${row.name}</td><td>${row.extra ?? ''}</td></tr>`)
      .join('')}</tbody></table></div>`;
}

const rows = (count: number, from = 0): Row[] =>
    PEOPLE.slice(from, from + count).map((name, index) => ({name, id: from + index + 1}));

/** What the select-all and the rows say: 'checked', 'mixed' or 'unchecked'. */
async function state(page: Page, scope = 'body') {
  return page.locator(scope).evaluate(root => {
    const selectAll = root.querySelector<HTMLInputElement>('.table-select-all input')!;
    return {
      all: selectAll.indeterminate ? 'mixed' : selectAll.checked ? 'checked' : 'unchecked',
      disabled: selectAll.disabled,
      rows: [...root.querySelectorAll<HTMLInputElement>('tbody input[type=checkbox]')].map(row => row.checked),
    };
  });
}

const count = (page: Page) => page.locator('.table-selection-count').first().textContent();
const announced = (page: Page) => page.locator('.table-select-all [role=status]').first().textContent();

// --- the showcase ------------------------------------------------------------------------------------------------------

for (const {skin, theme} of combinations) {
  test(`select-all, rows and count with the keyboard, and axe: ${skin}, ${theme}`, async ({page}) => {
    await openShowcase(page, skin, theme);
    await page.waitForFunction(() => 'Alpine' in window);
    const stage = '[data-scenario="table--selection"]';
    const selectAll = page.locator(`${stage} .table-select-all input`);
    const counter = page.locator(`${stage} .table-selection-count`);

    // Ada is selected on the server: select-all starts indeterminate.
    await expect.poll(() => state(page, stage)).toEqual({all: 'mixed', disabled: false, rows: [true, false, false]});
    await expect(counter).toHaveText('1 of 3 selected');
    await expect(selectAll).toHaveAccessibleName('Select all rows on this page');
    await expect(selectAll).toBeVisible();
    expect(await selectAll.evaluate(input => input.matches(':indeterminate'))).toBe(true);

    await selectAll.focus();
    await page.keyboard.press('Space');
    expect(await state(page, stage)).toEqual({all: 'checked', disabled: false, rows: [true, true, true]});
    await expect(counter).toHaveText('3 of 3 selected');
    await expect(page.locator(`${stage} .table-select-all [role=status]`)).toHaveText('3 of 3 selected');

    // Selected rows (and the focused checkbox) keep their contrast.
    const results = await new AxeBuilder({page}).include(stage).withTags(TAGS).analyze();
    expect(results.violations.map(violation => `${violation.id}: ${violation.help}`)).toEqual([]);

    await page.keyboard.press('Space');
    expect(await state(page, stage)).toEqual({all: 'unchecked', disabled: false, rows: [false, false, false]});
    await expect(counter).toHaveText('0 of 3 selected');

    // Tab through the rows, Space on each.
    for (let row = 0; row < 3; row++) {
      await page.keyboard.press('Tab');
      await expect(page.locator(`${stage} tbody tr`).nth(row).locator('input')).toBeFocused();
      await page.keyboard.press('Space');
      await expect(counter).toHaveText(`${row + 1} of 3 selected`);
      expect((await state(page, stage)).all).toBe(row < 2 ? 'mixed' : 'checked');
    }
  });
}

test('the indeterminate checkbox shows a minus until it is clicked', async ({page}) => {
  await openShowcase(page, 'vega', 'light');
  await page.waitForFunction(() => 'Alpine' in window);
  const wrapper = page.locator('[data-scenario="checkbox--indeterminate"] .checkbox-wrapper');
  const shown = () => wrapper.evaluate(root => [...root.querySelectorAll('svg')]
      .filter(svg => getComputedStyle(svg).visibility === 'visible')
      .map(svg => svg.classList.contains('checkbox-indicator') ? 'check' : 'minus'));

  await expect.poll(() => wrapper.locator('input').evaluate(input => (input as HTMLInputElement).indeterminate))
      .toBe(true);
  expect(await shown()).toEqual(['minus']);
  await wrapper.locator('input').click();
  expect(await wrapper.locator('input').evaluate(input => (input as HTMLInputElement).indeterminate)).toBe(false);
  await expect(wrapper.locator('input')).toBeChecked();
  expect(await shown()).toEqual(['check']);
});

// --- fixture pages ---------------------------------------------------------------------------------------------------

test('select-all follows the rows, selects them all and fires their events', async ({page}) => {
  const messages = await openFixture(page, `<form id="bulk">${table(rows(3))}</form>${COUNT}`);
  await page.evaluate(() => {
    (window as any).events = [];
    document.addEventListener('change', event => (window as any).events.push(
        (event.target as HTMLInputElement).value));
    document.addEventListener('sl-table-selection-change',
        event => (window as any).events.push((event as CustomEvent).detail));
  });

  await expect.poll(() => count(page)).toBe('0 of 3 selected');
  expect(await state(page)).toEqual({all: 'unchecked', disabled: false, rows: [false, false, false]});

  await page.locator('tbody tr').nth(1).locator('input').click();
  expect(await state(page)).toEqual({all: 'mixed', disabled: false, rows: [false, true, false]});
  expect(await count(page)).toBe('1 of 3 selected');
  // A row's own click is not announced: its checkbox says it is checked.
  expect(await announced(page)).toBe('');

  // From mixed, a click selects all, as Radix's checkbox does.
  await page.locator('.table-select-all input').click();
  expect(await state(page)).toEqual({all: 'checked', disabled: false, rows: [true, true, true]});
  expect(await count(page)).toBe('3 of 3 selected');
  await expect(page.locator('.table-select-all [role=status]')).toHaveText('3 of 3 selected');
  // The two rows it changed fired change, then the table fired the new count once.
  const events = await page.evaluate(() => (window as any).events);
  expect(events.slice(2)).toEqual(['1', '3', {selected: 3, total: 3}, 'on']);

  await page.locator('.table-select-all input').click();
  expect(await state(page)).toEqual({all: 'unchecked', disabled: false, rows: [false, false, false]});
  await expect(page.locator('.table-select-all [role=status]')).toHaveText('0 of 3 selected');
  expect(messages).toEqual([]);
});

test('a form reset puts the count back; a name prop and disabled rows narrow the rows', async ({page}) => {
  const people: Row[] = [{name: 'Ada Lovelace', id: 1, checked: true, extra: rowCheckbox({name: 'x', id: 9}, 'archived')},
    {name: 'Alan Turing', id: 2}, {name: 'Grace Hopper', id: 3, disabled: true, checked: true}];
  const selectAll = SELECT_ALL.replace('x-data="slTableSelection"', 'x-data="slTableSelection" data-name="ids"');
  await openFixture(page, `<form id="bulk">${table(people, {selectAll})}<button type="reset">Reset</button></form>${COUNT}`);

  // Grace's row is disabled (it would not be submitted), so it is not counted.
  await expect.poll(() => count(page)).toBe('1 of 2 selected');
  // The archived checkbox in the third column is not a row.
  await page.locator('input[name=archived]').check();
  expect(await count(page)).toBe('1 of 2 selected');
  await page.locator('.table-select-all input').click();
  expect(await count(page)).toBe('2 of 2 selected');
  expect(await page.locator('input[name=archived]').isChecked()).toBe(true);
  expect(await page.locator('tbody input[disabled]').isChecked()).toBe(true);

  await page.getByRole('button', {name: 'Reset'}).click();
  await expect.poll(() => count(page)).toBe('1 of 2 selected');
  expect((await state(page)).all).toBe('mixed');
});

test('a table without rows disables select-all', async ({page}) => {
  await openFixture(page, `${table([])}${COUNT}`);
  await expect.poll(() => count(page)).toBe('0 of 0 selected');
  expect(await state(page)).toEqual({all: 'unchecked', disabled: true, rows: []});
});

for (const alpine of [false, 'external'] as const) {
  test(`without ${alpine === false ? 'any script' : 'Alpine'} select-all is hidden and the rows still submit`,
      async ({page}) => {
    let submitted = '';
    await openFixture(page, `<form method="get" action="/delete">${table(rows(3))}${COUNT}`
        + `<button>Delete selected</button></form>`, {
      alpine,
      routes: {'/delete': ({url}: Request) => (submitted = url.search, {body: '<p>Deleted</p>'})},
    });

    await expect(page.locator('.table-select-all input')).toBeHidden();
    expect(await page.locator('.table-select-all').evaluate(element => getComputedStyle(element).visibility))
        .toBe('hidden');
    expect(await page.locator('.table-selection-count').evaluate(element => getComputedStyle(element).visibility))
        .toBe('hidden');
    // Tab skips the hidden select-all.
    await page.locator('body').focus();
    await page.keyboard.press('Tab');
    await expect(page.locator('tbody tr').first().locator('input')).toBeFocused();
    await page.keyboard.press('Space');
    await page.locator('tbody tr').nth(2).locator('input').check();
    await page.getByRole('button', {name: 'Delete selected'}).click();
    await expect(page.locator('main')).toHaveText('Deleted');
    expect(submitted).toBe('?ids=1&ids=3');
  });
}

test('the indeterminate prop works with shadleaf.js and no Alpine, also on a checkbox htmx swaps in', async ({page}) => {
  const indeterminate = ROW_CHECKBOX.replace('{attributes}', 'name="all" aria-label="All" data-indeterminate="true"');
  await openFixture(page, `${indeterminate}<div id="later"></div>
    <button hx-get="/more" hx-target="#later">More</button>`, {
    alpine: 'external', htmx: true,
    routes: {'/more': () => ({body: indeterminate.replace('name="all" aria-label="All"', 'name="b" aria-label="B"')})},
  });
  const isIndeterminate = (selector: string) => page.locator(selector).evaluate(
      input => (input as HTMLInputElement).indeterminate);

  await expect.poll(() => isIndeterminate('input[name=all]')).toBe(true);
  await page.locator('input[name=all]').click();
  expect(await isIndeterminate('input[name=all]')).toBe(false);
  await page.getByRole('button', {name: 'More'}).click();
  await expect.poll(() => isIndeterminate('input[name=b]')).toBe(true);
  // The first one stays as the click left it.
  expect(await isIndeterminate('input[name=all]')).toBe(false);
});

test('an htmx swap of the rows or of the whole table resets the selection, and the count follows', async ({page}) => {
  const messages = await openFixture(page, `<div id="results">${table(rows(3))}</div>${COUNT}
    <button id="more-rows" hx-get="/rows" hx-target="#people tbody">Other rows</button>
    <button id="other-table" hx-get="/table" hx-target="#results">Other table</button>`, {
    htmx: true,
    routes: {
      '/rows': () => ({body: rows(2, 3).map(row => `<tr><td>${rowCheckbox(row)}</td><td>${row.name}</td><td></td></tr>`)
          .join('')}),
      '/table': () => ({body: table([...rows(4, 1), {name: 'Ada Lovelace', id: 1, checked: true}])}),
    },
  });

  await expect.poll(() => count(page)).toBe('0 of 3 selected');
  await page.locator('.table-select-all input').click();
  expect(await count(page)).toBe('3 of 3 selected');

  await page.locator('#more-rows').click();
  await expect(page.locator('tbody tr')).toHaveCount(2);
  await expect.poll(() => count(page)).toBe('0 of 2 selected');
  expect(await state(page)).toEqual({all: 'unchecked', disabled: false, rows: [false, false]});

  await page.locator('tbody tr').first().locator('input').click();
  expect(await state(page)).toEqual({all: 'mixed', disabled: false, rows: [true, false]});

  // The count sits outside the swapped part; the new table's select-all starts from the server's checked row.
  await page.locator('#other-table').click();
  await expect(page.locator('tbody tr')).toHaveCount(5);
  await expect.poll(() => count(page)).toBe('1 of 5 selected');
  expect((await state(page)).all).toBe('mixed');
  expect(messages).toEqual([]);
});

test('back to a page htmx kept: select-all and the count agree with the restored rows', async ({page}) => {
  const results = (number: number) => `<div id="results" hx-boost="true" hx-target="#results" hx-select="#results"
      hx-swap="outerHTML">${table(rows(3, number * 3))}${COUNT}
      <a id="page-link" href="/people?page=${1 - number}">Page ${2 - number}</a></div>`;
  const messages = await openFixture(page, '', {
    htmx: true,
    path: '/people?page=0',
    routes: {'/people': ({url}: Request) => ({body: results(Number(url.searchParams.get('page')))})},
  });

  await expect.poll(() => count(page)).toBe('0 of 3 selected');
  await page.locator('tbody tr').first().locator('input').click();
  expect(await count(page)).toBe('1 of 3 selected');

  await page.locator('#page-link').click();
  await expect(page.locator('tbody tr').first()).toContainText('Edsger Dijkstra');
  await expect.poll(() => count(page)).toBe('0 of 3 selected');
  await page.locator('.table-select-all input').click();
  expect(await count(page)).toBe('3 of 3 selected');

  await page.goBack();
  await expect(page.locator('tbody tr').first()).toContainText('Ada Lovelace');
  // htmx's snapshot is markup, which does not hold what was checked: whatever comes back, the two agree with it.
  const restored = await state(page);
  const selected = restored.rows.filter(Boolean).length;
  await expect.poll(() => count(page)).toBe(`${selected} of 3 selected`);
  expect(restored.all).toBe(selected === 0 ? 'unchecked' : selected === 3 ? 'checked' : 'mixed');
  expect(messages).toEqual([]);
});
