---
title: 'TIP: Rich View Revisited'
slug: tip-rich-view-revisited
url: /blog/tip-rich-view-revisited/
original_url: https://www.codenameone.com/blog/tip-rich-view-revisited.html
aliases:
- /blog/tip-rich-view-revisited.html
date: '2019-04-22'
author: Shai Almog
---

![Header Image](/blog/tip-rich-view-revisited/tip.jpg)

A couple of years ago I posted a [tip about rich text view](/blog/tip-lightweight-rich-text-view/) which worked out reasonably well. But it’s a bit outdated by now and it’s worth revisiting. During these two years we published the [Facebook Clone](https://codenameone.teachable.com/p/build-real-world-full-stack-mobile-apps-in-java) which used this component.

In the facebook clone I did a lot of work for the rich text. This work added support for alignment and clickable hyperlinks as well as a couple of bug fixes. Following is the code I used in the Facebook Clone for the rich text.
    
    
    public class RichTextView extends Container {
        private String text;
        private float fontSize = 2.6f;
        private EventDispatcher listeners = new EventDispatcher();
    
        private Font currentFont;
        private int currentColor = 0;
        private String currentLink;
        private Style lastCmp;
        private Font defaultFont;
        private Font boldFont;
        private Font italicFont;
        private int sizeOfSpace;
    
        public RichTextView() {
            init(null);
        }
    
        public RichTextView(String text, String uiid) {
            init(uiid);
            setText(text);
        }
    
        public RichTextView(String text) {
            init(null);
            setText(text);
        }
    
        private void init(String uiid) {
            boldFont = Font.createTrueTypeFont(NATIVE_MAIN_BOLD, fontSize);
            italicFont = Font.createTrueTypeFont(NATIVE_ITALIC_LIGHT, fontSize);
            if(uiid == null) {
                defaultFont = Font.createTrueTypeFont(NATIVE_MAIN_LIGHT,
                    fontSize);
            } else {
                Style s = UIManager.getInstance().getComponentStyle(uiid);
                defaultFont = s.getFont();
                boldFont = boldFont.derive(defaultFont.getHeight(),
                    Font.STYLE_BOLD);
                italicFont = italicFont.derive(defaultFont.getHeight(),
                    Font.STYLE_ITALIC);
            }
            sizeOfSpace = defaultFont.charWidth(' ');
            currentFont = defaultFont;
        }
    
        public void setAlignment(int align) {
            ((FlowLayout)getLayout()).setAlign(align);
        }
    
        private void createComponent(String t) {
            if(t.indexOf(' ') > -1) {
                for(String s : StringUtil.tokenize(t, ' ')) {
                    createComponent(s);
                }
                return;
            }
            Label l;
            if(currentLink != null) {
                Button b = new Button(t, "Label");
                final String currentLinkValue = currentLink;
                b.addActionListener(e -> listeners.fireActionEvent(
                        new ActionEvent(currentLinkValue)));
                l = b;
            } else {
                l = new Label(t);
            }
            Style s = l.getAllStyles();
            s.setFont(currentFont);
            s.setFgColor(currentColor);
            s.setPaddingUnit(Style.UNIT_TYPE_PIXELS);
            s.setPadding(0, 0, 0, sizeOfSpace);
            s.setMargin(0, 0, 0, 0);
            lastCmp = s;
            add(l);
        }
    
        public final void setText(String text) {
            this.text = text;
            removeAll();
            try {
                char[] chrs = ("<body>" + text + "</body>").toCharArray();
                new Parser().eventParser(new CharArrayReader(chrs));
            } catch(IOException err) {
                log(err);
            }
        }
    
        public String getText() {
            return text;
        }
    
        public void addLinkListener(ActionListener al) {
            listeners.addListener(al);
        }
    
        public void removeLinkListener(ActionListener al) {
            listeners.removeListener(al);
        }
    
        class Parser extends XMLParser {
            @Override
            protected void textElement(String text) {
                if(text.length() > 0) {
                    if(lastCmp != null && text.startsWith(" ")) {
                        lastCmp.setPadding(0, 0, 0, sizeOfSpace);
                    }
                    createComponent(text);
                    if(!text.endsWith(" ")) {
                        lastCmp.setPadding(0, 0, 0, 0);
                    }
                }
            }
    
            @Override
            protected boolean startTag(String tag) {
                switch(tag.toLowerCase()) {
                    case "a":
                        currentColor = 0x4267B2;
                        break;
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
                currentColor = 0;
                currentLink = null;
                currentFont = defaultFont;
            }
    
            @Override
            protected void attribute(
                    String tag, String attributeName, String value) {
                if(tag.toLowerCase().equals("a") &&
                        attributeName.toLowerCase().equals("href")) {
                    currentLink = value;
                }
            }
    
            @Override
            protected void notifyError(int errorId, String tag,
                    String attribute, String value, String description) {
                log("Error during parsing: " + tag);
            }
        }
    }
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Durank** — September 30, 2020 at 12:19 pm ([permalink](/blog/tip-rich-view-revisited/#comment-24349))

> [Durank](https://avatars0.githubusercontent.com/u/16245755?v=4) says:
>
> Please provide an example to use this class and its results
>



### **Shai Almog** — October 1, 2020 at 6:38 am ([permalink](/blog/tip-rich-view-revisited/#comment-24352))

> Shai Almog says:
>
> This is available in the post I linked above: </blog/tip-lightweight-rich-text-view/>
>
> “`  
> hi.add(new RichTextView(“This is plain text **this is bold** and _this is italic_ and all of this breaks lines nicely as well….”));  
> “`
>



### **Julio Valeriron Ochoa** — September 23, 2021 at 12:51 pm ([permalink](/blog/tip-rich-view-revisited/#comment-24494))

> Julio Valeriron Ochoa says:
>
> how can I add a tag to set color to specific text?
>



### **Lianna Casper** — September 23, 2021 at 4:27 pm ([permalink](/blog/tip-rich-view-revisited/#comment-24496))

> Lianna Casper says:
>
> Just like the a tag only instead of the href get the value of the color there.
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
