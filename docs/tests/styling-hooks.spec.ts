import {expect, test} from '@playwright/test';
import {readdirSync, readFileSync} from 'node:fs';

// Every component page has a Styling hooks section after its attributes, for the same components
// (StylingHooks.astro, from styling-hooks.json), and each group of hooks copies as an empty stylesheet.

const directory = new URL('../src/content/docs/components/', import.meta.url);
const pages = readdirSync(directory).filter(file => file.endsWith('.mdx'));

const named = (source: string, tag: string) =>
    [...source.matchAll(new RegExp(`<${tag}\\s+component="([^"]+)"`, 'g'))].map(match => match[1]);

test('every page with an attribute table has the styling hooks of the same components, after it', () => {
  for (const page of pages) {
    const source = readFileSync(new URL(page, directory), 'utf8');
    const attributes = named(source, 'ComponentAttributes');
    if (attributes.length === 0) {
      continue;
    }
    expect(named(source, 'StylingHooks'), page).toEqual(attributes);
    expect(source.indexOf('\n## Styling hooks\n'), `${page} has a Styling hooks section after its Attributes`)
        .toBeGreaterThan(source.indexOf('\n## Attributes\n'));
  }
});

test.describe('copy as CSS', () => {
  test.use({permissions: ['clipboard-read', 'clipboard-write']});

  test('a group copies as an empty rule per selector; its button shows while the pointer is on the group', async ({page}) => {
    await page.goto('/components/button/');
    const props = page.locator('.styling-hooks-group').filter({has: page.locator('strong', {hasText: /^Props$/})});
    const copy = props.getByRole('button', {name: 'Copy the props of sl:button as CSS'});
    await expect(copy).toHaveCSS('opacity', '0');
    await props.locator('table').hover();
    await expect(copy).toHaveCSS('opacity', '1');

    await copy.click();
    await expect(copy).toHaveText('Copied');
    const css = await page.evaluate(() => navigator.clipboard.readText());
    expect(css).toContain('/* variant="default", the default: no attribute */\n.button:not([data-variant]) {\n}');
    expect(css).toContain(".button[data-variant='outline'] {\n}");
    expect(css).not.toContain('aria-');
    await expect(copy).toHaveText('CSS');
  });

  test('the button shows while focus is in the group', async ({page}) => {
    await page.goto('/components/button/');
    const states = page.locator('.styling-hooks-group').filter({has: page.locator('strong', {hasText: /^States$/})});
    const copy = states.getByRole('button', {name: 'Copy the states of sl:button as CSS'});
    await copy.focus();
    await expect(copy).toHaveCSS('opacity', '1');
  });
});
