// Reproduces the user's report by driving the deployed Initializr in
// its actual iframe context. The local bundle (white body bg) masks the
// black-square pattern because the iframe parent supplies the dark bg
// only in production.
import { chromium } from 'playwright';

const browser = await chromium.launch({ headless: true });
const context = await browser.newContext({
  viewport: { width: 1440, height: 900 },
  deviceScaleFactor: 2,
});
const page = await context.newPage();
const messages = [];
page.on('console', m => messages.push(`[${m.type()}] ${m.text()}`));
page.on('pageerror', e => messages.push(`[error] ${e.message}`));

await page.goto('https://pr-4795-website-preview.codenameone.pages.dev/initializr/', { waitUntil: 'networkidle' });
console.log('loaded outer page');
await page.waitForSelector('#cn1-initializr-frame');
const frameElement = await page.$('#cn1-initializr-frame');
const frame = await frameElement.contentFrame();
// Wait until the loader hides (cn1-initializr-ui-ready postMessage fires)
// or until the canvas has been resized away from its initial 320x480.
await page.waitForFunction(() => {
  const loader = document.getElementById('cn1-initializr-loader');
  return loader && loader.classList.contains('done');
}, { timeout: 180000 }).catch(() => console.log('loader-done timeout'));
// Give the form an extra few seconds to lay out / finish first paint
await new Promise(r => setTimeout(r, 8000));

await page.screenshot({ path: '/tmp/deployed-before-edit.png', fullPage: true });
console.log('saved /tmp/deployed-before-edit.png');

// Click somewhere into the Main Class textfield area inside the iframe.
const box = await frameElement.boundingBox();
console.log('iframe box:', box);
// User report: clicking the Main Class field (form left side, near top of essentials panel).
// Use page.mouse coordinates — these are page-relative; the iframe is positioned with absolute
// top so the form fields end up around y=300-350 in viewport coords for typical layouts.
const candidates = [
  { x: box.x + 200, y: box.y + 240, label: 'mainclass-A' },
  { x: box.x + 200, y: box.y + 290, label: 'mainclass-B' },
  { x: box.x + 200, y: box.y + 340, label: 'mainclass-C' },
  { x: box.x + 200, y: box.y + 410, label: 'package-A' },
];
for (const c of candidates) {
  console.log(`click ${c.label} at`, c.x, c.y);
  await page.mouse.click(c.x, c.y);
  await new Promise(r => setTimeout(r, 600));
  await page.keyboard.type('Hello', { delay: 80 });
  await new Promise(r => setTimeout(r, 600));
  await page.screenshot({ path: `/tmp/deployed-after-${c.label}.png`, fullPage: false });
  console.log(`saved /tmp/deployed-after-${c.label}.png`);
  // Inspect the canvas inside the iframe for transparent / pure-black regions
  const stats = await frame.evaluate(({ cx, cy }) => {
    const canvas = document.querySelector('#codenameone-canvas');
    if (!canvas) return { err: 'no canvas' };
    const ctx = canvas.getContext('2d');
    const dpr = window.devicePixelRatio || 1;
    // Map page coord (cx, cy) to canvas-pixel coord
    const rect = canvas.getBoundingClientRect();
    const px = Math.floor((cx - rect.left) * dpr);
    const py = Math.floor((cy - rect.top) * dpr);
    const stripH = Math.floor(80 * dpr);
    const stripW = Math.floor(180 * dpr);
    let blackPx = 0, transparentPx = 0, total = 0;
    try {
      const data = ctx.getImageData(Math.max(0, px - stripW/2), Math.max(0, py - stripH), stripW, stripH).data;
      for (let p = 0; p < data.length; p += 4) {
        total++;
        const lum = (data[p] + data[p+1] + data[p+2]) / 3 | 0;
        if (data[p+3] === 0) transparentPx++;
        if (lum < 8 && data[p+3] > 0) blackPx++;
      }
    } catch (e) { return { err: String(e) }; }
    return { total, blackPx, transparentPx, blackFrac: blackPx/total, transparentFrac: transparentPx/total, canvasW: canvas.width, canvasH: canvas.height };
  }, { cx: c.x, cy: c.y });
  console.log(`  strip-stats:`, JSON.stringify(stats));
  // dismiss editor before next click
  await page.keyboard.press('Escape');
  await page.keyboard.press('Tab');
  await new Promise(r => setTimeout(r, 300));
}

await browser.close();
console.log('done');
