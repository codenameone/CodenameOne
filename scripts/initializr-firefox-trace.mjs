#!/usr/bin/env node
/**
 * Trace what Firefox spends time on during the side-menu animation.
 *
 * For each browser, after canvas stable:
 *   1. Click hamburger.
 *   2. Sample timestamps from BOTH a worker-side PARPAR-LIFECYCLE
 *      / DIAG log stream AND a page-side rAF clock for 1500ms.
 *   3. Identify the time windows where neither side reports anything —
 *      those are the "stall windows" the user sees as flicker.
 *
 * Also reports how many host-call/UI-event messages the worker emits
 * during the animation window, to test the "Firefox WebWorker
 * postMessage is slow" hypothesis.
 */
import { chromium, firefox } from "playwright";

const URL = process.argv[2] || "https://pr-4795-website-preview.codenameone.pages.dev/initializr-app/";
const BROWSERS = ["chromium", "firefox"];
const VIEWPORT = { width: 1280, height: 900 };
const CLICK_X = 705;
const CLICK_Y = 100;
const TRACE_MS = 1500;

const launchers = { chromium, firefox };

async function measure(browserName) {
  const browser = await launchers[browserName].launch({ headless: true });
  const ctx = await browser.newContext({ viewport: VIEWPORT, deviceScaleFactor: 1 });
  const page = await ctx.newPage();
  const t0 = Date.now();
  const lifecycleEvents = [];

  page.on("console", (m) => {
    const t = m.text();
    if (t.startsWith("PARPAR")) {
      lifecycleEvents.push({ ts: Date.now() - t0, type: m.type(), text: t.substring(0, 120) });
    }
  });

  await page.goto(URL, { waitUntil: "domcontentloaded", timeout: 60000 });

  // Wait until canvas stable
  let lastSig = -1, lastChange = 0;
  while (Date.now() - t0 < 30000) {
    const sig = await page.evaluate(() => {
      const c = document.querySelector("canvas");
      if (!c) return 0;
      try {
        const img = c.getContext("2d").getImageData(0, 0, c.width, c.height).data;
        let s = 0;
        for (let i = 0; i < img.length; i += 32) s = ((s * 31) ^ (img[i] + img[i + 1] * 256 + img[i + 2] * 65536)) | 0;
        return s;
      } catch { return 0; }
    });
    if (sig !== lastSig) { lastSig = sig; lastChange = Date.now() - t0; }
    if (lastSig !== 0 && (Date.now() - t0 - lastChange) > 1500) break;
    await new Promise(r => setTimeout(r, 50));
  }
  const tStable = Date.now() - t0;

  const box = await page.evaluate(() => {
    const c = document.querySelector("canvas");
    const r = c.getBoundingClientRect();
    return { x: r.left, y: r.top };
  });

  // Page-side rAF ticker recording the wall-clock between each
  // animation frame. Cleared on click.
  await page.evaluate((traceMs) => {
    window.__rafTicks = [];
    window.__rafStart = performance.now();
    function tick() {
      if (performance.now() - window.__rafStart > traceMs) return;
      window.__rafTicks.push(performance.now() - window.__rafStart);
      requestAnimationFrame(tick);
    }
    requestAnimationFrame(tick);
  }, TRACE_MS);

  const tClickStart = Date.now() - t0;
  const lifecycleEventCountAtClick = lifecycleEvents.length;
  await page.mouse.click(box.x + CLICK_X, box.y + CLICK_Y);

  await new Promise(r => setTimeout(r, TRACE_MS + 200));

  const rafTicks = await page.evaluate(() => window.__rafTicks || []);

  await browser.close();

  // Compute rAF interval distribution
  const rafIntervals = [];
  for (let i = 1; i < rafTicks.length; i++) {
    rafIntervals.push(Math.round(rafTicks[i] - rafTicks[i - 1]));
  }
  rafIntervals.sort((a, b) => a - b);
  const max = rafIntervals[rafIntervals.length - 1] || 0;
  const p95 = rafIntervals[Math.floor(rafIntervals.length * 0.95)] || 0;
  const med = rafIntervals[Math.floor(rafIntervals.length / 2)] || 0;
  const avg = rafIntervals.length ? rafIntervals.reduce((a, b) => a + b, 0) / rafIntervals.length : 0;

  // Lifecycle events emitted during the trace window
  const tClickEnd = tClickStart + TRACE_MS;
  const lifecycleInWindow = lifecycleEvents.filter(e => e.ts >= tClickStart && e.ts <= tClickEnd);

  return {
    browser: browserName,
    tStable,
    rafFrames: rafTicks.length,
    rafAvg: Math.round(avg),
    rafMed: med,
    rafP95: p95,
    rafMax: max,
    lifecycleEventsBeforeClick: lifecycleEventCountAtClick,
    lifecycleEventsDuringAnimation: lifecycleInWindow.length,
    lifecycleSamplesInWindow: lifecycleInWindow.slice(0, 15),
  };
}

const results = [];
for (const b of BROWSERS) {
  process.stderr.write(`[${b}] ...\n`);
  results.push(await measure(b));
}

console.log("\n=== rAF cadence during side-menu animation ===\n");
console.log("  browser    stable  rAFcount  avg  med  p95  max");
for (const r of results) {
  console.log(`  ${r.browser.padEnd(9)} ${String(r.tStable).padStart(5)}ms ${
    String(r.rafFrames).padStart(8)}  ${
    String(r.rafAvg).padStart(3)}ms ${
    String(r.rafMed).padStart(3)}ms ${
    String(r.rafP95).padStart(3)}ms ${
    String(r.rafMax).padStart(3)}ms`);
}

console.log("\n=== PARPAR-LIFECYCLE traffic during animation ===\n");
for (const r of results) {
  console.log(`  ${r.browser}: ${r.lifecycleEventsDuringAnimation} events in ${TRACE_MS}ms`);
  for (const e of r.lifecycleSamplesInWindow.slice(0, 8)) {
    console.log(`    +${e.ts}ms: ${e.text}`);
  }
}
