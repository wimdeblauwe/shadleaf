import {expect, test, type Locator, type Page} from '@playwright/test';
import AxeBuilder from '@axe-core/playwright';
import previews from '../src/generated/previews.json' with {type: 'json'};
import {combinations, openShowcase} from './showcase';
import {openFixture, type Routes} from './fixture';

// sl:toaster and sl:toast. On the showcase, in every skin and theme: toasts from every variant pass axe, show focus,
// and work with the keyboard (Alt+T, Tab, Escape). In fixture pages under a strict Content-Security-Policy with the csp
// Alpine build: the sl-toast event (text only, never HTML), the announcers, HX-Trigger (also together with
// sl-dialog-close, in either order), an out-of-band swap, a toast over an open modal dialog, pausing on hover and
// focus, visible-toasts, motion, forced colours, htmx history, and a toast rendered without Alpine.

const TAGS = ['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa', 'wcag22aa', 'best-practice'];

type Scenario = { id: string, html: string };
const scenarios = previews.scenarios as Scenario[];
/** The markup <sl:toaster/> renders, from the default scenario (a button, then the toaster). */
const TOASTER = (() => {
  const html = scenarios.find(scenario => scenario.id === 'toaster--default')!.html;
  return html.slice(html.indexOf('<div class="toaster"')).replace('id="toaster-default"', 'id="toaster"');
})();
/** A toast <sl:toast> renders, from the server scenario. */
const SERVER_TOAST = (() => {
  const html = scenarios.find(scenario => scenario.id === 'toaster--server')!.html;
  return html.slice(html.indexOf('<li class="toast"'), html.indexOf('</li>') + '</li>'.length);
})();

function toaster(attributes = '', toasts = ''): string {
  return TOASTER
      .replace('<div class="toaster"', `<div class="toaster"${attributes ? ' ' + attributes : ''}`)
      .replace('<ol class="toaster-list">', `<ol class="toaster-list">${toasts}`);
}

/** A dialog as <sl:dialog> renders it (see dialog.approved.html). */
function dialog(id: string, content: string): string {
  return `<dialog class="dialog" id="${id}" closedby="any" x-data="slDialog" aria-labelledby="${id}-title">
    <div class="dialog-header"><div class="dialog-title" role="heading" aria-level="2" id="${id}-title">Edit</div></div>
    ${content}
  </dialog>`;
}

async function toast(page: Page, detail: unknown) {
  await page.evaluate(detail => document.dispatchEvent(new CustomEvent('sl-toast', {detail})), detail);
}

const toasts = (page: Page) => page.locator('.toaster-list > .toast:not([data-removed])');
const announced = (page: Page, role: 'status' | 'alert') =>
    page.locator(`.toaster-announcer[role="${role}"]`).evaluate(region => region.textContent);

/** Waits for the entry transition: axe reads colours half-way through a fade. */
async function settled(locator: Locator) {
  await expect.poll(() => locator.evaluate(element => getComputedStyle(element).opacity)).toBe('1');
}

// --- the showcase ------------------------------------------------------------------------------------------------------

for (const {skin, theme} of combinations) {
  test(`toasts of every variant pass axe and work with the keyboard: ${skin}, ${theme}`, async ({page}) => {
    await openShowcase(page, skin, theme);
    await page.waitForFunction(() => 'Alpine' in window);
    const stage = page.locator('[data-scenario="toaster--variants"]');
    const buttons = stage.getByRole('button');
    for (let i = 0; i < await buttons.count(); i++) {
      await buttons.nth(i).focus();
      await page.keyboard.press('Enter');
    }
    const trigger = buttons.last();
    await expect(toasts(page)).toHaveCount(5);
    const front = toasts(page).last();
    await expect(front).toContainText('Event has not been created');
    // Spread out, so axe sees every visible toast whole.
    await page.locator('.toaster-list').first().hover();
    await settled(front);

    const results = await new AxeBuilder({page}).include('.toaster-viewport').withTags(TAGS).analyze();
    expect(results.violations.map(violation => `${violation.id}: ${violation.help}\n    ${
        violation.nodes.map(node => node.target.join(' ')).join('\n    ')}`)).toEqual([]);

    // Alt+T: the focus goes to the newest toast, and back to the button once the last one is closed.
    await page.mouse.move(0, 0);
    await trigger.focus();
    await page.keyboard.press('Alt+KeyT');
    await expect(front).toBeFocused();
    const failures: string[] = [];
    for (let i = 0; i < 6; i++) {
      const focused = await page.evaluate(() => {
        const element = document.activeElement as HTMLElement;
        const style = getComputedStyle(element);
        return {
          label: element.outerHTML.slice(0, 80),
          visible: element.matches(':focus-visible')
              && ((style.outlineStyle !== 'none' && parseFloat(style.outlineWidth) > 0) || style.boxShadow !== 'none'),
        };
      });
      if (!focused.visible) {
        failures.push(`no visible focus on ${focused.label}`);
      }
      await page.keyboard.press('Tab');
    }
    expect(failures, failures.join('\n')).toEqual([]);

    // Escape closes the focused toast; the focus moves to the next one, and back to the button after the last.
    await page.keyboard.press('Alt+KeyT');
    for (let left = 4; left >= 0; left--) {
      await page.keyboard.press('Escape');
      await expect(toasts(page)).toHaveCount(left);
    }
    await expect(trigger).toBeFocused();
  });
}

test('the close button closes a toast; toasts stack and spread out on hover', async ({page}) => {
  await openShowcase(page, 'vega', 'light');
  await page.waitForFunction(() => 'Alpine' in window);
  await page.locator('[data-scenario="toaster--stack"]').getByRole('button').click();
  await expect(toasts(page)).toHaveCount(3);
  const list = page.locator('.toaster-list').first();
  const tops = () => toasts(page).evaluateAll(items => items.map(item => Math.round(item.getBoundingClientRect().top)));

  const collapsed = await tops();
  expect(collapsed[2] - collapsed[1], 'collapsed: 14px apart').toBeLessThan(20);
  await list.hover();
  await expect(list).toHaveAttribute('data-expanded', '');
  await expect.poll(async () => {
    const spread = await tops();
    return spread[2] - spread[1];
  }, {message: 'spread out: a toast\'s height apart'}).toBeGreaterThan(40);

  await toasts(page).last().getByRole('button', {name: 'Close'}).click();
  await expect(toasts(page)).toHaveCount(2);
  await expect(toasts(page).last()).toContainText('Invitation sent to Grace');
});

// --- fixture pages -----------------------------------------------------------------------------------------------------

test('the sl-toast event shows a toast, as text, and announces it', async ({page}) => {
  const messages = await openFixture(page, toaster());

  await toast(page, {title: 'Saved <img src="/x" id="injected">', description: 'Monday <b>6pm</b>', variant: 'success'});
  const shown = toasts(page);
  await expect(shown).toHaveCount(1);
  await expect(shown.locator('.toast-title')).toHaveText('Saved <img src="/x" id="injected">');
  await expect(shown.locator('.toast-description')).toHaveText('Monday <b>6pm</b>');
  await expect(page.locator('#injected, .toast-description b')).toHaveCount(0);
  await expect(shown).toHaveAttribute('data-variant', 'success');
  await expect(shown.locator('> svg')).toHaveCount(1);
  await expect(shown.getByRole('button', {name: 'Close'})).toBeVisible();
  await expect.poll(() => announced(page, 'status')).toBe('Saved <img src="/x" id="injected">. Monday <b>6pm</b>');
  expect(await announced(page, 'alert')).toBe('');

  // An error is announced at once (assertive); a string is a title; an unknown variant is the default.
  await toast(page, {title: 'Could not save', variant: 'error'});
  await expect.poll(() => announced(page, 'alert')).toBe('Could not save');
  await toast(page, {value: 'Copied'});
  await toast(page, {title: 'Plain', variant: 'danger'});
  await expect(toasts(page)).toHaveCount(4);
  await expect(toasts(page).last()).not.toHaveAttribute('data-variant');
  await expect(toasts(page).last().locator('.toast-description')).toHaveCount(0);
  await toast(page, {description: 'No title'});
  await expect(toasts(page)).toHaveCount(4);
  expect(messages).toEqual(['Shadleaf: an sl-toast event needs a title, e.g. {"title": "Member deleted"}.']);
});

test('the announcers are in the accessibility tree, empty, before any toast', async ({page, browserName}) => {
  test.skip(browserName !== 'chromium', 'reads the accessibility tree through the Chrome DevTools Protocol');
  await openFixture(page, toaster());
  const cdp = await page.context().newCDPSession(page);
  const {nodes} = await cdp.send('Accessibility.getFullAXTree') as {
    nodes: { ignored: boolean, role?: { value: string }, name?: { value: string },
      properties?: { name: string, value: { value: unknown } }[] }[]
  };
  const live = (role: string) => nodes.find(node => node.role?.value === role && !node.ignored);
  expect(live('status')?.properties?.find(property => property.name === 'live')?.value.value).toBe('polite');
  expect(live('alert')?.properties?.find(property => property.name === 'live')?.value.value).toBe('assertive');
  // The region is a landmark only while it holds toasts.
  expect(nodes.some(node => node.role?.value === 'region')).toBe(false);
  await toast(page, {title: 'Saved'});
  await expect(page.getByRole('region', {name: 'Notifications (Alt+T)'})).toBeVisible();
});

test('a toast rendered with the page shows, gets its close button and is announced', async ({page}) => {
  const messages = await openFixture(page, toaster('', SERVER_TOAST));

  const shown = toasts(page);
  await expect(shown).toHaveText(/Message sent/);
  await expect(shown).toHaveAttribute('data-enhanced', '');
  await expect(shown.getByRole('button', {name: 'Close'})).toBeVisible();
  await expect.poll(() => announced(page, 'status')).toBe('Message sent. We answer within one working day.');
  expect(messages).toEqual([]);
});

test('without Alpine, a toast rendered with the page still shows, without a close button', async ({page}) => {
  await openFixture(page, toaster('', SERVER_TOAST), {alpine: false});

  await expect(page.locator('.toast')).toBeVisible();
  await expect(page.locator('.toast .toast-close')).toBeHidden();
  expect(await page.locator('.toaster-viewport').evaluate(element => element.matches(':popover-open'))).toBe(false);
});

test('a toast closes after its duration, which stops on hover and on focus', async ({page}) => {
  await openFixture(page, `<button type="button" id="elsewhere">Elsewhere</button>${toaster('data-duration="800"')}`);

  await toast(page, {title: 'Short'});
  await expect(toasts(page)).toHaveCount(1);
  await expect(toasts(page)).toHaveCount(0, {timeout: 3000});

  // Hover: it stays; once the pointer leaves it gets at least a second more.
  await toast(page, {title: 'Hovered'});
  await toasts(page).hover();
  await page.waitForTimeout(1500);
  await expect(toasts(page)).toHaveCount(1);
  await page.mouse.move(0, 0);
  await expect(toasts(page)).toHaveCount(0, {timeout: 3000});

  // Focus: the same.
  await page.locator('#elsewhere').focus();
  await toast(page, {title: 'Focused'});
  await page.keyboard.press('Alt+KeyT');
  await expect(toasts(page)).toBeFocused();
  await page.waitForTimeout(1500);
  await expect(toasts(page)).toHaveCount(1);
  await page.keyboard.press('Shift+Tab');
  await expect(toasts(page)).toHaveCount(0, {timeout: 3000});

  // Its own duration wins; 0 keeps it.
  await toast(page, [{title: 'Kept', duration: 0}, {title: 'Long', duration: 60000}]);
  await page.waitForTimeout(1500);
  await expect(toasts(page)).toHaveCount(2);
});

test('at most visible-toasts show; older ones wait and come back', async ({page}) => {
  await openFixture(page, toaster('data-duration="0" data-visible-toasts="2"'));

  await toast(page, [{title: 'One'}, {title: 'Two'}, {title: 'Three'}]);
  await expect(toasts(page)).toHaveCount(3);
  await expect(page.locator('.toast[data-hidden]')).toHaveText(/One/);
  await expect(page.locator('.toast[data-hidden]')).toBeHidden();
  await toasts(page).last().getByRole('button', {name: 'Close'}).click();
  await expect(toasts(page)).toHaveCount(2);
  await expect(page.locator('.toast[data-hidden]')).toHaveCount(0);
  await expect(toasts(page).first()).toBeVisible();
});

test('a toast shows above an open modal dialog, and can be used there', async ({page}) => {
  const messages = await openFixture(page, `
      <button type="button" id="open" commandfor="d" command="show-modal">Edit</button>
      ${dialog('d', '<input id="name" aria-label="Name">')}
      ${toaster('data-duration="0"')}`);
  const d = page.locator('#d');

  await page.locator('#open').click();
  await expect(d).toHaveJSProperty('open', true);
  await toast(page, {title: 'Draft saved'});
  const shown = toasts(page);
  await expect(shown).toBeVisible();
  // Inside the dialog, so the dialog does not make it inert.
  expect(await page.locator('.toaster-viewport').evaluate(viewport => viewport.parentElement?.id)).toBe('d');
  const box = (await shown.boundingBox())!;
  expect(await page.evaluate(({x, y}) => !!document.elementFromPoint(x, y)?.closest('.toast'),
      {x: box.x + box.width / 2, y: box.y + box.height / 2}), 'drawn above the dialog').toBe(true);
  await expect.poll(() => announced(page, 'status')).toBe('Draft saved');

  // Escape closes the focused toast, not the dialog.
  await page.locator('#name').focus();
  await page.keyboard.press('Alt+KeyT');
  await expect(shown).toBeFocused();
  await page.keyboard.press('Escape');
  await expect(toasts(page)).toHaveCount(0);
  await expect(d).toHaveJSProperty('open', true);
  await expect(page.locator('#name'), 'the focus goes back to where it was').toBeFocused();

  // A click on the close button closes the toast, and a click on a toast does not light-dismiss the dialog.
  await toast(page, {title: 'Draft saved again'});
  await toasts(page).locator('.toast-title').click();
  await toasts(page).getByRole('button', {name: 'Close'}).click();
  await expect(toasts(page)).toHaveCount(0);
  await expect(d).toHaveJSProperty('open', true);

  // A toast showing while the dialog closes goes back to the page with it.
  await toast(page, {title: 'Still here'});
  await page.keyboard.press('Escape');
  await expect(d).toHaveJSProperty('open', false);
  await expect.poll(() => page.locator('.toaster-viewport').evaluate(viewport => viewport.parentElement?.id))
      .toBe('toaster');
  await expect(toasts(page)).toBeVisible();
  expect(await page.locator('.toaster-viewport').evaluate(viewport => viewport.matches(':popover-open'))).toBe(true);

  // A dialog opened while a toast shows takes the toaster in too.
  await page.locator('#open').click();
  await expect(d).toHaveJSProperty('open', true);
  await expect.poll(() => page.locator('.toaster-viewport').evaluate(viewport => viewport.parentElement?.id)).toBe('d');
  await toasts(page).getByRole('button', {name: 'Close'}).click();
  await expect(toasts(page)).toHaveCount(0);
  expect(messages).toEqual([]);
});

test('a dialog removed while open does not take the toaster, or its toasts, with it', async ({page}) => {
  await openFixture(page, `
      <div id="modal-root">${dialog('d', '<p>Open</p>')}</div>
      ${toaster('data-duration="0"')}`);
  await page.locator('#d').evaluate(element => (element as HTMLDialogElement).showModal());
  await toast(page, {title: 'First'});
  await expect.poll(() => page.locator('.toaster-viewport').evaluate(viewport => viewport.parentElement?.id)).toBe('d');

  await page.locator('#modal-root').evaluate(root => root.replaceChildren());
  await toast(page, {title: 'Second'});
  await expect(toasts(page)).toHaveText(['First', 'Second']);
  expect(await page.locator('.toaster-viewport').evaluate(viewport => viewport.parentElement?.id)).toBe('toaster');
});

function htmxButton(path: string): string {
  return `<button type="button" id="send" hx-post="${path}" hx-swap="none">Send</button>`;
}

test('htmx: HX-Trigger sends a toast', async ({page}) => {
  const detail = {title: 'Zoë was invited', description: null, variant: 'success', duration: null};
  const messages = await openFixture(page, htmxButton('/invite') + toaster(), {
    htmx: true,
    routes: {'/invite': () => ({body: '', headers: {'HX-Trigger': JSON.stringify({'sl-toast': detail})}})},
  });

  await page.locator('#send').click();
  await expect(toasts(page)).toHaveText(['Zoë was invited']);
  await expect(toasts(page).locator('.toast-description')).toHaveCount(0);
  await expect.poll(() => announced(page, 'status')).toBe('Zoë was invited');
  expect(messages).toEqual([]);
});

for (const order of [['sl-dialog-close', 'sl-toast'], ['sl-toast', 'sl-dialog-close']]) {
  test(`htmx: a toast and sl-dialog-close in one response, ${order.join(' before ')}`, async ({page}) => {
    const events: Record<string, unknown> = {};
    for (const name of order) {
      events[name] = name === 'sl-toast' ? {title: 'Member saved', variant: 'success'} : null;
    }
    const routes: Routes = {'/save': () => ({body: '', headers: {'HX-Trigger': JSON.stringify(events)}})};
    const messages = await openFixture(page, `
        <button type="button" id="open" commandfor="d" command="show-modal">Edit</button>
        ${dialog('d', htmxButton('/save'))}
        ${toaster()}`, {htmx: true, routes});
    const d = page.locator('#d');

    await page.locator('#open').click();
    await page.locator('#send').click();
    await expect(d).toHaveJSProperty('open', false);
    await expect(toasts(page)).toHaveText(['Member saved']);
    await expect.poll(() => page.locator('.toaster-viewport').evaluate(viewport => viewport.parentElement?.id))
        .toBe('toaster');
    await expect.poll(() => announced(page, 'status')).toBe('Member saved');
    await expect(page.locator('#open'), 'closing returns the focus to the button that opened it').toBeFocused();
    expect(messages).toEqual([]);
  });
}

test('htmx: a toast swapped in out of band', async ({page}) => {
  const messages = await openFixture(page, htmxButton('/invite') + toaster(), {
    htmx: true,
    routes: {'/invite': () => ({body: `<ol hx-swap-oob="beforeend:#toaster">${SERVER_TOAST}</ol>`})},
  });

  await page.locator('#send').click();
  await expect(toasts(page)).toHaveText([/Message sent/]);
  expect(await toasts(page).evaluate(item => item.parentElement?.classList.contains('toaster-list'))).toBe(true);
  await expect(toasts(page).getByRole('button', {name: 'Close'})).toBeVisible();
  await expect.poll(() => announced(page, 'status')).toBe('Message sent. We answer within one working day.');
  expect(messages).toEqual([]);
});

test('htmx: a toast does not come back from the history cache', async ({page}) => {
  const page2 = (url: URL) => url.pathname === '/'
      ? `<a href="/other" id="other" hx-get="/other" hx-target="main" hx-push-url="true">Other</a>${toaster('data-duration="0"')}`
      : '';
  const messages = await openFixture(page, page2, {
    htmx: true,
    routes: {'/other': () => ({body: '<p id="other-page">Other page</p>'})},
  });

  await toast(page, {title: 'Saved'});
  await expect(toasts(page)).toHaveCount(1);
  await page.locator('#other').click();
  await expect(page.locator('#other-page')).toBeVisible();
  await page.goBack();
  await expect(page.locator('#other')).toBeVisible();
  await page.waitForFunction(() => document.querySelector('.toaster-viewport')?.matches(':popover-open'));
  await expect(page.locator('.toast')).toHaveCount(0);
  await toast(page, {title: 'After'});
  await expect(toasts(page)).toHaveText(['After']);
  expect(messages).toEqual([]);
});

test('a toast slides in from the edge only where motion is fine', async ({page}) => {
  await openFixture(page, toaster('data-duration="0"'));
  const transition = () => toasts(page).last().evaluate(element => getComputedStyle(element).transitionProperty);

  await toast(page, {title: 'Reduced'});
  expect(await transition()).toBe('opacity');
  await page.emulateMedia({reducedMotion: 'no-preference'});
  await toast(page, {title: 'Moving'});
  expect(await transition()).toContain('translate');
});

test('forced colours: a toast keeps its border and shows its focus', async ({page}) => {
  await openFixture(page, toaster('data-duration="0"'));
  await page.emulateMedia({forcedColors: 'active'});
  await toast(page, {title: 'Saved'});
  await page.keyboard.press('Alt+KeyT');
  const style = await toasts(page).evaluate(element => {
    const computed = getComputedStyle(element);
    return {border: computed.borderTopStyle, borderWidth: parseFloat(computed.borderTopWidth),
      outline: computed.outlineStyle, outlineWidth: parseFloat(computed.outlineWidth)};
  });
  expect(style.border).toBe('solid');
  expect(style.borderWidth).toBeGreaterThan(0);
  expect(style.outline).toBe('solid');
  expect(style.outlineWidth).toBeGreaterThan(0);
});

test('two toasters on a page show each toast once', async ({page}) => {
  await openFixture(page, toaster() + toaster().replace('id="toaster"', 'id="second"'));

  await toast(page, {title: 'Once'});
  await expect(page.locator('.toast')).toHaveCount(1);
});
