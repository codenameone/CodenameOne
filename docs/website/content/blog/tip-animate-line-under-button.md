---
title: 'TIP: Animate Line Under Button'
slug: tip-animate-line-under-button
url: /blog/tip-animate-line-under-button/
original_url: https://www.codenameone.com/blog/tip-animate-line-under-button.html
aliases:
- /blog/tip-animate-line-under-button.html
date: '2017-07-16'
author: Shai Almog
---

![Header Image](/blog/tip-animate-line-under-button/tip.jpg)

I got a question the other day about a UI design for an upcoming Codename One app. In this UI the title floated on top of the content as the content scrolled past and tabs were a part of the title area in the style of material design.   
Our tabs component predated material design by many years and wasn’t designed for this sort of UI. When you scroll out of a tab it scrolls out. Using it for this sort of UI doesn’t make much sense. The solution was to use a custom title area with two toggle radio buttons representing each tab with a line below to indicate the selected button.

That’s pretty easy to do in Codename One, we can just style a toggle button with the right colors or border but the question is: “How do we animate the line motion”.

Well…​ That’s actually really easy to accomplish once we decide the line isn’t a part of the button…​

This is a quick demo I made using maps and a floating toolbar and here is the annotated code:
    
    
    public void init(Object context) {
        theme = UIManager.initFirstTheme("/theme");
    
        // Enable Toolbar on all Forms by default __**(1)**
        //Toolbar.setGlobalToolbar(true);
    }
    
    public void start() {
        if(current != null){
            current.show();
            return;
        }
        Form m = new Form("Map", new BorderLayout());
        Toolbar tb = new Toolbar(true); __**(2)**
        m.setToolbar(tb);
        m.add(BorderLayout.CENTER, new MapContainer("... PLACE A KEY HERE ...")); __**(3)**
    
        Label title = new Label("Map", "Title");
    
        ButtonGroup bg = new ButtonGroup();
        RadioButton modeA = RadioButton.createToggle("Mode A", bg); __**(4)**
        RadioButton modeB = RadioButton.createToggle("Mode B", bg);
        RadioButton modeC = RadioButton.createToggle("Mode C", bg);
        modeA.setUIID("SmallTitle");
        modeB.setUIID("SmallTitle");
        modeC.setUIID("SmallTitle");
        modeA.setSelected(true);
        Container radioGrid = GridLayout.encloseIn(3, modeA, modeB, modeC);
    
        Label whiteLine = new Label(); __**(5)**
        whiteLine.setShowEvenIfBlank(true);
        whiteLine.getUnselectedStyle().setBgColor(0xffffff);
        whiteLine.getUnselectedStyle().setBgTransparency(255);
        whiteLine.getUnselectedStyle().setPaddingUnit(Style.UNIT_TYPE_DIPS);
        whiteLine.getUnselectedStyle().setPadding(1, 0, 1, 1);
        Container lineGrid = GridLayout.encloseIn(3, whiteLine, new Label(), new Label()); __**(6)**
        bg.addActionListener(e -> { __**(7)**
            int offset = radioGrid.getComponentIndex(e.getComponent());
            whiteLine.remove();
            lineGrid.addComponent(offset, whiteLine);
            lineGrid.animateLayout(150);
        });
    
        Container titleArea = BoxLayout.encloseY(title, radioGrid, lineGrid); __**(8)**
        tb.setTitleComponent(titleArea);
    
        m.show();
    }

__**1** | I commented out the toolbar as I will use an overlay toolbar on top of the map  
---|---  
__**2** | When creating a toolbar instance with `true`, it “floats” on top of the UI notice the transparency to the underlying map  
__**3** | The API key is required if you want to use the JavaScript version  
__**4** | The radio toggle buttons are pretty standard, I styled them to look the same in all modes  
__**5** | I could have used the style to do the “white line” but chose to do it in code. It’s just a white background padding  
__**6** | Both the button and the white line are in 1×3 grids which means the elements will inherently have the same size  
__**7** | I listen on the button group and when a radio is selected I just move the white line to the selected radio offset. Then animate the layout  
__**8** | Box Y gives components the same width so the grids both get the same width and will thus match in size  
  
Check out the full code on github here: <https://github.com/codenameone/MapAnimateTitle/>

### Learn More

It’s small things like that and attention to detail that can transform your app into a loved app. If you See similar effects or don’t fully understand why the code above works just ask in the comments!

We’re here to help you make great looking apps.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **bryan** — July 18, 2017 at 12:00 am ([permalink](/blog/tip-animate-line-under-button/#comment-24209))

> bryan says:
>
> That’s pretty clever. Very neat.
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
