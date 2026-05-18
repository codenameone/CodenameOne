#!/usr/bin/env node
/**
 * Quick boot-time trace: prints relative-millisecond log lines for
 * each network response and each PARPAR-LIFECYCLE console message
 * during Initializr load. Useful for spotting where the worker
 * spends time during cold-start, especially the gap between
 * ``translated_app.js`` arriving and ``main-thread-completed``.
 *
 * Usage:
 *   node scripts/initializr-boot-trace.mjs                              # localhost:8765
 *   node scripts/initializr-boot-trace.mjs <url>                        # any URL
 *
 * Typical output (post-bulk-read-fix, localhost):
 *   104ms RESP 200 initializr-app/translated_app.js
 *   138ms PARPAR-LIFECYCLE:start:invoking-main-method=...
 *   197ms RESP 200 initializr-app/theme.res         <-- worker XHR fires
 *   316ms RESP 200 initializr-app/theme.res         <-- 119ms parse window
 *   320ms RESP 200 assets/iOS7Theme.res
 *   635ms PARPAR-LIFECYCLE:main-thread-completed
 */
import { chromium } from "playwright";

const url = process.argv[2] || "http://localhost:8765/initializr-app/";
const b = await chromium.launch({ headless: true });
const ctx = await b.newContext({ viewport: { width: 1280, height: 900 } });
const page = await ctx.newPage();
const t0 = Date.now();
page.on("console", (m) => {
  const t = m.text();
  if (t.startsWith("PARPAR") || t.includes("LIFECYCLE")) {
    console.log((Date.now() - t0) + "ms", t);
  }
});
page.on("response", (resp) => {
  const u = resp.url();
  if (u.startsWith("data:")) return;
  if (u.includes("cloudflareinsights")) return;
  console.log((Date.now() - t0) + "ms RESP", resp.status(), u.split("/").slice(-2).join("/"));
});
await page.goto(url, { waitUntil: "domcontentloaded" });
console.log((Date.now() - t0) + "ms DOMContentLoaded");
await new Promise((r) => setTimeout(r, 4000));
await b.close();
