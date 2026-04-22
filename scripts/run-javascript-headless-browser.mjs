import fs from 'node:fs';

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
const timeoutSeconds = Number(process.env.CN1_JS_BROWSER_LIFETIME_SECONDS || '120');

if (!url) {
  console.error('URL is required');
  process.exit(2);
}

const SUITE_FINISHED_MARKER = 'CN1SS:SUITE:FINISHED';

let suiteFinished = false;

function append(line) {
  const text = `[playwright] ${line}\n`;
  if (logFile) {
    fs.appendFileSync(logFile, text, 'utf8');
  } else {
    process.stdout.write(text);
  }
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
    viewport: { width: 375, height: 667 },
    // deviceScaleFactor=2 emulates a retina display so window.devicePixelRatio
    // reports 2 — without it Chromium reports 1 and CN1 picks DENSITY_MEDIUM,
    // which leaves padding/margin/font sizes about half of what an iOS/Android
    // reference screenshot shows. Backing store is 750x1334 (similar area to
    // the previous 1280x900 viewport; still comfortably smaller per-test).
    deviceScaleFactor: 2
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
  page.on('response', resp => {
    if (resp.status() >= 400) {
      append(`response:${resp.status()}:${resp.url()}`);
    }
  });

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
} finally {
  await browser.close();
}
