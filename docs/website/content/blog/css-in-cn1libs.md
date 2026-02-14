---
title: CSS in CN1Libs
slug: css-in-cn1libs
url: /blog/css-in-cn1libs/
original_url: https://www.codenameone.com/blog/css-in-cn1libs.html
aliases:
- /blog/css-in-cn1libs.html
date: '2020-02-20'
author: Steve Hannah
---

![Header Image](/blog/css-in-cn1libs/css-in-cn1llibs.jpg)

We’ve just added support for including CSS inside of Codename One library projects so that CSS styles can now be distributed inside a cn1lib. This opens up a world of possibilities for creating module UI libraries and themes.

### How it works

To begin, you just need to add a “css” directory inside your **Codename One Library** project, with a “theme.css” file in it.

Add your CSS styles into the **theme.css** file, and build the library project.

If you add this module to a Codename One application project, these styles will automatically be included.

__ |  If you try to install a cn1lib that includes CSS into a **Codename One application** project that doesn’t have CSS activated, it will fail. You must activate CSS in the application project first.   
---|---  
  
### Bonus Tip: Auto-Installing Library into Apps when Building Library

This tip is for those of you who are building your own cn1libs. When I’m developing a CN1lib, I always have a separate application project that uses the lib. This is because you can’t test a cn1lib directly inside a library project. The cn1lib first has to be installed into application project before it can be used and tested.

This can create a lot of manual steps each time you make changes to your cn1lib and want to test them out. You need to build the library project, then copy the cn1lib from the library’s dist directory, into the application project’s lib directory. Then you need to select “Refresh Cn1libs” from the Codename One menu in the IDE.

As far as I’m concerned, anything more than a single button press is too much for being able to test my changes. Luckily its really easy to eliminate the extra steps by adding a small snippet into your library project’s build.xml file.

At the end of the “jar” target, add the following:
    
    
    <copy file="dist/${application.title}.cn1lib" todir="path/to/AppProject/lib"/>
    <ant dir="path/to/AppProject" target="refresh-libs-impl" usenativebasedir="true"/>

Now, whenever you build the library project, it will automatically copy it into your application project, and refresh its CN1libs, so that you can test your changes instantly.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Francesco Galgani** — February 23, 2020 at 10:06 am ([permalink](https://www.codenameone.com/blog/css-in-cn1libs.html#comment-21390))

> [Francesco Galgani](https://lh6.googleusercontent.com/-4K0ax_DVJf4/AAAAAAAAAAI/AAAAAAAAAAA/AMZuuckEd1kcni0y8k6NMzNtxwOCEPatQQ/photo.jpg) says:
>
> Thank you, however this can cause that the cn1lib CSS conflict with existing CSS. Another tip for the developer of a cn1lib could be the use of an unique prefix for every CSS included in the cn1lib. That prefix could be the name of the cn1lib, for example. Do you agree?
>



### **Shai Almog** — February 24, 2020 at 3:06 am ([permalink](https://www.codenameone.com/blog/css-in-cn1libs.html#comment-21389))

> Shai Almog says:
>
> I think that just using a unique name for a CSS element should be enough for most cases although a library specific prefix would probably be healthy.  
> I don’t think this is something we should force as we’d like the option for CSS to override global theme settings when required e.g. in the case of a theme library. E.g. a cn1lib that offers dark mode theming.
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
