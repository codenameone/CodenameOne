// Deep integration test for the JS-port "black bar" / "label-goes-transparent"
// regression: when the user clicks the first TextField on the Initializr
// landing page (Essentials -> "Main class" / "MyAppName") the native HTML
// editor launches and the canvas region above the field stops rendering,
// leaving the page background visible. Reproduces in headless Chrome.
//
// What this test does:
//   1. Serves the freshly-built Initializr-js bundle on a local HTTP port.
//   2. Loads it in headless Chromium with deviceScaleFactor=2 (the user
//      hits the bug at retina DPR; some clip-rect / paint paths only get
//      exercised when DPR != 1).
//   3. Captures every browser console message and pageerror -- including
//      "unreachable code after return statement" warnings, which the user
//      noticed while reproducing and suspects are diagnostic.
//   4. Waits for the main thread to fully settle, snapshots the canvas
//      region directly above the MyAppName text field.
//   5. Clicks the MyAppName text field at its known position, waits for
//      the native editor overlay to be created (<input class="cn1-edit-string">
//      attached to the document).
//   6. Snapshots the same region and computes:
//        - transparentFrac: pixel fraction with alpha == 0 (clearRect with
//          no follow-up fill -- the failure mode the user described).
//        - colorDeltaFrac: pixel fraction whose color changed by more than
//          a small threshold from before-click.
//      A clean repaint should leave the label area essentially unchanged
//      (delta near zero, transparent near zero). The bug surface: high
//      transparent fraction, high color delta.
//   7. Captures the canvas screenshot to a deterministic output path so a
//      human reviewer can see the artifact for any failing run.
//   8. Counts and reports any "unreachable code" warnings the bundle
//      emitted during boot or after the click.
//
// Exit code is 0 on PASS, non-zero with diagnostic output on FAIL.
//
// Usage: node scripts/test-blackbar-textfield.mjs [bundle.zip]

import { chromium } from 'playwright';
import fs from 'node:fs';
import path from 'node:path';
import os from 'node:os';
import { spawn, execSync } from 'node:child_process';

const REPO_ROOT = path.resolve(path.dirname(new URL(import.meta.url).pathname), '..');
const DEFAULT_BUNDLE = path.join(REPO_ROOT, 'scripts/initializr/javascript/target/initializr-javascript-port.zip');
const bundle = process.argv[2] || DEFAULT_BUNDLE;
if (!fs.existsSync(bundle)) {
  console.error('bundle not found:', bundle);
  process.exit(2);
}

const tmpDir = fs.mkdtempSync(path.join(os.tmpdir(), 'blackbar-test-'));
const bundleDir = path.join(tmpDir, 'bundle');
fs.mkdirSync(bundleDir);
execSync(`unzip -q "${bundle}" -d "${bundleDir}"`);
const distEntry = fs.readdirSync(bundleDir)
  .filter(n => fs.statSync(path.join(bundleDir, n)).isDirectory())[0];
const distDir = path.join(bundleDir, distEntry);

const PORT = 8773;
const server = spawn('python3', ['-m', 'http.server', String(PORT), '--directory', distDir],
    { stdio: ['ignore', 'ignore', 'pipe'] });
await new Promise(r => setTimeout(r, 800));

const browser = await chromium.launch({ headless: true });
const page = await browser.newPage({
  viewport: { width: 1280, height: 900 },
  deviceScaleFactor: 2,
});

const messages = [];
const unreachableWarnings = [];
const pageErrors = [];
page.on('console', msg => {
  const text = msg.text();
  const type = msg.type();
  messages.push(`[${type}] ${text}`);
  // Firefox-style and Chrome-style warning text varies. Match both.
  if (/unreachable code|Unreachable code/.test(text)) {
    unreachableWarnings.push({ type, text, location: msg.location() });
  }
});
page.on('pageerror', err => {
  pageErrors.push(err.message);
  messages.push(`[pageerror] ${err.message}`);
  if (/unreachable code|Unreachable code/.test(err.message)) {
    unreachableWarnings.push({ type: 'error', text: err.message, location: null });
  }
});
page.on('worker', worker => {
  worker.on('console', msg => {
    const text = msg.text();
    messages.push(`[worker:${msg.type()}] ${text}`);
    if (/unreachable code|Unreachable code/.test(text)) {
      unreachableWarnings.push({ type: 'worker:' + msg.type(), text, location: msg.location() });
    }
  });
});

// Wrap canvas 2d-context primitives so we can capture every clearRect /
// fillRect / drawImage that hits the rect range we care about. This is
// strictly diagnostic: when the test fails, the dump tells us *which*
// op cleared the label region without a follow-up paint.
const opCaptureScript = `
window.__cn1OpLog = [];
const __startCapture = function() {
  const c = document.querySelector('canvas');
  if (!c || c.__captured) return;
  c.__captured = true;
  const ctx = c.getContext('2d');
  const labelYTop = ${`${0/* placeholder */}`};
  const wrap = (name) => {
    const orig = ctx[name].bind(ctx);
    ctx[name] = function() {
      const a = arguments;
      try {
        if (window.__cn1OpLog.length < 8000) {
          // For drawImage variants, the destination rect lives at a
          // different arg index depending on overload (3-arg, 5-arg,
          // 9-arg). Pull the dest rect for each shape so we record
          // *what got drawn where* rather than the source crop.
          let x = null, y = null, w = null, h = null;
          if (name === 'drawImage') {
            if (a.length === 3) { x = a[1]; y = a[2]; }
            else if (a.length === 5) { x = a[1]; y = a[2]; w = a[3]; h = a[4]; }
            else if (a.length === 9) { x = a[5]; y = a[6]; w = a[7]; h = a[8]; }
          } else if (name === 'putImageData') {
            x = a[1]; y = a[2];
            // Args 3-6 are dirty-rect (sx,sy,sw,sh) within the imageData.
          } else if (name === 'fillText' || name === 'strokeText') {
            x = a[1]; y = a[2];
          } else {
            // clearRect, fillRect, strokeRect, rect: (x, y, w, h)
            x = a[0]; y = a[1]; w = a[2]; h = a[3];
          }
          window.__cn1OpLog.push({ op: name,
              x: typeof x === 'number' ? x : null,
              y: typeof y === 'number' ? y : null,
              w: typeof w === 'number' ? w : null,
              h: typeof h === 'number' ? h : null,
              t: performance.now() });
        }
      } catch (_) {}
      return orig.apply(ctx, a);
    };
  };
  ['clearRect', 'fillRect', 'strokeRect', 'rect',
   'drawImage', 'putImageData', 'fillText', 'strokeText'].forEach(wrap);
};
// Start capture as soon as the canvas exists.
const __captureInterval = setInterval(() => {
  if (document.querySelector('canvas')) {
    __startCapture();
    clearInterval(__captureInterval);
  }
}, 50);
`;
await page.addInitScript(opCaptureScript);

console.log(`[blackbar] serving ${distDir} on :${PORT}`);
await page.goto(`http://127.0.0.1:${PORT}/`);

// Wait for the main-thread-completed lifecycle marker (or 60s timeout).
const bootStart = Date.now();
while (Date.now() - bootStart < 60_000) {
  if (messages.some(m => m.includes('main-thread-completed'))) break;
  await new Promise(r => setTimeout(r, 250));
}
// A few extra seconds for late paints (theme loaded -> form re-laid-out).
await new Promise(r => setTimeout(r, 3000));

// Resolve the canvas's position+size in page CSS pixels so the test is
// independent of any chrome around the canvas (header bar, padding, etc.).
const canvasBox = await page.evaluate(() => {
  const c = document.querySelector('canvas');
  if (!c) return null;
  const r = c.getBoundingClientRect();
  return { x: r.left, y: r.top, w: r.width, h: r.height,
           pxW: c.width, pxH: c.height,
           dpr: window.devicePixelRatio || 1 };
});
console.log('[blackbar] canvas box:', canvasBox);

// Layout in CANVAS-LOCAL CSS pixels (verified against /tmp/blackbar-before.png):
//   y ~  78        "Initializr - Scaffold a Project in Seconds" header
//   y ~ 120        subtitle
//   y ~ 195        "Essentials" subheader
//   y ~ 228        "Main Class" LABEL  <-- this is what disappears
//   y ~ 255        MyAppName TEXT FIELD <-- the click target
const LABEL_X_LOCAL = 60, LABEL_W_LOCAL = 540;
const LABEL_Y_TOP_LOCAL = 215, LABEL_Y_BOT_LOCAL = 245;
const HEADER_Y_TOP_LOCAL = 175, HEADER_Y_BOT_LOCAL = 205;
const FIELD_X_LOCAL = 220, FIELD_Y_LOCAL = 258;
// Convert to page coords (the click is dispatched against the page, not
// the canvas), and to canvas-buffer pixels (where getImageData operates,
// scaled by deviceScaleFactor).
const PROBE_X = canvasBox.x + LABEL_X_LOCAL;
const PROBE_W = LABEL_W_LOCAL;
const LABEL_Y_TOP = canvasBox.y + LABEL_Y_TOP_LOCAL;
const LABEL_Y_BOT = canvasBox.y + LABEL_Y_BOT_LOCAL;
const HEADER_Y_TOP = canvasBox.y + HEADER_Y_TOP_LOCAL;
const HEADER_Y_BOT = canvasBox.y + HEADER_Y_BOT_LOCAL;
const FIELD_X = canvasBox.x + FIELD_X_LOCAL;
const FIELD_Y = canvasBox.y + FIELD_Y_LOCAL;

async function sampleStrip(yTop, yBot) {
  return await page.evaluate(({ x, w, y0, y1 }) => {
    const c = document.querySelector('canvas');
    if (!c) return null;
    // Map page-CSS coords to canvas-buffer coords. The canvas's
    // bounding-rect width/height tell us the CSS size; c.width/c.height
    // tell us the buffer size. Their ratio is what we need to scale by --
    // *not* devicePixelRatio, because the JS port doesn't size its
    // backing buffer to match DPR (the canvas declares fixed
    // ``width=375 height=667`` in the HTML and is then resized by the
    // port logic to css-pixel dimensions).
    const r = c.getBoundingClientRect();
    const sx = c.width / r.width;
    const sy = c.height / r.height;
    const ctx = c.getContext('2d');
    const px = Math.max(0, Math.floor((x - r.left) * sx));
    const py0 = Math.max(0, Math.floor((y0 - r.top) * sy));
    const py1 = Math.min(c.height, Math.floor((y1 - r.top) * sy));
    const pw = Math.min(c.width - px, Math.floor(w * sx));
    const ph = py1 - py0;
    if (pw <= 0 || ph <= 0) return null;
    const img = ctx.getImageData(px, py0, pw, ph).data;
    let total = 0, transparent = 0, opaqueBlack = 0;
    let rSum = 0, gSum = 0, bSum = 0;
    // Build a 64-bin grayscale histogram so two snapshots can be diffed
    // without keeping the pixel buffer around.
    const hist = new Array(64).fill(0);
    for (let p = 0; p < img.length; p += 4) {
      total++;
      const r = img[p], g = img[p + 1], b = img[p + 2], a = img[p + 3];
      if (a === 0) transparent++;
      else {
        const lum = (r + g + b) / 3 | 0;
        if (lum < 8) opaqueBlack++;
        rSum += r; gSum += g; bSum += b;
        hist[(lum >> 2) & 63]++;
      }
    }
    const opaque = total - transparent;
    return {
      total,
      transparentFrac: transparent / total,
      opaqueBlackFrac: opaqueBlack / total,
      meanR: opaque ? rSum / opaque : 0,
      meanG: opaque ? gSum / opaque : 0,
      meanB: opaque ? bSum / opaque : 0,
      hist,
    };
  }, { x: PROBE_X, w: PROBE_W, y0: yTop, y1: yBot });
}
function histDistance(a, b) {
  if (!a || !b) return -1;
  let d = 0;
  for (let i = 0; i < a.length; i++) d += Math.abs(a[i] - b[i]);
  return d;
}

await page.locator('canvas').screenshot({ path: '/tmp/blackbar-before.png' }).catch(() => {});
const labelBefore = await sampleStrip(LABEL_Y_TOP, LABEL_Y_BOT);
const headerBefore = await sampleStrip(HEADER_Y_TOP, HEADER_Y_BOT);

// Snapshot the canvas-op log RIGHT NOW so we have the boot/initial-paint
// trace separate from the post-click trace below. The 8000-entry buffer
// rolls; without this we'd lose the initial label paint by the time we
// look at the after-click region.
const opsBeforeClick = await page.evaluate(() => {
  const out = (window.__cn1OpLog || []).slice();
  window.__cn1OpLog.length = 0;
  return out;
});
console.log(`[blackbar] captured ${opsBeforeClick.length} ops during boot/idle`);

console.log('[blackbar] label strip pre-click:',
    labelBefore && JSON.stringify({
      transparent: labelBefore.transparentFrac.toFixed(3),
      meanR: labelBefore.meanR.toFixed(0),
      meanG: labelBefore.meanG.toFixed(0),
      meanB: labelBefore.meanB.toFixed(0),
    }));

// Click the MyAppName text field. Single down/up at native input position.
await page.mouse.click(FIELD_X, FIELD_Y);

// Wait for the native edit overlay to be created. The cleanup path hides
// the input via display:none rather than removing it, so the first click
// should both create and show it. Poll for an attached <input> with the
// cn1-edit-string class.
let nativeEditorAppeared = false;
let nativeEditorBox = null;
for (let i = 0; i < 60; i++) {
  await new Promise(r => setTimeout(r, 100));
  const result = await page.evaluate(() => {
    const inp = document.querySelector('input.cn1-edit-string');
    if (!(inp && inp.style.display !== 'none' && inp.parentNode)) return null;
    const r = inp.getBoundingClientRect();
    return { x: r.left, y: r.top, w: r.width, h: r.height,
             cssTop: inp.style.top, cssLeft: inp.style.left,
             cssWidth: inp.style.width, cssHeight: inp.style.height };
  });
  if (result) { nativeEditorAppeared = true; nativeEditorBox = result; break; }
}
console.log('[blackbar] native input overlay appeared:', nativeEditorAppeared);
console.log('[blackbar] native input box (page CSS px):', nativeEditorBox);

// Give the worker a few extra paint frames in case the label clears as
// part of an *async* repaint after the editor is attached.
await new Promise(r => setTimeout(r, 1500));

await page.locator('canvas').screenshot({ path: '/tmp/blackbar-after.png' }).catch(() => {});
const labelAfter = await sampleStrip(LABEL_Y_TOP, LABEL_Y_BOT);
const headerAfter = await sampleStrip(HEADER_Y_TOP, HEADER_Y_BOT);

// Also click somewhere ELSE (a non-input area) to take focus off the
// textfield. If the label re-renders, this is a live focus-tracked
// rendering issue. If it stays transparent, the canvas is missing the
// label paint forever, not just during the editing window.
await page.keyboard.press('Tab');
await new Promise(r => setTimeout(r, 250));
await page.mouse.click(canvasBox.x + 950, canvasBox.y + 870);  // generate-button bottom bar
await new Promise(r => setTimeout(r, 500));
await page.mouse.click(canvasBox.x + 1000, canvasBox.y + 400);  // empty area in iphone preview
await new Promise(r => setTimeout(r, 1500));
await page.locator('canvas').screenshot({ path: '/tmp/blackbar-after-defocus.png' }).catch(() => {});
const labelAfterDefocus = await sampleStrip(LABEL_Y_TOP, LABEL_Y_BOT);
console.log('[blackbar] label strip post-defocus:',
    labelAfterDefocus && JSON.stringify({
      transparent: labelAfterDefocus.transparentFrac.toFixed(3),
      meanR: labelAfterDefocus.meanR.toFixed(0),
    }));

console.log('[blackbar] label strip post-click:',
    labelAfter && JSON.stringify({
      transparent: labelAfter.transparentFrac.toFixed(3),
      meanR: labelAfter.meanR.toFixed(0),
      meanG: labelAfter.meanG.toFixed(0),
      meanB: labelAfter.meanB.toFixed(0),
    }));
const headerHistDelta = histDistance(headerBefore && headerBefore.hist, headerAfter && headerAfter.hist);
const labelHistDelta = histDistance(labelBefore && labelBefore.hist, labelAfter && labelAfter.hist);
console.log('[blackbar] header-strip hist delta (control):', headerHistDelta);
console.log('[blackbar] label-strip hist delta (suspect):',  labelHistDelta);

// Pass criteria:
//   - native editor opened (precondition, otherwise we didn't repro)
//   - label-strip transparent fraction stays low (< 0.02)
//   - label-strip histogram delta is comparable to control header strip
//     (within 4x). On the bug, the label strip becomes mostly transparent
//     while the header is unchanged, so this ratio blows up.
//   - no "unreachable code" warnings emitted from the bundle.
const failures = [];
if (!nativeEditorAppeared) {
  failures.push('precondition: native input overlay never appeared after click');
}
if (labelAfter && labelAfter.transparentFrac > 0.02) {
  failures.push(`label strip is ${(labelAfter.transparentFrac * 100).toFixed(1)}% transparent post-click ` +
      `(was ${labelBefore ? (labelBefore.transparentFrac * 100).toFixed(1) : '?'}% before)`);
}
if (labelHistDelta >= 0 && headerHistDelta >= 0
    && headerHistDelta < 200 && labelHistDelta > headerHistDelta * 4) {
  failures.push(`label strip changed ${labelHistDelta} histogram bins vs ` +
      `${headerHistDelta} for the static header strip -- the label area was repainted ` +
      `or cleared while the surrounding form held still`);
}
if (unreachableWarnings.length > 0) {
  failures.push(`bundle emitted ${unreachableWarnings.length} ` +
      `"unreachable code" console warnings -- translator likely emits ` +
      `dead code after a return/throw and the JS engine flags it`);
}

console.log('[blackbar] unreachable-code warnings:', unreachableWarnings.length);
unreachableWarnings.slice(0, 5).forEach(w => {
  console.log(`  - ${w.type} ${w.text.slice(0, 200)}`);
  if (w.location && w.location.url) {
    console.log(`    at ${w.location.url}:${w.location.lineNumber}:${w.location.columnNumber}`);
  }
});
console.log('[blackbar] pageerror count:', pageErrors.length);
pageErrors.slice(0, 5).forEach(e => console.log('  -', e));

// Pull the captured canvas-op log. Filter to ops whose y range lands
// inside the label strip we sampled, so we can see the offending clear /
// fill events directly.
function filterToLabelBand(log, y0, y1) {
  return log.filter(o => {
    if (o.y == null) return false;
    if (o.y > y1 + 200) return false;
    if (o.h != null && o.y + o.h < Math.max(0, y0 - 100)) return false;
    if (o.h == null && o.y < Math.max(0, y0 - 100)) return false;
    return true;
  });
}
// Print the BEFORE-click label-band fillText/drawImage ops -- those are
// what painted "Main Class" originally. If after-click is missing one of
// them, we know which one isn't firing.
const beforeBand = filterToLabelBand(opsBeforeClick, LABEL_Y_TOP, LABEL_Y_BOT);
console.log('[blackbar] BEFORE-click ops in label band (' + beforeBand.length + ', filtered to fillText/drawImage):');
beforeBand.filter(o => o.op === 'fillText' || o.op === 'drawImage')
    .forEach(o => console.log(`  PRE  ${o.op}(${(o.x|0)}, ${(o.y|0)}, ${o.w|0}, ${o.h|0})  t+${(o.t|0)}`));

// Show the FULL BEFORE-click trace immediately around the label-text
// fillText (56, 222), so we can see what clip / save / restore brackets
// the label's text-paint and compare it to the after-click sequence.
const labelFillIdx = beforeBand.findIndex(o => o.op === 'fillText'
                                              && Math.abs(o.x - 56) < 6
                                              && Math.abs(o.y - 222) < 6);
if (labelFillIdx >= 0) {
  const start = Math.max(0, labelFillIdx - 12);
  const end = Math.min(beforeBand.length, labelFillIdx + 4);
  console.log(`[blackbar] PRE context around the "Main Class" fillText (idx ${labelFillIdx}):`);
  for (let i = start; i < end; i++) {
    const o = beforeBand[i];
    const marker = i === labelFillIdx ? '>>>' : '   ';
    console.log(`  ${marker} ${o.op}(${(o.x|0)}, ${(o.y|0)}, ${o.w|0}, ${o.h|0})  t+${(o.t|0)}`);
  }
}
const labelOps = await page.evaluate(({ y0Css, y1Css, fieldYCss }) => {
  const c = document.querySelector('canvas');
  const r = c.getBoundingClientRect();
  const sy = c.height / r.height;
  const y0 = (y0Css - r.top) * sy;
  const y1 = (y1Css - r.top) * sy;
  const log = (window.__cn1OpLog || []);
  // Filter: ops whose y is within 100px of the label band (or whose
  // y+h overlaps it for ops that record a height). Includes fillText
  // (no h) so we can see whether any text was actually drawn after
  // the label clear.
  return log.filter(o => {
              if (o.y == null) return false;
              if (o.y > y1 + 200) return false;
              if (o.h != null && o.y + o.h < Math.max(0, y0 - 100)) return false;
              if (o.h == null && o.y < Math.max(0, y0 - 100)) return false;
              return true;
            })
            .sort((a, b) => a.t - b.t)
            .slice(-300);
}, { y0Css: LABEL_Y_TOP, y1Css: LABEL_Y_BOT, fieldYCss: FIELD_Y });
console.log('[blackbar] last canvas ops touching label band (' + labelOps.length + '):');
labelOps.forEach(o => console.log(`  ${o.op}(${o.x | 0}, ${o.y | 0}, ${o.w | 0}, ${o.h | 0})  t+${(o.t | 0)}`));

// Also write the full message log so a human can grep for context.
fs.writeFileSync('/tmp/blackbar-messages.log', messages.join('\n'));
fs.writeFileSync('/tmp/blackbar-labelops.json', JSON.stringify(labelOps, null, 2));
console.log('[blackbar] full log -> /tmp/blackbar-messages.log');
console.log('[blackbar] before screenshot -> /tmp/blackbar-before.png');
console.log('[blackbar] after  screenshot -> /tmp/blackbar-after.png');

await browser.close();
server.kill();

if (failures.length === 0) {
  console.log('[blackbar] PASS');
  process.exit(0);
}
console.log('[blackbar] FAIL:');
failures.forEach(f => console.log('  -', f));
process.exit(1);
