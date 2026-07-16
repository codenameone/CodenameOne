import assert from 'node:assert/strict';

let chromium;
try {
  ({ chromium } = await import('playwright'));
} catch (playwrightError) {
  try {
    ({ chromium } = await import('@playwright/test'));
  } catch (playwrightTestError) {
    console.error('Unable to load Playwright. Install either "playwright" or "@playwright/test".');
    console.error('Import from "playwright" failed:', String(playwrightError));
    console.error('Import from "@playwright/test" failed:', String(playwrightTestError));
    process.exit(2);
  }
}

const url = process.argv[2];
if (!url) {
  console.error('Usage: node tools/verify-lightweight-editor-input.mjs <playground-url>');
  process.exit(2);
}

const browser = await chromium.launch({headless: true});
try {
  const page = await browser.newPage({viewport: {width: 1440, height: 900}});
  page.on('console', message => {
    if (message.type() === 'error') {
      console.error(`Browser console: ${message.text()}`);
    }
  });
  await page.goto(url, {waitUntil: 'domcontentloaded', timeout: 30000});
  const wrapperFrame = await page.waitForFunction(() => {
    const frame = document.querySelector('iframe[src*="playground-app"]');
    return frame ? true : window.cn1Started === true;
  }, null, {timeout: 30000});
  await wrapperFrame.dispose();

  const appFrame = page.frames().find(frame => frame.url().includes('/playground-app/')) || page.mainFrame();
  await appFrame.waitForFunction(() => window.cn1Started === true, null, {timeout: 30000});

  const frameElement = appFrame === page.mainFrame() ? null : await appFrame.frameElement();
  const frameBox = frameElement ? await frameElement.boundingBox() : {x: 0, y: 0};
  await page.mouse.click(frameBox.x + 300, frameBox.y + 190);
  await appFrame.waitForFunction(() => {
    const input = document.querySelector('textarea.cn1-lightweight-text-input');
    return input && document.activeElement === input;
  }, null, {timeout: 5000});

  const initial = await appFrame.evaluate(() => {
    const input = document.querySelector('textarea.cn1-lightweight-text-input');
    return {value: input.value, start: input.selectionStart, end: input.selectionEnd};
  });
  const typed = 'A B C';
  const afterTyped = initial.value.substring(0, initial.start) + typed + initial.value.substring(initial.end);
  const insertAt = initial.start + typed.length - 2;
  const expected = afterTyped.substring(0, insertAt) + 'X' + afterTyped.substring(insertAt);

  await page.keyboard.type(typed);
  await page.keyboard.press('ArrowLeft');
  await page.keyboard.press('ArrowLeft');
  await page.keyboard.type('X');
  await appFrame.waitForFunction(value => {
    const input = document.querySelector('textarea.cn1-lightweight-text-input');
    return input && input.value === value;
  }, expected, {timeout: 5000});

  const canvases = await appFrame.locator('canvas').all();
  let displayCanvas = null;
  let displayArea = 0;
  for (const canvas of canvases) {
    const box = await canvas.boundingBox();
    const area = box ? box.width * box.height : 0;
    if (area > displayArea) {
      displayCanvas = canvas;
      displayArea = area;
    }
  }
  assert.ok(displayCanvas, 'Playground did not expose a visible editor canvas');
  const beforeMarker = await displayCanvas.screenshot();

  const marker = 'VISIBLE_MARKER_123';
  await page.keyboard.press(process.platform === 'darwin' ? 'Meta+A' : 'Control+A');
  await page.keyboard.type(marker);
  await appFrame.waitForFunction(value => {
    const input = document.querySelector('textarea.cn1-lightweight-text-input');
    return input && input.value === value;
  }, marker, {timeout: 5000});
  const afterMarker = await displayCanvas.screenshot();
  assert.notDeepEqual(afterMarker, beforeMarker, 'Editor canvas did not visibly repaint after keyboard input');

  const pasteDispatch = await appFrame.evaluate(() => {
    const input = document.querySelector('textarea.cn1-lightweight-text-input');
    const transfer = new DataTransfer();
    transfer.setData('text/plain', 'PASTE_MARKER');
    transfer.setData('text/html', '<b>PASTE_MARKER</b>');
    const event = new ClipboardEvent('paste', {
      bubbles: true,
      cancelable: true,
      clipboardData: transfer
    });
    const dispatchResult = input.dispatchEvent(event);
    return {
      dispatchResult,
      defaultPrevented: event.defaultPrevented,
      plainText: event.clipboardData && event.clipboardData.getData('text/plain'),
      htmlText: event.clipboardData && event.clipboardData.getData('text/html')
    };
  });
  assert.deepEqual(pasteDispatch, {
    dispatchResult: false,
    defaultPrevented: true,
    plainText: 'PASTE_MARKER',
    htmlText: '<b>PASTE_MARKER</b>'
  }, 'Browser bridge did not synchronously negotiate and cancel the paste event');
  const pastedValue = 'VISIBLE_MARKER_123PASTE_MARKER';
  try {
    await appFrame.waitForFunction(value => {
      const input = document.querySelector('textarea.cn1-lightweight-text-input');
      return input && input.value === value;
    }, pastedValue, {timeout: 5000});
  } catch (error) {
    const actual = await appFrame.locator('textarea.cn1-lightweight-text-input').inputValue();
    assert.equal(actual, pastedValue, 'Negotiated clipboard paste produced the wrong editor text');
    throw error;
  }

  const artifactDir = process.env.PLAYGROUND_INPUT_ARTIFACT_DIR;
  if (artifactDir) {
    await page.screenshot({path: `${artifactDir}/playground-lightweight-input.png`, fullPage: true});
  }
  console.log('PASS: lightweight playground input accepted spaces, arrows, rapid text, negotiated paste, and visibly repainted.');
} finally {
  await browser.close();
}
