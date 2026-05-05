// Same shape as test-initializr-parity.mjs but compares the live TeaVM
// deployment to the LOCAL ParparVM bundle (to measure changes that are
// not yet deployed).
import { chromium } from 'playwright';
import fs from 'node:fs';
import path from 'node:path';
import os from 'node:os';
import { spawn, execSync } from 'node:child_process';

const REPO_ROOT = '/Users/shai/dev/cn1';
const bundle = path.join(REPO_ROOT, 'scripts/initializr/javascript/target/initializr-javascript-port.zip');
const tmpDir = fs.mkdtempSync(path.join(os.tmpdir(), 'parity-local-'));
const bundleDir = path.join(tmpDir, 'bundle');
fs.mkdirSync(bundleDir);
execSync(`unzip -q "${bundle}" -d "${bundleDir}"`);
const serveDir = path.join(bundleDir, 'Initializr-js');
const port = 7888;
const server = spawn('python3', ['-m', 'http.server', String(port)], { cwd: serveDir, stdio: ['ignore','ignore','pipe'] });
await new Promise(r => setTimeout(r, 700));

const URL_TEAVM = 'https://www.codenameone.com/initializr/';
const URL_LOCAL = `http://127.0.0.1:${port}/`;

async function openSession(label, url) {
  const browser = await chromium.launch();
  const ctx = await browser.newContext({ viewport: { width: 1280, height: 900 } });
  const page = await ctx.newPage();
  const messages = [];
  page.on('console', msg => messages.push(`[${label}/${msg.type()}] ${msg.text()}`));
  page.on('pageerror', err => messages.push(`[${label}/pageerror] ${err.message}`));
  page.on('worker', worker => {
    worker.on('console', msg => messages.push(`[${label}/worker:${msg.type()}] ${msg.text()}`));
  });
  await page.goto(url, { waitUntil: 'domcontentloaded' });
  return { browser, ctx, page, messages };
}

async function appFrame(s) {
  for (const f of s.page.frames()) {
    try { if (await f.evaluate(() => !!document.querySelector('canvas'))) return f; } catch (e) {}
  }
  return s.page.mainFrame();
}

async function waitForReady(s, label, deadlineMs) {
  const start = Date.now();
  while (Date.now() - start < deadlineMs) {
    try {
      const frame = await appFrame(s);
      const ok = await frame.evaluate(() => !!window.cn1Started).catch(() => false);
      if (ok) return Date.now() - start;
    } catch (e) {}
    await new Promise(r => setTimeout(r, 200));
  }
  return -1;
}

console.log('Opening TeaVM (reference):', URL_TEAVM);
const tea = await openSession('TEAVM', URL_TEAVM);
console.log('Opening LOCAL bundle:', URL_LOCAL);
const local = await openSession('LOCAL', URL_LOCAL);

const teaMs = await waitForReady(tea, 'TEAVM', 30000);
const localMs = await waitForReady(local, 'LOCAL', 90000);

console.log('\n=== READY TIMINGS ===');
console.log('TeaVM:    ', teaMs >= 0 ? `${teaMs} ms` : 'NEVER READY');
console.log('LOCAL:    ', localMs >= 0 ? `${localMs} ms` : 'NEVER READY');

const localErrors = local.messages.filter(m => /error|exception|uncaught|missing/i.test(m));
console.log('\nLOCAL errors:', localErrors.length);
localErrors.slice(0, 10).forEach(e => console.log(' ', e));

await tea.browser.close();
await local.browser.close();
server.kill();
