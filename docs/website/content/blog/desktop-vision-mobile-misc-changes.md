---
title: Desktop, Vision Mobile & Misc Changes
slug: desktop-vision-mobile-misc-changes
url: /blog/desktop-vision-mobile-misc-changes/
original_url: https://www.codenameone.com/blog/desktop-vision-mobile-misc-changes.html
aliases:
- /blog/desktop-vision-mobile-misc-changes.html
date: '2014-02-04'
author: Shai Almog
---

![Header Image](/blog/desktop-vision-mobile-misc-changes/desktop-vision-mobile-misc-changes-1.png)

  
  
  
  
![Picture](/blog/desktop-vision-mobile-misc-changes/desktop-vision-mobile-misc-changes-1.png)  
  
  
  

Vision mobile just released their new developer economics study, you can check it out  
[  
here  
](http://www.visionmobile.com/DE1Q14CodenameOne)  
.  
  
  
  
  
  
  
  
  
We toggled the  
[  
new pipeline mode  
](http://www.codenameone.com/3/post/2014/02/a-new-pipeline-for-windows-phone.html)  
for windows  
  
phone to be the default, its clearly the way to go forward in the long run since we just don’t have any other choice. All feedback on the new pipeline found it to improve performance significantly and generally reduces some of the paint issues that we run into with Windows Phone. However, there are a couple of reports pointing out that font quality has degraded with the new pipeline. We are looking into this but came to the decision that even if this isn’t solvable we will stick with the new pipeline since the old one is just not viable for moving forward.  
  
  
  
  
  
As part of our ongoing work with the new Android pipeline we added an experimental rendering mode that tries to automatically detect invisible layers and not draw them. You can enable this mode by invoking   
  
Display.getInstance().setProperty(“blockOverdraw”, “true”);  
  
  
This might improve performance for some cases and might improve performance on platforms other than Android. We’d be interested to hear if this mode break stuff in your application.  
  
  
  
  
**  
Desktop Build Arguments  
**  
  
  
  
  
  
Our recent desktop build support allows pro users to send builds for desktop applications. These builds are actually highly customizable using the following build arguments:  
  
desktop.width – width of the window in pixels (defaults to 800)  
  
  
desktop.height – height of the window in pixels  
  
  
  
(defaults to 800)  
  
  
desktop.fullScreen – whether the application will run in full screen  
  
(true/false defaults to false)  
  
  
desktop.adaptToRetina – doubles the pixels for a high resolution display (true/false defaults to true)  
  
  
desktop.resizable – whether the window would be resizable (true/false defaults to true)  
  
  
desktop.theme – the theme to use as the native theme defaults to “native”. Can be “none” to indicate no theme or the name of any resource file within the bundle that you would like to use as a base without the .res extension. E.g. iPhoneTheme assuming you placed the iPhoneTheme.res file in your src directory.  
  
  
  
desktop.windowsOutput – the installer type you want for Windows can be exe or msi defaults to msi  
  
  
  
  
  
  
  

* * *

Notice: This post was automatically converted using a script from an older blogging system. Some elements might not have come out as intended…. If that is the case please let us know via the comments section below.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Anonymous** — February 6, 2014 at 6:04 am ([permalink](/blog/desktop-vision-mobile-misc-changes/#comment-21846))

> Anonymous says:
>
> Display.getInstance().setProperty(“blockOverdraw”, “true”); doesn’t seem to break anything on first sight.
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
