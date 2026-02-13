---
title: Rich Push Notifications and Improved Validation
slug: tich-push-notification-improved-validation
url: /blog/tich-push-notification-improved-validation/
original_url: https://www.codenameone.com/blog/tich-push-notification-improved-validation.html
aliases:
- /blog/tich-push-notification-improved-validation.html
date: '2018-07-16'
author: Shai Almog
---

![Header Image](/blog/tich-push-notification-improved-validation/new-features-4.jpg)

Steve just implemented one of the [harder RFE’s](https://github.com/codenameone/CodenameOne/issues/2208) we had in a while. It isn’t finished but we can already try some of these features and you should be able to try some of these rich types of push messages.

The difficulty stems from the way these push messages are implemented differently in the native OS’s outside of the domain where we have full control. As part of that work our entire developer guide section related to push was rewritten here: <https://github.com/codenameone/CodenameOne/wiki/Push-Notifications>

It isn’t available yet in the developer guide as I’m still busy with the book and would need to do some work to incorporate it correctly but this is a huge leap forward for our push support.

We’ll have more announcements about this in the coming months.

### Validation on iOS

[Francesco Galgani](https://github.com/jsfan3) implemented this [pull request](https://github.com/codenameone/CodenameOne/pull/2475) which animates validation errors in `InputComponent` on top of the `Label`.

This was merged and looks great but it did create a regression in validation where regular text fields fail at the moment and throw an exception. We might push a hotfix on Thursday to fix that issue.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
