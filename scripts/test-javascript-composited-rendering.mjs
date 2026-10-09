/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */

/*
 * What a user of the JavaScript port actually sees, measured on the COMPOSITED page.
 *
 * The screenshot suite cannot catch this class of bug by construction: its capture reads the
 * app's own canvas back, and doing so suspends the DOM text layer, so it never sees the page a
 * browser shows -- the canvas, the text layer over it and whatever the input path did to both.
 * Issues #5910 and #5912 were exactly that shape and came back more than once while that suite
 * stayed green. Every check here drives real input and reads the browser's own screenshot or the
 * output canvas from outside the app.
 *
 * Checks, against scripts/javascript/composited-app (the Initializr Hello World plus one long
 * SpanLabel, the app both issues were reported against):
 *
 *   desktop, per browser (Chromium and Firefox by default):
 *     - a wheel scroll leaves the Toolbar exactly where it was and moves the content (#5910:
 *       the wheel used to scroll the Form itself, taking the Toolbar with it and leaving a
 *       blank band at the bottom);
 *     - a Dialog is drawn over its form, tinted, not over a blank page (#5910);
 *     - a mouse drag scrolls the content (the move events never reached the app);
 *     - a click on a string Picker puts its native <select> on the page, opens the list where
 *       the browser can, and a choice made there reaches the Picker (it failed with "Option is
 *       not defined"; also run on the phone);
 *   phone (Chromium with touch emulation):
 *     - a swipe scrolls the content (#5912: it did nothing at all);
 *     - a tap on the hamburger opens the side menu, and a tap on a button fires it (#5912);
 *     - with the side menu open, none of the form's DOM text floats over the menu's shade.
 *
 * Usage:
 *   node scripts/test-javascript-composited-rendering.mjs <bundle-dir> [artifacts-dir]
 *
 * Environment:
 *   CN1_COMPOSITED_BROWSERS  desktop browsers, comma separated (default "chromium,firefox")
 *   CN1_COMPOSITED_TIMEOUT   boot timeout in seconds (default 180)
 */
import fs from 'fs';
import http from 'http';
import path from 'path';
import { createRequire } from 'module';

const require = createRequire(import.meta.url);
const { chromium, firefox, webkit, devices } = require('playwright');

const bundleDir = process.argv[2] ? path.resolve(process.argv[2]) : null;
const outDir = process.argv[3] || path.resolve('artifacts/javascript-composited');
if (!bundleDir || !fs.existsSync(path.join(bundleDir, 'index.html'))) {
  console.error('usage: test-javascript-composited-rendering.mjs <bundle-dir> [artifacts-dir]');
  process.exit(2);
}
fs.mkdirSync(outDir, { recursive: true });
const bootTimeout = Number(process.env.CN1_COMPOSITED_TIMEOUT || '180') * 1000;
const desktopBrowsers = (process.env.CN1_COMPOSITED_BROWSERS || 'chromium,firefox')
  .split(',').map((s) => s.trim()).filter(Boolean);

const TYPES = {
  '.html': 'text/html', '.js': 'text/javascript', '.mjs': 'text/javascript', '.css': 'text/css',
  '.wasm': 'application/wasm', '.json': 'application/json', '.png': 'image/png', '.gif': 'image/gif',
  '.ttf': 'font/ttf', '.otf': 'font/otf'
};
const server = http.createServer((req, res) => {
  let p = decodeURIComponent(req.url.split('?')[0]);
  if (p === '/') p = '/index.html';
  const file = path.join(bundleDir, path.normalize(p));
  if (!file.startsWith(bundleDir)) { res.writeHead(403); res.end(); return; }
  fs.readFile(file, (err, data) => {
    if (err) { res.writeHead(404); res.end(); return; }
    res.writeHead(200, { 'Content-Type': TYPES[path.extname(file)] || 'application/octet-stream' });
    res.end(data);
  });
});
await new Promise((resolve) => server.listen(0, '127.0.0.1', resolve));
const url = `http://127.0.0.1:${server.address().port}/`;

const failures = [];
function check(ok, label, detail) {
  console.log(`${ok ? 'PASS' : 'FAIL'} ${label}${detail ? ' -- ' + detail : ''}`);
  if (!ok) failures.push(label);
}

async function boot(page, logs, query = '', firstText = 'hello world') {
  page.on('console', (m) => logs.push(`${m.type()}: ${m.text()}`));
  page.on('pageerror', (e) => logs.push(`pageerror: ${e.message}`));
  await page.goto(url + query);
  await page.waitForFunction(() => window.cn1Started, null, { timeout: bootTimeout });
  // The first form paints over a few frames after the lifecycle reports it started.
  // Case-insensitive: the Android theme a phone user agent gets upper-cases button labels.
  await page.waitForFunction((t) => Array.from(document.querySelectorAll('#cn1-text-layer span'))
    .some((s) => s.textContent.trim().toLowerCase() === t), firstText, { timeout: bootTimeout });
  await page.waitForTimeout(1500);
}

function span(page, text) {
  return page.evaluate((t) => {
    const s = Array.from(document.querySelectorAll('#cn1-text-layer span'))
      .find((e) => e.textContent.trim().toLowerCase() === t.toLowerCase()
        || e.textContent.toLowerCase().startsWith(t.toLowerCase()));
    if (!s) return null;
    const r = s.getBoundingClientRect();
    return { x: r.x + r.width / 2, y: r.y + r.height / 2, top: r.y, bottom: r.y + r.height };
  }, text);
}

// Pixels of the output canvas, read from the page rather than by the app, in CSS coordinates.
function canvasPixels(page, points) {
  return page.evaluate((pts) => {
    const c = document.getElementById('codenameone-canvas');
    const ctx = c.getContext('2d');
    const scale = c.width / c.clientWidth;
    const top = c.getBoundingClientRect().top;
    return pts.map(([x, y]) => Array.from(ctx.getImageData(Math.round(x * scale),
      Math.round((y - top) * scale), 1, 1).data));
  }, points);
}

function luminance(px) {
  return 0.2126 * px[0] + 0.7152 * px[1] + 0.0722 * px[2];
}

// Fraction of differing bytes between two PNG screenshots of the same size, over a band of rows.
// Decoding PNG here would need a dependency the workflow does not install, so the comparison is
// done in the page, which can decode them for free.
async function diffFraction(page, a, b, band) {
  return page.evaluate(async ({ a, b, band }) => {
    const load = (b64) => new Promise((res) => { const i = new Image(); i.onload = () => res(i); i.src = 'data:image/png;base64,' + b64; });
    const [ia, ib] = await Promise.all([load(a), load(b)]);
    const w = ia.width;
    const y0 = Math.max(0, Math.floor(band[0] * ia.height / window.innerHeight));
    const y1 = Math.min(ia.height, Math.ceil(band[1] * ia.height / window.innerHeight));
    const cv = document.createElement('canvas'); cv.width = w; cv.height = ia.height;
    const cx = cv.getContext('2d');
    cx.drawImage(ia, 0, 0); const da = cx.getImageData(0, y0, w, y1 - y0).data;
    cx.clearRect(0, 0, w, ia.height); cx.drawImage(ib, 0, 0); const db = cx.getImageData(0, y0, w, y1 - y0).data;
    let diff = 0;
    for (let i = 0; i < da.length; i += 4) {
      if (Math.abs(da[i] - db[i]) + Math.abs(da[i + 1] - db[i + 1]) + Math.abs(da[i + 2] - db[i + 2]) > 24) diff++;
    }
    return diff / (da.length / 4);
  }, { a: a.toString('base64'), b: b.toString('base64'), band });
}

// Fraction of pixels in a band that differ from the band's most common colour: near zero for an
// empty stretch of background, well above it where text is drawn.
async function bandContent(page, png, band) {
  return page.evaluate(async ({ a, band }) => {
    const img = await new Promise((res) => { const i = new Image(); i.onload = () => res(i); i.src = 'data:image/png;base64,' + a; });
    const y0 = Math.floor(band[0] * img.height / window.innerHeight);
    const y1 = Math.ceil(band[1] * img.height / window.innerHeight);
    const cv = document.createElement('canvas'); cv.width = img.width; cv.height = img.height;
    const cx = cv.getContext('2d'); cx.drawImage(img, 0, 0);
    const d = cx.getImageData(0, y0, img.width, y1 - y0).data;
    const counts = new Map();
    for (let i = 0; i < d.length; i += 4) { const k = (d[i] << 16) | (d[i + 1] << 8) | d[i + 2]; counts.set(k, (counts.get(k) || 0) + 1); }
    let top = 0; for (const v of counts.values()) { if (v > top) top = v; }
    return 1 - top / (d.length / 4);
  }, { a: png.toString('base64'), band });
}

// A string Picker takes the port's native path unless the application forces the lightweight
// popup, and that path built its <select> with "new Option(...)" evaluated in the worker, where
// there is no DOM: every such Picker answered a click with "Option is not defined" and never
// opened. The click here is a real one, and the choice is made on the <select> the way a
// browser reports one, with a change event.
async function picker(name, page, press) {
  const logs = [];
  await page.addInitScript(() => {
    window.__cn1ShowPicker = [];
    const original = HTMLSelectElement.prototype.showPicker;
    if (original) {
      HTMLSelectElement.prototype.showPicker = function() {
        try {
          original.call(this);
          window.__cn1ShowPicker.push('opened');
        } catch (e) {
          window.__cn1ShowPicker.push(String(e));
          throw e;
        }
      };
    }
  });
  await boot(page, logs, '?screen=picker', 'picked nothing');
  const face = await span(page, 'One');
  check(!!face, `${name}: the Picker shows its value`);
  if (face) {
    await press(Math.round(face.x), Math.round(face.y));
    const select = page.locator('select.cn1-string-picker');
    let shown = true;
    try {
      await select.waitFor({ state: 'attached', timeout: 10000 });
    } catch (err) {
      shown = false;
    }
    const failed = logs.filter((l) => /is not defined|Exception/.test(l));
    check(shown, `${name}: a click on a string Picker puts its <select> on the page`,
      failed.length ? failed[0].slice(0, 200) : '');
    if (shown) {
      const options = await select.locator('option').allTextContents();
      check(options.join(',') === 'One,Two,Three', `${name}: the <select> lists the Picker's strings`, options.join(','));
      check(await select.inputValue() === '0', `${name}: the <select> starts on the Picker's value`);
      // Browsers without showPicker() leave the list one more click away; where it exists it
      // has to succeed, since the click that asked for it is still a live gesture.
      const opened = await page.evaluate(() => ({ has: 'showPicker' in HTMLSelectElement.prototype, calls: window.__cn1ShowPicker }));
      check(!opened.has || (opened.calls.length === 1 && opened.calls[0] === 'opened'),
        `${name}: the list opens on the first click`, opened.has ? JSON.stringify(opened.calls) : 'no showPicker() in this browser');
      await select.selectOption({ label: 'Two' });
      let chosen = true;
      try {
        await page.waitForFunction(() => Array.from(document.querySelectorAll('#cn1-text-layer span'))
          .some((s) => s.textContent.trim().toLowerCase() === 'picked two'), null, { timeout: 10000 });
      } catch (err) {
        chosen = false;
      }
      check(chosen, `${name}: choosing an option sets the Picker's value`);
      await page.waitForTimeout(500);
      check(await select.count() === 0, `${name}: the <select> is removed once a choice is made`);
    }
  }
  await page.screenshot({ path: path.join(outDir, `${name}-picker.png`) });
  const errors = logs.filter((l) => /is not defined|Exception/.test(l));
  check(errors.length === 0, `${name}: the Picker raises no error`, errors.length ? errors[0].slice(0, 200) : '');
  fs.writeFileSync(path.join(outDir, `${name}-picker-console.txt`), logs.join('\n'));
}

async function desktop(name, browserType) {
  const browser = await browserType.launch();
  try {
    // Dialog first, on a fresh page: the wheel below scrolls the button out of view.
    {
      const logs = [];
      const page = await browser.newPage({ viewport: { width: 1000, height: 450 } });
      await boot(page, logs);
      const corners = [[6, 440], [994, 440], [994, 70]];
      const before = await canvasPixels(page, corners);
      const button = await span(page, 'Hello World');
      await page.mouse.click(button.x, button.y);
      await page.waitForTimeout(2500);
      await page.screenshot({ path: path.join(outDir, `${name}-dialog.png`) });
      const after = await canvasPixels(page, corners);
      const painted = after.every((px) => px[3] === 255);
      const tinted = after.every((px, i) => px[3] === 255 && luminance(px) < luminance(before[i]) - 15);
      check(painted, `${name}: the form stays painted around a Dialog`,
        `alpha around the dialog: ${after.map((p) => p[3]).join(',')}`);
      check(tinted, `${name}: the form around a Dialog is tinted`,
        `luminance ${before.map(luminance).map(Math.round).join(',')} -> ${after.map(luminance).map(Math.round).join(',')}`);
      fs.writeFileSync(path.join(outDir, `${name}-dialog-console.txt`), logs.join('\n'));
      await page.close();
    }
    {
      const logs = [];
      const page = await browser.newPage({ viewport: { width: 1000, height: 450 } });
      await boot(page, logs);
      const title = await span(page, 'Hi World');
      const toolbarBottom = Math.max(20, Math.floor(title.bottom));
      const a = await page.screenshot({ path: path.join(outDir, `${name}-scroll-0.png`) });
      await page.mouse.move(500, 300);
      for (let i = 0; i < 8; i++) { await page.mouse.wheel(0, 120); await page.waitForTimeout(120); }
      await page.waitForTimeout(1500);
      const b = await page.screenshot({ path: path.join(outDir, `${name}-scroll-1.png`) });
      const toolbarDiff = await diffFraction(page, a, b, [0, toolbarBottom]);
      const contentDiff = await diffFraction(page, a, b, [toolbarBottom + 20, 440]);
      check(toolbarDiff < 0.01, `${name}: the Toolbar stays put while the content scrolls`,
        `${(toolbarDiff * 100).toFixed(2)}% of the toolbar band changed`);
      check(contentDiff > 0.05, `${name}: the wheel scrolls the content`,
        `${(contentDiff * 100).toFixed(2)}% of the content changed`);
      // Past the end of the page the wheel used to scroll the Form itself, leaving a blank band
      // in the form's background colour -- painted, just empty -- so this looks for text there.
      const content = await bandContent(page, b, [330, 440]);
      check(content > 0.01, `${name}: the bottom of the page still shows content after scrolling`,
        `${(content * 100).toFixed(2)}% of the bottom band is not background`);
      fs.writeFileSync(path.join(outDir, `${name}-scroll-console.txt`), logs.join('\n'));
      await page.close();
    }
    {
      // A mouse drag scrolls too. The move listeners were registered in a way the worker
      // bridge never delivered, so the press and release arrived and every move between them
      // was lost -- a drag moved nothing on any platform.
      const logs = [];
      const page = await browser.newPage({ viewport: { width: 1000, height: 450 } });
      await boot(page, logs);
      const title = await span(page, 'Hi World');
      const a = await page.screenshot();
      await page.mouse.move(600, 400);
      await page.mouse.down();
      for (let i = 1; i <= 15; i++) { await page.mouse.move(600, 400 - i * 15); await page.waitForTimeout(20); }
      await page.mouse.up();
      await page.waitForTimeout(1500);
      const b = await page.screenshot({ path: path.join(outDir, `${name}-drag.png`) });
      const moved = await diffFraction(page, a, b, [Math.floor(title.bottom) + 20, 440]);
      check(moved > 0.05, `${name}: a mouse drag scrolls the content`, `${(moved * 100).toFixed(2)}% changed`);
      fs.writeFileSync(path.join(outDir, `${name}-drag-console.txt`), logs.join('\n'));
      await page.close();
    }
    {
      const page = await browser.newPage({ viewport: { width: 1000, height: 450 } });
      await picker(name, page, (x, y) => page.mouse.click(x, y));
      await page.close();
    }
  } finally {
    await browser.close();
  }
}

async function phone() {
  const browser = await chromium.launch();
  try {
    const context = await browser.newContext({ ...devices['Pixel 7'] });
    {
      const logs = [];
      const page = await context.newPage();
      await boot(page, logs);
      const vp = page.viewportSize();
      const title = await span(page, 'Hi World');
      const band = [Math.floor(title.bottom) + 30, vp.height - 10];
      const a = await page.screenshot({ path: path.join(outDir, 'phone-swipe-0.png') });
      const cdp = await context.newCDPSession(page);
      const x = Math.round(vp.width / 2);
      const y0 = Math.round(vp.height * 0.8);
      const y1 = Math.round(vp.height * 0.3);
      await cdp.send('Input.dispatchTouchEvent', { type: 'touchStart', touchPoints: [{ x, y: y0 }] });
      for (let i = 1; i <= 15; i++) {
        await cdp.send('Input.dispatchTouchEvent', { type: 'touchMove', touchPoints: [{ x, y: Math.round(y0 + (y1 - y0) * i / 15) }] });
        await page.waitForTimeout(16);
      }
      await cdp.send('Input.dispatchTouchEvent', { type: 'touchEnd', touchPoints: [] });
      await page.waitForTimeout(2000);
      const b = await page.screenshot({ path: path.join(outDir, 'phone-swipe-1.png') });
      const moved = await diffFraction(page, a, b, band);
      check(moved > 0.05, 'phone: a swipe scrolls the content', `${(moved * 100).toFixed(2)}% changed`);
      fs.writeFileSync(path.join(outDir, 'phone-swipe-console.txt'), logs.join('\n'));
      await page.close();
    }
    {
      const logs = [];
      const page = await context.newPage();
      await boot(page, logs);
      const vp = page.viewportSize();
      const title = await span(page, 'Hi World');
      const a = await page.screenshot();
      // The hamburger sits at the left end of the Toolbar, level with the title.
      await page.touchscreen.tap(22, Math.round(title.y));
      await page.waitForTimeout(2500);
      const b = await page.screenshot({ path: path.join(outDir, 'phone-menu.png') });
      const opened = await diffFraction(page, a, b, [title.bottom + 10, vp.height - 10]);
      check(opened > 0.2, 'phone: a tap on the hamburger opens the side menu', `${(opened * 100).toFixed(2)}% changed`);
      // The form's text beside the open menu is under the menu's shade. The DOM text layer
      // sits above the whole canvas, so any of the form's runs still in the DOM would float
      // over the shade, crisp and untinted; they have to be drawn on the canvas instead.
      const floating = await page.evaluate(() => Array.from(document.querySelectorAll('#cn1-text-layer span'))
        .filter((s) => { const r = s.getBoundingClientRect(); return r.width > 0 && r.right > window.innerWidth * 0.9
          && getComputedStyle(s).visibility !== 'hidden'; })
        .map((s) => s.textContent.slice(0, 20)));
      check(floating.length === 0, 'phone: the form\'s text stays under the side menu\'s shade',
        floating.length ? `DOM text over the shade: ${JSON.stringify(floating.slice(0, 5))}` : '');
      fs.writeFileSync(path.join(outDir, 'phone-menu-console.txt'), logs.join('\n'));
      await page.close();
    }
    {
      const logs = [];
      const page = await context.newPage();
      await boot(page, logs);
      const vp = page.viewportSize();
      const before = await canvasPixels(page, [[6, vp.height - 6]]);
      const button = await span(page, 'Hello World');
      await page.touchscreen.tap(Math.round(button.x), Math.round(button.y));
      await page.waitForTimeout(2500);
      await page.screenshot({ path: path.join(outDir, 'phone-dialog.png') });
      const after = await canvasPixels(page, [[6, vp.height - 6]]);
      check(luminance(after[0]) < luminance(before[0]) - 15 && after[0][3] === 255,
        'phone: a tap on a button fires it', `corner luminance ${Math.round(luminance(before[0]))} -> ${Math.round(luminance(after[0]))}`);
      fs.writeFileSync(path.join(outDir, 'phone-dialog-console.txt'), logs.join('\n'));
      await page.close();
    }
    {
      // A touch the browser cancels (palm rejection, focus loss) ends with touchcancel and no
      // touchend. It must not act as a release -- the button under the finger stays unfired --
      // but it must end the touch: left unanswered, the port kept the touch down and took every
      // later touch for an extra finger, so the tap below did nothing and touch input stayed dead.
      const logs = [];
      const page = await context.newPage();
      await boot(page, logs);
      const vp = page.viewportSize();
      const cdp = await context.newCDPSession(page);
      const button = await span(page, 'Hello World');
      const before = await canvasPixels(page, [[6, vp.height - 6]]);
      await cdp.send('Input.dispatchTouchEvent', { type: 'touchStart',
        touchPoints: [{ x: Math.round(button.x), y: Math.round(button.y) }] });
      await page.waitForTimeout(300);
      await cdp.send('Input.dispatchTouchEvent', { type: 'touchCancel', touchPoints: [] });
      await page.waitForTimeout(2500);
      const cancelled = await canvasPixels(page, [[6, vp.height - 6]]);
      check(Math.abs(luminance(cancelled[0]) - luminance(before[0])) < 15,
        'phone: a cancelled touch does not fire what it was pressing', `corner luminance ${Math.round(luminance(before[0]))} -> ${Math.round(luminance(cancelled[0]))}`);
      await page.touchscreen.tap(Math.round(button.x), Math.round(button.y));
      await page.waitForTimeout(2500);
      const after = await canvasPixels(page, [[6, vp.height - 6]]);
      check(luminance(after[0]) < luminance(before[0]) - 15 && after[0][3] === 255,
        'phone: a tap after a cancelled touch still fires', `corner luminance ${Math.round(luminance(before[0]))} -> ${Math.round(luminance(after[0]))}`);
      fs.writeFileSync(path.join(outDir, 'phone-cancel-console.txt'), logs.join('\n'));
      await page.close();
    }
    {
      const page = await context.newPage();
      await picker('phone', page, (x, y) => page.touchscreen.tap(x, y));
      await page.close();
    }
  } finally {
    await browser.close();
  }
}

try {
  const types = { chromium, firefox, webkit };
  for (const name of desktopBrowsers) {
    if (!types[name]) { check(false, `unknown browser ${name}`); continue; }
    await desktop(name, types[name]);
  }
  await phone();
} catch (err) {
  check(false, 'the run completed', err && err.stack ? err.stack : String(err));
} finally {
  server.close();
}

if (failures.length) {
  console.error(`\n${failures.length} check(s) failed; screenshots and console logs are in ${outDir}`);
  process.exit(1);
}
console.log('\nAll composited rendering and input checks passed.');
