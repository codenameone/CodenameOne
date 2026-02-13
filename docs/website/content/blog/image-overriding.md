---
title: Image Overriding
slug: image-overriding
url: /blog/image-overriding/
original_url: https://www.codenameone.com/blog/image-overriding.html
aliases:
- /blog/image-overriding.html
date: '2016-08-17'
author: Shai Almog
---

![Header Image](/blog/image-overriding/clock-image.gif)

I hoped todays post would cover the new Kitchen Sink demo but due to a couple of bugs we decided to postpone that to next week. In the meantime I’d like to discuss something I did there which is actually pretty cool and most developers have no idea that we can do: Image Overriding.

The new kitchen sink renders a set of icons like the old demo. In this version of the demo we wanted to show off graphics drawing which is an important feature missing from the old demo. Steve’s clock demo is a great example of that!

When the time came to create an icon for that demo it dawned on us that it would be cool if the clock icon was actually “live”. We decided to create an `Image` in code that draws everything dynamically. Yes, we could have used a `Painter` but that would have meant reworking a lot of the existing code, the same would have been true if we used another component.

This is the code we used to create a clock, I’m redacting the clock drawing code itself for brevity’s sake:
    
    
    class ClockImage extends Image {
        int w = 250, h = 250;
    
        ClockImage() {
            super(null);
        }
    
        ClockImage(int w, int h) {
            super(null);
            this.w = w;
            this.h = h;
        }
    
        @Override
        public int getWidth() {
            return w;
        }
    
        @Override
        public int getHeight() {
            return h;
        }
    
        @Override
        public void scale(int width, int height) {
            w = width;
            h = height;
        }
    
        @Override
        public Image fill(int width, int height) {
            return new ClockImage(width, height);
        }
    
        @Override
        public Image applyMask(Object mask) {
            return new ClockImage(w, h);
        }
    
    
        @Override
        public boolean isAnimation() {
            return true;
        }
    
        @Override
        public boolean requiresDrawImage() {
            return true;
        }
    
        @Override
        protected void drawImage(Graphics g, Object nativeGraphics, int x, int y) {
            paintClock(g, x, y, w, h, x + g.getTranslateX(), y + g.getTranslateY());
        }
    
        @Override
        protected void drawImage(Graphics g, Object nativeGraphics, int x, int y, int w, int h) {
            paintClock(g, x, y, w, h, x + g.getTranslateX(), y + g.getTranslateY());
        }
    
        @Override
        public boolean animate() {
            if (System.currentTimeMillis() / 1000 != lastRenderedTime / 1000) {
                currentTime.setTime(System.currentTimeMillis());
                return true;
            }
            return false;
        }
    };

That is it!

Notice that the image must declare itself as an animation and then override `animate()` to invoke `drawImage`. Also notice the method `requiresDrawImage` which disables some builtin image optimizations that we can perform in the native layer that might break this.

This is how we built `FontImage` within our code and we might do more of this as we move forward to allow more vector graphics primitives.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
