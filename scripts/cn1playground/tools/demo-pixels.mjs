// Screenshot assertions deliberately ignore text, editor pixels and antialiasing.
// Keep these independent of rendering commands: a draw call is not proof of a visible frame.
const ballColors = [0x5fd0d6, 0xf2b134, 0xe8615f, 0x9b8cf2, 0x6fcf6b, 0xf28cc8, 0x7aa7ff];
export function scenePixels({data, width, height}, region, kind) {
  let dark = 0, foreground = 0, total = 0, sumX = 0, sumY = 0;
  const mask = [], colors = new Set();
  let minX = Infinity, minY = Infinity, maxX = -1, maxY = -1;
  const near = (r, g, b, c) => Math.abs(r - (c >> 16)) <= 12
    && Math.abs(g - ((c >> 8) & 255)) <= 12 && Math.abs(b - (c & 255)) <= 12;
  for (let y = Math.ceil(region.y); y < Math.min(height, region.y + region.height); y++) {
    for (let x = Math.ceil(region.x); x < Math.min(width, region.x + region.width); x++) {
      const i = (y * width + x) * 4;
      const [r, g, b] = [data[i], data[i + 1], data[i + 2]];
      total++;
      if (near(r, g, b, 0x10182a)) dark++;
      const color = ballColors.findIndex(c => near(r, g, b, c));
      const hit = kind === 'balls' ? color >= 0
        : kind === 'camera' ? Math.max(r, g, b) - Math.min(r, g, b) > 40
          : b > 60 && b > r * 1.5 && b > g * 1.2;
      if (hit) {
        foreground++; sumX += x; sumY += y; colors.add(color);
        minX = Math.min(minX, x); minY = Math.min(minY, y);
        maxX = Math.max(maxX, x); maxY = Math.max(maxY, y);
      }
      mask.push(hit ? (kind === 'balls' ? color + 1 : kind === 'camera' ? 1 + r * 65536 + g * 256 + b : 1) : 0);
    }
  }
  return {darkFraction: dark / total, foreground, colors: colors.size,
    bounds: foreground ? {width: maxX - minX + 1, height: maxY - minY + 1} : null,
    center: foreground ? {x: sumX / foreground, y: sumY / foreground} : null, mask};
}

export function changedPixels(a, b) {
  if (a.mask.length !== b.mask.length) throw new Error('Frame sizes changed during animation check');
  return a.mask.reduce((n, value, i) => n + (value !== b.mask[i] ? 1 : 0), 0);
}

export function sceneChecks(frames, region, kind) {
  return {
    'visible content': frames.every(f => f.foreground > 150) && (kind === 'balls'
      ? frames.some(f => f.colors >= 4)
      : frames.every(f => f.center && Math.abs(f.center.x - (region.x + region.width / 2)) < region.width * .2
        && Math.abs(f.center.y - (region.y + region.height / 2)) < region.height * .2)),
    ...(kind === 'cube' ? {'cube proportions': frames.every(f => f.bounds
      && f.bounds.width / f.bounds.height > .65 && f.bounds.width / f.bounds.height < 1.55)} : {}),
    'fills preview': frames.every(f => f.darkFraction > .75),
    animation: frames.every(f => f.foreground > 150) && frames.slice(1).some(f => changedPixels(frames[0], f) > 100)
  };
}

// Compilation and semantic layout can finish before software WebGL presents a
// frame. Require the same pixel checks, but give startup/animation a bounded
// opportunity to present three usable frames instead of assuming a frame rate.
export async function sampleScene(sample, wait, region, kind, now = Date.now, timeout = 8000) {
  const deadline = now() + timeout;
  const frames = [];
  let index = 0;
  do {
    frames.push(await sample(index++));
    if (frames.length > 3) frames.shift();
    if (frames.length === 3 && Object.values(sceneChecks(frames, region, kind)).every(Boolean)) break;
    if (now() >= deadline) break;
    await wait(220);
  } while (true);
  return frames;
}
