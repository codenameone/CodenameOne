// Boot-only test: serve the bundle, open the page, wait, check whether
// PARPAR-LIFECYCLE:main-thread-completed fires WITHOUT any user interaction.
import { chromium } from 'playwright';
import fs from 'node:fs';
import path from 'node:path';
import os from 'node:os';
import { spawn, execSync } from 'node:child_process';

const REPO_ROOT = '/Users/shai/dev/cn1';
const bundle = path.join(REPO_ROOT, 'scripts/initializr/javascript/target/initializr-javascript-port.zip');

const tmpDir = fs.mkdtempSync(path.join(os.tmpdir(), 'boot-only-'));
const bundleDir = path.join(tmpDir, 'bundle');
fs.mkdirSync(bundleDir);
execSync(`unzip -q "${bundle}" -d "${bundleDir}"`);

const serveDir = path.join(bundleDir, 'Initializr-js');
const port = 7799;
const server = spawn('python3', ['-m', 'http.server', String(port)], {
  cwd: serveDir,
  stdio: ['ignore', 'ignore', 'pipe']
});
await new Promise(r => setTimeout(r, 700));

const browser = await chromium.launch();
const ctx = await browser.newContext({ viewport: { width: 1280, height: 900 } });
const page = await ctx.newPage();

const messages = [];
page.on('console', msg => messages.push(`[${msg.type()}] ${msg.text()}`));
page.on('pageerror', err => messages.push(`[pageerror] ${err.message}`));
page.on('worker', worker => {
  worker.on('console', msg => messages.push(`[worker:${msg.type()}] ${msg.text()}`));
});

await page.goto(`http://127.0.0.1:${port}/`);
console.log('Page loaded. Waiting 30s without any interaction...');

await new Promise(r => setTimeout(r, 90000));

console.log('---SUMMARY---');
const completed = messages.filter(m => m.includes('main-thread-completed'));
const formChanged = messages.filter(m => m.includes('currentForm CHANGED'));
const lastCallback = messages.filter(m => m.includes('main-host-callback')).slice(-1)[0] || '(none)';
console.log('main-thread-completed events:', completed.length);
console.log('currentForm CHANGED events:', formChanged.length);
console.log('Last main-host-callback:', lastCallback);
console.log('Total messages:', messages.length);
const errors = messages.filter(m => m.includes('Exception') || m.includes('[error]') || m.includes('[pageerror]'));
console.log('Error messages:', errors.length);
errors.slice(0, 5).forEach(e => console.log(' ', e));

if (completed.length > 0) {
  console.log('--- BOOT COMPLETES NATURALLY ---');
} else {
  console.log('--- BOOT NEVER COMPLETES ---');
}
console.log('--- ALL MESSAGES ---');
fs.writeFileSync('/tmp/boot-all-messages.log', messages.join('\n'));
console.log('messages dumped to /tmp/boot-all-messages.log');

await browser.close();
server.kill();
