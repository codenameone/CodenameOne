import test from 'node:test';
import assert from 'node:assert/strict';
import {scenePixels, sceneChecks, changedPixels, sampleScene} from './demo-pixels.mjs';

const region = {x: 120, y: 40, width: 160, height: 240};
function fixture({blank = false, small = false, outside = false, offset = 0, kind = 'balls'} = {}) {
  const width = 300, height = 300;
  const data = new Uint8ClampedArray(width * height * 4).fill(255);
  function rect(x, y, w, h, color) {
    for (let yy = y; yy < y + h; yy++) for (let xx = x; xx < x + w; xx++) {
      data.set([color >> 16, (color >> 8) & 255, color & 255, 255], (yy * width + xx) * 4);
    }
  }
  if (!blank) {
    rect(region.x, region.y, small ? 80 : region.width, small ? 100 : region.height, 0x10182a);
    const x = (outside ? 5 : region.x + 35) + offset, y = region.y + 100;
    if (kind === 'balls') {
      [0x5fd0d6, 0xf2b134, 0xe8615f, 0x9b8cf2].forEach((c, i) => rect(x + i * 20, y, 16, 16, c));
    } else rect(x + 25, y, 30, 30, 0x3399ff);
  }
  return scenePixels({data, width, height}, region, kind);
}
for (const kind of ['balls', 'cube']) {
  test(kind + ': accepts a visible, moving scene that fills its preview', () => {
    assert.deepEqual(sceneChecks([fixture({kind}), fixture({kind, offset: 8})], region, kind),
      {'visible content': true, ...(kind === 'cube' ? {'cube proportions': true} : {}), 'fills preview': true, animation: true});
  });
  test(kind + ': rejects blank output even when initialization succeeds', () => {
    const blank = fixture({kind, blank: true});
    assert.equal(sceneChecks([blank, blank], region, kind)['visible content'], false);
  });
  test(kind + ': rejects fixed-size upper-left painting and frozen animation', () => {
    const small = fixture({kind, small: true});
    const checks = sceneChecks([small, small], region, kind);
    assert.equal(checks['fills preview'], false);
    assert.equal(checks.animation, false);
  });
  test(kind + ': ignores moving pixels outside the preview', () => {
    const frames = [fixture({kind, outside: true}), fixture({kind, outside: true, offset: 8})];
    assert.equal(sceneChecks(frames, region, kind)['visible content'], false);
    assert.equal(changedPixels(...frames), 0);
  });
}
test('camera: distinguishes changing green test-pattern shades from a frozen frame', () => {
  const frame = g => scenePixels({data: [0, g, 0, 255], width: 1, height: 1},
    {x: 0, y: 0, width: 1, height: 1}, 'camera');
  assert.equal(changedPixels(frame(128), frame(240)), 1);
  assert.equal(changedPixels(frame(128), frame(128)), 0);
});

test('a disappearing cube is not successful animation', () => {
  const frames = [fixture({kind: 'cube'}), fixture({kind: 'cube', blank: true})];
  assert.equal(sceneChecks(frames, region, 'cube').animation, false);
});

test('sampling waits for delayed visible animation without changing pixel requirements', async () => {
  let time = 0;
  const frames = await sampleScene(async i => fixture({kind: 'cube', blank: i < 6,
    offset: i % 2 ? 8 : 0}), async ms => { time += ms; }, region, 'cube', () => time);
  assert.ok(time > 1000, 'the old three-sample window would have missed every frame');
  assert.ok(Object.values(sceneChecks(frames, region, 'cube')).every(Boolean));
});

test('slow screenshots get an animation window after the first visible frame', async () => {
  let time = 0;
  const frames = await sampleScene(async i => {
    time += 3000;
    return fixture({kind: 'cube', blank: i === 0, offset: i % 2 ? 8 : 0});
  }, async ms => { time += ms; }, region, 'cube', () => time);
  assert.equal(frames.length, 3);
  assert.ok(Object.values(sceneChecks(frames, region, 'cube')).every(Boolean));
  assert.ok(time < 16000);
});

for (const condition of ['blank', 'frozen', 'wrong-size']) {
  test('sampling still rejects ' + condition + ' scenes at its deadline', async () => {
    let time = 0;
    const frames = await sampleScene(async i => fixture({kind: 'cube', blank: condition === 'blank',
      small: condition === 'wrong-size', offset: condition === 'frozen' ? 0 : i % 2 ? 8 : 0}),
    async ms => { time += ms; }, region, 'cube', () => time);
    assert.ok(time >= 8000 && time < 8220);
    assert.ok(Object.values(sceneChecks(frames, region, 'cube')).some(ok => !ok));
  });
}
