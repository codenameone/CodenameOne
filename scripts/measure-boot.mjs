import { chromium } from 'playwright';
import { spawn, execSync } from 'node:child_process';
import fs from 'node:fs';
import path from 'node:path';
import os from 'node:os';

const BUNDLE = '/Users/shai/dev/cn1/scripts/initializr/javascript/target/initializr-javascript-port.zip';
const tmp = fs.mkdtempSync(path.join(os.tmpdir(), 'mboot-'));
const bd = path.join(tmp, 'b');
fs.mkdirSync(bd);
execSync(`unzip -q "${BUNDLE}" -d "${bd}"`);
const dist = path.join(bd, fs.readdirSync(bd).filter(n => fs.statSync(path.join(bd, n)).isDirectory())[0]);
const PORT = 8782;
const srv = spawn('python3', ['-m', 'http.server', String(PORT), '--directory', dist], { stdio: ['ignore','ignore','pipe'] });
await new Promise(r => setTimeout(r, 800));
const browser = await chromium.launch({ headless: true });
const page = await browser.newPage({ viewport: {width:1280,height:900}, deviceScaleFactor:2 });
let mainCompletedAt = null;
let firstPaintAt = null;
let lifecycleStartedAt = null;
const t0 = Date.now();
page.on('console', msg => {
  const t = Date.now() - t0;
  const text = msg.text();
  if (text.includes('main-thread-completed') && !mainCompletedAt) {
    mainCompletedAt = t;
    console.log(`main-thread-completed @ ${t}ms`);
  }
  if (text.includes('lifecycle:started') && !lifecycleStartedAt) {
    lifecycleStartedAt = t;
    console.log(`lifecycle:started @ ${t}ms`);
  }
});
await page.goto(`http://127.0.0.1:${PORT}/`);

// Detect first non-trivial canvas paint (Initializr title appears)
async function firstPaint() {
  const t0local = Date.now();
  while (Date.now() - t0local < 60000) {
    const sig = await page.evaluate(() => {
      const c = document.querySelector('canvas');
      if (!c || c.width === 0) return null;
      const ctx = c.getContext('2d');
      try {
        const img = ctx.getImageData(0, 0, c.width, c.height).data;
        let nonWhite = 0;
        for (let i = 0; i < img.length; i += 16) {
          const lum = (img[i]+img[i+1]+img[i+2])/3;
          if (img[i+3] > 0 && lum < 240) nonWhite++;
        }
        return nonWhite;
      } catch { return null; }
    });
    if (sig != null && sig > 1000) {
      return Date.now() - t0;
    }
    await new Promise(r => setTimeout(r, 100));
  }
  return null;
}
firstPaintAt = await firstPaint();
console.log(`first non-trivial paint @ ${firstPaintAt}ms`);
console.log(`SUMMARY: main=${mainCompletedAt}ms, lifecycle:started=${lifecycleStartedAt}ms, firstPaint=${firstPaintAt}ms`);
await browser.close();
srv.kill();
