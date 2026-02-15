---
title: 'TIP: Lightweight Rich Text View'
slug: tip-lightweight-rich-text-view
url: /blog/tip-lightweight-rich-text-view/
original_url: https://www.codenameone.com/blog/tip-lightweight-rich-text-view.html
aliases:
- /blog/tip-lightweight-rich-text-view.html
date: '2017-02-19'
author: Shai Almog
---

![Header Image](/blog/tip-lightweight-rich-text-view/tip.jpg)

A very common request is support for rich text in Codename One, this is hard to do in a generic/performant cross platform way but can be done as I explained in my answer [here](http://stackoverflow.com/questions/42288518/html-link-for-one-word-in-label-text-in-codename-one). This is pretty common though so it might be worth it doing this in a generic way.

Building a “proper” rich text view would take some work as there are many edge cases & complexities. However, since we already have an XML parser that’s perfectly capable of parsing HTML I decided to try a very simple HTML based syntax just to show how easy it would be to create something generic like this. I avoided the “link” use case as that would require some link handler code and I avoided font sizes/colors as I didn’t want to go into attributes and related complexities.

Instead I just added support for bold and italic while demonstrating that simple things like line breaks still work:
    
    
    Form hi = new Form("Rich Text", BoxLayout.y());
    
    class RichTextView extends Container {
        private String text;
        public RichTextView() { __**(1)**
        }
    
        public RichTextView(String text) {
            setText(text);
        }
    
        public final void setText(String text) {
            this.text = text;
            final Font defaultFont = Font.createTrueTypeFont("native:MainRegular", "native:MainRegular"); __**(2)**
            final Font boldFont = Font.createTrueTypeFont("native:MainBold", "native:MainBold");
            final Font italicFont = Font.createTrueTypeFont("native:ItalicRegular", "native:ItalicRegular");
            final int sizeOfSpace = defaultFont.charWidth(' '); __**(3)**
            XMLParser parser = new XMLParser() {
                private Font currentFont = defaultFont;
                @Override
                protected void textElement(String text) {
                    if(text.length() > 0) {
                        if(text.indexOf(' ') > -1) {
                            for(String s : StringUtil.tokenize(text, ' ')) {
                                createComponent(s);
                            }
                        } else {
                            createComponent(text);
                        }
                    }
                }
    
                private void createComponent(String t) {
                    Label l = new Label(t);
                    Style s = l.getAllStyles();
                    s.setFont(currentFont); __**(4)**
                    s.setPaddingUnit(Style.UNIT_TYPE_PIXELS);
                    s.setPadding(0, 0, 0, sizeOfSpace);
                    s.setMargin(0, 0, 0, 0);
                    add(l);
                }
    
                @Override
                protected boolean startTag(String tag) {
                    switch(tag.toLowerCase()) {
                        case "b":
                            currentFont = boldFont;
                            break;
                        case "i":
                            currentFont = italicFont;
                            break;
                    }
                    return true;
                }
    
                @Override
                protected void endTag(String tag) {
                    currentFont = defaultFont;
                }
    
                @Override
                protected void attribute(String tag, String attributeName, String value) {
                }
    
                @Override
                protected void notifyError(int errorId, String tag, String attribute, String value, String description) {
                    Log.p("Error during parsing: " + tag);
                }
    
            };
            try {
                parser.eventParser(new CharArrayReader(("<body>" + text + "</body>").toCharArray())); __**(5)**
            } catch(IOException err) {
                Log.e(err);
            }
        }
    
        public String getText() {
            return text;
        }
    }
    
    hi.add(new RichTextView("This is plain text <b>this is bold</b> and <i>this is italic</i> and all of this breaks lines nicely as well...."));
    
    hi.show();

This code produces the image below. Notice the following things about it:

__**1** | The default layout is `FlowLayout` which works well for simple things like that but might be a little flaky for complex use cases  
---|---  
__**2** | For simplicity I just hardcoded the fonts  
__**3** | I removed the spaces and padding/margin. Then I used the width of the spaces to re-add a space in the form of padding. This allows line breaks on word boundaries  
__**4** | Here I reset the individual padding/margin to 0 except for the space (see <3>)  
__**5** | I need to wrap the text in a `<body>` tag as XML requires one parent tag. I use the event callback parser instead of DOM as it is a bit more convenient (and faster)  
  
![The demo above running in the simulator](/blog/tip-lightweight-rich-text-view/lightweight-rich-text-view.png)

Figure 1. The demo above running in the simulator

### Moving On

The obvious question is “why isn’t this in Codename One?”.

This is a proof of concept, the devil is in the details with things such as this and once we start going into them this will drag us down a huge rabbit hole. However, your personal use case might not be as extensive as ours would need to be. With this as a starting point I’m sure most use cases could be adapted to handle some form of rich text within your app.

I chose to use HTML because I already had a parser but the basic concept should work well for any markup language out there.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Francesco Galgani** — April 6, 2018 at 10:17 pm ([permalink](/blog/tip-lightweight-rich-text-view/#comment-23789))

> Thank you Shai, your “proof of concept” (as you definited it) is exactly what I was looking for. In some simple circumstances, it can be a good replacement for SpanLabel and an easier and better solution than BrowserComponent (expecially if I need a text that should be automatically internationalized like in Label and SpanLabel). However, I found two issues in your code, that are easy to fix. The first one is that the font size is zero, so no text is shown. To solve, I changed the fonts definitions so:
>
> int fontSize = Display.getInstance().convertToPixels(3);  
> final Font defaultFont = Font.createTrueTypeFont(“native:MainRegular”, “native:MainRegular”).derive(fontSize, Font.STYLE_PLAIN);  
> final Font boldFont = Font.createTrueTypeFont(“native:MainBold”, “native:MainBold”).derive(fontSize, Font.STYLE_PLAIN);  
> final Font italicFont = Font.createTrueTypeFont(“native:ItalicRegular”, “native:ItalicRegular”).derive(fontSize, Font.STYLE_PLAIN);
>
> The second issue is that the text is not internationalized. To solve, at the start of the method setText I added the code:
>
> Label internalization = new Label(text);  
> text = internalization.getText();
>



### **Shai Almog** — April 7, 2018 at 4:36 am ([permalink](/blog/tip-lightweight-rich-text-view/#comment-23796))

> Shai Almog says:
>
> Thanks. Odd that I didn’t see that.  
> Text is internationalized as individual words which probably isn’t what you’d want.
>
> You can easily internationalize the body of HTML and this example using the UIManager.getInstance().localize() method.
>



### **Gareth Murfin** — August 8, 2018 at 3:23 am ([permalink](/blog/tip-lightweight-rich-text-view/#comment-23650))

> Gareth Murfin says:
>
> Great little widget, with these fixes its showing ok .
>



### **Durank** — September 30, 2020 at 12:49 pm ([permalink](/blog/tip-lightweight-rich-text-view/#comment-24351))

> [Durank](https://avatars0.githubusercontent.com/u/16245755?v=4) says:
>
> Provide and example how to applie color to specific portion of text
>



### **Shai Almog** — October 1, 2020 at 6:40 am ([permalink](/blog/tip-lightweight-rich-text-view/#comment-24348))

> Shai Almog says:
>
> There’s nothing built in but it should be trivial to add that as it isn’t much different than setting bold styles. You can parse a `font` tag or a custom tag of your own and set the color any way you want. That’s the point of including the source and not building this into Codename One.
>



### **Julio Valeriron Ochoa** — September 22, 2021 at 3:03 pm ([permalink](/blog/tip-lightweight-rich-text-view/#comment-24490))

> Julio Valeriron Ochoa says:
>
> Please provide support to color and size
>



### **Lianna Casper** — September 23, 2021 at 1:54 am ([permalink](/blog/tip-lightweight-rich-text-view/#comment-24493))

> Lianna Casper says:
>
> This is literally the same. Just add `int currentColor` next to currentFont. Then when a color attribute is hit set the value of the currentColor and reset it to the default when exiting the tag.  
> Then just do an `s.setFgColor(currentColor);`.
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
