// The components overview (src/content/docs/components/index.mdx): the sidebar's groups of component pages, each page
// with the title and description of its front matter. Read from the files' text, so the overview does not import the
// pages it lists (itself among them). Used by ComponentOverview.astro and by the Markdown copy (markdown.ts).
import {componentGroups} from '../component-groups.mjs';

export type OverviewPage = { page: string; title: string; description: string };
/** A group of the sidebar; `overview` is its page about the group as a whole, if it has one. */
export type OverviewGroup = { label: string; overview?: OverviewPage; pages: OverviewPage[] };

const sources = import.meta.glob<string>(
    ['../content/docs/components/*.mdx', '!../content/docs/components/index.mdx'],
    {eager: true, query: '?raw', import: 'default'});

function frontMatter(source: string, key: string): string {
  const block = source.match(/^---\n([\s\S]*?)\n---/)?.[1] ?? '';
  return block.match(new RegExp(`^${key}:\\s*(.*)$`, 'm'))?.[1].trim() ?? '';
}

export function componentOverview(): OverviewGroup[] {
  const read = (page: string): OverviewPage => {
    const source = sources[`../content/docs/components/${page}.mdx`] ?? '';
    return {page, title: frontMatter(source, 'title') || page, description: frontMatter(source, 'description')};
  };
  return componentGroups.map(group => ({
    label: group.label,
    overview: group.overview ? read(group.overview) : undefined,
    pages: group.pages.map(read),
  }));
}

/** A layout of the Layouts section, for its overview (src/content/docs/layouts/index.mdx). */
export type LayoutPage = { page: string; title: string; description: string; order: number };

const layoutSources = import.meta.glob<string>(
    ['../content/docs/layouts/*.mdx', '!../content/docs/layouts/index.mdx'],
    {eager: true, query: '?raw', import: 'default'});

/** The layouts in the sidebar's order (sidebar.order, then title). */
export function layoutOverview(): LayoutPage[] {
  return Object.entries(layoutSources).map(([path, source]) => {
    const page = path.replace(/^.*\/([^/]+)\.mdx$/, '$1');
    const block = source.match(/^---\n([\s\S]*?)\n---/)?.[1] ?? '';
    const order = Number(block.match(/^\s+order:\s*(\d+)\s*$/m)?.[1] ?? Infinity);
    return {page, title: frontMatter(source, 'title') || page, description: frontMatter(source, 'description'), order};
  }).sort((a, b) => a.order - b.order || a.title.localeCompare(b.title));
}
