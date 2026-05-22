// Drive the CN1 input-validation app (JavaScript port) through tap / drag /
// long-press in headless Chromium and assert the expected CN1IV:EVENT lines
// appear on the browser console. Mirrors the iOS XCUITest driver -- same
// suite, same log markers, same pass/fail contract.

import fs from 'node:fs';
import path from 'node:path';

let chromium;
try {
  ({ chromium } = await import('playwright'));
} catch (e1) {
  try {
    ({ chromium } = await import('@playwright/test'));
  } catch (e2) {
    console.error('Unable to load Playwright. Install "playwright" or "@playwright/test".');
    process.exit(2);
  }
}

const url = process.env.CN1IV_URL;
if (!url) {
  console.error('CN1IV_URL env var is required (URL of the deployed JS build).');
  process.exit(2);
}

const artifactsDir = process.env.CN1IV_ARTIFACTS_DIR
  || path.resolve('artifacts/input-validation-js');
fs.mkdirSync(artifactsDir, { recursive: true });
const logPath = path.join(artifactsDir, 'browser.log');
const logStream = fs.createWriteStream(logPath, { flags: 'w' });

const REQUIRED_EVENTS = [
  'CN1IV:READY:tap',
  'CN1IV:EVENT:tap',
  'CN1IV:READY:drag',
  'CN1IV:EVENT:drag',
  'CN1IV:READY:longpress',
  'CN1IV:EVENT:longpress',
  'CN1IV:SUITE:FINISHED',
];

// We capture every console line, but only the lines containing CN1IV are
// considered for assertion. The page also dumps stack traces on errors, so
// we tee everything to disk for post-mortem.
const seen = new Set();
const timeouts = [];
function record(line) {
  logStream.write(line + '\n');
  if (line.indexOf('CN1IV:TIMEOUT:') >= 0) {
    timeouts.push(line);
  }
  for (const m of REQUIRED_EVENTS) {
    if (!seen.has(m) && line.indexOf(m) >= 0) {
      seen.add(m);
    }
  }
}

const browser = await chromium.launch({ headless: true });
const viewport = { width: 393, height: 852 };
try {
  const page = await browser.newPage({ viewport, deviceScaleFactor: 2 });
  page.on('console', msg => record(`console:${msg.type()}:${msg.text()}`));
  page.on('pageerror', err => record(`pageerror:${String(err)}`));

  await page.goto(url, { waitUntil: 'domcontentloaded', timeout: 60_000 });

  // Wait for the CN1 EDT to finish first paint before driving inputs. CN1's
  // JS port emits CN1IV:READY:tap from GestureSuite.advance() right after
  // Form.show(); we poll for it with a generous ceiling so flaky CI machines
  // still pass.
  const readyDeadline = Date.now() + 30_000;
  while (Date.now() < readyDeadline && !seen.has('CN1IV:READY:tap')) {
    await page.waitForTimeout(250);
  }
  if (!seen.has('CN1IV:READY:tap')) {
    console.error('Timed out waiting for CN1IV:READY:tap from app.');
    process.exit(1);
  }

  // Dump the DOM topology and verify the dispatch path before driving any
  // gestures. The CN1 JS port registers pointer listeners on a
  // <div id="cn1-peers-container"> sibling of the canvas, so page-level
  // clicks miss the listener. We dispatch synthetic events on the
  // peersContainer directly. Pre-flight checks confirm dispatch is wired
  // before we blame a CN1 regression for a missing event.
  const layout = await page.evaluate(() => {
    const canvas = document.querySelector('#codenameone-canvas');
    const peers = document.querySelector('#cn1-peers-container');
    const r = canvas ? canvas.getBoundingClientRect() : null;
    const peersStyle = peers ? window.getComputedStyle(peers) : null;
    let selfTestFired = false;
    if (peers) {
      const probe = (e) => { selfTestFired = true; };
      peers.addEventListener('mousedown', probe, true);
      const evt = new MouseEvent('mousedown', {
        bubbles: true, cancelable: true, view: window,
        clientX: 10, clientY: 10, button: 0, buttons: 1,
      });
      peers.dispatchEvent(evt);
      peers.removeEventListener('mousedown', probe, true);
    }
    return {
      hasCanvas: !!canvas,
      hasPeersContainer: !!peers,
      canvasRect: r ? { x: r.x, y: r.y, width: r.width, height: r.height } : null,
      peersDisplay: peersStyle ? peersStyle.display : null,
      peersPointerEvents: peersStyle ? peersStyle.pointerEvents : null,
      peersPosition: peersStyle ? peersStyle.position : null,
      selfTestFired,
    };
  });
  record(`layout:${JSON.stringify(layout)}`);
  if (!layout.hasPeersContainer) {
    console.error('CN1 peers container not found; JS port did not initialise.');
    process.exit(1);
  }
  const rect = layout.canvasRect;
  const px = (frac) => rect.x + rect.width * frac;
  const py = (frac) => rect.y + rect.height * frac;

  // Dispatch mouse + pointer + touch variants for each gesture step. The CN1
  // JS port registers listeners on both mousedown/pointerdown and
  // touchstart/touchend; firing all variants maximises the chance that one
  // reaches the actual handler regardless of which path the port currently
  // routes through.
  async function fireMouse(type, x, y) {
    await page.evaluate(({ type, x, y }) => {
      const target = document.querySelector('#cn1-peers-container');
      const isUp = type === 'mouseup';
      const mouse = new MouseEvent(type, {
        bubbles: true, cancelable: true, view: window,
        clientX: x, clientY: y, button: 0, buttons: isUp ? 0 : 1,
      });
      target.dispatchEvent(mouse);
      const pointerType = type === 'mousedown' ? 'pointerdown'
                       : type === 'mouseup'   ? 'pointerup'
                       : 'pointermove';
      try {
        const ptr = new PointerEvent(pointerType, {
          bubbles: true, cancelable: true, view: window,
          clientX: x, clientY: y, button: 0, buttons: isUp ? 0 : 1,
          pointerId: 1, pointerType: 'mouse', isPrimary: true,
        });
        target.dispatchEvent(ptr);
      } catch (_) {}
    }, { type, x, y });
  }

  async function fireTouch(type, x, y) {
    await page.evaluate(({ type, x, y }) => {
      const target = document.querySelector('#cn1-peers-container');
      try {
        const touch = new Touch({
          identifier: 1, target, clientX: x, clientY: y,
        });
        const list = (type === 'touchend') ? [] : [touch];
        const evt = new TouchEvent(type, {
          bubbles: true, cancelable: true, view: window,
          touches: list, targetTouches: list,
          changedTouches: [touch],
        });
        target.dispatchEvent(evt);
      } catch (_) {}
    }, { type, x, y });
  }

  // Tap: down + up at the form centre. Touch first (matches real mobile),
  // then mouse as belt-and-braces.
  await fireTouch('touchstart', px(0.5), py(0.5));
  await fireMouse('mousedown', px(0.5), py(0.5));
  await page.waitForTimeout(80);
  await fireTouch('touchend', px(0.5), py(0.5));
  await fireMouse('mouseup', px(0.5), py(0.5));
  await waitFor('CN1IV:EVENT:tap', 5_000);

  // Drag: down at left, several moves across, up at right. Need >= 3
  // pointerDragged samples for DragStep to fire.
  await waitFor('CN1IV:READY:drag', 5_000);
  await fireTouch('touchstart', px(0.2), py(0.55));
  await fireMouse('mousedown', px(0.2), py(0.55));
  for (let i = 1; i <= 10; i++) {
    const frac = 0.2 + (0.6 * i / 10);
    await fireTouch('touchmove', px(frac), py(0.55));
    await fireMouse('mousemove', px(frac), py(0.55));
    await page.waitForTimeout(20);
  }
  await fireTouch('touchend', px(0.8), py(0.55));
  await fireMouse('mouseup', px(0.8), py(0.55));
  await waitFor('CN1IV:EVENT:drag', 5_000);

  // Long-press: down, hold ~1.5s, up. CN1's threshold is ~1s.
  await waitFor('CN1IV:READY:longpress', 5_000);
  await fireTouch('touchstart', px(0.5), py(0.5));
  await fireMouse('mousedown', px(0.5), py(0.5));
  await page.waitForTimeout(1500);
  await fireTouch('touchend', px(0.5), py(0.5));
  await fireMouse('mouseup', px(0.5), py(0.5));
  await waitFor('CN1IV:EVENT:longpress', 5_000);

  await waitFor('CN1IV:SUITE:FINISHED', 5_000);

  async function waitFor(marker, ms) {
    const deadline = Date.now() + ms;
    while (Date.now() < deadline) {
      if (seen.has(marker)) return;
      await page.waitForTimeout(150);
    }
    // Don't throw here -- continue so we get the full list of misses in the
    // final report.
  }
} finally {
  await browser.close();
  logStream.end();
}

const log = (line) => process.stdout.write(`[playwright-driver] ${line}\n`);
let failed = false;
for (const m of REQUIRED_EVENTS) {
  if (seen.has(m)) {
    log(`OK   ${m}`);
  } else {
    log(`MISS ${m}`);
    failed = true;
  }
}
if (timeouts.length) {
  for (const t of timeouts) log(`TIMEOUT  ${t}`);
  failed = true;
}
if (failed) {
  log(`Input-validation suite FAILED -- see ${logPath}`);
  process.exit(1);
}
log('Input-validation suite PASSED');
