---
title: CUSTOMIZE COMPONENT BORDERS AND BACKGROUNDS
slug: how-do-i-create-a-9-piece-image-border
url: /how-do-i/how-do-i-create-a-9-piece-image-border/
type: howdoi
original_url: https://www.codenameone.com/how_di_i/how-do-i-create-a-9-piece-image-border.html
tags:
- basic
- ui
description: The 9-piece image border is a cornerstone tool for theme design
youtube_id: ACsZ8qiwR8Q
thumbnail: https://www.codenameone.com/wp-content/uploads/2020/10/Mask-Group-7.png
---

{{< youtube "ACsZ8qiwR8Q" >}} 

#### Transcript

Backgrounds are a pretty complex subject so this won’t be as short as most of these videos. I’ll also focus on the designer tool instead of code but everything I show here can be done using code and the style API.  
Lets start with backgrounds what does that actually mean? A component can have one of the following background types: Border, Image, Gradient or Color. Notice that these are exclusive to one another…  
If you have a border and a background image the border will take over and the background image will be ignored. If you have a gradient the background color will be ignored. So the order of border, image or gradient and color is significant. It’s also important to notice that you can’t have both an image and a gradient, when we will go over the interface the reason will become clear.

There are a few types of borders but most of them aren’t used as much, I’ll focus on the top six border types. Notice I’ll go into each of these borders in more details soon… I’ll start with line border which is by far the simplest, just a line surrounding the component.

Underline is a newer border type, in the past we used to advocate using image borders or tiled images for underline but we eventually decided to introduce this type.

9-piece image allows us to use an image cut into 9 pieces 4 images represent the corners and 4 images for the top, bottom, left & right. One additional image for the center. The images in the top, bottom, left, right & center are tiled to allow scaling of the border without degradation.

3-piece image borders can be either horizontal or vertical. The horizontal version includes one image for the left one for the center which is tiled and one for the right. The same is true for the vertical one which has top, bottom and center. In both cases only the center image is tiled. This is useful for borders such as the old iOS back button where the arrow image on the left side couldn’t be cut in a reasonable way for a 9-piece border. A 3 piece border can’t scale on the opposite axis so a horizontal border can’t grow vertically and visa versa.

A round border can be either a circle like a floating action button in Android or a pill style border like an iOS hint.

The rounded rectangle border is a square border whose edges are rounded, there are two variations of this border type.

Lets start with the line border, when we open the designer tool we can pick it from the three dot button.  
Notice we can set the color of the border here and it’s separate from the background color or choose the theme color option.

We can also set the thickness of the border in pixels or in millimeters.

Underline is practically identical in every way except for the fact that the line is drawn strictly at the bottom of the UI. This is a pretty common use case when separating elements or even for material design style text fields.

Image borders are comprised of 9 images but making those images is pretty darn hard which is why we created this wizard in the designer too.

When you launch it you can see some UI that allows you to create an image for the 9 piece border but that’s mostly a legacy UI.

For most cases people choose to use a file which they usually get from a PSD or similar design source.

Here I chose a chat bubble element and now I can move to the next step of cutting it.

We need to switch to the cut image tab where we can see these 4 guides. You can move these 4 guides using the numbers on the bottom left.

These guides allow us to determine the 9 images that we need in order to build the border. If you look through them you will see 9 images in the cut. Notice several things about the locations of the guides: I kept the corners separate so they won’t be cut or tiled. I kept the arrow outside, otherwise it will look smeared. Notice I kept the two guides very close to one another… Normally that’s not ideal as smaller images reduce performance but since the image has a subtle gradient I didn’t want to impact that.

Another crucial feature is multi image. 9-Piece images are impacted by image density just like everything else. With regular images we can use multi-image to reduce problems with density and 9-piece borders are no exception. In order to use this you can just specify the density of the source image so in this case I specified that the image above was generated for a “Very High” DPI. You might choose to use HD images when generating a 9-piece border. Then I selected the DPI’s for which I want images generated and each one of the 9-piece images will be generated as a multi image and we’ll have a cool border.

However, this poses a big problem. Say you ran the 9-piece wizard and got a bad result then you ran it again and again. You can end up with a lot of multi images in the theme file that balloon the size of your app for no real reason. For that we have a special feature in the designer tool called “Delete Unused Images” that you can find in the menu of the designer too.  
Once you pick that you will be presented with the images to delete, you can unselect an image using control click and when you press OK everything selected will be deleted. If you have images for use within your application you need to unselect them here as we can’t detect whether you use something in your app.

Image borders can be customized with the manual UI instead of through the wizard. Each one of these combo boxes allows you to pick an image. In this case it shows the images we cut in the wizard but you can use this manually and set the 9 images yourself.

As I mentioned before the horizontal image border mode is similar but only accepts 3 images. It doesn’t have a wizard as it’s not as common as the 9-piece image border but it’s not as hard to build manually as it only requires 3 images.

Similarly the vertical image border requires images on the vertical axis. Both of these are very rarely used. In the past they were common for round borders but now we have a special border type for that.

Which brings us to round border. Round images can’t be tiled with the 9-piece images as the tiling will create a very ugly effect.  
Round borders allow for perfectly round buttons that grow with a fixed radius.  
They also allow for pill style borders that are rectangles whose sides are perfect 180 degree arcs.  
As you can see in this sample the X button at the top right is the round version of the round rectangle whereas the text fields use the round rectangle “pill” mode.

Round borders have their own custom UI in the tool It includes many customization options such as color and opacity. You can customize the stroke which is the line around the border… The border can have an optional shadow if you define a shadow opacity at which point you can customize the spread and blur of the shadow. On the bottom you will see the checkbox that toggles rectangle mode which produces the “pill” effect.

Rounded rectangle is very similar but unlike the round mode it inherits colors from the standard style. It obviously doesn’t have a rectangle mode but it has most of the other options in place and allows us to customize the look of a round rectangle. There is an older version of round rectangle that didn’t have the same level of customizability available to it.

Empty border sounds redundant but it’s often a crucial feature! As I mentioned before a border overrides other background styles so if the base theme has a border it will override any other setting we make. This is common with Button which usually has a border defined so if you want a different background type it just won’t work. Defining the border to empty solves this common problem.

Now that we have an empty border we can try setting up other properties the standard background can be a scaled image. In this case it’s the duke logo and you’ll notice that scaled looks really weird in the preview below.

That’s easily solvable with the next two entries. Image scaled to fill scales the image while maintaining aspect ratio. That means the image is scaled to fill up the entire area and everything that’s outside of the component bounds gets cropped. This is the most common image background style and makes sense for most cases, it looks weird here since the image is square and the preview is a very long rectangle but for most cases this makes sense.

Scaled to fit scales the image while maintaining the aspect ratio. It does that while fitting the whole image into the component. Notice that this leaves blank areas, in this case on the sides but unless the image is an exact match you will have blank areas that will be filled with the background color.

You can also tile an image all over the place, this is useful for pattern background and can create a great visual effect that adapts to any resolution. Notice that you shouldn’t use images that are too small with tiling as performance might suffer in such cases.

You can also tile on an axis with specific alignment so you can tile vertically on the left, right or center. You can also tile horizontally on the center, top or bottom. This is valuable for some nice visual effects but is also useful for cases such as line separators where you can use tiling to separate this component from the next one visually.

We can also just align the image vertically or horizontally on almost every corner side or to the center. This is useful for some special design decorations, for instance in an app I did for a Yoga Studio I placed a flower in the bottom right corner. The flower remains even when scrolling and creates a nice texture effect.

You can also use gradients to define a background, the gradients we support are relatively simple and might look a bit different on devices where they are implemented slightly differently. We have three gradient types, linear horizontal, vertical and radial. They are all pretty similar and relatively simple, if you need more complex gradients you might need to use a background image.

If you use none or don’t define an image for the background you can define the background color. Notice that the background color can have a transparency value associated with it. The transparency value is a number between 0 for completely transparent to 255 for completely opaque background.

Thanks for watching, I hope you found this helpful.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
