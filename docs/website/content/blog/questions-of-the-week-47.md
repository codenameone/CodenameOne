---
title: Questions of the Week 47
slug: questions-of-the-week-47
url: /blog/questions-of-the-week-47/
original_url: https://www.codenameone.com/blog/questions-of-the-week-47.html
aliases:
- /blog/questions-of-the-week-47.html
date: '2017-03-16'
author: Shai Almog
---

![Header Image](/blog/questions-of-the-week-47/qanda-friday2.jpg)

Thank you all for the great response to the bootcamp and especially those of you who signed up. Registration is closing in about 2 hours and we have one last spot left! I’m sure we’ll put out some incredible showcase apps around May/June.  
Despite all my efforts going into the bootcamp things have been moving fast in Codename One development.

Steve add a new blog post the other day detailing the new [ComponentSelector API](/blog/jquery-css-style-selectors-for-cn1.html). It was actually committed last week but we didn’t have room in the blog schedule until this week. He also posted about the big changes he made to the [Google Maps support](/blog/new-improved-native-google-maps.html) and how those can be used with z-ordered peers to make Uber style applications a reality in Codename One.

Today’s update doesn’t change that much in terms of functionality, just a few bug fixes and refinements. Next week will be have the last Friday update for a while (which means Codename One updates won’t happen for a while) as I go into the bootcamp and I don’t want to throw all of my tasks onto the team. I think they will have their hands full just covering the level of support work.

![Diamond](/blog/questions-of-the-week-47/diamond-rank.png)

Before I go into the stack overflow section I’d like to mention that I recently looked in our [stackoverflow leaderboard](http://stackoverflow.com/tags/codenameone/topusers) and noticed Diamond surpassed me by a pretty big margin!

I’m totally thrilled by this and hope many of you will follow suit. I think an answer provided by one of you guys is far more valuable than one provided by me. It’s also a great way to learn Codename One, when I started teaching programming courses back in the 90’s my skill level skyrocketed…​

Besides that we also had quite a few interesting posts worth mentioning:

[Tim](http://stackoverflow.com/users/4438165/timgallagher) asked about [bluetooth notification](http://stackoverflow.com/questions/42814133/bluetooth-notification-and-android-gui-updates). The interesting bit about this should extend beyond bluetooth.  
The bluetooth API accepts a callback action listener which Tim assumed would be invoked on the EDT. It’s a good assumption to make and not easy to debug because bluetooth currently only works on the device itself. Unfortunately the library didn’t do that and just invoked `actionPerformed` directly from what appears to be the native thread. This can cause huge problems such as deadlocks which is why we try to always hide the native thread…​

So this is really for library authors: If you need to expose the native thread do it in a dedicated callback and don’t use action listener (see the should navigate feature of the `BrowserComponent` which does just that). Otherwise users might fail in a painful way.

[Diamond](http://stackoverflow.com/users/2931146/diamond) gave a great answer to a question about [changing a text field in runtime](http://stackoverflow.com/questions/42807405/codaneame-one-mask-and-unmask-of-the-password-filed-not-working-in-ios). This is a feature that’s probably pretty important as inputing passwords on a mobile device is always painful/tedious.

[AL](http://stackoverflow.com/users/4625829/al) & I answered [this question](http://stackoverflow.com/questions/42783406/in-codename-one-which-is-supported-either-gcm-or-fcm) almost at the same time. Push is implemented in GCM but we can switch to FSM without most of you noticing the difference.

[Max](http://stackoverflow.com/users/6834956/max-r) asked about the [device calendar cn1lib](http://stackoverflow.com/questions/42762183/codename-one-use-google-calendar), I gave some details on this almost undocumented library and [Diamond](http://stackoverflow.com/users/2931146/diamond) filled in some blanks. There are other issues with the calendar on iOS as well as on Android with the new permission based system. If this will see usage it might be worth it to refresh this library as it’s a bit out of date.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
