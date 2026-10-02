import {expect, type Page, test} from '@playwright/test';
import {openFixture, type Request, type Response} from './fixture';

// An htmx request after the session expired, as the Spring Security guide's "htmx after the session expired" sets it
// up (htmx-spring-boot's HxRedirectToPage* classes) and sample-02 has it: the route plays the server, with a session
// that is signed in or not. A page asked for without a session is redirected to the sign-in page, which saves it as
// the page to return to. An htmx request without one gets HX-Redirect to its page (HX-Current-URL, or the link itself
// for a boosted link), with 401, or 403 for a POST whose CSRF token went with the session; the browser then loads the
// page and lands on the sign-in page, which returns to the page. The negative control answers as Spring Security does
// by default: the htmx request is redirected to the sign-in page, which htmx swaps into its target (or, for a POST, a
// 403 that htmx does not swap), and the fragment's URL is what signing in returns to.
// Playwright does not route the request a fulfilled redirect leads to, so where the server would redirect (to the
// sign-in page, and back to the saved page after signing in) the route answers with the page the redirect ends on.

type Server = { signedIn: boolean, saved: string | null, returnedTo: string | null, requests: { path: string, method: string, htmx: boolean }[] };

const page = (title: string, content: string) => `<h1>${title}</h1>${content}`;

const data = `<nav><a id="to-forms" href="/forms">Forms</a></nav>
<button id="load-about" hx-get="/data/about" hx-target="#about">Show About</button>
<section id="about"></section>`;
const about = '<p id="about-text">Loaded with htmx.</p>';
const forms = `<form id="profile" action="/forms" method="post">
  <input type="hidden" name="_csrf" value="token-of-the-session">
  <label>Name <input name="name" value="Grace Hopper"></label>
  <button id="save">Save</button>
</form>`;
const login = `<form id="sign-in" action="/login" method="post" hx-boost="false">
  <label>Username <input name="username"></label>
  <button id="sign-in-button">Sign in</button>
</form>`;

/** Path and query of HX-Current-URL, as HxRedirectToPage takes it. */
const currentPage = (request: Request) => {
  const current = new URL(request.headers['hx-current-url']);
  return current.pathname + current.search;
};

function server(htmxAware: boolean) {
  const state: Server = {signedIn: true, saved: null, returnedTo: null, requests: []};
  const htmx = (request: Request) => request.headers['hx-request'] === 'true';
  const boosted = (request: Request) => request.headers['hx-boosted'] === 'true';

  /** The content of a page, or of the fragment, as the signed-in server renders it. */
  const render = (path: string): string => {
    const pathname = new URL(path, 'http://app.test').pathname;
    return pathname === '/data/about' ? about : pathname === '/forms' ? page('Forms', forms) : page('Data', data);
  };

  /** What Spring Security answers a request without a session; null when it may pass. */
  const security = (request: Request, method: string): Response | null => {
    state.requests.push({path: request.url.pathname, method, htmx: htmx(request)});
    if (state.signedIn) {
      return null;
    }
    const path = request.url.pathname + request.url.search;
    if (method === 'POST') {
      // CsrfFilter: the token went with the session.
      return htmxAware && htmx(request)
          ? {body: '', status: 403, headers: {'HX-Redirect': currentPage(request)}}
          : {body: page('Forbidden', '<p id="forbidden">403</p>'), status: 403};
    }
    if (htmxAware && htmx(request)) {
      // HxRedirectToPageAuthenticationEntryPoint: nothing saved.
      const target = boosted(request) ? path : currentPage(request);
      return {body: '', status: 401, headers: {'HX-Redirect': target}};
    }
    // The login form's entry point saves the request and redirects to the sign-in page (an XHR follows that).
    state.saved = path;
    return {body: page('Sign in', login)};
  };

  const routes = {
    '/data': (request: Request) => security(request, 'GET') ?? {body: render('/data')},
    '/data/about': (request: Request) => security(request, 'GET') ?? {body: render('/data/about')},
    '/forms': (request: Request) => {
      const method = request.body ? 'POST' : 'GET';
      return security(request, method)
          ?? {body: method === 'POST' ? page('Saved', '<p>Profile saved.</p>') : render('/forms')};
    },
    '/login': (request: Request) => {
      if (request.body) {
        // Signed in: Spring Security redirects to the saved request.
        state.signedIn = true;
        state.returnedTo = state.saved ?? '/data';
        state.saved = null;
        return {body: render(state.returnedTo)};
      }
      return {body: page('Sign in', login)};
    },
  };
  return {state, routes};
}

async function open(page: Page, htmxAware = true, path = '/data') {
  const {state, routes} = server(htmxAware);
  const messages = await openFixture(page, '', {htmx: true, alpine: false, routes, path, bodyAttributes: 'hx-boost="true"'});
  return {state, messages};
}

const heading = (page: Page) => page.getByRole('heading', {level: 1});

async function signIn(page: Page) {
  await page.getByLabel('Username').fill('grace');
  await page.locator('#sign-in-button').click();
}

/** The console, without the browser's own lines for the 401 and 403 the server answers on purpose. */
const problems = (messages: string[]) => messages.filter(message => !message.startsWith('Failed to load resource'));

/** htmx is done with a request (swapped, or navigating away) when no element is .htmx-request any more. */
const settled = (page: Page) => expect(page.locator('.htmx-request')).toHaveCount(0);

test.describe('after the session expired', () => {
  test('a fragment request lands on the sign-in page, and signing in returns to the page', async ({page}) => {
    const {state, messages} = await open(page);
    state.signedIn = false;
    await page.locator('#load-about').click();
    await expect(heading(page)).toHaveText('Sign in');
    // A navigation to the page, not a swap: nothing of the Data page is left.
    expect(new URL(page.url()).pathname).toBe('/data');
    await expect(page.locator('#load-about')).toHaveCount(0);
    // The page is saved, not the fragment.
    expect(state.requests.map(r => `${r.method} ${r.path}${r.htmx ? ' htmx' : ''}`))
        .toEqual(['GET /data', 'GET /data/about htmx', 'GET /data']);
    expect(state.saved).toBe('/data');
    await signIn(page);
    expect(state.returnedTo).toBe('/data');
    await expect(heading(page)).toHaveText('Data');
    await page.locator('#load-about').click();
    await expect(page.locator('#about-text')).toBeVisible();
    expect(problems(messages)).toEqual([]);
  });

  test('a boosted link lands on the sign-in page, and signing in goes to the page it links to', async ({page}) => {
    const {state, messages} = await open(page);
    state.signedIn = false;
    await page.locator('#to-forms').click();
    await expect(heading(page)).toHaveText('Sign in');
    expect(new URL(page.url()).pathname).toBe('/forms');
    await signIn(page);
    expect(state.returnedTo).toBe('/forms');
    await expect(heading(page)).toHaveText('Forms');
    expect(problems(messages)).toEqual([]);
  });

  test('a boosted form whose token went with the session lands on the sign-in page and returns to the form', async ({page}) => {
    const {state, messages} = await open(page, true, '/forms');
    await settled(page);
    state.signedIn = false;
    await page.locator('#save').click();
    await expect(heading(page)).toHaveText('Sign in');
    expect(state.requests.filter(r => r.method === 'POST')).toEqual([{path: '/forms', method: 'POST', htmx: true}]);
    await signIn(page);
    expect(state.returnedTo).toBe('/forms');
    await expect(heading(page)).toHaveText('Forms');
    expect(problems(messages)).toEqual([]);
  });

  test('while signed in, nothing changes', async ({page}) => {
    const {messages} = await open(page);
    await page.locator('#load-about').click();
    await expect(page.locator('#about-text')).toBeVisible();
    await expect(heading(page)).toHaveText('Data');
    await page.locator('#to-forms').click();
    await expect(heading(page)).toHaveText('Forms');
    await page.locator('#save').click();
    await expect(heading(page)).toHaveText('Saved');
    expect(problems(messages)).toEqual([]);
  });
});

test.describe('negative control: Spring Security\'s defaults', () => {
  test('the sign-in page is swapped into the fragment\'s target, and signing in returns to the fragment', async ({page}) => {
    const {state} = await open(page, false);
    state.signedIn = false;
    await page.locator('#load-about').click();
    // The XHR followed the redirect: the sign-in page is now inside the Data page.
    await expect(page.locator('#about #sign-in')).toBeVisible();
    await expect(page.locator('h1').first()).toHaveText('Data');
    expect(state.saved).toBe('/data/about');
    // Signing in there returns to the fragment's URL: the fragment alone, as a page.
    await signIn(page);
    expect(state.returnedTo).toBe('/data/about');
    await expect(page.locator('#about-text')).toBeVisible();
    await expect(page.locator('h1')).toHaveCount(0);
  });

  test('a boosted form gets a 403 that htmx does not show', async ({page}) => {
    const {state} = await open(page, false, '/forms');
    await settled(page);
    state.signedIn = false;
    await page.locator('#save').click();
    await expect.poll(() => state.requests.some(r => r.method === 'POST')).toBe(true);
    await settled(page);
    await expect(heading(page)).toHaveText('Forms');
    await expect(page.locator('#forbidden')).toHaveCount(0);
  });
});
