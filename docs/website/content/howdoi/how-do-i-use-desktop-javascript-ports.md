---
title: USE THE DESKTOP AND JAVASCRIPT PORTS
slug: how-do-i-use-desktop-javascript-ports
url: /how-do-i/how-do-i-use-desktop-javascript-ports/
type: howdoi
original_url: https://www.codenameone.com/how_di_i/how-do-i-use-desktop-javascript-ports.html
tags:
- pro
description: Build apps that run in PC's and Macs
youtube_id: hCjmHoktlrU
thumbnail: https://www.codenameone.com/wp-content/uploads/2020/09/hqdefault-9-1.jpg
---
{{< youtube "hCjmHoktlrU" >}}

The desktop and JavaScript ports answer different questions, even though both let you run a Codename One application outside a phone. The desktop port is about packaging the application as a desktop-style app. The JavaScript port is about running the application in the browser as a Codename One app experience rendered on the web platform.

The desktop port is usually the easier one to reason about conceptually. You are still running a Codename One application, but the result is packaged for desktop operating systems. The app still feels like a Codename One UI, often closer to a tablet-style experience than to a native desktop app built around each platform's widget toolkit. That is fine for some use cases and awkward for others, so it should be chosen intentionally.

The JavaScript port is different. It does not turn your app into a normal hand-authored website. It translates the app into a browser-executed Codename One application using the web platform as the runtime. That means the result behaves more like an app rendered in the browser than like a typical server-rendered or frontend-framework website.

That distinction matters because browser rules apply. Same-origin restrictions, browser capabilities, and web-platform limitations still shape what the JavaScript port can do. The video explains this through the older proxy-servlet workaround, and the underlying point remains valid: browser deployment has a different networking and integration model than native or desktop targets.

Desktop and JavaScript also differ sharply in distribution. Desktop builds are distributed like installers or packaged desktop applications. JavaScript builds are deployed through a web server. Those are not interchangeable decisions; they affect installation, updates, integration, and how users experience the app.

Native extension story is another major difference. Desktop can lean on JavaSE APIs directly. JavaScript can reach into browser-side JavaScript functionality, but it remains constrained by the browser sandbox. If a project depends heavily on platform capabilities outside the browser model, that often influences the choice immediately.

The best way to choose between these ports is not to ask which one is more powerful in the abstract. Ask what environment you actually want the app to live in, what distribution model you need, and what platform limitations your feature set can tolerate.

## Further Reading

- [Developer Guide](/developer-guide/)
- [Introduction For Android Developers](/introduction-for-android-developers/)
- [Hello World](/hello-world/)

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
