---
title: Braintree (PayPal) cn1lib
slug: braintree-paypal-cn1lib
url: /blog/braintree-paypal-cn1lib/
original_url: https://www.codenameone.com/blog/braintree-paypal-cn1lib.html
aliases:
- /blog/braintree-paypal-cn1lib.html
date: '2017-05-01'
author: Shai Almog
---

![Header Image](/blog/braintree-paypal-cn1lib/new-features-3.jpg)

As part of the bootcamp we wrote a couple of cn1libs and the first one is the [Braintree cn1lib](https://github.com/codenameone/BraintreeCodenameOne) which allows us to do credit card payments within an app. If you aren’t familiar with [Braintree](https://www.braintreepayments.com/) it’s a PayPal company that provides payment integration for mobile devices.

Notice that this differs from [In App Purchase](https://www.codenameone.com/blog/intro-to-in-app-purchase.html) which targets “virtual goods”. This is useful for things like paying for physical goods and services e.g. paying for a taxi.

In order to make a purchase with [this API](https://github.com/codenameone/BraintreeCodenameOne) we can use code such as:
    
    
    Purchase.startOrder(new Purchase.Callback() {
            public String fetchToken() {
               // this method needs to return the token from the Brain tree server API.
               // You need to use this code to connect to your server or return the data
               // from a previous connection that fetched the token
            }
    
            public void onPurchaseSuccess(String nonce) {
                // this is a callback that will be invoked when the purchase succeeds
            }
    
            public void onPurchaseFail(String errorMessage) {
                // this is a callback that will be invoked when the purchase fails
            }
    
            public void onPurchaseCancel() {
                // this is a callback that will be invoked when the purchase is canceled
            }
        });

Notice that we don’t pass pricing or any other information within the code, this is all done in the server code that generates the token for the purchase. This allows our client code to remain “tamper proof”, all credit card collection and charge code is written by Braintree and is thus compliant with all the PCI level security restrictions and we can keep our code simple.

Many basic and subtle hacks can be avoided, e.g. a common hack is to manipulate client side code to change charge pricing but since pricing is determined by our (your) server and communicated directly to the Braintree server this is 100% tamper proof.

This is one of those cn1libs where most of the work is done in the server and so I’m only showing you the tip of the iceberg and you would need to followup with the [Braintree docs](https://developers.braintreepayments.com/start/overview) to understand how this is bound to your server then implement your server side logic.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Julien Sosin** — May 2, 2017 at 8:58 pm ([permalink](https://www.codenameone.com/blog/braintree-paypal-cn1lib.html#comment-23316))

> Julien Sosin says:
>
> Looks good !
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fbraintree-paypal-cn1lib.html)


### **salah Alhaddabi** — May 3, 2017 at 1:39 pm ([permalink](https://www.codenameone.com/blog/braintree-paypal-cn1lib.html#comment-23498))

> salah Alhaddabi says:
>
> Extremely Excellent Work Shai. You guys always keep CN1 ahead of the game!!!
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fbraintree-paypal-cn1lib.html)


### **Edwin Quai Hoi** — May 4, 2017 at 6:53 am ([permalink](https://www.codenameone.com/blog/braintree-paypal-cn1lib.html#comment-21432))

> Edwin Quai Hoi says:
>
> hi is there a list of existing cn1libs anywhere
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fbraintree-paypal-cn1lib.html)


### **Shai Almog** — May 5, 2017 at 4:44 am ([permalink](https://www.codenameone.com/blog/braintree-paypal-cn1lib.html#comment-23306))

> Shai Almog says:
>
> Sure. Right click project Codename One -> Codename One Settings -> Extensions.
>
> Also mirrored to [https://www.codenameone.com…](<https://www.codenameone.com/cn1libs.html>)
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fbraintree-paypal-cn1lib.html)


### **salah Alhaddabi** — June 15, 2017 at 6:54 am ([permalink](https://www.codenameone.com/blog/braintree-paypal-cn1lib.html#comment-23395))

> salah Alhaddabi says:
>
> Dear Shai,
>
> I have quoted the followings from PayPal Mobile SDK developer site:
>
> “In countries where Braintree Direct is not available, or to access other features of the PayPal REST API from a mobile app, the native libraries of the PayPal Mobile SDKs enable you to build fast, responsive apps”
>
> it also states that “You can use PayPal’s SDKs in any country where PayPal is accepted”.
>
> So the PayPal Mobile SDKs are accepted in more countries including my country “Oman”.
>
> is the CN1 library built using PayPal Mobile SDKs ??
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fbraintree-paypal-cn1lib.html)


### **Shai Almog** — June 16, 2017 at 6:59 am ([permalink](https://www.codenameone.com/blog/braintree-paypal-cn1lib.html#comment-24206))

> Shai Almog says:
>
> Hi,  
> it will fallback to paypal. The location restriction is for the payment receiver.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fbraintree-paypal-cn1lib.html)

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
