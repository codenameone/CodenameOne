---
title: SplitPane, Cursors and Push Registration
slug: splitpane-cursors-push-registration
url: /blog/splitpane-cursors-push-registration/
original_url: https://www.codenameone.com/blog/splitpane-cursors-push-registration.html
aliases:
- /blog/splitpane-cursors-push-registration.html
date: '2017-05-23'
author: Shai Almog
---

![Header Image](/blog/splitpane-cursors-push-registration/new-features-6.jpg)

Until this weeks release push notification was registered using `Display.registerPush(Hashtable, boolean)` the thing is that both of these arguments to that method are no longer used or not the best way to implement registration. So we deprecated that method and introduced a new version of the method `Display.registerPush()`.

Since push fallback hasn’t been supported since we migrated to the new API it’s going away isn’t a big deal but up until recently the `Hashtable` argument was used to pass the GCM push ID.

__ |  This is pretty old code from before our migration to Java 5 so it used Hashtable, we since migrated all the way to Java 8   
---|---  
  
To include the Android push id we should now use the build hint `gcm.sender_id` which will work for Chrome JavaScript builds too and is probably the better approach overall.

### SplitPane

Steve recently introduced a cool new split pane component to Codename One which is a long time request from many users:

We didn’t add it in the past because it’s so desktop specific but now that we have the JavaScript port and it makes more sense. To get the video above I changed `SalesDemo.java` in the kitchen sink by changing this:
    
    
    private Container encloseInMaximizableGrid(Component cmp1, Component cmp2) {
        GridLayout gl = new GridLayout(2, 1);
        Container grid = new Container(gl);
        gl.setHideZeroSized(true);
    
        grid.add(encloseInMaximize(grid, cmp1)).
                add(encloseInMaximize(grid, cmp2));
        return grid;
    }

To:
    
    
    private Container encloseInMaximizableGrid(Component cmp1, Component cmp2) {
        return new SplitPane(SplitPane.VERTICAL_SPLIT, cmp1, cmp2, "25%", "50%", "75%");
    }

This is mostly self explanatory but only “mostly”. We have 5 arguments the first 3 make sense:

  * Split orientation

  * Components to split

The last 3 arguments seem weird but they also make sense once you understand them, they are:

  * The minimum position of the split – 1/4 of available space

  * The default position of the split – middle of the screen

  * The maximum position of the split – 3/4 of available space

The units don’t have to be percentages they can be mm (millimeters) or px (pixels).

### Mouse Cursor

You might have noticed the video above was shot in the simulator, one of the tell tale signs is the mouse cursor.

If you paid attention you might have noticed the cursor changed its appearance when I was hovering over specific areas to indicate resizability on the Y axis. This is a new feature in Codename One that’s available in the desktop and JavaScript port. It’s off by default and needs to be enabled on a `Form` by `Form` basis using `Form.setEnableCursors(true);`. If you are writing a custom component that can use cursors such as `SplitPane` you can use:
    
    
    @Override
    protected void initComponent() {
        super.initComponent();
        getComponentForm().setEnableCursors(true);
    }

Once this is enabled you can set the cursor over a specific region using `cmp.setCursor()` which accepts one of the cursor constants defined in `Component`.

### Future Desktop

We have a lot of plans for improving the desktop/web support in Codename One moving forward but these specific features are a part of a specific set of changes going into the new GUI builder.

I don’t think this is the time to share too much but Steve has been working on some GUI builder changes that we are all pretty excited about. We want these changes to be great on launch and not something we constantly tune so they might take some time. I hope they will land in 3.7 but I’d rather we launch them after if we can’t get them to the place where they should be…​
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Nick Koirala** — May 24, 2017 at 10:13 pm ([permalink](https://www.codenameone.com/blog/splitpane-cursors-push-registration.html#comment-23317))

> Nick Koirala says:
>
> I like the simplified push registration with a build hint as the old way wasn’t intuitive enough for me to implement without looking it up every time. Is there a way to set then sender id programatically? One of my projects uses the process described here: [https://www.codenameone.com…](<https://www.codenameone.com/blog/deploy-same-mobile-app-template-multiple-times.html>) and currently the gcm sender id is also set in the overridden code.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fsplitpane-cursors-push-registration.html)


### **Shai Almog** — May 25, 2017 at 4:28 am ([permalink](https://www.codenameone.com/blog/splitpane-cursors-push-registration.html#comment-23384))

> Shai Almog says:
>
> If you do that then having the gcm value within the properties will easily allow you to adapt everywhere. The main motivation for using the build hint is chrome where is needed in a separate file whose loading we can’t control.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fsplitpane-cursors-push-registration.html)


### **Raffic** — July 26, 2017 at 1:38 pm ([permalink](https://www.codenameone.com/blog/splitpane-cursors-push-registration.html#comment-23527))

> Raffic says:
>
> Hello Shai,
>
> It seems the Android SDK does not support firebase Analytics. i am building an android application that uses the firebase analytics.When will the SDK be upgraded to support firebase.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fsplitpane-cursors-push-registration.html)


### **Shai Almog** — July 28, 2017 at 4:28 am ([permalink](https://www.codenameone.com/blog/splitpane-cursors-push-registration.html#comment-23744))

> Shai Almog says:
>
> Hi,  
> firebase is currently not on our priority list. Supporting analytics or any other API from there will require some effort and limit us to Android/iOS only. Unfortunately Google has a proven track record of canceling projects they were previously bullish on so we have serious reservations on support for firebase.
>
> Articles like this don’t exactly raise the confidence level [https://medium.com/@contact…](<https://medium.com/@contact_16315/firebase-costs-increased-by-7-000-81dc0a27271d?r=r>)
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fsplitpane-cursors-push-registration.html)

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
