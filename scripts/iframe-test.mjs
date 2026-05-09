import { chromium } from 'playwright';
const URL = 'https://pr-4795-website-preview.codenameone.pages.dev/initializr/';
const browser = await chromium.launch({ headless: true });
const page = await browser.newPage({ viewport:{width:1280,height:900}, deviceScaleFactor:2 });
const t0 = Date.now();
const messages = [];
page.on('console', msg => messages.push(`+${Date.now()-t0}ms [${msg.type()}] ${msg.text()}`));
page.on('pageerror', err => messages.push(`+${Date.now()-t0}ms [ERR] ${err.message}`));

await page.goto(URL);

// Wait for iframe to load and check its canvas
let bootMs = null, firstPaintMs = null;
while (Date.now() - t0 < 30000) {
  const status = await page.evaluate(() => {
    const f = document.querySelector('#cn1-initializr-frame');
    if (!f || !f.contentDocument) return { hasFrame: !!f };
    const c = f.contentDocument.querySelector('canvas');
    if (!c || c.width === 0) return { hasFrame:true, hasCanvas:!!c, canvasW: c?.width };
    try {
      const img = c.getContext('2d').getImageData(0,0,c.width,c.height).data;
      let nw = 0;
      for (let i = 0; i < img.length; i += 64) {
        const lum = (img[i]+img[i+1]+img[i+2])/3;
        if (img[i+3] > 0 && lum < 240) nw++;
      }
      return { hasFrame:true, hasCanvas:true, canvasW:c.width, canvasH:c.height, nonWhite:nw };
    } catch (e) { return { err: e.message }; }
  });
  if (status.nonWhite > 200 && firstPaintMs === null) {
    firstPaintMs = Date.now() - t0;
    console.log(`iframe canvas paint @ ${firstPaintMs}ms (status=${JSON.stringify(status)})`);
    break;
  }
  await new Promise(r => setTimeout(r, 500));
}
console.log(`firstPaintMs (iframe): ${firstPaintMs}`);
console.log(`Last 15 msgs:`);
messages.slice(-15).forEach(m => console.log(m.slice(0,250)));

const tail = await page.evaluate(() => {
  const f = document.querySelector('#cn1-initializr-frame');
  return f ? { src:f.src, contentWindow: !!f.contentWindow, sandbox: f.sandbox.value, allow: f.allow } : null;
});
console.log('iframe info:', JSON.stringify(tail));
await browser.close();
