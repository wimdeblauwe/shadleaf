import {defineConfig, devices} from '@playwright/test';

// Runs against the built site (pnpm run build first), served by `astro preview` at base /.
export default defineConfig({
  testDir: 'tests',
  forbidOnly: !!process.env.CI,
  reporter: process.env.CI ? [['list'], ['html', {open: 'never'}]] : 'list',
  use: {
    baseURL: 'http://localhost:4399',
    // Keeps the skins' motion-safe transitions from leaving a computed style half-way when it is read.
    contextOptions: {reducedMotion: 'reduce'},
  },
  projects: [
    {name: 'chromium', use: {...devices['Desktop Chrome']}},
    // Firefox has no customizable select (appearance: base-select), so sl:select shows slSelect's list box there; it
    // cannot animate a height to auto (interpolate-size), and was later with find-in-page opening details and
    // hidden="until-found", which the accordion and the tabs use. The toaster moves into a modal dialog on its toggle
    // and close events, and reads Alt+T by its key code. The theme toggle depends on the storage event between tabs, on
    // matchMedia change events and on the scripting media feature. Firefox draws nothing for a broken image with
    // alt="", where Chromium draws an icon, which the avatar hides. Firefox has no search event, so a live search
    // runs on the form's submit when Enter is pressed.
    {name: 'firefox', use: {...devices['Desktop Firefox']},
      testMatch: /(select|disclosure|tabs|toast|theme-toggle|avatar|table-htmx)\.spec\.ts/},
  ],
  webServer: {
    // --ignore-lock keeps it in the foreground: Astro moves preview to the background when it detects an AI agent.
    // Not through `pnpm exec`: pnpm 12 starts astro in a process group of its own, which survives Playwright's stop
    // and keeps the test run from exiting. `pnpm test` has node_modules/.bin on the PATH.
    command: 'astro preview --port 4399 --ignore-lock',
    url: 'http://localhost:4399/showcase/',
    reuseExistingServer: !process.env.CI,
    env: {DOCS_BASE: '/'},
  },
});
