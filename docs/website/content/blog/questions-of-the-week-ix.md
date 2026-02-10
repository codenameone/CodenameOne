---
title: Questions of the Week IX
slug: questions-of-the-week-ix
url: /blog/questions-of-the-week-ix/
original_url: https://www.codenameone.com/blog/questions-of-the-week-ix.html
aliases:
- /blog/questions-of-the-week-ix.html
date: '2016-06-09'
author: Shai Almog
---

![Header Image](/blog/questions-of-the-week-ix/qanda-friday.jpg)

This week has seen a lot of announcements and looking at the submission queue for next week it looks like  
it will be jam packed with updates and new features. Today we have a new plugin update which is jam packed  
with features and changes, the biggest of which is the removal of all the skins (they are now in the More menu  
item).

We also have a lot of great new features in this plugin including Windows UWP build preview, new settings UI &  
much more!

We’ll write more about all of those next week…​.

Stackoverflow was also brimming with great questions on a multitude of subjects.

### How to enhance a class in Codename One API

This is probably my favorite question of the week because it’s a tough question that is asked rather frequently.  
In this case it was asked in a proactive way (how can I do that, rather than why don’t you guys do that…​) which  
is really nice making it my favorite.

Unfortunately there are no easy answers!  
There is a reason why we shy away from adding too many VM level API’s, it’s damn hard to do.

[Read on stackoverflow…​](http://stackoverflow.com/questions/37624023/how-to-enhance-a-class-in-codename-one-api)

### Is there a way to save a Graphics object in Codename One without without taking a screenshot?

Graphics doesn’t always map to a surface which is why it doesn’t include capabilities such as `getPixel`.  
This allows us to use rather elaborate hardware acceleration when running on some platforms and also  
use two very different implementations of graphics (for screen drawing and mutable image drawing).

[Read on stackoverflow…​](http://stackoverflow.com/questions/37729384/is-there-a-way-to-save-a-graphics-object-in-codename-one-without-without-taking)

### Need to access native interface code (android paypal integration)

Adding native 3rd party activities is a pretty common task for native SDK integrations…​

[Read on stackoverflow…​](http://stackoverflow.com/questions/37724787/need-to-access-native-interface-code-android-paypal-integration-from-codename)

### How to add an unselectedTab icon in codename one?

Reading that question I tend to agree, the API naming here is really unintuitive.

[Read on stackoverflow…​](http://stackoverflow.com/questions/37724731/how-to-add-an-unselectedtab-icon-in-codename-one)

### Video Display in Codename One

Most issues with web/video or other peer components boil down to wrong layout. Since the preferred size of the  
media is 0 when we initially lay out the form it’s sized as 0 and stays that way…​ Using a different layout manager  
works around that.

[Read on stackoverflow…​](http://stackoverflow.com/questions/37722573/video-display-in-codename-one)

### Log into a NTLM server with CodeName One

We got asked about NTLM support in the past but no one has emphasized this as important. It’s doable but  
challenging, we could bake NTLM and other such capabilities directly into Codename One but to do that  
we need to see enterprise customer demand.

[Read on stackoverflow…​](http://stackoverflow.com/questions/37721347/log-into-a-ntlm-server-with-codename-one)

### How do I specify a degree character (U+00B0) in a Label

Our default encoding for source compilations was not UTF-8 for NetBeans/IntelliJ projects. We’ve changed this  
for the upcoming update but this answer is still interesting…​.

[Read on stackoverflow…​](http://stackoverflow.com/questions/37713504/how-do-i-specify-a-degree-character-u00b0-in-a-label)

### How should the result of getDeviceDensity() method from Codename One be used?

Working with multi-DPI devices can be quite challenging and the device density aspect is often murky…​

[Read on stackoverflow…​](http://stackoverflow.com/questions/37706794/how-should-the-result-of-getdevicedensity-method-from-codename-one-be-used)

### Is it possible to save a generated image in Codename One?

The answer is yes (thru `ImageIO`) but the question goes more into the details of how to create an image in  
terms of API’s…​

[Read on stackoverflow…​](http://stackoverflow.com/questions/37688930/is-it-possible-to-save-a-generated-image-in-codename-one)

### Provisioning profile does not match bundle identifier : Existing IOS bundle ID starts with a numeric

When we implemented Codename One we decided to tie package names to provisioning on iOS/Android/Windows etc.  
This simplified a lot of the underlying code and logic but introduced edge cases like this. Unfortunately, there  
are no easy answers for some problems…​

[Read on stackoverflow…​](http://stackoverflow.com/questions/37664935/provisioning-profile-does-not-match-bundle-identifier-existing-ios-bundle-id-s)

### Store inherited class object in codename one storage

Externalization has always been a challenging subject for developers, there are some common pitfalls  
that people fall into but it’s often messy to discover them the first time around. Caching is probably one  
of the bigger pains during debugging as it defers the error to a later point…​

[Read on stackoverflow…​](http://stackoverflow.com/questions/37663909/store-inherited-class-object-in-codename-one-storage)

### Best strategy for a magazine app

It’s always difficult to give advice on strategy/design. We can’t debug design and some things become obvious  
only after trying a design and discovering it’s flaws, that is why it is crucial to build applications that are robust  
enough to survive aggressive refactoring.

[Read on stackoverflow…​](http://stackoverflow.com/questions/37653426/best-strategy-for-a-magazine-app)

### What would cause the “time” picker to display an hour that is different in the selector

This question picked up a bug in Codename One with meridiem time. Notice it took 4 days for the asker to figure  
this out…​

If the question had included a test case this might have been resolved sooner.

[Read on stackoverflow…​](http://stackoverflow.com/questions/37636309/what-would-cause-the-time-picker-to-display-an-hour-that-is-different-in-the-s)

### Keypad decreases the screen Height issue

Centering elements in Codename One can be accomplished in several ways. I’m not a fan of the set paddings to  
position elements approach though…​

[Read on stackoverflow…​](http://stackoverflow.com/questions/37621432/keypad-decreases-the-screen-height-issue-codenameone)

### Database replication best-practices in codenameone

Storage is hard in mobile devices which has given rise to a cottage industry of on-device-databases.  
Some developers use sqlite but having used it for quite a few things I find it inherently quite flawed…​ We’d  
love to have a better story for on device database that is ideally also more cross platform than our current options.

[Read on stackoverflow…​](http://stackoverflow.com/questions/37619032/database-replication-best-practices-in-codenameone)

### Instant messaging in coenameone

We often have samples covering many different things, e.g. we have an actual IM app sample that answers  
this question:

[Read on stackoverflow…​](http://stackoverflow.com/questions/37611500/instant-messaging-in-coenameone)

### Restrict user to record video for 3 seconds only

There are some things you can only do in native code at this time. Actually implementing a “proper” camera  
view for most OS’s should be pretty trivial with a native interface and peer components. However, it would  
require some understanding of the underlying OS semantics.

[Read on stackoverflow…​](http://stackoverflow.com/questions/37608560/restrict-user-to-record-video-for-3-seconds-only-in-codename-one)

### Create a CN1 JSON Array from POJO for Jackson

A lot of Codename One developers don’t even know about this great library from Steve, it’s pretty cool to see  
developers finding out about it and figuring things for themselves!

[Read on stackoverflow…​](http://stackoverflow.com/questions/37596021/create-a-cn1-json-array-from-pojo-for-jackson)

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
