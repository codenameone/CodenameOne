---
title: Questions of the Week XIV
slug: questions-of-the-week-xiv
url: /blog/questions-of-the-week-xiv/
original_url: https://www.codenameone.com/blog/questions-of-the-week-xiv.html
aliases:
- /blog/questions-of-the-week-xiv.html
date: '2016-07-14'
author: Shai Almog
---

![Header Image](/blog/questions-of-the-week-xiv/qanda-friday.jpg)

With the pending release and some important issues we decide to make a minor update to the plugin today  
to introduce some improvements. We want the GUI builder to stabilize so we can finally crown it as “the” GUI  
builder rather than as “the new” GUI builder. To do that we need you guys to use it and submit issues, we also  
need you to use the latest version…​

With this release we added a lot of bug fixes and a few minor cosmetic improvements that should help narrow  
the gap with the old GUI builder. We are in the final stretch for 3.5 which means this is the time where features  
might be dropped. The biggest risk right now is the new build servers, we really want to do this before the 3.5  
release but with our current workload this might get delayed to September.

On stackoverflow things have been going on as usual:

### How to add Google Map Container to GUI builder on Codename One?

When asking about the gui builder you need to be very clear about which version you are using, the old or the new GUI builder.

[Read on stackoverflow…​](http://stackoverflow.com/questions/38382005/how-to-add-google-map-container-to-gui-builder-on-codename-one)

### How do I get my button to redirect to a web page within the app?

Generally you need to use the `BrowserComponent` but obviously there is more nuance than that…​

[Read on stackoverflow…​](http://stackoverflow.com/questions/38364640/how-do-i-get-my-button-to-redirect-to-a-web-page-within-the-app)

### Databases in Codename One

Storage on mobile is quite different to desktop/server so tools such as MySQL are just not available to developers…​

[Read on stackoverflow…​](http://stackoverflow.com/questions/38387821/databases-in-codename-one)

### UWP issue

UWP still has a lot of bugs, it will be at alpha/beta grade for the 3.5 release as it’s still a completely new port.  
It’s even new for Microsoft with whom we are experiencing a long conversation trying to get apps thru the store  
approval process.

[Read on stackoverflow…​](http://stackoverflow.com/questions/38361175/codenameone-uwp-issue)

### How Do I override button appearance in Codename One?

You need to override every element you want to control, the biggest pitfall for most developers is overriding border.  
You need to set it to empty if you don’t want a border…​

[Read on stackoverflow…​](http://stackoverflow.com/questions/38359710/how-do-i-override-button-appearance-in-codename-one)

### Math class functionality in cn1

A lot of the Codename One functionality is within com.codename1 packages to avoid the complexity of re-implementing/testing  
for every VM we support.

[Read on stackoverflow…​](http://stackoverflow.com/questions/38357636/math-class-functionality-in-cn1)

### How to integrate ads to android app in codename one

Providing a link to the specific ad library you used when you ask the original question often helps…​

[Read on stackoverflow…​](http://stackoverflow.com/questions/38344946/how-to-integrate-ads-to-android-app-in-codename-one)

### Open dialog disappears after screen turns off

Suspend/Resume behavior when the screensaver kicks in is really difficult to understand and follow sometimes.

[Read on stackoverflow…​](http://stackoverflow.com/questions/38339018/open-dialog-disappears-after-screen-turns-off)

### iOS build failure upon using CN1Bluetooth

Here is another reason to speed up the migration to xcode 7.x…​

[Read on stackoverflow…​](http://stackoverflow.com/questions/38334860/ios-build-failure-upon-using-cn1bluetooth/38383066#38383066)

### How do I apply multiple layouts in Codename One?

Nesting of containers is a concept that’s pretty hard to understand in all frameworks and not just Codename One…​

[Read on stackoverflow…​](http://stackoverflow.com/questions/38334414/how-do-i-apply-multiple-layouts-in-codename-one)

### Actionlistener in the command of the side menu opens blank form but the same in the button action listener opens the form normally

Sometimes layouts act out in odd ways, 9 times out of 10 it’s an EDT violation but there are odd edge cases where it’s not.

[Read on stackoverflow…​](http://stackoverflow.com/questions/38334199/actionlistener-in-the-command-of-the-side-menu-opens-blank-form-but-the-same-in)

### Toolbar search customization

We didn’t expose ways to customize the search functionality too much but because the toolbar is so darn  
flexible you can easily customize it yourself and we still have the sample that predated the builtin search.

[Read on stackoverflow…​](http://stackoverflow.com/questions/38333678/toolbar-search-customization)

### How do I avoid Android always for Bluetooth permission when running BTDemo

Bluetooth prompts are shown based on specific calls into the library…​

[Read on stackoverflow…​](http://stackoverflow.com/questions/38331028/how-do-i-avoid-android-always-for-bluetooth-permission-when-running-btdemo)

### setPreferredSize alernative calling for TextBox widths and heights not working

When you need to position/size a component think layout first and component size last. Once you get the layout right  
everything else should be trivial.

[Read on stackoverflow…​](http://stackoverflow.com/questions/38301423/setpreferredsize-alernative-calling-for-textbox-widths-and-heights-not-working)

### Issue on setting shadow in the container using nine piece border wizard

Cutting 9-piece images and understanding how to map the design to the actual component hierarchy is something  
we still debate after 10 years of working with this API…​

[Read on stackoverflow…​](http://stackoverflow.com/questions/38290355/issue-on-setting-shadow-in-the-container-using-nine-piece-border-wizard)

### How do I create an action for a button that I created in the GUI Builder on CodenameOne?

Hopefully the migration to the new GUI builder will make this process simpler.

[Read on stackoverflow…​](http://stackoverflow.com/questions/38287681/how-do-i-create-an-action-for-a-button-that-i-created-in-the-gui-builder-on-code)

### Macros dont working with POJOs

The JSON/POJO mapping thru mirah is a one of those tools I’m totally unfamiliar with…​

[Read on stackoverflow…​](http://stackoverflow.com/questions/38285490/macros-dont-working-with-pojos)

### Changing the width of hamburger menu

Theme constants are a great tool that you can also use within your own code to control dynamic application functionality.

[Read on stackoverflow…​](http://stackoverflow.com/questions/38279047/changing-the-width-of-hamburger-menu)

### List scroll to the selected item

A picture is worth a thousand words, had I had a picture in that question when it was initially asked this would have  
been much easier…​

[Read on stackoverflow…​](http://stackoverflow.com/questions/38261290/codename-one-list-scroll-to-the-selected-item)

### Open picture on iOS with execute()

In retrospect the canExecute() method states is too confusing to most developers who often misinterpret the null  
value returned.

[Read on stackoverflow…​](http://stackoverflow.com/questions/38254395/codename-one-open-picture-on-ios-with-execute)

### Text not showing using Roboto or Keep Calm Medium font on iOS

Fonts work very differently between Android/iOS and we make a great deal of effort trying to make them behave  
“seamlessly”. This seamlessness works when we have control (e.g. the theme) but is somewhat broken in the lower  
level API’s.

[Read on stackoverflow…​](http://stackoverflow.com/questions/38253008/codename-one-text-not-showing-using-roboto-or-keep-calm-medium-font-on-ios)

### Date gives different results on iOS and Android

You shouldn’t normally rely on the `toString()` method semantics. To be fair we should try and make them identical to other  
Java platforms but it is still not the right way to work with dates…​

[Read on stackoverflow…​](http://stackoverflow.com/questions/38249132/codename-one-date-gives-different-results-on-ios-and-android)

### App logs on production

I’m not 100% sure I get the question but generally you can listen to exceptions on the event dispatch thread which  
is a feature many developers aren’t aware of…​

[Read on stackoverflow…​](http://stackoverflow.com/questions/38245383/app-logs-on-production)

### Can swift code be used in Codename One native code instead of Objective-C

There is a long answer with quite a lot of detail but the bottom line is: probably. But there is no reason to do that…​

[Read on stackoverflow…​](http://stackoverflow.com/questions/38241771/can-swift-code-be-used-in-codename-one-native-code-instead-of-objective-c)

### Can the arguments be named as desired when using native code?

Yes for platforms other than iOS. Objective-C considers argument names to be a part of the method signature so if  
you change the names everything will fail. That’s why we named them in this particular way.

[Read on stackoverflow…​](http://stackoverflow.com/questions/38241420/can-the-arguments-be-named-as-desired-when-usign-native-code-in-codename-one)

### How do you show a video in it’s correct dimensions?

Sizing peer and native components especially ones that require loading such as video or html is really problematic.  
That’s why we always recommend placing them in the center of the border layout (with the default setting). This  
stretches them over the entire available space so their preferred size will be irrelevant.

[Read on stackoverflow…​](http://stackoverflow.com/questions/38241168/how-do-you-show-a-video-in-its-correct-dimensions-in-codenameone)

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
