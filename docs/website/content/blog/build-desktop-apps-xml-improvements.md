---
title: Build Desktop Apps & XML Improvements
slug: build-desktop-apps-xml-improvements
url: /blog/build-desktop-apps-xml-improvements/
original_url: https://www.codenameone.com/blog/build-desktop-apps-xml-improvements.html
aliases:
- /blog/build-desktop-apps-xml-improvements.html
date: '2014-01-12'
author: Shai Almog
---

![Header Image](/blog/build-desktop-apps-xml-improvements/build-desktop-apps-xml-improvements-1.png)

  
  
  
  
![Picture](/blog/build-desktop-apps-xml-improvements/build-desktop-apps-xml-improvements-1.png)  
  
  
  

The next plugin update will finally include the support for building desktop applications with Codename One and to celebrate this we added a  
[  
How Do I video on this subject  
](/how-do-i-use-desktop-javascript-ports.html)  
. Notice that this is a pro only feature so if you don’t have a pro account this will fail on the build server.  
  
  
The end result should be pretty similar to what you get in the simulator, I assume we will make some improvements as we move along to refine the user experience a bit further and adapt it for desktop development.  
  
  
  
  
  
We also added support for XML user interface elements in the designer, if you activate team mode in the designer and save the resource file you will now notice that we generate a UI file for every form (as well as a file with no extension which is a binary file). Currently the UI file is write only to prevent issues with this approach from breaking existing usage. However, if you want to move to a more XML based file format right now you would be able to edit your top level XML file and set  
  
useXmlUI=”false”.  
  
  
Once you change that all changes to the .ui files will be incorporated when  
  
you reload the resource file and you will be able to work with XML files which might be convenient if you are doing elaborate team work. This is also convenient for the case of a recovery if a file gets corrupted you should be able to recover more easily and see what happened.  
  
  
  

* * *

Notice: This post was automatically converted using a script from an older blogging system. Some elements might not have come out as intended…. If that is the case please let us know via the comments section below.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Anonymous** — January 28, 2014 at 4:50 am ([permalink](/blog/build-desktop-apps-xml-improvements/#comment-22065))

> Anonymous says:
>
> Good article but it should have been a bit more descriptive. Any suggestions for me, I do don’t have a pro account.
>



### **Anonymous** — January 28, 2014 at 5:00 pm ([permalink](/blog/build-desktop-apps-xml-improvements/#comment-21991))

> Anonymous says:
>
> You can handcode a desktop app by searching for instructions in the discussion forum. Pro subscription has many other benefits.
>



### **Anonymous** — January 30, 2014 at 1:08 pm ([permalink](/blog/build-desktop-apps-xml-improvements/#comment-21671))

> Anonymous says:
>
> Why? 
>
> What am I missing? Why on earth would I want a desktop version of my tablet app?
>



### **Anonymous** — January 30, 2014 at 2:33 pm ([permalink](/blog/build-desktop-apps-xml-improvements/#comment-21946))

> Anonymous says:
>
> Initially that’s what we thought but users asked for that quite a lot. There are several reasons: 
>
> 1\. Demo – this way users can download a demo version to run on their PC/Mac. 
>
> 2\. x86 tablets – E.g surface pro etc. have a niche following in some sectors. 
>
> 3\. Checklist feature – some developers need a desktop app as a feature but don’t care if it feels like a mobile app.
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
