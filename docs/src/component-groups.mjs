// @ts-check
// The groups of component pages in the docs' sidebar, in order. A page is a file name in src/content/docs/components/
// without its extension. A group may have an overview: a page first in the group (the sidebar calls it "Overview")
// that is about the group as a whole rather than one component. Read by astro.config.mjs (the sidebar, and checkComponentGroups when the build starts), the
// components overview page (src/content/docs/components/index.mdx) and the Markdown copies for coding agents
// (src/lib/markdown.ts). A new component page goes into one group here, or the build fails.

/** @type {{label: string, overview?: string, pages: string[]}[]} */
export const componentGroups = [
  {label: 'Actions', pages: ['button', 'theme-toggle']},
  {
    label: 'Forms',
    pages: ['field', 'form-errors', 'label', 'input', 'input-group', 'textarea', 'select', 'select-native', 'combobox',
      'checkbox', 'radio-group', 'switch', 'toggle', 'toggle-group', 'calendar', 'date-picker'],
  },
  {label: 'Overlays', pages: ['dialog', 'alert-dialog', 'sheet', 'popover', 'dropdown-menu', 'tooltip']},
  {label: 'Feedback', pages: ['alert', 'toast', 'skeleton', 'spinner', 'empty']},
  {label: 'Data', pages: ['table', 'pagination', 'query-params', 'month-view', 'chart']},
  {
    label: 'Application shell',
    overview: 'application-shell',
    pages: ['sidebar', 'site-header', 'user-menu', 'breadcrumb', 'skip-link'],
  },
  {label: 'Disclosure', pages: ['tabs', 'accordion', 'collapsible']},
  {label: 'Display', pages: ['card', 'badge', 'avatar', 'icon', 'kbd', 'separator']},
];

/**
 * Throws unless every component page is in exactly one group and every page a group names exists.
 *
 * @param {string[]} pages the component pages, without the overview (index)
 */
export function checkComponentGroups(pages) {
  const grouped = componentGroups.flatMap(group => group.overview ? [group.overview, ...group.pages] : group.pages);
  const problems = [
    ...pages.filter(page => !grouped.includes(page)).map(page => `${page} is in no group`),
    ...[...new Set(grouped.filter((page, index) => grouped.indexOf(page) !== index))]
        .map(page => `${page} is in more than one group`),
    ...grouped.filter(page => !pages.includes(page)).map(page => `${page} is in a group but has no page`),
  ];
  if (problems.length > 0) {
    throw new Error(`The component groups (src/component-groups.mjs) do not match src/content/docs/components/: `
        + problems.join('; ') + '.');
  }
}
