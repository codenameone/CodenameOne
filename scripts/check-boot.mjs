import { chromium } from 'playwright';
import { spawn, execSync } from 'node:child_process';
import fs from 'node:fs';
import path from 'node:path';
import os from 'node:os';

const REPO_ROOT = '/Users/shai/dev/cn1';
const BUNDLE = path.join(REPO_ROOT, 'scripts/initializr/javascript/target/initializr-javascript-port.zip');
const tmpDir = fs.mkdtempSync(path.join(os.tmpdir(), 'init-boot-'));
const bundleDir = path.join(tmpDir, 'bundle');
fs.mkdirSync(bundleDir);
execSync(`unzip -q "${BUNDLE}" -d "${bundleDir}"`);
const distEntry = fs.readdirSync(bundleDir).filter(n => fs.statSync(path.join(bundleDir, n)).isDirectory())[0];
const distDir = path.join(bundleDir, distEntry);

const PORT = 8779;
const server = spawn('python3', ['-m', 'http.server', String(PORT), '--directory', distDir], { stdio: ['ignore','ignore','pipe'] });
await new Promise(r => setTimeout(r, 800));
const browser = await chromium.launch({ headless: true });
const page = await browser.newPage({ viewport: { width:1280, height:900 }, deviceScaleFactor:2 });
const messages = [];
page.on('console', msg => messages.push(`[${msg.type()}] ${msg.text()}`));
page.on('pageerror', err => messages.push(`[error] ${err.message}`));
page.on('worker', w => w.on('console', msg => messages.push(`[w:${msg.type()}] ${msg.text()}`)));

await page.goto(`http://127.0.0.1:${PORT}/?parparDiag=1`);
await new Promise(r => setTimeout(r, 30000));
console.log('=== last 80 messages ===');
messages.slice(-80).forEach(m => console.log(m.slice(0,260)));
console.log('=== completed?', messages.some(m => m.includes('main-thread-completed')));
console.log('=== started?', messages.some(m => m.includes('cn1Started') || m.includes('lifecycle:started')));
console.log('=== errors:', messages.filter(m => m.includes('[error]') || m.includes('Exception')).length);
await browser.close();
server.kill();
