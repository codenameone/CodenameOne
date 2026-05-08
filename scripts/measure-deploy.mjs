import { chromium } from 'playwright';

const URL = process.argv[2] || 'https://pr-4795-website-preview.codenameone.pages.dev/initializr/javascript/';
console.log(`Testing ${URL}`);
const browser = await chromium.launch({ headless: true });
const page = await browser.newPage({ viewport: {width:1280,height:900}, deviceScaleFactor:2 });
let mainCompletedAt = null;
const t0 = Date.now();
const messages = [];
page.on('console', msg => {
  const text = msg.text();
  messages.push(text);
  const t = Date.now() - t0;
  if (text.includes('main-thread-completed') && !mainCompletedAt) {
    mainCompletedAt = t;
    console.log(`main-thread-completed @ ${t}ms`);
  }
});
page.on('pageerror', err => console.log(`[error] ${err.message}`));
await page.goto(URL);
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
    if (sig != null && sig > 1000) return Date.now() - t0;
    await new Promise(r => setTimeout(r, 100));
  }
  return null;
}
const fpt = await firstPaint();
console.log(`first non-trivial paint @ ${fpt}ms`);
console.log(`completed=${!!mainCompletedAt}, firstPaint=${fpt}`);
console.log(`last 10 console msgs:`);
messages.slice(-10).forEach(m => console.log(`  ${m.slice(0,200)}`));
await browser.close();
