---
title: How To Use The Codename One Sources
slug: how-to-use-the-codename-one-sources
url: /blog/how-to-use-the-codename-one-sources/
original_url: https://www.codenameone.com/blog/how-to-use-the-codename-one-sources.html
aliases:
- /blog/how-to-use-the-codename-one-sources.html
date: '2015-12-06'
author: Shai Almog
---

![Header Image](/blog/how-to-use-the-codename-one-sources/how-to-use-the-codename-one-sources.jpg)

**UPDATE August 10, 2018** – The steps described in this article are out of date. The Codename One project now includes an ANT script that will retrieve dependencies and build the project automatically. [See the Quick Start section of the README](https://github.com/codenameone/CodenameOne/#quick-start-1) for instructions.

I’ve written open source software since the 90’s both for my own projects and for Sun Microsystems. When we  
founded Codename One open source was the only option!  
We didn’t choose open source with  
the goal of receiving code contributions. Contributions are pretty rare even in highly visible projects. We saw  
the true benefits of open source for a project like Codename One: **trust**. 

When you choose a platform for your code you need to trust that it will be there tomorrow & open source  
alleviates some of this risk. Open source keeps us honest about our cloud pricing.   
E.g. if we overcharge or provide shoddy service a fork might gain traction. This is a benefit to the consumer  
that helps us in forming trust and gaining traction in the long run. 

I felt the need to write this opening paragraph due to a highly visible source code closing done by another  
company. We have no intentions, plans, thoughts or wavers in that direction. Furthermore, we consider  
contributions to be the least important benefits of Open Source Software. 

Codename One is “more” open source than most projects, e.g. OpenJDK is OSS but its pretty hard to change  
code within it. Its even hard to contribute these changes!  
You can use/change most of our code using a trivial process. You don’t need a complex compiler toolchain  
for changing or debugging Codename One. Most importantly: 99% of the code is in Java so you should feel  
right at home! 

#### Using The Source

We already have an old tutorial called “[use the source](/blog/use-the-source.html)”  
but its pretty old by now. It still points at the old SVN and didn’t really go into details, so re-doing that and reminding  
you that this can be done seems in order.  
When you debug your app with our source code you can place breakpoints deep within Codename One and  
gain unique insight. E.g. a `Form` flickers in your application just place a breakpoint in  
`Display.setCurrent()`. 

When you run into a bug or a missing feature you can push that feature/fix back to us using a pull request.  
Github makes that process trivial and in this new video and slides below we show you how.   
The steps to use the code are: 

  1. Signup for Github
  2. Fork  
<http://github.com/codenameone/CodenameOne> and  
<http://github.com/codenameone/codenameone-skins>  
(also star and watch the projects for good measure).
  3. Clone the git URL’s from the projects into the IDE using the Team->Git->Clone menu option. Notice  
that you must deselect projects in the IDE for the menu to appear.
  4. Download the cn1-binaries project from github [here](https://github.com/codenameone/cn1-binaries/archive/master.zip ).
  5. Unzip the cn1-binaries project and make sure the directory has the name cn1-binaries. Verify that  
cn1-binaries, CodenameOne and codenameone-skins are within the same parent directory.
  6. In your own project remove the jars both in the build & run libraries section. Replace the build libraries  
with the CodenameOne/CodenameOne project. Replace the runtime libraries with the  
CodenameOne/Ports/JavaSEPort project.

**Update August 10, 2018** – You will need to build the codenameone-skins project using the build.xml file in its root directory in order to generate some skins that are required to build the JavaSE project.

This allows you to run the existing Codename One project with our source code and debug into Codename One.  
You can now also commit, push and send a pull request with the changes. 

#### Video – How To Use The Codename One Sources

#### Slides

**[How To Use The Codename One Sources](//www.slideshare.net/vprise/how-to-use-the-codename-one-sources "How To Use The Codename One Sources") ** from **[Shai Almog](//www.slideshare.net/vprise)**
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Ch Hjelm** — June 19, 2016 at 6:15 am ([permalink](/blog/how-to-use-the-codename-one-sources/#comment-22579))

> Ch Hjelm says:
>
> Hi, this is a great approach and I’ve used debugging of CN1 sources for quite some time. However, the video does not mention if you can build for devices using your own copy of the CN1 sources – is that possible and how should it be done? Thanks
>



### **Shai Almog** — June 20, 2016 at 4:10 am ([permalink](/blog/how-to-use-the-codename-one-sources/#comment-22732))

> Shai Almog says:
>
> Building for devices is a bit more complicated so we left that as an exercise for the developers…
>
> This is actually a really difficult process to document e.g. in Android which is the simpler pipeline we run into issues all the time with Google changing things and having to run around after them. On iOS we gave the start of the process in the VM docs: [https://github.com/codename…](<https://github.com/codenameone/CodenameOne/tree/master/vm>) but it gets pretty hairy from there on.
>
> We are looking at offering a better solution for offline building to the enterprise subscription in the near future.
>



### **Ch Hjelm** — June 20, 2016 at 9:52 pm ([permalink](/blog/how-to-use-the-codename-one-sources/#comment-22675))

> Ch Hjelm says:
>
> Thanks for the answer. I’m not an Enterprise user (and probably never will have the budget). If you can’t build with your modified CN1 sources for a device, then isn’t what you described above useless when developing an app for a device? Unless the changes get accepted back into the code base before having to run it on a device. And you also cannot ‘hack’ the CN1 sources when needed (e.g. I’ve had to make some changes to implement specific functionality which is not likely to be accepted into your code base), making it pretty pointless to spend time trying to work around such limitations. Sorry, if I’m missing something obvious, but I think you should mention these limitations in the material above – it’s pretty frustrating to find out *after* the fact.
>



### **Shai Almog** — June 21, 2016 at 3:45 am ([permalink](/blog/how-to-use-the-codename-one-sources/#comment-22934))

> Shai Almog says:
>
> The reason we make the enterprise requirement is because the support overhead is so great that it requires extra effort.
>
> This is step 1 of building for devices. It’s not impossible and quite a few people did that but making it too simple will cause people to try and go thru this route and running against worse walls when doing that.
>
> I’d be interested to know what sort of changes can’t be done thru a cn1lib and must go into our core sources?
>



### **AMDP AMDP** — March 23, 2017 at 12:00 pm ([permalink](/blog/how-to-use-the-codename-one-sources/#comment-23130))

> AMDP AMDP says:
>
> “Most importantly: 99% of the code is in Java so you should feel right at home!”
>
> Just curious about the 1%. Can you describe what language and why?
>
> Troy.  
> #
>



### **Shai Almog** — March 24, 2017 at 5:33 am ([permalink](/blog/how-to-use-the-codename-one-sources/#comment-23299))

> Shai Almog says:
>
> It’s C, C#, Objective-C & JavaScript. Each port for each OS needs native code to implement the OS abstraction layer. 99% is actually a bit high I should have said 99% of the code that matters.
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
