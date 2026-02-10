---
title: New Website, SSO & Payment Processing
slug: new-website-sso-and-payment-processing
url: /blog/new-website-sso-and-payment-processing/
original_url: https://www.codenameone.com/blog/new-website-sso-and-payment-processing.html
aliases:
- /blog/new-website-sso-and-payment-processing.html
date: '2020-11-06'
author: Shai Almog
---

![Codename One - New Website, SSO and new Payment Processing](https://beta.codenameone.com/wp-content/uploads/2020/11/Single-Sign-On.jpg)

Welcome to our new website. There are a lot of things that are still being worked on so please bare with us and let us know ASAP when something is broken. Comments are still disabled in the current site but we’ll import them and turn them on in the next few weeks.

With the new website we’re migrating to a **Single Sign-On solution** based on**Keycloak**. This means login for this website will go through that system. Right now it’s disconnected from our existing Codename One Build login but we intend to unify everything under Keycloak and your account would merge seamlessly (assuming you used the same email address). 

### Goodby Paypal. Hello Paddle!

One of the biggest complaints people have about signing up for Codename One’s paid subscription is our use of Paypal. To be fair, Paypal is pretty amazing. They have unparalleled global service and are pretty easy to integrate/work with. 

But there are a lot of downsides such as limited ability to manage the payment settings, trigger payment, upgrade/downgrade etc. When developers ask us for help, we also have limited capabilities as Paypal manages their own system. Then there’s the matter of invoicing which becomes difficult for the various locales.

### Enter Paddle

Paddle solves most of these problems. It’s also simple to integrate and it works as a reseller not as a billing service. So effectively, you’re buying our service from a 3rd party reseller who handles invoicing and all the complexities. That means you can use your local currency and get local invoices without a problem.

It also gives us the power to fix your account.

Want to upgrade/downgrade?  
No more waiting to the end of the month, our support team in the website chat can do that immediately.

Right now upgrade/downgrade isn’t self service yet, but we’ll hopefully add that in the future. 

**Notice that Paypal is one of the supported payment methods for Paddle so you can still use your Paypal account if you choose to.**

Since we’re in a transition period, Paddle is used when subscribing through the website and Paypal is used when subscribing via the dashboard. We’ll fix everything to use Paddle and keycloak SSO in the near future.

### Existing Paypal Subscriptions

If you have an existing Paypal/SWIFT subscription, everything should keep working as it did before. There should be no change and we won’t force you to migrate.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
