// Runs the browser tests of one component: its own specs (tests/<component>*.spec.ts) and the showcase checks (a11y,
// focus, forced-colors) limited to its scenarios with SHADLEAF_TEST_COMPONENT (tests/showcase.ts). For working on one
// component; CI runs everything unfiltered.
//
//   pnpm run test:component -- calendar                          Chromium, the default skin
//   SHADLEAF_TEST_FULL=1 pnpm run test:component -- calendar     every skin, and Firefox for the specs it runs there
//   pnpm run test:component -- calendar --headed                 further arguments go to Playwright
//
// The component is its `component` in previews.json, which a family shares (select for select-item). Build first.

import {spawnSync} from 'node:child_process';
import {readdirSync} from 'node:fs';
import {join} from 'node:path';

const [component, ...playwrightArgs] = process.argv.slice(2).filter(arg => arg !== '--');
if (!component || component.startsWith('-')) {
  console.error('Usage: pnpm run test:component -- <component> [playwright arguments]');
  process.exit(2);
}

const testsDir = join(import.meta.dirname, '..', 'tests');
const ownSpecs = readdirSync(testsDir)
    .filter(file => file === `${component}.spec.ts` || file.startsWith(`${component}-`) && file.endsWith('.spec.ts'));
const specs = ['a11y.spec.ts', 'focus.spec.ts', 'forced-colors.spec.ts', ...ownSpecs].map(file => `tests/${file}`);
console.log(`[test:component] ${component}: ${specs.join(' ')}`);

// Not through `pnpm exec` (see playwright.config.ts); `pnpm run` has node_modules/.bin on the PATH.
const result = spawnSync('playwright', ['test', ...specs, ...playwrightArgs], {
  stdio: 'inherit',
  shell: process.platform === 'win32',
  env: {...process.env, SHADLEAF_TEST_COMPONENT: component},
});
if (result.error) {
  console.error(`[test:component] could not run \`playwright\`: ${result.error.message}`);
}
process.exit(result.status ?? 1);
