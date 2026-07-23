import assert from 'node:assert/strict';
import fs from 'node:fs';
import vm from 'node:vm';
import path from 'node:path';
import {fileURLToPath} from 'node:url';

const repository = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../..');
const source = fs.readFileSync(path.join(repository,
        'Ports/JavaScriptPort/src/main/webapp/sw.js'), 'utf8');

async function pushScenario(envelope, windows) {
    const handlers = {};
    const posted = [];
    const notifications = [];
    const clients = windows.map(window => ({
        url: window.url ?? 'https://fixture/index.html',
        focused: !!window.focused,
        postMessage: value => posted.push(value),
        focus: async function () { this.focused = true; return this; }
    }));
    const sandbox = {
        console,
        Promise,
        Number,
        URL,
        fetch: async () => { throw new Error('fetch not expected'); },
        caches: {match: async () => null, open: async () => ({}), keys: async () => []},
        clients: {
            matchAll: async () => clients,
            openWindow: async url => ({url, focused: true, postMessage: value => posted.push(value)})
        },
        self: {
            location: {href: 'https://fixture/sw.js'},
            registration: {showNotification: async (title, options) => notifications.push({title, options})},
            addEventListener: (type, callback) => { handlers[type] = callback; }
        }
    };
    vm.createContext(sandbox);
    vm.runInContext(source, sandbox, {filename: 'sw.js'});
    let completion;
    handlers.push({data: {json: () => envelope}, waitUntil: value => { completion = value; }});
    await completion;
    return {posted, notifications};
}

const visible = {schema: 3, id: 'visible', title: 'Title', body: 'Body'};
let result = await pushScenario(visible, [{focused: true}]);
assert.equal(result.posted.length, 1, 'focused delivery must reach the app exactly once');
assert.equal(result.notifications.length, 0, 'focused delivery must not create a system notification');

result = await pushScenario(visible, [{focused: false}]);
assert.equal(result.posted.length, 1, 'background delivery must be persisted/delivered once');
assert.equal(result.notifications.length, 1, 'background visual delivery must create a notification');
assert.equal(result.notifications[0].options.data.id, 'visible');

result = await pushScenario({schema: 3, id: 'silent', silent: true, data: {revision: 7}}, []);
assert.equal(result.posted.length, 0);
assert.equal(result.notifications.length, 0, 'silent delivery must never create a notification');

console.log('push service-worker contract: PASS');
