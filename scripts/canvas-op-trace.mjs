#!/usr/bin/env node
/**
 * Instrument the page's CanvasRenderingContext2D to log every paint
 * operation (fillRect, strokeRect, clip, save, restore, setTransform,
 * drawImage, ...) during a brief window after a click. Outputs a
 * timeline of ops with arguments so we can spot what's being drawn
 * (or not drawn) when a UI element renders broken.
 *
 * Usage:
 *   node scripts/canvas-op-trace.mjs <url> <x> <y> [--ms 1500] [--browser firefox]
 */
import { chromium, firefox } from "playwright";
import fs from "node:fs";

const args = process.argv.slice(2);
const url = args[0] || "https://pr-4795-website-preview.codenameone.pages.dev/initializr-app/";
const tx = Number(args[1] || 700);
const ty = Number(args[2] || 100);
const traceMs = Number(args.includes("--ms") ? args[args.indexOf("--ms") + 1] : 1500);
const browserName = args.includes("--browser") ? args[args.indexOf("--browser") + 1] : "chromium";
const outFile = args.includes("--out") ? args[args.indexOf("--out") + 1] : "/tmp/canvas-ops.log";

const launcher = browserName === "firefox" ? firefox : chromium;
const browser = await launcher.launch({ headless: true });
const ctx = await browser.newContext({ viewport: { width: 1280, height: 900 }, deviceScaleFactor: 1 });
const page = await ctx.newPage();

const ops = [];
await page.exposeBinding("__opLog", (_, e) => { ops.push(e); });

// Tracer runs in the page. It wraps the prototype methods of
// CanvasRenderingContext2D and posts a record per call. To keep noise
// down, only enabled when window.__cn1OpTraceArmed === true.
await page.addInitScript(() => {
  window.__cn1OpTraceArmed = false;
  const proto = CanvasRenderingContext2D.prototype;
  const seqRef = { n: 0 };
  function wrap(name) {
    const orig = proto[name];
    if (typeof orig !== "function") return;
    proto[name] = function (...a) {
      if (window.__cn1OpTraceArmed) {
        // Stringify args succinctly; skip image data / canvas refs
        const simpleArgs = a.map((x) => {
          if (x == null) return null;
          if (typeof x === "number" || typeof x === "string" || typeof x === "boolean") return x;
          if (typeof x === "object") {
            if (x.width != null && x.height != null && x.data != null) return `<ImageData ${x.width}x${x.height}>`;
            if (x instanceof Element || (x.tagName && x.getContext)) return `<${x.tagName || "Element"}>`;
            return `<${x.constructor && x.constructor.name || "obj"}>`;
          }
          return String(x);
        });
        try {
          window.__opLog({ seq: seqRef.n++, m: name, a: simpleArgs, t: performance.now() });
        } catch (_e) {}
      }
      return orig.apply(this, a);
    };
  }
  // Track transform/clip-relevant ops
  ["save", "restore", "beginPath", "clip", "rect", "fillRect", "strokeRect", "clearRect",
   "fill", "stroke", "setTransform", "transform", "translate", "scale", "rotate",
   "drawImage", "fillText", "strokeText", "moveTo", "lineTo", "closePath",
   "putImageData", "createImageData", "arc", "ellipse"]
    .forEach(wrap);
  // Property setter tracing for fillStyle/strokeStyle (less useful but
  // helps when colour is wrong)
  // Skipped for brevity.
});

const t0 = Date.now();
page.on("console", (m) => {
  const text = m.text();
  if (text.startsWith("PARPAR") || m.type() === "error") {
    console.log(`${Date.now() - t0}ms [${m.type()}] ${text.substring(0, 200)}`);
  }
});

await page.goto(url, { waitUntil: "domcontentloaded", timeout: 30000 });

// Wait until stable
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

const box = await page.evaluate(() => {
  const c = document.querySelector("canvas");
  const r = c.getBoundingClientRect();
  return { x: r.left, y: r.top };
});

// Arm the tracer, click, wait, disarm
await page.evaluate(() => { window.__cn1OpTraceArmed = true; });
console.log(`tracing armed; clicking (${tx}, ${ty})`);
await page.mouse.click(box.x + tx, box.y + ty);
await new Promise(r => setTimeout(r, traceMs));
await page.evaluate(() => { window.__cn1OpTraceArmed = false; });

await browser.close();

// Write ops to file
const lines = ops.map((o) => {
  return `${Math.round(o.t).toString().padStart(6)}ms #${o.seq.toString().padStart(5)} ${o.m}(${o.a.map((a) => (typeof a === "number" ? a.toFixed(2) : JSON.stringify(a))).join(", ")})`;
});
fs.writeFileSync(outFile, lines.join("\n"));
console.log(`captured ${ops.length} canvas ops → ${outFile}`);
