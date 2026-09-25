---
title: "Your Push Request Succeeded. The Device Key Is Dead."
slug: push-feedback-retire-dead-tokens
url: /blog/push-feedback-retire-dead-tokens/
date: '2026-09-30'
author: Shai Almog
description: "Use signed BuildCloud delivery digests to retire invalid push targets, retain recoverable ones, and acknowledge outcomes after durable database updates."
feed_html: '<img src="https://www.codenameone.com/blog/push-feedback-retire-dead-tokens.jpg" alt="Push feedback tells you which device keys to retire" /> Use signed BuildCloud delivery digests to retire invalid push targets, retain recoverable ones, and acknowledge outcomes after durable database updates.'
series: ["release-2026-09-25"]
---

![Push feedback tells you which device keys to retire](/blog/push-feedback-retire-dead-tokens.jpg)

Your server sends a push request and gets a successful response. Somewhere in the target list is a device key that stopped working weeks ago. The response can't identify it because the provider hasn't answered yet.

BuildCloud now sends delivery feedback to your backend. It closes the gap between admitting the request and learning what happened during provider delivery, so your own device database can stop retaining keys that the provider has rejected.

The feedback is a **signed daily digest**, configured per organization on Pro and above. It isn't an immediate webhook for every notification, and provider acceptance isn't proof that a device displayed or a person read the message.

## Configure the return path

In the Console, open **Push > Settings > Delivery feedback**. Set an HTTPS endpoint, enable the daily digest, and use **Send test event** to exercise the receiver. The card supplies the signing secret and reports what the endpoint returned.

The setting belongs to the organization, so a digest can contain outcomes for several applications. Store the `deliveryId` returned by a v3 send alongside the application and target you sent to. Classic sends don't return that ID, so their correlation uses the device key.

{{< mermaid >}}
flowchart TD
    Send[Your backend sends notification] --> Queue[BuildCloud admits it to queue]
    Queue --> Attempt[Provider delivery attempt]
    Attempt --> Outcome[Provider accepts or rejects]
    Outcome --> Digest[BuildCloud signs daily digest]
    Digest --> Verify[Your receiver verifies signature]
    Verify --> Commit[Commit deduplication and cleanup]
    Commit --> Ack[Return 2xx to acknowledge]
{{< /mermaid >}}

A successful send response is an admission result. The later digest describes provider outcomes. Keeping those two stages separate prevents a successful HTTP request from becoming a misleading “delivered” label in your own administration UI.

## Delete on the reason, not the status

The digest itemizes failures and summarizes accepted deliveries. This shortened example shows the part a cleanup handler needs:

```json
{
  "deliveryId": "6f0f6f1e-9a1e-4f2a-9a7a-6d5f5f6a1b02",
  "provider": "fcm",
  "token": "example-device-token",
  "status": "FAILED",
  "reason": "INVALID_TARGET",
  "attempts": 1
}
```

| Reason | What your device store should do |
| --- | --- |
| `INVALID_TARGET` | Remove the rejected key |
| `PERMANENT_FAILURE` | Keep the key; investigate the message or credentials |
| `TRANSIENT_FAILURE` | Keep the key; attempts were exhausted, but the target may still work |

A transient failure can have `status: "DEAD"`. That describes the exhausted delivery job. It doesn't establish that the device key is dead.

## Match the Key Your App Actually Stored

`Push.getPushKey()` returns a full key such as `cn1-fcm-example-device-token`. The digest's `token` is only `example-device-token`. Comparing those strings deletes nothing. Matching `device` alone also misses legacy `cn1-gcm-` keys because the digest uses the canonical `cn1-fcm-` spelling.

Keep the original key for sending, and store a normalized provider and target alongside it. This registration helper handles Android and Apple mobile keys:

```javascript
function mobileTarget(pushKey) {
  const match = /^cn1-(gcm|fcm|ios|apns)-(.+)$/.exec(pushKey);
  if (!match) throw new Error('Expected an Android or Apple push key');
  const provider = {gcm: 'fcm', ios: 'apns'}[match[1]] || match[1];
  return {provider, target: match[2], pushKey};
}
```

Both `cn1-gcm-abc` and `cn1-fcm-abc` now match feedback for provider `fcm`, token `abc`. Apple keys with the `ios` prefix match provider `apns`. For web push, store the subscription's `endpoint` separately from its key material; don't treat the whole `cn1-web-...` key as an endpoint.

**Backfill these normalized columns for existing registrations before enabling cleanup.** The examples below expect that schema. Scope lookups to the organization receiving the digest, and use both provider and target so identical strings from different providers don't collide.

## Verify before interpreting the body

The request carries:

```text
X-CN1-Signature: t=<unix-millis>,v1=<hex>
```

The signature is HMAC-SHA256 over the timestamp, a period, and the raw body bytes. Use the secret from the Settings card, compare the MAC in constant time, and reject timestamps outside your allowed age window. Do not parse JSON and serialize it again before verification; whitespace and field ordering can change the signed bytes.

For the Codename One backend, the guide includes a [complete Java receiver](https://github.com/codenameone/CodenameOne/blob/master/docs/demos/backend/src/main/java/com/codenameone/developerguide/backend/PushFeedback.java). It uses the raw `HttpServer.Request`, `Crypto.hmacSha256`, and `Crypto.equalsConstantTime`, so it also fits native backend packaging. Its device store is an application-supplied interface, not an automatically provisioned database.

After verification, a CN1 backend receiver can apply each failure like this:

```java
// Inside the loop over the verified digest's event maps.
if (!"INVALID_TARGET".equals(event.get("reason"))) {
    continue;
}
String provider = (String) event.get("provider");
String target = (String) event.get("token");
if (target == null || target.length() == 0) {
    target = (String) event.get("endpoint");
}
if (provider == null || provider.length() == 0
        || target == null || target.length() == 0) {
    throw new IllegalArgumentException("Missing push target");
}
String deliveryId = (String) event.get("deliveryId");
String eventKey;
if (deliveryId != null && deliveryId.length() > 0) {
    eventKey = "delivery:" + deliveryId;
} else {
    Object at = event.get("at");
    if (at == null) {
        throw new IllegalArgumentException("Missing classic event timestamp");
    }
    eventKey = "classic:" + provider + ":" + target.length()
            + ":" + target + ":" + at;
}
if (!store.alreadyApplied(eventKey)) {
    store.removeTargetAndMarkApplied(eventKey, provider, target);
}
```

Here `store` is an organization-scoped durable store with `alreadyApplied(eventKey)` and `removeTargetAndMarkApplied(eventKey, provider, target)` methods. The latter deletes by the normalized columns and writes the marker in one transaction. It must enforce a unique event key even if two requests pass the initial check concurrently.

The fallback key includes the provider, target, and event timestamp. It handles classic events without a `deliveryId`, distinguishes devices, and lets a later event for the same target be processed. A missing target or timestamp fails the request instead of acknowledging an event we couldn't correlate.

If a device has since registered a replacement key, remove the rejected key associated with the event. Don't translate an old failure into “delete whatever key this user has now.”

## Acknowledge after the database commits

Delivery is at-least-once. A lost HTTP response can cause a digest to arrive again even after you processed it.

Only a 2xx response advances the sender's watermark. Return it after your writes are durable. If you return success first and the database write then fails, the acknowledged window won't be resent. If the database commits and the response gets lost, the durable event-key marker lets the retry do nothing safely.

A digest has a size cap. `truncated: true` means another page follows in the same run, so the receiver must handle several requests close together. `eventsOmitted` counts accepted deliveries included only in the summary. It doesn't mean a set of unreported failure events has been lost.

A failing receiver is retried and eventually disabled after repeated failures. Check the reason on the Settings card and exercise the test event after fixing the endpoint. A receiver that has quietly stopped accepting feedback won't keep your database clean.

## Use the backend you already run

The [push guide](/developer-guide/push-notifications/) includes CN1 backend, Spring, MicroProfile, and Node/serverless receiver examples. PHP and other stacks can implement the same signed JSON contract, but the guide doesn't include a PHP receiver.

Here is the equivalent event-key helper for Node:

```javascript
function cleanupTarget(event) {
  const provider = event.provider;
  const target = event.token || event.endpoint;
  if (typeof provider !== 'string' || !provider
      || typeof target !== 'string' || !target) {
    throw new Error('Missing push target');
  }
  let eventKey;
  if (typeof event.deliveryId === 'string' && event.deliveryId) {
    eventKey = `delivery:${event.deliveryId}`;
  } else {
    if (!Number.isSafeInteger(event.at)) {
      throw new Error('Missing classic event timestamp');
    }
    eventKey = `classic:${provider}:${target.length}:${target}:${event.at}`;
  }
  return {eventKey, provider, target};
}
```

For a concrete durable store, this Node example uses the built-in `node:sqlite` module. Run it on a Node version that provides that module. The `devices` table holds the normalized registration described above; populate it as devices register, and migrate existing keys before turning on the receiver.

```javascript
const {DatabaseSync} = require('node:sqlite');
const db = new DatabaseSync('push-feedback.sqlite');
db.exec(`
  CREATE TABLE IF NOT EXISTS devices (
    organization TEXT NOT NULL,
    provider TEXT NOT NULL,
    target TEXT NOT NULL,
    push_key TEXT NOT NULL,
    PRIMARY KEY (organization, provider, target)
  );
  CREATE TABLE IF NOT EXISTS applied_feedback (
    organization TEXT NOT NULL,
    event_key TEXT NOT NULL,
    PRIMARY KEY (organization, event_key)
  );
`);

function applyInvalidTarget(organization, event) {
  if (event.reason !== 'INVALID_TARGET') return;
  const {eventKey, provider, target} = cleanupTarget(event);
  db.exec('BEGIN IMMEDIATE');
  try {
    const marker = db.prepare(`
      INSERT OR IGNORE INTO applied_feedback (organization, event_key)
      VALUES (?, ?)
    `).run(organization, eventKey);
    if (marker.changes) {
      db.prepare(`
        DELETE FROM devices
        WHERE organization = ? AND provider = ? AND target = ?
      `).run(organization, provider, target);
    }
    db.exec('COMMIT');
  } catch (error) {
    db.exec('ROLLBACK');
    throw error;
  }
}
```

Use the configured organization's ID after verifying the digest signature. Process its events through `applyInvalidTarget`, then return 2xx. On a database error, return a non-2xx response so the digest can be retried. The marker and deletion share a transaction: a failed deletion rolls the marker back too. A serverless deployment needs durable storage outside an ephemeral function filesystem; the local SQLite file above is for a server with persistent storage.

[PR #5890](https://github.com/codenameone/CodenameOne/pull/5890) adds those examples and documents the contract. Start with the test event, then send the same valid digest to your test receiver twice. Confirm that both requests get an appropriate response and only one durable cleanup operation occurs. Also test a failed database write: that request must not acknowledge a window it failed to apply.

---

## Discussion

_Does your push database distinguish a rejected device key from a delivery attempt that simply ran out of retries?_

{{< giscus >}}
