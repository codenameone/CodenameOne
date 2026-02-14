---
title: 'TIP: Auto Complete Renderer'
slug: tip-auto-complete-renderer
url: /blog/tip-auto-complete-renderer/
original_url: https://www.codenameone.com/blog/tip-auto-complete-renderer.html
aliases:
- /blog/tip-auto-complete-renderer.html
date: '2017-05-14'
author: Shai Almog
---

![Header Image](/blog/tip-auto-complete-renderer/tip.jpg)

I’m a bit conflicted about this tip. The `AutoCompleteTextField` is a problematic class that is in dire need of a rewrite. When we created it we still didn’t accept that [lists “need to go”](/blog/avoiding-lists.html). It also predated features like the `InteractionDialog` which would have made this component much easier to use.

**Check out a live preview of the demo on the right here thanks to our JavaScript port!**

However, we have other priorities and rewriting `AutoCompleteTextField` isn’t even in the top 100…​ So we need to still live with it and with the renderer that it exposes.

One question I got a few times is “How do you customize the results of the auto complete field”?

This sounds difficult to most people as we can only work with Strings so how do we represent additional data or format the date correctly?

The answer is actually pretty simple, we still need to work with Strings because auto-complete is first and foremost a text field. However, that doesn’t preclude our custom renderer from fetching data that might be placed in a different location and associated with the result.

The source below is annotated with a few comments below and you can see the “full repository” (which isn’t much more than that) [in github](https://github.com/codenameone/AutoCompleteWithImages).
    
    
    final String[] characters = { "Tyrion Lannister", "Jaime Lannister", "Cersei Lannister", "Daenerys Targaryen",
        "Jon Snow", "Petyr Baelish", "Jorah Mormont", "Sansa Stark", "Arya Stark", "Theon Greyjoy"
        // snipped the rest for clarity
    };
    
    Form current = new Form("AutoComplete", BoxLayout.y());
    
    AutoCompleteTextField ac = new AutoCompleteTextField(characters);
    
    final int size = Display.getInstance().convertToPixels(7);
    final EncodedImage placeholder = EncodedImage.createFromImage(Image.createImage(size, size, 0xffcccccc), true);
    
    final String[] actors = { "Peter Dinklage", "Nikolaj Coster-Waldau", "Lena Headey"}; __**(1)**
    final Image[] pictures = {
        URLImage.createToStorage(placeholder, "tyrion","http://i.lv3.hbo.com/assets/images/series/game-of-thrones/character/s5/tyrion-lannister-512x512.jpg"),
        URLImage.createToStorage(placeholder, "jaime","http://i.lv3.hbo.com/assets/images/series/game-of-thrones/character/s5/jamie-lannister-512x512.jpg"),
        URLImage.createToStorage(placeholder, "cersei","http://i.lv3.hbo.com/assets/images/series/game-of-thrones/character/s5/cersei-lannister-512x512.jpg")
    };
    
    ac.setCompletionRenderer(new ListCellRenderer() {
        private final Label focus = new Label(); __**(2)**
        private final Label line1 = new Label(characters[0]);
        private final Label line2 = new Label(actors[0]);
        private final Label icon = new Label(pictures[0]);
        private final Container selection = BorderLayout.center(
                BoxLayout.encloseY(line1, line2)).add(BorderLayout.EAST, icon);
    
        @Override
        public Component getListCellRendererComponent(com.codename1.ui.List list, Object value, int index, boolean isSelected) {
            for(int iter = 0 ; iter < characters.length ; iter++) {
                if(characters[iter].equals(value)) {
                    line1.setText(characters[iter]);
                    if(actors.length > iter) {
                        line2.setText(actors[iter]);
                        icon.setIcon(pictures[iter]);
                    } else {
                        line2.setText(""); __**(3)**
                        icon.setIcon(placeholder);
                    }
                    break;
                }
            }
            return selection;
        }
    
        @Override
        public Component getListFocusComponent(com.codename1.ui.List list) {
            return focus;
        }
    });
    current.add(ac);
    
    current.show();

__**1** | I have duplicate arrays that are only partial (I was lazy) this is a separate list of data element but you can fetch the additional data from anywhere  
---|---  
__**2** | I create the renderer UI instantly in the fields with the helper methods for wrapping elements which is pretty cool & terse  
__**3** | In a renderer it’s important to always set the value especially if you don’t have a value in place  
  
![Auto complete with images](/blog/tip-auto-complete-renderer/auto-complete-with-pictures.png)

Figure 1. Auto complete with images
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **davidwaf** — January 15, 2018 at 2:45 pm ([permalink](https://www.codenameone.com/blog/tip-auto-complete-renderer.html#comment-24221))

> Months later, this came in handy. Would be great to know how to implement the list-less version.
>



### **Shai Almog** — January 16, 2018 at 6:47 am ([permalink](https://www.codenameone.com/blog/tip-auto-complete-renderer.html#comment-23697))

> Shai Almog says:
>
> We’ll need to write a new component to implement that but technically it should be pretty easy to do similarly to some of the options highlighted in [https://www.codenameone.com…](<https://www.codenameone.com/blog/tip-dont-use-combobox.html>)
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
