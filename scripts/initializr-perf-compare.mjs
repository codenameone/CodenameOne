#!/usr/bin/env node
/**
 * Performance comparison between the JS port (PR preview) and the
 * production TeaVM build of Initializr. Loads each URL in a fresh
 * Chromium context, measures download / parse / first-paint /
 * time-to-interactive metrics, and reports a side-by-side delta.
 *
 * The script targets the iframe app URLs directly so the wrapping
 * Hugo page (header, loader CSS, cn1-site script tags) doesn't skew
 * the comparison.
 *
 * Usage:
 *   node scripts/initializr-perf-compare.mjs           # default URLs
 *   node scripts/initializr-perf-compare.mjs --runs 3  # avg of N runs
 *   node scripts/initializr-perf-compare.mjs --ours <url> --theirs <url>
 */

import { chromium } from "playwright";
import fs from "node:fs";

const args = parseArgs(process.argv.slice(2));
const URLS = {
  "ours-js-port": args.ours || "https://pr-4795-website-preview.codenameone.pages.dev/initializr-app/",
  "theirs-teavm": args.theirs || "https://www.codenameone.com/initializr-app/",
};
const RUNS = Number(args.runs || 1);
const VIEWPORT = { width: 1280, height: 900 };

function parseArgs(argv) {
  const out = {};
  for (let i = 0; i < argv.length; i++) {
    if (argv[i].startsWith("--")) {
      const k = argv[i].slice(2);
      const v = argv[i + 1] && !argv[i + 1].startsWith("--") ? argv[++i] : "1";
      out[k] = v;
    }
  }
  return out;
}

async function measureOne(url, label) {
  const browser = await chromium.launch({ headless: true });
  const ctx = await browser.newContext({
    viewport: VIEWPORT,
    deviceScaleFactor: 1,
    bypassCSP: true,
    // Disable HTTP cache for a clean cold-load measurement.
    storageState: undefined,
  });
  await ctx.addInitScript(() => {
    window.__perfMarks = [];
    window.__paintFrames = [];
  });

  const page = await ctx.newPage();

  let totalBytes = 0;
  const resources = [];
  page.on("response", async (resp) => {
    try {
      const headers = resp.headers();
      const len = Number(headers["content-length"] || 0);
      const url = resp.url();
      resources.push({
        url,
        status: resp.status(),
        contentType: headers["content-type"] || "",
        length: len,
      });
      if (len > 0) totalBytes += len;
    } catch (e) { /* ignore */ }
  });

  const consoleErrors = [];
  const consoleLogs = [];
  page.on("console", (msg) => {
    const t = msg.text();
    if (msg.type() === "error") consoleErrors.push(t);
    if (t.startsWith("PARPAR")) consoleLogs.push(t);
  });

  const t0 = Date.now();
  await page.goto(url, { waitUntil: "domcontentloaded", timeout: 60000 });
  const tDcl = Date.now() - t0;

  // Wait for canvas existence + first meaningful render (where canvas
  // has non-white pixels). We poll because the canvas paints over time
  // as the worker drains paint ops.
  let tCanvasFound = null;
  let tFirstPaint = null;
  let tLastSubstantialChange = null;
  let canvasSigHistory = [];
  const POLL_MS = 50;
  const TIMEOUT_MS = 60000;
  let lastSig = -1;
  while (Date.now() - t0 < TIMEOUT_MS) {
    const r = await page.evaluate(() => {
      const c = document.querySelector("canvas");
      if (!c) return { exists: false };
      if (c.width === 0 || c.height === 0) return { exists: true, sig: 0, paintRatio: 0 };
      try {
        const ctx = c.getContext("2d");
        const img = ctx.getImageData(0, 0, c.width, c.height).data;
        let sig = 0;
        let nonBlankPixels = 0;
        for (let i = 0; i < img.length; i += 32) {
          sig = ((sig * 31) ^ (img[i] + img[i + 1] * 256 + img[i + 2] * 65536)) | 0;
          // Count pixels with alpha > 0 and brightness < ~240 (i.e. not pure-white background).
          const lum = (img[i] + img[i + 1] + img[i + 2]) / 3;
          if (img[i + 3] > 0 && lum < 240) nonBlankPixels++;
        }
        return { exists: true, sig, paintRatio: nonBlankPixels / (img.length / 32), w: c.width, h: c.height };
      } catch (e) {
        return { exists: true, sig: 0, paintRatio: 0, err: e.message };
      }
    });
    if (r.exists && tCanvasFound === null) tCanvasFound = Date.now() - t0;
    if (r.exists && r.paintRatio > 0.02 && tFirstPaint === null) {
      tFirstPaint = Date.now() - t0;
    }
    if (r.exists && r.sig !== lastSig) {
      lastSig = r.sig;
      tLastSubstantialChange = Date.now() - t0;
      canvasSigHistory.push({ ms: tLastSubstantialChange, sig: r.sig, paintRatio: r.paintRatio });
    }
    // Done when no canvas change for 1500ms AND first paint already happened
    if (tFirstPaint !== null && Date.now() - t0 - tLastSubstantialChange > 1500) break;
    await new Promise((r) => setTimeout(r, POLL_MS));
  }
  const tStable = Date.now() - t0;

  // Collect performance + heap metrics
  const perf = await page.evaluate(() => {
    const nav = performance.getEntriesByType("navigation")[0] || {};
    const paints = performance.getEntriesByType("paint");
    const fcp = paints.find((p) => p.name === "first-contentful-paint");
    const heap = (performance.memory && performance.memory.usedJSHeapSize) || 0;
    const heapLimit = (performance.memory && performance.memory.jsHeapSizeLimit) || 0;
    return {
      navigationStart: nav.startTime || 0,
      domContentLoaded: nav.domContentLoadedEventEnd - nav.startTime,
      loadEvent: nav.loadEventEnd - nav.startTime,
      transferSize: nav.transferSize || 0,
      encodedBodySize: nav.encodedBodySize || 0,
      decodedBodySize: nav.decodedBodySize || 0,
      fcp: fcp ? fcp.startTime : null,
      jsHeapUsed: heap,
      jsHeapLimit: heapLimit,
    };
  });

  // Measure click → first canvas change latency (only if canvas painted).
  let clickLatency = null;
  if (tFirstPaint !== null) {
    try {
      const box = await page.evaluate(() => {
        const c = document.querySelector("canvas");
        const r = c.getBoundingClientRect();
        return { x: r.left, y: r.top, w: r.width, h: r.height };
      });
      // Click middle of canvas
      const tBefore = await page.evaluate(() => {
        const c = document.querySelector("canvas");
        const ctx = c.getContext("2d");
        const img = ctx.getImageData(0, 0, c.width, c.height).data;
        let sig = 0;
        for (let i = 0; i < img.length; i += 32) sig = ((sig * 31) ^ img[i]) | 0;
        return sig;
      });
      const tClick = Date.now();
      await page.mouse.click(box.x + box.w / 2, box.y + box.h / 2);
      for (let i = 0; i < 60; i++) {
        await new Promise((r) => setTimeout(r, 50));
        const tAfter = await page.evaluate(() => {
          const c = document.querySelector("canvas");
          const ctx = c.getContext("2d");
          const img = ctx.getImageData(0, 0, c.width, c.height).data;
          let sig = 0;
          for (let i = 0; i < img.length; i += 32) sig = ((sig * 31) ^ img[i]) | 0;
          return sig;
        });
        if (tAfter !== tBefore) { clickLatency = Date.now() - tClick; break; }
      }
    } catch (e) { /* ignore click measure errors */ }
  }

  await browser.close();

  // Bucket resources by suffix
  const byExt = {};
  for (const r of resources) {
    const u = r.url;
    const m = u.match(/\.([a-zA-Z0-9]+)(?:\?|$)/);
    const ext = m ? m[1] : "_no_ext_";
    if (!byExt[ext]) byExt[ext] = { count: 0, bytes: 0 };
    byExt[ext].count++;
    byExt[ext].bytes += r.length;
  }

  return {
    label,
    url,
    tDcl,
    tCanvasFound,
    tFirstPaint,
    tStable,
    clickLatency,
    totalBytes,
    resourceCount: resources.length,
    byExt,
    perf,
    consoleErrors: consoleErrors.slice(0, 5),
    parparLogs: consoleLogs.slice(0, 30),
  };
}

function fmtMs(v) { return v == null ? "n/a" : String(v).padStart(6) + "ms"; }
function fmtKb(v) { return v == null ? "n/a" : (v / 1024).toFixed(0).padStart(6) + "KB"; }

function reportRow(label, ours, theirs, fmt) {
  const o = fmt(ours), t = fmt(theirs);
  let delta = "";
  if (typeof ours === "number" && typeof theirs === "number" && ours && theirs) {
    const pct = ((ours - theirs) / theirs) * 100;
    delta = (ours > theirs ? "+" : "") + pct.toFixed(0) + "% vs theirs";
  }
  console.log(`  ${label.padEnd(28)} ours=${o}  theirs=${t}  ${delta}`);
}

async function runMany(url, label, runs) {
  const results = [];
  for (let i = 0; i < runs; i++) {
    process.stderr.write(`  [${label} run ${i + 1}/${runs}] ...`);
    const r = await measureOne(url, label);
    results.push(r);
    process.stderr.write(` tFirstPaint=${r.tFirstPaint}ms tStable=${r.tStable}ms\n`);
  }
  // Average numeric metrics
  const avg = { label, url };
  const numericKeys = ["tDcl", "tCanvasFound", "tFirstPaint", "tStable", "clickLatency", "totalBytes", "resourceCount"];
  for (const k of numericKeys) {
    const xs = results.map((r) => r[k]).filter((v) => v != null);
    avg[k] = xs.length ? Math.round(xs.reduce((a, b) => a + b, 0) / xs.length) : null;
  }
  avg.perf = results[0].perf;
  avg.byExt = results[0].byExt;
  avg.consoleErrors = results[0].consoleErrors;
  avg.parparLogs = results[0].parparLogs;
  return avg;
}

async function main() {
  console.error(`Initializr perf comparison (${RUNS} run${RUNS > 1 ? "s avg" : ""})`);
  console.error(`  ours:   ${URLS["ours-js-port"]}`);
  console.error(`  theirs: ${URLS["theirs-teavm"]}`);
  console.error("");

  const ours = await runMany(URLS["ours-js-port"], "ours", RUNS);
  const theirs = await runMany(URLS["theirs-teavm"], "theirs", RUNS);

  console.log("\n=== perf comparison ===\n");
  reportRow("DOMContentLoaded", ours.tDcl, theirs.tDcl, fmtMs);
  reportRow("canvas first found", ours.tCanvasFound, theirs.tCanvasFound, fmtMs);
  reportRow("canvas first paint", ours.tFirstPaint, theirs.tFirstPaint, fmtMs);
  reportRow("canvas stable", ours.tStable, theirs.tStable, fmtMs);
  reportRow("click->change latency", ours.clickLatency, theirs.clickLatency, fmtMs);
  reportRow("total transfer", ours.totalBytes, theirs.totalBytes, fmtKb);
  reportRow("resource count", ours.resourceCount, theirs.resourceCount, (v) => String(v).padStart(6));
  reportRow("FCP (browser)", ours.perf.fcp == null ? null : Math.round(ours.perf.fcp),
                              theirs.perf.fcp == null ? null : Math.round(theirs.perf.fcp), fmtMs);
  reportRow("JS heap used", ours.perf.jsHeapUsed, theirs.perf.jsHeapUsed, fmtKb);

  console.log("\n--- transfer by extension ---");
  const allExts = new Set([...Object.keys(ours.byExt), ...Object.keys(theirs.byExt)]);
  for (const ext of [...allExts].sort()) {
    const o = ours.byExt[ext] || { count: 0, bytes: 0 };
    const t = theirs.byExt[ext] || { count: 0, bytes: 0 };
    console.log(`  .${ext.padEnd(10)} ours=${String(o.count).padStart(3)}/${(o.bytes / 1024).toFixed(0).padStart(7)}KB    theirs=${String(t.count).padStart(3)}/${(t.bytes / 1024).toFixed(0).padStart(7)}KB`);
  }

  if (ours.consoleErrors.length) {
    console.log("\n--- ours console errors (first 5) ---");
    for (const e of ours.consoleErrors) console.log("  " + e.split("\n")[0].substring(0, 200));
  }

  if (args.json) {
    fs.writeFileSync(args.json, JSON.stringify({ ours, theirs }, null, 2));
    console.error(`\nFull JSON written to ${args.json}`);
  }
}

main().catch((e) => { console.error(e); process.exit(1); });
