// Time when in the boot the worker actually issues each XHR, not just
// the duration. The headline ``request.dur=900ms`` for theme.res in
// _perf-trace.mjs almost certainly is *waiting time before the worker
// gets to issue the XHR* (parsing 7 MiB of translated_app.js) plus the
// XHR itself. This script logs the start offset (relative to navigation)
// for every fetch, so we can tell parse-blocked from network-blocked.
import { chromium } from 'playwright';
import fs from 'node:fs';
import path from 'node:path';
import os from 'node:os';
import { spawn, execSync } from 'node:child_process';

const REPO_ROOT = '/Users/shai/dev/cn1';
const bundle = path.join(REPO_ROOT, 'scripts/initializr/javascript/target/initializr-javascript-port.zip');
const tmpDir = fs.mkdtempSync(path.join(os.tmpdir(), 'pdet-'));
const bundleDir = path.join(tmpDir, 'bundle');
fs.mkdirSync(bundleDir);
execSync(`unzip -q "${bundle}" -d "${bundleDir}"`);
const serveDir = path.join(bundleDir, 'Initializr-js');
const port = 7000 + (process.pid % 999);
const server = spawn('python3', ['-c',
  'import sys, http.server, socketserver;'
  + 'from http.server import SimpleHTTPRequestHandler;'
  + 'srv = socketserver.ThreadingTCPServer(("", int(sys.argv[1])), SimpleHTTPRequestHandler);'
  + 'srv.daemon_threads = True;'
  + 'srv.serve_forever()',
  String(port),
], { cwd: serveDir, stdio: ['ignore', 'ignore', 'pipe'] });
await new Promise(r => setTimeout(r, 500));

const browser = await chromium.launch();
const page = await browser.newPage();
const events = [];
const t0 = Date.now();
page.on('request', r => {
  const u = r.url();
  if (u.startsWith(`http://127.0.0.1:${port}/`)) {
    events.push({ kind: 'req', t: Date.now() - t0, url: u.replace(`http://127.0.0.1:${port}/`, '') });
  }
});
page.on('requestfinished', r => {
  const u = r.url();
  if (u.startsWith(`http://127.0.0.1:${port}/`)) {
    events.push({ kind: 'fin', t: Date.now() - t0, url: u.replace(`http://127.0.0.1:${port}/`, '') });
  }
});

await page.goto(`http://127.0.0.1:${port}/`, { waitUntil: 'domcontentloaded' });
const tDom = Date.now() - t0;

let tReady = -1;
while (Date.now() - t0 < 30000) {
  try {
    const f = page.frames().find(fr => fr.url() !== 'about:blank') || page.mainFrame();
    const ok = await f.evaluate(() => !!window.cn1Started).catch(() => false);
    if (ok) { tReady = Date.now() - t0; break; }
  } catch (_e) {}
  await new Promise(r => setTimeout(r, 30));
}
await browser.close();
server.kill();

console.log(`DOMContentLoaded: ${tDom} ms`);
console.log(`cn1Started:       ${tReady} ms`);
console.log('\nFetch timeline (req=request issued, fin=response done):');
for (const e of events) {
  const sz = e.sz != null ? `${(e.sz / 1024).toFixed(1).padStart(8)} KB` : ''.padStart(11);
  console.log(`  ${String(e.t).padStart(5)} ms  ${e.kind}  ${sz}  ${e.url}`);
}
