// Runs the unmodified bundled demos through user input and checks displayed pixels.
// No physical camera is used: Chromium supplies its deterministic moving test pattern.
import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import {scenePixels, changedPixels, sceneChecks} from './demo-pixels.mjs';
let chromium, firefox;
try { ({chromium, firefox} = await import('playwright')); }
catch { ({chromium, firefox} = await import('@playwright/test')); }
const url = process.argv[2];
if (!url) throw new Error('Usage: node verify-playground-demos.mjs <playground-app-url>');
const artifacts = process.env.PLAYGROUND_BROWSER_ARTIFACT_DIR || 'playground-demo-artifacts';
fs.mkdirSync(artifacts, {recursive: true});
const results = [];
let browser;
let browserName;
let viewport;
let deviceScaleFactor;
let appFrame;

async function measure(page, region, kind, screenshotPath) {
  const clip = {x: Math.floor(region.x), y: Math.floor(region.y),
    width: Math.floor(region.width), height: Math.floor(region.height)};
  const png = await page.screenshot({scale: 'css', clip, ...(screenshotPath ? {path: screenshotPath} : {})});
  // Decode the screenshot in an unattached canvas. Reading the app's WebGL drawing
  // buffer alone would miss overlays, clipping, placement and compositing failures.
  const decoded = await page.evaluate(async base64 => {
    const image = new Image();
    image.src = 'data:image/png;base64,' + base64;
    await image.decode();
    const canvas = document.createElement('canvas');
    canvas.width = image.width; canvas.height = image.height;
    const context = canvas.getContext('2d');
    context.drawImage(image, 0, 0);
    return {width: canvas.width, height: canvas.height,
      data: Array.from(context.getImageData(0, 0, canvas.width, canvas.height).data)};
  }, png.toString('base64'));
  const result = scenePixels(decoded, {x: 0, y: 0, width: decoded.width, height: decoded.height}, kind);
  if (result.center) { result.center.x += clip.x; result.center.y += clip.y; }
  return result;
}
async function clickControl(page, name) {
  const control = appFrame.getByRole('button', {name, exact: true});
  await control.waitFor();
  assert.notEqual(await control.getAttribute('aria-disabled'), 'true', name + ' is disabled');
  const r = await control.boundingBox();
  assert.ok(r && r.width > 0 && r.height > 0, name + ' has no bounds');
  assert.ok(r.y >= 0 && r.y + r.height <= page.viewportSize().height, name + ' is outside the viewport');
  // The accessibility overlay is pointer-transparent. Click the actual CN1 canvas
  // at the live semantic bounds, rather than dispatching an artificial action.
  await page.mouse.click(r.x + r.width / 2, r.y + r.height / 2);
}
async function cameraStatus(pattern) {
  // Labels expose an accessible name; wrapped read-only text exposes a value.
  await appFrame.waitForFunction(source => [...document.querySelectorAll('#cn1-accessibility-tree [data-cn1-accessibility-id]')]
    .some(e => new RegExp(source).test(e.getAttribute('aria-label') || e.value || e.textContent || '')),
    pattern, {timeout: 15000});
}
async function preview(page, title) {
  const form = appFrame.getByRole('group', {name: title, exact: true});
  await form.waitFor({timeout: 90000});
  const bounds = await form.boundingBox();
  const heading = await form.locator(`[aria-label="${title}"]`).boundingBox();
  assert.ok(bounds && heading, 'Missing preview geometry');
  const region = {x: bounds.x + 12, y: heading.y + heading.height + 12,
    width: bounds.width - 24, height: bounds.y + bounds.height - 12 - (heading.y + heading.height + 12)};
  assert.ok(region.width > 120 && region.height > 200, 'Preview is unexpectedly small');
  assert.ok(region.x > page.viewportSize().width / 2 && region.y + region.height <= page.viewportSize().height,
    'Preview must be visible alongside the editor');
  return region;
}
async function check(name, fn) {
  try { await fn(); results.push({name, ok: true}); console.log('PASS ' + name); }
  catch (error) { results.push({name, ok: false, error: error.message}); console.error('FAIL ' + name + ': ' + error.message); }
}
async function run(slug, title, width, exercise, caseName = slug) {
  const name = browserName + '-' + caseName + '-' + width + 'x' + viewport.height + '-dpr' + deviceScaleFactor;
  if (process.env.PLAYGROUND_DEMO_FILTER && !name.includes(process.env.PLAYGROUND_DEMO_FILTER)) return;
  console.log('RUN ' + name);
  appFrame = null;
  const page = await browser.newPage({viewport, deviceScaleFactor});
  page.setDefaultTimeout(15000);
  await page.addInitScript(() => {
    window.__playgroundMediaRequests = [];
    window.__playgroundMediaTracks = [];
    if (navigator.mediaDevices) {
      const getUserMedia = navigator.mediaDevices.getUserMedia.bind(navigator.mediaDevices);
      navigator.mediaDevices.getUserMedia = constraints => {
        window.__playgroundMediaRequests.push(constraints);
        return getUserMedia(constraints).then(stream => {
          window.__playgroundMediaTracks.push(...stream.getTracks());
          return stream;
        });
      };
    }
  });
  const log = [], runtimeErrors = [];
  page.on('console', message => {
    const text = message.text();
    log.push({type: message.type(), text});
    // Worker exceptions are console messages, not Playwright pageerror events.
    if (/PARPAR:ERROR|Missing virtual method|\[playground\].*failed|(?:^|\s)(?:Uncaught|Exception in thread)/.test(text)) runtimeErrors.push(text);
  });
  page.on('pageerror', error => runtimeErrors.push(error.stack || error.message));
  try {
    await check(name + ' interaction', async () => {
      const target = new URL(url); target.searchParams.set('sample', slug);
      await page.goto(target.href, {waitUntil: 'domcontentloaded', timeout: 90000});
      await page.waitForFunction(() => window.cn1Started === true || !!document.querySelector('iframe[title="Codename One Playground"]'));
      const embedded = await page.locator('iframe[title="Codename One Playground"]').elementHandles();
      appFrame = embedded.length ? await embedded[0].contentFrame() : page.mainFrame();
      assert.ok(appFrame, 'Playground frame did not load');
      assert.equal(await appFrame.evaluate(() => window.devicePixelRatio), deviceScaleFactor,
        'Browser must use the requested pixel ratio');
      const deadline = Date.now() + 90000;
      while (!log.some(m => m.text.startsWith('[playground]')) && Date.now() < deadline) {
        await page.waitForTimeout(100);
      }
      assert.ok(log.some(m => m.text.startsWith('[playground] preview updated')), 'Sample did not initialize');
      const consent = page.getByRole('button', {name: 'Keep Crisp Disabled', exact: true});
      if (await consent.count()) await consent.click();
      const region = await preview(page, title);
      await exercise(page, region, name);
    });
    // Keep observing beyond initial compilation, including asynchronous callbacks.
    await page.waitForTimeout(1000);
    await check(name + ' runtime', () => assert.deepEqual(runtimeErrors, []));
  } finally {
    await page.screenshot({path: path.join(artifacts, name + '.png')}).catch(() => {});
    const controls = await (appFrame || page.mainFrame()).evaluate(() => [...document.querySelectorAll('[aria-label]')]
      .filter(e => !e.closest('[aria-hidden="true"]')).map(e => ({label: e.getAttribute('aria-label'),
        role: e.getAttribute('role'), bounds: e.getBoundingClientRect().toJSON()}))).catch(() => []);
    const gpu = await page.evaluate(() => {
      const gl = document.createElement('canvas').getContext('webgl');
      if (!gl) return {available: false};
      const info = gl.getExtension('WEBGL_debug_renderer_info');
      const result = {available: true, renderer: info ? gl.getParameter(info.UNMASKED_RENDERER_WEBGL) : gl.getParameter(gl.RENDERER)};
      gl.getExtension('WEBGL_lose_context')?.loseContext();
      return result;
    }).catch(error => ({error: error.message}));
    fs.writeFileSync(path.join(artifacts, name + '.json'), JSON.stringify({url, browser: browserName, version: browser.version(), viewport, deviceScaleFactor, gpu,
      headed: process.env.PLAYGROUND_HEADED === '1', softwareGl: process.env.PLAYGROUND_SOFTWARE_GL === '1', log, runtimeErrors, controls,
      checks: results.filter(r => r.name.startsWith(name))}, null, 2));
    await page.close();
  }
}
async function animatedScene(page, region, name, kind) {
  // Start sampling promptly, while the balls still bounce. Compare only scene
  // foreground pixels so editor carets, status text and other UI cannot pass this.
  const frames = [];
  for (let i = 0; i < 3; i++) {
    frames.push(await measure(page, region, kind, path.join(artifacts, name + '-frame-' + i + '.png')));
    await page.waitForTimeout(220);
  }
  for (const [behavior, ok] of Object.entries(sceneChecks(frames, region, kind))) {
    await check(name + ' ' + behavior, () => assert.ok(ok,
      JSON.stringify(frames.map(({mask, ...metrics}) => metrics))));
  }
}
async function cameraDemo(page, region, name) {
  const startBounds = await appFrame.getByRole('button', {name: 'Start Camera', exact: true}).boundingBox();
  assert.ok(startBounds && startBounds.y + startBounds.height <= viewport.height,
    'Start Camera is below the visible preview; user cannot request permission');
  assert.equal(await appFrame.evaluate(() => window.__playgroundMediaRequests.length), 0,
    'Camera access must start with the user action');
  // Chromium's synthetic device needs fake media UI even on a minimal page;
  // permission acceptance is automated, but media access still requires this
  // real user click. The request spy independently rejects microphone access.
  await clickControl(page, 'Start Camera');
  await cameraStatus("^Live preview running - tap 'Take Photo'$");
  const requests = await appFrame.evaluate(() => window.__playgroundMediaRequests);
  assert.equal(requests.length, 1, 'Start Camera should open one media stream');
  assert.equal(requests[0].audio, false, 'Still-photo demo must not require microphone permission');
  const video = appFrame.locator('video');
  await video.waitFor();
  await appFrame.waitForFunction(() => [...document.querySelectorAll('video')]
    .some(v => v.readyState >= 2 && v.videoWidth > 0 && v.currentTime > 0));
  const box = await video.boundingBox();
  assert.ok(box && box.width > 100 && box.height > 100 && box.x >= region.x - 12
    && box.x + box.width <= region.x + region.width + 12, 'Live video is outside the preview');
  const firstTime = await video.evaluate(v => v.currentTime);
  const first = await measure(page, box, 'camera');
  let motion = false;
  for (let i = 0; i < 8 && !motion; i++) {
    await page.waitForTimeout(400);
    const next = await measure(page, box, 'camera');
    motion = next.foreground > 1000 && changedPixels(first, next) > 100;
  }
  assert.ok(first.foreground > 1000, 'Live camera frame is not visible');
  assert.ok(motion && await video.evaluate(v => v.currentTime) > firstTime + .1, 'Camera preview is frozen');
  await page.screenshot({path: path.join(artifacts, name + '-live.png')});
  await clickControl(page, 'Take Photo');
  await appFrame.getByRole('button', {name: 'Close', exact: true}).waitFor();
  // A successful status alone must not hide a blank captured-photo dialog.
  const dialog = appFrame.locator('[role="dialog"], [role="group"][aria-label="Captured Photo"]').last();
  await dialog.waitFor();
  const photoBounds = await dialog.boundingBox();
  assert.ok(photoBounds && photoBounds.width > 100 && photoBounds.height > 100, 'Photo dialog has no bounds');
  const photo = await measure(page, photoBounds, 'camera');
  assert.ok(photo.foreground > 1000, 'Captured photo has no camera pixels');
  await page.screenshot({path: path.join(artifacts, name + '-photo.png')});
  await clickControl(page, 'Close');
  await appFrame.getByRole('button', {name: 'Close', exact: true}).waitFor({state: 'hidden'});
  await cameraStatus('^Captured [1-9][0-9]* x [1-9][0-9]*$');
}

async function selectSample(page, sample, title) {
  // The icon-only activity bar currently has no accessible names. Locate its
  // topmost left-edge button by live bounds, then click the real canvas.
  if (!await appFrame.getByRole('button', {name: 'Hello World', exact: true}).count()) {
    const buttons = appFrame.getByRole('button');
    const candidates = [];
    for (const button of await buttons.all()) {
      const box = await button.boundingBox();
      if (box && box.x < 20 && box.width > 20 && box.height > 20) candidates.push(box);
    }
    candidates.sort((a, b) => a.y - b.y);
    assert.ok(candidates.length, 'Samples activity button is missing');
    const box = candidates[0];
    await page.mouse.click(box.x + box.width / 2, box.y + box.height / 2);
  }
  const button = appFrame.getByRole('button', {name: sample, exact: true});
  await button.waitFor();
  // Scroll the CN1 sample panel, not its pointer-transparent ARIA overlay.
  const searchBox = await appFrame.locator('[role="textbox"][aria-readonly="false"]').boundingBox();
  assert.ok(searchBox, 'Sample search field is missing');
  const top = searchBox.y + searchBox.height;
  for (let i = 0; i < 12; i++) {
    const box = await button.boundingBox();
    assert.ok(box, 'Sample button has no bounds: ' + sample);
    if (box.y >= top && box.y + box.height <= viewport.height) break;
    await page.mouse.move(searchBox.x + searchBox.width / 2, Math.min(viewport.height - 50, top + 150));
    await page.mouse.wheel(0, box.y < top ? -240 : 240);
    await page.waitForTimeout(150);
  }
  await clickControl(page, sample);
  await (title === 'Hello World' ? appFrame.getByLabel('Hello, World!', {exact: true})
    : appFrame.getByRole('group', {name: title, exact: true})).waitFor({timeout: 30000});
  await page.waitForTimeout(500);
}
async function demoNavigation(page, region, name) {
  await clickControl(page, 'Start Camera');
  await cameraStatus("^Live preview running - tap 'Take Photo'$");
  const tracks = () => appFrame.evaluate(() => window.__playgroundMediaTracks.map(t => t.readyState));
  assert.deepEqual(await tracks(), ['live'], 'Expected one live video track');
  await selectSample(page, 'Hello World', 'Hello World');
  await check(name + ' camera released on exit', async () => {
    await appFrame.waitForFunction(() => window.__playgroundMediaTracks.every(t => t.readyState === 'ended'), null, {timeout: 3000});
    assert.deepEqual(await tracks(), ['ended']);
  });
  await selectSample(page, '3D / GPU', '3D / GPU');
  await animatedScene(page, await preview(page, '3D / GPU'), name + ' selected cube', 'cube');
  await selectSample(page, 'Hello World', 'Hello World');
  await selectSample(page, '3D / GPU', '3D / GPU');
  await animatedScene(page, await preview(page, '3D / GPU'), name + ' reopened cube', 'cube');
  await selectSample(page, 'Camera Capture', 'Camera');
  await clickControl(page, 'Start Camera');
  await cameraStatus("^Live preview running - tap 'Take Photo'$");
  assert.deepEqual(await tracks(), ['ended', 'live'], 'Camera must reopen with a new stream');
  await selectSample(page, 'Hello World', 'Hello World');
  await appFrame.waitForFunction(() => window.__playgroundMediaTracks.every(t => t.readyState === 'ended'), null, {timeout: 3000});
  assert.deepEqual(await tracks(), ['ended', 'ended'], 'Reopened camera must also stop on exit');
}

try {
  for (browserName of (process.env.PLAYGROUND_BROWSERS || 'chromium,firefox').split(',')) {
    assert.ok(['chromium', 'chrome', 'firefox'].includes(browserName), 'Unknown browser: ' + browserName);
    browser = await (browserName === 'firefox' ? firefox : chromium).launch(browserName !== 'firefox'
      ? {headless: process.env.PLAYGROUND_HEADED !== '1', ...(browserName === 'chrome' ? {channel: 'chrome'} : {}),
        args: ['--use-fake-device-for-media-stream', '--use-fake-ui-for-media-stream',
          ...(process.env.PLAYGROUND_SOFTWARE_GL === '1' ? ['--enable-unsafe-swiftshader'] : [])]}
      : {headless: process.env.PLAYGROUND_HEADED !== '1', firefoxUserPrefs: {'media.navigator.streams.fake': true,
        'media.navigator.permission.disabled': true,
        ...(process.env.PLAYGROUND_SOFTWARE_GL === '1' ? {'webgl.force-enabled': true} : {})}});
    for (deviceScaleFactor of (process.env.PLAYGROUND_DEVICE_SCALE_FACTORS || '1,2').split(',').map(Number)) {
      assert.ok(deviceScaleFactor > 0 && Number.isFinite(deviceScaleFactor), 'Invalid device scale factor');
      for (viewport of [{width: 1440, height: 900}, {width: 1280, height: 720}]) {
        const width = viewport.width;
        await run('bouncing-balls', 'Bouncing Balls', width, (p, r, n) => animatedScene(p, r, n, 'balls'));
        await run('3d-gpu', '3D / GPU', width, (p, r, n) => animatedScene(p, r, n, 'cube'));
        await run('camera-capture', 'Camera', width, cameraDemo);
        await run('camera-capture', 'Camera', width, demoNavigation, 'demo-navigation');
      }
    }
    await browser.close();
  }
} finally {
  if (browser) await browser.close();
  fs.writeFileSync(path.join(artifacts, 'demo-results.json'), JSON.stringify(results, null, 2));
}
if (!results.length) throw new Error('PLAYGROUND_DEMO_FILTER matched no cases');
const failed = results.filter(r => !r.ok).length;
console.log(`${failed} of ${results.length} demo checks failed. Artifacts: ${path.resolve(artifacts)}`);
process.exitCode = failed ? 1 : 0;
