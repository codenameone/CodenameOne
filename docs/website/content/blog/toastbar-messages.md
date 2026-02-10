---
title: ToastBar Messages
slug: toastbar-messages
url: /blog/toastbar-messages/
original_url: https://www.codenameone.com/blog/toastbar-messages.html
aliases:
- /blog/toastbar-messages.html
date: '2016-06-28'
author: Shai Almog
---

![Header Image](/blog/toastbar-messages/toast-warning.png)

The [ToastBar](https://www.codenameone.com/javadoc/com/codename1/components/ToastBar.html) was one  
of those API’s I didn’t know I needed and yet I became addicted to it…​  
Ever since Steve came out with the ToastBar I constantly catch myself typing `Display.show` only to delete that  
to use the `ToastBar`. It’s both really easy to use and more consistent with modern mobile UI design.

As a replacement for `Dialog` it really needed the static “show methods”, so we added a simpler `showErrorMessage`  
which combined the `ToastBar` with the material font icons to create a proper error message. This allowed us to  
show an error message with a single line of code, but these things shouldn’t be used only for errors which is  
why we just added two new static methods: `showMessage(String msg, char icon, int timeout)` &  
`showMessage(String msg, char icon)`.

The icon argument is one of the [FontImage](https://www.codenameone.com/javadoc/com/codename1/ui/FontImage.html)  
constants representing the material design icons. This allows us to show simpler notifications e.g. file downloaded  
successfully etc.

We can now do this using code such as this:
    
    
    Form f = new Form("Toast", BoxLayout.y());
    
    Button showError = new Button("Show Error");
    Button showNotification = new Button("Show Notification");
    Button showWarning = new Button("Show Warning");
    
    showError.addActionListener(e -> ToastBar.showErrorMessage("This is an error"));
    showNotification.addActionListener(e -> ToastBar.showMessage("This is a notification", FontImage.MATERIAL_INFO));
    showWarning.addActionListener(e -> ToastBar.showMessage("This is a warning!", FontImage.MATERIAL_WARNING));
    
    f.add(showError).
            add(showNotification).
            add(showWarning);
    
    f.show();
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Chris** — June 12, 2017 at 4:39 am ([permalink](https://www.codenameone.com/blog/toastbar-messages.html#comment-23374))

> Chris says:
>
> Thanks for the info. ToastBar.showErrorMessage() is displaying with empty box as image( ” ! ” mark in the example) instead of actual image in the front of the message. Something has been changed recently.


### **Shai Almog** — June 13, 2017 at 5:21 am ([permalink](https://www.codenameone.com/blog/toastbar-messages.html#comment-23385))

> Shai Almog says:
>
> No to my knowledge. This is derived from the theme. The icon is created based on the style of the message UIID.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
