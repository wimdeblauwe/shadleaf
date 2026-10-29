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
 * The component the showcase checks (a11y, focus, forced-colors) are limited to, from SHADLEAF_TEST_COMPONENT
 * (`pnpm run test:component -- calendar`): its `component` in previews.json, which a family (select with its parts)
 * shares. Unset, they check every component, as CI does.
 */
export const component: string | undefined = process.env.SHADLEAF_TEST_COMPONENT || undefined;
if (component && !previews.scenarios.some(scenario => scenario.component === component)) {
  throw new Error(`SHADLEAF_TEST_COMPONENT: no previews for ${component}; one of ${
      [...new Set(previews.scenarios.map(scenario => scenario.component))].join(', ')}`);
}

/** Whether the checks of the given components run: always without SHADLEAF_TEST_COMPONENT. */
export const covers = (...components: string[]) => !component || components.includes(component);

/**
 * The showcase pages: every scenario (with the sidebar layout's shell), and the header layout's shell alone (a page
 * holds one main). With SHADLEAF_TEST_COMPONENT, only those that show one of its scenarios.
 */
export const showcasePages = ([
  {name: 'components', path: '', shows: (scenario: Scenario) => scenario.showcase !== false},
  {name: 'header layout', path: 'header/', shows: (scenario: Scenario) => scenario.id === 'sidebar--header'},
] as const).filter(showcase => previews.scenarios.some(scenario => showcase.shows(scenario) && covers(scenario.component)));

type Scenario = (typeof previews.scenarios)[number];

/**
 * A selector per scenario of SHADLEAF_TEST_COMPONENT (none without it). For axe's include, which leaves out the
 * page-level rules (one main, landmarks) then; the unfiltered run checks those.
 */
export const componentScenarios = previews.scenarios.filter(scenario => scenario.component === component)
    .map(scenario => `[data-scenario="${scenario.id}"]`);

/**
 * With SHADLEAF_TEST_COMPONENT, takes every other scenario off the opened showcase, so the hover and focus passes and the
 * forced-colors checks walk that component's controls only. Without it, does nothing.
 */
export async function keepComponentOnly(page: Page) {
  if (component) {
    await keepScenarios(page, componentScenarios);
  }
}

/** Takes every scenario but the given ones (selectors) off the opened showcase. */
export async function keepScenarios(page: Page, scenarios: string[]) {
  await page.locator('[data-scenario]').evaluateAll((stages, selector) => {
    for (const stage of stages.filter(stage => !stage.matches(selector))) {
      stage.remove();
    }
  }, scenarios.join(', '));
}

/**
 * Takes every scenario without an element matching the selector off the opened showcase: for tests that run axe per
 * element, as every axe run walks the whole page.
 */
export async function keepScenariosWith(page: Page, selector: string) {
  await keepScenarios(page, [`[data-scenario]:has(${selector})`]);
}

/**
 * The components a showcase page shows (only SHADLEAF_TEST_COMPONENT's with it), each with a selector per scenario: for
 * checks too slow to walk the whole page in one test.
 */
export function showcaseComponents(showcase: (typeof showcasePages)[number]): {name: string, scenarios: string[]}[] {
  const shown = previews.scenarios.filter(scenario => showcase.shows(scenario) && covers(scenario.component));
  return [...new Set(shown.map(scenario => scenario.component))].map(name => ({
    name,
    scenarios: shown.filter(scenario => scenario.component === name).map(scenario => `[data-scenario="${scenario.id}"]`),
  }));
}

/** Every skin in both themes: the combinations the showcase is checked in. */
export const combinations = skins.flatMap(skin => themes.map(theme => ({skin, theme})));

export async function openShowcase(page: Page, skin: string, theme: string, path = '') {
  expect(previews.scenarios.length, 'no previews: run `pnpm run generate` first').toBeGreaterThan(0);
  await page.goto(`/showcase/${path}?skin=${skin}&theme=${theme}`);
  await expect(page.locator('html')).toHaveAttribute('data-skin', skin);
  // The stylesheet is in place once the page background comes from the tokens.
  await page.waitForFunction(() => [...document.styleSheets].some(sheet => sheet.href?.includes('/shadleaf/assets/')));
}
