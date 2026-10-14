// Every docs page as one Markdown file, in the sidebar's order.
import type {APIRoute} from 'astro';
import {pagesMarkdown, sections} from '../lib/markdown';

export const GET: APIRoute = async () => new Response(
    pagesMarkdown('Shadleaf', 'shadcn/ui for Thymeleaf, as a Spring Boot starter. The complete documentation.',
        (await sections()).flatMap(section => section.entries)),
    {headers: {'Content-Type': 'text/plain; charset=utf-8'}});
