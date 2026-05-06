// Capture PARPAR-LIFECYCLE: console messages alongside fetch events
// to see what the worker is doing in the post-fetch CPU tail.
import { chromium } from 'playwright';
import fs from 'node:fs';
import path from 'node:path';
import os from 'node:os';
import { spawn, execSync } from 'node:child_process';

const REPO_ROOT = '/Users/shai/dev/cn1';
const bundle = path.join(REPO_ROOT, 'scripts/initializr/javascript/target/initializr-javascript-port.zip');
const tmpDir = fs.mkdtempSync(path.join(os.tmpdir(), 'plf-'));
const bundleDir = path.join(tmpDir, 'bundle');
fs.mkdirSync(bundleDir);
execSync(`unzip -q "${bundle}" -d "${bundleDir}"`);
const serveDir = path.join(bundleDir, 'Initializr-js');
const port = 7000 + (Math.floor(Date.now() / 1000) % 999);
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
    events.push({ kind: 'req', t: Date.now() - t0, label: u.replace(`http://127.0.0.1:${port}/`, '') });
  }
});
page.on('requestfinished', async r => {
  const u = r.url();
  if (u.startsWith(`http://127.0.0.1:${port}/`)) {
    let sz = 0;
    try { sz = (await (await r.response()).body()).length; } catch (_e) {}
    events.push({ kind: 'fin', t: Date.now() - t0, label: u.replace(`http://127.0.0.1:${port}/`, ''), sz });
  }
});
const captureConsole = (label, msg) => {
  if (msg.startsWith('PARPAR-LIFECYCLE:') || msg.startsWith('CN1JS:')) {
    events.push({ kind: 'log', t: Date.now() - t0, label: `[${label}] ${msg}` });
  }
};
page.on('console', m => captureConsole('main', m.text()));
page.on('worker', wkr => wkr.on('console', m => captureConsole('worker', m.text())));

await page.goto(`http://127.0.0.1:${port}/?cn1Perf=1`, { waitUntil: 'domcontentloaded' });
events.push({ kind: 'mark', t: Date.now() - t0, label: 'DOMContentLoaded' });

let tReady = -1;
while (Date.now() - t0 < 30000) {
  try {
    const f = page.frames().find(fr => fr.url() !== 'about:blank') || page.mainFrame();
    const ok = await f.evaluate(() => !!window.cn1Started).catch(() => false);
    if (ok) { tReady = Date.now() - t0; break; }
  } catch (_e) {}
  await new Promise(r => setTimeout(r, 30));
}
events.push({ kind: 'mark', t: tReady, label: 'cn1Started' });
await new Promise(r => setTimeout(r, 200));
await browser.close();
server.kill();

events.sort((a, b) => a.t - b.t);
let prev = 0;
for (const e of events) {
  const sz = e.sz != null ? `${(e.sz / 1024).toFixed(1).padStart(8)} KB` : ''.padStart(11);
  const dt = e.t - prev;
  console.log(`  ${String(e.t).padStart(5)} ms  +${String(dt).padStart(4)}  ${e.kind.padEnd(5)} ${sz}  ${e.label}`);
  prev = e.t;
}
