---
title: Wiki, ParparVM Performance, ActionEvent Type & more
slug: wiki-parparvm-performance-actionevent-type
url: /blog/wiki-parparvm-performance-actionevent-type/
original_url: https://www.codenameone.com/blog/wiki-parparvm-performance-actionevent-type.html
aliases:
- /blog/wiki-parparvm-performance-actionevent-type.html
date: '2016-01-12'
author: Shai Almog
---

![Header Image](/blog/wiki-parparvm-performance-actionevent-type/parparvm-blog.jpg)

Following with the feedback on the recent survey we spent a lot of time debating how we can improve the  
general process of documentation and ideally have the community more involved. So one of the first things  
we did is place our entire developer guide directly into the  
[Codename One Wiki](https://github.com/codenameone/CodenameOne/wiki/Index)!  
This allows any one of you without any special permissions (just a standard github account) to edit the wiki  
directly. No pull request or anything like that required. The syntax is based on asciidoc which is reasonably  
intuitive so if you see something wrong/missing etc. just edit it directly, we review all changes after the  
fact so if you get something wrong we will fix it! 

If you want to monitor changes to the wiki and help us maintain it you can use the wiki RSS feed from github:  
[  
https://github.com/codenameone/CodenameOne/wiki.atom](https://github.com/codenameone/CodenameOne/wiki.atom). I also created an  
[ifttt recipe](https://ifttt.com/recipes/372144-codename-one-wiki-update-to-e-mail)  
for this but I’m not sure if it works well.  
We’d also appreciate pull requests that improve our javadoc comments in the source so feel free to submit  
such changes as well if you see something that needs adding. 

#### ParparVM Performance

Another issue that was raised in the surveys was performance, this is an ongoing task and a lot of the regressions  
we had in the past month are related to a heavy commitment we made to native grade performance. Steve  
recently committed a lot of improvements to the architecture of ParparVM that should significantly boost  
the performance of some simple calculations and arithmetics bringing them to the level of native C performance  
by eliminating stack operations and removing the locals overhead. 

#### ActionEvent Type Information

AWT/Swing created a lot of listener type interfaces and classes for practically every possible type of event in  
the system. This made the code easy to understand but also made generic implementations harder and  
increased the overall footprint of the JVM for things like AOT compilation. 

With Codename One we went to the other extreme which is probably not ideal either (but somewhat hard  
to fix in retrospect) of using `ActionListener`/`ActionEvent` for everything.  
This has advantages such as really simple and generic event binding, however it creats a situation where  
the generic code to process an events can’t easily distinguish one type of event from another.  
Dave Dyer contributed a  
[pull request](https://github.com/codenameone/CodenameOne/commit/43237332b308e169ef4034a7b436e7e35518549a)  
that added a new type attribute to action events together with a `Type` enum. 

You can use this new capability with a line like this: 
    
    
    ActionEvent.Type t = ev.getEventType();

You can then use a switch case to handle various types of events or log them to the console for debugging. 

#### Shape API’s And Transforms On Mutable Images

Steve implemented the 2D shape API’s in Codename One quite a while back. One of the known problems was  
the fact that some of these API’s weren’t implemented on mutable images in iOS.   
The reasoning for that is that iOS has two very distinct and incompatible pipelines in Codename One. Rendering  
to the screen is done via OpenGL ES 2.0 and is very fast. Rendering to images is done via the iOS drawing API’s  
which are normally pretty slow but can be done on an offline surface. 

With this you can now finally draw charts onto images, however some other effects such as perspective transform  
probably won’t work on an image. 

#### Simpler Padding/Margin in Code

Up until now if you wanted to change padding/margin you had to do something like this: 
    
    
    style.setPadding(Component.TOP, padding);

Getting the padding wasn’t much better: 
    
    
    int paddingPixels = style.getPadding(Component.TOP);

We now have two ways to do this that are simpler: 
    
    
    style.setPaddingTop(padding);
    int paddingPixels = style.getPaddingTop();

This obviously applies to the sides/bottom as well.  
The main motivation isn’t just the shorter amount of code and intuitive nature of the API. Its also slightly faster  
as some checks become unnecessary by the specific code. 

#### Centering the Toolbar

We added to `Toolbar` the ability to center the title that was somewhat lost in the move between  
the old title and the new `Toolbar` class. To the uninitiated it would seem that just styling the  
“Title” UIID to center should work. It seems to work on the surface but fails when we have uneven element widths  
to the right & left.   
E.g. a sidemenu icon on the left with nothing on the right. 

This effectively allocates a space to the title that starts after the side menu and the centering operation is relative  
to that location not to absolute screen width. By doing it like this we layout everything to the center in the layout  
level which should produce the desired result.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Chidiebere Okwudire** — January 14, 2016 at 8:20 am ([permalink](/blog/wiki-parparvm-performance-actionevent-type/#comment-22507))

> Chidiebere Okwudire says:
>
> Hi,
>
> Good first step with the documentation in my opinion! I’m not sure though what will be more better – reviewing changes after the fact or managing changes via pull requests. Of course, that’s something that time will tell.
>
> One thing that’s immediately obvious is the absence of navigation. Not such a big issue but can be better. You might also want to check out what the Parse guys do. Sometime last year, they also moved their docs to github [[https://github.com/ParsePla…](<https://github.com/ParsePlatform/Docs>)] but somehow, they are able to still render it with more appealing formatting on their website [[https://parse.com/docs](<https://parse.com/docs>)]. On the github README, they roughly explain how that is realize this using a backend parse app and a background job. This kind of solution (not necessarily using a Parse backend) could be cool for the CodenameOne docs as well.
>
> Cheers
>



### **Shai Almog** — January 14, 2016 at 8:35 am ([permalink](/blog/wiki-parparvm-performance-actionevent-type/#comment-22399))

> Shai Almog says:
>
> We already got a great set of edits there from [https://github.com/Isborg](<https://github.com/Isborg>) who did an amazing job at fixing a lot of my mistakes! So I’m pretty happy.
>
> This wasn’t clear in the article above but [https://www.codenameone.com…](<https://www.codenameone.com/manual/>) and the generated PDF will still work exactly as before. The only difference is that we now generate them from the wiki sources. So the source in the wiki won’t necessarily be 100% correct since they didn’t go thru our validation but the manual should still work as expected. I explained our PDF generation workflow and placed the code back in the day within the jbake user group but it might warrant better docs especially with the work we’d like to do moving forward. I think we need to invest a lot in the look and feel of the developer guide/javadocs.
>
> I placed a right side navigation panel very similar to the existing manual which should be pretty intuitive too.
>



### **Chidiebere Okwudire** — January 20, 2016 at 11:57 am ([permalink](/blog/wiki-parparvm-performance-actionevent-type/#comment-22390))

> Chidiebere Okwudire says:
>
> Thanks for the clarification. Cool that the pdf is generated from the wiki. By the way, the IFTTT recipe works 😉
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
