---
title: Make it Look Like the Design
slug: make-it-look-the-same-as-the-design
url: /blog/make-it-look-the-same-as-the-design/
original_url: https://www.codenameone.com/blog/make-it-look-the-same-as-the-design.html
aliases:
- /blog/make-it-look-the-same-as-the-design.html
date: '2017-08-28'
author: Steve Hannah
---

![Header Image](/blog/make-it-look-the-same-as-the-design/learn-codenameone-2.jpg)

The Michael Jackson documentary, “This is It”, included a behind-the-scenes look at Michael Jackson’s preparation for his final tour, that tragically would never happen. In one scene, he has an exchange with the keyboard player that goes roughly as follows:

**Michael Jackson (to keyboard player) :** That isn’t right

**Keyboard player :** How do you want it? I can do it anyway you want it.

**Michael Jackson :** Just make it sound like on my CD.

I recall laughing at that time, thinking yeah “just like the CD”. What the keyboard player had been doing sounded like the CD to me, but apparently to Michael, it was off.

Obviously my untrained ear didn’t know what to look for. Had **I** been playing keyboard, the instruction of “make it sound like the CD” wouldn’t have helped me at all. I would need him to break it down for me into something more tangible. E.g Faster, slower, louder, softer, etc…​

User interface design is very similar to music in this sense. When we use a well-designed UI, it just “feels” nice, but we may not know why. Those who are skilled in design may be able to break a successful design down into components – but the rest of us can only sit and marvel at the magic.

I have blogged, in the past, about UI design in Codename One. In [this tutorial](/blog/psd-to-app-converting-a-beautiful-design-into-a-native-mobile-app/), I walk, step by step, through the process of converting a PSD file into a mobile app. Unfortunately, upon re-reading that tutorial, I realize that I completely skipped one of the **most** important steps. I essentially began with “let’s make our app look just like the design”, but I didn’t describe what it meant to look “just like the design”. In this blog post, I’d like to unpack what “just like the design” means to me, in the context of mobile app development.

So, what does “Just like the design” mean? A broad overview is that fonts, colors, padding, margin, and alignment must match the design. In addition, the sections of the UI must be retained correctly. The last part (sections), is most relevant when adapting the UI to different screen sizes. Below, I go into detail on each of these aspects of design.

__ |  I come from the “programming” side of the fence. I don’t have any formal design training, other than having read a few books on the subject, and tips that I have picked in in past projects by **actual** designers.   
---|---  
  
## Fonts

**Fonts** should match as much as possible. Try to use the exact same font. If that isn’t possible, then at least find a similar font.

[This website](http://www.identifont.com/similar.html) allows you to type in a font name, and it will show you a list of suggestions for similar fonts. At the bare minimum you’ll want to choose a font that is on the same side of the serif/sans-serif divide. One simple test I use to “rule out” a font is to compare the the lower case “a”, “g”, and “y”, as each has two distinct variants. I generally want my “similar” font to at least agree on those three glyphs.

## Colors

**Colors** should match exactly. I often use the built-in “Digital Color Meter” application on OS X for determining the color. An arguably better method is to go directly to the source, and ask Photoshop what color it is.

## Padding and Margin

**Padding and Margin** should match. This is important and it is too often over-looked by developers. The spacing between icons and their labels, the amount of padding between a button’s text and its border, etc…​ The designer chose the spacing very carefully to create an over-all feeling. You can butcher the design by simply forgetting to pay attention to padding and margin.

__ |  Always use millimetres for specifying margins, padding, and font-size. **NEVER** use pixels, except in cases where you are first calculating the run-time pixel dimensions of another component (or the screen) to use as a basis for your sizing.   
---|---  
  
## Alignment

**Alignment** is critical. Pay close attention to any invisible “lines” in the design. E.g. which labels are aligned on their left or right sides, and which ones are centered. Your app should preserve these invisible lines.

## Identify Sections

**Identify the Sections** of the design. Take a step back and look at the big picture of the design. You need to understand the semantics of the design if you want to be able to adapt it to different screen sizes and aspect ratios. A simplified version of this step is identifying “Header”, “Content”, and “Footer”. Usually you’ll want to ensure that sections retain their size and position relative to the screen dimensions.

I find it useful to draw a wire-frame representation of the design. Once I think I have it right, I’ll try to adapt the wireframe to different aspect ratios so I can decide, for example, whether an image should be 1 inch, or 25% of the screen height. This part can be the most challenging because there may be subtle design decisions in how the sections are laid out on the screen. Think along the lines of [rule of thirds](https://en.wikipedia.org/wiki/Rule_of_thirds), a rule of thumb used by photographers for staging photographs. If you don’t have direct access to the designer, it may be difficult to “big picture” composition decisions that should be retained to preserve the spirit of the design; but you should at least try.

## Stop, Stare, and Iterate

When you think you have achieved a finished product, you should stop and stare at the design for a while. Pay attention to how it makes you feel. If something feels wrong, you should walk through the above points (fonts, colors, padding, alignment, sections), and try to identify what is off. Try this in the simulator using different skins and orientations.

Repeat as necessary until it “feels” right.

## Fresh Eyes

Show the design and screenshots from your app to someone else and ask for feedback. They may be able to spot differences that you weren’t able to.

## Conclusion

These are the techniques that I use to recreate beautiful designs in Codename One. If you think I’ve missed something here, please leave comments so that I can improve my own process. The goal is perfection. Nothing less!
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **bryan** — August 29, 2017 at 11:51 pm ([permalink](/blog/make-it-look-the-same-as-the-design/#comment-23596))

> bryan says:
>
> I agree with all you wrote, but I’m wondering where you sit on the “native” L&F vs custom L&F. While you can set margins etc for your design, they may conflict with what people who expect a “pure” native design might expect ?
>



### **Shai Almog** — August 30, 2017 at 3:51 am ([permalink](/blog/make-it-look-the-same-as-the-design/#comment-23721))

> Shai Almog says:
>
> I think the native theme should be the starting point and everything that “matters” should be layered on top. E.g. iOS and Android have slightly different title area behaviors that should be derived from the native theme e.g. title alignment. However, some designs require the title to behave differently so if you have one of those designs then just override the native theme for that specific purpose.
>
> Since both OS converged a lot over design I think you can extract reasonable commonality that will work well for both and still feel native enough to the platform but this requires some imagination and a good relationship with the designer which leaves a lot of room for problems.
>



### **Francesco Galgani** — September 13, 2017 at 8:58 am ([permalink](/blog/make-it-look-the-same-as-the-design/#comment-23604))

> Francesco Galgani says:
>
> Thanks for this article. You suggested: «Fonts should match as much as possible. Try to use the exact same font. If that isn’t possible, then at least find a similar font.», but in several articles and tutorials the use of the native fonts (roboto for Android and HelveticaNeue for iOS) is the suggested option because they are “good looking by default” and they support well all languages. I’m a bit confused.
>



### **Shai Almog** — September 14, 2017 at 5:57 am ([permalink](/blog/make-it-look-the-same-as-the-design/#comment-23687))

> Shai Almog says:
>
> The goal of this article is to get “pixel perfect” compatibility with the design. In order to do that the fonts must match. I find that what the designer/client usually “want” is the native OS fonts. The main reason they want those is for consistency, even if your font is really pretty it might feel out of place on the device so I always go with the native fonts.  
> Having said that, fonts have a HUGE impact on design. If you change the font the UI feels different and won’t match the PSD you got from the designer.
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
