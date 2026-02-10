---
title: Java Mobile Development Webinar Recap
slug: java-mobile-dev-webinar-recap
url: /blog/java-mobile-dev-webinar-recap/
original_url: https://www.codenameone.com/blog/java-mobile-dev-webinar-recap.html
aliases:
- /blog/java-mobile-dev-webinar-recap.html
date: '2015-08-11'
author: Steve Hannah
---

![Header Image](/blog/java-mobile-dev-webinar-recap/webinar-header-graphic.png)

Tuesday morning I held a webinar on Java mobile development using Codename One. First of all, I’d like to thank all who signed up and attended. Unfortunately there were some technical difficulties with the Webinar software that caused some major glitches. Double thanks to those who endured and stayed to the end. We’ve learned from this experience and we will do better in future webinars.

Since most of the people who signed up for the webinar indicated that they had no experience with Codename One, I spent the first 10 minutes or so introducing the framework, and showing some demos of some existing apps to give participants an idea of the possibilities.

The rest of the presentation was spent building custom social media app similar to facebook. I created a simple PHP/MySQL server-side application before hand and deployed it on the internet so that the mobile app would be able to connect to it from anywhere in the world. Therefore, the finished result, is actually a usable social network – though it is only “demo” grade, and I’ll probably take it down in a few months.

Here are some screenshots of the app:

![Screen shots](/blog/java-mobile-dev-webinar-recap/social-app-screenshots.png)

The tutorial was structured like a cooking show, where I break the development of the app down into 12 “steps”. By the end of it we have a working app.

Since there were technical difficulties with the actual webinar, I “refilmed” it afterwards using Screenflow, and I am quite happy with the results. It has been posted on YouTube [here](https://youtu.be/8D-CEtWAgHk).

And you can access the source of the app (both client and server) [here](https://github.com/shannah/social-network).

Finally, you can try out the app on Android [here](http://cn-social-network-demo.weblite.ca/SocialNetwork-release.apk).

## The Next Webinar

In the next session, we will take this bare-bones, yet functional, social mobile app as a starting point and discuss ways to make it better. Since this is literally an app that was built in a day, there are obviously lots of low-hanging fruit that we can pick on. The agenda hasn’t been finalized, and the date isn’t set in stone, but it will be within the next two weeks, and some of the potential agenda items may include:

  1. Improving look and feel using styles, borders, and icons.

  2. Adding Push support so users will be notified when news is posted, or when they receive friend requests.

  3. Improving performance using various techniques including image compression, pre-sizing of images, network threads, and sprites.

  4. Introducing Local storage for offline support.

  5. Adding chat support

If there are particular issues you would like to cover in the next webinar, please post them in the comments below, and I’ll see if I can incorporate them.

### The Webinar Software

For this webinar, I used GotoWebinar because I was familiar with GotoMeeting. Unfortunately there were some issues that make it less than a perfect fit for our needs. I’ve been reviewing Google Hangouts, and may try that out for the next one. If you have suggestions, I’m also interested in hearing what solutions you have used.

Happy coding, and hope to see you all in the next session!
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **ugochukwu** — August 15, 2015 at 9:03 am ([permalink](https://www.codenameone.com/blog/java-mobile-dev-webinar-recap.html#comment-24191))

> thanks for this great tutorial
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fjava-mobile-dev-webinar-recap.html)


### **ugochukwu** — August 15, 2015 at 9:04 am ([permalink](https://www.codenameone.com/blog/java-mobile-dev-webinar-recap.html#comment-22217))

> ugochukwu says:
>
> i would like you to put create group feature in the next webinar
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fjava-mobile-dev-webinar-recap.html)


### **Lanre Makinde** — August 17, 2015 at 10:35 pm ([permalink](https://www.codenameone.com/blog/java-mobile-dev-webinar-recap.html#comment-22210))

> Lanre Makinde says:
>
> I really love this. Very straight to the nitty gritty details. And i really love the fact that the next webinar is going to be within 2 weeks. What a resourceful way to spend my summer break. However, there are a lot of stuffs i could ask you to treat in the next webinar. Below are a list you may take your pick.  
> 1\. Hybrid chats i.e. chat handling messages, pictures, video and audio sharing  
> 2\. Other social platform logins such as twitter, yahoo, etc  
> 3\. Can we have the location manager as a background service with a view to showcase a users/subscriber location accurately enough especially to showcase changes in geo coordinates.  
> 4\. Status of chats/messsages/hybrid chats unread/unaccessed as well as a notification on of chat arrivals, etc  
> Thanks
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fjava-mobile-dev-webinar-recap.html)


### **kilani rabah** — February 2, 2016 at 1:13 pm ([permalink](https://www.codenameone.com/blog/java-mobile-dev-webinar-recap.html#comment-22600))

> kilani rabah says:
>
> how to solve his problem can ????:
>
> No GUI Entries available
>
> init:
>
> Deleting: C:UsersMoiDocumentsNetBeansProjectsCodenameOne6build[built-jar.properties](<http://built-jar.properties>)
>
> deps-jar:
>
> Updating property file: C:UsersMoiDocumentsNetBeansProjectsCodenameOne6build[built-jar.properties](<http://built-jar.properties>)
>
> Compile is forcing compliance to the supported API’s/features for maximum device compatibility. This allows smaller
>
> code size and wider device support
>
> compile:
>
> run:
>
> févr. 02, 2016 1:56:37 PM java.util.prefs.WindowsPreferences <init>
>
> WARNING: Could not open/create prefs root node SoftwareJavaSoftPrefs at root 0x80000002. Windows RegCreateKeyEx(…) returned error code 5.
>
> java.lang.reflect.InvocationTargetException
>
> at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
>
> at sun.reflect.NativeMethodAccessorImpl.invoke([NativeMethodAccessorImpl.java](<http://NativeMethodAccessorImpl.java>):62)
>
> at sun.reflect.DelegatingMethodAccessorImpl.invoke([DelegatingMethodAccessorImp…](<http://DelegatingMethodAccessorImpl.java>):43)
>
> at java.lang.reflect.Method.invoke([Method.java](<http://Method.java>):498)
>
> at com.codename1.impl.javase.Executor$1$[1.run](<http://1.run)([Executor.java](http://Executor.java)>:75)
>
> at com.codename1.ui.Display.processSerialCalls([Display.java](<http://Display.java>):1149)
>
> at com.codename1.ui.Display.mainEDTLoop([Display.java](<http://Display.java>):966)
>
> at [com.codename1.ui.RunnableWr…](<http://com.codename1.ui.RunnableWrapper.run)([RunnableWrapper.java](http://RunnableWrapper.java)>:120)
>
> at [com.codename1.impl.Codename…](<http://com.codename1.impl.CodenameOneThread.run)([CodenameOneThread.java](http://CodenameOneThread.java)>:176)
>
> Caused by: java.lang.NullPointerException
>
> at com.mycompany.myapp.SocialNetwork.init([SocialNetwork.java](<http://SocialNetwork.java>):85)
>
> … 9 more
>
> Java Result: 1
>
> BUILD SUCCESSFUL (total time: 19 seconds)
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fjava-mobile-dev-webinar-recap.html)


### **Shai Almog** — February 3, 2016 at 3:34 am ([permalink](https://www.codenameone.com/blog/java-mobile-dev-webinar-recap.html#comment-22539))

> Shai Almog says:
>
> Did you create a GUI builder app or a handcoded app?
>
> The default is to create a handcoded app and if so you need to select the GUI builder option explicitly.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fjava-mobile-dev-webinar-recap.html)

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
