import {defineConfig} from 'vite';
import tailwindcss from '@tailwindcss/vite';
import path from 'path';
import {generateCssEntries} from './scripts/css-entries.js';

const projectDir = import.meta.dirname;
const staticDir = path.join(projectDir, 'src/main/resources/static');

export default defineConfig({
  plugins: [tailwindcss()],
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
      // One entry per skin x asset variant, e.g. css/entries/shadleaf-default.embedded.css
      input: generateCssEntries(path.join(staticDir, 'css')),
      output: {
        // Vite names a CSS-only entry after its file name up to the first dot, which would give
        // shadleaf-default.css and shadleaf-default.embedded.css the same asset name.
        assetFileNames: asset => `assets/${path.basename(asset.originalFileNames[0] ?? asset.names[0], '.css')}-[hash][extname]`
      }
    },
    // Served by Spring Boot under /shadleaf/** from the jar
    outDir: path.join(projectDir, 'target/classes/META-INF/resources/shadleaf'),
    emptyOutDir: true,
    copyPublicDir: false,
    // Keep the bundles readable: they ship in a jar, not over a slow network, and the build tests read them.
    cssMinify: false
  },
});
