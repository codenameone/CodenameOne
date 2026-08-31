#!/usr/bin/env node
//
// Regression gate for the surface command replay in browser_bridge.js.
//
// A mutable image in the JavaScript port is a host-side SURFACE: the worker
// records draw ops into a command buffer and the host replays the whole batch
// in one go. replaySurfaceCommands therefore has a property nothing else in
// the port has -- one bad op can cost every op BEHIND it, because an exception
// thrown mid-loop unwinds the rest of the batch.
//
// That is not hypothetical. ctx.drawImage() throws InvalidStateError for an
// <img> whose request is broken and for a canvas of zero width or height, and
// neither case was guarded. graphics-draw-image-rect failed intermittently in
// CI for exactly that reason: its two mutable-image cells came out truncated
// at the first EncodedImage draw -- every later draw in those cells missing,
// including ones that use no image at all -- while the directly-painted cells
// were complete, because each mutable image is its own surface with its own
// batch.
//
// This test drives the replay directly with a recording context, so it runs in
// milliseconds and needs no browser. It asserts the two properties that make
// the batch safe:
//
//   1. An op whose image source cannot be drawn is SKIPPED, and every op after
//      it still runs.
//   2. The argument cursors advance for a skipped op exactly as they do for a
//      drawn one, so the ops behind it get their own arguments rather than the
//      skipped op's.
//
// Property 2 is the one a naive fix breaks: wrapping the draw in try/catch but
// consuming the coordinates inside the try leaves the cursor short, and every
// later op in the batch silently draws with the wrong numbers.
//
// Usage: node scripts/test-javascript-surface-replay.mjs

import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { dirname, join } from 'node:path';
import vm from 'node:vm';

const here = dirname(fileURLToPath(import.meta.url));
const bridgePath = join(here, '..', 'vm', 'ByteCodeTranslator', 'src', 'javascript', 'browser_bridge.js');
const source = readFileSync(bridgePath, 'utf8');

/** Extract a top-level `function name(...) { ... }` by brace matching. */
function extractFunction(name) {
  const start = source.indexOf('function ' + name + '(');
  if (start < 0) {
    throw new Error('could not find function ' + name + ' in browser_bridge.js');
  }
  let depth = 0;
  let seenBody = false;
  for (let i = start; i < source.length; i++) {
    const c = source[i];
    if (c === '{') {
      depth++;
      seenBody = true;
    } else if (c === '}') {
      depth--;
      if (seenBody && depth === 0) {
        return source.slice(start, i + 1);
      }
    }
  }
  throw new Error('unbalanced braces extracting ' + name);
}

/** Extract the `var SURF = { ... };` opcode table. */
function extractSurfTable() {
  const start = source.indexOf('var SURF = {');
  if (start < 0) {
    throw new Error('could not find the SURF opcode table');
  }
  const end = source.indexOf('};', start);
  return source.slice(start, end + 2);
}

const calls = [];
const ctx = {
  save() { calls.push(['save']); },
  restore() { calls.push(['restore']); },
  fillRect(...a) { calls.push(['fillRect', ...a]); },
  drawImage(src, ...a) {
    if (src && src.__throwOnDraw) {
      // What a browser does with a broken <img> or a zero-sized canvas.
      const err = new Error('InvalidStateError');
      err.name = 'InvalidStateError';
      throw err;
    }
    calls.push(['drawImage', src && src.id, ...a]);
  },
};

const sandbox = {
  console,
  // Stubs for everything replaySurfaceCommands can reach that this test does
  // not drive. Any of them being called is itself a failure of the test's
  // assumptions, so they throw rather than silently passing.
  surfaceImageSource: (marker) => marker,
  surfaceTextElement: () => null,
  resolveHostRef: (m) => m,
  applyBlurSelfRegion() { throw new Error('unexpected blur op'); },
  applyLensSelfRegion() { throw new Error('unexpected lens op'); },
  surfaceTable: {},
  global: {},
};
vm.createContext(sandbox);
vm.runInContext(
  extractSurfTable() + '\n'
  + extractFunction('drawableImageSource') + '\n'
  + extractFunction('safeDrawImage') + '\n'
  + 'var surfaceDrawImageDropped = 0;\n'
  + extractFunction('replaySurfaceCommands') + '\n',
  sandbox);

const SURF = sandbox.SURF;
const failures = [];

function check(label, condition, detail) {
  if (condition) {
    console.log('  ok   ' + label);
  } else {
    console.log('  FAIL ' + label + (detail ? ' -- ' + detail : ''));
    failures.push(label);
  }
}

function run(scenario, badSource) {
  calls.length = 0;
  sandbox.surfaceDrawImageDropped = 0;
  // A batch shaped like the failing cell: draw an image, then a second image
  // whose source is unusable, then keep drawing. The trailing fillRect is the
  // op the truncation used to swallow.
  const good = { id: 'good', width: 10, height: 10 };
  const ops = [SURF.DRAW_IMAGE_XY, SURF.DRAW_IMAGE_XYWH, SURF.FILL_RECT];
  const nums = [1, 2, /* bad image xywh */ 3, 4, 5, 6, /* fillRect */ 7, 8, 9, 10];
  const objs = [good, badSource];
  sandbox.replaySurfaceCommands(ctx, ops, ops.length, nums, objs);

  const drew = calls.filter((c) => c[0] === 'drawImage');
  const filled = calls.filter((c) => c[0] === 'fillRect');
  check(scenario + ': the good image still draws',
    drew.length === 1 && drew[0][1] === 'good', JSON.stringify(drew));
  check(scenario + ': ops after the bad image still run',
    filled.length === 1, 'fillRect calls: ' + filled.length);
  check(scenario + ': the later op gets ITS OWN arguments (cursor stayed aligned)',
    filled.length === 1 && filled[0].slice(1).join(',') === '7,8,9,10',
    filled.length ? filled[0].slice(1).join(',') : 'never called');
  check(scenario + ': the drop is counted',
    sandbox.surfaceDrawImageDropped === 1, 'count=' + sandbox.surfaceDrawImageDropped);
}

console.log('surface replay: a bad image op must not truncate the batch');
// An <img> whose decode has not completed: drawImage would draw nothing.
run('decoding image', { id: 'bad', naturalWidth: 0, naturalHeight: 0, complete: false });
// An <img> in the broken state: drawImage throws InvalidStateError.
run('broken image', { id: 'bad', naturalWidth: 0, naturalHeight: 0, complete: true, __throwOnDraw: true });
// A backing canvas with no pixels: drawImage throws InvalidStateError.
run('zero-sized canvas', { id: 'bad', width: 0, height: 0, __throwOnDraw: true });
// A source that looks fine and throws anyway -- the backstop.
run('source that throws late', { id: 'bad', width: 10, height: 10, __throwOnDraw: true });

// A fully drawable source must be unaffected by the guard.
calls.length = 0;
sandbox.surfaceDrawImageDropped = 0;
sandbox.replaySurfaceCommands(ctx,
  [SURF.DRAW_IMAGE_XY], 1, [11, 12], [{ id: 'plain', width: 4, height: 4 }]);
check('a drawable source still draws, with its arguments',
  calls.length === 1 && calls[0].join(',') === 'drawImage,plain,11,12',
  JSON.stringify(calls));
check('a drawable source counts no drop', sandbox.surfaceDrawImageDropped === 0);

if (failures.length) {
  console.error('\nFAILED: ' + failures.length + ' assertion(s)');
  process.exit(1);
}
console.log('\nAll surface replay assertions passed.');
