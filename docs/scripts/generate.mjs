// Regenerates everything the docs derive from the library, and syncs it in:
//
//   1. `mvn -Dtest=PreviewGeneratorTest test` in the starter renders the previews from
//      src/test/resources/previews/*.yaml and writes the registry export, web-types, the theme script's CSP hash and
//      the CSS bundles to target/generated-docs.
//   2. scripts/sync.mjs copies them into the docs project.
//
//   node scripts/generate.mjs           run once
//   node scripts/generate.mjs --watch   run once, then again whenever a component template or preview changes
//
// In watch mode a step's output is only shown when it fails.

import {spawnSync} from 'node:child_process';
import {existsSync, watch} from 'node:fs';
import {join} from 'node:path';

const docsDir = join(import.meta.dirname, '..');
const starterDir = join(docsDir, '..', 'shadleaf-spring-boot-starter');
const watchDirs = [
  join(starterDir, 'src', 'main', 'resources', 'templates', 'sl', 'components'),
  join(starterDir, 'src', 'main', 'resources', 'static', 'css'),
  join(starterDir, 'src', 'test', 'resources', 'previews'),
];
const isWatch = process.argv.includes('--watch');

function runStep(name, command, args, {cwd, quiet}) {
  const result = spawnSync(command, args, {cwd, shell: process.platform === 'win32', stdio: quiet ? 'pipe' : 'inherit',
    encoding: 'utf8'});
  const ok = !result.error && result.status === 0;
  if (!ok && quiet) {
    if (result.stdout) process.stdout.write(result.stdout);
    if (result.stderr) process.stderr.write(result.stderr);
  }
  if (result.error) {
    console.error(`[generate] could not run \`${command}\`: ${result.error.message}`);
  }
  return {name, ok};
}

function generate({quiet}) {
  console.log('\n[generate] rendering the previews and syncing them into the docs…');
  const steps = [
    runStep('Render previews (mvn)', 'mvn',
        ['-B', '-q', '-Dtest=PreviewGeneratorTest', '-Dsurefire.failIfNoSpecifiedTests=false', 'test'],
        {cwd: starterDir, quiet}),
  ];
  steps.push(runStep('Sync into docs', process.execPath, [join(import.meta.dirname, 'sync.mjs')],
      {cwd: docsDir, quiet}));
  for (const step of steps) {
    console.log(`  ${step.ok ? '✓ OK    ' : '✗ FAILED'}  ${step.name}`);
  }
  return steps.every(step => step.ok);
}

const ok = generate({quiet: isWatch});

if (!isWatch) {
  process.exit(ok ? 0 : 1);
}

console.log('\n[generate] watching:');
watchDirs.forEach(dir => console.log(`  - ${dir}`));
let timer;
let running = false;
let pending = false;
const trigger = () => {
  if (running) {
    pending = true;
    return;
  }
  running = true;
  try {
    generate({quiet: true});
  } finally {
    running = false;
    if (pending) {
      pending = false;
      trigger();
    }
  }
};
for (const dir of watchDirs) {
  if (existsSync(dir)) {
    watch(dir, {recursive: true}, () => {
      clearTimeout(timer);
      timer = setTimeout(trigger, 300);
    });
  }
}
