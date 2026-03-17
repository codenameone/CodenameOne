---
title: "The Native Interface Callback"
layout: "course-lesson"
course_id: "course-02-deep-dive-mobile-development-with-codename-one"
course_title: "Deep Dive into Mobile Development with Codename One - Free Online Course Material"
module_title: "Native Interfaces - Billing"
module_key: "06-native-interfaces-billing"
module_order: 6
lesson_order: 1
weight: 17
is_course_lesson: true
description: "Understand how to design a native interface and bridge callback-based native APIs back into Java."
---
> Module 6: Native Interfaces - Billing

{{< youtube jVuNrLw4e-A >}}

Native interfaces are most useful when the Java side stays small, deliberate, and easy to reason about. The mistake developers often make is trying to expose every detail of a native SDK directly into the portable layer. That usually creates a fragile API that mirrors platform quirks instead of hiding them.

This lesson starts from a payment use case and shows a better approach. Look at the native SDK first, identify the smallest set of information the Java layer truly needs, and then define a narrow native interface around that. In the billing example, most of the heavy lifting is already handled by the native SDK and by the server. The portable layer mainly needs a token and a way to receive the outcome.

The callback part is where the design gets interesting. Native SDKs often return results asynchronously, but Codename One native interfaces cannot simply accept arbitrary Java callback objects in every situation. That constraint means you need a bridging strategy. In the older lesson that strategy uses static callback methods. The exact implementation detail can vary, but the underlying lesson is still right: the Java-facing API should absorb the awkward platform boundary so the rest of the application does not have to care about it.

That is also why the native interface itself should almost never be used directly from the UI or business layer. Wrap it in a plain Java abstraction. That wrapper can normalize differences between platforms, centralize fallback behavior, and keep the rest of the codebase isolated from native-specific edge cases.

The video uses Braintree as the concrete example, but there is an important modern note here: when an official or maintained Codename One library already exists for an integration, prefer that over re-creating the binding yourself. Native interface work is still valuable to understand, and sometimes you do need it for unsupported features, but it should not be the first choice when a stable library already covers the problem.

So the real takeaway from this lesson is not "copy this payment bridge." It is "design the narrowest native boundary you can, then wrap it so the application talks to a clean Java API instead of a pile of platform details."

## Further Reading

- [Developer Guide](/developer-guide/)
- [How Do I Access Native Device Functionality, Invoke Native Interfaces](/how-do-i/how-do-i-access-native-device-functionality-invoke-native-interfaces/)
- [How Do I Use The Include Sources Feature To Debug The Native Code On iOS/Android](/how-do-i/how-do-i-use-the-include-sources-feature-to-debug-the-native-code-on-iosandroid-etc/)
