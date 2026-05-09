import { chromium } from 'playwright';
const URL = 'https://pr-4795-website-preview.codenameone.pages.dev/initializr-app/?parparDiag=1';
const browser = await chromium.launch({ headless: true });
const page = await browser.newPage({ viewport:{width:1280,height:900}, deviceScaleFactor:2 });
const t0 = Date.now();
const all = [];
const errors = [];
const reqfails = [];
page.on('console', msg => all.push(`+${Date.now()-t0}ms [${msg.type()}] ${msg.text()}`));
page.on('pageerror', err => { errors.push(err.message); all.push(`+${Date.now()-t0}ms [pageerror] ${err.message}`); });
page.on('worker', w => w.on('console', msg => all.push(`+${Date.now()-t0}ms [w:${msg.type()}] ${msg.text()}`)));
page.on('requestfailed', req => { reqfails.push(`${req.url()}: ${req.failure().errorText}`); all.push(`+${Date.now()-t0}ms [reqfail] ${req.url()}: ${req.failure().errorText}`); });

await page.goto(URL);
await new Promise(r => setTimeout(r, 30000));
console.log('=== ERRORS ===');
errors.forEach(e => console.log(`  ${e}`));
console.log('=== REQ FAILS ===');
reqfails.forEach(r => console.log(`  ${r}`));
console.log('=== messages with "error" / "exception" / "fail" ===');
all.filter(m => /error|exception|fail|undefined|null|stuck/i.test(m)).forEach(m => console.log(m.slice(0,300)));
console.log('=== last 20 messages ===');
all.slice(-20).forEach(m => console.log(m.slice(0,250)));

// Check if canvas exists and has dimensions
const canvasInfo = await page.evaluate(() => {
  const c = document.querySelector('canvas');
  if (!c) return {exists:false};
  return { exists:true, w:c.width, h:c.height, css:c.getBoundingClientRect() };
});
console.log('=== canvas:', JSON.stringify(canvasInfo));
await browser.close();
