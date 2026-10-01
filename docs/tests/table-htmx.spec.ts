import {expect, test, type Page} from '@playwright/test';
import previews from '../src/generated/previews.json' with {type: 'json'};
import {openFixture, ORIGIN, type Request} from './fixture';

// A table with live search, as the guide "Tables with htmx" and sample-01's /people page build it, in a fixture page
// under a strict Content-Security-Policy with the csp Alpine build and htmx. The route plays the server: it filters,
// sorts and pages a list, builds the sort and page links from the request (as #slPaging does) and copies the request's
// parameters into the form's hidden inputs (as sl:query-params does), and answers an htmx request with the results
// alone. Checked: the search waits for a pause in typing, sends only the latest request, pushes the address, keeps
// the focus and what was typed while a request was out; Enter searches at once; a sort or page link swaps the results
// and gives the focus back to the link by its id; the next search keeps that order; back and forward show each
// search with its text in the field; without htmx the form still submits. Runs in Chromium and Firefox (no search
// event there, so Enter is the form's submit).
//
// Further down: "load more" for a Slice (a link or hx-trigger="revealed" appending the next slice's rows, with the
// focus on the first new row, select-all and the count following them, and the link without htmx), and a row menu
// in a table cell (from the table--row-actions preview) whose items post with htmx and keep the focus.

const PEOPLE = ['Ada Lovelace', 'Alan Turing', 'Grace Hopper', 'Adele Goldberg', 'Barbara Liskov', 'Edsger Dijkstra',
  'Donald Knuth', 'Frances Allen', 'Margaret Hamilton', 'Radia Perlman', 'Ken Thompson', 'Dana Scott'];
const SIZE = 3;

const escape = (text: string) => text.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;');

/** The same request with these parameters replaced (null removes one), as a link. */
function link(url: URL, changes: Record<string, string | null>) {
  const params = new URLSearchParams(url.search);
  for (const [name, value] of Object.entries(changes)) {
    params.delete(name);
    if (value !== null) {
      params.append(name, value);
    }
  }
  const query = params.toString().replace(/%2C/g, ',');
  return escape(`/people${query ? `?${query}` : ''}`);
}

function results(url: URL, fragment: boolean) {
  const q = (url.searchParams.get('q') ?? '').trim().toLowerCase();
  const descending = url.searchParams.get('sort') === 'name,desc';
  const number = Number(url.searchParams.get('page') ?? 0);
  const found = PEOPLE.filter(name => name.toLowerCase().includes(q)).sort();
  if (descending) {
    found.reverse();
  }
  const rows = found.slice(number * SIZE, number * SIZE + SIZE);
  const last = Math.max(Math.ceil(found.length / SIZE) - 1, 0);
  const hidden = [...url.searchParams].filter(([name]) => name !== 'q' && name !== 'page')
      .map(([name, value]) => `<input type="hidden" name="${escape(name)}" value="${escape(value)}" form="search">`)
      .join('');
  const pageLink = (id: string, target: number, enabled: boolean, text: string) => enabled
      ? `<a class="btn pagination-link" data-variant="ghost" id="${id}" href="${link(url, {page: String(target)})}">${text}</a>`
      : `<a class="btn pagination-link" data-variant="ghost" id="${id}" aria-disabled="true" role="link" tabindex="-1">${text}</a>`;
  return `<div id="results" hx-boost="true" hx-target="#results" hx-select="#results" hx-swap="outerHTML show:none">
    ${hidden}
    ${fragment ? `<p id="status" hx-swap-oob="innerHTML">${found.length} found</p>` : ''}
    <div class="table-container"><table class="table">
      <thead><tr><th class="table-head" aria-sort="${descending ? 'descending' : 'ascending'}"><a class="btn table-sort"
        data-variant="ghost" data-size="sm" id="sort-name"
        href="${link(url, {sort: descending ? 'name,asc' : 'name,desc', page: null})}">Name</a></th></tr></thead>
      <tbody>${rows.map(name => `<tr><td>${name}</td></tr>`).join('')
          || '<tr class="table-empty"><td>No results.</td></tr>'}</tbody>
    </table></div>
    <nav class="pagination" aria-label="Pagination"><span id="page-number">${number + 1}</span>
      ${pageLink('page-previous', number - 1, number > 0, 'Previous')}
      ${pageLink('page-next', number + 1, number < last, 'Next')}</nav>
  </div>`;
}

/** The page, or with HX-Request (not a history restore) only the results. */
function people({url, headers}: Request) {
  if (headers['hx-request'] === 'true' && headers['hx-history-restore-request'] !== 'true') {
    return {body: results(url, true)};
  }
  return {
    body: `<form id="search" role="search" method="get" action="/people"
        hx-get="/people" hx-trigger="input changed delay:300ms, search, submit" hx-sync="this:replace"
        hx-target="#results" hx-select="#results" hx-swap="outerHTML show:none" hx-push-url="true" hx-history="false">
      <input class="input" type="search" id="q" name="q" value="${escape(url.searchParams.get('q') ?? '')}"
             aria-label="Search people" autocomplete="off">
      <p id="status" class="sl-sr-only" role="status"></p>
    </form>
    ${results(url, false)}`,
    delay: responseDelay,
  };
}

let responseDelay = 0;
let requests: Request[] = [];

test.beforeEach(() => {
  responseDelay = 0;
  requests = [];
});

async function open(page: Page, path = '/people?sort=name,desc', htmx = true) {
  return openFixture(page, '', {
    htmx,
    path,
    routes: {
      '/people': request => {
        requests.push(request);
        const response = people(request);
        return request.headers['hx-request'] === 'true' ? {...response, delay: responseDelay} : response;
      },
    },
  });
}

const names = (page: Page) => page.locator('.table tbody td').allTextContents();
const focused = (page: Page) => page.evaluate(() => document.activeElement?.id);
const htmxRequests = () => requests.filter(request => request.headers['hx-request'] === 'true');

test('the search waits for a pause in typing, pushes the address and keeps the focus', async ({page}) => {
  const messages = await open(page);
  const marker = await page.evaluateHandle(() => document.body);

  await page.locator('#q').pressSequentially('ada', {delay: 50});
  await expect(page).toHaveURL(`${ORIGIN}/people?q=ada&sort=name%2Cdesc`);

  expect(await names(page)).toEqual(['Ada Lovelace']);
  expect(htmxRequests()).toHaveLength(1);
  expect(htmxRequests()[0].url.search).toBe('?q=ada&sort=name%2Cdesc');
  await expect.poll(() => focused(page)).toBe('q');
  await expect(page.locator('#status')).toHaveText('1 found');
  // The same document: htmx swapped the results, it did not load a page.
  expect(await marker.evaluate(body => body.isConnected)).toBe(true);
  expect(messages).toEqual([]);
});

test('what is typed while a request is out stays, and only the latest answer counts', async ({page}) => {
  await open(page, '/people');
  responseDelay = 600;

  await page.locator('#q').pressSequentially('a', {delay: 0});
  await expect.poll(() => htmxRequests().length).toBe(1);
  await page.locator('#q').pressSequentially('lan', {delay: 50});
  await expect(page).toHaveURL(`${ORIGIN}/people?q=alan`);

  await expect(page.locator('#q')).toHaveValue('alan');
  expect(await names(page)).toEqual(['Alan Turing']);
  await expect.poll(() => focused(page)).toBe('q');
  // Let the first (cancelled) answer's time pass: nothing changes.
  await page.waitForTimeout(700);
  expect(await names(page)).toEqual(['Alan Turing']);
});

test('Enter searches at once', async ({page}) => {
  await open(page, '/people');

  await page.locator('#q').fill('grace');
  await page.locator('#q').press('Enter');

  await expect(page).toHaveURL(`${ORIGIN}/people?q=grace`);
  expect(await names(page)).toEqual(['Grace Hopper']);
  await expect.poll(() => focused(page)).toBe('q');
  // No full page load: every request for the list was htmx's.
  expect(requests.filter(request => request.headers['hx-request'] !== 'true')).toHaveLength(1);
});

test('a sort or page link swaps the results and gives the focus back by id; the next search keeps the order',
    async ({page}) => {
  await open(page, '/people?q=a');

  await page.locator('#page-next').focus();
  await page.keyboard.press('Enter');
  await expect(page.locator('#page-number')).toHaveText('2');
  await expect(page).toHaveURL(`${ORIGIN}/people?q=a&page=1`);
  await expect.poll(() => focused(page)).toBe('page-next');

  // htmx boosts the swapped-in links when the swap settles (20 ms); a person cannot click sooner, a test can.
  await expect(page.locator('.htmx-settling')).toHaveCount(0);
  await page.locator('#sort-name').focus();
  await page.keyboard.press('Enter');
  await expect(page).toHaveURL(`${ORIGIN}/people?q=a&sort=name,desc`);
  await expect(page.locator('#page-number')).toHaveText('1');
  await expect.poll(() => focused(page)).toBe('sort-name');
  expect(await page.locator('input[type=hidden][form=search]').evaluateAll(
      inputs => inputs.map(input => `${(input as HTMLInputElement).name}=${(input as HTMLInputElement).value}`)))
      .toEqual(['sort=name,desc']);

  await page.locator('#q').fill('ad');
  await expect(page).toHaveURL(`${ORIGIN}/people?q=ad&sort=name%2Cdesc`);
  expect(await names(page)).toEqual(['Radia Perlman', 'Adele Goldberg', 'Ada Lovelace']);
  expect(htmxRequests().every(request => request.headers['hx-current-url']?.startsWith(`${ORIGIN}/people`))).toBe(true);
});

test('back and forward show each search with its text', async ({page}) => {
  await open(page, '/people');

  await page.locator('#q').fill('ada');
  await expect(page).toHaveURL(`${ORIGIN}/people?q=ada`);
  await page.locator('#q').fill('grace');
  await expect(page).toHaveURL(`${ORIGIN}/people?q=grace`);

  await page.goBack();
  await expect(page).toHaveURL(`${ORIGIN}/people?q=ada`);
  await expect(page.locator('#q')).toHaveValue('ada');
  expect(await names(page)).toEqual(['Ada Lovelace']);

  await page.goBack();
  await expect(page).toHaveURL(`${ORIGIN}/people`);
  await expect(page.locator('#q')).toHaveValue('');
  expect(await names(page)).toHaveLength(SIZE);

  await page.goForward();
  await expect(page).toHaveURL(`${ORIGIN}/people?q=ada`);
  await expect(page.locator('#q')).toHaveValue('ada');
  expect(await names(page)).toEqual(['Ada Lovelace']);
  // hx-history="false": htmx kept no snapshot, so each step back or forward asked the server for the whole page.
  expect(requests.filter(request => request.headers['hx-history-restore-request'] === 'true')).toHaveLength(3);
});

test('without htmx the form submits and keeps the order', async ({page}) => {
  await open(page, '/people?sort=name,desc&page=1', false);

  await page.locator('#q').fill('ada');
  await page.locator('#q').press('Enter');

  await expect(page).toHaveURL(`${ORIGIN}/people?q=ada&sort=name%2Cdesc`);
  expect(await names(page)).toEqual(['Ada Lovelace']);
});

// --- load more ---------------------------------------------------------------------------------------------------------

type Scenario = { id: string, html: string };
const scenarios = previews.scenarios as Scenario[];
const SELECTION = scenarios.find(scenario => scenario.id === 'table--selection')!.html;
const SELECT_ALL = SELECTION.match(/<span class="checkbox-wrapper table-select-all"[\s\S]*?role="status"><\/span>\s*<\/span>/)![0];
const COUNT = SELECTION.match(/<span class="table-selection-count"[\s\S]*?<\/span>/)![0]
    .replace('data-table="people-selection"', 'data-table="feed"');
const ROW_CHECKBOX = SELECTION.match(/<span class="checkbox-wrapper">[\s\S]*?<\/span>/)![0]
    .replace(/name="ids" value="1" aria-label="Select Ada Lovelace" checked/, '{attributes}');

type Feed = { revealed?: boolean, selection?: boolean };
let feedRequests: Request[] = [];

/**
 * The rows of one slice of PEOPLE (sorted) and, while there is a next one, a row with the Load more link: what
 * sample-01's /people-load-more renders. The first row of each slice after the first can take the focus; in an htmx
 * answer it has autofocus, unless the next slice comes on its own (revealed).
 */
function feedRows(url: URL, {revealed = false, selection = false}: Feed, from: number, to: number, fragment: boolean) {
  const people = [...PEOPLE].sort();
  const columns = selection ? 2 : 1;
  const rows = people.slice(from, to).map((name, index) => {
    const position = from + index;
    const first = position > 0 && position % SIZE === 0;
    const checkbox = selection ? `<td>${ROW_CHECKBOX.replace('{attributes}',
        `name="ids" value="${position + 1}" aria-label="Select ${name}"`)}</td>` : '';
    return `<tr id="row-${position + 1}"${first ? ' tabindex="-1"' : ''}${first && fragment && !revealed
        ? ' autofocus' : ''}>${checkbox}<td>${name}</td></tr>`;
  }).join('');
  if (to >= people.length) {
    return rows;
  }
  const params = new URLSearchParams(url.search);
  params.set('page', String(to / SIZE));
  const next = escape(`/feed?${params}`);
  const trigger = revealed
      ? `<span class="sl-sr-only" hx-get="${next}" hx-trigger="revealed" hx-target="closest tr" hx-swap="outerHTML"
          hx-replace-url="true">Loading more</span>`
      : `<a class="btn" data-variant="outline" href="${next}#row-${to + 1}" hx-get="${next}" hx-target="closest tr"
          hx-swap="outerHTML" hx-replace-url="true">Load more</a>`;
  return `${rows}<tr class="load-more-row"><td colspan="${columns}">${trigger}</td></tr>`;
}

function feed(options: Feed) {
  return ({url, headers}: Request) => {
    feedRequests.push({url, headers, body: ''});
    const page = Number(url.searchParams.get('page') ?? 0);
    if (headers['hx-request'] === 'true' && headers['hx-history-restore-request'] !== 'true') {
      return {body: feedRows(url, options, page * SIZE, page * SIZE + SIZE, true)};
    }
    // A page load gets every slice up to the asked one, as htmx would have appended them.
    return {
      body: `<div class="table-container"><table class="table" id="feed"><thead><tr>
          ${options.selection ? `<th>${SELECT_ALL}</th>` : ''}<th>Name</th></tr></thead>
        <tbody>${feedRows(url, options, 0, page * SIZE + SIZE, false)}</tbody></table></div>
        ${options.selection ? COUNT : ''}`,
    };
  };
}

async function openFeed(page: Page, options: Feed = {}, htmx = true, path = '/feed') {
  feedRequests = [];
  return openFixture(page, '', {htmx, path, routes: {'/feed': feed(options)}});
}

const feedNames = (page: Page) => page.locator('#feed tbody tr:not(.load-more-row) td:last-child').allTextContents();
const SORTED = [...PEOPLE].sort();

test('load more appends the next slice, replaces its own row and focuses the first new row', async ({page}) => {
  const messages = await openFeed(page);
  const entries = await page.evaluate(() => history.length);
  expect(await feedNames(page)).toEqual(SORTED.slice(0, 3));

  await page.getByRole('link', {name: 'Load more'}).focus();
  await page.keyboard.press('Enter');
  await expect(page.locator('#feed tbody tr:not(.load-more-row)')).toHaveCount(6);
  expect(await feedNames(page)).toEqual(SORTED.slice(0, 6));
  await expect(page.locator('.load-more-row')).toHaveCount(1);
  await expect(page.locator('.load-more-row a')).toHaveAttribute('hx-get', '/feed?page=2');
  await expect.poll(() => focused(page)).toBe('row-4');
  // The address follows, without a history entry: a reload shows the same rows.
  await expect(page).toHaveURL(`${ORIGIN}/feed?page=1`);
  expect(await page.evaluate(() => history.length)).toBe(entries);

  // Tab from the new row goes on to the next Load more; the next one appends the following slice. The browser focuses
  // the autofocus row when it is inserted, and htmx again when the swap settles (20 ms later): a person cannot press
  // Tab in between, a test can.
  await expect(page.locator('.htmx-settling')).toHaveCount(0);
  await page.keyboard.press('Tab');
  expect(await page.evaluate(() => document.activeElement?.textContent?.trim())).toBe('Load more');
  await page.keyboard.press('Enter');
  await expect.poll(() => focused(page)).toBe('row-7');
  await page.getByRole('link', {name: 'Load more'}).click();
  await expect.poll(() => focused(page)).toBe('row-10');
  expect(await feedNames(page)).toEqual(SORTED);
  await expect(page.locator('.load-more-row')).toHaveCount(0);
  expect(feedRequests.filter(request => request.headers['hx-request'] === 'true')).toHaveLength(3);

  await page.reload();
  expect(await feedNames(page)).toEqual(SORTED);
  expect(messages).toEqual([]);
});

test('hx-trigger="revealed" loads the next slice when its row scrolls into view, without moving the focus',
    async ({page}) => {
  // Short enough that the first slice's trigger row starts below the fold.
  await page.setViewportSize({width: 800, height: 150});
  await openFeed(page, {revealed: true});
  expect(await feedNames(page)).toEqual(SORTED.slice(0, 3));
  await page.evaluate(() => document.body.insertAdjacentHTML('afterbegin', '<button id="before">Before</button>'));
  await page.locator('#before').focus();
  const loaded = () => page.locator('#feed tbody tr:not(.load-more-row)').count();

  // Each slice's last row brings the next one into view, until the list ends.
  for (let rows = 3; rows < PEOPLE.length; rows += 3) {
    await page.locator('#feed tbody tr').last().scrollIntoViewIfNeeded();
    await expect.poll(loaded).toBeGreaterThan(rows);
  }
  expect(await feedNames(page)).toEqual(SORTED);
  await expect(page.locator('.load-more-row')).toHaveCount(0);
  expect(await focused(page)).toBe('before');
  await expect(page).toHaveURL(`${ORIGIN}/feed?page=3`);
});

test('select-all and the count follow the appended rows', async ({page}) => {
  const messages = await openFeed(page, {selection: true});
  const selectAll = page.locator('.table-select-all input');
  const count = page.locator('.table-selection-count');
  await expect(count).toHaveText('0 of 3 selected');

  await selectAll.check();
  await expect(count).toHaveText('3 of 3 selected');
  await page.getByRole('link', {name: 'Load more'}).click();
  await expect(page.locator('#feed tbody tr:not(.load-more-row)')).toHaveCount(6);

  // The new rows are not selected: select-all is now mixed, and the count says so.
  await expect(count).toHaveText('3 of 6 selected');
  await expect.poll(() => selectAll.evaluate(input => (input as HTMLInputElement).indeterminate)).toBe(true);
  await selectAll.click();
  await expect(count).toHaveText('6 of 6 selected');
  expect(await page.locator('#feed tbody input[name=ids]:checked').count()).toBe(6);
  expect(messages).toEqual([]);
});

test('without htmx Load more is a link to the page with one slice more, at the first new row', async ({page}) => {
  await openFeed(page, {}, false);

  await page.getByRole('link', {name: 'Load more'}).click();
  await expect(page).toHaveURL(`${ORIGIN}/feed?page=1#row-4`);
  expect(await feedNames(page)).toEqual(SORTED.slice(0, 6));
  // The browser moves the focus to the target of the address's fragment, as it can take it (tabindex="-1").
  await expect.poll(() => focused(page)).toBe('row-4');
  await page.keyboard.press('Tab');
  expect(await page.evaluate(() => document.activeElement?.textContent?.trim())).toBe('Load more');
});

// --- a row menu --------------------------------------------------------------------------------------------------------

const ROW_ACTIONS = scenarios.find(scenario => scenario.id === 'table--row-actions')!.html;
/** The first row's menu (Ada Lovelace's), as sl:dropdown-menu renders it. */
const ROW_MENU = ROW_ACTIONS.match(/<td data-align="end">([\s\S]*?)<\/td>/)![1];

/**
 * A table of people with a role each and, as sample-01's /people renders it, a menu per row in a form posting its
 * person: the role items submit it, and with htmx post it themselves; the results replace themselves.
 */
function rowMenus(people: [string, string][]) {
  const rows = people.map(([name, role], index) => {
    const id = `row-actions-${index + 1}`;
    const menu = ROW_MENU.replaceAll('row-actions-1', id)
        .replace('Actions for Ada Lovelace', `Actions for ${name}`)
        .replace(/aria-checked="true"/g, 'aria-checked="false"')
        .replace(`aria-checked="false" name="role" value="${role.toUpperCase()}"`,
            `aria-checked="true" name="role" value="${role.toUpperCase()}"`)
        .replace(/type="button"(\s+)class="dropdown-menu-radio-item"/g,
            'type="submit" hx-post="/people"$1class="dropdown-menu-radio-item"');
    return `<tr><td>${name}</td><td><span class="badge">${role}</span></td><td data-align="end">
      <form method="post" action="/people"><input type="hidden" name="person" value="${index + 1}">${menu}</form></td></tr>`;
  }).join('');
  return `<div id="results" hx-target="#results" hx-select="#results" hx-swap="outerHTML">
    <div class="table-container"><table class="table"><tbody>${rows}</tbody></table></div></div>`;
}

test('a row menu in an end-aligned cell, whose role items post with htmx and keep the focus', async ({page}) => {
  const people: [string, string][] = [['Ada Lovelace', 'Owner'], ['Alan Turing', 'Member'], ['Grace Hopper', 'Admin']];
  const posts: string[] = [];
  const messages = await openFixture(page, rowMenus(people), {
    htmx: true,
    routes: {
      '/people': ({body}) => {
        posts.push(body);
        const params = new URLSearchParams(body);
        const role = params.get('role')!;
        people[Number(params.get('person')) - 1][1] = role.charAt(0) + role.slice(1).toLowerCase();
        return {body: rowMenus(people)};
      },
    },
  });

  const trigger = page.locator('#row-actions-2-trigger');
  await trigger.focus();
  await page.keyboard.press('Enter');
  const menu = page.locator('#row-actions-2');
  await expect(menu).toBeVisible();
  // The keyboard opened it, so the focus moves to its first item (a moment after it opens).
  await expect.poll(() => menu.evaluate(element => element.contains(document.activeElement))).toBe(true);
  // The cell is end-aligned and does not wrap; the menu starts from the defaults.
  expect(await menu.locator('.dropdown-menu-label').evaluate(label => getComputedStyle(label).textAlign))
      .toMatch(/^(start|left)$/);
  expect(await menu.evaluate(element => getComputedStyle(element).whiteSpace)).toBe('normal');

  await page.keyboard.type('Ad');
  await page.keyboard.press('Enter');
  await expect(page.locator('tbody tr').nth(1).locator('.badge')).toHaveText('Admin');
  expect(posts).toEqual(['person=2&role=ADMIN']);
  await expect.poll(() => focused(page)).toBe('row-actions-2-trigger');
  await expect(page.locator('#row-actions-2 [aria-checked=true]')).toHaveText('Admin');
  expect(messages).toEqual([]);
});
