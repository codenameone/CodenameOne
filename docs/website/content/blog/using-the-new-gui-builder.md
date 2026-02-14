---
title: Using the new GUI Builder
slug: using-the-new-gui-builder
url: /blog/using-the-new-gui-builder/
original_url: https://www.codenameone.com/blog/using-the-new-gui-builder.html
aliases:
- /blog/using-the-new-gui-builder.html
date: '2016-07-11'
author: Shai Almog
---

![Header Image](/blog/using-the-new-gui-builder/gui-builder-chrome.png)

We had a couple of posts in the past about the new GUI builder but we didn’t have a “how to” guide yet. In this  
post we’ll try to go step by step over the process of using the GUI builder and understanding its inner workings.  
We’ll also try to clarify conclusively the role the new GUI builder plays in the toolchain and the migrations process  
from the old GUI builder.

### How do I Know if I’m Using the new GUI Builder?

The old GUI builder is in the designer tool, it’s a Swing application that includes the theme design etc.  
It generates a `Statemachine` class that contains all the main user GUI interaction code.

The new GUI builder is a standalone application that you launch from the right click menu by selecting a form  
as explained below. Here are screenshots of both to help you differentiate:

![The old GUI builder](/blog/using-the-new-gui-builder/old-gui-builder-sample.png)

Figure 1. The old GUI builder

![The new GUI builder](/blog/using-the-new-gui-builder/new-gui-builder-sample.png)

Figure 2. The same UI in the new GUI builder

Why two GUI Builders?

The original old GUI builder has it’s roots in our work at Sun Microsystems. We developed it as part of the  
designer tool and we store it’s data in the resource file. When creating an application for the old GUI  
builder you must define it as a “visual application” which will make it use the old GUI builder.

The roots of this GUI builder are pretty old. When we initially built it we still had to support feature phones with  
2mb of RAM and the iPad wasn’t announced yet. Due to that we picked an architecture that made sense for those  
phones with a greater focus on navigation and resource usage. Newer mobile applications are rivaling desktop  
applications in complexity and in those situations the old GUI builder doesn’t make as much sense

### Hello World

Creating a hello world app in the new GUI builder is actually pretty trivial, you need to start with a regular handcoded  
application. Not a GUI builder application as it refers to the old GUI builder!

Creating a new hello world is similar for all IDE’s. We cover this process in the getting started tutorials for all the  
IDE’s:  
[NetBeans](http://www.codenameone.com/how-do-i---create-a-basic-hello-world-application—​send-it-to-my-device-using-netbeans.html),  
[IntelliJ](http://www.codenameone.com/how-do-i---create-a-basic-hello-world-application—​send-it-to-my-device-using-intellij-idea.html) &  
[Eclipse](http://www.codenameone.com/how-do-i---create-a-basic-hello-world-application—​send-it-to-my-device-using-eclipse.html).

__ |  The new GUI builder requires Java 8. This means the IDE itself needs to run on top of Java 8!   
---|---  
  
Following are the instructions for creating a form and launching the GUI builder. While they are similar there  
are minor IDE differences. Usage of the GUI builder is identical in all IDE’s as the GUI builder is a separate  
application.

#### NetBeans

In NetBeans you need to follow these 4 steps:

![Right click the package select New -> Other](/blog/using-the-new-gui-builder/netbeans-gui-builder-step-1.png)

Figure 3. Right click the package select New → Other

![In the Codename One section select the GUI builder form](/blog/using-the-new-gui-builder/netbeans-gui-builder-step-2.png)

Figure 4. In the Codename One section select the GUI builder form

![Type in the name of the form and click finish](/blog/using-the-new-gui-builder/netbeans-gui-builder-step-3.png)

Figure 5. Type in the name of the form and click finish, you can change the type to be a Container or Dialog

![Launch the GUI builder thru the right click menu on the newly created file](/blog/using-the-new-gui-builder/netbeans-gui-builder-step-4.png)

Figure 6. Launch the GUI builder thru the right click menu on the newly created file

#### IntelliJ/IDEA

In IntelliJ you need to follow these 3 steps:

![Right click the package select New -> Codename One Form \(or Dialog/Container\)](/blog/using-the-new-gui-builder/intellij-gui-builder-step-1.png)

Figure 7. Right click the package select New → Codename One Form (or Dialog/Container)

![Type in a name for the new form](/blog/using-the-new-gui-builder/intellij-gui-builder-step-2.png)

Figure 8. Type in a name for the new form

![Launch the GUI builder thru the right click menu on the newly created file](/blog/using-the-new-gui-builder/intellij-gui-builder-step-3.png)

Figure 9. Launch the GUI builder thru the right click menu on the newly created file

#### Eclipse

In Eclipse you need to follow these 4 steps:

![Right click the package select New -> Other](/blog/using-the-new-gui-builder/eclipse-gui-builder-step-1.png)

Figure 10. Right click the package select New → Other

![In the Codename One section select the GUI builder option](/blog/using-the-new-gui-builder/eclipse-gui-builder-step-2.png)

Figure 11. In the Codename One section select the GUI builder option

![Type in the name of the form and click finish](/blog/using-the-new-gui-builder/eclipse-gui-builder-step-3.png)

Figure 12. Type in the name of the form and click finish, you can change the type to be a Container or Dialog

![Launch the GUI builder thru the right click menu on the newly created file](/blog/using-the-new-gui-builder/eclipse-gui-builder-step-4.png)

Figure 13. Launch the GUI builder thru the right click menu on the newly created file

#### Basic Usage

Notice that the UI of the new GUIBuilder might change in the future but the basic concepts should remain the same.

We control the GUI builder via it’s main toolbar, notice that your changes will apply only when you click  
the Save button on the right:

![The features of the main toolbar](/blog/using-the-new-gui-builder/gui-builder-main-toolbar.png)

Figure 14. The features of the main toolbar

The sidebar includes the functionality we will be working with most of the time:

![The sidebar options](/blog/using-the-new-gui-builder/gui-builder-sidebar.png)

Figure 15. The sidebar options

We’ll start by selecting the Component Palette and dragging a button into the UI:

![You can drag any component you want from the sidebar to the main UI](/blog/using-the-new-gui-builder/gui-builder-drag-button-1.png)

Figure 16. You can drag any component you want from the sidebar to the main UI

You can then re-arrange the order of the components but since they use the default `FlowLayout` you can’t  
position them anywhere you want. We’ll discuss arrangement and layout managers in the GUI builder below.

You should have a UI that looks like this when you select the button you placed, it shows the properties that you  
can modify and the events that you can bind:

![Properties allow you to customize everything about a component](/blog/using-the-new-gui-builder/gui-builder-properties.png)

Figure 17. Properties allow you to customize everything about a component

You can edit any property by clicking it, this launches the appropriate UI. E.g. for image properties you  
are presented with an image dialog that allows you to pick an image from the resource file:

![You can edit properties such as the icon property by clicking it](/blog/using-the-new-gui-builder/gui-builder-image-dialog.png)

Figure 18. You can edit properties such as the icon property by clicking it, this opens the image selection dialog

__ |  You can add an image to the resource file using the designer tool as covered in  
[this video](http://www.codenameone.com/how-do-i---fetch-an-image-from-the-resource-file---add-a-multiimage.html)  
---|---  
  
For things like setting the text on the component we can use a convenient “long click” on the component to  
edit the text in place as such:

![Use the long click to edit the text ](/blog/using-the-new-gui-builder/gui-builder-in-place-edit.png)

Figure 19. Use the long click to edit the text “in place”

#### Events

When a component supports broadcasting events you can bind such events by selecting it, then selecting  
the events tab and clicking the button matching the event type

![The events tab is listed below supported event types can be bound above](/blog/using-the-new-gui-builder/gui-builder-events.png)

Figure 20. The events tab is listed below supported event types can be bound above

Once an event is bound the IDE will open to the event code e.g.:
    
    
    public void onButton_1ActionEvent(com.codename1.ui.events.ActionEvent ev) {
    }

__ |  Some IDE’s only generate the project source code after you explicitly build the project so if your code needs  
to access variables etc. try building first   
---|---  
  
Within the code you can access all the GUI components you defined with the `gui_` prefix e.g. `Button_1` from the  
UI is represented as:
    
    
    private com.codename1.ui.Button gui_Button_1 = new com.codename1.ui.Button();

#### Layouts

In this section we won’t try to discuss layouts in depth as this is a deep and complex subject. You can read  
more about the properties of the Codename One layouts in  
[the developer guide](https://www.codenameone.com/manual/basics.html).

In general layouts define the mathematical logic for component positions that we can then apply to the  
resolutions supported by the devices. If we didn’t have layouts the UI wouldn’t fit on the multitude of devices  
where it should work. You can nest layouts by placing containers within the UI and giving any container a  
different layout manager, this allows you to construct very elaborate layouts.

You can pick a layout manager using this UI:

![Layouts can be picked via the GUI builder UI](/blog/using-the-new-gui-builder/gui-builder-layouts.png)

Figure 21. Layouts can be picked via the GUI builder UI

Most layouts support settings that allow you to configure their behavior, e.g. `FlowLayout` supports  
settings such as these that allow you to align the components within it to your locations of choice:

![FlowLayout settings](/blog/using-the-new-gui-builder/gui-builder-layouts-settings-flowlayout.png)

Figure 22. FlowLayout settings

`BorderLayout` & `TableLayout` support constraints that allow you to provide additional hints about the components  
within the layout:

![Border layout constraints](/blog/using-the-new-gui-builder/gui-builder-layouts-constraints-border.png)

Figure 23. Border layout constraints

Mixing these layouts in a hierarchy allows you to produce most UI’s.

E.g. one of the most powerful tricks in the new GUI builder is the multi selection mode:

![Multi selection icon](/blog/using-the-new-gui-builder/gui-builder-multi-select-off.png)

Figure 24. Multi selection icon

When this mode is turned on the icon turns into a cancel icon to cancel that mode. When it’s turned on you can  
select multiple components and perform operations on all of them such as changing a property on multiple  
components or enclosing them in a container e.g.:

![When we toggle on multi-select mode and select several components we can then enclose them in a Container](/blog/using-the-new-gui-builder/gui-builder-enclose-option.png)

Figure 25. When we toggle on multi-select mode and select several components we can then enclose them in a Container

#### Underlying XML

Saving the project generates an XML file representing the UI into the res directory in the project, the GUI file  
is created in a matching hierarchy in the project under the res/guibuilder directory:

![The java and GUI files in the hierarchy](/blog/using-the-new-gui-builder/gui-builder-java-and-gui-files.png)

Figure 26. The java and GUI files in the hierarchy

__ |  If you refactor (rename or move) the java file it’s connection with the GUI file will break. You need  
to move/rename both   
---|---  
  
You can edit the GUI file directly but changes won’t map into the GUI builder unless you reopen it. These files  
should be under version control as they are the main files that change. The GUI builder file for the button and  
label code looks like this:
    
    
    <?xml version="1.0" encoding="UTF-8"?>
    
    <component type="Form" layout="FlowLayout" flowLayoutFillRows="false" flowLayoutAlign="1" flowLayoutValign="0"  title="My new title" name="MyForm">
      <component type="Button" text="Button" name="Button_1" actionEvent="true">
      </component>
      <component type="Label" text="Hi World" name="Label_1">
      </component>
    </component>

This format is relatively simple and is roughly the same format used by the old GUI builder which makes the migration  
to the new GUI builder possible. This file triggers the following Java source file:
    
    
    package com.mycompany.myapp;
    
    /**
     * GUI builder created Form
     *
     * @author shai
     */
    public class MyForm extends com.codename1.ui.Form {
    
        public MyForm() {
            this(com.codename1.ui.util.Resources.getGlobalResources());
        }
    
        public MyForm(com.codename1.ui.util.Resources resourceObjectInstance) {
            initGuiBuilderComponents(resourceObjectInstance);
        }
    
    //-- DON'T EDIT BELOW THIS LINE!!!
        private com.codename1.ui.Label gui_Label_1 = new com.codename1.ui.Label();
        private com.codename1.ui.Button gui_Button_1 = new com.codename1.ui.Button();
    
    
    // <editor-fold defaultstate="collapsed" desc="Generated Code">
        private void guiBuilderBindComponentListeners() {
            EventCallbackClass callback = new EventCallbackClass();
            gui_Button_1.addActionListener(callback);
        }
    
        class EventCallbackClass implements com.codename1.ui.events.ActionListener, com.codename1.ui.events.DataChangedListener {
            private com.codename1.ui.Component cmp;
            public EventCallbackClass(com.codename1.ui.Component cmp) {
                this.cmp = cmp;
            }
    
            public EventCallbackClass() {
            }
    
            public void actionPerformed(com.codename1.ui.events.ActionEvent ev) {
                com.codename1.ui.Component sourceComponent = ev.getComponent();
                if(sourceComponent.getParent().getLeadParent() != null) {
                    sourceComponent = sourceComponent.getParent().getLeadParent();
                }
    
                if(sourceComponent == gui_Button_1) {
                    onButton_1ActionEvent(ev);
                }
            }
    
            public void dataChanged(int type, int index) {
            }
        }
        private void initGuiBuilderComponents(com.codename1.ui.util.Resources resourceObjectInstance) {
            guiBuilderBindComponentListeners();
            setLayout(new com.codename1.ui.layouts.FlowLayout());
            setTitle("My new title");
            setName("MyForm");
            addComponent(gui_Label_1);
            addComponent(gui_Button_1);
            gui_Label_1.setText("Hi World");
            gui_Label_1.setName("Label_1");
            gui_Button_1.setText("Click Me");
            gui_Button_1.setName("Button_1");
        }// </editor-fold>
    
    //-- DON'T EDIT ABOVE THIS LINE!!!
        public void onButton_1ActionEvent(com.codename1.ui.events.ActionEvent ev) {
        }
    
    }

__ |  Don’t touch the code within the DON’T EDIT comments…​   
---|---  
  
The GUI builder uses the “magic comments” approach where code is generated into those areas to match the  
XML defined in the GUI builder. The IDE’s generate that code at different times. Some IDE’s will generate it when you  
run the app while others will generate it as you save the GUI in the builder.

You can write code freely within the class both by using the event mechanism, by writing code in the  
constructors or thru overriding functionality in the base class.

### Should I Switch

Yes if you want to move forward.

If your app is in maintenence mode then no. We have no plans to remove support for the old GUI builder although  
we will de-emphasize it slowly.

While we still aren’t ready to crown the GUI builder as fully production grade we do think the XML based file format  
is very stable (since we inherited it from the old GUI builder) and should thus work rather well. So even if you  
run into issues in the tool itself you can probably workaround most of them thru the XML code which is already  
easier to work with than it was with the old GUI builder.

Currently, NetBeans and Eclipse have a migration wizard from the old GUI builder. It is flaky as it tries to “fake”  
the “statemachine navigation” and does it rather badly. So the assumption is that you would need to use that  
as a starting point and adapt the logic of the application.

If your app is very complex it might be a difficult task so we would suggest dedicating some time to evaluating this  
before jumping in head first.

### Final Word

We’ve spent a lot of time building up the new GUI builder and it is still not as refined as the old one. However,  
we think that once you guys start reporting bugs & requesting enhancements it will quickly mature into an  
amazing tool.

Take the time to work with it and let us know what you think.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Yaakov Gesher** — August 31, 2016 at 5:19 am ([permalink](https://www.codenameone.com/blog/using-the-new-gui-builder.html#comment-22943))

> How do we handle localization in the new GUI builder? What’s the equivalent to StaeMachine’s findMyComponent() methods?
>



### **Yngve Moe** — August 31, 2016 at 10:38 pm ([permalink](https://www.codenameone.com/blog/using-the-new-gui-builder.html#comment-22945))

> I can’t scroll the advanced properties list for a MultiButton, so a number of properties can’t be accessed. This is on a Windows 10 PC.
>
> If this is an error, I might try to edit the XML file manually. Is its format documented somewhere?
>



### **Shai Almog** — September 1, 2016 at 5:42 am ([permalink](https://www.codenameone.com/blog/using-the-new-gui-builder.html#comment-23031))

> Because the GUI builder is decoupled from the resource file at the moment the only solution is to add a string to the localization bundle in the resource file matching a string in the GUI builder. Ideally we’d like to rework localization and themeing in the same way we reworked the GUI workflow and remove the need for the old resource editor completely.
>



### **Yaakov Gesher** — September 1, 2016 at 6:01 am ([permalink](https://www.codenameone.com/blog/using-the-new-gui-builder.html#comment-21461))

> What about getting a handle on components?
>



### **Shai Almog** — September 2, 2016 at 5:13 am ([permalink](https://www.codenameone.com/blog/using-the-new-gui-builder.html#comment-22803))

> In the new GUI builder all components are just class fields e.g. gui_MyComponent. To get them just compile the project and the source will be generated.
>



### **Adebisi Oladipupo** — September 2, 2016 at 5:59 pm ([permalink](https://www.codenameone.com/blog/using-the-new-gui-builder.html#comment-22853))

> I have followed the new GUI builder overview and tried using it but it seems not matured. I am unable to attach background image to a form and/or container as it is possible with the old guy builder by creating new UIID. The property list for for components seem to be abbreviated (not enough) as compared to the old builder.
>
> Using the old builder also presents a problem with asking for a filename when saving the design; instead of creating a nd inserting the GUI form as part of the package. What am I missing?
>
> I have also bought a copy of the Developer guide for codename one from Amazon but the coverage on the GUI builder from Aug 2016 is very scanty. I will appreciate your guidance. I know you all prefer hand coding but some of us, visual developers like gui builders to alleviate design task. If GUI designer/builder is going to be part of Codenameone, then let’s make it usable and the best it can be just as the main program. Thanks
>



### **Shai Almog** — September 3, 2016 at 4:31 am ([permalink](https://www.codenameone.com/blog/using-the-new-gui-builder.html#comment-21655))

> The old GUI builder is 6+ years old so undoubtedly it’s more mature. For styling you need to launch the designer (old UI) and create the style, you don’t have the shortcuts we used to have because the tools are now completely separate.
>
> I didn’t understand the second issue.
>
> We’ll try to improve the GUI builder documentation further as we move forward, the focus is on handcoding as it’s much harder to document (drag this here etc.).
>



### **Shai Almog** — September 3, 2016 at 4:36 am ([permalink](https://www.codenameone.com/blog/using-the-new-gui-builder.html#comment-22558))

> Thanks, we’ll fix it for the next GUI builder update!
>



### **Yngve Moe** — September 3, 2016 at 6:56 am ([permalink](https://www.codenameone.com/blog/using-the-new-gui-builder.html#comment-22676))

> A very nice feature would have been if the GUI builder were able to see your CSS files and render the GUI accordingly. I suppose one would have to load the CSS file manually into the builder environment.
>



### **Shai Almog** — September 4, 2016 at 5:43 am ([permalink](https://www.codenameone.com/blog/using-the-new-gui-builder.html#comment-22985))

> On the very right side of the toolbar (top) you will see 3 vertical dots (overflow) pressing that will allow you to change the theme.
>
> CSS is a plugin and not a part of Codename One “proper” this works with the res files which CSS is supposed to generate.
>



### **Yngve Moe** — September 4, 2016 at 3:29 pm ([permalink](https://www.codenameone.com/blog/using-the-new-gui-builder.html#comment-24226))

> Ok, that sound reasonable. However I can’t find the vertical dots. The rightmost items on my toolbar are “Save” and “Preview Design”. Am I looking in the wrong place? I can’t find that button anywhere on this page, either.
>



### **Shai Almog** — September 5, 2016 at 4:29 am ([permalink](https://www.codenameone.com/blog/using-the-new-gui-builder.html#comment-22787))

> If the overflow menu is missing that means the GUI builder found one theme only and is using that.  
> I’m guessing Steve generated the res file as part of the CSS build process which makes some sense but it’s obviously problematic for use cases like this.
>
> A workaround would be to copy the res file from the build into the src directory where it should be detected. The problem with this though is that you would need to remember to remove it.
>



### **Adebisi Oladipupo** — September 5, 2016 at 6:28 pm ([permalink](https://www.codenameone.com/blog/using-the-new-gui-builder.html#comment-22963))

> Thanks Shai and sorry for not being clear. I am trying to adopt the new GUI builder since that’s the future. I am having difficult time nesting containers as you showed in the calculator example. I am not asking to be told “drag this here and there”.  
> It would be great if one can drag components in the component tree to structure the layout rather than dragging elements into their outlines on the right side of the screen (an action that is more of a trial and error). Doing so has actually resulted in deleting or making components disappear from the tree. I applaud you and your team for a great product. I am not being critical but rather providing input for making it better.  
> A quick refinement could be having the component tree visible while dragging new components into the project.
>
> I have tried for a week to duplicate your example of the calculator in the new GUI builder but no luck. I just can’t get the nesting to happen for the containers. It was much easier doing it in the old GUI since I could just drag the elements into the component tree rather than trying to do it in the graphic panel where things just jump around without guidance.
>
> I think the process will be helped along if the UI construction process is further enhanced with the new GUI builder rather than creating UI forms via hardcoding alone; just my two cents.
>
> Thanks for any and all assistance you can provide the growing visual developers who also code to make the forms functional.
>



### **Adebisi Oladipupo** — September 5, 2016 at 6:31 pm ([permalink](https://www.codenameone.com/blog/using-the-new-gui-builder.html#comment-22994))

> See a sample frustrated attempt to nest containers. All subsequent containers on the form are supposed to be nested under the first one …
>



### **Adebisi Oladipupo** — September 6, 2016 at 1:15 am ([permalink](https://www.codenameone.com/blog/using-the-new-gui-builder.html#comment-22888))

> To understand my last post, here’s the login form I am trying to reproduce. I got the free PSD files from one of codenameone tutorial links. I have retrieved and added all the relevant images into the res editor as instructed. Any help with advice on layout structures to use will be greatly appreciated. Thanks. P.S. I have the png file that makes the background translucent under the content pane.
>



### **Adebisi Oladipupo** — September 6, 2016 at 1:21 am ([permalink](https://www.codenameone.com/blog/using-the-new-gui-builder.html#comment-22815))

> This is how far I got. May get further if only I can nest containers easily and reliably in the new GUI builder (at least that is what I think, but could be wrong).
>



### **Shai Almog** — September 6, 2016 at 4:01 am ([permalink](https://www.codenameone.com/blog/using-the-new-gui-builder.html#comment-22714))

> Thanks. We are aware of some issues related to nesting that trigger exceptions. These exceptions are hard to recover from…
>
> We had such issues in the old GUI builder as well and we worked around them over time. The problem with fixing these is in producing reliable test cases as it’s pretty hard to do this for GUI builder where the test case consists of “drag this here” then “that here” etc. making the effort pretty big.
>
> If you have such a test case that would be helpful. Regardless we have some ideas on improving the robustness of the GUI builder so it acts in a more reliable way. If you used the old GUI builder extensively you might recall that when it failed it presented an error then would “undo” that error essentially stabilizing the development process. We’ll try to introduce similar behavior to the new GUI builder with this or the next update.
>



### **Adebisi Oladipupo** — September 8, 2016 at 1:01 am ([permalink](https://www.codenameone.com/blog/using-the-new-gui-builder.html#comment-21462))

> Thanks again Shai. One more question.
>
> Can a form designed in JFormdesigner be used in a codenameone application? If so, how? I know I can start a form so designed from the main class with: ” new LoginForm().show()” ; but not sure if the generated codes by JFormdesigner can be used and work with codenaeone.
>



### **Shai Almog** — September 8, 2016 at 4:40 am ([permalink](https://www.codenameone.com/blog/using-the-new-gui-builder.html#comment-22873))

> JFormDesigner is for Swing code and not Codename One API. You might be able to edit the code it generates for Codename One compatibility but it will stop working as a visual tool once you do that…
>



### **Adebisi Oladipupo** — September 8, 2016 at 11:49 pm ([permalink](https://www.codenameone.com/blog/using-the-new-gui-builder.html#comment-23050))

> It may not b worth the effort to use JFormdesigner in that respect. I will stick with Codenameone GUI knowing better days are ahead. Patience is a virtue. Thanks
>



### **Akinniranye James** — September 9, 2016 at 12:30 am ([permalink](https://www.codenameone.com/blog/using-the-new-gui-builder.html#comment-22804))

> I share your pain. Trying to create this layout for almost a day. The GUI Builder is far from okay. I just resolved to editing xml by hand (not as difficult as it sounds).
>



### **Yngve Moe** — September 13, 2016 at 6:56 pm ([permalink](https://www.codenameone.com/blog/using-the-new-gui-builder.html#comment-22995))

> Found a bug: if I try to add a “DataChange Event” to a Slider, the emitted Java code calls “addDataChangeListener(…)”, which is a syntax error (should be “addDataChangedListener(…)”).
>



### **Shai Almog** — September 14, 2016 at 4:51 am ([permalink](https://www.codenameone.com/blog/using-the-new-gui-builder.html#comment-22889))

> Ugh. This was actually something we changed in reverse without noticing: [https://github.com/codename…](<https://github.com/codenameone/CodenameOne/commit/5f41a94984e8c16dd1a292aad475ffd2964a8d61>)
>
> Turns out that this method was named inconsistently across the code with half of the cases using Change and half going with changed. Autocomplete hid this for what must have been 8 years or so… I’ve fixed it to use Changed as this makes more sense.
>



### **Phil ip** — March 29, 2017 at 4:25 pm ([permalink](https://www.codenameone.com/blog/using-the-new-gui-builder.html#comment-23399))

> Tried the new gui builder. Promissing, but not easy to handle at this state. I am wondering about using FXML like JavaFX2? Are there any plans in this direction?
>



### **Shai Almog** — March 30, 2017 at 6:09 am ([permalink](https://www.codenameone.com/blog/using-the-new-gui-builder.html#comment-23080))

> No. JavaFX hasn’t picked industry traction and since it’s remarkably heavy supporting it would cripple any mobile app. FXML is tied up too deep into FX and isn’t useful without it.
>
> We’d rather focus our efforts on helping Android developers since there are about 10000 of those compared to one FX developer…
>



### **Michael du Plessis** — April 6, 2017 at 3:03 pm ([permalink](https://www.codenameone.com/blog/using-the-new-gui-builder.html#comment-23503))

> Hi! I’m really keen on using CodeNameOne, I’ve got Java down fine, I’m just needing to get used to the GUI Builder. The code you mentioned for the GUI elements being generated within the “magical comments” aren’t generating. I notice this is post is about 9 months old and have seen there are updates to the new GUI Builder, it looks slightly different to the above screenshots.  
> I followed the instructions but like I said, even created a complete new project to make sure, but the code isn’t generating on it’s own within the .java file. The XML file is fine though. Any advice forward would be majorly appreciated, I’m very excited since I discovered this and wouldn’t want my excitement and motivation to wane because of a slight mishap.
>



### **Shai Almog** — April 7, 2017 at 5:50 am ([permalink](https://www.codenameone.com/blog/using-the-new-gui-builder.html#comment-23199))

> Shai Almog says:
>
> On which IDE/OS configuration are you experiencing this?
>



### **Michael du Plessis** — April 7, 2017 at 6:28 am ([permalink](https://www.codenameone.com/blog/using-the-new-gui-builder.html#comment-23142))

> Michael du Plessis says:
>
> I’m working in Netbeans on Windows
>



### **Shai Almog** — April 8, 2017 at 11:44 am ([permalink](https://www.codenameone.com/blog/using-the-new-gui-builder.html#comment-23409))

> Shai Almog says:
>
> Try running from command line by going to your user home directory and under the .codenameone directory you should see a file called guibuilder_1.jar try running it using:
>
> java -jar guibuilder_1.jar and see how that works. It should print out error messages with more details on the failure
>



### **Michael du Plessis** — April 8, 2017 at 9:35 pm ([permalink](https://www.codenameone.com/blog/using-the-new-gui-builder.html#comment-23421))

> Michael du Plessis says:
>
> I’ve figured out the issue. Netbeans requires me to manually right-click on the .java file for the GuiBuilder class and click on “Compile File”. Might be worth adding to the above instructions. 🙂  
> I’m so glad it works now, and thank you for your responses! 🙂  
> I also noted during my trial and error, when I tried checking to see if my plugin tool was maybe out of date, that it said the following:  
> “Unable to connect to the CodenameOnePlugin Update Center because of  
> [https://codenameone.googlec…](<https://codenameone.googlecode.com/svn/trunk/CodenameOne/repo/netbeans/updates.xml>) ”  
> Thought I’d just query about it anyway.
>



### **Shai Almog** — April 9, 2017 at 4:06 am ([permalink](https://www.codenameone.com/blog/using-the-new-gui-builder.html#comment-23101))

> Shai Almog says:
>
> The source is only generated on build you can build/run the project to see it.
>
> The update center problem is something we’ll fix in the next plugin update.
>



### **Jean Naude** — May 6, 2017 at 3:35 pm ([permalink](https://www.codenameone.com/blog/using-the-new-gui-builder.html#comment-23449))

> Jean Naude says:
>
> Hi, I am trying to follow this tutorial (using IntelliJ Idea on a MacBook) but when I open the GUI Builder I get the following message after a few moments, before I do anything in the GUI. [https://uploads.disquscdn.c…](<https://uploads.disquscdn.com/images/b2a15b22cc3eefc092e77df69d5caba0e55272d0ffd3d913d698dd2132552922.png>)
>



### **Shai Almog** — May 7, 2017 at 3:55 am ([permalink](https://www.codenameone.com/blog/using-the-new-gui-builder.html#comment-24146))

> Shai Almog says:
>
> Can you try this: [https://www.codenameone.com…](<https://www.codenameone.com/blog/tip-track-designer-guibuilder-issues.html>)  
> Let me know what’s printed in the prompt when you get this crash.
>



### **Jean Naude** — May 7, 2017 at 9:32 pm ([permalink](https://www.codenameone.com/blog/using-the-new-gui-builder.html#comment-23394))

> Jean Naude says:
>
> This is what I get:
>
> Jeans-MacBook-Pro:~ Jean$ cd .codenameone  
> Jeans-MacBook-Pro:.codenameone Jean$ java -jar guibuilder.jar  
> Connector: file:/Users/Jean/.guiBuilder/guibuilder.input  
> [EDT] 0:0:0,1 – Codename One revisions: 1933c5f6f587753f0b5a1eac2a0548bddc6ff41a  
> 2355
>
> [EDT] 0:0:0,1 – Trying to load the Codename One GUI builder file: file:/Users/Jean/IdeaProjects/TryAgain/res/guibuilder/com/mycompany/myapp/SecondForm.gui  
> [EDT] 0:0:0,1 – Successfully loaded the form XML:
>
> <component type=”Form” layout=”FlowLayout” title=”SecondForm” name=”SecondForm”></component>  
> [EDT] 0:0:0,63 – createComponent for element: <component name=”SecondForm” type=”Form” layout=”FlowLayout” title=”SecondForm”>  
> </component>
>
> dyld: lazy symbol binding failed: Symbol not found: ___sincos_stret  
> Referenced from: /Library/Java/JavaVirtualMachines/jdk1.8.0_131.jdk/Contents/Home/jre/lib/libjfxwebkit.dylib  
> Expected in: /usr/lib/libSystem.B.dylib
>
> dyld: Symbol not found: ___sincos_stret  
> Referenced from: /Library/Java/JavaVirtualMachines/jdk1.8.0_131.jdk/Contents/Home/jre/lib/libjfxwebkit.dylib  
> Expected in: /usr/lib/libSystem.B.dylib
>
> Trace/BPT trap: 5  
> Jeans-MacBook-Pro:.codenameone Jean$
>



### **Shai Almog** — May 8, 2017 at 5:18 am ([permalink](https://www.codenameone.com/blog/using-the-new-gui-builder.html#comment-23475))

> Shai Almog says:
>
> That’s a JDK bug it seems Oracle compiled the JDK’s webkit support incorrectly. I have the same JDK version on a Mac but I’m on El Capitan what’s your OS version?  
> This might be resolved by changing OS/JDK versions (reverting to 1.8.9x or something).
>



### **Jean Naude** — May 8, 2017 at 7:39 am ([permalink](https://www.codenameone.com/blog/using-the-new-gui-builder.html#comment-23568))

> Jean Naude says:
>
> I have OS X 10.8.5 (Mountain Lion). I have not upgraded to Sierra yet because I have burnt my fingers badly in the past with upgrades (Windows) and because I have limited bandwidth. Sierra does not seem to offer anything that I need. What is your advice?
>
> I see I have an old jdk 1.6 in the following directory: /System/Library/Java/JavaVirtualMachines  
> and jdk 1.8.0_91 and 1.8.0_131 in /Library/Java/JavaVirtualMachines. The Home Alias in /Library/Java points to jdk 1.6. I think the problem lies here but I don’t know how to fix it (new to OS X, have always used Windows).
>
> Following internet searches I tried  
> export JAVA_HOME=/Library/Java/Home  
> and then tried to run the GUI Builder again:
>
> Jeans-MacBook-Pro:.codenameone Jean$ java -jar guibuilder.jar  
> Exception in thread “main” java.lang.UnsupportedClassVersionError: com/codename1/apps/guibuilder/desktop/GUIBuilderMain : Unsupported major.minor version 52.0  
> at java.lang.ClassLoader.defineClass1(Native Method)  
> at java.lang.ClassLoader.defineClassCond([ClassLoader.java](<http://ClassLoader.java>):637)  
> at java.lang.ClassLoader.defineClass([ClassLoader.java](<http://ClassLoader.java>):621)  
> at java.security.SecureClassLoader.defineClass([SecureClassLoader.java](<http://SecureClassLoader.java>):141)  
> at java.net.URLClassLoader.defineClass([URLClassLoader.java](<http://URLClassLoader.java>):283)  
> at java.net.URLClassLoader.access$000([URLClassLoader.java](<http://URLClassLoader.java>):58)  
> at java.net.URLClassLoader$[1.run](<http://1.run)([URLClassLoader.java](http://URLClassLoader.java)>:197)  
> at java.security.AccessController.doPrivileged(Native Method)  
> at java.net.URLClassLoader.findClass([URLClassLoader.java](<http://URLClassLoader.java>):190)  
> at java.lang.ClassLoader.loadClass([ClassLoader.java](<http://ClassLoader.java>):306)  
> at sun.misc.Launcher$AppClassLoader.loadClass([Launcher.java](<http://Launcher.java>):301)  
> at java.lang.ClassLoader.loadClass([ClassLoader.java](<http://ClassLoader.java>):247)
>
> Help, please.
>



### **Shai Almog** — May 9, 2017 at 5:54 am ([permalink](https://www.codenameone.com/blog/using-the-new-gui-builder.html#comment-24131))

> Shai Almog says:
>
> 1.6.x won’t work because it’s an old JDK we need 1.8.x. Mountain Lion is really old by now so it’s possible Oracle doesn’t test some functionality well enough on it. I’m on El Capitan and can’t reproduce this issue.
>
> To check which Java is running just use java -version  
> You can try using an explicit version of Java by using a full path e.g.:
>
> /Library/Java/JavaVirtualMachines/jdk1.8.0_131.jdk/Contents/Home/bin/java
>
> or
>
> /Library/Java/JavaVirtualMachines/jdk1.8.0_91.jdk/Contents/Home/bin/java
>



### **Jean Naude** — May 9, 2017 at 7:11 am ([permalink](https://www.codenameone.com/blog/using-the-new-gui-builder.html#comment-23576))

> Jean Naude says:
>
> Using the explicit version jdk 1.8.0_91 works form the command line, but gives me the same error (dialog in first post) when I try to run the guibuilder from the IDE.
>



### **Shai Almog** — May 10, 2017 at 5:44 am ([permalink](https://www.codenameone.com/blog/using-the-new-gui-builder.html#comment-21585))

> Shai Almog says:
>
> This seems to be a regression in the new JDK. Try setting the JDK of the netbeans project to point at the older 91 version as a workaround. If this fails try changing the JDK netbeans itself runs on to the older one. Alternatively updating the OS should also resolve this.
>



### **Ch Hjelm** — July 7, 2017 at 8:29 pm ([permalink](https://www.codenameone.com/blog/using-the-new-gui-builder.html#comment-21852))

> Ch Hjelm says:
>
> How do you remove/delete a GUI Builder file from a Netbeans project? I tried creating one to try out the new GUI Builder but it has compilation errors (“error: illegal character: ‘u00b4′” which I cannot see in the GUI builder) and when I delete the generated Java file it just gets recreated every time I build the application.
>



### **Shai Almog** — July 8, 2017 at 5:31 am ([permalink](https://www.codenameone.com/blog/using-the-new-gui-builder.html#comment-23612))

> Shai Almog says:
>
> Under the res/guibuilder directory you should see a hierarchy containing a .gui file. Which version of the GUI builder did you use (it’s in the about screen) and how did you create that file? The .gui and Java file would be helpful in an issue assuming it’s 3.7+.
>
> Thanks.
>



### **Ch Hjelm** — July 8, 2017 at 6:21 am ([permalink](https://www.codenameone.com/blog/using-the-new-gui-builder.html#comment-23676))

> Ch Hjelm says:
>
> Thanks, I wasn’t aware of the res/guibuilder directory. I used 3.7.1, but the file was created a few weeks ago, using Netbeans New -> Codename One -> Gui Builder Form. Since this was blocking compilation, and was just an experimental file, I simply deleted the content in the Gui Builder so I could compile. Maybe not a realistic option, but if you could store the .gui file’s XML content directly in the Java file, it would be simpler/more intuitive to delete. Alternatively, add a Delete file button in the Gui Builder?
>



### **Shawn Ikope** — August 22, 2017 at 11:57 pm ([permalink](https://www.codenameone.com/blog/using-the-new-gui-builder.html#comment-24142))

> Shawn Ikope says:
>
> my gui builder keeps telling me that I cannot change the layout. How do I remove the Auto layout?
>



### **Shai Almog** — August 23, 2017 at 6:13 am ([permalink](https://www.codenameone.com/blog/using-the-new-gui-builder.html#comment-24143))

> Shai Almog says:
>
> I suggest you check out this post, the new mode is far superior: [https://www.codenameone.com…](<https://www.codenameone.com/blog/gui-builder-improvements-3.7.html>)
>
> You can just create a new GUI builder file and uncheck autolayout if you want.
>



### **Jill M** — February 13, 2018 at 5:07 am ([permalink](https://www.codenameone.com/blog/using-the-new-gui-builder.html#comment-23778))

> Jill M says:
>
> I would like to change my layout; however, when I try to click on another layout I get an error message that says “Auto layout mode currently on. The root layout manager must be Layered Layout.” How can I remove auto layout without having to start all over. I read the article below, but I did not find the answer I was looking for. Please help 🙂
>



### **Shai Almog** — February 13, 2018 at 5:20 am ([permalink](https://www.codenameone.com/blog/using-the-new-gui-builder.html#comment-23597))

> Shai Almog says:
>
> In version 3.8 we added a new auto layout mode which is now the default. It should make it much easier to build a UI without changing the layout. Steve discussed this in depth here: [https://www.codenameone.com…](<https://www.codenameone.com/blog/tutorial-gui-builder-autolayout-signin-form-responsive.html>)
>



### **linnet maruve** — June 18, 2018 at 12:09 pm ([permalink](https://www.codenameone.com/blog/using-the-new-gui-builder.html#comment-23890))

> linnet maruve says:
>
> On Gui builder when l want to pick an icon there are no images from res files as well as the google and facebook links for buttons how do l add them. on res file there is no codenameone logo at all. l am using netbeans on windows 10 please help
>



### **Shai Almog** — June 19, 2018 at 5:13 am ([permalink](https://www.codenameone.com/blog/using-the-new-gui-builder.html#comment-23981))

> Shai Almog says:
>
> Does the project itself have the Codename One logo?  
> Is the plugin installed? What do you see when you right click the project/file? Can you post screenshots?
>



### **linnet maruve** — June 19, 2018 at 9:41 am ([permalink](https://www.codenameone.com/blog/using-the-new-gui-builder.html#comment-23775))

> linnet maruve says:
>
> yes it has a logo.the plugin is installed l downloaded it from netbeans org and installed it. when l right click it gives me many options there one to access codename one build… and settings
>



### **linnet maruve** — June 19, 2018 at 10:01 am ([permalink](https://www.codenameone.com/blog/using-the-new-gui-builder.html#comment-21591))

> linnet maruve says:
>
> [https://uploads.disquscdn.c…](<https://uploads.disquscdn.com/images/8d980c79724d4677703c1443e04ed1f1cc4263ccf2f9681a40ceba6dee1684f4.png>) [https://uploads.disquscdn.c…](<https://uploads.disquscdn.com/images/eaf49b1970961edfd7e37c0804982bfe27cf4621ac90b6d70f353c2c100ce470.png>)
>



### **Shai Almog** — June 20, 2018 at 4:13 am ([permalink](https://www.codenameone.com/blog/using-the-new-gui-builder.html#comment-23798))

> Shai Almog says:
>
> There is a logo on the theme.res where you can add images using the resource editor tool
>



### **linnet maruve** — June 20, 2018 at 6:59 am ([permalink](https://www.codenameone.com/blog/using-the-new-gui-builder.html#comment-23717))

> linnet maruve says:
>
> can l have the procedure of doing that
>



### **Shai Almog** — June 21, 2018 at 6:07 am ([permalink](https://www.codenameone.com/blog/using-the-new-gui-builder.html#comment-23735))

> Shai Almog says:
>
> Sure, it’s in the developer guide: [https://www.codenameone.com…](<https://www.codenameone.com/manual/theme-basics.html>) just open it Images -> Add
>



### **linnet maruve** — June 21, 2018 at 2:21 pm ([permalink](https://www.codenameone.com/blog/using-the-new-gui-builder.html#comment-23620))

> linnet maruve says:
>
> ok let me work on it will come back to you
>



### **Jack Dore** — August 22, 2018 at 1:25 pm ([permalink](https://www.codenameone.com/blog/using-the-new-gui-builder.html#comment-23971))

> Jack Dore says:
>
> How do I connect two GUIs with a button.
>



### **Shai Almog** — August 23, 2018 at 5:25 am ([permalink](https://www.codenameone.com/blog/using-the-new-gui-builder.html#comment-24072))

> Shai Almog says:
>
> Press the action listener button in the events tab. Then go to the code. In the code you should have a new callback call for the action event. Just write something like new OtherGUIBuilderForm().show();
>



### **Medo Boui** — September 12, 2018 at 3:59 pm ([permalink](https://www.codenameone.com/blog/using-the-new-gui-builder.html#comment-23776))

> Medo Boui says:
>
> imagine that i have a form and a container withing it; the container is created using the New Gui Builder,and there are some components on it, is there a way to call these components like do some sort of getButtonX() for example or findButtonX()? because i tried to define getters and setters and whenever i try to run it, the code i added is deleted automatically
>



### **Shai Almog** — September 13, 2018 at 10:45 am ([permalink](https://www.codenameone.com/blog/using-the-new-gui-builder.html#comment-23874))

> Shai Almog says:
>
> When you build the project the content within the comment block is regenerated. You can write getters and setters just don’t place them between these two comments… Make sure to save before compiling.
>



### **Medo Boui** — September 13, 2018 at 3:58 pm ([permalink](https://www.codenameone.com/blog/using-the-new-gui-builder.html#comment-23675))

> Medo Boui says:
>
> Thank you so much
>



### **tobi adegoroye** — October 26, 2019 at 6:49 pm ([permalink](https://www.codenameone.com/blog/using-the-new-gui-builder.html#comment-24262))

> [tobi adegoroye](https://lh3.googleusercontent.com/-BWeQERwJuuU/AAAAAAAAAAI/AAAAAAAAAAA/ACHi3rfX2FANvmhqEYMU-mwi7M8EXmS-ig/photo.jpg) says:
>
> Hi it would be helpful if you could add a video showing how to use the constraints in the new gui builder
>



### **Shai Almog** — October 27, 2019 at 2:19 am ([permalink](https://www.codenameone.com/blog/using-the-new-gui-builder.html#comment-24265))

> Shai Almog says:
>
> Hi,  
> see <https://www.codenameone.com/blog/tutorial-gui-builder-autolayout-signin-form-responsive.html>
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
