import {expect, test, type Locator, type Page} from '@playwright/test';
import previews from '../src/generated/previews.json' with {type: 'json'};
import {openFixture} from './fixture';
import {combinations, openShowcase} from './showcase';

// sl:file-upload. On the showcase, in every skin and theme: the hidden input's focus ring shows on the Browse label, a
// field error colours the drop zone's border, and a disabled one's Browse label fades. Fixture pages under a strict
// Content-Security-Policy with the csp Alpine build, the markup the docs previews': picking and dropping files (through
// a DataTransfer, as a real drop hands them over), adding more, removing one, accept and max-size, one file without
// multiple, the announcements, the keyboard, the submitted form, htmx (swapped in, out, history restore) and the page
// without Alpine. The upload progress with htmx 2 and htmx 4: each file's share, processing, the outcomes, Cancel, a
// request without the files, and htmx 2's history snapshot taken while uploading (htmx 4 keeps no snapshot: it asks
// the server for the page again). file-upload-network.spec.ts has the browser's own upload events. Runs in Chromium
// and Firefox.

type Scenario = { id: string, html: string };
const preview = (id: string) => (previews.scenarios as Scenario[])
    .find(scenario => scenario.id === `file-upload--${id}`)!.html;
const scenario = (page: Page, id: string) => page.locator(`[data-scenario="file-upload--${id}"]`);

type FileSpec = { name: string, type: string, size?: number, lastModified?: number };
const pdf = (name: string): FileSpec => ({name, type: 'application/pdf', size: 1200});
const png = (name: string): FileSpec => ({name, type: 'image/png', size: 2600});

/** Files for setInputFiles, as the picker hands them over. */
const picked = (...files: FileSpec[]) => files.map(file => ({
  name: file.name, mimeType: file.type, buffer: Buffer.alloc(file.size ?? 10, 1),
}));

/** Drags the files over `target` and drops them, as the browser does: dragenter, dragover, drop. */
async function drop(page: Page, target: Locator, files: FileSpec[], {leave = false} = {}) {
  const dataTransfer = await page.evaluateHandle(files => {
    const transfer = new DataTransfer();
    for (const file of files) {
      transfer.items.add(new File([new Uint8Array(file.size ?? 10)], file.name,
          {type: file.type, lastModified: file.lastModified}));
    }
    return transfer;
  }, files);
  await target.dispatchEvent('dragenter', {dataTransfer});
  await target.dispatchEvent('dragover', {dataTransfer});
  if (leave) {
    await target.dispatchEvent('dragleave', {dataTransfer});
    return;
  }
  await target.dispatchEvent('drop', {dataTransfer});
}

class Upload {
  readonly root: Locator;
  readonly input: Locator;
  readonly list: Locator;
  readonly status: Locator;

  constructor(readonly page: Page, id: string) {
    this.input = page.locator(`#${id}`);
    this.root = page.locator('.file-upload').filter({has: this.input});
    this.list = this.root.locator('.file-upload-list');
    this.status = this.root.locator('.file-upload-status');
  }

  dropzone() {
    return this.root.locator('.file-upload-dropzone');
  }

  files() {
    return this.input.evaluate((input: HTMLInputElement) => Array.from(input.files ?? []).map(file => file.name));
  }

  titles() {
    return this.list.locator('.attachment-title').allTextContents();
  }

  rejected() {
    return this.list.locator('.attachment[data-state="error"]');
  }

  remove(name: string) {
    return this.list.getByRole('button', {name: `Remove ${name}`});
  }
}

for (const {skin, theme} of combinations) {
  test.describe(`${skin}, ${theme}`, () => {
    test.beforeEach(async ({page}) => openShowcase(page, skin, theme));

    test('the focus ring of the hidden input shows on the Browse label', async ({page}) => {
      const root = scenario(page, 'card');
      const input = root.locator('.file-upload-input');
      const label = root.locator('.file-upload-browse');
      const shadow = () => label.evaluate(element => getComputedStyle(element).boxShadow);
      const rest = await shadow();

      await page.keyboard.press('Tab');
      await input.focus();
      expect(await input.evaluate(element => element.matches(':focus-visible'))).toBe(true);
      expect(await shadow()).not.toBe(rest);
      expect(await shadow()).not.toBe('none');
    });

    test('an error colours the drop zone, and a disabled one fades its label', async ({page}) => {
      const border = (id: string) => scenario(page, id).locator('.file-upload-dropzone')
          .evaluate(element => getComputedStyle(element).borderTopColor);
      expect(await border('invalid')).not.toBe(await border('card'));
      expect(await scenario(page, 'disabled').locator('.file-upload-browse')
          .evaluate(element => Number(getComputedStyle(element).opacity))).toBeLessThan(1);
    });
  });
}

test.describe('behaviour', () => {
  test('picked files show as attachments, are announced, and picking again adds more', async ({page}) => {
    const messages = await openFixture(page, preview('default'));
    const upload = new Upload(page, 'uploadAttachments');
    await expect(upload.list).toBeHidden();

    await upload.input.setInputFiles(picked(pdf('report.pdf'), png('photo.png')));
    await expect(upload.list).toBeVisible();
    expect(await upload.titles()).toEqual(['report.pdf', 'photo.png']);
    await expect(upload.list.locator('.attachment-description').first()).toHaveText(/^PDF · 1\.2 kB$/i);
    await expect(upload.list.locator('.attachment').first()).toHaveAttribute('data-state', 'idle');
    await expect(upload.remove('report.pdf')).toBeVisible();
    await expect(upload.status).toHaveText('Added report.pdf and photo.png.');

    // The picker replaces the input's files; the earlier ones are kept.
    await upload.input.setInputFiles(picked(pdf('notes.pdf')));
    expect(await upload.files()).toEqual(['report.pdf', 'photo.png', 'notes.pdf']);
    expect(await upload.titles()).toEqual(['report.pdf', 'photo.png', 'notes.pdf']);
    await expect(upload.status).toHaveText('Added notes.pdf.');

    // A file that is there already (same name, size and date) is not added twice; one with another date is.
    const plan = {...pdf('plan.pdf'), lastModified: 1_700_000_000_000};
    await drop(page, upload.dropzone(), [plan]);
    await drop(page, upload.dropzone(), [plan, {...plan, lastModified: 1_800_000_000_000}]);
    expect(await upload.files()).toEqual(['report.pdf', 'photo.png', 'notes.pdf', 'plan.pdf', 'plan.pdf']);
    expect(messages).toEqual([]);
  });

  test('dropped files are added through the input, and the drop zone shows the drag', async ({page}) => {
    const messages = await openFixture(page, preview('default'));
    const upload = new Upload(page, 'uploadAttachments');
    await upload.input.evaluate(input => {
      (window as unknown as {changes: number}).changes = 0;
      input.addEventListener('change', () => (window as unknown as {changes: number}).changes++);
    });

    await drop(page, upload.dropzone(), [pdf('a.pdf')], {leave: true});
    await expect(upload.root).not.toHaveAttribute('data-dragging');

    const dataTransfer = await page.evaluateHandle(() => new DataTransfer());
    await upload.dropzone().dispatchEvent('dragenter', {dataTransfer});
    // Without files (text being dragged), nothing happens.
    await expect(upload.root).not.toHaveAttribute('data-dragging');

    await drop(page, upload.dropzone(), [pdf('a.pdf')], {leave: true});
    await drop(page, upload.root.locator('.empty-title'), [pdf('a.pdf'), png('b.png')]);
    await expect(upload.root).not.toHaveAttribute('data-dragging');
    expect(await upload.files()).toEqual(['a.pdf', 'b.png']);
    expect(await upload.titles()).toEqual(['a.pdf', 'b.png']);
    await expect(upload.status).toHaveText('Added a.pdf and b.png.');
    // A drop dispatches change, as a pick does (hx-trigger="change").
    expect(await page.evaluate(() => (window as unknown as {changes: number}).changes)).toBe(1);

    // Dropped on the list, files are added too, and the browser does not open them.
    await drop(page, upload.list, [pdf('c.pdf')]);
    expect(await upload.files()).toEqual(['a.pdf', 'b.png', 'c.pdf']);
    expect(page.url()).toBe('http://app.test/');
    expect(messages).toEqual([]);
  });

  test('a file of another type or too large shows as an error until it is dismissed, and is not submitted',
      async ({page}) => {
        await openFixture(page, preview('default'));
        const upload = new Upload(page, 'uploadAttachments');

        await drop(page, upload.dropzone(), [pdf('ok.pdf'), {name: 'setup.zip', type: 'application/zip'},
          {name: 'huge.pdf', type: 'application/pdf', size: 11 * 1024 * 1024}]);
        expect(await upload.files()).toEqual(['ok.pdf']);
        expect(await upload.titles()).toEqual(['ok.pdf', 'setup.zip', 'huge.pdf']);
        await expect(upload.rejected()).toHaveCount(2);
        await expect(upload.rejected().first().locator('.attachment-description')).toHaveText('File type not accepted');
        await expect(upload.rejected().nth(1).locator('.attachment-description')).toHaveText(/^Larger than 10 MB$/i);
        await expect(upload.status).toHaveText(/^Added ok\.pdf\. Could not add setup\.zip: File type not accepted\. Could not add huge\.pdf: Larger than 10 MB\.$/i);

        // A picked file is checked too: the picker lets one choose any file.
        await upload.input.setInputFiles(picked({name: 'script.sh', type: 'text/x-sh'}));
        expect(await upload.files()).toEqual(['ok.pdf']);
        // The rejections of the earlier drop are gone; the new one shows.
        await expect(upload.rejected()).toHaveCount(1);
        await expect(upload.rejected().locator('.attachment-title')).toHaveText('script.sh');

        await upload.list.getByRole('button', {name: 'Dismiss script.sh'}).click();
        await expect(upload.rejected()).toHaveCount(0);
        expect(await upload.files()).toEqual(['ok.pdf']);
        await expect(upload.remove('ok.pdf')).toBeFocused();
      });

  test('the keyboard: one tab stop that opens the picker, and removing a file moves the focus on', async ({page}) => {
    await openFixture(page, `<button type="button" id="before">Before</button>${preview('default')}`
        + '<button type="button" id="after">After</button>');
    const upload = new Upload(page, 'uploadAttachments');

    await page.locator('#before').focus();
    await page.keyboard.press('Tab');
    await expect(upload.input).toBeFocused();
    const chooser = page.waitForEvent('filechooser');
    await page.keyboard.press('Space');
    const fileChooser = await chooser;
    expect(fileChooser.isMultiple()).toBe(true);
    await fileChooser.setFiles(picked(pdf('a.pdf'), pdf('b.pdf'), pdf('c.pdf')));
    expect(await upload.titles()).toEqual(['a.pdf', 'b.pdf', 'c.pdf']);

    await page.keyboard.press('Tab');
    await expect(upload.remove('a.pdf')).toBeFocused();
    await page.keyboard.press('Tab');
    await expect(upload.remove('b.pdf')).toBeFocused();

    await page.keyboard.press('Enter');
    expect(await upload.files()).toEqual(['a.pdf', 'c.pdf']);
    await expect(upload.status).toHaveText('Removed b.pdf.');
    await expect(upload.remove('c.pdf')).toBeFocused();
    await page.keyboard.press('Enter');
    await expect(upload.remove('a.pdf')).toBeFocused();
    await page.keyboard.press('Space');
    expect(await upload.files()).toEqual([]);
    await expect(upload.input).toBeFocused();
    await expect(upload.list).toBeHidden();
    await page.keyboard.press('Tab');
    await expect(page.locator('#after')).toBeFocused();
  });

  test('a click on the Browse label opens the picker', async ({page}) => {
    await openFixture(page, preview('single'));
    const chooser = page.waitForEvent('filechooser');
    await page.locator('.file-upload-browse').click();
    expect((await chooser).isMultiple()).toBe(false);
  });

  test('without multiple, a new file replaces the old one, and only one of a drop is taken', async ({page}) => {
    await openFixture(page, preview('single'));
    const upload = new Upload(page, 'uploadAvatar');

    await upload.input.setInputFiles(picked(png('me.png')));
    await drop(page, upload.dropzone(), [png('new.png')]);
    expect(await upload.files()).toEqual(['new.png']);
    expect(await upload.titles()).toEqual(['new.png']);
    await expect(upload.status).toHaveText('Added new.png. Removed me.png.');

    await drop(page, upload.dropzone(), [png('one.png'), png('two.png')]);
    expect(await upload.files()).toEqual(['one.png']);
    await expect(upload.rejected().locator('.attachment-title')).toHaveText('two.png');
    await expect(upload.rejected().locator('.attachment-description')).toHaveText('Only one file can be added');
  });

  test('a disabled upload takes no dropped files', async ({page}) => {
    await openFixture(page, preview('disabled'));
    const upload = new Upload(page, 'uploadArchive');
    await drop(page, upload.dropzone(), [pdf('a.pdf')]);
    expect(await upload.files()).toEqual([]);
    await expect(upload.list).toBeHidden();
    expect(page.url()).toBe('http://app.test/');
  });

  test('the form submits the files in the list', async ({page}) => {
    const bodies: string[] = [];
    await openFixture(page, `<form method="post" action="/upload" enctype="multipart/form-data">${preview('default')}`
        + '<button type="submit">Save</button></form>', {
      routes: {'/upload': ({body}) => {
        bodies.push(body);
        return {body: '<p id="saved">Saved</p>'};
      }},
    });
    const upload = new Upload(page, 'uploadAttachments');
    await upload.input.setInputFiles(picked(pdf('keep.pdf'), pdf('drop.pdf')));
    await drop(page, upload.dropzone(), [png('dropped.png')]);
    await upload.remove('drop.pdf').click();

    await page.getByRole('button', {name: 'Save'}).click();
    await expect(page.locator('#saved')).toBeVisible();
    expect(bodies[0]).toContain('filename="keep.pdf"');
    expect(bodies[0]).toContain('filename="dropped.png"');
    expect(bodies[0]).not.toContain('filename="drop.pdf"');
  });

  test('under a strict CSP with the csp build, nothing is blocked', async ({page}) => {
    const messages = await openFixture(page, preview('default') + preview('single'));
    await new Upload(page, 'uploadAttachments').input.setInputFiles(picked(pdf('a.pdf')));
    await drop(page, new Upload(page, 'uploadAvatar').dropzone(), [{name: 'a.txt', type: 'text/plain'}]);
    await expect(page.locator('.attachment')).toHaveCount(2);
    expect(messages).toEqual([]);
  });

  test('without Alpine the input still picks files and submits them; there is no list', async ({page}) => {
    const bodies: string[] = [];
    await openFixture(page, `<form method="post" action="/upload" enctype="multipart/form-data">${preview('default')}`
        + '<button type="submit">Save</button></form>', {
      alpine: false,
      routes: {'/upload': ({body}) => {
        bodies.push(body);
        return {body: '<p id="saved">Saved</p>'};
      }},
    });
    const upload = new Upload(page, 'uploadAttachments');
    await expect(page.getByLabel('Browse files')).toHaveCount(1);
    const chooser = page.waitForEvent('filechooser');
    await page.locator('.file-upload-browse').click();
    await (await chooser).setFiles(picked(pdf('a.pdf')));
    await expect(upload.list).toBeHidden();
    await expect(upload.list.locator('li')).toHaveCount(0);

    await page.getByRole('button', {name: 'Save'}).click();
    await expect(page.locator('#saved')).toBeVisible();
    expect(bodies[0]).toContain('filename="a.pdf"');
  });

  test('htmx: swapped in it works, swapped out it leaves nothing behind, and back rebuilds the list from the input',
      async ({page}) => {
        const messages = await openFixture(page, '<div id="area"></div>'
            + '<button type="button" id="load" hx-get="/upload" hx-target="#area">Load</button>'
            + '<button type="button" id="clear" hx-get="/empty" hx-target="#area">Clear</button>'
            + '<a id="next" href="/next" hx-get="/next" hx-target="main" hx-push-url="true">Next</a>', {
          htmx: true,
          routes: {
            '/upload': () => ({body: preview('default')}),
            '/empty': () => ({body: '<p id="empty">Nothing</p>'}),
            '/next': () => ({body: '<p id="next-page">Next page</p>'}),
          },
        });
        const upload = new Upload(page, 'uploadAttachments');

        await page.locator('#load').click();
        await upload.input.setInputFiles(picked(pdf('a.pdf')));
        await drop(page, upload.dropzone(), [pdf('b.pdf')]);
        expect(await upload.titles()).toEqual(['a.pdf', 'b.pdf']);

        await page.locator('#clear').click();
        await expect(page.locator('#empty')).toBeVisible();
        await page.locator('#load').click();
        await expect(upload.list).toBeHidden();
        await upload.input.setInputFiles(picked(pdf('c.pdf')));
        expect(await upload.titles()).toEqual(['c.pdf']);

        await page.locator('#next').click();
        await expect(page.locator('#next-page')).toBeVisible();
        await page.goBack();
        await expect(upload.input).toBeAttached();
        // htmx restores the markup, not the files: the list follows the input.
        await expect.poll(() => upload.files()).toEqual([]);
        await expect(upload.list).toBeHidden();
        await expect(upload.list.locator('li')).toHaveCount(0);
        await upload.input.setInputFiles(picked(pdf('d.pdf')));
        expect(await upload.titles()).toEqual(['d.pdf']);
        await expect(upload.status).toHaveText('Added d.pdf.');
        expect(messages).toEqual([]);
      });
});

// Upload progress, with htmx 2 and htmx 4. The browser's own upload events would come all at once (the fixture answers
// from memory), so the init script keeps the listeners added to xhr.upload, and the test fires progress and load on
// them by hand. The answer waits until the test lets it go.
type Uploads = { fire(type: string, loaded?: number, total?: number): void, count(): number };

async function holdUploads(page: Page) {
  await page.addInitScript(() => {
    const uploads: { target: EventTarget, listeners: Record<string, EventListener[]> }[] = [];
    XMLHttpRequestUpload.prototype.addEventListener = function (type: string, listener: EventListener) {
      let entry = uploads.find(upload => upload.target === this);
      if (!entry) {
        entry = {target: this, listeners: {}};
        uploads.push(entry);
      }
      (entry.listeners[type] ??= []).push(listener);
    };
    (window as unknown as { uploads: Uploads }).uploads = {
      fire(type, loaded = 0, total = 0) {
        const event = new ProgressEvent(type, {lengthComputable: total > 0, loaded, total});
        uploads.at(-1)?.listeners[type]?.forEach(listener => listener(event));
      },
      count: () => uploads.length,
    };
  });
}

const fire = (page: Page, type: string, loaded?: number, total?: number) =>
    page.evaluate(([type, loaded, total]) => (window as unknown as { uploads: Uploads }).uploads
        .fire(type as string, loaded as number, total as number), [type, loaded, total]);

/** Holds the answers to POST /upload until release() is called; each request's body is kept. */
async function holdAnswers(page: Page, answer: () => { status?: number, body: string } | 'fail'
    = () => ({body: '<p id="saved">Saved</p>'})) {
  const bodies: string[] = [];
  let release = () => {};
  let held = new Promise<void>(resolve => (release = resolve));
  await page.route('**/upload', async route => {
    bodies.push(route.request().postData() ?? '');
    await held;
    held = new Promise<void>(resolve => (release = resolve));
    const response = answer();
    if (response === 'fail') {
      return route.abort('failed');
    }
    return route.fulfill({status: response.status ?? 200, contentType: 'text/html', body: response.body});
  });
  return {bodies, release: () => release()};
}

/** A form that posts with htmx: its answer goes into #area (around the form) or #result (beside it). */
const form = (target: 'area' | 'result', swap = 'innerHTML') => '<div id="area"><form id="form" hx-post="/upload" '
    + `hx-target="#${target}" hx-swap="${swap}" enctype="multipart/form-data">${preview('default')}`
    + '<button type="submit">Save</button></form></div><div id="result"></div>';

const a = pdf('a.pdf');
const b = {...pdf('b.pdf'), size: 3000};
const states = (upload: Upload) => upload.list.locator('.attachment').evaluateAll(elements => elements
    .map(element => [element.getAttribute('data-state'), element.getAttribute('aria-busy'),
      element.querySelector('.attachment-description')!.textContent]));

for (const version of [2, 4] as const) {
  test.describe(`upload progress, htmx ${version}`, () => {
    test('each file shows its share sent, then processing, and the answer replaces the component', async ({page}) => {
      await holdUploads(page);
      const messages = await openFixture(page, form('area'), {htmx: version});
      const answers = await holdAnswers(page);
      const upload = new Upload(page, 'uploadAttachments');
      await upload.input.setInputFiles(picked(a, b));
      const cancel = upload.root.getByRole('button', {name: 'Cancel upload'});
      await expect(cancel).toBeHidden();

      await page.getByRole('button', {name: 'Save'}).click();
      await expect(upload.root).toHaveAttribute('data-uploading', 'true');
      await expect(upload.status).toHaveText('Uploading 2 files.');
      await expect(upload.input).toBeDisabled();
      await expect(cancel).toBeVisible();
      await expect(upload.root.locator('.file-upload-browse')).toBeHidden();
      await expect(upload.list.getByRole('button')).toHaveCount(0);
      expect(await states(upload)).toEqual([['uploading', 'true', 'Uploading'], ['uploading', 'true', 'Uploading']]);

      // 4400 bytes in all, 4200 of them the files' (1200 and 3000): 2200 sent are 2100 of the files' bytes, all of a
      // and 900 of b's.
      await fire(page, 'progress', 2200, 4400);
      expect(await states(upload)).toEqual([['uploading', 'true', 'Uploading · 100%'],
        ['uploading', 'true', 'Uploading · 30%']]);
      await fire(page, 'progress', 4400, 4400);
      await fire(page, 'load', 4400, 4400);
      expect(await states(upload)).toEqual([['processing', 'true', 'Processing'], ['processing', 'true', 'Processing']]);
      // No announcement per percent.
      await expect(upload.status).toHaveText('Uploading 2 files.');

      answers.release();
      await expect(page.locator('#saved')).toBeVisible();
      await expect(upload.root).toHaveCount(0);
      expect(answers.bodies[0]).toContain('filename="a.pdf"');
      expect(answers.bodies[0]).toContain('filename="b.pdf"');
      expect(messages).toEqual([]);
    });

    test('a size the browser does not know shows no percentage', async ({page}) => {
      await holdUploads(page);
      await openFixture(page, form('area'), {htmx: version});
      const answers = await holdAnswers(page);
      const upload = new Upload(page, 'uploadAttachments');
      await upload.input.setInputFiles(picked(a));
      await page.getByRole('button', {name: 'Save'}).click();
      await expect(upload.status).toHaveText('Uploading a.pdf.');
      await fire(page, 'progress', 500, 0);
      expect(await states(upload)).toEqual([['uploading', 'true', 'Uploading']]);
      answers.release();
      await expect(page.locator('#saved')).toBeVisible();
    });

    test('when the component stays, the files show as uploaded and leave the input', async ({page}) => {
      await holdUploads(page);
      const messages = await openFixture(page, form('result'), {htmx: version});
      const answers = await holdAnswers(page);
      const upload = new Upload(page, 'uploadAttachments');
      await upload.input.setInputFiles(picked(a, b));
      await page.getByRole('button', {name: 'Save'}).click();
      await fire(page, 'load', 4400, 4400);
      answers.release();

      await expect(page.locator('#saved')).toBeVisible();
      await expect(upload.status).toHaveText('Uploaded 2 files.');
      expect(await states(upload)).toEqual([[null, null, 'Uploaded'], [null, null, 'Uploaded']]);
      expect(await upload.files()).toEqual([]);
      await expect(upload.input).toBeEnabled();
      await expect(upload.root).not.toHaveAttribute('data-uploading');
      await expect(upload.list.getByRole('button')).toHaveCount(0);

      // The next pick clears them.
      await upload.input.setInputFiles(picked(pdf('c.pdf')));
      expect(await upload.titles()).toEqual(['c.pdf']);
      expect(await states(upload)).toEqual([['idle', null, 'PDF · 1.2 kB']]);
      expect(messages).toEqual([]);
    });

    test('a failed answer marks the files as errors and keeps them, to be sent again', async ({page}) => {
      await holdUploads(page);
      let status = 500;
      const messages = await openFixture(page, form('result'), {htmx: version});
      const answers = await holdAnswers(page, () => ({status, body: status === 500 ? '' : '<p id="saved">Saved</p>'}));
      const upload = new Upload(page, 'uploadAttachments');
      await upload.input.setInputFiles(picked(a, b));
      await page.getByRole('button', {name: 'Save'}).click();
      await fire(page, 'load', 4400, 4400);
      answers.release();

      await expect(upload.status).toHaveText('The files could not be uploaded. Try again.');
      expect(await states(upload)).toEqual([['error', null, 'Upload failed'], ['error', null, 'Upload failed']]);
      expect(await upload.files()).toEqual(['a.pdf', 'b.pdf']);
      await expect(upload.input).toBeEnabled();
      await expect(upload.remove('b.pdf')).toBeVisible();

      status = 200;
      await page.getByRole('button', {name: 'Save'}).click();
      await expect(upload.status).toHaveText('Uploading 2 files.');
      answers.release();
      await expect(page.locator('#saved')).toBeVisible();
      expect(answers.bodies[1]).toContain('filename="b.pdf"');
      // htmx reports the error status itself.
      expect(messages.filter(message => !/500|Internal Server Error|htmx:response:error/i.test(message)))
          .toEqual([]);
    });

    test('no connection marks the files as errors', async ({page}) => {
      await holdUploads(page);
      await openFixture(page, form('result'), {htmx: version});
      const answers = await holdAnswers(page, () => 'fail');
      const upload = new Upload(page, 'uploadAttachments');
      await upload.input.setInputFiles(picked(a));
      await page.getByRole('button', {name: 'Save'}).click();
      answers.release();
      await expect(upload.status).toHaveText('The files could not be uploaded. Try again.');
      expect(await states(upload)).toEqual([['error', null, 'Upload failed']]);
      expect(await upload.files()).toEqual(['a.pdf']);
    });

    test('Cancel aborts the request, and the files are back as they were', async ({page}) => {
      await holdUploads(page);
      const messages = await openFixture(page, form('area'), {htmx: version});
      const answers = await holdAnswers(page);
      const upload = new Upload(page, 'uploadAttachments');
      await upload.input.setInputFiles(picked(a, b));
      await page.getByRole('button', {name: 'Save'}).click();
      await fire(page, 'progress', 1000, 4400);

      await upload.root.getByRole('button', {name: 'Cancel upload'}).click();
      await expect(upload.status).toHaveText('Upload cancelled.');
      await expect(upload.input).toBeFocused();
      expect(await states(upload)).toEqual([['idle', null, 'PDF · 1.2 kB'], ['idle', null, 'PDF · 2.9 kB']]);
      expect(await upload.files()).toEqual(['a.pdf', 'b.pdf']);
      await expect(upload.root.locator('.file-upload-browse')).toBeVisible();
      await expect(upload.remove('a.pdf')).toBeVisible();
      answers.release();
      await expect(page.locator('#saved')).toHaveCount(0);
      // htmx logs an aborted request as an error, of its own: htmx 2 its afterRequest and sendAbort, htmx 4 its
      // htmx:error (the fetch rejected with an AbortError).
      expect(messages.length).toBeGreaterThan(0);
      expect(messages.filter(message => !/^htmx(:afterRequest|:sendAbort|: htmx:error: .*abort)/is.test(message)))
          .toEqual([]);
    });

    test('a request that does not carry the files is left alone', async ({page}) => {
      await holdUploads(page);
      const messages = await openFixture(page, form('result')
          + '<button type="button" id="other" hx-post="/upload" hx-target="#result">Other</button>', {htmx: version});
      const answers = await holdAnswers(page);
      const upload = new Upload(page, 'uploadAttachments');
      await upload.input.setInputFiles(picked(a));
      await page.locator('#other').click();
      await expect.poll(() => answers.bodies.length).toBe(1);
      await expect(upload.root).not.toHaveAttribute('data-uploading');
      answers.release();
      await expect(page.locator('#saved')).toBeVisible();
      expect(await states(upload)).toEqual([['idle', null, 'PDF · 1.2 kB']]);
      expect(messages).toEqual([]);
    });
  });
}

test('upload progress: a page saved in htmx\'s history while uploading comes back idle', async ({page}) => {
  await holdUploads(page);
  const messages = await openFixture(page, form('area')
      + '<a id="next" href="/next" hx-get="/next" hx-target="main" hx-push-url="true">Next</a>', {
    htmx: true,
    routes: {'/next': () => ({body: '<p id="next-page">Next page</p>'})},
  });
  await holdAnswers(page);
  const upload = new Upload(page, 'uploadAttachments');
  await upload.input.setInputFiles(picked(a));
  await page.getByRole('button', {name: 'Save'}).click();
  await expect(upload.root).toHaveAttribute('data-uploading', 'true');

  await page.locator('#next').click();
  await expect(page.locator('#next-page')).toBeVisible();
  await page.goBack();
  await expect(upload.input).toBeAttached();
  await expect(upload.root).not.toHaveAttribute('data-uploading');
  await expect(upload.input).toBeEnabled();
  await expect(upload.root.locator('.file-upload-browse')).toBeVisible();
  await expect(upload.list).toBeHidden();
  await upload.input.setInputFiles(picked(pdf('d.pdf')));
  expect(await states(upload)).toEqual([['idle', null, 'PDF · 1.2 kB']]);
  expect(messages).toEqual([]);
});
