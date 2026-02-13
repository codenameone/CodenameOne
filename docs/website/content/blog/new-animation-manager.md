---
title: New Animation Manager
slug: new-animation-manager
url: /blog/new-animation-manager/
original_url: https://www.codenameone.com/blog/new-animation-manager.html
aliases:
- /blog/new-animation-manager.html
date: '2015-12-15'
author: Shai Almog
---

![Header Image](/blog/new-animation-manager/title-area-animation.gif)

We committed a major somewhat revolutionary change to how layout animations and the basic component tree  
work in Codename One. This is important to understand since it changes the very nature of how we expect  
components to behave in Codename One. This also allows us to create some pretty spectacular effects  
such as the ones in the title above. 

There are several types of animations in Codename One: 

  * Form/Dialog transitions
  * Replace transitions
  * Layout animations
  * Low level animations

Transitions and low level animations are both special cases that aren’t affected by this change. However, replace transitions  
and layout animations have been effectively rewritten to use the new `AnimationManger` class.  
The `AnimationManger` class allows executing a component animation synchronously or  
asynchronously and starting with current builds all replace/layout animations (e.g. `Container.replace`,  
`Container.animateLayout` etc.) delegate their work to this class. 

#### Broken Compatibility

This is important since some compatibility is broken as a result of this change!  
One of the core problems with the old animation framework was that you can’t have to animations going on  
at once e.g. if you did something like this: 
    
    
    myContainer.add(new Button("Button")).animateLayout(300);

This code could fail if you invoke it twice concurrently since there would be two animations mutating the  
container at the same time and they would not work in sequence!  
A lot of developers assumed incorrectly that this code would fix this problem: 
    
    
    myContainer.add(new Button("Button")).animateLayoutAndWait(300);

Since the AndWait methods block the EDT they assume no other addition can happen during this time  
but the AndWait method doesn’t block event dispatch so if a user triggers an event that triggers this code  
it could still fail. The workaround we often suggested was something like this: 
    
    
    if(!locked) {
        locked = true;
        myContainer.add(new Button("Button")).animateLayoutAndWait(300);
        locked = false;
    } else {
        // postpone animation/addition
    }
    

While this works it makes generic code difficult and has the side effect of only being aware of a specific change.  
So as part of this change we **changed the very way add/remove component work in Codename One!**  
If you add a component and there is no animation it will work like it always did. However, if you add a component and  
there is an animation in progress the addition will be delayed to the end of the animation!  
That might not produce the result you want but it will leave you with a valid UI and unlike some of the cases  
above it is unlikely to crash your application, which is probably the most important thing. 

#### Enhancements

We didn’t just do this to fix bugs though, we are looking at integrating some of the cool special effects you can  
see here: 

This animation is a combination of a new ability to step thru animation stages when scrolling with a new  
“style animation” that accepts a destination UIID for a Component and morphs from the source style to the  
destination UIID. This specific demo includes this code: 
    
    
    Form hi = new Form("Shai's Social App", new BoxLayout(BoxLayout.Y_AXIS));
    for(int iter = 0 ; iter < 100 ; iter++) {
        hi.add(new Label("Social Data Goes here..."));
    }
    Toolbar tb = new Toolbar();
    hi.setToolbar(tb);
    ComponentAnimation cna = tb.createStyleAnimation("TitleAreaClean", 200);
    ComponentAnimation title = tb.getTitleComponent().createStyleAnimation("TitleClean", 200);
    hi.getAnimationManager().onTitleScrollAnimation(cna, title);
    hi.show();

The styling for both the source title with my picture and the destination clean styles are defined in the theme but  
you can also define the source style in code.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Diamond** — February 19, 2016 at 9:12 am ([permalink](https://www.codenameone.com/blog/new-animation-manager.html#comment-22540))

> Diamond says:
>
> Hi Shai,
>
> Where can I get the full source for this new animation manager demo please? Where is the image placed, at the contentPane or TitleArea using Toolbar?
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fnew-animation-manager.html)


### **Shai Almog** — February 19, 2016 at 12:11 pm ([permalink](https://www.codenameone.com/blog/new-animation-manager.html#comment-22615))

> Shai Almog says:
>
> Hi Diamond,  
> we haven’t made it into a full fledged demo but I did add a shorter standalone section on this into the Toolbar javadocs and the developer guide: [https://www.codenameone.com…](<https://www.codenameone.com/manual/components.html#_title_animations>)
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fnew-animation-manager.html)


### **Mac Flanegan** — March 2, 2017 at 4:53 pm ([permalink](https://www.codenameone.com/blog/new-animation-manager.html#comment-23008))

> Mac Flanegan says:
>
> Hi Shai,
>
> I am trying to implement this feature.
>
> But the overflowbutton (Rigth command) and title are vertically centered.
>
> Is it possible to align the overflowbutton and title at the top?
>
> Edit: I tryed to use constant menuButtonTopBool but no success.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fnew-animation-manager.html)


### **Mac Flanegan** — March 2, 2017 at 5:12 pm ([permalink](https://www.codenameone.com/blog/new-animation-manager.html#comment-23165))

> Mac Flanegan says:
>
> Please, ignore…
>
> I have achieved the desired result using the ToolBar directly and not titleComponent …
>
> from  
> Style stitle = home.getToolbar().getTitleComponent().getUnselectedStyle();  
> to  
> Style stitle = home.getToolbar().getUnselectedStyle();
>
> stitle.setPaddingBottom(20);
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fnew-animation-manager.html)


### **Shai Almog** — March 3, 2017 at 8:03 am ([permalink](https://www.codenameone.com/blog/new-animation-manager.html#comment-23361))

> Shai Almog says:
>
> One of the approaches I took in one of my demos was to hide the side menu icon using the theme constant and then add a Command to the toolbar where I wanted it to be. The command just called the open side menu method of the toolbar.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fnew-animation-manager.html)


### **Mac Flanegan** — March 3, 2017 at 7:06 pm ([permalink](https://www.codenameone.com/blog/new-animation-manager.html#comment-23279))

> Mac Flanegan says:
>
> Hi Shai, thanks for reply.
>
> Any hints on how to gradually hide a component/container whith this method?
>
> I’m trying to create an interface similar to native android applications, like the YouTube App where the title is hidden with Scroll up.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fnew-animation-manager.html)


### **Shai Almog** — March 4, 2017 at 10:13 am ([permalink](https://www.codenameone.com/blog/new-animation-manager.html#comment-23369))

> Shai Almog says:
>
> I think the flickr demo might have something like this that uses the hide toolbar feature
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fnew-animation-manager.html)

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
