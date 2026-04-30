import fs from 'node:fs';
import path from 'node:path';

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

const url = process.env.URL;
const logFile = process.env.LOG_FILE;
const outputDir = process.env.CN1_JS_DEBUG_OUT_DIR;
const timeoutSeconds = Number(process.env.CN1_JS_BROWSER_LIFETIME_SECONDS || '120');
const SUITE_FINISHED_MARKER = 'CN1SS:SUITE:FINISHED';

if (!url) {
  console.error('URL is required');
  process.exit(2);
}
if (!outputDir) {
  console.error('CN1_JS_DEBUG_OUT_DIR is required');
  process.exit(2);
}

fs.mkdirSync(outputDir, { recursive: true });

let suiteFinished = false;

function append(line) {
  const text = `[playwright] ${line}\n`;
  if (logFile) {
    fs.appendFileSync(logFile, text, 'utf8');
  } else {
    process.stdout.write(text);
  }
}

function writePngFromDataUrl(targetPath, dataUrl) {
  const comma = String(dataUrl || '').indexOf(',');
  if (comma < 0) {
    return false;
  }
  const body = String(dataUrl).substring(comma + 1);
  fs.writeFileSync(targetPath, Buffer.from(body, 'base64'));
  return true;
}

const browser = await chromium.launch({
  headless: true,
  args: [
    '--autoplay-policy=no-user-gesture-required',
    '--disable-web-security',
    '--allow-file-access-from-files'
  ]
});

try {
  const page = await browser.newPage({
    viewport: { width: 1280, height: 900 }
  });

  page.on('console', msg => {
    const text = msg.text();
    append(`console:${msg.type()}:${text}`);
    if (text.indexOf(SUITE_FINISHED_MARKER) >= 0) {
      suiteFinished = true;
    }
  });
  page.on('pageerror', err => append(`pageerror:${String(err)}`));
  page.on('requestfailed', req => append(`requestfailed:${req.url()} ${req.failure()?.errorText || ''}`));

  append(`goto:${url}`);
  await page.goto(url, { waitUntil: 'domcontentloaded', timeout: 60000 });
  await page.waitForTimeout(2000);

  const start = Date.now();
  while (Date.now() - start < timeoutSeconds * 1000) {
    const state = await page.evaluate(() => ({
      initialized: !!window.cn1Initialized,
      started: !!window.cn1Started,
      error: window.__parparError ? JSON.stringify(window.__parparError) : ''
    }));
    append(`state:${JSON.stringify(state)}`);
    if (state.error || suiteFinished) {
      break;
    }
    await page.waitForTimeout(1000);
  }

  const canvases = await page.evaluate(async () => {
    const bridge = window.cn1HostBridge && window.cn1HostBridge.handlers;
    if (!bridge || typeof bridge.__cn1_debug_list_canvases__ !== 'function') {
      return [];
    }
    const listed = await bridge.__cn1_debug_list_canvases__();
    const out = [];
    for (const entry of listed || []) {
      let dataUrl = '';
      if (typeof bridge.__cn1_debug_capture_canvas_by_id__ === 'function') {
        dataUrl = await bridge.__cn1_debug_capture_canvas_by_id__({ id: entry.id });
      }
      out.push({ ...entry, dataUrl });
    }
    return out;
  });

  fs.writeFileSync(path.join(outputDir, 'canvas-summary.json'), JSON.stringify(canvases, null, 2));
  for (const entry of canvases) {
    if (!entry || !entry.dataUrl) {
      continue;
    }
    writePngFromDataUrl(path.join(outputDir, `canvas-${entry.id}.png`), entry.dataUrl);
  }
} finally {
  await browser.close();
}
