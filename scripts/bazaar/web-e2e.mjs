// The Bazaar web app, hosted by its own server, driven in a headless browser.
//
//   URL=http://localhost:18081/ OUT=target/web-e2e node web-e2e.mjs
//
// It checks what a person opening the server's address gets: the app loads from
// the server straight into shopping, as a guest -- Discover, a store, a
// product, the cart -- and asks for a sign-in only at the checkout. The demo
// shopper signs in there, the guest cart comes along, and the order is placed;
// the live channel is open. Then it signs out (back to the guest shop), signs
// in as the demo admin from the Account tab and reaches the admin console.
// Last, a guest buys on the website through the HTML checkout.
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
  console.error('[bazaar-web] Playwright is not installed. Run `npm ci` in scripts/ and '
      + '`npx playwright install chromium`, or point NODE_PATH at a node_modules that has it.');
  console.error(String(err));
  process.exit(2);
}

const url = process.env.URL;
const out = process.env.OUT || 'target/web-e2e';
const email = process.env.BAZAAR_USER || 'shopper@bazaar.example';
const password = process.env.BAZAAR_PASSWORD || 'bazaar-demo';
const admin = process.env.BAZAAR_ADMIN || 'admin@bazaar.example';
// A cold runner translates nothing at this point, but it does parse several
// megabytes of JavaScript before the first screen.
const startSeconds = Number(process.env.BAZAAR_WEB_START_SECONDS || '120');
const stepSeconds = Number(process.env.BAZAAR_WEB_STEP_SECONDS || '45');
if (!url) {
  console.error('[bazaar-web] URL is required');
  process.exit(2);
}
fs.mkdirSync(out, { recursive: true });
const logFile = path.join(out, 'browser.log');
fs.writeFileSync(logFile, '');
const note = line => fs.appendFileSync(logFile, line + '\n');
const say = line => { console.log('[bazaar-web] ' + line); note('# ' + line); };

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
// A browser reports where it is; the storefront uses it only to estimate
// shipping, so any location near the seeded stores does.
// PHONE_HEIGHT and PHONE_SCALE size the phone screenshots; the Initializr
// preview is taken at 909 and 562/420 to match its siblings' 562x1217.
const page = await browser.newPage({ viewport: { width: 420, height: Number(process.env.PHONE_HEIGHT || 860) },
  deviceScaleFactor: Number(process.env.PHONE_SCALE || 1),
  geolocation: { latitude: 37.8080, longitude: -122.4120 }, permissions: ['geolocation'] });
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

/// Fills in the sign-in form (the prompt's Sign in button opens it), and sends it.
async function signInAs(who) {
  await settled();
  await click(await waitFor('the sign-in prompt', startSeconds, button('Sign in')));
  await typeInto(0, who, 'email');
  await typeInto(1, password, 'password');
  // The form's own button now: the prompt's is gone with its form.
  await settled();
  await click(await waitFor('the form\'s Sign in button', stepSeconds, async () =>
      (await fields()).length >= 2 ? button('Sign in')() : null));
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

/// The website's purchase path, by ordinary page navigation: a product of the
/// seeded store into the cart, the checkout form filled as a guest, the order
/// placed, and the confirmation page with its invoice.
// The storefront's own forms, each in a fresh browser context: a shopper
// signs up on the web (the bot guard refuses a form sent in under its minimum
// fill time, so the test waits it out like a person would), picks Spanish in
// the footer, and buys in a market's currency.
// Every Content Security Policy refusal in a context: the browser reports
// each as a securitypolicyviolation event, which the init script prints as a
// console error the listener collects. Read with cspProblems().
const csp = [];
async function watchCsp(context) {
  await context.addInitScript(() => {
    document.addEventListener('securitypolicyviolation', e => {
      console.error('[csp] ' + e.violatedDirective + ' ' + (e.blockedURI || 'inline')
          + ' ' + (e.sourceFile || '') + ':' + (e.lineNumber || 0));
    });
  });
  context.on('page', p => p.on('console', m => {
    if (m.type() === 'error' && /^\[csp\]|Content Security Policy/.test(m.text())) {
      csp.push(p.url() + ' ' + m.text().split('\n')[0]);
    }
  }));
}

async function storefrontExtras() {
  const store = process.env.BAZAAR_WEB_STORE || 'northwind';
  const context = await browser.newContext({ viewport: { width: 1100, height: 900 } });
  await watchCsp(context);
  const shop = await context.newPage();
  try {
    // Sign up.
    await shop.goto(origin + '/auth/signup', { waitUntil: 'load' });
    if (await shop.locator('input[name=_ft]').count() !== 1) {
      throw new Error('the sign-up form carries no timing token');
    }
    const email = 'web-e2e-' + Date.now() + '@example.com';
    await shop.fill('#email', email);
    await shop.fill('#displayName', 'Web Shopper');
    await shop.fill('#phone', '+1 555 010 4477');
    await shop.fill('#password', 'longenough1');
    await shop.fill('#confirm', 'longenough1');
    await shop.waitForTimeout(2000);
    await shop.locator('form[action="/auth/signup"] button[type=submit]').click();
    await shop.waitForURL(/\/auth\/signin\?created=1$/, { timeout: stepSeconds * 1000 });
    await shop.screenshot({ path: path.join(out, '9-signed-up.png') });
    say('signed up on the web as ' + email);

    // The language picker, a menu in the header: Spanish labels, and the
    // page says so.
    await shop.goto(origin + '/s/' + store, { waitUntil: 'load' });
    await shop.locator('details[data-pick]:has(form[action="/lang"]) > summary').click();
    if (await shop.locator('details[data-pick] > summary[aria-expanded=true]').count() !== 1) {
      throw new Error('the language menu does not say it is open');
    }
    await shop.locator('form[action="/lang"] button[value=es]').first().click();
    await shop.waitForLoadState('load');
    const lang = await shop.locator('html').getAttribute('lang');
    const dir = await shop.locator('html').getAttribute('dir');
    const words = await shop.locator('body').innerText();
    if (lang !== 'es' || dir !== 'ltr' || !/Español/.test(words)) {
      throw new Error('after picking Spanish the page has lang=' + lang + ' dir=' + dir);
    }
    await shop.screenshot({ path: path.join(out, '10-spanish.png') });
    await shop.locator('details[data-pick]:has(form[action="/lang"]) > summary').click();
    await shop.locator('form[action="/lang"] button[value=en]').first().click();
    await shop.waitForLoadState('load');
    say('the language picker switched the storefront to Spanish (lang=es dir=ltr)');

    // The back office is the app's. An old back-office address, which a
    // merchant may have bookmarked, lands on the app (/app/), never a page.
    for (const old of ['/admin', '/admin/x/orders', '/platform']) {
      const r = await shop.request.get(origin + old, { maxRedirects: 0 });
      if (r.status() !== 301 || r.headers()['location'] !== '/app/') {
        throw new Error(old + ' answered ' + r.status() + ' to ' + r.headers()['location']);
      }
    }
    say('old back-office addresses redirect to the app');

    // The floating chat: a shopper writes from the widget and the message
    // reaches the server (its tick). The owner answers in the app's inbox.
    const visitor = await browser.newContext({ viewport: { width: 1100, height: 900 } });
    await watchCsp(visitor);
    try {
      const v = await visitor.newPage();
      await v.goto(origin + '/s/' + store, { waitUntil: 'load' });
      await v.locator('#chat .chat-bubble').click();
      // A header menu opened while the chat is open draws above the chat.
      // On a phone the two overlap; the topmost element at the centre of each
      // menu entry is the entry itself, never the chat panel.
      // On a phone the open chat covers the header's own buttons, so the
      // menu is opened the way its summary would open it.
      await v.setViewportSize({ width: 390, height: 700 });
      await v.evaluate(() => {
        document.querySelector('details[data-pick]:has(form[action="/lang"])').open = true;
      });
      const covered = await v.evaluate(() => {
        const menu = document.querySelector('details[data-pick][open] .pick-menu');
        const panel = document.querySelector('#chat .chat-panel');
        if (!menu || !panel) {
          return 'no open menu or chat panel';
        }
        const a = menu.getBoundingClientRect();
        const c = panel.getBoundingClientRect();
        if (a.right <= c.left || c.right <= a.left || a.bottom <= c.top || c.bottom <= a.top) {
          return 'the menu and the chat do not overlap, so this proves nothing';
        }
        for (const b of menu.querySelectorAll('button')) {
          const r = b.getBoundingClientRect();
          const top = document.elementFromPoint(r.left + r.width / 2, r.top + r.height / 2);
          if (top && !menu.contains(top)) {
            return (top.closest('#chat') ? 'the chat' : top.tagName) + ' covers ' + b.textContent;
          }
        }
        return '';
      });
      if (covered) {
        throw new Error('the header menu is under something: ' + covered);
      }
      await v.screenshot({ path: path.join(out, '12-menu-over-chat.png') });
      await v.evaluate(() => document.querySelectorAll('details[data-pick][open]')
          .forEach(d => d.removeAttribute('open')));
      await v.setViewportSize({ width: 1100, height: 900 });
      say('a header menu opens above the support chat');
      const question = 'Does this come gift wrapped? ' + Date.now();
      await v.fill('#chat .chat-form textarea', question);
      await v.locator('#chat .chat-send').click();
      await v.waitForSelector('#chat .chat-msg.me[data-id] .chat-tick', { timeout: stepSeconds * 1000 });
      await v.screenshot({ path: path.join(out, '12-chat-sent.png') });
    } finally {
      await visitor.close();
    }
    say('a shopper chatted from the widget and the server took the message');
    if (csp.length > 0) {
      throw new Error('the strict policy refused something: ' + csp.slice(0, 3).join(' | '));
    }
  } finally {
    await context.close();
  }
}

async function htmlCheckout() {
  const store = process.env.BAZAAR_WEB_STORE || 'northwind';
  const context = await browser.newContext({ viewport: { width: 1100, height: 900 } });
  const shop = await context.newPage();
  const cspErrors = [];
  shop.on('console', m => {
    if (m.type() === 'error' && /Content Security Policy/i.test(m.text())) {
      cspErrors.push(m.text() + ' at ' + shop.url());
    }
  });
  try {
    const home = await shop.goto(origin + '/s/' + store, { waitUntil: 'load' });
    if (!home || home.status() !== 200) {
      throw new Error('the store page answered ' + (home ? home.status() : 'nothing'));
    }
    await shop.locator('a[href*="/products/"]').first().click();
    await shop.waitForLoadState('load');
    await shop.locator('form.add button[type=submit]').click();
    await shop.waitForURL(/\/cart$/);
    await shop.locator('a.checkout').click();
    await shop.waitForURL(/\/checkout$/);
    await shop.fill('#email', 'guest@bazaar.example');
    await shop.fill('#name', 'Grace Hopper');
    await shop.fill('#line1', '1 Main St');
    await shop.fill('#city', 'Albany');
    await shop.fill('#region', 'NY');
    await shop.fill('#postal', '12207');
    // Changing the country asks for the rates again (htmx swaps the summary).
    await shop.selectOption('#country', 'CA');
    await shop.selectOption('#country', 'US');
    await shop.waitForSelector('#summary input[name=rateId]', { timeout: stepSeconds * 1000 });
    await shop.fill('#card-number', '4242 4242 4242 4242');
    await shop.fill('#card-expiry', '12/39');
    await shop.fill('#card-cvc', '123');
    await shop.screenshot({ path: path.join(out, '7-checkout.png') });
    await shop.locator('button.place').click();
    await shop.waitForURL(/\/orders\/\d+\?t=[0-9a-f]{32}$/, { timeout: stepSeconds * 1000 });
    const heading = await shop.locator('h1').innerText();
    if (!heading.includes('Thank you')) {
      throw new Error('the confirmation page says "' + heading + '"');
    }
    const invoice = await shop.locator('a[href*="invoice.pdf"]').getAttribute('href');
    const pdf = await shop.request.get(origin + invoice);
    if (pdf.status() !== 200 || !(pdf.headers()['content-type'] || '').startsWith('application/pdf')) {
      throw new Error('the invoice answered ' + pdf.status());
    }
    await shop.screenshot({ path: path.join(out, '8-confirmation.png') });
    if (cspErrors.length > 0) {
      throw new Error('the checkout broke its content security policy: ' + cspErrors[0]);
    }
    say('a guest bought through the HTML checkout: ' + heading);
  } finally {
    await context.close();
  }
}

/// "Open the app" on a storefront page opens that page in the app: the store
/// page's link names the store, a product page's the product as well, and the
/// app follows it to the product.
async function appDeepLink() {
  const context = await browser.newContext({ viewport: { width: 1100, height: 900 } });
  try {
    const shop = await context.newPage();
    await shop.goto(origin + '/s/littleclay', { waitUntil: 'load' });
    await shop.screenshot({ path: path.join(out, '13-storefront-home.png') });
    const storeLink = await shop.getAttribute('a.app-link', 'href');
    if (storeLink !== '/app/?store=littleclay') {
      throw new Error('the store page opens the app at ' + storeLink);
    }
    await shop.goto(origin + '/s/littleclay/products/speckled-morning-mug', { waitUntil: 'load' });
    await shop.screenshot({ path: path.join(out, '14-storefront-product.png') });
    const productLink = await shop.getAttribute('a.app-link', 'href');
    if (productLink !== '/app/?store=littleclay&product=speckled-morning-mug') {
      throw new Error('the product page opens the app at ' + productLink);
    }
    await shop.click('a.app-link');
    const end = Date.now() + startSeconds * 1000;
    for (;;) {
      const found = await shop.evaluate(() => Array.from(document.querySelectorAll(
          '#cn1-accessibility-tree [data-cn1-accessibility-id]')).some(el =>
          el.getAttribute('role') === 'button' && el.getAttribute('aria-label') === 'Add to cart')
          && document.body.innerText.includes('Speckled Morning Mug'));
      if (found) {
        break;
      }
      if (Date.now() > end) {
        throw new Error('the app did not open the product the storefront linked to');
      }
      await shop.waitForTimeout(500);
    }
    await shop.screenshot({ path: path.join(out, '15-app-deep-link.png') });
    say('"Open the app" on a product page opened that product in the app');
  } finally {
    await context.close();
  }
}

/// The product page as a guest on a phone, in a browser of its own: a gallery
/// dot jumps to its picture, a variant chip is selected, Share copies the link
/// (plain http has no Web Share), the rating opens every review, and the link
/// a cart reminder carries (`/app/?store=...&cart=...`) brings the cart back in
/// a browser that never saw it.
async function productControls() {
  const context = await browser.newContext({ viewport: { width: 420, height: 860 } });
  try {
    const shop = await context.newPage();
    const tokens = [];
    // The guest token is in the cart the server answers (the path names
    // the store, not the cart).
    shop.on('response', async r => {
      if (r.request().method() === 'POST' && /\/api\/shop\/cart\/[^/]+\/lines/.test(r.url())) {
        try {
          const token = (await r.json()).token;
          if (token) {
            tokens.push(token);
          }
        } catch (err) {
          // not a cart: ignored
        }
      }
    });
    const all = () => shop.evaluate(() => Array.from(document.querySelectorAll(
        '#cn1-accessibility-tree [data-cn1-accessibility-id]')).map(el => {
      const box = el.getBoundingClientRect();
      return { role: el.getAttribute('role'), label: el.getAttribute('aria-label'),
        text: el.innerText, x: box.x, y: box.y, width: box.width, height: box.height };
    }));
    const wait = async (what, seconds, probe) => {
      const end = Date.now() + seconds * 1000;
      for (;;) {
        const hit = probe(await all());
        if (hit) {
          return hit;
        }
        if (Date.now() > end) {
          throw new Error(`timed out after ${seconds}s waiting for ${what}`);
        }
        await shop.waitForTimeout(250);
      }
    };
    const tap = async c => {
      await shop.mouse.click(c.x + c.width / 2, c.y + c.height / 2);
      await shop.waitForTimeout(700);
    };
    const labelled = label => list => list.find(c => c.role === 'button' && c.label === label);
    const shot = name => shop.screenshot({ path: path.join(out, name + '.png') });

    await shop.goto(origin + '/app/?store=littleclay&product=speckled-morning-mug', { waitUntil: 'load' });
    const add = await wait('the product page', startSeconds, labelled('Add to cart'));
    await shop.waitForTimeout(1500);
    await tap(await wait('the second gallery dot', stepSeconds, labelled('Picture 2 of 2')));
    await wait('the second picture to show', stepSeconds, labelled('Picture 2 of 2, showing'));
    await shot('16-gallery-dot');
    await tap(await wait('the Sea glass chip', stepSeconds, labelled('Sea glass')));
    await wait('Sea glass to be selected', stepSeconds, labelled('Sea glass, selected'));
    await shot('17-variant-chip');
    await tap(await wait('the Share button', stepSeconds, labelled('Share')));
    await wait('the link to be copied', stepSeconds, list => list.some(c => c.text === 'Link copied'
        || c.label === 'Link copied'));
    say('a gallery dot, a variant chip and Share (copied over http) work on the product page');
    await shop.waitForTimeout(3500);
    await tap(add);
    await wait('the guest cart on the server', stepSeconds, () => tokens.length > 0);
    await tap(await wait('the rating row', stepSeconds, list => list.find(c => c.text === 'See reviews'
        || (typeof c.label === 'string' && c.label.indexOf('See reviews') >= 0))));
    await wait('the reviews list', stepSeconds, labelled('Write a review'));
    await shot('18-reviews');
    say('the rating opened every review of the product');

    const fresh = await browser.newContext({ viewport: { width: 420, height: 860 } });
    try {
      const other = await fresh.newPage();
      await other.goto(origin + '/app/?store=littleclay&cart=' + tokens[0], { waitUntil: 'load' });
      const end = Date.now() + startSeconds * 1000;
      while (!(await other.evaluate(() => Array.from(document.querySelectorAll(
          '#cn1-accessibility-tree [data-cn1-accessibility-id]')).some(el =>
          el.getAttribute('role') === 'button' && el.getAttribute('aria-label') === 'Checkout')
          && document.body.innerText.includes('Speckled Morning Mug')))) {
        if (Date.now() > end) {
          throw new Error('the cart link did not bring the cart back');
        }
        await other.waitForTimeout(500);
      }
      await other.screenshot({ path: path.join(out, '19-cart-link.png') });
      say('the cart reminder link restored the cart in a new browser');
    } finally {
      await fresh.close();
    }
  } finally {
    await context.close();
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

  const shot = name => page.screenshot({ path: path.join(out, name + '.png') });
  const textOn = words => async () => (await screenText()).includes(words);
  const named = words => async () =>
      // A tappable card reads its whole content as one label ("Little Clay
      // Studio, Handmade ceramics for every day"), so its first part counts.
      (await components()).find(c => c.text === words || c.label === words
          || (typeof c.label === 'string' && c.label.startsWith(words + ', ')));

  // 1. No sign-in wall: the app opens on Discover, as a guest, with the
  //    platform's stores and the newest products across them.
  await waitFor('Discover', startSeconds, textOn('New across Bazaar'));
  await waitFor('a store on Discover', stepSeconds, named('Little Clay Studio'));
  for (const file of ['/app/translated_app.js', '/app/port.js', '/app/worker.js']) {
    if (!requests.includes('200 GET ' + file)) {
      throw new Error(file + ' was not served by this server: ' + requests.join(', '));
    }
  }
  if (requests.some(r => r.includes('/api/me'))) {
    throw new Error('a guest asked who it is: ' + requests.filter(r => r.includes('/api/me')));
  }
  await waitFor('the feed across stores', stepSeconds, async () =>
      requests.some(r => r.startsWith('200 GET /api/shop/discover')));
  await page.waitForTimeout(2000);
  await settled();
  await shot('1-home');
  say('the app opened on Discover as a guest');

  // 2. A store's page.
  await click(await waitFor('Little Clay on Discover', stepSeconds, named('Little Clay Studio')));
  await waitFor('the store page', stepSeconds, textOn('New arrivals'));
  await page.waitForTimeout(1500);
  await settled();
  await shot('2-store');
  say('opened the Little Clay Studio store page');

  // 3. A product, into the cart, as a guest.
  // A card's name: the first line of words under the "New arrivals" heading.
  const productCard = async () => {
    const all = await components();
    const heading = all.filter(c => c.text === 'New arrivals').sort((a, b) => b.y - a.y)[0];
    // A product card is one accessible button now, its label the whole card.
    return heading && all.find(c => c.y > heading.y + 60 && c.y < 780 && c.height > 15
        && (c.text || c.label) && !(c.text || c.label).startsWith('$'));
  };
  await click(await waitFor('a product card', stepSeconds, productCard));
  const add = await waitFor('the product page', stepSeconds, button('Add to cart'));
  await page.waitForTimeout(1500);
  await settled();
  await shot('3-product');
  await click(add);
  await waitFor('the guest cart on the server', stepSeconds, async () =>
      requests.some(r => /^200 POST \/api\/shop\/cart\/[^/]+\/lines/.test(r)));
  say('a guest put a product in the cart');
  await page.waitForTimeout(800);
  await page.keyboard.press('Escape');
  await settled();
  // Back in history only while the product page is still up. Escape may
  // already have closed it (it does on a taller, denser screen), and then
  // the browser's Back leaves the app for the page before it: a blank tab
  // with no Cart tab to find.
  if (!(await button('Cart')())) {
    await page.goBack().catch(() => {});
    await settled();
  }
  await click(await waitFor('the Cart tab', stepSeconds, button('Cart')));
  const checkout = await waitFor('the cart', stepSeconds, button('Checkout'));
  await settled();
  await shot('4-cart');

  // 4. The checkout is where a guest is first asked to sign in.
  await click(checkout);
  await waitFor('the sign-in prompt at checkout', stepSeconds, textOn('Sign in to check out'));
  await settled();
  await shot('5-sign-in-at-checkout');
  say('the checkout asked the guest to sign in');
  await signInAs(email);
  // The first sign-in in a browser is asked whether usage statistics may be
  // shared. The answer is kept with the page's storage, so the admin's
  // sign-in further down is not asked again.
  await click(await waitFor('the usage statistics question', stepSeconds, button('Not now')));
  say('declined usage statistics');
  const expected = ['200 POST /login', '200 GET /signin/code', '200 POST /oauth2/token',
    '200 GET /api/me'];
  for (const line of expected) {
    if (!requests.includes(line)) {
      throw new Error('signing in did not make the request "' + line + '"; it made: '
          + requests.filter(r => !/\.(js|css|png|res|gif|json|ttf)$/.test(r)).join(', '));
    }
  }
  await waitFor('the guest cart merged into the account', stepSeconds, async () =>
      requests.some(r => /^200 POST \/api\/cart\/[^/]+\/merge/.test(r)));

  // 5. The checkout carried on after the sign-in. A first order types the
  //    address (Enter in the postal code continues); cash is the default.
  await waitFor('the checkout', stepSeconds, textOn('Shipping address'));
  await settled();
  await shot('6-checkout');
  if (!(await screenText()).includes('Or a new address')) {
    await typeInto(1, '1 Market St', 'text');
    await typeInto(3, 'San Francisco', 'text');
    await typeInto(4, 'CA', 'text');
    await typeInto(5, '94105', 'text');
    await page.keyboard.press('Enter');
  }
  const place = await waitFor('Place order to be ready', stepSeconds * 2, async () => {
    const b = await button('Place order')();
    return b && requests.some(r => r.startsWith('200 POST /api/shop/cart/') && r.endsWith('/estimate'))
        && requests.some(r => r === '200 GET /api/payments/methods') ? b : null;
  });
  await page.waitForTimeout(1500);
  await settled();
  // The button is at the foot of a long form: scroll it into view first.
  let ready = (await button('Place order')()) || place;
  for (let turn = 0; turn < 20 && ready.y + ready.height > 790; turn++) {
    await page.mouse.move(210, 400);
    await page.mouse.wheel(0, 400);
    await page.waitForTimeout(400);
    ready = (await button('Place order')()) || ready;
  }
  await settled();
  ready = (await button('Place order')()) || ready;
  await shot('7-review');
  await click(ready);
  await waitFor('the order to be placed', stepSeconds, async () =>
      requests.some(r => /^200 POST \/api\/checkout\//.test(r)) ? named('View order')() : null);
  await settled();
  await shot('8-order-placed');
  say('signed in at checkout and placed the order');
  await click(await named('View order')());
  await page.waitForTimeout(2000);
  await settled();
  await shot('9-order-tracking');
  // Back to the tabs, by the order's own back arrow (top left).
  await page.mouse.click(26, 22);
  await waitFor('the tabs after the order', stepSeconds, button('Account'));

  // 4. The live channel, a WebSocket to the same origin.
  const live = await waitFor('the live channel', stepSeconds, async () =>
      sockets.find(s => s.frames.some(f => f.includes('"ready"'))));
  const expectedSocket = origin.replace(/^http/, 'ws') + '/ws/live?ticket=';
  if (!live.url.startsWith(expectedSocket)) {
    throw new Error('the live channel went to ' + live.url + ', not to ' + expectedSocket);
  }
  say('the live channel is open');

  // Give a late failure -- the first location request, the first map tile --
  // the moment it needs to show itself.
  await page.waitForTimeout(3000);
  await noErrorDialog();
  await shot('10-settled');

  // 5. The session that carried the sign-in is over: the app ended it when it
  //    had its tokens, so the cookie the browser still holds is worth nothing.
  await waitFor('the app to end its sign-in session', stepSeconds, async () =>
      requests.includes('204 POST /logout'));
  const again = await page.evaluate(async () => (await fetch('/oauth2/authorize?response_type=code'
      + '&client_id=bazaar-app&scope=openid&state=x&code_challenge_method=S256'
      + '&code_challenge=E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM'
      + '&redirect_uri=' + encodeURIComponent(location.origin + '/signin/code'),
      { headers: { Accept: 'application/json' } })).status);
  if (again === 200) {
    throw new Error('the browser can still get an authorization code after signing in');
  }
  say('the sign-in session is over (asking for another code is answered ' + again + ')');

  // 6. Signing out, from the Account tab. (The title bar's overflow menu has
  //    the same entry, but its popup is not in the accessibility tree the
  //    test reads, so it cannot be found by its words.)
  await settled();
  // Follow a store from the front page's store cards, then find it on the
  // Account tab, under "Stores you follow", and unfollow it there.
  await click(await waitFor('the Home tab', stepSeconds, button('Home')));
  await settled();
  let follow = null;
  for (let i = 0; i < 12 && !(follow = await button('Follow')()); i++) {
    await page.mouse.move(210, 400);
    await page.mouse.wheel(0, 400);
    await page.waitForTimeout(500);
  }
  if (!follow) {
    throw new Error('no Follow button on a store card');
  }
  // Found is not the same as on screen: a card half under the tab bar is
  // in the accessibility tree, and a click there lands on something else.
  const homeH = page.viewportSize().height;
  for (let i = 0; i < 12 && (follow.y < 60 || follow.y + follow.height > homeH - 90); i++) {
    await page.mouse.move(210, 400);
    await page.mouse.wheel(0, follow.y < 60 ? -300 : 300);
    await page.waitForTimeout(500);
    follow = await waitFor('a Follow button on a store card', stepSeconds, button('Follow'));
  }
  await click(follow);
  await waitFor('the follow to be saved', stepSeconds, async () =>
      requests.some(r => /^20\d PUT \/api\/follows\//.test(r)));
  await click(await waitFor('the Account tab', stepSeconds, button('Account')));
  await settled();
  const heading = words => async () => (await components()).find(c => c.label === words || c.text === words);
  await waitFor('Stores you follow on the Account tab', stepSeconds, heading('Stores you follow'));
  await waitFor('the returns request', stepSeconds, async () => requests.includes('200 GET /api/returns'));
  await waitFor('Returns on the Account tab', stepSeconds, heading('Returns'));
  // The Account tab is taller than the window (rewards, addresses, orders),
  // so scroll until the Unfollow button is on screen before clicking it.
  let unfollow = await waitFor('Unfollow beside the store followed', stepSeconds, button('Unfollow'));
  const viewH = page.viewportSize().height;
  for (let i = 0; i < 12 && (unfollow.y < 60 || unfollow.y + unfollow.height > viewH - 90); i++) {
    await page.mouse.move(210, 400);
    await page.mouse.wheel(0, unfollow.y < 60 ? -300 : 300);
    await page.waitForTimeout(500);
    unfollow = await waitFor('Unfollow beside the store followed', stepSeconds, button('Unfollow'));
  }
  await click(unfollow);
  await waitFor('the unfollow to be saved', stepSeconds, async () =>
      requests.some(r => /^20\d DELETE \/api\/follows\//.test(r)));
  await waitFor('the store to leave the list', stepSeconds, async () => !(await button('Unfollow')()));
  say('followed a store, found it on the account, and unfollowed it there');
  await page.mouse.move(210, 400);
  await page.mouse.wheel(0, -3000);
  await settled();
  // Sign out is the last row, and on a phone it can sit under the tab bar:
  // a click at its place would land on a tab. Scroll until it is clear.
  const signOut = async () => (await components()).find(c => c.label === 'Sign out' || c.text === 'Sign out');
  await waitFor('Sign out on the Account tab', stepSeconds, signOut);
  for (let i = 0; i < 10 && (await signOut()).y + (await signOut()).height > page.viewportSize().height - 90; i++) {
    await page.mouse.move(210, 400);
    await page.mouse.wheel(0, 300);
    await page.waitForTimeout(400);
  }
  await settled();
  await click(await signOut());
  // Back to the guest shop, whose Account tab offers a sign-in again.
  await waitFor('the guest Account tab after signing out', stepSeconds, button('Create an account'));
  await settled();
  await noErrorDialog();
  await shot('11-signed-out');
  say('signed out: back to shopping as a guest');

  // 7. The admin, who signs in from Account and lands in the console.
  await signInAs(admin);
  await waitFor('the admin console', stepSeconds, async () => {
    // The console opens on its Overview tab, figures first.
    const text = await screenText();
    // A caption goes onto two lines where its tile is narrow.
    return /Overview/.test(text) && /Gross\s+sales/.test(text);
  });
  await waitFor('the console to ask the server for its figures', stepSeconds, async () =>
      requests.some(r => /^200 GET \/api\/admin\//.test(r)));
  await page.waitForTimeout(3000);
  await settled();
  await noErrorDialog();
  await shot('12-admin');
  say('signed in as ' + admin + ': the admin console is up');

  // 8. A guest buys on the website: the server-rendered storefront and its
  //    HTML checkout, in a browser of its own (no app, no account).
  await htmlCheckout();

  // 9. The storefront's sign-up, language picker and markets.
  await storefrontExtras();

  // 10. The storefront's "Open the app" link, into the app on that product.
  await appDeepLink();

  // 11. The product page's controls, and the cart reminder's link.
  await productControls();
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
  console.error('[bazaar-web] FAILED: ' + failed.message);
  console.error('[bazaar-web] the browser log is ' + logFile);
  process.exit(1);
}
say('passed');
