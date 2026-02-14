---
title: Location, iOS Beta Testing & Better Input
slug: location-ios-beta-testing-better-input
url: /blog/location-ios-beta-testing-better-input/
original_url: https://www.codenameone.com/blog/location-ios-beta-testing-better-input.html
aliases:
- /blog/location-ios-beta-testing-better-input.html
date: '2015-01-20'
author: Shai Almog
---

![Header Image](/blog/location-ios-beta-testing-better-input/location-ios-beta-testing-better-input-1.png)

  
  
  
  
![Picture](/blog/location-ios-beta-testing-better-input/location-ios-beta-testing-better-input-1.png)  
  
  
  

  
  
  
In recent versions of iOS Apple added the ability to  
[  
distribute  
](https://developer.apple.com/library/prerelease/ios/documentation/LanguagesUtilities/Conceptual/iTunesConnect_Guide/Chapters/BetaTestingTheApp.html)  
  
  
[  
beta versions of your application  
](https://developer.apple.com/library/prerelease/ios/documentation/LanguagesUtilities/Conceptual/iTunesConnect_Guide/Chapters/BetaTestingTheApp.html)  
to beta testers using tools they got from the testflight acquisition. We now support this with our crash protection pro feature, just use the build argument  
  
  
  
  
ios.testFlight=true and you can then submit your app to the store for beta testing. 

With iOS 8 Apple also introduced a requirement that’s important for you to be aware of if you use the location API. You need to state why you are using that API in the application meta-data. You can use the  

  
  
  
  
  
  
ios.locationUsageDescription build argument to describe the reason you need to obtain the users location data e.g.:  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
ios.locationUsageDescription  
  
  
  
  
=Allows us to offer you better deals when you are in the vicinity of our business 

Input in iOS is a bit more complex but I’d say somewhat better than Android since it allows you to scroll while editing by default. However, this input mode triggered some regressions for developers especially in cases where the app places a text field component at the bottom of the screen in a non-scrollable area. The default behavior is to fallback to the old editing mode and just scroll up the entire display which carries some issues with it.  
  
We now have a new API on form ”  

  
  
  
  
  
  
  
  
setFormBottomPaddingEditingMode” which allows you to hint that in such cases you would want to fallback to padding at the bottom of the form rather than shifting the entire screen. This still allows scrolling the screen as expected while editing. Currently this only has an effect on iOS but we’d like to modernize Android’s input to use similar semantics in the future. 

We also exposed the previously private stopEditing  

  
  
method in Display. This method is a companion method to editString and allows you to manually stop the editing in progress. This can be useful for cases such as filling a form where a user types into a field a social security number. You can detect a valid number, stop the editing and instantly move the user to the next relevant field using editString().  
  
  
  

* * *

Notice: This post was automatically converted using a script from an older blogging system. Some elements might not have come out as intended…. If that is the case please let us know via the comments section below.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Anonymous** — January 22, 2015 at 5:07 am ([permalink](https://www.codenameone.com/blog/location-ios-beta-testing-better-input.html#comment-22335))

> Anonymous says:
>
> Thanks, this setFormBottomPaddingEditingMode is a nice feature, but there seams to be a bug – scrolling works well when the next edited field is opened from keyboard native “Next” button, but when a lower field editing is opened by clicking on the field, then the field is not scrolled to make room for the keyboard. Interestingly in this case scrolling happens only after clicking on “Done” button.
>



### **Anonymous** — January 22, 2015 at 6:20 am ([permalink](https://www.codenameone.com/blog/location-ios-beta-testing-better-input.html#comment-22184))

> Anonymous says:
>
> I’m sorry for the previous misleading info. Actually the mentioned bug is not related to setFormBottomPaddingEditingMode, it happens always with ios.keyboardOpen=true.
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
