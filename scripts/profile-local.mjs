import { chromium } from 'playwright';
import { spawn, execSync } from 'node:child_process';
import fs from 'node:fs';
import path from 'node:path';
import os from 'node:os';

const tmp = fs.mkdtempSync(path.join(os.tmpdir(), 'profile-'));
const bd = path.join(tmp, 'b');
fs.mkdirSync(bd);
execSync(`unzip -q /Users/shai/dev/cn1/scripts/initializr/javascript/target/initializr-javascript-port.zip -d ${bd}`);
const dist = path.join(bd, fs.readdirSync(bd).filter(n => fs.statSync(path.join(bd, n)).isDirectory())[0]);
const PORT = 8790;
const srv = spawn('python3', ['-m', 'http.server', String(PORT), '--directory', dist], {stdio:['ignore','ignore','pipe']});
await new Promise(r => setTimeout(r, 800));
const browser = await chromium.launch({ headless: true });
const page = await browser.newPage({ viewport:{width:1280,height:900}, deviceScaleFactor:2 });
const t0 = Date.now();
page.on('console', msg => {
  if (msg.text().includes('main-thread-completed'))
    console.log(`main-thread-completed @ ${Date.now()-t0}ms`);
});
await page.goto(`http://127.0.0.1:${PORT}/`);
while (Date.now() - t0 < 30000) {
  const nw = await page.evaluate(() => {
    const c = document.querySelector('canvas');
    if (!c || c.width === 0) return 0;
    try {
      const img = c.getContext('2d').getImageData(0,0,c.width,c.height).data;
      let nw = 0;
      for (let i = 0; i < img.length; i += 64) {
        const lum = (img[i]+img[i+1]+img[i+2])/3;
        if (img[i+3] > 0 && lum < 240) nw++;
      }
      return nw;
    } catch { return 0; }
  });
  if (nw > 200) { console.log(`firstPaint @ ${Date.now()-t0}ms`); break; }
  await new Promise(r => setTimeout(r, 50));
}
const canvasBox = await page.evaluate(() => {
  const c = document.querySelector('canvas'); const r = c.getBoundingClientRect();
  return { x: r.left, y: r.top };
});
async function sig() {
  return await page.evaluate(() => {
    const c = document.querySelector('canvas');
    const img = c.getContext('2d').getImageData(0,0,c.width,c.height).data;
    let h = 0; for (let i = 0; i < img.length; i += 1024) h = ((h*31) ^ img[i]) | 0;
    return h;
  });
}
const tests = [
  { label: 'kotlin', x: 465, y: 405 },
  { label: 'grub', x: 195, y: 442 },
  { label: 'tweet', x: 465, y: 442 },
  { label: 'barebones', x: 195, y: 405 },
];
for (const t of tests) {
  const sigBefore = await sig();
  const clickAt = Date.now();
  await page.mouse.click(canvasBox.x + t.x, canvasBox.y + t.y);
  let firstChange = null;
  for (let i = 0; i < 60; i++) {
    await new Promise(r => setTimeout(r, 50));
    const s = await sig(); if (s !== sigBefore) { firstChange = Date.now() - clickAt; break; }
  }
  let last = await sig(), since = Date.now(), stable = null;
  for (let i = 0; i < 100; i++) {
    await new Promise(r => setTimeout(r, 100));
    const s = await sig();
    if (s === last) { if (Date.now() - since > 800) { stable = Date.now() - clickAt; break; } }
    else { last = s; since = Date.now(); }
  }
  console.log(`  click ${t.label}: firstChange=${firstChange}ms stableAt=${stable}ms`);
}
await browser.close();
srv.kill();
