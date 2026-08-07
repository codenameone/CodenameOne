---
title: "Your backend should not trust a client-side security check"
slug: 2026-08-07-1200-shai-app-shield-server-attestation
platform: linkedin
account: shai
source_slug: app-shield-server-attestation
publish_at: '2026-08-07T12:00:00'
timezone: Asia/Jerusalem
image: /blog/app-shield-server-attestation.jpg
---

Every root check, jailbreak detector, and pinning branch inside a mobile app has the same weakness: the attacker controls the device where it runs.

A modified app can patch a local boolean. It cannot mint a token signed by a key your server trusts.

That is the idea behind Codename One App Shield. The app obtains a one-time challenge, asks Apple App Attest or Google Play Integrity for a hardware-backed statement, and exchanges it for a short-lived token. Your backend verifies the token before it moves money or returns personal data.

The app-side setup is one build hint and a list of protected hosts. `ConnectionRequest` adds the token and checks the current SPKI pins automatically.

This does not make an app unhackable. It moves the final decision off the device and raises the cost of calling a protected API from a modified client.

This week's post also covers OpenType font support, replacement Windows desktop builders, phase two of our R2 migration, and Saturday's push hostname switch.

{{canonical}}
