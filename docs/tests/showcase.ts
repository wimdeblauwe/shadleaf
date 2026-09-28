import {expect, type Page} from '@playwright/test';
import previews from '../src/generated/previews.json' with {type: 'json'};

export const skins: string[] = previews.skins.map(skin => skin.name);
export const themes = ['light', 'dark'] as const;

/** Every skin in both themes: the combinations the showcase is checked in. */
export const combinations = skins.flatMap(skin => themes.map(theme => ({skin, theme})));

export async function openShowcase(page: Page, skin: string, theme: string) {
  expect(previews.scenarios.length, 'no previews: run `pnpm run generate` first').toBeGreaterThan(0);
  await page.goto(`/showcase/?skin=${skin}&theme=${theme}`);
  await expect(page.locator('html')).toHaveAttribute('data-skin', skin);
  // The stylesheet is in place once the page background comes from the tokens.
  await page.waitForFunction(() => [...document.styleSheets].some(sheet => sheet.href?.includes('/shadleaf/assets/')));
}
