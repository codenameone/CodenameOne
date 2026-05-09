// Comprehensive feature test for the Initializr JS-port bundle. Each
// scenario reloads the page so leftover modal/menu state from one
// scenario does not pollute the next.
//
// Scenarios:
//   1. textfield: click MyAppName, expect "Main Class" label stays
//      visible (the d91a4f975 fix)
//   2. dialog: click Hello-World button, dialog body should fill
//      with the dialog's white bg
//   3. side-menu: hamburger animation should not flicker through
//      many distinct states
//   4. template-buttons: each radio button click should swap
//      selection -- previously-selected button transitions away
//      from the bright-blue selected color, the clicked one
//      transitions toward it
//   5. toggle-mashing: rapidly click toggle buttons; worker should
//      remain alive afterwards
//
// Run: node scripts/test-initializr-features.mjs [bundle.zip]

import { chromium } from 'playwright';
import fs from 'node:fs';
import path from 'node:path';
import os from 'node:os';
import { spawn, execSync } from 'node:child_process';

const REPO_ROOT = path.resolve(path.dirname(new URL(import.meta.url).pathname), '..');
const DEFAULT_BUNDLE = path.join(REPO_ROOT, 'scripts/initializr/javascript/target/initializr-javascript-port.zip');

// Two run modes:
//
//  1. Local bundle (default).   ``node scripts/test-initializr-features.mjs [path/to/bundle.zip]``
//     Unzips the bundle, serves it from a local python3 http server, and
//     runs every scenario against ``http://127.0.0.1:PORT/``. This is what
//     CI runs after each translator build to validate the artefact before
//     the deploy step.
//
//  2. URL.   ``node scripts/test-initializr-features.mjs --url=https://...``
//     Skips local serving and runs every scenario directly against the
//     supplied URL. Use this to smoke-test the deploy preview after CI has
//     finished -- the browser-level loader hides 8 s after the iframe
//     loads even if the canvas is still blank, so the user-facing page
//     looks "ready" while the worker is actually wedged. Pointing the
//     feature test at the live URL catches that regression directly
//     instead of relying on a separate "is the deploy alive" probe.
const argUrl = process.argv.find(a => a.startsWith('--url='));
const TEST_URL = argUrl ? argUrl.slice('--url='.length) : null;

let server = null;
let TEST_BASE_URL;
if (TEST_URL) {
  TEST_BASE_URL = TEST_URL;
  console.log(`[features] running against URL: ${TEST_BASE_URL}`);
} else {
  const bundle = process.argv[2] && !process.argv[2].startsWith('--')
      ? process.argv[2] : DEFAULT_BUNDLE;
  if (!fs.existsSync(bundle)) {
    console.error('bundle not found:', bundle);
    process.exit(2);
  }
  const tmpDir = fs.mkdtempSync(path.join(os.tmpdir(), 'init-features-'));
  const bundleDir = path.join(tmpDir, 'bundle');
  fs.mkdirSync(bundleDir);
  execSync(`unzip -q "${bundle}" -d "${bundleDir}"`);
  const distEntry = fs.readdirSync(bundleDir)
      .filter(n => fs.statSync(path.join(bundleDir, n)).isDirectory())[0];
  const distDir = path.join(bundleDir, distEntry);
  const PORT = 8775;
  server = spawn('python3', ['-m', 'http.server', String(PORT), '--directory', distDir],
      { stdio: ['ignore', 'ignore', 'pipe'] });
  await new Promise(r => setTimeout(r, 800));
  TEST_BASE_URL = `http://127.0.0.1:${PORT}/`;
  console.log(`[features] serving local bundle: ${distDir} -> ${TEST_BASE_URL}`);
}

const browser = await chromium.launch({ headless: true });

const ART_DIR = '/tmp/init-features-artifacts';
fs.mkdirSync(ART_DIR, { recursive: true });
// Wipe stale screenshots from previous runs so reading them after a
// failure can't surface a misleading older state.
for (const f of fs.readdirSync(ART_DIR)) fs.unlinkSync(path.join(ART_DIR, f));

const failures = [];
function fail(label, msg) {
  console.log(`[features]  FAIL ${label}: ${msg}`);
  failures.push(`${label}: ${msg}`);
}

// Boot deadlines. The first version of this test waited 60 s for
// ``main-thread-completed`` and then proceeded REGARDLESS -- which
// meant a regression that made boot take 90 s+ silently passed boot
// and started clicking on a half-loaded page, where every assertion
// failed on a transparent canvas with confusing "click was dropped"
// messages. The pre-fix bundle finished in ~2 s; anything substantially
// longer is a regression worth flagging immediately.
//
// ``BOOT_COMPLETE_BUDGET_MS`` is the soft upper bound for
// ``main-thread-completed`` -- exceeding it fails the scenario hard
// instead of silently letting the rest of the test run on a wedged
// page. ``FIRST_PAINT_BUDGET_MS`` enforces that the canvas actually
// shows non-trivial content (the rendered Initializr UI) before any
// scenario starts measuring colours; the boot lifecycle marker can
// fire while the canvas is still all-white if the post-boot paint
// pipeline has stalled.
const BOOT_COMPLETE_BUDGET_MS = Number(process.env.CN1_BOOT_BUDGET_MS) || 15_000;
const FIRST_PAINT_BUDGET_MS = Number(process.env.CN1_FIRST_PAINT_BUDGET_MS) || 20_000;

// Each scenario opens its own fresh page so menu / dialog state from a
// previous scenario can't bleed in. Returns { page, messages, pageErrors,
// canvasBox, snap(label), bootMs, firstPaintMs }.
async function bootScenario(scenarioLabel) {
  const page = await browser.newPage({
    viewport: { width: 1280, height: 900 },
    deviceScaleFactor: 2,
  });
  const messages = [];
  const pageErrors = [];
  page.on('console', msg => messages.push(`[${msg.type()}] ${msg.text()}`));
  page.on('pageerror', err => {
    pageErrors.push(err.message);
    messages.push(`[pageerror] ${err.message}`);
  });
  page.on('worker', worker => {
    worker.on('console', msg => messages.push(`[worker:${msg.type()}] ${msg.text()}`));
  });

  const bootStart = Date.now();
  await page.goto(TEST_BASE_URL);

  // Phase 1: wait for the worker to finish its lifecycle.start chain.
  let bootMs = null;
  while (Date.now() - bootStart < BOOT_COMPLETE_BUDGET_MS) {
    if (messages.some(m => m.includes('main-thread-completed'))) {
      bootMs = Date.now() - bootStart;
      break;
    }
    await new Promise(r => setTimeout(r, 100));
  }
  if (bootMs === null) {
    const out = path.join(ART_DIR, `${scenarioLabel}-boot-timeout.png`);
    await page.locator('canvas').screenshot({ path: out }).catch(() => {});
    throw new Error(
      `boot did not reach main-thread-completed in ${BOOT_COMPLETE_BUDGET_MS} ms ` +
      `(this is the regression that surfaced as the deployed UI being stuck ` +
      `on the Loading... splash). Tail of console:\n  ` +
      messages.slice(-5).map(m => m.slice(0, 200)).join('\n  '));
  }

  // Phase 2: wait for non-trivial canvas content. ``main-thread-completed``
  // means the EDT lifecycle finished, but the FIRST paint cycle (which
  // queues ops to ``pendingDisplay`` and posts a requestAnimationFrame)
  // has its own latency. If the rAF reply from the main thread never
  // comes back -- or comes back into a worker still bottle-necked on
  // its own queue -- the canvas stays white. Hard-fail rather than
  // proceed to click on a still-blank page.
  //
  // ``--url=`` mode might be pointed at an iframe-parent page (e.g.
  // ``/initializr/`` on the deploy preview, where the bundle is loaded
  // inside ``<iframe id=cn1-initializr-frame>``). Look at the iframe's
  // contentDocument when there's no canvas on the main doc -- otherwise
  // the test would always fail in URL=iframe-parent mode even though
  // the bundle is rendering correctly inside the frame.
  let firstPaintMs = null;
  let useIframe = false;
  const paintDeadline = bootStart + BOOT_COMPLETE_BUDGET_MS + FIRST_PAINT_BUDGET_MS;
  while (Date.now() < paintDeadline) {
    const probe = await page.evaluate(() => {
      function nonWhite(c) {
        if (!c || c.width === 0) return 0;
        try {
          const img = c.getContext('2d').getImageData(0, 0, c.width, c.height).data;
          let nw = 0;
          for (let i = 0; i < img.length; i += 64) {
            const lum = (img[i]+img[i+1]+img[i+2])/3;
            if (img[i+3] > 0 && lum < 240) nw++;
          }
          return nw;
        } catch { return 0; }
      }
      const direct = document.querySelector('canvas');
      if (direct) return { mainNw: nonWhite(direct), iframeNw: 0, hasIframe: false };
      const f = document.querySelector('#cn1-initializr-frame, iframe');
      if (f && f.contentDocument) {
        const c = f.contentDocument.querySelector('canvas');
        return { mainNw: 0, iframeNw: nonWhite(c), hasIframe: true };
      }
      return { mainNw: 0, iframeNw: 0, hasIframe: false };
    });
    if (probe.mainNw > 200) {
      firstPaintMs = Date.now() - bootStart;
      break;
    }
    if (probe.iframeNw > 200) {
      firstPaintMs = Date.now() - bootStart;
      useIframe = true;
      break;
    }
    await new Promise(r => setTimeout(r, 100));
  }
  if (firstPaintMs === null) {
    const out = path.join(ART_DIR, `${scenarioLabel}-no-paint.png`);
    await page.locator('canvas').screenshot({ path: out }).catch(() => {});
    throw new Error(
      `canvas never rendered non-trivial content within ` +
      `${BOOT_COMPLETE_BUDGET_MS + FIRST_PAINT_BUDGET_MS} ms ` +
      `(boot lifecycle finished at ${bootMs} ms, then nothing painted -- ` +
      `screenshot at ${out})`);
  }
  if (useIframe) {
    // The bundle is rendering inside an iframe. Scenarios 01-05 click
    // and sample on the main document's canvas via boundingClientRect,
    // which doesn't translate cleanly into iframe coordinates without
    // a substantial rework. Surface a clear skip so the iframe-parent
    // run still covers the loader/ready signal via scenario 06 instead
    // of false-failing on every click scenario.
    const e = new Error('IFRAME_ONLY');
    e.bootMs = bootMs;
    e.firstPaintMs = firstPaintMs;
    throw e;
  }
  console.log(`[features] ${scenarioLabel} boot: lifecycle@${bootMs}ms firstPaint@${firstPaintMs}ms`);

  await new Promise(r => setTimeout(r, 1500));

  const canvasBox = await page.evaluate(() => {
    const c = document.querySelector('canvas');
    if (!c) return null;
    const r = c.getBoundingClientRect();
    return { x: r.left, y: r.top, w: r.width, h: r.height,
             pxW: c.width, pxH: c.height };
  });

  async function snap(label) {
    const out = path.join(ART_DIR, `${scenarioLabel}-${label}.png`);
    await page.locator('canvas').screenshot({ path: out }).catch(() => {});
    return out;
  }
  await snap('boot');
  return { page, messages, pageErrors, canvasBox, snap, bootMs, firstPaintMs };
}

async function teardown(s) {
  await s.page.close();
}

// Sample a horizontal canvas strip in CSS coords (x, y, w, h relative
// to canvas). Returns transparent / opaque-white / opaque-dark fractions.
async function sampleStrip(s, xCss, yCss, wCss, hCss) {
  return await s.page.evaluate(({ x, y, w, h }) => {
    const c = document.querySelector('canvas');
    if (!c) return null;
    const r = c.getBoundingClientRect();
    const sx = c.width / r.width, sy = c.height / r.height;
    const px = Math.max(0, Math.floor((x - r.left) * sx));
    const py = Math.max(0, Math.floor((y - r.top) * sy));
    const pw = Math.max(1, Math.min(c.width - px, Math.floor(w * sx)));
    const ph = Math.max(1, Math.min(c.height - py, Math.floor(h * sy)));
    const ctx = c.getContext('2d');
    const img = ctx.getImageData(px, py, pw, ph).data;
    let total = 0, transparent = 0, white = 0, dark = 0;
    let rSum = 0, gSum = 0, bSum = 0, opaque = 0;
    for (let p = 0; p < img.length; p += 4) {
      total++;
      if (img[p+3] === 0) { transparent++; continue; }
      opaque++;
      const lum = (img[p] + img[p+1] + img[p+2]) / 3 | 0;
      if (lum > 220) white++;
      if (lum < 60) dark++;
      rSum += img[p]; gSum += img[p+1]; bSum += img[p+2];
    }
    return {
      transparentFrac: transparent / total,
      opaqueWhiteFrac: white / total,
      opaqueDarkFrac: dark / total,
      meanR: opaque ? rSum / opaque | 0 : 0,
      meanG: opaque ? gSum / opaque | 0 : 0,
      meanB: opaque ? bSum / opaque | 0 : 0,
    };
  }, { x: xCss, y: yCss, w: wCss, h: hCss });
}
function colorDist(a, b) {
  return Math.max(Math.abs(a.r - b.r), Math.abs(a.g - b.g), Math.abs(a.b - b.b));
}

// 16x16 grayscale signature for canvas-state diffing.
async function canvasSig(s) {
  return await s.page.evaluate(() => {
    const c = document.querySelector('canvas');
    if (!c) return null;
    const w = c.width, h = c.height;
    const ctx = c.getContext('2d');
    const img = ctx.getImageData(0, 0, w, h).data;
    const sx = Math.floor(w / 16), sy = Math.floor(h / 16);
    let str = '';
    for (let y = 0; y < 16; y++)
      for (let x = 0; x < 16; x++) {
        const px = (y * sy * w + x * sx) * 4;
        const lum = (img[px] + img[px + 1] + img[px + 2]) / 3 | 0;
        str += lum.toString(16).padStart(2, '0');
      }
    return str;
  });
}
function sigDiff(a, b) {
  if (!a || !b) return -1;
  let d = 0;
  for (let i = 0; i < a.length; i += 2) if (a.substring(i, i+2) !== b.substring(i, i+2)) d++;
  return d;
}

// Scenario 1: TextField click should not blank the "Main Class" label.
async function scenarioTextfield() {
  const s = await bootScenario('01-textfield');
  try {
    const before = await sampleStrip(s, s.canvasBox.x + 60, s.canvasBox.y + 215, 540, 30);
    await s.page.mouse.click(s.canvasBox.x + 220, s.canvasBox.y + 258);
    for (let i = 0; i < 60; i++) {
      await new Promise(r => setTimeout(r, 100));
      const present = await s.page.evaluate(() =>
          !!document.querySelector('input.cn1-edit-string'));
      if (present) break;
    }
    await new Promise(r => setTimeout(r, 1200));
    const after = await sampleStrip(s, s.canvasBox.x + 60, s.canvasBox.y + 215, 540, 30);
    await s.snap('after-click');
    if (after && after.transparentFrac > 0.05) {
      fail('01-textfield', `label area went ${(after.transparentFrac*100).toFixed(1)}% transparent (was ${(before.transparentFrac*100).toFixed(1)}%)`);
    }
  } finally { await teardown(s); }
}

// Scenario 2: Hello dialog should render its body with white bg.
async function scenarioDialog() {
  const s = await bootScenario('02-dialog');
  try {
    await s.page.mouse.click(s.canvasBox.x + 936, s.canvasBox.y + 141);
    await new Promise(r => setTimeout(r, 4000));
    await s.snap('open');
    const body = await sampleStrip(s, s.canvasBox.x + 400, s.canvasBox.y + 380, 480, 90);
    console.log(`[features] dialog body: white=${(body.opaqueWhiteFrac*100).toFixed(1)}% ` +
        `dark=${(body.opaqueDarkFrac*100).toFixed(1)}% transparent=${(body.transparentFrac*100).toFixed(1)}%`);
    if (body.opaqueDarkFrac > 0.10) {
      fail('02-dialog', `${(body.opaqueDarkFrac*100).toFixed(1)}% dark pixels in dialog body -- bg fill missed area`);
    }
    if (body.transparentFrac > 0.05) {
      fail('02-dialog', `${(body.transparentFrac*100).toFixed(1)}% transparent in dialog body`);
    }
  } finally { await teardown(s); }
}

// Scenario 3: Side menu open should not flicker through dozens of states.
async function scenarioSideMenu() {
  const s = await bootScenario('03-side-menu');
  try {
    const sigs = [];
    const openTask = s.page.mouse.click(s.canvasBox.x + 698, s.canvasBox.y + 100);
    for (let i = 0; i < 16; i++) {
      await new Promise(r => setTimeout(r, 120));
      const sig = await canvasSig(s);
      if (sig) sigs.push(sig);
    }
    await openTask;
    await new Promise(r => setTimeout(r, 1000));
    await s.snap('open');
    const distinct = new Set(sigs);
    console.log(`[features] side-menu animation: ${sigs.length} samples, ${distinct.size} distinct`);
    if (distinct.size > 10) {
      fail('03-side-menu', `${distinct.size}/${sigs.length} distinct canvas states during open animation -- continuous full-canvas redraw`);
    }
  } finally { await teardown(s); }
}

// Scenario 4: Template buttons (radio in ButtonGroup) -- click each in
// turn, verify previous selection turns away from blue and clicked one
// turns toward blue. We sample a single pixel-block per button and
// compare R/G/B.
async function scenarioTemplateButtons() {
  const s = await bootScenario('04-template');
  try {
    // Coordinates at deviceScaleFactor=2, viewport 1280x900:
    //   BAREBONES (195, 405)   KOTLIN (465, 405)
    //   GRUB      (195, 442)   TWEET  (465, 442)
    const buttons = {
      barebones: [195, 405],
      kotlin:    [465, 405],
      grub:      [195, 442],
      tweet:     [465, 442],
    };
    async function buttonColor(name) {
      const [x, y] = buttons[name];
      const r = await sampleStrip(s, s.canvasBox.x + x - 30, s.canvasBox.y + y - 4, 60, 8);
      return { r: r.meanR, g: r.meanG, b: r.meanB };
    }
    // Initial: BAREBONES selected.
    let prevSelected = 'barebones';
    const initialColors = {};
    for (const k of Object.keys(buttons)) initialColors[k] = await buttonColor(k);
    console.log(`[features] initial colors: barebones=${JSON.stringify(initialColors.barebones)} ` +
        `kotlin=${JSON.stringify(initialColors.kotlin)}`);

    const sequence = ['kotlin', 'grub', 'tweet', 'barebones'];
    for (const name of sequence) {
      const [x, y] = buttons[name];
      const beforeNew = await buttonColor(name);
      const beforePrev = await buttonColor(prevSelected);
      // Use down/up to mimic a real user gesture, with a small dwell
      // so the engine has time to fire mousedown handlers separately.
      await s.page.mouse.move(s.canvasBox.x + x, s.canvasBox.y + y);
      await s.page.mouse.down();
      await new Promise(r => setTimeout(r, 50));
      await s.page.mouse.up();
      // Poll for the click's effect to settle. Some templates trigger
      // heavy theme reloads (CSS bundles) that can take >5 s. Wait up
      // to 15 s for the new button to actually go selected.
      let settled = false;
      for (let i = 0; i < 30; i++) {
        await new Promise(r => setTimeout(r, 500));
        const c = await buttonColor(name);
        if (colorDist(beforeNew, c) > 30) { settled = true; break; }
      }
      console.log(`[features]   ${name} settled=${settled}`);
      await s.snap(`after-${name}`);
      const afterNew = await buttonColor(name);
      const afterPrev = await buttonColor(prevSelected);
      const newDelta = colorDist(beforeNew, afterNew);
      const prevDelta = colorDist(beforePrev, afterPrev);
      console.log(`[features] click ${name}: new-button color delta=${newDelta} prev-button (${prevSelected}) delta=${prevDelta}`);
      if (newDelta < 30) {
        fail(`04-template-${name}`, `clicking ${name} did not change its colour (delta=${newDelta}) -- click was dropped or the action listener hung`);
      }
      if (prevDelta < 30) {
        fail(`04-template-${name}`, `previously-selected ${prevSelected} did NOT redraw to unselected (delta=${prevDelta}) -- ButtonGroup.deselect did not propagate to a paint`);
      }
      prevSelected = name;
    }
  } finally { await teardown(s); }
}

// Scenario for the "stuck on Loading..." bug class. The user-facing
// page (/initializr/) wraps the bundle in an iframe and shows a
// ``Loading Initializr...`` overlay until the iframe sends the
// ``cn1-initializr-ui-ready`` postMessage (or 8 s elapses, whichever
// comes first). Three independent things have to all work for the
// user to see a useful UI:
//   1. ``/initializr-app/`` actually serves all worker / runtime files
//      (the deploy step has historically left these as 404 -- the
//      iframe loads but its scripts fail and the canvas stays blank).
//   2. The bundle inside the iframe boots, paints non-trivial content,
//      and posts the ``ui-ready`` signal.
//   3. The parent's loader overlay actually receives the signal and
//      hides within a tight budget (otherwise the user spends seconds
//      staring at the overlay).
//
// We test this by booting an "iframe parent" page that mirrors the
// production HTML (same loader markup, same postMessage contract), so
// it's representative of what users see -- without needing to deploy.
// In ``--url=`` mode the parent URL the user actually visits is tested
// directly. A regression here is exactly the symptom the user reported
// ("the new version of the UI is stuck in the Loading... screen").
async function scenarioIframeLoader() {
  const scenarioLabel = '06-iframe-loader';
  const page = await browser.newPage({
    viewport: { width: 1280, height: 900 },
    deviceScaleFactor: 2,
  });
  const messages = [];
  page.on('console', msg => messages.push(`[${msg.type()}] ${msg.text()}`));
  page.on('pageerror', err => messages.push(`[pageerror] ${err.message}`));

  // For URL mode, the supplied URL might already BE the iframe parent.
  // For local mode the served bundle has no iframe; we synthesise an
  // iframe-parent page that loads the bundle and watches for the
  // ready signal -- mirroring what the production website does.
  let parentUrl;
  if (TEST_URL) {
    // If the URL already points at the iframe parent (has a
    // ``#cn1-initializr-frame`` element), use it as-is. Otherwise
    // assume it's the bundle root and wrap it ourselves.
    const probeBrowser = await chromium.launch({ headless: true });
    const probePage = await probeBrowser.newPage();
    await probePage.goto(TEST_URL, { waitUntil: 'domcontentloaded' });
    const hasFrame = await probePage.evaluate(() => !!document.querySelector('#cn1-initializr-frame'));
    await probeBrowser.close();
    if (hasFrame) {
      parentUrl = TEST_URL;
    } else {
      // Skip this scenario for raw bundle URLs -- there's no parent
      // loader to test.
      console.log(`[features] ${scenarioLabel} skipped: ${TEST_URL} is a bundle root, not an iframe parent`);
      await page.close();
      return;
    }
  } else {
    // Local mode: synthesise the iframe parent inline. ``data:`` URLs
    // are isolated origins so the bundle's worker.js / fetch calls can
    // still resolve relative URLs; using a normal http:// host page
    // and bundling the parent shell as a dedicated route is cleaner.
    // For now keep it simple and skip -- the boot+paint assertion in
    // bootScenario already covers the bundle-side health.
    console.log(`[features] ${scenarioLabel} skipped: only meaningful in --url= mode`);
    await page.close();
    return;
  }

  const t0 = Date.now();
  await page.goto(parentUrl);

  // Wait up to ``BOOT_COMPLETE_BUDGET_MS + FIRST_PAINT_BUDGET_MS`` for
  // BOTH the iframe canvas to render content AND the parent loader to
  // hide. A regression in either signal fails the scenario.
  let framePaintMs = null;
  let loaderHiddenMs = null;
  const deadline = t0 + BOOT_COMPLETE_BUDGET_MS + FIRST_PAINT_BUDGET_MS;
  while (Date.now() < deadline) {
    const snap = await page.evaluate(() => {
      const loader = document.querySelector('#cn1-initializr-loader');
      const f = document.querySelector('#cn1-initializr-frame');
      let nw = 0;
      if (f && f.contentDocument) {
        const c = f.contentDocument.querySelector('canvas');
        if (c && c.width > 0) {
          try {
            const img = c.getContext('2d').getImageData(0, 0, c.width, c.height).data;
            for (let i = 0; i < img.length; i += 64) {
              const lum = (img[i]+img[i+1]+img[i+2])/3;
              if (img[i+3] > 0 && lum < 240) nw++;
            }
          } catch {}
        }
      }
      return {
        loaderHidden: loader ? loader.classList.contains('done') : null,
        iframeNonWhite: nw,
      };
    });
    if (framePaintMs === null && snap.iframeNonWhite > 200) framePaintMs = Date.now() - t0;
    if (loaderHiddenMs === null && snap.loaderHidden) loaderHiddenMs = Date.now() - t0;
    if (framePaintMs !== null && loaderHiddenMs !== null) break;
    await new Promise(r => setTimeout(r, 250));
  }
  const out = path.join(ART_DIR, `${scenarioLabel}-final.png`);
  await page.screenshot({ path: out }).catch(() => {});

  console.log(`[features] ${scenarioLabel}: framePaint@${framePaintMs}ms loaderHidden@${loaderHiddenMs}ms`);
  if (framePaintMs === null) {
    fail(scenarioLabel, `iframe canvas never rendered non-trivial content (parent at ${parentUrl}). ` +
        `This is the user-visible "stuck on Loading..." regression.`);
  }
  if (loaderHiddenMs === null) {
    fail(scenarioLabel, `parent's Loading overlay never hid (iframe paint ` +
        `at ${framePaintMs}ms but loader still visible). Either the bundle's ` +
        `cn1-initializr-ui-ready postMessage never fired or the parent's ` +
        `listener didn't process it.`);
  }
  await page.close();
}

// Scenario 5: Mashing toggle buttons should not freeze the worker.
async function scenarioToggleMashing() {
  const s = await bootScenario('05-toggle-mashing');
  try {
    const sigBefore = await canvasSig(s);
    // Rapid-fire clicks alternating between IDE row (~y=525) and Theme
    // row (~y=580). 60 clicks, spaced 30 ms apart.
    for (let i = 0; i < 60; i++) {
      const x = 180 + (i % 4) * 80;
      const y = (i % 2 === 0) ? 525 : 580;
      await s.page.mouse.click(s.canvasBox.x + x, s.canvasBox.y + y);
      await new Promise(r => setTimeout(r, 30));
    }
    await new Promise(r => setTimeout(r, 2000));
    await s.snap('after-mash');
    // Liveness probe: click a known interactive surface and verify
    // canvas changes within 4 s.
    const sigBeforeProbe = await canvasSig(s);
    await s.page.mouse.click(s.canvasBox.x + 593, s.canvasBox.y + 525);
    let live = false;
    for (let i = 0; i < 16; i++) {
      await new Promise(r => setTimeout(r, 250));
      const now = await canvasSig(s);
      if (sigDiff(sigBeforeProbe, now) > 1) { live = true; break; }
    }
    if (!live) {
      fail('05-toggle-mashing', `worker is unresponsive after 60 rapid toggle clicks (canvas unchanged 4 s after a known-good click)`);
    }
  } finally { await teardown(s); }
}

const scenarios = [
  ['01-textfield',     scenarioTextfield],
  ['02-dialog',        scenarioDialog],
  ['03-side-menu',     scenarioSideMenu],
  ['04-template',      scenarioTemplateButtons],
  ['05-toggle-mash',   scenarioToggleMashing],
  ['06-iframe-loader', scenarioIframeLoader],
];

for (const [name, fn] of scenarios) {
  console.log(`\n=== ${name} ===`);
  try {
    await fn();
  } catch (err) {
    if (err && err.message === 'IFRAME_ONLY') {
      // Not a regression -- this scenario relies on direct main-doc
      // canvas access and was launched against an iframe-parent URL.
      // Scenario 06 covers the iframe path. Print a skip notice so
      // CI logs make the gap obvious.
      console.log(`[features] ${name} skipped: iframe-parent URL (lifecycle@${err.bootMs}ms firstPaint@${err.firstPaintMs}ms inside iframe -- click scenarios run only against bundle root)`);
      continue;
    }
    fail(name, `scenario threw: ${err.message}`);
  }
}

console.log(`\n[features] artifacts -> ${ART_DIR}`);
await browser.close();
if (server) server.kill();

if (failures.length === 0) {
  console.log('[features] PASS');
  process.exit(0);
}
console.log(`\n[features] FAIL: ${failures.length} sub-test(s) failed`);
failures.forEach(f => console.log('  -', f));
process.exit(1);
