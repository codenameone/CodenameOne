---
title: "Create an Uber Clone in Java with Wayline"
description: "Build a white-label ride-hailing app with Wayline: rider and driver apps, admin console, Java backend, live tracking and payment workflows. Free MIT-0 source."
url: "/wayline/"
summary: "A complete full-stack ride-hailing application you can brand and extend."
images: ["/developer-guide/img/wayline-admin-desktop-dashboard.png"]
ShowToc: true
---

Wayline is a complete, full-stack ride-hailing application in Java: rider and driver workflows, an administration console, and a backend that handles accounts, matching, pricing, ride state and payment workflows. Use it to build your own branded taxi or ride-sharing application with Codename One.

**[Start the Wayline tutorial](/blog/create-an-uber-clone-in-1-minute/)** · **[Generate your project](/initializr/)** · **[Browse the source](https://github.com/codenameone/CodenameOne/tree/master/scripts/wayline)**

In the Initializr, choose **Wayline**, set your application name and package, and generate the project. The application source is licensed under [MIT-0](https://github.com/codenameone/CodenameOne/blob/master/scripts/wayline/LICENSE): you can modify it, use it commercially and keep your changes private without royalties.

## What comes with the application?

| Part | Included workflows |
| --- | --- |
| Rider | Choose a destination, request a fare quote, book a ride, follow the driver, view receipts and rate the trip |
| Driver | Apply with documents, go online, accept offers, follow trips and inspect earnings |
| Administration | Review driver applications, manage accounts, inspect rides and payments, configure pricing and view the live map |
| Java backend | Authentication, ride matching, pricing, database persistence and payment workflows |
| Shared code | API contracts and data classes shared between the client and server |

![Wayline administration console with service activity and trends](/developer-guide/img/wayline-admin-desktop-dashboard.png)

The screens connect to the backend and database. You can follow a ride through the application and inspect the code behind each step. The same client project targets iOS, Android, desktop and web.

## Follow the tutorial from first run to customization

1. **[Start with the application walkthrough](/blog/create-an-uber-clone-in-1-minute/).** See the architecture, screenshots and source examples.
2. **[Run the project](https://github.com/codenameone/CodenameOne/tree/master/scripts/wayline#running-it).** Follow the source README for the backend and client commands.
3. **[Try the rider, driver and admin workflows](/developer-guide/wayline-users-guide/).** The user's guide covers demo accounts, screens and test payments.
4. **[Customize and deploy the application](/developer-guide/wayline-implementors-guide/).** The implementor's guide covers branding, database configuration, maps, authentication, messaging and hosting.

## What does a live service still need?

Wayline supplies the connected application and its source. Running a transport service also requires your own hosting, service configuration and operational testing.

Development uses simulated payments. The Stripe integration is a starting point tested with stand-in responses; it has not been validated against a Stripe account. Before accepting real money, follow the implementor's guide to test the integration and complete the webhook, authentication and payout handling your service needs. Maps, SMS, push and other providers need their own configuration and may charge fees.

## Coming from Create an Uber Clone in 7 Days?

Wayline is the current starting point for a new ride-hailing project. The older book and course used a different application and server; they remain useful historical learning material. Wayline has its own source and architecture, so follow its guides when building a new project.

[Open the new Wayline tutorial](/blog/create-an-uber-clone-in-1-minute/) or [continue the original full-stack Java course](/course-03-build-real-world-full-stack-mobile-apps-java/).

## Frequently asked questions

### Is the backend included?

Yes. Wayline includes the Java backend, shared API contracts, client application and administration workflows in one Maven project. You can inspect and change both ends of a request.

### Can I use my own branding and sell the application?

Yes. The Wayline application source uses MIT-0, which permits commercial use and modification without attribution or royalties. Dependencies and external services retain their own licenses and terms.

### Is the source free?

Yes. Hosting, external providers and any Codename One build-service plan you choose are separate from the free application source. See [Codename One pricing](/pricing/) for build-service options.

### Does generating the project launch a working taxi business?

Generating the project gives you the application code. A live service still needs provider setup, deployment, payment validation and testing for your operation. The [implementor's guide](/developer-guide/wayline-implementors-guide/) describes that work.

Uber is a trademark of Uber Technologies Inc. Wayline is an independent educational and customizable application, with no affiliation to Uber.
