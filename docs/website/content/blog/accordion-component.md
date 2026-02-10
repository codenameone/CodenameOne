---
title: Accordion Component
slug: accordion-component
url: /blog/accordion-component/
original_url: https://www.codenameone.com/blog/accordion-component.html
aliases:
- /blog/accordion-component.html
date: '2016-06-18'
author: Chen Fishbein
---

![Header Image](/blog/accordion-component/accordion-post.png)

The [Accordion](https://www.codenameone.com/javadoc/com/codename1/components/Accordion.html) ui pattern  
is a vertically stacked list of items. Each item can be opened/closed to reveal more content similarly to a  
[Tree](https://www.codenameone.com/javadoc/com/codename1/ui/tree/Tree.html) however unlike the  
`Tree` the `Accordion` is designed to include containers or arbitrary components rather than model based data.

This makes the `Accordion` more convenient as a tool for folding/collapsing UI elements known in advance  
whereas a `Tree` makes more sense as a tool to map data e.g. filesystem structure, XML hierarchy etc.

Note that the `Accordion` like many composite components in Codename One is scrollable by default which  
means you should use it within a non-scrollable hierarchy. If you wish to add it into a scrollable `Container` you  
should disable it’s default scrollability using `setScrollable(false)`.
    
    
    Form f = new Form("Accordion", new BorderLayout());
    Accordion accr = new Accordion();
    accr.addContent("Item1", new SpanLabel("The quick brown fox jumps over the lazy dogn"
            + "The quick brown fox jumps over the lazy dog"));
    accr.addContent("Item2", new SpanLabel("The quick brown fox jumps over the lazy dogn"
            + "The quick brown fox jumps over the lazy dogn "
            + "The quick brown fox jumps over the lazy dogn "
            + "The quick brown fox jumps over the lazy dogn "
            + ""));
    
    accr.addContent("Item3", BoxLayout.encloseY(new Label("Label"), new TextField(), new Button("Button"), new CheckBox("CheckBox")));
    
    f.add(BorderLayout.CENTER, accr);
    f.show();

![Accordion component](/blog/accordion-component/components-accordion.png)

Figure 1. Accordion component

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
