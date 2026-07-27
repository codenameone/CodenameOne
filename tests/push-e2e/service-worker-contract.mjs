import assert from 'node:assert/strict';
import fs from 'node:fs';
import vm from 'node:vm';
import path from 'node:path';
import {fileURLToPath} from 'node:url';

const repository = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../..');
const source = fs.readFileSync(path.join(repository,
        'Ports/JavaScriptPort/src/main/webapp/sw.js'), 'utf8');
const registrationSource = fs.readFileSync(path.join(repository,
        'Ports/JavaScriptPort/src/main/webapp/js/push.js'), 'utf8');

async function registrationScenario(subscription, typedEnvelope) {
    let registeredId;
    let registrationError;
    const workerMessages = [];
    const activeWorker = {
        state: 'activated',
        postMessage(value) { workerMessages.push(value); },
        addEventListener() {}
    };
    const serviceWorkerRegistration = {
        active: activeWorker,
        update() {},
        pushManager: {
            getSubscription: async () => subscription
        }
    };
    const sandbox = {
        console,
        Promise,
        Uint8Array,
        String,
        encodeURIComponent,
        Notification: {permission: 'granted'},
        ServiceWorkerRegistration: function () {},
        PushManager: function () {},
        navigator: {
            serviceWorker: {
                register: async () => serviceWorkerRegistration,
                ready: Promise.resolve(serviceWorkerRegistration),
                addEventListener() {}
            }
        },
        $: callback => callback()
    };
    sandbox.ServiceWorkerRegistration.prototype.showNotification = function () {};
    sandbox.window = sandbox;
    vm.createContext(sandbox);
    vm.runInContext(registrationSource, sandbox, {filename: 'push.js'});
    sandbox.cn1_registerPush(
            id => { registeredId = id; },
            error => { registrationError = error; },
            () => {},
            [],
            typedEnvelope);
    await new Promise(resolve => setImmediate(resolve));
    await new Promise(resolve => setImmediate(resolve));
    return {registeredId, registrationError, workerMessages};
}

async function pushScenario(envelope, windows) {
    const handlers = {};
    const posted = [];
    const notifications = [];
    const openedUrls = [];
    const openedClients = [];
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
            openWindow: async url => {
                openedUrls.push(url);
                const openedClient = {
                    url,
                    focused: true,
                    postMessage: value => posted.push(value)
                };
                openedClients.push(openedClient);
                return openedClient;
            }
        },
        self: {
            location: {href: 'https://fixture/sw.js'},
            registration: {showNotification: async (title, options) => notifications.push({title, options})},
            addEventListener: (type, callback) => {
                if (!handlers[type]) {
                    handlers[type] = [];
                }
                handlers[type].push(callback);
            }
        }
    };
    vm.createContext(sandbox);
    vm.runInContext(source, sandbox, {filename: 'sw.js'});
    assert.equal(typeof handlers.push?.[0], 'function',
            'service worker must register a push event handler');
    let completion;
    handlers.push[0]({data: {json: () => envelope}, waitUntil: value => { completion = value; }});
    assert.equal(typeof completion?.then, 'function',
            'push handler must pass asynchronous work to event.waitUntil()');
    await completion;
    return {
        posted,
        notifications,
        openedUrls,
        async click(notification, action) {
            assert.equal(typeof handlers.notificationclick?.[0], 'function');
            let clickCompletion;
            handlers.notificationclick[0]({
                notification: {
                    data: notification.options.data,
                    close() {}
                },
                action,
                waitUntil: value => { clickCompletion = value; }
            });
            assert.equal(typeof clickCompletion?.then, 'function');
            await clickCompletion;
        },
        signal(value) {
            assert.ok(handlers.message?.length > 0);
            assert.ok(openedClients.length > 0,
                    'a notification click must open a client before it signals readiness');
            for (const handler of handlers.message) {
                handler({
                    data: value,
                    source: openedClients[openedClients.length - 1]
                });
            }
        },
        signalReady() {
            this.signal({command: 'pushClientReady'});
        }
    };
}

const visible = {schema: 3, id: 'visible', title: 'Title', body: 'Body'};
let result = await pushScenario(visible, [{focused: true}]);
assert.equal(result.posted.length, 1, 'focused delivery must reach the app exactly once');
assert.equal(result.notifications.length, 0, 'focused delivery must not create a system notification');

result = await pushScenario(visible, [{focused: false}]);
assert.equal(result.posted.length, 0,
        'background visual delivery must wait for the notification click');
assert.equal(result.notifications.length, 1, 'background visual delivery must create a notification');
assert.equal(result.notifications[0].options.data.id, 'visible');
await result.click(result.notifications[0]);
assert.equal(result.posted.length, 1,
        'notification click must deliver a background visual message exactly once');

result = await pushScenario({schema: 3, id: 'silent', silent: true, data: {revision: 7}}, []);
assert.equal(result.posted.length, 0);
assert.equal(result.notifications.length, 0, 'silent delivery must never create a notification');

result = await pushScenario(
        {schema: 3, id: 'silent-deep-link', silent: true, data: {revision: 8}},
        [{url: 'https://fixture/orders/42', focused: false}]);
assert.equal(result.posted.length, 1,
        'silent delivery must reach an open same-origin deep-link client');
assert.equal(result.posted[0].data.id, 'silent-deep-link');
assert.equal(result.notifications.length, 0, 'silent delivery must never create a notification');

result = await pushScenario({schema: 3, id: 'data-only', data: {revision: 8}}, []);
assert.equal(result.posted.length, 0);
assert.equal(result.notifications.length, 0,
        'data-only delivery must not create a blank notification');

result = await pushScenario({...visible, deepLink: 'https://attacker.example/phish'}, []);
await result.click(result.notifications[0]);
assert.deepEqual(result.openedUrls, ['https://fixture/index.html'],
        'notification clicks must not navigate outside the application origin');

result = await pushScenario({...visible, id: 'deep-link', deepLink: '/orders/42'}, []);
await result.click(result.notifications[0]);
assert.deepEqual(result.openedUrls, ['https://fixture/orders/42']);
assert.equal(result.posted.length, 0,
        'a newly opened deep-link client must receive the push only after it is ready');
result.signal({command: 'unrelatedPageMessage'});
assert.equal(result.posted.length, 0,
        'an unrelated page message must not flush pending pushes');
result.signalReady();
assert.equal(result.posted.length, 1,
        'pending pushes must be delivered to the deep-link client that signals readiness');
assert.equal(result.posted[0].data.id, 'deep-link');

result = await registrationScenario({
    endpoint: 'https://push.example/subscription',
    getKey: () => null,
    toJSON: 'not-a-function'
}, true);
assert.equal(result.registrationError, undefined);
assert.match(result.registeredId, /^cn1-web-/,
        'typed registration must fall back safely when toJSON is not callable');
assert.equal(result.workerMessages[0]?.command, 'pushClientReady',
        'registration must send an explicit readiness command before configuration');

console.log('push service-worker contract: PASS');
