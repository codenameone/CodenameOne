#!/usr/bin/env node
/**
 * Capture a tight burst of canvas frames while clicking a target,
 * to see paint pipeline artifacts (dialog missing blocks, side-menu
 * flicker) that a wall-time screenshot bench misses.
 *
 * Captures every animation frame for 1500ms after the click via a
 * requestAnimationFrame loop running in the page that streams
 * timestamps + data URLs back through page.exposeBinding.
 *
 * Usage:
 *   node scripts/dialog-flicker-capture.mjs <url> <target-x> <target-y> <out-dir> [--browser firefox|chromium]
 */
import { chromium, firefox } from "playwright";
import fs from "node:fs";
import path from "node:path";

const args = process.argv.slice(2);
const url = args[0] || "https://pr-4795-website-preview.codenameone.pages.dev/initializr-app/";
const tx = Number(args[1] || 700);
const ty = Number(args[2] || 100);
const outDir = args[3] || "artifacts/flicker-capture";
const browserName = args.includes("--browser") ? args[args.indexOf("--browser") + 1] : "chromium";

fs.mkdirSync(outDir, { recursive: true });

const launcher = browserName === "firefox" ? firefox : chromium;
const browser = await launcher.launch({ headless: true });
const ctx = await browser.newContext({ viewport: { width: 1280, height: 900 }, deviceScaleFactor: 1 });
const page = await ctx.newPage();
const t0 = Date.now();
page.on("console", (m) => {
  const text = m.text();
  if (text.startsWith("PARPAR") || text.includes("LIFECYCLE") || m.type() === "error") {
    console.log(`${Date.now() - t0}ms [${m.type()}] ${text.substring(0, 200)}`);
  }
});

const frames = [];
await page.exposeBinding("__frameSnap", (_, { idx, ts, dataUrl }) => {
  frames.push({ idx, ts, dataUrl });
});

await page.goto(url, { waitUntil: "domcontentloaded", timeout: 30000 });

// Wait until canvas stable
let lastSig = -1, lastChange = 0;
while (Date.now() - t0 < 12000) {
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
  if (Date.now() - t0 - lastChange > 1200 && lastSig !== 0) break;
  await new Promise(r => setTimeout(r, 50));
}
console.log(`stable @ ${Date.now() - t0}ms`);

// Capture box
const box = await page.evaluate(() => {
  const c = document.querySelector("canvas");
  const r = c.getBoundingClientRect();
  return { x: r.left, y: r.top };
});

// Save the baseline before clicking
const baseline = await page.evaluate(() => document.querySelector("canvas").toDataURL("image/png"));
fs.writeFileSync(path.join(outDir, "00-baseline.png"), Buffer.from(baseline.split(",")[1], "base64"));

// Install rAF loop that snaps every frame
await page.evaluate(() => {
  window.__frames = [];
  window.__captureStart = performance.now();
  let idx = 0;
  function snap() {
    const c = document.querySelector("canvas");
    if (!c) return;
    const ts = performance.now() - window.__captureStart;
    if (ts > 1800) return; // stop after 1.8s
    const dataUrl = c.toDataURL("image/png");
    window.__frameSnap({ idx: idx++, ts, dataUrl });
    requestAnimationFrame(snap);
  }
  requestAnimationFrame(snap);
});

console.log(`clicking at canvas (${tx}, ${ty})`);
await page.mouse.click(box.x + tx, box.y + ty);

// Let the rAF loop run + 200ms slack
await new Promise(r => setTimeout(r, 2000));

// Write frames
console.log(`captured ${frames.length} frames`);
for (const f of frames) {
  const name = `f${String(f.idx).padStart(3, "0")}-${String(Math.round(f.ts)).padStart(5, "0")}ms.png`;
  fs.writeFileSync(path.join(outDir, name), Buffer.from(f.dataUrl.split(",")[1], "base64"));
}
console.log(`written to ${outDir}/`);

await browser.close();
