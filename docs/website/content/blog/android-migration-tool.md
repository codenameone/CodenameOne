---
title: Android Migration Tool
slug: android-migration-tool
url: /blog/android-migration-tool/
original_url: https://www.codenameone.com/blog/android-migration-tool.html
aliases:
- /blog/android-migration-tool.html
date: '2016-07-26'
author: Shai Almog
---

![Header Image](/blog/android-migration-tool/profiling-in-android-ios.png)

It’s tough to pick up a new toolchain like Codename One. There’s so much to learn…​  
A lot of our developers come from the Android world and even though Codename One is much simpler than Android porting the first app to Codename One is still painful.

We wanted to simplify this process since the day we launched Codename One but as we [explained before](/blog/why-we-dont-import-android-native-code.html), this isn’t simple and the results would “underwhelm”.

We decided that “underwhelming” isn’t always a bad place to start when you are doing open source work. With that in mind Steve [created an open source project](https://github.com/shannah/cn1-android-importer) to scaffold a new Codename One project from an existing Android native project.

Notice my choice of words, I chose scaffold instead of migrate. This will not turn an Android project into a Codename One project but rather make the process of getting started slightly easier. It migrates the images & strings.

It creates GUI builder files (using the new GUI builder) for every layout XML file. Notice that the layout isn’t replicated properly and neither is the proper styling.

These differ a lot between Android and Codename One and would require at least 6 months of intense work to get right.

Copying the layout seems deceptively easy on the surface but Android layouts differ considerably. We’d love to simplify that but the level of effort required is beyond our limited resources. We can’t justify the effort without a sense of demand…​

In the current version we don’t touch the source or the manifest at all but we could address both of these to some degree as part of the work.

### Moving This Forward

That’s where you come in. File issues, RFE’s and let us know that you want progress on this.

Fork and contribute to Steve’s project and provide samples that we should improve.

Let your friends know about this project and raise community awareness around it!

We don’t want to invest significant developer resources on something that won’t gain developer traction so we need your help to get this project off the ground.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Gareth Murfin** — July 27, 2016 at 3:09 pm ([permalink](/blog/android-migration-tool/#comment-22786))

> Gareth Murfin says:
>
> What a great idea, many Android devs looking to produce iOS ports end up thinking about using Codename One. As you say resources are so very different, I found learning Android GUI dev much harder than learning CN1 GUI dev (but previously I was into Swing, J2ME, LWUIT etc). I think one of the main paradigm shifts that is hard to learn is the lack of “activities”. That is in Android each screen has its own class and it starts to feel nice and correct (more OO/modular or something :)) – and when you go to CN1 it is very strange to have everything more “old school” in one or 2 classes. If it were possible it would be good if each screen in cn1 could actually be a separate class, so when you create an event for postShow or something it doesnt go into statemachine but a class called for example Splash(), and with a method postMain() in there. This would make it far easier to navigate projects and understand them (new coders have been scared of even looking at my gargantuan statemachines, preferring to do a rewrite(!)). Just a suggestion of course, and we could easily do this ourself by simply making calls from StateMachine to custom classes we can make for each screen, which is actually what I am planing on doing in my next cn1 app. Current I mostly have one large statemachine, another class holding the business logic that is called on from statemachine, and then a pile of POJOs.


### **bryan** — July 27, 2016 at 9:08 pm ([permalink](/blog/android-migration-tool/#comment-22906))

> bryan says:
>
> A class per screen/form is the “new” way to do CN1, and the way the new GUI builder works, so this porting tool would do that.


### **Shai Almog** — July 28, 2016 at 4:18 am ([permalink](/blog/android-migration-tool/#comment-22706))

> Shai Almog says:
>
> Yep. I mentioned this uses the new GUI builder so it’s one form class per layout.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
