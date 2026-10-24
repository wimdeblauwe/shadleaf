/*
  sl:chart's loader, in every entry (bundled, csp, external), whatever shadleaf.assets.alpine is: the chart is no
  Alpine component. It loads the chart chunk (sl-chart.jsx, with Recharts and Preact) when the first <sl-chart> comes
  within 200px of the viewport, so a page without charts, or with charts far down, loads nothing more until then.
  Charts that htmx (or a script) adds later are watched as well; once the chunk is there, the browser upgrades every
  <sl-chart> itself.
*/
const SELECTOR = 'sl-chart';

let intersections;
let mutations;

function load() {
  intersections.disconnect();
  mutations.disconnect();
  import('./sl-chart.jsx');
}

function observe(root) {
  if (root.nodeType !== Node.ELEMENT_NODE && root.nodeType !== Node.DOCUMENT_NODE) {
    return;
  }
  if (root.matches?.(SELECTOR)) {
    intersections.observe(root);
  }
  root.querySelectorAll(SELECTOR).forEach(chart => intersections.observe(chart));
}

export function watchCharts() {
  if (document.slCharts) {
    // A second Shadleaf script on the page (it logs why elsewhere); one loader is enough.
    return;
  }
  document.slCharts = true;
  intersections = new IntersectionObserver(entries => {
    if (entries.some(entry => entry.isIntersecting)) {
      load();
    }
  }, {rootMargin: '200px'});
  mutations = new MutationObserver(records => {
    for (const record of records) {
      record.addedNodes.forEach(observe);
    }
  });
  observe(document);
  mutations.observe(document.documentElement, {childList: true, subtree: true});
}
