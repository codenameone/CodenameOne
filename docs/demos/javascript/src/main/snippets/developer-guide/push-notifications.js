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
// Generated from docs/developer-guide source blocks. Edit the guide snippets here, not inline.

// tag::push-notifications-javascript-001[]
{"error":"Error message"}
// end::push-notifications-javascript-001[]

// tag::push-notifications-javascript-002[]
[
 {"id"="deviceId","status"="error","message"="Invalid Device ID"},
 {"id"="cn1-gcm-nativegcmkey","status"="updateId", "newId"="cn1-gcm-newgcmkey"},
 {"id"="cn1-gcm-okgcmkey","status"="OK"},
 {"id"="cn1-gcm-errorkey","status"="error","message"="Server error message"},
 {"id"="cn1-ios-iphonekey","status"="inactive"},
]
// end::push-notifications-javascript-002[]

// tag::push-notifications-javascript-003[]
// Express. Verify before any JSON middleware touches the body: the signature
// covers the bytes we sent, and express.json() hands you an object whose
// re-serialisation is not byte-identical, so it never verifies.
const express = require('express');
const crypto = require('crypto');

const app = express();
const SECRET = process.env.CN1_PUSH_CALLBACK_SECRET;
const MAX_AGE_MS = 5 * 60 * 1000;

function verifySignature(header, body) {
  const parts = Object.fromEntries((header || '').split(',')
      .map(p => [p.slice(0, p.indexOf('=')).trim(), p.slice(p.indexOf('=') + 1).trim()]));
  const expected = Buffer.from(
      crypto.createHmac('sha256', SECRET).update(`${parts.t}.${body}`).digest('hex'), 'utf8');
  const provided = Buffer.from(parts.v1 || '', 'utf8');
  return expected.length === provided.length
      && crypto.timingSafeEqual(expected, provided)
      && Math.abs(Date.now() - Number(parts.t)) <= MAX_AGE_MS;
}

app.post('/push/feedback', express.raw({type: 'application/json'}), async (req, res) => {
  const body = req.body.toString('utf8');
  if (!verifySignature(req.get('X-CN1-Signature'), body)) {
    return res.sendStatus(401);       // not 2xx, so the same window is resent
  }

  try {
    for (const event of JSON.parse(body).events || []) {
      // Digests are at-least-once, so the deduplication marker and the
      // deletion have to commit together: a marker stored first turns a retry
      // into a silent skip, and a deletion without one is applied twice.
      if (event.reason === 'INVALID_TARGET') {
        await removeKeyIfNotApplied(event.deliveryId, event.token || event.endpoint);
      } else {
        await markApplied(event.deliveryId);
      }
    }
  } catch (failure) {
    // Let it be resent rather than acknowledging work that did not land.
    return res.sendStatus(500);
  }

  // 2xx only now: it advances our watermark and this window is never sent again.
  res.sendStatus(200);
});
// end::push-notifications-javascript-003[]

// tag::push-notifications-javascript-004[]
// A serverless function behind an HTTP gateway. Two things differ from a
// long-lived server, and both are silent when they are wrong.
const crypto = require('crypto');

const SECRET = process.env.CN1_PUSH_CALLBACK_SECRET;
const MAX_AGE_MS = 5 * 60 * 1000;

function verifySignature(header, body) {
  const parts = Object.fromEntries((header || '').split(',')
      .map(p => [p.slice(0, p.indexOf('=')).trim(), p.slice(p.indexOf('=') + 1).trim()]));
  const expected = Buffer.from(
      crypto.createHmac('sha256', SECRET).update(`${parts.t}.${body}`).digest('hex'), 'utf8');
  const provided = Buffer.from(parts.v1 || '', 'utf8');
  return expected.length === provided.length
      && crypto.timingSafeEqual(expected, provided)
      && Math.abs(Date.now() - Number(parts.t)) <= MAX_AGE_MS;
}

exports.handler = async (event) => {
  // 1. The gateway may hand you the body base64-encoded, and it decides that
  //    by sniffing content rather than by anything you control. Hashing the
  //    encoded form verifies nothing and fails closed on every digest.
  const body = event.isBase64Encoded
      ? Buffer.from(event.body, 'base64').toString('utf8')
      : event.body;

  // Header names arrive lower-cased from some gateways and not from others.
  const headers = event.headers || {};
  const signature = headers['x-cn1-signature'] || headers['X-CN1-Signature'];
  if (!verifySignature(signature, body)) {
    return {statusCode: 401, body: 'bad signature'};
  }

  // 2. There is no process to keep state in, so deduplication and the device
  //    table both have to be the database. A warm container that remembers
  //    delivery ids is an optimisation, never the correctness mechanism.
  await applyDurably(JSON.parse(body).events || []);

  // Return after the writes, not alongside them: a function that answers 200
  // and is frozen before its writes flush has told us to advance the watermark
  // over a window nobody stored.
  return {statusCode: 200, body: 'ok'};
};
// end::push-notifications-javascript-004[]

