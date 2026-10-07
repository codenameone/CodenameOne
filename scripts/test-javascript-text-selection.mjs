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
    await openReview('nativepolicy');
    const policy = page.locator('.cn1-selection-editor[name="nativePolicy"]');
    await policy.waitFor({state: 'visible'});
    await clickButton('Toggle native input');
    await policy.waitFor({state: 'hidden'});
    await clickButton('Toggle native input');
    await policy.waitFor({state: 'visible'});
    console.log('PASS', name, 'native input policy refreshes static editors');

    await openReview('custompainter');
    await clickButton('Configure painter');
    await page.waitForFunction(() => document.body.innerText.includes('Painter configured'));
    await page.locator('.cn1-selection-editor[name="painterCovered"]').waitFor({state: 'hidden'});
    console.log('PASS', name, 'custom background painter occludes native text');

    await openReview('readback');
    const readback = page.locator('.cn1-selection-editor[name="readbackField"]');
    await readback.waitFor({state: 'visible'});
    await page.evaluate(() => { window.__cn1TextDraws = []; });
    await clickButton('Capture display');
    await page.waitForFunction(() => document.body.innerText.includes('Display captured'));
    const capturedDraws = await page.evaluate(() => window.__cn1TextDraws);
    assert.ok(capturedDraws.includes('Readback field value'), 'display capture rasterizes field text');
    assert.ok(capturedDraws.includes('Readback area value'), 'display capture rasterizes readonly area text');
    await readback.fill('Edited after capture');
    await page.waitForFunction(() => window.__cn1TextDraws.includes('Edited after capture'));
    console.log('PASS', name, 'display screenshot rasterizes persistent editors and editing continues');

    await openReview('autocompletemutation');
    for (const token of ['off', 'one-time-code', 'clear']) {
      await clickButton('Autocomplete ' + token);
      await page.waitForFunction(expected => document.querySelector('.cn1-selection-editor[name="autocompleteMutation"]').getAttribute('autocomplete') === expected,
        token === 'clear' ? 'on' : token);
    }
    console.log('PASS', name, 'autocomplete client-property changes refresh a static native editor');

    await openReview('blocklead');
    // Lead initialization makes the container focusable by default. Exercise a
    // non-focusable lead container so blockLead is the deciding eligibility flag.
    await clickButton('Configure lead');
    for (const blocked of [true, false, true, false]) {
      await clickButton(blocked ? 'Block lead' : 'Unblock lead');
      await page.waitForFunction(blocked => [...document.querySelectorAll('#cn1-text-layer span')].some(el =>
        el.textContent === 'Block lead text' && (getComputedStyle(el).pointerEvents === 'auto') === blocked), blocked);
    }
    await page.locator('#cn1-text-layer span').filter({hasText: /^Block lead text$/}).click({force: true});
    await page.waitForFunction(() => document.body.innerText.includes('Lead fired'));
    console.log('PASS', name, 'block-lead mutations restore the lead action gesture');

    await openReview('stopcallback');
    const completionFirst = page.locator('.cn1-selection-editor[name="completionFirst"]');
    await completionFirst.click();
    await page.waitForTimeout(300);
    // The final value deliberately has no input event: stop must capture it from
    // the DOM before invoking a callback that reads the model and edits a new field.
    await completionFirst.evaluate(el => { el.value = 'Pending final value'; });
    await clickButton('Stop and continue');
    await page.waitForFunction(() => document.body.innerText.includes('Stopped Pending final value editing=false'));
    await page.waitForFunction(() => document.activeElement?.getAttribute('name') === 'completionSecond');
    assert.equal(await completionFirst.inputValue(), 'Pending final value');
    console.log('PASS', name, 'stop callbacks observe committed text and released ownership before starting another editor');

    await openReview('actionmutations');
    const actionArea = page.locator('.cn1-selection-editor[name="actionMutationArea"]');
    await actionArea.waitFor({state: 'visible'});
    for (const add of [true, false, true, false]) {
      await clickButton(add ? 'Add action listener' : 'Remove action listener');
      await actionArea.waitFor({state: add ? 'hidden' : 'visible'});
    }
    console.log('PASS', name, 'action-listener mutations refresh native eligibility on a static form');

    await openReview('associatedlabels');
    for (const associated of [true, false, true, false]) {
      await clickButton(associated ? 'Associate label' : 'Disassociate label');
      await page.waitForFunction(label => document.querySelector('.cn1-selection-editor[name="associatedField"]')?.getAttribute('aria-label') === label,
        associated ? 'Associated field label' : 'associatedField');
    }
    console.log('PASS', name, 'label association alone refreshes native accessible names');

    if (!mobile) {
      for (const editor of [false, true]) {
        await openReview('nativehover');
        const target = editor ? page.locator('.cn1-selection-editor[name="hoverNativeArea"]')
          : page.locator('#cn1-text-layer span').filter({hasText: /^Hover native label$/});
        assert.equal(await target.evaluate(el => getComputedStyle(el).pointerEvents), 'auto');
        await page.mouse.move(0, 0);
        await target.hover();
        await page.waitForFunction(tip => document.body.innerText.includes(tip), editor ? 'Native editor tooltip' : 'Native label tooltip');
      }
      console.log('PASS', name, 'unpressed native label and editor hover opens framework tooltips');
    }

    await openReview('accessiblelabels');
    const namedEditor = page.locator('.cn1-selection-editor[name="internalFieldName"]');
    for (const [action, label] of [[null, 'Configured accessible label'], ['Set semantic label', 'Updated semantic label'],
      ['Use associated label', 'Associated label'], ['Clear accessible label', 'internalFieldName']]) {
      if (action) await clickButton(action);
      await page.waitForFunction(label => document.querySelector('.cn1-selection-editor[name="internalFieldName"]')?.getAttribute('aria-label') === label, label);
      assert.equal(await namedEditor.inputValue(), 'Field content');
    }
    console.log('PASS', name, 'native editor accessible names follow explicit and semantic labels and associated labels');

    await openReview('defaultselection');
    for (const enabled of [false, true, false, true]) {
      await clickButton(enabled ? 'Enable default selection' : 'Disable default selection');
      await page.waitForFunction(enabled => [...document.querySelectorAll('#cn1-text-layer span')]
        .some(el => el.textContent === 'Default selection label' && (getComputedStyle(el).pointerEvents === 'auto') === enabled), enabled);
      await page.locator('.cn1-selection-editor[name="defaultSelectionArea"]').waitFor({state: enabled ? 'visible' : 'hidden'});
      for (const [text, expected] of [['Explicit selection on', 'auto'], ['Explicit selection off', 'none']]) {
        assert.equal(await page.locator('#cn1-text-layer span').filter({hasText: new RegExp('^' + text + '$')})
          .evaluate(el => getComputedStyle(el).pointerEvents), expected);
      }
    }
    console.log('PASS', name, 'default selection policy refreshes static labels and readonly editors while preserving overrides');

    if (!mobile) for (const rtl of [false, true]) for (const drag of [true, false]) {
      await openReview('scroll&interactive=true&rtl=' + rtl);
      await clickButton('Configure scrollbar');
      const scrolling = page.locator('.cn1-selection-editor[name="reviewScroll"]');
      await scrolling.waitFor({state: 'visible'});
      await page.waitForFunction(() => document.querySelector('.cn1-selection-editor[name="reviewScroll"]').scrollTop === 0);
      await page.waitForFunction(rtl => {
        const css = getComputedStyle(document.querySelector('.cn1-selection-editor[name="reviewScroll"]'));
        const inset = css.clipPath.match(/[\d.]+/g)?.map(Number);
        return inset && (rtl ? inset[3] : inset[1]) > 0;
      }, rtl);
      assert.ok(await scrolling.evaluate((el, rtl) => {
        const css = getComputedStyle(el), inset = css.clipPath.match(/[\d.]+/g).map(Number);
        return parseFloat(rtl ? css.paddingLeft : css.paddingRight) > (rtl ? inset[3] : inset[1]);
      }, rtl), 'native layout reserves the scrollbar plus half-character row gap');
      const gutter = await scrolling.evaluate((el, rtl) => {
        const box = el.getBoundingClientRect(), inset = getComputedStyle(el).clipPath.match(/[\d.]+/g).map(Number);
        return {x: rtl ? box.left + inset[3] / 2 : box.right - inset[1] / 2, top: box.top, bottom: box.bottom};
      }, rtl);
      assert.equal(await page.evaluate(g => document.elementFromPoint(g.x, g.top + 10).tagName, gutter), 'CANVAS');
      if (drag) {
        await page.mouse.move(gutter.x, gutter.top + 10);
        await page.mouse.down();
        await page.waitForFunction(() => document.body.innerText.includes('Thumb grabbed'));
        await page.mouse.move(gutter.x, gutter.bottom - 25, {steps: 10});
        await page.mouse.up();
      } else {
        await page.mouse.click(gutter.x, gutter.bottom - 10);
      }
      await page.waitForFunction(() => /Scroll Y [1-9]/.test(document.body.innerText));
      await page.waitForFunction(() => document.querySelector('.cn1-selection-editor[name="reviewScroll"]').scrollTop > 0);
    }
    console.log('PASS', name, 'interactive scrollbar thumb and track stay reachable in LTR and RTL');

    await openReview('dynamicdrag');
    const dragArea = page.locator('.cn1-selection-editor[aria-label="dynamicDragArea"]');
    await dragArea.waitFor({state: 'visible'});
    for (const enabled of [true, false]) {
      await clickButton(enabled ? 'Enable dragging' : 'Disable dragging');
      await dragArea.waitFor({state: enabled ? 'hidden' : 'visible'});
      await page.waitForFunction(enabled => [...document.querySelectorAll('#cn1-text-layer span')]
        .some(el => el.textContent === 'Dynamic drag label' && (getComputedStyle(el).pointerEvents === 'none') === enabled), enabled);
    }
    console.log('PASS', name, 'draggable mutations refresh native labels and editors on a static form');

    for (const style of ['font', 'decoration', 'opacity']) {
      await openReview('selectedstyles&style=' + style);
      // Deferred theme initialization can reset decoration before the first frame.
      await clickButton('Apply selected style');
      await page.locator('.cn1-selection-editor:visible').waitFor({state: 'hidden'});
      await clickButton('Edit selected style');
      const legacy = page.locator('.cn1-edit-string:visible');
      await legacy.waitFor();
      await page.waitForFunction(() => document.activeElement?.classList.contains('cn1-edit-string')
        && document.activeElement.value === 'ABBA');
      await legacy.fill('ABAB');
      await page.waitForTimeout(350);
      assert.equal(await legacy.evaluate(el => document.activeElement === el), true, 'selected ' + style + ' retains editor focus');
      assert.equal(await legacy.inputValue(), 'ABAB');
      assert.equal(await page.locator('.cn1-selection-editor:visible').count(), 0);
    }
    console.log('PASS', name, 'unsupported selected fonts, decorations and opacity keep stable legacy editing');

    await openReview('legacysession');
    const persistentSession = page.locator('.cn1-selection-editor[aria-label="legacySessionArea"]');
    assert.equal(await persistentSession.isVisible(), false);
    await clickButton('Start legacy');
    const legacySession = page.locator('.cn1-edit-string:visible');
    await legacySession.waitFor();
    await page.waitForFunction(() => document.activeElement?.classList.contains('cn1-edit-string')
      && document.activeElement.value === 'Original model');
    await legacySession.fill('Legacy preserved');
    await page.evaluate(() => { window.__cn1TextDraws = []; });
    await clickButton('Repaint legacy');
    await page.waitForFunction(() => document.body.innerText.includes('Legacy repaint completed'));
    await page.waitForTimeout(300);
    assert.equal(await page.evaluate(() => window.__cn1TextDraws.some(text => /Original model|Legacy preserved/.test(text))
      || [...document.querySelectorAll('#cn1-text-layer span')].some(el => /Original model|Legacy preserved/.test(el.textContent))), false,
      'the canvas must not paint a second copy underneath the active legacy editor');
    await clickButton('Remove cover');
    await page.waitForFunction(() => document.body.innerText.includes('Cover removed'));
    // Exercise periodic layout updates and let the legacy startup latch expire.
    await page.waitForTimeout(3200);
    assert.equal(await persistentSession.isVisible(), false, 'uncovering the field cannot promote a competing editor');
    assert.equal(await legacySession.evaluate(el => document.activeElement === el), true);
    await legacySession.fill('Legacy preserved tail');
    const completedLegacyValue = await legacySession.inputValue();
    assert.equal(completedLegacyValue, 'Legacy preserved tail');
    await clickButton('Finish legacy');
    await persistentSession.waitFor({state: 'visible'});
    await page.waitForFunction(value => document.querySelector('.cn1-selection-editor[aria-label="legacySessionArea"]').value === value, completedLegacyValue);
    console.log('PASS', name, 'legacy editing retains ownership through uncovering and commits before persistent promotion');

    for (const disabled of [false, true]) {
      await openReview('readonlykeys&disabled=' + disabled);
      const readonlyKeys = page.locator('.cn1-selection-editor[aria-label="readonlyKeys"]');
      await readonlyKeys.click({force: disabled});
      await readonlyKeys.press('k');
      await page.waitForFunction(() => document.body.innerText.includes('K received'));
      assert.equal(await readonlyKeys.inputValue(), 'Readonly shortcut text');
      await readonlyKeys.press('ControlOrMeta+a');
      assert.deepEqual(await readonlyKeys.evaluate(el => [el.selectionStart, el.selectionEnd]), [0, 'Readonly shortcut text'.length]);
    }
    console.log('PASS', name, 'readonly and disabled native controls forward printable shortcuts and retain select-all');

    for (const enabled of [false, true]) {
      await openReview('optingate&selection=' + (enabled ? 'on' : 'off'));
      const explicit = page.locator('#cn1-text-layer span').filter({hasText: /^Explicit framework selection$/});
      assert.equal(await explicit.evaluate(el => getComputedStyle(el).pointerEvents), enabled ? 'auto' : 'none');
    }
    console.log('PASS', name, 'framework label selection only becomes browser-native with the build hint');

    await openReview('dynamicconstraints');
    const dynamic = page.locator('.cn1-selection-editor[aria-label="dynamicArea"]');
    await dynamic.waitFor({state: 'visible'});
    await clickButton('Enable password');
    await dynamic.waitFor({state: 'hidden'});
    await clickButton('Reset constraints');
    await dynamic.waitFor({state: 'visible'});
    await clickButton('Add done listener');
    await dynamic.waitFor({state: 'hidden'});
    await clickButton('Reset constraints');
    await dynamic.waitFor({state: 'visible'});
    console.log('PASS', name, 'password and done listener changes immediately re-evaluate editor eligibility');

    for (const action of ['Grab pointer', 'Focusable parent', 'Draggable parent']) {
      await openReview('ancestorownership');
      const ownedLabel = page.locator('#cn1-text-layer span').filter({hasText: /^Ancestor owned label$/});
      for (const ownsPointer of [true, false, true]) {
        await clickButton(ownsPointer ? action : 'Release pointer');
        await page.locator('.cn1-selection-editor[name="ancestorOwnedArea"]').waitFor({state: ownsPointer ? 'hidden' : 'visible'});
        await page.waitForFunction(owns => [...document.querySelectorAll('#cn1-text-layer span')]
          .some(el => el.textContent === 'Ancestor owned label' && (getComputedStyle(el).pointerEvents === 'none') === owns), ownsPointer);
      }
      // Click after testing the setter transitions: canvas selection itself may
      // keep the text on the canvas until that selection has been dismissed.
      await page.evaluate(() => { window.__cn1PointerEvents.length = 0; });
      await ownedLabel.click({force: true});
      assert.ok(await page.evaluate(() => window.__cn1PointerEvents.some(e => e.type === 'mousedown' && e.target === 'CANVAS')),
        'pointer-owning ancestor leaves glyph hits on the canvas');
    }
    console.log('PASS', name, 'intermediate ancestors retain pointer ownership after dynamic changes');

    await openReview('keyboardpadding');
    const padding = page.locator('.cn1-selection-editor[aria-label="paddingArea"]');
    await padding.waitFor({state: 'visible'});
    await clickButton('Enable keyboard padding');
    await padding.waitFor({state: 'hidden'});
    await clickButton('Disable keyboard padding');
    await padding.waitFor({state: 'visible'});
    console.log('PASS', name, 'keyboard padding mode selects the established layout-aware editor');

    await openReview('initiatingkeys');
    const initiating = page.locator('.cn1-selection-editor[aria-label="initiatingArea"]');
    await initiating.waitFor({state: 'visible'});
    await clickButton('Type queued keys');
    await page.waitForFunction(() => document.body.innerText.includes('Typed ABC'));
    assert.equal(await initiating.inputValue(), 'ABC', 'queued canvas keys survive asynchronous focus and respect max size');
    console.log('PASS', name, 'initiating keys accumulate before native focus');

    await openReview('elevated');
    assert.equal(await page.locator('.cn1-selection-editor[aria-label="elevatedArea"]:visible').count(), 0);
    console.log('PASS', name, 'elevated descendants in earlier branches exclude native editor coverage');

    await openReview('runorder');
    const order = () => page.evaluate(() => [...document.querySelectorAll('#cn1-text-layer span')]
      .map(el => el.textContent).filter(t => t.startsWith('Order ')));
    assert.deepEqual(await order(), ['Order alpha', 'Order beta']);
    await clickButton('Reverse labels');
    await page.waitForFunction(() => [...document.querySelectorAll('#cn1-text-layer span')]
      .filter(el => el.textContent.startsWith('Order '))[0]?.textContent === 'Order beta');
    await page.evaluate(() => {
      const spans = [...document.querySelectorAll('#cn1-text-layer span')].filter(el => el.textContent.startsWith('Order '));
      const range = document.createRange(); range.setStart(spans[0].firstChild, 0); range.setEnd(spans[1].firstChild, spans[1].textContent.length);
      getSelection().removeAllRanges(); getSelection().addRange(range);
    });
    const selected = await page.evaluate(() => getSelection().toString());
    assert.ok(selected.indexOf('Order beta') < selected.indexOf('Order alpha'));
    await clickButton('Reverse labels');
    await page.waitForTimeout(500);
    assert.equal(await page.evaluate(() => getSelection().toString()), selected, 'reorder defers while selection is active');
    await page.evaluate(() => getSelection().removeAllRanges());
    await page.waitForFunction(() => [...document.querySelectorAll('#cn1-text-layer span')]
      .filter(el => el.textContent.startsWith('Order '))[0]?.textContent === 'Order alpha');
    console.log('PASS', name, 'selection order follows component reordering without disturbing active ranges');

    await openReview('pointerfocus');
    const focusLabel = page.locator('#cn1-text-layer span').filter({hasText: /^Focusable label$/});
    assert.equal(await focusLabel.evaluate(el => getComputedStyle(el).pointerEvents), 'none');
    await page.locator('.cn1-selection-editor[aria-label="focusReadonly"]').click();
    await page.waitForFunction(() => document.body.innerText.includes('Readonly focused'));
    console.log('PASS', name, 'focusable labels retain canvas routing and readonly text synchronizes CN1 focus');

    await openReview('multialign');
    for (const [label, factor] of [['multiCenter', 0.5], ['multiBottom', 1]]) {
      const metrics = await page.locator('.cn1-selection-editor[aria-label="' + label + '"]').evaluate(el => {
        const css = getComputedStyle(el);
        return {height: el.getBoundingClientRect().height, top: parseFloat(css.paddingTop), bottom: parseFloat(css.paddingBottom), line: parseFloat(css.lineHeight)};
      });
      // Two lines, seven-pixel row gap, and asymmetric original padding.
      const content = metrics.line * 2 - 7;
      assert.ok(Math.abs(metrics.top - (11 + Math.floor((metrics.height - 11 - 19 - content) * factor))) <= 1, JSON.stringify(metrics));
    }
    console.log('PASS', name, 'multiline center and bottom alignment match canvas content height');

    await openReview('opacity');
    const faded = page.locator('.cn1-selection-editor[aria-label="opacityArea"]');
    for (const action of ['Fade field', 'Fade ancestor']) {
      await faded.waitFor({state: 'visible'});
      await clickButton(action);
      await faded.waitFor({state: 'hidden'});
      await clickButton('Reset opacity');
    }
    await faded.waitFor({state: 'visible'});
    console.log('PASS', name, 'field and ancestor opacity use canvas compositing and restore native editing');

    await openReview('uniqueeditor');
    const unique = page.locator('.cn1-selection-editor[aria-label="uniqueEditor"]');
    await unique.waitFor({state: 'visible'});
    await page.waitForFunction(() => !document.querySelector('#cn1-accessibility-tree input, #cn1-accessibility-tree textarea'));
    assert.equal(await page.getByRole('textbox').count(), 1, 'one accessible textbox while promoted');
    for (const [action, disabled] of [['Disable editor', 'true'], ['Enable editor', 'false']]) {
      await clickButton(action);
      await page.waitForFunction(disabled => document.querySelector('.cn1-selection-editor[aria-label="uniqueEditor"]').getAttribute('aria-disabled') === disabled, disabled);
    }
    await clickButton('Canvas fallback');
    await unique.waitFor({state: 'hidden'});
    await page.waitForFunction(() => document.querySelector('#cn1-accessibility-tree input, #cn1-accessibility-tree textarea'));
    assert.ok(await page.getByRole('textbox').count() > 0, 'semantic editing remains available on canvas');
    await clickButton('Restore editor');
    await unique.waitFor({state: 'visible'});
    await page.waitForFunction(() => !document.querySelector('#cn1-accessibility-tree input, #cn1-accessibility-tree textarea'));
    assert.equal(await page.getByRole('textbox').count(), 1);
    console.log('PASS', name, 'one accessible editor with live disabled semantics and semantic fallback');

    await openReview('traversal');
    const firstTab = page.locator('.cn1-selection-editor[aria-label="tabFirst"]');
    const lastTab = page.locator('.cn1-selection-editor[aria-label="tabLast"]');
    await firstTab.click();
    await page.keyboard.press('Tab');
    await page.waitForFunction(() => document.activeElement?.getAttribute('aria-label') === 'Middle button');
    await lastTab.click();
    await page.keyboard.press('Shift+Tab');
    await page.waitForFunction(() => document.activeElement?.getAttribute('aria-label') === 'Middle check');
    await lastTab.click();
    await page.keyboard.press('Tab');
    await page.waitForFunction(() => document.activeElement?.getAttribute('aria-label') === 'tabFirst');
    await firstTab.click();
    await page.keyboard.press('Shift+Tab');
    await page.waitForFunction(() => document.activeElement?.getAttribute('aria-label') === 'tabLast');
    console.log('PASS', name, 'native Tab includes buttons and checkboxes and wraps at both ends');

    await openReview('occlusion');
    assert.equal(await page.locator('.cn1-selection-editor[aria-label="coveredArea"]:visible').count(), 0);
    const cover = page.getByRole('button', {name: 'Edge button', exact: true});
    const coverBox = await cover.evaluate(el => el.getBoundingClientRect().toJSON());
    await page.mouse.click(coverBox.x + coverBox.width / 2, coverBox.y + coverBox.height / 2);
    await page.waitForFunction(() => document.body.innerText.includes('Edge button clicked'));
    console.log('PASS', name, 'partially overlapping buttons retain pointer input');

    await openReview('interactionstate');
    for (const active of [true, false, true]) {
      await page.waitForFunction(active => ['dynamicContext', 'dynamicStylus', 'dynamicCommands'].every(name => {
        const editor = document.querySelector('.cn1-selection-editor[aria-label="' + name + '"]');
        const label = [...document.querySelectorAll('#cn1-text-layer span')].find(el => el.textContent === name + ' label');
        return Boolean(editor && getComputedStyle(editor).display !== 'none') === active
          && Boolean(label && getComputedStyle(label).pointerEvents === 'auto') === active;
      }), active);
    }
    console.log('PASS', name, 'ancestor context menus, stylus listeners and commands refresh static native text');

    await openReview('accessiblename');
    for (const label of ['Original name', 'Updated name', 'Original hint', 'Updated hint']) {
      await page.waitForFunction(label => document.querySelector('.cn1-selection-editor')?.getAttribute('aria-label') === label, label);
    }
    console.log('PASS', name, 'accessible names track name and fallback hint changes');

    await openReview('maxsize');
    const limited = page.locator('.cn1-selection-editor[aria-label="limitedField"]');
    await limited.click();
    await page.waitForFunction(() => document.querySelector('.cn1-selection-editor[aria-label="limitedField"]').maxLength === 3);
    await limited.pressSequentially('abcdef');
    assert.equal(await limited.inputValue(), 'abc');
    await page.waitForFunction(() => document.querySelector('#cn1-accessibility-tree').textContent.includes('abc'));
    await clickButton('Check maximum');
    await page.waitForFunction(() => document.body.innerText.includes('Maximum 3 value abc'));
    console.log('PASS', name, 'reduced max size is enforced while the editor is focused');

    await openReview('subclasses');
    assert.equal(await page.locator('.cn1-selection-editor').count(), 0);
    for (const text of ['Custom area', 'Custom field']) {
      const field = page.locator('#cn1-accessibility-tree [role="textbox"]').filter({hasText: text});
      const box = await field.evaluate(el => el.getBoundingClientRect().toJSON());
      await page.mouse.click(box.x + 10, box.y + box.height / 2);
      await page.waitForFunction(text => document.body.innerText.includes(text + ' pressed'), text);
    }
    console.log('PASS', name, 'custom text component pointer overrides retain canvas input');

    await openReview('ellipsis');
    assert.equal(await page.locator('.cn1-selection-editor').count(), 0);
    await page.waitForFunction(() => window.__cn1TextDraws.concat([...document.querySelectorAll('#cn1-text-layer span')]
      .map(el => el.textContent)).some(text => text.includes('...')));
    console.log('PASS', name, 'truncated multiline text retains configured ellipses');

    for (const action of ['Make readonly', 'Disable field']) {
      await openReview('ownership');
      const editor = page.locator('.cn1-selection-editor[aria-label="ownershipField"]');
      await editor.waitFor({state: 'visible'});
      assert.equal(await editor.evaluate(el => el.closest('[aria-hidden="true"]') === null), true);
      assert.equal(await page.getByRole('textbox', {name: 'ownershipField', exact: true}).and(editor).count(), 1);
      await editor.click();
      await editor.fill('Updated session');
      await clickButton(action);
      await page.waitForFunction(() => document.querySelector('.cn1-selection-editor[aria-label="ownershipField"]').readOnly);
      await editor.evaluate(el => el.blur());
      await page.waitForTimeout(300);
      await clickButton('Edit password');
      const legacy = page.locator('input.cn1-edit-string[type="password"]:visible');
      await legacy.waitFor();
      await legacy.fill('secret');
      assert.equal(await legacy.inputValue(), 'secret');
    }
    console.log('PASS', name, 'native editors are accessible and release ownership after readonly/disabled transitions');

    await openReview('numeric');
    const numeric = page.locator('.cn1-selection-editor[aria-label="numericModel"]');
    await numeric.waitFor({state: 'visible'});
    assert.equal(await numeric.inputValue(), 'Not a number');
    assert.equal(await numeric.getAttribute('type'), 'text');
    assert.equal(await numeric.getAttribute('inputmode'), 'numeric');
    await clickButton('Change numeric model');
    await page.waitForFunction(() => document.querySelector('.cn1-selection-editor[aria-label="numericModel"]').value === 'Still not numeric');
    await numeric.fill('-123');
    await page.waitForFunction(() => document.body.innerText.includes('Numeric model -123'));
    await numeric.press('End');
    await numeric.pressSequentially('a');
    await page.waitForFunction(() => document.querySelector('.cn1-selection-editor[name="numericModel"]').value === '-123');
    await numeric.fill('12.3letters');
    await page.waitForFunction(() => document.querySelector('.cn1-selection-editor[name="numericModel"]').value === '-123');
    assert.ok((await page.locator('body').innerText()).includes('Numeric model -123'));
    await numeric.fill('');
    await numeric.pressSequentially('456');
    await page.waitForFunction(() => document.body.innerText.includes('Numeric model 456'));
    console.log('PASS', name, 'numeric user edits enforce integers while programmatic display values survive');

    await openReview('linemode');
    for (const tag of ['TEXTAREA', 'INPUT', 'TEXTAREA']) {
      await page.waitForFunction(tag => {
        const editors = document.querySelectorAll('.cn1-selection-editor[aria-label="changingLineMode"]');
        return editors.length === 1 && editors[0].tagName === tag && getComputedStyle(editors[0]).display !== 'none'
          && editors[0].value === 'Changing line mode';
      }, tag);
    }
    console.log('PASS', name, 'line mode changes recreate one native control on a static form');

    await openReview('canvasstyles');
    await clickButton('Apply decorations');
    await page.waitForFunction(() => [...document.querySelectorAll('.cn1-selection-editor')].every(el => getComputedStyle(el).display === 'none'));
    await page.waitForFunction(() => window.__cn1TextDraws.includes('Decoration 1'));
    assert.equal(await page.locator('#cn1-text-layer span').filter({hasText: 'ABBA'}).count(), 0);
    const bitmapPixels = await page.evaluate(() => {
      const semantic = [...document.querySelectorAll('#cn1-accessibility-tree [role="textbox"]')]
        .find(el => el.textContent === 'ABBA');
      const box = semantic.getBoundingClientRect();
      const canvas = document.getElementById('codenameone-canvas');
      const pixels = canvas.getContext('2d').getImageData(box.x + 6, box.y + 6, 48, 12).data;
      let dark = 0;
      for (let i = 0; i < pixels.length; i += 4) {
        if (pixels[i] < 80 && pixels[i + 1] < 80 && pixels[i + 2] < 80 && pixels[i + 3] > 200) dark++;
      }
      return dark;
    });
    assert.ok(bitmapPixels > 300, 'bitmap glyph pixels are painted on the canvas');
    await page.screenshot({path: path.join(artifacts, name + '-canvas-styles.png')});
    console.log('PASS', name, 'bitmap fonts and decorated text retain canvas rendering');

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
        // The preceding context-menu gesture can leave a selection menu open.
        // Exercise each pointer target on a fresh form without that occlusion.
        await openReview('exclusions');
        const span = page.locator('#cn1-text-layer span').filter({ hasText: text }).first();
        await span.waitFor({ state: 'attached' });
        await page.waitForFunction(text => [...document.querySelectorAll('#cn1-text-layer span')]
          .some(el => el.textContent.includes(text) && getComputedStyle(el).pointerEvents === 'none'), text);
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
    await page.waitForFunction(() => document.querySelector('[aria-label="reviewField"].cn1-selection-editor')?.getAttribute('inputmode') === 'numeric');
    assert.equal(await field.getAttribute('inputmode'), 'numeric');
    assert.equal(await field.getAttribute('autocomplete'), 'off');
    assert.equal(await field.getAttribute('spellcheck'), 'false');
    await clickButton('Reset constraints');
    await page.waitForFunction(() => document.querySelector('[aria-label="reviewField"].cn1-selection-editor')?.getAttribute('inputmode') === null);
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
    await page.waitForFunction(() => document.activeElement?.getAttribute('aria-label') === 'selectionNotes');
    assert.equal(await notes.evaluate(el => document.activeElement === el), true, 'semantic focus must not steal the native editor');
    assert.equal(await notes.inputValue(), accents);
    console.log('PASS', name, 'Unicode and editing survive focus changes');
    if (!mobile) {
      await title.click();
      await page.keyboard.press('Tab');
      await page.waitForFunction(() => document.activeElement?.getAttribute('aria-label') === 'selectionNotes');
      assert.equal(await notes.evaluate(el => document.activeElement === el), true, 'Tab moves to the next CN1 field');
      await page.keyboard.press('Shift+Tab');
      await page.waitForFunction(() => document.activeElement?.getAttribute('aria-label') === 'selectionTitle');
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
    logs.push(JSON.stringify(await page.evaluate(() => ({ url: location.href, active: document.activeElement && document.activeElement.outerHTML,
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

async function exerciseMobileEnter(browser, name) {
  const context = await browser.newContext({viewport: {width: 900, height: 800}, hasTouch: true,
    userAgent: 'Mozilla/5.0 (Linux; Android 14; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Mobile Safari/537.36'});
  try {
    const page = await context.newPage();
    const errors = [];
    page.on('pageerror', e => errors.push(String(e)));
    await page.goto(`http://127.0.0.1:${port}/?review=mobileenter`);
    const first = page.locator('.cn1-selection-editor[name="completionFirst"]');
    await first.waitFor({timeout: 60000});
    assert.equal(await first.getAttribute('data-cn1-enter-next'), 'true');
    await first.click();
    await first.fill('Mobile committed value');
    await first.press('Enter');
    await page.waitForFunction(() => document.activeElement?.getAttribute('name') === 'completionSecond');
    await page.waitForFunction(() => document.body.innerText.includes('First committed Mobile committed value'));
    assert.equal(await first.inputValue(), 'Mobile committed value');
    assert.deepEqual(errors, []);
    console.log('PASS', name, 'Android browser profile Enter commits and advances to the next native editor');
  } finally { await context.close(); }
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
        if (process.env.CN1_JS_MOBILE_NEXT_ONLY !== 'true') {
          const context = await browser.newContext({ viewport: { width: 900, height: 800 } });
          try { await exercise(context, name, '127.0.0.1'); } finally { await context.close(); }
        }
        await exerciseMobileEnter(browser, name);
      } finally { await browser.close(); }
    }
  }
} finally { server.close(); }
