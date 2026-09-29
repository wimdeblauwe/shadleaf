import {expect, test, type Page} from '@playwright/test';
import {join} from 'node:path';
import previews from '../src/generated/previews.json' with {type: 'json'};

// The three shadleaf.assets.alpine scripts, in pages served from a made-up origin through page.route, so each test
// controls the script order and the Content-Security-Policy header. The application's own Alpine is the standard
// CDN build from node_modules; its component is fixtures/app-components.js.

type Variant = 'bundled' | 'csp' | 'external';

const ORIGIN = 'http://app.test';
const PUBLIC_DIR = join(import.meta.dirname, '..', 'public');
const FIXTURES_DIR = join(import.meta.dirname, 'fixtures');
const ALPINE_CDN_BUILD = join(import.meta.dirname, '..', 'node_modules', 'alpinejs', 'dist', 'cdn.min.js');

const STRICT_CSP = "default-src 'self'; script-src 'self'; style-src 'self'; object-src 'none'; base-uri 'self'";
// What the standard build needs, and all it needs beyond the strict policy.
const EVAL_CSP = STRICT_CSP.replace("script-src 'self'", "script-src 'self' 'unsafe-eval'");

const scripts = (previews as { scripts?: Partial<Record<Variant, string>> }).scripts ?? {};

const tag = {
  shadleaf: (variant: Variant) => `<script type="module" src="/${scripts[variant]}"></script>`,
  appComponents: '<script type="module" src="/app/components.js"></script>',
  appAlpine: '<script defer src="/app/alpine.js"></script>',
};

const DISCLOSURE = `
  <div x-data="testDisclosure">
    <button type="button" id="trigger" @click="toggle" :aria-expanded="expanded">Details</button>
    <p id="panel" x-show="open">Panel</p>
  </div>`;

test.beforeAll(() => {
  expect(Object.keys(scripts), 'no scripts in previews.json: run `pnpm run generate` first')
      .toEqual(['bundled', 'csp', 'external']);
});

/** Serves a page with the given script tags in <head> and the disclosure in <body>; returns what the console said. */
async function open(page: Page, headScripts: string[], csp: string): Promise<string[]> {
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
    const path = new URL(route.request().url()).pathname;
    const javascript = {contentType: 'text/javascript'};
    if (path === '/') {
      return route.fulfill({
        contentType: 'text/html',
        headers: {'Content-Security-Policy': csp},
        body: `<!doctype html><html lang="en"><head><title>Fixture</title>${headScripts.join('')}</head>`
            + `<body>${DISCLOSURE}</body></html>`,
      });
    }
    if (path === '/app/components.js') {
      return route.fulfill({...javascript, path: join(FIXTURES_DIR, 'app-components.js')});
    }
    if (path === '/app/alpine.js') {
      return route.fulfill({...javascript, path: ALPINE_CDN_BUILD});
    }
    if (path.startsWith('/shadleaf/')) {
      return route.fulfill({...javascript, path: join(PUBLIC_DIR, path)});
    }
    return route.fulfill({status: 404});
  });
  await page.goto(`${ORIGIN}/`);
  return messages;
}

/** The disclosure works with the keyboard alone, and focus stays on its trigger. */
async function expectDisclosureWorks(page: Page) {
  const trigger = page.locator('#trigger');
  const panel = page.locator('#panel');
  await expect(trigger).toHaveAttribute('aria-expanded', 'false');
  await expect(panel).toBeHidden();

  await page.keyboard.press('Tab');
  await expect(trigger).toBeFocused();
  await page.keyboard.press('Enter');
  await expect(trigger).toHaveAttribute('aria-expanded', 'true');
  await expect(panel).toBeVisible();
  await page.keyboard.press('Space');
  await expect(trigger).toHaveAttribute('aria-expanded', 'false');
  await expect(panel).toBeHidden();
  await expect(trigger).toBeFocused();

  expect(await page.evaluate(() => (window as any).testDisclosureInits), 'initialisations').toBe(1);
}

test('bundled: starts the standard build, with the application\'s component registered before it', async ({page}) => {
  const messages = await open(page, [tag.appComponents, tag.shadleaf('bundled')], EVAL_CSP);

  await expectDisclosureWorks(page);
  expect(messages).toEqual([]);
});

test('csp: works under a policy without unsafe-eval', async ({page}) => {
  const messages = await open(page, [tag.appComponents, tag.shadleaf('csp')], STRICT_CSP);

  await expectDisclosureWorks(page);
  expect(messages).toEqual([]);
});

test('bundled under a policy without unsafe-eval fails, so the csp test proves something', async ({page}) => {
  const messages = await open(page, [tag.appComponents, tag.shadleaf('bundled')], STRICT_CSP);

  await expect.poll(() => messages.join('\n')).toContain('unsafe-eval');
});

test('external: registers with the application\'s Alpine when the assets come first', async ({page}) => {
  const messages = await open(page, [tag.appComponents, tag.shadleaf('external'), tag.appAlpine], EVAL_CSP);

  await expectDisclosureWorks(page);
  expect(messages).toEqual([]);
});

test('external: says so when it runs after the application\'s Alpine started', async ({page}) => {
  const messages = await open(page, [tag.appComponents, tag.appAlpine, tag.shadleaf('external')], EVAL_CSP);

  await expect.poll(() => messages.join('\n')).toContain('shadleaf.js ran after Alpine started');
});

test('bundled: does not start a second Alpine when the page already has one', async ({page}) => {
  const messages = await open(page, [tag.appComponents, tag.appAlpine, tag.shadleaf('bundled')], EVAL_CSP);

  await expect.poll(() => messages.join('\n')).toContain('this page already loads Alpine');
  // The application's Alpine runs the component, exactly once.
  await expectDisclosureWorks(page);
});

test('the showcase loads the bundled script and starts Alpine', async ({page}) => {
  const messages: string[] = [];
  page.on('console', message => message.type() === 'error' && messages.push(message.text()));
  await page.goto('/showcase/');

  await page.waitForFunction(() => (window as any).Alpine !== undefined);
  expect(messages).toEqual([]);
});
