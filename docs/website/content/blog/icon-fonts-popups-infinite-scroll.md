---
title: Icon Fonts, Popups and Infinite Scroll
slug: icon-fonts-popups-infinite-scroll
url: /blog/icon-fonts-popups-infinite-scroll/
original_url: https://www.codenameone.com/blog/icon-fonts-popups-infinite-scroll.html
aliases:
- /blog/icon-fonts-popups-infinite-scroll.html
date: '2019-09-06'
author: Shai Almog
---

![Header Image](/blog/icon-fonts-popups-infinite-scroll/new-features-1.jpg)

As I mentioned in the last post there are a lot of new features we need to go over and it will take time to cover everything. In fact this is still a partial list and there’s a lot more on the way…​

### Easier Icon Font Integration

First off [Thomas](https://github.com/ThomasH99) [submitted a PR](https://github.com/codenameone/CodenameOne/issues/2856) for a few new font icon methods:

Specifically in `Label` he added:
    
    
    public void setFontIcon(Font font, char c);
    public void setFontIcon(Font font, char c, float size);

And in `FontImage` he added:
    
    
    public static void setFontIcon(Label l, Font font, char icon, float size);
    public static void setFontIcon(Label l, Font font, char icon);
    public static void setFontIcon(Command c, Font font, char icon, String uiid, float size);

These new methods work in a similar way to the set material icon methods. However, they can work with an arbitrary icon font. That was possible to do before this change but this change makes that easier and makes the code more fluid. It also works similarly to the material API where icons are implicitly applied to all the states of the buttons. That means the pressed/selected/disabled styles will apply to the icon as well as the text.

### Additional Icon Fonts

[Francesco Galgani](https://github.com/jsfan3) came up with a solution for [#2421](https://github.com/codenameone/CodenameOne/issues/2421) which finally allowed us to update the material font and font constants.

This has been a long time RFE which came up every few months. When we launched the material font support. Unfortunately, Google stopped updating that font a few years ago and everyone who relied on it was stuck. Thankfully Francesco found an up to date version of the font as well as the diffs between the old/new font.

As a result we now have a lot more constants you can use when setting an icon in a default Codename One app. You can see the full list [here](https://github.com/codenameone/CodenameOne/commit/a826eac206062b52391480396884e3b4e5fa5f86).

### Lightweight Popup Dialog

When Codename One was young we needed a popup arrow implementation but our low level graphics API was pretty basic. As a workaround we created a version of the 9-piece image border that supported pointing arrows at a component.

Since we added a proper graphics pipeline we wanted to rewrite that logic to use proper graphics. This allows for better customization of the border (color, shape etc.) and it looks better on newer displays. It also works on all OSs. Right now only the iOS theme has the old image border approach.

To solve this we [added new support for arrows](https://github.com/codenameone/CodenameOne/commit/921395f6613e7a2bb883f41d4217797f7d790fa9) into the `RoundRectBorder` API. If you style a popup dialog with a round rect border this should “just work” and use this API. By default popup dialogs are styled that way with the exception of iOS where they still have the image border styling for compatibility (although we might change that).

This works by setting the track component property on border. When that’s done the border implicitly points to the right location.

### Continue the Infinite

`InfiniteContainer` and `InfiniteAdapter` work great for most use cases but they have a bit of an [“undefined” behavior when it comes to failure](https://github.com/codenameone/CodenameOne/issues/2721). E.g. if we have a network error and don’t have anything to fetch as a result.

to solve this we added this method to `InfiniteContainer`:
    
    
    public void continueFetching();

And these to `InfiniteScrollAdapter`:
    
    
    public void continueFetching();
    public static void continueFetching(Container cnt);

So when you get an error you can just add this error button and return null:
    
    
    SpanButton errorButton = new SpanButton("Networking Error, Press to retry");
    errorButton.addActionListener(e -> {
         errorButton.remove();
         continueFetching();
    });
    add(errorButton);
    return null;

The error button will let the user retry the operation and continue fetching the content even though it was previously “stopped”.

### More Coming

As I mentioned at the top of this post, this is still the tip of the iceberg of new features we’ve worked on over the summer. More is coming!
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Francesco Galgani** — September 14, 2019 at 8:23 am ([permalink](https://www.codenameone.com/blog/icon-fonts-popups-infinite-scroll.html#comment-24090))

> Francesco Galgani says:
>
> I’ve just tested that a Popup Dialog works fine also with an Android skin, thank you! About the Popup Dialog, isn’t the developer guide section “Styling The Arrow Of The Popup Dialog” more valid? Should this part of the developer guide be changed? Link: [https://www.codenameone.com…](<https://www.codenameone.com/manual/components.html#_styling_the_arrow_of_the_popup_dialog>)
>



### **Shai Almog** — September 15, 2019 at 3:44 am ([permalink](https://www.codenameone.com/blog/icon-fonts-popups-infinite-scroll.html#comment-23825))

> Shai Almog says:
>
> Thanks, I updated that.
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
