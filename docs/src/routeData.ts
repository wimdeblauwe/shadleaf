// Adds <link rel="alternate" type="text/markdown"> to every docs page, pointing at its Markdown copy.
import {defineRouteMiddleware} from '@astrojs/starlight/route-data';
import {markdownUrl} from './lib/markdown';

export const onRequest = defineRouteMiddleware(context => {
  const route = context.locals.starlightRoute;
  route.head.push({tag: 'link', attrs: {rel: 'alternate', type: 'text/markdown', href: markdownUrl(route.entry)}});
});
