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
 * Serves a page that exercises the vault's browser bridge against whatever
 * authenticator the person running it actually has.
 *
 * `scripts/test-javascript-vault-bridge.mjs` covers the same code automatically,
 * with Chrome's *virtual* authenticator -- which is told to support the WebAuthn
 * PRF extension and therefore always does. Real authenticators are the open
 * question: PRF is built on CTAP2's `hmac-secret`, plenty of platform
 * authenticators do not implement it, and no headless test can tell us which.
 * The same goes for Firefox and Safari, which have no virtual-authenticator
 * protocol at all.
 *
 * So this is the manual counterpart, and it is deliberately the *shipped* code:
 * the bridge is sliced out of browser_bridge.js between its markers rather than
 * re-implemented, so a pass here is evidence about what applications run.
 *
 * Usage:  node scripts/verify-javascript-vault-passkey.mjs [--port 8899]
 * Then open the printed URL. It must be a hostname -- WebAuthn refuses
 * IP-address origins -- which is why this serves on localhost.
 */

import fs from 'node:fs';
import http from 'node:http';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const here = path.dirname(fileURLToPath(import.meta.url));
const repoRoot = path.resolve(here, '..');
const portArgument = process.argv.indexOf('--port');
const port = portArgument >= 0 ? Number(process.argv[portArgument + 1]) : 8899;

const BEGIN = '// CN1_VAULT_BRIDGE_BEGIN';
const END = '// CN1_VAULT_BRIDGE_END';
const bridgeFile = path.join(repoRoot, 'vm', 'ByteCodeTranslator', 'src', 'javascript', 'browser_bridge.js');
const source = fs.readFileSync(bridgeFile, 'utf8');
const begin = source.indexOf(BEGIN);
const end = source.indexOf(END);
if (begin < 0 || end <= begin) {
  console.error('The vault bridge markers are missing from browser_bridge.js.');
  process.exit(1);
}
const slice = source.slice(begin, end);

const page = `<!doctype html>
<html><head><meta charset="utf-8"><title>Codename One vault -- browser check</title>
<style>
  body { font: 15px/1.5 -apple-system, system-ui, sans-serif; margin: 2rem auto; max-width: 46rem; padding: 0 1rem; }
  button { font: inherit; padding: .6rem 1rem; margin: .25rem .5rem .25rem 0; cursor: pointer; }
  pre { background: #f4f4f6; padding: 1rem; border-radius: 6px; white-space: pre-wrap; word-break: break-word; }
  .ok { color: #0a7a28; } .bad { color: #b00020; } .note { color: #555; font-size: .9em; }
</style></head><body>
<h1>Codename One vault: browser check</h1>
<p class="note">This runs the real bridge from <code>browser_bridge.js</code>. The passkey step
creates one credential for <code>localhost</code>; you can delete it afterwards at
<code>chrome://settings/passkeys</code> (or the equivalent in your browser).</p>

<button id="caps">1. Capabilities (no prompt)</button>
<button id="storage">2. Device key round trip (no prompt)</button>
<button id="passkey">3. Passkey enrol + derive (will prompt)</button>
<button id="again">4. Derive again (will prompt)</button>
<button id="platform">3b. Passkey, Touch ID only (will prompt)</button>
<button id="reset">Forget the passkey</button>

<pre id="out">Ready. Run them in order, then copy everything below back.</pre>

<script>
(function() {
  var global = window;
  var vaultHandler = null;
  var hostBridge = { register: function(name, fn) { if (name === '__cn1_vault__') { vaultHandler = fn; } } };
  function cn1CryptoApi() { return window.crypto; }
${slice}
  window.__cn1VaultCall = function(request) { return Promise.resolve(vaultHandler(request)); };
})();

const out = document.getElementById('out');
const lines = [];
function say(text) { lines.push(text); out.textContent = lines.join('\\n'); }
const KEY = 'manual-check';
const call = (r) => window.__cn1VaultCall(r);
const status = (r) => r[0];
const payload = (r) => r.slice(1);
const NAMES = ['OK','KEY_MISSING','AUTH_FAILED','CRYPTO_UNAVAILABLE','STORAGE_UNAVAILABLE',
  'QUOTA_EXCEEDED','INSECURE_CONTEXT','TEMPORARILY_UNREADABLE','UNKNOWN','CANCELLED'];
const name = (s) => NAMES[s] || ('status ' + s);
let firstDerive = null;

document.getElementById('caps').onclick = async () => {
  say('--- capabilities ---');
  say('userAgent: ' + navigator.userAgent);
  say('secureContext: ' + window.isSecureContext + '   origin: ' + location.origin);
  const r = await call({ op: 'capabilities' });
  const bits = payload(r)[0];
  say('bits=' + bits + '  secureContext=' + !!(bits & 1) + ' subtle=' + !!(bits & 2)
      + ' indexedDB=' + !!(bits & 4) + ' storagePersisted=' + !!(bits & 8)
      + ' webauthnApiPresent=' + !!(bits & 16));
  try {
    const uvpaa = await PublicKeyCredential.isUserVerifyingPlatformAuthenticatorAvailable();
    say('platform authenticator available: ' + uvpaa);
  } catch (e) { say('platform authenticator available: <error> ' + e); }
};

document.getElementById('storage').onclick = async () => {
  say('--- device key (non-extractable CryptoKey in IndexedDB) ---');
  say('ensureKey: ' + name(status(await call({ op: 'ensureKey', keyId: KEY }))));
  const secret = [1,2,3,4,5,6,7,8];
  const w = await call({ op: 'wrap', keyId: KEY, data: secret, aad: [9,9] });
  say('wrap: ' + name(status(w)) + '  bytes=' + payload(w).length);
  const o = await call({ op: 'unwrap', keyId: KEY, data: payload(w), aad: [9,9] });
  say('unwrap: ' + name(status(o)) + '  roundTripped='
      + (JSON.stringify(payload(o)) === JSON.stringify(secret)));
  const bad = await call({ op: 'unwrap', keyId: KEY, data: payload(w), aad: [7,7] });
  say('unwrap with wrong binding: ' + name(status(bad)) + ' (expected AUTH_FAILED)');
};

document.getElementById('passkey').onclick = async () => {
  say('--- passkey (WebAuthn PRF) ---');
  const e = await call({ op: 'prfEnroll', keyId: KEY, userName: 'vault check' });
  say('enrol: ' + name(status(e)));
  if (status(e) !== 0) {
    // The bridge reports a status, not a reason, and the two failures that land
    // here mean opposite things: CANCELLED is a ceremony nobody completed and
    // says nothing about PRF, while UNKNOWN is an authenticator that registered
    // a passkey and will not evaluate one. Guessing between them is how a
    // diagnostic page reports a conclusion its data does not support, so the
    // raw ceremony is repeated here purely to print what the browser said.
    if (status(e) === 9) {
      say('   CANCELLED means the prompt was dismissed, timed out, or never');
      say('   completed. It says NOTHING about PRF support. Re-run and complete');
      say('   the Touch ID / passkey prompt.');
    }
    say('--- raw ceremony, for the exact error ---');
    try {
      const cred = await navigator.credentials.create({ publicKey: {
        challenge: crypto.getRandomValues(new Uint8Array(32)),
        rp: { name: 'Codename One' },
        user: { id: crypto.getRandomValues(new Uint8Array(16)), name: 'vault check', displayName: 'vault check' },
        pubKeyCredParams: [{ type: 'public-key', alg: -7 }, { type: 'public-key', alg: -257 }],
        authenticatorSelection: { residentKey: 'required', requireResidentKey: true, userVerification: 'required' },
        extensions: { prf: {} }
      }});
      const ext = cred.getClientExtensionResults();
      say('raw create: SUCCEEDED');
      say('clientExtensionResults: ' + JSON.stringify(ext));
      if (ext && ext.prf && ext.prf.enabled) {
        say('>>> the authenticator DOES support PRF (prf.enabled=true) <<<');
        say('    so the CANCELLED above was the first ceremony being dismissed');
      } else {
        say('>>> the authenticator registered a passkey and reports NO PRF <<<');
        say('    this is the genuine unsupported case');
      }
    } catch (err) {
      say('raw create threw: ' + err.name + ': ' + err.message);
      say('    NotAllowedError  = dismissed/timed out -> inconclusive, try again');
      say('    NotSupportedError/ConstraintError = genuinely unsupported');
    }
    return;
  }
  const d = await call({ op: 'prfDerive', keyId: KEY });
  say('derive: ' + name(status(d)) + '  bytes=' + payload(d).length);
  if (status(d) === 0) {
    firstDerive = JSON.stringify(payload(d));
    // localStorage rather than session storage: a browser restart clears the
    // per-session kind, and surviving a restart is exactly what step 4 checks.
    // Measured -- the first version of this page answered "no earlier value to
    // compare" in the one situation it was written for.
    localStorage.setItem('cn1FirstDerive', firstDerive);
    say('derived (first 8 bytes): ' + payload(d).slice(0, 8).join(','));
    say('>>> PRF WORKS ON THIS AUTHENTICATOR <<<');
  }
};

document.getElementById('again').onclick = async () => {
  say('--- derive again (must match) ---');
  const d = await call({ op: 'prfDerive', keyId: KEY });
  say('derive: ' + name(status(d)));
  if (status(d) !== 0) { return; }
  const now = JSON.stringify(payload(d));
  const before = firstDerive || localStorage.getItem('cn1FirstDerive');
  say('derived (first 8 bytes): ' + payload(d).slice(0, 8).join(','));
  say(before ? ('stable across calls: ' + (now === before)) : 'no earlier value to compare');
  say('(reload the page, or restart the browser, and press this again to check it survives)');
};

// Step 3 lets the browser offer every passkey provider, which on a Mac means a
// chooser (iCloud Keychain, a phone, a security key). If that chooser is what
// got dismissed, this pins the ceremony to the built-in platform authenticator
// so Touch ID answers directly. It is a diagnostic, not what the port does --
// the port deliberately does not constrain attachment.
document.getElementById('platform').onclick = async () => {
  say('--- raw ceremony, platform authenticator only ---');
  try {
    const cred = await navigator.credentials.create({ publicKey: {
      challenge: crypto.getRandomValues(new Uint8Array(32)),
      rp: { name: 'Codename One' },
      user: { id: crypto.getRandomValues(new Uint8Array(16)), name: 'vault check', displayName: 'vault check' },
      pubKeyCredParams: [{ type: 'public-key', alg: -7 }, { type: 'public-key', alg: -257 }],
      authenticatorSelection: { authenticatorAttachment: 'platform', residentKey: 'required',
                                requireResidentKey: true, userVerification: 'required' },
      extensions: { prf: {} }
    }});
    const ext = cred.getClientExtensionResults();
    say('create: SUCCEEDED   clientExtensionResults: ' + JSON.stringify(ext));
    const supported = !!(ext && ext.prf && ext.prf.enabled);
    say(supported ? '>>> Touch ID DOES support PRF <<<' : '>>> Touch ID does NOT support PRF <<<');
    if (supported) {
      const salt = crypto.getRandomValues(new Uint8Array(32));
      const assertion = await navigator.credentials.get({ publicKey: {
        challenge: crypto.getRandomValues(new Uint8Array(32)),
        allowCredentials: [{ type: 'public-key', id: new Uint8Array(cred.rawId) }],
        userVerification: 'required',
        extensions: { prf: { eval: { first: salt } } }
      }});
      const r = assertion.getClientExtensionResults();
      const first = r && r.prf && r.prf.results ? new Uint8Array(r.prf.results.first) : null;
      say('derive: ' + (first ? (first.length + ' bytes, first 8: ' + Array.from(first.slice(0, 8)).join(',')) : 'NO PRF RESULT'));
    }
  } catch (err) {
    say('threw: ' + err.name + ': ' + err.message);
  }
};

document.getElementById('reset').onclick = async () => {
  say('forget passkey: ' + name(status(await call({ op: 'prfForget', keyId: KEY }))));
  say('delete device key: ' + name(status(await call({ op: 'deleteKey', keyId: KEY }))));
  localStorage.removeItem('cn1FirstDerive');
};
</script>
</body></html>`;

const server = http.createServer((req, res) => {
  res.writeHead(200, { 'Content-Type': 'text/html; charset=utf-8' });
  res.end(page);
});
server.listen(port, '127.0.0.1', () => {
  console.log('Open this in your browser:  http://localhost:' + port + '/');
  console.log('(localhost, not 127.0.0.1 -- WebAuthn refuses IP-address origins.)');
  console.log('Ctrl+C to stop.');
});
