---
title: You are cordially invited
slug: you-are-cordially-invited
url: /blog/you-are-cordially-invited/
original_url: https://www.codenameone.com/blog/you-are-cordially-invited.html
aliases:
- /blog/you-are-cordially-invited.html
date: '2013-10-09'
author: Shai Almog
---

![Header Image](https://img.youtube.com/vi/L4Rxlc_oUaQ/hqdefault.jpg)

  

This has been a busy week, I am gearing up for my  
[  
LTS lecture  
](http://www.luxoft.com/lts-luxoft-technology-series/)  
on the 30th. Its free for everyone and I hope you all tune in to watch it!  
  
The subject matter should be interesting to all Java developers and its essentially how you build a cross platform solution implementation, you don’t need any knowledge of Codename One since I won’t be discussing it much. Its about device issues and how these work with the JVM. So feel free to distribute this to your friends who aren’t necessarily Codename one developers. 

But this in’t why I’ve been busy, we had a major server failure this week which is rare since our architecture is highly redundant and should be resistant to failure. To understand what failed one needs to understand how Codename One servers actually work. When you send a build to the cloud you aren’t sending it to a build server. You are sending it to our cloud server which is based on Google’s App engine for Java.  
  
App engine is highly redundant and pretty stable so its very unlikely that it will fail, a failure in app engine would mean that our web console (where you see the build results appear) would stop working and you would be unable to send builds. Even if a build server fails only a specific build will fail, since we have redundancy all servers would need to fail for service to stop entirely.

The first failure included an in-ability to send a build while the console itself and everything else worked as expected, to make matters worse on the server side we got no error whatsoever and no indication that something was going wrong. Investigating something like this is naturally remarkably difficult!

  
App engine uses a tool called the blob store to perform uploads, so when data is submitted to app engine it really reaches a Google  
  
internal service after which it arrives at our Servlet where it is added to the database and delegated to a build server. Unfortunately, we weren’t getting to the servlet! The failure occured within the Google upload process which is effectively seamless to us.

  
So we started looking at whether upload or the blob store service were down, they were not. We were able to upload (e.g. device logs etc.).  

  
  
Since the blob store uses a Google internal URL the upload process consists of two distinct stages, the user needs to “request” an upload URL from google and then upload to that URL (after which he is redirected to the servlet). That was the culprit, when we requested a URL we passed the user credentials. The idea was to save blob store space/upload time in the case of a user who ran out of credits.  
  
  
Google extracted the arguments that we passed to the servlet (notice that these arguments are never explicitly passed to the blob store which doesn’t get the request context)!  
  
  
  
These arguments were then appended to the URL returned (which is naturally a post URL) creating an illegal URL that can’t be fixed.  

  
We made several attempts at patching this such as editing the URL dynamically (using substring, url encoding etc.) all of which produced cases such as uploads getting “swallowed” with no redirect to the actual build servlet. Eventually, we just redirected without the arguments and this worked around the issue.  

  
This was the second time this year where we had a failure of this type (the first was due to Google App Engine being down for a couple of hours). This still puts us at a 99.999  
  
% uptime on an annual basis which is pretty good.  
  
  

Notice: This post was automatically converted using a script from an older blogging system. Some elements might not have come out as intended…. If that is the case please let us know via the comments section below.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
