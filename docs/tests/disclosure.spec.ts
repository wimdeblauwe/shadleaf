import {expect, test, type Locator, type Page} from '@playwright/test';
import AxeBuilder from '@axe-core/playwright';
import {combinations, openShowcase} from './showcase';
import {openFixture} from './fixture';

// sl:accordion and sl:collapsible, details elements with no script. On the showcase, in every skin and theme: every
// enabled trigger opens and closes its section with the keyboard, shows focus, and the open section passes axe and
// shows focus on each control in it; a single accordion keeps one section open; a disabled trigger is out of the tab
// order and ignores the pointer. Then the height animation and reduced motion. In fixture pages under a strict
// Content-Security-Policy: find-in-page by a link to an element in a closed section, and htmx (a swap, history). Runs
// in Chromium and Firefox, whose support for the details features differs.

const TAGS = ['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa', 'wcag22aa', 'best-practice'];
const FOCUSABLE = ':is(a[href], button, input, select, textarea, [tabindex]):not(:disabled, [tabindex="-1"])';

function isOpen(details: Locator) {
  return details.evaluate(element => (element as HTMLDetailsElement).open);
}

/** Whether the focused element shows its focus (an outline or a box-shadow ring). */
function focusVisible(page: Page) {
  return page.evaluate(() => {
    const element = document.activeElement as HTMLElement;
    const style = getComputedStyle(element);
    return {
      label: element.outerHTML.slice(0, 100),
      visible: element.matches(':focus-visible')
          && ((style.outlineStyle !== 'none' && parseFloat(style.outlineWidth) > 0) || style.boxShadow !== 'none'),
    };
  });
}

for (const {skin, theme} of combinations) {
  test(`every accordion and collapsible works with the keyboard and passes axe open: ${skin}, ${theme}`,
      async ({page}) => {
        await openShowcase(page, skin, theme);
        const items = page.locator('main :is(.accordion-item, .collapsible)');
        const count = await items.count();
        expect(count, 'the showcase has accordions and collapsibles').toBeGreaterThan(0);

        const failures: string[] = [];
        for (let i = 0; i < count; i++) {
          const item = items.nth(i);
          const trigger = item.locator('> summary');
          const label = `${await item.getAttribute('class')} "${(await trigger.textContent())!.trim()}"`;
          const wasOpen = await isOpen(item);

          if (await item.getAttribute('data-disabled')) {
            await expect(trigger, `${label}: out of the tab order`).toHaveAttribute('tabindex', '-1');
            // The pointer goes through to the details element, which does not toggle.
            await trigger.click({force: true});
            expect(await isOpen(item), `${label}: a click does not toggle it`).toBe(wasOpen);
            continue;
          }

          // Reach the trigger with the keyboard, back from the element after it (Shift+Tab from the page's first
          // control leaves the page in Firefox).
          await trigger.focus();
          await page.keyboard.press('Tab');
          await page.keyboard.press('Shift+Tab');
          await expect(trigger).toBeFocused();
          const focused = await focusVisible(page);
          if (!focused.visible) {
            failures.push(`${label}: no visible focus on the trigger`);
          }

          if (wasOpen) {
            await page.keyboard.press('Enter');
            await expect.poll(() => isOpen(item), `${label}: Enter closes it`).toBe(false);
          }
          await page.keyboard.press(wasOpen ? ' ' : 'Enter');
          await expect.poll(() => isOpen(item), `${label}: opens from the keyboard`).toBe(true);

          // A single accordion (items with a name) keeps this one open only.
          const name = await item.getAttribute('name');
          if (name) {
            const open = await page.locator(`details[name="${name}"][open]`).count();
            expect(open, `${label}: the only open section of ${name}`).toBe(1);
          }

          // axe on the whole accordion (or the collapsible) with this section open.
          await item.evaluate(element => (element.closest('.accordion') ?? element).setAttribute('data-axe', ''));
          const results = await new AxeBuilder({page}).include('[data-axe]').withTags(TAGS).analyze();
          await page.locator('[data-axe]').evaluate(element => element.removeAttribute('data-axe'));
          failures.push(...results.violations.map(violation => `${label}: ${violation.id}: ${violation.help}\n    ${
              violation.nodes.map(node => node.target.join(' ')).join('\n    ')}`));

          // Tab through the content's controls: each shows its focus.
          const controls = item.locator(`> :not(summary) ${FOCUSABLE}, > :not(summary)${FOCUSABLE}`);
          const controlCount = await controls.count();
          for (let c = 0; c < controlCount; c++) {
            await page.keyboard.press('Tab');
            const inside = await focusVisible(page);
            if (!inside.visible) {
              failures.push(`${label}: no visible focus on ${inside.label}`);
            }
          }

          // Leave it as it was.
          await trigger.focus();
          if (!wasOpen) {
            await page.keyboard.press('Enter');
            await expect.poll(() => isOpen(item)).toBe(false);
          }
        }
        expect(failures, failures.join('\n')).toEqual([]);
      });
}

test('a single accordion: opening a section closes the other, and the open one closes on a click', async ({page}) => {
  await openShowcase(page, 'vega', 'light');
  const items = page.locator('[data-scenario="accordion--default"] .accordion-item');
  await expect(items.nth(0)).toHaveAttribute('open');

  await items.nth(1).locator('> summary').click();
  await expect(items.nth(1)).toHaveAttribute('open');
  await expect(items.nth(0)).not.toHaveAttribute('open');
  await items.nth(1).locator('> summary').click();
  await expect(items.nth(1)).not.toHaveAttribute('open');
  expect(await page.locator('[data-scenario="accordion--default"] details[open]').count()).toBe(0);
});

test('a multiple accordion opens and closes sections on their own', async ({page}) => {
  await openShowcase(page, 'vega', 'light');
  const items = page.locator('[data-scenario="accordion--multiple"] .accordion-item');
  await expect(items.nth(0)).toHaveAttribute('open');
  await expect(items.nth(1)).toHaveAttribute('open');
  await items.nth(0).locator('> summary').click();
  await expect(items.nth(0)).not.toHaveAttribute('open');
  await expect(items.nth(1)).toHaveAttribute('open');
});

test('the trigger is a disclosure with a heading inside', async ({page, browserName}) => {
  test.skip(browserName !== 'chromium', 'reads the accessibility tree through the Chrome DevTools Protocol');
  await openShowcase(page, 'vega', 'light');
  const client = await page.context().newCDPSession(page);
  const {nodes} = await client.send('Accessibility.getFullAXTree') as {nodes: Array<{
    ignored: boolean, role?: {value: string}, name?: {value: string},
    properties?: Array<{name: string, value: {value: unknown}}>,
  }>};
  const named = (text: string) => nodes.filter(node => !node.ignored && node.name?.value === text);
  const [disclosure] = named('Is it accessible?').filter(node => node.role?.value.startsWith('DisclosureTriangle'));
  expect(disclosure, 'the summary is still a disclosure').toBeDefined();
  expect(disclosure.properties?.find(property => property.name === 'expanded')?.value.value).toBe(true);
  const [heading] = named('Is it accessible?').filter(node => node.role?.value === 'heading');
  expect(heading, 'its text is a heading').toBeDefined();
  expect(heading.properties?.find(property => property.name === 'level')?.value.value).toBe(3);
});

for (const reducedMotion of ['no-preference', 'reduce'] as const) {
  test.describe(() => {
    test.use({contextOptions: {reducedMotion}});

    test(`the height animates only where the browser can animate to auto: ${reducedMotion} motion`,
        async ({page, browserName}) => {
          await openShowcase(page, 'vega', 'light');
          const item = page.locator('[data-scenario="accordion--default"] .accordion-item').nth(1);
          const animation = await item.evaluate(element => ({
            interpolates: CSS.supports('interpolate-size', 'allow-keywords'),
            duration: getComputedStyle(element, '::details-content').transitionDuration,
          }));
          expect(animation.interpolates, 'Chromium has interpolate-size, Firefox not yet')
              .toBe(browserName === 'chromium');
          const animates = reducedMotion === 'no-preference' && animation.interpolates;
          expect(animation.duration).toBe(animates ? '0.2s' : '0s');

          // Shortly after it starts opening, the section shows part of its content; without the animation, all of it.
          const shownAndFull = () => item.evaluate(element => ({
            shown: element.getBoundingClientRect().height - element.querySelector('summary')!.getBoundingClientRect().height,
            full: element.querySelector('.accordion-content')!.getBoundingClientRect().height,
          }));
          await item.locator('> summary').click();
          await page.waitForTimeout(50);
          const early = await shownAndFull();
          if (animates) {
            expect(early.shown, 'mid-animation').toBeLessThan(early.full);
          } else {
            expect(early.shown, 'at once').toBeGreaterThanOrEqual(early.full);
          }
          await expect.poll(async () => {
            const {shown, full} = await shownAndFull();
            return shown >= full;
          }).toBe(true);

          // The chevron turns, at once under reduced motion.
          const icon = item.locator('.accordion-trigger-icon');
          expect(await icon.evaluate(element => getComputedStyle(element).transitionDuration))
              .toBe(reducedMotion === 'reduce' ? '0s' : '0.15s');
          await expect.poll(() => icon.evaluate(element => getComputedStyle(element).rotate)).toBe('180deg');
        });
  });
}

// --- fixture pages ---------------------------------------------------------------------------------------------------

/** An accordion item marked up as <sl:accordion-item> renders it (see accordion.approved.html). */
function item(name: string | null, title: string, content: string, attributes = ''): string {
  return `<details class="accordion-item"${name ? ` name="${name}"` : ''} ${attributes}>`
      + `<summary class="accordion-trigger"><span class="accordion-trigger-text" role="heading" aria-level="3">${title}`
      + `</span></summary><div class="accordion-content">${content}</div></details>`;
}

test('under a strict policy, with no script: a link to an element in a closed section opens it',
    async ({page}) => {
      const messages = await openFixture(page, `<div class="accordion">
          ${item('faq', 'Plans', '<p id="plans">Three plans.</p>')}
          ${item('faq', 'Refunds', '<p id="refunds">Within 30 days.</p>')}</div>
          <details class="collapsible"><summary class="collapsible-trigger">More</summary>
            <div class="collapsible-content"><p id="more">More text.</p></div></details>
          <a href="#refunds" id="to-refunds">Refunds</a> <a href="#more" id="to-more">More</a>`,
      {alpine: false, path: '/#plans'});
      const accordionItems = page.locator('.accordion-item');

      // Loading the page with a fragment in a closed section opened it.
      await expect(accordionItems.nth(0)).toHaveAttribute('open');
      await page.locator('#to-refunds').click();
      await expect(accordionItems.nth(1)).toHaveAttribute('open');
      await expect(accordionItems.nth(0), 'the group keeps one open').not.toHaveAttribute('open');
      await expect(page.locator('#refunds')).toBeInViewport();
      await page.locator('#to-more').click();
      await expect(page.locator('.collapsible')).toHaveAttribute('open');
      expect(messages).toEqual([]);
    });

test('htmx: a swap shows the sections the server opens, and history keeps the ones the user opened', async ({page}) => {
  // A name per render, as <sl:accordion> generates without a name (see the next test for why).
  let renders = 0;
  const accordion = (open: number) => {
    const name = `faq-${renders++}`;
    return `<div class="accordion" id="faq">${
        [0, 1, 2].map(i => item(name, `Question ${i}`, `Answer ${i}`, i === open ? 'open' : '')).join('')}</div>`;
  };
  const messages = await openFixture(page, accordion(0) + `
      <button type="button" id="refresh" hx-get="/faq" hx-target="#faq" hx-swap="outerHTML">Refresh</button>
      <a href="/next" id="next" hx-get="/next" hx-target="main" hx-push-url="true">Next</a>`, {
    htmx: true,
    alpine: false,
    routes: {
      '/faq': () => ({body: accordion(2)}),
      '/next': () => ({body: '<p id="next-page">Next page</p>'}),
    },
  });
  const items = page.locator('.accordion-item');

  await page.locator('#refresh').click();
  await expect(items.nth(2)).toHaveAttribute('open');
  await expect(items.nth(0)).not.toHaveAttribute('open');
  expect(await items.count()).toBe(3);

  await items.nth(1).locator('> summary').click();
  await expect(items.nth(1)).toHaveAttribute('open');
  await expect(items.nth(2)).not.toHaveAttribute('open');
  await page.locator('#next').click();
  await expect(page.locator('#next-page')).toBeVisible();
  await page.goBack();
  await expect(items.nth(1), 'back restores the section the user opened').toHaveAttribute('open');
  await expect(items.nth(2)).not.toHaveAttribute('open');
  // Still one group after the restore.
  await items.nth(0).locator('> summary').click();
  await expect(items.nth(1)).not.toHaveAttribute('open');
  expect(messages).toEqual([]);
});

test('htmx: a fixed name loses the open section of a swap (negative control for the generated name)', async ({page}) => {
  // htmx inserts the new markup before it removes the old; the browser closes an open details element inserted into a
  // group that already has one open.
  const accordion = (open: number) => `<div class="accordion" id="faq">${
      [0, 1, 2].map(i => item('faq', `Question ${i}`, `Answer ${i}`, i === open ? 'open' : '')).join('')}</div>`;
  await openFixture(page, accordion(0) + `
      <button type="button" id="refresh" hx-get="/faq" hx-target="#faq" hx-swap="outerHTML">Refresh</button>`, {
    htmx: true,
    alpine: false,
    routes: {'/faq': () => ({body: accordion(2)})},
  });

  await page.locator('#refresh').click();
  await expect(page.locator('.accordion-item').nth(0)).not.toHaveAttribute('open');
  await expect(page.locator('.accordion-item').nth(2)).not.toHaveAttribute('open');
});
