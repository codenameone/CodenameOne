---
title: "Don't Order Fish on Monday. Don't Release on Friday"
slug: friday-release-better-gates
url: /blog/friday-release-better-gates/
date: '2026-10-08'
author: Shai Almog
description: "A difficult release week exposed an Apple API failure that passed device tests, leading to source and binary checks for undeclared SDK imports."
feed_html: '<img src="https://www.codenameone.com/blog/friday-release-better-gates.jpg" alt="A release gate catches a failure before App Store submission" /> A difficult release week exposed an Apple API failure that passed device tests, leading to source and binary checks for undeclared SDK imports.'
series: ["release-2026-10-02"]
---

![A release gate catches a failure before App Store submission](/blog/friday-release-better-gates.jpg)

Anthony Bourdain's famous warning about Monday fish was a warning about the supply chain behind the menu. In his [1999 essay](https://www.newyorker.com/magazine/1999/04/19/dont-eat-before-reading-this), he described seafood bought for the weekend still being served on Monday. Knowing how the kitchen worked changed what he would order.

Developers have a similar rule: never release on Friday. If something breaks, you have just made plans for everyone's weekend. It is excellent advice that we have ignored for years.

We settled on Friday releases to avoid dropping a framework update into the middle of our customers' release week. The idea was to use the quieter window to test and repair our release before it collided with theirs. That assumes a lot about when other people ship. It has also cost me plenty of weekends.

This time the problems consumed much of the week. There were regressions around the optional Xcode 27 path, push, and windows. Some overlapped, so a fix could make the original symptom disappear while the underlying problem resurfaced elsewhere. That is exhausting for us, and it is worse for the customer trying to finish an app.

## Catch a rejection your device tests can miss

One failure was especially instructive. Apps using particular crypto APIs could compile, link and run, then fail App Store Connect submission because the binary imported non-public symbols. Most apps did not enable the affected code, so most users and our usual tests never encountered it:

```text
_CCCryptorGCMAddAAD
_CCCryptorGCMAddIV
_CCCryptorGCMFinal
```

The SDK's libraries exported these symbols. The linker could resolve them. But they were not declared in the public headers. A successful link did not establish that they were public APIs suitable for submission.

Our tests had another blind spot: the calls sat behind an optional feature gate that the sample app did not normally enable. Testing the same sample more often would not make that branch appear.

The [new checker source](https://github.com/codenameone/CodenameOne/blob/master/scripts/check-ios-private-api.py) preserves the rejection and the causal chain. It is a more useful record than a claim that we “added more tests.”

## How your build could enable a feature you never requested

The builder enabled a feature by replacing a preprocessor definition. A plain string replacement for one name also matched the start of a longer name:

```c
#define CN1_INCLUDE_CRYPTO
#define CN1_INCLUDE_CRYPTO_GCM
```

Enabling general crypto could therefore enable the GCM path too. The customer did not have to request that optional path to compile its imports.

The fix makes the builder match the whole definition name. A focused regression test guards that behavior. The GCM implementation itself was rewritten using public CommonCrypto operations and a C implementation of GHASH, the authentication calculation. The authenticated-decryption path verifies the tag before releasing decrypted output.

A separate sweep found `sqlite3_key` and `sqlite3_rekey` imports that were valid for the bundled cipher engine but not public declarations in Apple's SQLite headers. Those imports now depend on the bundled engine defining them. One rejected upload exposed more than one place where “the symbol links” had stood in for “the symbol is public.”

## How we will catch this class of failure before it reaches you

A regression test for the three names would catch those three names returning. We also need a check for the broader mistake: importing an SDK symbol that has no public declaration.

The new gate asks that question in two ways:

| Check | What it sees | Why it is needed |
| --- | --- | --- |
| Source mode | Native port sources with feature gates enabled and optional-engine variants | The ordinary sample does not exercise every feature combination |
| Binary mode | Imports in a linked Release device app and the covered bundled components | The final link can contain code beyond the port source sweep |

{{< mermaid >}}
flowchart TD
    Source[Native sources and feature variants] --> Compile[Compile the covered configurations]
    Compile --> Imports[Collect SDK imports]
    Binary[Linked Release device app] --> Imports
    SDK[Public SDK headers] --> Compare[Compare exported names with declarations]
    Imports --> Compare
    Compare --> Fail[Fail on undeclared imported symbols]
    Probe[Known private and public control symbols] --> Verify[Verify the checker can distinguish them]
    Verify --> Compare
{{< /mermaid >}}

The checker uses SDK export information to identify the library symbols and scans public headers with comments removed. A name mentioned in a comment is not accepted as a declaration. It also exercises known private and public controls before trusting a clean result. A check that can never report a failure is a very comforting way to learn nothing.

For a local inspection of a linked app, the script exposes a binary mode:

```bash
python3 scripts/check-ios-private-api.py \
  --binary /path/to/Release-iphoneos/MyApp.app --sdk iphoneos
```

Replace the example path with your Release device artifact; use `--developer-dir` if the SDK belongs to a different Xcode installation. The repository wires source mode and Release binary mode into its iOS checks. [PR #5923](https://github.com/codenameone/CodenameOne/pull/5923) contains the implementation and the builder repair.

## Know what your submission check covers

This is an imported-symbol check for the covered C and Objective-C surface. It skips Swift and C++ mangled names that cannot be matched by this header scan, and it does not inspect private Objective-C selectors. The binary check in the repository runs against the covered sample and bundled components, not every arbitrary dependency a customer might add.

It also cannot predict every App Store review decision. Entitlements, privacy declarations and application behavior are different questions. Checking imports catches one specific reason for rejection before you upload.

Review those remaining checks separately when preparing your submission.

## We owe you more reliable releases

This week was hard. I spent too much of it chasing regressions, and some of you spent time proving to us that a fix had not really fixed your problem. That is time you should have been able to spend on your own apps. Our release process needs to do better.

Changing the day we ship would not repair the gaps that let these failures through. Our plan is to strengthen the gates around the things we deliver: exercise optional configurations, check the final artifacts, and test rules that cover a whole class of mistakes. We still need the small regression test for each bug. We also need to ask why that bug could reach a customer while the checks were green.

The private API gate is one concrete step in that plan. It checks feature combinations our regular sample missed, examines a linked Release app, and uses known control symbols to verify that the checker can actually fail. The wider work is ongoing; one new gate does not solve every regression involving Xcode, push, or windows.

I cannot promise next week will be quiet. I can promise that a difficult week should leave us with stronger release checks, not just a longer list of closed bugs. As that coverage grows, fewer failures should depend on a customer finding the right combination of APIs for us.

This closes the [weekly series](/blog/java-server-work-before-startup/). I am excited about the backend and the UI improvements, but you should be able to try them without wondering what else an update will break. That is the release quality we need to earn.

---

## Discussion

_Which green check has given your team the most misleading confidence before a release?_

{{< giscus >}}
