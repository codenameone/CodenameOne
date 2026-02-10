---
title: Pressed/Selected Icon Font & UTF-8
slug: pressed-selected-icon-font-utf-8
url: /blog/pressed-selected-icon-font-utf-8/
original_url: https://www.codenameone.com/blog/pressed-selected-icon-font-utf-8.html
aliases:
- /blog/pressed-selected-icon-font-utf-8.html
date: '2016-05-21'
author: Shai Almog
---

![Header Image](/blog/pressed-selected-icon-font-utf-8/unselected-pressed-disabled-buttons.png)

One of our support emails drew my attention to a glaring ommission in our icon font support…​  
When we create an icon for a `Button` it’s color matches the unselected color of the button which might not  
look as attractive when the `Buton` is selected or pressed!

The thing is that with an icon font this is trivial to accomplish and requires literally no code changes. So starting with  
the the next update calling  
[FontImage.setMaterialIcon(Label l, char icon)](https://www.codenameone.com/javadoc/com/codename1/ui/FontImage.html#setMaterialIcon-com.codename1.ui.Label-char-)  
or  
[FontImage.setMaterialIcon(Label l, char icon, float size)](https://www.codenameone.com/javadoc/com/codename1/ui/FontImage.html#setMaterialIcon-com.codename1.ui.Label-char-float-)  
with a `Button` will implicitly set the pressed/selected & disabled icons for the button.

E.g.:
    
    
    Form f = new Form("Buttons", BoxLayout.y());
    
    CheckBox pressed = CheckBox.createToggle("Pressed");
    pressed.setUIID("Button");
    FontImage.setMaterialIcon(pressed, FontImage.MATERIAL_THUMB_UP, 5);
    pressed.setSelected(true);
    Button unselected = CheckBox.createToggle("Unselected");
    unselected.setUIID("Button");
    FontImage.setMaterialIcon(unselected, FontImage.MATERIAL_THUMB_UP, 5);
    Button disabled = CheckBox.createToggle("Disabled");
    disabled.setUIID("Button");
    FontImage.setMaterialIcon(disabled, FontImage.MATERIAL_THUMB_UP, 5);
    disabled.setEnabled(false);
    
    f.add(pressed).
        add(unselected).
        add(disabled);
    
    f.show();

### PSA – Always Use UTF-8

Everyone has their “pet peeve” about Java and mine is the encoding support. In a typical Sun way the Java API  
chose the wrong defaults aligning itself with the system instead of aligning itself with whats right.

UTF-8 works for pretty much everyone and using it universally would solve many of the problems we see frequently.  
However, Java defaults to platform native encoding and so minor mistakes like calling the `String.getBytes()` method  
can be devastating and hard to track. To make matters even more annoying the correct method `String.getBytes(encoding)`  
throws a checked exception for no real reason making it far more painful to use than the default…​

I was reminded of this just the other day, it seems our [Properties](https://www.codenameone.com/javadoc/com/codename1/io/Properties.html)  
class used `ISO-8859-1` encoding. This is pretty much the most standard encoding and should be supported everywhere…​

Turns out it doesn’t work in the JavaScript port which uses UTF-8. Since UTF-8 makes more sense we just fixed the  
`Properties` instead of fixing the encoding which is apparently challenging.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Chibuike Mba** — May 25, 2016 at 6:03 pm ([permalink](https://www.codenameone.com/blog/pressed-selected-icon-font-utf-8.html#comment-22799))

> Chibuike Mba says:
>
> Hi Shai, does Toolbar Command support this button icon capability?
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fpressed-selected-icon-font-utf-8.html)


### **Shai Almog** — May 26, 2016 at 4:10 am ([permalink](https://www.codenameone.com/blog/pressed-selected-icon-font-utf-8.html#comment-22850))

> Shai Almog says:
>
> No & sort of…
>
> This is a bit problematic. Commands support the pressed/selected/disabled icon styles but since you don’t normally call the set icon method on them thru the FontImage class this won’t work well…
>
> We’ll add to the next update a version of setMaterialIcon that accepts a command and UIID. And we’ll also try to add a few methods to toolbar that should do effectively this e.g. addMaterialCommandToSideMenu(…)
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fpressed-selected-icon-font-utf-8.html)


### **Chibuike Mba** — May 26, 2016 at 7:10 am ([permalink](https://www.codenameone.com/blog/pressed-selected-icon-font-utf-8.html#comment-22723))

> Chibuike Mba says:
>
> OK. Great. Will be looking forward for that.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fpressed-selected-icon-font-utf-8.html)

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
