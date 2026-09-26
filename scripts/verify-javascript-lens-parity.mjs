import fs from 'node:fs';
import vm from 'node:vm';

// Pixel-parity guard for the complete iOS-26 glass-tab effect. This evaluates
// the implementation that browser_bridge.js actually ships (not a copied test
// implementation) against checksums produced by JavaSEPort.applyLensBuffer()
// and IOSImplementation's material/optics routines. A one-channel/one-pixel
// drift changes the checksum.
//
// The glass material/optics pins below are shared by THREE CPU
// implementations: IOSImplementation.{glassMaterialInPlace,applyGlassOptics},
// JavaSEPort.{glassMaterialInPlace,applyGlassOptics} (the simulator's
// backdrop-filter glass) and the browser bridge functions checked here. When
// one of them changes, regenerate with a reflection probe over the SAME
// glassPattern inputs (see the GlassParityDump/LensParityDump probes in the
// PR description) and update all backends together.
const bridgePath = new URL('../vm/ByteCodeTranslator/src/javascript/browser_bridge.js', import.meta.url);
const bridge = fs.readFileSync(bridgePath, 'utf8');
const javaSource = fs.readFileSync(new URL(
    '../Ports/JavaSE/src/com/codename1/impl/javase/JavaSEPort.java', import.meta.url), 'utf8');
const metalReference = fs.readFileSync(new URL(
    '../Ports/iOSPort/nativeSources/METALView.m', import.meta.url), 'utf8');
const metalShader = fs.readFileSync(new URL(
    '../Ports/iOSPort/nativeSources/CN1MetalShaders.metal', import.meta.url), 'utf8');
const start = bridge.indexOf('  var LENS_MAG_FLAT =');
const end = bridge.indexOf('  // Replay one command stream', start);
if (start < 0 || end < 0) {
  throw new Error('Unable to locate the lens implementation in browser_bridge.js');
}
// `global` backs createGlassScratchCanvas, which the colour-matrix path uses
// for the canvas it draws its result back from.
class FakeScratchCanvas {
  constructor(width, height) {
    this.width = width;
    this.height = height;
    this.pixels = new Uint8ClampedArray(width * height * 4);
  }
  getContext() {
    const canvas = this;
    return {
      createImageData(w, h) { return { data: new Uint8ClampedArray(w * h * 4) }; },
      putImageData(image) { canvas.pixels.set(image.data); }
    };
  }
}
const sandbox = { Math, Uint8ClampedArray, global: { OffscreenCanvas: FakeScratchCanvas } };
vm.runInNewContext(bridge.substring(start, end)
    + '\nthis.applyLens = applyLensSelfRegion;'
    + '\nthis.applyMaterial = glassMaterialInPlace;'
    + '\nthis.applyOptics = applyGlassOptics;'
    + '\nthis.colorMatrixBlend = colorMatrixBlendInPlace;'
    + '\nthis.applyColorMatrix = applyColorMatrixSelfRegion;', sandbox);

// The same foreground lens is implemented four times because each backend has
// a different pixel API. Fail before the CRC probe if a tuning constant drifts.
const lensConstants = [
  'LENS_MAG_FLAT', 'LENS_TINT_HI', 'LENS_TINT_LO', 'LENS_LIFT_COEF',
  'LENS_GLARE', 'LENS_RIM', 'LENS_RIM_W', 'LENS_REFRACT',
  'LENS_EDGE_SHADOW', 'LENS_RIM_SCALE', 'LENS_GLASS_TINT_STR',
  'LENS_SAT_BOOST'
];

function constant(source, pattern, name, backend) {
  const match = source.match(new RegExp(pattern.replace('%s', name)));
  if (!match) {
    throw new Error(`Missing ${name} in ${backend} lens implementation`);
  }
  return Number(match[1]);
}

for (const name of lensConstants) {
  const values = {
    javascript: constant(bridge, `var\\s+%s\\s*=\\s*([0-9.]+)\\s*;`, name, 'JavaScript'),
    javase: constant(javaSource,
        `private\\s+static\\s+final\\s+double\\s+%s\\s*=\\s*([0-9.]+)\\s*;`, name, 'JavaSE'),
    metalReference: constant(metalReference,
        `#define\\s+%s\\s+([0-9.]+)f`, name, 'Metal CPU reference'),
    metalShader: constant(metalShader,
        `constant\\s+float\\s+%s\\s*=\\s*([0-9.]+)\\s*;`, name, 'Metal shader')
  };
  if (new Set(Object.values(values)).size !== 1) {
    throw new Error(`Lens constant drift for ${name}: ${JSON.stringify(values)}`);
  }
}

// The liquid-layer gate (glassAmt = smoothstep(1.085, 1.25, magnify)) is a
// named pair only in the JavaScript port; the other backends inline the
// literals. Pin all four so the gate cannot drift silently either.
const gateStart = constant(bridge, `var\\s+%s\\s*=\\s*([0-9.]+)\\s*;`, 'LENS_GLASS_START', 'JavaScript');
const gateFull = constant(bridge, `var\\s+%s\\s*=\\s*([0-9.]+)\\s*;`, 'LENS_GLASS_FULL', 'JavaScript');
if (gateStart !== 1.085 || gateFull !== 1.25) {
  throw new Error(`JavaScript liquid gate drift: ${gateStart}..${gateFull}, expected 1.085..1.25`);
}
for (const [backend, source, pattern] of [
  ['JavaSE', javaSource, /lensSmoothstep\(1\.085,\s*1\.25,\s*magnify\)/],
  ['Metal CPU reference', metalReference, /glassSmoothstep\(1\.085f,\s*1\.25f,\s*magnify\)/],
  ['Metal shader', metalShader, /cn1_lens_smoothstep\(1\.085,\s*1\.25,\s*magnify\)/]
]) {
  if (!pattern.test(source)) {
    throw new Error(`Liquid gate smoothstep(1.085, 1.25) not found in ${backend}`);
  }
}
console.log(`lens constant parity PASS backends=4 constants=${lensConstants.length + 2}`);

function crc32(bytes) {
  let crc = 0xffffffff;
  for (const value of bytes) {
    crc ^= value;
    for (let bit = 0; bit < 8; bit++) {
      crc = (crc >>> 1) ^ ((crc & 1) ? 0xedb88320 : 0);
    }
  }
  return (crc ^ 0xffffffff) >>> 0;
}

function rgbaToArgb(rgba) {
  const argb = new Uint8Array(rgba.length);
  for (let i = 0; i < rgba.length / 4; i++) {
    argb[i * 4] = rgba[i * 4 + 3];
    argb[i * 4 + 1] = rgba[i * 4];
    argb[i * 4 + 2] = rgba[i * 4 + 1];
    argb[i * 4 + 3] = rgba[i * 4 + 2];
  }
  return argb;
}

function glassPattern(width, height) {
  const source = new Uint8ClampedArray(width * height * 4);
  for (let y = 0; y < height; y++) {
    for (let x = 0; x < width; x++) {
      const offset = (y * width + x) * 4;
      source[offset] = (x * 17 + y * 3) & 0xff;
      source[offset + 1] = (x * 5 + y * 19) & 0xff;
      source[offset + 2] = (x * 11 + y * 7) & 0xff;
      source[offset + 3] = (x * 7 + y * 13 + 31) & 0xff;
    }
  }
  return source;
}

// magnify/aberration sampled from the shipped "ios26" preset (rest 1.08x,
// peak 1.18x, peak aberration 0.02) at representative flight envelopes.
// Expected CRCs are produced by JavaSEPort.applyLensBuffer over the same
// synthetic source (see the LensParityDump probe in the PR description).
const frames = [
  { progress: 0, width: 100, height: 40, magnify: 1.08, aberration: 0, expected: 0xda50e7bd },
  { progress: 10, width: 100, height: 40, magnify: 1.1726, aberration: 0.01852, expected: 0xfb9082d1 },
  { progress: 25, width: 100, height: 40, magnify: 1.18, aberration: 0.02, expected: 0x766a3feb },
  { progress: 50, width: 100, height: 40, magnify: 1.18, aberration: 0.02, expected: 0x766a3feb },
  { progress: 75, width: 98, height: 40, magnify: 1.13, aberration: 0.01, expected: 0x4fe082c7 },
  { progress: 90, width: 99, height: 40, magnify: 1.08, aberration: 0, expected: 0xd0b8b264 },
  { progress: 100, width: 100, height: 40, magnify: 1.08, aberration: 0, expected: 0xda50e7bd }
];

for (const frame of frames) {
  const { width, height } = frame;
  const source = new Uint8ClampedArray(width * height * 4);
  for (let y = 0; y < height; y++) {
    for (let x = 0; x < width; x++) {
      const offset = (y * width + x) * 4;
      source[offset] = (x * 17 + y * 3) & 0xff;
      source[offset + 1] = (x * 5 + y * 19) & 0xff;
      source[offset + 2] = (x * 11 + y * 7) & 0xff;
      source[offset + 3] = 0xff;
    }
  }
  let output;
  const context = {
    canvas: { width, height },
    getTransform() { return { a: 1, b: 0, c: 0, d: 1, e: 0, f: 0 }; },
    getImageData() { return { data: new Uint8ClampedArray(source) }; },
    createImageData(w, h) { return { data: new Uint8ClampedArray(w * h * 4) }; },
    putImageData(image) { output = image.data; }
  };
  sandbox.applyLens(context, 0, 0, width, height, -1,
      frame.magnify, frame.aberration, 0x0a84ff, 1);

  // JavaSE's reference buffer is packed ARGB; Canvas ImageData is RGBA.
  const actual = crc32(rgbaToArgb(output));
  if (actual !== frame.expected && process.env.CN1_UPDATE_LENS_PARITY !== '1') {
    throw new Error(`Lens parity failed at ${frame.progress}%: `
        + `expected ${frame.expected.toString(16)}, got ${actual.toString(16)}`);
  }
  const status = process.env.CN1_UPDATE_LENS_PARITY === '1' ? 'UPDATE' : 'PASS';
  console.log(`lens parity ${status} t=${frame.progress}% size=${width}x${height} crc=${actual.toString(16)}`);
}

for (const recipe of [
  { name: 'pill-light', saturation: 1.8, scale: 1, offset: 108, expected: 0x5d960bce },
  { name: 'pill-dark', saturation: 2.5, scale: 0.3, offset: 13, expected: 0x836a16cf },
  // iOS 27 dark chrome and pill carry the luminance curve. A shipped recipe and
  // a synthetic negative curve, because the term is squared and a sign slip in
  // one backend would still match a single positive pin.
  { name: 'chrome27-dark', saturation: 4.5, scale: 0.349, offset: 96.6,
    curve: 1.65, curveMid: 0.45, expected: 0xc7afb510 },
  { name: 'negative-curve', saturation: 2.7, scale: 0.398, offset: 77.7,
    curve: -0.108, curveMid: 0.65, expected: 0x3d2787db }
]) {
  const material = glassPattern(64, 40);
  sandbox.applyMaterial(material, recipe.saturation, recipe.scale, recipe.offset,
      recipe.curve || 0, recipe.curveMid || 0);
  const actual = crc32(rgbaToArgb(material));
  if (actual !== recipe.expected) {
    throw new Error(`Glass material parity failed for ${recipe.name}: `
        + `expected ${recipe.expected.toString(16)}, got ${actual.toString(16)}`);
  }
  console.log(`glass material parity PASS recipe=${recipe.name} crc=${actual.toString(16)}`);
}

const optics = sandbox.applyOptics(glassPattern(52, 32), 52, 32, 6,
    40, 20, -1, 0.4, 0.5);
const opticsCrc = crc32(rgbaToArgb(optics));
if (opticsCrc !== 0x8057dd7a) {
  throw new Error(`Glass optics parity failed: expected 8057dd7a, got ${opticsCrc.toString(16)}`);
}
console.log(`glass optics parity PASS crc=${opticsCrc.toString(16)}`);

// The iOS 27 edge outline, which darkens the RAW backdrop under the edge (the
// last argument) weighted by the horizontal component of the edge normal. A
// capsule exercises the rounded-end branch, a rounded rectangle the straight
// sides, the corners and the unweighted top and bottom; two strengths catch a
// weight applied in the wrong place.
for (const probe of [
  { name: 'capsule-0.95', corner: -1, outline: 0.95, expected: 0xa8a79b12 },
  { name: 'rounded-0.95', corner: 6, outline: 0.95, expected: 0x2f4e7f99 },
  { name: 'rounded-0.55', corner: 6, outline: 0.55, expected: 0xee2536c0 }
]) {
  const outlined = sandbox.applyOptics(glassPattern(52, 32), 52, 32, 6,
      40, 20, probe.corner, 0.4, 0.5, probe.outline, glassPattern(40, 20));
  const outlineCrc = crc32(rgbaToArgb(outlined));
  if (outlineCrc !== probe.expected) {
    throw new Error(`Glass outline parity failed for ${probe.name}: `
        + `expected ${probe.expected.toString(16)}, got ${outlineCrc.toString(16)}`);
  }
  console.log(`glass outline parity PASS ${probe.name} crc=${outlineCrc.toString(16)}`);
}

// Graphics.colorMatrixRegion. The expected CRCs come from the core reference,
// com.codename1.ui.plaf.ColorMatrixBlend.apply, over the same inputs (a probe
// that fills packed ARGB with colorMatrixPattern, applies the blend and CRCs the
// ARGB bytes). colorMatrixBlendInPlace frounds every float operation in Java's
// order, so these are exact, not approximate. The matrix has negative and >1
// terms so both clamps are exercised; the mask is a non-divisor size so the
// nearest-sample stretch is too.
function colorMatrixPattern(width, height, originX, originY) {
  const pixels = new Uint8ClampedArray(width * height * 4);
  for (let y = 0; y < height; y++) {
    for (let x = 0; x < width; x++) {
      const px = x + originX, py = y + originY, offset = (y * width + x) * 4;
      pixels[offset] = (px * 17 + py * 3) & 0xff;
      pixels[offset + 1] = (px * 5 + py * 19) & 0xff;
      pixels[offset + 2] = (px * 11 + py * 7) & 0xff;
      pixels[offset + 3] = (px * 7 + py * 13 + 31) & 0xff;
    }
  }
  return pixels;
}

function colorMatrixMask(width, height) {
  const alpha = new Uint8ClampedArray(width * height);
  for (let y = 0; y < height; y++) {
    for (let x = 0; x < width; x++) {
      alpha[y * width + x] = (x * 37 + y * 53) & 0xff;
    }
  }
  return { alpha, w: width, h: height };
}

const vibrantMatrix = [0.4, -0.9, 1.7, 0.12, -0.35, 1.25, 0.3, -0.05, 1.1, 0.2, -0.6, 0.33]
    .map(Math.fround);
const greyMatrix = [0.2126, 0.7152, 0.0722, 0, 0.2126, 0.7152, 0.0722, 0,
  0.2126, 0.7152, 0.0722, 0].map(Math.fround);
for (const probe of [
  { name: 'rect-vibrant', w: 40, h: 24, matrix: vibrantMatrix, mask: null, corner: 0,
    amount: 1, expected: 0x9bce8eee },
  { name: 'capsule-grey', w: 52, h: 30, matrix: greyMatrix, mask: null, corner: -1,
    amount: 0.8, expected: 0x71cccd47 },
  { name: 'rounded-masked', w: 50, h: 28, matrix: vibrantMatrix, mask: colorMatrixMask(13, 9),
    corner: 7, amount: 0.65, expected: 0x6a55dd05 }
]) {
  const pixels = colorMatrixPattern(probe.w, probe.h, 0, 0);
  sandbox.colorMatrixBlend(pixels, probe.w, probe.h, probe.matrix,
      probe.mask ? probe.mask.alpha : null, probe.mask ? probe.mask.w : 0,
      probe.mask ? probe.mask.h : 0, probe.corner, probe.amount);
  const actual = crc32(rgbaToArgb(pixels));
  if (actual !== probe.expected) {
    throw new Error(`Colour matrix parity failed for ${probe.name}: `
        + `expected ${probe.expected.toString(16)}, got ${actual.toString(16)}`);
  }
  console.log(`colour matrix parity PASS ${probe.name} crc=${actual.toString(16)}`);
}

// The replayed op end to end, on a region hanging off the top-left of the
// canvas: only the visible part is read and written, but the capsule and the
// mask stay anchored to the FULL region, which is what JavaSEPort does. The
// Java side of this CRC applies the blend to the whole 44x26 region and then
// crops the visible 38x20.
{
  const canvasWidth = 60, canvasHeight = 20;
  const calls = [];
  let drawn = null;
  const context = {
    canvas: { width: canvasWidth, height: canvasHeight },
    getTransform() { return { a: 1, b: 0, c: 0, d: 1, e: 0, f: 0 }; },
    getImageData(x, y, w, h) {
      calls.push(['getImageData', x, y, w, h]);
      return { data: colorMatrixPattern(w, h, x, y) };
    },
    putImageData() { throw new Error('putImageData ignores the clip; the op must not use it'); },
    save() { calls.push(['save']); },
    restore() { calls.push(['restore']); },
    setTransform(...args) { calls.push(['setTransform', ...args]); },
    clearRect(...args) { calls.push(['clearRect', ...args]); },
    drawImage(source, x, y) { calls.push(['drawImage', x, y]); drawn = source; }
  };
  sandbox.applyColorMatrix(context, -6, -3, 44, 26, vibrantMatrix, colorMatrixMask(11, 7), -1, 0.9);
  const expectedCalls = JSON.stringify([
    ['getImageData', 0, 0, 38, 20], ['save'], ['setTransform', 1, 0, 0, 1, 0, 0],
    ['clearRect', 0, 0, 38, 20], ['drawImage', 0, 0], ['restore']
  ]);
  if (JSON.stringify(calls) !== expectedCalls) {
    throw new Error(`Colour matrix replay call sequence: ${JSON.stringify(calls)}`);
  }
  if (!drawn || drawn.width !== 38 || drawn.height !== 20) {
    throw new Error('Colour matrix replay drew back the wrong size');
  }
  const actual = crc32(rgbaToArgb(drawn.pixels));
  if (actual !== 0x48ee53ca) {
    throw new Error(`Colour matrix off-canvas parity failed: expected 48ee53ca, got ${actual.toString(16)}`);
  }
  console.log(`colour matrix replay PASS offcanvas crc=${actual.toString(16)}`);
}
