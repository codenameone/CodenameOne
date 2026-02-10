---
title: New Sheet Positioning
slug: sheet-positions
url: /blog/sheet-positions/
original_url: https://www.codenameone.com/blog/sheet-positions.html
aliases:
- /blog/sheet-positions.html
date: '2020-02-27'
author: Steve Hannah
---

![Header Image](https://www.codenameone.com/img/blog/new-sheet-positioning.jpgnew)

Not so long ago, we [released](https://www.codenameone.com/blog/sheets-samples.html) a [Sheet component](https://www.codenameone.com/javadoc/com/codename1/ui/Sheet.html) that acts like a non-modal dialog box that slides up from the bottom. It occupies only the amount of space required to house its contents, and it provides built-in navigation controls to go “back” to the previous sheet. By default Sheets are displayed along the bottom of the screen, but we have recently added an update that allows you to position it along the north, east, west, south, or center of the screen, as shown below:

![Sheet component positions](/blog/sheet-positions/sheet-positions.png)

In addition we have added the ability to use a different position for tablets and desktop, than for phones. On desktop, it is more common for dialogs to pop up in the center of the screen, whereas on mobile, it is quite common to have a dialog (or sheet) pop up from the bottom.

When positioning a sheet on the east or west, it is quite easy to create your own ad-hoc hamburger menu. This may be easier, in some cases, than using the ToolBar class, as it gives you more control over the result.

For a full working example, see the [updated Sheet sample here](https://github.com/codenameone/CodenameOne/blob/master/Samples/samples/SheetSample/SheetSample.java).

Below is a 35 second screen cast of that demo:
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Francesco Galgani** — February 29, 2020 at 11:27 am ([permalink](https://www.codenameone.com/blog/sheet-positions.html#comment-21414))

> Thank you, this is exactly what I need! 🙂  
> About the Sheet in the bottom position, can it be placed at a given distance from the bottom and not exactly at the bottom like in the video? In other words, is it possible to slide up the Sheet from bottom to up, until it reaches a given distance from the bottom?
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fsheet-positions.html)


### **Javier Anton** — February 29, 2020 at 9:32 pm ([permalink](https://www.codenameone.com/blog/sheet-positions.html#comment-21415))

> [Javier Anton](https://lh3.googleusercontent.com/a-/AAuE7mDRjHkEvAZNXquh9p7Oo00ey1yOwNzZ0SrFwD0IVA) says:
>
> Will definitely use this thx
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fsheet-positions.html)


### **Shai Almog** — March 1, 2020 at 2:06 am ([permalink](https://www.codenameone.com/blog/sheet-positions.html#comment-21413))

> Shai Almog says:
>
> Maybe a creative use of margin can help with that?  
> Haven’t tried it but I think it should work.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fsheet-positions.html)

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
