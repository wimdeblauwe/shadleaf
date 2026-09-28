import {defineConfig} from 'vite';
import tailwindcss from '@tailwindcss/vite';
import path from 'path';

const projectDir = import.meta.dirname;

export default defineConfig({
  plugins: [tailwindcss()],
  root: path.join(projectDir, 'src/main/resources/static'),
  server: {
    port: 5174,
    strictPort: true,
    cors: true,
    origin: 'http://localhost:5174'
  },
  build: {
    manifest: true,
    rolldownOptions: {
      input: {
        'shadleaf-css': path.join(projectDir, 'src/main/resources/static/css/shadleaf.css'),
      }
    },
    // Served by Spring Boot under /shadleaf/** from the jar
    outDir: path.join(projectDir, 'target/classes/META-INF/resources/shadleaf'),
    emptyOutDir: true,
    copyPublicDir: false
  },
});
