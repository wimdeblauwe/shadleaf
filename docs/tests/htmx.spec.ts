import {expect, test, type Page} from '@playwright/test';
import {join} from 'node:path';

// An htmx form that posts and gets its fragment back, as sample-01's /form does, served from a made-up origin through
// page.route under a strict Content-Security-Policy. The server renders `autofocus` on what should take focus after
// a submit (sl:form-errors autofocus: the summary when it shows, otherwise the first control with errors); these
// tests prove that htmx moves focus there, also when it would restore focus to the field the user pressed Enter in.
// htmx is pinned in package.json to the version sample-01's pom serves as a webjar.

const ORIGIN = 'http://app.test';
const HTMX = join(import.meta.dirname, '..', 'node_modules', 'htmx.org', 'dist', 'htmx.min.js');
const STRICT_CSP = "default-src 'self'; script-src 'self'; style-src 'self'; object-src 'none'; base-uri 'self'";
// sample-01's layout: no inline <style> for the request indicator, and no eval.
const HTMX_CONFIG = `<meta name="htmx-config" content='{"includeIndicatorStyles": false, "allowEval": false}'>`;

type Values = { name?: string, email?: string, message?: string };

/** The fragment, marked up as Shadleaf renders a form with <sl:form-errors autofocus/> and two fields. */
function contact(values: Values, submitted: boolean, autofocus = true): string {
  const name = values.name ?? '';
  const email = values.email ?? '';
  const message = values.message ?? '';
  const errors: Record<string, string> = {};
  if (submitted && !name) errors.name = 'must not be blank';
  if (submitted && !email.includes('@')) errors.email = 'must be a well-formed email address';
  const global = submitted && message.includes('https://') ? 'We do not accept messages with links.' : null;
  if (submitted && !global && Object.keys(errors).length === 0) {
    return `<div id="contact"><div class="alert" tabindex="-1" autofocus>Message sent to ${email}.</div>
      <a href="/other">Somewhere else</a></div>`;
  }
  let focusTaken = global !== null || !autofocus;
  const control = (id: string, value: string) => {
    const invalid = id in errors;
    const autofocus = invalid && !focusTaken;
    focusTaken ||= autofocus;
    return `<label for="${id}">${id}</label>
      <input id="${id}" name="${id}" value="${value}"${invalid ? ` aria-invalid="true" aria-describedby="${id}-error"` : ''}${autofocus ? ' autofocus' : ''}>
      ${invalid ? `<div class="field-error" id="${id}-error">${errors[id]}</div>` : ''}`;
  };
  return `<div id="contact">
    <form action="/" method="post" novalidate hx-post="/" hx-target="#contact" hx-swap="outerHTML">
      ${global ? `<div class="form-errors" id="contactForm-errors" tabindex="-1" autofocus>${global}</div>` : ''}
      ${control('name', name)}
      ${control('email', email)}
      <label for="message">message</label><textarea id="message" name="message">${message}</textarea>
      <button type="submit">Send</button>
    </form></div>`;
}

async function open(page: Page, {htmx = true, autofocus = true} = {}): Promise<string[]> {
  const messages: string[] = [];
  page.on('console', message => {
    if (message.type() === 'error' || message.type() === 'warning') {
      messages.push(message.text());
    }
  });
  page.on('pageerror', error => messages.push(error.message));
  await page.addInitScript(() => document.addEventListener('securitypolicyviolation',
      event => console.error(`CSP violation: ${event.violatedDirective}`)));
  await page.route(`${ORIGIN}/**`, route => {
    const request = route.request();
    const path = new URL(request.url()).pathname;
    const html = (body: string) => route.fulfill({
      contentType: 'text/html',
      headers: {'Content-Security-Policy': STRICT_CSP},
      body,
    });
    const script = htmx ? `${HTMX_CONFIG}<script defer src="/webjars/htmx.min.js"></script>` : '';
    const page = (body: string) => html(`<!doctype html><html lang="en"><head><title>Fixture</title>${script}`
        + `</head><body><main>${body}</main></body></html>`);
    if (path === '/webjars/htmx.min.js') {
      return route.fulfill({contentType: 'text/javascript', path: HTMX});
    }
    if (path === '/' && request.method() === 'GET') {
      return page(contact({}, false));
    }
    if (path === '/' && request.method() === 'POST') {
      const values = Object.fromEntries(new URLSearchParams(request.postData() ?? '')) as Values;
      const fragment = contact(values, true, autofocus);
      return request.headers()['hx-request'] === 'true' ? html(fragment) : page(fragment);
    }
    if (path === '/other') {
      return page('<p>Another page</p>');
    }
    return route.fulfill({status: 404});
  });
  await page.goto(`${ORIGIN}/`);
  if (htmx) {
    await page.waitForFunction(() => 'htmx' in window);
  }
  return messages;
}

/** Waits for the swap: htmx focuses [autofocus] when the new content settles, after the swap itself. */
async function submitted(page: Page, action: () => Promise<void>) {
  const settled = page.evaluate(() => new Promise(resolve =>
      document.body.addEventListener('htmx:afterSettle', resolve, {once: true})));
  await action();
  await settled;
}

test('a failed submit moves focus to the first field with an error', async ({page}) => {
  const messages = await open(page);

  await submitted(page, () => page.getByRole('button', {name: 'Send'}).click());

  await expect(page.locator('#name')).toBeFocused();
  await expect(page.locator('#name')).toHaveAttribute('aria-invalid', 'true');
  expect(messages, 'htmx runs under the strict policy with this config').toEqual([]);
});

test('Enter in a valid field still ends on the first field with an error', async ({page}) => {
  await open(page);

  // htmx restores focus to the element with the same id after a swap (#name); autofocus comes later and wins.
  await page.locator('#name').fill('Wim');
  await submitted(page, () => page.locator('#name').press('Enter'));

  await expect(page.locator('#email')).toBeFocused();
  await expect(page.locator('#name')).toHaveValue('Wim');
});

test('without autofocus, htmx leaves focus on the field Enter was pressed in, so the test above proves something',
    async ({page}) => {
      await open(page, {autofocus: false});

      await page.locator('#name').fill('Wim');
      await submitted(page, () => page.locator('#name').press('Enter'));

      await expect(page.locator('#name')).toBeFocused();
    });

test('a global error moves focus to the summary', async ({page}) => {
  await open(page);

  await page.locator('#name').fill('Wim');
  await page.locator('#email').fill('wim@example.com');
  await page.locator('#message').fill('See https://example.com');
  await submitted(page, () => page.getByRole('button', {name: 'Send'}).click());

  await expect(page.locator('#contactForm-errors')).toBeFocused();
});

test('a valid submit moves focus to the confirmation, and leaves history alone', async ({page}) => {
  await open(page);
  const historyLength = await page.evaluate(() => history.length);

  await page.locator('#name').fill('Wim');
  await page.locator('#email').fill('wim@example.com');
  await submitted(page, () => page.getByRole('button', {name: 'Send'}).click());

  await expect(page.locator('.alert')).toBeFocused();
  await expect(page.locator('.alert')).toHaveText('Message sent to wim@example.com.');
  expect(page.url()).toBe(`${ORIGIN}/`);
  expect(await page.evaluate(() => history.length), 'hx-post pushes no history entry').toBe(historyLength);
  expect(await page.evaluate(() => localStorage.getItem('htmx-history-cache')),
      'so htmx keeps no snapshot of the page, with the form values in it').toBeNull();

  // Back from another page shows the page again, as the browser kept it or as the server renders it, never an
  // htmx snapshot.
  await page.getByRole('link', {name: 'Somewhere else'}).click();
  await expect(page.getByText('Another page')).toBeVisible();
  await page.goBack();
  await expect(page.locator('#contact')).toBeVisible();
});

test('without htmx the same form posts and gets the whole page back', async ({page}) => {
  await open(page, {htmx: false});

  await page.getByRole('button', {name: 'Send'}).click();

  await expect(page).toHaveTitle('Fixture');
  await expect(page.locator('#name')).toHaveAttribute('aria-invalid', 'true');
  await expect(page.locator('#name')).toBeFocused();
});
