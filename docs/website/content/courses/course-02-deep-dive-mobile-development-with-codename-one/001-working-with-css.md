---
title: "Working with CSS"
layout: "course-lesson"
course_id: "course-02-deep-dive-mobile-development-with-codename-one"
course_title: "Deep Dive into Mobile Development with Codename One - Free Online Course Material"
module_title: "Working with CSS"
module_key: "01-working-with-css"
module_order: 1
lesson_order: 1
weight: 1
is_course_lesson: true
description: "Understand how CSS fits into modern Codename One projects."
---
> Module 1: Working with CSS

This lesson explains how CSS fits into a modern Codename One project and how it should be used in day-to-day styling work.

{{< youtube UxZ1HeheGwU >}}

For most new Codename One projects, CSS is the first place to reach when you want to style the UI. It is the most practical way to control colors, borders, fonts, spacing, and component states without pushing visual concerns into Java code. The video introduces CSS as an optional plugin, but that part is out of date now. In current Codename One development, CSS is a normal part of the workflow rather than something experimental.

That does not mean CSS replaces the Codename One theme system entirely. It means CSS is now the most practical way to drive that system. UIIDs still matter. Theme values still matter. The difference is that instead of editing everything through the older designer workflow, you usually define and evolve those visual decisions in CSS and let the build process generate the underlying resources as needed.

This is also why Codename One CSS feels familiar without being identical to browser CSS. Selectors, colors, borders, and states look recognizable, which is useful, but the framework has its own custom properties and behavior. You are styling Codename One components, not HTML elements. So it is better to think of CSS here as a clean styling language for Codename One rather than expecting arbitrary snippets from the web to drop in unchanged.

The easiest way to learn this is to style one simple component first. Take a button, give it a UIID, define its appearance in CSS, and rerun the application. That small loop teaches the core idea quickly: you describe the look once, and Codename One applies it consistently across the relevant states instead of forcing you to rebuild that appearance in Java code.

A Codename One CSS file works with the theme system rather than bypassing it. That is why theme overlays, UIIDs, and precedence still matter. If a base theme defines one thing and your CSS defines another, the later layer wins. Understanding that relationship makes it much easier to predict why a style is or is not being applied.

Another place where the video is dated is the role of the designer and resource editor. They were much more central to styling workflows at the time. That is no longer the best default. CSS should be the main styling layer in a new project, and localization should usually live in l10n property bundles rather than the older designer-driven localization flow.

In practice, the healthiest split is simple. Use layouts and component structure to define how the UI behaves. Use CSS to define how it looks. Use resource files only where they still make sense for assets that genuinely belong there. That separation keeps screens easier to evolve and makes style changes far less invasive than they were in older projects.

Once you approach the topic that way, the rest falls into place. Layouts define structure. CSS defines appearance. Resource files still have a role for the assets that belong there, but they are no longer where most visual work should begin.

## Further Reading

- [Themeing](/themeing/)
- [Designer](/designer/)
- [Developer Guide](/developer-guide/)
- [Layout Basics](/layout-basics/)
- [Moving To Maven](/blog/moving-to-maven/)
