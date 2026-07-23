# Evidence map

Source: `docs/website/content/blog/device-integrity-and-app-review.md`
Canonical: https://www.codenameone.com/blog/device-integrity-and-app-review/

## Thesis

Separating portable integrity signals from a respectful store-review flow

## Supported beats

- **Device integrity:** com.codename1.security.DeviceIntegrity is a portable runtime self-protection (RASP) and attestation API. It exists for the apps that actually need it: banking, payments, anything where you have to detect a hostile runtime and react to it.
- **App review:** com.codename1.appreview solves a smaller problem: asking for a rating at the right moment, through the right channel. It uses the platform's native store-review prompt when one exists, falls back to a built-in Codename One widget where it does not, and leaves the decision of when to ask entirely up to you.

## Independent problem evidence

- Google Play Integrity: https://developer.android.com/google/play/integrity — Google's flow binds an integrity token to a request and expects server-side decoding and policy decisions.
- Apple App Attest: https://developer.apple.com/documentation/devicecheck/establishing-your-app-s-integrity — Apple's App Attest service lets a server validate that requests came from a genuine app instance on a genuine Apple device.

## Product proof

- `docs/website/static/blog/device-integrity-and-app-review/app-review-sheet.png`
- `docs/website/static/blog/device-integrity-and-app-review.jpg`
