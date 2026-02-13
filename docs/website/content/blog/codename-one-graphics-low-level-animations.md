---
title: Codename One Graphics – Low Level Animations
slug: codename-one-graphics-low-level-animations
url: /blog/codename-one-graphics-low-level-animations/
original_url: https://www.codenameone.com/blog/codename-one-graphics-low-level-animations.html
aliases:
- /blog/codename-one-graphics-low-level-animations.html
date: '2015-02-10'
author: Steve Hannah
---

![Header Image](/blog/codename-one-graphics-low-level-animations/clock_small.png)

![](/blog/codename-one-graphics-low-level-animations/clock_small.png)

In my previous post, I created a static analog clock component. In this post, I will use Codename One’s animation API to update the clock every continually so that it will keep time correctly. 

#### How Animation Works in Codename One

From the [Developer’s Guide:](http://www.codenameone.com/manual/)

> The Codename One event dispatch thread has a special animation “pulse” allowing an animation to update its state and draw itself.
> 
> Every component in Codename One contains an animate() method that returns a boolean value, you can also implement the Animation interface in an arbitrary component to implement your own animation. In order to receive animation events you need to register yourself within the parent form, it is the responsibility of the parent for to call animate().
> 
> If the animate method returns true then the animation will be painted. It is important to deregister animations when they aren’t needed to conserve battery life. However, if you derive from a component, which has its own animation logic you might damage its animation behavior by deregistering it, so tread gently with the low level API’s.” 

#### Device Support

The Codename One animation API is a core feature of Codename One and it is supported on all platforms.

#### Animating the Clock

In order to animate our clock so that it updates once per second, we only need to do two things:

  1. Implement the animate() method to indicate when the clock needs to be updated/re-drawn.
  2. Register the component with the form so that it will receive animation “pulses”.

The animate() method in my AnalogClock class:
    
    
    Date currentTime = new Date();
    long lastRenderedTime = 0;
     
    @Override
    public boolean animate() {
    if ( System.currentTimeMillis()/1000 != lastRenderedTime/1000){
    currentTime.setTime(System.currentTimeMillis());
    return true;
    }
    return false;
    } 

This method will be called on each “pulse” of the EDT. It checks the last time the clock was rendered and returns true only if the clock hasn’t been rendered in the current “time second” interval. Otherwise it returns false. This ensures that the clock will only be redrawn when the time changes. 

#### Starting and Stopping the Animation

Animations can be started and stopped via the Form.registerAnimated(component) and Form.deregisterAnimated(component) methods. I have chosen to encapsulate these calls in start() and stop() methods in my component as follows: 
    
    
    public void start(){
    this.getComponentForm().registerAnimated(this);
    }
     
    public void stop(){
    this.getComponentForm().deregisterAnimated(this);
    }  

So the code to instantiate the clock, and start the animation would be something like: 
    
    
    AnalogClock clock = new AnalogClock();
    parent.addComponent(clock);
    clock.start(); 

#### The Final Result

You can view the full source to this component [here](https://gist.github.com/shannah/d83d30c851b6f746260f).  
You may want to compare this to the [static version](https://gist.github.com/shannah/7f6abb8f4e16a5203771) from the  
previous post to help identify the parts that are related to animation. 

#### Higher-Level Animations

This tutorial dealt with the low-level animation API since the focus of this series is on low-level graphics. For most common use-cases, however, Codename One provides higher-level APIs that make it much easier to incorporate animations into your UI. Some examples of these APIs include:

  1. Transitions between forms.
  2. Animating components when their layouts are changed.
  3. Animating components when their hierarchy is changed.

See the [Developer’s Guide](http://www.codenameone.com/manual/) for more information about these APIs.

#### More Advanced Animations

This tutorial only scratched the surface of what is possible with the Codename One animation API. In a future installment, we will explore the Motion class and see how it can be used to develop more complex animations and UI effects.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
