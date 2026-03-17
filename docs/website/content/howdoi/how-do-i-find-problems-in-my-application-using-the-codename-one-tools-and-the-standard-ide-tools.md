---
title: FIND PROBLEMS IN MY APPLICATION, USING THE CODENAME ONE TOOLS AND THE STANDARD
  IDE TOOLS
slug: how-do-i-find-problems-in-my-application-using-the-codename-one-tools-and-the-standard-ide-tools
url: /how-do-i/how-do-i-find-problems-in-my-application-using-the-codename-one-tools-and-the-standard-ide-tools/
type: howdoi
original_url: https://www.codenameone.com/how_di_i/how-do-i-find-problems-in-my-application-using-the-codename-one-tools-and-the-standard-ide-tools.html
tags:
- basic
- debugging
description: Review of the simulator tools for network monitoring, performance monitoring
  etc.
youtube_id: 1wHGnmO-vtE
thumbnail: https://www.codenameone.com/wp-content/uploads/2020/09/hqdefault-25.jpg
---
{{< youtube "1wHGnmO-vtE" >}}

When a Codename One application feels wrong, the first job is to identify what kind of problem you actually have. Is the UI frozen? Is the event dispatch thread blocked? Is a network request slow? Is a component repainting too often? Is memory pressure coming from images? Codename One gives you tools for each of those questions, but they work best when you use them alongside the normal debugger in your IDE.

The standard Java debugger is still the first tool to reach for. If the simulator is stuck, pause the process and inspect the stack. If a specific interaction misbehaves, put a breakpoint on the action path that matters. Many bugs are still just ordinary logic bugs, and the debugger remains the fastest way to see what the code is actually doing.

Codename One adds another layer of diagnostics inside the simulator. The EDT tools are especially important because so much UI behavior depends on the event dispatch thread. If you do heavy work on the EDT, the app becomes slow or unresponsive. If you touch UI state from the wrong thread, the behavior becomes unpredictable. The EDT diagnostics can help expose both kinds of mistakes, even though they are not perfect and can occasionally produce noisy warnings.

The network monitor is the next high-value tool. If your app talks to a server, inspect the requests and responses directly instead of guessing. Look at URLs, headers, payload sizes, response bodies, and timing. That is often enough to separate a client-side bug from a server-side bug or a simple latency problem.

The performance monitor becomes useful once the issue is more visual than logical. It helps show which components render frequently and which ones are expensive. That is a much better starting point than trying to optimize random code paths. If one screen feels slow, find the component or rendering pattern that is consuming the time before you start rewriting UI code.

Memory problems also become much easier once you inspect images explicitly. In Codename One, images are often the first place to investigate when memory usage grows unexpectedly. The tooling can help show which images are being created and how large they are in memory. That matters because the runtime memory cost of an image is often very different from the compressed file size you started with.

The practical lesson is to debug with evidence. Use the debugger for logic, the EDT tools for threading mistakes, the network monitor for IO, the performance monitor for rendering cost, and the logging APIs for information that needs to survive beyond the simulator.

## Further Reading

- [Developer Guide](/developer-guide/)
- [Performance Network Monitors](/performance-network-monitors/)
- [How Do I Improve Application Performance Or Track Down Performance Issues](/how-do-i/how-do-i-improve-application-performance-or-track-down-performance-issues/)
- [How Do I Use Crash Protection? Get Device Logs?](/how-do-i/how-do-i-use-crash-protection-get-device-logs/)

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
