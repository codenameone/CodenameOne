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
// Scenario driver for the __cn1_vault__ host bridge.
//
// JavascriptVaultBridgeTest slices the bridge out of browser_bridge.js and
// concatenates it AFTER this file's preamble and BEFORE the scenarios at the
// bottom, so the code under test is the shipped code rather than a copy.
//
// What the stubs supply is deliberately minimal: Node's own Web Crypto (which
// has non-extractable keys, AES-GCM with associated data, and a structured
// clone that carries a CryptoKey), plus an IndexedDB that implements only what
// the bridge uses. The interesting behaviour it has to model is ``add``
// rejecting a duplicate key with a ConstraintError, because that is the whole
// of the cross-tab convergence the bridge depends on.

'use strict';

const results = {};

// A rejection nobody awaited kills node with an empty stdout, and the Java side then reports
// only "the harness produced no results" -- which is what a genuine bridge bug looked like when
// the transaction-abort rule was first modelled here. Reported as a result instead, so the next
// one says what happened.
process.on('unhandledRejection', (reason) => {
  console.log(JSON.stringify({
    harnessError: 'unhandled rejection: ' + String(reason && reason.stack ? reason.stack : reason)
  }));
  process.exit(1);
});

function fail(name, detail) {
  results[name] = 'FAILED: ' + detail;
}

// ---------------------------------------------------------------- IndexedDB

function makeIndexedDb() {
  const stores = new Map();
  let failEveryOpen = false;
  // Counted so the convergence assertion cannot pass vacuously: if the two
  // ensureKey calls happened to serialise, no ConstraintError is raised, the
  // losing branch never runs, and a test that only checked "both succeeded"
  // would be reporting on a race that did not happen.
  let constraintErrors = 0;
  let clonedKeys = 0;
  // A real transaction can still abort AFTER its requests have reported success -- quota, a
  // storage failure, the tab going away. The stub had no transaction lifecycle at all, so the
  // bridge's commit wait had nothing to listen to and the difference between "the request
  // succeeded" and "the write is durable" could not be expressed here.
  let failEveryCommit = false;

  // onAbort is how the stub models the rule that cost a CI round: an IndexedDB request error
  // that is not cancelled with preventDefault() goes on to ABORT its transaction. Settling a
  // promise in onerror says nothing to the DOM, so a handler that only resolved still lost the
  // whole transaction -- which nothing here could see, because this stub simply had no such
  // rule and the Chromium suite was the first thing to notice.
  function request(run, onAbort) {
    const req = { onsuccess: null, onerror: null, result: undefined, error: null };
    queueMicrotask(() => {
      try {
        req.result = run();
        if (req.onsuccess) req.onsuccess();
      } catch (e) {
        req.error = e;
        let prevented = false;
        const event = { preventDefault() { prevented = true; } };
        if (req.onerror) req.onerror(event);
        if (!prevented && onAbort) {
          onAbort(e);
        }
      }
    });
    return req;
  }

  const db = {
    objectStoreNames: { contains: (n) => stores.has(n) },
    createObjectStore(name) {
      stores.set(name, new Map());
      return {};
    },
    transaction(name) {
      if (failEveryOpen) {
        // What a real browser does once the connection is closed: the call
        // throws rather than reporting through an error handler.
        const err = new Error('connection is closed');
        err.name = 'InvalidStateError';
        throw err;
      }
      const data = stores.get(name);
      // Writes land immediately and an ABORT rolls them back, rather than being buffered until
      // commit. Buffering would be the other faithful model, but not for what this harness is
      // for: two tabs are two connections and their adds genuinely interleave, which is the
      // whole of the ConstraintError convergence this file exists to exercise. Per-transaction
      // buffers hid that from each other and the convergence test went vacuous. An undo log
      // keeps the race and still discards an aborted transaction's writes.
      const undo = [];
      const remember = (key) => {
        undo.push({ key: key, had: data.has(key), prev: data.get(key) });
      };
      const rollback = () => {
        for (let i = undo.length - 1; i >= 0; i--) {
          const entry = undo[i];
          if (entry.had) {
            data.set(entry.key, entry.prev);
          } else {
            data.delete(entry.key);
          }
        }
        undo.length = 0;
      };
      // Settles after the requests queued on it, the way a real one does: the handlers are
      // attached during this turn, so the completion has to be scheduled behind them.
      const tx = { oncomplete: null, onabort: null, onerror: null, error: null };
      setTimeout(() => {
        if (aborted) {
          return;
        }
        if (failEveryCommit) {
          aborted = true;
          rollback();
          tx.error = Object.assign(new Error('quota exceeded'), { name: 'QuotaExceededError' });
          if (tx.onabort) tx.onabort();
          return;
        }
        if (tx.oncomplete) tx.oncomplete();
      }, 0);
      // Rolls the transaction back and fires onabort, exactly as an uncancelled request error
      // does in a browser.
      let aborted = false;
      const abort = (cause) => {
        if (aborted) {
          return;
        }
        aborted = true;
        rollback();
        tx.error = cause || null;
        // DISPATCHED as a task, not inline. A browser fires abort as an event, so handlers
        // attached later in the same turn still see it -- and cn1VaultCommit attaches its
        // handler in the .then AFTER the request settles. Calling tx.onabort inline found it
        // still null, nothing ever settled the commit promise, and the harness exited zero with
        // empty stdout: a hang dressed as a pass.
        setTimeout(() => { if (tx.onabort) tx.onabort(); }, 0);
      };
      const store = {
        get: (key) => request(() => data.get(key), abort),
        add: (record) => request(() => {
          if (data.has(record.id)) {
            constraintErrors++;
            const err = new Error('key already exists');
            err.name = 'ConstraintError';
            throw err;
          }
          // A real IndexedDB structured-clones what it stores, and a CryptoKey is
          // one of the few host objects that survives that -- which is the whole
          // reason this design is possible. Attempted rather than assumed,
          // because whether a given Node version can clone a CryptoKey is a fact
          // about the runtime and not about the code under test; a runtime that
          // cannot falls back to storing the reference and says so.
          let stored;
          try {
            stored = structuredClone(record);
            clonedKeys++;
          } catch (e) {
            stored = record;
          }
          remember(record.id);
          data.set(record.id, stored);
          return record.id;
        }, abort),
        delete: (key) => request(() => { remember(key); data.delete(key); return undefined; },
            abort)
      };
      tx.objectStore = () => store;
      return tx;
    }
  };

  return {
    setFailEveryOpen(value) { failEveryOpen = value; },
    setFailEveryCommit(value) { failEveryCommit = value; },
    constraintErrors() { return constraintErrors; },
    clonedKeys() { return clonedKeys; },
    recordCount(name) { return stores.has(name) ? stores.get(name).size : 0; },
    open() {
      const req = { onsuccess: null, onerror: null, onupgradeneeded: null, onblocked: null, result: db };
      queueMicrotask(() => {
        if (failEveryOpen) {
          if (req.onerror) req.onerror();
          return;
        }
        if (!stores.has('keys') && req.onupgradeneeded) req.onupgradeneeded();
        if (req.onsuccess) req.onsuccess();
      });
      return req;
    }
  };
}

const fakeIndexedDb = makeIndexedDb();

// ---------------------------------------------------------------- the globals the bridge reads

// A plain object rather than globalThis: Node defines ``navigator`` as a
// getter-only property, and the bridge reads ``global.navigator.storage`` when
// it reports persistence. Giving it its own object keeps the stub writable and
// keeps the test from depending on whatever the host runtime happens to expose.
const global = {
  indexedDB: fakeIndexedDb,
  isSecureContext: true,
  crypto: globalThis.crypto,
  navigator: { storage: { persisted: () => Promise.resolve(false) } },
  window: undefined,
  PublicKeyCredential: undefined
};

let vaultHandler = null;
const hostBridge = {
  register(name, fn) {
    if (name === '__cn1_vault__') {
      vaultHandler = fn;
    }
  }
};

function cn1CryptoApi() {
  return globalThis.crypto;
}

// CN1_VAULT_BRIDGE_SOURCE

// ---------------------------------------------------------------- scenarios

const KEY = 'vault-under-test';
const AAD = [1, 2, 3, 4];
const SECRET = [9, 8, 7, 6, 5];

function call(request) {
  return Promise.resolve(vaultHandler(request));
}

function status(reply) {
  return reply[0];
}

function payload(reply) {
  return reply.slice(1);
}

(async () => {
  try {
    if (!vaultHandler) {
      throw new Error('the bridge did not register a __cn1_vault__ handler');
    }

    // Capabilities. Bit 1 secure context, bit 2 subtle, bit 4 IndexedDB opened.
    const caps = await call({ op: 'capabilities' });
    results.capabilitiesStatus = status(caps);
    results.capabilityBits = payload(caps)[0];

    // Nothing stored yet, and the store can say so. That distinction is the one
    // the Java side needs before it will create anything.
    const before = await call({ op: 'keyState', keyId: KEY });
    results.keyStateBeforeStatus = status(before);
    results.keyStateBefore = payload(before)[0];

    // Unwrapping with no key is KEY_MISSING (1), never a generic failure.
    const orphan = await call({
      op: 'unwrap', keyId: KEY, data: new Array(40).fill(7), aad: AAD
    });
    results.unwrapWithNoKeyStatus = status(orphan);

    // Two callers race with nothing stored. Both generate; the store accepts
    // one; the loser must adopt the winner's key rather than overwrite it --
    // which is only observable by checking that what one wraps, the other opens.
    const [a, b] = await Promise.all([
      call({ op: 'ensureKey', keyId: KEY }),
      call({ op: 'ensureKey', keyId: KEY })
    ]);
    results.ensureA = status(a);
    results.ensureB = status(b);

    const after = await call({ op: 'keyState', keyId: KEY });
    results.keyStateAfter = payload(after)[0];
    // The race really raced, and it left one key rather than two.
    results.constraintErrors = fakeIndexedDb.constraintErrors();
    results.storedKeyCount = fakeIndexedDb.recordCount('keys');
    // Informational: whether this runtime could structured-clone the CryptoKey.
    // A browser can, which is what makes IndexedDB a place to keep one.
    results.clonedKeys = fakeIndexedDb.clonedKeys();

    const wrapped = await call({ op: 'wrap', keyId: KEY, data: SECRET, aad: AAD });
    results.wrapStatus = status(wrapped);
    const sealed = payload(wrapped);
    results.sealedLength = sealed.length;
    // A 12 byte nonce, the plaintext, and a 16 byte tag.
    results.sealedOverhead = sealed.length - SECRET.length;

    const opened = await call({ op: 'unwrap', keyId: KEY, data: sealed, aad: AAD });
    results.unwrapStatus = status(opened);
    results.roundTripped = JSON.stringify(payload(opened)) === JSON.stringify(SECRET);

    // Two wraps of the same bytes must differ: a repeated nonce under one
    // AES-GCM key is catastrophic rather than merely weak.
    const again = await call({ op: 'wrap', keyId: KEY, data: SECRET, aad: AAD });
    results.noncesDiffer = JSON.stringify(payload(again)) !== JSON.stringify(sealed);

    // A different binding fails the tag. This is what stops ciphertext being
    // moved from one account to another.
    const wrongAad = await call({
      op: 'unwrap', keyId: KEY, data: sealed, aad: [9, 9, 9, 9]
    });
    results.wrongAadStatus = status(wrongAad);
    results.wrongAadPayloadEmpty = payload(wrongAad).length === 0;

    // A flipped bit in the ciphertext likewise.
    const tampered = sealed.slice();
    tampered[tampered.length - 1] ^= 1;
    const tamperedReply = await call({ op: 'unwrap', keyId: KEY, data: tampered, aad: AAD });
    results.tamperedStatus = status(tamperedReply);

    // And a flipped bit in the nonce, which is authenticated by being an input
    // rather than by the tag covering it.
    const movedNonce = sealed.slice();
    movedNonce[0] ^= 1;
    const movedReply = await call({ op: 'unwrap', keyId: KEY, data: movedNonce, aad: AAD });
    results.movedNonceStatus = status(movedReply);

    // Truncated to less than a nonce.
    const short = await call({ op: 'unwrap', keyId: KEY, data: [1, 2, 3], aad: AAD });
    results.shortStatus = status(short);

    // The key the bridge stored must be non-extractable. Asserted here rather
    // than trusted, because "we passed false to generateKey" is a claim about
    // source and this is a claim about the object that exists.
    const storedDb = await new Promise((resolve, reject) => {
      const req = fakeIndexedDb.open();
      req.onsuccess = () => resolve(req.result);
      req.onerror = () => reject(new Error('open failed'));
    });
    const storedRecord = await new Promise((resolve, reject) => {
      const req = storedDb.transaction('keys').objectStore().get(KEY);
      req.onsuccess = () => resolve(req.result);
      req.onerror = () => reject(new Error('get failed'));
    });
    results.storedKeyExtractable = storedRecord.key.extractable;
    try {
      await crypto.subtle.exportKey('raw', storedRecord.key);
      results.exportRejected = false;
    } catch (e) {
      results.exportRejected = true;
    }

    // Deleting it makes the state absent again, and an unwrap of the old
    // ciphertext becomes KEY_MISSING rather than an authentication failure.
    const deleted = await call({ op: 'deleteKey', keyId: KEY });
    results.deleteStatus = status(deleted);
    const gone = await call({ op: 'keyState', keyId: KEY });
    results.keyStateAfterDelete = payload(gone)[0];

    // A store that cannot be reached must not answer "absent": the Java side
    // reads absence as permission to create a replacement key, and creating one
    // over a key that was there orphans everything it protected. The connection
    // is closed underneath, which is what a browser does when the user clears
    // site data -- the cached handle survives and every transaction on it
    // throws.
    fakeIndexedDb.setFailEveryOpen(true);
    const unreachable = await call({ op: 'keyState', keyId: KEY });
    results.keyStateUnreachableStatus = status(unreachable);
    const unreachableUnwrap = await call({
      op: 'unwrap', keyId: KEY, data: sealed, aad: AAD
    });
    results.unwrapUnreachableStatus = status(unreachableUnwrap);
    fakeIndexedDb.setFailEveryOpen(false);

    // And it recovers: the bridge drops the dead connection rather than caching
    // it forever, so the next call opens a fresh one.
    const recovered = await call({ op: 'keyState', keyId: KEY });
    results.keyStateAfterRecovery = status(recovered);

    // A write whose TRANSACTION aborts is not a write. The request reports success first --
    // that is how IndexedDB is specified -- so a bridge that answered there handed back a key
    // that never became durable, and the caller went on to wrap real records under it.
    fakeIndexedDb.setFailEveryCommit(true);
    const uncommitted = await call({ op: 'ensureKey', keyId: 'commit-probe' });
    results.ensureKeyUncommittedStatus = status(uncommitted);
    fakeIndexedDb.setFailEveryCommit(false);
    // And nothing was left behind claiming to be a key.
    const afterAbort = await call({ op: 'keyState', keyId: 'commit-probe' });
    results.keyStateAfterAbort = payload(afterAbort)[0];

    console.log(JSON.stringify(results));
  } catch (e) {
    console.log(JSON.stringify({ harnessError: String(e && e.stack ? e.stack : e) }));
    process.exitCode = 1;
  }
})();
