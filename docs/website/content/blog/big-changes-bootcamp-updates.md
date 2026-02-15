---
title: Big Changes and Bootcamp Updates
slug: big-changes-bootcamp-updates
url: /blog/big-changes-bootcamp-updates/
original_url: https://www.codenameone.com/blog/big-changes-bootcamp-updates.html
aliases:
- /blog/big-changes-bootcamp-updates.html
date: '2017-04-10'
author: Shai Almog
---

![Header Image](/blog/big-changes-bootcamp-updates/full-stack-java-bootcamp.jpg)

Since I haven’t blogged in a while a lot of stuff has piled up on my desk and I’ll get it out in batches in this post I’ll go over a few of the bigger changes we did while I was away on the bootcamp and also give you a bit of an update on what we’ve been doing within the bootcamp itself.

The week before we launched the bootcamp our Mac build servers reached a very heavy workload, this was becoming disruptive to our general developer population as builds got queued at a high rate. After a bit of investigation it seems that this could usually be pinned to specific users with **very** long build times.

Normally build time is divided as:

  * Upload

  * Queued

  * Build

  * Upload result

The latter is pretty fast since our servers have a very fast connection, the first depends on your Internet speed however the queue is problematic…​

We have a limited number of servers, if the build time is too long our servers can become occupied and then people will remain in queue for quite a long time. Worse, because our queue system is biased based on grade if a paying or a few high ranking paying user send builds in succession this might create a situation where low ranking users (free or basic) might be denied access to the server as a higher rank user will constantly step ahead in line.

Slow builds hurt everyone, up until now we always assumed people would align and try to avoid slow builds but I’m guessing that was naive. The incentive to do that isn’t enough. So we installed a timeout quota on a portion of the build phase which limited some builds.

It was a bad idea to do this right before the bootcamp but I felt some urgency due to the large amount of queuing. Initially we limited the time to 15 minutes on the xcode compile phase which is normally around 1.5-2 minutes. Due to some complaints I raised it to 25 minutes and some people still couldn’t get their builds thru (this should give you a sense of how problematic this traffic jam was). Finally Dave produced an idea for improvement in the build process that we implemented and deployed this weekend and it should produce faster builds for those use cases. I don’t see a difference with my builds but if your builds were taking 50 minutes they should be much faster now.

This change shouldn’t impact most users but since it changes the way headers are included some native code that relies on headers being included by VM generated code might fail.

### Read Response for Errors

Another potentially disruptive change we released this weekend is a flipped default on `readResponseForErrors`. Normally when you get an error from the server (e.g. 404) the `readResponse` method of connection request isn’t invoked and you can’t read the error message.

This seems to make sense but it causes two big problems:

  * When working with webservices you often get an error code and text describing the error which you would want to read

  * The network monitor won’t show the error response either…​

So we flipped the default to read the response for error codes. This shouldn’t impact most applications but if yours is impacted you can restore the old behavior either by using `setReadResponseForErrors(false)` on the connection request or do `ConnectionRequest.setReadResponseForErrorsDefault(false);` to disable this globally.

### Bootcamp Update

We just finished the first 2 weeks (10 missions) of the bootcamp and will resume the last 2 weeks on the 18th of April (at which point we will again suspend the blog posts).

We are covering a lot of material, in some regards more than I expected. I know I’ve personally learned a lot from interacting more closely with the developers in the bootcamp and I think I have a better sense of the pain points. I’ve already made a lot of commits to the project over the last few days to address some of the pain points I felt during the bootcamp but some are harder to fix with a few commits.

Once the bootcamp is over I plan to redo a lot of the videos in the site and rework some of our tools/tutorials.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **salah Alhaddabi** — April 18, 2017 at 3:40 pm ([permalink](/blog/big-changes-bootcamp-updates/#comment-23448))

> salah Alhaddabi says:
>
> Very nice Shai and hope the result of the bootcamp will be fruitful and lead to better tutorials/documentation
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
