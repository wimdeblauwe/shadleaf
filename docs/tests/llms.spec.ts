import {expect, test} from '@playwright/test';
import components from '../src/generated/components.json' with {type: 'json'};

// The docs for coding agents: llms.txt, its one-file sets and a Markdown copy of every page (src/lib/markdown.ts).
// Links in them are absolute (the published site's address); the tests follow their paths on the preview server.

const path = (url: string) => new URL(url).pathname;

/** The text outside fenced code blocks, where MDX left behind would show. */
function prose(markdown: string): string {
  let fence: string | null = null;
  return markdown.split('\n').filter(line => {
    const match = line.match(/^\s*>?\s*(`{3,}|~{3,})/);
    if (fence) {
      if (match && match[1].startsWith(fence)) fence = null;
      return false;
    }
    if (match) {
      fence = match[1];
      return false;
    }
    return true;
  }).join('\n');
}

test('llms.txt links a Markdown copy of every page in the sidebar', async ({page, request}) => {
  const llms = await (await request.get('/llms.txt')).text();
  const links = [...llms.matchAll(/^- \[[^\]]+\]\(([^)]+)\)/gm)].map(match => path(match[1]));

  await page.goto('/getting-started/');
  // The sidebar's own links; on a narrow screen it also holds the GitHub link.
  const sidebar = await page.locator('#starlight__sidebar a[href]').evaluateAll(anchors => anchors
      .map(anchor => new URL((anchor as HTMLAnchorElement).href))
      .filter(url => url.origin === location.origin)
      .map(url => url.pathname));
  const expected = sidebar.map(href => href === '/' ? '/index.md' : href.replace(/\/$/, '.md'));
  expect(links.filter(link => link.endsWith('.md'))).toEqual(expected);

  for (const link of links) {
    const response = await request.get(link);
    expect(response.status(), link).toBe(200);
  }
});

test('every page has a Markdown copy without MDX left in it, linked from its head', async ({page, request}) => {
  const llms = await (await request.get('/llms.txt')).text();
  const pages = [...llms.matchAll(/\]\(([^)]+\.md)\)/g)].map(match => path(match[1]));
  for (const markdownPath of pages) {
    const response = await request.get(markdownPath);
    expect(response.headers()['content-type'], markdownPath).toContain('text/markdown');
    const text = prose(await response.text());
    expect(text, markdownPath).not.toMatch(/<(ComponentPreview|LayoutCode|ComponentAttributes|CspHash|DependencySnippet|Steps)\b/);
    expect(text, markdownPath).not.toMatch(/^import\s|^:::|withBase|is missing from this build/m);
    // Links to other pages point at their Markdown copies, and those exist.
    for (const [, link] of text.matchAll(/\]\((https?:\/\/[^)\s#]+\.md)(?:#[^)]*)?\)/g)) {
      expect(pages, `${markdownPath} links ${link}`).toContain(path(link));
    }
  }

  await page.goto('/components/button/');
  const alternate = page.locator('link[rel="alternate"][type="text/markdown"]');
  await expect(alternate).toHaveAttribute('href', /\/components\/button\.md$/);
  await page.goto('/');
  await expect(alternate).toHaveAttribute('href', /\/index\.md$/);
});

test('the component set holds every component with every prop', async ({request}) => {
  const text = await (await request.get('/llms-components.txt')).text();
  for (const component of components.components) {
    const heading = `Attributes of \`<${component.tag}>\``;
    expect(text, component.tag).toContain(heading);
    const table = text.slice(text.indexOf(heading)).split('\n\n').slice(0, 3).join('\n\n');
    for (const prop of component.props) {
      const attribute = prop.type === 'object' ? `th:${prop.name}` : prop.name;
      expect(table, `${component.tag} ${attribute}`).toContain(`| \`${attribute}\` |`);
    }
  }
});

test('a component page shows the Thymeleaf source of its examples', async ({request}) => {
  const button = await (await request.get('/components/button.md')).text();
  expect(button).toMatch(/^# Button\n/);
  expect(button).toContain('**Example: Variants.**\n\n```html\n<sl:button>Default</sl:button>');
  expect(button).toContain('| `variant` | `default`, `outline`, `ghost`, `destructive`, `secondary`, `link` | `default` |');
});

test('the full file holds every page, with each page\'s headings one level down', async ({request}) => {
  const full = await (await request.get('/llms-full.txt')).text();
  const guides = await (await request.get('/llms-guides.txt')).text();
  expect(full).toMatch(/^# Shadleaf\n/);
  expect(full).toContain('\n## Button\n');
  expect(full).toContain('\n### Variants\n');
  expect(guides).toContain('\n## Using Shadleaf with coding agents\n');
  expect(guides).not.toContain('\n## Button\n');
});
