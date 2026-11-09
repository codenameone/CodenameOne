#!/usr/bin/env node
//
// Regression gate for the JavaScript runtime's dispatch and type-check caches.
//
// Painting a Codename One screen in the browser is dominated by three runtime
// helpers: virtual dispatch (resolveVirtual / cn1_iv*), the instanceof walk
// (assignableViaAncestors) and the generator wrappers around every call. Each
// now caches or short-circuits (#6195), and each cache has an invalidation
// point that must keep working:
//
// - A NEGATIVE instanceof answer is cached per class. Classes can be defined
//   out of inheritance order, so a "no" given before the superclass arrived
//   has to be dropped when defineClass runs.
// - Resolved virtual methods are cached per receiver class. A native override
//   registered later (applyNativeOverrides) has to win over the cached entry.
// - cn1_iv* / _dv* return the callee's iterator directly instead of wrapping
//   it in a generator. The value, the scheduler yield at the budget boundary
//   and exception propagation through `yield*` must be unchanged.
//
// Runs the real parparvm_runtime.js in a node:vm context with synthetic
// classes, so it needs no translation and no browser.
//
// Usage: node scripts/test-javascript-dispatch-caches.mjs

import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { dirname, join } from 'node:path';
import vm from 'node:vm';
import assert from 'node:assert/strict';

const here = dirname(fileURLToPath(import.meta.url));
const runtimePath = join(here, '..', 'vm', 'ByteCodeTranslator', 'src', 'javascript', 'parparvm_runtime.js');

let clock = 0;
const context = {
  console, TextEncoder, TextDecoder, URL, WeakRef, FinalizationRegistry,
  performance: { now: () => clock },
  setTimeout: () => 1, clearTimeout() {}, setInterval: () => 1, clearInterval() {},
  postMessage() {}
};
context.self = context;
vm.createContext(context);
vm.runInContext(readFileSync(runtimePath, 'utf8'), context, { filename: runtimePath });
const jvm = context.jvm;

function define(name, base, interfaces, methods) {
  jvm.defineClass({ n: name, b: base === undefined ? 'java_lang_Object' : base, i: interfaces || [], m: methods || {} });
  return jvm.classes[name];
}
function object(name) {
  return { __class: name, __classDef: jvm.classes[name] };
}
// Drive a generator to completion, returning its value and every yielded value.
function drive(iterator) {
  const yielded = [];
  for (;;) {
    const step = iterator.next();
    if (step.done) {
      return { value: step.value, yielded };
    }
    yielded.push(step.value);
  }
}
let failures = 0;
function check(name, fn) {
  try {
    fn();
    console.log('ok   ' + name);
  } catch (e) {
    failures++;
    console.log('FAIL ' + name + '\n     ' + (e && e.stack || e));
  }
}

define('java_lang_Object', null);

check('negative instanceof is dropped when a late superclass is defined', () => {
  define('LateChild', 'LateParent');
  define('Unrelated');
  assert.equal(jvm.instanceOf(object('LateChild'), 'LateGrandparent'), false);
  assert.equal(jvm.instanceOf(object('LateChild'), 'LateGrandparent'), false, 'cached no');
  define('LateParent', 'LateGrandparent');
  define('LateGrandparent');
  assert.equal(jvm.instanceOf(object('LateChild'), 'LateGrandparent'), true);
  assert.equal(jvm.instanceOf(object('LateChild'), 'Unrelated'), false);
});

check('negative instanceof is dropped when a late interface is defined', () => {
  define('Impl', 'java_lang_Object', ['LateIface']);
  assert.equal(jvm.instanceOf(object('Impl'), 'SuperIface'), false);
  define('LateIface', 'java_lang_Object', ['SuperIface']);
  define('SuperIface');
  assert.equal(jvm.instanceOf(object('Impl'), 'SuperIface'), true);
});

check('assignableViaAncestors answers both ways from cache consistently', () => {
  define('A1');
  define('B1', 'A1');
  for (let i = 0; i < 3; i++) {
    assert.equal(jvm.assignableViaAncestors('B1', 'A1'), true);
    assert.equal(jvm.assignableViaAncestors('B1', 'Unrelated'), false);
    assert.equal(jvm.assignableViaAncestors('A1', 'B1'), false);
  }
});

const mid = 'cn1_s_answer_R_int';
check('inherited virtual dispatch is cached per receiver class', () => {
  define('Base', 'java_lang_Object', [], { [mid]: () => 7 });
  define('Derived', 'Base');
  define('Sibling', 'Base', [], { [mid]: () => 8 });
  assert.equal(context.cn1_ivs0(object('Derived'), mid), 7);
  assert.equal(context.cn1_ivs0(object('Sibling'), mid), 8);
  assert.equal(context.cn1_ivs0(object('Derived'), mid), 7, 'cached');
  assert.equal(context.cn1_ivs0(object('Sibling'), mid), 8, 'cached, not shared with Derived');
});

check('a native override registered later replaces the cached method', () => {
  jvm.nativeMethods[mid] = () => 9;
  jvm.applyNativeOverrides();
  assert.equal(context.cn1_ivs0(object('Derived'), mid), 9);
  delete jvm.nativeMethods[mid];
});

check('generator dispatch returns a generator callee value', () => {
  const gmid = 'cn1_s_gen_R_int';
  define('GenBase', 'java_lang_Object', [], { [gmid]: function* (self, a) { yield 'inner'; return a + 1; } });
  define('GenDerived', 'GenBase');
  const r = drive((function* () { return yield* context.cn1_iv1(object('GenDerived'), gmid, 41); })());
  assert.equal(r.value, 42);
  assert.deepEqual(r.yielded, ['inner']);
});

check('generator dispatch returns a synchronous callee value', () => {
  const smid = 'cn1_s_sync_R_int';
  define('SyncBase', 'java_lang_Object', [], { [smid]: (self, a, b) => a * b });
  const r = drive((function* () { return yield* context.cn1_iv2(object('SyncBase'), smid, 6, 7); })());
  assert.equal(r.value, 42);
  assert.deepEqual(r.yielded, []);
  // A synchronous null/undefined/false result must not be mistaken for an iterator.
  define('NullBase', 'java_lang_Object', [], { [smid]: () => null, cn1_s_f_R_boolean: () => false });
  assert.equal(drive((function* () { return yield* context.cn1_iv2(object('NullBase'), smid, 1, 2); })()).value, null);
  assert.equal(drive((function* () { return yield* context.cn1_iv0(object('NullBase'), 'cn1_s_f_R_boolean'); })()).value, false);
});

check('direct dispatch (_dv*) returns both callee kinds', () => {
  const g = function* (self, a) { yield 'x'; return a; };
  const s = (self, a) => a;
  assert.equal(drive((function* () { return yield* context._dv1(object('SyncBase'), g, 5); })()).value, 5);
  assert.equal(drive((function* () { return yield* context._dv1(object('SyncBase'), s, 6); })()).value, 6);
});

check('the budget boundary still yields to the scheduler before the call', () => {
  const smid = 'cn1_s_sync_R_int';
  let calls = 0;
  define('Counted', 'java_lang_Object', [], { [smid]: () => ++calls });
  let sawYield = false;
  // _Yc samples the clock every 256th call; advance it so the next sample is over budget.
  for (let i = 0; i < 600 && !sawYield; i++) {
    clock += 1000;
    const before = calls;
    const r = drive((function* () { return yield* context.cn1_iv0(object('Counted'), smid); })());
    assert.equal(calls, before + 1, 'callee runs exactly once');
    assert.equal(r.value, calls);
    if (r.yielded.length) {
      assert.deepEqual(r.yielded, [context._Yv]);
      sawYield = true;
    }
  }
  assert.ok(sawYield, 'no scheduler yield in 600 over-budget calls');
});

check('an exception thrown by the callee propagates through yield*', () => {
  const emid = 'cn1_s_throw_R_void';
  const boom = new Error('boom');
  define('Thrower', 'java_lang_Object', [], {
    [emid]: () => { throw boom; },
    cn1_s_gthrow_R_void: function* () { yield 1; throw boom; }
  });
  for (const m of [emid, 'cn1_s_gthrow_R_void']) {
    let caught = null;
    drive((function* () { try { yield* context.cn1_iv0(object('Thrower'), m); } catch (e) { caught = e; } })());
    assert.equal(caught, boom, m);
  }
});

check('a null receiver fails instead of calling the method', () => {
  let called = false;
  const s = () => { called = true; };
  assert.throws(() => drive((function* () { yield* context._dv0(null, s); })()));
  assert.throws(() => drive((function* () { yield* context.cn1_iv0(null, 'cn1_s_sync_R_int'); })()));
  assert.equal(called, false);
});

if (failures) {
  console.log(failures + ' failure(s)');
  process.exit(1);
}
console.log('all dispatch cache checks passed');
