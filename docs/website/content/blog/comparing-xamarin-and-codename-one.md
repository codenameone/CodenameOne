---
title: Comparing Xamarin and Codename One
slug: comparing-xamarin-and-codename-one
url: /blog/comparing-xamarin-and-codename-one/
original_url: https://www.codenameone.com/blog/comparing-xamarin-and-codename-one.html
aliases:
- /blog/comparing-xamarin-and-codename-one.html
date: '2016-05-11'
author: Shai Almog
---

![Header Image](/blog/comparing-xamarin-and-codename-one/compare-to-xamarin.jpg)

Last time around we compared [Codename One to QT](/blog/comparing-qt-and-codename-one.html) and this  
time around I’d like to compare Codename One to the 800 pound gorilla: Xamarin. Xamarin is an amazing product  
that I [contrasted with Codename One in the past](https://forums.xamarin.com/discussion/55129/comparing-xamarin-to-other-cross-platfrom-frameworks-codename-one?)  
but this is worth repeating.

On it’s surface Xamarin might seem like a similar tool to Codename One using C# used instead of Java, but this  
is misleading as the tools are so different conceptually they have very little in common.

__ |  We updated this comparison after the initial publication to include the additional Property Cross section   
---|---  
  
### Background

Xamarin was founded by Nat Friedman and Miguel de Icaza famous for the GNOME desktop environment,  
Mono and Ximian.

Xamarin launched in 2011, during those years xcode was a very primitive IDE. It didn’t have ARC and had quite  
a few problems. Xamarin offered the ability to write native iOS apps in C# with garbage collection and the other  
great features allowed by C#.

Xamarin didn’t abstract the API much at the time and effectively provided mappings between C# and native  
API calls. It also produced a similar version for Android which did the same. In that sense Xamarin eschewed  
the write once run anywhere mantra in favor of a common set of business logic coupled with separate UI/native  
code/resources.

__ |  When we say “native code” in the Xamarin context that usually means C# code that uses the native OS API,  
the Codename One context of native code refers to actual OS native languages e.g. Objective-C on iOS   
---|---  
  
As the company grew it built the Xamarin Forms solution on top of the existing infrastructure which provides  
something closer to write once run anywhere. Since native widgets were used for Xamarin Forms this posed  
a problem as there are inherent insurmountable differences between the iOS/Android widget API’s.

#### Microsoft Purchase

Xamarin was purchased by Microsoft in February 2016 and integrated into visual studio. Most of  
it’s products were open sourced as part of this purchase.

### At a Glance

Category | Xamarin | Codename One  
---|---|---  
Language |  C# |  Java  
IDE |  Visual Studio/Xamarin Studio |  NetBeans, Eclipse or IntelliJ IDEA  
Cloud Build |  No (requires Mac & Windows for full OS support) |  Yes (can work on Linux for iOS development)  
Web Deployment |  No |  Yes  
Widgets |  Heavyweight |  Lightweight  
Portability Strategy |  Cross platform, sub projects |  WORA + Native Interfaces (one project)  
  
__ |  Xamarin requires a Mac for the iOS native app but you can develop on a Windows machine and have a Mac  
machine in your office to which Xamarin will connect to do the actual native work   
---|---  
  
### In Detail

#### Language & IDE

I’m not a fan of C# or visual studio but C# is a decent language as is Java especially with version 8. Since both  
languages have similar core concepts the differences come down to personal taste more than anything.

The same is true for Visual Studio vs. any of the popular Java IDEs (Eclipse, IntelliJ & NetBeans). These are all  
mature, powerful IDE’s that include anything you might need.

##### Native Language

While as a language C# might be a decent option, it is an alien to the two leading mobile platforms.

Android’s “native” language is Java. The UI widgets in Android are implemented in Java, which effectively means  
that if you write code in C it will perform slower than Java code as it would need to pass thru JNI. C#  
code can’t be implemented on top of the current Android VM and effectively needs to pass thru JNI  
back and forth repeatedly.

This overhead is very noticeable when working with heavyweight widgets as the communication between the widget  
and the logic needs to be very frequent. In that sense C# is less native than Codename One which is just implemented  
directly on top of the native layer.

On iOS the story is different, Xamarin implements the whole toolchain effectively hiding Apples tools  
completely. This is a very powerful abstraction but it sometimes creates another layer of complexity. E.g. when  
Apple introduces a new idea such as bitcode or a new profiling tool Xamarin can’t fully leverage such a tool. It has  
it’s own set of tools but they will always be second class citizens on Apples platform.

Codename One uses the open source [ParparVM](https://github.com/codenameone/CodenameOne/tree/master/vm)  
to translate Java bytecode to a native C xcode project on Mac OS. This effectively creates a native iOS project  
and allows you to write native Objective-C code write into that project. In a sense this is “more native”as it ends  
up using a greater portion of the “officially supported toolchain”.

Xamarin isn’t needed on Windows as Microsofts native tools can be used to provide portability to that platform, there  
Microsoft is the true native leader by definition.

#### Cloud Build

Codename One’s cloud build capability is unique. It allows developers to build a native application using the  
Codename One cloud servers (Macs for iOS, Windows machines for Windows etc.) This removes the need to  
own dedicated hardware and allows you to build native iOS apps from Windows and native Windows apps  
from your Mac or Linux machine.

This makes the installation of Codename One trivial, a single plugin to install. This isn’t true for development  
environments that don’t use that approach.

Xamarin is far more “low level” than Codename One. Xamarin assumes you have deep platform knowledge e.g.  
you have to understand the Android activity API and lifecycle in order to build a Xamarin Android app. You need  
to understand the `ViewController` to build a Xamarin iOS app. You don’t need  
to understand either one of those in order to build a Codename One application.

Both platforms allow access to the underlying native code with a different underlying philosophy as explained below.

__ |  Knowledge of the native platform is less essential with Xamarin Forms but is still expected and deemed as  
an advantage by the Xamarin team. This highlights the core conceptual/philosophical differences between the platforms   
---|---  
  
#### Web Deployment

Xamarin supports almost all of Codename One’s supported platforms either directly or thru Microsofts Visual  
Studio tools. However, it doesn’t support building native JavaScript web applications.

Codename One supports the process of compiling an application (threads and all) into a JavaScript application  
that can be hosted on the web. This is done by statically translating the Java bytecode to JavaScript obfuscated  
code.

__ |  There are some early stage efforts to bring XAML to JavaScript but none are at a mature stage of supporting  
complex notions like threads   
---|---  
  
#### Widgets

This is probably the biggest difference between Xamarin and Codename One.

Xamarin exposes the underlying UI API completely to the developer. This is exposed so completely that a developer  
can literally use the native iOS/Android visual design tools to build layouts (that naturally **won’t be portable**).

Codename One uses a lightweight widget approach where it draws the component hierarchy but allows embedding  
of native widgets for specific requirements (HTML, media, text etc.) this. The difference between these two approaches  
is highly documented and has been debated since the days of smalltalk (think Swing vs. SWT/AWT).

Heavyweight architecture is closer to the way the native OS behaves and as a result is inherently less portable  
and not as flexible. This is a tradeoff that some developers are willing to accept in order to be “more native”.

Heavyweight is sometimes deemed “faster” by its proponents but the technological basis for such claims is flawed as  
these widgets are harder to measure realistically across platforms and optimize properly.

##### Xamarin Forms

Xamarin also supports Xamarin Forms which uses XAML to allow sharing most UI code between various platforms  
by picking the lowest common denominator approach to the UI. Unlike Codename One the Xamarin Forms approach  
still uses native code and assumes some native access.

It isn’t designed as a complete WORA (Write Once Run Anywhere) solution but rather as a middle ground solution.

Notice that with Xamarin Forms you will not be able to use some native OS capabilities such as the native OS  
GUI builders for obvious reasons.

#### Portability Strategy

Codename One uses a single project that works everywhere. When you need access to native code this native  
code is hidden by the native interfaces abstraction that allow that single project to remain “clean” of native code.  
By default no native code of any type is necessary to build a Codename One application.

Xamarin requires a “pseudo native” project (still written in C#) to represent the lifecycle, resources and other elements  
of the various supported platforms. By default this will include a lot of the platform specific code such as UI etc.  
with the exception of Forms apps where there will be less code in the separate projects.

These might seem like small differences but they hide a major core difference. Codename One tries to abstract the  
aspect of platform native differences and Xamarin pushes it to the forefront.

#### Microsoft

A big selling point for Xamarin is the Microsoft acquisition positioning it as a major player backed by the full weight  
of Microsoft. Microsoft has repeatedly abandoned technologies in which they invested a great deal of money  
e.g. Windows Phone 7, Silverlight etc. It also demonstrated this recently by discarding RoboVM without the  
curtesy of opening its source code.

Xamarin is free in order to gain traction and serve Microsofts market goals. As long as those goals align with  
the goals of developers using Xamarin this is a good thing. However, since this is a market MS competes in  
it is not an impartial player.

No one can tell at this time whether the purchase will have a negative effect on the future development of Xamarin.  
So far the Microsoft purchase has been largely a positive thing with the exception of the RoboVM issue.

### Property Cross Comparison

The PropertyCross demo was built as a tool that allows us to compare two cross platform frameworks, as such  
there are versions of the demo for many such platforms. You can check out details of the Codename One  
implementation [here](/blog/property-cross-revisited.html). The github repository for this demo  
is [here](https://github.com/codenameone/PropertyCross/).

Xamarin has two cross platform implementations for property cross a  
[regular app](https://github.com/tastejs/PropertyCross/tree/master/xamarin) and a  
[mvvm version](https://github.com/tastejs/PropertyCross/tree/master/xamarinmvvmcross).  
Both don’t use Xamarin Forms which is a shame since it would probably be closer to what we offer with Codename  
One but it’s still a good overview of the differences.

Lines of code are a pretty bad measurement of the overall quality of a framework, however having said that they  
do provide some indication of the verbosity and Xamarin is far more verbose than Codename One. E.g.  
looking at the  
[MVVM model implementation alone](https://github.com/tastejs/PropertyCross/tree/master/xamarinmvvmcross/PropertyCross.Core/ViewModels)  
we can see that it has more lines of code than the entire Codename One implementation!

The Xamarin implementations are really 4 implementations for iOS, Android, Windows Phone & a portable common  
project. This effectively means that the Xamarin implementation requires a deep understanding of the native platforms  
since it’s in essence native programming.

__ |  The Xamarin Forms implementation would still require such a structure although a greater portion of the code  
would reside in the common project   
---|---  
  
This also means you will need to either use the iOS tools to build a UI or deal with the screenshots, icon resolutions  
& DPI changes for the various device types. E.g. on iOS you would need images using the `@` notation but on  
Android you will need to divide the images to the DPI directories creating a lot of work on resource maintenence  
which just doesn’t exist in Codename One.

In that sense PropertyCross is probably a poor demo as it doesn’t contain many resources that need maintenence.

### Final Word

As usual our opinions are biased but I think we presented a reasonably fair evaluation. Xamarin is an  
impressive tool that is quite different inherently from Codename One & we hope we highlighted those  
differences fairly.

If you are looking to write a native application you like C# & don’t mind dealing with platform differences it is a  
compelling option. However, if you prefer Java and portability is key I think we have a far more compelling story.

If you think we misrepresented Codename One or Xamarin in any way let us know in the comments.

Feel free to use the comments section for suggestions of future comparison segments.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **J Master** — May 13, 2016 at 9:50 am ([permalink](https://www.codenameone.com/blog/comparing-xamarin-and-codename-one.html#comment-22538))

> J Master says:
>
> Interesting, less than 300 stars for a github compare to 32,970 developers? Can it be compile to Swift code instead of C?
>
> 120,000,000 is based from which figure?


### **Shai Almog** — May 14, 2016 at 3:19 am ([permalink](https://www.codenameone.com/blog/comparing-xamarin-and-codename-one.html#comment-22663))

> Shai Almog says:
>
> We migrated from Google code at the last possible instance which explains the low star count.
>
> By default we include device counting code in builds (this can be disabled) so we composed that number from verified installed which we then correlated to actual appstore installs and to apps/devices where we know this was developed to come up with a composite number. The number is actually much larger by now but we didn’t get around to go thru this process to update it.


### **Siva Mamidi** — June 2, 2016 at 8:32 pm ([permalink](https://www.codenameone.com/blog/comparing-xamarin-and-codename-one.html#comment-22757))

> Siva Mamidi says:
>
> Very nice article and informative.


### **Benjamin Hamilton** — June 18, 2016 at 1:10 am ([permalink](https://www.codenameone.com/blog/comparing-xamarin-and-codename-one.html#comment-22613))

> Benjamin Hamilton says:
>
> From your article  
> Android’s “native” language is Java. The UI widgets in Android are implemented in Java, which effectively means that if you write code in C it will perform slower than Java code as it would need to pass thru JNI.
>
> From Google  
> [https://developer.android.c…](<https://developer.android.com/training/articles/perf-jni.html>)
>
> JNI is the Java Native Interface. It defines a way for managed code (written in the Java programming language) to interact with native code (written in C/C++). It’s vendor-neutral, has support for loading code from dynamic shared libraries, and while cumbersome at times is reasonably efficient.


### **Shai Almog** — June 18, 2016 at 4:56 am ([permalink](https://www.codenameone.com/blog/comparing-xamarin-and-codename-one.html#comment-22941))

> Shai Almog says:
>
> That’s a common misconception. Check out this article from Chet & Romain two brilliant ex-Sun guys who are very deep in the Android GUI system development: [https://realm.io/news/romai…](<https://realm.io/news/romain-guy-chet-haase-developing-for-android/>)
>
> Copied from the source article:
>
> Avoid JNI
>
> Sometimes you need JNI, but not if  
> you’re just using it out of convenience. Something interesting about JNI  
> code: every time you cross the boundary between Java runtime and native  
> there’s a cost because we need to validate the parameters and it has an  
> impact on the GC behaviors. It can be pretty expensive, so when you do a  
> lot of JNI calls, you might spend more time in the overhead of JNI than  
> in the actual code. If you have old JNI code, you might want to revisit  
> it.


### **Marcin** — July 15, 2016 at 1:28 am ([permalink](https://www.codenameone.com/blog/comparing-xamarin-and-codename-one.html#comment-22793))

> Marcin says:
>
> Hello Shai, how about the Dependency Injection with Codename One?
>
> I tried to search for spec, examples or even searched for phrase “@Inject” through the Github examples.  
> Is it possible to decouple code this way?
>
> Are there any other decoupling methods available like EventBus?
>
> It would be also really great to have CDI like producers, I mean annotations @Produces . Also some scopes…  
> Is it possible with Codename One?
>
> And finally how about writting testible code with Codename One?  
> It looks like I have to use lot of singletons like Display.getInstance()… or FaceBookAccess.getInstance()…. or static calls like LocationManager.getLocationManager().getCurrentLocation().  
> Could I simply inject those instead? Something like @Inject LocationManager?  
> Singleton objects definiton could be marked with @Singleton annotation. Unfortunately I don’t see it there ([https://goo.gl/UhJDSY)](<https://goo.gl/UhJDSY>)).  
> The reason I wan’t to inject this stuf is that I prefer simple UnitTests over long running Integration Tests.
>
> Regards,  
> Marcin


### **Shai Almog** — July 15, 2016 at 4:00 am ([permalink](https://www.codenameone.com/blog/comparing-xamarin-and-codename-one.html#comment-22801))

> Shai Almog says:
>
> Hi,  
> no we don’t support DI since we don’t support reflection and bytecode manipulation isn’t trivial since it must be done statically with great care. It’s possible to do bytecode manipulation as Steve did with the POJO mapper: [http://www.codenameone.com/…](<http://www.codenameone.com/blog/json-to-pojo-mapping.html>) but no one did it for DI.
>
> We don’t support JUnit etc. so you will need to use our test framework which includes a test recorder etc. I think a lot of the problems solved by DI are server problems and not as pertinent to the front end developer landscape.


### **Martin Grajcar** — September 9, 2017 at 1:26 am ([permalink](https://www.codenameone.com/blog/comparing-xamarin-and-codename-one.html#comment-23350))

> Martin Grajcar says:
>
> Dagger 2 uses no reflection at all, so in theory it must work. As this is the only relevant search result for “codenameone” and “dependency injection”, I’m afraid that nobody has really tried it yet.


### **Shai Almog** — September 9, 2017 at 7:13 am ([permalink](https://www.codenameone.com/blog/comparing-xamarin-and-codename-one.html#comment-23502))

> Shai Almog says:
>
> Steve did POJO mapping which is pretty similar, if you use bytecode manipulation tools it can work but DI isn’t as helpful in mobile client as it is in server code so there wasn’t as much demand.
>
> If you try it and run into difficulties just ask on stackoverflow (with the codenameone tag) or in the discussion forum. We’ll try to help.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
