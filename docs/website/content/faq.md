---
title: "FAQ"
date: 2020-09-11
slug: "faq"
description: "Quick answers about licensing, pricing, cloud builds, source code, and support."
ShowToc: true
---

## Start Here

Codename One is a cross-platform framework for building native mobile and desktop apps with Java or Kotlin.

> Looking for a fast answer? Start with **Pricing**, **Documentation**, or **Stack Overflow** below.

If you need implementation-specific help, use:

- [Stack Overflow (`codenameone` tag)](https://stackoverflow.com/questions/tagged/codenameone?sort=votes&pageSize=50)
- [GitHub Issues](https://github.com/codenameone/CodenameOne/issues)
- [Community Forum/Help](https://www.reddit.com/r/cn1/)

## Product & Platform

### What is Codename One and how does it work?
Codename One lets Java/Kotlin developers build native apps from one codebase. You write app code once, and Codename One generates platform-specific binaries for iOS, Android, desktop, and web targets.

Learn more in:

- [Introduction](/introduction/)
- [Developing in Codename One](/developing-in-codename-one/)

### Do I need a Mac to build iOS apps?
Not if you use the Codename One cloud build service. The cloud handles iOS compilation on Mac infrastructure.

If you build fully offline, Apple tooling still requires macOS for iOS builds and submission workflows.

### How does performance compare to native or HTML-based solutions?
Codename One compiles to native targets and is designed for production-level performance, including optimized rendering and modern VM/runtime improvements.

For a practical comparison, see [Compare](/compare/).

### Can I use third-party libraries?
Yes, via Codename One libraries (`cn1libs`) and Maven dependencies that fit the Codename One toolchain.

Browse available libraries at [Plugins/CN1Libs](/cn1libs/).

## Licensing & Pricing

### Is Codename One free and open source?
Yes. Codename One is open source and can be used commercially. The optional cloud build/runtime services are available in free and paid tiers.

### What are the limits of the free plan?
Free-plan limits apply mainly to cloud resources:

- Build credits per month (iOS builds consume more credits than non-iOS targets due to Mac build costs)
- Cloud service quotas (e.g., push limits)
- Build artifact/JAR size limits for cloud builds

Paid plans expand these limits and unlock additional production features.

See current details on [Pricing](/pricing/).

### Can I cancel a paid plan and keep my app live?
Yes. Built apps continue to work.  
Cloud-backed runtime services (such as certain push/cloud features) require an active subscription.

### Is pricing per app or per developer?
Pricing is per developer seat, not per app.

### Can teams mix different subscription tiers?
No. For team accounts, developer seats are expected to be on the same subscription level.

## Cloud Build, Source, and Security

### Is my source code sent to the cloud?
No source code is sent for normal cloud builds. Codename One processes compiled bytecode.

Native platform integrations may require native source segments to be uploaded for target-specific compilation.

### Can I build offline?
Yes. You can build using local/offline workflows (including Maven-based workflows), subject to platform toolchain requirements.

### Can I download generated native sources?
Paid tiers provide generated sources for supported targets.

### What still requires paid cloud services in production?
Your installed app binaries continue to run after cancellation, but cloud-backed runtime features (such as certain push/cloud capabilities) require an active subscription.

## Legal

### What license is Codename One released under?
Codename One source is licensed under GPL + Classpath Exception.

This allows commercial app distribution without requiring your app code to be open sourced solely because it links to Codename One.

## Production Functionality & Limitations

### Is this a web wrapper approach?
No. Codename One is not an HTML5 wrapper by default. It compiles to native targets with a cross-platform UI toolkit.

### What Java limitations should I expect?
Codename One is Java/Kotlin-first, but not a full desktop-JVM mirror. Some APIs are mobile-adapted, and reflection isn't supported for portability, size, performance and security.

### Can I still use native APIs when needed?
Yes. Native interfaces and plugins let you access platform-specific APIs where required.

### Can I build and ship multiple production apps?
Yes. There is no per-app licensing cap; plans are tied to developer seats and service usage tiers.

## Support & Community

### Where should I ask questions?
- [Stack Overflow (`codenameone` tag)](https://stackoverflow.com/questions/tagged/codenameone)
- [GitHub Issues](https://github.com/codenameone/CodenameOne/issues)
- [Community Forum/Help](/discussion-forum/)

### Can I contribute?
Yes. Contributions are welcome via pull requests:

- [Codename One on GitHub](https://github.com/codenameone/CodenameOne)

### Do you offer training resources?
Yes:

- [Academy / Training](/training/)
- [How Do I Tutorials](/how-do-i/)
- [Videos](/videos/)

## Quick Links

- [Getting Started](/getting-started/)
- [Documentation](/developing-in-codename-one/)
- [Pricing](/pricing/)
- [Compare](/compare/)
