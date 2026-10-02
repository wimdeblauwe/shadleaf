import {expect, test, type Page} from '@playwright/test';
import AxeBuilder from '@axe-core/playwright';
import previews from '../src/generated/previews.json' with {type: 'json'};
import {ORIGIN, openFixture, servePhotos, type FixtureOptions} from './fixture';

// The header layout: sl:site-header with a sl:sidebar placement="header" in it. Fixture pages under a strict
// Content-Security-Policy with the csp Alpine build. From 768 px the one <nav popover> is a row of links in the header
// (no dialog role, nothing to collapse, panel-only parts left out, scrolling sideways when the links do not fit); below
// it the same element is sidebar.spec.ts's panel (focus, inert, Escape, links, the breakpoint, htmx). The markup is the
// docs preview's (sidebar.yaml, scenario header). The showcase checks run on /showcase/header/ (a11y.spec.ts,
// focus.spec.ts).

const TAGS = ['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa', 'wcag22aa', 'best-practice'];
const DESKTOP = {width: 1024, height: 700};
const PHONE = {width: 390, height: 700};

type Scenario = { id: string, html: string };
const PREVIEW = servePhotos((previews.scenarios as Scenario[]).find(scenario => scenario.id === 'sidebar--header')!.html);

type ShellOptions = { title?: string, current?: string, side?: 'start' | 'end', inset?: boolean, more?: number };

/**
 * The preview's header layout as a server renders it for a page: the current page's link (#slNav.current; the panel's
 * large brand button never), a same-page link at the end of the row, a control in main. more adds that many links to
 * the row, to make it overflow.
 */
function shell({title = 'Home', current = '/', side = 'start', inset = true, more = 0}: ShellOptions = {}) {
  const extra = Array.from({length: more}, (_, i) =>
      `<li class="sidebar-menu-item"><a class="sidebar-menu-button" href="/reports/${i + 1}">`
      + `<span class="sidebar-menu-button-label">Quarterly report ${i + 1}</span></a></li>`).join('');
  return PREVIEW
      .replace(/(<a class="sidebar-menu-button"[^>]*?)\s*aria-current="page"/g, '$1')
      .replace(new RegExp(`(<a class="sidebar-menu-button")((?:(?!data-size="lg")[^>])*? href="${current}")`),
          '$1 aria-current="page"$2')
      .replace(' data-variant="inset"', inset ? ' data-variant="inset"' : '')
      .replace('data-placement="header"', `data-placement="header"${side === 'end' ? ' data-side="end"' : ''}`)
      .replace(/(<a class="sidebar-menu-button" href="\/documents">[\s\S]*?<\/li>)/,
          `$1${extra}<li class="sidebar-menu-item"><a class="sidebar-menu-button" href="#section">`
          + '<span class="sidebar-menu-button-label">Section</span></a></li>')
      .replace('<h1>Home</h1>', `<h1>${title}</h1><button id="in-main" type="button">In main</button>`
          + '<p id="section">Section</p>');
}

type OpenOptions = FixtureOptions & Omit<ShellOptions, 'title' | 'current'> & { boost?: boolean };

/** Serves the layout at / and at /inbox, /calendar, /settings and /help. */
async function openLayout(page: Page, {side, inset, more, boost, ...options}: OpenOptions = {}) {
  const page_ = (title: string, current: string) => () => ({body: shell({title, current, side, inset, more})});
  return openFixture(page, () => shell({side, inset, more}), {
    wrap: false,
    htmx: !!boost,
    bodyAttributes: boost ? 'hx-boost="true"' : '',
    routes: {
      '/inbox': page_('Inbox', '/inbox'), '/calendar': page_('Calendar', '/calendar'),
      '/settings': page_('Settings', '/settings'), '/help': page_('Help', '/help'),
    },
    ...options,
  });
}

const nav = (page: Page) => page.locator('#sidebar');
const trigger = (page: Page) => page.locator('.sidebar-trigger');
const link = (page: Page, name: string) => nav(page).getByRole('link', {name, exact: true});
const isOpen = (page: Page) => nav(page).evaluate(element => element.matches(':popover-open'));
const inertElements = (page: Page) => page.evaluate(() => document.querySelectorAll('[inert]').length);
const rect = (page: Page, selector: string) => page.locator(selector).first().evaluate(element => {
  const box = element.getBoundingClientRect();
  return {left: Math.round(box.left), right: Math.round(box.right), top: Math.round(box.top),
    bottom: Math.round(box.bottom), width: Math.round(box.width)};
});
/** The focused element: in the nav or not, and its text (or name). */
const focused = (page: Page) => page.evaluate(() => {
  const element = document.activeElement!;
  const text = (element.cloneNode(true) as Element);
  text.querySelectorAll('[aria-hidden="true"]').forEach(hidden => hidden.remove());
  return element === document.body ? 'body'
      : `${element.closest('#sidebar') ? 'nav: ' : ''}${
          element.getAttribute('aria-label') || text.textContent?.replace(/\s+/g, ' ').trim()}`;
});

type AxNode = { ignored: boolean, role?: { value: string }, name?: { value: string } };

/** Chromium: the unignored nodes of the accessibility tree, as role:name. */
async function roles(page: Page) {
  const cdp = await page.context().newCDPSession(page);
  const {nodes} = await cdp.send('Accessibility.getFullAXTree') as { nodes: AxNode[] };
  return nodes.filter(node => !node.ignored).map(node => `${node.role?.value}:${node.name?.value ?? ''}`);
}

const PANEL_ONLY = ['Marketing site', 'Android app', 'Settings', 'Help'];

test.describe('desktop', () => {
  test.beforeEach(async ({page}) => {
    await page.setViewportSize(DESKTOP);
  });

  test('the nav is a row of links in the banner, a navigation landmark and no dialog', async ({page, browserName}) => {
    const messages = await openLayout(page);

    await expect(nav(page)).toBeVisible();
    expect(await isOpen(page)).toBe(false);
    await expect(page.getByRole('banner')).toHaveCount(1);
    await expect(page.getByRole('banner').getByRole('navigation', {name: 'Main'})).toHaveCount(1);
    await expect(page.getByRole('dialog')).toHaveCount(0);
    // One row inside the header: every link of the row on the same line.
    const header = await rect(page, '.site-header');
    const tops = new Set<number>();
    for (const name of ['Home', 'Inbox', 'Calendar', 'Documents']) {
      const box = await link(page, name).boundingBox();
      tops.add(Math.round(box!.y));
      expect(box!.y >= header.top && box!.y + box!.height <= header.bottom, name).toBe(true);
    }
    expect(tops.size).toBe(1);
    // Nothing to collapse: the trigger is hidden and claims no state.
    await expect(trigger(page)).toBeHidden();
    await expect(trigger(page)).not.toHaveAttribute('popovertarget');
    await expect(trigger(page)).not.toHaveAttribute('aria-expanded');
    if (browserName === 'chromium') {
      const tree = await roles(page);
      expect(tree).toContain('navigation:Main');
      expect(tree).toContain('banner:');
      expect(tree.filter(role => role.startsWith('dialog:'))).toEqual([]);
    }
    const results = await new AxeBuilder({page}).withTags(TAGS).analyze();
    expect(results.violations.map(violation => violation.id)).toEqual([]);
    expect(messages).toEqual([]);
  });

  test('the current page is marked, and the keyboard goes through the row in order', async ({page}) => {
    await openLayout(page);
    await expect(link(page, 'Home')).toHaveAttribute('aria-current', 'page');
    const background = (name: string) => link(page, name).evaluate(element => getComputedStyle(element).backgroundColor);
    expect(await background('Home')).not.toBe(await background('Calendar'));
    expect(await link(page, 'Home').evaluate(element => getComputedStyle(element).fontWeight)).toBe('500');

    const stops: string[] = [];
    for (let i = 0; i < 10; i++) {
      await page.keyboard.press('Tab');
      stops.push(await focused(page));
    }
    expect(stops).toEqual(['Skip to main content', 'Acme Inc.', 'nav: Home', 'nav: Inbox', 'nav: Calendar',
      'nav: Documents', 'nav: More for Documents', 'nav: Section', 'Search', 'Theme']);
  });

  test('the badge stays beside its link and describes it', async ({page}) => {
    await openLayout(page);
    const inbox = await rect(page, '#sidebar a[href="/inbox"]');
    const badge = await rect(page, '#header-inbox-badge');
    expect(badge.left >= inbox.left && badge.right <= inbox.right, JSON.stringify({inbox, badge})).toBe(true);
    await expect(link(page, 'Inbox')).toHaveAccessibleDescription('12 unread');
  });

  test('panel-only parts, group labels and icons are left out of the row', async ({page}) => {
    await openLayout(page);
    for (const name of PANEL_ONLY) {
      await expect(nav(page).locator('a', {hasText: name})).toBeHidden();
    }
    await expect(nav(page).locator('.sidebar-header')).toBeHidden();
    await expect(nav(page).locator('.sidebar-footer')).toBeHidden();
    await expect(link(page, 'Home').locator('svg')).toBeHidden();
  });

  test('Ctrl/Cmd+B and a collapsed cookie change nothing', async ({page, context}) => {
    await context.addCookies([{name: 'sl-sidebar-state', value: 'collapsed', url: `${ORIGIN}/`}]);
    await openLayout(page);
    const before = await rect(page, '#sidebar');
    await page.locator('#in-main').focus();
    await page.keyboard.press('ControlOrMeta+b');
    expect(await isOpen(page)).toBe(false);
    expect(await rect(page, '#sidebar')).toEqual(before);
    await expect(link(page, 'Home')).toBeVisible();
    expect((await context.cookies()).find(cookie => cookie.name === 'sl-sidebar-state')?.value).toBe('collapsed');
  });

  test('variant="inset" makes the page a card below the header; without it the header has a border', async ({page}) => {
    await openLayout(page);
    const card = await rect(page, '.sidebar-inset');
    const header = await rect(page, '.site-header');
    expect(card.left).toBe(8);
    expect(card.right).toBe(DESKTOP.width - 8);
    expect(card.top).toBe(header.bottom);
    expect(await page.evaluate(() => document.documentElement.scrollWidth)).toBe(DESKTOP.width);
    expect(await page.locator('.site-header').evaluate(element => getComputedStyle(element).borderBottomWidth))
        .toBe('0px');

    await page.unrouteAll({behavior: 'ignoreErrors'});
    await openLayout(page, {inset: false});
    expect(await rect(page, '.sidebar-inset')).toMatchObject({left: 0, width: DESKTOP.width});
    expect(await page.locator('.site-header').evaluate(element => getComputedStyle(element).borderBottomWidth))
        .toBe('1px');
  });

  test('links that do not fit scroll sideways; a focused one scrolls into view', async ({page}) => {
    await page.setViewportSize({width: 800, height: 600});
    await openLayout(page, {more: 6});
    const sizes = await nav(page).evaluate(element => ({scroll: element.scrollWidth, client: element.clientWidth}));
    expect(sizes.scroll).toBeGreaterThan(sizes.client);
    // The page itself does not scroll sideways, and the end of the header stays in view.
    expect(await page.evaluate(() => document.documentElement.scrollWidth)).toBe(800);
    expect((await rect(page, '.site-header-end')).right).toBeLessThanOrEqual(800);
    const last = link(page, 'Quarterly report 6');
    await last.focus();
    await expect.poll(() => nav(page).evaluate(element => element.scrollLeft)).toBeGreaterThan(0);
    const box = await last.boundingBox();
    const navBox = await rect(page, '#sidebar');
    expect(box!.x + box!.width).toBeLessThanOrEqual(navBox.right + 1);
  });

  test.describe('without JavaScript', () => {
    test.use({javaScriptEnabled: false});

    test('the row is the same and the trigger hidden; on a phone the panel opens natively', async ({page}) => {
      await openLayout(page, {alpine: false});
      await expect(link(page, 'Calendar')).toBeVisible();
      await expect(trigger(page)).toBeHidden();

      await page.setViewportSize(PHONE);
      await expect(trigger(page)).toBeVisible();
      await trigger(page).click();
      expect(await isOpen(page)).toBe(true);
      await expect(nav(page).locator('a', {hasText: 'Help'})).toBeVisible();
    });
  });
});

test.describe('phone', () => {
  test.beforeEach(async ({page}) => {
    await page.setViewportSize(PHONE);
  });

  test('the menu button opens the panel with the panel-only parts, the focus in it and the page inert', async ({page, browserName}) => {
    const messages = await openLayout(page);
    await expect(nav(page)).toBeHidden();
    await expect(trigger(page)).toBeVisible();
    await expect(trigger(page)).toHaveAttribute('popovertarget', 'sidebar');
    await expect(trigger(page)).toHaveAttribute('aria-expanded', 'false');
    // The header's end stays at the end with the row gone.
    expect((await rect(page, '.site-header-end')).right).toBe(PHONE.width - 16);

    await trigger(page).focus();
    await page.keyboard.press('Enter');
    expect(await isOpen(page)).toBe(true);
    await expect.poll(() => focused(page)).toBe('nav: Home');
    await expect(trigger(page)).toHaveAttribute('aria-expanded', 'true');
    for (const name of PANEL_ONLY) {
      await expect(nav(page).locator('a', {hasText: name})).toBeVisible();
    }
    await expect(nav(page).getByRole('group', {name: 'Favorites'})).toBeVisible();
    await expect(link(page, 'Home').locator('svg')).toBeVisible();
    // The header around the nav is inert too: Tab stays in the panel.
    const stops: string[] = [];
    for (let i = 0; i < 14; i++) {
      await page.keyboard.press('Tab');
      stops.push(await focused(page));
    }
    expect(stops.filter(stop => !stop.startsWith('nav: ') && stop !== 'body'), stops.join(', ')).toEqual([]);
    expect(await page.locator('.site-header-end').evaluate(element => (element as HTMLElement).inert)).toBe(true);
    expect(await page.locator('.sidebar-inset').evaluate(element => (element as HTMLElement).inert)).toBe(true);
    if (browserName === 'chromium') {
      const tree = await roles(page);
      expect(tree).toContain('navigation:Main');
      expect(tree.filter(role => /^(main|heading|dialog):/.test(role)
          || ['button:Menu', 'button:Search', 'button:In main'].includes(role))).toEqual([]);
    }
    const results = await new AxeBuilder({page}).withTags(TAGS)
        .disableRules(['landmark-one-main', 'page-has-heading-one']).analyze();
    expect(results.violations.map(violation => violation.id)).toEqual([]);

    await page.keyboard.press('Escape');
    expect(await isOpen(page)).toBe(false);
    await expect(trigger(page)).toBeFocused();
    expect(await inertElements(page)).toBe(0);
    expect(messages).toEqual([]);
  });

  test('a click outside closes it, and the page is usable again', async ({page}) => {
    await openLayout(page);
    await trigger(page).click();
    await expect.poll(() => focused(page)).toBe('nav: Home');
    await page.mouse.click(370, 400);
    expect(await isOpen(page)).toBe(false);
    expect(await inertElements(page)).toBe(0);
    await page.locator('#in-main').focus();
    await expect(page.locator('#in-main')).toBeFocused();
  });

  test('a same-page link closes it; a followed one renders the next page closed', async ({page}) => {
    await openLayout(page);
    await trigger(page).click();
    await link(page, 'Section').click();
    expect(await isOpen(page)).toBe(false);
    expect(new URL(page.url()).hash).toBe('#section');

    await trigger(page).click();
    await nav(page).locator('a', {hasText: 'Settings'}).click();
    await page.waitForURL('**/settings');
    await expect(page.locator('h1')).toHaveText('Settings');
    expect(await isOpen(page)).toBe(false);
  });

  test('hx-boost: a link closes the panel, and history brings the page back closed', async ({page}) => {
    const messages = await openLayout(page, {boost: true});
    await trigger(page).click();
    await expect.poll(() => focused(page)).toBe('nav: Home');
    await link(page, 'Inbox').click();
    await page.waitForURL('**/inbox');
    await expect(page.locator('h1')).toHaveText('Inbox');
    expect(await isOpen(page)).toBe(false);
    expect(await inertElements(page)).toBe(0);
    // The closed panel's links are hidden, so not by role.
    await expect(nav(page).locator('a[href="/inbox"]')).toHaveAttribute('aria-current', 'page');

    await trigger(page).click();
    expect(await isOpen(page)).toBe(true);
    await page.goBack();
    await expect(page.locator('h1')).toHaveText('Home');
    expect(await isOpen(page)).toBe(false);
    expect(await inertElements(page)).toBe(0);
    await trigger(page).click();
    await expect.poll(() => focused(page)).toBe('nav: Home');
    expect(messages).toEqual([]);
  });

  test('crossing 768 px closes the panel and turns it into the row', async ({page}) => {
    await openLayout(page);
    await trigger(page).click();
    expect(await isOpen(page)).toBe(true);

    await page.setViewportSize(DESKTOP);
    await expect.poll(() => isOpen(page)).toBe(false);
    expect(await inertElements(page)).toBe(0);
    await expect(link(page, 'Calendar')).toBeVisible();
    await expect(trigger(page)).toBeHidden();
    await expect(trigger(page)).not.toHaveAttribute('popovertarget');
    await expect(trigger(page)).not.toHaveAttribute('aria-expanded');

    await page.setViewportSize(PHONE);
    await expect(trigger(page)).toHaveAttribute('popovertarget', 'sidebar');
    await expect(trigger(page)).toHaveAttribute('aria-expanded', 'false');
    await expect(nav(page)).toBeHidden();
  });
});

test.describe('direction and side', () => {
  for (const [dir, side, edge] of [['ltr', 'start', 'left'], ['rtl', 'start', 'right'], ['ltr', 'end', 'right'],
    ['rtl', 'end', 'left']] as const) {
    test(`${dir}, side ${side}: the row follows the line, the panel comes from the ${edge}`, async ({page}) => {
      await page.emulateMedia({reducedMotion: 'no-preference'});
      await page.setViewportSize(DESKTOP);
      await openLayout(page, {side, htmlAttributes: `dir="${dir}"`});
      // The row runs in the line's direction, whatever the side.
      const home = await rect(page, '#sidebar a[href="/"]:not([data-size])');
      const calendar = await rect(page, '#sidebar a[href="/calendar"]');
      expect(dir === 'ltr' ? home.left < calendar.left : home.left > calendar.left).toBe(true);
      expect(await nav(page).evaluate(element => getComputedStyle(element).order)).toBe('0');

      await page.setViewportSize(PHONE);
      await expect.poll(() => nav(page).evaluate(element => element.getAnimations().length)).toBe(0);
      await trigger(page).click();
      const translate = await nav(page).evaluate(element => element.getAnimations()
          .map(animation => (animation.effect as KeyframeEffect).getKeyframes())
          .flat().map(frame => frame.translate).filter(Boolean));
      expect(translate[0]).toBe(edge === 'left' ? '-100%' : '100%');
      await expect.poll(async () => {
        const box = await rect(page, '#sidebar');
        return edge === 'left' ? box.left === 0 : box.right === PHONE.width;
      }).toBe(true);
    });
  }
});

test('the panel slides only without reduced motion; the row never moves', async ({page}) => {
  for (const reducedMotion of ['no-preference', 'reduce'] as const) {
    await page.emulateMedia({reducedMotion});
    await page.setViewportSize(PHONE);
    await openLayout(page);
    await trigger(page).click();
    const opening = await nav(page).evaluate(element => element.getAnimations()
        .map(animation => (animation as CSSTransition).transitionProperty));
    expect(opening.includes('translate'), `${reducedMotion}: ${opening}`).toBe(reducedMotion === 'no-preference');
    await page.keyboard.press('Escape');

    await page.setViewportSize(DESKTOP);
    await expect.poll(() => nav(page).evaluate(element => element.getAnimations().length)).toBe(0);
    await page.unrouteAll({behavior: 'ignoreErrors'});
  }
});

test('forced colours: the current page keeps an outline in the row, the focus a thicker one', async ({page, browserName}) => {
  test.skip(browserName !== 'chromium', 'forced colours emulation is Chromium only');
  await page.emulateMedia({forcedColors: 'active'});
  await page.setViewportSize(DESKTOP);
  await openLayout(page);
  const outline = (name: string) => link(page, name).evaluate(element => {
    const style = getComputedStyle(element);
    return {style: style.outlineStyle, width: parseFloat(style.outlineWidth)};
  });
  expect(await outline('Home')).toMatchObject({style: 'solid', width: 1});
  expect((await outline('Calendar')).style).toBe('none');
  await link(page, 'Inbox').focus();
  await page.keyboard.press('Tab');
  expect(await outline('Calendar')).toMatchObject({style: 'solid', width: 2});
  // The card below the header keeps an edge.
  expect(await page.locator('.sidebar-inset').evaluate(element => getComputedStyle(element).borderTopStyle))
      .toBe('solid');

  await page.setViewportSize(PHONE);
  await trigger(page).click();
  expect(await nav(page).evaluate(element => getComputedStyle(element, '::backdrop').backgroundColor))
      .not.toBe('rgba(0, 0, 0, 0)');
});

// A dropdown menu opened from the row (sl:dropdown-menu-trigger as="sidebar-menu-action" on Documents, side="right"):
// below its trigger in the row, lined up with its end, as in the phone panel; the row's scrolling does not clip it.
test.describe('menus', () => {
  const moreTrigger = (page: Page) => page.locator('#header-documents-more-trigger');
  const moreMenu = (page: Page) => page.locator('#header-documents-more');

  test('desktop: "More" in the row opens its menu below it, and Escape gives the focus back', async ({page}) => {
    await page.setViewportSize(DESKTOP);
    const messages = await openLayout(page);
    await expect(moreTrigger(page)).toBeVisible();
    expect((await rect(page, '#header-documents-more-trigger')).width).toBe(24);
    await moreTrigger(page).focus();
    await page.keyboard.press('Enter');
    await expect(moreMenu(page)).toBeVisible();
    await expect.poll(() => focused(page)).toBe('nav: New document');
    const action = await rect(page, '#header-documents-more-trigger');
    const menu = await rect(page, '#header-documents-more');
    expect(menu.top).toBeGreaterThanOrEqual(action.bottom);
    expect(Math.abs(menu.right - action.right)).toBeLessThanOrEqual(1);
    // Painted above the row: the row's horizontal scrolling does not clip it.
    expect(menu.bottom).toBeGreaterThan(await nav(page).evaluate(element => element.getBoundingClientRect().bottom));
    const results = await new AxeBuilder({page}).withTags(TAGS).analyze();
    expect(results.violations.map(violation => violation.id)).toEqual([]);
    await page.keyboard.press('Escape');
    await expect(moreMenu(page)).toBeHidden();
    await expect(moreTrigger(page)).toBeFocused();
    expect(messages).toEqual([]);
  });

  test('desktop: the menu follows its trigger when the row scrolls', async ({page}) => {
    await page.setViewportSize(DESKTOP);
    await openLayout(page, {more: 8});
    await nav(page).evaluate(element => element.scrollLeft = 40);
    await moreTrigger(page).click();
    await expect(moreMenu(page)).toBeVisible();
    const action = await rect(page, '#header-documents-more-trigger');
    const menu = await rect(page, '#header-documents-more');
    expect(Math.abs(menu.right - action.right)).toBeLessThanOrEqual(1);
  });

  test('phone: in the panel the menu opens below "More", and Escape closes it before the panel', async ({page}) => {
    await page.setViewportSize(PHONE);
    await openLayout(page);
    await trigger(page).click();
    await expect(nav(page)).toBeVisible();
    await moreTrigger(page).click();
    await expect(moreMenu(page)).toBeVisible();
    const action = await rect(page, '#header-documents-more-trigger');
    const menu = await rect(page, '#header-documents-more');
    expect(menu.top).toBeGreaterThanOrEqual(action.bottom);
    expect(Math.abs(menu.right - action.right)).toBeLessThanOrEqual(1);
    expect(await moreMenu(page).evaluate(element => !!element.closest('[inert]'))).toBe(false);
    await page.keyboard.press('ArrowDown');
    await page.keyboard.press('Escape');
    await expect(moreMenu(page)).toBeHidden();
    expect(await isOpen(page)).toBe(true);
    await expect(moreTrigger(page)).toBeFocused();
    await page.keyboard.press('Escape');
    expect(await isOpen(page)).toBe(false);
  });
});
