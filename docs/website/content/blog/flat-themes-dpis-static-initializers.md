---
title: Flat Themes, DPI's & Static Initializers
slug: flat-themes-dpis-static-initializers
url: /blog/flat-themes-dpis-static-initializers/
original_url: https://www.codenameone.com/blog/flat-themes-dpis-static-initializers.html
aliases:
- /blog/flat-themes-dpis-static-initializers.html
date: '2015-05-12'
author: Shai Almog
---

![Header Image](/blog/flat-themes-dpis-static-initializers/flat-blue-theme.png)

With the release of 3.0 we were overwhelmed with a lot of last minute features and improvements. It seems  
that we neglected to mention a lot of features that made it into the final 3.0 product.  
One of the nicest new features is a set of new flat themes with various colors in the designer and the project  
wizard. We found that a lot of developers prefer themes with more control over the look, themes that look  
more similar across platform yet have a more modern “flat” feel. 

A long time request has been to add the additional DPI resolutions we added for higher density devices into  
the designer tool. We now support these additional densities in the designer tool both in the multi image import  
and when editing a specific resolution. 

On a different subject, we noticed a couple of developers had one of those hard to track down bugs that boiled  
down to using the FileSystemStorage API from static context. E.g. this: 
    
    
    public static final String MY_HOME_DIR = FileSystemStorage.getInstance().getAppHomePath();

This seems like code that should work and annoyingly enough it works on the simulator. However, because of the  
way classloaders work this code will probably fail on devices. Static context can be initialized at any time and since  
the initialization of the implementation code might occur after the initialization of static context the static variable  
might not work well…   
You should pay attention to such code where you invoke implementation classes from static context and try to avoid  
it, ideally we’d make this fail on the simulator but that is a bit tricky.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **rhg1968** — May 13, 2015 at 6:48 pm ([permalink](https://www.codenameone.com/blog/flat-themes-dpis-static-initializers.html#comment-22262))

> rhg1968 says:
>
> I see the new templates in NetBeans but not in IntelliJ 14.1.3
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fflat-themes-dpis-static-initializers.html)


### **Shai Almog** — May 14, 2015 at 6:52 pm ([permalink](https://www.codenameone.com/blog/flat-themes-dpis-static-initializers.html#comment-22192))

> Shai Almog says:
>
> Can you file an issue on that?  
> We’ll need to add them there too. As a workaround you can just create a new project with a native theme. Open the designer tool, delete the “Theme” entry and create a new theme, you should have these options there assuming you are using the latest plugin.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fflat-themes-dpis-static-initializers.html)


### **rhg1968** — May 14, 2015 at 7:01 pm ([permalink](https://www.codenameone.com/blog/flat-themes-dpis-static-initializers.html#comment-22229))

> rhg1968 says:
>
> Thanks for the reply. I just put the issue on GitHub. I am using plugin 3.0.2 and I don’t have any updates so I believe I am using the latest. I also refreshed the libraries but I don’t see the theme in the GUI builder if I try to add a new theme to an existing project.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fflat-themes-dpis-static-initializers.html)


### **Shai Almog** — May 15, 2015 at 9:48 am ([permalink](https://www.codenameone.com/blog/flat-themes-dpis-static-initializers.html#comment-22044))

> Shai Almog says:
>
> Its possible the IDEA plugin doesn’t update the designer to the latest version too. We’ll look into it as part of the bug.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fflat-themes-dpis-static-initializers.html)


### **J.C** — May 15, 2015 at 3:04 pm ([permalink](https://www.codenameone.com/blog/flat-themes-dpis-static-initializers.html#comment-24173))

> J.C says:
>
> Shai, to clarify about static initializers for some people, it would be clearer to enclose the statement you illustrated inside static {}.
>
> For example, is this what you mean?
>
> Avoid:
>
> public static final String MY_HOME_DIR;  
> static {  
> MY_HOME_DIR = FileSystemStorage.getInstance().getAppHomePath();  
> }
>
> If not, please clarify and include an example that says: Don’t {….} and Do{…} etc..if its not too much trouble :).
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fflat-themes-dpis-static-initializers.html)


### **Shai Almog** — May 15, 2015 at 4:24 pm ([permalink](https://www.codenameone.com/blog/flat-themes-dpis-static-initializers.html#comment-22354))

> Shai Almog says:
>
> That would be a problem. Use a getter that invokes that method. Don’t store it in that static context.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fflat-themes-dpis-static-initializers.html)


### **J.C** — May 15, 2015 at 5:45 pm ([permalink](https://www.codenameone.com/blog/flat-themes-dpis-static-initializers.html#comment-22239))

> J.C says:
>
> Hmm, so what you are saying is, in general, avoid storing values returned by methods that is initializing underlying platform implementations into static variables when a class is created?
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fflat-themes-dpis-static-initializers.html)


### **Shai Almog** — May 16, 2015 at 8:51 am ([permalink](https://www.codenameone.com/blog/flat-themes-dpis-static-initializers.html#comment-22263))

> Shai Almog says:
>
> No. Avoid access to Codename One API’s from static context since it might get initialized before the Codename One API gets initialized.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fflat-themes-dpis-static-initializers.html)

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
