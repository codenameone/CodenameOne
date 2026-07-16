import fs from 'node:fs';
import vm from 'node:vm';

// Pixel-parity guard for the complete iOS-26 glass-tab effect. This evaluates
// the implementation that browser_bridge.js actually ships (not a copied test
// implementation) against checksums produced by JavaSEPort.applyLensBuffer()
// and IOSImplementation's material/optics routines. A one-channel/one-pixel
// drift changes the checksum.
const bridgePath = new URL('../vm/ByteCodeTranslator/src/javascript/browser_bridge.js', import.meta.url);
const bridge = fs.readFileSync(bridgePath, 'utf8');
const start = bridge.indexOf('  var LENS_MAG_FLAT =');
const end = bridge.indexOf('  // Replay one command stream', start);
if (start < 0 || end < 0) {
  throw new Error('Unable to locate the lens implementation in browser_bridge.js');
}
const sandbox = { Math, Uint8ClampedArray };
vm.runInNewContext(bridge.substring(start, end)
    + '\nthis.applyLens = applyLensSelfRegion;'
    + '\nthis.applyMaterial = glassMaterialInPlace;'
    + '\nthis.applyOptics = applyGlassOptics;', sandbox);

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

const frames = [
  { progress: 0, width: 80, height: 40, magnify: 1.08, aberration: 0, expected: 0x9d4a06ea },
  { progress: 10, width: 105, height: 54, magnify: 1.1726, aberration: 0.0185, expected: 0x66a714e8 },
  { progress: 25, width: 105, height: 56, magnify: 1.18, aberration: 0.02, expected: 0x25e7edb0 },
  { progress: 50, width: 105, height: 56, magnify: 1.18, aberration: 0.02, expected: 0x25e7edb0 },
  { progress: 75, width: 90, height: 55, magnify: 1.13, aberration: 0.01, expected: 0x471ccf1d },
  { progress: 90, width: 80, height: 42, magnify: 1.08, aberration: 0, expected: 0x0f8060ba },
  { progress: 100, width: 80, height: 40, magnify: 1.08, aberration: 0, expected: 0x9d4a06ea }
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
  if (actual !== frame.expected) {
    throw new Error(`Lens parity failed at ${frame.progress}%: `
        + `expected ${frame.expected.toString(16)}, got ${actual.toString(16)}`);
  }
  console.log(`lens parity PASS t=${frame.progress}% size=${width}x${height} crc=${actual.toString(16)}`);
}

for (const recipe of [
  { name: 'pill-light', saturation: 1.8, scale: 1, offset: 108, expected: 0x5d960bce },
  { name: 'pill-dark', saturation: 2.5, scale: 0.3, offset: 13, expected: 0x836a16cf }
]) {
  const material = glassPattern(64, 40);
  sandbox.applyMaterial(material, recipe.saturation, recipe.scale, recipe.offset);
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
