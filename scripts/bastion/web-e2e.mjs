// The Bastion web app, hosted by its own server, driven in a headless browser.
//
//   URL=http://localhost:18091/ OUT=target/web-e2e node web-e2e.mjs
//
// It checks what a person opening the server's address gets: the app loads from
// the server, with no error, and shows its sign-in form with the way to open an
// account. Then, all through the app's own screens and the authorization-code
// flow the app runs from the page (no Cookie header, the browser's own jar):
// it opens an account, turns on the authenticator app with the key it is shown,
// signs in again in a fresh browser with the password and a six-digit code,
// reaches the dashboard, adds money, opens a second account and moves money
// between the two. BASTION_WEB_ACCOUNTS=0 stops after the sign-in form.
// run-web-e2e.sh builds everything, starts the server and runs this; see there.
//
// The app draws on a canvas, so there are no buttons in the page to ask a
// browser automation tool for. What there is, is the accessibility tree the
// port keeps over the canvas -- an element per component, with its role, its
// label and where it is -- and that is what this reads to find a button and
// what it clicks at. Text fields become real <input> elements while they are
// being edited, so typing is ordinary typing.

import crypto from 'node:crypto';
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
const accounts = process.env.BASTION_WEB_ACCOUNTS !== '0';
// The guides' screenshots; needs the development profile's demo accounts.
const tour = process.env.BASTION_WEB_TOUR || '';
// A new address each run: the server under test may have kept the last one.
const email = `web-${Date.now()}@bastion.example`;
const password = 'correct horse battery staple';
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
let page;
let cdp;
let authenticator;
/// A page in a browser of its own: no cookie and no storage from the page
/// before. It has a platform authenticator -- Chromium's virtual one, through
/// the DevTools protocol -- holding whatever passkeys the last one held, the
/// way a phone keeps its passkeys while the browser forgets its session.
async function newPage() {
  let passkeys = [];
  if (page) {
    passkeys = (await cdp.send('WebAuthn.getCredentials', { authenticatorId: authenticator }))
        .credentials;
    await page.context().close();
  }
  const context = await browser.newContext({ viewport: { width: 420, height: 860 } });
  page = await context.newPage();
  cdp = await context.newCDPSession(page);
  await cdp.send('WebAuthn.enable');
  ({ authenticatorId: authenticator } = await cdp.send('WebAuthn.addVirtualAuthenticator', {
    options: { protocol: 'ctap2', transport: 'internal', hasResidentKey: true,
      hasUserVerification: true, isUserVerified: true, automaticPresenceSimulation: true }
  }));
  for (const credential of passkeys) {
    await cdp.send('WebAuthn.addCredential', { authenticatorId: authenticator, credential });
  }
  watch(page);
}

function watch(page) {
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
}

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
  return (await components()).filter(c => c.role === 'textbox' && c.height > 44 && c.height < 66)
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
      wanted => document.activeElement && (wanted === 'any'
          ? /^(INPUT|TEXTAREA)$/.test(document.activeElement.tagName)
          : document.activeElement.tagName === 'INPUT' && document.activeElement.type === wanted),
      type);
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

async function press(label, seconds = stepSeconds) {
  // A field still being edited commits on the first click elsewhere, and
  // the button it enables would not take that click: end the edit first, on
  // the title bar, which does nothing.
  if (await page.evaluate(() => document.activeElement
      && /^(INPUT|TEXTAREA)$/.test(document.activeElement.tagName))) {
    await page.mouse.click(210, 8);
  }
  await settled();
  await click(await waitFor(`the ${label} button`, seconds, button(label)));
}

/// RFC 6238 with the authenticator-app defaults: SHA-1, six digits, 30 s.
function totp(secret, step) {
  const alphabet = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ234567';
  let bits = '';
  for (const c of secret.replace(/[\s=]/g, '').toUpperCase()) {
    bits += alphabet.indexOf(c).toString(2).padStart(5, '0');
  }
  const key = Buffer.alloc(Math.floor(bits.length / 8));
  for (let i = 0; i < key.length; i++) {
    key[i] = parseInt(bits.substr(i * 8, 8), 2);
  }
  const counter = Buffer.alloc(8);
  counter.writeBigUInt64BE(BigInt(step));
  const mac = crypto.createHmac('sha1', key).update(counter).digest();
  const at = mac[mac.length - 1] & 0xf;
  return String((mac.readUInt32BE(at) & 0x7fffffff) % 1000000).padStart(6, '0');
}
const step = () => Math.floor(Date.now() / 30000);

/// Fails on the dialog the app shows for an error nothing handled.
async function noErrorDialog() {
  if (/internal application error/i.test(await screenText())) {
    problems.push('the app is showing its internal-error dialog');
  }
  if (problems.length > 0) {
    throw new Error(problems[0]);
  }
}

/// Opens the side menu and picks `label` in it.
async function menu(label) {
  for (let tries = 0; tries < 6; tries++) {
    await settled();
    // The menu button is the right-most of the title bar's left buttons: a
    // form below the dashboard has its back button before it.
    const open = (await components()).filter(c => c.role === 'button' && c.y < 70 && c.x < 130)
        .sort((x, y) => y.x - x.x)[0];
    if (!open) {
      // The Security Center has its way back at the end of the page.
      const back = await button('Back')();
      if (!back) {
        throw new Error('no menu button and no Back button on screen');
      }
      if (back.y > 800) {
        await page.mouse.move(210, 430);
        await page.mouse.wheel(0, 6000);
        continue;
      }
      await click(back);
      continue;
    }
    await click(open);
    await settled();
    const item = await button(label)();
    if (item) {
      await click(item);
      await settled();
      return;
    }
  }
  throw new Error('no ' + label + ' in the side menu');
}

async function shot(name) {
  await settled();
  await noErrorDialog();
  await page.screenshot({ path: path.join(tour, name + '.png') });
  say('took ' + name);
}

const textOnScreen = pattern => async () => pattern.test(await screenText());

/// The screens the guides show, on the development profile's demo accounts:
/// BASTION_WEB_TOUR names the directory the pictures go to.
async function guideTour() {
  fs.mkdirSync(tour, { recursive: true });
  for (const [from, to] of [['1-sign-in', 'bastion-sign-in'],
    ['2-authenticator', 'bastion-totp-enrolment'], ['3-code', 'bastion-sign-in-code']]) {
    if (fs.existsSync(path.join(out, from + '.png'))) {
      fs.copyFileSync(path.join(out, from + '.png'), path.join(tour, to + '.png'));
    }
  }
  await newPage();
  await page.goto(url, { waitUntil: 'load' });
  await waitFor('the sign-in form', startSeconds, button('Sign in'));
  await typeInto(0, 'customer@bastion.example', 'email');
  await typeInto(1, 'bastion-demo-password', 'password');
  await press('Sign in');
  await waitFor('the dashboard', stepSeconds, textOnScreen(/Total balance/));
  await shot('bastion-dashboard-unregistered');
  await press('Register this device');
  await waitFor('the password prompt', stepSeconds, textOnScreen(/Confirm it is you/));
  await typeInto(0, 'bastion-demo-password', 'password');
  await press('Continue');
  await waitFor('the device to be registered', stepSeconds, async () =>
      requests.includes('200 POST /api/devices'));
  await settled();
  await shot('bastion-dashboard');

  const checking = await waitFor('the checking account', stepSeconds, async () =>
      (await components()).find(c => /Checking/.test(c.label || c.text || '') && c.height < 200));
  await click(checking);
  await waitFor('the history', stepSeconds, textOnScreen(/Money in/));
  await shot('bastion-account-history');
  await press('Order a card');
  await waitFor('the card', stepSeconds, async () => requests.some(r => /POST \/api\/accounts\/.*\/card/.test(r)));
  await waitFor('the cards screen', stepSeconds, textOnScreen(/Daily limit/));
  await shot('bastion-cards');

  await menu('Payees');
  await press('Add a payee');
  await typeInto(0, 'customer2@bastion.example', 'email');
  await typeInto(1, 'Robin', 'any');
  await press('Add');
  await waitFor('the payee', stepSeconds, textOnScreen(/Cooling off/));
  await shot('bastion-payees');

  await menu('Accounts');
  await press('Pay someone');
  await typeInto(0, '20.00', 'any');
  await page.mouse.click(210, 8);
  await shot('bastion-transfer');
  await press('Review');
  await waitFor('the confirmation', stepSeconds, textOnScreen(/Send .* to /));
  await shot('bastion-transfer-confirm');
  await press('Send');
  await waitFor('the receipt', stepSeconds, textOnScreen(/Sent/));
  await shot('bastion-receipt');


  await menu('Exchange');
  await waitFor('the markets', stepSeconds, textOnScreen(/BTC/));
  await shot('bastion-exchange');
  const btc = await waitFor('the BTC market', stepSeconds, async () =>
      (await components()).find(c => /BTC/.test(c.label || c.text || '') && c.height < 200));
  await click(btc);
  await waitFor('the order book', stepSeconds, textOnScreen(/Last price/));
  await page.waitForTimeout(3000);
  await shot('bastion-order-book');
  // A market buy: the wallet it fills is what Send crypto sends from.
  await press('Market');
  await page.mouse.move(210, 430);
  await page.mouse.wheel(0, 600);
  await typeInto(0, '100.00', 'any');
  await press('Place order');
  await press('Place');
  await waitFor('the fill', stepSeconds, async () => requests.includes('200 POST /api/exchange/orders'));
  await page.waitForTimeout(1500);
  await shot('bastion-order-filled');

  await menu('Send crypto');
  await press('Add an address');
  await typeInto(0, 'bc1qar0srrr7xfkvy5l643lydnw9re59gtzzwf5mdq', 'any');
  await typeInto(1, 'Hardware wallet', 'any');
  await press('Add');
  await press('OK');
  await shot('bastion-withdraw');

  await menu('Security Center');
  await shot('bastion-security-center');

  await menu('Settings');
  await shot('bastion-settings');
  await click(await waitFor('the Unlock PIN row', stepSeconds, async () =>
      (await components()).find(c => c.label === 'Unlock PIN')));
  await typeInto(0, '480261', 'any');
  await typeInto(1, '480261', 'any');
  await press('Set PIN');
  await waitFor('the PIN to be on', stepSeconds, textOnScreen(/Opens the lock/));
  // Two minutes without a touch lock the app (AppConfig.IDLE_LOCK_MILLIS).
  await waitFor('the idle lock', 200, textOnScreen(/Bastion is locked/));
  await page.waitForTimeout(1500);
  await shot('bastion-lock');

  await newPage();
  await page.goto(url, { waitUntil: 'load' });
  await waitFor('the sign-in form', startSeconds, button('Sign in'));
  await typeInto(0, 'admin@bastion.example', 'email');
  await typeInto(1, 'bastion-demo-password', 'password');
  await press('Sign in');
  // An administrator lands on the administration screen.
  await press('Verify the audit log');
  await press('Verify the ledger');
  await waitFor('the verification', stepSeconds, textOnScreen(/Intact|Problems found/));
  await shot('bastion-admin-verify');
}

let failed = null;
try {
  say('opening ' + url);
  await newPage();
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

  if (accounts) {
    // 2. Opening an account: the app registers, then signs in from the page.
    await press('Open an account');
    await typeInto(0, 'Web Tester', 'any');
    await typeInto(1, email, 'email');
    await typeInto(2, password, 'password');
    await typeInto(3, password, 'password');
    await press('Create account');
    say('asked for an account as ' + email);

    // 3. The authenticator app: the sign-in was made with the password alone,
    // so adding an app asks for it again. The key on screen is then what an
    // app would scan.
    await waitFor('the password prompt', stepSeconds, async () =>
        /Confirm it is you/.test(await screenText()));
    await typeInto(0, password, 'password');
    await press('Continue');
    const shown = await waitFor('the authenticator key', stepSeconds, async () =>
        (await components()).map(c => c.label || c.text || '')
            .find(text => /^[A-Z2-7]{4}( [A-Z2-7]{1,4})+$/.test(text.trim())));
    const secret = shown.replace(/\s/g, '');
    await page.screenshot({ path: path.join(out, '2-authenticator.png') });
    const confirmedAt = step();
    await typeInto(0, totp(secret, confirmedAt), 'any');
    await press('Turn on');
    await press('I have saved them');
    say('turned the authenticator app on');
    // A passkey, made in the browser with the session the sign-in kept.
    await press('Create a passkey');
    await waitFor('the passkey to be saved', stepSeconds, async () =>
        requests.includes('200 POST /webauthn/register'));
    say('created a passkey');
    // Biometrics and this device are offered next; the first is missing in a
    // headless browser, and the device is registered later, after a passkey
    // sign-in.
    await waitFor('the rest of onboarding', stepSeconds, async () =>
        (await button('Continue')()) || (await button('Not now')())
            || (await button('Later')()) || /Total balance/.test(await screenText()));
    for (const next of ['Continue', 'Not now', 'Later']) {
      await settled();
      const hit = await button(next)();
      if (hit) {
        await click(hit);
      }
    }
    await waitFor('the dashboard', stepSeconds, async () =>
        /Total balance/.test(await screenText()));
    await noErrorDialog();
    say('onboarded');

    // 4. A fresh browser: password, then the code the account now asks for.
    await newPage();
    await page.goto(url, { waitUntil: 'load' });
    await signInAs(email);
    await waitFor('the code screen', stepSeconds, button('Continue'));
    // The step the code was confirmed in is used up; wait for the next one.
    await waitFor('a new code', 35, async () => step() > confirmedAt);
    await typeInto(0, totp(secret, step()), 'any');
    await page.mouse.click(210, 8);
    await settled();
    await page.screenshot({ path: path.join(out, '3-code.png') });
    await press('Continue');
    await waitFor('the dashboard', stepSeconds, async () =>
        /Total balance/.test(await screenText()));
    await noErrorDialog();
    await page.screenshot({ path: path.join(out, '3-home.png') });
    say('signed in with the password and a code');
    for (const done of ['302 POST /login', '200 GET /login/mfa', '200 POST /login/mfa',
      '302 GET /oauth2/authorize', '200 GET /signin/code',
      '200 POST /oauth2/token']) {
      if (!requests.includes(done)) {
        throw new Error('the sign-in did not go ' + done + ': ' + requests.join(', '));
      }
    }

    // 5. A second account, money in, and a move between the two.
    await press('Open an account');
    await press('Open');
    await settled();
    const checking = await waitFor('the checking account', stepSeconds, async () =>
        (await components()).find(c => /Checking/.test(c.label || c.text || '')
            && c.height < 200));
    await click(checking);
    await press('Add money');
    await typeInto(0, '100.00', 'any');
    await press('Add');
    await waitFor('the deposit', stepSeconds, async () => /100\.00/.test(await screenText()));
    say('opened a second account and added 100.00');
    await press('Move money');
    await typeInto(0, '25.00', 'any');
    await press('Review');
    await press('Send');
    await waitFor('the receipt', stepSeconds, async () => /Sent/.test(await screenText()));
    await noErrorDialog();
    await page.screenshot({ path: path.join(out, '4-receipt.png') });
    if (!requests.some(line => line === '200 POST /api/transfers')) {
      throw new Error('no transfer was made: ' + requests.join(', '));
    }
    say('moved 25.00 between own accounts');

    // 6. A fresh browser, and the passkey alone: no password, no code. The
    // token says so (amr "hwk"), and that is enough to register this browser
    // as a signing device without typing a password.
    await newPage();
    await page.goto(url, { waitUntil: 'load' });
    await press('Sign in with a passkey', startSeconds);
    await waitFor('the dashboard', stepSeconds, async () =>
        /Total balance/.test(await screenText()));
    if (!requests.includes('200 POST /login/webauthn')) {
      throw new Error('the passkey sign-in did not go through /login/webauthn: '
          + requests.join(', '));
    }
    say('signed in with the passkey');
    await press('Register this device');
    await waitFor('the device to be registered', stepSeconds, async () =>
        requests.includes('200 POST /api/devices'));
    await settled();
    await noErrorDialog();
    if (/Confirm it is you/.test(await screenText())) {
      throw new Error('a passkey sign-in was asked for its password');
    }
    await page.screenshot({ path: path.join(out, '5-device.png') });
    say('registered this browser as a device after a passkey sign-in');

    // 7. A reload keeps the session: the stored one opens behind the lock
    // (there is no PIN or biometric here, so the lock asks for the password)
    // instead of the app forgetting it and showing the sign-in form.
    await page.reload({ waitUntil: 'load' });
    await waitFor('the lock after a reload', startSeconds, async () =>
        /Bastion is locked/.test(await screenText()));
    await page.screenshot({ path: path.join(out, '6-reload.png') });
    say('a reload found the stored session');
  }

  if (tour) {
    await guideTour();
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
