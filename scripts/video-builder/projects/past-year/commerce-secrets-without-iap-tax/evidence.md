# Evidence map

Source: `docs/website/content/blog/commerce-secrets-without-iap-tax.md`
Canonical: https://www.codenameone.com/blog/commerce-secrets-without-iap-tax/

## Thesis

Portable entitlements and build-time secrets without a framework revenue tax

## Supported beats

- **Entitlements Instead Of SKU Branches:** refresh() validates the current receipts with the cloud when the build has a build_key and commerce is enabled. In a local build or simulator, it safely falls back to the normal Purchase path.
- **What Happens When Quota Runs Out:** CommerceManager.isDegraded() tells you the cloud did not return a server-validated answer. In that state, entitlement checks fall back to the platform's own receipt signal, treating the entitlement id as a subscription SKU when no cached cloud answer exists.
- **What Commerce Adds:** That split explains how Commerce complements IAP instead of replacing it. Purchase starts the transaction. Commerce answers the longer-term entitlement question.
- **Secrets:** The same PR adds com.codename1.security.Secrets, which solves a different but related problem: API keys do not belong in source code or in the app binary.
- **The Boundary:** Commerce and Secrets are both cloud features, but they sit on different sides of the volume line. Secrets usage stays low enough to enable it for everyone. Commerce has tiers because validation and analytics can create real backend load.

## Referenced evidence

- https://github.com/codenameone/CodenameOne/pull/5300

## Independent problem evidence

- Apple In-App Purchase: https://developer.apple.com/in-app-purchase/ — Apple models renewals, billing retry, grace periods, refunds, and server notifications as an ongoing lifecycle.
- OWASP Mobile Security: https://mas.owasp.org/ — OWASP treats hardcoded credentials and secrets in mobile applications as recoverable by an attacker controlling the client.

## Product proof

- `docs/website/static/blog/commerce-secrets-without-iap-tax/commerce.png`
- `docs/website/static/blog/commerce-secrets-without-iap-tax/secrets.png`
