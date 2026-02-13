---
title: Pixel Perfect – Material Buttons
slug: pixel-perfect-material-buttons
url: /blog/pixel-perfect-material-buttons/
original_url: https://www.codenameone.com/blog/pixel-perfect-material-buttons.html
aliases:
- /blog/pixel-perfect-material-buttons.html
date: '2017-07-11'
author: Shai Almog
---

![Header Image](/blog/pixel-perfect-material-buttons/pixel-perfect.jpg)

I’ve mentioned before that our biggest priority in 3.8 is refining the UI design of Codename One applications. This is a difficult task as it is so vague. There are so many small things we can do, when they are viewed in unison they seem approachable but as we start going thru the tasks priorities muddle. That’s why we need feedback from you guys on what bothers you about the UI and its refinement.

Be specific, use screenshots, samples and references. Otherwise we can spend ages fixing things that don’t matter to any user out there or things that only matter to one user…​

### Pixel Perfect

To get this process moving I’d like to start a segment which ideally will run on a weekly basis. In this segment I’ll focus on refining one aspect of the UI completely. Besides the value this delivers to the UI itself as we’ll integrate those nuances into the code I think you would be able to gain insight into the way UI’s should be designed for the mobile platforms.

I’ll focus on iOS flat design and Android material design as those are more understood and approachable.

### Buttons

Today I’ll cover a relatively simple component: Button.

As you will see there is a lot of nuance there…​

#### As they are Today

The screenshots below are from the simulator but represent relatively closely the way buttons look on Android and iOS with Codename One applications.
    
    
    Form f = new Form("Pixel Perfect", BoxLayout.y());
    Button b = new Button("Button");
    f.addComponent(b);
    f.show();

![iOS Button Today](/blog/pixel-perfect-material-buttons/pixel-perfect-button-ios-today.png)

Figure 1. iOS Button Today

![Android Button Today](/blog/pixel-perfect-material-buttons/pixel-perfect-button-android-today.png)

Figure 2. Android Button Today

#### As they Should Be

Material design [specifies](https://material.io/guidelines/components/buttons.html) 3 types of buttons, one of them is a floating action button which we’ll discuss later but the other ones are:

![Material design raised button](/blog/pixel-perfect-material-buttons/pixel-perfect-button-android-material-raised.png)

Figure 3. Material design raised button

![Material design flat button](/blog/pixel-perfect-material-buttons/pixel-perfect-button-android-material-flat.png)

Figure 4. Material design flat button

iOS doesn’t have the plain button as much opting more for grouping but when it does it looks like this:

![iOS Button](/blog/pixel-perfect-material-buttons/pixel-perfect-button-ios-flat.png)

Figure 5. iOS Button

There is also a round button which is used in grouping scenarios mostly. I don’t think it’s applicable in this scenario but I’m mentioning it here for completeness:

![Grouped iOS Button](/blog/pixel-perfect-material-buttons/pixel-perfect-button-ios-group.png)

Figure 6. Grouped iOS Button

So both iOS and Android use a “flat” button by default and we should align based on that. We also need to provide a way to show the Android raised button but that’s a special case that doesn’t exist in iOS.

### Action Items

The theme for Android and iOS is current implemented within the resource files here: <https://github.com/codenameone/CodenameOne/tree/master/Themes>

Android uses the holo theme whereas iOS uses the iOS 7 theme. We need to update these files with improved fonts and new designs. This might be a problem for developers who relied on the older theme behavior but these changes should be done to keep the platform moving forward.

So here are the things we need to change:

  * Text on buttons in Android should be capitalized but not on iOS

  * Fonts should be switched to millimeter native fonts instead of the current system fonts

  * Background color on iOS & Android is out of date. By default on Android it should be `fafafa` and on iOS it should be `efeff4`

  * Android defaults to opaque components which is a bit inconsistent. They need to be transparent

  * Android button needs to be flat and match the native look

  * Pressed & disabled states should match native design

  * We should have a ripple effect for Android buttons

  * We should add an Android specific raised button UIID

A big advantage of switching to flat mode is in removing the image border images, this makes color customization much easier moving forward.

#### Caps

To solve this I added a new feature to `Button` called `setCapsText(boolean)` which has the corresponding `isCapsText`, `isCapsTextDefault` & `setCapsTextDefault(boolean)`. This is pretty core to Codename One so to prevent this from impacting everything unless you explicitly invoke `setCapsText(boolean)` the default value of `true` will only apply when the UIID is `Button` or for the builtin `Dialog` buttons.

I’ve also added the theme constant `capsButtonTextBool` to control this from the theme itself.

I’ve made this change by overriding `setText(String)` and the constructor both of which aren’t performance critical. The alternative of overriding `getText()` or the paint logic might have been better but it would have an impact on performance.

#### Colors, Opacity & Fonts

I’ve made a lot of changes to the fonts, transparency & colors in the Android and iOS themes. This means updating all the skin files and it might break some of your designs if you relied on some native theme behaviors.  
Since we plan to do a lot of changes like this there might be a lot of those disruptive changes but they are for a good cause!

This is what we have after the changes:

![Android Button after the change](/blog/pixel-perfect-material-buttons/pixel-perfect-button-android-post-change.png)

Figure 7. Android Button after the change

![iOS Button after the change](/blog/pixel-perfect-material-buttons/pixel-perfect-button-ios-post-change.png)

Figure 8. iOS Button after the change

Notice that these are screenshots from the simulator and the fonts will look a bit different on the devices.

### Coming Up

We have several big and small things we need to refine and a couple of tasks mentioned above:

  * Raised button UIID

  * Ripple effect

  * Round corners on Android raised button and pressed color

I hope I’ll be able to address at least some of these next week and this is where you come in…​

It’s not enough to complain about something missing or not functioning. We need issues but we also need “actionable” issues. Issues that we can look at and instantly see what needs fixing, e.g. if a button has the wrong default color, font or padding you can just provide a screenshot of a native app next to a screenshot from a simple Codename One application as part of the issue. This helps us make such fixes quickly.

The [issue tracker](http://github.com/codenameone/CodenameOne/issues/) is the best place for these things. We appreciate comments here and please do comment, but these things get lost. Once an issue is assigned to a milestone it might get postponed but it won’t be forgotten. So if you think Codename One can use a facelift, please help us pull that off!
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Ross Taylor** — July 12, 2017 at 5:30 pm ([permalink](https://www.codenameone.com/blog/pixel-perfect-material-buttons.html#comment-23694))

> Are you saying the priorities are not clear where CN1 should focus on when it comes to aligning CN1 UI with the UI guidelines of the respective OS? Its going to be tough because ideally I was hoping to rely on CN1 to have most of the guideline knowledge build-in and occasionally look at guidelines to verify things like spacings and so on.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fpixel-perfect-material-buttons.html)


### **Shai Almog** — July 13, 2017 at 4:02 am ([permalink](https://www.codenameone.com/blog/pixel-perfect-material-buttons.html#comment-23519))

> Native API’s don’t keep up with the guidelines at all times so that’s a bit more tricky.
>
> By default your app won’t look very good if you use native SDK’s or Codename One. You need to pay attention to the design regardless of the platform.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fpixel-perfect-material-buttons.html)


### **Ross Taylor** — July 14, 2017 at 5:46 pm ([permalink](https://www.codenameone.com/blog/pixel-perfect-material-buttons.html#comment-23595))

> So what you mean is one must be creating our own theme that conforms to design guidelines instead of relying on the default theme?
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fpixel-perfect-material-buttons.html)


### **Shai Almog** — July 15, 2017 at 8:08 am ([permalink](https://www.codenameone.com/blog/pixel-perfect-material-buttons.html#comment-23719))

> What I’m saying is that design guidelines wouldn’t exist if there was a technical possibility to make them seamless. We currently bring you to a point of portability but we can bring you closer in terms of look (which is what this article is about).
>
> You already have your own theme, I’m saying you should pay attention to design and not just expect it to “look good” without any conscious effort on your part.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fpixel-perfect-material-buttons.html)

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
