import {expect, type Page, test} from '@playwright/test';
import AxeBuilder from '@axe-core/playwright';
import axe, {type Result} from 'axe-core';
import {combinations, componentScenarios, keepScenarios, openShowcase, showcaseComponents, showcasePages, widths} from './showcase';

// axe-core over every preview scenario, in each skin, light and dark: contrast, names, roles, ARIA. It does not check
// that focus is visible; focus.spec.ts does. SHADLEAF_TEST_COMPONENT limits both checks to one component (showcase.ts).

// The hover pass is a test per component: in parallel, rather than one after the other in a single worker.
test.describe.configure({mode: 'parallel'});

// Violations a scenario shows on purpose: the rule, and the scenarios where it is accepted. The pinned-column and
// column-group tables are plain text and wider than the page, the case they are for; their container has no tabindex
// (table-scroll.spec.ts), so in WebKit it cannot be scrolled with the keyboard. The table page says so under "Wide
// tables".
const accepted: Record<string, string[]> = {
  'scrollable-region-focusable': ['table--sticky-first-column', 'table--column-groups'],
};

/** The violations without the nodes {@link accepted} allows, and without the violations left with no node. */
async function withoutAccepted(page: Page, violations: Result[]): Promise<Result[]> {
  const kept: Result[] = [];
  for (const violation of violations) {
    const scenarios = accepted[violation.id] ?? [];
    const nodes = [];
    for (const node of violation.nodes) {
      const inAccepted = await page.evaluate(([target, selector]) => !!selector && !!document.querySelector(target)?.closest(selector),
          [node.target.join(' '), scenarios.map(scenario => `[data-scenario="${scenario}"]`).join(', ')]);
      if (!inAccepted) {
        nodes.push(node);
      }
    }
    if (nodes.length > 0) {
      kept.push({...violation, nodes});
    }
  }
  return kept;
}

function describe(violations: Result[]): string[] {
  return violations.map(violation =>
      `${violation.id} (${violation.impact}): ${violation.help}\n` +
      violation.nodes.map(node => `    ${node.target.join(' ')}: ${node.failureSummary}`).join('\n'));
}
for (const {skin, theme} of combinations) {
  for (const width of widths) {
    for (const showcase of showcasePages) {
      test(`showcase has no axe violations: ${skin}, ${theme}, ${width.name}, ${showcase.name}`, async ({page}) => {
        await page.setViewportSize(width.viewport);
        await openShowcase(page, skin, theme, showcase.path);

        const builder = new AxeBuilder({page})
            .withTags(['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa', 'wcag22aa', 'best-practice']);
        componentScenarios.forEach(scenario => builder.include(scenario));
        const results = await builder.analyze();

        const summary = describe(await withoutAccepted(page, results.violations));
        expect(summary, summary.join('\n')).toEqual([]);
      });

      // axe sees the resting state only. A hover background is a different colour, so check the text on it as well: a real
      // hover per control (styles from a hovered ancestor count too), then axe on that control alone. axe is injected once
      // and run in the page directly; an AxeBuilder per control injects and sets it up again every time (~100 ms each).
      // A test per component, with the other scenarios taken off the page: over the whole showcase, a hover and an axe
      // run per control took minutes, and every axe run walks the whole page.
      for (const {name, scenarios} of showcaseComponents(showcase)) {
        test(`text keeps its contrast on hover: ${skin}, ${theme}, ${width.name}, ${showcase.name}, ${name}`, async ({page}) => {
          await page.setViewportSize(width.viewport);
          await openShowcase(page, skin, theme, showcase.path);
          await keepScenarios(page, scenarios);
          // evaluate, not addScriptTag: a page's Content-Security-Policy does not apply to it
          await page.evaluate(axe.source);
          // Every enabled link and button: only an interactive element has a hover state of its own. Those in a closed
          // dialog or popover cannot be hovered; dialog.spec.ts and popup.spec.ts check them open, as disclosure.spec.ts and
          // tabs.spec.ts check closed sections and inactive panels. The button of a customizable
          // select is part of the select, not a control of its own.
          // The shell scenario (the sidebar) follows the showcase region; on a desktop its popover is closed but shown in
          // the page, so a closed popover only counts when it is no sidebar.
          const controls = page.locator(
              ':is(.showcase, .sidebar-provider) :is(a[href], button):not([disabled]):not([aria-disabled])'
              + ':not(select > button)'
              + ':not(dialog:not([open]) *):not([popover]:not(:popover-open):not(.sidebar) *)'
              // Nor those in a closed details element (accordion, collapsible) or an inactive tab panel.
              + ':not(details:not([open]) > :not(summary) *):not([hidden] *)');
          const count = await controls.count();
          const failures: string[] = [];
          for (let i = 0; i < count; i++) {
            const control = controls.nth(i);
            // One round trip per control where possible: with some 150 controls per page, each one counts.
            const state = await control.evaluate(element => {
              // Nor those not shown at this width: the links of the closed sidebar panel on a phone.
              if (!element.checkVisibility()) {
                return 'hidden';
              }
              // A calendar's days look alike but for their state (today, selected, in a range, outside the month,
              // unavailable): one day per state and scenario, rather than some 40 per month.
              if (element.matches('.calendar-day-button')) {
                const seen = ((window as unknown as {slHoveredDays?: Set<string>}).slHoveredDays ??= new Set());
                const day = element as HTMLButtonElement;
                const look = [element.closest('[data-scenario]')?.getAttribute('data-scenario'), day.tabIndex,
                  ...Object.keys(day.dataset).filter(key => key !== 'date').sort()].join(' ');
                if (seen.has(look)) {
                  return 'seen';
                }
                seen.add(look);
              }
              element.setAttribute('data-hovered', '');
              // A skip link is only drawn while it has the focus.
              return element.matches('.skip-link') ? 'skip-link' : 'shown';
            });
            if (state === 'hidden' || state === 'seen') {
              continue;
            }
            if (state === 'skip-link') {
              await control.focus();
            }
            await control.hover();
            // window.axe, not the imported binding: the function runs in the page, where the import does not exist
            const violations = await page.evaluate(async () => {
              const results = await (window as unknown as {axe: typeof axe}).axe.run(
                  {include: [['[data-hovered]']]}, {runOnly: {type: 'rule', values: ['color-contrast']}});
              return results.violations;
            });
            failures.push(...describe(violations));
            await control.evaluate(element => {
              element.removeAttribute('data-hovered');
              (element as HTMLElement).blur();
            });
          }
          expect(failures, failures.join('\n')).toEqual([]);
        });
      }
    }
  }
}
