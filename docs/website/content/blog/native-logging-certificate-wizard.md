---
title: Native Logging and Certificate Wizard Downtime
slug: native-logging-certificate-wizard
url: /blog/native-logging-certificate-wizard/
original_url: https://www.codenameone.com/blog/native-logging-certificate-wizard.html
aliases:
- /blog/native-logging-certificate-wizard.html
date: '2018-06-18'
author: Shai Almog
---

![Header Image](/blog/native-logging-certificate-wizard/ios-cert-wizard-blog-post-header.png)

I’ve been so busy with the book I completely missed a lot of things I should have blogged about and one such thing is the [NativeLogsReader cn1lib](https://github.com/jsfan3/CN1Libs-NativeLogsReader) which has been in the extension manager for a while now.

The [NativeLogsReader cn1lib](https://github.com/jsfan3/CN1Libs-NativeLogsReader) was created by [Francesco Galgani](https://github.com/jsfan3) to include native logging into the Codename One log. A lot of times we get on device failures that are really hard to track. In those cases we ask users to connect cables and try to view the native logs to search for clues. With this library you can see native output even without physical access to the device!

That’s really helpful when you’re tracking an issue that happens on an end user device.

### Certificate Wizard Issues

We’ve had several cases of downtime with the certificate wizard lately and we are going through such a downtime right now. We are working on fixing it and hopefully it will be fixed by the time you read this…​

However, I’d like to explain why these things happen. The certificate wizard connects to Apples undocumented system to support generating certificates/provisioning. It’s a system they have in place for xcode but it’s a bit flaky. We could just use something like webscraping in the worst case scenario but either way every time Apple makes a change we need to adapt.

A while back Apple made a change, we adapted relatively quickly but introduced a few regressions which were really hard to pinpoint as they relate to behaviors such as cookie policies etc. Things that work for our localized test cases sometimes fail as we scale them to the entire community…​

Hopefully, this round of whack-a-mole will be over soon.

### Book Update and 5.0

As usual producing the book is taking way longer than planned but I’m getting there. I’m really excited about what we have so far and can’t wait to share it with you guys.

On a related subject we also need to update Codename One to run on newer JDK’s 9/10/11 all of which broke so many **documented** features in the JDK that we depend on. This is crucial as JDK 8 will EoL in 2019.

With those two things in mind we decided to postpone Codename One 5.0 to September instead of its current July target. This will give us time to address these issues and give me a bit of time to do some “actual work” that doesn’t revolve around the book.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Chris** — June 20, 2018 at 4:43 am ([permalink](https://www.codenameone.com/blog/native-logging-certificate-wizard.html#comment-23700))

> Chris says:
>
> NativeLogsReader works for debug builds or only for App Store Builds? Please advise.  
> Do we have to add the following statement in every form as we don’t where the App will Crash or have some issue  
> String logs = NativeLogs.getNativeLogs();
>



### **Francesco Galgani** — June 20, 2018 at 2:13 pm ([permalink](https://www.codenameone.com/blog/native-logging-certificate-wizard.html#comment-24000))

> Francesco Galgani says:
>
> It works for every app (it it’s a debug build or an app store build doesn’t matter).
>
> For example, if all the Forms of your app are subclasses of a custom base Form, you can add a side menu command in your custom base Form to read and/or send by email the native log. In this way, you can add this functionality to all the Forms, coding it only one time.
>



### **Yaakov Gesher** — June 23, 2018 at 10:10 pm ([permalink](https://www.codenameone.com/blog/native-logging-certificate-wizard.html#comment-23947))

> Yaakov Gesher says:
>
> Just add that code in the EDT error handler, and the network event handler, and anywhere you’re working off the EDT and might run into a problem.
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
