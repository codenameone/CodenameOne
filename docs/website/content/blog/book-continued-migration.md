---
title: Book & Continued Migration
slug: book-continued-migration
url: /blog/book-continued-migration/
original_url: https://www.codenameone.com/blog/book-continued-migration.html
aliases:
- /blog/book-continued-migration.html
date: '2015-06-28'
author: Shai Almog
---

![Header Image](/blog/book-continued-migration/codenameone-fr-book.png)

[Eric Dodji Gbofu](http://www.codenameonefr.com) has been working on a Codename One book in French for  
the past year and [it finally came out](http://www.d-booker.fr/cn1/214-codename-one.html)!  
I’m still waiting on my copy mostly to show to French speakers we meet (I have a very hard time picking languages), I’m  
pretty sure its a cool book. Chen and I wrote the forward for the book, I trust Eric did a great job in it just like he has done with  
[Codename One Fr](http://www.codenameonefr.com).  
You can order the book either directly thru the [publishers site](http://www.d-booker.fr/cn1/214-codename-one.html)  
(which is apprently the preferred way) or thru [Amazon](http://www.amazon.fr/Codename-One-D%C3%A9velopper-Android-Blackberry/dp/2822703485). 

We hope many of you purchase this book, if there is interest in it then it might prompt further updates and additional publishers. 

#### App Engine Migration Part III

This is going to be a long and winding saga covering the migration process of our backend servers from App Engine  
to a more “hands on” architecture. The build servers storage aspect already migrated to S3 and seem to be functioning  
reasonably well. We are now setting our focus on several different features, the first of which is  
[crash reporting](/how-do-i---use-crash-protection-get-device-logs.html). 

An important feature within crash reporting is the feature that allows sending an email with your log file from the device,  
with new builds this will use our new webservice that no longer uses app engine. It will also switch to using sendgrid  
for the actual email delivery…  
This should improve the functionality considerably since unlike app engine the log will now be embedded into the body  
of the email thus allowing you to use email filters etc. It will also solve the issue of log files no longer being available. 

We will now also quota emails so no more than 1 email per minute can be sent (per user) and no more than 200  
per 48 hours. This will prevent some cases we had in the past from recurring where a wayward app update  
decimated users inboxes. 

This is yet another step in an ongoing process, we intend to shift device registration/push to the new servers as soon  
as possible which should open up a whole set of possibilities.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
