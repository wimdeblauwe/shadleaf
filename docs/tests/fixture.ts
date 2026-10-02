import {type Page} from '@playwright/test';
import {join} from 'node:path';
import previews from '../src/generated/previews.json' with {type: 'json'};
import themeScript from '../src/generated/theme-script.json' with {type: 'json'};

// Fixture pages for disclosure.spec.ts, tabs.spec.ts, theme-toggle.spec.ts, avatar.spec.ts, table-htmx.spec.ts and
// sidebar.spec.ts,
// served from a made-up origin under a strict Content-Security-Policy, with the csp Alpine build (or shadleaf.js
// without any Alpine, or no script of Shadleaf's at all) and optionally htmx. The page at / and every route get the
// request (URL and headers, cookies included), so a fixture can render what a server would for ?tab=... or a cookie. The
// content goes in a <main>, unless it brings its own (wrap: false). A route can also answer with something other
// than a page (an image), a status and after a delay. An htmx request to a route gets the route's markup alone, as a
// fragment; any other request, a history restore included, gets it as a page.

export const ORIGIN = 'http://app.test';
const PUBLIC_DIR = join(import.meta.dirname, '..', 'public');
const HTMX = join(import.meta.dirname, '..', 'node_modules', 'htmx.org', 'dist', 'htmx.min.js');
const STRICT_CSP = "default-src 'self'; script-src 'self'; style-src 'self'; object-src 'none'; base-uri 'self'";
const HTMX_CONFIG = `<meta name="htmx-config" content='{"includeIndicatorStyles": false, "allowEval": false}'>`;

type Scripts = { bundled: string, csp: string, external: string };
const scripts = (previews as unknown as { scripts: Scripts }).scripts;
const css = previews.skins[0].css;

/** The pictures servePhotos took out of previews, by the path the fixture serves them at. */
const photos = new Map<string, { contentType: string, body: Buffer }>();

/**
 * A preview's markup with each data: picture (a photo from the generator's previews/photos/) replaced by a
 * same-origin URL the fixture serves: the fixture's policy blocks data: images, as an application's strict one would.
 */
export function servePhotos(html: string): string {
  const paths = new Map<string, string>();
  return html.replace(/src="data:(image\/[a-z+]+);base64,([^"]+)"/g, (_, contentType: string, data: string) => {
    let path = paths.get(data);
    if (!path) {
      path = `/photos/${photos.size + 1}`;
      paths.set(data, path);
      photos.set(path, {contentType, body: Buffer.from(data, 'base64')});
    }
    return `src="${path}"`;
  });
}

export type Request = { url: URL, body: string, headers: Record<string, string> };
export type Response = {
  body: string | Buffer,
  headers?: Record<string, string>,
  /** Anything but text/html is sent as it is, not wrapped in a page. */
  contentType?: string,
  status?: number,
  /** Milliseconds before the response is sent. */
  delay?: number,
};
export type Routes = Record<string, (request: Request) => Response>;

export type FixtureOptions = {
  /**
   * Load the csp Alpine build (true, the default), shadleaf.js with no Alpine on the page ('external': what an
   * application on shadleaf.assets.alpine=external has before it loads its own), or no script of Shadleaf's at all.
   */
  alpine?: boolean | 'external',
  /** Milliseconds before Shadleaf's scripts are sent, so the page is parsed and painted before they run. */
  scriptDelay?: number,
  htmx?: boolean,
  routes?: Routes,
  /** Where to go first, e.g. '/#refunds' or '/?tab=b'. */
  path?: string,
  /** Put the theme script (sl/layout :: theme-script) first in the head, allowed by its hash. */
  theme?: boolean,
  /** Put the content in a <main> (true, the default), or straight into the body: an application shell has its own. */
  wrap?: boolean,
  /** Attributes for <html> and <body>, e.g. 'dir="rtl"' or 'hx-boost="true"'. */
  htmlAttributes?: string,
  bodyAttributes?: string,
};

/** Serves `main` at /, and `routes` for other paths; returns what the console said (errors, warnings, CSP). */
export async function openFixture(page: Page, main: string | ((url: URL, request: Request) => string),
    {alpine = true, htmx = false, routes = {}, path = '/', theme = false, scriptDelay = 0, wrap = true,
      htmlAttributes = '', bodyAttributes = ''}: FixtureOptions = {})
    : Promise<string[]> {
  const messages: string[] = [];
  page.on('console', message => {
    // Firefox warns about deprecated globals when Alpine scans window; that is not the fixture's doing.
    if (message.type() === 'error' || (message.type() === 'warning' && !message.text().includes('deprecated'))) {
      messages.push(message.text());
    }
  });
  page.on('pageerror', error => messages.push(error.message));
  await page.addInitScript(() => document.addEventListener('securitypolicyviolation',
      event => console.error(`CSP violation: ${event.violatedDirective}`)));
  const document = (content: string) => `<!doctype html><html lang="en" ${htmlAttributes}><head><meta charset="utf-8"><title>Fixture</title>`
      + (theme ? `<script>${themeScript.script}</script>` : '')
      + `<link rel="stylesheet" href="/${css}">`
      + (htmx ? `${HTMX_CONFIG}<script defer src="/webjars/htmx.min.js"></script>` : '')
      + (alpine ? `<script type="module" src="/${alpine === 'external' ? scripts.external : scripts.csp}"></script>` : '')
      + `</head><body ${bodyAttributes}>${wrap ? `<main>${content}</main>` : content}</body></html>`;
  await page.route(`${ORIGIN}/**`, async route => {
    const request = route.request();
    const url = new URL(request.url());
    if (url.pathname === '/webjars/htmx.min.js') {
      return route.fulfill({contentType: 'text/javascript', path: HTMX});
    }
    const photo = photos.get(url.pathname);
    if (photo) {
      return route.fulfill(photo);
    }
    if (url.pathname.startsWith('/shadleaf/')) {
      if (scriptDelay && url.pathname.endsWith('.js')) {
        await new Promise(resolve => setTimeout(resolve, scriptDelay));
      }
      return route.fulfill({path: join(PUBLIC_DIR, url.pathname)});
    }
    const headers = {'Content-Security-Policy': theme
        ? STRICT_CSP.replace("script-src 'self'", `script-src 'self' ${themeScript.cspHash}`) : STRICT_CSP};
    if (url.pathname === '/') {
      const content = typeof main === 'string' ? main
          : main(url, {url, body: request.postData() ?? '', headers: request.headers()});
      return route.fulfill({contentType: 'text/html', headers, body: document(content)});
    }
    const handler = routes[url.pathname];
    if (handler) {
      const response = handler({url, body: request.postData() ?? '', headers: request.headers()});
      if (response.delay) {
        await new Promise(resolve => setTimeout(resolve, response.delay));
      }
      const {status = 200, contentType = 'text/html'} = response;
      if (contentType !== 'text/html') {
        return route.fulfill({status, contentType, headers: response.headers, body: response.body});
      }
      const body = String(response.body);
      // htmx asks for a page it did not keep in its history cache with HX-Request too: that gets the whole page.
      const fragment = request.headers()['hx-request'] === 'true'
          && request.headers()['hx-history-restore-request'] !== 'true';
      const html = fragment ? body : document(body);
      return route.fulfill({status, contentType, headers: {...headers, ...response.headers}, body: html});
    }
    return route.fulfill({status: 404});
  });
  await page.goto(`${ORIGIN}${path}`);
  if (alpine === true) {
    await page.waitForFunction(() => 'Alpine' in window);
  }
  if (htmx) {
    await page.waitForFunction(() => 'htmx' in window);
  }
  return messages;
}
