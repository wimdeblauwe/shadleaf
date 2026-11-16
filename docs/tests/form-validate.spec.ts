import AxeBuilder from '@axe-core/playwright';
import {expect, type Locator, type Page, test} from '@playwright/test';
import previews from '../src/generated/previews.json' with {type: 'json'};
import {openFixture, type Request, type Response} from './fixture';

// sl:form validate on htmx 4, in a fixture page under a strict Content-Security-Policy with the csp Alpine build.
// The form is the preview form--fixture-valid: every control Shadleaf binds, in a sl:field or sl:field-set. Its
// server (validationServer) answers a validation request as an application's controller would: it validates the
// posted form and renders it again, each field from form--fixture-invalid when it breaks a rule and from
// form--fixture-valid otherwise, with the posted values, and the end time in an hx-partial when the start time was
// validated and the end has a value (what field.html's depends-on renders). htmx swaps the field the request names
// (hx-select) and the hx-partial, with outerMorph.

const TAGS = ['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa', 'wcag22aa', 'best-practice'];

type Scenario = { id: string, html: string };
const preview = (id: string) => (previews.scenarios as Scenario[]).find(scenario => scenario.id === `form--${id}`)!
    .html;
const VALID = preview('fixture-valid');
const INVALID = preview('fixture-invalid');

/** The rules of the fixture's form: the message for a field, or null when it is valid. */
const RULES: Record<string, (values: URLSearchParams) => string | null> = {
  title: values => !values.get('title')?.trim() ? 'Enter a title.'
      : values.get('title')!.length > 20 ? 'At most 20 characters.' : null,
  password: values => (values.get('password') ?? '').length < 8 ? 'At least 8 characters.' : null,
  notes: values => (values.get('notes') ?? '').length > 20 ? 'At most 20 characters.' : null,
  start: values => !values.get('start') ? 'Enter a start time.' : null,
  end: values => !values.get('end') ? 'Enter an end time.'
      : values.get('start') && values.get('end')! <= values.get('start')! ? 'The meeting must end after it starts.'
          : null,
  room: values => !values.get('room') ? 'Choose a room.' : null,
  fruit: values => !values.get('fruit') ? 'Choose a fruit.' : null,
  framework: values => !values.get('framework') ? 'Choose a framework.'
      : values.get('framework') === 'remix' ? 'Remix is not supported.' : null,
  day: values => !values.get('day') ? 'Pick a day.'
      : [0, 6].includes(new Date(`${values.get('day')}T00:00:00Z`).getUTCDay()) ? 'Pick a weekday.' : null,
  plan: values => !values.get('plan') ? 'Choose a plan.' : null,
  terms: values => values.get('terms') !== 'true' ? 'Accept the terms.' : null,
  updates: () => null,
};
/** depends-on of the fixture's fields. */
const DEPENDS_ON: Record<string, string[]> = {end: ['start']};

const escape = (text: string) => text.replace(/&/g, '&amp;').replace(/"/g, '&quot;').replace(/</g, '&lt;');

/** The markup of the field (div.field or fieldset.field-set) whose data-validate-field is `name`. */
function fieldMarkup(html: string, name: string): string {
  const marker = html.indexOf(`data-validate-field="${name}"`);
  const start = Math.max(html.lastIndexOf('<div class="field"', marker), html.lastIndexOf('<fieldset', marker));
  const tag = html.startsWith('<fieldset', start) ? 'fieldset' : 'div';
  const pattern = new RegExp(`<${tag}[\\s>]|</${tag}>`, 'g');
  pattern.lastIndex = start;
  let depth = 0;
  for (let match; (match = pattern.exec(html));) {
    depth += match[0].startsWith('</') ? -1 : 1;
    if (depth === 0) {
      return html.slice(start, pattern.lastIndex);
    }
  }
  throw new Error(`No field ${name}`);
}

/** A field's markup with the posted value, as th:field renders it (a password never gets its value back). */
function withValue(markup: string, name: string, values: URLSearchParams): string {
  const value = values.get(name) ?? '';
  const all = values.getAll(name);
  return markup
      .replace(new RegExp(`(<input(?=[^>]*name="${name}")[^>]*?)(?: checked="checked")?( value="([^"]*)")`, 'g'),
          (input, before: string, attribute: string, own: string) => {
            if (/type="password"/.test(input)) {
              return input;
            }
            if (/type="(checkbox|radio)"/.test(input)) {
              return `${before}${all.includes(own) ? ' checked="checked"' : ''}${attribute}`;
            }
            return `${before} value="${escape(value)}"`;
          })
      .replace(/(<textarea[^>]*>)[^<]*(<\/textarea>)/, `$1${escape(value)}$2`)
      .replace(/<option([^>]*?) selected="selected"/g, '<option$1')
      .replace(new RegExp(`(<option[^>]*value="${escape(value)}")`), '$1 selected="selected"')
      .replace(/ data-empty="true"/, value ? '' : ' data-empty="true"')
      .replace(/(<span class="date-picker-value">)[^<]*/, (_, span: string) => span + (value
          ? new Date(`${value}T00:00:00Z`).toLocaleDateString('en-US',
              {month: 'long', day: 'numeric', year: 'numeric', timeZone: 'UTC'})
          : 'Pick a date'));
}

/** The form rendered for the posted values. */
function render(values: URLSearchParams, validated: string | undefined): string {
  let html = VALID;
  for (const [name, rule] of Object.entries(RULES)) {
    const message = rule(values);
    let markup = message === null ? fieldMarkup(VALID, name)
        : fieldMarkup(INVALID, name).replace(/(class="field-error"\s+id="[^"]+">)[^<]*/, `$1${message}`);
    markup = withValue(markup, name, values);
    if (validated && validated !== name && DEPENDS_ON[name]?.includes(validated) && values.get(name)) {
      markup = `<hx-partial hx-target="[data-validate-field='${name}']" hx-swap="outerMorph">${markup}</hx-partial>`;
    }
    html = html.replace(fieldMarkup(VALID, name), () => markup);
  }
  return html;
}

type Validation = { field: string, values: URLSearchParams };

/** The routes of the fixture's server, and the validation requests it got. */
function validationServer() {
  const validations: Validation[] = [];
  const routes = {
    '/validate': (request: Request): Response => {
      const values = new URLSearchParams(request.body);
      const field = request.headers['sl-validate-field'];
      validations.push({field, values});
      return {body: render(values, field)};
    },
  };
  return {validations, routes};
}

async function openForm(page: Page) {
  const {validations, routes} = validationServer();
  const messages = await openFixture(page, VALID, {htmx: 4, routes});
  // Firefox warns about any password field on a page served over http, as the fixture's is.
  return {validations, messages: messages.filter(message => !message.includes('Password fields present'))};
}

/** Resolves once htmx has swapped the response to the next validation request. */
function nextSwap(page: Page) {
  return page.evaluate(() => new Promise<void>(resolve => document.addEventListener('htmx:after:settle',
      () => resolve(), {once: true})));
}

async function expectValid(control: Locator, field: Locator) {
  await expect(control).not.toHaveAttribute('aria-invalid');
  await expect(field).not.toHaveAttribute('data-invalid');
  await expect(field.locator('.field-error')).toHaveCount(0);
}

async function expectInvalid(control: Locator, field: Locator, message: string) {
  await expect(field.locator('.field-error')).toHaveText(message);
  await expect(control).toHaveAttribute('aria-invalid', 'true');
  await expect(field).toHaveAttribute('data-invalid', 'true');
  await expect(control).toHaveAccessibleDescription(message);
}

const field = (page: Page, name: string) => page.locator(`[data-validate-field="${name}"]`);

test('an error appears and clears while the user types, and the control keeps the focus and the caret',
    async ({page}) => {
      const {validations, messages} = await openForm(page);
      const title = page.getByLabel('Title');

      await title.click();
      await title.pressSequentially('A meeting about everything');
      await expectInvalid(title, field(page, 'title'), 'At most 20 characters.');
      await expect(title).toBeFocused();
      await expect(title).toHaveValue('A meeting about everything');
      // One request for the whole burst of keystrokes (delay:200ms).
      expect(validations.length).toBeLessThanOrEqual(2);

      // The caret stays where the user put it: in the middle of the text, not at its end. (Set from script: Home
      // does not move it in Firefox on macOS.)
      await title.evaluate((input: HTMLInputElement) => input.setSelectionRange(2, 2));
      const swapped = nextSwap(page);
      await title.press('Delete');
      await swapped;
      await expect(title).toBeFocused();
      expect(await title.evaluate((input: HTMLInputElement) => [input.selectionStart, input.selectionEnd]))
          .toEqual([2, 2]);

      await title.fill('Standup');
      await expectValid(title, field(page, 'title'));
      await expect(title).toBeFocused();
      expect(validations.at(-1)!.field).toBe('title');
      expect(validations.at(-1)!.values.get('title')).toBe('Standup');
      expect(messages).toEqual([]);
    });

test('fields the user has not touched stay clean', async ({page}) => {
  const {messages} = await openForm(page);
  await page.getByLabel('Title').fill('Standup');
  await page.getByLabel('Title').fill('');
  await expectInvalid(page.getByLabel('Title'), field(page, 'title'), 'Enter a title.');

  await expect(page.locator('[aria-invalid]')).toHaveCount(1);
  await expect(page.locator('[data-invalid]')).toHaveCount(1);
  expect(messages).toEqual([]);
});

test('a password keeps what the user typed, though the server never sends it back', async ({page}) => {
  const {validations} = await openForm(page);
  const password = page.getByLabel('Password');

  await password.pressSequentially('secret');
  await expectInvalid(password, field(page, 'password'), 'At least 8 characters.');
  await expect(password).toHaveValue('secret');
  await password.pressSequentially('-word');
  await expectValid(password, field(page, 'password'));
  await expect(password).toHaveValue('secret-word');
  expect(validations.at(-1)!.values.get('password')).toBe('secret-word');

  // Another field's validation morphs nothing of the password's field.
  await page.getByLabel('Title').fill('Standup');
  await expect.poll(() => validations.at(-1)!.field).toBe('title');
  await expect(password).toHaveValue('secret-word');
});

test('a textarea keeps its text and the description stays in its aria-describedby', async ({page}) => {
  await openForm(page);
  const notes = page.getByLabel('Notes');

  await notes.pressSequentially('A very long note that goes on');
  await expect(field(page, 'notes').locator('.field-error')).toHaveText('At most 20 characters.');
  await expect(notes).toHaveAttribute('aria-describedby', 'notes-description notes-error');
  await expect(notes).toHaveValue('A very long note that goes on');
  await expect(notes).toBeFocused();
});

test('a dependent field is validated again when the field it depends on changes, once it has a value',
    async ({page}) => {
      const {validations} = await openForm(page);
      const start = page.getByLabel('Start');
      const end = page.getByLabel('End');

      // The end has no value yet: validating the start leaves it alone.
      await start.fill('10:00');
      await expect.poll(() => validations.length).toBe(1);
      await expectValid(start, field(page, 'start'));
      await expectValid(end, field(page, 'end'));

      await end.fill('09:30');
      await expectInvalid(end, field(page, 'end'), 'The meeting must end after it starts.');

      // Now validating the start swaps the end too: an earlier start makes it valid, a later one invalid again.
      await start.fill('09:00');
      await expectValid(end, field(page, 'end'));
      await expect(end).toHaveValue('09:30');
      await start.fill('11:00');
      await expectInvalid(end, field(page, 'end'), 'The meeting must end after it starts.');
      expect(validations.at(-1)!.field).toBe('start');
      await expect(page.locator('hx-partial')).toHaveCount(0);
    });

test('passes axe with errors shown', async ({page}) => {
  await openForm(page);
  await page.getByLabel('Title').fill('x'.repeat(30));
  await page.getByLabel('Start').fill('10:00');
  await page.getByLabel('End').fill('09:00');
  await expect(field(page, 'end').locator('.field-error')).toBeVisible();
  await expect(field(page, 'title').locator('.field-error')).toBeVisible();

  const results = await new AxeBuilder({page}).include('form').withTags(TAGS).analyze();
  expect(results.violations.map(violation => `${violation.id}: ${violation.help}`)).toEqual([]);
});

test('a native select, a checkbox, a switch and a radio group validate on change', async ({page}) => {
  const {validations, messages} = await openForm(page);

  const room = page.getByLabel('Room');
  await room.focus();
  await room.selectOption('atrium');
  await expect.poll(() => validations.at(-1)?.field).toBe('room');
  await expectValid(room, field(page, 'room'));
  await room.selectOption('');
  await expectInvalid(room, field(page, 'room'), 'Choose a room.');
  await expect(room).toBeFocused();

  const terms = page.getByLabel('I accept the terms');
  await terms.check();
  await expect.poll(() => validations.at(-1)?.field).toBe('terms');
  await terms.uncheck();
  await expectInvalid(terms, field(page, 'terms'), 'Accept the terms.');
  await expect(terms).not.toBeChecked();
  await terms.check();
  await expectValid(terms, field(page, 'terms'));
  await expect(terms).toBeChecked();
  await expect(terms).toBeFocused();

  const updates = page.getByRole('switch', {name: 'Product updates'});
  const count = validations.length;
  await updates.check();
  await expect.poll(() => validations.at(-1)?.field).toBe('updates');
  expect(validations.length).toBe(count + 1);
  await expect(updates).toBeChecked();

  // The radio group's field-set is the target: its error describes the fieldset.
  const yearly = page.getByLabel('Yearly');
  await yearly.check();
  await expect.poll(() => validations.at(-1)?.field).toBe('plan');
  expect(validations.at(-1)!.values.get('plan')).toBe('yearly');
  await expect(yearly).toBeChecked();
  await expect(yearly).toBeFocused();
  await expect(field(page, 'plan')).not.toHaveAttribute('data-invalid');
  expect(messages).toEqual([]);
});

test('a select validates its choice and still opens after the swap', async ({page}) => {
  const {validations, messages} = await openForm(page);
  // The select itself, or in a browser without customizable selects (Firefox) the button slSelect puts in its place.
  const fruit = page.getByRole('combobox', {name: 'Fruit'});
  const select = page.locator('#fruit');

  await fruit.click();
  await page.getByRole('option', {name: 'Banana'}).click();
  await expect.poll(() => validations.at(-1)?.field).toBe('fruit');
  expect(validations.at(-1)!.values.get('fruit')).toBe('banana');
  await expectValid(select, field(page, 'fruit'));
  await expect(select).toHaveValue('banana');
  await expect(fruit).toContainText('Banana');
  await expect(fruit).not.toHaveAttribute('aria-invalid');

  await fruit.click();
  await page.getByRole('option', {name: 'Apple'}).click();
  await expect.poll(() => validations.at(-1)?.values.get('fruit')).toBe('apple');
  await expect(select).toHaveValue('apple');
  await expect(fruit).toContainText('Apple');
  expect(messages).toEqual([]);
});

test('a combobox validates its choice and keeps working after the swap', async ({page}) => {
  const {validations, messages} = await openForm(page);
  const input = page.getByRole('combobox', {name: 'Framework'});

  await input.click();
  await input.pressSequentially('Rem');
  await page.getByRole('option', {name: 'Remix'}).click();
  await expect.poll(() => validations.at(-1)?.field).toBe('framework');
  expect(validations.at(-1)!.values.get('framework')).toBe('remix');
  // The select gets the field's state, and the input the user works with follows it.
  await expectInvalid(page.locator('#framework'), field(page, 'framework'), 'Remix is not supported.');
  await expect(input).toHaveAttribute('aria-invalid', 'true');
  await expect(input).toHaveAccessibleDescription('Remix is not supported.');
  await expect(input).toHaveValue('Remix');
  await expect(input).toBeFocused();

  // Still a combobox: it filters and picks again, and its select follows.
  await input.fill('');
  await input.pressSequentially('Ast');
  await page.getByRole('option', {name: 'Astro'}).click();
  await expect.poll(() => validations.at(-1)?.values.get('framework')).toBe('astro');
  await expectValid(page.locator('#framework'), field(page, 'framework'));
  await expect(input).not.toHaveAttribute('aria-invalid');
  await expect(input).toHaveAccessibleDescription('');
  await expect(input).toHaveValue('Astro');
  await expect(input).toHaveAccessibleName('Framework');
  await expect(page.locator('#framework')).toHaveValue('astro');
  await expect(page.locator('#framework')).toHaveAttribute('aria-hidden', 'true');
  await expect(page.locator('.combobox')).toHaveAttribute('data-enhanced', 'true');
  expect(messages).toEqual([]);
});

test('a date picker validates the picked date and still opens after the swap', async ({page}) => {
  await page.clock.setFixedTime(new Date('2026-11-16T10:00:00'));
  const {validations, messages} = await openForm(page);
  const trigger = page.locator('.date-picker-trigger');
  const dialog = page.getByRole('dialog', {name: 'Choose a date'});

  // Saturday 21 November.
  await trigger.click();
  await dialog.getByRole('button', {name: /November 21/}).click();
  await expect.poll(() => validations.at(-1)?.field).toBe('day');
  expect(validations.at(-1)!.values.get('day')).toBe('2026-11-21');
  await expectInvalid(page.locator('#day'), field(page, 'day'), 'Pick a weekday.');
  // The button the user works with follows the input.
  await expect(trigger).toHaveAttribute('aria-invalid', 'true');
  await expect(trigger).toHaveAccessibleDescription('Pick a weekday.');
  await expect(trigger).toHaveAccessibleName(/Day.*November 21, 2026/);
  await expect(trigger).toBeFocused();

  // Still a date picker: it opens on the chosen month, picks again, and its button follows.
  await trigger.click();
  await expect(dialog).toBeVisible();
  await dialog.getByRole('button', {name: /November 18/}).click();
  await expect.poll(() => validations.at(-1)?.values.get('day')).toBe('2026-11-18');
  await expectValid(page.locator('#day'), field(page, 'day'));
  await expect(trigger).not.toHaveAttribute('aria-invalid');
  await expect(trigger).not.toHaveAttribute('aria-describedby');
  await expect(trigger).toHaveAccessibleName(/Day.*November 18, 2026/);
  await expect(page.locator('#day')).toHaveValue('2026-11-18');
  await trigger.click();
  await expect(dialog.getByRole('group', {name: 'Day'})).toBeVisible();
  expect(messages).toEqual([]);
});

// sl:validate on htmx 4: a validation request (it carries SL-Validate-Field) leaves the form's files out
// (validate.js), whatever the form's encoding; any other htmx request from the form still sends them.

const PNG = {name: 'photo.png', mimeType: 'image/png', buffer: Buffer.from('not really a png')};

/** The markup sl:validate writes on a control in a sl:field (see FormApprovalTest), next to a file input. */
const page = (enctype: string) => `<form method="post" enctype="${enctype}">
  <div class="field" data-validate-field="title">
    <label class="label field-label" for="title">Title</label>
    <input class="input" type="text" id="title" name="title" hx-post="/validate"
           hx-trigger="input changed delay:200ms" hx-include="closest form" hx-swap="outerMorph"
           hx-target="closest .field" hx-select="[data-validate-field='title']"
           hx-headers="SL-Validate-Field:'title'">
  </div>
  <label class="label" for="photo">Photo</label>
  <input type="file" id="photo" name="photo">
  <button type="button" hx-post="/save" hx-swap="none">Save</button>
</form>`;

function recorder() {
  const requests: Record<string, Request[]> = {validate: [], save: []};
  const routes = {
    '/validate': (request: Request) => {
      requests.validate.push(request);
      return {body: page('')};
    },
    '/save': (request: Request) => {
      requests.save.push(request);
      return {body: ''};
    },
  };
  return {requests, routes};
}

for (const enctype of ['multipart/form-data', 'application/x-www-form-urlencoded']) {
  test(`a validation request sends no files (${enctype})`, async ({page: browser}) => {
    const {requests, routes} = recorder();
    const messages = await openFixture(browser, page(enctype), {htmx: 4, routes});
    await browser.getByLabel('Photo').setInputFiles(PNG);

    await browser.getByLabel('Title').pressSequentially('Standup');
    await expect.poll(() => requests.validate.length).toBe(1);

    const request = requests.validate[0];
    expect(request.headers['sl-validate-field']).toBe('title');
    expect(request.headers['content-type']).toContain(enctype);
    expect(request.body).toContain('Standup');
    expect(request.body).not.toContain('photo');
    expect(request.body).not.toContain('not really a png');
    expect(request.body).not.toContain('object File');
    expect(messages).toEqual([]);
  });
}

test('another htmx request from the form still sends its files', async ({page: browser}) => {
  const {requests, routes} = recorder();
  await openFixture(browser, page('multipart/form-data'), {htmx: 4, routes});
  await browser.getByLabel('Photo').setInputFiles(PNG);

  await browser.getByRole('button', {name: 'Save'}).click();
  await expect.poll(() => requests.save.length).toBe(1);

  expect(requests.save[0].body).toContain('filename="photo.png"');
  expect(requests.save[0].body).toContain('not really a png');
});
