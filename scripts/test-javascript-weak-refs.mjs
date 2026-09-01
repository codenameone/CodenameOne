#!/usr/bin/env node
//
// Regression gate for createSoftWeakRef / extractHardRef in port.js.
//
// Display.createSoftWeakRef is the framework's one way of saying "cache this,
// but let the collector have it back". EncodedImage's decode cache, Image's
// scale cache, Border's round-rect cache, Resources' cached resource,
// CacheMap.weakCache and com.codename1.ui.util.WeakHashMap are all built on it.
//
// The JavaScript port used to implement it with a WeakMap: a throwaway {} was
// the key, the referent was the VALUE, and the key travelled back to Java as
// the token. That is the wrong way round -- a WeakMap holds its keys weakly and
// its values STRONGLY -- so the referent stayed reachable for exactly as long as
// the token, which is exactly as long as an ordinary strong field. Nothing was
// ever reclaimable, and the two callers that OWN the token map rather than
// borrow it (CacheMap.weakCache and WeakHashMap, both plain hashtables that drop
// an entry only on an explicit remove/clear) therefore grew without bound.
//
// The fix is WeakRef, which is the direct analogue of java.lang.ref.WeakReference.
// This test drives the two bindNative callbacks out of port.js against a stub
// jvm, so it runs in milliseconds and needs no browser.
//
// Note what is deliberately NOT asserted here: that a referent is actually
// collected. That needs --expose-gc and a GC to happen to run, which is a timing
// assertion and would be flaky. What IS asserted is the property the bug was
// about and which decides the outcome: the token is a real WeakRef holding the
// referent as its target, and no strong container anywhere retains it.
//
// Usage: node scripts/test-javascript-weak-refs.mjs

import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { dirname, join } from 'node:path';
import vm from 'node:vm';

const here = dirname(fileURLToPath(import.meta.url));
const portPath = join(here, '..', 'Ports', 'JavaScriptPort', 'src', 'main', 'webapp', 'port.js');
const source = readFileSync(portPath, 'utf8');

/**
 * Extract the `bindNative([...], function(...) {...});` call whose name list
 * contains `nativeName`, by brace matching from the bindNative that precedes it.
 */
function extractBinding(nativeName) {
  const marker = source.indexOf('"' + nativeName + '"');
  if (marker < 0) {
    throw new Error('port.js no longer binds ' + nativeName
      + ' -- ParparVM binds natives by mangled name, so a rename here silently '
      + 'un-binds the native and the @JSBody twin runs instead');
  }
  const start = source.lastIndexOf('bindNative(', marker);
  if (start < 0) {
    throw new Error('no bindNative call precedes ' + nativeName);
  }
  let depth = 0;
  let seenBody = false;
  for (let i = start; i < source.length; i++) {
    const c = source[i];
    if (c === '(') {
      depth++;
      seenBody = true;
    } else if (c === ')') {
      depth--;
      if (seenBody && depth === 0) {
        return source.slice(start, i + 1);
      }
    }
  }
  throw new Error('unbalanced parens extracting ' + nativeName);
}

const CREATE = 'cn1_com_codename1_impl_html5_HTML5Implementation_createSoftWeakRefImpl_java_lang_Object_R_com_codename1_html5_js_JSObject';
const EXTRACT = 'cn1_com_codename1_impl_html5_HTML5Implementation_extractHardRefImpl_com_codename1_html5_js_JSObject_R_java_lang_Object';
const SUPPORTED = 'cn1_com_codename1_impl_html5_HTML5Implementation_isWeakRefSupported_R_boolean';

/** A VM object as the translated code sees one: it carries a __classDef. */
function vmObject(name) {
  return { __classDef: { name: name }, __class: name, __id: name };
}

/**
 * Stand-in for the pieces of the runtime the two bindings touch. wrapJsObject
 * is the real one's contract: a foreign JS value gets a Java-visible wrapper
 * whose __jsValue is that value, cached in a WeakMap so the same value wraps to
 * the same wrapper.
 */
function makeJvm() {
  const wrappers = new WeakMap();
  return {
    wrapped: [],
    unwrapJsValue(value) {
      return value && value.__jsValue !== undefined ? value.__jsValue : value;
    },
    wrapJsObject(value, expectedClass) {
      if (value == null || (typeof value !== 'object' && typeof value !== 'function')) {
        return value;
      }
      let wrapper = wrappers.get(value);
      if (!wrapper) {
        wrapper = { __jsValue: value, __class: expectedClass || 'com_codename1_html5_js_JSObject' };
        wrappers.set(value, wrapper);
      }
      this.wrapped.push(wrapper);
      return wrapper;
    },
    inferJsObjectClass(value, expectedClass) {
      return expectedClass || 'com_codename1_html5_js_JSObject';
    },
  };
}

/** Load the two bindings into a sandbox whose globals we control. */
function loadBindings({ withWeakRef }) {
  const bound = {};
  const sandbox = {
    console,
    WeakMap,
    WeakRef: withWeakRef ? WeakRef : undefined,
    jvm: makeJvm(),
    bindNative(names, fn) {
      for (const name of names) {
        bound[name] = fn;
      }
    },
  };
  // `global` is what the bindings reach for when they want the realm object;
  // in the worker it is `self`. Give them one that is NOT the sandbox itself so
  // any stray write to a "global weak map" is visible to the assertions below.
  sandbox.global = { window: {} };
  vm.createContext(sandbox);
  vm.runInContext(
    extractBinding(SUPPORTED) + '\n'
    + extractBinding(CREATE) + '\n'
    + extractBinding(EXTRACT) + '\n',
    sandbox);
  return { bound, sandbox };
}

const failures = [];

function check(label, condition, detail) {
  if (condition) {
    console.log('  ok   ' + label);
  } else {
    console.log('  FAIL ' + label + (detail ? ' -- ' + detail : ''));
    failures.push(label);
  }
}

console.log('weak references: the token must be a WeakRef, not a strong map entry');

// ---------------------------------------------------------------- WeakRef present
{
  const { bound, sandbox } = loadBindings({ withWeakRef: true });
  const create = bound[CREATE];
  const extract = bound[EXTRACT];
  const supported = bound[SUPPORTED];

  check('the support probe reports WeakRef, not WeakMap', supported() === 1,
    'probe returned ' + supported());

  const referent = vmObject('com_codename1_ui_Image');
  const token = create(referent);

  check('create answers a token', token != null && typeof token === 'object',
    String(token));

  const inner = sandbox.jvm.unwrapJsValue(token);
  check('the token IS a WeakRef (this is the whole fix)', inner instanceof WeakRef,
    'token holds ' + Object.prototype.toString.call(inner));
  check('the WeakRef targets the referent', inner instanceof WeakRef && inner.deref() === referent);

  check('extract answers the identical referent -- not a re-wrapped shell',
    extract(token) === referent, String(extract(token)));

  // The regression itself: the old implementation parked the referent as the
  // VALUE of a WeakMap hung off the realm object, which is a strong edge.
  check('no global weak map is created on the realm',
    sandbox.global.cn1GlobalWeakMap === undefined
    && sandbox.global.window.cn1GlobalWeakMap === undefined,
    'realm gained ' + String(sandbox.global.cn1GlobalWeakMap)
      + ' / ' + String(sandbox.global.window.cn1GlobalWeakMap));

  // The token is the only thing Java holds. Nothing that survives this call may
  // name the referent through a strong own property, or the WeakRef is decorative.
  const strongOwners = sandbox.jvm.wrapped.filter(
    (w) => w.__jsValue === referent);
  check('nothing wraps the referent itself in a strong holder',
    strongOwners.length === 0, strongOwners.length + ' strong wrapper(s)');

  // A collected referent reads back as a miss rather than throwing: deref()
  // answers undefined, which is the shape extract must survive.
  const collected = sandbox.jvm.wrapJsObject({ deref() { return undefined; } },
    'com_codename1_html5_js_JSObject');
  check('a collected referent extracts as null', extract(collected) === null,
    String(extract(collected)));

  // Repeated create for the same referent must not alias: each token is its own
  // reference, exactly as `new WeakReference(o)` is each time it is called.
  const second = create(referent);
  check('two tokens for one referent are distinct references',
    second !== token && sandbox.jvm.unwrapJsValue(second) !== inner);
  check('both tokens still resolve to the referent',
    extract(second) === referent && extract(token) === referent);

  // A foreign JS value (not a VM object) must round-trip through the wrapper path.
  const foreign = { hostThing: true };
  const foreignToken = create(foreign);
  const back = extract(foreignToken);
  check('a foreign JS value round-trips wrapped',
    back != null && sandbox.jvm.unwrapJsValue(back) === foreign, String(back));

  // A primitive cannot be a WeakRef target; declining is what lets the Java side
  // fall back to a strong ref instead of the native throwing a TypeError.
  let threw = null;
  let primitiveToken;
  try {
    primitiveToken = create(42);
  } catch (e) {
    threw = e;
  }
  check('a non-object referent is declined, not thrown on',
    threw === null && primitiveToken === null,
    threw ? String(threw) : String(primitiveToken));

  check('a null token extracts as null', extract(null) === null);
}

// ---------------------------------------------------------------- WeakRef absent
{
  const { bound, sandbox } = loadBindings({ withWeakRef: false });

  check('the support probe reports false without WeakRef', bound[SUPPORTED]() === 0);

  let threw = null;
  let token;
  try {
    token = bound[CREATE](vmObject('com_codename1_ui_Image'));
  } catch (e) {
    threw = e;
  }
  check('create declines rather than throwing when WeakRef is missing',
    threw === null && token === null, threw ? String(threw) : String(token));
  check('no global weak map is created on the fallback path either',
    sandbox.global.cn1GlobalWeakMap === undefined
    && sandbox.global.window.cn1GlobalWeakMap === undefined);
}

if (failures.length) {
  console.error('\nFAILED: ' + failures.length + ' assertion(s)');
  process.exit(1);
}
console.log('\nAll weak reference assertions passed.');
