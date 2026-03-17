---
title: CREATE A GORGEOUS SIDEMENU
slug: how-do-i-create-gorgeous-sidemenu
url: /how-do-i/how-do-i-create-gorgeous-sidemenu/
type: howdoi
original_url: https://www.codenameone.com/how_di_i/how-do-i-create-gorgeous-sidemenu.html
tags:
- basic
- featured
- ui
description: Great UI needs great navigational interface
youtube_id: 99DAeP9LG6c
thumbnail: https://www.codenameone.com/wp-content/uploads/2020/09/hqdefault-8-1.jpg
---
{{< youtube "99DAeP9LG6c" >}}

A good side menu does two jobs at once. It gives the user a clear navigation structure, and it makes that structure feel intentional rather than bolted on. The old video builds this through the toolbar side-menu APIs and then styles the result in the theme designer. The basic navigation idea still works, but the modern styling path should usually be CSS rather than designer-driven theme editing.

The first step is structural rather than visual. Define the side-menu commands you actually need, give them clear labels, and choose icons that reinforce the meaning instead of decorating it. A side menu becomes cluttered quickly when it is used as a dumping ground for everything that does not fit elsewhere in the UI.

Once the commands exist, the real visual work is about hierarchy and spacing. The menu background, the command row style, the selected state, and the optional header area all need to feel like part of the same visual system. The video spends time on padding, alignment, color, and pressed-state styling, and those are still the right levers to care about. Touch targets need enough padding to feel comfortable, selected states need to be obvious, and typography needs to be readable before it tries to be clever.

The older workflow styles these pieces in the theme editor with UIIDs such as the side command and the side panel. In a new project, those same ideas should usually be expressed in CSS. That gives you a more maintainable styling workflow and makes it easier to keep the side menu aligned with the rest of the application theme.

The header area at the top of the menu is often what separates an ordinary side menu from one that feels designed. A logo, profile image, app title, or short tagline can make the menu feel anchored instead of generic. The video demonstrates this by building a top section from ordinary components and then adding it to the toolbar side menu. That is still the right way to think about it: the decorative header is just another piece of UI composition, not a special magic feature.

The main modern caution is not to over-invest in ornament if the menu is no longer the primary navigation pattern for the app. Some applications are now better served by bottom navigation, tabs, or a simpler toolbar structure. A side menu is still useful, but it should be chosen because it fits the navigation model, not because it was once the default mobile pattern.

## Further Reading

- [Themeing](/themeing/)
- [Developer Guide](/developer-guide/)
- [Layout Basics](/layout-basics/)
- [How Do I Create A Simple Theme](/how-do-i/how-do-i-create-a-simple-theme/)

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
