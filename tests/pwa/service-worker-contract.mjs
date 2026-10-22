import assert from 'node:assert/strict';
import fs from 'node:fs';
import vm from 'node:vm';
const source = fs.readFileSync(new URL('../../maven/build-engine/src/main/resources/com/codename1/builders/javascript/pwa-worker.js', import.meta.url), 'utf8');
const scope = 'https://example.com/cn1test/';
const prefix = 'cn1-pwa:' + encodeURIComponent(scope) + ':';
const handlers = new Map();
const deleted = [];
let added;
let failInstall = false;
let claimed = false;
const context = {
    URL, Set, Request, Response, Promise, encodeURIComponent,
    self: {
        registration: {scope},
        cn1Pwa: {version:'new', startUrl:'./dashboard?installed=1#section', assets:['index.html', 'worker.js', 'translated_app_1.js', 'assets/font%20name.ttf']},
        clients: {claim:async () => { claimed = true; }},
        addEventListener: (name, handler) => handlers.set(name, handler),
        skipWaiting: () => assert.fail('must not replace the worker of an open app')
    },
    caches: {
        open: async name => {
            assert.equal(name, prefix + 'new');
            return {
                addAll: async requests => { added = requests; if (failInstall) throw Error('missing asset'); },
                match: async url => ({cached:url})
            };
        },
        keys: async () => [prefix+'old', prefix+'new', 'cn1-pwa:another-app:old', 'other-cache'],
        delete: async name => { deleted.push(name); }
    },
    fetch: async () => assert.fail('a cached resource should not require the network')
};
vm.runInNewContext(source, context);
async function lifecycle(name) {
    let pending;
    handlers.get(name)({waitUntil:promise => { pending = promise; }});
    assert.ok(pending, name + ' must extend the event lifetime');
    await pending;
}
await lifecycle('install');
assert.deepEqual(Array.from(added, request => request.url), ['index.html','worker.js','translated_app_1.js','assets/font%20name.ttf'].map(p => scope+p));
assert.ok(added.every(request => request.cache === 'reload'));
failInstall = true;
await assert.rejects(lifecycle('install'), /missing asset/, 'a partial cache must not install');
await lifecycle('activate');
assert.deepEqual(deleted, [prefix+'old']);
assert.ok(claimed);
async function cached(url, mode = 'cors', method = 'GET') {
    let response;
    handlers.get('fetch')({request:{url, mode, method}, respondWith:promise => { response = promise; }});
    return response ? await response : null;
}
assert.equal((await cached(scope+'?installed=1', 'navigate')).cached, scope+'index.html');
assert.equal((await cached(scope+'index.html?installed=1', 'navigate')).cached, scope+'index.html');
assert.equal((await cached(scope+'translated_app_1.js')).cached, scope+'translated_app_1.js');
assert.equal((await cached(scope+'dashboard?installed=1', 'navigate')).cached, scope+'index.html');
assert.equal(await cached(scope+'dashboard'), null, 'only navigations get the configured shell');
assert.equal(await cached(scope+'unrelated', 'navigate'), null, 'unknown routes still reach the server');
for (const url of [scope+'api/private', scope+'cn1-cors-proxy?_target=x', 'https://example.com/other/', 'https://cdn.example.com/file.js', scope+'worker.js?dynamic=1']) {
    assert.equal(await cached(url), null, url);
}
assert.equal(await cached(scope+'index.html', 'cors', 'POST'), null);
context.self.cn1Pwa.startUrl = 'https://elsewhere.example/dashboard';
vm.runInNewContext(source, context);
assert.equal(await cached('https://elsewhere.example/dashboard', 'navigate'), null);
context.self.cn1Pwa.startUrl = '/outside/dashboard';
vm.runInNewContext(source, context);
assert.equal(await cached('https://example.com/outside/dashboard', 'navigate'), null);
context.self.cn1Pwa.startUrl = './worker.js';
vm.runInNewContext(source, context);
assert.equal((await cached(scope+'worker.js', 'navigate')).cached, scope+'worker.js', 'existing files keep their own content');
// A precached shell that the host served through a redirect must not reach a
// navigation still marked redirected: Chrome rejects it as a network error.
const shellResponse = await (async () => {
    let response;
    const original = context.caches.open;
    context.caches.open = async name => {
        const cache = await original(name);
        return {...cache, match: async url => {
            const r = new Response('shell of ' + url, {status:200, statusText:'OK', headers:{'X-Cached':url}});
            Object.defineProperty(r, 'redirected', {value:true});
            return r;
        }};
    };
    handlers.get('fetch')({request:{url:scope+'?theme=light', mode:'navigate', method:'GET'}, respondWith:promise => { response = promise; }});
    const result = await response;
    context.caches.open = original;
    return result;
})();
assert.equal(shellResponse.redirected, false, 'a redirected cache entry must be copied before answering a navigation');
assert.equal(shellResponse.status, 200);
assert.equal(shellResponse.headers.get('X-Cached'), scope+'index.html');
assert.equal(await shellResponse.text(), 'shell of '+scope+'index.html');
console.log('PASS: atomic precache, waiting updates, scope isolation, navigation queries, API exclusion and redirected shells');
