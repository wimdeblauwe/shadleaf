import {expect, test, type Page} from '@playwright/test';
import AxeBuilder from '@axe-core/playwright';
import previews from '../src/generated/previews.json' with {type: 'json'};
import {ORIGIN, openFixture, type FixtureOptions, type Request} from './fixture';

// The sidebar's panel mechanism: sl:sidebar-provider, sl:sidebar, sl:sidebar-trigger, sl:sidebar-inset and slSidebar.
// Fixture pages under a strict Content-Security-Policy with the csp Alpine build, at a desktop width (the sidebar in the
// page, collapsed and expanded by the trigger, Ctrl/Cmd+B and the sl-sidebar-state cookie the "server" reads) and a
// phone width (the panel: focus, inert, Escape, a click outside, links, the breakpoint, htmx). The markup is the
// docs preview's. The showcase checks (a11y.spec.ts, focus.spec.ts) run at both widths too.

const TAGS = ['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa', 'wcag22aa', 'best-practice'];
const DESKTOP = {width: 1024, height: 700};
const PHONE = {width: 390, height: 700};

type Scenario = { id: string, html: string };
const PREVIEW = (previews.scenarios as Scenario[]).find(scenario => scenario.id === 'sidebar--default')!.html;

type ShellOptions = { state?: 'expanded' | 'collapsed', side?: 'start' | 'end', title?: string, current?: string };

/**
 * The preview's shell as a server renders it for a page: the state, the current page's menu button (as
 * #slNav.current marks it; the large brand button in the header never), a same-page link, a control in main.
 */
function shell({state = 'expanded', side = 'start', title = 'Home', current = '/'}: ShellOptions = {}) {
  return PREVIEW
      .replace(/\s*aria-current="page"/g, '')
      .replace(new RegExp(`(<a class="sidebar-menu-button")((?:(?!data-size)[^>])*? href="${current}")`),
          '$1 aria-current="page"$2')
      .replace('data-state="expanded"', `data-state="${state}"`)
      .replace('aria-label="Main">', `aria-label="Main"${side === 'end' ? ' data-side="end"' : ''}>`)
      .replace('</nav>', '<a class="btn" data-variant="ghost" href="#section">Section</a></nav>')
      .replace('<h1>Home</h1>', `<h1>${title}</h1><button id="in-main" type="button">In main</button>`
          + '<p id="section">Section</p>');
}

/** The state the sl-sidebar-state cookie asks for, as #slSidebar reads it. */
const stateFrom = (request: Request) =>
    /(?:^|;\s*)sl-sidebar-state=collapsed/.test(request.headers['cookie'] ?? '') ? 'collapsed' : 'expanded';

type OpenOptions = FixtureOptions & { side?: 'start' | 'end', boost?: 'body' | 'main' };

/** Serves the shell at / and at /inbox, /calendar and /settings, each rendered from the cookie. */
async function openShell(page: Page, {side, boost, ...options}: OpenOptions = {}) {
  const page_ = (title: string, current: string) => (request: Request) =>
      ({body: shell({state: stateFrom(request), side, title, current})});
  const boostAttributes = boost === 'body' ? 'hx-boost="true"'
      : boost === 'main' ? 'hx-boost="true" hx-target="main" hx-select="main" hx-swap="outerHTML"' : '';
  return openFixture(page, (url, request) => shell({state: stateFrom(request), side}), {
    wrap: false,
    htmx: !!boost,
    bodyAttributes: boostAttributes,
    routes: {
      '/inbox': page_('Inbox', '/inbox'), '/calendar': page_('Calendar', '/calendar'),
      '/settings': page_('Settings', '/settings'),
    },
    ...options,
  });
}

const sidebar = (page: Page) => page.locator('#sidebar');
const trigger = (page: Page) => page.locator('.sidebar-trigger');
const isOpen = (page: Page) => sidebar(page).evaluate(element => element.matches(':popover-open'));
const focused = (page: Page) => page.evaluate(() => {
  const element = document.activeElement!;
  return element === document.body ? 'body'
      : `${element.closest('#sidebar') ? 'sidebar: ' : ''}${element.id ? '#' + element.id + ' ' : ''}${
          element.textContent?.replace(/\s+/g, ' ').trim() || element.getAttribute('aria-label')}`;
});
const inertElements = (page: Page) => page.evaluate(() => document.querySelectorAll('[inert]').length);
const box = (page: Page) => sidebar(page).evaluate(element => {
  const rect = element.getBoundingClientRect();
  return {left: Math.round(rect.left), right: Math.round(rect.right), width: Math.round(rect.width)};
});

type AxNode = {
  ignored: boolean, role?: { value: string }, name?: { value: string },
  properties?: { name: string, value: { value: unknown } }[]
};

/** Chromium: the unignored nodes of the accessibility tree, as role:name, and the trigger's expanded state. */
async function accessibilityTree(page: Page) {
  const cdp = await page.context().newCDPSession(page);
  const {nodes} = await cdp.send('Accessibility.getFullAXTree') as { nodes: AxNode[] };
  const visible = nodes.filter(node => !node.ignored);
  const button = visible.find(node => node.role?.value === 'button' && node.name?.value === 'Toggle sidebar');
  return {
    roles: visible.map(node => `${node.role?.value}:${node.name?.value ?? ''}`),
    expanded: button?.properties?.find(property => property.name === 'expanded')?.value.value,
  };
}

test.describe('desktop', () => {
  test.beforeEach(async ({page}) => {
    await page.setViewportSize(DESKTOP);
  });

  test('the sidebar is a navigation landmark beside the page, not a dialog', async ({page, browserName}) => {
    const messages = await openShell(page);

    await expect(sidebar(page)).toBeVisible();
    expect(await isOpen(page)).toBe(false);
    expect(await box(page)).toMatchObject({left: 0, width: 256});
    await expect(page.getByRole('navigation', {name: 'Main'})).toHaveCount(1);
    await expect(page.getByRole('dialog')).toHaveCount(0);
    if (browserName === 'chromium') {
      const {roles} = await accessibilityTree(page);
      expect(roles).toContain('navigation:Main');
      expect(roles).toContain('main:');
      expect(roles.filter(role => role.startsWith('dialog'))).toEqual([]);
    }
    // The first Tab lands on the first link, the application's name in the header: the sidebar itself is no tab stop
    // (Firefox made a dialog one).
    await page.keyboard.press('Tab');
    expect(await focused(page)).toBe('sidebar: Acme Inc. Enterprise');
    const results = await new AxeBuilder({page}).withTags(TAGS).analyze();
    expect(results.violations.map(violation => violation.id)).toEqual([]);
    expect(messages).toEqual([]);
  });

  test('the trigger reports the expanded state and collapses the sidebar into a cookie', async ({page, context, browserName}) => {
    await openShell(page);
    await page.evaluate(() => document.addEventListener('sl-sidebar-toggle',
        event => (window as unknown as { toggles: unknown[] }).toggles.push((event as CustomEvent).detail)));
    await page.evaluate(() => (window as unknown as { toggles: unknown[] }).toggles = []);

    // popovertarget would make the browser report it collapsed: it is taken off, aria-expanded tells the truth.
    await expect(trigger(page)).not.toHaveAttribute('popovertarget');
    await expect(trigger(page)).toHaveAttribute('aria-expanded', 'true');
    await expect(trigger(page)).toHaveAttribute('aria-controls', 'sidebar');
    if (browserName === 'chromium') {
      expect((await accessibilityTree(page)).expanded).toBe(true);
    }

    await trigger(page).click();
    await expect(page.locator('.sidebar-provider')).toHaveAttribute('data-state', 'collapsed');
    await expect(trigger(page)).toHaveAttribute('aria-expanded', 'false');
    await expect(sidebar(page)).toBeHidden();
    expect(await isOpen(page)).toBe(false);
    if (browserName === 'chromium') {
      const {roles, expanded} = await accessibilityTree(page);
      expect(expanded).toBe(false);
      expect(roles).not.toContain('navigation:Main');
    }
    expect((await context.cookies()).filter(cookie => cookie.name === 'sl-sidebar-state'))
        .toEqual([expect.objectContaining({value: 'collapsed', path: '/', sameSite: 'Lax'})]);
    // Its links leave the tab order: hidden, they cannot take the focus.
    expect(await sidebar(page).locator('a').first().evaluate(link => {
      (link as HTMLElement).focus();
      return document.activeElement === link;
    })).toBe(false);

    await trigger(page).click();
    await expect(page.locator('.sidebar-provider')).toHaveAttribute('data-state', 'expanded');
    await expect(sidebar(page)).toBeVisible();
    expect((await context.cookies()).find(cookie => cookie.name === 'sl-sidebar-state')?.value).toBe('expanded');
    expect(await page.evaluate(() => (window as unknown as { toggles: unknown[] }).toggles)).toEqual([
      {state: 'collapsed', open: false, mobile: false},
      {state: 'expanded', open: false, mobile: false},
    ]);
  });

  test('Ctrl/Cmd+B collapses and expands, but not while typing in an editor', async ({page}) => {
    await openShell(page);
    const inset = page.locator('.sidebar-inset');
    const wide = (await inset.boundingBox())!.width;

    await page.keyboard.press('ControlOrMeta+b');
    await expect(page.locator('.sidebar-provider')).toHaveAttribute('data-state', 'collapsed');
    expect((await inset.boundingBox())!.width).toBe(DESKTOP.width);
    expect(wide).toBe(DESKTOP.width - 256);

    await page.keyboard.press('ControlOrMeta+b');
    await expect(page.locator('.sidebar-provider')).toHaveAttribute('data-state', 'expanded');

    await page.locator('h1').evaluate(heading => {
      heading.contentEditable = 'true';
      heading.focus();
    });
    await page.keyboard.press('ControlOrMeta+b');
    await expect(page.locator('.sidebar-provider')).toHaveAttribute('data-state', 'expanded');
  });

  test('the server renders the state from the cookie after a reload', async ({page}) => {
    await openShell(page);
    await trigger(page).click();
    await expect(page.locator('.sidebar-provider')).toHaveAttribute('data-state', 'collapsed');

    // The state is in the response: check it before any script could have changed it.
    const response = await page.reload();
    expect(await response!.text()).toContain('data-state="collapsed"');
    await page.waitForFunction(() => 'Alpine' in window);
    await expect(sidebar(page)).toBeHidden();
    await expect(trigger(page)).toHaveAttribute('aria-expanded', 'false');
  });

  test('a trigger htmx swaps in is set up for the desktop too', async ({page}) => {
    await openShell(page, {boost: 'main'});
    await page.locator('#sidebar a[href="/inbox"]').click();
    await expect(page.locator('h1')).toHaveText('Inbox');

    await expect(trigger(page)).not.toHaveAttribute('popovertarget');
    await expect(trigger(page)).toHaveAttribute('aria-expanded', 'true');
    await trigger(page).click();
    await expect(page.locator('.sidebar-provider')).toHaveAttribute('data-state', 'collapsed');
    expect(await isOpen(page)).toBe(false);
  });

  test.describe('without JavaScript', () => {
    test.use({javaScriptEnabled: false});

    test('the collapsed state is painted from the cookie, and the trigger is hidden', async ({page, context}) => {
      await context.addCookies([{name: 'sl-sidebar-state', value: 'collapsed', url: `${ORIGIN}/`}]);
      await openShell(page, {alpine: false});

      await expect(page.locator('.sidebar-provider')).toHaveAttribute('data-state', 'collapsed');
      await expect(sidebar(page)).toBeHidden();
      expect((await page.locator('.sidebar-inset').boundingBox())!.width).toBe(DESKTOP.width);
      await expect(trigger(page)).toBeHidden();

      // The panel ignores the desktop's state: on a phone the trigger shows and opens it with its links.
      await page.setViewportSize(PHONE);
      await expect(trigger(page)).toBeVisible();
      await trigger(page).click();
      expect(await isOpen(page)).toBe(true);
      await expect(sidebar(page).getByRole('link', {name: 'Inbox'})).toBeVisible();
    });

    test('expanded without a cookie, the trigger hidden', async ({page}) => {
      await openShell(page, {alpine: false});
      await expect(sidebar(page)).toBeVisible();
      await expect(trigger(page)).toBeHidden();
    });
  });
});

test.describe('phone', () => {
  test.beforeEach(async ({page}) => {
    await page.setViewportSize(PHONE);
  });

  test('the panel opens with the focus in it and the page inert', async ({page, browserName}) => {
    const messages = await openShell(page);
    await expect(sidebar(page)).toBeHidden();
    await expect(trigger(page)).toHaveAttribute('popovertarget', 'sidebar');
    await expect(trigger(page)).toHaveAttribute('aria-expanded', 'false');

    await trigger(page).focus();
    await page.keyboard.press('Enter');
    await expect(sidebar(page)).toBeVisible();
    expect(await isOpen(page)).toBe(true);
    // On the current page's link.
    await expect.poll(() => focused(page)).toBe('sidebar: Home');
    await expect(trigger(page)).toHaveAttribute('aria-expanded', 'true');
    expect(await box(page)).toMatchObject({left: 0, width: 288});

    // Tab stays in the panel: everything else is inert. Past its last link the focus goes to the browser's own
    // controls (the page reports the body), never into the page.
    const stops: string[] = [];
    for (let i = 0; i < 12; i++) {
      await page.keyboard.press('Tab');
      stops.push(await focused(page));
    }
    expect(stops.filter(stop => !stop.startsWith('sidebar: ') && stop !== 'body'), stops.join(', ')).toEqual([]);
    expect(await page.locator('.sidebar-inset').evaluate(element => (element as HTMLElement).inert)).toBe(true);
    expect(await page.evaluate(() => {
      document.getElementById('in-main')!.focus();
      return document.activeElement!.id;
    })).not.toBe('in-main');
    // And out of the accessibility tree (read through the DevTools protocol, so Chromium only; Playwright's role
    // queries do not leave out inert content).
    if (browserName === 'chromium') {
      const {roles} = await accessibilityTree(page);
      expect(roles).toContain('navigation:Main');
      // The panel's own buttons (Add project, More for ...) stay; the page's are gone.
      expect(roles.filter(role => /^(main|heading):/.test(role)
          || ['button:Toggle sidebar', 'button:In main'].includes(role))).toEqual([]);
      expect(roles).toContain('button:Add project');
    }
    // A click beside the panel does not reach the page under it: inert content is not hit.
    expect(await page.evaluate(() => document.elementFromPoint(370, 300)?.closest('.sidebar-inset') ?? null)).toBeNull();
    // While the panel is open the page's main and its h1 are inert, as behind a modal dialog: the two page-level rules
    // that look for them are expected to fail. Everything else must pass.
    const results = await new AxeBuilder({page}).withTags(TAGS)
        .disableRules(['landmark-one-main', 'page-has-heading-one']).analyze();
    expect(results.violations.map(violation => violation.id)).toEqual([]);
    expect(messages).toEqual([]);
  });

  test('Escape closes it and returns the focus to the trigger', async ({page}) => {
    await openShell(page);
    await trigger(page).click();
    await expect.poll(() => focused(page)).toBe('sidebar: Home');

    await page.keyboard.press('Escape');
    expect(await isOpen(page)).toBe(false);
    await expect(trigger(page)).toBeFocused();
    await expect(trigger(page)).toHaveAttribute('aria-expanded', 'false');
    expect(await inertElements(page)).toBe(0);
  });

  test('a click outside closes it, and the page is usable again', async ({page}) => {
    await openShell(page);
    await trigger(page).click();
    await expect.poll(() => focused(page)).toBe('sidebar: Home');

    await page.mouse.click(370, 300);
    expect(await isOpen(page)).toBe(false);
    expect(await inertElements(page)).toBe(0);
    await page.locator('#in-main').focus();
    await expect(page.locator('#in-main')).toBeFocused();
  });

  test('Ctrl/Cmd+B opens and closes the panel, and the focus goes back where it was', async ({page}) => {
    await openShell(page);
    await page.locator('#in-main').focus();
    await page.keyboard.press('ControlOrMeta+b');
    expect(await isOpen(page)).toBe(true);
    await expect.poll(() => focused(page)).toBe('sidebar: Home');
    await page.keyboard.press('ControlOrMeta+b');
    expect(await isOpen(page)).toBe(false);
    await expect(page.locator('#in-main')).toBeFocused();
    await expect(page.locator('.sidebar-provider')).toHaveAttribute('data-state', 'expanded');
  });

  test('sl-sidebar-toggle tells the application the panel opened and closed', async ({page}) => {
    await openShell(page);
    await page.evaluate(() => {
      const toggles: unknown[] = (window as unknown as { toggles: unknown[] }).toggles = [];
      document.addEventListener('sl-sidebar-toggle', event => toggles.push((event as CustomEvent).detail));
    });
    await trigger(page).click();
    await page.keyboard.press('Escape');
    expect(await page.evaluate(() => (window as unknown as { toggles: unknown[] }).toggles)).toEqual([
      {state: 'expanded', open: true, mobile: true},
      {state: 'expanded', open: false, mobile: true},
    ]);
  });

  test('a same-page link closes it', async ({page}) => {
    await openShell(page);
    await trigger(page).click();
    await sidebar(page).getByRole('link', {name: 'Section'}).click();
    expect(await isOpen(page)).toBe(false);
    expect(await inertElements(page)).toBe(0);
    expect(new URL(page.url()).hash).toBe('#section');
  });

  test('a followed link renders the next page with the panel closed', async ({page}) => {
    await openShell(page);
    await trigger(page).click();
    await sidebar(page).getByRole('link', {name: 'Inbox'}).click();
    await page.waitForURL('**/inbox');
    await expect(page.locator('h1')).toHaveText('Inbox');
    expect(await isOpen(page)).toBe(false);
  });

  for (const boost of ['body', 'main'] as const) {
    test(`hx-boost on ${boost}: a link closes the panel, and history brings the page back closed`, async ({page}) => {
      const messages = await openShell(page, {boost});
      await trigger(page).click();
      await expect.poll(() => focused(page)).toBe('sidebar: Home');

      await sidebar(page).getByRole('link', {name: 'Inbox'}).click();
      await page.waitForURL('**/inbox');
      await expect(page.locator('h1')).toHaveText('Inbox');
      expect(await isOpen(page)).toBe(false);
      expect(await inertElements(page)).toBe(0);
      await page.locator('#in-main').focus();
      await expect(page.locator('#in-main')).toBeFocused();

      // Open it again, then go back: htmx's snapshot was taken with the panel closed and nothing inert.
      await trigger(page).click();
      expect(await isOpen(page)).toBe(true);
      await page.goBack();
      await expect(page.locator('h1')).toHaveText('Home');
      expect(await isOpen(page)).toBe(false);
      expect(await inertElements(page)).toBe(0);
      await trigger(page).click();
      expect(await isOpen(page)).toBe(true);
      await expect.poll(() => focused(page)).toBe('sidebar: Home');
      expect(messages).toEqual([]);
    });
  }

  test('crossing 768 px closes the panel and hands the trigger over', async ({page}) => {
    await openShell(page);
    await trigger(page).click();
    expect(await isOpen(page)).toBe(true);

    await page.setViewportSize(DESKTOP);
    await expect.poll(() => isOpen(page)).toBe(false);
    expect(await inertElements(page)).toBe(0);
    await expect(sidebar(page)).toBeVisible();
    await expect(trigger(page)).not.toHaveAttribute('popovertarget');
    await expect(trigger(page)).toHaveAttribute('aria-expanded', 'true');

    await page.setViewportSize(PHONE);
    await expect(trigger(page)).toHaveAttribute('popovertarget', 'sidebar');
    await expect(trigger(page)).toHaveAttribute('aria-expanded', 'false');
    await expect(sidebar(page)).toBeHidden();
  });

  test('a page restored from the back/forward cache comes back closed', async ({page}) => {
    // Playwright turns the back/forward cache off: send the event the browser would.
    await openShell(page);
    await trigger(page).click();
    expect(await isOpen(page)).toBe(true);
    await page.evaluate(() => window.dispatchEvent(new PageTransitionEvent('pageshow', {persisted: true})));
    expect(await isOpen(page)).toBe(false);
    expect(await inertElements(page)).toBe(0);
  });

  // Negative control: what slSidebar adds. Without it the panel still opens, but it is not modal.
  test('without Shadleaf\'s script the panel opens natively, and Tab walks out into the page', async ({page}) => {
    await openShell(page, {alpine: false});
    await trigger(page).focus();
    await page.keyboard.press('Enter');
    expect(await isOpen(page)).toBe(true);
    // The focus stays on the trigger, and the page is not inert.
    expect(await focused(page)).toBe('Toggle sidebar');
    expect(await inertElements(page)).toBe(0);
    // The browser puts a popover's content right after its trigger in the tab order: past it, into the page.
    const stops: string[] = [];
    for (let i = 0; i < 20; i++) {
      await page.keyboard.press('Tab');
      stops.push(await focused(page));
    }
    expect(stops.some(stop => !stop.startsWith('sidebar: ')), stops.join(', ')).toBe(true);
    await expect(page.getByRole('button', {name: 'In main'})).toHaveCount(1);
  });
});

test.describe('content parts', () => {
  const link = (page: Page, name: string) => sidebar(page).getByRole('link', {name, exact: true});
  const rect = (page: Page, selector: string) => page.locator(selector).first().evaluate(element => {
    const box = element.getBoundingClientRect();
    return {left: box.left, right: box.right, top: box.top, bottom: box.bottom};
  });

  test('groups are named by their labels, and a badge describes its link', async ({page}) => {
    await page.setViewportSize(DESKTOP);
    await openShell(page);

    await expect(sidebar(page).getByRole('group', {name: 'Platform'})).toBeVisible();
    await expect(sidebar(page).getByRole('group', {name: 'Projects'})).toBeVisible();
    await expect(sidebar(page).getByRole('group', {name: 'Platform'}).getByRole('listitem')).toHaveCount(3);
    await expect(link(page, 'Inbox')).toHaveAccessibleDescription('12 unread');
    await expect(link(page, 'Home')).toHaveAttribute('aria-current', 'page');
    await expect(link(page, 'Home')).not.toHaveAttribute('aria-describedby');
    await expect(sidebar(page).getByRole('button', {name: 'Add project'})).toBeVisible();
    // The current page stands out: the accent behind it, a plain entry has none.
    const background = (name: string) => link(page, name).evaluate(element => getComputedStyle(element).backgroundColor);
    expect(await background('Home')).not.toBe(await background('Calendar'));
    expect(await background('Calendar')).toBe('rgba(0, 0, 0, 0)');
  });

  test('the server marks the current page, and the panel opens on it', async ({page}) => {
    await page.setViewportSize(PHONE);
    await openShell(page, {path: '/calendar'});
    // The panel is closed, so by its href: role queries skip hidden content.
    await expect(sidebar(page).locator('a[href="/calendar"]')).toHaveAttribute('aria-current', 'page');
    await expect(sidebar(page).locator('[aria-current]')).toHaveCount(1);

    await trigger(page).click();
    // Not the first link (the application's name), nor Home: the current page's menu button.
    await expect.poll(() => focused(page)).toBe('sidebar: Calendar');
  });

  for (const viewport of [DESKTOP, PHONE]) {
    const width = viewport === DESKTOP ? 'desktop' : 'phone';

    test(`${width}: badges and actions lie at the end of their item, the label beside them`, async ({page}) => {
      await page.setViewportSize(viewport);
      await openShell(page);
      if (viewport === PHONE) {
        await trigger(page).click();
        await expect(sidebar(page)).toBeVisible();
      }
      for (const [item, end] of [['#inbox', '.sidebar-menu-badge'],
        ['.sidebar-menu-item:has(> .sidebar-menu-action)', '.sidebar-menu-action']] as const) {
        const button = await rect(page, `${item} > .sidebar-menu-button`);
        const label = await rect(page, `${item} .sidebar-menu-button-label`);
        const at = await rect(page, `${item} > ${end}`);
        expect(at.right).toBeLessThanOrEqual(button.right);
        expect(at.right).toBeGreaterThan(button.right - 8);
        expect(label.right).toBeLessThanOrEqual(at.left);
        // Centred on the button, give or take a pixel.
        expect(Math.abs((at.top + at.bottom) / 2 - (button.top + button.bottom) / 2)).toBeLessThanOrEqual(1);
      }
      // The header's two lines: one under the other, inside the large button.
      const strong = await rect(page, '.sidebar-header strong');
      const small = await rect(page, '.sidebar-header small');
      expect(small.top).toBeGreaterThanOrEqual(strong.bottom - 1);
    });
  }

  test('desktop: an action shown on hover appears on hover and on focus', async ({page}) => {
    await page.setViewportSize(DESKTOP);
    await openShell(page);
    const action = sidebar(page).getByRole('button', {name: 'More for Design Engineering'});
    const opacity = () => action.evaluate(element => getComputedStyle(element).opacity);

    expect(await opacity()).toBe('0');
    await link(page, 'Design Engineering').hover();
    expect(await opacity()).toBe('1');
    await page.mouse.move(700, 600);
    expect(await opacity()).toBe('0');
    await link(page, 'Design Engineering').focus();
    await page.keyboard.press('Tab');
    await expect(action).toBeFocused();
    expect(await opacity()).toBe('1');
  });

  test('phone: an action shown on hover is always there', async ({page}) => {
    await page.setViewportSize(PHONE);
    await openShell(page);
    await trigger(page).click();
    const action = sidebar(page).getByRole('button', {name: 'More for Design Engineering'});
    expect(await action.evaluate(element => getComputedStyle(element).opacity)).toBe('1');
  });
});

test.describe('direction and side', () => {
  for (const [dir, side, edge] of [['ltr', 'start', 'left'], ['rtl', 'start', 'right'], ['ltr', 'end', 'right'],
    ['rtl', 'end', 'left']] as const) {
    test(`${dir}, side ${side}: on the ${edge}, sliding in from there`, async ({page}) => {
      await page.emulateMedia({reducedMotion: 'no-preference'});
      await page.setViewportSize(DESKTOP);
      await openShell(page, {side, htmlAttributes: `dir="${dir}"`});
      const atEdge = (rect: { left: number, right: number }, width: number) =>
          edge === 'left' ? rect.left === 0 : rect.right === width;
      expect(atEdge(await box(page), DESKTOP.width), JSON.stringify(await box(page))).toBe(true);

      // Collapsing moves it out past that edge.
      await trigger(page).click();
      await expect(sidebar(page)).toBeHidden();
      const collapsed = await box(page);
      expect(edge === 'left' ? collapsed.right <= 0 : collapsed.left >= DESKTOP.width, JSON.stringify(collapsed))
          .toBe(true);
      await trigger(page).click();

      await page.setViewportSize(PHONE);
      await expect.poll(() => sidebar(page).evaluate(element => element.getAnimations().length)).toBe(0);
      await trigger(page).click();
      const translate = await sidebar(page).evaluate(element => element.getAnimations()
          .map(animation => (animation.effect as KeyframeEffect).getKeyframes())
          .flat().map(frame => frame.translate).filter(Boolean));
      expect(translate[0]).toBe(edge === 'left' ? '-100%' : '100%');
      await expect.poll(async () => atEdge(await box(page), PHONE.width)).toBe(true);
    });
  }
});

test.describe('motion', () => {
  test('the panel slides and the desktop sidebar moves only without reduced motion', async ({page}) => {
    for (const reducedMotion of ['no-preference', 'reduce'] as const) {
      await page.emulateMedia({reducedMotion});
      await page.setViewportSize(PHONE);
      await openShell(page);
      await trigger(page).click();
      const opening = await sidebar(page).evaluate(element => element.getAnimations()
          .map(animation => (animation as CSSTransition).transitionProperty));
      expect(opening.includes('translate'), `${reducedMotion}: ${opening}`).toBe(reducedMotion === 'no-preference');
      await page.keyboard.press('Escape');

      await page.setViewportSize(DESKTOP);
      await trigger(page).click();
      const collapsing = await sidebar(page).evaluate(element => element.getAnimations()
          .map(animation => (animation as CSSTransition).transitionProperty));
      expect(collapsing.includes('margin-inline-start') || collapsing.includes('margin-left'),
          `${reducedMotion}: ${collapsing}`).toBe(reducedMotion === 'no-preference');
      await page.unrouteAll({behavior: 'ignoreErrors'});
      await page.context().clearCookies();
    }
  });
});

test('forced colours: the sidebar keeps a border, the panel its backdrop, the current page an outline', async ({page, browserName}) => {
  test.skip(browserName !== 'chromium', 'forced colours emulation is Chromium only');
  await page.emulateMedia({forcedColors: 'active'});
  await page.setViewportSize(DESKTOP);
  await openShell(page);
  const border = () => sidebar(page).evaluate(element => {
    const style = getComputedStyle(element);
    return {style: style.borderInlineEndStyle, width: parseFloat(style.borderInlineEndWidth)};
  });
  expect(await border()).toMatchObject({style: 'solid', width: 1});
  // Without backgrounds the current page keeps an outline; the focus gets a thicker one.
  const outline = (name: string) => sidebar(page).getByRole('link', {name, exact: true}).evaluate(element => {
    const style = getComputedStyle(element);
    return {style: style.outlineStyle, width: parseFloat(style.outlineWidth)};
  });
  expect(await outline('Home')).toMatchObject({style: 'solid', width: 1});
  expect((await outline('Calendar')).style).toBe('none');
  await sidebar(page).getByRole('link', {name: 'Calendar', exact: true}).focus();
  await page.keyboard.press('Shift+Tab');
  await page.keyboard.press('Tab');
  expect(await outline('Calendar')).toMatchObject({style: 'solid', width: 2});

  await page.setViewportSize(PHONE);
  await trigger(page).click();
  expect(await border()).toMatchObject({style: 'solid', width: 1});
  expect(await sidebar(page).evaluate(element => getComputedStyle(element, '::backdrop').backgroundColor))
      .not.toBe('rgba(0, 0, 0, 0)');
});
