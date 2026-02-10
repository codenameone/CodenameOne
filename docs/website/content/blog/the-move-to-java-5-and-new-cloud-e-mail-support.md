---
title: The Move To Java 5 And New Cloud E-Mail Support
slug: the-move-to-java-5-and-new-cloud-e-mail-support
url: /blog/the-move-to-java-5-and-new-cloud-e-mail-support/
original_url: https://www.codenameone.com/blog/the-move-to-java-5-and-new-cloud-e-mail-support.html
aliases:
- /blog/the-move-to-java-5-and-new-cloud-e-mail-support.html
date: '2013-07-02'
author: Shai Almog
---

![Header Image](/blog/the-move-to-java-5-and-new-cloud-e-mail-support/the-move-to-java-5-and-new-cloud-e-mail-support-1.jpg)

  
  
  
  
![Picture](/blog/the-move-to-java-5-and-new-cloud-e-mail-support/the-move-to-java-5-and-new-cloud-e-mail-support-1.jpg)  
  
  
  

We started moving the usages of Vector and Hashtable to use Map/Collection/HashMap & ArrayList. This follows our release of the full source code for our modified version of retroweaver and the Java packages that go with it. Recap: we had to change retroweaver which was designed to allow Java 5 code to run on Java 2 in order to get it to work with CLDC which is missing MANY features. 

  
We started by changing the most important and central features (there is a lot of code that uses old collections, changing it all will be challenging and sometimes not worth the hassle e.g. HTMLComponent). So we changed the TextArea, Component, Container, Display & DefaultListModel to use  
  
the ArrayList class. We chose those since they are central and should yield the greatest performance benefit, we also chose them because they are well encapsulated and thus the change should be completely seamless API wise.

  
We also added  
  
Map/Collection support to the Util write object classes and Storage serialization code. So if you pass a vector or hashtable to the code it will act like before, however if you pass a different collection (e.g. ArrayList) or Map (e.g. HashMap) it will be stored properly as well. Right now when we unmarshal (read) the data for a Map or Collection we will always create as HashMap or ArrayList never creating the actual concrete class, the reason we can’t do that is that if we saved the class name and it was obfuscated this might break between revisions.

  
So this was the “low hanging fruit” now comes the tough part… Some developers have asked us in the past to generify some interfaces e.g. EventDispatcher, ListCellRenderer & DefaultListModel. To be honest I’m not so sure how well this will work but I understand the value of being able to do that. I have some concerns about binary compatibility for these cases but from my understanding it should be possible.  

  
How important would this be to you guys?  

  
  
  
Do you want to see our common interfaces generified?  

  
As a side note we also added an ability in our cloud server to send email messages directly from the app, which can be pretty useful since it doesn’t require a working email account or permissions other than networking. Its only available for pro accounts or higher, and allows you to use the Message API’s methods to send an HTML or plain text email directly through our build cloud server. Notice that due to spam issues all emails will arrive from a Codename One address (although you can customize the name of the sender) E.g. to send an email you can do something like this:  
  

* * *

Notice: This post was automatically converted using a script from an older blogging system. Some elements might not have come out as intended…. If that is the case please let us know via the comments section below.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Anonymous** — July 4, 2013 at 2:48 am ([permalink](https://www.codenameone.com/blog/the-move-to-java-5-and-new-cloud-e-mail-support.html#comment-21616))

> Anonymous says:
>
> I for one would appreciate Generifying the interface. The most important thing for me is (#1) Being able to write the same code for iPhone and Android apps. (#2) A nice API, (#3) Windows 8 Phone and Blackberry 10 support.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fthe-move-to-java-5-and-new-cloud-e-mail-support.html)


### **Anonymous** — July 6, 2013 at 8:52 am ([permalink](https://www.codenameone.com/blog/the-move-to-java-5-and-new-cloud-e-mail-support.html#comment-21928))

> Anonymous says:
>
> I am new to codename one, but based on your blog post, it seems that there are few things which are still used just to support CLDC profile, I would suggest that make framework separate for smart ;phone and CLDC so that you guys can make full use of Smart phones capabilities.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fthe-move-to-java-5-and-new-cloud-e-mail-support.html)


### **Anonymous** — July 6, 2013 at 9:04 am ([permalink](https://www.codenameone.com/blog/the-move-to-java-5-and-new-cloud-e-mail-support.html#comment-21690))

> Anonymous says:
>
> The code is optimized for smartphones and provides access to the full smartphone capabilities including access to native code. 
>
> The post is about changing API’s not about CLDC compatibility. 
>
> We hide some Java language capabilities so our distribution size can be smaller and more efficient on iOS/Windows Phone.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fthe-move-to-java-5-and-new-cloud-e-mail-support.html)

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
