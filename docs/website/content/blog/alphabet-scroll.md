---
title: Alphabet Scroll
slug: alphabet-scroll
url: /blog/alphabet-scroll/
original_url: https://www.codenameone.com/blog/alphabet-scroll.html
aliases:
- /blog/alphabet-scroll.html
date: '2016-07-17'
author: Shai Almog
---

![Header Image](/blog/alphabet-scroll/alphabet-scroll.png)

We got a lot of requests from developers over the years to do an iOS style alphabet side scroll. Some developers  
implements such scrolling but no one made it generic or contributed it back. So a recent stack overflow question  
got me thinking about how easy it would be to actually implement something like that and I decided to try…​

I ended up building this in 10 minutes and the concept is remarkably simple. I have two containers, one contains  
the list of the people and the other one contains the letters used for these people. Notice that I chose to only use  
letters that used in the names, I could have just hardcoded the English alphabet but chose to avoid that as this  
would break for internationalization and include letters that might not be common in such cases such as ‘Z’.

Thanks to the usage of layered layout the containers just appear on top of one another, notice that in the sample  
code below I had to cancel the default scrollability of the form to allow the two containers to have their own  
scrollability. The scrolling isn’t nested in this case since these are two separate containers that just happen  
to be one on top of the other.

We scroll to the selected component by finding it as we loop over the components in the actual container and  
using `scrollComponentToVisible` which is pretty convenient for this case.

**Check out the live demo on the right side thanks to the JavaScript port**

You can also check out the full project on github [here](https://github.com/codenameone/AlphabetScroll) and see  
the relevant code below:
    
    
    String[] characters = { "Tyrion Lannister", "Jaime Lannister", "Cersei Lannister", "Daenerys Targaryen",
        "Jon Snow", "Petyr Baelish", "Jorah Mormont", "Sansa Stark", "Arya Stark", "Theon Greyjoy",
        "Bran Stark", "Sandor Clegane", "Joffrey Baratheon", "Catelyn Stark", "Robb Stark", "Ned Stark",
        "Robert Baratheon", "Viserys Targaryen", "Varys", "Samwell Tarly", "Bronn","Tywin Lannister",
        "Shae", "Jeor Mormont","Gendry","Tommen Baratheon","Jaqen H'ghar","Khal Drogo","Davos Seaworth",
        "Melisandre","Margaery Tyrell","Stannis Baratheon","Ygritte","Talisa Stark","Brienne of Tarth","Gilly",
        "Roose Bolton","Tormund Giantsbane","Ramsay Bolton","Daario Naharis","Missandei","Ellaria Sand",
        "The High Sparrow","Grand Maester Pycelle","Loras Tyrell","Hodor","Gregor Clegane","Meryn Trant",
        "Alliser Thorne","Othell Yarwyck","Kevan Lannister","Lancel Lannister","Myrcella Baratheon",
        "Rickon Stark","Osha","Janos Slynt","Barristan Selmy","Maester Aemon","Grenn","Hot Pie",
        "Pypar","Rast","Ros","Rodrik Cassel","Maester Luwin","Irri","Doreah","Eddison Tollett","Podrick Payne",
        "Yara Greyjoy","Selyse Baratheon","Olenna Tyrell","Qyburn","Grey Worm","Meera Reed","Shireen Baratheon",
        "Jojen Reed","Mace Tyrell","Olly","The Waif","Bowen Marsh"
    };
    
    Toolbar.setGlobalToolbar(true);
    
    Form f = new Form("Letter Scroll", new LayeredLayout());
    
    f.setScrollable(false);
    Container characterContainer = new Container(BoxLayout.y());
    Container lettersContainer = new Container(BoxLayout.y());
    characterContainer.setScrollableY(true);
    lettersContainer.setScrollableY(true);
    
    char lastLetter = 0;
    Arrays.sort(characters, new CaseInsensitiveOrder());
    for(String character : characters) {
        MultiButton mb = new MultiButton(character);
        characterContainer.add(mb);
        char c = Character.toUpperCase(character.charAt(0));
        if(c != lastLetter) {
            lastLetter = c;
            Button btn = new Button("" + lastLetter);
            lettersContainer.add(btn);
            btn.getAllStyles().setPadding(0, 0, 0, 0);
            btn.getAllStyles().setMargin(0, 0, 0, 0);
            btn.addActionListener(e -> {
                for(Component cmp : characterContainer) {
                    MultiButton m = (MultiButton)cmp;
                    if(Character.toUpperCase(m.getTextLine1().charAt(0)) == c) {
                        characterContainer.scrollComponentToVisible(m);
                        return;
                    }
                }
            });
        }
    }
    
    f.add(characterContainer).
            add(BorderLayout.east(lettersContainer));
    
    f.show();
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Chidiebere Okwudire** — July 19, 2016 at 7:39 am ([permalink](https://www.codenameone.com/blog/alphabet-scroll.html#comment-22861))

> Chidiebere Okwudire says:
>
> Nice and short! Would it be possible to animate the alphabet list as happens on some phones. So pressing down on the list and scrolling causes the list to kind of bump out around the position?
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Falphabet-scroll.html)


### **Ross Taylor** — July 19, 2016 at 8:56 am ([permalink](https://www.codenameone.com/blog/alphabet-scroll.html#comment-22644))

> Ross Taylor says:
>
> Neat. However when I scroll the list, the title bar disappears and is turned into a blank space. Is this suppose to happen?
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Falphabet-scroll.html)


### **Chidiebere Okwudire** — July 19, 2016 at 1:29 pm ([permalink](https://www.codenameone.com/blog/alphabet-scroll.html#comment-22818))

> Chidiebere Okwudire says:
>
> One more thing: Is there a catalog of these handy features? Something as simple as an appendix in the user manual, for example, that refers to the corresponding blog posts. I don’t know about others but it happens quite often that I want to do something and I remember once reading about it but I’m not sure where and how to find the post quickly. It would be nice if there’s an overview and also make these handy utilties more visible to newcomers.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Falphabet-scroll.html)


### **Shai Almog** — July 20, 2016 at 4:20 am ([permalink](https://www.codenameone.com/blog/alphabet-scroll.html#comment-22580))

> Shai Almog says:
>
> You can just set the UIID of one of them to selected or even just increase the font or padding so this would be easy and should “just work”. The main challenge is detecting where you are in the scroll. When a user scrolls by pressing a button this should be pretty easy but when the user scrolls via touch this might be more challenging. You can use the scroll listener which should be pretty easy to work with and then hack something with getComponentAt(x, y) to find the location you are currently in.
>
> Another approach which I haven’t tested but might be more elegant is this:  
> After constructing and initializing the layout loop over the component and assign a Y range to every alphabet letter. getY() of a specific component should return the right scroll offset and this should work nicely with the scroll listener.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Falphabet-scroll.html)


### **Shai Almog** — July 20, 2016 at 4:20 am ([permalink](https://www.codenameone.com/blog/alphabet-scroll.html#comment-22733))

> Shai Almog says:
>
> That’s a bug in the JavaScript port, Steve just committed a fix for this.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Falphabet-scroll.html)


### **Shai Almog** — July 20, 2016 at 4:23 am ([permalink](https://www.codenameone.com/blog/alphabet-scroll.html#comment-22977))

> Shai Almog says:
>
> The problem with that is that someone needs to maintain it and if something doesn’t make it there then people assume it doesn’t exist. Sometimes it’s better off to have nothing than to have something that’s half done.
>
> The thing I’d really like to add to the developer guide is a big section on cn1libs covering usage of the top cn1libs e.g. parse, maps, bouncy castle etc. but I can’t seem to find the time/person to do that.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Falphabet-scroll.html)

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
