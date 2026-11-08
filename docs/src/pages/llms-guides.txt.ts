// The Start here pages, the layouts and the guides as one Markdown file: setup, conventions and patterns, without the
// components or the comparisons with other libraries.
import type {APIRoute} from 'astro';
import {pagesMarkdown, sections} from '../lib/markdown';

export const GET: APIRoute = async () => new Response(
    pagesMarkdown('Shadleaf guides', 'Getting started with Shadleaf, its conventions, the layouts and the guides.',
        (await sections()).filter(section => !['Components', 'Compared with'].includes(section.title)).flatMap(section => section.entries)),
    {headers: {'Content-Type': 'text/plain; charset=utf-8'}});
