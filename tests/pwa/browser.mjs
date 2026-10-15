// Run after JavaScriptPwaTest with -Dcn1.pwa.fixture=/absolute/fixture/path.
// Uses the repository's scripts/package.json Playwright dependency.
import assert from 'node:assert/strict';
import fs from 'node:fs/promises';
import path from 'node:path';
import http from 'node:http';
import {createRequire} from 'node:module';
const require = createRequire(new URL('../../scripts/package.json', import.meta.url));
const {chromium} = require(process.env.CN1_PLAYWRIGHT_MODULE || 'playwright');
const fixture = path.resolve(process.argv[2]);
let version = 1;
const server = http.createServer(async (req, res) => {
    try {
        const url = new URL(req.url, 'http://localhost');
        const prefix = url.pathname.startsWith('/second/') ? '/second/' : '/cn1test/';
        if (!url.pathname.startsWith(prefix)) { res.writeHead(404).end(); return; }
        const relative = decodeURIComponent(url.pathname.slice(prefix.length)) || 'index.html';
        if (relative.split('/').includes('..')) { res.writeHead(400).end(); return; }
        let body = await fs.readFile(path.join(fixture, relative));
        if (version === 2 && prefix === '/cn1test/') {
            if (relative === 'sw.js') body = Buffer.from(body.toString().replace(/"version":\s*"[a-f0-9]+"/, '"version":"second-build"'));
            if (relative === 'assets/theme.res') body = Buffer.from('theme-v2');
        }
        const ext = path.extname(relative);
        const type = {'.js':'application/javascript', '.html':'text/html', '.json':'application/manifest+json', '.png':'image/png'}[ext] || 'application/octet-stream';
        res.writeHead(200, {'Content-Type':type, 'Cache-Control':'no-store'}).end(body);
    } catch { res.writeHead(404).end(); }
});
await new Promise(resolve => server.listen(0, '127.0.0.1', resolve));
const origin = `http://127.0.0.1:${server.address().port}`;
const browser = await chromium.launch({headless:true});
const context = await browser.newContext();
const waitForControl = async page => {
    await page.waitForFunction(() => !!navigator.serviceWorker.controller, null, {timeout:15000});
};
try {
    const page = await context.newPage();
    await page.goto(origin + '/cn1test/');
    await waitForControl(page);
    const cdp = await context.newCDPSession(page);
    const manifest = await cdp.send('Page.getAppManifest');
    assert.equal(manifest.errors.length, 0, JSON.stringify(manifest.errors));
    const install = await cdp.send('Page.getInstallabilityErrors');
    assert.deepEqual(install.installabilityErrors, []);
    const second = await context.newPage();
    await second.goto(origin + '/second/');
    await waitForControl(second);
    const keys = await page.evaluate(() => caches.keys());
    assert.equal(keys.filter(key => key.startsWith('cn1-pwa:')).length, 2, 'separate caches per deployment');
    await context.setOffline(true);
    await page.reload();
    await page.goto(origin + '/cn1test/index.html?installed=1');
    for (const resource of ['worker.js', 'translated_app.js', 'translated_app_1.js', 'assets/theme.res', 'assets/font%20with%20spaces.ttf']) {
        assert.equal(await page.evaluate(async name => (await fetch(name)).status, resource), 200, resource);
    }
    assert.equal(await page.evaluate(async () => {
        try { await fetch('api/private-data'); return 'unexpected cache'; } catch { return 'offline'; }
    }), 'offline');
    await context.setOffline(false);
    version = 2;
    await page.evaluate(async () => { const registration = await navigator.serviceWorker.ready; await registration.update(); });
    await page.waitForFunction(async () => !!(await navigator.serviceWorker.ready).waiting);
    assert.equal(await page.evaluate(async () => (await fetch('assets/theme.res')).text()), 'theme-v1', 'old tab stays on old bundle');
    await page.close();
    const updated = await context.newPage();
    await updated.goto(origin + '/cn1test/');
    await waitForControl(updated);
    await updated.waitForFunction(async () => (await (await fetch('assets/theme.res')).text()) === 'theme-v2');
    const currentKeys = await updated.evaluate(() => caches.keys());
    assert.equal(currentKeys.filter(key => key.startsWith('cn1-pwa:')).length, 2, 'only own obsolete cache deleted');
    assert.equal(await second.evaluate(async () => (await fetch('assets/theme.res')).text()), 'theme-v1');
    await context.setOffline(true);
    await updated.reload();
    assert.equal(await updated.evaluate(async () => (await fetch('assets/theme.res')).text()), 'theme-v2');
    console.log('PASS: manifest/installability, subpath offline reload, chunks/resources, API exclusion, waiting update, cache isolation');
} finally {
    await browser.close();
    await new Promise(resolve => server.close(resolve));
}
