---
title: We're Back from Vacation
slug: back-vacation
url: /blog/back-vacation/
original_url: https://www.codenameone.com/blog/back-vacation.html
aliases:
- /blog/back-vacation.html
date: '2019-09-02'
author: Shai Almog
---

![Header Image](/blog/back-vacation/new-features-6.jpg)

Summer is finally over and the kids are going back to school/kindergarten so it’s time to go back to our regularly scheduled posts. I won’t be posting as often as before as I’ll dedicate more time for support/development activities but still there’s a lot to write about…​

During our time off we had a lot of changes, I’ll repeat a few big ones which you might have run into already and cover a few that might have gone under the radar.

### GCM Removal and FCM as Default

During the month of August Google finally removed their old GCM servers. We’ve prepared for this ages ago but this still took us a bit off guard. We were ready for the switch itself but there were still a couple of things we weren’t prepared for.

Users who still used the old style of push notifications (prior to the `google-services.json` file approach) had push messages blocked. That was expected.

__ |  You can read about the modern approach to push [here](/manual/push/)  
---|---  
  
Because that no longer works anyway we switched the default build mode to FCM. This solves an issue for developers who neglected to define the `android.messagingService=fcm` build hint (which you no longer need). However, this causes a build error if you don’t have that JSON file in place. You can get this to compile for now by explicitly stating the build hint `android.messagingService=gcm`. However, push won’t work if you do that since the Google run GCM push servers are no longer there. But it will compile which is a start.

To migrate to the new FCM approach check out the [developer guide section on push](/manual/push/).

### API Level 28 and HTTPS Requirement

We migrated the build servers to Android API level 28 as required by Google. This migration was a bit painful because Google changed the way clipping works under Android so we had to make some extensive changes to our rendering pipeline.

However, one thing we can’t mitigate is that Google now blocks HTTP connections (not HTTPS).This is generally a good practice and a requirement on iOS as well. However, if you have an HTTP URL you need to use you can do so with the build hint:
    
    
    android.xapplication_attr=android:usesCleartextTraffic="true"

### Component Inspector Enhancements

We implemented a couple of RFEs in the venerable component inspector, specifically [2695](https://github.com/codenameone/CodenameOne/issues/2695) and [1476](https://github.com/codenameone/CodenameOne/issues/1476).

You can now refresh the component tree and the selection would remain in place but more importantly you can select a component and it will be highlighted in the UI. This is very helpful as a debugging tool.

![Component inspector highlights current selection](/blog/back-vacation/component-inspector-selection-highlight.png)

Figure 1. Component inspector highlights current selection

### AutoCompleteTextComponent and TextComponentPassword

[Francesco Galgani](https://github.com/jsfan3) contributed an implementation of  
[AutoCompleteTextComponent](https://github.com/codenameone/CodenameOne/issues/2705) which allows you to use an auto-complete with the text component framework for a more fluid input UI.

He also contributed [TextComponentPassword](https://github.com/codenameone/CodenameOne/issues/2654) which is a password field with the same convention. It carries the more modern “show password” icon convention which is far more convenient than the old “double type” approach.

### Error Callbacks for URLImage

It’s hard to handle errors in `URLImage` objects. Because they are so “seamless” the point for exception handling is deep withing the class. To solve issue [2703](https://github.com/codenameone/CodenameOne/issues/2703) we had to do something different.

You can now use the static method `setExceptionHandler` on `URLImage`. It accepts the inner interface `ErrorCallback` which has a single method:
    
    
    public static interface ErrorCallback {
        public void onError(URLImage source, Exception err);
    }

So effectively you can do something like this:
    
    
    URLImage.setExceptionHandler((img, err) -> handleError());

### So Much More

I’ll write about other things in the coming weeks as this post is getting a bit long. There’s a lot to cover.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Francesco Galgani** — September 7, 2019 at 7:57 pm ([permalink](/blog/back-vacation/#comment-24112))

> The Component Inspector enhancements are very helpful! Thank you very much!
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
