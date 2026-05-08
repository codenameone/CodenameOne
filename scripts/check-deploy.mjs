import { chromium } from 'playwright';

const URL = 'https://pr-4795-website-preview.codenameone.pages.dev/initializr/';
const BOOT_BUDGET = 15_000;
const PAINT_BUDGET = 20_000;

const browser = await chromium.launch({ headless: true });
const page = await browser.newPage({ viewport:{width:1280,height:900}, deviceScaleFactor:2 });
const messages = [];
page.on('console', msg => messages.push(`[${msg.type()}] ${msg.text()}`));

const t0 = Date.now();
await page.goto(URL);

let bootMs = null;
while (Date.now() - t0 < BOOT_BUDGET) {
  if (messages.some(m => m.includes('main-thread-completed'))) { bootMs = Date.now() - t0; break; }
  await new Promise(r => setTimeout(r, 100));
}
console.log(`bootMs (${BOOT_BUDGET}ms budget): ${bootMs}`);

let firstPaintMs = null;
const paintDeadline = t0 + BOOT_BUDGET + PAINT_BUDGET;
while (Date.now() < paintDeadline) {
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
  if (nw > 200) { firstPaintMs = Date.now() - t0; break; }
  await new Promise(r => setTimeout(r, 100));
}
console.log(`firstPaintMs (${PAINT_BUDGET}ms budget after boot): ${firstPaintMs}`);
if (bootMs === null) console.log("ASSERTION FAILED: boot did not complete in budget");
if (firstPaintMs === null) console.log("ASSERTION FAILED: canvas never rendered non-trivial content");
await browser.close();
