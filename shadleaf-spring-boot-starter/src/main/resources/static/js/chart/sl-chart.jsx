/*
  <sl-chart>, the element sl:chart renders: reads the data block and the part markers the server rendered, and mounts
  the Recharts tree (chart.jsx) into a root of its own. The chart type follows the series parts: only bars a BarChart,
  only lines a LineChart, only areas an AreaChart, a mix a ComposedChart. Loaded by loader.js when the first chart
  comes near the viewport.

  - Connecting mounts, disconnecting unmounts, so an htmx swap that brings a chart or takes it away needs nothing more.
  - Before htmx saves a page for its history, every chart is unmounted and its skeleton put back, and Recharts' text
    measuring span leaves the body: the snapshot would otherwise hold the inline styles Recharts sets, which a strict
    policy reports when the page is restored. The charts still on the page mount again after it.
  - The --color-<key> properties are set on the root through CSSOM (a <style> element, as shadcn's ChartStyle, needs
    'unsafe-inline'); the root goes when the chart unmounts, so sl-chart never carries a style attribute.
  - data-ready hides the skeleton once the chart has drawn its first frame.
*/
import './preact-fixes.js';
import {useEffect} from 'react';
import {createRoot} from 'react-dom/client';
import {
  Area,
  AreaChart,
  Bar,
  BarChart,
  CartesianGrid,
  ComposedChart,
  Legend,
  Line,
  LineChart,
  Tooltip,
  XAxis,
  YAxis
} from 'recharts';
import {ChartContainer, ChartLegendContent, ChartTooltipContent} from './chart.jsx';
import {formatter} from './formats.js';

const SERIES = {'SL-CHART-BAR': BarChart, 'SL-CHART-LINE': LineChart, 'SL-CHART-AREA': AreaChart};
// The chart tokens; a series without a color takes the next one, by its position among the series.
const CHART_COLORS = 5;
// The dash pattern of a dashed line, in pixels.
const DASH = '5 5';
const MEASUREMENT_SPAN = 'recharts_measurement_span';

function bool(marker, name, fallback) {
  const value = marker.getAttribute(name);
  return value == null ? fallback : value !== 'false';
}

function num(marker, name, fallback) {
  const value = marker.getAttribute(name);
  return value == null || value === '' ? fallback : Number(value);
}

/** "4" for every corner, or "4,4,0,0": top-left, top-right, bottom-right, bottom-left. */
function radius(marker) {
  const value = marker.getAttribute('radius');
  if (value == null) {
    return undefined;
  }
  const corners = value.split(',').map(Number);
  return corners.length === 1 ? corners[0] : corners;
}

/** Recharts' curve types are camel case: step-before is stepBefore. */
function curve(marker, fallback) {
  const value = marker.getAttribute('curve') ?? fallback;
  return value.replace(/-([a-z])/g, (_, letter) => letter.toUpperCase());
}

function format(marker, name, context) {
  return formatter(marker.getAttribute(name), {...context, currency: marker.getAttribute('currency') ?? undefined});
}

function part(marker, index, context) {
  const key = marker.getAttribute('key');
  const color = `var(--color-${key})`;
  switch (marker.tagName) {
    case 'SL-CHART-GRID':
      return <CartesianGrid key={index} vertical={bool(marker, 'vertical', false)}
                            horizontal={bool(marker, 'horizontal', true)}/>;
    case 'SL-CHART-X-AXIS':
      return <XAxis key={index} dataKey={key ?? undefined} tickLine={false} axisLine={false}
                    tickMargin={num(marker, 'tick-margin', 8)} minTickGap={num(marker, 'min-tick-gap', 5)}
                    tickFormatter={format(marker, 'format', context)} reversed={context.rtl}/>;
    case 'SL-CHART-Y-AXIS':
      return <YAxis key={index} tickLine={false} axisLine={false} width="auto"
                    tickMargin={num(marker, 'tick-margin', 8)} tickCount={num(marker, 'tick-count', 5)}
                    tickFormatter={format(marker, 'format', context)} orientation={context.rtl ? 'right' : 'left'}/>;
    case 'SL-CHART-TOOLTIP':
      return <Tooltip key={index} cursor={bool(marker, 'cursor', true)}
                      content={<ChartTooltipContent indicator={marker.getAttribute('indicator') ?? 'dot'}
                                                    hideLabel={bool(marker, 'hide-label', false)}
                                                    hideIndicator={bool(marker, 'hide-indicator', false)}
                                                    labelKey={marker.getAttribute('label-key') ?? undefined}
                                                    formatLabel={format(marker, 'label-format', context)}
                                                    formatValue={format(marker, 'value-format', context)}/>}/>;
    case 'SL-CHART-LEGEND':
      return <Legend key={index} verticalAlign={marker.getAttribute('vertical-align') ?? 'bottom'}
                     content={<ChartLegendContent/>}/>;
    case 'SL-CHART-BAR':
      return <Bar key={index} dataKey={key} stackId={marker.getAttribute('stack') ?? undefined} fill={color}
                  radius={radius(marker)}/>;
    case 'SL-CHART-LINE':
      return <Line key={index} dataKey={key} type={curve(marker, 'monotone')} stroke={color}
                   strokeWidth={num(marker, 'stroke-width', 2)}
                   strokeDasharray={bool(marker, 'dashed', false) ? DASH : undefined}
                   dot={bool(marker, 'dots', false) && {fill: color}}/>;
    case 'SL-CHART-AREA':
      return <Area key={index} dataKey={key} type={curve(marker, 'natural')}
                   stackId={marker.getAttribute('stack') ?? undefined} fill={color}
                   fillOpacity={num(marker, 'fill-opacity', 0.4)} stroke={color}
                   strokeDasharray={bool(marker, 'dashed', false) ? DASH : undefined}/>;
    default:
      return null;
  }
}

/**
 * A dashed line with join="actual" continues that series: its number at the last row where "actual" has one (before
 * its own first number) is filled in, so the dashed line starts there. Returns copies of the rows, and for each such
 * series the row it was filled in, which the tooltip leaves the series out of.
 */
function joinRows(rows, series) {
  const joins = new Map();
  const markers = series.filter(marker => marker.hasAttribute('join') && bool(marker, 'dashed', false));
  if (!markers.length) {
    return {data: rows, joins};
  }
  const data = rows.map(row => ({...row}));
  for (const marker of markers) {
    const key = marker.getAttribute('key');
    const from = marker.getAttribute('join');
    const first = data.findIndex(row => row[key] != null);
    for (let index = first - 1; first > 0 && index >= 0; index--) {
      if (data[index][from] != null) {
        data[index][key] = data[index][from];
        joins.set(key, data[index]);
        break;
      }
    }
  }
  return {data, joins};
}

/** Calls onReady once the chart has drawn its first frame. */
function Ready({onReady, children}) {
  useEffect(() => {
    const frame = requestAnimationFrame(onReady);
    return () => cancelAnimationFrame(frame);
  }, []);
  return children;
}

class SlChart extends HTMLElement {
  root = null;
  container = null;

  connectedCallback() {
    this.mount();
  }

  disconnectedCallback() {
    this.unmount();
  }

  mount() {
    const json = this.querySelector(':scope > script[type="application/json"]');
    if (!json || this.root) {
      return;
    }
    const rows = JSON.parse(json.textContent);
    const markers = Array.from(this.children).filter(element => element.tagName.startsWith('SL-CHART-'));
    const series = markers.filter(marker => marker.tagName in SERIES);

    // A history restore brings back the markup of a mounted chart: start again from a fresh root.
    this.querySelector(':scope > .chart-root')?.remove();
    this.removeAttribute('data-ready');
    this.container = document.createElement('div');
    this.container.className = 'chart-root';

    const {data, joins} = joinRows(rows, series);

    // shadcn's ChartConfig, from the series parts. No prototype, so a key such as "constructor" is no entry.
    const config = Object.create(null);
    series.forEach((marker, index) => {
      const key = marker.getAttribute('key');
      const light = marker.getAttribute('color') ?? `var(--chart-${index % CHART_COLORS + 1})`;
      const dark = marker.getAttribute('color-dark');
      config[key] = {label: marker.getAttribute('label') ?? key, dashed: bool(marker, 'dashed', false),
        joinRow: joins.get(key)};
      this.container.style.setProperty(`--color-${key}`, dark ? `light-dark(${light}, ${dark})` : light);
    });
    this.append(this.container);

    const types = new Set(series.map(marker => marker.tagName));
    const Chart = types.size === 1 ? SERIES[[...types][0]] : ComposedChart;
    const marginX = num(this, 'margin', undefined);
    const context = {
      locale: this.closest('[lang]')?.lang || undefined,
      timeZone: this.getAttribute('time-zone') ?? undefined,
      rtl: getComputedStyle(this).direction === 'rtl'
    };

    this.root = createRoot(this.container);
    this.root.render(
      <Ready onReady={() => this.setAttribute('data-ready', 'true')}>
        <ChartContainer config={config}>
          {/* aria-label rather than Recharts' title: a browser shows an svg's <title> as a tooltip of its own. */}
          <Chart accessibilityLayer data={data} aria-label={this.getAttribute('label') ?? undefined}
                 margin={marginX == null ? undefined : {left: marginX, right: marginX}}>
            {markers.map((marker, index) => part(marker, index, context))}
          </Chart>
        </ChartContainer>
      </Ready>
    );
  }

  unmount() {
    this.root?.unmount();
    this.root = null;
    this.container?.remove();
    this.container = null;
    this.removeAttribute('data-ready');
  }
}

if (!customElements.get('sl-chart')) {
  customElements.define('sl-chart', SlChart);
  document.addEventListener('htmx:beforeHistorySave', () => {
    const mounted = Array.from(document.querySelectorAll('sl-chart')).filter(chart => chart.root);
    mounted.forEach(chart => chart.unmount());
    // Recharts measures text in a span with inline styles of its own, appended to the body; it makes a new one.
    document.getElementById(MEASUREMENT_SPAN)?.remove();
    // The snapshot is taken right after this event; the swap that follows takes most charts away.
    setTimeout(() => mounted.filter(chart => chart.isConnected).forEach(chart => chart.mount()));
  });
}
