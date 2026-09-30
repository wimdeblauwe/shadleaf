import {expect, test} from '@playwright/test';
import AxeBuilder from '@axe-core/playwright';
import axe, {type Result} from 'axe-core';
import {combinations, openShowcase} from './showcase';

// axe-core over every preview scenario, in each skin, light and dark: contrast, names, roles, ARIA. It does not check
// that focus is visible; focus.spec.ts does.

function describe(violations: Result[]): string[] {
  return violations.map(violation =>
      `${violation.id} (${violation.impact}): ${violation.help}\n` +
      violation.nodes.map(node => `    ${node.target.join(' ')}: ${node.failureSummary}`).join('\n'));
}
for (const {skin, theme} of combinations) {
  test(`showcase has no axe violations: ${skin}, ${theme}`, async ({page}) => {
    await openShowcase(page, skin, theme);

    const results = await new AxeBuilder({page})
        .withTags(['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa', 'wcag22aa', 'best-practice'])
        .analyze();

    const summary = describe(results.violations);
    expect(summary, summary.join('\n')).toEqual([]);
  });

  // axe sees the resting state only. A hover background is a different colour, so check the text on it as well: a real
  // hover per control (styles from a hovered ancestor count too), then axe on that control alone. axe is injected once
  // and run in the page directly; an AxeBuilder per control injects and sets it up again every time (~100 ms each).
  test(`text keeps its contrast on hover: ${skin}, ${theme}`, async ({page}) => {
    await openShowcase(page, skin, theme);
    // evaluate, not addScriptTag: a page's Content-Security-Policy does not apply to it
    await page.evaluate(axe.source);
    // Every enabled link and button: only an interactive element has a hover state of its own. Those in a closed
    // dialog or popover cannot be hovered; dialog.spec.ts and popup.spec.ts check them open.
    const controls = page.locator(
        'main :is(a[href], button):not([disabled]):not([aria-disabled])'
        + ':not(dialog:not([open]) *):not([popover]:not(:popover-open) *)');
    const count = await controls.count();
    const failures: string[] = [];
    for (let i = 0; i < count; i++) {
      const control = controls.nth(i);
      await control.evaluate(element => element.setAttribute('data-hovered', ''));
      await control.hover();
      // window.axe, not the imported binding: the function runs in the page, where the import does not exist
      const violations = await page.evaluate(async () => {
        const results = await (window as unknown as {axe: typeof axe}).axe.run(
            {include: [['[data-hovered]']]}, {runOnly: {type: 'rule', values: ['color-contrast']}});
        return results.violations;
      });
      failures.push(...describe(violations));
      await control.evaluate(element => element.removeAttribute('data-hovered'));
    }
    expect(failures, failures.join('\n')).toEqual([]);
  });
}
