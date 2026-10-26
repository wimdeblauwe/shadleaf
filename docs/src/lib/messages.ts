// The built-in messages (messages.json): the languages the library ships, for SupportedLanguages.astro, and the keys
// grouped by the component page they belong to, for MessageKeys.astro; markdown.ts writes both as Markdown. A key's
// component is its second segment (sl.<component>.…), a part's key goes to its family's page.
import {componentOverview} from './component-overview';
import {messages, type Message} from './generated';

export type MessageGroup = { page: string; title: string; linked: boolean; messages: Message[] };

const PAGES: Record<string, string> = {toaster: 'toast', 'pagination-size': 'pagination'};

// The country whose flag stands for a language, as a language code has none. A new language without one here shows no
// flag.
const FLAGS: Record<string, string> = {en: 'GB', nl: 'NL', fr: 'FR', de: 'DE'};

export type Language = { code: string; name: string; flag?: string };

/** The languages Shadleaf ships messages in, English first, each with its English name and a flag emoji. */
export function supportedLanguages(): Language[] {
  const names = new Intl.DisplayNames(['en'], {type: 'language'});
  return messages.languages.map(code => ({
    code,
    name: names.of(code) ?? code,
    flag: FLAGS[code] && String.fromCodePoint(...[...FLAGS[code]].map(letter => 0x1f1a5 + letter.charCodeAt(0))),
  }));
}

/** The groups in the order of messages.properties. */
export function messageGroups(): MessageGroup[] {
  const titles = new Map(componentOverview().flatMap(group => group.pages).map(page => [page.page, page.title]));
  const groups = new Map<string, MessageGroup>();
  for (const message of messages.messages) {
    const component = message.key.split('.')[1];
    const page = PAGES[component] ?? component;
    let group = groups.get(page);
    if (!group) {
      group = {page, title: titles.get(page) ?? page, linked: titles.has(page), messages: []};
      groups.set(page, group);
    }
    group.messages.push(message);
  }
  return [...groups.values()];
}
