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
// re-serialisation is not byte-identical, so it will never verify.
const express = require('express');
const crypto = require('crypto');

const app = express();
const SECRET = process.env.CN1_PUSH_CALLBACK_SECRET;
const MAX_AGE_MS = 5 * 60 * 1000;
const applied = new Set();          // stand-in for a durable "already applied" table

app.post('/push/feedback', express.raw({type: 'application/json'}), (req, res) => {
  const header = req.get('X-CN1-Signature') || '';
  const parts = Object.fromEntries(
      header.split(',').map(p => [p.slice(0, p.indexOf('=')), p.slice(p.indexOf('=') + 1)]));
  const body = req.body.toString('utf8');

  const expected = Buffer.from(
      crypto.createHmac('sha256', SECRET).update(`${parts.t}.${body}`).digest('hex'), 'utf8');
  const provided = Buffer.from(parts.v1 || '', 'utf8');
  if (expected.length !== provided.length
      || !crypto.timingSafeEqual(expected, provided)
      || Math.abs(Date.now() - Number(parts.t)) > MAX_AGE_MS) {
    return res.sendStatus(401);     // not 2xx, so the same window is resent
  }

  const digest = JSON.parse(body);
  for (const event of digest.events || []) {
    if (applied.has(event.deliveryId)) {
      continue;                     // digests are at-least-once; dedupe on this
    }
    applied.add(event.deliveryId);
    if (event.reason === 'INVALID_TARGET') {
      deleteDeviceKey(event.token || event.endpoint);
    }
  }
  // Answer 2xx only once those writes are durable: it advances our watermark
  // and this window is never sent again.
  res.sendStatus(200);
});
// end::push-notifications-javascript-003[]

// tag::push-notifications-javascript-004[]
// A serverless function behind an HTTP gateway. Two things differ from a
// long-lived server, and both are silent when wrong.
exports.handler = async (event) => {
  // 1. The gateway may hand you the body base64-encoded, and it decides that
  //    on content sniffing rather than on anything you control. Signing the
  //    encoded form verifies nothing and fails closed on every digest.
  const body = event.isBase64Encoded
      ? Buffer.from(event.body, 'base64').toString('utf8')
      : event.body;

  const header = event.headers['x-cn1-signature'] || event.headers['X-CN1-Signature'] || '';
  if (!verify(header, body)) {      // same HMAC check as the Express handler
    return {statusCode: 401, body: 'bad signature'};
  }

  // 2. There is no process to keep state in, so the dedupe set and the device
  //    table both have to be the database. A warm container that remembers
  //    delivery ids is an optimisation, never the correctness mechanism.
  const digest = JSON.parse(body);
  await applyDurably(digest.events || []);

  // Return after the writes, not alongside them: a function that answers 200
  // and is frozen before its writes flush has told us to advance the watermark
  // over a window nobody stored.
  return {statusCode: 200, body: 'ok'};
};
// end::push-notifications-javascript-004[]
