---
title: Permanent SideMenu, getAllStyles, Scrollbar & more
slug: permanent-sidemenu-getallstyles-scrollbar-and-more
url: /blog/permanent-sidemenu-getallstyles-scrollbar-and-more/
original_url: https://www.codenameone.com/blog/permanent-sidemenu-getallstyles-scrollbar-and-more.html
aliases:
- /blog/permanent-sidemenu-getallstyles-scrollbar-and-more.html
date: '2015-09-08'
author: Shai Almog
---

![Header Image](/blog/permanent-sidemenu-getallstyles-scrollbar-and-more/yoga-and-rest-shots.jpg)

We’ve been so busy recently that changes and features keep piling up with no end…

#### Permanent Sidemenu

It was always possible to create a permanent side menu but up until now it wasn’t trivial. For Tablets/Desktops  
the side menu UI is perfect, but the folding aspect of it isn’t as great. We have more space so we’d like to keep  
it open all the time.  
Unfortunately, the side menu implementation wasn’t very suited for such usage. With the `Toolbar` this became  
much easier to implement in a generic way. Right now we hardcoded the side menu to the left, fixing it to work  
for all sides would requite some effort. But its still pretty neat, just do something like this in your `init(Object)`  
method and your code will implicitly adapt to tablets/desktops if you used the `Toolbar` API. 
    
    
    // will return true for desktops as well...
    if(Display.getInstance().isTablet()) {
        Toolbar.setPermanentSideMenu(true);
    }

#### All Styles

We recommend using the designer and UIID’s for styles but sometimes its just inconvenient or wrong. That’s why  
we allow things such as: 
    
    
    myCmp.getUnselectedStyle().setFgColor(0xffffff);

A lot of people misuse `getStyle()` for this case which returns the current style based on state. Also  
with focusable or pressable components (e.g. Button) this becomes a HUGE chore. If I want to disable a border  
for a button in all of its states I need to do this: 
    
    
    myButton.getUnselectedStyle().setBorder(null);
    myButton.getSelectedStyle().setBorder(null);
    myButton.getPressedStyle().setBorder(null);
    myButton.getDisabledStyle().setBorder(null);

If I need to set more than one property this becomes a huge pain and I’d want to refactor this into a method that  
accepts a `Style` object etc… Painful!  
So to simplify this common use case we finally came up with something simple: `getAllStyles()`.  
This new method returns a special “fake” style object that will implicitly call all the above setters in a single go.  
**Important:** the getters for this style return meaningless values and should never be used,  
we would have added an exception for them but this would create a performance penalty.  
To use this new style for the above use case just do: 
    
    
    myButton.getAllStyles().setBorder(null);

#### Minor Updates

  * Chen added support for customizing the audio when a push arrives in Android, he posted the details to the  
[relevant issue in git](https://github.com/codenameone/CodenameOne/issues/1569).
  * We added a setter to the Picker allowing you to specify a `SimpleDateFormat` to properly format the displayed date.
  * We added a new utility method to Container that makes the common process of wrapping a `Component` in a  
`Container` trivial. E.g.  
`Container enclosed = Container.encloseIn(new BorderLayout(), new Button("Up North"), BorderLayout.NORTH);`

#### Creating A Scrollbar in Codename One

Codename One has always been “mobile first” and while we do have a desktop/web port it still feels like  
a mobile app even there which is normally fine by us.   
In the past some guys asked for scrollbar functionality and we thought it would be a bit tough, but recently  
a [discussion in the forums](https://groups.google.com/d/msg/codenameone-discussions/l-PIHO3CAEw/tO56K-p0DgAJ)  
made my aging brain cogs spin a bit and I came up with a really simple way to implement a scrollbar in  
Codename One with almost no code!  
We might build something like this into Codename One proper if there is demand for it although right now I think  
this should be enough for most cases: 
    
    
    private Container makeScrollable(final Component scrollable, Image thumb) {
        scrollable.setScrollVisible(false);
        final Slider scroll = new Slider();
        scroll.setThumbImage(thumb);
        Container sc = new Container(new BorderLayout());
        sc.addComponent(BorderLayout.CENTER, scrollable);
        sc.addComponent(BorderLayout.EAST, scroll);
        scroll.setVertical(true);
        scroll.setMinValue(0);
        scroll.setEditable(true);
        scroll.setMaxValue(scrollable.getScrollDimension().getHeight());
        scroll.setProgress(scroll.getMaxValue());
        final boolean[] lock = new boolean[1];
        scroll.addDataChangedListener(new DataChangedListener() {
            public void dataChanged(int type, int index) {
                if(!lock[0]) {
                    lock[0] = true;
                    scrollable.scrollRectToVisible(0, scroll.getMaxValue() - index, 5, scrollable.getHeight(), scrollable);
                    lock[0] = false;
                }
            }
        });
        scrollable.addScrollListener(new ScrollListener() {
            public void scrollChanged(int scrollX, int scrollY, int oldscrollX, int oldscrollY) {
                if(!lock[0]) {
                    lock[0] = true;
                    scroll.setProgress(scroll.getMaxValue() - scrollY);
                    lock[0] = false;
                }
            }
        });        
        return sc;
    }

#### Coming Soon

I have the last part of the chat app tutorial practically ready. It was delayed waiting for the new push stuff to  
come out and then got delayed because of some bugs related to specific usage of the certificate wizard.  
I’ll try to get this out next week or the week after that time permitting. 

I’ve been working on some apps recently, most specifically I was building an app in my free time for my spouses  
[Yoga Studio](http://www.ashtangayoga.co.il) and I’ve been playing with the excellent  
[Parse cn1lib](https://github.com/sidiabale/parse4cn1) from Chidiebere Okwudire.  
I was pleasantly surprised by how mature and solid it felt!  
This has been something I procrastinated on for more than 4 years and I got the whole thing done in 4 days!  
The Parse/server storage logic took me less than 24 hours and worked on the first try which is something that  
took me by surprise… 

We have some other apps mostly built as PoC’s and demos, we are thinking of a way to expose them to  
a wider audience probably as a bundle.  
At the top of the post you can see some screenshots of a couple of those apps. Ideally we’ll pair this with  
a course that would hopefully refresh the now ageing [  
CodenameOne 101](https://www.udemy.com/codenameone101/) with something more similar to the [  
Learn mobile programming by example with Codename One](https://www.udemy.com/learn-mobile-programming-by-example-with-codename-one/) course. 

Update: Since writing this we’ve launched the [Codename One Academy](/blog/launching-codename-one-academy.htmls). Check it out…
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Chidiebere Okwudire** — September 10, 2015 at 9:22 am ([permalink](/blog/permanent-sidemenu-getallstyles-scrollbar-and-more/#comment-21496))

> Chidiebere Okwudire says:
>
> Thanks for the compliments about parse4cn1. Of course, the main credit goes to the author of parse4j, Thiago Locatelli, who already laid a solid foundation in terms of design and implementation that I built upon.
>
> Parse offers some other ‘goodies’ that might be interesting to the CN1 community like analytics (though from the last I remember, Flurry had better options for analysis and segmentation). Then there’s also the now rather infamous/controversial push notification service 😉 I hope others will contribute to parse4cn1 to further extend the coverage.


### **pollaris** — April 30, 2017 at 11:53 pm ([permalink](/blog/permanent-sidemenu-getallstyles-scrollbar-and-more/#comment-23364))

> pollaris says:
>
> Your comments about getAllStyles saved me a lot of grief. The simulator was allowing a null exception when I was using getUnselectedStyle, but the device was failing it; my main form would not show, and all I had to go on was a null exception error message when my app started up on my device. I thought of your comments, and changed all of my styling for fonts to getAllStyles, and sure enough, the simulator caught the null exceptions and I was able to debug.  
> (I am sending to Universal Windows Phone). Thanks again!  
> -Russ


### **Blessing Mahlalela** — October 22, 2017 at 4:16 pm ([permalink](/blog/permanent-sidemenu-getallstyles-scrollbar-and-more/#comment-23688))

> Blessing Mahlalela says:
>
> Hi, how can I disable tensile drag on side menu. I tried to inspect the components but I can’t seem to find a container that is within Toolbar… disabling tensile drag on form worked On form only and not the Side menu. Note the app is hand coded


### **Shai Almog** — October 22, 2017 at 4:38 pm ([permalink](/blog/permanent-sidemenu-getallstyles-scrollbar-and-more/#comment-21473))

> Shai Almog says:
>
> Hi,  
> It’s a special case. There is a special theme constant for that: sideMenuTensileDragBool which you can set to false.  
> Notice we’ll switch to the on-top side menu this weekend (hopefully) so I’ll try to make that flag work over there too.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
