---
title: Composite Animations
slug: composite-animations
url: /blog/composite-animations/
original_url: https://www.codenameone.com/blog/composite-animations.html
aliases:
- /blog/composite-animations.html
date: '2016-07-31'
author: Shai Almog
---

![Header Image](/blog/composite-animations/title-area-animation.gif)

When we announced Codename One 3.4 we also announced a major animation overhaul. This was an important milestone that we didn’t fully actualize until this past week…​

Up until now animations in Codename One consisted of a fixed set of animations you could perform, e.g. if I want to move a `Component` from one place to another I could do an `animateLayout`.

When I want to do multiple animations in a sequence I can chain them using the `AndWait` version or thru callbacks etc.

This seems like it should be enough but it sometimes isn’t, e.g. if I have two or more containers and I want them to animate layout together, or if I want an animation to both animate the position and style of a component!

To solve this we introduced the [ComponentAnimation](/javadoc/com/codename1/ui/animations/ComponentAnimation/) class way back in 3.4. We now have new API’s that return `ComponentAnimation` instances that we can subsequently chain together e.g. this code is from the upcoming kitchen sink demo:
    
    
    ComponentAnimation cn2 = createStyleAnimation(newUIID, 1000);
    ComponentAnimation cn1 = createAnimateLayout(1000);
    return ComponentAnimation.compoundAnimation(cn1, cn2);

This code is within a subclass of `Container` so both methods are a part of `Container`. This code will result in an animation that both changes the style of the container as well as its layout!

We’ve added five methods that create this type to container:
    
    
    public ComponentAnimation createAnimateLayout(int duration);
    public ComponentAnimation createReplaceTransition(Component current, Component next, Transition t);
    public ComponentAnimation createAnimateHierarchy(int duration);
    public ComponentAnimation createAnimateHierarchyFade(int duration, int startingOpacity);
    public ComponentAnimation createAnimateLayoutFade(int duration, int startingOpacity);

__ |  `createStyleAnimation` has been around since 3.4 so it isn’t listed, but we fixed a some bugs there…​   
---|---  
  
You will notice that there are no `AndWait` version since you add these animations to the animation queue thru the [AnimationManager](/javadoc/com/codename1/ui/AnimationManager/) API which has both `addAnimation` & `addAnimationAndBlock` allowing both use cases.

The code above uses the `compoundAnimation` static method to merge together animations (you can merge any number of animations not just two). You can also use the `sequentialAnimation` method to merge an animation sequence which might be more convenient than `AndWait` or `AndBlock`.

### Published Developer Guide On Amazon

On an unrelated subject we published the developer guide thru the Kindle book store  
[here](https://www.amazon.com/Codename-One-Developer-Guide-Android-ebook/dp/B01JG0O3CK/). It’s the  
same as the free PDF developer guide but it seems we can’t publish for free on Amazon.

We’d appreciate customer reviews though and if you can help us promote it to drive awareness of Codename One.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
