---
title: "Build a Commerce Platform in Java with Bazaar"
description: "Bazaar is a commerce platform: a storefront and checkout, a back office with orders, fulfilment, inventory across locations, a sales pipeline, analytics and an AI store assistant, and a register that keeps selling with no network. Shopper, merchant, register and admin apps, a server-rendered web store and a Java backend. Free MIT-0 source."
url: "/bazaar/"
summary: "A complete full-stack commerce platform you can brand and extend."
images: ["/developer-guide/img/bazaar-shop-home-phone.png"]
ShowToc: true
---

Bazaar is a complete, full-stack commerce platform in Java. Shoppers browse independent stores, fill one cart and check out on a phone or in a browser. The store's staff run the business from a back office on a tablet, a desktop or the web: orders and fulfilment, products and inventory across locations, customers and discounts, a sales pipeline of every open cart and order, analytics and an assistant that answers from the store's own numbers. At the counter, a cashier sells on a register that keeps working when the network does not. An administrator runs the marketplace the stores live on. Use it to build your own store, marketplace or point of sale with Codename One.

{{< guide-block >}}
<video controls playsinline preload="metadata" poster="/bazaar/bazaar-merchant-desktop-poster.jpg" style="width:100%;max-width:960px;height:auto;display:block;margin:0 auto" aria-label="The back office as a desktop app: the dashboard, the sales pipeline with a deal dragged on, analytics, the store assistant, the product editor, inventory, orders, fulfilment and the inbox">
<source src="/bazaar/bazaar-merchant-desktop.mp4" type="video/mp4">
<a href="/bazaar/bazaar-merchant-desktop.mp4">Download the video</a>.
</video>

<video controls playsinline preload="metadata" poster="/bazaar/bazaar-pos-desktop-poster.jpg" style="width:100%;max-width:960px;height:auto;display:block;margin:1.5rem auto 0" aria-label="A sale on the register as a desktop app: the cashier's PIN, opening a shift with the float, ringing up three products, charging cash and the receipt">
<source src="/bazaar/bazaar-pos-desktop.mp4" type="video/mp4">
<a href="/bazaar/bazaar-pos-desktop.mp4">Download the video</a>.
</video>

<video controls playsinline preload="metadata" poster="/bazaar/bazaar-admin-desktop-poster.jpg" style="width:100%;max-width:960px;height:auto;display:block;margin:1.5rem auto 0" aria-label="The platform console as a desktop app: the marketplace overview, the stores, the users and the payments">
<source src="/bazaar/bazaar-admin-desktop.mp4" type="video/mp4">
<a href="/bazaar/bazaar-admin-desktop.mp4">Download the video</a>.
</video>

<video controls playsinline preload="metadata" poster="/bazaar/bazaar-shopper-phone-poster.jpg" style="width:100%;max-width:400px;height:auto;display:block;margin:1.5rem auto 0" aria-label="A shopper on a phone: browsing as a guest, search, a product, the cart, signing in, checkout with shipping and payment, and the placed order">
<source src="/bazaar/bazaar-shopper-phone.mp4" type="video/mp4">
<a href="/bazaar/bazaar-shopper-phone.mp4">Download the video</a>.
</video>

<video controls playsinline preload="metadata" poster="/bazaar/bazaar-web-storefront-poster.jpg" style="width:100%;max-width:960px;height:auto;display:block;margin:1.5rem auto 0" aria-label="The web storefront: the marketplace, a store, search, a product page, the cart and a guest checkout to the placed order">
<source src="/bazaar/bazaar-web-storefront.mp4" type="video/mp4">
<a href="/bazaar/bazaar-web-storefront.mp4">Download the video</a>.
</video>

<video controls playsinline preload="metadata" poster="/bazaar/bazaar-web-account-poster.jpg" style="width:100%;max-width:960px;height:auto;display:block;margin:1.5rem auto 0" aria-label="A customer account on the web: a signed-in checkout, the account overview, orders, an order with its timeline, loyalty, subscriptions and security">
<source src="/bazaar/bazaar-web-account.mp4" type="video/mp4">
<a href="/bazaar/bazaar-web-account.mp4">Download the video</a>.
</video>

{{< /guide-block >}}

**[Generate your project](/initializr/)** · **[Read the user's guide](/developer-guide/bazaar-users-guide/)** · **[Browse the source](https://github.com/codenameone/CodenameOne/tree/master/scripts/bazaar)**

In the Initializr, choose the **Bazaar** commerce template, set your application name and package, and generate the project. The application source is licensed under [MIT-0](https://github.com/codenameone/CodenameOne/blob/master/scripts/bazaar/LICENSE): you can modify it, use it commercially and keep your changes private without royalties.

## What comes with the application?

| Part | Included workflows |
| --- | --- |
| Shopper | Browse as a guest with no sign-in wall, search every store, product galleries with variants and reviews, one cart across stores, discount codes and gift cards, a checkout with live shipping rates and saved cards, order tracking, a wishlist, saved addresses, followed stores, loyalty and returns |
| Merchant | A dashboard, products with variants, media, translations and SEO, inventory across locations with transfers and purchase orders, orders with bulk actions, a fulfilment board from packing to delivery, customers and segments, discounts, gift cards, campaigns and journeys, shipping and taxes, automations and webhooks |
| Sales and insight | A sales pipeline of every live cart and open order with purchase intent, analytics against the previous period, reports, a forecast, and a store assistant that answers from the store's data |
| Register | A cashier's PIN, shifts with a counted float, a product grid and scanner, discounts, parked carts, layaways, split tenders and tips, receipts printed, emailed or texted, returns, and sales that sync once the network is back |
| Web | A server-rendered storefront search engines can read, a guest and signed-in checkout, a customer account, in light and dark, and the whole app, back office included, served at `/app/` as its JavaScript build |
| Administrator | Store approval and suspension, accounts, and the platform's payments and totals |
| Java backend | Accounts and roles, stores, catalog and an inventory ledger, carts and checkout, payments authorized at checkout and captured at shipping, markets and tax, a simulated carrier, webhooks, push notifications and the live channel |

The account's roles decide which screens a person sees. The same client project targets iOS, Android, desktop and the web, and the server runs on SQLite, PostgreSQL, MySQL and MariaDB.

![Bazaar shop on a phone](/developer-guide/img/bazaar-shop-home-phone.png)
![Bazaar checkout on a phone](/developer-guide/img/bazaar-shop-checkout-shipping-phone.png)
![Bazaar merchant dashboard on a tablet](/developer-guide/img/bazaar-merchant-dashboard-tablet.png)
![Bazaar sales pipeline on a tablet](/developer-guide/img/bazaar-merchant-pipeline-tablet.png)
![Bazaar register taking a payment](/developer-guide/img/bazaar-pos-tender-tablet.png)
![Bazaar web storefront](/developer-guide/img/bazaar-web-store.png)

## From first run to customization

1. **[Run the project](https://github.com/codenameone/CodenameOne/tree/master/scripts/bazaar).** The development profile needs nothing installed: an in-memory database, two demo stores with a month of past sales, demo accounts for every role, simulated cards and a simulated carrier.
2. **Try every role.** Shop as `shopper@bazaar.example`, run Northwind Outfitters as `owner@bazaar.example`, sell at the register as `cashier@bazaar.example` with the PIN `1234`, and run the platform as `admin@bazaar.example`. The password is `bazaar-demo`.
3. **Customize and deploy.** Change the theme, the catalog and the storefront templates, connect a payment processor and a carrier, and point the server at your own database.

## What does a live business still need?

Bazaar supplies the connected application and its source. Taking real orders also requires your own hosting, a payment processor account, a carrier account for labels and rates, push credentials, a key for the AI model if the assistant should use one, and the legal pages and tax settings of the places you sell.

## Other full-stack samples

[Savor](/savor/) is food delivery, [Wayline](/wayline/) ride hailing, [Fieldline](/fieldline/) field service, [Slate](/slate/) appointment booking, [Pulse](/pulse/) a fitness studio and [Bastion](/bastion/) a security-first bank, all on the same architecture.
