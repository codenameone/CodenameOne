#!/usr/bin/env node
/**
 * Cross-browser perf bench for the JS-port Initializr.
 *
 * Loads the same URL in Chromium, Firefox, and (optionally) Webkit;
 * captures phase timings from `PARPAR-LIFECYCLE` console markers plus
 * canvas-pixel-stable detection. Outputs a side-by-side comparison so
 * we can localize browser-specific slowdowns (e.g. "main-class invoke
 * 7s in Firefox, 0.5s in Chrome").
 *
 * Phases captured:
 *   - DOMContentLoaded
 *   - runtime-script-loaded (PARPAR-LIFECYCLE)
 *   - start:invoking-main-method (PARPAR-LIFECYCLE)
 *   - start:drain-returned (PARPAR-LIFECYCLE)
 *   - main-thread-completed (PARPAR-LIFECYCLE)
 *   - first non-blank canvas paint
 *   - canvas stable (no change for 1500ms)
 *
 * Usage:
 *   node scripts/initializr-cross-browser-bench.mjs
 *   node scripts/initializr-cross-browser-bench.mjs --runs 3
 *   node scripts/initializr-cross-browser-bench.mjs --url <url> --browsers chromium,firefox
 *   node scripts/initializr-cross-browser-bench.mjs --json /tmp/out.json
 */
import { chromium, firefox, webkit } from "playwright";
import fs from "node:fs";

const args = parseArgs(process.argv.slice(2));
const URL = args.url || "https://pr-4795-website-preview.codenameone.pages.dev/initializr-app/";
const RUNS = Number(args.runs || 1);
const BROWSERS = (args.browsers || "chromium,firefox").split(",").map(s => s.trim());
const VIEWPORT = { width: 1280, height: 900 };
const MAX_WAIT_MS = 60000;

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
  if (!launcher) throw new Error(`Unknown browser: ${browserName}`);
  const browser = await launcher.launch({ headless: true });
  const ctx = await browser.newContext({ viewport: VIEWPORT, deviceScaleFactor: 1 });
  const page = await ctx.newPage();
  const t0 = Date.now();
  const phases = {};
  function record(name, ms) {
    if (!(name in phases)) phases[name] = ms;
  }
  page.on("console", (m) => {
    const t = m.text();
    if (!t.startsWith("PARPAR-LIFECYCLE")) return;
    const ms = Date.now() - t0;
    if (t.includes("runtime-script-loaded")) record("runtime", ms);
    else if (t.includes("invoking-main-method")) record("mainInvoke", ms);
    else if (t.includes("drain-returned")) record("drainReturn", ms);
    else if (t.includes("main-thread-completed")) record("mainComplete", ms);
  });

  await page.goto(URL, { waitUntil: "domcontentloaded", timeout: 60000 });
  record("dcl", Date.now() - t0);

  let firstPaint = null;
  let lastSig = -1, lastChange = Date.now() - t0;
  while (Date.now() - t0 < MAX_WAIT_MS) {
    const probe = await page.evaluate(() => {
      const c = document.querySelector("canvas");
      if (!c || c.width === 0) return { sig: 0, nonBlank: 0 };
      try {
        const ctx = c.getContext("2d");
        const img = ctx.getImageData(0, 0, c.width, c.height).data;
        let s = 0, nb = 0;
        for (let i = 0; i < img.length; i += 32) {
          s = ((s * 31) ^ (img[i] + img[i + 1] * 256 + img[i + 2] * 65536)) | 0;
          const lum = (img[i] + img[i + 1] + img[i + 2]) / 3;
          if (img[i + 3] > 0 && lum < 240) nb++;
        }
        return { sig: s, nonBlank: nb / (img.length / 32) };
      } catch { return { sig: 0, nonBlank: 0 }; }
    });
    if (firstPaint === null && probe.nonBlank > 0.02) firstPaint = Date.now() - t0;
    if (probe.sig !== lastSig) { lastSig = probe.sig; lastChange = Date.now() - t0; }
    if (firstPaint !== null && (Date.now() - t0 - lastChange) > 1500) break;
    await new Promise(r => setTimeout(r, 50));
  }
  record("firstPaint", firstPaint);
  record("stable", Date.now() - t0);

  await browser.close();
  return { browser: browserName, phases };
}

async function runMany(browserName, runs) {
  const results = [];
  for (let i = 0; i < runs; i++) {
    process.stderr.write(`  [${browserName} ${i + 1}/${runs}]\n`);
    results.push(await measureOnce(browserName));
  }
  // Average numeric phases
  const avg = { browser: browserName, phases: {} };
  const phaseKeys = ["dcl", "runtime", "mainInvoke", "drainReturn", "mainComplete", "firstPaint", "stable"];
  for (const k of phaseKeys) {
    const xs = results.map(r => r.phases[k]).filter(v => v != null);
    avg.phases[k] = xs.length ? Math.round(xs.reduce((a, b) => a + b, 0) / xs.length) : null;
  }
  return avg;
}

function fmtMs(v) { return v == null ? "  n/a" : String(v).padStart(6) + "ms"; }

async function main() {
  console.error(`Cross-browser bench (${RUNS} run(s) per browser)`);
  console.error(`  url: ${URL}`);
  console.error(`  browsers: ${BROWSERS.join(", ")}\n`);

  const results = [];
  for (const b of BROWSERS) {
    results.push(await runMany(b, RUNS));
  }

  console.log("\n=== phase timings (averaged) ===\n");
  const header = "  phase            " + BROWSERS.map(b => b.padStart(11)).join("");
  console.log(header);
  const phaseRows = [
    ["dcl",          "DOMContentLoaded"],
    ["runtime",      "runtime-loaded"],
    ["mainInvoke",   "main-invoked"],
    ["drainReturn",  "drain-returned"],
    ["mainComplete", "main-completed"],
    ["firstPaint",   "first-paint"],
    ["stable",       "canvas-stable"],
  ];
  for (const [k, label] of phaseRows) {
    const cells = results.map(r => fmtMs(r.phases[k]));
    console.log(`  ${label.padEnd(18)}` + cells.join(""));
  }

  // Compute slowdown ratios vs first browser (usually chromium)
  if (BROWSERS.length >= 2) {
    console.log("\n=== slowdown ratios (vs " + BROWSERS[0] + ") ===\n");
    const base = results[0];
    const ratioHeader = "  phase            ";
    console.log(ratioHeader + BROWSERS.slice(1).map(b => b.padStart(11)).join(""));
    for (const [k, label] of phaseRows) {
      const baseMs = base.phases[k];
      if (baseMs == null || baseMs <= 0) continue;
      const cells = results.slice(1).map(r => {
        const x = r.phases[k];
        if (x == null) return "n/a".padStart(11);
        const ratio = x / baseMs;
        return (ratio.toFixed(2) + "x").padStart(11);
      });
      console.log(`  ${label.padEnd(18)}` + cells.join(""));
    }
  }

  if (args.json) {
    fs.writeFileSync(args.json, JSON.stringify({ url: URL, runs: RUNS, results }, null, 2));
    console.error(`\nJSON written to ${args.json}`);
  }
}

main().catch(e => { console.error(e); process.exit(1); });
