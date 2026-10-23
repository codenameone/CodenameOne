---
title: "Parley: A Private Messenger Template You Can Brand"
slug: launch-your-own-private-messenger
url: /blog/launch-your-own-private-messenger/
date: '2026-10-25'
author: Shai Almog
description: "Parley is a full-stack, end-to-end encrypted messenger in Java: chats, groups, channels, voice notes, voice and video calls, linked devices and an admin console. Generate it from the Initializr and ship it under your own brand."
feed_html: "<img src=\"https://www.codenameone.com/blog/launch-your-own-private-messenger.jpg\" alt=\"Parley, an end-to-end encrypted messenger built with Codename One\" /> Parley is a full-stack, end-to-end encrypted messenger in Java with groups, voice and video calls, linked devices and an admin console."
series: ["release-2026-10-23"]
---

![Parley, an end-to-end encrypted messenger built with Codename One](/blog/launch-your-own-private-messenger.jpg)

Plenty of organizations want their own messenger and can't use WhatsApp for it. A hospital that needs staff chat it controls. A company that wants messaging inside its product, under its brand, with no third party reading along. A community that doesn't want to live on someone else's platform. Building a secure messenger from scratch is a long project: the encryption protocol, multi-device sync, push, calls, media, groups and moderation are each a project of their own.

**Parley** is that messenger, finished. It's the second of this week's templates, after [Skirmish](/blog/build-a-multiplayer-strategy-game-in-java/), and like the others it comes as one Java project with the app, the server and the admin console. Pick the **messaging** template in the [Initializr](/initializr/) and you have it. The source is under [MIT-0](https://github.com/codenameone/CodenameOne/blob/master/scripts/parley/LICENSE): brand it, sell it and keep your changes private.

{{< guide-block >}}
<div class="release-screenshots" style="display:grid;grid-template-columns:repeat(auto-fit,minmax(min(100%,150px),1fr));gap:16px;align-items:start;margin:24px 0">
<figure style="margin:0"><a href="/developer-guide/img/parley-chats.png"><img width="419" height="909" src="/developer-guide/img/parley-chats.png" alt="The Parley chat list" loading="lazy" style="width:100%;height:auto" /></a><figcaption>Chats, groups and channels.</figcaption></figure>
<figure style="margin:0"><a href="/developer-guide/img/parley-direct-chat-received.png"><img width="419" height="909" src="/developer-guide/img/parley-direct-chat-received.png" alt="A one-to-one Parley conversation" loading="lazy" style="width:100%;height:auto" /></a><figcaption>An encrypted conversation.</figcaption></figure>
<figure style="margin:0"><a href="/developer-guide/img/parley-call-connected.png"><img width="419" height="909" src="/developer-guide/img/parley-call-connected.png" alt="A connected Parley video call" loading="lazy" style="width:100%;height:auto" /></a><figcaption>Voice and video calls.</figcaption></figure>
</div>
{{< /guide-block >}}

## What you get

| Part | What's in it |
| --- | --- |
| Messaging | One-to-one chats, groups of up to 256 with invite links, broadcast channels, replies, forwards, edits and delete for everyone |
| Media | Photos, video and voice notes, encrypted on the device before upload |
| Calls | Voice and video calls over WebRTC, peer to peer |
| Privacy | Disappearing messages, safety numbers, app lock, screen security, and controls for last seen, profile photo, read receipts and typing |
| Devices | Linked devices, including a browser as another device |
| Bridges | Optional Telegram and WhatsApp bridges, clearly marked in every conversation that uses them |
| Operations | An admin console with a dashboard, users, reports and bridges |
| Reach | iOS, Android, desktop and the browser from one project, in five languages including right-to-left Hebrew |

## The server can't read your users' messages

Parley uses the same design as Signal: X3DH to agree on keys and the Double Ratchet to rotate them with every message. It uses P-256 ECDH, which every platform's native crypto supports, with HKDF-SHA-256 and AES-256-GCM. Each message has its own key, and a key that leaks doesn't expose earlier messages.

The server stores and forwards envelopes: sender, target device, header, and ciphertext. It never sees the payload, so a breach of your server is a breach of ciphertext. Attachments are encrypted on the device with a fresh key before upload. On the phone, the history is sealed with a key held in the platform keystore.

{{< mermaid >}}
sequenceDiagram
    participant A as Alice's phone
    participant S as Parley server
    participant B as Bob's phone
    B->>S: Publish identity and prekeys
    A->>S: Fetch Bob's prekeys
    Note over A: X3DH: four ECDH results,<br/>HKDF into a root key
    A->>S: Envelope (ciphertext only)
    S->>B: Envelope (ciphertext only)
    Note over B: Same X3DH, same root key,<br/>Double Ratchet from here on
{{< /mermaid >}}

The code that does this is short and readable, because the primitives are part of Codename One. This is the start of a session, from `Ratchet.java`:

```java {class="cn1-no-try"}
KeyPair ephemeral = KeyGenerator.ec(256);
PrivateKey me = Identity.privateKey();
byte[] dh1 = KeyAgreement.ecdh(me, pub(signedKey));
byte[] dh2 = KeyAgreement.ecdh(ephemeral.getPrivateKey(), pub(identity));
byte[] dh3 = KeyAgreement.ecdh(ephemeral.getPrivateKey(), pub(signedKey));
byte[] secret = x3dh(dh1, dh2, dh3,
        oneTime == null ? null : KeyAgreement.ecdh(ephemeral.getPrivateKey(), pub(oneTime)));
```

`KeyAgreement`, `KeyGenerator` and `Hkdf` are new in `com.codename1.security`, added for Parley. They work on every platform, on the backend too, and Thursday's post covers them along with the WebRTC API.

## Calls that never touch your server

Calls use WebRTC with DTLS-SRTP, directly between the two devices. The Parley server rings the other side, relays the offer, answer and ICE candidates over the live WebSocket channel and hands out STUN and TURN credentials. The audio and video never pass through it, so a call costs your server almost nothing and stays private.

## Make it yours

The look starts at the top of `theme.css`:

```css
--brand: #0a7d6b;  /* White on the teal reads at 5:1. */
--brand-contrast: #ffffff;
```

Change the brand color and every screen follows, with dark mode variants beside it. The name, icon, languages, push and SMS providers, database and hosting work the same way they do in Wayline. A few directions to take it:

- **An internal company messenger:** run your own TURN relay for calls (`parley.ice.turn`) and leave the bridges out, so nothing leaves your infrastructure.
- **Messaging inside your product:** a marketplace that lets buyers and sellers talk, a clinic that talks to patients, a coaching app that talks to clients. The chat screens are ordinary Codename One forms, so they fit into another app.
- **A community app:** channels for announcements, groups for members and the admin console for moderation.
- **Your own crypto choices:** the curve and the cipher are in one class, `Ratchet`, if your compliance team has opinions.

{{< guide-block >}}
<div class="release-screenshots" style="display:grid;grid-template-columns:repeat(auto-fit,minmax(min(100%,150px),1fr));gap:16px;align-items:start;margin:24px 0">
<figure style="margin:0"><a href="/developer-guide/img/parley-privacy.png"><img width="419" height="909" src="/developer-guide/img/parley-privacy.png" alt="Parley privacy settings" loading="lazy" style="width:100%;height:auto" /></a><figcaption>Privacy settings.</figcaption></figure>
<figure style="margin:0"><a href="/developer-guide/img/parley-settings-dark.png"><img width="419" height="909" src="/developer-guide/img/parley-settings-dark.png" alt="Parley settings in dark mode" loading="lazy" style="width:100%;height:auto" /></a><figcaption>Dark mode.</figcaption></figure>
<figure style="margin:0"><a href="/developer-guide/img/parley-admin-dashboard.png"><img width="419" height="909" src="/developer-guide/img/parley-admin-dashboard.png" alt="The Parley admin dashboard" loading="lazy" style="width:100%;height:auto" /></a><figcaption>The admin dashboard.</figcaption></figure>
</div>
{{< /guide-block >}}

## Moderation without reading messages

An encrypted messenger still needs moderation. The admin console only sees what the server sees: accounts, message counts, conversation types and busy hours. When a user reports a conversation, their phone attaches the messages they chose to report, so content reaches an admin only through the people in that conversation. That keeps the privacy promise while still letting you handle abuse.

## Try it

The development profile runs with nothing installed. Start the server, open two simulators, sign in as `alice@parley.example` and `bob@parley.example`, and message yourself. The server can also host the browser build, so a laptop browser becomes a linked device. The [Parley page](/parley/) has the overview, and the [user's guide](/developer-guide/parley-users-guide/) and [implementor's guide](/developer-guide/parley-implementors-guide/) cover the rest.

Tomorrow: {{< post-link path="/blog/launch-your-own-food-delivery-app" text="Savor: A Food Delivery Template for Restaurants and Local Fleets" >}}.

{{< giscus >}}
