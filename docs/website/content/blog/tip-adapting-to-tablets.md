---
title: 'TIP: Adapting to Tablets'
slug: tip-adapting-to-tablets
url: /blog/tip-adapting-to-tablets/
original_url: https://www.codenameone.com/blog/tip-adapting-to-tablets.html
aliases:
- /blog/tip-adapting-to-tablets.html
date: '2019-03-04'
author: Shai Almog
---

![Header Image](/blog/tip-adapting-to-tablets/tip.jpg)

A while back someone asked on stackoverflow [how to adapt a Codename One app to tablets](https://stackoverflow.com/questions/54269508/how-to-structure-the-cn1-code-for-a-tablet-form-layout). I provided quite a few references in the answer and following discussion but I think a better approach is to explain what we did with the recent Codename One Build app because that’s what I’ve been doing in all recent apps I worked on.

I call this approach the “phone first” approach for universal app development. It starts by forgetting about the tablet and focusing on building a good looking phone app. In this app I usually subclass `Form` for all the classes which instantly creates an app that’s very suitable for phones.

When this is done I give some thought to how I would like the app to work as a tablet app. In general I want the app to have a [permanent side menu](/blog/permanent-sidemenu-getAllStyles-scrollbar-and-more/) and one `Form` where the content is replaced.

To accomplish this I change all the existing subclasses of `Form` so they will derive from my private class `BaseForm` which is basically something like this:
    
    
    public class BaseForm extends Container {
        private String title;
        private static Form tabletForm;
    
        public BaseForm(String title, Layout l) {
            super(l);
            this.title = title;
            if(!(l instanceof BorderLayout)) {
                setScrollableY(true);
            }
        }
    
    // rest of code
    }

Since there is only one form in the tablet I can keep it as a static global, that’s obvious.

But why is the title a variable?

Where are the rest of the methods we expect in `Form`?

Simple, we create every method thats missing and make it work in such a way that makes sense for our applications tablet design. E.g. this is `show()` which is one of the bigger methods here:
    
    
    public void show() {
        if(isTablet()) {
            if(tabletForm == null) { __**(1)**
                tabletForm = new Form(title, new BorderLayout()); __**(2)**
                getUnselectedStyle().setBgTransparency(255); __**(3)**
                UIUtil.createSideMenu(tabletForm);
                tabletForm.add(CENTER, this);
                tabletForm.show();
            } else {
                replaceTabletForm(true); __**(4)**
            }
        } else {
            if(getParent() != null) { __**(5)**
                getComponentForm().show();
            } else {
                Form f = new Form(title, new BorderLayout());
                UIUtil.createSideMenu(f);
                f.add(CENTER, this); __**(6)**
                f.show();
            }
        }
    }
    private void replaceTabletForm(boolean dir) {
        getUnselectedStyle().setBgTransparency(255);
        Container c = tabletForm.getContentPane();
        c.replaceAndWait(c.getComponentAt(0), this, CommonTransitions.createCover(CommonTransitions.SLIDE_HORIZONTAL, dir, 300));
        tabletForm.setTitle(title); __**(7)**
    }

__**1** | If this is the first form shown we create a new `Form` for the case of a tablet  
---|---  
__**2** | The `BaseForm` is in the center of a border layout which means it will take the available space, we can use more creative layouts e.g. with 3 panes  
__**3** | By default `Container` is transparent but this looks bad in replace animation so for tablets we make the containers opaque  
__**4** | If this isn’t the first form we just use the replace method below  
__**5** | For a phone we wrap a `BaseForm` in a `Form` so they are effectively identical, we can reuse the `Form` instances  
__**6** | This code is almost identical to the code in the tablet mode but happens multiple times  
__**7** | Since we reuse the same form in the tablet mode we need to update the title value  
  
Once we do this there would be compilation errors for various methods of `Form` that we relied on. These are mostly easy to fix as they just mean implementing the logic for every `Form` method you need e.g.:
    
    
    public void showBack() {
        if(isTablet()) {
            replaceTabletForm(false);
        } else {
            getComponentForm().showBack();
        }
    }

#### Generalization via API

I tried to generalize this process through a standardized API multiple times in the past and failed. It’s easy to solve the simple use cases but converting a “best practice” into an API is more challenging.

If you have thoughts or ideas on how this approach can be adapted to create a more versatile API I’m open to suggestions.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
