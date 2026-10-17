import {expect, type Page} from '@playwright/test';
import previews from '../src/generated/previews.json' with {type: 'json'};

/** The full suite (`pnpm run test:full`): every skin, and Firefox too (playwright.config.ts). */
export const full = !!process.env.SHADLEAF_TEST_FULL;

/** The skins the showcase is checked in: the default skin (vega, first), or every skin in the full suite. */
export const skins: string[] = previews.skins.map(skin => skin.name).slice(0, full ? undefined : 1);
export const themes = ['light', 'dark'] as const;

/**
 * The widths the showcase is checked at: Playwright's desktop, and a phone, where the sidebar is a closed panel and its
 * trigger opens it (768 px is the sidebar's breakpoint).
 */
export const widths = [
  {name: 'desktop', viewport: {width: 1280, height: 720}},
  {name: 'phone', viewport: {width: 390, height: 844}},
] as const;

/**
 * The showcase pages: every scenario (with the sidebar layout's shell), and the header layout's shell alone (a page
 * holds one main).
 */
export const showcasePages = [
  {name: 'components', path: ''},
  {name: 'header layout', path: 'header/'},
] as const;

/** Every skin in both themes: the combinations the showcase is checked in. */
export const combinations = skins.flatMap(skin => themes.map(theme => ({skin, theme})));

export async function openShowcase(page: Page, skin: string, theme: string, path = '') {
  expect(previews.scenarios.length, 'no previews: run `pnpm run generate` first').toBeGreaterThan(0);
  await page.goto(`/showcase/${path}?skin=${skin}&theme=${theme}`);
  await expect(page.locator('html')).toHaveAttribute('data-skin', skin);
  // The stylesheet is in place once the page background comes from the tokens.
  await page.waitForFunction(() => [...document.styleSheets].some(sheet => sheet.href?.includes('/shadleaf/assets/')));
}
