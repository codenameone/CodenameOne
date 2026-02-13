---
title: 'The Road Ahead: 2015 In Review & Survey Results'
slug: the-road-ahead-2015-in-review-survey-results
url: /blog/the-road-ahead-2015-in-review-survey-results/
original_url: https://www.codenameone.com/blog/the-road-ahead-2015-in-review-survey-results.html
aliases:
- /blog/the-road-ahead-2015-in-review-survey-results.html
date: '2016-01-03'
author: Shai Almog
---

![Header Image](/blog/the-road-ahead-2015-in-review-survey-results/looking-forward.jpg)

I think we’ll remember 2015 as the year we finally “hit our stride” and “got it”. Up until now we did a lot of  
things correctly but were a bit disorganized both in the way we communicated Codename One and in the  
refinement of the product itself.   
We changed and refined almost every piece of [Codename One](/) in 2015 and the results were  
great. But we want to do better so here is my analysis of what we did wrong (both in 2015 and before) and  
what we did right. Followed by the initial survey results and feedback from we got from you guys and our  
2016 direction. 

#### What We Did Wrong

I worked for a company that built flight simulators for EA in a previous lifetime. We had about 20 airforce pilots  
on staff and they brought the wonderful culture of “debrief” to the development process. Every time we  
did something we’d analyze what went bad and what went well so we can learn. I think focusing on the bad  
first really helps drive that point thru! 

  * [Windows Phone Support](/blog/invite-friends-websockets-windows-phone-more.html) –  
we have huge problems here. Relying on Microsoft to add the Android support  
was a mistake and we need to fix this as soon as we can. 
  * IntelliJ/IDEA Support – Our current support for IntelliJ/IDEA is by far the worst. Because the code for every  
IDE plugin is separate fixes/enhancements should be made 3 times and currently we only do them twice.  
This is made worse by the fact that IntelliJ is used by an excellent demographic  
that is getting a horrible first impression of Codename One!  
One of the roads we are thinking about is moving away from Ant to support a more IDE neutral architecture.  
I don’t like Maven much and Gradle seems to also have its issues but if we can unify more IDE specific code  
that could be a huge difference.
  * Unclear messaging – our website and materials are still unclear and out of date. E.g. there is very little  
in the form of introduction to Codename One. A lot of the videos are stale by now and need newer versions.
  * Build failures – builds still fail and produce unclear error messages. We need to improve that process  
so people don’t hit a wall. 
  * Easier migration – currently moving from Android/Swing still requires adapting a lot of API’s. While zero  
changes is probably an unattainable goal we should still make that process less painful.
  * Debugging – we still don’t have on-device debugging which is something we can and probably should offer.

#### What We Did Wrong & Fixed In 2015

We made a lot of changes in 2015 that fixed long running problems: 

  * [Moved to stack overflow](/blog/stackoverflow-cordova-update-validation-text-input-hints-icon.html)  
– I had my reservations about this but its a HUGE improvement in every regard.  
It also helped our SEO efforts and our general visibility.
  * [Bad design by default](/blog/good-looking-by-default-native-fonts-simulator-detection-more.html) –  
a default hello world application looked horrible in Codename One. The default  
fonts were also pretty horrible. Material design and other common paradigms are still not properly addressed  
out of the box in Codename One. We are working on filling the gap here and cosider these to be important priorities.
  * [Fast release cycle](/blog/smooth-scrolling-async-android-editing-3.3.html) –  
we procrastinated on releases prior to 2015. This made basic features like versioned  
build completely useless. Shortening the release cycles made them easier as each release became simpler.
  * [We failed to communicate that we are open source](/blog/how-to-use-the-codename-one-sources.html)  
– a lot of people misunderstood Codename One’s source  
code strategy and we only have ourselves to blame. We were pretty opaque about some of these things and  
didn’t properly highlight our code on the site.
  * Moving to github – we had a lot of reservations about this move. Ultimately we are thrilled with github  
and it really simplified a lot of things for us. Its a far better venue than Google code.
  * Scroll performance – for quite a few years we got complaints about janky scrolling behavior in Codename One  
when compared to native iOS apps and recently also to Android apps. Since the complaints were vague  
we just left them at that as they were pretty hard to investigate (too subjective). We recently made a big  
push here both on iOS & Android. The end result is much smoother and far more “native”.
  * General performance – the initial release of ParparVM wasn’t as performant even when compared  
to XMLVM for some use cases. Thanks to many commits made by Steve and myself this has changed  
drastically!  
ParparVM will probably never beat the top of the line AOT VM’s in microbenchmarks since it isn’t designed to  
do so, we pay a performance penalty to remove the GC overhead/stalls. But its now far more competitive  
for many common use cases. Optimizing ParparVM is relatively easy because of its ridiculously simple  
architecture that doesn’t require prior VM experience.
  * Blogging – in 2015 I invested more time blogging and this resulted in an amazing traffic boost to the site  
and far more awareness of Codename One. In the past we used to get a lot of traffic from Slashdot/HN/reddit  
but we stopped working at that circa 2013… This year I wrote the two most popular (ever) dzone mobile articles and  
the second most popular Java dzone article. I got quoted in multiple outlets and this ultimately resulted in great  
traffic for Codename One.
  * Moved away from app engine – while the move isn’t complete we moved away the bulk of the functionality  
and a heavy weight was lifted from our wallet. 

#### What We Did Right

We released a lot of new things this year that are brand new, here are some that we are most proud of. 

  * Java 8 support – we went into this not expecting much and it blew us away. This worked better than we expected.
  * ParparVM – when we started writing our own VM we weren’t all in agreement that this was the right way to go.  
When we released it the bugs were significant and very hard to iron out. As we moved forward it matured  
to a product we are really proud of. Steve came up with the idea of rebranding the VM and Chen came up with  
the name which ultimately was one of the best decisions we made this year. 
  * JavaScript Port – the JavaScript Port has proved to be a really useful tool for many different purposes. Our  
ability to instantly demonstrate an app with no installation requirement is pretty darn amazing.. 
  * Certificate wizard – this was a “killer feature” the one big missing piece here is appstore 
  * Toolbar – when we launched Codename One the title area functionality looked totally different between  
the mobile OS’s. In the interim this slowly converged to something far more similar. The Toolbar API closes  
the gaps between the OS differences while bringing with it pretty advanced features…
  * URLImage/FontImage – both of these really changed the way we write Codename One apps/demos.  
We feel we need to redo all the demos based on font image.

#### What You Guys Said

We posted a link to our  
[  
annual developer survey](https://docs.google.com/forms/d/1XltDhMS2Jlz7yVtiM6X7xkE-fF_eJeylzXXDd1eQtws/viewform), if you haven’t filled it out yet then please do so right now. Its very unscientific  
but it gives us a sense of how we are doing both in communicating what Codename One has and doesn’t have. 

The two most important questions were first: “Which of the following is the most important to you?”  
To which you answered: 

![Which of the following is the most important to you?](/blog/the-road-ahead-2015-in-review-survey-results/which-of-the-following-is-the-most-important-to-you.png)

The next question was: “Which of the following is the second most important to you?”  
To which you answered: 

![Which of the following is the second most important to you?](/blog/the-road-ahead-2015-in-review-survey-results/which-of-the-following-is_the-second-most-important-to-you.png)

To me these results are quite interesting as they show some interesting patterns e.g.: 

  1. I would have expected on-device-debugging to be really important to some developers but its not  
even mentioned as the first order priority.
  2. Had I wrote the survey now I would have added “which Java language features are you missing”… I have  
a strong sense that pretty much everyone who answered that wanted something different from the other guys  
who picked that option.
  3. The new GUI builder is an important advancement and we need to finish it for 3.3.
  4. Better Windows Mobile support is probably almost as important as the new GUI builder

Another interesting question was “What form of documentation/help do you value the most?”. The response  
for this took us by a bit of a surprise: 

![What form of documentation/help do you value the most?](/blog/the-road-ahead-2015-in-review-survey-results/what-form-of-documentation-help-do-you-value-the-most.png)

I would have expected the videos to be somewhat higher on the list, the “other” segment mostly refers to the forum  
but that’s not quite documentation. Removing that it seems the clear majority shows a preference for written  
documentation over videos/courses…  
Our developer guide is probably the least refined part of our documentation as we were focusing more on  
video/blog entries. I prefer working with videos sometimes as they allow me to gauge viewer interest more  
accurately and are easier to promote, but it seems we need to put more effort into the developer guide. 

The comments were probably some of the most interesting parts. I selected a few to both highlight and comment  
on, if you have additional thoughts about this I’d love to hear them in the comments section to this post: 

> Support more languages (e.g. Kotlin), support Gradle, support Jar libraries on public repositories (e.g.  
>  Mavencentral, JCenter, etc) perhaps with some checking to see whether it uses any unsupported api or stuff then  
>  providing a way for the developers to add those missing bits.

JVM languages are very interesting, [Steve explored Mirah](/blog/mirah-for-codename-one.html)  
quite a while back and it is something we think could be interesting. Having said that I think keeping focus  
at this stage is crucial so this (at this time) isn’t a priority. However, if community members want to port  
a language on top of Codename One or ParparVM we’d be happy to try and help in any way we can. 

I don’t like Maven and from dealing with Gradle recently it doesn’t seem to have fixed all the things broken  
in Maven. However, its more flexible and might be a good direction moving forward. The main motivation would  
be in improving the intellij/IDEA integration and ideally having more common code between all IDE’s.  
Currently Gradle isn’t as mature as Maven and Maven is a bit too rigid for what we are trying to do so we don’t  
have any concrete plans here…  
Getting dependencies from Maven central might be a problem. Most standard libraries won’t work  
out of the box since most assume a lot of things about Java (e.g. filesystem/networking etc.) that we just don’t  
support. They also don’t support cn1lib’s which are pretty powerful.   
Having said that, we would still like to add something like “maven central” to Codename One eventually, we’re  
just unsure whether maven central itself can actually be used for something like this. 

> Codename One is getting better all the time anyway. I realise that reflection is challenging but RoboVM  
>  manages it. It would make a big difference to simplifying some of the more complex data driven apps that I  
>  have. Headless JUnit testing for apps, channels for push notifications. Google maps in the simulator. 

I agree that we need better headless testing but probably not JUnit. We need something to run on devices  
and ideally offer device farm support so you can instantly test your app (ideally with CI) on actual devices.  
The reason we don’t support reflection isn’t technical. Its REALLY easy to add reflection. The  
reason is conceptual. We won’t be able to properly optimize/obfuscate the code, reflection is remarkably  
slow for AOT invocations and has no critical use case in mobile. Tools that add reflection also require that you  
import everything into the final build resulting in 100mb+ build results. That isn’t a smart thing to do…  
We could add something like reflection and just proclaim “if you use it then its your problem”, unfortunately once  
something like that exists people use it “unaware” because its a part of the API. Then we get the “blame” for  
the resulting problems. 

Having said that, I don’t want to rule anything out completely. We might add something that’s reflection “like”  
or even reflection itself to support various cases. The problem is avoiding the “link everything” end result that  
comes with reflection. 

> Move away from visual builder – it’s always more restricted than coding. Move towards more of a  
>  markup (XML, CSS) format.

Steve introduced [CSS support](/blog/rounded-corners-shadows-and-gradients-with-css.html) a while  
back and the new GUI builder really just uses XML. We didn’t see that much activity around the CSS support  
so its hard for us to gauge interest here but it seems low. 

The plugin for CSS was meant to gauge community interest, if there is none then the effort of integrating CSS  
directly is probably not worth it. However, if this would pick up and gain traction we might make it an official  
part of the toolchain. 

FYI other comments (not mentioned here) asked for more GUI builder samples and other related features  
(GUI builder was the second highest RFE). So these sort of things are very subjective especially when covering  
a varied community as we have in Codename One. 

> Codename One has a lot of quirks, its taken a fair bit of time to learn and work around the niggly  
>  little differences between performance and rendering on different platforms. Things like screen flicking on  
>  Android with native components and SQLite issues on iOS. Apart from these issues its a great system for  
>  getting cross platform apps together.

I pretty much agree with everything you’ve said. We need a better story for database storage, peer components  
and on-device issues. 

> Like: the GUI library, true WORA. Dislike: performance on iOS and to some extent on WP

We’ve done a lot of work on performance on iOS (see above) which should put both ParparVM and the Codename One  
port for iOS/Android at a whole different level in terms of performance.  
The Windows Phone/Mobile port should be rewritten from the ground up, its an important task in our short term  
goals. 

> I feel you go too fast sometimes, need to ensure stability before releasing new version (regressions  
>  stop our development)

Agreed. We need a better QA process.  
Notice that for pro accounts we have the stabler [  
versioned build](/how-do-i---get-repeatable-builds-build-against-a-consistent-version-of-codename-one-use-the-versioning-feature.html) option. 

> Getting started was a challenge.

I’d love to hear more on that but this was the only part of the comment submitted. The initial onboarding is probably  
the most crucial part in any tool. 

#### The Plan For 2016

Based on the above and general feedback we get continously from developers we would like to focus on the following  
thru 2016. The highest priority bits would be: 

  1. GUI builder – get it out the door and into the default projects. Hello world app should be a GUI builder app
  2. Windows Phone/Mobile – Leverage the excellent work done by Fabricio et. al to get Windows back into  
a first class citizen platform.
  3. Performance/Native – improve and refine the performance of Codename One to make it indistinguishable from native.  
I’d also like to improve the way peer components act across platforms.
  4. Design – we need Codename One apps to be gorgeous out of the box. The first impression is crucial and  
we currently botch it. Its much better than it was in 2014 but I think we can do a lot more!
  5. Documentation & Samples – that’s always crucial but I’d like to make these a top priority.

There are a lot of other priorities I’d like to see going into Codename One. I’d like to see us leverage the JavaScript  
port more, I’d like us to offer a full ALM solution (automatic appstore upload as part of a CI process). I’d like  
to see on device debugging & better security options (encrypted filesystem/DB etc.). 

Let us know what you think both in the comments and thru the discussion-forum/support. Even if we say no initially  
we often shift our opinions on things as we move forward and if we get enough feedback from the community.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Diamond** — January 4, 2016 at 3:34 pm ([permalink](https://www.codenameone.com/blog/the-road-ahead-2015-in-review-survey-results.html#comment-22475))

> Diamond says:
>
> That’s a great success story for 2015.
>
> I would be commenting only on CSS support for codename one…
>
> I tried it out and realised that it generates additional 9-piece border images for my app, which increases the app size and in turn slows down the performance.
>
> It would have been great if it could really create true styling like css does on webpages without generating images.  
> That’s why I stopped using it and I think that’s why other developers don’t really fancy it that much.
>
> On a separate note, As codename one is open source, I think you should devise a way to attract more code contributions by all developers. As most of us create some cool features and unique codes but never contribute them back. Look for a way to promote the culture.


### **Shai Almog** — January 4, 2016 at 6:27 pm ([permalink](https://www.codenameone.com/blog/the-road-ahead-2015-in-review-survey-results.html#comment-22647))

> Shai Almog says:
>
> Thanks, that’s great feedback. I doubt CSS will ever be like it is on a web page but we do need to improve some of the border API’s to be more performant and customizable.
>
> Very few open source projects get significant code contributions so we’re not counting on that as much as we hope for small bug fixes/pet-peeves from developers. E.g. this was a wonderful contribution: [https://github.com/codename…](<https://github.com/codenameone/CodenameOne/pull/1622>)
>
> Just a bunch of fixes for my horrible grammar/spelling.
>
> But the main value of being open source is the community aspect, e.g. the work you are doing in stack overflow is tremendous!


### **Jerry** — January 4, 2016 at 7:05 pm ([permalink](https://www.codenameone.com/blog/the-road-ahead-2015-in-review-survey-results.html#comment-21553))

> Jerry says:
>
> Thumbs up for the CN1 team. Good job guys.  
> I would love to see you integrate the CSS plugin into CN1 after a little more housekeeping on the Plugin. This will make it easier to work with, especially for the new starters. I believe you will be overwhelmed by the result.


### **Shai Almog** — January 5, 2016 at 3:39 am ([permalink](https://www.codenameone.com/blog/the-road-ahead-2015-in-review-survey-results.html#comment-22361))

> Shai Almog says:
>
> Thanks. I know Steve agrees and Chen to a lesser extent.
>
> Personally, I’d like proof before committing resources to support this.
>
> Did you use the plugin?
>
> I think a lot of the people advocating CSS have a bit of a misconception of how it will “feel” within the Codename One developer workflow. I’d like to know if my concerns are founded or not before we take this big piece of code with all its baggage into Codename One.


### **gardnr** — August 1, 2016 at 9:58 pm ([permalink](https://www.codenameone.com/blog/the-road-ahead-2015-in-review-survey-results.html#comment-22961))

> gardnr says:
>
> The next sentences after “The reason is conceptual.” go on to discuss technical aspects of adding reflection.


### **Shai Almog** — August 2, 2016 at 3:54 am ([permalink](https://www.codenameone.com/blog/the-road-ahead-2015-in-review-survey-results.html#comment-22993))

> Shai Almog says:
>
> They go hand in hand. The conceptual reason is that we want Codename One to be “seamless & efficient”. To do that we need to generate an app that is small like a native app, secure (as in obfuscated), fast etc.  
> Due to the technical reasons above we can’t deliver the conceptual vision of Codename One & reflection.
>
> I think what I was getting at is that while this is solvable technically most people were oblivious to the problems this created as a result so this is more of “this is the wrong way of doing it that caused problems”.
>
> We are thinking about offering a client side API that would solve the big reflection use cases (IoC, ORM, language support etc.) without the problems typically associated with reflection. This will require a different API and usage pattern but if this is interesting to you let us know.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
