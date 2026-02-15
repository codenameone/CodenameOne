---
title: Questions of the Week X
slug: questions-of-the-week-x
url: /blog/questions-of-the-week-x/
original_url: https://www.codenameone.com/blog/questions-of-the-week-x.html
aliases:
- /blog/questions-of-the-week-x.html
date: '2016-06-16'
author: Shai Almog
---

![Header Image](/blog/questions-of-the-week-x/qanda-friday.jpg)

This has been a tremendous week with a lot of news & next week will be even more hectic!  
Next week we will turn on the new iOS build servers by default which means a huge change is coming  
the biggest is probably [http connection problems](/blog/ios-http-urls.html) but I’m sure we’ll run into  
quite a few other issues…​

Please let us know immediately as you run into issues…​

Onwards to the activity on stackoverflow this week:

### IS there a way to obtain device screen width and height

This was marked as a duplicate despite being very explicitly defined as a Codename One question. I think  
the biggest takeaway is don’t call anything “urgent” on stackoverflow…​

[Read on stackoverflow…​](http://stackoverflow.com/questions/37859571/is-there-a-way-to-obtain-device-screen-width-and-height)

### Is glassPane painting model broken?

Surprisingly the answer is yes, we broke something when fixing  
[issue 1680](https://github.com/codenameone/CodenameOne/issues/1680) a while back and this should be fixed  
in todays release.

[Read on stackoverflow…​](http://stackoverflow.com/questions/37831571/is-glasspane-painting-model-broken)

### Searching for a good parse4cn11 alternative

I’m curious about that myself, I’d like to actually keep using parse4cn1 but map it to a different 3rd party server  
for simpler migration.

[Read on stackoverflow…​](http://stackoverflow.com/questions/37828674/searching-for-a-good-parse4cn11-alternative)

### WebSocket with multiple ClientEndpoints and binary messages

Turns out this was a server issue related to the websocket implementation there. I think we need to have better  
websocket demos/tutorials as this is picking up as a standard and is really appropriate for Codename One…​

[Read on stackoverflow…​](http://stackoverflow.com/questions/37819361/websocket-with-multiple-clientendpoints-and-binary-messages)

### Adding multiple multibuttons to a form after querying a webservice

When you change a form after it is shown you need to revalidate…​  
Another common mistake is to change the form from a network thread.

[Read on stackoverflow…​](http://stackoverflow.com/questions/37815337/adding-multiple-multibuttons-to-a-form-after-querying-a-webservice)

### Swipe the screens for next pages

[Tabs](/javadoc/com/codename1/ui/Tabs/) was designed with exactly that  
use case in mind…​

[Read on stackoverflow…​](http://stackoverflow.com/questions/37814686/swipe-the-screens-for-next-pages)

### App unfortunately stopped form codenameone with paypal integration

When you get that error it means you got an exception that you didn’t catch in the native code.

[Read on stackoverflow…​](http://stackoverflow.com/questions/37814316/app-unfortunately-stopped-form-codenameone-with-paypal-integration)

### Initialising subcomponents of a GUI form

We are in the process of moving to the new GUI builder which is mostly delay due to lack of documentation.  
We hope to release docs for this in the coming weeks.

[Read on stackoverflow…​](http://stackoverflow.com/questions/37810040/initialising-subcomponents-of-a-gui-form)

### Error in Native Interface

It’s often hard to tell what the error is without a call stack or more information.

[Read on stackoverflow…​](http://stackoverflow.com/questions/37808812/error-in-native-interface-in-codenameone)

### Log in with Facebook using Webview component

This was a rather long thread that stretched into several related questions, it turns out that the problem originates  
from a slightly older Facebook SDK using an approach Apple suddenly decided to reject.

Facebook updated the SDK and we already integrated the new SDK to support the new bitcode build mode.  
However, to preserve compatibility we limited the SDK only for that new version. So starting with todays  
release all builds are using the new Facebook SDK.

[Read on stackoverflow…​](http://stackoverflow.com/questions/37808517/codename-one-log-in-with-facebook-using-webview-component)

### Codename one Native IOS implementation error: .h file not found

This is related to the previous question which was eventually resolved by us but it might be interesting to developers  
working with native SDKs.

[Read on stackoverflow…​](http://stackoverflow.com/questions/37820511/codename-one-native-ios-implementation-error-h-file-not-found)

### How to include using Smartwatches in an app written with Codename One?

We don’t support smartwatches at this time although you should be able to add support for them using native code.  
We think supporting smartwatches/TV’s should be pretty easy but both depend on demand we get from developers  
which is currently non-existent. This might change overnight though.

[Read on stackoverflow…​](http://stackoverflow.com/questions/37807655/how-to-include-using-smartwatches-in-an-app-written-with-codename-one)

### React on Tree expansion or collapse

`Tree` is **very** customizeable, I would also recommend checking out the new  
[Accordion](/javadoc/com/codename1/components/Accordion/) component  
which we will cover more in depth in a post sometime next week…​

[Read on stackoverflow…​](http://stackoverflow.com/questions/37798624/codenameone-react-on-tree-expansion-or-collapse)

### Change color of checkbox in theme

The color of checkboxes in some themes is derived from images, we are trying to slowly de-emphasize that  
in favor of the builtin material design icon font which should make color inheritance much easier.

[Read on stackoverflow…​](http://stackoverflow.com/questions/37796869/codenameone-change-color-of-checkbox-in-theme)

### Can not access com.codename1.impl.android.AndroidNativeUtil From nativeInterface

You still need import statements when building the native code for an application…​

[Read on stackoverflow…​](http://stackoverflow.com/questions/37783366/can-not-access-com-codename1-impl-android-androidnativeutil-from-nativeinterface)

### Why the decreasing the png/jpg quality factor in ImageIO.save() does not decrease the output image file size?

It’s nice that people notice small things like that about the API, it was indeed unclear from the JavaDocs that  
this was acceptable by design.

[Read on stackoverflow…​](http://stackoverflow.com/questions/37775640/why-the-decreasing-the-png-jpg-quality-factor-in-imageio-save-does-not-decreas)

### Disable pull-to-refresh on InfiniteContainer in codenameone

That’s an easy answer, you can’t. For that we have the `InfiniteScrollAdapter` which is roughly of the same complexity  
level as the `InfiniteContainer`.

[Read on stackoverflow…​](http://stackoverflow.com/questions/37768901/disable-pull-to-refresh-on-infinitecontainer-in-codenameone)

### How to prevent west and East elements from overlapping center in BorderLayout

`BorderLayout` is often over simplistic when it comes to some UI styles, in these cases we need more elaborate  
layouts which we already have…​

[Read on stackoverflow…​](http://stackoverflow.com/questions/37758048/how-to-prevent-west-and-east-elements-from-overlapping-center-in-borderlayout)

### What is the best way to write a GUI in Codename One : Use the designer or code the GUI?

One of the motivations we have to switch to the new GUI builder is the fact that it will remove the need to decide  
up-front whether you want to code the UI with the GUI builder or by hand. You could decide this dynamically  
on a specific Form.

[Read on stackoverflow…​](http://stackoverflow.com/questions/37752567/what-is-the-best-way-to-write-a-gui-in-codename-one-use-the-designer-or-code-t)

### Is Codenameone geofencing locations limited to 1?

It’s not, but it is sometimes hard to find out exactly how many locations were bounds and what are the platform  
limits in regards to location binding.

[Read on stackoverflow…​](http://stackoverflow.com/questions/37751930/is-codenameone-geofencing-locations-limited-to-1)

### How to minimize gap between Tab icon and Tab text in Codename one?

There is a default padding within the `FontImage` class, the padding allows for a more whitespace which is  
often more visually pleasing. However, you can turn that off…​

[Read on stackoverflow…​](http://stackoverflow.com/questions/37748218/how-to-minimize-gap-between-tab-icon-and-tab-text-in-codename-one)

### Error during android build for native interface

Native interfaces are sometimes challenging especially if you just copy and paste code, this does need  
some native coding skills which is why we often recommend leaving the stubs and building the app. Then  
implementing the native code in the native IDE and copying the results back to Codename One.

[Read on stackoverflow…​](http://stackoverflow.com/questions/37745289/error-during-android-build-for-native-interface)

### CodenameOne APK is not zip aligned

Google broke Android builds again thru some changes to it’s build again, so we had to scramble to find the  
reason. One of the nice things about Codename One is that we have to do the scrambling after Google/Apple’s  
shenanigans for you…​

[Read on stackoverflow…​](http://stackoverflow.com/questions/37744198/codenameone-apk-is-not-zip-aligned)

### Specifying animations and Toolbar items in the resource editor

`Toolbar` didn’t exist during the days we developed the resource editor, we are bringing it more into the new  
GUI builder which will hopefully become our chief tool in the near future.

[Read on stackoverflow…​](http://stackoverflow.com/questions/37740067/specifying-animations-and-toolbar-items-in-the-resource-editor)

### Why a String drawn on a Graphics object change its position depending on the used skin?

This is a bit of a challenging thread, `drawString` is core to the platform and it’s hard to figure  
out what is the reason for the inconsistency n this thread.

[Read on stackoverflow…​](http://stackoverflow.com/questions/37739521/why-a-string-drawn-on-a-graphics-object-change-its-position-depending-on-the-use)

### Avoid app from closing on app switching

Controlling lifecycle in mobile is problematic, a lot of the assumptions that are true for desktop just don’t carry  
over…​

[Read on stackoverflow…​](http://stackoverflow.com/questions/37732323/avoid-app-from-closing-on-app-switching)

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
