---
title: JavaScript Beta & InteractionDialog Popup
slug: javascript-beta-interactiondialog-popup
url: /blog/javascript-beta-interactiondialog-popup/
original_url: https://www.codenameone.com/blog/javascript-beta-interactiondialog-popup.html
aliases:
- /blog/javascript-beta-interactiondialog-popup.html
date: '2015-06-07'
author: Shai Almog
---

![Header Image](/blog/javascript-beta-interactiondialog-popup/html5-banner.jpg)

The JavaScript port is nearing beta stage which will start next week its already added support for most API’s  
including SQL support and many other features. Once the JavaScript port is in beta it will become an enterprise  
only feature so if you haven’t tried it yet you have one week to try your app. 

#### Interaction Dialog Popup

One often requested feature of the `InteractionDialog` is support for popup dialog semantics  
where the dialog can point at the originating component. You can now use the `showPopup`  
method also on an `InteractionDialog` to provide pretty elaborate UI’s. 

One of the main use cases for such a dialog is to point at a component within the title area, e.g. with the `Toolbar`  
API this can be very simple and intuitive. However, because we abstract components within the menu thru commands  
its problematic to point the `showPopup` call at the right location, to solve this we added a new  
method to both the `MenuBar` and `Toolbar` API’s: `findCommandButton(Command)`  
Notice that this API isn’t guranteed to return anything, e.g. if you use the native menus which are the default  
on Android you will get null as the result of that method. The same is probably true if you use overflow or other such API’s.  
However, this API can also be useful for many edge cases such as manipulating the appearance of a command  
dynamically which is currently pretty awkward.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
