#!/usr/bin/env node
/**
 * Targeted trace for the graphics-clip-under-rotation bug.
 *
 * Runs the hellocodenameone JS-port test suite in Chromium, listens
 * for the `CN1SS:INFO:suite starting test=ClipUnderRotation` log, then
 * captures every CanvasRenderingContext2D op (save/restore/setTransform/
 * transform/translate/rotate/clip/rect/fillRect/...) until the test's
 * `chunks=` log fires. Writes the op timeline to disk so we can spot
 * what's leaving R+30 on the canvas vs. what should be at identity.
 *
 * Run hellocodenameone server before invoking, e.g.:
 *   cd /tmp/hcn-serve && python3 -m http.server 8766
 *
 * Usage:
 *   node scripts/clip-rotation-trace.mjs                  # localhost:8766
 *   node scripts/clip-rotation-trace.mjs --url <url>
 *   node scripts/clip-rotation-trace.mjs --out /tmp/ops.log
 */
import { chromium } from "playwright";
import fs from "node:fs";

const args = parseArgs(process.argv.slice(2));
const URL = args.url || "http://localhost:8766/HelloCodenameOne-js/";
const OUT = args.out || "/tmp/clip-rotation-ops.log";
const VIEWPORT = { width: 375, height: 667 };
const SUITE_TIMEOUT_MS = 480000;

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

const browser = await chromium.launch({ headless: true });
const ctx = await browser.newContext({ viewport: VIEWPORT, deviceScaleFactor: 1 });
const page = await ctx.newPage();

const ops = [];
const events = [];
await page.exposeBinding("__opLog", (_, e) => { ops.push(e); });
await page.exposeBinding("__evLog", (_, e) => { events.push(e); });

await page.addInitScript(() => {
  window.__cn1OpTraceArmed = false;
  const proto = CanvasRenderingContext2D.prototype;
  const seqRef = { n: 0 };
  // Assign each unique canvas a small numeric id so we can tell
  // foreground/off-screen contexts apart in the op log. Off-screen
  // image-backed canvases never appear in the DOM, so the heuristic
  // is "first context to appear that's the document's visible canvas
  // gets id=main, all others get id=N".
  const canvasIds = new WeakMap();
  let nextId = 0;
  function ctxId(ctx) {
    const c = ctx && ctx.canvas;
    if (!c) return "?";
    let id = canvasIds.get(c);
    if (id != null) return id;
    if (typeof document !== "undefined" && c.parentNode != null) {
      id = "main";
    } else {
      id = `off${nextId++}`;
    }
    canvasIds.set(c, id);
    return id;
  }
  function wrap(name) {
    const orig = proto[name];
    if (typeof orig !== "function") return;
    proto[name] = function (...a) {
      if (window.__cn1OpTraceArmed) {
        const simpleArgs = a.map((x) => {
          if (x == null) return null;
          if (typeof x === "number") return Number(x.toFixed(2));
          if (typeof x === "string" || typeof x === "boolean") return x;
          if (typeof x === "object") {
            if (x.width != null && x.height != null && x.data != null) return `<ImageData ${x.width}x${x.height}>`;
            if (x.tagName) return `<${x.tagName}>`;
            return `<${(x.constructor && x.constructor.name) || "obj"}>`;
          }
          return String(x);
        });
        try {
          window.__opLog({ seq: seqRef.n++, m: name, a: simpleArgs, t: performance.now(), ctx: ctxId(this) });
        } catch {}
      }
      return orig.apply(this, a);
    };
  }
  ["save", "restore", "beginPath", "clip", "rect", "fillRect", "strokeRect", "clearRect",
   "fill", "stroke", "setTransform", "transform", "translate", "scale", "rotate",
   "drawImage", "fillText", "strokeText", "moveTo", "lineTo", "closePath"]
    .forEach(wrap);
});

const t0 = Date.now();
let armedAt = null, disarmedAt = null;
page.on("console", (m) => {
  const t = m.text();
  const ms = Date.now() - t0;
  events.push({ ms, type: m.type(), text: t });
  // Light progress log so we can see where we are in the suite from
  // the script output too.
  if (t.startsWith("CN1SS:INFO:suite starting") || t.includes("graphics-clip-under-rotation")) {
    console.error(`+${ms}ms ${t.substring(0, 100)}`);
  }
  if (t.includes("CN1SS:INFO:suite starting test=ClipUnderRotation")) {
    armedAt = ms;
    console.error(`+${ms}ms >>> ARMED tracer`);
    page.evaluate(() => { window.__cn1OpTraceArmed = true; }).catch((e) => console.error("arm-eval-err", e.message));
  }
  // Stop tracing once chunks have been emitted (test screenshot done)
  if (t.includes("CN1SS:INFO:test=graphics-clip-under-rotation chunks=") && armedAt) {
    disarmedAt = ms;
    console.error(`+${ms}ms >>> DISARMED tracer`);
    page.evaluate(() => { window.__cn1OpTraceArmed = false; }).catch(() => {});
  }
});

await page.goto(URL, { waitUntil: "domcontentloaded", timeout: 60000 });

// Wait until either tracing finished or timeout
while (Date.now() - t0 < SUITE_TIMEOUT_MS) {
  if (disarmedAt) {
    // Give a small drain buffer
    await new Promise(r => setTimeout(r, 250));
    break;
  }
  await new Promise(r => setTimeout(r, 200));
}

await browser.close();

if (!armedAt) {
  console.error("Did not see ClipUnderRotation start within timeout. Captured ops:", ops.length);
  process.exit(1);
}

console.error(`Tracing armed at +${armedAt}ms, disarmed at +${disarmedAt || "timeout"}ms`);
console.error(`Captured ${ops.length} canvas ops`);

// Write op log (with ctx id per op)
const lines = ops.map((o) => {
  return `${Math.round(o.t).toString().padStart(6)}ms [${(o.ctx || "?").padEnd(4)}] #${o.seq.toString().padStart(5)} ${o.m}(${o.a.map((a) => (typeof a === "number" ? a.toFixed(2) : JSON.stringify(a))).join(", ")})`;
});
fs.writeFileSync(OUT, lines.join("\n"));
console.error(`Wrote op log to ${OUT}`);

// Per-ctx op count
const byCtx = new Map();
for (const o of ops) {
  byCtx.set(o.ctx || "?", (byCtx.get(o.ctx || "?") || 0) + 1);
}
console.error("Ops per context:");
for (const [k, v] of [...byCtx.entries()].sort((a, b) => b[1] - a[1])) {
  console.error(`  ${k}: ${v}`);
}

// Quick analysis: count setTransform calls and their matrices
const setTransforms = ops.filter(o => o.m === "setTransform");
console.error(`\nsetTransform calls: ${setTransforms.length}`);
const matrixCounts = new Map();
for (const op of setTransforms) {
  const key = op.a.slice(0, 4).map(v => Number(v).toFixed(3)).join(",");
  matrixCounts.set(key, (matrixCounts.get(key) || 0) + 1);
}
console.error("Unique setTransform matrices (a,b,c,d - skipping tx,ty):");
for (const [k, c] of [...matrixCounts.entries()].sort((a, b) => b[1] - a[1]).slice(0, 10)) {
  console.error(`  ${k}: ${c} calls`);
}

// Count save/restore pairs
const saves = ops.filter(o => o.m === "save").length;
const restores = ops.filter(o => o.m === "restore").length;
console.error(`\nsave: ${saves}, restore: ${restores}, balance: ${saves - restores}`);
