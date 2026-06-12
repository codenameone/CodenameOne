---
title: "Print Anywhere, And Put Your Cards In Apple Wallet"
slug: printing-and-apple-wallet
url: /blog/printing-and-apple-wallet/
date: '2026-06-16'
author: Shai Almog
description: A new cross-platform printing API hands PDFs and images to the native print dialog on every port, and iOS builds can now generate Apple Wallet issuer-provisioning extensions from build hints with no native code.
feed_html: '<img src="https://www.codenameone.com/blog/printing-and-apple-wallet.jpg" alt="Print Anywhere, And Put Your Cards In Apple Wallet" /> A new cross-platform printing API hands PDFs and images to the native print dialog on every port, and iOS builds can now generate Apple Wallet issuer-provisioning extensions from build hints with no native code.'
---

![Print Anywhere, And Put Your Cards In Apple Wallet](/blog/printing-and-apple-wallet.jpg)

The last two items in this week's release are platform integrations that business apps ask for constantly: printing a document, and getting a payment card into Apple Wallet. Both are the kind of feature where the cross-platform story usually ends and a pile of per-platform native code begins. Both are now core APIs.

## Printing: one call, five platforms

[PR #5217](https://github.com/codenameone/CodenameOne/pull/5217) adds `com.codename1.printing`, a deliberately small API: hand a document to the platform's printing system, typically through the native print dialog, and hear back once about how it went.

```java
if (Printer.isPrintingSupported()) {
    Printer.printPDF(reportPath, result -> {
        if (result.isFailed()) {
            ToastBar.showErrorMessage("Print failed: " + result.getError());
        }
    });
}
```

`Printer.printPDF(path, listener)` and `Printer.print(path, mimeType, listener)` print a file from `FileSystemStorage`; `Printer.printImage(image, listener)` takes any Codename One `Image` and handles the encoding for you. The listener is invoked exactly once, on the EDT, with a `PrintResult` that is completed, cancelled, or failed.

What backs the API on each platform:

| Port | Mechanism |
| --- | --- |
| iOS | `UIPrintInteractionController`, presented properly on iPad |
| Android | `PrintManager` print framework: PDFs stream through a print adapter, images go through the platform print helper |
| Simulator / desktop | The Java printing pipeline with the native dialog, with real cancel detection |
| Web | The browser's print flow via a hidden frame |
| Windows native | The Win32 print dialog, with PDFs rendered at printer resolution through the Windows PDF engine |

That last row is worth a beat: the printing API shipped in the same week as [the native Windows port](/blog/native-windows-port-no-jvm/) and already covers it, dialog, spooling, PDF rasterization and all.

One honest caveat that the JavaDoc spells out per method: "completed" means the document was handed to the printing system. Some platforms don't expose what happened inside the native dialog afterwards, so completion there is reported best-effort. Design your UX around handing off, not around confirming pages hit paper.

There is a `PrinterSample` in the repository that prints a generated image and a downloaded PDF if you want a starting point.

## Apple Wallet: provision cards from inside the Wallet app

[PR #5227](https://github.com/codenameone/CodenameOne/pull/5227) is for card issuers: banks and fintechs whose users want to add their cards to Apple Wallet. Apple supports this from inside the Wallet app itself (the "From apps on your iPhone" section), but it requires shipping an *issuer-provisioning app extension*, a separate binary with a strict contract. That has historically meant Xcode, Objective-C, and a long appendix of plist incantations.

Now it means build hints:

```properties
ios.wallet.extension=true
ios.wallet.appGroup=group.com.mybank.app
ios.wallet.issuerEndpoint=https://api.mybank.com/wallet/provision
```

With those set, the iOS build generates Apple's extension pair as fixed, framework-maintained Objective-C. Add `ios.wallet.includeUI=true` and `ios.wallet.authEndpoint=...` and you also get the optional in-Wallet login screen.

### How it works under the hood

Wallet extensions run in a separate process under a 100 millisecond response deadline, so they cannot spin up your Java code. The design splits the work: your app publishes its card data ahead of time through the new `com.codename1.payment.WalletExtension` API, and the extension reads it from the shared App Group when Wallet asks.

```java
if (WalletExtension.isSupported()) {
    WalletExtension.setPassEntries(new WalletPassEntry[] {
        new WalletPassEntry()
            .identifier("card-1234")
            .title("Platinum Card")
            .cardholderName("Jane Doe")
            .primaryAccountSuffix("1234")
            .paymentNetwork("visa")
            .artPng(cardArt)
    });
    WalletExtension.setRequiresAuthentication(true);
    WalletExtension.setAuthToken(token);
}
```

The one step that genuinely needs your backend, producing the encrypted pass payload from Apple's certificates and nonce, is a JSON POST to the issuer endpoint you host. And for teams with special requirements, ten injection hints let you insert custom Objective-C at marked points in every extension callback without forking the generated code.

On the token in that snippet: treat it like any credential. Fetch it from your backend at runtime, hand it to `WalletExtension` for the extension to use, and never hard-code it or check it into source. Anything baked into a shipped binary is effectively public.

### Bring your own extension

Some issuers receive a prebuilt extension from an SDK vendor. The generic iOS app-extension mechanism (`ios/app_extensions/<Name>/` in your project) covers that path, and this release improves it: it now works without CocoaPods, and each extension can carry its own provisioning profile, either as a file in its folder or through a per-extension hint. A bug that could duplicate extension targets in the generated Xcode project when build phases re-ran is also fixed.

### Before you start

Apple gates issuer provisioning behind the restricted `payment-pass-provisioning` entitlement, which your developer account must be granted; the new "Apple Wallet Extension" chapter in the developer guide walks through the prerequisites, the endpoint contracts, and both integration modes.

That wraps the week. Yesterday's post covered [the native Windows port](/blog/native-windows-port-no-jvm/), and the [release post](/blog/native-java-win32-3d-gaming-printing-and-wallet/) has the full index: the portable 3D API, the game development API, native Windows, and these two. As always, the [issue tracker](https://github.com/codenameone/CodenameOne/issues) is the best place to reach us.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
