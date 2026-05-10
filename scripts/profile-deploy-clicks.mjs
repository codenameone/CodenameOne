import { chromium } from 'playwright';
const URL = 'https://pr-4795-website-preview.codenameone.pages.dev/initializr-app/';
const browser = await chromium.launch({ headless: true });
const page = await browser.newPage({ viewport: {width:1280,height:900}, deviceScaleFactor:2 });
const t0 = Date.now();
let firstPaintMs = null;
const hostCallStarts = [];
const hostCallReturns = [];
page.on('console', msg => {
  const text = msg.text();
  if (text.includes('main-thread-completed')) {
    console.log(`main-thread-completed @ ${Date.now()-t0}ms`);
  }
});

await page.goto(URL);

// Wait for first paint
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
  if (nw > 200) { firstPaintMs = Date.now() - t0; break; }
  await new Promise(r => setTimeout(r, 50));
}
console.log(`firstPaint @ ${firstPaintMs}ms`);

// Now measure click → paint latency
const canvasBox = await page.evaluate(() => {
  const c = document.querySelector('canvas');
  const r = c.getBoundingClientRect();
  return { x: r.left, y: r.top };
});

async function canvasSig() {
  return await page.evaluate(() => {
    const c = document.querySelector('canvas');
    const ctx = c.getContext('2d');
    const img = ctx.getImageData(0,0,c.width,c.height).data;
    let h = 0;
    for (let i = 0; i < img.length; i += 1024) h = ((h*31) ^ img[i]) | 0;
    return h;
  });
}

console.log('--- click latency profile ---');
// Click various template buttons and measure click→change time
const tests = [
  { label: 'kotlin', x: 465, y: 405 },
  { label: 'grub',   x: 195, y: 442 },
  { label: 'tweet',  x: 465, y: 442 },
  { label: 'barebones', x: 195, y: 405 },
];
for (const t of tests) {
  const sigBefore = await canvasSig();
  const clickAt = Date.now();
  await page.mouse.click(canvasBox.x + t.x, canvasBox.y + t.y);
  let firstChange = null;
  for (let i = 0; i < 60; i++) {
    await new Promise(r => setTimeout(r, 50));
    const sig = await canvasSig();
    if (sig !== sigBefore) { firstChange = Date.now() - clickAt; break; }
  }
  // also wait for stable
  let stableAt = null;
  let lastSig = await canvasSig();
  let stableSince = Date.now();
  for (let i = 0; i < 100; i++) {
    await new Promise(r => setTimeout(r, 100));
    const sig = await canvasSig();
    if (sig === lastSig) {
      if (Date.now() - stableSince > 800) { stableAt = Date.now() - clickAt; break; }
    } else {
      lastSig = sig;
      stableSince = Date.now();
    }
  }
  console.log(`  click ${t.label}: firstChange=${firstChange}ms stableAt=${stableAt}ms`);
}
await browser.close();
