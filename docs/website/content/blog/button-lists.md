---
title: Button Lists
slug: button-lists
url: /blog/button-lists/
original_url: https://www.codenameone.com/blog/button-lists.html
aliases:
- /blog/button-lists.html
date: '2018-12-04'
author: Shai Almog
---

![Header Image](/blog/button-lists/new-features-4.jpg)

I wrote in the past about the [problems in the List class](/blog/avoiding-lists.html), so I won’t rehash them here. However, the ideas behind list are still valuable. One such idea is the list model which allows us to separate the state from the view. Unfortunately the renderer architecture makes this hard to implement for most developers and limits the flexibility of the API.

To leverage this idea with the easier to use layout/container hierarchy we introduced `ButtonList` and its subclasses: `CheckBoxList`, `RadioButtonList` & `SwitchList`.

We also added a new `MultipleSelectionListModel` which extends the `ListModel` with multi-selection capabilities. This allows us to use a set of buttons as we would use a list and also generate them from data more easily.

In the code below we show two lists that use the same model. Changes to the checkboxes reflect instantly in the switch and vice versa:
    
    
    Form hi = new Form("Button Lists", new BorderLayout());
    SwitchList switchList = new SwitchList(new DefaultListModel("Red", "Green", "Blue", "Indigo"));
    switchList.addActionListener(e->{
        Log.p("Selected indices: "+Arrays.toString(switchList.getMultiListModel().getSelectedIndices()));
    }); __**(1)**
    switchList.setScrollableY(true);
    Button clearSelections = new Button("Clear");
    clearSelections.addActionListener(e ->
        switchList.getMultiListModel().setSelectedIndices());
    
    Button addOption = new Button("Add Option");
    addOption.addActionListener(e -> { __**(2)**
        callSerially(()->{
            TextField val = new TextField();
            Command res = Dialog.show("Enter label", val, new Command("OK"));
            switchList.getMultiListModel().addItem(val.getText());
    
        });
    });
    RadioButtonList layoutSelector = new RadioButtonList(new DefaultListModel("Flow", "X", "Y", "2-Col Table", "3-Col Table", "2 Col Grid", "3 Col Grid"));
    layoutSelector.addActionListener(e->{ __**(3)**
        boolean yScroll = true;
        switch (layoutSelector.getModel().getSelectedIndex()) {
            case 0:
                switchList.setLayout(new FlowLayout());
                break;
            case 1:
                switchList.setLayout(BoxLayout.x());
                yScroll = false;
                break;
            case 2:
                switchList.setLayout(BoxLayout.y());
                break;
            case 3:
                switchList.setLayout(new TableLayout(switchList.getComponentCount()/2+1, 2));
                break;
            case 4:
                switchList.setLayout(new TableLayout(switchList.getComponentCount()/3+1, 3));
                break;
            case 5:
                switchList.setLayout(new GridLayout(2));
                break;
            case 6:
                switchList.setLayout(new GridLayout(3));
        }
        switchList.setScrollableX(!yScroll);
        switchList.setScrollableY(yScroll);
        switchList.animateLayout(300);
    });
    CheckBoxList checkBoxList = new CheckBoxList(switchList.getMultiListModel()); __**(4)**
    checkBoxList.addActionListener(e->
        System.out.println("CheckBox actionEvent.  "+Arrays.toString(checkBoxList.getMultiListModel().getSelectedIndices())));
    hi.add(BorderLayout.NORTH, layoutSelector);
    hi.add(BorderLayout.CENTER, BoxLayout.encloseY(checkBoxList, switchList));
    hi.add(BorderLayout.SOUTH, GridLayout.encloseIn(2, addOption, clearSelections));
    hi.show();

__**1** | Instead of binding multiple listeners we can bind a listener to the list itself and get the selections  
---|---  
__**2** | It’s easy to add options like clear selection and add option thanks to the structure of the list model  
__**3** | This list uses a layout as it’s in-effect just a `Container`  
__**4** | The `CheckBoxList` uses the same model as the `SwitchList`  
  
![Demo of the button list](/blog/button-lists/button-list.png)

Figure 1. Demo of the button list classes
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Francesco Galgani** — December 8, 2018 at 11:14 am ([permalink](/blog/button-lists/#comment-24102))

> Great! However, in this example, there is an issue: the “Add Option” works fine with iOS skin, but it doesn’t work as expected on Android skin (because it’s necessary to change the layout to see the new added Button). I done tests with “[iPhoneX.skin](<http://iPhoneX.skin>)” and “[GooglePixel2.skin](<http://GooglePixel2.skin>)”
>



### **Francesco Galgani** — December 8, 2018 at 11:14 am ([permalink](/blog/button-lists/#comment-23875))

> Francesco Galgani says:
>
> Why do you use the callSerially in the ActionListener of the addOption button?
>



### **Francesco Galgani** — December 8, 2018 at 11:14 am ([permalink](/blog/button-lists/#comment-21539))

> Francesco Galgani says:
>
> To create the DefaultListModel, you used a list of Strings as args. Is it possible to bind the DefaultListModel with a list of BooleanProperty as args? It could be an interesting enhancement to seamlessly map the user selections in a ButtonList with a PropertyBusinessObject.
>



### **Shai Almog** — December 8, 2018 at 1:46 pm ([permalink](/blog/button-lists/#comment-24105))

> Shai Almog says:
>
> The strings map to the labels matching the entries, the models map to the selection which is an integer or set of integers. A boolean would be a problem as you can’t get the labels from the boolean values. However, it would probably be easy to map this to boolean property objects as the model selection logic can be a great place to do that.
>
> I use the callSerially because of the [Dialog.show](<http://Dialog.show)(>) that follows. It allows the event queue to flush before we block it.
>
> There is an issue which wasn’t noticeable to me because of the different animations for the dialog showing. We need a hi.revalidate() in the end of the callSerially invocation.
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
