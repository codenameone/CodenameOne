# Evidence map

Source: `docs/website/content/blog/compliance-improvements-simulator-updates-and-more.md`
Canonical: https://www.codenameone.com/blog/compliance-improvements-simulator-updates-and-more/

## Thesis

Replacing ProGuard-era compliance checks with explicit build-time verification

## Supported beats

- **Compliance Checks without Proguard:** Codename One only supports a subset of JavaSE’s API. While we’re constantly trying to expand the supported set of APIs, it would always be a partial list.
- **The Problem of String’s split():** Don’t use String.split(). It’s a terrible API. I use it myself occasionally, but the feature is deeply problematic, which is why we didn’t add it.
- **New Java Versions on Android:** This also means we will be able to adopt newer versions of Java past 17. Android stopped updating JVMs after JDK 17, which means newer Java language features aren’t supported.
- **Bug Fixes of Note:** This week we fixed some interesting bugs in the issue tracker.
- **Simulator Location Support:** LocationSimulation was updated to replace the old JavaFX-based mapping with a JCEF-based implementation, along with follow-up fixes for JCEF detection and graceful fallback behavior.
- **Incorporated Text Scale by Default:** This is a small change, but it improves the out-of-the-box experience in a very practical way.
