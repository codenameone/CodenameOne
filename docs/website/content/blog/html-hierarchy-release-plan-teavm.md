---
title: HTML Hierarchy, Release Plan & TeaVM
slug: html-hierarchy-release-plan-teavm
url: /blog/html-hierarchy-release-plan-teavm/
original_url: https://www.codenameone.com/blog/html-hierarchy-release-plan-teavm.html
aliases:
- /blog/html-hierarchy-release-plan-teavm.html
date: '2015-04-07'
author: Shai Almog
---

![Header Image](/blog/html-hierarchy-release-plan-teavm/html5-banner.jpg)

When Codename One packages applications into native apps we hide a lot of details to make the process simpler.  
One of the things we had an issue with is `getResource/getResourceAsStream` both of which  
are problematic since they support hierarchies and a concept of package relativity.   
That’s a concept that is problematic in iOS, generally everything about file access within the bundle in iOS  
is a bit problematic to accomplish in a cross platform way because Apple tries so hard to “simplify” and ends  
up creating fragmentation for us. 

So we have our own `getResourceAsSteam` in the `Display` class and that works  
just fine, but it requires that all files be in the src root directory and still has some issues (e.g. if a file has 2 extensions).  
Unfortunately there is still one major use case that’s very difficult to adapt to this usage and that is html…  
Web developers are used to constructing hierarchies to represent the various dependencies and use relative  
links/references. This is pretty difficult to avoid if you use pretty much any framework out there and so it was  
pretty difficult to embed HTML into a Codename One application since using relative references was downright  
difficult… 

We now have a solution: the html package…  
Just place all of your resources in a hierarchy under the html package in the project root. Our build server will  
`tar` the entire content of that package and add an html.tar file into your native package instead.  
It will then untar it on the device when you actually need the resources and only with new builds (not on every launch).  
So just place all your HTML’s, javascripts, images & CSS’s in the html package and the packages/directories below it.  
Then you can use the web browser component like this: 
    
    
    try {
        browserComponent.setURLHierarchy("/htmlFile.html");
    } catch(IOException err) {
        ...
    }
    

Notice that the path is relative to the html directory and starts with `/` but inside the HTML files  
you should use relative (not absolute) paths. Also notice that an `IOException` can be thrown due  
to the process of untarring. Its unlikely to happen but is entirely possible. 

### Codename One 3.0

We’ve been really bad about releasing Codename One 3.0, we wanted to do that quite a few times but  
things got sidetracked with the new VM and various other issues that prevented us from reaching the point  
we wanted for release. 

We decided that now is probably the best time to do it and we shouldn’t procrastinate further. Currently  
the tentative release date is April 27th, but this might move based on issues. With that in mind we would  
like to have a 2 week code freeze to improve stability and bring the docs up to date. So we expect to enter  
code freeze on April 13th next week! This means some features/issues will be delayed to post 3.0 but we are also trying to cram as many fixes as possible  
into this release and are working hard on the issue tracker. 

When we release so rarely its a big problem and makes releases harder to produce, we are now thinking of  
migrating towards a Chrome/Mozilla like release schedule and release every 3 months on a fixed freeze/release  
schedule. If you have thoughts on this please feel free to chime in. 

### TeaVM

In a previous post I mentioned a Javascript VM port and this was misunderstood by some developers as  
co-opting a separate open source project. Just to be clear, the work I posted about was based on  
[Tea VM](http://teavm.org/) and what Steve did was mostly create a Codename One  
port on top of that. Steve went into details [here](http://sjhannah.com/blog/?p=382)  
in his personal blog. 

So why didn’t I just write TeaVM instead of making a generic Javascript VM statement?  
We didn’t announce the product and I was not the technical guy involved, I’m neither familiar with the code  
or the project/people involved so I preferred to just use a generic term. I assumed people would understand  
that when we make the actual announcement/availability we will explain the technical details including the  
role of TeaVM in this. 

I apologize to [Alexey](https://twitter.com/konsoletyper), I had no intention of  
minimizing or reducing the credit of your effort/achievement!

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
