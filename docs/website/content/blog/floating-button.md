---
title: Floating Button
slug: floating-button
url: /blog/floating-button/
original_url: https://www.codenameone.com/blog/floating-button.html
aliases:
- /blog/floating-button.html
date: '2016-09-13'
author: Shai Almog
---

![Header Image](/blog/floating-button/floating-action.png)

The material design floating action button is a powerful tool for promoting an action within your application. Quite  
a few Codename One developers implemented with own interpretation of this UI element and with the coming update  
we will have an official implementation.

The `FloatingActionButton` is a round button that resides on top of the UI typically in the bottom right hand side.  
It has a drop shadow to distinguish it from the UI underneath and it can hide two or more additional actions under  
the surface. E.g. we can create a simple single click button such as this:
    
    
    FloatingActionButton fab = FloatingActionButton.createFAB(FontImage.MATERIAL_ADD);
    fab.addActionListener(e -> ToastBar.showErrorMessage("Not implemented yet..."));
    fab.bindFabToContainer(form.getContentPane());

Which will place a `+` sign button that will perform the action. Alternatively we can create a nested action  
where a click on the button will produce a submenu for users to pick from e.g.:
    
    
    FloatingActionButton fab = FloatingActionButton.createFAB(FontImage.MATERIAL_ADD);
    fab.createSubFAB(FontImage.MATERIAL_PEOPLE, "");
    fab.createSubFAB(FontImage.MATERIAL_IMPORT_CONTACTS, "");
    fab.bindFabToContainer(form.getContentPane());

![FloatingActionButton with submenu expanded](/blog/floating-button/floating-action.png)

Figure 1. FloatingActionButton with submenu expanded

Those familiar with this widget know that there are many nuances to this UI that we might implement/expose  
in the future. At the moment we chose to keep the API simple and minimal for the common use cases and refine  
it based on demand.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Chidiebere Okwudire** — September 14, 2016 at 3:10 pm ([permalink](/blog/floating-button/#comment-22898))

> Another nice one! Does the “+” also animate to an “x” when opening the sub-menu? 🙂
>



### **Lukman Javalove Idealist Jaji** — September 14, 2016 at 3:15 pm ([permalink](/blog/floating-button/#comment-22986))

> Lukman Javalove Idealist Jaji says:
>
> So many good things coming out this week…i m happy… Am I the only developer who refreshes the homepage daily to check new blog posts? I find these articles and tutorials more useful.
>



### **Bayu Sanjaya** — September 14, 2016 at 7:32 pm ([permalink](/blog/floating-button/#comment-22922))

> Bayu Sanjaya says:
>
> Hi I got this error when trying to add fab into my container (not by form.getcontentPane()).  
> java.lang.IllegalArgumentException: Component is already contained in Container: Container[x=0 y=0 width=0 height=0, layout = LayeredLayout, scrollableX = false, scrollableY = false, components = [Container, Container]]
>



### **Shai Almog** — September 15, 2016 at 3:58 am ([permalink](/blog/floating-button/#comment-22788))

> Shai Almog says:
>
> That’s one of the nuances I was referring to. I have some thoughts on how to do these subtle animations but I don’t think they will work nicely with the FontImage class. I want to do it in a way that is elegant.
>



### **Shai Almog** — September 15, 2016 at 4:00 am ([permalink](/blog/floating-button/#comment-22908))

> Shai Almog says:
>
> I think I changed the sample code before posting and broke it, I’ll update the article later today. Notice that this will be available only on the Friday update. What you have right now is experimental.
>



### **Shai Almog** — September 15, 2016 at 4:01 am ([permalink](/blog/floating-button/#comment-22708))

> Shai Almog says:
>
> Thanks!  
> FYI we have an RSS feed so you can just subscribe to that or use RSS to email services such as ifttt  
> [https://www.codenameone.com…](<https://www.codenameone.com/feed.xml>)
>



### **Bayu Sanjaya** — September 15, 2016 at 4:04 am ([permalink](/blog/floating-button/#comment-22979))

> Bayu Sanjaya says:
>
> Hi, I use a workaround for this one, I use a container for fab.bindFabToContainer(myContainer) and add it into main form. I think it’s stable enough to implement even it is still experimental. But I’m not really sure.  
> Btw, how to change the color, i tried with UIID but the button changed into square.
>



### **Shai Almog** — September 15, 2016 at 4:59 am ([permalink](/blog/floating-button/#comment-22685))

> Shai Almog says:
>
> I’ve given this some thought and decided to just change the implementation so the code above “just works”. Set the background color of the UIID: FloatingActionButton
>



### **Jérémy MARQUER** — September 15, 2016 at 7:17 am ([permalink](/blog/floating-button/#comment-22743))

> Jérémy MARQUER says:
>
> You’re not the only one 🙂
>



### **Andreas Grätz** — September 17, 2016 at 3:47 pm ([permalink](/blog/floating-button/#comment-23102))

> Andreas Grätz says:
>
> FABs are great! But what if we have more FABs as space on the screen? The list of FABs and sub-FABs isn’t scrollable. We are working on an app, which loads the business logic from an application server. We don’t now the final count of commands.
>



### **Shai Almog** — September 18, 2016 at 4:05 am ([permalink](/blog/floating-button/#comment-23113))

> Shai Almog says:
>
> Google’s design explicitly forbids scrolling.
>
> They consider these to be “speed dial” which means you pick the top 4 entries and anything beyond that should reside in a “…” more action. This is indeed more intuitive for such cases.
>



### **disqus** — September 30, 2016 at 9:42 pm ([permalink](/blog/floating-button/#comment-23041))

> disqus says:
>
> How do I add a floating button to a Gui Builder generated form? How do I add any initialization code on Gui Builder generated forms since init method is generated code and is not supposed to be edited?
>



### **Shai Almog** — October 1, 2016 at 1:30 am ([permalink](/blog/floating-button/#comment-22834))

> Shai Almog says:
>
> You can edit the constructors just fine so code that runs before the init can go before it and code that runs after can go after the init method.
>



### **ShmuDesign** — March 1, 2017 at 8:11 am ([permalink](/blog/floating-button/#comment-23177))

> ShmuDesign says:
>
> Hi, this is good, but is it possible to remove the shadow or change the thickness ? Because I have a strange square ! ([http://shmu.fr/wooz/square-…](<http://shmu.fr/wooz/square-shadow.jpg>) I change the color to show what append) that problem appear only on IOS !
>



### **Shai Almog** — March 2, 2017 at 8:35 am ([permalink](/blog/floating-button/#comment-23224))

> Shai Almog says:
>
> Hi,  
> you can customize every aspect in the style including the shadow. I think the padding for the component might be too small causing the shadow to crop but if I’m wrong then it might be a clipping bug we need to fix in which case we’ll need to file an issue
>



### **Mo** — June 24, 2017 at 10:40 pm ([permalink](/blog/floating-button/#comment-23479))

> Mo says:
>
> Hi, I have been having trouble trying to show the FloatingActionButton when switching Locale (RTL), while the LTR works as expected, below is the code am currently using to align Right, Center,  
> `  
> fab.bindFabToContainer(root, Component.RIGHT, Component.CENTER);  
> `
>



### **Shai Almog** — June 25, 2017 at 4:45 am ([permalink](/blog/floating-button/#comment-23483))

> Shai Almog says:
>
> By convention in Codename One right becomes left in RTL mode unless you explicitly disable RTL behavior for that component. I don’t think we tested RTL on the FAB so it might be broken there. I’d expect that code to place the FAB on the left side in Codename One. I’m guessing it doesn’t? How does it look?
>



### **Mo** — June 25, 2017 at 1:28 pm ([permalink](/blog/floating-button/#comment-23508))

> Mo says:
>
> Hi Shai, thank you for getting back to me, when changing to Arabic locale (RTL) it’s not showing at all and unlike when it’s LTR direction, keeping in mind that, the root container is correctly changing direction!!
>



### **Mo** — June 25, 2017 at 11:08 pm ([permalink](/blog/floating-button/#comment-23593))

> Mo says:
>
> Hi Shai, any thoughts as to when this will be looked at or fixed, since I am planning to utilise this component on most of my Containers ??
>



### **Shai Almog** — June 26, 2017 at 4:37 am ([permalink](/blog/floating-button/#comment-23562))

> Shai Almog says:
>
> Hi,  
> first you need to file an issue and then we need to set a priority. We’re just releasing 3.7 so it will probably take a while if it’s even scheduled for 3.8 as that release is already pretty full with issues.  
> Since this issue probably existed since 2016 without a report that probably indicates the low priority of this issue.
>
> I think you might be able to workaround this by disabling RTL on the content pane and aligning the FAB to the left.
>
> FYI You can also try to fix this issue yourself and submit a pull request which is excellent practice. I have a tutorial about that here: [https://www.codenameone.com…](</blog/how-to-use-the-codename-one-sources/>)
>



### **Mo** — July 2, 2017 at 10:53 am ([permalink](/blog/floating-button/#comment-23460))

> Mo says:
>
> Apologies for the late reply!!,  
> Issue filed for this at ([https://github.com/codename…](<https://github.com/codenameone/CodenameOne/issues/2148>)), and have been trying to implement the workaround suggested, sadly this caused my other component layout to miss-behave, thus, I would rather wait for the fix and hopefully not too long!
>



### **Shai Almog** — July 3, 2017 at 3:13 pm ([permalink](/blog/floating-button/#comment-23537))

> Shai Almog says:
>
> If you only apply the LTR mode to the button and the layered layout (not the other components) it shouldn’t impact anything else.
>



### **Tommy Mogaka** — July 12, 2018 at 10:51 am ([permalink](/blog/floating-button/#comment-23927))

> Tommy Mogaka says:
>
> Hi,  
> How do I add an action to each of the submenu items? I have  
> been able to create a floating action button with three submenus but when I  
> add an actionlister it is fires on the main button and not the sub  
> buttons.
>
> FloatingActionButton fabtn = FloatingActionButton.createFAB(FontImage.MATERIAL_ADD);  
> fabtn.getAllStyles().setFont(font_small_bold);  
> fabtn.getAllStyles().setFgColor(0xffd800);  
> fabtn.getAllStyles().setBgColor(0xffd800);
>
> fabtn.addActionListener(e -> ToastBar.showErrorMessage(“Add Personel Not implemented yet…”));  
> fabtn.createSubFAB(FontImage.MATERIAL_PEOPLE, “Add Personel”).getAllStyles().setBgColor(0xffd800);  
> fabtn.setTextPosition(BOTTOM);
>
> fabtn.addActionListener(f -> ToastBar.showErrorMessage(“Add Equipment Not implemented yet…”));  
> fabtn.createSubFAB(FontImage.MATERIAL_COMMENT, “Add Equipment”).getAllStyles().setBgColor(0xffd800);  
> fabtn.setTextPosition(BOTTOM);
>
> Also, I can’t seem to get the submenu text to show! Finally, how do I style the submenu items individually e.g. text position or font colour? I have only been able to change the icon colours for the main button and the sub buttons as shown above.
>
> Thanks!
>



### **Shai Almog** — July 13, 2018 at 4:51 am ([permalink](/blog/floating-button/#comment-23847))

> Shai Almog says:
>
> createSubFab returns its own FAB instance. You need to bind your listener to that instance.
>



### **Tommy Mogaka** — February 3, 2019 at 1:34 pm ([permalink](/blog/floating-button/#comment-24015))

> Tommy Mogaka says:
>
> Hi Shai.. I was able to a create a separate ActionListener for each SubFab but how do I style each SubFub separately?
>
> Below is what I have done but the way I’ve done it doesn’t allow changing the BgColor for each SubFub as previously. Could you please show my how I can be able to do so?
>
> fabtn.createSubFAB(FontImage.MATERIAL_HELP, “Help”).addActionListener(new ActionListener()  
> {  
> @Override  
> public void actionPerformed(ActionEvent evt)  
> {  
> ToastBar.showErrorMessage(“Help button pressed…”);  
> }  
> });
>
> fabtn.createSubFAB(FontImage.MATERIAL_BUILD, “Settings”).addActionListener(new ActionListener()  
> {  
> @Override  
> public void actionPerformed(ActionEvent evt)  
> {  
> ToastBar.showErrorMessage(“Admin button pressed…”);  
> }  
> });
>



### **Shai Almog** — February 4, 2019 at 3:31 am ([permalink](/blog/floating-button/#comment-24021))

> Shai Almog says:
>
> Instead of adding the action listener save the returned instance of the subfab and then add the action listener. Then customize the color on that returned instance.
>



### **Tommy Mogaka** — February 4, 2019 at 7:31 am ([permalink](/blog/floating-button/#comment-24027))

> Tommy Mogaka says:
>
> Thanks Shai for the prompt reply… I will have to expose that my Java skills are quiet questionable as I am mostly self-taught and so I am not familiar with some concepts e.g. how to return an instance of a component.
>
> Could you please share some code snippet on how I can return an instance of a Subfab and how to save and style it? Many thanks!
>



### **Shai Almog** — February 5, 2019 at 3:10 am ([permalink](/blog/floating-button/#comment-24053))

> Shai Almog says:
>
> `FloatingActionButton subFabHelp = fabtn.createSubFAB(FontImage.MATERIAL_HELP, “Help”);  
> subFabHelp.addActionListener(…);  
> subFabHelp.setUIID(“SubFabStyle”);  
> `
>
> Or similar.
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
