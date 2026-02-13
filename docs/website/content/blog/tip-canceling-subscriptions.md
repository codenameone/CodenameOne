---
title: 'TIP: Canceling Subscriptions'
slug: tip-canceling-subscriptions
url: /blog/tip-canceling-subscriptions/
original_url: https://www.codenameone.com/blog/tip-canceling-subscriptions.html
aliases:
- /blog/tip-canceling-subscriptions.html
date: '2018-11-26'
author: Shai Almog
---

![Header Image](/blog/tip-canceling-subscriptions/tip.jpg)

One of the things I like most about our subscription base is its solid nature. We still have a lot of subscribers in the $9 per month plan which we discontinued several years ago (it was so long ago I can’t find the relevant blog post anymore). That’s wonderful, it means people like our product and are with us for the long run.

However, flexibility is important too. The fact that subscriptions can be canceled easily is important. Canceling subscriptions is easy although we don’t have any control over that.  
When you sign up for a subscription you effectively define a PayPal recurring payment process associated with your account. When we get a payment we keep the account at that level.

To cancel the subscription you need to login to paypal and cancel the recurring payment there. If this isn’t obvious to you we can do that (although we can’t change anything about the subscription, only cancel it). Just use the chat and give us the account email and paypal email addresses (assuming they differ).

### Problematic Upgrades

We chose this approach for simplicity and security. This way we have no valuable credit card or billing information on our servers. This reduces their appeal to would be hackers. It also lets us focus on our business instead of billing etc.

One of the problems with this system is that we don’t have control over payment. We can’t fix mistakes and some things aren’t as seamless as they should be. One such thing is upgrades. If you upgrade your account or resubscribe on billing failure you can end up with two payments on the same account. These things are hard to detect due to some complex assumptions made in the system. So it’s crucial you cancel old payments when upgrading.

If you run into billing issues be sure to contact our support, using the chat button.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **baron** — December 1, 2018 at 10:27 am ([permalink](https://www.codenameone.com/blog/tip-canceling-subscriptions.html#comment-23984))

> baron says:
>
> i hope to create subscribe with specific thing e.g:  
> – i want just create app for android but the jar file is more 50MB  
> in this case i can select ok for Android Builds features in my subscribe and dispose another things i don’t need it .  
> -on anther hand the price is increase whenever increase select many of features.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Ftip-canceling-subscriptions.html)


### **Shai Almog** — December 2, 2018 at 7:16 am ([permalink](https://www.codenameone.com/blog/tip-canceling-subscriptions.html#comment-23957))

> Shai Almog says:
>
> The price is fixed additional features don’t change that. You can’t create apps with jars larger than 50mb see this [https://help.codenameone.co…](<https://help.codenameone.com/en-us/article/whats-the-jar-size-limit-1a9ujwp/>)
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Ftip-canceling-subscriptions.html)

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
