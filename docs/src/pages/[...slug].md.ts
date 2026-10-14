// Every docs page as Markdown next to its HTML: /components/button/ -> /components/button.md, / -> /index.md.
import type {APIRoute, GetStaticPaths} from 'astro';
import {getCollection} from 'astro:content';
import {pageMarkdown, type DocsEntry} from '../lib/markdown';

export const getStaticPaths: GetStaticPaths = async () =>
    (await getCollection('docs')).map(entry => ({params: {slug: entry.id}, props: {entry}}));

export const GET: APIRoute<{ entry: DocsEntry }> = ({props}) =>
    new Response(pageMarkdown(props.entry), {headers: {'Content-Type': 'text/markdown; charset=utf-8'}});
