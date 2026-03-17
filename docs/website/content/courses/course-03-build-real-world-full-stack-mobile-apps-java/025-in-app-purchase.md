---
title: "In-App Purchase"
layout: "course-lesson"
course_id: "course-03-build-real-world-full-stack-mobile-apps-java"
course_title: "Build Real World Full Stack Mobile Apps in Java - Free Course"
module_title: "Push and In-App Purchase"
module_key: "08-push-and-in-app-purchase"
module_order: 8
lesson_order: 6
weight: 25
is_course_lesson: true
description: "Use the store billing flow to sell in-app items and continue the build workflow once payment succeeds."
---
> Module 8: Push and In-App Purchase

{{< youtube uvQKs_PdB64 >}}

In-app purchase is one of those features where the code is usually easier than the store setup. Apple and Google own the payment flow, product definitions, pricing, refunds, and approval rules. Your app mostly needs to define what is being sold and decide what to unlock when a purchase succeeds.

That store ownership is not just convenient. For digital goods and in-app features, it is often a platform requirement. If the user is buying something that exists inside the app experience, the stores generally expect that transaction to go through their billing system rather than through a custom payment form.

In this project, the purchase flow is tied directly to the build workflow. The user chooses a build target, buys that build if necessary, and then the normal build process continues. That is a sensible product design because the purchase does not create a separate mini-application inside the app. It simply unlocks the next step the user was already trying to perform.

Like push, purchase callbacks belong in the main application class. The app starts a purchase, waits for the store result, and then handles success, failure, cancellation, or refund through the appropriate callback methods. The most important callback is the successful purchase event, which includes the SKU of the item that was bought.

The SKU is the bridge between store configuration and application logic. You define it in App Store Connect or Google Play Console, and the app uses it to decide what the purchase means. Once you have more than one purchasable item, that mapping becomes the heart of your purchase logic.

The lesson keeps the client-side code intentionally small: present a purchase choice, start the purchase, and continue into the pending build flow once the store confirms success. That is the right scope. Most of the complexity in in-app purchase is not in the callback methods. It is in product configuration, testing, and making sure the entitlement model is correct.

The warnings about store policy from the video are still worth taking seriously. Apple in particular is strict about in-app purchase rules, and review feedback can be broader than many developers expect. If a reviewer believes something inside the app experience should be sold through store billing, you should assume they may challenge any alternative payment path.

The exact screens shown in the video are dated now, but the overall store workflow has not really changed. You create the product, assign its SKU, give it display text and pricing, and make sure the application uses the same SKU constants when purchase callbacks arrive.

## Further Reading

- [Push 3 - The Server Side and Build Logic](/courses/course-03-build-real-world-full-stack-mobile-apps-java/022-push-3-the-server-side-and-build-logic/)
- [Billing and Global Server](/courses/course-03-build-real-world-full-stack-mobile-apps-java/013-billing-and-global-server/)
- [In-App Purchase](/in-app-purchase/)
- [What Is Codename One](/courses/course-01-java-for-mobile-devices/004-what-is-codename-one/)
