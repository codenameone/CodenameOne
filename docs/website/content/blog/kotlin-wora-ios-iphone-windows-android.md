---
title: Kotlin WORA for iOS (iPhone), Windows & Android
slug: kotlin-wora-ios-iphone-windows-android
url: /blog/kotlin-wora-ios-iphone-windows-android/
original_url: https://www.codenameone.com/blog/kotlin-wora-ios-iphone-windows-android.html
aliases:
- /blog/kotlin-wora-ios-iphone-windows-android.html
date: '2017-06-12'
author: Shai Almog
---

![Header Image](/blog/kotlin-wora-ios-iphone-windows-android/kotlin_800x320.png)

We received some interest related to Kotlin over the past couple of years and this has risen noticeably in the past month or so. Up until now we tried to be very focused on Java which is why we didn’t add support to other JVM languages even though this shouldn’t be too hard. But Kotlins similarity to Java and its special relationship to Android make it an ideal second language for us.

We plan to implement Kotlin support in the “right way” and offer all the Codename One supported platforms as it would just be a Codename One project for our build process. This should work in a similar way to the Android support by allowing you to mix Kotlin sources and Java sources in a single project.

Right now the beta is planned in the 3.8 time frame. The 3.8 release is planned for December so this will probably be out sooner.

Obviously this work will be open source and a part of our project just like the rest of Codename One.

### What do we Need from You?

We don’t announce features so far ahead under normal circumstances but we decided to make an exception here as we will need alpha testers and we would like some feedback before we begin prototyping.

If you use Kotlin or are interested in picking it up we’d appreciate a few minutes to [fill out this survey](https://goo.gl/forms/rC5pAMZyDTZFuaKH3). You can also signup for the alpha of this support if you are interested.

Let us know what you think in the comments.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **kutoman** — June 13, 2017 at 2:20 pm ([permalink](/blog/kotlin-wora-ios-iphone-windows-android/#comment-24134))

> kutoman says:
>
> great news!
>



### **Chad Elofson** — June 13, 2017 at 6:17 pm ([permalink](/blog/kotlin-wora-ios-iphone-windows-android/#comment-24133))

> Chad Elofson says:
>
> I would be interested in helping out.
>



### **Don't Bother** — June 13, 2017 at 7:31 pm ([permalink](/blog/kotlin-wora-ios-iphone-windows-android/#comment-23457))

> Don't Bother says:
>
> Frankly speaking I don’t understand how this will help you to increase adoption of C1. I mean it is still not possible to debug C1 app on iPhone and java.time is not available for C1. There are other features I would consider as more important but this is only my personal point of view.
>



### **Shai Almog** — June 13, 2017 at 7:50 pm ([permalink](/blog/kotlin-wora-ios-iphone-windows-android/#comment-22593))

> Shai Almog says:
>
> Thanks!
>



### **Shai Almog** — June 13, 2017 at 7:59 pm ([permalink](/blog/kotlin-wora-ios-iphone-windows-android/#comment-23591))

> Shai Almog says:
>
> You can debug on device with xcode (the generated source) which I do a lot and it’s relatively easy. We actually planned to release on device debugging for 3.7 (from Java IDE’s) but it got delayed. On device debugging is a feature that would probably take far more manpower than kotlin support. Java time is on my personal wish list too but it’s pretty low, if you want it just port this which should be pretty trivial: [https://github.com/JakeWhar…](<https://github.com/JakeWharton/ThreeTenABP>)
>
> Far more important work IMO is working on good looking UI by default and better tutorials/guides. I’m currently spending most of my time on both of those things which is why Kotlin is scheduled for later and not right now. It’s also why on device debugging isn’t getting out.
>



### **Nick Apperley** — June 14, 2017 at 1:20 am ([permalink](/blog/kotlin-wora-ios-iphone-windows-android/#comment-23560))

> Nick Apperley says:
>
> Since Kotlin support will be added eventually it will be EXTREMELY IMPORTANT that the support is Kotlinic (idiomatic Kotlin/the Kotlin way of doing things). Highly recommend getting into contact with Vert.x ([http://vertx.io/)](<http://vertx.io/>)) on how to add Kotlin support to a Java project. The head of the Vert.x project is Julien Viet who occasionally hangs out in Kotlin Slack on the vertx channel. Also note that Vert.x use a documentation generation system (is language agnostic), which can also generate code examples in the documentation.
>
> Pivotal would also be another company to contact in relation to their extensive use of Kotlin, on the server-side (especially micro services) and Android throughout the company since around 2015. Currently Pivotal is the biggest Enterprise adopter of Kotlin who are similar in size to Google. Sébastien Deleuze heads the Kotlin group at Pivotal and can be found hanging out in Kotlin Slack on the spring channel.
>



### **Shai Almog** — June 14, 2017 at 4:06 am ([permalink](/blog/kotlin-wora-ios-iphone-windows-android/#comment-21468))

> Shai Almog says:
>
> Thanks. I’m not versed enough in Kotlin beyond the basic tutorials but won’t we get it implicitly by going thru the Kotlin compiler to bytecode and adding support for the Kotlin libraries?
>
> Since the starting point in Java the effort seems relatively small.
>



### **Don't Bother** — June 15, 2017 at 4:23 am ([permalink](/blog/kotlin-wora-ios-iphone-windows-android/#comment-23450))

> Don't Bother says:
>
> Completely agree about nice looking UI. And kotlin support is not bringing any improvements in this area.  
> I know that I can debug generated c code. However in this case the whole point of C1 is lost imho. I mean if I am comfortable with xcode and c++ I may not need another language and tools(c1). And I believe that on device debugger is more complex. My point is that adding features like kotlin support (which is not a deal breaker at all at the moment) you keep pushing on device debugger and UI improvements more and more. And imho absence of on device debugger is a deal breaker for some cases.
>



### **Shai Almog** — June 16, 2017 at 6:58 am ([permalink](/blog/kotlin-wora-ios-iphone-windows-android/#comment-23569))

> Shai Almog says:
>
> You lose a lot of the point of cn1 when you reach the actual need to debug on the device… It’s there for emergencies and should be used very rarely if at all. That’s why we have an enterprise account so when an enterprise developer runs into an issue on device we can help with that.  
> Notice that when you debug the C code in xcode it’s still Java. You can see the line numbers of the Java source and map that directly, you can see the stack traces and get a picture of what failed and you can use the wonderful xcode profiling tools and see which Java method is taking up CPU/GPU/RAM. The thing is you don’t need to know C or objective-c to still get a lot from the debugger in xcode and Codename One.
>
> I understand your point, but it’s flawed…
>
> 1\. It assumes that if we didn’t do this then the other things we can do would be delivered quicker. That’s probably not the case. Different people, different complexities and tasks.
>
> 2\. It assumes that what’s important to you is the same as other developers. A lot of our developers are Android developers looking to migrate existing code to iOS. The moment Kotlin became an official language they want to know there is a path.  
> Since Swift and Kotlin are very similar this is something we need to support to prevent leakage.
>
> I’m sorry if an on-device-debugger is a deal breaker for you. Unfortunately people aren’t willing to put their money where their mouth is and help support projects so they can move in the direction that they want. I’m not saying that you are like that… but here’s the thing:
>
> Every single time someone said to us: “I like Codename One but I’ll use it if you just add feature X”. That person didn’t back that up…
>
> We actually conducted an experiment, we picked a big feature that a lot of people asked for and got written commitments from developers saying they would upgrade if we add that feature.
>
> Not a single developer upgraded, not one!!!
>
> When someone says “I will buy your product if…” it’s bullshit. You might be the exception shining star on the hill that will actually take up Codename One when we do finally add on device debugging but I have no way of knowing it. So we implement features based on market research and feedback from people who are paying.
>
> Please don’t be offended by this, I don’t know who you are so I have absolutely no way of qualifying your statements. It’s business practices we had to learn the hard way when running the company.
>



### **Don't Bother** — June 16, 2017 at 2:19 pm ([permalink](/blog/kotlin-wora-ios-iphone-windows-android/#comment-21586))

> Don't Bother says:
>
> No worries. First of all I never wrote you that if you have feature x I will do something :-). I wrote that for some people including me ability to debug actuall application on real device is a serious concern.
>
> Looks like you got offended. Yes, I wrote critical remarks but I was hoping that it will not offend you. Sorry if it did.
>



### **Shai Almog** — June 17, 2017 at 7:03 am ([permalink](/blog/kotlin-wora-ios-iphone-windows-android/#comment-23598))

> Shai Almog says:
>
> No, I’m sorry. I’m just exhausted and sometimes my prickly personality comes out in a bad way.
>
> Not offended and I appreciate your taking the time. I do agree with most of your points 😉
>



### **Tom Tantisalidchai** — June 17, 2017 at 7:30 am ([permalink](/blog/kotlin-wora-ios-iphone-windows-android/#comment-23355))

> Tom Tantisalidchai says:
>
> Awesome news!
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
