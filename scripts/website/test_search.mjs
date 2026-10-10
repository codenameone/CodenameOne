#!/usr/bin/env node
// Run against the minified website served by Cloudflare Pages, including API indexes.
import assert from 'node:assert/strict';
import fs from 'node:fs';
import { chromium } from 'playwright';

const base = process.argv[2] || 'http://localhost:8794';
const browser = await chromium.launch({ headless: true });
const page = await browser.newPage();
const errors = [];
page.on('pageerror', error => errors.push(error.message));
const ready = async () => {
  await page.waitForFunction(() => {
    const status = document.querySelector('#cn1-search-status')?.textContent || '';
    return status && !/Loading|Searching/.test(status);
  }, undefined, { timeout: 60000 });
};
const query = async value => {
  await page.locator('#cn1-search-input').fill(value);
  await page.waitForTimeout(250);
  await ready();
};
try {
  await page.goto(base + '/search/?scope=guide&q=build%20hints');
  await ready();
  const consent = page.getByRole('button', { name: 'Keep Crisp Disabled', exact: true });
  if (await consent.isVisible()) await consent.click();
  assert.equal(await page.locator('#cn1-search-guide-results h3 a').first().textContent(), 'Build hints reference');
  assert.equal(await page.locator('#cn1-search-api').isVisible(), false);
  assert.equal(await page.locator('#cn1-search-pages').isVisible(), false);
  const guideUrls = await page.locator('#cn1-search-guide-results h3 a').evaluateAll(nodes => nodes.map(node => node.getAttribute('href')));
  assert(guideUrls.every(url => url.startsWith('/developer-guide/')));

  await page.locator('[data-search-scope=blog]').click();
  await ready();
  assert.equal(new URL(page.url()).searchParams.get('q'), 'build hints');
  assert.equal(await page.locator('#cn1-search-guide').isVisible(), false);
  assert.equal(await page.locator('#cn1-search-api').isVisible(), false);
  const blogUrls = await page.locator('#cn1-search-results h3 a').evaluateAll(nodes => nodes.map(node => node.getAttribute('href')));
  assert(blogUrls.length && blogUrls.every(url => url.startsWith('/blog/')));

  await page.locator('[data-search-scope=javadoc]').click();
  await query('BorderLayout');
  assert.match(await page.locator('.cn1-search-api__name').first().textContent(), /^BorderLayout$/);
  assert.equal(await page.locator('#cn1-search-guide').isVisible(), false);
  assert.equal(await page.locator('#cn1-search-pages').isVisible(), false);
  await query('addActionListener');
  assert.match(await page.locator('.cn1-search-api__name').first().textContent(), /^addActionListener/);
  await query('HttpServer');
  assert.match(await page.locator('.cn1-search-api__hit').first().getAttribute('href'), /^\/backend\/javadoc\//);

  await page.locator('[data-search-scope=all]').click();
  await query('BorderLayout');
  assert(await page.locator('#cn1-search-guide').isVisible());
  assert(await page.locator('#cn1-search-api').isVisible());
  const order = await page.locator('#cn1-search-guide, #cn1-search-api, #cn1-search-pages').evaluateAll(nodes => nodes.filter(node => !node.hidden).map(node => node.id));
  assert.deepEqual(order, ['cn1-search-guide', 'cn1-search-api', 'cn1-search-pages']);
  await page.reload();
  await ready();
  assert.equal(await page.locator('#cn1-search-input').inputValue(), 'BorderLayout');
  assert.equal(await page.locator('[aria-current=page][data-search-scope]').getAttribute('data-search-scope'), 'all');

  for (const dark of [false, true]) {
    await page.evaluate(dark => document.body.classList.toggle('dark', dark), dark);
    for (const width of [1280, 390]) {
      await page.setViewportSize({ width, height: 900 });
      assert(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth), 'Horizontal overflow in search');
      await page.screenshot({ path: `/tmp/cn1-search-${dark ? 'dark' : 'light'}-${width}.png` });
    }
  }
  await query('nonsensezzzzzzzz');
  assert.equal(await page.locator('#cn1-search-status').textContent(), 'No results found.');
  await query('title:(');
  assert.equal(await page.locator('#cn1-search-status').textContent(), 'No results found.');
  await query('');
  assert.equal(await page.locator('#cn1-search-status').textContent(), 'Type to search.');

  // Guide and API search fail independently, with no false zero-results report.
  await page.route('**/lunr-index*.json', route => route.abort());
  await page.goto(base + '/search/?scope=javadoc&q=BorderLayout');
  await ready();
  assert(await page.locator('#cn1-search-api').isVisible());
  assert.doesNotMatch(await page.locator('#cn1-search-status').textContent(), /unavailable|No results/);
  await page.locator('[data-search-scope=guide]').click();
  await ready();
  assert.match(await page.locator('#cn1-search-status').textContent(), /unavailable/);
  await page.unroute('**/lunr-index*.json');

  await page.goto(base + '/developer-guide/build-hints/');
  const source = fs.readFileSync(new URL('../../docs/developer-guide/_generated-build-hints.adoc', import.meta.url), 'utf8');
  const names = source.split('\n\n').filter(block => block.startsWith('|') && !block.startsWith('|==='))
    .map(block => block.split('\n')[0].slice(1).replace(/^`\+|\+`$/g, ''));
  assert(names.length > 600);
  const renderedNames = await page.locator('.cn1-guide-content table tbody tr td:first-child').allTextContents();
  assert.deepEqual(renderedNames.map(name => name.trim()), names, 'Catalog rows lost from consolidated table');
  for (const width of [1280, 390]) {
    await page.setViewportSize({ width, height: 900 });
    await page.locator('#_all_build_hints').scrollIntoViewIfNeeded();
    assert(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth), 'Horizontal overflow in build hints');
    await page.screenshot({ path: `/tmp/cn1-build-hints-${width}.png` });
  }
  await page.goto(base + '/developer-guide/advanced-topics-under-the-hood/#_sending_arguments_to_the_build_server');
  await page.waitForURL('**/developer-guide/build-hints/#_sending_arguments_to_the_build_server');
  // A controlled corpus makes typo recovery and required-word matching observable:
  // two near matches each omit one word, and another match belongs to the blog.
  await page.route('**/lunr-index.json', route => route.fulfill({ json: { docs: [
    { id: 'push', title: 'Push notifications', content: 'Deliver a notification with push.', url: '/developer-guide/push/', section: 'guide' },
    { id: 'local', title: 'Local notifications', content: 'Display a notification locally.', url: '/developer-guide/local/', section: 'guide' },
    { id: 'messages', title: 'Push messages', content: 'Send a push message.', url: '/developer-guide/messages/', section: 'guide' },
    { id: 'blog', title: 'Push notifications', content: 'Deliver a notification with push.', url: '/blog/push/', section: 'blog' },
  ] } }));
  await page.goto(base + '/search/?scope=guide&q=push%20notificaton');
  await ready();
  const guideMatches = () => page.locator('#cn1-search-guide-results h3 a').evaluateAll(nodes => nodes.map(node => node.getAttribute('href')));
  assert.deepEqual(await guideMatches(), ['/developer-guide/push/'], 'Missing-character typo must retain every query word');
  for (const typo of ['notificationx', 'notifikation']) {
    await query('push ' + typo);
    assert.deepEqual(await guideMatches(), ['/developer-guide/push/'], 'One-character insertions and substitutions must match');
  }
  await query('notificaton');
  assert.deepEqual((await guideMatches()).sort(), ['/developer-guide/local/', '/developer-guide/push/']);
  await query('push notificaton unrelatedzzzz');
  assert.equal(await page.locator('#cn1-search-status').textContent(), 'No results found.');
  await query('notificatonxx');
  assert.equal(await page.locator('#cn1-search-status').textContent(), 'No results found.');
  await query('push notificaton');
  await page.locator('[data-search-scope=blog]').click();
  await ready();
  assert.equal(await page.locator('#cn1-search-guide').isVisible(), false);
  assert.equal(await page.locator('#cn1-search-results h3 a').first().getAttribute('href'), '/blog/push/');
  await page.unroute('**/lunr-index.json');
  assert.deepEqual(errors, []);
  console.log(`Search scopes, ordering, deep links, API names, failure isolation, themes, mobile layout, ${names.length} catalog rows, and legacy bookmark passed.`);
} finally {
  await browser.close();
}
