---
title: Sheets and Samples
slug: sheets-samples
url: /blog/sheets-samples/
original_url: https://www.codenameone.com/blog/sheets-samples.html
aliases:
- /blog/sheets-samples.html
date: '2019-06-03'
author: Shai Almog
---

![Header Image](/blog/sheets-samples/new-features-2.jpg)

Over the years we wrote a lot of demos for Codename One as well as a lot of test cases. This can get painful after a few dozen projects each of which with their own version of the JARs and build script. To solve this never ending need to organize samples Steve introduced a new samples folder into the open source project.

This makes it easy to build/run samples if you’re command line inclined. All you need is an install of Java and Ant to be in business. Steve introduced this quite a while back but it took a while to reach a critical mass of samples which it recently passed. By now the samples cover a lot of the functionality of Codename One. There’s a lot of documentation for the samples folder [here](https://github.com/codenameone/CodenameOne/tree/master/Samples) so I won’t go too much into details. I will however refine a few steps in the getting started process…​

To run the samples you need to checkout the [git cn1 project](https://github.com/codenameone/CodenameOne/) then do the following in the project root directory:
    
    
    ant samples

__ |  `JAVA_HOME` must point at a valid JDK for Codename One and ant must by in the system `PATH` variable. Otherwise this won’t work.   
---|---  
  
![The Sample Runner App](/blog/sheets-samples/sample-runner.png)

Figure 1. The Sample Runner App

It doesn’t look pretty but you can run a sample for almost any Codename One component and see the applicable code. You can send device builds etc. This is a very convenient system for test cases.

### Sheet

As such the samples contain a sample for the new `Sheet` API which you can [see here](https://github.com/codenameone/CodenameOne/blob/master/Samples/samples/SheetSample/SheetSample.java). This is it, there is no need for resources or anything, the file is self contained.

`Sheet` is a new API that lets you show the property sheet UI common in iOS and recently on Android as well. It was easy to do this in the past but there was no dedicated class to address it…​ Now there is.

![The Sheet Sample](/blog/sheets-samples/sheet.png)

Figure 2. The Sheet Sample
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Francesco Galgani** — June 8, 2019 at 10:42 pm ([permalink](/blog/sheets-samples/#comment-24067))

> The “More -> Launch JS” option opens the url “http://localhost:40549/”. Do you have integrated a web server in this Codename One Samples? Just a curiosity, which server is it?
>



### **Shai Almog** — June 9, 2019 at 3:49 am ([permalink](/blog/sheets-samples/#comment-21561))

> It’s tomcat. The web target in the build XML file lets you do that.
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
