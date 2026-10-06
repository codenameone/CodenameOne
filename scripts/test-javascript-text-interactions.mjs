// Host-side dispatch regression: cancellation must occur before the worker hop.
import assert from 'node:assert/strict';
import fs from 'node:fs';
import vm from 'node:vm';
const source = fs.readFileSync(new URL('../vm/ByteCodeTranslator/src/javascript/browser_bridge.js', import.meta.url), 'utf8');
function extract(name) {
  const start = source.indexOf('  function ' + name + '(');
  assert.ok(start >= 0);
  const end = source.indexOf('\n  function ', start + 1);
  return source.slice(start, end < 0 ? source.length : end);
}
const messages = [];
const context = {
  workerCallbackProxies: {},
  global: { __parparWorker: { postMessage: message => messages.push(message) } },
  serializeEventForWorker: event => ({ type: event.type, defaultPrevented: event.defaultPrevented }),
  diag() {}
};
vm.createContext(context);
vm.runInContext(extract('nativeSelectionElement') + '\n' + extract('nativeTextOwnsKey') + '\n' + extract('makeWorkerCallback'), context);
const callback = context.makeWorkerCallback(1);
function node(attributes, parentNode = null) {
  return { parentNode, getAttribute: name => attributes[name] || null, id: attributes.id };
}
function event(type, target, currentTarget = {}) {
  return { type, target, currentTarget, defaultPrevented: false,
    preventDefault() { this.defaultPrevented = true; }, stopPropagation() {} };
}
const canvas = node({ id: 'codenameone-canvas', 'data-cn1-text-selection': 'true' });
callback(event('contextmenu', canvas));
assert.equal(messages.pop().args[0].defaultPrevented, true, 'cancel canvas menu before posting to worker');
const plainCanvas = node({ id: 'codenameone-canvas' });
callback(event('contextmenu', plainCanvas));
assert.equal(messages.pop().args[0].defaultPrevented, false, 'no selection: preserve the browser menu');
const text = node({ 'data-cn1-native-selection': 'true' });
for (const type of ['pointerdown', 'mousedown', 'touchstart', 'keydown', 'keyup', 'keypress', 'contextmenu', 'copy', 'cut', 'paste']) {
  const e = event(type, text);
  callback(e);
  assert.equal(e.defaultPrevented, false, type + ' remains browser-native');
  assert.equal(messages.length, 0, type + ' does not also start a CN1 action');
}
for (const type of ['input', 'focus', 'blur', 'keydown']) {
  callback(event(type, text, text));
  assert.equal(messages.pop().args[0].type, type, 'editor lifecycle still reaches the worker');
}
context.global.getSelection = () => ({ isCollapsed: false, anchorNode: { parentNode: text } });
callback(event('copy', {}));
assert.equal(messages.length, 0, 'a DOM selection cannot be overwritten by stale CN1 clipboard text');
callback(event('pointerdown', canvas));
assert.equal(messages.pop().args[0].type, 'pointerdown', 'canvas controls retain their pointer path');
for (const key of ['Escape', 'F2']) {
  for (const type of ['keydown', 'keyup']) {
    callback(Object.assign(event(type, text), { key }));
    assert.equal(messages.pop().args[0].type, type, key + ' reaches form key handlers');
  }
}
for (const [key, modifiers, owns] of [['s', { ctrlKey: true }, false], ['c', { metaKey: true }, true],
    ['ArrowLeft', { ctrlKey: true }, true], ['a', {}, true], ['Dead', { isComposing: true }, true]]) {
  callback(Object.assign(event('keydown', text), { key }, modifiers));
  assert.equal(messages.length, owns ? 0 : 1, key + ' keeps the appropriate keyboard owner');
  messages.length = 0;
}
const listeners = {}, relayed = [];
const doc = { addEventListener(type, listener) { listeners[type] = listener; },
  getElementById() { return canvas; }, activeElement: null };
context.global.document = doc;
context.global.Touch = function(data) { Object.assign(this, data); };
context.global.WheelEvent = function(type, data) { Object.assign(this, data, { type, target: canvas }); };
context.global.TouchEvent = function(type, data) { Object.assign(this, data, { type, target: canvas }); };
canvas.dispatchEvent = e => { relayed.push(e.type); listeners[e.type](e); };
vm.runInContext(extract('installNativeTextInteractions'), context);
context.installNativeTextInteractions();
text.tagName = 'TEXTAREA';
const tab = Object.assign(event('keydown', text), { key: 'Tab' });
listeners.keydown(tab);
assert.equal(tab.defaultPrevented, true, 'cancel native Tab synchronously so CN1 chooses the next field');
function touch(type, y, touches = true, x = 20) {
  const point = { identifier: 1, clientX: x, clientY: y };
  const e = Object.assign(event(type, text), { touches: touches ? [point] : [], changedTouches: [point] });
  listeners[type](e);
  return e;
}
touch('touchstart', 100);
assert.equal(touch('touchmove', 80).defaultPrevented, true);
touch('touchend', 80, false);
assert.deepEqual(relayed, ['touchstart', 'touchmove', 'touchend'], 'a swipe relays one complete canvas gesture');
relayed.length = 0;
touch('touchstart', 100);
assert.equal(touch('touchmove', 100, true, 70).defaultPrevented, true);
touch('touchend', 100, false, 70);
assert.deepEqual(relayed, ['touchstart', 'touchmove', 'touchend'], 'horizontal swipes also reach the canvas');
relayed.length = 0;
for (const modifier of ['ctrlKey', 'metaKey']) {
  const zoom = Object.assign(event('wheel', text), { [modifier]: true });
  listeners.wheel(zoom);
  assert.equal(zoom.defaultPrevented, false, 'modified wheel keeps browser zoom');
  assert.deepEqual(relayed, []);
}
listeners.wheel(Object.assign(event('wheel', text), { deltaY: 100 }));
assert.deepEqual(relayed, ['wheel'], 'ordinary scrolling still reaches CN1');
relayed.length = 0;
doc.activeElement = text;
text.selectionStart = 0;
text.selectionEnd = 4;
touch('touchstart', 100);
touch('touchmove', 80);
touch('touchend', 80, false);
assert.deepEqual(relayed, [], 'selection handles stay native');
console.log('PASS native text event ownership and synchronous context-menu cancellation');

// Exercise the real JSO bridge, including focus suppression before the worker's
// semantic snapshot can overwrite a newly focused native editor.
let bridge;
Object.assign(context, {
  hostBridge: { register(name, handler) { bridge = handler; } },
  resolveHostRef: ref => ref,
  mapHostArgs: args => args,
  hostResult: value => value,
  noteDrawTarget() {},
  isCanvasLike: () => false
});
const bridgeStart = source.indexOf("  hostBridge.register('__cn1_jso_bridge__',");
const bridgeEnd = source.indexOf('\n  });', bridgeStart) + '\n  });'.length;
vm.runInContext(source.slice(bridgeStart, bridgeEnd), context);
let focusCalls = 0;
const semanticTarget = { closest: selector => selector === '#cn1-accessibility-tree', focus() { focusCalls++; } };
for (const [tagName, readOnly, disabled, shouldFocus] of [
  ['INPUT', false, false, false], ['TEXTAREA', false, false, false],
  ['INPUT', true, false, true], ['TEXTAREA', true, false, true],
  ['TEXTAREA', false, true, true], ['SPAN', false, false, true]
]) {
  Object.assign(text, { tagName, readOnly, disabled });
  doc.activeElement = text;
  focusCalls = 0;
  bridge({ kind: 'method', member: 'focus', receiver: semanticTarget });
  assert.equal(focusCalls, shouldFocus ? 1 : 0, `${tagName} readonly=${readOnly} disabled=${disabled} focus handoff`);
}
console.log('PASS semantic focus protection only applies to editable native controls');
