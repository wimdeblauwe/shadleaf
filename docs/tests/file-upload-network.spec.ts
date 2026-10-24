import {expect, test} from '@playwright/test';
import {createServer} from 'node:http';
import previews from '../src/generated/previews.json' with {type: 'json'};
import {openFixture} from './fixture';

// sl:file-upload's upload progress with the browser's own upload events, which a request Playwright answers never
// gets: the fixture page is served as usual, then its routes are dropped and app.test is resolved to a server on the
// loopback, which reads the whole body and answers a second later. Chromium only, for its command-line switches: the
// host rule, and no local network access check (a page from app.test would not be let through to a loopback address).
// The loopback is fast, so the shares in between are not checked: file-upload.spec.ts fires them by hand. The switches
// are set when the browser starts, so the port is fixed, and the tests run one after the other.

const PORT = 41987;

const server = createServer((request, response) => {
  let size = 0;
  request.on('data', chunk => (size += chunk.length));
  request.on('end', () => setTimeout(() => {
    response.writeHead(200, {'Content-Type': 'text/html'});
    response.end(`<p id="saved">Saved ${size} bytes</p>`);
  }, 1000));
});

test.skip(({browserName}) => browserName !== 'chromium', 'Chromium\'s switches');
test.describe.configure({mode: 'serial'});
test.beforeAll(() => new Promise<void>(resolve => server.listen(PORT, '127.0.0.1', resolve)));
test.afterAll(() => new Promise<void>(resolve => server.close(() => resolve())));
test.use({
  launchOptions: {
    args: [`--host-resolver-rules=MAP app.test 127.0.0.1:${PORT}`,
      '--disable-features=LocalNetworkAccessChecks,BlockInsecurePrivateNetworkRequests'],
  },
});

const html = (previews.scenarios as { id: string, html: string }[])
    .find(scenario => scenario.id === 'file-upload--default')!.html;

for (const version of [2, 4] as const) {
  test(`htmx ${version}: uploading, then processing, then the answer`, async ({page}) => {
    const messages = await openFixture(page, '<div id="area"><form hx-post="/upload" hx-target="#area" '
        + `enctype="multipart/form-data">${html}<button type="submit">Save</button></form></div>`, {htmx: version});
    await page.unrouteAll();
    await page.evaluate(() => {
      const seen: string[] = [];
      (window as unknown as { seen: string[] }).seen = seen;
      new MutationObserver(() => {
        const attachment = document.querySelector('.file-upload-list .attachment');
        const text = attachment && `${attachment.getAttribute('data-state')}: `
            + attachment.querySelector('.attachment-description')!.textContent;
        if (text && seen.at(-1) !== text) {
          seen.push(text);
        }
      }).observe(document.body, {subtree: true, childList: true, characterData: true, attributes: true});
    });
    const size = 9 * 1024 * 1024 - 4096;
    await page.locator('#uploadAttachments').setInputFiles([{name: 'big.pdf', mimeType: 'application/pdf',
      buffer: Buffer.alloc(size, 1)}]);

    await page.getByRole('button', {name: 'Save'}).click();
    await expect(page.locator('#saved')).toBeVisible();
    expect(Number((await page.locator('#saved').textContent())!.match(/\d+/)![0])).toBeGreaterThan(size);
    const seen = await page.evaluate(() => (window as unknown as { seen: string[] }).seen);
    expect(seen[0]).toBe('idle: PDF · 9 MB');
    expect(seen.at(-1)).toBe('processing: Processing');
    expect(seen.slice(1, -1).every(text => /^uploading: Uploading( · \d+%)?$/.test(text))).toBe(true);
    expect(seen).toContain('uploading: Uploading · 100%');
    expect(messages).toEqual([]);
  });
}
