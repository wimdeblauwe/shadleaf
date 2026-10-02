import {expect, test, type Locator, type Page} from '@playwright/test';
import AxeBuilder from '@axe-core/playwright';
import previews from '../src/generated/previews.json' with {type: 'json'};
import {openFixture, servePhotos, type FixtureOptions, type Request} from './fixture';
import {openShowcase, skins, themes} from './showcase';

// sl:user-menu: the signed-in user's menu in the sidebar's footer (the sidebar's showcase shell, sidebar--default) and
// in the header layout's site header (sidebar--header), and the Sign in link (user-menu--anonymous*). Fixture pages
// under a strict Content-Security-Policy with the csp Alpine build. Sign out posts its form, which the server
// rendered with the CSRF token (UserMenuComponentTest checks th:action adds it; here it is put in as the server would).

const TAGS = ['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa', 'wcag22aa', 'best-practice'];
const DESKTOP = {width: 1024, height: 700};
const PHONE = {width: 390, height: 700};
const TOKEN = 'b2a1e7c4-csrf';

type Scenario = { id: string, html: string };
const preview = (id: string) => servePhotos((previews.scenarios as Scenario[]).find(scenario => scenario.id === id)!.html);
/** The form as the server renders it, with Spring Security's CSRF token. */
const withToken = (html: string) =>
    html.replace(/(<form id="[^"]*-sign-out"[^>]*>)/g, `$1<input type="hidden" name="_csrf" value="${TOKEN}">`);
const SIDEBAR = withToken(preview('sidebar--default'));
const HEADER = withToken(preview('sidebar--header'));
const ANONYMOUS_HEADER = preview('user-menu--anonymous');
const ANONYMOUS_SIDEBAR = preview('user-menu--anonymous-sidebar');

type Posted = { body: string, headers: Record<string, string> };

/** Serves `html` at /, and records what reaches /logout and /login. */
async function open(page: Page, html: string, options: FixtureOptions = {}) {
  const requests: Record<string, Posted[]> = {'/logout': [], '/login': []};
  const record = (path: string) => (request: Request) => {
    requests[path].push({body: request.body, headers: request.headers});
    return {body: path === '/logout' ? '<h1>Signed out</h1>' : '<h1>Sign in</h1>'};
  };
  const messages = await openFixture(page, html, {
    wrap: false,
    routes: {'/logout': record('/logout'), '/login': record('/login')},
    ...options,
  });
  return {messages, requests};
}

const trigger = (page: Page, id = 'user-menu') => page.locator(`#${id}-trigger`);
const menu = (page: Page, id = 'user-menu') => page.locator(`#${id}`);
const focusedText = (page: Page) => page.evaluate(() => document.activeElement?.textContent?.replace(/\s+/g, ' ').trim());
const rectOf = (locator: Locator) => locator.evaluate(element => {
  const rect = element.getBoundingClientRect();
  return {left: Math.round(rect.left), right: Math.round(rect.right), top: Math.round(rect.top),
    bottom: Math.round(rect.bottom), width: Math.round(rect.width)};
});
/** The element painted at the middle of `locator`: what a click there reaches. */
const hit = (locator: Locator) => locator.evaluate(element => {
  const rect = element.getBoundingClientRect();
  const top = document.elementFromPoint(rect.left + rect.width / 2, rect.top + rect.height / 2);
  return !!top && (top === element || element.contains(top));
});

test.describe('sidebar, desktop', () => {
  test.beforeEach(async ({page}) => {
    await page.setViewportSize(DESKTOP);
  });

  test('the keyboard opens the menu beside the sidebar, the user above the items and Sign out', async ({page}) => {
    const {messages} = await open(page, SIDEBAR);
    await expect(trigger(page)).toHaveRole('button');
    await expect(trigger(page)).toHaveAccessibleName('shadcn m@example.com');
    await expect(trigger(page)).toHaveAttribute('aria-expanded', 'false');
    // The picture loaded: the image shows, the initials do not.
    await expect(trigger(page).locator('.avatar-image')).toHaveAttribute('data-status', 'loaded');
    await expect(trigger(page).locator('.avatar-fallback')).toBeHidden();

    await trigger(page).focus();
    await page.keyboard.press('Enter');
    await expect(menu(page)).toBeVisible();
    await expect(trigger(page)).toHaveAttribute('aria-expanded', 'true');
    await expect.poll(() => focusedText(page)).toBe('Account');
    await expect(menu(page)).toHaveAccessibleName('shadcn m@example.com');
    await expect(menu(page).locator('.user-menu-label')).toHaveText(/shadcn\s*m@example\.com/);
    await expect(menu(page).getByRole('menuitem')).toHaveText(['Account', 'Notifications', 'Sign out']);
    await page.keyboard.press('ArrowDown');
    await page.keyboard.press('ArrowDown');
    await expect.poll(() => focusedText(page)).toBe('Sign out');

    // Beside the sidebar, its end (bottom) lined up with the button's: from the footer it grows upwards.
    const button = await rectOf(trigger(page));
    const panel = await rectOf(menu(page));
    expect(panel.left).toBeGreaterThanOrEqual(button.right);
    expect(Math.abs(panel.bottom - button.bottom)).toBeLessThanOrEqual(1);
    expect(panel.width).toBeGreaterThanOrEqual(button.width);

    const results = await new AxeBuilder({page}).withTags(TAGS).analyze();
    expect(results.violations.map(violation => violation.id)).toEqual([]);

    await page.keyboard.press('Escape');
    await expect(menu(page)).toBeHidden();
    await expect(trigger(page)).toBeFocused();
    await expect(trigger(page)).toHaveAttribute('aria-expanded', 'false');
    expect(messages).toEqual([]);
  });

  test('Sign out posts its form with the CSRF token as a whole page, though the body is boosted', async ({page}) => {
    const {messages, requests} = await open(page, SIDEBAR, {htmx: true, bodyAttributes: 'hx-boost="true"'});
    await trigger(page).focus();
    // ArrowUp opens the menu on its last item.
    await page.keyboard.press('ArrowUp');
    await expect.poll(() => focusedText(page)).toBe('Sign out');
    await page.keyboard.press('Enter');

    await expect(page.getByRole('heading', {name: 'Signed out'})).toBeVisible();
    expect(new URL(page.url()).pathname).toBe('/logout');
    expect(requests['/logout']).toHaveLength(1);
    const [request] = requests['/logout'];
    expect(request.body).toBe(`_csrf=${TOKEN}`);
    expect(request.headers['content-type']).toBe('application/x-www-form-urlencoded');
    expect(request.headers['hx-request']).toBeUndefined();
    // A whole page: the shell is gone, not swapped around the response.
    await expect(page.locator('.sidebar-provider')).toHaveCount(0);
    expect(messages).toEqual([]);
  });

  test('a pointer signs out too', async ({page}) => {
    const {requests} = await open(page, SIDEBAR);
    await trigger(page).click();
    await expect(menu(page)).toBeVisible();
    await menu(page).getByRole('menuitem', {name: 'Sign out'}).click();
    await expect(page.getByRole('heading', {name: 'Signed out'})).toBeVisible();
    expect(requests['/logout'].map(request => request.body)).toEqual([`_csrf=${TOKEN}`]);
  });

  test('collapsed to icons: the picture, the name as a tooltip, never with the menu', async ({page}) => {
    const {messages} = await open(page, SIDEBAR.replace('data-state="expanded"', 'data-state="collapsed"'));
    const tooltip = trigger(page).locator('> .sidebar-menu-tooltip');
    const button = await rectOf(trigger(page));
    expect(button.width).toBe(32);
    // The picture fills the square; the name and email are visually hidden, and still name the button.
    expect(await rectOf(trigger(page).locator('.avatar'))).toMatchObject({width: 32});
    await expect(trigger(page)).toHaveAccessibleName('shadcn m@example.com');

    await trigger(page).hover();
    await expect(tooltip).toBeVisible();
    await expect(tooltip).toHaveText('shadcn');
    await trigger(page).click();
    await expect(menu(page)).toBeVisible();
    await expect(tooltip).toBeHidden();
    const panel = await rectOf(menu(page));
    expect(panel.left).toBeGreaterThanOrEqual(button.right);
    expect(panel.width).toBeGreaterThanOrEqual(224);
    await page.mouse.move(panel.left + 20, panel.top + 20);
    await trigger(page).hover();
    await page.waitForTimeout(600);
    await expect(tooltip).toBeHidden();

    await page.keyboard.press('Escape');
    await expect(menu(page)).toBeHidden();
    await expect(trigger(page)).toBeFocused();
    await expect(page.locator('.sidebar-provider')).toHaveAttribute('data-state', 'collapsed');
    expect(messages).toEqual([]);
  });

  test('without JavaScript the menu opens and Sign out signs out', async ({browser}) => {
    const context = await browser.newContext({javaScriptEnabled: false, viewport: DESKTOP});
    const page = await context.newPage();
    const {requests} = await open(page, SIDEBAR, {alpine: false});
    await trigger(page).click();
    await expect(menu(page)).toBeVisible();
    await menu(page).getByRole('menuitem', {name: 'Sign out'}).click();
    await expect(page.getByRole('heading', {name: 'Signed out'})).toBeVisible();
    expect(requests['/logout'].map(request => request.body)).toEqual([`_csrf=${TOKEN}`]);
    await context.close();
  });
});

test.describe('sidebar, phone', () => {
  test.beforeEach(async ({page}) => {
    await page.setViewportSize(PHONE);
  });

  test('in the panel the menu opens above the button, above the panel and outside its inert', async ({page}) => {
    const {messages} = await open(page, SIDEBAR);
    await page.locator('.sidebar-trigger').click();
    await expect(page.locator('#sidebar')).toBeVisible();
    await trigger(page).click();
    await expect(menu(page)).toBeVisible();

    // The footer is at the bottom of the screen: no room below, so it flips above, lined up with the button's end.
    const button = await rectOf(trigger(page));
    const panel = await rectOf(menu(page));
    expect(panel.bottom).toBeLessThanOrEqual(button.top);
    expect(Math.abs(panel.right - button.right)).toBeLessThanOrEqual(1);
    expect(panel.left).toBeGreaterThanOrEqual(0);
    expect(await menu(page).evaluate(element => !!element.closest('[inert]'))).toBe(false);
    for (const name of ['Account', 'Notifications', 'Sign out']) {
      expect(await hit(menu(page).getByRole('menuitem', {name})), name).toBe(true);
    }
    const results = await new AxeBuilder({page}).withTags(TAGS)
        .disableRules(['landmark-one-main', 'page-has-heading-one']).analyze();
    expect(results.violations.map(violation => violation.id)).toEqual([]);

    // Escape closes the menu first, the focus back on its button, then the panel.
    await page.keyboard.press('Escape');
    await expect(menu(page)).toBeHidden();
    await expect(trigger(page)).toBeFocused();
    await expect(page.locator('#sidebar')).toBeVisible();
    await page.keyboard.press('Escape');
    await expect(page.locator('#sidebar')).toBeHidden();
    expect(messages).toEqual([]);
  });

  test('Sign out from the panel signs out', async ({page}) => {
    const {requests} = await open(page, SIDEBAR);
    await page.locator('.sidebar-trigger').click();
    await trigger(page).click();
    await menu(page).getByRole('menuitem', {name: 'Sign out'}).click();
    await expect(page.getByRole('heading', {name: 'Signed out'})).toBeVisible();
    expect(requests['/logout'].map(request => request.body)).toEqual([`_csrf=${TOKEN}`]);
  });
});

test.describe('header', () => {
  const id = 'header-user-menu';

  for (const viewport of [DESKTOP, PHONE]) {
    test(`${viewport.width} px: the picture button opens the menu below it, lined up with its end`, async ({page}) => {
      await page.setViewportSize(viewport);
      const {messages} = await open(page, HEADER);
      await expect(trigger(page, id)).toHaveAccessibleName('Account: shadcn');
      await expect(trigger(page, id).locator('.avatar-image')).toHaveAttribute('data-status', 'loaded');
      const button = await rectOf(trigger(page, id));
      // Round, the picture filling it.
      expect(await trigger(page, id).evaluate(element => getComputedStyle(element).borderRadius)).not.toBe('0px');

      await trigger(page, id).focus();
      await page.keyboard.press('Enter');
      await expect(menu(page, id)).toBeVisible();
      await expect.poll(() => focusedText(page)).toBe('Account');
      const panel = await rectOf(menu(page, id));
      expect(panel.top).toBeGreaterThanOrEqual(button.bottom);
      expect(Math.abs(panel.right - button.right)).toBeLessThanOrEqual(1);
      expect(panel.left).toBeGreaterThanOrEqual(0);
      const results = await new AxeBuilder({page}).withTags(TAGS).analyze();
      expect(results.violations.map(violation => violation.id)).toEqual([]);

      await page.keyboard.press('Escape');
      await expect(menu(page, id)).toBeHidden();
      await expect(trigger(page, id)).toBeFocused();
      expect(messages).toEqual([]);
    });
  }

  test('Sign out posts with the CSRF token', async ({page}) => {
    await page.setViewportSize(DESKTOP);
    const {requests} = await open(page, HEADER, {htmx: true, bodyAttributes: 'hx-boost="true"'});
    await trigger(page, id).click();
    await menu(page, id).getByRole('menuitem', {name: 'Sign out'}).click();
    await expect(page.getByRole('heading', {name: 'Signed out'})).toBeVisible();
    expect(requests['/logout'].map(request => request.body)).toEqual([`_csrf=${TOKEN}`]);
    expect(requests['/logout'][0].headers['hx-request']).toBeUndefined();
  });
});

test.describe('nobody signed in', () => {
  for (const [name, html] of [['header', ANONYMOUS_HEADER], ['sidebar', ANONYMOUS_SIDEBAR]] as const) {
    test(`${name}: Sign in is a link that loads the sign-in page whole, though the body is boosted`, async ({page}) => {
      await page.setViewportSize(DESKTOP);
      const {messages, requests} = await open(page, html, {htmx: true, bodyAttributes: 'hx-boost="true"'});
      const link = page.getByRole('link', {name: 'Sign in'});
      await expect(link).toHaveAttribute('href', '/login');
      await expect(page.getByRole('menu', {includeHidden: true})).toHaveCount(0);
      await link.click();
      await expect(page.getByRole('heading', {name: 'Sign in'})).toBeVisible();
      expect(requests['/login']).toHaveLength(1);
      expect(requests['/login'][0].headers['hx-request']).toBeUndefined();
      expect(new URL(page.url()).pathname).toBe('/login');
      expect(messages).toEqual([]);
    });
  }
});

// The showcase in every skin and theme: the sidebar's user menu and the header layout's, opened, with no axe violation.
for (const skin of skins) {
  for (const theme of themes) {
    test(`showcase, ${skin}, ${theme}: the user menus`, async ({page}) => {
      await page.setViewportSize({width: 1280, height: 720});
      for (const [path, id] of [['', 'user-menu'], ['header/', 'header-user-menu']] as const) {
        await openShowcase(page, skin, theme, path);
        await trigger(page, id).focus();
        await page.keyboard.press('Enter');
        await expect(menu(page, id)).toBeVisible();
        await expect.poll(() => focusedText(page)).toBe('Account');
        const results = await new AxeBuilder({page}).withTags(TAGS).include(`#${id}`).include(`#${id}-trigger`)
            .analyze();
        expect(results.violations.map(violation => violation.id), path || 'sidebar').toEqual([]);
        await page.keyboard.press('Escape');
        await expect(trigger(page, id)).toBeFocused();
      }
    });
  }
}
