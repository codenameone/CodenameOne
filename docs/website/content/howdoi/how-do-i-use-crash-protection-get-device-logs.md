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

#### Transcript

In this video I’ll discuss the crash protection pro feature. Crash protection makes debugging application issues on devices in production possible. It does that by providing you with crash logs and information.  
But before we begin we need to clarify that crash protection is a pro level feature which means it is only available to pro subscribers or higher due to the heavy usage of production email servers.

As I said before crash protection automatically emails your account when there is an error in production.  
It also includes a logging system which is available for all users, this logging system allows you to log exceptions with their full call stack.  
You can also trigger the emailing of the log manually. A good example for that would be if your application logic determined an error condition you could send a crash log to your account.

Crash protection is based around the Log class which prints out information that you can then follow to understand your production failures. This is crucial with a tool like Codename One where the breadth of supported devices is so huge you can’t possibly anticipate every eventuality.  
Log.p() prints out log information, you can print any arbitrary string and should generally use this instead of System.out.println(). Log.e() prints out an exception and its stack trace. Notice that printStackTrace() will not work on some platforms and will not provide the desired effect.  
To send the log manually we can just call sendLog() it will instantly send the log to the email of the user who built the app.

Typical applications have a bindCrashProtection call in their init(Object) callback method. This call handles all uncaught exceptions and automatically sends an email if an exception was thrown in runtime and wasn’t handled. Notice the argument for the bind method is set to true. This argument means that exception error messages are swallowed. Normally if the event dispatch thread has an error we catch that exception and show an error dialog. That’s great during development but might be worse than crashing in production… When you pass true it means this error message is consumed and the user won’t see it.  
Notice that bindCrashProtection doesn’t do anything on the simulator to avoid “noise” when you are trying to debug an app.  
Bind crash protection tries to catch all exceptions but it focuses mostly on the event dispatch thread exceptions. You can also handle those manually

Event dispatch thread exceptions can be caught by using the EDT error listener from the CN class or from Display. If you consume the event object the error message won’t reach the EDT and an error dialog won’t be shown to the user. Notice that the exception is logged near the end with the Log.e() method and sent manually. You can create your own custom EDT error handler although we’d recommend the bind method which also tracks uncaught exceptions on other threads.

Thanks for watching, I hope you found this helpful

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
