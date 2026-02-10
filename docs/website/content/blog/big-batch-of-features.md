---
title: Big Batch Of Features
slug: big-batch-of-features
url: /blog/big-batch-of-features/
original_url: https://www.codenameone.com/blog/big-batch-of-features.html
aliases:
- /blog/big-batch-of-features.html
date: '2013-06-29'
author: Shai Almog
---

![Header Image](/blog/big-batch-of-features/big-batch-of-features-1.png)

  
  
  
[  
![Multi Column Generic Spinner](/blog/big-batch-of-features/big-batch-of-features-1.png)  
](/img/blog/old_posts/big-batch-of-features-large-5.png)  
  
  

We’ve been so busy this past month that we just forgot mentioning many of the features that went into Codename One in June and  
  
  
  
there are MANY small features that just didn’t get a blog post or even a mention! 

  
We just open sourced a major piece of our backend code, the changes to retroweaver and library code that allow us to compile Java SE 5 code to J2ME/RIM. With this change in place we will start using Java 5 features throughout the code but mostly transition towards using ArrayList/HashMap and the base collection interfaces rather than Vector/Hashtable.  
  
  
  
  
  
  
  
On the right you should see the multi column generic spinner used in the  
[  
Maker  
](/maker.html)  
GUI builder, its right columns are filled dynamically based on the layout type. This is pretty easy to accomplish even from the GUI builder. The Generic spinner’s methods now accept an offset for the column and change the values/model within the given column.

Paul Harrison Williams contributed an  
[  
XML  
](http://code.google.com/p/codenameone/issues/detail?id=753)  
[  
writer  
](http://code.google.com/p/codenameone/issues/detail?id=753)  
implementation that allows you to serialize XML Elements to a stream.

  
Probably the most interesting/important feature  
  
is two new scale modes in Style: Scale to fit and Scale to fill. When you set a bg image to a style (either via the designer or in code) you can set the image behavior. The default is scaled, which never really made much sense. You can also tile or align the image in many ways but up until now we were missing two very basic and important features:  

  *   
Scale to fit scales the image so it will fit on the component while maintaining its aspect ratio. It will leave the background in the bgColor if transparency is defined as expected.  

  *   
Scale to fill scales the image so it will fill the entire component while maintaining aspect ratio, in this case the image will usually “flow” out of the screen in one of its edges.  
  

This is easier to explain with an example (in order scaled, scale to fit, scale to fill):  
  
  

* * *

[  
![](/blog/big-batch-of-features/big-batch-of-features-2.png)  
](/img/blog/old_posts/big-batch-of-features-large-6.png "Scaled - ignores aspect ratio and distorts the image")

[  
![](/blog/big-batch-of-features/big-batch-of-features-3.png)  
](/img/blog/old_posts/big-batch-of-features-large-7.png "Scale to fit - fills the background with bgColor and fits the image in the component")

[  
![](/blog/big-batch-of-features/big-batch-of-features-4.png)  
](/img/blog/old_posts/big-batch-of-features-large-8.png "Scale to fill - scales the image to fill the component while allowing for overflow")

  

As part of this work we also added a flag to image download service allowing it to maintain the aspect ratio of downloaded images which is really useful when working with images off the internet from users photo albums etc. 

  
Other than that we  
  
added a simple ability to create/delete contacts, this is rather basic but can help for many common use cases you can delete a contact using its “id” as: Display.getInstance().deleteContact(id);

  
You can also create a basic contact using:  
  
  
Display.getInstance().createContact(firstName, familyName, officePhone, homePhone, cellPhone, email);

  
We made some enhancements to our Facebook support as well but one  
  
of the more interesting ones is the ability to sign in to an App without a user visible OAuth process. This will grant you the ability to access public information on Facebook (e.g. pages, public posts/profiles etc.) but since no user will be logged in this won’t provide an “identity”. Note that you will still need to create a Facebook App to associate with the requests, then you can just login using:  
  
  
  
FacebookAccess.anonymousLogin(appid, clientSecret); 

Notice: This post was automatically converted using a script from an older blogging system. Some elements might not have come out as intended…. If that is the case please let us know via the comments section below.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
