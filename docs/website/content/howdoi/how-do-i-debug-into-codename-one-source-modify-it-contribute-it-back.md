---
title: DEBUG INTO CODENAME ONE SOURCE, MODIFY IT & CONTRIBUTE IT BACK
slug: how-do-i-debug-into-codename-one-source-modify-it-contribute-it-back
url: /how-do-i/how-do-i-debug-into-codename-one-source-modify-it-contribute-it-back/
type: howdoi
original_url: https://www.codenameone.com/how_di_i/how-do-i-debug-into-codename-one-source-modify-it-contribute-it-back.html
tags:
- advanced
- debugging
description: Use the sources on github, to improve your app, learn & contribute
youtube_id: 2nD75pODPWk
thumbnail: https://www.codenameone.com/wp-content/uploads/2020/09/hqdefault-22.jpg
---
{{< youtube "2nD75pODPWk" >}}
Debugging into Codename One source is one of the most useful ways to understand why a framework-level behavior is happening. It lets you answer questions that are hard to resolve from application code alone: is the issue in your usage, in the framework, in a specific port, or in a recent Codename One change?

The basic workflow is to work against the actual Codename One source tree instead of only depending on prebuilt jars. Once the relevant projects are checked out and wired into your IDE, stepping into a framework class takes you to real source that you can read, debug, modify, and test immediately. That turns Codename One from a black box into an ordinary codebase you can inspect like any other dependency under development.

The old video is IDE-specific and reflects an older project setup, but the important lesson is unchanged: if you want to debug framework behavior seriously, you need the source projects in your workspace and you need your application to resolve against them instead of the packaged binaries.

This is valuable even if you never plan to contribute a patch. Walking into framework code while the debugger is live is often the fastest way to understand why a UI behaves the way it does, why a property is ignored, or why a specific port diverges from another. Reading the code is useful; reading it while stopped at the relevant call site is better.

Once you are set up this way, local experimentation becomes much easier. You can make a framework change, rerun the app, and immediately verify whether the change solves the problem. That is often faster than trying to infer the right fix abstractly. The video demonstrates this by adding a small utility method and pushing it through a fork, but the broader point is that Codename One can be debugged and modified with the same workflows you would use for any normal open source Java project.

If you do decide to contribute a fix, keep the standard open source rules in mind. Make sure you own the code you are submitting, preserve the project's legal and coding conventions, and keep the change focused. Small, clear contributions are easier to review and easier to merge than wide changes that mix refactoring, cleanup, and new behavior all at once.

It is also smart to discuss non-trivial changes before investing heavily in them. If a proposed fix changes behavior or introduces a new API, a short discussion first can save a lot of time and reduce the chance that the patch heads in the wrong direction.

The real educational value here is that the framework source is not off-limits. If something in Codename One is confusing, stepping into it is often the right move. Even if the final outcome is not a pull request, understanding the code path usually improves how you structure your own application and how you diagnose similar issues later.

## Further Reading

- [Developer Guide](/developer-guide/)
- [How Do I Find Problems In My Application Using The Codename One Tools And The Standard IDE Tools?](/how-do-i/how-do-i-find-problems-in-my-application-using-the-codename-one-tools-and-the-standard-ide-tools/)
- [How Do I Debug On An Android Device](/how-do-i/how-do-i-debug-on-an-android-device/)
- [How Do I Use The Include Sources Feature To Debug The Native Code On iOS/Android Etc.](/how-do-i/how-do-i-use-the-include-sources-feature-to-debug-the-native-code-on-iosandroid-etc/)

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
