---
title: HANDLE EVENTS/NAVIGATION IN THE GUI BUILDER & POPULATE THE FORM FROM CODE
slug: how-do-i-handle-eventsnavigation-in-the-gui-builder-populate-the-form-from-code
url: /how-do-i/how-do-i-handle-eventsnavigation-in-the-gui-builder-populate-the-form-from-code/
type: howdoi
original_url: https://www.codenameone.com/how_di_i/how-do-i-handle-eventsnavigation-in-the-gui-builder-populate-the-form-from-code.html
tags:
- basic
- ui
description: Event/variable handling in the Codename One GUI builder is a bit different
  from typical desktop development
youtube_id: 3IC2qZ3wUO4
thumbnail: https://www.codenameone.com/wp-content/uploads/2020/09/hqdefault-26.jpg
---
{{< youtube "3IC2qZ3wUO4" >}}

The GUI builder workflow in Codename One is based on generated forms and generated state-machine code, which means events and screen population work a little differently from the desktop UI builders many Java developers are used to. The old video focuses on that generated workflow directly, and that is still the right mental model if you are maintaining a GUI-builder based project.

Navigation is easiest when you use the builder’s command and form-linking features instead of trying to wire everything manually. If a button simply needs to open another form, the generated navigation model is often the cleanest route because it keeps the relationship visible in the builder and preserves the expected back-navigation behavior automatically.

Event handling becomes more interesting when a component needs custom logic. In the builder-driven approach, you attach the event in the UI resource and then implement the generated handler in code. That part is easy to trip over if you forget that the generated base classes are updated when the resource file is saved. The video calls this out indirectly through the save cycle, and it is still one of the main stumbling points when working in this style.

Populating a form from code is really about timing. A generated form is not a permanently alive singleton sitting in memory waiting for you to reach into it. It is created when needed and discarded when it is no longer active. That is why screen-specific initialization belongs in the lifecycle hooks that run just before the form is shown, not in some random place that assumes the components already exist.

This is the part of the video that still matters most conceptually. If you want to change a label, fill a field, or update a screen-specific component, you do it at the point where that form is being prepared to appear. That way the components actually exist and the change is applied to the live form instance rather than to a screen that has already been discarded.

The modern caveat is that new Codename One projects are usually less GUI-builder centric than the old workflow assumes. Many teams now prefer code-based UI plus CSS for styling. But if you are working in the builder, the generated-form lifecycle is still the rule you need to understand: navigation is configured declaratively where possible, event handlers live in the generated override points, and screen population happens at form-show time.

## Further Reading

- [Developer Guide](/developer-guide/)
- [Layout Basics](/layout-basics/)
- [How Do I Create A Gorgeous Sidemenu](/how-do-i/how-do-i-create-gorgeous-sidemenu/)
- [How Do I Create A List Of Items](/how-do-i/how-do-i-create-a-list-of-items-the-easy-way/)

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
