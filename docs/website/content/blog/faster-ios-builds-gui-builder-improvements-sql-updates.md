---
title: Faster iOS Builds, GUI Builder Improvements & SQL Updates
slug: faster-ios-builds-gui-builder-improvements-sql-updates
url: /blog/faster-ios-builds-gui-builder-improvements-sql-updates/
original_url: https://www.codenameone.com/blog/faster-ios-builds-gui-builder-improvements-sql-updates.html
aliases:
- /blog/faster-ios-builds-gui-builder-improvements-sql-updates.html
date: '2016-10-24'
author: Shai Almog
---

![Header Image](/blog/faster-ios-builds-gui-builder-improvements-sql-updates/generic-java-2.jpg)

I’ve recently noticed that distribution build sizes were identical for appstore and debug builds which wasn’t the case  
before the xcode 7.x migration we did a while back. This shouldn’t be the case as it indicates that the standard  
debug builds include both the 64bit and 32bit code which is redundant during debugging.

We made some changes that should apply for the next update this Friday that might double build speed for some of  
you as it will do less work during compiling but also produce a smaller binary.

__ |  I would also recommend unchecking “include source” which slows down the build as the zip and upload of  
a huge file slows down the whole process   
---|---  
  
### GUI Builder Update

I wanted to put out a new demo today and I have something really nice on the way…​ But then we decided to do it  
with the GUI builder which resulted in about [15 issues so far](https://github.com/codenameone/CodenameOne/issues?utf8=%E2%9C%93&q=is%3Aissue%20is%3Aopen%20guibuilder).

So the good news is that we are improving the GUI builder and tracking down bugs/inconveniences etc. The bad  
news is that we’ll need to wait for a nice new demo for next week as this is pretty difficult.

We also added quite a few cool new features to the GUI builder such as:

  * Ability to refresh to see the list of images added to the designer

  * Ability to add a new image directly from the GUI builder without launching the designer

  * UIID editing that allows us to pick from a list of UIID’s in the theme

And quite a few other features.

### SQL Updates

Steve made some changes to the SQL Javadocs to clarify some SQL limitations in the JavaScript port.

Besides the lack of portability for the SQL support there (due to the fact that SQL is not an official W3C standard),  
there are two big issues:

  * Transactions can’t be mapped properly – the browser support for SQL wraps transactions in a very different way  
and this can’t map to the Java side.

  * There is no database file so the trick of shipping a database file with your app and installing it won’t work!

Ideally I’d love for us to have a better object persistence solution that will remove the need for SQL in the long  
term.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Nick Koirala** — October 27, 2016 at 3:40 am ([permalink](https://www.codenameone.com/blog/faster-ios-builds-gui-builder-improvements-sql-updates.html#comment-22745))

> Nick Koirala says:
>
> What sort of solution are you thinking for an SQL replacement? The advantage of SQL in my mind compared to key/value storage is complex querying of the data. Its possible in NoSQL solutions of course, but they seem complicated to embed in platforms that (almost) all support SQLite. But a better solution is of course welcome, there are quirks and issues with SQLite – though I use it extensively as I don’t know of any good alternatives.
>



### **Shai Almog** — October 28, 2016 at 3:43 am ([permalink](https://www.codenameone.com/blog/faster-ios-builds-gui-builder-improvements-sql-updates.html#comment-23143))

> Shai Almog says:
>
> We considered a lot of options but haven’t made a choice yet. While all platforms support a form of sqlite this is misleading as the functionality varies a lot resulting in device issues which is what we try to avoid.
>
> 1\. Offering hsql or a similar javadb as a cn1lib or even builtin – this will probably increase the dist size a bit so I’m a bit weary of it. I’m not too crazy about SQL either.  
> 2\. Supporting one of the leading mobile object DB’s e.g. realm – While this has value this might be a problem as none of them is nearly as portable as we are e.g. the JavaScript port is always problematic.  
> 3\. Build a simple Object storage maybe on top of something else – I’m hesitant to go into something like that. It’s not hard but those are famous last words…
>



### **Nick Koirala** — October 28, 2016 at 3:43 am ([permalink](https://www.codenameone.com/blog/faster-ios-builds-gui-builder-improvements-sql-updates.html#comment-23094))

> Nick Koirala says:
>
> iOS 10.1 shows a message ‘This app may slow down your iPhone’ if its a 32bit build. *sigh* I guess it only affects debug / ad-hoc builds but it is a bit dumb.
>



### **Akinniranye James** — November 1, 2016 at 3:51 pm ([permalink](https://www.codenameone.com/blog/faster-ios-builds-gui-builder-improvements-sql-updates.html#comment-23035))

> Akinniranye James says:
>
> Will vote for 1
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
