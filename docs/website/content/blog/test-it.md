---
title: Test It
slug: test-it
url: /blog/test-it/
original_url: https://www.codenameone.com/blog/test-it.html
aliases:
- /blog/test-it.html
date: '2012-12-19'
author: Shai Almog
---

![Header Image](/blog/test-it/hqdefault.jpg)

  

One of the common questions we get for Codename One is regarding testability. Cross platform frameworks are notoriously hard to auto-test and so fail when you try to build more complex applications.  
  
We see this as one of the most important areas in which we can innovate and leapfrog native OS environments by offering testing that is just as cross platform as Codename One is. We aren’t just announcing our own unit testing API, we are announcing a fully integrated test recorder to auto-generate GUI tests for your applications and run them on the simulator. 

We will also integrate this exact same testing framework into our build server to seamlessly test device support for you in the future.

In the video above you can see me going through the process of recording a test for the kitchen sink and then running it in the simulator.

I’m sure I’m going to get this question so I’ll hit it right now before it comes up. No the tests are not JUnit compatible and we haven’t used another well known testing framework. The main logic behind this is that tools such as JUnit rely on reflection to work since they use annotations or method invocation without an interface. This doesn’t mesh well with Codename One’s device targets so we chose to implement something of our own.  
  
You would have to write most of the test cases for Codename One anyway since the API/functionality would be pretty different. 

Notice: This post was automatically converted using a script from an older blogging system. Some elements might not have come out as intended…. If that is the case please let us know via the comments section below.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Murali Kumar** — June 4, 2016 at 1:54 pm ([permalink](https://www.codenameone.com/blog/test-it.html#comment-22586))

> Murali Kumar says:
>
> No audio in the above video
>



### **Shai Almog** — June 5, 2016 at 4:14 am ([permalink](https://www.codenameone.com/blog/test-it.html#comment-22770))

> Shai Almog says:
>
> The video was recorded without audio just to demo the process. We’ll probably do a more thorough tutorial on building automated tests in the future.
>



### **Hristo Vrigazov** — September 4, 2016 at 8:17 am ([permalink](https://www.codenameone.com/blog/test-it.html#comment-21430))

> Hristo Vrigazov says:
>
> How can we test classes (without the test recorder)?
>



### **Shai Almog** — September 5, 2016 at 4:25 am ([permalink](https://www.codenameone.com/blog/test-it.html#comment-22761))

> Shai Almog says:
>
> Tests are just classes that derive abstract test. You can use the test recorder to create a template and then create as many of those as you want purely from code. The test process auto-detects them and runs them.
>



### **Takii Marskii** — October 19, 2016 at 9:03 pm ([permalink](https://www.codenameone.com/blog/test-it.html#comment-24228))

> Takii Marskii says:
>
> Is it possible to just run one Script out of the whole Test Scripts? If so how would I do that?
>



### **Shai Almog** — October 20, 2016 at 2:12 am ([permalink](https://www.codenameone.com/blog/test-it.html#comment-23116))

> Shai Almog says:
>
> We don’t provide that option currently.
>



### **Kumuda Garlapati** — May 1, 2024 at 6:14 pm ([permalink](https://www.codenameone.com/blog/test-it.html#comment-24603))

> Kumuda Garlapati says:
>
> I’m new to codename one and trying to find out how we can record and play back help us testing desktop application, how we can install simulator and record and run the tests. any help is really appreciated.
>



### **Shai Almog** — May 2, 2024 at 1:27 am ([permalink](https://www.codenameone.com/blog/test-it.html#comment-24604))

> Shai Almog says:
>
> The simulator is a part of the Codename One project. When you run the getting started project it’s there and so is the test recorder. Recorded tests are just source code. You need to add the source to the test package and then you have unit tests.
>



### **Kumuda Garlapati** — May 2, 2024 at 12:31 pm ([permalink](https://www.codenameone.com/blog/test-it.html#comment-24605))

> Kumuda Garlapati says:
>
> ok thanks for the info.
>



### **Kumuda Garlapati** — May 8, 2024 at 4:03 pm ([permalink](https://www.codenameone.com/blog/test-it.html#comment-24607))

> Kumuda Garlapati says:
>
> I was able to record the tests but not able to run them as seeing some errors “java.lang.ClassCastException: class com.codename1.components.InfiniteProgress cannot be cast to class com.codename1.ui.Container (com.codename1.components.InfiniteProgress and com.codename1.ui.Container are in unnamed module of loader com.codename1.impl.javase.ClassPathLoader @6f539caf)”  
> Just a question do I need to use specific version of net beans to run tests? I’m not sure what I’m missing here. Thanks
>



### **Shai Almog** — May 9, 2024 at 10:34 am ([permalink](https://www.codenameone.com/blog/test-it.html#comment-24609))

> Shai Almog says:
>
> You need to edit the tests after recording. The recording process is imperfect by definition and generates code that might fail and might be too fragile (e.g. fail on a different simulator/device).
>
> In this case it seems that the failure is triggered by code that shows a progress indicator for a long running task. When recording this wasn’t caught since we skip delays. Otherwise your code would be littered with “sleep” commands. The code is running too quickly and is then running into the InfiniteProgress component which has taken over the form. You need to wait within the test code for the InfiniteProgress to complete.


### **Kumuda Garlapati** — May 8, 2024 at 5:16 pm ([permalink](https://www.codenameone.com/blog/test-it.html#comment-24608))

> Kumuda Garlapati says:
>
> Hi Shai Almong, this is the test class that was recorded  
> pointerPress(0.5816994f, 0.7446808f, new int[]{0, 1, 0, 0, 0, 1, 0, 1});  
> waitFor(149);  
> pointerRelease(0.5816994f, 0.7446808f, new int[]{0, 1, 0, 0, 0, 1, 0, 1});  
> pointerPress(0.43809524f, 0.425f, new int[]{0, 1, 0, 2, 1, 2, 0, 0});  
> waitFor(202);  
> pointerRelease(0.43809524f, 0.425f, new int[]{0, 1, 0, 2, 1, 2, 0, 0});  
> return true;  
> Whenever I try to run encountered error”java.lang.ClassCastException: class com.codename1.components.InfiniteProgress cannot be cast to class com.codename1.ui.Container (com.codename1.components.InfiniteProgress and com.codename1.ui.Container are in unnamed module of loader com.codename1.impl.javase.ClassPathLoader @6f539caf)”
>



### **Kumuda Garlapati** — May 9, 2024 at 12:17 pm ([permalink](https://www.codenameone.com/blog/test-it.html#comment-24610))

> Kumuda Garlapati says:
>
> Thank you Shai Almong for the reply, I always worked with selenium, this is a new thing for me, where do i get documentation on how to code?
>



### **Kumuda Garlapati** — May 10, 2024 at 4:43 pm ([permalink](https://www.codenameone.com/blog/test-it.html#comment-24611))

> Kumuda Garlapati says:
>
> I’m able to record and run test cases, I really appreciate your help on this. thanks
>



### **Kumuda Garlapati** — September 5, 2024 at 2:11 pm ([permalink](https://www.codenameone.com/blog/test-it.html#comment-24633))

> Kumuda Garlapati says:
>
> Hi Shai Almong, I have a question as we are using test recorder and question is once I record all the tests and when running is it able to handle different screen resolutions?
>



### **Shai Almog** — September 6, 2024 at 3:02 am ([permalink](https://www.codenameone.com/blog/test-it.html#comment-24634))

> Shai Almog says:
>
> Not seamlessly. We try to generate “logical code” but can’t always detect that correctly.
>



### **Kumuda Garlapati** — September 6, 2024 at 3:05 pm ([permalink](https://www.codenameone.com/blog/test-it.html#comment-24635))

> Kumuda Garlapati says:
>
> Got it. thank you.
>



### **Kumuda Garlapati** — October 17, 2024 at 2:44 pm ([permalink](https://www.codenameone.com/blog/test-it.html#comment-24643))

> Kumuda Garlapati says:
>
> Hi Shai Almog, Sorry for bothering you, have a question on whether the .apk file provided to us by developers can be integrated with appium framework are there any limitations or is it compatible? as we have selenium framework for ui and want to have this application embedded into it for better management. Really appreciate your help on this.
>



### **Shai Almog** — October 19, 2024 at 5:23 am ([permalink](https://www.codenameone.com/blog/test-it.html#comment-24645))

> Shai Almog says:
>
> Hi,  
> We don’t have any support for Appium. It might be possible to build something like that or adapt existing APIs into a cn1lib but there was no effort in that direction.
>



### **Kumuda Garlapati** — October 21, 2024 at 12:58 pm ([permalink](https://www.codenameone.com/blog/test-it.html#comment-24647))

> Kumuda Garlapati says:
>
> Thank you for the reply, I will have conversation with developers.
>



### **Kumuda Garlapati** — January 7, 2025 at 4:50 pm ([permalink](https://www.codenameone.com/blog/test-it.html#comment-24669))

> Kumuda Garlapati says:
>
> Hi Shai Almong, We are looking for enabling accessibility id’s to work with android apk automation, Can you tell us how we can enable those in properties file? is there any documentation that we can refer. Thanks for your help.
>



### **Shai Almog** — January 8, 2025 at 2:50 am ([permalink](https://www.codenameone.com/blog/test-it.html#comment-24671))

> Shai Almog says:
>
> I’m afraid not. Accessibility on Android/iOS is one of our big open issues: <https://github.com/codenameone/CodenameOne/issues/426>
>



### **Gaurav P** — January 8, 2025 at 1:36 pm ([permalink](https://www.codenameone.com/blog/test-it.html#comment-24672))

> Gaurav P says:
>
> Hi Shai, is there any ETA we have at the moment for enabling accessibility on Android or iOS. as this is stopping us to perform UI testing on different platforms like iOS, Android, Windows or Mac. Kindly advise


### **Gaurav P** — January 8, 2025 at 1:38 pm ([permalink](https://www.codenameone.com/blog/test-it.html#comment-24673))

> Gaurav P says:
>
> Hi Shai, is there any ETA we have at the moment for enabling accessibility on Android or iOS. as this is stopping us to perform UI testing on different platforms like iOS, Android, Windows or Mac. Kindly advise
>



### **Shai Almog** — January 9, 2025 at 2:56 am ([permalink](https://www.codenameone.com/blog/test-it.html#comment-24674))

> Shai Almog says:
>
> Unfortunately no. It’s been an open issue for 9 years, I doubt that this will change unless there’s a major drive behind it. It’s a lot of deep effort so community attempts at fixing this even for one platform have failed.
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
