---
title: Toolbar Default, Millimeter Sizes & Weekly Releases
slug: toolbar-default-millimeter-sizes-weekly-releases
url: /blog/toolbar-default-millimeter-sizes-weekly-releases/
original_url: https://www.codenameone.com/blog/toolbar-default-millimeter-sizes-weekly-releases.html
aliases:
- /blog/toolbar-default-millimeter-sizes-weekly-releases.html
date: '2016-03-06'
author: Shai Almog
---

![Header Image](/blog/toolbar-default-millimeter-sizes-weekly-releases/generic-java-1.jpg)

Starting with the next update of Codename One (later this week) we will switch `Toolbar` on as the default for  
all newly created projects. This doesn’t mean much for most of us as existing projects won’t be affected, however  
if you are creating a new project this means you won’t need to create a `Toolbar` for every `Form` and that we  
won’t have to deal with as many issues related to the native Android title.

This is clearly the way to go, as we look at modern mobile apps even from Google itself. They use the side menu  
for almost all apps and eschew the overflow for most usages (e.g. gmail). By focusing on the `Toolbar` we  
can further refine it and add better support for its appearance within the templates.

As part of that we already refined the overflow button a bit and the default padding for the `TitleArea` etc. We will  
probably add additional refinements e.g. the drop shadow for the Android theme.

### Millimeter Sizes

We added a new API to handle millimeters to pixel conversion: `Display.convertToPixels(float)`. This API accepts  
a floating point value which is more convenient than the existing integer based API. Ideally we’d want this to  
propagate as an option into the designer tool but that is not trivial as it requires a change in the resource file  
format. It’s something we plan to do as we move forward though.

We also added the ability to create a material icon with an explicit millimeter size:  
`FontImage.createMaterial(char icon, Style s, float size)`

This is useful as the current approach of picking the size of the style font doesn’t always work as we’d want especially  
in cases where we want the icon to be larger than the text.

### Docs Update & Weekly Releases

While we made a lot of progress with the docs I’ve slowed down the server updates for them. The main issue is that  
we are moving too quickly with the docs and don’t have time to release minor changes that we make to the API’s.

E.g. newer samples in the docs already use the API’s mentioned above as well as some other newer API’s. This  
causes a situation where developers can’t compile our samples when working against the latest version. One  
solution for this is to make a weekly release schedule instead of our current “ad hoc” updates which are problematic.  
This way we can both update the docs and the binary in the same time and also have a clearer schedule of  
library updates (notice that library updates don’t necessarily include a plugin update).

So right now the tentative plan is to have a weekly release every Friday to include all the stuff we worked on  
over the week. This week we’ll probably release a bit earlier due to the plugin update but this should start next  
week.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Lukman Javalove Idealist Jaji** — March 8, 2016 at 4:59 am ([permalink](https://www.codenameone.com/blog/toolbar-default-millimeter-sizes-weekly-releases.html#comment-21502))

> Lukman Javalove Idealist Jaji says:
>
> Great…I’ve been trying to use StatusBar ToastMessages and the signature component but all to no avail….Even when I updated my plugin…This is a better approach…and also when you anounce feature and even show a demo of it, let the releae time of the feature into the core API not be too long from the time of incorporation into the API as with the StatusBar and others…


### **bryan** — March 9, 2016 at 7:26 am ([permalink](https://www.codenameone.com/blog/toolbar-default-millimeter-sizes-weekly-releases.html#comment-22736))

> bryan says:
>
> Is there any chance you can create a new demo app which shows most of these new features. All the existing demos are semi-obsolete, and whilst the docs provide example snippets, it’s not always obvious how to put everything together.


### **Shai Almog** — March 10, 2016 at 3:48 am ([permalink](https://www.codenameone.com/blog/toolbar-default-millimeter-sizes-weekly-releases.html#comment-22601))

> Shai Almog says:
>
> This was updated yesterday so all these API’s should be there now.
>
> I agree. We are trying to be more organized all across the board and having a proper release schedule is part of that organized mentality.


### **Shai Almog** — March 10, 2016 at 3:51 am ([permalink](https://www.codenameone.com/blog/toolbar-default-millimeter-sizes-weekly-releases.html#comment-22552))

> Shai Almog says:
>
> Refreshing the demos is high on our priority and will probably happen in tandem with new video tutorials/courses.
>
> We need to update all demos to Java 8 as most of them are currently out of date. Since the docs are now mostly targeted at Java 8 we need to do the same there.
>
> We also need new demos that are more refined, the kitchen sink is REALLY old by now and we need a new version of that. We consider all of that work as “documentation” which is currently one of our top 4 priorities.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
