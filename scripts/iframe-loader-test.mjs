import { chromium } from 'playwright';
const URL = 'https://pr-4795-website-preview.codenameone.pages.dev/initializr/';
const browser = await chromium.launch({ headless: true });
const page = await browser.newPage({ viewport:{width:1280,height:900}, deviceScaleFactor:2 });
const t0 = Date.now();
let uiReady = null;
page.on('console', msg => {
  if (msg.text().includes('cn1-initializr-ui-ready') && !uiReady) {
    uiReady = Date.now() - t0;
  }
});

await page.goto(URL);

// Wait up to 30s, polling every 250ms for loader status
let loaderHidden = null, framePaintAt = null;
while (Date.now() - t0 < 30000) {
  const status = await page.evaluate(() => {
    const loader = document.querySelector('#cn1-initializr-loader');
    const f = document.querySelector('#cn1-initializr-frame');
    let nw = 0;
    if (f && f.contentDocument) {
      const c = f.contentDocument.querySelector('canvas');
      if (c && c.width > 0) {
        try {
          const img = c.getContext('2d').getImageData(0,0,c.width,c.height).data;
          for (let i = 0; i < img.length; i += 64) {
            const lum = (img[i]+img[i+1]+img[i+2])/3;
            if (img[i+3] > 0 && lum < 240) nw++;
          }
        } catch {}
      }
    }
    return {
      loaderClass: loader?.className || '',
      loaderHidden: loader ? loader.classList.contains('done') : null,
      loaderVisible: loader ? getComputedStyle(loader).visibility : null,
      iframeNonWhite: nw,
    };
  });
  if (status.iframeNonWhite > 200 && !framePaintAt) framePaintAt = Date.now() - t0;
  if (status.loaderHidden && !loaderHidden) loaderHidden = Date.now() - t0;
  await new Promise(r => setTimeout(r, 250));
}
const final = await page.evaluate(() => {
  const loader = document.querySelector('#cn1-initializr-loader');
  return { className: loader?.className, hidden: loader ? loader.classList.contains('done') : null };
});
console.log(`framePaint @ ${framePaintAt}ms`);
console.log(`loaderHidden @ ${loaderHidden}ms`);
console.log(`final loader state: ${JSON.stringify(final)}`);
await browser.close();
