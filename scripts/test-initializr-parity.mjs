// Side-by-side parity test: TeaVM (production) vs ParparVM (PR preview).
// Run the same interactions on each, log console messages and exception
// traces, snapshot the canvas before/after each step, and report deltas.
//
// node scripts/test-initializr-parity.mjs
import { chromium } from 'playwright';
import fs from 'node:fs';
import path from 'node:path';

const URL_TEAVM = process.env.URL_TEAVM || 'https://www.codenameone.com/initializr/';
const URL_PARPAR = process.env.URL_PARPAR || 'https://pr-4795-website-preview.codenameone.pages.dev/initializr/';

async function openSession(label, url) {
  const browser = await chromium.launch();
  const ctx = await browser.newContext({ viewport: { width: 1280, height: 900 } });
  const page = await ctx.newPage();
  const messages = [];
  page.on('console', msg => messages.push(`[${label}/${msg.type()}] ${msg.text()}`));
  page.on('pageerror', err => messages.push(`[${label}/pageerror] ${err.message}`));
  page.on('worker', worker => {
    worker.on('console', msg => messages.push(`[${label}/worker:${msg.type()}] ${msg.text()}`));
  });
  await page.goto(url, { waitUntil: 'domcontentloaded' });
  return { browser, ctx, page, messages };
}

async function appFrame(s) {
  // Find the frame that has a <canvas> in its document — that's the
  // CN1 app. URL patterns vary across the two deploys.
  for (const f of s.page.frames()) {
    try {
      const has = await f.evaluate(() => !!document.querySelector('canvas'));
      if (has) return f;
    } catch (e) {}
  }
  return s.page.mainFrame();
}

async function waitForReady(s, label, deadlineMs) {
  // Both samples set window.cn1Started inside the embedded app iframe
  // after lifecycle.start() completes.
  const start = Date.now();
  while (Date.now() - start < deadlineMs) {
    try {
      const frame = await appFrame(s);
      const ok = await frame.evaluate(() => !!window.cn1Started).catch(() => false);
      if (ok) return Date.now() - start;
    } catch (e) {}
    await new Promise(r => setTimeout(r, 250));
  }
  return -1;
}

async function snapshotSig(s) {
  const frame = await appFrame(s);
  return await frame.evaluate(() => {
    const cnv = document.querySelector('canvas');
    if (!cnv) return null;
    const tmp = document.createElement('canvas');
    tmp.width = 16; tmp.height = 16;
    const ctx = tmp.getContext('2d');
    ctx.drawImage(cnv, 0, 0, 16, 16);
    const px = ctx.getImageData(0, 0, 16, 16).data;
    let sig = '';
    let blackCells = 0;
    for (let i = 0; i < px.length; i += 4) {
      const lum = ((px[i] * 299 + px[i+1] * 587 + px[i+2] * 114) / 1000) | 0;
      if (lum < 16) blackCells++;
      sig += lum.toString(16).padStart(2, '0');
    }
    return { sig, blackFrac: blackCells / 256, w: cnv.width, h: cnv.height };
  });
}

function sigHamming(a, b) {
  if (!a || !b) return -1;
  let diff = 0;
  for (let i = 0; i < a.sig.length; i += 2) {
    if (a.sig.substring(i, i+2) !== b.sig.substring(i, i+2)) diff++;
  }
  return diff;
}

async function snapshotPng(s, label) {
  const out = path.join('/tmp', `parity-${label}.png`);
  const frame = await appFrame(s);
  // Snapshot via frame element so we capture inside the iframe.
  try {
    const handle = await frame.locator('canvas').elementHandle();
    if (handle) {
      await handle.screenshot({ path: out });
      return out;
    }
  } catch (e) {}
  await s.page.screenshot({ path: out, clip: { x: 0, y: 0, width: 1280, height: 900 } }).catch(() => {});
  return out;
}

async function clickAt(s, x, y) {
  await s.page.mouse.move(x, y);
  await new Promise(r => setTimeout(r, 80));
  await s.page.mouse.down();
  await new Promise(r => setTimeout(r, 100));
  await s.page.mouse.up();
}

async function dragFromTo(s, x1, y1, x2, y2) {
  await s.page.mouse.move(x1, y1);
  await s.page.mouse.down();
  const steps = 8;
  for (let i = 1; i <= steps; i++) {
    await s.page.mouse.move(x1 + (x2-x1)*i/steps, y1 + (y2-y1)*i/steps);
    await new Promise(r => setTimeout(r, 30));
  }
  await s.page.mouse.up();
}

async function runScenario(s, label) {
  const result = { label, steps: [] };
  const ms = (k, v) => result.steps.push({ k, ...v });

  const readyMs = await waitForReady(s, label, 30000);
  ms('ready', { ms: readyMs });
  if (readyMs < 0) return result;

  await new Promise(r => setTimeout(r, 1000));
  const sigStart = await snapshotSig(s);
  await snapshotPng(s, `${label}-01-start`);
  ms('snap-start', { blackFrac: sigStart && sigStart.blackFrac, sig: sigStart && sigStart.sig.slice(0, 16) });

  // 1) Click the Hello World button in the preview (top-right pane).
  await clickAt(s, 936, 141);
  await new Promise(r => setTimeout(r, 1500));
  const sigAfterHello = await snapshotSig(s);
  await snapshotPng(s, `${label}-02-after-hello-click`);
  ms('after-hello-click', {
    blackFrac: sigAfterHello && sigAfterHello.blackFrac,
    diffFromStart: sigHamming(sigStart, sigAfterHello)
  });

  // 2) Click center-bottom area where the OK button typically sits in
  //    the dialog. (574, 455) was the historical position; without DPR
  //    doubling the dialog may be smaller -- click roughly center too.
  for (const [cx, cy] of [[574, 455], [640, 510], [640, 470], [594, 510]]) {
    await clickAt(s, cx, cy);
    await new Promise(r => setTimeout(r, 800));
    const sig = await snapshotSig(s);
    if (sigHamming(sigAfterHello, sig) > 8) {
      ms('ok-click-took-effect', { at: [cx, cy] });
      break;
    }
  }
  await new Promise(r => setTimeout(r, 1500));
  const sigAfterOk = await snapshotSig(s);
  await snapshotPng(s, `${label}-03-after-ok`);
  ms('after-ok', {
    blackFrac: sigAfterOk && sigAfterOk.blackFrac,
    diffFromStart: sigHamming(sigStart, sigAfterOk),
    diffFromDialog: sigHamming(sigAfterHello, sigAfterOk)
  });

  // 3) Open the side menu via the hamburger icon in the preview's top-
  //    left corner of the right pane (~700, 100).
  await clickAt(s, 700, 100);
  await new Promise(r => setTimeout(r, 1500));
  const sigAfterMenu = await snapshotSig(s);
  await snapshotPng(s, `${label}-04-after-menu-click`);
  ms('after-menu-click', {
    blackFrac: sigAfterMenu && sigAfterMenu.blackFrac,
    diffFromOk: sigHamming(sigAfterOk, sigAfterMenu)
  });

  // 4) Try scrolling the left column.
  await s.page.mouse.move(300, 500);
  await s.page.mouse.wheel(0, 400);
  await new Promise(r => setTimeout(r, 800));
  const sigAfterScroll = await snapshotSig(s);
  await snapshotPng(s, `${label}-05-after-scroll`);
  ms('after-scroll', {
    blackFrac: sigAfterScroll && sigAfterScroll.blackFrac,
    diffFromMenu: sigHamming(sigAfterMenu, sigAfterScroll)
  });

  // 5) Drag-scroll the left column.
  await dragFromTo(s, 300, 600, 300, 200);
  await new Promise(r => setTimeout(r, 800));
  const sigAfterDrag = await snapshotSig(s);
  await snapshotPng(s, `${label}-06-after-drag`);
  ms('after-drag', {
    blackFrac: sigAfterDrag && sigAfterDrag.blackFrac,
    diffFromScroll: sigHamming(sigAfterScroll, sigAfterDrag)
  });

  return result;
}

console.log('Opening TeaVM (reference):', URL_TEAVM);
const tea = await openSession('TEAVM', URL_TEAVM);
console.log('Opening ParparVM (PR):', URL_PARPAR);
const parpar = await openSession('PARPAR', URL_PARPAR);

console.log('\n=== Running scenario on TeaVM ===');
const teaResult = await runScenario(tea, 'TEAVM');
console.log('\n=== Running scenario on ParparVM ===');
const parparResult = await runScenario(parpar, 'PARPAR');

console.log('\n=== TeaVM steps ===');
teaResult.steps.forEach(s => console.log('  ', JSON.stringify(s)));
console.log('\n=== ParparVM steps ===');
parparResult.steps.forEach(s => console.log('  ', JSON.stringify(s)));

const teaErrors = tea.messages.filter(m => /error|exception|uncaught|missing/i.test(m));
const parparErrors = parpar.messages.filter(m => /error|exception|uncaught|missing/i.test(m));

console.log('\n=== TeaVM errors (first 10) ===');
teaErrors.slice(0, 10).forEach(e => console.log(' ', e));
console.log(`(total: ${teaErrors.length})`);

console.log('\n=== ParparVM errors (first 30) ===');
parparErrors.slice(0, 30).forEach(e => console.log(' ', e));
console.log(`(total: ${parparErrors.length})`);

fs.writeFileSync('/tmp/parity-tea-messages.log', tea.messages.join('\n'));
fs.writeFileSync('/tmp/parity-parpar-messages.log', parpar.messages.join('\n'));

await tea.browser.close();
await parpar.browser.close();
console.log('\nLogs: /tmp/parity-tea-messages.log /tmp/parity-parpar-messages.log');
console.log('Screenshots: /tmp/parity-TEAVM-*.png /tmp/parity-PARPAR-*.png');
