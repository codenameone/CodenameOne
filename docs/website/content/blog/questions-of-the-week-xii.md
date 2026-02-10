---
title: Questions of the Week XII
slug: questions-of-the-week-xii
url: /blog/questions-of-the-week-xii/
original_url: https://www.codenameone.com/blog/questions-of-the-week-xii.html
aliases:
- /blog/questions-of-the-week-xii.html
date: '2016-06-30'
author: Shai Almog
---

![Header Image](/blog/questions-of-the-week-xii/qanda-friday.jpg)

June just ended and we are starting the final stretch to get 3.5 ready, there is a lot on our  
table right now so most of the work in the coming month will probably focus on stabilizing the GUI builder and  
fixing the remaining issues for 3.5.

One thing we pushed to todays release in the last minute is a new API called: `Toolbar.setCenteredDefault(boolean)`.  
This was triggered in part by a  
[stackoverflow question](http://stackoverflow.com/questions/38065436/how-can-i-prevent-my-title-text-from-getting-misaligned)  
below but is a recurring theme. The `Toolbar` used to center it’s title thru the alignment style, which is problematic  
as it isn’t centered relatively to the commands on the sides. We now center it properly but this might not be what  
you want so you can disable this functionality by calling `Toolbar.setCenteredDefault(false);`.

On stackoverflow things are relatively calm:

### How to make the app use the whole screen height (remove the Toolbar)

If you don’t add commands and set the title to blank the `Toolbar` won’t show up. There will be additional spacing  
for the `StatusBar` on iOS but you can style that to padding 0.

[Read on stackoverflow…​](http://stackoverflow.com/questions/38113977/how-to-make-the-app-use-the-whole-screen-height-remove-the-toolbar-in-codename)

### Adding firebase to Codename One

This was asked a few times so I fired a question at the guys from codapps who might have some old demo code lying around…​

[Read on stackoverflow…​](http://stackoverflow.com/questions/38113048/adding-firebase-to-codename-one)

### How to Store an Array of JSON data in Persistent Memory and Read it Back

There are many ways to accomplish that…​

[Read on stackoverflow…​](http://stackoverflow.com/questions/38097931/codename-one-how-to-store-an-array-of-json-data-in-persistent-memory-and-read)

### getAppHomePath() seems to throw a wrong path in Simulator?

`Storage` & `FileSystemStorage` are very confusing and this is compounded by the simulator that somewhat mixes  
them in the same directory hierarchy. You need to use one or the other for a specific task!

[Read on stackoverflow…​](http://stackoverflow.com/questions/38096286/getapphomepath-seems-to-throw-a-wrong-path-in-simulator)

### GPS fail if not on top

Location in the background is a special case that requires special permissions and a different apparoach to the API…​

[Read on stackoverflow…​](http://stackoverflow.com/questions/38085499/codenameone-gps-fail-is-not-on-top)

### Adding jar files to codenameone in NetBeans

Integrating 3rd party libraries for which there isn’t a cn1lib wrapper yet will probably always be a bit tricky…​ There  
is a big section on this in the developer guide and Steve did some videos but it’s still not easy.

[Read on stackoverflow…​](http://stackoverflow.com/questions/38073770/adding-jar-files-to-codenameone-in-netbeans)

### How can I prevent my title text from getting misaligned?

This is something we now fixed by default so it should no longer be an issue…​

[Read on stackoverflow…​](http://stackoverflow.com/questions/38065436/how-can-i-prevent-my-title-text-from-getting-misaligned)

### How can I center a SpanLabel’s text in CodenameOne?

`SpanLabel` like many other composite components uses separate UIID’s for the various elements within it.

[Read on stackoverflow…​](http://stackoverflow.com/questions/38065384/how-can-i-center-a-spanlabels-text-in-codenameone)

### CodenameOne calander event for next/previous month arrow

The `Calendar` component is a bit long in the tooth by now, we don’t use it as much with the picker working natively.

[Read on stackoverflow…​](http://stackoverflow.com/questions/38055781/codenameone-calander-event-for-next-previous-month-arrow)

### One Drive Multipart upload error HTTP 400 Bad Request

This is more of a One Drive question than a Codename One question so there isn’t much we can do here. One  
of the nice things in SO though is the fact that you can get answers from everyone at one place…​

[Read on stackoverflow…​](http://stackoverflow.com/questions/38050903/one-drive-multipart-upload-error-http-400-bad-request)

### Image always missing in the received message while sharing an image

Like many issues this was multi-layered but it seems that there is also an issue with the Codename One share  
button. The share button handles it’s action performed before it’s listeners had time to react. By enclosing it  
in a call serially we were able to workaround the bug mentioned here.

[Read on stackoverflow…​](http://stackoverflow.com/questions/38042512/image-always-missing-in-the-received-message-while-sharing-an-image-in-codename)

### How can I get an Android device current SDK level in a native interface

Native interfaces are harder to debug, we always suggest building a native project and debugging them there  
otherwise you might be mislead that something isn’t working as expected.

[Read on stackoverflow…​](http://stackoverflow.com/questions/38035545/how-can-i-get-an-android-device-current-sdk-level-in-a-native-interface)

### How to include Adobe Analytics in CodenameOne

I don’t really know…​ Those are often the harder questions we get as I’m obviously not an expert in the 3rd party  
library discussed.

[Read on stackoverflow…​](http://stackoverflow.com/questions/37988768/how-to-include-adobe-analytics-in-codenameone)

### Why do I get a different behavior in Codename One simulator than on a real Android device?

This was one of those questions that kept coming up again and again from the same persistent user, unfortunately  
I initially discarded the issue as unlikely, then I wasn’t able to reproduce it properly until finally I had a working  
test case…​ It turns out the simulator used incorrect coordinates for mutable images when it was running in scale  
mode. Persistence pays off…​

[Read on stackoverflow…​](http://stackoverflow.com/questions/37986722/why-do-i-get-a-different-behviour-in-codename-one-simulator-than-on-a-real-andro)

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
