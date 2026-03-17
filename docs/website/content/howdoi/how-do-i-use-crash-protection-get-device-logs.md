---
title: USE CRASH PROTECTION? GET DEVICE LOGS?
slug: how-do-i-use-crash-protection-get-device-logs
url: /how-do-i/how-do-i-use-crash-protection-get-device-logs/
type: howdoi
original_url: https://www.codenameone.com/how_di_i/how-do-i-use-crash-protection-get-device-logs.html
tags:
- pro
description: Track down issues that occur on the device or in production using these
  tools
youtube_id: C3PLjAWQ-XA
thumbnail: https://www.codenameone.com/wp-content/uploads/2020/09/hqdefault-13-1.jpg
---
{{< youtube "C3PLjAWQ-XA" >}}

Crash protection is about getting useful failure information out of production devices instead of trying to reproduce every rare crash locally. In a framework that targets a wide range of devices and operating systems, that is not optional for serious apps. Some failures only show up in the field.

The underlying tool is the Codename One `Log` API. Use `Log.p()` for ordinary diagnostic messages and `Log.e()` for exceptions. That matters because ordinary Java console habits such as `System.out.println()` and `printStackTrace()` are not the right production tools across all Codename One targets. If you want logs that are portable and useful, go through the framework logging APIs.

Crash protection builds on top of that logging infrastructure. The usual pattern is to bind crash protection early in application startup so uncaught runtime exceptions are captured automatically. That gives you a fallback path even when the user cannot explain what happened or when the device is nowhere near your development environment.

Manual log sending is useful too. Not every failure is an uncaught crash. Sometimes the app reaches a bad state, detects a server-side inconsistency, or encounters a condition you know should be reported even though the application continues running. In those cases, sending the log explicitly can be more valuable than waiting for an unhandled exception.

One detail the video explains well is the tradeoff around swallowing user-visible error dialogs. During development, an obvious error popup can be useful. In production, that same behavior can create a poor user experience. Whether you suppress the default dialog or not should be an intentional product decision, not an accident.

EDT error handling is part of this story as well. A lot of visible Codename One failures show up on the event dispatch thread. If you need custom behavior, you can listen for EDT errors yourself, log them, and decide how much of the default user-facing handling should still happen. The built-in crash protection binding is still the best default for most applications because it covers the common cases with less custom code.

The key point is that crash reporting works best when it is part of a broader logging discipline. If the log already contains meaningful context leading up to the failure, the crash report becomes much more useful. If the only thing in the log is the final exception, you will still know that the app failed, but you may not know why.

## Further Reading

- [Developer Guide](/developer-guide/)
- [How Do I Find Problems In My Application, Using The Codename One Tools And The Standard IDE Tools](/how-do-i/how-do-i-find-problems-in-my-application-using-the-codename-one-tools-and-the-standard-ide-tools/)
- [How Do I Debug On An Android Device](/how-do-i/how-do-i-debug-on-an-android-device/)

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
