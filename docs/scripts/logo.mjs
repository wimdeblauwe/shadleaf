// Writes the Shadleaf logo files (see branding/README.md):
//
//   branding/shadleaf-logo-{light,dark}.svg|png   the mark and the wordmark
//   branding/shadleaf-mark-{light,dark}.svg|png   the mark alone
//   docs/public/favicon.svg                       the mark, following the system's colour scheme
//   samples/*/src/main/resources/static/favicon.svg   the same, for the sample applications
//   branding/swatches/<hex>.svg                   a swatch for every colour in a table of branding/README.md
//   docs/public/social.png                        the preview image a link to the site shows when shared (Open Graph)
//
// GitHub shows no colour for a hex code in a Markdown file, so the script puts a swatch image before each one in the
// README's tables (and re-pads the tables). The README is the list of colours: add a row there and run the script.
//
// The wordmark is turned into outlines from Schibsted Grotesk (@fontsource), so the SVGs need no font: GitHub and
// most image viewers load none. The PNGs are rendered by Playwright's Chromium, on a transparent background.
// Run with `pnpm run logo` after changing a colour or the layout here; the docs header (src/components/SiteTitle.astro)
// and src/styles/docs.css hold the same colours and must be changed with them.

import opentype from 'opentype.js';
import {chromium} from '@playwright/test';
import {mkdirSync, readFileSync, readdirSync, rmSync, writeFileSync} from 'node:fs';
import {join} from 'node:path';
import {createRequire} from 'node:module';

const docsDir = join(import.meta.dirname, '..');
const brandingDir = join(docsDir, '..', 'branding');
const fontDir = join(createRequire(import.meta.url).resolve('@fontsource/schibsted-grotesk/package.json'), '..', 'files');

const colors = {
  light: {lit: '#0f6e66', shade: '#6fcfc3', shad: '#475569', leaf: '#0f172a'},
  dark: {lit: '#3bbfb1', shade: '#13665e', shad: '#94a3b8', leaf: '#f1f5f9'},
};

// The leaf on a 32×32 grid: the lit half above the midrib, the shaded half below it, and the stem.
const markPaths = c => `<path fill="${c.lit}" d="M5 27C5 14 14 5 27 5Z"/>
    <path fill="${c.shade}" d="M5 27L27 5C27 18 18 27 5 27Z"/>
    <path stroke="${c.lit}" stroke-width="2.25" stroke-linecap="round" d="M5.5 26.5L2.5 29.5"/>`;

// The lockup: a 44px mark, a 12px gap and "shad" (400) + "leaf" (800) at 36px, tracked -0.02em, with the cap height
// centred on the mark.
const regular = opentype.loadSync(join(fontDir, 'schibsted-grotesk-latin-400-normal.woff'));
const heavy = opentype.loadSync(join(fontDir, 'schibsted-grotesk-latin-800-normal.woff'));
const size = 36, tracking = -0.02 * size, mark = 44, gap = 12, pad = 2;
const baseline = pad + mark / 2 + regular.tables.os2.sCapHeight / regular.unitsPerEm * size / 2;

function outline(font, text, x) {
  let d = '';
  const glyphs = font.stringToGlyphs(text);
  glyphs.forEach((glyph, i) => {
    d += glyph.getPath(x, baseline, size).toPathData(2);
    x += glyph.advanceWidth / font.unitsPerEm * size + tracking;
    if (glyphs[i + 1]) x += font.getKerningValue(glyph, glyphs[i + 1]) / font.unitsPerEm * size;
  });
  return {d, x};
}

const shad = outline(regular, 'shad', pad + mark + gap);
const leaf = outline(heavy, 'leaf', shad.x);
const logoWidth = Math.ceil(leaf.x + pad), logoHeight = mark + 2 * pad;

const logo = c => `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 ${logoWidth} ${logoHeight}" width="${logoWidth}" height="${logoHeight}" role="img" aria-label="Shadleaf">
  <g transform="translate(${pad} ${pad}) scale(${mark / 32})">
    ${markPaths(c)}
  </g>
  <path fill="${c.shad}" d="${shad.d}"/>
  <path fill="${c.leaf}" d="${leaf.d}"/>
</svg>
`;

const markOnly = c => `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 32 32" width="32" height="32" role="img" aria-label="Shadleaf">
  ${markPaths(c).replaceAll('\n    ', '\n  ')}
</svg>
`;

const favicon = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 32 32">
  <style>
    .lit { fill: ${colors.light.lit}; }
    .stem { stroke: ${colors.light.lit}; }
    .shade { fill: ${colors.light.shade}; }
    @media (prefers-color-scheme: dark) {
      .lit { fill: ${colors.dark.lit}; }
      .stem { stroke: ${colors.dark.lit}; }
      .shade { fill: ${colors.dark.shade}; }
    }
  </style>
  <path class="lit" d="M5 27C5 14 14 5 27 5Z"/>
  <path class="shade" d="M5 27L27 5C27 18 18 27 5 27Z"/>
  <path class="stem" d="M5.5 26.5L2.5 29.5" stroke-width="2.25" stroke-linecap="round"/>
</svg>
`;

mkdirSync(brandingDir, {recursive: true});
const files = [];
for (const [theme, c] of Object.entries(colors)) {
  files.push({name: `shadleaf-logo-${theme}`, svg: logo(c), width: logoWidth * 4, height: logoHeight * 4});
  files.push({name: `shadleaf-mark-${theme}`, svg: markOnly(c), width: 512, height: 512});
}
for (const file of files) writeFileSync(join(brandingDir, `${file.name}.svg`), file.svg);
writeFileSync(join(docsDir, 'public', 'favicon.svg'), favicon);
const samplesDir = join(docsDir, '..', 'samples');
for (const sample of readdirSync(samplesDir).filter(name => name.startsWith('shadleaf-sample-'))) {
  writeFileSync(join(samplesDir, sample, 'src', 'main', 'resources', 'static', 'favicon.svg'), favicon);
}

// Swatches: a rounded square with a faint outline, so white and near-black still show on either GitHub theme.
// GitHub strips `style`, but keeps `align`: absmiddle centres the swatch on the text instead of the baseline.
const readmePath = join(brandingDir, 'README.md');
const swatchesDir = join(brandingDir, 'swatches');
const swatchImg = /<img src="swatches\/[0-9a-f]{6}\.svg"[^>]*> /g;
const hexCode = /`#([0-9a-fA-F]{6})`/g;
const swatch = hex => `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 16 16" width="16" height="16">
  <rect x="0.5" y="0.5" width="15" height="15" rx="3.5" fill="#${hex}" stroke="#8b949e" stroke-opacity="0.5"/>
</svg>
`;
const swatchColors = new Set();
const readme = formatTables(readFileSync(readmePath, 'utf8').split('\n').map(line => {
  if (!line.startsWith('|')) return line;
  return line.replace(swatchImg, '').replace(hexCode, (code, hex) => {
    swatchColors.add(hex.toLowerCase());
    return `<img src="swatches/${hex.toLowerCase()}.svg" width="16" height="16" alt="" align="absmiddle"> ${code}`;
  });
})).join('\n');
rmSync(swatchesDir, {recursive: true, force: true});
mkdirSync(swatchesDir);
for (const hex of swatchColors) writeFileSync(join(swatchesDir, `${hex}.svg`), swatch(hex));
writeFileSync(readmePath, readme);

// Pads every Markdown table's cells to its column widths.
function formatTables(lines) {
  const result = [];
  for (let i = 0; i < lines.length;) {
    if (!lines[i].startsWith('|')) {
      result.push(lines[i++]);
      continue;
    }
    const rows = [];
    while (i < lines.length && lines[i].startsWith('|')) rows.push(lines[i++].slice(1, -1).split('|').map(cell => cell.trim()));
    const isRule = row => row.every(cell => /^:?-+:?$/.test(cell));
    const widths = rows[0].map((_, c) => Math.max(...rows.filter(row => !isRule(row)).map(row => row[c].length)));
    for (const row of rows) {
      result.push('|' + row.map((cell, c) => isRule(row) ? '-'.repeat(widths[c] + 2) : ` ${cell.padEnd(widths[c])} `).join('|') + '|');
    }
  }
  return result;
}

// The social image: 1200×630, the size Open Graph, X, LinkedIn and Slack all crop least. The dark logo on the docs'
// dark background, the home page's title and tagline, and the leaf large on the right. Rendered at 2× for sharp text.
const fontFace = weight => `@font-face {
    font-family: 'Schibsted Grotesk';
    font-weight: ${weight};
    src: url(data:font/woff2;base64,${readFileSync(join(fontDir, `schibsted-grotesk-latin-${weight}-normal.woff2`)).toString('base64')}) format('woff2');
  }`;
const social = `<style>
  ${fontFace(400)}
  ${fontFace(800)}
  html, body { margin: 0; }
  body {
    box-sizing: border-box;
    width: 1200px;
    height: 630px;
    padding: 72px 80px;
    display: flex;
    flex-direction: column;
    position: relative;
    overflow: hidden;
    background: radial-gradient(circle at 88% 40%, #0c2725 0, #0f172a 62%);
    color: #f1f5f9;
    font-family: 'Schibsted Grotesk', sans-serif;
  }
  .logo { display: block; align-self: flex-start; height: 52px; width: ${logoWidth * 52 / logoHeight}px; }
  .mark { position: absolute; inset-inline-end: 64px; inset-block-start: 115px; width: 400px; height: 400px; }
  h1 {
    margin: auto 0 0;
    max-width: 700px;
    font-size: 72px;
    font-weight: 800;
    line-height: 1.02;
    letter-spacing: -0.035em;
  }
  p { margin: 24px 0 0; font-size: 34px; letter-spacing: -0.015em; color: #3bbfb1; }
  footer { display: flex; justify-content: space-between; margin-block-start: auto; font-size: 24px; color: #94a3b8; }
</style>
${logo(colors.dark).replace(/<svg /, '<svg class="logo" ')}
<svg class="mark" viewBox="0 0 32 32" aria-hidden="true">${markPaths(colors.dark)}</svg>
<h1>UI components for Spring Boot and Thymeleaf</h1>
<p>Themeable, rendered by Thymeleaf.</p>
<footer><span>A Spring Boot starter</span><span>shadleaf.dev</span></footer>`;

const browser = await chromium.launch();
try {
  for (const file of files) {
    const page = await browser.newPage({viewport: {width: file.width, height: file.height}});
    const svg = file.svg.replace(/width="\d+" height="\d+"/, `width="${file.width}" height="${file.height}"`);
    await page.setContent(`<style>html,body{margin:0;background:transparent}svg{display:block}</style>${svg}`);
    await page.screenshot({path: join(brandingDir, `${file.name}.png`), omitBackground: true});
    await page.close();
  }
  const page = await browser.newPage({viewport: {width: 1200, height: 630}, deviceScaleFactor: 2});
  await page.setContent(social);
  await page.evaluate(() => document.fonts.ready);
  await page.screenshot({path: join(docsDir, 'public', 'social.png')});
  await page.close();
} finally {
  await browser.close();
}
console.log(`Wrote ${files.length} SVGs, ${files.length} PNGs, the favicons, ${swatchColors.size} swatches and the social image.`);
