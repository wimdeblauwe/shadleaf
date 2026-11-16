import {expect, test} from '@playwright/test';
import {openFixture, type Request} from './fixture';

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
