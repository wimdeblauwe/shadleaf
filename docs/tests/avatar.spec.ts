import {expect, test, type Page} from '@playwright/test';
import AxeBuilder from '@axe-core/playwright';
import previews from '../src/generated/previews.json' with {type: 'json'};
import {combinations, openShowcase} from './showcase';
import {openFixture, type Routes} from './fixture';

// sl:avatar's broken-image fallback (js/avatar.js marks the image data-status="loaded" or "error"; the stylesheet shows
// the image only once it is loaded). On the showcase, in every skin and theme: axe, a picture that loads and one that
// fails. In fixture pages under a strict Content-Security-Policy with the csp build: a 404, an image img-src blocks, one
// that failed or loaded before the script ran, the fallback while loading with no layout shift, a src changed later,
// htmx swaps and history, shadleaf.js with no Alpine on the page, no Shadleaf script, no JavaScript, forced colours.
// Runs in Chromium and Firefox: Chromium draws a broken-image icon even with alt="", Firefox draws nothing.

const TAGS = ['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa', 'wcag22aa', 'best-practice'];

type Scenario = { id: string, html: string };
const scenarios = previews.scenarios as Scenario[];
/** The markup of the "broken" scenario: a named avatar with an image and the initials GH. */
const AVATAR = scenarios.find(scenario => scenario.id === 'avatar--broken')!.html;

/** An avatar as <sl:avatar label=...> renders it, with an id and the image's src (or none). */
function avatar(id: string, src: string | null, imageAttributes = ''): string {
  return AVATAR
      .replace('<span class="avatar"', `<span class="avatar" id="${id}"`)
      .replace(/ src="[^"]*"/, src === null ? '' : ` src="${src}"`)
      .replace('<img class="avatar-image"', `<img class="avatar-image"${imageAttributes ? ' ' + imageAttributes : ''}`);
}

/** A portrait with a size of its own, and one with only a viewBox (no natural width in every browser). */
const PHOTO = Buffer.from(`<svg xmlns="http://www.w3.org/2000/svg" width="40" height="40" viewBox="0 0 40 40">`
    + `<rect width="40" height="40" fill="#b08968"/><circle cx="20" cy="16" r="7" fill="#f5ebe0"/></svg>`);
const SIZELESS = Buffer.from(`<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 40 40">`
    + `<rect width="40" height="40" fill="#6b705c"/></svg>`);

function photos(delay = 0): Routes {
  return {
    '/photos/ada.svg': () => ({contentType: 'image/svg+xml', body: PHOTO, delay}),
    '/photos/alan.svg': () => ({contentType: 'image/svg+xml', body: SIZELESS, delay}),
    '/photos/slow.svg': () => ({contentType: 'image/svg+xml', body: PHOTO, delay: 1500}),
    '/photos/missing.png': () => ({contentType: 'text/plain', status: 404, body: 'Not found', delay}),
  };
}

type State = { status: string | null, image: boolean, fallback: boolean };

/** What the avatar shows: its image's mark, and which of image and fallback is displayed and visible. */
function state(page: Page, id: string): Promise<State> {
  return page.locator(`#${id}`).evaluate(avatar => {
    const shows = (element: Element | null) => {
      if (!element) {
        return false;
      }
      const style = getComputedStyle(element);
      return style.display !== 'none' && style.visibility === 'visible';
    };
    const image = avatar.querySelector<HTMLImageElement>('.avatar-image');
    return {status: image?.dataset.status ?? null, image: shows(image),
      fallback: shows(avatar.querySelector('.avatar-fallback'))};
  });
}

const LOADED: State = {status: 'loaded', image: true, fallback: false};
const FAILED: State = {status: 'error', image: false, fallback: true};
const LOADING: State = {status: null, image: false, fallback: true};

/** The avatar's box and its image's: the same, whichever shows, so nothing moves. */
function boxes(page: Page, id: string) {
  return page.locator(`#${id}`).evaluate(avatar => [avatar, avatar.querySelector('.avatar-image')!]
      .map(element => {
        const box = element.getBoundingClientRect();
        return [box.x, box.y, box.width, box.height];
      }));
}

/** Console messages other than the failures the tests cause on purpose. */
function unexpected(messages: string[]): string[] {
  return messages.filter(message => !/Failed to load resource|404|img-src/.test(message));
}

// --- the showcase ------------------------------------------------------------------------------------------------------

for (const {skin, theme} of combinations) {
  test(`a picture that loads covers the fallback, a broken one shows it; axe: ${skin}, ${theme}`, async ({page}) => {
    await openShowcase(page, skin, theme);
    const loaded = page.locator('[data-scenario="avatar--default"] .avatar');
    const broken = page.locator('[data-scenario="avatar--broken"] .avatar');
    await expect(loaded.locator('.avatar-image')).toHaveAttribute('data-status', 'loaded');
    await expect(broken.locator('.avatar-image')).toHaveAttribute('data-status', 'error');
    await expect(loaded.locator('.avatar-image')).toBeVisible();
    await expect(loaded.locator('.avatar-fallback')).toBeHidden();
    await expect(broken.locator('.avatar-image')).toBeHidden();
    await expect(broken.locator('.avatar-fallback')).toBeVisible();
    // Named by the label whichever part shows; the image itself stays alt="".
    await expect(broken).toHaveAccessibleName('Grace Hopper');
    await expect(broken).toHaveAttribute('role', 'img');
    await expect(broken.locator('.avatar-image')).toHaveAttribute('alt', '');

    const results = await new AxeBuilder({page}).include('[data-scenario^="avatar--"]').withTags(TAGS).analyze();
    expect(results.violations.map(violation => `${violation.id}: ${violation.help}\n    ${
        violation.nodes.map(node => `${node.target.join(' ')} ${node.failureSummary}`).join('\n    ')}`)).toEqual([]);
  });
}

// --- fixture pages -----------------------------------------------------------------------------------------------------

test('a 404 and a URL img-src blocks show the fallback; a picture that loads shows', async ({page}) => {
  const messages = await openFixture(page, avatar('ada', '/photos/ada.svg') + avatar('grace', '/photos/missing.png')
      // The fixture's policy has no img-src, so default-src 'self' blocks a data: URI.
      + avatar('blocked', `data:image/svg+xml,${encodeURIComponent(PHOTO.toString())}`)
      + avatar('none', null) + avatar('empty', ''), {routes: photos()});

  await expect.poll(() => state(page, 'ada')).toEqual(LOADED);
  await expect.poll(() => state(page, 'grace')).toEqual(FAILED);
  await expect.poll(() => state(page, 'blocked')).toEqual(FAILED);
  // No src or an empty one: the stylesheet hides the image, whatever the script marks.
  expect(await state(page, 'none')).toEqual(LOADING);
  expect((await state(page, 'empty')).fallback).toBe(true);
  expect((await state(page, 'empty')).image).toBe(false);
  expect(unexpected(messages)).toEqual([]);
});

test('an image that failed or loaded before the script ran is marked when it starts; nothing broken shows before',
    async ({page}) => {
      // The images finish while Shadleaf's script is held back. Record, at every frame with the stylesheet applied
      // before the script ran, whether the finished images showed.
      await page.addInitScript(() => {
        const frames: string[] = [];
        (window as unknown as {frames_: string[]}).frames_ = frames;
        const sample = () => {
          if ((document as unknown as {slAvatarImages?: boolean}).slAvatarImages) {
            return;
          }
          const images = [...document.querySelectorAll<HTMLImageElement>('.avatar-image')]
              .filter(image => image.complete && getComputedStyle(image).position === 'absolute');
          if (images.length === 3) {
            frames.push(images.map(image => getComputedStyle(image).visibility).join(' '));
          }
          requestAnimationFrame(sample);
        };
        requestAnimationFrame(sample);
      });
      const messages = await openFixture(page, avatar('ada', '/photos/ada.svg') + avatar('alan', '/photos/alan.svg')
          + avatar('grace', '/photos/missing.png'), {routes: photos(), scriptDelay: 800});

      const frames = await page.evaluate(() => (window as unknown as {frames_: string[]}).frames_);
      expect(frames.length, 'frames painted before the script ran').toBeGreaterThan(5);
      expect(new Set(frames)).toEqual(new Set(['hidden hidden hidden']));
      await expect.poll(() => state(page, 'ada')).toEqual(LOADED);
      // An SVG with only a viewBox has no natural width in every browser: decode() tells it from a broken one.
      await expect.poll(() => state(page, 'alan')).toEqual(LOADED);
      await expect.poll(() => state(page, 'grace')).toEqual(FAILED);
      expect(unexpected(messages)).toEqual([]);
    });

test('the fallback shows while the picture loads, in the same box', async ({page}) => {
  // The page's load event waits for its images, so the picture is asked for once the page is there.
  const messages = await openFixture(page, avatar('slow', ''), {routes: photos()});
  await page.locator('#slow .avatar-image').evaluate(image => image.setAttribute('src', '/photos/slow.svg'));
  expect(await state(page, 'slow')).toEqual(LOADING);
  const before = await boxes(page, 'slow');
  expect(before[0], 'the image lies exactly over the avatar').toEqual(before[1]);

  await expect.poll(() => state(page, 'slow'), {timeout: 5000}).toEqual(LOADED);
  expect(await boxes(page, 'slow'), 'no layout shift').toEqual(before);
  expect(unexpected(messages)).toEqual([]);
});

test('a src that changes later: the fallback while the new one loads, then the new state', async ({page}) => {
  const messages = await openFixture(page, avatar('a', '/photos/ada.svg') + avatar('b', ''), {routes: photos()});
  await expect.poll(() => state(page, 'a')).toEqual(LOADED);
  const src = (id: string, value: string) => page.locator(`#${id} .avatar-image`)
      .evaluate((image, value) => image.setAttribute('src', value), value);

  await src('a', '/photos/missing.png');
  await expect.poll(() => state(page, 'a')).toEqual(FAILED);
  await src('a', '/photos/slow.svg');
  // The old mark is gone at once: no broken image while the new one loads, and no stale picture either.
  expect(await state(page, 'a')).toEqual(LOADING);
  await expect.poll(() => state(page, 'a'), {timeout: 5000}).toEqual(LOADED);

  // A user who had no photo gets one (an empty src first, as th:src="${null}" renders it).
  await src('b', '/photos/alan.svg');
  await expect.poll(() => state(page, 'b')).toEqual(LOADED);
  // Back to the same picture, from the browser's cache: marked loaded again.
  await src('a', '/photos/missing.png');
  await expect.poll(() => state(page, 'a')).toEqual(FAILED);
  await src('a', '/photos/slow.svg');
  await expect.poll(() => state(page, 'a')).toEqual(LOADED);
  expect(unexpected(messages)).toEqual([]);
});

test('htmx: swapped-in avatars and a swapped src are marked; history brings back the right state', async ({page}) => {
  const list = (prefix: string) => avatar(`${prefix}-ada`, '/photos/ada.svg')
      + avatar(`${prefix}-grace`, '/photos/missing.png');
  const messages = await openFixture(page, `<div id="area">${list('first')}</div>
      <button id="load" hx-get="/members" hx-target="#area">Load</button>
      <button id="swap-src" hx-get="/photo-src" hx-target="#first-ada .avatar-image" hx-swap="outerHTML">Swap</button>
      <a href="/next" id="next" hx-get="/next" hx-target="main" hx-push-url="true">Next</a>`, {
    htmx: true,
    routes: {
      ...photos(),
      '/members': () => ({body: list('swapped')}),
      // The image again with another src, as a server would re-render it.
      '/photo-src': () => ({body: '<img class="avatar-image" alt="" src="/photos/missing.png">'}),
      '/next': () => ({body: '<p id="next-page">Next page</p>'}),
    },
  });
  await expect.poll(() => state(page, 'first-ada')).toEqual(LOADED);
  await expect.poll(() => state(page, 'first-grace')).toEqual(FAILED);

  await page.locator('#swap-src').click();
  await expect.poll(() => state(page, 'first-ada')).toEqual(FAILED);

  await page.locator('#load').click();
  await expect.poll(() => state(page, 'swapped-ada')).toEqual(LOADED);
  await expect.poll(() => state(page, 'swapped-grace')).toEqual(FAILED);

  // htmx saves the page with the marks. The restored images come from the browser's memory cache (also with
  // Cache-Control: no-store), fire load or error again, and keep or correct them.
  await page.locator('#next').click();
  await expect(page.locator('#next-page')).toBeVisible();
  await page.goBack();
  await expect(page.locator('#swapped-ada')).toBeAttached();
  await expect.poll(() => state(page, 'swapped-ada')).toEqual(LOADED);
  await expect.poll(() => state(page, 'swapped-grace')).toEqual(FAILED);
  expect(unexpected(messages)).toEqual([]);
});

test('shadleaf.js marks the images on a page without Alpine (shadleaf.assets.alpine=external)', async ({page}) => {
  const messages = await openFixture(page, avatar('ada', '/photos/ada.svg') + avatar('grace', '/photos/missing.png'),
      {routes: photos(), alpine: 'external'});
  await expect.poll(() => state(page, 'ada')).toEqual(LOADED);
  await expect.poll(() => state(page, 'grace')).toEqual(FAILED);
  expect(await page.evaluate(() => 'Alpine' in window)).toBe(false);
  expect(unexpected(messages)).toEqual([]);
});

test('without Shadleaf\'s script every avatar shows its fallback, never a broken image', async ({page}) => {
  await openFixture(page, avatar('ada', '/photos/ada.svg') + avatar('grace', '/photos/missing.png'),
      {routes: photos(), alpine: false});
  await page.waitForLoadState('load');
  expect(await state(page, 'ada')).toEqual(LOADING);
  expect(await state(page, 'grace')).toEqual(LOADING);
});

test('without JavaScript the picture lies over the fallback', async ({browser}) => {
  const context = await browser.newContext({javaScriptEnabled: false});
  const page = await context.newPage();
  await openFixture(page, avatar('ada', '/photos/ada.svg') + avatar('grace', '/photos/missing.png'),
      {routes: photos(), alpine: false});
  await page.waitForLoadState('load');
  // Nothing marks the images: both show (scripting: none), on top of the fallback, which a loaded picture covers.
  await expect(page.locator('#ada .avatar-image')).toBeVisible();
  await expect(page.locator('#ada .avatar-image')).not.toHaveAttribute('data-status', /.*/);
  const top = await page.locator('#ada').evaluate(avatar => {
    const box = avatar.getBoundingClientRect();
    return (document.elementFromPoint(box.x + box.width / 2, box.y + box.height / 2) as HTMLElement).className;
  });
  expect(top).toBe('avatar-image');
  await context.close();
});

test('forced colours: a broken image shows the initials in the system colours', async ({page}) => {
  await page.emulateMedia({forcedColors: 'active'});
  await openFixture(page, avatar('ada', '/photos/ada.svg') + avatar('grace', '/photos/missing.png'),
      {routes: photos()});
  await expect.poll(() => state(page, 'ada')).toEqual(LOADED);
  await expect.poll(() => state(page, 'grace')).toEqual(FAILED);
  const colours = await page.locator('#grace').evaluate(avatar => {
    const probe = document.createElement('span');
    probe.style.color = 'CanvasText';
    document.body.append(probe);
    const canvasText = getComputedStyle(probe).color;
    probe.remove();
    return {fallback: getComputedStyle(avatar.querySelector('.avatar-fallback')!).color, canvasText};
  });
  expect(colours.fallback).toBe(colours.canvasText);
});
