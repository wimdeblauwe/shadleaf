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

// The docs are published into per-version directories of the gh-pages branch (/current/, /0.1.0/), served as a
// GitHub project page under /shadleaf/. So `base` is the repository name plus that directory, set by the publish
// workflow through DOCS_BASE (e.g. /shadleaf/current). Local dev, preview and the Playwright tests use /.
// Every internal link and asset must go through import.meta.env.BASE_URL, never a hard-coded "/".
// Without DOCS_BASE the site is the local dev or preview server (Astro's default port), so the absolute links in
// llms.txt and the Markdown copies of the pages work where the site is served.
export default defineConfig({
  site: process.env.DOCS_BASE ? 'https://wimdeblauwe.github.io' : 'http://localhost:4321',
  base: process.env.DOCS_BASE ?? '/',
  integrations: [
    starlight({
      title: 'Shadleaf',
      description: 'shadcn/ui for Thymeleaf, as a Spring Boot starter.',
      customCss: ['./src/styles/docs.css'],
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
