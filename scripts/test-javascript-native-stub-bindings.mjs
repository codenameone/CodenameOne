#!/usr/bin/env node
//
// Regression gate: every static "Native implementation" stub in the JavaScript
// port's interop interfaces (com.codename1.html5.js.*) is bound in port.js.
//
// Those stubs are ordinary Java bodies that `return null;`. The JSO bridge only
// dispatches INSTANCE members to the browser, so a static factory runs its Java
// body unless port.js replaces it with bindNative -- and an unbound one answers
// null without any error. JSString.valueOf was unbound: every
// LocalForage.setItem(String, String) stored null, which localStorage reads as a
// delete, so FileSystemStorage.mkdir (whose directory marker is an empty string)
// never created a directory and every write under a new directory failed with
// "No such file or directory". JSNumber/JSBoolean.valueOf, Int32Array.create,
// Int16Array.create and Window.encodeURIComponent were unbound the same way.
//
// The binding names are derived from the Java signatures the way the
// translator mangles them, so a renamed or newly added stub is caught too.
//
// Usage: node scripts/test-javascript-native-stub-bindings.mjs

import { readFileSync, readdirSync, statSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { dirname, join, relative, sep } from 'node:path';
import vm from 'node:vm';

const here = dirname(fileURLToPath(import.meta.url));
const root = join(here, '..');
const javaRoot = join(root, 'Ports', 'JavaScriptPort', 'src', 'main', 'java');
const interopRoot = join(javaRoot, 'com', 'codename1', 'html5', 'js');
const portSource = readFileSync(join(root, 'Ports', 'JavaScriptPort', 'src', 'main', 'webapp', 'port.js'), 'utf8');

let failures = 0;
function check(ok, message) {
  if (ok) {
    console.log('  ok   ' + message);
  } else {
    failures++;
    console.log('  FAIL ' + message);
  }
}

function javaFiles(dir) {
  const out = [];
  for (const name of readdirSync(dir)) {
    const p = join(dir, name);
    if (statSync(p).isDirectory()) {
      out.push(...javaFiles(p));
    } else if (name.endsWith('.java')) {
      out.push(p);
    }
  }
  return out;
}

const PRIMITIVES = new Set(['int', 'long', 'short', 'byte', 'char', 'float', 'double', 'boolean']);

/** The translator's name for a Java type as written in `file`. */
function mangleType(type, pkg, imports, typeVars) {
  let t = type.replace(/<.*>/, '').trim();
  let dims = 0;
  while (t.endsWith('[]')) {
    dims++;
    t = t.slice(0, -2).trim();
  }
  let name;
  if (PRIMITIVES.has(t)) {
    name = t;
  } else if (typeVars.has(t)) {
    name = 'java_lang_Object';
  } else if (t.includes('.')) {
    name = t.replace(/\./g, '_');
  } else if (imports.has(t)) {
    name = imports.get(t).replace(/\./g, '_');
  } else if (t === 'String' || t === 'Object') {
    name = 'java_lang_' + t;
  } else {
    name = (pkg + '.' + t).replace(/\./g, '_');
  }
  return dims ? name + '_' + dims + 'ARRAY' : name;
}

/** Splits a parameter list on top-level commas (generic arguments nest). */
function splitParams(params) {
  const out = [];
  let depth = 0;
  let cur = '';
  for (const c of params) {
    if (c === '<') depth++;
    if (c === '>') depth--;
    if (c === ',' && depth === 0) {
      out.push(cur);
      cur = '';
    } else {
      cur += c;
    }
  }
  if (cur.trim()) out.push(cur);
  return out;
}

console.log('static interop stubs are bound in port.js');
// `return null;`, `return 0;` or an empty void body, then the marker comment.
const stubRe = /static\s+(<[^>]*>\s*)?([\w.<>\[\], ?]+?)\s+(\w+)\s*\(([^)]*)\)\s*\{\s*(?:return\s+[\w.]+;\s*)?\/\/\s*Native implementation/g;
let stubs = 0;
for (const file of javaFiles(interopRoot)) {
  const src = readFileSync(file, 'utf8');
  const cls = relative(javaRoot, file).replace(/\.java$/, '').split(sep).join('_');
  const pkg = (src.match(/^package\s+([\w.]+);/m) || [])[1];
  const imports = new Map();
  for (const m of src.matchAll(/^import\s+([\w.]+)\s*;/gm)) {
    imports.set(m[1].slice(m[1].lastIndexOf('.') + 1), m[1]);
  }
  for (const m of src.matchAll(stubRe)) {
    stubs++;
    const typeVars = new Set(m[1] ? m[1].slice(1, -1).split(',').map(s => s.trim().split(/\s+/)[0]) : []);
    const ret = m[2].trim();
    const method = m[3];
    const args = splitParams(m[4]).map(p => {
      const parts = p.trim().split(/\s+/);
      parts.pop();
      return mangleType(parts.join(' '), pkg, imports, typeVars);
    });
    const sig = args.length ? '_' + args.join('_') : '';
    const sigLegacy = args.length ? '___' + args.join('_') : '___';
    const retPart = ret === 'void' ? '' : '_R_' + mangleType(ret, pkg, imports, typeVars);
    const names = [
      'cn1_' + cls + '_' + method + sig + retPart,
      'cn1_' + cls + '_' + method + sigLegacy + retPart,
    ];
    const bound = names.some(n => portSource.includes('"' + n + '"'));
    check(bound, cls.replace(/_/g, '.') + '.' + method + '(' + m[4].trim() + ') is bound as ' + names[0]);
  }
}
check(stubs >= 19, 'found the interop stubs (' + stubs + ')');

// A behavioural check of the binding that broke the file system: an EMPTY
// string must come back as an empty string, not null.
console.log('JSString.valueOf keeps an empty string');
const VALUE_OF = 'cn1_com_codename1_html5_js_core_JSString_valueOf_java_lang_String_R_com_codename1_html5_js_core_JSString';
function extractBinding(nativeName) {
  const marker = portSource.indexOf('"' + nativeName + '"');
  if (marker < 0) {
    return null;
  }
  const start = portSource.lastIndexOf('bindNative(', marker);
  let depth = 0;
  for (let i = start; i < portSource.length; i++) {
    const c = portSource[i];
    if (c === '(') depth++;
    if (c === ')' && --depth === 0) {
      return portSource.slice(start, i + 1);
    }
  }
  return null;
}
const binding = extractBinding(VALUE_OF);
check(binding != null, 'port.js binds JSString.valueOf');
if (binding != null) {
  let fn = null;
  const jvm = {
    toNativeString(v) {
      return typeof v === 'string' ? v : v.__nativeString;
    },
  };
  vm.runInNewContext(binding, { bindNative(names, f) { fn = f; }, jvm });
  check(fn('') === '', 'valueOf("") is "" (got ' + JSON.stringify(fn('')) + ')');
  check(fn({ __class: 'java_lang_String', __nativeString: 'abc' }) === 'abc', 'valueOf of a Java string is its text');
  check(fn(null) === null, 'valueOf(null) is null');
}

if (failures) {
  console.log(failures + ' failure(s)');
  process.exit(1);
}
console.log('all passed');
