---
title: Changes & Stacks
slug: changes-stacks
url: /blog/changes-stacks/
original_url: https://www.codenameone.com/blog/changes-stacks.html
aliases:
- /blog/changes-stacks.html
date: '2014-05-10'
author: Shai Almog
---

![Header Image](/blog/changes-stacks/changes-stacks-1.png)

  
  
  
  
![Picture](/blog/changes-stacks/changes-stacks-1.png)  
  
  
  

One of the great features we’ve added to the new iOS VM is the ability to get proper stack traces without a performance penalty. This is actually pretty easy to implement in a performant way, every entry to a method just registers an integer number representing the method name and class name and every time we reach a line number in the source file we update the current line number. When throwing the exception we just assemble all of that data to produce the exception and the cost in terms of RAM/CPU is very low.  
  
We were hoping to expose the build argument for the new VM by now but right now builds are slow for some unclear reason… We have significantly less code in the new VM and its simpler code since it doesn’t include a lot of functionality that was necessary for the full Harmony API. But it builds slower and we are a bit stumped by that… 

We are busy catching up to the many issues and RFE’s opened by pro/enterprise users during the past few weeks as we were working on the new VM. Some features/fixes of interest include the JSONParser which now has a special mode called setUseLongs(). This mode uses long objects for round values rather than doubles. So a numeric value might be a long or it might be a double.

By default the JSON parser always produces Doubles, the reason behind that is that we are missing support for the java.lang.Number class which would have been really useful for this case. Without that class its difficult to write a generic parser so we chose to go only with doubles. However, for larger numbers this is a problem which is why we now also offer the option to generate longs.

In addition we made maps within the JSON (key/value pairs) use LinkedHashMap rather than HashMap, this preserves their order from within the JSON after parsing which is important for some use cases.

We added an isDesktop method to indicate if we are currently within a Desktop application (similar to isTablet()) and a canDial() method, both of whihc are part of Display. This is important since dialing from Tablets or Desktop machines will naturally fail so you might not want to show a button in the UI that will lead to a dial() invocation.  

* * *

Notice: This post was automatically converted using a script from an older blogging system. Some elements might not have come out as intended…. If that is the case please let us know via the comments section below.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
