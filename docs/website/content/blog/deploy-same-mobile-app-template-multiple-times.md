---
title: Deploy the Same Mobile App/Template Multiple Times
slug: deploy-same-mobile-app-template-multiple-times
url: /blog/deploy-same-mobile-app-template-multiple-times/
original_url: https://www.codenameone.com/blog/deploy-same-mobile-app-template-multiple-times.html
aliases:
- /blog/deploy-same-mobile-app-template-multiple-times.html
date: '2016-09-25'
author: Shai Almog
---

![Header Image](/blog/deploy-same-mobile-app-template-multiple-times/clone-app.jpg)

We often build one app and sell it to multiple customers. After all, most customers ask for similar things with minor  
changes. E.g. if I build a restaurant app and then sell it to one establishment I can then resell it to another with almost  
no change at all…​

Another common use case is the demo or free version of a paid app, you want to reuse as much of the work as possible  
without maintaining two code bases.

This question has come up quite a few times in the forums and most recently again  
[on stackoverflow](http://stackoverflow.com/questions/39505331/managing-demo-full-version-of-my-app-in-codename-one).  
It’s something we need to document better so here is a brief tutorial.

### How does the Appstore “Know”?

The first thing we need to understand is how the appstore identifies your application.

Pretty much all apps use a unique identifier string that is similar to package names, which we map to the main  
application package name.

__ |  Apple doesn’t allow underscores in these names, Java doesn’t allow the minus character `-` so avoid both   
---|---  
  
### Getting Started

Lets assume we created an app for acme corporation under the package `com.acme.apps.supercoolapp`. We  
now want to sell a similar app to  
[Evil Corp.](https://www.rottentomatoes.com/tv/mr-robot/) (please no spoilers for those who didn’t see this yet…​)  
which we will ship as `com.evil.apps.supercoolapp`. To do this I need to follow these steps…​

#### Step 1: Create the Acme app as usual

This is pretty strait forward and I’m assuming you all know how to do that…​

#### Step 2: Create the Evil Corp Package

We can just create a new package in the IDE named `com.evil.apps.supercoolapp` and place a main class there.  
Ideally it should have the same name as the main class we have in `com.acme.apps.supercoolapp`, I’ll assume they  
are both named `Main`.

The cool thing as that one main can derive functionality from another e.g.:
    
    
    package com.evil.apps.supercoolapp;
    public class Main extends com.acme.apps.supercoolapp.Main {
        @Override
        public void init(Object context) {
            super.init(context);
        }
    
        @Override
        public void start() {
            super.start();
        }
    
        @Override
        public void stop() {
            super.stop();
        }
    
        @Override
        public void destroy() {
            super.destroy();
        }
    }

Notice that I can just write additional logic in every one of these methods or even replace them entirely to provide  
unique functionality for Evil Corp.

E.g. I can set the theme to a different theme and some common strings from the resource bundle to different values.

#### Step 3: Run in the Simulator

The simulator is really just a java class that gets the main class/package as an argument. You can change that  
in the project settings under the run section. NetBeans is particularly good with these things as it allows you to define  
project configurations and you can switch dynamically between said configurations with just a right click.

#### Step 4: Build Native App

Just replace all the references to `com.acme.apps.supercoolapp` with `com.evil.apps.supercoolapp` in the  
`codenameone_settings.properties`. You will also need to update the certificates/provisioning information depending  
on the customer. Make sure to save the provisioning/certificate data of each customer separately so you don’t get  
confused…​

You might also want to update the title and other properties within the file. Some developers keep multiple versions  
of the file and the application icon and just replace them dynamically with a script.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Nick Koirala** — October 6, 2016 at 1:28 am ([permalink](https://www.codenameone.com/blog/deploy-same-mobile-app-template-multiple-times.html#comment-22560))

> Nick Koirala says:
>
> Can you elaborate on how to run in the simulator? I set up a new configuration and put the package/class as the arguments in the run section and it still runs the original Main, not the new Main
>



### **Shai Almog** — October 6, 2016 at 5:22 am ([permalink](https://www.codenameone.com/blog/deploy-same-mobile-app-template-multiple-times.html#comment-22948))

> Shai Almog says:
>
> Did you select that configuration as the active one?  
> In which IDE?  
> Is there output in the console?
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
