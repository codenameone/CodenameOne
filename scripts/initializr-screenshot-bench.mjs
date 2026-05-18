#!/usr/bin/env node
/**
 * Screenshot-based perf + visual-regression bench.
 *
 * Loads ours (JS port) and theirs (TeaVM) Initializr in parallel,
 * snaps the canvas at fixed wall-time checkpoints (250/500/1000/2000/3000ms
 * and "stable"), and emits:
 *   - PNGs per checkpoint into artifacts/screenshot-bench/<label>/<t>.png
 *   - Pixel-diff PNG vs TeaVM at each checkpoint (when both painted)
 *   - JSON report with diff%, paint times, transfer size
 *
 * Goals:
 *   - Detect rendering regressions (dialog border, side menu flicker)
 *   - Validate that ours catches up to theirs visually
 *   - Validate that ours reaches "stable" within ~same wall-time as theirs
 *
 * Usage:
 *   node scripts/initializr-screenshot-bench.mjs
 *   node scripts/initializr-screenshot-bench.mjs --runs 3
 *   node scripts/initializr-screenshot-bench.mjs --ours <url> --theirs <url>
 *   node scripts/initializr-screenshot-bench.mjs --interact   # also click first form field, snap after settle
 */

import { chromium } from "playwright";
import fs from "node:fs";
import path from "node:path";
import { PNG } from "pngjs";
import pixelmatch from "pixelmatch";

const args = parseArgs(process.argv.slice(2));
const URLS = {
  ours: args.ours || "https://pr-4795-website-preview.codenameone.pages.dev/initializr-app/",
  theirs: args.theirs || "https://www.codenameone.com/initializr-app/",
};
const RUNS = Number(args.runs || 1);
const INTERACT = !!args.interact;
const OUT_DIR = args.out || "artifacts/screenshot-bench";
const VIEWPORT = { width: 1280, height: 900 };
const CHECKPOINTS_MS = [250, 500, 750, 1000, 1500, 2000, 3000, 5000];

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

async function snapCanvas(page) {
  // Capture canvas pixel data via the page; convert to PNG via pngjs.
  return await page.evaluate(() => {
    const c = document.querySelector("canvas");
    if (!c || c.width === 0 || c.height === 0) return null;
    try {
      const dataUrl = c.toDataURL("image/png");
      return { w: c.width, h: c.height, dataUrl };
    } catch (e) { return { err: e.message }; }
  });
}

function decodePngDataUrl(dataUrl) {
  const b64 = dataUrl.replace(/^data:image\/png;base64,/, "");
  const buf = Buffer.from(b64, "base64");
  return PNG.sync.read(buf);
}

function nonBlankRatio(png) {
  let non = 0;
  for (let i = 0; i < png.data.length; i += 4) {
    const r = png.data[i], g = png.data[i+1], b = png.data[i+2], a = png.data[i+3];
    const lum = (r + g + b) / 3;
    if (a > 0 && lum < 240) non++;
  }
  return non / (png.data.length / 4);
}

async function runOne(url, label, runIdx) {
  const runDir = path.join(OUT_DIR, label, "run" + runIdx);
  mkdirp(runDir);

  const browser = await chromium.launch({ headless: true });
  const ctx = await browser.newContext({ viewport: VIEWPORT, deviceScaleFactor: 1, bypassCSP: true });
  const page = await ctx.newPage();

  let totalBytes = 0;
  const xhrTimings = [];
  page.on("response", async (resp) => {
    try {
      const headers = resp.headers();
      const len = Number(headers["content-length"] || 0);
      if (len > 0) totalBytes += len;
      const u = resp.url();
      if (u.endsWith(".res") || u.endsWith("translated_app.js") || u.endsWith("port.js")) {
        const t = resp.timing();
        xhrTimings.push({ url: u.split("/").pop(), len, t });
      }
    } catch (e) { /* ignore */ }
  });

  const t0 = Date.now();
  await page.goto(url, { waitUntil: "domcontentloaded", timeout: 60000 });
  const tDcl = Date.now() - t0;

  const snapshots = {};
  let nextCpIdx = 0;
  let firstPaintMs = null;
  let lastSig = -1;
  let lastChangeMs = 0;
  const POLL_MS = 50;
  const SETTLE_TIMEOUT_MS = 15000;

  // Take a checkpoint snapshot when we cross a CHECKPOINTS_MS threshold.
  while (true) {
    const now = Date.now() - t0;

    // Trigger any due checkpoints.
    while (nextCpIdx < CHECKPOINTS_MS.length && now >= CHECKPOINTS_MS[nextCpIdx]) {
      const cp = CHECKPOINTS_MS[nextCpIdx++];
      const snap = await snapCanvas(page);
      if (snap && snap.dataUrl) {
        const png = decodePngDataUrl(snap.dataUrl);
        const fname = path.join(runDir, `t${String(cp).padStart(5, "0")}ms.png`);
        fs.writeFileSync(fname, PNG.sync.write(png));
        const nb = nonBlankRatio(png);
        snapshots[cp] = { file: fname, w: png.width, h: png.height, nonBlank: nb, sampledAt: now };
        if (firstPaintMs === null && nb > 0.02) firstPaintMs = now;
      } else {
        snapshots[cp] = { file: null, nonBlank: 0, sampledAt: now };
      }
    }

    // Sample for stability + first-paint detection.
    const sig = await page.evaluate(() => {
      const c = document.querySelector("canvas");
      if (!c || c.width === 0 || c.height === 0) return 0;
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
    if (sig !== lastSig) { lastSig = sig; lastChangeMs = now; }

    // Done when canvas exists, painted, and idle 1500ms.
    const idleEnough = firstPaintMs !== null && (now - lastChangeMs) > 1500;
    const haveAllCheckpoints = nextCpIdx >= CHECKPOINTS_MS.length;
    if (idleEnough && haveAllCheckpoints) break;
    if (now > SETTLE_TIMEOUT_MS) break;
    await new Promise((r) => setTimeout(r, POLL_MS));
  }
  const tStable = Date.now() - t0;

  // Final "stable" snapshot.
  const stableSnap = await snapCanvas(page);
  let stableFile = null;
  if (stableSnap && stableSnap.dataUrl) {
    const png = decodePngDataUrl(stableSnap.dataUrl);
    stableFile = path.join(runDir, "stable.png");
    fs.writeFileSync(stableFile, PNG.sync.write(png));
  }

  // Optional interaction: click middle of first text-input area.
  let interactResult = null;
  if (INTERACT && stableFile) {
    const box = await page.evaluate(() => {
      const c = document.querySelector("canvas");
      const r = c.getBoundingClientRect();
      return { x: r.left, y: r.top, w: r.width, h: r.height };
    });
    const tClick = Date.now();
    // Click somewhere within the canvas where Initializr places the IDE button.
    // Use a rough heuristic: 70% across, 25% down.
    await page.mouse.click(box.x + box.w * 0.70, box.y + box.h * 0.25);
    let firstChangeMs = null;
    const beforeSig = lastSig;
    for (let i = 0; i < 80; i++) {
      await new Promise((r) => setTimeout(r, 25));
      const sigNow = await page.evaluate(() => {
        const c = document.querySelector("canvas");
        const ctx = c.getContext("2d");
        const img = ctx.getImageData(0, 0, c.width, c.height).data;
        let s = 0;
        for (let i = 0; i < img.length; i += 32) {
          s = ((s * 31) ^ (img[i] + img[i + 1] * 256 + img[i + 2] * 65536)) | 0;
        }
        return s;
      });
      if (sigNow !== beforeSig) { firstChangeMs = Date.now() - tClick; break; }
    }
    // Wait for settle after click
    await new Promise((r) => setTimeout(r, 1500));
    const afterSnap = await snapCanvas(page);
    let afterFile = null;
    if (afterSnap && afterSnap.dataUrl) {
      const png = decodePngDataUrl(afterSnap.dataUrl);
      afterFile = path.join(runDir, "after-click.png");
      fs.writeFileSync(afterFile, PNG.sync.write(png));
    }
    interactResult = { firstChangeMs, afterFile };
  }

  await browser.close();

  return {
    label, url, tDcl, tStable, firstPaintMs, totalBytes,
    snapshots, stableFile, interactResult, xhrTimings,
  };
}

async function runMany(url, label, runs) {
  const results = [];
  for (let i = 0; i < runs; i++) {
    process.stderr.write(`  [${label} run ${i + 1}/${runs}]\n`);
    results.push(await runOne(url, label, i));
  }
  return results;
}

function diffPngs(a, b, outFile) {
  if (!a || !b) return null;
  if (a.width !== b.width || a.height !== b.height) {
    // Pad smaller to larger
    const w = Math.max(a.width, b.width), h = Math.max(a.height, b.height);
    a = padPng(a, w, h);
    b = padPng(b, w, h);
  }
  const diff = new PNG({ width: a.width, height: a.height });
  const pixels = pixelmatch(a.data, b.data, diff.data, a.width, a.height,
    { threshold: 0.18, includeAA: false });
  if (outFile) fs.writeFileSync(outFile, PNG.sync.write(diff));
  return { pixels, total: a.width * a.height, ratio: pixels / (a.width * a.height) };
}

function padPng(src, w, h) {
  const out = new PNG({ width: w, height: h });
  // White background
  for (let i = 0; i < out.data.length; i += 4) {
    out.data[i] = 255; out.data[i+1] = 255; out.data[i+2] = 255; out.data[i+3] = 255;
  }
  for (let y = 0; y < src.height; y++) {
    for (let x = 0; x < src.width; x++) {
      const si = (y * src.width + x) * 4;
      const di = (y * w + x) * 4;
      out.data[di] = src.data[si];
      out.data[di+1] = src.data[si+1];
      out.data[di+2] = src.data[si+2];
      out.data[di+3] = src.data[si+3];
    }
  }
  return out;
}

function loadPng(file) {
  if (!file || !fs.existsSync(file)) return null;
  return PNG.sync.read(fs.readFileSync(file));
}

async function main() {
  mkdirp(OUT_DIR);
  console.error(`Screenshot bench (${RUNS} run(s) per side)`);
  console.error(`  ours:   ${URLS.ours}`);
  console.error(`  theirs: ${URLS.theirs}`);

  const oursRuns = await runMany(URLS.ours, "ours", RUNS);
  const theirsRuns = await runMany(URLS.theirs, "theirs", RUNS);
  const ours = oursRuns[0];
  const theirs = theirsRuns[0];

  console.log("\n=== timing ===");
  console.log(`  metric                ours       theirs`);
  console.log(`  DOMContentLoaded      ${String(ours.tDcl).padStart(6)}ms   ${String(theirs.tDcl).padStart(6)}ms`);
  console.log(`  first non-blank paint ${String(ours.firstPaintMs).padStart(6)}ms   ${String(theirs.firstPaintMs).padStart(6)}ms`);
  console.log(`  stable               ${String(ours.tStable).padStart(6)}ms   ${String(theirs.tStable).padStart(6)}ms`);
  console.log(`  transfer             ${(ours.totalBytes/1024).toFixed(0).padStart(6)}KB   ${(theirs.totalBytes/1024).toFixed(0).padStart(6)}KB`);

  console.log("\n=== checkpoint visual diff (ours vs theirs) ===");
  const diffDir = path.join(OUT_DIR, "diff");
  mkdirp(diffDir);
  const checkpointResults = [];
  for (const cp of CHECKPOINTS_MS) {
    const oFile = ours.snapshots[cp] && ours.snapshots[cp].file;
    const tFile = theirs.snapshots[cp] && theirs.snapshots[cp].file;
    const o = loadPng(oFile);
    const t = loadPng(tFile);
    const diff = diffPngs(o, t, path.join(diffDir, `cp_${String(cp).padStart(5,"0")}ms.diff.png`));
    const oNB = ours.snapshots[cp] ? ours.snapshots[cp].nonBlank : 0;
    const tNB = theirs.snapshots[cp] ? theirs.snapshots[cp].nonBlank : 0;
    console.log(`  t=${String(cp).padStart(5)}ms  ours-nonblank=${(oNB*100).toFixed(1)}%  theirs-nonblank=${(tNB*100).toFixed(1)}%  diff=${diff ? (diff.ratio*100).toFixed(1)+"%" : "n/a"}`);
    checkpointResults.push({ cp, oNB, tNB, diff: diff ? diff.ratio : null });
  }

  console.log("\n=== stable snapshot diff ===");
  const oStable = loadPng(ours.stableFile);
  const tStable2 = loadPng(theirs.stableFile);
  const stableDiff = diffPngs(oStable, tStable2, path.join(diffDir, "stable.diff.png"));
  console.log(`  stable diff = ${stableDiff ? (stableDiff.ratio*100).toFixed(1)+"%" : "n/a"}  -> ${diffDir}/stable.diff.png`);

  if (INTERACT) {
    console.log("\n=== interaction ===");
    console.log(`  ours click->change   ${ours.interactResult?.firstChangeMs ?? "n/a"}ms`);
    console.log(`  theirs click->change ${theirs.interactResult?.firstChangeMs ?? "n/a"}ms`);
    if (ours.interactResult?.afterFile && theirs.interactResult?.afterFile) {
      const oa = loadPng(ours.interactResult.afterFile);
      const ta = loadPng(theirs.interactResult.afterFile);
      const ad = diffPngs(oa, ta, path.join(diffDir, "after-click.diff.png"));
      console.log(`  after-click diff = ${ad ? (ad.ratio*100).toFixed(1)+"%" : "n/a"}`);
    }
  }

  const report = {
    urls: URLS, viewport: VIEWPORT, runs: RUNS,
    ours: { tDcl: ours.tDcl, tStable: ours.tStable, firstPaintMs: ours.firstPaintMs, totalBytes: ours.totalBytes, xhrTimings: ours.xhrTimings },
    theirs: { tDcl: theirs.tDcl, tStable: theirs.tStable, firstPaintMs: theirs.firstPaintMs, totalBytes: theirs.totalBytes, xhrTimings: theirs.xhrTimings },
    checkpointResults,
    stableDiffRatio: stableDiff ? stableDiff.ratio : null,
    interact: INTERACT ? { ours: ours.interactResult, theirs: theirs.interactResult } : null,
  };
  fs.writeFileSync(path.join(OUT_DIR, "report.json"), JSON.stringify(report, null, 2));
  console.error(`\nReport written to ${path.join(OUT_DIR, "report.json")}`);
  console.error(`Screenshots in ${OUT_DIR}/{ours,theirs,diff}/`);
}

main().catch((e) => { console.error(e); process.exit(1); });
