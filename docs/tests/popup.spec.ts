import {expect, test, type Locator, type Page} from '@playwright/test';
import AxeBuilder from '@axe-core/playwright';
import {join} from 'node:path';
import previews from '../src/generated/previews.json' with {type: 'json'};
import {combinations, openShowcase} from './showcase';

// sl:dropdown-menu, sl:popover and sl:tooltip, all popovers placed with CSS anchor positioning. On the showcase, in
// every skin and theme: every menu and popover opens from its trigger with the keyboard, takes focus, passes axe open,
// shows focus on every item or control, and Escape closes it with focus back on the trigger; every tooltip shows on
// keyboard focus, passes axe and hides on Escape. Then the keyboard, pointer and placement details. In fixture pages
// served from a made-up origin under a strict Content-Security-Policy with the csp Alpine build: each component, the
// stand-in for anchor positioning, menu items as links, buttons, submit buttons and dialog openers, and htmx (items
// with hx-*, a swap replacing an open menu, history, a popover closed by HX-Trigger).

const TAGS = ['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa', 'wcag22aa', 'best-practice'];
const FOCUSABLE = ':is(a[href], button, input, select, textarea, [tabindex]):not(:disabled, [tabindex="-1"])';

/** Waits until the opening fade is over: axe reads a half-transparent popup's colours as they are mid-way. */
async function waitUntilOpen(popup: Locator) {
  await expect.poll(() => popup.evaluate(element => element.matches(':popover-open'))).toBe(true);
  await expect.poll(() => popup.evaluate(element => getComputedStyle(element).opacity)).toBe('1');
}

async function expectClosed(popup: Locator) {
  await expect.poll(() => popup.evaluate(element => element.matches(':popover-open'))).toBe(false);
}

/** What the focused element shows: whether it is inside `container`, and whether its focus is visible. */
function focusState(page: Page, containerId: string) {
  return page.evaluate(id => {
    const element = document.activeElement as HTMLElement;
    const style = getComputedStyle(element);
    return {
      inside: !!element.closest(`#${id}`),
      label: element.outerHTML.slice(0, 100),
      visible: element.matches(':focus-visible')
          && ((style.outlineStyle !== 'none' && parseFloat(style.outlineWidth) > 0) || style.boxShadow !== 'none'),
    };
  }, containerId);
}

function axeFailures(id: string, violations: Awaited<ReturnType<AxeBuilder['analyze']>>['violations']) {
  return violations.map(violation => `${id}: ${violation.id}: ${violation.help}\n    ${
      violation.nodes.map(node => node.target.join(' ')).join('\n    ')}`);
}

for (const {skin, theme} of combinations) {
  test(`every dropdown menu works with the keyboard and passes axe open: ${skin}, ${theme}`, async ({page}) => {
    await openShowcase(page, skin, theme);
    await page.waitForFunction(() => 'Alpine' in window);
    const ids = await page.locator('main .dropdown-menu-content').evaluateAll(menus => menus.map(menu => menu.id));
    expect(ids.length, 'the showcase has dropdown menus').toBeGreaterThan(0);

    const failures: string[] = [];
    for (const id of ids) {
      const menu = page.locator(`#${id}`);
      const trigger = page.locator(`#${id}-trigger`);
      await trigger.focus();
      await page.keyboard.press('Enter');
      await waitUntilOpen(menu);
      await expect(trigger).toHaveAttribute('aria-expanded', 'true');
      const first = menu.locator('[role^="menuitem"]:not(:disabled, [aria-disabled="true"])').first();
      await expect(first, `${id} focuses its first item`).toBeFocused();

      const results = await new AxeBuilder({page}).include(`#${id}`).withTags(TAGS).analyze();
      failures.push(...axeFailures(id, results.violations));

      // Arrow down through every item: focus stays in the menu and is visible on each.
      const count = await menu.locator('[role^="menuitem"]:not(:disabled, [aria-disabled="true"])').count();
      for (let i = 0; i < count; i++) {
        const focused = await focusState(page, id);
        expect(focused.inside, `focus stays in ${id}`).toBe(true);
        if (!focused.visible) {
          failures.push(`${id}: no visible focus on ${focused.label}`);
        }
        await page.keyboard.press('ArrowDown');
      }

      await page.keyboard.press('Escape');
      await expectClosed(menu);
      await expect(trigger).toBeFocused();
      await expect(trigger).toHaveAttribute('aria-expanded', 'false');
    }
    expect(failures, failures.join('\n')).toEqual([]);
  });

  test(`every popover works with the keyboard and passes axe open: ${skin}, ${theme}`, async ({page}) => {
    await openShowcase(page, skin, theme);
    await page.waitForFunction(() => 'Alpine' in window);
    const ids = await page.locator('main .popover-content').evaluateAll(popovers => popovers.map(popover => popover.id));
    expect(ids.length, 'the showcase has popovers').toBeGreaterThan(0);

    const failures: string[] = [];
    for (const id of ids) {
      const popover = page.locator(`#${id}`);
      const trigger = page.locator(`main button[popovertarget="${id}"]`);
      await trigger.focus();
      await page.keyboard.press('Enter');
      await waitUntilOpen(popover);
      await expect(trigger).toHaveAttribute('aria-expanded', 'true');
      expect(await popover.evaluate(element => element.contains(document.activeElement)), `${id} takes focus`)
          .toBe(true);

      const results = await new AxeBuilder({page}).include(`#${id}`).withTags(TAGS).analyze();
      failures.push(...axeFailures(id, results.violations));

      const count = await popover.locator(FOCUSABLE).count();
      for (let i = 0; i < Math.max(count, 1); i++) {
        const focused = await focusState(page, id);
        expect(focused.inside, `focus is in ${id}`).toBe(true);
        if (!focused.visible) {
          failures.push(`${id}: no visible focus on ${focused.label}`);
        }
        if (i < count - 1) {
          await page.keyboard.press('Tab');
        }
      }

      await page.keyboard.press('Escape');
      await expectClosed(popover);
      await expect(trigger).toBeFocused();
      await expect(trigger).toHaveAttribute('aria-expanded', 'false');
    }
    expect(failures, failures.join('\n')).toEqual([]);
  });

  test(`every tooltip shows on keyboard focus and passes axe: ${skin}, ${theme}`, async ({page}) => {
    await openShowcase(page, skin, theme);
    await page.waitForFunction(() => 'Alpine' in window);
    const tooltips = page.locator('main .tooltip');
    const count = await tooltips.count();
    expect(count, 'the showcase has tooltips').toBeGreaterThan(0);

    const failures: string[] = [];
    for (let i = 0; i < count; i++) {
      const trigger = tooltips.nth(i).locator('> :not(.tooltip-content)').first();
      const content = tooltips.nth(i).locator('> .tooltip-content');
      const contentId = await content.evaluate(element => element.id);
      await expect(trigger).toHaveAttribute('aria-describedby', new RegExp(`\\b${contentId}\\b`));
      await trigger.focus();
      await waitUntilOpen(content);
      const results = await new AxeBuilder({page}).include(`#${contentId}`).withTags(TAGS).analyze();
      failures.push(...axeFailures(contentId, results.violations));

      await page.keyboard.press('Escape');
      await expectClosed(content);
      await expect(trigger).toBeFocused();
      await trigger.blur();
    }
    expect(failures, failures.join('\n')).toEqual([]);
  });
}

test('menu keyboard: arrows, Home and End, disabled items skipped, typeahead, Tab moves on', async ({page}) => {
  await openShowcase(page, 'vega', 'light');
  await page.waitForFunction(() => 'Alpine' in window);
  const menu = page.locator('#account-menu');
  const trigger = page.locator('#account-menu-trigger');
  const focusedText = () => page.evaluate(() => document.activeElement!.childNodes[0].textContent!.trim());

  await trigger.focus();
  await page.keyboard.press('ArrowDown');
  await waitUntilOpen(menu);
  await expect.poll(focusedText).toBe('Profile');
  await page.keyboard.press('ArrowUp');
  await expect.poll(focusedText, 'no wrapping at the top').toBe('Profile');
  await page.keyboard.press('End');
  await expect.poll(focusedText).toBe('Log out');
  await page.keyboard.press('ArrowUp');
  await expect.poll(focusedText, 'the disabled API item is skipped').toBe('Support');
  await page.keyboard.press('Home');
  await expect.poll(focusedText).toBe('Profile');
  await page.keyboard.press('s');
  await expect.poll(focusedText, 'typeahead, shortcuts ignored').toBe('Settings');
  await page.keyboard.press('s');
  await expect.poll(focusedText, 'the same letter again moves on').toBe('Support');
  await page.waitForTimeout(600);
  await page.keyboard.type('ne');
  await expect.poll(focusedText, 'typed letters build a prefix').toBe('New Team');

  await page.keyboard.press('Tab');
  await expectClosed(menu);
  expect(await page.evaluate(() => document.activeElement?.id), 'focus moves on past the trigger')
      .not.toBe('account-menu-trigger');
  expect(await page.evaluate(() => !!document.activeElement?.closest('#account-menu'))).toBe(false);

  await trigger.focus();
  await page.keyboard.press('ArrowUp');
  await waitUntilOpen(menu);
  await expect.poll(focusedText, 'arrow up opens on the last item').toBe('Log out');
  await page.keyboard.press('Escape');
  await expect(trigger).toBeFocused();
});

test('menu pointer: a click focuses the menu, the pointer moves the focus, choosing an item closes it', async ({page}) => {
  await openShowcase(page, 'vega', 'light');
  await page.waitForFunction(() => 'Alpine' in window);
  const menu = page.locator('#file-menu');
  const trigger = page.locator('#file-menu-trigger');

  await trigger.click();
  await waitUntilOpen(menu);
  await expect(menu, 'a pointer opens it with the focus on the menu itself').toBeFocused();
  await menu.getByRole('menuitem', {name: 'Share'}).hover();
  await expect(menu.getByRole('menuitem', {name: 'Share'})).toBeFocused();
  expect(await menu.getByRole('menuitem', {name: 'Share'}).evaluate(element => element.matches(':focus-visible')),
      'no focus ring for the pointer').toBe(false);
  await menu.getByRole('menuitem', {name: 'Share'}).click();
  await expectClosed(menu);
  await expect(trigger).toBeFocused();
  await expect(trigger).toHaveAttribute('aria-expanded', 'false');

  await trigger.click();
  await waitUntilOpen(menu);
  await page.mouse.click(600, 10);
  await expectClosed(menu);

  await trigger.click();
  await waitUntilOpen(menu);
  await trigger.click();
  await expectClosed(menu);
});

test('menu keyboard: Space chooses a link item, Enter a button item', async ({page}) => {
  await openShowcase(page, 'vega', 'light');
  await page.waitForFunction(() => 'Alpine' in window);
  // Keeps the showcase in place: count the clicks instead.
  await page.evaluate(() => {
    (window as unknown as {clicks: string[]}).clicks = [];
    document.addEventListener('click', event => {
      const item = (event.target as Element).closest('[role^="menuitem"]');
      if (item) {
        event.preventDefault();
        (window as unknown as {clicks: string[]}).clicks.push(item.textContent!.trim());
      }
    });
  });
  const clicks = () => page.evaluate(() => (window as unknown as {clicks: string[]}).clicks);
  const menu = page.locator('#position-menu');

  await page.locator('#position-menu-trigger').focus();
  await page.keyboard.press('Enter');
  await waitUntilOpen(menu);
  await page.keyboard.press(' ');
  await expectClosed(menu);
  expect(await clicks()).toEqual(['Top']);

  await page.locator('#file-menu-trigger').focus();
  await page.keyboard.press('Enter');
  await waitUntilOpen(page.locator('#file-menu'));
  await page.keyboard.press('Enter');
  await expectClosed(page.locator('#file-menu'));
  expect(await clicks()).toEqual(['Top', 'New file']);
});

test('placement: a menu opens below its trigger, lined up with its start, and flips when there is no room', async ({page}) => {
  await page.setViewportSize({width: 1000, height: 700});
  await openShowcase(page, 'vega', 'light');
  await page.waitForFunction(() => 'Alpine' in window);
  const menu = page.locator('#file-menu');
  const trigger = page.locator('#file-menu-trigger');

  await trigger.evaluate(element => element.scrollIntoView({block: 'start'}));
  await trigger.click();
  await waitUntilOpen(menu);
  let t = (await trigger.boundingBox())!;
  let m = (await menu.boundingBox())!;
  expect(Math.round(m.y - (t.y + t.height)), 'the gap below the trigger').toBe(4);
  expect(Math.round(m.x)).toBe(Math.round(t.x));
  await page.keyboard.press('Escape');

  await trigger.evaluate(element => element.scrollIntoView({block: 'end'}));
  await trigger.click();
  await waitUntilOpen(menu);
  t = (await trigger.boundingBox())!;
  m = (await menu.boundingBox())!;
  expect(Math.round(t.y - (m.y + m.height)), 'flipped above the trigger').toBe(4);
  await page.keyboard.press('Escape');

  // align="end": the end edges line up, on the right, or on the left in right-to-left. Away from the viewport's edges,
  // where it would flip.
  const rowTrigger = page.locator('#order-1042-actions-trigger');
  const rowMenu = page.locator('#order-1042-actions');
  await page.locator('[data-scenario="dropdown-menu--row-actions"]').evaluate(stage => {
    (stage as HTMLElement).style.paddingInline = '400px';
  });
  await rowTrigger.evaluate(element => element.scrollIntoView({block: 'start'}));
  await rowTrigger.click();
  await waitUntilOpen(rowMenu);
  t = (await rowTrigger.boundingBox())!;
  m = (await rowMenu.boundingBox())!;
  expect(Math.round(m.x + m.width)).toBe(Math.round(t.x + t.width));
  await page.keyboard.press('Escape');

  await page.evaluate(() => document.documentElement.setAttribute('dir', 'rtl'));
  await rowTrigger.click();
  await waitUntilOpen(rowMenu);
  t = (await rowTrigger.boundingBox())!;
  m = (await rowMenu.boundingBox())!;
  expect(Math.round(m.x), 'the end is on the left in right-to-left').toBe(Math.round(t.x));
});

test('placement: sides and the tooltip arrow follow a flip', async ({page}) => {
  await page.setViewportSize({width: 1000, height: 700});
  await openShowcase(page, 'vega', 'light');
  await page.waitForFunction(() => 'Alpine' in window);

  const sides = {'popover-top': 'top', 'popover-right': 'right', 'popover-bottom': 'bottom', 'popover-left': 'left'};
  // Room on every side, so none of them flips.
  await page.locator('[data-scenario="popover--sides"]').evaluate(stage => {
    (stage as HTMLElement).style.paddingInline = '320px';
  });
  await page.locator('#popover-left').evaluate(element => element.previousElementSibling!.scrollIntoView({block: 'center'}));
  for (const [id, side] of Object.entries(sides)) {
    const trigger = page.locator(`main button[popovertarget="${id}"]`);
    await trigger.click();
    const popover = page.locator(`#${id}`);
    await waitUntilOpen(popover);
    const t = (await trigger.boundingBox())!;
    const p = (await popover.boundingBox())!;
    const placed = p.y + p.height <= t.y ? 'top' : p.y >= t.y + t.height ? 'bottom' : p.x >= t.x + t.width ? 'right' : 'left';
    expect(placed, id).toBe(side);
    await page.keyboard.press('Escape');
  }

  // A tooltip at the top of the viewport flips below its trigger, and its arrow with it. (Room below the page, so its
  // trigger can scroll to the top.)
  await page.evaluate(() => document.body.style.paddingBlockEnd = '100vh');
  const trigger = page.locator('[data-scenario="tooltip--default"] .tooltip > button');
  await trigger.evaluate(element => element.scrollIntoView({block: 'start'}));
  await trigger.focus();
  const content = page.locator('[data-scenario="tooltip--default"] .tooltip-content');
  await waitUntilOpen(content);
  await expect(content).toHaveAttribute('data-placed', 'bottom');
  const arrow = (await content.locator('.tooltip-arrow').boundingBox())!;
  const box = (await content.boundingBox())!;
  expect(arrow.y, 'the arrow points up at the trigger').toBeLessThan(box.y);
});

test('popover: focus moving out closes it; a click outside closes it', async ({page}) => {
  await openShowcase(page, 'vega', 'light');
  await page.waitForFunction(() => 'Alpine' in window);
  const popover = page.locator('#dimensions');
  const trigger = page.locator('main button[popovertarget="dimensions"]');

  await trigger.click();
  await waitUntilOpen(popover);
  await expect(page.locator('#dimensions-width')).toBeFocused();
  await page.keyboard.press('Tab');
  await expect(page.locator('#dimensions-height')).toBeFocused();
  await page.keyboard.press('Tab');
  await expectClosed(popover);
  await expect(trigger).toHaveAttribute('aria-expanded', 'false');

  await trigger.click();
  await waitUntilOpen(popover);
  await page.mouse.click(700, 10);
  await expectClosed(popover);
});

test('tooltip: shows after the delay on hover, stays while the pointer is on it, hides when it leaves', async ({page}) => {
  await openShowcase(page, 'vega', 'light');
  await page.waitForFunction(() => 'Alpine' in window);
  const trigger = page.locator('[data-scenario="tooltip--default"] .tooltip > button');
  const content = page.locator('[data-scenario="tooltip--default"] .tooltip-content');
  await trigger.evaluate(element => element.scrollIntoView({block: 'center'}));

  await trigger.hover();
  await page.waitForTimeout(150);
  expect(await content.evaluate(element => element.matches(':popover-open')), 'not before the delay').toBe(false);
  await waitUntilOpen(content);
  expect(await trigger.evaluate(element => element.matches(':focus')), 'hovering does not focus').toBe(false);

  // Onto the tooltip, across the gap: it stays.
  const box = (await content.boundingBox())!;
  await page.mouse.move(box.x + box.width / 2, box.y + box.height / 2, {steps: 5});
  await page.waitForTimeout(300);
  expect(await content.evaluate(element => element.matches(':popover-open')), 'the pointer can rest on it').toBe(true);
  await page.mouse.move(5, 5);
  await expectClosed(content);

  // Pressing the trigger hides it.
  await trigger.hover();
  await waitUntilOpen(content);
  await page.mouse.down();
  await expectClosed(content);
  await page.mouse.up();

  // Keyboard focus shows it at once; blur hides it. (The press focused the trigger, without showing it: a pointer's
  // focus is not :focus-visible.)
  await page.mouse.move(5, 5);
  expect(await content.evaluate(element => element.matches(':popover-open'))).toBe(false);
  await trigger.blur();
  await page.keyboard.press('Shift');
  await trigger.focus();
  await waitUntilOpen(content);
  await trigger.blur();
  await expectClosed(content);
});

test('tooltip: one at a time, and the next one shows at once', async ({page}) => {
  await openShowcase(page, 'vega', 'light');
  await page.waitForFunction(() => 'Alpine' in window);
  const buttons = page.locator('[data-scenario="tooltip--sides"] .tooltip > button');
  const contents = page.locator('[data-scenario="tooltip--sides"] .tooltip-content');
  await buttons.first().evaluate(element => element.scrollIntoView({block: 'center'}));

  await buttons.nth(0).hover();
  await waitUntilOpen(contents.nth(0));
  await buttons.nth(2).hover();
  await expect.poll(() => contents.nth(2).evaluate(element => element.matches(':popover-open')), {timeout: 200})
      .toBe(true);
  expect(await page.locator('.tooltip-content:popover-open').count()).toBe(1);
});

test('tooltip: touch never shows one', async ({browser}) => {
  const context = await browser.newContext({hasTouch: true, reducedMotion: 'reduce'});
  const page = await context.newPage();
  await openShowcase(page, 'vega', 'light');
  await page.waitForFunction(() => 'Alpine' in window);
  const trigger = page.locator('[data-scenario="tooltip--default"] .tooltip > button');
  await trigger.evaluate(element => element.scrollIntoView({block: 'center'}));
  await trigger.tap();
  await page.waitForTimeout(500);
  expect(await page.locator('.tooltip-content:popover-open').count()).toBe(0);
  await context.close();
});

test('menus and popovers slide and zoom in only where motion is fine', async ({page}) => {
  await openShowcase(page, 'vega', 'light');
  const style = (selector: string) => page.locator(selector).evaluate(element => {
    const computed = getComputedStyle(element);
    return {translate: computed.translate, scale: computed.scale};
  });
  expect(await style('#file-menu'), 'reduced motion: only the fade').toEqual({translate: 'none', scale: 'none'});
  await page.emulateMedia({reducedMotion: 'no-preference'});
  expect(await style('#file-menu')).toEqual({translate: '0px -8px', scale: '0.95'});
  expect(await style('#popover-top')).toEqual({translate: '0px 8px', scale: '0.95'});
  expect((await style('#popover-right')).translate).toBe('-8px');
  expect((await style('#popover-left')).translate).toBe('8px');
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

/** A menu marked up as <sl:dropdown-menu> renders it (see dropdown-menu.approved.html). */
function menu(id: string, items: string): string {
  return `<button class="btn dropdown-menu-trigger" type="button" id="${id}-trigger" popovertarget="${id}"
      aria-haspopup="menu">Actions</button>
    <div class="dropdown-menu-content" id="${id}" role="menu" popover tabindex="-1" x-data="slDropdownMenu"
      aria-labelledby="${id}-trigger">${items}</div>`;
}

function item(text: string, attributes = ''): string {
  const type = attributes.includes('type=') ? '' : 'type="button" ';
  return `<button ${type}class="dropdown-menu-item" role="menuitem" ${attributes}>${text}</button>`;
}

/** A popover marked up as <sl:popover> renders it (see popover.approved.html). */
function popover(id: string, content: string): string {
  return `<button class="btn popover-trigger" type="button" popovertarget="${id}" aria-haspopup="dialog">Open</button>
    <div class="popover-content" id="${id}" role="dialog" popover tabindex="-1" x-data="slPopover"
      aria-label="Popover ${id}">${content}</div>`;
}

/** A tooltip marked up as <sl:tooltip> renders it (see tooltip.approved.html). */
function tooltip(trigger: string, text: string): string {
  return `<span class="tooltip" x-data="slTooltip">${trigger}<span class="tooltip-content" role="tooltip"
      popover="manual">${text}<span class="tooltip-arrow" aria-hidden="true"></span></span></span>`;
}

type Routes = Record<string, (body: string, headers: Record<string, string>) => { body: string, headers?: Record<string, string> }>;

/** Serves `body` at / with the csp build (and htmx), and `routes` for other requests; returns what the console said. */
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

test('the csp build runs a dropdown menu under a strict policy', async ({page}) => {
  const messages = await openFixture(page, menu('m', item('Edit', 'id="edit"') + item('Archive')));
  const m = page.locator('#m');
  const trigger = page.locator('#m-trigger');

  await expect(trigger).toHaveAttribute('aria-expanded', 'false');
  await trigger.focus();
  await page.keyboard.press('Enter');
  await waitUntilOpen(m);
  await expect(page.locator('#edit')).toBeFocused();
  await page.keyboard.press('ArrowDown');
  await expect(m.getByRole('menuitem', {name: 'Archive'})).toBeFocused();
  await page.keyboard.press('Escape');
  await expectClosed(m);
  await expect(trigger).toBeFocused();
  expect(messages).toEqual([]);
});

test('the csp build runs a popover under a strict policy, and sl-popover-close closes it', async ({page}) => {
  const messages = await openFixture(page, popover('p', '<label for="name">Name</label><input id="name">'));
  const p = page.locator('#p');

  await page.locator('button[popovertarget="p"]').click();
  await waitUntilOpen(p);
  await expect(page.locator('#name')).toBeFocused();
  await page.locator('#name').dispatchEvent('sl-popover-close', {bubbles: true});
  await expectClosed(p);
  expect(messages).toEqual([]);
});

test('the csp build runs a tooltip under a strict policy', async ({page}) => {
  const messages = await openFixture(page, `<p><button type="button" id="before">Before</button></p>`
      + tooltip('<button type="button" id="save" aria-describedby="hint">Save</button>', 'Save changes')
      + '<p id="hint">Saved to the server.</p>');
  const content = page.locator('.tooltip-content');

  await expect(page.locator('#save')).toHaveAttribute('aria-describedby', /^hint sl-tooltip-\d+$/);
  await page.locator('#before').focus();
  await page.keyboard.press('Tab');
  await waitUntilOpen(content);
  await expect(page.locator('#save')).toHaveAccessibleDescription('Saved to the server. Save changes');
  await page.keyboard.press('Escape');
  await expectClosed(content);
  expect(messages).toEqual([]);
});

test('Escape hides a tooltip in a menu, and only the next one closes the menu', async ({page}) => {
  await openFixture(page, menu('m', tooltip(item('Share', 'id="share"'), 'Share with your team') + item('Archive')));
  await page.locator('#m-trigger').focus();
  await page.keyboard.press('Enter');
  await waitUntilOpen(page.locator('#m'));
  await expect(page.locator('#share')).toBeFocused();
  // The first item took the focus from the keyboard: its tooltip shows.
  await waitUntilOpen(page.locator('.tooltip-content'));
  await page.keyboard.press('Escape');
  await expectClosed(page.locator('.tooltip-content'));
  expect(await page.locator('#m').evaluate(element => element.matches(':popover-open'))).toBe(true);
  await page.keyboard.press('Escape');
  await expectClosed(page.locator('#m'));
});

test('without anchor positioning, the stand-in places a menu, a popover and a tooltip', async ({page}) => {
  await page.addInitScript(() => {
    const supports = CSS.supports.bind(CSS);
    CSS.supports = ((...args: [string, string?]) => !String(args[0]).startsWith('position-area')
        && supports(...(args as [string, string]))) as typeof CSS.supports;
  });
  await page.setViewportSize({width: 800, height: 600});
  const messages = await openFixture(page, menu('m', item('Edit') + item('Archive'))
      + popover('p', '<p>Popover text</p>')
      + tooltip('<button type="button" id="tip-trigger">Tip</button>', 'Tooltip text'));

  await page.locator('#m-trigger').click();
  await waitUntilOpen(page.locator('#m'));
  let t = (await page.locator('#m-trigger').boundingBox())!;
  let b = (await page.locator('#m').boundingBox())!;
  expect(await page.locator('#m').evaluate(element => element.style.positionArea)).toBe('none');
  expect(Math.round(b.y - (t.y + t.height)), 'below the trigger, with the gap').toBe(4);
  expect(Math.round(b.x), 'lined up with its start').toBe(Math.round(t.x));
  await page.keyboard.press('Escape');

  await page.locator('button[popovertarget="p"]').click();
  await waitUntilOpen(page.locator('#p'));
  t = (await page.locator('button[popovertarget="p"]').boundingBox())!;
  b = (await page.locator('#p').boundingBox())!;
  expect(Math.round(b.y - (t.y + t.height))).toBe(4);
  expect(Math.abs((b.x + b.width / 2) - (t.x + t.width / 2)), 'centred, or kept in the viewport')
      .toBeLessThanOrEqual(Math.max(0, b.width / 2 - (t.x + t.width / 2)) + 1);
  await page.keyboard.press('Escape');

  await page.locator('#tip-trigger').focus();
  const content = page.locator('.tooltip-content');
  await waitUntilOpen(content);
  t = (await page.locator('#tip-trigger').boundingBox())!;
  b = (await content.boundingBox())!;
  // Too close to the top of the viewport for its side: flipped below the trigger.
  expect(b.y, 'flipped below the trigger').toBeGreaterThanOrEqual(t.y + t.height);
  expect(Math.abs((b.x + b.width / 2) - (t.x + t.width / 2)), 'centred').toBeLessThanOrEqual(1);
  expect(messages).toEqual([]);
});

test('menu items: a submit button submits its form, a link navigates, a dialog opener returns focus', async ({page}) => {
  let posted = false;
  await openFixture(page, `
      <form id="logout" method="post" action="/logout"></form>
      ${menu('m', item('Open dialog', 'id="open-dialog" commandfor="d" command="show-modal"')
        + '<a class="dropdown-menu-item" role="menuitem" href="/next" id="link">Next page</a>'
        + item('Log out', 'type="submit" form="logout" id="logout-item"'))}
      <dialog id="d" class="dialog" closedby="any" x-data="slDialog" aria-label="Dialog">
        <button type="button" commandfor="d" command="close">Close</button>
      </dialog>`, {
    routes: {
      '/logout': () => {
        posted = true;
        return {body: '<p id="logged-out">Logged out</p>'};
      },
      '/next': () => ({body: '<p id="next-page">Next page</p>'}),
    },
  });
  const trigger = page.locator('#m-trigger');

  await trigger.click();
  await page.locator('#open-dialog').click();
  await expect(page.locator('#d')).toHaveJSProperty('open', true);
  await expectClosed(page.locator('#m'));
  await page.keyboard.press('Escape');
  await expect(page.locator('#d')).toHaveJSProperty('open', false);
  await expect(trigger, 'focus returns to the menu trigger, not a hidden item').toBeFocused();

  await trigger.click();
  await page.locator('#logout-item').click();
  await expect(page.locator('#logged-out')).toBeVisible();
  expect(posted).toBe(true);

  await page.goBack();
  await page.waitForFunction(() => 'Alpine' in window);
  await page.locator('#m-trigger').focus();
  await page.keyboard.press('Enter');
  await waitUntilOpen(page.locator('#m'));
  await page.keyboard.press('ArrowDown');
  await expect(page.locator('#link')).toBeFocused();
  await page.keyboard.press('Enter');
  await expect(page.locator('#next-page')).toBeVisible();
});

test('htmx: an hx-* item sends its request and the menu closes', async ({page}) => {
  const archived: string[] = [];
  const messages = await openFixture(page, `<p id="status">Nothing archived</p>
      ${menu('m', item('Archive', 'id="archive" hx-post="/archive" hx-target="#status" hx-swap="outerHTML"'))}`, {
    htmx: true,
    routes: {
      '/archive': () => {
        archived.push('1');
        return {body: '<p id="status">Archived</p>'};
      },
    },
  });

  await page.locator('#m-trigger').focus();
  await page.keyboard.press('Enter');
  await waitUntilOpen(page.locator('#m'));
  await page.keyboard.press('Enter');
  await expectClosed(page.locator('#m'));
  await expect(page.locator('#status')).toHaveText('Archived');
  await expect(page.locator('#m-trigger')).toBeFocused();
  expect(archived).toEqual(['1']);
  expect(messages).toEqual([]);
});

test('htmx: a swap that replaces an open menu leaves a closed, working one', async ({page}) => {
  await openFixture(page, `<div id="row">${menu('m', item('Edit'))}</div>
      <button type="button" id="refresh" hx-get="/row" hx-target="#row" hx-swap="innerHTML">Refresh</button>`, {
    htmx: true,
    routes: {'/row': () => ({body: menu('m', item('Edit') + item('Renamed'))})},
  });

  await page.locator('#m-trigger').click();
  await waitUntilOpen(page.locator('#m'));
  // A swap from elsewhere (a poll, a server event) while the menu is open.
  await page.evaluate(() => (window as unknown as {htmx: {ajax: Function}}).htmx.ajax('GET', '/row',
      {target: '#row', swap: 'innerHTML'}));
  await expect(page.locator('#m [role="menuitem"]', {hasText: 'Renamed'})).toBeAttached();
  expect(await page.locator(':popover-open').count(), 'nothing is left open').toBe(0);
  await expect(page.locator('#m-trigger')).toHaveAttribute('aria-expanded', 'false');

  await page.locator('#m-trigger').click();
  await waitUntilOpen(page.locator('#m'));
  await expect(page.locator('#m-trigger')).toHaveAttribute('aria-expanded', 'true');
  await page.locator('#m').getByRole('menuitem', {name: 'Renamed'}).hover();
  await expect(page.locator('#m').getByRole('menuitem', {name: 'Renamed'})).toBeFocused();
});

test('htmx: history never brings a menu back open or expanded', async ({page}) => {
  await openFixture(page, menu('m', item('Next page', 'id="next" hx-get="/next" hx-target="main" hx-push-url="true"')),
      {htmx: true, routes: {'/next': () => ({body: '<p id="next-page">Next page</p>'})}});

  await page.locator('#m-trigger').click();
  await waitUntilOpen(page.locator('#m'));
  await page.locator('#next').click();
  await expect(page.locator('#next-page')).toBeVisible();
  await page.goBack();
  await expect(page.locator('#m-trigger')).toBeVisible();
  expect(await page.locator(':popover-open').count()).toBe(0);
  await expect(page.locator('#m-trigger')).toHaveAttribute('aria-expanded', 'false');
  await page.locator('#m-trigger').click();
  await waitUntilOpen(page.locator('#m'));
});

test('htmx: history after a navigation from elsewhere with the menu open', async ({page}) => {
  // The snapshot is taken with the menu open and its trigger expanded; going back shows it closed and not expanded
  // (the open state is not in the markup, and Alpine initialises the restored markup again).
  await openFixture(page, `${menu('m', item('Edit'))}
      <a href="/next" id="next" hx-get="/next" hx-target="main" hx-push-url="true">Next</a>`,
  {htmx: true, routes: {'/next': () => ({body: '<p id="next-page">Next page</p>'})}});

  await page.locator('#m-trigger').click();
  await waitUntilOpen(page.locator('#m'));
  await page.evaluate(() => (document.getElementById('next') as HTMLElement).click());
  await expect(page.locator('#next-page')).toBeVisible();
  await page.goBack();
  await expect(page.locator('#m-trigger')).toHaveAttribute('aria-expanded', 'false');
  expect(await page.locator(':popover-open').count()).toBe(0);
});

test('htmx: a form in a popover swaps itself and HX-Trigger sl-popover-close closes it', async ({page}) => {
  const messages = await openFixture(page, `<h2 id="team-name">Team</h2>
      ${popover('rename', `<form hx-post="/rename" hx-target="#team-name" hx-swap="outerHTML">
        <label for="name">Name</label><input id="name" name="name"><button type="submit">Save</button></form>`)}`, {
    htmx: true,
    routes: {
      '/rename': body => ({
        body: `<h2 id="team-name">${new URLSearchParams(body).get('name')}</h2>`,
        headers: {'HX-Trigger': 'sl-popover-close'},
      }),
    },
  });
  const trigger = page.locator('button[popovertarget="rename"]');

  await trigger.click();
  await waitUntilOpen(page.locator('#rename'));
  await expect(page.locator('#name')).toBeFocused();
  await page.locator('#name').fill('Design');
  await page.keyboard.press('Enter');
  await expectClosed(page.locator('#rename'));
  await expect(page.locator('#team-name')).toHaveText('Design');
  await expect(trigger).toBeFocused();
  expect(messages).toEqual([]);
});
