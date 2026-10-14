import {expect, test} from '@playwright/test';
import {readdirSync} from 'node:fs';
import {checkComponentGroups, componentGroups} from '../src/component-groups.mjs';

// The docs' sidebar lists the component pages in groups (src/component-groups.mjs, an explicit list so the URLs stay
// components/<page>/). astro.config.mjs runs checkComponentGroups when the build starts, so a page in no group, in
// two, or a group naming a page that does not exist fails the build.

const pages = readdirSync(new URL('../src/content/docs/components/', import.meta.url))
    .map(file => file.replace(/\.mdx?$/, ''))
    .filter(page => page !== 'index');

test('every component page is in exactly one group', () => {
  expect(() => checkComponentGroups(pages)).not.toThrow();
  expect(componentGroups.flatMap(group => [...(group.overview ? [group.overview] : []), ...group.pages]).sort())
      .toEqual([...pages].sort());
});

test('the check names a page in no group, in two, or without a file', () => {
  expect(() => checkComponentGroups([...pages, 'carousel'])).toThrow('carousel is in no group');
  expect(() => checkComponentGroups(pages.filter(page => page !== 'kbd'))).toThrow('kbd is in a group but has no page');
  componentGroups[0].pages.push('kbd');
  try {
    expect(() => checkComponentGroups(pages)).toThrow('kbd is in more than one group');
  } finally {
    componentGroups[0].pages.pop();
  }
});

test('the sidebar shows the overview, then the groups in order, with their pages', async ({page}) => {
  await page.goto('/components/');
  const components = page.locator('#starlight__sidebar details').filter({has: page.locator('summary', {hasText: /^\s*Components\s*$/})}).first();
  const overview = components.getByRole('link').first();
  await expect(overview).toHaveText('Overview');
  await expect(overview).toHaveAttribute('href', '/components/');
  await expect(overview).toHaveAttribute('aria-current', 'page');
  const groups = await components.locator(':scope > ul > li > details > summary').allTextContents();
  expect(groups.map(text => text.trim())).toEqual(componentGroups.map(group => group.label));
  const forms = components.locator('details').filter({has: page.locator('summary', {hasText: 'Forms'})});
  await expect(forms.getByRole('link')).toHaveCount(componentGroups.find(group => group.label === 'Forms')!.pages.length);
  await expect(forms.getByRole('link', {name: 'Select (Native)'})).toHaveAttribute('href', '/components/select-native/');
  // A group with an overview page lists it first, as "Overview".
  const shell = components.locator('details').filter({has: page.locator('summary', {hasText: 'Application shell'})});
  await expect(shell.getByRole('link').first()).toHaveText('Overview');
  await expect(shell.getByRole('link').first()).toHaveAttribute('href', '/components/application-shell/');
});

test('the overview has a card per page, under its group\'s heading', async ({page}) => {
  await page.goto('/components/');
  const main = page.locator('main');
  for (const group of componentGroups) {
    await expect(main.getByRole('heading', {level: 2, name: group.label, exact: true})).toBeVisible();
  }
  // A card per component; a group's overview page is a link below its heading instead.
  const overviews = componentGroups.filter(group => group.overview).length;
  await expect(main.locator('.sl-link-card a[href^="/components/"]')).toHaveCount(pages.length - overviews);
  await expect(main.locator('p a[href="/components/application-shell/"]')).toHaveText('overview');
  const button = main.locator('.sl-link-card').filter({has: page.locator('a[href="/components/button/"]')});
  await expect(button).toContainText('Button');
  await expect(button).toContainText('A button, or a link that looks like one.');
});

test('the layouts overview is first in the Layouts group and has a card per layout', async ({page}) => {
  const layouts = readdirSync(new URL('../src/content/docs/layouts/', import.meta.url))
      .map(file => file.replace(/\.mdx?$/, ''))
      .filter(name => name !== 'index');
  await page.goto('/layouts/');
  const group = page.locator('#starlight__sidebar details').filter({has: page.locator('summary', {hasText: /^\s*Layouts\s*$/})}).first();
  await expect(group.getByRole('link').first()).toHaveText('Overview');
  await expect(group.getByRole('link').first()).toHaveAttribute('aria-current', 'page');
  const cards = page.locator('main .sl-link-card a');
  await expect(cards).toHaveCount(layouts.length);
  // In the sidebar's order: by sidebar.order, so shadcn's block numbers.
  const sidebarLinks = (await group.getByRole('link').evaluateAll(links => links.map(link => link.getAttribute('href'))))
      .slice(1);
  expect(await cards.evaluateAll(links => links.map(link => link.getAttribute('href')))).toEqual(sidebarLinks);
});
