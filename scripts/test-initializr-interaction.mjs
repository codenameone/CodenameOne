// Playwright-driven interaction tests for the Initializr JS-port bundle.
//
// Reproduces the two regressions reported on the live preview:
//   1. UI freeze when the embedded "Hello World" button triggers
//      ``Dialog.show(...)`` — stack pointed at MenuBar.setBackCommand
//      throwing NPE on a null ``parent`` field.
//   2. Black squares appearing during pointer interaction — likely a
//      paint/double-buffer race in the worker green-thread / EDT
//      handoff.
//
// Run: node scripts/test-initializr-interaction.mjs <path-to-bundle.zip>
// Defaults to scripts/initializr/javascript/target/initializr-javascript-port.zip.
import { chromium } from 'playwright';
import fs from 'node:fs';
import path from 'node:path';
import os from 'node:os';
import { spawn } from 'node:child_process';
import { execSync } from 'node:child_process';

const REPO_ROOT = path.resolve(path.dirname(new URL(import.meta.url).pathname), '..');
const DEFAULT_BUNDLE = path.join(REPO_ROOT, 'scripts/initializr/javascript/target/initializr-javascript-port.zip');
const bundle = process.argv[2] || DEFAULT_BUNDLE;
if (!fs.existsSync(bundle)) {
  console.error('bundle not found:', bundle);
  process.exit(2);
}

const tmpDir = fs.mkdtempSync(path.join(os.tmpdir(), 'init-test-'));
const bundleDir = path.join(tmpDir, 'bundle');
fs.mkdirSync(bundleDir);
execSync(`unzip -q "${bundle}" -d "${bundleDir}"`);
const distEntry = fs.readdirSync(bundleDir).filter(n => fs.statSync(path.join(bundleDir, n)).isDirectory())[0];
const distDir = path.join(bundleDir, distEntry);

// Inject worker-side instrumentation that wraps the mangled MenuBar /
// Form / Dialog methods we want to inspect. The mangle map ships with the
// build under target/initializr-javascript-port.mangle-map.json — we read
// the symbol names there and emit a hook script that the worker pulls in
// via importScripts before translated_app.js gets a chance to define the
// real symbols. We then *re-wrap* in port-after-load (which executes
// after translated_app.js) so the wrapper closes over the loaded real fn.
const mangleMapPath = bundle.replace(/\.zip$/, '.mangle-map.json');
let mangleMap = {};
if (fs.existsSync(mangleMapPath)) {
  mangleMap = JSON.parse(fs.readFileSync(mangleMapPath, 'utf8'));
}
function mangledFor(unmangled) {
  for (const [k, v] of Object.entries(mangleMap)) if (v === unmangled) return k;
  return null;
}
const sym = {
  menuBarInit: mangledFor('cn1_com_codename1_ui_MenuBar_initMenuBar_com_codename1_ui_Form'),
  menuBarSetBack: mangledFor('cn1_com_codename1_ui_MenuBar_setBackCommand_com_codename1_ui_Command'),
  formInitLaf: mangledFor('cn1_com_codename1_ui_Form_initLaf_com_codename1_ui_plaf_UIManager'),
  componentInitLaf: mangledFor('cn1_com_codename1_ui_Component_initLaf_com_codename1_ui_plaf_UIManager'),
  dialogInitLaf: mangledFor('cn1_com_codename1_ui_Dialog_initLaf_com_codename1_ui_plaf_UIManager'),
  formSetBackCmd: mangledFor('cn1_com_codename1_ui_Form_setBackCommand_com_codename1_ui_Command'),
  formSetMenuBar: mangledFor('cn1_com_codename1_ui_Form_setMenuBar_com_codename1_ui_MenuBar'),
  parentField: mangledFor('cn1_com_codename1_ui_MenuBar_parent'),
  menuBarField: mangledFor('cn1_com_codename1_ui_Form_menuBar'),
  menuBarCtor: mangledFor('cn1_com_codename1_ui_MenuBar___INIT__'),
  menuBarOuterParent: mangledFor('cn1_com_codename1_ui_SideMenuBar_parent'),
};
console.log('symbols:', sym);

const hookSrc = `
// === injected by test-initializr-interaction.mjs ===
// We need diagnostics for two questions:
//   1. When Form.initLaf runs for the new Dialog, what is the menuBar
//      field value at *entry*? (If it's null we'll see initMenuBar
//      called; if it's already non-null, we won't.)
//   2. Which MenuBar instance has its setBackCommand called, and what
//      is its parent at that moment?
// Hooks are installed in the worker; events are pushed to a buffer
// AND emitted via console.log so they show in the page log.
self.__cn1Trace = { events: [], counts: {} };
function __idOf(o) {
  if (!o) return null;
  if (!o.__cn1id) o.__cn1id = Math.random().toString(36).slice(2,8);
  // Use the runtime's nextIdentity-assigned __id so the same JVM object
  // gets the same id even if it changes hands between worker turns.
  return (o.__class || '?') + '#' + o.__cn1id + '/r' + (o.__id != null ? o.__id : '?');
}
function __push(e) {
  self.__cn1Trace.events.push(e);
  self.__cn1Trace.counts[e.k] = (self.__cn1Trace.counts[e.k] || 0) + 1;
  if ((self.__cn1Trace.counts[e.k] || 0) <= 60) {
    if (typeof console !== 'undefined') console.log('[trace]', JSON.stringify(e));
  }
}
self.__cn1InstallHooks = function() {
  // Most virtual dispatch goes through cls.methods[dispatchId], which
  // captures function references at class-registration time. Replacing
  // self.\$xxx only catches direct (invokespecial) calls and any code
  // that does a self lookup; it MISSES virtual dispatch entirely. So
  // we also walk every jvm.classes[cls].methods map and replace
  // entries pointing at our target functions with the wrapped variant.
  // The runtime caches resolved entries in resolvedVirtualCache - clear
  // it after re-wiring so subsequent dispatches re-walk and pick up
  // our wrappers.
  function wrapFn(orig, label, preFn, postFn) {
    if (typeof orig !== 'function') return orig;
    const wrapped = function*(...args) {
      if (preFn) preFn(args);
      const r = yield* orig.apply(this, args);
      if (postFn) postFn(args);
      return r;
    };
    wrapped.__cn1WrappedLabel = label;
    wrapped.__cn1Original = orig;
    return wrapped;
  }
  const wrappedTargets = Object.create(null);
  function defineWrap(name, label, preFn, postFn) {
    const orig = self[name];
    if (typeof orig !== 'function') {
      console.log('[trace] cannot wrap missing function ' + name + ' (' + label + ')');
      return;
    }
    const wrapped = wrapFn(orig, label, preFn, postFn);
    self[name] = wrapped;
    wrappedTargets[name] = { orig: orig, wrapped: wrapped };
  }
  function rewireDispatchTables() {
    const jvm = self.jvm;
    if (!jvm || !jvm.classes) return;
    let count = 0;
    for (const clsName in jvm.classes) {
      const cls = jvm.classes[clsName];
      if (!cls || !cls.methods) continue;
      for (const dispatchId in cls.methods) {
        const entry = cls.methods[dispatchId];
        for (const targetName in wrappedTargets) {
          if (entry === wrappedTargets[targetName].orig) {
            cls.methods[dispatchId] = wrappedTargets[targetName].wrapped;
            count++;
          }
        }
      }
    }
    if (jvm.resolvedVirtualCache) {
      jvm.resolvedVirtualCache = Object.create(null);
    }
    console.log('[trace] rewired ' + count + ' dispatch entries');
  }
  // Backwards-compatible wrapGen alias for the old code below.
  function wrapGen(name, label, preFn, postFn) { defineWrap(name, label, preFn, postFn); }
  ${sym.menuBarInit ? `wrapGen(${JSON.stringify(sym.menuBarInit)}, 'MenuBar.initMenuBar',
    args => __push({ k: 'enter:MenuBar.initMenuBar', menuBar: __idOf(args[0]), form: __idOf(args[1]) }),
    args => __push({ k: 'leave:MenuBar.initMenuBar', menuBar: __idOf(args[0]), parent_set_to: __idOf(args[0] && args[0][${JSON.stringify(sym.parentField)}]) })
  );` : ''}
  ${sym.menuBarSetBack ? `wrapGen(${JSON.stringify(sym.menuBarSetBack)}, 'MenuBar.setBackCommand',
    args => __push({ k: 'enter:MenuBar.setBackCommand', menuBar: __idOf(args[0]), parent: __idOf(args[0] && args[0][${JSON.stringify(sym.parentField)}]) }),
    args => __push({ k: 'leave:MenuBar.setBackCommand' })
  );` : ''}
  ${sym.componentInitLaf ? `wrapGen(${JSON.stringify(sym.componentInitLaf)}, 'Component.initLaf',
    args => __push({ k: 'enter:Component.initLaf', recv: __idOf(args[0]) })
  );` : ''}
  ${sym.dialogInitLaf ? `wrapGen(${JSON.stringify(sym.dialogInitLaf)}, 'Dialog.initLaf',
    args => __push({ k: 'enter:Dialog.initLaf', form: __idOf(args[0]), menuBar_at_entry: __idOf(args[0] && args[0][${JSON.stringify(sym.menuBarField)}]) }),
    args => __push({ k: 'leave:Dialog.initLaf', form: __idOf(args[0]), menuBar_at_exit: __idOf(args[0] && args[0][${JSON.stringify(sym.menuBarField)}]) })
  );` : ''}
  ${sym.formInitLaf ? `wrapGen(${JSON.stringify(sym.formInitLaf)}, 'Form.initLaf',
    args => {
      const o = args[0];
      __push({
        k: 'enter:Form.initLaf',
        form: __idOf(o),
        menuBar_at_entry: __idOf(o && o[${JSON.stringify(sym.menuBarField)}]),
      });
    },
    args => __push({ k: 'leave:Form.initLaf', form: __idOf(args[0]), menuBar_at_exit: __idOf(args[0] && args[0][${JSON.stringify(sym.menuBarField)}]) })
  );` : ''}
  ${sym.formSetBackCmd ? `wrapGen(${JSON.stringify(sym.formSetBackCmd)}, 'Form.setBackCommand',
    args => __push({ k: 'enter:Form.setBackCommand', form: __idOf(args[0]), menuBar: __idOf(args[0] && args[0][${JSON.stringify(sym.menuBarField)}]) })
  );` : ''}
  ${sym.formSetMenuBar ? `wrapGen(${JSON.stringify(sym.formSetMenuBar)}, 'Form.setMenuBar',
    args => __push({ k: 'enter:Form.setMenuBar', form: __idOf(args[0]), newMenuBar: __idOf(args[1]) })
  );` : ''}
  rewireDispatchTables();
  console.log('[trace] hooks installed');
};
`;
const hookPath = path.join(distDir, '__hooks.js');
fs.writeFileSync(hookPath, hookSrc);

// Patch worker.js so importScripts loads the hook script *between*
// translated_app.js and the runtime kicks off, then call __cn1InstallHooks
// at startup-message time (right before jvm.start()).
const workerPath = path.join(distDir, 'worker.js');
let workerSrc = fs.readFileSync(workerPath, 'utf8');
if (!workerSrc.includes('__hooks.js')) {
  workerSrc = workerSrc.replace(
    "importScripts('initializr_native_bindings.js');",
    "importScripts('initializr_native_bindings.js');\nimportScripts('__hooks.js');\nif (typeof self.__cn1InstallHooks === 'function') self.__cn1InstallHooks();"
  );
  fs.writeFileSync(workerPath, workerSrc);
}

const PORT = 8772;
const server = spawn('python3', ['-m', 'http.server', String(PORT), '--directory', distDir], { stdio: 'pipe' });
await new Promise(r => setTimeout(r, 1500));

const browser = await chromium.launch({ headless: true });
const page = await browser.newPage({ viewport: { width: 1280, height: 900 } });
const messages = [];
page.on('console', msg => { messages.push(`[${msg.type()}] ${msg.text()}`); });
page.on('pageerror', err => messages.push(`[error] ${err.message}`));

const failures = [];
function expect(cond, label) {
  if (!cond) failures.push(label);
}

await page.goto(`http://localhost:${PORT}/`);
await page.waitForFunction(() => window.cn1Started === true, { timeout: 30000 }).catch(() => {});
const bootStart = Date.now();
while (Date.now() - bootStart < 90000) {
  if (messages.some(m => m.includes('main-thread-completed'))) break;
  await new Promise(r => setTimeout(r, 500));
}
await new Promise(r => setTimeout(r, 4000));

const exceptionsBeforeInteraction = messages.filter(m => m.includes('Exception:')).length;
console.log(`exceptions after boot: ${exceptionsBeforeInteraction}`);

// Capture canvas state pre-interaction
async function snapshotCanvas(label) {
  const out = path.join('/tmp', `init-test-${label}.png`);
  await page.locator('canvas').screenshot({ path: out }).catch(() => {});
  return out;
}
const beforePng = await snapshotCanvas('before-click');
console.log('saved:', beforePng);

// Get hash of canvas data so we can detect black-square-style corruption.
async function canvasSignature() {
  return await page.evaluate(() => {
    const c = document.querySelector('canvas');
    if (!c) return null;
    const w = c.width, h = c.height;
    const ctx = c.getContext('2d');
    const img = ctx.getImageData(0, 0, w, h).data;
    const sx = Math.floor(w / 16), sy = Math.floor(h / 16);
    let s = '';
    let blackPixels = 0, totalSamples = 0;
    for (let y = 0; y < 16; y++) {
      for (let x = 0; x < 16; x++) {
        const px = (y * sy * w + x * sx) * 4;
        const lum = (img[px] + img[px + 1] + img[px + 2]) / 3 | 0;
        s += lum.toString(16).padStart(2, '0');
        totalSamples++;
        if (lum < 8 && img[px + 3] > 0) blackPixels++;
      }
    }
    return { sig: s, blackFrac: blackPixels / totalSamples };
  });
}

const sigBefore = await canvasSignature();
console.log('canvas signature pre-click:', sigBefore && sigBefore.sig.substring(0, 32));
console.log('black fraction pre-click:', sigBefore && sigBefore.blackFrac.toFixed(3));

// === Test 1: Dialog freeze ===
console.log('\n=== Test 1: Dialog.show via Hello World button ===');
const messagesBeforeClick = messages.length;
const helloCandidates = [[936, 141], [936, 145], [936, 138], [946, 141], [926, 141]];
for (const [hx, hy] of helloCandidates) {
  await page.mouse.click(hx, hy);
  await new Promise(r => setTimeout(r, 600));
}
await new Promise(r => setTimeout(r, 3000));

const newMessages = messages.slice(messagesBeforeClick);
const helloDialogShown = newMessages.some(m => m.includes('Hello Codename One') || m.includes('Welcome to Codename One'));
const exceptionsAfterClick = newMessages.filter(m => m.includes('Exception:')).length;
console.log('messages added after click:', newMessages.length);
console.log('exceptions after click:', exceptionsAfterClick);
console.log('hello-dialog text in log:', helloDialogShown);
expect(exceptionsAfterClick === 0, `Test 1: Dialog click triggered ${exceptionsAfterClick} new exceptions`);

const afterHelloPng = await snapshotCanvas('after-hello-click');
console.log('saved:', afterHelloPng);

// Try to click an OK button in the dialog to dismiss
await new Promise(r => setTimeout(r, 1000));
const messagesBeforeOk = messages.length;
await page.mouse.click(640, 450);
await new Promise(r => setTimeout(r, 1500));
const newAfterOk = messages.slice(messagesBeforeOk);
expect(newAfterOk.filter(m => m.includes('Exception:')).length === 0,
  `Test 1: clicking dialog OK position triggered exceptions`);

// === Test 2: Repeated interaction — black squares ===
// Beefed up: more iterations, longer drags, drags that move *across* the
// canvas (not just 5px nudges), keyboard input on focused fields, and
// rapid-fire clicks designed to overlap with the EDT paint cadence.
console.log('\n=== Test 2: Repeated interactions (black-square detection) ===');
const baseSig = sigBefore;
let darkenedFrames = 0;
let maxDelta = 0;
const interactions = [];
function addInteraction(label, fn) { interactions.push({ label, fn }); }

// Drag across the form vertically (scroll-y attempt)
addInteraction('drag-vertical-long', async () => {
  await page.mouse.move(640, 200);
  await page.mouse.down();
  for (let y = 200; y <= 800; y += 30) {
    await page.mouse.move(640, y);
    await new Promise(r => setTimeout(r, 8));
  }
  await page.mouse.up();
});

// Drag across the form horizontally
addInteraction('drag-horizontal-long', async () => {
  await page.mouse.move(150, 500);
  await page.mouse.down();
  for (let x = 150; x <= 1200; x += 50) {
    await page.mouse.move(x, 500);
    await new Promise(r => setTimeout(r, 8));
  }
  await page.mouse.up();
});

// Rapid clicks on a row of options
addInteraction('rapid-clicks-IDE-row', async () => {
  for (let i = 0; i < 6; i++) {
    await page.mouse.click(180 + i * 80, 525);
    await new Promise(r => setTimeout(r, 50));
  }
});

addInteraction('rapid-clicks-theme-row', async () => {
  for (let i = 0; i < 6; i++) {
    await page.mouse.click(180 + i * 80, 580);
    await new Promise(r => setTimeout(r, 50));
  }
});

// Type in any focused text field
addInteraction('keyboard-input', async () => {
  await page.mouse.click(300, 300);
  await new Promise(r => setTimeout(r, 200));
  await page.keyboard.type('TestProject123', { delay: 30 });
});

// Quick wheel scroll
addInteraction('wheel-scroll', async () => {
  await page.mouse.move(640, 500);
  await page.mouse.wheel(0, 600);
  await new Promise(r => setTimeout(r, 100));
  await page.mouse.wheel(0, -600);
});

// Resize the viewport (forces full repaint cycle) — likely place where
// double-buffer race shows up.
addInteraction('viewport-resize', async () => {
  await page.setViewportSize({ width: 1024, height: 768 });
  await new Promise(r => setTimeout(r, 400));
  await page.setViewportSize({ width: 1280, height: 900 });
  await new Promise(r => setTimeout(r, 400));
});

// Click + drag overlapping with paint
addInteraction('quick-clicks-on-preview', async () => {
  for (let i = 0; i < 5; i++) {
    await page.mouse.click(936 + (i % 3) * 10, 141 + (i % 2) * 10);
    await new Promise(r => setTimeout(r, 30));
  }
});

const blackFractions = [];
for (let i = 0; i < interactions.length; i++) {
  const t = interactions[i];
  console.log(`  interaction ${i}: ${t.label}`);
  await t.fn();
  await new Promise(r => setTimeout(r, 350));
  const sig = await canvasSignature();
  if (sig && baseSig) {
    blackFractions.push({ label: t.label, frac: sig.blackFrac });
    const delta = sig.blackFrac - baseSig.blackFrac;
    maxDelta = Math.max(maxDelta, delta);
    if (delta > 0.05) {
      darkenedFrames++;
      console.log(`    blackFrac=${sig.blackFrac.toFixed(3)} (delta=+${delta.toFixed(3)}) — DARKENED`);
      await snapshotCanvas(`dark-${i}-${t.label}`);
    } else {
      console.log(`    blackFrac=${sig.blackFrac.toFixed(3)} (delta=${delta >= 0 ? '+' : ''}${delta.toFixed(3)})`);
    }
  }
}
console.log(`darkened frames: ${darkenedFrames}/${interactions.length}, maxDelta=${maxDelta.toFixed(3)}`);
expect(darkenedFrames < 2, `Test 2: ${darkenedFrames}/${interactions.length} interactions caused unusual blackness — likely black-square corruption`);

await snapshotCanvas('after-many-interactions');

// === Diagnostic: dump the worker-side trace events ===
const trace = await page.evaluate(() => {
  // We need to ask the worker for its trace because hooks live in the
  // worker. Workers are not directly accessible from main, but the page
  // talks to the worker via postMessage; instead we use a side-channel:
  // browser_bridge.js exposes ``window.__cn1Trace`` only if the bridge
  // chose to mirror it. We instead retrieve via a postMessage round-trip
  // by recording onto window as a fallback.
  return window.__cn1WorkerTrace || null;
});
console.log('trace from page-side:', trace);

// === Final summary ===
await browser.close();
server.kill();

const exceptions = messages.filter(m => m.includes('Exception:'));
const traceLines = messages.filter(m => m.includes('[trace]'));
const uniqueExceptions = new Map();
for (const ex of exceptions) {
  const key = ex.split('|').slice(0, 2).join('|').substring(0, 100);
  uniqueExceptions.set(key, (uniqueExceptions.get(key) || 0) + 1);
}
console.log('\n=== Summary ===');
console.log('total messages:', messages.length);
console.log('total Exception lines:', exceptions.length);
console.log('total trace lines:', traceLines.length);
console.log('unique exception types:');
for (const [k, n] of uniqueExceptions) console.log(`  ${n}x ${k}`);

// Print key trace entries that bear on the parent-null question.
console.log('\n=== MenuBar/Form trace events (last 60) ===');
const interesting = traceLines.filter(m => /MenuBar|initLaf|setBackCommand|setMenuBar/.test(m));
const tail = interesting.slice(-60);
for (const line of tail) console.log(' ', line);

console.log('\nfailures:');
if (failures.length === 0) console.log('  (none)');
for (const f of failures) console.log(`  - ${f}`);

fs.writeFileSync('/tmp/init-interaction.log', messages.join('\n'));
console.log('full log: /tmp/init-interaction.log');
process.exit(failures.length === 0 && exceptions.length === 0 ? 0 : 1);
