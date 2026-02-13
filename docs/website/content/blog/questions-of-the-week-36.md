---
title: Questions of the Week 36
slug: questions-of-the-week-36
url: /blog/questions-of-the-week-36/
original_url: https://www.codenameone.com/blog/questions-of-the-week-36.html
aliases:
- /blog/questions-of-the-week-36.html
date: '2016-12-15'
author: Shai Almog
---

![Header Image](/blog/questions-of-the-week-36/qanda-friday2.jpg)

We had to push out an update to the IntelliJ/IDEA plugin to workaround an issue that started happening with their  
latest IDE update. The 3.5.11 version didn’t change much just fixed those specific issues. Other than that this  
weeks release includes some new In-App-Purchase features (that we will discuss next week) and the new  
seamless caching API discussed yesterday.

One of the features we didn’t mention this week in the posts is a new ability to set the default size of a floating  
action button using code like:
    
    
    FloatingActionButton.setIconDefaultSize(4);

I didn’t mention this before because it was a result of  
[this question](http://stackoverflow.com/questions/41048508/how-to-change-codenameone-floating-action-button-size/41053436)  
so please ask on stackoverflow and you might get a new feature…​

[Hello World](http://stackoverflow.com/users/6351897/helloworld) who asked that also had a question about  
[audio capture timing](http://stackoverflow.com/questions/41129436/is-there-a-way-to-limit-the-duration-of-an-audio-capture-in-codename-one).  
We place so much emphasis on the `Capture` API that sometimes people don’t notice we can just record audio  
directly via the `MediaManager` class.

I’ve been going back and forth with [ravimaran](http://stackoverflow.com/users/4237411/ravimaran) over the process  
of [uploading to S3](http://stackoverflow.com/questions/41094606/codename-one-upload-a-picture-taken-by-the-camera-to-amazon-s3-bucket).  
This is actually pretty easy thanks to the multipart API. However, permissions/visibility settings in S3 aren’t trivial.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
