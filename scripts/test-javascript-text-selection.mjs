// Run against scripts/build-javascript-selection-fixture.sh's output.
// NODE_PATH may point to an existing Playwright installation.
import fs from 'node:fs';
import http from 'node:http';
import path from 'node:path';
import assert from 'node:assert/strict';
import { createRequire } from 'node:module';
const { chromium, firefox, _android } = createRequire(import.meta.url)('playwright');
const root = path.resolve(process.argv[2]);
const artifacts = path.resolve(process.argv[3] || 'artifacts/javascript-text-selection');
fs.mkdirSync(artifacts, { recursive: true });
const types = { '.js': 'text/javascript', '.html': 'text/html', '.css': 'text/css' };
const server = http.createServer((req, res) => {
  const file = path.resolve(root, '.' + (req.url.split('?')[0] === '/' ? '/index.html' : req.url.split('?')[0]));
  if (!file.startsWith(root + path.sep)) { res.writeHead(403).end(); return; }
  fs.readFile(file, (error, data) => {
    res.writeHead(error ? 404 : 200, { 'Content-Type': types[path.extname(file)] || 'application/octet-stream' });
    res.end(error ? 'Not found' : data);
  });
});
await new Promise(resolve => server.listen(0, '127.0.0.1', resolve));
const port = server.address().port;
const accents = 'Caffè, perché, città, più, però.\nà è é ì ò ù — À È É Ì Ò Ù\nこんにちは | שלום | مرحبًا | 😀 | é';

async function exercise(context, name, host, mobileDevice = null) {
  const mobile = mobileDevice != null;
  if (name === 'chromium') await context.grantPermissions(['clipboard-read', 'clipboard-write']);
  const page = await context.newPage();
  page.setDefaultTimeout(15000);
  await page.addInitScript(() => {
    window.__cn1TextDraws = [];
    window.__cn1PointerEvents = [];
    for (const type of ['touchstart', 'touchend', 'touchcancel', 'mousedown', 'mouseup', 'click']) {
      document.addEventListener(type, e => window.__cn1PointerEvents.push({ type, target: e.target.tagName,
        x: e.clientX, y: e.clientY, touches: e.touches && e.touches.length }), true);
    }
    const fillText = CanvasRenderingContext2D.prototype.fillText;
    CanvasRenderingContext2D.prototype.fillText = function(text, ...args) {
      window.__cn1TextDraws.push(String(text));
      if (window.__cn1TextDraws.length > 2000) window.__cn1TextDraws.shift();
      return fillText.call(this, text, ...args);
    };
  });
  const errors = [], logs = [];
  async function hideAndroidKeyboard() {
    await page.evaluate(() => document.activeElement?.blur());
    await page.waitForTimeout(750);
  }
  page.on('console', m => { logs.push(m.type() + ': ' + m.text()); if (/Exception:|CAUGHT_RAW_JS_ERROR/.test(m.text())) errors.push(m.text()); });
  page.on('pageerror', e => errors.push(String(e)));
  async function exerciseReview() {
    async function openReview(kind) {
      await page.goto(`http://${host}:${port}/?review=${kind}`, { waitUntil: 'domcontentloaded' });
      await page.waitForFunction(() => !document.getElementById('cn1-splash'), null, { timeout: 60000 });
      await page.waitForTimeout(500);
    }
    async function clickButton(name) {
      if (mobile) await hideAndroidKeyboard();
      // These controls mutate fixture state. Use their accessible actions so
      // Android IME viewport resizing cannot redirect a coordinate click.
      const activated = await page.evaluate(name => {
        const button = [...document.querySelectorAll('#cn1-accessibility-tree [role="button"]')]
          .find(el => el.getAttribute('aria-label')?.toLowerCase() === name.toLowerCase());
        if (!button) return false;
        button.click();
        return true;
      }, name);
      assert.ok(activated, name + ' is available');
    }
    await openReview('selectionstate');
    async function selectionState(text, enabled) {
      await page.waitForFunction(({ text, enabled }) => {
        const span = [...document.querySelectorAll('#cn1-text-layer span')].find(el => el.textContent === text);
        return Boolean(span && getComputedStyle(span).pointerEvents === 'auto'
          && getComputedStyle(span).userSelect === 'text') === enabled;
      }, { text, enabled });
    }
    await selectionState('Toggle selection label', true);
    for (const enabled of [false, true]) {
      await selectionState('Toggle selection label', enabled);
    }
    for (const enabled of [false, true]) {
      await page.waitForFunction(enabled => {
        const editor = document.querySelector('.cn1-selection-editor[aria-label="toggleReadOnly"]');
        return Boolean(editor && getComputedStyle(editor).display !== 'none') === enabled;
      }, enabled);
    }
    for (const enabled of [false, true]) {
      await selectionState('Toggle selection label', enabled);
    }
    console.log('PASS', name, 'selection flags refresh static text without unrelated repaints');

    await openReview('editable');
    await page.locator('.cn1-selection-editor[aria-label="toggleEditable"]').waitFor({ state: 'visible' });
    for (const readOnly of [false, true, false]) {
      await page.waitForFunction(readOnly =>
        document.querySelector('.cn1-selection-editor[aria-label="toggleEditable"]')?.readOnly === readOnly, readOnly);
    }
    console.log('PASS', name, 'editability refreshes native controls on a static form');

    for (const label of ['focusReadOnly', 'focusDisabled']) {
      await openReview('focus');
      const editor = page.locator(`.cn1-selection-editor[aria-label="${label}"]`);
      await editor.waitFor({ state: 'visible' });
      await editor.focus();
      await page.waitForTimeout(300);
      assert.equal(await page.evaluate(() => document.activeElement?.getAttribute('aria-label')), label);
      await clickButton('Move focus');
      await page.waitForFunction(() => document.activeElement?.getAttribute('aria-label') === 'Focus destination');
    }
    console.log('PASS', name, 'readonly and disabled text release DOM focus to semantic targets');

    for (const handler of ['press', 'release', 'drag', 'long']) {
      await openReview('formpointer&handler=' + handler);
      const span = page.locator('#cn1-text-layer span').filter({ hasText: 'Form pointer label' }).first();
      await span.waitFor({ state: 'attached' });
      assert.equal(await span.evaluate(el => getComputedStyle(el).pointerEvents), 'none');
      assert.equal(await page.locator('.cn1-selection-editor[aria-label="formPointerArea"]').count(), 0);
      const bounds = await span.evaluate(el => el.getBoundingClientRect().toJSON());
      const x = bounds.x + 5, y = bounds.y + bounds.height / 2;
      assert.equal(await page.evaluate(({x, y}) => document.elementFromPoint(x, y)?.tagName, {x, y}), 'CANVAS');
      await page.mouse.move(x, y);
      await page.mouse.down();
      if (handler === 'drag') {
        await page.waitForTimeout(200);
        await page.mouse.move(x + 100, y, { steps: 10 });
        await page.waitForTimeout(200);
      }
      if (handler === 'long') await page.waitForTimeout(1500);
      await page.mouse.up();
      await page.waitForFunction(handler => document.body.innerText.includes('Form ' + handler + ' received'), handler);
    }
    console.log('PASS', name, 'form pointer callbacks retain canvas gestures');

    await openReview('snapshot');
    await page.locator('.cn1-selection-editor[aria-label="snapshotField"]').waitFor({ state: 'visible' });
    await page.locator('.cn1-selection-editor[aria-label="snapshotArea"]').waitFor({ state: 'visible' });
    await clickButton('Capture text images');
    await page.waitForFunction(() => document.body.innerText.includes('Snapshots contain text'));
    console.log('PASS', name, 'offscreen field and textarea snapshots contain text');

    await openReview('stylus');
    for (const text of ['Stylus listener label', 'Inherited stylus label']) {
      const span = page.locator('#cn1-text-layer span').filter({ hasText: text }).first();
      await span.waitFor({ state: 'attached' });
      assert.equal(await span.evaluate(el => getComputedStyle(el).pointerEvents), 'none');
      const bounds = await span.evaluate(el => el.getBoundingClientRect().toJSON());
      assert.equal(await page.evaluate(({ x, y }) => document.elementFromPoint(x, y)?.tagName,
        { x: bounds.x + 5, y: bounds.y + bounds.height / 2 }), 'CANVAS');
    }
    assert.equal(await page.locator('.cn1-selection-editor[aria-label="stylusArea"]').count(), 0);
    console.log('PASS', name, 'component and inherited stylus listeners retain canvas hit testing');

    await openReview('rendering');
    const hint = page.locator('.cn1-selection-editor[aria-label="reviewHint"]');
    await hint.waitFor({ state: 'visible' });
    async function waitForHint(text) {
      await page.waitForFunction(text => window.__cn1TextDraws.concat(
        [...document.querySelectorAll('#cn1-text-layer span')].map(el => el.textContent)).includes(text), text);
    }
    await waitForHint('Styled empty hint');
    await waitForHint('Multiline empty hint');
    await hint.fill('Entered value');
    await page.waitForTimeout(300);
    await page.evaluate(() => { window.__cn1TextDraws = []; });
    await clickButton('Clear hint field');
    await page.waitForFunction(() => document.querySelector('.cn1-selection-editor[aria-label="reviewHint"]').value === '');
    await waitForHint('Styled empty hint');
    for (const [align, expected] of [[1, 'right'], [3, 'left'], [4, 'center']]) {
      const rtl = page.locator(`.cn1-selection-editor[aria-label="rtl${align}"]`);
      assert.equal(await rtl.evaluate(el => getComputedStyle(el).textAlign), expected);
      assert.equal(await rtl.evaluate(el => getComputedStyle(el).direction), 'rtl');
    }
    console.log('PASS', name, 'empty field hints, hint restoration and RTL alignments');

    await openReview('exclusions');
    assert.equal(await page.locator('.cn1-selection-editor[aria-label="reviewLightweight"]').count(), 0);
    assert.equal(await page.locator('.cn1-selection-editor[aria-label="reviewContextCommands"]').count(), 0);
    for (const text of ['Lightweight value', 'Context listener label', 'Inherited context label']) {
      await waitForHint(text);
    }
    if (!mobile) {
      for (const [text, status] of [['Context listener label', 'Context received'],
        ['Inherited context label', 'Inherited context received']]) {
        const span = page.locator('#cn1-text-layer span').filter({ hasText: text }).first();
        await span.waitFor({ state: 'attached' });
        assert.equal(await span.evaluate(el => getComputedStyle(el).pointerEvents), 'none');
        const bounds = await span.evaluate(el => el.getBoundingClientRect().toJSON());
        await page.mouse.click(bounds.x + 10, bounds.y + bounds.height / 2, { button: 'right' });
        await page.waitForFunction(text => document.body.innerText.includes(text), status);
      }
    }
    console.log('PASS', name, 'lightweight input and component context menus retain canvas interaction');

    await openReview('metadata');
    const field = page.locator('.cn1-selection-editor[aria-label="reviewField"]');
    await field.waitFor({ state: 'visible' });
    assert.equal(await field.getAttribute('type'), 'email');
    const appearance = await field.evaluate(el => {
      const css = getComputedStyle(el);
      return { color: css.color, top: parseFloat(css.paddingTop), bottom: parseFloat(css.paddingBottom),
        height: el.clientHeight, font: parseFloat(css.fontSize) };
    });
    assert.match(appearance.color, /^rgba\(18, 52, 86, 0\.37/);
    assert.ok(appearance.top > appearance.height / 2, 'bottom-aligned text leaves space above it');
    await clickButton('Change constraints');
    await page.waitForFunction(() => document.querySelector('[aria-label="reviewField"].cn1-selection-editor')?.type === 'number');
    assert.equal(await field.getAttribute('inputmode'), 'numeric');
    assert.equal(await field.getAttribute('autocomplete'), 'off');
    assert.equal(await field.getAttribute('spellcheck'), 'false');
    await clickButton('Reset constraints');
    await page.waitForFunction(() => document.querySelector('[aria-label="reviewField"].cn1-selection-editor')?.type === 'text');
    assert.equal(await field.getAttribute('inputmode'), null);
    assert.equal(await field.getAttribute('autocomplete'), 'nickname');
    assert.equal(await field.getAttribute('autocapitalize'), 'words');
    assert.equal(await field.getAttribute('spellcheck'), 'true');
    if (!mobile) {
      await field.click();
      await page.keyboard.press('Escape', { delay: 100 });
      await page.waitForFunction(() => document.body.innerText.includes('Escape received'));
      await page.keyboard.press('F2', { delay: 100 });
      await page.waitForFunction(() => document.body.innerText.includes('F2 received'));
    }
    console.log('PASS', name, 'dynamic constraints, translucent foreground and bottom alignment' + (mobile ? '' : ', form shortcuts'));

    await openReview('scroll');
    const area = page.locator('.cn1-selection-editor[aria-label="reviewScroll"]');
    await area.waitFor({ state: 'visible' });
    const box = await area.evaluate(el => el.getBoundingClientRect().toJSON());
    if (mobile) {
      const cdp = await context.newCDPSession(page);
      await cdp.send('Input.synthesizeScrollGesture', { x: box.x + box.width / 2, y: box.y + box.height * 0.8,
        yDistance: -40, speed: 500, gestureSourceType: 'touch' });
      await cdp.detach();
    } else {
      await page.mouse.move(box.x + 30, box.y + 25);
      await page.mouse.wheel(0, 65);
    }
    await page.waitForFunction(() => document.querySelector('.cn1-selection-editor[aria-label="reviewScroll"]').scrollTop > 0);
    assert.ok(Math.abs((await area.evaluate(el => el.getBoundingClientRect().toJSON())).y - box.y) < 2, 'the text viewport stays fixed while its content scrolls');
    await page.waitForFunction(() => /Scroll Y [1-9]/.test(document.body.innerText));
    if (!mobile) {
      await clickButton('Reset scroll');
      await page.waitForFunction(() => document.querySelector('.cn1-selection-editor[aria-label="reviewScroll"]').scrollTop === 0);
      // Browser caret/selection scrolling is reflected back into the component too.
      await area.evaluate(el => { el.scrollTop = 85; el.dispatchEvent(new Event('scroll')); });
      await page.waitForFunction(() => /Scroll Y [1-9]/.test(document.body.innerText));
    }
    console.log('PASS', name, mobile ? 'fixed-height textarea touch scrolling' : 'fixed-height textarea scroll synchronization in both directions');

    await openReview('labels');
    for (const text of ['Custom pointer label', 'Draggable label', '50%']) {
      const span = page.locator('#cn1-text-layer span').filter({ hasText: text }).first();
      await span.waitFor({ state: 'attached' });
      assert.equal(await span.evaluate(el => getComputedStyle(el).pointerEvents), 'none', text + ' retains canvas gestures');
    }
    const plain = page.locator('#cn1-text-layer span').filter({ hasText: 'Plain selectable label' }).first();
    assert.equal(await plain.getAttribute('data-cn1-native-selection'), 'true');
    const item = page.locator('.cn1-selection-editor[aria-label="horizontalText"]');
    const hb = await item.evaluate(el => el.getBoundingClientRect().toJSON());
    if (mobile) {
      const cdp = await context.newCDPSession(page);
      await cdp.send('Input.synthesizeScrollGesture', { x: hb.x + Math.min(80, hb.width / 3), y: hb.y + hb.height / 2,
        xDistance: -70, speed: 500, gestureSourceType: 'touch' });
      await cdp.detach();
    } else {
      await page.mouse.move(hb.x + 30, hb.y + hb.height / 2);
      await page.mouse.wheel(70, 0);
    }
    await page.waitForFunction(x => {
      const span = document.querySelector('.cn1-selection-editor[aria-label="horizontalText"]');
      return !span || span.getBoundingClientRect().x < x - 10;
    }, hb.x);
    const custom = page.locator('#cn1-text-layer span').filter({ hasText: 'Custom pointer label' }).first();
    const cb = await custom.evaluate(el => el.getBoundingClientRect().toJSON());
    await page.mouse.click(cb.x + 15, cb.y + cb.height / 2);
    assert.equal(await page.evaluate(() => window.__cn1PointerEvents.filter(e => e.type === 'mousedown').pop()?.target),
      'CANVAS', 'custom label pointer input reaches the canvas');
    console.log('PASS', name, 'interactive labels and horizontal scrolling over selectable text');
    assert.deepEqual(errors, [], 'no browser/worker exceptions in review regressions');
  }
  try {
    if (process.env.CN1_JS_REVIEW_ONLY === 'true') { await exerciseReview(); return; }
    await page.goto(`http://${host}:${port}/`, { waitUntil: 'domcontentloaded' });
    await page.bringToFront();
    await page.waitForFunction(() => !document.getElementById('cn1-splash'), null, { timeout: 60000 });
    const notes = page.locator('.cn1-selection-editor[aria-label="selectionNotes"]');
    const title = page.locator('.cn1-selection-editor[aria-label="selectionTitle"]');
    const readOnly = page.locator('.cn1-selection-editor[aria-label="selectionReadOnly"]');
    await notes.waitFor({ state: 'visible' });
    assert.equal(await page.locator('.cn1-selection-editor').count(), 3);
    console.log('PASS', name, 'fresh-session startup and default-font metrics');
    async function clickText(text) {
      if (mobile) await hideAndroidKeyboard();
      // Scrolling can correctly demote painted text back to the canvas. Semantic
      // button bounds remain available even when there is no visible DOM span.
      const pattern = new RegExp('^' + text + '$', 'i');
      const button = page.getByRole('button', { name: pattern }).first();
      const span = await button.count() ? button : page.locator('#cn1-text-layer span').filter({ hasText: pattern }).first();
      const box = await span.evaluate(el => el.getBoundingClientRect().toJSON());
      assert.ok(box, 'text is rendered: ' + text);
      logs.push('click ' + text + ': ' + await page.evaluate(({ x, y }) => document.elementFromPoint(x, y)?.outerHTML,
        { x: box.x + box.width / 2, y: box.y + box.height / 2 }));
      await page.mouse.click(box.x + box.width / 2, box.y + box.height / 2);
    }
    await notes.fill(accents);
    await title.click();
    await page.waitForTimeout(300);
    assert.equal(await notes.inputValue(), accents);
    await notes.click();
    await page.waitForTimeout(300);
    assert.equal(await notes.evaluate(el => document.activeElement === el), true, 'semantic focus must not steal the native editor');
    assert.equal(await notes.inputValue(), accents);
    console.log('PASS', name, 'Unicode and editing survive focus changes');
    if (!mobile) {
      await title.click();
      await page.keyboard.press('Tab');
      await page.waitForTimeout(500);
      assert.equal(await notes.evaluate(el => document.activeElement === el), true, 'Tab moves to the next CN1 field');
      await page.keyboard.press('Shift+Tab');
      await page.waitForTimeout(300);
      assert.equal(await title.evaluate(el => document.activeElement === el), true, 'Shift+Tab moves to the previous CN1 field');
      console.log('PASS', name, 'keyboard field traversal');
      await notes.click();
      await notes.dblclick({ position: { x: 25, y: 12 } });
      assert.ok(await notes.evaluate(el => el.selectionEnd > el.selectionStart), 'double-click selects text');
      const before = await notes.evaluate(el => [el.selectionStart, el.selectionEnd]);
      await page.waitForTimeout(600);
      assert.deepEqual(await notes.evaluate(el => [el.selectionStart, el.selectionEnd]), before, 'repaints preserve selection');
      await notes.click({ button: 'right', position: { x: 25, y: 12 } });
      assert.equal(await page.locator('#cn1-text-layer span').filter({ hasText: /^Select All$/ }).count(), 0, 'no competing CN1 context menu');
      await page.keyboard.press('Escape');
      console.log('PASS', name, 'word selection, repaint stability and one context menu');
      await readOnly.click({ clickCount: 3, position: { x: 30, y: 12 } });
      assert.equal(await readOnly.evaluate(el => el.value.slice(el.selectionStart, el.selectionEnd).trim()),
        (await readOnly.inputValue()).split('\n')[0], 'triple-click selects the logical paragraph');
      await readOnly.click();
      await page.keyboard.press(process.platform === 'darwin' ? 'Meta+a' : 'Control+a');
      assert.equal(await readOnly.evaluate(el => el.value.slice(el.selectionStart, el.selectionEnd)), await readOnly.inputValue());
      if (name === 'chromium') {
        await page.keyboard.press(process.platform === 'darwin' ? 'Meta+c' : 'Control+c');
        assert.equal(await page.evaluate(() => navigator.clipboard.readText()), await readOnly.inputValue());
      }
      console.log('PASS', name, 'readonly select-all preserves logical paragraphs');
      const box = await readOnly.evaluate(el => el.getBoundingClientRect().toJSON());
      await page.mouse.move(box.x + 30, box.y + 15);
      await page.mouse.wheel(0, 160);
      await page.waitForTimeout(700);
      assert.ok((await readOnly.evaluate(el => el.getBoundingClientRect().toJSON())).y < box.y - 10, 'wheel over native text scrolls the CN1 form');
      await page.mouse.wheel(0, -2000);
      await page.waitForTimeout(700);
      console.log('PASS', name, 'scrolling over native text');
    } else {
      await title.click();
      await hideAndroidKeyboard();
      const cdp = await context.newCDPSession(page);
      const box = await readOnly.evaluate(el => el.getBoundingClientRect().toJSON());
      const point = { x: box.x + 30, y: box.y + 15 };
      // Raw dispatchTouchEvent produces a caret, even on a plain textarea.
      // synthesizeTapGesture also runs Android's long-press recognizer.
      await cdp.send('Input.synthesizeTapGesture', { ...point, duration: 1000, gestureSourceType: 'touch' });
      assert.ok(await readOnly.evaluate(el => el.selectionEnd > el.selectionStart), 'long press selects a word');
      console.log('PASS', name, 'native long-press selection');
      await readOnly.evaluate(el => { el.setSelectionRange(0, 0); el.blur(); });
      await page.waitForTimeout(300);
      await cdp.send('Input.synthesizeScrollGesture', { x: box.x + 80, y: box.y + 80,
        yDistance: -100, speed: 500, gestureSourceType: 'touch' });
      await page.waitForTimeout(700);
      const shiftedY = await page.evaluate(() => {
        const el = document.querySelector('.cn1-selection-editor[aria-label="selectionReadOnly"]');
        return el && getComputedStyle(el).display !== 'none' ? el.getBoundingClientRect().y : null;
      });
      assert.ok(shiftedY === null || shiftedY < box.y - 10, 'swipe over text scrolls the form');
      // CN1 performs its own kinetic scrolling after the browser gesture ends.
      // A tap during that motion intentionally stops scrolling instead of firing.
      // Offscreen native text overlays are released during scrolling. The
      // semantic button remains available even when the textarea leaves view.
      const scrollAnchor = page.getByRole('button', { name: /^Run action$/i }).first();
      let previousY = (await scrollAnchor.evaluate(el => el.getBoundingClientRect().toJSON())).y, stableSince = Date.now();
      const deadline = Date.now() + 8000;
      while (Date.now() < deadline && Date.now() - stableSince < 1000) {
        await page.waitForTimeout(250);
        const currentY = (await scrollAnchor.evaluate(el => el.getBoundingClientRect().toJSON())).y;
        if (Math.abs(currentY - previousY) > 0.5) stableSince = Date.now();
        previousY = currentY;
      }
      assert.ok(Date.now() - stableSince >= 1000, 'kinetic scrolling settles');
      await cdp.detach();
      console.log('PASS', name, 'touch scrolling over native text');
    }
    await clickText('Run action');
    await page.waitForFunction(() => document.body.innerText.includes('Action fired'));
    assert.equal(await readOnly.isVisible(), true, 'readonly text is visible before the dialog');
    await clickText('Open dialog');
    await page.waitForFunction(() => document.body.innerText.includes('Selection dialog'));
    await page.waitForTimeout(500);
    assert.equal(await readOnly.isVisible(), false, 'modal dialog hides background editor');
    await clickText('OK');
    await readOnly.waitFor({ state: 'visible' });
    if (mobile && !await notes.isVisible()) {
      // A restored form may recreate its editors. Offscreen controls acquire
      // their value when promoted back into view, so scroll before inspecting it.
      const box = await readOnly.evaluate(el => el.getBoundingClientRect().toJSON());
      const cdp = await context.newCDPSession(page);
      await cdp.send('Input.synthesizeScrollGesture', { x: box.x + 80, y: box.y + 80,
        yDistance: 180, speed: 400, gestureSourceType: 'touch' });
      await cdp.detach();
    }
    await notes.waitFor({ state: 'visible' });
    assert.equal(await notes.inputValue(), accents);
    console.log('PASS', name, 'button actions, dialog occlusion and form restoration');
    await page.screenshot({ path: path.join(artifacts, name + '.png') });
    if (!mobile) {
      await page.goto(`http://${host}:${port}/?selection=off`, { waitUntil: 'domcontentloaded' });
      await page.waitForFunction(() => !document.getElementById('cn1-splash'), null, { timeout: 60000 });
      await page.waitForTimeout(500);
      assert.equal(await page.locator('.cn1-selection-editor').count(), 0);
      const text = page.locator('#cn1-text-layer span').filter({ hasText: /^Caffè, perché/ }).first();
      const box = await text.evaluate(el => el.getBoundingClientRect().toJSON());
      await page.mouse.click(box.x + 30, box.y + box.height / 2);
      const editor = page.locator('textarea.cn1-edit-string:visible');
      await editor.waitFor();
      await editor.fill(accents);
      await page.evaluate(() => { window.__cn1TextDraws = []; });
      // Use the semantic field's bounds; it may currently render on the canvas.
      const titleBounds = await page.locator('#cn1-accessibility-tree [role="textbox"]').first().evaluate(el => el.getBoundingClientRect().toJSON());
      await page.mouse.click(titleBounds.x + 20, titleBounds.y + titleBounds.height / 2);
      await page.waitForFunction(() => {
        const rendered = window.__cn1TextDraws.concat([...document.querySelectorAll('#cn1-text-layer span')]
          .map(el => el.textContent)).join(' ');
        return rendered.includes('Caffè') && rendered.includes('più') && rendered.includes('À È É');
      });
      console.log('PASS', name, 'hint disabled: legacy editor and unfocused Unicode rendering');
    }
    await exerciseReview();
    assert.deepEqual(errors, [], 'no browser/worker exceptions');
  } finally {
    if (mobile) await mobileDevice.screenshot({ path: path.join(artifacts, name + '-device.png') }).catch(() => {});
    await page.screenshot({ path: path.join(artifacts, name + '-last.png') }).catch(() => {});
    logs.push(JSON.stringify(await page.evaluate(() => ({ active: document.activeElement && document.activeElement.outerHTML,
      editors: [...document.querySelectorAll('.cn1-selection-editor')].map(el => ({ label: el.getAttribute('aria-label'),
        start: el.selectionStart, end: el.selectionEnd, rect: el.getBoundingClientRect().toJSON() })),
      textRuns: [...document.querySelectorAll('#cn1-text-layer span')].map(el => ({ text: el.textContent, css: el.style.cssText })),
      events: window.__cn1PointerEvents,
      workerLog: (window.__parparMessages || []).filter(m => m.type === 'log').map(m => m.message),
      text: document.body.innerText.slice(0, 1000) })).catch(() => null)));
    fs.writeFileSync(path.join(artifacts, name + '.log'), logs.join('\n') + '\n' + errors.join('\n'));
    await page.close();
  }
}

try {
  if (process.argv.includes('--android')) {
    const devices = await _android.devices();
    const device = devices.find(d => d.serial() === (process.env.CN1_ANDROID_SERIAL || 'emulator-5584'));
    assert.ok(device, 'Android test device is connected');
    try {
      // The server uses a fresh origin each run, without prior app storage or caches.
      const context = await device.launchBrowser({ args: ['--disable-notifications'] });
      try { await exercise(context, 'android-chrome', '10.0.2.2', device); }
      finally { await context.close(); }
    } finally { await device.close(); }
  } else {
    for (const name of (process.env.CN1_JS_BROWSERS || 'chromium,firefox').split(',')) {
      const browser = name === 'firefox'
        ? await firefox.launch(process.env.CN1_JS_FIREFOX_EXECUTABLE ? { executablePath: process.env.CN1_JS_FIREFOX_EXECUTABLE } : {})
        : await chromium.launch(process.env.CN1_JS_CHROME_CHANNEL ? { channel: process.env.CN1_JS_CHROME_CHANNEL } : {});
      try {
        const context = await browser.newContext({ viewport: { width: 900, height: 800 } });
        try { await exercise(context, name, '127.0.0.1'); } finally { await context.close(); }
      } finally { await browser.close(); }
    }
  }
} finally { server.close(); }
