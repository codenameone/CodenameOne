---
title: SQL Explorer, Global Toolbar, Location & Docs
slug: sql-explorer-global-toolbar-location-docs
url: /blog/sql-explorer-global-toolbar-location-docs/
original_url: https://www.codenameone.com/blog/sql-explorer-global-toolbar-location-docs.html
aliases:
- /blog/sql-explorer-global-toolbar-location-docs.html
date: '2016-03-02'
author: Shai Almog
---

![Header Image](/blog/sql-explorer-global-toolbar-location-docs/documention.jpg)

I was working on documenting the SQLite support in Codename One, you can see some of that work both in the  
[db package](https://www.codenameone.com/javadoc/com/codename1/db/package-summary.html) and in the  
[developer guide](https://www.codenameone.com/manual/files-storage-networking.html). As a demo for  
SQL I decided to just create a tool that allows you to type arbitrary SQL to execute it on the device and see  
the results in a `Table`…​

Beside being a cool example this can be a hugely powerful debugging tool to one of the more painful API’s to  
debug on the device. You can integrate this as a hidden feature into your application and use it to debug odd “on device”  
issues by querying the DB!

### Toolbars All Around

The [Toolbar API](https://www.codenameone.com/javadoc/com/codename1/ui/Toolbar.html) is the way forward  
but up until now we didn’t include the option to set the `Toolbar` globally so for every `Form` you had to do the:
    
    
    Toolbar tb = new Toolbar();
    form.setToolbar(tb);

You can now set the `Toolbar` to be on by default on all forms so `form.getToolbar()` will return a valid `Toolbar`  
that you can use right away. There are two ways to enable it, in code using:
    
    
    Toolbar.setGlobalToolbar(true);

Or using the theme constant:
    
    
    globalToobarBool=true

### Debugging Location Calls

[Piotr](https://github.com/PiotrZub) contributed another [pull request](https://github.com/codenameone/CodenameOne/pull/1703)  
that refines the behavior of the simulator when working with the location API. This highlights a relatively  
accessible path to code contribution thru the Java SE code which should be easier.

Check out my post on contributing code to the Codename One project  
[here](https://www.codenameone.com/blog/how-to-use-the-codename-one-sources.html)

### Documentation Progress

Our manual is over 650 pages. We just finished a rewrite of the IO section which is **HUGE**!

Similarly to the galleries we made for components and layouts we created sections for the  
[database](https://www.codenameone.com/javadoc/com/codename1/db/package-summary.html),  
[XPath processing/parsing language](https://www.codenameone.com/javadoc/com/codename1/processing/package-summary.html) &  
[general IO (storage, filesystem, parsing, networking etc.)](https://www.codenameone.com/javadoc/com/codename1/io/package-summary.html).

Check out these sections and let us know what you think.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Dalvik** — March 4, 2016 at 1:21 pm ([permalink](https://www.codenameone.com/blog/sql-explorer-global-toolbar-location-docs.html#comment-22517))

> Dalvik says:
>
> Can we maybe add more functionality to the DB explorer tool?  
> This opens some potential to “on device analysis” style API’s. Any plans on that?
>



### **Shai Almog** — March 5, 2016 at 4:14 am ([permalink](https://www.codenameone.com/blog/sql-explorer-global-toolbar-location-docs.html#comment-22667))

> Shai Almog says:
>
> I’d love that but I’m not sure when we’ll get around to do something like this.
>



### **Mr Emma** — April 14, 2016 at 3:25 pm ([permalink](https://www.codenameone.com/blog/sql-explorer-global-toolbar-location-docs.html#comment-22595))

> Mr Emma says:
>
> For some reason your locationmanager never works for me even tho i follow all possible instructions, it works perfectly on the emulator but moving to a real android device it never works
>



### **Shai Almog** — April 15, 2016 at 3:01 am ([permalink](https://www.codenameone.com/blog/sql-explorer-global-toolbar-location-docs.html#comment-21508))

> Shai Almog says:
>
> I suggest trying it as we explained it.
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
