// Typed access to what scripts/sync.mjs copies in from the library build. The files always exist: sync writes empty
// placeholders when the library has not been built, and every component shows a hint for that case.
import previewsJson from '../generated/previews.json';
import componentsJson from '../generated/components.json';
import themeScriptJson from '../generated/theme-script.json';

export type Skin = { name: string; css: string };

export type Scenario = {
  id: string;
  component: string;
  title?: string;
  description?: string;
  source: string;
  html: string;
  normalizedHtml: string;
};

export type Prop = {
  name: string;
  type: 'string' | 'boolean' | 'number' | 'enum';
  defaultValue?: string;
  required?: boolean;
  values: string[];
  description: string;
};

export type Component = {
  name: string;
  tag: string;
  description: string;
  declared: boolean;
  props: Prop[];
  accessibleName?: { prop?: string; values: string[] };
};

/** The script of each shadleaf.assets.alpine value, relative to the site's base. */
export type Scripts = { bundled?: string; csp?: string; external?: string };

export const previews = previewsJson as { version: string; skins: Skin[]; scripts?: Scripts; scenarios: Scenario[] };
export const components = (componentsJson as { version: string; components: Component[] }).components;
export const themeScript = themeScriptJson as { cspHash: string; script: string };

/** The command that fills the generated files, for the hints shown when they are empty. */
export const GENERATE_COMMAND = 'pnpm run generate';

/** A path under the site's base, e.g. withBase('shadleaf/assets/x.css') -> /shadleaf/current/shadleaf/assets/x.css */
export function withBase(path: string): string {
  return import.meta.env.BASE_URL.replace(/\/$/, '') + '/' + path.replace(/^\//, '');
}

/** Splits text on markdown code spans: even indices are text, odd indices code. */
export function codeSpans(text: string): string[] {
  return text.split(/`([^`]+)`/g);
}
