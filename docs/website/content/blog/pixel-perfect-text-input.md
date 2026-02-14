---
title: Pixel Perfect – Text Input
slug: pixel-perfect-text-input
url: /blog/pixel-perfect-text-input/
original_url: https://www.codenameone.com/blog/pixel-perfect-text-input.html
aliases:
- /blog/pixel-perfect-text-input.html
date: '2017-10-24'
author: Shai Almog
---

![Header Image](/blog/pixel-perfect-text-input/pixel-perfect.jpg)

I started working on this post back in August and so much happened in between I just had to scrap the whole thing and start over again…​ I’ve discussed buttons before and now it’s time for `TextField` & `TextArea`. But before we go into that lets take a moment to talk about the problems that arose from the [last installment of pixel perfect](/blog/pixel-perfect-material-buttons-part-2.html).

I made a lot of changes to Codename One but there was one particularly painful change: caps text. This proved to be a far more controversial change than I had intended and I had an [interesting debate with Nick](http://disq.us/p/1l8b041). I’d appreciate participation in the thread there or here. The gist of the debate is how can we change functionality people rely on without disrupting working code. I honestly didn’t think the caps text change was a big change or would cause the problems it did. However, having said that it’s really hard to rollout a feature without disrupting developers occasionally.

IMO one of the best ways around it is if more people [used the source](/blog/how-to-use-the-codename-one-sources.html) when working with Codename One:

  * You would see problematic things before they make it to the build servers

  * You would be able to patch/fix issues at least locally

  * Debugging into the code would be easier for you

So please use the source and help us all make a better product!

### On Top Sidemenu Update

One of the things that did get some scrutiny over the past few weeks is the on-top side menu that caused [quite a few regressions](/blog/dont-touch-that-code.html). We now have an up to date version of this code which should work nicely with the on-top side menu so if all goes according to plan we’ll flip the switch over this weekend.

As a reminder, we will transition from the original `Toolbar` sidemenu implementation to a completely new approach that solves a lot of the issues with the old side menu and looks closer to modern native side menus by residing on top of the UI. You can toggle this side menu on/off by invoking `Toolbar.setOnTopSideMenu(true);` in your `init(Object)` method (you should use `false` to turn it off).

### Text Components on Android

The material design guideline includes several modes and states for text fields but generally this image represents the desired native look reasonably well:

![Native Android text fields](/blog/pixel-perfect-text-input/pixel-perfect-text-field-android-native.png)

Figure 1. Native Android text fields

One thing that isn’t clear from the screenshot is the floating label which is now becoming standard in Android. I think we’ll probably need a better abstraction for that one…​ Right now a similar UI in Codename One looks like this:

![The before shot](/blog/pixel-perfect-text-input/pixel-perfect-text-field-android-codenameone-before.png)

Figure 2. The before shot

The current code for doing this looks like this:
    
    
    TableLayout tl = new TableLayout(3, 2);
    Form f = new Form("Pixel Perfect", tl);
    
    TextField title = new TextField("", "Title");
    TextField price = new TextField("", "Price");
    TextField location = new TextField("", "Location");
    TextArea description = new TextArea("");
    description.setHint("Description");
    
    f.add(tl.createConstraint().horizontalSpan(2), new FloatingHint(title));
    f.add(tl.createConstraint().widthPercentage(30), new FloatingHint(price));
    f.add(tl.createConstraint().widthPercentage(70), new FloatingHint(location));
    f.add(tl.createConstraint().horizontalSpan(2), new FloatingHint(description));
    
    f.show();

### What’s Wrong

There are several problems when we look at the screenshots side by side:

  * No margin

  * Text field border is in Android 4.x style instead of the material design line style

  * Font color is wrong for the title label and the color of the bottom line (both should match)

  * The font is wrong both in the title label and the text

  * Floating hint makes sense in Android but we might need to rethink it for iOS

  * Price has a special case $ symbol prefix that has the color of the title label

Besides those things that are obvious from the screenshots here are a few other things that might be a problem:

  * Focus color behavior is incorrect – in native code the color of the title text and underline change on focus but not the color of the text itself

  * Error message labels should appear below in red

  * Hint text looks different from native hint text

As before I’ll try going through this list and I’ll try fixing these so the look will be more native.

### Margin & Border

These are probably the easiest fixes to do with the exception of the colors. After making a few changes to the margin and the border and applying that to the theme we can now see this:

![After applying margin and underline border change](/blog/pixel-perfect-text-input/pixel-perfect-text-field-android-codenameone-margin-underline.png)

Figure 3. After applying margin and underline border change

These were both pretty easy to fix in the designer tool, I just edited the Android native theme and changed the styling for the border and margin. I also needed a style for `FloatingHint` which matches the label on top. It needs to match the margin of the text field now.

### Colors

One of the problem in the simulator screenshot process is that it hides the currently editing text, in the picture below it isn’t clear that the text we are editing is black while the line is blue and the label on top is also blue.

![The editing line and label have the same color](/blog/pixel-perfect-text-input/pixel-perfect-text-field-android-codenameone-colors.png)

Figure 4. The editing line and label have the same color

Right now we don’t have any way to define a standard color palette in the theme. So I can’t pick a color constant and need to pick a specific color for the border to work with. This is something we should probably address in the theme toolchain but it’s not a trivial fix and requires some thought. In general I’d like to define a color scheme and automatically generate the native Android `colors.xml` etc. to create a more uniform UX.

In terms of focus I had to do a bit more work. I added a new method to `Component` `shouldRenderSelection()` which can be overriden to indicate whether selection is shown. Normally in touch devices the selected state only appears when touching the screen so this is essential for this case.

### Fonts

The fonts are also a pretty easy change. I used the native regular at first but then comparing it to the native UI it seems that the light version of the font is closer to the design.

![Better sized roboto font](/blog/pixel-perfect-text-input/pixel-perfect-text-field-android-codenameone-font.png)

Figure 5. Better sized roboto font

I also styled the `FloatingHint` text with a smaller version of the same font and adapted the `TextHint` to use the same font with a gray color.

### What’s Next

All of these changes should be up in the next update and might disrupt existing code. As I mentioned before this is expected as we move the product forward. In the next installment I’ll try to address the remaining issues and hopefully fine tune some of the behaviors:

  * The floating hint API is bad – we will need a new API to represent text field and label together both for iOS and Android

  * We will need a solution for the special case characters or icons that remain when typing and aren’t there as a hint. There are several tricks we can do but I need to give this some thought especially how they will work with something like a floating hint

  * We need to handle error messages in a standard way

I hope to address all of those quicker than I got around to doing this installment…​
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **3lix** — February 11, 2018 at 2:19 am ([permalink](https://www.codenameone.com/blog/pixel-perfect-text-input.html#comment-24137))

> Is there a way I can get the same code above to produce the Android style on IOS using the TextComponent?
>



### **Shai Almog** — February 11, 2018 at 7:32 am ([permalink](https://www.codenameone.com/blog/pixel-perfect-text-input.html#comment-23828))

> Are you referring to this API or the improved one from part 2: [https://www.codenameone.com…](<https://www.codenameone.com/blog/pixel-perfect-text-input-part-2.html>)
>
> Generally you can get this to work by styling the text and defining the theme constants in your theme to make it look like the Android version. After working with the Uber app one of my conclusions is that we need a Material theme that’s cross platform. I’m not sure when I’ll get around to do that but I think it’s generally a good idea to offer that as one of the options. I have some thoughts on this that I still need to define as there are some edge cases.
>



### **3lix** — February 11, 2018 at 5:22 pm ([permalink](https://www.codenameone.com/blog/pixel-perfect-text-input.html#comment-23756))

> I meant the API from part 2. I just noticed the newly added theme constants! Thanks  
> +1 for cross platform Material theme.
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
