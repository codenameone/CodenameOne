---
title: WORK WITH MULTI IMAGES AND DEVICE DENSITIES
slug: how-do-i-fetch-an-image-from-the-resource-file-add-a-multiimage
url: /how-do-i/how-do-i-fetch-an-image-from-the-resource-file-add-a-multiimage/
type: howdoi
original_url: https://www.codenameone.com/how_di_i/how-do-i-fetch-an-image-from-the-resource-file-add-a-multiimage.html
tags:
- basic
- ui
description: Understand the complexity of phone/tablet images
youtube_id: sK-u1TBWFX8
thumbnail: https://www.codenameone.com/wp-content/uploads/2020/09/hqdefault-3-1.jpg
---

{{< youtube "sK-u1TBWFX8" >}} 

#### Transcript

In this short video I’ll explain the ideas behind device densities and images in a mobile phone environment. In this environment the differences between device screens are big. Lets start with a simple question, on the left you see an iPhone 6 and on the right you see an iPad Air 2. Both are not to scale but I’ll ask the abstract question: “which device is bigger?”

In terms of resolution the iPad is clearly bigger. It has more pixels than the iPhone 6. The iphone has 1 million pixels and the ipads has 3 million pixels. Thats a 3 fold difference!

The ipad is also much bigger than the iphone nearly twice the size diagonally. This might seem to seal the deal, the ipad seems bigger. However, it’s the exact opposite!

In reality the iphone is arguably bigger. It has more pixels per inch or PPI than the ipad simply because the pixels are more densely packed together. So for every inch of iphone you get more pixels. This means that in order to show an image on the iphone we’ll need 50% more pixels… This difference is even further highlighted when comparing newer devices to older devices as densities keep rising.

To repeat the point, this means that if you load an image in an iphone and show the exact same image on an ipad this image will look much smaller on the iphone almost 50% smaller! This might not sound like a big deal but keep in mind that these are touch oriented devices so if an image is an icon that you are meant to tap on. This icon might be too small for your finger in a high PPI device.

This difference gets freakishly comical in some situations, I still have a second generation ipad and that’s a 132 ppi device a newer pixel device which isn’t the highest density device available is 538 ppi. Making an app that adapts properly to both of these requires good understanding of the tools at hand…

As developers resolution is something that’s instantly obvious but density isn’t. However, on mobile because of the touch interface it’s crucial that we adapt properly to the device density. It’s also important that we use it effectively. Many developers expect iPads to just use bigger pictures but that would create a tablet interface that just feels like a scaled up phone application. This is a bad user experience. Users expect the touch interface to adapt to higher PPI devices to make use of the extra space for more content.

To drive the point home here is the kitchen sink demo on a phone and an ipad. Notice that the iPad UI adapts to make the most use of available screen real-estate rather than just scaling up the phone UI. In fact we chose to use slightly smaller images in the tablet mode so we can fit in more data.

One of the most important things we need to do to adapt to high resolution UI’s is scale the images. To understand the problems and benefits we first need to understand that there are 2 types of images. Vector and raster. Vector images are stored as a set of low level drawing commands.  
This means that when we scale a vector it’s still perfectly smooth as the lines are redrawn in the new resolution.

FontImage and the material design icon fonts we have builtin are types of vector images and you should use them when possible. We also have some basic support for an SVG transcoder although at this time this is a bit new. SVG is the standard for vector graphics.  
However, most graphic resources aren’t in vector format and are just stored as a set of colors for pixels. The common formats that you use every day such as JPEG, GIF & PNG are all raster images.

Since rasters are just a set of pixels scaling them without quality degradation is a problem. There will always be some loss of quality but doing the scaling on the device will be nearly impossible.  
So the common solution which exists natively in all mobile OS’s is to ship the app with ready made scaled rasters to all the resolutions you need. Our solution for this is multi-image.

We support automatic scaling on the fly of multi images which means you don’t need to maintain 5-10 versions of the same image. You can just have one high quality version of the image and we will generate the rest for you using a high quality scaling algorithm on the desktop.

Before we get into that I’d like to introduce the concept of densities in Codename One. Codename One defines these density constants that match past and future devices. One of the cool and unexpected surprises with density is that HD TV is a very low density device and even 4k TV sits at medium or low density. That sounds odd until you realize that a TV is typically huge and you sit relatively far from it. You can’t distinguish pixels in the same level as you could on a phone.  
Other than that notice that we have many densities ranging from low to 4K. Some of them don’t have devices targeting them yet and might not be required. Also notice that density is an estimate here, devices are notoriously bad at these things so the numbers might fluctuate slightly.

Lets move on and add a multi image in the Codename One Designer using the quick add multi image option. I’ll pick our standard icon which is a 512 by 512 image. Here I’m faced with an option to select the source resolution. The source resolution is the density from the previous list to which the image was built. So if the resource image I’m importing was designed for a very high density I should pick that. In this case the image is just a random image I’m using so I’ll pick 560. However, if I’d have picked a lower DPI all of the resulting images would have been bigger. If I’d have picked a higher DPI all the resulting images would have been smaller.

You will notice all the images are now automatically created and sized. We can toggle between them and see the size difference and we can use the standard features of the designer tool such as opening them in an external editor etc.

The add multi images as opposed to the quick add we used before is a manual approach for adding multi images. We don’t use it as much but if you want more fine grain control over scaling you can use this. You can literally determine the size in pixels of every scaled instance where 0 means an instance will not be generated.  
You can select the option to preserve aspect ratio which makes sure the image will scale without distortion. You can also use percentage to scale the image uniformly with a single number.

You can use the square image to indicate that the width/height should remain square in which point it will force the values to remain the same.  
Once this is done you can inspect the resulting images and manipulate them just like we could before in the quick add mode.

In both cases you can load the image from code by using getImage on the theme resources object. Notice you can create additional resource files and work with those files as well and also download resource files dynamically. Another important aspect of this is that multi image is seamless. It acts just like any other image…  
When you read the resource file the resolutions that you don’t need are discarded and you don’t pay for the multi image in runtime. There is a cost in application size so it isn’t completely free though.

Thanks for watching, I hope you found this helpful.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
