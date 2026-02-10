---
title: Set Properties, Density PR and Short Material Icon Syntax
slug: set-properties-density-pr-short-material-icon
url: /blog/set-properties-density-pr-short-material-icon/
original_url: https://www.codenameone.com/blog/set-properties-density-pr-short-material-icon.html
aliases:
- /blog/set-properties-density-pr-short-material-icon.html
date: '2018-06-04'
author: Shai Almog
---

![Header Image](/blog/set-properties-density-pr-short-material-icon/new-features-1.jpg)

I have a lot to write about so today I’ll only focus on two of the several PR’s we handled over the last month. I’ll try to cover more over the rest of the week. Also as a friendly reminder we will migrate to API level 27 this Friday and the price of the [online course including the Facebook/Uber clone apps](https://codenameone.teachable.com/p/build-real-world-full-stack-mobile-apps-in-java) will go up next week…​  
If you didn’t sign up yet this is your chance.

### Set Properties

[ramsestom](https://github.com/ramsestom) contributed a [pull request](https://github.com/codenameone/CodenameOne/pull/2417) which filled in a gap in the properties API: `SetProperty`. I don’t use `Set` very often as `List` is typically more convenient so it’s something that I just didn’t notice.

You can now declare `SetProperty` as you would a declare a `ListProperty` e.g.:
    
    
    public final SetProperty<User, User> peopleYouMayKnow =
            new SetProperty<>("peopleYouMayKnow", User.class);

The nice thing about this PR is that it also refactored the code in a sensible way so `SetProperty` & `ListProperty` have a common base class `CollectionProperty` so there isn’t too much code duplication.

### Density PR

[ramsestom](https://github.com/ramsestom) also made a more [challenging change](https://github.com/codenameone/CodenameOne/pull/2430) to the way densities are managed. When we originally integrated this it caused a performance regression on Android which demonstrates just how hard it is to work with such huge PR’s.

This PR is still conflicting and I don’t have the time to go over some of the nuances that should be fixed there. It’s not something I want to merge at the last minute either as it’s a huge change. Ideally if this could be broken down to smaller PR’s we can adopt individually it would be far easier to merge again.

One crucial thing to keep in mind when you submit a PR is binary compatibility. Method signatures must match completely or things will fail badly for some users. This is also true when introducing new API’s, once we accept a PR we need to maintain it for years to come.

### Short Material Icon Syntax

One of the tricks I’ve been using recently is:
    
    
    import static com.codename1.ui.FontImage.*;

This lets me do things like:
    
    
    setMaterialIcon(send, MATERIAL_SEND);

That’s short and to the point. But why require a static import?

So I’ve added this method to `Label` (which is the base class of `Button` etc.):
    
    
    send.setMaterialIcon(FontImage.MATERIAL_SEND);

That might not seem very different but it carries one big nuance that will appear in the update this Friday. When you make changes to the theme they will be reflected in the material icon.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
