import {expect, test, type Locator, type Page} from '@playwright/test';
import AxeBuilder from '@axe-core/playwright';
import previews from '../src/generated/previews.json' with {type: 'json'};
import {combinations, keepScenariosWith, openShowcase} from './showcase';
import {openFixture} from './fixture';

// sl:chart: Recharts on Preact in <sl-chart>, loaded on demand by the loader every entry holds. On the showcase, in
// every skin and theme: every chart draws, takes the focus with a visible ring, opens its tooltip with the arrow keys,
// and passes axe at rest and with the tooltip open. The tooltip by hover, the named formats, time zones, colours,
// reduced motion and resizing. In fixture pages under a strict Content-Security-Policy: the csp build and the
// external entry, no chart chunk on a page without charts (nor before a chart nears the viewport), the page's
// language, right to left, and htmx (a swap in and out, history) without a CSP violation. Runs in Chromium and
// Firefox: the violation of a restored chart's inline styles showed in Firefox only.

const TAGS = ['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa', 'wcag22aa', 'best-practice'];
const CHART_CHUNK = /\/sl-chart-[\w-]+\.js$/;

const chartScenarios = previews.scenarios.filter(scenario => scenario.component === 'chart');

function html(id: string): string {
  const scenario = previews.scenarios.find(scenario => scenario.id === id);
  if (!scenario) {
    throw new Error(`no preview ${id}: run \`pnpm run generate\``);
  }
  return scenario.html;
}

/** Errors, warnings and CSP violations, as openFixture collects them. */
const unexpected = (messages: string[]) => messages;

async function drawn(chart: Locator) {
  await chart.scrollIntoViewIfNeeded();
  await expect(chart).toHaveAttribute('data-ready', 'true');
  await expect(chart.locator('.recharts-surface')).toBeVisible();
}

/**
 * Tabs onto the chart from a button put before it, which opens the tooltip on the first category, and moves it on with
 * the arrow keys.
 */
async function focusChart(page: Page, chart: Locator, arrows = 0) {
  await chart.evaluate(element => {
    const before = document.createElement('button');
    before.type = 'button';
    before.textContent = 'Before the chart';
    before.className = 'test-before-chart';
    element.before(before);
    before.focus();
  });
  await page.keyboard.press('Tab');
  await expect(chart.locator('.recharts-surface')).toBeFocused();
  for (let i = 0; i < arrows; i++) {
    await page.keyboard.press('ArrowRight');
  }
  await expect(chart.locator('.chart-tooltip-content')).toBeVisible();
}

async function removeFocusHelpers(page: Page) {
  await page.locator('.test-before-chart').evaluateAll(buttons => buttons.forEach(button => button.remove()));
}

function tooltip(chart: Locator) {
  return chart.locator('.chart-tooltip-content');
}

function ticks(chart: Locator, axis: 'x' | 'y') {
  return chart.locator(`.recharts-${axis}Axis-tick-labels .recharts-cartesian-axis-tick-value`).allTextContents();
}

for (const {skin, theme} of combinations) {
  test(`every chart draws, shows focus and passes axe with its tooltip: ${skin}, ${theme}`, async ({page}) => {
    await openShowcase(page, skin, theme);
    await keepScenariosWith(page, 'sl-chart');
    expect(chartScenarios.length, 'the showcase has charts').toBeGreaterThan(0);
    const failures: string[] = [];
    for (const scenario of chartScenarios) {
      const stage = `[data-scenario="${scenario.id}"]`;
      const chart = page.locator(`${stage} sl-chart`);
      await drawn(chart);
      const rest = await new AxeBuilder({page}).include(stage).withTags(TAGS).analyze();
      failures.push(...rest.violations.map(violation => `${scenario.id} at rest: ${violation.id}: ${violation.help}`));

      await focusChart(page, chart);
      const ring = await chart.locator('.recharts-surface').evaluate(surface => {
        const style = getComputedStyle(surface);
        return surface.matches(':focus-visible') && style.outlineStyle !== 'none' && parseFloat(style.outlineWidth) > 0;
      });
      if (!ring) {
        failures.push(`${scenario.id}: no visible focus ring`);
      }
      const open = await new AxeBuilder({page}).include(stage).withTags(TAGS).analyze();
      failures.push(...open.violations.map(violation => `${scenario.id} with the tooltip: ${violation.id}: ${
          violation.help}\n    ${violation.nodes.map(node => node.target.join(' ')).join('\n    ')}`));
      await removeFocusHelpers(page);
    }
    expect(failures, failures.join('\n')).toEqual([]);
  });
}

test('the chart is named by its label, and the arrow keys move the tooltip', async ({page}) => {
  await openShowcase(page, 'vega', 'light');
  const chart = page.locator('[data-scenario="chart--line-multiple"] sl-chart');
  await drawn(chart);
  await expect(chart.locator('.recharts-surface')).toHaveAttribute('role', 'application');
  await expect(chart.locator('.recharts-surface')).toHaveAttribute('aria-label', 'Visitors, January to June 2024');
  // No name in the svg's <title>, which the browser would show as a second, native tooltip.
  await expect(chart.locator('.recharts-surface > title')).toHaveText('');
  await expect(page.locator('[data-scenario="chart--line-multiple"]')
      .getByRole('application', {name: 'Visitors, January to June 2024'})).toHaveCount(1);

  await focusChart(page, chart);
  await expect(tooltip(chart).locator('.chart-tooltip-label')).toHaveText('January');
  await page.keyboard.press('ArrowRight');
  await expect(tooltip(chart).locator('.chart-tooltip-label')).toHaveText('February');
  await page.keyboard.press('ArrowRight');
  await expect(tooltip(chart).locator('.chart-tooltip-label')).toHaveText('March');
  await expect(tooltip(chart).locator('.chart-tooltip-item')).toHaveText([/Desktop\s*237/, /Mobile\s*120/]);
  await page.keyboard.press('ArrowLeft');
  await expect(tooltip(chart).locator('.chart-tooltip-label')).toHaveText('February');
});

test('a dashed series is drawn, listed and shown in the tooltip dashed', async ({page}) => {
  await openShowcase(page, 'vega', 'light');
  const chart = page.locator('[data-scenario="chart--line-forecast"] sl-chart');
  await drawn(chart);
  // The animation ends with the series' own dashes (none for the solid line).
  await expect.poll(() => chart.locator('.recharts-line-curve').evaluateAll(curves =>
      curves.map(curve => curve.getAttribute('stroke-dasharray')))).toEqual([null, '5 5']);
  await expect(chart.locator('.chart-legend-indicator[data-dashed]')).toHaveCount(1);
  await expect(chart.locator('.chart-legend-indicator[data-dashed]')).toHaveCSS('border-block-start-style', 'dashed');
  // July: only the forecast has a number.
  await focusChart(page, chart, 6);
  await expect(tooltip(chart).locator('.chart-tooltip-label')).toHaveText('July');
  await expect(tooltip(chart).locator('.chart-tooltip-indicator[data-dashed]')).toHaveCount(1);
  // June is where the forecast joins the actual line: the tooltip lists only Actual is listed.
  await page.keyboard.press('ArrowLeft');
  await expect(tooltip(chart).locator('.chart-tooltip-label')).toHaveText('June');
  await expect(tooltip(chart).locator('.chart-tooltip-name > span')).toHaveText(['Actual']);
});

test('hovering shows the tooltip with the series\' labels and values', async ({page}) => {
  await openShowcase(page, 'vega', 'light');
  const chart = page.locator('[data-scenario="chart--bar-multiple"] sl-chart');
  await drawn(chart);
  const box = (await chart.locator('.recharts-surface').boundingBox())!;
  // The first of six months.
  await page.mouse.move(box.x + box.width / 12, box.y + box.height / 2);
  await expect(tooltip(chart)).toBeVisible();
  await expect(tooltip(chart).locator('.chart-tooltip-label')).toHaveText('January');
  await expect(tooltip(chart).locator('.chart-tooltip-name > span')).toHaveText(['Desktop', 'Mobile']);
  await expect(tooltip(chart).locator('.chart-tooltip-value')).toHaveText(['186', '80']);
  await expect(tooltip(chart).locator('.chart-tooltip-indicator').first()).toHaveAttribute('data-indicator', 'dashed');
  // hide-label in the stacked bars, the line indicator nested beside the label with one series.
  const stacked = page.locator('[data-scenario="chart--bar-stacked"] sl-chart');
  await drawn(stacked);
  await focusChart(page, stacked);
  await expect(tooltip(stacked).locator('.chart-tooltip-label')).toHaveCount(0);
  const area = page.locator('[data-scenario="chart--area-default"] sl-chart');
  await drawn(area);
  await focusChart(page, area);
  await expect(tooltip(area).locator('.chart-tooltip-name > .chart-tooltip-label')).toHaveText('January');
});

test('the legend shows every series with its colour, in the order of the series', async ({page}) => {
  await openShowcase(page, 'vega', 'light');
  const chart = page.locator('[data-scenario="chart--dynamic-series"] sl-chart');
  await drawn(chart);
  await expect(chart.locator('.chart-legend-item')).toHaveText(['Chrome', 'Safari', 'Firefox']);
  const colours = await chart.locator('.chart-legend-indicator').evaluateAll(indicators =>
      indicators.map(indicator => getComputedStyle(indicator).backgroundColor));
  const tokens = await page.evaluate(() => ['--chart-1', '--chart-2', '--chart-3'].map(token => {
    const probe = document.createElement('div');
    probe.style.backgroundColor = `var(${token})`;
    document.body.append(probe);
    const colour = getComputedStyle(probe).backgroundColor;
    probe.remove();
    return colour;
  }));
  expect(colours).toEqual(tokens);
});

test('named formats write the ticks and the tooltip with Intl', async ({page}) => {
  await openShowcase(page, 'vega', 'light');
  const chart = page.locator('[data-scenario="chart--formats"] sl-chart');
  await drawn(chart);
  expect((await ticks(chart, 'x')).slice(0, 2)).toEqual(['Apr 1', 'Apr 2']);
  expect(await ticks(chart, 'y')).toContain('20K');
  await focusChart(page, chart);
  await expect(tooltip(chart).locator('.chart-tooltip-label')).toHaveText('Apr 1, 2024');
  await expect(tooltip(chart).locator('.chart-tooltip-value')).toHaveText('€12,400.00');
  // The name and the value keep apart, also when they fill the tooltip.
  const [name, value] = await Promise.all([tooltip(chart).locator('.chart-tooltip-name > span').boundingBox(),
    tooltip(chart).locator('.chart-tooltip-value').boundingBox()]);
  expect(value!.x - (name!.x + name!.width)).toBeGreaterThanOrEqual(16);
  await page.keyboard.press('ArrowRight');
  await page.keyboard.press('ArrowRight');
  await expect(tooltip(chart).locator('.chart-tooltip-value')).toHaveText('€15,250.50');
});

test('instants are shown in the chart\'s time zone, whatever the browser\'s', async ({browser}) => {
  for (const timezoneId of ['America/Los_Angeles', 'Asia/Tokyo']) {
    const context = await browser.newContext({timezoneId, reducedMotion: 'reduce'});
    const page = await context.newPage();
    await openShowcase(page, 'vega', 'light');
    const chart = page.locator('[data-scenario="chart--time-zone"] sl-chart');
    await drawn(chart);
    // 06:00 UTC is 08:00 in Brussels (summer time).
    await focusChart(page, chart);
    await expect(tooltip(chart).locator('.chart-tooltip-label'), timezoneId).toHaveText('8:00 AM');
    expect(await ticks(chart, 'x'), timezoneId).toContain('9:00 AM');
    // And calendar dates never shift a day.
    const formats = page.locator('[data-scenario="chart--formats"] sl-chart');
    await drawn(formats);
    expect((await ticks(formats, 'x'))[0], timezoneId).toBe('Apr 1');
    await context.close();
  }
});

test('a series takes the next chart token, or its own colour, and color-dark in the dark theme', async ({page}) => {
  const messages = await openFixture(page, `<div id="charts">${html('chart--bar-multiple')}</div>`, {theme: true});
  const chart = page.locator('sl-chart');
  await drawn(chart);
  const root = chart.locator('.chart-root');
  expect(await root.evaluate(element => [element.style.getPropertyValue('--color-desktop'),
    element.style.getPropertyValue('--color-mobile')])).toEqual(['var(--chart-1)', 'var(--chart-2)']);

  await page.locator('#charts').evaluate(charts => {
    const copy = charts.querySelector('sl-chart')!.cloneNode(true) as HTMLElement;
    copy.querySelector('.chart-root')?.remove();
    copy.removeAttribute('data-ready');
    copy.querySelector('sl-chart-bar[key="desktop"]')!.setAttribute('color', 'rgb(255, 0, 0)');
    copy.querySelector('sl-chart-bar[key="desktop"]')!.setAttribute('color-dark', 'rgb(0, 0, 255)');
    charts.replaceChildren(copy);
  });
  await drawn(chart);
  const fill = () => chart.locator('.recharts-bar-rectangle path').first().evaluate(path => getComputedStyle(path).fill);
  expect(await fill()).toBe('rgb(255, 0, 0)');
  await page.evaluate(() => document.documentElement.classList.add('dark'));
  await expect.poll(fill).toBe('rgb(0, 0, 255)');
  expect(unexpected(messages)).toEqual([]);
});

test('with reduced motion the lines are drawn whole at once', async ({page}) => {
  // playwright.config.ts sets reducedMotion: 'reduce'.
  await openShowcase(page, 'vega', 'light');
  const chart = page.locator('[data-scenario="chart--line-multiple"] sl-chart');
  await drawn(chart);
  const dashes = await chart.locator('.recharts-line-curve').evaluateAll(curves =>
      curves.map(curve => curve.getAttribute('stroke-dasharray')));
  expect(dashes).toEqual([null, null]);
});

test('with motion the lines are drawn whole once the animation ends', async ({browser}) => {
  const context = await browser.newContext({reducedMotion: 'no-preference'});
  const page = await context.newPage();
  await openShowcase(page, 'vega', 'light');
  const chart = page.locator('[data-scenario="chart--line-multiple"] sl-chart');
  await drawn(chart);
  // Recharts draws a line by animating its dashes, and leaves none when it is done.
  await expect.poll(() => chart.locator('.recharts-line-curve').evaluateAll(curves => curves.map(curve => {
    const dashes = curve.getAttribute('stroke-dasharray');
    const length = (curve as SVGPathElement).getTotalLength();
    return dashes == null || parseFloat(dashes.split(/[ ,]+/)[0]) >= length - 1;
  })), {timeout: 5000}).toEqual([true, true]);
  await context.close();
});

test('the chart follows its container\'s width', async ({page}) => {
  const messages = await openFixture(page, `<div id="box" class="test-box">${html('chart--area-default')}</div>`,
      {stylesheet: '.test-box { inline-size: 600px; }'});
  const chart = page.locator('sl-chart');
  await drawn(chart);
  const width = () => chart.locator('.recharts-surface').evaluate(surface => surface.getBoundingClientRect().width);
  expect(Math.round(await width())).toBe(600);
  // Through CSSOM: the fixture's policy blocks inline style attributes.
  await page.locator('#box').evaluate(box => box.style.inlineSize = '300px');
  await expect.poll(async () => Math.round(await width())).toBe(300);
  expect(unexpected(messages)).toEqual([]);
});

test('under a strict policy with the csp build: draws, opens its tooltip, reports no violation', async ({page}) => {
  const messages = await openFixture(page, html('chart--formats') + html('chart--bar-stacked'));
  for (const chart of await page.locator('sl-chart').all()) {
    await drawn(chart);
  }
  await focusChart(page, page.locator('sl-chart').first());
  expect(unexpected(messages)).toEqual([]);
});

test('shadleaf.js draws the chart without any Alpine on the page', async ({page}) => {
  const messages = await openFixture(page, html('chart--line-multiple'), {alpine: 'external'});
  await drawn(page.locator('sl-chart'));
  expect(await page.evaluate(() => 'Alpine' in window)).toBe(false);
  expect(unexpected(messages)).toEqual([]);
});

test('a page without charts never loads the chart chunk, nor one whose chart is far below', async ({page}) => {
  const requests: string[] = [];
  page.on('request', request => requests.push(request.url()));
  await openFixture(page, '<p>No chart here.</p>', {routes: {
    '/far': () => ({body: `<div id="spacer" class="test-spacer"></div>${html('chart--area-default')}`}),
  }, stylesheet: '.test-spacer { block-size: 4000px; }'});
  await page.waitForTimeout(500);
  expect(requests.filter(url => CHART_CHUNK.test(url))).toEqual([]);

  await page.goto('http://app.test/far');
  await page.waitForFunction(() => 'Alpine' in window);
  await page.waitForTimeout(500);
  expect(requests.filter(url => CHART_CHUNK.test(url))).toEqual([]);
  await expect(page.locator('sl-chart')).not.toHaveAttribute('data-ready');
  await page.locator('sl-chart').scrollIntoViewIfNeeded();
  await drawn(page.locator('sl-chart'));
  expect(requests.filter(url => CHART_CHUNK.test(url))).toHaveLength(1);
});

test('formats follow the language of the chart\'s nearest lang attribute', async ({page}) => {
  const messages = await openFixture(page, `<div lang="nl-BE">${html('chart--formats')}</div>`);
  const chart = page.locator('sl-chart');
  await drawn(chart);
  expect((await ticks(chart, 'x'))[0]).toBe('1 apr');
  await focusChart(page, chart);
  await expect(tooltip(chart).locator('.chart-tooltip-label')).toHaveText('1 apr 2024');
  await expect(tooltip(chart).locator('.chart-tooltip-value')).toHaveText(/^€\s?12\.400,00$/);
  expect(unexpected(messages)).toEqual([]);
});

test('right to left: the categories run from the right, the value axis stands on the right', async ({page}) => {
  const messages = await openFixture(page, `<div dir="rtl">${html('chart--formats')}</div>`);
  const chart = page.locator('sl-chart');
  await drawn(chart);
  const x = (text: string) => chart.locator('.recharts-xAxis-tick-labels .recharts-cartesian-axis-tick-value',
      {hasText: new RegExp(`^${text}$`)}).evaluate(tick => tick.getBoundingClientRect().x);
  expect(await x('Apr 1')).toBeGreaterThan(await x('Apr 10'));
  const plot = (await chart.locator('.recharts-cartesian-grid').boundingBox())!;
  const yTick = (await chart.locator('.recharts-yAxis-tick-labels .recharts-cartesian-axis-tick-value').first()
      .boundingBox())!;
  expect(yTick.x).toBeGreaterThan(plot.x + plot.width - 1);
  // The arrow keys follow what you see: left moves on from the first category, at the right.
  await focusChart(page, chart);
  await expect(tooltip(chart).locator('.chart-tooltip-label')).toHaveText('Apr 1, 2024');
  await page.keyboard.press('ArrowLeft');
  await expect(tooltip(chart).locator('.chart-tooltip-label')).toHaveText('Apr 2, 2024');
  expect(unexpected(messages)).toEqual([]);
});

test('htmx: a swapped-in chart draws, a swapped-out one goes, and history restores it without a violation',
    async ({page}) => {
      const messages = await openFixture(page, `<div id="area"></div>
      <button id="load" hx-get="/chart" hx-target="#area">Load</button>
      <button id="clear" hx-get="/empty" hx-target="#area">Clear</button>
      <a href="/next" id="next" hx-get="/next" hx-target="main" hx-push-url="true">Next</a>`, {
        htmx: true,
        routes: {
          '/chart': () => ({body: html('chart--bar-stacked')}),
          '/empty': () => ({body: '<p id="empty">No chart</p>'}),
          '/next': () => ({body: '<p id="next-page">Next page</p>'}),
        },
      });
      await page.locator('#load').click();
      await drawn(page.locator('sl-chart'));
      await page.locator('#clear').click();
      await expect(page.locator('#empty')).toBeVisible();
      await expect(page.locator('sl-chart')).toHaveCount(0);

      await page.locator('#load').click();
      await drawn(page.locator('sl-chart'));
      await page.locator('#next').click();
      await expect(page.locator('#next-page')).toBeVisible();
      await page.goBack();
      await drawn(page.locator('sl-chart'));
      await expect(page.locator('sl-chart > .chart-root')).toHaveCount(1);
      await focusChart(page, page.locator('sl-chart'));
      expect(unexpected(messages)).toEqual([]);
    });

test('htmx: a chart outside the swapped area draws again after htmx saved the page', async ({page}) => {
  const messages = await openFixture(page, `${html('chart--area-default')}<div id="area"></div>
      <a href="/next" id="next" hx-get="/next" hx-target="#area" hx-push-url="true">Next</a>`, {
    htmx: true,
    routes: {'/next': () => ({body: '<p id="next-page">Next page</p>'})},
  });
  const chart = page.locator('sl-chart');
  await drawn(chart);
  await page.locator('#next').click();
  await expect(page.locator('#next-page')).toBeVisible();
  await drawn(chart);
  expect(unexpected(messages)).toEqual([]);
});
