/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */

/**
 * Runs the __cn1_vault__ host bridge in a real Chromium, against the real
 * IndexedDB, the real Web Crypto, and a virtual authenticator.
 *
 * The Node test in vm/tests covers the same bridge against a stub IndexedDB,
 * which is enough to pin the logic and is not enough to pin the browser: a
 * structured clone that will not carry a CryptoKey, a transaction that
 * auto-closes before an await resumes, and an authenticator that registers a
 * passkey without supporting PRF are all things only a browser can tell us.
 *
 * The passkey half cannot be tested any other way. WebAuthn needs an
 * authenticator and a user, and Chrome's virtual authenticator (over the
 * DevTools protocol) supplies both -- with hmac-secret enabled, which is what
 * the PRF extension is built on. Without this the PRF path would ship never
 * having run.
 *
 * Usage:  node scripts/test-javascript-vault-bridge.mjs
 * Requires: playwright (scripts/package.json), and a Chromium it can launch.
 */

import fs from 'node:fs';
import http from 'node:http';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const here = path.dirname(fileURLToPath(import.meta.url));
const repoRoot = path.resolve(here, '..');

let chromium;
try {
  ({ chromium } = await import('playwright'));
} catch (e) {
  console.error('Playwright is required. Run `npm install` in scripts/.');
  console.error(String(e));
  process.exit(2);
}

const BEGIN = '// CN1_VAULT_BRIDGE_BEGIN';
const END = '// CN1_VAULT_BRIDGE_END';

const bridgePath = path.join(repoRoot, 'vm', 'ByteCodeTranslator', 'src', 'javascript', 'browser_bridge.js');
const source = fs.readFileSync(bridgePath, 'utf8');
const begin = source.indexOf(BEGIN);
const end = source.indexOf(END);
if (begin < 0 || end <= begin) {
  console.error('The vault bridge markers are missing from browser_bridge.js.');
  process.exit(1);
}
const slice = source.slice(begin, end);
if (!slice.includes("hostBridge.register('__cn1_vault__'")) {
  console.error('The sliced region does not contain the vault bridge.');
  process.exit(1);
}

// The page supplies only what the bridge reads from its enclosing scope. Everything
// else -- IndexedDB, Web Crypto, WebAuthn -- is the browser's own.
const page = `<!doctype html>
<html><head><meta charset="utf-8"><title>cn1 vault bridge</title></head>
<body><script>
(function() {
  var global = window;
  var vaultHandler = null;
  var hostBridge = { register: function(name, fn) { if (name === '__cn1_vault__') { vaultHandler = fn; } } };
  function cn1CryptoApi() { return window.crypto; }
${slice}
  window.__cn1VaultCall = function(request) { return Promise.resolve(vaultHandler(request)); };
})();
</script></body></html>`;

const server = http.createServer((req, res) => {
  res.writeHead(200, { 'Content-Type': 'text/html; charset=utf-8' });
  res.end(page);
});
await new Promise((resolve) => server.listen(0, '127.0.0.1', resolve));
const { port } = server.address();
// ``localhost``, not ``127.0.0.1``, and the difference is not cosmetic. Both are
// secure contexts, so Web Crypto works either way -- but WebAuthn's relying-party
// id must be a domain, an IP literal is not one, and Chrome refuses every
// ceremony on 127.0.0.1 with "This is an invalid domain". The passkey half of
// this file silently tests nothing if the host is an address.
const url = `http://localhost:${port}/`;

const results = [];
let failures = 0;
function check(name, actual, expected) {
  const ok = JSON.stringify(actual) === JSON.stringify(expected);
  if (!ok) {
    failures++;
  }
  results.push(`${ok ? 'ok  ' : 'FAIL'}  ${name}${ok ? '' : `  (expected ${JSON.stringify(expected)}, got ${JSON.stringify(actual)})`}`);
}

const browser = await chromium.launch();
try {
  const context = await browser.newContext();
  const tab = await context.newPage();
  tab.on('console', (m) => { if (m.type() === 'error') { console.error('[page]', m.text()); } });
  tab.on('pageerror', (e) => { console.error('[pageerror]', String(e)); });
  await tab.goto(url);

  const call = (request) => tab.evaluate((r) => window.__cn1VaultCall(r), request);
  const status = (reply) => reply[0];
  const payload = (reply) => reply.slice(1);

  // ---------------------------------------------------------------- device key
  const KEY = 'browser-vault';
  const AAD = [1, 2, 3, 4];
  const SECRET = [9, 8, 7, 6, 5];

  const caps = await call({ op: 'capabilities' });
  check('capabilities succeed', status(caps), 0);
  // Secure context (1) + subtle (2) + IndexedDB opened (4). Persistence is not
  // granted to a fresh headless profile, so bit 8 stays clear.
  check('capability bits report a usable secure origin', payload(caps)[0] & 7, 7);

  check('an absent key reads as absent', await call({ op: 'keyState', keyId: KEY }), [0, 0]);
  check('unwrap with no key is KEY_MISSING',
    status(await call({ op: 'unwrap', keyId: KEY, data: new Array(40).fill(7), aad: AAD })), 1);

  const [a, b] = await Promise.all([
    call({ op: 'ensureKey', keyId: KEY }),
    call({ op: 'ensureKey', keyId: KEY })
  ]);
  check('racing ensureKey both succeed', [status(a), status(b)], [0, 0]);
  check('a key now exists', payload(await call({ op: 'keyState', keyId: KEY }))[0], 1);

  const wrapped = await call({ op: 'wrap', keyId: KEY, data: SECRET, aad: AAD });
  check('wrap succeeds', status(wrapped), 0);
  const sealed = payload(wrapped);
  check('a 12 byte nonce and a 16 byte tag', sealed.length - SECRET.length, 28);

  const opened = await call({ op: 'unwrap', keyId: KEY, data: sealed, aad: AAD });
  check('unwrap returns the plaintext', [status(opened), ...payload(opened)], [0, ...SECRET]);

  const again = payload(await call({ op: 'wrap', keyId: KEY, data: SECRET, aad: AAD }));
  check('two wraps of the same bytes differ', JSON.stringify(again) !== JSON.stringify(sealed), true);

  check('a different binding fails the tag',
    status(await call({ op: 'unwrap', keyId: KEY, data: sealed, aad: [9, 9, 9, 9] })), 2);
  const tampered = sealed.slice();
  tampered[tampered.length - 1] ^= 1;
  check('a flipped ciphertext bit fails the tag',
    status(await call({ op: 'unwrap', keyId: KEY, data: tampered, aad: AAD })), 2);

  // The claim the documentation makes about the stored key, asserted against the
  // object Chrome actually stored rather than against the argument we passed.
  const exportable = await tab.evaluate(async () => {
    const db = await new Promise((resolve, reject) => {
      const r = indexedDB.open('cn1-vault', 1);
      r.onsuccess = () => resolve(r.result);
      r.onerror = () => reject(r.error);
    });
    const record = await new Promise((resolve, reject) => {
      const r = db.transaction('keys').objectStore('keys').get('browser-vault');
      r.onsuccess = () => resolve(r.result);
      r.onerror = () => reject(r.error);
    });
    let exported = 'rejected';
    try {
      await crypto.subtle.exportKey('raw', record.key);
      exported = 'SUCCEEDED';
    } catch (e) { /* expected */ }
    return { extractable: record.key.extractable, exported };
  });
  check('the stored CryptoKey is not extractable', exportable.extractable, false);
  check('exportKey on it rejects', exportable.exported, 'rejected');

  // The key survives a reload -- which is the whole point of storing it.
  await tab.reload();
  const afterReload = await call({ op: 'unwrap', keyId: KEY, data: sealed, aad: AAD });
  check('the wrap still opens after a page reload',
    [status(afterReload), ...payload(afterReload)], [0, ...SECRET]);

  // ---------------------------------------------------------------- passkey PRF
  const cdp = await context.newCDPSession(tab);
  await cdp.send('WebAuthn.enable');
  const { authenticatorId } = await cdp.send('WebAuthn.addVirtualAuthenticator', {
    options: {
      protocol: 'ctap2',
      ctap2Version: 'ctap2_1',
      transport: 'internal',
      hasResidentKey: true,
      hasUserVerification: true,
      hasPrf: true,
      isUserVerified: true,
      automaticPresenceSimulation: true
    }
  });

  // Asked before anything is enrolled, and it must not prompt: an application deciding
  // whether to offer the option would otherwise have to ask for the thing it is offering.
  // [status, enrolled, syncable]. The third byte is only meaningful when the
  // second is 1, and is 0 here because there is nothing to describe.
  check('no passkey is enrolled yet', await call({ op: 'prfState', keyId: KEY }), [0, 0, 0]);

  const enrolled = await call({ op: 'prfEnroll', keyId: KEY, userName: 'tester' });
  check('passkey enrolment succeeds', status(enrolled), 0);
  check('and the state now reports one', await call({ op: 'prfState', keyId: KEY }), [0, 1, 0]);

  const first = await call({ op: 'prfDerive', keyId: KEY });
  check('the PRF derivation succeeds', status(first), 0);
  check('it produces 32 bytes', payload(first).length, 32);

  const second = await call({ op: 'prfDerive', keyId: KEY });
  check('the same credential and salt derive the same bytes',
    JSON.stringify(payload(second)), JSON.stringify(payload(first)));
  check('the derived bytes are not all zero',
    payload(first).some((v) => v !== 0), true);

  // A second vault gets its own credential and its own salt, so its key must differ.
  const OTHER = 'browser-vault-2';
  check('a second vault enrols', status(await call({ op: 'prfEnroll', keyId: OTHER, userName: 'tester' })), 0);
  const otherKey = await call({ op: 'prfDerive', keyId: OTHER });
  check('a different vault derives different bytes',
    JSON.stringify(payload(otherKey)) !== JSON.stringify(payload(first)), true);

  check('deriving for a vault that never enrolled is KEY_MISSING',
    status(await call({ op: 'prfDerive', keyId: 'never-enrolled' })), 1);

  // A user who declines is cancelled, not failed: the difference is whether the
  // application shows an error for a button nobody pressed.
  await cdp.send('WebAuthn.setUserVerified', { authenticatorId, isUserVerified: false });
  check('a refused verification reports CANCELLED',
    status(await call({ op: 'prfDerive', keyId: KEY })), 9);
  await cdp.send('WebAuthn.setUserVerified', { authenticatorId, isUserVerified: true });

  // ------------------------------------------------- device-bound passkeys
  //
  // The virtual authenticator's backup flags are what make this testable: a real
  // one is whatever it is, and the refusal path would otherwise never run.
  check('this authenticator reports a non-syncable credential',
    (await call({ op: 'prfState', keyId: KEY }))[2], 0);

  await cdp.send('WebAuthn.removeVirtualAuthenticator', { authenticatorId });
  const syncing = await cdp.send('WebAuthn.addVirtualAuthenticator', {
    options: {
      protocol: 'ctap2', ctap2Version: 'ctap2_1', transport: 'internal',
      hasResidentKey: true, hasUserVerification: true, hasPrf: true,
      isUserVerified: true, automaticPresenceSimulation: true,
      defaultBackupEligibility: true, defaultBackupState: true
    }
  });

  const SYNCED = 'browser-vault-synced';
  // Without the requirement, a syncable credential is accepted -- that is the
  // ordinary case and most passkeys are like this.
  check('a syncable passkey enrols when device binding is not required',
    status(await call({ op: 'prfEnroll', keyId: SYNCED, userName: 'tester' })), 0);
  check('and it reports itself as able to leave this device',
    (await call({ op: 'prfState', keyId: SYNCED }))[2], 1);

  // The policy is re-checked against a credential that ALREADY exists, which is the
  // case an application reaches by adding requireDeviceBoundPasskey() to a vault that
  // was enrolled without it. Reporting OK on the record's mere existence re-wrapped the
  // vault under the syncable credential it was supposed to refuse.
  check('an existing syncable passkey is refused once device binding is required',
    status(await call({ op: 'prfEnroll', keyId: SYNCED, userName: 'tester', deviceBound: true })), 10);
  check('and the existing credential is left alone rather than dropped',
    (await call({ op: 'prfState', keyId: SYNCED }))[1], 1);

  const STRICT = 'browser-vault-strict';
  // With it, the same authenticator is refused rather than quietly accepted.
  check('a syncable passkey is refused when device binding IS required',
    status(await call({ op: 'prfEnroll', keyId: STRICT, userName: 'tester', deviceBound: true })), 10);
  check('and nothing was stored for it',
    (await call({ op: 'prfState', keyId: STRICT }))[1], 0);

  await cdp.send('WebAuthn.removeVirtualAuthenticator', { authenticatorId: syncing.authenticatorId });

  // ------------------------------------------------------- the secure-store gate
  // An ordinary account's record has to settle in ONE place. It used to settle in two: the
  // atomic create wrote this store and a plain set() wrote ordinary Storage, so a create
  // paused past its "nothing here" check could mirror its own candidate over a value set()
  // had already stored. Both writers go through this store now, and the create answers with
  // what the store holds rather than with what it tried to write.
  const ENTRY = 'account.token';
  const asText = (reply) => String.fromCharCode.apply(null, payload(reply));

  const gateCreate = await call({ op: 'secureStoreSetIfAbsent', entry: ENTRY, sealed: 'first' });
  check('an absent account is created', status(gateCreate), 0);
  check('and the create answers its own record', asText(gateCreate), 'first');

  const gateReCreate = await call({ op: 'secureStoreSetIfAbsent', entry: ENTRY, sealed: 'second' });
  check('a second create is refused and answers the settled record', asText(gateReCreate), 'first');

  check('an ordinary set replaces it',
    status(await call({ op: 'secureStoreSet', entry: ENTRY, sealed: 'third' })), 0);
  // The check the re-read exists for: the create must report what the store holds now, not
  // the record that originally won the gate.
  const gateAfterSet = await call({ op: 'secureStoreSetIfAbsent', entry: ENTRY, sealed: 'fourth' });
  check('and a later create adopts the value the set stored', asText(gateAfterSet), 'third');

  // The read-only probe the mirror uses to tell whether what it is copying is still what the
  // store holds. It must NOT create: a record removed between the create and the mirror would
  // otherwise be resurrected by the very check meant to keep the two namespaces in step.
  const readBack = await call({ op: 'secureStoreRead', entry: ENTRY });
  check('the gate can be read without creating', status(readBack), 0);
  check('and it answers what the store holds', asText(readBack), 'third');
  const absentRead = await call({ op: 'secureStoreRead', entry: 'account.nothing' });
  check('an absent record reads as empty', asText(absentRead), '');
  check('and reading it did not create one',
    asText(await call({ op: 'secureStoreSetIfAbsent', entry: 'account.nothing', sealed: 'new' })),
    'new');
  check('releasing that probe entry',
    status(await call({ op: 'secureStoreForget', entry: 'account.nothing' })), 0);

  check('forgetting the entry succeeds',
    status(await call({ op: 'secureStoreForget', entry: ENTRY })), 0);
  const gateReopened = await call({ op: 'secureStoreSetIfAbsent', entry: ENTRY, sealed: 'fifth' });
  check('and the gate is free again', asText(gateReopened), 'fifth');
  check('releasing a gate that was never taken is not an error',
    status(await call({ op: 'secureStoreForget', entry: 'account.never' })), 0);

  check('forgetting the passkey succeeds', status(await call({ op: 'prfForget', keyId: KEY })), 0);
  check('and the state reports none', await call({ op: 'prfState', keyId: KEY }), [0, 0, 0]);
  check('and the vault then has no passkey', status(await call({ op: 'prfDerive', keyId: KEY })), 1);
} finally {
  await browser.close();
  server.close();
}

console.log(results.join('\n'));
console.log(`\n${results.length - failures}/${results.length} checks passed`);
process.exit(failures === 0 ? 0 : 1);
