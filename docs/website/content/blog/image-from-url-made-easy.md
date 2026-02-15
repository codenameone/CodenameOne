---
title: Image From URL Made Easy
slug: image-from-url-made-easy
url: /blog/image-from-url-made-easy/
original_url: https://www.codenameone.com/blog/image-from-url-made-easy.html
aliases:
- /blog/image-from-url-made-easy.html
date: '2014-03-02'
author: Shai Almog
---

![Header Image](/blog/image-from-url-made-easy/image-from-url-made-easy-1.png)

  
  
  
  
![Picture](/blog/image-from-url-made-easy/image-from-url-made-easy-1.png)  
  
  
  

ImageDownloadService is one of the first classes we wrote when creating the original IO package, as such we were still thinking over the API and the code… stinks.  
  
  
I’ve had the task of writing a tutorial for ImageDownloadService for such a long time and I just kept procrastinating on it because it is so painful to deal with. Eventually I broke down and decided to solve the problem in the proper way of creating a completely new API that’s simpler and thus doesn’t really require an extensive tutorial: URLImage.  
  
  
  
  
URLImage is an image created with a URL… Simple. It is seamlessly downloaded, scaled (adapted) and automatically updates itself in place. All good.  
  
  
  
  
The simple use case is pretty trivial:  
  
  
Image i = URLImage.createToStorage(placeholder, “fileNameInStorage”, “http://xxx/myurl.jpg”, URLImage.RESIZE_SCALE);  
  
  
  
  
Alternatively you can use the similar  
  
  
  
  
  
  
  
  
URLImage.createToFileSystem method instead of the Storage version.  
  
  
  
This image can now be used anywhere a regular image will appear, it will initially show the placeholder image and then seamlessly replace it with the file after it was downloaded and stored.  
  
  
Notice: Since  
  
ImageIO is used to perform the operations of the adapter interface its required that ImageIO will work. It is currently working in JavaSE, Android, iOS & Windows Phone. It doesn’t work on J2ME/Blackberry devices so if you pass an adapter instance on those platforms it will probably fail to perform its task.  
  
  
  
  
If the file in the URL contains an image that is too bit it will scale it to match the size of the placeholder precisely!  
  
  
  
We currently also have an option to fail if the sizes don’t match. Notice that the image that will be saved is the scaled image, which means you will have very little overhead in downloading images that are the wrong size although you will get some artifacts.  
  
  
  
  
  
The last argument is really quite powerful, its an interface called URLImage.ImageAdapter and you can implement it to adapt the downloaded image in any way you like. E.g. you can use an image mask to automatically create a rounded version of the downloaded image or to scale based on aspect ratio. We will probably add some tools to implement such functionality based on user demand.  
  
  
  
  
To do this you just override public EncodedImage adaptImage(EncodedImage downloadedImage, Image placeholderImage) in the adapter interface and just return the processed encoded image. If you do heavy processing (e.g. rounded edge images) you would need to convert the processed image back to an encoded image so it can be saved. You would then also want to indicate that this operation should run asynchronously via the appropriate method in the class.  
  
  
  
  
If you need to download the file instantly and not wait for the image to appear before download initiates you can explicitly invoke the fetch() method which will asynchronously fetch the image from the network.  
  
  
  
  
  
  
**  
Lists  
**  
  
  
The biggest problem with image download service is with lists. We decided to attack this issue at the core by integrating URLImage support directly into GenericListCellRenderer which means it will work with MultiList, List & ContainerList. To use this support just define the name of the component (name not UIID) to end with _URLImage and give it an icon to use as the placeholder. This is easy to do in the multilist by changing the name of icon to icon_URLImage then using:  
  
  
map.put(“icon_URLImage”, urlToActualImage); in the data.  
  
  
Make sure you also set a “real” icon to the entry in the GUI builder or in handcoded applications. This is important since the icon will be implicitly extracted and used as the placeholder value. Everything else should be handled automatically. You can use setDefaultAdapter & setAdapter on the generic list cell renderer to install adapters for the images. The default is a scale adapter although we might change that to scale fill in the future.  
  
  
  
  
  
  
  
  
  
  
  
  
  
  

* * *

Notice: This post was automatically converted using a script from an older blogging system. Some elements might not have come out as intended…. If that is the case please let us know via the comments section below.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Anonymous** — March 14, 2014 at 7:15 am ([permalink](/blog/image-from-url-made-easy/#comment-21869))

> Anonymous says:
>
> Thanks, this is just what I needed for my new project! That old ImageDownloadService worked as well, but it surely felt a bit clumsy.
>



### **Anonymous** — April 4, 2014 at 12:27 am ([permalink](/blog/image-from-url-made-easy/#comment-22066))

> Anonymous says:
>
> Hi, i want to use the URLImage class in my current project but can’t seem to find it in my current version of codenameone running on netbeans 7.4. How do I incorperate it into my IDE? I tried downloading the latest plugin but it did not do the job. Any tips? As I find the ImageDownloadService confusing.
>



### **Anonymous** — April 4, 2014 at 2:57 am ([permalink](/blog/image-from-url-made-easy/#comment-21913))

> Anonymous says:
>
> Libraries are updated by going to the project properties Codename One section and clicking the update client libs button.
>



### **Anonymous** — April 4, 2014 at 9:12 am ([permalink](/blog/image-from-url-made-easy/#comment-22049))

> Anonymous says:
>
> Thanks for the prompt response!
>



### **Anonymous** — April 4, 2014 at 10:34 am ([permalink](/blog/image-from-url-made-easy/#comment-21976))

> Anonymous says:
>
> Hi Shai, I tested between two seperate image urls (via simulator) and noticed that unless the “fileNameInStorage” is changed, the image displayed will not change despite the url used. This likely means that the image is stored locally and is no longer being called via the URL. 
>
> I would like to know if the images that are downloaded via the URL are stored in a directory and if so where?
>



### **Anonymous** — April 4, 2014 at 3:46 pm ([permalink](/blog/image-from-url-made-easy/#comment-21992))

> Anonymous says:
>
> All images are placed in Storage or FileSystemStorage based on the type of URL you created under the file name you gave. Storage and file system are mapped to the .cn1 directory in your home directory for the simulator.
>



### **Anonymous** — June 19, 2014 at 5:47 am ([permalink](/blog/image-from-url-made-easy/#comment-22114))

> Anonymous says:
>
> Hi Shai, 
>
> This seems very convenient in most case. 
>
> I have 2 questions: 
>
> – what about background pictures ? 
>
> – is there anylimitations we should be aware of (like only PNG files, max size file, resolution) ? 
>
> Thank you.
>



### **Anonymous** — June 20, 2014 at 12:46 am ([permalink](/blog/image-from-url-made-easy/#comment-21675))

> Anonymous says:
>
> Hi, 
>
> background images should work too. 
>
> Any file type should work although if the image is rescaled using the default scaling in some cases the image will become a PNG and in others it will become a JPEG. The logic isn’t as clear as it should be.
>



### **Anonymous** — June 20, 2014 at 10:00 am ([permalink](/blog/image-from-url-made-easy/#comment-22143))

> Anonymous says:
>
> Hi. 
>
> I am not able to work with URLImage for background pictures. 
>
> Also, I have done a dumb test. I try to insert 4000 pictures downloaded inside my form. 
>
> When pictures are downloaded, scroll is smooth and efficient. 
>
> But, when app is downloading pictures, and during this time, we have a bad scroll effect on Android device (Nexus 4). But not on iOS. 
>
> Any idea ? 
>
> Thank you.
>



### **Anonymous** — June 20, 2014 at 1:53 pm ([permalink](/blog/image-from-url-made-easy/#comment-24238))

> Anonymous says:
>
> 4000 seems like a bit much.
>



### **Anonymous** — June 21, 2014 at 4:20 pm ([permalink](/blog/image-from-url-made-easy/#comment-21848))

> Anonymous says:
>
> Thank you for your answer Shai. 
>
> I agree, 4000 is quite too much. But I have the same result with 400. 
>
> And I do not get the difference between Android and iOS. 
>
> Is there something I can / should do ? 
>
> I read on the forum other option like pre-download the pictures in the background and I will use it anyway. But if I can do something on this case, it would be great. 
>
> Thank you.
>



### **Anonymous** — March 3, 2015 at 6:31 am ([permalink](/blog/image-from-url-made-easy/#comment-22148))

> Anonymous says:
>
> How can I fetch changing placeholder image by downloaded image for revalidate ImageViewer?
>



### **Anonymous** — March 3, 2015 at 12:19 pm ([permalink](/blog/image-from-url-made-easy/#comment-22178))

> Anonymous says:
>
> I don’t quite understand the question but did you see the property cross demo and tutorial where we use this API in two different settings: 
>
> [http://www.codenameone.com/…](<http://www.codenameone.com/blog/propertycross-demo>) 
>
> [https://www.udemy.com/learn…](<https://www.udemy.com/learn-mobile-programming-by-example-with-codename-one/>)
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
