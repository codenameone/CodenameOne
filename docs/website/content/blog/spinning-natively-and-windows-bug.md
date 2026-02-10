---
title: Spinning Natively And Windows Bug
slug: spinning-natively-and-windows-bug
url: /blog/spinning-natively-and-windows-bug/
original_url: https://www.codenameone.com/blog/spinning-natively-and-windows-bug.html
aliases:
- /blog/spinning-natively-and-windows-bug.html
date: '2013-11-10'
author: Shai Almog
---

![Header Image](/blog/spinning-natively-and-windows-bug/spinning-natively-and-windows-bug-1.jpg)

  
  
  
  
![Picture](/blog/spinning-natively-and-windows-bug/spinning-natively-and-windows-bug-1.jpg)  
  
  
  

[  
Shai Ifrach  
](https://twitter.com/future_soft)  
spent the weekend fixing one of the harder Windows Phone issues we had, sometimes on some Windows Phone devices the UI would start with a black screen but never when connected to the debugger! Ugh.  
  
  
Turns out this is a race condition related to using the wrong lifecycle callback (which is used by all of MS’s demos)!  
  
  
  
  
  
Kudos to him because I spent just about ages trying to tackle that issue unsuccessfully, this should now allow us to move forward with some of the additional issues on Windows Phone and hopefully bring the port up to the level of the other ports we have or at least a little bit closer.  

  
  
One of the changes in iOS 7 was the new 3d spinner component (not sure how that works with the flat design metaphor) this is a component that we were just completely unable to replicate no matter how hard we tried. So we are introducing a new native spinner API currently specifically for Date & Time picking but as we move along we might add additional options. This API will effectively popup a dialog and prompt the user to pick a date or time and use our Spinners where a native option doesn’t exist. Currently the native aspect is implemented for iOS/Android only.

* * *

  
  
  
[  
![Picture](/blog/spinning-natively-and-windows-bug/spinning-natively-and-windows-bug-2.png)  
](/img/blog/old_posts/spinning-natively-and-windows-bug-large-3.png)  
  
  

When we started working on this we wanted to use the amazing new Android widgets for date/time, but apparently these are specific to the Android calendar and not really available in the OS… So Google itself isn’t using native widgets for their own application and choosing to go with app specific widgets…. lovely.  
  
  
If you aren’t an Android 4 calendar user check out the screenshot to the left, its a pretty sweet time picker UI that looks better in real life where it animates very nicely.  
  
  
  
  
  
  
So we provide two API layers the first is a simple API with a fallback that just allows you to place a time/date widget. If a native picker is available it will be used otherwise our picker is used. This is the  
  
Picker API and although it looks likes a text field its really just a button that will popup a picker when pressed. Its really simple to use just add it to your UI either via the designer or in code and set/get the date/time. The only potential source of confusion is that time is defined as minutes since midnight and if you invoke setDate() this won’t work.  
  
  
On the other hand if you use the Date And Time spinner then the Date object you use will have the time embedded in it and you should not use the time related functions.  
  
  
  
  
  
  
  
  
The second API is the native calls we use for the pickers itself, they are in Display where we provide the methods:  
  
isNativePickerTypeSupported(int type) & showNativePicker(int type, Component source, Object currentValue, Object data). 

  
There are currently 3 picker type constants in Display:  
  
PICKER_TYPE_DATE_AND_TIME, PICKER_TYPE_TIME, PICKER_TYPE_DATE.  
  
  
So to show a native picker you must first verify that the picker type is supported then just show it, a source component would be important for tablets where we use a popup dialog instead of a regular dialog.  
  
  
  
The value type depends on the picker type, for date/date and time a java.util.Date object is expected. For time and Integer value with minutes since midnight is expected.  

  
The API is blocking like typical dialog API’s and will return with the value when finished.  
  

* * *

Notice: This post was automatically converted using a script from an older blogging system. Some elements might not have come out as intended…. If that is the case please let us know via the comments section below.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
