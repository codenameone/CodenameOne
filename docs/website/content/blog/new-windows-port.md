---
title: New Windows Port
slug: new-windows-port
url: /blog/new-windows-port/
original_url: https://www.codenameone.com/blog/new-windows-port.html
aliases:
- /blog/new-windows-port.html
date: '2016-04-03'
author: Shai Almog
---

![Header Image](/blog/new-windows-port/universal-windows-apps_thumb.jpg)

Our existing Windows Phone port has already gone thru 3 rewrites and a community rewrite and we are hard  
at work on the 4th rewrite (or 5th counting the community port). However, we decided to take a radically different  
direction with the new port and with backwards compatibility to allow better scale.

So lets start with the “bad news” and follow by an explanation of why and how we plan to do that.

### Dropping Windows Phone 8 Support

We will focus on Windows 10 and [Universal Windows platform](https://msdn.microsoft.com/en-us/windows/uwp/get-started/whats-a-uwp).  
This means applications will run seamlessly on  
tablets/desktops & phones within the “new UI” and would be sellable via the Windows Store without any special  
trickery.

However, this new packaging isn’t compatible with the old Windows Phone 8 API’s. Most newer devices are upgradable  
to 10 so this shouldn’t be a huge deal and the benefits far outweigh the pain.

#### What to do If We Still Need Windows Phone 8 Support?

If there is demand for building on top of the old port (e.g. for compatibility with [the work from Fabricio](https://github.com/Pmovil/CN1WindowsPort))  
we will include a special way to get the sources of a Windows Phone project built dynamically. You will need  
a windows machine with visual studio to compile them and work with them.

The reason for this complexity is explained below.

### The New Approach: iKVM

Our existing Windows Phone port is based on a very experimental and partially broken branch of XMLVM. When  
we originally developed this port we tried 4 different tools all of which failed completely and XMLVM was the  
least broken among them.

Back in those days (Windows Phone 7.5) only C# or .net managed languages were supported so using C or other  
languages for the VM wasn’t an option. Windows Phone 8 introduced some capabilities in this regard so we considered  
a port of [ParparVM](/blog/parparvm-spreads-its-wings.html). Up until recently it seemed like the only option.

In the past we briefly reviewed [iKVM](http://www.ikvm.net) which translates Java bytecode to .net assemblies. This  
seemed like the ideal solution but due to the Windows environment restrictions it didn’t support Windows Phone.  
However, with Windows 10 we thought it’s a good time to re-evaluate that and saw some mentions of developers  
who were successful in getting it to work. Thankfully over the weekend Steve made a break-thru and was able to  
get iKVM to work with universal windows applications. While this took a lot of effort this reduced the complexity  
of the overall port by at least 10 man months!

The work is far from done but we are on a solid path and once we have a stable build server process I think we would  
be able to move really quickly.

The port itself (which is on top of the VM used) is based on the work that Fabcio did and will remain open source  
together with the forked changes we needed to get iKVM to work.

#### How Will it Differ From the Existing Port?

iKVM generates assemblies (.nets DLLs). This effectively means that the port will look more like the Android port  
and less like the iOS port (or the existing Windows Phone port).

The old Windows Phone XMLVM port translated the bytecode into C# code so for every class file you had in the  
java bytecode you would have a C# file with all the same methods. This would produce a large project containing  
all your translated code.

iKVM translates the bytecode to .net and doesn’t include any source. So you will have a project you can run in the IDE  
but you won’t be able to really debug it in the sense that you can debug iOS builds. This has some drawbacks but it  
does produce a very fast build process.

### Reliability & Continuity

One of the big problems with Windows support has been the long, slow queued builds. The reason for this is  
that we were unable to install the Windows toolchains on Windows Server setups due to reliance on the virtualization  
instruction set.

This put us into a problematic situation of hosting the Windows servers in our offices which is far from ideal.

The newer version of visual studio and the better build process might allow us to run on cloud hardware and  
get rid of our existing servers. Hosted solutions are, cheaper, faster and more reliable so this is something we’d  
love to do as soon as possible!

Unfortunately it means we would need to remove the original servers as they might grab builds intended for the new  
servers.

### Final Word

We’ll make the transition as gently as possible by providing a way for those of you who rely on Windows builds  
to try the new servers thru a `build.xml` edit. Once we confirm this approach we will flip the switch for everyone.  
Keep an eye on this blog and once we have the process in place be sure to try it right away to provide feedback.

We will provide a way to build on our old windows servers for a while to soften the migration but eventually  
we’ll send them out to pasture.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Fabrício Cabeça** — April 4, 2016 at 6:40 pm ([permalink](/blog/new-windows-port/#comment-22432))

> Fabrício Cabeça says:
>
> This is great news indeed ! About supporting Windows 8.1 onwards we already accomplished this in our port, I will try to provide this support in your port. Many devices are still 8.1 and there are devices that are not going to receive the Windows 10 update.


### **Chidiebere Okwudire** — April 5, 2016 at 7:33 am ([permalink](/blog/new-windows-port/#comment-22720))

> Chidiebere Okwudire says:
>
> This sounds promising. I hope there’ll be a way to stil support Windows 8.x devices in the long run but we’ll see.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
