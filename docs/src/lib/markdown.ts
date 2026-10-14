// The docs as Markdown, for coding agents: every page at <page>.md and the llms.txt files at the site's root
// (see src/pages/*.md.ts and src/pages/llms*.txt.ts). Built from the MDX source, not from the rendered HTML: the
// generated parts of a page become Markdown of their own here. A preview becomes its Thymeleaf source, an attribute
// table a Markdown table from the same registry export, so an agent gets exactly what a reader sees.
import {getCollection, type CollectionEntry} from 'astro:content';
import {components, previews, themeScript} from './generated';
import {componentOverview, layoutOverview} from './component-overview';

export type DocsEntry = CollectionEntry<'docs'>;

/** A part of the sidebar; Components has its groups too (src/component-groups.mjs), after the overview. */
export type Section = { title: string; entries: DocsEntry[]; groups?: { title: string; entries: DocsEntry[] }[] };

/** The pages of "Start here", in the sidebar's order (astro.config.mjs). */
const START_HERE = ['index', 'getting-started', 'coming-from-shadcn', 'ide-completion', 'coding-agents'];

/**
 * The docs in the sidebar's order: Start here, Components (the overview, then by group), Layouts and Guides (by
 * sidebar.order).
 */
export async function sections(): Promise<Section[]> {
  const entries = await getCollection('docs');
  const startHere = START_HERE
      .map(id => entries.find(entry => entry.id === id))
      .filter((entry): entry is DocsEntry => entry !== undefined);
  // A directory's pages, its overview (index.mdx, id = the directory) included.
  const inDirectory = (directory: string) => entries.filter(entry =>
      entry.id === directory || entry.id.startsWith(directory + '/'));
  const overview = entries.filter(entry => entry.id === 'components');
  const groups = componentOverview().map(group => ({
    title: group.label,
    entries: [...(group.overview ? [group.overview] : []), ...group.pages]
        .map(page => entries.find(entry => entry.id === `components/${page.page}`))
        .filter((entry): entry is DocsEntry => entry !== undefined),
  }));
  const byOrder = (directory: string) => inDirectory(directory).sort((a, b) =>
      (a.data.sidebar?.order ?? Infinity) - (b.data.sidebar?.order ?? Infinity) || a.data.title.localeCompare(b.data.title));
  return [
    {title: 'Start here', entries: startHere},
    {title: 'Components', entries: [...overview, ...groups.flatMap(group => group.entries)], groups},
    {title: 'Layouts', entries: byOrder('layouts')},
    {title: 'Guides', entries: byOrder('guides')},
  ];
}

/**
 * The site's root as an absolute URL, with a trailing slash: https://wimdeblauwe.github.io/shadleaf/current/ when
 * published, http://localhost:4321/ for a local build (astro.config.mjs).
 */
export function siteRoot(): URL {
  const base = import.meta.env.BASE_URL.replace(/\/?$/, '/');
  return new URL(base, import.meta.env.SITE);
}

/** The HTML page of an entry, e.g. .../components/button/ (the root page for "index"). */
export function pageUrl(entry: DocsEntry): string {
  return new URL(!entry.id || entry.id === 'index' ? '' : entry.id + '/', siteRoot()).href;
}

/** The Markdown copy of an entry, e.g. .../components/button.md (.../index.md for the root page). */
export function markdownUrl(entry: DocsEntry): string {
  // Starlight's route data has the root page's id as '', the collection as 'index'.
  return new URL((entry.id || 'index') + '.md', siteRoot()).href;
}

/** The library version these docs were built from, or '' before the first library build. */
export const version = previews.version;

/** The whole page as Markdown: title, description, the page's URL, then the body. */
export function pageMarkdown(entry: DocsEntry): string {
  const description = entry.data.description ? `\n> ${entry.data.description}\n` : '';
  return `# ${entry.data.title}\n${description}\nSource: ${pageUrl(entry)}\n\n${bodyMarkdown(entry).trim()}\n`;
}

/** Several pages as one Markdown file, for llms-full.txt and its parts. */
export function pagesMarkdown(title: string, summary: string, entries: DocsEntry[]): string {
  const versionLine = version ? `\n\nShadleaf ${version}.` : '';
  return `# ${title}\n\n> ${summary}${versionLine}\n\n`
      + entries.map(entry => demoteHeadings(pageMarkdown(entry))).join('\n---\n\n');
}

/** One level deeper for every heading outside code blocks, so a page's title sits under the file's. */
function demoteHeadings(markdown: string): string {
  let fence: string | null = null;
  return markdown.split('\n').map(line => {
    const fenceMatch = line.match(/^\s*(`{3,}|~{3,})/);
    if (fence) {
      if (fenceMatch && fenceMatch[1].startsWith(fence)) {
        fence = null;
      }
      return line;
    }
    if (fenceMatch) {
      fence = fenceMatch[1];
      return line;
    }
    return line.replace(/^(#{1,5}) /, '#$1 ');
  }).join('\n');
}

/**
 * The MDX body with everything that is not Markdown replaced: imports dropped, ComponentPreview, LayoutCode,
 * ComponentAttributes, CspHash and DependencySnippet written out, Starlight's asides as block quotes, and links to other pages pointed at
 * their Markdown copies. Code blocks are left alone (they hold JSX and imports as examples).
 */
export function bodyMarkdown(entry: DocsEntry): string {
  const lines = (entry.body ?? '').split('\n');
  const out: string[] = [];
  let fence: string | null = null;
  let aside = false;
  for (const line of lines) {
    const fenceMatch = line.match(/^\s*(`{3,}|~{3,})/);
    if (fence) {
      out.push(line);
      if (fenceMatch && fenceMatch[1].startsWith(fence)) {
        fence = null;
      }
      continue;
    }
    if (fenceMatch) {
      fence = fenceMatch[1];
      out.push(aside ? '> ' + line : line);
      continue;
    }
    if (/^import\s.+from\s+['"].+['"];?\s*$/.test(line)) {
      continue;
    }
    const asideStart = line.match(/^:::(note|tip|caution|danger)(?:\[(.*)\])?\s*$/);
    if (asideStart) {
      const kind = asideStart[1][0].toUpperCase() + asideStart[1].slice(1);
      out.push(`> **${kind}${asideStart[2] ? ': ' + asideStart[2] : ''}**`, '>');
      aside = true;
      continue;
    }
    if (aside && line.trim() === ':::') {
      aside = false;
      continue;
    }
    if (/^\s*<\/?Steps>\s*$/.test(line)) {
      continue;
    }
    const converted = convertLine(line, entry);
    out.push(aside ? (converted ? '> ' + converted : '>') : converted);
  }
  return out.join('\n').replace(/\n{3,}/g, '\n\n');
}

function convertLine(line: string, entry: DocsEntry): string {
  if (/^\s*<ComponentOverview\s*\/>\s*$/.test(line)) {
    return overviewMarkdown();
  }
  if (/^\s*<LayoutOverview\s*\/>\s*$/.test(line)) {
    return layoutOverview().map(layout => `- [${layout.title}](${new URL(`layouts/${layout.page}.md`, siteRoot()).href})`
        + (layout.description ? `: ${layout.description}` : '')).join('\n');
  }
  const preview = line.match(/^\s*<ComponentPreview\s+id="([^"]+)"\s*\/>\s*$/);
  if (preview) {
    return previewMarkdown(preview[1]);
  }
  const layoutCode = line.match(/^\s*<LayoutCode\s+id="([^"]+)"\s+part="(template|page|css)"\s*\/>\s*$/);
  if (layoutCode) {
    return layoutCodeMarkdown(layoutCode[1], layoutCode[2] as 'template' | 'page' | 'css');
  }
  const attributes = line.match(/^\s*<ComponentAttributes\s+component="([^"]+)"\s*\/>\s*$/);
  if (attributes) {
    return attributesMarkdown(attributes[1]);
  }
  if (/^\s*<CspHash\s*\/>\s*$/.test(line)) {
    return fenced('text', `script-src 'self' ${themeScript.cspHash || '<the hash of your Shadleaf version>'}`);
  }
  if (/^\s*<DependencySnippet\s*\/>\s*$/.test(line)) {
    return fenced('xml', `<dependency>
  <groupId>io.github.wimdeblauwe</groupId>
  <artifactId>shadleaf-spring-boot-starter</artifactId>
  <version>${version || '<version>'}</version>
</dependency>`);
  }
  return rewriteLinks(line, entry)
      .replace(/<a href=\{withBase\('([^']+)'\)\}[^>]*>(.*?)<\/a>/g,
          (_, path: string, text: string) => `[${text}](${new URL(path, siteRoot()).href})`);
}

/**
 * A preview: its title and description, then the Thymeleaf source an application writes. A layout's source is on its
 * page as whole files (LayoutCode), so its preview is the description alone.
 */
export function previewMarkdown(id: string): string {
  const scenario = previews.scenarios.find(s => s.id === id);
  if (!scenario) {
    return `(Example ${id} is missing from this build.)`;
  }
  const title = scenario.title ? `**Example: ${scenario.title}.**` : '**Example.**';
  const description = `${title}${scenario.description ? ' ' + scenario.description : ''}`;
  return scenario.layout ? description : `${description}\n\n` + fenced('html', scenario.source);
}

/** The components overview, as ComponentOverview.astro shows it: a heading and a list of pages per group. */
function overviewMarkdown(): string {
  const url = (page: string) => new URL(`components/${page}.md`, siteRoot()).href;
  return componentOverview().map(group => `## ${group.label}\n\n`
      + (group.overview ? `Start with the [overview](${url(group.overview.page)}): ${group.overview.description}\n\n` : '')
      + group.pages
      .map(page => `- [${page.title}](${url(page.page)})`
          + (page.description ? `: ${page.description}` : ''))
      .join('\n')).join('\n\n');
}

/** One of a layout's files, as LayoutCode.astro shows it. */
export function layoutCodeMarkdown(id: string, part: 'template' | 'page' | 'css'): string {
  const code = previews.scenarios.find(s => s.id === id && s.layout)?.[part];
  return code ? fenced(part === 'css' ? 'css' : 'html', code.trimEnd()) : `(The ${part} of ${id} is missing from this build.)`;
}

/** The attribute table of a component, as ComponentAttributes.astro shows it. */
export function attributesMarkdown(name: string): string {
  const component = components.find(c => c.name === name.replace(/^sl:/, ''));
  if (!component) {
    return `(The attribute table of sl:${name} is missing from this build.)`;
  }
  const heading = `Attributes of \`<${component.tag}>\`${component.description ? ': ' + component.description : ''}`;
  if (component.props.length === 0) {
    return `${heading}\n\n\`${component.tag}\` declares no props; every attribute passes through.`;
  }
  const cell = (text: string) => text.replace(/\|/g, '\\|').replace(/\n/g, ' ');
  const rows = component.props.map(prop => {
    const attribute = prop.type === 'object' ? `th:${prop.name}` : prop.name;
    const values = prop.type === 'enum' ? prop.values.map(v => `\`${v}\``).join(', ') : `*${prop.type}*`;
    const defaultValue = prop.required ? '**required**'
        : prop.defaultValue && (prop.type !== 'boolean' || prop.defaultValue === 'true') ? `\`${prop.defaultValue}\`` : '—';
    return `| \`${attribute}\` | ${values} | ${defaultValue} | ${cell(prop.description)} |`;
  });
  let note = `Every prop can also be set from an expression: \`th:${component.props[0].name}="\${...}"\`. `
      + 'Any other attribute passes through to the rendered element, and `class` is appended.';
  const accessibleName = component.accessibleName;
  if (accessibleName) {
    const when = accessibleName.prop
        ? ` when \`${accessibleName.prop}\` is ${accessibleName.values.map(v => `\`${v}\``).join(' or ')}` : '';
    note += ` An \`aria-label\` or \`aria-labelledby\` is required${when}; rendering fails without one.`;
  }
  return [heading, '', '| Attribute | Values | Default | Description |', '| --- | --- | --- | --- |', ...rows, '', note]
      .join('\n');
}

/** Relative links to other docs pages, e.g. ](../icon/#sizes), become absolute links to their Markdown copies. */
function rewriteLinks(line: string, entry: DocsEntry): string {
  const page = new URL(entry.id === 'index' ? '' : entry.id + '/', siteRoot());
  return line.replace(/\]\((\.{1,2}\/[^)\s]*)\)/g, (match, link: string) => {
    const target = new URL(link, page);
    if (!target.href.startsWith(siteRoot().href)) {
      return match;
    }
    const path = target.pathname.slice(siteRoot().pathname.length).replace(/\/$/, '');
    return `](${new URL((path || 'index') + '.md', siteRoot()).href}${target.hash})`;
  });
}

function fenced(lang: string, code: string): string {
  const longest = Math.max(2, ...(code.match(/`+/g) ?? []).map(run => run.length));
  const ticks = '`'.repeat(longest + 1);
  return `${ticks}${lang}\n${code}\n${ticks}`;
}
