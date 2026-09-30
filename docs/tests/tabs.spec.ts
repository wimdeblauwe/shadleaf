import {expect, test, type Locator, type Page} from '@playwright/test';
import AxeBuilder from '@axe-core/playwright';
import {combinations, openShowcase} from './showcase';
import {openFixture} from './fixture';

// sl:tabs, with slTabs adding the ARIA tabs pattern to markup the server renders with the active panel showing. On the
// showcase, in every skin and theme: every tabs is one tab stop, the arrow keys go through every enabled tab and back
// to the first (showing its panel, at once or with Enter for manual activation), each tab and panel shows focus, and
// axe passes with each panel showing. In fixture pages under a strict Content-Security-Policy with the csp Alpine
// build: the keyboard in detail (vertical, right-to-left, manual), links (the address, modifier clicks, no Alpine),
// find-in-page through a link to an element in an inactive panel, and htmx (a panel loaded on first view, a swap
// replacing the tabs, history). Runs in Chromium and Firefox.

const TAGS = ['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa', 'wcag22aa', 'best-practice'];

/** Whether the focused element shows its focus (an outline or a box-shadow ring). */
function focusState(page: Page) {
  return page.evaluate(() => {
    const element = document.activeElement as HTMLElement;
    const style = getComputedStyle(element);
    return {
      label: element.outerHTML.slice(0, 100),
      visible: element.matches(':focus-visible')
          && ((style.outlineStyle !== 'none' && parseFloat(style.outlineWidth) > 0) || style.boxShadow !== 'none'),
    };
  });
}

/** The tab stops, the active tab and the panels that show, of the tabs with this id. */
function state(page: Page, id: string) {
  return page.locator(`#${id}`).evaluate(root => {
    const own = (selector: string) => [...root.querySelectorAll(selector)].filter(element => element.closest('.tabs') === root);
    return {
      stops: own('[role=tab]').filter(tab => tab.getAttribute('tabindex') !== '-1').map(tab => tab.id),
      active: own('[role=tab][aria-selected=true]').map(tab => tab.id),
      shown: own('[role=tabpanel]').filter(panel => !panel.hasAttribute('hidden')).map(panel => panel.id),
      focused: document.activeElement?.id,
    };
  });
}

for (const {skin, theme} of combinations) {
  test(`every tabs works with the keyboard and passes axe: ${skin}, ${theme}`, async ({page}) => {
    await openShowcase(page, skin, theme);
    await page.waitForFunction(() => 'Alpine' in window);
    const ids = await page.locator('main .tabs').evaluateAll(all => all.map(tabs => tabs.id));
    expect(ids.length, 'the showcase has tabs').toBeGreaterThan(0);

    const failures: string[] = [];
    for (const id of ids) {
      const root = page.locator(`#${id}`);
      const manual = await root.getAttribute('data-activation') === 'manual';
      const next = await root.getAttribute('data-orientation') === 'vertical' ? 'ArrowDown' : 'ArrowRight';
      const enabled = await root.locator('[role=tab]:not(:disabled, [aria-disabled=true])').count();
      const first = (await state(page, id)).active[0];

      // Reach the list with the keyboard: back from the panel to the one tab stop.
      await page.locator(`#${first}`).focus();
      await page.keyboard.press('Tab');
      await page.keyboard.press('Shift+Tab');
      await expect(page.locator(`#${first}`)).toBeFocused();

      for (let i = 0; i < enabled; i++) {
        if (manual) {
          await page.keyboard.press('Enter');
        }
        const now = await state(page, id);
        expect(now.stops, `${id}: one tab stop`).toEqual([now.focused]);
        expect(now.active, `${id}: the focused tab is active`).toEqual([now.focused]);
        const panel = await page.locator(`#${now.focused}`).getAttribute('aria-controls');
        expect(now.shown, `${id}: only its panel shows`).toEqual([panel]);
        const tab = await focusState(page);
        if (!tab.visible) {
          failures.push(`${id}: no visible focus on ${tab.label}`);
        }

        const results = await new AxeBuilder({page}).include(`#${id}`).withTags(TAGS).analyze();
        failures.push(...results.violations.map(violation => `${id} (${now.focused}): ${violation.id}: ${
            violation.help}\n    ${violation.nodes.map(node => node.target.join(' ')).join('\n    ')}`));

        // Tab goes to the panel, which shows its focus; Shift+Tab comes back.
        await page.keyboard.press('Tab');
        await expect(page.locator(`#${panel}`)).toBeFocused();
        const panelFocus = await focusState(page);
        if (!panelFocus.visible) {
          failures.push(`${id}: no visible focus on the panel ${panel}`);
        }
        await page.keyboard.press('Shift+Tab');
        await expect(page.locator(`#${now.focused}`)).toBeFocused();
        await page.keyboard.press(next);
      }
      if (manual) {
        await page.keyboard.press('Enter');
      }
      expect((await state(page, id)).active, `${id}: the arrow keys wrap around`).toEqual([first]);
    }
    expect(failures, failures.join('\n')).toEqual([]);
  });
}

test('a click on a tab shows its panel; a disabled tab does nothing', async ({page}) => {
  await openShowcase(page, 'vega', 'light');
  await page.waitForFunction(() => 'Alpine' in window);
  await page.locator('#tabs-icons-code-trigger').click();
  expect(await state(page, 'tabs-icons')).toMatchObject({
    active: ['tabs-icons-code-trigger'], shown: ['tabs-icons-code-content'], stops: ['tabs-icons-code-trigger'],
  });
  await page.locator('#tabs-icons-history-trigger').click({force: true});
  expect((await state(page, 'tabs-icons')).active).toEqual(['tabs-icons-code-trigger']);
});

// --- fixture pages ---------------------------------------------------------------------------------------------------

type Tab = { tab: string, disabled?: boolean, href?: string, panel?: string, panelAttributes?: string };

/** Tabs marked up as <sl:tabs> renders them (see tabs.approved.html). */
function tabs(id: string, active: string, list: Tab[], {orientation = '', manual = false, attributes = ''} = {}): string {
  const triggers = list.map(({tab, disabled, href}) => {
    const common = `class="tabs-trigger" role="tab" id="${id}-${tab}-trigger" aria-controls="${id}-${tab}-content" `
        + `aria-selected="${tab === active}"`;
    return href !== undefined
        ? `<a ${common}${disabled ? ' aria-disabled="true"' : ` href="${href}"`}>${tab}</a>`
        : `<button type="button" ${common}${disabled ? ' disabled' : ''}>${tab}</button>`;
  }).join('');
  const panels = list.map(({tab, panel, panelAttributes}) => `<div class="tabs-content" role="tabpanel" tabindex="0" `
      + `id="${id}-${tab}-content" aria-labelledby="${id}-${tab}-trigger"${tab === active ? '' : ' hidden'} `
      + `${panelAttributes ?? ''}>${panel ?? `Panel ${tab}`}</div>`).join('');
  return `<div class="tabs" x-data="slTabs" id="${id}"${orientation ? ` data-orientation="${orientation}"` : ''}`
      + `${manual ? ' data-activation="manual"' : ''} ${attributes}><div class="tabs-list" role="tablist"`
      + `${orientation ? ` aria-orientation="${orientation}"` : ''}>${triggers}</div>${panels}</div>`;
}

const ABCD: Tab[] = [{tab: 'a'}, {tab: 'b'}, {tab: 'c', disabled: true}, {tab: 'd'}];

async function focusTab(page: Page, id: string) {
  await page.locator(`#${id}`).focus();
  await expect(page.locator(`#${id}`)).toBeFocused();
}

function activeTab(page: Page, id: string): Promise<string | undefined> {
  return state(page, id).then(now => now.active[0]);
}

test('the csp build runs tabs under a strict policy: arrows wrap and skip disabled tabs, Home, End, Tab', async ({page}) => {
  const messages = await openFixture(page, `<button type="button" id="before">Before</button>${tabs('t', 'b', ABCD)}`);

  // The server's active tab is the one tab stop; the inactive panels can be found by find-in-page.
  expect(await state(page, 't')).toMatchObject({stops: ['t-b-trigger'], active: ['t-b-trigger'], shown: ['t-b-content']});
  await expect(page.locator('#t-a-content')).toHaveAttribute('hidden', 'until-found');
  await page.locator('#before').focus();
  await page.keyboard.press('Tab');
  await expect(page.locator('#t-b-trigger')).toBeFocused();

  await page.keyboard.press('ArrowRight');
  await expect(page.locator('#t-d-trigger'), 'c is disabled').toBeFocused();
  expect(await activeTab(page, 't'), 'automatic activation').toBe('t-d-trigger');
  await page.keyboard.press('ArrowRight');
  await expect(page.locator('#t-a-trigger'), 'wraps around').toBeFocused();
  await page.keyboard.press('ArrowLeft');
  await expect(page.locator('#t-d-trigger')).toBeFocused();
  await page.keyboard.press('Home');
  await expect(page.locator('#t-a-trigger')).toBeFocused();
  await page.keyboard.press('End');
  await expect(page.locator('#t-d-trigger')).toBeFocused();
  await page.keyboard.press('ArrowDown');
  await expect(page.locator('#t-d-trigger'), 'up and down do nothing in a horizontal list').toBeFocused();
  await page.keyboard.press('Tab');
  await expect(page.locator('#t-d-content')).toBeFocused();
  await page.keyboard.press('Shift+Tab');
  await expect(page.locator('#t-d-trigger'), 'back to the active tab').toBeFocused();
  await page.keyboard.press('Shift+Tab');
  await expect(page.locator('#before'), 'one tab stop in the list').toBeFocused();
  expect(messages).toEqual([]);
});

test('vertical tabs use up and down; right-to-left swaps left and right', async ({page}) => {
  await openFixture(page, tabs('v', 'a', ABCD, {orientation: 'vertical'})
      + `<div dir="rtl">${tabs('r', 'a', ABCD)}</div>`);

  await focusTab(page, 'v-a-trigger');
  await page.keyboard.press('ArrowRight');
  await expect(page.locator('#v-a-trigger')).toBeFocused();
  await page.keyboard.press('ArrowDown');
  await expect(page.locator('#v-b-trigger')).toBeFocused();
  await page.keyboard.press('ArrowUp');
  await expect(page.locator('#v-a-trigger')).toBeFocused();
  expect(await activeTab(page, 'v')).toBe('v-a-trigger');

  await focusTab(page, 'r-a-trigger');
  await page.keyboard.press('ArrowLeft');
  await expect(page.locator('#r-b-trigger'), 'left is forward on a right-to-left page').toBeFocused();
  await page.keyboard.press('ArrowRight');
  await expect(page.locator('#r-a-trigger')).toBeFocused();
});

test('manual activation: the arrows move the focus, Enter or Space shows the panel', async ({page}) => {
  await openFixture(page, `${tabs('m', 'a', ABCD, {manual: true})}<button type="button" id="after">After</button>`);

  await focusTab(page, 'm-a-trigger');
  await page.keyboard.press('ArrowRight');
  await expect(page.locator('#m-b-trigger')).toBeFocused();
  expect(await state(page, 'm')).toMatchObject({active: ['m-a-trigger'], shown: ['m-a-content'], stops: ['m-b-trigger']});
  await page.keyboard.press('Enter');
  expect(await state(page, 'm')).toMatchObject({active: ['m-b-trigger'], shown: ['m-b-content']});
  await page.keyboard.press('ArrowRight');
  await page.keyboard.press(' ');
  expect(await activeTab(page, 'm')).toBe('m-d-trigger');

  // Moving on without choosing hands the tab stop back to the active tab.
  await page.keyboard.press('ArrowRight');
  await expect(page.locator('#m-a-trigger')).toBeFocused();
  await page.keyboard.press('Tab');
  await expect(page.locator('#m-d-content')).toBeFocused();
  expect((await state(page, 'm')).stops).toEqual(['m-d-trigger']);
});

const LINKS: Tab[] = [{tab: 'a', href: '/?tab=a'}, {tab: 'b', href: '/?tab=b'}, {tab: 'c', href: '/?tab=c', disabled: true}];

test('link tabs switch in place and replace the address; a modifier click is left to the browser', async ({page}) => {
  const messages = await openFixture(page, url => tabs('l', url.searchParams.get('tab') ?? 'a', LINKS));
  const entries = await page.evaluate(() => history.length);

  await page.locator('#l-b-trigger').click();
  expect(await state(page, 'l')).toMatchObject({active: ['l-b-trigger'], shown: ['l-b-content']});
  expect(new URL(page.url()).search).toBe('?tab=b');
  expect(await page.evaluate(() => history.length), 'no history entry').toBe(entries);

  await page.keyboard.press('ArrowLeft');
  expect(new URL(page.url()).search, 'the keyboard too').toBe('?tab=a');
  await page.keyboard.press('ArrowRight');
  await page.keyboard.press(' ');
  expect(await activeTab(page, 'l'), 'Space on a link').toBe('l-b-trigger');

  // slTabs listens on the tabs; a listener on the document sees whether it prevented the click (and prevents the
  // navigation itself).
  const left = await page.locator('#l-a-trigger').evaluate(tab => {
    const event = new MouseEvent('click', {bubbles: true, cancelable: true, ctrlKey: true});
    let prevented = true;
    document.addEventListener('click', clicked => {
      prevented = clicked.defaultPrevented;
      clicked.preventDefault();
    }, {once: true});
    tab.dispatchEvent(event);
    return !prevented;
  });
  expect(left, 'a Ctrl-click stays a link click').toBe(true);
  expect(await activeTab(page, 'l')).toBe('l-b-trigger');

  // After a reload the server renders the tab from the address.
  await page.reload();
  await page.waitForFunction(() => 'Alpine' in window);
  expect(await activeTab(page, 'l')).toBe('l-b-trigger');
  expect(messages).toEqual([]);
});

test('without Alpine the server renders the active panel, and link tabs load the page with theirs', async ({page}) => {
  await openFixture(page, url => tabs('l', url.searchParams.get('tab') ?? 'a', LINKS), {alpine: false});

  expect(await state(page, 'l')).toMatchObject({shown: ['l-a-content']});
  await expect(page.locator('#l-b-content')).toHaveAttribute('hidden', '');
  await page.locator('#l-b-trigger').click();
  await page.waitForURL('**/?tab=b');
  expect(await state(page, 'l')).toMatchObject({active: ['l-b-trigger'], shown: ['l-b-content']});
  await expect(page.locator('#l-c-trigger')).not.toHaveAttribute('href');
});

test('a link to an element in an inactive panel activates its tab, through nested tabs too', async ({page}) => {
  const inner = tabs('inner', 'x', [{tab: 'x'}, {tab: 'y', panel: '<p id="deep">Deep</p>'}]);
  await openFixture(page, tabs('outer', 'a', [{tab: 'a'}, {tab: 'b', panel: '<p id="billing">Billing</p>'},
    {tab: 'c', panel: inner}]) + '<p><a href="#billing" id="to-billing">Billing</a> <a href="#deep" id="to-deep">Deep</a></p>');
  const shown: string[] = [];
  await page.exposeFunction('reportShown', (id: string) => shown.push(id));
  await page.evaluate(() => document.querySelectorAll('[role=tabpanel]').forEach(panel => panel.addEventListener(
      'sl-tabs-show', () => (window as unknown as {reportShown: (id: string) => void}).reportShown(panel.id))));

  await page.locator('#to-billing').click();
  expect(await activeTab(page, 'outer')).toBe('outer-b-trigger');
  await expect(page.locator('#billing')).toBeVisible();
  await page.locator('#to-deep').click();
  expect(await activeTab(page, 'outer')).toBe('outer-c-trigger');
  expect(await activeTab(page, 'inner')).toBe('inner-y-trigger');
  await expect(page.locator('#deep')).toBeVisible();
  expect((await state(page, 'outer')).shown).toEqual(['outer-c-content']);
  expect(shown, 'sl-tabs-show on each panel shown').toEqual(['outer-b-content', 'outer-c-content', 'inner-y-content']);
});

test('htmx: a panel loads on first view (sl-tabs-show once); a swap and history keep working tabs', async ({page}) => {
  let loads = 0;
  const settings = (active: string) => tabs('s', active, [
    {tab: 'account', panel: 'Account'},
    {tab: 'sessions', panel: 'Loading…', panelAttributes: 'hx-get="/sessions" hx-trigger="sl-tabs-show once"'},
    {tab: 'danger', panel: 'Danger zone'},
  ], {manual: true});
  const messages = await openFixture(page, `<div id="area">${settings('account')}</div>
      <a href="/next" id="next" hx-get="/next" hx-target="main" hx-push-url="true">Next</a>`, {
    htmx: true,
    routes: {
      '/sessions': () => ({body: `<p id="sessions">${++loads} session</p>`}),
      '/area': () => ({body: settings('danger')}),
      '/next': () => ({body: '<p id="next-page">Next page</p>'}),
    },
  });

  // Arrowing past the lazy tab (manual activation) loads nothing; choosing it loads it once.
  await focusTab(page, 's-account-trigger');
  await page.keyboard.press('ArrowRight');
  await page.keyboard.press('ArrowRight');
  expect(loads).toBe(0);
  await page.keyboard.press('ArrowLeft');
  await page.keyboard.press('Enter');
  await expect(page.locator('#sessions')).toHaveText('1 session');
  await page.locator('#s-account-trigger').click();
  await page.locator('#s-sessions-trigger').click();
  await expect(page.locator('#sessions')).toHaveText('1 session');
  expect(loads, 'once').toBe(1);

  // History: back shows the tab the user chose, with the loaded content, and the tabs still work.
  await page.locator('#next').click();
  await expect(page.locator('#next-page')).toBeVisible();
  await page.goBack();
  await expect(page.locator('#sessions')).toHaveText('1 session');
  expect(await state(page, 's')).toMatchObject({active: ['s-sessions-trigger'], shown: ['s-sessions-content'],
    stops: ['s-sessions-trigger']});
  await focusTab(page, 's-sessions-trigger');
  await page.keyboard.press('ArrowRight');
  await page.keyboard.press('Enter');
  expect(await activeTab(page, 's')).toBe('s-danger-trigger');
  expect(loads).toBe(1);

  // A swap that replaces the tabs shows the tab its markup has active, and they work.
  await page.evaluate(() => (window as unknown as {htmx: {ajax: Function}}).htmx.ajax('GET', '/area',
      {target: '#area', swap: 'innerHTML'}));
  await expect(page.locator('#s-danger-content')).toBeVisible();
  expect(await state(page, 's')).toMatchObject({active: ['s-danger-trigger'], stops: ['s-danger-trigger']});
  await expect(page.locator('#s-account-content')).toHaveAttribute('hidden', 'until-found');
  await page.locator('#s-account-trigger').click();
  expect(await activeTab(page, 's')).toBe('s-account-trigger');
  expect(messages).toEqual([]);
});

test.describe(() => {
  test.use({contextOptions: {reducedMotion: 'no-preference'}});

  test('the tabs transition only where motion is fine', async ({page}) => {
    await openShowcase(page, 'vega', 'light');
    const duration = (element: Locator) => element.evaluate(tab => getComputedStyle(tab).transitionDuration);
    expect(await duration(page.locator('#tabs-line-overview-trigger'))).not.toBe('0s');
    await page.emulateMedia({reducedMotion: 'reduce'});
    expect(await duration(page.locator('#tabs-line-overview-trigger'))).toBe('0s');
  });
});
