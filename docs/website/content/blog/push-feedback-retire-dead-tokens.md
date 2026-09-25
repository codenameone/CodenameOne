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

Match the canonical `token` field, or `endpoint` for web push. Matching only `device` can miss older `cn1-gcm-` entries because the digest normalizes that alias to `cn1-fcm-`. A data-cleanup job that never finds its legacy keys looks reassuringly quiet while doing nothing.

## Verify before interpreting the body

The request carries:

```text
X-CN1-Signature: t=<unix-millis>,v1=<hex>
```

The signature is HMAC-SHA256 over the timestamp, a period, and the raw body bytes. Use the secret from the Settings card, compare the MAC in constant time, and reject timestamps outside your allowed age window. Do not parse JSON and serialize it again before verification; whitespace and field ordering can change the signed bytes.

For the Codename One backend, the guide includes a [complete Java receiver](https://github.com/codenameone/CodenameOne/blob/master/docs/demos/backend/src/main/java/com/codenameone/developerguide/backend/PushFeedback.java). It uses the raw `HttpServer.Request`, `Crypto.hmacSha256`, and `Crypto.equalsConstantTime`, so it also fits native backend packaging. Its device store is an application-supplied interface, not an automatically provisioned database.

After verification, the cleanup decision is small:

```java
// Excerpt inside the receiver's verified-event loop.
String deliveryId = (String) event.get("deliveryId");
if (!store.alreadyApplied(deliveryId)
        && "INVALID_TARGET".equals(event.get("reason"))) {
    String key = (String) event.get("token");
    if (key == null) {
        key = (String) event.get("endpoint");
    }
    store.removeKeyAndMarkApplied(deliveryId, key);
}
```

Here `event` is the parsed event map, and `store` is your durable implementation of the example's `DeviceStore`. The deletion and the applied-event marker must commit in the same database transaction. Make `deliveryId` unique in that store so concurrent retries can't apply the event twice.

If a device has since registered a replacement key, remove the rejected key associated with the event. Don't translate an old failure into “delete whatever key this user has now.”

## Acknowledge after the database commits

Delivery is at-least-once. A lost HTTP response can cause a digest to arrive again even after you processed it.

Only a 2xx response advances the sender's watermark. Return it after your writes are durable. If you return success first and the database write then fails, the acknowledged window won't be resent. If the database commits and the response gets lost, the durable `deliveryId` marker lets the retry do nothing safely.

A digest has a size cap. `truncated: true` means another page follows in the same run, so the receiver must handle several requests close together. `eventsOmitted` counts accepted deliveries included only in the summary. It doesn't mean a set of unreported failure events has been lost.

A failing receiver is retried and eventually disabled after repeated failures. Check the reason on the Settings card and exercise the test event after fixing the endpoint. A receiver that has quietly stopped accepting feedback won't keep your database clean.

## Use the backend you already run

The [push guide](/developer-guide/push-notifications/) includes CN1 backend, Spring, Node.js, and PHP receiver examples. The signature and acknowledgment contract is the same for each. You don't have to adopt the CN1 backend to receive BuildCloud feedback.

[PR #5890](https://github.com/codenameone/CodenameOne/pull/5890) adds those examples and documents the contract. Start with the test event, then send the same valid digest to your test receiver twice. Confirm that both requests get an appropriate response and only one durable cleanup operation occurs. Also test a failed database write: that request must not acknowledge a window it failed to apply.

---

## Discussion

_Does your push database distinguish a rejected device key from a delivery attempt that simply ran out of retries?_

{{< giscus >}}
