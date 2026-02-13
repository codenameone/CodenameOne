---
title: Codename One 3.2 Now Live
slug: codename-one-3-2-now-live
url: /blog/codename-one-3-2-now-live/
original_url: https://www.codenameone.com/blog/codename-one-3-2-now-live.html
aliases:
- /blog/codename-one-3-2-now-live.html
date: '2015-10-26'
author: Shai Almog
---

![Header Image](/blog/codename-one-3-2-now-live/CodenameOne-Horizontal.png)

We are thrilled to announce the immediate availability of Codename One 3.2!  
Version 3.2 sets the pace for many upcoming features & migration processes such as the  
new cloud infrastructure for push servers, modernized GUI builder etc.  
Codename One 3.3 is currently scheduled for January 27th 2016 and should continue the trend of iterative changes  
that form a larger platform evolution arch. 

### Highlights Of The Release – Click For Details

____New GUI Builder (technology preview)

The new GUI builder is a big departure from our existing  
designer tool. This tool is now in “technology preview” status meaning that its not quite ready for  
prime time but we want feedback on its direction and issues.  
Read more about this work in [this blog post](/blog/new-gui-builder.html). 

____Local Notifications on iOS and Android

Local notifications are similar to push notifications, except that they are initiated locally by the app,  
rather than remotely. They are useful for communicating information to the user while the app is running in the background, since they  
manifest themselves as pop-up notifications on supported devices.  
Read more about this work in [this blog post](/blog/local-notifications.html). 

____Introduced New Push Server Architecture

We completely overhauled the way Codename One handles push services  
and added several long time RFE’s to the mix.  
Read more about this work in [this blog post](/blog/new-push-servers.html). 

____Added Ability for cn1libs To Include Build Hints

cn1libs now include the ability to include build hints thus  
integrate more seamlessly without complex integration instructions.  
Read more about this work in [this blog post](/blog/deprecations-simplified-cn1lib-installs-theme-layering.html). 

____Improved iOS/Android Rendering Speed

Thanks to a community contribution we took a deep look at the rendering  
code and are using faster code for tiling/string rendering.  
Read more about this work in [this github pull request](https://github.com/codenameone/CodenameOne/pull/1580). 

____Added A Permanent Side Menu Option

The Toolbar API has really picked up, in order to make it more useful for Tablets  
we added the ability to keep the SideMenuBar that’s builtin to it always on.  
Read more about this work in [this blog post](/blog/permanent-sidemenu-getAllStyles-scrollbar-and-more.html). 

____Get All Styles – Simplified Handcoding Theme Elements

getAllStyles() allows writing code that is more concise to perform an operation on multiple  
style objects at once.  
Read more about this work in [this blog post](/blog/permanent-sidemenu-getAllStyles-scrollbar-and-more.html). 

____Added Support For Facebooks “Invite A Friend”

New integration for Facebooks “invite a friend” feature that simplifies  
viral marketing for your app.  
Read more about this work in [this blog post](/blog/invite-friends-websockets-windows-phone-more.html). 

____Terse Syntax For Building UI’s

A shorter syntax for adding components and labels into the UI resulting in less code for the same functionality.  
Read more about this work in [this blog post](/blog/terse-syntax-migration-wizard-more.html). 

____Java 8 Language Features are now on by default

We fixed many things in this implementation over the past  
three months and feel confident enough to switch this into the default.  
Read more about this work in [this blog post](/blog/java-8-support.html). 

You can also read the far more detailed list of release notes [here](/codenameone-3-2-release-notes.html).
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Diamond** — October 27, 2015 at 11:52 am ([permalink](https://www.codenameone.com/blog/codename-one-3-2-now-live.html#comment-22472))

> Diamond says:
>
> Thank you guys for the hardwork,
>
> My build always fail whenever I send a build using 1.8, I got a message that codenameone supports up to 1.7 java version… Does the build server support 1.8 now?
>
> And may I ask if “background process while app is not running” would be implemented anytime soon?
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fcodename-one-3-2-now-live.html)


### **Shai Almog** — October 28, 2015 at 3:35 am ([permalink](https://www.codenameone.com/blog/codename-one-3-2-now-live.html#comment-22195))

> Shai Almog says:
>
> You need to explicitly use the Java 8 support either by migrating your project to a Java 8 project type or creating a new project. Our server code converts Java 8 bytecode down to Java 5 making this seamless to our servers.
>
> Background processes is something we slated and discussed for 3.2 and worked on a lot. The end result was just background notifications which IMO is the least important of the bunch. Hopefully this will land sooner rather than later.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fcodename-one-3-2-now-live.html)


### **Diamond** — October 28, 2015 at 4:17 am ([permalink](https://www.codenameone.com/blog/codename-one-3-2-now-live.html#comment-22487))

> Diamond says:
>
> Hi,
>
> How do I add MultiImages or create UIIDs in the new GUI Builder.
>
> Can we have a double click function on components to open their properties please.
>
> Dragging components crashes the Gui Builder sometimes.
>
> When I delete component, it doesn’t disappear from the tree and if I try to delete it again, the Gui Builder crashes.
>
> When I select a Container that has one component inside, I got properties of the component and not the container itself.
>
> Most of this stuff happens randomly.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fcodename-one-3-2-now-live.html)


### **Shai Almog** — October 28, 2015 at 9:47 am ([permalink](https://www.codenameone.com/blog/codename-one-3-2-now-live.html#comment-22180))

> Shai Almog says:
>
> Hi,  
> are you sure you are building with the right account? It should be logged to the console.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fcodename-one-3-2-now-live.html)


### **Gerben** — October 28, 2015 at 9:49 am ([permalink](https://www.codenameone.com/blog/codename-one-3-2-now-live.html#comment-21940))

> Gerben says:
>
> Turned out we had paypal issues and our account was terminated or something like that at the exact moment I installed 3.2. But is was unrelated.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fcodename-one-3-2-now-live.html)


### **Shai Almog** — October 28, 2015 at 9:55 am ([permalink](https://www.codenameone.com/blog/codename-one-3-2-now-live.html#comment-22242))

> Shai Almog says:
>
> Hi,  
> adding multi-images is part of the theme which is a separate feature altogether. You need to still do that with the old designer and it will be accessible then. The same is true for creating/manipulating UIID’s. We’ll replace the theme generating functionality in the old designer using a different tool, it was a mistake mixing everything into a single tool.
>
> Single clicking a component in the UI opens its properties on the left side. If you pick it from the tree then it will be selected as you change the tabs. Notice that properties are now split into “Basic”, “Advanced” & “Events” (at the bottom of the tab). There is also a separate tab to control layout.
>
> If you get crashes or errors a log would be nice, we will add some better crash logging for the next update and hopefully start fixing these bugs quickly.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fcodename-one-3-2-now-live.html)


### **Tom Arn** — October 30, 2015 at 10:03 am ([permalink](https://www.codenameone.com/blog/codename-one-3-2-now-live.html#comment-22266))

> Tom Arn says:
>
> Is the source code of the new gui builder already available for download?  
> Best regards  
> Tom
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fcodename-one-3-2-now-live.html)


### **ahmed** — October 31, 2015 at 3:52 am ([permalink](https://www.codenameone.com/blog/codename-one-3-2-now-live.html#comment-22386))

> ahmed says:
>
> The latest version i get in intellij is 3.1
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fcodename-one-3-2-now-live.html)


### **Shai Almog** — October 31, 2015 at 4:16 am ([permalink](https://www.codenameone.com/blog/codename-one-3-2-now-live.html#comment-22396))

> Shai Almog says:
>
> Our plugins aren’t open source and we are looking at the new GUI builder as a part of the plugin so at this time we don’t plan to open source it.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fcodename-one-3-2-now-live.html)


### **Shai Almog** — October 31, 2015 at 4:17 am ([permalink](https://www.codenameone.com/blog/codename-one-3-2-now-live.html#comment-22403))

> Shai Almog says:
>
> We are working on a partial rewrite of the IntelliJ plugin, this is taking some time.  
> Most features of 3.2 are available on IntelliJ via a library update.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fcodename-one-3-2-now-live.html)


### **Yaakov Gesher** — November 10, 2015 at 10:07 pm ([permalink](https://www.codenameone.com/blog/codename-one-3-2-now-live.html#comment-22393))

> Yaakov Gesher says:
>
> After upgrading to 3.2, using the old GUI Builder, every time I make a change in a form I get a little popup saying “GUI Builder error – undoing”, but it doesn’t actually undo the changes made.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fcodename-one-3-2-now-live.html)


### **Shai Almog** — November 11, 2015 at 4:53 am ([permalink](https://www.codenameone.com/blog/codename-one-3-2-now-live.html#comment-22368))

> Shai Almog says:
>
> Can you run the GUI builder from command line and get the logged output when you get that error?
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fcodename-one-3-2-now-live.html)


### **Yaakov Gesher** — November 11, 2015 at 7:51 pm ([permalink](https://www.codenameone.com/blog/codename-one-3-2-now-live.html#comment-22455))

> Yaakov Gesher says:
>
> What’s the command I use for that?
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fcodename-one-3-2-now-live.html)


### **Shai Almog** — November 12, 2015 at 3:09 am ([permalink](https://www.codenameone.com/blog/codename-one-3-2-now-live.html#comment-21498))

> Shai Almog says:
>
> java -jar ~/.codenameone/designer_1.jar  
> ~ is the home directory if you are doing this in Windows and you would naturally need to reverse the slashes.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fcodename-one-3-2-now-live.html)


### **Yaakov Gesher** — November 14, 2015 at 9:16 pm ([permalink](https://www.codenameone.com/blog/codename-one-3-2-now-live.html#comment-21540))

> Yaakov Gesher says:
>
> I’ll email you the stack trace.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fcodename-one-3-2-now-live.html)


### **Shai Almog** — November 15, 2015 at 4:08 am ([permalink](https://www.codenameone.com/blog/codename-one-3-2-now-live.html#comment-22548))

> Shai Almog says:
>
> I see the issue but I don’t think its a regression since this is pretty old code. Did you change something with the TableLayout in that hierarchy?  
> Can you change the column count to be larger or is this inaccessible in the GUI?
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fcodename-one-3-2-now-live.html)


### **Yaakov Gesher** — November 21, 2015 at 8:48 pm ([permalink](https://www.codenameone.com/blog/codename-one-3-2-now-live.html#comment-24200))

> Yaakov Gesher says:
>
> Yeah, I realized later that I was adding components beyond the TableLayout’s defined row count. But shouldn’t there be a more user-friendly error message?
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fcodename-one-3-2-now-live.html)


### **Shai Almog** — November 22, 2015 at 4:37 am ([permalink](https://www.codenameone.com/blog/codename-one-3-2-now-live.html#comment-21548))

> Shai Almog says:
>
> It auto increments the row. This is a bug that we fixed.
>
> That’s just a workaround until we provide a new version of the designer.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fcodename-one-3-2-now-live.html)

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
