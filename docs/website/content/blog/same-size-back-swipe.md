---
title: Same Size & Back Swipe
slug: same-size-back-swipe
url: /blog/same-size-back-swipe/
original_url: https://www.codenameone.com/blog/same-size-back-swipe.html
aliases:
- /blog/same-size-back-swipe.html
date: '2014-07-01'
author: Shai Almog
---

![Header Image](/blog/same-size-back-swipe/same-size-back-swipe-1.jpg)

  
  
  
  
![Picture](/blog/same-size-back-swipe/same-size-back-swipe-1.jpg)  
  
  
  

Codename One inherited basic layout concepts from Swing which in turn inherited them from AWT. We modernized and adapted them quite a bit by removing various behaviors and adding others, but a key to sizing and placing components is the preferred size.  
  
  
Every component calculates its own preferred size in the calcPreferredSize() method. This calculation takes into account the font, icon, padding and theme to  
  
produce the space required by the given component but sometimes that’s not good enough.  
  
  
  
  
Preferred size is just one of the values the layout managers take into account when placing components; so you can use layout managers in pretty much any way you want to achieve the desired placement. However, there was one use case that was remarkably elusive for us with this approach: sizing a group of components to have the same size.  
  
  
  
  
Normally, we would do this by finding the largest preferred size in the group then invoke setPreferredSize on all the various components. This is hugely problematic since its both a hassle and changes to the theme, text etc. of a component in the group wouldn’t be reflected. Now we have a solution: Component.setSameWidth(Component… c); & setSameHeight(Component… c)  
  
  
  
  
So we can effectively do something like Component.setSameWidth(cmp1, cmp2, cmp3, cmp4); and they would all have the same preferred width that will be maintained even if we change the text of one of the components. Since that is the biggest use case for setPreferredSize()/W()/H() we are effectively deprecating these methods. Its generally bad practice to use them and would restrict the portability of your code so we highly recommend you use an alternative means for sizing components.  
  
  
  
  
One of our enterprise developers pointed out functionality in iOS7 that allows swiping back a form to the previous form. This is pretty hard to accomplish in Codename One because we expect transitions to be non-interactive. However, we decided to pick up that challenge since we want to upgrade transitions for iOS/Android to more closely match the new designs (e.g. Material) so we added a new API to enable back swipe transition: SwipeBackSupport.bindBack(Form currentForm, LazyValue<Form> destination);  
  
  
  
  
That one command will enable swiping back from currentForm. LazyValue is a new interface that we will probably use quite often moving forward, its defined as:  
  
  
public interface LazyValue<T> {  
  
public T get(Object… args);  
  
}  
  
  
  
  
Which effectively allows us to pass a form and only create it as necessary (e.g. for a GUI builder app we don’t have the actual previous form instance),  
  
notice that the arguments aren’t used for this case but will be used in other cases (and are used in some new capabilities in the motion).  
  
  
  
  
  
  
  
  
  
  
  
  

* * *

Notice: This post was automatically converted using a script from an older blogging system. Some elements might not have come out as intended…. If that is the case please let us know via the comments section below.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Anonymous** — July 3, 2014 at 5:38 am ([permalink](https://www.codenameone.com/blog/same-size-back-swipe.html#comment-22163))

> Anonymous says:
>
> Hi Shai, 
>
> You mention many nice new features in your blogs, but unfortunately sometimes, they get lost especially for new comers using your framework. Summarizing these features into your news letters are nice but still they get lost. Is it possible to have a section in the CodenameOne users manual on these critical fundamentals you talk about in your blog so it won’t be easily forgotten? You don’t have to rewrite, just a short description and a link to the blog post so we can refer to it further. 
>
> Thanks.


### **Anonymous** — July 3, 2014 at 11:25 am ([permalink](https://www.codenameone.com/blog/same-size-back-swipe.html#comment-21730))

> Anonymous says:
>
> Hi, 
>
> When making a point release we refresh the PDF for the developer guide with the various posts from the blog so they won’t get lost moving forward. 
>
> Updating the guide continuously is both tedious and problematic since things often change too fast and by the time we reach a point release a specific post might no longer be relevant.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fsame-size-back-swipe.html)


### **Anonymous** — July 6, 2014 at 6:25 pm ([permalink](https://www.codenameone.com/blog/same-size-back-swipe.html#comment-21925))

> Anonymous says:
>
> In my ListCellRenderer’s I quite often have: 
>
> int h = Display.getInstance().convertToPixels(8, false); 
>
> setPreferredH(h); 
>
> to set each cell to a comfortable height for selecting a row. If this method is to be deprecated, how should I do this ?
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fsame-size-back-swipe.html)


### **Anonymous** — July 7, 2014 at 2:51 am ([permalink](https://www.codenameone.com/blog/same-size-back-swipe.html#comment-22087))

> Anonymous says:
>
> setPreferredH/W are particularly bad since they rely on the existing preferred height/width which might create unintended results. 
>
> Your approach would be problematic since the font size might be very different between various platforms (e.g. TV/Watch have very different font/DPI densities). 
>
> You need to define a rendering prototype for a list (which will also make it faster) that includes the exact type of data that will give it the right size e.g. the text XXXXXXXXXXXXX and this will be used to calculate the height of the list element to match the text/images that you provide.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fsame-size-back-swipe.html)


### **Anonymous** — July 7, 2014 at 3:11 am ([permalink](https://www.codenameone.com/blog/same-size-back-swipe.html#comment-22014))

> Anonymous says:
>
> This is the render constructor: 
>
> public OptionsRenderer(Resources res) { 
>
> int h = Display.getInstance().convertToPixels(8, false); 
>
> setPreferredH(h); 
>
> title = new Label(); 
>
> title.setUIID(“MyLabel”); 
>
> setLayout(new BorderLayout()); 
>
> addComponent(BorderLayout.WEST, title); 
>
> setUIID(“Underline”); 
>
> focus = new Label(“”); 
>
> focus.setUIID(“UnderlineSelected”); 
>
> } 
>
> I want every cell to be 8mm (or thereabouts) high. I thought convertToPixels took account of DPI etc ?
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fsame-size-back-swipe.html)


### **Anonymous** — July 7, 2014 at 11:20 am ([permalink](https://www.codenameone.com/blog/same-size-back-swipe.html#comment-21661))

> Anonymous says:
>
> It takes account of DPI (when applicable) but keep in mind that DPI != font size. E.g. TV is at least 40 inches thus even though its theoretically HD its really low DPI since you sit very far from it. A watch has a ridiculously high DPI on the other hand. 
>
> Hardcoding 8 might not look good in all the ranges you want to size these things based on the content and the content is determined by the renderingPrototype. Your renderer wouldn’t matter and you won’t need to change the preferred size, it would “just work”.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fsame-size-back-swipe.html)


### **Anonymous** — July 7, 2014 at 6:24 pm ([permalink](https://www.codenameone.com/blog/same-size-back-swipe.html#comment-22173))

> Anonymous says:
>
> I take on board what you say, but what I’m trying to achieve is a minimum list cell height so that the list cells are easy to select. If a list cell has only a label, the height is obviously determined by the font/DPI etc, but on a regular sized phone, the cells are not big enough to be easily selected and I’d like some way to make sure they are atleast 8mm high.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fsame-size-back-swipe.html)


### **Anonymous** — July 7, 2014 at 6:25 pm ([permalink](https://www.codenameone.com/blog/same-size-back-swipe.html#comment-21441))

> Anonymous says:
>
> FYI – each time I press “Submit” here I get this message: 
>
> “There was an error submitting your comment. Please try again”
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fsame-size-back-swipe.html)


### **Anonymous** — July 8, 2014 at 1:47 am ([permalink](https://www.codenameone.com/blog/same-size-back-swipe.html#comment-22097))

> Anonymous says:
>
> Odd. I’m not getting that error. 
>
> You can have an entry occupy two lines not just one line, this depends on the content of your prototype. You can also add padding to the UIID. 
>
> Generally one line will indeed be smaller than 8mm. However, 8mm on a watch is an entirely different thing from 8mm on a television so its probably not a good idea to code something like this to the height of the row. If it were just padding this wouldn’t be a big deal but for actual row height you would get cut off rows etc.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fsame-size-back-swipe.html)


### **Anonymous** — July 8, 2014 at 2:50 am ([permalink](https://www.codenameone.com/blog/same-size-back-swipe.html#comment-21783))

> Anonymous says:
>
> I’ve changed my code to do this: 
>
> desc = new Label(); 
>
> int h = Display.getInstance().convertToPixels(8, false); 
>
> setPreferredH(desc.getPreferredH() < h ? h : desc.getPreferredH()); 
>
> which should be OK for all except perhaps watches.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fsame-size-back-swipe.html)


### **Anonymous** — July 8, 2014 at 11:53 am ([permalink](https://www.codenameone.com/blog/same-size-back-swipe.html#comment-21731))

> Anonymous says:
>
> This can work. Just to be clear, we have no intention of removing those methods. Just discouraging their use which we always deemed problematic at best.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fsame-size-back-swipe.html)

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
