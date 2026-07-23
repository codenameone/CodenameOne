# Evidence map

Source: `docs/website/content/blog/printing-and-apple-wallet.md`
Canonical: https://www.codenameone.com/blog/printing-and-apple-wallet/

## Thesis

Portable printing and build-generated Apple Wallet extensions without native application code

## Supported beats

- **Printing: one call, five platforms:** PR #5217 adds com.codename1.printing, a deliberately small API: hand a document to the platform's printing system, typically through the native print dialog, and hear back once about how it went.
- **Apple Wallet: provision cards from inside the Wallet app:** PR #5227 is for card issuers: banks and fintechs whose users want to add their cards to Apple Wallet. Apple supports this from inside the Wallet app itself (the "From apps on your iPhone" section), but it requires shipping an issuer-provisioning app extension, a separate binary with a strict contract.
- **How it works under the hood:** Wallet extensions run in a separate process under a 100-millisecond response deadline, so they cannot spin up your Java code. The design splits the work: your app publishes its card data ahead of time through the new com.codename1.payment.WalletExtension API, and the extension reads it from the shared App Group when Wallet asks.
- **Bring your own extension:** Some issuers receive a prebuilt extension from an SDK vendor. The generic iOS app-extension mechanism (ios/app_extensions// in your project) covers that path, and this release improves it: it now works without CocoaPods, and each extension can carry its own provisioning profile, either as a file in its folder or through a per-extension hint.
- **Before you start:** Apple gates issuer provisioning behind the restricted payment-pass-provisioning entitlement, which your developer account must be granted; the new "Apple Wallet Extension" chapter in the developer guide walks through the prerequisites, the endpoint contracts, and both integration modes.

## Referenced evidence

- https://github.com/codenameone/CodenameOne/pull/5217
- https://github.com/codenameone/CodenameOne/pull/5227
- https://api.mybank.com/wallet/provision
- https://github.com/codenameone/CodenameOne/issues
