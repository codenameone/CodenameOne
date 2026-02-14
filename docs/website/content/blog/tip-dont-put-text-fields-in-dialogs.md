---
title: 'TIP: Don''t Put Text Fields in Dialogs'
slug: tip-dont-put-text-fields-in-dialogs
url: /blog/tip-dont-put-text-fields-in-dialogs/
original_url: https://www.codenameone.com/blog/tip-dont-put-text-fields-in-dialogs.html
aliases:
- /blog/tip-dont-put-text-fields-in-dialogs.html
date: '2017-06-12'
author: Shai Almog
---

![Header Image](/blog/tip-dont-put-text-fields-in-dialogs/tip.jpg)

Text input is a very special case. Besides mixing the native and Java code we also need to deal with the appearance of the virtual keyboard which doesn’t act consistently across platforms. This creates many complex edge cases that are just as problematic on native OS platforms as they are in Codename One.

When we show a virtual keyboard there are two main scenarios that can take place:

  * The screen can resize to show only the relevant area

  * Scrolling size can increase to allow us to scroll all the way down

Both of these are problematic in a Dialog. The dialog can’t be properly moved once it is shown. This means that a screen resize can leave the dialog without enough space. Increased scrolling might not be enough since a dialog starts from a cramped position.

This is a problem that is often observed in native Android applications (not as much on iOS) where a field is unreachable when the virtual keyboard is showing.

These issues apply to other UI types where the text field is stuck in a position that doesn’t work well with scrolling or resizing. So you should pay attention to the behavior of the app on resize. A good approach to testing this is rotating a phone simulator which limits the height by a similar ratio. If the field isn’t easily reachable when you rotate the phone you might have a problem with a virtual keyboard too.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Martin Grajcar** — November 28, 2018 at 7:06 pm ([permalink](https://www.codenameone.com/blog/tip-dont-put-text-fields-in-dialogs.html#comment-24071))

> Martin Grajcar says:
>
> I can see that you’re right, but I can’t see any good alternative. In a form of mine, there are some 3-5 groups of 4-8 fields, which some users may want to edit, but most of the time, they won’t. So I’m planning to let the user click on a group and then edit its fields. Popping up a dialog with 4-8 text edits seems to be the natural choice. What’s the alternative? A form, which “disposes” by `showBack` the previous form (in exactly the same state just with the changes applied)?
>



### **Shai Almog** — November 29, 2018 at 7:37 am ([permalink](https://www.codenameone.com/blog/tip-dont-put-text-fields-in-dialogs.html#comment-21581))

> Shai Almog says:
>
> Apps like Uber and other apps send you to a deeper form in the hierarchy where you can edit those specific fields. You can then save or cancel to return to the parent form. It’s a pretty common UI paradigm in mobile. You would still have the same state, you don’t need a dialog here as long as you keep the form instance.
>



### **Martin Grajcar** — November 29, 2018 at 10:30 pm ([permalink](https://www.codenameone.com/blog/tip-dont-put-text-fields-in-dialogs.html#comment-23978))

> Martin Grajcar says:
>
> I’ve tried it, and it’s really good, except for the need to pass a `Consumer<t> resultConsumer` instead of getting the result directly. But I guess, there’s no other way and I can get used to it.
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
