// The llms.txt index (https://llmstxt.org): every page with its description, linking the Markdown copies, plus the
// files holding the whole docs (llms-full.txt) or one part of them.
import type {APIRoute} from 'astro';
import {markdownUrl, sections, siteRoot, version, type DocsEntry} from '../lib/markdown';

export const GET: APIRoute = async () => {
  const list = (entries: DocsEntry[]) => entries
      .map(entry => `- [${entry.data.title}](${markdownUrl(entry)})${entry.data.description ? ': ' + entry.data.description : ''}`)
      .join('\n');
  // Components: the overview, then a list per group.
  const lists = (await sections()).map(section => `## ${section.title}\n\n` + (section.groups
      ? [list(section.entries.filter(entry => entry.id === 'components')),
        ...section.groups.map(group => `### ${group.title}\n\n${list(group.entries)}`)].filter(Boolean).join('\n\n')
      : list(section.entries)));
  const file = (name: string) => new URL(name, siteRoot()).href;
  const text = `# Shadleaf

> shadcn/ui for Thymeleaf, as a Spring Boot starter. Components are Thymeleaf tags with the \`sl:\` prefix
> (\`<sl:button variant="outline">\`), rendered on the server. Not React: no JSX, no Radix, no \`asChild\`.
${version ? `\nThese docs describe Shadleaf ${version}. The docs of other versions live next to this one, at /shadleaf/<version>/.\n` : ''}
Read "Using Shadleaf with coding agents" first: it lists the rules an agent most often gets wrong. Every component
page ends with its attribute table (props, allowed values, defaults); the examples are the Thymeleaf source to write.

${lists.join('\n\n')}

## Optional

- [All docs in one file](${file('llms-full.txt')}): every page above, in this order.
- [All component pages](${file('llms-components.txt')}): the Components section in one file.
- [All guides](${file('llms-guides.txt')}): the Start here, Layouts and Guides sections in one file.
`;
  return new Response(text, {headers: {'Content-Type': 'text/plain; charset=utf-8'}});
};
