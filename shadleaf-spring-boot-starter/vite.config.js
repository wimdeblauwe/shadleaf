import {defineConfig} from 'vite';
import tailwindcss from '@tailwindcss/vite';
import fs from 'fs';
import path from 'path';
import {generateCssEntries} from './scripts/css-entries.js';

const projectDir = import.meta.dirname;
const staticDir = path.join(projectDir, 'src/main/resources/static');
const outDir = path.join(projectDir, 'target/classes/META-INF/resources/shadleaf');
// Read by ShadleafAssets (MANIFEST_LOCATION). Outside META-INF/resources, so it is never served.
const manifestTarget = path.join(projectDir, 'target/classes/shadleaf/vite-manifest.json');
// One per shadleaf.assets.alpine value (AlpineVariant); written by hand, as they do not vary per skin.
const jsEntries = Object.fromEntries(['shadleaf', 'shadleaf.alpine', 'shadleaf.alpine-csp']
    .map(name => [name, path.join(staticDir, 'js/entries', `${name}.js`)]));

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
  server: {
    port: 5174,
    strictPort: true,
    cors: true,
    origin: 'http://localhost:5174'
  },
  build: {
    manifest: true,
    rolldownOptions: {
      // One CSS entry per skin x asset variant, e.g. css/entries/shadleaf-vega.embedded.css, plus the JS entries
      input: {...generateCssEntries(path.join(staticDir, 'css')), ...jsEntries},
      output: {
        // Vite names a CSS-only entry after its file name up to the first dot, which would give
        // shadleaf-vega.css and shadleaf-vega.embedded.css the same asset name.
        assetFileNames: asset => `assets/${path.basename(asset.originalFileNames[0] ?? asset.names[0], '.css')}-[hash][extname]`
      }
    },
    // Served by Spring Boot under /shadleaf/** from the jar
    outDir,
    emptyOutDir: true,
    copyPublicDir: false,
    // Keep the bundles readable: they ship in a jar, not over a slow network, and the build tests read them.
    cssMinify: false
  },
});
