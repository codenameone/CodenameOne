---
title: "Overview and Basic Model"
layout: "course-lesson"
course_id: "course-02-deep-dive-mobile-development-with-codename-one"
course_title: "Deep Dive into Mobile Development with Codename One - Free Online Course Material"
module_title: "Architecture"
module_key: "05-architecture"
module_order: 5
lesson_order: 1
weight: 15
is_course_lesson: true
description: "Move from visual prototyping to a clearer application model and architecture."
---
> Module 5: Architecture

{{< youtube mFFjxs9EDW8 >}}

At some point a project can no longer live as a collection of forms and helper methods. Once the UI starts to stabilize, the next step is to make the data model explicit and decide where application state should live. That shift is what this lesson is about.

The important thing here is not to overreact and invent a giant architecture too early. The lesson takes a more useful approach: start from the things the UI already proved are necessary. If the app clearly revolves around dishes, categories, orders, and restaurant-level information, then those are the right places to start shaping the model.

That is a healthier way to design architecture than beginning with abstract diagrams and hoping the product eventually fits them. A menu screen implies a menu model. A checkout flow implies an order model. Restaurant branding, address, contact details, and currency imply some application-level object that represents the current restaurant state. The UI has already told you what the model needs to be.

The singleton-style restaurant object used in the lesson is a pragmatic choice for a demo-sized application. It centralizes the state the rest of the UI depends on and keeps the app easy to reason about while the architecture is still evolving. In a larger system you might eventually split responsibilities further, but this is a reasonable starting point because it matches the actual scope of the app.

This lesson also hints at a broader modeling principle: not everything needs the same identity rules. Some objects naturally need stable identifiers because they represent persistent records or externally referenced entities. Others exist more as configuration or supporting data. It is fine for the model to reflect that difference instead of forcing uniformity where it is not useful.

The result of this first architecture pass is not a grand framework. It is a cleaner separation between the UI and the data it presents. That alone makes the next set of changes easier, because once the model is explicit, the forms stop having to carry hidden application state in ad hoc ways.

## Further Reading

- [Developer Guide](/developer-guide/)
- [How Do I Use Properties To Speed Development](/how-do-i/how-do-i-use-properties-to-speed-development/)
- [Connecting to a Web Service](/courses/course-02-deep-dive-mobile-development-with-codename-one/003-connecting-to-a-web-service/)
