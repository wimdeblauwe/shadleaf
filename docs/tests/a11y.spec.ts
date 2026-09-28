import {expect, test} from '@playwright/test';
import AxeBuilder from '@axe-core/playwright';
import {combinations, openShowcase} from './showcase';

// axe-core over every preview scenario, in each skin, light and dark: contrast, names, roles, ARIA. It does not check
// that focus is visible; focus.spec.ts does.

function describe(violations: Awaited<ReturnType<AxeBuilder['analyze']>>['violations']): string[] {
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

  // axe sees the resting state only. A hover background is a different colour, so check the text on it as well.
  test(`text keeps its contrast on hover: ${skin}, ${theme}`, async ({page}) => {
    await openShowcase(page, skin, theme);
    const controls = page.locator('main .btn:not([disabled]):not([aria-disabled])');
    const failures: string[] = [];
    for (let i = 0; i < await controls.count(); i++) {
      const control = controls.nth(i);
      await control.evaluate(element => element.setAttribute('data-hovered', ''));
      await control.hover();
      const results = await new AxeBuilder({page}).include('[data-hovered]').withRules(['color-contrast']).analyze();
      failures.push(...describe(results.violations));
      await control.evaluate(element => element.removeAttribute('data-hovered'));
    }
    expect(failures, failures.join('\n')).toEqual([]);
  });
}
