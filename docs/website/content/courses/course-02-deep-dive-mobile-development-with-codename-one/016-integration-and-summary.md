---
title: "Integration and Summary"
layout: "course-lesson"
course_id: "course-02-deep-dive-mobile-development-with-codename-one"
course_title: "Deep Dive into Mobile Development with Codename One - Free Online Course Material"
module_title: "Architecture"
module_key: "05-architecture"
module_order: 5
lesson_order: 2
weight: 16
is_course_lesson: true
description: "Connect the model to the UI and let application state drive the visible behavior."
---
> Module 5: Architecture

{{< youtube Ke8bjFdD1bc >}}

Once the model exists, the next question is whether the UI is truly using it or just sitting beside it. This lesson is the point where the restaurant, menu, order, and dish data start driving what the application shows instead of merely backing it in theory.

That integration is where architecture starts paying off. A category list no longer needs to be hard-coded because the menu model can supply it. Totals no longer need to be manually synchronized in several places because the order model can become the source of truth. Contact screens no longer need literal strings embedded in the UI because restaurant-level data can fill them in.

The listener-based update pattern in the lesson is especially important. When one part of the application changes shared state, the rest of the UI should respond through a clear mechanism instead of requiring scattered manual refresh logic. That keeps the application from turning into a patchwork of "remember to update this label too" style code.

This is also the point where formatting decisions become part of the model rather than accidental view logic. Currency is a good example. If the restaurant defines the currency, then the UI should present values through that lens rather than applying a generic formatting assumption in random places. Small decisions like that are a sign that the app is beginning to think in domain terms instead of just widget terms.

The wider lesson here is that good architecture does not need to arrive all at once. It can emerge from repeated cleanup as the UI becomes real. Start with the screens, identify the shared data they imply, move that data into an explicit model, then let the forms depend on that model instead of inventing their own local truths. That is often a much more productive path than trying to design the whole system upfront.

## Further Reading

- [Developer Guide](/developer-guide/)
- [Overview and Basic Model](/courses/course-02-deep-dive-mobile-development-with-codename-one/015-overview-and-basic-model/)
- [How Do I Use Properties To Speed Development](/how-do-i/how-do-i-use-properties-to-speed-development/)
- [How Do I Localize/Translate My Application, Apply i18n/l10n to My App](/how-do-i/how-do-i-localizetranslate-my-application-apply-i18nl10n-internationalizationlocalization-to-my-app/)
