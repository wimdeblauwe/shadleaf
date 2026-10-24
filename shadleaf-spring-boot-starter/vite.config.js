import {defineConfig} from 'vite';
import tailwindcss from '@tailwindcss/vite';
import fs from 'fs';
import path from 'path';
import {fileURLToPath} from 'url';
import {generateCssEntries} from './scripts/css-entries.js';

const projectDir = import.meta.dirname;
const staticDir = path.join(projectDir, 'src/main/resources/static');
const outDir = path.join(projectDir, 'target/classes/META-INF/resources/shadleaf');
// Read by ShadleafAssets (MANIFEST_LOCATION). Outside META-INF/resources, so it is never served.
const manifestTarget = path.join(projectDir, 'target/classes/shadleaf/vite-manifest.json');
// One per shadleaf.assets.alpine value (AlpineVariant); written by hand, as they do not vary per skin.
const jsEntries = Object.fromEntries(['shadleaf', 'shadleaf.alpine', 'shadleaf.alpine-csp']
    .map(name => [name, path.join(staticDir, 'js/entries', `${name}.js`)]));

/*
  sl:chart runs Recharts, a React library, on Preact (preact/compat): the chart chunk is some 50 KB (gzip) smaller than
  with React. Aliased to absolute paths, because under pnpm Recharts cannot resolve preact from its own directory.
  react-is stays itself (Recharts' isFragment).
*/
const resolveModule = specifier => fileURLToPath(import.meta.resolve(specifier));
const preactAliases = [
  {find: /^react-dom\/client$/, replacement: resolveModule('preact/compat/client')},
  {find: /^react-dom$/, replacement: resolveModule('preact/compat')},
  {find: /^react\/jsx-runtime$/, replacement: resolveModule('preact/jsx-runtime')},
  {find: /^react\/jsx-dev-runtime$/, replacement: resolveModule('preact/jsx-dev-runtime')},
  {find: /^react$/, replacement: resolveModule('preact/compat')}
];

/** Moves the manifest Vite writes into the served output directory to manifestTarget. */
function moveManifest() {
  return {
    name: 'shadleaf-move-manifest',
    apply: 'build',
    writeBundle() {
      const viteDir = path.join(outDir, '.vite');
      fs.mkdirSync(path.dirname(manifestTarget), {recursive: true});
      fs.renameSync(path.join(viteDir, 'manifest.json'), manifestTarget);
      fs.rmSync(viteDir, {recursive: true, force: true});
    }
  };
}

export default defineConfig({
  plugins: [tailwindcss(), moveManifest()],
  root: staticDir,
  resolve: {alias: preactAliases},
  server: {
    port: 5174,
    strictPort: true,
    cors: true,
    origin: 'http://localhost:5174'
  },
  build: {
    manifest: true,
    // The chart chunk is a dynamic import. Vite's preload helper would prefix it with / rather than the entry's own
    // location (/shadleaf/assets/), and the entries need no preloading: they import their shared chunk statically.
    modulePreload: false,
    rolldownOptions: {
      // One CSS entry per skin x asset variant, e.g. css/entries/shadleaf-vega.embedded.css, plus the JS entries
      input: {...generateCssEntries(path.join(staticDir, 'css')), ...jsEntries},
      output: {
        // Vite names a CSS-only entry after its file name up to the first dot, which would give
        // shadleaf-vega.css and shadleaf-vega.embedded.css the same asset name.
        assetFileNames: asset => {
          const name = asset.originalFileNames[0] ?? asset.names[0];
          return `assets/${path.basename(name, path.extname(name))}-[hash][extname]`;
        }
      }
    },
    // Served by Spring Boot under /shadleaf/** from the jar
    outDir,
    emptyOutDir: true,
    copyPublicDir: false,
    // Keep the bundles readable: they ship in a jar, not over a slow network, and the build tests read them.
    cssMinify: false
  },
  experimental: {
    // A font a stylesheet links (the sketch skin's) is found next to it, wherever the application serves /shadleaf/**.
    renderBuiltUrl: (filename, {hostType}) => hostType === 'css' ? {relative: true} : undefined
  },
});
