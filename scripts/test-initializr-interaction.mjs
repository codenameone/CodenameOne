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
  // OK-button-doesn't-dismiss diagnostics:
  buttonReleased: mangledFor('cn1_com_codename1_ui_Button_released_int_int'),
  buttonFireActionEvent: mangledFor('cn1_com_codename1_ui_Button_fireActionEvent_int_int'),
  implSetCurrentForm: mangledFor('cn1_com_codename1_impl_CodenameOneImplementation_setCurrentForm_com_codename1_ui_Form'),
  displaySetCurrent: mangledFor('cn1_com_codename1_ui_Display_setCurrent_com_codename1_ui_Form_boolean'),
  formInitTransition: mangledFor('cn1_com_codename1_ui_Display_initTransition_com_codename1_ui_animations_Transition_com_codename1_ui_Form_com_codename1_ui_Form_R_boolean'),
  formShow: mangledFor('cn1_com_codename1_ui_Form_show'),
  dialogShow: mangledFor('cn1_com_codename1_ui_Dialog_show'),
  dialogShowImpl: mangledFor('cn1_com_codename1_ui_Dialog_showImpl_boolean'),
  implCurrentFormField: mangledFor('cn1_com_codename1_impl_CodenameOneImplementation_currentForm'),
  displayAnimationQueue: mangledFor('cn1_com_codename1_ui_Display_animationQueue'),
  formShowBoolean: mangledFor('cn1_com_codename1_ui_Form_show_boolean'),
  formShowModal: mangledFor('cn1_com_codename1_ui_Form_showModal_int_int_int_int_boolean_boolean_boolean'),
  displaySetCurrentForm: mangledFor('cn1_com_codename1_ui_Display_setCurrentForm_com_codename1_ui_Form'),
  formPointerReleased: mangledFor('cn1_com_codename1_ui_Form_pointerReleased_int_int'),
  formPointerPressed: mangledFor('cn1_com_codename1_ui_Form_pointerPressed_int_int'),
  formGetComponentAt: mangledFor('cn1_com_codename1_ui_Form_getResponderAt_int_int_R_com_codename1_ui_Component'),
  containerGetComponentAt: mangledFor('cn1_com_codename1_ui_Container_getComponentAt_int_int_R_com_codename1_ui_Component'),
  formActionCommandImplNoRecurse: mangledFor('cn1_com_codename1_ui_Form_actionCommandImplNoRecurseComponent_com_codename1_ui_Command_com_codename1_ui_events_ActionEvent'),
  dialogActionCommand: mangledFor('cn1_com_codename1_ui_Dialog_actionCommand_com_codename1_ui_Command'),
  formActionCommand: mangledFor('cn1_com_codename1_ui_Form_actionCommand_com_codename1_ui_Command'),
  dialogDispose: mangledFor('cn1_com_codename1_ui_Dialog_dispose'),
  dialogIsDisposed: mangledFor('cn1_com_codename1_ui_Dialog_isDisposed_R_boolean'),
  formIsDisposed: mangledFor('cn1_com_codename1_ui_Form_isDisposed_R_boolean'),
  disposedField: mangledFor('cn1_com_codename1_ui_Dialog_disposed'),
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
    const summary = {};
    for (const clsName in jvm.classes) {
      const cls = jvm.classes[clsName];
      if (!cls || !cls.methods) continue;
      for (const dispatchId in cls.methods) {
        const entry = cls.methods[dispatchId];
        for (const targetName in wrappedTargets) {
          if (entry === wrappedTargets[targetName].orig) {
            cls.methods[dispatchId] = wrappedTargets[targetName].wrapped;
            count++;
            summary[targetName] = (summary[targetName] || 0) + 1;
          }
        }
      }
    }
    if (jvm.resolvedVirtualCache) {
      jvm.resolvedVirtualCache = Object.create(null);
    }
    console.log('[trace] rewired ' + count + ' dispatch entries summary=' + JSON.stringify(summary));
    // Log targets that NEVER got rewired - these are the wraps that never fire via virtual dispatch
    for (const t in wrappedTargets) {
      if (!summary[t]) console.log('[trace] WARN: wrap ' + t + ' was never matched in any cls.methods entry');
    }
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
  ${sym.formPointerPressed ? `wrapGen(${JSON.stringify(sym.formPointerPressed)}, 'Form.pointerPressed',
    args => __push({ k: 'enter:Form.pointerPressed', form: __idOf(args[0]), x: args[1], y: args[2] })
  );` : ''}
  ${sym.formPointerReleased ? `wrapGen(${JSON.stringify(sym.formPointerReleased)}, 'Form.pointerReleased',
    args => __push({ k: 'enter:Form.pointerReleased', form: __idOf(args[0]), x: args[1], y: args[2] })
  );` : ''}
  ${sym.buttonReleased ? `wrapGen(${JSON.stringify(sym.buttonReleased)}, 'Button.released',
    args => __push({ k: 'enter:Button.released', btn: __idOf(args[0]), x: args[1], y: args[2] })
  );` : ''}
  ${sym.buttonFireActionEvent ? `wrapGen(${JSON.stringify(sym.buttonFireActionEvent)}, 'Button.fireActionEvent',
    args => __push({ k: 'enter:Button.fireActionEvent', btn: __idOf(args[0]) })
  );` : ''}
  ${sym.formActionCommandImplNoRecurse ? `wrapGen(${JSON.stringify(sym.formActionCommandImplNoRecurse)}, 'Form.actionCommandImplNoRecurseComponent',
    args => __push({ k: 'enter:Form.actionCommandImplNoRecurseComponent', form: __idOf(args[0]), cmd: __idOf(args[1]) })
  );` : ''}
  ${sym.formActionCommand ? `wrapGen(${JSON.stringify(sym.formActionCommand)}, 'Form.actionCommand',
    args => __push({ k: 'enter:Form.actionCommand', form: __idOf(args[0]), cmd: __idOf(args[1]) })
  );` : ''}
  ${sym.dialogActionCommand ? `wrapGen(${JSON.stringify(sym.dialogActionCommand)}, 'Dialog.actionCommand',
    args => __push({ k: 'enter:Dialog.actionCommand', dlg: __idOf(args[0]), cmd: __idOf(args[1]) })
  );` : ''}
  ${sym.dialogDispose ? `wrapGen(${JSON.stringify(sym.dialogDispose)}, 'Dialog.dispose',
    args => __push({ k: 'enter:Dialog.dispose', dlg: __idOf(args[0]), disposed_field: args[0] && args[0][${JSON.stringify(sym.disposedField)}] })
  );` : ''}
  ${sym.dialogIsDisposed ? `wrapGen(${JSON.stringify(sym.dialogIsDisposed)}, 'Dialog.isDisposed',
    args => __push({ k: 'enter:Dialog.isDisposed', dlg: __idOf(args[0]), disposed_field: args[0] && args[0][${JSON.stringify(sym.disposedField)}] })
  );` : ''}
  rewireDispatchTables();
  let lastSeen = 'NOT-INIT';
  let pollErrCount = 0;
  let pollTickCount = 0;
  const currentFormField = ${JSON.stringify(sym.implCurrentFormField || '$aI5')};
  setInterval(function() {
    pollTickCount++;
    try {
      const jvm = self.jvm;
      if (!jvm || !jvm.classes) {
        if (lastSeen === 'NOT-INIT' && pollTickCount % 30 === 1) console.log('[trace] currentForm poll: no jvm');
        return;
      }
      // jvm.classes is keyed on mangled class symbols. Walk it once and
      // pick the entry whose def.name suggests Display.
      let dispCls = null;
      for (const k in jvm.classes) {
        const c = jvm.classes[k];
        if (c && c.staticFields && c.staticFields.INSTANCE !== undefined && c.staticFields.lock !== undefined && c.staticFields.impl !== undefined) {
          // Display has these three statics — likely it.
          dispCls = c;
          break;
        }
      }
      if (!dispCls) {
        if (lastSeen === 'NOT-INIT' && pollTickCount % 30 === 1) console.log('[trace] currentForm poll: no Display class found');
        return;
      }
      // Display.impl is a static field on the Display class.
      const impl = dispCls.staticFields.impl;
      if (!impl) {
        if (lastSeen === 'NOT-INIT' && pollTickCount % 30 === 1) console.log('[trace] currentForm poll: Display.impl is null');
        return;
      }
      const cur = impl[currentFormField];
      const sig = cur ? (cur.__class + '#' + (cur.__id || '?')) : 'null';
      // Also examine the Display animationQueue — a queued transition
      // is what defers setCurrentForm in the show flow.
      let aqSig = '?';
      try {
        const aqField = ${JSON.stringify(sym.displayAnimationQueue || '')};
        if (aqField) {
          const inst = dispCls.staticFields.INSTANCE;
          if (inst) {
            const aq = inst[aqField];
            aqSig = aq ? ('len=' + (aq.cn1_java_util_ArrayList_size != null ? aq.cn1_java_util_ArrayList_size : '?')) : 'null';
          }
        }
      } catch (_) {}
      if (sig !== lastSeen) {
        console.log('[trace] currentForm CHANGED from=' + lastSeen + ' to=' + sig + ' aq=' + aqSig);
        lastSeen = sig;
      }
    } catch (e) {
      pollErrCount++;
      if (pollErrCount <= 3) console.log('[trace] currentForm poll err: ' + (e && e.message ? e.message : e));
    }
  }, 200);
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

// Match a typical macOS Retina viewer (DPR=2). Several painters and the
// pointer-event coord transformer multiply/divide by DPR; bugs that
// only appear at non-1 DPR get missed at the headless default.
const browser = await chromium.launch({ headless: true });
const page = await browser.newPage({
  viewport: { width: 1280, height: 900 },
  deviceScaleFactor: 2,
});
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
// The user-reported bug is: a region of the canvas gets cleared (alpha=0
// or fully transparent) and never repainted, so the iframe parent's dark
// background shows through. ``blackFrac`` counts opaque-black pixels;
// ``transparentFrac`` counts genuinely cleared pixels — the latter is the
// real signal for the reported regression.
async function canvasSignature() {
  return await page.evaluate(() => {
    const c = document.querySelector('canvas');
    if (!c) return null;
    const w = c.width, h = c.height;
    const ctx = c.getContext('2d');
    const img = ctx.getImageData(0, 0, w, h).data;
    const sx = Math.floor(w / 16), sy = Math.floor(h / 16);
    let s = '';
    let blackPixels = 0, transparentPixels = 0, totalSamples = 0;
    for (let y = 0; y < 16; y++) {
      for (let x = 0; x < 16; x++) {
        const px = (y * sy * w + x * sx) * 4;
        const lum = (img[px] + img[px + 1] + img[px + 2]) / 3 | 0;
        s += lum.toString(16).padStart(2, '0');
        totalSamples++;
        if (lum < 8 && img[px + 3] > 0) blackPixels++;
        if (img[px + 3] === 0) transparentPixels++;
      }
    }
    return {
      sig: s,
      blackFrac: blackPixels / totalSamples,
      transparentFrac: transparentPixels / totalSamples,
    };
  });
}

const sigBefore = await canvasSignature();
console.log('canvas signature pre-click:', sigBefore && sigBefore.sig.substring(0, 32));
console.log('black fraction pre-click:', sigBefore && sigBefore.blackFrac.toFixed(3));

// === Test 1: Dialog freeze ===
console.log('\n=== Test 1: Dialog.show via Hello World button ===');
const messagesBeforeClick = messages.length;
await page.mouse.click(936, 141);
await new Promise(r => setTimeout(r, 4000));

const newMessages = messages.slice(messagesBeforeClick);
const helloDialogShown = newMessages.some(m => m.includes('Hello Codename One') || m.includes('Welcome to Codename One'));
const exceptionsAfterClick = newMessages.filter(m => m.includes('Exception:')).length;
console.log('messages added after click:', newMessages.length);
console.log('exceptions after click:', exceptionsAfterClick);
console.log('hello-dialog text in log:', helloDialogShown);
expect(exceptionsAfterClick === 0, `Test 1: Dialog click triggered ${exceptionsAfterClick} new exceptions`);

const afterHelloPng = await snapshotCanvas('after-hello-click');
console.log('saved:', afterHelloPng);

// Try to click the OK button in the dialog to dismiss. The dialog
// renders centered horizontally around the page mid-x; OK lives at the
// bottom of the dialog. The user reports OK doesn't dismiss — verify
// by checking whether the canvas signature changes back toward the
// pre-dialog state after clicking OK. If it doesn't, the dispose chain
// is broken.
await new Promise(r => setTimeout(r, 1000));
const messagesBeforeOk = messages.length;
const sigWithDialog = await canvasSignature();
console.log('canvas-with-dialog blackFrac:', sigWithDialog.blackFrac.toFixed(3));
// At DPR=2 in a 1280x900 viewport, the OK label sits roughly at CSS
// (574, 455). Click ONCE and give the worker a long time to process.
await page.mouse.move(574, 455);
await new Promise(r => setTimeout(r, 100));
await page.mouse.down();
await new Promise(r => setTimeout(r, 200));
await page.mouse.up();
await new Promise(r => setTimeout(r, 3000));
const sigAfterOk = await canvasSignature();
console.log('canvas-after-OK blackFrac:', sigAfterOk.blackFrac.toFixed(3));
// If the dialog dismissed, the canvas signature should differ
// significantly from "with-dialog". Compute hamming distance over the
// 16x16 luminance grid.
function sigHamming(a, b) {
  if (!a || !b) return -1;
  let diff = 0;
  for (let i = 0; i < a.sig.length; i += 2) {
    if (a.sig.substring(i, i+2) !== b.sig.substring(i, i+2)) diff++;
  }
  return diff;
}
const dialogToOk = sigHamming(sigWithDialog, sigAfterOk);
const okToBefore = sigHamming(sigBefore, sigAfterOk);
console.log('grid-cells-changed dialog->after-OK:', dialogToOk, 'after-OK vs before-click:', okToBefore);
const newAfterOk = messages.slice(messagesBeforeOk);
expect(newAfterOk.filter(m => m.includes('Exception:')).length === 0,
  `Test 1: clicking dialog OK position triggered exceptions`);
// The dialog dismissing should bring the canvas closer to the original
// (pre-dialog) state than to the with-dialog state.
expect(okToBefore < dialogToOk,
  `Test 1: OK click did NOT dismiss the dialog (after-OK still looks like with-dialog: ${dialogToOk} vs ${okToBefore} cells changed)`);

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

// User report: clicking the "Main Class" text field and typing makes
// the corresponding label go black. The label sits above the field, and
// the dark square shifts with the click position. Try every textfield
// region in the form: click, focus, type, screenshot, then look for a
// dark band that appeared *above* the click point. We also save a wider
// PNG (full canvas) so a human reviewer can spot the artifact.
addInteraction('click-textfield-and-type', async () => {
  // The form layout has labels above each input. Walk down the form and
  // click candidate "input row" Y-coordinates one at a time, typing into
  // each, screenshotting after every step.
  const fieldYs = [225, 290, 355, 420, 485, 550, 615, 680];
  for (let i = 0; i < fieldYs.length; i++) {
    const y = fieldYs[i];
    const x = 300; // form is on left side, this is roughly mid-field
    await page.mouse.click(x, y);
    await new Promise(r => setTimeout(r, 250));
    await page.keyboard.type('Foo', { delay: 60 });
    await new Promise(r => setTimeout(r, 150));
    // Look at a vertical strip 100px tall above the click for darkening.
    const stripDark = await page.evaluate(({ cx, cy }) => {
      const c = document.querySelector('canvas');
      if (!c) return 0;
      const ctx = c.getContext('2d');
      const dpr = window.devicePixelRatio || 1;
      const px = Math.floor(cx * dpr);
      const py = Math.floor(cy * dpr);
      const stripH = Math.floor(80 * dpr);
      const stripW = Math.floor(160 * dpr);
      let blackPixels = 0;
      let total = 0;
      try {
        const img = ctx.getImageData(Math.max(0, px - stripW/2), Math.max(0, py - stripH), stripW, stripH).data;
        for (let p = 0; p < img.length; p += 4) {
          total++;
          const lum = (img[p] + img[p+1] + img[p+2]) / 3 | 0;
          if (lum < 8 && img[p+3] > 0) blackPixels++;
        }
      } catch (_) {}
      return total > 0 ? blackPixels / total : 0;
    }, { cx: x, cy: y });
    console.log(`    field-strip y=${y}: blackFrac-above=${stripDark.toFixed(3)}`);
    if (stripDark > 0.05) {
      console.log(`      ↑ DARK BAND DETECTED above click y=${y}`);
      await snapshotCanvas(`dark-band-y${y}`);
    }
    // dismiss focus / commit value
    await page.keyboard.press('Tab');
    await new Promise(r => setTimeout(r, 100));
  }
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
let transparentFrames = 0;
let maxTransparentDelta = 0;
for (let i = 0; i < interactions.length; i++) {
  const t = interactions[i];
  console.log(`  interaction ${i}: ${t.label}`);
  await t.fn();
  await new Promise(r => setTimeout(r, 350));
  const sig = await canvasSignature();
  if (sig && baseSig) {
    blackFractions.push({ label: t.label, blackFrac: sig.blackFrac, transparentFrac: sig.transparentFrac });
    const delta = sig.blackFrac - baseSig.blackFrac;
    const tdelta = sig.transparentFrac - baseSig.transparentFrac;
    maxDelta = Math.max(maxDelta, delta);
    maxTransparentDelta = Math.max(maxTransparentDelta, tdelta);
    const tag = (delta > 0.05) ? ' DARKENED' : '';
    const ttag = (tdelta > 0.02) ? ' TRANSPARENT-HOLE' : '';
    if (delta > 0.05) darkenedFrames++;
    if (tdelta > 0.02) transparentFrames++;
    console.log(`    blackFrac=${sig.blackFrac.toFixed(3)} (delta=${delta >= 0 ? '+' : ''}${delta.toFixed(3)}) transparentFrac=${sig.transparentFrac.toFixed(3)} (delta=${tdelta >= 0 ? '+' : ''}${tdelta.toFixed(3)})${tag}${ttag}`);
    if (tag || ttag) await snapshotCanvas(`anomaly-${i}-${t.label}`);
  }
}
console.log(`darkened frames: ${darkenedFrames}/${interactions.length}, maxDelta=${maxDelta.toFixed(3)}`);
console.log(`transparent-hole frames: ${transparentFrames}/${interactions.length}, maxTransparentDelta=${maxTransparentDelta.toFixed(3)}`);
expect(darkenedFrames < 2, `Test 2: ${darkenedFrames}/${interactions.length} interactions caused unusual blackness — likely black-square corruption`);
expect(transparentFrames < 2, `Test 2: ${transparentFrames}/${interactions.length} interactions left transparent holes — canvas-cleared-but-not-repainted regression`);

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
