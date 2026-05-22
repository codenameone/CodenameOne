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

  // Dump the DOM topology once so a failure leaves a breadcrumb. The CN1 JS
  // port creates a <div id="cn1-peers-container"> sibling of the
  // <canvas id="codenameone-canvas"> and registers all pointer listeners on
  // the peersContainer (not on the canvas itself, which has pointer-events:
  // none). Page-level mouse clicks miss because the canvas is in front and
  // pointer-events:none routes the click to body rather than the sibling
  // peersContainer. We therefore dispatch synthetic events on the
  // peersContainer directly -- this still exercises the entire CN1 listener
  // chain that PR #5003-class bugs would break.
  const layout = await page.evaluate(() => {
    const canvas = document.querySelector('#codenameone-canvas');
    const peers = document.querySelector('#cn1-peers-container');
    const r = canvas ? canvas.getBoundingClientRect() : null;
    return {
      hasCanvas: !!canvas,
      hasPeersContainer: !!peers,
      canvasRect: r ? { x: r.x, y: r.y, width: r.width, height: r.height } : null,
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

  async function fireMouse(type, x, y) {
    await page.evaluate(({ type, x, y }) => {
      const target = document.querySelector('#cn1-peers-container');
      const evt = new MouseEvent(type, {
        bubbles: true, cancelable: true, view: window,
        clientX: x, clientY: y, button: 0, buttons: type === 'mouseup' ? 0 : 1,
      });
      target.dispatchEvent(evt);
    }, { type, x, y });
  }

  // Tap: down + up at the form centre.
  await fireMouse('mousedown', px(0.5), py(0.5));
  await page.waitForTimeout(50);
  await fireMouse('mouseup', px(0.5), py(0.5));
  await waitFor('CN1IV:EVENT:tap', 5_000);

  // Drag: down at left, several mousemoves across, up at right. Need ≥ 3
  // pointerDragged samples for DragStep to fire.
  await waitFor('CN1IV:READY:drag', 5_000);
  await fireMouse('mousedown', px(0.2), py(0.55));
  for (let i = 1; i <= 10; i++) {
    const frac = 0.2 + (0.6 * i / 10);
    await fireMouse('mousemove', px(frac), py(0.55));
    await page.waitForTimeout(20);
  }
  await fireMouse('mouseup', px(0.8), py(0.55));
  await waitFor('CN1IV:EVENT:drag', 5_000);

  // Long-press: mousedown, hold ~1.5s, mouseup. CN1's threshold is ~1s.
  await waitFor('CN1IV:READY:longpress', 5_000);
  await fireMouse('mousedown', px(0.5), py(0.5));
  await page.waitForTimeout(1500);
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
