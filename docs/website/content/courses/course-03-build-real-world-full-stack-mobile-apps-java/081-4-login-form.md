---
title: "4. Login Form"
layout: "course-lesson"
course_id: "course-03-build-real-world-full-stack-mobile-apps-java"
course_title: "Build Real World Full Stack Mobile Apps in Java - Free Course"
module_title: "Creating a Facebook Clone"
module_key: "13-creating-a-facebook-clone"
module_order: 13
lesson_order: 4
weight: 81
is_course_lesson: true
description: "Build a login form that adapts across platforms and orientations without pretending every device should look identical."
---
> Module 13: Creating a Facebook Clone

{{< youtube vdFr815Dhpg >}}

The login screen is the first place where the Facebook clone stops being an abstract styling exercise and starts making real product decisions. Facebook’s native login UI is inconsistent across platforms, and the video handles that honestly by taking inspiration rather than trying to reproduce every awkward choice exactly.

That is the right instinct. A good clone should learn from the source product, not inherit every mistake. This lesson keeps the broad shape of the Facebook login flow while making clearer decisions about tablets, landscape mode, and how much the interface should diverge between iOS and Android.

One useful Codename One detail here is `ComponentGroup`. On iOS, grouped fields are a familiar pattern and the native theme already knows how to style first, middle, and last grouped entries. On Android, that same container can degrade gracefully into a more ordinary stacked layout without forcing the whole screen into an iOS look. That makes it a good example of using native theme behavior as an asset instead of trying to flatten every platform into one visual language.

The landscape treatment is also worth noticing. The lesson is not just making the same login form wider. It is changing the balance of the layout so the screen still feels intentional when the device rotates. That is much better than letting the form expand passively and hoping it still looks good.

The logo transition from the splash screen matters for the same reason. Naming the logo component so the morph transition can connect startup to login gives the first seconds of the app a sense of continuity, which is exactly the kind of polish users notice without consciously naming it.

The CSS work in this lesson is more than ornament. It defines which buttons should follow Android’s uppercase convention, which containers should carry platform-specific spacing, and how the login screen should hold together visually across form factors. This is where the earlier decision to use CSS starts paying off.

## Further Reading

- [3. Splash Screen](/courses/course-03-build-real-world-full-stack-mobile-apps-java/080-3-splash-screen/)
- [5. Rich Text View and Signup Form](/courses/course-03-build-real-world-full-stack-mobile-apps-java/082-5-rich-text-view-and-signup-form/)
- [Working with CSS](/courses/course-02-deep-dive-mobile-development-with-codename-one/001-working-with-css/)
- [Adapting a UI Design](/courses/course-01-java-for-mobile-devices/009-adapting-a-ui-design/)
