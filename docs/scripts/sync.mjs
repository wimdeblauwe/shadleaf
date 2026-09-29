// Copies what the docs derive from the library into the docs project:
//
//   <starter>/target/generated-docs/previews.json        -> src/generated/previews.json
//   <starter>/target/generated-docs/components.json      -> src/generated/components.json
//   <starter>/target/generated-docs/theme-script.json    -> src/generated/theme-script.json
//   <starter>/target/generated-docs/shadleaf/            -> public/shadleaf/  (CSS per skin, JS per Alpine variant)
//   <starter>/target/generated-docs/web-types.json       -> public/shadleaf.web-types.json  (for download)
//
// All of it is written by PreviewGeneratorTest (run by `mvn install`, or `pnpm run generate`). Runs before
// `astro dev` and `astro build`. When an artifact is missing, as in a fresh clone, it writes an empty placeholder so
// the site still builds; the pages then say which command to run.

import {cpSync, existsSync, mkdirSync, rmSync, writeFileSync} from 'node:fs';
import {join} from 'node:path';

const docsDir = join(import.meta.dirname, '..');
const source = join(docsDir, '..', 'shadleaf-spring-boot-starter', 'target', 'generated-docs');
const generatedDir = join(docsDir, 'src', 'generated');
const publicDir = join(docsDir, 'public');

const placeholders = {
  'previews.json': {version: '', skins: [], scripts: {}, scenarios: []},
  'components.json': {version: '', components: []},
  'theme-script.json': {cspHash: '', script: ''},
};

mkdirSync(generatedDir, {recursive: true});
const available = existsSync(join(source, 'previews.json'));
if (!available) {
  console.warn(`[sync] ${source} has no previews.json. Run \`pnpm run generate\` (or \`mvn install\` in the\n` +
      `  repository root) to render the previews. Writing placeholders so the site still builds.`);
}

for (const [name, placeholder] of Object.entries(placeholders)) {
  const from = join(source, name);
  const to = join(generatedDir, name);
  if (available && existsSync(from)) {
    cpSync(from, to);
  } else if (!existsSync(to)) {
    writeFileSync(to, JSON.stringify(placeholder, null, 2) + '\n');
  }
}

if (available) {
  // Replace rather than merge, so bundles of an older build (other hashes) do not pile up.
  rmSync(join(publicDir, 'shadleaf'), {recursive: true, force: true});
  cpSync(join(source, 'shadleaf'), join(publicDir, 'shadleaf'), {recursive: true});
  cpSync(join(source, 'web-types.json'), join(publicDir, 'shadleaf.web-types.json'));
  console.log(`[sync] copied the generated docs artifacts from ${source}`);
}
