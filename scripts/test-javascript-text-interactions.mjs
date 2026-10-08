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
text.tagName = 'TEXTAREA';
for (const type of ['mousemove', 'pointermove']) {
  callback(Object.assign(event(type, text), {buttons: 0}));
  assert.equal(messages.pop().args[0].type, type, type + ' reaches framework hover without a pressed button');
  for (const state of [{buttons: 1}, {buttons: 0, __cn1NativeTextGesture: true}]) {
    callback(Object.assign(event(type, text), state));
    assert.equal(messages.length, 0, type + ' stays browser-owned during a selection gesture');
  }
}
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
    ['k', {ctrlKey: true, altKey: true, getModifierState: () => false}, false],
    ['c', {ctrlKey: true, altKey: true, getModifierState: () => false}, false],
    ['@', {ctrlKey: true, altKey: true, getModifierState: key => key === 'AltGraph'}, true],
    ['k', {ctrlKey: true, altKey: true}, false],
    ['ArrowLeft', { ctrlKey: true }, true], ['a', {}, true], ['Dead', { isComposing: true }, true], ['Dead', {keyCode: 222}, true]]) {
  callback(Object.assign(event('keydown', text), { key }, modifiers));
  assert.equal(messages.length, owns ? 0 : 1, key + ' keeps the appropriate keyboard owner');
  messages.length = 0;
}
for (const state of ['readOnly', 'disabled']) {
  text[state] = true;
  for (const [key, modifiers, owns] of [['k', {}, false], ['Enter', {}, false], ['Backspace', {}, false],
      ['Dead', {keyCode: 222}, false], ['Delete', {}, false], ['ArrowLeft', {}, true], ['Tab', {}, true], ['c', {ctrlKey: true}, true],
      ['a', {metaKey: true}, true], ['v', {ctrlKey: true}, false], ['z', {metaKey: true}, false],
      ['@', {ctrlKey: true, altKey: true, getModifierState: () => true}, false]]) {
    for (const type of ['keydown', 'keyup', 'keypress']) {
      callback(Object.assign(event(type, text), {key}, modifiers));
      assert.equal(messages.length, owns ? 0 : 1, state + ' ' + key + ' retains the appropriate keyboard owner');
      messages.length = 0;
    }
  }
  text[state] = false;
}
for (const readOnly of [false, true]) {
  text.readOnly = readOnly;
  for (const [modifiers, owns] of [[{ctrlKey: true}, true], [{shiftKey: true}, !readOnly],
      [{}, false], [{ctrlKey: true, altKey: true}, false], [{shiftKey: true, altKey: true}, false],
      [{shiftKey: true, metaKey: true}, false]]) {
    for (const keyInfo of [{key: 'Insert'}, {keyCode: 45}]) {
      for (const type of ['keydown', 'keyup', 'keypress']) {
        const e = Object.assign(event(type, text), keyInfo, modifiers);
        callback(e);
        assert.equal(messages.length, owns ? 0 : 1, 'Insert clipboard shortcut has one keyboard owner');
        assert.equal(e.defaultPrevented, false, 'native clipboard default action is preserved');
        messages.length = 0;
      }
    }
  }
}
text.readOnly = false;
for (const type of ['keydown', 'keyup', 'keypress']) {
  const dead = Object.assign(event(type, text), {key: 'Dead', keyCode: 222, isComposing: false});
  callback(dead);
  assert.equal(messages.length, 0, 'non-composing dead keys remain native-owned');
  assert.equal(dead.defaultPrevented, false, 'dead-key composition keeps its browser default');
}
const listeners = {}, relayed = [];
const doc = { addEventListener(type, listener) { listeners[type] = listener; },
  getElementById() { return canvas; }, activeElement: null };
context.global.document = doc;
context.global.Touch = function(data) { Object.assign(this, data); };
context.global.WheelEvent = function(type, data) { Object.assign(this, data, { type, target: canvas }); };
context.global.TouchEvent = function(type, data) { Object.assign(this, data, { type, target: canvas }); };
canvas.dispatchEvent = e => { relayed.push(e.type); listeners[e.type](e); };
vm.runInContext(extract('reconcileNativeTextOrder') + '\n' + extract('installNativeTextInteractions'), context);
context.installNativeTextInteractions();
text.tagName = 'TEXTAREA';
const tab = Object.assign(event('keydown', text), { key: 'Tab' });
listeners.keydown(tab);
assert.equal(tab.defaultPrevented, true, 'cancel native Tab synchronously so CN1 chooses the next field');
const singleLine = node({'data-cn1-native-selection': 'true', 'data-cn1-single-line': 'true'});
singleLine.tagName = 'INPUT';
let enterBlurCount = 0;
singleLine.blur = () => enterBlurCount++;
for (const readOnly of [true, false]) {
  singleLine.readOnly = readOnly;
  const enter = Object.assign(event('keydown', singleLine), {key: 'Enter'});
  listeners.keydown(enter);
  assert.equal(enter.defaultPrevented, !readOnly, 'only an editable single-line input consumes Enter');
  assert.equal(enterBlurCount, readOnly ? 0 : 1, 'readonly Enter keeps browser focus for the app handler');
}

function touch(type, y, touches = true, x = 20) {
  const point = { identifier: 1, clientX: x, clientY: y };
  const e = Object.assign(event(type, text), { touches: touches ? [point] : [], changedTouches: [point] });
  listeners[type](e);
  return e;
}
const mobileLine = node({'data-cn1-native-selection': 'true', 'data-cn1-single-line': 'true', 'data-cn1-enter-next': 'true'});
mobileLine.tagName = 'INPUT';
const nextEvents = [];
context.global.Event = function(type) { this.type = type; };
mobileLine.dispatchEvent = e => nextEvents.push(e.type);
mobileLine.blur = () => assert.fail('mobile completion must wait for worker traversal');
listeners.keydown(Object.assign(event('keydown', mobileLine), {key: 'Enter'}));
assert.deepEqual(nextEvents, ['cn1-next']);
listeners.keydown(Object.assign(event('keydown', mobileLine), {key: 'Enter', isComposing: true}));
assert.deepEqual(nextEvents, ['cn1-next'], 'IME confirmation cannot advance focus');

const otherRun = node({'data-cn1-native-selection': 'true'});
text.tagName = 'SPAN';
for (const focusEnd of [false, true]) {
  context.global.getSelection = () => ({isCollapsed: false,
    anchorNode: {parentNode: focusEnd ? otherRun : text}, focusNode: {parentNode: focusEnd ? text : otherRun}});
  touch('touchstart', 100);
  assert.equal(touch('touchmove', 70).defaultPrevented, false);
  touch('touchend', 70, false);
  assert.deepEqual(relayed, [], 'either endpoint of a cross-run selection keeps its touch handle');
}
context.global.getSelection = () => null;
text.tagName = 'TEXTAREA';
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

// A second finger transfers the entire gesture to the canvas, including when
// it lands outside the native text. Canvas relays must not recurse or duplicate
// the original event's worker delivery.
for (const tag of ['SPAN', 'TEXTAREA']) {
  for (const secondTarget of [text, canvas]) {
    text.tagName = tag;
    relayed.length = 0;
    const first = {identifier: 1, target: text, clientX: 20, clientY: 100};
    const second = {identifier: 2, target: secondTarget, clientX: 60, clientY: 100};
    function send(type, target, points, changed) {
      const e = Object.assign(event(type, target), {touches: points, changedTouches: changed});
      listeners[type](e);
      if (points.length > 1 || type !== 'touchstart') {
        assert.equal(e.defaultPrevented, true, 'forwarded pinch suppresses native default');
        callback(e);
        assert.equal(messages.length, 0, 'original pinch event is not also sent to worker');
      }
    }
    send('touchstart', text, [first], [first]);
    send('touchstart', secondTarget, [first, second], [second]);
    send('touchmove', secondTarget, [first, second], [second]);
    send('touchend', secondTarget, [first], [second]);
    send('touchmove', text, [first], [first]);
    send('touchcancel', text, [], [first]);
    assert.deepEqual(relayed, ['touchstart', 'touchmove', 'touchend', 'touchmove', 'touchcancel']);
  }
}
relayed.length = 0;
touch('touchstart', 100);
touch('touchend', 100, false);
assert.deepEqual(relayed, [], 'a later single touch returns to native selection');
console.log('PASS native text multi-touch relays once across mixed targets and releases ownership');

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

// A browser selection owns its entire gesture even after leaving the glyphs.
for (const prefix of ['mouse', 'pointer']) {
  for (const [suffix, target] of [['down', text], ['move', canvas], ['up', canvas]]) {
    const e = Object.assign(event(prefix + suffix, target), {pointerId: 7, pointerType: 'mouse'});
    listeners[e.type](e); callback(e);
    assert.equal(messages.length, 0, e.type + ' stays native across the canvas boundary');
  }
  const fresh = Object.assign(event(prefix + 'down', canvas), {pointerId: 7, pointerType: 'mouse'});
  listeners[fresh.type](fresh); callback(fresh);
  assert.equal(messages.pop().args[0].type, fresh.type, 'the next canvas gesture starts normally');
}
console.log('PASS native selection owns move and release beyond the text hit region');

const down = event('mousedown', text), up = event('mouseup', canvas);
listeners.mousedown(down); callback(down); listeners.mouseup(up); callback(up);
const nativeClick = Object.assign(event('click', canvas), {detail: 1});
listeners.click(nativeClick); callback(nativeClick);
assert.equal(messages.length, 0, 'the terminating mouse click stays native');
const semanticClick = Object.assign(event('click', canvas), {detail: 0});
listeners.click(semanticClick); callback(semanticClick);
assert.equal(messages.pop().args[0].type, 'click', 'keyboard and semantic activation are not mouse gesture continuations');
