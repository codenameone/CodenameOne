---
title: JBake & New Website Coming Soon
slug: jbake-new-website-coming-soon
url: /blog/jbake-new-website-coming-soon/
original_url: https://www.codenameone.com/blog/jbake-new-website-coming-soon.html
aliases:
- /blog/jbake-new-website-coming-soon.html
date: '2015-03-08'
author: Shai Almog
---

![Header Image](/blog/jbake-new-website-coming-soon/javascript_port_teaser.png)

![](/blog/jbake-new-website-coming-soon/javascript_port_teaser.png)

This website/blog has gotten really long in the tooth, we would have replaced it ages ago but since the build server is so tightly integrated in the code the effort to migrate was just too big. So recently we finally made the effort and migrated large blocks of code to be far more generic and we are now working hard on moving more than 3 years of content to the new website… 

But I want to talk to you about something different and rather cool I discovered: [JBake](http://jbake.org/). 

Don’t let the spartan website fool you. Its a really cool and rather complete tool that’s meant to be Java’s answer to Jekyll.

In case you aren’t very familiar with the current fashion in website development, the current trend is static websites that can be super scalable and easy to deploy. Thanks to modern day JavaScript, such static websites are still very dynamic and interactive while providing levels of performance/scalability that are ridiculously high with CDN’s. 

The problem here is that you want stuff to still be easy to maintain, e.g. if you have site-wide navigation  
code you would want to reuse it. That’s where [JBake](http://jbake.org/) comes in.  
It allows you to use tools like [FreeMarker](http://freemarker.org/),  
[AsciiDoc](http://www.methods.co.nz/asciidoc/) etc. during website build time (rather than dynamically in runtime) thus generate a static website  
with dynamic scripts. The basic level is things like server side includes done statically, but it also generates  
things like the RSS feed (statically) and pretty much everything you need! 

One of the nice things about it is that even if the tool isn’t perfectly mature in some ways since the output is  
static HTML so this isn’t something your users will ever be exposed to. [JBake](http://jbake.org/)  
is very intuitive if you have Java programming experience, I had the basic site, with blog and designs running in less than a day and within two days it was perfect.  
Another plus is that the community is very open and supportive which is hugely important when you are picking up any such tool. 

Currently the only struggle is in migrating everything over since our existing CMS really made a mess of all the data and I still have no idea how we will migrate the old blog comments… 

Since JBake doesn’t have a logo file I chose to include a “teaser” to the  
[JavaScript port of Codename One](http://www.codenameone.com/blog/web-app-build-target)…

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
