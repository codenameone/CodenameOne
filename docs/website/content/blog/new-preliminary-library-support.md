---
title: New Preliminary Library Support
slug: new-preliminary-library-support
url: /blog/new-preliminary-library-support/
original_url: https://www.codenameone.com/blog/new-preliminary-library-support.html
aliases:
- /blog/new-preliminary-library-support.html
date: '2013-02-07'
author: Shai Almog
---

![Header Image](/blog/new-preliminary-library-support/new-preliminary-library-support-1.png)

  
  
  
[  
![Picture](/blog/new-preliminary-library-support/new-preliminary-library-support-1.png)  
](/img/blog/old_posts/new-preliminary-library-support-large-2.png)

We’ve just launched a new library project type for Codename One, this is very preliminary but we think this is pretty much the final direction we will take with the Codename One library support. 

To get started just create a new project and select the Codename One library project as your option.  
  
This will generate a cn1lib file when you build the project which you can just place in a lib dir of any new Codename One project in order to distribute binary libraries. You will need to use the Refresh  
  
Libs right click option when adding a new library to a project.

  
The obvious question we get quite frequently is: Why not just use  
  
Jars?

  
There are several different answers here:  
  

  *   
We only support a subset of Java 5 hence something might not be supported and we will fail.  
  

  *   
We expect code to be compiled in a specific way (target 1.5 etc.) we just don’t test other configurations and they are likely to fail.  

  *   
We would like to enable native device code, e.g. including an Android specific JAR or iOS specific .a library, this is possible with our file format but impossible with JAR’s.  

  *   
We want  
  
intellisense  
  
code completion to work properly (include the javadoc when you use the library) while this is possible with jar’s its not automatic.  

  *   
We want everything to integrate with the Codename One designer tool (this is work in progress, more on that later).  

  
So now that we have the basic requirements in place what is actually happening and what’s a cn1lib file?  

  
The cn1lib file is a zip containing between 2 – 7 zips within it.  

  
The two required zips are one containing the compiled classes of the library and the  
  
other one contains stub sources for the library. We run a doclet on your sources and generate sources that only contain the signatures and javadocs, this allows code completion to work correctly in the various IDE’s without you having to do anything about it and still allows for proprietary libs.

  
Other than that we package into zips all the content of the native directories for every one of the platforms, so if you used Codename One’s ability to generate native interfaces then this will all be packaged in source form into the library. To keep proprietary data there you would need to use a library such as a jar (for Android, Blackberry & J2ME) or an ‘a’ file for iOS. That gets into the territory of native OS programming which is a bit out of the scope of this mini introduction so I won’t get into that.  

  
I hope you guys start banging on the tires of this tool and let us know how it works out for you.  
  
  
  

* * *

Notice: This post was automatically converted using a script from an older blogging system. Some elements might not have come out as intended…. If that is the case please let us know via the comments section below.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Anonymous** — February 12, 2013 at 6:41 am ([permalink](/blog/new-preliminary-library-support/#comment-24156))

> Anonymous says:
>
> Great news ! The ability to embed native code reminds me of the Native Extensions for Adobe AIR, it offers many opportunities for developers. 
>
> Can’t wait to do some testing.
>



### **Anonymous** — February 15, 2013 at 2:10 pm ([permalink](/blog/new-preliminary-library-support/#comment-21887))

> Anonymous says:
>
> It cannot be overstated how important a step this is for CN1. Just as vibrant App market is critical to the success of a device, a flourishing ecosystem of 3rd party libraries is a compelling reason to choose one platform over another. 
>
> Looking forward to digging in and creating some libraries.
>



### **Anonymous** — June 18, 2013 at 10:07 pm ([permalink](/blog/new-preliminary-library-support/#comment-21801))

> Anonymous says:
>
> May I make a suggestion. Modify SWIG to generate wrappers to common C libs for codename one and your on a winner. The approach will need to be different between the platforms , BUT, a lot of C libs have IOS *and* android ports (ultimately the only platforms that really matter imho) but expose common APIs. Doing this gives you a huge range of potential libraries whilst preserving at least some degree of cross platform support.
>



### **Anonymous** — June 19, 2013 at 4:26 am ([permalink](/blog/new-preliminary-library-support/#comment-21953))

> Anonymous says:
>
> Thanks. I think the SWIG interface requirement might be problematic. There are several tools out there that map Objective-C code to Java and since Android is Java this should theoretically work. 
>
> The problem with these sort of approaches (e.g. taken on C# with Mono) is that when you have a failure you will only find help for Objective-C code and you would only be able to properly debug on xcode so you end up back in square one. 
>
> Native wrappers essentially mean we expect people to go into native code and we feel the opposite. If you need to write native code that means our platform is missing something.
>



### **Anonymous** — October 23, 2014 at 11:25 pm ([permalink](/blog/new-preliminary-library-support/#comment-22212))

> Anonymous says:
>
> It seems we can not import to Cn1 library project another Cn1 lib.( got compile error) 
>
> Example: We make a chart Cn1 lib project but can not import font or graphic Cn1 lib( can’t see refresh libs option)
>



### **Anonymous** — October 24, 2014 at 9:59 am ([permalink](/blog/new-preliminary-library-support/#comment-21595))

> Anonymous says:
>
> We currently don’t support nesting libraries into one another. Most developers who needed that ended up just adding the other library to the classpath in compilation then requiring that both cn1libs be in the lib directory. This isn’t ideal but can be worked with for most cases. You can also just hack the ant scripts to achieve more elaborate build options.
>



### **David Hinckley** — December 25, 2015 at 10:23 am ([permalink](/blog/new-preliminary-library-support/#comment-22308))

> David Hinckley says:
>
> I do not see the CodenameOne Library project option. I have CodenameOneFeature1.0.0.201511241324 on Eclipse 4.4.2. What am I missing?
>



### **Shai Almog** — December 26, 2015 at 5:00 am ([permalink](/blog/new-preliminary-library-support/#comment-22370))

> Shai Almog says:
>
> We just didn’t implement this on Eclipse. Back when we did this there wasn’t much demand and most of the hardcore hackers used NetBeans since it made it easier to work with our sources: [https://www.codenameone.com…](</blog/how-to-use-the-codename-one-sources/>)
>
> We’ll add this in a future update, however you can just open any existing library project and just change it to suite your needs since they are just ant projects that don’t need much IDE integration.
>



### **Orlando D'Free** — March 31, 2016 at 8:36 am ([permalink](/blog/new-preliminary-library-support/#comment-22572))

> Orlando D'Free says:
>
> When I create a Codename One Library project, I find a file called [private.properties](<http://private.properties>) that has this line in it:
>
> application.args=com.codename1.hello.FirstCodenameOneLibrary
>
> This is referring to the one class that was placed in my library code. But I’m not using it, so my first instinct is to delete it. Does this property need to point to an existing class? Is there anything about that class that I need to know?
>



### **Shai Almog** — April 1, 2016 at 3:20 am ([permalink](/blog/new-preliminary-library-support/#comment-22640))

> Shai Almog says:
>
> This is part of the NetBeans project structure. You don’t need it and it won’t have any affect to remove it or leave it there.
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
