---
title: "Subscriptions, Paywalls and a Customer Center, and We Don't Take a Cut"
slug: subscriptions-and-paywalls-without-a-revenue-cut
url: /blog/subscriptions-and-paywalls-without-a-revenue-cut/
date: '2026-11-15'
author: Shai Almog
description: "Codename One Commerce now covers offerings, remotely configured paywalls, targeting and experiments, a customer center with refunds and retention offers, invites with referral rewards and virtual currency, across the App Store, Google Play, Amazon, Stripe and Paddle. We never touch your money and take no percentage of it."
feed_html: "<img src=\"https://www.codenameone.com/blog/subscriptions-and-paywalls-without-a-revenue-cut.jpg\" alt=\"Subscriptions and paywalls without a revenue cut\" /> Offerings, paywalls, experiments, a customer center and invites across five stores. Your money goes from the store to you, and we take no percentage of it."
series: ["release-2026-11-13"]
---

![Subscriptions and paywalls without a revenue cut](/blog/subscriptions-and-paywalls-without-a-revenue-cut.jpg)

If you sell subscriptions in an app, you already give Apple or Google up to 30%. The usual next step is a subscription platform, because store receipts are miserable to validate and a paywall you can't change without an app update is a paywall you'll never test. The best known of those platforms charge a percentage of your revenue on top of the store's. The more you earn, the more you pay them, for the same API calls.

A percentage is the wrong price for this. Validating a receipt costs the same whether it's for a $2 app or a $200 plan, so a fee that grows with your revenue is a tax on success. In July we shipped [Commerce](/blog/commerce-secrets-without-iap-tax/) as receipt validation and entitlements. This release adds offerings, remote paywalls, experiments, a customer center and invites, and keeps the rule that matters: the money goes from the store, or your own Stripe or Paddle account, straight to you. Codename One never touches the money, so there's no commission to take.

![The Commerce overview in the console](/blog/commerce-secrets-without-iap-tax/commerce.png)

## One question: does this user have access?

Commerce is built around entitlements. An entitlement is an access right like `pro` or `remove_ads`, and you map store products to it in the console. Your code never asks about products:

```java {class="cn1-no-try"}
CommerceManager cm = CommerceManager.getInstance();
if (cm.isEntitled("pro")) {
    // unlock
} else {
    new Thread(() -> {
        Offering current = cm.getOfferings().getCurrent();   // network call
        if (current != null) {
            CN.callSerially(() -> Paywall.show(current, null));
        }
    }).start();
}
```

A subscription bought on an iPhone unlocks the same account on Android, on the desktop and on the web. Access is decided by one rule, the latest expiry over the active grants measured against the service's clock, so a user who sets the phone's clock back doesn't get a free month.

{{< mermaid >}}
flowchart LR
    I[App Store] --> V[Commerce service<br/>validates receipts]
    G[Google Play] --> V
    A[Amazon Appstore] --> V
    S[Your Stripe or Paddle] --> V
    V --> E[Entitlement: pro]
    E --> P[iPhone]
    E --> D[Android]
    E --> W[Web and desktop]
    I -. money .-> Y[You]
    G -. money .-> Y
    A -. money .-> Y
    S -. money .-> Y
{{< /mermaid >}}

The dotted lines are the point. The service sees receipts. It never sees a card number and nothing flows through us.

## Five stores, one purchase call

- **Apple App Store**, on iOS and on the Mac App Store for native Mac builds.
- **Google Play**, validated through Google's own server API.
- **Amazon Appstore**, with one build hint. The build swaps the Play Billing Library for Amazon's SDK and drops the billing permission Amazon rejects. Your code doesn't change.
- **Stripe and Paddle** for the web and the desktop, where there's no store. A hosted checkout in your own account sells the same package.

`purchase(CommercePackage)` does the right thing on each platform. Where there's a store it sells the store product. Where there isn't, and the package has a web price, it opens the checkout:

```java {class="cn1-no-try"}
new Thread(() -> {
    Offering current = cm.getOfferings().getCurrent();
    if (current != null && current.getAnnual() != null) {
        CN.callSerially(() -> cm.purchase(current.getAnnual()));
    }
}).start();
```

## Paywalls you change without a release

Offerings are configured in the console: which packages you sell, which one is current, and placements for different screens. Targeting rules and experiments decide which offering a given customer gets, and they're evaluated on every `getOfferings` call, so a change takes effect without an app update.

Experiments assign variants by hashing the experiment id with the user id. The same customer sees the same variant on every device and after every restart, and nothing about the assignment is stored. The console reports impressions, purchases and the conversion rate for each variant.

The built-in paywall is themed with UIIDs, like any other Codename One component. For more control there's a paywall document, a JSON description with strings per locale, colors and `{{price}}` style templates, edited in the console with a live preview that uses the same `PaywallView` class as the device.

## A customer center, so users don't email you to cancel

One call shows a form where the customer sees their subscriptions and acts on them:

```java {class="cn1-no-try"}
CustomerCenter.show();
```

You choose which paths it offers and in what order:

| Path | What it does |
| --- | --- |
| `MANAGE` | The store's subscription management, or the billing portal for a web subscription |
| `RESTORE` | Restores the store account's purchases |
| `REFUND` | Asks the store for a refund |
| `CANCEL` | Asks why, shows a retention offer for that reason if you have one, then continues to the store |
| `SUPPORT` | Your support page or email |

The cancel path is where the money is. A customer who's leaving because it's too expensive can be offered a discount right there, and the reason is reported either way, so you learn why people leave instead of guessing.

## Invites, referral rewards and virtual currency

Users can invite friends, and purchases by invited users are tracked back to the person who invited them. A referral can grant an entitlement or currency, and by the time your code sees it the reward is already in effect:

```java {class="cn1-no-try"}
CustomerInfo info = CommerceManager.getInstance().getCustomerInfo();
for (ReferralReward reward : info.getReferralRewards()) {
    String earned = reward.isCurrency()
            ? reward.getAmount() + " " + reward.getCurrencyCode()
            : reward.getEntitlementId();
    Dialog.show("Thank you", "Your invite earned you " + earned, "OK", null);
}
long coins = info.getCurrencyBalance("coins");
```

Subscriber attributes, lifecycle webhooks, a REST API, revenue reports and export and import round out the list. Export matters: if you ever want to leave, your customer data leaves with you.

## When the service is down, users keep access

Commerce is optional and fails soft. If commerce is off in the build, the device is offline or the service can't answer, the app falls back to the stored answer and the store's own receipts, so a paying user keeps what they paid for. Code that talks to the older `Purchase` API directly keeps working unchanged.

For server-authoritative validation you enter your own store credentials in the console: the App Store key, a Google service account, the Amazon shared secret and your Stripe or Paddle keys. They're your accounts. We validate against them, and the payouts never pass through anything we run.

## Learn more

The [Commerce chapter](/developer-guide/#_commerce) covers setup for each store, identity and account transfer, the paywall document format, experiments and the analytics events. If you're paying a percentage of your revenue for this today, run the numbers and tell me what you find.

{{< giscus >}}
