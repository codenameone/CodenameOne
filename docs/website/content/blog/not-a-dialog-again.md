---
title: Not A Dialog – Again
slug: not-a-dialog-again
url: /blog/not-a-dialog-again/
original_url: https://www.codenameone.com/blog/not-a-dialog-again.html
aliases:
- /blog/not-a-dialog-again.html
date: '2014-06-21'
author: Shai Almog
---

![Header Image](/blog/not-a-dialog-again/not-a-dialog-again-1.png)

  
  
  
  
![Picture](/blog/not-a-dialog-again/not-a-dialog-again-1.png)  
  
  
  

When we introduced the idea of the EDT from Swing (and pretty much any modern UI toolkit) into what eventually became Codename One, one of the chief benefits was modal dialogs. Its the ability to block the executing thread in order to ask the user a question, that’s a very powerful tool for a developer. As a result of that we defined modal dialogs the way Swing/AWT defined them: stopping the execution of the code. 

However, there is another definition… Dialogs that block the rest of the UI…  
  
In that regard all dialogs in Codename One are modal; they block the parent form since they are really just forms that show the “parent” in their background as a simple image. A while back we showed you how to  
[  
“fake” a dialog  
](http://www.codenameone.com/blog/when-a-dialog-isnt-a-dialog)  
, which is pretty cool. We ended up using this technique a few times until eventually we asked ourselves: “Shouldn’t this be a part of the framework?”.

Well, now it is. We just added InteractionDialog to Codename One which tries to be very similar to Dialog in terms of API but unlike dialog it never blocks anything. Not the calling thread or the UI.  
  
Its really just a container that is positioned within the layered pane. Notice that because of that design you can have only one such dialog at the moment and if you add something else to the layered pane you might run into trouble.

Using the interaction dialog is be pretty trivial and very similar to dialog:  
  
final InteractionDialog dlg = new InteractionDialog(“Hello”);  
  
dlg.setLayout(new BorderLayout());  
  
dlg.addComponent(BorderLayout.CENTER, new Label(“Hello Dialog”));  
  
Button close = new Button(“Close”);  
  
close.addActionListener(new ActionListener() {  
  
public void actionPerformed(ActionEvent evt) {  
  
dlg.dispose();  
  
}  
  
});  
  
dlg.addComponent(BorderLayout.SOUTH, close);  
  
Dimension pre = dlg.getContentPane().getPreferredSize();  
  
dlg.show(0, 0, Display.getInstance().getDisplayWidth() – (pre.getWidth() + pre.getWidth() / 6), 0);

This will show the dialog on the right hand side of the screen, which is pretty useful for a floating in place dialog.  

* * *

Notice: This post was automatically converted using a script from an older blogging system. Some elements might not have come out as intended…. If that is the case please let us know via the comments section below.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Anonymous** — June 25, 2014 at 1:50 am ([permalink](https://www.codenameone.com/blog/not-a-dialog-again.html#comment-21838))

> Anonymous says:
>
> Is my understanding correct: the ‘fake’ dialog is the component with focus, so no events got to the parent form, but because the parent form is ‘live’ under the dialog, the UI of the parent form can be updated from the dialog ? (Unlike in ‘real’ dialogs where the parent form is just a static ‘background’ to to the dialog). 
>
> If there is no need to update the parent form is there any particular reason to prefer one over the other ?
>



### **Anonymous** — June 25, 2014 at 1:19 pm ([permalink](https://www.codenameone.com/blog/not-a-dialog-again.html#comment-21684))

> Anonymous says:
>
> Is there something that we need to do apart from refreshing libs. I am not able to add this code snnipet and test it as InteractionDialog is not available.
>



### **Anonymous** — June 25, 2014 at 4:21 pm ([permalink](https://www.codenameone.com/blog/not-a-dialog-again.html#comment-21729))

> Anonymous says:
>
> With current Dialogs the dialog takes over the entire screen so you can’t drag a widget from a dialog to the form beneath it or some other unique use cases. 
>
> For 98% of the cases I’d just use dialogs, this is a special case tool.
>



### **Anonymous** — June 25, 2014 at 4:22 pm ([permalink](https://www.codenameone.com/blog/not-a-dialog-again.html#comment-22154))

> Anonymous says:
>
> You will need to use Update Client Libs (not refresh libs which is something else) when this is updated. Since we didn’t update the plugin yet this is only available by building the source right now. The plugin will probably be refreshed sometime next week.
>



### **Anonymous** — July 13, 2014 at 6:58 am ([permalink](https://www.codenameone.com/blog/not-a-dialog-again.html#comment-21798))

> Anonymous says:
>
> Good! However, is there a way the interaction dialog can listen to events like if outside of the bounds of the dialog is clicked/touched so the dialog can animate or close. Can we determine if the interaction dialog is showing? For example dialog.isShowing(). I see some interesting use cases like in the new google plus app.
>



### **Anonymous** — July 13, 2014 at 11:46 am ([permalink](https://www.codenameone.com/blog/not-a-dialog-again.html#comment-21877))

> Anonymous says:
>
> Just add a pointer listener to the parent form to see. A dialog is showing method would probably be useful, feel free to file an RFE on that into the issue tracker.
>



### **Anonymous** — December 1, 2014 at 10:33 pm ([permalink](https://www.codenameone.com/blog/not-a-dialog-again.html#comment-22032))

> Anonymous says:
>
> Pointer pressed not solvingthat because the pointer press is also passed through the InteractionDialog to the parent form
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
