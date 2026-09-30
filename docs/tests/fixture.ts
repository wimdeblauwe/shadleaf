import {type Page} from '@playwright/test';
import {join} from 'node:path';
import previews from '../src/generated/previews.json' with {type: 'json'};

// Fixture pages for disclosure.spec.ts and tabs.spec.ts, served from a made-up origin under a strict
// Content-Security-Policy, with the csp Alpine build (or no Alpine at all) and optionally htmx. The page at / and every
// route get the request's URL, so a fixture can render what a server would for ?tab=...

export const ORIGIN = 'http://app.test';
const PUBLIC_DIR = join(import.meta.dirname, '..', 'public');
const HTMX = join(import.meta.dirname, '..', 'node_modules', 'htmx.org', 'dist', 'htmx.min.js');
const STRICT_CSP = "default-src 'self'; script-src 'self'; style-src 'self'; object-src 'none'; base-uri 'self'";
const HTMX_CONFIG = `<meta name="htmx-config" content='{"includeIndicatorStyles": false, "allowEval": false}'>`;

type Scripts = { bundled: string, csp: string, external: string };
const scripts = (previews as unknown as { scripts: Scripts }).scripts;
const css = previews.skins[0].css;

export type Request = { url: URL, body: string, headers: Record<string, string> };
export type Response = { body: string, headers?: Record<string, string> };
export type Routes = Record<string, (request: Request) => Response>;

export type FixtureOptions = {
  /** Load the csp Alpine build (the default), or no script of Shadleaf's at all. */
  alpine?: boolean,
  htmx?: boolean,
  routes?: Routes,
  /** Where to go first, e.g. '/#refunds' or '/?tab=b'. */
  path?: string,
};

/** Serves `main` at /, and `routes` for other paths; returns what the console said (errors, warnings, CSP). */
export async function openFixture(page: Page, main: string | ((url: URL) => string),
    {alpine = true, htmx = false, routes = {}, path = '/'}: FixtureOptions = {}): Promise<string[]> {
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
  const document = (content: string) => `<!doctype html><html lang="en"><head><title>Fixture</title>`
      + `<link rel="stylesheet" href="/${css}">`
      + (htmx ? `${HTMX_CONFIG}<script defer src="/webjars/htmx.min.js"></script>` : '')
      + (alpine ? `<script type="module" src="/${scripts.csp}"></script>` : '')
      + `</head><body><main>${content}</main></body></html>`;
  await page.route(`${ORIGIN}/**`, route => {
    const request = route.request();
    const url = new URL(request.url());
    if (url.pathname === '/webjars/htmx.min.js') {
      return route.fulfill({contentType: 'text/javascript', path: HTMX});
    }
    if (url.pathname.startsWith('/shadleaf/')) {
      return route.fulfill({path: join(PUBLIC_DIR, url.pathname)});
    }
    const headers = {'Content-Security-Policy': STRICT_CSP};
    if (url.pathname === '/') {
      const content = typeof main === 'string' ? main : main(url);
      return route.fulfill({contentType: 'text/html', headers, body: document(content)});
    }
    const handler = routes[url.pathname];
    if (handler) {
      const response = handler({url, body: request.postData() ?? '', headers: request.headers()});
      const html = request.headers()['hx-request'] === 'true' ? response.body : document(response.body);
      return route.fulfill({contentType: 'text/html', headers: {...headers, ...response.headers}, body: html});
    }
    return route.fulfill({status: 404});
  });
  await page.goto(`${ORIGIN}${path}`);
  if (alpine) {
    await page.waitForFunction(() => 'Alpine' in window);
  }
  if (htmx) {
    await page.waitForFunction(() => 'htmx' in window);
  }
  return messages;
}
