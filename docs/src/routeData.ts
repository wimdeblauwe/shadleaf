// Adds <link rel="alternate" type="text/markdown"> to every docs page, pointing at its Markdown copy. Pages of their
// own (src/pages/, as the home page) are no docs entries and have no Markdown copy.
import {getEntry} from 'astro:content';
import {defineRouteMiddleware} from '@astrojs/starlight/route-data';
import {markdownUrl} from './lib/markdown';

export const onRequest = defineRouteMiddleware(async context => {
  const route = context.locals.starlightRoute;
  if (!(await getEntry('docs', route.entry.id || 'index'))) {
    return;
  }
  route.head.push({tag: 'link', attrs: {rel: 'alternate', type: 'text/markdown', href: markdownUrl(route.entry)}});
});
