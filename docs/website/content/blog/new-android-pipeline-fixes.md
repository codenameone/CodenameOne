---
title: New Android Pipeline Fixes
slug: new-android-pipeline-fixes
url: /blog/new-android-pipeline-fixes/
original_url: https://www.codenameone.com/blog/new-android-pipeline-fixes.html
aliases:
- /blog/new-android-pipeline-fixes.html
date: '2014-03-29'
author: Shai Almog
---

![Header Image](/blog/new-android-pipeline-fixes/new-android-pipeline-fixes-1.png)

  
  
  
[  
![Picture](/blog/new-android-pipeline-fixes/new-android-pipeline-fixes-1.png)  
](/img/blog/old_posts/new-android-pipeline-fixes-large-2.png)  
  
  

We introduced a new rendering pipeline for Android a while back, it showed a lot of potential but unfortunately still had some major bugs. Chen just made some major fixes for this pipeline which should hopefully address those issues, please start testing your app with this pipeline and let us know if you experience regressions as a result of that.  
  
  
  
  
To test the new pipeline just define the build argument  
  
  
android.asyncPaint  
  
  
=true  
  
  
  
You define the build argument by going to your project properties, selecting Codename One and you should see a “Build Hints” tab option. Just enter the values there.  
  
  
  
  
  
Once we feel this is stable enough we will “flip the switch” and make it the default rendering pipeline on Android so hopefully it should be stable and performant by then.  
  
  
  
  
The regular Android pipeline should still work for the foreseeable future although due to changes Android 4.x made to various types of rendering strategies things that used to work in Android no longer work as expected for various edge cases (device fragmentation is indeed a problem here).  
  
  
  
  
Since the new pipeline is completely different performance of your application might actually degrade when you switch to it. You need to spend time investigating the paint chain in the Component Inspector tool (in the simulator menu) and make sure that there isn’t too much “overdraw”. Overdraw is the situation where a pixel on the screen is painted multiple times over and over. The old native pipeline eliminated this overdraw but the newer system doesn’t do a good job with that.  
  
  
  
  
On an unrelated subject we also added a “Clean Storage” option to the simulator and a  
  
  
  
  
  
  
  
“Remove All” option to the network monitor tool.  

* * *

Notice: This post was automatically converted using a script from an older blogging system. Some elements might not have come out as intended…. If that is the case please let us know via the comments section below.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Anonymous** — April 1, 2014 at 4:29 am ([permalink](/blog/new-android-pipeline-fixes/#comment-24213))

> Anonymous says:
>
> Async issue reported here [https://code.google.com/p/c…](<https://code.google.com/p/codenameone/issues/detail?id=1071>) is not yet fixed. 
>
> On March 5th I sent you an email with “Black issue with WebBrowser component”; that was also an async paint issue. Any progress on that? 
>
> Wim
>



### **Anonymous** — April 1, 2014 at 4:33 am ([permalink](/blog/new-android-pipeline-fixes/#comment-22038))

> Anonymous says:
>
> Thanks for the reminder, we will look into that.
>



### **Anonymous** — April 28, 2014 at 3:50 am ([permalink](/blog/new-android-pipeline-fixes/#comment-22121))

> Anonymous says:
>
> hi, 
>
> am a new user of your product codename one, i have some challenges i will want your team to help me with 
>
> 1\. We are trying to wirte a mobile app for our client, this app will collect data from the field officers and submit to 
>
> database (SQL server 2008) does codebname one have the capabilties for SQL server database, if we subscribe do you host 
>
> database online or we have to get one ourselves ? 
>
> 2\. In the form designer we have some controls on the form labale and textfield boxes, but the screen filled and we need to 
>
> add more under, we can’t scroll the designer screen up so as to add more why is codename one screen unscrollable. 
>
> we will appreciate if you can help with detail light on this two challenges. 
>
> thanks ABRAHAM (ABDATA SOLUTIONS)
>



### **Anonymous** — April 28, 2014 at 12:26 pm ([permalink](/blog/new-android-pipeline-fixes/#comment-21683))

> Anonymous says:
>
> 1\. No. You should use a webservice to hide the database. 
>
> 2\. You can add them via the tree and you can expand the view further to add more space. Due to the architecture of the current designer its hard to allow scrolling in design mode. We have plans to replace that tool in the future.
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
