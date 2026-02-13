---
title: Terse Syntax, Migration Wizard & more
slug: terse-syntax-migration-wizard-more
url: /blog/terse-syntax-migration-wizard-more/
original_url: https://www.codenameone.com/blog/terse-syntax-migration-wizard-more.html
aliases:
- /blog/terse-syntax-migration-wizard-more.html
date: '2015-10-24'
author: Shai Almog
---

![Header Image](/blog/terse-syntax-migration-wizard-more/gui-builder-migration-wizard.png)

In this last minute before 3.2 finally comes out we have a batch of new features & updates. Some of  
the last minute features that went into Codename One include: shorter more terse syntax for creating forms,  
migration wizard for the new GUI builder & dialog adaptive sizing. 

#### Better Syntax For Populating Containers and Toogle Buttons

Codename One is slightly verbose. In part its due to Java’s tradition of verbosity but some of that comes from  
the fact that we never considered terse to be a virtue. Obviously a lot of developers disagree with that notion  
and would like shorter syntax, which does have a point…  
One such small modification of the “why didn’t we do this sooner” variety is new methods we added to container  
that are shorter (`add` instead of `addComponent`) but also return the `Container`  
instance thus allowing chaining. E.g. this is taken directly from the KitchenSink’s input demo: 
    
    
    Container input = new Container(new BoxLayout(BoxLayout.Y_AXIS));
    input.addComponent(new Label("Text Field"));
    input.addComponent(new TextField("Hi World"));
    input.addComponent(new Label("Text Field With Hint"));
    TextField hint = new TextField();
    hint.setHint("Hint");
    input.addComponent(hint);
    

With the newer syntax we can cut down the verbosity significantly. Notice we can just `add("String")`  
which is equivalent to `addComponent(new Label("String"))`: 
    
    
    TextField hint = new TextField();
    hint.setHint("Hint");
    Container input = new Container(new BoxLayout(BoxLayout.Y_AXIS)).
        add("Text Field").add(new TextField("Hi World")).
        add("Text Field With Hint").add(hint);
    

The thing I really like about the code above is that its short enough to place the label and component side  
by side to each other without side scrolling in the IDE window.  
Another feature we added in the same vain is a simple factory method for `RadioButton`/  
`ToggleButton` creation. Up until now if we wanted to create a toggle button we would do something like: 
    
    
    Container toggles = new Container(new BoxLayout(BoxLayout.Y_AXIS));
    ButtonGroup bg = new ButtonGroup();
    RadioButton a = new RadioButton("a");
    a.setToggleButton(true);
    bg.add(a);
    toggles.addComponent(a);
    RadioButton b = new RadioButton("b");
    b.setToggleButton(true);
    bg.add(b);
    toggles.addComponent(b);
    

That’s pretty verbose and ridden with boilerplate code. So we shortened this slightly using the  
`createToggle` factory method that combines 3 lines above into one e.g: 
    
    
    Container toggles = new Container(new BoxLayout(BoxLayout.Y_AXIS));
    ButtonGroup bg = new ButtonGroup();
    RadioButton a = RadioButton.createToggle("a", null, bg);
    RadioButton b = RadioButton.createToggle("b", null, bg);;
    toggles.add(a).add(b);
    

#### Dialog Adaptive Sizing

Dialogs in Codename One are forms that show the previous form as a background. This causes some confusion  
since developers often think of them as standard OS “windows” which isn’t the case at all…   
We position the dialog by padding the dialog body from the sides to move it into the right place and for most  
standard dialogs we “auto calculate” this margin based on the preferred size of the dialogs content. This is  
all great until we want to change something with the content of the dialog…. 

We normally try to discourage people from building complex UI’s into a dialogs body but sometimes this is  
necessitated by the UI/UX and can’t be avoided. Up until now our workaround to grow the dialog was to dispose  
the current dialog and show a new dialog (without the transitions). However, with the new update we now have  
a new method: `growOrShrink()` which will implicitly resize a dialog to its new preferred size. 

#### GUI Builder Updates

The new GUI builder and its integration with Codename One has been going on very well, we were even able  
to add a preliminary migration wizard for existing resource files just before the 3.2 release. This wizard and  
the rest of the GUI builder are HIGHLY experimental so caution is advised, copy your project to the side and  
experiment on that! 

The migration wizard will appear for GUI builder projects and effectively should convert them to manual projects  
with the new GUI builder in place. It will replace the `StateMachineBase` class with a hardcoded  
class that does a lot of the work for partial compatibility. Its quite possible your code will work after the migration  
as is (although we would assume some work would be required). 

The following things might have an issue with the new GUI builder and the migration wizard, please use the comments  
section below with other things you notice and file issues in the  
[github issue tracker](https://github.com/codenameone/CodenameOne/issues/): 

  * Renderers – at this time renderers aren’t supported in the new GUI builder. 
  * State machine navigation is MUCH simpler and keeps an instance of the previous form so before/exit behaviors  
might vary a lot.
  * The new GUI builder always uses the `Toolbar` class, code that relies on different command  
semantics might not work properly
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Chibuike Mba** — January 29, 2016 at 10:32 pm ([permalink](https://www.codenameone.com/blog/terse-syntax-migration-wizard-more.html#comment-22457))

> Chibuike Mba says:
>
> Hi Shai,  
> the conversion tool while converting ui List Instantiates:  
> private com.codename1.ui.list.List gui_listContact = new com.codename1.ui.list.List();  
> instead of:  
> private com.codename1.ui.List gui_listContact = new com.codename1.ui.List();
>
> Am using CodenameOne 3.3


### **Shai Almog** — January 30, 2016 at 5:35 am ([permalink](https://www.codenameone.com/blog/terse-syntax-migration-wizard-more.html#comment-21501))

> Shai Almog says:
>
> Damn. Fixing it now.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
