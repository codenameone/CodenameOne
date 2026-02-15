---
title: Social Boo Revisited
slug: social-boo-revisited
url: /blog/social-boo-revisited/
original_url: https://www.codenameone.com/blog/social-boo-revisited.html
aliases:
- /blog/social-boo-revisited.html
date: '2016-06-14'
author: Shai Almog
---

![Header Image](/blog/social-boo-revisited/socialboo.jpg)

SocialBoo originated from the same [app design vault](http://www.appdesignvault.com/shop/social-boo/) set  
of designs that brought us the Chrome Demo. This demo uses a somewhat outdated but still reasonable gunmetal look, it also features different UI styles when running  
on a tablet/phone. Notice in the screenshot above that the UI for the demo differs when running in a  
phone/tablet and even changes when the tablet flips between portrait and landscape…​

This demo was originally created using the old GUI builder but was modernized to use the new GUI builder.

**Check the live version running on the right hand side thanks to the power of the Codename One JavaScript port!**

You can check out the  
[full source code here](https://github.com/codenameone/SocialBoo).

We barely changed anything in this demo, although it did “age” it still looks reasonable even today. One of the  
few things added was a button to toggle the phone/tablet mode, this is really useful for a demo to show off  
how these differences apply especially when running in a browser.

We replaced the old title behavior with the new `Toolbar` API, this required a few code changes but not much and  
in terms of the design we didn’t need to do anything…​

The switch from the old GUI builder to the new GUI builder almost worked but there was some reliance on splash  
screen support in the old code. This isn’t supported by the new GUI builder as there is no concept of “flow”  
so we replicated roughly the same behavior using:
    
```java
if(SocialBoo.isTabletMode()) {
    UITimer.timer(2000, false, this, () -> new Person().show());
} else {
    UITimer.timer(2000, false, this, () -> new Main().show());
}
```

Notice that we used the `SocialBoo.isTabletMode()` call instead of `Display.getInstance().isTablet()`. We did that  
to allow forcing tablet/phone mode which is great for a demo although not really necessary for a “real world app”.

### Up Next

Social Boo isn’t currently a part of the builtin demos in the IDE’s but I think it should be. In our last update we  
were supposed to include some of the revised demos but the build scripts didn’t chug correctly and some demos  
just didn’t make it…​

We fixed the scripts so hopefully Social Boo and the other demos we’ve revisited will all make it to the coming  
plugin updates.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **salah Alhaddabi** — December 22, 2016 at 4:43 pm ([permalink](https://www.codenameone.com/blog/social-boo-revisited.html#comment-22957))

> salah Alhaddabi says:
>
> Dear shai,
>
> Can you please help me i dont know exactly how to clone this prject from github. When i import it using the download url it only clones the source folder and when i try to edit the form java classes it gives an error that the gui builder is only for classes created with the new gui builder.
>
> Can you please guide me how to clone this project so after i clone it it appears as if it was created using the netbeans codename one plugin???
>
> Thanks a lot in advance…
>



### **Shai Almog** — December 23, 2016 at 6:36 am ([permalink](https://www.codenameone.com/blog/social-boo-revisited.html#comment-23016))

> Shai Almog says:
>
> Hi,  
> It was created with NetBeans. I’m assuming you use Eclipse?
>
> You need to create a new project with the right package names and copy the source/properties and resources into the eclipse workspace.
>



### **salah Alhaddabi** — December 24, 2016 at 7:13 am ([permalink](https://www.codenameone.com/blog/social-boo-revisited.html#comment-23187))

> salah Alhaddabi says:
>
> Dear Shai I am using netbeans  
> I want to know how to clone the project using netbeans please
>



### **Shai Almog** — December 24, 2016 at 9:12 am ([permalink](https://www.codenameone.com/blog/social-boo-revisited.html#comment-23285))

> Shai Almog says:
>
> File -> New Project -> Codename One -> Demos -> Social Boo…
>



### **salah Alhaddabi** — December 24, 2016 at 5:53 pm ([permalink](https://www.codenameone.com/blog/social-boo-revisited.html#comment-23001))

> salah Alhaddabi says:
>
> Dear Shai thanks a lot.
>
> Now I have deleted the themes from the old GUI designer and only kept the UIIDs and the imported images.
>
> I am depending on the new GUI builder. When I run the app in the simulator I got the following issues:
>
> Error messages in the generated code in [Main.java](<http://Main.java>) file
>
> When I checked I found that all of the setPropertyValue methods which have “icon”, “icons”, or “selectedIcons” property name are not generated correctly.
>
> For example, the closing parentheses in the following statement is missing when the code is regenerated every time the app is run:
>
> gui_JessicaButton.setPropertyValue(“icon”, (resourceObjectInstance.getImage(“avatar-1a.jpg”));
>
> There are other similar icon setters methods in the same file that have the same issue.
>
> Due to that I am not able to run the app. Please help.
>



### **Shai Almog** — December 25, 2016 at 8:23 am ([permalink](https://www.codenameone.com/blog/social-boo-revisited.html#comment-23233))

> Shai Almog says:
>
> Thanks I see the bug. Unfortunately the only workaround I can see for now is to edit the .GUI file by hand and deleting this block:  
> <custom name=”icons” type=”com.codename1.ui.Image” array=”true” dimensions=”1″>  
> <str>[[email protected]](/cdn-cgi/l/email-protection)</str>  
> <str>[[email protected]](/cdn-cgi/l/email-protection)</str>  
> <str>[[email protected]](/cdn-cgi/l/email-protection)</str>  
> <str>[[email protected]](/cdn-cgi/l/email-protection)</str>  
> </custom>  
> <custom name=”selectedIcons” type=”com.codename1.ui.Image” array=”true” dimensions=”1″>  
> <str>friends@2x_sel.png</str>  
> <str>chats@2x_sel.png</str>  
> <str>send-email@2x_sel.png</str>  
> <str>settings@2x_sel.png</str>  
> </custom>
>
> We’ve committed a fix for this but because it needs a plugin update it might take a couple of weeks to release it as it’s a longer cycle than the typical library cycle.
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
