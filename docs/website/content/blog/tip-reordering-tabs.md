---
title: 'TIP: Reordering Tabs'
slug: tip-reordering-tabs
url: /blog/tip-reordering-tabs/
original_url: https://www.codenameone.com/blog/tip-reordering-tabs.html
aliases:
- /blog/tip-reordering-tabs.html
date: '2019-05-13'
author: Shai Almog
---

![Header Image](/blog/tip-reordering-tabs/tip.jpg)

The `Tabs` class is pretty powerful and flexible as I [mentioned before](https://www.codenameone.com/blog/tip-customize-tabs-behavior.html). One thing it doesn’t support is drag and drop to re-order the tabs. Here the flexibility of Codename One takes over and allows us to accomplish it.

We can use the builtin drag and drop support for components within the tabs container. This is exactly how the code below works. In the drop listener we block the the default processing of drop behavior so we can move the tab manually to the new location:
    
    
    Form hi = new Form("Tabs", new BorderLayout());
    
    Tabs t = new Tabs();
    
    t.addTab("T1", new Label("Tab 1"));
    t.addTab("T2", new Label("Tab 2"));
    t.addTab("T3", new Label("Tab 3"));
    t.addTab("T4", new Label("Tab 4"));
    
    Container tabsC = t.getTabsContainer();
    tabsC.setDropTarget(true);
    for(Component c : tabsC) {
        c.setDraggable(true);
        c.addDropListener(e -> {
            e.consume();
            Component dragged = c;
            int x = e.getX();
            int y = e.getY();
            int i = tabsC.getComponentIndex(dragged);
            if(i > -1) {
                Component dest = tabsC.getComponentAt(x, y);
                if(dest != dragged) {
                    Component source = t.getTabComponentAt(i);
                    int destIndex = tabsC.getComponentIndex(dest);
                    if(destIndex > -1 && destIndex != i) {
                        String title = t.getTabTitle(i);
                        t.removeTabAt(i);
                        t.insertTab(title, null, source, destIndex);
                    }
                }
                tabsC.animateLayout(400);
            }
        });
    }
    
    hi.add(CENTER, t);
    hi.show();
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Stefan Eder** — May 16, 2019 at 10:06 am ([permalink](https://www.codenameone.com/blog/tip-reordering-tabs.html#comment-24107))

> Stefan Eder says:
>
> Dragging and dropping a tab as in this example doesn’t feel right. Instead the tabs should make room while dragging and not move again into place when dropping. That way it would be much more natural.
>
> See [https://github.com/codename…](<https://github.com/codenameone/CodenameOne/issues/2800>)
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Ftip-reordering-tabs.html)

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
