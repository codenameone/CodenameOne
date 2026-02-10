---
title: 'TIP: Don''t Ask Again'
slug: tip-dont-ask-again
url: /blog/tip-dont-ask-again/
original_url: https://www.codenameone.com/blog/tip-dont-ask-again.html
aliases:
- /blog/tip-dont-ask-again.html
date: '2017-06-04'
author: Shai Almog
---

![Header Image](/blog/tip-dont-ask-again/tip.jpg)

One of my favorite things about Mac OS is something subtle that took me a while to notice: it doesn’t ask stupid questions. If you delete a file on a Mac it goes to the trashcan immediately, no question. Even though other OS’s copied the trashcan concept they didn’t embrace it in the same way, most of them still ask whether you are sure about this action even though the action is reversible.

Part of the problem is in the way other OS’s work, restoring some files is not as smooth as it is in Mac OS. So I understand the timidness even though I don’t like it. There is a middle of the road approach, letting the user choose between safety and minor annoyance. It’s not the ideal approach but when you are afraid to take the full plunge of “not asking” you can use something like this to show a dialog that prompts the user but provides a “don’t ask again” checkbox:
    
    
    Form current = new Form("Don't Ask Again", BoxLayout.y());
    
    Button clear = new Button("Clear");
    Button show = new Button("Show Dialog");
    
    clear.addActionListener(e -> Preferences.set("dontShowDialog", false));
    show.addActionListener(e -> {
        boolean b = Preferences.get("dontShowDialog", false);
        if(!b) {
            CheckBox areYouSure = new CheckBox("Don't ask again");
            areYouSure.setUIID("DialogBody");
            SpanLabel body = new SpanLabel("Are you sure you want to do this thing?", "DialogBody");
            Command ok = new Command("OK");
            Command cancel = new Command("Cancel");
            Command result = Dialog.show("Are you Sure?", BoxLayout.encloseY(body, areYouSure),
                    new Command[] {cancel, ok});
            if(result == ok) {
                ToastBar.showMessage("OK Pressed...", FontImage.MATERIAL_INFO);
                Preferences.set("dontShowDialog", areYouSure.isSelected());
            }
        } else {
            ToastBar.showMessage("Skipped dialog", FontImage.MATERIAL_INFO);
        }
    });
    
    current.addAll(clear, show);
    
    current.show();

This results in:

![Dialog with check box](/blog/tip-dont-ask-again/dont-ask-again.png)

Figure 1. Dialog with check box

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
