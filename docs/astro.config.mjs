// @ts-check
import {defineConfig} from 'astro/config';
import starlight from '@astrojs/starlight';

// The docs are published into per-version directories of the gh-pages branch (/current/, /0.1.0/), served as a
// GitHub project page under /shadleaf/. So `base` is the repository name plus that directory, set by the publish
// workflow through DOCS_BASE (e.g. /shadleaf/current). Local dev, preview and the Playwright tests use /.
// Every internal link and asset must go through import.meta.env.BASE_URL, never a hard-coded "/".
export default defineConfig({
  site: 'https://wimdeblauwe.github.io',
  base: process.env.DOCS_BASE ?? '/',
  integrations: [
    starlight({
      title: 'Shadleaf',
      description: 'shadcn/ui for Thymeleaf, as a Spring Boot starter.',
      customCss: ['./src/styles/docs.css'],
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
          ],
        },
        {label: 'Components', items: [{autogenerate: {directory: 'components'}}]},
        {label: 'Guides', items: [{autogenerate: {directory: 'guides'}}]},
      ],
    }),
  ],
});
