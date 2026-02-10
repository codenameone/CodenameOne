---
title: The Challenge of Multiline Strings
slug: the-challenge-of-multiline-strings
url: /blog/the-challenge-of-multiline-strings/
original_url: https://www.codenameone.com/blog/the-challenge-of-multiline-strings.html
aliases:
- /blog/the-challenge-of-multiline-strings.html
date: '2016-04-19'
author: Shai Almog
---

![Header Image](/blog/the-challenge-of-multiline-strings/setActAsLabel.png)

As part of the bug fixes for the 3.4 release we fixed [issue  
#1725](https://github.com/codenameone/CodenameOne/issues/1725) which was surprisingly difficult to fix…​ As part of that fix we also added a new method to `TextArea`  
by the name of `setActAsLabel` which we now implicitly call in the `SpanLabel` & `SpanButton` classes.

So why do we need that and why not just use the text area as it is?

There are two basic challenges in multiline strings, the first is related to the way layout managers work. Layout managers  
set the X/Y/Size of the components dynamically based on available space. This seems simple enough until we get  
into the notion of nesting & changes.

E.g. if I have a layout manager on the `Form` it knows that it can has the width/height of the screen to place components  
so it can arrange them based on that constraint. However, when nesting starts things start to get tricky…​

A child container needs its parent to allocate space for it before it knows the amount of available space. But here  
lies the paradox, how much space does it need?

For that we have the preferred size, in the case of an image this is trivial: “the image size”. In case of a `Label`  
this is trivial: “the string width”. In the case of multi-line this is hard!

Preferred size for multi-line text depends on the size we are given. E.g. if we have enough width available we won’t  
need to break another line but if we don’t then we’d want more height. The problem is that we decide on the space  
before layout is done so we don’t know the available space yet. This is called “reflow”, we ask for a specific amount of  
space and have to “reflow” to fit what is given us.

Browsers handle reflow accurately which is a major reason for their slow performance as reflow is pretty  
expensive since we have to recursively re-allocate space based on parents siblings etc.

Codename One expects you to explicitly reflow using `revalidate()` or `animateLayout(int)` if you change the  
layout after it is shown.

__ |  Don’t invoke `revalidate()` on UI that isn’t shown, it’s redundant and an expensive operation!   
---|---  
  
**Deciding where a line of text should break is one of the most delicate tasks in terms of performance!**

### String Width is Slow

So up to here we understood how hard it is to do layout properly…​ But the kicker is that just calling an API like  
`stringWidth` in pretty much every platform on earth is surprisingly slow…​

To be fair this isn’t “horribly” slow but if you invoke the code as many times as you need to it becomes a problem.  
The complexity of font rendering is tremendous and while we do cache a lot of values related to text width  
in the various implementations it is challenging since this varies a lot based on font, size and content.

E.g. say I want to render the phrase: “Mary had a little lamb” over multiple lines. A stupid algorithm  
would do something like checking the width of “M” then of “Ma” and “Mar” etc.

A slightly smarter algorithm would have a simple optimization to check if “Mary had a little lamb” fits in the space  
available and go backwards but that might be an advantage for only some of the cases.

We have a lot of optimizations to deal with those computational complexities and they mostly hide that issue while  
providing some benefits but also some penalties.

### “Faking” Preferred Width

`TextArea` effectively fakes preferred width.

This isn’t a bad thing as normally we want the text area to take up a decent amount of space and not necessarily the  
real amount it needs. So when we construct a text component in Codename One we usually give it the number of  
columns/rows and these are used to indicate the correct preferred size value that is later used for calculations.

This isn’t what we want for `SpanLabel`. We’d like it to have the actual preferred size based on it’s text.

So the `setActAsLabel` does exactly that, provides the size based on `stringWidth` and it tries to ignore the row/column  
values when the text just occupies one line.

Normally all of the information above shouldn’t matter to you in your day to day programming tasks,  
however if you run into issues such as line breaks in multi-line text areas this should give you a general understanding  
of the complexities involved.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Jacques Koorts** — April 28, 2016 at 3:25 pm ([permalink](https://www.codenameone.com/blog/the-challenge-of-multiline-strings.html#comment-22839))

> Jacques Koorts says:
>
> In HTML they introduced width by percentage to help with this problem. So if I have two multiline labels next to each other and give them a weight (android) or percentage (html) then you help the algorithm get there faster.
>
> You look at the problem from a ui designer point of view


### **Shai Almog** — April 29, 2016 at 4:06 am ([permalink](https://www.codenameone.com/blog/the-challenge-of-multiline-strings.html#comment-22576))

> Shai Almog says:
>
> Nope. You can specify the width in Codename One too (e.g. table layout) and it isn’t designed to help in this situation (for HTML or our case).
>
> Width is in box model units (e.g. percentage) and isn’t required which means you need the hierarchy size. If you actually specify the width in pixels you are effectively disabling resolution independence.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
