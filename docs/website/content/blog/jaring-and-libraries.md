---
title: Jaring And Libraries
slug: jaring-and-libraries
url: /blog/jaring-and-libraries/
original_url: https://www.codenameone.com/blog/jaring-and-libraries.html
aliases:
- /blog/jaring-and-libraries.html
date: '2013-07-21'
author: Shai Almog
---

![Header Image](/blog/jaring-and-libraries/jaring-and-libraries-1.png)

  
  
  
  
![Classpath](/blog/jaring-and-libraries/jaring-and-libraries-1.png)  
  
  
Don’t change the classpath  
  

Support for JAR files in Codename One has been a source of confusion despite  
[  
my previous post on the matter  
](http://www.codenameone.com/3/post/2013/02/new-preliminary-library-support.html)  
so its probably a good idea to revisit this subject again and clarify all the details. 

  
The first source of confusion is changing the classpath. You should  
**  
NEVER  
**  
change  
  
the classpath or add an external JAR via the NetBeans/Eclipse classpath UI. The reasoning here is very simple, these IDE’s don’t package the JAR’s into the final executable and even if they did these JAR’s would probably use features unavailable or inappropriate for the device (e.g. java.io.File etc.).

There are two use cases for wanting JAR’s and they both have very different solutions:  

  1.   
Modularity – you want to divide your work to an external group. For this purpose use the cn1lib approach.  

  2.   
Work with an existing JAR. For this you will need native interfaces.  

  
  
**  
CN1Lib’s  
**  
  
  
  
Lets start with modularity since its simpler and you can pretty much read  
[  
my previous post on the matter  
](http://www.codenameone.com/3/post/2013/02/new-preliminary-library-support.html)  
which covers it pretty accurately.  
  
You can create a cn1lib in NetBeans although its really just a simple ant project with some special targets and a simple ant task for stubbing. In it you can write all your source code (including native code and libs as described below), when you build the file you will get a cn1lib file that you can place in your projects lib directory.  

  
After a right click and refresh project libs completion will be available for you and you will be able to work as if the code was a part of your project.  

**  
  
  
**  
  
**  
Native JAR’s/Libs  
**  
  
  
  
  
For the second use case of existing JAR’s we have a more complex situation, the JAR could depend on features unavailable in Codename One and even if it doesn’t it might be compiled in a way that isn’t supported by Codename One.  
  
  
  
So taking an arbitrary JAR off the internet and expecting it to work on a mobile device is something that will probably never happen for any platform.  
  
  
  
  
  
Android supports using JAR’s since it is based around the java language  
  
, some JAR’s might even work on J2ME/RIM. To make use of this capability we can just place the JAR’s as is under the native/android directory (and respectively for J2ME/RIM). The way to go for iOS support would be to use .a files (iOS static libraries) which will get linked with your app in the same way that the JAR’s get linked.

  
The problem is that you still won’t be able to use the JAR (or .a file) from  
  
within your code, this JAR would be platform specific and you would need to write the “bridging” code to connect it to the native layer. To learn more about native interfaces I suggest reading the developer guide but basically what you need to do is define a native interface e.g.:  
  
  

* * *

Here you can define the interface between your application (which can’t directly access the jar or .a file) and the native code which can. Now you can right click the interface select: Generate Native Access and go the native directory where you can edit the native code. 

  
Notice that the jar will not appear in your IDE’s code completion and the code might be marked as “red” with error messages. That is because we can’t compile that code on the client, only on the server since we don’t have the native Android/iOS SDK’s installed on your PC or Mac (and even if we did, integrating so many difference pieces of software is problematic).  

**  
  
Final words  
  
**  
  
  
Supporting  
  
an arbitrary JAR off the internet is something no one will ever be able to fully deliver on, although we do hope the cn1lib format will take off since it is pretty open and has many advantages over the standard jar format (proper javadoc based code completion is HUGE).  

Notice: This post was automatically converted using a script from an older blogging system. Some elements might not have come out as intended…. If that is the case please let us know via the comments section below.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Anonymous** — October 6, 2014 at 10:32 am ([permalink](/blog/jaring-and-libraries/#comment-22246))

> Anonymous says:
>
> Hello. Sorry for noob question, but I not fully understand, Codename1 supported JNI libraries? I have Android project with few JNI dependencies (source code – C/C++) and would be great to port this project to WinPhone/Blackberry, but I don’t understand is this real with Codename1?
>



### **Anonymous** — October 7, 2014 at 3:17 am ([permalink](/blog/jaring-and-libraries/#comment-22259))

> Anonymous says:
>
> JNI makes sense on Android where you call from Java to C you can use JNI in the Android port by wrapping in an andlib. In iOS we support an Objective-C bridge not JNI so you can invoke your C code from objective-c. Blackberry has no ability to define native C code, in Windows Phone we support C# not C but you might be able to do something with unsafe although I haven’t tried this.
>



### **Anonymous** — October 16, 2014 at 11:42 am ([permalink](/blog/jaring-and-libraries/#comment-22164))

> Anonymous says:
>
> “Supporting an arbitrary JAR off the internet is something no one will ever be able to fully deliver on” 
>
> I’m very skeptical about this claim after working with robovm, almost 99.99% of the time the arbitrary jar libraries that I downloaded from the internet and used worked out of the box.
>



### **Anonymous** — October 16, 2014 at 10:33 pm ([permalink](/blog/jaring-and-libraries/#comment-22282))

> Anonymous says:
>
> Besides the obvious problems of huge size and other problems generated in that approach there is a much bigger problem. JDK IS HUGE, we had a rooms full of QA engineers running TCK’s on our JVM ports which were CLDC grade at Sun, tonnes of tests for a WAY smaller API. OSS doesn’t test at that level. 
>
> This isn’t some theoretical debate. java.io.File is problematic since the iOS filesystem structure is very limited on iOS. Sockets (hence the entire network stack) can’t be implemented correctly in iOS. It will work for you in the testing environment but when you see disconnects in the field know that there is a reason for that. 
>
> This can be OK for some games to hack something together, but its not an option if you need something to be forward compatible and warranted.
>



### **Anonymous** — October 18, 2014 at 7:19 am ([permalink](/blog/jaring-and-libraries/#comment-21662))

> Anonymous says:
>
> the java.lang.*, java.util.*, etc in robovm are based on Android’s runtime, they don’t suffer from the size problems as in Java SE but still allows a lot of flexibility regarding the usage of third party libraries. I hope codenameone could learn something from robovm’s approach in using Java for IOS developement
>



### **Anonymous** — October 18, 2014 at 8:54 am ([permalink](/blog/jaring-and-libraries/#comment-22176))

> Anonymous says:
>
> Android VM is just as big as JavaSE. In an Android device it takes up that space ONCE for all the apps. On iOS you need to package it with the app so its HUGE. 
>
> I’ve spoken at length with Niklas at the last JavaOne and briefly in this one (we were both quite busy). I like him, hes very smart and a nice guy to boot. Unfortunately I think he picked a wrong and remarkably risky approach for building iOS apps. 
>
> To understand why you need to understand that: 
>
> works for me right now != works for all use cases always
>



### **Anonymous** — October 24, 2014 at 4:07 am ([permalink](/blog/jaring-and-libraries/#comment-21926))

> Anonymous says:
>
> Thanks for your replies, 
>
> packaging a run-time for each and every app is a bad idea. I agree. 
>
> a better approach would be to have some sort of a separate “run-time app” that manages all the java apps. 
>
> an analogy to this could be the python run-time app in Symbian.
>



### **Anonymous** — October 24, 2014 at 10:02 am ([permalink](/blog/jaring-and-libraries/#comment-22052))

> Anonymous says:
>
> Apple disallows that just like it disallows a JIT or downloadable code. 
>
> Both RoboVM and us take a very similar approach with the difference that we took a far more conservative route.
>



### **Anonymous** — November 24, 2014 at 6:16 pm ([permalink](/blog/jaring-and-libraries/#comment-21927))

> Anonymous says:
>
> I think i can use codename one for my project, but I wonder if It’ll support some complex libraries like Picasso or retrofit… and as there is no iOS alternative for those libraries what can i do about it. If not i may try it anyway for less complex projects. Thanks
>



### **Anonymous** — November 25, 2014 at 4:50 am ([permalink](/blog/jaring-and-libraries/#comment-22309))

> Anonymous says:
>
> Both of these don’t make sense in Codename One since they are there to solve Android specific problems and use Android networking API’s (unavailable in Codename One). 
>
> Picasso is redundant since we have URLImage which is even more seamless in its image handling builtin to the system e.g. [http://www.codenameone.com/…](<http://www.codenameone.com/blog/build-mobile-ios-apps-in-java-using-codename-one-on-youtube>) 
>
> Retrofit is nice but if you have access to the server a more portable (and much faster) approach would be to use the webservice wizard [http://www.codenameone.com/…]([http://www.codenameone.com/how-do-i—access-remote-webservices-perform-operations-on-the-server.html](http://www.codenameone.com/how-do-i---access-remote-webservices-perform-operations-on-the-server.html))
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
