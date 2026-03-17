---
title: "CSS Changes"
layout: "course-lesson"
course_id: "course-02-deep-dive-mobile-development-with-codename-one"
course_title: "Deep Dive into Mobile Development with Codename One - Free Online Course Material"
module_title: "UI Design From Scratch"
module_key: "04-ui-design-from-scratch"
module_order: 4
lesson_order: 4
weight: 13
is_course_lesson: true
description: "Refine the CSS so the newly added screens feel like part of the same product."
---
> Module 4: UI Design From Scratch

{{< youtube MLbOdCt67Rw >}}

Whenever new screens are added, the styling layer has to catch up. Otherwise the application ends up with correct layouts but mismatched visual language. This lesson is about extending the CSS so the new forms, overlays, and side menu feel like the same product rather than late additions.

That work is usually less about dramatic restyling and more about tightening the small things that carry consistency: gradients, padding, transparency, text treatment, separators, and inherited button styles. These are the pieces that make a screen feel related to the rest of the app even when its structure is different.

The newer forms in this module need that kind of refinement. The dish view, the contact screen, and the side menu all reuse ideas from earlier screens, but they each need their own UIIDs and spacing rules. Without those explicit rules, native defaults and inherited behavior start leaking in, and the app begins to look inconsistent even if the layouts are technically correct.

The side menu is a particularly good example. Side commands often receive strong native-theme defaults, so if you want them to look intentional you usually need to override more than you first expect. Background, spacing, text decoration, and separator behavior all need deliberate choices. Otherwise the menu keeps the behavior of the framework but not the visual identity of your application.

This lesson also reinforces a maintainability rule: if two components are visually related, their styles should be related too. Reuse through inheritance or shared style concepts is not just a convenience. It is how you keep the UI from drifting as the project grows. Modern Codename One projects should keep that work in CSS whenever possible rather than bouncing between multiple styling systems.

So the right way to think about these CSS changes is not "decorate the new screens." It is "extend the design system so the new screens belong." That mindset produces more durable styling decisions.

## Further Reading

- [Themeing](/themeing/)
- [Working With CSS](/courses/course-02-deep-dive-mobile-development-with-codename-one/001-working-with-css/)
- [CSS](/courses/course-02-deep-dive-mobile-development-with-codename-one/006-css/)
- [How Do I Create A Simple Theme](/how-do-i/how-do-i-create-a-simple-theme/)
