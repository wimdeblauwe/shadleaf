// Converts the lucide-static package into the icon catalogue that <sl:icon> reads from the jar:
//
//   target/classes/shadleaf/icons/lucide.json          { version, icons: { name: innerSvg }, aliases: { alias: name } }
//   target/classes/shadleaf/icons/LICENSE-lucide.txt   lucide's ISC licence, including the Feather notice
//
// The catalogue is a classpath resource, not a web asset: it lives outside META-INF/resources, so it is never served.
// Run by `pnpm run build` before `vite build`.

import fs from 'node:fs';
import path from 'node:path';
import {createRequire} from 'node:module';

const projectDir = path.resolve(import.meta.dirname, '..');
const outDir = path.join(projectDir, 'target/classes/shadleaf/icons');

const require = createRequire(import.meta.url);
const packageDir = path.dirname(require.resolve('lucide-static/package.json'));
const {version} = JSON.parse(fs.readFileSync(path.join(packageDir, 'package.json'), 'utf8'));
const iconNodes = JSON.parse(fs.readFileSync(path.join(packageDir, 'icon-nodes.json'), 'utf8'));

const escapeAttribute = value => String(value)
    .replaceAll('&', '&amp;')
    .replaceAll('"', '&quot;')
    .replaceAll('<', '&lt;');

// [["path", {"d": "M3 6h18"}], ...] -> <path d="M3 6h18"/>...
const toMarkup = nodes => nodes
    .map(([tag, attributes]) => `<${tag}${Object.entries(attributes)
        .map(([name, value]) => ` ${name}="${escapeAttribute(value)}"`).join('')}/>`)
    .join('');

const icons = {};
for (const name of Object.keys(iconNodes).sort()) {
  icons[name] = toMarkup(iconNodes[name]);
}

// icons/*.svg also holds the aliases lucide keeps for renamed icons (loader-2 -> loader-circle). They are full copies,
// so an alias is found by comparing its markup with the canonical icons'.
const svgBody = file => fs.readFileSync(file, 'utf8')
    .replace(/<!--[\s\S]*?-->/g, '')
    .replace(/^[\s\S]*?<svg[^>]*>/, '')
    .replace(/<\/svg>[\s\S]*$/, '')
    .replace(/\s+/g, ' ')
    .trim();

const iconsDir = path.join(packageDir, 'icons');
const canonicalByBody = new Map();
for (const name of Object.keys(icons)) {
  const body = svgBody(path.join(iconsDir, `${name}.svg`));
  if (!canonicalByBody.has(body)) {
    canonicalByBody.set(body, name);
  }
}

const aliases = {};
const unresolved = [];
for (const file of fs.readdirSync(iconsDir).sort()) {
  const name = path.basename(file, '.svg');
  if (!file.endsWith('.svg') || icons[name]) {
    continue;
  }
  const canonical = canonicalByBody.get(svgBody(path.join(iconsDir, file)));
  if (canonical) {
    aliases[name] = canonical;
  } else {
    unresolved.push(name);
  }
}
if (unresolved.length > 0) {
  throw new Error(`lucide-static ${version}: no canonical icon matches the aliases ${unresolved.join(', ')}`);
}

fs.mkdirSync(outDir, {recursive: true});
fs.writeFileSync(path.join(outDir, 'lucide.json'), JSON.stringify({version, icons, aliases}));
fs.copyFileSync(path.join(packageDir, 'LICENSE'), path.join(outDir, 'LICENSE-lucide.txt'));

console.log(`lucide-static ${version}: ${Object.keys(icons).length} icons and ${Object.keys(aliases).length} aliases`
    + ` written to ${path.relative(projectDir, outDir)}`);
