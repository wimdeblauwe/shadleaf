import {expect, test, type Page} from '@playwright/test';
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
