#!/usr/bin/env node
/**
 * Measure animation smoothness during the side-menu slide-in.
 *
 * For each browser:
 *   1. Boot the Initializr until canvas stable.
 *   2. Click the hamburger to trigger the side-menu slide-in.
 *   3. Sample the canvas signature every ~16ms for the next 2 seconds.
 *   4. Report frame deltas, total frames where pixels actually changed,
 *      and time gaps between consecutive distinct frames (jank windows).
 *
 * A smooth 60fps slide-in should show ~25-30 distinct frames spaced
 * ~16ms apart. A janky port shows fewer distinct frames with large
 * gaps (200ms+) between transitions — that's what the user perceives
 * as flicker.
 *
 * Usage:
 *   node scripts/initializr-animation-smoothness.mjs
 *   node scripts/initializr-animation-smoothness.mjs --browsers chromium,firefox --runs 2
 */
import { chromium, firefox, webkit } from "playwright";

const args = parseArgs(process.argv.slice(2));
const URL = args.url || "https://pr-4795-website-preview.codenameone.pages.dev/initializr-app/";
const RUNS = Number(args.runs || 1);
const BROWSERS = (args.browsers || "chromium,firefox").split(",").map(s => s.trim());
const VIEWPORT = { width: 1280, height: 900 };
// Coords are in canvas pixels. Default targets the hamburger at the
// top-left of the "Hi World" preview pane.
const CLICK_X = Number(args.x || 705);
const CLICK_Y = Number(args.y || 100);
const SAMPLE_MS = Number(args.sample || 8);
const SAMPLE_DURATION_MS = Number(args.duration || 2000);

function parseArgs(argv) {
  const out = {};
  for (let i = 0; i < argv.length; i++) {
    if (argv[i].startsWith("--")) {
      const k = argv[i].slice(2);
      const next = argv[i + 1];
      if (next && !next.startsWith("--")) { out[k] = next; i++; }
      else { out[k] = true; }
    }
  }
  return out;
}

const launchers = { chromium, firefox, webkit };

async function measureOnce(browserName) {
  const launcher = launchers[browserName];
  const browser = await launcher.launch({ headless: true });
  const ctx = await browser.newContext({ viewport: VIEWPORT, deviceScaleFactor: 1 });
  const page = await ctx.newPage();
  const t0 = Date.now();
  await page.goto(URL, { waitUntil: "domcontentloaded", timeout: 60000 });

  // Wait for canvas stable (boot)
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

  // Click hamburger; immediately start in-page rAF sampler that pushes
  // timestamps + signature back to us via exposeBinding.
  const samples = [];
  await page.exposeBinding("__sigSample", (_, payload) => {
    samples.push(payload);
  });
  await page.evaluate(({ sampleMs, durationMs }) => {
    window.__sigSamplerStart = performance.now();
    window.__sigSamplerStop = window.__sigSamplerStart + durationMs;
    const c = document.querySelector("canvas");
    const ctx = c.getContext("2d");
    function take() {
      const now = performance.now();
      if (now > window.__sigSamplerStop) return;
      try {
        const img = ctx.getImageData(0, 0, c.width, c.height).data;
        let s = 0;
        for (let i = 0; i < img.length; i += 32) s = ((s * 31) ^ (img[i] + img[i + 1] * 256 + img[i + 2] * 65536)) | 0;
        window.__sigSample({ t: now - window.__sigSamplerStart, sig: s });
      } catch {}
      setTimeout(take, sampleMs);
    }
    take();
  }, { sampleMs: SAMPLE_MS, durationMs: SAMPLE_DURATION_MS });

  const tClick = Date.now();
  await page.mouse.click(box.x + CLICK_X, box.y + CLICK_Y);
  void tClick;

  // Wait for sampler to finish + small buffer
  await new Promise(r => setTimeout(r, SAMPLE_DURATION_MS + 300));

  await browser.close();

  // Compute frame-change boundaries
  samples.sort((a, b) => a.t - b.t);
  const distinctFrames = [];
  let prevSig = null;
  for (const s of samples) {
    if (s.sig !== prevSig) {
      distinctFrames.push(s.t);
      prevSig = s.sig;
    }
  }
  // Frame intervals between distinct frames
  const intervals = [];
  for (let i = 1; i < distinctFrames.length; i++) {
    intervals.push(distinctFrames[i] - distinctFrames[i - 1]);
  }
  intervals.sort((a, b) => a - b);
  const sum = intervals.reduce((a, b) => a + b, 0);
  const avg = intervals.length ? sum / intervals.length : 0;
  const median = intervals.length ? intervals[Math.floor(intervals.length / 2)] : 0;
  const p95 = intervals.length ? intervals[Math.floor(intervals.length * 0.95)] : 0;
  const max = intervals.length ? intervals[intervals.length - 1] : 0;

  return {
    browser: browserName,
    tStable,
    sampleCount: samples.length,
    distinctFrameCount: distinctFrames.length,
    avgIntervalMs: Math.round(avg),
    medianIntervalMs: Math.round(median),
    p95IntervalMs: Math.round(p95),
    maxIntervalMs: Math.round(max),
    distinctFrameTimes: distinctFrames.map(t => Math.round(t)),
  };
}

async function main() {
  console.error(`Animation-smoothness bench (${RUNS} run(s) per browser)`);
  console.error(`  url: ${URL}`);
  console.error(`  click: (${CLICK_X}, ${CLICK_Y}) on canvas`);
  console.error(`  sample: every ${SAMPLE_MS}ms for ${SAMPLE_DURATION_MS}ms\n`);

  const allResults = {};
  for (const b of BROWSERS) {
    allResults[b] = [];
    for (let i = 0; i < RUNS; i++) {
      process.stderr.write(`  [${b} ${i + 1}/${RUNS}]\n`);
      allResults[b].push(await measureOnce(b));
    }
  }

  console.log("\n=== animation smoothness ===\n");
  console.log("  browser   stable  samples  frames  avg-ms  med-ms  p95-ms  max-ms");
  for (const b of BROWSERS) {
    for (const r of allResults[b]) {
      console.log(
        `  ${b.padEnd(9)} ${String(r.tStable).padStart(5)}ms ${
          String(r.sampleCount).padStart(7)} ${
          String(r.distinctFrameCount).padStart(7)} ${
          String(r.avgIntervalMs).padStart(6)}  ${
          String(r.medianIntervalMs).padStart(6)}  ${
          String(r.p95IntervalMs).padStart(6)}  ${
          String(r.maxIntervalMs).padStart(6)}`
      );
    }
  }

  console.log("\n=== first 20 distinct-frame timestamps (ms post-click) ===\n");
  for (const b of BROWSERS) {
    for (let i = 0; i < allResults[b].length; i++) {
      const r = allResults[b][i];
      console.log(`  ${b} run ${i + 1}: ${r.distinctFrameTimes.slice(0, 20).join(", ")}`);
    }
  }
}

main().catch(e => { console.error(e); process.exit(1); });
