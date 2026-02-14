---
title: Hamburger Sidemenu
slug: hamburger-sidemenu
url: /blog/hamburger-sidemenu/
original_url: https://www.codenameone.com/blog/hamburger-sidemenu.html
aliases:
- /blog/hamburger-sidemenu.html
date: '2013-02-26'
author: Shai Almog
---

![Header Image](/blog/hamburger-sidemenu/hqdefault.jpg)

  

The title of this post might be a bit misleading… Hamburger?  
  
  
The Hambuger sidemenu is the menu style popularized by the Facebook app, its called a Hamburger because of the 3 line icon on the top left resembling a hamburger patty between two buns (get it: its a side menu…)!  
  
  
  
Up until now these things were a pain to implement smoothly in Codename One, but Chen wouldn’t let this rest and just committed support for this feature. Working with a Hamburger menu couldn’t be simpler! Just set the command behavior to side menu (in the Display class) and it just works. You can also set it by setting the commandBehavior theme constant in the  
  
Codename One designer to “Side”. 

  
Then just add commands and watch them make their way into the side menu allowing you to build any sort of navigation you desire.  

  
Chen updated the Facebook demo to show this off, its still only available via SVN and requires the latest SVN version of Codename One for all the bells and whistles to function properly but it already looks pretty sweet!  

  
Now all we need is a cheeseburger sidemenu with fries.  
  

Notice: This post was automatically converted using a script from an older blogging system. Some elements might not have come out as intended…. If that is the case please let us know via the comments section below.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Anonymous** — February 28, 2013 at 7:21 pm ([permalink](https://www.codenameone.com/blog/hamburger-sidemenu.html#comment-21664))

> Anonymous says:
>
> Thanks Chen, was hoping that would land for 1.1 – looks great!
>



### **Anonymous** — March 1, 2013 at 6:37 am ([permalink](https://www.codenameone.com/blog/hamburger-sidemenu.html#comment-21915))

> Anonymous says:
>
> Great job Chen!!
>



### **Anonymous** — August 17, 2013 at 4:36 pm ([permalink](https://www.codenameone.com/blog/hamburger-sidemenu.html#comment-21666))

> Anonymous says:
>
> This looks great, but I have no idea how to use it. Unfortunately, as a total beginner, ” Just set the command behavior to side menu (in the Display class)” does not mean anything to me yet, and i dont know how to setup the menus on the left to start with. There is a “Commands” Properties in the editor, but when you hit Add, and enter “side menu” as the “name” attribute, it doesnt seen to do anything. 
>
> Has anyone got a link to a demo handy?
>



### **Anonymous** — August 18, 2013 at 1:34 am ([permalink](https://www.codenameone.com/blog/hamburger-sidemenu.html#comment-21917))

> Anonymous says:
>
> Thanks, its sometimes hard to see where we are being obtuse. The facebook demo shows the side menu. 
>
> To activate command behavior either: 
>
> Open the designer, select the theme. Go to the constants tab, click “Add” and select commandBehavior from the combo box, type in Side for the value. 
>
> Or use: 
>
> Display.getInstance().setCommandBehavior(Display.COMMAND_BEHAVIOR_SIDE_NAVIGATION); 
>
> Commands are either added by clicking the Command section in the form in the GUI designer and adding commands or by invoking the method on form called addCommand.
>



### **Anonymous** — August 18, 2013 at 7:20 am ([permalink](https://www.codenameone.com/blog/hamburger-sidemenu.html#comment-21811))

> Anonymous says:
>
> Magic – works! Added in the designer. Worked with latest eclipse plugin, no SVN pull required. CN1 is amazing.
>



### **George Njoroge** — September 4, 2015 at 9:29 am ([permalink](https://www.codenameone.com/blog/hamburger-sidemenu.html#comment-22218))

> George Njoroge says:
>
> Can I get a link project source code
>



### **Shai Almog** — September 5, 2015 at 3:58 am ([permalink](https://www.codenameone.com/blog/hamburger-sidemenu.html#comment-22340))

> Shai Almog says:
>
> Its in our demos repository [http://github.com/codenameo…](<http://github.com/codenameone/codenameone-demos/>)
>



### **Bertrand Gauvreau** — October 16, 2015 at 3:52 pm ([permalink](https://www.codenameone.com/blog/hamburger-sidemenu.html#comment-22442))

> Bertrand Gauvreau says:
>
> I Shai ! First, Thank you for offering us CodenameOne ! It’s a fantastic tool !  
> I have an issue with the hamburger menu. When I run my app on my Android device and click on the hamburger menu icon, the menu opens but it slides up with the whole window and makes the Action Bar disappear. The action bar reappears only when I restart the app.  
> Do you have any idea ?  
> Thank You
>



### **Shai Almog** — October 17, 2015 at 4:33 am ([permalink](https://www.codenameone.com/blog/hamburger-sidemenu.html#comment-22367))

> Shai Almog says:
>
> Great to hear!  
> Did you update the theme constant to side?
>
> I would suggest you migrate to the new Toolbar API which is more powerful than the side menu on its own and encapsulates all its functionality.
>



### **Bertrand Gauvreau** — October 17, 2015 at 12:46 pm ([permalink](https://www.codenameone.com/blog/hamburger-sidemenu.html#comment-24182))

> Bertrand Gauvreau says:
>
> Thank you Shai
>



### **Mahmoud** — March 26, 2016 at 8:43 pm ([permalink](https://www.codenameone.com/blog/hamburger-sidemenu.html#comment-22701))

> Mahmoud says:
>
> Dear Shai,  
> i have menu and my background is white but i have gradient line at the first of menu  
> how i can remove it
>



### **Shai Almog** — March 27, 2016 at 4:26 am ([permalink](https://www.codenameone.com/blog/hamburger-sidemenu.html#comment-21503))

> Shai Almog says:
>
> That’s the shadow for the sidemenu set the theme constant sideMenuShadowBool=false
>



### **Mahmoud** — March 27, 2016 at 5:43 am ([permalink](https://www.codenameone.com/blog/hamburger-sidemenu.html#comment-24203))

> Mahmoud says:
>
> Thanks Shai 🙂
>



### **Akinniranye James** — September 14, 2016 at 1:39 pm ([permalink](https://www.codenameone.com/blog/hamburger-sidemenu.html#comment-22559))

> Akinniranye James says:
>
> Is it possible to achieve a side menu as rich as this? Since we cant even set different uiids for commands. Am considering using layered pane layout
>



### **Shai Almog** — September 15, 2016 at 3:47 am ([permalink](https://www.codenameone.com/blog/hamburger-sidemenu.html#comment-22762))

> Shai Almog says:
>
> This is the side menu in the up to date kitchen sink. You can customize the Commands heavily but it’s far more intuitive to use the Toolbar API…
>
> [https://uploads.disquscdn.c…](<https://uploads.disquscdn.com/images/4de23e2fec20427d2fc6d5bc27191e5934536d8287fffcb59186d90880ef197d.png>)
>



### **Akinniranye James** — September 15, 2016 at 8:01 pm ([permalink](https://www.codenameone.com/blog/hamburger-sidemenu.html#comment-21458))

> Akinniranye James says:
>
> Wow, I barely recognize this Kitchen Sink, wow. Good job.
>



### **Akinniranye James** — September 15, 2016 at 8:26 pm ([permalink](https://www.codenameone.com/blog/hamburger-sidemenu.html#comment-21823))

> Akinniranye James says:
>
> My bad. I never knew there is toolbar..addComponentToSideMenu, I have only been using toolbar.addCommandToSideMenu
>



### **tracey-de santa** — July 26, 2017 at 6:21 am ([permalink](https://www.codenameone.com/blog/hamburger-sidemenu.html#comment-23538))

> tracey-de santa says:
>
> cool,
>



### **Akinniranye James** — July 26, 2017 at 3:59 pm ([permalink](https://www.codenameone.com/blog/hamburger-sidemenu.html#comment-23253))

> Akinniranye James says:
>
> check out our app [https://play.google.com/sto…](<https://play.google.com/store/apps/details?id=com.encentral.tattara.mobile>).  
> using codenameone
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
