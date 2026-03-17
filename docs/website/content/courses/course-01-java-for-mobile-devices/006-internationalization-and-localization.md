---
title: "Internationalization and Localization"
layout: "course-lesson"
course_id: "course-01-java-for-mobile-devices"
course_title: "Java for Mobile Devices - Free online course"
module_title: "Course Lessons"
module_key: "01-course-lessons"
module_order: 1
lesson_order: 6
weight: 6
is_course_lesson: true
description: "Internationalization, localization, RTL support, and locale-aware formatting in modern Codename One projects."
---
> Module 1: Course Lessons

{{< youtube 32mkZymqa6E >}}

Internationalization and localization are broader than translation. Internationalization means structuring the app so it can adapt to different locales. Localization is the work of adapting the app to a specific locale. Language is part of that, but so are dates, numbers, currency, right-to-left behavior, phrasing, and the cultural meaning of visual choices.

The first practical rule is to stop hard-coding user-facing text directly into the UI wherever possible. Codename One is designed to work with key/value bundles so that the text shown to the user can change with the current locale. The older video demonstrates this through the resource editor. For new projects, the better default is l10n property bundles, but the core idea remains the same: components should reference localizable keys, not baked-in strings that force you to revisit the code for every translation.

This matters because translation is only the first layer. Locale-specific behavior also affects the way you display dates, numbers, and currency. If you use locale-aware formatting utilities, the app can present those values in a way that feels natural to the user instead of forcing one fixed representation on every market. In Codename One, `L10NManager` and the framework's localization utilities are the right place to start for this kind of formatting.

Testing localization also needs to be part of the normal development loop. It is much easier to catch problems early if you force the simulator into a different language and inspect the UI there. A localized build should not only show translated strings. It should still fit properly, align correctly, and feel intentional when labels grow longer or date and number formatting changes.

Right-to-left support is one of the most important areas to get right. Languages such as Hebrew and Arabic do not just translate the text. They change the expected flow of the interface. Text aligns differently, component order often reverses, and icons or directional affordances may need to be mirrored. Codename One helps a lot here because layouts react to RTL mode by flipping positions and alignment where appropriate. A `BorderLayout.EAST` relationship, for example, is interpreted relative to the active writing direction.

That said, RTL is not automatic magic. Mixed-direction content still needs attention. Numbers are still read left-to-right even inside right-to-left languages, so bidirectional text can produce cursor movement and layout behavior that surprises developers who only test in English. Icons such as play arrows, chevrons, and back buttons also need review because a mirrored UI with unmirrored directional icons still feels wrong.

The video uses the older designer workflow to define bundles and RTL markers. That part is outdated for new projects, but the localization concepts themselves are still the right ones to learn. In current Codename One development, the better default is property-bundle based localization plus locale-aware formatting in code, with the layout system doing most of the heavy lifting for RTL-aware component ordering.

## Further Reading

- [Developer Guide](/developer-guide/)
- [Themeing](/themeing/)
- [Layout Basics](/layout-basics/)
- [Properties Are Amazing](/blog/properties-are-amazing/)
