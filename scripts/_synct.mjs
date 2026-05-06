import { chromium } from 'playwright';
import fs from 'node:fs';
import path from 'node:path';
import os from 'node:os';
import { spawn, execSync } from 'node:child_process';

const REPO_ROOT = '/Users/shai/dev/cn1';
const bundle = path.join(REPO_ROOT, 'scripts/initializr/javascript/target/initializr-javascript-port.zip');
const tmpDir = fs.mkdtempSync(path.join(os.tmpdir(), 'syt-'));
const bundleDir = path.join(tmpDir, 'bundle');
fs.mkdirSync(bundleDir);
execSync(`unzip -q "${bundle}" -d "${bundleDir}"`);
const serveDir = path.join(bundleDir, 'Initializr-js');
const port = 7896;
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
page.on('console', m => console.log(`[${m.type()}] ${m.text()}`));
await page.goto(`http://127.0.0.1:${port}/`, { waitUntil: 'domcontentloaded' });
const result = await page.evaluate((port) => new Promise((resolve) => {
  const code = `
    self.onmessage = function(e) {
      const url = e.data;
      const t0 = performance.now();
      const xhr = new XMLHttpRequest();
      xhr.open('get', url, false);
      xhr.responseType = 'arraybuffer';
      xhr.send();
      const dur = Math.round(performance.now() - t0);
      const sz = xhr.response ? xhr.response.byteLength : -1;
      self.postMessage({url, dur, sz});
    };
  `;
  const blob = new Blob([code], {type: 'application/javascript'});
  const w = new Worker(URL.createObjectURL(blob));
  const out = [];
  w.onmessage = (e) => {
    out.push(e.data);
    if (out.length === 4) { w.terminate(); resolve(out); }
  };
  // First call (cold) and second call (warm cache) for each
  w.postMessage(`http://127.0.0.1:${port}/theme.res?xhr=1`);
  w.postMessage(`http://127.0.0.1:${port}/theme.res?xhr=2`);
  w.postMessage(`http://127.0.0.1:${port}/assets/iOS7Theme.res?v=1.0`);
  w.postMessage(`http://127.0.0.1:${port}/assets/iOS7Theme.res?v=2.0`);
}), port);
console.log(JSON.stringify(result, null, 2));
await browser.close();
server.kill();
