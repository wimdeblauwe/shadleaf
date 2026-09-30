import {expect, test, type Locator, type Page} from '@playwright/test';
import AxeBuilder from '@axe-core/playwright';
import {join} from 'node:path';
import previews from '../src/generated/previews.json' with {type: 'json'};
import {combinations, openShowcase} from './showcase';

// sl:dialog. On the showcase, in every skin and theme: every dialog opens from its trigger with the keyboard, takes
// focus, passes axe open, shows focus on every control in it, and Escape closes it with focus back on the trigger.
// In fixture pages served from a made-up origin under a strict Content-Security-Policy with the csp Alpine build: the
// slDialog component (open on load, the sl-dialog-close event, htmx swaps and history) and its stand-ins for invoker
// commands and closedby in browsers without them.

const TAGS = ['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa', 'wcag22aa', 'best-practice'];

/** Waits until the opening fade is over: axe reads a half-transparent dialog's colours as they are mid-way. */
async function waitUntilOpen(dialog: Locator) {
  await expect(dialog).toHaveJSProperty('open', true);
  await expect.poll(() => dialog.evaluate(element => getComputedStyle(element).opacity)).toBe('1');
}

for (const {skin, theme} of combinations) {
  test(`every dialog works with the keyboard and passes axe open: ${skin}, ${theme}`, async ({page}) => {
    await openShowcase(page, skin, theme);
    await page.waitForFunction(() => 'Alpine' in window);
    const ids = await page.locator('main dialog.dialog').evaluateAll(dialogs => dialogs.map(dialog => dialog.id));
    expect(ids.length).toBeGreaterThan(0);

    const failures: string[] = [];
    for (const id of ids) {
      const dialog = page.locator(`#${id}`);
      const trigger = page.locator(`main button[commandfor="${id}"][command="show-modal"]`);
      await trigger.focus();
      await page.keyboard.press('Enter');
      await waitUntilOpen(dialog);
      expect(await dialog.evaluate(element => element.matches(':modal')), `${id} is modal`).toBe(true);
      expect(await dialog.evaluate(element => element.contains(document.activeElement)), `${id} takes focus`)
          .toBe(true);

      const results = await new AxeBuilder({page}).include(`#${id}`).withTags(TAGS).analyze();
      failures.push(...results.violations.map(violation => `${id}: ${violation.id}: ${violation.help}\n    ${
          violation.nodes.map(node => node.target.join(' ')).join('\n    ')}`));

      // Tab through the dialog: focus stays in it and is visible on every control.
      const count = await dialog.evaluate(element => element.querySelectorAll(
          ':is(a[href], button, input, select, textarea, [tabindex]):not(:disabled, [tabindex="-1"])').length);
      await dialog.evaluate(element => (element.querySelector<HTMLElement>(
          ':is(a[href], button, input, select, textarea, [tabindex]):not(:disabled, [tabindex="-1"])'))?.focus());
      for (let i = 0; i < count; i++) {
        const focused = await page.evaluate(() => {
          const element = document.activeElement as HTMLElement;
          const style = getComputedStyle(element);
          return {
            inside: !!element.closest('dialog[open]'),
            label: element.outerHTML.slice(0, 100),
            visible: element.matches(':focus-visible')
                && ((style.outlineStyle !== 'none' && parseFloat(style.outlineWidth) > 0) || style.boxShadow !== 'none'),
          };
        });
        expect(focused.inside, `focus stays in ${id}`).toBe(true);
        if (!focused.visible) {
          failures.push(`${id}: no visible focus on ${focused.label}`);
        }
        await page.keyboard.press('Tab');
      }

      await page.keyboard.press('Escape');
      await expect(dialog).toHaveJSProperty('open', false);
      await expect(trigger).toBeFocused();
    }
    expect(failures, failures.join('\n')).toEqual([]);
  });
}

test('a click outside, the close button and a close command close a dialog', async ({page}) => {
  await openShowcase(page, 'vega', 'light');
  await page.waitForFunction(() => 'Alpine' in window);
  const dialog = page.locator('#edit-profile');
  const trigger = page.locator('main button[commandfor="edit-profile"][command="show-modal"]');

  await trigger.click();
  await waitUntilOpen(dialog);
  await dialog.click({position: {x: 10, y: 10}});
  await expect(dialog, 'a click inside keeps it open').toHaveJSProperty('open', true);
  await page.mouse.click(5, 5);
  await expect(dialog).toHaveJSProperty('open', false);

  await trigger.click();
  await waitUntilOpen(dialog);
  await dialog.getByRole('button', {name: 'Close'}).click();
  await expect(dialog).toHaveJSProperty('open', false);

  await trigger.click();
  await waitUntilOpen(dialog);
  await dialog.getByRole('button', {name: 'Cancel'}).click();
  await expect(dialog).toHaveJSProperty('open', false);
});

// --- fixture pages ---------------------------------------------------------------------------------------------------

const ORIGIN = 'http://app.test';
const PUBLIC_DIR = join(import.meta.dirname, '..', 'public');
const HTMX = join(import.meta.dirname, '..', 'node_modules', 'htmx.org', 'dist', 'htmx.min.js');
const STRICT_CSP = "default-src 'self'; script-src 'self'; style-src 'self'; object-src 'none'; base-uri 'self'";
const HTMX_CONFIG = `<meta name="htmx-config" content='{"includeIndicatorStyles": false, "allowEval": false}'>`;

type Scripts = { bundled: string, csp: string, external: string };
const scripts = (previews as unknown as { scripts: Scripts }).scripts;
const css = previews.skins[0].css;

/** A dialog marked up as <sl:dialog> renders it (see dialog.approved.html). */
function dialog(id: string, content: string, {open = false} = {}): string {
  return `<dialog class="dialog" id="${id}" closedby="any" x-data="slDialog" aria-labelledby="${id}-title"${
      open ? ' data-show-modal="true"' : ''}>
    <div class="dialog-header"><div class="dialog-title" role="heading" aria-level="2" id="${id}-title">Title of ${id}</div></div>
    ${content}
    <button class="btn dialog-close" type="button" data-variant="ghost" data-size="icon-sm" commandfor="${id}"
            command="close" aria-label="Close">x</button>
  </dialog>`;
}

type Routes = Record<string, (body: string, headers: Record<string, string>) => { body: string, headers?: Record<string, string> }>;

/** Serves `body` at / with the csp build (and htmx), and `routes` for htmx requests; returns what the console said. */
async function openFixture(page: Page, body: string, {htmx = false, routes = {} as Routes} = {}): Promise<string[]> {
  const messages: string[] = [];
  page.on('console', message => {
    if (message.type() === 'error' || message.type() === 'warning') {
      messages.push(message.text());
    }
  });
  page.on('pageerror', error => messages.push(error.message));
  await page.addInitScript(() => document.addEventListener('securitypolicyviolation',
      event => console.error(`CSP violation: ${event.violatedDirective}`)));
  const document = (main: string) => `<!doctype html><html lang="en"><head><title>Fixture</title>`
      + `<link rel="stylesheet" href="/${css}">`
      + (htmx ? `${HTMX_CONFIG}<script defer src="/webjars/htmx.min.js"></script>` : '')
      + `<script type="module" src="/${scripts.csp}"></script></head><body><main>${main}</main></body></html>`;
  await page.route(`${ORIGIN}/**`, route => {
    const request = route.request();
    const path = new URL(request.url()).pathname;
    if (path === '/webjars/htmx.min.js') {
      return route.fulfill({contentType: 'text/javascript', path: HTMX});
    }
    if (path.startsWith('/shadleaf/')) {
      return route.fulfill({path: join(PUBLIC_DIR, path)});
    }
    const headers = {'Content-Security-Policy': STRICT_CSP};
    if (path === '/') {
      return route.fulfill({contentType: 'text/html', headers, body: document(body)});
    }
    const handler = routes[path];
    if (handler) {
      const response = handler(request.postData() ?? '', request.headers());
      const html = request.headers()['hx-request'] === 'true' ? response.body : document(response.body);
      return route.fulfill({contentType: 'text/html', headers: {...headers, ...response.headers}, body: html});
    }
    return route.fulfill({status: 404});
  });
  await page.goto(`${ORIGIN}/`);
  await page.waitForFunction(() => 'Alpine' in window);
  if (htmx) {
    await page.waitForFunction(() => 'htmx' in window);
  }
  return messages;
}

const TRIGGER = '<button type="button" id="trigger" commandfor="d" command="show-modal">Open</button>';

test('the csp build opens and closes a dialog under a strict policy', async ({page}) => {
  const messages = await openFixture(page, TRIGGER + dialog('d', '<p><a href="/elsewhere">A link</a></p>'));
  const d = page.locator('#d');

  await page.locator('#trigger').click();
  await waitUntilOpen(d);
  await page.keyboard.press('Escape');
  await expect(d).toHaveJSProperty('open', false);
  await expect(page.locator('#trigger')).toBeFocused();
  expect(messages).toEqual([]);
});

test('open shows the dialog as a modal once Alpine starts', async ({page}) => {
  const messages = await openFixture(page, dialog('d', '<p>Shown on load</p>', {open: true}));
  const d = page.locator('#d');

  await waitUntilOpen(d);
  expect(await d.evaluate(element => element.matches(':modal'))).toBe(true);
  expect(messages).toEqual([]);
});

test('the sl-dialog-close event closes the dialog it comes from, not an outer one', async ({page}) => {
  await openFixture(page, TRIGGER + dialog('d', `
      <button type="button" id="open-inner" commandfor="inner" command="show-modal">Inner</button>
      ${dialog('inner', '<p id="inside">Inner content</p>')}`));

  await page.locator('#trigger').click();
  await page.locator('#open-inner').click();
  await waitUntilOpen(page.locator('#inner'));
  await page.locator('#inside').dispatchEvent('sl-dialog-close', {bubbles: true});
  await expect(page.locator('#inner')).toHaveJSProperty('open', false);
  await expect(page.locator('#d')).toHaveJSProperty('open', true);
});

test('without invoker commands and closedby, the component stands in for them', async ({page}) => {
  await page.addInitScript(() => {
    delete (HTMLButtonElement.prototype as unknown as Record<string, unknown>).commandForElement;
    delete (HTMLDialogElement.prototype as unknown as Record<string, unknown>).closedBy;
    // What such a browser does with commandfor: nothing. (Chromium's own closedby stays: a click outside is checked
    // with both closing it, but the dragged selection only with the stand-in's start check.)
    document.addEventListener('command', event => event.preventDefault(), true);
  });
  await openFixture(page, TRIGGER + dialog('d', '<p id="text">Some text to select</p>'));
  const d = page.locator('#d');
  const trigger = page.locator('#trigger');
  expect(await page.evaluate(() => 'commandForElement' in HTMLButtonElement.prototype)).toBe(false);

  await trigger.click();
  await waitUntilOpen(d);
  await d.getByRole('button', {name: 'Close'}).click();
  await expect(d).toHaveJSProperty('open', false);

  await trigger.click();
  await waitUntilOpen(d);
  // A selection dragged from inside to outside keeps it open.
  const box = (await page.locator('#text').boundingBox())!;
  await page.mouse.move(box.x + 2, box.y + box.height / 2);
  await page.mouse.down();
  await page.mouse.move(5, 5);
  await page.mouse.up();
  await expect(d).toHaveJSProperty('open', true);
  await page.mouse.click(5, 5);
  await expect(d).toHaveJSProperty('open', false);
});

test('htmx: a dialog the server renders with open opens, and HX-Trigger sl-dialog-close closes it', async ({page}) => {
  let saved = '';
  const messages = await openFixture(page, `
      <button type="button" id="load" hx-get="/dialog" hx-target="#modal-root">Edit</button>
      <p id="saved">Nothing saved</p>
      <div id="modal-root"></div>`, {
    htmx: true,
    routes: {
      '/dialog': () => ({
        body: dialog('edit', `<form hx-post="/save" hx-target="#saved" hx-swap="outerHTML">
          <label for="name">Name</label><input id="name" name="name">
          <button type="submit">Save</button></form>`, {open: true}),
      }),
      '/save': body => {
        saved = new URLSearchParams(body).get('name') ?? '';
        return {body: `<p id="saved">Saved ${saved}</p>`, headers: {'HX-Trigger': 'sl-dialog-close'}};
      },
    },
  });
  const edit = page.locator('#edit');

  await page.locator('#load').click();
  await waitUntilOpen(edit);
  await expect(page.locator('#name')).toBeFocused();
  await page.locator('#name').fill('Ada');
  await page.keyboard.press('Enter');
  await expect(edit).toHaveJSProperty('open', false);
  await expect(page.locator('#saved')).toHaveText('Saved Ada');
  await expect(page.locator('#load')).toBeFocused();
  expect(messages).toEqual([]);
});

test('htmx: a swap that removes an open dialog leaves no inert page behind', async ({page}) => {
  await openFixture(page, `
      ${TRIGGER}
      <button type="button" id="after" hx-get="/clicked" hx-target="this" hx-swap="outerHTML">Page button</button>
      <div id="region">${dialog('d', `<button type="button" id="replace" hx-get="/region" hx-target="#region"
          hx-swap="innerHTML">Replace</button>`)}</div>`, {
    htmx: true,
    routes: {
      '/region': () => ({body: '<p>Replaced</p>'}),
      '/clicked': () => ({body: '<p id="clicked">Clicked</p>'}),
    },
  });

  await page.locator('#trigger').click();
  await waitUntilOpen(page.locator('#d'));
  await page.locator('#replace').click();
  await expect(page.locator('#region')).toHaveText('Replaced');
  expect(await page.evaluate(() => document.querySelector(':modal'))).toBeNull();
  await page.locator('#after').click();
  await expect(page.locator('#clicked')).toBeVisible();
});

test('htmx: history never restores a dialog open', async ({page}) => {
  await openFixture(page, `${TRIGGER}${dialog('d', `
      <a href="/next" id="next" hx-get="/next" hx-target="main" hx-push-url="true">Next page</a>`)}`, {
    htmx: true,
    routes: {'/next': () => ({body: '<p id="next-page">Next page</p>'})},
  });

  await page.locator('#trigger').click();
  await waitUntilOpen(page.locator('#d'));
  await page.locator('#next').click();
  await expect(page.locator('#next-page')).toBeVisible();
  await page.goBack();
  await expect(page.locator('#trigger')).toBeVisible();
  expect(await page.evaluate(() => document.querySelector('dialog[open]'))).toBeNull();
});
