---
title: Toolbar Back & Easier Material Icons
slug: toolbar-back-easier-material-icons
url: /blog/toolbar-back-easier-material-icons/
original_url: https://www.codenameone.com/blog/toolbar-back-easier-material-icons.html
aliases:
- /blog/toolbar-back-easier-material-icons.html
date: '2016-05-16'
author: Shai Almog
---

![Header Image](/blog/toolbar-back-easier-material-icons/back-arrow.png)

When we initially launched Codename One it was pretty hard to imagine todays apps. Menus and back navigation  
were miles apart when comparing Android 2.x and iOS 4.x…​ So we created a very elaborate set of abstractions  
(command behavior) that served as a set of patch framework we could live with for a while.

But like any patch framework this started crumbling under its shear weight and this lead us to the  
[Toolbar API](/javadoc/com/codename1/ui/Toolbar/). The reason we  
could do that is that a lot of the platform differences have converged, in 2012 it was blasphemy to have  
a back button in Android title area and now it’s the official material design guideline.

So while platforms are still different their differences are more refined an less conceptual and that is the perfect  
environment for the `Toolbar` API. `Toolbar` solved most of these differences in a very portable and robust way but  
one thing it didn’t codify was back button behavior, this was mostly due to the many nuances within this functionality  
and due to our bad experience with abstracting it in the old command behavior framework.

### Back Command

In the previous implementation we would just call:
    
    
    form.setBackCommand(cmd);

This would implicitly place the command in the title for iOS and wouldn’t do it for other platforms.

When you use this with the [Toolbar API](/javadoc/com/codename1/ui/Toolbar/)  
you would only get the hardware back button behavior and the command wouldn’t map to the title area leaving  
iOS users with no other means to navigate.

To solve this in a more generic way we now added 4 new methods to `Toolbar`:
    
    
    public Command setBackCommand(String title, ActionListener<ActionEvent> listener)
    public void setBackCommand(Command cmd)
    public Command setBackCommand(String title, BackCommandPolicy policy, ActionListener<ActionEvent> listener)
    public void setBackCommand(Command cmd, BackCommandPolicy policy)

The default `setBackCommand(cmd)` and `setBackCommand(String, ActionListener<ActionEvent>)` will set the  
hardware back command and always set the material design back arrow to the left title area. This will allow you  
to keep a consistent look across platforms.

You will notice the usage of `BackCommandPolicy`, that is an enum with the following possible values:

  * `ALWAYS` – Show the back command always within the title bar on the left hand side. This will add the command  
supplied “as is” but will give it the `BackCommand` UIID

  * `AS_REGULAR_COMMAND` – this is effectively the same as calling `addCommandToLeftBar` and setting the  
hardware back command. This is identical to the `ALWAYS` option with the exception of using the `TitleCommand`  
UIID instead of the `BackCommand` UIID

  * `AS_ARROW` – **this is the default behavior** for `setCommand`. Uses the material design arrow icon for the back  
button which will always appear on all platforms

  * `ONLY_WHEN_USES_TITLE` – this will only place the back command when the theme constant `backUsesTitleBool`  
is set to true. By default only the iOS themes have it set to true so the back command will only appear on the title  
for iOS devices

  * `WHEN_USES_TITLE_OTHERWISE_ARROW` – this option combines the logic of `AS_ARROW` and `ONLY_WHEN_USES_TITLE`.  
It will use the command to show the back button on iOS devices but will use the material design arrow icon  
for other devices

  * `NEVER` – this adds nothing to the title bar. It only sets the hardware button. Effectively this option is here mostly  
for completeness.

#### GUI Builder Apps

The old GUI builder has a navigation stack that automatically adds back buttons. Thus apps built with the old  
GUI builder had a problem mapping to back behavior with the `Toolbar`.

We now fixed that so the [UIBuilder](/javadoc/com/codename1/ui/util/UIBuilder/)  
checks if a `Toolbar` is installed and if so calls its version of `setBackCommand` effectively using the `AS_ARROW`  
back behavior.

If this isn’t what you had in mind but you still want to use the `Toolbar` with an old GUI builder app you can override the  
method:
    
    
    protected void setBackCommand(Form f, Command backCommand)

You can then set the command as you see fit to the parent form.

### Simpler Icon Font

One of the minor annoyances with using icon fonts is the need to adapt them to a given style. This works nicely  
when we have a component like a button where we can use the buttons style as the basis for the UI but it  
becomes painful with features such as commands where we don’t have a reference to the style object.

To make this a bit easier we made this code simpler:
    
    
    Style s = UIManager.getInstance().getComponentStyle("TitleCommand");
    FontImage icon = FontImage.createMaterial(FontImage.MATERIAL_SEARCH, s, 3);

As this:
    
    
    FontImage icon = FontImage.createMaterial(FontImage.MATERIAL_SEARCH, "TitleCommand", 3);

This will create a 3mm icon with the search icon and the foreground/background scheme of the `TitleCommand`  
UIID.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Lukman Javalove Idealist Jaji** — May 20, 2016 at 8:25 am ([permalink](/blog/toolbar-back-easier-material-icons/#comment-22718))

> Lukman Javalove Idealist Jaji says:
>
> Thanks Shai for this post. I am particularly happy with the new Material Icon Code …. Means I don’t have to create several UIIDS in the designer which sometimes slows me down….


### **Mo** — May 28, 2016 at 6:19 pm ([permalink](/blog/toolbar-back-easier-material-icons/#comment-22926))

> Mo says:
>
> I am on 3.4.0 plugin but unable to use the Toolbar.setBackCommand, which version of the lib the above function is available on?? should the lib be updated manually or via the Plugin??


### **Shai Almog** — May 29, 2016 at 3:12 am ([permalink](/blog/toolbar-back-easier-material-icons/#comment-22900))

> Shai Almog says:
>
> Go to the preferences, under the Codename One section select “Update Client Libs”.


### **Mo** — May 31, 2016 at 9:01 pm ([permalink](/blog/toolbar-back-easier-material-icons/#comment-22933))

> Mo says:
>
> Hi Shai, many thanks for your earlier reply, and having updated the Project Libs via the project properties, I came across strange and unexpected behavior while using the TextField, which can be seen/reproduced on the Simulator with the Kitchen Sink Input demo after updating the libs, please verify at your end and advice if something was overlooked on my end or with the latest libs?
>
> I am using Netbeans 8.1 with JDK1.8 and CodenameOnePlugin 3.4.0.


### **Shai Almog** — June 1, 2016 at 3:46 am ([permalink](/blog/toolbar-back-easier-material-icons/#comment-21453))

> Shai Almog says:
>
> Hi,  
> what’s the unexpected behavior?


### **Mo** — June 1, 2016 at 11:30 am ([permalink](/blog/toolbar-back-easier-material-icons/#comment-22603))

> Mo says:
>
> Thank you for the speedy reply, and indeed, some of the strange behavior can be seen on the Kitchen Sink Input Demo via the Simulator after “updating the Project Libs” such as:
>
> 1.On the Text Field, if you use the Tab key and returned , the original text cannot be removed and any new input will be visibly written on top of the previous.  
> Please try a few times and you will see this behavior,
>
> 2\. On the Text Field With Hint, if you type any text and press the Tab, the text will be replicated to the Multi-Line Text Field and cannot be removed.  
> Try a couple of times and you will see that, it’s written on top of the previous!!
>
> I have already shared the screenshots along with the updated jar!
>
> Please keep in mind that, this behavior is not happening on the previous version and before updating the Project Libs,
>
> Please let me know if you would like any additional clarification on this?


### **Shai Almog** — June 2, 2016 at 3:34 am ([permalink](/blog/toolbar-back-easier-material-icons/#comment-22911))

> Shai Almog says:
>
> I didn’t see that but looking at the commit history I see Chen committed a fix that looks to be related to that so I guess it will be resolved this Friday. You can easily follow commits as explained here: [https://www.codenameone.com…](</blog/keep-track-of-codename-one-changes-duplicates/>)

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
