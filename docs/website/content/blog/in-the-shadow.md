---
title: In the Shadow
slug: in-the-shadow
url: /blog/in-the-shadow/
original_url: https://www.codenameone.com/blog/in-the-shadow.html
aliases:
- /blog/in-the-shadow.html
date: '2016-08-28'
author: Shai Almog
---

![Header Image](/blog/in-the-shadow/dropshadow.png)

[Diamond](https://twitter.com/diamondobama) asked us about an easy way to do dropshadows in Codename One to which I answered that it’s pretty easy thanks to our [Gaussian blur support](/blog/toastbar-gaussian-blur.html)…​

We ended up with this code which is usable but probably not as intuitive for most:
    
    
    Form hi = new Form("Drop Shadow", new BorderLayout(BorderLayout.CENTER_BEHAVIOR_CENTER));
    
    FontImage mm = FontImage.createMaterial(FontImage.MATERIAL_PERSON, "Button", 30);
    int[] rgb = mm.toImage().getRGB();
    for(int iter = 0 ; iter < rgb.length ; iter++) {
        rgb[iter] = rgb[iter] & 0xff000000;
    }
    Image shadow = Image.createImage(rgb, mm.getWidth(), mm.getHeight());
    if(Display.getInstance().isGaussianBlurSupported()) {
        shadow = Display.getInstance().gaussianBlurImage(shadow, 10);
    }
    
    Label top = new Label(mm, "Container");
    Label bottom = new Label(shadow, "Container");
    bottom.getAllStyles().setMargin(1, 0, 1, 0);
    bottom.getAllStyles().setMarginUnit(Style.UNIT_TYPE_DIPS);
    hi.add(BorderLayout.CENTER, LayeredLayout.encloseIn(bottom, top));
    
    hi.show();

The effect is attractive and commonplace so I think it would be great to add it universally so we added two methods which will be a part of the coming update. These methods are in the `Effects` class and are both simple utility methods. Once creates a shadow and incorporates it into a new image at the given location. The other returns the shadow alone which you can shift/position as you see fit (e.g. if you have similarly shaped images this might also be useful in terms of CPU/RAM).
    
    
    /**
     * Generates a shadow for the source image and returns a new larger image containing the shadow
     *
     * @param source the source image for whom the shadow should be generated
     * @param blurRadius a shadow is blurred using a gaussian blur when available, a value of 10 is often satisfactory
     * @param opacity the opacity of the shadow between 0 - 1 where 1 is completely opaque
     * @param xDistance the distance on the x axis from the main image body in pixels e.g. a negative value will represent a lightsource from the right (shadow on the left)
     * @param yDistance the distance on the y axis from the main image body in pixels e.g. a negative value will represent a lightsource from the bottom (shadow on top)
     * @return a new image whose size incorporates x/yDistance
     */
    public static Image dropshadow(Image source, int blurRadius, float opacity, int xDistance, int yDistance);
    
    /**
     * Generates a shadow for the source image and returns either the shadow itself or the image merged with the
     * shadow.
     *
     * @param source the source image for whom the shadow should be generated
     * @param blurRadius a shadow is blurred using a gaussian blur when available, a value of 10 is often satisfactory
     * @param opacity the opacity of the shadow between 0 - 1 where 1 is completely opaque
     * @return an image containing the shadow for source
     */
    public static Image dropshadow(Image source, int blurRadius, float opacity);

This can result in simple code to do a drop shadow effect:
    
    
    Form hi = new Form("Drop Shadow", new BorderLayout(BorderLayout.CENTER_BEHAVIOR_CENTER));
    
    int twoMM = Display.getInstance().convertToPixels(2);
    Image img = duke.scaledWidth(Display.getInstance().getDisplayWidth() / 2);
    hi.add(BorderLayout.CENTER, new Label(Effects.dropshadow(img, 10, 0.8f, twoMM, twoMM), "Container"));
    
    hi.show();

__ |  The variable duke is the image of the icon placed into the src directory   
---|---
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **beck** — September 11, 2016 at 9:46 am ([permalink](https://www.codenameone.com/blog/in-the-shadow.html#comment-22916))

> beck says:
>
> We can use the shadow effect in img only or in the components as well. eg I have a blue background container. Can i have the shadow effect in this container as well?
>



### **Shai Almog** — September 12, 2016 at 4:17 am ([permalink](https://www.codenameone.com/blog/in-the-shadow.html#comment-22972))

> Shai Almog says:
>
> If the background of the container is fixed you can draw the image with the background then create a shadow for that and place everything in a layered layout.
>



### **Bayu Sanjaya** — September 12, 2016 at 1:09 pm ([permalink](https://www.codenameone.com/blog/in-the-shadow.html#comment-23022))

> Bayu Sanjaya says:
>
> can you give us an example for shadowed container?
>



### **Shai Almog** — September 13, 2016 at 3:47 am ([permalink](https://www.codenameone.com/blog/in-the-shadow.html#comment-21463))

> Shai Almog says:
>
> Containers are transparent by default so doing this generically isn’t necessarily ideal. It might also pose a problem if the container is scrollable or if it reflows (e.g. on rotation). I would suggest doing this for individual components within the container. Just paint the component on an image and use that as the shadow base then do something like LayeredLayout.encloseIn(new Label(myShadow), myComponent)
>



### **Ali Ahmadur Rahman** — August 5, 2018 at 8:42 pm ([permalink](https://www.codenameone.com/blog/in-the-shadow.html#comment-23820))

> Ali Ahmadur Rahman says:
>
> Nice tutorial. Hope to get more from you.
>



### **Martin Brook** — October 28, 2018 at 10:17 am ([permalink](https://www.codenameone.com/blog/in-the-shadow.html#comment-23950))

> Martin Brook says:
>
> Great article. thanks a lot for sharing with us.
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
