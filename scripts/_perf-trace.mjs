// Profile the boot of the LOCAL ParparVM bundle and the TeaVM live
// reference. Reports: per-resource fetch durations, worker creation
// time, time-to-cn1Started, top JS evaluation chunks. Output is a
// table you can scan to spot the next perf lever.
import { chromium } from 'playwright';
import fs from 'node:fs';
import path from 'node:path';
import os from 'node:os';
import { spawn, execSync } from 'node:child_process';

const REPO_ROOT = '/Users/shai/dev/cn1';
const bundle = path.join(REPO_ROOT, 'scripts/initializr/javascript/target/initializr-javascript-port.zip');
const tmpDir = fs.mkdtempSync(path.join(os.tmpdir(), 'perf-'));
const bundleDir = path.join(tmpDir, 'bundle');
fs.mkdirSync(bundleDir);
execSync(`unzip -q "${bundle}" -d "${bundleDir}"`);
const serveDir = path.join(bundleDir, 'Initializr-js');
const port = 7892;
// ``-m http.server`` is single-threaded -- serving 7 preloads in
// parallel queues every fetch behind the previous one and inflates
// theme.res from ~50 ms to ~900 ms. ThreadingHTTPServer is the
// drop-in fix; matches what a real CDN looks like for parallel
// fetches.
const server = spawn('python3', ['-c',
  'import sys, http.server, socketserver;'
  + 'from http.server import SimpleHTTPRequestHandler;'
  + 'srv = socketserver.ThreadingTCPServer(("", int(sys.argv[1])), SimpleHTTPRequestHandler);'
  + 'srv.daemon_threads = True;'
  + 'srv.serve_forever()',
  String(port),
], { cwd: serveDir, stdio: ['ignore', 'ignore', 'pipe'] });
await new Promise(r => setTimeout(r, 500));

const URL_TEAVM = 'https://www.codenameone.com/initializr/';
const URL_LOCAL = `http://127.0.0.1:${port}/`;

async function profile(label, url, deadlineMs = 30000) {
  const browser = await chromium.launch();
  const ctx = await browser.newContext({ viewport: { width: 1280, height: 900 } });
  const page = await ctx.newPage();
  const requests = new Map();
  const fetched = [];
  page.on('request', r => requests.set(r.url(), { start: Date.now(), url: r.url() }));
  page.on('requestfinished', async r => {
    const rec = requests.get(r.url()); if (!rec) return;
    rec.end = Date.now();
    rec.dur = rec.end - rec.start;
    try {
      const resp = await r.response();
      rec.status = resp ? resp.status() : null;
      rec.size = resp ? Number((await resp.allHeaders())['content-length'] || 0) : 0;
      try {
        const body = await resp.body();
        rec.bodyBytes = body.length;
      } catch (_e) {}
    } catch (_e) {}
    fetched.push(rec);
  });

  const t0 = Date.now();
  await page.goto(url, { waitUntil: 'domcontentloaded' });
  const tDom = Date.now() - t0;

  // Poll for cn1Started
  let tReady = -1;
  while (Date.now() - t0 < deadlineMs) {
    try {
      const f = page.frames().find(fr => fr.url() !== 'about:blank') || page.mainFrame();
      const ok = await f.evaluate(() => !!window.cn1Started).catch(() => false);
      if (ok) { tReady = Date.now() - t0; break; }
    } catch (e) {}
    await new Promise(r => setTimeout(r, 50));
  }

  await browser.close();
  return { label, tDom, tReady, fetched };
}

console.log('Profiling TeaVM (live)...');
const tea = await profile('TEAVM', URL_TEAVM);
console.log('Profiling LOCAL bundle...');
const loc = await profile('LOCAL', URL_LOCAL);

server.kill();

function summarize(r) {
  console.log(`\n=== ${r.label} ===`);
  console.log(`  DOMContentLoaded: ${r.tDom} ms`);
  console.log(`  cn1Started:       ${r.tReady} ms`);
  console.log(`  Resources fetched: ${r.fetched.length}`);
  const sortedByDur = [...r.fetched].sort((a, b) => b.dur - a.dur);
  console.log('  Top 10 by fetch duration:');
  for (const f of sortedByDur.slice(0, 10)) {
    const url = f.url.length > 60 ? '...' + f.url.slice(-60) : f.url;
    const sz = f.bodyBytes ? `${(f.bodyBytes / 1024).toFixed(1)} KB` : 'n/a';
    console.log(`    ${String(f.dur).padStart(5)} ms  ${sz.padStart(10)}  ${url}`);
  }
  const totalBytes = r.fetched.reduce((a, b) => a + (b.bodyBytes || 0), 0);
  console.log(`  Total bytes: ${(totalBytes / 1048576).toFixed(2)} MiB`);
}

summarize(tea);
summarize(loc);
