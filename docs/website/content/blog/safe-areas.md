---
title: Safe Areas
slug: safe-areas
url: /blog/safe-areas/
original_url: https://www.codenameone.com/blog/safe-areas.html
aliases:
- /blog/safe-areas.html
date: '2020-02-07'
author: Steve Hannah
---

![Header Image](/blog/safe-areas/safe-areas.jpg)

Apple is so very clever with its designs. With the iPhone X, they found way to squeeze in a larger screen without increasing the phone dimensions. The screen nearly covers the entire front face of the phone. The “notch”, as it has come to be known, may have been a practical concession (they needed to put the camera and speaker somewhere, after all) or an intentional design choice – or maybe a little of both. However the notch was conceived, it is here to stay, and we developers need to “work around” it.

And it’s not only the notch that we have to work around. The iPhone X screen has round corners too. Not the old, boring, right angled corners that we’re used to. And the coup de gras is the task bar that now appears along the bottom of the screen, that allows the user to “swipe up” to minimize the app. This has made it more difficult to use bottom navigation in our apps because we have to make sure that the user doesn’t accidentally close the app when they try to click on one of our app’s buttons at the bottom of the screen.

We have recently added an API to help you work around these landmines. We’ve added the method `Form.getSafeArea()`, that will load the “safe” bounds of the current form. If you are drawing components on the screen and want to make sure that they aren’t clipped by “the notch”, the corners, or the task bar, then you can use this API to create a safe play-pen for yourself.

We’ve also added `Container.setSafeArea(boolean)` which you can use to ensure that the container renders its children inside the “Safe” area. The `Tabs` component has been updated to that its tab buttons are rendered inside a “safe area”. When run on the iPhone X, you’ll notice a little bit of extra padding under your tab buttons that appear at the bottom of the screen.

## Some Examples
    
    
    // Tab 1:  A safe area.  Will render all children inside safe area.
    Container tab1Contents = new Container();
    tab1Contents.setSafeArea(true);
    // ... omitted code - building the tab 1 contents.
    
    // Tab 2: A non-safe area.  Children may clip "unsafe" areas.
    Container tab2Contents = new Container();
    // ... omitted code - building tab 2 contents.
    
    tabs.addTab("Safe Tab", tab1Contents);
    tabs.addTab("Unsafe Tab", tab2Contents);
    
    form.add(BorderLayout.CENTER, tabs);

![Tab 1 is a safe area](/blog/safe-areas/Image-290120-070124.063.png)

Figure 1. Tab 1 is a safe area, so its children will not get clipped by the notch

![Tab 2. Unsafe area](/blog/safe-areas/Image-290120-070239.254.png)

Figure 2. Tab 2 is not a safe area, so some of its children may be clipped by the notch

## Further Reading

Check out the [SafeAreasSample](https://github.com/codenameone/CodenameOne/blob/master/Samples/samples/SafeAreasSample/SafeAreasSample.java) in the [Samples project](https://github.com/codenameone/CodenameOne/tree/master/Samples) for the full working example from which the sample code above was taken.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Francesco Galgani** — February 10, 2020 at 11:40 am ([permalink](https://www.codenameone.com/blog/safe-areas.html#comment-21379))

> Thank you Steve, this solves a lot of issues in a simple way! 😀  
> Is it fine if it’s applied to the contentPane?
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fsafe-areas.html)


### **Steve Hannah** — February 10, 2020 at 1:25 pm ([permalink](https://www.codenameone.com/blog/safe-areas.html#comment-21381))

> You can apply it to the content pane as long as you don’t have any components that are supposed to bleed to the edge. E.g. If you have tabs, it would look weird because the tab buttons wouldn’t be at the very bottom of the screen.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fsafe-areas.html)


### **Javier Anton** — March 10, 2021 at 1:33 pm ([permalink](https://www.codenameone.com/blog/safe-areas.html#comment-24411))

> Javier Anton says:
>
> A bit late to the conversation, but thought I’d share an alternative to this. To have a bit more control over the “padding” one way is to manually detect the notches with the below code from SO
>
> – (BOOL)hasTopNotch{  
> if (@available(iOS 13.0, *)) {  
> return [self keyWindow].safeAreaInsets.top > 20.0;  
> }else{  
> return [[[UIApplication sharedApplication] delegate] window].safeAreaInsets.top > 20.0;  
> }  
> return NO;  
> }
>
> – (UIWindow*)keyWindow {  
> UIWindow *foundWindow = nil;  
> NSArray *windows = [[UIApplication sharedApplication]windows];  
> for (UIWindow *window in windows) {  
> if (window.isKeyWindow) {  
> foundWindow = window;  
> break;  
> }  
> }  
> return foundWindow;  
> }
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fsafe-areas.html)

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
