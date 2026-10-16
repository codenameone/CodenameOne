---
title: "Let Users Install Your Java Web App Again"
slug: install-your-java-web-app-again
url: /blog/install-your-java-web-app-again/
date: '2026-10-21'
author: Shai Almog
description: "PWA installation and cached offline launch return to ParparVM JavaScript builds. The generated service worker versions application files while leaving network data to your app."
feed_html: "<img src=\"https://www.codenameone.com/blog/install-your-java-web-app-again.jpg\" alt=\"A Java web app moves from a browser tab to an installed app\" /> PWA installation and cached offline launch return to ParparVM JavaScript builds. The generated service worker versions application files while leaving network data to your app."
series: ["release-2026-10-16"]
---

![A Java web app moves from a browser tab to an installed app](/blog/install-your-java-web-app-again.jpg)

We lost progressive web app support when we moved the JavaScript port from TeaVM to ParparVM. It's back. Users can install your browser app on supporting browsers, and its cached application files can load when the network is unavailable.

For a small business, a link can be the quickest way to put an app in someone's hands. Installation lets that same app remain within reach after the tab closes. This is Wednesday's follow-up to the [Wayline release story](/blog/create-an-uber-clone-in-1-minute/).

## The build makes the installation files

A ParparVM JavaScript application build now produces `manifest.json`, home-screen icons and `sw.js` automatically. There is no separate PWA enablement switch.

Deploy the whole generated bundle, including subdirectories, over HTTPS. Localhost also works for testing. Relative manifest and asset URLs let an application live under a path such as `/dispatch/` rather than requiring a dedicated domain root.

You can customize the app's identity through build hints in `codenameone_settings.properties`:

```properties
codename1.arg.javascript.manifest.name=Station Dispatch
codename1.arg.javascript.manifest.short_name=Dispatch
codename1.arg.javascript.manifest.description=Your station's dispatch app
codename1.arg.javascript.manifest.display=standalone
```

The default manifest already requests a standalone window. `name` and `short_name` default to the application's display name. Installation UI and eligibility depend on the browser, so avoid making your onboarding depend on one particular prompt appearing.

## Offline launch has a precise boundary

The service worker caches generated application files: the runtime, translated JavaScript chunks, themes and bundled resources. It must finish installing while online before those files can support an offline launch.

{{< mermaid >}}
flowchart TD
    Online[First visit while online] --> Install[Install service worker]
    Install --> Cache[Cache generated application files]
    Cache --> Offline[Launch cached app without a network]
    Offline --> Local[Use available local data and features]
    Offline --> API[Network operations still need the server]
{{< /mermaid >}}

It doesn't cache API responses, proxy requests or external resources. Application data and synchronization remain your responsibility.

In Wayline, that means a cached UI is not permission to accept a ride offline. Matching needs the server, and the map's external tiles and routes aren't bundled application assets. A dispatch app should say that it is disconnected rather than show a stale assignment as confirmed.

This distinction is useful outside ride hailing too. A locally stored draft can remain editable; publishing that draft still needs a connection. Design the interaction around what you actually have on the device.

## An update should belong to one version

A running app needs its runtime and resources to agree. The worker creates a new cache when content changes, even if the application's version string stays the same.

The new worker waits until the old app's tabs and windows close before activating. That keeps an open app from mixing an old runtime with new chunks. Each deployment path has its own cache, so two apps on the same host don't share one undifferentiated bucket.

{{< mermaid >}}
sequenceDiagram
    participant Old as Open app
    participant Worker as New worker
    participant Cache as New cache
    Worker->>Cache: Store changed bundle
    Worker->>Worker: Wait while old app remains open
    Old->>Old: User closes old tabs and windows
    Worker->>Worker: Activate new version
    Worker->>Cache: Use matching application files
{{< /mermaid >}}

Deploy the files together and allow the browser to revalidate `sw.js`. Cache versioning helps the client choose a coherent bundle; it doesn't turn a partially uploaded deployment into a complete one.

## Check the behavior your users will get

After deploying a test build, open the browser's Application tools and inspect the manifest, service worker and cache. Wait for installation to finish, then reload offline. Check both the UI that should work and the operations that should report a missing connection.

For an update check, keep one old tab open, deploy a visibly changed build and confirm that the new worker waits. Close the old tabs and reopen the app. Test a subdirectory deployment too if that is how you host it.

The repository includes a [browser smoke test and service-worker contract checks](https://github.com/codenameone/CodenameOne/tree/master/tests/pwa). Those tests exercise caching and update behavior; they don't establish that every browser offers the same install experience.

[PR #6022](https://github.com/codenameone/CodenameOne/pull/6022) restores the capability, and the [JavaScript guide](/developer-guide/working-with-javascript/) explains manifest customization. A web deployment can be a useful first distribution channel for your application, with native apps alongside it when their device integration matters.

## Discussion

_What should your app still let a user do after the connection drops?_

{{< giscus >}}
