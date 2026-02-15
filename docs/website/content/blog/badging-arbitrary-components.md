---
title: Badging Arbitrary Components
slug: badging-arbitrary-components
url: /blog/badging-arbitrary-components/
original_url: https://www.codenameone.com/blog/badging-arbitrary-components.html
aliases:
- /blog/badging-arbitrary-components.html
date: '2016-11-02'
author: Shai Almog
---

![Header Image](/blog/badging-arbitrary-components/badge-floating-button.png)

Last week a question came up in [stackoverflow](http://stackoverflow.com/questions/40256864/how-to-create-facebook-like-notification-badges-in-codenameone)  
that came out quite a few times before but this time I had a better answer thanks to the round border. After giving  
that answer I recalled that I already wrote some code to implement badging in the [FloatingActionButton](/blog/floating-button.html)  
but never exposed it because of some bugs…​

So I took the time and looked at the bugs in that code, turns out it was pretty trivial to fix so the same sample  
I gave for stackoverflow can now look like this:
    
    
    Form hi = new Form("Badge");
    
    Button chat = new Button("");
    FontImage.setMaterialIcon(chat, FontImage.MATERIAL_CHAT, 7);
    
    FloatingActionButton badge = FloatingActionButton.createBadge("33");
    hi.add(badge.bindFabToContainer(chat, Component.RIGHT, Component.TOP));
    
    TextField changeBadgeValue = new TextField("33");
    changeBadgeValue.addDataChangedListener((i, ii) -> {
        badge.setText(changeBadgeValue.getText());
        badge.getParent().revalidate();
    });
    hi.add(changeBadgeValue);
    
    hi.show();

That’s shorter (mostly because of default styles) and pretty neat in general. It results in this:

![Badge floating button in action](/blog/badging-arbitrary-components/badge-floating-button.png)

Figure 1. Badge floating button in action

Ideally I’d like to continue this trend into validators and other builtin tools to use more of the builtin borders and  
material icons. This is something we need to work on more as we move forward.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **João Bastos** — November 3, 2016 at 8:47 am ([permalink](/blog/badging-arbitrary-components/#comment-22949))

> createBadge error. Is it missing “public” statement in the method? or i´m missing something here?
>


### **Shai Almog** — November 4, 2016 at 5:47 am ([permalink](/blog/badging-arbitrary-components/#comment-22930))

> Shai Almog says:
>
> Sorry, I neglected to mention that this is landing in the Friday release today so when you get a new update the code should compile fine. You can use Update Client Libraries later today to get the update.


### **João Bastos** — November 4, 2016 at 8:43 am ([permalink](/blog/badging-arbitrary-components/#comment-23013))

> João Bastos says:
>
> Ok. Thanks Shai

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
