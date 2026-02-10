---
title: Diagonal Badging
slug: diagonal-badging
url: /blog/diagonal-badging/
original_url: https://www.codenameone.com/blog/diagonal-badging.html
aliases:
- /blog/diagonal-badging.html
date: '2020-07-09'
author: Shai Almog
---

![Header Image](/blog/diagonal-badging/learn-codenameone-2.jpg)

Recently I got a [question on stackoverflow](https://stackoverflow.com/questions/62639544/codename-one-how-to-design-an-inverted-status-badge/62669046) about doing a diagonal badge that’s actually quite similar to the one on our website. This isn’t hard to do in Codename One but it requires a bit of tinkering so here’s how you would implement that.

As a matter of fact the original code I provided in the answer had a bit of a misbehavior which I’m fixing for this blog post so everything will look correct:
    
    
    Form hi = new Form("Hi World", BoxLayout.y());
    
    // just filling some space so the layer below will be visible
    SpanLabel base = new SpanLabel("Hi World,nLorem IpsumnLorem IpsumnLorem IpsumnLorem IpsumnLorem IpsumnLorem IpsumnLorem IpsumnLorem IpsumnLorem IpsumnLorem Ipsumn");
    
    // this is the green label
    Label green = new Label("Green Stuff") {
        private int actualHeight = 10;
    
        // we ask for more space so when we rotate the label it won't be clipped out
        @Override
        protected Dimension calcPreferredSize() {
            Dimension d = super.calcPreferredSize();
    
            // since we asked for more space the background will become a sqare
            // we don't want that so we save the "real" height here
            actualHeight = d.getHeight();
            d.setHeight(d.getWidth());
            return d;
        }
    
        @Override
        public void paint(Graphics g) {
            // I move the drawing context up and to the left otherwise
            // the banner will be cropped on the corner and look odd
            g.translate(-(actualHeight / 2), -(actualHeight / 2)); __**(1)**
    
            // we rotate by 45 degrees in radians around the pivot point
            // which is the center of the component
            g.rotateRadians((float)(-Math.PI / 4.0),
                    getX() + getWidth() / 2,
                    getY() + getHeight() / 2);
    
            // we save the old color and set a background color then
            // draw the background manually
            int c = g.getColor();
            g.setColor(0xff00);
    
            // we take extra space so the banner will stretch further
            // I use fill here but I can use draw image if I have an
            // image from the designer that looks better
            g.fillRect(getX() - 50,
                    getY() + getHeight() / 2 - actualHeight / 2,
                    getWidth() + 100, actualHeight);
    
            // we let the label draw its content
            super.paint(g);
    
            // restoring the graphics context to the original value
            g.setColor(c);
            g.resetAffine();
        }
    };
    
    // we're drawing the background manually so we must make it transparent
    Style s = green.getUnselectedStyle();
    s.setBgTransparency(0);
    
    // I want extra side padding because the rotation will cause the sides
    // to crop so the text needs to be in the center
    s.setPaddingUnit(Style.UNIT_TYPE_DIPS); __**(2)**
    s.setPadding(1, 1, 3, 3);
    
    // we're layering the component on top of one another. The green
    // label is positioned in the top left coordinate.
    Container cnt = LayeredLayout.encloseIn(base,
            FlowLayout.encloseIn(green));
    hi.add(cnt);
    
    hi.show();

__**1** | The original post was missing this line to move the context up and to the left  
---|---  
__**2** | It was also missing these two lines  
  
So this is what we see in the new code:

![The Correct Banner](/blog/diagonal-badging/green-banner.png)

Figure 1. The Correct Banner

But the older code cropped things badly causing an arrow effect:

![The Arrow Effect](/blog/diagonal-badging/green-banner-arrow.png)

Figure 2. The Arrow Effect

What we see here is the banner being drawn at the edge of the component bounds. As a result the component bounds crop it on the right and bottom producing an arrow.

Moving the drawing a bit up and to the left makes sure we reach the edge at just the right place.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
