// Records the server-rendered web walkthroughs with a CDP screencast.
// ORIGIN=http://localhost:18097 OUT=dir SCHEME=light|dark FLOW=storefront|account|backoffice node web-rec.mjs
import fs from 'node:fs';
import path from 'node:path';
import { createRequire } from 'node:module';
const require = createRequire(import.meta.url);
const { chromium } = require('playwright');

const origin = process.env.ORIGIN;
const out = process.env.OUT;
const scheme = process.env.SCHEME || 'light';
const flow = process.env.FLOW;
const sfx = scheme === 'dark' ? '-dark' : '';
const frames = path.join(out, 'frames-' + flow + sfx);
fs.rmSync(frames, { recursive: true, force: true });
fs.mkdirSync(frames, { recursive: true });

const browser = await chromium.launch({ headless: true });
const context = await browser.newContext({ userAgent: 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/140.0.0.0 Safari/537.36', viewport: { width: 1440, height: 810 }, deviceScaleFactor: 4 / 3,
  colorScheme: scheme });
await context.addInitScript(() => {
  const css = (el, s) => Object.assign(el.style, s);
  const boot = () => {
    for (const a of document.querySelectorAll('a')) { if (/^\s*Skip to/.test(a.textContent)) a.style.display = 'none'; }
    if (document.getElementById('__rec_dot')) return;
    const dot = document.createElement('div');
    dot.id = '__rec_dot';
    css(dot, { position: 'fixed', left: (window.__recX || 720) + 'px', top: (window.__recY || 400) + 'px', width: '22px', height: '22px',
      margin: '-11px 0 0 -11px', borderRadius: '50%', background: 'rgba(90,110,255,0.45)', border: '2px solid rgba(70,90,230,0.9)',
      zIndex: 2147483647, pointerEvents: 'none', transition: 'transform 120ms' });
    const cap = document.createElement('div');
    cap.id = '__rec_cap';
    css(cap, { position: 'fixed', left: '50%', bottom: '28px', transform: 'translateX(-50%)', maxWidth: '70%',
      padding: '10px 20px', borderRadius: '10px', background: 'rgba(20,20,24,0.88)', color: '#fff',
      font: '500 17px system-ui, -apple-system, Segoe UI, Roboto, sans-serif', textAlign: 'center',
      zIndex: 2147483646, pointerEvents: 'none', opacity: '0', transition: 'opacity 250ms' });
    document.documentElement.appendChild(dot);
    document.documentElement.appendChild(cap);
    const c = sessionStorage.getItem('__rec_cap');

    document.addEventListener('mousemove', e => { window.__recX = e.clientX; window.__recY = e.clientY;
      sessionStorage.setItem('__rec_xy', e.clientX + ',' + e.clientY); dot.style.left = e.clientX + 'px'; dot.style.top = e.clientY + 'px'; }, true);
    document.addEventListener('mousedown', () => { dot.style.transform = 'scale(0.7)'; }, true);
    document.addEventListener('mouseup', () => { dot.style.transform = 'scale(1)'; }, true);
    const xy = (sessionStorage.getItem('__rec_xy') || '').split(',');
    if (xy.length === 2) { dot.style.left = xy[0] + 'px'; dot.style.top = xy[1] + 'px'; }
  };
  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', boot); else boot();
  document.addEventListener('htmx:afterSettle', boot);
});
const page = await context.newPage();
const errors = [];
page.on('pageerror', e => errors.push(String(e)));
page.on('console', m => { if (m.type() === 'error') errors.push(m.text()); });

// Screencast.
const cdp = await context.newCDPSession(page);
const list = [];
let n = 0;
cdp.on('Page.screencastFrame', async f => {
  const file = path.join(frames, String(n++).padStart(6, '0') + '.jpg');
  fs.writeFileSync(file, Buffer.from(f.data, 'base64'));
  list.push([file, f.metadata.timestamp]);
  try { await cdp.send('Page.screencastFrameAck', { sessionId: f.sessionId }); } catch (e) { /* closed */ }
});

let mx = 720, my = 400;
const wait = ms => page.waitForTimeout(ms);
async function caption(text, ms = 2600) {
  await page.waitForLoadState('load');
  await page.evaluate(() => { const a = document.activeElement; if (a && a !== document.body && a.tagName === 'A') a.blur(); });
  await page.evaluate(t => { sessionStorage.setItem('__rec_cap', t); const c = document.getElementById('__rec_cap');
    if (c) { c.textContent = t; c.style.opacity = '1'; } }, text);
  await wait(ms);
}
async function moveTo(x, y) {
  const steps = 22;
  for (let i = 1; i <= steps; i++) {
    const t = i / steps, e = t < 0.5 ? 2 * t * t : 1 - Math.pow(-2 * t + 2, 2) / 2;
    await page.mouse.move(mx + (x - mx) * e, my + (y - my) * e);
    await wait(14);
  }
  mx = x; my = y;
}
async function click(sel, opts = {}) {
  const loc = typeof sel === 'string' ? page.locator(sel).first() : sel;
  await loc.scrollIntoViewIfNeeded();
  const b = await loc.boundingBox();
  if (!b) throw new Error('no box for ' + sel);
  await moveTo(b.x + Math.min(b.width / 2, 60), b.y + b.height / 2);
  await wait(250);
  if (opts.nav) {
    await page.evaluate(() => { const c = document.getElementById('__rec_cap'); if (c) c.style.opacity = '0'; });
    await Promise.all([page.waitForLoadState('load'), loc.click()]);
  } else {
    await loc.click();
  }
  await wait(opts.after ?? 1200);
}
async function type(sel, text) {
  await click(sel, { after: 200 });
  await page.locator(sel).first().fill('');
  await page.locator(sel).first().pressSequentially(text, { delay: 45 });
  await wait(300);
}
async function scroll(px, ms = 900) {
  const steps = 20;
  for (let i = 0; i < steps; i++) { await page.mouse.wheel(0, px / steps); await wait(ms / steps); }
  await wait(400);
}
async function go(url) { await page.goto(origin + url, { waitUntil: 'load' }); await wait(600); }
async function shot(name) {
  await page.evaluate(() => { for (const id of ['__rec_dot', '__rec_cap']) { const e = document.getElementById(id); if (e) e.style.visibility = 'hidden'; } });
  await page.screenshot({ path: path.join(out, 'bazaar-' + name + '.png') });
  await page.evaluate(() => { for (const id of ['__rec_dot', '__rec_cap']) { const e = document.getElementById(id); if (e) e.style.visibility = 'visible'; } });
}
async function signIn(user) {
  await go('/auth/signin');
  await caption('Sign in', 1200);
  await type('input[name=username]', user);
  await type('input[name=password]', 'bazaar-demo');
  await click('form button[type=submit]', { nav: true, after: 1500 });
}
async function storeId() {
  await go('/admin');
  const links = await page.locator('a[href^="/admin/"]').evaluateAll(as => as.map(a => a.getAttribute('href')));
  return links.map(h => (h.match(/^\/admin\/([^/?#]+)/) || [])[1]).find(id => id && id.length > 8);
}

async function storefront() {
  await go('/');
  await cdp.send('Page.startScreencast', { format: 'jpeg', quality: 92, everyNthFrame: 1 });
  await caption('Bazaar on the web: one marketplace, many independent shops', 3200);
  if (!sfx) await shot('web-marketplace');
  await scroll(500);
  await scroll(-500);
  await click('a[href="/s/northwind"]', { nav: true, after: 1500 });
  await caption('Each store has its own storefront, rendered on the server', 3000);
  await shot('web-store' + sfx);
  await scroll(600, 1400);
  await wait(800);
  await type('input[type=search], input[name=q]', 'merino');
  await Promise.all([page.waitForURL(u => u.toString().includes('merino')), page.keyboard.press('Enter')]);
  await page.waitForLoadState('load');
  await wait(500);
  await caption('Search, with sorting by price, newest and rating', 3000);
  if (!sfx) await shot('web-search');
  await click(page.locator('a[href*="/products/"]').first(), { nav: true, after: 1500 });
  await caption('A product page: options, reviews and a one-step wallet', 3000);
  if (!sfx) await shot('web-product');
  await scroll(500, 1200);
  await scroll(-500, 800);
  await click('form.add button[type=submit]', { nav: true, after: 1500 });
  await caption('The cart: quantities, discount codes and gift cards', 3000);
  if (!sfx) await shot('web-cart');
  await click('a.checkout', { nav: true, after: 1200 });
  await caption('Checkout as a guest: address, delivery and payment on one page', 2200);
  await type('#email', 'guest@example.com');
  await type('#name', 'Grace Hopper');
  await type('#line1', '1 Main St');
  await type('#city', 'Albany');
  await type('#region', 'NY');
  await type('#postal', '12207');
  await page.selectOption('#country', 'CA');
  await page.selectOption('#country', 'US');
  await page.waitForSelector('#summary input[name=rateId]', { timeout: 30000 });
  await wait(800);
  await type('#card-number', '4242 4242 4242 4242');
  await type('#card-expiry', '12/39');
  await type('#card-cvc', '123');
  if (!sfx) await shot('web-checkout');
  await click('button.place', { after: 0 });
  await page.waitForURL(/\/orders\/\d+/, { timeout: 30000 });
  await wait(1200);
  await caption('The order is placed, with its invoice and tracking', 3200);
  if (!sfx) await shot('web-order-placed');
  await scroll(400, 1000);
  await wait(1500);
}

async function account() {
  // Off camera: the shopper signs in and buys something, so the account has an order.
  await go('/auth/signin');
  await page.fill('input[name=username]', 'shopper@bazaar.example');
  await page.fill('input[name=password]', 'bazaar-demo');
  await Promise.all([page.waitForLoadState('load'), page.locator('form button[type=submit]').first().click()]);
  await go('/s/northwind/products/merino-base-layer-crew');
  if (!(await page.locator('form.add button[type=submit]').count())) { await go('/s/northwind'); await page.locator('a[href*="/products/"]').first().click(); await page.waitForLoadState('load'); }
  await page.locator('form.add button[type=submit]').click();
  await page.waitForURL(/\/cart$/);
  await page.locator('a.checkout').click();
  await page.waitForURL(/\/checkout$/);
  const fillIf = async (sel, v) => { const l = page.locator(sel); if (await l.count() && await l.isVisible() && !(await l.inputValue())) await l.fill(v); };
  await fillIf('#email', 'shopper@bazaar.example'); await fillIf('#name', 'Riley Shopper'); await fillIf('#line1', '1 Market St');
  await fillIf('#city', 'San Francisco'); await fillIf('#region', 'CA'); await fillIf('#postal', '94105');
  await page.selectOption('#country', 'CA'); await page.selectOption('#country', 'US');
  await page.waitForSelector('#summary input[name=rateId]', { timeout: 30000 });
  await fillIf('#card-number', '4242 4242 4242 4242'); await fillIf('#card-expiry', '12/39'); await fillIf('#card-cvc', '123');
  await page.locator('button.place').click();
  await page.waitForURL(/\/orders\/\d+/, { timeout: 30000 });
  await go('/account');
  await cdp.send('Page.startScreencast', { format: 'jpeg', quality: 92, everyNthFrame: 1 });
  if (!page.url().includes('/account')) await go('/account');
  await caption('One account across every store on the marketplace', 3000);
  if (!sfx) await shot('web-account');
  await click('a[href="/account/orders"]', { nav: true });
  await caption('Every order, from every store', 2800);
  if (!sfx) await shot('web-account-orders');
  await click(page.locator('a[href^="/account/orders/"]').first(), { nav: true });
  await caption('An order: its items, delivery, invoice and returns', 3000);
  if (!sfx) await shot('web-account-order');
  await scroll(500, 1200);
  await go('/account');
  await click('a[href="/account/loyalty"]', { nav: true });
  await caption('Loyalty points and tiers', 2800);
  if (!sfx) await shot('web-account-loyalty');
  await click('a[href="/account/security"]', { nav: true });
  await caption('Security: passkeys, two-step sign-in and sessions', 3000);
  if (!sfx) await shot('web-account-security');
  await scroll(400, 1000);
  await wait(1200);
}

async function backoffice() {
  await go('/auth/signin');
  await cdp.send('Page.startScreencast', { format: 'jpeg', quality: 92, everyNthFrame: 1 });
  await caption('The store owner signs in to the back office', 1500);
  await type('input[name=username]', 'owner@bazaar.example');
  await type('input[name=password]', 'bazaar-demo');
  await click('form button[type=submit]', { nav: true, after: 800 });
  const id = await storeId();
  if (!id) throw new Error('no store for the owner');
  const a = '/admin/' + id;
  await go(a);
  await caption('The back office in a browser: the owner\'s dashboard', 3200);
  await shot('web-admin-dashboard' + sfx);
  const nav = async (p, text, name, ms = 2800) => {
    const l = page.locator('a[href="' + a + p + '"]').first();
    if (await l.count()) await click(l, { nav: true }); else await go(a + p);
    await caption(text, ms);
    if (name && !sfx) await shot(name);
  };
  await nav('/orders', 'Orders: filter, bulk fulfil, buy labels, print packing slips', 'web-admin-orders');
  await click(page.locator('a[href^="' + a + '/orders/"]').first(), { nav: true });
  await caption('An order: payment, fulfilment, refunds and its timeline', 3000);
  if (!sfx) await shot('web-admin-order');
  await scroll(700, 1500);
  await nav('/products', 'Products, with variants, media and translations', 'web-admin-products');
  await click(page.locator('a[href^="' + a + '/products/"]').first(), { nav: true });
  await caption('The product editor', 2600);
  if (!sfx) await shot('web-admin-product');
  await scroll(600, 1300);
  await nav('/inventory', 'Inventory by location', 'web-admin-inventory');
  await nav('/fulfilment', 'Fulfilment: pick, pack and ship', 'web-admin-fulfilment');
  await nav('/customers', 'Customers and their history', 'web-admin-customers');
  await nav('/discounts', 'Discounts, codes and automatic offers', 'web-admin-discounts');
  await nav('/inbox', 'The inbox: every conversation with shoppers', 'web-admin-inbox');
  await nav('/sales', 'Sales: what sold, where and when', 'web-admin-sales');
  await nav('/reports', 'Reports, built from any table', 'web-admin-reports');
  await nav('/pipeline', 'The sales pipeline for B2B deals', null);
  await shot('web-admin-pipeline' + sfx);
  await wait(1200);
}

let ok = true;
try {
  if (flow === 'storefront') await storefront();
  else if (flow === 'account') await account();
  else await backoffice();
} catch (e) {
  ok = false;
  console.error('FAILED', e.message);
  await page.screenshot({ path: path.join(out, 'failure-' + flow + sfx + '.png') });
}
try { await cdp.send('Page.stopScreencast'); } catch (e) { /* */ }
await wait(300);
await browser.close();
// concat list with real durations
let txt = '';
for (let i = 0; i < list.length; i++) {
  const d = i + 1 < list.length ? Math.max(0.001, list[i + 1][1] - list[i][1]) : 0.5;
  txt += "file '" + list[i][0] + "'\nduration " + d.toFixed(4) + '\n';
}
if (list.length) txt += "file '" + list[list.length - 1][0] + "'\n";
fs.writeFileSync(path.join(out, 'list-' + flow + sfx + '.txt'), txt);
console.log('frames', list.length, 'errors', errors.slice(0, 5).join(' | '));
process.exit(ok ? 0 : 1);
