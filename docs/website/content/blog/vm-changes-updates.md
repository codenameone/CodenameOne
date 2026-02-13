---
title: VM Changes and Updates
slug: vm-changes-updates
url: /blog/vm-changes-updates/
original_url: https://www.codenameone.com/blog/vm-changes-updates.html
aliases:
- /blog/vm-changes-updates.html
date: '2017-08-23'
author: Shai Almog
---

![Header Image](/blog/vm-changes-updates/new-features-3.jpg)

The summer is finally coming to an end but we might not get to full speed before mid September. Thankfully this didn’t impact Steve who did some pretty great things this August including a lot of GUI builder fixes/enhancements that we’ll cover with the next plugin update. But what we will have this weekend is pretty spectacular. As a short PSA: we’re pushing out the weekend release right now because I’ll be traveling tomorrow. So all this should be available today.

### VM Overhaul

When we started ParparVM our goal was to create a stable, portable & simple VM. We didn’t aim for the performance crown in any way as stability, simplicity & size were far more important in our experience. As the VM stabilized we slowly expanded our ambition and improved performance.  
For many use cases our VM now performs as fast as C but there were still a few things we could improve and Steve made major strides on these.

#### Signal Handling

VM’s usually use low level signaling to detect things like null pointer exceptions which can be detected with zero cost to the runtime environment. Up until now we didn’t do that as we focused on performance and this had some implications. Starting with this update we will convert signals to exceptions which means a null pointer exception should be automatic.

This also means you might get a null pointer exception or a runtime exception from a native OS failure and it can also mean that crash protection will become more effective as OS level crashes would translate to standard exceptions.

That’s a HUGE change in the way the VM works, it has one drawback: this doesn’t happen in the xcode debugger as the debug environment overrides the signals. So if you run into a null in the debugger you will get a crash and breakpoint instead of the exception. This isn’t a “bad thing” but the behavior difference is something you should be aware of. We hope this will improve the reliability & stability of Codename One and bring it to the next level. We also hope this will improve performance by eliminating null checks from our code.

#### New Allocator

If you did any serious C/C++ coding you would know that malloc is pretty problematic. One of the advantages of Java SE is that allocation is faster in Java than it is in C. We didn’t create an allocator as fast as the one in the JDK but Steve integrated a well known malloc replacement that should make heap allocation much faster.

Since Java is based mostly on heap this could have a huge impact on performance both in reducing GC overhead and in allocations themselves. This is off by default right now as it’s still undergoing testing but we hope to flip it to the default next week.

You can try it right now by setting the build hint `ios.rpmalloc=true`.

Notice that if you set this flag to true (which will be the default soon) the iOS deployment target will be set to 8.0+ so if you use the build hint `ios.deployment_target` with a lower value or if you need support for older devices/OS’s you will need to explictly set this to false!

#### Faster UTF-8

This might seem like a smaller feature but string decoding is probably one of the biggest bottlenecks in app logic.

In fact it was such a big bottleneck that we optimized UTF-8 decoding with a special case that detected ASCII code and used a special case fast decode. This worked great for ASCII but not as great for localized text. If you had an app that did a lot of character decoding from a source that wasn’t English  
In fact it was such a big bottleneck that we optimized UTF-8 decoding with a special case that detected ASCII code and used a special case fast decode. This worked great for ASCII but not as great for localized text. If you had an app that did a lot of character decoding from a source that wasn’t of the lower 7 bit ASCII table you paid a penalty in terms of performance as parsing was delegated to Objective-C code.

Steve wrote a fast C based UTF-8 decoding function that should remove that significant bottleneck that might impact a lot of apps.

### Upcoming Changes

There are quite a few other updates I’d like to share including some great pull requests from Diamond and some other things we’ve been working on but this post is getting long enough.

The next two-three weeks will probably be pretty busy but after that I hope to push a plugin refresh and start working on getting 3.8 out of the door. With 3.8 we will be focusing on refinement and stability and don’t plan major new features at this time.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
