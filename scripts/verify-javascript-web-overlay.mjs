#!/usr/bin/env node
// Verifies the JavaScript port's web-native overlay layers against a running build.
//
// Canvas rendering could only ever be checked by comparing pixels. Once text and
// semantics live in the DOM they can be asserted directly, which is both faster and
// diagnostic: a failure names what is wrong instead of reporting a pixel delta.
//
// Usage:
//   node scripts/verify-javascript-web-overlay.mjs <url> [--channel chrome]
//
// Serve an unpacked bundle first, e.g.
//   (cd dist/MyApp-js && python3 -m http.server 8099)
//   node scripts/verify-javascript-web-overlay.mjs http://localhost:8099/index.html
//
// Exits non-zero when a check fails.

let chromium;
try {
  ({ chromium } = await import('playwright'));
} catch (playwrightError) {
  try {
    ({ chromium } = await import('@playwright/test'));
  } catch (playwrightTestError) {
    console.error('Unable to load Playwright. Install either "playwright" or "@playwright/test".');
    console.error('Import from "playwright" failed:', String(playwrightError));
    console.error('Import from "@playwright/test" failed:', String(playwrightTestError));
    process.exit(3);
  }
}

const args = process.argv.slice(2);
const url = args.find(a => !a.startsWith('--'));
if (!url) {
  console.error('Usage: node scripts/verify-javascript-web-overlay.mjs <url> [--channel chrome]');
  process.exit(2);
}
const channelIndex = args.indexOf('--channel');
const channel = channelIndex >= 0 ? args[channelIndex + 1] : process.env.CN1_JS_BROWSER_CHANNEL;
const bootTimeoutMs = Number(process.env.CN1_JS_TIMEOUT_SECONDS || 120) * 1000;

const results = [];
function check(name, pass, detail) {
  results.push({ name, pass });
  console.log(`${pass ? 'PASS' : 'FAIL'}  ${name}${detail ? ' :: ' + detail : ''}`);
}

const browser = await chromium.launch(channel ? { channel } : {});
// A dark color scheme is emulated so the prefers-color-scheme path is exercised: that
// query is evaluated on the main thread, because the worker has no matchMedia.
const page = await browser.newPage({
  viewport: { width: 375, height: 667 },
  deviceScaleFactor: 2,
  colorScheme: 'dark'
});

const logs = [];
page.on('console', m => logs.push(m.text()));
page.on('pageerror', e => logs.push('pageerror:' + String(e)));

await page.goto(url, { waitUntil: 'domcontentloaded' });

// Sampled before the app has had a chance to navigate, so growth can be attributed to it.
const initialHistoryLength = await page.evaluate(() => history.length);

// The app boots through a worker, so poll rather than assuming it is up.
let snap = { runs: 0 };
const deadline = Date.now() + bootTimeoutMs;
while (Date.now() < deadline) {
  snap = await page.evaluate(() => {
    const layer = document.getElementById('cn1-text-layer');
    const tree = document.getElementById('cn1-accessibility-tree');
    const canvas = document.getElementById('codenameone-canvas');
    return {
      hasLayer: !!layer,
      hasTree: !!tree,
      layerAriaHidden: layer ? layer.getAttribute('aria-hidden') : null,
      layerPointerEvents: layer ? layer.style.pointerEvents : null,
      canvasAriaHidden: canvas ? canvas.getAttribute('aria-hidden') : null,
      runs: layer ? layer.querySelectorAll('span').length : 0,
      semanticNodes: tree ? tree.querySelectorAll('[data-cn1-accessibility-id]').length : 0,
      text: layer ? layer.innerText.replace(/\s+/g, ' ').trim() : ''
    };
  });
  if (snap.runs > 0 && snap.semanticNodes > 0) break;
  await page.waitForTimeout(1000);
}

check('text layer present', snap.hasLayer);
check('text promoted to real DOM text', snap.runs > 0, `${snap.runs} run(s)`);
check('promoted text is readable', snap.text.length > 0, JSON.stringify(snap.text.slice(0, 100)));
check('text layer hidden from assistive tech', snap.layerAriaHidden === 'true',
      `aria-hidden=${snap.layerAriaHidden}`);
check('text layer does not take pointer events', snap.layerPointerEvents === 'none',
      `pointer-events=${snap.layerPointerEvents}`);
check('semantic overlay populated', snap.semanticNodes > 0, `${snap.semanticNodes} node(s)`);
check('canvas hidden from assistive tech', snap.canvasAriaHidden === 'true');

// The overlay must reuse elements across invalidations. Rebuilding would drop DOM focus
// and any in-progress text selection on every CHANGE_BOUNDS, which every setX/setY raises.
let identity = null;
for (let attempt = 0; attempt < 25 && !identity; attempt++) {
  identity = await page.evaluate(async () => {
    const tree = document.getElementById('cn1-accessibility-tree');
    if (!tree) return null;
    const before = Array.from(tree.querySelectorAll('[data-cn1-accessibility-id]'));
    if (before.length === 0) return null;
    before.forEach((el, i) => { el.__cn1probe = 'probe-' + i; });
    const ids = before.map(el => el.getAttribute('data-cn1-accessibility-id'));
    await new Promise(r => setTimeout(r, 250));
    const present = ids
      .map(id => tree.querySelector(`[data-cn1-accessibility-id="${id}"]`))
      .filter(Boolean);
    if (present.length === 0) return null;
    return {
      present: present.length,
      reused: present.filter(el => typeof el.__cn1probe === 'string').length
    };
  });
  if (!identity) await page.waitForTimeout(200);
}
if (identity) {
  check('semantic nodes reused across invalidations', identity.reused === identity.present,
        `${identity.reused}/${identity.present} kept identity`);
} else {
  check('semantic nodes reused across invalidations', false, 'no stable sample available');
}

// history.pushState is a main-thread API. When it was compiled into the worker it threw on
// every form change and the port logged that the back command would not work.
check('no "pushState not supported" warning',
      !logs.some(l => l.includes('history.pushState not supported')));
// The root form deliberately pushes nothing -- it has nothing to go back to -- so a single
// form app sits at length 1 legitimately. Only assert growth when the app actually navigated;
// otherwise report it rather than failing a valid overlay.
const historyLength = await page.evaluate(() => history.length);
if (historyLength > initialHistoryLength) {
  check('history entries pushed on navigation', true,
        `${initialHistoryLength} -> ${historyLength}`);
} else {
  console.log(`SKIP  history entries pushed on navigation :: no navigation observed `
    + `(history.length=${historyLength})`);
}

// An editable field is reachable through the overlay only if something can carry the text to
// set. A button cannot -- it dispatches with no argument -- so SET_TEXT gets an input, and this
// asserts the field and its control are actually connected.
const setText = await page.evaluate(() => {
  const tree = document.getElementById('cn1-accessibility-tree');
  const actions = document.getElementById('cn1-accessibility-actions');
  if (!tree) return null;
  const boxes = Array.from(tree.querySelectorAll('[role="textbox"],[role="searchbox"]'));
  if (boxes.length === 0) return { fields: 0 };
  const ids = boxes.map(b => b.getAttribute('id')).filter(Boolean);
  // A multiline field's control is a textarea, not an input -- looking only for inputs would
  // report a screen of TextAreas as having no way to set text.
  const inputs = actions ? Array.from(actions.querySelectorAll('input,textarea')) : [];
  const wired = inputs.filter(i => ids.includes(i.getAttribute('aria-controls')));
  return { fields: boxes.length, inputs: inputs.length, wired: wired.length };
});
if (!setText || setText.fields === 0) {
  console.log('SKIP  editable fields expose a control that can set text :: no field on screen');
} else {
  check('editable fields expose a control that can set text', setText.wired > 0,
        `${setText.wired} control(s) for ${setText.fields} field(s)`);
}

// A reload keeps the entry it happened on, and with it any id this port stamped there before
// the reload -- while the port's own counters start again from zero. An entry left claiming an
// id above anything the new session pushed reads as being ahead of the app, so Back to it is
// taken for Forward and spends entries instead of returning to the first form. The port
// rewrites the entry as it shows its first form; this asserts it did.
if (historyLength > initialHistoryLength) {
  await page.reload({ waitUntil: 'domcontentloaded' });
  let rootState = null;
  const stateDeadline = Date.now() + bootTimeoutMs;
  while (Date.now() < stateDeadline) {
    const state = await page.evaluate(() => {
      const layer = document.getElementById('cn1-text-layer');
      if (!layer || layer.childElementCount === 0) return null;
      return String(history.state);
    });
    if (state !== null) { rootState = state; break; }
    await page.waitForTimeout(200);
  }
  check('reload claims its history entry as the root', rootState === 'cn1-history:0',
        `history.state=${rootState}`);
} else {
  console.log('SKIP  reload claims its history entry as the root :: no navigation observed');
}

console.log('\n--- summary ---');
const failed = results.filter(r => !r.pass);
console.log(`${results.length - failed.length}/${results.length} checks passed`);
if (failed.length) {
  console.log('failed: ' + failed.map(f => f.name).join(', '));
  console.log('\n--- recent console ---');
  console.log(logs.slice(-10).join('\n'));
}

await browser.close();
process.exit(failed.length ? 1 : 0);
