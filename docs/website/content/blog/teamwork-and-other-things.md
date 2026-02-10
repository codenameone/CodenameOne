---
title: Teamwork (and other things)
slug: teamwork-and-other-things
url: /blog/teamwork-and-other-things/
original_url: https://www.codenameone.com/blog/teamwork-and-other-things.html
aliases:
- /blog/teamwork-and-other-things.html
date: '2013-08-12'
author: Shai Almog
---

![Header Image](/blog/teamwork-and-other-things/teamwork-and-other-things-1.jpg)

  
  
  
  
![Team](/blog/teamwork-and-other-things/teamwork-and-other-things-1.jpg)  
  
  
  

While Java rocks for teamwork because of its strict and streamlined heuristics, our resource files are less ideal. However we are changing that in the next update to Codename One!  
  
  
The reason we chose to put everything into a binary resource  
  
file should be obvious: small size, portability. These are two critical features we had in mind when we designed the resource file format, we also wanted it to be really simple so we can e-mail it to our designer and so people could use it without an IDE (which back in 2006 was far more common). We initially thought about creating an XML file format which a special tool will convert into the res file format (and we had some prior work on the matter such as the XML ant tasks) but these proved to be cumbersome to maintain. 

  
The reason we looked at XML was team support, a binary file can only be modified by one person in the team which forced us to use “a wireless protocol for file locking contention based on sonic broadcasts” (otherwise known as shouting in the office: I’m changing the resource file, don’t make any changes to it).  

  
With the advent of Codename One a lot of the initial constraints are no more, resources are in the root of the source folder and so we can make various assumptions that can keep the compatibility & ease of the existing resource file while provid  
  
ing the full team capabilities and history support you expect. As a bonus this also allows for various capabilities such as scripting/automating file replacements etc.

  
The new version of the designer tool includes a checkbox option for XML team support (under the file menu), notice that it is in the version we released earlier  
  
this week but it isn’t working properly.

  
When you activate  
  
the option every time you save the resource file we will automatically store an XML file in a “res” directory under your project root and a directory hierarchy containing all the data from within the resource file. When you load a res file (notice you always work with the res file never with the XML file) we will automatically detect that an XML file exists (assuming the XML option is turned on) and load that instead of the res file.

  
What this means is that if the option is turned on, XML overrides resource. So even though you will have conflicts in the res file the XML file can just override the conflicts every time and its far more team friendly! Also because of the way it is laid out you can actually change things such as images without using the designer tool (you will need to reopen the file for changes to take effect). It is however important to save within the design tool for your changes to actually apply to the running project.  

  
Right now a lot of the internal data such as the actual screen designs is still in binary form mostly due to the logistics of changing this, we will look at changing this to XML too as we move along.  
  

* * *

##  Improved iOS Settings & New Features  
  

  
  
  
[  
![Settings](/blog/teamwork-and-other-things/teamwork-and-other-things-2.png)  
](/img/blog/old_posts/teamwork-and-other-things-large-3.png)  
  
  

We’ve made some UI improvements to the project settings for iOS. We just had so many build arguments it was getting out of hand. Ideally we will add most build arguments to the UI so the configuration will be simpler. 

  
In other news the side menu now allows you to swipe the menu in by just swiping the screen  
  
and it is also more robust in terms of “regret” e.g. if you start swiping it to close and don’t drag it enough it will bounce back to open state.

  
There are also some changes for dSYM files but these will have to wait for their own blog post.  
  

* * *

Notice: This post was automatically converted using a script from an older blogging system. Some elements might not have come out as intended…. If that is the case please let us know via the comments section below.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
