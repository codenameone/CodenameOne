---
title: Restaurant App Builder
slug: restaurant-app-builder
url: /blog/restaurant-app-builder/
original_url: https://www.codenameone.com/blog/restaurant-app-builder.html
aliases:
- /blog/restaurant-app-builder.html
date: '2017-06-13'
author: Shai Almog
---

![Header Image](/blog/restaurant-app-builder/full-stack-java-bootcamp.jpg)

In the bootcamp we didn’t build just one big app, we built two…​ Or infinity…​ The first app was a restaurant ordering system that allows you to pick dishes from a menu and add them to a shopping cart. The second app was an “app builder” that allows you to customize the first app and then generate a native app based on that for your specific restaurant.

I kept that under wraps because I wanted to do a big “launch” and release the app to the wild based on that but I ended up being so busy after the bootcamp completed that this just didn’t materialize. So instead of doing a big launch I’m doing the softest possible launch for this app. This is how the restaurant app looks, I cover its creation in the [upcoming courses](/blog/new-online-courses-coming-soon.html) too:

![Main menu selection](/blog/restaurant-app-builder/restaurant-app-main.png)

Figure 1. Main menu selection

![Edit order UI](/blog/restaurant-app-builder/restaurant-app-order.png)

Figure 2. Edit order UI

![Contact us form](/blog/restaurant-app-builder/restaurant-app-contact-us.png)

Figure 3. Contact us form

### What Next?

If you own a restaurant or know someone who does and is interested in this or giving feedback by joining the beta please let us know in the website chat. Just add the email there and we’ll follow up with you.

I’d like to make this into a real world application and part of doing that is gathering feedback from real users. Notice that billing is done thru braintree directly to your account. Since braintree only works in some countries you need to verify if works for yours, it doesn’t work for mine!

To check if you qualify see the braintree [faq](https://www.braintreepayments.com/faq), currently these are the supported countries: United States, Canada, Australia, Europe, Singapore, Hong Kong, Malaysia, and New Zealand.

Hopefully with good feedback this app will reach production grade and serve as a good tutorial plus help restaurants get more customers. Since the code is available within the upcoming courses you could adapt this concept to build app builders for any field or product whatsoever…​
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Roman H.** — June 21, 2017 at 3:40 pm ([permalink](https://www.codenameone.com/blog/restaurant-app-builder.html#comment-21851))

> Is this an open source project. Can we contribute ?


### **Shai Almog** — June 22, 2017 at 4:16 am ([permalink](https://www.codenameone.com/blog/restaurant-app-builder.html#comment-23431))

> No. It was developed as part of the bootcamp where we walked thru every step of the process.  
> It will soon be featured in the coming course material we are launching Monday


### **Amuche Chimezie** — June 23, 2017 at 2:40 pm ([permalink](https://www.codenameone.com/blog/restaurant-app-builder.html#comment-23404))

> Hi Shai, Is this available now? If so, how can one access the material?


### **Roman H.** — June 23, 2017 at 4:11 pm ([permalink](https://www.codenameone.com/blog/restaurant-app-builder.html#comment-23416))

> Roman H. says:
>
> Please point me to a page with that course, so I can gather further info for my self.


### **Shai Almog** — June 24, 2017 at 5:13 am ([permalink](https://www.codenameone.com/blog/restaurant-app-builder.html#comment-23571))

> Shai Almog says:
>
> We will post an announcement this Monday


### **Gareth Murfin** — July 5, 2020 at 6:33 am ([permalink](https://www.codenameone.com/blog/restaurant-app-builder.html#comment-24286))

> [Gareth Murfin](https://lh3.googleusercontent.com/a-/AOh14GiKSl5jm7N1Rsw8eobcYTOzEcg7dMk62FKKC_SboA) says:
>
> Hi Shai, could you post a link to the course that contains this app? I want to sign up 🙂 Also did you link up actual payment, can users of this app actually purchase anything with their credit card? And if not what needs to be done to complete that bit? Thanks.


### **Shai Almog** — July 6, 2020 at 5:37 am ([permalink](https://www.codenameone.com/blog/restaurant-app-builder.html#comment-24291))

> Shai Almog says:
>
> It’s here: <https://codenameone.teachable.com/p/build-real-world-full-stack-mobile-apps-in-java>
>
> The payment is implemented on top of the braintree cn1lib <https://www.codenameone.com/blog/braintree-paypal-cn1lib.html>


### **Gareth Murfin** — August 4, 2020 at 5:01 pm ([permalink](https://www.codenameone.com/blog/restaurant-app-builder.html#comment-24307))

> [Gareth Murfin](https://lh3.googleusercontent.com/a-/AOh14GiKSl5jm7N1Rsw8eobcYTOzEcg7dMk62FKKC_SboA) says:
>
> Thanks Shai, it’s a real shame braintree only operate in a few countries. Are there any other cn1 libs that we can use that operate all over? or even http based apis that could easily be used from other companies (requiring no lib).


### **Shai Almog** — August 5, 2020 at 2:36 am ([permalink](https://www.codenameone.com/blog/restaurant-app-builder.html#comment-24310))

> Shai Almog says:
>
> It accepts charges everywhere. However it (and stripe et. al) only work for companies in very limited locales.  
> The workaround is to open an account in the US/EU and use that for your billing. This can be done relatively easily using foreign banks operating in your country.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
