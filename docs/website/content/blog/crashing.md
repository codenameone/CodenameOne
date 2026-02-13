---
title: Crashing
slug: crashing
url: /blog/crashing/
original_url: https://www.codenameone.com/blog/crashing.html
aliases:
- /blog/crashing.html
date: '2013-07-16'
author: Shai Almog
---

![Header Image](/blog/crashing/crashing-1.png)

  
  
  
  
![Picture](/blog/crashing/crashing-1.png)  
  
  
  

**  
Note:  
**  
we decided to go in a different direction than the content of this blog post. We will make a new post with further details when we complete the new feature. Generally we found a way to allow deobfuscation of release binaries as well so you would be able to get a crash report on standard applications.  
  
  
  
  
  
  
In an ideal world your app would work perfectly on the device just as it does on the simulator, however that ideal world never exists under any development environment. Performance is different and unfortunately you occasionally get crashes on the device that you don’t get on a simulator. The reasons for this are many and varied, usually a crash means we did something wrong, but sometimes it might mean an exception was thrown and never caught or an API was misused. 

iOS allows you to extract crash logs from the device using itunes or xcode, this allows your beta testers to send you a log that allows you to gleam some information related to the cause of the crash. If you use Log.p() all your entries will appear in the device log (System.out will not appear). The log will also contain stacks indicating the thread stacks including the reason for the crash, this isn’t always helpful but sometimes can provide important clues as to what went wrong. Unfortunately for performance reasons XCode strips out the compiled binary and the stacks just contain memory addresses that provide very little indication applicable to your app.

We now have a feature for pro users allowing them to build an unstripped binary, we are limiting it to pro users since the size of the binary is double and the so is the time it takes to produce such a binary (its a server hog). This can be achieved using the ios.no_strip=true build argument which should allow logs to include symbol information in them by default.  

* * *

Notice: This post was automatically converted using a script from an older blogging system. Some elements might not have come out as intended…. If that is the case please let us know via the comments section below.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
