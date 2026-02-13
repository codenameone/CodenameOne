---
title: 'TIP: Listen on All Radios'
slug: tip-listen-on-all-radios
url: /blog/tip-listen-on-all-radios/
original_url: https://www.codenameone.com/blog/tip-listen-on-all-radios.html
aliases:
- /blog/tip-listen-on-all-radios.html
date: '2017-05-07'
author: Shai Almog
---

![Header Image](/blog/tip-listen-on-all-radios/tip.jpg)

Using toggle buttons in touch interfaces is very intuitive for many use cases. We implement them via `RadioButton` or `CheckBox` to indicate inclusive or exclusive selection. As a result I find myself using `RadioButton` quite a lot and ran into an ommission that frankly should have been there from day 1.

Up until now you couldn’t listen to selection on the `ButtonGroup` only on the `RadioButton` itself which isn’t as convenient since for pretty much every case you had to bind listeners over and over. With the latest version of Codename One you can bind a listener to the button group directly like this:
    
    
    Form current = new Form("Toggles", BoxLayout.y());
    
    ButtonGroup bg = new ButtonGroup();
    RadioButton option1 = RadioButton.createToggle("Option 1", bg);
    RadioButton option2 = RadioButton.createToggle("Option 2", bg);
    RadioButton option3 = RadioButton.createToggle("Option 3", bg);
    
    current.addAll(option1, option2, option3);
    bg.addActionListener(e -> ToastBar.showMessage("You selected " + (bg.getSelectedIndex() + 1), FontImage.MATERIAL_INFO));
    
    current.show();

![Toggle button selection](/blog/tip-listen-on-all-radios/radiobutton-listeners.png)

Figure 1. Toggle button selection

You will notice I used the selected index but I could have used the source component to get the radio button instance. This might not be useful for all cases but it was pretty convenient for several of my use cases.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
