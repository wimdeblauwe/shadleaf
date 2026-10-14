// The component pages as one Markdown file: examples and attribute tables of every component.
import type {APIRoute} from 'astro';
import {pagesMarkdown, sections} from '../lib/markdown';

export const GET: APIRoute = async () => new Response(
    pagesMarkdown('Shadleaf components', 'Every Shadleaf component: examples as Thymeleaf source, and attribute tables.',
        (await sections()).find(section => section.title === 'Components')!.entries),
    {headers: {'Content-Type': 'text/plain; charset=utf-8'}});
