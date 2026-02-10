---
title: 'Tutorial: Building A Cross Platform Mobile Chat App for Android, iOS (iPhone)
  With Codename One Part I'
slug: building-a-chat-app-with-codename-one-part-1
url: /blog/building-a-chat-app-with-codename-one-part-1/
original_url: https://www.codenameone.com/blog/building-a-chat-app-with-codename-one-part-1.html
aliases:
- /blog/building-a-chat-app-with-codename-one-part-1.html
date: '2015-07-14'
author: Shai Almog
---

![Header Image](/blog/building-a-chat-app-with-codename-one-part-1/chat-app-tutorial-part1.png)

In this tutorial we will cover the basics of building a good looking chat application with Codename One that  
will work on all mobile OS’s. We will cover everything from design to social network login and the actual  
chat behavior. This tutorial is for a hand coded application mostly because GUI builder tutorials require video  
and are thus less searchable.

This project is created with the new Java 8 support to make the code simple and short.

## Creating The New Project

We create a new Codename One project and select the new flat blue theme.

![New Project](/blog/building-a-chat-app-with-codename-one-part-1/chat-app-tutorial-part2.png)

![Theme](/blog/building-a-chat-app-with-codename-one-part-1/chat-app-tutorial-part3.png)

Once finish is clicked we have a new project in place, so we need some files to get started. First place the file  
[fontello.ttf](/files/fontello.ttf) in the src directory. This font file contains image font icons which we will  
use to provide icons in the UI.

Next you will need to save [this image](/files/social-chat-tutorial-image.jpg) to your hard drive for use in the first form.

## Login UI

Now that we have all the rest in order its time to launch the designer by double clicking the theme file. In the designer  
we need to click the `Images→Quick Add Multi Images` option, then select the [image you downloaded to the disk](/files/social-chat-tutorial-image.jpg).

When prompted leave the default of `Very High`, this will effectively create a multi image which stores the image  
in multiple different resolutions and deliver the properly sized image based on the device density at hand.

Now we can just create the main form which is the entry point to the app, this will end up looking as such:

![Final Result](/blog/building-a-chat-app-with-codename-one-part-1/chat-app-tutorial-part4.png)

To achieve this we select the main theme and click the add button to add a new entry. We then type in `MainForm`  
in the top area and design the form:

  * In the first tab we uncheck ‘derive’ and select the type as `IMAGE_SCALED_FILL`. This effectively means we will  
use an image as the background and scale it across the screen. We also make sure to select the multi image that  
we just added.

  * In the Derive tab we uncheck `derive` (slightly confusing), then select `Form` in the combo box. This means that the  
`MainForm` style inherits the basic settings from the Form style.

Add another UIID called Padding so we can space the buttons away from the sides/bottoms of the form:

  * In the color tab uncheck the `Derive Transparency` and set the value to 0. This will make the container invisible.

  * In the padding section uncheck `derive` and enter 2 millimeters for all entries except for the bottom where we need 8 millimeters for extra spacing.  
This will space out the various pieces, its important to use millimeters otherwise the result will be too different on various devices based  
on their density.

![MainForm UIID Styling](/blog/building-a-chat-app-with-codename-one-part-1/chat-app-tutorial-part5.png)

### Initial Code

In the code we open the main `SocialChat` class and replace the start method with this:
    
    
    public void start() {
        if(current != null){
            current.show();
            return;
        }
        showLoginForm();
    }
    
    private void showLoginForm() {
        Form loginForm = new Form();
    
        // the blue theme styles the title area normally this is good but in this case we don't want the blue bar at the top
        loginForm.getTitleArea().setUIID("Container");
        loginForm.setLayout(new BorderLayout());
        loginForm.setUIID("MainForm");
        Container cnt = new Container(new BoxLayout(BoxLayout.Y_AXIS));
        cnt.setUIID("Padding");
        Button loginWithGoogle = new Button("Signin with Google");
        Button loginWithFacebook = new Button("Signin with Facebook");
        cnt.addComponent(loginWithGoogle);
        cnt.addComponent(loginWithFacebook);
        loginWithGoogle.addActionListener((e) -> {
            doLogin(GoogleConnect.getInstance());
        });
        loginWithFacebook.addActionListener((e) -> {
            doLogin(FacebookConnect.getInstance());
        });
        loginForm.addComponent(BorderLayout.SOUTH, cnt);
        loginForm.show();
    }
    
    void doLogin(Login lg) {
        // TODO...
    }

You will end up with something that looks like this:

![Basic styling](/blog/building-a-chat-app-with-codename-one-part-1/chat-app-tutorial-part6.png)

To get the buttons to the right color we and add the icons we will need to go back to the designer…​.

### Customizing The Buttons

Lets start with the icons, since we use an icon font this is pretty easy…​ Just add a new style UIID to the theme called  
`IconFont`.

  * In the color section click `Derive Foreground` and type in `ffffff` to set the foreground to white, then click  
`Derive Transparency` and set it to zero.

  * In the Font tab uncheck the `Derive` flag and select the `fontello.ttf` font (make sure you downloaded it and placed it  
in the src directory as instructed earlier). Select the `True Type Size` as Large.

![Icon Font](/blog/building-a-chat-app-with-codename-one-part-1/chat-app-tutorial-part7.png)

To get the buttons to work nicely we need to create an image border add 2 new UIID’s named `LoginButtonGoogle` &  
`LoginButtonFacebook`. Both should be identical with the exception of the color for the background…​

  * In the color tab set the foreground to `ffffff` and transparency to 0 (naturally uncheck derive in both cases).

  * In the `Alignment` tab uncheck derive and define the alignment as `Center`.

  * In the padding and margin tabs define all top/bottom/left/right paddings/margins to be 1 millimeter.

  * In the font tab define the system font to be large.

  * In the border section click the `Image Border Wizard` button. For the Facebook button enter 3B5999 to all the color fields  
for the Google button enter DD4B39 to all the color fields. Increase the arc width/height to 15 and then move to the  
`Cut Image` section. Enter 14 for the top/bottom/left & right values and press OK. This will effectively cut 9 multi images  
out of the given image and make a border out of them!

![Image Border Start](/blog/building-a-chat-app-with-codename-one-part-1/chat-app-tutorial-part8.png)

![Image Border Cutting](/blog/building-a-chat-app-with-codename-one-part-1/chat-app-tutorial-part9.png)

### Integrating These Changes

Now we can easily integrate the above changes in the code by just changing these lines:
    
    
    Button loginWithGoogle = new Button("Signin with Google");
    loginWithGoogle.setUIID("LoginButtonGoogle");
    Button loginWithFacebook = new Button("Signin with Facebook");
    loginWithFacebook.setUIID("LoginButtonFacebook");
    Style iconFontStyle = UIManager.getInstance().getComponentStyle("IconFont");
    loginWithFacebook.setIcon(FontImage.create(" ue96c ", iconFontStyle));
    loginWithGoogle.setIcon(FontImage.create(" ue976 ", iconFontStyle));

These effectively assign the new UIID and create two icons with the given font defined within the icon font style!

### Other Posts In This Series

This is a multi-part series of posts including the following parts:

  * [Part 1 – Initial UI](/blog/building-a-chat-app-with-codename-one-part-1.html)

  * [Part 2 – Login With Google](/blog/building-a-chat-app-with-codename-one-part-2.html)

  * [Part 3 – Login With Facebook](/blog/building-a-chat-app-with-codename-one-part-3.html)

  * [Part 4 – The Contacts Form](/blog/building-a-chat-app-with-codename-one-part-4.html)

  * [Part 5 – The Chat Form](/blog/building-a-chat-app-with-codename-one-part-5.html)

  * [Part 6 – Native Push & Finishing Up](/blog/building-a-chat-app-with-codename-one-part-6.html)

You can check out the final source code of this tutorial [here](https://github.com/codenameone/codenameone-demos/tree/master/SocialChat).
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Nigel Chomba** — July 20, 2015 at 8:39 am ([permalink](https://www.codenameone.com/blog/building-a-chat-app-with-codename-one-part-1.html#comment-24171))

> Lets move on Shai…..whats next?


### **Shai Almog** — July 20, 2015 at 2:43 pm ([permalink](https://www.codenameone.com/blog/building-a-chat-app-with-codename-one-part-1.html#comment-21651))

> We will post it when its ready.


### **Nigel Chomba** — July 22, 2015 at 5:22 pm ([permalink](https://www.codenameone.com/blog/building-a-chat-app-with-codename-one-part-1.html#comment-22252))

> Great sir


### **Michael du Plessis** — April 10, 2017 at 1:36 pm ([permalink](https://www.codenameone.com/blog/building-a-chat-app-with-codename-one-part-1.html#comment-23332))

> I notice in the documentation that you recommend not using .getTitleArea().setUIID(“Container”); anymore because it allowed hacks.  
> How can we remove the TitleArea now?


### **Shai Almog** — April 11, 2017 at 4:36 am ([permalink](https://www.codenameone.com/blog/building-a-chat-app-with-codename-one-part-1.html#comment-23354))

> getTitleArea() is deprecated because it was used as getTitleArea().removeAll(); getTitleArea().addComponent(myCmp);
>
> That was a hack we allowed in some cases but now that we have the toolbar it’s redundant & messy. Even when we allowed the hack it was discouraged because it relies too much on implementation details. However, setting the UIID doesn’t rely as much on the implementation details.
>
> You can avoid this though by adding a UIID TitleArea and overriding the styling to make it transparent, without a border, padding or margin.


### **Michael du Plessis** — April 18, 2017 at 8:34 am ([permalink](https://www.codenameone.com/blog/building-a-chat-app-with-codename-one-part-1.html#comment-23438))

> Ah I understand! Thank you for the explanation. I will also play around with the customisation of TitleArea within the Theme Editor.


### **Francesco Galgani** — November 28, 2017 at 8:58 am ([permalink](https://www.codenameone.com/blog/building-a-chat-app-with-codename-one-part-1.html#comment-23834))

> About “FontImage.create(” ue96c “, iconFontStyle)”, what is the meaning of ” ue96c “? How is chosen or generated this string? Thank you.


### **Shai Almog** — November 29, 2017 at 6:50 am ([permalink](https://www.codenameone.com/blog/building-a-chat-app-with-codename-one-part-1.html#comment-23771))

> This tutorial was written before we had material design icons integrated into Codename One so I used the fontello fonts which include various icons. When you download a font from fontello you get an HTML with all the codes in the font. I explained this here: [https://www.codenameone.com…](<https://www.codenameone.com/blog/using-icon-fonts-such-as-fontello.html>)


### **Francesco Galgani** — December 1, 2017 at 6:21 pm ([permalink](https://www.codenameone.com/blog/building-a-chat-app-with-codename-one-part-1.html#comment-23903))

> Francesco Galgani says:
>
> I’ve just seen that interesting tutorial, useful if I need custom icons not included in the material icons set. Thank you


### **Ahnaf Tahmeed** — May 3, 2020 at 11:20 pm ([permalink](https://www.codenameone.com/blog/building-a-chat-app-with-codename-one-part-1.html#comment-21399))

> [Ahnaf Tahmeed](https://lh3.googleusercontent.com/a-/AOh14GhIoPjnxowmY7XUvnUL5kz7LSzCdIa-9HB-kCXxPg) says:
>
> Do I need to be a JAVA expert to go through this course …??


### **Shai Almog** — May 4, 2020 at 6:03 am ([permalink](https://www.codenameone.com/blog/building-a-chat-app-with-codename-one-part-1.html#comment-21398))

> Shai Almog says:
>
> No. But you should know Java language syntax.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
