// @ts-check
import {defineConfig} from 'astro/config';
import starlight from '@astrojs/starlight';
import {readdirSync} from 'node:fs';
import {checkComponentGroups, componentGroups} from './src/component-groups.mjs';

// Every component page must be in exactly one group of the sidebar: an explicit list would otherwise forget a new one.
checkComponentGroups(readdirSync(new URL('./src/content/docs/components/', import.meta.url))
    .filter(file => /\.mdx?$/.test(file))
    .map(file => file.replace(/\.mdx?$/, ''))
    .filter(page => page !== 'index'));

// The docs are published into per-version directories of the gh-pages branch (/current/, /0.1.0/), served by GitHub
// Pages on the custom domain shadleaf.dev. So `base` is that directory, set by the publish workflow through DOCS_BASE
// (e.g. /current). Local dev, preview and the Playwright tests use /.
// Every internal link and asset must go through import.meta.env.BASE_URL, never a hard-coded "/".
// Without DOCS_BASE the site is the local dev or preview server (Astro's default port), so the absolute links in
// llms.txt and the Markdown copies of the pages work where the site is served.
const site = process.env.DOCS_BASE ? 'https://shadleaf.dev' : 'http://localhost:4321';
const base = process.env.DOCS_BASE ?? '/';

// The preview image of a shared link (public/social.png, written by scripts/logo.mjs). Starlight sets the other Open
// Graph and Twitter tags, but none for an image, which must be an absolute URL.
const socialImage = new URL(base.replace(/\/?$/, '/') + 'social.png', site).href;

export default defineConfig({
  site,
  base,
  integrations: [
    starlight({
      title: 'Shadleaf',
      description: 'UI components for Thymeleaf, as a Spring Boot starter.',
      customCss: ['./src/styles/docs.css'],
      head: [
        {tag: 'meta', attrs: {property: 'og:image', content: socialImage}},
        {tag: 'meta', attrs: {property: 'og:image:width', content: '1200'}},
        {tag: 'meta', attrs: {property: 'og:image:height', content: '630'}},
        {tag: 'meta', attrs: {property: 'og:image:alt', content: 'Shadleaf: UI components for Spring Boot and Thymeleaf. Themeable, rendered by Thymeleaf.'}},
        {tag: 'meta', attrs: {name: 'twitter:image', content: socialImage}},
      ],
      components: {SiteTitle: './src/components/SiteTitle.astro'},
      // Links each page's Markdown copy (src/pages/[...slug].md.ts), so an agent that fetched the HTML finds it.
      routeMiddleware: './src/routeData.ts',
      social: [{icon: 'github', label: 'GitHub', href: 'https://github.com/wimdeblauwe/shadleaf'}],
      editLink: {baseUrl: 'https://github.com/wimdeblauwe/shadleaf/edit/main/docs/'},
      sidebar: [
        {
          label: 'Start here',
          items: [
            {label: 'Introduction', slug: 'index'},
            'getting-started',
            'coming-from-shadcn',
            'ide-completion',
            'coding-agents',
          ],
        },
        {
          label: 'Components',
          items: [
            {label: 'Overview', slug: 'components'},
            ...componentGroups.map(group => ({
              label: group.label,
              items: [
                ...(group.overview ? [{label: 'Overview', slug: `components/${group.overview}`}] : []),
                ...group.pages.map(page => `components/${page}`),
              ],
            })),
          ],
        },
        {label: 'Layouts', items: [{autogenerate: {directory: 'layouts'}}]},
        {label: 'Guides', items: [{autogenerate: {directory: 'guides'}}]},
      ],
    }),
  ],
});
