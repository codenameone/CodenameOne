---
title: Millimeter Underline
slug: millimeter-underline
url: /blog/millimeter-underline/
original_url: https://www.codenameone.com/blog/millimeter-underline.html
aliases:
- /blog/millimeter-underline.html
date: '2017-08-21'
author: Shai Almog
---

![Header Image](/blog/millimeter-underline/uidesign.jpg)

I’ve been working on the new pixel perfect post which I would like to focus around the text components, in order to do that I needed an underline border type. Historically this is something we shrugged off and pointed people at the 9-piece border. That was the wrong answer and it partially related to the way rendering used to work in Codename One and partially related to our reluctance in changing the resource file format.

Both of these are no longer an issue or a priority so in the upcoming update I added new methods to the `Border` class:
    
    
    public static Border createLineBorder(float thickness, int color)
    public static Border createLineBorder(float thickness)
    public static Border createUnderlineBorder(int thickness, int color)
    public static Border createUnderlineBorder(float thickness, int color)
    public static Border createUnderlineBorder(int thickness)
    public static Border createUnderlineBorder(float thickness)

Notice that we already had a `createLineBorder(int thickness, int color)` method which worked with pixels. The floating point version of the method works with millimeters. For consistency I made the new underline border mode work in the same way and so the `int` version of the method works with pixels and the `float` version of the method works with millimeters.

Also notice the versions of the methods that don’t take a color as an argument use the styles foreground color for the painting.

The upcoming version of the Codename One designer will now include the additional `underline` option in the combo box of borders and a new millimeter checkbox next to the thickness entry in the first tab.

This can have far reaching effect as a lot of our components currently feature default 9-piece borders to workaround the lack of this border type.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Francesco Galgani** — August 24, 2017 at 3:43 pm ([permalink](https://www.codenameone.com/blog/millimeter-underline.html#comment-23703))

> Francesco Galgani says:
>
> Thank you. Can you add some screenshots to show the new features? 🙂
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fmillimeter-underline.html)


### **Shai Almog** — August 25, 2017 at 3:24 am ([permalink](https://www.codenameone.com/blog/millimeter-underline.html#comment-23711))

> Shai Almog says:
>
> I thought about that when I was making the post but frankly there isn’t much to see. It’s just a line at the bottom like you have in the iOS theme for multi button and other components.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fmillimeter-underline.html)

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
