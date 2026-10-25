import {expect, test, type Locator, type Page} from '@playwright/test';
import previews from '../src/generated/previews.json' with {type: 'json'};
import {openFixture} from './fixture';
import {combinations, openShowcase, widths} from './showcase';

// sl:file-upload. On the showcase, in every skin and theme: the hidden input's focus ring shows on the Browse label, a
// field error colours the drop zone's border, and a disabled one's Browse label fades. Fixture pages under a strict
// Content-Security-Policy with the csp Alpine build, the markup the docs previews': picking and dropping files (through
// a DataTransfer, as a real drop hands them over), adding more, removing one, accept and max-size, one file without
// multiple, the announcements, the keyboard, the submitted form, htmx (swapped in, out, history restore) and the page
// without Alpine. The upload progress with htmx 2 and htmx 4: each file's share, processing, the outcomes, Cancel, a
// request without the files, and htmx 2's history snapshot taken while uploading (htmx 4 keeps no snapshot: it asks
// the server for the page again). Image previews: the blob: URLs made and revoked, the strict CSP's fallback, the
// states and the history snapshot. The grid: thumbnails, icon and error cards, remove and the focus, the upload's
// states. file-upload-network.spec.ts has the browser's own upload events. Runs in Chromium
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

    test('a grid shows the picked files as cards in columns, at a narrow and a wide width', async ({page}) => {
      const root = scenario(page, 'grid');
      const input = root.locator('.file-upload-input');
      await input.setInputFiles([...Array.from({length: 7}, (_, index) => photo(`defect-${index + 1}.png`)),
        ...picked(pdf('report.pdf'))]);
      await drop(page, root.locator('.file-upload-dropzone'), [{name: 'notes.txt', type: 'text/plain'}]);
      const list = root.locator('.file-upload-list');
      await expect(list.locator('.attachment')).toHaveCount(9);
      await expect(list.locator('.attachment').last()).toHaveAttribute('data-state', 'error');

      const layout = () => list.evaluate(element => {
        const box = (target: Element) => target.getBoundingClientRect();
        const cards = [...element.querySelectorAll(':scope > .file-upload-item > .attachment')];
        const boxes = cards.map(box);
        const outer = box(element);
        return {
          columns: new Set(boxes.map(card => Math.round(card.left))).size,
          rows: new Set(boxes.map(card => Math.round(card.top))).size,
          scrolls: element.scrollWidth > element.clientWidth,
          inside: boxes.every(card => card.left >= outer.left - 0.5 && card.right <= outer.right + 0.5),
          widths: new Set(boxes.map(card => Math.round(card.width))).size,
          // Every card's media is a square that fills the card's width (inside its padding), its button in the
          // media's top corner.
          square: cards.every(card => {
            const media = box(card.querySelector(':scope > .attachment-media')!);
            const action = box(card.querySelector(':scope > .attachment-actions')!);
            const style = getComputedStyle(card);
            const inner = card.clientWidth - parseFloat(style.paddingInlineStart) - parseFloat(style.paddingInlineEnd);
            return Math.abs(media.width - media.height) < 1 && Math.abs(media.width - inner) < 1
                && action.top >= media.top && action.bottom <= media.top + media.height / 2
                && action.right <= media.right && action.left >= media.left + media.width / 2;
          }),
        };
      });

      const columns: Record<string, number> = {};
      for (const {name, viewport} of widths) {
        await page.setViewportSize(viewport);
        await list.scrollIntoViewIfNeeded();
        const result = await layout();
        expect(result.columns, `columns at ${name} width`).toBeGreaterThan(1);
        expect(result.rows, `rows at ${name} width`).toBeGreaterThan(1);
        expect(result.scrolls).toBe(false);
        expect(result.inside).toBe(true);
        expect(result.widths).toBe(1);
        expect(result.square).toBe(true);
        columns[name] = result.columns;
      }
      expect(columns.desktop).toBeGreaterThan(columns.phone);
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

// Image previews: a picked image's blob: URL, made and revoked through URL.createObjectURL and revokeObjectURL, which
// the init script records. A real PNG (1×1), as a picked file and as a dropped one; the fixture's strict CSP blocks
// blob: images unless imgSrc allows them.
const PNG = 'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNkYAAAAAYAAjCB0C8AAAAASUVORK5CYII=';
type Blobs = { created: string[], revoked: string[] };

async function recordBlobs(page: Page) {
  await page.addInitScript(() => {
    const blobs: Blobs = {created: [], revoked: []};
    (window as unknown as { blobs: Blobs }).blobs = blobs;
    const create = URL.createObjectURL;
    const revoke = URL.revokeObjectURL;
    URL.createObjectURL = object => {
      const url = create(object);
      blobs.created.push(url);
      return url;
    };
    URL.revokeObjectURL = url => {
      blobs.revoked.push(url);
      revoke(url);
    };
  });
}

const blobs = (page: Page) => page.evaluate(() => (window as unknown as { blobs: Blobs }).blobs);

const photo = (name: string) => ({name, mimeType: 'image/png', buffer: Buffer.from(PNG, 'base64')});

/** Drops files with real bytes (base64), as a drop hands them over. */
async function dropBytes(page: Page, target: Locator, files: { name: string, type: string, base64: string }[]) {
  const dataTransfer = await page.evaluateHandle(files => {
    const transfer = new DataTransfer();
    for (const file of files) {
      transfer.items.add(new File([Uint8Array.from(atob(file.base64), c => c.charCodeAt(0))], file.name,
          {type: file.type}));
    }
    return transfer;
  }, files);
  for (const type of ['dragenter', 'dragover', 'drop']) {
    await target.dispatchEvent(type, {dataTransfer});
  }
}

/** Each attachment's media: the img's src for a thumbnail, 'icon' for an icon. */
const media = (upload: Upload) => upload.list.locator('.attachment-media').evaluateAll(elements => elements
    .map(element => element.getAttribute('data-variant') === 'image'
      ? element.querySelector('img')!.getAttribute('src') : 'icon'));

const ALLOW_BLOB = "'self' blob:";

test.describe('image previews', () => {
  test('a picked or dropped image shows a thumbnail from a blob: URL; other files keep their icons', async ({page}) => {
    await recordBlobs(page);
    const messages = await openFixture(page, preview('previews'), {imgSrc: ALLOW_BLOB});
    const upload = new Upload(page, 'uploadPhotos');

    await upload.input.setInputFiles([photo('photo.png'), ...picked(pdf('report.pdf'))]);
    await expect.poll(() => media(upload)).toEqual([expect.stringMatching(/^blob:/), 'icon']);
    const [url] = (await blobs(page)).created;
    expect(await media(upload)).toEqual([url, 'icon']);
    const img = upload.list.locator('.attachment-media img');
    await expect(img).toHaveAttribute('alt', '');
    expect(await img.evaluate((image: HTMLImageElement) => image.complete && image.naturalWidth)).toBe(1);

    // Dropped: a PNG gets one too; bytes that are no image keep the icon, and their URL is revoked at once; an SVG
    // gets no URL at all.
    const svg = Buffer.from('<svg xmlns="http://www.w3.org/2000/svg"><script>alert(1)</script></svg>').toString('base64');
    await dropBytes(page, upload.dropzone(), [{name: 'second.png', type: 'image/png', base64: PNG},
      {name: 'broken.png', type: 'image/png', base64: Buffer.alloc(40, 7).toString('base64')},
      {name: 'drawing.svg', type: 'image/svg+xml', base64: svg}]);
    expect(await upload.titles()).toEqual(['photo.png', 'report.pdf', 'second.png', 'broken.png', 'drawing.svg']);
    await expect.poll(async () => (await blobs(page)).revoked.length).toBe(1);
    const {created, revoked} = await blobs(page);
    expect(created).toHaveLength(3);
    expect(revoked).toEqual([created[2]]);
    await expect.poll(() => media(upload)).toEqual([url, 'icon', created[1], 'icon', 'icon']);
    // A change that keeps them makes no new URL, and the thumbnail is there at once.
    await upload.remove('report.pdf').click();
    expect(await media(upload)).toEqual([url, created[1], 'icon', 'icon']);
    expect((await blobs(page)).created).toHaveLength(3);
    expect(messages).toEqual([]);
  });

  test('an image larger than 20 MB keeps its icon', async ({page}) => {
    await recordBlobs(page);
    await openFixture(page, preview('previews').replace(/ data-max-size="\d+"/, ''), {imgSrc: ALLOW_BLOB});
    const upload = new Upload(page, 'uploadPhotos');
    const dataTransfer = await page.evaluateHandle(() => {
      const transfer = new DataTransfer();
      transfer.items.add(new File([new Uint8Array(20 * 1024 * 1024 + 1)], 'huge.png', {type: 'image/png'}));
      transfer.items.add(new File([new Uint8Array(20 * 1024 * 1024)], 'large.png', {type: 'image/png'}));
      return transfer;
    });
    for (const type of ['dragenter', 'dragover', 'drop']) {
      await upload.dropzone().dispatchEvent(type, {dataTransfer});
    }
    expect(await upload.titles()).toEqual(['huge.png', 'large.png']);
    // Only the one of 20 MB is tried (its bytes are no image: the icon stays).
    expect((await blobs(page)).created).toHaveLength(1);
    expect(await media(upload)).toEqual(['icon', 'icon']);
  });

  test('each URL is revoked when its file is removed, replaced, or the component is swapped out', async ({page}) => {
    await recordBlobs(page);
    const single = preview('previews').replace(' multiple=""', '').replace(' multiple', '');
    const messages = await openFixture(page, '<div id="area"></div>'
        + '<button type="button" id="load" hx-get="/upload" hx-target="#area">Load</button>'
        + '<button type="button" id="single" hx-get="/single" hx-target="#area">Single</button>'
        + '<button type="button" id="clear" hx-get="/empty" hx-target="#area">Clear</button>', {
      htmx: true,
      imgSrc: ALLOW_BLOB,
      routes: {
        '/upload': () => ({body: preview('previews')}),
        '/single': () => ({body: single}),
        '/empty': () => ({body: '<p id="empty">Nothing</p>'}),
      },
    });
    const upload = new Upload(page, 'uploadPhotos');

    await page.locator('#load').click();
    await upload.input.setInputFiles([photo('a.png'), photo('b.png'), photo('c.png')]);
    await expect.poll(() => media(upload)).toEqual(Array(3).fill(expect.stringMatching(/^blob:/)));
    const [a, b, c] = (await blobs(page)).created;
    await upload.remove('b.png').click();
    expect((await blobs(page)).revoked).toEqual([b]);
    expect(await media(upload)).toEqual([a, c]);

    // Swapped out: the rest go.
    await page.locator('#clear').click();
    await expect(page.locator('#empty')).toBeVisible();
    await expect.poll(async () => (await blobs(page)).revoked.toSorted()).toEqual([a, b, c].toSorted());

    // Without multiple, a new image replaces the old one, and so does its URL.
    await page.locator('#single').click();
    await upload.input.setInputFiles([photo('d.png')]);
    await expect.poll(() => media(upload)).toEqual([expect.stringMatching(/^blob:/)]);
    await upload.input.setInputFiles([photo('e.png')]);
    await expect.poll(() => media(upload)).toEqual([expect.stringMatching(/^blob:/)]);
    const {created, revoked} = await blobs(page);
    expect(created).toHaveLength(5);
    expect(revoked).toContain(created[3]);
    expect(await media(upload)).toEqual([created[4]]);
    expect(messages).toEqual([]);
  });

  test('under the strict CSP the icon stays, and after the first block no more URLs are made', async ({page}) => {
    await recordBlobs(page);
    const messages = await openFixture(page, preview('previews'));
    const upload = new Upload(page, 'uploadPhotos');

    await upload.input.setInputFiles([photo('photo.png')]);
    await expect.poll(() => messages.some(message => message === 'CSP violation: img-src')).toBe(true);
    await expect.poll(async () => (await blobs(page)).revoked).toEqual((await blobs(page)).created);
    expect(await media(upload)).toEqual(['icon']);
    await expect(upload.list.locator('img')).toHaveCount(0);

    await dropBytes(page, upload.dropzone(), [{name: 'second.png', type: 'image/png', base64: PNG}]);
    expect(await upload.titles()).toEqual(['photo.png', 'second.png']);
    expect((await blobs(page)).created).toHaveLength(1);
    expect(await media(upload)).toEqual(['icon', 'icon']);
    // The browser's report of the one blocked image, and nothing of ours.
    expect(messages.filter(message => !/^CSP violation: img-src$|Refused to load the image|Content[- ]Security[- ]Policy/i
        .test(message))).toEqual([]);
    expect(messages.filter(message => message === 'CSP violation: img-src')).toHaveLength(1);
  });

  const previewForm = (target: 'area' | 'result') => '<div id="area"><form id="form" hx-post="/upload" '
      + `hx-target="#${target}" enctype="multipart/form-data">${preview('previews')}`
      + '<button type="submit">Save</button></form></div><div id="result"></div>';

  for (const version of [2, 4] as const) {
    test(`htmx ${version}: the thumbnail stays, dimmed, through the upload's states`, async ({page}) => {
      await recordBlobs(page);
      await holdUploads(page);
      let status = 500;
      const messages = await openFixture(page, previewForm('result'), {htmx: version, imgSrc: ALLOW_BLOB});
      const answers = await holdAnswers(page, () => ({status, body: status === 500 ? '' : '<p id="saved">Saved</p>'}));
      const upload = new Upload(page, 'uploadPhotos');
      await upload.input.setInputFiles([photo('photo.png')]);
      await expect.poll(() => media(upload)).toEqual([expect.stringMatching(/^blob:/)]);
      const [url] = (await blobs(page)).created;
      const opacity = () => upload.list.locator('.attachment-media')
          .evaluate(element => Number(getComputedStyle(element).opacity));
      expect(await opacity()).toBe(1);

      await page.getByRole('button', {name: 'Save'}).click();
      await expect(upload.root).toHaveAttribute('data-uploading', 'true');
      await fire(page, 'progress', 100, 400);
      expect(await states(upload)).toEqual([['uploading', 'true', expect.stringMatching(/^Uploading · \d+%$/)]]);
      expect(await media(upload)).toEqual([url]);
      expect(await opacity()).toBeLessThan(1);
      await fire(page, 'load', 400, 400);
      expect(await states(upload)).toEqual([['processing', 'true', 'Processing']]);
      expect(await media(upload)).toEqual([url]);

      answers.release();
      await expect(upload.status).toHaveText('The files could not be uploaded. Try again.');
      expect(await states(upload)).toEqual([['error', null, 'Upload failed']]);
      expect(await media(upload)).toEqual([url]);
      expect(await opacity()).toBeLessThan(1);

      status = 200;
      await page.getByRole('button', {name: 'Save'}).click();
      await expect(upload.root).toHaveAttribute('data-uploading', 'true');
      await fire(page, 'load', 400, 400);
      answers.release();
      await expect(page.locator('#saved')).toBeVisible();
      expect(await states(upload)).toEqual([[null, null, 'Uploaded']]);
      expect(await media(upload)).toEqual([url]);
      expect(await opacity()).toBe(1);
      expect((await blobs(page)).revoked).toEqual([]);

      // The next change takes the uploaded files away, and their URLs.
      await upload.input.setInputFiles(picked(pdf('c.pdf')));
      expect((await blobs(page)).revoked).toEqual([url]);
      expect(await media(upload)).toEqual(['icon']);
      expect(messages.filter(message => !/500|Internal Server Error|htmx:response:error/i.test(message)))
          .toEqual([]);
    });
  }

  test('htmx 2: the history snapshot holds no blob: URL, and back shows no dead image', async ({page}) => {
    await recordBlobs(page);
    const messages = await openFixture(page, '<div id="area">' + preview('previews') + '</div>'
        + '<a id="other" href="/other" hx-get="/other" hx-target="#result" hx-push-url="true">Other</a>'
        + '<div id="result"></div>'
        + '<a id="next" href="/next" hx-get="/next" hx-target="#area" hx-push-url="true">Next</a>', {
      htmx: true,
      imgSrc: ALLOW_BLOB,
      routes: {
        '/other': () => ({body: '<p id="other-page">Other</p>'}),
        '/next': () => ({body: '<p id="next-page">Next page</p>'}),
      },
    });
    const upload = new Upload(page, 'uploadPhotos');
    const history = () => page.evaluate(() => sessionStorage.getItem('htmx-history-cache') ?? '');

    await upload.input.setInputFiles([photo('photo.png')]);
    await expect.poll(() => media(upload)).toEqual([expect.stringMatching(/^blob:/)]);
    const [url] = (await blobs(page)).created;

    // A push that leaves the component there: the snapshot has the icon, the page gets the thumbnail back.
    await page.locator('#other').click();
    await expect(page.locator('#other-page')).toBeVisible();
    expect(await history()).toContain('photo.png');
    expect(await history()).not.toContain('blob:');
    await expect.poll(() => media(upload)).toEqual([url]);

    await page.locator('#next').click();
    await expect(page.locator('#next-page')).toBeVisible();
    expect(await history()).not.toContain('blob:');
    await expect.poll(async () => (await blobs(page)).revoked).toEqual([url]);
    await page.goBack();
    await expect(upload.input).toBeAttached();
    await expect(upload.list).toBeHidden();
    await expect(page.locator('img')).toHaveCount(0);
    await upload.input.setInputFiles([photo('again.png')]);
    await expect.poll(() => media(upload)).toEqual([expect.stringMatching(/^blob:/)]);
    expect(messages).toEqual([]);
  });
});

test.describe('grid', () => {
  test('picked and dropped files show as cards: thumbnails, icons and errors, in the order of the input',
      async ({page}) => {
        await recordBlobs(page);
        const messages = await openFixture(page, `${preview('grid')}<button type="button" id="after">After</button>`,
            {imgSrc: ALLOW_BLOB});
        const upload = new Upload(page, 'uploadInspectionPhotos');
        await expect(upload.root).toHaveAttribute('data-layout', 'grid');

        await upload.input.setInputFiles([photo('crack.png'), ...picked(pdf('report.pdf'))]);
        await dropBytes(page, upload.dropzone(), [{name: 'leak.png', type: 'image/png', base64: PNG},
          {name: 'notes.txt', type: 'text/plain', base64: 'eA=='}]);
        expect(await upload.files()).toEqual(['crack.png', 'report.pdf', 'leak.png']);
        expect(await upload.titles()).toEqual(['crack.png', 'report.pdf', 'leak.png', 'notes.txt']);
        await expect.poll(() => media(upload)).toEqual([expect.stringMatching(/^blob:/), 'icon',
          expect.stringMatching(/^blob:/), 'icon']);
        const cards = upload.list.locator('.attachment');
        for (const card of await cards.all()) {
          await expect(card).toHaveAttribute('data-orientation', 'vertical');
          await expect(card).toHaveAttribute('data-width', 'full');
          await expect(card).not.toHaveAttribute('data-size', /.*/);
        }
        await expect(cards.last()).toHaveAttribute('data-state', 'error');
        await expect(cards.last().locator('.attachment-description')).toHaveText('File type not accepted');
        await expect(upload.status).toHaveText('Added leak.png. Could not add notes.txt: File type not accepted.');
        // The thumbnail fills the card's square media.
        const img = upload.list.locator('.attachment-media img').first();
        expect(await img.evaluate(element => {
          const image = element.getBoundingClientRect();
          const media = element.parentElement!.getBoundingClientRect();
          return Math.abs(image.width - media.width) < 1 && Math.abs(image.width - image.height) < 1
              && media.width > 100;
        })).toBe(true);

        // The focus order is the input's order; a remove moves the focus on, as in the list.
        await upload.input.focus();
        await page.keyboard.press('Tab');
        await expect(upload.remove('crack.png')).toBeFocused();
        await page.keyboard.press('Tab');
        await expect(upload.remove('report.pdf')).toBeFocused();
        await page.keyboard.press('Enter');
        expect(await upload.files()).toEqual(['crack.png', 'leak.png']);
        await expect(upload.status).toHaveText('Removed report.pdf.');
        await expect(upload.remove('leak.png')).toBeFocused();
        await page.keyboard.press('Tab');
        await expect(upload.list.getByRole('button', {name: 'Dismiss notes.txt'})).toBeFocused();
        await page.keyboard.press('Enter');
        expect(await upload.titles()).toEqual(['crack.png', 'leak.png']);
        await expect(upload.remove('leak.png')).toBeFocused();
        await page.keyboard.press('Tab');
        await expect(page.locator('#after')).toBeFocused();
        expect(messages).toEqual([]);
      });

  for (const version of [2, 4] as const) {
    test(`htmx ${version}: the cards go through the upload's states, their buttons hidden meanwhile`, async ({page}) => {
      await recordBlobs(page);
      await holdUploads(page);
      let status = 500;
      const messages = await openFixture(page, '<form id="form" hx-post="/upload" hx-target="#result" '
          + `enctype="multipart/form-data">${preview('grid')}<button type="submit">Save</button></form>`
          + '<div id="result"></div>', {htmx: version, imgSrc: ALLOW_BLOB});
      const answers = await holdAnswers(page, () => ({status, body: status === 500 ? '' : '<p id="saved">Saved</p>'}));
      const upload = new Upload(page, 'uploadInspectionPhotos');
      await upload.input.setInputFiles([photo('crack.png'), ...picked(pdf('report.pdf'))]);
      await expect.poll(() => media(upload)).toEqual([expect.stringMatching(/^blob:/), 'icon']);
      const opacity = () => upload.list.locator('.attachment-media').first()
          .evaluate(element => Number(getComputedStyle(element).opacity));

      await page.getByRole('button', {name: 'Save'}).click();
      await expect(upload.root).toHaveAttribute('data-uploading', 'true');
      await expect(upload.list.getByRole('button')).toHaveCount(0);
      await fire(page, 'progress', 100, 4000);
      expect((await states(upload)).map(([state]) => state)).toEqual(['uploading', 'uploading']);
      expect(await opacity()).toBeLessThan(1);
      await fire(page, 'load', 4000, 4000);
      expect(await states(upload)).toEqual([['processing', 'true', 'Processing'], ['processing', 'true', 'Processing']]);

      answers.release();
      await expect(upload.status).toHaveText('The files could not be uploaded. Try again.');
      expect(await states(upload)).toEqual([['error', null, 'Upload failed'], ['error', null, 'Upload failed']]);
      await expect(upload.list.getByRole('button')).toHaveCount(2);

      status = 200;
      await page.getByRole('button', {name: 'Save'}).click();
      await expect(upload.root).toHaveAttribute('data-uploading', 'true');
      await fire(page, 'load', 4000, 4000);
      answers.release();
      await expect(page.locator('#saved')).toBeVisible();
      expect(await states(upload)).toEqual([[null, null, 'Uploaded'], [null, null, 'Uploaded']]);
      expect(await opacity()).toBe(1);
      expect(messages.filter(message => !/500|Internal Server Error|htmx:response:error/i.test(message)))
          .toEqual([]);
    });
  }
});
