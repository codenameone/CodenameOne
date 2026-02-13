---
title: Library Update & NativeInterface Generics
slug: library-update-nativeinterface-generics
url: /blog/library-update-nativeinterface-generics/
original_url: https://www.codenameone.com/blog/library-update-nativeinterface-generics.html
aliases:
- /blog/library-update-nativeinterface-generics.html
date: '2016-03-08'
author: Shai Almog
---

![Header Image](/blog/library-update-nativeinterface-generics/phone-gallery.jpg)

We just updated the library with our last ad hoc release, starting next week we will release libraries every Friday  
and occasionally also a plugin update within that same proximity. We made some refinements to some  
Android theme elements and one of those refinements is a new `TitleArea` drop shadow.

This effectively creates a situation where we can’t reasonably detect an empty title and so if you want the title  
to truly disappear you can either:

  * Set the `TitleArea` border attribute to empty

  * Do something like `form.getToolbar().setUIID("Container")`

We released an update for the Eclipse plugin but are experiencing some issues with the NetBeans update  
center which we will hopefully resolve before the weekend.

As a side note we are working hard to re-imagining the IntelliJ/IDEA plugin, but our current approach will  
probably still rely on Ant instead of a move to gradle or maven. Both of these seem like really good ideas  
on paper but at the moment they might not be as convenient. I’ll try to write a bit on the process as we  
move forward and have a clearer picture on this.

### NativeInterface Generics

The `NativeInterface` API’s haven’t changed much since their inception but we just committed a small  
nice to have feature.

Up until now if you wanted to get a reference to your native interface you would have to do this:
    
    
    MyNativeInterface m = (MyNativeInterface)NativeLookup.create(MyNativeInterface.class);

With the new version you can now do this:
    
    
    MyNativeInterface m = NativeLookup.create(MyNativeInterface.class);

That’s a nice trick generics can pull off by setting the method signature to:
    
    
    public static <T extends NativeInterface> T create(Class<T> c);

I wasn’t a big fan of generics when they came out and I still don’t like the declaration of the method but the end  
result is really nice.

### Documentation Update

We are on the brink of 700 pages for the developer guide and I think we’ll finish just shy of 1000 for this iteration.

This work is taking some time but it’s totally worth the effort as the level of the documentation is at a league  
of its own. E.g. check out the IO section in the guide [here](https://www.codenameone.com/manual/files-storage-networking.html).  
The original was remarkably bare and didn’t cover so many basic things that should be covered e.g.  
the webservice wizard, [SliderBridge](https://www.codenameone.com/javadoc/com/codename1/components/SliderBridge.html)  
etc…​
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Chidiebere Okwudire** — March 10, 2016 at 8:27 am ([permalink](https://www.codenameone.com/blog/library-update-nativeinterface-generics.html#comment-22391))

> Chidiebere Okwudire says:
>
> I’m really excited about the updates to the Android toolbar and will try it out as soon as the netbeans plugin is updated. Well done!
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Flibrary-update-nativeinterface-generics.html)


### **znprojects** — March 10, 2016 at 11:33 am ([permalink](https://www.codenameone.com/blog/library-update-nativeinterface-generics.html#comment-22747))

> znprojects says:
>
> The documentation looks fantastic. I remember this being one of the things that always made me hesitant to recommend CN1 to others in the past. Seeing what’s there now, that hesitation is gone.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Flibrary-update-nativeinterface-generics.html)


### **Nigel Chomba** — March 15, 2016 at 5:50 pm ([permalink](https://www.codenameone.com/blog/library-update-nativeinterface-generics.html#comment-22536))

> Nigel Chomba says:
>
> Netbeans plug in is taking too long, cant the plugin be hosted on your site as well since you updated us netbeans has issues for now.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Flibrary-update-nativeinterface-generics.html)


### **Shai Almog** — March 16, 2016 at 4:07 am ([permalink](https://www.codenameone.com/blog/library-update-nativeinterface-generics.html#comment-22351))

> Shai Almog says:
>
> We are communicating with them: [http://forums.netbeans.org/…](<http://forums.netbeans.org/viewtopic.php?p=169246#169246>)
>
> We used to have our own host but maintaining both was a pain and bred confusion for developers.
>
> This isn’t a big deal since the libraries/simulator etc. are all updated instantly as we release them.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Flibrary-update-nativeinterface-generics.html)


### **Nigel Chomba** — March 16, 2016 at 12:05 pm ([permalink](https://www.codenameone.com/blog/library-update-nativeinterface-generics.html#comment-22692))

> Nigel Chomba says:
>
> Okay thanks for the clarity.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Flibrary-update-nativeinterface-generics.html)

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
