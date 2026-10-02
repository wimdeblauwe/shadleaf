import {expect, test, type Page} from '@playwright/test';
import AxeBuilder from '@axe-core/playwright';
import previews from '../src/generated/previews.json' with {type: 'json'};
import {ORIGIN, openFixture, type FixtureOptions, type Request} from './fixture';
import {openShowcase, skins} from './showcase';

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

type Variant = 'sidebar' | 'floating' | 'inset';
type Collapsible = 'offcanvas' | 'icon' | 'none';
type ShellOptions = {
  state?: 'expanded' | 'collapsed', side?: 'start' | 'end', variant?: Variant, collapsible?: Collapsible,
  title?: string, current?: string
};

/**
 * The preview's shell as a server renders it for a page: the state, the current page's menu button or sub-button (as
 * #slNav.current marks it; the large brand button in the header never) and the Documents group open when it holds it,
 * the breadcrumb's page, a same-page link, a control in main. The preview is the inset variant, collapsing to icons;
 * the plain sidebar, collapsing off-canvas, unless asked.
 */
function shell({state = 'expanded', side = 'start', variant = 'sidebar', collapsible = 'offcanvas', title = 'Home',
                 current = '/'}: ShellOptions = {}) {
  return PREVIEW
      .replace(' data-variant="inset"', '')
      .replace(' data-collapsible="icon"', '')
      .replace(/(<a class="sidebar-menu-(?:sub-)?button"[^>]*?)\s*aria-current="page"/g, '$1')
      .replace(new RegExp(`(<a class="sidebar-menu-(?:sub-)?button")((?:(?!data-size)[^>])*? href="${current}")`),
          '$1 aria-current="page"$2')
      .replace('<details class="sidebar-menu-collapsible">',
          `<details class="sidebar-menu-collapsible"${current.startsWith('/documents') ? ' open' : ''}>`)
      .replace('data-state="expanded"', `data-state="${state}"`)
      .replace('aria-label="Main">', `aria-label="Main"${side === 'end' ? ' data-side="end"' : ''}${
          variant === 'sidebar' ? '' : ` data-variant="${variant}"`}${
          collapsible === 'offcanvas' ? '' : ` data-collapsible="${collapsible}"`}>`)
      .replace('</nav>', '<a class="btn" data-variant="ghost" href="#section">Section</a></nav>')
      .replace('aria-current="page">Home</span>', `aria-current="page">${title}</span>`)
      .replace('<h1>Home</h1>', `<h1>${title}</h1><button id="in-main" type="button">In main</button>`
          + '<p id="section">Section</p>');
}

/** The state the sl-sidebar-state cookie asks for, as #slSidebar reads it. */
const stateFrom = (request: Request) =>
    /(?:^|;\s*)sl-sidebar-state=collapsed/.test(request.headers['cookie'] ?? '') ? 'collapsed' : 'expanded';

type OpenOptions = FixtureOptions & {
  side?: 'start' | 'end', variant?: Variant, collapsible?: Collapsible, boost?: 'body' | 'main'
};

/** Serves the shell at / and at /inbox, /calendar, /settings and the two documents pages, each rendered from the cookie. */
async function openShell(page: Page, {side, variant, collapsible, boost, ...options}: OpenOptions = {}) {
  const page_ = (title: string, current: string) => (request: Request) =>
      ({body: shell({state: stateFrom(request), side, variant, collapsible, title, current})});
  const boostAttributes = boost === 'body' ? 'hx-boost="true"'
      : boost === 'main' ? 'hx-boost="true" hx-target="main" hx-select="main" hx-swap="outerHTML"' : '';
  return openFixture(page, (url, request) => shell({state: stateFrom(request), side, variant, collapsible}), {
    wrap: false,
    htmx: !!boost,
    bodyAttributes: boostAttributes,
    routes: {
      '/inbox': page_('Inbox', '/inbox'), '/calendar': page_('Calendar', '/calendar'),
      '/settings': page_('Settings', '/settings'),
      '/documents': page_('Recent', '/documents'), '/documents/shared': page_('Shared with me', '/documents/shared'),
    },
    ...options,
  });
}

const sidebar = (page: Page) => page.locator('#sidebar');
const trigger = (page: Page) => page.locator('.sidebar-trigger');
const isOpen = (page: Page) => sidebar(page).evaluate(element => element.matches(':popover-open'));
/** The focused element: in the sidebar or not, its id, and its text (not a menu button's tooltip, which repeats it). */
const focused = (page: Page) => page.evaluate(() => {
  const element = document.activeElement!;
  const text = (element.cloneNode(true) as Element);
  text.querySelectorAll('[aria-hidden="true"]').forEach(hidden => hidden.remove());
  return element === document.body ? 'body'
      : `${element.closest('#sidebar') ? 'sidebar: ' : ''}${element.id ? '#' + element.id + ' ' : ''}${
          text.textContent?.replace(/\s+/g, ' ').trim() || element.getAttribute('aria-label')}`;
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
    // The first Tab lands on the skip link, the second on the first link, the application's name in the header: the
    // sidebar itself is no tab stop (Firefox made a dialog one).
    await page.keyboard.press('Tab');
    expect(await focused(page)).toBe('Skip to main content');
    await page.keyboard.press('Tab');
    expect(await focused(page)).toBe('sidebar: #team-switcher-trigger Acme Inc. Enterprise');
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

  for (const boost of ['body', 'main'] as const) {
    test(`hx-boost on ${boost}: back shows the state the user chose last, not the one in htmx's snapshot`, async ({page}) => {
      await openShell(page, {boost});
      await trigger(page).click();
      await expect(page.locator('.sidebar-provider')).toHaveAttribute('data-state', 'collapsed');
      // Collapsed off-canvas the links are hidden: click one from script, which htmx boosts as any click.
      await page.locator('#sidebar a[href="/inbox"]').evaluate(link => (link as HTMLElement).click());
      await page.waitForURL('**/inbox');
      await expect(page.locator('h1')).toHaveText('Inbox');
      await trigger(page).click();
      await expect(page.locator('.sidebar-provider')).toHaveAttribute('data-state', 'expanded');

      await page.goBack();
      await expect(page.locator('h1')).toHaveText('Home');
      await expect(page.locator('.sidebar-provider')).toHaveAttribute('data-state', 'expanded');
      await expect(sidebar(page)).toBeVisible();
      await expect(trigger(page)).toHaveAttribute('aria-expanded', 'true');
      await expect(page.locator('.sidebar-provider')).not.toHaveAttribute('data-history');
    });
  }

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
    // Home, Inbox, Calendar and the closed Documents group (its sub-items are hidden).
    await expect(sidebar(page).getByRole('group', {name: 'Platform'}).getByRole('listitem')).toHaveCount(4);
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

test.describe('skip link', () => {
  const requests = (page: Page) => {
    const urls: string[] = [];
    page.on('request', request => urls.push(new URL(request.url()).pathname));
    return urls;
  };

  for (const width of [DESKTOP, PHONE]) {
    test(`the first Tab shows it, and following it moves the focus to main: ${width.width} px`, async ({page}) => {
      await page.setViewportSize(width);
      const messages = await openShell(page);
      const link = page.locator('.skip-link');

      // Hidden (one clipped pixel) until it has the focus, then drawn at the top of the page, above the sidebar.
      expect((await link.boundingBox())!.width).toBeLessThanOrEqual(1);
      await page.keyboard.press('Tab');
      expect(await focused(page)).toBe('Skip to main content');
      const box = (await link.boundingBox())!;
      expect(box.width).toBeGreaterThan(100);
      expect(box.y).toBeLessThan(20);
      expect(await page.evaluate(([x, y]) => document.elementFromPoint(x, y)?.closest('.skip-link') !== null,
          [box.x + box.width / 2, box.y + box.height / 2])).toBe(true);

      await page.keyboard.press('Enter');
      await expect.poll(() => focused(page)).toMatch(/^#main /);
      expect(page.url()).toBe(`${ORIGIN}/#main`);
      // The skip link is hidden again, and the next Tab goes on inside main: the trigger in its header.
      expect((await link.boundingBox())!.width).toBeLessThanOrEqual(1);
      await page.keyboard.press('Tab');
      expect(await focused(page)).toBe('Toggle sidebar');
      expect(messages).toEqual([]);
    });
  }

  test('main is no tab stop, and draws no ring when it has the focus', async ({page}) => {
    await page.setViewportSize(DESKTOP);
    await openShell(page);
    await page.locator('.skip-link').focus();
    await page.keyboard.press('Enter');
    await expect.poll(() => focused(page)).toMatch(/^#main /);
    expect(await page.locator('#main').evaluate(element =>
        [element.matches(':focus-visible'), getComputedStyle(element).outlineStyle])).toEqual([true, 'none']);

    // Shift+Tab from the trigger in main goes back to the sidebar's last link (the fixture's Section), not to main.
    await page.keyboard.press('Tab');
    await page.keyboard.press('Shift+Tab');
    expect(await focused(page)).toBe('sidebar: Section');
  });

  test('with hx-boost on the body it moves the focus without a request', async ({page}) => {
    await page.setViewportSize(DESKTOP);
    const messages = await openShell(page, {boost: 'body'});
    const urls = requests(page);

    await page.keyboard.press('Tab');
    await page.keyboard.press('Enter');
    await expect.poll(() => focused(page)).toMatch(/^#main /);
    expect(urls).toEqual([]);
    expect(page.url()).toBe(`${ORIGIN}/#main`);

    // After a boosted navigation the new page's skip link works the same.
    await page.locator('#sidebar a[href="/inbox"]').click();
    await expect(page.locator('h1')).toHaveText('Inbox');
    await page.locator('.skip-link').focus();
    await page.keyboard.press('Enter');
    await expect.poll(() => focused(page)).toMatch(/^#main /);
    expect(messages).toEqual([]);
  });

  // Both browsers blur the link and leave the focus on the body: nothing tells a screen reader the content was reached.
  test('without tabindex on main the focus goes to the body (why sl:sidebar-inset renders it)', async ({page, browserName}) => {
    await page.setViewportSize(DESKTOP);
    await openFixture(page, PREVIEW.replace(/(<main[^>]*?) tabindex="-1"/, '$1'), {wrap: false});
    await page.keyboard.press('Tab');
    await page.keyboard.press('Enter');
    await page.waitForFunction(() => location.hash === '#main');
    expect(await focused(page), browserName).toBe('body');
    // Only where the next Tab starts has moved: into main.
    await page.keyboard.press('Tab');
    expect(await focused(page), browserName).toBe('Toggle sidebar');
  });
});

const inset = (page: Page) => page.locator('#main');
const rectOf = (locator: ReturnType<Page['locator']>) => locator.evaluate(element => {
  const rect = element.getBoundingClientRect();
  return {left: Math.round(rect.left), right: Math.round(rect.right), top: Math.round(rect.top),
    bottom: Math.round(rect.bottom), width: Math.round(rect.width)};
});
const styleOf = (locator: ReturnType<Page['locator']>) => locator.evaluate(element => {
  const style = getComputedStyle(element);
  return {
    background: style.backgroundColor, radius: parseFloat(style.borderStartStartRadius), shadow: style.boxShadow,
    padding: parseFloat(style.paddingInlineStart), borders: [style.borderTopWidth, style.borderRightWidth,
      style.borderBottomWidth, style.borderLeftWidth].map(parseFloat), borderStyle: style.borderTopStyle,
  };
});

test.describe('variants', () => {
  // [dir, side, where the sidebar is]: the inset card keeps an 8 px margin on every edge but the sidebar's.
  const placements = [['ltr', 'start', 'left'], ['rtl', 'start', 'right'], ['ltr', 'end', 'right'],
    ['rtl', 'end', 'left']] as const;

  for (const [dir, side, edge] of placements) {
    test(`inset, ${dir}, side ${side}: the page is a card on the sidebar's ground`, async ({page}) => {
      await page.setViewportSize(DESKTOP);
      await openShell(page, {variant: 'inset', side, htmlAttributes: `dir="${dir}"`});
      const ground = await styleOf(page.locator('.sidebar-provider'));
      expect(ground.background).toBe((await styleOf(sidebar(page))).background);
      const card = await styleOf(inset(page));
      expect(card.background).not.toBe(ground.background);
      expect(card.radius).toBeGreaterThan(0);
      expect(card.shadow).not.toBe('none');
      expect(await rectOf(inset(page))).toMatchObject(edge === 'left'
          ? {left: 256, right: DESKTOP.width - 8, top: 8, bottom: DESKTOP.height - 8}
          : {left: 8, right: DESKTOP.width - 256, top: 8, bottom: DESKTOP.height - 8});
      // The sidebar has no edge of its own; its content keeps the card's margin from the screen's edges.
      expect(await styleOf(sidebar(page))).toMatchObject({borders: [0, 0, 0, 0], padding: 8});

      await trigger(page).click();
      await expect(sidebar(page)).toBeHidden();
      await expect.poll(() => rectOf(inset(page))).toMatchObject({left: 8, right: DESKTOP.width - 8, top: 8});
    });

    test(`floating, ${dir}, side ${side}: a panel with a margin all round`, async ({page}) => {
      await page.setViewportSize(DESKTOP);
      await openShell(page, {variant: 'floating', side, htmlAttributes: `dir="${dir}"`});
      expect(await rectOf(sidebar(page))).toMatchObject(edge === 'left'
          ? {left: 8, width: 240, top: 8, bottom: DESKTOP.height - 8}
          : {right: DESKTOP.width - 8, width: 240, top: 8, bottom: DESKTOP.height - 8});
      const panel = await styleOf(sidebar(page));
      expect(panel).toMatchObject({borders: [1, 1, 1, 1], borderStyle: 'solid'});
      expect(panel.radius).toBeGreaterThan(0);
      expect(panel.shadow).not.toBe('none');
      // Its footprint is the plain sidebar's, and the page beside it is no card.
      expect(await rectOf(inset(page))).toMatchObject(edge === 'left'
          ? {left: 256, right: DESKTOP.width, top: 0} : {left: 0, right: DESKTOP.width - 256, top: 0});
      expect((await styleOf(inset(page))).radius).toBe(0);

      await trigger(page).click();
      await expect(sidebar(page)).toBeHidden();
      await expect.poll(() => rectOf(inset(page))).toMatchObject({left: 0, right: DESKTOP.width});
      const collapsed = await rectOf(sidebar(page));
      expect(edge === 'left' ? collapsed.right <= 0 : collapsed.left >= DESKTOP.width, JSON.stringify(collapsed))
          .toBe(true);
    });
  }

  for (const variant of ['floating', 'inset'] as const) {
    test(`${variant} on a phone: the page and the panel as without a variant`, async ({page}) => {
      await page.setViewportSize(PHONE);
      await openShell(page, {variant});
      expect(await rectOf(inset(page))).toMatchObject({left: 0, right: PHONE.width, top: 0});
      expect((await styleOf(inset(page))).radius).toBe(0);
      await trigger(page).click();
      await expect.poll(() => rectOf(sidebar(page))).toMatchObject({left: 0, width: 288, top: 0, bottom: PHONE.height});
      expect(await styleOf(sidebar(page))).toMatchObject({radius: 0, padding: 0, borders: [0, 1, 0, 0]});
    });
  }

  test('inset: the card follows the collapse only without reduced motion', async ({page}) => {
    for (const reducedMotion of ['no-preference', 'reduce'] as const) {
      await page.emulateMedia({reducedMotion});
      await page.setViewportSize(DESKTOP);
      await openShell(page, {variant: 'inset'});
      await trigger(page).click();
      const moving = await inset(page).evaluate(element => element.getAnimations()
          .map(animation => (animation as CSSTransition).transitionProperty));
      expect(moving.includes('margin-inline-start') || moving.includes('margin-left'), `${reducedMotion}: ${moving}`)
          .toBe(reducedMotion === 'no-preference');
      await page.unrouteAll({behavior: 'ignoreErrors'});
      await page.context().clearCookies();
    }
  });

  test('forced colours: the inset card and the floating panel keep an edge', async ({page, browserName}) => {
    test.skip(browserName !== 'chromium', 'forced colours emulation is Chromium only');
    await page.emulateMedia({forcedColors: 'active'});
    await page.setViewportSize(DESKTOP);
    await openShell(page, {variant: 'inset'});
    expect(await styleOf(inset(page))).toMatchObject({borders: [1, 1, 1, 1], borderStyle: 'solid'});
    await page.unrouteAll({behavior: 'ignoreErrors'});
    await openShell(page, {variant: 'floating'});
    expect(await styleOf(sidebar(page))).toMatchObject({borders: [1, 1, 1, 1], borderStyle: 'solid'});
  });

  // The showcase's shell is the preview's, inset, in every skin: vega rounds the card, lyra keeps it square.
  for (const skin of skins) {
    test(`showcase, ${skin}: the inset card's corners`, async ({page}) => {
      await page.setViewportSize({width: 1280, height: 720});
      await openShowcase(page, skin, 'light');
      const card = await styleOf(page.locator('.sidebar-inset'));
      expect(card.radius > 0, `${skin}: ${card.radius}`).toBe(skin !== 'lyra');
      expect(card.shadow).not.toBe('none');
    });
  }
});

// The button skins give an expanded secondary, outline or ghost button the hover look (an open menu's trigger), as
// shadcn's. The sidebar's trigger is expanded whenever the sidebar is on a desktop, so it keeps its plain look.
for (const skin of skins) {
  test(`showcase, ${skin}: the expanded sidebar trigger does not look pressed, an open menu's trigger does`, async ({page}) => {
    await page.setViewportSize({width: 1280, height: 720});
    await openShowcase(page, skin, 'light');
    const background = (selector: string) => page.locator(selector).first()
        .evaluate(element => getComputedStyle(element).backgroundColor);
    const sidebarTrigger = page.locator('.sidebar-provider .sidebar-trigger');
    await expect(sidebarTrigger).toHaveAttribute('aria-expanded', 'true');
    expect(await background('.sidebar-provider .sidebar-trigger')).toBe('rgba(0, 0, 0, 0)');

    const menuTrigger = page.locator('.showcase .breadcrumb-ellipsis-trigger').first();
    expect(await background('.showcase .breadcrumb-ellipsis-trigger')).toBe('rgba(0, 0, 0, 0)');
    await menuTrigger.focus();
    await page.keyboard.press('Enter');
    await expect(menuTrigger).toHaveAttribute('aria-expanded', 'true');
    await page.mouse.move(0, 0);
    expect(await background('.showcase .breadcrumb-ellipsis-trigger')).not.toBe('rgba(0, 0, 0, 0)');
  });
}

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

// collapsible="icon": the strip of icons, its tooltips, and collapsible groups (sub-menus on details).
test.describe('collapsed to icons', () => {
  const ICON = 48;
  const collapseCookie = (page: Page) => page.context().addCookies([
    {name: 'sl-sidebar-state', value: 'collapsed', url: `${ORIGIN}/`}]);
  const link = (page: Page, name: string) => sidebar(page).getByRole('link', {name, exact: true});
  const tooltip = (page: Page, name: string) => link(page, name).locator('> .sidebar-menu-tooltip');
  const openTooltips = (page: Page) => page.evaluate(() => document.querySelectorAll('.sidebar-menu-tooltip:popover-open').length);
  const documents = (page: Page) => sidebar(page).locator('details.sidebar-menu-collapsible');
  const summary = (page: Page) => documents(page).locator('> summary');
  const isDetailsOpen = (page: Page) => documents(page).evaluate(element => (element as HTMLDetailsElement).open);

  test.beforeEach(async ({page}) => {
    await page.setViewportSize(DESKTOP);
  });

  test('the trigger narrows the sidebar to its icons; the labels stay the names', async ({page, context, browserName}) => {
    const messages = await openShell(page, {collapsible: 'icon'});
    await trigger(page).click();
    await expect(page.locator('.sidebar-provider')).toHaveAttribute('data-state', 'collapsed');
    await expect(trigger(page)).toHaveAttribute('aria-expanded', 'false');
    expect((await context.cookies()).find(cookie => cookie.name === 'sl-sidebar-state')?.value).toBe('collapsed');

    await expect.poll(() => box(page)).toMatchObject({left: 0, width: ICON});
    await expect.poll(async () => (await inset(page).boundingBox())!.width).toBe(DESKTOP.width - ICON);
    await expect(sidebar(page)).toBeVisible();
    // Every menu button is a 32 px square showing only its icon, in the middle of the strip; the label is hidden from
    // view but still the link's name. The large header button too (the team switcher, a button that opens a menu),
    // whose icon is smaller than the square.
    for (const name of ['Acme Inc. Enterprise', 'Home', 'Inbox', 'Calendar', 'Design Engineering', 'Settings', 'Help']) {
      const entry = name === 'Acme Inc. Enterprise'
          ? sidebar(page).getByRole('button', {name, exact: true}) : link(page, name);
      await expect(entry).toBeVisible();
      expect(await rectOf(entry), name).toMatchObject({width: 32});
      expect(await entry.locator('.sidebar-menu-button-label').evaluate(label => label.clientWidth), name)
          .toBeLessThanOrEqual(1);
      const icon = await rectOf(entry.locator('> svg').first());
      expect(Math.abs((icon.left + icon.right) / 2 - ICON / 2), name).toBeLessThanOrEqual(1);
    }
    // No text of a menu button is painted anywhere in the strip.
    expect(await sidebar(page).evaluate(nav => Array.from(nav.querySelectorAll('.sidebar-menu-button-label'))
        .filter(label => label.getBoundingClientRect().width > 1).length)).toBe(0);
    await expect(link(page, 'Inbox')).toHaveAccessibleDescription('12 unread');
    await expect(sidebar(page).getByRole('group', {name: 'Platform'})).toHaveCount(1);
    expect(await sidebar(page).locator('.sidebar-group-label').first()
        .evaluate(label => getComputedStyle(label).opacity)).toBe('0');
    // What the strip has no room for is hidden.
    expect(await rectOf(sidebar(page).locator('.sidebar-menu-badge'))).toMatchObject({width: 1});
    for (const selector of ['.sidebar-menu-action', '.sidebar-group-action',
      '.sidebar-menu-button-chevron', '.sidebar-menu-sub']) {
      await expect(sidebar(page).locator(selector).first(), selector).toBeHidden();
    }
    // Header and footer buttons sit in the middle of the strip.
    for (const selector of ['.sidebar-header .sidebar-menu-button', '.sidebar-footer .sidebar-menu-button']) {
      const button = await rectOf(sidebar(page).locator(selector).first());
      expect(Math.abs((button.left + button.right) / 2 - ICON / 2), selector).toBeLessThanOrEqual(1);
    }
    if (browserName === 'chromium') {
      const {roles} = await accessibilityTree(page);
      expect(roles).toEqual(expect.arrayContaining(['link:Home', 'link:Inbox', 'link:Calendar']));
    }
    const results = await new AxeBuilder({page}).withTags(TAGS).analyze();
    expect(results.violations.map(violation => violation.id)).toEqual([]);

    await trigger(page).click();
    await expect.poll(() => box(page)).toMatchObject({left: 0, width: 256});
    expect(await link(page, 'Home').locator('.sidebar-menu-button-label').evaluate(label => label.clientWidth))
        .toBeGreaterThan(0);
    expect(messages).toEqual([]);
  });

  test('the keyboard reaches only what the strip shows, in order', async ({page}) => {
    await collapseCookie(page);
    await openShell(page, {collapsible: 'icon'});
    const stops: string[] = [];
    for (let i = 0; i < 11; i++) {
      await page.keyboard.press('Tab');
      stops.push(await focused(page));
    }
    expect(stops).toEqual(['Skip to main content', 'sidebar: #team-switcher-trigger Acme Inc. Enterprise', 'sidebar: Home',
      'sidebar: Inbox',
      'sidebar: Calendar', 'sidebar: Documents', 'sidebar: Design Engineering', 'sidebar: Sales & Marketing',
      'sidebar: Settings', 'sidebar: Help', 'sidebar: Section']);
  });

  test.describe('without JavaScript', () => {
    test.use({javaScriptEnabled: false});

    test('the strip is painted from the cookie; the phone panel shows everything', async ({page}) => {
      await collapseCookie(page);
      await openShell(page, {collapsible: 'icon', alpine: false});
      expect(await box(page)).toMatchObject({left: 0, width: ICON});
      await expect(trigger(page)).toBeHidden();
      await expect(link(page, 'Home')).toBeVisible();
      expect(await rectOf(sidebar(page).locator('.sidebar-menu-badge'))).toMatchObject({width: 1});

      await page.setViewportSize(PHONE);
      await trigger(page).click();
      expect(await isOpen(page)).toBe(true);
      expect(await box(page)).toMatchObject({left: 0, width: 288});
      expect(await link(page, 'Home').locator('.sidebar-menu-button-label').evaluate(label => label.clientWidth))
          .toBeGreaterThan(100);
      await expect(sidebar(page).locator('.sidebar-menu-badge')).toBeVisible();
      await expect(summary(page).locator('.sidebar-menu-button-chevron')).toBeVisible();
      await expect(sidebar(page).locator('.sidebar-group-label').first()).toHaveCSS('opacity', '1');
    });
  });

  test('a tooltip shows the label on hover and keyboard focus, and describes nothing', async ({page, browserName}) => {
    await collapseCookie(page);
    const messages = await openShell(page, {collapsible: 'icon'});
    await page.waitForFunction(() => 'Alpine' in window);

    await link(page, 'Home').hover();
    await expect.poll(() => tooltip(page, 'Home').evaluate(element => element.matches(':popover-open'))).toBe(true);
    await expect(tooltip(page, 'Home')).toHaveText('Home');
    const button = await rectOf(link(page, 'Home'));
    const tip = await rectOf(tooltip(page, 'Home'));
    expect(tip.left).toBeGreaterThanOrEqual(button.right);
    await expect(link(page, 'Home')).toHaveAccessibleDescription('');
    await expect(link(page, 'Inbox')).toHaveAccessibleDescription('12 unread');
    if (browserName === 'chromium') {
      const {roles} = await accessibilityTree(page);
      expect(roles.filter(role => role.startsWith('tooltip'))).toEqual([]);
    }
    const results = await new AxeBuilder({page}).withTags(TAGS).analyze();
    expect(results.violations.map(violation => violation.id)).toEqual([]);
    // A click on the tooltip goes nowhere.
    await tooltip(page, 'Home').click();
    expect(new URL(page.url()).pathname).toBe('/');

    await page.mouse.move(700, 600);
    await expect.poll(() => openTooltips(page)).toBe(0);

    // Keyboard focus shows it at once; Escape hides it and leaves the focus.
    await link(page, 'Home').focus();
    await page.keyboard.press('Tab');
    await expect(link(page, 'Inbox')).toBeFocused();
    await expect.poll(() => tooltip(page, 'Inbox').evaluate(element => element.matches(':popover-open'))).toBe(true);
    await page.keyboard.press('Escape');
    await expect.poll(() => openTooltips(page)).toBe(0);
    await expect(link(page, 'Inbox')).toBeFocused();
    // The collapsible item's summary has one too.
    await page.keyboard.press('Tab');
    await page.keyboard.press('Tab');
    await expect.poll(() => summary(page).locator('.sidebar-menu-tooltip')
        .evaluate(element => element.matches(':popover-open'))).toBe(true);
    expect(messages).toEqual([]);
  });

  test('no tooltip while expanded, collapsed off-canvas, or on a phone', async ({page}) => {
    await openShell(page, {collapsible: 'icon'});
    await page.waitForFunction(() => 'Alpine' in window);
    await link(page, 'Home').hover();
    await page.waitForTimeout(600);
    expect(await openTooltips(page)).toBe(0);
    await link(page, 'Home').focus();
    await page.keyboard.press('Tab');
    await page.waitForTimeout(100);
    expect(await openTooltips(page)).toBe(0);

    // Collapsing hides a tooltip that shows; expanding again hides one too.
    await trigger(page).click();
    await link(page, 'Calendar').hover();
    await expect.poll(() => openTooltips(page)).toBe(1);
    await page.keyboard.press('ControlOrMeta+b');
    await expect.poll(() => openTooltips(page)).toBe(0);
    await expect(tooltip(page, 'Calendar')).toBeHidden();

    await page.setViewportSize(PHONE);
    await trigger(page).click();
    await link(page, 'Calendar').hover();
    await page.waitForTimeout(600);
    expect(await openTooltips(page)).toBe(0);
  });

  test('a collapsible item opens and closes its sub-menu natively, the chevron turning', async ({page}) => {
    for (const alpine of [true, false]) {
      await openShell(page, {collapsible: 'icon', alpine});
      const chevron = summary(page).locator('.sidebar-menu-button-chevron');
      expect(await isDetailsOpen(page)).toBe(false);
      await expect(link(page, 'Recent')).toBeHidden();
      await summary(page).click();
      expect(await isDetailsOpen(page)).toBe(true);
      await expect(link(page, 'Recent')).toBeVisible();
      await expect(link(page, 'Shared with me')).toBeVisible();
      await expect.poll(() => chevron.evaluate(element => getComputedStyle(element).rotate)).toBe('90deg');
      // Below the button, indented.
      expect((await rectOf(link(page, 'Recent'))).left).toBeGreaterThan((await rectOf(summary(page))).left);

      await summary(page).focus();
      await page.keyboard.press('Enter');
      expect(await isDetailsOpen(page)).toBe(false);
      await page.keyboard.press('Space');
      expect(await isDetailsOpen(page)).toBe(true);
      await page.unrouteAll({behavior: 'ignoreErrors'});
    }
  });

  test('the server opens the group holding the current page', async ({page}) => {
    await openShell(page, {collapsible: 'icon', path: '/documents/shared'});
    expect(await isDetailsOpen(page)).toBe(true);
    await expect(link(page, 'Shared with me')).toHaveAttribute('aria-current', 'page');
    await expect(link(page, 'Recent')).not.toHaveAttribute('aria-current');
    // The sub-button looks current, as a menu button does.
    const background = (name: string) => link(page, name).evaluate(element => getComputedStyle(element).backgroundColor);
    expect(await background('Recent')).toBe('rgba(0, 0, 0, 0)');
    expect(await background('Shared with me')).not.toBe('rgba(0, 0, 0, 0)');
  });

  test('collapsed, a group holding the current page marks its icon; the sub-menu stays hidden', async ({page}) => {
    await collapseCookie(page);
    await openShell(page, {collapsible: 'icon', path: '/documents/shared'});
    expect(await isDetailsOpen(page)).toBe(true);
    await expect(link(page, 'Shared with me')).toBeHidden();
    expect(await summary(page).evaluate(element => getComputedStyle(element).backgroundColor))
        .not.toBe('rgba(0, 0, 0, 0)');
    // Not aria-current: the summary is no link to the page.
    await expect(summary(page)).not.toHaveAttribute('aria-current');
  });

  for (const how of ['pointer', 'keyboard'] as const) {
    test(`collapsed, a group's icon expands the sidebar and opens the group: ${how}`, async ({page, context}) => {
      await collapseCookie(page);
      await openShell(page, {collapsible: 'icon'});
      await page.waitForFunction(() => 'Alpine' in window);
      if (how === 'pointer') {
        await summary(page).click();
      } else {
        await summary(page).focus();
        await page.keyboard.press('Enter');
      }
      await expect(page.locator('.sidebar-provider')).toHaveAttribute('data-state', 'expanded');
      expect(await isDetailsOpen(page)).toBe(true);
      await expect(link(page, 'Recent')).toBeVisible();
      await expect(summary(page)).toBeFocused();
      await expect(trigger(page)).toHaveAttribute('aria-expanded', 'true');
      expect((await context.cookies()).find(cookie => cookie.name === 'sl-sidebar-state')?.value).toBe('expanded');
      await expect.poll(() => openTooltips(page)).toBe(0);

      // An open group stays open: collapse again, then the icon again.
      await page.keyboard.press('ControlOrMeta+b');
      await expect(page.locator('.sidebar-provider')).toHaveAttribute('data-state', 'collapsed');
      await expect(link(page, 'Recent')).toBeHidden();
      await summary(page).click();
      await expect(page.locator('.sidebar-provider')).toHaveAttribute('data-state', 'expanded');
      expect(await isDetailsOpen(page)).toBe(true);
    });
  }

  const placements = [['ltr', 'start', 'left'], ['rtl', 'start', 'right'], ['ltr', 'end', 'right'],
    ['rtl', 'end', 'left']] as const;
  for (const [dir, side, edge] of placements) {
    test(`${dir}, side ${side}: the strip on the ${edge}, its tooltips towards the page`, async ({page}) => {
      await collapseCookie(page);
      await openShell(page, {collapsible: 'icon', side, htmlAttributes: `dir="${dir}"`});
      await page.waitForFunction(() => 'Alpine' in window);
      const strip = await box(page);
      expect(strip.width).toBe(ICON);
      expect(edge === 'left' ? strip.left : DESKTOP.width - strip.right).toBe(0);
      await link(page, 'Home').hover();
      await expect.poll(() => openTooltips(page)).toBe(1);
      const button = await rectOf(link(page, 'Home'));
      const tip = await rectOf(tooltip(page, 'Home'));
      if (edge === 'left') {
        expect(tip.left).toBeGreaterThanOrEqual(button.right);
      } else {
        expect(tip.right).toBeLessThanOrEqual(button.left);
      }
    });

    for (const variant of ['floating', 'inset'] as const) {
      test(`${variant}, ${dir}, side ${side}: the strip with its margin`, async ({page}) => {
        await collapseCookie(page);
        await openShell(page, {collapsible: 'icon', variant, side, htmlAttributes: `dir="${dir}"`});
        // floating: the strip plus its two borders, 8 px from the edge; inset: the strip plus its 8 px padding, the
        // card beside it.
        const expected = variant === 'floating' ? {width: ICON + 2, gap: 8, page: ICON + 2 + 16}
            : {width: ICON + 16, gap: 0, page: ICON + 16};
        const strip = await rectOf(sidebar(page));
        expect(strip.width).toBe(expected.width);
        expect(edge === 'left' ? strip.left : DESKTOP.width - strip.right).toBe(expected.gap);
        const card = await rectOf(inset(page));
        expect(edge === 'left' ? card.left : DESKTOP.width - card.right).toBe(expected.page);
        // Every menu button inside, the same square.
        const home = await rectOf(link(page, 'Home'));
        expect(home.width).toBe(32);
        expect(home.left).toBeGreaterThanOrEqual(strip.left);
        expect(home.right).toBeLessThanOrEqual(strip.right);
      });
    }
  }

  test('the strip narrows and widens only without reduced motion', async ({page}) => {
    for (const reducedMotion of ['no-preference', 'reduce'] as const) {
      await page.emulateMedia({reducedMotion});
      await openShell(page, {collapsible: 'icon'});
      await trigger(page).click();
      const moving = await sidebar(page).evaluate(element => element.getAnimations({subtree: true})
          .map(animation => (animation as CSSTransition).transitionProperty));
      expect(moving.includes('width'), `${reducedMotion}: ${moving}`).toBe(reducedMotion === 'no-preference');
      expect(moving.includes('margin-left') || moving.includes('margin-inline-start'), `${reducedMotion}: ${moving}`)
          .toBe(false);
      await page.unrouteAll({behavior: 'ignoreErrors'});
      await page.context().clearCookies();
    }
  });

  test('forced colours: the strip keeps its border, a group holding the current page an outline', async ({page, browserName}) => {
    test.skip(browserName !== 'chromium', 'forced colours emulation is Chromium only');
    await page.emulateMedia({forcedColors: 'active'});
    await collapseCookie(page);
    await openShell(page, {collapsible: 'icon', path: '/documents/shared'});
    expect(await sidebar(page).evaluate(element => getComputedStyle(element).borderInlineEndStyle)).toBe('solid');
    expect(await summary(page).evaluate(element => getComputedStyle(element).outlineStyle)).toBe('solid');
    expect(await link(page, 'Home').evaluate(element => getComputedStyle(element).outlineStyle)).toBe('none');
  });

  for (const boost of ['body', 'main'] as const) {
    test(`hx-boost on ${boost}: the next page keeps the strip, history brings it back without a tooltip`, async ({page}) => {
      await collapseCookie(page);
      const messages = await openShell(page, {collapsible: 'icon', boost});
      await page.waitForFunction(() => 'Alpine' in window);
      await link(page, 'Inbox').hover();
      await expect.poll(() => openTooltips(page)).toBe(1);
      await link(page, 'Inbox').click();
      await page.waitForURL('**/inbox');
      await expect(page.locator('h1')).toHaveText('Inbox');
      expect(await box(page)).toMatchObject({width: ICON});
      // Boosting only main leaves the sidebar, and its current page, as it was (see the docs' htmx section).
      await expect(link(page, boost === 'body' ? 'Inbox' : 'Home')).toHaveAttribute('aria-current', 'page');
      await page.mouse.move(700, 600);
      await expect.poll(() => openTooltips(page)).toBe(0);
      // The new page's tooltips work.
      await link(page, 'Calendar').hover();
      await expect.poll(() => openTooltips(page)).toBe(1);

      await page.goBack();
      await expect(page.locator('h1')).toHaveText('Home');
      expect(await box(page)).toMatchObject({width: ICON});
      await page.mouse.move(700, 600);
      await expect.poll(() => openTooltips(page)).toBe(0);
      await link(page, 'Home').hover();
      await expect.poll(() => openTooltips(page)).toBe(1);
      expect(messages).toEqual([]);
    });
  }

  // The showcase's shell collapses to icons, in every skin: the strip, square buttons, the large one without padding.
  for (const skin of skins) {
    test(`showcase, ${skin}: collapsed to icons`, async ({page}) => {
      await page.setViewportSize({width: 1280, height: 720});
      await openShowcase(page, skin, 'light');
      await page.waitForFunction(() => 'Alpine' in window);
      await page.locator('.sidebar-provider .sidebar-trigger').click();
      await expect.poll(async () => (await rectOf(page.locator('.sidebar-provider > .sidebar'))).width).toBe(ICON + 16);
      const square = await rectOf(page.locator('.sidebar-provider .sidebar-content .sidebar-menu-button').first());
      expect(square.width).toBe(32);
      expect(square.bottom - square.top).toBe(32);
      expect(await page.locator('.sidebar-provider .sidebar-header .sidebar-menu-button')
          .evaluate(element => getComputedStyle(element).paddingInlineStart)).toBe('0px');
      const results = await new AxeBuilder({page}).include('.sidebar-provider').withTags(TAGS).analyze();
      expect(results.violations.map(violation => violation.id)).toEqual([]);
      await page.context().clearCookies();
    });
  }
});

test.describe('collapsible none', () => {
  test('desktop: the sidebar stays, whatever the cookie, and the trigger is hidden', async ({page, context}) => {
    await page.setViewportSize(DESKTOP);
    await context.addCookies([{name: 'sl-sidebar-state', value: 'collapsed', url: `${ORIGIN}/`}]);
    await openShell(page, {collapsible: 'none'});
    await page.waitForFunction(() => 'Alpine' in window);
    expect(await box(page)).toMatchObject({left: 0, width: 256});
    await expect(sidebar(page)).toBeVisible();
    await expect(trigger(page)).toBeHidden();
    await page.keyboard.press('ControlOrMeta+b');
    await expect(page.locator('.sidebar-provider')).toHaveAttribute('data-state', 'collapsed');
    expect(await box(page)).toMatchObject({left: 0, width: 256});
    expect((await context.cookies()).find(cookie => cookie.name === 'sl-sidebar-state')?.value).toBe('collapsed');
  });

  test('phone: the panel as always', async ({page}) => {
    await page.setViewportSize(PHONE);
    await openShell(page, {collapsible: 'none'});
    await expect(trigger(page)).toBeVisible();
    await trigger(page).click();
    expect(await isOpen(page)).toBe(true);
    await expect.poll(() => focused(page)).toBe('sidebar: Home');
    await page.keyboard.press('ControlOrMeta+b');
    expect(await isOpen(page)).toBe(false);
  });
});

// Dropdown menus opened from sidebar parts (sl:dropdown-menu-trigger as="sidebar-menu-button" and
// as="sidebar-menu-action"): the preview's team switcher in the header and the "More" menu on Design Engineering, both
// side="right". Beside the sidebar on a desktop, expanded or collapsed to icons; below the trigger in the phone panel,
// above it, spared by the panel's inert, and closed by Escape before the panel.
test.describe('menus', () => {
  const teamTrigger = (page: Page) => page.locator('#team-switcher-trigger');
  const teamMenu = (page: Page) => page.locator('#team-switcher');
  const moreTrigger = (page: Page) => page.locator('#project-design-more-trigger');
  const moreMenu = (page: Page) => page.locator('#project-design-more');
  const menuOpen = (locator: ReturnType<Page['locator']>) => locator.evaluate(element => element.matches(':popover-open'));
  const rectOf = (locator: ReturnType<Page['locator']>) => locator.evaluate(element => {
    const rect = element.getBoundingClientRect();
    return {left: Math.round(rect.left), right: Math.round(rect.right), top: Math.round(rect.top),
      bottom: Math.round(rect.bottom), width: Math.round(rect.width)};
  });
  const collapseCookie = (page: Page) => page.context().addCookies([
    {name: 'sl-sidebar-state', value: 'collapsed', url: `${ORIGIN}/`}]);
  const teamTooltip = (page: Page) => teamTrigger(page).locator('> .sidebar-menu-tooltip');
  /** The element painted at the middle of `locator`: what a click there reaches. */
  const hit = (locator: ReturnType<Page['locator']>) => locator.evaluate(element => {
    const rect = element.getBoundingClientRect();
    const top = document.elementFromPoint(rect.left + rect.width / 2, rect.top + rect.height / 2);
    return !!top && (top === element || element.contains(top));
  });

  test.describe('desktop', () => {
    test.beforeEach(async ({page}) => {
      await page.setViewportSize(DESKTOP);
    });

    test('the team switcher opens beside the sidebar, the current team checked', async ({page}) => {
      const messages = await openShell(page);
      await expect(teamTrigger(page)).toHaveRole('button');
      await expect(teamTrigger(page)).toHaveAttribute('aria-haspopup', 'menu');
      await expect(teamTrigger(page)).toHaveAttribute('aria-expanded', 'false');
      const restBackground = await teamTrigger(page).evaluate(element => getComputedStyle(element).backgroundColor);

      await teamTrigger(page).focus();
      await page.keyboard.press('ArrowDown');
      await expect(teamMenu(page)).toBeVisible();
      await expect(teamTrigger(page)).toHaveAttribute('aria-expanded', 'true');
      await expect.poll(() => page.evaluate(() => document.activeElement?.textContent?.trim())).toBe('Acme Inc.');
      await expect(teamMenu(page).getByRole('menuitemradio', {name: 'Acme Inc.'})).toHaveAttribute('aria-checked', 'true');
      await expect(teamMenu(page).getByRole('menuitemradio', {name: 'Evil Corp.'})).toHaveAttribute('aria-checked', 'false');
      await expect(teamMenu(page).getByRole('group', {name: 'Teams'})).toHaveCount(1);

      // To the right of the trigger, its top lined up, at least as wide as the trigger.
      const button = await rectOf(teamTrigger(page));
      const menu = await rectOf(teamMenu(page));
      expect(menu.left).toBeGreaterThanOrEqual(button.right);
      expect(Math.abs(menu.top - button.top)).toBeLessThanOrEqual(1);
      expect(menu.width).toBeGreaterThanOrEqual(button.width);
      // The open trigger has the active look, also when the pointer is elsewhere.
      await page.mouse.move(DESKTOP.width - 10, DESKTOP.height - 10);
      expect(await teamTrigger(page).evaluate(element => getComputedStyle(element).backgroundColor)).not.toBe(restBackground);

      const results = await new AxeBuilder({page}).withTags(TAGS).analyze();
      expect(results.violations.map(violation => violation.id)).toEqual([]);

      await page.keyboard.press('Escape');
      await expect(teamMenu(page)).toBeHidden();
      await expect(teamTrigger(page)).toBeFocused();
      await expect(teamTrigger(page)).toHaveAttribute('aria-expanded', 'false');
      // The sidebar stays expanded: the menu is not the sidebar's trigger.
      await expect(page.locator('.sidebar-provider')).toHaveAttribute('data-state', 'expanded');
      expect(messages).toEqual([]);
    });

    test('a pointer opens the team switcher on the menu, and a click outside closes it', async ({page}) => {
      await openShell(page);
      await teamTrigger(page).click();
      await expect(teamMenu(page)).toBeVisible();
      await expect.poll(() => page.evaluate(() => document.activeElement?.id)).toBe('team-switcher');
      await page.locator('#in-main').click();
      await expect(teamMenu(page)).toBeHidden();
    });

    test('"More" is shown on hover, and stays while its menu is open', async ({page}) => {
      await openShell(page);
      const opacity = () => moreTrigger(page).evaluate(element => getComputedStyle(element).opacity);
      expect(await opacity()).toBe('0');
      await moreTrigger(page).focus();
      expect(await opacity()).toBe('1');
      await page.keyboard.press('Enter');
      await expect(moreMenu(page)).toBeVisible();
      await expect.poll(() => page.evaluate(() => document.activeElement?.textContent?.trim())).toBe('View project');
      await page.mouse.move(DESKTOP.width - 10, DESKTOP.height - 10);
      expect(await opacity()).toBe('1');
      const action = await rectOf(moreTrigger(page));
      const menu = await rectOf(moreMenu(page));
      expect(menu.left).toBeGreaterThanOrEqual(action.right);
      await page.keyboard.press('Escape');
      await expect(moreMenu(page)).toBeHidden();
      await expect(moreTrigger(page)).toBeFocused();
    });

    test('collapsed to icons: the menu opens beside the strip, and never with the tooltip', async ({page}) => {
      await collapseCookie(page);
      const messages = await openShell(page, {collapsible: 'icon'});
      expect((await rectOf(teamTrigger(page))).width).toBe(32);

      // Hover shows the tooltip; pressing the button hides it and opens the menu.
      await teamTrigger(page).hover();
      await expect(teamTooltip(page)).toBeVisible();
      await teamTrigger(page).click();
      await expect(teamMenu(page)).toBeVisible();
      await expect(teamTooltip(page)).toBeHidden();
      const button = await rectOf(teamTrigger(page));
      const menu = await rectOf(teamMenu(page));
      expect(menu.left).toBeGreaterThanOrEqual(button.right);
      // shadcn's team switcher menu: at least 14rem, though the trigger is a 32px square.
      expect(menu.width).toBeGreaterThanOrEqual(224);

      // The pointer leaves for the menu and comes back: still no tooltip while the menu is open.
      await page.mouse.move(menu.left + 20, menu.top + 20);
      await teamTrigger(page).hover();
      await page.waitForTimeout(600);
      await expect(teamTooltip(page)).toBeHidden();
      await page.keyboard.press('Escape');
      await expect(teamMenu(page)).toBeHidden();

      // Keyboard: focus shows the tooltip, opening the menu hides it.
      await page.locator('.skip-link').focus();
      await page.keyboard.press('Tab');
      await expect(teamTrigger(page)).toBeFocused();
      await expect(teamTooltip(page)).toBeVisible();
      await page.keyboard.press('Enter');
      await expect(teamMenu(page)).toBeVisible();
      await expect(teamTooltip(page)).toBeHidden();
      await expect.poll(() => page.evaluate(() => document.activeElement?.textContent?.trim())).toBe('Acme Inc.');
      await expect(teamTooltip(page)).toBeHidden();
      // Escape gives the focus back; the menu is closed, so the tooltip may show again.
      await page.keyboard.press('Escape');
      await expect(teamMenu(page)).toBeHidden();
      await expect(teamTrigger(page)).toBeFocused();
      await expect(page.locator('.sidebar-provider')).toHaveAttribute('data-state', 'collapsed');
      // The "More" action is not in the strip.
      await expect(moreTrigger(page)).toBeHidden();
      expect(messages).toEqual([]);
    });

    test('without JavaScript the trigger still opens the menu', async ({browser}) => {
      const context = await browser.newContext({javaScriptEnabled: false, viewport: DESKTOP});
      const page = await context.newPage();
      await openShell(page, {alpine: false});
      await teamTrigger(page).click();
      await expect(teamMenu(page)).toBeVisible();
      await page.keyboard.press('Escape');
      await expect(teamMenu(page)).toBeHidden();
      await context.close();
    });
  });

  test.describe('phone', () => {
    test.beforeEach(async ({page}) => {
      await page.setViewportSize(PHONE);
    });

    test('in the panel the menu opens below its trigger, above the panel and outside its inert', async ({page, browserName}) => {
      const messages = await openShell(page);
      await trigger(page).click();
      await expect(sidebar(page)).toBeVisible();
      await teamTrigger(page).click();
      await expect(teamMenu(page)).toBeVisible();
      expect(await isOpen(page)).toBe(true);

      const button = await rectOf(teamTrigger(page));
      const menu = await rectOf(teamMenu(page));
      expect(menu.top).toBeGreaterThanOrEqual(button.bottom);
      expect(Math.abs(menu.right - button.right)).toBeLessThanOrEqual(1);
      expect(menu.right).toBeLessThanOrEqual(PHONE.width);
      expect(await teamMenu(page).evaluate(element => !!element.closest('[inert]'))).toBe(false);
      for (const name of ['Acme Inc.', 'Evil Corp.', 'Add team']) {
        expect(await hit(teamMenu(page).getByRole(name === 'Add team' ? 'menuitem' : 'menuitemradio', {name})), name)
            .toBe(true);
      }
      if (browserName === 'chromium') {
        const {roles} = await accessibilityTree(page);
        expect(roles).toEqual(expect.arrayContaining(['menu:Acme Inc. Enterprise', 'menuitemradio:Acme Inc.']));
        expect(roles).not.toContain('button:In main');
      }
      const results = await new AxeBuilder({page}).withTags(TAGS)
          .disableRules(['landmark-one-main', 'page-has-heading-one']).analyze();
      expect(results.violations.map(violation => violation.id)).toEqual([]);
      expect(messages).toEqual([]);
    });

    test('Escape closes the menu first, the focus back on its trigger, then the panel', async ({page}) => {
      await openShell(page);
      await trigger(page).click();
      await teamTrigger(page).focus();
      await page.keyboard.press('ArrowDown');
      await expect(teamMenu(page)).toBeVisible();
      await expect.poll(() => page.evaluate(() => document.activeElement?.textContent?.trim())).toBe('Acme Inc.');
      await page.keyboard.press('Escape');
      await expect(teamMenu(page)).toBeHidden();
      expect(await isOpen(page)).toBe(true);
      // The browser leaves the focus on the body for a popover inside another; slDropdownMenu puts it back.
      await expect(teamTrigger(page)).toBeFocused();
      await page.keyboard.press('Escape');
      await expect(sidebar(page)).toBeHidden();
      await expect(trigger(page)).toBeFocused();
    });

    test('"More" is always shown, and its menu lines up with its end below it; a click in the panel closes only the menu', async ({page}) => {
      await openShell(page);
      await trigger(page).click();
      expect(await moreTrigger(page).evaluate(element => getComputedStyle(element).opacity)).toBe('1');
      expect((await rectOf(moreTrigger(page))).width).toBe(24);
      await moreTrigger(page).click();
      await expect(moreMenu(page)).toBeVisible();
      const action = await rectOf(moreTrigger(page));
      const menu = await rectOf(moreMenu(page));
      expect(menu.top).toBeGreaterThanOrEqual(action.bottom);
      expect(Math.abs(menu.right - action.right)).toBeLessThanOrEqual(1);
      expect(menu.left).toBeGreaterThanOrEqual(0);

      await sidebar(page).locator('.sidebar-group-label').first().click();
      await expect(moreMenu(page)).toBeHidden();
      expect(await isOpen(page)).toBe(true);
    });
  });

  test('right to left: beside the sidebar on its left, below in the panel', async ({page}) => {
    await page.setViewportSize(DESKTOP);
    await openShell(page, {htmlAttributes: 'dir="rtl"'});
    await teamTrigger(page).click();
    await expect(teamMenu(page)).toBeVisible();
    let button = await rectOf(teamTrigger(page));
    let menu = await rectOf(teamMenu(page));
    expect(menu.right).toBeLessThanOrEqual(button.left);
    await page.keyboard.press('Escape');

    await page.setViewportSize(PHONE);
    await trigger(page).click();
    await expect(sidebar(page)).toBeVisible();
    await teamTrigger(page).click();
    await expect(teamMenu(page)).toBeVisible();
    button = await rectOf(teamTrigger(page));
    menu = await rectOf(teamMenu(page));
    expect(menu.top).toBeGreaterThanOrEqual(button.bottom);
    expect(Math.abs(menu.left - button.left)).toBeLessThanOrEqual(1);
  });
});

// The showcase's shell, in every skin and theme: the team switcher's menu opens beside the sidebar on a desktop and
// below its trigger in the phone panel, with no axe violation, and its open trigger looks pressed.
for (const skin of skins) {
  for (const theme of ['light', 'dark'] as const) {
    test(`showcase, ${skin}, ${theme}: the team switcher and "More" menus`, async ({page}) => {
      await page.setViewportSize({width: 1280, height: 720});
      await openShowcase(page, skin, theme);
      const switcher = page.locator('#team-switcher-trigger');
      const rest = await switcher.evaluate(element => getComputedStyle(element).backgroundColor);
      await switcher.focus();
      await page.keyboard.press('Enter');
      await expect(page.locator('#team-switcher')).toBeVisible();
      await page.mouse.move(1270, 710);
      expect(await switcher.evaluate(element => getComputedStyle(element).backgroundColor)).not.toBe(rest);
      let results = await new AxeBuilder({page}).withTags(TAGS).include('.sidebar-provider').analyze();
      expect(results.violations.map(violation => violation.id)).toEqual([]);
      await page.keyboard.press('Escape');
      await expect(switcher).toBeFocused();

      const more = page.locator('#project-design-more-trigger');
      await more.focus();
      await page.keyboard.press('Enter');
      await expect(page.locator('#project-design-more')).toBeVisible();
      results = await new AxeBuilder({page}).withTags(TAGS).include('.sidebar-provider').analyze();
      expect(results.violations.map(violation => violation.id)).toEqual([]);
      await page.keyboard.press('Escape');
      await expect(more).toBeFocused();

      await page.setViewportSize({width: 390, height: 720});
      await page.locator('.sidebar-provider .sidebar-trigger').click();
      await expect(page.locator('#sidebar')).toBeVisible();
      await switcher.click();
      await expect(page.locator('#team-switcher')).toBeVisible();
      const button = (await switcher.boundingBox())!;
      const menu = (await page.locator('#team-switcher').boundingBox())!;
      expect(menu.y).toBeGreaterThanOrEqual(button.y + button.height);
      results = await new AxeBuilder({page}).withTags(TAGS).include('#sidebar')
          .disableRules(['landmark-one-main', 'page-has-heading-one']).analyze();
      expect(results.violations.map(violation => violation.id)).toEqual([]);
    });
  }
}
