---
title: Bottom Align
slug: bottom-align
url: /blog/bottom-align/
original_url: https://www.codenameone.com/blog/bottom-align.html
aliases:
- /blog/bottom-align.html
date: '2018-11-21'
author: Shai Almog
---

![Header Image](/blog/bottom-align/new-features-2.jpg)

Box layout Y is one of the most used layouts in Codename One. It’s a wonderful, easy to use layout that makes vertical scrollable layout trivial. I love its simplicity, but sometimes that simplicity goes too far. A good example of that is the a common layout where we have a button at the bottom of the screen.

Historically we solved this by nesting box into a border layout:
    
    
    Form f = new Form("Border Layout", new BorderLayout()); __**(1)**
    
    Container box = new Container(BoxLayout.y());
    box.setScrollableY(true); __**(2)**
    
    Button b = new Button("Add New Button");
    
    b.addActionListener(e -> {
        MultiButton mb = new MultiButton("Added Button");
        box.addComponent(0, mb);
        mb.setWidth(f.getWidth());
        mb.setY(f.getHeight());
        box.animateLayout(150);
    });
    
    f.add(SOUTH, b);
    f.add(CENTER, box);
    
    f.show();

__**1** | Border layout implicitly disables the default scrolling of the `Form`  
---|---  
__**2** | Because of that we need to scroll the box layout  
  
When launched the UI looks like this:

![Newly launched UI](/blog/bottom-align/bottom-y-layout-1.png)

Figure 1. Newly launched UI

![After adding a couple of elements it looks like this](/blog/bottom-align/bottom-y-layout-2.png)

Figure 2. After adding a couple of elements it looks like this

![After adding a lot of elements it looks like this](/blog/bottom-align/bottom-y-layout-3.png)

Figure 3. After adding a lot of elements it looks like this

Now this might be what you want. The add button is always clearly visible and easily accessible. However, in some cases this doesn’t work.

Lets say you want this exact behavior like we see in the first two images. But once we reach the edge of the form you want the button to act as if this was a regular box layout. Effectively the button would either align to the bottom of the form or the edge of the layout.

To accomplish this we are adding a new `yLast` mode in the `BoxLayout` which can be created using `BoxLayout.yLast()` or `new BoxLayout(BoxLayout.Y_AXIS_BOTTOM_LAST)`. E.g the code below will produce the exact same result for the first two images:
    
    
    Form f = new Form("Border Layout", BoxLayout.yLast()); __**(1)**
    Button b = new Button("Add New Button");
    b.addActionListener(e -> {
        MultiButton mb = new MultiButton("Added Button");
        f.addComponent(0, mb);
        mb.setWidth(f.getWidth());
        mb.setY(f.getHeight());
        f.getContentPane().animateLayout(150);
    });
    
    f.add(b);
    
    f.show();

__**1** | Box layout doesn’t disable the default scrollability of form  
---|---  
  
When it’s completely filled the button is pushed down out of the view area:

![The button scrolls down when there is no more space](/blog/bottom-align/bottom-y-layout-4.png)

Figure 4. The button scrolls down when there is no more space

I like this approach as it reduces clutter for the UI and leaves more space available. It doesn’t fit for all cases but it’s a valuable addition to the API. These changes will be available with the update we’ll release this Friday.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Francesco Galgani** — November 29, 2018 at 7:08 am ([permalink](https://www.codenameone.com/blog/bottom-align.html#comment-24075))

> Why did you use f.addComponent(0, mb);?
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fbottom-align.html)


### **Shai Almog** — November 29, 2018 at 7:35 am ([permalink](https://www.codenameone.com/blog/bottom-align.html#comment-24045))

> This adds the component to the first index in the component list. When I call add(Component) or addComponent(Component) it adds the component at the last offset which in this case will replace the existing “last component”. Here I added it to the top so a new entry will always appear first.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fbottom-align.html)


### **Francesco Galgani** — November 29, 2018 at 7:46 am ([permalink](https://www.codenameone.com/blog/bottom-align.html#comment-24088))

> So in your example all the MultiButtons are disposed in inverted order of insertion, right? And if we want that every new MultiButton is added as penultimate?
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fbottom-align.html)


### **Shai Almog** — November 29, 2018 at 8:24 am ([permalink](https://www.codenameone.com/blog/bottom-align.html#comment-23823))

> No buttons are disposed but the new button is added to the top of the list instead of the bottom. That way the last button is always the add button.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fbottom-align.html)


### **Francesco Galgani** — November 29, 2018 at 11:21 am ([permalink](https://www.codenameone.com/blog/bottom-align.html#comment-23894))

> Francesco Galgani says:
>
> Thank you for the quick reply. I’m sorry, my mistake: “disposed” is a false friend in my language, I wrote a thing thinking another. I understood your first reply. My question, in your example, is how to place the new added multibuttons to the bottom of list instead to the top. I mean if it’s possible something like: addComponent(n-2), where n is the number of added components to the container (plus the one we are going to add), n-1 is the Button always in the bottom, n-2 is the place to add a new multibutton. I guess that it’s not possible in this way.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fbottom-align.html)


### **Shai Almog** — November 29, 2018 at 11:45 am ([permalink](https://www.codenameone.com/blog/bottom-align.html#comment-24080))

> Shai Almog says:
>
> It’s possible to call it that way. I just wanted to keep the code a bit simpler without a “weird” – offset calculation. I think it would be “cmpCount -1” but it might be “cmpCount -2” I don’t recall.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fbottom-align.html)

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
