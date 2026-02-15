---
title: 'TIP: Don''t Use ComboBox'
slug: tip-dont-use-combobox
url: /blog/tip-dont-use-combobox/
original_url: https://www.codenameone.com/blog/tip-dont-use-combobox.html
aliases:
- /blog/tip-dont-use-combobox.html
date: '2016-10-16'
author: Shai Almog
---

![Header Image](/blog/tip-dont-use-combobox/just-the-tip.jpg)

We previously [discussed the problems with List](/blog/avoiding-lists.html) and somewhat neglected  
`ComboBox` which is a subclass of `List`. `ComboBox` has the dubious “honor” of deriving most of the problems  
`List` has and adding a slew of its own problems such as two separate renderers, different behaviors between OS’s  
etc.

In addition mobile OS’s don’t really have a `ComboBox` in their native UI arsenal. E.g. iOS has no native  
`ComboBox` support. When web code has a select entry that needs to show a `ComboBox` Safari shows a UI  
specific to it and launches a spinner for interaction.

`ComboBox` was pretty common before iOS and it’s a component we had since 2006 but it’s time to move forward  
and here are better alternatives that should support every single use case…​

### Picker

Picker doesn’t look good in the simulator and has its issues but on the device it is mapped to a native device  
value picker. If you need to pick a Time, Date or String then picker is by far the best alternative out there.

__ |  Picker has a time & date option that only works natively on iOS…​   
---|---  
  
The cool thing about picker is that it uses native UI for time/date which allow things like a clock face on Android  
when picking the time…​

However, I’m guessing that with the case of replacing the ComboBox you would want a String `Picker` which  
works like this:
    
    
    String[] characters = { "Tyrion Lannister", "Jaime Lannister", "Cersei Lannister", "Daenerys Targaryen",
        "Jon Snow" /* cropped */
    };
    
    Form hi = new Form("Picker");
    Picker p = new Picker();
    p.setStrings(characters);
    p.setSelectedString(characters[0]);
    p.addActionListener(e -> ToastBar.showMessage("You picked " + p.getSelectedString(), FontImage.MATERIAL_INFO));
    hi.add(p);
    hi.show();

### AutoCompleteTextField

The `AutoCompleteTextField` is probably the best alternative if you are picking a String we discussed this in some  
details in the [blog post here](/blog/dynamic-autocomplete.html). But this is only a partial replacement to the  
combo box.

We can combine an auto complete with a button to produce an editable ComboBox that is just amazing e.g.  
check out this code:
    
    
    String[] characters = { "Tyrion Lannister", "Jaime Lannister", "Cersei Lannister", "Daenerys Targaryen",
        "Jon Snow" /* cropped */
    };
    
    Form hi = new Form("Picker");
    AutoCompleteTextField act = new AutoCompleteTextField(characters);
    act.addActionListener(e -> ToastBar.showMessage("You picked " + act.getText(), FontImage.MATERIAL_INFO));
    Button down = new Button();
    FontImage.setMaterialIcon(down, FontImage.MATERIAL_KEYBOARD_ARROW_DOWN);
    hi.add(
            BorderLayout.center(act).
                    add(BorderLayout.EAST, down));
    down.addActionListener(e -> act.showPopup());
    hi.show();

![AutoCompleteTextField as editable combo box](/blog/tip-dont-use-combobox/autocomplete-editable-combobox.png)

Figure 1. AutoCompleteTextField as editable combo box

This component is one of my favorites as it allows for complex searches and the typical combo box use cases.

### Button (or MultiButton etc.) & Popup

The last one seems like “hard work” but when you look at the alternative of building a cell renderer for a  
`ComboBox` this is actually really easy and more powerful…​

Instead of using a ComboBox just use a button, you can style it to look like a `ComboBox` and then show a  
`Dialog` as a popup so it will appear next to the `Button`.

E.g. the code in the demo below has some complexities but they are far simpler than the complexities of building  
a renderer:
    
    
    Form hi = new Form("Button", BoxLayout.y());
    
    String[] characters = { "Tyrion Lannister", "Jaime Lannister", "Cersei Lannister"};
    String[] actors = { "Peter Dinklage", "Nikolaj Coster-Waldau", "Lena Headey"};
    int size = Display.getInstance().convertToPixels(7);
    EncodedImage placeholder = EncodedImage.createFromImage(Image.createImage(size, size, 0xffcccccc), true);
    Image[] pictures = {
        URLImage.createToStorage(placeholder, "tyrion","http://i.lv3.hbo.com/assets/images/series/game-of-thrones/character/s5/tyrion-lannister-512x512.jpg"),
        URLImage.createToStorage(placeholder, "jaime","http://i.lv3.hbo.com/assets/images/series/game-of-thrones/character/s5/jamie-lannister-512x512.jpg"),
        URLImage.createToStorage(placeholder, "cersei","http://i.lv3.hbo.com/assets/images/series/game-of-thrones/character/s5/cersei-lannister-512x512.jpg")
    };
    
    MultiButton b = new MultiButton("Pick A Lanister...");
    b.addActionListener(e -> {
        Dialog d = new Dialog();
        d.setLayout(BoxLayout.y());
        d.getContentPane().setScrollableY(true);
        for(int iter = 0 ; iter < characters.length ; iter++) {
            MultiButton mb = new MultiButton(characters[iter]);
            mb.setTextLine2(actors[iter]);
            mb.setIcon(pictures[iter]);
            d.add(mb);
            mb.addActionListener(ee -> {
                b.setTextLine1(mb.getTextLine1());
                b.setTextLine2(mb.getTextLine2());
                b.setIcon(mb.getIcon());
                d.dispose();
                b.revalidate();
            });
        }
        d.showPopupDialog(b);
    });
    hi.add(b);
    hi.show();

![Pick a Lanister with the popup open](/blog/tip-dont-use-combobox/pick-a-lanister-1.png)

Figure 2. Pick a Lanister with the popup open

![I like Tyrion but this was Lena's season...](/blog/tip-dont-use-combobox/pick-a-lanister-2.png)

Figure 3. I like Tyrion but this was Lena’s season…​

__ |  I forgot to set the emblem character in the `MultiButton` to the down arrow which would have made it  
look like a combo box   
---|---  
  
### Finally

I think every single one of the options above is superior to `ComboBox` my intuition is telling me that we should  
deprecate it so people stop using it but we probably won’t remove it as it is still used a lot…​

What are your thoughts on the options listed above? Did I miss a use case for which ComboBox is essential?
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Ulises Escobar Aranda** — October 17, 2016 at 2:48 pm ([permalink](/blog/tip-dont-use-combobox/#comment-23042))

> The reason why I have not stopped using the combobox is because I use a list caption-value, with repeated titles. Is there any way that you can add this functionality to the Picker and also a getSelectedIndex() and setSelectedIndex() functions?
>



### **Shai Almog** — October 18, 2016 at 2:49 am ([permalink](/blog/tip-dont-use-combobox/#comment-22855))

> The picker is implemented natively so no, something like this will not be realistic in the picker. I think this falls squarely into the Button+Dialog territory
>



### **Tommy Mogaka** — October 18, 2016 at 1:39 pm ([permalink](/blog/tip-dont-use-combobox/#comment-21657))

> I agree… combo lists can be a nightmare and styling them is just the begining! Even so, I think it is still the best component for displaying a selection of large data in a way that the user can quickly choose what they want e.g. in the case of countries as shown below. The combo here is better be replaceable by an autocomplete textfield because the user already knows what s/he wants. Still, in the case that they don’t know what they want to select in advance, isn’t a combo way better?  
> combo_countries = new ComboBox();  
> try  
> {  
> Object rawStringCountries = Storage.getInstance().readObject(“countries.json”);  
> JSONObject j = new JSONObject(rawStringCountries.toString());  
> JSONArray Arr = (JSONArray) j.get(“data”);
>
> str_countries = new String[Arr.length()];  
> List<string> listOfString = new ArrayList<>();
>
> for (int i = 0; i < Arr.length(); i++)  
> {  
> String p = Arr.getString(i);  
> JSONObject obj_country = new JSONObject(p);  
> final String str_id = (String)obj_country .get(“id”);  
> final String str_title = (String)obj_country .get(“title”);  
> combo_countries.addItem(str_title);  
> }  
> combo_inchi.setIncludeSelectCancel(true);  
> }  
> catch (JSONException e)  
> {  
> e.printStackTrace();  
> }
>



### **Shai Almog** — October 19, 2016 at 8:20 am ([permalink](/blog/tip-dont-use-combobox/#comment-22996))

> Shai Almog says:
>
> You can use a hybrid approach of auto-complete coupled with the button dialog which would be superior although slightly more verbose.
>



### **Mo** — March 8, 2017 at 8:18 pm ([permalink](/blog/tip-dont-use-combobox/#comment-23129))

> Mo says:
>
> Hi, having just experimented with the above Button & Popup, but when I use the Validator, I am unable to get the same behavior as the ComboBox on an Invalid value, can you advice on how to use the Validator with the above approach??
>



### **Shai Almog** — March 9, 2017 at 7:29 am ([permalink](/blog/tip-dont-use-combobox/#comment-23152))

> Shai Almog says:
>
> Hi,  
> you will need to derive the validator and override getComponentValue() to return the right value for that component and super.getComponentValue() for the other components.
>



### **Mo** — March 9, 2017 at 2:31 pm ([permalink](/blog/tip-dont-use-combobox/#comment-23445))

> Mo says:
>
> Hi Shai, thank you for the reply and would appreciated if you could elaborate your hint above with an example?  
> The code below, is an example of how I am experimenting with Button and Popup technique, where MultiButtonBox is overriding the Button/MultiButton component!  
> `  
> langField = new MultiButtonBox(language, langMap);  
> validator.addConstraint(langField, new com.codename1.ui.validation.LengthConstraint(2, “errors.invalid”));//validate  
> `
>



### **Shai Almog** — March 10, 2017 at 8:47 am ([permalink](/blog/tip-dont-use-combobox/#comment-23454))

> Shai Almog says:
>
> Instead of doing new Validator(). Subclass it and override that method. That method retrieves the value of the component for validation, since your component is arbitrary (a button) the validator doesn’t know the actual value of the component and can’t figure out what to do in this case.
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
