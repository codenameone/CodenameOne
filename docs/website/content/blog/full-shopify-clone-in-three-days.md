---
title: "We Built a Full Shopify Clone in Three Days"
slug: full-shopify-clone-in-three-days
url: /blog/full-shopify-clone-in-three-days/
date: '2026-11-27'
author: Shai Almog
description: "Two weeks ago I said Shopify was wrong to answer React Native with two native codebases. So we built Bazaar: a shopper app, a merchant back office, a point of sale, a marketplace admin, a server-rendered web store, a blog, an app platform with checkout functions, and a Java backend on four databases. It took three days, 1,877 files and 781 tests, and it's free MIT-0 source."
feed_html: "<img src=\"https://www.codenameone.com/blog/full-shopify-clone-in-three-days.jpg\" alt=\"A full Shopify clone in three days\" /> Shopper app, back office, point of sale, web store, extension platform and backend. Three days, one Java codebase, free source. Plus Word and PowerPoint files, Photoshop and Figma, new charts and components, banking grade security and Oracle support."
series: ["release-2026-11-27"]
---

![A full Shopify clone in three days](/blog/full-shopify-clone-in-three-days.jpg)

Two weeks ago I wrote that [Shopify was wrong to dump React Native for two native codebases](/blog/shopify-was-wrong-and-right-to-dump-react-native/). Plenty of people told me it's easy to criticize a company that ships. Fair. So I put my money where my mouth is.

We built Shopify. Not the mobile app. All of it.

A shopper app. A merchant back office on phone, tablet, desktop, and web. A point of sale that keeps selling when the network dies. A marketplace admin. A server-rendered web store that search engines index. A blog. An app platform with OAuth installs, webhooks, UI extensions and checkout functions. Inventory across locations, invoicing, B2B price lists, subscriptions, loyalty, marketing journeys and an AI assistant. One Java codebase and one Java backend, running on SQLite, PostgreSQL, MySQL, or MariaDB.

It took three days of work. The first commit landed on Friday, November 20, every phase of the plan passed its tests on SQLite, PostgreSQL and MySQL the next day, and the rest went into matching Shopify's feature list row by row. It's called [Bazaar](/bazaar/), the source is free under MIT-0, and it went in as [PR #6329](https://github.com/codenameone/CodenameOne/pull/6329) while we shipped everything else in this post.

## TL;DR

- **[Bazaar covers more than a store needs.](#what-three-days-bought)** Our comparison table lists 267 Shopify features. Bazaar fully supports about 250 of them, including 28 that Shopify leaves to its app store.
- **[It has things Shopify doesn't.](#things-shopify-doesnt-have)** A sales pipeline board where dragging a card does the real work, and a purchase intent score on every cart.
- **[The web store is HTML, on purpose.](#the-web-store-is-html-on-purpose)** Server-rendered pages, a sprinkle of htmx, no framework. HTML is the best tool for a web page.
- **[1,877 files is small.](#1877-files-is-small)** For a mobile app, back office, register, website, extension platform, docs, tests and demo data, it's tiny.
- **[Shopify went backwards.](#agents-should-raise-the-abstraction-not-lower-it)** Every decade of computing raised the abstraction. Agents are a reason to climb higher, not to climb down.
- **[What your customers want has nothing to do with your toolchain.](#native-isnt-what-your-customers-want)** Stability, fast fixes, speed, reach and a great look. All of them come easier from a high level platform.

## Coming This Week

- **Saturday, November 28:** {{< post-link path="/blog/word-documents-and-presentations-in-your-app" text="Open, Edit, Spell Check and Present Word and PowerPoint Files Inside Your App" >}}. DOCX, ODT, RTF, PPTX, ODP, PPT and Keynote.
- **Sunday, November 29:** {{< post-link path="/blog/photoshop-sketch-figma-and-rive-in-your-app" text="Open Photoshop, Sketch and Figma Files in Your App, and Play Rive Animations" >}}. Plus live SVG and a drawing editor.
- **Monday, November 30:** {{< post-link path="/blog/sixteen-components-and-charts-you-can-touch" text="Sixteen New Components, and Charts You Can Touch" >}}. Carousels, coach marks, chips, color pickers and 19 chart types that animate and zoom.
- **Tuesday, December 1:** {{< post-link path="/blog/stop-debuggers-hooks-and-keyloggers" text="Stop Debuggers, Hooking Tools and Keyloggers Before They Reach Your Users' Money" >}}. The defenses a bank app needs.
- **Wednesday, December 2:** {{< post-link path="/blog/oracle-sql-server-and-your-own-map-server" text="Oracle, SQL Server and Your Own Map Server, From One Java Backend" >}}. Plus `io_uring` under virtual threads.
- **Thursday, December 3:** {{< post-link path="/blog/agents-that-freeze-time-and-on-device-ai" text="Let Your Agent Freeze Time in the Simulator, and Run the Phone's Own AI Model" >}}. Gemini Nano and Apple Foundation Models from Java.

{{< guide-block >}}
<div class="release-screenshots" style="display:grid;grid-template-columns:repeat(auto-fit,minmax(min(100%,180px),1fr));gap:16px;align-items:start;margin:24px 0">
<figure style="margin:0"><a href="/blog/word-documents-and-presentations-in-your-app/"><img src="/developer-guide/img/generated/slides-presenter.png" alt="Presenter mode with the next slide, notes and a clock" loading="lazy" style="width:100%;height:auto" /></a><figcaption>Presenter mode.</figcaption></figure>
<figure style="margin:0"><a href="/blog/photoshop-sketch-figma-and-rive-in-your-app/"><img src="/developer-guide/img/generated/art-editor.png" alt="The drawing editor with layers and an inspector" loading="lazy" style="width:100%;height:auto" /></a><figcaption>Layers and effects.</figcaption></figure>
<figure style="margin:0"><a href="/blog/sixteen-components-and-charts-you-can-touch/"><img src="/developer-guide/img/generated/chart-sankey.png" alt="A Sankey chart" loading="lazy" style="width:100%;height:auto" /></a><figcaption>Sankey and 18 more.</figcaption></figure>
<figure style="margin:0"><a href="/blog/sixteen-components-and-charts-you-can-touch/"><img src="/developer-guide/img/generated/components-coach-marks.png" alt="A coach mark tour highlighting a button" loading="lazy" style="width:100%;height:auto" /></a><figcaption>Coach marks.</figcaption></figure>
</div>
{{< /guide-block >}}

## What three days bought

Bazaar is four apps and a website over one backend. Shoppers browse many independent stores and check out on a phone or in a browser. Staff run the business from a back office. A cashier sells at a register. An administrator runs the marketplace.

{{< mermaid >}}
flowchart LR
    subgraph One Java codebase
        Shop[Shopper app<br/>iOS, Android]
        Office[Back office<br/>tablet, desktop, web]
        POS[Register<br/>phone, tablet, desktop]
        Admin[Marketplace admin]
    end
    Web[Web store<br/>server-rendered HTML + htmx]
    Shop --> API
    Office --> API
    POS -- offline queue --> API
    Admin --> API
    Web --> API
    API[Java backend<br/>REST, GraphQL, WebSocket]
    API --> DB[(SQLite, PostgreSQL,<br/>MySQL or MariaDB)]
    API -- signed webhooks --> Apps[Third-party apps<br/>any language]
    Apps -- OAuth tokens --> API
{{< /mermaid >}}

We keep a [feature comparison](https://www.codenameone.com/developer-guide/bazaar-feature-comparison/) in the developer guide that walks Shopify, WooCommerce, BigCommerce and Square row by row. Here is the short version.

| Area | What's in Bazaar |
| --- | --- |
| Catalog | Variants, video and 3D models, bundles, digital downloads, combined listings, metafields and metaobjects, Google's product taxonomy, unit pricing, translations |
| Inventory | Multiple locations, transfers, purchase orders, suppliers, stocktakes, committed and incoming stock, serial numbers and lots with expiry, barcode label sheets |
| Checkout | One-page checkout, Apple Pay and Google Pay, Klarna and Afterpay, iDEAL and SEPA, saved cards, gift cards, store credit, tipping, post-purchase upsell, fraud rules |
| Orders | Edits after placement, draft orders with pay links, authorize then capture on ship, PDF invoices, bulk actions, returns and exchanges with return labels |
| Shipping | Zones, carrier-calculated rates, label buying, local delivery and pickup, routing rules across locations, CN 22 and CN 23 customs forms |
| Point of sale | PIN sign-in, shifts and Z reports, split tender, card readers, ESC/POS printers and cash drawers, layaways, parked carts, offline mode with sync |
| Marketing | Discounts of every shape, flash sales with countdowns, email and SMS campaigns, journeys drawn as flowcharts, points, VIP tiers, referrals, paid clubs |
| B2B and markets | Company accounts, price lists, net terms, quotes, multi-currency, EU VAT with VIES checks, OSS reports, duties estimates |
| Web | Themes, a blog with RSS and Atom, menus, redirects, JSON-LD, sitemaps, predictive search with synonyms, custom domains, a cookie banner |
| Extensibility | OAuth apps, scoped API keys, REST and GraphQL, webhooks with retries, UI extensions, checkout functions, an app directory with billing |
| Operations | Analytics with a report builder and cohorts, live chat and calls, TOTP, OpenID Connect sign-in, audit log, multi-store organizations |

The demo seeds three stores with 90 days of history, and a demo pipeline keeps moving orders through to delivery so every screen has something happening on it. Here's the shopper app:

{{< guide-block >}}
<video controls playsinline preload="metadata" poster="/bazaar/bazaar-shopper-phone-poster.jpg" style="width:100%;max-width:360px;height:auto;display:block;margin:16px auto" aria-label="Shopping in the Bazaar app on a phone"><source src="/bazaar/bazaar-shopper-phone.mp4" type="video/mp4"><a href="/bazaar/bazaar-shopper-phone.mp4">Download the shopper video</a>.</video>
{{< /guide-block >}}

And the back office the merchant runs it from:

{{< guide-block >}}
<video controls playsinline preload="metadata" poster="/bazaar/bazaar-merchant-desktop-poster.jpg" style="width:100%;height:auto;display:block;margin:16px auto" aria-label="The Bazaar back office on the desktop"><source src="/bazaar/bazaar-merchant-desktop.mp4" type="video/mp4"><a href="/bazaar/bazaar-merchant-desktop.mp4">Download the back office video</a>.</video>
{{< /guide-block >}}

### The paid add-ons are built in

This is the table I'd show a merchant. Each row is a feature Shopify leaves to its app store, which usually means another monthly subscription and another vendor with access to your customers. In Bazaar they ship in the box:

| Usually a paid Shopify app | In Bazaar |
| --- | --- |
| Product reviews and review requests after delivery | Built in |
| Wishlists and back-in-stock alerts | Built in |
| Bundles and digital downloads | Built in |
| Subscriptions, pre-orders, try before you buy | Built in |
| Points, VIP tiers, referrals, paid memberships | Built in |
| Tiered and volume discounts, flash sales | Built in |
| Browse abandonment and push to shoppers | Built in |
| Serial and lot tracking, barcode label printing | Built in |
| Staff commissions at the register | Built in |
| B2B quotes | Built in |
| Amazon, eBay and Walmart marketplace sync | Built in, behind a connector interface |
| A Kanban fulfilment board with bins and pick lists | Built in |

### Things Shopify doesn't have

The **sales pipeline** puts every open cart next to every open order on one board: browsing, at checkout, reminded, paid, fulfilling, shipped, delivered, return requested. Dragging a card does the real thing. Drag a cart to "reminded" and the recovery mail goes out. Drag an order to "shipped" and it ships. The server refuses any move that doesn't make sense. Shopify keeps abandoned checkouts and orders on separate pages.

Every cart on that board carries a **purchase intent score**, built from product views, return visits, time on product pages, cart value and past orders, with the reasons written in words on the card. A merchant knows which cart to chase before lunch.

![The Bazaar sales pipeline on the desktop](/developer-guide/img/bazaar-merchant-pipeline-desktop.png)

The **assistant** answers from the store's own numbers, through a fixed set of queries the model calls as tools. The owner picks the provider (OpenAI, Anthropic, Gemini or any compatible server) and the key never leaves the server. With no provider at all, the same screens answer by rules.

And the **whole thing is yours**. Self-hosted, with the source, under a license that lets you copy it, change it and sell it.

### A register that doesn't care about Wi-Fi

Retail floors have dead spots, and payment terminals go offline at the worst possible time. Bazaar's register keeps every sale on the device and uploads when it reconnects. The trick that makes it safe is boring and correct: each sale carries an id the register made, and the server stores it as unique.

```java {class="cn1-no-try"}
final String clientId = clientId(storeId, sale.clientId().trim());
StoreOrder again = existing(clientId);
if (again != null) {
    // The answer was lost and the register retried: record it once.
    return duplicate(sale, again);
}
```

Send a batch twice and every sale lands once, and stock moves once. A sale is never refused for lack of stock, because the goods already left with the customer. It's marked oversold and audited instead.

{{< guide-block >}}
<video controls playsinline preload="metadata" poster="/bazaar/bazaar-pos-desktop-poster.jpg" style="width:100%;height:auto;display:block;margin:16px auto" aria-label="Selling at the Bazaar register"><source src="/bazaar/bazaar-pos-desktop.mp4" type="video/mp4"><a href="/bazaar/bazaar-pos-desktop.mp4">Download the register video</a>.</video>
{{< /guide-block >}}

### Apps that can't take the store down

The backend compiles ahead of time to a native server with no class loader, so a plugin can't be loaded into it. That's a feature. A plugin that shares your memory, threads and database connections is one slow plugin away from slowing every store on the platform.

So a Bazaar app is a separate web service, in any language. It installs with OAuth and PKCE, reads and writes through scoped tokens, and hears about events through signed webhooks. Its UI extensions are JSON trees over a fixed set of blocks that the merchant app draws with its own components, so there is no script in a block. Checkout logic is a tiny language that Bazaar compiles, verifies and runs with a fixed instruction budget. Here's a pair deal and a bulk discount:

```text
for line in lines {
  if line.quantity >= 2 { emit line_discount(line.index, line.total / 4, "Pair deal"); }
}
if cart.quantity >= 3 { emit discount(100, "Three and up"); }
```

Every loop walks a list, so every loop ends. Overflow stops the function. An app that doesn't answer in time is left out, not waited for. The [Extending Bazaar](/developer-guide/#extending-bazaar) chapter walks through a complete sample app, Restock Alerts, from install to its checkout field.

## The web store is HTML, on purpose

We could have shipped the storefront as the same Java app compiled to JavaScript. We do that for the back office in the browser, where it's the right call: it's an app, staff use it all day, and it's the same code as the tablet.

A storefront is a different animal. It's a web page. It has to paint before a shopper loses interest, work without JavaScript, open from a link in an email, and rank on Google. For that, HTML is the best tool ever made. Better than React, better than any framework that ships a runtime to render the text that was already in the response.

So product, collection, cart, checkout and account pages are server-rendered templates with titles, canonical links, JSON-LD and a sitemap. Small interactions use [htmx](https://htmx.org/), and every one of them falls back to a plain form post. Changing the postal code at checkout refreshes the delivery rates and nothing else:

```html
<input id="postal" name="postal" autocomplete="postal-code"
       hx-post="/checkout" hx-trigger="change"
       hx-target="#summary" hx-select="#summary" hx-swap="outerHTML">
```

Each page sends a Content Security Policy built from script hashes and a per-request nonce. Third-party widgets run in sandboxed frames, and scripts in an optional category load only after the visitor accepts that category in the cookie banner.

{{< guide-block >}}
<div class="release-screenshots" style="display:grid;grid-template-columns:repeat(auto-fit,minmax(min(100%,240px),1fr));gap:16px;align-items:start;margin:24px 0">
<figure style="margin:0"><img src="/developer-guide/img/bazaar-web-store.png" alt="A Bazaar store's home page in the browser" loading="lazy" style="width:100%;height:auto" /><figcaption>A store's home page.</figcaption></figure>
<figure style="margin:0"><img src="/developer-guide/img/bazaar-web-product.png" alt="A product page with variants and reviews" loading="lazy" style="width:100%;height:auto" /><figcaption>Product pages.</figcaption></figure>
<figure style="margin:0"><img src="/developer-guide/img/bazaar-web-checkout.png" alt="The one-page web checkout" loading="lazy" style="width:100%;height:auto" /><figcaption>One-page checkout.</figcaption></figure>
</div>
{{< /guide-block >}}

{{< guide-block >}}
<video controls playsinline preload="metadata" poster="/bazaar/bazaar-web-storefront-poster.jpg" style="width:100%;height:auto;display:block;margin:16px auto" aria-label="Browsing a Bazaar store in the browser"><source src="/bazaar/bazaar-web-storefront.mp4" type="video/mp4"><a href="/bazaar/bazaar-web-storefront.mp4">Download the web store video</a>.</video>
{{< /guide-block >}}

Picking the right tool per surface is the whole point. Java on every device, HTML for web pages, and one backend enforcing every rule once, so the web checkout and the app checkout can't disagree about a discount.

## 1,877 files is small

That sounds like a lot until you look at what's in it.

| Part | Files | Lines |
| --- | ---: | ---: |
| Backend, 60 packages from catalog to taxes | 692 | 124,659 |
| Shared request and response types | 431 | 19,622 |
| The app: shopper, back office, register, admin | 231 | 50,567 |
| Tests (781 of them) | 151 | 34,827 |
| SQL migrations for three database dialects | 104 | |
| Web templates, styles and scripts | 40 | |
| Demo media, screenshots and walkthrough videos | 139 | |
| Guides for users, implementors and app developers | 4 | 3,061 |

The 431 shared files average 45 lines each: they are the request and response records the app and the server both compile. The client is 50,000 lines for four apps on six platforms. A native team would write each of those screens twice before it reached a desktop, and a third time for the web.

I don't know how big Shopify's code is, but I know they have a large mobile team. I'd bet their code runs to millions of lines. To be fair, it covers more than Bazaar does, but I doubt that closes an order of magnitude.

Yes, I could have pointed an agent at any stack and eventually gotten something that demos well. Not in three days, though: on many stacks the compile and test cycle alone eats that. Agents generate code in every language. The question is what happens on day four. Who tests it on every device? Who keeps the iOS and Android copies of the register in sync when a tax rule changes? Who reviews 260,000 lines spread over three languages and two UI toolkits?

Here, the answer is the framework. Bazaar's tests run on the JVM, its screens are captured headless in a container, and its backend tests run against SQLite, PostgreSQL and MySQL in CI. It barely moved our token or CI budget. And because the client and the server share one language and one set of request types, a change to a tax rule is one change, reviewed once, tested once.

## Agents should raise the abstraction, not lower it

We don't want to compete with Shopify. I hope they take this code and build their next platform on it. That's what MIT-0 is for.

I'm picking on Shopify because they're telling everyone else to make the same move. Their argument is that agents make the second copy of every feature cheap, so why share code at all. That's the lazy answer. It's doing what they always did, with a lower level of abstraction, and it gives their users nothing they can measure.

Every decade of computing raised the abstraction. Machine code to assembly. Assembly to C. C to managed languages. Hand-rolled UIs to toolkits, toolkits to frameworks. Each step let the same team build something bigger. If agents make writing code cheap enough to justify climbing down a level, why stop at Swift and Kotlin? Have the agent write machine code. It doesn't get bored.

Nobody does that, because the cost of software was never typing it. It's testing it, understanding it, and changing it without breaking it. Lower abstractions make all three harder, and agents make them harder still, because they write faster than anyone can review.

Developers should expand to fill what their tools make possible. If you won't, your competitor will. Agents feel like an unlimited resource, but they aren't, and a lot of that resource is going into rebuilding solved problems one platform at a time. Sometimes that's right: plenty of web apps should never have been web apps. More often it's a regression wearing a new jacket.

Put agents on a high level platform and the result is jet fuel. You get three days to a commerce platform, instead of three days to a second copy of the cart screen.

## Native isn't what your customers want

Ten years ago an early Codename One customer argued with me about this. "If I had unlimited resources," he said, "I'd pick native, because that's the best I can offer my customers. Right?"

Wrong, for two reasons. No resource is unlimited, and agents didn't change that. But more important, what's best for his customers had nothing to do with which toolchain he used. Here's what customers want, and why every one of them is easier from a high level:

- **A stable app.** One codebase has one set of bugs. Two codebases have two sets that drift apart, plus the bugs in the space between them.
- **Fast fixes.** A fix written once ships everywhere the same day.
- **An app the next OS update won't break.** Codename One's UI is compiled into your app, and platform churn is our job. When Apple made UIScene mandatory and broke apps that hooked the app delegate, [our users didn't notice](/blog/uiscene-on-by-default-ios-performance/).
- **Speed.** Native code isn't faster by default. Performance comes from caching, batching and doing less work, and those are much easier to get right once, in one place. Our Base64 encoder is [nearly three times faster than Apple's](/blog/ios-density-scroll-and-accessibility/).
- **Features.** A Gantt chart, a spreadsheet or a sales pipeline written once is a feature on every platform. Written natively, it's a project per platform.
- **Everywhere.** On my phone, my desktop, my watch and my friend's GrapheneOS phone. One codebase reaches them all.
- **A great look.** People got it into their heads that native apps look better. That was true of older frameworks, and of older or lazily built Codename One apps. Look and feel isn't a function of the toolchain. It's a function of the developer.

No, Bazaar isn't ready to replace your production store tomorrow morning. But the ground it covers in three days is staggering, it stands up to platforms with thousands of engineers, and parts of it are ahead of them. That's what a modern framework does with an agent behind it.

## More This Week

### Word documents and presentations, in your app

Last week we added [spreadsheets with live formulas](/blog/a-million-rows-and-live-excel-formulas/). This week the rest of the office arrived. `com.codename1.wordprocessing` reads and writes DOCX, ODT, RTF, HTML and Markdown, reads old Word files and exports PDF. `DocumentView` shows pages and edits them with undo, and `SpellChecker` and `GrammarChecker` proof them as you type.

```java {class="cn1-no-try"}
Document d = Document.read(in, Document.Format.DOCX);
d.write(out, Document.Format.PDF);
```

`com.codename1.presentation` opens PPTX, ODP, PPT and Keynote files, edits them in `SlideView`, and plays them in `SlideShow` with builds, transitions and a presenter mode. Why put presentations in a mobile framework? Convergence. Apps can't be islands that do one small thing anymore. The field service app needs the inspection report. The sales app needs the deck. Saturday's {{< post-link path="/blog/word-documents-and-presentations-in-your-app" text="article" >}} has the details.

{{< guide-block >}}
<div class="release-screenshots" style="display:grid;grid-template-columns:repeat(auto-fit,minmax(min(100%,240px),1fr));gap:16px;align-items:start;margin:24px 0">
<figure style="margin:0"><img src="/blog/document-review.png" alt="A document with spelling and grammar marks, tracked changes, a highlight and a table" loading="lazy" style="width:100%;height:auto" /><figcaption>Proofing and tracked changes.</figcaption></figure>
<figure style="margin:0"><img src="/blog/document-pages.png" alt="A page with a table of contents, a floating chart, a hanging indent and balanced columns" loading="lazy" style="width:100%;height:auto" /><figcaption>Floats, columns and a table of contents.</figcaption></figure>
</div>
{{< /guide-block >}}

### Photoshop, Sketch, Figma and live SVG

`com.codename1.art` opens PSD and PSB with layers, masks and layer styles, Sketch files, Figma files through the REST API, and SVG at runtime with filters, masks, and CSS. They all become one document tree that renders the same pixels on every platform, and `ArtEditor` is a drawing editor you can drop into your app. Rive files now play as ordinary images, translated to Java at build time.

```java {class="cn1-no-try"}
ArtDocument doc = ArtFormats.read(in);        // SVG, PSD, Sketch, Figma or .cn1gfx
Image image = ArtRenderer.toImage(doc, 2.0);  // two pixels per document unit
```

![The drawing editor with toolbar, layers and inspector](/developer-guide/img/generated/art-editor.png)

Sunday's {{< post-link path="/blog/photoshop-sketch-figma-and-rive-in-your-app" text="article" >}} shows both.

### Sixteen components and a new chart package

Skeleton loaders, carousels with page dots, coach marks, empty states, range sliders, chips, a color picker, circular progress, audio waveforms, expandable text, activity timelines, an emoji picker, a mention field and a masonry layout are all in core now. The new `com.codename1.ui.chart` package draws 19 series types, from bars to Sankey, treemaps and candlesticks, with animation, pan, zoom, tooltips and screen reader support. You can see it in every Bazaar dashboard. Monday's {{< post-link path="/blog/sixteen-components-and-charts-you-can-touch" text="article" >}} has the gallery.

![The new chart package showing revenue against a target](/developer-guide/img/generated/chart-first.png)

### Defenses for banking apps

Banking and payments apps live under a threat model most apps never see. `android.debugGuard=true` now refuses to run with a debugger attached, and can also refuse to run with Developer Options or USB debugging on. Root detection catches Magisk, KernelSU and APatch, and hook detection catches Frida and the Xposed family. For the keyboard, `TextArea.SENSITIVE` keeps card and account numbers out of learned words, `isThirdPartyKeyboardActive` tells you when an unknown keyboard is reading every key, and `SecureKeyboard` draws your own, scrambled if you like, on every platform including iOS.

```java {class="cn1-no-try"}
TextField pin = new TextField("", "PIN", 6, TextArea.NUMERIC | TextArea.PASSWORD);
if (DeviceIntegrity.isThirdPartyKeyboardActive(DeviceIntegrity.wellKnownKeyboards())) {
    SecureKeyboard.install(pin).setScrambled(true);
}
```

![The secure keyboard with scrambled keys](/developer-guide/img/generated/secure-keyboard-scrambled.png)

Tuesday's {{< post-link path="/blog/stop-debuggers-hooks-and-keyloggers" text="security article" >}} covers every signal.

### Oracle, SQL Server, maps and `io_uring`

The backend now speaks to SQL Server 2017 and newer, Azure SQL, and Oracle 19c and newer, next to PostgreSQL, MySQL, MariaDB, and SQLite. It speaks each wire protocol directly, with no driver. And the same `Database` API now reaches all of them from the client too, so a desktop database admin tool or a reporting app is plain Codename One code:

```java {class="cn1-no-try"}
Database db = new OracleDatabase("db.example.com", 1521, "ORCLPDB1", "app", password);
Cursor c = db.executeQuery("SELECT id, name FROM item WHERE price < ?", 10.0);
```

The backend can also serve your own OpenStreetMap tiles, routes and geocoding, OSRM and Nominatim compatible, so your maps stop depending on someone else's rate limit. Under the virtual threads, blocking file and SQLite I/O moves off the host threads, through `io_uring` on Linux, so one slow query no longer stalls unrelated requests. Wednesday's {{< post-link path="/blog/oracle-sql-server-and-your-own-map-server" text="backend article" >}} has the rest.

### Agents that freeze time, and the phone's own model

Animations are the hardest thing for an agent to check, because the screenshot it takes is always mid-flight. Now it can stop the clock. `ui_clock_freeze` holds every animation, `ui_clock_advance` steps it frame by frame, and `ui_clock_settle` runs it to the end, so the agent inspects exactly the frame it means to. `mvn cn1:verify` checks your first form in light, dark, landscape and the largest text size for unlabeled controls, truncated text and poor contrast, and fails the build on an error.

`LlmClient.system()` reaches the language model the operating system already ships: Apple Foundation Models on iOS and macOS, and Gemini Nano on Android devices with AICore. No download, no API key, and the prompt never leaves the device. Thursday's {{< post-link path="/blog/agents-that-freeze-time-and-on-device-ai" text="article" >}} shows both.

### A currency API that should have been there years ago

`java.util.Currency` now exists on every platform, with the same ISO 4217 data everywhere: codes, numeric codes, fraction digits and the currency of every country. `L10NManager.formatCurrency` formats in the user's locale with the currency's own digits, two for euros, none for yen, three for Bahraini dinars.

```java {class="cn1-no-try"}
Currency yen = Currency.getInstance("JPY");
int digits = yen.getDefaultFractionDigits();              // 0: no sen in prices
Currency euro = Currency.getInstance(new Locale("de", "DE"));
NumberFormat format = NumberFormat.getCurrencyInstance();
format.setCurrency(euro);
String text = format.format(19.99);
```

One change to know about: on iOS, `getCurrencySymbol()` now returns the symbol, `$`, where it used to return the code `USD`.

### Papercut, a paper shooter for your lunch break

Papercut is a twin-stick arena shooter drawn in cut paper and ink on a notebook page. You fly a paper quill, your right thumb aims the pencil, and cranes split into cranelets when you hit them. Ink stains stay on the page, which ripples like a grid of springs. It has three modes, including a daily shared seed, leaderboards with scores the server verifies, a coin shop and achievements. Like every one of our samples, it's a template in the [Initializr](https://start.codenameone.com/) and free MIT-0 source.

{{< guide-block >}}
<video controls playsinline preload="metadata" poster="/papercut/papercut-desktop-poster.jpg" style="width:100%;height:auto;display:block;margin:16px auto" aria-label="Playing Papercut on the desktop"><source src="/papercut/papercut-desktop.mp4" type="video/mp4"><a href="/papercut/papercut-desktop.mp4">Download the Papercut video</a>.</video>
{{< /guide-block >}}

### Xcode 27.1 and Gradle 9 are now the defaults

Builds without a version hint now use Xcode 27 on the build cloud, and Android builds use Gradle 9.8.0 with AGP 9.4.1. We do this every year, for the same reason: Apple and Google set the floor for store submissions, and the toolchain is where the new APIs, the new privacy rules and the security fixes live. Moving early, on our schedule, means your app never gets stuck when a store deadline lands on a Friday.

Gradle 9 turns on only when we know your project works with it. If you add your own Gradle plugins, apply `kotlin-android`, pin google-services below 4.4 or use Huawei push, the build stays on Gradle 8 and the log says why. If something does break, two build hints put you back where you were while you sort it out:

```properties
codename1.arg.ios.xcode_version=26
codename1.arg.android.gradleVersion=8
```

Please tell us when you need them. A hint you rely on is a bug we want to fix.

## Go build the next great thing

We're entering an era of new possibilities. A complex app, a commerce platform with a register, a back office and a web store, can now be taken apart and remixed in days. The code is no longer the limit. Your vision is.

Generate Bazaar from the [Initializr](https://start.codenameone.com/), point your agent at our [llms.txt](/llms.txt), and make it yours. Take this and go build the next great thing. Tomorrow: {{< post-link path="/blog/word-documents-and-presentations-in-your-app" text="Word and PowerPoint inside your app" >}}.
