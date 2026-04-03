import fs from 'node:fs';

let chromium;
try {
  ({ chromium } = await import('playwright'));
} catch (playwrightError) {
  try {
    ({ chromium } = await import('@playwright/test'));
  } catch (playwrightTestError) {
    console.error('Unable to load Playwright. Install either "playwright" or "@playwright/test".');
    console.error(String(playwrightError));
    console.error(String(playwrightTestError));
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
    viewport: { width: 1280, height: 900 }
  });

  page.on('console', msg => append(`console:${msg.type()}:${msg.text()}`));
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
    if (state.error) {
      break;
    }
    await page.waitForTimeout(1000);
  }
} finally {
  await browser.close();
}
