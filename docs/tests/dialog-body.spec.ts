import {expect, test, type Locator, type Page} from '@playwright/test';
import AxeBuilder from '@axe-core/playwright';
import previews from '../src/generated/previews.json' with {type: 'json'};
import {openFixture, type Request, type Routes} from './fixture';

// sl:sheet-body and sl:dialog-body as htmx targets, in fixture pages under a strict Content-Security-Policy with the
// csp Alpine build and htmx: a request from outside opens the sheet at once, the spinner shows only for a slow answer,
// requests into a busy body are cancelled, closing aborts a request, an empty body is emptied on close, failed requests
// show their error as a toast or in the sheet, and the content scrolls between the header and the footer.

const TAGS = ['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa', 'wcag22aa', 'best-practice'];

type Scenario = { id: string, html: string };
const scenarios = previews.scenarios as Scenario[];

/** The <dialog> of a preview scenario, under another id and without the preview's fixed data-loading. */
function dialogOf(scenario: string, previewId: string, id: string): string {
  const html = scenarios.find(candidate => candidate.id === scenario)!.html;
  return html.slice(html.indexOf('<dialog'), html.lastIndexOf('</dialog>') + '</dialog>'.length)
      .replaceAll(previewId, id)
      .replace(' data-loading="true"', '');
}

type BodyOptions = { content?: string, footer?: string, bodyAttributes?: string };

/** A sheet as <sl:sheet><sl:sheet-header>...<sl:sheet-body/><sl:sheet-footer close-button/></sl:sheet> renders it. */
function sheet(id: string, {content = '', footer = '', bodyAttributes = ''}: BodyOptions = {}): string {
  let html = dialogOf('sheet--loading', 'sheet-loading', id)
      .replace('<div class="sheet-footer">', `<div class="sheet-footer">${footer}`)
      .replace(`<div class="sheet-body"`, `<div class="sheet-body"${bodyAttributes ? ' ' + bodyAttributes : ''}`);
  if (content) {
    html = html.replace(` id="${id}-body" data-clear-on-close="true"></div>`, ` id="${id}-body">${content}</div>`);
  }
  return html;
}

/** A dialog as <sl:dialog> with a header, <sl:dialog-body/> and <sl:dialog-footer close-button/> renders it. */
function dialog(id: string): string {
  return dialogOf('dialog--loading', 'dialog-loading', id);
}

/** The markup <sl:toaster/> renders, from the toaster's default scenario. */
function toaster(): string {
  const html = scenarios.find(scenario => scenario.id === 'toaster--default')!.html;
  return html.slice(html.indexOf('<div class="toaster"')).replace('id="toaster-default"', 'id="toaster"');
}

const DETAILS = '<p id="details">Ada Lovelace, ada@example.com</p>';
const LOAD = '<button type="button" id="load" hx-get="/details" hx-target="#member-body">Details</button>';

/** Opens a fixture with htmx; returns what the console said. */
function open(page: Page, main: string, routes: Routes): Promise<string[]> {
  return openFixture(page, main, {htmx: true, routes});
}

const opacity = (element: Locator) => element.evaluate(node => getComputedStyle(node).opacity);

async function waitUntilOpen(dialog: Locator) {
  await expect(dialog).toHaveJSProperty('open', true);
  await expect.poll(() => opacity(dialog)).toBe('1');
}

test('a request from outside opens the sheet at once; the spinner shows once the answer takes a moment', async ({page}) => {
  const messages = await open(page, LOAD + sheet('member'), {'/details': () => ({body: DETAILS, delay: 1200})});
  const member = page.locator('#member');
  const body = member.locator('.sheet-body');
  const loading = member.locator('.sheet-body-loading');

  await page.locator('#load').click();
  await expect(member).toHaveJSProperty('open', true);
  expect(await member.evaluate(element => element.matches(':modal'))).toBe(true);
  await expect(body).toHaveAttribute('data-loading', 'true');
  await expect(member.locator('#member-body')).toHaveAttribute('aria-busy', 'true');
  expect(await opacity(loading), 'no spinner at first').toBe('0');
  await expect.poll(() => opacity(loading)).toBe('1');
  expect(await loading.locator('svg').evaluate(svg => getComputedStyle(svg).animationName)).not.toBe('none');

  await expect(member.locator('#details')).toBeVisible();
  await expect(body).not.toHaveAttribute('data-loading');
  await expect(member.locator('#member-body')).not.toHaveAttribute('aria-busy');
  await expect.poll(() => opacity(loading)).toBe('0');
  await expect(member.locator('.sheet-body-error')).toBeHidden();

  await page.keyboard.press('Escape');
  await expect(member).toHaveJSProperty('open', false);
  await expect(page.locator('#load')).toBeFocused();
  expect(messages).toEqual([]);
});

test('a quick answer never shows the spinner', async ({page}) => {
  await open(page, LOAD + sheet('member'), {'/details': () => ({body: DETAILS})});
  await page.evaluate(() => {
    const loading = document.querySelector('#member .sheet-body-loading')!;
    const window_ = window as unknown as { maxOpacity: number };
    window_.maxOpacity = 0;
    const sample = () => {
      // Only while it is drawn: a closed sheet's layer has its target opacity, but is not displayed.
      if (loading.checkVisibility()) {
        window_.maxOpacity = Math.max(window_.maxOpacity, Number(getComputedStyle(loading).opacity));
      }
      requestAnimationFrame(sample);
    };
    sample();
  });

  await page.locator('#load').click();
  await expect(page.locator('#details')).toBeVisible();
  await page.waitForTimeout(400);
  expect(await page.evaluate(() => (window as unknown as { maxOpacity: number }).maxOpacity)).toBe(0);
});

test('the open sheet passes axe while it loads and once the content is in', async ({page}) => {
  await open(page, LOAD + sheet('member'), {'/details': () => ({body: DETAILS, delay: 800})});
  const member = page.locator('#member');

  await page.locator('#load').click();
  await waitUntilOpen(member);
  await expect.poll(() => opacity(member.locator('.sheet-body-loading'))).toBe('1');
  let results = await new AxeBuilder({page}).include('#member').withTags(TAGS).analyze();
  expect(results.violations.map(violation => violation.id)).toEqual([]);

  await expect(member.locator('#details')).toBeVisible();
  results = await new AxeBuilder({page}).include('#member').withTags(TAGS).analyze();
  expect(results.violations.map(violation => violation.id)).toEqual([]);
});

const FORM = `<form id="edit-form" hx-post="/save" hx-target="this" hx-swap="outerHTML">
    <label for="name">Name</label><input id="name" name="name" value="Ada"></form>`;
const SAVE = '<button type="submit" form="edit-form" id="save">Save</button>';

test('while a request runs, the content takes no clicks and a second Save sends nothing', async ({page}) => {
  let saves = 0;
  const messages = await open(page, `<button type="button" id="edit" hx-get="/edit" hx-target="#member-body">Edit</button>
      ${sheet('member', {footer: SAVE})}`, {
    '/edit': () => ({body: FORM}),
    '/save': () => {
      saves++;
      return {body: FORM.replace('value="Ada"', 'value="Ada Lovelace"'), delay: 800};
    },
  });
  const member = page.locator('#member');

  await page.locator('#edit').click();
  await expect(page.locator('#name')).toBeVisible();
  await page.locator('#save').click();
  await expect(member.locator('.sheet-body')).toHaveAttribute('data-loading', 'true');
  await page.locator('#save').click();
  await page.keyboard.press('Enter');
  // The loading layer is what a click on the field reaches.
  expect(await page.evaluate(() => {
    const box = document.querySelector('#name')!.getBoundingClientRect();
    return document.elementFromPoint(box.x + 5, box.y + box.height / 2)?.className;
  })).toBe('sheet-body-loading');

  await expect(page.locator('#name')).toHaveValue('Ada Lovelace');
  await page.waitForTimeout(1000);
  expect(saves).toBe(1);
  await expect(member).toHaveJSProperty('open', true);
  expect(messages).toEqual([]);
});

test('the content that opened the sheet takes the focus: the [autofocus] element, else the first control', async ({page}) => {
  await open(page, `<button type="button" id="edit" hx-get="/edit" hx-target="#member-body">Edit</button>
      <button type="button" id="notes" hx-get="/notes" hx-target="#member-body">Notes</button>
      <button type="button" id="load" hx-get="/details" hx-target="#member-body">Details</button>
      ${sheet('member', {footer: SAVE})}`, {
    '/edit': () => ({body: FORM, delay: 300}),
    '/notes': () => ({body: FORM.replace('</form>', '<label for="notes-field">Notes</label><textarea id="notes-field" '
          + 'name="notes" autofocus></textarea></form>')}),
    '/details': () => ({body: DETAILS}),
  });
  const member = page.locator('#member');

  await page.locator('#edit').click();
  await expect(page.locator('#name')).toBeFocused();
  await page.keyboard.press('Escape');
  await expect(member).toHaveJSProperty('open', false);

  await page.locator('#notes').click();
  await expect(page.locator('#notes-field')).toBeFocused();
  await page.keyboard.press('Escape');
  await expect(page.locator('#notes')).toBeFocused();

  // Nothing to focus in the content: the focus stays where showModal put it.
  await page.locator('#load').click();
  await expect(page.locator('#details')).toBeVisible();
  expect(await member.evaluate(element => element.contains(document.activeElement))).toBe(true);
});

test('closing the sheet aborts the request; nothing is swapped in afterwards', async ({page}) => {
  let requests = 0;
  await open(page, LOAD + sheet('member'), {'/details': () => {
    requests++;
    return {body: DETAILS, delay: 1500};
  }});
  const member = page.locator('#member');

  await page.locator('#load').click();
  await expect(member).toHaveJSProperty('open', true);
  await expect(member.locator('.sheet-body')).toHaveAttribute('data-loading', 'true');
  await page.keyboard.press('Escape');
  await expect(member).toHaveJSProperty('open', false);
  await expect(member.locator('.sheet-body')).not.toHaveAttribute('data-loading');
  await page.waitForTimeout(2000);
  await expect(member.locator('#member-body')).toBeEmpty();
  expect(requests).toBe(1);
});

test('an empty body is emptied once the sheet closes, so it opens without the previous content', async ({page}) => {
  await open(page, `<button type="button" id="ada" hx-get="/members/1" hx-target="#member-body">Ada</button>
      <button type="button" id="grace" hx-get="/members/2" hx-target="#member-body">Grace</button>
      ${sheet('member')}`, {
    '/members/1': () => ({body: '<p id="ada-details">Ada Lovelace</p>'}),
    '/members/2': () => ({body: '<p id="grace-details">Grace Hopper</p>', delay: 800}),
  });
  const member = page.locator('#member');

  await page.locator('#ada').click();
  await expect(page.locator('#ada-details')).toBeVisible();
  await page.keyboard.press('Escape');
  await expect(member).toHaveJSProperty('open', false);
  await expect(member.locator('#member-body')).toBeEmpty();

  // Right after closing, before the closing fade is over, too.
  await page.locator('#ada').click();
  await expect(page.locator('#ada-details')).toBeVisible();
  await page.keyboard.press('Escape');
  await page.locator('#grace').click();
  await expect(member).toHaveJSProperty('open', true);
  await expect(page.locator('#ada-details')).toHaveCount(0);
  await expect(page.locator('#grace-details')).toBeVisible();
});

test('content the page put in the body stays when the sheet closes', async ({page}) => {
  await open(page, `<button type="button" id="open" commandfor="help" command="show-modal">Help</button>
      ${sheet('help', {content: '<p id="help-text">Some help</p>'})}`, {});
  const help = page.locator('#help');

  await page.locator('#open').click();
  await waitUntilOpen(help);
  await page.keyboard.press('Escape');
  await expect(help).toHaveJSProperty('open', false);
  await page.waitForTimeout(500);
  await expect(help.locator('#help-text')).toHaveCount(1);
});

test('a failed load closes the sheet and shows the error as a toast', async ({page}) => {
  await open(page, LOAD + sheet('member') + toaster(), {'/details': () => ({body: 'Oops', status: 500})});
  const member = page.locator('#member');

  await page.locator('#load').click();
  await expect(page.locator('.toaster-list > .toast[data-variant="error"]'))
      .toContainText('Something went wrong. Please try again.');
  await expect(member).toHaveJSProperty('open', false);
  await expect(page.locator('#load')).toBeFocused();
  await expect(member.locator('.sheet-body-error')).toBeHidden();
});

test('the error slot is the toast\'s text', async ({page}) => {
  const custom = sheet('member').replace('Something went wrong. Please try again.', 'The member could not be loaded.');
  await open(page, LOAD + custom + toaster(), {'/details': () => ({body: 'Oops', status: 503})});

  await page.locator('#load').click();
  await expect(page.locator('.toaster-list > .toast')).toContainText('The member could not be loaded.');
});

test('without a toaster on the page, a failed load shows the error in the sheet', async ({page}) => {
  await open(page, LOAD + sheet('member'), {'/details': () => ({body: 'Oops', status: 500})});

  await page.locator('#load').click();
  await expect(page.locator('#member .sheet-body-error')).toBeVisible();
  await expect(page.locator('#member')).toHaveJSProperty('open', true);
  await expect(page.locator('#member .sheet-body')).not.toHaveAttribute('data-loading');

  // Closing hides the error again.
  await page.keyboard.press('Escape');
  await expect(page.locator('#member .sheet-body-error')).toBeHidden();
});

test('load-error="inline" keeps the sheet open with the error, also with a toaster on the page', async ({page}) => {
  await open(page, `<button type="button" id="load" hx-get="/details" hx-target="#inline-body">Details</button>
      ${sheet('inline', {bodyAttributes: 'data-on-load-error="inline"'})}${toaster()}`, {
    '/details': () => ({body: 'Oops', status: 500}),
  });

  await page.locator('#load').click();
  await expect(page.locator('#inline .sheet-body-error')).toBeVisible();
  await expect(page.locator('#inline')).toHaveJSProperty('open', true);
  await expect(page.locator('.toaster-list > .toast')).toHaveCount(0);
});

test('a failed save shows the error in the sheet and keeps what was typed; the next request hides it', async ({page}) => {
  let fail = true;
  await open(page, `<button type="button" id="edit" hx-get="/edit" hx-target="#member-body">Edit</button>
      ${sheet('member', {footer: SAVE})}${toaster()}`, {
    '/edit': () => ({body: FORM}),
    '/save': () => fail ? {body: 'Oops', status: 500} : {body: '<p id="saved">Saved</p>'},
  });
  const member = page.locator('#member');

  await page.locator('#edit').click();
  await page.locator('#name').fill('Ada Lovelace');
  await page.locator('#save').click();
  await expect(member.locator('.sheet-body-error')).toBeVisible();
  await expect(member).toHaveJSProperty('open', true);
  await expect(page.locator('#name')).toHaveValue('Ada Lovelace');
  await expect(page.locator('.toaster-list > .toast')).toHaveCount(0);

  fail = false;
  await page.locator('#save').click();
  await expect(page.locator('#saved')).toBeVisible();
  await expect(member.locator('.sheet-body-error')).toBeHidden();
});

test('HX-Trigger sl-dialog-close on a save closes the sheet; the body is emptied', async ({page}) => {
  const messages = await open(page, `<button type="button" id="edit" hx-get="/edit" hx-target="#member-body">Edit</button>
      <p id="row">Ada</p>${sheet('member', {footer: SAVE})}`, {
    '/edit': () => ({body: FORM}),
    '/save': (request: Request) => ({
      body: `<p id="row" hx-swap-oob="true">${new URLSearchParams(request.body).get('name')}</p>`,
      headers: {'HX-Trigger': 'sl-dialog-close', 'HX-Reswap': 'none'},
    }),
  });
  const member = page.locator('#member');

  await page.locator('#edit').click();
  await page.locator('#name').fill('Ada Lovelace');
  await page.locator('#save').click();
  await expect(member).toHaveJSProperty('open', false);
  await expect(page.locator('#row')).toHaveText('Ada Lovelace');
  await expect(member.locator('#member-body')).toBeEmpty();
  await expect(page.locator('#edit')).toBeFocused();
  expect(messages).toEqual([]);
});

test('a request from inside a closed sheet opens nothing', async ({page}) => {
  let requests = 0;
  await open(page, sheet('member', {content: '<div hx-get="/lazy" hx-trigger="load" hx-target="this">Lazy</div>'}), {
    '/lazy': () => {
      requests++;
      return {body: '<p id="lazy">Loaded</p>'};
    },
  });

  await expect.poll(() => requests).toBe(1);
  await expect(page.locator('#lazy')).toHaveCount(1);
  await expect(page.locator('#member')).toHaveJSProperty('open', false);
});

test('htmx history: back shows the page with the sheet closed and its body empty', async ({page}) => {
  await open(page, `${LOAD}
      <a href="/next" id="next" hx-get="/next" hx-target="main" hx-push-url="true">Next page</a>
      ${sheet('member', {content: ''})}`, {
    '/details': () => ({body: `${DETAILS}<a href="/next" id="next-inside" hx-get="/next" hx-target="main"
        hx-push-url="true">Next page</a>`}),
    '/next': () => ({body: '<p id="next-page">Next page</p>'}),
  });

  await page.locator('#load').click();
  await page.locator('#next-inside').click();
  await expect(page.locator('#next-page')).toBeVisible();
  await page.goBack();
  await expect(page.locator('#load')).toBeVisible();
  expect(await page.evaluate(() => document.querySelector('dialog[open]'))).toBeNull();
  await expect(page.locator('#member-body')).toBeEmpty();
});

test('a dialog body loads the same way and scrolls between the header and the footer', async ({page}) => {
  const long = Array.from({length: 40}, (_, i) => `<p>Line ${i + 1}</p>`).join('');
  const messages = await open(page, `<button type="button" id="load" hx-get="/edit" hx-target="#edit-body">Edit</button>
      ${dialog('edit')}`, {'/edit': () => ({body: long, delay: 1200})});
  const edit = page.locator('#edit');

  await page.setViewportSize({width: 800, height: 500});
  await page.locator('#load').click();
  await expect(edit).toHaveJSProperty('open', true);
  await expect.poll(() => opacity(edit.locator('.dialog-body-loading'))).toBe('1');
  await expect(edit.getByText('Line 40')).toBeAttached();

  const sizes = await edit.evaluate(element => {
    const content = element.querySelector('.dialog-body-content')!;
    const footer = element.querySelector('.dialog-footer')!.getBoundingClientRect();
    return {
      dialogScrolls: element.scrollHeight > element.clientHeight,
      bodyScrolls: content.scrollHeight > content.clientHeight,
      footerInView: footer.bottom <= window.innerHeight,
    };
  });
  expect(sizes).toEqual({dialogScrolls: false, bodyScrolls: true, footerInView: true});
  expect(messages).toEqual([]);
});

test('a sheet body scrolls between the header and the footer', async ({page}) => {
  const long = Array.from({length: 60}, (_, i) => `<p>Line ${i + 1}</p>`).join('');
  await open(page, LOAD + sheet('member'), {'/details': () => ({body: long})});
  const member = page.locator('#member');

  await page.setViewportSize({width: 800, height: 500});
  await page.locator('#load').click();
  await expect(member.getByText('Line 60')).toBeAttached();
  const sizes = await member.evaluate(element => {
    const content = element.querySelector('.sheet-body-content')!;
    const header = element.querySelector('.sheet-header')!.getBoundingClientRect();
    const footer = element.querySelector('.sheet-footer')!.getBoundingClientRect();
    return {
      sheetScrolls: element.scrollHeight > element.clientHeight,
      bodyScrolls: content.scrollHeight > content.clientHeight,
      headerInView: header.top >= 0,
      footerInView: footer.bottom <= window.innerHeight,
    };
  });
  expect(sizes).toEqual({sheetScrolls: false, bodyScrolls: true, headerInView: true, footerInView: true});
});

test('removing a component keeps the listeners of another in the same x-data', async ({page}) => {
  // Alpine puts a property no scope declares on the outermost x-data: without cleanups declared, the components in
  // it shared one list of cleanups, and removing one removed the listeners of whichever started last.
  const messages = await open(page, `<div x-data>
      <button type="button" id="remove" hx-get="/gone" hx-target="#first-region" hx-swap="outerHTML">Remove</button>
      <div id="first-region">${dialog('first')}</div>
      ${LOAD.replace('#member-body', '#second-body')}${dialog('second')}</div>`, {
    '/gone': () => ({body: '<p id="gone">Removed</p>'}),
    '/details': () => ({body: DETAILS}),
  });

  await page.locator('#remove').click();
  await expect(page.locator('#gone')).toBeVisible();
  await page.locator('#load').click();
  await expect(page.locator('#second')).toHaveJSProperty('open', true);
  await expect(page.locator('#second #details')).toBeVisible();
  await page.locator('#details').dispatchEvent('sl-dialog-close', {bubbles: true});
  await expect(page.locator('#second')).toHaveJSProperty('open', false);
  await expect(page.locator('#second-body')).toBeEmpty();
  expect(messages).toEqual([]);
});
