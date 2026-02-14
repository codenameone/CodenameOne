---
title: VM Updates & Webservices
slug: vm-updates-webservices
url: /blog/vm-updates-webservices/
original_url: https://www.codenameone.com/blog/vm-updates-webservices.html
aliases:
- /blog/vm-updates-webservices.html
date: '2014-05-27'
author: Shai Almog
---

![Header Image](/blog/vm-updates-webservices/vm-updates-webservices-1.jpg)

  
  
  
  
![Picture](/blog/vm-updates-webservices/vm-updates-webservices-1.jpg)  
  
  
  

We are making good progress on our new iOS VM and are starting to test a much wider range of apps. The VM is still experimental however many features that didn’t work such as native interfaces, build flags etc. should now function as expected and perform well.  
  
Build times are still longer than Android build times and this can be attributed in a large part to  
[  
the screenshot process  
](http://www.codenameone.com/3/post/2014/03/the-7-screenshots-of-ios.html)  
and the fact that we still have a lot to compile. The compilation is not as fast as we would like it to be due to the overhead of reference counting and GC both of which are compiled directly into the code. This means more lines of code and thus more complexity for the compiler to tackle. However, the performance is still much better and the compile times are already shorter than the original XML VM backend. 

Our team is currently in the process of generating a simplified webservices API/wizard that would allow you to easily invoke server side functionality within your own custom server. We are currently thinking about something very similar to the GWT request response however unlike the GWT approach we plan to offer both synchronous and asynchronous API’s (which GWT can’t offer). This would mean that in order to use this API you will need a proxy server to intercept client calls then forward them to your actual webservice functionality (notice that if you use Java on the server this just means adding a servlet and some classes that we will generate for you. We are soliciting feedback for this so if you have a strong opinion/view on this now is the time to speak up!

On a completely different subject we added some error handling API’s to the ImageDownloadService which is a frequent RFE.  

* * *

Notice: This post was automatically converted using a script from an older blogging system. Some elements might not have come out as intended…. If that is the case please let us know via the comments section below.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Anonymous** — May 29, 2014 at 1:26 am ([permalink](https://www.codenameone.com/blog/vm-updates-webservices.html#comment-21780))

> Anonymous says:
>
> Very cool. Have you already decided when will you open-source the new iOS VM?
>



### **Anonymous** — May 29, 2014 at 2:51 am ([permalink](https://www.codenameone.com/blog/vm-updates-webservices.html#comment-22096))

> Anonymous says:
>
> Yes, I wrote in a previous post that we intend to open source it once the work is done.
>



### **Anonymous** — June 4, 2014 at 5:29 am ([permalink](https://www.codenameone.com/blog/vm-updates-webservices.html#comment-21726))

> Anonymous says:
>
> Hi, 
>
> This new VM seems very interesting. However, every time we compiled with the new VM for iOS, we got an error. 
>
> I am not sure where I should send it to you, so I pasted the link below. 
>
> [https://codename-one.appspo…]([https://codename-one.appspot.com/getData?m=result&i=6151835748401152&b=788a8a3c-7706-4f3b-a86c-bf6701f38f1d&n=error.txt](https://codename-one.appspot.com/getData?m=result&i=6151835748401152&b=788a8a3c-7706-4f3b-a86c-bf6701f38f1d&n=error.txt)) 
>
> Thank you. 
>
> Kind regards,
>



### **Anonymous** — June 4, 2014 at 2:27 pm ([permalink](https://www.codenameone.com/blog/vm-updates-webservices.html#comment-21711))

> Anonymous says:
>
> Hi, 
>
> thanks for the report. We broke some things a few days ago with some changes to the Facebook functionality. This should now work as expected.
>



### **Anonymous** — June 4, 2014 at 7:21 pm ([permalink](https://www.codenameone.com/blog/vm-updates-webservices.html#comment-21727))

> Anonymous says:
>
> Hi. 
>
> Thank you. 
>
> Unfortunately, the code I sent is still building (more than 2 hours now).
>



### **Anonymous** — June 5, 2014 at 12:25 am ([permalink](https://www.codenameone.com/blog/vm-updates-webservices.html#comment-22077))

> Anonymous says:
>
> Did you try canceling and resending the build?
>



### **Anonymous** — June 5, 2014 at 1:22 am ([permalink](https://www.codenameone.com/blog/vm-updates-webservices.html#comment-21961))

> Anonymous says:
>
> Hi. 
>
> Thank you for your answer. 
>
> I did it after your post. It’s now building since 30 minutes. 
>
> Thank you.
>



### **Anonymous** — June 5, 2014 at 2:10 am ([permalink](https://www.codenameone.com/blog/vm-updates-webservices.html#comment-21884))

> Anonymous says:
>
> Make sure your project binaries aren’t too large (do you have many images/resources). If you used include source uncheck that feature, from the looks of it your build exceeded App Engines size quotas.
>



### **Anonymous** — June 5, 2014 at 3:36 am ([permalink](https://www.codenameone.com/blog/vm-updates-webservices.html#comment-22058))

> Anonymous says:
>
> Thank you for your very prompt answer. 
>
> Yes, our theme.res file’s size is more than 80 MB. We do not checked the sources’s option. 
>
> The problem is we need these pictures (and we have only imported half of them for now). 
>
> Is there a way to delete the smaller resolutions ? 
>
> Or any other advices ? 
>
> Thanks again.
>



### **Anonymous** — June 5, 2014 at 7:15 am ([permalink](https://www.codenameone.com/blog/vm-updates-webservices.html#comment-21876))

> Anonymous says:
>
> Ok, I think we will manage with different solutions. 
>
> Thank you. 
>
> How can I change the optipng’s path / file ? 
>
> Thanks again.
>



### **Anonymous** — June 6, 2014 at 4:29 am ([permalink](https://www.codenameone.com/blog/vm-updates-webservices.html#comment-21622))

> Anonymous says:
>
> A theme above 2mb might impact performance and above 10mb would seriously impact performance. A binary over 50mb will not install on Android devices (due to google play limitations). 
>
> You should dynamically download the images and store them in the local filesystem or storage after application install. 
>
> Optipng can be configured in the advanced menu. You can remove the unnecessary DPI’s from the image advanced menu too.
>



### **Anonymous** — June 6, 2014 at 4:32 am ([permalink](https://www.codenameone.com/blog/vm-updates-webservices.html#comment-22027))

> Anonymous says:
>
> Hi, 
>
> Thank you for your advices and precisions. 
>
> I have seen for optipng but I would like to change the path I choose for it the first time. I have tried to remove and re-install my Netbeans Plug-in but I am not able to find where to change this path. 
>
> Thank you.
>



### **Anonymous** — June 6, 2014 at 1:46 pm ([permalink](https://www.codenameone.com/blog/vm-updates-webservices.html#comment-21933))

> Anonymous says:
>
> Hi, 
>
> What is the technical reason regarding the impact of the theme’s size on the performance ? 
>
> Could we store the pictures inside the executable but not inside the theme (without adding too many weight)? 
>
> Or the only good way to proceed is to download all the images ? 
>
> Thank you.
>



### **Anonymous** — June 6, 2014 at 3:38 pm ([permalink](https://www.codenameone.com/blog/vm-updates-webservices.html#comment-22130))

> Anonymous says:
>
> You can create multiple res files and also add images to the root. 
>
> Since a resource file is always loaded in its entirety that means all the images will be loaded when you load the theme. So its pretty heavy if you don’t intend to use most of them right away. 
>
> I would still suggest keeping the executable smaller for faster build times and you would still have the 50mb limit.
>



### **Anonymous** — June 17, 2014 at 11:32 am ([permalink](https://www.codenameone.com/blog/vm-updates-webservices.html#comment-21914))

> Anonymous says:
>
> Hi Shai, 
>
> We have significantly reduced the weight of our executable. And it does compile with the new VM and the new graphic pipeline. 
>
> But unfortunately, the app crashes (with or without the new graphic pipeline) just after the iOS splash screen. 
>
> exited abnormally with signal 11: Segmentation fault: 11 
>
> Also, on the simulator and on Android devices, transition between forms is now very very very slow. 
>
> Thank you.
>



### **Anonymous** — June 17, 2014 at 4:52 pm ([permalink](https://www.codenameone.com/blog/vm-updates-webservices.html#comment-21993))

> Anonymous says:
>
> We made an improvement to the new VM today which might help. If not try isolating functionality to pinpoint the crash and build a test case we can inspect.
>



### **Anonymous** — June 17, 2014 at 5:26 pm ([permalink](https://www.codenameone.com/blog/vm-updates-webservices.html#comment-21820))

> Anonymous says:
>
> Thank you. 
>
> It’s still not working so we will try to isolate more precisely the crash.
>



### **Anonymous** — June 20, 2014 at 5:49 am ([permalink](https://www.codenameone.com/blog/vm-updates-webservices.html#comment-21676))

> Anonymous says:
>
> Hi Shai, 
>
> We have started with an empty project. This project is build and we can launch it on our iPhone 5S without any issue. 
>
> Otherwise, as soon as we add URLImage.createToStorage inside the post Show method of the selected form, we only got a black screen. 
>
> The app didn’t crash but the screen is entirely black. 
>
> I can send you this test project if you want to have a look. 
>
> I develop on a Mac Book Pro, with Mavericks, and Netbeans 7.4. We test the result on iPhone 5S iOS 7.1.1. 
>
> Thank you.
>



### **Anonymous** — June 20, 2014 at 1:52 pm ([permalink](https://www.codenameone.com/blog/vm-updates-webservices.html#comment-21443))

> Anonymous says:
>
> If you have a test case you can file an issue in the issue tracker here: [http://code.google.com/p/co…](<http://code.google.com/p/codenameone/issues/>) 
>
> We only accept code from pro accounts or higher and then after exhausting other options to track the issue.
>



### **Anonymous** — June 21, 2014 at 4:21 pm ([permalink](https://www.codenameone.com/blog/vm-updates-webservices.html#comment-21885))

> Anonymous says:
>
> I will, thank you.
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
