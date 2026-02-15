---
title: Skin Designer
slug: skin-designer
url: /blog/skin-designer/
original_url: https://www.codenameone.com/blog/skin-designer.html
aliases:
- /blog/skin-designer.html
date: '2016-06-01'
author: Shai Almog
---

![Header Image](/blog/skin-designer/skindesigner.png)

While the Codename One skin file format is trivial it is a bit under documented, to partially alleviate this problem  
we created a simple tool: [Skin Designer](/demos/SkinDesigner/).

This tool allows us to create a device skin from two images (landscape & portrait). This skin file can then  
be used with the Codename One simulator & also contributed so other developers can enjoy it!

The first version of this tool is relatively simple but should allow you to create an contribute skins to our skin  
database as explained below.

### Using the Skin Designer

The Skin Designer is a Codename One app that you can  
[checkout from github](https://github.com/codenameone/CodenameOneSkinDesigner/tree/master). You can also  
run it directly in the web browser or install it locally on your Mac or Windows desktop from its demos page.

__ |  You can also run it on your mobile devices but it might not be as convenient   
---|---  
  
The main value of building this app in Codename One is the ability to run it in the web and the desktop unmodified.  
It also serves as an excellent reference sample for developers.

### How to Use it?

The skin designer tool allows you to visually design and save a Codename One skin file that you can  
use to simulate a custom device type. To use this tool you need two pictures of the device in portrait  
and landscape mode. The screen area of these pictures must match the device screen size!

__ |  To find files like that just search for the name of the device you are looking for followed by “mockup”.  
This will bring up device mockups you can often use. FYI be sure to read the license of the graphic you use  
if you intend to publish this, many creators require attribution!   
---|---  
  
#### Creating a Skin

You will need to select the device image in portrait/landscape, you can then enter the screen dimensions  
in pixels that should match the area within the graphic.  
You can enter the X/Y position or use the hand “pan tool” to visually position the screen within the graphic.

Check out the help page within the application for detailed explanation of the skin creation process.

### Submitting the Skin

Assuming your skin file doesn’t violate any copyrights we’d appreciate if you submit it to our official skin repository!  
If your skin file requires attribution please include that within the skin image…​

Start by navigating to [github.com/codenameone/codenameone-skins](https://github.com/codenameone/codenameone-skins)  
& fork the project. Drag your skin file into the OTA directory. Create a `112x112` icon png file for the skin  
and drag it into that directory as well.

Next select the `Skins.xml` file and edit it. Add your skin in a similar way to the other skin files and commit.

Now click the submit pull request button to submit these changes for inclusion in the main repository.

__ |  Skins created with the web interface will only start working with the upcoming Friday release as they require  
a bug fix in the simulator   
---|---
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Gareth Murfin** — February 5, 2018 at 10:18 am ([permalink](/blog/skin-designer/#comment-23681))

> Gareth Murfin says:
>
> Is there a way to change default zoom some how? I just want to use a good modern skin without having to resize everytime.
>



### **Shai Almog** — February 6, 2018 at 7:16 am ([permalink](/blog/skin-designer/#comment-23944))

> Shai Almog says:
>
> Haven’t played with it for a while so I’m not sure. Do you see an exception? Notice you can just debug this as it’s a regular cn1 project. FYI next week we’ll be pushing out an update to the simulator and at that point we’ll add a lot of new skins e.g. ![https://uploads.disquscdn.c…](https://uploads.disquscdn.com/images/805671ba5cdb91663df059a3086359b86f489273d482ea9c10093f9979c4d310.png)
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
