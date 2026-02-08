---
title: "Themeing"
date: 2015-03-03
slug: "themeing"
---

# Themeing

Build a Codename One theme and leverage the native theme layer

1. [Home](/)
2. Developers
3. Themeing

<iframe src="https://www.youtube.com/embed/LLJyWt_eDRw?rel=0" width="640" height="360" frameborder="0" allowfullscreen="allowfullscreen"></iframe>

### When Creating a Codename One Theme The Default Uses The Platform Native Theme

![](images/themeing-1.jpg)

You can easily create a theme with any look you desire or you can "inherit" the platform native theme and start from that point. When adding a new theme you are given the option.

### Any Theme Can Be Made To Derive A Native Theme

![](images/themeing-2.jpg)

Codename One uses a theme constant called "includeNativeBool", when that constant is set to true Codename One starts by loading the native theme first and then applying all the theme settings. This effectively means your theme "derives" the style of the native theme first, similar to the cascading effect of CSS.

By avoiding this flag you can create themes that look EXACTLY the same on all platforms.

### You Can Simulate Different OS Platforms By Using The Native Theme Menu

![](images/themeing-3.jpg)

Developers can pick the platform of their liking and see how the theme will appear in that particular platform by selecting it and having the preview update on the fly.

### You Can Easily Create Deep Customizations That Span Across All Themes

![](images/themeing-4.jpg)

In this case we just customized the UIID of a label and created a style for the new UIID. When deriving a native theme its important to check the various platform options to make sure that basic assumptions aren't violated. E.g. labels might be transparent on one platform but opaque on others. Or labels might look good in a dialog in Android but look horrible in an iOS dialog (hint: use the DialogBody UIID for text content within a dialog).

### Codename One Allows You to Override a Resource For A Specific Platform

![](images/themeing-5.jpg)

A common case we run into when adapting for platform specific looks is that a specific resource should be different to match the platform conventions. The Override feature allows us to define resources that are specific to a given platform combination. Override resources take precedence over embedded resources thus allowing us to change the look or even behavior (when overriding a GUI builder element) for a specific platform/OS.

### To Override Just Select The Platform Where Overriding Is Applicable

![](images/themeing-6.jpg)

Then click the green checkbox to define that this resource is specific to this platform. All resources added at this point will only apply to the given platform. If you change your mind and are no longer interested in a particular override just delete it in the override mode and it will no longer be overridden.

### In This Case We Just Select a New Image Object Applicable To This Platform

![](images/themeing-7.jpg)

This is easily done by selecting the "..." button. We can easily do the same in the GUI builder although this is a dangerous road to start following since it might end up with a great deal of fragmentation.
