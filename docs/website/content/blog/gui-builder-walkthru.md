---
title: GUI Builder Walkthru
slug: gui-builder-walkthru
url: /blog/gui-builder-walkthru/
original_url: https://www.codenameone.com/blog/gui-builder-walkthru.html
aliases:
- /blog/gui-builder-walkthru.html
date: '2015-10-13'
author: Shai Almog
---

![Header Image](/blog/gui-builder-walkthru/new-gui-builder-preview.png)

Since we announced the new GUI builder work we got quite a few questions in the discussion forum and offline  
so I prepared a quick video showing how the new GUI builder will look when released (more or less). Notice that  
a lot of things will change with the GUI builder but some things are pretty much fixed such as the basic architecture  
with the XML to Java process. This is unlikely to change much. 

Some developers asked for samples of the XML and Java code the new GUI builder generates.  
For your convenience this is the code for the demo above specifically the XML gui file is: 
    
    
    <?xml version="1.0" encoding="UTF-8"?>
    
    <component type="Form" layout="BoxLayout" boxLayoutAxis="Y"  title="TestForm" name="TestForm">
      <component type="TextField" text="TextField" name="Text_Field_1">
      </component>
      <component type="Label" text="Label" name="Label_1">
      </component>
      <component type="Container" layout="FlowLayout" flowLayoutFillRows="false" flowLayoutAlign="1" flowLayoutValign="0"  name="Container_1">
        <component type="Button" text="This Button" name="Button_2">
        </component>
      </component>
      <component type="Button" text="Hi World" name="Button_1" actionEvent="true">
      </component>
    </component>
    

The Java code to match that is: 
    
    
    public class TestForm extends com.codename1.ui.Form {
        public TestForm() {
            this(com.codename1.ui.util.Resources.getGlobalResources());
        }
        
        public TestForm(com.codename1.ui.util.Resources resourceObjectInstance) {
            initGuiBuilderComponents(resourceObjectInstance);
        }
    
    //-- DON'T EDIT BELOW THIS LINE!!!
        private com.codename1.ui.TextField gui_Text_Field_1 = new com.codename1.ui.TextField();
        private com.codename1.ui.Label gui_Label_1 = new com.codename1.ui.Label();
        private com.codename1.ui.Container gui_Container_1 = new com.codename1.ui.Container(new com.codename1.ui.layouts.FlowLayout());
        private com.codename1.ui.Button gui_Button_2 = new com.codename1.ui.Button();
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
                if(ev.getSource() == gui_Button_1) {
                    onButton_1ActionEvent(ev);
                }
            }
    
            public void dataChanged(int type, int index) {
            }
        }
        private void initGuiBuilderComponents(com.codename1.ui.util.Resources resourceObjectInstance) {
            guiBuilderBindComponentListeners();
            setLayout(new com.codename1.ui.layouts.BoxLayout(com.codename1.ui.layouts.BoxLayout.Y_AXIS));
            setTitle("TestForm");
            setName("TestForm");
            addComponent(gui_Text_Field_1);
            addComponent(gui_Label_1);
            addComponent(gui_Container_1);
            gui_Container_1.setName("Container_1");
            gui_Container_1.addComponent(gui_Button_2);
            gui_Button_2.setText("This Button");
            gui_Button_2.setName("Button_2");
            addComponent(gui_Button_1);
            gui_Text_Field_1.setText("TextField");
            gui_Text_Field_1.setName("Text_Field_1");
            gui_Label_1.setText("Label");
            gui_Label_1.setName("Label_1");
            gui_Container_1.setName("Container_1");
            gui_Button_1.setText("Hi World");
            gui_Button_1.setName("Button_1");
        }// </editor-fold>
    
    //-- DON'T EDIT ABOVE THIS LINE!!!
        public void onButton_1ActionEvent(com.codename1.ui.events.ActionEvent ev) {
            Dialog.show("Hi", "Hi world: " + gui_Text_Field_1.getText(), "OK", null);
        }
    }
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Clement Levallois** — October 15, 2015 at 6:07 am ([permalink](https://www.codenameone.com/blog/gui-builder-walkthru.html#comment-21486))

> Clement Levallois says:
>
> hello,  
> great stuff as usual. May I suggest that the workflow allows for a “all without coding” route? Your demo showed that 2 lines of code had to be added when a new “GUi form” was created, in order to make it displayed. A tick box in the wizard at the moment of Form creation could do it.  
> Essentially, it is key to allow beginners to use the designer much before they have to write even one line of code, as is the case now.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fgui-builder-walkthru.html)


### **Shai Almog** — October 15, 2015 at 6:39 am ([permalink](https://www.codenameone.com/blog/gui-builder-walkthru.html#comment-22502))

> Shai Almog says:
>
> Good point. We should have some better project templates, ideally with ready made projects that already include some forms.  
> We did that with the old designer (e.g. with the Tabs based application) but it wasn’t used much so we need to get more refined use cases for this.
>
> Right now I’d like to stabilize the designer so we can start building on top of it both tutorials and things such as this. At the moment this is somewhat of a regression for a “no coding” approach since some things like navigating from one form to the next aren’t supported in the new GUI builder. Naturally, we want to fix that.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fgui-builder-walkthru.html)


### **mtran** — October 16, 2015 at 2:49 pm ([permalink](https://www.codenameone.com/blog/gui-builder-walkthru.html#comment-21479))

> mtran says:
>
> Where can I download the Gui Buider ?
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fgui-builder-walkthru.html)


### **Shai Almog** — October 17, 2015 at 4:31 am ([permalink](https://www.codenameone.com/blog/gui-builder-walkthru.html#comment-22142))

> Shai Almog says:
>
> It will be a part of the next plugin update early next week.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fgui-builder-walkthru.html)


### **Jeremy** — October 18, 2015 at 2:38 pm ([permalink](https://www.codenameone.com/blog/gui-builder-walkthru.html#comment-24193))

> Jeremy says:
>
> Good job, but I think it would even be better if the Gui builder is designed such that it is possible to switch between the drag and drop style and the XML layout.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fgui-builder-walkthru.html)


### **Shai Almog** — October 19, 2015 at 2:17 am ([permalink](https://www.codenameone.com/blog/gui-builder-walkthru.html#comment-22341))

> Shai Almog says:
>
> Thanks for the feedback.  
> As you can see above the XML syntax wasn’t designed for human consumption as much as it was designed for accuracy. So editing this will be less intuitive.  
> You can edit this XML thru NetBeans and also use it to find out what happened if something went wrong. I think its a better approach since NetBeans already has great XML editing capabilities.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fgui-builder-walkthru.html)


### **Chidiebere Okwudire** — October 29, 2015 at 3:48 pm ([permalink](https://www.codenameone.com/blog/gui-builder-walkthru.html#comment-22511))

> Chidiebere Okwudire says:
>
> Hi Shai,
>
> I like what I see in the video. One thing that’s still unclear to me is what will eventually happen to the navigation logic that is present in the current GUI builder. Can you say something about that? Basically, how is navigation envisaged to work in the new GUI builder *especially* for scenarios where some forms are completely handcoded whereas others are created using the GUI builder?
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fgui-builder-walkthru.html)


### **Shai Almog** — October 30, 2015 at 5:04 am ([permalink](https://www.codenameone.com/blog/gui-builder-walkthru.html#comment-22254))

> Shai Almog says:
>
> Hi,  
> in the new GUI builder each form/container lives in isolation which is similar to the way traditional Java GUI builders have always worked.  
> The migration wizard we just introduced generates a fake state machine that just moves you between form instances.
>
> Because of that the new GUI builder is more like a handcoded app than a GUI builder app.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fgui-builder-walkthru.html)


### **bledi** — October 31, 2015 at 4:52 pm ([permalink](https://www.codenameone.com/blog/gui-builder-walkthru.html#comment-22495))

> bledi says:
>
> Hello,  
> i just downloaded the last version of the netbenas plugin and there seems not te be a new gui builder. When are you planning to relase the new one, as ti seems very promissing. Thank you
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fgui-builder-walkthru.html)


### **Shai Almog** — November 1, 2015 at 4:27 am ([permalink](https://www.codenameone.com/blog/gui-builder-walkthru.html#comment-22437))

> Shai Almog says:
>
> Hi,  
> Where did you download it from and what’s the version. 3.2.2 is the latest version of the NetBeans plugin.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fgui-builder-walkthru.html)


### **bledi** — November 1, 2015 at 11:01 am ([permalink](https://www.codenameone.com/blog/gui-builder-walkthru.html#comment-21487))

> bledi says:
>
> I installed again from the netbeans plugins and now i have 3.2.2. On all newly created project i cannot add events to buttons etc. On already existing projects it works as before. There is a thread in the google group for this also.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fgui-builder-walkthru.html)


### **Dan** — November 5, 2015 at 3:16 am ([permalink](https://www.codenameone.com/blog/gui-builder-walkthru.html#comment-22267))

> Dan says:
>
> Thanks for this overview, Shai – and thanks for including it in the latest CodenameOne release. Downloaded it today and have started playing with it. Thanks for the tip about the XML file – for some reason, dragging and dropping a textfield (for me at least) kept it as type “Label”. so if i go into the XML and change the type to “TextField” it seems to come out ok. Keep up the good work! – dan
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fgui-builder-walkthru.html)


### **Shai Almog** — November 6, 2015 at 4:26 am ([permalink](https://www.codenameone.com/blog/gui-builder-walkthru.html#comment-22438))

> Shai Almog says:
>
> Hi,  
> thanks for the feedback. We are making heavy changes to the tool as we speak to get it to alpha level. Our focus in the technology preview was on the concept and the XML process with back and forth communication to the IDE.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fgui-builder-walkthru.html)


### **Yaakov Gesher** — November 8, 2015 at 7:28 am ([permalink](https://www.codenameone.com/blog/gui-builder-walkthru.html#comment-22530))

> Yaakov Gesher says:
>
> Hi, I just converted an existing project using the migration wizard, and in all the generated files the NetBeans shows me compilation errors: Cannot find symbol ‘setGlobalResources’ in class ‘Resources’. After refreshing project libraries, the errors all went away. Just in case anyone else runs into the same issue!
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fgui-builder-walkthru.html)

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
