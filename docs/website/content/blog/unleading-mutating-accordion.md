---
title: Unleading & Mutating Accordion
slug: unleading-mutating-accordion
url: /blog/unleading-mutating-accordion/
original_url: https://www.codenameone.com/blog/unleading-mutating-accordion.html
aliases:
- /blog/unleading-mutating-accordion.html
date: '2016-06-25'
author: Shai Almog
---

![Header Image](/blog/unleading-mutating-accordion/accordion-post.png)

We covered the [new Accordion component last week](/blog/accordion-component.html) and just started using  
it in a demo for which it was very suitable. As we were working with it we discovered that it was missing some  
core methods to remove an `Accordion` entry or change it’s title. But worse, was the fact that a delete button in  
the title wouldn’t work!  
The crux of the issue is in the fact that  
[lead component](https://www.codenameone.com/manual/misc-features.html#lead-component-section)  
doesn’t support excluding a specific component within the hierarchy from it’s behavior so we set about to fix that…​

We added two new methods to the `Component` class: `setBlockLead(boolean)` & `isBlockLead()`.

Effectively when you have a `Component` within the lead hierarchy that you would like to treat differently from the  
rest you can use this method to exclude it from the lead component behavior while keeping the rest in line…​

This should have no effect if the component isn’t a part of a lead component.

Other than that we also added to `Accordion` some methods that allow us to mutate it’s state:  
<https://www.codenameone.com/javadoc/com/codename1/components/Accordion.html#removeContent-com.codename1.ui.Component-> [removeContent]  
& two variations of  
[setHeader](https://www.codenameone.com/javadoc/com/codename1/components/Accordion.html#setHeader-com.codename1.ui.Component-com.codename1.ui.Component-).

__ |  If you use a `Component` as the header you will need to use the version of the method that accepts a  
component and not the version that accepts a `String` otherwise you will get an exception…​   
---|---  
  
With these new API’s we can now add mutable content. The following demo allows us to add/remove elements  
and as we change them we can see the title updating. We can also remove elements from the accordion by  
pressing the delete button, however pressing in any other place will just expand/collapse the accordion.

Check out the live demo powered by the JavaScript port here on the right, you can also see the source  
below or the full project (which doesn’t include much more)  
[here](https://github.com/codenameone/AccordionDemo).
    
    
    public void start() {
        if(current != null){
            current.show();
            return;
        }
        Form f = new Form("Accordion", new BorderLayout());
        Accordion accr = new Accordion();
        f.getToolbar().addMaterialCommandToRightBar("", FontImage.MATERIAL_ADD, e -> addEntry(accr));
        addEntry(accr);
        f.add(BorderLayout.CENTER, accr);
        f.show();
    }
    
    void addEntry(Accordion accr) {
        TextArea t = new TextArea("New Entry");
        Button delete = new Button();
        FontImage.setMaterialIcon(delete, FontImage.MATERIAL_DELETE);
        Label title = new Label(t.getText());
        t.addActionListener(ee -> title.setText(t.getText()));
        delete.addActionListener(ee -> {
            accr.removeContent(t);
            accr.animateLayout(200);
        });
        delete.setBlockLead(true);
        delete.setUIID("Label");
        Container header = BorderLayout.center(title).
                add(BorderLayout.EAST, delete);
        accr.addContent(header, t);
        accr.animateLayout(200);
    }

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
