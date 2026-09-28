import {defineConfig} from 'vite';
import path from 'path';
import springBoot from '@wim.deblauwe/vite-plugin-spring-boot';

const projectDir = import.meta.dirname;
const libraryTemplatesDir = path.resolve(projectDir, '../../shadleaf-spring-boot-starter/src/main/resources/templates');

export default defineConfig({
    plugins: [
        springBoot({
            fullCopyFilePaths: {
                // Don't let the Spring Boot plugin copy the watched library templates.
                // We read them directly using the `FileTemplateResolver` in the library.
                exclude: [path.join(libraryTemplatesDir, '**')]
            }
        }),
        watchLibraryTemplates()
    ],
    root: path.join(projectDir, './src/main/resources'),
    build: {
        manifest: true,
        rollupOptions: {
            input: [
                '/static/css/application.css',
                '/static/js/theme-toggle.js'
            ]
        },
        outDir: path.join(projectDir, `./target/classes/static`),
        copyPublicDir: false,
        emptyOutDir: true
    },
    server: {
        proxy: {
            // Proxy all backend requests to Spring Boot except for static assets
            '^/(?!static|assets|@|.*\\.(js|css|png|svg|jpg|jpeg|gif|ico|woff|woff2)$)': {
                target: 'http://localhost:8080',  // Proxy to Spring Boot backend
                changeOrigin: true,
                secure: false
            }
        },
        watch: {
            ignored: ['target/**']
        }
    }
});

function watchLibraryTemplates() {
    return {
        name: 'watch-shadleaf-library-templates',
        configureServer(server) {
            server.watcher.add(libraryTemplatesDir);
            const reload = (file) => {
                if (file.startsWith(libraryTemplatesDir)) {
                    server.ws.send({type: 'full-reload'});
                }
            };
            server.watcher.on('change', reload);
            server.watcher.on('add', reload);
            server.watcher.on('unlink', reload);
        }
    };
}
