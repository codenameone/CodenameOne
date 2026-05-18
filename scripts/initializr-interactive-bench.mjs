#!/usr/bin/env node
/**
 * Interactive responsiveness bench: loads the app, waits for stable,
 * then performs a sequence of clicks on known UI elements and measures
 * click->canvas-change latency for each.
 *
 * Targets: side menu rows (IDE, Theme Customization, Localization,
 * Java Version, Current Settings), template buttons (BAREBONES,
 * KOTLIN, GRUB, TWEET), Generate Project button.
 *
 * Saves before/after PNGs per click to artifacts/interactive-bench/.
 */

import { chromium } from "playwright";
import fs from "node:fs";
import path from "node:path";

const args = parseArgs(process.argv.slice(2));
const URLS = {
  ours: args.ours || "https://pr-4795-website-preview.codenameone.pages.dev/initializr-app/",
  theirs: args.theirs || "https://www.codenameone.com/initializr-app/",
};
const OUT_DIR = args.out || "artifacts/interactive-bench";
const VIEWPORT = { width: 1280, height: 900 };

// Click targets at known canvas coordinates (matched against the
// stable.png layout). Coordinates approximate but in similar enough
// regions in both ours and theirs that they hit the same widget.
const TARGETS = [
  { name: "ide-row",         x: 332, y: 525 },
  { name: "theme-row",       x: 332, y: 578 },
  { name: "loc-row",         x: 332, y: 631 },
  { name: "java-row",        x: 332, y: 685 },
  { name: "current-row",     x: 332, y: 738 },
  { name: "tpl-kotlin",      x: 466, y: 403 },
  { name: "tpl-tweet",       x: 466, y: 441 },
  { name: "tpl-grub",        x: 195, y: 441 },
  { name: "tpl-barebones",   x: 195, y: 403 },
  { name: "generate",        x: 640, y: 864 },
];

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

function mkdirp(p) { fs.mkdirSync(p, { recursive: true }); }

async function canvasSig(page) {
  return await page.evaluate(() => {
    const c = document.querySelector("canvas");
    if (!c) return 0;
    try {
      const ctx = c.getContext("2d");
      const img = ctx.getImageData(0, 0, c.width, c.height).data;
      let s = 0;
      for (let i = 0; i < img.length; i += 32) {
        s = ((s * 31) ^ (img[i] + img[i + 1] * 256 + img[i + 2] * 65536)) | 0;
      }
      return s;
    } catch { return 0; }
  });
}

async function snapCanvas(page) {
  return await page.evaluate(() => {
    const c = document.querySelector("canvas");
    if (!c) return null;
    return c.toDataURL("image/png");
  });
}

async function waitForStable(page, t0) {
  let lastSig = -1;
  let lastChange = 0;
  while (Date.now() - t0 < 15000) {
    const sig = await canvasSig(page);
    if (sig !== lastSig) { lastSig = sig; lastChange = Date.now() - t0; }
    if (Date.now() - t0 - lastChange > 1200 && lastSig !== 0) return;
    await new Promise(r => setTimeout(r, 50));
  }
}

async function runOne(url, label) {
  const dir = path.join(OUT_DIR, label);
  mkdirp(dir);
  const browser = await chromium.launch({ headless: true });
  const ctx = await browser.newContext({ viewport: VIEWPORT, deviceScaleFactor: 1, bypassCSP: true });
  const page = await ctx.newPage();
  const t0 = Date.now();
  await page.goto(url, { waitUntil: "domcontentloaded", timeout: 60000 });
  await waitForStable(page, t0);
  const tStable = Date.now() - t0;

  // Capture baseline
  const baseline = await snapCanvas(page);
  if (baseline) fs.writeFileSync(path.join(dir, "00-baseline.png"), Buffer.from(baseline.split(",")[1], "base64"));

  const box = await page.evaluate(() => {
    const c = document.querySelector("canvas");
    const r = c.getBoundingClientRect();
    return { x: r.left, y: r.top };
  });

  const results = [];
  for (let idx = 0; idx < TARGETS.length; idx++) {
    const t = TARGETS[idx];
    const sigBefore = await canvasSig(page);
    const clickT0 = Date.now();
    await page.mouse.click(box.x + t.x, box.y + t.y);
    let firstChange = null;
    let lastChange = null;
    let lastSig = sigBefore;
    // Sample fast for 3s after click
    for (let i = 0; i < 60; i++) {
      await new Promise(r => setTimeout(r, 50));
      const sigNow = await canvasSig(page);
      if (sigNow !== lastSig) {
        if (firstChange === null) firstChange = Date.now() - clickT0;
        lastChange = Date.now() - clickT0;
        lastSig = sigNow;
      } else if (firstChange !== null && Date.now() - clickT0 - lastChange > 500) {
        break;
      }
    }
    const after = await snapCanvas(page);
    if (after) fs.writeFileSync(path.join(dir, `${String(idx + 1).padStart(2, "0")}-${t.name}.png`), Buffer.from(after.split(",")[1], "base64"));
    const changed = firstChange !== null;
    results.push({ name: t.name, x: t.x, y: t.y, firstChange, lastChange, changed });
    // Wait to settle before next click
    await new Promise(r => setTimeout(r, 300));
  }

  await browser.close();
  return { label, url, tStable, results };
}

async function main() {
  mkdirp(OUT_DIR);
  console.error(`Interactive bench`);
  console.error(`  ours:   ${URLS.ours}`);
  console.error(`  theirs: ${URLS.theirs}`);
  const ours = await runOne(URLS.ours, "ours");
  const theirs = await runOne(URLS.theirs, "theirs");

  console.log("\n=== interactive click response ===");
  console.log(`  metric                  ours       theirs`);
  console.log(`  tStable                ${String(ours.tStable).padStart(6)}ms   ${String(theirs.tStable).padStart(6)}ms`);
  console.log();
  console.log(`  click target            ours-1st    ours-settle  theirs-1st  theirs-settle`);
  for (let i = 0; i < ours.results.length; i++) {
    const o = ours.results[i], t = theirs.results[i];
    const fmt = (v) => v == null ? "  miss" : String(v).padStart(5) + "ms";
    console.log(`  ${o.name.padEnd(22)}  ${fmt(o.firstChange)}    ${fmt(o.lastChange)}     ${fmt(t.firstChange)}    ${fmt(t.lastChange)}`);
  }

  fs.writeFileSync(path.join(OUT_DIR, "report.json"), JSON.stringify({ ours, theirs }, null, 2));
  console.error(`\nReport: ${path.join(OUT_DIR, "report.json")}`);
  console.error(`Screenshots: ${OUT_DIR}/{ours,theirs}/`);
}

main().catch(e => { console.error(e); process.exit(1); });
