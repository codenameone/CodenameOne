// The Bastion web app, hosted by its own server, driven in a headless browser.
//
//   URL=http://localhost:18091/ OUT=target/web-e2e node web-e2e.mjs
//
// It checks what a person opening the server's address gets: the app loads from
// the server, with no error, and shows its sign-in form with the way to open an
// account. With BASTION_USER and BASTION_PASSWORD it then signs in through that
// form and waits for the dashboard.
// run-web-e2e.sh builds everything, starts the server and runs this; see there.
//
// The app draws on a canvas, so there are no buttons in the page to ask a
// browser automation tool for. What there is, is the accessibility tree the
// port keeps over the canvas -- an element per component, with its role, its
// label and where it is -- and that is what this reads to find a button and
// what it clicks at. Text fields become real <input> elements while they are
// being edited, so typing is ordinary typing.

import fs from 'node:fs';
import path from 'node:path';
import { createRequire } from 'node:module';

// require() and not import: it also looks in NODE_PATH, which is how a
// container whose packages are installed somewhere else finds Playwright.
const require = createRequire(import.meta.url);
let chromium;
try {
  ({ chromium } = require('playwright'));
} catch (err) {
  console.error('[bastion-web] Playwright is not installed. Run `npm ci` in scripts/ and '
      + '`npx playwright install chromium`, or point NODE_PATH at a node_modules that has it.');
  console.error(String(err));
  process.exit(2);
}

const url = process.env.URL;
const out = process.env.OUT || 'target/web-e2e';
const email = process.env.BASTION_USER || '';
const password = process.env.BASTION_PASSWORD || '';
// A cold runner translates nothing at this point, but it does parse several
// megabytes of JavaScript before the first screen.
const startSeconds = Number(process.env.BASTION_WEB_START_SECONDS || '120');
const stepSeconds = Number(process.env.BASTION_WEB_STEP_SECONDS || '45');
if (!url) {
  console.error('[bastion-web] URL is required');
  process.exit(2);
}
fs.mkdirSync(out, { recursive: true });
const logFile = path.join(out, 'browser.log');
fs.writeFileSync(logFile, '');
const note = line => fs.appendFileSync(logFile, line + '\n');
const say = line => { console.log('[bastion-web] ' + line); note('# ' + line); };

const origin = new URL(url).origin;
const requests = [];       // "200 POST /login", in order
const problems = [];       // anything that must not happen
const sockets = [];        // { url, frames: [...] }

const browser = await chromium.launch({
  headless: true,
  // A headless page counts as hidden, and a hidden page's timers are throttled
  // to about one a minute once its budget is spent. The app's threads sleep on
  // those timers.
  args: ['--disable-background-timer-throttling', '--disable-renderer-backgrounding',
    '--disable-backgrounding-occluded-windows', '--disable-features=IntensiveWakeUpThrottling']
});
// A browser reports where it is, and the restaurants listed are the ones that
// deliver there: this one is by the seeded restaurants, near Pier 39.
const page = await browser.newPage({ viewport: { width: 420, height: 860 } });
page.on('console', message => {
  const text = message.text();
  if (text.startsWith('LF-SHIM')) {
    return;
  }
  note(`[${message.type()}] ${text}`);
  if (/internal application error|Missing JS member/i.test(text)) {
    problems.push('the app reported an error: ' + text.split('\n')[0]);
  }
});
page.on('pageerror', err => {
  note('[pageerror] ' + err);
  problems.push('the page threw: ' + err);
});
page.on('response', response => {
  const at = new URL(response.url());
  if (at.origin !== origin) {
    return;
  }
  const line = `${response.status()} ${response.request().method()} ${at.pathname}`;
  requests.push(line);
  // The query says where a list was asked for; it holds no secrets (a token
  // travels in a header).
  note('NET ' + line + (at.pathname.startsWith('/api/') ? at.search : ''));
});
page.on('websocket', socket => {
  const seen = { url: socket.url(), frames: [] };
  sockets.push(seen);
  note('WS open ' + socket.url().replace(/ticket=.*/, 'ticket=...'));
  socket.on('framereceived', frame => {
    seen.frames.push(String(frame.payload));
    note('WS frame ' + String(frame.payload).slice(0, 200));
  });
});

/// What the app has on screen, as the port describes it to assistive technology.
async function components() {
  return page.evaluate(() => Array.from(
      document.querySelectorAll('#cn1-accessibility-tree [data-cn1-accessibility-id]')).map(el => {
    const box = el.getBoundingClientRect();
    return {
      role: el.getAttribute('role'),
      label: el.getAttribute('aria-label'),
      text: el.innerText,
      x: box.x, y: box.y, width: box.width, height: box.height
    };
  }));
}

async function screenText() {
  return page.evaluate(() => document.body.innerText);
}

async function waitFor(what, seconds, probe) {
  const end = Date.now() + seconds * 1000;
  for (;;) {
    const hit = await probe();
    if (hit) {
      return hit;
    }
    if (problems.length > 0) {
      throw new Error(problems[0]);
    }
    if (Date.now() > end) {
      throw new Error(`timed out after ${seconds}s waiting for ${what}`);
    }
    await page.waitForTimeout(250);
  }
}

const button = label => async () =>
    (await components()).find(c => c.role === 'button' && c.label === label);

/// The single-line fields of the form on screen, top to bottom.
async function fields() {
  return (await components()).filter(c => c.role === 'textbox' && c.height > 40 && c.height < 120)
      .sort((a, b) => a.y - b.y);
}

async function click(component) {
  await page.mouse.click(component.x + component.width / 2, component.y + component.height / 2);
}

/// Waits until nothing on screen has moved for a moment. A form slides in, and
/// a click aimed at where a field is half-way through lands on what was there.
async function settled() {
  let before = JSON.stringify(await components());
  await waitFor('the screen to stop moving', stepSeconds, async () => {
    await page.waitForTimeout(350);
    const now = JSON.stringify(await components());
    const still = now === before;
    before = now;
    return still;
  });
}

async function typeInto(index, text, type) {
  const editing = () => page.evaluate(
      wanted => document.activeElement && document.activeElement.tagName === 'INPUT'
          && document.activeElement.type === wanted, type);
  await waitFor(`the ${type} field to take the keyboard`, stepSeconds, async () => {
    await settled();
    const all = await fields();
    if (all.length <= index) {
      return false;
    }
    await click(all[index]);
    await page.waitForTimeout(500);
    return editing();
  });
  await page.keyboard.type(text, { delay: 10 });
}

/// Fills in the sign-in form, which is the first screen, and sends it.
async function signInAs(who) {
  await waitFor('the sign-in form', startSeconds, button('Sign in'));
  await typeInto(0, who, 'email');
  await typeInto(1, password, 'password');
  await settled();
  await click(await waitFor('the Sign in button', stepSeconds, button('Sign in')));
  say('submitted the sign-in form as ' + who);
}

/// Fails on the dialog the app shows for an error nothing handled.
async function noErrorDialog() {
  if (/internal application error/i.test(await screenText())) {
    problems.push('the app is showing its internal-error dialog');
  }
  if (problems.length > 0) {
    throw new Error(problems[0]);
  }
}

let failed = null;
try {
  say('opening ' + url);
  const first = await page.goto(url, { waitUntil: 'load' });
  if (!first || first.status() !== 200) {
    throw new Error('the server answered ' + (first ? first.status() : 'nothing') + ' at /');
  }
  const type = first.headers()['content-type'] || '';
  if (!type.startsWith('text/html')) {
    throw new Error('/ is served as ' + type + ', not as a page');
  }

  // 1. The sign-in form: the app started, in the browser, from this server.
  await waitFor('the sign-in form', startSeconds, button('Sign in'));
  await waitFor('the form to offer an account', stepSeconds, button('Open an account'));
  await noErrorDialog();
  await page.screenshot({ path: path.join(out, '1-sign-in.png') });
  say('the sign-in form is up');
  for (const file of ['/translated_app.js', '/port.js', '/worker.js']) {
    if (!requests.includes('200 GET ' + file)) {
      throw new Error(file + ' was not served by this server: ' + requests.join(', '));
    }
  }

  // 2. Signing in, when an account is given.
  if (email) {
    await signInAs(email);
    await waitFor('the dashboard', stepSeconds, async () =>
        /Total balance|Accounts/.test(await screenText()));
    await noErrorDialog();
    await page.screenshot({ path: path.join(out, '2-home.png') });
    say('signed in as ' + email);
  }
} catch (err) {
  failed = err;
  try {
    await page.screenshot({ path: path.join(out, 'failure.png') });
    fs.writeFileSync(path.join(out, 'failure.txt'), await screenText());
    fs.writeFileSync(path.join(out, 'failure-components.json'),
        JSON.stringify(await components(), null, 1));
  } catch (ignored) {
    // The page is gone; the log is what there is.
  }
}
await browser.close();

if (failed) {
  console.error('[bastion-web] FAILED: ' + failed.message);
  console.error('[bastion-web] the browser log is ' + logFile);
  process.exit(1);
}
say('passed');
