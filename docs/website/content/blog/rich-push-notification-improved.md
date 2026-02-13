---
title: Rich Push Notifications Improved
slug: rich-push-notification-improved
url: /blog/rich-push-notification-improved/
original_url: https://www.codenameone.com/blog/rich-push-notification-improved.html
aliases:
- /blog/rich-push-notification-improved.html
date: '2019-02-04'
author: Shai Almog
---

![Header Image](/blog/rich-push-notification-improved/new-features-3.jpg)

Last week Steve committed the final piece of the [rich push notification support RFE](https://github.com/codenameone/CodenameOne/issues/2208). This commit introduces support for replies in push messages. This came too late for the [whatsapp clone](https://www.codenameone.com/blog/whatsapp-clone.html) but if you want to build an app of this type you would need this feature.

The app main class should implement `PushActionProvider`. This defines a method that returns a set of categories. E.g.
    
    
    public PushActionCategory[] getPushActionCategories() {
        return new PushActionCategory[]{
            new PushActionCategory("fo", new PushAction[]{
                new PushAction("yes", "Yes"),
                new PushAction("no", "No"),
                new PushAction("maybe", "Maybe", null, "Enter reason", "Reply")
            })
    
        };
    }

Then, when sending a push notification, you can specify the “category” of the message. If the category corresponds with a defined category in your `getPushActionCategories()` method, then the user will be presented with a set of buttons corresponding to the PushActions in that category.

In the above example, we would send a push type 99 and a body of
    
    
    <push type="0" body="Hello" category="fo"/>

This would trigger the “fo” category that we defined, which has 3 actions: Yes, No, and Maybe. And the “Maybe” action will provide a text input because of the extra parameters provided:
    
    
    new PushAction("maybe", "Maybe", null, "Enter reason", "Reply")

The last 2 parameters are the “hint” text and the reply button label. On android, the notification will look like this.

![Push Reply](/blog/rich-push-notification-improved/push-replies.png)

Figure 1. Push Reply

If you click on “Maybe” (with Android API level 27 or higher which is the default), then you’ll get a text field to enter a reply directly.

You can retrieve both which action was pressed, and what the user text input was using the `PushContent` class.

An example push callback method to retrieve this data:
    
    
    public void push(String value) {
        PushContent data = PushContent.get();
        if (data != null) {
            Log.p("Image URL: "+data.getImageUrl());
            Log.p("Category: "+data.getCategory());
            Log.p("Action: "+data.getActionId());
            Log.p("Text Response: "+data.getTextResponse());
        } else {
            Log.p("PushContent is null");
        }
        Log.p("Push "+value);
        Display.getInstance().callSerially(()->{
            Dialog.show("Push received", value, "OK", null);
        });
    }
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Ch Hjelm** — May 21, 2019 at 11:12 am ([permalink](https://www.codenameone.com/blog/rich-push-notification-improved.html#comment-24062))

> This is a great feature! Is it possible to use the rich push features with *local* notifications (described here[](<https://www.codenameone.com/blog/local-notifications.html>)? E.g. is it possible to call the `PushContent.get()` from within the callback `localNotificationReceived(String notificationId)`?
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Frich-push-notification-improved.html)


### **Shai Almog** — May 22, 2019 at 9:33 am ([permalink](https://www.codenameone.com/blog/rich-push-notification-improved.html#comment-24109))

> Shai Almog says:
>
> No. Unfortunately they are completely separate features that have no connection between them at this time.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Frich-push-notification-improved.html)


### **Ch Hjelm** — May 22, 2019 at 4:45 pm ([permalink](https://www.codenameone.com/blog/rich-push-notification-improved.html#comment-24097))

> Ch Hjelm says:
>
> That’s a pity, i thought local and ‘remote’ push notifications shared the same mechanics, so I hoped there was a synergy. I use local notifications for on-device popups when the app is not active, and the ‘rich’ features would be perfect to enhance that. Is it possible that the ‘rich’ features might become available for local notifications some day?
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Frich-push-notification-improved.html)


### **Shai Almog** — May 23, 2019 at 5:05 am ([permalink](https://www.codenameone.com/blog/rich-push-notification-improved.html#comment-24116))

> Shai Almog says:
>
> Unless an enterprise user explicitly asks for this I don’t see this happening. This was a very hard to implement RFE that took forever. We’re already backlogged with enterprise RFE’s so even if this was requested by an enterprise user it would probably take a lot of time to resolve as it’s a big task.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Frich-push-notification-improved.html)


### **Rochana Sawatzky** — July 9, 2020 at 2:04 pm ([permalink](https://www.codenameone.com/blog/rich-push-notification-improved.html#comment-24293))

> [Rochana Sawatzky](https://avatars3.githubusercontent.com/u/25064795?v=4) says:
>
> I’ve managed to get Push working on both Android and iOS, however my Android notifications come as silent notifications. Is there a setting where I can change them to be regular notifications?
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Frich-push-notification-improved.html)


### **Shai Almog** — July 10, 2020 at 4:12 am ([permalink](https://www.codenameone.com/blog/rich-push-notification-improved.html#comment-24292))

> Shai Almog says:
>
> It seems to be an issue with Android 10: <https://support.google.com/pixelphone/thread/14478025?hl=en>  
> We’re still not sure if this is something that we can handle/hide or if it’s something that Google must fix.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Frich-push-notification-improved.html)


### **Rochana Sawatzky** — July 11, 2020 at 4:39 am ([permalink](https://www.codenameone.com/blog/rich-push-notification-improved.html#comment-24017))

> [Rochana Sawatzky](https://avatars3.githubusercontent.com/u/25064795?v=4) says:
>
> Good to know, thank you!
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Frich-push-notification-improved.html)

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
