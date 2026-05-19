import { chromium } from "playwright";
const url = process.argv[2] || "http://localhost:8766/HelloCodenameOne-js/";
const b = await chromium.launch({ headless: true });
const ctx = await b.newContext({ viewport:{width:375,height:667} });
const page = await ctx.newPage();
const t0 = Date.now();
page.on("console", (m) => {
  const t = m.text();
  if (t.startsWith("CN1SS") || (t.startsWith("PARPAR") && t.includes("LIFECYCLE"))) {
    console.log(`+${Date.now()-t0}ms ${t.substring(0, 180)}`);
  }
});
await page.goto(url, { waitUntil: "domcontentloaded" });
await new Promise(r => setTimeout(r, Number(process.argv[3] || 90000)));
await b.close();
