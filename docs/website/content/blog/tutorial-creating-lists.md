---
title: Tutorial – Creating Lists
slug: tutorial-creating-lists
url: /blog/tutorial-creating-lists/
original_url: https://www.codenameone.com/blog/tutorial-creating-lists.html
aliases:
- /blog/tutorial-creating-lists.html
date: '2017-10-25'
author: Shai Almog
---

![Header Image](/blog/tutorial-creating-lists/learn-codenameone-1.jpg)

Some of our [how do I videos](/how-do-i.html) are so old it’s embarrassing…​ This is especially true for some core concepts videos as they were some of the first ones we made. The worst part is that these are probably the most important videos we have as they are viewed by developers completely new to Codename One. We had two videos covering creations of lists and both of them used `com.codename1.ui.List` with the old GUI builder!

That’s embarrassing…​ Especially considering [this](/blog/avoiding-lists.html).

So I’ve created a new video that’s more up to date. I discuss lists as a concept but I really describe box layout Y and the `InfiniteContainer` class. It’s a big improvement over the old videos.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Tommy Mogaka** — October 26, 2017 at 2:09 pm ([permalink](/blog/tutorial-creating-lists/#comment-23770))

> Tommy Mogaka says:
>
> Hi Shai, thanks for this. Is it possible on the list created using the above method to have LongPress detection capability? If not, how can this be added to the list items of the list itself?
>



### **Shai Almog** — October 27, 2017 at 4:31 am ([permalink](/blog/tutorial-creating-lists/#comment-23532))

> Shai Almog says:
>
> Hi,  
> sure. You can add any component to the “list” and you can subclass any component to override longPointerPress
>



### **Tommy Mogaka** — October 27, 2017 at 11:48 am ([permalink](/blog/tutorial-creating-lists/#comment-24153))

> Tommy Mogaka says:
>
> Thank Shai, one more question on lists.
>
> After scrolling and pressing an item that navigates you away from a list(e.g. to another form or page), how do you get back to a list to previously displayed list? In my case, the form usually in another class. Is it achievable and what can I do to my code to add this “remember last position” feature onto a list? The use case is when you have a long list and you need to go back and forth without having to keep scrolling from the top to get back to where you had last clicked.
>



### **Shai Almog** — October 28, 2017 at 4:54 am ([permalink](/blog/tutorial-creating-lists/#comment-23739))

> Shai Almog says:
>
> If the same form instance is used when you get back it should still be scrolled to that same position. If not you can just call requestFocus() in the show listener event.
>



### **Francesco Galgani** — November 4, 2017 at 4:03 pm ([permalink](/blog/tutorial-creating-lists/#comment-24220))

> Francesco Galgani says:
>
> It’s useful. Thank you. For my better understanding, I would ask to you the transcript and the code, like in the Codename One Academy lessons… or, can you add this video to the Academy, so I can access to the transcript and to the source code?  
> Thank you.
>



### **Shai Almog** — November 5, 2017 at 5:05 am ([permalink](/blog/tutorial-creating-lists/#comment-23696))

> Shai Almog says:
>
> Thanks. All of my new videos on youtube have subtitles which you can turn on using the CC button. You can also see the full transcript of this and other videos in the how do I page for each of them here [https://www.codenameone.com…](</how-do-i/>)
>



### **מֶרֶס יָֽשָׁבְעָם** — February 2, 2020 at 3:27 am ([permalink](/blog/tutorial-creating-lists/#comment-21376))

> [מֶרֶס יָֽשָׁבְעָם](https://lh3.googleusercontent.com/a-/AAuE7mB3cyN4s67wp3h7huxY5OJOpFN36eyqduFhS7KW) says:
>
> Really good explanation. Thanks Shai, one thing I appreciate of your explanations is the fact that you consider the way to improve also the way to get things look nice, always at the programmer point of view.
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
